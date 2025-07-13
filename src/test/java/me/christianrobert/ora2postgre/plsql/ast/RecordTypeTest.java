package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.oracledb.ColumnMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RecordTypeTest {

  @Test
  public void testBasicRecordTypeDeclaration() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.RECORD_TEST_PKG is  
  TYPE emp_record IS RECORD (
    emp_id NUMBER,
    emp_name VARCHAR2(100),
    hire_date DATE DEFAULT SYSDATE
  );
  
  FUNCTION test_function RETURN NUMBER IS
    v_emp emp_record;
  BEGIN
    v_emp.emp_id := 123;
    v_emp.emp_name := 'John Doe';
    RETURN v_emp.emp_id;
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

    assertNotNull(ast);
    assertTrue(ast instanceof OraclePackage);
    
    OraclePackage pkg = (OraclePackage) ast;
    assertEquals("RECORD_TEST_PKG", pkg.getName());
    assertEquals(1, pkg.getFunctions().size());
    
    Function function = pkg.getFunctions().get(0);
    assertEquals("test_function", function.getName());
    
    // Note: The type declaration and variable usage would be parsed as part of the function/package
    // For now, we test that the package parses without errors
  }

  @Test
  public void testRowTypeDeclaration() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.ROWTYPE_TEST_PKG is  
  FUNCTION get_employee_data RETURN NUMBER IS
    v_emp employees%ROWTYPE;
    v_dept TEST_SCHEMA.departments%ROWTYPE;
  BEGIN
    v_emp.employee_id := 100;
    v_dept.department_id := 10;
    RETURN v_emp.employee_id + v_dept.department_id;
  END;
