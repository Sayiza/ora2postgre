package me.christianrobert.ora2postgre.plsql.ast.tools.managers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.Procedure;
import me.christianrobert.ora2postgre.plsql.ast.tools.strategies.ProcedureTransformationStrategy;
import me.christianrobert.ora2postgre.plsql.ast.tools.strategies.StandardProcedureStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manager class that orchestrates Oracle to PostgreSQL procedure conversion using different strategies.
 * Handles strategy registration, selection, and conversion coordination.
 */
public class ProcedureTransformationManager {

  private static final Logger log = LoggerFactory.getLogger(ProcedureTransformationManager.class);

  private final List<ProcedureTransformationStrategy> strategies;
  private final StandardProcedureStrategy standardStrategy;

  /**
   * Constructor that initializes all available strategies.
   */
  public ProcedureTransformationManager() {
    this.strategies = new ArrayList<>();
    this.standardStrategy = new StandardProcedureStrategy();

    // Register strategies in priority order (highest priority first)
    registerStrategy(standardStrategy);

    log.info("Initialized ProcedureTransformationManager with {} strategies", strategies.size());
  }

  /**
   * Registers a new strategy with the manager.
   * Strategies are automatically sorted by priority.
   */
  public void registerStrategy(ProcedureTransformationStrategy strategy) {
    if (strategy != null) {
      strategies.add(strategy);
      // Sort by priority (descending - highest priority first)
      strategies.sort((s1, s2) -> Integer.compare(s2.getPriority(), s1.getPriority()));
      log.debug("Registered strategy: {} (priority: {})", strategy.getStrategyName(), strategy.getPriority());
    }
  }

  /**
   * Finds the most appropriate strategy for the given procedure.
   *
   * @param procedure The procedure to find a strategy for
   * @return The best matching strategy, or StandardProcedureStrategy as fallback
   */
  public ProcedureTransformationStrategy selectStrategy(Procedure procedure) {
    log.debug("Selecting strategy for procedure {}", procedure.getName());

    // Find the first strategy that supports this procedure (strategies are sorted by priority)
    for (ProcedureTransformationStrategy strategy : strategies) {
      if (strategy.supports(procedure)) {
        log.debug("Selected strategy: {} for procedure {}",
                strategy.getStrategyName(), procedure.getName());
        return strategy;
      }
    }

    // This should never happen since StandardProcedureStrategy supports all procedures
    log.warn("No strategy found for procedure {}, using StandardProcedureStrategy as fallback",
            procedure.getName());
    return standardStrategy;
  }

  /**
   * Transforms the given procedure using the most appropriate strategy.
   *
   * @param procedure The procedure to transform
   * @param context The global context containing all migration data
   * @param specOnly If true, only generate procedure spec without body
   * @return PostgreSQL DDL statement for the procedure
   */
  public String transform(Procedure procedure, Everything context, boolean specOnly) {
    ProcedureTransformationStrategy strategy = selectStrategy(procedure);

    try {
      String result = strategy.transform(procedure, context, specOnly);
      log.debug("Successfully transformed procedure {} using {} strategy",
              procedure.getName(), strategy.getStrategyName());
      return result;
    } catch (Exception e) {
      log.error("Error transforming procedure {} with strategy {}: {}",
              procedure.getName(), strategy.getStrategyName(), e.getMessage());
      throw new RuntimeException("Failed to transform procedure " + procedure.getName(), e);
    }
  }

  /**
   * Gets all registered strategies.
   *
   * @return Immutable list of all registered strategies
   */
  public List<ProcedureTransformationStrategy> getStrategies() {
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