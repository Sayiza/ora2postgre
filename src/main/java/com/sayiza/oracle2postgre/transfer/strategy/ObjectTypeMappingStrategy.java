package com.sayiza.oracle2postgre.transfer.strategy;

import com.sayiza.oracle2postgre.global.Everything;
import com.sayiza.oracle2postgre.global.PostgreSqlIdentifierUtils;
import com.sayiza.oracle2postgre.oracledb.ColumnMetadata;
import com.sayiza.oracle2postgre.oracledb.TableMetadata;
import com.sayiza.oracle2postgre.plsql.ast.ObjectType;
import com.sayiza.oracle2postgre.transfer.ObjectTypeMapper;
import com.sayiza.oracle2postgre.transfer.ParameterSetter;
import com.sayiza.oracle2postgre.transfer.TableAnalyzer;
import com.sayiza.oracle2postgre.transfer.progress.TransferProgress;
import com.sayiza.oracle2postgre.transfer.progress.TransferResult;
import com.sayiza.oracle2postgre.oracledb.tools.NameNormalizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Transfer strategy for tables containing Oracle object types.
 *
 * This strategy handles tables like:
 * CREATE TABLE langtable (nr NUMBER, text VARCHAR2(300), langy langdata2)
 *
 * Where langdata2 is an Oracle object type that needs to be converted to PostgreSQL JSONB.
 *
 * The strategy:
 * 1. Detects object type columns in table metadata
 * 2. Converts Oracle object instances to JSON using ObjectTypeMapper
 * 3. Uses PostgreSQL JSONB for storage
 * 4. Handles both simple and complex object type structures
 */
public class ObjectTypeMappingStrategy implements TransferStrategy {

  private static final Logger log = LoggerFactory.getLogger(ObjectTypeMappingStrategy.class);
  private final ObjectTypeMapper mapper;

  public ObjectTypeMappingStrategy() {
    this.mapper = new ObjectTypeMapper();
  }

  @Override
  public boolean canHandle(TableMetadata table, Everything everything) {
    if (everything == null) {
      log.debug("ObjectTypeMappingStrategy.canHandle({}.{}): false (no Everything context)",
              table.getSchema(), table.getTableName());
      return false;
    }
    
    boolean hasObjectTypes = TableAnalyzer.hasObjectTypes(table, everything);
    boolean hasComplexTypes = !TableAnalyzer.hasOnlyPrimitiveTypes(table);
    boolean hasAnydataColumns = TableAnalyzer.hasAnydataColumns(table);
    
    // Handle tables with object types, ANYDATA columns, OR other complex types (or combinations)
    boolean canHandle = hasObjectTypes || hasComplexTypes || hasAnydataColumns;
    
    log.debug("ObjectTypeMappingStrategy.canHandle({}.{}): {} (hasObjectTypes: {}, hasComplexTypes: {}, hasAnydataColumns: {}, objectTypes in context: {})",
            table.getSchema(), table.getTableName(), canHandle, hasObjectTypes, hasComplexTypes, hasAnydataColumns,
            everything.getObjectTypeSpecAst().size());

    // Additional debugging for columns
    if (log.isDebugEnabled()) {
      for (ColumnMetadata column : table.getColumns()) {
        String dataType = column.getDataType().toUpperCase();
        log.debug("  Column {}: type '{}'", column.getColumnName(), dataType);
      }
    }

    return canHandle;
  }

  @Override
  public String getStrategyName() {
    return "Unified Object Type, ANYDATA, and Complex Data Transfer";
  }

  @Override
  public TransferResult transferTable(TableMetadata table,
                                      Connection oracleConn,
                                      Connection postgresConn,
                                      TransferProgress progress,
                                      Everything everything) {

    long startTime = System.currentTimeMillis();
    String schema = table.getSchema();
    String tableName = table.getTableName();

    log.info("Starting object type mapping transfer for {}.{}", schema, tableName);

    try {
      // Start progress tracking
      long estimatedRows = estimateRowCount(oracleConn, table);
      progress.startTable(schema, tableName, estimatedRows);

      // Build column information
      List<ColumnMetadata> columns = table.getColumns();
      List<ObjectTypeColumnInfo> objectTypeColumns = analyzeObjectTypeColumns(columns, schema, everything);

      // Build SELECT and INSERT queries
      String selectSql = buildSelectQuery(table);
      String insertSql = buildInsertQuery(table, objectTypeColumns);

      log.debug("Oracle SELECT: {}", selectSql);
      log.debug("PostgreSQL INSERT: {}", insertSql);

      // Execute transfer
      long transferredRows = executeTransfer(table, oracleConn, postgresConn, selectSql, insertSql,
              columns, objectTypeColumns, progress);

      long endTime = System.currentTimeMillis();
      long transferTime = endTime - startTime;

      log.info("Object type mapping transfer completed for {}.{}: {} rows in {} ms",
              schema, tableName, transferredRows, transferTime);

      return TransferResult.success(schema, tableName, transferredRows, estimatedRows,
              transferTime, getStrategyName());

    } catch (Exception e) {
      log.error("Object type mapping transfer failed for {}.{}: {}", schema, tableName, e.getMessage(), e);

      return TransferResult.failure(schema, tableName, getStrategyName(),
              "Transfer failed: " + e.getMessage(), e);
    }
  }

