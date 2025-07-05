package me.christianrobert.ora2postgre.oracledb;

import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.oracledb.tools.UserExcluder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TriggerExtractor {

  private static final Logger log = LoggerFactory.getLogger(TriggerExtractor.class);

  /**
   * Extracts PL/SQL code for all triggers in the specified schemas from an Oracle database.
   * Converts trigger metadata to PlsqlCode objects for further AST processing.
   *
   * @param oracleConn Oracle database connection
   * @param users      List of schema names (Oracle users)
   * @return List of PlsqlCode objects containing trigger source code
   * @throws SQLException if database operations fail
   */
  public static List<PlsqlCode> extract(Connection oracleConn, List<String> users) throws SQLException {
    List<PlsqlCode> triggerPlsqlList = new ArrayList<>();

    for (String user : users) {
      if (UserExcluder.is2BeExclueded(user)) {
        continue;
      }

      List<TriggerMetadata> triggers = extractTriggersForSchema(oracleConn, user);

      for (TriggerMetadata trigger : triggers) {
        // Convert trigger metadata to PlsqlCode for AST processing
        String fullTriggerCode = buildFullTriggerCode(trigger);
        PlsqlCode triggerCode = new PlsqlCode(trigger.getSchema(), fullTriggerCode);
        triggerPlsqlList.add(triggerCode);
      }
      
      log.info("Extracted {} triggers from schema {}", triggers.size(), user);
    }
    
    log.info("Total triggers extracted: {}", triggerPlsqlList.size());
    return triggerPlsqlList;
  }

  /**
   * Extracts metadata for all triggers in a specific schema.
   *
   * @param oracleConn Oracle database connection
   * @param owner      Schema name (Oracle user)
   * @return List of TriggerMetadata objects
   * @throws SQLException if database operations fail
   */
  public static List<TriggerMetadata> extractTriggersForSchema(Connection oracleConn, String owner) throws SQLException {
    List<TriggerMetadata> triggerMetadataList = new ArrayList<>();

    List<String> triggerNames = fetchTriggerNames(oracleConn, owner);

    for (String triggerName : triggerNames) {
      // Skip system triggers
      if (triggerName.matches("SYS_.*|BIN\\$.*")) {
        continue;
      }

      TriggerMetadata triggerMetadata = fetchTriggerMetadata(oracleConn, owner, triggerName);
      triggerMetadataList.add(triggerMetadata);
    }

    return triggerMetadataList;
  }

  /**
   * Fetches trigger names for a given schema from all_triggers.
   */
  private static List<String> fetchTriggerNames(Connection oracleConn, String owner) throws SQLException {
    List<String> result = new ArrayList<>();
    String sql = "SELECT trigger_name FROM all_triggers WHERE owner = ? ORDER BY trigger_name";

    try (PreparedStatement ps = oracleConn.prepareStatement(sql)) {
      ps.setString(1, owner.toUpperCase());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(rs.getString("trigger_name"));
        }
      }
    }
    return result;
  }

  /**
   * Fetches metadata for a single trigger, including all trigger properties and source code.
   */
  private static TriggerMetadata fetchTriggerMetadata(Connection oracleConn, String owner, String triggerName) throws SQLException {
    TriggerMetadata triggerMetadata = new TriggerMetadata(owner, triggerName);

    String sql = """
        SELECT 
          trigger_type,
          triggering_event,
          table_name,
          table_owner,
          status,
          description,
          trigger_body
        FROM all_triggers 
        WHERE owner = ? AND trigger_name = ?
        """;

    try (PreparedStatement ps = oracleConn.prepareStatement(sql)) {
      ps.setString(1, owner.toUpperCase());
      ps.setString(2, triggerName);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          triggerMetadata.setTriggerType(rs.getString("trigger_type"));
          triggerMetadata.setTriggeringEvent(rs.getString("triggering_event"));
          triggerMetadata.setTableName(rs.getString("table_name"));
          triggerMetadata.setTableOwner(rs.getString("table_owner"));
          triggerMetadata.setStatus(rs.getString("status"));
          triggerMetadata.setDescription(rs.getString("description"));
          
          String triggerBody = rs.getString("trigger_body");
          triggerMetadata.setTriggerBody(triggerBody != null ? triggerBody.trim() : "");
        }
      }
    }

    return triggerMetadata;
  }

  /**
   * Builds the complete trigger code including CREATE OR REPLACE TRIGGER statement.
   * This reconstructs the full Oracle trigger definition for AST parsing.
   */
  private static String buildFullTriggerCode(TriggerMetadata trigger) {
    StringBuilder fullCode = new StringBuilder();
    
    fullCode.append("CREATE OR REPLACE TRIGGER ")
            .append(trigger.getSchema()).append(".").append(trigger.getTriggerName())
            .append("\n  ").append(trigger.getTriggerType())
            .append(" ").append(trigger.getTriggeringEvent())
            .append(" ON ").append(trigger.getTableOwner()).append(".").append(trigger.getTableName())
            .append("\n  FOR EACH ROW\n")
            .append(trigger.getTriggerBody());
    
    // Ensure proper termination
    if (!fullCode.toString().trim().endsWith(";")) {
      fullCode.append("\n;");
    }
    
    return fullCode.toString();
  }
}