package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PackageVariableReferenceTransformer;

public class AssignmentStatement extends Statement {
  private final String target; // e.g., "vVariable"
  private final Expression expression; // e.g., Expression{rawText="0"}

  public AssignmentStatement(String target, Expression expression) {
    this.target = target;
    this.expression = expression;
  }

  public String getTarget() { return target; }
  public Expression getExpression() { return expression; }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "AssignmentStatement{target=" + target + ", expression=" + expression + "}";
  }

  // toJava() method removed - assignments stay in PostgreSQL

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    
    // Check if target is a collection indexing operation (e.g., arr(1))
    if (target.contains("(") && target.contains(")")) {
      // Extract collection name and index
      int parenIndex = target.indexOf("(");
      String collectionName = target.substring(0, parenIndex).trim();
      String indexPart = target.substring(parenIndex + 1, target.lastIndexOf(")")).trim();
      
      // Check if this is a package collection variable
      if (PackageVariableReferenceTransformer.isPackageVariableReference(collectionName, data)) {
        OraclePackage pkg = PackageVariableReferenceTransformer.findContainingPackage(collectionName, data);
        if (pkg != null) {
          String dataType = PackageVariableReferenceTransformer.getPackageVariableDataType(collectionName, pkg);
          
          // Extract element type for collections - get directly from varray/table definition
          String elementType = getOriginalOracleDataType(collectionName, pkg);
          
          // Transform the index expression (may contain collection methods like .COUNT)
          String transformedIndex = transformIndexExpression(indexPart, data);
          
          // Transform package collection element assignment to direct table access
          String writeCall = PackageVariableReferenceTransformer.transformCollectionElementWrite(
              pkg.getSchema(), pkg.getName(), collectionName, elementType, transformedIndex, expression.toPostgre(data));
          
          b.append(data.getIntendation())
              .append(writeCall)
              .append(";");
          
          return b.toString();
        }
      }
    }
    
    // Check if target is a regular package variable
    if (PackageVariableReferenceTransformer.isPackageVariableReference(target, data)) {
      OraclePackage pkg = PackageVariableReferenceTransformer.findContainingPackage(target, data);
      if (pkg != null) {
        String dataType = PackageVariableReferenceTransformer.getPackageVariableDataType(target, pkg);
        
        // Transform package variable assignment to direct table access
        String writeCall = PackageVariableReferenceTransformer.transformWrite(
            pkg.getSchema(), pkg.getName(), target, dataType, expression.toPostgre(data));
        
        b.append(data.getIntendation())
            .append(writeCall)
            .append(";");
      } else {
        // Fallback to regular assignment if package not found
        b.append(data.getIntendation())
            .append(target)
            .append(" := ")
            .append(expression.toPostgre(data))
            .append(";");
      }
    } else {
      // Regular local variable assignment
      b.append(data.getIntendation())
          .append(target)
          .append(" := ")
          .append(expression.toPostgre(data))
          .append(";");
    }
    
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
   * Transform an index expression that may contain collection methods.
   * This handles cases like "g_numbers.COUNT" which need to be converted to array_length calls.
   */
  private String transformIndexExpression(String indexExpression, Everything data) {
    if (indexExpression == null || indexExpression.trim().isEmpty()) {
      return indexExpression;
    }
    
    String trimmed = indexExpression.trim();
    
    // Check if this is a collection method call like "g_numbers.COUNT"
    if (trimmed.contains(".")) {
      int dotIndex = trimmed.lastIndexOf(".");
      String variablePart = trimmed.substring(0, dotIndex).trim();
      String methodPart = trimmed.substring(dotIndex + 1).trim();
      
      // Handle collection methods
      switch (methodPart.toUpperCase()) {
        case "COUNT":
          // Check if this is a package collection variable
          if (PackageVariableReferenceTransformer.isPackageVariableReference(variablePart, data)) {
            OraclePackage pkg = PackageVariableReferenceTransformer.findContainingPackage(variablePart, data);
            if (pkg != null) {
              // Transform to package collection count
              return PackageVariableReferenceTransformer.transformCollectionMethod(
                  pkg.getSchema(), pkg.getName(), variablePart, "COUNT");
            }
          } else {
            // Transform to regular array_length call
            return "array_length(" + variablePart + ", 1)";
          }
          break;
          
        case "FIRST":
          // FIRST is always 1 in PostgreSQL arrays (1-indexed)
          return "1";
          
        case "LAST":
          // Check if this is a package collection variable
          if (PackageVariableReferenceTransformer.isPackageVariableReference(variablePart, data)) {
            OraclePackage pkg = PackageVariableReferenceTransformer.findContainingPackage(variablePart, data);
            if (pkg != null) {
              // Transform to package collection last
              return PackageVariableReferenceTransformer.transformCollectionMethod(
                  pkg.getSchema(), pkg.getName(), variablePart, "LAST");
            }
          } else {
            // Transform to regular array_length call
            return "array_length(" + variablePart + ", 1)";
          }
          break;
          
        default:
          // Unknown method, return as-is
          return trimmed;
      }
    }
    
    // No special transformation needed, return as-is
    return trimmed;
  }

  /**
   * Extract the element data type from a collection data type.
   */
  private String extractElementDataType(String collectionType) {
    if (collectionType == null) {
      return "text";
    }
    
    // Handle VARRAY and TABLE OF types
    if (collectionType.toUpperCase().contains("VARRAY") || collectionType.toUpperCase().contains("TABLE")) {
      // Look for "OF type" pattern
      int ofIndex = collectionType.toUpperCase().indexOf(" OF ");
      if (ofIndex != -1) {
        String elementType = collectionType.substring(ofIndex + 4).trim();
        // Remove any trailing size specifications
        if (elementType.contains("(")) {
          elementType = elementType.substring(0, elementType.indexOf("(")).trim();
        }
        return elementType;
      }
    }
    
    return "text"; // Default fallback
  }
}