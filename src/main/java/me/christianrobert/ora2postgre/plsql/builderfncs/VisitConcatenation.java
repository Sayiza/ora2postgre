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
    
    // Handle binary operations (arithmetic/concatenation operations)
    if (ctx.concatenation() != null && ctx.concatenation().size() == 2 && ctx.op != null) {
      // Parse left and right operands recursively
      PlSqlAst leftAst = astBuilder.visit(ctx.concatenation(0));
      PlSqlAst rightAst = astBuilder.visit(ctx.concatenation(1));
      
      if (leftAst instanceof Concatenation && rightAst instanceof Concatenation) {
        String operator = ctx.op.getText();
        return new Concatenation((Concatenation) leftAst, operator, (Concatenation) rightAst);
      }
    }
    
    // For other concatenation patterns, fall back to default behavior
    return new Concatenation(new ModelExpression(UnaryExpression.forAtom(new Expression(new LogicalExpression(new UnaryLogicalExpression(ctx.getText()))))));
  }
}