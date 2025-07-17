package me.christianrobert.ora2postgre.plsql.ast.tools.helpers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;

import java.util.*;

/**
 * Helper class for managing regular package variable materialization.
 * This handles the conversion of package-level regular variables (NUMBER, VARCHAR2, BOOLEAN, etc.) 
 * to function-local variables with proper prologue/epilogue generation.
 * 
 * Pattern: Package variables are stored in temporary tables and materialized as local variables
 * in functions/procedures through PRE/POST phases for transparent access.
 */
public class PackageVariableHelper {

  /**
   * Represents a regular package variable that needs materialization.
   */
  public static class PackageVariable {
    private final String variableName;
    private final String tableName;
    private final DataTypeSpec dataType;
    private final String postgresType;
    
    public PackageVariable(String variableName, String tableName, DataTypeSpec dataType, String postgresType) {
      this.variableName = variableName;
      this.tableName = tableName;
      this.dataType = dataType;
      this.postgresType = postgresType;
    }
    
    public String getVariableName() { return variableName; }
    public String getTableName() { return tableName; }
    public DataTypeSpec getDataType() { return dataType; }
    public String getPostgresType() { return postgresType; }
  }

  /**
   * Analyzes a function to determine which regular package variables
   * are used but not shadowed by local declarations.
   * 
   * @param function The function to analyze
   * @param parentPackage The parent package containing variables
   * @param context The Everything context for type resolution
   * @return List of package variables that need materialization
   */
  public static List<PackageVariable> analyzePackageVariables(
      Function function, OraclePackage parentPackage, Everything context) {
    
    if (parentPackage == null) {
      return Collections.emptyList();
    }
    
    List<PackageVariable> result = new ArrayList<>();
    
    // Get all local variable names to detect shadowing
    Set<String> localVariableNames = new HashSet<>();
    if (function.getVariables() != null) {
      for (Variable localVar : function.getVariables()) {
        localVariableNames.add(localVar.getName().toLowerCase());
      }
    }
    
    // Check package variables for regular (non-collection) types
    for (Variable packageVar : parentPackage.getVariables()) {
      String varName = packageVar.getName().toLowerCase();
      
      // Skip if this variable is shadowed by a local declaration
      if (localVariableNames.contains(varName)) {
        continue;
      }
      
      // Check if this is a regular variable (not a collection type)
      DataTypeSpec dataType = packageVar.getDataType();
      if (isRegularVariable(dataType, parentPackage)) {
        String tableName = generateTableName(parentPackage.getSchema(), 
                                           parentPackage.getName(), varName);
        String postgresType = dataType.toPostgre(context, parentPackage.getSchema(), parentPackage.getName());
        
        result.add(new PackageVariable(varName, tableName, dataType, postgresType));
      }
    }
    
    return result;
  }
  
  /**
   * Analyzes a procedure to determine which regular package variables
   * are used but not shadowed by local declarations.
   */
  public static List<PackageVariable> analyzePackageVariables(
      Procedure procedure, OraclePackage parentPackage, Everything context) {
    
    if (parentPackage == null) {
      return Collections.emptyList();
    }
    
    List<PackageVariable> result = new ArrayList<>();
    
    // Get all local variable names to detect shadowing
    Set<String> localVariableNames = new HashSet<>();
    if (procedure.getVariables() != null) {
      for (Variable localVar : procedure.getVariables()) {
        localVariableNames.add(localVar.getName().toLowerCase());
      }
    }
    
    // Check package variables for regular (non-collection) types
    for (Variable packageVar : parentPackage.getVariables()) {
      String varName = packageVar.getName().toLowerCase();
      
      // Skip if this variable is shadowed by a local declaration
      if (localVariableNames.contains(varName)) {
        continue;
      }
      
      // Check if this is a regular variable (not a collection type)
      DataTypeSpec dataType = packageVar.getDataType();
      if (isRegularVariable(dataType, parentPackage)) {
        String tableName = generateTableName(parentPackage.getSchema(), 
                                           parentPackage.getName(), varName);
        String postgresType = dataType.toPostgre(context, parentPackage.getSchema(), parentPackage.getName());
        
        result.add(new PackageVariable(varName, tableName, dataType, postgresType));
      }
    }
    
    return result;
  }

