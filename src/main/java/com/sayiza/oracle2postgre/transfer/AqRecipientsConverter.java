package com.sayiza.oracle2postgre.transfer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Timestamp;
import java.time.Instant;

// Oracle-specific imports for AQ recipients support
import oracle.sql.Datum;
import oracle.sql.STRUCT;
import oracle.sql.CHAR;
import oracle.sql.CLOB;

/**
 * Converter for Oracle AQ$_RECIPIENTS type to PostgreSQL JSONB format.
 * 
 * Handles the extraction of AQ recipients data from Oracle Advanced Queuing tables
 * and converts them to a structured JSON format suitable for PostgreSQL JSONB storage.
 * 
 * The AQ$_RECIPIENTS type typically contains recipient lists with addresses, delivery
 * status information, and routing metadata for message distribution in Oracle AQ systems.
 * 
 * JSON Output Format:
 * {
 *   "recipients_type": "AQ_RECIPIENTS",
 *   "recipients": [
 *     {
 *       "address": "user@domain.com",
 *       "name": "User Name",
 *       "delivery_status": "PENDING",
 *       "routing_info": "DIRECT",
 *       "priority": 5
 *     }
 *   ],
 *   "metadata": {
 *     "total_count": 1,
 *     "timestamp": 1640995200000,
 *     "delivery_mode": "BROADCAST"
 *   },
 *   "technical_info": {
 *     "original_type": "SYS.AQ$_RECIPIENTS",
 *     "conversion_timestamp": "2024-01-01T12:00:00Z",
 *     "attributes_count": 5
 *   }
 * }
 */
public class AqRecipientsConverter {
    
