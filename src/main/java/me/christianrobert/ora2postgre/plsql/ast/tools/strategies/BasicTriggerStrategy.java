package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.TriggerMetadata;
import me.christianrobert.ora2postgre.plsql.ast.Trigger;

/**
 * Basic implementation of TriggerTransformationStrategy that delegates to existing transformation logic.
 * This strategy handles all trigger types by forwarding to the current toPostgre() methods.
 * It serves as a backward compatibility layer during the transition to the strategy pattern.
 */
public class BasicTriggerStrategy implements TriggerTransformationStrategy {

  @Override
  public boolean supports(Trigger trigger) {
    // This basic strategy supports all triggers by delegating to existing logic
    return trigger != null;
  }

  @Override
  public boolean supports(TriggerMetadata triggerMetadata) {
    // This basic strategy supports all trigger metadata by delegating to existing logic
    return triggerMetadata != null;
  }

  @Override
  public String transformTriggerFunction(Trigger trigger, Everything context) {
    if (trigger == null) {
      throw new UnsupportedOperationException("Trigger cannot be null");
    }
    
    // Delegate to existing AST transformation logic
    return trigger.toPostgreTriggerFunction(context);
  }

  @Override
  public String transformTriggerDefinition(Trigger trigger, Everything context) {
    if (trigger == null) {
      throw new UnsupportedOperationException("Trigger cannot be null");
    }
    
    // Delegate to existing AST transformation logic
    return trigger.toPostgreTriggerDefinition(context);
  }

  @Override
  public String transformTriggerFunctionStub(TriggerMetadata triggerMetadata, Everything context) {
    if (triggerMetadata == null) {
      throw new UnsupportedOperationException("TriggerMetadata cannot be null");
    }
    
    // Delegate to existing metadata transformation logic
    return triggerMetadata.toPostgreFunctionStub();
  }

  @Override
  public String transformTriggerDefinitionStub(TriggerMetadata triggerMetadata, Everything context) {
    if (triggerMetadata == null) {
      throw new UnsupportedOperationException("TriggerMetadata cannot be null");
    }
    
    // Delegate to existing metadata transformation logic
    return triggerMetadata.toPostgreTriggerStub();
  }

  @Override
  public String getStrategyName() {
    return "Basic Trigger Strategy";
  }

  @Override
  public int getPriority() {
    // Lower priority so more specific strategies can override this one
    return 10;
  }

  @Override
  public String getTriggerType() {
    return "ALL"; // This strategy handles all trigger types
  }

  @Override
  public String getConversionNotes(Trigger trigger) {
    return "Converted using basic strategy - delegates to existing transformation logic";
  }

  @Override
  public String getConversionNotes(TriggerMetadata triggerMetadata) {
    return "Converted using basic strategy - delegates to existing transformation logic";
  }
}