package me.christianrobert.ora2postgre.plsql.ast.tools;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;
import me.christianrobert.ora2postgre.plsql.ast.tools.CursorLoopAnalyzer.CursorLoopInfo;
import me.christianrobert.ora2postgre.plsql.ast.tools.CursorLoopTransformer.CursorForLoopStatement;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CursorLoopTransformerTest {

    @Test
    public void testCursorLoopDetectionAndTransformation() {
        // Create mock statements representing the cursor loop pattern
        
        // OPEN emp_cursor;
        OpenStatement openStmt = new OpenStatement("emp_cursor", null);
        
        // FETCH emp_cursor INTO v_emp_id, v_first_name;
        FetchStatement fetchStmt = new FetchStatement("emp_cursor", Arrays.asList("v_emp_id", "v_first_name"));
        
        // EXIT WHEN emp_cursor%NOTFOUND;
        Expression notFoundCondition = createSimpleExpression("emp_cursor%NOTFOUND");
        ExitStatement exitStmt = new ExitStatement(notFoundCondition);
        
        // v_count := v_count + 1;
        AssignmentStatement businessLogic = new AssignmentStatement(
            "v_count",
            createSimpleExpression("v_count + 1")
        );
        
        // LOOP containing FETCH, EXIT, and business logic
        LoopStatement loopStmt = new LoopStatement(Arrays.asList(fetchStmt, exitStmt, businessLogic));
        
        // CLOSE emp_cursor;
        CloseStatement closeStmt = new CloseStatement("emp_cursor");
        
        // Complete sequence: OPEN, LOOP, CLOSE
        List<Statement> statements = Arrays.asList(openStmt, loopStmt, closeStmt);
        
        // Test detection
        CursorLoopInfo cursorInfo = CursorLoopAnalyzer.detectCursorLoopPattern(statements);
        assertNotNull(cursorInfo, "Should detect cursor loop pattern");
        assertEquals("emp_cursor", cursorInfo.getCursorName());
        assertEquals(Arrays.asList("v_emp_id", "v_first_name"), cursorInfo.getFetchVariables());
        assertEquals(1, cursorInfo.getLoopBodyStatements().size()); // Just the business logic
        
        // Test transformation
        assertTrue(CursorLoopTransformer.shouldTransformToForLoop(cursorInfo));
        CursorForLoopStatement forLoop = CursorLoopTransformer.transformToCursorForLoop(cursorInfo);
        
        assertNotNull(forLoop);
        assertEquals("rec", forLoop.getRecordVariable());
        assertEquals("emp_cursor", forLoop.getCursorName());
        assertEquals(3, forLoop.getStatements().size()); // 2 assignments + 1 business logic
        
        // Test PostgreSQL output
        Everything data = new Everything();
        data.getUserNames().add("TEST_SCHEMA");
        
        String pgOutput = forLoop.toPostgre(data);
        assertNotNull(pgOutput);
        assertTrue(pgOutput.contains("FOR rec IN emp_cursor LOOP"));
        assertTrue(pgOutput.contains("END LOOP"));
        
        System.out.println("Generated PostgreSQL FOR loop:");
        System.out.println(pgOutput);
    }
    
    /**
     * Creates a simple expression from text for testing purposes.
     */
    private Expression createSimpleExpression(String text) {
        UnaryLogicalExpression unary = new UnaryLogicalExpression(text);
        LogicalExpression logical = new LogicalExpression(unary);
        return new Expression(logical);
    }
}