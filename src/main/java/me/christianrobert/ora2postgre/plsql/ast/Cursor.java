package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter;

public class Cursor extends PlSqlAst {
  private String name;
  private String content; //TODO

  public Cursor(String name, String content) {
    this.name = name;
    this.content = content;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Cursor{name=" + name + "}";
  }

  public String toJava(Everything data) {
    StringBuilder b = new StringBuilder();
    b.append("TODO ")
            .append(" ") //TODO
            .append(name)
    ;
    return b.toString();
  }

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    b.append(name)
            .append(" ") //TODO
            .append(TypeConverter.toPostgre(" TODO "))
    ;
    return b.toString();
  }
}