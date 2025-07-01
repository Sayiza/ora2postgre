package com.sayiza.oracle2postgre.plsql.ast;

import com.sayiza.oracle2postgre.global.Everything;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

/**
 * Test for ObjectType case normalization functionality.
 */
public class ObjectTypeCaseTest {

    @Test
    public void testObjectTypeNameNormalization() {
        // Test that object type names are normalized to lowercase in PostgreSQL output
        ObjectType objectType = new ObjectType(
                "PERSON_TYPE",  // Oracle-style uppercase name
                "TEST_SCHEMA",
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                null
        );
        
        Everything data = new Everything();
        String result = objectType.toPostgreType(data);
        
        // Verify the generated CREATE TYPE statement uses lowercase
        assertTrue(result.contains("person_type"), 
            "Object type name should be normalized to lowercase");
        assertFalse(result.contains("PERSON_TYPE"), 
            "Object type should not use uppercase");
        assertFalse(result.contains("Person_type"), 
            "Object type should not use mixed case");
    }
    
    @Test
    public void testVarrayTypeNameNormalization() {
        // Test varray types also get normalized  
        DataTypeSpec dataTypeSpec = new DataTypeSpec("NUMBER", null, null, null);
        VarrayType varray = new VarrayType(10L, null, dataTypeSpec);
        ObjectType objectType = new ObjectType(
                "NUMBER_ARRAY_TYPE",
                "TEST_SCHEMA", 
                null, null, null, null,
                varray,
                null
        );
        
        Everything data = new Everything();
        String result = objectType.toPostgreType(data);
        
        // Verify domain name is lowercase
        assertTrue(result.contains("number_array_type"), 
            "Varray domain name should be normalized to lowercase");
    }
    
    @Test
    public void testNestedTableTypeNameNormalization() {
        // Test nested table types also get normalized
        DataTypeSpec dataTypeSpec = new DataTypeSpec("VARCHAR2", null, null, null);
        NestedTableType nestedTable = new NestedTableType(dataTypeSpec);
        ObjectType objectType = new ObjectType(
                "STRING_TABLE_TYPE",
                "TEST_SCHEMA",
                null, null, null, null,
                null,
                nestedTable
        );
        
        Everything data = new Everything();
        String result = objectType.toPostgreType(data);
        
        // Verify domain name is lowercase
        assertTrue(result.contains("string_table_type"), 
            "Nested table domain name should be normalized to lowercase");
    }
}