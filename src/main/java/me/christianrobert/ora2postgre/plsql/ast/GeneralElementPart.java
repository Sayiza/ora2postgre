package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import java.util.ArrayList;
import java.util.List;

/**
 * AST class representing general_element_part from the grammar.
 * Grammar rule: general_element_part
 *   : (INTRODUCER char_set_name)? id_expression ('@' link_name)? function_argument*
 * 
 * This class handles individual parts of general elements, including function calls
 * and collection indexing operations.
 */
public class GeneralElementPart extends PlSqlAst {
  private final String introducerCharSet;
  private final String idExpression;
  private final String linkName;
  private final List<Expression> functionArguments;

  // Constructor for simple identifier
  public GeneralElementPart(String idExpression) {
    this.introducerCharSet = null;
    this.idExpression = idExpression;
    this.linkName = null;
    this.functionArguments = new ArrayList<>();
  }

  // Constructor for identifier with function arguments
  public GeneralElementPart(String idExpression, List<Expression> functionArguments) {
    this.introducerCharSet = null;
    this.idExpression = idExpression;
    this.linkName = null;
    this.functionArguments = functionArguments != null ? new ArrayList<>(functionArguments) : new ArrayList<>();
  }

  // Full constructor
  public GeneralElementPart(String introducerCharSet, String idExpression, String linkName, 
                           List<Expression> functionArguments) {
    this.introducerCharSet = introducerCharSet;
    this.idExpression = idExpression;
    this.linkName = linkName;
    this.functionArguments = functionArguments != null ? new ArrayList<>(functionArguments) : new ArrayList<>();
  }

  public String getIntroducerCharSet() {
    return introducerCharSet;
  }

  public String getIdExpression() {
    return idExpression;
  }

  public String getLinkName() {
    return linkName;
  }

  public List<Expression> getFunctionArguments() {
    return new ArrayList<>(functionArguments);
  }

  public boolean hasFunctionArguments() {
    return !functionArguments.isEmpty();
  }

  public boolean isSimpleIdentifier() {
    return introducerCharSet == null && linkName == null && functionArguments.isEmpty();
  }

  public boolean isFunctionCall() {
    return !functionArguments.isEmpty();
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    
    if (introducerCharSet != null) {
      sb.append("INTRODUCER ").append(introducerCharSet).append(" ");
    }
    
    sb.append(idExpression);
    
    if (linkName != null) {
      sb.append("@").append(linkName);
    }
    
    if (!functionArguments.isEmpty()) {
      sb.append("(");
      for (int i = 0; i < functionArguments.size(); i++) {
        if (i > 0) sb.append(", ");
        sb.append(functionArguments.get(i).toString());
      }
      sb.append(")");
    }
    
    return sb.toString();
  }

  public String toPostgre(Everything data) {
    StringBuilder sb = new StringBuilder();
    
    // Handle introducer charset if present (rarely used)
    if (introducerCharSet != null) {
      // For now, ignore introducer charset in PostgreSQL
      // Could be used for character set conversion if needed
    }
    
    // Base identifier
    sb.append(idExpression);
    
    // Handle link name (database links)
    if (linkName != null) {
      // PostgreSQL doesn't have database links - comment it out
      sb.append(" /* @").append(linkName).append(" - database link not supported */");
    }
    
    // Handle function arguments (this includes collection indexing)
    if (!functionArguments.isEmpty()) {
      sb.append("(");
      for (int i = 0; i < functionArguments.size(); i++) {
        if (i > 0) sb.append(", ");
        // Transform each argument - this is where collection methods in indices get transformed
        sb.append(functionArguments.get(i).toPostgre(data));
      }
      sb.append(")");
    }
    
    return sb.toString();
  }

  /**
   * Check if this part represents a collection indexing operation.
   * Collection indexing has exactly one function argument.
   */
  public boolean isCollectionIndexing() {
    return functionArguments.size() == 1;
  }

  /**
   * Get the index expression for collection indexing.
   */
  public Expression getIndexExpression() {
    if (isCollectionIndexing()) {
      return functionArguments.get(0);
    }
    return null;
  }
}