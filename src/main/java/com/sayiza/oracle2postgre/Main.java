package com.sayiza.oracle2postgre;

import com.sayiza.oracle2postgre.global.Everything;
import com.sayiza.oracle2postgre.global.Config;
import com.sayiza.oracle2postgre.oracledb.ObjectTypeExtractor;
import com.sayiza.oracle2postgre.oracledb.PackageExtractor;
import com.sayiza.oracle2postgre.oracledb.RowCountExtractor;
import com.sayiza.oracle2postgre.oracledb.SchemaExtractor;
import com.sayiza.oracle2postgre.oracledb.SynonymExtractor;
import com.sayiza.oracle2postgre.oracledb.TableExtractor;
import com.sayiza.oracle2postgre.oracledb.ViewExtractor;
import com.sayiza.oracle2postgre.oracledb.ViewMetadata;
import com.sayiza.oracle2postgre.plsql.PlSqlAstMain;
import com.sayiza.oracle2postgre.plsql.ast.ObjectType;
import com.sayiza.oracle2postgre.plsql.ast.OraclePackage;
import com.sayiza.oracle2postgre.plsql.ast.SelectStatement;
import com.sayiza.oracle2postgre.global.PlsqlCode;
import com.sayiza.oracle2postgre.global.ViewSpecAndQuery;
import com.sayiza.oracle2postgre.postgre.PostgresExecuter;
import com.sayiza.oracle2postgre.postgre.PostgresExecuter.ExecutionPhase;
import com.sayiza.oracle2postgre.postgre.PostgresStatsService;
import com.sayiza.oracle2postgre.transfer.DataTransferService;
import com.sayiza.oracle2postgre.writing.ExportObjectType;
import com.sayiza.oracle2postgre.writing.ExportPackage;
import com.sayiza.oracle2postgre.writing.ExportProjectPostgre;
import com.sayiza.oracle2postgre.writing.ExportRestControllers;
import com.sayiza.oracle2postgre.writing.ExportSchema;
import com.sayiza.oracle2postgre.writing.ExportTable;
import com.sayiza.oracle2postgre.writing.ExportView;
import com.sayiza.oracle2postgre.jobs.JobManager;
import com.sayiza.oracle2postgre.jobs.JobStatus;
import com.sayiza.oracle2postgre.jobs.MigrationProgressService;
import com.sayiza.oracle2postgre.jobs.MigrationStep;
import com.sayiza.oracle2postgre.config.ConfigurationService;
import com.sayiza.oracle2postgre.config.RuntimeConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.Arrays;

