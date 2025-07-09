package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

public class IfStatementDebugTest {

  @Test
  public void debugIfStatementParsing() {
    // Test Oracle function with simple IF statement
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION testifsimple( pId number ) 
    return varchar2
  is 
    vResult varchar2(100);
  begin 
    if pId > 0 then
      vResult := 'Positive';
    end if;
    return vResult;
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

    System.out.println("AST Type: " + ast.getClass().getSimpleName());

    if (ast instanceof OraclePackage) {
      OraclePackage pkg = (OraclePackage) ast;
      System.out.println("Package name: " + pkg.getName());
      System.out.println("Functions count: " + pkg.getFunctions().size());

      if (!pkg.getFunctions().isEmpty()) {
        Function func = pkg.getFunctions().get(0);
        System.out.println("Function name: " + func.getName());
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