package com.sayiza.oracle2postgre.plsql.ast.tools;

import com.sayiza.oracle2postgre.global.Everything;
import com.sayiza.oracle2postgre.plsql.ast.Parameter;

import java.util.List;

public class ToExportPostgre {

  public static void doParametersPostgre(StringBuilder b, List<Parameter> parameters, Everything data) {
    for (int i = 0; i < parameters.size(); i++) {
      Parameter p = parameters.get(i);
      b.append("  ")
              .append(p.toPostgre(data));
      if (i < parameters.size() - 1) {
        b.append(", ");
      }
    }
  }
}
