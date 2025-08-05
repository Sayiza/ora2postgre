package me.christianrobert.ora2postgre.plsql.ast.tools.managers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.ConstraintMetadata;
import me.christianrobert.ora2postgre.oracledb.IndexMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.plsql.ast.tools.strategies.*;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PostgreSQLIndexDDL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manager class that orchestrates Oracle to PostgreSQL index conversion using different strategies.
 * Handles strategy registration, selection, and conversion coordination.
 */
public class IndexMigrationStrategyManager {

  private static final Logger log = LoggerFactory.getLogger(IndexMigrationStrategyManager.class);

  private final List<IndexMigrationStrategy> strategies;
  private final UnsupportedIndexStrategy unsupportedStrategy;

  /**
   * Constructor that initializes all available strategies.
   */
  public IndexMigrationStrategyManager() {
    this.strategies = new ArrayList<>();
    this.unsupportedStrategy = new UnsupportedIndexStrategy();

    // Register strategies in priority order (highest priority first)
    registerStrategy(new UniqueIndexStrategy());
    registerStrategy(new CompositeIndexStrategy());
    registerStrategy(new BTreeIndexStrategy());

    // UnsupportedIndexStrategy is not registered in the list since it's the fallback

    log.info("Initialized IndexMigrationStrategyManager with {} strategies", strategies.size());
  }

  /**
   * Registers a new strategy with the manager.
   * Strategies are automatically sorted by priority.
   */
  public void registerStrategy(IndexMigrationStrategy strategy) {
    if (strategy != null) {
      strategies.add(strategy);
      // Sort by priority (descending - highest priority first)
      strategies.sort((s1, s2) -> Integer.compare(s2.getPriority(), s1.getPriority()));
      log.debug("Registered strategy: {} (priority: {})", strategy.getStrategyName(), strategy.getPriority());
    }
  }

  /**
   * Selects the appropriate strategy for converting the given index.
   * Returns the first strategy that supports the index, or UnsupportedIndexStrategy as fallback.
   */
  public IndexMigrationStrategy selectStrategy(IndexMetadata index) {
    for (IndexMigrationStrategy strategy : strategies) {
      if (strategy.supports(index)) {
        log.debug("Selected strategy '{}' for index: {}", strategy.getStrategyName(), index.getIndexName());
        return strategy;
      }
    }

    log.debug("No specific strategy found for index: {}, using UnsupportedIndexStrategy", index.getIndexName());
    return unsupportedStrategy;
  }

  /**
   * Converts a single Oracle index using the most appropriate strategy.
   */
  public PostgreSQLIndexDDL convertIndex(IndexMetadata index) {
    try {
      IndexMigrationStrategy strategy = selectStrategy(index);
      PostgreSQLIndexDDL result = strategy.convert(index);

      log.debug("Converted index {} using strategy: {}", index.getIndexName(), strategy.getStrategyName());
      return result;

    } catch (Exception e) {
      log.error("Failed to convert index: {} - {}", index.getIndexName(), e.getMessage(), e);

      // Create an error result using unsupported strategy
      return new PostgreSQLIndexDDL(
              index.getIndexName(),
              index.getTableName(),
              index.getSchemaName(),
              "Conversion failed: " + e.getMessage()
      );
    }
  }

  /**
   * Converts a list of Oracle indexes to PostgreSQL DDL with constraint deduplication.
   * Returns results separated by supported and unsupported indexes.
   */
  public IndexConversionResult convertIndexes(List<IndexMetadata> indexes, Everything context) {
    List<PostgreSQLIndexDDL> supportedIndexes = new ArrayList<>();
    List<PostgreSQLIndexDDL> unsupportedIndexes = new ArrayList<>();
    Map<String, Integer> strategyUsageStats = new HashMap<>();

    log.info("Converting {} indexes to PostgreSQL", indexes.size());

    // Filter out indexes that conflict with constraints
    List<IndexMetadata> filteredIndexes = filterConflictingIndexes(indexes, context);
    int conflictingCount = indexes.size() - filteredIndexes.size();
    
    if (conflictingCount > 0) {
      log.info("Filtered out {} indexes that conflict with constraints", conflictingCount);
      strategyUsageStats.put("Constraint Conflict", conflictingCount);
    }

    for (IndexMetadata index : filteredIndexes) {
      try {
        PostgreSQLIndexDDL result = convertIndex(index);

        // Track strategy usage
        IndexMigrationStrategy strategy = selectStrategy(index);
        strategyUsageStats.merge(strategy.getStrategyName(), 1, Integer::sum);

        if (result.isSupported()) {
          supportedIndexes.add(result);
          log.debug("✓ Converted: {}", index.getIndexName());
        } else {
          unsupportedIndexes.add(result);
          log.debug("✗ Unsupported: {} - {}", index.getIndexName(), result.getConversionNotes());
        }

      } catch (Exception e) {
        log.error("Error converting index {}: {}", index.getIndexName(), e.getMessage());

        // Add to unsupported with error information
        PostgreSQLIndexDDL errorResult = new PostgreSQLIndexDDL(
                index.getIndexName(),
                index.getTableName(),
                index.getSchemaName(),
                "Conversion error: " + e.getMessage()
        );
        unsupportedIndexes.add(errorResult);
        strategyUsageStats.merge("Error", 1, Integer::sum);
      }
    }

    log.info("Index conversion completed: {} supported, {} unsupported, {} skipped due to constraint conflicts",
            supportedIndexes.size(), unsupportedIndexes.size(), conflictingCount);

    // Log strategy usage statistics
    strategyUsageStats.forEach((strategy, count) ->
            log.info("Strategy '{}': {} indexes", strategy, count));

    return new IndexConversionResult(supportedIndexes, unsupportedIndexes, strategyUsageStats);
  }

