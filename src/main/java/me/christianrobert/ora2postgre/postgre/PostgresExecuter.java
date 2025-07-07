package me.christianrobert.ora2postgre.postgre;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PostgresExecuter {
  
  private static final Logger logger = LoggerFactory.getLogger(PostgresExecuter.class);

  public enum ExecutionPhase {
    PRE_TRANSFER_TYPES,  // Execute type specs at very first
    PRE_TRANSFER_TABLES,  // Execute schema and table files before data transfer
    POST_TRANSFER,  // Execute views, packages, and other files after data transfer
    POST_TRANSFER_CONSTRAINTS,  // Execute constraints after all other objects are created
    POST_TRANSFER_TRIGGERS  // Execute triggers after constraints are created
  }

  public static void executeAllSqlFiles(
          String mainBaseDir,
          Connection postgresConn,
          List<String> priorityNames,
          List<String> excludeList,
          ExecutionPhase phase
  ) throws Exception {
    Path mainBasePath = Paths.get(mainBaseDir);
    if (!Files.exists(mainBasePath) || !Files.isDirectory(mainBasePath)) {
      throw new IllegalArgumentException("Invalid base directory: " + mainBaseDir);
    }

    executeDirectoryRecursively(mainBasePath, postgresConn, priorityNames, excludeList, phase);
  }

  private static void executeDirectoryRecursively(
          Path directory,
          Connection postgresConn,
          List<String> priorityNames,
          List<String> excludeList,
          ExecutionPhase phase
  ) throws Exception {
    if (!Files.exists(directory) || !Files.isDirectory(directory)) {
      return;
    }

    List<Path> sqlFiles = new ArrayList<>();
    List<Path> subdirectories = new ArrayList<>();

    // Collect SQL files and subdirectories
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
      for (Path entry : stream) {
        if (Files.isDirectory(entry)) {
          subdirectories.add(entry);
        } else if (entry.getFileName().toString().toLowerCase().endsWith(".sql")) {
          String fileName = entry.getFileName().toString();
          if (shouldExecuteFileUnified(entry, phase)) {
            sqlFiles.add(entry);
          }
        }
      }
    }

    // Sort SQL files alphabetically (with priority list consideration)
    sqlFiles.sort(Comparator
            .comparingInt((Path p) -> {
              String fileName = p.getFileName().toString();
              int idx = priorityNames.indexOf(fileName);
              return idx == -1 ? Integer.MAX_VALUE : idx;
            })
            .thenComparing(p -> p.getFileName().toString().toLowerCase())
    );

    // Execute SQL files first
    for (Path file : sqlFiles) {
      if (!excludeList.contains(file.getFileName().toString())) {
        executeSQLFile(file, postgresConn);
      }
    }

    // Sort subdirectories alphabetically
    subdirectories.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()));

    // Process subdirectories recursively
    for (Path subDir : subdirectories) {
      String dirName = subDir.getFileName().toString();
      
      // Special handling for constraint directories in POST_TRANSFER_CONSTRAINTS phase
      if (phase == ExecutionPhase.POST_TRANSFER_CONSTRAINTS && "step8constraints".equals(dirName)) {
        logger.info("Executing constraints with dependency ordering for: {}", subDir);
        executeConstraintsInOrder(subDir, postgresConn);
      } else {
        executeDirectoryRecursively(subDir, postgresConn, priorityNames, excludeList, phase);
      }
    }
  }

  /**
   * Unified file execution detection method that handles both filename and path-based detection.
   * This prevents duplicate file execution by using a single detection method.
   */
  private static boolean shouldExecuteFileUnified(Path filePath, ExecutionPhase phase) {
    if (phase == null) {
      return true; // Execute all files if no phase specified (backward compatibility)
    }
    
    String fileName = filePath.getFileName().toString();
    String upperFileName = fileName.toUpperCase();
    boolean isTriggerFile = isTriggerFileByPath(filePath);
    boolean isConstraintFile = isConstraintFileByPath(filePath);
    
    switch (phase) {
      case PRE_TRANSFER_TYPES:
        // Execute types only
        return upperFileName.endsWith("SCHEMA.SQL")
                || upperFileName.endsWith("OBJECTTYPESPEC.SQL");
      case PRE_TRANSFER_TABLES:
        // Execute table files only
        return upperFileName.endsWith("TABLE.SQL");
      case POST_TRANSFER:
        // Execute all other files after data transfer (excluding schema, table, constraint, and trigger files)
        return !upperFileName.endsWith("SCHEMA.SQL")
                && !upperFileName.endsWith("TABLE.SQL")
                && !upperFileName.endsWith("OBJECTTYPESPEC.SQL")
                && !isTriggerFile
                && !isConstraintFile;
      case POST_TRANSFER_CONSTRAINTS:
        // Execute only constraint files
        logger.debug("POST_TRANSFER_CONSTRAINTS phase - File: {}, IsConstraint: {}", fileName, isConstraintFile);
        return isConstraintFile;
      case POST_TRANSFER_TRIGGERS:
        // Execute only trigger files in the final phase
        logger.debug("POST_TRANSFER_TRIGGERS phase - File: {}, IsTrigger: {}", fileName, isTriggerFile);
        return isTriggerFile;

      default:
        return true;
    }
  }


  /**
   * Enhanced trigger file detection that considers the full path.
   * This provides more accurate detection by examining the directory structure.
   */
  private static boolean isTriggerFileByPath(Path filePath) {
    String pathStr = filePath.toString().toLowerCase();
    boolean isTrigger = pathStr.contains("step7atriggerfunctions") || 
                       pathStr.contains("step7btriggerdefinitions") ||
                       pathStr.contains("triggers");
    
    // Debug logging for trigger file detection
    if (isTrigger) {
      logger.debug("Detected trigger file: {}", filePath);
    }
    
    return isTrigger;
  }

  /**
   * Enhanced constraint file detection that considers the full path.
   * Detects files in step8constraints directories.
   */
  private static boolean isConstraintFileByPath(Path filePath) {
    String pathStr = filePath.toString().toLowerCase();
    boolean isConstraint = pathStr.contains("step8constraints");
    
    // Debug logging for constraint file detection
    if (isConstraint) {
      logger.debug("Detected constraint file: {}", filePath);
    }
    
    return isConstraint;
  }

  /**
   * Executes constraint files in the correct dependency order.
   * Order: PRIMARY KEY → UNIQUE → CHECK → FOREIGN KEY
   * 
   * @param directory Directory containing constraint files
   * @param postgresConn PostgreSQL database connection
   * @throws Exception if execution fails
   */
  public static void executeConstraintsInOrder(Path directory, Connection postgresConn) throws Exception {
    if (!Files.exists(directory) || !Files.isDirectory(directory)) {
      logger.debug("Constraint directory not found: {}", directory);
      return;
    }

    logger.info("Executing constraints in dependency order from: {}", directory);

    // Execute constraints in dependency order
    String[] constraintOrder = {"primary_keys", "unique_constraints", "check_constraints", "foreign_keys"};
    
    for (String constraintType : constraintOrder) {
      Path constraintDir = directory.resolve(constraintType);
      if (Files.exists(constraintDir) && Files.isDirectory(constraintDir)) {
        logger.info("Executing {} constraints...", constraintType);
        executeFilesInDirectory(constraintDir, postgresConn);
        logger.info("Completed {} constraints", constraintType);
      } else {
        logger.debug("No {} constraints found in: {}", constraintType, constraintDir);
      }
    }

    logger.info("All constraints executed successfully");
  }

  /**
   * Executes all SQL files in a directory in alphabetical order.
   * 
   * @param directory Directory containing SQL files
   * @param postgresConn PostgreSQL database connection
   * @throws Exception if execution fails
   */
  private static void executeFilesInDirectory(Path directory, Connection postgresConn) throws Exception {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.sql")) {
      List<Path> sqlFiles = new ArrayList<>();
      for (Path file : stream) {
        sqlFiles.add(file);
      }
      
      // Sort files alphabetically for consistent execution order
      sqlFiles.sort(Comparator.comparing(path -> path.getFileName().toString()));
      
      for (Path sqlFile : sqlFiles) {
        try {
          executeSQLFile(sqlFile, postgresConn);
          logger.debug("Successfully executed constraint file: {}", sqlFile.getFileName());
        } catch (Exception e) {
          String constraintName = extractConstraintNameFromFilename(sqlFile.getFileName().toString());
          logger.error("Failed to execute constraint '{}' from file {}: {}", constraintName, sqlFile, e.getMessage());
          
          // Log specific constraint error details
          if (e.getMessage().contains("already exists")) {
            logger.warn("Constraint '{}' already exists - this may be expected in some scenarios", constraintName);
          } else if (e.getMessage().contains("does not exist")) {
            logger.error("Referenced table/column does not exist for constraint '{}' - check migration order", constraintName);
          } else if (e.getMessage().contains("violates")) {
            logger.error("Data violation for constraint '{}' - existing data does not meet constraint requirements", constraintName);
          } else {
            logger.error("Unexpected error creating constraint '{}': {}", constraintName, e.getMessage());
          }
          
          // Continue with other constraints - don't fail the entire migration
          logger.warn("Continuing with remaining constraints...");
        }
      }
    }
  }

  /**
   * Extracts constraint name from filename.
   * Constraint files are named like "pk_employees.sql", "fk_emp_dept.sql", etc.
   * 
   * @param filename The constraint filename
   * @return The constraint name (without .sql extension)
   */
  private static String extractConstraintNameFromFilename(String filename) {
    if (filename.endsWith(".sql")) {
      return filename.substring(0, filename.length() - 4);
    }
    return filename;
  }

  public static void executeSQLFile(Path sqlFilePath, Connection connection) throws Exception {
    logger.info("Executing PostgreSQL DDL file: {}", sqlFilePath);

    // Read entire file as one string
    String sql = Files.readString(sqlFilePath);
    String[] statements = sql.split("(?m)^;\\n");

    try (Statement stmt = connection.createStatement()) {
      for (int i = 0; i < statements.length; i++) {
        String statement = statements[i].trim();
        if (!statement.isEmpty()) {
          String statementPreview = statement.substring(0, Math.min(200, statement.length())).replace("\n", " ");
          logger.debug("Executing statement {}/{}: {}", i + 1, statements.length, statementPreview);
          
          try {
            stmt.execute(statement);
          } catch (SQLException e) {
            // Log comprehensive error context for system logs monitoring
            logger.error("SQL execution failed in file: {}", sqlFilePath, e);
            logger.error("Failed statement {}/{}: {}", i + 1, statements.length, statementPreview);
            logger.error("SQL Error Code: {}, SQL State: {}", e.getErrorCode(), e.getSQLState());
            
            // Re-throw to fail fast instead of continuing with broken state
            throw new SQLException(
              String.format("SQL execution failed in file '%s' at statement %d/%d: %s", 
                sqlFilePath, i + 1, statements.length, e.getMessage()), 
              e.getSQLState(), 
              e.getErrorCode(), 
              e
            );
          }
        }
      }
      logger.info("Successfully executed {} statements from file: {}", statements.length, sqlFilePath);
    } catch (Exception e) {
      // Log any other unexpected errors with full context
      logger.error("Unexpected error executing SQL file: {}", sqlFilePath, e);
      throw e;
    }
  }
}
