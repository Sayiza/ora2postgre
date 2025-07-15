package me.christianrobert.ora2postgre.controllers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.Config;
import me.christianrobert.ora2postgre.oracledb.ObjectTypeExtractor;
import me.christianrobert.ora2postgre.oracledb.PackageExtractor;
import me.christianrobert.ora2postgre.oracledb.RowCountExtractor;
import me.christianrobert.ora2postgre.oracledb.StandaloneFunctionExtractor;
import me.christianrobert.ora2postgre.oracledb.StandaloneProcedureExtractor;
import me.christianrobert.ora2postgre.oracledb.RowCountConfig;
import me.christianrobert.ora2postgre.oracledb.SchemaExtractor;
import me.christianrobert.ora2postgre.oracledb.SynonymExtractor;
import me.christianrobert.ora2postgre.oracledb.TableExtractor;
import me.christianrobert.ora2postgre.oracledb.ViewExtractor;
import me.christianrobert.ora2postgre.oracledb.TriggerExtractor;
import me.christianrobert.ora2postgre.oracledb.IndexExtractor;
import me.christianrobert.ora2postgre.oracledb.ViewMetadata;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import me.christianrobert.ora2postgre.plsql.ast.Function;
import me.christianrobert.ora2postgre.plsql.ast.ObjectType;
import me.christianrobert.ora2postgre.plsql.ast.OraclePackage;
import me.christianrobert.ora2postgre.plsql.ast.Procedure;
import me.christianrobert.ora2postgre.plsql.ast.SelectStatement;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.global.ViewSpecAndQuery;
import me.christianrobert.ora2postgre.plsql.ast.Statement;
import me.christianrobert.ora2postgre.plsql.ast.Trigger;
import me.christianrobert.ora2postgre.writing.ExportObjectType;
import me.christianrobert.ora2postgre.writing.ExportPackage;
import me.christianrobert.ora2postgre.writing.ExportProjectPostgre;
import me.christianrobert.ora2postgre.writing.ExportStandaloneFunction;
import me.christianrobert.ora2postgre.writing.ExportStandaloneProcedure;
import me.christianrobert.ora2postgre.writing.ExportModPlsqlSimulator;
import me.christianrobert.ora2postgre.writing.ExportSchema;
import me.christianrobert.ora2postgre.writing.ExportTable;
import me.christianrobert.ora2postgre.writing.ExportTrigger;
import me.christianrobert.ora2postgre.writing.ExportView;
import me.christianrobert.ora2postgre.writing.ExportConstraint;
import me.christianrobert.ora2postgre.writing.ExportIndex;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Arrays;

@Path("/migration")
@ApplicationScoped
@Tag(name = "Migration", description = "Core Oracle to PostgreSQL migration pipeline operations")
public class MigrationController {

  private static final Logger log = LoggerFactory.getLogger(MigrationController.class);

  @Inject
  Everything data;

  @Inject
  Config config;

  @Inject
  JobManager jobManager;

  @Inject
  ConfigurationService configurationService;

  @Inject
  RowCountConfig rowCountConfig;

  @Inject
  MigrationProgressService progressService;

  @Inject
  ExecutionController executionController;

  @Inject
  DataTransferController dataTransferController;

