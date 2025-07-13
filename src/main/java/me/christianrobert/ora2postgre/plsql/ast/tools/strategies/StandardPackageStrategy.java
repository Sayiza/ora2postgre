package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standard implementation for transforming Oracle packages to PostgreSQL.
 * This strategy handles most common Oracle package patterns and converts them
 * to PostgreSQL-compatible CREATE FUNCTION/PROCEDURE statements.
 */
public class StandardPackageStrategy implements PackageTransformationStrategy {

  private static final Logger log = LoggerFactory.getLogger(StandardPackageStrategy.class);

  @Override
  public boolean supports(OraclePackage oraclePackage) {
    // Standard strategy supports all packages as a fallback
    return true;
  }

  @Override
  public String transform(OraclePackage oraclePackage, Everything context, boolean specOnly) {
    log.debug("Transforming package {}.{} using StandardPackageStrategy (specOnly={})",
            oraclePackage.getSchema(), oraclePackage.getName(), specOnly);

    StringBuilder b = new StringBuilder();
    
    // Transform collection types to PostgreSQL domain types FIRST (before variables that reference them)
    // Only generate collection types in the spec phase (specOnly=true) to avoid duplication
    if (specOnly && (!oraclePackage.getVarrayTypes().isEmpty() || !oraclePackage.getNestedTableTypes().isEmpty())) {
      b.append(generateCollectionTypes(oraclePackage, context));
      b.append("\n");
    }
    
    // Transform package variables to PostgreSQL schema-level variables
    // Only generate variables in the spec phase (specOnly=true) to avoid duplication
    if (specOnly && !oraclePackage.getVariables().isEmpty()) {
      b.append(generatePackageVariables(oraclePackage, context));
      b.append("\n");
    }
    
    // Transform package subtypes - currently TODO in original code
    // Only generate subtypes in the spec phase (specOnly=true) to avoid duplication
    if (specOnly) {
      for (SubType subtype : oraclePackage.getSubtypes()) {
        // TODO: Oracle subtypes might need to become PostgreSQL domains or composite types
        // This is currently a TODO in the original OraclePackage.toPostgre() method
      }
    }
    
    // Transform package cursors - currently TODO in original code
    // Only generate cursors in the spec phase (specOnly=true) to avoid duplication
    if (specOnly) {
      for (Cursor cursor : oraclePackage.getCursors()) {
        // TODO: Package cursors might need special handling in PostgreSQL
        // This is currently a TODO in the original OraclePackage.toPostgre() method
      }
    }
    
    // Transform package types - currently TODO in original code
    // Only generate types in the spec phase (specOnly=true) to avoid duplication
    if (specOnly) {
      for (PackageType type : oraclePackage.getTypes()) {
        // TODO: Package types might need to become PostgreSQL composite types
        // This is currently a TODO in the original OraclePackage.toPostgre() method
      }
    }
    
    // Transform record types to PostgreSQL composite types
    // Only generate record types in the spec phase (specOnly=true) to avoid duplication
    if (specOnly && !oraclePackage.getRecordTypes().isEmpty()) {
      b.append(generateRecordTypes(oraclePackage, context));
      b.append("\n");
    }
    
    // Transform functions
    for (Function function : oraclePackage.getFunctions()) {
      b.append(function.toPostgre(context, specOnly));
      b.append("\n\n");
    }
    
    // Transform procedures
    for (Procedure procedure : oraclePackage.getProcedures()) {
      b.append(procedure.toPostgre(context, specOnly));
      b.append("\n\n");
    }
    
    // TODO: Body statements need to be passed to functions and procedures
    // This is currently a TODO in the original OraclePackage.toPostgre() method
    
    log.debug("Successfully transformed package {}.{} with {} functions and {} procedures",
            oraclePackage.getSchema(), oraclePackage.getName(), 
            oraclePackage.getFunctions().size(), oraclePackage.getProcedures().size());

    return b.toString();
  }

