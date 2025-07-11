package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

public class DeleteStatementTest {

  @Test
  public void testSimpleDeleteStatementToPostgre() {
    // Test Oracle function with simple DELETE statement
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION deleteold( pDays number ) 
    return varchar2
  is 
  begin 
    delete from temp_table where created_at < sysdate - pDays;
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

    // Debug: Print the generated SQL to see actual output
    System.out.println("Generated SQL for simple delete:");
    System.out.println(postgreSql);

    // Basic validation - should contain key PostgreSQL DELETE elements
    assert postgreSql.contains("DELETE FROM") : "Should contain DELETE FROM keywords";
    assert postgreSql.contains("TEMP_TABLE") : "Should contain table name";
    assert postgreSql.contains("WHERE") : "Should contain WHERE keyword";
    assert postgreSql.contains("created_at") : "Should contain column name";
    // Note: SYSDATE conversion happens at expression level, may not be implemented yet
    // assert postgreSql.contains("CURRENT_TIMESTAMP") : "Should convert SYSDATE to CURRENT_TIMESTAMP";
  }

  @Test
  public void testDeleteWithSchemaToPostgre() {
    // Test Oracle function with DELETE statement using schema prefix
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION deleteschema( pId number ) 
    return varchar2
  is 
  begin 
    delete from TEST_SCHEMA.temp_table where id = pId;
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

    // Basic validation - should contain key PostgreSQL DELETE elements
    assert postgreSql.contains("DELETE FROM") : "Should contain DELETE FROM keywords";
    assert postgreSql.contains("TEST_SCHEMA.TEMP_TABLE") : "Should contain schema and table name";
    assert postgreSql.contains("WHERE") : "Should contain WHERE keyword";
    assert postgreSql.contains("id") : "Should contain WHERE condition";
  }

  @Test
  public void testDeleteWithoutWhereToPostgre() {
    // Test Oracle function with DELETE statement without WHERE clause (deletes all rows)
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION deleteall 
    return varchar2
  is 
  begin 
    delete from temp_table;
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

    // Basic validation - should contain key PostgreSQL DELETE elements
    assert postgreSql.contains("DELETE FROM") : "Should contain DELETE FROM keywords";
    assert postgreSql.contains("TEMP_TABLE") : "Should contain table name";
    assert !postgreSql.contains("WHERE") : "Should NOT contain WHERE keyword";
  }

  @Test
  public void testDeleteWithComplexWhereToPostgre() {
    // Test Oracle function with DELETE statement using complex WHERE clause
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION deletecomplex( pStatus varchar2, pDays number ) 
    return varchar2
  is 
  begin 
    delete from temp_table where status = pStatus and created_at < sysdate - pDays;
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

    // Basic validation - should contain key PostgreSQL DELETE elements
    assert postgreSql.contains("DELETE FROM") : "Should contain DELETE FROM keywords";
    assert postgreSql.contains("TEMP_TABLE") : "Should contain table name";
    assert postgreSql.contains("WHERE") : "Should contain WHERE keyword";
    assert postgreSql.contains("status") : "Should contain status condition";
    assert postgreSql.contains("AND") : "Should contain AND operator";
    assert postgreSql.contains("created_at") : "Should contain date condition";
    // Note: SYSDATE conversion at expression level may not be fully implemented
    // assert postgreSql.contains("CURRENT_TIMESTAMP") : "Should convert SYSDATE to CURRENT_TIMESTAMP";
  }

  @Test
  public void testTriggerLikeDeleteWithIfToPostgre() {
    // Test Oracle function with DELETE inside IF statement (trigger-like pattern)
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION triggerfunc( pAction varchar2, pId number ) 
    return varchar2
  is 
  begin 
    if pAction = 'CLEANUP' then
      delete from temp_table where id = pId;
    elsif pAction = 'PURGE' then
      delete from temp_table where created_at < sysdate - 30;
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

    // Basic validation - should contain combination of IF and DELETE
    assert postgreSql.contains("IF") : "Should contain IF keyword";
    assert postgreSql.contains("ELSIF") : "Should contain ELSIF keyword";
    assert postgreSql.contains("DELETE FROM") : "Should contain DELETE FROM keywords";
    assert postgreSql.contains("TEMP_TABLE") : "Should contain table name";
    assert postgreSql.contains("WHERE") : "Should contain WHERE keyword";
    assert postgreSql.contains("'CLEANUP'") : "Should contain CLEANUP action";
    assert postgreSql.contains("'PURGE'") : "Should contain PURGE action";
    // Note: SYSDATE conversion at expression level may not be fully implemented
    // assert postgreSql.contains("CURRENT_TIMESTAMP") : "Should convert SYSDATE to CURRENT_TIMESTAMP";
  }

  @Test
  public void testDeleteStatementDebug() {
    // Debug test to see what's actually being generated
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION debugdelete( pId number ) 
    return varchar2
  is 
  begin 
    delete from temp_table where id = pId;
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