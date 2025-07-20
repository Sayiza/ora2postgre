package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils;

import java.util.List;

/**
 * Expression wrapper class - ARCHITECTURAL DEBT
 * 
 * NOTE: This class represents architectural debt from the chaotic expression hierarchy.
 * It uses a problematic either/or pattern that should be refactored.
 * 
 * CURRENT ISSUES:
 * - Runtime null checks instead of compile-time type safety
 * - Complex delegation chains through 8+ expression levels  
 * - Either/or pattern instead of proper inheritance/composition
 * - Collection constructor logic scattered across multiple classes
 * 
 * PHASE 2 REFACTORING PLAN:
 * - Replace either/or pattern with direct expression types
 * - Flatten the unnecessary hierarchy layers
 * - Consolidate transformation logic in Everything context
 * 
 * USAGE: This class should eventually be replaced with ExpressionBase hierarchy
 */
public class Expression extends PlSqlAst {
  private final CursorExpression cursorExpression;
  private final LogicalExpression logicalExpression;

  // Constructor for cursor expression
  public Expression(CursorExpression cursorExpression) {
    this.cursorExpression = cursorExpression;
    this.logicalExpression = null;
  }

  // Constructor for logical expression  
  public Expression(LogicalExpression logicalExpression) {
    this.cursorExpression = null;
    this.logicalExpression = logicalExpression;
  }

  public boolean isCursorExpression() {
    return cursorExpression != null;
  }

  public boolean isLogicalExpression() {
    return logicalExpression != null;
  }

  public CursorExpression getCursorExpression() {
    return cursorExpression;
  }

  public LogicalExpression getLogicalExpression() {
    return logicalExpression;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    if (cursorExpression != null) {
      return cursorExpression.toString();
    } else if (logicalExpression != null) {
      return logicalExpression.toString();
    } else {
      return "Expression{}";
    }
  }

  // toJava() method removed - expressions stay in PostgreSQL

  /**
   * Transform to PostgreSQL syntax using clean Everything-based semantic resolution.
   * 
   * ARCHITECTURAL NOTE: This method demonstrates the either/or delegation pattern
   * that should be eliminated in future refactoring. The transformation logic
   * properly uses Everything context for semantic analysis.
   */
  public String toPostgre(Everything data) {
    // Validate state - this check highlights the either/or pattern problem
    if (cursorExpression == null && logicalExpression == null) {
      throw new IllegalStateException("Expression must have either cursor or logical expression - this highlights the either/or pattern problem");
    }
    
    // ARCHITECTURAL DEBT: Either/or delegation
    if (cursorExpression != null) {
      if (logicalExpression != null) {
        throw new IllegalStateException("Expression cannot have both cursor and logical expression - either/or pattern violation");
      }
      return cursorExpression.toPostgre(data);
    } else {
      // logicalExpression != null (validated above)
      return logicalExpression.toPostgre(data);
    }
  }

  /**
   * Determines if this expression represents a function call.
   * 
   * ARCHITECTURAL DEBT: This method uses string-based analysis due to the
   * complex expression hierarchy. Should be replaced with proper type checking
   * when the hierarchy is flattened.
   */
  public boolean isFunctionCall() {
    // EITHER/OR DELEGATION: Another example of the pattern problem
    if (cursorExpression != null) {
      return false; // Cursor expressions are not function calls
    } else if (logicalExpression != null) {
      // String-based analysis - architectural debt
      String text = logicalExpression.toString();
      return text != null && text.contains("(") && text.contains(")");
    } else {
      return false; // Invalid state
    }
  }

  /**
   * Gets the processed name with schema prefix for PostgreSQL output.
   * Adds schema prefix for function calls to packages/object types when needed.
   * Returns raw text for simple column references.
   */
  public String getNameForStatementExpressionWithSchema(Everything data, String schemaWhereTheStatementIsRunning, List<TableReference> fromTables) {
    String expressionText = this.toString();
    String schemaPrefix = data.lookupSchemaForExpression(this, schemaWhereTheStatementIsRunning, fromTables);
    
    if (schemaPrefix != null) {
      // A schema prefix has been found, so it is a function
      // if the text starts with the Schema prefix (case insenstive compared)
      // then we do not need to add the schema again, but need to follow the convention
      // for schema and function/procedurenaming: the schema is always upper case,
      // the packagename is also upper case and a PREFIX (with a following underscore)
      // to the function/procedurename which is always lowercase
      // if the schema name is present it will be added.
      // so in both cases the segment that is the packagename must be found, uppercased
      // and the dot between packagename and function/procedurename needs to be replaced
      // with an underscore.
      
      String processedText = expressionText;
      boolean hasSchemaInText = false;
      
      // Check if the text already starts with the schema prefix (case insensitive)
      if (expressionText.toLowerCase().startsWith(schemaPrefix.toLowerCase() + ".")) {
        // Remove the schema prefix from the text since we'll handle it separately  
        processedText = expressionText.substring(schemaPrefix.length() + 1);
        hasSchemaInText = true;
      }
      
      // Find the package name and function name
      int dotIndex = processedText.indexOf('.');
      if (dotIndex > 0) {
        String packageName = processedText.substring(0, dotIndex);
        String functionName = processedText.substring(dotIndex + 1);
        
        // Apply naming convention: SCHEMA.PACKAGE_functionname
        String result = schemaPrefix.toUpperCase() + "." + 
                       packageName.toUpperCase() + "_" + 
                       functionName.toLowerCase();
        return result;
      } else {
        // No package separation found, treat as simple function
        return schemaPrefix.toUpperCase() + "." + processedText.toLowerCase();
      }
    }
    
    // Return expression text for simple column references
    if (expressionText.contains(".")) {
      return expressionText; // TODO it has a table alias or name prefix..
    }
    
    // Check if this is a literal constant that should not be quoted
    if (isLiteralConstant(expressionText)) {
      return expressionText; // Return literals unchanged
    }
    
    return PostgreSqlIdentifierUtils.quoteIdentifier(expressionText.toUpperCase());
  }

  /**
   * Determines if the given expression text represents a literal constant that should
   * not be quoted as an identifier.
   * 
   * @param expressionText The text to check
   * @return true if this is a literal constant (number, string, boolean, etc.)
   */
  private boolean isLiteralConstant(String expressionText) {
    if (expressionText == null || expressionText.trim().isEmpty()) {
      return false;
    }
    
    String trimmed = expressionText.trim();
    
    // Check for numeric literals (integers, decimals, scientific notation)
    if (isNumericLiteral(trimmed)) {
      return true;
    }
    
    // Check for string literals (single quoted)
    if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2) {
      return true;
    }
    
    // Check for boolean literals
    if (trimmed.equalsIgnoreCase("TRUE") || trimmed.equalsIgnoreCase("FALSE")) {
      return true;
    }
    
    // Check for NULL literal
    if (trimmed.equalsIgnoreCase("NULL")) {
      return true;
    }
    
    return false;
  }
  
  /**
   * Checks if a string represents a numeric literal.
   */
  private boolean isNumericLiteral(String text) {
    if (text == null || text.isEmpty()) {
      return false;
    }
    
    try {
      // Try parsing as different numeric types
      if (text.contains(".") || text.toLowerCase().contains("e")) {
        // Decimal or scientific notation
        Double.parseDouble(text);
      } else {
        // Integer
        Long.parseLong(text);
      }
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

}