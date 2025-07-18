package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PackageVariableReferenceTransformer;

import java.util.List;

/**
 * AST class representing unary_expression from the grammar.
 * Grammar rule: unary_expression
 *   : ('-' | '+') unary_expression
 *   | PRIOR unary_expression
 *   | CONNECT_BY_ROOT unary_expression
 *   | NEW unary_expression
 *   | DISTINCT unary_expression
 *   | ALL unary_expression
 *   | case_expression
 *   | unary_expression '.' (
 *       (COUNT | FIRST | LAST | LIMIT)
 *       | (EXISTS | NEXT | PRIOR) '(' index += expression ')'
 *   )
 *   | quantified_expression
 *   | standard_function
 *   | atom
 *   | implicit_cursor_expression
 */
public class UnaryExpression extends PlSqlAst {
  private final String unaryOperator; // -, +, PRIOR, CONNECT_BY_ROOT, NEW, DISTINCT, ALL
  private final UnaryExpression childExpression; // For unary operations
  private final Expression caseExpression;
  private final Expression quantifiedExpression;
  private final Expression standardFunction;
  private final Expression atom;
  private final Expression implicitCursorExpression;
  private final String collectionMethod; // COUNT, FIRST, LAST, LIMIT, EXISTS, NEXT, PRIOR
  private final List<Expression> methodArguments; // For collection methods with arguments
  private final boolean isArrayIndexing; // True if this represents array indexing: arr(i) -> arr[i]
  private final String arrayVariable; // Variable name for array indexing
  private final Expression indexExpression; // Index expression for array indexing
  private final boolean isCollectionConstructor; // True if this represents collection constructor: type_name(args)
  private final String collectionTypeName; // Type name for collection constructor
  private final List<Expression> constructorArguments; // Arguments for collection constructor

  // Constructor for unary operations (-, +, PRIOR, etc.)
  public UnaryExpression(String unaryOperator, UnaryExpression childExpression) {
    this.unaryOperator = unaryOperator;
    this.childExpression = childExpression;
    this.caseExpression = null;
    this.quantifiedExpression = null;
    this.standardFunction = null;
    this.atom = null;
    this.implicitCursorExpression = null;
    this.collectionMethod = null;
    this.methodArguments = null;
    this.isArrayIndexing = false;
    this.arrayVariable = null;
    this.indexExpression = null;
    this.isCollectionConstructor = false;
    this.collectionTypeName = null;
    this.constructorArguments = null;
  }

  // Constructor for case expressions
  public UnaryExpression(Expression caseExpression) {
    this.unaryOperator = null;
    this.childExpression = null;
    this.caseExpression = caseExpression;
    this.quantifiedExpression = null;
    this.standardFunction = null;
    this.atom = null;
    this.implicitCursorExpression = null;
    this.collectionMethod = null;
    this.methodArguments = null;
    this.isArrayIndexing = false;
    this.arrayVariable = null;
    this.indexExpression = null;
    this.isCollectionConstructor = false;
    this.collectionTypeName = null;
    this.constructorArguments = null;
  }

  // Private constructor for specific types
  private UnaryExpression(Expression caseExpression, Expression quantifiedExpression, Expression standardFunction, Expression atom, Expression implicitCursorExpression) {
    this.unaryOperator = null;
    this.childExpression = null;
    this.caseExpression = caseExpression;
    this.quantifiedExpression = quantifiedExpression;
    this.standardFunction = standardFunction;
    this.atom = atom;
    this.implicitCursorExpression = implicitCursorExpression;
    this.collectionMethod = null;
    this.methodArguments = null;
    this.isArrayIndexing = false;
    this.arrayVariable = null;
    this.indexExpression = null;
    this.isCollectionConstructor = false;
    this.collectionTypeName = null;
    this.constructorArguments = null;
  }

  // Constructor for standard functions
  public static UnaryExpression forStandardFunction(Expression standardFunction) {
    return new UnaryExpression(null, null, standardFunction, null, null);
  }

  // Constructor for atoms
  public static UnaryExpression forAtom(Expression atom) {
    return new UnaryExpression(null, null, null, atom, null);
  }

  // Constructor for collection method calls
  public UnaryExpression(UnaryExpression baseExpression, String collectionMethod, List<Expression> methodArguments) {
    this.unaryOperator = null;
    this.childExpression = baseExpression;
    this.caseExpression = null;
    this.quantifiedExpression = null;
    this.standardFunction = null;
    this.atom = null;
    this.implicitCursorExpression = null;
    this.collectionMethod = collectionMethod;
    this.methodArguments = methodArguments;
    this.isArrayIndexing = false;
    this.arrayVariable = null;
    this.indexExpression = null;
    this.isCollectionConstructor = false;
    this.collectionTypeName = null;
    this.constructorArguments = null;
  }