  /**
   * Analyzes table columns to identify which ones contain object types.
   */
  private List<ObjectTypeColumnInfo> analyzeObjectTypeColumns(List<ColumnMetadata> columns, String schema, Everything everything) {
    List<ObjectTypeColumnInfo> objectTypeColumns = new ArrayList<>();

    for (int i = 0; i < columns.size(); i++) {
      ColumnMetadata column = columns.get(i);
      String dataType = NameNormalizer.normalizeDataType(column.getDataType());

      // Check if this data type is a known object type
      String actualSchemaOfObjType = everything.lookupSchema4ObjectType(dataType, schema);
      ObjectType objectTypeAst = findObjectType(actualSchemaOfObjType, dataType, everything);
      if (objectTypeAst != null) {
        objectTypeColumns.add(new ObjectTypeColumnInfo(i, column, objectTypeAst));
        log.debug("Detected object type column: {} of type {}", column.getColumnName(), dataType);
      }
    }

    return objectTypeColumns;
  }

  /**
   * Builds the Oracle SELECT query for extracting data.
   */
  private String buildSelectQuery(TableMetadata table) {
    StringBuilder sql = new StringBuilder();
    sql.append("SELECT ");

    List<String> columnNames = new ArrayList<>();
    for (ColumnMetadata column : table.getColumns()) {
      columnNames.add(PostgreSqlIdentifierUtils.quoteIdentifier(column.getColumnName()));
    }

    sql.append(String.join(", ", columnNames));
    sql.append(" FROM ").append(PostgreSqlIdentifierUtils.quoteIdentifier(table.getSchema()))
            .append(".").append(PostgreSqlIdentifierUtils.quoteIdentifier(table.getTableName()));

    return sql.toString();
  }

  /**
   * Builds the PostgreSQL INSERT query with JSONB placeholders for object types.
   */
  private String buildInsertQuery(TableMetadata table, List<ObjectTypeColumnInfo> objectTypeColumns) {
    StringBuilder sql = new StringBuilder();
    sql.append("INSERT INTO ").append(table.getSchema())
            .append(".").append(table.getTableName()).append(" (");

    // Column names
    List<String> columnNames = new ArrayList<>();
    for (ColumnMetadata column : table.getColumns()) {
      columnNames.add(PostgreSqlIdentifierUtils.quoteIdentifier(column.getColumnName()));
    }
    sql.append(String.join(", ", columnNames));

    // VALUES clause with placeholders
    sql.append(") VALUES (");
    List<String> placeholders = new ArrayList<>();
    for (int i = 0; i < table.getColumns().size(); i++) {
      placeholders.add("?");
    }
    sql.append(String.join(", ", placeholders));
    sql.append(")");

    return sql.toString();
  }

  /**
   * Executes the actual data transfer with object type conversion.
   */
  private long executeTransfer(TableMetadata table, Connection oracleConn, Connection postgresConn,
                               String selectSql, String insertSql,
                               List<ColumnMetadata> columns,
                               List<ObjectTypeColumnInfo> objectTypeColumns,
                               TransferProgress progress) throws SQLException {

    long rowCount = 0;

    try (PreparedStatement selectStmt = oracleConn.prepareStatement(selectSql)) {
      selectStmt.setFetchSize(1000); // Reasonable fetch size for object types

      try (ResultSet rs = selectStmt.executeQuery();
           PreparedStatement insertStmt = postgresConn.prepareStatement(insertSql)) {

        while (rs.next()) {

          try {
            // Set parameters for each column

            for (int i = 0; i < columns.size(); i++) {
              ColumnMetadata column = columns.get(i);
              int paramIndex = i + 1; // PreparedStatement is 1-based

              // Check if this column is an object type
              ObjectTypeColumnInfo objectTypeInfo = findObjectTypeColumn(objectTypeColumns, i);

              if (objectTypeInfo != null) {
                // Handle object type column
                try {
                  Object oracleObject = rs.getObject(column.getColumnName());
                  String compositeValue = mapper.convertObjectToCompositeType(oracleObject, objectTypeInfo.objectTypeAst);
                  mapper.setCompositeTypeParameter(insertStmt, paramIndex, compositeValue);
                } catch (Exception e) {
                  log.error("Failed to convert object type for column {} at row {}: {}", 
                          column.getColumnName(), rowCount + 1, e.getMessage(), e);
                  throw new SQLException("Object type conversion failed for column " + column.getColumnName() + 
                          " at row " + (rowCount + 1), e);
                }
              } else {
                // Handle regular column
                try {
                  setRegularParameter(insertStmt, paramIndex, rs, column);
                } catch (SQLException e) {
                  log.error("Failed to set regular parameter for column {} at row {}: {}", 
                          column.getColumnName(), rowCount + 1, e.getMessage(), e);
                  throw new SQLException("Failed to set parameter for column " + column.getColumnName() + 
                          " at row " + (rowCount + 1), e);
                }
              }
            }

            insertStmt.addBatch();
            rowCount++;

          } catch (SQLException e) {
            log.error("Failed to process row {} for table {}.{}: {}", 
                    rowCount + 1, table.getSchema(), table.getTableName(), e.getMessage(), e);
            throw new SQLException("Row processing failed at row " + (rowCount + 1) + 
                    " for table " + table.getSchema() + "." + table.getTableName(), e);
          }

          // Execute batch periodically


          if (rowCount % 1000 == 0) {
            try {
              insertStmt.executeBatch();
              progress.updateCurrentTableProgress(rowCount);
              log.debug("Transferred {} rows for object type table", rowCount);
            } catch (SQLException e) {
              log.error("Failed to execute batch at row {} for table {}.{}: {}", 
                      rowCount, table.getSchema(), table.getTableName(), e.getMessage(), e);
              throw new SQLException("Batch execution failed at row " + rowCount + 
                      " for table " + table.getSchema() + "." + table.getTableName(), e);
            }
          }
        }

        // Execute final batch
        if (rowCount % 1000 != 0) {
          try {
            insertStmt.executeBatch();
            progress.updateCurrentTableProgress(rowCount);
          } catch (SQLException e) {
            log.error("Failed to execute final batch for table {}.{}: {}", 
                    table.getSchema(), table.getTableName(), e.getMessage(), e);
            throw new SQLException("Final batch execution failed for table " + 
                    table.getSchema() + "." + table.getTableName(), e);
          }
        }
      }
    }

    return rowCount;
  }

