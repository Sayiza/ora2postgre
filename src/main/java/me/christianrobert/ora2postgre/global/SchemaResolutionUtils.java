package me.christianrobert.ora2postgre.global;

import me.christianrobert.ora2postgre.oracledb.ColumnMetadata;
import me.christianrobert.ora2postgre.oracledb.SynonymMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.oracledb.ViewMetadata;
import me.christianrobert.ora2postgre.plsql.ast.DataTypeSpec;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.Function;
import me.christianrobert.ora2postgre.plsql.ast.NestedTableType;
import me.christianrobert.ora2postgre.plsql.ast.ObjectType;
import me.christianrobert.ora2postgre.plsql.ast.OraclePackage;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;
import me.christianrobert.ora2postgre.plsql.ast.Procedure;
import me.christianrobert.ora2postgre.plsql.ast.TableReference;
import me.christianrobert.ora2postgre.plsql.ast.Variable;
import me.christianrobert.ora2postgre.plsql.ast.VarrayType;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for Oracle schema resolution and synonym handling.
 * Provides static methods for resolving Oracle object schemas, data types, and function lookups
 * when converting Oracle PL/SQL to PostgreSQL.
 * 
 * These utilities handle the complex Oracle synonym system where table/view/object references
 * might be synonyms pointing to objects in different schemas. PostgreSQL doesn't have synonyms,
 * so the actual schema must be resolved during transformation.
 */
public class SchemaResolutionUtils {

  /**
   * Resolves the actual schema for an Oracle object type, considering synonyms.
   * 
   * @param data The Everything context containing all metadata
   * @param objectTypeName The object type name to resolve
   * @param schemaWhereTheAskingCodeResides The schema context where the lookup is happening
   * @return The resolved schema name, or null if not found
   */
  public static String lookupSchema4ObjectType(Everything data, String objectTypeName, String schemaWhereTheAskingCodeResides) {
    for (ObjectType objectType : data.getObjectTypeSpecAst()) {
      if (objectType.getName().equalsIgnoreCase(objectTypeName)
              && objectType.getSchema().equalsIgnoreCase(schemaWhereTheAskingCodeResides)) {
        return objectType.getSchema();
      }
    }

    // Step 2: Look up synonym in the given schema
    List<SynonymMetadata> matchingSynonyms = data.getSynonyms().stream()
            .filter(s -> s.getSynonymName().equalsIgnoreCase(objectTypeName) &&
                    ( s.getSchema().equalsIgnoreCase(schemaWhereTheAskingCodeResides)
                            || s.getSchema().equalsIgnoreCase("PUBLIC")
                    )
            )
            .toList();

    if (!matchingSynonyms.isEmpty()) {
      SynonymMetadata synonym = matchingSynonyms.get(0);
      // Verify the referenced object exists in tableSql or viewDefinition
      String refSchema = synonym.getReferencedSchema();
      String refObject = synonym.getReferencedObjectName();
      boolean objectExists = data.getObjectTypeBodyAst().stream()
              .anyMatch(t -> t.getName().equalsIgnoreCase(refObject) &&
                      t.getSchema().equalsIgnoreCase(refSchema));

      if (objectExists) {
        return refSchema;
      }
    }

    // Step 3, public synonyms are not included, take the next best!? TODO
    for (ObjectType objectType : data.getObjectTypeSpecAst()) {
      if (objectType.getName().equalsIgnoreCase(objectTypeName)) {
        return objectType.getSchema();
      }
    }
    return null;
  }

  /**
   * Resolves the actual schema for a table or view, considering synonyms.
   * 
   * @param data The Everything context containing all metadata
   * @param tableOrViewName The table or view name to resolve
   * @param schema The schema context where the lookup is happening
   * @return The resolved schema name
   * @throws IllegalStateException if synonym resolution fails
   */
  public static String lookupSchema4Field(Everything data, String tableOrViewName, String schema) {
    // Step 1: Check for exact match in the given schema
    for (TableMetadata t : data.getTableSql()) {
      if (t.getTableName().equalsIgnoreCase(tableOrViewName) && t.getSchema().equalsIgnoreCase(schema)) {
        return t.getSchema();
      }
    }
    for (ViewMetadata v : data.getViewDefinition()) {
      if (v.getViewName().equalsIgnoreCase(tableOrViewName) && v.getSchema().equalsIgnoreCase(schema)) {
        return v.getSchema();
      }
    }

    // Step 2: Look up synonym in the given schema
    List<SynonymMetadata> matchingSynonyms = data.getSynonyms().stream()
            .filter(s -> s.getSynonymName().equalsIgnoreCase(tableOrViewName) &&
                    s.getSchema().equalsIgnoreCase(schema))
            .toList();

    if (matchingSynonyms.size() == 1) {
      SynonymMetadata synonym = matchingSynonyms.get(0);
      // Verify the referenced object exists in tableSql or viewDefinition
      String refSchema = synonym.getReferencedSchema();
      String refObject = synonym.getReferencedObjectName();
      boolean objectExists = data.getTableSql().stream()
              .anyMatch(t -> t.getTableName().equalsIgnoreCase(refObject) &&
                      t.getSchema().equalsIgnoreCase(refSchema)) ||
              data.getViewDefinition().stream()
                      .anyMatch(v -> v.getViewName().equalsIgnoreCase(refObject) &&
                              v.getSchema().equalsIgnoreCase(refSchema));

      if (objectExists) {
        return refSchema;
      } else {
        throw new IllegalStateException("Synonym " + schema + "." + tableOrViewName +
                " points to non-existent object: " + refSchema + "." + refObject);
      }
    } else if (matchingSynonyms.isEmpty()) {
      throw new IllegalStateException("No synonym found for " + schema + "." + tableOrViewName +
              " in schema " + schema);
    } else {
      throw new IllegalStateException("Multiple synonyms found for " + schema + "." + tableOrViewName +
              " in schema " + schema);
    }
  }

