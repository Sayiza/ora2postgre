package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for parsing PL/SQL packages with varray operations.
 * This test addresses a specific parsing issue where varray operations
 * cause the parser to get stuck.
 */
public class VarrayPackageParsingTest {

  @Test
  public void testVarrayPackageBodyParsing() {
    // Test PL/SQL package body with varray operations that was causing parsing problems
    String oracleSql = """
-- Package body
CREATE OR REPLACE PACKAGE BODY user_robert.pkg_varray_example AS
  
  PROCEDURE add_number(p_number IN NUMBER) IS
  BEGIN
    IF g_numbers IS NULL THEN
      g_numbers := t_numbers(4,5,6); -- Initialize varray
    END IF;
    
    IF g_numbers.COUNT < g_numbers.LIMIT THEN
      g_numbers.EXTEND; -- Add new element
      g_numbers(g_numbers.COUNT) := p_number;
    ELSE
      RAISE_APPLICATION_ERROR(-20001, 'Varray limit reached');
    END IF;
  END add_number;
  
  PROCEDURE display_numbers IS
  BEGIN
    IF g_numbers IS NOT NULL AND g_numbers.COUNT > 0 THEN
      FOR i IN 1..g_numbers.COUNT LOOP
        htp.p('Number ' || i || ': ' || g_numbers(i));
      END LOOP;
    ELSE
      htp.p('No numbers in varray');
    END IF;
  END display_numbers;
END pkg_varray_example;
/
""";

    String oracleSqlSpec = """
-- Package body
CREATE OR REPLACE PACKAGE user_robert.pkg_varray_example AS
  TYPE t_numbers IS VARRAY(10) OF NUMBER; -- Varray type definition
  g_numbers t_numbers; -- Package variable
END pkg_varray_example;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("user_robert");

    PlsqlCode plsqlCodeSpec = new PlsqlCode("user_robert", oracleSqlSpec);
    PlsqlCode plsqlCode = new PlsqlCode("user_robert", oracleSql);

    // Parse the Oracle package - this should not get stuck or fail

    PlSqlAst astSpec = PlSqlAstMain.processPlsqlCode(plsqlCodeSpec);
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    // Basic assertion: parsing should succeed and return a non-null result
    assertNotNull(ast, "AST parsing should not return null");
    
    System.out.println("Parsed AST: " + ast.getClass().getSimpleName());

    if (ast instanceof OraclePackage && astSpec instanceof OraclePackage) {
      OraclePackage oracleSpec = (OraclePackage) astSpec;
      OraclePackage pkg = (OraclePackage) ast;

      pkg.getVariables().addAll(oracleSpec.getVariables());
      pkg.getVarrayTypes().addAll(oracleSpec.getVarrayTypes());

      data.getPackageSpecAst().add(oracleSpec);
      data.getPackageBodyAst().add(pkg);

      System.out.println("Package name: " + pkg.getName());
      
      // Test that toPostgre() call does not return null and doesn't get stuck
      String postgreResult = pkg.toPostgre(data, false);
      assertNotNull(postgreResult, "toPostgre() should not return null");
      
      System.out.println("PostgreSQL conversion successful, result length: " + postgreResult.length());
      System.out.println("Generated PostgreSQL code:");
      System.out.println("=".repeat(80));
      System.out.println(postgreResult);
      System.out.println("=".repeat(80));
      
      // Additional validations to ensure parsing captured the structure
      assertFalse(postgreResult.trim().isEmpty(), "PostgreSQL result should not be empty");
      
      // Verify that collection element assignment uses correct numeric type
      assertTrue(postgreResult.contains("sys.set_package_collection_element_numeric"), 
          "Should use numeric type for NUMBER collection elements, but got: " + 
          (postgreResult.contains("sys.set_package_collection_element_text") ? "text type" : "unknown type"));
      
      // Verify basic structure was parsed
      if (pkg.getProcedures() != null) {
        System.out.println("Procedures count: " + pkg.getProcedures().size());
      }
      
    } else {
      System.out.println("Parsed AST type: " + ast.getClass().getSimpleName());
      
      // For non-package ASTs, just verify we got something non-null
      // Note: Not all AST types have toPostgre method, so we'll skip that test
      System.out.println("Parsing completed for non-package AST type");
    }
  }
}