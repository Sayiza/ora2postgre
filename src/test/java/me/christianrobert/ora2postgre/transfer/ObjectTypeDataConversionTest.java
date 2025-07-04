package me.christianrobert.ora2postgre.transfer;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.plsql.ast.ObjectType;
import me.christianrobert.ora2postgre.transfer.strategy.ObjectTypeMappingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for object type data conversion functionality.
 * Tests the langtable/langdata2 example:
 * 
 * Oracle:
 * CREATE TABLE langtable (nr NUMBER, text VARCHAR2(300), langy langdata2)
 * CREATE TYPE langdata2 AS OBJECT (de VARCHAR2(4000), en VARCHAR2(4000))
 * 
 * PostgreSQL:
 * CREATE TYPE langdata2 AS (de text, en text)
 * CREATE TABLE USER_ROBERT.langtable (nr numeric, text text, langy langdata2)
 */
@DisplayName("Object Type Data Conversion Tests")
public class ObjectTypeDataConversionTest {

    private Everything everything;
    private TableMetadata langTableMetadata;
    private ObjectType langdata2ObjectType;

    @BeforeEach
    void setUp() {
        // Initialize test data
        everything = MockDataFactory.createEverythingWithLangdata2();
        langTableMetadata = MockDataFactory.createLangTableMetadata();
        langdata2ObjectType = MockDataFactory.createLangdata2ObjectType();
    }

    @Test
    @DisplayName("Should detect table has object types")
    void testObjectTypeDetection() {
        // Test that we can detect the langdata2 object type in the langtable
        // This test will verify our object type detection logic
        // Currently this method doesn't exist, so this test will guide our implementation
        boolean hasObjectTypes = TableAnalyzer.hasObjectTypes(langTableMetadata, everything);
        
        assertTrue(hasObjectTypes, "TableAnalyzer should detect langdata2 object type in langtable");
    }

    @Test
    @DisplayName("Should not detect object types in simple table")
    void testSimpleTableDetection() {
        TableMetadata simpleTable = MockDataFactory.createSimpleTableMetadata();
        
        boolean hasObjectTypes = TableAnalyzer.hasObjectTypes(simpleTable, everything);
        
        assertFalse(hasObjectTypes, "Simple table should not be detected as having object types");
    }

    @Test
    @DisplayName("Should generate PostgreSQL composite type for langdata2")
    void testObjectTypePostgreSQLGeneration() {
        // Test that langdata2 generates correct PostgreSQL composite type
        String postgresType = langdata2ObjectType.toPostgreType(everything);
        
        // Verify the generated PostgreSQL type definition
        assertNotNull(postgresType, "PostgreSQL type generation should not be null");
        assertTrue(postgresType.contains("CREATE TYPE"), "Should generate CREATE TYPE statement");
        assertTrue(postgresType.contains("langdata2"), "Should contain the type name");
        assertTrue(postgresType.contains("de"), "Should contain 'de' field");
        assertTrue(postgresType.contains("en"), "Should contain 'en' field");
        assertTrue(postgresType.contains("text"), "Should use 'text' type for varchar fields");
        
        // Verify lowercase normalization
        assertFalse(postgresType.contains("LANGDATA2"), "Type name should be lowercase");
    }

    @Test
    @DisplayName("Should convert Oracle object to composite type")
    void testOracleObjectToCompositeTypeConversion() throws Exception {
        // Test conversion of Oracle langdata2 object to PostgreSQL composite type
        ResultSet mockResultSet = MockDataFactory.createMockResultSetWithObjectData();
        
        ObjectTypeMapper mapper = new ObjectTypeMapper();
        
        // Simulate extracting object data from ResultSet
        mockResultSet.next();
        Object oracleObject = mockResultSet.getObject("LANGY");
        
        // Convert to composite type constructor
        String compositeResult = mapper.convertObjectToCompositeType(oracleObject, langdata2ObjectType);
        
        assertEquals("(\"Hallo\",\"Hello\")", compositeResult,
            "Oracle object should convert to expected tuple literal format");
    }

