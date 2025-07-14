package me.christianrobert.ora2postgre.plsql.ast.tools.helpers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;

import java.util.*;

/**
 * Helper class for managing package collection variable materialization.
 * This handles the conversion of package-level collection variables to 
 * function-local arrays with proper prologue/epilogue generation.
 */
public class PackageCollectionHelper {

  /**
   * Represents a package collection variable that needs materialization.
   */
  public static class PackageCollection {
    private final String variableName;
    private final String tableName;
    private final DataTypeSpec dataType;
    private final String baseArrayType;
    private final boolean isVarray;
    
    public PackageCollection(String variableName, String tableName, DataTypeSpec dataType, 
                           String baseArrayType, boolean isVarray) {
      this.variableName = variableName;
      this.tableName = tableName;
      this.dataType = dataType;
      this.baseArrayType = baseArrayType;
      this.isVarray = isVarray;
    }
    
    public String getVariableName() { return variableName; }
    public String getTableName() { return tableName; }
    public DataTypeSpec getDataType() { return dataType; }
    public String getBaseArrayType() { return baseArrayType; }
    public boolean isVarray() { return isVarray; }
  }

  /**
   * Analyzes a function to determine which package collection variables
   * are used but not shadowed by local declarations.
   * 
   * @param function The function to analyze
   * @param parentPackage The parent package containing collection variables
   * @param context The Everything context for type resolution
   * @return List of package collections that need materialization
   */
  public static List<PackageCollection> analyzePackageCollections(
      Function function, OraclePackage parentPackage, Everything context) {
    
    if (parentPackage == null) {
      return Collections.emptyList();
    }
    
    List<PackageCollection> result = new ArrayList<>();
    
    // Get all local variable names to detect shadowing
    Set<String> localVariableNames = new HashSet<>();
    if (function.getVariables() != null) {
      for (Variable localVar : function.getVariables()) {
        localVariableNames.add(localVar.getName().toLowerCase());
      }
    }
    
    // Check package variables for collection types
    for (Variable packageVar : parentPackage.getVariables()) {
      String varName = packageVar.getName().toLowerCase();
      
      // Skip if this variable is shadowed by a local declaration
      if (localVariableNames.contains(varName)) {
        continue;
      }
      
      // Check if this is a collection type variable
      DataTypeSpec dataType = packageVar.getDataType();
      if (dataType.getCustumDataType() != null) {
        String customType = dataType.getCustumDataType();
        
        // Check if it's a VARRAY type
        for (VarrayType varrayType : parentPackage.getVarrayTypes()) {
          if (varrayType.getName().equalsIgnoreCase(customType)) {
            String tableName = generateTableName(parentPackage.getSchema(), 
                                               parentPackage.getName(), varName);
            String baseArrayType = varrayType.getDataType().toPostgre(context) + "[]";
            
            result.add(new PackageCollection(varName, tableName, dataType, baseArrayType, true));
            break;
          }
        }
        
        // Check if it's a TABLE OF type
        for (NestedTableType tableType : parentPackage.getNestedTableTypes()) {
          if (tableType.getName().equalsIgnoreCase(customType)) {
            String tableName = generateTableName(parentPackage.getSchema(), 
                                               parentPackage.getName(), varName);
            String baseArrayType = tableType.getDataType().toPostgre(context) + "[]";
            
            result.add(new PackageCollection(varName, tableName, dataType, baseArrayType, false));
            break;
          }
        }
      }
    }
    
    return result;
  }
  
