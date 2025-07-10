package me.christianrobert.ora2postgre.plsql.ast.tools.managers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.OraclePackage;
import me.christianrobert.ora2postgre.plsql.ast.tools.strategies.PackageTransformationStrategy;
import me.christianrobert.ora2postgre.plsql.ast.tools.strategies.StandardPackageStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manager class that orchestrates Oracle to PostgreSQL package conversion using different strategies.
 * Handles strategy registration, selection, and conversion coordination.
 */
public class PackageTransformationManager {

  private static final Logger log = LoggerFactory.getLogger(PackageTransformationManager.class);

  private final List<PackageTransformationStrategy> strategies;
  private final StandardPackageStrategy standardStrategy;

  /**
   * Constructor that initializes all available strategies.
   */
  public PackageTransformationManager() {
    this.strategies = new ArrayList<>();
    this.standardStrategy = new StandardPackageStrategy();

    // Register strategies in priority order (highest priority first)
    registerStrategy(standardStrategy);

    log.info("Initialized PackageTransformationManager with {} strategies", strategies.size());
  }

  /**
   * Registers a new strategy with the manager.
   * Strategies are automatically sorted by priority.
   */
  public void registerStrategy(PackageTransformationStrategy strategy) {
    if (strategy != null) {
      strategies.add(strategy);
      // Sort by priority (descending - highest priority first)
      strategies.sort((s1, s2) -> Integer.compare(s2.getPriority(), s1.getPriority()));
      log.debug("Registered strategy: {} (priority: {})", strategy.getStrategyName(), strategy.getPriority());
    }
  }

  /**
   * Finds the most appropriate strategy for the given package.
   *
   * @param oraclePackage The package to find a strategy for
   * @return The best matching strategy, or StandardPackageStrategy as fallback
   */
  public PackageTransformationStrategy selectStrategy(OraclePackage oraclePackage) {
    log.debug("Selecting strategy for package {}.{}", oraclePackage.getSchema(), oraclePackage.getName());

    // Find the first strategy that supports this package (strategies are sorted by priority)
    for (PackageTransformationStrategy strategy : strategies) {
      if (strategy.supports(oraclePackage)) {
        log.debug("Selected strategy: {} for package {}.{}",
                strategy.getStrategyName(), oraclePackage.getSchema(), oraclePackage.getName());
        return strategy;
      }
    }

    // This should never happen since StandardPackageStrategy supports all packages
    log.warn("No strategy found for package {}.{}, using StandardPackageStrategy as fallback",
            oraclePackage.getSchema(), oraclePackage.getName());
    return standardStrategy;
  }

  /**
   * Transforms the given package using the most appropriate strategy.
   *
   * @param oraclePackage The package to transform
   * @param context The global context containing all migration data
   * @param specOnly If true, only generate function/procedure specs without bodies
   * @return PostgreSQL DDL statements for the package
   */
  public String transform(OraclePackage oraclePackage, Everything context, boolean specOnly) {
    PackageTransformationStrategy strategy = selectStrategy(oraclePackage);

    try {
      String result = strategy.transform(oraclePackage, context, specOnly);
      log.debug("Successfully transformed package {}.{} using {} strategy",
              oraclePackage.getSchema(), oraclePackage.getName(), strategy.getStrategyName());
      return result;
    } catch (Exception e) {
      log.error("Error transforming package {}.{} with strategy {}: {}",
              oraclePackage.getSchema(), oraclePackage.getName(), strategy.getStrategyName(), e.getMessage());
      throw new RuntimeException("Failed to transform package " + oraclePackage.getSchema() + "." + oraclePackage.getName(), e);
    }
  }

  /**
   * Gets all registered strategies.
   *
   * @return Immutable list of all registered strategies
   */
  public List<PackageTransformationStrategy> getStrategies() {
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