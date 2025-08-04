package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.oracledb.ColumnMetadata;
import me.christianrobert.ora2postgre.oracledb.SynonymMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for CTE tests that provides proper metadata setup for Everything objects.
 * This ensures that table resolution works correctly during CTE testing.
 */
public class CTETestHelper {
  
  /**
   * Creates a properly configured Everything object with common test tables.
   * This is needed because CTE tests reference tables like 'employees' that must
   * exist in the metadata for schema resolution to work.
   */
  public static Everything createTestEverything() {
    Everything data = new Everything();
    setupTestData(data);
    return data;
  }

  /**
   * Sets up test data in an existing Everything object with common test tables.
   * This method is used when working with injected Everything instances in Quarkus tests.
   */
  public static void setupTestData(Everything data) {
    // Clear existing data first
    data.getUserNames().clear();
    data.getTableSql().clear();
    data.getSynonyms().clear();
    
    data.getUserNames().add("TEST_SCHEMA");
    
    // Add employees table metadata
    addEmployeesTable(data);
    
    // Add config_table metadata for other CTE tests
    addConfigTable(data);
  }
  
  /**
   * Adds employees table metadata to Everything object.
   * This table is commonly referenced in CTE tests.
   */
  private static void addEmployeesTable(Everything data) {
    TableMetadata employeesTable = new TableMetadata("TEST_SCHEMA", "EMPLOYEES");
    
    // Add columns that are referenced in CTE tests - use getColumns() to add to the existing list
    List<ColumnMetadata> columns = employeesTable.getColumns();
    
    ColumnMetadata employeeId = new ColumnMetadata("EMPLOYEE_ID", "NUMBER", null, null, null, false, null);
    columns.add(employeeId);
    
    ColumnMetadata departmentId = new ColumnMetadata("DEPARTMENT_ID", "NUMBER", null, null, null, false, null);
    columns.add(departmentId);
    
    ColumnMetadata salary = new ColumnMetadata("SALARY", "NUMBER", null, null, null, false, null);
    columns.add(salary);
    
    ColumnMetadata firstName = new ColumnMetadata("FIRST_NAME", "VARCHAR2", null, null, null, false, null);
    columns.add(firstName);
    
    ColumnMetadata managerId = new ColumnMetadata("MANAGER_ID", "NUMBER", null, null, null, false, null);
    columns.add(managerId);
    
    data.getTableSql().add(employeesTable);
    
    // Add synonym for employees table to enable schema resolution
    SynonymMetadata employeesSynonym = new SynonymMetadata("TEST_SCHEMA", "EMPLOYEES", "TEST_SCHEMA", "EMPLOYEES", "TABLE");
    data.getSynonyms().add(employeesSynonym);
  }
  
  /**
   * Adds config_table metadata to Everything object.
   * This table is used in some CTE tests.
   */
  private static void addConfigTable(Everything data) {
    TableMetadata configTable = new TableMetadata("TEST_SCHEMA", "CONFIG_TABLE");
    
    List<ColumnMetadata> columns = configTable.getColumns();
    
    ColumnMetadata configKey = new ColumnMetadata("CONFIG_KEY", "VARCHAR2", null, null, null, false, null);
    columns.add(configKey);
    
    ColumnMetadata status = new ColumnMetadata("STATUS", "VARCHAR2", null, null, null, false, null);
    columns.add(status);
    
    data.getTableSql().add(configTable);
    
    // Add synonym for config_table
    SynonymMetadata configSynonym = new SynonymMetadata("TEST_SCHEMA", "CONFIG_TABLE", "TEST_SCHEMA", "CONFIG_TABLE", "TABLE");
    data.getSynonyms().add(configSynonym);
  }
  
  /**
   * Creates Everything object with custom table metadata.
   * Use this for tests that need specific table configurations.
   */
  public static Everything createTestEverythingWithTable(String schemaName, String tableName, String... columnNames) {
    Everything data = new Everything();
    data.getUserNames().add(schemaName);
    
    TableMetadata table = new TableMetadata(schemaName, tableName.toUpperCase());
    
    List<ColumnMetadata> columns = table.getColumns();
    for (String columnName : columnNames) {
      ColumnMetadata column = new ColumnMetadata(columnName.toUpperCase(), "VARCHAR2", null, null, null, false, null);
      columns.add(column);
    }
    
    data.getTableSql().add(table);
    
    // Add synonym for table resolution
    SynonymMetadata synonym = new SynonymMetadata(schemaName, tableName.toUpperCase(), schemaName, tableName.toUpperCase(), "TABLE");
    data.getSynonyms().add(synonym);
    
    return data;
  }
}