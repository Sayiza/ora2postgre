package me.christianrobert.ora2postgre.oracledb;

import me.christianrobert.ora2postgre.oracledb.tools.UserExcluder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public class TableExtractor {

  private static final Logger log = LoggerFactory.getLogger(TableExtractor.class);

  public static List<TableMetadata> extractAllTables(Connection oracleConn, List<String> users) throws SQLException {
    List<TableMetadata> tableMetadataList = new ArrayList<>();

    for (String user : users) {
      if (UserExcluder.is2BeExclueded(user)) {
        continue;
      }

      List<String> tables = fetchTableNames(oracleConn, user);

      for (String table : tables) {
        //if (table.matches("SYS_IOT_OVER_.*|BIN\\$.*|BW_STUDIUM_SEM_CFG_BAK\\$.*|DR\\$.*|MLOG\\$_.*|RUPD\\$_.*|AQ\\$.*|QUEUE_TABLE.*|ISEQ\\$\\$_.*|SYS_LOB.*|LOB\\$.*|WRI\\$_.*|SHSPACE.*|SQL\\$.*")) {
        //  continue; // Skip internal/system tables
        //}

        // Check if table is global temporary
        if (isGlobalTemporaryTable(oracleConn, user, table)) {
          continue;
        }

        TableMetadata tableMetadata =
                fetchTableMetadata(oracleConn, user, table);
        tableMetadataList.add(tableMetadata);
      }
      log.info("Extracted tables from schema {}", user);
    }
    return tableMetadataList;
  }

  private static List<String> fetchTableNames(Connection oracleConn, String owner) throws SQLException {
    List<String> result = new ArrayList<>();
    String sql = "SELECT table_name FROM all_tables WHERE owner = ? ORDER BY table_name";

    try (PreparedStatement ps = oracleConn.prepareStatement(sql)) {
      ps.setString(1, owner.toUpperCase());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(rs.getString("table_name"));
        }
      }
    }
    return result;
  }

  private static boolean isGlobalTemporaryTable(Connection oracleConn, String owner, String table) throws SQLException {
    String sql = "SELECT temporary FROM all_tables WHERE owner = ? AND table_name = ?";
    try (PreparedStatement ps = oracleConn.prepareStatement(sql)) {
      ps.setString(1, owner.toUpperCase());
      ps.setString(2, table);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return "Y".equals(rs.getString("temporary"));
        }
      }
    }
    return false;
  }

  private static TableMetadata fetchTableMetadata(Connection oracleConn, String owner, String table) throws SQLException {
    TableMetadata tableMetadata = new TableMetadata(owner, table);

    // Fetch column metadata (exclude hidden, virtual, and system-generated columns)
    String columnSql = "SELECT column_name, data_type, char_length, data_precision, data_scale, nullable, data_default " +
            "FROM all_tab_cols WHERE owner = ? AND table_name = ? " +
            "AND hidden_column = 'NO' AND virtual_column = 'NO' AND user_generated = 'YES' " +
            "ORDER BY column_id";
    try (PreparedStatement ps = oracleConn.prepareStatement(columnSql)) {
      ps.setString(1, owner.toUpperCase());
      ps.setString(2, table);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String columnName = rs.getString("column_name");
          String dataType = rs.getString("data_type");
          Integer charLength = rs.getInt("char_length");
          if (rs.wasNull()) charLength = null;
          Integer precision = rs.getInt("data_precision");
          if (rs.wasNull()) precision = null;
          Integer scale = rs.getInt("data_scale");
          if (rs.wasNull()) scale = null;
          boolean nullable = "Y".equals(rs.getString("nullable"));
          String defaultValue = rs.getString("data_default");
          if (defaultValue != null) {
            defaultValue = defaultValue.trim();
          }

          ColumnMetadata column = new ColumnMetadata(columnName, dataType, charLength, precision, scale, nullable, defaultValue);
          tableMetadata.addColumn(column);
        }
      }
    }

    // Fetch primary key constraints
    String constraintSql = "SELECT ac.constraint_name, ac.constraint_type " +
            "FROM all_constraints ac " +
            "WHERE ac.owner = ? AND ac.table_name = ? AND ac.constraint_type = 'P'";
    try (PreparedStatement ps = oracleConn.prepareStatement(constraintSql)) {
      ps.setString(1, owner.toUpperCase());
      ps.setString(2, table);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String constraintName = rs.getString("constraint_name");
          String constraintType = rs.getString("constraint_type");
          ConstraintMetadata constraint = new ConstraintMetadata(constraintName, constraintType);
          tableMetadata.addConstraint(constraint);

          // Fetch columns for this constraint
          String consColsSql = "SELECT column_name FROM all_cons_columns " +
                  "WHERE owner = ? AND table_name = ? AND constraint_name = ? ORDER BY position";
          try (PreparedStatement psCols = oracleConn.prepareStatement(consColsSql)) {
            psCols.setString(1, owner.toUpperCase());
            psCols.setString(2, table);
            psCols.setString(3, constraintName);
            try (ResultSet rsCols = psCols.executeQuery()) {
              while (rsCols.next()) {
                constraint.addColumnName(rsCols.getString("column_name"));
              }
            }
          }
        }
      }
    }

    return tableMetadata;
  }
}