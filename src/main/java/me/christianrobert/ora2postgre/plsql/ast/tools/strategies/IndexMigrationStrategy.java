package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.oracledb.IndexMetadata;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PostgreSQLIndexDDL;

/**
 * Strategy interface for converting Oracle indexes to PostgreSQL equivalents.
 * Different implementations handle different types of Oracle indexes based on their
 * convertibility and complexity.
 */
public interface IndexMigrationStrategy extends TransformationStrategy<IndexMetadata> {

  /**
   * Determines if this strategy can handle the given Oracle index.
   *
   * @param index The Oracle index metadata to evaluate
   * @return true if this strategy can convert the index, false otherwise
   */
  boolean supports(IndexMetadata index);

  /**
   * Converts an Oracle index to PostgreSQL DDL.
   * This method should only be called if supports() returns true.
   *
   * @param index The Oracle index metadata to convert
   * @return PostgreSQL DDL representation of the index
   * @throws UnsupportedOperationException if the index is not supported by this strategy
   */
  PostgreSQLIndexDDL convert(IndexMetadata index);

  /**
   * Gets a human-readable name for this strategy.
   * Used for logging and debugging purposes.
   *
   * @return Strategy name (e.g., "B-Tree Index", "Unique Index", "Unsupported")
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
   * Determines if this strategy generates actual PostgreSQL DDL or just reports.
   *
   * @return true if this strategy generates executable DDL, false if it only reports issues
   */
  default boolean generatesDDL() {
    return true;
  }

  /**
   * Gets additional information about the conversion process.
   * Can include warnings, notes, or recommendations for the converted index.
   *
   * @param index The Oracle index being converted
   * @return Additional conversion information, or null if none
   */
  default String getConversionNotes(IndexMetadata index) {
    return null;
  }
}