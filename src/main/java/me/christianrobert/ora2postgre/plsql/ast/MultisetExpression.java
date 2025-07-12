package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

/**
 * AST class representing multiset_expression from the grammar.
 * Grammar rule: multiset_expression
 *   : relational_expression (multiset_type = NOT? (MEMBER | SUBMULTISET) OF? concatenation)?
 *   | multiset_expression MULTISET multiset_operator = (EXCEPT | INTERSECT | UNION) (ALL | DISTINCT)? relational_expression
 */
public class MultisetExpression extends PlSqlAst {
  private final RelationalExpression relationalExpression;
  private final String multisetOperator; // MEMBER, SUBMULTISET, EXCEPT, INTERSECT, UNION
  private final boolean hasNot;
  private final boolean hasOf;
  private final String quantifier; // ALL, DISTINCT
  private final Expression operandExpression; // For MEMBER/SUBMULTISET operations
  private final List<MultisetExpression> operands; // For compound multiset operations

  // Constructor for simple relational expression (most common case)
  public MultisetExpression(RelationalExpression relationalExpression) {
    this.relationalExpression = relationalExpression;
    this.multisetOperator = null;
    this.hasNot = false;
    this.hasOf = false;
    this.quantifier = null;
    this.operandExpression = null;
    this.operands = null;
  }

  // Constructor for multiset operations (MEMBER, SUBMULTISET)
  public MultisetExpression(RelationalExpression relationalExpression, String multisetOperator, 
                           boolean hasNot, boolean hasOf, Expression operandExpression) {
    this.relationalExpression = relationalExpression;
    this.multisetOperator = multisetOperator;
    this.hasNot = hasNot;
    this.hasOf = hasOf;
    this.quantifier = null;
    this.operandExpression = operandExpression;
    this.operands = null;
  }

  // Constructor for compound multiset operations (EXCEPT, INTERSECT, UNION)
  public MultisetExpression(List<MultisetExpression> operands, String multisetOperator, String quantifier) {
    this.relationalExpression = null;
    this.multisetOperator = multisetOperator;
    this.hasNot = false;
    this.hasOf = false;
    this.quantifier = quantifier;
    this.operandExpression = null;
    this.operands = operands;
  }

  public RelationalExpression getRelationalExpression() {
    return relationalExpression;
  }

  public String getMultisetOperator() {
    return multisetOperator;
  }

  public boolean hasNot() {
    return hasNot;
  }

  public boolean hasOf() {
    return hasOf;
  }

  public String getQuantifier() {
    return quantifier;
  }

  public Expression getOperandExpression() {
    return operandExpression;
  }

  public List<MultisetExpression> getOperands() {
    return operands;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    if (multisetOperator == null) {
      // Simple relational expression
      return relationalExpression != null ? relationalExpression.toString() : "";
    } else if (operands != null) {
      // Compound multiset operation
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < operands.size(); i++) {
        if (i > 0) {
          sb.append(" MULTISET ").append(multisetOperator);
          if (quantifier != null) {
            sb.append(" ").append(quantifier);
          }
          sb.append(" ");
        }
        sb.append(operands.get(i).toString());
      }
      return sb.toString();
    } else {
      // MEMBER/SUBMULTISET operation
      StringBuilder sb = new StringBuilder();
      if (relationalExpression != null) {
        sb.append(relationalExpression.toString());
      }
      if (hasNot) {
        sb.append(" NOT");
      }
      sb.append(" ").append(multisetOperator);
      if (hasOf) {
        sb.append(" OF");
      }
      if (operandExpression != null) {
        sb.append(" ").append(operandExpression.toString());
      }
      return sb.toString();
    }
  }

  public String toPostgre(Everything data) {
    if (multisetOperator == null) {
      // Simple relational expression - delegate to child
      return relationalExpression != null ? relationalExpression.toPostgre(data) : "";
    } else {
      // For multiset operations, we'll need to implement PostgreSQL equivalents
      // For now, add a comment indicating this needs manual conversion
      return "/* MULTISET operation: " + toString() + " - manual conversion required */";
    }
  }
}