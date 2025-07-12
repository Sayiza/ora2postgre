package me.christianrobert.ora2postgre.writing;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.ViewMetadata;
import me.christianrobert.ora2postgre.global.StringAux;
import me.christianrobert.ora2postgre.global.ViewSpecAndQuery;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.ViewTransformationManager;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

public class ExportView {

  private static final ViewTransformationManager viewManager = new ViewTransformationManager();

  public static void saveEmptyViews(String path, List<ViewMetadata> views) {
    for (ViewMetadata v : views) {
      FileWriter.write(
              Paths.get(path +
                      File.separator +
                      v.getSchema().toLowerCase() +
                      File.separator +
                      "step1viewspec"),
              StringAux.capitalizeFirst(v.getViewName()) + "VIEW.sql",
              viewManager.transformViewMetadata(v, true, null)
      );
    }
  }

  public static void saveFullViews(String path, List<ViewSpecAndQuery> views, Everything data) {
    for (ViewSpecAndQuery v : views) {
      FileWriter.write(
              Paths.get(path + File.separator + v.spec.getSchema().toLowerCase() +
                      File.separator +
                      "step4viewbody"),
              StringAux.capitalizeFirst(v.spec.getViewName()) + "VIEW.sql",
              viewManager.transformViewMetadata(v.spec, false, data) +
                      "\n" +
                      v.query.toPostgre(data) +
                      // The view Manager only deals with the metadata part, the
                      // query is a "simple ast" element, and does not follow the manager-strategy pattern
                      //viewManager.transformSelectStatement(v.query, data, v.spec.getSchema()) +
                      "\n;\n"
      );
    }
  }
}
