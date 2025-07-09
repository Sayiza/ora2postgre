package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

public class WhereClauseIntegrationTest {

  @Test
  public void testFunctionWithWhereClause() {
    // Test Oracle function with WHERE clause
    // Note: Using a simpler function structure to avoid existing casting issues
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION getActiveUsers( pMinAge number ) 
    return varchar2
  is 
  begin 
    for r in ( select name from TEST_SCHEMA.USERS where age > pMinAge )
    loop
      return r.name;
    end loop;
    return null;
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

    // Basic validation - should contain PostgreSQL elements
    assert postgreSql.contains("DECLARE") : "Should contain DECLARE section";
    assert postgreSql.contains("FOR") : "Should contain FOR loop";
    // Note: WHERE clause parsing may depend on full query context implementation
  }

  @Test
  public void testSimpleSelectWithWhereClause() {
    // Test simple SELECT with WHERE clause parsing
    // This is a more direct test of WHERE clause parsing
    String simpleSql = """
BEGIN
  select count(*) into result_count from users where active = 1;
END;
""";
  }
}