    @Test
    @DisplayName("Should handle NULL object values")
    void testNullObjectHandling() throws Exception {
        ResultSet mockResultSet = MockDataFactory.createMockResultSetWithNullObject();
        ObjectTypeMapper mapper = new ObjectTypeMapper();
        
        mockResultSet.next();
        Object oracleObject = mockResultSet.getObject("LANGY");
        
        String compositeResult = mapper.convertObjectToCompositeType(oracleObject, langdata2ObjectType);
        
        assertNull(compositeResult, "NULL Oracle object should convert to null");
    }

    @Test
    @DisplayName("Should generate correct PostgreSQL INSERT with object data")
    void testPostgreSQLInsertGeneration() throws Exception {
        // Test that ObjectTypeMapper can generate composite type constructor syntax
        ObjectTypeMapper mapper = new ObjectTypeMapper();
        
        ResultSet mockResultSet = MockDataFactory.createMockResultSetWithObjectData();
        mockResultSet.next();
        Object oracleObject = mockResultSet.getObject("LANGY");
        
        // Test conversion to tuple literal
        String compositeTypeValue = mapper.convertObjectToCompositeType(oracleObject, langdata2ObjectType);
        
        // Should generate tuple literal syntax
        assertNotNull(compositeTypeValue, "Composite type generation should not be null");
        assertTrue(compositeTypeValue.startsWith("("), "Should start with opening parenthesis");
        assertTrue(compositeTypeValue.endsWith(")"), "Should end with closing parenthesis");
        assertTrue(compositeTypeValue.contains("\"Hallo\""), "Should contain de field value");
        assertTrue(compositeTypeValue.contains("\"Hello\""), "Should contain en field value");
        
        String expectedValue = "(\"Hallo\",\"Hello\")";
        assertEquals(expectedValue, compositeTypeValue, "Should generate expected tuple literal syntax");
    }

    @Test
    @DisplayName("Should select ObjectTypeMappingStrategy for object type tables")
    void testStrategySelection() {
        // Test that DataTransferService selects the correct strategy
        DataTransferService service = new DataTransferService();
        
        // This will test our future ObjectTypeMappingStrategy
        // For now, we verify the strategy selection logic
        
        // Create a mock strategy to test selection logic
        // In full implementation, this would verify ObjectTypeMappingStrategy is selected
        assertDoesNotThrow(() -> {
            // Strategy selection should not fail
            TableAnalyzer.analyzeTable(langTableMetadata);
        }, "Strategy selection should handle object type tables");
    }

    @Test
    @DisplayName("Should create ObjectTypeMappingStrategy correctly")
    void testObjectTypeMappingStrategyCreation() {
        // Test that we can create and configure an ObjectTypeMappingStrategy
        ObjectTypeMappingStrategy strategy = new ObjectTypeMappingStrategy();
        
        assertNotNull(strategy, "ObjectTypeMappingStrategy should be created");
        assertTrue(strategy.canHandle(langTableMetadata, everything), 
            "Strategy should handle tables with object types");
        assertFalse(strategy.canHandle(MockDataFactory.createSimpleTableMetadata(), everything),
            "Strategy should not handle simple tables");
        assertEquals("Unified Object Type, ANYDATA, and Complex Data Transfer", strategy.getStrategyName(),
            "Strategy should have correct name");
    }

    @Test
    @DisplayName("Should integrate with existing transfer pipeline")
    void testTransferPipelineIntegration() {
        // Test that object type conversion integrates with existing DataTransferService
        DataTransferService service = new DataTransferService(true); // enable fallback
        
        // This test verifies the integration points are working
        assertDoesNotThrow(() -> {
            // Service should be able to analyze table with object types
            String analysis = TableAnalyzer.analyzeTable(langTableMetadata);
            assertNotNull(analysis, "Table analysis should work with object types");
        }, "Integration with existing pipeline should work");
    }

    @Test
    @DisplayName("Should handle complex nested object structures")
    void testComplexObjectStructures() {
        // Test for future enhancement: nested objects, arrays, etc.
        // For now, just verify basic structure handling
        
        ObjectType complexType = langdata2ObjectType;
        assertNotNull(complexType.getVariables(), "Object type should have variables");
        assertEquals(2, complexType.getVariables().size(), "langdata2 should have 2 fields");
        
        // Verify field names and types
        assertEquals("de", complexType.getVariables().get(0).getName());
        assertEquals("en", complexType.getVariables().get(1).getName());
    }
}