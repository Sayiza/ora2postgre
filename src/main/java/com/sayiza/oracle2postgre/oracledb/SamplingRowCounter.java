package com.sayiza.oracle2postgre.oracledb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Utility class for Oracle table row counting using sampling techniques.
 */
public class SamplingRowCounter {
    
    private static final Logger log = LoggerFactory.getLogger(SamplingRowCounter.class);
    
    /**
     * Estimates row count using Oracle's SAMPLE clause.
     * 
     * @param conn Oracle database connection
     * @param owner table owner/schema
     * @param tableName table name
     * @param samplingPercentage percentage to sample (0.1-100.0)
     * @return estimated row count
     * @throws SQLException if database query fails
     */
    public static long estimateRowCountBySampling(Connection conn, String owner, String tableName, 
                                                 double samplingPercentage) throws SQLException {
        
        // Ensure sampling percentage is within reasonable bounds
        double adjustedPercentage = Math.max(0.1, Math.min(100.0, samplingPercentage));
        
        String sql = String.format(
            "SELECT COUNT(*) * %.1f FROM %s.%s SAMPLE(%.1f)", 
            100.0 / adjustedPercentage,
            owner.toUpperCase(), 
            tableName.toUpperCase(),
            adjustedPercentage
        );
        
        log.debug("Estimating row count for {}.{} using sampling: {}", owner, tableName, sql);
        
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                long estimatedCount = rs.getLong(1);
                log.info("Estimated row count for {}.{} via sampling ({}%): {}", 
                        owner, tableName, adjustedPercentage, estimatedCount);
                return estimatedCount;
            }
        } catch (Exception e) {
            log.info("Error on estimating rows", e.getMessage());
            return 0;
        }
        
        return 0;
    }
    
    /**
     * Gets exact row count using SELECT COUNT(*).
     * 
     * @param conn Oracle database connection
     * @param owner table owner/schema
     * @param tableName table name
     * @return exact row count
     * @throws SQLException if database query fails
     */
    public static long getExactRowCount(Connection conn, String owner, String tableName) throws SQLException {
        String sql = String.format("SELECT COUNT(*) FROM %s.%s", owner.toUpperCase(), tableName.toUpperCase());
        
        log.debug("Getting exact row count for {}.{}: {}", owner, tableName, sql);
        
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                long exactCount = rs.getLong(1);
                log.info("Exact row count for {}.{}: {}", owner, tableName, exactCount);
                return exactCount;
            }
        } catch (Exception e) {
            log.info("Error on estimating rows", e.getMessage());
            return 0;
        }
        
        return 0;
    }
    
    /**
     * Estimates total row count for multiple schemas using sampling.
     * 
     * @param conn Oracle database connection
     * @param allSchemas whether to include all schemas
     * @param schemaList list of specific schemas
     * @param samplingPercentage percentage to sample
     * @return estimated total row count
     * @throws SQLException if database query fails
     */
    public static long estimateTotalRowCountBySampling(Connection conn, boolean allSchemas, 
                                                      List<String> schemaList, double samplingPercentage) throws SQLException {
        
        double adjustedPercentage = Math.max(0.1, Math.min(100.0, samplingPercentage));
        String sql;
        
        if (allSchemas || schemaList.isEmpty()) {
            // Sample all schemas (excluding system schemas)
            sql = String.format("""
                SELECT SUM(estimated_count) FROM (
                    SELECT COUNT(*) * %.1f as estimated_count
                    FROM all_tables t
                    WHERE t.owner NOT IN ('SYS', 'SYSTEM', 'APEX_030200', 'APEX_040000', 'APEX_040200', 
                                          'APPQOSSYS', 'CTXSYS', 'DBSNMP', 'DIP', 'FLOWS_FILES', 
                                          'HR', 'MDSYS', 'ORACLE_OCM', 'OUTLN', 'WMSYS', 'XDB', 'XS$NULL',
                                          'ANONYMOUS', 'BI', 'MDDATA', 'OE', 'PM', 'IX', 'SH')
                    GROUP BY t.owner, t.table_name
                ) sampled_tables
                """, 100.0 / adjustedPercentage);
        } else {
            // Sample specific schemas
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < schemaList.size(); i++) {
                if (i > 0) placeholders.append(", ");
                placeholders.append("?");
            }
            
            sql = String.format("""
                SELECT SUM(estimated_count) FROM (
                    SELECT COUNT(*) * %.1f as estimated_count
                    FROM all_tables t
                    WHERE t.owner IN (%s)
                    GROUP BY t.owner, t.table_name
                ) sampled_tables
                """, 100.0 / adjustedPercentage, placeholders);
        }
        
        log.info("Estimating total row count via sampling ({}%): {}", adjustedPercentage, sql);
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Set parameters for specific schemas
            if (!allSchemas && !schemaList.isEmpty()) {
                for (int i = 0; i < schemaList.size(); i++) {
                    stmt.setString(i + 1, schemaList.get(i).toUpperCase());
                }
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long estimatedTotal = rs.getLong(1);
                    log.info("Total estimated row count via sampling: {}", estimatedTotal);
                    return estimatedTotal;
                }
            }
        }
        
        return 0;
    }
    
    /**
     * Estimates row count using table segment size and average row length.
     * Falls back through multiple approaches if all_segments is not accessible.
     * 
     * @param conn Oracle database connection
     * @param owner table owner/schema
     * @param tableName table name
     * @return estimated row count based on segment size
     * @throws SQLException if database query fails
     */
    public static long estimateRowCountBySegmentSize(Connection conn, String owner, String tableName) throws SQLException {
        // Try 1: Use dba_segments (requires DBA privileges)
        try {
            String sql = """
                SELECT 
                    s.bytes,
                    t.avg_row_len
                FROM dba_segments s
                JOIN dba_tables t ON s.owner = t.owner AND s.segment_name = t.table_name
                WHERE s.owner = ? AND s.segment_name = ? AND s.segment_type = 'TABLE'
                """;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, owner.toUpperCase());
                stmt.setString(2, tableName.toUpperCase());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long bytes = rs.getLong("bytes");
                        long avgRowLen = rs.getLong("avg_row_len");
                        
                        if (avgRowLen > 0) {
                            long estimatedRows = bytes / avgRowLen;
                            log.debug("Estimated row count for {}.{} by dba_segments: {} bytes / {} avg_row_len = {} rows", 
                                     owner, tableName, bytes, avgRowLen, estimatedRows);
                            return estimatedRows;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.debug("dba_segments approach failed for {}.{}: {}", owner, tableName, e.getMessage());
        }
        
        // Try 2: Use table statistics only (fallback)
        try {
            String sql = """
                SELECT 
                    NVL(num_rows, 0),
                    NVL(blocks, 0),
                    NVL(avg_row_len, 0)
                FROM all_tables
                WHERE owner = ? AND table_name = ?
                """;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, owner.toUpperCase());
                stmt.setString(2, tableName.toUpperCase());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long numRows = rs.getLong(1);
                        long blocks = rs.getLong(2);
                        long avgRowLen = rs.getLong(3);
                        
                        if (numRows > 0) {
                            log.debug("Using table statistics for {}.{}: {} rows", owner, tableName, numRows);
                            return numRows;
                        } else if (blocks > 0 && avgRowLen > 0) {
                            // Estimate: Oracle block size is typically 8K, assume 80% utilization
                            long estimatedRows = (blocks * 8192 * 80 / 100) / avgRowLen;
                            log.debug("Estimated row count for {}.{} by blocks: {} blocks * 8192 * 0.8 / {} avg_row_len = {} rows", 
                                     owner, tableName, blocks, avgRowLen, estimatedRows);
                            return estimatedRows;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.debug("Table statistics approach failed for {}.{}: {}", owner, tableName, e.getMessage());
        }
        
        log.warn("All segment size estimation methods failed for {}.{}", owner, tableName);
        return 0;
    }
}