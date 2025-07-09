package me.christianrobert.ora2postgre.transfer;

import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.oracledb.ColumnMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Integration test demonstrating the enhanced data transfer progress tracking.
 * Shows how the DataTransferService progress callbacks integrate with migration job progress.
 */
public class DataTransferProgressIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(DataTransferProgressIntegrationTest.class);

  private List<String> progressMessages;
  private DataTransferService.ProgressCallback testCallback;

  @BeforeEach
  void setUp() {
    progressMessages = new ArrayList<>();

    // Create a test callback that captures progress messages
    testCallback = new DataTransferService.ProgressCallback() {
      @Override
      public void onTableStart(int tableIndex, int totalTables, String tableName) {
        String message = String.format("Starting table %d/%d: %s",
                tableIndex + 1, totalTables, tableName);
        progressMessages.add(message);
        log.info("PROGRESS: {}", message);
      }

      @Override
      public void onTableComplete(int tableIndex, int totalTables, String tableName,
                                  boolean success, long rowsTransferred) {
        String message = String.format("Completed table %d/%d: %s (%s, %,d rows)",
                tableIndex + 1, totalTables, tableName,
                success ? "SUCCESS" : "FAILED", rowsTransferred);
        progressMessages.add(message);
        log.info("PROGRESS: {}", message);
      }
    };
  }

  @Test
  public void testProgressCallbackIntegration() {
    log.info("=== Enhanced Data Transfer Progress Tracking Test ===");

    // Create sample table metadata
    List<TableMetadata> testTables = createSampleTables();

    log.info("Created {} sample tables for testing", testTables.size());

    // Simulate the progress callback workflow that would happen during real data transfer
    int totalTables = testTables.size();
    for (int i = 0; i < testTables.size(); i++) {
      TableMetadata table = testTables.get(i);
      String fullTableName = table.getSchema() + "." + table.getTableName();

      // Simulate table start
      testCallback.onTableStart(i, totalTables, fullTableName);

      // Simulate some processing time
      try {
        Thread.sleep(100); // 100ms delay to simulate transfer time
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      // Simulate table completion with mock row counts
      long mockRowsTransferred = (long) (Math.random() * 5000) + 100; // Random between 100-5100
      boolean success = Math.random() > 0.1; // 90% success rate

      testCallback.onTableComplete(i, totalTables, fullTableName, success, mockRowsTransferred);
    }

    // Verify progress tracking
    log.info("=== Progress Summary ===");
    log.info("Total progress messages captured: {}", progressMessages.size());
    log.info("Expected messages: {} (2 per table: start + complete)", testTables.size() * 2);

    // Display all captured progress messages
    for (int i = 0; i < progressMessages.size(); i++) {
      log.info("Message {}: {}", i + 1, progressMessages.get(i));
    }

    // Demonstrate how this integrates with the MigrationProgressService
    demonstrateMigrationProgressIntegration(testTables);
  }

  private void demonstrateMigrationProgressIntegration(List<TableMetadata> tables) {
    log.info("=== Migration Progress Service Integration Demo ===");

    // This shows how the callback would integrate with actual job progress tracking
    String mockJobId = "test-job-123";

    // Simulate the progress updates that would happen in Main.performDataTransferWithProgress
    for (int i = 0; i < tables.size(); i++) {
      String tableName = tables.get(i).getSchema() + "." + tables.get(i).getTableName();

      // Start progress (this is what our callback calls)
      logSimulatedProgressUpdate(mockJobId, i, tables.size(), tableName, true, 0L);

      // Complete progress (this is what our callback calls)
      long mockRows = (long) (Math.random() * 2000) + 50;
      logSimulatedProgressUpdate(mockJobId, i, tables.size(), tableName, false, mockRows);
    }
  }

  private void logSimulatedProgressUpdate(String jobId, int tableIndex, int totalTables,
                                          String tableName, boolean isStarting, long rowsTransferred) {
    // This simulates what MigrationProgressService.updateDynamicDataTransferProgress would do
    double stepProgress = isStarting ?
            (double) tableIndex / totalTables :
            (double) (tableIndex + 1) / totalTables;

    String action = isStarting ? "Starting" : "Completed";
    String details = isStarting ?
            String.format("%s table %d of %d: %s", action, tableIndex + 1, totalTables, tableName) :
            String.format("%s table %d of %d: %s (%,d rows)", action, tableIndex + 1, totalTables, tableName, rowsTransferred);

    log.info("Job {}: {} (Step Progress: {}%)", jobId, details, Math.round(stepProgress * 100));
  }

  private List<TableMetadata> createSampleTables() {
    List<TableMetadata> tables = new ArrayList<>();

    // Create various types of sample tables to demonstrate progress tracking
    tables.add(createSampleTable("HR", "EMPLOYEES", 4));
    tables.add(createSampleTable("HR", "DEPARTMENTS", 2));
    tables.add(createSampleTable("SALES", "ORDERS", 6));
    tables.add(createSampleTable("SALES", "ORDER_ITEMS", 3));
    tables.add(createSampleTable("INVENTORY", "PRODUCTS", 5));

    return tables;
  }

  private TableMetadata createSampleTable(String schema, String tableName, int columnCount) {
    // Create table with the correct constructor
    TableMetadata table = new TableMetadata(schema, tableName);

    // Add an ID column
    ColumnMetadata idColumn = new ColumnMetadata("ID", "NUMBER", null, 22, 0, false, null);
    table.addColumn(idColumn);

    // Add some sample columns
    for (int i = 1; i < columnCount; i++) {
      ColumnMetadata column = new ColumnMetadata("COL_" + i, "VARCHAR2", 100, null, null, true, null);
      table.addColumn(column);
    }

    return table;
  }
}