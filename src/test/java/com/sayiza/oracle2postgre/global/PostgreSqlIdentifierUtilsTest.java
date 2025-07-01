package com.sayiza.oracle2postgre.global;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PostgreSqlIdentifierUtils to verify comprehensive PostgreSQL identifier handling.
 * This test validates the unified functionality that replaced multiple scattered implementations.
 */
public class PostgreSqlIdentifierUtilsTest {
    
    @Test
    public void testReservedWordDetection() {
        // Test common PostgreSQL reserved words
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("select"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("SELECT"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("from"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("where"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("table"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("index"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("constraint"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("primary"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("key"));
        
        // Test the "end" keyword that was missing from previous implementations
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("end"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("END"));
        
        // Test other comprehensive keywords
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("union"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("order"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("group"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("having"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("window"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("with"));
        
        // Test non-reserved words
        assertFalse(PostgreSqlIdentifierUtils.isPostgresReservedWord("employee"));
        assertFalse(PostgreSqlIdentifierUtils.isPostgresReservedWord("customer_id"));
        assertFalse(PostgreSqlIdentifierUtils.isPostgresReservedWord("my_table"));
        
        // Test edge cases
        assertFalse(PostgreSqlIdentifierUtils.isPostgresReservedWord(""));
        assertFalse(PostgreSqlIdentifierUtils.isPostgresReservedWord(null));
        assertFalse(PostgreSqlIdentifierUtils.isPostgresReservedWord("   "));
    }
    
    @Test
    public void testIdentifierQuoting() {
        // Test reserved words - should be quoted
        assertEquals("\"select\"", PostgreSqlIdentifierUtils.quoteIdentifier("select"));
        assertEquals("\"from\"", PostgreSqlIdentifierUtils.quoteIdentifier("from"));
        assertEquals("\"end\"", PostgreSqlIdentifierUtils.quoteIdentifier("end"));
        assertEquals("\"table\"", PostgreSqlIdentifierUtils.quoteIdentifier("table"));
        
        // Test mixed case - should NOT be quoted (current convention)
        assertEquals("MyTable", PostgreSqlIdentifierUtils.quoteIdentifier("MyTable"));
        assertEquals("customerId", PostgreSqlIdentifierUtils.quoteIdentifier("customerId"));
        
        // Test special characters - current implementation does NOT quote these
        assertEquals("my-table", PostgreSqlIdentifierUtils.quoteIdentifier("my-table"));
        assertEquals("my table", PostgreSqlIdentifierUtils.quoteIdentifier("my table"));
        assertEquals("my.table", PostgreSqlIdentifierUtils.quoteIdentifier("my.table"));
        assertEquals("my@table", PostgreSqlIdentifierUtils.quoteIdentifier("my@table"));
        
        // Test starting with digit - should be quoted
        assertEquals("\"123table\"", PostgreSqlIdentifierUtils.quoteIdentifier("123table"));
        assertEquals("\"9abc\"", PostgreSqlIdentifierUtils.quoteIdentifier("9abc"));
        
        // Test normal identifiers - should NOT be quoted
        assertEquals("employee", PostgreSqlIdentifierUtils.quoteIdentifier("employee"));
        assertEquals("customer_id", PostgreSqlIdentifierUtils.quoteIdentifier("customer_id"));
        assertEquals("my_table", PostgreSqlIdentifierUtils.quoteIdentifier("my_table"));
        assertEquals("abc123", PostgreSqlIdentifierUtils.quoteIdentifier("abc123"));
        
        // Test all lowercase - should NOT be quoted (normal PostgreSQL style)
        assertEquals("table_name", PostgreSqlIdentifierUtils.quoteIdentifier("table_name"));
        assertEquals("column_name", PostgreSqlIdentifierUtils.quoteIdentifier("column_name"));
        
        // Test edge cases
        assertEquals("", PostgreSqlIdentifierUtils.quoteIdentifier(""));
        assertEquals(null, PostgreSqlIdentifierUtils.quoteIdentifier(null));
        assertEquals("", PostgreSqlIdentifierUtils.quoteIdentifier("   ").trim());
    }
    
    @Test
    public void testSchemaQualifiedIdentifiers() {
        // Test normal schema.table
        assertEquals("hr.employees", 
            PostgreSqlIdentifierUtils.quoteSchemaQualifiedIdentifier("hr", "employees"));
        
        // Test with reserved words
        assertEquals("\"schema\".\"table\"", 
            PostgreSqlIdentifierUtils.quoteSchemaQualifiedIdentifier("schema", "table"));
        
        // Test mixed case - should NOT be quoted (current convention)
        assertEquals("MySchema.MyTable", 
            PostgreSqlIdentifierUtils.quoteSchemaQualifiedIdentifier("MySchema", "MyTable"));
        
        // Test one reserved, one normal
        assertEquals("\"from\".employees", 
            PostgreSqlIdentifierUtils.quoteSchemaQualifiedIdentifier("from", "employees"));
        
        // Test null/empty schema
        assertEquals("employees", 
            PostgreSqlIdentifierUtils.quoteSchemaQualifiedIdentifier(null, "employees"));
        assertEquals("employees", 
            PostgreSqlIdentifierUtils.quoteSchemaQualifiedIdentifier("", "employees"));
        assertEquals("employees", 
            PostgreSqlIdentifierUtils.quoteSchemaQualifiedIdentifier("   ", "employees"));
    }
    
    @Test
    public void testQuotedIdentifierDetection() {
        // Test quoted identifiers
        assertTrue(PostgreSqlIdentifierUtils.isQuoted("\"table\""));
        assertTrue(PostgreSqlIdentifierUtils.isQuoted("\"MyTable\""));
        assertTrue(PostgreSqlIdentifierUtils.isQuoted("\"my table\""));
        
        // Test unquoted identifiers
        assertFalse(PostgreSqlIdentifierUtils.isQuoted("table"));
        assertFalse(PostgreSqlIdentifierUtils.isQuoted("MyTable"));
        assertFalse(PostgreSqlIdentifierUtils.isQuoted("my_table"));
        
        // Test edge cases
        assertFalse(PostgreSqlIdentifierUtils.isQuoted("\""));
        assertFalse(PostgreSqlIdentifierUtils.isQuoted(""));
        assertFalse(PostgreSqlIdentifierUtils.isQuoted(null));
        assertFalse(PostgreSqlIdentifierUtils.isQuoted("\"table"));
        assertFalse(PostgreSqlIdentifierUtils.isQuoted("table\""));
    }
    
    @Test
    public void testUnquoteIdentifier() {
        // Test removing quotes
        assertEquals("table", PostgreSqlIdentifierUtils.unquoteIdentifier("\"table\""));
        assertEquals("MyTable", PostgreSqlIdentifierUtils.unquoteIdentifier("\"MyTable\""));
        assertEquals("my table", PostgreSqlIdentifierUtils.unquoteIdentifier("\"my table\""));
        
        // Test already unquoted
        assertEquals("table", PostgreSqlIdentifierUtils.unquoteIdentifier("table"));
        assertEquals("MyTable", PostgreSqlIdentifierUtils.unquoteIdentifier("MyTable"));
        
        // Test edge cases
        assertEquals("", PostgreSqlIdentifierUtils.unquoteIdentifier(""));
        assertEquals(null, PostgreSqlIdentifierUtils.unquoteIdentifier(null));
        assertEquals("\"table", PostgreSqlIdentifierUtils.unquoteIdentifier("\"table"));
        assertEquals("table\"", PostgreSqlIdentifierUtils.unquoteIdentifier("table\""));
    }
    
    @Test
    public void testReservedWordCount() {
        // Verify we have a comprehensive list
        int count = PostgreSqlIdentifierUtils.getReservedWordCount();
        assertTrue(count > 200, "Should have over 200 PostgreSQL reserved words, but found: " + count);
        
        // Verify the set is not null and contains expected words
        assertNotNull(PostgreSqlIdentifierUtils.getReservedWords());
        assertTrue(PostgreSqlIdentifierUtils.getReservedWords().contains("end"));
        assertTrue(PostgreSqlIdentifierUtils.getReservedWords().contains("select"));
        assertTrue(PostgreSqlIdentifierUtils.getReservedWords().contains("table"));
    }
    
    @Test
    public void testPreviouslyMissingKeywords() {
        // Test keywords that were missing from previous implementations
        // This addresses the user's specific concern about "end"
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("end"));
        
        // Test other commonly missed keywords
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("window"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("with"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("recursive"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("partition"));
        assertTrue(PostgreSqlIdentifierUtils.isPostgresReservedWord("filter"));
        
        // Verify these get quoted
        assertEquals("\"end\"", PostgreSqlIdentifierUtils.quoteIdentifier("end"));
        assertEquals("\"window\"", PostgreSqlIdentifierUtils.quoteIdentifier("window"));
        assertEquals("\"with\"", PostgreSqlIdentifierUtils.quoteIdentifier("with"));
    }
    
    @Test
    public void testOracleToPostgresCommonCases() {
        // Test common Oracle identifier patterns that caused issues
        
        // Oracle allows "END" as column name, PostgreSQL requires quoting
        assertEquals("\"end\"", PostgreSqlIdentifierUtils.quoteIdentifier("end"));
        assertEquals("\"END\"", PostgreSqlIdentifierUtils.quoteIdentifier("END"));
        
        // Oracle mixed case column names - should NOT be quoted (current convention)
        assertEquals("CustomerName", PostgreSqlIdentifierUtils.quoteIdentifier("CustomerName"));
        assertEquals("OrderDate", PostgreSqlIdentifierUtils.quoteIdentifier("OrderDate"));
        
        // Oracle allows spaces in quoted identifiers - current implementation does NOT quote spaces
        assertEquals("Customer Name", PostgreSqlIdentifierUtils.quoteIdentifier("Customer Name"));
        assertEquals("Order Date", PostgreSqlIdentifierUtils.quoteIdentifier("Order Date"));
        
        // Normal Oracle patterns that don't need quoting in PostgreSQL
        assertEquals("customer_name", PostgreSqlIdentifierUtils.quoteIdentifier("customer_name"));
        assertEquals("order_date", PostgreSqlIdentifierUtils.quoteIdentifier("order_date"));
        assertEquals("employee_id", PostgreSqlIdentifierUtils.quoteIdentifier("employee_id"));
    }
}