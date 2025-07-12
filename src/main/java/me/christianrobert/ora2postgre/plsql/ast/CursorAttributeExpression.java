package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

/**
 * AST class representing Oracle cursor attributes (%FOUND, %NOTFOUND, %ROWCOUNT, %ISOPEN).
 * 
 * This class handles the transformation of Oracle cursor attributes to their PostgreSQL equivalents:
 * - cursor%FOUND → FOUND
 * - cursor%NOTFOUND → NOT FOUND  
 * - cursor%ROWCOUNT → GET DIAGNOSTICS variable = ROW_COUNT (with comment)
 * - cursor%ISOPEN → manual cursor state tracking (with comment)
 */
public class CursorAttributeExpression extends PlSqlAst {
  
  /**
   * Enumeration of supported cursor attribute types.
   */
  public enum CursorAttributeType {
    FOUND,
    NOTFOUND, 
    ROWCOUNT,
    ISOPEN
  }
  
  private final String cursorName;
  private final CursorAttributeType attributeType;
  
  /**
   * Constructor for cursor attribute expression.
   * 
   * @param cursorName The name of the cursor (e.g., "emp_cursor")
   * @param attributeType The type of attribute (%FOUND, %NOTFOUND, etc.)
   */
  public CursorAttributeExpression(String cursorName, CursorAttributeType attributeType) {
    this.cursorName = cursorName;
    this.attributeType = attributeType;
  }
  
  public String getCursorName() {
    return cursorName;
  }
  
  public CursorAttributeType getAttributeType() {
    return attributeType;
  }
  
  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public String toString() {
    return cursorName + "%" + attributeType.name();
  }
  
  /**
   * Transforms Oracle cursor attributes to PostgreSQL equivalents.
   * 
   * @param data The context data for transformation
   * @return PostgreSQL equivalent of the cursor attribute
   */
  public String toPostgre(Everything data) {
    switch (attributeType) {
      case FOUND:
        return "FOUND";
        
      case NOTFOUND:
        return "NOT FOUND";
        
      case ROWCOUNT:
        return "/* " + cursorName + "%ROWCOUNT - use GET DIAGNOSTICS variable = ROW_COUNT */";
        
      case ISOPEN:
        return "/* " + cursorName + "%ISOPEN - manual cursor state tracking required */";
        
      default:
        return "/* UNKNOWN CURSOR ATTRIBUTE: " + attributeType + " */";
    }
  }
}