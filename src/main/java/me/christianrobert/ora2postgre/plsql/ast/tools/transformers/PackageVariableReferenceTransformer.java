package me.christianrobert.ora2postgre.plsql.ast.tools.transformers;

import me.christianrobert.ora2postgre.plsql.ast.OraclePackage;
import me.christianrobert.ora2postgre.plsql.ast.Variable;
import me.christianrobert.ora2postgre.plsql.ast.DataTypeSpec;
import me.christianrobert.ora2postgre.plsql.ast.VarrayType;
import me.christianrobert.ora2postgre.plsql.ast.NestedTableType;
import me.christianrobert.ora2postgre.global.Everything;
import java.util.HashMap;
import java.util.Map;

/**
 * Transformer class for converting Oracle package variable references to PostgreSQL 
 * unified JSON-based storage function calls. This eliminates type-specific functions
 * and provides consistent storage for all variable types including collections.
 * 
 * Transforms:
 * - Regular variables: gX -> sys.get_package_var('schema', 'package', 'gX')
 * - Collection variables: arr(1) -> sys.get_package_var_element('schema', 'package', 'arr', 1)
 * - Collection methods: arr.COUNT -> sys.get_package_var_count('schema', 'package', 'arr')
 * - Assignment: gX := value -> sys.set_package_var('schema', 'package', 'gX', value::jsonb)
 */
public class PackageVariableReferenceTransformer {

  // Legacy data type mapping - kept for backward compatibility but no longer used in JSON approach
  @Deprecated
  private static final Map<String, String> DATA_TYPE_TO_ACCESSOR = new HashMap<>();
  
  static {
    // Keep for backward compatibility - but all operations now use unified JSON storage
    DATA_TYPE_TO_ACCESSOR.put("NUMBER", "numeric");
    DATA_TYPE_TO_ACCESSOR.put("VARCHAR2", "text");
    DATA_TYPE_TO_ACCESSOR.put("BOOLEAN", "boolean");
    DATA_TYPE_TO_ACCESSOR.put("DATE", "timestamp");
    DATA_TYPE_TO_ACCESSOR.put("VARRAY", "collection");
    DATA_TYPE_TO_ACCESSOR.put("TABLE", "collection");
  }

  /**
   * Transform Oracle package variable read access to unified JSON-based PostgreSQL function call.
   * 
   * @param targetSchema Target schema where package variable tables are located
   * @param packageName Name of the Oracle package
   * @param varName Name of the package variable
   * @param dataType Oracle data type of the variable (for documentation only)
   * @return PostgreSQL function call for reading the variable
   */
  public static String transformRead(String targetSchema, String packageName, String varName, String dataType) {
    return transformRead(targetSchema, packageName, varName, dataType, null);
  }

  /**
   * Transform Oracle package variable read access to unified JSON-based PostgreSQL function call.
   * All package variables are now stored as JSONB for consistency and extensibility.
   * 
   * @param targetSchema Target schema where package variable tables are located
   * @param packageName Name of the Oracle package
   * @param varName Name of the package variable
   * @param dataType Oracle data type of the variable (for documentation only)
   * @param pkg Package context for custom type lookup (can be null)
   * @return PostgreSQL function call for reading the variable
   */
  public static String transformRead(String targetSchema, String packageName, String varName, String dataType, OraclePackage pkg) {
    // Unified JSON-based storage for all variable types
    return String.format("sys.get_package_var('%s', '%s', '%s')", 
        targetSchema.toLowerCase(), packageName.toLowerCase(), varName.toLowerCase());
  }

  /**
   * Transform Oracle package variable write access to unified JSON-based PostgreSQL function call.
   * 
   * @param targetSchema Target schema where package variable tables are located
   * @param packageName Name of the Oracle package
   * @param varName Name of the package variable
   * @param dataType Oracle data type of the variable (for casting hints)
   * @param value PostgreSQL expression for the value to write
   * @return PostgreSQL function call for writing the variable
   */
  public static String transformWrite(String targetSchema, String packageName, String varName, String dataType, String value) {
    return transformWrite(targetSchema, packageName, varName, dataType, value, null);
  }

