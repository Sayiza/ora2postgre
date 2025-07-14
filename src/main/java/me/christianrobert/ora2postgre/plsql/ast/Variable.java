package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

public class Variable extends PlSqlAst {
  private String name;
  private DataTypeSpec dataType;
  private Expression defaultValue;

  public Variable(String name, DataTypeSpec dataType, Expression defaultValue) {
    this.name = name;
    this.dataType = dataType;
    this.defaultValue = defaultValue;
  }

  public String getName() {
    return name;
  }

  public DataTypeSpec getDataType() {
    return dataType;
  }

  public Expression getDefaultValue() { return defaultValue; }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Variable{name=" + name + ", dataType=" + dataType + "}";
  }

  // toJava() method removed - variables stay in PostgreSQL

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    b.append(name)
            .append(" ")
            .append(dataType.toPostgre(data));
    
    // Add default value if present
    if (defaultValue != null) {
      b.append(" := ").append(defaultValue.toPostgre(data));
    }
    
    return b.toString();
  }

  public String toPostgre(Everything data, Function function) {
    StringBuilder b = new StringBuilder();
    b.append(name)
            .append(" ")
            .append(dataType.toPostgre(data, function));
    
    // Add default value if present
    if (defaultValue != null) {
      b.append(" := ").append(defaultValue.toPostgre(data));
    }
    
    return b.toString();
  }
}