@Path("/migration")
@ApplicationScoped
public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  @Inject
  Everything data;
  
  @Inject
  Config config;
  
  @Inject
  JobManager jobManager;
  
  @Inject
  ConfigurationService configurationService;
  
  @Inject
  PostgresStatsService postgresStatsService;
  
  @Inject
  MigrationProgressService progressService;
  
  @POST
  @Path("/extract")
  @Produces(MediaType.APPLICATION_JSON)
  public Response extractData() {
    log.info("Extract data endpoint called");
    
    if (jobManager.isJobRunning("extract")) {
      log.warn("Extract job is already running, rejecting request");
      Map<String, String> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "Extract job is already running");
      return Response.status(409).entity(error).build();
    }
    
    String jobId = jobManager.startJob("extract", () -> {
      try {
        log.info("Starting data extraction job");
        performExtraction();
        log.info("Data extraction job completed successfully");
      } catch (Exception e) {
        log.error("Data extraction job failed", e);
        throw new RuntimeException(e);
      }
    });
    
    Map<String, String> result = new HashMap<>();
    result.put("status", "started");
    result.put("jobId", jobId);
    result.put("message", "Data extraction started");
    log.info("Extract job started with ID: {}", jobId);
    return Response.accepted(result).build();
  }
  
  @POST
  @Path("/parse")
  @Produces(MediaType.APPLICATION_JSON)
  public Response parseData() {
    if (jobManager.isJobRunning("parse")) {
      Map<String, String> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "Parse job is already running");
      return Response.status(409).entity(error).build();
    }
    
    String jobId = jobManager.startJob("parse", () -> {
      try {
        performParsing();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    
    Map<String, String> result = new HashMap<>();
    result.put("status", "started");
    result.put("jobId", jobId);
    result.put("message", "AST parsing started");
    return Response.accepted(result).build();
  }
  
  @POST
  @Path("/export")
  @Produces(MediaType.APPLICATION_JSON)
  public Response exportFiles() {
    if (jobManager.isJobRunning("export")) {
      Map<String, String> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "Export job is already running");
      return Response.status(409).entity(error).build();
    }
    
    String jobId = jobManager.startJob("export", () -> {
      try {
        performExport();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    
    Map<String, String> result = new HashMap<>();
    result.put("status", "started");
    result.put("jobId", jobId);
    result.put("message", "File export started");
    return Response.accepted(result).build();
  }
  
  @POST
  @Path("/execute-pre")
  @Produces(MediaType.APPLICATION_JSON)
  public Response executePreTransferSQL() {
    if (jobManager.isJobRunning("execute-pre")) {
      Map<String, String> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "Pre-transfer execute job is already running");
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
  public Response executePostTransferSQL() {
    if (jobManager.isJobRunning("execute-post")) {
      Map<String, String> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "Post-transfer execute job is already running");
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

  @POST
  @Path("/transferdata")
  @Produces(MediaType.APPLICATION_JSON)
  public Response transferData() {
    if (jobManager.isJobRunning("transferdata")) {
      Map<String, String> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "Execute job is already running");
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

  @POST
  @Path("/full")
  @Produces(MediaType.APPLICATION_JSON)
  public Response runFullMigration() {
    log.info("Full migration endpoint called");
    
    if (jobManager.isJobRunning("full")) {
      log.warn("Full migration job is already running, rejecting request");
      Map<String, String> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "Full migration job is already running");
      return Response.status(409).entity(error).build();
    }
    
    String jobId = jobManager.startJobWithId("full", (String currentJobId) -> {
      try {
        log.info("Starting full migration job with progress tracking");
        
        // Initialize progress tracking
        progressService.initializeJobProgress(currentJobId, MigrationStep.EXTRACT);
        
        // Phase 1: Extract
        log.info("Phase 1: Starting data extraction");
        progressService.advanceToNextStep(currentJobId, MigrationStep.EXTRACT);
        performExtractionWithProgress(currentJobId);
        
        // Phase 2: Parse  
        log.info("Phase 2: Starting AST parsing");
        progressService.advanceToNextStep(currentJobId, MigrationStep.PARSE);
        performParsingWithProgress(currentJobId);
        
        // Phase 3: Export
        log.info("Phase 3: Starting file export");
        progressService.advanceToNextStep(currentJobId, MigrationStep.EXPORT);
        performExportWithProgress(currentJobId);
        
        // Phase 4: Execute Pre
        log.info("Phase 4: Starting pre-transfer SQL execution (schema and tables)");
        progressService.advanceToNextStep(currentJobId, MigrationStep.EXECUTE_PRE);
        performPreExecutionWithProgress(currentJobId);
        
        // Phase 5: Transfer Data
        log.info("Phase 5: Starting data transfer");
        progressService.advanceToNextStep(currentJobId, MigrationStep.TRANSFERDATA);
        performDataTransferWithProgress(currentJobId);
        
        // Phase 6: Execute Post
        log.info("Phase 6: Starting post-transfer SQL execution (constraints and other objects)");
        progressService.advanceToNextStep(currentJobId, MigrationStep.EXECUTE_POST);
        performPostExecutionWithProgress(currentJobId);
        
        // Complete the job
        progressService.completeJob(currentJobId);
        log.info("Full migration job completed successfully");
      } catch (Exception e) {
        log.error("Full migration job failed", e);
        throw new RuntimeException(e);
      }
    });
    
    Map<String, String> result = new HashMap<>();
    result.put("status", "started");
    result.put("jobId", jobId);
    result.put("message", "Full migration started");
    log.info("Full migration job started with ID: {}", jobId);
    return Response.accepted(result).build();
  }
  
  @GET
  @Path("/status")
  @Produces(MediaType.APPLICATION_JSON)
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
    status.put("parsedViews", data.getViewSpecAndQueries().size());
    status.put("parsedObjectTypes", data.getObjectTypeSpecAst().size());
    status.put("parsedPackages", data.getPackageSpecAst().size());
    status.put("totalRowCount", data.getTotalRowCount());
    return Response.ok(status).build();
  }
  
  @GET
  @Path("/jobs/{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJobStatus(@PathParam("jobId") String jobId) {
    JobStatus status = jobManager.getJobStatus(jobId);
    if (status == null) {
      Map<String, String> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "Job not found");
      return Response.status(404).entity(error).build();
    }
    return Response.ok(status).build();
  }
  
  @GET
  @Path("/jobs/{jobId}/progress")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJobProgress(@PathParam("jobId") String jobId) {
    MigrationProgressService.JobProgressInfo progressInfo = progressService.getDetailedProgress(jobId);
    if (progressInfo == null) {
      Map<String, String> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "Job not found");
      return Response.status(404).entity(error).build();
    }
    return Response.ok(progressInfo).build();
  }
  
  @GET
  @Path("/jobs")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllJobs() {
    return Response.ok(jobManager.getAllJobs()).build();
  }
  
  @DELETE
  @Path("/jobs/completed")
  @Produces(MediaType.APPLICATION_JSON)
  public Response clearCompletedJobs() {
    jobManager.clearCompletedJobs();
    Map<String, String> result = new HashMap<>();
    result.put("status", "success");
    result.put("message", "Completed jobs cleared");
    return Response.ok(result).build();
  }
  
  @POST
  @Path("/reset")
  @Produces(MediaType.APPLICATION_JSON)
  public Response resetEverything() {
    // Clear all data in the Everything singleton
    data.getUserNames().clear();
    data.getTableSql().clear();
    data.getViewDefinition().clear();
    data.getSynonyms().clear();
    data.getObjectTypeSpecPlsql().clear();
    data.getObjectTypeBodyPlsql().clear();
    data.getPackageSpecPlsql().clear();
    data.getPackageBodyPlsql().clear();
    data.getViewSpecAndQueries().clear();
    data.getObjectTypeSpecAst().clear();
    data.getObjectTypeBodyAst().clear();
    data.getPackageSpecAst().clear();
    data.getPackageBodyAst().clear();
    data.setTotalRowCount(0);
    
    Map<String, String> result = new HashMap<>();
    result.put("status", "success");
    result.put("message", "Everything data has been reset");
    return Response.ok(result).build();
  }
  
  @GET
  @Path("/health")
  @Produces(MediaType.APPLICATION_JSON)
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
  
  @POST
  @Path("/transform/sql")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response transformSql(Map<String, String> request) {
    try {
      String schema = request.get("schema");
      String sql = request.get("sql");
      
      if (schema == null || schema.trim().isEmpty()) {
        Map<String, String> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", "schema parameter is required");
        return Response.status(400).entity(error).build();
      }
      
      if (sql == null || sql.trim().isEmpty()) {
        Map<String, String> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", "sql parameter is required");
        return Response.status(400).entity(error).build();
      }
      
      // Use the existing transformation pipeline with full Everything context
      SelectStatement ast = (SelectStatement) PlSqlAstMain.processPlsqlCode(
          new PlsqlCode(schema, sql));
      String transformedSql = ast.toPostgre(data);
      
      Map<String, String> result = new HashMap<>();
      result.put("status", "success");
      result.put("originalSql", sql);
      result.put("transformedSql", transformedSql);
      result.put("schema", schema);
      return Response.ok(result).build();
      
    } catch (Exception e) {
      Map<String, String> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "SQL transformation failed: " + e.getMessage());
      error.put("errorType", e.getClass().getSimpleName());
      return Response.status(500).entity(error).build();
    }
  }
  
  @GET
  @Path("/config")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getConfiguration() {
    try {
      RuntimeConfiguration currentConfig = configurationService.getCurrentConfiguration();
      return Response.ok(currentConfig).build();
    } catch (Exception e) {
      log.error("Error getting configuration", e);
      Map<String, String> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "Failed to get configuration: " + e.getMessage());
      return Response.status(500).entity(error).build();
    }
  }
  
  @PUT
  @Path("/config")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateConfiguration(RuntimeConfiguration updates) {
    try {
      configurationService.updateConfiguration(updates);
      RuntimeConfiguration updatedConfig = configurationService.getCurrentConfiguration();
      
      Map<String, Object> result = new HashMap<>();
      result.put("status", "success");
      result.put("message", "Configuration updated successfully");
      result.put("config", updatedConfig);
      
      log.info("Configuration updated successfully");
      return Response.ok(result).build();
    } catch (Exception e) {
      log.error("Error updating configuration", e);
      Map<String, String> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "Failed to update configuration: " + e.getMessage());
      return Response.status(500).entity(error).build();
    }
  }
  
  @GET
  @Path("/target-stats")
  @Produces(MediaType.APPLICATION_JSON)
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
  
  private void performExtraction() throws Exception {
    List<String> doOnlySomeSchema = Arrays.stream(configurationService.getDoOnlyTestSchema().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    boolean doAllSchema = configurationService.isDoAllSchemas();
    boolean doAddTestData = config.isDoAddTestData();
    boolean doTable = configurationService.isDoTable();
    boolean doSynonyms = configurationService.isDoSynonyms();
    boolean doObjectTypeSpec = configurationService.isDoObjectTypeSpec();
    boolean doObjectTypeBody = configurationService.isDoObjectTypeBody();
    boolean doPackageSpec = configurationService.isDoPackageSpec();
    boolean doPackageBody = configurationService.isDoPackageBody();
    boolean doViewSignature = configurationService.isDoViewSignature();

    if (doAddTestData) {
      // depricated
    }

    String oraclePassword = configurationService.getOraclePassword();
    if ("xxx".equals(oraclePassword)) {
      throw new RuntimeException("Oracle password not configured - please update configuration with real password before extraction");
    }

    try (Connection conn = DriverManager.getConnection(
        configurationService.getOracleUrl(), 
        configurationService.getOracleUser(), 
        oraclePassword)) {
        
      if (doOnlySomeSchema.isEmpty() || doAllSchema) {
        data.getUserNames().addAll(SchemaExtractor.fetchUsernames(conn));
      } else {
        data.getUserNames().addAll(doOnlySomeSchema);
      }
      if (doTable) {
        data.getTableSql().addAll(TableExtractor.extractAllTables(conn, data.getUserNames()));
      }
      if (doViewSignature) {
        data.getViewDefinition().addAll(ViewExtractor.extractAllViews(conn, data.getUserNames()));
      }
      if (doSynonyms) {
        data.getSynonyms().addAll(SynonymExtractor.extractAllSynonyms(conn, data.getUserNames()));
      }
      if (doObjectTypeSpec) {
        data.getObjectTypeSpecPlsql().addAll(ObjectTypeExtractor.extract(conn, data.getUserNames(), true));
      }
      if (doObjectTypeBody) {
        data.getObjectTypeBodyPlsql().addAll(ObjectTypeExtractor.extract(conn, data.getUserNames(), false));
      }
      if (doPackageSpec) {
        data.getPackageSpecPlsql().addAll(PackageExtractor.extract(conn, data.getUserNames(), true));
      }
      if (doPackageBody) {
        data.getPackageBodyPlsql().addAll(PackageExtractor.extract(conn, data.getUserNames(), false));
      }
      
      // Calculate total row count for the extracted schemas
      log.info("Calculating total row count for extracted schemas");
      long totalRowCount = RowCountExtractor.calculateTotalRowCount(conn, doAllSchema, data.getUserNames());
      data.setTotalRowCount(totalRowCount);
      
      // Debug logging for extracted data
      log.info("Extraction completed: {} schemas, {} tables, {} object type specs, {} package specs",
          data.getUserNames().size(), data.getTableSql().size(), 
          data.getObjectTypeSpecPlsql().size(), data.getPackageSpecPlsql().size());
    }
  }
  
  private void performExtractionWithProgress(String jobId) throws Exception {
    List<String> doOnlySomeSchema = Arrays.stream(configurationService.getDoOnlyTestSchema().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    boolean doAllSchema = configurationService.isDoAllSchemas();
    boolean doAddTestData = config.isDoAddTestData();
    boolean doTable = configurationService.isDoTable();
    boolean doSynonyms = configurationService.isDoSynonyms();
    boolean doObjectTypeSpec = configurationService.isDoObjectTypeSpec();
    boolean doObjectTypeBody = configurationService.isDoObjectTypeBody();
    boolean doPackageSpec = configurationService.isDoPackageSpec();
    boolean doPackageBody = configurationService.isDoPackageBody();
    boolean doViewSignature = configurationService.isDoViewSignature();

    if (doAddTestData) {
      // deprecated
    }

    String oraclePassword = configurationService.getOraclePassword();
    if ("xxx".equals(oraclePassword)) {
      throw new RuntimeException("Oracle password not configured - please update configuration with real password before extraction");
    }

    try (Connection conn = DriverManager.getConnection(
        configurationService.getOracleUrl(), 
        configurationService.getOracleUser(), 
        oraclePassword)) {
        
      int completedSubSteps = 0;
      int totalSubSteps = MigrationStep.EXTRACT.getSubStepCount();
      
      // Sub-step 1: Extract user schemas
      progressService.updateSubStepProgress(jobId, MigrationStep.EXTRACT, completedSubSteps, "Extracting user schemas");
      if (doOnlySomeSchema.isEmpty() || doAllSchema) {
        data.getUserNames().addAll(SchemaExtractor.fetchUsernames(conn));
      } else {
        data.getUserNames().addAll(doOnlySomeSchema);
      }
      completedSubSteps++;
      
      // Sub-step 2: Extract table metadata
      progressService.updateSubStepProgress(jobId, MigrationStep.EXTRACT, completedSubSteps, "Extracting table metadata");
      if (doTable) {
        data.getTableSql().addAll(TableExtractor.extractAllTables(conn, data.getUserNames()));
      }
      completedSubSteps++;
      
      // Sub-step 3: Extract view definitions
      progressService.updateSubStepProgress(jobId, MigrationStep.EXTRACT, completedSubSteps, "Extracting view definitions");
      if (doViewSignature) {
        data.getViewDefinition().addAll(ViewExtractor.extractAllViews(conn, data.getUserNames()));
      }
      completedSubSteps++;
      
      // Sub-step 4: Extract synonyms
      progressService.updateSubStepProgress(jobId, MigrationStep.EXTRACT, completedSubSteps, "Extracting synonyms");
      if (doSynonyms) {
        data.getSynonyms().addAll(SynonymExtractor.extractAllSynonyms(conn, data.getUserNames()));
      }
      completedSubSteps++;
      
      // Sub-step 5: Extract object type specs
      progressService.updateSubStepProgress(jobId, MigrationStep.EXTRACT, completedSubSteps, "Extracting object type specs");
      if (doObjectTypeSpec) {
        data.getObjectTypeSpecPlsql().addAll(ObjectTypeExtractor.extract(conn, data.getUserNames(), true));
      }
      completedSubSteps++;
      
      // Sub-step 6: Extract object type bodies
      progressService.updateSubStepProgress(jobId, MigrationStep.EXTRACT, completedSubSteps, "Extracting object type bodies");
      if (doObjectTypeBody) {
        data.getObjectTypeBodyPlsql().addAll(ObjectTypeExtractor.extract(conn, data.getUserNames(), false));
      }
      completedSubSteps++;
      
      // Sub-step 7: Extract package specs
      progressService.updateSubStepProgress(jobId, MigrationStep.EXTRACT, completedSubSteps, "Extracting package specs");
      if (doPackageSpec) {
        data.getPackageSpecPlsql().addAll(PackageExtractor.extract(conn, data.getUserNames(), true));
      }
      completedSubSteps++;
      
      // Sub-step 8: Extract package bodies
      progressService.updateSubStepProgress(jobId, MigrationStep.EXTRACT, completedSubSteps, "Extracting package bodies");
      if (doPackageBody) {
        data.getPackageBodyPlsql().addAll(PackageExtractor.extract(conn, data.getUserNames(), false));
      }
      completedSubSteps++;
      
      // Sub-step 9: Calculate total row counts
      progressService.updateSubStepProgress(jobId, MigrationStep.EXTRACT, completedSubSteps, "Calculating total row count");
      log.info("Calculating total row count for extracted schemas");
      long totalRowCount = RowCountExtractor.calculateTotalRowCount(conn, doAllSchema, data.getUserNames());
      data.setTotalRowCount(totalRowCount);
      completedSubSteps++;
      
      // Complete extraction step
      progressService.updateSubStepProgress(jobId, MigrationStep.EXTRACT, completedSubSteps, "Extraction completed");
      
      // Debug logging for extracted data
      log.info("Extraction completed: {} schemas, {} tables, {} object type specs, {} package specs",
          data.getUserNames().size(), data.getTableSql().size(), 
          data.getObjectTypeSpecPlsql().size(), data.getPackageSpecPlsql().size());
    }
  }
  
  private void performParsingWithProgress(String jobId) throws Exception {
    // Delegate to existing method for now - can be enhanced later with detailed sub-step tracking
    progressService.updateSubStepProgress(jobId, MigrationStep.PARSE, 0, "Starting AST parsing");
    performParsing();
    progressService.updateSubStepProgress(jobId, MigrationStep.PARSE, MigrationStep.PARSE.getSubStepCount(), "AST parsing completed");
  }
  
  private void performExportWithProgress(String jobId) throws Exception {
    // Delegate to existing method for now - can be enhanced later with detailed sub-step tracking
    progressService.updateSubStepProgress(jobId, MigrationStep.EXPORT, 0, "Starting file export");
    performExport();
    progressService.updateSubStepProgress(jobId, MigrationStep.EXPORT, MigrationStep.EXPORT.getSubStepCount(), "File export completed");
  }
  
  private void performPreExecutionWithProgress(String jobId) throws Exception {
    // Delegate to existing method for now - can be enhanced later with detailed sub-step tracking
    progressService.updateSubStepProgress(jobId, MigrationStep.EXECUTE_PRE, 0, "Starting pre-transfer SQL execution");
    performPreExecution();
    progressService.updateSubStepProgress(jobId, MigrationStep.EXECUTE_PRE, MigrationStep.EXECUTE_PRE.getSubStepCount(), "Pre-transfer SQL execution completed");
  }
  
  private void performDataTransferWithProgress(String jobId) throws Exception {
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
  
  private void performPostExecutionWithProgress(String jobId) throws Exception {
    // Delegate to existing method for now - can be enhanced later with detailed sub-step tracking
    progressService.updateSubStepProgress(jobId, MigrationStep.EXECUTE_POST, 0, "Starting post-transfer SQL execution");
    performPostExecution();
    progressService.updateSubStepProgress(jobId, MigrationStep.EXECUTE_POST, MigrationStep.EXECUTE_POST.getSubStepCount(), "Post-transfer SQL execution completed");
  }
  
  private void performParsing() throws Exception {
    boolean doViewDdl = configurationService.isDoViewDdl();
    boolean doObjectTypeSpec = configurationService.isDoObjectTypeSpec();
    boolean doObjectTypeBody = configurationService.isDoObjectTypeBody();
    boolean doPackageSpec = configurationService.isDoPackageSpec();
    boolean doPackageBody = configurationService.isDoPackageBody();

    if (doViewDdl) {
      for (ViewMetadata view : data.getViewDefinition()) {
        data.getViewSpecAndQueries().add(
                new ViewSpecAndQuery(
                        view,
                        (SelectStatement) PlSqlAstMain.processPlsqlCode(
                        new PlsqlCode(view.getSchema(),view.getRawQuery()))));
      }
    }
    if (doObjectTypeSpec) {
      for (PlsqlCode s : data.getObjectTypeSpecPlsql()) {
        data.getObjectTypeSpecAst().add((ObjectType) PlSqlAstMain.processPlsqlCode(s));
      }
    }
    if (doObjectTypeBody) {
      for (PlsqlCode s : data.getObjectTypeBodyPlsql()) {
        data.getObjectTypeBodyAst().add((ObjectType) PlSqlAstMain.processPlsqlCode(s));
      }
    }
    if (doPackageSpec) {
      for (PlsqlCode s : data.getPackageSpecPlsql()) {
        data.getPackageSpecAst().add((OraclePackage) PlSqlAstMain.processPlsqlCode(s));
      }
    }
    if (doPackageBody) {
      for (PlsqlCode s : data.getPackageBodyPlsql()) {
        data.getPackageBodyAst().add((OraclePackage) PlSqlAstMain.processPlsqlCode(s));
      }
    }
    
    // Debug logging for parsed ASTs
    log.info("Parsing completed: {} object type ASTs, {} package spec ASTs, {} package body ASTs",
        data.getObjectTypeSpecAst().size(), data.getPackageSpecAst().size(), data.getPackageBodyAst().size());
    
    // Debug log object type details
    if (!data.getObjectTypeSpecAst().isEmpty()) {
      log.info("Object types found:");
      for (ObjectType objType : data.getObjectTypeSpecAst()) {
        log.info("  - {}.{} with {} variables", objType.getSchema(), objType.getName(), objType.getVariables().size());
      }
    } else {
      log.info("No object types found in AST parsing");
    }
  }
  private void performDataTransfer() throws Exception {
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

  private void performExport() throws Exception {

    // New REST controller generation
    boolean doWriteRestControllers = configurationService.isDoWriteRestControllers();
    
    boolean doWritePostgreFiles = configurationService.isDoWritePostgreFiles();
    boolean doTable = configurationService.isDoTable();
    boolean doViewSignature = configurationService.isDoViewSignature();
    boolean doObjectTypeSpec = configurationService.isDoObjectTypeSpec();
    boolean doObjectTypeBody = configurationService.isDoObjectTypeBody();
    boolean doPackageSpec = configurationService.isDoPackageSpec();
    boolean doPackageBody = configurationService.isDoPackageBody();
    boolean doViewDdl = configurationService.isDoViewDdl();
    
    // NEW: REST controller generation (PostgreSQL-first approach)
    if (doWriteRestControllers) {
      String pathJava = configurationService.getPathTargetProjectRoot() + configurationService.getPathTargetProjectJava();
      String pathResources = configurationService.getPathTargetProjectRoot() + configurationService.getPathTargetProjectResources();
      String javaPackageName = configurationService.getJavaGeneratedPackageName();
      
      // Set up the target project infrastructure (pom.xml, application.properties, etc.)
      ExportRestControllers.setupTargetProject(
          configurationService.getPathTargetProjectRoot(),
          pathJava,
          pathResources,
          javaPackageName,
          configurationService.getPostgreUrl(),
          configurationService.getPostgreUsername(),
          configurationService.getPostgrePassword()
      );
      
      // Generate REST controllers for packages
      if (doPackageSpec && doPackageBody) {
        ExportRestControllers.generateControllers(
            pathJava, 
            javaPackageName, 
            data.getPackageSpecAst(), 
            data.getPackageBodyAst(), 
            data
        );
      }
      
      // TODO: Add ObjectType REST controller generation if needed
      // if (doObjectTypeSpec && doObjectTypeBody) {
      //   ExportRestControllers.generateObjectTypeControllers(...);
      // }
    }
    if (doWritePostgreFiles) {
      String path = configurationService.getPathTargetProjectRoot() + configurationService.getPathTargetProjectPostgre();
      ExportProjectPostgre.save(path);

      ExportSchema.saveSql(path, data.getUserNames());

      if (doTable) {
        ExportTable.saveSql(path, data.getTableSql(), data);
      }
      if (doViewSignature) {
        ExportView.saveEmptyViews(path, data.getViewDefinition());
      }
      if (doObjectTypeSpec) {
        ExportObjectType.saveObjectTypeSpecToPostgre(path, data.getObjectTypeSpecAst(), data.getObjectTypeBodyAst(), data);
      }
      if (doPackageSpec) {
        ExportPackage.savePackageSpecToPostgre(path, data.getPackageSpecAst(), data.getPackageBodyAst(), data);
      }
      if (doViewDdl) {
        ExportView.saveFullViews(path, data.getViewSpecAndQueries(), data);
      }
      if (doObjectTypeBody) {
        ExportObjectType.saveObjectTypeBodyToPostgre(path, data.getObjectTypeSpecAst(), data.getObjectTypeBodyAst(), data);
      }
      if (doPackageBody) {
        ExportPackage.savePackageBodyToPostgre(path, data.getPackageSpecAst(), data.getPackageBodyAst(), data);
      }
    }
  }
  
  private void performPreExecution() throws Exception {
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

  private void performPostExecution() throws Exception {
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
        
        log.info("Post-transfer SQL execution completed successfully (constraints and other objects)");
      }
    }
  }
}