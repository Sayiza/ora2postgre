package com.sayiza.oracle2postgre.oracledb;

import com.sayiza.oracle2postgre.global.PlsqlCode;
import com.sayiza.oracle2postgre.oracledb.tools.UserExcluder;
import com.sayiza.oracle2postgre.oracledb.tools.CodeCleaner;

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

public class PackageExtractor {

  private static final Logger log = LoggerFactory.getLogger(PackageExtractor.class);

  public static List<PlsqlCode> extract(Connection connection, List<String> schemas, boolean doSpecYesdoBodyNo)
          throws SQLException, IOException {

    schemas = schemas.stream().filter(e -> !UserExcluder.is2BeExclueded(e, "PACKAGE")).toList();

    List<PlsqlCode> dllList = new ArrayList<>();

    // Prepare SQL query to get package metadata from ALL_OBJECTS
    String packageSql = """
                SELECT owner, object_name
                FROM all_objects
                WHERE owner IN (%s)
                  AND object_type = ?
                ORDER BY owner, object_name
            """;
    StringJoiner placeholders = new StringJoiner(",");
    for (int i = 0; i < schemas.size(); i++) {
      placeholders.add("?");
    }
    packageSql = String.format(packageSql, placeholders);

    try (PreparedStatement packageStmt = connection.prepareStatement(packageSql)) {
      // Set schema parameters
      for (int i = 0; i < schemas.size(); i++) {
        packageStmt.setString(i + 1, schemas.get(i));
      }
      // Set object type (PACKAGE for spec, PACKAGE BODY for body)
      packageStmt.setString(schemas.size() + 1, doSpecYesdoBodyNo ? "PACKAGE" : "PACKAGE BODY");

      // Execute query to get package metadata
      try (ResultSet packageRs = packageStmt.executeQuery()) {
        while (packageRs.next()) {
          String schema = packageRs.getString("owner");
          String packageName = packageRs.getString("object_name");

          if (packageName.contains("=")) {
            continue;
          }

          // Query ALL_SOURCE for package specification or body
          String sourceType = doSpecYesdoBodyNo ? "PACKAGE" : "PACKAGE BODY";
          String sourceCode = getSourceCode(connection, schema, packageName, sourceType);
          if (sourceCode != null) {
            dllList.add(new PlsqlCode(schema, sourceCode));
          }

          log.info("Extracted {} {} from schema {}", sourceType, packageName, schema);
        }
      }
    }
    return dllList;
  }

  private static String getSourceCode(Connection connection, String schema,
                                      String packageName, String sourceType)
          throws SQLException {
    String sourceSql = """
                SELECT text
                FROM all_source
                WHERE owner = ?
                  AND name = ?
                  AND type = ?
                ORDER BY line
            """;

    StringBuilder sourceCode = new StringBuilder();
    boolean isWrapped = false;
    try (PreparedStatement sourceStmt = connection.prepareStatement(sourceSql)) {
      sourceStmt.setString(1, schema);
      sourceStmt.setString(2, packageName);
      sourceStmt.setString(3, sourceType);

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