  /**
   * Transform Oracle package variable write access to unified JSON-based PostgreSQL function call.
   * All values are converted to JSONB for consistent storage.
   * 
   * @param targetSchema Target schema where package variable tables are located
   * @param packageName Name of the Oracle package
   * @param varName Name of the package variable
   * @param dataType Oracle data type of the variable (for casting hints)
   * @param value PostgreSQL expression for the value to write
   * @param pkg Package context for custom type lookup (can be null)
   * @return PostgreSQL function call for writing the variable
   */
  public static String transformWrite(String targetSchema, String packageName, String varName, String dataType, String value, OraclePackage pkg) {
    // Convert value to JSONB based on data type context
    String jsonValue = convertValueToJsonb(value, dataType, pkg);
    
    // Unified JSON-based storage for all variable types
    return String.format("PERFORM sys.set_package_var('%s', '%s', '%s', %s)", 
        targetSchema.toLowerCase(), packageName.toLowerCase(), varName.toLowerCase(), jsonValue);
  }

  /**
   * Transform Oracle collection element read access to unified JSON-based PostgreSQL function call.
   * 
   * @param targetSchema Target schema name
   * @param packageName Name of the Oracle package
   * @param collectionName Name of the collection variable
   * @param elementDataType Oracle data type of the collection elements (for documentation)
   * @param index Index expression (1-based)
   * @return PostgreSQL function call for reading the collection element
   */
  public static String transformCollectionElementRead(String targetSchema, String packageName, String collectionName, 
      String elementDataType, String index) {
    // Unified JSON-based element access for all collection types
    return String.format("sys.get_package_var_element('%s', '%s', '%s', %s)", 
        targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase(), index);
  }

  /**
   * Transform Oracle collection element write access to unified JSON-based PostgreSQL function call.
   * 
   * @param targetSchema Target schema name
   * @param packageName Name of the Oracle package
   * @param collectionName Name of the collection variable
   * @param elementDataType Oracle data type of the collection elements (for casting hints)
   * @param index Index expression (1-based)
   * @param value PostgreSQL expression for the value to write
   * @return PostgreSQL function call for writing the collection element
   */
  public static String transformCollectionElementWrite(String targetSchema, String packageName, String collectionName, 
      String elementDataType, String index, String value) {
    // Convert element value to JSONB
    String jsonValue = convertValueToJsonb(value, elementDataType, null);
    
    // Unified JSON-based element assignment for all collection types
    return String.format("PERFORM sys.set_package_var_element('%s', '%s', '%s', %s, %s)", 
        targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase(), index, jsonValue);
  }

  /**
   * Transform Oracle collection method calls to unified JSON-based PostgreSQL function calls.
   * 
   * @param targetSchema Target schema name
   * @param packageName Name of the Oracle package
   * @param collectionName Name of the collection variable
   * @param methodName Oracle collection method (COUNT, FIRST, LAST, etc.)
   * @return PostgreSQL function call for the collection method
   */
  public static String transformCollectionMethod(String targetSchema, String packageName, String collectionName, String methodName) {
    String method = methodName.toUpperCase();
    
    switch (method) {
      case "COUNT":
        return String.format("sys.get_package_var_count('%s', '%s', '%s')", 
            targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase());
      case "FIRST":
        return String.format("sys.get_package_var_first('%s', '%s', '%s')", 
            targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase());
      case "LAST":
        return String.format("sys.get_package_var_last('%s', '%s', '%s')", 
            targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase());
      case "EXISTS":
        // EXISTS requires an index parameter, returns placeholder for parameter substitution
        return String.format("sys.get_package_var_exists('%s', '%s', '%s', %%s)", 
            targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase());
      case "EXTEND":
        // EXTEND is a procedure, not a function - handle in statement transformation
        return String.format("sys.extend_package_var('%s', '%s', '%s', NULL)", 
            targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase());
      default:
        // Unknown method - return as comment for manual handling
        return String.format("/* TODO: Transform collection method %s.%s for package %s */", 
            collectionName, methodName, packageName);
    }
  }

  /**
   * Transform Oracle collection EXTEND method call to unified JSON-based PostgreSQL function call.
   * 
   * @param targetSchema Target schema name
   * @param packageName Name of the Oracle package
   * @param collectionName Name of the collection variable
   * @param value Optional value to add (can be null)
   * @return PostgreSQL function call for extending the collection
   */
  public static String transformCollectionExtend(String targetSchema, String packageName, String collectionName, String value) {
    if (value == null || value.trim().isEmpty()) {
      return String.format("PERFORM sys.extend_package_var('%s', '%s', '%s', NULL)", 
          targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase());
    } else {
      String jsonValue = convertValueToJsonb(value, null, null);
      return String.format("PERFORM sys.extend_package_var('%s', '%s', '%s', %s)", 
          targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase(), jsonValue);
    }
  }

