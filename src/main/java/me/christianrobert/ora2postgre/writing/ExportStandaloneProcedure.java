package me.christianrobert.ora2postgre.writing;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.Procedure;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.ProcedureTransformationManager;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

public class ExportStandaloneProcedure {
  
  private static final ProcedureTransformationManager procedureManager = new ProcedureTransformationManager();

  /**
   * Exports standalone procedures to PostgreSQL SQL files.
   * Files are saved to step3bprocedures/ directory.
   */
  public static void saveStandaloneProceduresToPostgre(String path, List<Procedure> procedures, Everything data) {
    for (Procedure procedure : procedures) {
      if (!procedure.isStandalone()) {
        continue; // Safety check - only process standalone procedures
      }
      
      String fullPathAsString = path + 
              File.separator + 
              procedure.getSchema().toLowerCase() + 
              File.separator + 
              "step3bprocedures";
      
      String fileName = procedure.getName().toLowerCase() + ".sql";
      String transformedContent = procedureManager.transform(procedure, data, false);
      
      FileWriter.write(Paths.get(fullPathAsString), fileName, transformedContent);
    }
  }
}