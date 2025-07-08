package me.christianrobert.ora2postgre.indexes;

import me.christianrobert.ora2postgre.oracledb.IndexMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
     * Converts a list of Oracle indexes to PostgreSQL DDL.
     * Returns results separated by supported and unsupported indexes.
     */
    public IndexConversionResult convertIndexes(List<IndexMetadata> indexes) {
        List<PostgreSQLIndexDDL> supportedIndexes = new ArrayList<>();
        List<PostgreSQLIndexDDL> unsupportedIndexes = new ArrayList<>();
        Map<String, Integer> strategyUsageStats = new HashMap<>();
        
        log.info("Converting {} indexes to PostgreSQL", indexes.size());
        
        for (IndexMetadata index : indexes) {
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
        
        log.info("Index conversion completed: {} supported, {} unsupported", 
                supportedIndexes.size(), unsupportedIndexes.size());
        
        // Log strategy usage statistics
        strategyUsageStats.forEach((strategy, count) -> 
            log.info("Strategy '{}': {} indexes", strategy, count));
        
        return new IndexConversionResult(supportedIndexes, unsupportedIndexes, strategyUsageStats);
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