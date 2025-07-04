package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

public class TableReference extends PlSqlAst {

  private TableReferenceAux tableReferenceAux; // OR
  private JoinClause joinClause;
  private PivotClause pivotClause;
  private UnPivotClause unPivotClause;

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  public TableReference(TableReferenceAux tableReferenceAux, JoinClause joinClause, PivotClause pivotClause, UnPivotClause unPivotClause) {
    this.tableReferenceAux = tableReferenceAux;
    this.joinClause = joinClause;
    this.pivotClause = pivotClause;
    this.unPivotClause = unPivotClause;
  }

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    b.append(tableReferenceAux.toPostgre(data));
    //data.lookupSchema4Field(tableName, schema))
    //TODO append clauses

    return b.toString();
  }

  public String getSchemaName() {
    return tableReferenceAux.getSchemaName();
  }

  public String getTableName() {
    return tableReferenceAux.getTableName();
  }

  public String getTableAlias() {
    return tableReferenceAux.getTableAlias();
  }
}
