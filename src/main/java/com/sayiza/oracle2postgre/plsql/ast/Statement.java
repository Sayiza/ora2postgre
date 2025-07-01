package com.sayiza.oracle2postgre.plsql.ast;

import com.sayiza.oracle2postgre.global.Everything;

public class Statement extends PlSqlAst {

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "For Statement {code=" + "}";
  }

  public String toJava(Everything data) {
    return " /* statement not implemented " + this.getClass() + " */ ";
  }

  public String toPostgre(Everything data) {
    return " /* statement not implemented  */ ";
  }
}
