package me.christianrobert.ora2postgre.oracledb.tools;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the NameNormalizer utility class.
 */
class NameNormalizerTest {

    @Test
    void testNormalizeObjectTypeName() {
        // Test quoted names
        assertEquals("MYTYPE", NameNormalizer.normalizeObjectTypeName("\"MyType\""));
        assertEquals("MYTYPE", NameNormalizer.normalizeObjectTypeName("\"MYTYPE\""));
        assertEquals("MYTYPE", NameNormalizer.normalizeObjectTypeName("\"mytype\""));
        
        // Test unquoted names
        assertEquals("MYTYPE", NameNormalizer.normalizeObjectTypeName("MyType"));
        assertEquals("MYTYPE", NameNormalizer.normalizeObjectTypeName("MYTYPE"));
        assertEquals("MYTYPE", NameNormalizer.normalizeObjectTypeName("mytype"));
        
        // Test with whitespace
        assertEquals("MYTYPE", NameNormalizer.normalizeObjectTypeName("  \"MyType\"  "));
        assertEquals("MYTYPE", NameNormalizer.normalizeObjectTypeName("  MyType  "));
        
        // Test edge cases
        assertNull(NameNormalizer.normalizeObjectTypeName(null));
        assertEquals("", NameNormalizer.normalizeObjectTypeName(""));
        assertEquals("", NameNormalizer.normalizeObjectTypeName("   "));
    }

    @Test
    void testNormalizeIdentifier() {
        // Test quoted identifiers
        assertEquals("IDENTIFIER", NameNormalizer.normalizeIdentifier("\"Identifier\""));
        assertEquals("MIXEDCASE", NameNormalizer.normalizeIdentifier("\"MixedCase\""));
        
        // Test unquoted identifiers
        assertEquals("IDENTIFIER", NameNormalizer.normalizeIdentifier("identifier"));
        assertEquals("IDENTIFIER", NameNormalizer.normalizeIdentifier("IDENTIFIER"));
        
        // Test edge cases
        assertNull(NameNormalizer.normalizeIdentifier(null));
        assertEquals("", NameNormalizer.normalizeIdentifier(""));
    }

    @Test
    void testIsQuoted() {
        // Test quoted identifiers
        assertTrue(NameNormalizer.isQuoted("\"MyType\""));
        assertTrue(NameNormalizer.isQuoted("\"IDENTIFIER\""));
        assertTrue(NameNormalizer.isQuoted("  \"Test\"  "));
        
        // Test unquoted identifiers
        assertFalse(NameNormalizer.isQuoted("MyType"));
        assertFalse(NameNormalizer.isQuoted("IDENTIFIER"));
        assertFalse(NameNormalizer.isQuoted(""));
        
        // Test malformed quotes
        assertFalse(NameNormalizer.isQuoted("\""));
        assertFalse(NameNormalizer.isQuoted("\"incomplete"));
        assertFalse(NameNormalizer.isQuoted("incomplete\""));
        
        // Test null
        assertFalse(NameNormalizer.isQuoted(null));
    }

    @Test
    void testNormalizeDataType() {
        // Test simple data types
        assertEquals("VARCHAR2", NameNormalizer.normalizeDataType("varchar2"));
        assertEquals("NUMBER", NameNormalizer.normalizeDataType("NUMBER"));
        assertEquals("MYTYPE", NameNormalizer.normalizeDataType("\"MyType\""));
        
        // Test qualified names
        assertEquals("SCHEMA.TYPE", NameNormalizer.normalizeDataType("schema.type"));
        assertEquals("SCHEMA.TYPE", NameNormalizer.normalizeDataType("\"Schema\".\"Type\""));
        assertEquals("MYSCHEMA.MYTYPE", NameNormalizer.normalizeDataType("\"MySchema\".\"MyType\""));
        
        // Test mixed qualified names
        assertEquals("SCHEMA.TYPE", NameNormalizer.normalizeDataType("SCHEMA.\"type\""));
        assertEquals("SCHEMA.TYPE", NameNormalizer.normalizeDataType("\"schema\".TYPE"));
        
        // Test edge cases
        assertNull(NameNormalizer.normalizeDataType(null));
        assertEquals("", NameNormalizer.normalizeDataType(""));
    }

    @Test
    void testRealWorldScenarios() {
        // Test typical Oracle object type scenarios
        assertEquals("LANGDATA2", NameNormalizer.normalizeObjectTypeName("\"langdata2\""));
        assertEquals("PERSON_TYPE", NameNormalizer.normalizeObjectTypeName("person_type"));
        assertEquals("ADDRESS_OBJ", NameNormalizer.normalizeObjectTypeName("\"Address_Obj\""));
        
        // Test schema-qualified scenarios
        assertEquals("MYSCHEMA.LANGDATA2", NameNormalizer.normalizeDataType("MYSCHEMA.\"langdata2\""));
        assertEquals("TESTSCHEMA.PERSON_TYPE", NameNormalizer.normalizeDataType("\"TestSchema\".person_type"));
    }
}