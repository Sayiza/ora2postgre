package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents Oracle WITH clause containing Common Table Expressions (CTEs).
 * Oracle WITH clauses can contain both CTE definitions and PL/SQL function/procedure definitions.
 * PostgreSQL WITH clauses only support CTE definitions.
 */
public class SelectWithClause extends PlSqlAst {
  
  private List<CommonTableExpression> cteList;
  private List<Function> functions; // PL/SQL function definitions (Oracle-specific)
  private List<Procedure> procedures; // PL/SQL procedure definitions (Oracle-specific)
  private boolean recursive; // True if any CTE is recursive
  
  public SelectWithClause(List<CommonTableExpression> cteList, List<Function> functions, List<Procedure> procedures) {
    this.cteList = cteList != null ? cteList : new ArrayList<>();
    this.functions = functions != null ? functions : new ArrayList<>();
    this.procedures = procedures != null ? procedures : new ArrayList<>();
    this.recursive = false;
    
    // Check if any CTE is recursive
    for (CommonTableExpression cte : this.cteList) {
      if (cte.isRecursive()) {
        this.recursive = true;
        break;
      }
    }
  }
  
  public List<CommonTableExpression> getCteList() {
    return cteList;
  }
  
  public List<Function> getFunctions() {
    return functions;
  }
  
  public List<Procedure> getProcedures() {
    return procedures;
  }
  
  public boolean isRecursive() {
    return recursive;
  }
  
  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public String toString() {
    return "SelectWithClause{" +
           "cteList=" + cteList.size() +
           ", functions=" + functions.size() +
           ", procedures=" + procedures.size() +
           ", recursive=" + recursive +
           '}';
  }
  
  /**
   * Transforms Oracle WITH clause to PostgreSQL WITH clause.
   * Note: Oracle allows PL/SQL functions/procedures in WITH clause, but PostgreSQL doesn't.
   * These would need to be converted to separate function definitions.
   */
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    
    // Only process CTEs for now (functions/procedures would need separate handling)
    if (!cteList.isEmpty()) {
      b.append("WITH ");
      
      // Add RECURSIVE keyword if needed
      if (recursive) {
        b.append("RECURSIVE ");
      }
      
      // Add CTEs
      for (int i = 0; i < cteList.size(); i++) {
        if (i > 0) b.append(",\n");
        b.append(cteList.get(i).toPostgre(data));
      }
    }
    
    // TODO: Handle functions and procedures - these would need to be converted to 
    // separate PostgreSQL function definitions outside the query
    if (!functions.isEmpty() || !procedures.isEmpty()) {
      // For now, add a comment indicating this limitation
      b.append("\n-- TODO: Oracle WITH clause PL/SQL functions/procedures not yet supported");
    }
    
    return b.toString();
  }
}
