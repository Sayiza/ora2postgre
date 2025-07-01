package com.sayiza.oracle2postgre.plsql.ast.tools;

import com.sayiza.oracle2postgre.global.Everything;
import com.sayiza.oracle2postgre.plsql.ast.ForLoopStatement;
import com.sayiza.oracle2postgre.plsql.ast.Statement;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StatementDeclarationCollectorTest {

    @Test
    public void testSingleForLoopDeclaration() {
        // Arrange
        ForLoopStatement forLoop = new ForLoopStatement("TEST_SCHEMA", "r", null, Arrays.asList());
        List<Statement> statements = Arrays.asList(forLoop);
        Everything data = new Everything();

        // Act
        StringBuilder declarations = StatementDeclarationCollector.collectNecessaryDeclarations(statements, data);

        // Assert
        String result = declarations.toString();
        assertEquals("  r RECORD;\n", result);
    }

    @Test
    public void testDuplicateForLoopVariableNames() {
        // Arrange - two FOR loops with the same variable name "r"
        ForLoopStatement forLoop1 = new ForLoopStatement("TEST_SCHEMA", "r", null, Arrays.asList());
        ForLoopStatement forLoop2 = new ForLoopStatement("TEST_SCHEMA", "r", null, Arrays.asList());
        
        List<Statement> statements = Arrays.asList(forLoop1, forLoop2);
        Everything data = new Everything();

        // Act
        StringBuilder declarations = StatementDeclarationCollector.collectNecessaryDeclarations(statements, data);

        // Assert - should only have one declaration for "r"
        String result = declarations.toString();
        assertEquals("  r RECORD;\n", result);
        
        // Verify that the declaration appears only once, not twice
        long recordCount = result.lines().filter(line -> line.contains("r RECORD")).count();
        assertEquals(1, recordCount, "Should only declare variable 'r' once");
    }

    @Test
    public void testDifferentForLoopVariableNames() {
        // Arrange - two FOR loops with different variable names
        ForLoopStatement forLoop1 = new ForLoopStatement("TEST_SCHEMA", "r", null, Arrays.asList());
        ForLoopStatement forLoop2 = new ForLoopStatement("TEST_SCHEMA", "row", null, Arrays.asList());
        
        List<Statement> statements = Arrays.asList(forLoop1, forLoop2);
        Everything data = new Everything();

        // Act
        StringBuilder declarations = StatementDeclarationCollector.collectNecessaryDeclarations(statements, data);

        // Assert - should have declarations for both variables
        String result = declarations.toString();
        assertTrue(result.contains("  r RECORD;\n"), "Should contain declaration for 'r'");
        assertTrue(result.contains("  row RECORD;\n"), "Should contain declaration for 'row'");
        
        // Verify that we have exactly two declarations
        long recordCount = result.lines().filter(line -> line.contains("RECORD")).count();
        assertEquals(2, recordCount, "Should have exactly two RECORD declarations");
    }

    @Test
    public void testNestedForLoopsWithSameVariableName() {
        // Arrange - nested FOR loops with same variable name
        ForLoopStatement innerLoop = new ForLoopStatement("TEST_SCHEMA", "r", null, Arrays.asList());
        ForLoopStatement outerLoop = new ForLoopStatement("TEST_SCHEMA", "r", null, Arrays.asList(innerLoop));
        
        List<Statement> statements = Arrays.asList(outerLoop);
        Everything data = new Everything();

        // Act
        StringBuilder declarations = StatementDeclarationCollector.collectNecessaryDeclarations(statements, data);

        // Assert - should only have one declaration for "r" even in nested case
        String result = declarations.toString();
        assertEquals("  r RECORD;\n", result);
        
        // Verify that the declaration appears only once
        long recordCount = result.lines().filter(line -> line.contains("r RECORD")).count();
        assertEquals(1, recordCount, "Should only declare variable 'r' once even in nested loops");
    }

    @Test
    public void testMixedScenario() {
        // Arrange - complex scenario with both duplicate and unique variable names
        ForLoopStatement loop1 = new ForLoopStatement("TEST_SCHEMA", "r", null, Arrays.asList());
        ForLoopStatement loop2 = new ForLoopStatement("TEST_SCHEMA", "row", null, Arrays.asList());
        ForLoopStatement loop3 = new ForLoopStatement("TEST_SCHEMA", "r", null, Arrays.asList()); // duplicate "r"
        
        List<Statement> statements = Arrays.asList(loop1, loop2, loop3);
        Everything data = new Everything();

        // Act
        StringBuilder declarations = StatementDeclarationCollector.collectNecessaryDeclarations(statements, data);

        // Assert
        String result = declarations.toString();
        
        // Should contain both unique variables but "r" only once
        assertTrue(result.contains("  r RECORD;\n"), "Should contain declaration for 'r'");
        assertTrue(result.contains("  row RECORD;\n"), "Should contain declaration for 'row'");
        
        // Verify correct counts
        long rCount = result.lines().filter(line -> line.trim().equals("r RECORD;")).count();
        long rowCount = result.lines().filter(line -> line.trim().equals("row RECORD;")).count();
        long totalRecordCount = result.lines().filter(line -> line.contains("RECORD")).count();
        
        assertEquals(1, rCount, "Should declare 'r' exactly once");
        assertEquals(1, rowCount, "Should declare 'row' exactly once");
        assertEquals(2, totalRecordCount, "Should have exactly two RECORD declarations total");
    }
}