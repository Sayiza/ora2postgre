package me.christianrobert.ora2postgre.plsql.ast.tools.managers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.TriggerMetadata;
import me.christianrobert.ora2postgre.plsql.ast.Trigger;
import me.christianrobert.ora2postgre.plsql.ast.tools.strategies.BasicTriggerStrategy;
import me.christianrobert.ora2postgre.plsql.ast.tools.strategies.TriggerTransformationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manager class that orchestrates Oracle to PostgreSQL trigger conversion using different strategies.
 * Handles strategy registration, selection, and transformation coordination for both AST and metadata triggers.
 */
public class TriggerTransformationManager {

  private static final Logger log = LoggerFactory.getLogger(TriggerTransformationManager.class);

  private final List<TriggerTransformationStrategy> strategies;
  private final Map<String, TriggerTransformationStrategy> strategyByType;

  /**
   * Constructor that initializes all available strategies.
   */
  public TriggerTransformationManager() {
    this.strategies = new ArrayList<>();
    this.strategyByType = new HashMap<>();

    // Register strategies in priority order (highest priority first)
    registerStrategy(new BasicTriggerStrategy());

    log.info("Initialized TriggerTransformationManager with {} strategies", strategies.size());
  }

  /**
   * Registers a new strategy with the manager.
   * Strategies are automatically sorted by priority.
   */
  public void registerStrategy(TriggerTransformationStrategy strategy) {
    if (strategy != null) {
      strategies.add(strategy);
      strategyByType.put(strategy.getTriggerType(), strategy);
      // Sort by priority (descending - highest priority first)
      strategies.sort((s1, s2) -> Integer.compare(s2.getPriority(), s1.getPriority()));
      log.debug("Registered strategy: {} (priority: {})", strategy.getStrategyName(), strategy.getPriority());
    }
  }

  /**
   * Finds the most appropriate strategy for the given trigger AST.
   *
   * @param trigger The trigger AST to find a strategy for
   * @return The best matching strategy, or null if none found
   */
  public TriggerTransformationStrategy selectStrategy(Trigger trigger) {
    log.debug("Selecting strategy for trigger {}", trigger.getTriggerName());

    // Find the first strategy that supports this trigger (strategies are sorted by priority)
    for (TriggerTransformationStrategy strategy : strategies) {
      if (strategy.supports(trigger)) {
        log.debug("Selected strategy: {} for trigger {}",
                strategy.getStrategyName(), trigger.getTriggerName());
        return strategy;
      }
    }

    log.warn("No strategy found for trigger {} of type {}",
            trigger.getTriggerName(), trigger.getTriggerType());
    return null;
  }

  /**
   * Finds the most appropriate strategy for the given trigger metadata.
   *
   * @param triggerMetadata The trigger metadata to find a strategy for
   * @return The best matching strategy, or null if none found
   */
  public TriggerTransformationStrategy selectStrategy(TriggerMetadata triggerMetadata) {
    log.debug("Selecting strategy for trigger metadata {}", triggerMetadata.getTriggerName());

    // Find the first strategy that supports this trigger metadata (strategies are sorted by priority)
    for (TriggerTransformationStrategy strategy : strategies) {
      if (strategy.supports(triggerMetadata)) {
        log.debug("Selected strategy: {} for trigger metadata {}",
                strategy.getStrategyName(), triggerMetadata.getTriggerName());
        return strategy;
      }
    }

    log.warn("No strategy found for trigger metadata {} of type {}",
            triggerMetadata.getTriggerName(), triggerMetadata.getTriggerType());
    return null;
  }

  /**
   * Transforms the given trigger AST into PostgreSQL trigger function.
   *
   * @param trigger The trigger AST to transform
   * @param context The global context containing all migration data
   * @return PostgreSQL trigger function DDL
   */
  public String transformTriggerFunction(Trigger trigger, Everything context) {
    TriggerTransformationStrategy strategy = selectStrategy(trigger);

    if (strategy == null) {
      throw new RuntimeException("No strategy found for trigger " + trigger.getTriggerName());
    }

    try {
      String result = strategy.transformTriggerFunction(trigger, context);
      log.debug("Successfully transformed trigger {} function using {} strategy",
              trigger.getTriggerName(), strategy.getStrategyName());
      return result;
    } catch (Exception e) {
      log.error("Error transforming trigger {} function with strategy {}: {}",
              trigger.getTriggerName(), strategy.getStrategyName(), e.getMessage());
      throw new RuntimeException("Failed to transform trigger function " + trigger.getTriggerName(), e);
    }
  }

