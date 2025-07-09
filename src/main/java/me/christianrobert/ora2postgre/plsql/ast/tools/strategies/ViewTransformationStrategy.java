package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.ViewMetadata;
import me.christianrobert.ora2postgre.plsql.ast.SelectStatement;

/**
 * Strategy interface for converting Oracle views to PostgreSQL equivalents.
 * This interface supports both metadata-based and AST-based view transformations,
 * including the two-phase view export pattern (empty views first, then full views).
 */
public interface ViewTransformationStrategy {

  /**
   * Determines if this strategy can handle the given Oracle view metadata.
   *
   * @param viewMetadata The Oracle view metadata to evaluate
   * @return true if this strategy can convert the view metadata, false otherwise
   */
  boolean supports(ViewMetadata viewMetadata);

  /**
   * Determines if this strategy can handle the given Oracle view AST.
   *
   * @param selectStatement The Oracle select statement AST to evaluate
   * @return true if this strategy can convert the select statement, false otherwise
   */
  boolean supports(SelectStatement selectStatement);

  /**
   * Transforms Oracle view metadata to PostgreSQL CREATE VIEW statement.
   * This method should only be called if supports() returns true.
   *
   * @param viewMetadata The Oracle view metadata to convert
   * @param withDummyQuery Whether to include dummy NULL query for empty views
   * @param context The global context containing all migration data
   * @return PostgreSQL CREATE VIEW statement
   * @throws UnsupportedOperationException if the view is not supported by this strategy
   */
  String transformViewMetadata(ViewMetadata viewMetadata, boolean withDummyQuery, Everything context);

  /**
   * Transforms Oracle select statement AST to PostgreSQL SELECT statement.
   * This method should only be called if supports() returns true.
   *
   * @param selectStatement The Oracle select statement AST to convert
   * @param context The global context containing all migration data
   * @return PostgreSQL SELECT statement
   * @throws UnsupportedOperationException if the select statement is not supported by this strategy
   */
  String transformSelectStatement(SelectStatement selectStatement, Everything context);

  /**
   * Transforms Oracle select statement AST to PostgreSQL SELECT statement with schema context.
   * This method should only be called if supports() returns true.
   *
   * @param selectStatement The Oracle select statement AST to convert
   * @param context The global context containing all migration data
   * @param schemaContext The schema context for transformation
   * @return PostgreSQL SELECT statement
   * @throws UnsupportedOperationException if the select statement is not supported by this strategy
   */
  String transformSelectStatement(SelectStatement selectStatement, Everything context, String schemaContext);

  /**
   * Gets a human-readable name for this strategy.
   * Used for logging and debugging purposes.
   *
   * @return Strategy name (e.g., "Simple View", "Complex View", "Materialized View")
   */
  String getStrategyName();

  /**
   * Gets the priority of this strategy for selection.
   * Higher priority strategies are checked first.
   * This allows more specific strategies to take precedence over general ones.
   *
   * @return Priority value (higher = checked first)
   */
  default int getPriority() {
    return 0;
  }

  /**
   * Gets the view complexity level this strategy handles.
   *
   * @return View complexity level (e.g., "SIMPLE", "COMPLEX", "MATERIALIZED")
   */
  String getViewComplexity();

  /**
   * Determines if this strategy requires full AST processing.
   * Simple views might only need metadata transformation.
   *
   * @return true if this strategy requires AST processing
   */
  default boolean requiresAstProcessing() {
    return true;
  }

  /**
   * Gets additional information about the conversion process.
   * Can include warnings, notes, or recommendations for the converted view.
   *
   * @param viewMetadata The Oracle view metadata being converted
   * @return Additional conversion information, or null if none
   */
  default String getConversionNotes(ViewMetadata viewMetadata) {
    return null;
  }

  /**
   * Gets additional information about the conversion process.
   * Can include warnings, notes, or recommendations for the converted select statement.
   *
   * @param selectStatement The Oracle select statement being converted
   * @return Additional conversion information, or null if none
   */
  default String getConversionNotes(SelectStatement selectStatement) {
    return null;
  }
}