package com.sayiza.oracle2postgre.plsql.ast;

public class SelectFetchClause extends PlSqlAst {

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

}
