package com.sayiza.oracle2postgre.writing;

import com.sayiza.oracle2postgre.global.Everything;
import com.sayiza.oracle2postgre.oracledb.ViewMetadata;
import com.sayiza.oracle2postgre.global.StringAux;
import com.sayiza.oracle2postgre.global.ViewSpecAndQuery;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

public class ExportView {

  public static void saveEmptyViews(String path, List<ViewMetadata> views) {
    for (ViewMetadata v : views) {
      FileWriter.write(
              Paths.get(path +
                      File.separator +
                      v.getSchema().toLowerCase() +
                      File.separator +
                      "step1viewspec"),
              StringAux.capitalizeFirst(v.getViewName()) + "VIEW.sql",
              v.toPostgre(true)
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
              v.spec.toPostgre(false) +
                      "\n" + v.query.toPostgre(data, v.spec.getSchema()) + "\n;\n"
      );
    }
  }
}
