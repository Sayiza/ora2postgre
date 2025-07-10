package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.OraclePackage;

/**
 * Strategy interface for converting Oracle packages to PostgreSQL equivalents.
 * Different implementations can handle different types of Oracle packages or 
 * special cases in the transformation process.
 */
public interface PackageTransformationStrategy {

  /**
   * Determines if this strategy can handle the given Oracle package.
   *
   * @param oraclePackage The Oracle package to evaluate
   * @return true if this strategy can convert the package, false otherwise
   */
  boolean supports(OraclePackage oraclePackage);

  /**
   * Converts an Oracle package to PostgreSQL DDL statements.
   * This method should only be called if supports() returns true.
   *
   * @param oraclePackage The Oracle package to convert
   * @param context The global context containing all migration data
   * @param specOnly If true, only generate function/procedure specs without bodies
   * @return PostgreSQL DDL statements for the package (functions and procedures)
   * @throws UnsupportedOperationException if the package is not supported by this strategy
   */
  String transform(OraclePackage oraclePackage, Everything context, boolean specOnly);

  /**
   * Gets a human-readable name for this strategy.
   * Used for logging and debugging purposes.
   *
   * @return Strategy name (e.g., "Standard Package", "HTP Package")
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
   * Can include warnings, notes, or recommendations for the converted package.
   *
   * @param oraclePackage The Oracle package being converted
   * @return Additional conversion information, or null if none
   */
  default String getConversionNotes(OraclePackage oraclePackage) {
    return null;
  }
}