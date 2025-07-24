package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PackageVariableReferenceTransformer;
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
    
    // Check if this is a package collection variable
    if (PackageVariableReferenceTransformer.isPackageVariableReference(variableName, data)) {
      OraclePackage pkg = PackageVariableReferenceTransformer.findContainingPackage(variableName, data);
      if (pkg != null) {
        // Transform to package collection method call
        return PackageVariableReferenceTransformer.transformCollectionMethod(
            pkg.getSchema(), pkg.getName(), variableName, methodName);
      }
    }
    
    // Regular collection method transformation (function-local collections)
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
   * Transform simple identifiers or function calls.
   */
  private String transformSinglePart(Everything data) {
    return singlePart.toPostgre(data);
  }

  /**
   * Transform chained access: obj.field.method
   */
  private String transformChainedAccess(Everything data) {
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
}