  /**
   * Legacy method for backward compatibility. Uses convertIndexes with null context.
   */
  public IndexConversionResult convertIndexes(List<IndexMetadata> indexes) {
    return convertIndexes(indexes, null);
  }

  /**
   * Gets statistics about registered strategies.
   */
  public Map<String, Object> getStrategyStatistics() {
    Map<String, Object> stats = new HashMap<>();

    stats.put("totalStrategies", strategies.size() + 1); // +1 for unsupported strategy
    stats.put("registeredStrategies", strategies.stream()
            .map(IndexMigrationStrategy::getStrategyName)
            .toList());

    return stats;
  }

  /**
   * Gets all registered strategies for inspection or testing.
   */
  public List<IndexMigrationStrategy> getRegisteredStrategies() {
    return new ArrayList<>(strategies);
  }

  /**
   * Filters out indexes that would conflict with constraint-generated indexes.
   * PostgreSQL constraints automatically create indexes, so we need to avoid duplicates.
   */
  private List<IndexMetadata> filterConflictingIndexes(List<IndexMetadata> indexes, Everything context) {
    if (context == null) {
      log.debug("No context provided - skipping constraint conflict detection");
      return new ArrayList<>(indexes);
    }

    // Build set of constraint-generated index signatures
    Set<String> constraintIndexSignatures = buildConstraintIndexSignatures(context);
    
    if (constraintIndexSignatures.isEmpty()) {
      log.debug("No constraint-generated indexes found");
      return new ArrayList<>(indexes);
    }

    List<IndexMetadata> filteredIndexes = new ArrayList<>();
    
    for (IndexMetadata index : indexes) {
      String indexSignature = buildIndexSignature(index);
      
      if (constraintIndexSignatures.contains(indexSignature)) {
        log.debug("Filtering out index {} - conflicts with constraint-generated index", index.getIndexName());
      } else {
        filteredIndexes.add(index);
      }
    }
    
    return filteredIndexes;
  }

  /**
   * Builds signatures for indexes that will be automatically created by constraints.
   */
  private Set<String> buildConstraintIndexSignatures(Everything context) {
    Set<String> signatures = new HashSet<>();
    
    for (TableMetadata table : context.getTableSql()) {
      String schema = table.getSchema();
      String tableName = table.getTableName();
      
      for (ConstraintMetadata constraint : table.getConstraints()) {
        // Primary key and unique constraints create indexes automatically in PostgreSQL
        if (constraint.isPrimaryKey() || constraint.isUniqueConstraint()) {
          String signature = buildConstraintIndexSignature(schema, tableName, constraint.getColumnNames());
          signatures.add(signature);
          log.debug("Added constraint index signature: {}", signature);
        }
      }
    }
    
    return signatures;
  }

  /**
   * Builds a signature for an index based on schema, table, and column names.
   */
  private String buildIndexSignature(IndexMetadata index) {
    List<String> columnNames = index.getColumns().stream()
        .map(col -> col.getColumnName())
        .filter(name -> name != null && !name.trim().isEmpty())
        .collect(Collectors.toList());
    
    return buildConstraintIndexSignature(index.getSchemaName(), index.getTableName(), columnNames);
  }

  /**
   * Builds a standardized signature for comparing indexes and constraints.
   */
  private String buildConstraintIndexSignature(String schema, String tableName, List<String> columnNames) {
    // Normalize schema and table names to lowercase
    String normalizedSchema = schema != null ? schema.toLowerCase() : "";
    String normalizedTable = tableName != null ? tableName.toLowerCase() : "";
    
    // Sort column names to handle different ordering
    List<String> sortedColumns = columnNames.stream()
        .map(String::toLowerCase)
        .sorted()
        .collect(Collectors.toList());
    
    return normalizedSchema + "." + normalizedTable + ":" + String.join(",", sortedColumns);
  }

  /**
   * Result class that contains the outcome of index conversion process.
   */
  public static class IndexConversionResult {
    private final List<PostgreSQLIndexDDL> supportedIndexes;
    private final List<PostgreSQLIndexDDL> unsupportedIndexes;
    private final Map<String, Integer> strategyUsageStats;

    public IndexConversionResult(List<PostgreSQLIndexDDL> supportedIndexes,
                                 List<PostgreSQLIndexDDL> unsupportedIndexes,
                                 Map<String, Integer> strategyUsageStats) {
      this.supportedIndexes = new ArrayList<>(supportedIndexes);
      this.unsupportedIndexes = new ArrayList<>(unsupportedIndexes);
      this.strategyUsageStats = new HashMap<>(strategyUsageStats);
    }

    public List<PostgreSQLIndexDDL> getSupportedIndexes() {
      return supportedIndexes;
    }

    public List<PostgreSQLIndexDDL> getUnsupportedIndexes() {
      return unsupportedIndexes;
    }

    public Map<String, Integer> getStrategyUsageStats() {
      return strategyUsageStats;
    }

    public int getTotalIndexes() {
      return supportedIndexes.size() + unsupportedIndexes.size();
    }

    public int getSupportedCount() {
      return supportedIndexes.size();
    }

    public int getUnsupportedCount() {
      return unsupportedIndexes.size();
    }

    public double getSupportedPercentage() {
      int total = getTotalIndexes();
      return total > 0 ? (double) getSupportedCount() / total * 100.0 : 0.0;
    }

    /**
     * Gets a summary of the conversion results.
     */
    public String getSummary() {
      return String.format("Index Conversion: %d total, %d supported (%.1f%%), %d unsupported",
              getTotalIndexes(), getSupportedCount(), getSupportedPercentage(), getUnsupportedCount());
    }
  }
}