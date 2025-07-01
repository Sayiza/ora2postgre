package com.sayiza.oracle2postgre.plsql.ast;

import com.sayiza.oracle2postgre.global.Everything;

public class TableReferenceAux extends PlSqlAst {

  private TableReferenceAuxInternal referenceAuxInternal;
  private FlashbackClause flashbackClause;
  private String alias;

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  public TableReferenceAux(TableReferenceAuxInternal referenceAuxInternal, FlashbackClause flashbackClause, String alias) {
    this.referenceAuxInternal = referenceAuxInternal;
    this.flashbackClause = flashbackClause;
    this.alias = alias;
  }

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    b.append(referenceAuxInternal.toPostgre(data));
    if (alias != null) {
      b.append(" AS ").append(alias);
    }
    //TODO append clauses

    return b.toString();
  }

  public String getSchemaName() {
    return referenceAuxInternal.getSchemaName();
  }

  public String getTableName() {
    return referenceAuxInternal.getTableName();
  }

  public String getTableAlias() {
    return alias;
  }
}
