package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

public class VarrayType extends PlSqlAst {

  private Long sizeNumeric;
  private Expression sizeExpression;
  private DataTypeSpec dataTypespec; // TODO replace with TypespecDatatype Object
  // TODO also replace all other data type occurencies :-/

  public VarrayType(Long sizeNumeric, Expression sizeExpression, DataTypeSpec dataType) {
    this.sizeNumeric = sizeNumeric;
    this.sizeExpression = sizeExpression;
    this.dataTypespec = dataType;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  public String toJava(Everything data) {
    return dataTypespec.toJava(data);
  }

  public String toPostgre(Everything data) {
    return dataTypespec.toPostgre(data); // size?
  }
}
