package com.sayiza.oracle2postgre.transfer;

import com.sayiza.oracle2postgre.oracledb.ColumnMetadata;
import com.sayiza.oracle2postgre.plsql.ast.tools.TypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.*;

/**
 * Unified parameter setting utility for PostgreSQL PreparedStatements.
 * 
 * This class handles setting parameters for both regular Oracle data types
 * and complex Oracle data types (BLOB, CLOB, RAW, XMLTYPE, etc.) in a 
 * consistent manner across all transfer strategies.
 * 
 * Centralizes the logic for Oracle → PostgreSQL data type conversion
 * and parameter setting to avoid code duplication between strategies.
 */
public class ParameterSetter {
    
    private static final Logger log = LoggerFactory.getLogger(ParameterSetter.class);
    
    /**
     * Sets a parameter in a PostgreSQL PreparedStatement for any Oracle data type.
     * 
     * Handles both primitive types (VARCHAR2, NUMBER, DATE) and complex types
     * (BLOB, CLOB, RAW, XMLTYPE, TIMESTAMP WITH TIME ZONE, etc.) using the
     * appropriate JDBC methods and PostgreSQL type conversion.
     * 
     * @param stmt The PreparedStatement to set the parameter in
     * @param paramIndex The parameter index (1-based)
     * @param rs The Oracle ResultSet to read the value from
     * @param column The column metadata containing Oracle type information
     * @throws SQLException if parameter setting fails
     */
    public static void setParameter(PreparedStatement stmt, int paramIndex, 
                                  ResultSet rs, ColumnMetadata column) throws SQLException {
        String columnName = column.getColumnName();
        String oracleDataType = column.getDataType().toUpperCase();
        String postgresDataType = TypeConverter.toPostgre(oracleDataType.toLowerCase());
        
        Object value = rs.getObject(columnName);
        if (value == null) {
            stmt.setNull(paramIndex, java.sql.Types.NULL);
            return;
        }
        
        try {
            // Handle specific Oracle data types
            switch (oracleDataType) {
                // Complex Large Object types
                case "BLOB":
                    setBlobParameter(stmt, paramIndex, rs, columnName);
                    break;
                    
                case "CLOB":
                case "NCLOB":
                    setClobParameter(stmt, paramIndex, rs, columnName);
                    break;
                    
                // Binary data types
                case "RAW":
                case "LONG RAW":
                    setRawParameter(stmt, paramIndex, rs, columnName);
                    break;
                    
                // XML type
                case "XMLTYPE":
                    setXmlTypeParameter(stmt, paramIndex, rs, columnName);
                    break;
                    
                // File reference type
                case "BFILE":
                    setBFileParameter(stmt, paramIndex, rs, columnName);
                    break;
                    
                // Oracle ANYDATA type - convert to JSONB
                case "ANYDATA":
                    setAnydataParameter(stmt, paramIndex, rs, columnName);
                    break;
                    
                // Oracle AQ JMS message types - convert to JSONB
                case "AQ$_JMS_TEXT_MESSAGE":
                case "SYS.AQ$_JMS_TEXT_MESSAGE":
                    setAqJmsMessageParameter(stmt, paramIndex, rs, columnName);
                    break;
                    
                // Oracle AQ signature property types - convert to JSONB
                case "AQ$_SIG_PROP":
                case "SYS.AQ$_SIG_PROP":
                    setAqSigPropParameter(stmt, paramIndex, rs, columnName);
                    break;
                    
                // Oracle AQ recipients types - convert to JSONB
                case "AQ$_RECIPIENTS":
                case "SYS.AQ$_RECIPIENTS":
                    setAqRecipientsParameter(stmt, paramIndex, rs, columnName);
                    break;
                    
                // Extended timestamp types
                case "TIMESTAMP WITH TIME ZONE":
                case "TIMESTAMP WITH LOCAL TIME ZONE":
                    setTimestampWithTimeZoneParameter(stmt, paramIndex, rs, columnName);
                    break;
                    
                // Interval types
                case "INTERVAL YEAR TO MONTH":
                case "INTERVAL DAY TO SECOND":
                    setIntervalParameter(stmt, paramIndex, rs, columnName);
                    break;
                    
                // Primitive types and other complex types handled with unified logic
                default:
                    if (oracleDataType.startsWith("TIMESTAMP WITH")) {
                        setTimestampWithTimeZoneParameter(stmt, paramIndex, rs, columnName);
                    } else if (oracleDataType.startsWith("INTERVAL")) {
                        setIntervalParameter(stmt, paramIndex, rs, columnName);
                    } else if (oracleDataType.contains("AQ$_JMS_TEXT_MESSAGE")) {
                        setAqJmsMessageParameter(stmt, paramIndex, rs, columnName);
                    } else if (oracleDataType.contains("AQ$_SIG_PROP")) {
                        setAqSigPropParameter(stmt, paramIndex, rs, columnName);
                    } else if (oracleDataType.contains("AQ$_RECIPIENTS")) {
                        setAqRecipientsParameter(stmt, paramIndex, rs, columnName);
                    } else {
                        // Handle primitive types and unknown types
                        setPrimitiveParameter(stmt, paramIndex, rs, column, oracleDataType);
                    }
                    break;
            }
            
        } catch (SQLException e) {
            log.error("Failed to set parameter for column {} (Oracle type: {}, PostgreSQL type: {}) at index {}: {}", 
                    columnName, oracleDataType, postgresDataType, paramIndex, e.getMessage(), e);
            throw new SQLException("Failed to set parameter for column " + columnName + 
                    " (type: " + oracleDataType + ")", e);
        }
    }
    
