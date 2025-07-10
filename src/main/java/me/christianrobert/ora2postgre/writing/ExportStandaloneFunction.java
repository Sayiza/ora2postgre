package me.christianrobert.ora2postgre.writing;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.Function;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.FunctionTransformationManager;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

public class ExportStandaloneFunction {
  
  private static final FunctionTransformationManager functionManager = new FunctionTransformationManager();

  /**
   * Exports standalone functions to PostgreSQL SQL files.
   * Files are saved to step3afunctions/ directory.
   */
  public static void saveStandaloneFunctionsToPostgre(String path, List<Function> functions, Everything data) {
    for (Function function : functions) {
      if (!function.isStandalone()) {
        continue; // Safety check - only process standalone functions
      }
      
      String fullPathAsString = path + 
              File.separator + 
              function.getSchema().toLowerCase() + 
              File.separator + 
              "step3afunctions";
      
      String fileName = function.getName().toLowerCase() + ".sql";
      String transformedContent = functionManager.transform(function, data, false);
      
      FileWriter.write(Paths.get(fullPathAsString), fileName, transformedContent);
    }
  }
}