package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for CallStatement AST node and enhanced visitCall_statement method.
 */
public class CallStatementTest {

    @Test
    public void testCallStatementBasicConstructor() {
        // Test basic constructor
        CallStatement call = new CallStatement("testProcedure");
        
        assertEquals("testProcedure", call.getRoutineName());
        assertNull(call.getPackageName());
        assertNull(call.getSchema());
        assertFalse(call.isFunction());
        assertNotNull(call.getArguments());
        assertTrue(call.getArguments().isEmpty());
        assertNull(call.getReturnTarget());
    }

    @Test
    public void testCallStatementWithPackageAndArguments() {
        // Test constructor with package and arguments
        List<Expression> args = new ArrayList<>();
        args.add(new Expression(new LogicalExpression(new UnaryLogicalExpression("arg1"))));
        
        CallStatement call = new CallStatement("testProcedure", "testPackage", args, false);
        
        assertEquals("testProcedure", call.getRoutineName());
        assertEquals("testPackage", call.getPackageName());
        assertFalse(call.isFunction());
        assertEquals(1, call.getArguments().size());
        assertNull(call.getReturnTarget());
    }

    @Test
    public void testCallStatementFunctionWithReturn() {
        // Test function constructor with return target
        List<Expression> args = new ArrayList<>();
        Expression returnTarget = new Expression(new LogicalExpression(new UnaryLogicalExpression("result")));
        
        CallStatement call = new CallStatement("testFunction", "testPackage", args, returnTarget);
        
        assertEquals("testFunction", call.getRoutineName());
        assertEquals("testPackage", call.getPackageName());
        assertTrue(call.isFunction());
        assertEquals(0, call.getArguments().size());
        assertNotNull(call.getReturnTarget());
    }

    @Test
    public void testCallStatementToString() {
        // Test toString method
        CallStatement call = new CallStatement("testProcedure", "testPackage", new ArrayList<>(), false);
        String result = call.toString();
        
        assertTrue(result.contains("CallStatement"));
        assertTrue(result.contains("package=testPackage"));
        assertTrue(result.contains("routine=testProcedure"));
        assertTrue(result.contains("args=0"));
        assertTrue(result.contains("isFunction=false"));
    }

    @Test
    public void testCallStatementPostgreTransformation() {
        // Test PostgreSQL transformation
        Everything data = new Everything();
        
        // Create a simple call statement
        CallStatement call = new CallStatement("testProcedure");
        call.setSchema("TEST_SCHEMA");
        
        String result = call.toPostgre(data);
        
        assertNotNull(result);
        assertTrue(result.contains("CALL"));
        assertTrue(result.contains("TEST_SCHEMA"));
        assertTrue(result.contains("testprocedure"));
        assertTrue(result.contains("();"));
    }

    @Test
    public void testCallStatementFunctionTransformation() {
        // Test PostgreSQL transformation for function
        Everything data = new Everything();
        
        // Create a function call statement
        CallStatement call = new CallStatement("testFunction");
        call.setSchema("TEST_SCHEMA");
        call.setFunction(true);
        
        String result = call.toPostgre(data);
        
        assertNotNull(result);
        assertTrue(result.contains("SELECT"));
        assertTrue(result.contains("TEST_SCHEMA"));
        assertTrue(result.contains("testfunction"));
        assertTrue(result.contains("();"));
    }

    @Test
    public void testCallStatementWithPackageTransformation() {
        // Test PostgreSQL transformation with package
        Everything data = new Everything();
        
        // Create a call statement with package
        CallStatement call = new CallStatement("testProcedure", "testPackage", new ArrayList<>(), false);
        call.setSchema("TEST_SCHEMA");
        
        String result = call.toPostgre(data);
        
        assertNotNull(result);
        assertTrue(result.contains("CALL"));
        assertTrue(result.contains("TEST_SCHEMA"));
        assertTrue(result.contains("TESTPACKAGE_testprocedure"));
        assertTrue(result.contains("();"));
    }

    @Test
    public void testCallStatementWithArguments() {
        // Test PostgreSQL transformation with arguments
        Everything data = new Everything();
        
        // Create arguments
        List<Expression> args = new ArrayList<>();
        args.add(new Expression(new LogicalExpression(new UnaryLogicalExpression("'test'"))));
        args.add(new Expression(new LogicalExpression(new UnaryLogicalExpression("123"))));
        
        CallStatement call = new CallStatement("testProcedure", "testPackage", args, false);
        call.setSchema("TEST_SCHEMA");
        
        String result = call.toPostgre(data);
        
        assertNotNull(result);
        assertTrue(result.contains("CALL"));
        assertTrue(result.contains("TEST_SCHEMA"));
        assertTrue(result.contains("TESTPACKAGE_testprocedure"));
        assertTrue(result.contains("'test'"));
        assertTrue(result.contains("123"));
    }

    @Test
    public void testCallStatementSchemaResolution() {
        // Test basic schema resolution behavior
        Everything data = new Everything();
        
        CallStatement call = new CallStatement("testProcedure", "testPackage", new ArrayList<>(), false);
        
        String result = call.toPostgre(data);
        
        assertNotNull(result);
        assertTrue(result.contains("CALL"));
        // Should use fallback schema if no packages found
        assertTrue(result.contains("USER_ROBERT")); // fallback
    }
}