package com.sayiza.oracle2postgre.writing;

import com.sayiza.oracle2postgre.global.Everything;
import com.sayiza.oracle2postgre.global.StringAux;
import com.sayiza.oracle2postgre.plsql.ast.ObjectType;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ExportObjectType {
  // save2Java method disabled - no longer generating Java classes from ObjectTypes  
  public static void save2Java(String path, String javaPackageName, List<ObjectType> specs, List<ObjectType> bodies, Everything data) {
    // Method body removed - no longer generating Java classes from ObjectTypes
    // throw new UnsupportedOperationException("Java class generation from ObjectTypes removed - using PostgreSQL-first approach");
    // Silently ignore for now to avoid breaking existing calls
  }

  public static void saveObjectTypeSpecToPostgre(String path, List<ObjectType> specs, List<ObjectType> bodies, Everything data) {
    for (ObjectType o : mergeObjectSpecAndBody(specs, bodies)) {
      String fullPathAsString = path +
              File.separator +
              o.getSchema().toLowerCase() +
              File.separator +
              "step2objecttypespec";
      //TODO varray...
      String postgreType = o.toPostgreType(data);
      if (postgreType != null
              && !postgreType.startsWith("CREATE DOMAIN")
              && !postgreType.contains("data type not implemented")
      ) {
        FileWriter.write(Paths.get(fullPathAsString), o.getName() + "OBJECTTYPESPEC.sql", postgreType);
      }
    }
  }

  public static void saveObjectTypeBodyToPostgre(String path, List<ObjectType> specs, List<ObjectType> bodies, Everything data) {
    for (ObjectType o : mergeObjectSpecAndBody(specs, bodies)) {
      String fullPathAsString = path +
              File.separator +
              o.getSchema().toLowerCase() +
              File.separator +
              "step5objecttypebody";
      //TODO name
      String postgreFunctions = o.toPostgreFunctions(data, false);
      if (postgreFunctions != null
              && !postgreFunctions.startsWith("CREATE DOMAIN")
              && !postgreFunctions.contains("data type not implemented")) {
        FileWriter.write(Paths.get(fullPathAsString), o.getName() + ".sql", postgreFunctions);

      }
    }
  }

  private static List<ObjectType> mergeObjectSpecAndBody(List<ObjectType> specs, List<ObjectType> bodies) {
    List<ObjectType> newO = new ArrayList<>();
    for (ObjectType spec : specs) {
      boolean found = false;
      for (ObjectType body : bodies) {
        if (spec.getName().equals(body.getName()) && spec.getSchema().equals(body.getSchema())) {
          found = true;
          newO.add(
                  new ObjectType(
                          spec.getName(),
                          spec.getSchema(),
                          spec.getVariables(),
                          body.getFunctions(),
                          body.getProcedures(),
                          body.getConstuctors(),
                          spec.getVarray(),
                          spec.getNestedTable()
                  ));
          // TODO fine tune this?!
        }

      }
      if (!found) {
        newO.add(spec);
      }
    }
    return newO;
  }
}
