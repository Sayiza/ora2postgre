package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.CallStatement;
import me.christianrobert.ora2postgre.plsql.ast.Comment;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.HtpStatement;
import me.christianrobert.ora2postgre.plsql.ast.LogicalExpression;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.UnaryLogicalExpression;

import java.util.ArrayList;
import java.util.List;

public class VisitCallStatement {
  public static PlSqlAst visit(PlSqlParser.Call_statementContext ctx,
                                      PlSqlAstBuilder astBuilder) {
    // Parse the primary routine name
    if (ctx.routine_name() == null || ctx.routine_name().isEmpty()) {
      return new Comment("/* Empty call statement */");
    }

    String routineNameText = ctx.routine_name(0).getText();

    // Handle HTP calls specifically - now parse arguments through expression hierarchy
    if (routineNameText.contains("htp.p")) {
      if (ctx.function_argument(0) != null) {
        // Parse the HTP argument through the expression hierarchy to enable package variable transformation
        List<Expression> htpArgs = parseCallArguments(ctx.function_argument(0), astBuilder);
        if (!htpArgs.isEmpty()) {
          return new HtpStatement(htpArgs.get(0));
        }
      }
      // Empty HTP call - create expression with empty string
      return new HtpStatement(new Expression(new LogicalExpression(new UnaryLogicalExpression("''"))));
    }

    // Parse routine name components
    String packageName = null;
    String routineName = null;

    if (routineNameText.contains(".")) {
      // Package.routine format
      String[] parts = routineNameText.split("\\.", 2);
      packageName = parts[0];
      routineName = parts[1];
    } else {
      // Simple routine name
      routineName = routineNameText;
    }

    // Parse arguments if present
    List<Expression> arguments = new ArrayList<>();
    if (ctx.function_argument(0) != null) {
      arguments = parseCallArguments(ctx.function_argument(0), astBuilder);
    }

    // Check if this is a function call with return target (INTO clause)
    Expression returnTarget = null;
    boolean isFunction = false;
    if (ctx.bind_variable() != null) {
      // Function call with INTO clause
      isFunction = true;
      String targetVar = ctx.bind_variable().getText();
      // Create a simple expression for the target variable
      UnaryLogicalExpression targetExpr = new UnaryLogicalExpression(targetVar);
      returnTarget = new Expression(new LogicalExpression(targetExpr));
    }

    // Handle chained calls (routine_name ('.' routine_name function_argument?)*)
    if (ctx.routine_name().size() > 1) {
      // For now, handle simple case of package.routine
      // TODO: Implement full chained call support
      return new Comment("/* Chained call not yet implemented: " + routineNameText + " */");
    }

    // Create appropriate CallStatement
    CallStatement callStatement;
    if (isFunction && returnTarget != null) {
      callStatement = new CallStatement(routineName, packageName, arguments, returnTarget);
    } else {
      // Determine if this is a function or procedure (will be resolved later)
      callStatement = new CallStatement(routineName, packageName, arguments, false);
    }

    // Set calling context for package-less call resolution
    callStatement.setCallingPackage(astBuilder.getCurrentPackageName());
    callStatement.setCallingSchema(astBuilder.getSchema());

    return callStatement;
  }


  /**
   * Parse function arguments from ANTLR context.
   *
   * @param funcArgCtx The function_argument context
   * @return List of parsed Expression objects
   */
  private static List<Expression> parseCallArguments(
          PlSqlParser.Function_argumentContext funcArgCtx,
          PlSqlAstBuilder astBuilder) {
    List<Expression> arguments = new ArrayList<>();

    if (funcArgCtx.argument() != null) {
      for (PlSqlParser.ArgumentContext argCtx : funcArgCtx.argument()) {
        Expression expr = (Expression) astBuilder.visit(argCtx.expression());
        if (expr != null) {
          arguments.add(expr);
        }
      }
    }

    return arguments;
  }
}
