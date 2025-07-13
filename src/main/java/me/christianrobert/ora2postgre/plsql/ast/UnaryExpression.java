package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

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
}