  @Override
  public String getStrategyName() {
    return "Standard Package";
  }

  @Override
  public int getPriority() {
    // Standard strategy has lowest priority as it's the fallback
    return 0;
  }

  @Override
  public String getConversionNotes(OraclePackage oraclePackage) {
    StringBuilder notes = new StringBuilder("Converted using standard package transformation");
    
    // Add notes about implemented transformations
    if (!oraclePackage.getVariables().isEmpty()) {
      notes.append("; Package variables implemented as session-specific temporary tables (one per variable)");
    }
    if (!oraclePackage.getSubtypes().isEmpty()) {
      notes.append("; Package subtypes not yet implemented");  
    }
    if (!oraclePackage.getCursors().isEmpty()) {
      notes.append("; Package cursors not yet implemented");
    }
    if (!oraclePackage.getTypes().isEmpty()) {
      notes.append("; Package types not yet implemented");
    }
    if (!oraclePackage.getRecordTypes().isEmpty()) {
      notes.append("; Package record types implemented as PostgreSQL composite types");
    }
    if (!oraclePackage.getBodyStatements().isEmpty()) {
      notes.append("; Body statements not yet passed to functions/procedures");
    }
    
    return notes.toString();
  }

  /**
   * Generates PostgreSQL DDL for package variables using session-specific temporary tables approach.
   * This creates one temporary table per variable for session isolation and direct type safety.
   */
  private String generatePackageVariables(OraclePackage oraclePackage, Everything context) {
    StringBuilder b = new StringBuilder();
    String packageName = oraclePackage.getName().toLowerCase();
    
    b.append("-- Package Variables for ").append(oraclePackage.getSchema()).append(".").append(packageName).append("\n");
    b.append("-- Implemented using PostgreSQL session-specific temporary tables (one per variable)\n\n");
    
    // Create one temporary table per package variable
    for (Variable variable : oraclePackage.getVariables()) {
      String varName = variable.getName().toLowerCase();
      String schemaName = oraclePackage.getSchema().toLowerCase();
      String varType = variable.getDataType().toPostgre(context, oraclePackage.getSchema(), oraclePackage.getName());
      String tableName = schemaName + "_" + packageName + "_" + varName;
      
      b.append("-- Temporary table for variable: ").append(variable.getName()).append("\n");
      b.append("CREATE TEMPORARY TABLE ").append(tableName).append(" (\n");
      b.append("  value ").append(varType);
      
      // Add default value if present
      if (variable.getDefaultValue() != null) {
        String defaultValue = variable.getDefaultValue().toPostgre(context);
        // Handle different default value types appropriately
        if (needsQuotes(variable.getDataType(), defaultValue)) {
          b.append(" DEFAULT '").append(defaultValue.replace("'", "''")).append("'");
        } else {
          b.append(" DEFAULT ").append(defaultValue);
        }
      }
      
      b.append("\n) ON COMMIT PRESERVE ROWS;\n");
      
      // Insert initial value
      b.append("INSERT INTO ").append(tableName).append(" VALUES (DEFAULT);\n\n");
    }
    
    b.append("-- Variable Access Pattern:\n");
    b.append("-- Read:  SELECT value FROM schema_packagename_variablename LIMIT 1;\n");
    b.append("-- Write: UPDATE schema_packagename_variablename SET value = new_value;\n\n");
    
    return b.toString();
  }
  
  /**
   * Determines if a default value needs to be quoted based on the data type.
   */
  private boolean needsQuotes(DataTypeSpec dataType, String defaultValue) {
    String pgType = dataType.toPostgre(null).toLowerCase();
    
    // Don't quote numeric types, booleans, or expressions
    if (pgType.contains("numeric") || pgType.contains("integer") || pgType.contains("decimal") ||
        pgType.contains("boolean") || pgType.contains("bool") ||
        defaultValue.equalsIgnoreCase("true") || defaultValue.equalsIgnoreCase("false") ||
        defaultValue.equalsIgnoreCase("null") ||
        isNumericExpression(defaultValue)) {
      return false;
    }
    
    // Quote text types and dates
    return true;
  }
  
