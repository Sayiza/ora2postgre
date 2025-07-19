package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.*;

public class VisitOverClause {
  public static PlSqlAst visit(
          PlSqlParser.Over_clauseContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    java.util.List<Expression> partitionByColumns = null;
    java.util.List<OrderByElement> orderByElements = null;
    WindowingClause windowingClause = null;
    
    // Parse PARTITION BY clause
    if (ctx.query_partition_clause() != null) {
      partitionByColumns = parsePartitionByClause(ctx.query_partition_clause());
    }
    
    // Parse ORDER BY clause
    if (ctx.order_by_clause() != null) {
      orderByElements = parseOrderByClause(ctx.order_by_clause());
    }
    
    // Parse windowing clause
    if (ctx.windowing_clause() != null) {
      windowingClause = (WindowingClause) astBuilder.visit(ctx.windowing_clause());
    }
    
    return new OverClause(partitionByColumns, orderByElements, windowingClause);
  }

  /**
   * Parse PARTITION BY clause.
   */
  private static java.util.List<Expression> parsePartitionByClause(PlSqlParser.Query_partition_clauseContext ctx) {
    java.util.List<Expression> partitionByColumns = new java.util.ArrayList<>();
    
    // Create simple text-based expressions for now
    // A more complete implementation would parse the actual expressions
    if (ctx != null && ctx.getText() != null) {
      String text = ctx.getText();
      if (text.contains("PARTITION")) {
        // Simple text-based expression as placeholder
        LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression(text));
        Expression expr = new Expression(logicalExpr);
        partitionByColumns.add(expr);
      }
    }
    
    return partitionByColumns;
  }

  /**
   * Parse ORDER BY clause for OVER clause.
   */
  private static java.util.List<OrderByElement> parseOrderByClause(PlSqlParser.Order_by_clauseContext ctx) {
    java.util.List<OrderByElement> orderByElements = new java.util.ArrayList<>();
    
    // Create simple text-based order by elements for now
    // A more complete implementation would parse the actual order by elements
    if (ctx != null && ctx.getText() != null) {
      String text = ctx.getText();
      if (text.contains("ORDER")) {
        // Simple text-based expression as placeholder
        LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression(text));
        Expression expr = new Expression(logicalExpr);
        OrderByElement.SortDirection direction = text.contains("DESC") ? 
            OrderByElement.SortDirection.DESC : OrderByElement.SortDirection.ASC;
        OrderByElement orderByElement = new OrderByElement(expr, direction);
        orderByElements.add(orderByElement);
      }
    }
    
    return orderByElements;
  }
}