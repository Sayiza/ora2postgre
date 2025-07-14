package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

public class Parameter extends PlSqlAst {
  private String name;
  private DataTypeSpec dataType;
  private Expression defaultValue;
  private boolean in;
  private boolean out;

  public Parameter(String name, DataTypeSpec dataType, Expression defaultValue, boolean in, boolean out) {
    this.name = name;
    this.dataType = dataType;
    this.defaultValue = defaultValue;
    this.in = in;
    this.out = out;
  }

  public String getName() {
    return name;
  }
  
  public DataTypeSpec getDataType() {
    return dataType;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Parameter{name=" + name + ", dataType=" + dataType + "}";
  }

  public String toJava(boolean web, Everything data) {
    StringBuilder b = new StringBuilder();
    if (web) {
      b.append("@QueryParam(\"" + name.toLowerCase() + "\") String"); //TODO actual type mapping?!
    } else {
      b.append(dataType.toJava(data));
    }
    b.append(" ")
            .append(name.toLowerCase())
    ;
    return b.toString();
  }

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    b.append(name.toLowerCase())
            .append(" ")
            .append((in && !out) ? "IN " : " ")
            .append((in && out) ? "INOUT " : " ")
            .append((!in && out) ? "OUT " : " ")
            .append((!in && !out) ? "IN " : " ")
            .append(dataType.toPostgre(data))
    ;
    return b.toString();
  }

  /**
   * Enhanced toPostgre method with function context for collection type resolution.
   * This enables function parameters to use function-local collection types.
   */
  public String toPostgre(Everything data, Function function) {
    StringBuilder b = new StringBuilder();
    b.append(name.toLowerCase())
            .append(" ")
            .append((in && !out) ? "IN " : " ")
            .append((in && out) ? "INOUT " : " ")
            .append((!in && out) ? "OUT " : " ")
            .append((!in && !out) ? "IN " : " ")
            .append(dataType.toPostgre(data, function))
    ;
    return b.toString();
  }
}