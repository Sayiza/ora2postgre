package me.christianrobert.ora2postgre.transfer;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.Config;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.oracledb.RowCountConfig;
import me.christianrobert.ora2postgre.oracledb.SamplingRowCounter;
import me.christianrobert.ora2postgre.transfer.strategy.ObjectTypeMappingStrategy;
import me.christianrobert.ora2postgre.transfer.strategy.StreamingCsvStrategy;
import me.christianrobert.ora2postgre.transfer.strategy.TransferStrategy;
import me.christianrobert.ora2postgre.transfer.progress.TransferProgress;
import me.christianrobert.ora2postgre.transfer.progress.TransferResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main orchestrator for data transfer operations between Oracle and PostgreSQL.
 * 
 * Responsibilities:
 * - Analyze tables and select appropriate transfer strategies
 * - Coordinate transfer operations with progress tracking  
 * - Handle fallback to legacy SQL generation for complex tables
 * - Provide detailed transfer results and error reporting
 */
public class DataTransferService {
    
    private static final Logger log = LoggerFactory.getLogger(DataTransferService.class);
    
    private final List<TransferStrategy> availableStrategies;
    private final boolean enableFallback;
    
    /**
     * Callback interface for reporting data transfer progress.
     */
    public interface ProgressCallback {
        /**
         * Called when starting to transfer a table.
         * @param tableIndex 0-based index of current table
         * @param totalTables Total number of tables to transfer
         * @param tableName Full table name (schema.table)
         */
        void onTableStart(int tableIndex, int totalTables, String tableName);
        
        /**
         * Called when a table transfer completes.
         * @param tableIndex 0-based index of current table
         * @param totalTables Total number of tables to transfer
         * @param tableName Full table name (schema.table)
         * @param success Whether the transfer was successful
         * @param rowsTransferred Number of rows transferred (0 if failed)
         */
        void onTableComplete(int tableIndex, int totalTables, String tableName, boolean success, long rowsTransferred);
    }
    
    public DataTransferService() {
        this(true);
    }
    
    public DataTransferService(boolean enableFallback) {
        this.enableFallback = enableFallback;
        this.availableStrategies = initializeStrategies();
    }
    
    /**
     * Transfers data for all provided tables from Oracle to PostgreSQL.
     * 
     * @param tables List of table metadata to transfer
     * @param oracleConn Active Oracle database connection
     * @param postgresConn Active PostgreSQL database connection
     * @param everything The Everything context for object type and metadata lookups (may be null)
     * @param config The Config object for configuration settings (may be null)
     * @return DataTransferResults containing success status and detailed results for each table
     */
    public DataTransferResults transferTables(List<TableMetadata> tables, 
                                            Connection oracleConn, 
                                            Connection postgresConn,
                                            Everything everything,
                                            Config config) {
        return transferTables(tables, oracleConn, postgresConn, everything, config, null);
    }
    
    /**
     * Transfers data for all provided tables from Oracle to PostgreSQL with progress callback.
     * 
     * @param tables List of table metadata to transfer
     * @param oracleConn Active Oracle database connection
     * @param postgresConn Active PostgreSQL database connection
     * @param everything The Everything context for object type and metadata lookups (may be null)
     * @param config The Config object for configuration settings (may be null)
     * @param progressCallback Optional callback for progress updates (may be null)
     * @return DataTransferResults containing success status and detailed results for each table
     */
    public DataTransferResults transferTables(List<TableMetadata> tables, 
                                            Connection oracleConn, 
                                            Connection postgresConn,
                                            Everything everything,
                                            Config config,
                                            ProgressCallback progressCallback) {
        
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        log.info("Starting data transfer session {} for {} tables", sessionId, tables.size());
        
        TransferProgress progress = new TransferProgress(sessionId);
        long totalEstimatedRows = estimateTotalRows(tables);
        progress.initializeTransfer(tables.size(), totalEstimatedRows);
        
        List<TransferResult> results = new ArrayList<>();
        
        // Process each table
        for (int tableIndex = 0; tableIndex < tables.size(); tableIndex++) {
            TableMetadata table = tables.get(tableIndex);
            String fullTableName = table.getSchema() + "." + table.getTableName();
            
            // Notify progress callback that we're starting this table
            if (progressCallback != null) {
                progressCallback.onTableStart(tableIndex, tables.size(), fullTableName);
            }
            
            try {
                // Use enhanced analysis if Everything context is available
                String analysis = everything != null ? 
                    TableAnalyzer.analyzeTableWithObjectTypes(table, everything) :
                    TableAnalyzer.analyzeTable(table);
                log.info("Analyzing table: {}", analysis);
                
                TransferStrategy strategy = selectStrategy(table, everything);
                if (strategy != null) {
                    log.info("Using {} strategy for {}.{}", 
                    strategy.getStrategyName(), table.getSchema(), table.getTableName());

                    TransferResult result = strategy.transferTable(table, oracleConn, postgresConn, progress, everything);
                    results.add(result);
                    
                    // Notify progress callback of completion
                    if (progressCallback != null) {
                        progressCallback.onTableComplete(tableIndex, tables.size(), fullTableName, 
                            result.isSuccessful(), result.getRowsTransferred());
                    }
                    
                    if (result.isSuccessful()) {
                        log.info("Transfer completed: {}", result.toString());
                    } else {
                        log.error("Transfer failed: {}", result.toString());
                        if (result.getException() != null) {
                            log.error("Exception details:", result.getException());
                        }
                    }
                } else {
                    // No strategy available - this is an error, migration should fail
                    String errorMessage = String.format("No transfer strategy can handle table %s.%s. "
                            + "This table contains data types that are not supported by any available strategy.", 
                            table.getSchema(), table.getTableName());
                    log.error(errorMessage);
                    
                    TransferResult failureResult = TransferResult.failure(
                        table.getSchema(), table.getTableName(), "No Strategy Available", 
                        errorMessage, null);
                    results.add(failureResult);
                    
                    // Notify progress callback of failure
                    if (progressCallback != null) {
                        progressCallback.onTableComplete(tableIndex, tables.size(), fullTableName, false, 0);
                    }
                }
                
            } catch (Exception e) {
                log.error("Unexpected error transferring table {}.{}: {}", 
                    table.getSchema(), table.getTableName(), e.getMessage(), e);
                
                TransferResult failureResult = TransferResult.failure(
                    table.getSchema(), table.getTableName(), "Unknown", 
                    "Unexpected error: " + e.getMessage(), e);
                results.add(failureResult);
                
                // Notify progress callback of failure
                if (progressCallback != null) {
                    progressCallback.onTableComplete(tableIndex, tables.size(), fullTableName, false, 0);
                }
            }
        }

        // All tables should now be handled by proper strategies
        // If any failures occurred, they are captured in the results list

        log.info("Data transfer session {} completed. Processed {} tables", sessionId, results.size());
        
        return new DataTransferResults(sessionId, results, progress);
    }
    
