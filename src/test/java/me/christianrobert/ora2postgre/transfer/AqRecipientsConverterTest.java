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
 * Comprehensive test suite for AQ$_RECIPIENTS to JSONB conversion functionality.
 *
 * Tests the AqRecipientsConverter class and its integration with the migration pipeline,
 * covering Oracle Advanced Queuing recipients conversion to PostgreSQL JSONB format.
 */
public class AqRecipientsConverterTest {

  private static final Logger log = LoggerFactory.getLogger(AqRecipientsConverterTest.class);
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
  public void testNullRecipientsConversion() throws SQLException {
    // Test NULL AQ$_RECIPIENTS conversion
    when(mockResultSet.getObject("recipients_col")).thenReturn(null);
    when(mockResultSet.wasNull()).thenReturn(true);

    String result = AqRecipientsConverter.convertToJson(mockResultSet, "recipients_col");

    assertNull(result, "NULL AQ$_RECIPIENTS should return null JSON");
  }

  @Test
  public void testStructRecipientsConversion() throws Exception {
    // Test Oracle STRUCT representing AQ$_RECIPIENTS conversion
    Object[] attributes = {
            "user1@domain.com",                     // recipient 1 address
            "User One",                             // recipient 1 name
            "user2@company.org",                    // recipient 2 address  
            "BROADCAST",                            // delivery mode
            "HIGH"                                  // priority level
    };

    when(mockStruct.getAttributes()).thenReturn(attributes);
    when(mockResultSet.getObject("recipients_col")).thenReturn(mockStruct);
    when(mockResultSet.wasNull()).thenReturn(false);

    String result = AqRecipientsConverter.convertToJson(mockResultSet, "recipients_col");

    assertNotNull(result, "AQ$_RECIPIENTS conversion should not return null");

    // Parse and validate JSON structure
    JsonNode jsonNode = objectMapper.readTree(result);

    assertEquals("AQ_RECIPIENTS", jsonNode.get("recipients_type").asText(), "Recipients type should be AQ_RECIPIENTS");

    // Test recipients array
    JsonNode recipientsArray = jsonNode.get("recipients");
    assertNotNull(recipientsArray, "Recipients array should be present");
    assertTrue(recipientsArray.isArray(), "Recipients should be an array");
    assertTrue(recipientsArray.size() > 0, "Recipients array should not be empty");

    // Test first recipient
    JsonNode firstRecipient = recipientsArray.get(0);
    assertNotNull(firstRecipient, "First recipient should be present");
    assertTrue(firstRecipient.has("address"), "Recipient should have address");
    assertTrue(firstRecipient.has("name"), "Recipient should have name");
    assertTrue(firstRecipient.has("delivery_status"), "Recipient should have delivery status");
    assertTrue(firstRecipient.has("routing_info"), "Recipient should have routing info");

    // Test metadata
    JsonNode metadata = jsonNode.get("metadata");
    assertNotNull(metadata, "Metadata should be present");
    assertTrue(metadata.has("total_count"), "Metadata should have total count");
    assertTrue(metadata.has("timestamp"), "Metadata should have timestamp");
    assertTrue(metadata.has("delivery_mode"), "Metadata should have delivery mode");

    // Test technical information
    JsonNode techInfo = jsonNode.get("technical_info");
    assertNotNull(techInfo, "Technical info should be present");
    assertEquals("SYS.AQ$_RECIPIENTS", techInfo.get("original_type").asText(), "Original type should be SYS.AQ$_RECIPIENTS");
    assertTrue(techInfo.has("conversion_timestamp"), "Conversion timestamp should be present");

    log.info("Struct AQ$_RECIPIENTS conversion result: {}", result);
  }

  @Test
  public void testEmptyStructRecipientsConversion() throws Exception {
    // Test Oracle STRUCT with no attributes
    Object[] attributes = {};

    when(mockStruct.getAttributes()).thenReturn(attributes);
    when(mockResultSet.getObject("recipients_col")).thenReturn(mockStruct);
    when(mockResultSet.wasNull()).thenReturn(false);

    String result = AqRecipientsConverter.convertToJson(mockResultSet, "recipients_col");

    assertNotNull(result, "Empty struct conversion should not return null");

    // Parse and validate JSON structure
    JsonNode jsonNode = objectMapper.readTree(result);
    assertEquals("AQ_RECIPIENTS", jsonNode.get("recipients_type").asText());

    // Check that it has the simple structure
    JsonNode recipientsArray = jsonNode.get("recipients");
    assertNotNull(recipientsArray, "Recipients array should be present");
    assertTrue(recipientsArray.isArray(), "Recipients should be an array");
    assertEquals(1, recipientsArray.size(), "Should have one placeholder recipient");

    JsonNode recipient = recipientsArray.get(0);
    assertEquals("unknown@domain.com", recipient.get("address").asText(), "Should have default address");
    assertEquals("Unknown Recipient", recipient.get("name").asText(), "Should have default name");

    log.info("Empty struct AQ$_RECIPIENTS conversion result: {}", result);
  }

