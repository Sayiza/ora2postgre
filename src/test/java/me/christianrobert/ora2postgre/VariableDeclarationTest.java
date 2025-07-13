package me.christianrobert.ora2postgre;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;
import me.christianrobert.ora2postgre.plsql.ast.tools.strategies.StandardProcedureStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for variable declaration support in procedures
 */
public class VariableDeclarationTest {

    private Everything data;
    private StandardProcedureStrategy strategy;

    @BeforeEach
    void setUp() {
        data = new Everything();
        data.resetIntendation();
        strategy = new StandardProcedureStrategy();
    }

    @Test
    void testProcedureWithSingleVariableDeclaration() {
        // Create a simple variable: vX number;
        DataTypeSpec numberType = new DataTypeSpec("number", null, null, null);
        Variable vX = new Variable("vX", numberType, null);
        
        // Create a simple assignment statement: vX := vX + 1;
        Statement assignment = new Comment("vX := vX + 1;");
        
        // Create procedure with variable declaration
        Procedure procedure = new Procedure(
            "testprocedureinpackage",
            Collections.emptyList(), // no parameters
            Arrays.asList(vX), // one variable
            Arrays.asList(assignment) // one statement
        );
        
        // Mock package parent
        OraclePackage mockPackage = createMockPackage();
        procedure.setParentPackage(mockPackage);
        
        String result = strategy.transform(procedure, data, false);
        
        assertNotNull(result);
        assertTrue(result.contains("CREATE OR REPLACE PROCEDURE"), "Should contain procedure header");
        assertTrue(result.contains("DECLARE"), "Should contain DECLARE section");
        assertTrue(result.contains("vX numeric;"), "Should contain variable declaration");
        assertTrue(result.contains("vX := vX + 1"), "Should contain assignment statement");
        assertTrue(result.contains("BEGIN"), "Should contain BEGIN");
        assertTrue(result.contains("END"), "Should contain END");
        
        System.out.println("Generated PostgreSQL:");
        System.out.println(result);
    }

    @Test
    void testProcedureWithMultipleVariableDeclarations() {
        // Create multiple variables
        DataTypeSpec numberType = new DataTypeSpec("number", null, null, null);
        DataTypeSpec varcharType = new DataTypeSpec("varchar2", null, null, null);
        
        Variable vCounter = new Variable("vCounter", numberType, null);
        Variable vName = new Variable("vName", varcharType, null);
        
        // Create statements
        Statement stmt1 = new Comment("vCounter := 1;");
        Statement stmt2 = new Comment("vName := 'test';");
        
        Procedure procedure = new Procedure(
            "testmultivars",
            Collections.emptyList(),
            Arrays.asList(vCounter, vName),
            Arrays.asList(stmt1, stmt2)
        );
        
        // Mock package parent
        OraclePackage mockPackage = createMockPackage();
        procedure.setParentPackage(mockPackage);
        
        String result = strategy.transform(procedure, data, false);
        
        assertNotNull(result);
        assertTrue(result.contains("vCounter numeric"), "Should contain vCounter declaration");
        assertTrue(result.contains("vName text;"), "Should contain vName declaration");
        assertTrue(result.contains("vCounter := 1"), "Should contain first assignment");
        assertTrue(result.contains("vName := 'test'"), "Should contain second assignment");
    }

    @Test
    void testProcedureWithVariableAndDefaultValue() {
        // Create variable with default value: vStatus varchar2(10) := 'ACTIVE';
        DataTypeSpec varcharType = new DataTypeSpec("varchar2", null, null, null);
        
        // Create a simple expression for the default value
        LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression("'ACTIVE'"));
        Expression defaultValue = new Expression(logicalExpr);
        
        Variable vStatus = new Variable("vStatus", varcharType, defaultValue);
        
        Statement stmt = new Comment("NULL; -- placeholder");
        
        Procedure procedure = new Procedure(
            "testdefaultvalue",
            Collections.emptyList(),
            Arrays.asList(vStatus),
            Arrays.asList(stmt)
        );
        
        // Mock package parent
        OraclePackage mockPackage = createMockPackage();
        procedure.setParentPackage(mockPackage);
        
        String result = strategy.transform(procedure, data, false);
        
        assertNotNull(result);
        assertTrue(result.contains("vStatus text;"), "Should contain variable declaration");
        // Note: Default values in PostgreSQL are handled differently, so we check for the variable declaration
    }

    @Test
    void testProcedureWithNoVariables() {
        // Test procedure without any variable declarations
        Statement stmt = new Comment("NULL; -- no variables");
        
        Procedure procedure = new Procedure(
            "testnovars",
            Collections.emptyList(),
            Collections.emptyList(), // no variables
            Arrays.asList(stmt)
        );
        
        // Mock package parent
        OraclePackage mockPackage = createMockPackage();
        procedure.setParentPackage(mockPackage);
        
        String result = strategy.transform(procedure, data, false);
        
        assertNotNull(result);
        assertTrue(result.contains("DECLARE"), "Should still contain DECLARE section");
        assertTrue(result.contains("BEGIN"), "Should contain BEGIN");
        assertTrue(result.contains("NULL; -- no variables"), "Should contain statement");
    }

    /**
     * Helper method to create a mock package for testing
     */
    private OraclePackage createMockPackage() {
        return new OraclePackage(
            "TESTPACKAGE", 
            "TEST_SCHEMA", 
            Collections.emptyList(), // variables
            Collections.emptyList(), // subTypes  
            Collections.emptyList(), // cursors
            Collections.emptyList(), // packageTypes
            Collections.emptyList(), // recordTypes
            Collections.emptyList(), // functions
            Collections.emptyList(), // procedures
            Collections.emptyList()  // bodyStatements
        );
    }
}