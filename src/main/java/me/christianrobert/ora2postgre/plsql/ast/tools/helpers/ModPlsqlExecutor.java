package me.christianrobert.ora2postgre.plsql.ast.tools.helpers;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Utility class for executing PostgreSQL procedures with HTP buffer management.
 * 
 * This class provides the core functionality for the mod-plsql simulator by:
 * 1. Initializing the HTP buffer before procedure execution
 * 2. Executing the target procedure with parameters
 * 3. Retrieving the generated HTML content from the HTP buffer
 */
public class ModPlsqlExecutor {

  /**
   * Initializes the HTP buffer by calling SYS.HTP_init().
   * This creates a fresh temporary table for HTML content generation.
   */
  public static void initializeHtpBuffer(Connection conn) throws SQLException {
    try (CallableStatement stmt = conn.prepareCall("CALL SYS.HTP_init()")) {
      stmt.execute();
    }
  }

  /**
   * Executes a PostgreSQL procedure with HTP support and returns the generated HTML.
   * 
   * @param conn Database connection
   * @param procedureName Fully qualified PostgreSQL procedure name (e.g., "SCHEMA.PACKAGE_procedure")
   * @param parameters Map of parameter names to values
   * @return Generated HTML content from HTP buffer
   * @throws SQLException If database operation fails
   */
  public static String executeProcedureWithHtp(Connection conn, String procedureName, 
                                               Map<String, String> parameters) throws SQLException {
    // Execute the procedure with parameters
    executeProcedure(conn, procedureName, parameters);
    
    // Retrieve and return the generated HTML
    return getHtmlFromBuffer(conn);
  }

  /**
   * Executes a PostgreSQL procedure with the provided parameters.
   * Parameters are passed as strings and the database handles type conversion.
   */
  private static void executeProcedure(Connection conn, String procedureName, 
                                       Map<String, String> parameters) throws SQLException {
    if (parameters.isEmpty()) {
      // Simple case: no parameters
      try (CallableStatement stmt = conn.prepareCall("CALL " + procedureName + "()")) {
        stmt.execute();
      }
    } else {
      // Build parameterized call
      StringJoiner placeholders = new StringJoiner(", ");
      for (int i = 0; i < parameters.size(); i++) {
        placeholders.add("?");
      }
      
      String sql = "CALL " + procedureName + "(" + placeholders.toString() + ")";
      
      try (CallableStatement stmt = conn.prepareCall(sql)) {
        // Set parameters in order (note: parameter order matters!)
        int paramIndex = 1;
        for (String value : parameters.values()) {
          if (value == null || value.trim().isEmpty()) {
            stmt.setNull(paramIndex, java.sql.Types.VARCHAR);
          } else {
            stmt.setString(paramIndex, value);
          }
          paramIndex++;
        }
        
        stmt.execute();
      }
    }
  }

  /**
   * Retrieves the complete HTML content from the HTP buffer.
   * Calls SYS.HTP_page() to get the concatenated HTML output.
   */
  private static String getHtmlFromBuffer(Connection conn) throws SQLException {
    try (PreparedStatement stmt = conn.prepareStatement("SELECT SYS.HTP_page()")) {
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          String html = rs.getString(1);
          return html != null ? html : "";
        }
        return "";
      }
    }
  }

  /**
   * Advanced version that supports named parameter mapping.
   * This method attempts to match procedure parameter names with provided values.
   * 
   * Note: This requires procedure metadata which may not always be available.
   * The basic version above uses positional parameters which is more reliable.
   */
  public static String executeProcedureWithNamedParameters(Connection conn, String procedureName, 
                                                           Map<String, String> namedParameters,
                                                           String[] expectedParameterOrder) throws SQLException {
    // Build parameter array in expected order
    String[] orderedValues = new String[expectedParameterOrder.length];
    for (int i = 0; i < expectedParameterOrder.length; i++) {
      String paramName = expectedParameterOrder[i];
      orderedValues[i] = namedParameters.getOrDefault(paramName, null);
    }

    // Convert to simple map with positional keys
    Map<String, String> positionalParams = new java.util.LinkedHashMap<>();
    for (int i = 0; i < orderedValues.length; i++) {
      positionalParams.put("param" + i, orderedValues[i]);
    }

    return executeProcedureWithHtp(conn, procedureName, positionalParams);
  }

  /**
   * Utility method to flush the HTP buffer (clear contents).
   * Useful for testing or error recovery scenarios.
   */
  public static void flushHtpBuffer(Connection conn) throws SQLException {
    try (CallableStatement stmt = conn.prepareCall("CALL SYS.HTP_flush()")) {
      stmt.execute();
    }
  }

  /**
   * Gets the current size of the HTP buffer.
   * Useful for debugging or monitoring HTML generation.
   */
  public static int getHtpBufferSize(Connection conn) throws SQLException {
    try (PreparedStatement stmt = conn.prepareStatement("SELECT SYS.HTP_buffer_size()")) {
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
        return 0;
      }
    }
  }
}