  @Test
  public void testNonStructRecipientsConversion() throws Exception {
    // Test non-STRUCT object conversion (fallback case)
    String fallbackValue = "admin@system.com";
    when(mockResultSet.getObject("recipients_col")).thenReturn(fallbackValue);
    when(mockResultSet.wasNull()).thenReturn(false);

    String result = AqRecipientsConverter.convertToJson(mockResultSet, "recipients_col");

    assertNotNull(result, "Non-STRUCT conversion should not return null");

    // Parse and validate JSON structure
    JsonNode jsonNode = objectMapper.readTree(result);
    assertEquals("AQ_RECIPIENTS", jsonNode.get("recipients_type").asText());

    JsonNode recipientsArray = jsonNode.get("recipients");
    JsonNode recipient = recipientsArray.get(0);
    assertEquals(fallbackValue, recipient.get("address").asText(), "Address should be preserved");
    assertEquals("Admin", recipient.get("name").asText(), "Name should be extracted from address");

    JsonNode techInfo = jsonNode.get("technical_info");
    assertEquals("simple", techInfo.get("conversion_method").asText());

    log.info("Non-STRUCT AQ$_RECIPIENTS conversion result: {}", result);
  }

  @Test
  public void testMultipleRecipientsConversion() throws Exception {
    // Test STRUCT with multiple recipient-like attributes
    Object[] attributes = {
            "admin@company.com",
            "support@company.com",
            "alerts@company.com",
            "notifications@company.com",
            "MULTICAST",
            "MEDIUM"
    };

    when(mockStruct.getAttributes()).thenReturn(attributes);
    when(mockResultSet.getObject("recipients_col")).thenReturn(mockStruct);
    when(mockResultSet.wasNull()).thenReturn(false);

    String result = AqRecipientsConverter.convertToJson(mockResultSet, "recipients_col");

    assertNotNull(result, "Multiple recipients conversion should not return null");

    JsonNode jsonNode = objectMapper.readTree(result);
    JsonNode recipientsArray = jsonNode.get("recipients");

    assertTrue(recipientsArray.size() > 1, "Should have multiple recipients");

    // Check that all email addresses were converted to recipients
    for (int i = 0; i < recipientsArray.size(); i++) {
      JsonNode recipient = recipientsArray.get(i);
      assertTrue(recipient.get("address").asText().contains("@"), "All recipients should have email addresses");
      assertTrue(recipient.has("name"), "All recipients should have names");
    }

    log.info("Multiple recipients conversion result: {} recipients", recipientsArray.size());
  }

  @Test
  public void testStructuredDataConversion() throws Exception {
    // Test STRUCT with structured key=value data
    Object[] attributes = {
            "address=john@example.com,name=John Doe,status=PENDING",
            "address=jane@example.com,name=Jane Smith,status=DELIVERED",
            "UNICAST"
    };

    when(mockStruct.getAttributes()).thenReturn(attributes);
    when(mockResultSet.getObject("recipients_col")).thenReturn(mockStruct);
    when(mockResultSet.wasNull()).thenReturn(false);

    String result = AqRecipientsConverter.convertToJson(mockResultSet, "recipients_col");

    assertNotNull(result, "Structured data conversion should not return null");

    JsonNode jsonNode = objectMapper.readTree(result);
    JsonNode recipientsArray = jsonNode.get("recipients");

    assertTrue(recipientsArray.size() >= 1, "Should have parsed recipients");

    // Check if structured data was parsed (this is best effort)
    JsonNode firstRecipient = recipientsArray.get(0);
    assertNotNull(firstRecipient.get("address"), "Should have address");
    assertNotNull(firstRecipient.get("name"), "Should have name");

    log.info("Structured data conversion result: {}", result);
  }

