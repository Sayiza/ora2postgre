package me.christianrobert.ora2postgre.transfer.strategy;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils;
import me.christianrobert.ora2postgre.oracledb.ColumnMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.transfer.TableAnalyzer;
import me.christianrobert.ora2postgre.transfer.progress.TransferProgress;
import me.christianrobert.ora2postgre.transfer.progress.TransferResult;

import java.io.StringReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Transfer strategy that uses CSV streaming for tables with only primitive data types.
 * 
 * Process:
 * 1. Stream data from Oracle ResultSet
 * 2. Convert to CSV format in memory (batched)
 * 3. Use PostgreSQL COPY FROM to bulk insert
 * 
 * This approach is memory-efficient and fast for tables with simple data types.
 */
public class StreamingCsvStrategy implements TransferStrategy {
    
    private static final int DEFAULT_BATCH_SIZE = 10000;
    private static final int FETCH_SIZE = 5000;
    private final int batchSize;
    
    public StreamingCsvStrategy() {
        this(DEFAULT_BATCH_SIZE);
    }
    
    public StreamingCsvStrategy(int batchSize) {
        this.batchSize = batchSize;
    }
    
    @Override
    public boolean canHandle(TableMetadata table, Everything everything) {
        // StreamingCsvStrategy doesn't need Everything context, only primitive type analysis
        return TableAnalyzer.hasOnlyPrimitiveTypes(table);
    }
    
    @Override
    public String getStrategyName() {
        return "CSV Streaming";
    }
    
    @Override
    public TransferResult transferTable(TableMetadata table, Connection oracleConn, 
                                      Connection postgresConn, TransferProgress progress, Everything everything) throws Exception {
        
        long startTime = System.currentTimeMillis();
        String schemaName = table.getSchema();
        String tableName = table.getTableName();
        
        try {
            // Count total rows for progress tracking
            long totalRows = countTableRows(oracleConn, table);
            progress.startTable(schemaName, tableName, totalRows);
            
            if (totalRows == 0) {
                progress.completeTable(0);
                return TransferResult.success(schemaName, tableName, 0, 0, 
                    System.currentTimeMillis() - startTime, getStrategyName());
            }
            
            // Transfer data in batches
            long totalTransferred = transferDataInBatches(table, oracleConn, postgresConn, progress, totalRows);
            
            long transferTime = System.currentTimeMillis() - startTime;
            progress.completeTable(totalTransferred);
            
            return TransferResult.success(schemaName, tableName, totalTransferred, totalRows, 
                transferTime, getStrategyName());
                
        } catch (Exception e) {
            return TransferResult.failure(schemaName, tableName, getStrategyName(), 
                "Transfer failed: " + e.getMessage(), e);
        }
    }
    
    private long countTableRows(Connection oracleConn, TableMetadata table) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM " + PostgreSqlIdentifierUtils.quoteIdentifier(table.getSchema()) + 
                         "." + PostgreSqlIdentifierUtils.quoteIdentifier(table.getTableName());
        
        try (PreparedStatement ps = oracleConn.prepareStatement(countSql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        }
    }
    
    private long transferDataInBatches(TableMetadata table, Connection oracleConn, 
                                     Connection postgresConn, TransferProgress progress, 
                                     long totalRows) throws Exception {
        
        List<ColumnMetadata> columns = table.getColumns();
        String selectSql = buildSelectQuery(table);
        String copyColumns = buildCopyColumnList(columns);
        
        long totalTransferred = 0;
        
        try (PreparedStatement selectStmt = oracleConn.prepareStatement(selectSql)) {
            selectStmt.setFetchSize(FETCH_SIZE);
            
            try (ResultSet rs = selectStmt.executeQuery()) {
                List<String> csvBatch = new ArrayList<>();
                
                while (rs.next()) {
                    String csvRow = convertRowToCsv(rs, columns);
                    csvBatch.add(csvRow);
                    
                    // Process batch when full
                    if (csvBatch.size() >= batchSize) {
                        long batchTransferred = executeCopyFromBatch(postgresConn, table, copyColumns, csvBatch);
                        totalTransferred += batchTransferred;
                        progress.updateCurrentTableProgress(totalTransferred);
                        csvBatch.clear();
                    }
                }
                
                // Process remaining rows
                if (!csvBatch.isEmpty()) {
                    long batchTransferred = executeCopyFromBatch(postgresConn, table, copyColumns, csvBatch);
                    totalTransferred += batchTransferred;
                    progress.updateCurrentTableProgress(totalTransferred);
                }
            }
        }
        
        return totalTransferred;
    }
    
    private String buildSelectQuery(TableMetadata table) {
        List<ColumnMetadata> columns = table.getColumns();
        List<String> columnNames = new ArrayList<>();
        
        for (ColumnMetadata column : columns) {
            columnNames.add(PostgreSqlIdentifierUtils.quoteIdentifier(column.getColumnName()));
        }
        
        return "SELECT " + String.join(", ", columnNames) + 
               " FROM " + PostgreSqlIdentifierUtils.quoteIdentifier(table.getSchema()) + "." + PostgreSqlIdentifierUtils.quoteIdentifier(table.getTableName());
    }
    