  /**
   * Analyzes a procedure to determine which package collection variables
   * are used but not shadowed by local declarations.
   */
  public static List<PackageCollection> analyzePackageCollections(
      Procedure procedure, OraclePackage parentPackage, Everything context) {
    
    if (parentPackage == null) {
      return Collections.emptyList();
    }
    
    List<PackageCollection> result = new ArrayList<>();
    
    // Get all local variable names to detect shadowing
    Set<String> localVariableNames = new HashSet<>();
    if (procedure.getVariables() != null) {
      for (Variable localVar : procedure.getVariables()) {
        localVariableNames.add(localVar.getName().toLowerCase());
      }
    }
    
    // Check package variables for collection types
    for (Variable packageVar : parentPackage.getVariables()) {
      String varName = packageVar.getName().toLowerCase();
      
      // Skip if this variable is shadowed by a local declaration
      if (localVariableNames.contains(varName)) {
        continue;
      }
      
      // Check if this is a collection type variable
      DataTypeSpec dataType = packageVar.getDataType();
      if (dataType.getCustumDataType() != null) {
        String customType = dataType.getCustumDataType();
        
        // Check if it's a VARRAY type
        for (VarrayType varrayType : parentPackage.getVarrayTypes()) {
          if (varrayType.getName().equalsIgnoreCase(customType)) {
            String tableName = generateTableName(parentPackage.getSchema(), 
                                               parentPackage.getName(), varName);
            String baseArrayType = varrayType.getDataType().toPostgre(context) + "[]";
            
            result.add(new PackageCollection(varName, tableName, dataType, baseArrayType, true));
            break;
          }
        }
        
        // Check if it's a TABLE OF type
        for (NestedTableType tableType : parentPackage.getNestedTableTypes()) {
          if (tableType.getName().equalsIgnoreCase(customType)) {
            String tableName = generateTableName(parentPackage.getSchema(), 
                                               parentPackage.getName(), varName);
            String baseArrayType = tableType.getDataType().toPostgre(context) + "[]";
            
            result.add(new PackageCollection(varName, tableName, dataType, baseArrayType, false));
            break;
          }
        }
      }
    }
    
    return result;
  }

  /**
   * Generates the function prologue that materializes package collection variables
   * into local array variables.
   * 
   * @param packageCollections List of package collections to materialize
   * @return Generated PostgreSQL prologue code
   */
  public static String generatePrologue(List<PackageCollection> packageCollections) {
    if (packageCollections.isEmpty()) {
      return "";
    }
    
    StringBuilder b = new StringBuilder();
    b.append("  -- Auto-generated prologue: materialize package collection variables\n");
    
    for (PackageCollection collection : packageCollections) {
      b.append("  -- Materialize package collection: ").append(collection.getVariableName()).append("\n");
      b.append("  SELECT CASE WHEN COUNT(*) = 0 THEN ARRAY[]::").append(collection.getBaseArrayType()).append("\n");
      b.append("              ELSE array_agg(value ORDER BY row_number() OVER ())\n");
      b.append("         END INTO ").append(collection.getVariableName()).append("\n");
      b.append("  FROM ").append(collection.getTableName()).append(";\n");
      b.append("\n");
    }
    
    return b.toString();
  }

  /**
   * Generates the function epilogue that persists local array variables
   * back to package collection temporary tables.
   * 
   * @param packageCollections List of package collections to persist
   * @return Generated PostgreSQL epilogue code
   */
  public static String generateEpilogue(List<PackageCollection> packageCollections) {
    if (packageCollections.isEmpty()) {
      return "";
    }
    
    StringBuilder b = new StringBuilder();
    b.append("  -- Auto-generated epilogue: persist package collection variables\n");
    
    for (PackageCollection collection : packageCollections) {
      b.append("  -- Persist package collection: ").append(collection.getVariableName()).append("\n");
      b.append("  DELETE FROM ").append(collection.getTableName()).append(";\n");
      b.append("  INSERT INTO ").append(collection.getTableName()).append(" (value)\n");
      b.append("  SELECT unnest(").append(collection.getVariableName()).append(");\n");
      b.append("\n");
    }
    
    return b.toString();
  }

  /**
   * Generates variable declarations for package collection variables
   * that need to be materialized as local arrays.
   * 
   * @param packageCollections List of package collections to declare
   * @return Generated PostgreSQL variable declarations
   */
  public static String generateVariableDeclarations(List<PackageCollection> packageCollections) {
    if (packageCollections.isEmpty()) {
      return "";
    }
    
    StringBuilder b = new StringBuilder();
    b.append("  -- Auto-generated declarations for package collection variables\n");
    
    for (PackageCollection collection : packageCollections) {
      b.append("  ").append(collection.getVariableName()).append(" ").append(collection.getBaseArrayType()).append(";\n");
    }
    b.append("\n");
    
    return b.toString();
  }

  /**
   * Generates the table name for a package collection variable
   * following the naming convention: schema_package_variable
   */
  private static String generateTableName(String schema, String packageName, String variableName) {
    return schema.toLowerCase() + "_" + packageName.toLowerCase() + "_" + variableName.toLowerCase();
  }
}