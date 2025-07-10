package me.christianrobert.ora2postgre.transfer;

import me.christianrobert.ora2postgre.oracledb.ColumnMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for ANYDATA to JSONB conversion functionality.
 *
 * Tests the AnydataConverter class and its integration with the migration pipeline,
 * covering various Oracle data types that can be stored in ANYDATA columns.
 */
public class AnydataConversionTest {

  private static final Logger log = LoggerFactory.getLogger(AnydataConversionTest.class);
  private ObjectMapper objectMapper;
  private ResultSet mockResultSet;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockResultSet = mock(ResultSet.class);
  }

  @Test
  public void testNullAnydataConversion() throws SQLException {
    // Test NULL ANYDATA conversion
    when(mockResultSet.getObject("anydata_col")).thenReturn(null);

    String result = AnydataConverter.convertAnydataToJson(mockResultSet, "anydata_col");

    assertNull(result, "NULL ANYDATA should return null JSON");
  }

  @Test
  public void testOracleSpecificAnydataConversion() throws Exception {
    // Test Oracle-specific ANYDATA conversion with mock oracle.sql.ANYDATA
    oracle.sql.ANYDATA mockAnydata = mock(oracle.sql.ANYDATA.class);
    oracle.sql.TypeDescriptor mockTypeDescriptor = mock(oracle.sql.TypeDescriptor.class);
    oracle.sql.Datum mockDatum = mock(oracle.sql.Datum.class);

    // Setup mocks
    when(mockAnydata.isNull()).thenReturn(false);
    when(mockAnydata.getTypeDescriptor()).thenReturn(mockTypeDescriptor);
    when(mockAnydata.accessDatum()).thenReturn(mockDatum);
    when(mockTypeDescriptor.getTypeCode()).thenReturn((int)oracle.sql.TypeDescriptor.TYPECODE_VARCHAR2);
    when(mockTypeDescriptor.getName()).thenReturn("SYS.VARCHAR2");
    when(mockDatum.stringValue()).thenReturn("Test Oracle String");

    when(mockResultSet.getObject("anydata_col")).thenReturn(mockAnydata);

    String result = AnydataConverter.convertAnydataToJson(mockResultSet, "anydata_col");

    assertNotNull(result, "Oracle ANYDATA conversion should not return null");
    assertTrue(AnydataConverter.isValidJson(result), "Result should be valid JSON");

    // Parse and validate JSON structure
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(result);

    assertEquals("SYS.VARCHAR2", jsonNode.get("type").asText(), "Type should be SYS.VARCHAR2");
    assertEquals("Test Oracle String", jsonNode.get("value").asText(), "Value should be extracted correctly");

    JsonNode metadata = jsonNode.get("metadata");
    assertNotNull(metadata, "Metadata should be present");
    assertEquals("SYS.ANYDATA", metadata.get("original_type").asText(), "Original type should be SYS.ANYDATA");
    assertEquals("oracle_specific", metadata.get("conversion_method").asText(), "Should use Oracle-specific conversion");
    assertEquals("SYS.VARCHAR2", metadata.get("extracted_type").asText(), "Extracted type should match");
    assertEquals((int)oracle.sql.TypeDescriptor.TYPECODE_VARCHAR2, metadata.get("type_code").asInt(), "Type code should match");

    log.info("Oracle-specific ANYDATA conversion result: {}", result);
  }

  @Test
  public void testOracleStructAnydataConversion() throws Exception {
    // Test Oracle STRUCT (user-defined object type) conversion
    oracle.sql.ANYDATA mockAnydata = mock(oracle.sql.ANYDATA.class);
    oracle.sql.TypeDescriptor mockTypeDescriptor = mock(oracle.sql.TypeDescriptor.class);
    oracle.sql.STRUCT mockStruct = mock(oracle.sql.STRUCT.class);

    // Setup mocks for user-defined object type
    when(mockAnydata.isNull()).thenReturn(false);
    when(mockAnydata.getTypeDescriptor()).thenReturn(mockTypeDescriptor);
    when(mockAnydata.accessDatum()).thenReturn(mockStruct);
    when(mockTypeDescriptor.getTypeCode()).thenReturn((int)oracle.sql.TypeDescriptor.TYPECODE_OBJECT);
    when(mockTypeDescriptor.getName()).thenReturn("SCHEMA.PERSON_TYPE");
    when(mockStruct.getSQLTypeName()).thenReturn("SCHEMA.PERSON_TYPE");
    when(mockStruct.getAttributes()).thenReturn(new Object[]{"John Doe", new BigDecimal("30"), new java.sql.Date(System.currentTimeMillis())});

    when(mockResultSet.getObject("anydata_col")).thenReturn(mockAnydata);

    String result = AnydataConverter.convertAnydataToJson(mockResultSet, "anydata_col");

    assertNotNull(result, "Oracle STRUCT ANYDATA conversion should not return null");
    assertTrue(AnydataConverter.isValidJson(result), "Result should be valid JSON");

    // Parse and validate JSON structure
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(result);

    assertEquals("SCHEMA.PERSON_TYPE", jsonNode.get("type").asText(), "Type should be user-defined type name");

    JsonNode valueNode = jsonNode.get("value");
    assertNotNull(valueNode, "Value should be present");
    assertTrue(valueNode.isObject(), "Value should be an object for STRUCT types");

    // Check that attributes are properly decomposed
    assertEquals("John Doe", valueNode.get("attr_0").asText(), "First attribute should be name");
    assertEquals(30, valueNode.get("attr_1").asInt(), "Second attribute should be age");
    assertNotNull(valueNode.get("attr_2"), "Third attribute should be date");
    assertEquals("SCHEMA.PERSON_TYPE", valueNode.get("_struct_type").asText(), "Struct type should be preserved");

    JsonNode metadata = jsonNode.get("metadata");
    assertNotNull(metadata, "Metadata should be present");
    assertEquals("oracle_specific", metadata.get("conversion_method").asText(), "Should use Oracle-specific conversion");
    assertEquals((int)oracle.sql.TypeDescriptor.TYPECODE_OBJECT, metadata.get("type_code").asInt(), "Type code should be OBJECT");

    log.info("Oracle STRUCT ANYDATA conversion result: {}", result);
  }

  @Test
  public void testOracleAnydataFallbackToGeneric() throws Exception {
    // Test fallback when ANYDATA is not oracle.sql.ANYDATA instance
    String testValue = "Generic JDBC String";
    when(mockResultSet.getObject("anydata_col")).thenReturn(testValue);

    String result = AnydataConverter.convertAnydataToJson(mockResultSet, "anydata_col");

    assertNotNull(result, "Fallback conversion should not return null");
    assertTrue(AnydataConverter.isValidJson(result), "Result should be valid JSON");

    // Parse and validate JSON structure
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(result);

    assertEquals("SYS.VARCHAR2", jsonNode.get("type").asText(), "Type should be inferred as VARCHAR2");
    assertEquals(testValue, jsonNode.get("value").asText(), "Value should be preserved");

    JsonNode metadata = jsonNode.get("metadata");
    assertNotNull(metadata, "Metadata should be present");
    assertEquals("SYS.ANYDATA", metadata.get("original_type").asText(), "Original type should be SYS.ANYDATA");
    // Should not have "oracle_specific" conversion method since it's using fallback
    assertNull(metadata.get("conversion_method"), "Should not have oracle_specific conversion method");

    log.info("Fallback ANYDATA conversion result: {}", result);
  }

  @Test
  public void testStringAnydataConversion() throws Exception {
    // Test VARCHAR2 data stored in ANYDATA
    String testValue = "Hello World";
    when(mockResultSet.getObject("anydata_col")).thenReturn(testValue);

    String jsonResult = AnydataConverter.convertAnydataToJson(mockResultSet, "anydata_col");

    assertNotNull(jsonResult);
    assertTrue(AnydataConverter.isValidJson(jsonResult));

    // Parse and verify JSON structure
    JsonNode jsonNode = objectMapper.readTree(jsonResult);
    assertEquals("SYS.VARCHAR2", jsonNode.get("type").asText());
    assertEquals(testValue, jsonNode.get("value").asText());

    JsonNode metadata = jsonNode.get("metadata");
    assertEquals("SYS.ANYDATA", metadata.get("original_type").asText());
    assertEquals("anydata_col", metadata.get("column_name").asText());

    log.info("String ANYDATA conversion result: {}", jsonResult);
  }

  @Test
  public void testNumericAnydataConversion() throws Exception {
    // Test NUMBER data stored in ANYDATA
    BigDecimal testValue = new BigDecimal("123.45");
    when(mockResultSet.getObject("anydata_col")).thenReturn(testValue);

    String jsonResult = AnydataConverter.convertAnydataToJson(mockResultSet, "anydata_col");

    assertNotNull(jsonResult);
    assertTrue(AnydataConverter.isValidJson(jsonResult));

    // Parse and verify JSON structure
    JsonNode jsonNode = objectMapper.readTree(jsonResult);
    assertEquals("SYS.NUMBER", jsonNode.get("type").asText());
    assertEquals(testValue.doubleValue(), jsonNode.get("value").asDouble(), 0.001);

    log.info("Numeric ANYDATA conversion result: {}", jsonResult);
  }

  @Test
  public void testIntegerAnydataConversion() throws Exception {
    // Test INTEGER data stored in ANYDATA
    Integer testValue = 42;
    when(mockResultSet.getObject("anydata_col")).thenReturn(testValue);

    String jsonResult = AnydataConverter.convertAnydataToJson(mockResultSet, "anydata_col");

    assertNotNull(jsonResult);
    assertTrue(AnydataConverter.isValidJson(jsonResult));

    // Parse and verify JSON structure
    JsonNode jsonNode = objectMapper.readTree(jsonResult);
    assertEquals("SYS.NUMBER", jsonNode.get("type").asText());
    assertEquals(testValue.intValue(), jsonNode.get("value").asInt());

    log.info("Integer ANYDATA conversion result: {}", jsonResult);
  }

  @Test
  public void testDateAnydataConversion() throws Exception {
    // Test DATE data stored in ANYDATA
    Date testValue = Date.valueOf("2023-12-25");
    when(mockResultSet.getObject("anydata_col")).thenReturn(testValue);

    String jsonResult = AnydataConverter.convertAnydataToJson(mockResultSet, "anydata_col");

    assertNotNull(jsonResult);
    assertTrue(AnydataConverter.isValidJson(jsonResult));

    // Parse and verify JSON structure
    JsonNode jsonNode = objectMapper.readTree(jsonResult);
    assertEquals("SYS.DATE", jsonNode.get("type").asText());
    assertEquals(testValue.toString(), jsonNode.get("value").asText());

    log.info("Date ANYDATA conversion result: {}", jsonResult);
  }

  @Test
  public void testTimestampAnydataConversion() throws Exception {
    // Test TIMESTAMP data stored in ANYDATA
    Timestamp testValue = Timestamp.valueOf("2023-12-25 15:30:45.123");
    when(mockResultSet.getObject("anydata_col")).thenReturn(testValue);

    String jsonResult = AnydataConverter.convertAnydataToJson(mockResultSet, "anydata_col");

    assertNotNull(jsonResult);
    assertTrue(AnydataConverter.isValidJson(jsonResult));

    // Parse and verify JSON structure
    JsonNode jsonNode = objectMapper.readTree(jsonResult);
    assertEquals("SYS.TIMESTAMP", jsonNode.get("type").asText());
    assertEquals(testValue.toString(), jsonNode.get("value").asText());

    log.info("Timestamp ANYDATA conversion result: {}", jsonResult);
  }

  @Test
  public void testAnydataConversionWithSQLException() throws Exception {
    // Test error handling when SQLException occurs
    when(mockResultSet.getObject("anydata_col")).thenThrow(new SQLException("Test SQL exception"));

    String jsonResult = AnydataConverter.convertAnydataToJson(mockResultSet, "anydata_col");

    assertNotNull(jsonResult);
    assertTrue(AnydataConverter.isValidJson(jsonResult));

    // Parse and verify error JSON structure
    JsonNode jsonNode = objectMapper.readTree(jsonResult);
    assertEquals("ERROR", jsonNode.get("type").asText());
    assertEquals("CONVERSION_ERROR", jsonNode.get("error_type").asText());
    assertTrue(jsonNode.get("error_message").asText().contains("Test SQL exception"));

    log.info("Error ANYDATA conversion result: {}", jsonResult);
  }

  @Test
  public void testJsonValidation() {
    // Test JSON validation utility methods
    assertTrue(AnydataConverter.isValidJson("{\"type\":\"test\",\"value\":\"data\"}"));
    assertFalse(AnydataConverter.isValidJson("{invalid json"));
    assertFalse(AnydataConverter.isValidJson(null));
    assertFalse(AnydataConverter.isValidJson(""));
    assertFalse(AnydataConverter.isValidJson("   "));
  }

  @Test
  public void testTypeExtractionFromJson() throws Exception {
    // Test utility method for extracting type from JSON
    String testJson = "{\"type\":\"SYS.VARCHAR2\",\"value\":\"test\",\"metadata\":{}}";

    String extractedType = AnydataConverter.extractTypeFromJson(testJson);
    assertEquals("SYS.VARCHAR2", extractedType);

    // Test with invalid JSON
    String invalidType = AnydataConverter.extractTypeFromJson("{invalid}");
    assertEquals("PARSE_ERROR", invalidType);

    // Test with missing type field
    String noType = AnydataConverter.extractTypeFromJson("{\"value\":\"test\"}");
    assertEquals("UNKNOWN", noType);
  }

  @Test
  public void testValueExtractionFromJson() throws Exception {
    // Test utility method for extracting value from JSON
    String stringJson = "{\"type\":\"SYS.VARCHAR2\",\"value\":\"test string\",\"metadata\":{}}";
    Object extractedValue = AnydataConverter.extractValueFromJson(stringJson);
    assertEquals("test string", extractedValue);

    String numberJson = "{\"type\":\"SYS.NUMBER\",\"value\":123.45,\"metadata\":{}}";
    Object extractedNumber = AnydataConverter.extractValueFromJson(numberJson);
    assertEquals(123.45, (Double) extractedNumber, 0.001);

    String nullJson = "{\"type\":\"SYS.VARCHAR2\",\"value\":null,\"metadata\":{}}";
    Object extractedNull = AnydataConverter.extractValueFromJson(nullJson);
    assertNull(extractedNull);
  }

  @Test
  public void testTableAnalyzerAnydataDetection() {
    // Test enhanced TableAnalyzer ANYDATA detection
    TableMetadata table = new TableMetadata("TEST_SCHEMA", "TEST_TABLE");

    // Add various column types including ANYDATA
    ColumnMetadata varchar2Col = new ColumnMetadata("name", "VARCHAR2", 100, null, null, true, null);
    ColumnMetadata numberCol = new ColumnMetadata("id", "NUMBER", null, 10, 0, false, null);
    ColumnMetadata anydataCol = new ColumnMetadata("data", "ANYDATA", null, null, null, true, null);
    ColumnMetadata anotherAnydataCol = new ColumnMetadata("extra_data", "anydata", null, null, null, true, null);

    table.addColumn(varchar2Col);
    table.addColumn(numberCol);
    table.addColumn(anydataCol);
    table.addColumn(anotherAnydataCol);

    // Test ANYDATA detection
    assertTrue(TableAnalyzer.hasAnydataColumns(table), "Should detect ANYDATA columns");
    assertEquals(2, TableAnalyzer.countAnydataColumns(table), "Should count 2 ANYDATA columns");
    assertFalse(TableAnalyzer.hasOnlyPrimitiveTypes(table), "Table with ANYDATA should not be primitive-only");

    // Test table without ANYDATA
    TableMetadata simpleTable = new TableMetadata("TEST_SCHEMA", "SIMPLE_TABLE");
    simpleTable.addColumn(varchar2Col);
    simpleTable.addColumn(numberCol);

    assertFalse(TableAnalyzer.hasAnydataColumns(simpleTable), "Should not detect ANYDATA in simple table");
    assertEquals(0, TableAnalyzer.countAnydataColumns(simpleTable), "Should count 0 ANYDATA columns");
    assertTrue(TableAnalyzer.hasOnlyPrimitiveTypes(simpleTable), "Simple table should be primitive-only");
  }

  @Test
  public void testAnydataColumnAnalysisIntegration() {
    // Test integration with enhanced table analysis
    TableMetadata table = new TableMetadata("HR", "EMPLOYEE_DATA");

    // Add columns including ANYDATA
    table.addColumn(new ColumnMetadata("emp_id", "NUMBER", null, 10, 0, false, null));
    table.addColumn(new ColumnMetadata("name", "VARCHAR2", 100, null, null, false, null));
    table.addColumn(new ColumnMetadata("metadata", "ANYDATA", null, null, null, true, null));
    table.addColumn(new ColumnMetadata("config", "ANYDATA", null, null, null, true, null));

    // Create a mock Everything context for the test
    me.christianrobert.ora2postgre.global.Everything mockEverything = mock(me.christianrobert.ora2postgre.global.Everything.class);
    when(mockEverything.getObjectTypeSpecAst()).thenReturn(java.util.List.of());

    // Test enhanced analysis
    String analysis = TableAnalyzer.analyzeTableWithObjectTypes(table, mockEverything);

    assertNotNull(analysis);
    assertTrue(analysis.contains("ANYDATA"), "Analysis should mention ANYDATA");
    assertTrue(analysis.contains("JSONB"), "Analysis should mention JSONB conversion");

    log.info("Table analysis result: {}", analysis);
  }

  @Test
  public void testComplexDataTypePatterns() {
    // Test various case variations and patterns of ANYDATA
    String[] anydataVariations = {
            "ANYDATA", "anydata", "AnyData", "ANYDATA()", "SYS.ANYDATA"
    };

    for (String variation : anydataVariations) {
      TableMetadata table = new TableMetadata("TEST", "TABLE");
      table.addColumn(new ColumnMetadata("data_col", variation, null, null, null, true, null));

      // The TableAnalyzer should detect ANYDATA regardless of case
      // Note: This depends on NameNormalizer.normalizeDataType() handling these cases
      int count = TableAnalyzer.countAnydataColumns(table);
      log.info("Testing ANYDATA variation '{}': detected count = {}", variation, count);
    }
  }

  @Test
  public void testErrorJsonCreation() {
    // Test error JSON creation
    String errorJson = AnydataConverter.extractTypeFromJson("invalid json");
    assertEquals("PARSE_ERROR", errorJson);

    // This should trigger the error JSON creation path
    try {
      when(mockResultSet.getObject("test_col")).thenThrow(new RuntimeException("Test error"));
      String result = AnydataConverter.convertAnydataToJson(mockResultSet, "test_col");

      assertNotNull(result);
      assertTrue(result.contains("ERROR"));
      assertTrue(result.contains("Test error"));

    } catch (SQLException e) {
      fail("SQLException should be handled by AnydataConverter");
    }
  }

  @Test
  public void testPostgreSQLTypeMapping() {
    // Test that TypeConverter properly maps ANYDATA to JSONB
    assertEquals("jsonb", me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter.toPostgre("anydata"));
    assertEquals("jsonb", me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter.toPostgre("ANYDATA"));

    // Verify other types still work correctly
    assertEquals("text", me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter.toPostgre("dburitype"));
    assertEquals("text", me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter.toPostgre("varchar2"));
  }
}