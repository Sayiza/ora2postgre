package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;

import java.util.List;

/**
 * Strategy interface for converting Oracle tables to PostgreSQL equivalents.
 * Different implementations can handle different types of Oracle tables or 
 * special cases in the transformation process.
 */
public interface TableTransformationStrategy {
    
    /**
     * Determines if this strategy can handle the given Oracle table.
     * 
     * @param table The Oracle table metadata to evaluate
     * @return true if this strategy can convert the table, false otherwise
     */
    boolean supports(TableMetadata table);
    
    /**
     * Converts an Oracle table to PostgreSQL DDL statements.
     * This method should only be called if supports() returns true.
     * 
     * @param table The Oracle table metadata to convert
     * @param context The global context containing all migration data
     * @return List of PostgreSQL DDL statements for the table
     * @throws UnsupportedOperationException if the table is not supported by this strategy
     */
    List<String> transform(TableMetadata table, Everything context);
    
    /**
     * Gets a human-readable name for this strategy.
     * Used for logging and debugging purposes.
     * 
     * @return Strategy name (e.g., "Standard Table", "Partitioned Table")
     */
    String getStrategyName();
    
    /**
     * Gets the priority of this strategy for selection.
     * Higher priority strategies are checked first.
     * This allows more specific strategies to take precedence over general ones.
     * 
     * @return Priority value (higher = checked first)
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * Gets additional information about the conversion process.
     * Can include warnings, notes, or recommendations for the converted table.
     * 
     * @param table The Oracle table being converted
     * @return Additional conversion information, or null if none
     */
    default String getConversionNotes(TableMetadata table) {
        return null;
    }
}