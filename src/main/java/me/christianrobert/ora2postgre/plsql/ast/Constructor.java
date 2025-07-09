package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.ToExportPostgre;

import java.util.List;

public class Constructor extends PlSqlAst {
  private String name;
  private List<Parameter> parameters;
  private List<Statement> statements;

  private ObjectType parentType;

  public Constructor(String name, List<Parameter> parameters, List<Statement> statements) {
    this.name = name;
    this.parameters = parameters;
    this.statements = statements;
  }

  public ObjectType getParentType() {
    return parentType;
  }

  public void setParentType(ObjectType parentType) {
    this.parentType = parentType;
  }

  public List<Statement> getStatements() {
    return statements;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Constructor{name=" + name + ", parameters=" + parameters + ", body=?}";
  }

  // toJava() method removed - constructors become PostgreSQL functions

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    b.append("CREATE FUNCTION ")
            .append(getParentType().getSchema().toUpperCase())
            .append(".")
            .append("_CONSTRUCTOR ")
            .append( name.toUpperCase())
            .append("(");
    ToExportPostgre.doParametersPostgre(b, parameters, data);
    b.append(")")
            .append("TODO") //TODO
    ;
    return b.toString();
  }
}