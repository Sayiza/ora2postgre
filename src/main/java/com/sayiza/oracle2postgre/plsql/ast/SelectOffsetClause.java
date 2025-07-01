package com.sayiza.oracle2postgre.plsql.ast;

public class SelectOffsetClause extends PlSqlAst {

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

}
