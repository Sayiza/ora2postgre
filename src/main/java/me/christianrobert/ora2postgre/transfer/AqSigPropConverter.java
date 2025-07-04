package me.christianrobert.ora2postgre.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.time.Instant;

// Oracle-specific imports for AQ signature property support
import oracle.sql.STRUCT;
import oracle.sql.CHAR;
import oracle.sql.CLOB;

/**
 * Converter for Oracle AQ$_SIG_PROP type to PostgreSQL JSONB format.
 * 
 * Handles the extraction of AQ signature property data from Oracle Advanced Queuing tables
 * and converts them to a structured JSON format suitable for PostgreSQL JSONB storage.
 * 
 * The AQ$_SIG_PROP type typically contains signature properties and metadata for message
 * authentication and validation in Oracle Advanced Queuing systems.
 * 
 * JSON Output Format:
 * {
 *   "signature_type": "AQ_SIG_PROP",
 *   "signature_properties": {
 *     "algorithm": "SHA256",
 *     "digest": "base64-encoded-digest",
 *     "signature": "base64-encoded-signature"
 *   },
 *   "metadata": {
 *     "timestamp": 1640995200000,
 *     "signer": "certificate-info",
 *     "validation_status": "VALID"
 *   },
 *   "technical_info": {
 *     "original_type": "SYS.AQ$_SIG_PROP",
 *     "conversion_timestamp": "2024-01-01T12:00:00Z",
 *     "attributes_count": 5
 *   }
 * }
 */
public class AqSigPropConverter {
    
