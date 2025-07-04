package me.christianrobert.ora2postgre.oracledb;

/**
 * Enumeration of different row count estimation methods for Oracle tables.
 * Each method provides different trade-offs between accuracy and performance.
 */
public enum RowCountMethod {
    /**
     * Uses Oracle's all_tables.num_rows statistics.
     * Fast but may be inaccurate if statistics are stale.
     */
    STATISTICS_ONLY,
    
    /**
     * Executes SELECT COUNT(*) for exact counts.
     * Slow but completely accurate, suitable for smaller tables.
     */
    EXACT_COUNT,
    
    /**
     * Uses Oracle's SAMPLE clause to estimate row counts.
     * Good balance of speed and accuracy for large tables.
     */
    SAMPLING,
    
    /**
     * Combines statistics validation with sampling fallback.
     * Uses statistics when fresh, falls back to sampling when stale.
     */
    HYBRID
}