end;
/
""";

    // Create test data with table metadata
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");
    
    // Add table metadata for %ROWTYPE resolution
    TableMetadata empTable = new TableMetadata("TEST_SCHEMA", "employees");
    empTable.addColumn(new ColumnMetadata("employee_id", "NUMBER", null, 10, 0, true, null));
    empTable.addColumn(new ColumnMetadata("first_name", "VARCHAR2", 50, null, null, false, null));
    empTable.addColumn(new ColumnMetadata("last_name", "VARCHAR2", 50, null, null, false, null));
    empTable.addColumn(new ColumnMetadata("hire_date", "DATE", null, null, null, false, null));
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
    assertEquals("ROWTYPE_TEST_PKG", pkg.getName());
    assertEquals(1, pkg.getFunctions().size());
  }

  @Test
  public void testColumnTypeDeclaration() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.COLTYPE_TEST_PKG is  
  FUNCTION test_column_types RETURN NUMBER IS
    v_emp_id employees.employee_id%TYPE;
    v_name TEST_SCHEMA.employees.first_name%TYPE;
  BEGIN
    v_emp_id := 100;
    v_name := 'John';
    RETURN v_emp_id;
  END;
end;
/
""";

    // Create test data with table metadata
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");
    
    // Add table metadata for %TYPE resolution
    TableMetadata empTable = new TableMetadata("TEST_SCHEMA", "employees");
    empTable.addColumn(new ColumnMetadata("employee_id", "NUMBER", null, 10, 0, true, null));
    empTable.addColumn(new ColumnMetadata("first_name", "VARCHAR2", 50, null, null, false, null));
    data.getTableSql().add(empTable);

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle package
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast);
    assertTrue(ast instanceof OraclePackage);
    
    OraclePackage pkg = (OraclePackage) ast;
    assertEquals("COLTYPE_TEST_PKG", pkg.getName());
    assertEquals(1, pkg.getFunctions().size());
  }

  @Test
  public void testRecordTypePostgreSQLGeneration() {
    // Test direct AST creation and PostgreSQL generation
    Everything data = new Everything();
    
    // Create record type with fields
    List<RecordType.RecordField> fields = Arrays.asList(
      new RecordType.RecordField("emp_id", 
        new DataTypeSpec("NUMBER", null, null, null), false, null),
      new RecordType.RecordField("emp_name", 
        new DataTypeSpec("VARCHAR2", null, null, null), true, null),
      new RecordType.RecordField("hire_date", 
        new DataTypeSpec("DATE", null, null, null), false, 
        new Expression(new LogicalExpression(new UnaryLogicalExpression("SYSDATE"))))
    );
    
    RecordType recordType = new RecordType("emp_record", fields);
    
    // Test PostgreSQL generation
    String postgresCode = recordType.toPostgre(data);
    
    assertNotNull(postgresCode);
    assertTrue(postgresCode.contains("CREATE TYPE emp_record AS"));
    assertTrue(postgresCode.contains("emp_id"));
    assertTrue(postgresCode.contains("emp_name"));
    assertTrue(postgresCode.contains("hire_date"));
    assertTrue(postgresCode.contains("numeric")); // NUMBER should be converted
    assertTrue(postgresCode.contains("text")); // VARCHAR2 should be converted
    assertTrue(postgresCode.contains("timestamp")); // DATE should be converted
    
    // Test field properties
    assertEquals("emp_record", recordType.getName());
    assertEquals(3, recordType.getFields().size());
    
    RecordType.RecordField firstField = recordType.getFields().get(0);
    assertEquals("emp_id", firstField.getName());
    assertFalse(firstField.isNotNull());
    
    RecordType.RecordField secondField = recordType.getFields().get(1);
    assertEquals("emp_name", secondField.getName());
    assertTrue(secondField.isNotNull());
  }

  @Test
  public void testRecordTypeSpecPostgreSQLGeneration() {
    // Test RecordTypeSpec for %ROWTYPE resolution
    Everything data = new Everything();
    
    // Add table metadata
    TableMetadata empTable = new TableMetadata("TEST_SCHEMA", "employees");
    empTable.addColumn(new ColumnMetadata("employee_id", "NUMBER", null, 10, 0, true, null));
    empTable.addColumn(new ColumnMetadata("first_name", "VARCHAR2", 50, null, null, false, null));
    empTable.addColumn(new ColumnMetadata("salary", "NUMBER", null, 10, 2, false, null));
    data.getTableSql().add(empTable);
    
    // Test %ROWTYPE
    RecordTypeSpec rowtypeSpec = RecordTypeSpec.forRowType("TEST_SCHEMA", "employees");
    assertTrue(rowtypeSpec.isRowType());
    assertFalse(rowtypeSpec.isColumnType());
    assertFalse(rowtypeSpec.isRecordType());
    
    String pgType = rowtypeSpec.toPostgre(data);
    assertTrue(pgType.contains("test_schema_employees_rowtype"));
    
    // Test composite type generation
    String compositeTypeDef = rowtypeSpec.generateCompositeTypeDefinition(data);
    assertNotNull(compositeTypeDef);
    assertTrue(compositeTypeDef.contains("CREATE TYPE test_schema_employees_rowtype AS"));
    assertTrue(compositeTypeDef.contains("employee_id"));
    assertTrue(compositeTypeDef.contains("first_name"));
    assertTrue(compositeTypeDef.contains("salary"));
    
    // Test %TYPE
    RecordTypeSpec typeSpec = RecordTypeSpec.forColumnType("TEST_SCHEMA", "employees", "employee_id");
    assertTrue(typeSpec.isColumnType());
    assertFalse(typeSpec.isRowType());
    assertFalse(typeSpec.isRecordType());
    
    String pgColumnType = typeSpec.toPostgre(data);
    assertTrue(pgColumnType.contains("numeric")); // NUMBER should convert to NUMERIC
  }

  @Test
  public void testComplexRecordScenario() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.COMPLEX_RECORD_PKG is  
  TYPE address_rec IS RECORD (
    street VARCHAR2(100),
    city VARCHAR2(50),
    zip_code VARCHAR2(10)
  );
  
  TYPE employee_rec IS RECORD (
    emp_id NUMBER,
    name VARCHAR2(100),
    address address_rec,
    department departments%ROWTYPE
  );
  
  FUNCTION process_employee(p_emp_id NUMBER) RETURN VARCHAR2 IS
    v_employee employee_rec;
    v_temp_emp employees%ROWTYPE;
  BEGIN
    v_employee.emp_id := p_emp_id;
    v_employee.name := 'Test Employee';
    v_employee.address.street := '123 Main St';
    
    SELECT * INTO v_temp_emp FROM employees WHERE employee_id = p_emp_id;
    
    RETURN v_employee.name;
  END;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");
    
    // Add required table metadata
    TableMetadata empTable = new TableMetadata("TEST_SCHEMA", "employees");
    empTable.addColumn(new ColumnMetadata("employee_id", "NUMBER", null, 10, 0, true, null));
    empTable.addColumn(new ColumnMetadata("first_name", "VARCHAR2", 50, null, null, false, null));
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
    assertEquals("COMPLEX_RECORD_PKG", pkg.getName());
    assertEquals(1, pkg.getFunctions().size());
    
    Function function = pkg.getFunctions().get(0);
    assertEquals("process_employee", function.getName());
    
    // Test that the function has statements (record field assignments, SELECT INTO, etc.)
    assertNotNull(function.getStatements());
    assertTrue(function.getStatements().size() > 0);
  }
}