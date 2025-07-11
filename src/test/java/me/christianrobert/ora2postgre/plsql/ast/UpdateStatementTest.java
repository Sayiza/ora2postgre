package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

public class UpdateStatementTest {

  @Test
  public void testSimpleUpdateStatementToPostgre() {
    // Test Oracle function with simple UPDATE statement
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION updatestatus( pId number, pStatus varchar2 ) 
    return varchar2
  is 
  begin 
    update status_table set status = pStatus where id = pId;
    return 'OK';
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

    // Basic validation - should contain key PostgreSQL UPDATE elements
    assert postgreSql.contains("UPDATE") : "Should contain UPDATE keyword";
    assert postgreSql.contains("STATUS_TABLE") : "Should contain table name";
    assert postgreSql.contains("SET") : "Should contain SET keyword";
    assert postgreSql.contains("status = ") : "Should contain SET column assignment";
    assert postgreSql.contains("WHERE") : "Should contain WHERE keyword";
  }

  @Test
  public void testUpdateMultipleColumnsToPostgre() {
    // Test Oracle function with UPDATE statement setting multiple columns
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION updateemployee( pId number, pName varchar2, pSalary number ) 
    return varchar2
  is 
  begin 
    update employees set name = pName, salary = pSalary, updated_at = sysdate where employee_id = pId;
    return 'OK';
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

    // Basic validation - should contain key PostgreSQL UPDATE elements
    assert postgreSql.contains("UPDATE") : "Should contain UPDATE keyword";
    assert postgreSql.contains("TEST_SCHEMA.EMPLOYEES") : "Should contain schema and table name";
    assert postgreSql.contains("SET") : "Should contain SET keyword";
    assert postgreSql.contains("name = ") : "Should contain name column assignment";
    assert postgreSql.contains("salary = ") : "Should contain salary column assignment";
    assert postgreSql.contains("updated_at = ") : "Should contain updated_at column assignment";
    assert postgreSql.contains("CURRENT_TIMESTAMP") : "Should convert SYSDATE to CURRENT_TIMESTAMP";
    assert postgreSql.contains("WHERE") : "Should contain WHERE keyword";
  }

  @Test
  public void testUpdateWithSchemaToPostgre() {
    // Test Oracle function with UPDATE statement using schema prefix
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION updateschema( pId number, pStatus varchar2 ) 
    return varchar2
  is 
  begin 
    update TEST_SCHEMA.status_table set status = pStatus, updated_at = sysdate where id = pId;
    return 'OK';
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

    // Basic validation - should contain key PostgreSQL UPDATE elements
    assert postgreSql.contains("UPDATE") : "Should contain UPDATE keyword";
    assert postgreSql.contains("TEST_SCHEMA.STATUS_TABLE") : "Should contain schema and table name";
    assert postgreSql.contains("SET") : "Should contain SET keyword";
    assert postgreSql.contains("WHERE") : "Should contain WHERE keyword";
  }

  @Test
  public void testUpdateWithoutWhereToPostgre() {
    // Test Oracle function with UPDATE statement without WHERE clause
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION updateall( pStatus varchar2 ) 
    return varchar2
  is 
  begin 
    update status_table set status = pStatus;
    return 'OK';
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

    // Basic validation - should contain key PostgreSQL UPDATE elements
    assert postgreSql.contains("UPDATE") : "Should contain UPDATE keyword";
    assert postgreSql.contains("STATUS_TABLE") : "Should contain table name";
    assert postgreSql.contains("SET") : "Should contain SET keyword";
    assert postgreSql.contains("status = ") : "Should contain SET column assignment";
    assert !postgreSql.contains("WHERE") : "Should NOT contain WHERE keyword";
  }

  @Test
  public void testTriggerLikeUpdateWithIfToPostgre() {
    // Test Oracle function with UPDATE inside IF statement (trigger-like pattern)
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION triggerfunc( pAction varchar2, pId number, pNewStatus varchar2 ) 
    return varchar2
  is 
  begin 
    if pAction = 'UPDATE' then
      update status_table set status = pNewStatus, updated_at = sysdate where id = pId;
    elsif pAction = 'ARCHIVE' then
      update status_table set status = 'ARCHIVED', archived_at = sysdate where id = pId;
    end if;
    return 'OK';
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

    // Basic validation - should contain combination of IF and UPDATE
    assert postgreSql.contains("IF") : "Should contain IF keyword";
    assert postgreSql.contains("ELSIF") : "Should contain ELSIF keyword";
    assert postgreSql.contains("UPDATE") : "Should contain UPDATE keyword";
    assert postgreSql.contains("STATUS_TABLE") : "Should contain table name";
    assert postgreSql.contains("SET") : "Should contain SET keyword";
    assert postgreSql.contains("'ARCHIVED'") : "Should contain ARCHIVED status";
    assert postgreSql.contains("CURRENT_TIMESTAMP") : "Should convert SYSDATE to CURRENT_TIMESTAMP";
    assert postgreSql.contains("WHERE") : "Should contain WHERE keyword";
  }

  @Test
  public void testUpdateStatementDebug() {
    // Debug test to see what's actually being generated
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION debugupdate( pId number, pStatus varchar2 ) 
    return varchar2
  is 
  begin 
    update status_table set status = pStatus, updated_at = sysdate where id = pId;
    return 'OK';
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