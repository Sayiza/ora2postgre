package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PackageVariableReferenceTransformer;
import me.christianrobert.ora2postgre.services.TransformationContext;
import java.util.ArrayList;
import java.util.List;

/**
 * AST class representing general_element from the grammar.
 * Grammar rule: general_element
 *   : general_element_part
 *   | general_element ('.' general_element_part)+
 *   | '(' general_element ')'
 * 
 * This class handles Oracle general elements which can be:
 * - Simple identifiers: variable_name
 * - Chained access: obj.method.field  
 * - Function calls: func(args)
 * - Collection indexing: arr(index)
 * - Parenthesized elements: (element)
 */
public class GeneralElement extends PlSqlAst {
  private final GeneralElementPart singlePart;
  private final GeneralElement baseElement;
  private final List<GeneralElementPart> chainedParts;
  private final GeneralElement parenthesizedElement;

  // Constructor for single part: general_element_part
  public GeneralElement(GeneralElementPart singlePart) {
    this.singlePart = singlePart;
    this.baseElement = null;
    this.chainedParts = new ArrayList<>();
    this.parenthesizedElement = null;
  }

  // Constructor for chained access: general_element ('.' general_element_part)+
  public GeneralElement(GeneralElement baseElement, List<GeneralElementPart> chainedParts) {
    this.singlePart = null;
    this.baseElement = baseElement;
    this.chainedParts = chainedParts != null ? new ArrayList<>(chainedParts) : new ArrayList<>();
    this.parenthesizedElement = null;
  }

  // Constructor for parenthesized: '(' general_element ')'
  private GeneralElement(GeneralElement parenthesizedElement) {
    this.singlePart = null;
    this.baseElement = null;
    this.chainedParts = new ArrayList<>();
    this.parenthesizedElement = parenthesizedElement;
  }

  public static GeneralElement parenthesized(GeneralElement element) {
    return new GeneralElement(element);
  }

  public GeneralElementPart getSinglePart() {
    return singlePart;
  }

  public GeneralElement getBaseElement() {
    return baseElement;
  }

  public List<GeneralElementPart> getChainedParts() {
    return new ArrayList<>(chainedParts);
  }

  public GeneralElement getParenthesizedElement() {
    return parenthesizedElement;
  }

  public boolean isSinglePart() {
    return singlePart != null;
  }

  public boolean isChainedAccess() {
    return baseElement != null && !chainedParts.isEmpty();
  }

  public boolean isParenthesized() {
    return parenthesizedElement != null;
  }

  /**
   * Check if this represents a collection indexing operation (e.g., arr(index)).
   */
  public boolean isCollectionIndexing() {
    if (isSinglePart()) {
      return singlePart.isCollectionIndexing();
    }
    return false;
  }

  /**
   * Check if this represents a collection method call (e.g., arr.COUNT).
   */
  public boolean isCollectionMethodCall() {
    if (isChainedAccess()) {
      // Check if this is variable.METHOD pattern
      return chainedParts.size() == 1 && chainedParts.get(0).isSimpleIdentifier();
    }
    return false;
  }

  /**
   * Get the variable name for collection operations.
   */
  public String getVariableName() {
    if (isSinglePart()) {
      return singlePart.getIdExpression();
    } else if (isChainedAccess() && baseElement.isSinglePart()) {
      return baseElement.getSinglePart().getIdExpression();
    }
    return null;
  }

  /**
   * Get the index expression for collection indexing.
   */
  public Expression getIndexExpression() {
    if (isCollectionIndexing()) {
      return singlePart.getIndexExpression();
    }
    return null;
  }

  /**
   * Get the method name for collection method calls.
   */
  public String getMethodName() {
    if (isCollectionMethodCall()) {
      return chainedParts.get(0).getIdExpression();
    }
    return null;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    if (isSinglePart()) {
      return singlePart.toString();
    } else if (isChainedAccess()) {
      StringBuilder sb = new StringBuilder();
      sb.append(baseElement.toString());
      for (GeneralElementPart part : chainedParts) {
        sb.append(".").append(part.toString());
      }
      return sb.toString();
    } else if (isParenthesized()) {
      return "(" + parenthesizedElement.toString() + ")";
    } else {
      return "/* INVALID GENERAL ELEMENT */";
    }
  }

