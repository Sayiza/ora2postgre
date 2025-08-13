package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for parsing PL/SQL packages with varray operations.
 * This test addresses a specific parsing issue where varray operations
 * cause the parser to get stuck.
 */
public class VarrayPackageParsingMiniTest {

  @Test
  public void testVarrayMiniParsing() {
    // Test PL/SQL package body with varray operations that was causing parsing problems
    String oracleSql = """
-- Package body
CREATE OR REPLACE PACKAGE BODY user_robert.pkg_varray_example AS
    
  PROCEDURE display_numbers IS
    vX number;
  BEGIN
    g_numbers(1) := 1;
    vX := g_numbers(1);
    --htp.p('Number ' || i || ': ' || g_numbers(1));
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
      
      // Verify that collection element assignment uses unified JSON-based storage
      assertTrue(postgreResult.contains("sys.set_package_var_element"), 
          "Should use unified JSON storage for all collection elements, but got: " + 
          (postgreResult.contains("sys.set_package_collection_element") ? "legacy type-specific storage" : "unknown storage method"));
      
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