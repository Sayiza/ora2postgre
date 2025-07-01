package com.sayiza.oracle2postgre.transfer;

import com.sayiza.oracle2postgre.oracledb.ColumnMetadata;
import com.sayiza.oracle2postgre.oracledb.TableMetadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataExtractor {

  /**
   * Formats a single column value from the ResultSet based on its data type.
   */
  public static String formatValue(ResultSet rs, ColumnMetadata col) throws SQLException {
    String colName = col.getColumnName();
    String dataType = col.getDataType().toUpperCase();

    // Handle NULL values
    if (rs.getObject(colName) == null) {
      return "NULL";
    }

    // Handle different data types
    if (dataType.contains("CHAR") || dataType.contains("CLOB")) {
      String value = rs.getString(colName);
      // Escape single quotes and wrap in quotes
      return "'" + value.replace("'", "''") + "'";
    } else if (dataType.equals("NUMBER") || dataType.equals("INTEGER") || dataType.equals("FLOAT")) {
      // Numbers can be written as-is
      return rs.getString(colName);
    } else if (dataType.equals("DATE") || dataType.contains("TIMESTAMP")) {
      // Format dates/timestamps for PostgreSQL
      java.sql.Timestamp timestamp = rs.getTimestamp(colName);
      return "'" + timestamp.toString() + "'";
    } else if (dataType.equals("BLOB") || dataType.equals("RAW")) {
      // For prototype, convert BLOB/RAW to hex string
      byte[] bytes = rs.getBytes(colName);
      return "decode('" + bytesToHex(bytes) + "', 'hex')";
    } else {
      // Fallback: treat as string with escaping
      String value = rs.getString(colName);
      return "'" + value.replace("'", "''") + "'";
    }
  }


  /**
   * Converts a byte array to a hexadecimal string for PostgreSQL decode function.
   */
  private static String bytesToHex(byte[] bytes) {
    StringBuilder hex = new StringBuilder();
    for (byte b : bytes) {
      hex.append(String.format("%02x", b));
    }
    return hex.toString();
  }
}