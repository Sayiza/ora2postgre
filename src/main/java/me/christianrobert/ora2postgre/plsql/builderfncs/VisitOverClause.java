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
      partitionByColumns = astBuilder.parsePartitionByClause(ctx.query_partition_clause());
    }
    
    // Parse ORDER BY clause
    if (ctx.order_by_clause() != null) {
      orderByElements = astBuilder.parseOrderByClause(ctx.order_by_clause());
    }
    
    // Parse windowing clause
    if (ctx.windowing_clause() != null) {
      windowingClause = (WindowingClause) astBuilder.visit(ctx.windowing_clause());
    }
    
    return new OverClause(partitionByColumns, orderByElements, windowingClause);
  }
}