package com.sayiza.oracle2postgre.plsql.ast;

public class TableExpressionClause extends PlSqlAst{

  private String schemaName;
  private String tableName;
  // TODO other possibilites like subqueries...

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  public TableExpressionClause(String schemaName, String tableName) {
    this.schemaName = schemaName;
    this.tableName = tableName;
  }

  public String getSchemaName() {
    return schemaName;
  }

  public String getTableName() {
    return tableName;
  }
}
