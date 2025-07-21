package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.LogicalExpression;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.UnaryExpression;
import me.christianrobert.ora2postgre.plsql.ast.UnaryLogicalExpression;

import java.util.ArrayList;
import java.util.List;

public class VisitUnaryExpression {
  public static PlSqlAst visit(
          PlSqlParser.Unary_expressionContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    // Handle collection method calls: unary_expression '.' (COUNT | FIRST | LAST | etc.)
    if (ctx.unary_expression() != null) {
      // Check for collection methods without arguments
      if (ctx.COUNT() != null || ctx.FIRST() != null || ctx.LAST() != null || ctx.LIMIT() != null) {
        // Parse the base expression
        PlSqlAst baseExpressionAst = astBuilder.visit(ctx.unary_expression());
        if (baseExpressionAst instanceof UnaryExpression) {
          String methodName = null;
          if (ctx.COUNT() != null) methodName = "COUNT";
          else if (ctx.FIRST() != null) methodName = "FIRST";
          else if (ctx.LAST() != null) methodName = "LAST";
          else if (ctx.LIMIT() != null) methodName = "LIMIT";
          
          return new UnaryExpression((UnaryExpression) baseExpressionAst, methodName, null);
        }
      }
      
      // Check for collection methods with arguments: EXISTS, NEXT, PRIOR
      if (ctx.EXISTS() != null || ctx.NEXT() != null || ctx.PRIOR() != null) {
        PlSqlAst baseExpressionAst = astBuilder.visit(ctx.unary_expression());
        if (baseExpressionAst instanceof UnaryExpression) {
          String methodName = null;
          if (ctx.EXISTS() != null) methodName = "EXISTS";
          else if (ctx.NEXT() != null) methodName = "NEXT";
          else if (ctx.PRIOR() != null) methodName = "PRIOR";
          
          // Parse method arguments
          List<Expression> methodArguments = new ArrayList<>();
          if (ctx.index != null) {
            for (var exprCtx : ctx.index) {
              PlSqlAst argAst = astBuilder.visit(exprCtx);
              if (argAst instanceof Expression) {
                methodArguments.add((Expression) argAst);
              }
            }
          }
          
          return new UnaryExpression((UnaryExpression) baseExpressionAst, methodName, methodArguments);
        }
      }
    }
    
    if (ctx.standard_function() != null) {
      PlSqlAst standardFunctionAst = astBuilder.visit(ctx.standard_function());
      if (standardFunctionAst instanceof Expression) {
        return UnaryExpression.forStandardFunction((Expression) standardFunctionAst);
      }
      // Fallback
      return astBuilder.visitChildren(ctx);
    }
    
    if (ctx.atom() != null) {
      // Check if the atom contains a collection method call before visiting
      UnaryExpression collectionMethodCall = checkAtomForCollectionMethod(ctx.atom(), astBuilder);
      if (collectionMethodCall != null) {
        return collectionMethodCall;
      }
      
      PlSqlAst atomAst = astBuilder.visit(ctx.atom());
      if (atomAst instanceof Expression) {
        return UnaryExpression.forAtom((Expression) atomAst);
      }
      // Fallback
      return astBuilder.visitChildren(ctx);
    }
    
    // For other types of unary expressions, fall back to default behavior
    return astBuilder.visitChildren(ctx);
  }

  /**
   * Check if an atom contains a collection method call (e.g., v_arr.COUNT, v_arr.FIRST)
   * or array indexing (e.g., v_arr(i)).
   * This handles cases where these expressions are parsed through the general_element path
   * instead of the specific unary_expression dot notation rule.
   */
  private static UnaryExpression checkAtomForCollectionMethod(PlSqlParser.AtomContext atomCtx, PlSqlAstBuilder astBuilder) {
    // Check if atom contains a general_element with dot notation (collection methods)
    if (atomCtx.general_element() != null) {
      // Check for collection methods (e.g., array_var.COUNT, array_var.FIRST)
      UnaryExpression collectionMethod = checkGeneralElementForCollectionMethod(atomCtx.general_element(), astBuilder);
      if (collectionMethod != null) {
        return collectionMethod;
      }
      
      // Finally check for array indexing
      UnaryExpression arrayIndexing = checkGeneralElementForArrayIndexing(atomCtx.general_element(), astBuilder);
      if (arrayIndexing != null) {
        return arrayIndexing;
      }
    }
    return null;
  }

