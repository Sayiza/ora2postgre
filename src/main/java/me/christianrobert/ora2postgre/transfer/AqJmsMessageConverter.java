package me.christianrobert.ora2postgre.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.time.Instant;

// Oracle-specific imports for AQ JMS message support
import oracle.sql.STRUCT;
import oracle.sql.CHAR;
import oracle.sql.CLOB;

/**
 * Converter for Oracle AQ$_JMS_TEXT_MESSAGE type to PostgreSQL JSONB format.
 *
 * Handles the extraction of JMS message data from Oracle Advanced Queuing tables
 * and converts them to a structured JSON format suitable for PostgreSQL JSONB storage.
 *
 * The resulting JSON structure preserves both the JMS message content and metadata,
 * enabling message querying and processing operations in PostgreSQL.
 *
 * JSON Output Format:
 * {
 *   "message_type": "JMS_TEXT_MESSAGE",
 *   "text_content": "actual message text",
 *   "headers": {
 *     "jms_message_id": "ID:...",
 *     "jms_timestamp": 1640995200000,
 *     "jms_correlation_id": "CORR123",
 *     "jms_delivery_mode": "PERSISTENT"
 *   },
 *   "properties": {
 *     "custom_prop1": "value1",
 *     "priority": 5
 *   },
 *   "metadata": {
 *     "original_type": "SYS.AQ$_JMS_TEXT_MESSAGE",
 *     "conversion_timestamp": "2024-01-01T12:00:00Z"
 *   }
 * }
 */
public class AqJmsMessageConverter {

