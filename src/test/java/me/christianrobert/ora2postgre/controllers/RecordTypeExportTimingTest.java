package me.christianrobert.ora2postgre.controllers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.RecordTypeCollectionManager;
import me.christianrobert.ora2postgre.services.TransformationContext;
import me.christianrobert.ora2postgre.writing.ExportRecordType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify that record type collection and export timing works correctly.
 * This test simulates the actual MigrationController flow to ensure record types are 
 * collected during transformation and exported properly.
 */
public class RecordTypeExportTimingTest {

  @TempDir
  Path tempDir;

  private TransformationContext transformationContext;
  private Everything data;

  @BeforeEach
  public void setUp() {
    transformationContext = new TransformationContext();
    TransformationContext.setTestInstance(transformationContext);
    data = new Everything();
    
    // Clear any previous collections (simulating export phase start)
    RecordTypeCollectionManager.clear();
  }

  @Test
  public void testRecordTypeCollectionAndExportTiming() throws IOException {
    // Verify no record types are collected initially
    assertEquals(0, RecordTypeCollectionManager.getCollectedCount());
    
    // Create a function with record types
    Function function = createTestFunction();
    
    // Simulate export phase: transformation happens during toPostgre() calls
    // This is when record types should be collected
    String postgresSQL = function.toPostgre(data, false);
    
    // Verify record types were collected during transformation
    assertEquals(1, RecordTypeCollectionManager.getCollectedCount());
    assertNotNull(postgresSQL);
    assertTrue(postgresSQL.contains("-- Using schema-level composite type:"));
    
    // Now export record types (this should work because they were collected)
    ExportRecordType.saveRecordTypesToPostgre(tempDir.toString(), data);
    
    // Verify files were created
    Path schemaDir = tempDir.resolve("test_schema").resolve("step2brecordtypes");
    assertTrue(Files.exists(schemaDir));
    
    Path recordTypeFile = schemaDir.resolve("test_schema_record_types.sql");
    assertTrue(Files.exists(recordTypeFile));
    
    // Verify file contents
    String content = Files.readString(recordTypeFile);
    assertNotNull(content);
    assertFalse(content.isEmpty());
    assertTrue(content.contains("CREATE TYPE"));
    assertTrue(content.contains("test_schema_test_pkg_test_function_employee_rec"));
    
    // Verify export summary
    String summary = ExportRecordType.getExportSummary();
    assertTrue(summary.contains("Exported"));
    assertTrue(summary.contains("record types"));
  }

  @Test
  public void testMultipleComponentsCollectionTiming() throws IOException {
    // Test that multiple components can collect record types in correct order
    
    // Create components with record types
    Function function = createTestFunction();
    Procedure procedure = createTestProcedure();
    OraclePackage pkg = createTestPackage();
    
    // Simulate export phase transformations in typical order
    String functionSQL = function.toPostgre(data, false);
    String procedureSQL = procedure.toPostgre(data, false);
    String packageSQL = pkg.toPostgre(data, true); // spec only for package
    
    // Verify all record types were collected
    assertEquals(4, RecordTypeCollectionManager.getCollectedCount()); // 1 + 1 + 2 = 4
    
    // Now export record types
    ExportRecordType.saveRecordTypesToPostgre(tempDir.toString(), data);
    
    // Verify export worked
    Path recordTypeFile = tempDir.resolve("test_schema").resolve("step2brecordtypes")
                                 .resolve("test_schema_record_types.sql");
    assertTrue(Files.exists(recordTypeFile));
    
    String content = Files.readString(recordTypeFile);
    
    // Verify all record types are in the output
    assertTrue(content.contains("test_schema_test_pkg_test_function_employee_rec"));
    assertTrue(content.contains("test_schema_test_pkg_test_procedure_salary_rec"));
    assertTrue(content.contains("test_schema_test_pkg_employee_record"));
    assertTrue(content.contains("test_schema_test_pkg_department_record"));
  }

