package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

public class SelectSubQuery extends PlSqlAst {

  private SelectSubQueryBasicElement subQueryBasicElement;
  private List<SelectSubQueryBasicElement> unionList;
  private List<SelectSubQueryBasicElement> unionAllList;
  private List<SelectSubQueryBasicElement> minusList;
  private List<SelectSubQueryBasicElement> intersectList;

  public SelectSubQuery(SelectSubQueryBasicElement subQueryBasicElement, List<SelectSubQueryBasicElement> unionList, List<SelectSubQueryBasicElement> unionAllList, List<SelectSubQueryBasicElement> minusList, List<SelectSubQueryBasicElement> intersectList) {
    this.subQueryBasicElement = subQueryBasicElement;
    this.unionList = unionList;
    this.unionAllList = unionAllList;
    this.minusList = minusList;
    this.intersectList = intersectList;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  public List<SelectListElement> getSelectedFields() {
    return subQueryBasicElement.getSelectedFields();
  }

  public List<TableReference> getFromTables() {
    return subQueryBasicElement.getFromTables();
  }

  public String toPostgre(Everything data, String schemaContext) {
    return subQueryBasicElement.toPostgre(data, schemaContext);
    //TODO join the other set operators!
  }
}
