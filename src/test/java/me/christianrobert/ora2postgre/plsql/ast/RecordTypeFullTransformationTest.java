package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.RecordTypeCollectionManager;
import me.christianrobert.ora2postgre.services.TransformationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class that simulates the full transformation pipeline for record types.
 * This tests the complete process from AST through collection to PostgreSQL generation.
 */
public class RecordTypeFullTransformationTest {

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
  public void testFullProcedureTransformationWithRecordTypes() {
    // Create a procedure that mirrors the user's example
    Procedure procedure = createSimpleProcedureWithRecordType();
    
    // Before transformation - verify record types exist
    assertEquals(1, procedure.getRecordTypes().size());
    assertEquals("employee_rec", procedure.getRecordTypes().get(0).getName());
    assertEquals(1, procedure.getVariables().size());
    assertEquals("asingularemploee", procedure.getVariables().get(0).getName());
    
    // Verify no record types are collected initially
    assertEquals(0, RecordTypeCollectionManager.getCollectedCount());
    
    // Perform the full transformation (this should collect record types)
    String transformedProcedure = procedure.toPostgre(data, false);
    
    System.out.println("=== TRANSFORMED PROCEDURE ===");
    System.out.println(transformedProcedure);
    System.out.println("==============================");
    
    // After transformation - verify record types were collected
    assertEquals(1, RecordTypeCollectionManager.getCollectedCount());
    
    // The transformed procedure should contain:
    assertNotNull(transformedProcedure);
    
    // 1. Schema-level composite type reference comment
    assertTrue(transformedProcedure.contains("-- Using schema-level composite type:"));
    
    // 2. Variable declaration should use qualified name (not "/* data type not implemented */")
    assertFalse(transformedProcedure.contains("/* data type not implemented"));
    
    // 3. Should contain the CREATE PROCEDURE statement
    assertTrue(transformedProcedure.contains("CREATE OR REPLACE PROCEDURE"));
    assertTrue(transformedProcedure.contains("SIMPLE_PKG_display_employees"));
    
    // Test qualified name resolution
    RecordType recordType = procedure.getRecordTypes().get(0);
    String qualifiedName = RecordTypeCollectionManager.getQualifiedName(procedure, recordType);
    System.out.println("Qualified name: " + qualifiedName);
    
    // Should be something like: user_robert_simple_pkg_display_employees_employee_rec
    assertFalse(qualifiedName.equals("employee_rec")); // Should not be just the original name
    assertTrue(qualifiedName.contains("employee_rec")); // But should contain the original name
  }

  @Test
  public void testRecordTypeCollectionTiming() {
    // Test that collection happens at the right time
    Procedure procedure = createSimpleProcedureWithRecordType();
    
    // Manually collect (simulating what transformation strategy should do)
    RecordTypeCollectionManager.collectFromProcedure(procedure);
    
    assertEquals(1, RecordTypeCollectionManager.getCollectedCount());
    
    // Test qualified name generation
    RecordType recordType = procedure.getRecordTypes().get(0);
    String qualifiedName = RecordTypeCollectionManager.getQualifiedName(procedure, recordType);
    
    System.out.println("Manual collection qualified name: " + qualifiedName);
    assertNotNull(qualifiedName);
    assertFalse(qualifiedName.isEmpty());
    
    // Test variable transformation with collection available
    Variable recordVariable = procedure.getVariables().get(0);
    String variableDeclaration = recordVariable.toPostgre(data, procedure);
    
    System.out.println("Variable declaration with collection: " + variableDeclaration);
    
    // Should use the qualified name, not the original type name
    if (qualifiedName.equals("employee_rec")) {
      System.out.println("WARNING: Qualified name is not being generated correctly!");
    }
  }

  // Helper method that creates a procedure similar to the user's example
  private Procedure createSimpleProcedureWithRecordType() {
    // Create the package to provide context
    OraclePackage parentPackage = new OraclePackage("simple_pkg", "USER_ROBERT", null, null, null, 
                                                    null, null, null, null, null, null, null);
    
    // Create record type definition matching user's example
    RecordType.RecordField empId = new RecordType.RecordField(
        "emp_id", 
        new DataTypeSpec("NUMBER", null, null, null), 
        false, 
        null
    );
    RecordType.RecordField empName = new RecordType.RecordField(
        "emp_name", 
        new DataTypeSpec("VARCHAR2(100)", null, null, null), 
        false, 
        null
    );
    
    RecordType employeeRec = new RecordType("employee_rec", Arrays.asList(empId, empName));
    
    // Create variable that uses the record type (matching user's example)
    Variable recordVariable = new Variable(
        "asingularemploee",
        new DataTypeSpec(null, "employee_rec", null, null), // Custom type reference
        null
    );
    
    // Create procedure with record type and variable
    Procedure procedure = new Procedure(
        "display_employees",
        Arrays.asList(), // empty parameters
        Arrays.asList(recordVariable), // variables using record type
        Arrays.asList() // empty statements for this test
    );
    
    procedure.setSchema("USER_ROBERT");
    procedure.setParentPackage(parentPackage);
    procedure.setRecordTypes(Arrays.asList(employeeRec));
    
    return procedure;
  }
}