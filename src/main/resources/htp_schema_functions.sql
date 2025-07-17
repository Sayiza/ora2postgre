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

-- For NUMERIC (custom formatting, e.g., 2 decimal places)
CREATE OR REPLACE PROCEDURE SYS.HTP_p(content NUMERIC)
AS $$
BEGIN
INSERT INTO temp_htp_buffer (content) VALUES (TO_CHAR(content, 'FM999999999.99'));
END;
$$ LANGUAGE plpgsql;

-- For INTEGER
CREATE OR REPLACE PROCEDURE SYS.HTP_p(content INTEGER)
AS $$
BEGIN
INSERT INTO temp_htp_buffer (content) VALUES (content::TEXT);
END;
$$ LANGUAGE plpgsql;

-- For VARCHAR
CREATE OR REPLACE PROCEDURE SYS.HTP_p(content VARCHAR)
AS $$
BEGIN
INSERT INTO temp_htp_buffer (content) VALUES (content);
END;
$$ LANGUAGE plpgsql;

-- For DATE (custom formatting)
CREATE OR REPLACE PROCEDURE SYS.HTP_p(content DATE)
AS $$
BEGIN
INSERT INTO temp_htp_buffer (content) VALUES (TO_CHAR(content, 'YYYY-MM-DD'));
END;
$$ LANGUAGE plpgsql;

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

-- Package Variable Accessor Functions
-- These functions provide direct access to package variables stored in temporary tables
-- Following the same session-isolated pattern as HTP buffer functions

-- Read package variable (returns text, caller handles casting)
CREATE OR REPLACE FUNCTION SYS.get_package_var(
  package_name text, 
  var_name text
) RETURNS text LANGUAGE plpgsql AS $$
DECLARE
  table_name text;
  value text;
BEGIN
  -- Build table name using existing naming convention
  table_name := lower(current_schema()) || '_' || lower(package_name) || '_' || lower(var_name);
  
  -- Read from session temp table
  EXECUTE format('SELECT value FROM %I LIMIT 1', table_name) INTO value;
  
  RETURN value;
EXCEPTION
  WHEN undefined_table THEN
    -- Table doesn't exist, return NULL (will be handled by caller)
    RETURN NULL;
  WHEN others THEN
    -- Log error and return NULL for graceful degradation
    RAISE WARNING 'Error reading package variable %.%: %', package_name, var_name, SQLERRM;
    RETURN NULL;
END;
$$;

-- Write package variable (accepts text, caller handles casting)
CREATE OR REPLACE FUNCTION SYS.set_package_var(
  package_name text, 
  var_name text, 
  value text
) RETURNS void LANGUAGE plpgsql AS $$
DECLARE
  table_name text;
BEGIN
  -- Build table name using existing naming convention
  table_name := lower(current_schema()) || '_' || lower(package_name) || '_' || lower(var_name);
  
  -- Update session temp table
  EXECUTE format('UPDATE %I SET value = %L', table_name, value);
EXCEPTION
  WHEN undefined_table THEN
    -- Table doesn't exist, log warning for debugging
    RAISE WARNING 'Package variable table does not exist: %', table_name;
  WHEN others THEN
    -- Log error for debugging
    RAISE WARNING 'Error writing package variable %.%: %', package_name, var_name, SQLERRM;
END;
$$;

-- Type-safe wrapper functions for common data types

-- Numeric getter/setter
CREATE OR REPLACE FUNCTION SYS.get_package_var_numeric(package_name text, var_name text) 
RETURNS numeric LANGUAGE plpgsql AS $$
DECLARE
  value text;
BEGIN
  value := SYS.get_package_var(package_name, var_name);
  IF value IS NULL THEN
    RETURN NULL;
  END IF;
  RETURN value::numeric;