  // Constructor for array indexing calls
  public UnaryExpression(String arrayVariable, Expression indexExpression) {
    this.unaryOperator = null;
    this.childExpression = null;
    this.caseExpression = null;
    this.quantifiedExpression = null;
    this.standardFunction = null;
    this.atom = null;
    this.implicitCursorExpression = null;
    this.collectionMethod = null;
    this.methodArguments = null;
    this.isArrayIndexing = true;
    this.arrayVariable = arrayVariable;
    this.indexExpression = indexExpression;
    this.isCollectionConstructor = false;
    this.collectionTypeName = null;
    this.constructorArguments = null;
  }

  // Constructor for collection constructor calls
  public UnaryExpression(String collectionTypeName, List<Expression> constructorArguments) {
    this.unaryOperator = null;
    this.childExpression = null;
    this.caseExpression = null;
    this.quantifiedExpression = null;
    this.standardFunction = null;
    this.atom = null;
    this.implicitCursorExpression = null;
    this.collectionMethod = null;
    this.methodArguments = null;
    this.isArrayIndexing = false;
    this.arrayVariable = null;
    this.indexExpression = null;
    this.isCollectionConstructor = true;
    this.collectionTypeName = collectionTypeName;
    this.constructorArguments = constructorArguments;
  }

  public String getUnaryOperator() {
    return unaryOperator;
  }

  public UnaryExpression getChildExpression() {
    return childExpression;
  }

  public Expression getCaseExpression() {
    return caseExpression;
  }

  public Expression getQuantifiedExpression() {
    return quantifiedExpression;
  }

  public Expression getStandardFunction() {
    return standardFunction;
  }

  public Expression getAtom() {
    return atom;
  }

  public Expression getImplicitCursorExpression() {
    return implicitCursorExpression;
  }

  public String getCollectionMethod() {
    return collectionMethod;
  }

  public List<Expression> getMethodArguments() {
    return methodArguments;
  }

  public boolean isUnaryOperation() {
    return unaryOperator != null && childExpression != null;
  }

  public boolean isCaseExpression() {
    return caseExpression != null;
  }

  public boolean isQuantifiedExpression() {
    return quantifiedExpression != null;
  }

  public boolean isStandardFunction() {
    return standardFunction != null;
  }

  public boolean isAtom() {
    return atom != null;
  }

  public boolean isImplicitCursorExpression() {
    return implicitCursorExpression != null;
  }

  public boolean isCollectionMethodCall() {
    return collectionMethod != null;
  }

  public boolean isCollectionConstructor() {
    return isCollectionConstructor;
  }

  public boolean isArrayIndexing() {
    return isArrayIndexing;
  }

  public String getArrayVariable() {
    return arrayVariable;
  }

  public Expression getIndexExpression() {
    return indexExpression;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    if (isUnaryOperation()) {
      return unaryOperator + " " + childExpression.toString();
    } else if (isCaseExpression()) {
      return caseExpression.toString();
    } else if (isQuantifiedExpression()) {
      return quantifiedExpression.toString();
    } else if (isStandardFunction()) {
      return standardFunction.toString();
    } else if (isAtom()) {
      return atom.toString();
    } else if (isImplicitCursorExpression()) {
      return implicitCursorExpression.toString();
    } else if (isCollectionMethodCall()) {
      StringBuilder sb = new StringBuilder();
      if (childExpression != null) {
        sb.append(childExpression.toString());
      }
      sb.append(".").append(collectionMethod);
      if (methodArguments != null && !methodArguments.isEmpty()) {
        sb.append("(");
        for (int i = 0; i < methodArguments.size(); i++) {
          if (i > 0) sb.append(", ");
          sb.append(methodArguments.get(i).toString());
        }
        sb.append(")");
      }
      return sb.toString();
    } else if (isArrayIndexing()) {
      return arrayVariable + "(" + indexExpression.toString() + ")";
    } else {
      return "/* INVALID UNARY EXPRESSION */";
    }
  }

  public String toPostgre(Everything data) {
    if (isUnaryOperation()) {
      String transformedOperator = transformUnaryOperator(unaryOperator);
      return transformedOperator + " " + childExpression.toPostgre(data);
    } else if (isCaseExpression()) {
      return caseExpression.toPostgre(data);
    } else if (isQuantifiedExpression()) {
      return quantifiedExpression.toPostgre(data);
    } else if (isStandardFunction()) {
      return standardFunction.toPostgre(data);
    } else if (isAtom()) {
      return atom.toPostgre(data);
    } else if (isImplicitCursorExpression()) {
      return implicitCursorExpression.toPostgre(data);
    } else if (isCollectionMethodCall()) {
      // Transform Oracle collection methods to PostgreSQL function calls
      return transformCollectionMethodToPostgreSQL(data);
    } else if (isCollectionConstructor()) {
      // Transform Oracle collection constructors to PostgreSQL ARRAY syntax
      return transformCollectionConstructorToPostgreSQL(data);
    } else if (isArrayIndexing()) {
      // Transform Oracle array indexing to PostgreSQL array indexing
      return transformArrayIndexingToPostgreSQL(data);
    } else {
      return "/* INVALID UNARY EXPRESSION */";
    }
  }

