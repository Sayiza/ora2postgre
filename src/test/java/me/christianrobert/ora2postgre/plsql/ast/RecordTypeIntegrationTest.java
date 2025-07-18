package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.oracledb.ColumnMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Record Types functionality across the entire parsing and transformation pipeline.
 * Tests that record types are properly parsed, collected, and transformed in packages, functions, and procedures.
 */
public class RecordTypeIntegrationTest {

  @Test
  public void testPackageWithRecordTypesEndToEnd() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.INTEGRATION_TEST_PKG is  
  
  -- Package-level record type
  TYPE employee_record IS RECORD (
    emp_id NUMBER,
    emp_name VARCHAR2(100),
    hire_date DATE DEFAULT SYSDATE,
    is_active BOOLEAN DEFAULT TRUE
  );
  
  TYPE department_record IS RECORD (
    dept_id NUMBER,
    dept_name VARCHAR2(50),
    emp_count NUMBER DEFAULT 0
  );
  
  FUNCTION get_employee_info(p_emp_id NUMBER) RETURN NUMBER IS
    -- Function-level record type
    TYPE temp_record IS RECORD (
      temp_id NUMBER,
      temp_value VARCHAR2(50)
    );
    
    v_emp employee_record;
    v_temp temp_record;
    v_dept_info departments%ROWTYPE;
  BEGIN
    v_emp.emp_id := p_emp_id;
    v_emp.emp_name := 'Test Employee';
    v_temp.temp_id := 1;
    v_temp.temp_value := 'Temporary';
    
    SELECT * INTO v_dept_info FROM departments WHERE department_id = 10;
    
    RETURN v_emp.emp_id;
  END;
  
  PROCEDURE update_employee_data(p_emp_id NUMBER) IS
    -- Procedure-level record type
    TYPE audit_record IS RECORD (
      audit_id NUMBER,
      operation VARCHAR2(20),
      timestamp DATE DEFAULT SYSDATE
    );
    
    v_emp employee_record;
    v_audit audit_record;
  BEGIN
    v_emp.emp_id := p_emp_id;
    v_audit.audit_id := 1001;
    v_audit.operation := 'UPDATE';
    