  public String toPostgre(Everything data) {
    if (isParenthesized()) {
      // Handle parenthesized elements
      return "(" + parenthesizedElement.toPostgre(data) + ")";
    } else if (isCollectionIndexing()) {
      // Handle collection indexing: arr(index)
      return transformCollectionIndexing(data);
    } else if (isCollectionMethodCall()) {
      // Handle collection method calls: arr.COUNT
      return transformCollectionMethodCall(data);
    } else if (isSinglePart()) {
      // Handle simple identifiers or function calls
      return transformSinglePart(data);
    } else if (isChainedAccess()) {
      // Handle chained access: obj.field.method
      return transformChainedAccess(data);
    } else {
      return "/* INVALID GENERAL ELEMENT */";
    }
  }

  /**
   * Transform collection indexing operations: arr(index)
   */
  private String transformCollectionIndexing(Everything data) {
    String variableName = getVariableName();
    Expression indexExpr = getIndexExpression();
    
    if (variableName == null || indexExpr == null) {
      return toString(); // Fallback
    }
    
    // Check if this is a block-level table of records access (Phase 1.9)
    if (isBlockLevelTableOfRecordsAccess(variableName, data)) {
      return transformBlockLevelTableOfRecordsAccess(variableName, indexExpr, data);
    }
    
    // Check if this is a package collection variable
    if (PackageVariableReferenceTransformer.isPackageVariableReference(variableName, data)) {
      OraclePackage pkg = PackageVariableReferenceTransformer.findContainingPackage(variableName, data);
      if (pkg != null) {
        // Get element type from the collection definition
        String elementType = getOriginalOracleDataType(variableName, pkg);
        
        // Transform the index expression - this will handle collection methods in the index!
        String transformedIndex = indexExpr.toPostgre(data);
        
        // Transform to package collection element read access
        return PackageVariableReferenceTransformer.transformCollectionElementRead(
            pkg.getSchema(), pkg.getName(), variableName, elementType, transformedIndex);
      }
    }
    
    // Regular array indexing: arr(index) → arr[index]
    String transformedIndex = indexExpr.toPostgre(data);
    return variableName + "[" + transformedIndex + "]";
  }

  /**
   * Transform collection method calls: arr.COUNT
   */
  private String transformCollectionMethodCall(Everything data) {
    String variableName = getVariableName();
    String methodName = getMethodName();
    
    if (variableName == null || methodName == null) {
      return toString(); // Fallback
    }
    
    // FIRST: Check if this is actually record field access instead of a collection method
    if (isRecordFieldAccess(data, variableName, methodName)) {
      return transformRecordFieldAccess(data, variableName, methodName);
    }
    
    // Check if this is a package collection variable
    if (PackageVariableReferenceTransformer.isPackageVariableReference(variableName, data)) {
      OraclePackage pkg = PackageVariableReferenceTransformer.findContainingPackage(variableName, data);
      if (pkg != null) {
        // Transform to package collection method call
        return PackageVariableReferenceTransformer.transformCollectionMethod(
            pkg.getSchema(), pkg.getName(), variableName, methodName);
      }
    }
    
    // Check if this is a block-level table of records collection
    if (isBlockLevelTableOfRecordsAccess(variableName, data)) {
      return transformTableOfRecordsMethodCall(variableName, methodName, data);
    }
    
    // Regular collection method transformation (function-local arrays/varrays)
    switch (methodName.toUpperCase()) {
      case "COUNT":
        return "array_length(" + variableName + ", 1)";
      case "FIRST":
        return "1"; // PostgreSQL arrays are 1-indexed
      case "LAST":
        return "array_length(" + variableName + ", 1)";
      default:
        return "/* Unknown collection method: " + methodName + " */";
    }
  }

