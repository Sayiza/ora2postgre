package me.christianrobert.ora2postgre.oracledb;

import me.christianrobert.ora2postgre.oracledb.tools.UserExcluder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ViewExtractor {

  private static final Logger log = LoggerFactory.getLogger(ViewExtractor.class);

  /**
   * Extracts metadata for all views in the specified schemas from an Oracle database.
   *
   * @param oracleConn Oracle database connection
   * @param users      List of schema names (Oracle users)
   * @return List of ViewMetadata objects
   * @throws SQLException if database operations fail
   */
  public static List<ViewMetadata> extractAllViews(Connection oracleConn, List<String> users) throws SQLException {
    List<ViewMetadata> viewMetadataList = new ArrayList<>();

    for (String user : users) {
      if (UserExcluder.is2BeExclueded(user)) {
        continue;
      }

      List<String> views = fetchViewNames(oracleConn, user);

      for (String view : views) {
        // Skip system views
        if (view.matches("SYS_.*|V\\$.*|GV\\$.*|DBA_.*|ALL_.*|USER_.*")) {
          continue;
        }

        ViewMetadata viewMetadata = fetchViewMetadata(oracleConn, user, view);
        viewMetadataList.add(viewMetadata);
      }
      log.info("Extracted views from schema {}", user);
    }
    return viewMetadataList;
  }

  /**
   * Fetches view names for a given schema from all_views.
   */
  private static List<String> fetchViewNames(Connection oracleConn, String owner) throws SQLException {
    List<String> result = new ArrayList<>();
    String sql = "SELECT view_name FROM all_views WHERE owner = ? ORDER BY view_name";

    try (PreparedStatement ps = oracleConn.prepareStatement(sql)) {
      ps.setString(1, owner.toUpperCase());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(rs.getString("view_name"));
        }
      }
    }
    return result;
  }

  /**
   * Fetches metadata for a single view, including columns and raw query text.
   */
  private static ViewMetadata fetchViewMetadata(Connection oracleConn, String owner, String view) throws SQLException {
    ViewMetadata viewMetadata = new ViewMetadata(owner, view);

    // Fetch column metadata from all_tab_cols
    String columnSql = "SELECT column_name, data_type, char_length, data_precision, data_scale, nullable " +
                       "FROM all_tab_cols WHERE owner = ? AND table_name = ? ORDER BY column_id";
    try (PreparedStatement ps = oracleConn.prepareStatement(columnSql)) {
      ps.setString(1, owner.toUpperCase());
      ps.setString(2, view);
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

          ColumnMetadata column = new ColumnMetadata(columnName, dataType, charLength, precision, scale, nullable, null);
          viewMetadata.addColumn(column);
        }
      }
    }

    // Fetch raw query text from all_views
    String viewSql = "SELECT text FROM all_views WHERE owner = ? AND view_name = ?";
    try (PreparedStatement ps = oracleConn.prepareStatement(viewSql)) {
      ps.setString(1, owner.toUpperCase());
      ps.setString(2, view);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          String rawQuery = rs.getString("text");
          viewMetadata.setRawQuery(rawQuery != null ? rawQuery.trim() : "");
        }
      }
    }

    return viewMetadata;
  }
}