    UPDATE employees SET last_name = v_emp.emp_name WHERE employee_id = p_emp_id;
  END;
  
end;
/
""";

    // Create test data with table metadata for %ROWTYPE resolution
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");
    
    // Add table metadata for %ROWTYPE resolution
    TableMetadata empTable = new TableMetadata("TEST_SCHEMA", "employees");
    empTable.addColumn(new ColumnMetadata("employee_id", "NUMBER", null, 10, 0, true, null));
    empTable.addColumn(new ColumnMetadata("first_name", "VARCHAR2", 50, null, null, false, null));
    empTable.addColumn(new ColumnMetadata("last_name", "VARCHAR2", 50, null, null, false, null));
    data.getTableSql().add(empTable);
    
    TableMetadata deptTable = new TableMetadata("TEST_SCHEMA", "departments");
    deptTable.addColumn(new ColumnMetadata("department_id", "NUMBER", null, 10, 0, true, null));
    deptTable.addColumn(new ColumnMetadata("department_name", "VARCHAR2", 100, null, null, false, null));
    data.getTableSql().add(deptTable);

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle package
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast);
    assertTrue(ast instanceof OraclePackage);
    
    OraclePackage pkg = (OraclePackage) ast;
    assertEquals("INTEGRATION_TEST_PKG", pkg.getName());
    assertEquals("TEST_SCHEMA", pkg.getSchema());
    
    // Verify package-level record types are collected
    assertEquals(2, pkg.getRecordTypes().size());
    assertEquals("employee_record", pkg.getRecordTypes().get(0).getName());
    assertEquals("department_record", pkg.getRecordTypes().get(1).getName());
    
    // Verify package has functions and procedures
    assertEquals(1, pkg.getFunctions().size());
    assertEquals(1, pkg.getProcedures().size());
    
    Function function = pkg.getFunctions().get(0);
    assertEquals("get_employee_info", function.getName());
    
    // Verify function-level record types are collected
    assertEquals(1, function.getRecordTypes().size());
    assertEquals("temp_record", function.getRecordTypes().get(0).getName());
    
    Procedure procedure = pkg.getProcedures().get(0);
    assertEquals("update_employee_data", procedure.getName());
    
    // Verify procedure-level record types are collected
    assertEquals(1, procedure.getRecordTypes().size());
    assertEquals("audit_record", procedure.getRecordTypes().get(0).getName());
  }

  @Test
  public void testPackagePostgreSQLGeneration() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.POSTGRES_TEST_PKG is  
  
  TYPE simple_record IS RECORD (
    id NUMBER,
    name VARCHAR2(50)
  );
  
  FUNCTION test_function RETURN NUMBER IS
    v_rec simple_record;
  BEGIN
    v_rec.id := 123;
    v_rec.name := 'Test';
    RETURN v_rec.id;
  END;
  
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle package
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);
    assertTrue(ast instanceof OraclePackage);
    
    OraclePackage pkg = (OraclePackage) ast;
    
    // Test PostgreSQL generation for package spec (record types should be generated)
    String packageSpecSQL = pkg.toPostgre(data, true);
    assertNotNull(packageSpecSQL);
    assertTrue(packageSpecSQL.contains("Record Types for TEST_SCHEMA.postgres_test_pkg"));
    assertTrue(packageSpecSQL.contains("CREATE TYPE test_schema_postgres_test_pkg_simple_record AS"));
    assertTrue(packageSpecSQL.contains("id numeric"));
    assertTrue(packageSpecSQL.contains("name text"));
    
    // Test PostgreSQL generation for package body (record types should NOT be generated to avoid duplication)
    String packageBodySQL = pkg.toPostgre(data, false);
    assertNotNull(packageBodySQL);
    assertFalse(packageBodySQL.contains("Record Types for TEST_SCHEMA.postgres_test_pkg"));
    
    // But function should be generated with body
    assertTrue(packageBodySQL.contains("CREATE OR REPLACE FUNCTION TEST_SCHEMA.POSTGRES_TEST_PKG_test_function"));
    assertTrue(packageBodySQL.contains("LANGUAGE plpgsql"));
  }

  @Test
  public void testFunctionWithRecordTypesPostgreSQLGeneration() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.FUNCTION_RECORD_PKG is  
  
  FUNCTION complex_function(p_id NUMBER) RETURN VARCHAR2 IS
    TYPE local_record IS RECORD (
      local_id NUMBER,
      local_desc VARCHAR2(100)
    );
    
    v_local local_record;
    v_result VARCHAR2(200);
  BEGIN
    v_local.local_id := p_id;
    v_local.local_desc := 'Local description';
    v_result := 'ID: ' || v_local.local_id || ', Desc: ' || v_local.local_desc;
    RETURN v_result;
  END;
  
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse and verify
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);
    assertTrue(ast instanceof OraclePackage);
    
    OraclePackage pkg = (OraclePackage) ast;
    assertEquals(1, pkg.getFunctions().size());
    
    Function function = pkg.getFunctions().get(0);
    assertEquals(1, function.getRecordTypes().size());
    assertEquals("local_record", function.getRecordTypes().get(0).getName());
    
    // Test function PostgreSQL generation includes record type comment
    String functionSQL = function.toPostgre(data, false);
    assertNotNull(functionSQL);
    assertTrue(functionSQL.contains("-- Record type local_record should be created as composite type at schema level"));
    assertTrue(functionSQL.contains("CREATE OR REPLACE FUNCTION TEST_SCHEMA.FUNCTION_RECORD_PKG_complex_function"));
  }

  @Test
  public void testRecordTypeFieldGeneration() {
    // Test that record type fields are properly converted to PostgreSQL
    Everything data = new Everything();
    
    // Create a record type with various field types
    RecordType.RecordField field1 = new RecordType.RecordField("id", 
        new DataTypeSpec("NUMBER", null, null, null), false, null);
    RecordType.RecordField field2 = new RecordType.RecordField("name", 
        new DataTypeSpec("VARCHAR2", null, null, null), true, null);
    RecordType.RecordField field3 = new RecordType.RecordField("created_date", 
        new DataTypeSpec("DATE", null, null, null), false, 
        new Expression(new LogicalExpression(new UnaryLogicalExpression("CURRENT_TIMESTAMP"))));
    
    RecordType recordType = new RecordType("test_record", java.util.Arrays.asList(field1, field2, field3));
    
    String postgresSQL = recordType.toPostgre(data);
    
    assertNotNull(postgresSQL);
    assertTrue(postgresSQL.contains("CREATE TYPE test_record AS"));
    assertTrue(postgresSQL.contains("id numeric"));
    assertTrue(postgresSQL.contains("name text"));
    assertTrue(postgresSQL.contains("created_date timestamp"));
    
    // Test field properties
    assertEquals("id", field1.getName());
    assertFalse(field1.isNotNull());
    
    assertEquals("name", field2.getName());
    assertTrue(field2.isNotNull());
    
    assertEquals("created_date", field3.getName());
    assertNotNull(field3.getDefaultValue());
  }
}