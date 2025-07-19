package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Oracle varray .LIMIT transformation in various boolean expression patterns.
 * Tests the complete transformation from Oracle .LIMIT syntax to PostgreSQL boolean expressions.
 */
public class VarrayLimitTransformationTest {

  @Test
  public void testLimitOnRightSideComparisons() {
    // Test various patterns where .LIMIT is on the right side of comparison
    String oracleSql = """
CREATE OR REPLACE PACKAGE BODY test_schema.limit_test AS
  PROCEDURE test_right_limit IS
  BEGIN
    -- Case 1: something < arr.LIMIT (should become TRUE)
    IF arr_count < arr.LIMIT THEN
      result := 'less than limit';
    END IF;
    
    -- Case 2: something > arr.LIMIT (should become FALSE) 
    IF arr_count > arr.LIMIT THEN
      result := 'greater than limit';
    END IF;
    
    -- Case 3: something = arr.LIMIT (should become FALSE)
    IF arr_count = arr.LIMIT THEN
      result := 'equals limit';
    END IF;
    
    -- Case 4: something >= arr.LIMIT (should become FALSE)
    IF arr_count >= arr.LIMIT THEN
      result := 'greater or equal to limit';
    END IF;
  END test_right_limit;
END limit_test;
/
""";

    Everything data = new Everything();
    data.getUserNames().add("test_schema");

    PlsqlCode plsqlCode = new PlsqlCode("test_schema", oracleSql);
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast, "AST parsing should not return null");
    
    if (ast instanceof OraclePackage) {
      OraclePackage pkg = (OraclePackage) ast;
      String postgreResult = pkg.toPostgre(data, false);
      assertNotNull(postgreResult, "toPostgre() should not return null");
      
      // Verify transformations
      assertTrue(postgreResult.contains("IF TRUE /* Oracle varray limit check"), 
                "Should transform '< arr.LIMIT' to TRUE");
      assertTrue(postgreResult.contains("IF FALSE /* Oracle varray limit check"), 
                "Should transform '> arr.LIMIT' to FALSE");
      
      System.out.println("Right-side LIMIT transformations:");
      System.out.println(postgreResult);
    }
  }

  @Test
  public void testLimitOnLeftSideComparisons() {
    // Test patterns where .LIMIT is on the left side of comparison
    String oracleSql = """
CREATE OR REPLACE PACKAGE BODY test_schema.limit_test AS
  PROCEDURE test_left_limit IS
  BEGIN
    -- Case 1: arr.LIMIT > something (should become TRUE)
    IF arr.LIMIT > current_size THEN
      result := 'limit greater than size';
    END IF;
    
    -- Case 2: arr.LIMIT < something (should become FALSE)
    IF arr.LIMIT < current_size THEN
      result := 'limit less than size';
    END IF;
    
    -- Case 3: arr.LIMIT = something (should become FALSE)
    IF arr.LIMIT = current_size THEN
      result := 'limit equals size';
    END IF;
  END test_left_limit;
END limit_test;
/
""";

    Everything data = new Everything();
    data.getUserNames().add("test_schema");

    PlsqlCode plsqlCode = new PlsqlCode("test_schema", oracleSql);
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast, "AST parsing should not return null");
    
    if (ast instanceof OraclePackage) {
      OraclePackage pkg = (OraclePackage) ast;
      String postgreResult = pkg.toPostgre(data, false);
      assertNotNull(postgreResult, "toPostgre() should not return null");
      
      // Verify transformations  
      assertTrue(postgreResult.contains("IF TRUE /* Oracle varray limit check"), 
                "Should transform 'arr.LIMIT >' to TRUE");
      assertTrue(postgreResult.contains("IF FALSE /* Oracle varray limit check"), 
                "Should transform 'arr.LIMIT <' and 'arr.LIMIT =' to FALSE");
      
      System.out.println("Left-side LIMIT transformations:");
      System.out.println(postgreResult);
    }
  }

  @Test
  public void testLimitInComplexExpressions() {
    // Test .LIMIT in more complex boolean expressions
    String oracleSql = """
CREATE OR REPLACE PACKAGE BODY test_schema.limit_test AS
  PROCEDURE test_complex_limit IS
  BEGIN
    -- Case 1: Combined with AND/OR (this tests the outer logical expression handling)
    IF (arr_count < arr.LIMIT) AND (other_condition = true) THEN
      result := 'complex condition met';
    END IF;
    
    -- Case 2: Inequality operator
    IF arr_count <> arr.LIMIT THEN
      result := 'not equal to limit';
    END IF;
    
    -- Case 3: Less than or equal
    IF arr_count <= arr.LIMIT THEN
      result := 'less than or equal to limit';
    END IF;
  END test_complex_limit;
END limit_test;
/
""";

    Everything data = new Everything();
    data.getUserNames().add("test_schema");

    PlsqlCode plsqlCode = new PlsqlCode("test_schema", oracleSql);
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast, "AST parsing should not return null");
    
    if (ast instanceof OraclePackage) {
      OraclePackage pkg = (OraclePackage) ast;
      String postgreResult = pkg.toPostgre(data, false);
      assertNotNull(postgreResult, "toPostgre() should not return null");
      
      // Verify transformations
      assertTrue(postgreResult.contains("TRUE /* Oracle varray limit check"), 
                "Should contain TRUE transformations for < and <= operations");
      assertTrue(postgreResult.contains("/* Oracle varray limit check"), 
                "Should contain limit transformation comments");
      
      System.out.println("Complex LIMIT expressions:");
      System.out.println(postgreResult);
    }
  }

  @Test
  public void testOriginalVarrayExample() {
    // Re-test the original problematic case to ensure it's fixed
    String oracleSql = """
CREATE OR REPLACE PACKAGE BODY user_robert.pkg_varray_example AS
  PROCEDURE add_number(p_number IN NUMBER) IS
  BEGIN
    IF g_numbers IS NULL THEN
      g_numbers := t_numbers();
    END IF;
    
    IF g_numbers.COUNT < g_numbers.LIMIT THEN
      g_numbers.EXTEND;
      g_numbers(g_numbers.COUNT) := p_number;
    ELSE
      RAISE_APPLICATION_ERROR(-20001, 'Varray limit reached');
    END IF;
  END add_number;
END pkg_varray_example;
/
""";

    Everything data = new Everything();
    data.getUserNames().add("user_robert");

    PlsqlCode plsqlCode = new PlsqlCode("user_robert", oracleSql);
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    assertNotNull(ast, "AST parsing should not return null");
    
    if (ast instanceof OraclePackage) {
      OraclePackage pkg = (OraclePackage) ast;
      String postgreResult = pkg.toPostgre(data, false);
      assertNotNull(postgreResult, "toPostgre() should not return null");
      
      // Verify the problematic line is fixed
      assertFalse(postgreResult.contains("/* LIMIT - no direct PostgreSQL equivalent"), 
                 "Should not contain the old LIMIT comment causing syntax error");
      assertTrue(postgreResult.contains("IF TRUE /* Oracle varray limit check"), 
                "Should transform 'COUNT < LIMIT' to TRUE");
      
      // Verify it's valid PostgreSQL syntax (no incomplete expressions)
      assertFalse(postgreResult.contains(" < /* "), 
                 "Should not have incomplete comparison expressions");
      
      System.out.println("Fixed original varray example:");
      System.out.println(postgreResult);
    }
  }
}