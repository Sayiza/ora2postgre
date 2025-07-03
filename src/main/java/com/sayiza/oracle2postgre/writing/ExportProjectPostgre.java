package com.sayiza.oracle2postgre.writing;

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
        return getFallbackHtpSchema();
      }
      
      String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      log.debug("Successfully loaded HTP schema from resource file ({} characters)", content.length());
      return content;
      
    } catch (IOException e) {
      log.error("Error reading htp_schema_functions.sql resource file: {}", e.getMessage());
      return getFallbackHtpSchema();
    }
  }
  
  /**
   * Provides fallback HTP schema in case the resource file cannot be loaded.
   * 
   * @return Basic HTP schema implementation
   */
  private static String getFallbackHtpSchema() {
    log.warn("Using fallback HTP schema implementation");
    return """
CREATE SCHEMA IF NOT EXISTS SYS
;

CREATE OR REPLACE PROCEDURE SYS.HTP_init()
AS $$
BEGIN
    DROP TABLE IF EXISTS SYS.temp_htp_buffer;
    CREATE TEMP TABLE SYS.temp_htp_buffer (
        line_no SERIAL,
        content TEXT
    );
END;
$$ LANGUAGE plpgsql
;

CREATE OR REPLACE PROCEDURE SYS.HTP_p(content TEXT)
AS $$
BEGIN
    INSERT INTO SYS.temp_htp_buffer (content) VALUES (content);
END;
$$ LANGUAGE plpgsql
;

CREATE OR REPLACE FUNCTION SYS.HTP_page()
RETURNS TEXT AS $$
DECLARE
    html_output TEXT := '';
BEGIN
    SELECT string_agg(content, chr(10) ORDER BY line_no)
    INTO html_output
    FROM SYS.temp_htp_buffer;
   
    RETURN COALESCE(html_output, '');
END;
$$ LANGUAGE plpgsql;
            """;
  }
}
