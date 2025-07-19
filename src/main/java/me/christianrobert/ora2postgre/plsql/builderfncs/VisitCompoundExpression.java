package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.*;

public class VisitCompoundExpression {
  public static PlSqlAst visit(
          PlSqlParser.Compound_expressionContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    if (ctx.concatenation() != null && !ctx.concatenation().isEmpty()) {
      // Visit the first concatenation
      PlSqlAst concatenationAst = astBuilder.visit(ctx.concatenation(0));
      if (concatenationAst instanceof Concatenation) {
        return new CompoundExpression((Concatenation) concatenationAst);
      }
      // Fallback
      return astBuilder.visitChildren(ctx);
    }
    
    // For other compound expressions, fall back to default behavior
    return astBuilder.visitChildren(ctx);
  }
}