package me.christianrobert.ora2postgre;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WhileLoopStatement transpilation from Oracle PL/SQL to PostgreSQL
 */
public class WhileLoopStatementTest {

    private Everything data;

    @BeforeEach
    void setUp() {
        data = new Everything();
        data.resetIntendation();
    }

    /**
     * Helper method to create Expression objects for testing
     */
    private Expression createExpression(String text) {
        LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression(text));
        return new Expression(logicalExpr);
    }

    @Test
    void testSimpleWhileLoop() {
        // WHILE counter < 10 LOOP
        //   counter := counter + 1;
        // END LOOP;
        
        // Create condition: counter < 10
        Expression condition = createExpression("counter < 10");
        
        // Create assignment statement: counter := counter + 1
        Statement assignment = new Comment("counter := counter + 1;");
        
        WhileLoopStatement whileLoop = new WhileLoopStatement(condition, Arrays.asList(assignment));
        
        String result = whileLoop.toPostgre(data);
        
        assertNotNull(result);
        assertTrue(result.contains("WHILE"));
        assertTrue(result.contains("LOOP"));
        assertTrue(result.contains("END LOOP;"));
        assertTrue(result.contains("counter < 10"));
        assertTrue(result.contains("counter := counter + 1;"));
    }

    @Test
    void testWhileLoopWithMultipleStatements() {
        // WHILE condition LOOP
        //   statement1;
        //   statement2;
        //   statement3;
        // END LOOP;
        
        Expression condition = createExpression("i <= max_count");
        Statement stmt1 = new Comment("INSERT INTO log_table VALUES (i);");
        Statement stmt2 = new Comment("i := i + 1;");
        Statement stmt3 = new Comment("COMMIT;");
        
        WhileLoopStatement whileLoop = new WhileLoopStatement(condition, Arrays.asList(stmt1, stmt2, stmt3));
        
        String result = whileLoop.toPostgre(data);
        
        assertNotNull(result);
        assertTrue(result.contains("WHILE i <= max_count LOOP"));
        assertTrue(result.contains("INSERT INTO log_table VALUES (i);"));
        assertTrue(result.contains("i := i + 1;"));
        assertTrue(result.contains("COMMIT;"));
        assertTrue(result.contains("END LOOP;"));
    }

    @Test
    void testWhileLoopWithEmptyBody() {
        // WHILE condition LOOP
        // END LOOP;
        
        Expression condition = createExpression("flag = TRUE");
        
        WhileLoopStatement whileLoop = new WhileLoopStatement(condition, Collections.emptyList());
        
        String result = whileLoop.toPostgre(data);
        
        assertNotNull(result);
        assertTrue(result.contains("WHILE flag = TRUE LOOP"));
        assertTrue(result.contains("END LOOP;"));
    }

    @Test
    void testWhileLoopIndentation() {
        // Test that indentation works correctly
        Expression condition = createExpression("true");
        Statement stmt = new Comment("-- inner statement");
        
        WhileLoopStatement whileLoop = new WhileLoopStatement(condition, Arrays.asList(stmt));
        
        // Set initial indentation
        data.intendMore();
        
        String result = whileLoop.toPostgre(data);
        
        assertNotNull(result);
        // Should have proper indentation for WHILE and END LOOP
        assertTrue(result.contains("  WHILE"), "Expected '  WHILE' in: " + result);
        assertTrue(result.contains("  END LOOP;"), "Expected '  END LOOP;' in: " + result);
        // Comment renders as /* comment */ format, so check for that
        assertTrue(result.contains("/* -- inner statement */"), "Expected '/* -- inner statement */' in: " + result);
    }

    @Test
    void testToString() {
        Expression condition = createExpression("counter < 10");
        Statement stmt1 = new Comment("stmt1");
        Statement stmt2 = new Comment("stmt2");
        
        WhileLoopStatement whileLoop = new WhileLoopStatement(condition, Arrays.asList(stmt1, stmt2));
        
        String toString = whileLoop.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("WhileLoopStatement"));
        assertTrue(toString.contains("condition="));
        assertTrue(toString.contains("statements=2"));
    }

    @Test
    void testGetters() {
        Expression condition = createExpression("test condition");
        Statement stmt = new Comment("test statement");
        
        WhileLoopStatement whileLoop = new WhileLoopStatement(condition, Arrays.asList(stmt));
        
        assertEquals(condition, whileLoop.getCondition());
        assertEquals(1, whileLoop.getStatements().size());
        assertEquals(stmt, whileLoop.getStatements().get(0));
    }
}