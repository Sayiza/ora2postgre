package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for HTP package variable integration.
 * Tests Issue 2: Package Variable Detection in Complex Expressions
 */
public class HtpPackageVariableTest {

    @Test
    public void testHtpStatementWithSimpleString() {
        // Test to ensure simple string arguments still work
        Everything data = new Everything();
        
        // Create expression with simple string
        UnaryLogicalExpression strExpr = new UnaryLogicalExpression("'hello world'");
        Expression expr = new Expression(new LogicalExpression(strExpr));
        
        // Create HtpStatement with expression
        HtpStatement htpStmt = new HtpStatement(expr);
        
        // Transform to PostgreSQL
        String result = htpStmt.toPostgre(data);
        System.out.println("HtpStatement simple string result:");
        System.out.println(result);
        
        // Verify the transformation
        assertTrue(result.contains("CALL SYS.HTP_p('hello world')"), 
                   "Should contain HTP call with simple string");
    }
    
    @Test
    public void testHtpStatementLegacyConstructor() {
        // Test legacy constructor for backward compatibility
        Everything data = new Everything();
        
        // Create HtpStatement with legacy constructor
        HtpStatement htpStmt = new HtpStatement("'test string'");
        
        // Transform to PostgreSQL
        String result = htpStmt.toPostgre(data);
        System.out.println("HtpStatement legacy constructor result:");
        System.out.println(result);
        
        // Verify the transformation
        assertTrue(result.contains("CALL SYS.HTP_p('test string')"), 
                   "Should contain HTP call with legacy string handling");
    }
    
    @Test
    public void testHtpStatementWithNumericExpression() {
        // Test HTP with numeric expression
        Everything data = new Everything();
        
        // Create expression with numeric value
        UnaryLogicalExpression numExpr = new UnaryLogicalExpression("42");
        Expression expr = new Expression(new LogicalExpression(numExpr));
        
        // Create HtpStatement with expression
        HtpStatement htpStmt = new HtpStatement(expr);
        
        // Transform to PostgreSQL
        String result = htpStmt.toPostgre(data);
        System.out.println("HtpStatement numeric expression result:");
        System.out.println(result);
        
        // Verify the transformation
        assertTrue(result.contains("CALL SYS.HTP_p(42)"), 
                   "Should contain HTP call with numeric value");
    }
    
    @Test
    public void testHtpStatementWithVariable() {
        // Test HTP with variable name (will not be transformed without package context)
        Everything data = new Everything();
        
        // Create expression with variable name
        UnaryLogicalExpression varExpr = new UnaryLogicalExpression("gX");
        Expression expr = new Expression(new LogicalExpression(varExpr));
        
        // Create HtpStatement with expression
        HtpStatement htpStmt = new HtpStatement(expr);
        
        // Transform to PostgreSQL
        String result = htpStmt.toPostgre(data);
        System.out.println("HtpStatement variable expression result:");
        System.out.println(result);
        
        // Verify the transformation (should contain variable name as-is since no package context)
        assertTrue(result.contains("CALL SYS.HTP_p(gX)"), 
                   "Should contain HTP call with variable name");
    }
}