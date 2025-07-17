package me.christianrobert.ora2postgre.writing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportProjectPostgre {
  
  private static final Logger log = LoggerFactory.getLogger(ExportProjectPostgre.class);
  
  public static void save(String path) {
    String content = loadHtpSchemaFromResource();
    FileWriter.write(
            Paths.get(path),
            "HTPSCHEMA.sql",
            content
    );
  }
  
  /**
   * Loads HTP schema functions from the resource file.
   * 
   * @return The content of the HTP schema SQL file
   */
  private static String loadHtpSchemaFromResource() {
    try (InputStream inputStream = ExportProjectPostgre.class.getClassLoader()
            .getResourceAsStream("htp_schema_functions.sql")) {
      
      if (inputStream == null) {
        log.error("Could not find htp_schema_functions.sql resource file");
        throw new IllegalStateException("Could not find htp_schema_functions.sql resource file");
      }
      
      String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      log.debug("Successfully loaded HTP schema from resource file ({} characters)", content.length());
      return content;
      
    } catch (IOException e) {
      log.error("Error reading htp_schema_functions.sql resource file: {}", e.getMessage());
      throw new IllegalStateException("Could not find htp_schema_functions.sql resource file (2)");
    }
  }

}
