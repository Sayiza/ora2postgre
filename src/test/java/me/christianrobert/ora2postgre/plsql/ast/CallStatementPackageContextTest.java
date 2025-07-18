package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for CallStatement package context resolution.
 * Tests Issue 3: Package-less Call Resolution
 */
public class CallStatementPackageContextTest {

    @Test
    public void testCallStatementWithCallingContext() {
        // Test CallStatement with calling package context
        Everything data = new Everything();
        
        // Create a mock package structure
        createMockPackage(data);
        
        // Create a CallStatement without explicit package (simulating package-less call)
        List<Expression> arguments = new ArrayList<>();
        CallStatement callStatement = new CallStatement("add2gxpackagevar", null, arguments, false);
        
        // Set calling context (this would normally be set by PlSqlAstBuilder)
        callStatement.setCallingPackage("minitest");
        callStatement.setCallingSchema("USER_ROBERT");
        
        // Transform to PostgreSQL
        String result = callStatement.toPostgre(data);
        System.out.println("CallStatement with calling context:");
        System.out.println(result);
        
        // Verify the transformation
        assertTrue(result.contains("CALL"), "Should contain CALL statement");
        assertTrue(result.contains("USER_ROBERT"), "Should contain schema");
        assertTrue(result.contains("MINITEST_add2gxpackagevar"), "Should contain package-prefixed procedure name");
        assertTrue(result.contains("();"), "Should contain empty argument list");
        
        // Verify that package context was set during resolution
        assertEquals("minitest", callStatement.getPackageName(), "Package name should be set during resolution");
        assertEquals("USER_ROBERT", callStatement.getSchema(), "Schema should be set during resolution");
    }
    
    @Test
    public void testCallStatementWithExplicitPackage() {
        // Test CallStatement with explicit package (should work as before)
        Everything data = new Everything();
        
        // Create a mock package structure
        createMockPackage(data);
        
        // Create a CallStatement with explicit package
        List<Expression> arguments = new ArrayList<>();
        CallStatement callStatement = new CallStatement("add2gxpackagevar", "minitest", arguments, false);
        
        // Set calling context (should be ignored since explicit package is provided)
        callStatement.setCallingPackage("differentpackage");
        callStatement.setCallingSchema("USER_ROBERT");
        
        // Transform to PostgreSQL
        String result = callStatement.toPostgre(data);
        System.out.println("CallStatement with explicit package:");
        System.out.println(result);
        
        // Verify the transformation
        assertTrue(result.contains("CALL"), "Should contain CALL statement");
        assertTrue(result.contains("USER_ROBERT"), "Should contain schema");
        assertTrue(result.contains("MINITEST_add2gxpackagevar"), "Should contain package-prefixed procedure name");
        
        // Verify that explicit package was preserved
        assertEquals("minitest", callStatement.getPackageName(), "Explicit package name should be preserved");
    }
    
    @Test
    public void testCallStatementWithoutCallingContext() {
        // Test CallStatement without calling context (fallback behavior)
        Everything data = new Everything();
        
        // Create a mock package structure
        createMockPackage(data);
        
        // Create a CallStatement without explicit package and no calling context
        List<Expression> arguments = new ArrayList<>();
        CallStatement callStatement = new CallStatement("add2gxpackagevar", null, arguments, false);
        
        // No calling context set (callingPackage and callingSchema remain null)
        
        // Transform to PostgreSQL
        String result = callStatement.toPostgre(data);
        System.out.println("CallStatement without calling context:");
        System.out.println(result);
        
        // Verify the transformation (should fall back to default behavior)
        assertTrue(result.contains("CALL"), "Should contain CALL statement");
        assertNotNull(result, "Should generate valid SQL even without context");
    }
    
    @Test
    public void testToStringWithCallingContext() {
        // Test toString method includes calling context information
        List<Expression> arguments = new ArrayList<>();
        CallStatement callStatement = new CallStatement("testProcedure", null, arguments, false);
        
        // Set calling context
        callStatement.setCallingPackage("testPackage");
        callStatement.setCallingSchema("TEST_SCHEMA");
        
        String result = callStatement.toString();
        System.out.println("CallStatement toString with calling context:");
        System.out.println(result);
        
        // Verify toString includes relevant information
        assertTrue(result.contains("CallStatement"), "Should contain class name");
        assertTrue(result.contains("routine=testProcedure"), "Should contain routine name");
        assertTrue(result.contains("isFunction=false"), "Should contain function flag");
        assertNotNull(result, "Should generate valid string representation");
    }
    
    private void createMockPackage(Everything data) {
        // Create a simplified package structure for testing
        try {
            // Create package variables
            List<Variable> variables = new ArrayList<>();
            DataTypeSpec numberType = new DataTypeSpec("number", null, null, null);
            Expression defaultValue = new Expression(new LogicalExpression(new UnaryLogicalExpression("1")));
            Variable gXVar = new Variable("gX", numberType, defaultValue);
            variables.add(gXVar);
            
            // Create procedures
            List<Procedure> procedures = new ArrayList<>();
            List<Parameter> params = new ArrayList<>();
            List<Variable> procedureVariables = new ArrayList<>();
            List<Statement> statements = new ArrayList<>();
            
            // Create add2gxpackagevar procedure
            Procedure testProc = new Procedure("add2gxpackagevar", params, procedureVariables, statements);
            procedures.add(testProc);
            
            // Create empty lists for other components
            List<SubType> subTypes = new ArrayList<>();
            List<Cursor> cursors = new ArrayList<>();
            List<PackageType> packageTypes = new ArrayList<>();
            List<RecordType> recordTypes = new ArrayList<>();
            List<VarrayType> varrayTypes = new ArrayList<>();
            List<NestedTableType> nestedTableTypes = new ArrayList<>();
            List<Function> functions = new ArrayList<>();
            List<Statement> packageStatements = new ArrayList<>();
            
            // Create package
            OraclePackage pkg = new OraclePackage("minitest", "USER_ROBERT", variables, subTypes, cursors,
                                                 packageTypes, recordTypes, varrayTypes, nestedTableTypes,
                                                 functions, procedures, packageStatements);
            
            // Add to data context
            data.getPackageBodyAst().add(pkg);
            
            System.out.println("Created mock package: " + pkg.getName() + " with " + procedures.size() + " procedures");
        } catch (Exception e) {
            System.err.println("Error creating mock package: " + e.getMessage());
            e.printStackTrace();
        }
    }
}