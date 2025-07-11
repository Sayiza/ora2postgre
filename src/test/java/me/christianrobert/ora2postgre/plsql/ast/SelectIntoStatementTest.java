package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

public class SelectIntoStatementTest {

  @Test
  public void testSimpleSelectIntoStatementToPostgre() {
    // Test Oracle function with simple SELECT INTO statement
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION getemployeename( pId number ) 
    return varchar2
  is 
    vName varchar2(100);
  begin 
    select first_name into vName from employees where employee_id = pId;
    return vName;
  end;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    OraclePackage o = (OraclePackage) ast;

    // Convert to PostgreSQL
    String postgreSql = o.getFunctions().get(0).toPostgre(data, false);

    // Basic validation - should contain key PostgreSQL SELECT INTO elements
    assert postgreSql.contains("SELECT") : "Should contain SELECT keyword";
    assert postgreSql.contains("first_name") : "Should contain column name";
    assert postgreSql.contains("INTO") : "Should contain INTO keyword";
    assert postgreSql.contains("vName") : "Should contain variable name";
    assert postgreSql.contains("FROM") : "Should contain FROM keyword";
    assert postgreSql.contains("EMPLOYEES") : "Should contain table name";
    assert postgreSql.contains("WHERE") : "Should contain WHERE keyword";
  }

  @Test
  public void testSelectIntoMultipleColumnsToPostgre() {
    // Test Oracle function with SELECT INTO for multiple columns
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION getemployeeinfo( pId number ) 
    return varchar2
  is 
    vName varchar2(100);
    vSalary number;
  begin 
    select first_name, salary into vName, vSalary from employees where employee_id = pId;
    return vName || ' - ' || vSalary;
  end;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    OraclePackage o = (OraclePackage) ast;

    // Convert to PostgreSQL
    String postgreSql = o.getFunctions().get(0).toPostgre(data, false);

    // Basic validation - should contain key PostgreSQL SELECT INTO elements
    assert postgreSql.contains("SELECT") : "Should contain SELECT keyword";
    assert postgreSql.contains("first_name") : "Should contain first column name";
    assert postgreSql.contains("salary") : "Should contain second column name";
    assert postgreSql.contains("INTO") : "Should contain INTO keyword";
    assert postgreSql.contains("vName") : "Should contain first variable name";
    assert postgreSql.contains("vSalary") : "Should contain second variable name";
    assert postgreSql.contains("FROM") : "Should contain FROM keyword";
    assert postgreSql.contains("TEST_SCHEMA.EMPLOYEES") : "Should contain schema and table name";
  }

  @Test
  public void testSelectIntoWithSchemaToPostgre() {
    // Test Oracle function with SELECT INTO using schema prefix
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION getconfig( pKey varchar2 ) 
    return varchar2
  is 
    vValue varchar2(500);
  begin 
    select config_value into vValue from TEST_SCHEMA.config_table where config_key = pKey;
    return vValue;
  end;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    OraclePackage o = (OraclePackage) ast;

    // Convert to PostgreSQL
    String postgreSql = o.getFunctions().get(0).toPostgre(data, false);

    // Basic validation - should contain key PostgreSQL SELECT INTO elements
    assert postgreSql.contains("SELECT") : "Should contain SELECT keyword";
    assert postgreSql.contains("config_value") : "Should contain column name";
    assert postgreSql.contains("INTO") : "Should contain INTO keyword";
    assert postgreSql.contains("vValue") : "Should contain variable name";
    assert postgreSql.contains("FROM") : "Should contain FROM keyword";
    assert postgreSql.contains("TEST_SCHEMA.CONFIG_TABLE") : "Should contain schema and table name";
    assert postgreSql.contains("WHERE") : "Should contain WHERE keyword";
  }

  @Test
  public void testSelectIntoWithoutWhereToPostgre() {
    // Test Oracle function with SELECT INTO without WHERE clause
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION getcount 
    return number
  is 
    vCount number;
  begin 
    select count(*) into vCount from employees;
    return vCount;
  end;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    OraclePackage o = (OraclePackage) ast;

    // Convert to PostgreSQL
    String postgreSql = o.getFunctions().get(0).toPostgre(data, false);

    // Basic validation - should contain key PostgreSQL SELECT INTO elements
    assert postgreSql.contains("SELECT") : "Should contain SELECT keyword";
    assert postgreSql.contains("count(*)") : "Should contain count function";
    assert postgreSql.contains("INTO") : "Should contain INTO keyword";
    assert postgreSql.contains("vCount") : "Should contain variable name";
    assert postgreSql.contains("FROM") : "Should contain FROM keyword";
    assert postgreSql.contains("EMPLOYEES") : "Should contain table name";
    assert !postgreSql.contains("WHERE") : "Should NOT contain WHERE keyword";
  }

  @Test
  public void testSelectIntoWithIfToPostgre() {
    // Test Oracle function with SELECT INTO inside IF statement
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION getemployeestatus( pId number, pCheckActive varchar2 ) 
    return varchar2
  is 
    vName varchar2(100);
    vStatus varchar2(50);
  begin 
    if pCheckActive = 'Y' then
      select first_name into vName from employees where employee_id = pId and status = 'ACTIVE';
    else
      select first_name into vName from employees where employee_id = pId;
    end if;
    return vName;
  end;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    OraclePackage o = (OraclePackage) ast;

    // Convert to PostgreSQL
    String postgreSql = o.getFunctions().get(0).toPostgre(data, false);

    // Basic validation - should contain combination of IF and SELECT INTO
    assert postgreSql.contains("IF") : "Should contain IF keyword";
    assert postgreSql.contains("ELSE") : "Should contain ELSE keyword";
    assert postgreSql.contains("SELECT") : "Should contain SELECT keyword";
    assert postgreSql.contains("first_name") : "Should contain column name";
    assert postgreSql.contains("INTO") : "Should contain INTO keyword";
    assert postgreSql.contains("vName") : "Should contain variable name";
    assert postgreSql.contains("FROM") : "Should contain FROM keyword";
    assert postgreSql.contains("EMPLOYEES") : "Should contain table name";
    assert postgreSql.contains("'ACTIVE'") : "Should contain status filter";
  }

  @Test
  public void testSelectIntoStatementDebug() {
    // Debug test to see what's actually being generated
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION debugselectinto( pId number ) 
    return varchar2
  is 
    vName varchar2(100);
  begin 
    select first_name into vName from employees where employee_id = pId;
    return vName;
  end;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    if (ast instanceof OraclePackage) {
      OraclePackage pkg = (OraclePackage) ast;
      if (!pkg.getFunctions().isEmpty()) {
        Function func = pkg.getFunctions().get(0);
        System.out.println("Function statements count: " + func.getStatements().size());

        for (int i = 0; i < func.getStatements().size(); i++) {
          Statement stmt = func.getStatements().get(i);
          System.out.println("Statement " + i + ": " + stmt.getClass().getSimpleName() + " - " + stmt.toString());
        }

        // Convert to PostgreSQL
        String postgreSql = func.toPostgre(data, false);
        System.out.println("Generated PostgreSQL:");
        System.out.println(postgreSql);
      }
    }
  }
}