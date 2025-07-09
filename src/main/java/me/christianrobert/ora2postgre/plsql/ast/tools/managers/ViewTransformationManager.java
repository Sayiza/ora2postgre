package me.christianrobert.ora2postgre.plsql.ast.tools.managers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.ViewMetadata;
import me.christianrobert.ora2postgre.plsql.ast.SelectStatement;
import me.christianrobert.ora2postgre.plsql.ast.tools.strategies.BasicViewStrategy;
import me.christianrobert.ora2postgre.plsql.ast.tools.strategies.ViewTransformationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manager class that orchestrates Oracle to PostgreSQL view conversion using different strategies.
 * Handles strategy registration, selection, and transformation coordination for both metadata and AST views.
 */
public class ViewTransformationManager {

  private static final Logger log = LoggerFactory.getLogger(ViewTransformationManager.class);

  private final List<ViewTransformationStrategy> strategies;
  private final Map<String, ViewTransformationStrategy> strategyByComplexity;

  /**
   * Constructor that initializes all available strategies.
   */
  public ViewTransformationManager() {
    this.strategies = new ArrayList<>();
    this.strategyByComplexity = new HashMap<>();

    // Register strategies in priority order (highest priority first)
    registerStrategy(new BasicViewStrategy());

    log.info("Initialized ViewTransformationManager with {} strategies", strategies.size());
  }

  /**
   * Registers a new strategy with the manager.
   * Strategies are automatically sorted by priority.
   */
  public void registerStrategy(ViewTransformationStrategy strategy) {
    if (strategy != null) {
      strategies.add(strategy);
      strategyByComplexity.put(strategy.getViewComplexity(), strategy);
      // Sort by priority (descending - highest priority first)
      strategies.sort((s1, s2) -> Integer.compare(s2.getPriority(), s1.getPriority()));
      log.debug("Registered strategy: {} (priority: {})", strategy.getStrategyName(), strategy.getPriority());
    }
  }

  /**
   * Finds the most appropriate strategy for the given view metadata.
   *
   * @param viewMetadata The view metadata to find a strategy for
   * @return The best matching strategy, or null if none found
   */
  public ViewTransformationStrategy selectStrategy(ViewMetadata viewMetadata) {
    log.debug("Selecting strategy for view metadata {}", viewMetadata.getViewName());

    // Find the first strategy that supports this view metadata (strategies are sorted by priority)
    for (ViewTransformationStrategy strategy : strategies) {
      if (strategy.supports(viewMetadata)) {
        log.debug("Selected strategy: {} for view metadata {}",
                strategy.getStrategyName(), viewMetadata.getViewName());
        return strategy;
      }
    }

    log.warn("No strategy found for view metadata {} in schema {}",
            viewMetadata.getViewName(), viewMetadata.getSchema());
    return null;
  }

  /**
   * Finds the most appropriate strategy for the given select statement.
   *
   * @param selectStatement The select statement to find a strategy for
   * @return The best matching strategy, or null if none found
   */
  public ViewTransformationStrategy selectStrategy(SelectStatement selectStatement) {
    log.debug("Selecting strategy for select statement");

    // Find the first strategy that supports this select statement (strategies are sorted by priority)
    for (ViewTransformationStrategy strategy : strategies) {
      if (strategy.supports(selectStatement)) {
        log.debug("Selected strategy: {} for select statement", strategy.getStrategyName());
        return strategy;
      }
    }

    log.warn("No strategy found for select statement");
    return null;
  }

  /**
   * Transforms the given view metadata into PostgreSQL CREATE VIEW statement.
   *
   * @param viewMetadata The view metadata to transform
   * @param withDummyQuery Whether to include dummy NULL query for empty views
   * @param context The global context containing all migration data
   * @return PostgreSQL CREATE VIEW statement
   */
  public String transformViewMetadata(ViewMetadata viewMetadata, boolean withDummyQuery, Everything context) {
    ViewTransformationStrategy strategy = selectStrategy(viewMetadata);

    if (strategy == null) {
      throw new RuntimeException("No strategy found for view metadata " + viewMetadata.getViewName());
    }

    try {
      String result = strategy.transformViewMetadata(viewMetadata, withDummyQuery, context);
      log.debug("Successfully transformed view metadata {} using {} strategy",
              viewMetadata.getViewName(), strategy.getStrategyName());
      return result;
    } catch (Exception e) {
      log.error("Error transforming view metadata {} with strategy {}: {}",
              viewMetadata.getViewName(), strategy.getStrategyName(), e.getMessage());
      throw new RuntimeException("Failed to transform view metadata " + viewMetadata.getViewName(), e);
    }
  }

  /**
   * Transforms the given select statement into PostgreSQL SELECT statement.
   *
   * @param selectStatement The select statement to transform
   * @param context The global context containing all migration data
   * @return PostgreSQL SELECT statement
   */
  public String transformSelectStatement(SelectStatement selectStatement, Everything context) {
    ViewTransformationStrategy strategy = selectStrategy(selectStatement);

    if (strategy == null) {
      throw new RuntimeException("No strategy found for select statement");
    }

    try {
      String result = strategy.transformSelectStatement(selectStatement, context);
      log.debug("Successfully transformed select statement using {} strategy", strategy.getStrategyName());
      return result;
    } catch (Exception e) {
      log.error("Error transforming select statement with strategy {}: {}",
              strategy.getStrategyName(), e.getMessage());
      throw new RuntimeException("Failed to transform select statement", e);
    }
  }

  /**
   * Transforms the given select statement into PostgreSQL SELECT statement with schema context.
   *
   * @param selectStatement The select statement to transform
   * @param context The global context containing all migration data
   * @param schemaContext The schema context for transformation
   * @return PostgreSQL SELECT statement
   */
  public String transformSelectStatement(SelectStatement selectStatement, Everything context, String schemaContext) {
    ViewTransformationStrategy strategy = selectStrategy(selectStatement);

    if (strategy == null) {
      throw new RuntimeException("No strategy found for select statement");
    }

    try {
      String result = strategy.transformSelectStatement(selectStatement, context, schemaContext);
      log.debug("Successfully transformed select statement with schema context using {} strategy", strategy.getStrategyName());
      return result;
    } catch (Exception e) {
      log.error("Error transforming select statement with schema context using strategy {}: {}",
              strategy.getStrategyName(), e.getMessage());
      throw new RuntimeException("Failed to transform select statement with schema context", e);
    }
  }

  /**
   * Gets all registered strategies.
   *
   * @return Immutable list of all registered strategies
   */
  public List<ViewTransformationStrategy> getStrategies() {
    return Collections.unmodifiableList(strategies);
  }

  /**
   * Gets a strategy by view complexity.
   *
   * @param viewComplexity View complexity level
   * @return Strategy for the view complexity, or null if not found
   */
  public ViewTransformationStrategy getStrategyByComplexity(String viewComplexity) {
    return strategyByComplexity.get(viewComplexity);
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