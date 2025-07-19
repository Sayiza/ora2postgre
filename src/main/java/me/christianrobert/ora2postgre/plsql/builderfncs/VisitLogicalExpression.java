package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.LogicalExpression;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.UnaryLogicalExpression;

public class VisitLogicalExpression {
  public static PlSqlAst visit(PlSqlParser.Logical_expressionContext ctx,
                               PlSqlAstBuilder astBuilder) {
    if (ctx.unary_logical_expression() != null && ctx.logical_expression().isEmpty()) {
      // Simple case: just unary logical expression
      UnaryLogicalExpression unaryExpr = (UnaryLogicalExpression) astBuilder.visit(ctx.unary_logical_expression());
      return new LogicalExpression(unaryExpr);
    }

    if (ctx.logical_expression().size() == 2) {
      // Binary case: logical_expression AND/OR logical_expression
      LogicalExpression left = (LogicalExpression) astBuilder.visit(ctx.logical_expression(0));
      LogicalExpression right = (LogicalExpression) astBuilder.visit(ctx.logical_expression(1));
      String operator = null;

      if (ctx.AND() != null) {
        operator = "AND";
      } else if (ctx.OR() != null) {
        operator = "OR";
      }

      return new LogicalExpression(left, operator, right);
    }

    // Fallback - convert raw text to logical expression
    LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression(ctx.getText()));
    return new Expression(logicalExpr);
  }
}
