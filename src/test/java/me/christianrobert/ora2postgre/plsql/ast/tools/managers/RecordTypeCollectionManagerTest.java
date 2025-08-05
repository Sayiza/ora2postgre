package me.christianrobert.ora2postgre.plsql.ast.tools.managers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;
import me.christianrobert.ora2postgre.services.TransformationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RecordTypeCollectionManager functionality.
 * Tests collection, qualified naming, and PostgreSQL composite type generation.
 */
public class RecordTypeCollectionManagerTest {

  private TransformationContext transformationContext;
  private Everything data;

  @BeforeEach
  public void setUp() {
    transformationContext = new TransformationContext();
    TransformationContext.setTestInstance(transformationContext);
    data = new Everything();
    
    // Clear any previous collections
    RecordTypeCollectionManager.clear();
  }

  @Test
  public void testCollectFromFunction() {
    // Create a function with record types
    Function function = createTestFunction();
    
    // Collect record types
    RecordTypeCollectionManager.collectFromFunction(function);
    
    // Verify collection count
    assertEquals(1, RecordTypeCollectionManager.getCollectedCount());
    
    // Verify qualified name generation
    RecordType recordType = function.getRecordTypes().get(0);
    String qualifiedName = RecordTypeCollectionManager.getQualifiedName(function, recordType);
    assertEquals("test_schema_test_pkg_test_function_employee_rec", qualifiedName);
  }

  @Test
  public void testCollectFromProcedure() {
    // Create a procedure with record types
    Procedure procedure = createTestProcedure();
    
    // Collect record types
    RecordTypeCollectionManager.collectFromProcedure(procedure);
    
    // Verify collection count
    assertEquals(1, RecordTypeCollectionManager.getCollectedCount());
    
    // Verify qualified name generation
    RecordType recordType = procedure.getRecordTypes().get(0);
    String qualifiedName = RecordTypeCollectionManager.getQualifiedName(procedure, recordType);
    assertEquals("test_schema_test_pkg_test_procedure_salary_rec", qualifiedName);
  }

  @Test
  public void testCollectFromPackage() {
    // Create a package with record types
    OraclePackage pkg = createTestPackage();
    
    // Collect record types
    RecordTypeCollectionManager.collectFromPackage(pkg);
    
    // Verify collection count
    assertEquals(2, RecordTypeCollectionManager.getCollectedCount());
    
    // Verify qualified name generation for package-level record types
    RecordType recordType1 = pkg.getRecordTypes().get(0);
    String qualifiedName1 = RecordTypeCollectionManager.getQualifiedName(pkg, recordType1);
    assertEquals("test_schema_test_pkg_employee_record", qualifiedName1);
    
    RecordType recordType2 = pkg.getRecordTypes().get(1);
    String qualifiedName2 = RecordTypeCollectionManager.getQualifiedName(pkg, recordType2);
    assertEquals("test_schema_test_pkg_department_record", qualifiedName2);
  }

  @Test
  public void testMultipleCollections() {
    // Create multiple components with record types
    Function function = createTestFunction();
    Procedure procedure = createTestProcedure();
    OraclePackage pkg = createTestPackage();
    
    // Collect from all
    RecordTypeCollectionManager.collectFromFunction(function);
    RecordTypeCollectionManager.collectFromProcedure(procedure);
    RecordTypeCollectionManager.collectFromPackage(pkg);
    
    // Verify total collection count (1 + 1 + 2 = 4)
    assertEquals(4, RecordTypeCollectionManager.getCollectedCount());
    
    // Verify organization by schema
    Map<String, List<RecordTypeCollectionManager.RecordTypeInfo>> bySchema = 
        RecordTypeCollectionManager.getAllRecordTypesBySchema();
    
    assertEquals(1, bySchema.size());
    assertTrue(bySchema.containsKey("TEST_SCHEMA"));
    assertEquals(4, bySchema.get("TEST_SCHEMA").size());
  }

  @Test
  public void testNameConflictResolution() {
    // Create two functions with same record type name
    Function function1 = createTestFunction();
    Function function2 = createAnotherTestFunction();
    
    // Both have record type named "employee_rec"
    RecordTypeCollectionManager.collectFromFunction(function1);
    RecordTypeCollectionManager.collectFromFunction(function2);
    
    // Verify both are collected with different qualified names
    assertEquals(2, RecordTypeCollectionManager.getCollectedCount());
    
    String qualifiedName1 = RecordTypeCollectionManager.getQualifiedName(function1, 
        function1.getRecordTypes().get(0));
    String qualifiedName2 = RecordTypeCollectionManager.getQualifiedName(function2, 
        function2.getRecordTypes().get(0));
    
    // Names should be different due to conflict resolution
    assertNotEquals(qualifiedName1, qualifiedName2);
    assertTrue(qualifiedName1.contains("test_function"));
    assertTrue(qualifiedName2.contains("another_function"));
  }

