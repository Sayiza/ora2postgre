package me.christianrobert.ora2postgre.plsql.ast.tools;

import me.christianrobert.ora2postgre.oracledb.IndexMetadata;
import me.christianrobert.ora2postgre.oracledb.IndexColumn;
import me.christianrobert.ora2postgre.writing.PostgreSQLIndexDDL;

/**
 * Strategy for converting Oracle composite (multi-column) indexes to PostgreSQL.
 * Handles indexes with multiple columns while preserving column order and sort directions.
 */
public class CompositeIndexStrategy implements IndexMigrationStrategy {
    
    @Override
    public boolean supports(IndexMetadata index) {
        // Support composite indexes that are not functional and have multiple columns
        return index.isComposite() && 
               !index.isFunctional() && 
               index.isValid() &&
               index.getColumns().size() > 1;
    }
    
    @Override
    public PostgreSQLIndexDDL convert(IndexMetadata index) {
        if (!supports(index)) {
            throw new UnsupportedOperationException("Index not supported by CompositeIndexStrategy: " + index.getIndexName());
        }
        
        StringBuilder sql = new StringBuilder();
        
        // Build CREATE INDEX statement (unique if needed)
        sql.append("CREATE ");
        if (index.isUniqueIndex()) {
            sql.append("UNIQUE ");
        }
        sql.append("INDEX ");
        
        // Generate PostgreSQL-compatible index name (handle length limit)
        String pgIndexName = generatePostgreSQLIndexName(index);
        sql.append(pgIndexName);
        
        // ON clause with table name
        sql.append(" ON ");
        if (index.getSchemaName() != null) {
            sql.append(index.getSchemaName().toLowerCase()).append(".");
        }
        sql.append(index.getTableName().toLowerCase());
        
        // Column list - preserve order from Oracle
        sql.append(" (");
        for (int i = 0; i < index.getColumns().size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            IndexColumn column = index.getColumns().get(i);
            sql.append(column.getColumnName().toLowerCase());
            
            // Add sort order if specified
            if (column.isDescending()) {
                sql.append(" DESC");
            }
        }
        sql.append(")");
        
        // Add tablespace if specified and not default
        if (index.getTablespace() != null && 
            !index.getTablespace().trim().isEmpty() && 
            !"USERS".equalsIgnoreCase(index.getTablespace()) &&
            !"SYSTEM".equalsIgnoreCase(index.getTablespace())) {
            sql.append(" TABLESPACE ").append(index.getTablespace().toLowerCase());
        }
        
        String conversionNotes = generateConversionNotes(index);
        
        return new PostgreSQLIndexDDL(
            sql.toString(),
            pgIndexName,
            index.getIndexName(),
            index.getTableName(),
            index.getSchemaName(),
            "POST_TRANSFER_INDEXES",
            conversionNotes
        );
    }
    
    @Override
    public String getStrategyName() {
        return "Composite Index";
    }
    
    @Override
    public int getPriority() {
        return 15; // Medium priority, between unique and regular B-tree
    }
    
    /**
     * Generates a PostgreSQL-compatible index name, handling the 63-character limit.
     */
    private String generatePostgreSQLIndexName(IndexMetadata index) {
        String originalName = index.getIndexName().toLowerCase();
        
        // PostgreSQL identifier limit is 63 characters
        if (originalName.length() <= 63) {
            return originalName;
        }
        
        // Truncate and add hash to ensure uniqueness
        String baseNameTruncated = originalName.substring(0, 55);
        String hash = String.format("%08x", originalName.hashCode()).substring(0, 7);
        return baseNameTruncated + "_" + hash;
    }
    
    /**
     * Generates conversion notes based on the index characteristics.
     */
    private String generateConversionNotes(IndexMetadata index) {
        StringBuilder notes = new StringBuilder();
        
        notes.append("Composite index with ").append(index.getColumns().size()).append(" columns");
        
        // List column names for reference
        if (index.getColumns().size() <= 5) { // Don't list too many columns
            notes.append(" (");
            for (int i = 0; i < index.getColumns().size(); i++) {
                if (i > 0) notes.append(", ");
                notes.append(index.getColumns().get(i).getColumnName().toLowerCase());
            }
            notes.append(")");
        }
        
        // Check for descending columns
        long descendingColumns = index.getColumns().stream()
            .mapToLong(col -> col.isDescending() ? 1 : 0)
            .sum();
        
        if (descendingColumns > 0) {
            notes.append("; ").append(descendingColumns).append(" descending column(s)");
        }
        
        // Check for mixed sort orders (some ASC, some DESC)
        boolean hasAscending = index.getColumns().stream().anyMatch(col -> !col.isDescending());
        boolean hasDescending = index.getColumns().stream().anyMatch(IndexColumn::isDescending);
        
        if (hasAscending && hasDescending) {
            notes.append("; Mixed sort orders");
        }
        
        // Check if unique
        if (index.isUniqueIndex()) {
            notes.append("; Enforces uniqueness constraint");
        }
        
        // Check if name was truncated
        if (index.getIndexName().length() > 63) {
            notes.append("; Name truncated due to PostgreSQL 63-char limit");
        }
        
        // Check for unusual tablespace
        if (index.getTablespace() != null && 
            !index.getTablespace().trim().isEmpty() && 
            !"USERS".equalsIgnoreCase(index.getTablespace()) &&
            !"SYSTEM".equalsIgnoreCase(index.getTablespace())) {
            notes.append("; Custom tablespace: ").append(index.getTablespace());
        }
        
        return notes.toString();
    }
    
    @Override
    public String getConversionNotes(IndexMetadata index) {
        return generateConversionNotes(index);
    }
}