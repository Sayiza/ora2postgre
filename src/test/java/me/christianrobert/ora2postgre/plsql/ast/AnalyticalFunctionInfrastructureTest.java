package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for analytical function infrastructure.
 * Tests the basic AST classes and their toPostgre() methods.
 */
public class AnalyticalFunctionInfrastructureTest {

  private Everything everything;

  @BeforeEach
  void setUp() {
    everything = new Everything();
    everything.getUserNames().add("TEST_SCHEMA");
  }

  @Test
  void testSimpleOverClause() {
    // Test basic OVER clause without PARTITION BY or windowing
    OverClause overClause = new OverClause();
    
    String result = overClause.toPostgre(everything);
    
    System.out.println("Simple OVER clause: " + result);
    assertNotNull(result);
    assertTrue(result.contains("OVER"), "Should contain OVER keyword");
    assertTrue(result.contains("("), "Should contain opening parenthesis");
    assertTrue(result.contains(")"), "Should contain closing parenthesis");
  }

  @Test
  void testRowNumberFunction() {
    // Test ROW_NUMBER() analytical function
    OverClause overClause = new OverClause();
    AnalyticalFunction rowNumber = new AnalyticalFunction(
        AnalyticalFunction.AnalyticalFunctionType.ROW_NUMBER, 
        null, 
        overClause
    );
    
    String result = rowNumber.toPostgre(everything);
    
    System.out.println("ROW_NUMBER() function: " + result);
    assertNotNull(result);
    assertTrue(result.contains("ROW_NUMBER"), "Should contain ROW_NUMBER function name");
    assertTrue(result.contains("()"), "Should contain parentheses for function call");
    assertTrue(result.contains("OVER"), "Should contain OVER clause");
  }

  @Test 
  void testRankFunction() {
    // Test RANK() analytical function
    OverClause overClause = new OverClause();
    AnalyticalFunction rank = new AnalyticalFunction(
        AnalyticalFunction.AnalyticalFunctionType.RANK, 
        null, 
        overClause
    );
    
    String result = rank.toPostgre(everything);
    
    System.out.println("RANK() function: " + result);
    assertNotNull(result);
    assertTrue(result.contains("RANK"), "Should contain RANK function name");
    assertTrue(result.contains("()"), "Should contain parentheses for function call");
    assertTrue(result.contains("OVER"), "Should contain OVER clause");
  }

  @Test
  void testDenseRankFunction() {
    // Test DENSE_RANK() analytical function
    OverClause overClause = new OverClause();
    AnalyticalFunction denseRank = new AnalyticalFunction(
        AnalyticalFunction.AnalyticalFunctionType.DENSE_RANK, 
        null, 
        overClause
    );
    
    String result = denseRank.toPostgre(everything);
    
    System.out.println("DENSE_RANK() function: " + result);
    assertNotNull(result);
    assertTrue(result.contains("DENSE_RANK"), "Should contain DENSE_RANK function name");
    assertTrue(result.contains("()"), "Should contain parentheses for function call");
    assertTrue(result.contains("OVER"), "Should contain OVER clause");
  }

  @Test
  void testFactoryMethods() {
    // Test static factory methods
    OverClause overClause = new OverClause();
    
    AnalyticalFunction rowNumber = AnalyticalFunction.rowNumber(overClause);
    AnalyticalFunction rank = AnalyticalFunction.rank(overClause);
    AnalyticalFunction denseRank = AnalyticalFunction.denseRank(overClause);
    
    assertEquals(AnalyticalFunction.AnalyticalFunctionType.ROW_NUMBER, rowNumber.getFunctionType());
    assertEquals(AnalyticalFunction.AnalyticalFunctionType.RANK, rank.getFunctionType());
    assertEquals(AnalyticalFunction.AnalyticalFunctionType.DENSE_RANK, denseRank.getFunctionType());
  }
}