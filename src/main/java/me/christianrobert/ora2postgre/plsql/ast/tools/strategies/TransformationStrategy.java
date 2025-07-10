package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

/**
 * Base interface for all transformation strategies in the ora2postgre project.
 * 
 * This interface establishes a common contract for transforming Oracle database
 * elements (tables, constraints, triggers, views, packages, functions, procedures)
 * to their PostgreSQL equivalents.
 * 
 * All specific transformation strategy interfaces should extend this base interface
 * to ensure consistency across the transformation architecture.
 */
public interface TransformationStrategy<T> {
  
  /**
   * Determines if this strategy can handle the given input object.
   * 
   * @param input The Oracle database element to be transformed
   * @return true if this strategy supports transforming the input, false otherwise
   */
  boolean supports(T input);
  
  /**
   * Gets a human-readable name for this transformation strategy.
   * Used for logging, debugging, and error reporting.
   * 
   * @return The strategy name (e.g., "StandardTableStrategy", "PrimaryKeyConstraintStrategy")
   */
  String getStrategyName();
  
  /**
   * Gets the priority of this strategy when multiple strategies support the same input.
   * Higher priority strategies are selected first.
   * 
   * @return The strategy priority (default: 0, higher values = higher priority)
   */
  default int getPriority() {
    return 0;
  }
}