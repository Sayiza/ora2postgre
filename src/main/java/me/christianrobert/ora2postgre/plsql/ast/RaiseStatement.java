package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

/**
 * AST class for Oracle RAISE statements.
 * Supports both specific exception raising and re-raising of current exceptions.
 * 
 * Oracle patterns:
 * - RAISE exception_name;        (raise specific exception)
 * - RAISE;                       (re-raise current exception)
 * - RAISE exception_name('message');  (raise with message - future enhancement)
 * 
 * PostgreSQL transformation:
 * - RAISE exception_name;        → RAISE exception_name;
 * - RAISE;                       → RAISE;
 */
public class RaiseStatement extends Statement {
  private final String exceptionName; // null for bare RAISE (re-raise)
  private final String message;       // null if no message provided

  /**
   * Constructor for RAISE statement with exception name only
   */
  public RaiseStatement(String exceptionName) {
    this.exceptionName = exceptionName;
    this.message = null;
  }

  /**
   * Constructor for RAISE statement with exception name and message
   */
  public RaiseStatement(String exceptionName, String message) {
    this.exceptionName = exceptionName;
    this.message = message;
  }

  /**
   * Constructor for bare RAISE (re-raise current exception)
   */
  public RaiseStatement() {
    this.exceptionName = null;
    this.message = null;
  }

  public String getExceptionName() {
    return exceptionName;
  }

  public String getMessage() {
    return message;
  }

  public boolean isReRaise() {
    return exceptionName == null;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    if (isReRaise()) {
      return "RaiseStatement{re-raise}";
    } else if (message != null) {
      return "RaiseStatement{exception=" + exceptionName + ", message=" + message + "}";
    } else {
      return "RaiseStatement{exception=" + exceptionName + "}";
    }
  }

  @Override
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    b.append("RAISE ");

    if (isReRaise()) {
      // Bare RAISE - re-raise current exception
      b.append(";");
    } else {
      // RAISE with specific exception name
      String pgExceptionName = mapExceptionName(exceptionName);
      b.append(pgExceptionName);
      
      // Add message if provided (future enhancement)
      if (message != null && !message.trim().isEmpty()) {
        // PostgreSQL RAISE with message: RAISE exception_name USING MESSAGE = 'text';
        b.append(" USING MESSAGE = '").append(message.replace("'", "''")).append("'");
      }
      
      b.append(";");
    }

    return b.toString();
  }

  /**
   * Map Oracle exception names to PostgreSQL equivalents.
   * Uses the same mapping as ExceptionHandler for consistency.
   */
  private String mapExceptionName(String oracleException) {
    if (oracleException == null) {
      return "OTHERS";
    }
    
    return switch (oracleException.toUpperCase()) {
      case "NO_DATA_FOUND" -> "NO_DATA_FOUND";
      case "TOO_MANY_ROWS" -> "TOO_MANY_ROWS";
      case "DUP_VAL_ON_INDEX" -> "unique_violation";
      case "INVALID_CURSOR" -> "INVALID_CURSOR_STATE";
      case "INVALID_NUMBER" -> "invalid_text_representation";
      case "VALUE_ERROR" -> "data_exception";
      case "ZERO_DIVIDE" -> "division_by_zero";
      case "STORAGE_ERROR" -> "insufficient_resources";
      case "PROGRAM_ERROR" -> "internal_error";
      case "CURSOR_ALREADY_OPEN" -> "CURSOR_ALREADY_OPEN";
      case "ACCESS_INTO_NULL" -> "null_value_not_allowed";
      case "COLLECTION_IS_NULL" -> "null_value_not_allowed";
      case "SUBSCRIPT_BEYOND_COUNT" -> "array_subscript_error";
      case "SUBSCRIPT_OUTSIDE_LIMIT" -> "array_subscript_error";
      case "CASE_NOT_FOUND" -> "case_not_found";
      case "SELF_IS_NULL" -> "null_value_not_allowed";
      case "TIMEOUT_ON_RESOURCE" -> "lock_not_available";
      default -> oracleException; // Keep original name for user-defined exceptions
    };
  }
}