  private static final Logger log = LoggerFactory.getLogger(AqJmsMessageConverter.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Converts Oracle AQ$_JMS_TEXT_MESSAGE to JSON format.
   *
   * @param rs ResultSet containing the AQ message data
   * @param columnName Name of the column containing the AQ message
   * @return JSON string representation or null if conversion fails
   */
  public static String convertToJson(ResultSet rs, String columnName) {
    try {
      Object messageObj = rs.getObject(columnName);
      if (messageObj == null || rs.wasNull()) {
        return null;
      }

      log.debug("Converting AQ JMS message from column: {}", columnName);

      // Handle Oracle STRUCT type
      if (messageObj instanceof Struct || messageObj instanceof STRUCT) {
        return convertStructToJson((Struct) messageObj);
      }

      // Fallback for other object types
      log.warn("Unexpected AQ message object type: {}", messageObj.getClass().getName());
      return createSimpleJson(messageObj.toString());

    } catch (SQLException e) {
      log.error("Failed to convert AQ JMS message from column {}: {}", columnName, e.getMessage());
      return createErrorJson(e.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error converting AQ JMS message: {}", e.getMessage());
      return createErrorJson(e.getMessage());
    }
  }

  /**
   * Converts Oracle STRUCT representing AQ$_JMS_TEXT_MESSAGE to JSON.
   */
  private static String convertStructToJson(Struct struct) throws SQLException {
    ObjectNode jsonNode = objectMapper.createObjectNode();

    try {
      // Get struct attributes
      Object[] attributes = struct.getAttributes();
      if (attributes == null || attributes.length == 0) {
        log.warn("AQ JMS message struct has no attributes");
        return createSimpleJson("Empty AQ message");
      }

      // Set message type
      jsonNode.put("message_type", "JMS_TEXT_MESSAGE");

      // Extract text content (typically first attribute)
      extractTextContent(jsonNode, attributes);

      // Extract JMS headers and properties
      extractJmsHeaders(jsonNode, attributes);

      // Add metadata
      ObjectNode metadata = objectMapper.createObjectNode();
      metadata.put("original_type", "SYS.AQ$_JMS_TEXT_MESSAGE");
      metadata.put("conversion_timestamp", Instant.now().toString());
      metadata.put("attributes_count", attributes.length);
      jsonNode.set("metadata", metadata);

      return objectMapper.writeValueAsString(jsonNode);

    } catch (Exception e) {
      log.error("Error processing AQ JMS message struct: {}", e.getMessage());
      return createErrorJson("Struct processing error: " + e.getMessage());
    }
  }

  /**
   * Extracts text content from AQ message attributes.
   */
  private static void extractTextContent(ObjectNode jsonNode, Object[] attributes) {
    try {
      // AQ$_JMS_TEXT_MESSAGE typically has text in early attributes
      for (int i = 0; i < attributes.length && i < 3; i++) {
        Object attr = attributes[i];
        if (attr == null) continue;

        String textContent = extractTextFromAttribute(attr);
        if (textContent != null && !textContent.trim().isEmpty()) {
          jsonNode.put("text_content", textContent);
          log.debug("Extracted text content from attribute {}: {} chars", i, textContent.length());
          return;
        }
      }

      // If no text content found, set empty
      jsonNode.put("text_content", "");

    } catch (Exception e) {
      log.warn("Error extracting text content: {}", e.getMessage());
      jsonNode.put("text_content", "Error extracting text");
    }
  }

  /**
   * Extracts text from various Oracle attribute types.
   */
  private static String extractTextFromAttribute(Object attr) {
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
      log.warn("Error extracting text from attribute type {}: {}", attr.getClass().getName(), e.getMessage());
      return null;
    }
  }

  /**
   * Extracts JMS headers and properties from message attributes.
   */
  private static void extractJmsHeaders(ObjectNode jsonNode, Object[] attributes) {
    ObjectNode headers = objectMapper.createObjectNode();
    ObjectNode properties = objectMapper.createObjectNode();

    try {
      // JMS headers are typically in later attributes
      // This is a best-effort extraction based on common AQ patterns
      if (attributes.length > 1) {
        // Try to extract common JMS headers
        extractJmsHeadersFromAttributes(headers, attributes);
      }

      // Add timestamp
      headers.put("jms_timestamp", System.currentTimeMillis());

      // Generate a message ID if not found
      if (!headers.has("jms_message_id")) {
        headers.put("jms_message_id", "ID:" + System.currentTimeMillis());
      }

    } catch (Exception e) {
      log.warn("Error extracting JMS headers: {}", e.getMessage());
      headers.put("extraction_error", e.getMessage());
    }

    jsonNode.set("headers", headers);
    jsonNode.set("properties", properties);
  }

  /**
   * Attempts to extract JMS headers from message attributes.
   */
  private static void extractJmsHeadersFromAttributes(ObjectNode headers, Object[] attributes) {
    // This is a heuristic approach since AQ message structure can vary
    for (int i = 1; i < attributes.length; i++) {
      Object attr = attributes[i];
      if (attr == null) continue;

      try {
        // Try to extract meaningful header values
        String attrStr = attr.toString();
        if (attrStr.length() > 0 && attrStr.length() < 200) {
          headers.put("attr_" + i, attrStr);
        }
      } catch (Exception e) {
        log.debug("Could not process attribute {} as header: {}", i, e.getMessage());
      }
    }
  }

  /**
   * Creates a simple JSON structure for basic message content.
   */
  private static String createSimpleJson(String content) {
    try {
      ObjectNode jsonNode = objectMapper.createObjectNode();
      jsonNode.put("message_type", "JMS_TEXT_MESSAGE");
      jsonNode.put("text_content", content != null ? content : "");

      ObjectNode headers = objectMapper.createObjectNode();
      headers.put("jms_message_id", "ID:" + System.currentTimeMillis());
      headers.put("jms_timestamp", System.currentTimeMillis());
      jsonNode.set("headers", headers);

      ObjectNode metadata = objectMapper.createObjectNode();
      metadata.put("original_type", "SYS.AQ$_JMS_TEXT_MESSAGE");
      metadata.put("conversion_method", "simple");
      metadata.put("conversion_timestamp", Instant.now().toString());
      jsonNode.set("metadata", metadata);

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
      jsonNode.put("message_type", "JMS_TEXT_MESSAGE");
      jsonNode.put("text_content", "");
      jsonNode.put("conversion_error", errorMessage);

      ObjectNode metadata = objectMapper.createObjectNode();
      metadata.put("original_type", "SYS.AQ$_JMS_TEXT_MESSAGE");
      metadata.put("conversion_status", "ERROR");
      metadata.put("conversion_timestamp", Instant.now().toString());
      jsonNode.set("metadata", metadata);

      return objectMapper.writeValueAsString(jsonNode);
    } catch (Exception e) {
      log.error("Failed to create error JSON: {}", e.getMessage());
      return "{\"error\":\"Error JSON creation failed\"}";
    }
  }

  /**
   * Checks if a column contains AQ$_JMS_TEXT_MESSAGE data type.
   *
   * @param dataType The column data type string
   * @return true if it's an AQ JMS message type
   */
  public static boolean isAqJmsMessageType(String dataType) {
    if (dataType == null) return false;

    String normalizedType = dataType.toLowerCase().trim();
    return normalizedType.equals("aq$_jms_text_message") ||
            normalizedType.equals("sys.aq$_jms_text_message") ||
            normalizedType.contains("aq$_jms_text_message");
  }
}