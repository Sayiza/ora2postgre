package me.christianrobert.ora2postgre.transfer;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.transfer.strategy.ObjectTypeMappingStrategy;
import me.christianrobert.ora2postgre.transfer.strategy.StreamingCsvStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the unified strategy approach where ObjectTypeMappingStrategy
 * handles both Oracle object types AND complex data types.
 */
@DisplayName("Unified Strategy Tests")
public class UnifiedStrategyTest {

    private ObjectTypeMappingStrategy objectStrategy;
    private StreamingCsvStrategy csvStrategy;
    private Everything everything;

    @BeforeEach
    void setUp() {
        objectStrategy = new ObjectTypeMappingStrategy();
        csvStrategy = new StreamingCsvStrategy();
        everything = MockDataFactory.createEverythingWithLangdata2();
    }

    @Test
    @DisplayName("ObjectTypeMappingStrategy should handle tables with object types")
    void testHandlesObjectTypes() {
        TableMetadata langTable = MockDataFactory.createLangTableMetadata();
        
        assertTrue(objectStrategy.canHandle(langTable, everything), 
            "ObjectTypeMappingStrategy should handle tables with object types");
        
        assertFalse(csvStrategy.canHandle(langTable, everything), 
            "StreamingCsvStrategy should NOT handle tables with object types");
    }

    @Test
    @DisplayName("ObjectTypeMappingStrategy should handle tables with only primitive types when Everything is null")
    void testHandlesNullEverything() {
        TableMetadata simpleTable = MockDataFactory.createSimpleTableMetadata();
        
        assertFalse(objectStrategy.canHandle(simpleTable, null), 
            "ObjectTypeMappingStrategy should NOT handle tables when Everything is null");
        
        assertTrue(csvStrategy.canHandle(simpleTable, null), 
            "StreamingCsvStrategy should handle simple tables even when Everything is null");
    }

    @Test
    @DisplayName("StreamingCsvStrategy should handle simple tables")
    void testCsvHandlesSimpleTables() {
        TableMetadata simpleTable = MockDataFactory.createSimpleTableMetadata();
        
        assertFalse(objectStrategy.canHandle(simpleTable, everything), 
            "ObjectTypeMappingStrategy should NOT handle tables with only primitive types");
        
        assertTrue(csvStrategy.canHandle(simpleTable, everything), 
            "StreamingCsvStrategy should handle tables with only primitive types");
    }

    @Test
    @DisplayName("Strategy names should reflect their unified functionality")
    void testStrategyNames() {
        assertEquals("Unified Object Type, ANYDATA, and Complex Data Transfer", objectStrategy.getStrategyName(),
            "ObjectTypeMappingStrategy should reflect its unified capabilities");
        
        assertEquals("CSV Streaming", csvStrategy.getStrategyName(),
            "StreamingCsvStrategy should have its original name");
    }

    @Test
    @DisplayName("All table types should be handled by at least one strategy")
    void testCompleteTableCoverage() {
        // Test that every table type is handled by at least one strategy
        
        // Object type tables -> ObjectTypeMappingStrategy
        TableMetadata objectTable = MockDataFactory.createLangTableMetadata();
        assertTrue(objectStrategy.canHandle(objectTable, everything) || csvStrategy.canHandle(objectTable, everything),
            "Object type tables should be handled");
        
        // Simple tables -> StreamingCsvStrategy  
        TableMetadata simpleTable = MockDataFactory.createSimpleTableMetadata();
        assertTrue(objectStrategy.canHandle(simpleTable, everything) || csvStrategy.canHandle(simpleTable, everything),
            "Simple tables should be handled");
    }
}