package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.*;

import java.util.ArrayList;
import java.util.List;

public class VisitOpenStatement {
  public static PlSqlAst visit(
          PlSqlParser.Open_statementContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    String cursorName = ctx.cursor_name().getText();
    
    // Parse parameters if present
    List<Expression> parameters = new ArrayList<>();
    if (ctx.expressions_() != null && ctx.expressions_().expression() != null) {
      for (PlSqlParser.ExpressionContext exprCtx : ctx.expressions_().expression()) {
        Expression expr = (Expression) astBuilder.visit(exprCtx);
        if (expr != null) {
          parameters.add(expr);
        }
      }
    }
    
    return new OpenStatement(cursorName, parameters);
  }
}