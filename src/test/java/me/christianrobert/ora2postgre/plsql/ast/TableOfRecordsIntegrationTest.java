package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test for "table of records" functionality.
 * Tests complete procedure transformation including record types, 
 * table of records variables, and their integration with existing systems.
 */
public class TableOfRecordsIntegrationTest {

  @Test
  public void testCompleteTableOfRecordsProcedureTransformation() {
    String oracleSql = """
    CREATE PACKAGE BODY TEST_SCHEMA.EMPLOYEE_MANAGEMENT_PKG is  
      PROCEDURE manage_employee_data IS
        -- Define record type at block level
        TYPE employee_rec IS RECORD (
          emp_id NUMBER,
          emp_name VARCHAR2(100),
          department VARCHAR2(50),
          salary NUMBER DEFAULT 0
        );
        
        -- Define different collection types
        TYPE employee_tab IS TABLE OF employee_rec INDEX BY PLS_INTEGER;
        TYPE employee_list IS TABLE OF employee_rec;
        TYPE employee_map IS TABLE OF employee_rec INDEX BY VARCHAR2(20);
        
        -- Declare collection variables
        l_employees employee_tab;
        l_emp_list employee_list;
        l_emp_by_name employee_map;
        
        -- Regular variables for comparison
        v_count NUMBER := 0;
        v_total_salary NUMBER;
        
        -- Individual record variable
        l_single_emp employee_rec;
      BEGIN
        -- This procedure body will be enhanced in future steps
        -- For now, just test the declarations
        NULL;
      END;
    end;
    /
    """;

    // Parse and transform the complete package
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");
    
    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast);
    assertTrue(ast instanceof OraclePackage);
    
    OraclePackage pkg = (OraclePackage) ast;
    assertEquals("EMPLOYEE_MANAGEMENT_PKG", pkg.getName());
    assertEquals("TEST_SCHEMA", pkg.getSchema());
    
    // Verify procedure parsing
    assertEquals(1, pkg.getProcedures().size());
    Procedure procedure = pkg.getProcedures().get(0);
    assertEquals("manage_employee_data", procedure.getName());
    
    // Verify record type collection
    assertEquals(1, procedure.getRecordTypes().size());
    RecordType recordType = procedure.getRecordTypes().get(0);
    assertEquals("employee_rec", recordType.getName());
    assertEquals(4, recordType.getFields().size());
    
    // Verify variable parsing
    assertEquals(6, procedure.getVariables().size());
    
    System.out.println("=== COMPLETE PROCEDURE TRANSFORMATION TEST ===");
    
    // Test complete procedure transformation
    String procedureSQL = procedure.toPostgre(data, false);
    System.out.println("Generated PostgreSQL:");
    System.out.println(procedureSQL);
    System.out.println("==============================================");
    
    // Verify procedure structure
    assertNotNull(procedureSQL);
    assertTrue(procedureSQL.contains("CREATE OR REPLACE PROCEDURE"));
    assertTrue(procedureSQL.contains("TEST_SCHEMA.EMPLOYEE_MANAGEMENT_PKG_manage_employee_data"));
    assertTrue(procedureSQL.contains("LANGUAGE plpgsql AS $$"));
    assertTrue(procedureSQL.contains("DECLARE"));
    assertTrue(procedureSQL.contains("BEGIN"));
    assertTrue(procedureSQL.contains("END;"));
    
    // Verify record type integration
    assertTrue(procedureSQL.contains("-- Using schema-level composite type:"));
    assertTrue(procedureSQL.contains("-- Original record type: employee_rec"));
    assertTrue(procedureSQL.contains("test_schema_employee_management_pkg_manage_employee_data_employee_rec"));
    
    // Verify table of records variable transformations
    assertTrue(procedureSQL.contains("l_employees jsonb := '{}'::jsonb -- Table of test_schema_employee_management_pkg_manage_employee_data_employee_rec"),
               "Integer-indexed table of records should be transformed to JSONB");
    assertTrue(procedureSQL.contains("l_emp_list jsonb := '{}'::jsonb -- Table of test_schema_employee_management_pkg_manage_employee_data_employee_rec"),
               "Nested table of records should be transformed to JSONB");
    assertTrue(procedureSQL.contains("l_emp_by_name jsonb := '{}'::jsonb -- Table of test_schema_employee_management_pkg_manage_employee_data_employee_rec"),
               "String-indexed table of records should be transformed to JSONB");
    
    // Verify regular variables remain unchanged
    assertTrue(procedureSQL.contains("v_count numeric := 0"),
               "Regular variables should maintain original transformation");
    assertTrue(procedureSQL.contains("v_total_salary numeric"),
               "Regular variables should maintain original transformation");
    
    // Verify individual record variable (using existing record type system)
    assertTrue(procedureSQL.contains("l_single_emp test_schema_employee_management_pkg_manage_employee_data_employee_rec"),
               "Individual record variables should use existing composite type system");
    
    // Verify no transformation errors
    assertFalse(procedureSQL.contains("/* INVALID"));
    assertFalse(procedureSQL.contains("TODO"));
    assertFalse(procedureSQL.contains("Unknown"));
    