  /**
   * Transform Oracle unary operators to PostgreSQL equivalents.
   */
  private String transformUnaryOperator(String oracleOperator) {
    if (oracleOperator == null) {
      return "";
    }
    
    switch (oracleOperator.toUpperCase()) {
      case "-":
      case "+":
        return oracleOperator; // Same in PostgreSQL
      case "PRIOR":
        return "/* PRIOR - hierarchical query operator not directly supported */";
      case "CONNECT_BY_ROOT":
        return "/* CONNECT_BY_ROOT - hierarchical query operator not directly supported */";
      case "NEW":
        return "NEW"; // Similar concept in PostgreSQL triggers
      case "DISTINCT":
      case "ALL":
        return oracleOperator; // Same in PostgreSQL
      default:
        return oracleOperator; // Pass through unknown operators
    }
  }

  /**
   * Transform Oracle collection methods to PostgreSQL function calls.
   * This completely restructures the syntax from Oracle dot notation to PostgreSQL functions.
   */
  private String transformCollectionMethodToPostgreSQL(Everything data) {
    if (collectionMethod == null || childExpression == null) {
      return "/* INVALID COLLECTION METHOD CALL */";
    }
    
    String arrayExpression = childExpression.toPostgre(data);
    
    // Check if this is a package collection variable
    String baseVariable = extractBaseVariableName(arrayExpression);
    if (baseVariable != null && PackageVariableReferenceTransformer.isPackageVariableReference(baseVariable, data)) {
      OraclePackage pkg = PackageVariableReferenceTransformer.findContainingPackage(baseVariable, data);
      if (pkg != null) {
        // Transform package collection method to direct table access
        return PackageVariableReferenceTransformer.transformCollectionMethod(pkg.getSchema(), pkg.getName(), baseVariable, collectionMethod);
      }
    }
    
    // Regular collection method transformation (function-local collections)
    
    switch (collectionMethod.toUpperCase()) {
      case "COUNT":
        // Oracle: arr.COUNT → PostgreSQL: array_length(arr, 1)
        return "array_length(" + arrayExpression + ", 1)";
        
      case "FIRST":
        // Oracle: arr.FIRST → PostgreSQL: 1 (arrays are 1-indexed in PostgreSQL)
        return "1";
        
      case "LAST":
        // Oracle: arr.LAST → PostgreSQL: array_length(arr, 1)
        return "array_length(" + arrayExpression + ", 1)";
        
      case "EXISTS":
        // Oracle: arr.EXISTS(i) → PostgreSQL: (i >= 1 AND i <= array_length(arr, 1))
        if (methodArguments != null && !methodArguments.isEmpty()) {
          String index = methodArguments.get(0).toPostgre(data);
          return "(" + index + " >= 1 AND " + index + " <= array_length(" + arrayExpression + ", 1))";
        } else {
          return "/* EXISTS requires an index argument */";
        }
        
      case "NEXT":
        // Oracle: arr.NEXT(i) → PostgreSQL: (CASE WHEN i < array_length(arr, 1) THEN i + 1 ELSE NULL END)
        if (methodArguments != null && !methodArguments.isEmpty()) {
          String index = methodArguments.get(0).toPostgre(data);
          return "(CASE WHEN " + index + " < array_length(" + arrayExpression + ", 1) THEN " + index + " + 1 ELSE NULL END)";
        } else {
          return "/* NEXT requires an index argument */";
        }
        
      case "PRIOR":
        // Oracle: arr.PRIOR(i) → PostgreSQL: (CASE WHEN i > 1 THEN i - 1 ELSE NULL END)
        if (methodArguments != null && !methodArguments.isEmpty()) {
          String index = methodArguments.get(0).toPostgre(data);
          return "(CASE WHEN " + index + " > 1 THEN " + index + " - 1 ELSE NULL END)";
        } else {
          return "/* PRIOR requires an index argument */";
        }
        
      case "LIMIT":
        // Oracle: arr.LIMIT → PostgreSQL: No direct equivalent, return a comment
        return "/* LIMIT - no direct PostgreSQL equivalent for dynamic array limits */";
        
      default:
        return "/* Unknown collection method: " + collectionMethod + " */";
    }
  }