  /**
   * Generates the function prologue that materializes package variables
   * into local variables by loading them from temporary tables.
   * 
   * @param packageVariables List of package variables to materialize
   * @return Generated PostgreSQL prologue code
   */
  public static String generatePrologue(List<PackageVariable> packageVariables) {
    if (packageVariables.isEmpty()) {
      return "";
    }
    
    StringBuilder b = new StringBuilder();
    b.append("  -- Auto-generated prologue: materialize package variables\n");
    
    for (PackageVariable variable : packageVariables) {
      b.append("  -- Materialize package variable: ").append(variable.getVariableName()).append("\n");
      b.append("  SELECT value INTO ").append(variable.getVariableName()).append("\n");
      b.append("  FROM ").append(variable.getTableName()).append(" LIMIT 1;\n");
      b.append("\n");
    }
    
    return b.toString();
  }

  /**
   * Generates the function epilogue that persists local variables
   * back to package variable temporary tables.
   * 
   * @param packageVariables List of package variables to persist
   * @return Generated PostgreSQL epilogue code
   */
  public static String generateEpilogue(List<PackageVariable> packageVariables) {
    if (packageVariables.isEmpty()) {
      return "";
    }
    
    StringBuilder b = new StringBuilder();
    b.append("  -- Auto-generated epilogue: persist package variables\n");
    
    for (PackageVariable variable : packageVariables) {
      b.append("  -- Persist package variable: ").append(variable.getVariableName()).append("\n");
      b.append("  UPDATE ").append(variable.getTableName()).append("\n");
      b.append("  SET value = ").append(variable.getVariableName()).append(";\n");
      b.append("\n");
    }
    
    return b.toString();
  }

  /**
   * Generates variable declarations for package variables
   * that need to be materialized as local variables.
   * 
   * @param packageVariables List of package variables to declare
   * @return Generated PostgreSQL variable declarations
   */
  public static String generateVariableDeclarations(List<PackageVariable> packageVariables) {
    if (packageVariables.isEmpty()) {
      return "";
    }
    
    StringBuilder b = new StringBuilder();
    b.append("  -- Auto-generated declarations for package variables\n");
    
    for (PackageVariable variable : packageVariables) {
      b.append("  ").append(variable.getVariableName()).append(" ").append(variable.getPostgresType()).append(";\n");
    }
    b.append("\n");
    
    return b.toString();
  }

  /**
   * Determines if a variable is a regular (non-collection) type that should be handled
   * by PackageVariableHelper rather than PackageCollectionHelper.
   * 
   * @param dataType The data type to check
   * @param parentPackage The package context for custom type lookup
   * @return true if this is a regular variable, false if it's a collection type
   */
  private static boolean isRegularVariable(DataTypeSpec dataType, OraclePackage parentPackage) {
    // If it has a custom data type, check if it's a collection type
    if (dataType.getCustumDataType() != null) {
      String customType = dataType.getCustumDataType();
      
      // Check if it's a VARRAY type (handled by PackageCollectionHelper)
      for (VarrayType varrayType : parentPackage.getVarrayTypes()) {
        if (varrayType.getName().equalsIgnoreCase(customType)) {
          return false; // This is a collection, not a regular variable
        }
      }
      
      // Check if it's a TABLE OF type (handled by PackageCollectionHelper)
      for (NestedTableType tableType : parentPackage.getNestedTableTypes()) {
        if (tableType.getName().equalsIgnoreCase(customType)) {
          return false; // This is a collection, not a regular variable
        }
      }
      
      // If it's a custom type but not a collection, it might be a package type alias
      // Package type aliases are considered regular variables
      return true;
    }
    
    // If it's a native Oracle type (NUMBER, VARCHAR2, BOOLEAN, DATE, etc.), it's regular
    return true;
  }

  /**
   * Generates the table name for a package variable
   * following the naming convention: schema_package_variable
   */
  private static String generateTableName(String schema, String packageName, String variableName) {
    return schema.toLowerCase() + "_" + packageName.toLowerCase() + "_" + variableName.toLowerCase();
  }
}