    private List<TransferStrategy> initializeStrategies() {
        List<TransferStrategy> strategies = new ArrayList<>();
        
        // Add unified object type and complex data strategy first (highest priority)
        // Handles tables with object types OR complex data types (or both)
        strategies.add(new ObjectTypeMappingStrategy());
        log.debug("Added ObjectTypeMappingStrategy (unified object types and complex data)");
        
        // Add CSV streaming strategy for simple tables (lowest priority)
        strategies.add(new StreamingCsvStrategy());
        log.debug("Added StreamingCsvStrategy");
        
        log.debug("Initialized {} transfer strategies", strategies.size());
        return strategies;
    }
    
    private TransferStrategy selectStrategy(TableMetadata table, Everything everything) {
        log.debug("Selecting strategy for {}.{} (available strategies: {})", 
            table.getSchema(), table.getTableName(), availableStrategies.size());
            
        for (TransferStrategy strategy : availableStrategies) {
            log.debug("  Checking strategy: {}", strategy.getStrategyName());
            if (strategy.canHandle(table, everything)) {
                log.debug("  ✓ Strategy {} can handle this table", strategy.getStrategyName());
                return strategy;
            } else {
                log.debug("  ✗ Strategy {} cannot handle this table", strategy.getStrategyName());
            }
        }
        
        log.debug("  No suitable strategy found for {}.{}", table.getSchema(), table.getTableName());
        return null; // No suitable strategy found
    }
    
    private long estimateTotalRows(List<TableMetadata> tables) {
        return estimateTotalRows(tables, null, null);
    }
    
    /**
     * Estimates total rows using improved logic based on table metadata and optional configuration.
     * 
     * @param tables list of tables to estimate
     * @param conn Oracle database connection (optional, for more accurate estimates)
     * @param config row count configuration (optional)
     * @return estimated total row count
     */
    private long estimateTotalRows(List<TableMetadata> tables, Connection conn, RowCountConfig config) {
        if (tables.isEmpty()) {
            return 0;
        }
        
        long totalEstimate = 0;
        
        for (TableMetadata table : tables) {
            long tableEstimate = estimateTableRows(table, conn, config);
            totalEstimate += tableEstimate;
            
            log.debug("Estimated rows for {}.{}: {}", table.getSchema(), table.getTableName(), tableEstimate);
        }
        
        log.info("Total estimated rows for {} tables: {}", tables.size(), totalEstimate);
        return totalEstimate;
    }
    