    private static final Logger log = LoggerFactory.getLogger(AqSigPropConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Converts Oracle AQ$_SIG_PROP to JSON format.
     * 
     * @param rs ResultSet containing the AQ signature property data
     * @param columnName Name of the column containing the AQ signature property
     * @return JSON string representation or null if conversion fails
     */
    public static String convertToJson(ResultSet rs, String columnName) {
        try {
            Object sigPropObj = rs.getObject(columnName);
            if (sigPropObj == null || rs.wasNull()) {
                return null;
            }
            
            log.debug("Converting AQ signature property from column: {}", columnName);
            
            // Handle Oracle STRUCT type
            if (sigPropObj instanceof Struct || sigPropObj instanceof STRUCT) {
                return convertStructToJson((Struct) sigPropObj);
            }
            
            // Fallback for other object types
            log.warn("Unexpected AQ signature property object type: {}", sigPropObj.getClass().getName());
            return createSimpleJson(sigPropObj.toString());
            
        } catch (SQLException e) {
            log.error("Failed to convert AQ signature property from column {}: {}", columnName, e.getMessage());
            return createErrorJson(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error converting AQ signature property: {}", e.getMessage());
            return createErrorJson(e.getMessage());
        }
    }
    
    /**
     * Converts Oracle STRUCT representing AQ$_SIG_PROP to JSON.
     */
    private static String convertStructToJson(Struct struct) throws SQLException {
        ObjectNode jsonNode = objectMapper.createObjectNode();
        
        try {
            // Get struct attributes
            Object[] attributes = struct.getAttributes();
            if (attributes == null || attributes.length == 0) {
                log.warn("AQ signature property struct has no attributes");
                return createSimpleJson("");
            }
            
            // Set signature type
            jsonNode.put("signature_type", "AQ_SIG_PROP");
            
            // Extract signature properties
            extractSignatureProperties(jsonNode, attributes);
            
            // Extract metadata
            extractSignatureMetadata(jsonNode, attributes);
            
            // Add technical information
            ObjectNode technicalInfo = objectMapper.createObjectNode();
            technicalInfo.put("original_type", "SYS.AQ$_SIG_PROP");
            technicalInfo.put("conversion_timestamp", Instant.now().toString());
            technicalInfo.put("attributes_count", attributes.length);
            jsonNode.set("technical_info", technicalInfo);
            
            return objectMapper.writeValueAsString(jsonNode);
            
        } catch (Exception e) {
            log.error("Error processing AQ signature property struct: {}", e.getMessage());
            return createErrorJson("Struct processing error: " + e.getMessage());
        }
    }
    
    /**
     * Extracts signature properties from AQ signature property attributes.
     */
    private static void extractSignatureProperties(ObjectNode jsonNode, Object[] attributes) {
        ObjectNode sigProps = objectMapper.createObjectNode();
        
        try {
            // AQ$_SIG_PROP typically has signature-related data in early attributes
            // This is a best-effort extraction based on common AQ signature property patterns
            for (int i = 0; i < attributes.length && i < 5; i++) {
                Object attr = attributes[i];
                if (attr == null) continue;
                
                String attrValue = extractStringFromAttribute(attr);
                if (attrValue != null && !attrValue.trim().isEmpty()) {
                    switch (i) {
                        case 0:
                            // First attribute often contains algorithm info
                            sigProps.put("algorithm", attrValue);
                            break;
                        case 1:
                            // Second attribute might contain digest
                            sigProps.put("digest", attrValue);
                            break;
                        case 2:
                            // Third attribute might contain signature
                            sigProps.put("signature", attrValue);
                            break;
                        default:
                            // Other attributes as generic properties
                            sigProps.put("prop_" + i, attrValue);
                            break;
                    }
                    log.debug("Extracted signature property from attribute {}: {} chars", i, attrValue.length());
                }
            }
            
            // If no properties found, set defaults
            if (sigProps.isEmpty()) {
                sigProps.put("algorithm", "UNKNOWN");
                sigProps.put("digest", "");
                sigProps.put("signature", "");
            }
            
        } catch (Exception e) {
            log.warn("Error extracting signature properties: {}", e.getMessage());
            sigProps.put("algorithm", "EXTRACTION_ERROR");
            sigProps.put("error_message", e.getMessage());
        }
        
        jsonNode.set("signature_properties", sigProps);
    }
    
    /**
     * Extracts metadata from AQ signature property attributes.
     */
    private static void extractSignatureMetadata(ObjectNode jsonNode, Object[] attributes) {
        ObjectNode metadata = objectMapper.createObjectNode();
        
        try {
            // Extract metadata from later attributes
            if (attributes.length > 3) {
                for (int i = 3; i < attributes.length; i++) {
                    Object attr = attributes[i];
                    if (attr == null) continue;
                    
                    String attrValue = extractStringFromAttribute(attr);
                    if (attrValue != null && !attrValue.trim().isEmpty()) {
                        // Try to identify common metadata patterns
                        if (attrValue.toUpperCase().matches("(VALID|INVALID|PENDING|ERROR|UNKNOWN)")) {
                            metadata.put("validation_status", attrValue);
                        } else if (attrValue.contains("CN=") || attrValue.contains("CERT")) {
                            metadata.put("signer", attrValue);
                        } else {
                            metadata.put("attr_" + i, attrValue);
                        }
                    }
                }
            }
            
            // Add current timestamp
            metadata.put("timestamp", System.currentTimeMillis());
            
            // Set default validation status if not found
            if (!metadata.has("validation_status")) {
                metadata.put("validation_status", "UNKNOWN");
            }
            
        } catch (Exception e) {
            log.warn("Error extracting signature metadata: {}", e.getMessage());
            metadata.put("extraction_error", e.getMessage());
        }
        
        jsonNode.set("metadata", metadata);
    }
    
    /**
     * Extracts string from various Oracle attribute types.
     */
    private static String extractStringFromAttribute(Object attr) {
        try {
            if (attr instanceof String) {
                return (String) attr;
            } else if (attr instanceof CHAR) {
                return ((CHAR) attr).getString();
            } else if (attr instanceof CLOB) {
                CLOB clob = (CLOB) attr;
                return clob.getSubString(1, (int) clob.length());
            } else if (attr instanceof oracle.sql.CLOB) {
                oracle.sql.CLOB clob = (oracle.sql.CLOB) attr;
                return clob.stringValue();
            } else {
                return attr.toString();
            }
        } catch (Exception e) {
            log.warn("Error extracting string from attribute type {}: {}", attr.getClass().getName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Creates a simple JSON structure for basic signature property content.
     */
    private static String createSimpleJson(String content) {
        try {
            ObjectNode jsonNode = objectMapper.createObjectNode();
            jsonNode.put("signature_type", "AQ_SIG_PROP");
            
            ObjectNode sigProps = objectMapper.createObjectNode();
            sigProps.put("algorithm", "UNKNOWN");
            sigProps.put("digest", "");
            sigProps.put("signature", content != null ? content : "");
            jsonNode.set("signature_properties", sigProps);
            
            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.put("timestamp", System.currentTimeMillis());
            metadata.put("validation_status", "UNKNOWN");
            jsonNode.set("metadata", metadata);
            
            ObjectNode technicalInfo = objectMapper.createObjectNode();
            technicalInfo.put("original_type", "SYS.AQ$_SIG_PROP");
            technicalInfo.put("conversion_method", "simple");
            technicalInfo.put("conversion_timestamp", Instant.now().toString());
            jsonNode.set("technical_info", technicalInfo);
            
            return objectMapper.writeValueAsString(jsonNode);
        } catch (Exception e) {
            log.error("Failed to create simple JSON: {}", e.getMessage());
            return "{\"error\":\"JSON creation failed\"}";
        }
    }
    
    /**
     * Creates an error JSON structure when conversion fails.
     */
    private static String createErrorJson(String errorMessage) {
        try {
            ObjectNode jsonNode = objectMapper.createObjectNode();
            jsonNode.put("signature_type", "AQ_SIG_PROP");
            jsonNode.put("conversion_error", errorMessage);
            
            ObjectNode sigProps = objectMapper.createObjectNode();
            sigProps.put("algorithm", "ERROR");
            sigProps.put("digest", "");
            sigProps.put("signature", "");
            jsonNode.set("signature_properties", sigProps);
            
            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.put("timestamp", System.currentTimeMillis());
            metadata.put("validation_status", "ERROR");
            jsonNode.set("metadata", metadata);
            
            ObjectNode technicalInfo = objectMapper.createObjectNode();
            technicalInfo.put("original_type", "SYS.AQ$_SIG_PROP");
            technicalInfo.put("conversion_status", "ERROR");
            technicalInfo.put("conversion_timestamp", Instant.now().toString());
            jsonNode.set("technical_info", technicalInfo);
            
            return objectMapper.writeValueAsString(jsonNode);
        } catch (Exception e) {
            log.error("Failed to create error JSON: {}", e.getMessage());
            return "{\"error\":\"Error JSON creation failed\"}";
        }
    }
    
    /**
     * Checks if a column contains AQ$_SIG_PROP data type.
     * 
     * @param dataType The column data type string
     * @return true if it's an AQ signature property type
     */
    public static boolean isAqSigPropType(String dataType) {
        if (dataType == null) return false;
        
        String normalizedType = dataType.toLowerCase().trim();
        return normalizedType.equals("aq$_sig_prop") || 
               normalizedType.equals("sys.aq$_sig_prop") ||
               normalizedType.contains("aq$_sig_prop");
    }
}