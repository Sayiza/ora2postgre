package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

public class Statement extends PlSqlAst {

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "For Statement {code=" + "}";
  }

  public String toPostgre(Everything data) {
    return " /* statement not implemented  */ ";
  }
}
