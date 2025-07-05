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
    POST_TRANSFER,  // Execute constraint and other files after data transfer
    POST_TRANSFER_TRIGGERS  // Execute triggers after all other objects are created
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
      executeDirectoryRecursively(subDir, postgresConn, priorityNames, excludeList, phase);
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
    
    switch (phase) {
      case PRE_TRANSFER_TYPES:
        // Execute types only
        return upperFileName.endsWith("SCHEMA.SQL")
                || upperFileName.endsWith("OBJECTTYPESPEC.SQL");
      case PRE_TRANSFER_TABLES:
        // Execute table files only
        return upperFileName.endsWith("TABLE.SQL");
      case POST_TRANSFER:
        // Execute all other files after data transfer (excluding schema, table, and trigger files)
        return !upperFileName.endsWith("SCHEMA.SQL")
                && !upperFileName.endsWith("TABLE.SQL")
                && !upperFileName.endsWith("OBJECTTYPESPEC.SQL")
                && !isTriggerFile;
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
