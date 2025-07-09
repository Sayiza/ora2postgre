package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

public class SelectSubQueryBasicElement extends PlSqlAst {

  SelectSubQuery subQuery;
  SelectQueryBlock queryBlock;

  public SelectSubQueryBasicElement(SelectSubQuery subQuery, SelectQueryBlock queryBlock) {
    this.subQuery = subQuery;
    this.queryBlock = queryBlock;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  public List<SelectListElement> getSelectedFields() {
    if (subQuery != null) {
      return subQuery.getSelectedFields();
    }
    if (queryBlock != null) {
      return queryBlock.getSelectedField();
    }
    return null;
  }

  public List<TableReference> getFromTables() {
    if (subQuery != null) {
      return subQuery.getFromTables();
    }
    if (queryBlock != null) {
      return queryBlock.getFromTables();
    }
    return null;
  }

  public String toPostgre(Everything data) {
    return toPostgre(data, null);
  }

  public String toPostgre(Everything data, String schemaContext) {
    if (subQuery != null) {
      return subQuery.toPostgre(data, schemaContext);
    }
    if (queryBlock != null) {
      return queryBlock.toPostgre(data, schemaContext);
    }
    return ""; //TODO should be either OR enforced?!
  }
}
