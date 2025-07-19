package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.*;

public class VisitMultisetExpression {
  public static PlSqlAst visit(
          PlSqlParser.Multiset_expressionContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    if (ctx.relational_expression() != null) {
      // Visit the relational expression
      PlSqlAst relationalAst = astBuilder.visit(ctx.relational_expression());
      if (relationalAst instanceof RelationalExpression) {
        return new MultisetExpression((RelationalExpression) relationalAst);
      }
      // Fallback
      return astBuilder.visitChildren(ctx);
    }
    
    // For other types of multiset expressions, fall back to default behavior
    return astBuilder.visitChildren(ctx);
  }
}