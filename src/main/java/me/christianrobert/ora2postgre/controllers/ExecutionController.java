package me.christianrobert.ora2postgre.controllers;

import me.christianrobert.ora2postgre.postgre.PostgresExecuter;
import me.christianrobert.ora2postgre.postgre.PostgresExecuter.ExecutionPhase;
import me.christianrobert.ora2postgre.jobs.JobManager;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Path("/migration")
@ApplicationScoped
@Tag(name = "SQL Execution", description = "PostgreSQL DDL execution operations")
public class ExecutionController {

    private static final Logger log = LoggerFactory.getLogger(ExecutionController.class);

    @Inject
    JobManager jobManager;
    
    @Inject
    ConfigurationService configurationService;

    @POST
    @Path("/execute-pre")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Execute Pre-Transfer SQL (Schema & Tables)",
        description = "Phase 4A: Executes generated PostgreSQL DDL to create schemas, tables, functions, and basic constraints. Prepares database structure for data transfer."
    )
    @APIResponses({
        @APIResponse(responseCode = "202", description = "Pre-transfer execution started successfully"),
        @APIResponse(responseCode = "409", description = "Another pre-transfer execution is running")
    })
    public Response executePreTransferSQL() {
        if (jobManager.isAnyJobRunning()) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Another job is already running. Only one job can run at a time.");
            error.put("currentJobId", jobManager.getCurrentRunningJobId());
            return Response.status(409).entity(error).build();
        }
        
        String jobId = jobManager.startJob("execute-pre", () -> {
            try {
                performPreExecution();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        Map<String, String> result = new HashMap<>();
        result.put("status", "started");
        result.put("jobId", jobId);
        result.put("message", "Pre-transfer SQL execution started (schema and tables)");
        return Response.accepted(result).build();
    }

    @POST
    @Path("/execute-post")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Execute Post-Transfer SQL (Views, Constraints & Triggers)",
        description = "Phase 4B: Executes remaining PostgreSQL DDL after data transfer in dependency order: views/packages → constraints → triggers to finalize database structure."
    )
    @APIResponses({
        @APIResponse(responseCode = "202", description = "Post-transfer execution started successfully"),
        @APIResponse(responseCode = "409", description = "Another post-transfer execution is running")
    })
    public Response executePostTransferSQL() {
        if (jobManager.isAnyJobRunning()) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Another job is already running. Only one job can run at a time.");
            error.put("currentJobId", jobManager.getCurrentRunningJobId());
            return Response.status(409).entity(error).build();
        }
        
        String jobId = jobManager.startJob("execute-post", () -> {
            try {
                performPostExecution();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        Map<String, String> result = new HashMap<>();
        result.put("status", "started");
        result.put("jobId", jobId);
        result.put("message", "Post-transfer SQL execution started (constraints and other objects)");
        return Response.accepted(result).build();
    }

    public void performPreExecution() throws Exception {
        boolean doExecutePostgreFiles = configurationService.isDoExecutePostgreFiles();

        if (doExecutePostgreFiles) {
            String postgresPassword = configurationService.getPostgrePassword();
            if ("xxx".equals(postgresPassword)) {
                throw new RuntimeException("PostgreSQL password not configured - please update configuration with real password before execution");
            }
            String path = configurationService.getPathTargetProjectRoot() + configurationService.getPathTargetProjectPostgre();
            try (Connection postgresConn = DriverManager.getConnection(
                configurationService.getPostgreUrl(), 
                configurationService.getPostgreUsername(), 
                postgresPassword)) {
                PostgresExecuter.executeAllSqlFiles(
                        path,
                        postgresConn,
                        new ArrayList<>(), 
                        new ArrayList<>(),
                        ExecutionPhase.PRE_TRANSFER_TYPES
                );
                PostgresExecuter.executeAllSqlFiles(
                        path,
                        postgresConn,
                        new ArrayList<>(),
                        new ArrayList<>(),
                        ExecutionPhase.PRE_TRANSFER_TABLES
                );
                
                log.info("Pre-transfer SQL execution completed successfully (schema and tables)");
            }
        }
    }

    public void performPostExecution() throws Exception {
        boolean doExecutePostgreFiles = configurationService.isDoExecutePostgreFiles();

        if (doExecutePostgreFiles) {
            String postgresPassword = configurationService.getPostgrePassword();
            if ("xxx".equals(postgresPassword)) {
                throw new RuntimeException("PostgreSQL password not configured - please update configuration with real password before execution");
            }
            String path = configurationService.getPathTargetProjectRoot() + configurationService.getPathTargetProjectPostgre();
            try (Connection postgresConn = DriverManager.getConnection(
                configurationService.getPostgreUrl(), 
                configurationService.getPostgreUsername(), 
                postgresPassword)) {
                PostgresExecuter.executeAllSqlFiles(
                        path,
                        postgresConn,
                        new ArrayList<>(), 
                        new ArrayList<>(),
                        ExecutionPhase.POST_TRANSFER
                );
                
                log.info("Post-transfer SQL execution completed successfully (views and packages)");
                
                // Execute constraints after basic objects are created but before triggers
                try {
                    PostgresExecuter.executeAllSqlFiles(
                            path,
                            postgresConn,
                            new ArrayList<>(), 
                            new ArrayList<>(),
                            ExecutionPhase.POST_TRANSFER_CONSTRAINTS
                    );
                    
                    log.info("Constraint execution completed successfully (foreign keys and advanced constraints)");
                } catch (Exception constraintException) {
                    log.error("Constraint execution failed - some constraints may not have been created", constraintException);
                    log.warn("Continuing with migration despite constraint errors - constraints can be created manually later");
                    // Don't re-throw the exception - allow migration to continue
                }
                
                // Execute triggers after all other objects are created
                try {
                    PostgresExecuter.executeAllSqlFiles(
                            path,
                            postgresConn,
                            new ArrayList<>(), 
                            new ArrayList<>(),
                            ExecutionPhase.POST_TRANSFER_TRIGGERS
                    );
                    
                    log.info("Trigger execution completed successfully (functions and definitions)");
                } catch (Exception triggerException) {
                    log.error("Trigger execution failed - some triggers may not have been created", triggerException);
                    log.warn("Continuing with migration despite trigger errors - triggers can be created manually later");
                    // Don't re-throw the exception - allow migration to continue
                }
            }
        }
    }
}