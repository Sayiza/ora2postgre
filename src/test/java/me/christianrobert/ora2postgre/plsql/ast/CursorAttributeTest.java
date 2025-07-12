package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test class for cursor attribute functionality.
 * Tests Oracle cursor attributes (%FOUND, %NOTFOUND, %ROWCOUNT, %ISOPEN) 
 * and their transformation to PostgreSQL equivalents.
 */
public class CursorAttributeTest {

    @Test
    public void testCursorFoundAttribute() {
        String oracleSql = """
        CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
          FUNCTION test_cursor_found RETURN VARCHAR2 IS
            CURSOR emp_cursor IS SELECT id FROM employees WHERE active = 1;
            v_id NUMBER;
          BEGIN
            OPEN emp_cursor;
            FETCH emp_cursor INTO v_id;
            IF emp_cursor%FOUND THEN
              CLOSE emp_cursor;
              RETURN 'Found record';
            END IF;
            CLOSE emp_cursor;
            RETURN 'No record';
          END;
        end;
        /
        """;

        Everything data = createTestData();
        PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

        // Parse the Oracle function
        PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);
        OraclePackage parsedPackage = (OraclePackage) ast;
        assertNotNull(parsedPackage);

        Function function = parsedPackage.getFunctions().get(0);
        assertEquals("test_cursor_found", function.getName());

        // Test complete function transformation
        String completePgFunction = function.toPostgre(data, false);
        
        System.out.println("Generated PostgreSQL function with cursor%FOUND:");
        System.out.println(completePgFunction);

        assertNotNull(completePgFunction);
        // Test cursor declaration uses correct PostgreSQL syntax
        assertTrue(completePgFunction.contains("emp_cursor CURSOR FOR"));
        
        // Test cursor%FOUND is transformed to FOUND
        assertTrue(completePgFunction.contains("IF FOUND THEN"));
        
