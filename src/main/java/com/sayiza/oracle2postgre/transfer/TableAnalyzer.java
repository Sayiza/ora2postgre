package com.sayiza.oracle2postgre.transfer;

import com.sayiza.oracle2postgre.global.Everything;
import com.sayiza.oracle2postgre.oracledb.ColumnMetadata;
import com.sayiza.oracle2postgre.oracledb.TableMetadata;
import com.sayiza.oracle2postgre.plsql.ast.ObjectType;
import com.sayiza.oracle2postgre.transfer.strategy.TransferStrategy;
import com.sayiza.oracle2postgre.oracledb.tools.NameNormalizer;

import java.util.Set;

/**
 * Analyzes table metadata to determine the appropriate transfer strategy.
 * Categorizes tables based on their column data types and complexity.
 */
public class TableAnalyzer {
    
    // Simple data types that can be handled by CSV streaming
    private static final Set<String> SIMPLE_DATA_TYPES = Set.of(
        "VARCHAR2", "VARCHAR", "CHAR", "NVARCHAR2", "NCHAR",
        "NUMBER", "INTEGER", "INT", "DECIMAL", "NUMERIC", "FLOAT", "DOUBLE",
        "DATE", "TIMESTAMP"
    );
    
    // Complex data types that require special handling
    private static final Set<String> COMPLEX_DATA_TYPES = Set.of(
        "CLOB", "NCLOB", "BLOB", "RAW", "LONG RAW", "BFILE",
        "TIMESTAMP WITH TIME ZONE", "TIMESTAMP WITH LOCAL TIME ZONE",
        "INTERVAL YEAR TO MONTH", "INTERVAL DAY TO SECOND",
        "XMLTYPE", "ANYDATA", "ANYTYPE", "URITYPE",
        "AQ$_JMS_TEXT_MESSAGE", "SYS.AQ$_JMS_TEXT_MESSAGE",
        "AQ$_SIG_PROP", "SYS.AQ$_SIG_PROP"
    );
    
