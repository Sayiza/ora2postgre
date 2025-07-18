package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

public class SelectQueryBlock extends PlSqlAst {
  private String schema;
  List<SelectListElement> selectedFields;
  List<TableReference> fromTables;
  WhereClause whereClause;
  // TODO other clauses like GROUP BY, HAVING, ORDER BY, etc.

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  public SelectQueryBlock(String schema, List<SelectListElement> selectedFields, List<TableReference> fromTables, WhereClause whereClause) {
    this.schema = schema;
    this.selectedFields = selectedFields;
    this.fromTables = fromTables;
    this.whereClause = whereClause;
  }

  public List<SelectListElement> getSelectedField() {
    return selectedFields;
  }

  public List<TableReference> getFromTables() {
    return fromTables;
  }

  public WhereClause getWhereClause() {
    return whereClause;
  }

  // there is no toJava function here ... the java code will use the postgre sql
  // for to get the data with native queries

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    b.append("SELECT ");
    for (int i = 0; i < selectedFields.size(); i++) {
      SelectListElement selectedField = selectedFields.get(i);
      if (schema != null) {
        b.append(selectedField.getProcessedNameWithSchema(data, schema, fromTables));
      } else {
        b.append(selectedField.getProcessedName(data));
      }
      if (selectedField.getColumnAlias() != null) {
        b.append(" AS ").append(selectedField.getColumnAlias());
      }
      if (i < selectedFields.size() - 1) {
        b.append(", ");
      }
    }
    b.append(" FROM ");
    for (int i = 0; i < fromTables.size(); i++) {
      TableReference tableReference = fromTables.get(i);
      b.append(tableReference.toPostgre(data));
      if (i < fromTables.size() - 1) {
        b.append(", \n");
      }
    }
    
    if (whereClause != null) {
      b.append("\n").append(whereClause.toPostgre(data));
    }
    
    return b.toString();
  }

  /**
   * @deprecated Use toPostgre(Everything data) instead - schema is now stored as member variable
   */
  @Deprecated
  public String toPostgre(Everything data, String schemaContext) {
    StringBuilder b = new StringBuilder();
    b.append("SELECT ");
    for (int i = 0; i < selectedFields.size(); i++) {
      SelectListElement selectedField = selectedFields.get(i);
      if (schemaContext != null) {
        b.append(selectedField.getProcessedNameWithSchema(data, schemaContext, fromTables));
      } else {
        b.append(selectedField.getProcessedName(data));
      }
      if (selectedField.getColumnAlias() != null) {
        b.append(" AS ").append(selectedField.getColumnAlias());
      }
      if (i < selectedFields.size() - 1) {
        b.append(", ");
      }
    }
    b.append(" FROM ");
    for (int i = 0; i < fromTables.size(); i++) {
      TableReference tableReference = fromTables.get(i);
      b.append(tableReference.toPostgre(data));
      if (i < fromTables.size() - 1) {
        b.append(", \n");
      }
    }
    
    if (whereClause != null) {
      b.append("\n").append(whereClause.toPostgre(data));
    }
    
    return b.toString();
  }
}
