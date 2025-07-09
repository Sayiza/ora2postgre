package me.christianrobert.ora2postgre.plsql.ast.tools.managers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.plsql.ast.tools.strategies.StandardTableStrategy;
import me.christianrobert.ora2postgre.plsql.ast.tools.strategies.TableTransformationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manager class that orchestrates Oracle to PostgreSQL table conversion using different strategies.
 * Handles strategy registration, selection, and conversion coordination.
 */
public class TableTransformationManager {
    
    private static final Logger log = LoggerFactory.getLogger(TableTransformationManager.class);
    
    private final List<TableTransformationStrategy> strategies;
    private final StandardTableStrategy standardStrategy;
    
    /**
     * Constructor that initializes all available strategies.
     */
    public TableTransformationManager() {
        this.strategies = new ArrayList<>();
        this.standardStrategy = new StandardTableStrategy();
        
        // Register strategies in priority order (highest priority first)
        registerStrategy(standardStrategy);
        
        log.info("Initialized TableTransformationManager with {} strategies", strategies.size());
    }
    
    /**
     * Registers a new strategy with the manager.
     * Strategies are automatically sorted by priority.
     */
    public void registerStrategy(TableTransformationStrategy strategy) {
        if (strategy != null) {
            strategies.add(strategy);
            // Sort by priority (descending - highest priority first)
            strategies.sort((s1, s2) -> Integer.compare(s2.getPriority(), s1.getPriority()));
            log.debug("Registered strategy: {} (priority: {})", strategy.getStrategyName(), strategy.getPriority());
        }
    }
    
    /**
     * Finds the most appropriate strategy for the given table.
     * 
     * @param table The table metadata to find a strategy for
     * @return The best matching strategy, or StandardTableStrategy as fallback
     */
    public TableTransformationStrategy selectStrategy(TableMetadata table) {
        log.debug("Selecting strategy for table {}.{}", table.getSchema(), table.getTableName());
        
        // Find the first strategy that supports this table (strategies are sorted by priority)
        for (TableTransformationStrategy strategy : strategies) {
            if (strategy.supports(table)) {
                log.debug("Selected strategy: {} for table {}.{}", 
                         strategy.getStrategyName(), table.getSchema(), table.getTableName());
                return strategy;
            }
        }
        
        // This should never happen since StandardTableStrategy supports all tables
        log.warn("No strategy found for table {}.{}, using StandardTableStrategy as fallback", 
                table.getSchema(), table.getTableName());
        return standardStrategy;
    }
    
    /**
     * Transforms the given table using the most appropriate strategy.
     * 
     * @param table The table metadata to transform
     * @param context The global context containing all migration data
     * @return List of PostgreSQL DDL statements for the table
     */
    public List<String> transform(TableMetadata table, Everything context) {
        TableTransformationStrategy strategy = selectStrategy(table);
        
        try {
            List<String> result = strategy.transform(table, context);
            log.debug("Successfully transformed table {}.{} using {} strategy", 
                     table.getSchema(), table.getTableName(), strategy.getStrategyName());
            return result;
        } catch (Exception e) {
            log.error("Error transforming table {}.{} with strategy {}: {}", 
                     table.getSchema(), table.getTableName(), strategy.getStrategyName(), e.getMessage());
            throw new RuntimeException("Failed to transform table " + table.getSchema() + "." + table.getTableName(), e);
        }
    }
    
    /**
     * Gets all registered strategies.
     * 
     * @return Immutable list of all registered strategies
     */
    public List<TableTransformationStrategy> getStrategies() {
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