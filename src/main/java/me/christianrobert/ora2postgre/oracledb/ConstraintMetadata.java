package me.christianrobert.ora2postgre.oracledb;

import me.christianrobert.ora2postgre.plsql.ast.tools.managers.ConstraintTransformationManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a database constraint with comprehensive Oracle constraint support.
 * Supports PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK, and NOT NULL constraints.
 */
public class ConstraintMetadata {
  // Basic constraint information
  private String constraintName;
  private String constraintType;         // P, R, U, C, N (Oracle constraint types)
  private List<String> columnNames;
  
  // Foreign key specific fields
  private String referencedSchema;       // For foreign keys
  private String referencedTable;        // For foreign keys  
  private List<String> referencedColumns; // For foreign keys
  private String deleteRule;             // CASCADE, SET NULL, RESTRICT, NO ACTION
  private String updateRule;             // CASCADE, SET NULL, RESTRICT, NO ACTION
  
  // Check constraint specific fields
  private String checkCondition;         // For check constraints
  
  // Constraint state and properties
  private String status;                 // ENABLED, DISABLED
  private boolean deferrable;            // Deferrable constraint
  private boolean initiallyDeferred;     // Initially deferred
  private boolean validated;             // Constraint validated
  private String indexName;              // Associated index name
  
  // Constraint type constants
  public static final String PRIMARY_KEY = "P";
  public static final String FOREIGN_KEY = "R"; // Referential
  public static final String UNIQUE = "U";
  public static final String CHECK = "C";
  public static final String NOT_NULL = "N";
  
  // PostgreSQL constraint type mapping
  private static final Map<String, String> CONSTRAINT_TYPE_MAP = Map.of(
      PRIMARY_KEY, "PRIMARY KEY",
      FOREIGN_KEY, "FOREIGN KEY", 
      UNIQUE, "UNIQUE",
      CHECK, "CHECK"
  );
  
  // Delete/Update rule mapping
  private static final Map<String, String> REFERENTIAL_ACTION_MAP = Map.of(
      "CASCADE", "CASCADE",
      "SET NULL", "SET NULL",
      "RESTRICT", "RESTRICT",
      "NO ACTION", "NO ACTION"
  );

  /**
   * Constructor for basic constraints (PRIMARY KEY, UNIQUE, CHECK)
   */
  public ConstraintMetadata(String constraintName, String constraintType) {
    this.constraintName = constraintName;
    this.constraintType = constraintType;
    this.columnNames = new ArrayList<>();
    this.referencedColumns = new ArrayList<>();
    this.status = "ENABLED";
    this.deferrable = false;
    this.initiallyDeferred = false;
    this.validated = true;
  }
  
  /**
   * Constructor for foreign key constraints with referenced table information
   */
  public ConstraintMetadata(String constraintName, String constraintType, 
                           String referencedSchema, String referencedTable) {
    this(constraintName, constraintType);
    this.referencedSchema = referencedSchema;
    this.referencedTable = referencedTable;
  }

  // Column management methods
  public void addColumnName(String columnName) { 
    columnNames.add(columnName); 
  }
  
  public void addReferencedColumnName(String columnName) { 
    referencedColumns.add(columnName); 
  }

  // Basic getters
  public String getConstraintName() { return constraintName; }
  public String getConstraintType() { return constraintType; }
  public List<String> getColumnNames() { return columnNames; }
  
  // Foreign key getters
  public String getReferencedSchema() { return referencedSchema; }
  public String getReferencedTable() { return referencedTable; }
  public List<String> getReferencedColumns() { return referencedColumns; }
  public String getDeleteRule() { return deleteRule; }
  public String getUpdateRule() { return updateRule; }
  
  // Check constraint getters
  public String getCheckCondition() { return checkCondition; }
  
  // Constraint state getters
  public String getStatus() { return status; }
  public boolean isDeferrable() { return deferrable; }
  public boolean isInitiallyDeferred() { return initiallyDeferred; }
  public boolean isValidated() { return validated; }
  public String getIndexName() { return indexName; }
  
