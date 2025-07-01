package com.sayiza.oracle2postgre.oracledb;

import com.sayiza.oracle2postgre.global.PlsqlCode;
import com.sayiza.oracle2postgre.oracledb.tools.UserExcluder;
import com.sayiza.oracle2postgre.oracledb.tools.CodeCleaner;
import com.sayiza.oracle2postgre.oracledb.tools.NameNormalizer;

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

public class ObjectTypeExtractor {

  private static final Logger log = LoggerFactory.getLogger(ObjectTypeExtractor.class);

  public static List<PlsqlCode> extract(Connection connection, List<String> schemas, boolean doSpecYesdoBodyNo)
          throws SQLException, IOException {

    schemas = schemas.stream().filter(e -> !UserExcluder.is2BeExclueded(e, "TYPE")).toList();

    List<PlsqlCode> dllList = new ArrayList<>();

    //do this in a later step separately
    // Map<String, String> buildTypeNameMap = HelpObjectType.buildObjectTypeNameMap(connection);

    // Prepare SQL query to get type metadata from ALL_TYPES
    String typeSql = """
            SELECT owner, type_name, typecode, attributes, methods
            FROM all_types
            WHERE owner IN (%s)
            ORDER BY owner, type_name
        """;
    StringJoiner placeholders = new StringJoiner(",");
    for (int i = 0; i < schemas.size(); i++) {
      placeholders.add("?");
    }
    typeSql = String.format(typeSql, placeholders);

    try (PreparedStatement typeStmt = connection.prepareStatement(typeSql)) {
      // Set schema parameters
      for (int i = 0; i < schemas.size(); i++) {
        typeStmt.setString(i + 1, schemas.get(i));
      }

      // Execute query to get type metadata
      try (ResultSet typeRs = typeStmt.executeQuery()) {
        while (typeRs.next()) {
          String schema = typeRs.getString("owner");
          String typeName = NameNormalizer.normalizeObjectTypeName(typeRs.getString("type_name"));

          if (typeName.contains("="))
            continue;

          // Query ALL_SOURCE for type specification (TYPE)
          if (doSpecYesdoBodyNo) {
            String sourceCode = getSourceCode(connection, schema, typeName, "TYPE");
            if (sourceCode != null) {
              dllList.add(new PlsqlCode(schema, sourceCode));
              log.info("Extracted type spec {} from schema {}", typeName, schema);
            }
          }
          // Query ALL_SOURCE for type body (TYPE BODY)
          else {
            String sourceCode = getSourceCode(connection, schema, typeName, "TYPE BODY");
            if (sourceCode != null) {
              //if (typeName.toUpperCase().contains("DCOBJECTCONTEXT")) //DEBUG
              dllList.add(new PlsqlCode(schema, sourceCode));
              log.info("Extracted type body {} from schema {}", typeName, schema);
            }
          }
        }
      }
    }
    return dllList;
  }

  private static String getSourceCode(Connection connection, String schema,
                                      String typeName, String sourceType)
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
    try (PreparedStatement sourceStmt = connection.prepareStatement(sourceSql)) {
      sourceStmt.setString(1, schema);
      sourceStmt.setString(2, typeName);
      sourceStmt.setString(3, sourceType);

      try (ResultSet sourceRs = sourceStmt.executeQuery()) {
        while (sourceRs.next()) {
          String line = sourceRs.getString("text");
          if (line != null) {
            sourceCode.append(line);
          }
        }
      }
    }
    if (sourceCode.isEmpty())
      return null;
    return "CREATE " + CodeCleaner.noComments(sourceCode.toString().trim());
  }
}