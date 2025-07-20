package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.LogicalExpression;
import me.christianrobert.ora2postgre.plsql.ast.MultisetExpression;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.UnaryLogicalExpression;

public class VisitUnaryLogicalExpression {
  public static PlSqlAst visit(
          PlSqlParser.Unary_logical_expressionContext ctx,
          PlSqlAstBuilder astBuilder) {
    boolean hasNot = ctx.NOT() != null;
    MultisetExpression multisetExpr = null;
    String logicalOperation = null;

    if (ctx.multiset_expression() != null) {
      // Visit the multiset_expression to get the proper MultisetExpression
      PlSqlAst multisetAst = astBuilder.visit(ctx.multiset_expression());
      if (multisetAst instanceof MultisetExpression) {
        multisetExpr = (MultisetExpression) multisetAst;
      } else {
        // Fallback: For now, create null MultisetExpression and preserve text in logicalOperation
        // This will be handled by the text-based constructor
        multisetExpr = null;
        if (logicalOperation == null) {
          logicalOperation = ctx.multiset_expression().getText();
        }
      }
    }

    if (ctx.unary_logical_operation() != null) {
      // Build the logical operation string (IS NULL, IS NOT NULL, etc.)
      StringBuilder opBuilder = new StringBuilder();
      var operation = ctx.unary_logical_operation();

      if (operation.IS() != null) {
        opBuilder.append("IS");
        if (operation.NOT() != null) {
          opBuilder.append(" NOT");
        }
        if (operation.logical_operation() != null) {
          if (operation.logical_operation().NULL_() != null) {
            opBuilder.append(" NULL");
          } else if (operation.logical_operation().NAN_() != null) {
            opBuilder.append(" NAN");
          } else if (operation.logical_operation().EMPTY_() != null) {
            opBuilder.append(" EMPTY");
          } else {
            // Handle other logical operations
            opBuilder.append(" ").append(operation.logical_operation().getText());
          }
        }
      }

      logicalOperation = opBuilder.toString();
    }

    return new UnaryLogicalExpression(hasNot, multisetExpr, logicalOperation);
  }
}
