package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.Function;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;
import me.christianrobert.ora2postgre.plsql.ast.Statement;
import me.christianrobert.ora2postgre.plsql.ast.tools.CursorLoopAnalyzer;
import me.christianrobert.ora2postgre.plsql.ast.tools.CursorLoopTransformer;
import me.christianrobert.ora2postgre.plsql.ast.tools.helpers.StatementDeclarationCollector;
import me.christianrobert.ora2postgre.plsql.ast.tools.helpers.ToExportPostgre;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standard implementation for transforming Oracle functions to PostgreSQL.
 * This strategy handles most common Oracle function patterns and converts them
 * to PostgreSQL-compatible CREATE FUNCTION statements.
 */
public class StandardFunctionStrategy implements FunctionTransformationStrategy {

  private static final Logger log = LoggerFactory.getLogger(StandardFunctionStrategy.class);

  @Override
  public boolean supports(Function function) {
    // Standard strategy supports all functions as a fallback
    return true;
  }

  @Override
  public String transform(Function function, Everything context, boolean specOnly) {
    log.debug("Transforming function {} using StandardFunctionStrategy (specOnly={})",
            function.getName(), specOnly);

    StringBuilder b = new StringBuilder();
    
    // Build CREATE FUNCTION statement
    b.append("CREATE OR REPLACE FUNCTION ");
    
    // Add schema and package prefix
    if (function.getParentType() != null) {
      b.append(function.getParentType().getSchema().toUpperCase())
              .append(".")
              .append(function.getParentType().getName().toUpperCase());
    } else if (function.getParentPackage() != null) {
      b.append(function.getParentPackage().getSchema().toUpperCase())
              .append(".")
              .append(function.getParentPackage().getName().toUpperCase());
    } else {
      log.warn("Function {} has no parent package or type", function.getName());
    }
    
    b.append("_")
            .append(function.getName().toLowerCase())
            .append("(");
    
    // Add parameters
    ToExportPostgre.doParametersPostgre(b, function.getParameters(), context);
    
    // Add return type and function header
    b.append(") \n")
            .append("RETURNS ")
            .append(TypeConverter.toPostgre(function.getReturnType()))
            .append("\nLANGUAGE plpgsql AS $$\n")
            .append("DECLARE\n");

    if (!specOnly) {
      // Add explicit variable declarations from procedure's DECLARE section
      if (function.getVariables() != null && !function.getVariables().isEmpty()) {
        for (me.christianrobert.ora2postgre.plsql.ast.Variable variable : function.getVariables()) {
          b.append("  ")
                  .append(variable.toPostgre(context))
                  .append(";")
                  .append("\n");
        }
      }
      
      // Add cursor declarations from DECLARE section
      if (function.getCursorDeclarations() != null && !function.getCursorDeclarations().isEmpty()) {
        for (me.christianrobert.ora2postgre.plsql.ast.CursorDeclaration cursor : function.getCursorDeclarations()) {
          b.append("  ")
                  .append(cursor.toPostgre(context))
                  .append("\n");
        }
      }
      
      // Collect and add variable declarations from FOR loops
      StringBuilder stmtDeclarations = StatementDeclarationCollector.collectNecessaryDeclarations(
              function.getStatements(), context);
      b.append(stmtDeclarations);
      // TODO: Add declarations from plsql source code
    }

    b.append("BEGIN\n");
    
    if (specOnly) {
      b.append("return null;\n");
    } else {
      // Add function body statements with cursor loop transformation
      processStatementsWithCursorTransformation(function.getStatements(), b, context);
    }
    
    // Add exception handling if present
    if (function.hasExceptionHandling()) {
      b.append(function.getExceptionBlock().toPostgre(context));
    }
    
    b.append("END;\n$$\n;\n");
    
    log.debug("Successfully transformed function {} with {} statements",
            function.getName(), function.getStatements().size());

    return b.toString();
  }

  @Override
  public String getStrategyName() {
    return "Standard Function";
  }

  @Override
  public int getPriority() {
    // Standard strategy has lowest priority as it's the fallback
    return 0;
  }

  @Override
  public String getConversionNotes(Function function) {
    StringBuilder notes = new StringBuilder("Converted using standard function transformation");
    
    if (function.getParentType() == null && function.getParentPackage() == null) {
      notes.append("; Warning: Function has no parent package or type");
    }
    
    return notes.toString();
  }

  /**
   * Processes a list of statements, detecting and transforming cursor loop patterns.
   * This method scans for OPEN/LOOP/CLOSE patterns and converts them to PostgreSQL FOR loops.
   */
  private void processStatementsWithCursorTransformation(java.util.List<Statement> statements, StringBuilder b, Everything context) {
    java.util.Set<Integer> consumedIndices = new java.util.HashSet<>();
    
    for (int i = 0; i < statements.size(); i++) {
      // Skip if this statement was already consumed as part of a cursor pattern
      if (consumedIndices.contains(i)) {
        continue;
      }
      
      // Try to detect cursor loop pattern starting from current position
      java.util.List<Statement> remainingStatements = statements.subList(i, statements.size());
      CursorLoopAnalyzer.CursorLoopInfo cursorInfo = CursorLoopAnalyzer.detectCursorLoopPattern(remainingStatements);
      
      if (cursorInfo != null && CursorLoopTransformer.shouldTransformToForLoop(cursorInfo)) {
        // Transform cursor pattern to FOR loop
        CursorLoopTransformer.CursorForLoopStatement forLoop = CursorLoopTransformer.transformToCursorForLoop(cursorInfo);
        b.append(forLoop.toPostgre(context)).append("\n");
        
        // Mark all statements in the pattern as consumed
        for (int j = 0; j < cursorInfo.getStatementCount(); j++) {
          consumedIndices.add(i + j);
        }
        
        log.debug("Transformed cursor loop pattern '{}' to FOR loop (consumed {} statements)", 
                 cursorInfo.getCursorName(), cursorInfo.getStatementCount());
      } else {
        // Regular statement processing
        Statement statement = statements.get(i);
        b.append(statement.toPostgre(context)).append("\n");
      }
    }
  }
}