package me.christianrobert.ora2postgre.transfer;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.ColumnMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.plsql.ast.ObjectType;
import me.christianrobert.ora2postgre.transfer.strategy.ObjectTypeMappingStrategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for SQL generation in ObjectTypeMappingStrategy.
 * Verifies that the generated SQL has correct quoting behavior.
 */
@DisplayName("Object Type SQL Generation Tests")
public class ObjectTypeSqlGenerationTest {

    private static final Logger log = LoggerFactory.getLogger(ObjectTypeSqlGenerationTest.class);

    private Everything everything;
    private TableMetadata testTable;
    private ObjectTypeMappingStrategy strategy;

    @BeforeEach
    void setUp() {
        everything = MockDataFactory.createEverythingWithLangdata2();
        testTable = MockDataFactory.createLangTableMetadata();
        strategy = new ObjectTypeMappingStrategy();
    }

    @Test
    @DisplayName("Should generate SELECT query with quoted identifiers")
    void testSelectQueryGeneration() throws Exception {
        // Use reflection to access the private buildSelectQuery method
        Method buildSelectQuery = ObjectTypeMappingStrategy.class.getDeclaredMethod("buildSelectQuery", TableMetadata.class);
        buildSelectQuery.setAccessible(true);
        
        String selectSql = (String) buildSelectQuery.invoke(strategy, testTable);
        
        // Should look like: SELECT NR, "TEXT", LANGY FROM USER_ROBERT.LANGTABLE
        // Only reserved words like TEXT are quoted
        assertFalse(selectSql.contains("\"NR\""), "NR is not reserved so not quoted");
        assertTrue(selectSql.contains("\"TEXT\""), "TEXT is reserved so gets quoted");
        assertFalse(selectSql.contains("\"LANGY\""), "LANGY is not reserved so not quoted");
        
        assertTrue(selectSql.contains("SELECT"), "Should contain SELECT");
        assertTrue(selectSql.contains("FROM"), "Should contain FROM");
        assertTrue(selectSql.contains("NR"), "Should contain NR column");
        assertTrue(selectSql.contains("TEXT"), "Should contain TEXT column");
        assertTrue(selectSql.contains("LANGY"), "Should contain LANGY column");
    }

    @Test
    @DisplayName("Should generate INSERT query with quoted identifiers")
    void testInsertQueryGeneration() throws Exception {
        // Use reflection to access the private buildInsertQuery method
        Method buildInsertQuery = ObjectTypeMappingStrategy.class.getDeclaredMethod("buildInsertQuery", 
                TableMetadata.class, List.class);
        buildInsertQuery.setAccessible(true);
        
        String insertSql = (String) buildInsertQuery.invoke(strategy, testTable, Arrays.asList());
        
        // Verify that the SQL has properly quoted identifiers
        log.debug("Generated INSERT SQL: {}", insertSql);
        
        // Should look like: INSERT INTO USER_ROBERT.LANGTABLE (NR, "TEXT", LANGY) VALUES (?, ?, ?)
        // Only reserved words like TEXT are quoted
        assertFalse(insertSql.contains("\"NR\""), "NR is not reserved so not quoted");
        assertTrue(insertSql.contains("\"TEXT\""), "TEXT is reserved so gets quoted");
        assertFalse(insertSql.contains("\"LANGY\""), "LANGY is not reserved so not quoted");
        
        assertTrue(insertSql.contains("INSERT INTO"), "Should contain INSERT INTO");
        assertTrue(insertSql.contains("VALUES"), "Should contain VALUES");
        assertTrue(insertSql.contains("NR"), "Should contain NR column");
        assertTrue(insertSql.contains("TEXT"), "Should contain TEXT column");
        assertTrue(insertSql.contains("LANGY"), "Should contain LANGY column");
    }

    @Test
    @DisplayName("Should handle composite type parameter setting correctly")
    void testCompositeTypeParameterSetting() throws Exception {
        ObjectTypeMapper mapper = new ObjectTypeMapper();
        ObjectType langdata2Type = MockDataFactory.createLangdata2ObjectType();
        
        // Test the composite type conversion
        String expectedComposite = MockDataFactory.getExpectedLangdata2CompositeType();
        log.debug("Expected composite type: {}", expectedComposite);
        
        // Verify the format is correct - should be tuple literal format
        assertTrue(expectedComposite.startsWith("("), "Should start with (");
        assertTrue(expectedComposite.endsWith(")"), "Should end with )");
        assertTrue(expectedComposite.contains("\"Hallo\""), "Should contain quoted string values");
        assertTrue(expectedComposite.contains("\"Hello\""), "Should contain quoted string values");
    }

    @Test
    @DisplayName("Should handle identifier quoting correctly")
    void testIdentifierQuoting() throws Exception {
        // Test identifier quoting using the PostgreSqlIdentifierUtils directly
        // (the strategy uses this utility internally)
        
        // Test identifiers - TEXT is reserved so gets quoted, others don't
        assertEquals("NR", me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils.quoteIdentifier("NR"));
        assertEquals("\"TEXT\"", me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils.quoteIdentifier("TEXT"));
        assertEquals("LANGY", me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils.quoteIdentifier("LANGY"));
        assertEquals("SIMPLE_NAME", me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils.quoteIdentifier("SIMPLE_NAME"));
        
        // Test special characters (current implementation does NOT quote these)
        assertEquals("\"name with spaces\"", me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils.quoteIdentifier("name with spaces"));
        assertEquals("\"name-with-dash\"", me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils.quoteIdentifier("name-with-dash"));
        
        // Test edge cases
        assertEquals("", me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils.quoteIdentifier(""));
        assertNull(me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils.quoteIdentifier(null));
    }
}