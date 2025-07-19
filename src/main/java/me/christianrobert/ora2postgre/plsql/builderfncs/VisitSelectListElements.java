package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.SelectListElement;

public class VisitSelectListElements {
  public static PlSqlAst visit(
          PlSqlParser.Select_list_elementsContext ctx,
          PlSqlAstBuilder astBuilder) {

    //String columnName = null;
    //String columnPrefix = null;
    String tableNameB4asterisk = null;
    String schemaNameB4asterisk = null;
    if (ctx.tableview_name() != null) {
      if (ctx.tableview_name().id_expression() != null) {
        tableNameB4asterisk = ctx.tableview_name().id_expression().getText();
        schemaNameB4asterisk = ctx.tableview_name().identifier().getText();
      }
    }

    // TODO expression is INSANELY complicated .. do this later

    // expression can either be a cursor expression or an arbitrary logical expression
    // infering the datatype from table is done in the toJava or toPostgre step!
    return new SelectListElement(
                        astBuilder.getSchema(),
            ctx.column_alias() != null ? ctx.column_alias().getText() : null,
            ctx.expression() != null ? (Expression) astBuilder.visit(ctx.expression()) : null,
            ctx.ASTERISK() != null,
            tableNameB4asterisk,
            schemaNameB4asterisk
    );
  }
}