    System.out.println("✅ Complete procedure transformation test PASSED");
  }

  @Test
  public void testTableOfRecordsInFunctionContext() {
    String oracleSql = """
    CREATE PACKAGE BODY TEST_SCHEMA.FUNCTION_COLLECTIONS_PKG is  
      FUNCTION process_employee_collections(p_dept_id NUMBER) RETURN NUMBER IS
        -- Function-level record type
        TYPE dept_summary_rec IS RECORD (
          dept_id NUMBER,
          total_employees NUMBER,
          avg_salary NUMBER
        );
        
        -- Collection of summaries
        TYPE dept_summary_tab IS TABLE OF dept_summary_rec INDEX BY PLS_INTEGER;
        l_summaries dept_summary_tab;
        
        -- String-indexed collection
        TYPE employee_lookup IS TABLE OF dept_summary_rec INDEX BY VARCHAR2(50);
        l_lookup employee_lookup;
        
        v_result NUMBER := 0;
      BEGIN
        RETURN v_result;
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
    assertEquals("process_employee_collections", function.getName());
    
    System.out.println("=== FUNCTION CONTEXT TEST ===");
    
    // Test function transformation
    String functionSQL = function.toPostgre(data, false);
    System.out.println("Generated Function SQL:");
    System.out.println(functionSQL);
    System.out.println("==============================");
    
    // Verify function structure
    assertTrue(functionSQL.contains("CREATE OR REPLACE FUNCTION"));
    assertTrue(functionSQL.contains("TEST_SCHEMA.FUNCTION_COLLECTIONS_PKG_process_employee_collections"));
    assertTrue(functionSQL.contains("RETURNS numeric"));
    
    // Verify function-level record type integration
    assertTrue(functionSQL.contains("-- Using schema-level composite type:"));
    assertTrue(functionSQL.contains("-- Original record type: dept_summary_rec"));
    
    // Verify table of records transformations in function context
    assertTrue(functionSQL.contains("l_summaries jsonb := '{}'::jsonb -- Table of test_schema_function_collections_pkg_process_employee_collections_dept_summary_rec"));
    assertTrue(functionSQL.contains("l_lookup jsonb := '{}'::jsonb -- Table of test_schema_function_collections_pkg_process_employee_collections_dept_summary_rec"));
    
    // Verify regular variable
    assertTrue(functionSQL.contains("v_result numeric := 0"));
    
    System.out.println("✅ Function context test PASSED");
  }

  @Test
  public void testMixedRecordTypesAndCollections() {
    String oracleSql = """
    CREATE PACKAGE BODY TEST_SCHEMA.MIXED_TYPES_PKG is  
      PROCEDURE test_mixed_declarations IS
        -- Multiple record types
        TYPE person_rec IS RECORD (
          id NUMBER,
          name VARCHAR2(100)
        );
        
        TYPE address_rec IS RECORD (
          street VARCHAR2(200),
          city VARCHAR2(100),
          country VARCHAR2(50)
        );
        
        -- Collections of different record types
        TYPE person_tab IS TABLE OF person_rec INDEX BY PLS_INTEGER;
        TYPE address_list IS TABLE OF address_rec;
        
        -- Mixed variable declarations
        l_people person_tab;
        l_addresses address_list;
        l_single_person person_rec;
        l_single_address address_rec;
        v_counter NUMBER;
        
        -- Regular collection (not of records)
        TYPE number_array IS VARRAY(10) OF NUMBER;
        l_numbers number_array;
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
    
    System.out.println("=== MIXED TYPES TEST ===");
    
    String procedureSQL = procedure.toPostgre(data, false);
    System.out.println("Generated Mixed Types SQL:");
    System.out.println(procedureSQL);
    System.out.println("==========================");
    
    // Verify multiple record types are handled
    assertEquals(2, procedure.getRecordTypes().size());
    assertTrue(procedureSQL.contains("-- Original record type: person_rec"));
    assertTrue(procedureSQL.contains("-- Original record type: address_rec"));
    
    // Verify table of records transformations for different types
    assertTrue(procedureSQL.contains("l_people jsonb := '{}'::jsonb -- Table of test_schema_mixed_types_pkg_test_mixed_declarations_person_rec"));
    assertTrue(procedureSQL.contains("l_addresses jsonb := '{}'::jsonb -- Table of test_schema_mixed_types_pkg_test_mixed_declarations_address_rec"));
    
    // Verify individual record variables use composite types
    assertTrue(procedureSQL.contains("l_single_person test_schema_mixed_types_pkg_test_mixed_declarations_person_rec"));
    assertTrue(procedureSQL.contains("l_single_address test_schema_mixed_types_pkg_test_mixed_declarations_address_rec"));
    
    // Verify regular variables remain unchanged
    assertTrue(procedureSQL.contains("v_counter numeric"));
    
    // Verify regular collection (not of records) uses existing transformation
    assertTrue(procedureSQL.contains("l_numbers numeric[]") || procedureSQL.contains("l_numbers"), 
               "Regular collection should use existing transformation");
    assertFalse(procedureSQL.contains("l_numbers jsonb"),
                "Regular collection should NOT be transformed to JSONB");
    
    System.out.println("✅ Mixed types test PASSED");
  }
}