package me.christianrobert.ora2postgre.plsql.ast.tools.transformers;

import me.christianrobert.ora2postgre.plsql.ast.OraclePackage;
import me.christianrobert.ora2postgre.plsql.ast.Variable;
import me.christianrobert.ora2postgre.plsql.ast.DataTypeSpec;
import me.christianrobert.ora2postgre.global.Everything;
import java.util.HashMap;
import java.util.Map;

/**
 * Transformer class for converting Oracle package variable references to PostgreSQL 
 * direct table access function calls. This eliminates the need for PRE/POST 
 * synchronization by using the Direct Table Access Pattern.
 * 
 * Transforms:
 * - Regular variables: gX -> sys.get_package_var_numeric('package', 'gX')
 * - Collection variables: arr(1) -> sys.get_package_collection_element_numeric('package', 'arr', 1)
 * - Collection methods: arr.COUNT -> sys.get_package_collection_count('package', 'arr')
 * - Assignment: gX := value -> sys.set_package_var_numeric('package', 'gX', value)
 */
public class PackageVariableReferenceTransformer {

  // Oracle data type to PostgreSQL accessor function mapping
  private static final Map<String, String> DATA_TYPE_TO_ACCESSOR = new HashMap<>();
  
  static {
    initializeDataTypeMapping();
  }

  /**
   * Initialize Oracle data type to PostgreSQL accessor function mapping.
   */
  private static void initializeDataTypeMapping() {
    // Numeric types
    DATA_TYPE_TO_ACCESSOR.put("NUMBER", "numeric");
    DATA_TYPE_TO_ACCESSOR.put("INTEGER", "numeric");
    DATA_TYPE_TO_ACCESSOR.put("INT", "numeric");
    DATA_TYPE_TO_ACCESSOR.put("NUMERIC", "numeric");
    DATA_TYPE_TO_ACCESSOR.put("DECIMAL", "numeric");
    DATA_TYPE_TO_ACCESSOR.put("FLOAT", "numeric");
    DATA_TYPE_TO_ACCESSOR.put("REAL", "numeric");
    DATA_TYPE_TO_ACCESSOR.put("DOUBLE", "numeric");
    
    // String types
    DATA_TYPE_TO_ACCESSOR.put("VARCHAR2", "text");
    DATA_TYPE_TO_ACCESSOR.put("VARCHAR", "text");
    DATA_TYPE_TO_ACCESSOR.put("CHAR", "text");
    DATA_TYPE_TO_ACCESSOR.put("NCHAR", "text");
    DATA_TYPE_TO_ACCESSOR.put("NVARCHAR2", "text");
    DATA_TYPE_TO_ACCESSOR.put("CLOB", "text");
    DATA_TYPE_TO_ACCESSOR.put("NCLOB", "text");
    
    // Boolean types
    DATA_TYPE_TO_ACCESSOR.put("BOOLEAN", "boolean");
    
    // Date/time types
    DATA_TYPE_TO_ACCESSOR.put("DATE", "timestamp");
    DATA_TYPE_TO_ACCESSOR.put("TIMESTAMP", "timestamp");
    DATA_TYPE_TO_ACCESSOR.put("TIMESTAMP WITH TIME ZONE", "timestamp");
    DATA_TYPE_TO_ACCESSOR.put("TIMESTAMP WITH LOCAL TIME ZONE", "timestamp");
    
    // Collection types - handled by collection-specific functions
    DATA_TYPE_TO_ACCESSOR.put("VARRAY", "collection");
    DATA_TYPE_TO_ACCESSOR.put("TABLE", "collection");
    DATA_TYPE_TO_ACCESSOR.put("NESTED TABLE", "collection");
  }

  /**
   * Transform Oracle package variable read access to PostgreSQL function call.
   * 
   * @param targetSchema Target schema where package variable tables are located
   * @param packageName Name of the Oracle package
   * @param varName Name of the package variable
   * @param dataType Oracle data type of the variable
   * @return PostgreSQL function call for reading the variable
   */
  public static String transformRead(String targetSchema, String packageName, String varName, String dataType) {
    String accessorType = mapDataTypeToAccessor(dataType);
    
    if ("collection".equals(accessorType)) {
      // Collection variables require special handling
      return String.format("sys.get_package_collection('%s', '%s', '%s')", 
          targetSchema.toLowerCase(), packageName.toLowerCase(), varName.toLowerCase());
    } else {
      // Regular package variables
      return String.format("sys.get_package_var_%s('%s', '%s', '%s')", 
          accessorType, targetSchema.toLowerCase(), packageName.toLowerCase(), varName.toLowerCase());
    }
  }