  /**
   * Simple check for numeric expressions that shouldn't be quoted.
   */
  private boolean isNumericExpression(String value) {
    // Simple patterns for numeric expressions
    return value.matches("^[0-9+\\-*/\\s().]+$") && !value.trim().isEmpty();
  }
  
  /**
   * Generates PostgreSQL DDL for package record types using CREATE TYPE statements.
   * This creates PostgreSQL composite types that can be used within package functions and procedures.
   */
  private String generateRecordTypes(OraclePackage oraclePackage, Everything context) {
    StringBuilder b = new StringBuilder();
    String packageName = oraclePackage.getName().toLowerCase();
    String schemaName = oraclePackage.getSchema().toLowerCase();
    
    b.append("-- Record Types for ").append(oraclePackage.getSchema()).append(".").append(packageName).append("\n");
    b.append("-- Implemented using PostgreSQL composite types\n\n");
    
    for (RecordType recordType : oraclePackage.getRecordTypes()) {
      String typeName = schemaName + "_" + packageName + "_" + recordType.getName().toLowerCase();
      
      b.append("-- Record type: ").append(recordType.getName()).append("\n");
      
      // Get the PostgreSQL DDL and replace the type name to include package prefix
      String recordTypeDDL = recordType.toPostgre(context);
      if (recordTypeDDL.contains("CREATE TYPE " + recordType.getName().toLowerCase() + " AS")) {
        recordTypeDDL = recordTypeDDL.replace("CREATE TYPE " + recordType.getName().toLowerCase() + " AS", 
                                              "CREATE TYPE " + typeName + " AS");
      }
      
      b.append(recordTypeDDL);
      b.append("\n\n");
    }
    
    b.append("-- Usage Pattern:\n");
    b.append("-- DECLARE variable_name schema_packagename_recordtypename;\n");
    b.append("-- Example: DECLARE emp_rec test_schema_emp_pkg_employee_record;\n\n");
    
    return b.toString();
  }

  private String generateCollectionTypes(OraclePackage oraclePackage, Everything context) {
    StringBuilder b = new StringBuilder();
    String packageName = oraclePackage.getName().toLowerCase();
    String schemaName = oraclePackage.getSchema().toLowerCase();
    
    b.append("-- Collection Types for ").append(oraclePackage.getSchema()).append(".").append(packageName).append("\n");
    b.append("-- Implemented using PostgreSQL domain types\n\n");
    
    // Generate VARRAY types
    for (VarrayType varrayType : oraclePackage.getVarrayTypes()) {
      String domainName = schemaName + "_" + packageName + "_" + varrayType.getName().toLowerCase();
      
      b.append("-- VARRAY type: ").append(varrayType.getName()).append("\n");
      b.append("CREATE DOMAIN ").append(domainName).append(" AS ").append(varrayType.toPostgre(context)).append(";\n\n");
    }
    
    // Generate TABLE OF types
    for (NestedTableType nestedTableType : oraclePackage.getNestedTableTypes()) {
      String domainName = schemaName + "_" + packageName + "_" + nestedTableType.getName().toLowerCase();
      
      b.append("-- TABLE OF type: ").append(nestedTableType.getName()).append("\n");
      b.append("CREATE DOMAIN ").append(domainName).append(" AS ").append(nestedTableType.toPostgre(context)).append(";\n\n");
    }
    
    b.append("-- Usage Pattern:\n");
    b.append("-- DECLARE variable_name schema_packagename_collectiontypename;\n");
    b.append("-- Example: DECLARE str_array test_schema_pkg_string_array;\n");
    b.append("-- Access elements: str_array[1], array_length(str_array, 1)\n\n");
    
    return b.toString();
  }
}