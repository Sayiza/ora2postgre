package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.AnalyticalFunction;
import me.christianrobert.ora2postgre.plsql.ast.CursorAttributeExpression;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.LogicalExpression;
import me.christianrobert.ora2postgre.plsql.ast.OverClause;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.UnaryLogicalExpression;

import java.util.ArrayList;
import java.util.List;

public class VisitOtherFunction {
  public static PlSqlAst visit(
          PlSqlParser.Other_functionContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    // Check if this is a cursor attribute (cursor_name %FOUND, %NOTFOUND, etc.)
    if (ctx.cursor_name() != null) {
      String cursorName = ctx.cursor_name().getText();
      
      CursorAttributeExpression.CursorAttributeType attributeType = null;
      
      if (ctx.PERCENT_FOUND() != null) {
        attributeType = CursorAttributeExpression.CursorAttributeType.FOUND;
      } else if (ctx.PERCENT_NOTFOUND() != null) {
        attributeType = CursorAttributeExpression.CursorAttributeType.NOTFOUND;
      } else if (ctx.PERCENT_ROWCOUNT() != null) {
        attributeType = CursorAttributeExpression.CursorAttributeType.ROWCOUNT;
      } else if (ctx.PERCENT_ISOPEN() != null) {
        attributeType = CursorAttributeExpression.CursorAttributeType.ISOPEN;
      }
      
      if (attributeType != null) {
        // Create a CursorAttributeExpression and wrap it in UnaryExpression
        CursorAttributeExpression cursorAttr = new CursorAttributeExpression(cursorName, attributeType);
        
        // Create an Expression that delegates to our cursor attribute
        LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression("CURSOR_ATTR_PLACEHOLDER"));
        return new Expression(logicalExpr) {
          @Override
          public String toPostgre(me.christianrobert.ora2postgre.global.Everything data) {
            return cursorAttr.toPostgre(data);
          }
          
          @Override
          public String toString() {
            return cursorAttr.toString();
          }
        };
      }
    }
    
    // Check for analytical functions: over_clause_keyword function_argument_analytic over_clause?
    if (ctx.over_clause_keyword() != null && ctx.over_clause() != null) {
      return parseAnalyticalFunction(ctx, astBuilder);
    }
    
    // Check for within_or_over functions: within_or_over_clause_keyword function_argument within_or_over_part+
    if (ctx.within_or_over_clause_keyword() != null && !ctx.within_or_over_part().isEmpty()) {
      // Check if any of the within_or_over_part elements contain an over_clause
      for (PlSqlParser.Within_or_over_partContext partCtx : ctx.within_or_over_part()) {
        if (partCtx.over_clause() != null) {
          return parseAnalyticalFunctionFromWithinOrOver(ctx, astBuilder);
        }
      }
    }
    
    // For other function types, fall back to default behavior
    return astBuilder.visitChildren(ctx);
  }

  /**
   * Parse analytical function from over_clause_keyword function_argument_analytic over_clause pattern.
   */
  private static AnalyticalFunction parseAnalyticalFunction(PlSqlParser.Other_functionContext ctx, PlSqlAstBuilder astBuilder) {
    // Determine function type from over_clause_keyword
    AnalyticalFunction.AnalyticalFunctionType functionType = 
        parseAnalyticalFunctionType(ctx.over_clause_keyword().getText());
    
    // Parse arguments (if any)
    List<Expression> arguments = new ArrayList<>();
    if (ctx.function_argument_analytic() != null) {
      arguments = parseAnalyticalFunctionArguments(ctx.function_argument_analytic());
    }
    
    // Parse OVER clause
    OverClause overClause = null;
    if (ctx.over_clause() != null) {
      overClause = (OverClause) astBuilder.visit(ctx.over_clause());
    }
    
    return new AnalyticalFunction(functionType, arguments, overClause);
  }

  /**
   * Parse analytical function from within_or_over_clause_keyword pattern.
   */
  private static AnalyticalFunction parseAnalyticalFunctionFromWithinOrOver(PlSqlParser.Other_functionContext ctx, PlSqlAstBuilder astBuilder) {
    // Determine function type from within_or_over_clause_keyword
    AnalyticalFunction.AnalyticalFunctionType functionType = 
        parseAnalyticalFunctionTypeFromWithinOrOver(ctx.within_or_over_clause_keyword().getText());
    
    // Parse arguments
    List<Expression> arguments = new ArrayList<>();
    if (ctx.function_argument() != null) {
      // Parse standard function arguments
      arguments = parseStandardFunctionArguments(ctx.function_argument());
    }
    
    // Find the OVER clause from within_or_over_part
    OverClause overClause = null;
    for (PlSqlParser.Within_or_over_partContext partCtx : ctx.within_or_over_part()) {
      if (partCtx.over_clause() != null) {
        overClause = (OverClause) astBuilder.visit(partCtx.over_clause());
        break;
      }
    }
    
    return new AnalyticalFunction(functionType, arguments, overClause);
  }

  /**
   * Map over_clause_keyword to AnalyticalFunctionType.
   */
  private static AnalyticalFunction.AnalyticalFunctionType parseAnalyticalFunctionType(String keyword) {
    switch (keyword.toUpperCase()) {
      case "ROW_NUMBER":
        return AnalyticalFunction.AnalyticalFunctionType.ROW_NUMBER;
      case "AVG":
        return AnalyticalFunction.AnalyticalFunctionType.AVG;
      case "MAX":
        return AnalyticalFunction.AnalyticalFunctionType.MAX;
      case "MIN":
        return AnalyticalFunction.AnalyticalFunctionType.MIN;
      case "SUM":
        return AnalyticalFunction.AnalyticalFunctionType.SUM;
      case "COUNT":
        return AnalyticalFunction.AnalyticalFunctionType.COUNT;
      case "NTILE":
        return AnalyticalFunction.AnalyticalFunctionType.NTILE;
      default:
        // For unknown functions, default to a safe option or throw exception
        return AnalyticalFunction.AnalyticalFunctionType.ROW_NUMBER;
    }
  }

  /**
   * Map within_or_over_clause_keyword to AnalyticalFunctionType.
   */
  private static AnalyticalFunction.AnalyticalFunctionType parseAnalyticalFunctionTypeFromWithinOrOver(String keyword) {
    switch (keyword.toUpperCase()) {
      case "RANK":
        return AnalyticalFunction.AnalyticalFunctionType.RANK;
      case "DENSE_RANK":
        return AnalyticalFunction.AnalyticalFunctionType.DENSE_RANK;
      case "PERCENT_RANK":
        return AnalyticalFunction.AnalyticalFunctionType.PERCENT_RANK;
      case "CUME_DIST":
        return AnalyticalFunction.AnalyticalFunctionType.CUME_DIST;
      default:
        return AnalyticalFunction.AnalyticalFunctionType.RANK;
    }
  }

  /**
   * Parse analytical function arguments.
   */
  private static List<Expression> parseAnalyticalFunctionArguments(PlSqlParser.Function_argument_analyticContext ctx) {
    List<Expression> arguments = new ArrayList<>();
    
    // For most analytical functions, arguments are optional or handled differently
    // This is a placeholder for future argument parsing if needed
    
    return arguments;
  }

  /**
   * Parse standard function arguments.
   */
  private static List<Expression> parseStandardFunctionArguments(PlSqlParser.Function_argumentContext ctx) {
    List<Expression> arguments = new ArrayList<>();
    
    // This is a placeholder for standard function argument parsing
    // The exact implementation depends on the function_argument grammar structure
    
    return arguments;
  }
}