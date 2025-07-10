package me.christianrobert.ora2postgre.plsql.ast.tools.managers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.Function;
import me.christianrobert.ora2postgre.plsql.ast.tools.strategies.FunctionTransformationStrategy;
import me.christianrobert.ora2postgre.plsql.ast.tools.strategies.StandardFunctionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manager class that orchestrates Oracle to PostgreSQL function conversion using different strategies.
 * Handles strategy registration, selection, and conversion coordination.
 */
public class FunctionTransformationManager {

  private static final Logger log = LoggerFactory.getLogger(FunctionTransformationManager.class);

  private final List<FunctionTransformationStrategy> strategies;
  private final StandardFunctionStrategy standardStrategy;

  /**
   * Constructor that initializes all available strategies.
   */
  public FunctionTransformationManager() {
    this.strategies = new ArrayList<>();
    this.standardStrategy = new StandardFunctionStrategy();

    // Register strategies in priority order (highest priority first)
    registerStrategy(standardStrategy);

    log.info("Initialized FunctionTransformationManager with {} strategies", strategies.size());
  }

  /**
   * Registers a new strategy with the manager.
   * Strategies are automatically sorted by priority.
   */
  public void registerStrategy(FunctionTransformationStrategy strategy) {
    if (strategy != null) {
      strategies.add(strategy);
      // Sort by priority (descending - highest priority first)
      strategies.sort((s1, s2) -> Integer.compare(s2.getPriority(), s1.getPriority()));
      log.debug("Registered strategy: {} (priority: {})", strategy.getStrategyName(), strategy.getPriority());
    }
  }

  /**
   * Finds the most appropriate strategy for the given function.
   *
   * @param function The function to find a strategy for
   * @return The best matching strategy, or StandardFunctionStrategy as fallback
   */
  public FunctionTransformationStrategy selectStrategy(Function function) {
    log.debug("Selecting strategy for function {}", function.getName());

    // Find the first strategy that supports this function (strategies are sorted by priority)
    for (FunctionTransformationStrategy strategy : strategies) {
      if (strategy.supports(function)) {
        log.debug("Selected strategy: {} for function {}",
                strategy.getStrategyName(), function.getName());
        return strategy;
      }
    }

    // This should never happen since StandardFunctionStrategy supports all functions
    log.warn("No strategy found for function {}, using StandardFunctionStrategy as fallback",
            function.getName());
    return standardStrategy;
  }

  /**
   * Transforms the given function using the most appropriate strategy.
   *
   * @param function The function to transform
   * @param context The global context containing all migration data
   * @param specOnly If true, only generate function spec without body
   * @return PostgreSQL DDL statement for the function
   */
  public String transform(Function function, Everything context, boolean specOnly) {
    FunctionTransformationStrategy strategy = selectStrategy(function);

    try {
      String result = strategy.transform(function, context, specOnly);
      log.debug("Successfully transformed function {} using {} strategy",
              function.getName(), strategy.getStrategyName());
      return result;
    } catch (Exception e) {
      log.error("Error transforming function {} with strategy {}: {}",
              function.getName(), strategy.getStrategyName(), e.getMessage());
      throw new RuntimeException("Failed to transform function " + function.getName(), e);
    }
  }

  /**
   * Gets all registered strategies.
   *
   * @return Immutable list of all registered strategies
   */
  public List<FunctionTransformationStrategy> getStrategies() {
    return Collections.unmodifiableList(strategies);
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