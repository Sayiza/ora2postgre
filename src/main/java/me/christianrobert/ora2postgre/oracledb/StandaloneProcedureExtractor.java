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

public class StandaloneProcedureExtractor {

  private static final Logger log = LoggerFactory.getLogger(StandaloneProcedureExtractor.class);

  public static List<PlsqlCode> extract(Connection connection, List<String> schemas)
          throws SQLException, IOException {

    schemas = schemas.stream().filter(e -> !UserExcluder.is2BeExclueded(e, "PROCEDURE")).toList();

    List<PlsqlCode> dllList = new ArrayList<>();

    // Prepare SQL query to get procedure metadata from ALL_OBJECTS
    String procedureSql = """
                SELECT owner, object_name
                FROM all_objects
                WHERE owner IN (%s)
                  AND object_type = 'PROCEDURE'
                ORDER BY owner, object_name
            """;
    StringJoiner placeholders = new StringJoiner(",");
    for (int i = 0; i < schemas.size(); i++) {
      placeholders.add("?");
    }
    procedureSql = String.format(procedureSql, placeholders);

    try (PreparedStatement procedureStmt = connection.prepareStatement(procedureSql)) {
      // Set schema parameters
      for (int i = 0; i < schemas.size(); i++) {
        procedureStmt.setString(i + 1, schemas.get(i));
      }

      // Execute query to get procedure metadata
      try (ResultSet procedureRs = procedureStmt.executeQuery()) {
        while (procedureRs.next()) {
          String schema = procedureRs.getString("owner");
          String procedureName = procedureRs.getString("object_name");

          if (procedureName.contains("=")) {
            continue;
          }

          // Query ALL_SOURCE for procedure source code
          String sourceCode = getSourceCode(connection, schema, procedureName);
          if (sourceCode != null) {
            dllList.add(new PlsqlCode(schema, sourceCode));
          }

          log.info("Extracted standalone procedure {} from schema {}", procedureName, schema);
        }
      }
    }
    return dllList;
  }

  private static String getSourceCode(Connection connection, String schema, String procedureName)
          throws SQLException {
    String sourceSql = """
                SELECT text
                FROM all_source
                WHERE owner = ?
                  AND name = ?
                  AND type = 'PROCEDURE'
                ORDER BY line
            """;

    StringBuilder sourceCode = new StringBuilder();
    try (PreparedStatement sourceStmt = connection.prepareStatement(sourceSql)) {
      sourceStmt.setString(1, schema);
      sourceStmt.setString(2, procedureName);

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