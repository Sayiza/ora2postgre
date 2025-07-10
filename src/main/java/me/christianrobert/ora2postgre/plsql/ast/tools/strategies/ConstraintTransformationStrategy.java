package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.ConstraintMetadata;

/**
 * Strategy interface for converting Oracle constraints to PostgreSQL equivalents.
 * Different implementations handle different types of Oracle constraints based on their
 * complexity and transformation requirements.
 */
public interface ConstraintTransformationStrategy extends TransformationStrategy<ConstraintMetadata> {


  /**
   * Converts an Oracle constraint to PostgreSQL DDL.
   * This method should only be called if supports() returns true.
   *
   * @param constraint The Oracle constraint metadata to convert
   * @param context The global context containing all migration data
   * @return PostgreSQL DDL representation of the constraint
   * @throws UnsupportedOperationException if the constraint is not supported by this strategy
   */
  String transformConstraintDDL(ConstraintMetadata constraint, Everything context);

  /**
   * Converts an Oracle constraint to a complete PostgreSQL ALTER TABLE statement.
   * This method should only be called if supports() returns true.
   *
   * @param constraint The Oracle constraint metadata to convert
   * @param schemaName Schema name for the table
   * @param tableName Table name for the constraint
   * @param context The global context containing all migration data
   * @return Complete PostgreSQL ALTER TABLE statement
   * @throws UnsupportedOperationException if the constraint is not supported by this strategy
   */
  String transformAlterTableDDL(ConstraintMetadata constraint, String schemaName, String tableName, Everything context);


  /**
   * Gets the Oracle constraint type this strategy handles.
   *
   * @return Oracle constraint type code (e.g., "P", "R", "U", "C")
   */
  String getConstraintType();

  /**
   * Determines if this strategy requires dependency validation.
   * Foreign keys typically require validation that referenced tables exist.
   *
   * @return true if this strategy requires dependency validation
   */
  default boolean requiresDependencyValidation() {
    return false;
  }

  /**
   * Validates dependencies for this constraint.
   * Only called if requiresDependencyValidation() returns true.
   *
   * @param constraint The constraint to validate
   * @param context The global context containing all migration data
   * @return true if dependencies are satisfied, false otherwise
   */
  default boolean validateDependencies(ConstraintMetadata constraint, Everything context) {
    return true;
  }

  /**
   * Gets additional information about the conversion process.
   * Can include warnings, notes, or recommendations for the converted constraint.
   *
   * @param constraint The Oracle constraint being converted
   * @return Additional conversion information, or null if none
   */
  default String getConversionNotes(ConstraintMetadata constraint) {
    return null;
  }
}