    /**
     * Sets a BLOB parameter, handling binary stream data.
     */
    private static void setBlobParameter(PreparedStatement stmt, int paramIndex, 
                                       ResultSet rs, String columnName) throws SQLException {
        Blob blob = rs.getBlob(columnName);
        if (blob != null) {
            try {
                InputStream inputStream = blob.getBinaryStream();
                stmt.setBinaryStream(paramIndex, inputStream, blob.length());
                // Note: InputStream will be closed when PreparedStatement is executed/closed
                // Do NOT close it here as it causes "connection has been closed" errors
            } catch (Exception e) {
                throw new SQLException("Failed to handle BLOB for column " + columnName, e);
            }
        } else {
            stmt.setNull(paramIndex, Types.BINARY);
        }
    }
    
    /**
     * Sets a CLOB parameter, handling large text data.
     */
    private static void setClobParameter(PreparedStatement stmt, int paramIndex, 
                                       ResultSet rs, String columnName) throws SQLException {
        Clob clob = rs.getClob(columnName);
        if (clob != null) {
            try {
                String clobText = clob.getSubString(1, (int) clob.length());
                stmt.setString(paramIndex, clobText);
            } catch (Exception e) {
                throw new SQLException("Failed to handle CLOB for column " + columnName, e);
            }
        } else {
            stmt.setNull(paramIndex, Types.CLOB);
        }
    }
    
    /**
     * Sets a RAW parameter, handling binary data.
     */
    private static void setRawParameter(PreparedStatement stmt, int paramIndex, 
                                      ResultSet rs, String columnName) throws SQLException {
        byte[] rawData = rs.getBytes(columnName);
        if (rawData != null) {
            stmt.setBytes(paramIndex, rawData);
        } else {
            stmt.setNull(paramIndex, Types.BINARY);
        }
    }
    
    /**
     * Sets an XMLTYPE parameter.
     */
    private static void setXmlTypeParameter(PreparedStatement stmt, int paramIndex, 
                                          ResultSet rs, String columnName) throws SQLException {
        // XMLTYPE is typically retrieved as a string representation
        String xmlString = rs.getString(columnName);
        if (xmlString != null) {
            // PostgreSQL expects XML type as string
            stmt.setObject(paramIndex, xmlString, Types.SQLXML);
        } else {
            stmt.setNull(paramIndex, Types.SQLXML);
        }
    }
    
    /**
     * Sets a BFILE parameter (file reference).
     */
    private static void setBFileParameter(PreparedStatement stmt, int paramIndex, 
                                        ResultSet rs, String columnName) throws SQLException {
        // BFILE typically maps to text in PostgreSQL (file path reference)
        String bfileString = rs.getString(columnName);
        stmt.setString(paramIndex, bfileString);
    }
    
    /**
     * Sets an ANYDATA parameter, converting it to JSONB format.
     * Uses AnydataConverter to extract type information and convert to structured JSON.
     */
    private static void setAnydataParameter(PreparedStatement stmt, int paramIndex, 
                                          ResultSet rs, String columnName) throws SQLException {
        try {
            // Use AnydataConverter to convert ANYDATA to JSON
            String jsonValue = AnydataConverter.convertAnydataToJson(rs, columnName);
            
            if (jsonValue != null) {
                // Set as JSONB parameter (requires PostgreSQL JDBC driver support)
                stmt.setObject(paramIndex, jsonValue, Types.OTHER);
            } else {
                stmt.setNull(paramIndex, Types.OTHER);
            }
            
        } catch (Exception e) {
            throw new SQLException("Failed to convert ANYDATA for column " + columnName, e);
        }
    }
    
