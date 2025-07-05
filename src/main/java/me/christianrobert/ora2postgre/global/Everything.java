package me.christianrobert.ora2postgre.global;

import me.christianrobert.ora2postgre.oracledb.ColumnMetadata;
import me.christianrobert.ora2postgre.oracledb.SynonymMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.oracledb.ViewMetadata;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.Function;
import me.christianrobert.ora2postgre.plsql.ast.ObjectType;
import me.christianrobert.ora2postgre.plsql.ast.OraclePackage;
import me.christianrobert.ora2postgre.plsql.ast.TableReference;
import me.christianrobert.ora2postgre.plsql.ast.Trigger;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class Everything {
  // raw data from the database
  private List<String> userNames = new ArrayList<>();
  private List<TableMetadata> tableSql = new ArrayList<>();
  private List<ViewMetadata> viewDefinition = new ArrayList<>();
  private List<SynonymMetadata> synonyms = new ArrayList<>();
  private List<PlsqlCode> objectTypeSpecPlsql = new ArrayList<>();
  private List<PlsqlCode> objectTypeBodyPlsql = new ArrayList<>();
  private List<PlsqlCode> packageSpecPlsql = new ArrayList<>();
  private List<PlsqlCode> packageBodyPlsql = new ArrayList<>();
  private List<PlsqlCode> triggerPlsql = new ArrayList<>();

  // parsed data
  // TODO table default Expression!!!
  private List<ViewSpecAndQuery> viewSpecAndQueries = new ArrayList<>();
  private List<ObjectType> objectTypeSpecAst = new ArrayList<>();
  private List<ObjectType> objectTypeBodyAst = new ArrayList<>();
  private List<OraclePackage> packageSpecAst = new ArrayList<>();
  private List<OraclePackage> packageBodyAst = new ArrayList<>();
  private List<Trigger> triggerAst = new ArrayList<>();

  private long totalRowCount = 0;
  private int intendations = 0;

  public List<String> getUserNames() {
    return userNames;
  }

  public List<TableMetadata> getTableSql() {
    return tableSql;
  }

  public List<ViewMetadata> getViewDefinition() {
    return viewDefinition;
  }

  public List<SynonymMetadata> getSynonyms() { return synonyms; }

  public List<PlsqlCode> getObjectTypeSpecPlsql() {
    return objectTypeSpecPlsql;
  }

  public List<PlsqlCode> getObjectTypeBodyPlsql() {
    return objectTypeBodyPlsql;
  }

  public List<PlsqlCode> getPackageSpecPlsql() {
    return packageSpecPlsql;
  }

  public List<PlsqlCode> getPackageBodyPlsql() {
    return packageBodyPlsql;
  }

  public List<PlsqlCode> getTriggerPlsql() {
    return triggerPlsql;
  }

  public List<ViewSpecAndQuery> getViewSpecAndQueries() {
    return viewSpecAndQueries;
  }

  public List<ObjectType> getObjectTypeSpecAst() {
    return objectTypeSpecAst;
  }

  public List<ObjectType> getObjectTypeBodyAst() {
    return objectTypeBodyAst;
  }

  public List<OraclePackage> getPackageSpecAst() {
    return packageSpecAst;
  }

  public List<OraclePackage> getPackageBodyAst() {
    return packageBodyAst;
  }

  public List<Trigger> getTriggerAst() {
    return triggerAst;
  }

  public long getTotalRowCount() {
    return totalRowCount;
  }

  public void setTotalRowCount(long totalRowCount) {
    this.totalRowCount = totalRowCount;
  }

  public void intendMore() {
    intendations += 2;
  }

  public void intendLess() {
    intendations -= 2;
  }

  public String getIntendation() {
    if (intendations <= 0) {
      return "";
    }
    return " ".repeat(intendations);
  }

  public String lookupSchema4ObjectType(String objectTypeName, String schemaWhereTheAskingCodeResides) {
    for (ObjectType objectType : objectTypeSpecAst) {
      if (objectType.getName().equalsIgnoreCase(objectTypeName)
              && objectType.getSchema().equalsIgnoreCase(schemaWhereTheAskingCodeResides)) {
        return objectType.getSchema();
      }
    }

    // Step 2: Look up synonym in the given schema
    List<SynonymMetadata> matchingSynonyms = synonyms.stream()
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
      boolean objectExists = objectTypeBodyAst.stream()
              .anyMatch(t -> t.getName().equalsIgnoreCase(refObject) &&
                      t.getSchema().equalsIgnoreCase(refSchema));

      if (objectExists) {
        return refSchema;
      }
    }

    // Step 3, public synonyms are not included, take the next best!? TODO
    for (ObjectType objectType : objectTypeSpecAst) {
      if (objectType.getName().equalsIgnoreCase(objectTypeName)) {
        return objectType.getSchema();
      }
    }
    return null;
  }

  public String lookupSchema4Field(String tableOrViewName, String schema) {
    // Step 1: Check for exact match in the given schema
    for (TableMetadata t : tableSql) {
      if (t.getTableName().equalsIgnoreCase(tableOrViewName) && t.getSchema().equalsIgnoreCase(schema)) {
        return t.getSchema();
      }
    }
    for (ViewMetadata v : viewDefinition) {
      if (v.getViewName().equalsIgnoreCase(tableOrViewName) && v.getSchema().equalsIgnoreCase(schema)) {
        return v.getSchema();
      }
    }

    // Step 2: Look up synonym in the given schema
    List<SynonymMetadata> matchingSynonyms = synonyms.stream()
            .filter(s -> s.getSynonymName().equalsIgnoreCase(tableOrViewName) &&
                    s.getSchema().equalsIgnoreCase(schema))
            .toList();

    if (matchingSynonyms.size() == 1) {
      SynonymMetadata synonym = matchingSynonyms.get(0);
      // Verify the referenced object exists in tableSql or viewDefinition
      String refSchema = synonym.getReferencedSchema();
      String refObject = synonym.getReferencedObjectName();
      boolean objectExists = tableSql.stream()
              .anyMatch(t -> t.getTableName().equalsIgnoreCase(refObject) &&
                      t.getSchema().equalsIgnoreCase(refSchema)) ||
              viewDefinition.stream()
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

  public String lookUpDataType(Expression expression, String schemaWhereTheStatementIsRunning, List<TableReference> fromTables) {
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
      String dataType = lookUpDataType(column, prefix, otherschema, schemaWhereTheStatementIsRunning, fromTables);
      if (dataType != null) {
        return dataType; // nice it really is a column!
      }
    }

    // what else could it be ... a function:
    String functionReturnType = lookUpFunctionReturnType(rawExpression, schemaWhereTheStatementIsRunning);
    if (functionReturnType != null) {
      return functionReturnType;
    }

    return "varchar2"; // TODO: handle other cases like literals, operators, etc.
  }

  /**
   * Determines the Oracle data type of a column in an SQL statement, based on the FROM clause tables/views
   * and considering aliases, table names, synonyms, and optional schema prefix.
   *
   * @param targetColumnName                 The column name (e.g., "first_name") or function name in a package reference
   * @param targetColumnTablePrefixExpression The table alias or table name prefix (e.g., "e" or "employees"), or null/empty if none
   * @param targetColumnSchemaPrefixExpression The schema prefix (e.g., "HR" in "HR.employees.first_name"), or null if none
   * @param schemaWhereTheStatementIsRunning The schema context where the statement is running
   * @param fromTables                       List of TableReference objects from the FROM clause
   * @return The Oracle data type of the column (e.g., "VARCHAR2", "NUMBER"), or null if not found or if referencing a package function
   */
  private String lookUpDataType(String targetColumnName, String targetColumnTablePrefixExpression,
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
      String dataType = findColumnDataType(targetColumnName, schema, table);
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
        String schema = lookupSchema4Field(matchedTable.getTableName(),
                matchedTable.getSchemaName() != null ? matchedTable.getSchemaName() : schemaWhereTheStatementIsRunning);
        String dataType = findColumnDataType(targetColumnName, schema, matchedTable.getTableName());
        if (dataType != null) {
          return dataType;
        }
        // Column not found in matched table; assume package function or invalid reference
        return null;
      }

      // Prefix not found in fromTables; try synonym in the current schema
      try {
        String refSchema = lookupSchema4Field(prefix, schemaWhereTheStatementIsRunning);
        String dataType = findColumnDataType(targetColumnName, refSchema, prefix);
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
        String resolvedSchema = lookupSchema4Field(tableName, schema);
        String dataType = findColumnDataType(targetColumnName, resolvedSchema, tableName);
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
   * @param columnName The column name to look up
   * @param schema     The schema of the table/view
   * @param tableName  The table or view name
   * @/rootfs/podman/podman.socketurn The Oracle data type, or null if not found
   */
  private String findColumnDataType(String columnName, String schema, String tableName) {
    // Check tables
    for (TableMetadata t : tableSql) {
      if (t.getSchema().equalsIgnoreCase(schema) && t.getTableName().equalsIgnoreCase(tableName)) {
        for (ColumnMetadata col : t.getColumns()) {
          if (col.getColumnName().equalsIgnoreCase(columnName)) {
            return col.getDataType();
          }
        }
      }
    }

    // Check views
    for (ViewMetadata v : viewDefinition) {
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
   * @param name The object type or package name to look up
   * @param schema The schema context to search in
   * @param type The type of object we're looking for
   * @return SynonymResolutionResult with resolved schema and name, or null if not found
   */
  private SynonymResolutionResult lookupSchemaAndName(String name, String schema, DatabaseObjectType type) {
    // Step 1: Check for direct match in the given schema
    switch (type) {
      case OBJECT_TYPE:
        for (ObjectType objType : objectTypeSpecAst) {
          if (objType.getName().equalsIgnoreCase(name) && objType.getSchema().equalsIgnoreCase(schema)) {
            return new SynonymResolutionResult(objType.getSchema(), objType.getName());
          }
        }
        break;
      case PACKAGE:
        for (OraclePackage pkg : packageSpecAst) {
          if (pkg.getName().equalsIgnoreCase(name) && pkg.getSchema().equalsIgnoreCase(schema)) {
            return new SynonymResolutionResult(pkg.getSchema(), pkg.getName());
          }
        }
        break;
    }

    // Step 2: Look up synonym in the given schema
    String synonymObjectType = type == DatabaseObjectType.OBJECT_TYPE ? "TYPE" : "PACKAGE";
    List<SynonymMetadata> matchingSynonyms = synonyms.stream()
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
          objectExists = objectTypeSpecAst.stream()
                  .anyMatch(ot -> ot.getName().equalsIgnoreCase(refObject) &&
                          ot.getSchema().equalsIgnoreCase(refSchema));
          break;
        case PACKAGE:
          objectExists = packageSpecAst.stream()
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

  public void findDefaultExpression(String schemaWhereWeAreNow, String myTableName, String columnName) {
    //TODO
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
   * Attempts to resolve a function call expression to its return type.
   * Handles various function call patterns:
   * - package.function(args)
   * - objecttype.function(args) 
   * - schema.package.function(args)
   * - schema.objecttype.function(args)
   * - Chained calls: obj1.getObj2(x).getField(y)
   * 
   * @param functionExpression The raw function expression (e.g., "PKG.FUNC(1,2)" or "obj.getX().getY()")
   * @param schemaWhereTheStatementIsRunning The schema context where the statement is running
   * @return The return type of the function, or null if not found or not a function
   */
  private String lookUpFunctionReturnType(String functionExpression, String schemaWhereTheStatementIsRunning) {
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
        currentType = resolveFirstFunctionCall(callInfo, currentSchema);
      } else {
        // Subsequent segments: function calls on the result of previous call
        currentType = resolveChainedFunctionCall(callInfo, currentType, currentSchema);
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
  private String[] splitFunctionChain(String expression) {
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
  private FunctionCallInfo parseFunctionCall(String segment) {
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
  private String resolveFirstFunctionCall(FunctionCallInfo callInfo, String defaultSchema) {
    String targetSchema = callInfo.schemaName != null ? callInfo.schemaName : defaultSchema;
    
    if (callInfo.packageOrTypeName != null) {
      // Look for package function first
      String returnType = findPackageFunction(targetSchema, callInfo.packageOrTypeName, callInfo.functionName);
      if (returnType != null) {
        return returnType;
      }

      // Look for object type function
      returnType = findObjectTypeFunction(targetSchema, callInfo.packageOrTypeName, callInfo.functionName);
      if (returnType != null) {
        return returnType;
      }

      // Check synonyms for object types
      try {
        SynonymResolutionResult synonymResult = lookupSchemaAndName(callInfo.packageOrTypeName, targetSchema, DatabaseObjectType.OBJECT_TYPE);
        if (synonymResult != null && !synonymResult.schema.equals(targetSchema)) {
          return resolveFirstFunctionCall(
            new FunctionCallInfo(null, synonymResult.objectTypeName, callInfo.functionName), 
            synonymResult.schema);
        }
      } catch (IllegalStateException e) {
        // Object type synonym not found, try package synonyms
      }

      // Check synonyms for packages
      try {
        SynonymResolutionResult synonymResult = lookupSchemaAndName(callInfo.packageOrTypeName, targetSchema, DatabaseObjectType.PACKAGE);
        if (synonymResult != null && !synonymResult.schema.equals(targetSchema)) {
          return resolveFirstFunctionCall(
            new FunctionCallInfo(null, synonymResult.objectTypeName, callInfo.functionName), 
            synonymResult.schema);
        }
      } catch (IllegalStateException e) {
        // Package synonym not found, continue
      }
    } else {
      // Just function name - search all packages and object types in schema
      for (OraclePackage pkg : packageSpecAst) {
        if (pkg.getSchema().equalsIgnoreCase(targetSchema)) {
          String returnType = findFunctionInPackage(pkg, callInfo.functionName);
          if (returnType != null) {
            return returnType;
          }
        }
      }

      for (ObjectType objType : objectTypeSpecAst) {
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
  private String resolveChainedFunctionCall(FunctionCallInfo callInfo, String objectTypeName, String schema) {
    // For chained calls, we expect the previous call returned an object type
    // Now we look for the function in that object type
    return findObjectTypeFunction(schema, objectTypeName, callInfo.functionName);
  }

  /**
   * Finds a function in a specific package.
   */
  private String findPackageFunction(String schema, String packageName, String functionName) {
    for (OraclePackage pkg : packageSpecAst) {
      if (pkg.getSchema().equalsIgnoreCase(schema) && pkg.getName().equalsIgnoreCase(packageName)) {
        return findFunctionInPackage(pkg, functionName);
      }
    }
    return null;
  }

  /**
   * Finds a function in a specific object type.
   */
  private String findObjectTypeFunction(String schema, String typeName, String functionName) {
    for (ObjectType objType : objectTypeSpecAst) {
      if (objType.getSchema().equalsIgnoreCase(schema) && objType.getName().equalsIgnoreCase(typeName)) {
        return findFunctionInObjectType(objType, functionName);
      }
    }
    return null;
  }

  /**
   * Helper to find a function within a package and return its return type.
   */
  private String findFunctionInPackage(OraclePackage pkg, String functionName) {
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
  private String findFunctionInObjectType(ObjectType objType, String functionName) {
    for (Function func : objType.getFunctions()) {
      if (func.getName().equalsIgnoreCase(functionName)) {
        return func.getReturnType();
      }
    }
    return null;
  }

  /**
   * Determines the schema prefix needed for an expression in PostgreSQL output.
   * Handles function calls to packages/object types with synonym resolution.
   * Returns null for simple column references or non-function expressions.
   *
   * @param expression The expression to analyze
   * @param schemaWhereTheStatementIsRunning The schema context where the statement is running
   * @param fromTables List of TableReference objects from the FROM clause (may be null for some contexts)
   * @return The schema prefix to use, or null if no schema prefix is needed
   */
  public String lookupSchemaForExpression(Expression expression, String schemaWhereTheStatementIsRunning, List<TableReference> fromTables) {
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
    SynonymResolutionResult packageResult = lookupSchemaAndName(
        callInfo.packageOrTypeName, targetSchema, DatabaseObjectType.PACKAGE);
    if (packageResult != null) {
      return packageResult.schema;
    }
    
    // Try object type lookup
    SynonymResolutionResult objectTypeResult = lookupSchemaAndName(
        callInfo.packageOrTypeName, targetSchema, DatabaseObjectType.OBJECT_TYPE);
    if (objectTypeResult != null) {
      return objectTypeResult.schema;
    }
    
    return null;
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