  /**
   * Sets a regular (non-object-type) parameter in the PreparedStatement.
   * Now uses the unified ParameterSetter to handle both primitive and complex types.
   */
  private void setRegularParameter(PreparedStatement stmt, int paramIndex, ResultSet rs, ColumnMetadata column)
          throws SQLException {
    // Use the unified parameter setter that handles all Oracle data types
    ParameterSetter.setParameter(stmt, paramIndex, rs, column);
  }

  /**
   * Finds object type column info by column index.
   */
  private ObjectTypeColumnInfo findObjectTypeColumn(List<ObjectTypeColumnInfo> objectTypeColumns, int columnIndex) {
    return objectTypeColumns.stream()
            .filter(info -> info.columnIndex == columnIndex)
            .findFirst()
            .orElse(null);
  }

  /**
   * Estimates row count for progress tracking.
   */
  private long estimateRowCount(Connection oracleConn, TableMetadata table) throws SQLException {
    try (PreparedStatement stmt = oracleConn.prepareStatement(
            "SELECT COUNT(*) FROM " + PostgreSqlIdentifierUtils.quoteIdentifier(table.getSchema()) +
                    "." + PostgreSqlIdentifierUtils.quoteIdentifier(table.getTableName()))) {

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getLong(1);
        }
      }
    } catch (SQLException e) {
      log.error("Failed to estimate row count for {}.{}: {}",
              table.getSchema(), table.getTableName(), e.getMessage(), e);
      throw new SQLException("Failed to estimate row count for " + table.getSchema() + "." + table.getTableName(), e);
    }

    return 1000; // Default estimate
  }


  /**
   * Helper method to find an ObjectType by schema and name.
   * Uses normalized names for consistent matching.
   */
  private ObjectType findObjectType(String schema, String typeName, Everything everything) {
    String normalizedSchema = NameNormalizer.normalizeIdentifier(schema);
    String normalizedTypeName = NameNormalizer.normalizeObjectTypeName(typeName);
    
    log.debug("Looking for object type: schema='{}', typeName='{}' (normalized: {}.{})", 
            schema, typeName, normalizedSchema, normalizedTypeName);

    ObjectType result = everything.getObjectTypeSpecAst().stream()
            .filter(objectType -> {
              String objSchema = NameNormalizer.normalizeIdentifier(objectType.getSchema());
              String objName = NameNormalizer.normalizeObjectTypeName(objectType.getName());
              boolean schemaMatch = objSchema.equals(normalizedSchema);
              boolean nameMatch = objName.equals(normalizedTypeName);
              //log.debug("  Checking: {}.{} (normalized: {}.{}) - schema match: {}, name match: {}",
              //        objectType.getSchema(), objectType.getName(), objSchema, objName, schemaMatch, nameMatch);
              return schemaMatch && nameMatch;
            })
            .findFirst()
            .orElse(null);

    log.debug("Found object type: {}", result != null ? result.getName() : "null");
    return result;
  }

  /**
   * Information about a column that contains an object type.
   */
  private static class ObjectTypeColumnInfo {
    final int columnIndex;
    final ColumnMetadata columnMetadata;
    final ObjectType objectTypeAst;

    ObjectTypeColumnInfo(int columnIndex, ColumnMetadata columnMetadata, ObjectType objectTypeAst) {
      this.columnIndex = columnIndex;
      this.columnMetadata = columnMetadata;
      this.objectTypeAst = objectTypeAst;
    }
  }
}