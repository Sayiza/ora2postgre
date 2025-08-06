package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.Function;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;
import me.christianrobert.ora2postgre.plsql.ast.Statement;
import me.christianrobert.ora2postgre.plsql.ast.tools.helpers.StatementDeclarationCollector;
import me.christianrobert.ora2postgre.plsql.ast.tools.helpers.ToExportPostgre;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.RecordTypeCollectionManager;
import me.christianrobert.ora2postgre.services.TransformationContext;
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
    
    // Add parameters with function context for collection type resolution
    ToExportPostgre.doParametersPostgre(b, function.getParameters(), context, function);
    
    // Add return type and function header with function context for collection type resolution
    b.append(") \n")
            .append("RETURNS ")
            .append(TypeConverter.toPostgre(function.getReturnType(), context, function))
            .append("\nLANGUAGE plpgsql AS $$\n")
            .append("DECLARE\n");

    if (!specOnly) {
      // FIRST: Collect record types for schema-level generation (MUST happen before variable processing)
      if (function.getRecordTypes() != null && !function.getRecordTypes().isEmpty()) {
        // Collect record types for schema-level composite type generation
        RecordTypeCollectionManager.collectFromFunction(function);
        
        for (me.christianrobert.ora2postgre.plsql.ast.RecordType recordType : function.getRecordTypes()) {
          String qualifiedName = RecordTypeCollectionManager.getQualifiedName(function, recordType);
          b.append("  -- Using schema-level composite type: ").append(qualifiedName).append("\n");
          b.append("  -- Original record type: ").append(recordType.getName()).append("\n");
        }
      }
      
      // SECOND: Add explicit variable declarations from function's DECLARE section
      if (function.getVariables() != null && !function.getVariables().isEmpty()) {
        for (me.christianrobert.ora2postgre.plsql.ast.Variable variable : function.getVariables()) {
          b.append("  ")
                  .append(variable.toPostgre(context, function))
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
      // Set function context for statement processing (needed for record field access)
      TransformationContext transformationContext = TransformationContext.getTestInstance();
      if (transformationContext == null) {
        // Create a new context if none exists (for integration tests and real application)
        transformationContext = new TransformationContext();
        TransformationContext.setTestInstance(transformationContext);
      }
      
      // Use function context during statement transformation
      transformationContext.withFunctionContext(function, () -> {
        // Add function body statements
        for (Statement statement : function.getStatements()) {
          b.append(statement.toPostgre(context))
                  .append("\n");
        }
      });
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
}