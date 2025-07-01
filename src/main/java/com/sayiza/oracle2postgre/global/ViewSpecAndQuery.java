package com.sayiza.oracle2postgre.global;

import com.sayiza.oracle2postgre.oracledb.ViewMetadata;
import com.sayiza.oracle2postgre.plsql.ast.SelectStatement;

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
