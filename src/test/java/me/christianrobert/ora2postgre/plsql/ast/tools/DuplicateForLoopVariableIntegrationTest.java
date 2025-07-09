package me.christianrobert.ora2postgre.plsql.ast.tools;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import me.christianrobert.ora2postgre.plsql.ast.OraclePackage;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import org.junit.jupiter.api.Test;

public class DuplicateForLoopVariableIntegrationTest {

  @Test
  public void testFunctionWithDuplicateForLoopVariables() {
    // Test Oracle function with multiple FOR loops using the same variable name "r"
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION processData( pMinAge number ) 
    return varchar2
  is 
  begin 
    -- First FOR loop with variable "r"
    for r in ( select name from TEST_SCHEMA.USERS where age > pMinAge )
    loop
      return r.name;
    end loop;
    
    -- Second FOR loop with the same variable name "r"
    for r in ( select title from TEST_SCHEMA.JOBS where active = 1 )
    loop
      return r.title;
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

    // Basic validation - should contain only ONE declaration for "r"
    assert postgreSql.contains("DECLARE") : "Should contain DECLARE section";
    assert postgreSql.contains("r RECORD") : "Should contain record declaration for 'r'";

    // Count how many times "r RECORD" appears in the DECLARE section
    String declareSection = postgreSql.substring(
            postgreSql.indexOf("DECLARE"),
            postgreSql.indexOf("BEGIN")
    );

    long recordDeclarationCount = declareSection.lines()
            .filter(line -> line.trim().equals("r RECORD;"))
            .count();

    assert recordDeclarationCount == 1 :
            "Should only declare 'r RECORD' once, but found " + recordDeclarationCount + " declarations";
  }
}