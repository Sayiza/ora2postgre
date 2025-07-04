package me.christianrobert.ora2postgre.transfer.progress;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe progress tracker for data transfer operations.
 * Tracks overall progress across multiple tables and detailed progress for the current table.
 */
public class TransferProgress {
    
    private final String sessionId;
    private final long startTime;
    
    // Overall progress
    private final AtomicLong totalTables = new AtomicLong(0);
    private final AtomicLong completedTables = new AtomicLong(0);
    private final AtomicLong totalEstimatedRows = new AtomicLong(0);
    private final AtomicLong totalTransferredRows = new AtomicLong(0);
    
    // Current table progress
    private final AtomicReference<String> currentTable = new AtomicReference<>("");
    private final AtomicLong currentTableTotalRows = new AtomicLong(0);
    private final AtomicLong currentTableTransferredRows = new AtomicLong(0);
    private final AtomicReference<String> currentStatus = new AtomicReference<>("Initializing");
    
    public TransferProgress(String sessionId) {
        this.sessionId = sessionId;
        this.startTime = System.currentTimeMillis();
    }
    
    // Getters
    public String getSessionId() { return sessionId; }
    public long getStartTime() { return startTime; }
    public long getTotalTables() { return totalTables.get(); }
    public long getCompletedTables() { return completedTables.get(); }
    public long getTotalEstimatedRows() { return totalEstimatedRows.get(); }
    public long getTotalTransferredRows() { return totalTransferredRows.get(); }
    public String getCurrentTable() { return currentTable.get(); }
    public long getCurrentTableTotalRows() { return currentTableTotalRows.get(); }
    public long getCurrentTableTransferredRows() { return currentTableTransferredRows.get(); }
    public String getCurrentStatus() { return currentStatus.get(); }
    
    // Calculated progress values
    public double getOverallProgressPercent() {
        long total = totalEstimatedRows.get();
        if (total == 0) return 0.0;
        return (double) totalTransferredRows.get() / total * 100.0;
    }
    
    public double getCurrentTableProgressPercent() {
        long total = currentTableTotalRows.get();
        if (total == 0) return 0.0;
        return (double) currentTableTransferredRows.get() / total * 100.0;
    }
    
    public long getElapsedTimeMs() {
        return System.currentTimeMillis() - startTime;
    }
    
    public double getOverallTransferRateRowsPerSecond() {
        long elapsedMs = getElapsedTimeMs();
        if (elapsedMs <= 0) return 0.0;
        return (double) totalTransferredRows.get() / (elapsedMs / 1000.0);
    }
    
    public long getEstimatedRemainingTimeMs() {
        long transferred = totalTransferredRows.get();
        long total = totalEstimatedRows.get();
        long elapsed = getElapsedTimeMs();
        
        if (transferred <= 0 || total <= transferred || elapsed <= 0) {
            return 0;
        }
        
        double rate = (double) transferred / elapsed;
        return (long) ((total - transferred) / rate);
    }
    
    // Progress update methods
    public void initializeTransfer(long totalTables, long totalEstimatedRows) {
        this.totalTables.set(totalTables);
        this.totalEstimatedRows.set(totalEstimatedRows);
        this.currentStatus.set("Starting transfer");
    }
    
    public void startTable(String schemaName, String tableName, long estimatedRows) {
        this.currentTable.set(schemaName + "." + tableName);
        this.currentTableTotalRows.set(estimatedRows);
        this.currentTableTransferredRows.set(0);
        this.currentStatus.set("Transferring " + schemaName + "." + tableName);
    }
    
    public void updateCurrentTableProgress(long transferredRows) {
        this.currentTableTransferredRows.set(transferredRows);
    }
    
    public void completeTable(long actualRowsTransferred) {
        this.completedTables.incrementAndGet();
        this.totalTransferredRows.addAndGet(actualRowsTransferred);
        this.currentTableTransferredRows.set(actualRowsTransferred);
        
        // Update status
        long completed = completedTables.get();
        long total = totalTables.get();
        if (completed >= total) {
            this.currentStatus.set("Transfer completed");
        } else {
            this.currentStatus.set(String.format("Completed %d/%d tables", completed, total));
        }
    }
    
    public void updateStatus(String status) {
        this.currentStatus.set(status);
    }
    
    // Summary methods
    public boolean isCompleted() {
        return completedTables.get() >= totalTables.get() && totalTables.get() > 0;
    }
    
    public String getSummary() {
        return String.format(
            "Session %s: %d/%d tables (%.1f%%), %d/%d rows (%.1f%%), %.1f rows/sec, %s",
            sessionId,
            completedTables.get(), totalTables.get(), 
            (double) completedTables.get() / Math.max(1, totalTables.get()) * 100.0,
            totalTransferredRows.get(), totalEstimatedRows.get(), getOverallProgressPercent(),
            getOverallTransferRateRowsPerSecond(),
            currentStatus.get()
        );
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
}