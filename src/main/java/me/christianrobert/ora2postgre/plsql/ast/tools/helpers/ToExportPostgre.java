package me.christianrobert.ora2postgre.plsql.ast.tools.helpers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;
import me.christianrobert.ora2postgre.plsql.ast.Function;

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

  /**
   * Enhanced parameter processing with function context for collection type resolution.
   * This enables function parameters to use function-local collection types.
   */
  public static void doParametersPostgre(StringBuilder b, List<Parameter> parameters, Everything data, Function function) {
    for (int i = 0; i < parameters.size(); i++) {
      Parameter p = parameters.get(i);
      b.append("  ")
              .append(p.toPostgre(data, function));
      if (i < parameters.size() - 1) {
        b.append(", ");
      }
    }
  }
}