    /**
     * Determines if a table contains only simple primitive data types
     * that can be efficiently handled by CSV streaming.
     */
    public static boolean hasOnlyPrimitiveTypes(TableMetadata table) {
        if (table.getColumns() == null || table.getColumns().isEmpty()) {
            return false;
        }
        
        for (ColumnMetadata column : table.getColumns()) {
            String dataType = normalizeDataType(column.getDataType());
            
            // Check if it's a known complex type
            if (isComplexDataType(dataType)) {
                return false;
            }
            
            // Check if it's a known simple type
            if (!isSimpleDataType(dataType)) {
                // Unknown data type - treat as complex for safety
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Determines if a table has complex data types requiring special handling.
     */
    public static boolean hasComplexDataTypes(TableMetadata table) {
        if (table.getColumns() == null || table.getColumns().isEmpty()) {
            return false;
        }
        
        for (ColumnMetadata column : table.getColumns()) {
            String dataType = normalizeDataType(column.getDataType());
            if (isComplexDataType(dataType)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Determines if a table contains Oracle object types.
     * Object types require special handling with JSON/JSONB conversion.
     * 
     * @param table The table metadata to analyze
     * @param everything The context containing object type definitions
     * @return true if the table contains any object type columns
     */
    public static boolean hasObjectTypes(TableMetadata table, Everything everything) {
        if (table.getColumns() == null || table.getColumns().isEmpty()) {
            return false;
        }
        
        String schema = table.getSchema();
        
        for (ColumnMetadata column : table.getColumns()) {
            String dataType = NameNormalizer.normalizeDataType(column.getDataType());
            
            // Check if this data type is a known object type in the Everything context
            ObjectType objectTypeAst = findObjectType(everything, schema, dataType);
            if (objectTypeAst != null) {
                return true;
            }
            
            // Also check for common object type patterns
            if (isObjectTypePattern(dataType)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a table contains ANYDATA columns that require JSONB conversion.
     * ANYDATA columns need special handling to preserve type information during migration.
     * 
     * @param table The table metadata to analyze
     * @return true if the table contains any ANYDATA columns
     */
    public static boolean hasAnydataColumns(TableMetadata table) {
        if (table.getColumns() == null || table.getColumns().isEmpty()) {
            return false;
        }
        
        for (ColumnMetadata column : table.getColumns()) {
            String dataType = NameNormalizer.normalizeDataType(column.getDataType());
            if ("ANYDATA".equalsIgnoreCase(dataType)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Counts the number of ANYDATA columns in a table.
     * Useful for estimating the complexity of the migration process.
     * 
     * @param table The table metadata to analyze
     * @return The number of ANYDATA columns found
     */
    public static int countAnydataColumns(TableMetadata table) {
        if (table.getColumns() == null || table.getColumns().isEmpty()) {
            return 0;
        }
        
        int count = 0;
        for (ColumnMetadata column : table.getColumns()) {
            String dataType = NameNormalizer.normalizeDataType(column.getDataType());
            if ("ANYDATA".equalsIgnoreCase(dataType)) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Checks if a table contains any AQ$_JMS_TEXT_MESSAGE columns.
     * These columns require special JSON conversion handling.
     * 
     * @param table The table metadata to analyze
     * @return true if the table contains any AQ JMS message columns
     */
    public static boolean hasAqJmsMessageColumns(TableMetadata table) {
        if (table.getColumns() == null || table.getColumns().isEmpty()) {
            return false;
        }
        
        for (ColumnMetadata column : table.getColumns()) {
            String dataType = NameNormalizer.normalizeDataType(column.getDataType());
            if (isAqJmsMessageType(dataType)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Counts the number of AQ$_JMS_TEXT_MESSAGE columns in a table.
     * Useful for estimating the complexity of the migration process.
     * 
     * @param table The table metadata to analyze
     * @return The number of AQ JMS message columns found
     */
    public static int countAqJmsMessageColumns(TableMetadata table) {
        if (table.getColumns() == null || table.getColumns().isEmpty()) {
            return 0;
        }
        
        int count = 0;
        for (ColumnMetadata column : table.getColumns()) {
            String dataType = NameNormalizer.normalizeDataType(column.getDataType());
            if (isAqJmsMessageType(dataType)) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Checks if a table contains any AQ$_SIG_PROP columns.
     * These columns require special JSON conversion handling.
     * 
     * @param table The table metadata to analyze
     * @return true if the table contains any AQ signature property columns
     */
    public static boolean hasAqSigPropColumns(TableMetadata table) {
        if (table.getColumns() == null || table.getColumns().isEmpty()) {
            return false;
        }
        
        for (ColumnMetadata column : table.getColumns()) {
            String dataType = NameNormalizer.normalizeDataType(column.getDataType());
            if (isAqSigPropType(dataType)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Counts the number of AQ$_SIG_PROP columns in a table.
     * Useful for estimating the complexity of the migration process.
     * 
     * @param table The table metadata to analyze
     * @return The number of AQ signature property columns found
     */
    public static int countAqSigPropColumns(TableMetadata table) {
        if (table.getColumns() == null || table.getColumns().isEmpty()) {
            return 0;
        }
        
        int count = 0;
        for (ColumnMetadata column : table.getColumns()) {
            String dataType = NameNormalizer.normalizeDataType(column.getDataType());
            if (isAqSigPropType(dataType)) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Checks if a data type is an AQ JMS message type.
     * 
     * @param dataType The data type to check
     * @return true if it's an AQ JMS message type
     */
    private static boolean isAqJmsMessageType(String dataType) {
        if (dataType == null) return false;
        
        String normalized = dataType.toUpperCase().trim();
        return normalized.equals("AQ$_JMS_TEXT_MESSAGE") || 
               normalized.equals("SYS.AQ$_JMS_TEXT_MESSAGE") ||
               normalized.contains("AQ$_JMS_TEXT_MESSAGE");
    }
    
    /**
     * Checks if a data type is an AQ signature property type.
     * 
     * @param dataType The data type to check
     * @return true if it's an AQ signature property type
     */
    private static boolean isAqSigPropType(String dataType) {
        if (dataType == null) return false;
        
        String normalized = dataType.toUpperCase().trim();
        return normalized.equals("AQ$_SIG_PROP") || 
               normalized.equals("SYS.AQ$_SIG_PROP") ||
               normalized.contains("AQ$_SIG_PROP");
    }
    
    /**
     * Returns the number of object type columns in a table.
     * 
     * @param table The table metadata to analyze
     * @param everything The context containing object type definitions
     * @return Count of object type columns
     */
    public static int countObjectTypeColumns(TableMetadata table, Everything everything) {
        if (table.getColumns() == null || table.getColumns().isEmpty()) {
            return 0;
        }
        
        String schema = table.getSchema();
        int objectTypeCount = 0;
        
        for (ColumnMetadata column : table.getColumns()) {
            String dataType = NameNormalizer.normalizeDataType(column.getDataType());
            
            // Check if this data type is a known object type
            ObjectType objectTypeAst = findObjectType(everything, schema, dataType);
            if (objectTypeAst != null) {
                objectTypeCount++;
            } else if (isObjectTypePattern(dataType)) {
                objectTypeCount++;
            }
        }
        
        return objectTypeCount;
    }
    
    /**
     * Selects the appropriate transfer strategy for a given table.
     * Currently returns null for strategy selection logic to be implemented later.
     */
    public static TransferStrategy selectStrategy(TableMetadata table) {
        // Strategy selection logic will be implemented when strategies are created
        // For now, this method serves as a placeholder for the selection logic
        return null;
    }
    
    /**
     * Estimates the transfer time for a table based on row count and complexity.
     * This is a rough estimate for planning purposes.
     */
    public static long estimateTransferTimeMs(TableMetadata table, long estimatedRowCount) {
        if (estimatedRowCount == 0) {
            return 100; // Minimum time for empty tables
        }
        
        // Base transfer rate estimates (rows per second)
        long baseRateRowsPerSecond;
        
        if (hasOnlyPrimitiveTypes(table)) {
            // Simple tables can be transferred faster via CSV
            baseRateRowsPerSecond = 10000; // 10K rows/sec for simple data
        } else if (hasComplexDataTypes(table)) {
            // Complex tables require slower processing
            baseRateRowsPerSecond = 1000; // 1K rows/sec for complex data  
        } else {
            // Mixed or unknown types
            baseRateRowsPerSecond = 5000; // 5K rows/sec for mixed data
        }
        
        // Calculate estimated time with some overhead
        long estimatedSeconds = estimatedRowCount / baseRateRowsPerSecond;
        return Math.max(1000, estimatedSeconds * 1000); // Minimum 1 second
    }
    
    /**
     * Provides a summary of table characteristics for logging and debugging.
     */
    public static String analyzeTable(TableMetadata table) {
        if (table.getColumns() == null || table.getColumns().isEmpty()) {
            return String.format("%s.%s: No columns found", 
                table.getSchema(), table.getTableName());
        }
        
        int simpleColumns = 0;
        int complexColumns = 0;
        int unknownColumns = 0;
        
        for (ColumnMetadata column : table.getColumns()) {
            String dataType = normalizeDataType(column.getDataType());
            
            if (isSimpleDataType(dataType)) {
                simpleColumns++;
            } else if (isComplexDataType(dataType)) {
                complexColumns++;
            } else {
                unknownColumns++;
            }
        }
        
        String complexity;
        if (complexColumns > 0 || unknownColumns > 0) {
            complexity = "COMPLEX";
        } else {
            complexity = "SIMPLE";
        }
        
        return String.format("%s.%s: %s (%d simple, %d complex, %d unknown columns)", 
            table.getSchema(), table.getTableName(), complexity, 
            simpleColumns, complexColumns, unknownColumns);
    }
    
    /**
     * Enhanced table analysis that includes object type detection.
     * 
     * @param table The table metadata to analyze
     * @param everything The context containing object type definitions (can be null)
     * @return Analysis string with object type information
     */
    public static String analyzeTableWithObjectTypes(TableMetadata table, Everything everything) {
        String basicAnalysis = analyzeTable(table);
        
        if (everything != null) {
            int objectTypeColumns = countObjectTypeColumns(table, everything);
            int anydataColumns = countAnydataColumns(table);
            int aqJmsMessageColumns = countAqJmsMessageColumns(table);
            int aqSigPropColumns = countAqSigPropColumns(table);
            
            StringBuilder analysis = new StringBuilder(basicAnalysis);
            
            if (objectTypeColumns > 0) {
                analysis.append(String.format(" [%d object type columns]", objectTypeColumns));
            }
            
            if (anydataColumns > 0) {
                analysis.append(String.format(" [%d ANYDATA columns → JSONB]", anydataColumns));
            }
            
            if (aqJmsMessageColumns > 0) {
                analysis.append(String.format(" [%d AQ$_JMS_TEXT_MESSAGE columns → JSONB]", aqJmsMessageColumns));
            }
            
            if (aqSigPropColumns > 0) {
                analysis.append(String.format(" [%d AQ$_SIG_PROP columns → JSONB]", aqSigPropColumns));
            }
            
            return analysis.toString();
        }
        
        return basicAnalysis;
    }
    
    // Helper methods
    
    private static String normalizeDataType(String dataType) {
        if (dataType == null) {
            return "";
        }
        
        // Use NameNormalizer for consistent normalization, then remove precision info
        String normalized = NameNormalizer.normalizeDataType(dataType);
        
        // Remove precision info like NUMBER(10,2) -> NUMBER
        int parenIndex = normalized.indexOf('(');
        if (parenIndex > 0) {
            normalized = normalized.substring(0, parenIndex);
        }
        
        return normalized;
    }
    
    private static boolean isSimpleDataType(String dataType) {
        return SIMPLE_DATA_TYPES.contains(dataType);
    }
    
    private static boolean isComplexDataType(String dataType) {
        return COMPLEX_DATA_TYPES.contains(dataType) || 
               dataType.startsWith("TIMESTAMP WITH") ||
               dataType.startsWith("INTERVAL") ||
               dataType.contains("OBJECT") ||
               dataType.contains("VARRAY") ||
               dataType.contains("TABLE");
    }
    
    /**
     * Checks if a data type name follows Oracle object type patterns.
     * This is used as a fallback when the object type is not found in the Everything context.
     */
    private static boolean isObjectTypePattern(String dataType) {
        // Common patterns for user-defined object types
        return dataType.matches("[A-Z][A-Z0-9_]*") && // Uppercase identifier pattern
               !isSimpleDataType(dataType) && 
               !isComplexDataType(dataType) &&
               !dataType.startsWith("SYS_") && // Exclude system types
               !dataType.startsWith("APEX_") && // Exclude APEX types
               !dataType.startsWith("MDSYS_"); // Exclude spatial types
    }
    
    /**
     * Helper method to find an ObjectType by schema and name.
     * Uses normalized names for consistent matching.
     */
    private static ObjectType findObjectType(Everything everything, String schema, String typeName) {
        String normalizedSchema = NameNormalizer.normalizeIdentifier(schema);
        String normalizedTypeName = NameNormalizer.normalizeObjectTypeName(typeName);
        
        return everything.getObjectTypeSpecAst().stream()
            .filter(objectType -> {
                String objSchema = NameNormalizer.normalizeIdentifier(objectType.getSchema());
                String objName = NameNormalizer.normalizeObjectTypeName(objectType.getName());
                return objSchema.equals(normalizedSchema) && objName.equals(normalizedTypeName);
            })
            .findFirst()
            .orElse(null);
    }
}