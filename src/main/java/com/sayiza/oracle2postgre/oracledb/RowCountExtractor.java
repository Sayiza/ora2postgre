package com.sayiza.oracle2postgre.oracledb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class RowCountExtractor {

    private static final Logger log = LoggerFactory.getLogger(RowCountExtractor.class);
    
    /**
     * Calculates the total row count using the specified method and configuration.
     * 
     * @param conn Oracle database connection
     * @param allSchemas whether to include all schemas or limit to specific ones
     * @param schemaList list of specific schemas to include (used when allSchemas is false)
     * @param config row count configuration
     * @return total row count across all tables in the specified schemas
     * @throws SQLException if database query fails
     */
    public static long calculateTotalRowCount(Connection conn, boolean allSchemas, 
                                            List<String> schemaList, RowCountConfig config) throws SQLException {
        
        switch (config.method()) {
            case STATISTICS_ONLY:
                return calculateTotalRowCountByStatistics(conn, allSchemas, schemaList);
            case EXACT_COUNT:
                return calculateTotalRowCountExact(conn, allSchemas, schemaList, config);
            case SAMPLING:
                return SamplingRowCounter.estimateTotalRowCountBySampling(conn, allSchemas, schemaList, config.samplingPercentage());
            case HYBRID:
                return calculateTotalRowCountHybrid(conn, allSchemas, schemaList, config);
            default:
                log.warn("Unknown row count method: {}, falling back to STATISTICS_ONLY", config.method());
                return calculateTotalRowCountByStatistics(conn, allSchemas, schemaList);
        }
    }

    /**
     * Legacy method for backward compatibility. Uses statistics-only approach.
     * 
     * @param conn Oracle database connection
     * @param allSchemas whether to include all schemas or limit to specific ones
     * @param schemaList list of specific schemas to include (used when allSchemas is false)
     * @return total row count across all tables in the specified schemas
     * @throws SQLException if database query fails
     * @deprecated Use {@link #calculateTotalRowCount(Connection, boolean, List, RowCountConfig)} instead
     */
    @Deprecated
    public static long calculateTotalRowCount(Connection conn, boolean allSchemas, List<String> schemaList) throws SQLException {
        return calculateTotalRowCountByStatistics(conn, allSchemas, schemaList);
    }
    
    /**
     * Calculates the total row count for tables in the specified schemas using statistics only.
     * Uses Oracle's all_tables view to get approximate row counts via num_rows statistics.
     * 
     * @param conn Oracle database connection
     * @param allSchemas whether to include all schemas or limit to specific ones
     * @param schemaList list of specific schemas to include (used when allSchemas is false)
     * @return total row count across all tables in the specified schemas
     * @throws SQLException if database query fails
     */
    public static long calculateTotalRowCountByStatistics(Connection conn, boolean allSchemas, List<String> schemaList) throws SQLException {
        String sql;
        
        if (allSchemas || schemaList.isEmpty()) {
            // Query all schemas (excluding system schemas)
            sql = """
                SELECT NVL(SUM(num_rows), 0) 
                FROM all_tables x 
                WHERE x.owner NOT IN ('SYS', 'SYSTEM', 'APEX_030200', 'APEX_040000', 'APEX_040200', 
                                      'APPQOSSYS', 'CTXSYS', 'DBSNMP', 'DIP', 'FLOWS_FILES', 
                                      'HR', 'MDSYS', 'ORACLE_OCM', 'OUTLN', 'WMSYS', 'XDB', 'XS$NULL',
                                      'ANONYMOUS', 'BI', 'MDDATA', 'OE', 'PM', 'IX', 'SH')
                """;
        } else {
            // Query specific schemas
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < schemaList.size(); i++) {
                if (i > 0) placeholders.append(", ");
                placeholders.append("?");
            }
            
            sql = "SELECT NVL(SUM(num_rows), 0) FROM all_tables x WHERE x.owner IN (" + placeholders + ")";
        }
        
        log.info("Calculating total row count with query: {}", sql);
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Set parameters for specific schemas
            if (!allSchemas && !schemaList.isEmpty()) {
                for (int i = 0; i < schemaList.size(); i++) {
                    stmt.setString(i + 1, schemaList.get(i).toUpperCase());
                }
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long rowCount = rs.getLong(1);
                    log.info("Total row count calculated: {}", rowCount);
                    return rowCount;
                }
            }
        }
        
        return 0;
    }
    
    /**
     * Calculates total row count using exact counting with limits.
     */
    private static long calculateTotalRowCountExact(Connection conn, boolean allSchemas, 
                                                   List<String> schemaList, RowCountConfig config) throws SQLException {
        
        List<String> tableList = getTableList(conn, allSchemas, schemaList);
        long totalCount = 0;
        int exactCountUsed = 0;
        
        for (String table : tableList) {
            String[] parts = table.split("\\.");
            if (parts.length != 2) continue;
            
            String owner = parts[0];
            String tableName = parts[1];
            
            if (exactCountUsed < config.maxExactCountTables()) {
                totalCount += SamplingRowCounter.getExactRowCount(conn, owner, tableName);
                exactCountUsed++;
            } else {
                // Fall back to sampling for remaining tables
                totalCount += SamplingRowCounter.estimateRowCountBySampling(conn, owner, tableName, config.samplingPercentage());
            }
        }
        
        return totalCount;
    }
    
    /**
     * Calculates total row count using hybrid approach.
     */
    private static long calculateTotalRowCountHybrid(Connection conn, boolean allSchemas, 
                                                    List<String> schemaList, RowCountConfig config) throws SQLException {
        
        List<String> tableList = getTableList(conn, allSchemas, schemaList);
        long totalCount = 0;
        int exactCountUsed = 0;
        
        for (String table : tableList) {
            String[] parts = table.split("\\.");
            if (parts.length != 2) continue;
            
            String owner = parts[0];
            String tableName = parts[1];
            
            // Check statistics freshness
            StatisticsFreshness.StatisticsFreshnessInfo freshness = 
                StatisticsFreshness.checkStatisticsFreshness(conn, owner, tableName, config.statisticsStalenessThreshold());
            
            if (!freshness.isStale() && freshness.getNumRows() > 0) {
                // Use statistics if they're fresh
                totalCount += freshness.getNumRows();
                log.debug("Using fresh statistics for {}.{}: {} rows", owner, tableName, freshness.getNumRows());
            } else if (exactCountUsed < config.maxExactCountTables() && freshness.getNumRows() < config.samplingThreshold()) {
                // Use exact count for small tables
                totalCount += SamplingRowCounter.getExactRowCount(conn, owner, tableName);
                exactCountUsed++;
                log.debug("Using exact count for small table {}.{}", owner, tableName);
            } else {
                // Use sampling for large tables or when exact count limit reached
                long sampledCount = SamplingRowCounter.estimateRowCountBySampling(conn, owner, tableName, config.samplingPercentage());
                if (sampledCount == 0) {
                    // Fall back to segment size estimation
                    sampledCount = SamplingRowCounter.estimateRowCountBySegmentSize(conn, owner, tableName);
                }
                totalCount += sampledCount;
                log.debug("Using sampling for {}.{}: {} rows", owner, tableName, sampledCount);
            }
        }
        
        return totalCount;
    }
    
    /**
     * Gets list of tables for the specified schemas.
     */
    private static List<String> getTableList(Connection conn, boolean allSchemas, List<String> schemaList) throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql;
        
        if (allSchemas || schemaList.isEmpty()) {
            sql = """
                SELECT owner || '.' || table_name as full_name
                FROM all_tables
                WHERE owner NOT IN ('SYS', 'SYSTEM', 'APEX_030200', 'APEX_040000', 'APEX_040200', 
                                   'APPQOSSYS', 'CTXSYS', 'DBSNMP', 'DIP', 'FLOWS_FILES', 
                                   'HR', 'MDSYS', 'ORACLE_OCM', 'OUTLN', 'WMSYS', 'XDB', 'XS$NULL',
                                   'ANONYMOUS', 'BI', 'MDDATA', 'OE', 'PM', 'IX', 'SH')
                ORDER BY owner, table_name
                """;
        } else {
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < schemaList.size(); i++) {
                if (i > 0) placeholders.append(", ");
                placeholders.append("?");
            }
            sql = "SELECT owner || '.' || table_name as full_name FROM all_tables WHERE owner IN (" + placeholders + ") ORDER BY owner, table_name";
        }
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (!allSchemas && !schemaList.isEmpty()) {
                for (int i = 0; i < schemaList.size(); i++) {
                    stmt.setString(i + 1, schemaList.get(i).toUpperCase());
                }
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("full_name"));
                }
            }
        }
        
        return tables;
    }
}