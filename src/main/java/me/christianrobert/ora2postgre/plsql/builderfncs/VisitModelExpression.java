package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.*;

public class VisitModelExpression {
  public static PlSqlAst visit(
          PlSqlParser.Model_expressionContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    if (ctx.unary_expression() != null) {
      PlSqlAst unaryAst = astBuilder.visit(ctx.unary_expression());
      if (unaryAst instanceof UnaryExpression) {
        return new ModelExpression((UnaryExpression) unaryAst);
      }
      // Fallback - create a simple UnaryExpression from text
      UnaryExpression unaryExpr = UnaryExpression.forAtom(new Expression(new LogicalExpression(new UnaryLogicalExpression(ctx.unary_expression().getText()))));
      return new ModelExpression(unaryExpr);
    }
    
    // For other model expressions, fall back to default behavior
    return astBuilder.visitChildren(ctx);
  }
}