package me.christianrobert.ora2postgre.plsql.ast.tools.managers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.ConstraintMetadata;
import me.christianrobert.ora2postgre.plsql.ast.tools.strategies.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manager class that orchestrates Oracle to PostgreSQL constraint conversion using different strategies.
 * Handles strategy registration, selection, dependency ordering, and conversion coordination.
 */
public class ConstraintTransformationManager {
    
    private static final Logger log = LoggerFactory.getLogger(ConstraintTransformationManager.class);
    
    private final List<ConstraintTransformationStrategy> strategies;
    private final Map<String, ConstraintTransformationStrategy> strategyByType;
    
    /**
     * Constructor that initializes all available strategies.
     */
    public ConstraintTransformationManager() {
        this.strategies = new ArrayList<>();
        this.strategyByType = new HashMap<>();
        
        // Register strategies in priority order (highest priority first)
        registerStrategy(new PrimaryKeyConstraintStrategy());
        registerStrategy(new UniqueConstraintStrategy());
        registerStrategy(new CheckConstraintStrategy());
        registerStrategy(new ForeignKeyConstraintStrategy());
        
        log.info("Initialized ConstraintTransformationManager with {} strategies", strategies.size());
    }
    
    /**
     * Registers a new strategy with the manager.
     * Strategies are automatically sorted by priority.
     */
    public void registerStrategy(ConstraintTransformationStrategy strategy) {
        if (strategy != null) {
            strategies.add(strategy);
            strategyByType.put(strategy.getConstraintType(), strategy);
            // Sort by priority (descending - highest priority first)
            strategies.sort((s1, s2) -> Integer.compare(s2.getPriority(), s1.getPriority()));
            log.debug("Registered strategy: {} (priority: {})", strategy.getStrategyName(), strategy.getPriority());
        }
    }
    
    /**
     * Finds the most appropriate strategy for the given constraint.
     * 
     * @param constraint The constraint metadata to find a strategy for
     * @return The best matching strategy, or null if none found
     */
    public ConstraintTransformationStrategy selectStrategy(ConstraintMetadata constraint) {
        log.debug("Selecting strategy for constraint {}", constraint.getConstraintName());
        
        // Find the first strategy that supports this constraint (strategies are sorted by priority)
        for (ConstraintTransformationStrategy strategy : strategies) {
            if (strategy.supports(constraint)) {
                log.debug("Selected strategy: {} for constraint {}", 
                         strategy.getStrategyName(), constraint.getConstraintName());
                return strategy;
            }
        }
        
        log.warn("No strategy found for constraint {} of type {}", 
                constraint.getConstraintName(), constraint.getConstraintType());
        return null;
    }
    
    /**
     * Transforms the given constraint using the most appropriate strategy.
     * 
     * @param constraint The constraint metadata to transform
     * @param context The global context containing all migration data
     * @return PostgreSQL constraint DDL without ALTER TABLE wrapper
     */
    public String transformConstraintDDL(ConstraintMetadata constraint, Everything context) {
        ConstraintTransformationStrategy strategy = selectStrategy(constraint);
        
        if (strategy == null) {
            throw new RuntimeException("No strategy found for constraint " + constraint.getConstraintName());
        }
        
        try {
            String result = strategy.transformConstraintDDL(constraint, context);
            log.debug("Successfully transformed constraint {} using {} strategy", 
                     constraint.getConstraintName(), strategy.getStrategyName());
            return result;
        } catch (Exception e) {
            log.error("Error transforming constraint {} with strategy {}: {}", 
                     constraint.getConstraintName(), strategy.getStrategyName(), e.getMessage());
            throw new RuntimeException("Failed to transform constraint " + constraint.getConstraintName(), e);
        }
    }
    
    /**
     * Transforms the given constraint into a complete ALTER TABLE statement.
     * 
     * @param constraint The constraint metadata to transform
     * @param schemaName Schema name for the table
     * @param tableName Table name for the constraint
     * @param context The global context containing all migration data
     * @return Complete PostgreSQL ALTER TABLE statement
     */
    public String transformAlterTableDDL(ConstraintMetadata constraint, String schemaName, String tableName, Everything context) {
        ConstraintTransformationStrategy strategy = selectStrategy(constraint);
        
        if (strategy == null) {
            throw new RuntimeException("No strategy found for constraint " + constraint.getConstraintName());
        }
        
        try {
            String result = strategy.transformAlterTableDDL(constraint, schemaName, tableName, context);
            log.debug("Successfully transformed constraint {} into ALTER TABLE statement using {} strategy", 
                     constraint.getConstraintName(), strategy.getStrategyName());
            return result;
        } catch (Exception e) {
            log.error("Error transforming constraint {} into ALTER TABLE with strategy {}: {}", 
                     constraint.getConstraintName(), strategy.getStrategyName(), e.getMessage());
            throw new RuntimeException("Failed to transform constraint " + constraint.getConstraintName(), e);
        }
    }
    
    /**
     * Validates dependencies for a constraint if the strategy requires it.
     * 
     * @param constraint The constraint to validate
     * @param context The global context containing all migration data
     * @return true if dependencies are satisfied or validation not required
     */
    public boolean validateDependencies(ConstraintMetadata constraint, Everything context) {
        ConstraintTransformationStrategy strategy = selectStrategy(constraint);
        
        if (strategy == null) {
            return false;
        }
        
        if (!strategy.requiresDependencyValidation()) {
            return true;
        }
        
        return strategy.validateDependencies(constraint, context);
    }
    
    /**
     * Gets all constraints of a specific type, sorted by priority.
     * 
     * @param constraints List of constraints to filter
     * @param constraintType Oracle constraint type ("P", "R", "U", "C")
     * @return List of constraints of the specified type
     */
    public List<ConstraintMetadata> getConstraintsByType(List<ConstraintMetadata> constraints, String constraintType) {
        return constraints.stream()
                .filter(constraint -> constraintType.equals(constraint.getConstraintType()))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all constraints in dependency order (primary keys first, foreign keys last).
     * 
     * @param constraints List of constraints to order
     * @return List of constraints in dependency order
     */
    public List<ConstraintMetadata> getConstraintsInDependencyOrder(List<ConstraintMetadata> constraints) {
        List<ConstraintMetadata> ordered = new ArrayList<>();
        
        // Add constraints in dependency order
        ordered.addAll(getConstraintsByType(constraints, ConstraintMetadata.PRIMARY_KEY));
        ordered.addAll(getConstraintsByType(constraints, ConstraintMetadata.UNIQUE));
        ordered.addAll(getConstraintsByType(constraints, ConstraintMetadata.CHECK));
        ordered.addAll(getConstraintsByType(constraints, ConstraintMetadata.FOREIGN_KEY));
        
        return ordered;
    }
    
    /**
     * Gets all registered strategies.
     * 
     * @return Immutable list of all registered strategies
     */
    public List<ConstraintTransformationStrategy> getStrategies() {
        return Collections.unmodifiableList(strategies);
    }
    
    /**
     * Gets a strategy by constraint type.
     * 
     * @param constraintType Oracle constraint type
     * @return Strategy for the constraint type, or null if not found
     */
    public ConstraintTransformationStrategy getStrategyByType(String constraintType) {
        return strategyByType.get(constraintType);
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