  /**
   * Determines the Oracle data type of an expression based on context.
   * 
   * @param data The Everything context containing all metadata
   * @param expression The expression to analyze
   * @param schemaWhereTheStatementIsRunning The schema context
   * @param fromTables List of table references from FROM clause
   * @return The Oracle data type of the expression
   */
  public static String lookUpDataType(Everything data, Expression expression, String schemaWhereTheStatementIsRunning, List<TableReference> fromTables) {
    String rawExpression = expression.toString();
    String identifierPattern = "[a-zA-Z_][a-zA-Z0-9_]*";
    String[] segments = rawExpression.split("\\.");

    String prefix = null;
    String column = null;
    String otherschema = null;

    if (segments.length == 1) {
      // Just a column name
      if (segments[0].matches(identifierPattern)) {
        column = segments[0];
      }
    } else if (segments.length == 2) {
      // table.column
      if (segments[0].matches(identifierPattern) && segments[1].matches(identifierPattern)) {
        prefix = segments[0];
        column = segments[1];
      }
    } else if (segments.length == 3) {
      // schema.table.column
      if (segments[0].matches(identifierPattern) &&
              segments[1].matches(identifierPattern) &&
              segments[2].matches(identifierPattern)) {
        prefix = segments[1];
        column = segments[2];
      }
    }
    if (column != null) {
      String dataType = lookUpDataType(data, column, prefix, otherschema, schemaWhereTheStatementIsRunning, fromTables);
      if (dataType != null) {
        return dataType; // nice it really is a column!
      }
    }

    // what else could it be ... a function:
    String functionReturnType = lookUpFunctionReturnType(data, rawExpression, schemaWhereTheStatementIsRunning);
    if (functionReturnType != null) {
      return functionReturnType;
    }

    return "varchar2"; // TODO: handle other cases like literals, operators, etc.
  }

  /**
   * Determines the Oracle data type of a column in an SQL statement, based on the FROM clause tables/views
   * and considering aliases, table names, synonyms, and optional schema prefix.
   *
   * @param data The Everything context containing all metadata
   * @param targetColumnName                 The column name (e.g., "first_name") or function name in a package reference
   * @param targetColumnTablePrefixExpression The table alias or table name prefix (e.g., "e" or "employees"), or null/empty if none
   * @param targetColumnSchemaPrefixExpression The schema prefix (e.g., "HR" in "HR.employees.first_name"), or null if none
   * @param schemaWhereTheStatementIsRunning The schema context where the statement is running
   * @param fromTables                       List of TableReference objects from the FROM clause
   * @return The Oracle data type of the column (e.g., "VARCHAR2", "NUMBER"), or null if not found or if referencing a package function
   */
  private static String lookUpDataType(Everything data, String targetColumnName, String targetColumnTablePrefixExpression,
                                String targetColumnSchemaPrefixExpression, String schemaWhereTheStatementIsRunning,
                                List<TableReference> fromTables) {
    if (targetColumnName == null || targetColumnName.trim().isEmpty()) {
      return null; // Invalid column name
    }
    if (fromTables == null || fromTables.isEmpty()) {
      return null; // No tables/views to search
    }

    // Step 1: Handle three-part identifier (schema.table.column) if schema prefix is provided
    if (targetColumnSchemaPrefixExpression != null && !targetColumnSchemaPrefixExpression.trim().isEmpty() &&
            targetColumnTablePrefixExpression != null && !targetColumnTablePrefixExpression.trim().isEmpty()) {
      String schema = targetColumnSchemaPrefixExpression.trim();
      String table = targetColumnTablePrefixExpression.trim();
      String dataType = findColumnDataType(data, targetColumnName, schema, table);
      if (dataType != null) {
        return dataType;
      }
      // If schema.table.column doesn't match, assume it might be a package function (e.g., pkg.func)
      return null;
    }

    // Step 2: Handle column with table prefix (alias or table name)
    if (targetColumnTablePrefixExpression != null && !targetColumnTablePrefixExpression.trim().isEmpty()) {
      String prefix = targetColumnTablePrefixExpression.trim();

      // Find matching TableReference by alias or table name
      TableReference matchedTable = null;
      for (TableReference tr : fromTables) {
        String alias = tr.getTableAlias();
        String tableName = tr.getTableName();
        if (alias != null && alias.equalsIgnoreCase(prefix)) {
          matchedTable = tr;
          break;
        }
        if (tableName != null && tableName.equalsIgnoreCase(prefix)) {
          matchedTable = tr;
          break;
        }
      }

      if (matchedTable != null) {
        // Resolve schema using lookupSchema4Field (handles synonyms)
        String schema = lookupSchema4Field(data, matchedTable.getTableName(),
                matchedTable.getSchemaName() != null ? matchedTable.getSchemaName() : schemaWhereTheStatementIsRunning);
        String dataType = findColumnDataType(data, targetColumnName, schema, matchedTable.getTableName());
        if (dataType != null) {
          return dataType;
        }
        // Column not found in matched table; assume package function or invalid reference
        return null;
      }

      // Prefix not found in fromTables; try synonym in the current schema
      try {
        String refSchema = lookupSchema4Field(data, prefix, schemaWhereTheStatementIsRunning);
        String dataType = findColumnDataType(data, targetColumnName, refSchema, prefix);
        if (dataType != null) {
          return dataType;
        }
        // Column not found for synonym; assume package function or invalid reference
        return null;
      } catch (IllegalStateException e) {
        // No synonym or multiple synonyms found; assume package function
        return null;
      }
    }

    // Step 3: Handle column without prefix (must be unique across fromTables)
    List<String> foundDataTypes = new ArrayList<>();
    for (TableReference tr : fromTables) {
      String tableName = tr.getTableName();
      String schema = tr.getSchemaName() != null ? tr.getSchemaName() : schemaWhereTheStatementIsRunning;

      // Resolve schema using lookupSchema4Field (handles synonyms)
      try {
        String resolvedSchema = lookupSchema4Field(data, tableName, schema);
        String dataType = findColumnDataType(data, targetColumnName, resolvedSchema, tableName);
        if (dataType != null) {
          foundDataTypes.add(dataType);
        }
      } catch (IllegalStateException e) {
        // Skip tables with unresolved synonyms
        continue;
      }
    }

    if (foundDataTypes.size() == 1) {
      return foundDataTypes.get(0);
    } else {
      // Ambiguous column or no match; return null
      return null;
    }
  }

