package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

public class Comment extends Statement {

  private String what;

  public Comment(String what) {
    this.what = what;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Comment{" + what + "}";
  }

  public String toPostgre(Everything data) {
    return "  /* " + what + " */";
  }
}