package me.christianrobert.ora2postgre.plsql.ast.tools.helpers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;

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