  @Test
  public void testRecipientsConversionWithSQLException() throws Exception {
    // Test error handling when SQLException occurs
    when(mockResultSet.getObject("recipients_col")).thenThrow(new SQLException("Test SQL exception"));

    String result = AqRecipientsConverter.convertToJson(mockResultSet, "recipients_col");

    assertNotNull(result, "Error conversion should not return null");

    // Parse and validate error JSON structure
    JsonNode jsonNode = objectMapper.readTree(result);
    assertEquals("AQ_RECIPIENTS", jsonNode.get("recipients_type").asText());
    assertTrue(jsonNode.has("conversion_error"), "Should have conversion error");
    assertTrue(jsonNode.get("conversion_error").asText().contains("Test SQL exception"));

    JsonNode techInfo = jsonNode.get("technical_info");
    assertEquals("ERROR", techInfo.get("conversion_status").asText());

    JsonNode metadata = jsonNode.get("metadata");
    assertEquals("ERROR", metadata.get("delivery_mode").asText());

    log.info("Error AQ$_RECIPIENTS conversion result: {}", result);
  }

  @Test
  public void testRecipientsConversionWithRuntimeException() throws Exception {
    // Test error handling when RuntimeException occurs
    when(mockResultSet.getObject("recipients_col")).thenThrow(new RuntimeException("Test runtime exception"));

    String result = AqRecipientsConverter.convertToJson(mockResultSet, "recipients_col");

    assertNotNull(result, "Error conversion should not return null");

    // Parse and validate error JSON structure
    JsonNode jsonNode = objectMapper.readTree(result);
    assertEquals("AQ_RECIPIENTS", jsonNode.get("recipients_type").asText());
    assertTrue(jsonNode.has("conversion_error"), "Should have conversion error");
    assertTrue(jsonNode.get("conversion_error").asText().contains("Test runtime exception"));

    log.info("Runtime error AQ$_RECIPIENTS conversion result: {}", result);
  }

  @Test
  public void testIsAqRecipientsType() {
    // Test type detection utility method
    assertTrue(AqRecipientsConverter.isAqRecipientsType("aq$_recipients"));
    assertTrue(AqRecipientsConverter.isAqRecipientsType("AQ$_RECIPIENTS"));
    assertTrue(AqRecipientsConverter.isAqRecipientsType("sys.aq$_recipients"));
    assertTrue(AqRecipientsConverter.isAqRecipientsType("SYS.AQ$_RECIPIENTS"));
    assertTrue(AqRecipientsConverter.isAqRecipientsType("something_aq$_recipients_something"));

    assertFalse(AqRecipientsConverter.isAqRecipientsType("aq$_jms_text_message"));
    assertFalse(AqRecipientsConverter.isAqRecipientsType("aq$_sig_prop"));
    assertFalse(AqRecipientsConverter.isAqRecipientsType("varchar2"));
    assertFalse(AqRecipientsConverter.isAqRecipientsType(null));
    assertFalse(AqRecipientsConverter.isAqRecipientsType(""));
  }

  @Test
  public void testTableAnalyzerRecipientsDetection() {
    // Test enhanced TableAnalyzer AQ$_RECIPIENTS detection
    TableMetadata table = new TableMetadata("TEST_SCHEMA", "TEST_TABLE");

    // Add various column types including AQ$_RECIPIENTS
    ColumnMetadata varchar2Col = new ColumnMetadata("name", "VARCHAR2", 100, null, null, true, null);
    ColumnMetadata numberCol = new ColumnMetadata("id", "NUMBER", null, 10, 0, false, null);
    ColumnMetadata recipientsCol = new ColumnMetadata("recipients_data", "AQ$_RECIPIENTS", null, null, null, true, null);
    ColumnMetadata anotherRecipientsCol = new ColumnMetadata("extra_recipients", "sys.aq$_recipients", null, null, null, true, null);

    table.addColumn(varchar2Col);
    table.addColumn(numberCol);
    table.addColumn(recipientsCol);
    table.addColumn(anotherRecipientsCol);

    // Test AQ$_RECIPIENTS detection
    assertTrue(TableAnalyzer.hasAqRecipientsColumns(table), "Should detect AQ$_RECIPIENTS columns");
    assertEquals(2, TableAnalyzer.countAqRecipientsColumns(table), "Should count 2 AQ$_RECIPIENTS columns");
    assertFalse(TableAnalyzer.hasOnlyPrimitiveTypes(table), "Table with AQ$_RECIPIENTS should not be primitive-only");
    assertTrue(TableAnalyzer.hasComplexDataTypes(table), "Table with AQ$_RECIPIENTS should have complex data types");

    // Test table without AQ$_RECIPIENTS
    TableMetadata simpleTable = new TableMetadata("TEST_SCHEMA", "SIMPLE_TABLE");
    simpleTable.addColumn(varchar2Col);
    simpleTable.addColumn(numberCol);

    assertFalse(TableAnalyzer.hasAqRecipientsColumns(simpleTable), "Should not detect AQ$_RECIPIENTS in simple table");
    assertEquals(0, TableAnalyzer.countAqRecipientsColumns(simpleTable), "Should count 0 AQ$_RECIPIENTS columns");
    assertTrue(TableAnalyzer.hasOnlyPrimitiveTypes(simpleTable), "Simple table should be primitive-only");
  }

