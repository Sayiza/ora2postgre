package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.Procedure;

/**
 * Strategy interface for converting Oracle procedures to PostgreSQL equivalents.
 * Different implementations can handle different types of Oracle procedures or 
 * special cases in the transformation process.
 */
public interface ProcedureTransformationStrategy {

  /**
   * Determines if this strategy can handle the given Oracle procedure.
   *
   * @param procedure The Oracle procedure to evaluate
   * @return true if this strategy can convert the procedure, false otherwise
   */
  boolean supports(Procedure procedure);

  /**
   * Converts an Oracle procedure to PostgreSQL DDL statement.
   * This method should only be called if supports() returns true.
   *
   * @param procedure The Oracle procedure to convert
   * @param context The global context containing all migration data
   * @param specOnly If true, only generate procedure spec without body
   * @return PostgreSQL DDL statement for the procedure
   * @throws UnsupportedOperationException if the procedure is not supported by this strategy
   */
  String transform(Procedure procedure, Everything context, boolean specOnly);

  /**
   * Gets a human-readable name for this strategy.
   * Used for logging and debugging purposes.
   *
   * @return Strategy name (e.g., "Standard Procedure", "HTP Procedure")
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
   * Gets additional information about the conversion process.
   * Can include warnings, notes, or recommendations for the converted procedure.
   *
   * @param procedure The Oracle procedure being converted
   * @return Additional conversion information, or null if none
   */
  default String getConversionNotes(Procedure procedure) {
    return null;
  }
}