  /**
   * Check if a general_element represents a collection method call.
   * Looks for patterns like: variable.COUNT, variable.FIRST, variable.LAST, etc.
   */
  private static UnaryExpression checkGeneralElementForCollectionMethod(PlSqlParser.General_elementContext generalElementCtx, PlSqlAstBuilder astBuilder) {
    // Check for the pattern: general_element ('.' general_element_part)+
    if (generalElementCtx.general_element() != null && 
        generalElementCtx.general_element_part() != null && 
        !generalElementCtx.general_element_part().isEmpty()) {
      
      // Get the base expression (the variable part)
      PlSqlParser.General_elementContext baseElement = generalElementCtx.general_element();
      
      // Check each dot notation part for collection methods
      for (PlSqlParser.General_element_partContext partCtx : generalElementCtx.general_element_part()) {
        String methodName = extractCollectionMethodName(partCtx);
        if (methodName != null) {
          // Create a simple text-based expression for the base element
          String variableName = baseElement.getText();
          
          // Create a LogicalExpression that wraps this variable reference
          LogicalExpression logicalExpr = createLogicalExpressionFromText(variableName);
          Expression baseExpression = new Expression(logicalExpr);
          UnaryExpression baseUnaryExpression = UnaryExpression.forAtom(baseExpression);
          
          // Check if it's a method with arguments (like EXISTS, NEXT, PRIOR)
          List<Expression> methodArguments = extractMethodArguments(partCtx, astBuilder);
          
          return new UnaryExpression(baseUnaryExpression, methodName, methodArguments);
        }
      }
    }
    return null;
  }

  /**
   * Extract collection method name from a general_element_part if it's a collection method.
   * Returns null if it's not a recognized collection method.
   */
  private static String extractCollectionMethodName(PlSqlParser.General_element_partContext partCtx) {
    if (partCtx.id_expression() != null) {
      String methodName = partCtx.id_expression().getText().toUpperCase();
      switch (methodName) {
        case "COUNT":
        case "FIRST":
        case "LAST":
        case "LIMIT":
        case "EXISTS":
        case "NEXT":
        case "PRIOR":
          return methodName;
        default:
          return null;
      }
    }
    return null;
  }

  /**
   * Extract method arguments from a general_element_part if it has function_argument.
   * Used for methods like EXISTS(index), NEXT(index), PRIOR(index).
   */
  private static List<Expression> extractMethodArguments(PlSqlParser.General_element_partContext partCtx, PlSqlAstBuilder astBuilder) {
    List<Expression> arguments = new ArrayList<>();
    
    if (partCtx.function_argument() != null && !partCtx.function_argument().isEmpty()) {
      for (PlSqlParser.Function_argumentContext argCtx : partCtx.function_argument()) {
        if (argCtx.argument() != null && !argCtx.argument().isEmpty()) {
          for (PlSqlParser.ArgumentContext arg : argCtx.argument()) {
            if (arg.expression() != null) {
              PlSqlAst argAst = astBuilder.visit(arg.expression());
              if (argAst instanceof Expression) {
                arguments.add((Expression) argAst);
              }
            }
          }
        }
      }
    }
    
    return arguments.isEmpty() ? null : arguments;
  }

  /**
   * Create a simple LogicalExpression from text.
   * This is a helper method to wrap variable names in the Expression hierarchy.
   */
  private static LogicalExpression createLogicalExpressionFromText(String text) {
    // Create a UnaryLogicalExpression with the text, then wrap it in LogicalExpression
    UnaryLogicalExpression unaryLogicalExpr = new UnaryLogicalExpression(text);
    return new LogicalExpression(unaryLogicalExpr);
  }

  /**
   * Check if a general_element represents array indexing (e.g., v_arr(i)).
   * This uses the Everything metadata to distinguish between function calls and array indexing.
   */
  private static UnaryExpression checkGeneralElementForArrayIndexing(PlSqlParser.General_elementContext generalElementCtx, PlSqlAstBuilder astBuilder) {
    // Check for the pattern: general_element_part with function_argument (parentheses syntax)
    if (generalElementCtx.general_element_part() != null && 
        !generalElementCtx.general_element_part().isEmpty()) {
      
      // We need a simple identifier (not dot notation) with function arguments
      if (generalElementCtx.general_element() == null) {
        // This is a simple identifier with parentheses: identifier(args)
        PlSqlParser.General_element_partContext partCtx = generalElementCtx.general_element_part().get(0);
        
        if (partCtx.id_expression() != null && 
            partCtx.function_argument() != null && 
            !partCtx.function_argument().isEmpty()) {
          
          String identifier = partCtx.id_expression().getText();
          
          // Use Everything.isKnownFunction to determine if this is a function or variable
          // For now, we'll need access to Everything and current function context
          // This will be passed from the calling context
          
          // Extract ALL arguments for proper collection constructor handling
          List<Expression> arguments = extractMethodArguments(partCtx, astBuilder);
          if (arguments != null && !arguments.isEmpty()) {
            // For now, create collection constructor with ALL arguments
            // This will be enhanced with semantic detection when we have the full context
            
            // Create collection constructor with ALL arguments instead of just the first one
            return new UnaryExpression(identifier, arguments);
          } else {
            // Handle empty constructor: t_numbers() -> ARRAY[]
            return new UnaryExpression(identifier, new ArrayList<>());
          }
        }
      }
    }
    return null;
  }

}