    private String buildCopyColumnList(List<ColumnMetadata> columns) {
        List<String> columnNames = new ArrayList<>();
        for (ColumnMetadata column : columns) {
            columnNames.add(PostgreSqlIdentifierUtils.quoteIdentifier(column.getColumnName()));
        }
        return String.join(", ", columnNames);
    }
    
    private String convertRowToCsv(ResultSet rs, List<ColumnMetadata> columns) throws SQLException {
        List<String> values = new ArrayList<>();
        
        for (ColumnMetadata column : columns) {
            String value = formatValueForCsv(rs, column);
            values.add(value);
        }
        
        return String.join("\t", values); // Using tab-separated values for PostgreSQL COPY
    }
    
    private String formatValueForCsv(ResultSet rs, ColumnMetadata column) throws SQLException {
        String columnName = column.getColumnName();
        String dataType = column.getDataType().toUpperCase();
        
        Object value = rs.getObject(columnName);
        if (value == null) {
            return "\\N"; // PostgreSQL NULL representation in COPY format
        }
        
        if (dataType.contains("CHAR") || dataType.contains("CLOB")) {
            String stringValue = rs.getString(columnName);
            // Escape special characters for CSV
            return escapeForCsv(stringValue);
        } else if (dataType.equals("NUMBER") || dataType.equals("INTEGER") || dataType.equals("FLOAT")) {
            return rs.getString(columnName);
        } else if (dataType.equals("DATE") || dataType.contains("TIMESTAMP")) {
            Timestamp timestamp = rs.getTimestamp(columnName);
            if (timestamp != null) {
                return timestamp.toString();
            }
            return "\\N";
        } else {
            // Default: treat as string with escaping
            String stringValue = rs.getString(columnName);
            return escapeForCsv(stringValue);
        }
    }
    
    private String escapeForCsv(String value) {
        if (value == null) {
            return "\\N";
        }
        
        // Escape backslashes, tabs, newlines, and carriage returns for PostgreSQL COPY format
        return value.replace("\\", "\\\\")
                   .replace("\t", "\\t")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }
    
    private long executeCopyFromBatch(Connection postgresConn, TableMetadata table, 
                                    String copyColumns, List<String> csvBatch) throws SQLException {
        
        String copyQuery = String.format("COPY %s.%s (%s) FROM STDIN", 
            PostgreSqlIdentifierUtils.quoteIdentifier(table.getSchema()), 
            PostgreSqlIdentifierUtils.quoteIdentifier(table.getTableName()), 
            copyColumns);
        
        // Join all CSV rows with newlines
        String csvData = String.join("\n", csvBatch);
        
        // Use PostgreSQL CopyManager for efficient bulk insert
        try {
            if (postgresConn instanceof org.postgresql.PGConnection) {
                org.postgresql.copy.CopyManager copyManager = 
                    ((org.postgresql.PGConnection) postgresConn).getCopyAPI();
                
                StringReader reader = new StringReader(csvData);
                return copyManager.copyIn(copyQuery, reader);
            } else {
                // Fallback: use regular INSERT statements if COPY is not available
                return executeBatchInsert(postgresConn, table, csvBatch);
            }
        } catch (Exception e) {
            throw new SQLException("COPY operation failed: " + e.getMessage(), e);
        }
    }
    
    private long executeBatchInsert(Connection postgresConn, TableMetadata table, 
                                  List<String> csvBatch) throws SQLException {
        // Fallback method using regular INSERT statements
        // This is less efficient but more compatible
        
        List<ColumnMetadata> columns = table.getColumns();
        String insertSql = buildInsertStatement(table, columns);
        
        try (PreparedStatement ps = postgresConn.prepareStatement(insertSql)) {
            for (String csvRow : csvBatch) {
                String[] values = csvRow.split("\t");
                for (int i = 0; i < values.length && i < columns.size(); i++) {
                    if ("\\N".equals(values[i])) {
                        ps.setNull(i + 1, Types.NULL);
                    } else {
                        ps.setString(i + 1, unescapeFromCsv(values[i]));
                    }
                }
                ps.addBatch();
            }
            
            int[] results = ps.executeBatch();
            return results.length;
        }
    }
    
    private String buildInsertStatement(TableMetadata table, List<ColumnMetadata> columns) {
        List<String> columnNames = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        
        for (ColumnMetadata column : columns) {
            columnNames.add(PostgreSqlIdentifierUtils.quoteIdentifier(column.getColumnName()));
            placeholders.add("?");
        }
        
        return String.format("INSERT INTO %s.%s (%s) VALUES (%s)",
            PostgreSqlIdentifierUtils.quoteIdentifier(table.getSchema()),
            PostgreSqlIdentifierUtils.quoteIdentifier(table.getTableName()),
            String.join(", ", columnNames),
            String.join(", ", placeholders));
    }
    
    private String unescapeFromCsv(String value) {
        if (value == null || "\\N".equals(value)) {
            return null;
        }
        
        return value.replace("\\\\", "\\")
                   .replace("\\t", "\t")
                   .replace("\\n", "\n")
                   .replace("\\r", "\r");
    }
}