  // Setters for constraint properties
  public void setReferencedSchema(String referencedSchema) { this.referencedSchema = referencedSchema; }
  public void setReferencedTable(String referencedTable) { this.referencedTable = referencedTable; }
  public void setDeleteRule(String deleteRule) { this.deleteRule = deleteRule; }
  public void setUpdateRule(String updateRule) { this.updateRule = updateRule; }
  public void setCheckCondition(String checkCondition) { this.checkCondition = checkCondition; }
  public void setStatus(String status) { this.status = status; }
  public void setDeferrable(boolean deferrable) { this.deferrable = deferrable; }
  public void setInitiallyDeferred(boolean initiallyDeferred) { this.initiallyDeferred = initiallyDeferred; }
  public void setValidated(boolean validated) { this.validated = validated; }
  public void setIndexName(String indexName) { this.indexName = indexName; }

  // Constraint type checking methods
  public boolean isPrimaryKey() { return PRIMARY_KEY.equals(constraintType); }
  public boolean isForeignKey() { return FOREIGN_KEY.equals(constraintType); }
  public boolean isUniqueConstraint() { return UNIQUE.equals(constraintType); }
  public boolean isCheckConstraint() { return CHECK.equals(constraintType); }
  public boolean isNotNullConstraint() { return NOT_NULL.equals(constraintType); }
  
  /**
   * Gets the human-readable constraint type name
   */
  public String getConstraintTypeName() {
    return CONSTRAINT_TYPE_MAP.getOrDefault(constraintType, "UNKNOWN");
  }
  
  /**
   * Generates PostgreSQL constraint DDL for use in ALTER TABLE statements
   * @deprecated Use ConstraintTransformationManager instead for better strategy-based transformation
   */
  @Deprecated
  public String toPostgreConstraintDDL() {
    // Delegate to strategy manager for consistent transformation
    // Note: This method doesn't have access to Everything context, so we pass null
    // This is acceptable for backward compatibility as the original implementation
    // didn't use context either
    ConstraintTransformationManager manager = new ConstraintTransformationManager();
    return manager.transformConstraintDDL(this, null);
  }
  
  /**
   * Generates complete ALTER TABLE statement for this constraint
   * @deprecated Use ConstraintTransformationManager instead for better strategy-based transformation
   */
  @Deprecated
  public String toPostgreAlterTableDDL(String schemaName, String tableName) {
    // Delegate to strategy manager for consistent transformation
    ConstraintTransformationManager manager = new ConstraintTransformationManager();
    return manager.transformAlterTableDDL(this, schemaName, tableName, null);
  }
  
  /**
   * Validates that this constraint has all required fields for its type
   */
  public boolean isValid() {
    if (constraintName == null || constraintName.trim().isEmpty()) {
      return false;
    }
    if (constraintType == null || constraintType.trim().isEmpty()) {
      return false;
    }
    
    switch (constraintType) {
      case "P": // PRIMARY_KEY
      case "U": // UNIQUE
        return !columnNames.isEmpty();
        
      case "R": // FOREIGN_KEY
        return !columnNames.isEmpty() && referencedTable != null && !referencedTable.trim().isEmpty();
        
      case "C": // CHECK
        return checkCondition != null && !checkCondition.trim().isEmpty();
        
      default:
        return true; // For unknown types, assume valid
    }
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ConstraintMetadata{name='").append(constraintName)
      .append("', type='").append(constraintType)
      .append("', columns=").append(columnNames);
    
    if (isForeignKey()) {
      sb.append(", referencedTable='").append(referencedTable).append("'");
      if (!referencedColumns.isEmpty()) {
        sb.append(", referencedColumns=").append(referencedColumns);
      }
    }
    
    if (isCheckConstraint()) {
      sb.append(", checkCondition='").append(checkCondition).append("'");
    }
    
    sb.append(", status='").append(status).append("'");
    sb.append("}");
    return sb.toString();
  }
}