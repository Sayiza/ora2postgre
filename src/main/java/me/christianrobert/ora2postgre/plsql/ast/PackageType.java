package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

public class PackageType extends PlSqlAst {
  private String name;
  private DataTypeSpec dataType; //TODO

  public PackageType(String name, DataTypeSpec dataType) {
    this.name = name;
    this.dataType = dataType;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "PackageType{name=" + name + ", dataType=" + dataType + "}";
  }

  public String toJava(Everything data) {
    StringBuilder b = new StringBuilder();
    b.append(data.getIntendation())
            .append(dataType.toJava(data))
            .append(" ")
            .append(name)
            .append(";") //TODO
    ;
    return b.toString();
  }

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    b.append(name)
            .append(" ") //TODO
            .append(dataType.toPostgre(data))
    ;
    return b.toString();
  }
}