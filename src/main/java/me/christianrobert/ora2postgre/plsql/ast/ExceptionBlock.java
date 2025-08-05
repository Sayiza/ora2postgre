package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

public class ExceptionBlock extends PlSqlAst {
  private final List<ExceptionHandler> handlers;

  public ExceptionBlock(List<ExceptionHandler> handlers) {
    this.handlers = handlers;
  }

  public List<ExceptionHandler> getHandlers() {
    return handlers;
  }

  public boolean hasHandlers() {
    return handlers != null && !handlers.isEmpty();
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "ExceptionBlock{" +
            "handlers=" + (handlers != null ? handlers.size() : 0) + "}";
  }

  /**
   * Generate PostgreSQL exception block code
   */
  public String toPostgre(Everything data) {
    if (!hasHandlers()) {
      return "";
    }

    StringBuilder b = new StringBuilder();

    b.append("EXCEPTION\n");

    // Generate all exception handlers
    for (ExceptionHandler handler : handlers) {
      b.append(handler.toPostgre(data));
    }

    return b.toString();
  }
}