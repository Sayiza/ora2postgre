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
      // Relational operation - transform operator if needed and delegate to children
      String leftPg = leftExpression.toPostgre(data);
      String rightPg = rightExpression.toPostgre(data);
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
}