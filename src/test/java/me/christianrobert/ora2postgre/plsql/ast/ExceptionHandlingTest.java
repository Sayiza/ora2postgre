package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExceptionHandlingTest {

  @Test
  public void testBasicExceptionHandling() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION sample_function RETURN NUMBER IS
    v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM dual;
    RETURN v_count;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      RETURN 0;
    WHEN TOO_MANY_ROWS THEN
      RETURN -1;
  END;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    OraclePackage pkg = (OraclePackage) ast;
    assertNotNull(pkg);
    assertEquals(1, pkg.getFunctions().size());
    
    Function function = pkg.getFunctions().get(0);

    assertNotNull(function);
    assertEquals("sample_function", function.getName());
    assertTrue(function.hasExceptionHandling());
    
    ExceptionBlock exceptionBlock = function.getExceptionBlock();
    assertNotNull(exceptionBlock);
    assertEquals(2, exceptionBlock.getHandlers().size());
    
    // Check first handler
    ExceptionHandler handler1 = exceptionBlock.getHandlers().get(0);
    assertEquals(1, handler1.getExceptionNames().size());
    assertEquals("NO_DATA_FOUND", handler1.getExceptionNames().get(0));
    assertEquals(1, handler1.getStatements().size());
    
    // Check second handler  
    ExceptionHandler handler2 = exceptionBlock.getHandlers().get(1);
    assertEquals(1, handler2.getExceptionNames().size());
    assertEquals("TOO_MANY_ROWS", handler2.getExceptionNames().get(0));
    assertEquals(1, handler2.getStatements().size());
  }

  @Test
  public void testProcedureWithoutExceptionHandling() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  PROCEDURE simple_procedure IS
    v_temp NUMBER := 42;
  BEGIN
    NULL; -- Simple statement
  END;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle procedure
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    OraclePackage pkg = (OraclePackage) ast;
    assertNotNull(pkg);
    assertEquals(1, pkg.getProcedures().size());
    
    Procedure procedure = pkg.getProcedures().get(0);

    assertNotNull(procedure);
    assertFalse(procedure.hasExceptionHandling());
    assertNull(procedure.getExceptionBlock());
  }

  @Test
  public void testPostgreGeneration() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION error_test RETURN NUMBER IS
    v_result NUMBER;
  BEGIN
    SELECT id INTO v_result FROM test_table WHERE id = 999;
    RETURN v_result;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      RETURN -1;
    WHEN DUP_VAL_ON_INDEX THEN
      RETURN -2;
    WHEN OTHERS THEN
      RETURN -999;
  END;
end;
/
""";

    // Create test data
    Everything data = new Everything();
    data.getUserNames().add("TEST_SCHEMA");

    PlsqlCode plsqlCode = new PlsqlCode("TEST_SCHEMA", oracleSql);

    // Parse the Oracle function
    PlSqlAst ast = PlSqlAstMain.processPlsqlCode(plsqlCode);

    OraclePackage pkg = (OraclePackage) ast;
    assertNotNull(pkg);
    
    Function function = pkg.getFunctions().get(0);
    assertNotNull(function);
    assertTrue(function.hasExceptionHandling());

    String postgre = function.getExceptionBlock().toPostgre(data);
    
    assertNotNull(postgre);
    assertTrue(postgre.contains("EXCEPTION"));
    assertTrue(postgre.contains("WHEN NO_DATA_FOUND THEN"));
    assertTrue(postgre.contains("WHEN unique_violation THEN")); // Oracle DUP_VAL_ON_INDEX -> PostgreSQL unique_violation
    assertTrue(postgre.contains("WHEN OTHERS THEN"));
  }
}