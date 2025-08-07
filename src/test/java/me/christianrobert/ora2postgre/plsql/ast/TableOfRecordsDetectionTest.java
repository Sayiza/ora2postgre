package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import me.christianrobert.ora2postgre.services.TransformationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test detection of "table of records" collection types in Oracle PL/SQL.
 * This is the first step in implementing block-level collection support.
 */
public class TableOfRecordsDetectionTest {

  @Test
  public void testTableOfRecordsWithIntegerIndexDetection() {
    String oracleSql = """
    CREATE PACKAGE BODY TEST_SCHEMA.TABLE_OF_RECORDS_PKG is  
      PROCEDURE process_employees IS
        -- Define record type at block level
        TYPE employee_rec IS RECORD (
          emp_id NUMBER,
          emp_name VARCHAR2(100)
        );
        
        -- Define table of records with integer index
        TYPE employee_tab IS TABLE OF employee_rec INDEX BY PLS_INTEGER;
        l_employees employee_tab;
      BEGIN
        NULL;
      END;
    end;
    /
    """;

    // Parse the Oracle package
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");
    
    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast);
    assertTrue(ast instanceof OraclePackage);
    
    OraclePackage pkg = (OraclePackage) ast;
    assertEquals(1, pkg.getProcedures().size());
    
    Procedure procedure = pkg.getProcedures().get(0);
    assertEquals("process_employees", procedure.getName());
    
    // Verify record type is detected
    assertEquals(1, procedure.getRecordTypes().size());
    assertEquals("employee_rec", procedure.getRecordTypes().get(0).getName());
    
    // Verify variable is detected
    assertEquals(1, procedure.getVariables().size());
    Variable variable = procedure.getVariables().get(0);
    assertEquals("l_employees", variable.getName());
    
    // Test the new collection type detection methods
    System.out.println("=== TESTING COLLECTION TYPE DETECTION ===");
    System.out.println("Variable: " + variable.getName());
    System.out.println("Data type: " + variable.getDataType());
    System.out.println("Custom data type: " + variable.getDataType().getCustumDataType());
    
    // Debug: Check if we can extract the record type name
    String extractedRecordType = extractRecordTypeFromCollectionTypeForTesting(variable.getDataType().getCustumDataType());
    System.out.println("Extracted record type name: " + extractedRecordType);
    
    // Debug: Check if the record type exists in the procedure
    boolean recordTypeExists = procedure.getRecordTypes().stream()
        .anyMatch(rt -> rt.getName().equalsIgnoreCase(extractedRecordType));
    System.out.println("Record type exists in procedure: " + recordTypeExists);
    
    // Set up TransformationContext for the test
    TransformationContext context = new TransformationContext();
    TransformationContext.setTestInstance(context);
    