  /**
   * Helper method to find the data type of a column in a specific table or view.
   *
   * @param data The Everything context containing all metadata
   * @param columnName The column name to look up
   * @param schema     The schema of the table/view
   * @param tableName  The table or view name
   * @return The Oracle data type, or null if not found
   */
  private static String findColumnDataType(Everything data, String columnName, String schema, String tableName) {
    // Check tables
    for (TableMetadata t : data.getTableSql()) {
      if (t.getSchema().equalsIgnoreCase(schema) && t.getTableName().equalsIgnoreCase(tableName)) {
        for (ColumnMetadata col : t.getColumns()) {
          if (col.getColumnName().equalsIgnoreCase(columnName)) {
            return col.getDataType();
          }
        }
      }
    }

    // Check views
    for (ViewMetadata v : data.getViewDefinition()) {
      if (v.getSchema().equalsIgnoreCase(schema) && v.getViewName().equalsIgnoreCase(tableName)) {
        for (ColumnMetadata col : v.getColumns()) {
          if (col.getColumnName().equalsIgnoreCase(columnName)) {
            return col.getDataType();
          }
        }
      }
    }

    return null;
  }

  /**
   * Unified function for resolving schema and name for both object types and packages.
   * Handles direct lookups and synonym resolution.
   * 
   * @param data The Everything context containing all metadata
   * @param name The object type or package name to look up
   * @param schema The schema context to search in
   * @param type The type of object we're looking for
   * @return SynonymResolutionResult with resolved schema and name, or null if not found
   */
  private static SynonymResolutionResult lookupSchemaAndName(Everything data, String name, String schema, DatabaseObjectType type) {
    // Step 1: Check for direct match in the given schema
    switch (type) {
      case OBJECT_TYPE:
        for (ObjectType objType : data.getObjectTypeSpecAst()) {
          if (objType.getName().equalsIgnoreCase(name) && objType.getSchema().equalsIgnoreCase(schema)) {
            return new SynonymResolutionResult(objType.getSchema(), objType.getName());
          }
        }
        break;
      case PACKAGE:
        for (OraclePackage pkg : data.getPackageSpecAst()) {
          if (pkg.getName().equalsIgnoreCase(name) && pkg.getSchema().equalsIgnoreCase(schema)) {
            return new SynonymResolutionResult(pkg.getSchema(), pkg.getName());
          }
        }
        break;
    }

    // Step 2: Look up synonym in the given schema
    String synonymObjectType = type == DatabaseObjectType.OBJECT_TYPE ? "TYPE" : "PACKAGE";
    List<SynonymMetadata> matchingSynonyms = data.getSynonyms().stream()
            .filter(s -> s.getSynonymName().equalsIgnoreCase(name) &&
                    s.getSchema().equalsIgnoreCase(schema) &&
                    synonymObjectType.equalsIgnoreCase(s.getReferencedObjectType()))
            .toList();

    if (matchingSynonyms.size() == 1) {
      SynonymMetadata synonym = matchingSynonyms.get(0);
      String refSchema = synonym.getReferencedSchema();
      String refObject = synonym.getReferencedObjectName();
      
      // Verify the referenced object exists
      boolean objectExists = false;
      switch (type) {
        case OBJECT_TYPE:
          objectExists = data.getObjectTypeSpecAst().stream()
                  .anyMatch(ot -> ot.getName().equalsIgnoreCase(refObject) &&
                          ot.getSchema().equalsIgnoreCase(refSchema));
          break;
        case PACKAGE:
          objectExists = data.getPackageSpecAst().stream()
                  .anyMatch(pkg -> pkg.getName().equalsIgnoreCase(refObject) &&
                          pkg.getSchema().equalsIgnoreCase(refSchema));
          break;
      }

      if (objectExists) {
        return new SynonymResolutionResult(refSchema, refObject);
      } else {
        throw new IllegalStateException("Synonym " + schema + "." + name +
                " points to non-existent " + type.toString().toLowerCase() + ": " + refSchema + "." + refObject);
      }
    } else if (matchingSynonyms.size() > 1) {
      throw new IllegalStateException("Multiple synonyms found for " + type.toString().toLowerCase() + " " + schema + "." + name);
    }

    // No direct match or synonym found
    return null;
  }

