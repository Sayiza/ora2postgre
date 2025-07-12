package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.StringAux;
import java.util.List;

public class SelectStatement extends Statement {

  String schema;
  SelectSubQuery subQuery;
  SelectWithClause withClause;
  SelectForUpdateClause forUpdateClause;
  SelectOrderByClause orderByClause;
  SelectOffsetClause offsetClause;
  SelectFetchClause fetchIntoClause;

  // TODO where clause, other statement variations, like union and with clause!
  // TODO a lot of work ... what about connect by?!


  public SelectStatement(
          String schema,
          SelectSubQuery subQuery,
          SelectWithClause withClause,
          SelectForUpdateClause forUpdateClause,
          SelectOrderByClause orderByClause,
          SelectOffsetClause offsetClause,
          SelectFetchClause fetchIntoClause) {
    this.schema = schema;
    this.subQuery = subQuery;
    this.withClause = withClause;
    this.forUpdateClause = forUpdateClause;
    this.orderByClause = orderByClause;
    this.offsetClause = offsetClause;
    this.fetchIntoClause = fetchIntoClause;
  }

  private List<SelectListElement> getSelectedFields() {
    return subQuery.getSelectedFields();
  }

  private List<TableReference> getFromTables() {
    return subQuery.getFromTables();
  }

  public String getSchema() {
    return schema;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Select Statement {code=" + "}";
  }

  //public String toPostgre(Everything data) {
  //  return viewManager.transformSelectStatement(this, data);
  //}

  /**
   * @deprecated Use ViewTransformationManager.transformSelectStatement() instead
   */
  //@Deprecated
  //public String toPostgre(Everything data, String schemaContext) {
  //  return viewManager.transformSelectStatement(this, data, schemaContext);
  //}

  /**
   * @deprecated Use ViewTransformationManager.transformSelectStatement() instead
   */
  //@Deprecated
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    b.append(subQuery.toPostgre(data, schema))
            //.append("\n")
            //.append(orderByClause.toJava()) TODO later
            //.append("\n")
            // TODO .append(forUpdateClause.toJava()
            ;
    return b.toString();
  }

  private String getJavaClassName() {

    StringBuilder b = new StringBuilder();
    for (SelectListElement s : getSelectedFields()) {
      b.append(StringAux.capitalizeFirst(s.getReferenceName())); //TODO better way of anon Entities?!
    }
    return b.toString(); //TODO
  }

  public String getJavaClassNameCursorDto() {
    return getJavaClassName() + "CursorDto";
  }

  public String getJavaClassNameResultMapping() {
    return getJavaClassName() + "ResultMappingEntity";
  }

  // toJavaCursorDto() method removed - cursor DTOs not needed for PostgreSQL-first approach

  // toJavaResultMapping() method removed - JPA result mappings not needed for PostgreSQL-first approach
}
