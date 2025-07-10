package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.Function;

/**
 * Strategy interface for converting Oracle functions to PostgreSQL equivalents.
 * Different implementations can handle different types of Oracle functions or 
 * special cases in the transformation process.
 */
public interface FunctionTransformationStrategy {

  /**
   * Determines if this strategy can handle the given Oracle function.
   *
   * @param function The Oracle function to evaluate
   * @return true if this strategy can convert the function, false otherwise
   */
  boolean supports(Function function);

  /**
   * Converts an Oracle function to PostgreSQL DDL statement.
   * This method should only be called if supports() returns true.
   *
   * @param function The Oracle function to convert
   * @param context The global context containing all migration data
   * @param specOnly If true, only generate function spec without body
   * @return PostgreSQL DDL statement for the function
   * @throws UnsupportedOperationException if the function is not supported by this strategy
   */
  String transform(Function function, Everything context, boolean specOnly);

  /**
   * Gets a human-readable name for this strategy.
   * Used for logging and debugging purposes.
   *
   * @return Strategy name (e.g., "Standard Function", "Aggregation Function")
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
   * Can include warnings, notes, or recommendations for the converted function.
   *
   * @param function The Oracle function being converted
   * @return Additional conversion information, or null if none
   */
  default String getConversionNotes(Function function) {
    return null;
  }
}