  @Test
  public void testRecipientsColumnAnalysisIntegration() {
    // Test integration with enhanced table analysis
    TableMetadata table = new TableMetadata("MESSAGING", "RECIPIENT_DATA");

    // Add columns including AQ$_RECIPIENTS
    table.addColumn(new ColumnMetadata("msg_id", "NUMBER", null, 10, 0, false, null));
    table.addColumn(new ColumnMetadata("subject", "VARCHAR2", 255, null, null, false, null));
    table.addColumn(new ColumnMetadata("to_recipients", "AQ$_RECIPIENTS", null, null, null, true, null));
    table.addColumn(new ColumnMetadata("cc_recipients", "SYS.AQ$_RECIPIENTS", null, null, null, true, null));

    // Create a mock Everything context for the test
    me.christianrobert.ora2postgre.global.Everything mockEverything = mock(me.christianrobert.ora2postgre.global.Everything.class);
    when(mockEverything.getObjectTypeSpecAst()).thenReturn(java.util.List.of());

    // Test enhanced analysis
    String analysis = TableAnalyzer.analyzeTableWithObjectTypes(table, mockEverything);

    assertNotNull(analysis);
    assertTrue(analysis.contains("AQ$_RECIPIENTS"), "Analysis should mention AQ$_RECIPIENTS");
    assertTrue(analysis.contains("JSONB"), "Analysis should mention JSONB conversion");

    log.info("Table analysis result: {}", analysis);
  }

  @Test
  public void testPostgreSQLTypeMapping() {
    // Test that TypeConverter properly maps AQ$_RECIPIENTS to JSONB
    assertEquals("jsonb", me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter.toPostgre("aq$_recipients"));
    assertEquals("jsonb", me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter.toPostgre("AQ$_RECIPIENTS"));
    assertEquals("jsonb", me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter.toPostgre("sys.aq$_recipients"));
    assertEquals("jsonb", me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter.toPostgre("SYS.AQ$_RECIPIENTS"));

    // Verify other AQ types still work correctly
    assertEquals("jsonb", me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter.toPostgre("aq$_jms_text_message"));
    assertEquals("jsonb", me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter.toPostgre("aq$_sig_prop"));
    assertEquals("text", me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter.toPostgre("varchar2"));
  }

  @Test
  public void testComplexDataTypePatterns() {
    // Test various case variations and patterns of AQ$_RECIPIENTS
    String[] recipientsVariations = {
            "AQ$_RECIPIENTS", "aq$_recipients", "Aq$_Recipients", "SYS.AQ$_RECIPIENTS", "sys.aq$_recipients"
    };

    for (String variation : recipientsVariations) {
      TableMetadata table = new TableMetadata("TEST", "TABLE");
      table.addColumn(new ColumnMetadata("recipients_col", variation, null, null, null, true, null));

      // The TableAnalyzer should detect AQ$_RECIPIENTS regardless of case
      int count = TableAnalyzer.countAqRecipientsColumns(table);
      boolean hasColumns = TableAnalyzer.hasAqRecipientsColumns(table);

      assertTrue(count >= 1, "Should detect AQ$_RECIPIENTS variation: " + variation);
      assertTrue(hasColumns, "Should detect AQ$_RECIPIENTS columns for variation: " + variation);

      log.info("Testing AQ$_RECIPIENTS variation '{}': detected count = {}, hasColumns = {}",
              variation, count, hasColumns);
    }
  }