  @Test
  public void testClearAndReCollectTiming() throws IOException {
    // Test that clearing works properly between runs
    
    // First run
    Function function1 = createTestFunction();
    function1.toPostgre(data, false);
    assertEquals(1, RecordTypeCollectionManager.getCollectedCount());
    
    // Simulate new export run - clear should happen
    RecordTypeCollectionManager.clear();
    assertEquals(0, RecordTypeCollectionManager.getCollectedCount());
    
    // Second run with different function
    Function function2 = createAnotherTestFunction();
    function2.toPostgre(data, false);
    assertEquals(1, RecordTypeCollectionManager.getCollectedCount());
    
    // Export should only contain the second function's record types
    ExportRecordType.saveRecordTypesToPostgre(tempDir.toString(), data);
    
    Path recordTypeFile = tempDir.resolve("test_schema").resolve("step2brecordtypes")
                                 .resolve("test_schema_record_types.sql");
    assertTrue(Files.exists(recordTypeFile));
    
    String content = Files.readString(recordTypeFile);
    assertTrue(content.contains("test_schema_test_pkg_another_function_employee_rec"));
    // Should NOT contain the first function's record type
    assertFalse(content.contains("test_schema_test_pkg_test_function_employee_rec"));
  }

  // Helper methods to create test objects

  private Function createTestFunction() {
    OraclePackage parentPackage = new OraclePackage("test_pkg", "TEST_SCHEMA", null, null, null, 
                                                    null, null, null, null, null, null, null);
    Function function = new Function("test_function", Arrays.asList(), Arrays.asList(), "VARCHAR2", Arrays.asList());
    function.setSchema("TEST_SCHEMA");
    function.setParentPackage(parentPackage);
    
    RecordType.RecordField field1 = new RecordType.RecordField(
        "emp_id", 
        new DataTypeSpec("NUMBER", null, null, null), 
        false, 
        null
    );
    RecordType.RecordField field2 = new RecordType.RecordField(
        "emp_name", 
        new DataTypeSpec("VARCHAR2(100)", null, null, null), 
        false, 
        null
    );
    
    RecordType recordType = new RecordType("employee_rec", Arrays.asList(field1, field2));
    function.setRecordTypes(Arrays.asList(recordType));
    
    return function;
  }

  private Function createAnotherTestFunction() {
    OraclePackage parentPackage = new OraclePackage("test_pkg", "TEST_SCHEMA", null, null, null, 
                                                    null, null, null, null, null, null, null);
    Function function = new Function("another_function", Arrays.asList(), Arrays.asList(), "VARCHAR2", Arrays.asList());
    function.setSchema("TEST_SCHEMA");
    function.setParentPackage(parentPackage);
    
    RecordType.RecordField field = new RecordType.RecordField(
        "emp_id", 
        new DataTypeSpec("NUMBER", null, null, null), 
        false, 
        null
    );
    
    RecordType recordType = new RecordType("employee_rec", Arrays.asList(field));
    function.setRecordTypes(Arrays.asList(recordType));
    
    return function;
  }

  private Procedure createTestProcedure() {
    OraclePackage parentPackage = new OraclePackage("test_pkg", "TEST_SCHEMA", null, null, null, 
                                                    null, null, null, null, null, null, null);
    Procedure procedure = new Procedure("test_procedure", Arrays.asList(), Arrays.asList(), Arrays.asList());
    procedure.setSchema("TEST_SCHEMA");
    procedure.setParentPackage(parentPackage);
    
    RecordType.RecordField field = new RecordType.RecordField(
        "salary", 
        new DataTypeSpec("NUMBER(10,2)", null, null, null), 
        false, 
        null
    );
    
    RecordType recordType = new RecordType("salary_rec", Arrays.asList(field));
    procedure.setRecordTypes(Arrays.asList(recordType));
    
    return procedure;
  }

  private OraclePackage createTestPackage() {
    RecordType.RecordField empField = new RecordType.RecordField(
        "emp_id", 
        new DataTypeSpec("NUMBER", null, null, null), 
        false, 
        null
    );
    RecordType empRecord = new RecordType("employee_record", Arrays.asList(empField));
    
    RecordType.RecordField deptField = new RecordType.RecordField(
        "dept_id", 
        new DataTypeSpec("NUMBER", null, null, null), 
        false, 
        null
    );
    RecordType deptRecord = new RecordType("department_record", Arrays.asList(deptField));
    
    OraclePackage pkg = new OraclePackage("test_pkg", "TEST_SCHEMA", null, null, null, 
                                         null, Arrays.asList(empRecord, deptRecord), null, null, null, null, null);
    
    return pkg;
  }
}