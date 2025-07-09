package me.christianrobert.ora2postgre.oracledb;

import java.util.ArrayList;
import java.util.List;
import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils;
import me.christianrobert.ora2postgre.plsql.ast.tools.TypeConverter;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.ViewTransformationManager;

public class ViewMetadata {
  private static final ViewTransformationManager viewManager = new ViewTransformationManager();
  
  private String schema; // Oracle schema (user)
  private String viewName;
  private List<ColumnMetadata> columns;
  private String rawQuery; // Raw SQL query from all_views.text

  public ViewMetadata(String schema, String viewName) {
    this.schema = schema;
    this.viewName = viewName;
    this.columns = new ArrayList<>();
    this.rawQuery = "";
  }

  // Getters and setters
  public String getSchema() { return schema; }
  public String getViewName() { return viewName; }
  public List<ColumnMetadata> getColumns() { return columns; }
  public String getRawQuery() { return rawQuery; }
  public void setRawQuery(String rawQuery) { this.rawQuery = rawQuery; }

  public void addColumn(ColumnMetadata column) { columns.add(column); }

  @Override
  public String toString() {
    return "ViewMetadata{schema='" + schema + "', viewName='" + viewName + "', columns=" + columns.size() + ", rawQueryLength=" + rawQuery.length() + "}";
  }

  /**
   * Generates PostgreSQL-compatible CREATE OR REPLACE VIEW statements.
   * Uses a dummy query with NULLs for now, as the raw query will be processed later.
   *
   * @deprecated Use ViewTransformationManager.transformViewMetadata() instead
   * @return List of SQL statements (one per view)
   */
  @Deprecated
  public String toPostgre(boolean withDummyQuery) {
    return viewManager.transformViewMetadata(this, withDummyQuery, null);
  }

  /**
   * Generates PostgreSQL-compatible CREATE OR REPLACE VIEW statements.
   * Uses a dummy query with NULLs for now, as the raw query will be processed later.
   *
   * @param withDummyQuery Whether to include dummy NULL query for empty views
   * @param context The global context containing all migration data
   * @return List of SQL statements (one per view)
   */
  public String toPostgre(boolean withDummyQuery, Everything context) {
    return viewManager.transformViewMetadata(this, withDummyQuery, context);
  }

  /**
   * Generates PostgreSQL-compatible CREATE OR REPLACE VIEW statements.
   * Uses a dummy query with NULLs for now, as the raw query will be processed later.
   *
   * @return List of SQL statements (one per view)
   * @deprecated Use toPostgre(boolean, Everything) instead
   */
  @Deprecated
  public String toPostgreLegacy(boolean withDummyQuery) {
    StringBuilder createView = new StringBuilder("CREATE OR REPLACE VIEW ");
    createView.append(schema)
            .append(".")
            .append(PostgreSqlIdentifierUtils.quoteIdentifier(viewName))
            .append(" (");

    // Column list
    List<String> columnDefs = columns.stream()
            .map(col -> PostgreSqlIdentifierUtils.quoteIdentifier(col.getColumnName()))
            .toList();
    createView.append(String.join(", ", columnDefs));
    createView.append(") AS\n");

    // Dummy query with NULLs
    if (withDummyQuery) {
      List<String> nullColumns = columns.stream()
              .map(col -> "NULL::" + TypeConverter.toPostgre(col.getDataType()) + " AS " + PostgreSqlIdentifierUtils.quoteIdentifier(col.getColumnName()))
              .toList();
      createView.append("SELECT ")
              .append(String.join(", ", nullColumns))
              .append("\n;");
    }
    // else: needs to be done in the Export file, as we do not have the processed data in this file
    return createView.toString();
  }

}