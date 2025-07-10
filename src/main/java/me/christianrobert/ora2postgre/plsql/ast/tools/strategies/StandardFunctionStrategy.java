package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.Function;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;
import me.christianrobert.ora2postgre.plsql.ast.Statement;
import me.christianrobert.ora2postgre.plsql.ast.tools.StatementDeclarationCollector;
import me.christianrobert.ora2postgre.plsql.ast.tools.ToExportPostgre;
import me.christianrobert.ora2postgre.plsql.ast.tools.TypeConverter;
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
    
    // Collect and add variable declarations from FOR loops
    StringBuilder stmtDeclarations = StatementDeclarationCollector.collectNecessaryDeclarations(
            function.getStatements(), context);
    b.append(stmtDeclarations);
    
    // TODO: Add declarations from plsql source code
    // This is currently a TODO in the original Function.toPostgre() method
    
    b.append("BEGIN\n");
    
    if (specOnly) {
      b.append("return null;\n");
    } else {
      // Add function body statements
      for (Statement statement : function.getStatements()) {
        b.append(statement.toPostgre(context))
                .append("\n");
      }
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
}