    /**
     * Estimates row count for a single table using available metadata and optional database connection.
     */
    private long estimateTableRows(TableMetadata table, Connection conn, RowCountConfig config) {
        try {
            // Try to use actual row counting if connection is available
            if (conn != null && config != null) {
                switch (config.method()) {
                    case EXACT_COUNT:
                        return SamplingRowCounter.getExactRowCount(conn, table.getSchema(), table.getTableName());
                    case SAMPLING:
                        return SamplingRowCounter.estimateRowCountBySampling(conn, table.getSchema(), table.getTableName(), config.samplingPercentage());
                    case HYBRID:
                        // For individual tables in transfer, use sampling for large tables
                        long segmentEstimate = SamplingRowCounter.estimateRowCountBySegmentSize(conn, table.getSchema(), table.getTableName());
                        if (segmentEstimate > config.samplingThreshold()) {
                            return SamplingRowCounter.estimateRowCountBySampling(conn, table.getSchema(), table.getTableName(), config.samplingPercentage());
                        } else {
                            return SamplingRowCounter.getExactRowCount(conn, table.getSchema(), table.getTableName());
                        }
                    default:
                        // Fall through to heuristic estimation
                        break;
                }
            }
            
            // Fallback to heuristic estimation based on table characteristics
            return estimateTableRowsHeuristic(table);
            
        } catch (Exception e) {
            log.warn("Failed to estimate row count for {}.{}: {}", table.getSchema(), table.getTableName(), e.getMessage());
            return estimateTableRowsHeuristic(table);
        }
    }
    
    /**
     * Estimates table rows using heuristic approach based on table metadata.
     */
    private long estimateTableRowsHeuristic(TableMetadata table) {
        // Use table name patterns to make educated guesses
        String tableName = table.getTableName().toLowerCase();
        
        // Configuration/reference tables - typically small
        if (tableName.contains("config") || tableName.contains("setting") || 
            tableName.contains("lookup") || tableName.contains("ref") ||
            tableName.startsWith("cfg_") || tableName.endsWith("_config")) {
            return 100;
        }
        
        // Log/audit tables - typically large
        if (tableName.contains("log") || tableName.contains("audit") || 
            tableName.contains("history") || tableName.contains("trace") ||
            tableName.startsWith("log_") || tableName.endsWith("_log") ||
            tableName.endsWith("_audit") || tableName.endsWith("_history")) {
            return 100000;
        }
        
        // Transaction/data tables - medium to large
        if (tableName.contains("transaction") || tableName.contains("order") ||
            tableName.contains("payment") || tableName.contains("invoice") ||
            tableName.contains("data") || tableName.startsWith("t_")) {
            return 10000;
        }
        
        // User/customer tables - medium size
        if (tableName.contains("user") || tableName.contains("customer") ||
            tableName.contains("account") || tableName.contains("person")) {
            return 5000;
        }
        
        // Junction/mapping tables - small to medium
        if (tableName.contains("_") && (tableName.contains("map") || 
            tableName.matches(".*_[a-z]+_[a-z]+.*"))) { // Pattern like table_other_mapping
            return 1000;
        }
        
        // Default estimate for unknown tables
        return 2000;
    }
    
    /**
     * Updates the row estimation method to use improved logic with database connection.
     * 
     * @param tables list of tables
     * @param conn Oracle database connection
     * @param config row count configuration
     * @return improved row count estimate
     * @deprecated This method name is misleading, use estimateTotalRows instead
     */
    @Deprecated
    public long estimateTotalRowsWithConnection(List<TableMetadata> tables, Connection conn, RowCountConfig config) {
        return estimateTotalRows(tables, conn, config);
    }
    
    /**
     * Container class for data transfer results and progress information.
     */
    public static class DataTransferResults {
        private final String sessionId;
        private final List<TransferResult> tableResults;
        private final TransferProgress finalProgress;
        
        public DataTransferResults(String sessionId, List<TransferResult> tableResults, TransferProgress finalProgress) {
            this.sessionId = sessionId;
            this.tableResults = tableResults;
            this.finalProgress = finalProgress;
        }
        
        public String getSessionId() { return sessionId; }
        public List<TransferResult> getTableResults() { return tableResults; }
        public TransferProgress getFinalProgress() { return finalProgress; }
        
        public boolean isOverallSuccess() {
            return tableResults.stream().allMatch(TransferResult::isSuccessful);
        }
        
        public long getTotalRowsTransferred() {
            return tableResults.stream().mapToLong(TransferResult::getRowsTransferred).sum();
        }
        
        public long getTotalTransferTimeMs() {
            return tableResults.stream().mapToLong(TransferResult::getTransferTimeMs).sum();
        }
        
        public List<TransferResult> getFailedTables() {
            return tableResults.stream()
                .filter(result -> !result.isSuccessful())
                .toList();
        }
        
        public List<TransferResult> getSuccessfulTables() {
            return tableResults.stream()
                .filter(TransferResult::isSuccessful)
                .toList();
        }
        
        public String getSummary() {
            long successful = getSuccessfulTables().size();
            long failed = getFailedTables().size();
            long totalRows = getTotalRowsTransferred();
            long totalTime = getTotalTransferTimeMs();
            
            return String.format(
                "Session %s: %d/%d tables successful, %d rows transferred in %d ms",
                sessionId, successful, successful + failed, totalRows, totalTime);
        }
        
        @Override
        public String toString() {
            return getSummary();
        }
    }
}