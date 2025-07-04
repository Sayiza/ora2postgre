package me.christianrobert.ora2postgre.transfer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

// Oracle-specific imports for enhanced ANYDATA support
import oracle.sql.Datum;
import oracle.sql.STRUCT;
import oracle.sql.NUMBER;
import oracle.sql.CHAR;
import oracle.sql.TypeDescriptor;

// TODO the custom types are not working yet

/**
 * Converter for Oracle ANYDATA type to PostgreSQL JSONB format.
 * 
 * Handles the extraction of type information and data from Oracle ANYDATA columns
 * and converts them to a structured JSON format suitable for PostgreSQL JSONB storage.
 * 
 * The resulting JSON structure preserves both the original data type information
 * and the actual data value, enabling type-aware operations in PostgreSQL.
 * 
 * JSON Output Format:
 * {
 *   "type": "oracle_type_name",
 *   "value": actual_data,
 *   "metadata": {
 *     "original_type": "SYS.ANYDATA",
 *     "extracted_type": "specific_type"
 *   }
 * }
 */
public class AnydataConverter {
    
    private static final Logger log = LoggerFactory.getLogger(AnydataConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Converts Oracle ANYDATA value to JSONB-compatible JSON string.
     * 
     * Enhanced version that uses oracle.sql.ANYDATA for precise type detection
     * and better handling of user-defined object types.
     * 
     * @param resultSet The ResultSet containing the ANYDATA column
     * @param columnName The name of the ANYDATA column
     * @return JSON string representation suitable for PostgreSQL JSONB, or null if the ANYDATA is null
     * @throws SQLException if there's an error accessing the ANYDATA or converting it
     */
    public static String convertAnydataToJson(ResultSet resultSet, String columnName) throws SQLException {
        try {
            // Get the ANYDATA object from the result set
            Object anydataObject = resultSet.getObject(columnName);
            
            if (anydataObject == null) {
                log.debug("ANYDATA column '{}' is null", columnName);
                return null;
            }
            
            log.debug("Converting ANYDATA column '{}' to JSON, object type: {}", 
                    columnName, anydataObject.getClass().getName());
            
            // Try Oracle-specific ANYDATA handling first
            if (anydataObject instanceof oracle.sql.ANYDATA) {
                log.debug("Using Oracle-specific ANYDATA conversion for column '{}'", columnName);
                return convertOracleAnydataToJson((oracle.sql.ANYDATA) anydataObject, columnName);
            } else {
                // Fallback to generic JDBC approach
                log.debug("Using generic JDBC conversion for column '{}' (type: {})", 
                        columnName, anydataObject.getClass().getSimpleName());
                return convertGenericObjectToJson(anydataObject, columnName);
            }
            
        } catch (Exception e) {
            log.error("Error converting ANYDATA column '{}' to JSON: {}", columnName, e.getMessage(), e);
            // Return error indicator in JSON format
            return createErrorJson("CONVERSION_ERROR", e.getMessage());
        }
    }
    
    /**
     * Converts Oracle-specific ANYDATA object to JSON using oracle.sql.ANYDATA methods.
     * This provides precise type detection and proper handling of user-defined object types.
     * 
     * @param anydata The oracle.sql.ANYDATA object
     * @param columnName The name of the column for logging purposes
     * @return JSON string representation with accurate type information
     * @throws SQLException if there's an error accessing the ANYDATA
     */
    private static String convertOracleAnydataToJson(oracle.sql.ANYDATA anydata, String columnName) throws SQLException {
        try {
            ObjectNode jsonNode = objectMapper.createObjectNode();
            ObjectNode metadata = objectMapper.createObjectNode();
            
            metadata.put("original_type", "SYS.ANYDATA");
            metadata.put("column_name", columnName);
            metadata.put("conversion_method", "oracle_specific");
            
            // Check if ANYDATA is null
            if (anydata.isNull()) {
                jsonNode.put("type", "NULL");
                jsonNode.putNull("value");
                jsonNode.set("metadata", metadata);
                return objectMapper.writeValueAsString(jsonNode);
            }
            
            // Get type descriptor for precise type information
            TypeDescriptor typeDescriptor = anydata.getTypeDescriptor();
            String actualTypeName = getActualTypeName(typeDescriptor);
            
            jsonNode.put("type", actualTypeName);
            metadata.put("extracted_type", actualTypeName);
            metadata.put("type_code", typeDescriptor.getTypeCode());
            
            log.debug("ANYDATA type detected: {} (code: {})", actualTypeName, typeDescriptor.getTypeCode());
            
            // Extract the actual value using appropriate method
            Datum datum = anydata.accessDatum();
            convertOracleDatumToJsonValue(jsonNode, datum, typeDescriptor, actualTypeName);
            
            jsonNode.set("metadata", metadata);
            
            String result = objectMapper.writeValueAsString(jsonNode);
            log.debug("Oracle ANYDATA converted to JSON: {}", result);
            return result;
            
        } catch (Exception e) {
            log.error("Error in Oracle-specific ANYDATA conversion for column '{}': {}", columnName, e.getMessage(), e);
            return createErrorJson("ORACLE_CONVERSION_ERROR", e.getMessage());
        }
    }
    
    /**
     * Gets the actual Oracle type name from TypeDescriptor.
     * Handles both built-in and user-defined types.
     */
    private static String getActualTypeName(TypeDescriptor typeDescriptor) throws SQLException {
        try {
            // For user-defined types, getName() returns the schema-qualified name
            String typeName = typeDescriptor.getName();
            if (typeName != null && !typeName.isEmpty()) {
                return typeName;
            }
            
            // For built-in types, map type codes to names
            int typeCode = typeDescriptor.getTypeCode();
            return mapTypeCodeToName(typeCode);
            
        } catch (Exception e) {
            log.warn("Error getting type name from TypeDescriptor: {}", e.getMessage());
            return "SYS.UNKNOWN";
        }
    }
    
    /**
     * Maps Oracle type codes to standard type names for built-in types.
     */
    private static String mapTypeCodeToName(int typeCode) {
        switch (typeCode) {
            case TypeDescriptor.TYPECODE_VARCHAR:
            case TypeDescriptor.TYPECODE_VARCHAR2:
                return "SYS.VARCHAR2";
            case TypeDescriptor.TYPECODE_CHAR:
                return "SYS.CHAR";
            case TypeDescriptor.TYPECODE_NUMBER:
                return "SYS.NUMBER";
            case TypeDescriptor.TYPECODE_DATE:
                return "SYS.DATE";
            case TypeDescriptor.TYPECODE_TIMESTAMP:
                return "SYS.TIMESTAMP";
            case TypeDescriptor.TYPECODE_TIMESTAMP_TZ:
                return "SYS.TIMESTAMP_WITH_TIMEZONE";
            case TypeDescriptor.TYPECODE_TIMESTAMP_LTZ:
                return "SYS.TIMESTAMP_WITH_LOCAL_TIMEZONE";
            case TypeDescriptor.TYPECODE_CLOB:
                return "SYS.CLOB";
            case TypeDescriptor.TYPECODE_BLOB:
                return "SYS.BLOB";
            case TypeDescriptor.TYPECODE_OBJECT:
                return "SYS.OBJECT";
            case TypeDescriptor.TYPECODE_VARRAY:
                return "SYS.VARRAY";
            case TypeDescriptor.TYPECODE_TABLE:
                return "SYS.TABLE";
            default:
                log.warn("Unknown Oracle type code: {}", typeCode);
                return "SYS.TYPECODE_" + typeCode;
        }
    }
    
    /**
     * Converts Oracle Datum to JSON value based on the type descriptor.
     * Handles user-defined object types by decomposing STRUCT attributes.
     */
    private static void convertOracleDatumToJsonValue(ObjectNode jsonNode, Datum datum, 
                                                    TypeDescriptor typeDescriptor, String typeName) throws SQLException {
        try {
            if (datum == null) {
                jsonNode.putNull("value");
                return;
            }
            
            int typeCode = typeDescriptor.getTypeCode();
            
            switch (typeCode) {
                case TypeDescriptor.TYPECODE_VARCHAR:
                case TypeDescriptor.TYPECODE_VARCHAR2:
                case TypeDescriptor.TYPECODE_CHAR:
                    if (datum instanceof CHAR) {
                        jsonNode.put("value", ((CHAR) datum).stringValue());
                    } else {
                        jsonNode.put("value", datum.stringValue());
                    }
                    break;
                    
                case TypeDescriptor.TYPECODE_NUMBER:
                    if (datum instanceof NUMBER) {
                        NUMBER numberDatum = (NUMBER) datum;
                        // Try to preserve number precision
                        if (numberDatum.isInt()) {
                            jsonNode.put("value", numberDatum.intValue());
                        } else {
                            jsonNode.put("value", numberDatum.bigDecimalValue());
                        }
                    } else {
                        jsonNode.put("value", datum.stringValue());
                    }
                    break;
                    
                case TypeDescriptor.TYPECODE_DATE:
                case TypeDescriptor.TYPECODE_TIMESTAMP:
                case TypeDescriptor.TYPECODE_TIMESTAMP_TZ:
                case TypeDescriptor.TYPECODE_TIMESTAMP_LTZ:
                    jsonNode.put("value", datum.stringValue());
                    break;
                    
                case TypeDescriptor.TYPECODE_OBJECT:
                    // Handle user-defined object types
                    if (datum instanceof STRUCT) {
                        convertOracleStructToJson(jsonNode, (STRUCT) datum, typeDescriptor);
                    } else {
                        jsonNode.put("value", datum.stringValue());
                    }
                    break;
                    
                default:
                    // Fallback for other types
                    jsonNode.put("value", datum.stringValue());
                    log.debug("Using string fallback for type code: {}", typeCode);
                    break;
            }
            
        } catch (Exception e) {
            log.warn("Error converting Oracle Datum to JSON value, using string fallback: {}", e.getMessage());
            jsonNode.put("value", datum != null ? datum.toString() : null);
        }
    }
    
    /**
     * Converts Oracle STRUCT (user-defined object type) to detailed JSON representation.
     * This decomposes the object into its constituent attributes.
     */
    private static void convertOracleStructToJson(ObjectNode jsonNode, STRUCT struct, 
                                                TypeDescriptor typeDescriptor) throws SQLException {
        try {
            ObjectNode structJson = objectMapper.createObjectNode();
            
            // Get the attributes of the user-defined object
            Object[] attributes = struct.getAttributes();
            
            // Try to get attribute names from type descriptor
            if (typeDescriptor != null) {
                try {
                    // Note: Getting attribute names from TypeDescriptor requires connection context
                    // For now, we'll use indexed attribute names
                    for (int i = 0; i < attributes.length; i++) {
                        String attrName = "attr_" + i;
                        Object attrValue = attributes[i];
                        
                        if (attrValue == null) {
                            structJson.putNull(attrName);
                        } else if (attrValue instanceof String) {
                            structJson.put(attrName, (String) attrValue);
                        } else if (attrValue instanceof Number) {
                            if (attrValue instanceof BigDecimal) {
                                structJson.put(attrName, (BigDecimal) attrValue);
                            } else if (attrValue instanceof Integer) {
                                structJson.put(attrName, (Integer) attrValue);
                            } else if (attrValue instanceof Long) {
                                structJson.put(attrName, (Long) attrValue);
                            } else {
                                structJson.put(attrName, attrValue.toString());
                            }
                        } else if (attrValue instanceof Date) {
                            structJson.put(attrName, attrValue.toString());
                        } else if (attrValue instanceof Timestamp) {
                            structJson.put(attrName, attrValue.toString());
                        } else if (attrValue instanceof STRUCT) {
                            // Handle nested objects recursively
                            ObjectNode nestedJson = objectMapper.createObjectNode();
                            convertOracleStructToJson(nestedJson, (STRUCT) attrValue, null);
                            structJson.set(attrName, nestedJson.get("value"));
                        } else {
                            // Fallback to string representation
                            structJson.put(attrName, attrValue.toString());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error processing STRUCT attributes, using basic approach: {}", e.getMessage());
                    // Fallback: just put the entire struct as string
                    structJson.put("struct_value", struct.toString());
                }
            } else {
                // No type descriptor available, use basic attribute extraction
                for (int i = 0; i < attributes.length; i++) {
                    Object attr = attributes[i];
                    if (attr != null) {
                        structJson.put("attr_" + i, attr.toString());
                    } else {
                        structJson.putNull("attr_" + i);
                    }
                }
            }
            
            structJson.put("_struct_type", struct.getSQLTypeName());
            jsonNode.set("value", structJson);
            
        } catch (Exception e) {
            log.warn("Error converting Oracle STRUCT to JSON: {}", e.getMessage());
            jsonNode.put("value", struct.toString());
        }
    }
    
    /**
     * Converts a generic ANYDATA object to JSON when direct ANYDATA methods are not available.
     * This handles the common case where JDBC returns the contained value directly.
     */
    private static String convertGenericObjectToJson(Object anydataValue, String columnName) throws SQLException {
        try {
            ObjectNode jsonNode = objectMapper.createObjectNode();
            ObjectNode metadata = objectMapper.createObjectNode();
            
            metadata.put("original_type", "SYS.ANYDATA");
            metadata.put("column_name", columnName);
            
            if (anydataValue == null) {
                jsonNode.put("type", "NULL");
                jsonNode.putNull("value");
            } else {
                String detectedType = detectValueType(anydataValue);
                jsonNode.put("type", detectedType);
                metadata.put("extracted_type", detectedType);
                metadata.put("java_class", anydataValue.getClass().getSimpleName());
                
                // Convert the value based on its detected type
                convertValueToJsonNode(jsonNode, anydataValue, detectedType);
            }
            
            jsonNode.set("metadata", metadata);
            
            String result = objectMapper.writeValueAsString(jsonNode);
            log.debug("Converted ANYDATA to JSON: {}", result);
            return result;
            
        } catch (Exception e) {
            log.error("Error in generic ANYDATA conversion for column '{}': {}", columnName, e.getMessage(), e);
            return createErrorJson("GENERIC_CONVERSION_ERROR", e.getMessage());
        }
    }
    
    /**
     * Detects the type of the value contained in ANYDATA based on Java class.
     */
    private static String detectValueType(Object value) {
        if (value == null) {
            return "NULL";
        }
        
        Class<?> valueClass = value.getClass();
        
        // Map Java types to Oracle type names
        if (String.class.isAssignableFrom(valueClass)) {
            return "SYS.VARCHAR2";
        } else if (BigDecimal.class.isAssignableFrom(valueClass)) {
            return "SYS.NUMBER";
        } else if (Integer.class.isAssignableFrom(valueClass) || Long.class.isAssignableFrom(valueClass)) {
            return "SYS.NUMBER";
        } else if (Double.class.isAssignableFrom(valueClass) || Float.class.isAssignableFrom(valueClass)) {
            return "SYS.NUMBER";
        } else if (Date.class.isAssignableFrom(valueClass)) {
            return "SYS.DATE";
        } else if (Timestamp.class.isAssignableFrom(valueClass)) {
            return "SYS.TIMESTAMP";
        } else if (Struct.class.isAssignableFrom(valueClass)) {
            Struct struct = (Struct) value;
            try {
                return struct.getSQLTypeName(); // Returns the Oracle object type name
            } catch (SQLException e) {
                log.warn("Could not get SQL type name from Struct: {}", e.getMessage());
                return "SYS.OBJECT";
            }
        } else if (valueClass.getName().startsWith("oracle.sql.")) {
            // Handle Oracle-specific types
            return "SYS." + valueClass.getSimpleName().toUpperCase();
        } else {
            log.warn("Unknown ANYDATA value type: {}", valueClass.getName());
            return "SYS.UNKNOWN";
        }
    }
    
    /**
     * Converts the actual value to appropriate JSON representation.
     */
    private static void convertValueToJsonNode(ObjectNode jsonNode, Object value, String detectedType) {
        try {
            switch (detectedType) {
                case "SYS.VARCHAR2":
                    jsonNode.put("value", value.toString());
                    break;
                    
                case "SYS.NUMBER":
                    if (value instanceof BigDecimal) {
                        jsonNode.put("value", (BigDecimal) value);
                    } else if (value instanceof Integer) {
                        jsonNode.put("value", (Integer) value);
                    } else if (value instanceof Long) {
                        jsonNode.put("value", (Long) value);
                    } else if (value instanceof Double) {
                        jsonNode.put("value", (Double) value);
                    } else if (value instanceof Float) {
                        jsonNode.put("value", (Float) value);
                    } else {
                        // Fallback: convert to string representation
                        jsonNode.put("value", value.toString());
                    }
                    break;
                    
                case "SYS.DATE":
                case "SYS.TIMESTAMP":
                    jsonNode.put("value", value.toString());
                    break;
                    
                default:
                    if (value instanceof Struct) {
                        // Handle Oracle object types
                        Struct struct = (Struct) value;
                        convertStructToJson(jsonNode, struct);
                    } else {
                        // Fallback: convert to string
                        jsonNode.put("value", value.toString());
                    }
                    break;
            }
        } catch (Exception e) {
            log.warn("Error converting value to JSON node, using string fallback: {}", e.getMessage());
            jsonNode.put("value", value.toString());
        }
    }
    
    /**
     * Converts Oracle Struct (object type) to JSON representation.
     */
    private static void convertStructToJson(ObjectNode jsonNode, Struct struct) throws SQLException {
        try {
            Object[] attributes = struct.getAttributes();
            ObjectNode structJson = objectMapper.createObjectNode();
            
            // Note: We don't have attribute names here, so we'll use indexes
            for (int i = 0; i < attributes.length; i++) {
                Object attr = attributes[i];
                if (attr != null) {
                    structJson.put("attr_" + i, attr.toString());
                } else {
                    structJson.putNull("attr_" + i);
                }
            }
            
            jsonNode.set("value", structJson);
            
        } catch (Exception e) {
            log.warn("Error converting Struct to JSON: {}", e.getMessage());
            jsonNode.put("value", struct.toString());
        }
    }
    
    /**
     * Creates a JSON error representation for failed conversions.
     */
    private static String createErrorJson(String errorType, String errorMessage) {
        try {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("type", "ERROR");
            errorNode.put("error_type", errorType);
            errorNode.put("error_message", errorMessage);
            
            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.put("original_type", "SYS.ANYDATA");
            metadata.put("conversion_failed", true);
            errorNode.set("metadata", metadata);
            
            return objectMapper.writeValueAsString(errorNode);
        } catch (Exception e) {
            // If we can't even create error JSON, return a simple string
            log.error("Failed to create error JSON: {}", e.getMessage());
            return "{\"type\":\"ERROR\",\"error_type\":\"JSON_CREATION_FAILED\",\"error_message\":\"" + 
                   errorMessage.replace("\"", "\\\"") + "\"}";
        }
    }
    
    /**
     * Utility method to validate if a string is valid JSON.
     * Used for testing and validation purposes.
     */
    public static boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }
        
        try {
            objectMapper.readTree(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Utility method to extract the type from converted ANYDATA JSON.
     * Useful for application code that needs to determine how to process the JSONB data.
     */
    public static String extractTypeFromJson(String jsonString) {
        try {
            JsonNode node = objectMapper.readTree(jsonString);
            JsonNode typeNode = node.get("type");
            return typeNode != null ? typeNode.asText() : "UNKNOWN";
        } catch (Exception e) {
            log.warn("Error extracting type from JSON: {}", e.getMessage());
            return "PARSE_ERROR";
        }
    }
    
    /**
     * Utility method to extract the value from converted ANYDATA JSON.
     * Returns the actual data value, stripping away the type metadata.
     */
    public static Object extractValueFromJson(String jsonString) {
        try {
            JsonNode node = objectMapper.readTree(jsonString);
            JsonNode valueNode = node.get("value");
            
            if (valueNode == null || valueNode.isNull()) {
                return null;
            }
            
            // Return appropriate Java type based on JSON node type
            if (valueNode.isTextual()) {
                return valueNode.asText();
            } else if (valueNode.isNumber()) {
                return valueNode.asDouble();
            } else if (valueNode.isBoolean()) {
                return valueNode.asBoolean();
            } else if (valueNode.isObject()) {
                return valueNode.toString(); // Return as JSON string for complex objects
            } else {
                return valueNode.asText();
            }
            
        } catch (Exception e) {
            log.warn("Error extracting value from JSON: {}", e.getMessage());
            return jsonString; // Return original string as fallback
        }
    }
}