  /**
   * Transform Oracle collection DELETE method call to unified JSON-based PostgreSQL function call.
   * 
   * @param targetSchema Target schema name
   * @param packageName Name of the Oracle package
   * @param collectionName Name of the collection variable
   * @param index Optional index to delete (null for delete all)
   * @return PostgreSQL function call for deleting collection elements
   */
  public static String transformCollectionDelete(String targetSchema, String packageName, String collectionName, String index) {
    if (index == null || index.trim().isEmpty()) {
      return String.format("PERFORM sys.delete_package_var_all('%s', '%s', '%s')", 
          targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase());
    } else {
      return String.format("PERFORM sys.delete_package_var_element('%s', '%s', '%s', %s)", 
          targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase(), index);
    }
  }

  /**
   * Transform Oracle collection TRIM method call to unified JSON-based PostgreSQL function call.
   * 
   * @param targetSchema Target schema name
   * @param packageName Name of the Oracle package
   * @param collectionName Name of the collection variable
   * @param trimCount Optional number of elements to trim (default 1)
   * @return PostgreSQL function call for trimming collection
   */
  public static String transformCollectionTrim(String targetSchema, String packageName, String collectionName, String trimCount) {
    if (trimCount == null || trimCount.trim().isEmpty()) {
      return String.format("PERFORM sys.trim_package_var('%s', '%s', '%s', 1)", 
          targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase());
    } else {
      return String.format("PERFORM sys.trim_package_var('%s', '%s', '%s', %s)", 
          targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase(), trimCount);
    }
  }

  /**
   * Convert Oracle value expression to PostgreSQL JSONB format.
   * This method handles type-specific conversion for JSON storage.
   * 
   * @param value PostgreSQL expression for the value
   * @param dataType Oracle data type (for conversion hints)
   * @param pkg Package context (can be null)
   * @return JSONB-compatible expression
   */
  public static String convertValueToJsonb(String value, String dataType, OraclePackage pkg) {
    if (value == null || value.trim().isEmpty()) {
      return "NULL";
    }
    
    String trimmedValue = value.trim();
    
    // Handle Oracle collection constructors
    if (isCollectionConstructor(trimmedValue, dataType, pkg)) {
      return convertCollectionConstructorToJsonb(trimmedValue);
    }
    
    // Handle PostgreSQL arrays (from previous transformations)
    if (trimmedValue.startsWith("ARRAY[")) {
      // Find the closing bracket for the array content (not type casting)
      int arrayStart = 6; // After "ARRAY["
      int arrayEnd = -1;
      int bracketCount = 1; // Already opened one bracket
      
      for (int i = arrayStart; i < trimmedValue.length() && bracketCount > 0; i++) {
        char c = trimmedValue.charAt(i);
        if (c == '[') bracketCount++;
        else if (c == ']') {
          bracketCount--;
          if (bracketCount == 0) {
            arrayEnd = i;
            break;
          }
        }
      }
      
      if (arrayEnd > arrayStart) {
        // Extract just the array content, ignore any type casting
        String arrayContent = trimmedValue.substring(arrayStart, arrayEnd);
        return String.format("'[%s]'::jsonb", arrayContent);
      } else {
        // Fallback for malformed arrays
        return "'[]'::jsonb";
      }
    }
    
    // Handle NULL literals
    if ("NULL".equalsIgnoreCase(trimmedValue)) {
      return "NULL";
    }
    
    // Handle string literals - keep as JSON strings
    if (trimmedValue.startsWith("'") && trimmedValue.endsWith("'")) {
      return String.format("%s::jsonb", trimmedValue);
    }
    
    // Handle numeric literals
    if (trimmedValue.matches("\\d+(\\.\\d+)?")) {
      return String.format("%s::jsonb", trimmedValue);
    }
    
    // Handle boolean literals
    if ("TRUE".equalsIgnoreCase(trimmedValue) || "FALSE".equalsIgnoreCase(trimmedValue)) {
      return String.format("%s::jsonb", trimmedValue.toLowerCase());
    }
    
    // For complex expressions, wrap in to_jsonb()
    return String.format("to_jsonb(%s)", trimmedValue);
  }
  
