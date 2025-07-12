package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.ViewMetadata;
import me.christianrobert.ora2postgre.plsql.ast.SelectStatement;

/**
 * Basic implementation of ViewTransformationStrategy that delegates to existing transformation logic.
 * This strategy handles all view types by forwarding to the current toPostgre() methods.
 * It serves as a backward compatibility layer during the transition to the strategy pattern.
 */
public class BasicViewStrategy implements ViewTransformationStrategy {

  @Override
  public boolean supports(ViewMetadata viewMetadata) {
    // This basic strategy supports all view metadata by delegating to existing logic
    return viewMetadata != null;
  }

  @Override
  public boolean supports(SelectStatement selectStatement) {
    // This basic strategy supports all select statements by delegating to existing logic
    return selectStatement != null;
  }

  @Override
  public String transformViewMetadata(ViewMetadata viewMetadata, boolean withDummyQuery, Everything context) {
    if (viewMetadata == null) {
      throw new UnsupportedOperationException("ViewMetadata cannot be null");
    }
    
    // Delegate to existing metadata transformation logic (legacy method to avoid circular dependency)
    return viewMetadata.toPostgreLegacy(withDummyQuery);
  }

  //@Override
  /*public String transformSelectStatement(SelectStatement selectStatement, Everything context, String schemaContext) {
    if (selectStatement == null) {
      throw new UnsupportedOperationException("SelectStatement cannot be null");
    }
    
    return selectStatement.toPostgre(context, schemaContext);
  }*/

  @Override
  public String getStrategyName() {
    return "Basic View Strategy";
  }

  @Override
  public int getPriority() {
    // Lower priority so more specific strategies can override this one
    return 10;
  }

  @Override
  public String getViewComplexity() {
    return "ALL"; // This strategy handles all view complexities
  }

  @Override
  public boolean requiresAstProcessing() {
    return true; // This strategy supports both metadata and AST processing
  }

  @Override
  public String getConversionNotes(ViewMetadata viewMetadata) {
    return "Converted using basic strategy - delegates to existing transformation logic";
  }

  @Override
  public String getConversionNotes(SelectStatement selectStatement) {
    return "Converted using basic strategy - delegates to existing transformation logic";
  }
}