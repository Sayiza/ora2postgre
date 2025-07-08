package me.christianrobert.ora2postgre.oracledb;

import java.util.List;
import java.util.ArrayList;

/**
 * Metadata class representing an Oracle database index.
 * Contains all information needed to extract, analyze, and convert indexes to PostgreSQL.
 */
public class IndexMetadata {
    private final String indexName;
    private final String tableName;
    private final String schemaName;
    private final String indexType;
    private final boolean uniqueIndex;
    private final boolean partialIndex;
    private final String whereClause; // For partial indexes
    private final String tablespace;
    private final List<IndexColumn> columns;
    private final String status; // VALID, UNUSABLE, etc.
    private final boolean partitioned;
    
    public IndexMetadata(String indexName, String tableName, String schemaName, 
                        String indexType, boolean uniqueIndex, boolean partialIndex,
                        String whereClause, String tablespace, String status, boolean partitioned) {
        this.indexName = indexName;
        this.tableName = tableName;
        this.schemaName = schemaName;
        this.indexType = indexType;
        this.uniqueIndex = uniqueIndex;
        this.partialIndex = partialIndex;
        this.whereClause = whereClause;
        this.tablespace = tablespace;
        this.status = status;
        this.partitioned = partitioned;
        this.columns = new ArrayList<>();
    }
    
    // Getters
    public String getIndexName() {
        return indexName;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public String getSchemaName() {
        return schemaName;
    }
    
    public String getIndexType() {
        return indexType;
    }
    
    public boolean isUniqueIndex() {
        return uniqueIndex;
    }
    
    public boolean isPartialIndex() {
        return partialIndex;
    }
    
    public String getWhereClause() {
        return whereClause;
    }
    
    public String getTablespace() {
        return tablespace;
    }
    
    public List<IndexColumn> getColumns() {
        return columns;
    }
    
    public String getStatus() {
        return status;
    }
    
    public boolean isPartitioned() {
        return partitioned;
    }
    
    // Utility methods
    public void addColumn(IndexColumn column) {
        this.columns.add(column);
    }
    
    public boolean isComposite() {
        return columns.size() > 1;
    }
    
    public boolean isFunctional() {
        return columns.stream().anyMatch(col -> col.getColumnExpression() != null);
    }
    
    public boolean isBitmap() {
        return "BITMAP".equalsIgnoreCase(indexType);
    }
    
    public boolean isNormal() {
        return "NORMAL".equalsIgnoreCase(indexType);
    }
    
    public boolean isValid() {
        return "VALID".equalsIgnoreCase(status);
    }
    
    /**
     * Gets the full qualified name of the index including schema.
     */
    public String getFullName() {
        return schemaName != null ? schemaName + "." + indexName : indexName;
    }
    
    /**
     * Gets the full qualified table name including schema.
     */
    public String getFullTableName() {
        return schemaName != null ? schemaName + "." + tableName : tableName;
    }
    
    /**
     * Determines if this index is easily convertible to PostgreSQL.
     * Returns true for B-tree, unique, partial, and composite indexes.
     */
    public boolean isEasilyConvertible() {
        // Check if it's a supported index type
        if (isBitmap() || "FUNCTION-BASED NORMAL".equalsIgnoreCase(indexType) || 
            "DOMAIN".equalsIgnoreCase(indexType) || "CLUSTER".equalsIgnoreCase(indexType)) {
            return false;
        }
        
        // Check if it's a complex functional index
        if (isFunctional()) {
            // Simple functional indexes might be convertible, but complex ones are not
            // For now, mark all functional indexes as not easily convertible
            return false;
        }
        
        // Check if it's a valid index
        if (!isValid()) {
            return false;
        }
        
        // Normal B-tree indexes, unique indexes, partial indexes, and composite indexes are convertible
        return isNormal() || uniqueIndex || partialIndex || isComposite();
    }
    
    /**
     * Gets the reason why this index is not easily convertible.
     */
    public String getConversionIssue() {
        if (!isValid()) {
            return "Index status is " + status + " (not VALID)";
        }
        
        if (isBitmap()) {
            return "Bitmap indexes are not supported in PostgreSQL (use B-tree instead)";
        }
        
        if ("FUNCTION-BASED NORMAL".equalsIgnoreCase(indexType)) {
            return "Function-based indexes require manual expression transformation";
        }
        
        if ("DOMAIN".equalsIgnoreCase(indexType)) {
            return "Domain indexes are Oracle-specific and not supported in PostgreSQL";
        }
        
        if ("CLUSTER".equalsIgnoreCase(indexType)) {
            return "Cluster indexes work differently in PostgreSQL (use CLUSTER command)";
        }
        
        if (isFunctional()) {
            return "Functional indexes with complex expressions need manual review";
        }
        
        return "Unknown conversion issue";
    }
    
    @Override
    public String toString() {
        return "IndexMetadata{" +
               "indexName='" + indexName + '\'' +
               ", tableName='" + tableName + '\'' +
               ", schemaName='" + schemaName + '\'' +
               ", indexType='" + indexType + '\'' +
               ", uniqueIndex=" + uniqueIndex +
               ", partialIndex=" + partialIndex +
               ", columns=" + columns.size() +
               ", status='" + status + '\'' +
               '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        IndexMetadata that = (IndexMetadata) obj;
        return indexName.equals(that.indexName) && 
               schemaName.equals(that.schemaName);
    }
    
    @Override
    public int hashCode() {
        return indexName.hashCode() * 31 + (schemaName != null ? schemaName.hashCode() : 0);
    }
}