  @Test
  public void testGenerateSchemaLevelTypes() {
    // Create components with record types
    Function function = createTestFunction();
    OraclePackage pkg = createTestPackage();
    
    // Collect record types
    RecordTypeCollectionManager.collectFromFunction(function);
    RecordTypeCollectionManager.collectFromPackage(pkg);
    
    // Generate PostgreSQL composite types
    String generatedSQL = RecordTypeCollectionManager.generateSchemaLevelTypes(data);
    
    // Verify output contains expected elements
    assertNotNull(generatedSQL);
    assertFalse(generatedSQL.isEmpty());
    assertTrue(generatedSQL.contains("CREATE TYPE"));
    assertTrue(generatedSQL.contains("test_schema_test_pkg_test_function_employee_rec"));
    assertTrue(generatedSQL.contains("test_schema_test_pkg_employee_record"));
    assertTrue(generatedSQL.contains("test_schema_test_pkg_department_record"));
    assertTrue(generatedSQL.contains("Schema: TEST_SCHEMA"));
  }

  @Test
  public void testClearCollection() {
    // Collect some record types
    Function function = createTestFunction();
    RecordTypeCollectionManager.collectFromFunction(function);
    
    assertEquals(1, RecordTypeCollectionManager.getCollectedCount());
    
    // Clear collection
    RecordTypeCollectionManager.clear();
    
    assertEquals(0, RecordTypeCollectionManager.getCollectedCount());
    
    // Verify generation returns empty string
    String generatedSQL = RecordTypeCollectionManager.generateSchemaLevelTypes(data);
    assertEquals("", generatedSQL);
  }

  @Test
  public void testEmptyCollections() {
    // Test with null/empty record type lists
    OraclePackage emptyPackage = new OraclePackage("empty_pkg", "TEST_SCHEMA", null, null, null, 
                                                   null, null, null, null, null, null, null);
    // Don't add any record types
    
    RecordTypeCollectionManager.collectFromPackage(emptyPackage);
    
    assertEquals(0, RecordTypeCollectionManager.getCollectedCount());
  }

  @Test
  public void testStandaloneFunctionAndProcedure() {
    // Create standalone function and procedure (no parent package)
    Function standaloneFunction = createStandaloneFunction();
    Procedure standaloneProcedure = createStandaloneProcedure();
    
    RecordTypeCollectionManager.collectFromFunction(standaloneFunction);
    RecordTypeCollectionManager.collectFromProcedure(standaloneProcedure);
    
    assertEquals(2, RecordTypeCollectionManager.getCollectedCount());
    
    // Verify qualified names for standalone components
    String functionQualifiedName = RecordTypeCollectionManager.getQualifiedName(
        standaloneFunction, standaloneFunction.getRecordTypes().get(0));
    String procedureQualifiedName = RecordTypeCollectionManager.getQualifiedName(
        standaloneProcedure, standaloneProcedure.getRecordTypes().get(0));
    
    // Should not contain package name (null package)
    assertEquals("test_schema_standalone_func_local_rec", functionQualifiedName);
    assertEquals("test_schema_standalone_proc_audit_rec", procedureQualifiedName);
  }

  // Helper methods to create test objects

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

  private Function createAnotherTestFunction() {
    OraclePackage parentPackage = new OraclePackage("test_pkg", "TEST_SCHEMA", null, null, null, 
                                                    null, null, null, null, null, null, null);
    Function function = new Function("another_function", null, null, "VARCHAR2", null);
    function.setSchema("TEST_SCHEMA");
    function.setParentPackage(parentPackage);
    
    // Create record type with same name but different context
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
    Procedure procedure = new Procedure("test_procedure", null, null, null, null);
    procedure.setSchema("TEST_SCHEMA");
    procedure.setParentPackage(parentPackage);
    
    // Create record type with fields
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
    OraclePackage pkg = new OraclePackage("test_pkg", "TEST_SCHEMA", null, null, null, 
                                         null, null, null, null, null, null, null);
    
    // Create multiple record types
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
    
    // Use constructor with record types
    pkg = new OraclePackage("test_pkg", "TEST_SCHEMA", null, null, null, 
                           null, Arrays.asList(empRecord, deptRecord), null, null, null, null, null);
    
    return pkg;
  }

  private Function createStandaloneFunction() {
    Function function = new Function("standalone_func", null, null, "VARCHAR2", null);
    function.setSchema("TEST_SCHEMA");
    // No parent package - standalone function
    
    RecordType.RecordField field = new RecordType.RecordField(
        "local_var", 
        new DataTypeSpec("VARCHAR2(50)", null, null, null), 
        false, 
        null
    );
    
    RecordType recordType = new RecordType("local_rec", Arrays.asList(field));
    function.setRecordTypes(Arrays.asList(recordType));
    
    return function;
  }

  private Procedure createStandaloneProcedure() {
    Procedure procedure = new Procedure("standalone_proc", null, null, null, null);
    procedure.setSchema("TEST_SCHEMA");
    // No parent package - standalone procedure
    
    RecordType.RecordField field = new RecordType.RecordField(
        "audit_date", 
        new DataTypeSpec("DATE", null, null, null), 
        false, 
        null
    );
    
    RecordType recordType = new RecordType("audit_rec", Arrays.asList(field));
    procedure.setRecordTypes(Arrays.asList(recordType));
    
    return procedure;
  }
}