  /**
   * Transform Oracle package variable write access to PostgreSQL function call.
   * 
   * @param targetSchema Target schema where package variable tables are located
   * @param packageName Name of the Oracle package
   * @param varName Name of the package variable
   * @param dataType Oracle data type of the variable
   * @param value PostgreSQL expression for the value to write
   * @return PostgreSQL function call for writing the variable
   */
  public static String transformWrite(String targetSchema, String packageName, String varName, String dataType, String value) {
    String accessorType = mapDataTypeToAccessor(dataType);
    
    if ("collection".equals(accessorType)) {
      // Collection variables require special handling
      return String.format("PERFORM sys.set_package_collection('%s', '%s', '%s', %s)", 
          targetSchema.toLowerCase(), packageName.toLowerCase(), varName.toLowerCase(), value);
    } else {
      // Regular package variables
      return String.format("PERFORM sys.set_package_var_%s('%s', '%s', '%s', %s)", 
          accessorType, targetSchema.toLowerCase(), packageName.toLowerCase(), varName.toLowerCase(), value);
    }
  }

  /**
   * Transform Oracle collection element read access to PostgreSQL function call.
   * 
   * @param packageName Name of the Oracle package
   * @param collectionName Name of the collection variable
   * @param elementDataType Oracle data type of the collection elements
   * @param index Index expression (1-based)
   * @return PostgreSQL function call for reading the collection element
   */
  public static String transformCollectionElementRead(String targetSchema, String packageName, String collectionName, 
      String elementDataType, String index) {
    String accessorType = mapDataTypeToAccessor(elementDataType);
    
    if ("collection".equals(accessorType)) {
      // Nested collections - return as text and let caller handle casting
      return String.format("sys.get_package_collection_element('%s', '%s', '%s', %s)", 
          targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase(), index);
    } else {
      // Typed collection elements
      return String.format("sys.get_package_collection_element_%s('%s', '%s', '%s', %s)", 
          accessorType, targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase(), index);
    }
  }

  /**
   * Transform Oracle collection element write access to PostgreSQL function call.
   * 
   * @param packageName Name of the Oracle package
   * @param collectionName Name of the collection variable
   * @param elementDataType Oracle data type of the collection elements
   * @param index Index expression (1-based)
   * @param value PostgreSQL expression for the value to write
   * @return PostgreSQL function call for writing the collection element
   */
  public static String transformCollectionElementWrite(String targetSchema, String packageName, String collectionName, 
      String elementDataType, String index, String value) {
    String accessorType = mapDataTypeToAccessor(elementDataType);
    
    if ("collection".equals(accessorType)) {
      // Nested collections - accept as text
      return String.format("PERFORM sys.set_package_collection_element('%s', '%s', '%s', %s, %s)", 
          targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase(), index, value);
    } else {
      // Typed collection elements
      return String.format("PERFORM sys.set_package_collection_element_%s('%s', '%s', '%s', %s, %s)", 
          accessorType, targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase(), index, value);
    }
  }

  /**
   * Transform Oracle collection method calls to PostgreSQL function calls.
   * 
   * @param packageName Name of the Oracle package
   * @param collectionName Name of the collection variable
   * @param methodName Oracle collection method (COUNT, FIRST, LAST, etc.)
   * @return PostgreSQL function call for the collection method
   */
  public static String transformCollectionMethod(String targetSchema, String packageName, String collectionName, String methodName) {
    String method = methodName.toUpperCase();
    
    switch (method) {
      case "COUNT":
        return String.format("sys.get_package_collection_count('%s', '%s', '%s')", 
            targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase());
      case "FIRST":
        return String.format("sys.get_package_collection_first('%s', '%s', '%s')", 
            targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase());
      case "LAST":
        return String.format("sys.get_package_collection_last('%s', '%s', '%s')", 
            targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase());
      case "EXISTS":
        // EXISTS requires an index parameter, returns placeholder for parameter substitution
        return String.format("sys.package_collection_exists('%s', '%s', '%s', %%s)", 
            targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase());
      case "EXTEND":
        // EXTEND is a procedure, not a function - handle in statement transformation
        return String.format("sys.extend_package_collection('%s', '%s', '%s', NULL)", 
            targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase());
      default:
        // Unknown method - return as comment for manual handling
        return String.format("/* TODO: Transform collection method %s.%s for package %s */", 
            collectionName, methodName, packageName);
    }
  }

