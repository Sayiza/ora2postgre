package com.sayiza.oracle2postgre.oracledb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Utility class for checking Oracle table statistics freshness.
 */
public class StatisticsFreshness {
    
    private static final Logger log = LoggerFactory.getLogger(StatisticsFreshness.class);
    
    /**
     * Data class to hold statistics freshness information.
     */
    public static class StatisticsFreshnessInfo {
        private final String tableName;
        private final String owner;
        private final long numRows;
        private final long modificationsSinceAnalyze;
        private final boolean isStale;
        private final double stalenessRatio;
        
        public StatisticsFreshnessInfo(String tableName, String owner, long numRows, 
                                     long modificationsSinceAnalyze, boolean isStale, double stalenessRatio) {
            this.tableName = tableName;
            this.owner = owner;
            this.numRows = numRows;
            this.modificationsSinceAnalyze = modificationsSinceAnalyze;
            this.isStale = isStale;
            this.stalenessRatio = stalenessRatio;
        }
        
        public String getTableName() { return tableName; }
        public String getOwner() { return owner; }
        public long getNumRows() { return numRows; }
        public long getModificationsSinceAnalyze() { return modificationsSinceAnalyze; }
        public boolean isStale() { return isStale; }
        public double getStalenessRatio() { return stalenessRatio; }
    }
    
    /**
     * Checks if table statistics are stale based on modifications since last analyze.
     * 
     * @param conn Oracle database connection
     * @param owner table owner/schema
     * @param tableName table name
     * @param stalenessThreshold threshold for considering statistics stale (0.0-1.0)
     * @return statistics freshness information
     * @throws SQLException if database query fails
     */
    public static StatisticsFreshnessInfo checkStatisticsFreshness(Connection conn, String owner, 
                                                                  String tableName, double stalenessThreshold) throws SQLException {
        
        String sql = """
            SELECT 
                t.num_rows,
                NVL(m.inserts + m.updates + m.deletes, 0) as modifications_since_analyze
            FROM all_tables t
            LEFT JOIN all_tab_modifications m ON t.owner = m.table_owner AND t.table_name = m.table_name
            WHERE t.owner = ? AND t.table_name = ?
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, owner.toUpperCase());
            stmt.setString(2, tableName.toUpperCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long numRows = rs.getLong("num_rows");
                    long modifications = rs.getLong("modifications_since_analyze");
                    
                    // Handle case where num_rows is 0 or null
                    double stalenessRatio = numRows > 0 ? (double) modifications / numRows : 0.0;
                    boolean isStale = stalenessRatio > stalenessThreshold;
                    
                    log.debug("Statistics freshness for {}.{}: num_rows={}, modifications={}, ratio={:.3f}, stale={}", 
                             owner, tableName, numRows, modifications, stalenessRatio, isStale);
                    
                    return new StatisticsFreshnessInfo(tableName, owner, numRows, modifications, isStale, stalenessRatio);
                }
            }
        }
        
        // Table not found or no statistics
        log.warn("No statistics found for table {}.{}", owner, tableName);
        return new StatisticsFreshnessInfo(tableName, owner, 0, 0, true, 1.0);
    }
    
    /**
     * Batch check statistics freshness for multiple tables.
     * 
     * @param conn Oracle database connection
     * @param owner table owner/schema
     * @param tableNames array of table names
     * @param stalenessThreshold threshold for considering statistics stale
     * @return array of statistics freshness information
     * @throws SQLException if database query fails
     */
    /*
    public static StatisticsFreshnessInfo[] checkStatisticsFreshnessBatch(Connection conn, String owner, 
                                                                         String[] tableNames, double stalenessThreshold) throws SQLException {
        
        if (tableNames.length == 0) {
            return new StatisticsFreshnessInfo[0];
        }
        
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < tableNames.length; i++) {
            if (i > 0) placeholders.append(", ");
            placeholders.append("?");
        }
        
        String sql = """
            SELECT 
                t.table_name,
                t.num_rows,
                NVL(m.inserts + m.updates + m.deletes, 0) as modifications_since_analyze
            FROM all_tables t
            LEFT JOIN all_tab_modifications m ON t.owner = m.table_owner AND t.table_name = m.table_name
            WHERE t.owner = ? AND t.table_name IN (""" + placeholders + ")";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, owner.toUpperCase());
            for (int i = 0; i < tableNames.length; i++) {
                stmt.setString(i + 2, tableNames[i].toUpperCase());
            }
            
            StatisticsFreshnessInfo[] results = new StatisticsFreshnessInfo[tableNames.length];
            
            try (ResultSet rs = stmt.executeQuery()) {
                int index = 0;
                while (rs.next() && index < tableNames.length) {
                    String tableName = rs.getString("table_name");
                    long numRows = rs.getLong("num_rows");
                    long modifications = rs.getLong("modifications_since_analyze");
                    
                    double stalenessRatio = numRows > 0 ? (double) modifications / numRows : 0.0;
                    boolean isStale = stalenessRatio > stalenessThreshold;
                    
                    results[index] = new StatisticsFreshnessInfo(tableName, owner, numRows, modifications, isStale, stalenessRatio);
                    index++;
                }
            }
            
            return results;
        }
    }*/
}