  @Test
  public void testDifferentDeliveryModes() throws Exception {
    // Test different delivery modes
    String[] deliveryModes = {"BROADCAST", "MULTICAST", "UNICAST", "DIRECT", "RELAY"};

    for (String mode : deliveryModes) {
      Object[] attributes = {
              "test@example.com",
              mode  // delivery mode
      };

      when(mockStruct.getAttributes()).thenReturn(attributes);
      when(mockResultSet.getObject("recipients_col")).thenReturn(mockStruct);
      when(mockResultSet.wasNull()).thenReturn(false);

      String result = AqRecipientsConverter.convertToJson(mockResultSet, "recipients_col");
      JsonNode jsonNode = objectMapper.readTree(result);

      // Check if delivery mode was properly detected and stored
      JsonNode metadata = jsonNode.get("metadata");
      assertNotNull(metadata, "Metadata should be present");

      log.info("Testing delivery mode '{}': result contains metadata", mode);
    }
  }

  @Test
  public void testEmailAddressExtraction() throws Exception {
    // Test various email address formats
    String[] emailFormats = {
            "user@domain.com",
            "first.last@company.org",
            "admin+alerts@system.net",
            "test_user@sub.domain.co.uk"
    };

    for (String email : emailFormats) {
      Object[] attributes = { email };

      when(mockStruct.getAttributes()).thenReturn(attributes);
      when(mockResultSet.getObject("recipients_col")).thenReturn(mockStruct);
      when(mockResultSet.wasNull()).thenReturn(false);

      String result = AqRecipientsConverter.convertToJson(mockResultSet, "recipients_col");
      JsonNode jsonNode = objectMapper.readTree(result);
      JsonNode recipientsArray = jsonNode.get("recipients");
      JsonNode recipient = recipientsArray.get(0);

      assertEquals(email, recipient.get("address").asText(), "Email address should be preserved: " + email);
      assertTrue(recipient.get("name").asText().length() > 0, "Name should be extracted from email");

      log.info("Testing email format '{}': extracted name = '{}'",
              email, recipient.get("name").asText());
    }
  }

  @Test
  public void testJsonStructureConsistency() throws Exception {
    // Test that the JSON structure is consistent across different inputs
    Object[] attributes = {"test@example.com", "DIRECT", "HIGH"};

    when(mockStruct.getAttributes()).thenReturn(attributes);
    when(mockResultSet.getObject("recipients_col")).thenReturn(mockStruct);
    when(mockResultSet.wasNull()).thenReturn(false);

    String result = AqRecipientsConverter.convertToJson(mockResultSet, "recipients_col");
    JsonNode jsonNode = objectMapper.readTree(result);

    // Verify all required top-level fields are present
    assertTrue(jsonNode.has("recipients_type"), "Should have recipients_type field");
    assertTrue(jsonNode.has("recipients"), "Should have recipients field");
    assertTrue(jsonNode.has("metadata"), "Should have metadata field");
    assertTrue(jsonNode.has("technical_info"), "Should have technical_info field");

    // Verify recipients array structure
    JsonNode recipientsArray = jsonNode.get("recipients");
    assertTrue(recipientsArray.isArray(), "Recipients should be an array");
    assertTrue(recipientsArray.size() > 0, "Recipients array should not be empty");

    // Verify recipient structure
    JsonNode recipient = recipientsArray.get(0);
    assertTrue(recipient.has("address"), "Recipient should have address");
    assertTrue(recipient.has("name"), "Recipient should have name");
    assertTrue(recipient.has("delivery_status"), "Recipient should have delivery_status");
    assertTrue(recipient.has("routing_info"), "Recipient should have routing_info");

    // Verify metadata structure
    JsonNode metadata = jsonNode.get("metadata");
    assertTrue(metadata.has("total_count"), "Should have total_count in metadata");
    assertTrue(metadata.has("timestamp"), "Should have timestamp in metadata");
    assertTrue(metadata.has("delivery_mode"), "Should have delivery_mode in metadata");

    // Verify technical_info structure
    JsonNode techInfo = jsonNode.get("technical_info");
    assertTrue(techInfo.has("original_type"), "Should have original_type in technical_info");
    assertTrue(techInfo.has("conversion_timestamp"), "Should have conversion_timestamp in technical_info");

    log.info("JSON structure consistency test passed");
  }
}