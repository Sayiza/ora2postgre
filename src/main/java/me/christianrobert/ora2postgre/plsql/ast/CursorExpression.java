package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

public class CursorExpression extends PlSqlAst {

  private SelectSubQuery subquery;
  private String schema;

  public CursorExpression(SelectSubQuery subquery, String schema) {
    this.subquery = subquery;
    this.schema = schema;
  }

  public SelectSubQuery getSubquery() {
    return subquery;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "CURSOR(" + (subquery != null ? subquery.toString() : "") + ")";
  }

  public String toPostgre(Everything data) {
    // In PostgreSQL, cursor expressions can be handled with CURSOR FOR syntax
    if (subquery != null) {
      return "CURSOR FOR " + subquery.toPostgre(data, schema);
    }
    return "CURSOR FOR /* TODO: handle empty cursor */";
  }
}