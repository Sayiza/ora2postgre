package com.sayiza.oracle2postgre.writing;

import java.nio.file.Paths;

public class ExportProjectPostgre {
  public static void save(String path) {
    String content = """
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
   \s
    RETURN COALESCE(html_output, '');
END;
$$ LANGUAGE plpgsql;
            """;
    FileWriter.write(
            Paths.get(path),
            "HTPSCHEMA.sql",
            content
    );
  }
}
