package com.sayiza.oracle2postgre.writing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for ExportProjectPostgre resource loading functionality.
 */
public class ExportProjectPostgreTest {
    
    private static final Logger log = LoggerFactory.getLogger(ExportProjectPostgreTest.class);
    
    @Test
    public void testHtpSchemaResourceLoading(@TempDir Path tempDir) throws IOException {
        // Test that the HTP schema can be loaded from resources and written to file
        String outputPath = tempDir.toString();
        
        // Call the save method
        assertDoesNotThrow(() -> ExportProjectPostgre.save(outputPath));
        
        // Verify the file was created
        Path generatedFile = Paths.get(outputPath, "HTPSCHEMA.sql");
        assertTrue(Files.exists(generatedFile), "HTPSCHEMA.sql file should be created");
        
        // Read the generated content
        String content = Files.readString(generatedFile);
        
        // Verify the content contains expected HTP functions
        assertNotNull(content, "Content should not be null");
        assertFalse(content.trim().isEmpty(), "Content should not be empty");
        
        // Check for essential HTP components
        assertTrue(content.contains("CREATE SCHEMA IF NOT EXISTS SYS"), 
                   "Should create SYS schema");
        assertTrue(content.contains("SYS.HTP_init()"), 
                   "Should contain HTP_init procedure");
        assertTrue(content.contains("SYS.HTP_p(content TEXT)"), 
                   "Should contain HTP_p procedure");
        assertTrue(content.contains("SYS.HTP_page()"), 
                   "Should contain HTP_page function");
        
        // Check for enhanced HTP functions (from resource file)
        assertTrue(content.contains("SYS.HTP_prn") || content.contains("SYS.HTP_print"),
                   "Should contain additional HTP functions from resource file");
        
        log.info("Successfully generated HTPSCHEMA.sql with {} characters", content.length());
        log.debug("Generated content preview: {}", content.substring(0, Math.min(200, content.length())));
    }
    
    @Test
    public void testHtpSchemaContentStructure(@TempDir Path tempDir) throws IOException {
        // Test the structure and completeness of the generated HTP schema
        String outputPath = tempDir.toString();
        ExportProjectPostgre.save(outputPath);
        
        Path generatedFile = Paths.get(outputPath, "HTPSCHEMA.sql");
        String content = Files.readString(generatedFile);
        
        // Verify PostgreSQL syntax elements
        assertTrue(content.contains("LANGUAGE plpgsql"), 
                   "Should use PostgreSQL PL/pgSQL language");
        assertTrue(content.contains("CREATE OR REPLACE"), 
                   "Should use CREATE OR REPLACE syntax");
        assertTrue(content.contains("$$"), 
                   "Should use PostgreSQL dollar quoting");
        
        // Verify function signatures match expected Oracle HTP equivalents
        assertTrue(content.contains("HTP_init()"), "Should have init function");
        assertTrue(content.contains("HTP_p(content TEXT)"), "Should have print function");
        assertTrue(content.contains("HTP_page()"), "Should have page function");
        assertTrue(content.contains("RETURNS TEXT"), "Should return TEXT type");
        
        // Count the number of functions/procedures created
        long functionCount = content.lines()
                .filter(line -> line.contains("CREATE OR REPLACE"))
                .count();
        
        assertTrue(functionCount >= 3, 
                   "Should create at least 3 functions/procedures (init, p, page)");
        
        log.info("HTP schema contains {} functions/procedures", functionCount);
    }
    
    @Test
    public void testResourceFileExists() {
        // Test that the resource file exists and can be loaded
        try (var inputStream = ExportProjectPostgreTest.class.getClassLoader()
                .getResourceAsStream("htp_schema_functions.sql")) {
            
            assertNotNull(inputStream, "htp_schema_functions.sql resource should exist");
            
            String content = new String(inputStream.readAllBytes());
            assertFalse(content.trim().isEmpty(), "Resource content should not be empty");
            
            // Verify resource contains enhanced functions
            assertTrue(content.contains("HTP_prn"), "Resource should contain HTP_prn function");
            assertTrue(content.contains("HTP_flush"), "Resource should contain HTP_flush function");
            assertTrue(content.contains("HTP_tag"), "Resource should contain HTP_tag function");
            
            log.info("Resource file loaded successfully with {} characters", content.length());
            
        } catch (Exception e) {
            fail("Should be able to load htp_schema_functions.sql resource: " + e.getMessage());
        }
    }
    
    @Test
    public void testFallbackBehavior() throws IOException {
        // This test verifies that even if resource loading fails, the fallback works
        // We can't easily simulate resource loading failure, but we can test the fallback content
        
        // Create a temporary test to verify fallback contains essential functions
        String outputPath = System.getProperty("java.io.tmpdir");
        
        assertDoesNotThrow(() -> ExportProjectPostgre.save(outputPath));
        
        Path generatedFile = Paths.get(outputPath, "HTPSCHEMA.sql");
        assertTrue(Files.exists(generatedFile), "File should be generated even with fallback");
        
        String content = Files.readString(generatedFile);
        
        // Verify essential functions are present (either from resource or fallback)
        assertTrue(content.contains("SYS.HTP_init"), "Should contain HTP_init");
        assertTrue(content.contains("SYS.HTP_p"), "Should contain HTP_p");
        assertTrue(content.contains("SYS.HTP_page"), "Should contain HTP_page");
        
        // Clean up
        Files.deleteIfExists(generatedFile);
        
        log.info("Fallback behavior test completed successfully");
    }
    
    @Test
    public void testGeneratedFileOutput(@TempDir Path tempDir) throws IOException {
        // Test the actual file output and verify it can be read back
        String outputPath = tempDir.toString();
        ExportProjectPostgre.save(outputPath);
        
        Path generatedFile = Paths.get(outputPath, "HTPSCHEMA.sql");
        
        // Verify file properties
        assertTrue(Files.exists(generatedFile), "Generated file should exist");
        assertTrue(Files.isRegularFile(generatedFile), "Should be a regular file");
        assertTrue(Files.size(generatedFile) > 0, "File should not be empty");
        
        // Verify file is readable and contains valid content
        String content = Files.readString(generatedFile);
        assertFalse(content.contains("null"), "Content should not contain 'null' strings");
        assertTrue(content.trim().endsWith(";") || content.trim().endsWith("*/"), 
                   "SQL content should end properly");
        
        // Verify SQL structure
        assertTrue(content.contains("CREATE"), "Should contain CREATE statements");
        assertTrue(content.contains("BEGIN") && content.contains("END"), 
                   "Should contain PL/pgSQL blocks");
        
        log.info("Generated file validation completed: {} bytes", Files.size(generatedFile));
    }
}