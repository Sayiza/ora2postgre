package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the new expression architecture and cursor attribute functionality.
 */
public class ExpressionArchitectureTest {

    @Test
    public void testCursorAttributeParsingWithNewArchitecture() {
        String oracleSql = """
        CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
          PROCEDURE test_cursor_attr IS
            CURSOR emp_cursor IS SELECT id FROM employees WHERE active = 1;
            v_id NUMBER;
          BEGIN
            OPEN emp_cursor;
            FETCH emp_cursor INTO v_id;
            IF emp_cursor%NOTFOUND THEN
              CLOSE emp_cursor;
            END IF;
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
        assertEquals("test_cursor_attr", procedure.getName());

        // Test complete procedure transformation
        String completePgProcedure = procedure.toPostgre(data, false);
        
        System.out.println("Generated PostgreSQL procedure with cursor%NOTFOUND:");
        System.out.println(completePgProcedure);

        assertNotNull(completePgProcedure);
        
        // Test the core issue: Is cursor%NOTFOUND being transformed?
        boolean hasOracleAttribute = completePgProcedure.contains("emp_cursor%NOTFOUND");
        boolean hasPostgreEquivalent = completePgProcedure.contains("NOT FOUND") || 
                                      completePgProcedure.contains("CURSOR_ATTR:");
        
        System.out.println("Contains Oracle syntax (emp_cursor%NOTFOUND): " + hasOracleAttribute);
        System.out.println("Contains PostgreSQL equivalent or marker: " + hasPostgreEquivalent);
        
        // The goal is to have transformed cursor attributes
        assertFalse(hasOracleAttribute, "Oracle cursor attribute syntax should be transformed");
        assertTrue(hasPostgreEquivalent, "Should contain PostgreSQL equivalent or transformation marker");
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