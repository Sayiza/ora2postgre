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
          
          // Extract element type for collections
          String elementType = extractElementDataType(dataType);
          
          // Transform package collection element assignment to direct table access
          String writeCall = PackageVariableReferenceTransformer.transformCollectionElementWrite(
              pkg.getName(), collectionName, elementType, indexPart, expression.toPostgre(data));
          
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
            pkg.getName(), target, dataType, expression.toPostgre(data));
        
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