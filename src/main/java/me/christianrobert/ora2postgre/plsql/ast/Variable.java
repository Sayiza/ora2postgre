package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.services.TransformationContext;
import jakarta.inject.Inject;
import java.util.List;

public class Variable extends PlSqlAst {

  @Inject
  TransformationContext transformationContext;

  /**
   * For testing purposes - allows manual injection of TransformationContext
   * when CDI container is not available.
   */
  public void setTransformationContext(TransformationContext transformationContext) {
    this.transformationContext = transformationContext;
  }
  private String name;
  private DataTypeSpec dataType;
  private Expression defaultValue;

  public Variable(String name, DataTypeSpec dataType, Expression defaultValue) {
    this.name = name;
    this.dataType = dataType;
    this.defaultValue = defaultValue;
  }

  public String getName() {
    return name;
  }

  public DataTypeSpec getDataType() {
    return dataType;
  }

  public Expression getDefaultValue() { return defaultValue; }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Variable{name=" + name + ", dataType=" + dataType + "}";
  }

  // toJava() method removed - variables stay in PostgreSQL

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    
    // Check if this is a table of records variable
    if (isTableOfRecords()) {
      // Transform table of records to JSONB
      String recordTypeName = getRecordTypeName();
      b.append(name)
              .append(" jsonb := '{}'::jsonb;"); // Initialize as empty JSONB object
      
      // Add comment to indicate original type
      if (recordTypeName != null) {
        b.append(" -- Table of ").append(recordTypeName);
      }
    } else {
      // Regular variable handling
      b.append(name)
              .append(" ")
              .append(dataType.toPostgre(data));
      
      // Add default value if present
      if (defaultValue != null) {
        b.append(" := ").append(defaultValue.toPostgre(data));
      }
    }
    
    return b.toString();
  }

  public String toPostgre(Everything data, Function function) {
    StringBuilder b = new StringBuilder();
    
    // Set up context for table of records detection
    TransformationContext context = transformationContext != null ? transformationContext : TransformationContext.getTestInstance();
    if (context == null) {
      context = new TransformationContext();
      TransformationContext.setTestInstance(context);
      setTransformationContext(context);
    }
    
    // Check if this is a table of records variable (within function context)
    final StringBuilder finalB = b;
    context.withFunctionContext(function, () -> {
      if (isTableOfRecords()) {
        // Transform table of records to JSONB
        String recordTypeName = getRecordTypeName();
        String qualifiedRecordType = getQualifiedRecordTypeName(recordTypeName, function, data);
        
        finalB.append(name)
                .append(" jsonb := '{}'::jsonb;"); // Initialize as empty JSONB object
        
        // Add comment to indicate original type
        if (qualifiedRecordType != null) {
          finalB.append(" -- Table of ").append(qualifiedRecordType);
        }
      } else {
        // Regular variable handling
        finalB.append(name)
                .append(" ")
                .append(dataType.toPostgre(data, function));
        
        // Add default value if present
        if (defaultValue != null) {
          finalB.append(" := ").append(defaultValue.toPostgre(data));
        }
      }
    });
    
    return b.toString();
  }

  public String toPostgre(Everything data, Procedure procedure) {
    StringBuilder b = new StringBuilder();
    
    // Set up context for table of records detection
    TransformationContext context = transformationContext != null ? transformationContext : TransformationContext.getTestInstance();
    if (context == null) {
      context = new TransformationContext();
      TransformationContext.setTestInstance(context);
      setTransformationContext(context);
    }
    
    // Check if this is a table of records variable (within procedure context)
    final StringBuilder finalB = b;
    context.withProcedureContext(procedure, () -> {
      if (isTableOfRecords()) {
        // Transform table of records to JSONB
        String recordTypeName = getRecordTypeName();
        String qualifiedRecordType = getQualifiedRecordTypeName(recordTypeName, procedure, data);
        
        finalB.append(name)
                .append(" jsonb := '{}'::jsonb;"); // Initialize as empty JSONB object
        
        // Add comment to indicate original type
        if (qualifiedRecordType != null) {
          finalB.append(" -- Table of ").append(qualifiedRecordType);
        }
      } else {
        // Regular variable handling
        finalB.append(name)
                .append(" ")
                .append(dataType.toPostgre(data, procedure));
        
        // Add default value if present
        if (defaultValue != null) {
          finalB.append(" := ").append(defaultValue.toPostgre(data));
        }
      }
    });
    
    return b.toString();
  }

  /**
   * Check if this variable represents a "table of records" collection type.
   * This detects Oracle patterns like:
   * - TYPE employee_tab IS TABLE OF employee_rec INDEX BY PLS_INTEGER;
   * - TYPE employee_tab IS TABLE OF employee_rec INDEX BY VARCHAR2(299);
   * - TYPE employee_tab IS TABLE OF employee_rec;
   * 
   * This does NOT detect individual record variables (those use existing composite type system).
   * 
   * NEW APPROACH: Uses actual parsed type declarations instead of naming patterns.
   */
  public boolean isTableOfRecords() {
    if (dataType == null || dataType.getCustumDataType() == null) {
      return false;
    }
    
    String customType = dataType.getCustumDataType();
    
    // Get current transformation context to check for collection type declarations
    TransformationContext context = transformationContext != null ? transformationContext : TransformationContext.getTestInstance();
    if (context != null) {
      // Check function context for NestedTableType declarations
      Function currentFunction = context.getCurrentFunction();
      if (currentFunction != null && isNestedTableTypeInFunction(currentFunction, customType)) {
        return isTableOfRecordsType(currentFunction.getNestedTableTypes(), customType);
      }
      
      // Check procedure context for NestedTableType declarations
      Procedure currentProcedure = context.getCurrentProcedure();
      if (currentProcedure != null && isNestedTableTypeInProcedure(currentProcedure, customType)) {
        return isTableOfRecordsType(currentProcedure.getNestedTableTypes(), customType);
      }
    }
    
    return false;
  }

  /**
   * Check if a function contains a NestedTableType with the given name.
   */
  private boolean isNestedTableTypeInFunction(Function function, String typeName) {
    if (function.getNestedTableTypes() == null || typeName == null) {
      return false;
    }
    
    return function.getNestedTableTypes().stream()
        .anyMatch(nt -> typeName.equalsIgnoreCase(nt.getName()));
  }

  /**
   * Check if a procedure contains a NestedTableType with the given name.
   */
  private boolean isNestedTableTypeInProcedure(Procedure procedure, String typeName) {
    if (procedure.getNestedTableTypes() == null || typeName == null) {
      return false;
    }
    
    return procedure.getNestedTableTypes().stream()
        .anyMatch(nt -> typeName.equalsIgnoreCase(nt.getName()));
  }

  /**
   * Check if a NestedTableType is actually a "table of records" (vs table of simple types).
   * This examines the actual parsed type declaration to see if it references a record type.
   */
  private boolean isTableOfRecordsType(List<NestedTableType> nestedTableTypes, String typeName) {
    if (nestedTableTypes == null || typeName == null) {
      return false;
    }
    
    // Find the NestedTableType with matching name
    NestedTableType matchingType = nestedTableTypes.stream()
        .filter(nt -> typeName.equalsIgnoreCase(nt.getName()))
        .findFirst()
        .orElse(null);
    
    if (matchingType == null) {
      return false;
    }
    
    // Check if the nested table's data type is a custom type (likely a record)
    DataTypeSpec dataTypeSpec = matchingType.getDataType();
    if (dataTypeSpec != null && dataTypeSpec.getCustumDataType() != null) {
      // This is a TABLE OF custom_type - check if that custom type is a record
      return isRecordTypeInCurrentContext(dataTypeSpec.getCustumDataType());
    }
    
    return false;
  }

  /**
   * Check if a custom data type name references a record type in the current context.
   */
  private boolean isRecordTypeInCurrentContext(String recordTypeName) {
    if (recordTypeName == null) {
      return false;
    }
    
    // Get current transformation context to check for record types
    TransformationContext context = transformationContext != null ? transformationContext : TransformationContext.getTestInstance();
    if (context != null) {
      Function currentFunction = context.getCurrentFunction();
      if (currentFunction != null && hasRecordTypeInFunction(currentFunction, recordTypeName)) {
        return true;
      }
      
      Procedure currentProcedure = context.getCurrentProcedure();
      if (currentProcedure != null && hasRecordTypeInProcedure(currentProcedure, recordTypeName)) {
        return true;
      }
    }
    
    return false;
  }

  /**
   * Get the name of the record type that this table of records references.
   * For example, if variable is of type "employee_tab" which is "TABLE OF employee_rec",
   * this returns "employee_rec".
   * 
   * NEW APPROACH: Uses actual parsed type declarations.
   */
  public String getRecordTypeName() {
    if (!isTableOfRecords()) {
      return null;
    }
    
    String customType = dataType.getCustumDataType();
    
    // Get current transformation context to find the collection type
    TransformationContext context = transformationContext != null ? transformationContext : TransformationContext.getTestInstance();
    if (context != null) {
      // Check function context
      Function currentFunction = context.getCurrentFunction();
      if (currentFunction != null) {
        String recordType = getRecordTypeFromNestedTableTypes(currentFunction.getNestedTableTypes(), customType);
        if (recordType != null) {
          return recordType;
        }
      }
      
      // Check procedure context  
      Procedure currentProcedure = context.getCurrentProcedure();
      if (currentProcedure != null) {
        String recordType = getRecordTypeFromNestedTableTypes(currentProcedure.getNestedTableTypes(), customType);
        if (recordType != null) {
          return recordType;
        }
      }
    }
    
    return null;
  }

  /**
   * Extract the record type name from NestedTableType declarations.
   */
  private String getRecordTypeFromNestedTableTypes(List<NestedTableType> nestedTableTypes, String typeName) {
    if (nestedTableTypes == null || typeName == null) {
      return null;
    }
    
    NestedTableType matchingType = nestedTableTypes.stream()
        .filter(nt -> typeName.equalsIgnoreCase(nt.getName()))
        .findFirst()
        .orElse(null);
    
    if (matchingType != null && matchingType.getDataType() != null) {
      return matchingType.getDataType().getCustumDataType();
    }
    
    return null;
  }

  /**
   * Check if a function contains a record type with the given name.
   */
  private boolean hasRecordTypeInFunction(Function function, String recordTypeName) {
    if (function.getRecordTypes() == null || recordTypeName == null) {
      return false;
    }
    
    return function.getRecordTypes().stream()
        .anyMatch(rt -> rt.getName().equalsIgnoreCase(recordTypeName));
  }

  /**
   * Check if a procedure contains a record type with the given name.
   */
  private boolean hasRecordTypeInProcedure(Procedure procedure, String recordTypeName) {
    if (procedure.getRecordTypes() == null || recordTypeName == null) {
      return false;
    }
    
    return procedure.getRecordTypes().stream()
        .anyMatch(rt -> rt.getName().equalsIgnoreCase(recordTypeName));
  }


  /**
   * Get the qualified record type name for comments and documentation.
   * This builds on existing record type infrastructure to show the full composite type name.
   */
  private String getQualifiedRecordTypeName(String recordTypeName, Function function, Everything data) {
    if (recordTypeName == null || function == null) {
      return recordTypeName;
    }
    
    // Build qualified name using existing RecordTypeCollectionManager pattern
    // This follows the same naming convention as the existing record type system
    String functionName = function.getName();
    OraclePackage parentPackage = function.getParentPackage();
    
    if (parentPackage != null) {
      return parentPackage.getSchema().toLowerCase() + "_" + 
             parentPackage.getName().toLowerCase() + "_" + 
             functionName.toLowerCase() + "_" + recordTypeName.toLowerCase();
    } else {
      return functionName.toLowerCase() + "_" + recordTypeName.toLowerCase();
    }
  }

  /**
   * Get the qualified record type name for comments and documentation.
   */
  private String getQualifiedRecordTypeName(String recordTypeName, Procedure procedure, Everything data) {
    if (recordTypeName == null || procedure == null) {
      return recordTypeName;
    }
    
    // Build qualified name using existing RecordTypeCollectionManager pattern
    String procedureName = procedure.getName();
    OraclePackage parentPackage = procedure.getParentPackage();
    
    if (parentPackage != null) {
      return parentPackage.getSchema().toLowerCase() + "_" + 
             parentPackage.getName().toLowerCase() + "_" + 
             procedureName.toLowerCase() + "_" + recordTypeName.toLowerCase();
    } else {
      return procedureName.toLowerCase() + "_" + recordTypeName.toLowerCase();
    }
  }
}