EXCEPTION
  WHEN invalid_text_representation THEN
    RAISE WARNING 'Invalid numeric value for package variable %.%: %', package_name, var_name, value;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION SYS.set_package_var_numeric(package_name text, var_name text, value numeric) 
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  PERFORM SYS.set_package_var(package_name, var_name, value::text);
END;
$$;

-- Boolean getter/setter
CREATE OR REPLACE FUNCTION SYS.get_package_var_boolean(package_name text, var_name text) 
RETURNS boolean LANGUAGE plpgsql AS $$
DECLARE
  value text;
BEGIN
  value := SYS.get_package_var(package_name, var_name);
  IF value IS NULL THEN
    RETURN NULL;
  END IF;
  RETURN value::boolean;
EXCEPTION
  WHEN invalid_text_representation THEN
    RAISE WARNING 'Invalid boolean value for package variable %.%: %', package_name, var_name, value;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION SYS.set_package_var_boolean(package_name text, var_name text, value boolean) 
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  PERFORM SYS.set_package_var(package_name, var_name, value::text);
END;
$$;

-- Text/VARCHAR2 getter/setter
CREATE OR REPLACE FUNCTION SYS.get_package_var_text(package_name text, var_name text) 
RETURNS text LANGUAGE plpgsql AS $$
BEGIN
  RETURN SYS.get_package_var(package_name, var_name);
END;
$$;

CREATE OR REPLACE FUNCTION SYS.set_package_var_text(package_name text, var_name text, value text) 
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  PERFORM SYS.set_package_var(package_name, var_name, value);
END;
$$;

-- Date/Timestamp getter/setter
CREATE OR REPLACE FUNCTION SYS.get_package_var_timestamp(package_name text, var_name text) 
RETURNS timestamp LANGUAGE plpgsql AS $$
DECLARE
  value text;
BEGIN
  value := SYS.get_package_var(package_name, var_name);
  IF value IS NULL THEN
    RETURN NULL;
  END IF;
  RETURN value::timestamp;
EXCEPTION
  WHEN invalid_text_representation THEN
    RAISE WARNING 'Invalid timestamp value for package variable %.%: %', package_name, var_name, value;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION SYS.set_package_var_timestamp(package_name text, var_name text, value timestamp) 
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  PERFORM SYS.set_package_var(package_name, var_name, value::text);
END;
$$;

-- Collection Accessor Functions
-- These functions provide direct access to package collection variables (VARRAY/TABLE OF)
-- Following the same session-isolated pattern as regular package variables

-- Get entire collection as PostgreSQL array
CREATE OR REPLACE FUNCTION SYS.get_package_collection(package_name text, var_name text) 
RETURNS text[] LANGUAGE plpgsql AS $$
DECLARE
  table_name text;
  result text[];