  /**
   * Attempts to resolve a function call expression to its return type.
   * Handles various function call patterns:
   * - package.function(args)
   * - objecttype.function(args) 
   * - schema.package.function(args)
   * - schema.objecttype.function(args)
   * - Chained calls: obj1.getObj2(x).getField(y)
   * 
   * @param data The Everything context containing all metadata
   * @param functionExpression The raw function expression (e.g., "PKG.FUNC(1,2)" or "obj.getX().getY()")
   * @param schemaWhereTheStatementIsRunning The schema context where the statement is running
   * @return The return type of the function, or null if not found or not a function
   */
  private static String lookUpFunctionReturnType(Everything data, String functionExpression, String schemaWhereTheStatementIsRunning) {
    if (functionExpression == null || functionExpression.trim().isEmpty()) {
      return null;
    }

    // Handle chained function calls by processing each segment
    String[] chainSegments = splitFunctionChain(functionExpression);
    if (chainSegments.length == 0) {
      return null;
    }

    String currentType = null;
    String currentSchema = schemaWhereTheStatementIsRunning;

    for (int i = 0; i < chainSegments.length; i++) {
      String segment = chainSegments[i].trim();
      FunctionCallInfo callInfo = parseFunctionCall(segment);
      
      if (callInfo == null) {
        return null; // Not a valid function call
      }

      if (i == 0) {
        // First segment: resolve package/objecttype and function
        currentType = resolveFirstFunctionCall(data, callInfo, currentSchema);
      } else {
        // Subsequent segments: function calls on the result of previous call
        currentType = resolveChainedFunctionCall(data, callInfo, currentType, currentSchema);
      }

      if (currentType == null) {
        return null; // Chain broken
      }
    }

    return currentType;
  }

  /**
   * Splits a function expression into chain segments.
   * E.g., "obj.getX().getY(1,2)" -> ["obj.getX()", "getY(1,2)"]
   */
  private static String[] splitFunctionChain(String expression) {
    // This is a simplified approach - in reality you'd need proper parsing
    // to handle nested parentheses correctly
    String[] segments = expression.split("\\)\\.");
    for (int i = 0; i < segments.length - 1; i++) {
      segments[i] += ")"; // Add back the closing parenthesis
    }
    return segments;
  }

  /**
   * Parses a function call segment to extract components.
   */
  private static FunctionCallInfo parseFunctionCall(String segment) {
    int parenIndex = segment.indexOf('(');
    if (parenIndex == -1) {
      return null; // Not a function call
    }

    String beforeParen = segment.substring(0, parenIndex).trim();
    String[] parts = beforeParen.split("\\.");

    FunctionCallInfo info = new FunctionCallInfo();
    
    if (parts.length == 1) {
      // function()
      info.functionName = parts[0];
    } else if (parts.length == 2) {
      // package.function() or objecttype.function()
      info.packageOrTypeName = parts[0];
      info.functionName = parts[1];
    } else if (parts.length == 3) {
      // schema.package.function() or schema.objecttype.function()
      info.schemaName = parts[0];
      info.packageOrTypeName = parts[1];
      info.functionName = parts[2];
    } else {
      return null; // Too many parts
    }

    return info;
  }

