package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.*;

public class VisitRelationalExpression {
  public static PlSqlAst visit(
          PlSqlParser.Relational_expressionContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    if (ctx.compound_expression() != null) {
      PlSqlAst compoundAst = astBuilder.visit(ctx.compound_expression());
      if (compoundAst instanceof CompoundExpression) {
        return new RelationalExpression((CompoundExpression) compoundAst);
      }
      // Fallback
      return astBuilder.visitChildren(ctx);
    }
    
    // Handle relational operations: relational_expression relational_operator relational_expression
    if (ctx.relational_expression() != null && ctx.relational_expression().size() == 2 
        && ctx.relational_operator() != null) {
      PlSqlAst leftAst = astBuilder.visit(ctx.relational_expression(0));
      PlSqlAst rightAst = astBuilder.visit(ctx.relational_expression(1));
      String operator = ctx.relational_operator().getText();
      
      if (leftAst instanceof RelationalExpression && rightAst instanceof RelationalExpression) {
        return new RelationalExpression((RelationalExpression) leftAst, operator, (RelationalExpression) rightAst);
      }
    }
    
    // Fallback for other cases - should not normally happen, but use raw text
    throw new IllegalStateException("Unhandled relational expression case: " + ctx.getText());
  }
}