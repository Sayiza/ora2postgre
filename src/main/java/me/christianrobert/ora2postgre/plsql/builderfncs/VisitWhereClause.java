package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.*;

public class VisitWhereClause {
  public static PlSqlAst visit(
          PlSqlParser.Where_clauseContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    if (ctx.cursor_name() != null) {
      // CURRENT OF cursor_name variant
      String cursorName = ctx.cursor_name().getText();
      return new WhereClause(cursorName);
    } else if (ctx.condition() != null) {
      // condition variant
      Expression condition = (Expression) astBuilder.visit(ctx.condition());
      return new WhereClause(condition);
    }
    return null;
  }
}