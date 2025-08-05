package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.SchemaResolutionUtils;

import java.util.List;

public class SelectListElement extends PlSqlAst {

  private Expression expression;
  private String schema;
  private String columnAlias;

  // or a single *
  private boolean isAsteriskForAllColumns;

  // or this, a.* , schema123.table123.*
  private String tableNameWithAsterisk;
  private String schemaNameWithAsterisk;
  // TODO subList with replacement fields for asterisk

  public SelectListElement(String schema, String columnAlias, Expression expression, boolean isAsteriskForAllColumns, String tableNameWithAsterisk, String schemaNameWithAsterisk) {
    this.expression = expression;
    this.schema = schema;
    this.columnAlias = columnAlias;
    this.isAsteriskForAllColumns = isAsteriskForAllColumns;
    this.tableNameWithAsterisk = tableNameWithAsterisk;
    this.schemaNameWithAsterisk = schemaNameWithAsterisk;
  }

  public Expression getExpression() {
    return expression;
  }

  public String getReferenceName() {
    //return expression.getNameForStatementExpression(); // TODO could be an alias!
    if ( columnAlias != null ) {
      return columnAlias;
    }
    return expression.toString();
  }

  public String getProcessedName(Everything data) {
    return expression.getNameForStatementExpressionWithSchema(data, schema, null);
    // TODO do the lookup in data, get schemaprefix  and correct case
    // the Class name for the package-like-class could start capitalized
    // the schema is changed to a java package and all lowercase
    // function names are also supposed to me made lower case
  }

  /**
   * Gets the processed name with schema prefix for PostgreSQL output.
   * This method provides schema context information needed for proper function resolution.
   */
  public String getProcessedNameWithSchema(Everything data, String schemaContext, List<TableReference> fromTables) {
    return expression.getNameForStatementExpressionWithSchema(data, schemaContext, fromTables);
  }

  public String getColumnAlias() {
    return columnAlias;
  }

  //public String getPrefix() {
  //  if (expression == null && expression.getRawText() != null && expression.getRawText().contains(".")) {
  //    String[] split = expression.getRawText().split("\\.");
  //    return split[0];
  //  }
  //  return null; // TODO
  //}

  public boolean isAsteriskForAllColumns() {
    return isAsteriskForAllColumns;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Selectlistelement{name=" + getReferenceName() + "}";
  }

  // toJava() method removed - select list elements don't need Java DTO generation

  /* may be not needed, the reference name and the processed name and the datatype will do?
  public String toPostgre(Everything data, List<TableReference> fromTables) {
    StringBuilder b = new StringBuilder();
    // if name is set, is is a normal column, treat asterisk differently
    b.append(getReferenceName())
            .append(" ")
            .append(TypeConverter.toPostgre(
                    SchemaResolutionUtils.lookUpDataType(data, expression, schema, fromTables)))
    ;
    return b.toString();
  } */
}