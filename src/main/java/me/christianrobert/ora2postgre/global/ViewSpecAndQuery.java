package me.christianrobert.ora2postgre.global;

import me.christianrobert.ora2postgre.oracledb.ViewMetadata;
import me.christianrobert.ora2postgre.plsql.ast.SelectStatement;

public class ViewSpecAndQuery {
  public ViewMetadata spec;
  public SelectStatement query;

  public ViewSpecAndQuery(ViewMetadata spec, SelectStatement query) {
    this.spec = spec;
    this.query = query;
  }

  public ViewMetadata getSpec() {
    return spec;
  }
}
