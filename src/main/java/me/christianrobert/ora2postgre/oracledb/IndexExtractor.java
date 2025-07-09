package me.christianrobert.ora2postgre.oracledb;

import me.christianrobert.ora2postgre.oracledb.tools.UserExcluder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Extracts Oracle database index metadata for migration to PostgreSQL.
 * Handles both simple and composite indexes, including functional indexes.
 */
public class IndexExtractor {

  private static final Logger log = LoggerFactory.getLogger(IndexExtractor.class);

  /**
   * Extracts all indexes from specified Oracle schemas.
   *
   * @param oracleConn Oracle database connection
   * @param users List of schema names to extract indexes from
   * @return List of IndexMetadata objects containing complete index information
   * @throws SQLException if database extraction fails
   */
  public static List<IndexMetadata> extractAllIndexes(Connection oracleConn, List<String> users) throws SQLException {
    List<IndexMetadata> indexMetadataList = new ArrayList<>();

    for (String user : users) {
      if (UserExcluder.is2BeExclueded(user)) {
        log.debug("Skipping excluded user schema: {}", user);
        continue;
      }

      List<IndexMetadata> userIndexes = extractIndexesForSchema(oracleConn, user);
      indexMetadataList.addAll(userIndexes);

      log.info("Extracted {} indexes from schema {}", userIndexes.size(), user);
    }

    log.info("Total indexes extracted: {}", indexMetadataList.size());
    return indexMetadataList;
  }

