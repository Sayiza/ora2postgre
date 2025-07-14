package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

public class BulkCollectStatementTest {

  @Test
  public void testSimpleBulkCollectStatementToPostgre() {
    // Test Oracle function with simple BULK COLLECT INTO statement
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION getemployeenames 
    return varchar2
  is 
    vNames string_array := string_array();
  begin 
    select first_name bulk collect into vNames from employees;
    return 'Found ' || vNames.COUNT || ' employees';
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

    // Basic validation - should contain key PostgreSQL BULK COLLECT elements
    assert postgreSql.contains("vNames := ARRAY(") : "Should contain array assignment with ARRAY()";
    assert postgreSql.contains("SELECT first_name") : "Should contain SELECT column";
    assert postgreSql.contains("FROM") : "Should contain FROM keyword";
    assert postgreSql.contains("EMPLOYEES") : "Should contain table name";
    
    System.out.println("Generated PostgreSQL:");
    System.out.println(postgreSql);
  }

  @Test
  public void testBulkCollectMultipleColumnsToPostgre() {
    // Test Oracle function with BULK COLLECT INTO for multiple columns
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION getemployeedata 
    return varchar2
  is 
    vNames string_array := string_array();
    vSalaries number_table := number_table();
  begin 
    select first_name, salary bulk collect into vNames, vSalaries from employees where department_id = 10;
    return 'Found ' || vNames.COUNT || ' employees';
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

    // Basic validation - should contain separate array assignments
    assert postgreSql.contains("vNames := ARRAY(") : "Should contain first array assignment";
    assert postgreSql.contains("vSalaries := ARRAY(") : "Should contain second array assignment";
    assert postgreSql.contains("SELECT first_name") : "Should contain first column";
    assert postgreSql.contains("SELECT salary") : "Should contain second column";
    assert postgreSql.contains("WHERE") : "Should contain WHERE keyword";
    assert postgreSql.contains("department_id = 10") : "Should contain WHERE condition";
    
    System.out.println("Generated PostgreSQL:");
    System.out.println(postgreSql);
  }

  @Test
  public void testBulkCollectWithSchemaToPostgre() {
    // Test Oracle function with BULK COLLECT using schema prefix
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION getconfigs 
    return varchar2
  is 
    vKeys string_array := string_array();
  begin 
    select config_key bulk collect into vKeys from TEST_SCHEMA.config_table where status = 'ACTIVE';
    return 'Found ' || vKeys.COUNT || ' configs';
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

    // Basic validation - should contain key PostgreSQL BULK COLLECT elements
    assert postgreSql.contains("vKeys := ARRAY(") : "Should contain array assignment";
    assert postgreSql.contains("SELECT config_key") : "Should contain column name";
    assert postgreSql.contains("FROM") : "Should contain FROM keyword";
    assert postgreSql.contains("TEST_SCHEMA.CONFIG_TABLE") : "Should contain schema and table name";
    assert postgreSql.contains("WHERE") : "Should contain WHERE keyword";
    assert postgreSql.contains("status = 'ACTIVE'") : "Should contain WHERE condition";
    
    System.out.println("Generated PostgreSQL:");
    System.out.println(postgreSql);
  }

  @Test
  public void testBulkCollectDebugStatementParsing() {
    // Debug test to see what statement type is being created
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION debugbulkcollect 
    return varchar2
  is 
    vNames string_array := string_array();
  begin 
    select first_name bulk collect into vNames from employees;
    return 'Found ' || vNames.COUNT || ' employees';
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
          
          // Check if it's our new BulkCollectStatement
          if (stmt instanceof BulkCollectStatement) {
            BulkCollectStatement bulkStmt = (BulkCollectStatement) stmt;
            System.out.println("  - BULK COLLECT detected!");
            System.out.println("  - Columns: " + bulkStmt.getSelectedColumns());
            System.out.println("  - Arrays: " + bulkStmt.getIntoArrays());
            System.out.println("  - Table: " + bulkStmt.getTableName());
            System.out.println("  - Schema: " + bulkStmt.getSchemaName());
          }
        }

        // Convert to PostgreSQL
        String postgreSql = func.toPostgre(data, false);
        System.out.println("Generated PostgreSQL:");
        System.out.println(postgreSql);
      }
    }
  }
}