package me.christianrobert.ora2postgre.transfer;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.ObjectType;
import me.christianrobert.ora2postgre.plsql.ast.Variable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.List;

/**
 * Handles conversion of Oracle object types to PostgreSQL JSON/JSONB format.
 *
 * This class is responsible for:
 * - Converting Oracle Struct objects to JSON
 * - Mapping object type fields using AST definitions
 * - Handling nested objects and NULL values
 * - Setting JSONB parameters in PostgreSQL PreparedStatements
 */
public class ObjectTypeMapper {

  private static final Logger log = LoggerFactory.getLogger(ObjectTypeMapper.class);
  private final ObjectMapper objectMapper;

  public ObjectTypeMapper() {
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Converts an Oracle object (Struct) to PostgreSQL composite type tuple format.
   *
   * PostgreSQL composite types should be passed as tuple literals in the format (val1,val2)
   * when used as parameters in prepared statements, not as ROW(...) constructors.
   *
   * @param oracleObject The Oracle object (typically java.sql.Struct or similar)
   * @param objectTypeAst The AST definition of the object type
   * @return PostgreSQL tuple literal string, or null if object is null
   */
  public String convertObjectToCompositeType(Object oracleObject, ObjectType objectTypeAst) {
    if (oracleObject == null) {
      return null; // Return null for PostgreSQL NULL handling
    }

    try {
      Object[] attributes = extractAttributes(oracleObject);
      List<Variable> variables = objectTypeAst.getVariables();

      if (attributes.length != variables.size()) {
        log.warn("Attribute count mismatch: object has {} attributes, type definition has {} fields",
                attributes.length, variables.size());
      }

      // Build PostgreSQL tuple literal format: (value1,value2,...)
      StringBuilder result = new StringBuilder("(");

      // Convert each attribute to its PostgreSQL representation
      for (int i = 0; i < Math.min(attributes.length, variables.size()); i++) {
        if (i > 0) {
          result.append(",");
        }

        Object attributeValue = attributes[i];
        Variable variable = variables.get(i);
        String postgresValue = convertAttributeToTupleValue(attributeValue, variable);
        result.append(postgresValue);
      }

      result.append(")");
      return result.toString();

    } catch (Exception e) {
      log.error("Failed to convert Oracle object to composite type: {}", e.getMessage(), e);
      throw new RuntimeException("Object to composite type conversion failed", e);
    }
  }

  /**
   * Converts an Oracle object (Struct) to JSON string representation.
   *
   * @param oracleObject The Oracle object (typically java.sql.Struct or similar)
   * @param objectTypeAst The AST definition of the object type
   * @param everything The Everything context for object type lookups
   * @return JSON string representation of the object, or "null" if object is null
   */
  public String convertObjectToJson(Object oracleObject, ObjectType objectTypeAst, Everything everything) {
    if (oracleObject == null) {
      return "null";
    }

    try {
      JsonNode jsonNode = convertObjectToJsonNode(oracleObject, objectTypeAst, everything);
      return objectMapper.writeValueAsString(jsonNode);
    } catch (Exception e) {
      log.error("Failed to convert Oracle object to JSON: {}", e.getMessage(), e);
      throw new RuntimeException("Object to JSON conversion failed", e);
    }
  }

  /**
   * Converts an Oracle object to a Jackson JsonNode for further processing.
   *
   * @param oracleObject The Oracle object
   * @param objectTypeAst The AST definition of the object type
   * @param everything The Everything context for object type lookups
   * @return JsonNode representation of the object
   */
  public JsonNode convertObjectToJsonNode(Object oracleObject, ObjectType objectTypeAst, Everything everything) throws SQLException {
    if (oracleObject == null) {
      return JsonNodeFactory.instance.nullNode();
    }

    ObjectNode result = JsonNodeFactory.instance.objectNode();
    Object[] attributes = extractAttributes(oracleObject);
    List<Variable> variables = objectTypeAst.getVariables();

    if (attributes.length != variables.size()) {
      log.warn("Attribute count mismatch: object has {} attributes, type definition has {} fields",
              attributes.length, variables.size());
    }

    // Map each attribute to its corresponding field name
    for (int i = 0; i < Math.min(attributes.length, variables.size()); i++) {
      Variable variable = variables.get(i);
      Object attributeValue = attributes[i];
      String fieldName = variable.getName().toLowerCase(); // PostgreSQL convention

      if (attributeValue == null) {
        result.set(fieldName, JsonNodeFactory.instance.nullNode());
      } else {
        // Convert attribute based on its type
        JsonNode valueNode = convertAttributeToJsonNode(attributeValue, variable);
        result.set(fieldName, valueNode);
      }
    }

    return result;
  }

  /**
   * Extracts attributes array from an Oracle object.
   * Handles both real java.sql.Struct objects and mock objects for testing.
   */
  private Object[] extractAttributes(Object oracleObject) throws SQLException {
    if (oracleObject instanceof Struct) {
      // Real Oracle Struct object
      Struct struct = (Struct) oracleObject;
      return struct.getAttributes();
    } else if (oracleObject.getClass().getSimpleName().equals("MockOracleStruct")) {
      // Mock object for testing - use reflection to avoid compilation dependency
      try {
        return (Object[]) oracleObject.getClass().getMethod("getAttributes").invoke(oracleObject);
      } catch (Exception e) {
        throw new SQLException("Failed to extract attributes from mock object", e);
      }
    } else {
      throw new IllegalArgumentException("Unsupported Oracle object type: " + oracleObject.getClass());
    }
  }

  /**
   * Converts a single attribute value to PostgreSQL value string for composite type constructor.
   */
  private String convertAttributeToPostgresValue(Object attributeValue, Variable variable) {
    if (attributeValue == null) {
      return "NULL";
    }

    String dataType = variable.getDataType().getNativeDataType();
    if (dataType != null) {
      dataType = dataType.toUpperCase();
    } else {
      // Handle custom data types or other cases
      dataType = "VARCHAR2"; // Default fallback
    }

    // Handle different Oracle data types for PostgreSQL composite type constructors
    switch (dataType) {
      case "VARCHAR2":
      case "CHAR":
      case "CLOB":
        // String values need to be quoted and escaped
        String stringValue = attributeValue.toString();
        return "'" + stringValue.replace("'", "''") + "'";

      case "NUMBER":
      case "INTEGER":
        // Numbers can be written as-is
        return attributeValue.toString();

      case "DATE":
      case "TIMESTAMP":
        // Dates need to be quoted for PostgreSQL
        return "'" + attributeValue.toString() + "'";

      default:
        // For unknown types, treat as string with escaping
        log.debug("Unknown data type '{}', converting to string", dataType);
        String defaultValue = attributeValue.toString();
        return "'" + defaultValue.replace("'", "''") + "'";
    }
  }

  /**
   * Converts a single attribute value to PostgreSQL tuple literal format.
   *
   * For PostgreSQL tuple literals (value1,value2), the format is slightly different
   * from ROW constructor syntax. String values need proper escaping.
   */
  private String convertAttributeToTupleValue(Object attributeValue, Variable variable) {
    if (attributeValue == null) {
      return ""; // Empty value in tuple represents NULL
    }

    String dataType = variable.getDataType().getNativeDataType();
    if (dataType != null) {
      dataType = dataType.toUpperCase();
    } else {
      // Handle custom data types or other cases
      dataType = "VARCHAR2"; // Default fallback
    }

    // Handle different Oracle data types for PostgreSQL tuple literals
    switch (dataType) {
      case "VARCHAR2":
      case "CHAR":
      case "CLOB":
        // String values in tuple literals need to be quoted and escaped
        String stringValue = attributeValue.toString();
        // Escape quotes and backslashes for tuple literal format
        return "\"" + stringValue.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";

      case "NUMBER":
      case "INTEGER":
        // Numbers can be written as-is
        return attributeValue.toString();

      case "DATE":
      case "TIMESTAMP":
        // Dates need to be quoted for PostgreSQL tuple literals
        String dateValue = attributeValue.toString();
        return "\"" + dateValue.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";

      default:
        // For unknown types, treat as string with escaping
        log.debug("Unknown data type '{}', converting to string", dataType);
        String defaultValue = attributeValue.toString();
        return "\"" + defaultValue.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
  }

  /**
   * Converts a single attribute value to JsonNode based on the variable type.
   */
  private JsonNode convertAttributeToJsonNode(Object attributeValue, Variable variable) {
    if (attributeValue == null) {
      return JsonNodeFactory.instance.nullNode();
    }

    String dataType = variable.getDataType().getNativeDataType();
    if (dataType != null) {
      dataType = dataType.toUpperCase();
    } else {
      // Handle custom data types or other cases
      dataType = "VARCHAR2"; // Default fallback
    }

    // Handle different Oracle data types
    switch (dataType) {
      case "VARCHAR2":
      case "CHAR":
      case "CLOB":
        return JsonNodeFactory.instance.textNode(attributeValue.toString());

      case "NUMBER":
      case "INTEGER":
        if (attributeValue instanceof Number) {
          Number num = (Number) attributeValue;
          if (num.doubleValue() == num.longValue()) {
            return JsonNodeFactory.instance.numberNode(num.longValue());
          } else {
            return JsonNodeFactory.instance.numberNode(num.doubleValue());
          }
        } else {
          return JsonNodeFactory.instance.numberNode(Double.parseDouble(attributeValue.toString()));
        }

      case "DATE":
      case "TIMESTAMP":
        return JsonNodeFactory.instance.textNode(attributeValue.toString());

      default:
        // For unknown types, convert to string
        log.debug("Unknown data type '{}', converting to string", dataType);
        return JsonNodeFactory.instance.textNode(attributeValue.toString());
    }
  }

  /**
   * Sets a composite type parameter in a PostgreSQL PreparedStatement.
   *
   * For PostgreSQL composite types, we pass tuple literals in the format (val1,val2)
   * and let PostgreSQL handle the type conversion automatically.
   *
   * @param statement The PreparedStatement
   * @param parameterIndex The parameter index (1-based)
   * @param compositeValue The tuple literal string (e.g., "(val1,val2)") or null
   */
  public void setCompositeTypeParameter(PreparedStatement statement, int parameterIndex, String compositeValue)
          throws SQLException {
    if (compositeValue == null) {
      statement.setNull(parameterIndex, java.sql.Types.OTHER);
    } else {
      // For PostgreSQL composite types, the simplest approach is to set the tuple literal as a string
      // PostgreSQL will automatically parse and convert it to the appropriate composite type
      // based on the target column's type definition
      statement.setObject(parameterIndex, compositeValue, java.sql.Types.OTHER);
    }
  }

  /**
   * Sets a JSON parameter in a PostgreSQL PreparedStatement as JSONB.
   *
   * @param statement The PreparedStatement
   * @param parameterIndex The parameter index (1-based)
   * @param jsonValue The JSON string value
   */
  public void setJsonbParameter(PreparedStatement statement, int parameterIndex, String jsonValue)
          throws SQLException {
    if ("null".equals(jsonValue)) {
      statement.setNull(parameterIndex, java.sql.Types.OTHER);
    } else {
      // Set as JSONB - PostgreSQL will handle the conversion
      statement.setObject(parameterIndex, jsonValue, java.sql.Types.OTHER);
    }
  }

  /**
   * Formats a JSON value for use in SQL INSERT statements.
   *
   * @param jsonValue The JSON string
   * @return Formatted string for SQL (e.g., "'json_value'::jsonb")
   */
  public String formatJsonForSql(String jsonValue) {
    if ("null".equals(jsonValue)) {
      return "NULL";
    } else {
      // Escape single quotes and wrap for PostgreSQL JSONB casting
      String escaped = jsonValue.replace("'", "''");
      return "'" + escaped + "'::jsonb";
    }
  }

  /**
   * Looks up an ObjectType definition by name and schema.
   *
   * @param schema The schema name
   * @param typeName The object type name
   * @param everything The Everything context for object type lookups
   * @return The ObjectType AST node, or null if not found
   */
  public ObjectType lookupObjectType(String schema, String typeName, Everything everything) {
    return findObjectType(schema, typeName, everything);
  }

  /**
   * Checks if a data type name represents an Oracle object type.
   *
   * @param schema The schema context
   * @param dataTypeName The data type name to check
   * @param everything The Everything context for object type lookups
   * @return true if it's a known object type, false otherwise
   */
  public boolean isObjectType(String schema, String dataTypeName, Everything everything) {
    return findObjectType(schema, dataTypeName, everything) != null;
  }

  /**
   * Helper method to find an ObjectType by schema and name.
   *
   * @param schema The schema name
   * @param typeName The object type name
   * @param everything The Everything context for object type lookups
   * @return The ObjectType AST node, or null if not found
   */
  private ObjectType findObjectType(String schema, String typeName, Everything everything) {
    return everything.getObjectTypeSpecAst().stream()
            .filter(objectType -> objectType.getSchema().equalsIgnoreCase(schema) &&
                    objectType.getName().equalsIgnoreCase(typeName))
            .findFirst()
            .orElse(null);
  }
}