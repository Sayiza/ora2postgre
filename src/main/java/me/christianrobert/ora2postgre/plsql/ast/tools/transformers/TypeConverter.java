package me.christianrobert.ora2postgre.plsql.ast.tools.transformers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.Function;
import me.christianrobert.ora2postgre.plsql.ast.VarrayType;
import me.christianrobert.ora2postgre.plsql.ast.NestedTableType;

public class TypeConverter {

  public static String toJava(String returnType) {
    if (returnType.toLowerCase().startsWith("varchar2")) { return "String"; }
    if (returnType.toLowerCase().equals("clob")) { return "String"; }
    if (returnType.toLowerCase().equals("number")) { return "BigDecimal"; }
    return "Object"; //TODO
  }

  public static String toJavaMediaType(String returnType) {
    if (returnType.startsWith("varchar2")) { return "MediaType.TEXT_PLAIN"; }
    if (returnType.equals("clob")) { return "MediaType.TEXT_PLAIN"; }
    if (returnType.equals("number")) { return "MediaType.TEXT_PLAIN"; }
    return "MediaType.APPLICATION_JSON";
  }

  public static String toPostgre(String returnType) {
    if (returnType == null) {
      return null;
    }

    returnType = returnType.toLowerCase().trim();

    // Handle parameterized types first
    if (returnType.startsWith("number")) {
      return "numeric"; 
    }
    if (returnType.startsWith("varchar2")) {
      return "text";
    }
    if (returnType.startsWith("nvarchar2")) {
      return "text";
    }
    if (returnType.startsWith("char")) {
      return "text";
    }
    if (returnType.startsWith("nchar")) {
      return "text";
    }
    if (returnType.startsWith("raw")) {
      return "bytea";
    }
    if (returnType.startsWith("timestamp")) {
      return "timestamp";
    }
    if (returnType.startsWith("interval")) {
      return "interval";
    }

    // Handle exact type matches
    switch (returnType) {
      // Integer types
      case "binary_integer":
      case "pls_integer":
      case "natural":
      case "naturaln":
      case "positive":
      case "positiven":
      case "simple_integer":
      case "integer":
      case "int":
        return "integer";

      // Small integer
      case "signtype":
      case "smallint":
        return "smallint";

      // Floating point types
      case "binary_float":
      case "float":
      case "real":
        return "real";

      case "binary_double":
      case "double":
        return "double precision";

      // Decimal types
      case "dec":
      case "decimal":
        return "decimal";

      case "numeric":
        return "numeric";

      // Character types
      case "long":
      case "string":
      case "varchar":
        return "varchar";

      case "character":
        return "char";

      // Boolean
      case "boolean":
        return "boolean";

      // Date and time types
      case "date":
        return "timestamp";

      case "timestamp_unconstrained":
        return "timestamp";

      case "timestamp_tz_unconstrained":
      case "timestamp_ltz_unconstrained":
        return "timestamp with time zone";

      case "yminterval_unconstrained":
      case "dsinterval_unconstrained":
        return "interval";

      // Time component types
      case "year":
      case "month":
      case "day":
      case "hour":
      case "minute":
      case "second":
      case "timezone_hour":
      case "timezone_minute":
        return "integer";

      // Text types for timezone and identifiers
      case "timezone_region":
      case "timezone_abbr":
      case "rowid":
      case "urowid":
      case "mlslabel":
        return "text";

      // Large object types
      case "bfile":
        return "text";

      case "blob":
        return "bytea";

      case "clob":
      case "nclob":
        return "text";

      // XML type
      case "xmltype":
        return "xml";

      // Additional types not in original list
      case "long raw":
        return "bytea";

      case "interval year to month":
      case "interval day to second":
        return "interval";

      case "httpuritype":
      case "dburitype":
      case "xdburitype":
        return "text";
        
      case "anydata":
        return "jsonb";

      case "aq$_jms_text_message":
      case "sys.aq$_jms_text_message":
        return "jsonb";

      case "aq$_sig_prop":
      case "sys.aq$_sig_prop":
        return "jsonb";

      case "aq$_recipients":
      case "sys.aq$_recipients":
        return "jsonb";

      case "sdo_geometry":
        return "geometry";

      case "sys_refcursor":
        return "refcursor";

      case "json":
        return "jsonb";

      case "varray":
      case "nested table":
        return "array";

      // Default case - normalize to lowercase for custom object types
      default:
        return returnType;
    }
  }

  /**
   * Enhanced return type conversion with function context for collection type resolution.
   * This enables function return types to use function-local collection types.
   */
  public static String toPostgre(String returnType, Everything data, Function function) {
    if (returnType == null) {
      return null;
    }

    // First check if this is a function-local collection type
    if (function != null) {
      // Check function-local VARRAY types
      for (VarrayType varrayType : function.getVarrayTypes()) {
        if (varrayType.getName().equalsIgnoreCase(returnType)) {
          // Get base type and add [] - TYPE local_array IS VARRAY(10) OF VARCHAR2(100) → text[]
          String baseType = varrayType.getDataType().toPostgre(data);
          return baseType + "[]";
        }
      }
      
      // Check function-local TABLE OF types
      for (NestedTableType nestedTableType : function.getNestedTableTypes()) {
        if (nestedTableType.getName().equalsIgnoreCase(returnType)) {
          // Get base type and add [] - TYPE local_table IS TABLE OF NUMBER → numeric[]
          String baseType = nestedTableType.getDataType().toPostgre(data);
          return baseType + "[]";
        }
      }
      
      // Check if it's a package-level collection type
      if (function.getParentPackage() != null) {
        // Check package VARRAY types
        for (VarrayType varrayType : function.getParentPackage().getVarrayTypes()) {
          if (varrayType.getName().equalsIgnoreCase(returnType)) {
            // Package-level types use DOMAIN references
            String schema = function.getParentPackage().getSchema().toLowerCase();
            String packageName = function.getParentPackage().getName().toLowerCase();
            return schema + "_" + packageName + "_" + returnType.toLowerCase();
          }
        }
        
        // Check package TABLE OF types
        for (NestedTableType nestedTableType : function.getParentPackage().getNestedTableTypes()) {
          if (nestedTableType.getName().equalsIgnoreCase(returnType)) {
            // Package-level types use DOMAIN references
            String schema = function.getParentPackage().getSchema().toLowerCase();
            String packageName = function.getParentPackage().getName().toLowerCase();
            return schema + "_" + packageName + "_" + returnType.toLowerCase();
          }
        }
      }
    }

    // Fall back to the standard type conversion
    return toPostgre(returnType);
  }
}
