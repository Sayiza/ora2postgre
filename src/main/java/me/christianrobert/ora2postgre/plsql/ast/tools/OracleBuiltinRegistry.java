package me.christianrobert.ora2postgre.plsql.ast.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of Oracle built-in functions and procedures with their PostgreSQL equivalents.
 * This prevents infinite lookup loops for system functions and provides proper transformations.
 */
public class OracleBuiltinRegistry {
  
  /**
   * Interface for Oracle built-in transformations.
   */
  public interface BuiltinTransformer {
    /**
     * Transform Oracle built-in call to PostgreSQL equivalent.
     * @param arguments List of argument expressions
     * @return PostgreSQL equivalent statement
     */
    String transform(java.util.List<String> arguments);
  }
  
  // Registry of Oracle built-in functions/procedures
  private static final Map<String, BuiltinTransformer> BUILTIN_REGISTRY = new HashMap<>();
  
  static {
    // RAISE_APPLICATION_ERROR transformation
    BUILTIN_REGISTRY.put("RAISE_APPLICATION_ERROR", (arguments) -> {
      if (arguments.size() >= 2) {
        String errorCode = arguments.get(0);
        String message = arguments.get(1);
        
        // Oracle error codes -20000 to -20999 are user-defined
        // PostgreSQL RAISE EXCEPTION with ERRCODE
        return "RAISE EXCEPTION " + message + " USING ERRCODE = 'P0001'";
      } else if (arguments.size() == 1) {
        // Only message provided
        return "RAISE EXCEPTION " + arguments.get(0);
      } else {
        return "RAISE EXCEPTION 'Application error'";
      }
    });
    
    // DBMS_OUTPUT.PUT_LINE transformation
    BUILTIN_REGISTRY.put("DBMS_OUTPUT.PUT_LINE", (arguments) -> {
      if (arguments.size() >= 1) {
        return "RAISE NOTICE " + arguments.get(0);
      } else {
        return "RAISE NOTICE 'Debug output'";
      }
    });
    
    // SYSDATE transformation (when used as procedure call, rare but possible)
    BUILTIN_REGISTRY.put("SYSDATE", (arguments) -> {
      return "CURRENT_TIMESTAMP";
    });
    
    // USER transformation
    BUILTIN_REGISTRY.put("USER", (arguments) -> {
      return "CURRENT_USER";
    });
    
    // COMMIT (as procedure call)
    BUILTIN_REGISTRY.put("COMMIT", (arguments) -> {
      return "COMMIT";
    });
    
    // ROLLBACK (as procedure call)
    BUILTIN_REGISTRY.put("ROLLBACK", (arguments) -> {
      return "ROLLBACK";
    });
  }
  
  /**
   * Check if a routine name is an Oracle built-in function/procedure.
   * @param routineName The routine name to check (case-insensitive)
   * @return true if it's a recognized Oracle built-in
   */
  public static boolean isOracleBuiltin(String routineName) {
    if (routineName == null) {
      return false;
    }
    
    String upperName = routineName.toUpperCase();
    return BUILTIN_REGISTRY.containsKey(upperName);
  }
  
  /**
   * Check if a package.routine combination is an Oracle built-in.
   * @param packageName The package name (e.g., "DBMS_OUTPUT")
   * @param routineName The routine name (e.g., "PUT_LINE")
   * @return true if it's a recognized Oracle built-in
   */
  public static boolean isOracleBuiltin(String packageName, String routineName) {
    if (packageName == null || routineName == null) {
      return isOracleBuiltin(routineName);
    }
    
    String fullName = packageName.toUpperCase() + "." + routineName.toUpperCase();
    return BUILTIN_REGISTRY.containsKey(fullName);
  }
  
  /**
   * Transform an Oracle built-in to its PostgreSQL equivalent.
   * @param routineName The Oracle built-in routine name
   * @param arguments List of argument expressions as strings
   * @return PostgreSQL equivalent statement
   */
  public static String transformBuiltin(String routineName, java.util.List<String> arguments) {
    if (routineName == null) {
      return "/* Unknown Oracle built-in */";
    }
    
    String upperName = routineName.toUpperCase();
    BuiltinTransformer transformer = BUILTIN_REGISTRY.get(upperName);
    
    if (transformer != null) {
      return transformer.transform(arguments);
    } else {
      return "/* Unhandled Oracle built-in: " + routineName + " */";
    }
  }
  
  /**
   * Transform an Oracle built-in package procedure to its PostgreSQL equivalent.
   * @param packageName The Oracle package name (e.g., "DBMS_OUTPUT")
   * @param routineName The Oracle routine name (e.g., "PUT_LINE")
   * @param arguments List of argument expressions as strings
   * @return PostgreSQL equivalent statement
   */
  public static String transformBuiltin(String packageName, String routineName, java.util.List<String> arguments) {
    if (packageName == null || routineName == null) {
      return transformBuiltin(routineName, arguments);
    }
    
    String fullName = packageName.toUpperCase() + "." + routineName.toUpperCase();
    BuiltinTransformer transformer = BUILTIN_REGISTRY.get(fullName);
    
    if (transformer != null) {
      return transformer.transform(arguments);
    } else {
      return transformBuiltin(routineName, arguments);
    }
  }
  
  /**
   * Get all registered Oracle built-in names for debugging/logging purposes.
   * @return Set of all registered built-in names
   */
  public static Set<String> getAllBuiltinNames() {
    return BUILTIN_REGISTRY.keySet();
  }
  
  /**
   * Add a custom Oracle built-in transformation.
   * This can be used to extend the registry at runtime.
   * @param routineName The Oracle routine name (will be converted to uppercase)
   * @param transformer The transformation function
   */
  public static void registerBuiltin(String routineName, BuiltinTransformer transformer) {
    if (routineName != null && transformer != null) {
      BUILTIN_REGISTRY.put(routineName.toUpperCase(), transformer);
    }
  }
}