  /**
   * Extracts indexes for a specific Oracle schema.
   */
  private static List<IndexMetadata> extractIndexesForSchema(Connection oracleConn, String schema) throws SQLException {
    List<IndexMetadata> indexes = new ArrayList<>();

    // Query to extract index metadata from DBA_INDEXES and DBA_IND_COLUMNS
    // For functional indexes, we need to query DBA_IND_EXPRESSIONS separately
    String sql = """
            SELECT 
                i.index_name,
                i.table_name,
                i.index_type,
                i.uniqueness,
                i.tablespace_name,
                i.status,
                i.partitioned,
                ic.column_name,
                ic.column_position,
                ic.descend
            FROM dba_indexes i
            JOIN dba_ind_columns ic ON i.owner = ic.index_owner AND i.index_name = ic.index_name
            WHERE i.owner = ?
            AND i.table_name IN (
                SELECT table_name FROM dba_tables
                WHERE owner = ? AND temporary = 'N'
            )
            AND i.index_name NOT LIKE 'SYS_%'
            AND i.index_name NOT LIKE 'BIN$%'
            AND i.table_name NOT LIKE 'BIN$%'
            ORDER BY i.index_name, ic.column_position
            """;

    Map<String, IndexMetadata> indexMap = new HashMap<>();

    try (PreparedStatement ps = oracleConn.prepareStatement(sql)) {
      ps.setString(1, schema.toUpperCase());
      ps.setString(2, schema.toUpperCase());

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String indexName = rs.getString("index_name");
          String tableName = rs.getString("table_name");
          String indexType = rs.getString("index_type");
          String uniqueness = rs.getString("uniqueness");
          String tablespace = rs.getString("tablespace_name");
          String status = rs.getString("status");
          String partitioned = rs.getString("partitioned");

          String columnName = rs.getString("column_name");
          int columnPosition = rs.getInt("column_position");
          String descend = rs.getString("descend");

          // Create or retrieve IndexMetadata
          IndexMetadata indexMetadata = indexMap.get(indexName);
          if (indexMetadata == null) {
            boolean isUnique = "UNIQUE".equalsIgnoreCase(uniqueness);
            boolean isPartitioned = "YES".equalsIgnoreCase(partitioned);

            indexMetadata = new IndexMetadata(
                    indexName,
                    tableName,
                    schema,
                    indexType,
                    isUnique,
                    false, // partialIndex - will be determined later if needed
                    null,  // whereClause - Oracle doesn't have partial indexes like PostgreSQL
                    tablespace,
                    status,
                    isPartitioned
            );
            indexMap.put(indexName, indexMetadata);
          }

          // Create IndexColumn - we'll get expressions separately for functional indexes
          boolean isDescending = "DESC".equalsIgnoreCase(descend);
          IndexColumn indexColumn = new IndexColumn(
                  columnName,
                  null, // columnExpression - will be populated later for functional indexes
                  descend,
                  columnPosition,
                  isDescending
          );

          indexMetadata.addColumn(indexColumn);
        }
      }
    }

    // Now fetch functional index expressions separately
    populateFunctionalIndexExpressions(oracleConn, schema, indexMap);

    // Convert map to list and filter out unwanted indexes
    for (IndexMetadata index : indexMap.values()) {
      if (shouldIncludeIndex(index)) {
        indexes.add(index);
        log.debug("Extracted index: {} on table {} with {} columns",
                index.getIndexName(), index.getTableName(), index.getColumns().size());
      } else {
        log.debug("Skipped index: {} (reason: {})",
                index.getIndexName(), getSkipReason(index));
      }
    }

    return indexes;
  }

  /**
   * Populates functional index expressions using DBA_IND_EXPRESSIONS.
   */
  private static void populateFunctionalIndexExpressions(Connection oracleConn, String schema, Map<String, IndexMetadata> indexMap) throws SQLException {
    String expressionSql = """
            SELECT 
                ie.index_name,
                ie.column_position,
                ie.column_expression
            FROM dba_ind_expressions ie
            WHERE ie.index_owner = ?
            AND ie.index_name IN (
                SELECT index_name FROM dba_indexes 
                WHERE owner = ? AND index_type LIKE '%FUNCTION%'
            )
            ORDER BY ie.index_name, ie.column_position
            """;

    try (PreparedStatement ps = oracleConn.prepareStatement(expressionSql)) {
      ps.setString(1, schema.toUpperCase());
      ps.setString(2, schema.toUpperCase());

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String indexName = rs.getString("index_name");
          int columnPosition = rs.getInt("column_position");
          String columnExpression = rs.getString("column_expression");

          IndexMetadata indexMetadata = indexMap.get(indexName);
          if (indexMetadata != null && columnExpression != null) {
            // Find the corresponding column and update its expression
            for (IndexColumn column : indexMetadata.getColumns()) {
              if (column.getPosition() == columnPosition) {
                // Create a new IndexColumn with the expression
                IndexColumn updatedColumn = new IndexColumn(
                        column.getColumnName(),
                        columnExpression.trim(),
                        column.getSortOrder(),
                        column.getPosition(),
                        column.isDescending()
                );

                // Replace the column in the index
                indexMetadata.getColumns().set(
                        indexMetadata.getColumns().indexOf(column),
                        updatedColumn
                );
                break;
              }
            }

            log.debug("Added expression to functional index {}: {}", indexName, columnExpression);
          }
        }
      }
    } catch (SQLException e) {
      // Log the error but don't fail the entire extraction
      log.warn("Could not fetch functional index expressions for schema {}: {}", schema, e.getMessage());
    }
  }

  /**
   * Determines if an index should be included in the extraction.
   * Filters out system indexes and other unwanted indexes.
   */
  private static boolean shouldIncludeIndex(IndexMetadata index) {
    String indexName = index.getIndexName();
    String tableName = index.getTableName();

    // Skip system-generated indexes
    if (indexName.startsWith("SYS_") || indexName.startsWith("BIN$") ||
            indexName.startsWith("PK_") || indexName.startsWith("UK_") ||
            indexName.startsWith("FK_")) {
      return false;
    }

    // Skip indexes on system tables
    if (tableName.startsWith("SYS_") || tableName.startsWith("BIN$") ||
            tableName.startsWith("MLOG$_") || tableName.startsWith("RUPD$_") ||
            tableName.startsWith("AQ$") || tableName.startsWith("DR$")) {
      return false;
    }

    // Skip invalid indexes
    if (!"VALID".equalsIgnoreCase(index.getStatus())) {
      return false;
    }

    // Skip if no columns (shouldn't happen, but safety check)
    if (index.getColumns().isEmpty()) {
      return false;
    }

    return true;
  }

  /**
   * Gets the reason why an index was skipped (for logging).
   */
  private static String getSkipReason(IndexMetadata index) {
    String indexName = index.getIndexName();
    String tableName = index.getTableName();

    if (indexName.startsWith("SYS_") || indexName.startsWith("BIN$")) {
      return "System-generated index";
    }

    if (indexName.startsWith("PK_") || indexName.startsWith("UK_") || indexName.startsWith("FK_")) {
      return "Constraint-related index";
    }

    if (tableName.startsWith("SYS_") || tableName.startsWith("BIN$")) {
      return "System table";
    }

    if (!"VALID".equalsIgnoreCase(index.getStatus())) {
      return "Invalid index status: " + index.getStatus();
    }

    if (index.getColumns().isEmpty()) {
      return "No columns found";
    }

    return "Unknown reason";
  }

  /**
   * Extracts indexes for a specific table (useful for targeted extraction).
   */
  public static List<IndexMetadata> extractIndexesForTable(Connection oracleConn, String schema, String tableName) throws SQLException {
    if (UserExcluder.is2BeExclueded(schema)) {
      log.debug("Skipping excluded schema: {}", schema);
      return new ArrayList<>();
    }

    List<IndexMetadata> indexes = new ArrayList<>();

    String sql = """
            SELECT 
                i.index_name,
                i.table_name,
                i.index_type,
                i.uniqueness,
                i.tablespace_name,
                i.status,
                i.partitioned,
                ic.column_name,
                ic.column_position,
                ic.descend
            FROM dba_indexes i
            JOIN dba_ind_columns ic ON i.owner = ic.index_owner AND i.index_name = ic.index_name
            WHERE i.owner = ? AND i.table_name = ?
            AND i.index_name NOT LIKE 'SYS_%'
            AND i.index_name NOT LIKE 'BIN$%'
            ORDER BY i.index_name, ic.column_position
            """;

    Map<String, IndexMetadata> indexMap = new HashMap<>();

    try (PreparedStatement ps = oracleConn.prepareStatement(sql)) {
      ps.setString(1, schema.toUpperCase());
      ps.setString(2, tableName.toUpperCase());

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String indexName = rs.getString("index_name");
          String indexType = rs.getString("index_type");
          String uniqueness = rs.getString("uniqueness");
          String tablespace = rs.getString("tablespace_name");
          String status = rs.getString("status");
          String partitioned = rs.getString("partitioned");

          String columnName = rs.getString("column_name");
          int columnPosition = rs.getInt("column_position");
          String descend = rs.getString("descend");

          // Create or retrieve IndexMetadata
          IndexMetadata indexMetadata = indexMap.get(indexName);
          if (indexMetadata == null) {
            boolean isUnique = "UNIQUE".equalsIgnoreCase(uniqueness);
            boolean isPartitioned = "YES".equalsIgnoreCase(partitioned);

            indexMetadata = new IndexMetadata(
                    indexName,
                    tableName,
                    schema,
                    indexType,
                    isUnique,
                    false,
                    null,
                    tablespace,
                    status,
                    isPartitioned
            );
            indexMap.put(indexName, indexMetadata);
          }

          // Create IndexColumn
          boolean isDescending = "DESC".equalsIgnoreCase(descend);
          IndexColumn indexColumn = new IndexColumn(
                  columnName,
                  null, // columnExpression - will be populated later for functional indexes
                  descend,
                  columnPosition,
                  isDescending
          );

          indexMetadata.addColumn(indexColumn);
        }
      }
    }

    // Populate functional index expressions for this table's indexes
    populateFunctionalIndexExpressionsForTable(oracleConn, schema, tableName, indexMap);

    // Filter and add valid indexes
    for (IndexMetadata index : indexMap.values()) {
      if (shouldIncludeIndex(index)) {
        indexes.add(index);
      }
    }

    log.info("Extracted {} indexes for table {}.{}", indexes.size(), schema, tableName);
    return indexes;
  }

  /**
   * Populates functional index expressions for a specific table's indexes.
   */
  private static void populateFunctionalIndexExpressionsForTable(Connection oracleConn, String schema, String tableName, Map<String, IndexMetadata> indexMap) throws SQLException {
    String expressionSql = """
            SELECT 
                ie.index_name,
                ie.column_position,
                ie.column_expression
            FROM dba_ind_expressions ie
            JOIN dba_indexes i ON ie.index_owner = i.owner AND ie.index_name = i.index_name
            WHERE ie.index_owner = ? 
            AND i.table_name = ?
            AND i.index_type LIKE '%FUNCTION%'
            ORDER BY ie.index_name, ie.column_position
            """;

    try (PreparedStatement ps = oracleConn.prepareStatement(expressionSql)) {
      ps.setString(1, schema.toUpperCase());
      ps.setString(2, tableName.toUpperCase());

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String indexName = rs.getString("index_name");
          int columnPosition = rs.getInt("column_position");
          String columnExpression = rs.getString("column_expression");

          IndexMetadata indexMetadata = indexMap.get(indexName);
          if (indexMetadata != null && columnExpression != null) {
            // Find the corresponding column and update its expression
            for (IndexColumn column : indexMetadata.getColumns()) {
              if (column.getPosition() == columnPosition) {
                // Create a new IndexColumn with the expression
                IndexColumn updatedColumn = new IndexColumn(
                        column.getColumnName(),
                        columnExpression.trim(),
                        column.getSortOrder(),
                        column.getPosition(),
                        column.isDescending()
                );

                // Replace the column in the index
                indexMetadata.getColumns().set(
                        indexMetadata.getColumns().indexOf(column),
                        updatedColumn
                );
                break;
              }
            }

            log.debug("Added expression to functional index {} on table {}: {}", indexName, tableName, columnExpression);
          }
        }
      }
    } catch (SQLException e) {
      // Log the error but don't fail the entire extraction
      log.warn("Could not fetch functional index expressions for table {}.{}: {}", schema, tableName, e.getMessage());
    }
  }

  /**
   * Gets index statistics for reporting purposes.
   */
  public static Map<String, Integer> getIndexStatistics(List<IndexMetadata> indexes) {
    Map<String, Integer> stats = new HashMap<>();

    int totalIndexes = indexes.size();
    int uniqueIndexes = 0;
    int compositeIndexes = 0;
    int functionalIndexes = 0;
    int bitmapIndexes = 0;
    int convertibleIndexes = 0;

    for (IndexMetadata index : indexes) {
      if (index.isUniqueIndex()) {
        uniqueIndexes++;
      }
      if (index.isComposite()) {
        compositeIndexes++;
      }
      if (index.isFunctional()) {
        functionalIndexes++;
      }
      if (index.isBitmap()) {
        bitmapIndexes++;
      }
      if (index.isEasilyConvertible()) {
        convertibleIndexes++;
      }
    }

    stats.put("total", totalIndexes);
    stats.put("unique", uniqueIndexes);
    stats.put("composite", compositeIndexes);
    stats.put("functional", functionalIndexes);
    stats.put("bitmap", bitmapIndexes);
    stats.put("convertible", convertibleIndexes);
    stats.put("problematic", totalIndexes - convertibleIndexes);

    return stats;
  }
}