package me.christianrobert.ora2postgre.oracledb;

import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.oracledb.tools.UserExcluder;
import me.christianrobert.ora2postgre.oracledb.tools.CodeCleaner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class StandaloneFunctionExtractor {

  private static final Logger log = LoggerFactory.getLogger(StandaloneFunctionExtractor.class);

  public static List<PlsqlCode> extract(Connection connection, List<String> schemas)
          throws SQLException, IOException {

    schemas = schemas.stream().filter(e -> !UserExcluder.is2BeExclueded(e, "FUNCTION")).toList();

    List<PlsqlCode> dllList = new ArrayList<>();

    // Prepare SQL query to get function metadata from ALL_OBJECTS
    String functionSql = """
                SELECT owner, object_name
                FROM all_objects
                WHERE owner IN (%s)
                  AND object_type = 'FUNCTION'
                ORDER BY owner, object_name
            """;
    StringJoiner placeholders = new StringJoiner(",");
    for (int i = 0; i < schemas.size(); i++) {
      placeholders.add("?");
    }
    functionSql = String.format(functionSql, placeholders);

    try (PreparedStatement functionStmt = connection.prepareStatement(functionSql)) {
      // Set schema parameters
      for (int i = 0; i < schemas.size(); i++) {
        functionStmt.setString(i + 1, schemas.get(i));
      }

      // Execute query to get function metadata
      try (ResultSet functionRs = functionStmt.executeQuery()) {
        while (functionRs.next()) {
          String schema = functionRs.getString("owner");
          String functionName = functionRs.getString("object_name");

          if (functionName.contains("=")) {
            continue;
          }

          // Query ALL_SOURCE for function source code
          String sourceCode = getSourceCode(connection, schema, functionName);
          if (sourceCode != null) {
            dllList.add(new PlsqlCode(schema, sourceCode));
          }

          log.info("Extracted standalone function {} from schema {}", functionName, schema);
        }
      }
    }
    return dllList;
  }

  private static String getSourceCode(Connection connection, String schema, String functionName)
          throws SQLException {
    String sourceSql = """
                SELECT text
                FROM all_source
                WHERE owner = ?
                  AND name = ?
                  AND type = 'FUNCTION'
                ORDER BY line
            """;

    StringBuilder sourceCode = new StringBuilder();
    try (PreparedStatement sourceStmt = connection.prepareStatement(sourceSql)) {
      sourceStmt.setString(1, schema);
      sourceStmt.setString(2, functionName);

      try (ResultSet sourceRs = sourceStmt.executeQuery()) {
        while (sourceRs.next()) {
          String line = sourceRs.getString("text");
          if (line != null) {
            if (line.toLowerCase().contains("wrapped"))
              return null;
            sourceCode.append(line);
          }
        }
      }
    }
    if (sourceCode.isEmpty()) {
      return null;
    }

    return "CREATE " + CodeCleaner.noComments(sourceCode.toString().trim());
  }
}