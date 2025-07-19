package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

/**
 * AST class representing relational_expression from the grammar.
 * Grammar rule: relational_expression
 *   : relational_expression relational_operator relational_expression
 *   | compound_expression
 */
public class RelationalExpression extends PlSqlAst {
  private final CompoundExpression compoundExpression;
  private final RelationalExpression leftExpression;
  private final String relationalOperator; // =, <>, !=, <, >, <=, >=, etc.
  private final RelationalExpression rightExpression;

  // Constructor for simple compound expression (most common case)
  public RelationalExpression(CompoundExpression compoundExpression) {
    this.compoundExpression = compoundExpression;
    this.leftExpression = null;
    this.relationalOperator = null;
    this.rightExpression = null;
  }

  // Constructor for relational operations (comparison)
  public RelationalExpression(RelationalExpression leftExpression, String relationalOperator, 
                             RelationalExpression rightExpression) {
    this.compoundExpression = null;
    this.leftExpression = leftExpression;
    this.relationalOperator = relationalOperator;
    this.rightExpression = rightExpression;
  }

  public CompoundExpression getCompoundExpression() {
    return compoundExpression;
  }

  public RelationalExpression getLeftExpression() {
    return leftExpression;
  }

  public String getRelationalOperator() {
    return relationalOperator;
  }

  public RelationalExpression getRightExpression() {
    return rightExpression;
  }

  public boolean isSimpleCompoundExpression() {
    return compoundExpression != null;
  }

  public boolean isRelationalOperation() {
    return relationalOperator != null;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    if (isSimpleCompoundExpression()) {
      return compoundExpression.toString();
    } else if (isRelationalOperation()) {
      return leftExpression.toString() + " " + relationalOperator + " " + rightExpression.toString();
    } else {
      return "/* INVALID RELATIONAL EXPRESSION */";
    }
  }

  public String toPostgre(Everything data) {
    if (isSimpleCompoundExpression()) {
      // Simple compound expression - delegate to child
      return compoundExpression.toPostgre(data);
    } else if (isRelationalOperation()) {
      // Check for Oracle varray .LIMIT expressions in comparisons
      String leftPg = leftExpression.toPostgre(data);
      String rightPg = rightExpression.toPostgre(data);
      
      // Detect .LIMIT expressions and transform the entire boolean expression
      boolean leftHasLimit = containsLimitExpression(leftPg);
      boolean rightHasLimit = containsLimitExpression(rightPg);
      
      if (leftHasLimit || rightHasLimit) {
        return transformLimitComparison(leftPg, relationalOperator, rightPg, leftHasLimit, rightHasLimit);
      }
      
      // Regular relational operation - transform operator if needed and delegate to children
      String operatorPg = transformRelationalOperator(relationalOperator);
      return leftPg + " " + operatorPg + " " + rightPg;
    } else {
      return "/* INVALID RELATIONAL EXPRESSION */";
    }
  }

  /**
   * Transform Oracle relational operators to PostgreSQL equivalents.
   */
  private String transformRelationalOperator(String oracleOperator) {
    if (oracleOperator == null) {
      return "";
    }
    
    switch (oracleOperator.toUpperCase()) {
      case "<>":
      case "!=":
      case "^=":
        return "<>"; // PostgreSQL standard not-equal
      case "=":
      case "<":
      case ">":
      case "<=":
      case ">=":
        return oracleOperator; // Same in both Oracle and PostgreSQL
      default:
        return oracleOperator; // Pass through unknown operators
    }
  }

  /**
   * Check if a PostgreSQL expression contains a .LIMIT comment indicating Oracle varray limit usage.
   */
  private boolean containsLimitExpression(String postgresExpression) {
    return postgresExpression != null && 
           postgresExpression.contains("/* LIMIT - no direct PostgreSQL equivalent for dynamic array limits */");
  }

  /**
   * Transform Oracle varray .LIMIT comparisons to appropriate PostgreSQL boolean expressions.
   * Since PostgreSQL arrays don't have artificial limits, we transform these comparisons
   * to reflect that PostgreSQL arrays can grow dynamically within available memory.
   * 
   * Examples:
   * - something < arr.LIMIT  → TRUE (always room to grow)
   * - something > arr.LIMIT  → FALSE (never exceeds limit)
   * - something = arr.LIMIT  → FALSE (no fixed limit to equal)
   * - arr.LIMIT < something  → FALSE (limit doesn't constrain)
   * - arr.LIMIT > something  → TRUE (limit always larger)
   */
  private String transformLimitComparison(String leftPg, String operator, String rightPg, 
                                        boolean leftHasLimit, boolean rightHasLimit) {
    if (operator == null) {
      return "TRUE /* Oracle varray limit check - always true in PostgreSQL */";
    }
    
    String op = operator.trim().toUpperCase();
    String comment = " /* Oracle varray limit check - PostgreSQL arrays have no fixed limits */";
    
    // Handle cases where .LIMIT is on the left side
    if (leftHasLimit) {
      switch (op) {
        case "<":
        case "<=":
          // arr.LIMIT < something  →  FALSE (limit is conceptually infinite)
          // arr.LIMIT <= something →  FALSE (limit is conceptually infinite)
          return "FALSE" + comment;
        case ">":
        case ">=":
          // arr.LIMIT > something  →  TRUE (limit is conceptually infinite)
          // arr.LIMIT >= something →  TRUE (limit is conceptually infinite)
          return "TRUE" + comment;
        case "=":
          // arr.LIMIT = something  →  FALSE (no fixed limit to equal)
          return "FALSE" + comment;
        case "<>":
        case "!=":
        case "^=":
          // arr.LIMIT <> something →  TRUE (no fixed limit, so always different)
          return "TRUE" + comment;
        default:
          return "TRUE" + comment;
      }
    }
    
    // Handle cases where .LIMIT is on the right side
    if (rightHasLimit) {
      switch (op) {
        case "<":
        case "<=":
          // something < arr.LIMIT  →  TRUE (always room to grow)
          // something <= arr.LIMIT →  TRUE (always room to grow)
          return "TRUE" + comment;
        case ">":
        case ">=":
          // something > arr.LIMIT  →  FALSE (never exceeds conceptual infinite limit)
          // something >= arr.LIMIT →  FALSE (never reaches conceptual infinite limit)
          return "FALSE" + comment;
        case "=":
          // something = arr.LIMIT  →  FALSE (no fixed limit to equal)
          return "FALSE" + comment;
        case "<>":
        case "!=":
        case "^=":
          // something <> arr.LIMIT →  TRUE (no fixed limit, so always different)
          return "TRUE" + comment;
        default:
          return "TRUE" + comment;
      }
    }
    
    // Fallback - should not reach here if containsLimitExpression is working correctly
    return "TRUE" + comment;
  }
}