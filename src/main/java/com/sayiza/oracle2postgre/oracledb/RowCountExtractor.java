package com.sayiza.oracle2postgre.oracledb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class RowCountExtractor {

    private static final Logger log = LoggerFactory.getLogger(RowCountExtractor.class);

    /**
     * Calculates the total row count for tables in the specified schemas.
     * Uses Oracle's all_tables view to get approximate row counts via num_rows statistics.
     * 
     * @param conn Oracle database connection
     * @param allSchemas whether to include all schemas or limit to specific ones
     * @param schemaList list of specific schemas to include (used when allSchemas is false)
     * @return total row count across all tables in the specified schemas
     * @throws SQLException if database query fails
     */
    public static long calculateTotalRowCount(Connection conn, boolean allSchemas, List<String> schemaList) throws SQLException {
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
}