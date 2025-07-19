package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.CommonTableExpression;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.SelectSubQuery;

import java.util.ArrayList;
import java.util.List;

public class VisitSubqueryFactoringClause {
  public static PlSqlAst visit(
          PlSqlParser.Subquery_factoring_clauseContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    // Extract query name
    String queryName = ctx.query_name().getText();
    
    // Extract optional column list
    List<String> columnList = null;
    if (ctx.paren_column_list() != null) {
      columnList = new ArrayList<>();
      for (PlSqlParser.Column_nameContext colCtx : ctx.paren_column_list().column_list().column_name()) {
        columnList.add(colCtx.getText());
      }
    }
    
    // Extract subquery
    SelectSubQuery subQuery = null;
    if (ctx.subquery() != null) {
      subQuery = (SelectSubQuery) astBuilder.visit(ctx.subquery());
    }
    
    // Check for recursive CTE (Oracle uses SEARCH and CYCLE clauses to indicate recursion)
    boolean recursive = (ctx.search_clause() != null || ctx.cycle_clause() != null);
    
    // TODO: Handle search_clause and cycle_clause transformation
    // For now, we'll just detect their presence to mark as recursive
    
    return new CommonTableExpression(queryName, columnList, subQuery, recursive);
  }
}