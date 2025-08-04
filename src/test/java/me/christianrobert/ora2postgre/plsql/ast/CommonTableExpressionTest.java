package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import me.christianrobert.ora2postgre.services.CTETrackingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

public class CommonTableExpressionTest {

  private Everything data;
  private CTETrackingService cteTrackingService;

  @BeforeEach
  public void setUp() {
    data = CTETestHelper.createTestEverything();
    cteTrackingService = new CTETrackingService();
    
    // Set the test instance so it can be used as fallback when CDI injection is not available
    CTETrackingService.setTestInstance(cteTrackingService);
  }


  @Test
  public void testSimpleCommonTableExpression() {
    // Test Oracle function with simple CTE
    String oracleSql = """
CREATE FUNCTION TEST_SCHEMA.GET_DEPARTMENT_EMPLOYEES 
RETURN NUMBER IS
  v_count NUMBER;
BEGIN
  WITH dept_employees AS (
    SELECT employee_id, department_id, salary
    FROM employees
    WHERE department_id = 10
  )
  SELECT COUNT(*) INTO v_count
  FROM dept_employees
  WHERE salary > 5000;
  
  RETURN v_count;
END;
""";

    // Test data is already set up in @BeforeEach

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    System.out.println("Parsed AST: " + ast.getClass().getSimpleName());
    
    if (ast instanceof Function) {
      Function function = (Function) ast;
      System.out.println("Function name: " + function.getName());
      System.out.println("Function statements count: " + function.getStatements().size());
      
      // Convert to PostgreSQL
      String postgreSql = function.toPostgre(data, false);
      System.out.println("Generated PostgreSQL:");
      System.out.println(postgreSql);
      
      // Verify the CTE is transformed correctly
      assert postgreSql.toUpperCase().contains("WITH DEPT_EMPLOYEES AS") : "Should contain CTE definition";
      assert postgreSql.toUpperCase().contains("SELECT EMPLOYEE_ID, DEPARTMENT_ID, SALARY") : "Should contain CTE query";
      assert postgreSql.toUpperCase().contains("FROM TEST_SCHEMA.EMPLOYEES") : "Should contain CTE FROM clause";
      assert postgreSql.toUpperCase().contains("WHERE DEPARTMENT_ID = 10") : "Should contain CTE WHERE clause";
    }
  }

  @Test
  public void testMultipleCommonTableExpressions() {
    // Test Oracle function with multiple CTEs
    String oracleSql = """
CREATE FUNCTION TEST_SCHEMA.GET_DEPARTMENT_STATS 
RETURN NUMBER IS
  v_result NUMBER;
BEGIN
  WITH dept_employees AS (
    SELECT employee_id, department_id, salary
    FROM employees
    WHERE department_id = 10
  ),
  high_earners AS (
    SELECT employee_id, salary
    FROM dept_employees
    WHERE salary > 5000
  )
  SELECT COUNT(*) INTO v_result
  FROM high_earners;
  
  RETURN v_result;
END;
""";

    // Test data is already set up in @BeforeEach

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    System.out.println("Multiple CTEs test:");
    System.out.println("Parsed AST: " + ast.getClass().getSimpleName());
    
    if (ast instanceof Function) {
      Function function = (Function) ast;
      System.out.println("Function name: " + function.getName());
      
      // Convert to PostgreSQL
      String postgreSql = function.toPostgre(data, false);
      System.out.println("Generated PostgreSQL:");
      System.out.println(postgreSql);
      
      // Verify both CTEs are transformed correctly
      assert postgreSql.toUpperCase().contains("WITH DEPT_EMPLOYEES AS") : "Should contain first CTE";
      assert postgreSql.toUpperCase().contains("HIGH_EARNERS AS") : "Should contain second CTE";
      assert postgreSql.toUpperCase().contains("FROM DEPT_EMPLOYEES") : "Should reference first CTE in second CTE";
      assert postgreSql.toUpperCase().contains("FROM HIGH_EARNERS") : "Should reference second CTE in main query";
    }
  }

  @Test
  public void testCTEWithColumnList() {
    // Test Oracle CTE with explicit column list
    String oracleSql = """
CREATE FUNCTION TEST_SCHEMA.GET_EMPLOYEE_SUMMARY
RETURN NUMBER IS
  v_count NUMBER;
BEGIN
  WITH employee_summary (emp_id, emp_name, emp_salary) AS (
    SELECT employee_id, first_name, salary
    FROM employees
    WHERE department_id = 10
  )
  SELECT COUNT(*) INTO v_count
  FROM employee_summary
  WHERE emp_salary > 3000;
  
  RETURN v_count;
END;
""";

    // Test data is already set up in @BeforeEach

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    System.out.println("CTE with column list test:");
    System.out.println("Parsed AST: " + ast.getClass().getSimpleName());
    
    if (ast instanceof Function) {
      Function function = (Function) ast;
      System.out.println("Function name: " + function.getName());
      
      // Convert to PostgreSQL
      String postgreSql = function.toPostgre(data, false);
      System.out.println("Generated PostgreSQL:");
      System.out.println(postgreSql);
      
      // Verify CTE with column list is transformed correctly
      assert postgreSql.toUpperCase().contains("WITH EMPLOYEE_SUMMARY (EMP_ID, EMP_NAME, EMP_SALARY) AS") : "Should contain CTE with column list";
      assert postgreSql.toUpperCase().contains("SELECT EMPLOYEE_ID, FIRST_NAME, SALARY") : "Should contain CTE query";
      assert postgreSql.toUpperCase().contains("FROM EMPLOYEE_SUMMARY") : "Should reference CTE in main query";
      assert postgreSql.toUpperCase().contains("WHERE EMP_SALARY > 3000") : "Should use CTE column names";
    }
  }

  @Test
  public void testDirectCommonTableExpressionTransformation() {
    // Test direct CTE transformation without function wrapper
    CommonTableExpression cte = new CommonTableExpression(
        "test_cte", 
        java.util.Arrays.asList("col1", "col2"), 
        null, // subquery would normally be provided
        false
    );
    
    String result = cte.toPostgre(data);
    
    System.out.println("Direct CTE transformation:");
    System.out.println("Result: " + result);
    
    // Verify the basic structure
    assert result.contains("test_cte (col1, col2) AS") : "Should contain CTE name and columns";
    assert result.endsWith(")") : "Should end with closing parenthesis";
    
    // Test CTE without column list
    CommonTableExpression cte2 = new CommonTableExpression("simple_cte", null, null, false);
    String result2 = cte2.toPostgre(data);
    
    System.out.println("Simple CTE result: " + result2);
    assert result2.equals("simple_cte AS ()") : "Should contain simple CTE structure";
  }
}