package com.sayiza.oracle2postgre.writing;

import com.sayiza.oracle2postgre.global.Everything;
import com.sayiza.oracle2postgre.global.StringAux;
import com.sayiza.oracle2postgre.plsql.ast.OraclePackage;
import com.sayiza.oracle2postgre.plsql.ast.SubType;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ExportPackage {
  // save2Java method disabled - no longer generating Java classes from Oracle packages
  public static void save2Java(String path, String javaPackageName, List<OraclePackage> specs, List<OraclePackage> bodies, Everything data) {
    // Method body removed - no longer generating Java classes from Oracle packages
    // Silently ignore for now to avoid breaking existing calls
  }

  public static void savePackageSpecToPostgre(String path, List<OraclePackage> specs, List<OraclePackage> bodies, Everything data) {
    for (OraclePackage o : mergeSpecAndBody(specs, bodies)) {
      String fullPathAsString = path +
              File.separator +
              o.getSchema().toLowerCase() +
              File.separator +
              "step3packagespec";
      // TODO name
      FileWriter.write(Paths.get(fullPathAsString), o.getName() + ".sql", o.toPostgre(data, true));
    }
  }

  public static void savePackageBodyToPostgre(String path, List<OraclePackage> specs, List<OraclePackage> bodies, Everything data) {
    for (OraclePackage o : mergeSpecAndBody(specs, bodies)) {
      String fullPathAsString = path +
              File.separator +
              o.getSchema().toLowerCase() +
              File.separator +
              "step6packagebody";
      // TODO name
      FileWriter.write(Paths.get(fullPathAsString), o.getName() + ".sql", o.toPostgre(data, false));
    }
  }

  private static List<OraclePackage> mergeSpecAndBody(List<OraclePackage> specs, List<OraclePackage> bodies) {
    List<OraclePackage> newO = new ArrayList<>();
    for (OraclePackage spec : specs) {
      boolean found = false;
      for (OraclePackage body : bodies) {
        if (spec.getName().equals(body.getName()) && spec.getSchema().equals(body.getSchema())) {
          found = true;
          newO.add(
                  new OraclePackage(
                          spec.getName(),
                          spec.getSchema(),
                          spec.getVariables(),
                          mergeSubTypes(spec.getSubtypes(),body.getSubtypes()),
                          body.getCursors(), // TODO merge all private/public etc
                          body.getTypes(),
                          body.getFunctions(),
                          body.getProcedures(),
                          body.getBodyStatements()));
          // TODO fine tune this?!
        }

      }
      if (!found) {
        newO.add(spec);
      }
    }
    return newO;
  }

  private static List<SubType> mergeSubTypes(List<SubType> subtypes, List<SubType> subtypes1) {
    return null; //TODO
  }
}
