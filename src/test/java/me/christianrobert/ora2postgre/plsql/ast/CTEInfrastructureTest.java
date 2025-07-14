package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class CTEInfrastructureTest {

  @Test
  public void testCommonTableExpressionBasic() {
    // Test CommonTableExpression class directly
    System.out.println("=== Testing CommonTableExpression Class ===");
    
    CommonTableExpression cte = new CommonTableExpression(
        "employee_summary", 
        Arrays.asList("emp_id", "emp_name", "emp_salary"), 
        null, // No subquery to avoid table resolution issues
        false
    );
    
    Everything data = new Everything();
    String result = cte.toPostgre(data);
    
    System.out.println("CTE Result: " + result);
    assert result.equals("employee_summary (emp_id, emp_name, emp_salary) AS ()") : "Should generate correct CTE structure";
    
    // Test without column list
    CommonTableExpression simpleCte = new CommonTableExpression("simple_cte", null, null, false);
    String simpleResult = simpleCte.toPostgre(data);
    System.out.println("Simple CTE Result: " + simpleResult);
    assert simpleResult.equals("simple_cte AS ()") : "Should generate simple CTE structure";
    
    // Test recursive CTE
    CommonTableExpression recursiveCte = new CommonTableExpression("recursive_cte", null, null, true);
    String recursiveResult = recursiveCte.toPostgre(data);
    System.out.println("Recursive CTE Result: " + recursiveResult);
    assert recursiveResult.equals("recursive_cte AS ()") : "Should generate recursive CTE structure";
  }

  @Test
  public void testSelectWithClauseBasic() {
    // Test SelectWithClause class directly
    System.out.println("=== Testing SelectWithClause Class ===");
    
    CommonTableExpression cte1 = new CommonTableExpression("cte1", Arrays.asList("col1", "col2"), null, false);
    CommonTableExpression cte2 = new CommonTableExpression("cte2", null, null, false);
    CommonTableExpression recursiveCte = new CommonTableExpression("recursive_cte", null, null, true);
    
    List<CommonTableExpression> cteList = Arrays.asList(cte1, cte2);
    SelectWithClause withClause = new SelectWithClause(cteList, List.of(), List.of());
    
    Everything data = new Everything();
    String result = withClause.toPostgre(data);
    
    System.out.println("WITH Clause Result:");
    System.out.println(result);
    
    // Verify the structure
    assert result.contains("WITH cte1 (col1, col2) AS ()") : "Should contain first CTE";
    assert result.contains("cte2 AS ()") : "Should contain second CTE";
    assert result.contains(",") : "Should have comma between CTEs";
    
    // Test recursive WITH clause
    List<CommonTableExpression> recursiveList = Arrays.asList(recursiveCte);
    SelectWithClause recursiveWithClause = new SelectWithClause(recursiveList, List.of(), List.of());
    String recursiveResult = recursiveWithClause.toPostgre(data);
    
    System.out.println("Recursive WITH Clause Result:");
    System.out.println(recursiveResult);
    assert recursiveResult.contains("WITH RECURSIVE recursive_cte AS ()") : "Should contain RECURSIVE keyword";
  }

  @Test
  public void testSelectStatementWithClause() {
    // Test SelectStatement integration without table dependencies
    System.out.println("=== Testing SelectStatement with CTE ===");
    
    CommonTableExpression cte = new CommonTableExpression("test_cte", Arrays.asList("id", "name"), null, false);
    SelectWithClause withClause = new SelectWithClause(Arrays.asList(cte), List.of(), List.of());
    
    // For now, test just the WITH clause part since SelectSubQuery constructor is complex
    // The key test is that the WITH clause can be generated correctly
    Everything data = new Everything();
    String withResult = withClause.toPostgre(data);
    
    System.out.println("WITH clause integration test:");
    System.out.println(withResult);
    
    // The key test is that the WITH clause can be generated
    assert withResult.contains("WITH test_cte (id, name) AS ()") : "Should contain CTE definition";
    
    System.out.println("WITH clause integration successful - ready for full SELECT statement integration");
  }

  @Test
  public void testCTEMetadata() {
    // Test CTE metadata and properties
    System.out.println("=== Testing CTE Metadata ===");
    
    CommonTableExpression cte = new CommonTableExpression(
        "employee_data", 
        Arrays.asList("emp_id", "first_name", "last_name"), 
        null, 
        false
    );
    
    // Test getters
    assert cte.getQueryName().equals("employee_data") : "Should return correct query name";
    assert cte.getColumnList().size() == 3 : "Should have 3 columns";
    assert cte.getColumnList().get(0).equals("emp_id") : "Should have correct first column";
    assert !cte.isRecursive() : "Should not be recursive";
    
    // Test recursive CTE
    CommonTableExpression recursiveCte = new CommonTableExpression("hierarchy", null, null, true);
    assert recursiveCte.isRecursive() : "Should be recursive";
    
    // Test SelectWithClause metadata
    SelectWithClause withClause = new SelectWithClause(Arrays.asList(cte, recursiveCte), List.of(), List.of());
    assert withClause.getCteList().size() == 2 : "Should have 2 CTEs";
    assert withClause.isRecursive() : "Should be marked as recursive because one CTE is recursive";
    
    System.out.println("CTE metadata tests passed!");
  }
}