BEGIN
  table_name := lower(current_schema()) || '_' || lower(package_name) || '_' || lower(var_name);
  
  -- Reconstruct array from table rows (same as PackageCollectionHelper prologue)
  EXECUTE format('SELECT CASE WHEN COUNT(*) = 0 THEN ARRAY[]::text[]
                               ELSE array_agg(value ORDER BY row_number() OVER ())
                          END FROM %I', table_name) INTO result;
  
  RETURN result;
EXCEPTION
  WHEN undefined_table THEN
    RETURN ARRAY[]::text[];
  WHEN others THEN
    RAISE WARNING 'Error reading package collection %.%: %', package_name, var_name, SQLERRM;
    RETURN ARRAY[]::text[];
END;
$$;

-- Set entire collection from PostgreSQL array
CREATE OR REPLACE FUNCTION SYS.set_package_collection(package_name text, var_name text, value text[]) 
RETURNS void LANGUAGE plpgsql AS $$
DECLARE
  table_name text;
BEGIN
  table_name := lower(current_schema()) || '_' || lower(package_name) || '_' || lower(var_name);
  
  -- Clear and repopulate table (same as PackageCollectionHelper epilogue)
  EXECUTE format('DELETE FROM %I', table_name);
  EXECUTE format('INSERT INTO %I (value) SELECT unnest(%L)', table_name, value);
EXCEPTION
  WHEN undefined_table THEN
    RAISE WARNING 'Package collection table does not exist: %', table_name;
  WHEN others THEN
    RAISE WARNING 'Error writing package collection %.%: %', package_name, var_name, SQLERRM;
END;
$$;

-- Get collection element by index (1-based, Oracle-style)
CREATE OR REPLACE FUNCTION SYS.get_package_collection_element(package_name text, var_name text, index_pos integer) 
RETURNS text LANGUAGE plpgsql AS $$
DECLARE
  table_name text;
  result text;
BEGIN
  table_name := lower(current_schema()) || '_' || lower(package_name) || '_' || lower(var_name);
  
  -- Get element at specific position (1-based indexing)
  EXECUTE format('SELECT value FROM (SELECT value, row_number() OVER () as rn FROM %I) t 
                  WHERE rn = %L', table_name, index_pos) INTO result;
  
  RETURN result;
EXCEPTION
  WHEN undefined_table THEN
    RETURN NULL;
  WHEN others THEN
    RAISE WARNING 'Error reading package collection element %.%[%]: %', package_name, var_name, index_pos, SQLERRM;
    RETURN NULL;
END;
$$;

-- Set collection element by index (1-based, Oracle-style)
CREATE OR REPLACE FUNCTION SYS.set_package_collection_element(package_name text, var_name text, index_pos integer, value text) 
RETURNS void LANGUAGE plpgsql AS $$
DECLARE
  table_name text;
BEGIN
  table_name := lower(current_schema()) || '_' || lower(package_name) || '_' || lower(var_name);
  
  -- Update element at specific position using ctid for direct row access
  EXECUTE format('UPDATE %I SET value = %L WHERE ctid = (
                    SELECT ctid FROM (SELECT ctid, row_number() OVER () as rn FROM %I) t 
                    WHERE rn = %L
                  )', table_name, value, table_name, index_pos);
EXCEPTION
  WHEN undefined_table THEN
    RAISE WARNING 'Package collection table does not exist: %', table_name;
  WHEN others THEN
    RAISE WARNING 'Error writing package collection element %.%[%]: %', package_name, var_name, index_pos, SQLERRM;
END;
$$;

-- Collection COUNT method (Oracle arr.COUNT equivalent)
CREATE OR REPLACE FUNCTION SYS.get_package_collection_count(package_name text, var_name text) 
RETURNS integer LANGUAGE plpgsql AS $$
DECLARE
  table_name text;
  result integer;
BEGIN
  table_name := lower(current_schema()) || '_' || lower(package_name) || '_' || lower(var_name);
  
  EXECUTE format('SELECT COUNT(*) FROM %I', table_name) INTO result;
  
  RETURN result;
EXCEPTION
  WHEN undefined_table THEN
    RETURN 0;
  WHEN others THEN
    RAISE WARNING 'Error counting package collection %.%: %', package_name, var_name, SQLERRM;
    RETURN 0;
END;
$$;

-- Collection EXTEND method (Oracle arr.EXTEND equivalent)
CREATE OR REPLACE FUNCTION SYS.extend_package_collection(package_name text, var_name text, value text DEFAULT NULL) 
RETURNS void LANGUAGE plpgsql AS $$
DECLARE
  table_name text;
BEGIN
  table_name := lower(current_schema()) || '_' || lower(package_name) || '_' || lower(var_name);
  
  -- Add new element to end of collection
  EXECUTE format('INSERT INTO %I (value) VALUES (%L)', table_name, value);
EXCEPTION
  WHEN undefined_table THEN
    RAISE WARNING 'Package collection table does not exist: %', table_name;
  WHEN others THEN
    RAISE WARNING 'Error extending package collection %.%: %', package_name, var_name, SQLERRM;
END;
$$;

-- Collection FIRST method (Oracle arr.FIRST equivalent) - always returns 1
CREATE OR REPLACE FUNCTION SYS.get_package_collection_first(package_name text, var_name text) 
RETURNS integer LANGUAGE plpgsql AS $$
DECLARE
  table_name text;
  count_result integer;
BEGIN
  table_name := lower(current_schema()) || '_' || lower(package_name) || '_' || lower(var_name);
  
  EXECUTE format('SELECT COUNT(*) FROM %I', table_name) INTO count_result;
  
  -- Return 1 if collection has elements, NULL if empty
  IF count_result > 0 THEN
    RETURN 1;
  ELSE
    RETURN NULL;
  END IF;
EXCEPTION
  WHEN undefined_table THEN
    RETURN NULL;
END;
$$;

-- Collection LAST method (Oracle arr.LAST equivalent) - returns count
CREATE OR REPLACE FUNCTION SYS.get_package_collection_last(package_name text, var_name text) 
RETURNS integer LANGUAGE plpgsql AS $$
BEGIN
  RETURN SYS.get_package_collection_count(package_name, var_name);
END;
$$;

-- Type-safe collection wrappers for common data types

-- Numeric collection element getter/setter
CREATE OR REPLACE FUNCTION SYS.get_package_collection_element_numeric(package_name text, var_name text, index_pos integer) 
RETURNS numeric LANGUAGE plpgsql AS $$
DECLARE
  value text;
BEGIN
  value := SYS.get_package_collection_element(package_name, var_name, index_pos);
  IF value IS NULL THEN
    RETURN NULL;
  END IF;
  RETURN value::numeric;
EXCEPTION
  WHEN invalid_text_representation THEN
    RAISE WARNING 'Invalid numeric value for package collection element %.%[%]: %', package_name, var_name, index_pos, value;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION SYS.set_package_collection_element_numeric(package_name text, var_name text, index_pos integer, value numeric) 
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  PERFORM SYS.set_package_collection_element(package_name, var_name, index_pos, value::text);
END;
$$;

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

-- Example of using package variable functions
DO $$
BEGIN
    -- Example assumes package variable table exists
    -- CREATE TEMP TABLE test_schema_minitest_gx (value text DEFAULT '1');
    
    -- Read package variable
    RAISE NOTICE 'Package variable gX: %', SYS.get_package_var_numeric('minitest', 'gX');
    
    -- Write package variable
    PERFORM SYS.set_package_var_numeric('minitest', 'gX', 42);
    
    -- Read updated value
    RAISE NOTICE 'Updated package variable gX: %', SYS.get_package_var_numeric('minitest', 'gX');
END;
$$;

-- Example of using package collection functions
DO $$
BEGIN
    -- Example assumes package collection table exists
    -- CREATE TEMP TABLE test_schema_minitest_arr (value text);
    -- INSERT INTO test_schema_minitest_arr (value) VALUES ('1'), ('2'), ('3');
    
    -- Read collection element
    RAISE NOTICE 'Collection element arr[1]: %', SYS.get_package_collection_element_numeric('minitest', 'arr', 1);
    
    -- Write collection element
    PERFORM SYS.set_package_collection_element_numeric('minitest', 'arr', 1, 42);
    
    -- Read updated element
    RAISE NOTICE 'Updated collection element arr[1]: %', SYS.get_package_collection_element_numeric('minitest', 'arr', 1);
    
    -- Collection operations
    RAISE NOTICE 'Collection count: %', SYS.get_package_collection_count('minitest', 'arr');
    RAISE NOTICE 'Collection first: %', SYS.get_package_collection_first('minitest', 'arr');
    RAISE NOTICE 'Collection last: %', SYS.get_package_collection_last('minitest', 'arr');
    
    -- Extend collection
    PERFORM SYS.extend_package_collection('minitest', 'arr', '99');
    RAISE NOTICE 'Collection after extend: %', SYS.get_package_collection_count('minitest', 'arr');
END;
$$;
*/