  /**
   * Transform Oracle array indexing to PostgreSQL array indexing.
   * Oracle uses parentheses: arr(i), PostgreSQL uses brackets: arr[i]
   */
  private String transformArrayIndexingToPostgreSQL(Everything data) {
    if (arrayVariable == null || indexExpression == null) {
      return "/* INVALID ARRAY INDEXING */";
    }
    
    String indexString = indexExpression.toPostgre(data);
    
    // Check if this is a package collection variable
    if (PackageVariableReferenceTransformer.isPackageVariableReference(arrayVariable, data)) {
      OraclePackage pkg = PackageVariableReferenceTransformer.findContainingPackage(arrayVariable, data);
      if (pkg != null) {
        String dataType = PackageVariableReferenceTransformer.getPackageVariableDataType(arrayVariable, pkg);
        
        // For package collections, we need to get the element type
        String elementType = extractElementDataType(dataType);
        
        // Transform package collection element access to direct table access
        return PackageVariableReferenceTransformer.transformCollectionElementRead(
            pkg.getSchema(), pkg.getName(), arrayVariable, elementType, indexString);
      }
    }
    
    // Regular array indexing transformation (function-local collections)
    // Transform Oracle arr(i) to PostgreSQL arr[i]
    return arrayVariable + "[" + indexString + "]";
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

  /**
   * Transform Oracle collection constructors to PostgreSQL ARRAY syntax.
   * Oracle: string_array('a', 'b', 'c') → PostgreSQL: ARRAY['a', 'b', 'c']
   * Oracle: number_table(1, 2, 3) → PostgreSQL: ARRAY[1, 2, 3]
   * Oracle: string_array() → PostgreSQL: ARRAY[]::TEXT[]
   */
  private String transformCollectionConstructorToPostgreSQL(Everything data) {
    if (collectionTypeName == null) {
      return "/* INVALID COLLECTION CONSTRUCTOR */";
    }
    
    // Handle empty constructor: type_name() → ARRAY[]::type[]
    if (constructorArguments == null || constructorArguments.isEmpty()) {
      String baseType = inferPostgreSQLTypeFromCollectionName(collectionTypeName);
      return "ARRAY[]::" + baseType + "[]";
    }
    
    // Handle constructor with arguments: type_name(arg1, arg2, ...) → ARRAY[arg1, arg2, ...]
    StringBuilder sb = new StringBuilder();
    sb.append("ARRAY[");
    
    for (int i = 0; i < constructorArguments.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(constructorArguments.get(i).toPostgre(data));
    }
    
    sb.append("]");
    return sb.toString();
  }

  /**
   * Infer PostgreSQL base type from Oracle collection type name.
   * This is a heuristic approach that can be enhanced with full type context.
   */
  private String inferPostgreSQLTypeFromCollectionName(String typeName) {
    if (typeName == null) return "TEXT";
    
    String lowerTypeName = typeName.toLowerCase();
    
    // Common Oracle collection type name patterns
    if (lowerTypeName.contains("string") || lowerTypeName.contains("varchar") || lowerTypeName.contains("char")) {
      return "TEXT";
    } else if (lowerTypeName.contains("number") || lowerTypeName.contains("numeric") || lowerTypeName.contains("int")) {
      return "NUMERIC";
    } else if (lowerTypeName.contains("date") || lowerTypeName.contains("timestamp")) {
      return "TIMESTAMP";
    } else {
      // Default to TEXT for unknown types
      return "TEXT";
    }
  }
  
  /**
   * Extract the base variable name from a PostgreSQL expression.
   * This is used to identify package collection variables.
   */
  private String extractBaseVariableName(String expression) {
    if (expression == null || expression.trim().isEmpty()) {
      return null;
    }
    
    String trimmed = expression.trim();
    
    // Handle function calls - extract variable name from function parameters
    if (trimmed.startsWith("sys.get_package_collection(")) {
      // This is already a transformed package collection - extract the variable name
      // Expected format: sys.get_package_collection('package', 'variable')
      int firstQuote = trimmed.indexOf("'", trimmed.indexOf(','));
      if (firstQuote != -1) {
        int secondQuote = trimmed.indexOf("'", firstQuote + 1);
        if (secondQuote != -1) {
          return trimmed.substring(firstQuote + 1, secondQuote);
        }
      }
    }
    
    // Handle simple variable names (for function-local collections)
    if (trimmed.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
      return trimmed;
    }
    
    // Handle complex expressions - try to extract the first identifier
    if (trimmed.contains("[") || trimmed.contains("(") || trimmed.contains(".")) {
      // Extract the first identifier before any special characters
      int specialChar = Integer.MAX_VALUE;
      for (char c : new char[]{'[', '(', '.', ' ', '\t', '\n'}) {
        int pos = trimmed.indexOf(c);
        if (pos != -1 && pos < specialChar) {
          specialChar = pos;
        }
      }
      
      if (specialChar != Integer.MAX_VALUE && specialChar > 0) {
        String candidate = trimmed.substring(0, specialChar);
        if (candidate.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
          return candidate;
        }
      }
    }
    
    return null;
  }
}