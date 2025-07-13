package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RaiseStatementTest {

  @Test
  public void testRaiseSpecificException() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION test_function RETURN NUMBER IS
    v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM dual;
    IF v_count = 0 THEN
      RAISE NO_DATA_FOUND;
    END IF;
    RETURN v_count;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
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
    assertEquals("test_function", function.getName());
    
    // Verify function has statements including RAISE
    assertNotNull(function.getStatements());
    assertTrue(function.getStatements().size() > 0);
    
    // Check for RAISE statement in IF block (this might be nested)
    boolean foundRaiseStatement = findRaiseStatementInStatements(function.getStatements());
    assertTrue(foundRaiseStatement, "Should find RAISE statement in function body");
  }

  @Test
  public void testBareRaiseStatement() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  FUNCTION test_function RETURN NUMBER IS
    v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM dual;
    RETURN v_count;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      RAISE;  -- Re-raise current exception
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
    assertTrue(function.hasExceptionHandling());
    
    ExceptionBlock exceptionBlock = function.getExceptionBlock();
    assertNotNull(exceptionBlock);
    assertEquals(2, exceptionBlock.getHandlers().size());
    
    // Check first exception handler contains RAISE statement
    ExceptionHandler firstHandler = exceptionBlock.getHandlers().get(0);
    assertNotNull(firstHandler.getStatements());
    assertTrue(firstHandler.getStatements().size() > 0);
    
    // Verify it's a RAISE statement and it's a re-raise (bare RAISE)
    Statement statement = firstHandler.getStatements().get(0);
    assertTrue(statement instanceof RaiseStatement);
    RaiseStatement raiseStatement = (RaiseStatement) statement;
    assertTrue(raiseStatement.isReRaise(), "Should be a bare RAISE (re-raise)");
    assertNull(raiseStatement.getExceptionName());
  }

  @Test
  public void testPostgreSQLGeneration() {
    // Test direct AST creation and PostgreSQL generation
    Everything data = new Everything();
    data.intendMore(); // Add some indentation
    
    // Test specific exception RAISE
    RaiseStatement raiseSpecific = new RaiseStatement("NO_DATA_FOUND");
    String pgSpecific = raiseSpecific.toPostgre(data);
    assertTrue(pgSpecific.contains("RAISE NO_DATA_FOUND;"));
    assertFalse(raiseSpecific.isReRaise());
    assertEquals("NO_DATA_FOUND", raiseSpecific.getExceptionName());
    
    // Test bare RAISE (re-raise)
    RaiseStatement raiseReRaise = new RaiseStatement();
    String pgReRaise = raiseReRaise.toPostgre(data);
    assertTrue(pgReRaise.contains("RAISE ;"));
    assertTrue(raiseReRaise.isReRaise());
    assertNull(raiseReRaise.getExceptionName());
    
    // Test Oracle to PostgreSQL exception mapping
    RaiseStatement raiseDup = new RaiseStatement("DUP_VAL_ON_INDEX");
    String pgDup = raiseDup.toPostgre(data);
    assertTrue(pgDup.contains("RAISE unique_violation;"));
  }

  @Test
  public void testComplexExceptionFlow() {
    String oracleSql = """
CREATE PACKAGE BODY TEST_SCHEMA.TESTPACKAGE is  
  PROCEDURE test_procedure(p_id NUMBER) IS
    v_count NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_count FROM dual WHERE 1 = p_id;
    
    IF v_count = 0 THEN
      RAISE NO_DATA_FOUND;
    ELSIF v_count > 1 THEN
      RAISE TOO_MANY_ROWS;  
    END IF;
    
    INSERT INTO log_table VALUES (p_id, 'SUCCESS');
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      INSERT INTO log_table VALUES (p_id, 'NO_DATA');
      RAISE;  -- Re-raise to caller
    WHEN TOO_MANY_ROWS THEN  
      RAISE PROGRAM_ERROR;  -- Raise different exception
    WHEN OTHERS THEN
      INSERT INTO log_table VALUES (p_id, 'ERROR');
      RAISE;  -- Re-raise any other exception
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
    assertEquals("test_procedure", procedure.getName());
    assertTrue(procedure.hasExceptionHandling());
    
    // Verify exception block structure
    ExceptionBlock exceptionBlock = procedure.getExceptionBlock();
    assertNotNull(exceptionBlock);
    assertEquals(3, exceptionBlock.getHandlers().size());
    
    // Check each exception handler contains appropriate statements
    for (ExceptionHandler handler : exceptionBlock.getHandlers()) {
      assertNotNull(handler.getStatements());
      assertTrue(handler.getStatements().size() > 0);
    }
    
    // Look for RAISE statements in the main procedure body (IF blocks)
    boolean foundRaiseInBody = findRaiseStatementInStatements(procedure.getStatements());
    assertTrue(foundRaiseInBody, "Should find RAISE statements in procedure body");
  }

  /**
   * Helper method to recursively search for RAISE statements in a list of statements.
   */
  private boolean findRaiseStatementInStatements(java.util.List<Statement> statements) {
    if (statements == null) return false;
    
    for (Statement stmt : statements) {
      if (stmt instanceof RaiseStatement) {
        return true;
      }
      
      // Check nested statements (e.g., in IF blocks)
      if (stmt instanceof IfStatement) {
        IfStatement ifStmt = (IfStatement) stmt;
        if (findRaiseStatementInStatements(ifStmt.getThenStatements()) ||
            findRaiseStatementInStatements(ifStmt.getElseStatements())) {
          return true;
        }
        
        // Check ELSIF parts
        if (ifStmt.getElsifParts() != null) {
          for (var elsifPart : ifStmt.getElsifParts()) {
            if (findRaiseStatementInStatements(elsifPart.getStatements())) {
              return true;
            }
          }
        }
      }
      
      // Can add more nested statement types as needed (WHILE, LOOP, etc.)
    }
    
    return false;
  }
}