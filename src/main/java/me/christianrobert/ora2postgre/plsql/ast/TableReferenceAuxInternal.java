package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.ArrayList;
import java.util.List;

public class TableReferenceAuxInternal extends PlSqlAst {

  private String schema;

  private TableExpressionClause tableExpressionClause;
  private PivotClause pivotClause;
  private UnPivotClause unpivotClause;
  private TableReference innerTableReference;
  private List<SelectSubQueryBasicElement> innerTableUnionList = new ArrayList<>();
  private List<SelectSubQueryBasicElement> innerTableUnionAllList = new ArrayList<>();
  private List<SelectSubQueryBasicElement> innerTableMinusList = new ArrayList<>();
  private List<SelectSubQueryBasicElement> innerTableIntersectList = new ArrayList<>();

  private boolean hasAnOnlyPart = false;

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  public TableReferenceAuxInternal(String schema, TableExpressionClause tableExpressionClause, PivotClause pivotClause, UnPivotClause unpivotClause, boolean hasAnOnlyPart) {
    this.schema = schema;
    this.tableExpressionClause = tableExpressionClause;
    this.pivotClause = pivotClause;
    this.unpivotClause = unpivotClause;
    this.hasAnOnlyPart = hasAnOnlyPart;
  }

  public TableReferenceAuxInternal(String schema, TableReference innerTableReference, List<SelectSubQueryBasicElement> innerTableUnionList, List<SelectSubQueryBasicElement> innerTableUnionAllList, List<SelectSubQueryBasicElement> innerTableMinusList, List<SelectSubQueryBasicElement> innerTableIntersectList) {
    this.schema = schema;
    this.innerTableReference = innerTableReference;
    this.innerTableUnionList = innerTableUnionList;
    this.innerTableUnionAllList = innerTableUnionAllList;
    this.innerTableMinusList = innerTableMinusList;
    this.innerTableIntersectList = innerTableIntersectList;
  }

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    if (tableExpressionClause != null) {
      if (tableExpressionClause.getSchemaName() != null) {
        b.append(tableExpressionClause.getSchemaName());
      } else {
        b.append(data.lookupSchema4Field(
                tableExpressionClause.getTableName(), schema));
      }
      b.append(".")
              .append(tableExpressionClause.getTableName());
    }
    //
    //TODO other types of subqueries

    // We have the convention that all schema and all tables and all views are uppercase
    return b.toString().toUpperCase();
  }

  public String getSchemaName() {
    return tableExpressionClause.getSchemaName();
  }

  public String getTableName() {
    return tableExpressionClause.getTableName();
  }
}
