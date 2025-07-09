package me.christianrobert.ora2postgre.transfer.strategy;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.transfer.progress.TransferProgress;
import me.christianrobert.ora2postgre.transfer.progress.TransferResult;

import java.sql.Connection;

/**
 * Strategy interface for different data transfer approaches between Oracle and PostgreSQL.
 *
 * Implementations can use different methods like:
 * - CSV streaming with COPY FROM
 * - Direct batched INSERT statements  
 * - Object type mapping and conversion
 * - Custom handling for complex data types
 */
public interface TransferStrategy {

  /**
   * Transfers data for a single table from Oracle to PostgreSQL.
   *
   * @param table TableMetadata containing schema, table name, and column information
   * @param oracleConn Active Oracle database connection
   * @param postgresConn Active PostgreSQL database connection
   * @param progress Progress tracker for reporting transfer status
   * @param everything The Everything context for object type and metadata lookups
   * @return TransferResult containing success status, row counts, and any error details
   * @throws Exception if transfer fails critically and cannot be recovered
   */
  TransferResult transferTable(TableMetadata table,
                               Connection oracleConn,
                               Connection postgresConn,
                               TransferProgress progress,
                               Everything everything) throws Exception;

  /**
   * Determines if this strategy can handle the given table based on its column types
   * and other characteristics.
   *
   * @param table TableMetadata to analyze
   * @param everything The Everything context for object type and metadata lookups (may be null)
   * @return true if this strategy can handle the table, false otherwise
   */
  boolean canHandle(TableMetadata table, Everything everything);

  /**
   * Returns a human-readable name for this strategy for logging and debugging.
   *
   * @return Strategy name (e.g., "CSV Streaming", "Direct Batching")
   */
  String getStrategyName();
}