  /**
   * Transform collection method calls for table of records (JSONB collections).
   * Pattern: collection.METHOD() where collection is a table of records
   */
  private String transformTableOfRecordsMethodCall(String variableName, String methodName, Everything data) {
    switch (methodName.toUpperCase()) {
      case "COUNT":
        // Count keys in JSONB object: jsonb_object_keys(collection)
        return String.format("jsonb_array_length(jsonb_object_keys(%s))", variableName);
      case "EXISTS":
        // EXISTS requires a parameter - return placeholder for parameter substitution
        // This will be handled by calling code that has access to the parameter
        return String.format("(%s ? %%s)", variableName);
      case "FIRST":
        // Find minimum key (for integer keys): SELECT MIN(key::integer) FROM jsonb_object_keys(collection)
        return String.format("(SELECT MIN(key::integer) FROM jsonb_object_keys(%s) AS k(key) WHERE key ~ '^[0-9]+$')", variableName);
      case "LAST":
        // Find maximum key (for integer keys): SELECT MAX(key::integer) FROM jsonb_object_keys(collection)  
        return String.format("(SELECT MAX(key::integer) FROM jsonb_object_keys(%s) AS k(key) WHERE key ~ '^[0-9]+$')", variableName);
      case "DELETE":
        // DELETE method requires special statement handling or parameters
        return String.format("/* DELETE method for %s - requires statement-level transformation */", variableName);
      default:
        return String.format("/* Unknown table of records method: %s.%s */", variableName, methodName);
    }
  }

  /**
   * Transform simple identifiers or function calls.
   */
  private String transformSinglePart(Everything data) {
    return singlePart.toPostgre(data);
  }

  /**
   * Transform chained access: obj.field.method
   * Special handling for table of records access like l_products(100).prod_id
   */
  private String transformChainedAccess(Everything data) {
    // Check if base element is collection indexing on table of records
    if (baseElement != null && baseElement.isCollectionIndexing()) {
      String collectionName = baseElement.getVariableName();
      Expression indexExpr = baseElement.getIndexExpression();
      
      // Check if this is a table of records collection access
      if (isBlockLevelTableOfRecordsAccess(collectionName, data)) {
        // Transform the base: collection(index) → (collection->'index')::record_type
        String transformedBase = transformBlockLevelTableOfRecordsAccess(collectionName, indexExpr, data);
        
        // Append the field access
        StringBuilder sb = new StringBuilder();
        sb.append(transformedBase);
        for (GeneralElementPart part : chainedParts) {
          sb.append(".").append(part.toPostgre(data));
        }
        return sb.toString();
      }
    }
    
    // Regular chained access transformation
    StringBuilder sb = new StringBuilder();
    sb.append(baseElement.toPostgre(data));
    for (GeneralElementPart part : chainedParts) {
      sb.append(".").append(part.toPostgre(data));
    }
    return sb.toString();
  }

  /**
   * Get the original Oracle data type from a package variable.
   * This is a copy of the method from AssignmentStatement - we'll consolidate later.
   */
  private String getOriginalOracleDataType(String varName, OraclePackage pkg) {
    if (varName == null || pkg == null) {
      return "text";
    }
    
    for (Variable var : pkg.getVariables()) {
      if (varName.equals(var.getName())) {
        if (var.getDataType() != null) {
          DataTypeSpec dataType = var.getDataType();
          
          // Check if this is a custom collection type (varray/table)
          if (dataType.getCustumDataType() != null) {
            // Look up the varray/table type definition in this package
            return getElementTypeFromCollectionDefinition(dataType.getCustumDataType(), pkg);
          } else if (dataType.getNativeDataType() != null) {
            return dataType.getNativeDataType();
          }
        }
      }
    }
    
    return "text";
  }

  /**
   * Get the element type from a collection type definition (varray or table) in the package.
   * This is a copy of the method from AssignmentStatement - we'll consolidate later.
   */
  private String getElementTypeFromCollectionDefinition(String collectionTypeName, OraclePackage pkg) {
    if (collectionTypeName == null || pkg == null) {
      return "text";
    }
    
    // Check varray types
    for (VarrayType varrayType : pkg.getVarrayTypes()) {
      if (collectionTypeName.equalsIgnoreCase(varrayType.getName())) {
        if (varrayType.getDataType() != null) {
          return varrayType.getDataType().getNativeDataType();
        }
      }
    }
    
    // Check nested table types
    for (NestedTableType tableType : pkg.getNestedTableTypes()) {
      if (collectionTypeName.equalsIgnoreCase(tableType.getName())) {
        if (tableType.getDataType() != null) {
          return tableType.getDataType().getNativeDataType();
        }
      }
    }
    
    return "text";
  }

