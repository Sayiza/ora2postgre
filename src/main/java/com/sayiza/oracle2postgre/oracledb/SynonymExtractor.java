package com.sayiza.oracle2postgre.oracledb;

import com.sayiza.oracle2postgre.oracledb.tools.UserExcluder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SynonymExtractor {

  private static final Logger log = LoggerFactory.getLogger(SynonymExtractor.class);

  /**
   * Extracts metadata for all synonyms in the specified schemas from an Oracle database.
   *
   * @param oracleConn Oracle database connection
   * @param users      List of schema names (Oracle users)
   * @return List of SynonymMetadata objects
   * @throws SQLException if database operations fail
   */
  public static List<SynonymMetadata> extractAllSynonyms(Connection oracleConn, List<String> users) throws SQLException {
    List<SynonymMetadata> synonymMetadataList = new ArrayList<>();

    for (String user : users) {
      if (UserExcluder.is2BeExclueded(user)) {
        continue;
      }

      List<SynonymMetadata> synonyms = fetchSynonyms(oracleConn, user);
      synonymMetadataList.addAll(synonyms);
      log.info("Extracted synonyms from schema {}", user);
    }
    return synonymMetadataList;
  }

  /**
   * Fetches synonyms for a given schema from all_synonyms.
   */
  private static List<SynonymMetadata> fetchSynonyms(Connection oracleConn, String owner) throws SQLException {
    List<SynonymMetadata> result = new ArrayList<>();
    String sql = "SELECT synonym_name, table_owner, table_name, db_link " +
            "FROM all_synonyms WHERE owner = ? ORDER BY synonym_name";

    try (PreparedStatement ps = oracleConn.prepareStatement(sql)) {
      ps.setString(1, owner.toUpperCase());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String synonymName = rs.getString("synonym_name");
          String referencedSchema = rs.getString("table_owner");
          String referencedObjectName = rs.getString("table_name");
          String dbLink = rs.getString("db_link");

          // Skip synonyms with database links (cross-database references)
          if (dbLink != null && !dbLink.isEmpty()) {
            log.warn("Skipping synonym with DB link: {}.{}", owner, synonymName);
            continue;
          }

          // Skip synonyms referencing system schemas
          if (referencedSchema != null && UserExcluder.is2BeExclueded(referencedSchema)) {
            continue;
          }

          // Determine object type (TABLE, VIEW, etc.) by querying all_objects
          String objectType = getObjectType(oracleConn, referencedSchema, referencedObjectName);
          if (objectType == null || !objectType.matches("TABLE|VIEW")) {
            log.warn("Skipping synonym with unsupported object type: {}.{} -> {}.{} ({})",
                    owner, synonymName, referencedSchema, referencedObjectName, objectType);
            continue;
          }

          SynonymMetadata synonym = new SynonymMetadata(
                  owner, synonymName, referencedSchema, referencedObjectName, objectType);
          result.add(synonym);
        }
      }
    }
    return result;
  }

  /**
   * Determines the object type (TABLE, VIEW, etc.) of the referenced object.
   */
  private static String getObjectType(Connection oracleConn, String schema, String objectName) throws SQLException {
    String sql = "SELECT object_type FROM all_objects WHERE owner = ? AND object_name = ?";
    try (PreparedStatement ps = oracleConn.prepareStatement(sql)) {
      ps.setString(1, schema.toUpperCase());
      ps.setString(2, objectName);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getString("object_type");
        }
      }
    }
    return null;
  }
}