  @POST
  @Path("/extract")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Extract Oracle Database Metadata",
          description = "Phase 1: Connects to Oracle database and extracts comprehensive metadata including schemas, tables, views, synonyms, PL/SQL code, and row count statistics. This is the first step in the migration pipeline."
  )
  @APIResponses({
          @APIResponse(responseCode = "202", description = "Extraction job started successfully"),
          @APIResponse(responseCode = "409", description = "Another extraction job is already running")
  })
  public Response extractData() {
    log.info("Extract data endpoint called");

    if (jobManager.isAnyJobRunning()) {
      return Response.status(409).entity(jobManager.getJobError()).build();
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
  @Operation(
          summary = "Parse PL/SQL Code to AST",
          description = "Phase 2: Processes extracted PL/SQL source code using ANTLR4 grammar to generate Abstract Syntax Trees for accurate code transformation. Requires successful extraction first."
  )
  @APIResponses({
          @APIResponse(responseCode = "202", description = "Parsing job started successfully"),
          @APIResponse(responseCode = "409", description = "Another parsing job is already running")
  })
  public Response parseData() {
    if (jobManager.isAnyJobRunning()) {
      return Response.status(409).entity(jobManager.getJobError()).build();
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
  @Operation(
          summary = "Generate PostgreSQL Code & REST Controllers",
          description = "Phase 3: Transforms parsed AST into PostgreSQL functions and minimal JAX-RS REST controllers using PostgreSQL-first architecture. Business logic stays in PostgreSQL, REST controllers are thin API proxies."
  )
  @APIResponses({
          @APIResponse(responseCode = "202", description = "Code generation job started successfully"),
          @APIResponse(responseCode = "409", description = "Another export job is already running")
  })
  public Response exportFiles() {
    if (jobManager.isAnyJobRunning()) {
      return Response.status(409).entity(jobManager.getJobError()).build();
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
  @Path("/full")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "🚀 Execute Complete Migration Pipeline",
          description = "Orchestrates the complete Oracle-to-PostgreSQL migration: Extract → Parse → Export → Execute-Pre → Transfer Data → Execute-Post. Can take hours for large databases. Use /migration/jobs/{jobId} for progress tracking."
  )
  @APIResponses({
          @APIResponse(responseCode = "202", description = "Full migration pipeline started with progress tracking"),
          @APIResponse(responseCode = "409", description = "Another full migration is already running")
  })
  public Response runFullMigration() {
    log.info("Full migration endpoint called");

    if (jobManager.isAnyJobRunning()) {
      return Response.status(409).entity(jobManager.getJobError()).build();
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
        dataTransferController.performDataTransferWithProgress(currentJobId);

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
    boolean doStandaloneFunctions = configurationService.isDoStandaloneFunctions();
    boolean doStandaloneProcedures = configurationService.isDoStandaloneProcedures();
    boolean doTriggers = configurationService.isDoTriggers();
    boolean doIndexes = configurationService.isDoIndexes();

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
      if (doStandaloneFunctions) {
        data.getStandaloneFunctionPlsql().addAll(StandaloneFunctionExtractor.extract(conn, data.getUserNames()));
      }
      if (doStandaloneProcedures) {
        data.getStandaloneProcedurePlsql().addAll(StandaloneProcedureExtractor.extract(conn, data.getUserNames()));
      }
      if (doTriggers) {
        data.getTriggerPlsql().addAll(TriggerExtractor.extract(conn, data.getUserNames()));
      }
      if (doIndexes) {
        data.getIndexes().addAll(IndexExtractor.extractAllIndexes(conn, data.getUserNames()));
      }

      if (configurationService.isDoData()) {
        log.info("Calculating total row count for extracted schemas");
        long totalRowCount = RowCountExtractor.calculateTotalRowCount(conn, doAllSchema, data.getUserNames(), rowCountConfig);
        data.setTotalRowCount(totalRowCount);
      } else {
        data.setTotalRowCount(0);
      }

      log.info("Extraction completed: {} schemas, {} tables, {} object type specs, {} package specs, {} standalone functions, {} standalone procedures, {} triggers, {} indexes",
              data.getUserNames().size(), data.getTableSql().size(),
              data.getObjectTypeSpecPlsql().size(), data.getPackageSpecPlsql().size(), 
              data.getStandaloneFunctionPlsqlCount(), data.getStandaloneProcedurePlsqlCount(),
              data.getTriggerPlsql().size(), data.getIndexes().size());
    }
  }

  private void performExtractionWithProgress(String jobId) throws Exception {
    if (progressService.isJobCancelled(jobId)) {
      log.info("Job {} was cancelled before extraction started", jobId);
      return;
    }

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
    boolean doStandaloneFunctions = configurationService.isDoStandaloneFunctions();
    boolean doStandaloneProcedures = configurationService.isDoStandaloneProcedures();
    boolean doTriggers = configurationService.isDoTriggers();
    boolean doIndexes = configurationService.isDoIndexes();

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

      // Sub-step 9: Extract standalone functions
      progressService.updateSubStepProgress(jobId, MigrationStep.EXTRACT, completedSubSteps, "Extracting standalone functions");
      if (doStandaloneFunctions) {
        data.getStandaloneFunctionPlsql().addAll(StandaloneFunctionExtractor.extract(conn, data.getUserNames()));
      }
      completedSubSteps++;

      // Sub-step 10: Extract standalone procedures
      progressService.updateSubStepProgress(jobId, MigrationStep.EXTRACT, completedSubSteps, "Extracting standalone procedures");
      if (doStandaloneProcedures) {
        data.getStandaloneProcedurePlsql().addAll(StandaloneProcedureExtractor.extract(conn, data.getUserNames()));
      }
      completedSubSteps++;

      // Sub-step 11: Extract triggers
      progressService.updateSubStepProgress(jobId, MigrationStep.EXTRACT, completedSubSteps, "Extracting triggers");
      if (doTriggers) {
        data.getTriggerPlsql().addAll(TriggerExtractor.extract(conn, data.getUserNames()));
      }
      completedSubSteps++;

      // Sub-step 12: Extract indexes
      progressService.updateSubStepProgress(jobId, MigrationStep.EXTRACT, completedSubSteps, "Extracting indexes");
      if (doIndexes) {
        data.getIndexes().addAll(IndexExtractor.extractAllIndexes(conn, data.getUserNames()));
      }
      completedSubSteps++;

      // Sub-step 13: Calculate total row counts
      if (configurationService.isDoData()) {
        progressService.updateSubStepProgress(jobId, MigrationStep.EXTRACT, completedSubSteps, "Calculating total row count");
        log.info("Calculating total row count for extracted schemas");
        long totalRowCount = RowCountExtractor.calculateTotalRowCount(conn, doAllSchema, data.getUserNames(), rowCountConfig);
        data.setTotalRowCount(totalRowCount);
      } else {
        data.setTotalRowCount(0);
      }
      completedSubSteps++;

      // Complete extraction step
      progressService.updateSubStepProgress(jobId, MigrationStep.EXTRACT, completedSubSteps, "Extraction completed");

      log.info("Extraction completed: {} schemas, {} tables, {} object type specs, {} package specs, {} triggers, {} indexes",
              data.getUserNames().size(), data.getTableSql().size(),
              data.getObjectTypeSpecPlsql().size(), data.getPackageSpecPlsql().size(), data.getTriggerPlsql().size(), data.getIndexes().size());
    }
  }

  private void performParsingWithProgress(String jobId) throws Exception {
    if (progressService.isJobCancelled(jobId)) {
      log.info("Job {} was cancelled before parsing started", jobId);
      return;
    }

    progressService.updateSubStepProgress(jobId, MigrationStep.PARSE, 0, "Starting AST parsing");
    performParsing();

    if (progressService.isJobCancelled(jobId)) {
      log.info("Job {} was cancelled during parsing", jobId);
      return;
    }

    progressService.updateSubStepProgress(jobId, MigrationStep.PARSE, MigrationStep.PARSE.getSubStepCount(), "AST parsing completed");
  }

  private void performExportWithProgress(String jobId) throws Exception {
    if (progressService.isJobCancelled(jobId)) {
      log.info("Job {} was cancelled before export started", jobId);
      return;
    }

    progressService.updateSubStepProgress(jobId, MigrationStep.EXPORT, 0, "Starting file export");
    performExport();

    if (progressService.isJobCancelled(jobId)) {
      log.info("Job {} was cancelled during export", jobId);
      return;
    }

    progressService.updateSubStepProgress(jobId, MigrationStep.EXPORT, MigrationStep.EXPORT.getSubStepCount(), "File export completed");
  }

  private void performPreExecutionWithProgress(String jobId) throws Exception {
    progressService.updateSubStepProgress(jobId, MigrationStep.EXECUTE_PRE, 0, "Starting pre-transfer SQL execution");
    executionController.performPreExecution();
    progressService.updateSubStepProgress(jobId, MigrationStep.EXECUTE_PRE, MigrationStep.EXECUTE_PRE.getSubStepCount(), "Pre-transfer SQL execution completed");
  }

  private void performPostExecutionWithProgress(String jobId) throws Exception {
    progressService.updateSubStepProgress(jobId, MigrationStep.EXECUTE_POST, 0, "Starting post-transfer SQL execution");
    executionController.performPostExecution();
    progressService.updateSubStepProgress(jobId, MigrationStep.EXECUTE_POST, MigrationStep.EXECUTE_POST.getSubStepCount(), "Post-transfer SQL execution completed");
  }

  private void performParsing() throws Exception {
    boolean doViewDdl = configurationService.isDoViewDdl();
    boolean doObjectTypeSpec = configurationService.isDoObjectTypeSpec();
    boolean doObjectTypeBody = configurationService.isDoObjectTypeBody();
    boolean doPackageSpec = configurationService.isDoPackageSpec();
    boolean doPackageBody = configurationService.isDoPackageBody();
    boolean doStandaloneFunctions = configurationService.isDoStandaloneFunctions();
    boolean doStandaloneProcedures = configurationService.isDoStandaloneProcedures();
    boolean doTriggers = configurationService.isDoTriggers();

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
    if (doStandaloneFunctions) {
      for (PlsqlCode s : data.getStandaloneFunctionPlsql()) {
        try {
          Function function = PlSqlAstMain.buildStandaloneFunctionAst(s);
          data.getStandaloneFunctionAst().add(function);
        } catch (Exception e) {
          log.error("Error parsing standalone function from schema: " + s.schema, e);
        }
      }
    }
    if (doStandaloneProcedures) {
      for (PlsqlCode s : data.getStandaloneProcedurePlsql()) {
        try {
          Procedure procedure = PlSqlAstMain.buildStandaloneProcedureAst(s);
          data.getStandaloneProcedureAst().add(procedure);
        } catch (Exception e) {
          log.error("Error parsing standalone procedure from schema: " + s.schema, e);
        }
      }
    }
    if (doTriggers) {
      log.info("Starting trigger parsing...");
      for (PlsqlCode triggerCode : data.getTriggerPlsql()) {
        try {
          Trigger triggerAst =
                  parseTriggerFromPlsqlCode(triggerCode);
          data.getTriggerAst().add(triggerAst);
          log.debug("Parsed trigger: {}", triggerAst.getTriggerName());
        } catch (Exception e) {
          log.error("Failed to parse trigger from schema {}: {}",
                  triggerCode.schema, e.getMessage());
        }
      }
      log.info("Trigger parsing completed: {} triggers parsed", data.getTriggerAst().size());
    }

    log.info("Parsing completed: {} object type ASTs, {} package spec ASTs, {} package body ASTs",
            data.getObjectTypeSpecAst().size(), data.getPackageSpecAst().size(), data.getPackageBodyAst().size());

    if (!data.getObjectTypeSpecAst().isEmpty()) {
      log.info("Object types found:");
      for (ObjectType objType : data.getObjectTypeSpecAst()) {
        log.info("  - {}.{} with {} variables", objType.getSchema(), objType.getName(), objType.getVariables().size());
      }
    } else {
      log.info("No object types found in AST parsing");
    }
  }

  private Trigger parseTriggerFromPlsqlCode(PlsqlCode triggerCode) {
    String fullCode = triggerCode.code;
    String schema = triggerCode.schema;

    String triggerName = extractTriggerName(fullCode);
    String tableName = extractTableName(fullCode);
    String tableOwner = extractTableOwner(fullCode, schema);

    Trigger trigger =
            new Trigger(triggerName, tableName, tableOwner, schema);

    trigger.setTriggerType(extractTriggerType(fullCode));
    trigger.setTriggeringEvent(extractTriggeringEvent(fullCode));

    String whenClause = extractWhenClause(fullCode);
    if (whenClause != null && !whenClause.trim().isEmpty()) {
      trigger.setWhenClause(whenClause);
    }

    List<Statement> bodyStatements =
            parseSimpleTriggerBody(fullCode);
    trigger.setTriggerBody(bodyStatements);

    return trigger;
  }

  private String extractTriggerName(String code) {
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "CREATE\\s+(?:OR\\s+REPLACE\\s+)?TRIGGER\\s+(?:\\w+\\.)?([\\w_]+)",
            java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher matcher = pattern.matcher(code);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return "unknown_trigger";
  }

  private String extractTableName(String code) {
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "ON\\s+(?:\\w+\\.)?([\\w_]+)",
            java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher matcher = pattern.matcher(code);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return "unknown_table";
  }

  private String extractTableOwner(String code, String defaultSchema) {
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "ON\\s+([\\w_]+)\\.([\\w_]+)",
            java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher matcher = pattern.matcher(code);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return defaultSchema;
  }

  private String extractTriggerType(String code) {
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "(BEFORE|AFTER|INSTEAD\\s+OF)",
            java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher matcher = pattern.matcher(code);
    if (matcher.find()) {
      return matcher.group(1).toUpperCase();
    }
    return "BEFORE";
  }

  private String extractTriggeringEvent(String code) {
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "((?:INSERT|UPDATE(?:\\s+OF\\s+[\\w,\\s]+)?|DELETE)(?:\\s+OR\\s+(?:INSERT|UPDATE(?:\\s+OF\\s+[\\w,\\s]+)?|DELETE))*)",
            java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher matcher = pattern.matcher(code);
    if (matcher.find()) {
      return matcher.group(1).toUpperCase().replaceAll("\\s+OR\\s+", ",");
    }
    return "INSERT";
  }

  private String extractWhenClause(String code) {
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "WHEN\\s+\\(([^)]+)\\)",
            java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher matcher = pattern.matcher(code);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  private List<Statement> parseSimpleTriggerBody(String code) {
    String triggerBody = extractTriggerBodyContent(code);

    List<Statement> statements = new ArrayList<>();

    Statement bodyStatement =
            new Statement() {
              @Override
              public <T> T accept(me.christianrobert.ora2postgre.plsql.ast.PlSqlAstVisitor<T> visitor) {
                return visitor.visit(this);
              }

              @Override
              public String toString() {
                return triggerBody;
              }
            };

    statements.add(bodyStatement);
    return statements;
  }

  private String extractTriggerBodyContent(String fullTriggerCode) {
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "(?:BEGIN|FOR\\s+EACH\\s+ROW)\\s*(.*?)(?:END|;)\\s*$",
            java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL);

    java.util.regex.Matcher matcher = pattern.matcher(fullTriggerCode);
    if (matcher.find()) {
      String body = matcher.group(1).trim();
      if (!body.isEmpty()) {
        return body;
      }
    }

    pattern = java.util.regex.Pattern.compile(
            "FOR\\s+EACH\\s+ROW\\s+(.*?)$",
            java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL);

    matcher = pattern.matcher(fullTriggerCode);
    if (matcher.find()) {
      String body = matcher.group(1).trim();
      body = body.replaceAll("(?i)\\s*END\\s*;?\\s*$", "").trim();
      if (!body.isEmpty()) {
        return body;
      }
    }

    return "-- No trigger body found to transform";
  }

  private void performExport() throws Exception {
    boolean doModPlsqlSimulator = configurationService.isDoModPlsqlSimulator();

    boolean doWritePostgreFiles = configurationService.isDoWritePostgreFiles();
    boolean doTable = configurationService.isDoTable();
    boolean doViewSignature = configurationService.isDoViewSignature();
    boolean doObjectTypeSpec = configurationService.isDoObjectTypeSpec();
    boolean doObjectTypeBody = configurationService.isDoObjectTypeBody();
    boolean doPackageSpec = configurationService.isDoPackageSpec();
    boolean doPackageBody = configurationService.isDoPackageBody();
    boolean doViewDdl = configurationService.isDoViewDdl();
    boolean doStandaloneFunctions = configurationService.isDoStandaloneFunctions();
    boolean doStandaloneProcedures = configurationService.isDoStandaloneProcedures();
    boolean doTriggers = configurationService.isDoTriggers();
    boolean doConstraints = configurationService.isDoConstraints();
    boolean doIndexes = configurationService.isDoIndexes();

    if (doModPlsqlSimulator) {
      String pathJava = configurationService.getPathTargetProjectRoot() + configurationService.getPathTargetProjectJava();
      String pathResources = configurationService.getPathTargetProjectRoot() + configurationService.getPathTargetProjectResources();
      String javaPackageName = configurationService.getJavaGeneratedPackageName();

      ExportModPlsqlSimulator.setupTargetProject(
              configurationService.getPathTargetProjectRoot(),
              pathJava,
              pathResources,
              javaPackageName,
              configurationService.getPostgreUrl(),
              configurationService.getPostgreUsername(),
              configurationService.getPostgrePassword()
      );

      if (doPackageSpec && doPackageBody) {
        ExportModPlsqlSimulator.generateSimulators(
                pathJava,
                javaPackageName,
                data.getPackageSpecAst(),
                data.getPackageBodyAst(),
                data
        );
      }
    }

    if (doWritePostgreFiles) {
      String path = configurationService.getPathTargetProjectRoot() + configurationService.getPathTargetProjectPostgre();
      ExportProjectPostgre.save(path);

      ExportSchema.saveSql(path, data.getUserNames());

      if (doTable) {
        ExportTable.saveSql(path, data.getTableSql(), data);
      }
      if (doConstraints) {
        log.info("Starting constraint export to PostgreSQL files");
        ExportConstraint.saveConstraints(path, data);
        log.info("Constraint export completed");
      }
      if (doIndexes) {
        log.info("Starting index export to PostgreSQL files");
        ExportIndex.saveIndexes(path, data);
        log.info("Index export completed");
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
      if (doStandaloneFunctions) {
        ExportStandaloneFunction.saveStandaloneFunctionsToPostgre(path, data.getStandaloneFunctionAst(), data);
      }
      if (doStandaloneProcedures) {
        ExportStandaloneProcedure.saveStandaloneProceduresToPostgre(path, data.getStandaloneProcedureAst(), data);
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
      if (doTriggers) {
        log.info("Starting trigger export to PostgreSQL files");
        ExportTrigger.saveAllTriggers(path, data);
        log.info("Trigger export completed");
      }
    }
  }

}