  /**
   * Check if this expression represents record field access rather than a collection method.
   * This distinguishes between record.field (field access) and collection.method() (collection methods).
   */
  private boolean isRecordFieldAccess(Everything data, String variableName, String fieldName) {
    if (variableName == null || fieldName == null) {
      return false;
    }
    
    // Get current function context to check for record types
    Function currentFunction = getCurrentFunctionFromContext();
    if (currentFunction != null) {
      // Check if this variable is declared as a record type in the current function
      if (isVariableOfRecordType(currentFunction, variableName, fieldName)) {        
        return true;
      }
    }
    
    // Check procedure context as well (if available)
    Procedure currentProcedure = getCurrentProcedureFromContext();
    if (currentProcedure != null) {
      // Check if this variable is declared as a record type in the current procedure
      if (isVariableOfRecordTypeInProcedure(currentProcedure, variableName, fieldName)) {        
        return true;
      }
    }
    
    return false;
  }

  /**
   * Transform record field access to PostgreSQL composite type field access.
   * Oracle: record_var.field_name → PostgreSQL: record_var.field_name
   */
  private String transformRecordFieldAccess(Everything data, String variableName, String fieldName) {
    if (variableName == null || fieldName == null) {
      return "/* INVALID RECORD FIELD ACCESS */";
    }
    
    // PostgreSQL composite type field access syntax: composite_value.field_name
    return variableName + "." + fieldName.toLowerCase();
  }

  /**
   * Get current function from transformation context.
   */
  private Function getCurrentFunctionFromContext() {
    TransformationContext context = TransformationContext.getTestInstance();
    return context != null ? context.getCurrentFunction() : null;
  }

  /**
   * Get current procedure from transformation context.
   */
  private Procedure getCurrentProcedureFromContext() {
    TransformationContext context = TransformationContext.getTestInstance();
    return context != null ? context.getCurrentProcedure() : null;
  }

  /**
   * Check if a variable is declared as a record type and the field exists in that record type.
   */
  private boolean isVariableOfRecordType(Function function, String variableName, String fieldName) {
    // Check function variables for record type declarations
    if (function.getVariables() != null) {
      for (Variable variable : function.getVariables()) {
        if (variable.getName().equalsIgnoreCase(variableName)) {
          // Check if this variable's data type is a custom type (potential record type)
          DataTypeSpec dataType = variable.getDataType();
          if (dataType.getCustumDataType() != null) {
            String customTypeName = dataType.getCustumDataType();
            
            // Check if this custom type is a record type in the current function
            for (RecordType recordType : function.getRecordTypes()) {
              if (recordType.getName().equalsIgnoreCase(customTypeName)) {
                // Check if the field exists in this record type
                return recordTypeHasField(recordType, fieldName);
              }
            }
          }
        }
      }
    }
    
    return false;
  }

  /**
   * Check if a variable is declared as a record type in a procedure and the field exists in that record type.
   */
  private boolean isVariableOfRecordTypeInProcedure(Procedure procedure, String variableName, String fieldName) {
    // Check procedure variables for record type declarations
    if (procedure.getVariables() != null) {
      for (Variable variable : procedure.getVariables()) {
        if (variable.getName().equalsIgnoreCase(variableName)) {
          // Check if this variable's data type is a custom type (potential record type)
          DataTypeSpec dataType = variable.getDataType();
          if (dataType.getCustumDataType() != null) {
            String customTypeName = dataType.getCustumDataType();
            
            // Check if this custom type is a record type in the current procedure
            for (RecordType recordType : procedure.getRecordTypes()) {
              if (recordType.getName().equalsIgnoreCase(customTypeName)) {
                // Check if the field exists in this record type
                return recordTypeHasField(recordType, fieldName);
              }
            }
          }
        }
      }
    }
    
    return false;
  }

