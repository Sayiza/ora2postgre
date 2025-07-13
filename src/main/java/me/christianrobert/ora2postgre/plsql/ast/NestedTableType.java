package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

public class NestedTableType extends PlSqlAst {

  private String name;
  private DataTypeSpec dataType; // TODO replace with TypesüecsDatatype Object
  // TODO also replace all other data type occurencies :-/

  public NestedTableType(DataTypeSpec dataType) {
    this("", dataType);
  }

  public NestedTableType(String name, DataTypeSpec dataType) {
    this.name = name;
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
    // Convert Oracle TABLE OF to PostgreSQL array type
    String baseType = dataType.toPostgre(data);
    if (baseType != null && !baseType.contains("/* data type not implemented")) {
      return baseType + "[]";
    }
    return "text[]"; // fallback to text array
  }

  public String getName() {
    return name;
  }

  public DataTypeSpec getDataType() {
    return dataType;
  }
}
