package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.TableExpressionClause;

public class VisitDmlTableExpressionClause {
  public static PlSqlAst visit(
          PlSqlParser.Dml_table_expression_clauseContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    String schemaName = null;
    String tableName = "?";
    if (ctx.tableview_name() != null) {
      if (ctx.tableview_name().id_expression() != null) {
        schemaName = ctx.tableview_name().identifier().getText();
        tableName = ctx.tableview_name().id_expression().getText();
      } else {
        tableName = ctx.tableview_name().identifier().getText();
      }
    }

    return new TableExpressionClause(
            schemaName,
            tableName
    );
  }
}