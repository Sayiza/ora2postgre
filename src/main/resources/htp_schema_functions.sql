-- PostgreSQL implementation of Oracle HTP (Hypertext Procedures) package
-- This provides equivalent functionality to Oracle's HTP package for generating HTML output

CREATE SCHEMA IF NOT EXISTS SYS
;

-- Initialize HTP buffer - equivalent to Oracle's HTP.init
CREATE OR REPLACE PROCEDURE SYS.HTP_init()
AS $$
BEGIN
    DROP TABLE IF EXISTS temp_htp_buffer;
    CREATE TEMP TABLE temp_htp_buffer (
        line_no SERIAL,
        content TEXT
    );
END;
$$ LANGUAGE plpgsql
;

-- Print content to HTP buffer - equivalent to Oracle's HTP.p
CREATE OR REPLACE PROCEDURE SYS.HTP_p(content TEXT)
AS $$
BEGIN
    INSERT INTO temp_htp_buffer (content) VALUES (content);
END;
$$ LANGUAGE plpgsql
;

-- Get complete HTML page from buffer - equivalent to Oracle's HTP.get_page
CREATE OR REPLACE FUNCTION SYS.HTP_page()
RETURNS TEXT AS $$
DECLARE
    html_output TEXT := '';
BEGIN
    SELECT string_agg(content, chr(10) ORDER BY line_no)
    INTO html_output
    FROM temp_htp_buffer;
   
    RETURN COALESCE(html_output, '');
END;
$$ LANGUAGE plpgsql;

-- Additional HTP functions for better Oracle compatibility

-- Print line with newline - equivalent to Oracle's HTP.prn
CREATE OR REPLACE PROCEDURE SYS.HTP_prn(content TEXT)
AS $$
BEGIN
    INSERT INTO temp_htp_buffer (content) VALUES (content || chr(10));
END;
$$ LANGUAGE plpgsql
;

-- Print without newline (alias for HTP_p) - equivalent to Oracle's HTP.print
CREATE OR REPLACE PROCEDURE SYS.HTP_print(content TEXT)
AS $$
BEGIN
    CALL SYS.HTP_p(content);
END;
$$ LANGUAGE plpgsql
;

-- Clear the HTP buffer - equivalent to Oracle's HTP.flush
CREATE OR REPLACE PROCEDURE SYS.HTP_flush()
AS $$
BEGIN
    DELETE FROM temp_htp_buffer;
END;
$$ LANGUAGE plpgsql
;

-- Get buffer size
CREATE OR REPLACE FUNCTION SYS.HTP_buffer_size()
RETURNS INTEGER AS $$
BEGIN
    RETURN (SELECT COUNT(*) FROM temp_htp_buffer);
END;
$$ LANGUAGE plpgsql
;

-- HTML utility functions for common HTML generation

-- Generate HTML tag with content
CREATE OR REPLACE PROCEDURE SYS.HTP_tag(tag_name TEXT, content TEXT DEFAULT '', attributes TEXT DEFAULT '')
AS $$
BEGIN
    IF attributes IS NOT NULL AND attributes != '' THEN
        CALL SYS.HTP_p('<' || tag_name || ' ' || attributes || '>');
    ELSE
        CALL SYS.HTP_p('<' || tag_name || '>');
    END IF;
    
    IF content IS NOT NULL AND content != '' THEN
        CALL SYS.HTP_p(content);
    END IF;
    
    CALL SYS.HTP_p('</' || tag_name || '>');
END;
$$ LANGUAGE plpgsql
;

-- Generate HTML header
CREATE OR REPLACE PROCEDURE SYS.HTP_htmlOpen(title TEXT DEFAULT 'Generated Page')
AS $$
BEGIN
    CALL SYS.HTP_p('<!DOCTYPE html>');
    CALL SYS.HTP_p('<html>');
    CALL SYS.HTP_p('<head>');
    CALL SYS.HTP_p('<title>' || COALESCE(title, 'Generated Page') || '</title>');
    CALL SYS.HTP_p('</head>');
    CALL SYS.HTP_p('<body>');
END;
$$ LANGUAGE plpgsql
;

-- Close HTML document
CREATE OR REPLACE PROCEDURE SYS.HTP_htmlClose()
AS $$
BEGIN
    CALL SYS.HTP_p('</body>');
    CALL SYS.HTP_p('</html>');
END;
$$ LANGUAGE plpgsql
;

-- Example usage (commented out):
/*
-- Example of using HTP functions
DO $$
BEGIN
    -- Initialize the buffer
    CALL SYS.HTP_init();
    
    -- Generate HTML content
    CALL SYS.HTP_htmlOpen('My Page');
    CALL SYS.HTP_tag('h1', 'Welcome to My Page');
    CALL SYS.HTP_tag('p', 'This is generated content.');
    CALL SYS.HTP_htmlClose();
    
    -- Get the complete page
    RAISE NOTICE 'Generated HTML: %', SYS.HTP_page();
END;
$$;
*/