package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

/**
 * AST class representing concatenation from the grammar.
 * Grammar rule: concatenation
 *   : model_expression (AT (LOCAL | TIME ZONE concatenation) | interval_expression)? (
 *       ON OVERFLOW_ (TRUNCATE | ERROR)
 *   )?
 *   | concatenation op = DOUBLE_ASTERISK concatenation
 *   | concatenation op = (ASTERISK | SOLIDUS | MOD) concatenation
 *   | concatenation op = (PLUS_SIGN | MINUS_SIGN) concatenation
 *   | concatenation BAR BAR concatenation
 *   | concatenation COLLATE column_collation_name
 */
public class Concatenation extends PlSqlAst {
  private final ModelExpression modelExpression;
  private final Concatenation leftConcatenation;
  private final String operator; // ||, +, -, *, /, MOD, **, etc.
  private final Concatenation rightConcatenation;
  private final String timeZoneModifier; // LOCAL, TIME ZONE
  private final Concatenation timeZoneExpression;
  private final String overflowAction; // TRUNCATE, ERROR
  private final String collationName;

  // Constructor for simple model expression (most common case)
  public Concatenation(ModelExpression modelExpression) {
    this.modelExpression = modelExpression;
    this.leftConcatenation = null;
    this.operator = null;
    this.rightConcatenation = null;
    this.timeZoneModifier = null;
    this.timeZoneExpression = null;
    this.overflowAction = null;
    this.collationName = null;
  }

  // Constructor for arithmetic/concatenation operations
  public Concatenation(Concatenation leftConcatenation, String operator, Concatenation rightConcatenation) {
    this.modelExpression = null;
    this.leftConcatenation = leftConcatenation;
    this.operator = operator;
    this.rightConcatenation = rightConcatenation;
    this.timeZoneModifier = null;
    this.timeZoneExpression = null;
    this.overflowAction = null;
    this.collationName = null;
  }

  // Constructor for model expression with time zone
  public Concatenation(ModelExpression modelExpression, String timeZoneModifier, 
                      Concatenation timeZoneExpression, String overflowAction) {
    this.modelExpression = modelExpression;
    this.leftConcatenation = null;
    this.operator = null;
    this.rightConcatenation = null;
    this.timeZoneModifier = timeZoneModifier;
    this.timeZoneExpression = timeZoneExpression;
    this.overflowAction = overflowAction;
    this.collationName = null;
  }

  // Constructor for collation
  public Concatenation(Concatenation concatenation, String collationName) {
    this.modelExpression = null;
    this.leftConcatenation = concatenation;
    this.operator = "COLLATE";
    this.rightConcatenation = null;
    this.timeZoneModifier = null;
    this.timeZoneExpression = null;
    this.overflowAction = null;
    this.collationName = collationName;
  }

  public ModelExpression getModelExpression() {
    return modelExpression;
  }

  public Concatenation getLeftConcatenation() {
    return leftConcatenation;
  }

  public String getOperator() {
    return operator;
  }

  public Concatenation getRightConcatenation() {
    return rightConcatenation;
  }

  public String getTimeZoneModifier() {
    return timeZoneModifier;
  }

  public Concatenation getTimeZoneExpression() {
    return timeZoneExpression;
  }

  public String getOverflowAction() {
    return overflowAction;
  }

  public String getCollationName() {
    return collationName;
  }

  public boolean isSimpleModelExpression() {
    return modelExpression != null && operator == null;
  }

  public boolean isArithmeticOperation() {
    return operator != null && !operator.equals("COLLATE");
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    if (isSimpleModelExpression()) {
      StringBuilder sb = new StringBuilder();
      sb.append(modelExpression.toString());
      
      if (timeZoneModifier != null) {
        sb.append(" AT ").append(timeZoneModifier);
        if (timeZoneExpression != null) {
          sb.append(" ").append(timeZoneExpression.toString());
        }
      }
      
      if (overflowAction != null) {
        sb.append(" ON OVERFLOW ").append(overflowAction);
      }
      
      return sb.toString();
    } else if (isArithmeticOperation()) {
      if (operator.equals("COLLATE")) {
        return leftConcatenation.toString() + " COLLATE " + collationName;
      } else {
        return leftConcatenation.toString() + " " + operator + " " + rightConcatenation.toString();
      }
    } else {
      return "/* INVALID CONCATENATION */";
    }
  }

  public String toPostgre(Everything data) {
    if (isSimpleModelExpression()) {
      StringBuilder sb = new StringBuilder();
      sb.append(modelExpression.toPostgre(data));
      
      // PostgreSQL handles time zones differently
      if (timeZoneModifier != null) {
        if (timeZoneModifier.equals("LOCAL")) {
          sb.append(" AT TIME ZONE 'localtime'");
        } else if (timeZoneExpression != null) {
          sb.append(" AT TIME ZONE ").append(timeZoneExpression.toPostgre(data));
        }
      }
      
      // PostgreSQL doesn't have ON OVERFLOW - add comment
      if (overflowAction != null) {
        sb.append(" /* ON OVERFLOW ").append(overflowAction).append(" - manual handling required */");
      }
      
      return sb.toString();
    } else if (isArithmeticOperation()) {
      if (operator.equals("COLLATE")) {
        // PostgreSQL collation syntax is similar
        return leftConcatenation.toPostgre(data) + " COLLATE " + collationName;
      } else {
        String leftPg = leftConcatenation.toPostgre(data);
        String rightPg = rightConcatenation.toPostgre(data);
        String operatorPg = transformOperator(operator);
        return leftPg + " " + operatorPg + " " + rightPg;
      }
    } else {
      return "/* INVALID CONCATENATION */";
    }
  }

  /**
   * Transform Oracle arithmetic operators to PostgreSQL equivalents.
   */
  private String transformOperator(String oracleOperator) {
    if (oracleOperator == null) {
      return "";
    }
    
    switch (oracleOperator) {
      case "||":
        return "||"; // String concatenation - same in PostgreSQL
      case "+":
      case "-":
      case "*":
      case "/":
        return oracleOperator; // Arithmetic operators - same in PostgreSQL
      case "**":
        return "^"; // Power operator - PostgreSQL uses ^
      case "MOD":
        return "%"; // Modulo operator - PostgreSQL uses %
      default:
        return oracleOperator; // Pass through unknown operators
    }
  }
}