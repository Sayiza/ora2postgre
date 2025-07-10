package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;

import java.util.List;

/**
 * Strategy interface for converting Oracle tables to PostgreSQL equivalents.
 * Different implementations can handle different types of Oracle tables or 
 * special cases in the transformation process.
 */
public interface TableTransformationStrategy extends TransformationStrategy<TableMetadata> {

  /**
   * Converts an Oracle table to PostgreSQL DDL statements.
   * This method should only be called if supports() returns true.
   *
   * @param table The Oracle table metadata to convert
   * @param context The global context containing all migration data
   * @return List of PostgreSQL DDL statements for the table
   * @throws UnsupportedOperationException if the table is not supported by this strategy
   */
  List<String> transform(TableMetadata table, Everything context);

  /**
   * Gets additional information about the conversion process.
   * Can include warnings, notes, or recommendations for the converted table.
   *
   * @param table The Oracle table being converted
   * @return Additional conversion information, or null if none
   */
  default String getConversionNotes(TableMetadata table) {
    return null;
  }
}