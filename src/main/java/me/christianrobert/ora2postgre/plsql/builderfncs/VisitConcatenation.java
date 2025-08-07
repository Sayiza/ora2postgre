package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.*;

public class VisitConcatenation {
  public static PlSqlAst visit(
          PlSqlParser.ConcatenationContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    // Handle simple model expression (most common case)
    if (ctx.model_expression() != null) {
      PlSqlAst modelAst = astBuilder.visit(ctx.model_expression());
      if (modelAst instanceof ModelExpression) {
        return new Concatenation((ModelExpression) modelAst);
      }
      // Fallback
      return astBuilder.visitChildren(ctx);
    }
    
    // Handle BAR BAR concatenation: concatenation BAR BAR concatenation
    if (ctx.BAR() != null && ctx.BAR().size() == 2 && ctx.concatenation() != null && ctx.concatenation().size() == 2) {
      // Parse left and right operands recursively
      PlSqlAst leftAst = astBuilder.visit(ctx.concatenation(0));
      PlSqlAst rightAst = astBuilder.visit(ctx.concatenation(1));
      
      if (leftAst instanceof Concatenation && rightAst instanceof Concatenation) {
        return new Concatenation((Concatenation) leftAst, "||", (Concatenation) rightAst);
      }
    }
    
    // Handle arithmetic operations with op field: +, -, *, /, **, MOD
    if (ctx.concatenation() != null && ctx.concatenation().size() == 2 && ctx.op != null) {
      // Parse left and right operands recursively
      PlSqlAst leftAst = astBuilder.visit(ctx.concatenation(0));
      PlSqlAst rightAst = astBuilder.visit(ctx.concatenation(1));
      
      if (leftAst instanceof Concatenation && rightAst instanceof Concatenation) {
        String operator = ctx.op.getText();
        return new Concatenation((Concatenation) leftAst, operator, (Concatenation) rightAst);
      }
    }
    
    // Use proper default visitor behavior for other cases
    return astBuilder.visitChildren(ctx);
  }
}