package me.christianrobert.ora2postgre.transfer;

import me.christianrobert.ora2postgre.oracledb.ColumnMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for AQ$_SIG_PROP to JSONB conversion functionality.
 * 
 * Tests the AqSigPropConverter class and its integration with the migration pipeline,
 * covering Oracle Advanced Queuing signature property conversion to PostgreSQL JSONB format.
 */
public class AqSigPropConverterTest {
    
    private static final Logger log = LoggerFactory.getLogger(AqSigPropConverterTest.class);
    private ObjectMapper objectMapper;
    private ResultSet mockResultSet;
    private Struct mockStruct;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockResultSet = mock(ResultSet.class);
        mockStruct = mock(Struct.class);
    }
    
    @Test
    public void testNullSigPropConversion() throws SQLException {
        // Test NULL AQ$_SIG_PROP conversion
        when(mockResultSet.getObject("sig_prop_col")).thenReturn(null);
        when(mockResultSet.wasNull()).thenReturn(true);
        
        String result = AqSigPropConverter.convertToJson(mockResultSet, "sig_prop_col");
        
        assertNull(result, "NULL AQ$_SIG_PROP should return null JSON");
    }
    
    @Test
    public void testStructSigPropConversion() throws Exception {
        // Test Oracle STRUCT representing AQ$_SIG_PROP conversion
        Object[] attributes = {
            "SHA256",                           // algorithm
            "base64-encoded-digest-data",       // digest
            "base64-encoded-signature-data",    // signature
            "VALID",                           // validation status
            "CN=TestCA,O=TestOrg"              // signer info
        };
        
        when(mockStruct.getAttributes()).thenReturn(attributes);
        when(mockResultSet.getObject("sig_prop_col")).thenReturn(mockStruct);
        when(mockResultSet.wasNull()).thenReturn(false);
        
        String result = AqSigPropConverter.convertToJson(mockResultSet, "sig_prop_col");
        
        assertNotNull(result, "AQ$_SIG_PROP conversion should not return null");
        
        // Parse and validate JSON structure
        JsonNode jsonNode = objectMapper.readTree(result);
        
        assertEquals("AQ_SIG_PROP", jsonNode.get("signature_type").asText(), "Signature type should be AQ_SIG_PROP");
        
        // Test signature properties
        JsonNode sigProps = jsonNode.get("signature_properties");
        assertNotNull(sigProps, "Signature properties should be present");
        assertEquals("SHA256", sigProps.get("algorithm").asText(), "Algorithm should be extracted correctly");
        assertEquals("base64-encoded-digest-data", sigProps.get("digest").asText(), "Digest should be extracted correctly");
        assertEquals("base64-encoded-signature-data", sigProps.get("signature").asText(), "Signature should be extracted correctly");
        
        // Test metadata
        JsonNode metadata = jsonNode.get("metadata");
        assertNotNull(metadata, "Metadata should be present");
        assertEquals("VALID", metadata.get("validation_status").asText(), "Validation status should be extracted");
        assertEquals("CN=TestCA,O=TestOrg", metadata.get("signer").asText(), "Signer should be extracted");
        assertTrue(metadata.has("timestamp"), "Timestamp should be present");
        
        // Test technical information
        JsonNode techInfo = jsonNode.get("technical_info");
        assertNotNull(techInfo, "Technical info should be present");
        assertEquals("SYS.AQ$_SIG_PROP", techInfo.get("original_type").asText(), "Original type should be SYS.AQ$_SIG_PROP");
        assertEquals(5, techInfo.get("attributes_count").asInt(), "Attributes count should match");
        assertTrue(techInfo.has("conversion_timestamp"), "Conversion timestamp should be present");
        
        log.info("Struct AQ$_SIG_PROP conversion result: {}", result);
    }
    
    @Test
    public void testEmptyStructSigPropConversion() throws Exception {
        // Test Oracle STRUCT with no attributes
        Object[] attributes = {};
        
        when(mockStruct.getAttributes()).thenReturn(attributes);
        when(mockResultSet.getObject("sig_prop_col")).thenReturn(mockStruct);
        when(mockResultSet.wasNull()).thenReturn(false);
        
        String result = AqSigPropConverter.convertToJson(mockResultSet, "sig_prop_col");
        
        assertNotNull(result, "Empty struct conversion should not return null");
        
        // Parse and validate JSON structure
        JsonNode jsonNode = objectMapper.readTree(result);
        assertEquals("AQ_SIG_PROP", jsonNode.get("signature_type").asText());
        
        // Check that it has the simple structure (from createSimpleJson)
        JsonNode sigProps = jsonNode.get("signature_properties");
        assertNotNull(sigProps, "Signature properties should be present");
        assertEquals("UNKNOWN", sigProps.get("algorithm").asText());
        assertEquals("", sigProps.get("signature").asText());
        
        log.info("Empty struct AQ$_SIG_PROP conversion result: {}", result);
    }
    
    @Test
    public void testNonStructSigPropConversion() throws Exception {
        // Test non-STRUCT object conversion (fallback case)
        String fallbackValue = "Non-STRUCT signature property data";
        when(mockResultSet.getObject("sig_prop_col")).thenReturn(fallbackValue);
        when(mockResultSet.wasNull()).thenReturn(false);
        
        String result = AqSigPropConverter.convertToJson(mockResultSet, "sig_prop_col");
        
        assertNotNull(result, "Non-STRUCT conversion should not return null");
        
        // Parse and validate JSON structure
        JsonNode jsonNode = objectMapper.readTree(result);
        assertEquals("AQ_SIG_PROP", jsonNode.get("signature_type").asText());
        
        JsonNode sigProps = jsonNode.get("signature_properties");
        assertEquals("UNKNOWN", sigProps.get("algorithm").asText());
        assertEquals(fallbackValue, sigProps.get("signature").asText());
        
        JsonNode techInfo = jsonNode.get("technical_info");
        assertEquals("simple", techInfo.get("conversion_method").asText());
        
        log.info("Non-STRUCT AQ$_SIG_PROP conversion result: {}", result);
    }
    
    @Test
    public void testPartialAttributesSigPropConversion() throws Exception {
        // Test STRUCT with partial attributes (some null/missing)
        Object[] attributes = {
            "RSA-SHA256",     // algorithm
            null,             // digest (null)
            "partial-signature-data"  // signature
            // missing validation status and signer
        };
        
        when(mockStruct.getAttributes()).thenReturn(attributes);
        when(mockResultSet.getObject("sig_prop_col")).thenReturn(mockStruct);
        when(mockResultSet.wasNull()).thenReturn(false);
        
        String result = AqSigPropConverter.convertToJson(mockResultSet, "sig_prop_col");
        
        assertNotNull(result, "Partial attributes conversion should not return null");
        
        // Parse and validate JSON structure
        JsonNode jsonNode = objectMapper.readTree(result);
        
        JsonNode sigProps = jsonNode.get("signature_properties");
        assertEquals("RSA-SHA256", sigProps.get("algorithm").asText());
        assertEquals("partial-signature-data", sigProps.get("signature").asText());
        // Since digest was null, it won't be added to JSON or will be empty
        assertTrue(!sigProps.has("digest") || sigProps.get("digest").asText().isEmpty(), 
                   "Digest should be missing or empty when attribute is null");
        
        JsonNode metadata = jsonNode.get("metadata");
        assertEquals("UNKNOWN", metadata.get("validation_status").asText()); // Default when not found
        
        log.info("Partial attributes AQ$_SIG_PROP conversion result: {}", result);
    }
    
    @Test
    public void testSigPropConversionWithSQLException() throws Exception {
        // Test error handling when SQLException occurs
        when(mockResultSet.getObject("sig_prop_col")).thenThrow(new SQLException("Test SQL exception"));
        
        String result = AqSigPropConverter.convertToJson(mockResultSet, "sig_prop_col");
        
        assertNotNull(result, "Error conversion should not return null");
        
        // Parse and validate error JSON structure
        JsonNode jsonNode = objectMapper.readTree(result);
        assertEquals("AQ_SIG_PROP", jsonNode.get("signature_type").asText());
        assertTrue(jsonNode.has("conversion_error"), "Should have conversion error");
        assertTrue(jsonNode.get("conversion_error").asText().contains("Test SQL exception"));
        
        JsonNode techInfo = jsonNode.get("technical_info");
        assertEquals("ERROR", techInfo.get("conversion_status").asText());
        
        log.info("Error AQ$_SIG_PROP conversion result: {}", result);
    }
    
    @Test
    public void testSigPropConversionWithRuntimeException() throws Exception {
        // Test error handling when RuntimeException occurs
        when(mockResultSet.getObject("sig_prop_col")).thenThrow(new RuntimeException("Test runtime exception"));
        
        String result = AqSigPropConverter.convertToJson(mockResultSet, "sig_prop_col");
        
        assertNotNull(result, "Error conversion should not return null");
        
        // Parse and validate error JSON structure
        JsonNode jsonNode = objectMapper.readTree(result);
        assertEquals("AQ_SIG_PROP", jsonNode.get("signature_type").asText());
        assertTrue(jsonNode.has("conversion_error"), "Should have conversion error");
        assertTrue(jsonNode.get("conversion_error").asText().contains("Test runtime exception"));
        
        log.info("Runtime error AQ$_SIG_PROP conversion result: {}", result);
    }
    
    @Test
    public void testIsAqSigPropType() {
        // Test type detection utility method
        assertTrue(AqSigPropConverter.isAqSigPropType("aq$_sig_prop"));
        assertTrue(AqSigPropConverter.isAqSigPropType("AQ$_SIG_PROP"));
        assertTrue(AqSigPropConverter.isAqSigPropType("sys.aq$_sig_prop"));
        assertTrue(AqSigPropConverter.isAqSigPropType("SYS.AQ$_SIG_PROP"));
        assertTrue(AqSigPropConverter.isAqSigPropType("something_aq$_sig_prop_something"));
        
        assertFalse(AqSigPropConverter.isAqSigPropType("aq$_jms_text_message"));
        assertFalse(AqSigPropConverter.isAqSigPropType("varchar2"));
        assertFalse(AqSigPropConverter.isAqSigPropType(null));
        assertFalse(AqSigPropConverter.isAqSigPropType(""));
    }
    
    @Test
    public void testTableAnalyzerSigPropDetection() {
        // Test enhanced TableAnalyzer AQ$_SIG_PROP detection
        TableMetadata table = new TableMetadata("TEST_SCHEMA", "TEST_TABLE");
        
        // Add various column types including AQ$_SIG_PROP
        ColumnMetadata varchar2Col = new ColumnMetadata("name", "VARCHAR2", 100, null, null, true, null);
        ColumnMetadata numberCol = new ColumnMetadata("id", "NUMBER", null, 10, 0, false, null);
        ColumnMetadata sigPropCol = new ColumnMetadata("sig_data", "AQ$_SIG_PROP", null, null, null, true, null);
        ColumnMetadata anotherSigPropCol = new ColumnMetadata("extra_sig", "sys.aq$_sig_prop", null, null, null, true, null);
        
        table.addColumn(varchar2Col);
        table.addColumn(numberCol);
        table.addColumn(sigPropCol);
        table.addColumn(anotherSigPropCol);
        
        // Test AQ$_SIG_PROP detection
        assertTrue(TableAnalyzer.hasAqSigPropColumns(table), "Should detect AQ$_SIG_PROP columns");
        assertEquals(2, TableAnalyzer.countAqSigPropColumns(table), "Should count 2 AQ$_SIG_PROP columns");
        assertFalse(TableAnalyzer.hasOnlyPrimitiveTypes(table), "Table with AQ$_SIG_PROP should not be primitive-only");
        assertTrue(TableAnalyzer.hasComplexDataTypes(table), "Table with AQ$_SIG_PROP should have complex data types");
        
        // Test table without AQ$_SIG_PROP
        TableMetadata simpleTable = new TableMetadata("TEST_SCHEMA", "SIMPLE_TABLE");
        simpleTable.addColumn(varchar2Col);
        simpleTable.addColumn(numberCol);
        
        assertFalse(TableAnalyzer.hasAqSigPropColumns(simpleTable), "Should not detect AQ$_SIG_PROP in simple table");
        assertEquals(0, TableAnalyzer.countAqSigPropColumns(simpleTable), "Should count 0 AQ$_SIG_PROP columns");
        assertTrue(TableAnalyzer.hasOnlyPrimitiveTypes(simpleTable), "Simple table should be primitive-only");
    }
    
    @Test
    public void testSigPropColumnAnalysisIntegration() {
        // Test integration with enhanced table analysis
        TableMetadata table = new TableMetadata("HR", "SIGNATURE_DATA");
        
        // Add columns including AQ$_SIG_PROP
        table.addColumn(new ColumnMetadata("doc_id", "NUMBER", null, 10, 0, false, null));
        table.addColumn(new ColumnMetadata("doc_name", "VARCHAR2", 100, null, null, false, null));
        table.addColumn(new ColumnMetadata("signature1", "AQ$_SIG_PROP", null, null, null, true, null));
        table.addColumn(new ColumnMetadata("signature2", "SYS.AQ$_SIG_PROP", null, null, null, true, null));
        
        // Create a mock Everything context for the test
        me.christianrobert.ora2postgre.global.Everything mockEverything = mock(me.christianrobert.ora2postgre.global.Everything.class);
        when(mockEverything.getObjectTypeSpecAst()).thenReturn(java.util.List.of());
        
        // Test enhanced analysis
        String analysis = TableAnalyzer.analyzeTableWithObjectTypes(table, mockEverything);
        
        assertNotNull(analysis);
        assertTrue(analysis.contains("AQ$_SIG_PROP"), "Analysis should mention AQ$_SIG_PROP");
        assertTrue(analysis.contains("JSONB"), "Analysis should mention JSONB conversion");
        
        log.info("Table analysis result: {}", analysis);
    }
    
    @Test
    public void testPostgreSQLTypeMapping() {
        // Test that TypeConverter properly maps AQ$_SIG_PROP to JSONB
        assertEquals("jsonb", me.christianrobert.ora2postgre.plsql.ast.tools.TypeConverter.toPostgre("aq$_sig_prop"));
        assertEquals("jsonb", me.christianrobert.ora2postgre.plsql.ast.tools.TypeConverter.toPostgre("AQ$_SIG_PROP"));
        assertEquals("jsonb", me.christianrobert.ora2postgre.plsql.ast.tools.TypeConverter.toPostgre("sys.aq$_sig_prop"));
        assertEquals("jsonb", me.christianrobert.ora2postgre.plsql.ast.tools.TypeConverter.toPostgre("SYS.AQ$_SIG_PROP"));
        
        // Verify other types still work correctly
        assertEquals("jsonb", me.christianrobert.ora2postgre.plsql.ast.tools.TypeConverter.toPostgre("aq$_jms_text_message"));
        assertEquals("text", me.christianrobert.ora2postgre.plsql.ast.tools.TypeConverter.toPostgre("varchar2"));
    }
    
    @Test
    public void testComplexDataTypePatterns() {
        // Test various case variations and patterns of AQ$_SIG_PROP
        String[] sigPropVariations = {
            "AQ$_SIG_PROP", "aq$_sig_prop", "Aq$_Sig_Prop", "SYS.AQ$_SIG_PROP", "sys.aq$_sig_prop"
        };
        
        for (String variation : sigPropVariations) {
            TableMetadata table = new TableMetadata("TEST", "TABLE");
            table.addColumn(new ColumnMetadata("sig_col", variation, null, null, null, true, null));
            
            // The TableAnalyzer should detect AQ$_SIG_PROP regardless of case
            int count = TableAnalyzer.countAqSigPropColumns(table);
            boolean hasColumns = TableAnalyzer.hasAqSigPropColumns(table);
            
            assertTrue(count >= 1, "Should detect AQ$_SIG_PROP variation: " + variation);
            assertTrue(hasColumns, "Should detect AQ$_SIG_PROP columns for variation: " + variation);
            
            log.info("Testing AQ$_SIG_PROP variation '{}': detected count = {}, hasColumns = {}", 
                    variation, count, hasColumns);
        }
    }
    
    @Test
    public void testSignatureValidationStatuses() throws Exception {
        // Test different validation status scenarios
        String[] validationStatuses = {"VALID", "INVALID", "PENDING", "ERROR", "UNKNOWN"};
        
        for (String status : validationStatuses) {
            Object[] attributes = {
                "SHA256",
                "test-digest",
                "test-signature",
                status,  // validation status
                "CN=TestSigner"
            };
            
            when(mockStruct.getAttributes()).thenReturn(attributes);
            when(mockResultSet.getObject("sig_prop_col")).thenReturn(mockStruct);
            when(mockResultSet.wasNull()).thenReturn(false);
            
            String result = AqSigPropConverter.convertToJson(mockResultSet, "sig_prop_col");
            JsonNode jsonNode = objectMapper.readTree(result);
            
            assertEquals(status, jsonNode.get("metadata").get("validation_status").asText(),
                    "Validation status should be preserved: " + status);
            
            log.info("Testing validation status '{}': result contains correct status", status);
        }
    }
    
    @Test
    public void testDifferentAlgorithms() throws Exception {
        // Test different signature algorithms
        String[] algorithms = {"SHA256", "SHA512", "RSA-SHA256", "RSA-SHA512", "ECDSA-SHA256", "MD5"};
        
        for (String algorithm : algorithms) {
            Object[] attributes = {
                algorithm,  // algorithm
                "test-digest",
                "test-signature",
                "VALID",
                "CN=TestSigner"
            };
            
            when(mockStruct.getAttributes()).thenReturn(attributes);
            when(mockResultSet.getObject("sig_prop_col")).thenReturn(mockStruct);
            when(mockResultSet.wasNull()).thenReturn(false);
            
            String result = AqSigPropConverter.convertToJson(mockResultSet, "sig_prop_col");
            JsonNode jsonNode = objectMapper.readTree(result);
            
            assertEquals(algorithm, jsonNode.get("signature_properties").get("algorithm").asText(),
                    "Algorithm should be preserved: " + algorithm);
            
            log.info("Testing algorithm '{}': result contains correct algorithm", algorithm);
        }
    }
    
    @Test
    public void testJsonStructureConsistency() throws Exception {
        // Test that the JSON structure is consistent across different inputs
        Object[] attributes = {"SHA256", "digest", "signature", "VALID", "signer"};
        
        when(mockStruct.getAttributes()).thenReturn(attributes);
        when(mockResultSet.getObject("sig_prop_col")).thenReturn(mockStruct);
        when(mockResultSet.wasNull()).thenReturn(false);
        
        String result = AqSigPropConverter.convertToJson(mockResultSet, "sig_prop_col");
        JsonNode jsonNode = objectMapper.readTree(result);
        
        // Verify all required top-level fields are present
        assertTrue(jsonNode.has("signature_type"), "Should have signature_type field");
        assertTrue(jsonNode.has("signature_properties"), "Should have signature_properties field");
        assertTrue(jsonNode.has("metadata"), "Should have metadata field");
        assertTrue(jsonNode.has("technical_info"), "Should have technical_info field");
        
        // Verify signature_properties structure
        JsonNode sigProps = jsonNode.get("signature_properties");
        assertTrue(sigProps.has("algorithm"), "Should have algorithm in signature_properties");
        assertTrue(sigProps.has("digest"), "Should have digest in signature_properties");
        assertTrue(sigProps.has("signature"), "Should have signature in signature_properties");
        
        // Verify metadata structure
        JsonNode metadata = jsonNode.get("metadata");
        assertTrue(metadata.has("timestamp"), "Should have timestamp in metadata");
        assertTrue(metadata.has("validation_status"), "Should have validation_status in metadata");
        
        // Verify technical_info structure
        JsonNode techInfo = jsonNode.get("technical_info");
        assertTrue(techInfo.has("original_type"), "Should have original_type in technical_info");
        assertTrue(techInfo.has("conversion_timestamp"), "Should have conversion_timestamp in technical_info");
        assertTrue(techInfo.has("attributes_count"), "Should have attributes_count in technical_info");
        
        log.info("JSON structure consistency test passed");
    }
}