package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

public class ExceptionHandler extends PlSqlAst {
  private final List<String> exceptionNames; // Can handle multiple exceptions with OR
  private final List<Statement> statements; // THEN statements

  public ExceptionHandler(List<String> exceptionNames, List<Statement> statements) {
    this.exceptionNames = exceptionNames;
    this.statements = statements;
  }

  public List<String> getExceptionNames() {
    return exceptionNames;
  }

  public List<Statement> getStatements() {
    return statements;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "ExceptionHandler{" +
            "exceptions=" + exceptionNames +
            ", statements=" + (statements != null ? statements.size() : 0) + "}";
  }

  /**
   * Generate PostgreSQL exception handling code
   */
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();

    b.append(data.getIntendation()).append("WHEN ");

    // Handle exception name mapping (Oracle to PostgreSQL)
    if (exceptionNames != null && !exceptionNames.isEmpty()) {
      for (int i = 0; i < exceptionNames.size(); i++) {
        if (i > 0) {
          b.append(" OR ");
        }
        b.append(mapExceptionName(exceptionNames.get(i)));
      }
    } else {
      b.append("OTHERS"); // Default to catch-all
    }

    b.append(" THEN\n");

    // Generate statements with increased indentation
    if (statements != null && !statements.isEmpty()) {
      data.intendMore();
      for (Statement stmt : statements) {
        b.append(stmt.toPostgre(data)).append("\n");
      }
      data.intendLess();
    }

    return b.toString();
  }

  /**
   * Map Oracle exception names to PostgreSQL equivalents
   */
  private String mapExceptionName(String oracleException) {
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
      default -> "OTHERS"; // Catch-all for unknown exceptions
    };
  }
}