package me.christianrobert.ora2postgre.controllers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.Config;
import me.christianrobert.ora2postgre.transfer.DataTransferService;
import me.christianrobert.ora2postgre.jobs.JobManager;
import me.christianrobert.ora2postgre.jobs.MigrationProgressService;
import me.christianrobert.ora2postgre.jobs.MigrationStep;
import me.christianrobert.ora2postgre.config.ConfigurationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

@Path("/migration")
@ApplicationScoped
@Tag(name = "Data Transfer", description = "Oracle to PostgreSQL data migration operations")
public class DataTransferController {

    private static final Logger log = LoggerFactory.getLogger(DataTransferController.class);

    @Inject
    Everything data;
    
    @Inject
    Config config;
    
    @Inject
    JobManager jobManager;
    
    @Inject
    ConfigurationService configurationService;
    
    @Inject
    MigrationProgressService progressService;

    @POST
    @Path("/transferdata")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Transfer Table Data Oracle → PostgreSQL",
        description = "Phase 5: Performs high-performance bulk data transfer from Oracle to PostgreSQL with data type conversion, ANYDATA handling, and parallel processing."
    )
    @APIResponses({
        @APIResponse(responseCode = "202", description = "Data transfer job started successfully"),
        @APIResponse(responseCode = "409", description = "Another data transfer job is running")
    })
    public Response transferData() {
        if (jobManager.isAnyJobRunning()) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Another job is already running. Only one job can run at a time.");
            error.put("currentJobId", jobManager.getCurrentRunningJobId());
            return Response.status(409).entity(error).build();
        }

        String jobId = jobManager.startJob("transferdata", () -> {
            try {
                performDataTransfer();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Map<String, String> result = new HashMap<>();
        result.put("status", "started");
        result.put("jobId", jobId);
        result.put("message", "transfer data started");
        return Response.accepted(result).build();
    }

    public void performDataTransfer() throws Exception {
        boolean doData = configurationService.isDoData();
        
        if (doData) {
            String oraclePasswordForExport = configurationService.getOraclePassword();
            String postgresPassword = configurationService.getPostgrePassword();
            
            if ("xxx".equals(oraclePasswordForExport)) {
                throw new RuntimeException("Oracle password not configured - please update configuration with real password before data export");
            }
            if ("xxx".equals(postgresPassword)) {
                throw new RuntimeException("PostgreSQL password not configured - please update configuration with real password before data transfer");
            }
            
            log.info("Starting data transfer for {} tables using new streaming approach", data.getTableSql().size());
            
            try (Connection oracleConn = DriverManager.getConnection(
                    configurationService.getOracleUrl(),
                    configurationService.getOracleUser(),
                    oraclePasswordForExport);
                 Connection postgresConn = DriverManager.getConnection(
                    configurationService.getPostgreUrl(),
                    configurationService.getPostgreUsername(),
                    postgresPassword)) {
                
                // Use new DataTransferService for direct data transfer
                DataTransferService transferService = new DataTransferService(true); // Enable fallback
                DataTransferService.DataTransferResults results = transferService.transferTables(
                    data.getTableSql(), oracleConn, postgresConn, data, config);
                
                log.info("Data transfer completed: {}", results.getSummary());
                
                // Log detailed results
                for (var result : results.getTableResults()) {
                    if (result.isSuccessful()) {
                        log.info("SUCCESS: {}", result.toString());
                    } else {
                        log.error("FAILED: {}", result.toString());
                    }
                }
                
                // Report any failed tables
                if (!results.getFailedTables().isEmpty()) {
                    log.warn("Failed to transfer {} tables - check logs for details", 
                        results.getFailedTables().size());
                }
                
            } catch (Exception e) {
                log.error("Data transfer failed with database connection error", e);
                throw e;
            }
        }
    }

    public void performDataTransferWithProgress(String jobId) throws Exception {
        // Check if job was cancelled before starting
        if (progressService.isJobCancelled(jobId)) {
            log.info("Job {} was cancelled before data transfer started", jobId);
            return;
        }
        
        boolean doData = configurationService.isDoData();
        
        if (doData) {
            String oraclePasswordForExport = configurationService.getOraclePassword();
            String postgresPassword = configurationService.getPostgrePassword();
            
            if ("xxx".equals(oraclePasswordForExport)) {
                throw new RuntimeException("Oracle password not configured - please update configuration with real password before data export");
            }
            if ("xxx".equals(postgresPassword)) {
                throw new RuntimeException("PostgreSQL password not configured - please update configuration with real password before data transfer");
            }
            
            log.info("Starting data transfer for {} tables with integrated progress tracking", data.getTableSql().size());
            progressService.updateSubStepProgress(jobId, MigrationStep.TRANSFERDATA, 0, 
                "Starting data transfer for " + data.getTableSql().size() + " tables");
            
            try (Connection oracleConn = DriverManager.getConnection(
                    configurationService.getOracleUrl(),
                    configurationService.getOracleUser(),
                    oraclePasswordForExport);
                 Connection postgresConn = DriverManager.getConnection(
                    configurationService.getPostgreUrl(),
                    configurationService.getPostgreUsername(),
                    postgresPassword)) {
                
                // Create DataTransferService with progress callback integration
                DataTransferService transferService = new DataTransferService(true);
                
                // Track total rows transferred for enhanced progress reporting
                final long[] totalRowsTransferred = {0};
                
                // Create progress callback that integrates with our job progress system
                DataTransferService.ProgressCallback progressCallback = new DataTransferService.ProgressCallback() {
                    @Override
                    public void onTableStart(int tableIndex, int totalTables, String tableName) {
                        progressService.updateDynamicDataTransferProgress(jobId, tableIndex, totalTables, 
                            tableName, true, totalRowsTransferred[0]);
                    }
                    
                    @Override
                    public void onTableComplete(int tableIndex, int totalTables, String tableName, 
                                              boolean success, long rowsTransferred) {
                        totalRowsTransferred[0] += rowsTransferred;
                        progressService.updateDynamicDataTransferProgress(jobId, tableIndex, totalTables, 
                            tableName, false, totalRowsTransferred[0]);
                    }
                };
                
                // Execute transfer with dynamic progress tracking
                DataTransferService.DataTransferResults results = transferService.transferTables(
                    data.getTableSql(), oracleConn, postgresConn, data, config, progressCallback);
                
                log.info("Data transfer completed: {}", results.getSummary());
                
                // Log detailed results
                for (var result : results.getTableResults()) {
                    if (result.isSuccessful()) {
                        log.info("SUCCESS: {}", result.toString());
                    } else {
                        log.error("FAILED: {}", result.toString());
                    }
                }
                
                // Report any failed tables
                if (!results.getFailedTables().isEmpty()) {
                    log.warn("Failed to transfer {} tables - check logs for details", 
                        results.getFailedTables().size());
                }
                
            } catch (Exception e) {
                log.error("Data transfer failed with database connection error", e);
                throw e;
            }
        } else {
            // Data transfer is disabled
            progressService.updateSubStepProgress(jobId, MigrationStep.TRANSFERDATA, 
                MigrationStep.TRANSFERDATA.getSubStepCount(), "Data transfer skipped (disabled in configuration)");
        }
    }
}