  /**
   * Transforms the given trigger AST into PostgreSQL trigger definition.
   *
   * @param trigger The trigger AST to transform
   * @param context The global context containing all migration data
   * @return PostgreSQL CREATE TRIGGER statement
   */
  public String transformTriggerDefinition(Trigger trigger, Everything context) {
    TriggerTransformationStrategy strategy = selectStrategy(trigger);

    if (strategy == null) {
      throw new RuntimeException("No strategy found for trigger " + trigger.getTriggerName());
    }

    try {
      String result = strategy.transformTriggerDefinition(trigger, context);
      log.debug("Successfully transformed trigger {} definition using {} strategy",
              trigger.getTriggerName(), strategy.getStrategyName());
      return result;
    } catch (Exception e) {
      log.error("Error transforming trigger {} definition with strategy {}: {}",
              trigger.getTriggerName(), strategy.getStrategyName(), e.getMessage());
      throw new RuntimeException("Failed to transform trigger definition " + trigger.getTriggerName(), e);
    }
  }

  /**
   * Transforms the given trigger metadata into PostgreSQL trigger function stub.
   *
   * @param triggerMetadata The trigger metadata to transform
   * @param context The global context containing all migration data
   * @return PostgreSQL trigger function stub
   */
  public String transformTriggerFunctionStub(TriggerMetadata triggerMetadata, Everything context) {
    TriggerTransformationStrategy strategy = selectStrategy(triggerMetadata);

    if (strategy == null) {
      throw new RuntimeException("No strategy found for trigger metadata " + triggerMetadata.getTriggerName());
    }

    try {
      String result = strategy.transformTriggerFunctionStub(triggerMetadata, context);
      log.debug("Successfully transformed trigger metadata {} function stub using {} strategy",
              triggerMetadata.getTriggerName(), strategy.getStrategyName());
      return result;
    } catch (Exception e) {
      log.error("Error transforming trigger metadata {} function stub with strategy {}: {}",
              triggerMetadata.getTriggerName(), strategy.getStrategyName(), e.getMessage());
      throw new RuntimeException("Failed to transform trigger metadata function stub " + triggerMetadata.getTriggerName(), e);
    }
  }

  /**
   * Transforms the given trigger metadata into PostgreSQL trigger definition stub.
   *
   * @param triggerMetadata The trigger metadata to transform
   * @param context The global context containing all migration data
   * @return PostgreSQL CREATE TRIGGER statement stub
   */
  public String transformTriggerDefinitionStub(TriggerMetadata triggerMetadata, Everything context) {
    TriggerTransformationStrategy strategy = selectStrategy(triggerMetadata);

    if (strategy == null) {
      throw new RuntimeException("No strategy found for trigger metadata " + triggerMetadata.getTriggerName());
    }

    try {
      String result = strategy.transformTriggerDefinitionStub(triggerMetadata, context);
      log.debug("Successfully transformed trigger metadata {} definition stub using {} strategy",
              triggerMetadata.getTriggerName(), strategy.getStrategyName());
      return result;
    } catch (Exception e) {
      log.error("Error transforming trigger metadata {} definition stub with strategy {}: {}",
              triggerMetadata.getTriggerName(), strategy.getStrategyName(), e.getMessage());
      throw new RuntimeException("Failed to transform trigger metadata definition stub " + triggerMetadata.getTriggerName(), e);
    }
  }

  /**
   * Gets all registered strategies.
   *
   * @return Immutable list of all registered strategies
   */
  public List<TriggerTransformationStrategy> getStrategies() {
    return Collections.unmodifiableList(strategies);
  }

  /**
   * Gets a strategy by trigger type.
   *
   * @param triggerType Oracle trigger type
   * @return Strategy for the trigger type, or null if not found
   */
  public TriggerTransformationStrategy getStrategyByType(String triggerType) {
    return strategyByType.get(triggerType);
  }

  /**
   * Gets statistics about strategy usage.
   *
   * @return Map of strategy names to their usage counts
   */
  public Map<String, Integer> getStrategyUsage() {
    // For now, return empty map - this could be enhanced to track usage
    return Collections.emptyMap();
  }
}