  /**
   * Check if a value expression is an Oracle collection constructor.
   */
  private static boolean isCollectionConstructor(String value, String dataType, OraclePackage pkg) {
    if (value == null || dataType == null) {
      return false;
    }
    
    // Check for custom type constructors (e.g., t_numbers())
    if (pkg != null && value.matches("\\w+\\(.*\\)")) {
      String typeName = value.substring(0, value.indexOf("("));
      return isDefinedCollectionType(typeName, pkg);
    }
    
    return false;
  }
  
  /**
   * Convert Oracle collection constructor to JSONB array.
   * Examples: t_numbers() -> '[]'::jsonb, t_numbers(1,2,3) -> '[1,2,3]'::jsonb
   */
  private static String convertCollectionConstructorToJsonb(String constructor) {
    if (constructor == null) {
      return "'[]'::jsonb";
    }
    
    int parenStart = constructor.indexOf("(");
    int parenEnd = constructor.lastIndexOf(")");
    
    if (parenStart == -1 || parenEnd == -1 || parenEnd <= parenStart) {
      return "'[]'::jsonb"; // Empty constructor
    }
    
    String arguments = constructor.substring(parenStart + 1, parenEnd).trim();
    
    if (arguments.isEmpty()) {
      return "'[]'::jsonb"; // Empty constructor
    }
    
    // Convert arguments to JSON array format
    return String.format("'[%s]'::jsonb", arguments);
  }

  /**
   * Legacy method for backward compatibility.
   * @deprecated Use unified JSON-based approach instead
   */
  @Deprecated
  public static String mapDataTypeToAccessor(String oracleType) {
    return "jsonb"; // All types now use unified JSON storage
  }

  /**
   * Legacy method for backward compatibility.
   * @deprecated Use unified JSON-based approach instead
   */
  @Deprecated
  public static String mapDataTypeToAccessorWithContext(String oracleType, OraclePackage pkg) {
    return "jsonb"; // All types now use unified JSON storage
  }

  /**
   * Check if a variable is a package variable based on context.
   * 
   * @param varName Variable name to check
   * @param data Everything context for package lookup
   * @return true if this is a package variable reference
   */
  public static boolean isPackageVariableReference(String varName, Everything data) {
    if (varName == null || data == null) {
      return false;
    }
    
    // Check if variable name exists in any package in current schema
    // Check both spec and body packages
    for (OraclePackage pkg : data.getPackageSpecAst()) {
      if (hasVariable(pkg, varName)) {
        return true;
      }
    }
    for (OraclePackage pkg : data.getPackageBodyAst()) {
      if (hasVariable(pkg, varName)) {
        return true;
      }
    }
    
    return false;
  }

  /**
   * Find the package that contains a specific variable.
   * 
   * @param varName Variable name to find
   * @param data Everything context for package lookup
   * @return OraclePackage containing the variable, or null if not found
   */
  public static OraclePackage findContainingPackage(String varName, Everything data) {
    if (varName == null || data == null) {
      return null;
    }
    
    // Find which package contains this variable
    // Check both spec and body packages
    for (OraclePackage pkg : data.getPackageSpecAst()) {
      if (hasVariable(pkg, varName)) {
        return pkg;
      }
    }
    for (OraclePackage pkg : data.getPackageBodyAst()) {
      if (hasVariable(pkg, varName)) {
        return pkg;
      }
    }
    
    return null;
  }

  /**
   * Get the data type of a package variable.
   * 
   * @param varName Variable name
   * @param pkg Package containing the variable
   * @return Data type string, or "text" if not found
   */
  public static String getPackageVariableDataType(String varName, OraclePackage pkg) {
    if (varName == null || pkg == null) {
      return "text";
    }
    
    Variable var = findVariable(pkg, varName);
    if (var != null) {
      // For collection types, preserve Oracle type information for mapping
      String oracleType = getOracleDataTypeString(var.getDataType());
      
      // Check if this is a collection type by looking up the type definition in the package
      if (oracleType != null && (isOracleCollectionType(oracleType) || isDefinedCollectionType(oracleType, pkg))) {
        return oracleType; // Return Oracle type for proper collection detection
      }
      
      // For non-collection types, convert to PostgreSQL syntax
      return var.getDataType().toPostgre(new Everything(), pkg.getSchema(), pkg.getName());
    }
    
    return "text";
  }