        // Should not contain Oracle syntax anymore
        assertFalse(completePgFunction.contains("emp_cursor%FOUND"));
    }

    @Test
    public void testCursorNotFoundAttribute() {
        String oracleSql = """
        CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
          PROCEDURE test_cursor_loop IS
            CURSOR emp_cursor IS SELECT id, name FROM employees;
            v_id NUMBER;
            v_name VARCHAR2(100);
          BEGIN
            OPEN emp_cursor;
            LOOP
              FETCH emp_cursor INTO v_id, v_name;
              EXIT WHEN emp_cursor%NOTFOUND;
              -- Process record
            END LOOP;
            CLOSE emp_cursor;
          END;
        end;
        /
        """;

        Everything data = createTestData();
        PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

        // Parse the Oracle procedure
        PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);
        OraclePackage parsedPackage = (OraclePackage) ast;
        assertNotNull(parsedPackage);

        Procedure procedure = parsedPackage.getProcedures().get(0);
        assertEquals("test_cursor_loop", procedure.getName());

        // Test complete procedure transformation
        String completePgProcedure = procedure.toPostgre(data, false);
        
        System.out.println("Generated PostgreSQL procedure with cursor%NOTFOUND:");
        System.out.println(completePgProcedure);

        assertNotNull(completePgProcedure);
        // Test cursor%NOTFOUND is transformed to NOT FOUND
        assertTrue(completePgProcedure.contains("EXIT WHEN NOT FOUND"));
        
        // Should not contain Oracle syntax anymore
        assertFalse(completePgProcedure.contains("emp_cursor%NOTFOUND"));
    }

    @Test
    public void testCursorRowCountAttribute() {
        String oracleSql = """
        CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
          FUNCTION get_processed_count RETURN NUMBER IS
            CURSOR emp_cursor IS SELECT id FROM employees WHERE active = 1;
            v_id NUMBER;
            v_count NUMBER;
          BEGIN
            OPEN emp_cursor;
            LOOP
              FETCH emp_cursor INTO v_id;
              EXIT WHEN emp_cursor%NOTFOUND;
              -- Process record
            END LOOP;
            v_count := emp_cursor%ROWCOUNT;
            CLOSE emp_cursor;
            RETURN v_count;
          END;
        end;
        /
        """;

        Everything data = createTestData();
        PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

        // Parse the Oracle function
        PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);
        OraclePackage parsedPackage = (OraclePackage) ast;
        assertNotNull(parsedPackage);

        Function function = parsedPackage.getFunctions().get(0);
        assertEquals("get_processed_count", function.getName());

        // Test complete function transformation
        String completePgFunction = function.toPostgre(data, false);
        
        System.out.println("Generated PostgreSQL function with cursor%ROWCOUNT:");
        System.out.println(completePgFunction);

        assertNotNull(completePgFunction);
        // Test cursor%ROWCOUNT gets commented with explanation
        assertTrue(completePgFunction.contains("/* emp_cursor%ROWCOUNT - use GET DIAGNOSTICS variable = ROW_COUNT */"));
        
        // Should not contain the raw Oracle syntax outside of comments
        // (the comment preserves the original syntax for reference, which is expected)
        assertFalse(completePgFunction.contains("v_count := emp_cursor%ROWCOUNT"));
    }

    @Test
    public void testCursorIsOpenAttribute() {
        String oracleSql = """
        CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
          FUNCTION check_cursor_status RETURN VARCHAR2 IS
            CURSOR emp_cursor IS SELECT id FROM employees;
          BEGIN
            IF emp_cursor%ISOPEN THEN
              RETURN 'Cursor is open';
            ELSE
              RETURN 'Cursor is closed';
            END IF;
          END;
        end;
        /
        """;

        Everything data = createTestData();
        PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

        // Parse the Oracle function
        PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);
        OraclePackage parsedPackage = (OraclePackage) ast;
        assertNotNull(parsedPackage);

        Function function = parsedPackage.getFunctions().get(0);
        assertEquals("check_cursor_status", function.getName());

        // Test complete function transformation
        String completePgFunction = function.toPostgre(data, false);
        
        System.out.println("Generated PostgreSQL function with cursor%ISOPEN:");
        System.out.println(completePgFunction);

        assertNotNull(completePgFunction);
        // Test cursor%ISOPEN gets commented with explanation
        assertTrue(completePgFunction.contains("/* emp_cursor%ISOPEN - manual cursor state tracking required */"));
        
        // Should not contain the raw Oracle syntax outside of comments
        // (the comment preserves the original syntax for reference, which is expected)
        assertFalse(completePgFunction.contains("IF emp_cursor%ISOPEN THEN"));
    }

    @Test
    public void testMultipleCursorAttributesInSameFunction() {
        String oracleSql = """
        CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
          FUNCTION complex_cursor_logic RETURN NUMBER IS
            CURSOR emp_cursor IS SELECT id FROM employees;
            v_id NUMBER;
            v_count NUMBER := 0;
          BEGIN
            IF NOT emp_cursor%ISOPEN THEN
              OPEN emp_cursor;
            END IF;
            
            LOOP
              FETCH emp_cursor INTO v_id;
              IF emp_cursor%FOUND THEN
                v_count := v_count + 1;
              END IF;
              EXIT WHEN emp_cursor%NOTFOUND;
            END LOOP;
            
            CLOSE emp_cursor;
            RETURN v_count;
          END;
        end;
        /
        """;

        Everything data = createTestData();
        PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

        // Parse the Oracle function
        PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);
        OraclePackage parsedPackage = (OraclePackage) ast;
        assertNotNull(parsedPackage);

        Function function = parsedPackage.getFunctions().get(0);
        assertEquals("complex_cursor_logic", function.getName());

        // Test complete function transformation
        String completePgFunction = function.toPostgre(data, false);
        
        System.out.println("Generated PostgreSQL function with multiple cursor attributes:");
        System.out.println(completePgFunction);

        assertNotNull(completePgFunction);
        
        // Test all cursor attributes are properly transformed
        assertTrue(completePgFunction.contains("/* emp_cursor%ISOPEN - manual cursor state tracking required */"));
        assertTrue(completePgFunction.contains("IF FOUND THEN"));
        assertTrue(completePgFunction.contains("EXIT WHEN NOT FOUND"));
        
        // Should not contain raw Oracle syntax outside of comments
        // (comments may preserve original syntax for reference, which is expected)
        assertFalse(completePgFunction.contains("IF NOT emp_cursor%ISOPEN THEN"));
        assertFalse(completePgFunction.contains("IF emp_cursor%FOUND THEN"));
        assertFalse(completePgFunction.contains("EXIT WHEN emp_cursor%NOTFOUND"));
    }

    @Test
    public void testCursorAttributeInAssignmentStatement() {
        String oracleSql = """
        CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
          PROCEDURE test_assignment IS
            CURSOR emp_cursor IS SELECT id FROM employees;
            v_id NUMBER;
            v_is_open BOOLEAN;
            v_has_data BOOLEAN;
          BEGIN
            OPEN emp_cursor;
            FETCH emp_cursor INTO v_id;
            
            v_is_open := emp_cursor%ISOPEN;
            v_has_data := emp_cursor%FOUND;
            
            CLOSE emp_cursor;
          END;
        end;
        /
        """;

        Everything data = createTestData();
        PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

        // Parse the Oracle procedure
        PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);
        OraclePackage parsedPackage = (OraclePackage) ast;
        assertNotNull(parsedPackage);

        Procedure procedure = parsedPackage.getProcedures().get(0);
        assertEquals("test_assignment", procedure.getName());

        // Test complete procedure transformation
        String completePgProcedure = procedure.toPostgre(data, false);
        
        System.out.println("Generated PostgreSQL procedure with cursor attributes in assignments:");
        System.out.println(completePgProcedure);

        assertNotNull(completePgProcedure);
        
        // Test cursor attributes in assignment statements are transformed
        assertTrue(completePgProcedure.contains("/* emp_cursor%ISOPEN - manual cursor state tracking required */"));
        assertTrue(completePgProcedure.contains("FOUND"));
        
        // Should not contain raw Oracle syntax outside of comments
        // (comments may preserve original syntax for reference, which is expected)
        assertFalse(completePgProcedure.contains("v_is_open := emp_cursor%ISOPEN"));
        assertFalse(completePgProcedure.contains("v_has_data := emp_cursor%FOUND"));
    }

    /**
     * Helper method to create test data context
     */
    private Everything createTestData() {
        Everything data = new Everything();
        data.getUserNames().add("TEST_SCHEMA");
        
        // Add required table metadata for employees table
        TableMetadata employeesTable = new TableMetadata("TEST_SCHEMA", "employees");
        data.getTableSql().add(employeesTable);
        
        return data;
    }
}