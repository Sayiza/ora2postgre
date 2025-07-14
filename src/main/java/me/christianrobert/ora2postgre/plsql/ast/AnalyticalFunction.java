package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import java.util.List;

/**
 * AST class representing Oracle analytical functions like ROW_NUMBER(), RANK(), DENSE_RANK().
 * Handles function name, arguments, and OVER clause transformation to PostgreSQL.
 */
public class AnalyticalFunction extends PlSqlAst {
  
  public enum AnalyticalFunctionType {
    ROW_NUMBER,
    RANK,
    DENSE_RANK,
    FIRST_VALUE,
    LAST_VALUE,
    LAG,
    LEAD,
    COUNT,
    SUM,
    AVG,
    MIN,
    MAX,
    NTILE,
    PERCENT_RANK,
    CUME_DIST,
    NTH_VALUE
  }
  
  private AnalyticalFunctionType functionType;
  private List<Expression> arguments;
  private OverClause overClause;

  public AnalyticalFunction() {
    // Default constructor
  }

  public AnalyticalFunction(AnalyticalFunctionType functionType, List<Expression> arguments, OverClause overClause) {
    this.functionType = functionType;
    this.arguments = arguments;
    this.overClause = overClause;
  }

  public String toPostgre(Everything data) {
    StringBuilder result = new StringBuilder();
    
    // Add function name - most analytical functions have direct PostgreSQL equivalents
    result.append(getFunctionName());
    
    // Add arguments
    result.append("(");
    if (arguments != null && !arguments.isEmpty()) {
      for (int i = 0; i < arguments.size(); i++) {
        if (i > 0) result.append(", ");
        result.append(arguments.get(i).toPostgre(data));
      }
    }
    result.append(")");
    
    // Add OVER clause
    if (overClause != null) {
      result.append(" ").append(overClause.toPostgre(data));
    }
    
    return result.toString();
  }
  
  /**
   * Maps Oracle analytical function names to PostgreSQL equivalents.
   * Most have direct 1:1 mapping.
   */
  private String getFunctionName() {
    switch (functionType) {
      case ROW_NUMBER:
        return "ROW_NUMBER";
      case RANK:
        return "RANK";
      case DENSE_RANK:
        return "DENSE_RANK";
      case FIRST_VALUE:
        return "FIRST_VALUE";
      case LAST_VALUE:
        return "LAST_VALUE";
      case LAG:
        return "LAG";
      case LEAD:
        return "LEAD";
      case COUNT:
        return "COUNT";
      case SUM:
        return "SUM";
      case AVG:
        return "AVG";
      case MIN:
        return "MIN";
      case MAX:
        return "MAX";
      case NTILE:
        return "NTILE";
      case PERCENT_RANK:
        return "PERCENT_RANK";
      case CUME_DIST:
        return "CUME_DIST";
      case NTH_VALUE:
        return "NTH_VALUE";
      default:
        return functionType.toString();
    }
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  // Static factory methods for common analytical functions
  public static AnalyticalFunction rowNumber(OverClause overClause) {
    return new AnalyticalFunction(AnalyticalFunctionType.ROW_NUMBER, null, overClause);
  }

  public static AnalyticalFunction rank(OverClause overClause) {
    return new AnalyticalFunction(AnalyticalFunctionType.RANK, null, overClause);
  }

  public static AnalyticalFunction denseRank(OverClause overClause) {
    return new AnalyticalFunction(AnalyticalFunctionType.DENSE_RANK, null, overClause);
  }

  // Getters and setters
  public AnalyticalFunctionType getFunctionType() {
    return functionType;
  }

  public void setFunctionType(AnalyticalFunctionType functionType) {
    this.functionType = functionType;
  }

  public List<Expression> getArguments() {
    return arguments;
  }

  public void setArguments(List<Expression> arguments) {
    this.arguments = arguments;
  }

  public OverClause getOverClause() {
    return overClause;
  }

  public void setOverClause(OverClause overClause) {
    this.overClause = overClause;
  }

  @Override
  public String toString() {
    return "AnalyticalFunction{" +
        "functionType=" + functionType +
        ", arguments=" + arguments +
        ", overClause=" + overClause +
        '}';
  }
}