package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

public class VarrayType extends PlSqlAst {

  private String name;
  private Long sizeNumeric;
  private Expression sizeExpression;
  private DataTypeSpec dataTypespec; // TODO replace with TypespecDatatype Object
  // TODO also replace all other data type occurencies :-/

  public VarrayType(Long sizeNumeric, Expression sizeExpression, DataTypeSpec dataType) {
    this("", sizeNumeric, sizeExpression, dataType);
  }

  public VarrayType(String name, Long sizeNumeric, Expression sizeExpression, DataTypeSpec dataType) {
    this.name = name;
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
    // Convert Oracle VARRAY to PostgreSQL array type
    String baseType = dataTypespec.toPostgre(data);
    if (baseType != null && !baseType.contains("/* data type not implemented")) {
      return baseType + "[]";
    }
    return "text[]"; // fallback to text array
  }

  public String getName() {
    return name;
  }

  public DataTypeSpec getDataType() {
    return dataTypespec;
  }

  public Long getSize() {
    return sizeNumeric;
  }

  public Expression getSizeExpression() {
    return sizeExpression;
  }
}