  /**
   * Resolves the first function call in a chain.
   */
  private static String resolveFirstFunctionCall(Everything data, FunctionCallInfo callInfo, String defaultSchema) {
    String targetSchema = callInfo.schemaName != null ? callInfo.schemaName : defaultSchema;
    
    if (callInfo.packageOrTypeName != null) {
      // Look for package function first
      String returnType = findPackageFunction(data, targetSchema, callInfo.packageOrTypeName, callInfo.functionName);
      if (returnType != null) {
        return returnType;
      }

      // Look for object type function
      returnType = findObjectTypeFunction(data, targetSchema, callInfo.packageOrTypeName, callInfo.functionName);
      if (returnType != null) {
        return returnType;
      }

      // Check synonyms for object types
      try {
        SynonymResolutionResult synonymResult = lookupSchemaAndName(data, callInfo.packageOrTypeName, targetSchema, DatabaseObjectType.OBJECT_TYPE);
        if (synonymResult != null && !synonymResult.schema.equals(targetSchema)) {
          return resolveFirstFunctionCall(data,
            new FunctionCallInfo(null, synonymResult.objectTypeName, callInfo.functionName), 
            synonymResult.schema);
        }
      } catch (IllegalStateException e) {
        // Object type synonym not found, try package synonyms
      }

      // Check synonyms for packages
      try {
        SynonymResolutionResult synonymResult = lookupSchemaAndName(data, callInfo.packageOrTypeName, targetSchema, DatabaseObjectType.PACKAGE);
        if (synonymResult != null && !synonymResult.schema.equals(targetSchema)) {
          return resolveFirstFunctionCall(data,
            new FunctionCallInfo(null, synonymResult.objectTypeName, callInfo.functionName), 
            synonymResult.schema);
        }
      } catch (IllegalStateException e) {
        // Package synonym not found, continue
      }
    } else {
      // Just function name - search all packages and object types in schema
      for (OraclePackage pkg : data.getPackageSpecAst()) {
        if (pkg.getSchema().equalsIgnoreCase(targetSchema)) {
          String returnType = findFunctionInPackage(pkg, callInfo.functionName);
          if (returnType != null) {
            return returnType;
          }
        }
      }

      for (ObjectType objType : data.getObjectTypeSpecAst()) {
        if (objType.getSchema().equalsIgnoreCase(targetSchema)) {
          String returnType = findFunctionInObjectType(objType, callInfo.functionName);
          if (returnType != null) {
            return returnType;
          }
        }
      }
    }

    return null;
  }

  /**
   * Resolves a chained function call based on the type returned by previous call.
   */
  private static String resolveChainedFunctionCall(Everything data, FunctionCallInfo callInfo, String objectTypeName, String schema) {
    // For chained calls, we expect the previous call returned an object type
    // Now we look for the function in that object type
    return findObjectTypeFunction(data, schema, objectTypeName, callInfo.functionName);
  }

  /**
   * Finds a function in a specific package.
   */
  private static String findPackageFunction(Everything data, String schema, String packageName, String functionName) {
    for (OraclePackage pkg : data.getPackageSpecAst()) {
      if (pkg.getSchema().equalsIgnoreCase(schema) && pkg.getName().equalsIgnoreCase(packageName)) {
        return findFunctionInPackage(pkg, functionName);
      }
    }
    return null;
  }

  /**
   * Finds a function in a specific object type.
   */
  private static String findObjectTypeFunction(Everything data, String schema, String typeName, String functionName) {
    for (ObjectType objType : data.getObjectTypeSpecAst()) {
      if (objType.getSchema().equalsIgnoreCase(schema) && objType.getName().equalsIgnoreCase(typeName)) {
        return findFunctionInObjectType(objType, functionName);
      }
    }
    return null;
  }

  /**
   * Helper to find a function within a package and return its return type.
   */
  private static String findFunctionInPackage(OraclePackage pkg, String functionName) {
    for (Function func : pkg.getFunctions()) {
      if (func.getName().equalsIgnoreCase(functionName)) {
        return func.getReturnType();
      }
    }
    return null;
  }

  /**
   * Helper to find a function within an object type and return its return type.
   */
  private static String findFunctionInObjectType(ObjectType objType, String functionName) {
    for (Function func : objType.getFunctions()) {
      if (func.getName().equalsIgnoreCase(functionName)) {
        return func.getReturnType();
      }
    }
    return null;
  }

