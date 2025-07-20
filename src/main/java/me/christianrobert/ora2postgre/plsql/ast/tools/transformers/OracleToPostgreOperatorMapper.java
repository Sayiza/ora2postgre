package me.christianrobert.ora2postgre.plsql.ast.tools.transformers;

/**
 * Centralized Oracle to PostgreSQL operator transformation utility.
 * 
 * PHASE 3 REFACTORING: This class consolidates operator mapping logic that was
 * previously scattered across multiple expression classes (RelationalExpression,
 * CompoundExpression, Concatenation, etc.).
 * 
 * ARCHITECTURAL BENEFIT: Single source of truth for operator transformations,
 * eliminating duplication and providing consistent mapping.
 */
public class OracleToPostgreOperatorMapper {
  
  /**
   * Transform Oracle logical operators to PostgreSQL equivalents.
   * Used for AND, OR, NOT operations.
   */
  public static String transformLogicalOperator(String oracleOperator) {
    if (oracleOperator == null) {
      return "";
    }
    
    switch (oracleOperator.toUpperCase()) {
      case "AND":
        return "AND";
      case "OR":
        return "OR";
      case "NOT":
        return "NOT";
      default:
        return oracleOperator.toUpperCase();
    }
  }
  
  /**
   * Transform Oracle comparison operators to PostgreSQL equivalents.
   * Used for =, !=, <>, <, <=, >, >= operations.
   */
  public static String transformComparisonOperator(String oracleOperator) {
    if (oracleOperator == null) {
      return "";
    }
    
    switch (oracleOperator) {
      case "=":
        return "=";
      case "!=":
      case "<>":
        return "<>"; // PostgreSQL prefers <> over !=
      case "<":
        return "<";
      case "<=":
        return "<=";
      case ">":
        return ">";
      case ">=":
        return ">=";
      default:
        return oracleOperator;
    }
  }
  
  /**
   * Transform Oracle arithmetic operators to PostgreSQL equivalents.
   * Used for +, -, *, /, MOD operations.
   */
  public static String transformArithmeticOperator(String oracleOperator) {
    if (oracleOperator == null) {
      return "";
    }
    
    switch (oracleOperator.toUpperCase()) {
      case "+":
        return "+";
      case "-":
        return "-";
      case "*":
        return "*";
      case "/":
        return "/";
      case "MOD":
        return "%"; // PostgreSQL uses % for modulo
      default:
        return oracleOperator;
    }
  }
  
  /**
   * Transform Oracle string operators to PostgreSQL equivalents.
   * Used for concatenation and pattern matching.
   */
  public static String transformStringOperator(String oracleOperator) {
    if (oracleOperator == null) {
      return "";
    }
    
    switch (oracleOperator.toUpperCase()) {
      case "||":
        return "||"; // Same in PostgreSQL
      case "LIKE":
        return "LIKE";
      case "NOT LIKE":
        return "NOT LIKE";
      case "ILIKE":
        return "ILIKE"; // PostgreSQL-specific case-insensitive LIKE
      default:
        return oracleOperator;
    }
  }
  
  /**
   * Transform Oracle set operators to PostgreSQL equivalents.
   * Used for IN, NOT IN, EXISTS operations.
   */
  public static String transformSetOperator(String oracleOperator) {
    if (oracleOperator == null) {
      return "";
    }
    
    switch (oracleOperator.toUpperCase()) {
      case "IN":
        return "IN";
      case "NOT IN":
        return "NOT IN";
      case "EXISTS":
        return "EXISTS";
      case "NOT EXISTS":
        return "NOT EXISTS";
      default:
        return oracleOperator.toUpperCase();
    }
  }
  
  /**
   * Transform Oracle range operators to PostgreSQL equivalents.
   * Used for BETWEEN, NOT BETWEEN operations.
   */
  public static String transformRangeOperator(String oracleOperator) {
    if (oracleOperator == null) {
      return "";
    }
    
    switch (oracleOperator.toUpperCase()) {
      case "BETWEEN":
        return "BETWEEN";
      case "NOT BETWEEN":
        return "NOT BETWEEN";
      default:
        return oracleOperator.toUpperCase();
    }
  }
  
  /**
   * General operator transformation method that delegates to specific methods.
   * Automatically detects operator type and applies appropriate transformation.
   * 
   * USAGE: This is the main entry point for operator transformation when the
   * specific operator type is not known.
   */
  public static String transformOperator(String oracleOperator) {
    if (oracleOperator == null || oracleOperator.trim().isEmpty()) {
      return "";
    }
    
    String upper = oracleOperator.toUpperCase();
    
    // Logical operators
    if (upper.equals("AND") || upper.equals("OR") || upper.equals("NOT")) {
      return transformLogicalOperator(oracleOperator);
    }
    
    // Comparison operators
    if (upper.equals("=") || upper.equals("!=") || upper.equals("<>") || 
        upper.equals("<") || upper.equals("<=") || upper.equals(">") || upper.equals(">=")) {
      return transformComparisonOperator(oracleOperator);
    }
    
    // Arithmetic operators
    if (upper.equals("+") || upper.equals("-") || upper.equals("*") || 
        upper.equals("/") || upper.equals("MOD")) {
      return transformArithmeticOperator(oracleOperator);
    }
    
    // String operators
    if (upper.equals("||") || upper.contains("LIKE")) {
      return transformStringOperator(oracleOperator);
    }
    
    // Set operators
    if (upper.contains("IN") || upper.contains("EXISTS")) {
      return transformSetOperator(oracleOperator);
    }
    
    // Range operators
    if (upper.contains("BETWEEN")) {
      return transformRangeOperator(oracleOperator);
    }
    
    // Default: pass through unknown operators with warning comment
    return oracleOperator + " /* Unknown operator - review transformation */";
  }
  
  /**
   * Get operator precedence for PostgreSQL (higher number = higher precedence).
   * Useful for determining when parentheses are needed in complex expressions.
   */
  public static int getOperatorPrecedence(String operator) {
    if (operator == null) {
      return 0;
    }
    
    String upper = operator.toUpperCase();
    
    // Arithmetic operators (highest precedence)
    if (upper.equals("*") || upper.equals("/") || upper.equals("%")) {
      return 10;
    }
    if (upper.equals("+") || upper.equals("-")) {
      return 9;
    }
    
    // String concatenation
    if (upper.equals("||")) {
      return 8;
    }
    
    // Comparison operators
    if (upper.equals("=") || upper.equals("<>") || upper.equals("!=") || 
        upper.equals("<") || upper.equals("<=") || upper.equals(">") || upper.equals(">=")) {
      return 7;
    }
    
    // Pattern matching
    if (upper.contains("LIKE") || upper.contains("IN") || upper.contains("BETWEEN")) {
      return 6;
    }
    
    // Logical NOT
    if (upper.equals("NOT")) {
      return 5;
    }
    
    // Logical AND
    if (upper.equals("AND")) {
      return 4;
    }
    
    // Logical OR (lowest precedence)
    if (upper.equals("OR")) {
      return 3;
    }
    
    return 1; // Unknown operators get low precedence
  }
}