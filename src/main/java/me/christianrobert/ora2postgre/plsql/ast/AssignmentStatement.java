package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PackageVariableReferenceTransformer;
import me.christianrobert.ora2postgre.services.TransformationContext;

public class AssignmentStatement extends Statement {
  private final GeneralElement target; // e.g., GeneralElement for "vVariable" or "arr(index)"
  private final Expression expression; // e.g., Expression{rawText="0"}

  public AssignmentStatement(GeneralElement target, Expression expression) {
    this.target = target;
    this.expression = expression;
  }

  public GeneralElement getTarget() { return target; }
  public Expression getExpression() { return expression; }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "AssignmentStatement{target=" + target.toString() + ", expression=" + expression + "}";
  }

  // toJava() method removed - assignments stay in PostgreSQL

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    
    // Check if target is a collection indexing operation (e.g., arr(index))
    if (target.isCollectionIndexing()) {
      String collectionName = target.getVariableName();
      Expression indexExpr = target.getIndexExpression();
      
      // Check if this is a block-level table of records assignment (Phase 1.8)
      if (isBlockLevelTableOfRecordsAssignment(collectionName, data)) {
        return transformBlockLevelTableOfRecordsAssignment(collectionName, indexExpr, expression, data);
      }
      
      // Check if this is a package collection variable
      if (PackageVariableReferenceTransformer.isPackageVariableReference(collectionName, data)) {
        OraclePackage pkg = PackageVariableReferenceTransformer.findContainingPackage(collectionName, data);
        if (pkg != null) {
          // Get element type from the collection definition
          String elementType = getOriginalOracleDataType(collectionName, pkg);
          
          // Transform the index expression - this handles collection methods in the index!
          String transformedIndex = indexExpr.toPostgre(data);
          
          // Transform package collection element assignment to direct table access
          String writeCall = PackageVariableReferenceTransformer.transformCollectionElementWrite(
              pkg.getSchema(), pkg.getName(), collectionName, elementType, transformedIndex, expression.toPostgre(data));
          
          b.append(writeCall)
              .append(";");
          
          return b.toString();
        }
      }
      
      // Regular array indexing assignment: arr[index] := value
      String transformedIndex = indexExpr.toPostgre(data);
      b.append(collectionName)
          .append("[")
          .append(transformedIndex)
          .append("] := ")
          .append(expression.toPostgre(data))
          .append(";");
      
      return b.toString();
    }
    
    // Check if target is a regular package variable (simple identifier)
    String targetVariableName = target.getVariableName();
    if (targetVariableName != null && PackageVariableReferenceTransformer.isPackageVariableReference(targetVariableName, data)) {
      OraclePackage pkg = PackageVariableReferenceTransformer.findContainingPackage(targetVariableName, data);
      if (pkg != null) {
        String dataType = PackageVariableReferenceTransformer.getPackageVariableDataType(targetVariableName, pkg);
        
        // Transform package variable assignment to direct table access
        String writeCall = PackageVariableReferenceTransformer.transformWrite(
            pkg.getSchema(), pkg.getName(), targetVariableName, dataType, expression.toPostgre(data));
        
        b.append(writeCall)
            .append(";");
        
        return b.toString();
      }
    }
    
    // Check if target is record field access assignment
    if (isRecordFieldAssignmentTarget(target, data)) {
      String transformedTarget = transformRecordFieldAssignmentTarget(target, data);
      b.append(transformedTarget)
          .append(" := ")
          .append(expression.toPostgre(data))
          .append(";");
      return b.toString();
    }
    
    // Regular assignment - let GeneralElement handle its own transformation
    b.append(target.toPostgre(data))
        .append(" := ")
        .append(expression.toPostgre(data))
        .append(";");
    
    return b.toString();
  }
  
  /**
   * Get the original Oracle data type string from a package variable.
   */
  private String getOriginalOracleDataType(String varName, OraclePackage pkg) {
    if (varName == null || pkg == null) {
      return null;
    }
    
    for (Variable var : pkg.getVariables()) {
      if (varName.equals(var.getName())) {
        // Get the original Oracle data type string
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
    
    return null;
  }

  /**
   * Get the element type from a collection type definition (varray or table) in the package.
   */
  private String getElementTypeFromCollectionDefinition(String collectionTypeName, OraclePackage pkg) {
    if (collectionTypeName == null || pkg == null) {
      return null;
    }
    
    // Check varray types
    for (VarrayType varrayType : pkg.getVarrayTypes()) {
      if (collectionTypeName.equalsIgnoreCase(varrayType.getName())) {
        if (varrayType.getDataType() != null) {
          String elementType = varrayType.getDataType().getNativeDataType();
          return elementType;
        }
      }
    }
    
    // Check nested table types
    for (NestedTableType tableType : pkg.getNestedTableTypes()) {
      if (collectionTypeName.equalsIgnoreCase(tableType.getName())) {
        if (tableType.getDataType() != null) {
          String elementType = tableType.getDataType().getNativeDataType();
          return elementType;
        }
      }
    }
    
    return null;
  }

  /**
   * Get the original Oracle data type string from DataTypeSpec.
   * This preserves Oracle keywords like VARRAY and TABLE for proper type mapping.
   */
  private String getOracleDataTypeString(DataTypeSpec dataTypeSpec) {
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
   * Check if the assignment target is record field access.
   */
  private boolean isRecordFieldAssignmentTarget(GeneralElement target, Everything data) {
    // Check if target is chained access (record.field pattern)
    if (!target.isChainedAccess()) {
      return false;
    }
    
    String variableName = target.getVariableName();
    String methodName = target.getMethodName();
    
    if (variableName == null || methodName == null) {
      return false;
    }
    
    // Get current procedure/function context
    TransformationContext context = TransformationContext.getTestInstance();
    if (context != null) {
      Procedure currentProcedure = context.getCurrentProcedure();
      if (currentProcedure != null) {
        return isVariableOfRecordTypeInProcedure(currentProcedure, variableName, methodName);
      }
      
      Function currentFunction = context.getCurrentFunction();
      if (currentFunction != null) {
        return isVariableOfRecordTypeInFunction(currentFunction, variableName, methodName);
      }
    }
    
    return false;
  }

  /**
   * Transform record field assignment target.
   */
  private String transformRecordFieldAssignmentTarget(GeneralElement target, Everything data) {
    String variableName = target.getVariableName();
    String fieldName = target.getMethodName();
    
    if (variableName == null || fieldName == null) {
      return target.toString();
    }
    
    // PostgreSQL composite type field access syntax: composite_value.field_name
    return variableName + "." + fieldName.toLowerCase();
  }

  /**
   * Check if a variable is declared as a record type in a procedure and the field exists.
   */
  private boolean isVariableOfRecordTypeInProcedure(Procedure procedure, String variableName, String fieldName) {
    if (procedure.getVariables() != null) {
      for (Variable variable : procedure.getVariables()) {
        if (variable.getName().equalsIgnoreCase(variableName)) {
          DataTypeSpec dataType = variable.getDataType();
          if (dataType.getCustumDataType() != null) {
            String customTypeName = dataType.getCustumDataType();
            
            for (RecordType recordType : procedure.getRecordTypes()) {
              if (recordType.getName().equalsIgnoreCase(customTypeName)) {
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
   * Check if a variable is declared as a record type in a function and the field exists.
   */
  private boolean isVariableOfRecordTypeInFunction(Function function, String variableName, String fieldName) {
    if (function.getVariables() != null) {
      for (Variable variable : function.getVariables()) {
        if (variable.getName().equalsIgnoreCase(variableName)) {
          DataTypeSpec dataType = variable.getDataType();
          if (dataType.getCustumDataType() != null) {
            String customTypeName = dataType.getCustumDataType();
            
            for (RecordType recordType : function.getRecordTypes()) {
              if (recordType.getName().equalsIgnoreCase(customTypeName)) {
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
   * Check if this is a block-level table of records assignment.
   * Phase 1.8: Table of Records Assignment Transformation
   */
  private boolean isBlockLevelTableOfRecordsAssignment(String collectionName, Everything data) {
    if (collectionName == null) {
      return false;
    }
    
    // Get current transformation context
    TransformationContext context = TransformationContext.getTestInstance();
    if (context == null) {
      return false;
    }
    
    // Check procedure context for table of records variables
    Procedure currentProcedure = context.getCurrentProcedure();
    if (currentProcedure != null) {
      return isVariableTableOfRecordsInProcedure(currentProcedure, collectionName);
    }
    
    // Check function context for table of records variables
    Function currentFunction = context.getCurrentFunction();
    if (currentFunction != null) {
      return isVariableTableOfRecordsInFunction(currentFunction, collectionName);
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
   * Transform block-level table of records assignment.
   * Pattern: collection[index] := record_value
   * Transformation: collection := jsonb_set(collection, '{index}', to_jsonb(record_value))
   * Phase 1.8: Table of Records Assignment Transformation
   */
  private String transformBlockLevelTableOfRecordsAssignment(String collectionName, Expression indexExpr, 
                                                           Expression value, Everything data) {
    String indexValue = indexExpr.toPostgre(data);
    String recordValue = value.toPostgre(data);
    
    // Get record type name for proper composite type casting
    String recordTypeName = getRecordTypeNameForCollection(collectionName, data);
    
    if (recordTypeName != null) {
      // If we have explicit record construction, ensure proper composite type casting
      if (recordValue.contains("ROW(") || recordValue.contains("::")) {
        // Value already includes type casting - use as is
        return String.format("%s := jsonb_set(%s, '{%s}', to_jsonb(%s));", 
            collectionName, collectionName, indexValue, recordValue);
      } else {
        // Add composite type casting for the record value
        return String.format("%s := jsonb_set(%s, '{%s}', to_jsonb((%s)::%s));", 
            collectionName, collectionName, indexValue, recordValue, recordTypeName);
      }
    } else {
      // Fallback without explicit type casting
      return String.format("%s := jsonb_set(%s, '{%s}', to_jsonb(%s));", 
          collectionName, collectionName, indexValue, recordValue);
    }
  }

  /**
   * Get the qualified record type name for a table of records collection.
   * Builds qualified names using the same pattern as Variable.toPostgre()
   */
  private String getRecordTypeNameForCollection(String collectionName, Everything data) {
    TransformationContext context = TransformationContext.getTestInstance();
    if (context == null) {
      return null;
    }
    
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
    
    return null;
  }

  /**
   * Build qualified record type name using the same pattern as Variable class.
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