  /**
   * Check if a record type has a specific field.
   */
  private boolean recordTypeHasField(RecordType recordType, String fieldName) {
    if (recordType.getFields() != null) {
      for (RecordType.RecordField field : recordType.getFields()) {
        if (field.getName().equalsIgnoreCase(fieldName)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check if this is a block-level table of records access.
   * Phase 1.9: Collection Access Transformation
   */
  private boolean isBlockLevelTableOfRecordsAccess(String collectionName, Everything data) {
    if (collectionName == null) {
      return false;
    }
    
    // Try to get transformation context, but provide fallback logic
    TransformationContext context = TransformationContext.getTestInstance();
    
    // If we have context, use it
    if (context != null) {
      // Check procedure context for table of records variables
      Procedure currentProcedure = context.getCurrentProcedure();
      if (currentProcedure != null && isVariableTableOfRecordsInProcedure(currentProcedure, collectionName)) {
        return true;
      }
      
      // Check function context for table of records variables
      Function currentFunction = context.getCurrentFunction();
      if (currentFunction != null && isVariableTableOfRecordsInFunction(currentFunction, collectionName)) {
        return true;
      }
    }
    
    // FALLBACK: Check all packages for table of records variables with this name
    // This handles cases where context is not properly set up
    if (data != null) {
      // Check package bodies (which contain procedure/function implementations)
      if (data.getPackageBodyAst() != null) {
        for (OraclePackage pkg : data.getPackageBodyAst()) {
          // Check package procedures
          if (pkg.getProcedures() != null) {
            for (Procedure procedure : pkg.getProcedures()) {
              if (isVariableTableOfRecordsInProcedure(procedure, collectionName)) {
                return true;
              }
            }
          }
          
          // Check package functions  
          if (pkg.getFunctions() != null) {
            for (Function function : pkg.getFunctions()) {
              if (isVariableTableOfRecordsInFunction(function, collectionName)) {
                return true;
              }
            }
          }
        }
      }
    }
    
    return false;
  }

  /**
   * Check if a variable is a table of records type in a procedure.
   */
  private boolean isVariableTableOfRecordsInProcedure(Procedure procedure, String collectionName) {
    if (procedure.getVariables() == null) {
      return false;
    }
    
    for (Variable variable : procedure.getVariables()) {
      if (variable.getName().equalsIgnoreCase(collectionName)) {
        return variable.isTableOfRecords();
      }
    }
    
    return false;
  }

  /**
   * Check if a variable is a table of records type in a function.
   */
  private boolean isVariableTableOfRecordsInFunction(Function function, String collectionName) {
    if (function.getVariables() == null) {
      return false;
    }
    
    for (Variable variable : function.getVariables()) {
      if (variable.getName().equalsIgnoreCase(collectionName)) {
        return variable.isTableOfRecords();
      }
    }
    
    return false;
  }

  /**
   * Transform block-level table of records access.
   * Pattern: collection(index) → (collection->'index')::record_type
   * Phase 1.9: Collection Access Transformation
   */
  private String transformBlockLevelTableOfRecordsAccess(String collectionName, Expression indexExpr, Everything data) {
    String indexValue = indexExpr.toPostgre(data);
    
    // Fix double quotes issue: strip outer quotes from string literals for JSONB keys
    String jsonbKey = stripOuterQuotesForJsonbKey(indexValue);
    
    // Get record type name for proper composite type casting
    String recordTypeName = getRecordTypeNameForCollection(collectionName, data);
    
    if (recordTypeName != null) {
      // Transform: collection(index) → (collection->'index')::record_type
      return String.format("(%s->'%s')::%s", 
          collectionName, jsonbKey, recordTypeName);
    } else {
      // Fallback without explicit type casting
      return String.format("(%s->'%s')::jsonb", 
          collectionName, jsonbKey);
    }
  }

  /**
   * Strip outer quotes from string literals for use as JSONB object keys.
   * Oracle: l_collection('key') → PostgreSQL JSONB: l_collection->'key'
   * This prevents double-quoting issues where 'key' becomes '{'key'}' instead of 'key'.
   */
  private String stripOuterQuotesForJsonbKey(String indexValue) {
    if (indexValue == null) {
      return indexValue;
    }
    
    // Check if this is a quoted string literal
    if (indexValue.startsWith("'") && indexValue.endsWith("'") && indexValue.length() > 2) {
      // Strip the outer quotes: 'key' → key
      return indexValue.substring(1, indexValue.length() - 1);
    }
    
    // For non-string values (numbers, expressions, etc.), return as-is
    return indexValue;
  }

  /**
   * Get the qualified record type name for a table of records collection.
   * Reuses the same logic as AssignmentStatement for consistency.
   */
  private String getRecordTypeNameForCollection(String collectionName, Everything data) {
    TransformationContext context = TransformationContext.getTestInstance();
    
    // If we have context, try it first
    if (context != null) {
      // Check procedure context
      Procedure currentProcedure = context.getCurrentProcedure();
      if (currentProcedure != null) {
        for (Variable variable : currentProcedure.getVariables()) {
          if (variable.getName().equalsIgnoreCase(collectionName) && variable.isTableOfRecords()) {
            String recordTypeName = variable.getRecordTypeName();
            if (recordTypeName != null) {
              return buildQualifiedRecordTypeName(recordTypeName, currentProcedure, data);
            }
          }
        }
      }
      
      // Check function context
      Function currentFunction = context.getCurrentFunction();
      if (currentFunction != null) {
        for (Variable variable : currentFunction.getVariables()) {
          if (variable.getName().equalsIgnoreCase(collectionName) && variable.isTableOfRecords()) {
            String recordTypeName = variable.getRecordTypeName();
            if (recordTypeName != null) {
              return buildQualifiedRecordTypeName(recordTypeName, currentFunction, data);
            }
          }
        }
      }
    }
    
    // FALLBACK: Search all packages for table of records variables with this name
    if (data != null) {
      // Check package bodies (which contain procedure/function implementations)
      if (data.getPackageBodyAst() != null) {
        for (OraclePackage pkg : data.getPackageBodyAst()) {
          // Check package procedures
          if (pkg.getProcedures() != null) {
            for (Procedure procedure : pkg.getProcedures()) {
              for (Variable variable : procedure.getVariables()) {
                if (variable.getName().equalsIgnoreCase(collectionName) && variable.isTableOfRecords()) {
                  String recordTypeName = variable.getRecordTypeName();
                  if (recordTypeName != null) {
                    return buildQualifiedRecordTypeName(recordTypeName, procedure, data);
                  }
                }
              }
            }
          }
          
          // Check package functions  
          if (pkg.getFunctions() != null) {
            for (Function function : pkg.getFunctions()) {
              for (Variable variable : function.getVariables()) {
                if (variable.getName().equalsIgnoreCase(collectionName) && variable.isTableOfRecords()) {
                  String recordTypeName = variable.getRecordTypeName();
                  if (recordTypeName != null) {
                    return buildQualifiedRecordTypeName(recordTypeName, function, data);
                  }
                }
              }
            }
          }
        }
      }
    }
    
    return null;
  }

  /**
   * Build qualified record type name using the same pattern as AssignmentStatement.
   * Format: schema_package_routine_recordtype (lowercase with underscores)
   */
  private String buildQualifiedRecordTypeName(String recordTypeName, ExecutableRoutine routine, Everything data) {
    if (recordTypeName == null || routine == null) {
      return null;
    }
    
    // Build qualified name: schema_package_routine_recordtype
    StringBuilder qualifiedName = new StringBuilder();
    
    // Add schema (lowercase)
    String schema = routine.getSchema();
    if (schema == null && data != null && !data.getUserNames().isEmpty()) {
      // Use the first schema from the data context as fallback
      schema = data.getUserNames().get(0);
    }
    if (schema == null) {
      schema = "unknown_schema"; // Last resort fallback
    }
    qualifiedName.append(schema.toLowerCase());
    
    // Add package/parent context
    if (routine.getParentPackage() != null) {
      qualifiedName.append("_").append(routine.getParentPackage().getName().toLowerCase());
    } else if (routine.getParentType() != null) {
      qualifiedName.append("_").append(routine.getParentType().getName().toLowerCase());
    } else {
      // Standalone routine - no package context
    }
    
    // Add routine name
    qualifiedName.append("_").append(routine.getName().toLowerCase());
    
    // Add record type name
    qualifiedName.append("_").append(recordTypeName.toLowerCase());
    
    return qualifiedName.toString();
  }
}