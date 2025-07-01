package com.sayiza.oracle2postgre.writing;

import com.sayiza.oracle2postgre.global.Everything;
import com.sayiza.oracle2postgre.oracledb.TableMetadata;
import com.sayiza.oracle2postgre.global.StringAux;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

public class ExportTable {
  public static void save2Java(String path, String javaPackageName, List<TableMetadata> tables) {
    for (TableMetadata t : tables) {
      String fullPathAsString = path +
              File.separator +
              javaPackageName.replace('.', File.separatorChar) +
              File.separator +
              t.getSchema().toLowerCase();
      String leName = StringAux.capitalizeFirst(t.getTableName()) + ".java";
      // TODO make entity
      FileWriter.write(Paths.get(fullPathAsString), leName, t.toJava(javaPackageName));
    }
  }

  public static void saveSql(String path, List<TableMetadata> tables, Everything data) {
    for (TableMetadata t : tables) {
      FileWriter.write(
              Paths.get(path + File.separator + t.getSchema().toLowerCase()),
              StringAux.capitalizeFirst(t.getTableName()) + "TABLE.sql",
              String.join("\n",t.toPostgre(data))
      );
    }
  }
}
