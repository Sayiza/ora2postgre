package me.christianrobert.ora2postgre.controllers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.postgre.PostgresStatsService;
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
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Path("/migration")
@ApplicationScoped
@Tag(name = "Status & Monitoring", description = "System status and monitoring operations")
public class StatusController {

    private static final Logger log = LoggerFactory.getLogger(StatusController.class);

    @Inject
    Everything data;
    
    @Inject
    PostgresStatsService postgresStatsService;
    
    @Inject
    ConfigurationService configurationService;

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "📊 Get Current Migration Status & Statistics",
        description = "Returns comprehensive statistics about extracted and parsed database objects including tables, views, packages, and total row counts. Provides instant feedback without triggering processing."
    )
    @APIResponse(responseCode = "200", description = "Current migration statistics")
    public Response getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("schemas", data.getUserNames().size());
        status.put("tables", data.getTableSql().size());
        status.put("views", data.getViewDefinition().size());
        status.put("synonyms", data.getSynonyms().size());
        status.put("objectTypeSpecs", data.getObjectTypeSpecPlsql().size());
        status.put("objectTypeBodies", data.getObjectTypeBodyPlsql().size());
        status.put("packageSpecs", data.getPackageSpecPlsql().size());
        status.put("packageBodies", data.getPackageBodyPlsql().size());
        status.put("triggers", data.getTriggerPlsql().size());
        status.put("parsedViews", data.getViewSpecAndQueries().size());
        status.put("parsedObjectTypes", data.getObjectTypeSpecAst().size());
        status.put("parsedPackages", data.getPackageSpecAst().size());
        status.put("parsedTriggers", data.getTriggerAst().size());
        status.put("totalRowCount", data.getTotalRowCount());
        return Response.ok(status).build();
    }

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "🏥 Get System Health Status",
        description = "Checks connectivity to both Oracle and PostgreSQL databases and returns detailed connection status information including any connection errors."
    )
    @APIResponse(responseCode = "200", description = "Health status retrieved successfully")
    public Response getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        
        // Check Oracle connection
        Map<String, Object> oracleHealth = new HashMap<>();
        String oraclePassword = configurationService.getOraclePassword();
        
        if ("xxx".equals(oraclePassword)) {
            oracleHealth.put("connected", false);
            oracleHealth.put("error", "Password not configured (placeholder value)");
            oracleHealth.put("url", configurationService.getOracleUrl());
            oracleHealth.put("user", configurationService.getOracleUser());
        } else {
            try {
                Connection oracleConn = DriverManager.getConnection(
                    configurationService.getOracleUrl(), 
                    configurationService.getOracleUser(), 
                    oraclePassword);
                if (oracleConn != null && !oracleConn.isClosed()) {
                    oracleHealth.put("connected", true);
                    oracleHealth.put("url", configurationService.getOracleUrl());
                    oracleHealth.put("user", configurationService.getOracleUser());
                    oracleConn.close();
                } else {
                    oracleHealth.put("connected", false);
                }
            } catch (Exception e) {
                oracleHealth.put("connected", false);
                oracleHealth.put("error", e.getMessage());
            }
        }
        health.put("oracle", oracleHealth);
        
        // Check PostgreSQL connection
        Map<String, Object> postgresHealth = new HashMap<>();
        String postgresPassword = configurationService.getPostgrePassword();
        
        if ("xxx".equals(postgresPassword)) {
            postgresHealth.put("connected", false);
            postgresHealth.put("error", "Password not configured (placeholder value)");
            postgresHealth.put("url", configurationService.getPostgreUrl());
            postgresHealth.put("user", configurationService.getPostgreUsername());
        } else {
            try {
                Connection postgresConn = DriverManager.getConnection(
                    configurationService.getPostgreUrl(), 
                    configurationService.getPostgreUsername(), 
                    postgresPassword);
                if (postgresConn != null && !postgresConn.isClosed()) {
                    postgresHealth.put("connected", true);
                    postgresHealth.put("url", configurationService.getPostgreUrl());
                    postgresHealth.put("user", configurationService.getPostgreUsername());
                    postgresConn.close();
                } else {
                    postgresHealth.put("connected", false);
                }
            } catch (Exception e) {
                postgresHealth.put("connected", false);
                postgresHealth.put("error", e.getMessage());
            }
        }
        health.put("postgres", postgresHealth);
        
        return Response.ok(health).build();
    }

    @GET
    @Path("/logs")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(
        summary = "📋 Get Recent Application Logs",
        description = "Returns the most recent application log entries for debugging and monitoring purposes. Default returns last 100 lines."
    )
    @APIResponse(responseCode = "200", description = "Log entries retrieved successfully")
    public Response getLogs(@QueryParam("lines") @DefaultValue("100") int lines) {
        try {
            java.nio.file.Path logFile = Paths.get("logs/migration.log");
            
            if (!Files.exists(logFile)) {
                log.info("Log file does not exist yet, creating initial log entry");
                Files.createDirectories(logFile.getParent());
                return Response.ok("Log file not created yet. Starting up migration service...\n" +
                                  "Timestamp: " + java.time.LocalDateTime.now() + "\n").build();
            }
            
            List<String> allLines = Files.readAllLines(logFile);
            
            // Get the last N lines
            int start = Math.max(0, allLines.size() - lines);
            List<String> recentLines = allLines.subList(start, allLines.size());
            
            String logs = recentLines.stream().collect(Collectors.joining("\n"));
            
            if (logs.isEmpty()) {
                logs = "No log entries available yet.\nTimestamp: " + java.time.LocalDateTime.now();
            }
            
            return Response.ok(logs).build();
        } catch (Exception e) {
            log.error("Error fetching logs", e);
            return Response.serverError()
                .entity("Error fetching logs: " + e.getMessage())
                .build();
        }
    }

    @GET
    @Path("/target-stats")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "🎯 Get Target Database Statistics",
        description = "Returns comprehensive statistics about the target PostgreSQL database including object counts, schema information, and migration progress indicators."
    )
    @APIResponse(responseCode = "200", description = "Target database statistics retrieved successfully")
    public Response getTargetDatabaseStats() {
        try {
            String postgresPassword = configurationService.getPostgrePassword();
            if ("xxx".equals(postgresPassword)) {
                Map<String, String> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "PostgreSQL password not configured - please update configuration with real password");
                return Response.status(400).entity(error).build();
            }
            
            try (Connection postgresConn = DriverManager.getConnection(
                configurationService.getPostgreUrl(), 
                configurationService.getPostgreUsername(), 
                postgresPassword)) {
                
                Map<String, Object> stats = postgresStatsService.getTargetDatabaseStats(postgresConn);
                Map<String, Object> result = new HashMap<>();
                result.put("status", "success");
                result.put("stats", stats);
                result.put("fetchedAt", System.currentTimeMillis());
                return Response.ok(result).build();
                
            } catch (SQLException e) {
                log.error("Database error getting target database stats", e);
                Map<String, String> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Database connection failed: " + e.getMessage());
                return Response.status(500).entity(error).build();
            }
        } catch (Exception e) {
            log.error("Error getting target database stats", e);
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to get target database stats: " + e.getMessage());
            return Response.status(500).entity(error).build();
        }
    }
}