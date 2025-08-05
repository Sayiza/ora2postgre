package me.christianrobert.ora2postgre.writing;

import me.christianrobert.ora2postgre.config.ConfigurationService;
import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.RecordTypeCollectionManager;
import me.christianrobert.ora2postgre.services.TransformationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Test class for ExportRecordType functionality.
 * Tests record type export, file generation, and configuration integration.
 */
public class ExportRecordTypeTest {

  @TempDir
  Path tempDir;

  @Mock
  private ConfigurationService configurationService;

  private TransformationContext transformationContext;
  private Everything data;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    transformationContext = new TransformationContext();
    TransformationContext.setTestInstance(transformationContext);
    data = new Everything();
    
    // Clear any previous collections
    RecordTypeCollectionManager.clear();
  }

  @Test
  public void testSaveRecordTypesToPostgre() throws IOException {
    // Create and collect some record types
    setupTestRecordTypes();
    
    // Export to temp directory
    ExportRecordType.saveRecordTypesToPostgre(tempDir.toString(), data);
    
    // Verify directory structure was created
    Path testschemaDir = tempDir.resolve("test_schema").resolve("step2brecordtypes");
    assertTrue(Files.exists(testschemaDir));
    
    // Verify file was created
    Path recordTypeFile = testschemaDir.resolve("test_schema_record_types.sql");
    assertTrue(Files.exists(recordTypeFile));
    
    // Verify file contents
    String content = Files.readString(recordTypeFile);
    assertNotNull(content);
    assertFalse(content.isEmpty());
    
    // Check for expected PostgreSQL composite type definitions
    assertTrue(content.contains("CREATE TYPE"));
    assertTrue(content.contains("test_schema_test_pkg_test_function_employee_rec"));
    assertTrue(content.contains("test_schema_test_pkg_employee_record"));
    assertTrue(content.contains("Schema: TEST_SCHEMA"));
    assertTrue(content.contains("emp_id"));
    assertTrue(content.contains("emp_name"));
    
    // Check file header
    assertTrue(content.contains("PostgreSQL Composite Types for Schema"));
    assertTrue(content.contains("Generated from Oracle block-level record types"));
  }

  @Test
  public void testNoRecordTypesCollected() throws IOException {
    // Don't collect any record types
    
    // Export to temp directory
    ExportRecordType.saveRecordTypesToPostgre(tempDir.toString(), data);
    
    // Verify no directories or files were created
    assertFalse(Files.exists(tempDir.resolve("test_schema")));
  }

  @Test
  public void testMultipleSchemas() throws IOException {
    // Create record types for multiple schemas
    setupTestRecordTypesMultipleSchemas();
    
    // Export to temp directory
    ExportRecordType.saveRecordTypesToPostgre(tempDir.toString(), data);
    
    // Verify both schemas have directories and files
    Path schema1Dir = tempDir.resolve("test_schema1").resolve("step2brecordtypes");
    Path schema2Dir = tempDir.resolve("test_schema2").resolve("step2brecordtypes");
    
    assertTrue(Files.exists(schema1Dir));
    assertTrue(Files.exists(schema2Dir));
    
    Path file1 = schema1Dir.resolve("test_schema1_record_types.sql");
    Path file2 = schema2Dir.resolve("test_schema2_record_types.sql");
    
    assertTrue(Files.exists(file1));
    assertTrue(Files.exists(file2));
    
    // Verify content is schema-specific
    String content1 = Files.readString(file1);
    String content2 = Files.readString(file2);
    
    assertTrue(content1.contains("test_schema1_pkg1_function1_rec1"));
    assertTrue(content2.contains("test_schema2_pkg2_function2_rec2"));
    assertFalse(content1.contains("test_schema2"));
    assertFalse(content2.contains("test_schema1"));
  }

  @Test
  public void testIsRecordTypeExportNeeded() {
    // Test with triggers enabled
    when(configurationService.isDoTriggers()).thenReturn(true);
    when(configurationService.isDoPackageBody()).thenReturn(false);
    when(configurationService.isDoStandaloneFunctions()).thenReturn(false);
    when(configurationService.isDoStandaloneProcedures()).thenReturn(false);
    
    assertTrue(ExportRecordType.isRecordTypeExportNeeded(configurationService));
    
    // Test with package body enabled
    when(configurationService.isDoTriggers()).thenReturn(false);
    when(configurationService.isDoPackageBody()).thenReturn(true);
    
    assertTrue(ExportRecordType.isRecordTypeExportNeeded(configurationService));
    
    // Test with standalone functions enabled
    when(configurationService.isDoPackageBody()).thenReturn(false);
    when(configurationService.isDoStandaloneFunctions()).thenReturn(true);
    
    assertTrue(ExportRecordType.isRecordTypeExportNeeded(configurationService));
    
    // Test with standalone procedures enabled
    when(configurationService.isDoStandaloneFunctions()).thenReturn(false);
    when(configurationService.isDoStandaloneProcedures()).thenReturn(true);
    
    assertTrue(ExportRecordType.isRecordTypeExportNeeded(configurationService));
    
    // Test with all disabled
    when(configurationService.isDoStandaloneProcedures()).thenReturn(false);
    
    assertFalse(ExportRecordType.isRecordTypeExportNeeded(configurationService));
  }

  @Test
  public void testGetStepName() {
    assertEquals("step2brecordtypes", ExportRecordType.getStepName());
  }

  @Test
  public void testGetExportSummary() {
    // Test with no record types
    String summary = ExportRecordType.getExportSummary();
    assertTrue(summary.contains("No record types collected"));
    
    // Test with collected record types
    setupTestRecordTypes();
    
    summary = ExportRecordType.getExportSummary();
    assertTrue(summary.contains("Exported"));
    assertTrue(summary.contains("record types"));
    assertTrue(summary.contains("schemas"));
  }

  @Test
  public void testRecordTypeFieldsGeneration() throws IOException {
    // Create record type with various field types
    setupComplexRecordType();
    
    // Export to temp directory
    ExportRecordType.saveRecordTypesToPostgre(tempDir.toString(), data);
    
    // Verify file was created
    Path recordTypeFile = tempDir.resolve("test_schema").resolve("step2brecordtypes")
                                  .resolve("test_schema_record_types.sql");
    assertTrue(Files.exists(recordTypeFile));
    
    // Verify field types are properly converted
    String content = Files.readString(recordTypeFile);
    assertTrue(content.contains("emp_id"));
    assertTrue(content.contains("emp_name"));
    assertTrue(content.contains("hire_date"));
    assertTrue(content.contains("salary"));
    
    // Check for PostgreSQL data types (should be converted from Oracle types) 
    assertTrue(content.contains("numeric") || content.contains("NUMERIC") || 
               content.contains("integer") || content.contains("INTEGER") ||
               content.contains("NUMBER") || content.contains("number"));
    assertTrue(content.contains("text") || content.contains("TEXT") || 
               content.contains("varchar") || content.contains("VARCHAR"));
  }

  // Helper methods to create test data

  private void setupTestRecordTypes() {
    // Create function with record type
    Function function = createTestFunction();
    RecordTypeCollectionManager.collectFromFunction(function);
    
    // Create package with record types
    OraclePackage pkg = createTestPackage();
    RecordTypeCollectionManager.collectFromPackage(pkg);
  }

  private void setupTestRecordTypesMultipleSchemas() {
    // Schema 1
    Function function1 = createFunctionForSchema("TEST_SCHEMA1", "pkg1", "function1", "rec1");
    RecordTypeCollectionManager.collectFromFunction(function1);
    
    // Schema 2
    Function function2 = createFunctionForSchema("TEST_SCHEMA2", "pkg2", "function2", "rec2");
    RecordTypeCollectionManager.collectFromFunction(function2);
  }

  private void setupComplexRecordType() {
    OraclePackage parentPackage = new OraclePackage("complex_pkg", "TEST_SCHEMA", null, null, null, 
                                                    null, null, null, null, null, null, null);
    Function function = new Function("complex_function", null, null, "VARCHAR2", null);
    function.setSchema("TEST_SCHEMA");
    function.setParentPackage(parentPackage);
    
    // Create record type with various field types
    RecordType.RecordField empId = new RecordType.RecordField(
        "emp_id", 
        new DataTypeSpec("NUMBER(10)", null, null, null), 
        true,  // NOT NULL
        null
    );
    
    RecordType.RecordField empName = new RecordType.RecordField(
        "emp_name", 
        new DataTypeSpec("VARCHAR2(100)", null, null, null), 
        false, 
        null
    );
    
    RecordType.RecordField hireDate = new RecordType.RecordField(
        "hire_date", 
        new DataTypeSpec("DATE", null, null, null), 
        false, 
        null  // Default SYSDATE would be here in real scenario
    );
    
    RecordType.RecordField salary = new RecordType.RecordField(
        "salary", 
        new DataTypeSpec("NUMBER(10,2)", null, null, null), 
        false, 
        null
    );
    
    RecordType recordType = new RecordType("complex_employee_rec", 
                                          Arrays.asList(empId, empName, hireDate, salary));
    function.setRecordTypes(Arrays.asList(recordType));
    
    RecordTypeCollectionManager.collectFromFunction(function);
  }

  private Function createTestFunction() {
    OraclePackage parentPackage = new OraclePackage("test_pkg", "TEST_SCHEMA", null, null, null, 
                                                    null, null, null, null, null, null, null);
    Function function = new Function("test_function", null, null, "VARCHAR2", null);
    function.setSchema("TEST_SCHEMA");
    function.setParentPackage(parentPackage);
    
    // Create record type with fields
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

  private OraclePackage createTestPackage() {
    
    // Create record type
    RecordType.RecordField empField = new RecordType.RecordField(
        "emp_id", 
        new DataTypeSpec("NUMBER", null, null, null), 
        false, 
        null
    );
    RecordType empRecord = new RecordType("employee_record", Arrays.asList(empField));
    
    // Create package with record types
    OraclePackage pkg = new OraclePackage("test_pkg", "TEST_SCHEMA", null, null, null, 
                                         null, Arrays.asList(empRecord), null, null, null, null, null);
    
    return pkg;
  }

  private Function createFunctionForSchema(String schema, String packageName, 
                                         String functionName, String recordName) {
    OraclePackage parentPackage = new OraclePackage(packageName, schema, null, null, null, 
                                                    null, null, null, null, null, null, null);
    Function function = new Function(functionName, null, null, "VARCHAR2", null);
    function.setSchema(schema);
    function.setParentPackage(parentPackage);
    
    RecordType.RecordField field = new RecordType.RecordField(
        "test_field", 
        new DataTypeSpec("VARCHAR2(50)", null, null, null), 
        false, 
        null
    );
    
    RecordType recordType = new RecordType(recordName, Arrays.asList(field));
    function.setRecordTypes(Arrays.asList(recordType));
    
    return function;
  }
}