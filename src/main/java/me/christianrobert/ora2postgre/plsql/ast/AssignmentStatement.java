package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PackageVariableReferenceTransformer;

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
          
          b.append(data.getIntendation())
              .append(writeCall)
              .append(";");
          
          return b.toString();
        }
      }
      
      // Regular array indexing assignment: arr[index] := value
      String transformedIndex = indexExpr.toPostgre(data);
      b.append(data.getIntendation())
          .append(collectionName)
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
        
        b.append(data.getIntendation())
            .append(writeCall)
            .append(";");
        
        return b.toString();
      }
    }
    
    // Regular assignment - let GeneralElement handle its own transformation
    b.append(data.getIntendation())
        .append(target.toPostgre(data))
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

}