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
    
    // Transform package variables to PostgreSQL schema-level variables
    if (!oraclePackage.getVariables().isEmpty()) {
      b.append(generatePackageVariables(oraclePackage, context));
      b.append("\n");
    }
    
    // Transform package subtypes - currently TODO in original code
    for (SubType subtype : oraclePackage.getSubtypes()) {
      // TODO: Oracle subtypes might need to become PostgreSQL domains or composite types
      // This is currently a TODO in the original OraclePackage.toPostgre() method
    }
    
    // Transform package cursors - currently TODO in original code  
    for (Cursor cursor : oraclePackage.getCursors()) {
      // TODO: Package cursors might need special handling in PostgreSQL
      // This is currently a TODO in the original OraclePackage.toPostgre() method
    }
    
    // Transform package types - currently TODO in original code
    for (PackageType type : oraclePackage.getTypes()) {
      // TODO: Package types might need to become PostgreSQL composite types
      // This is currently a TODO in the original OraclePackage.toPostgre() method
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
      notes.append("; Package variables implemented as schema-level variables table with getter/setter functions");
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
    if (!oraclePackage.getBodyStatements().isEmpty()) {
      notes.append("; Body statements not yet passed to functions/procedures");
    }
    
    return notes.toString();
  }

  /**
   * Generates PostgreSQL DDL for package variables using a schema-level variables table approach.
   * This creates a custom table to store package-level variables and provides getter/setter functions.
   */
  private String generatePackageVariables(OraclePackage oraclePackage, Everything context) {
    StringBuilder b = new StringBuilder();
    String packageName = oraclePackage.getName().toLowerCase();
    String schemaName = oraclePackage.getSchema().toLowerCase();
    String variableTableName = packageName + "_variables";
    
    b.append("-- Package Variables for ").append(schemaName).append(".").append(packageName).append("\n");
    b.append("-- Implemented using PostgreSQL schema-level variables table\n\n");
    
    // Create variables table
    b.append("CREATE TABLE IF NOT EXISTS ").append(schemaName).append(".").append(variableTableName).append(" (\n");
    b.append("  variable_name VARCHAR(100) PRIMARY KEY,\n");
    b.append("  variable_value TEXT,\n");
    b.append("  variable_type VARCHAR(50),\n");
    b.append("  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n");
    b.append("  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n");
    b.append(");\n\n");
    
    // Create or replace initialization function
    b.append("CREATE OR REPLACE FUNCTION ").append(schemaName).append(".").append(packageName).append("_init_variables() \n");
    b.append("RETURNS VOID\n");
    b.append("LANGUAGE plpgsql AS $$\n");
    b.append("BEGIN\n");
    
    // Initialize each package variable
    for (Variable variable : oraclePackage.getVariables()) {
      String varName = variable.getName().toLowerCase();
      String varType = variable.getDataType().toPostgre(context);
      String defaultValue = "NULL";
      
      if (variable.getDefaultValue() != null) {
        defaultValue = "'" + variable.getDefaultValue().toPostgre(context).replace("'", "''") + "'";
      }
      
      b.append("  -- Initialize ").append(variable.getName()).append(" (").append(varType).append(")\n");
      b.append("  INSERT INTO ").append(schemaName).append(".").append(variableTableName).append(" \n");
      b.append("    (variable_name, variable_value, variable_type) \n");
      b.append("  VALUES ('").append(varName).append("', ").append(defaultValue).append(", '").append(varType).append("')\n");
      b.append("  ON CONFLICT (variable_name) DO UPDATE SET \n");
      b.append("    variable_value = EXCLUDED.variable_value,\n");
      b.append("    updated_at = CURRENT_TIMESTAMP;\n\n");
    }
    
    b.append("END;\n$$;\n\n");
    
    // Create getter and setter functions for each variable
    for (Variable variable : oraclePackage.getVariables()) {
      String varName = variable.getName().toLowerCase();
      String varType = variable.getDataType().toPostgre(context);
      
      // Getter function
      b.append("CREATE OR REPLACE FUNCTION ").append(schemaName).append(".").append(packageName).append("_get_").append(varName).append("() \n");
      b.append("RETURNS ").append(varType).append("\n");
      b.append("LANGUAGE plpgsql AS $$\n");
      b.append("DECLARE\n");
      b.append("  result ").append(varType).append(";\n");
      b.append("BEGIN\n");
      b.append("  SELECT variable_value::").append(varType).append(" INTO result \n");
      b.append("  FROM ").append(schemaName).append(".").append(variableTableName).append(" \n");
      b.append("  WHERE variable_name = '").append(varName).append("';\n");
      b.append("  \n");
      b.append("  IF NOT FOUND THEN\n");
      b.append("    -- Initialize if not found\n");
      b.append("    PERFORM ").append(schemaName).append(".").append(packageName).append("_init_variables();\n");
      b.append("    SELECT variable_value::").append(varType).append(" INTO result \n");
      b.append("    FROM ").append(schemaName).append(".").append(variableTableName).append(" \n");
      b.append("    WHERE variable_name = '").append(varName).append("';\n");
      b.append("  END IF;\n");
      b.append("  \n");
      b.append("  RETURN result;\n");
      b.append("END;\n$$;\n\n");
      
      // Setter function
      b.append("CREATE OR REPLACE FUNCTION ").append(schemaName).append(".").append(packageName).append("_set_").append(varName).append("(new_value ").append(varType).append(") \n");
      b.append("RETURNS VOID\n");
      b.append("LANGUAGE plpgsql AS $$\n");
      b.append("BEGIN\n");
      b.append("  UPDATE ").append(schemaName).append(".").append(variableTableName).append(" \n");
      b.append("  SET variable_value = new_value::TEXT, updated_at = CURRENT_TIMESTAMP \n");
      b.append("  WHERE variable_name = '").append(varName).append("';\n");
      b.append("  \n");
      b.append("  IF NOT FOUND THEN\n");
      b.append("    -- Initialize if not found\n");
      b.append("    PERFORM ").append(schemaName).append(".").append(packageName).append("_init_variables();\n");
      b.append("    UPDATE ").append(schemaName).append(".").append(variableTableName).append(" \n");
      b.append("    SET variable_value = new_value::TEXT, updated_at = CURRENT_TIMESTAMP \n");
      b.append("    WHERE variable_name = '").append(varName).append("';\n");
      b.append("  END IF;\n");
      b.append("END;\n$$;\n\n");
    }
    
    // Initialize variables on first creation
    b.append("-- Initialize package variables\n");
    b.append("SELECT ").append(schemaName).append(".").append(packageName).append("_init_variables();\n\n");
    
    return b.toString();
  }
}