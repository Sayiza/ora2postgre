package me.christianrobert.ora2postgre.oracledb;

import io.smallrye.config.ConfigMapping;

/**
 * Configuration properties for row count estimation strategies.
 */
@ConfigMapping(prefix = "row-count")
public interface RowCountConfig {

  /**
   * The method to use for row count estimation.
   * @return the row count method
   */
  RowCountMethod method();

  /**
   * Threshold above which to use sampling instead of exact count.
   * @return the sampling threshold
   */
  long samplingThreshold();

  /**
   * Maximum number of tables to use exact count for (regardless of size).
   * @return the max exact count tables
   */
  int maxExactCountTables();

  /**
   * Threshold for considering statistics stale (0.0-1.0).
   * 0.1 means statistics are stale if modifications > 10% of num_rows.
   * @return the staleness threshold
   */
  double statisticsStalenessThreshold();

  /**
   * Percentage to use for sampling (0.1-100.0).
   * 1.0 means 1% sample, 10.0 means 10% sample.
   * @return the sampling percentage
   */
  double samplingPercentage();
}