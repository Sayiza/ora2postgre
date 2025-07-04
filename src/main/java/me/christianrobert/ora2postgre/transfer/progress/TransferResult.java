package me.christianrobert.ora2postgre.transfer.progress;

/**
 * Represents the result of a single table transfer operation.
 * Contains success status, row counts, timing information, and error details.
 */
public class TransferResult {
    
    private final String schemaName;
    private final String tableName;
    private final boolean successful;
    private final long rowsTransferred;
    private final long sourceRowCount;
    private final long transferTimeMs;
    private final String strategyUsed;
    private final String errorMessage;
    private final Exception exception;
    
    private TransferResult(Builder builder) {
        this.schemaName = builder.schemaName;
        this.tableName = builder.tableName;
        this.successful = builder.successful;
        this.rowsTransferred = builder.rowsTransferred;
        this.sourceRowCount = builder.sourceRowCount;
        this.transferTimeMs = builder.transferTimeMs;
        this.strategyUsed = builder.strategyUsed;
        this.errorMessage = builder.errorMessage;
        this.exception = builder.exception;
    }
    
    // Getters
    public String getSchemaName() { return schemaName; }
    public String getTableName() { return tableName; }
    public boolean isSuccessful() { return successful; }
    public long getRowsTransferred() { return rowsTransferred; }
    public long getSourceRowCount() { return sourceRowCount; }
    public long getTransferTimeMs() { return transferTimeMs; }
    public String getStrategyUsed() { return strategyUsed; }
    public String getErrorMessage() { return errorMessage; }
    public Exception getException() { return exception; }
    
    public String getFullTableName() {
        return schemaName + "." + tableName;
    }
    
    public boolean hasRowCountMismatch() {
        return successful && sourceRowCount != rowsTransferred;
    }
    
    public double getTransferRateRowsPerSecond() {
        if (transferTimeMs <= 0) return 0.0;
        return (double) rowsTransferred / (transferTimeMs / 1000.0);
    }
    
    @Override
    public String toString() {
        if (successful) {
            return String.format("SUCCESS: %s - %d/%d rows in %dms using %s (%.1f rows/sec)", 
                getFullTableName(), rowsTransferred, sourceRowCount, transferTimeMs, 
                strategyUsed, getTransferRateRowsPerSecond());
        } else {
            return String.format("FAILED: %s - %s", getFullTableName(), errorMessage);
        }
    }
    
    // Builder pattern for easy construction
    public static class Builder {
        private String schemaName;
        private String tableName;
        private boolean successful = false;
        private long rowsTransferred = 0;
        private long sourceRowCount = 0;
        private long transferTimeMs = 0;
        private String strategyUsed;
        private String errorMessage;
        private Exception exception;
        
        public Builder(String schemaName, String tableName) {
            this.schemaName = schemaName;
            this.tableName = tableName;
        }
        
        public Builder successful(boolean successful) {
            this.successful = successful;
            return this;
        }
        
        public Builder rowsTransferred(long rowsTransferred) {
            this.rowsTransferred = rowsTransferred;
            return this;
        }
        
        public Builder sourceRowCount(long sourceRowCount) {
            this.sourceRowCount = sourceRowCount;
            return this;
        }
        
        public Builder transferTimeMs(long transferTimeMs) {
            this.transferTimeMs = transferTimeMs;
            return this;
        }
        
        public Builder strategyUsed(String strategyUsed) {
            this.strategyUsed = strategyUsed;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Builder exception(Exception exception) {
            this.exception = exception;
            this.errorMessage = exception.getMessage();
            return this;
        }
        
        public TransferResult build() {
            return new TransferResult(this);
        }
    }
    
    // Static factory methods for common cases
    public static TransferResult success(String schema, String table, long rowsTransferred, 
                                       long sourceRowCount, long transferTimeMs, String strategy) {
        return new Builder(schema, table)
                .successful(true)
                .rowsTransferred(rowsTransferred)
                .sourceRowCount(sourceRowCount)
                .transferTimeMs(transferTimeMs)
                .strategyUsed(strategy)
                .build();
    }
    
    public static TransferResult failure(String schema, String table, String strategy, 
                                       String errorMessage, Exception exception) {
        return new Builder(schema, table)
                .successful(false)
                .strategyUsed(strategy)
                .errorMessage(errorMessage)
                .exception(exception)
                .build();
    }
}