  /**
   * Helper to find a procedure within a package.
   */
  private static boolean findProcedureInPackage(OraclePackage pkg, String procedureName) {
    for (Procedure proc : pkg.getProcedures()) {
      if (proc.getName().equalsIgnoreCase(procedureName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Lookup procedure with schema resolution and synonym support.
   * 
   * @param data The Everything context containing all metadata
   * @param procedureName The name of the procedure to find
   * @param packageName The package name (optional)
   * @param currentSchema The current schema context
   * @return The resolved schema name, or null if not found
   */
  public static String lookupProcedureSchema(Everything data, String procedureName, String packageName, String currentSchema) {
    return lookupProcedureSchema(data, procedureName, packageName, currentSchema, 0);
  }
  
  /**
   * Internal lookup procedure with recursion depth tracking to prevent infinite loops.
   * 
   * @param data The Everything context containing all metadata
   * @param procedureName The name of the procedure to find
   * @param packageName The package name (optional)
   * @param currentSchema The current schema context
   * @param recursionDepth Current recursion depth for infinite loop prevention
   * @return The resolved schema name, or null if not found
   */
  private static String lookupProcedureSchema(Everything data, String procedureName, String packageName, String currentSchema, int recursionDepth) {
    // Prevent infinite recursion in synonym resolution
    if (recursionDepth > 10) {
      System.err.println("Warning: Maximum recursion depth reached in lookupProcedureSchema for " + 
                        procedureName + " (package: " + packageName + ", schema: " + currentSchema + ")");
      return null;
    }
    if (packageName != null) {
      // Look for package.procedure
      for (OraclePackage pkg : data.getPackageSpecAst()) {
        if (pkg.getName().equalsIgnoreCase(packageName) && 
            pkg.getSchema().equalsIgnoreCase(currentSchema)) {
          if (findProcedureInPackage(pkg, procedureName)) {
            return pkg.getSchema();
          }
        }
      }
      for (OraclePackage pkg : data.getPackageBodyAst()) {
        if (pkg.getName().equalsIgnoreCase(packageName) && 
            pkg.getSchema().equalsIgnoreCase(currentSchema)) {
          if (findProcedureInPackage(pkg, procedureName)) {
            return pkg.getSchema();
          }
        }
      }
      
      // Try synonym resolution for package
      try {
        SynonymResolutionResult synonymResult = lookupSchemaAndName(data, packageName, currentSchema, DatabaseObjectType.PACKAGE);
        if (synonymResult != null) {
          return lookupProcedureSchema(data, procedureName, synonymResult.objectTypeName, synonymResult.schema, recursionDepth + 1);
        }
      } catch (IllegalStateException e) {
        // Package synonym not found, continue
      }
    } else {
      // Look for standalone procedure
      for (Procedure proc : data.getStandaloneProcedureAst()) {
        if (proc.getName().equalsIgnoreCase(procedureName) && 
            proc.getSchema().equalsIgnoreCase(currentSchema)) {
          return proc.getSchema();
        }
      }
    }
    
    return null; // Not found
  }

  /**
   * Determine if a routine is a function or procedure.
   * 
   * @param data The Everything context containing all metadata
   * @param routineName The name of the routine
   * @param packageName The package name (optional)
   * @param schema The schema name
   * @return true if it's a function, false if it's a procedure
   */
  public static boolean isFunction(Everything data, String routineName, String packageName, String schema) {
    if (packageName != null) {
      // Check in packages
      for (OraclePackage pkg : data.getPackageSpecAst()) {
        if (pkg.getName().equalsIgnoreCase(packageName) && 
            pkg.getSchema().equalsIgnoreCase(schema)) {
          // Check if it's a function
          for (Function func : pkg.getFunctions()) {
            if (func.getName().equalsIgnoreCase(routineName)) {
              return true;
            }
          }
          // Check if it's a procedure
          for (Procedure proc : pkg.getProcedures()) {
            if (proc.getName().equalsIgnoreCase(routineName)) {
              return false;
            }
          }
        }
      }
      
      for (OraclePackage pkg : data.getPackageBodyAst()) {
        if (pkg.getName().equalsIgnoreCase(packageName) && 
            pkg.getSchema().equalsIgnoreCase(schema)) {
          // Check if it's a function
          for (Function func : pkg.getFunctions()) {
            if (func.getName().equalsIgnoreCase(routineName)) {
              return true;
            }
          }
          // Check if it's a procedure
          for (Procedure proc : pkg.getProcedures()) {
            if (proc.getName().equalsIgnoreCase(routineName)) {
              return false;
            }
          }
        }
      }
    } else {
      // Check standalone functions
      for (Function func : data.getStandaloneFunctionAst()) {
        if (func.getName().equalsIgnoreCase(routineName) && 
            func.getSchema().equalsIgnoreCase(schema)) {
          return true;
        }
      }
      
      // Check standalone procedures
      for (Procedure proc : data.getStandaloneProcedureAst()) {
        if (proc.getName().equalsIgnoreCase(routineName) && 
            proc.getSchema().equalsIgnoreCase(schema)) {
          return false;
        }
      }
    }
    
    // Default to procedure if not found (safer assumption)
    return false;
  }

  /**
   * Determines the schema prefix needed for an expression in PostgreSQL output.
   * Handles function calls to packages/object types with synonym resolution.
   * Returns null for simple column references or non-function expressions.
   *
   * @param data The Everything context containing all metadata
   * @param expression The expression to analyze
   * @param schemaWhereTheStatementIsRunning The schema context where the statement is running
   * @param fromTables List of TableReference objects from the FROM clause (may be null for some contexts)
   * @return The schema prefix to use, or null if no schema prefix is needed
   */
  public static String lookupSchemaForExpression(Everything data, Expression expression, String schemaWhereTheStatementIsRunning, List<TableReference> fromTables) {
    String rawExpression = expression.toString();
    
    // Check if it's a function call pattern
    if (!expression.isFunctionCall()) {
      return null; // Simple column reference - no schema prefix needed
    }
    
    // Parse function call to extract components
    FunctionCallInfo callInfo = parseFunctionCall(rawExpression);
    if (callInfo == null || callInfo.packageOrTypeName == null) {
      return null; // Not a qualified function call
    }
    
    String targetSchema = callInfo.schemaName != null ? callInfo.schemaName : schemaWhereTheStatementIsRunning;
    
    // Try package lookup first
    SynonymResolutionResult packageResult = lookupSchemaAndName(data,
        callInfo.packageOrTypeName, targetSchema, DatabaseObjectType.PACKAGE);
    if (packageResult != null) {
      return packageResult.schema;
    }
    
    // Try object type lookup
    SynonymResolutionResult objectTypeResult = lookupSchemaAndName(data,
        callInfo.packageOrTypeName, targetSchema, DatabaseObjectType.OBJECT_TYPE);
    if (objectTypeResult != null) {
      return objectTypeResult.schema;
    }
    
    return null;
  }

  /**
   * Determines if a given identifier is a known function rather than a variable.
   * This is crucial for distinguishing Oracle array indexing (arr(i)) from function calls (func(i)).
   * 
   * @param data The Everything context containing all metadata
   * @param identifier The identifier to check (e.g., "v_arr", "my_function")
   * @param schema The schema context where the check is happening
   * @param currentFunction The function context where this identifier is being used (may be null)
   * @return true if the identifier is a known function, false if it's likely a variable
   */
  public static boolean isKnownFunction(Everything data, String identifier, String schema, Function currentFunction) {
    if (identifier == null || identifier.trim().isEmpty()) {
      return false;
    }
    
    String cleanIdentifier = identifier.trim();
    
    // Priority 1: Check if it's a variable in current function context
    if (currentFunction != null) {
      // Check function parameters
      if (currentFunction.getParameters() != null) {
        for (Parameter param : currentFunction.getParameters()) {
          if (param.getName().equalsIgnoreCase(cleanIdentifier)) {
            return false; // It's a parameter, not a function
          }
        }
      }
      
      // Check local variables
      if (currentFunction.getVariables() != null) {
        for (Variable var : currentFunction.getVariables()) {
          if (var.getName().equalsIgnoreCase(cleanIdentifier)) {
            return false; // It's a local variable, not a function
          }
        }
      }
      
      // Check local collection types (VARRAY/TABLE OF variables)
      if (currentFunction.getVarrayTypes() != null) {
        for (VarrayType varrayType : currentFunction.getVarrayTypes()) {
          // VarrayType represents a type declaration, but we need to check if variables use this type
          // This is more complex - for now, we'll rely on the variable list above
        }
      }
      
      if (currentFunction.getNestedTableTypes() != null) {
        for (NestedTableType nestedTableType : currentFunction.getNestedTableTypes()) {
          // Similar to VarrayType - type declarations don't directly tell us variable names
        }
      }
    }
    
    // Priority 2: Check standalone functions in the schema
    for (Function func : data.getStandaloneFunctionAst()) {
      if (func.getSchema() != null && func.getSchema().equalsIgnoreCase(schema) &&
          func.getName().equalsIgnoreCase(cleanIdentifier)) {
        return true; // It's a standalone function
      }
    }
    
    // Priority 3: Check functions in packages within the schema
    for (OraclePackage pkg : data.getPackageSpecAst()) {
      if (pkg.getSchema().equalsIgnoreCase(schema)) {
        for (Function func : pkg.getFunctions()) {
          if (func.getName().equalsIgnoreCase(cleanIdentifier)) {
            return true; // It's a package function
          }
        }
      }
    }
    
    // Priority 4: Check functions in object types within the schema
    for (ObjectType objType : data.getObjectTypeSpecAst()) {
      if (objType.getSchema().equalsIgnoreCase(schema)) {
        for (Function func : objType.getFunctions()) {
          if (func.getName().equalsIgnoreCase(cleanIdentifier)) {
            return true; // It's an object type function
          }
        }
      }
    }
    
    // Priority 5: Check built-in Oracle functions (common ones that might be confused with variables)
    if (isBuiltInOracleFunction(cleanIdentifier)) {
      return true;
    }
    
    // If we can't find it as a function and didn't find it as a variable, 
    // assume it's a variable (safer for array indexing)
    return false;
  }
  
  /**
   * Helper method to identify common Oracle built-in functions that might be confused with variables.
   */
  private static boolean isBuiltInOracleFunction(String identifier) {
    // Common Oracle functions that might appear in parentheses syntax
    String upperIdentifier = identifier.toUpperCase();
    switch (upperIdentifier) {
      case "SUBSTR":
      case "LENGTH":
      case "UPPER":
      case "LOWER":
      case "TRIM":
      case "LTRIM":
      case "RTRIM":
      case "DECODE":
      case "NVL":
      case "NVL2":
      case "COALESCE":
      case "TO_CHAR":
      case "TO_NUMBER":
      case "TO_DATE":
      case "SYSDATE":
      case "GREATEST":
      case "LEAST":
      case "ABS":
      case "ROUND":
      case "TRUNC":
      case "FLOOR":
      case "CEIL":
      case "MOD":
      case "POWER":
      case "SQRT":
        return true;
      default:
        return false;
    }
  }

  /**
   * Determines if an identifier represents a collection type constructor.
   * This is the central semantic analysis point for collection constructors,
   * following the architectural principle of keeping semantic logic centralized.
   * 
   * @param data The Everything context containing all metadata
   * @param identifier The identifier to check (e.g., "local_array", "t_numbers")
   * @param currentFunction The function context (null if not in function)
   * @return true if this is a collection type constructor
   */
  public static boolean isCollectionTypeConstructor(Everything data, String identifier, Function currentFunction) {
    if (identifier == null || identifier.trim().isEmpty()) {
      return false;
    }
    
    String cleanIdentifier = identifier.trim();
    
    // 1. Check function-local collection types first (highest priority)
    if (currentFunction != null) {
      if (hasCollectionType(currentFunction, cleanIdentifier)) {
        return true;
      }
    }
    
    // 2. Check all functions for function-local collection types
    for (Function func : data.getAllFunctions()) {
      if (hasCollectionType(func, cleanIdentifier)) {
        return true;
      }
    }
    
    // 3. Check package-level collection types
    for (OraclePackage pkg : data.getPackageSpecAst()) {
      if (hasCollectionType(pkg, cleanIdentifier)) {
        return true;
      }
    }
    
    for (OraclePackage pkg : data.getPackageBodyAst()) {
      if (hasCollectionType(pkg, cleanIdentifier)) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * Transforms a collection constructor to PostgreSQL array syntax.
   * This centralizes all collection constructor transformation logic.
   * 
   * @param data The Everything context containing all metadata
   * @param identifier The collection type name
   * @param arguments The constructor arguments (may be truncated due to parsing limitations)
   * @param currentFunction The function context
   * @return PostgreSQL array syntax
   */
  public static String transformCollectionConstructor(Everything data, String identifier, List<Expression> arguments, Function currentFunction) {
    // Get the collection type information
    DataTypeSpec dataType = getCollectionElementType(data, identifier, currentFunction);
    
    if (arguments == null || arguments.isEmpty()) {
      // Empty constructor
      String postgresType = dataType != null ? dataType.toPostgre(data) : "TEXT";
      return "ARRAY[]::" + postgresType + "[]";
    }
    
    // Transform arguments
    StringBuilder argList = new StringBuilder();
    for (int i = 0; i < arguments.size(); i++) {
      if (i > 0) argList.append(", ");
      argList.append(arguments.get(i).toPostgre(data));
    }
    
    return "ARRAY[" + argList.toString() + "]";
  }
  
  /**
   * Helper: Check if a function has a collection type with the given name.
   */
  private static boolean hasCollectionType(Function func, String typeName) {
    if (func == null || typeName == null) return false;
    
    // Check VARRAY types
    for (VarrayType varray : func.getVarrayTypes()) {
      if (typeName.equalsIgnoreCase(varray.getName())) {
        return true;
      }
    }
    
    // Check nested table types
    for (NestedTableType nestedTable : func.getNestedTableTypes()) {
      if (typeName.equalsIgnoreCase(nestedTable.getName())) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * Helper: Check if a package has a collection type with the given name.
   */
  private static boolean hasCollectionType(OraclePackage pkg, String typeName) {
    if (pkg == null || typeName == null) return false;
    
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
   * Helper: Get the element data type for a collection type.
   */
  private static DataTypeSpec getCollectionElementType(Everything data, String typeName, Function currentFunction) {
    // Check current function first
    if (currentFunction != null) {
      DataTypeSpec type = getCollectionElementTypeFromFunction(currentFunction, typeName);
      if (type != null) return type;
    }
    
    // Check all functions
    for (Function func : data.getAllFunctions()) {
      DataTypeSpec type = getCollectionElementTypeFromFunction(func, typeName);
      if (type != null) return type;
    }
    
    // Check packages
    for (OraclePackage pkg : data.getPackageSpecAst()) {
      DataTypeSpec type = getCollectionElementTypeFromPackage(pkg, typeName);
      if (type != null) return type;
    }
    
    for (OraclePackage pkg : data.getPackageBodyAst()) {
      DataTypeSpec type = getCollectionElementTypeFromPackage(pkg, typeName);
      if (type != null) return type;
    }
    
    return null; // Will default to TEXT in caller
  }
  
  private static DataTypeSpec getCollectionElementTypeFromFunction(Function func, String typeName) {
    for (VarrayType varray : func.getVarrayTypes()) {
      if (typeName.equalsIgnoreCase(varray.getName())) {
        return varray.getDataType();
      }
    }
    
    for (NestedTableType nestedTable : func.getNestedTableTypes()) {
      if (typeName.equalsIgnoreCase(nestedTable.getName())) {
        return nestedTable.getDataType();
      }
    }
    
    return null;
  }
  
  private static DataTypeSpec getCollectionElementTypeFromPackage(OraclePackage pkg, String typeName) {
    for (VarrayType varray : pkg.getVarrayTypes()) {
      if (typeName.equalsIgnoreCase(varray.getName())) {
        return varray.getDataType();
      }
    }
    
    for (NestedTableType nestedTable : pkg.getNestedTableTypes()) {
      if (typeName.equalsIgnoreCase(nestedTable.getName())) {
        return nestedTable.getDataType();
      }
    }
    
    return null;
  }

  /**
   * Enum for specifying the type of database object we're looking for.
   */
  private enum DatabaseObjectType {
    OBJECT_TYPE,
    PACKAGE
  }

  /**
   * Helper class to hold synonym resolution results.
   */
  private static class SynonymResolutionResult {
    final String schema;
    final String objectTypeName;

    SynonymResolutionResult(String schema, String objectTypeName) {
      this.schema = schema;
      this.objectTypeName = objectTypeName;
    }
  }

  /**
   * Helper class to hold parsed function call information.
   */
  private static class FunctionCallInfo {
    String schemaName;
    String packageOrTypeName;
    String functionName;

    FunctionCallInfo() {}

    FunctionCallInfo(String schemaName, String packageOrTypeName, String functionName) {
      this.schemaName = schemaName;
      this.packageOrTypeName = packageOrTypeName;
      this.functionName = functionName;
    }
  }
}