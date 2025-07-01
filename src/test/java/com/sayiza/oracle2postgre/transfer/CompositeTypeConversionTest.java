package com.sayiza.oracle2postgre.transfer;

import com.sayiza.oracle2postgre.global.Everything;
import com.sayiza.oracle2postgre.plsql.ast.ObjectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for composite type conversion in ObjectTypeMapper.
 * Verifies that Oracle objects are correctly converted to PostgreSQL tuple literals.
 */
@DisplayName("Composite Type Conversion Tests")
public class CompositeTypeConversionTest {

    private ObjectTypeMapper mapper;
    private ObjectType langdata2Type;

    @BeforeEach
    void setUp() {
        Everything everything = MockDataFactory.createEverythingWithLangdata2();
        mapper = new ObjectTypeMapper();
        langdata2Type = MockDataFactory.createLangdata2ObjectType();
    }

    @Test
    @DisplayName("Should convert Oracle object to PostgreSQL tuple literal")
    void testObjectToCompositeTypeConversion() {
        // Create a mock Oracle object
        MockDataFactory.MockOracleStruct mockStruct = (MockDataFactory.MockOracleStruct) MockDataFactory.createMockOracleStruct();
        
        // Convert to composite type
        String result = mapper.convertObjectToCompositeType(mockStruct, langdata2Type);

        // Verify the result is in tuple literal format
        assertNotNull(result, "Result should not be null");
        assertTrue(result.startsWith("("), "Should start with opening parenthesis");
        assertTrue(result.endsWith(")"), "Should end with closing parenthesis");
        
        // Should contain quoted string values
        assertTrue(result.contains("\"Hallo\""), "Should contain German text");
        assertTrue(result.contains("\"Hello\""), "Should contain English text");
        
        // Should be in the format ("Hallo","Hello")
        assertEquals("(\"Hallo\",\"Hello\")", result, "Should match expected tuple format");
    }

    @Test
    @DisplayName("Should handle null Oracle object")
    void testNullObjectConversion() {
        String result = mapper.convertObjectToCompositeType(null, langdata2Type);
        
        assertNull(result, "Null object should return null");
    }

    @Test
    @DisplayName("Should handle object with null attributes")
    void testObjectWithNullAttributes() {
        // Create mock object with null values
        MockDataFactory.MockOracleStruct mockStruct = new MockDataFactory.MockOracleStruct(
                "LANGDATA2", new Object[]{null, "Hello"}
        );
        
        String result = mapper.convertObjectToCompositeType(mockStruct, langdata2Type);

        // Should handle null values appropriately 
        assertNotNull(result, "Result should not be null");
        assertTrue(result.startsWith("("), "Should start with opening parenthesis");
        assertTrue(result.endsWith(")"), "Should end with closing parenthesis");
        
        // First value is null (empty), second is quoted
        assertTrue(result.contains(",\"Hello\""), "Should contain English text");
    }

    @Test
    @DisplayName("Should handle strings with special characters")
    void testStringEscaping() {
        // Create mock object with special characters
        MockDataFactory.MockOracleStruct mockStruct = new MockDataFactory.MockOracleStruct(
                "LANGDATA2", new Object[]{"Text with \"quotes\"", "Text with\\backslash"}
        );
        
        String result = mapper.convertObjectToCompositeType(mockStruct, langdata2Type);

        // Should properly escape quotes and backslashes
        assertNotNull(result, "Result should not be null");
        assertTrue(result.contains("\\\""), "Should escape quotes");
        assertTrue(result.contains("\\\\"), "Should escape backslashes");
    }

    @Test
    @DisplayName("Should handle numeric values correctly")
    void testNumericValues() {
        // Create a mock object type with numeric field
        ObjectType numericType = MockDataFactory.createObjectTypeWithNumericField();
        
        // Create mock object with numeric values
        MockDataFactory.MockOracleStruct mockStruct = new MockDataFactory.MockOracleStruct(
                "NUMERICTYPE", new Object[]{42, 3.14}
        );
        
        String result = mapper.convertObjectToCompositeType(mockStruct, numericType);

        // Numbers should not be quoted
        assertNotNull(result, "Result should not be null");
        assertTrue(result.contains("42"), "Should contain integer value");
        assertTrue(result.contains("3.14"), "Should contain decimal value");
        assertFalse(result.contains("\"42\""), "Numbers should not be quoted");
    }
}