  /**
   * Get the original Oracle data type string from DataTypeSpec.
   * This preserves Oracle keywords like VARRAY and TABLE for proper type mapping.
   * 
   * @param dataTypeSpec DataTypeSpec object
   * @return Oracle data type string
   */
  private static String getOracleDataTypeString(me.christianrobert.ora2postgre.plsql.ast.DataTypeSpec dataTypeSpec) {
    if (dataTypeSpec == null) {
      return null;
    }
    
    // First try to get custom data type (user-defined types like t_numbers)
    if (dataTypeSpec.getCustumDataType() != null && !dataTypeSpec.getCustumDataType().trim().isEmpty()) {
      return dataTypeSpec.getCustumDataType().trim();
    }
    
    // Then try native data type (built-in Oracle types like VARCHAR2, NUMBER)
    if (dataTypeSpec.getNativeDataType() != null && !dataTypeSpec.getNativeDataType().trim().isEmpty()) {
      return dataTypeSpec.getNativeDataType().trim();
    }
    
    // Last resort: toString() - but this might not be reliable
    String typeString = dataTypeSpec.toString();
    if (typeString != null && !typeString.trim().isEmpty() && !typeString.contains("@")) {
      return typeString.trim();
    }
    
    return null;
  }

  /**
   * Check if an Oracle data type string represents a collection type.
   * 
   * @param oracleType Oracle data type string
   * @return true if this is a collection type
   */
  private static boolean isOracleCollectionType(String oracleType) {
    if (oracleType == null) {
      return false;
    }
    
    String upperType = oracleType.toUpperCase();
    return upperType.contains("VARRAY") || upperType.contains("TABLE OF") || 
           upperType.contains("NESTED TABLE");
  }
  
  /**
   * Check if a type name is defined as a collection type in the given package.
   * 
   * @param typeName Type name to check
   * @param pkg Package to search
   * @return true if the type is defined as a VARRAY or nested table type
   */
  private static boolean isDefinedCollectionType(String typeName, OraclePackage pkg) {
    if (typeName == null || pkg == null) {
      return false;
    }
    
    // Check VARRAY types
    for (VarrayType varray : pkg.getVarrayTypes()) {
      if (typeName.equalsIgnoreCase(varray.getName())) {
        return true;
      }
    }
    
    // Check nested table types
    for (NestedTableType nestedTable : pkg.getNestedTableTypes()) {
      if (typeName.equalsIgnoreCase(nestedTable.getName())) {
        return true;
      }
    }
    
    return false;
  }

  /**
   * Check if a data type represents a collection type.
   * 
   * @param dataType Oracle data type string
   * @return true if this is a collection type
   */
  public static boolean isCollectionType(String dataType) {
    if (dataType == null) {
      return false;
    }
    
    String upperType = dataType.toUpperCase();
    return upperType.contains("VARRAY") || upperType.contains("TABLE") || 
           upperType.contains("NESTED TABLE") || "collection".equals(mapDataTypeToAccessor(dataType));
  }

  /**
   * Get supported Oracle data types for validation purposes.
   * 
   * @return Set of supported Oracle data type names
   */
  public static java.util.Set<String> getSupportedDataTypes() {
    return DATA_TYPE_TO_ACCESSOR.keySet();
  }

  /**
   * Check if a package has a variable with the given name.
   * 
   * @param pkg Package to search
   * @param varName Variable name to find
   * @return true if the package contains the variable
   */
  private static boolean hasVariable(OraclePackage pkg, String varName) {
    if (pkg == null || varName == null) {
      return false;
    }
    
    for (Variable var : pkg.getVariables()) {
      if (varName.equals(var.getName())) {
        return true;
      }
    }
    
    return false;
  }

  /**
   * Find a variable in a package by name.
   * 
   * @param pkg Package to search
   * @param varName Variable name to find
   * @return Variable if found, null otherwise
   */
  private static Variable findVariable(OraclePackage pkg, String varName) {
    if (pkg == null || varName == null) {
      return null;
    }
    
    for (Variable var : pkg.getVariables()) {
      if (varName.equals(var.getName())) {
        return var;
      }
    }
    
    return null;
  }
}