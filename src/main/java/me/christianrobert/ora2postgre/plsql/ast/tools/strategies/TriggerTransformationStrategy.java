package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.TriggerMetadata;
import me.christianrobert.ora2postgre.plsql.ast.Trigger;

/**
 * Strategy interface for converting Oracle triggers to PostgreSQL equivalents.
 * This interface supports the two-phase trigger transformation required by PostgreSQL:
 * 1. Function generation (contains trigger logic)
 * 2. Trigger definition generation (CREATE TRIGGER statement)
 */
public interface TriggerTransformationStrategy extends TransformationStrategy<TriggerMetadata> {

  /**
   * Determines if this strategy can handle the given Oracle trigger.
   *
   * @param trigger The Oracle trigger AST to evaluate
   * @return true if this strategy can convert the trigger, false otherwise
   */
  boolean supports(Trigger trigger);

  /**
   * Determines if this strategy can handle the given Oracle trigger metadata.
   *
   * @param triggerMetadata The Oracle trigger metadata to evaluate
   * @return true if this strategy can convert the trigger, false otherwise
   */
  boolean supports(TriggerMetadata triggerMetadata);

  /**
   * Transforms an Oracle trigger AST to PostgreSQL trigger function.
   * This method should only be called if supports() returns true.
   *
   * @param trigger The Oracle trigger AST to convert
   * @param context The global context containing all migration data
   * @return PostgreSQL function DDL for the trigger
   * @throws UnsupportedOperationException if the trigger is not supported by this strategy
   */
  String transformTriggerFunction(Trigger trigger, Everything context);

  /**
   * Transforms an Oracle trigger AST to PostgreSQL trigger definition.
   * This method should only be called if supports() returns true.
   *
   * @param trigger The Oracle trigger AST to convert
   * @param context The global context containing all migration data
   * @return PostgreSQL CREATE TRIGGER statement
   * @throws UnsupportedOperationException if the trigger is not supported by this strategy
   */
  String transformTriggerDefinition(Trigger trigger, Everything context);

  /**
   * Transforms an Oracle trigger metadata to PostgreSQL trigger function stub.
   * This method should only be called if supports() returns true.
   *
   * @param triggerMetadata The Oracle trigger metadata to convert
   * @param context The global context containing all migration data
   * @return PostgreSQL function stub for the trigger
   * @throws UnsupportedOperationException if the trigger is not supported by this strategy
   */
  String transformTriggerFunctionStub(TriggerMetadata triggerMetadata, Everything context);

  /**
   * Transforms an Oracle trigger metadata to PostgreSQL trigger definition stub.
   * This method should only be called if supports() returns true.
   *
   * @param triggerMetadata The Oracle trigger metadata to convert
   * @param context The global context containing all migration data
   * @return PostgreSQL CREATE TRIGGER statement stub
   * @throws UnsupportedOperationException if the trigger is not supported by this strategy
   */
  String transformTriggerDefinitionStub(TriggerMetadata triggerMetadata, Everything context);

  /**
   * Gets a human-readable name for this strategy.
   * Used for logging and debugging purposes.
   *
   * @return Strategy name (e.g., "Before/After Trigger", "Instead Of Trigger")
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
   * Gets the Oracle trigger type this strategy handles.
   *
   * @return Oracle trigger type (e.g., "BEFORE", "AFTER", "INSTEAD OF")
   */
  String getTriggerType();

  /**
   * Gets additional information about the conversion process.
   * Can include warnings, notes, or recommendations for the converted trigger.
   *
   * @param trigger The Oracle trigger being converted
   * @return Additional conversion information, or null if none
   */
  default String getConversionNotes(Trigger trigger) {
    return null;
  }

  /**
   * Gets additional information about the conversion process.
   * Can include warnings, notes, or recommendations for the converted trigger.
   *
   * @param triggerMetadata The Oracle trigger metadata being converted
   * @return Additional conversion information, or null if none
   */
  default String getConversionNotes(TriggerMetadata triggerMetadata) {
    return null;
  }
}