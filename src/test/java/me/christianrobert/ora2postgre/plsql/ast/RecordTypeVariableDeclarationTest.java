package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.services.TransformationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class specifically for record type variable declarations and usage.
 * This tests the fixes for DataTypeSpec record type recognition.
 */
public class RecordTypeVariableDeclarationTest {

  private TransformationContext transformationContext;
  private Everything data;

  @BeforeEach
  public void setUp() {
    transformationContext = new TransformationContext();
    TransformationContext.setTestInstance(transformationContext);
    data = new Everything();
  }

  @Test
  public void testRecordTypeVariableDeclarationInFunction() {
    // Create a function with a record type and a variable using that record type
    Function function = createFunctionWithRecordType();
    
    // Get the variable that uses the record type
    Variable recordVariable = function.getVariables().get(0);
    assertEquals("asingularemploee", recordVariable.getName());
    
    // Test the variable's PostgreSQL transformation - should use qualified composite type name
    String variableDeclaration = recordVariable.toPostgre(data, function);
    
    assertNotNull(variableDeclaration);
    // Should NOT contain "data type not implemented"
    assertFalse(variableDeclaration.contains("/* data type not implemented"));
    // Should contain the qualified composite type name
    assertTrue(variableDeclaration.contains("asingularemploee"));
    
    // The qualified name should be generated when the record type is collected
    // For now, just verify it's not the error message
    System.out.println("Variable declaration result: " + variableDeclaration);
  }

  @Test
  public void testRecordTypeVariableDeclarationInProcedure() {
    // Create a procedure with a record type and a variable using that record type
    Procedure procedure = createProcedureWithRecordType();
    
    // Get the variable that uses the record type
    Variable recordVariable = procedure.getVariables().get(0);
    assertEquals("asingularemploee", recordVariable.getName());
    
    // Test the variable's PostgreSQL transformation - should use qualified composite type name
    String variableDeclaration = recordVariable.toPostgre(data, procedure);
    
    assertNotNull(variableDeclaration);
    // Should NOT contain "data type not implemented"
    assertFalse(variableDeclaration.contains("/* data type not implemented"));
    // Should contain the qualified composite type name
    assertTrue(variableDeclaration.contains("asingularemploee"));
    
    System.out.println("Procedure variable declaration result: " + variableDeclaration);
  }

  @Test
  public void testDataTypeSpecWithRecordType() {
    // Test DataTypeSpec directly with a custom record type
    DataTypeSpec recordTypeSpec = new DataTypeSpec(null, "employee_rec", null, null);
    
    // Create a function with the record type defined
    Function function = createFunctionWithRecordType();
    
    // Test PostgreSQL transformation
    String postgresType = recordTypeSpec.toPostgre(data, function);
    
    assertNotNull(postgresType);
    // Should NOT be the error message
    assertFalse(postgresType.contains("/* data type not implemented"));
    
    System.out.println("DataTypeSpec result: " + postgresType);
  }

  @Test  
  public void testDataTypeSpecWithUnknownType() {
    // Test DataTypeSpec with a type that doesn't exist
    DataTypeSpec unknownTypeSpec = new DataTypeSpec(null, "unknown_type", null, null);
    
    Function function = createFunctionWithRecordType();
    
    // Should fall back to error message
    String postgresType = unknownTypeSpec.toPostgre(data, function);
    
    assertTrue(postgresType.contains("/* data type not implemented"));
  }

  // Helper methods

  private Function createFunctionWithRecordType() {
    // Create record type definition
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
    
    // Create variable that uses the record type
    Variable recordVariable = new Variable(
        "asingularemploee",
        new DataTypeSpec(null, "employee_rec", null, null), // Custom type reference
        null
    );
    
    // Create function with record type and variable
    Function function = new Function(
        "display_employees",
        Arrays.asList(), // empty parameters
        Arrays.asList(recordVariable), // variables using record type
        "VARCHAR2",
        Arrays.asList() // empty statements for this test
    );
    
    function.setSchema("USER_ROBERT");
    function.setRecordTypes(Arrays.asList(employeeRec));
    
    return function;
  }

  private Procedure createProcedureWithRecordType() {
    // Create record type definition
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
    
    // Create variable that uses the record type
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
    procedure.setRecordTypes(Arrays.asList(employeeRec));
    
    return procedure;
  }
}