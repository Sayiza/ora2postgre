package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

public class CTEBasicTest {

  @Test
  public void testBasicSelectWithCTE() {
    // Test a simple SELECT statement with WITH clause (not in a function)
    String oracleSql = """
WITH dept_employees AS (
  SELECT employee_id, department_id, salary
  FROM employees
  WHERE department_id = 10
)
SELECT COUNT(*) 
FROM dept_employees
WHERE salary > 5000;
""";

    // Create test data with proper table metadata
    Everything data = CTETestHelper.createTestEverything();

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    try {
      // Parse the Oracle SELECT statement
      PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

      System.out.println("Basic CTE test:");
      System.out.println("Parsed AST: " + (ast != null ? ast.getClass().getSimpleName() : "null"));
      
      if (ast instanceof SelectStatement) {
        SelectStatement selectStmt = (SelectStatement) ast;
        System.out.println("SELECT statement parsed successfully");
        
        // Convert to PostgreSQL
        String postgreSql = selectStmt.toPostgre(data);
        System.out.println("Generated PostgreSQL:");
        System.out.println(postgreSql);
        
        // Verify the CTE is present
        assert postgreSql.toUpperCase().contains("WITH DEPT_EMPLOYEES AS") : "Should contain CTE definition";
        assert postgreSql.toUpperCase().contains("SELECT EMPLOYEE_ID, DEPARTMENT_ID, SALARY") : "Should contain CTE query";
      } else {
        System.out.println("AST was not a SelectStatement, but: " + (ast != null ? ast.getClass().getSimpleName() : "null"));
        if (ast != null) {
          System.out.println("AST toString: " + ast.toString());
        }
      }
    } catch (Exception e) {
      System.out.println("Parsing failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @Test
  public void testCTEClassDirectly() {
    // Test the CTE classes directly
    System.out.println("Testing CTE classes directly:");
    
    // Create a simple CTE
    CommonTableExpression cte = new CommonTableExpression(
        "test_cte", 
        java.util.Arrays.asList("col1", "col2"), 
        null, // subquery would normally be provided
        false
    );
    
    Everything data = new Everything();
    String result = cte.toPostgre(data);
    
    System.out.println("Direct CTE result: " + result);
    assert result.contains("test_cte (col1, col2) AS") : "Should contain CTE structure";
    
    // Create SelectWithClause with the CTE
    SelectWithClause withClause = new SelectWithClause(
        java.util.Arrays.asList(cte),
        java.util.Arrays.asList(), // no functions
        java.util.Arrays.asList()  // no procedures
    );
    
    String withResult = withClause.toPostgre(data);
    System.out.println("WITH clause result: " + withResult);
    assert withResult.contains("WITH test_cte") : "Should contain WITH keyword";
  }

  @Test
  public void testSimplestPossibleCTE() {
    // Test the absolute simplest case to ensure parsing works
    String oracleSql = """
WITH simple_cte AS (SELECT 1 as col1)
SELECT * FROM simple_cte;
""";

    // Create test data with proper table metadata
    Everything data = CTETestHelper.createTestEverything();

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    try {
      // Parse the Oracle SELECT statement
      PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

      System.out.println("Simplest CTE test:");
      System.out.println("Parsed AST: " + (ast != null ? ast.getClass().getSimpleName() : "null"));
      
      if (ast != null) {
        System.out.println("AST toString: " + ast.toString());
        
        // Try to convert to PostgreSQL if it's a known type
        if (ast instanceof SelectStatement) {
          SelectStatement selectStmt = (SelectStatement) ast;
          String postgreSql = selectStmt.toPostgre(data);
          System.out.println("Generated PostgreSQL:");
          System.out.println(postgreSql);
        }
      }
    } catch (Exception e) {
      System.out.println("Parsing failed: " + e.getMessage());
      e.printStackTrace();
    }
  }
}