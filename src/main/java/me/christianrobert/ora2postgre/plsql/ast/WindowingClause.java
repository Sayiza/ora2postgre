package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

/**
 * AST class representing a windowing clause in an OVER clause.
 * Handles ROWS/RANGE window frame specifications.
 */
public class WindowingClause extends PlSqlAst {
  
  public enum WindowingType {
    ROWS, RANGE
  }
  
  public enum FrameBound {
    UNBOUNDED_PRECEDING,
    CURRENT_ROW,
    UNBOUNDED_FOLLOWING,
    PRECEDING,
    FOLLOWING
  }
  
  private WindowingType windowingType;
  private FrameBound startBound;
  private Expression startExpression; // for n PRECEDING/FOLLOWING
  private FrameBound endBound;
  private Expression endExpression; // for n PRECEDING/FOLLOWING
  private boolean isBetween; // true for BETWEEN ... AND ..., false for single bound

  public WindowingClause() {
    // Default constructor
  }

  public WindowingClause(WindowingType windowingType, FrameBound startBound, Expression startExpression, 
                        FrameBound endBound, Expression endExpression, boolean isBetween) {
    this.windowingType = windowingType;
    this.startBound = startBound;
    this.startExpression = startExpression;
    this.endBound = endBound;
    this.endExpression = endExpression;
    this.isBetween = isBetween;
  }

  public String toPostgre(Everything data) {
    StringBuilder result = new StringBuilder();
    
    // Add windowing type
    result.append(windowingType.toString());
    
    if (isBetween) {
      result.append(" BETWEEN ");
      result.append(formatFrameBound(startBound, startExpression, data));
      result.append(" AND ");
      result.append(formatFrameBound(endBound, endExpression, data));
    } else {
      result.append(" ");
      result.append(formatFrameBound(startBound, startExpression, data));
    }
    
    return result.toString();
  }
  
  private String formatFrameBound(FrameBound bound, Expression expression, Everything data) {
    switch (bound) {
      case UNBOUNDED_PRECEDING:
        return "UNBOUNDED PRECEDING";
      case CURRENT_ROW:
        return "CURRENT ROW";
      case UNBOUNDED_FOLLOWING:
        return "UNBOUNDED FOLLOWING";
      case PRECEDING:
        return (expression != null ? expression.toPostgre(data) : "1") + " PRECEDING";
      case FOLLOWING:
        return (expression != null ? expression.toPostgre(data) : "1") + " FOLLOWING";
      default:
        return "CURRENT ROW";
    }
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  // Getters and setters
  public WindowingType getWindowingType() {
    return windowingType;
  }

  public void setWindowingType(WindowingType windowingType) {
    this.windowingType = windowingType;
  }

  public FrameBound getStartBound() {
    return startBound;
  }

  public void setStartBound(FrameBound startBound) {
    this.startBound = startBound;
  }

  public Expression getStartExpression() {
    return startExpression;
  }

  public void setStartExpression(Expression startExpression) {
    this.startExpression = startExpression;
  }

  public FrameBound getEndBound() {
    return endBound;
  }

  public void setEndBound(FrameBound endBound) {
    this.endBound = endBound;
  }

  public Expression getEndExpression() {
    return endExpression;
  }

  public void setEndExpression(Expression endExpression) {
    this.endExpression = endExpression;
  }

  public boolean isBetween() {
    return isBetween;
  }

  public void setBetween(boolean between) {
    isBetween = between;
  }

  @Override
  public String toString() {
    return "WindowingClause{" +
        "windowingType=" + windowingType +
        ", startBound=" + startBound +
        ", startExpression=" + startExpression +
        ", endBound=" + endBound +
        ", endExpression=" + endExpression +
        ", isBetween=" + isBetween +
        '}';
  }
}