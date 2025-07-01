package com.sayiza.oracle2postgre.plsql.ast;

import com.sayiza.oracle2postgre.global.Everything;

public class NestedTableType extends PlSqlAst {

  private DataTypeSpec dataType; // TODO replace with TypesüecsDatatype Object
  // TODO also replace all other data type occurencies :-/

  public NestedTableType(DataTypeSpec dataType) {
    this.dataType = dataType;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  public String toJava(Everything data) {
    return dataType.toJava(data);
  }

  public String toPostgre(Everything data) {
    return dataType.toPostgre(data); // size?
  }
}