    // Set the procedure context for the variable
    variable.setTransformationContext(context);
    context.withProcedureContext(procedure, () -> {
      // Now test the detection within the proper context
      boolean isTableOfRecords = variable.isTableOfRecords();
      System.out.println("Is table of records (with context): " + isTableOfRecords);
      
      // This should now work
      assertTrue(isTableOfRecords, "Variable should be detected as table of records type");
      
      if (isTableOfRecords) {
        String recordTypeName = variable.getRecordTypeName();
        System.out.println("Record type name: " + recordTypeName);
        assertEquals("employee_rec", recordTypeName, "Should correctly extract record type name from collection type");
      }
    });
  }

  @Test
  public void testTableOfRecordsWithStringIndexDetection() {
    String oracleSql = """
    CREATE PACKAGE BODY TEST_SCHEMA.STRING_INDEX_PKG is  
      FUNCTION get_employee(p_key VARCHAR2) RETURN NUMBER IS
        TYPE person_rec IS RECORD (
          name VARCHAR2(50), 
          age NUMBER
        );
        
        TYPE person_tab IS TABLE OF person_rec INDEX BY VARCHAR2(20);
        l_people person_tab;
      BEGIN
        RETURN 1;
      END;
    end;
    /
    """;

    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");
    
    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertTrue(ast instanceof OraclePackage);
    OraclePackage pkg = (OraclePackage) ast;
    
    Function function = pkg.getFunctions().get(0);
    Variable variable = function.getVariables().get(0);
    
    System.out.println("=== TESTING STRING INDEX DETECTION ===");
    System.out.println("Variable: " + variable.getName());
    System.out.println("Custom data type: " + variable.getDataType().getCustumDataType());
    
    // Debug logic
    String extractedRecordType = extractRecordTypeFromCollectionTypeForTesting(variable.getDataType().getCustumDataType());
    System.out.println("Extracted record type name: " + extractedRecordType);
    
    boolean recordTypeExists = function.getRecordTypes().stream()
        .anyMatch(rt -> rt.getName().equalsIgnoreCase(extractedRecordType));
    System.out.println("Record type exists in function: " + recordTypeExists);
    
    // Set up TransformationContext for the test
    TransformationContext context = new TransformationContext();
    TransformationContext.setTestInstance(context);
    
    // Set the function context for the variable
    variable.setTransformationContext(context);
    context.withFunctionContext(function, () -> {
      boolean isTableOfRecords = variable.isTableOfRecords();
      System.out.println("Is table of records (with context): " + isTableOfRecords);
      
      assertTrue(isTableOfRecords, "String-indexed table of records should also be detected");
      assertEquals("person_rec", variable.getRecordTypeName());
      
      System.out.println("✅ String index detection test PASSED");
    });
  }

  @Test
  public void testNestedTableOfRecordsDetection() {
    String oracleSql = """
    CREATE PACKAGE BODY TEST_SCHEMA.NESTED_TABLE_PKG is  
      PROCEDURE process_data IS
        TYPE item_rec IS RECORD (
          id NUMBER,
          description VARCHAR2(200)
        );
        
        -- Nested table (no INDEX BY clause)
        TYPE item_list IS TABLE OF item_rec;
        l_items item_list;
      BEGIN
        NULL;
      END;
    end;
    /
    """;

    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");
    
    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertTrue(ast instanceof OraclePackage);
    OraclePackage pkg = (OraclePackage) ast;
    
    Procedure procedure = pkg.getProcedures().get(0);
    Variable variable = procedure.getVariables().get(0);
    
    System.out.println("=== TESTING NESTED TABLE DETECTION ===");
    System.out.println("Variable: " + variable.getName());
    System.out.println("Custom data type: " + variable.getDataType().getCustumDataType());
    
    // Debug logic
    String extractedRecordType = extractRecordTypeFromCollectionTypeForTesting(variable.getDataType().getCustumDataType());
    System.out.println("Extracted record type name: " + extractedRecordType);
    
    boolean recordTypeExists = procedure.getRecordTypes().stream()
        .anyMatch(rt -> rt.getName().equalsIgnoreCase(extractedRecordType));
    System.out.println("Record type exists in procedure: " + recordTypeExists);
    
    // Set up TransformationContext for the test
    TransformationContext context = new TransformationContext();
    TransformationContext.setTestInstance(context);
    
    // Set the procedure context for the variable
    variable.setTransformationContext(context);
    context.withProcedureContext(procedure, () -> {
      boolean isTableOfRecords = variable.isTableOfRecords();
      System.out.println("Is table of records (with context): " + isTableOfRecords);
      
      assertTrue(isTableOfRecords, "Nested table of records should be detected");
      assertEquals("item_rec", variable.getRecordTypeName());
      
      System.out.println("✅ Nested table detection test PASSED");
    });
  }

  @Test
  public void testTableOfRecordsVariableTransformation() {
    String oracleSql = """
    CREATE PACKAGE BODY TEST_SCHEMA.VARIABLE_TRANSFORM_PKG is  
      PROCEDURE test_variable_declarations IS
        -- Define record type
        TYPE employee_rec IS RECORD (
          emp_id NUMBER,
          emp_name VARCHAR2(100)
        );
        
        -- Test different collection patterns
        TYPE employee_tab IS TABLE OF employee_rec INDEX BY PLS_INTEGER;
        TYPE employee_list IS TABLE OF employee_rec;
        TYPE employee_array IS TABLE OF employee_rec INDEX BY VARCHAR2(50);
        
        -- Declare variables
        l_employees employee_tab;
        l_emp_list employee_list;
        l_emp_array employee_array;
        v_regular_var NUMBER;
      BEGIN
        NULL;
      END;
    end;
    /
    """;

    // Parse and transform
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");
    
    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertTrue(ast instanceof OraclePackage);
    OraclePackage pkg = (OraclePackage) ast;
    
    Procedure procedure = pkg.getProcedures().get(0);
    
    System.out.println("=== TESTING VARIABLE TRANSFORMATION ===");
    
    // Set up context
    TransformationContext context = new TransformationContext();
    TransformationContext.setTestInstance(context);
    
    context.withProcedureContext(procedure, () -> {
      // Test each variable transformation
      assertEquals(4, procedure.getVariables().size());
      
      for (Variable variable : procedure.getVariables()) {
        variable.setTransformationContext(context);
        String transformed = variable.toPostgre(data, procedure);
        
        System.out.println("Variable: " + variable.getName());
        System.out.println("Transformed: " + transformed);
        System.out.println("Is table of records: " + variable.isTableOfRecords());
        
        if (variable.isTableOfRecords()) {
          // Should be transformed to JSONB
          assertTrue(transformed.contains("jsonb := '{}'::jsonb"), 
                     "Table of records variable should be transformed to JSONB: " + variable.getName());
          assertTrue(transformed.contains("-- Table of"), 
                     "Should include comment indicating original record type: " + variable.getName());
        } else {
          // Regular variable should keep original transformation
          assertFalse(transformed.contains("jsonb"), 
                      "Regular variable should not be transformed to JSONB: " + variable.getName());
        }
        
        System.out.println("---");
      }
      
      // Test specific transformations
      Variable l_employees = procedure.getVariables().stream()
          .filter(v -> "l_employees".equals(v.getName()))
          .findFirst().orElse(null);
      assertNotNull(l_employees);
      String empTransformed = l_employees.toPostgre(data, procedure);
      assertTrue(empTransformed.contains("l_employees jsonb := '{}'::jsonb; -- Table of test_schema_variable_transform_pkg_test_variable_declarations_employee_rec"));
      
      Variable v_regular_var = procedure.getVariables().stream()
          .filter(v -> "v_regular_var".equals(v.getName()))
          .findFirst().orElse(null);
      assertNotNull(v_regular_var);
      String regularTransformed = v_regular_var.toPostgre(data, procedure);
      assertTrue(regularTransformed.contains("v_regular_var numeric"));
      assertFalse(regularTransformed.contains("jsonb"));
    });
    
    System.out.println("✅ Variable transformation test PASSED");
  }

  /**
   * Helper method to test the record type extraction logic.
   */
  private String extractRecordTypeFromCollectionTypeForTesting(String collectionType) {
    if (collectionType == null) {
      return null;
    }
    
    String lowerType = collectionType.toLowerCase();
    
    // Handle explicit "table_of_xxx" pattern
    if (lowerType.startsWith("table_of_")) {
      return collectionType.substring(9); // Remove "table_of_" prefix
    }
    
    // Handle common suffixes by replacing with "_rec"
    if (lowerType.endsWith("_tab")) {
      return collectionType.substring(0, collectionType.length() - 4) + "_rec";
    }
    if (lowerType.endsWith("_list")) {
      return collectionType.substring(0, collectionType.length() - 5) + "_rec";
    }
    if (lowerType.endsWith("_array")) {
      return collectionType.substring(0, collectionType.length() - 6) + "_rec";
    }
    if (lowerType.endsWith("_collection")) {
      return collectionType.substring(0, collectionType.length() - 11) + "_rec";
    }
    
    // If no pattern matches, assume the collection type name is the record type name
    return collectionType;
  }
}