  /**
   * Transform Oracle collection EXTEND method call to PostgreSQL function call.
   * 
   * @param targetSchema Target schema name
   * @param packageName Name of the Oracle package
   * @param collectionName Name of the collection variable
   * @param value Optional value to add (can be null)
   * @return PostgreSQL function call for extending the collection
   */
  public static String transformCollectionExtend(String targetSchema, String packageName, String collectionName, String value) {
    if (value == null || value.trim().isEmpty()) {
      return String.format("PERFORM sys.extend_package_collection('%s', '%s', '%s', NULL)", 
          targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase());
    } else {
      return String.format("PERFORM sys.extend_package_collection('%s', '%s', '%s', %s)", 
          targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase(), value);
    }
  }

  /**
   * Transform Oracle collection DELETE method call to PostgreSQL function call.
   * 
   * @param targetSchema Target schema name
   * @param packageName Name of the Oracle package
   * @param collectionName Name of the collection variable
   * @param index Optional index to delete (null for delete all)
   * @return PostgreSQL function call for deleting collection elements
   */
  public static String transformCollectionDelete(String targetSchema, String packageName, String collectionName, String index) {
    if (index == null || index.trim().isEmpty()) {
      return String.format("PERFORM sys.delete_package_collection_all('%s', '%s', '%s')", 
          targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase());
    } else {
      return String.format("PERFORM sys.delete_package_collection_element('%s', '%s', '%s', %s)", 
          targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase(), index);
    }
  }

  /**
   * Transform Oracle collection TRIM method call to PostgreSQL function call.
   * 
   * @param targetSchema Target schema name
   * @param packageName Name of the Oracle package
   * @param collectionName Name of the collection variable
   * @param trimCount Optional number of elements to trim (default 1)
   * @return PostgreSQL function call for trimming collection
   */
  public static String transformCollectionTrim(String targetSchema, String packageName, String collectionName, String trimCount) {
    if (trimCount == null || trimCount.trim().isEmpty()) {
      return String.format("PERFORM sys.trim_package_collection('%s', '%s', '%s', 1)", 
          targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase());
    } else {
      return String.format("PERFORM sys.trim_package_collection('%s', '%s', '%s', %s)", 
          targetSchema.toLowerCase(), packageName.toLowerCase(), collectionName.toLowerCase(), trimCount);
    }
  }

  /**
   * Map Oracle data type to PostgreSQL accessor function suffix.
   * 
   * @param oracleType Oracle data type string
   * @return PostgreSQL accessor function suffix
   */
  public static String mapDataTypeToAccessor(String oracleType) {
    if (oracleType == null) {
      return "text"; // fallback
    }
    
    String upperType = oracleType.toUpperCase();
    
    // Handle collection types (Oracle and PostgreSQL syntax)
    if (upperType.contains("VARRAY") || upperType.contains("TABLE OF") || 
        upperType.contains("NESTED TABLE") || upperType.endsWith("[]")) {
      return "collection";
    }
    
    // Handle parameterized types (e.g., VARCHAR2(100), NUMBER(10,2))
    if (upperType.contains("(")) {
      upperType = upperType.substring(0, upperType.indexOf("("));
    }
    
    return DATA_TYPE_TO_ACCESSOR.getOrDefault(upperType, "text");
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
      if (oracleType != null && isOracleCollectionType(oracleType)) {
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
    
    // Use the toString() method which should preserve Oracle syntax
    String typeString = dataTypeSpec.toString();
    
    // Also check the native and custom data type fields
    if (typeString == null || typeString.trim().isEmpty()) {
      if (dataTypeSpec.getNativeDataType() != null) {
        typeString = dataTypeSpec.getNativeDataType();
      } else if (dataTypeSpec.getCustumDataType() != null) {
        typeString = dataTypeSpec.getCustumDataType();
      }
    }
    
    return typeString;
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