    private static final Logger log = LoggerFactory.getLogger(AqRecipientsConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Converts Oracle AQ$_RECIPIENTS to JSON format.
     * 
     * @param rs ResultSet containing the AQ recipients data
     * @param columnName Name of the column containing the AQ recipients
     * @return JSON string representation or null if conversion fails
     */
    public static String convertToJson(ResultSet rs, String columnName) {
        try {
            Object recipientsObj = rs.getObject(columnName);
            if (recipientsObj == null || rs.wasNull()) {
                return null;
            }
            
            log.debug("Converting AQ recipients from column: {}", columnName);
            
            // Handle Oracle STRUCT type
            if (recipientsObj instanceof Struct || recipientsObj instanceof STRUCT) {
                return convertStructToJson((Struct) recipientsObj);
            }
            
            // Fallback for other object types
            log.warn("Unexpected AQ recipients object type: {}", recipientsObj.getClass().getName());
            return createSimpleJson(recipientsObj.toString());
            
        } catch (SQLException e) {
            log.error("Failed to convert AQ recipients from column {}: {}", columnName, e.getMessage());
            return createErrorJson(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error converting AQ recipients: {}", e.getMessage());
            return createErrorJson(e.getMessage());
        }
    }
    
    /**
     * Converts Oracle STRUCT representing AQ$_RECIPIENTS to JSON.
     */
    private static String convertStructToJson(Struct struct) throws SQLException {
        ObjectNode jsonNode = objectMapper.createObjectNode();
        
        try {
            // Get struct attributes
            Object[] attributes = struct.getAttributes();
            if (attributes == null || attributes.length == 0) {
                log.warn("AQ recipients struct has no attributes");
                return createSimpleJson("");
            }
            
            // Set recipients type
            jsonNode.put("recipients_type", "AQ_RECIPIENTS");
            
            // Extract recipients list
            ArrayNode recipientsArray = extractRecipients(attributes);
            jsonNode.set("recipients", recipientsArray);
            
            // Extract metadata
            extractRecipientsMetadata(jsonNode, attributes, recipientsArray.size());
            
            // Add technical information
            ObjectNode technicalInfo = objectMapper.createObjectNode();
            technicalInfo.put("original_type", "SYS.AQ$_RECIPIENTS");
            technicalInfo.put("conversion_timestamp", Instant.now().toString());
            technicalInfo.put("attributes_count", attributes.length);
            jsonNode.set("technical_info", technicalInfo);
            
            return objectMapper.writeValueAsString(jsonNode);
            
        } catch (Exception e) {
            log.error("Error processing AQ recipients struct: {}", e.getMessage());
            return createErrorJson("Struct processing error: " + e.getMessage());
        }
    }
    
    /**
     * Extracts recipients from AQ recipients attributes.
     */
    private static ArrayNode extractRecipients(Object[] attributes) {
        ArrayNode recipientsArray = objectMapper.createArrayNode();
        
        try {
            // AQ$_RECIPIENTS typically contains recipient data in multiple attributes
            // Try to extract recipient information from available attributes
            int recipientCount = 0;
            
            for (int i = 0; i < attributes.length && recipientCount < 10; i++) {
                Object attr = attributes[i];
                if (attr == null) continue;
                
                String attrValue = extractStringFromAttribute(attr);
                if (attrValue != null && !attrValue.trim().isEmpty()) {
                    ObjectNode recipient = createRecipientFromAttribute(attrValue, i);
                    if (recipient != null) {
                        recipientsArray.add(recipient);
                        recipientCount++;
                        log.debug("Extracted recipient from attribute {}: {}", i, attrValue);
                    }
                }
            }
            
            // If no recipients found, create a placeholder
            if (recipientsArray.size() == 0) {
                ObjectNode placeholder = objectMapper.createObjectNode();
                placeholder.put("address", "unknown@unknown.com");
                placeholder.put("name", "Unknown Recipient");
                placeholder.put("delivery_status", "UNKNOWN");
                placeholder.put("routing_info", "UNKNOWN");
                recipientsArray.add(placeholder);
            }
            
        } catch (Exception e) {
            log.warn("Error extracting recipients: {}", e.getMessage());
            // Return empty array on error
        }
        
        return recipientsArray;
    }
    
    /**
     * Creates a recipient object from an attribute value.
     */
    private static ObjectNode createRecipientFromAttribute(String attrValue, int index) {
        try {
            ObjectNode recipient = objectMapper.createObjectNode();
            
            // Try to parse the attribute value to extract recipient information
            if (attrValue.contains("@")) {
                // Looks like an email address
                recipient.put("address", attrValue);
                recipient.put("name", extractNameFromAddress(attrValue));
                recipient.put("delivery_status", "PENDING");
                recipient.put("routing_info", "DIRECT");
            } else if (attrValue.contains("=") || attrValue.contains(":")) {
                // Looks like structured data (name=value pairs)
                parseStructuredRecipient(recipient, attrValue);
            } else {
                // Generic attribute - treat as address or name
                if (attrValue.length() > 0 && attrValue.length() < 100) {
                    recipient.put("address", attrValue + "@domain.com");
                    recipient.put("name", attrValue);
                    recipient.put("delivery_status", "PENDING");
                    recipient.put("routing_info", "DIRECT");
                } else {
                    return null; // Skip very long or empty attributes
                }
            }
            
            // Add index for tracking
            recipient.put("index", index);
            
            return recipient;
            
        } catch (Exception e) {
            log.debug("Could not create recipient from attribute: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extracts name from email address.
     */
    private static String extractNameFromAddress(String address) {
        try {
            if (address.contains("@")) {
                String localPart = address.substring(0, address.indexOf("@"));
                // Convert underscores and dots to spaces and capitalize first letter
                String formatted = localPart.replace("_", " ").replace(".", " ").toLowerCase();
                if (formatted.length() > 0) {
                    formatted = Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
                }
                return formatted;
            }
            return address;
        } catch (Exception e) {
            return address;
        }
    }
    
    /**
     * Parses structured recipient data from key=value format.
     */
    private static void parseStructuredRecipient(ObjectNode recipient, String structuredData) {
        try {
            // Default values
            recipient.put("address", "unknown@domain.com");
            recipient.put("name", "Unknown");
            recipient.put("delivery_status", "PENDING");
            recipient.put("routing_info", "DIRECT");
            
            // Parse key=value pairs
            String[] pairs = structuredData.split("[,;|]");
            for (String pair : pairs) {
                if (pair.contains("=")) {
                    String[] keyValue = pair.split("=", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().toLowerCase();
                        String value = keyValue[1].trim();
                        
                        switch (key) {
                            case "address":
                            case "email":
                            case "addr":
                                recipient.put("address", value);
                                break;
                            case "name":
                            case "recipient":
                            case "user":
                                recipient.put("name", value);
                                break;
                            case "status":
                            case "delivery_status":
                            case "state":
                                recipient.put("delivery_status", value.toUpperCase());
                                break;
                            case "routing":
                            case "routing_info":
                            case "route":
                                recipient.put("routing_info", value.toUpperCase());
                                break;
                            case "priority":
                            case "prio":
                                try {
                                    recipient.put("priority", Integer.parseInt(value));
                                } catch (NumberFormatException e) {
                                    recipient.put("priority", 5); // Default priority
                                }
                                break;
                            default:
                                // Store unknown attributes as custom properties
                                recipient.put("custom_" + key, value);
                                break;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.debug("Error parsing structured recipient data: {}", e.getMessage());
        }
    }
    
    /**
     * Extracts metadata from AQ recipients attributes.
     */
    private static void extractRecipientsMetadata(ObjectNode jsonNode, Object[] attributes, int recipientCount) {
        ObjectNode metadata = objectMapper.createObjectNode();
        
        try {
            metadata.put("total_count", recipientCount);
            metadata.put("timestamp", System.currentTimeMillis());
            
            // Try to extract delivery mode and other metadata from attributes
            if (attributes.length > recipientCount) {
                for (int i = recipientCount; i < attributes.length; i++) {
                    Object attr = attributes[i];
                    if (attr == null) continue;
                    
                    String attrValue = extractStringFromAttribute(attr);
                    if (attrValue != null && !attrValue.trim().isEmpty()) {
                        // Try to identify metadata patterns
                        String upperValue = attrValue.toUpperCase();
                        if (upperValue.matches("(BROADCAST|MULTICAST|UNICAST|DIRECT)")) {
                            metadata.put("delivery_mode", attrValue.toUpperCase());
                        } else if (upperValue.matches("(HIGH|MEDIUM|LOW|URGENT|NORMAL)")) {
                            metadata.put("priority_level", attrValue.toUpperCase());
                        } else if (attrValue.length() < 50) {
                            metadata.put("attr_" + i, attrValue);
                        }
                    }
                }
            }
            
            // Set default delivery mode if not found
            if (!metadata.has("delivery_mode")) {
                metadata.put("delivery_mode", "DIRECT");
            }
            
        } catch (Exception e) {
            log.warn("Error extracting recipients metadata: {}", e.getMessage());
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
     * Creates a simple JSON structure for basic recipients content.
     */
    private static String createSimpleJson(String content) {
        try {
            ObjectNode jsonNode = objectMapper.createObjectNode();
            jsonNode.put("recipients_type", "AQ_RECIPIENTS");
            
            // Create a simple recipient from the content
            ArrayNode recipientsArray = objectMapper.createArrayNode();
            ObjectNode recipient = objectMapper.createObjectNode();
            
            if (content != null && !content.trim().isEmpty()) {
                if (content.contains("@")) {
                    recipient.put("address", content);
                    recipient.put("name", extractNameFromAddress(content));
                } else {
                    recipient.put("address", "unknown@domain.com");
                    recipient.put("name", content);
                }
            } else {
                recipient.put("address", "unknown@domain.com");
                recipient.put("name", "Unknown Recipient");
            }
            
            recipient.put("delivery_status", "PENDING");
            recipient.put("routing_info", "DIRECT");
            recipientsArray.add(recipient);
            jsonNode.set("recipients", recipientsArray);
            
            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.put("total_count", 1);
            metadata.put("timestamp", System.currentTimeMillis());
            metadata.put("delivery_mode", "DIRECT");
            jsonNode.set("metadata", metadata);
            
            ObjectNode technicalInfo = objectMapper.createObjectNode();
            technicalInfo.put("original_type", "SYS.AQ$_RECIPIENTS");
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
            jsonNode.put("recipients_type", "AQ_RECIPIENTS");
            jsonNode.put("conversion_error", errorMessage);
            
            // Create empty recipients array
            ArrayNode recipientsArray = objectMapper.createArrayNode();
            jsonNode.set("recipients", recipientsArray);
            
            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.put("total_count", 0);
            metadata.put("timestamp", System.currentTimeMillis());
            metadata.put("delivery_mode", "ERROR");
            jsonNode.set("metadata", metadata);
            
            ObjectNode technicalInfo = objectMapper.createObjectNode();
            technicalInfo.put("original_type", "SYS.AQ$_RECIPIENTS");
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
     * Checks if a column contains AQ$_RECIPIENTS data type.
     * 
     * @param dataType The column data type string
     * @return true if it's an AQ recipients type
     */
    public static boolean isAqRecipientsType(String dataType) {
        if (dataType == null) return false;
        
        String normalizedType = dataType.toLowerCase().trim();
        return normalizedType.equals("aq$_recipients") || 
               normalizedType.equals("sys.aq$_recipients") ||
               normalizedType.contains("aq$_recipients");
    }
}