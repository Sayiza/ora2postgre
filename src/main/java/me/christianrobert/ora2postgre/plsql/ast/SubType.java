package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter;

public class SubType extends PlSqlAst {
  private String name;
  private String dataType; //TODO

  public SubType(String name, String dataType) {
    this.name = name;
    this.dataType = dataType;
  }

  public String getName() {
    return name;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "SubType{name=" + name + ", dataType=" + dataType + "}";
  }

  public String toJava() {
    StringBuilder b = new StringBuilder();
    b.append(TypeConverter.toJava(dataType))
            .append(" TODO ") //TODO
            .append(name)
    ;
    return b.toString();
  }

  public String toPostgre() {
    StringBuilder b = new StringBuilder();
    b.append(name)
            .append(" TODO ") //TODO
            .append(TypeConverter.toPostgre(dataType))
    ;
    return b.toString();
  }
}