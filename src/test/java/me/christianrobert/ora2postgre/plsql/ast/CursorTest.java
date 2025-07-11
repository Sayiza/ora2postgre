package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.oracledb.SynonymMetadata;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CursorTest {

  @Test
  public void testBasicCursorDeclarationAndUsage() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION process_employees RETURN NUMBER IS
    CURSOR emp_cursor IS SELECT 1, 'test';
    v_emp_id NUMBER;
    v_first_name VARCHAR2(50);
    v_count NUMBER := 0;
  BEGIN
    OPEN emp_cursor;
    LOOP
      FETCH emp_cursor INTO v_emp_id, v_first_name;
      EXIT WHEN emp_cursor%NOTFOUND;
      v_count := v_count + 1;
    END LOOP;
    CLOSE emp_cursor;
    RETURN v_count;
  END;
end;
/
""";

    // Create test data and setup schema information
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    OraclePackage pkg = (OraclePackage) ast;
    assertNotNull(pkg);
    assertEquals(1, pkg.getFunctions().size());
    
    Function function = pkg.getFunctions().get(0);
    assertNotNull(function);
    assertEquals("process_employees", function.getName());

    // Test that cursor declarations are parsed correctly
    List<CursorDeclaration> cursors = function.getCursorDeclarations();
    assertNotNull(cursors);
    assertEquals(1, cursors.size());
    assertEquals("emp_cursor", cursors.get(0).getCursorName());

    // Test complete function transformation
    String completePgFunction = function.toPostgre(data, false);
    
    // Debug: Print the complete function to see what's actually generated
    System.out.println("Generated PostgreSQL function:");
    System.out.println(completePgFunction);
    
    assertNotNull(completePgFunction);
    // Test PostgreSQL function structure
    assertTrue(completePgFunction.contains("CREATE OR REPLACE FUNCTION"));
    assertTrue(completePgFunction.contains("TEST_SCHEMA.TESTPACKAGE_process_employees"));
    assertTrue(completePgFunction.contains("RETURNS"));
    assertTrue(completePgFunction.contains("LANGUAGE plpgsql"));
    assertTrue(completePgFunction.contains("DECLARE"));
    assertTrue(completePgFunction.contains("BEGIN"));
    assertTrue(completePgFunction.contains("END"));
    
    // Test cursor declaration is included
    assertTrue(completePgFunction.contains("CURSOR emp_cursor IS"));
    assertTrue(completePgFunction.contains("SELECT")); // SELECT statement is transformed
    
    // Test cursor statements are included
    assertTrue(completePgFunction.contains("OPEN emp_cursor"));
    assertTrue(completePgFunction.contains("CLOSE emp_cursor"));
    
    // Note: FETCH statement is part of LOOP which isn't fully transpiled yet
    // This is expected - cursor declarations and basic OPEN/CLOSE work
  }

  @Test
  public void testParameterizedCursor() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION get_department_count(p_dept_id IN NUMBER) RETURN NUMBER IS
    CURSOR dept_cursor(c_dept_id NUMBER) IS 
      SELECT c_dept_id, 'Department';
    v_count NUMBER;
  BEGIN
    OPEN dept_cursor(p_dept_id);
    FETCH dept_cursor INTO v_count;
    CLOSE dept_cursor;
    RETURN v_count;
  END;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    OraclePackage pkg = (OraclePackage) ast;
    assertNotNull(pkg);
    
    Function function = pkg.getFunctions().get(0);
    assertNotNull(function);

    // Test that cursor declarations are parsed correctly
    List<CursorDeclaration> cursors = function.getCursorDeclarations();
    assertNotNull(cursors);
    assertEquals(1, cursors.size());
    assertEquals("dept_cursor", cursors.get(0).getCursorName());
    assertEquals(1, cursors.get(0).getParameters().size());

    // Test complete function transformation
    String completePgFunction = function.toPostgre(data, false);
    
    assertNotNull(completePgFunction);
    // Test parameterized cursor declaration
    assertTrue(completePgFunction.contains("CURSOR dept_cursor"));
    assertTrue(completePgFunction.contains("c_dept_id"));
    
    // Test cursor opening with parameter
    assertTrue(completePgFunction.contains("OPEN dept_cursor"));
  }

  @Test
  public void testCursorInProcedure() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  PROCEDURE update_salaries IS
    CURSOR sal_cursor IS SELECT 1 as employee_id, 50000 as salary;
    v_emp_id NUMBER;
    v_salary NUMBER;
  BEGIN
    OPEN sal_cursor;
    LOOP
      FETCH sal_cursor INTO v_emp_id, v_salary;
      EXIT WHEN sal_cursor%NOTFOUND;
      -- Simulate UPDATE without table dependency
      v_salary := v_salary * 1.1;
    END LOOP;
    CLOSE sal_cursor;
  END;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle procedure
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    OraclePackage pkg = (OraclePackage) ast;
    assertNotNull(pkg);
    assertEquals(1, pkg.getProcedures().size());
    
    Procedure procedure = pkg.getProcedures().get(0);
    assertNotNull(procedure);

    // Test that cursor declarations are parsed correctly
    List<CursorDeclaration> cursors = procedure.getCursorDeclarations();
    assertNotNull(cursors);
    assertEquals(1, cursors.size());
    assertEquals("sal_cursor", cursors.get(0).getCursorName());

    // Test complete procedure transformation
    String completePgProcedure = procedure.toPostgre(data, false);
    
    assertNotNull(completePgProcedure);
    // Test PostgreSQL procedure structure
    assertTrue(completePgProcedure.contains("CREATE OR REPLACE PROCEDURE"));
    assertTrue(completePgProcedure.contains("TEST_SCHEMA.TESTPACKAGE_update_salaries"));
    
    // Test cursor functionality in procedure
    assertTrue(completePgProcedure.contains("CURSOR sal_cursor IS"));
    assertTrue(completePgProcedure.contains("OPEN sal_cursor"));
    assertTrue(completePgProcedure.contains("CLOSE sal_cursor"));
    
    // Note: FETCH is part of LOOP which isn't fully transpiled yet
  }

  @Test
  public void testSimpleCursorStatements() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  PROCEDURE simple_cursor_test IS
    CURSOR test_cursor IS SELECT 1;
    v_dummy NUMBER;
  BEGIN
    OPEN test_cursor;
    FETCH test_cursor INTO v_dummy;
    CLOSE test_cursor;
  END;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle procedure
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    OraclePackage pkg = (OraclePackage) ast;
    assertNotNull(pkg);
    
    Procedure procedure = pkg.getProcedures().get(0);
    assertNotNull(procedure);

    // Test that cursor declarations are parsed correctly
    List<CursorDeclaration> cursors = procedure.getCursorDeclarations();
    assertNotNull(cursors);
    assertEquals(1, cursors.size());
    assertEquals("test_cursor", cursors.get(0).getCursorName());

    // Test complete procedure transformation
    String completePgProcedure = procedure.toPostgre(data, false);
    
    assertNotNull(completePgProcedure);
    // Test basic cursor operations
    assertTrue(completePgProcedure.contains("CURSOR test_cursor IS"));
    assertTrue(completePgProcedure.contains("SELECT"));
    assertTrue(completePgProcedure.contains("OPEN test_cursor"));
    assertTrue(completePgProcedure.contains("CLOSE test_cursor"));
  }
}