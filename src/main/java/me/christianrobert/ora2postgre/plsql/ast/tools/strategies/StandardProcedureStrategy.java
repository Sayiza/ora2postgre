package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.Procedure;
import me.christianrobert.ora2postgre.plsql.ast.Statement;
import me.christianrobert.ora2postgre.plsql.ast.tools.CursorLoopAnalyzer;
import me.christianrobert.ora2postgre.plsql.ast.tools.CursorLoopTransformer;
import me.christianrobert.ora2postgre.plsql.ast.tools.helpers.StatementDeclarationCollector;
import me.christianrobert.ora2postgre.plsql.ast.tools.helpers.ToExportPostgre;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standard implementation for transforming Oracle procedures to PostgreSQL.
 * This strategy handles most common Oracle procedure patterns and converts them
 * to PostgreSQL-compatible CREATE PROCEDURE statements.
 */
public class StandardProcedureStrategy implements ProcedureTransformationStrategy {

  private static final Logger log = LoggerFactory.getLogger(StandardProcedureStrategy.class);

  @Override
  public boolean supports(Procedure procedure) {
    // Standard strategy supports all procedures as a fallback
    return true;
  }

  @Override
  public String transform(Procedure procedure, Everything context, boolean specOnly) {
    log.debug("Transforming procedure {} using StandardProcedureStrategy (specOnly={})",
            procedure.getName(), specOnly);

    StringBuilder b = new StringBuilder();
    
    // Build CREATE PROCEDURE statement
    b.append("CREATE OR REPLACE PROCEDURE ");
    
    // Add schema and package prefix
    if (procedure.getParentType() != null) {
      b.append(procedure.getParentType().getSchema().toUpperCase())
              .append(".")
              .append(procedure.getParentType().getName().toUpperCase());
    } else if (procedure.getParentPackage() != null) {
      b.append(procedure.getParentPackage().getSchema().toUpperCase())
              .append(".")
              .append(procedure.getParentPackage().getName().toUpperCase());
    } else {
      log.warn("Procedure {} has no parent package or type", procedure.getName());
    }
    
    b.append("_")
            .append(procedure.getName().toLowerCase())
            .append("(");
    
    // Add parameters
    ToExportPostgre.doParametersPostgre(b, procedure.getParameters(), context);
    
    // Add procedure header  
    b.append(") LANGUAGE plpgsql AS $$\n")
            .append("DECLARE\n");

    if (!specOnly) {
      // Add explicit variable declarations from procedure's DECLARE section
      if (procedure.getVariables() != null && !procedure.getVariables().isEmpty()) {
        for (me.christianrobert.ora2postgre.plsql.ast.Variable variable : procedure.getVariables()) {
          b.append("  ")
                  .append(variable.toPostgre(context))
                  .append(";")
                  .append("\n");
        }
      }
      
      // Add cursor declarations from DECLARE section
      if (procedure.getCursorDeclarations() != null && !procedure.getCursorDeclarations().isEmpty()) {
        for (me.christianrobert.ora2postgre.plsql.ast.CursorDeclaration cursor : procedure.getCursorDeclarations()) {
          b.append("  ")
                  .append(cursor.toPostgre(context))
                  .append("\n");
        }
      }
      
      // Collect and add variable declarations from FOR loops and other nested statements
      StringBuilder declarations = StatementDeclarationCollector.collectNecessaryDeclarations(
              procedure.getStatements(), context);
      b.append(declarations);
    }

    b.append("BEGIN\n");
    
    if (specOnly) {
      b.append("null;\n");
    } else {
      // Add procedure body statements with cursor loop transformation
      processStatementsWithCursorTransformation(procedure.getStatements(), b, context);
    }
    
    // Add exception handling if present
    if (procedure.hasExceptionHandling()) {
      b.append(procedure.getExceptionBlock().toPostgre(context));
    }
    
    b.append("END;\n$$\n;\n");
    
    log.debug("Successfully transformed procedure {} with {} statements",
            procedure.getName(), procedure.getStatements().size());

    return b.toString();
  }

  @Override
  public String getStrategyName() {
    return "Standard Procedure";
  }

  @Override
  public int getPriority() {
    // Standard strategy has lowest priority as it's the fallback
    return 0;
  }

  @Override
  public String getConversionNotes(Procedure procedure) {
    StringBuilder notes = new StringBuilder("Converted using standard procedure transformation");
    
    if (procedure.getParentType() == null && procedure.getParentPackage() == null) {
      notes.append("; Warning: Procedure has no parent package or type");
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