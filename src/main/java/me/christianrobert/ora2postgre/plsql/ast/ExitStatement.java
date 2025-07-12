package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

/**
 * AST class representing EXIT statements in PL/SQL loops.
 * Oracle: EXIT [label_name] [WHEN condition];
 * PostgreSQL: EXIT [label_name] [WHEN condition];
 * 
 * EXIT statements are used to exit loops, either unconditionally or with a WHEN condition.
 * They can also exit specific labeled loops in nested loop scenarios.
 */
public class ExitStatement extends Statement {

  private final String labelName;     // Optional label for nested loop exits
  private final Expression condition; // Optional WHEN condition

  /**
   * Constructor for EXIT statement with optional label and condition.
   * 
   * @param labelName Optional label name for exiting specific nested loops
   * @param condition Optional condition for conditional exits (WHEN clause)
   */
  public ExitStatement(String labelName, Expression condition) {
    this.labelName = labelName;
    this.condition = condition;
  }

  /**
   * Constructor for simple EXIT statement without label or condition.
   */
  public ExitStatement() {
    this(null, null);
  }

  /**
   * Constructor for EXIT with condition but no label.
   */
  public ExitStatement(Expression condition) {
    this(null, condition);
  }

  public String getLabelName() {
    return labelName;
  }

  public Expression getCondition() {
    return condition;
  }

  public boolean hasLabel() {
    return labelName != null && !labelName.trim().isEmpty();
  }

  public boolean hasCondition() {
    return condition != null;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("ExitStatement{");
    if (hasLabel()) {
      sb.append("label='").append(labelName).append("'");
    }
    if (hasCondition()) {
      if (hasLabel()) sb.append(", ");
      sb.append("condition=").append(condition);
    }
    sb.append("}");
    return sb.toString();
  }

  /**
   * Generate PostgreSQL EXIT statement.
   * PostgreSQL syntax is identical to Oracle for EXIT statements.
   */
  @Override
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    
    b.append(data.getIntendation()).append("EXIT");
    
    // Add label name if present
    if (hasLabel()) {
      b.append(" ").append(labelName);
    }
    
    // Add WHEN condition if present  
    if (hasCondition()) {
      b.append(" WHEN ").append(condition.toPostgre(data));
    }
    
    b.append(";\n");
    
    return b.toString();
  }
}