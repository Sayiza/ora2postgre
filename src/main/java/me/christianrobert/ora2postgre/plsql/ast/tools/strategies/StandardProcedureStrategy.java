package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.Procedure;
import me.christianrobert.ora2postgre.plsql.ast.Statement;
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
      
      // Add record type declarations from DECLARE section
      if (procedure.getRecordTypes() != null && !procedure.getRecordTypes().isEmpty()) {
        for (me.christianrobert.ora2postgre.plsql.ast.RecordType recordType : procedure.getRecordTypes()) {
          // Record types need to be created at the schema level, not inside procedures
          // For now, add a comment indicating the record type is needed
          b.append("  -- Record type ").append(recordType.getName())
                  .append(" should be created as composite type at schema level\n");
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
      // Add procedure body statements
      for (Statement statement : procedure.getStatements()) {
        b.append(statement.toPostgre(context))
                .append("\n");
      }
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
}