    /**
     * Sets an AQ$_JMS_TEXT_MESSAGE parameter by converting to JSONB.
     */
    private static void setAqJmsMessageParameter(PreparedStatement stmt, int paramIndex, 
                                               ResultSet rs, String columnName) throws SQLException {
        try {
            // Use AqJmsMessageConverter to convert AQ message to JSON
            String jsonValue = AqJmsMessageConverter.convertToJson(rs, columnName);
            
            if (jsonValue != null) {
                // Set as JSONB parameter (requires PostgreSQL JDBC driver support)
                stmt.setObject(paramIndex, jsonValue, Types.OTHER);
            } else {
                stmt.setNull(paramIndex, Types.OTHER);
            }
            
        } catch (Exception e) {
            throw new SQLException("Failed to convert AQ JMS message for column " + columnName, e);
        }
    }
    
    /**
     * Sets an AQ$_SIG_PROP parameter by converting to JSONB.
     */
    private static void setAqSigPropParameter(PreparedStatement stmt, int paramIndex, 
                                            ResultSet rs, String columnName) throws SQLException {
        try {
            // Use AqSigPropConverter to convert AQ signature property to JSON
            String jsonValue = AqSigPropConverter.convertToJson(rs, columnName);
            
            if (jsonValue != null) {
                // Set as JSONB parameter (requires PostgreSQL JDBC driver support)
                stmt.setObject(paramIndex, jsonValue, Types.OTHER);
            } else {
                stmt.setNull(paramIndex, Types.OTHER);
            }
            
        } catch (Exception e) {
            throw new SQLException("Failed to convert AQ signature property for column " + columnName, e);
        }
    }
    
    /**
     * Sets an AQ$_RECIPIENTS parameter by converting to JSONB.
     */
    private static void setAqRecipientsParameter(PreparedStatement stmt, int paramIndex, 
                                               ResultSet rs, String columnName) throws SQLException {
        try {
            // Use AqRecipientsConverter to convert AQ recipients to JSON
            String jsonValue = AqRecipientsConverter.convertToJson(rs, columnName);
            
            if (jsonValue != null) {
                // Set as JSONB parameter (requires PostgreSQL JDBC driver support)
                stmt.setObject(paramIndex, jsonValue, Types.OTHER);
            } else {
                stmt.setNull(paramIndex, Types.OTHER);
            }
            
        } catch (Exception e) {
            throw new SQLException("Failed to convert AQ recipients for column " + columnName, e);
        }
    }
    
    /**
     * Sets a TIMESTAMP WITH TIME ZONE parameter.
     */
    private static void setTimestampWithTimeZoneParameter(PreparedStatement stmt, int paramIndex, 
                                                        ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        if (timestamp != null) {
            stmt.setTimestamp(paramIndex, timestamp);
        } else {
            stmt.setNull(paramIndex, Types.TIMESTAMP_WITH_TIMEZONE);
        }
    }
    
    /**
     * Sets an INTERVAL parameter.
     */
    private static void setIntervalParameter(PreparedStatement stmt, int paramIndex, 
                                           ResultSet rs, String columnName) throws SQLException {
        // Oracle INTERVAL types are typically retrieved as strings
        String intervalString = rs.getString(columnName);
        if (intervalString != null) {
            // PostgreSQL can parse interval strings directly
            stmt.setObject(paramIndex, intervalString, Types.OTHER);
        } else {
            stmt.setNull(paramIndex, Types.OTHER);
        }
    }
    
    /**
     * Sets a primitive parameter (VARCHAR2, NUMBER, DATE, etc.).
     */
    private static void setPrimitiveParameter(PreparedStatement stmt, int paramIndex, 
                                            ResultSet rs, ColumnMetadata column, String oracleDataType) throws SQLException {
        String columnName = column.getColumnName();
        
        // Handle primitive Oracle data types
        if (oracleDataType.contains("CHAR") || oracleDataType.contains("CLOB")) {
            // String types - but CLOB should have been handled above
            String stringValue = rs.getString(columnName);
            stmt.setString(paramIndex, stringValue);
            
        } else if (oracleDataType.equals("NUMBER") || oracleDataType.equals("INTEGER") || oracleDataType.equals("FLOAT")) {
            // Numeric types
            stmt.setBigDecimal(paramIndex, rs.getBigDecimal(columnName));
            
        } else if (oracleDataType.equals("DATE") || oracleDataType.contains("TIMESTAMP")) {
            // Date/time types (but complex timestamps should have been handled above)
            stmt.setTimestamp(paramIndex, rs.getTimestamp(columnName));
            
        } else {
            // Unknown/other types - treat as string
            log.debug("Unknown Oracle data type '{}' for column '{}', treating as string", oracleDataType, columnName);
            String stringValue = rs.getString(columnName);
            stmt.setString(paramIndex, stringValue);
        }
    }
}