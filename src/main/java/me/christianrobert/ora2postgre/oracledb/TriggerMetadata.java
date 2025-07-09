package me.christianrobert.ora2postgre.oracledb;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.TriggerTransformationManager;

public class TriggerMetadata {
  private static final TriggerTransformationManager triggerManager = new TriggerTransformationManager();
  
  private String schema; // Oracle schema (user)
  private String triggerName;
  private String triggerType; // BEFORE/AFTER/INSTEAD OF
  private String triggeringEvent; // INSERT/UPDATE/DELETE or combination
  private String tableName; // Target table name
  private String tableOwner; // Owner of the target table
  private String status; // ENABLED/DISABLED
  private String description; // Trigger description if available
  private String triggerBody; // Raw PL/SQL code from user_triggers.trigger_body

  public TriggerMetadata(String schema, String triggerName) {
    this.schema = schema;
    this.triggerName = triggerName;
    this.triggerType = "";
    this.triggeringEvent = "";
    this.tableName = "";
    this.tableOwner = "";
    this.status = "";
    this.description = "";
    this.triggerBody = "";
  }

  // Getters and setters
  public String getSchema() { return schema; }
  public String getTriggerName() { return triggerName; }
  public String getTriggerType() { return triggerType; }
  public String getTriggeringEvent() { return triggeringEvent; }
  public String getTableName() { return tableName; }
  public String getTableOwner() { return tableOwner; }
  public String getStatus() { return status; }
  public String getDescription() { return description; }
  public String getTriggerBody() { return triggerBody; }

  public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
  public void setTriggeringEvent(String triggeringEvent) { this.triggeringEvent = triggeringEvent; }
  public void setTableName(String tableName) { this.tableName = tableName; }
  public void setTableOwner(String tableOwner) { this.tableOwner = tableOwner; }
  public void setStatus(String status) { this.status = status; }
  public void setDescription(String description) { this.description = description; }
  public void setTriggerBody(String triggerBody) { this.triggerBody = triggerBody; }

  @Override
  public String toString() {
    return "TriggerMetadata{" +
            "schema='" + schema + '\'' +
            ", triggerName='" + triggerName + '\'' +
            ", triggerType='" + triggerType + '\'' +
            ", triggeringEvent='" + triggeringEvent + '\'' +
            ", tableName='" + tableName + '\'' +
            ", tableOwner='" + tableOwner + '\'' +
            ", status='" + status + '\'' +
            ", triggerBodyLength=" + triggerBody.length() +
            '}';
  }

  /**
   * Generates PostgreSQL-compatible CREATE OR REPLACE FUNCTION statement for the trigger.
   * Oracle triggers become PostgreSQL functions that return TRIGGER.
   * 
   * @deprecated Use TriggerTransformationManager.transformTriggerFunctionStub() instead
   * @return PostgreSQL function stub
   */
  @Deprecated
  public String toPostgreFunctionStub() {
    return triggerManager.transformTriggerFunctionStub(this, null);
  }

  /**
   * Generates PostgreSQL-compatible CREATE OR REPLACE FUNCTION statement for the trigger.
   * Oracle triggers become PostgreSQL functions that return TRIGGER.
   * 
   * @param context The global context containing all migration data
   * @return PostgreSQL function stub
   */
  public String toPostgreFunctionStub(Everything context) {
    return triggerManager.transformTriggerFunctionStub(this, context);
  }

  /**
   * Generates PostgreSQL-compatible CREATE OR REPLACE FUNCTION statement for the trigger.
   * Oracle triggers become PostgreSQL functions that return TRIGGER.
   * 
   * Note: This is a placeholder implementation. The actual transformation will be done
   * in the AST transformation phase using the parsed trigger body.
   *
   * @return PostgreSQL function stub
   * @deprecated Use toPostgreFunctionStub(Everything) instead
   */
  @Deprecated
  public String toPostgreFunctionStubLegacy() {
    StringBuilder functionSql = new StringBuilder();
    
    String functionName = schema.toLowerCase() + "_" + triggerName.toLowerCase() + "_func";
    
    functionSql.append("CREATE OR REPLACE FUNCTION ")
            .append(PostgreSqlIdentifierUtils.quoteIdentifier(functionName))
            .append("() RETURNS TRIGGER AS $$\n")
            .append("BEGIN\n")
            .append("  -- TODO: Implement trigger logic from AST transformation\n")
            .append("  -- Original Oracle trigger: ").append(triggerName).append("\n")
            .append("  -- Target table: ").append(tableOwner).append(".").append(tableName).append("\n")
            .append("  -- Trigger type: ").append(triggerType).append(" ").append(triggeringEvent).append("\n")
            .append("  RETURN COALESCE(NEW, OLD);\n")
            .append("END;\n")
            .append("$$ LANGUAGE plpgsql;\n");
    
    return functionSql.toString();
  }

  /**
   * Generates PostgreSQL CREATE TRIGGER statement.
   * This creates the actual trigger that calls the function.
   *
   * @deprecated Use TriggerTransformationManager.transformTriggerDefinitionStub() instead
   * @return PostgreSQL trigger creation SQL
   */
  @Deprecated
  public String toPostgreTriggerStub() {
    return triggerManager.transformTriggerDefinitionStub(this, null);
  }

  /**
   * Generates PostgreSQL CREATE TRIGGER statement.
   * This creates the actual trigger that calls the function.
   *
   * @param context The global context containing all migration data
   * @return PostgreSQL trigger creation SQL
   */
  public String toPostgreTriggerStub(Everything context) {
    return triggerManager.transformTriggerDefinitionStub(this, context);
  }

  /**
   * Generates PostgreSQL CREATE TRIGGER statement.
   * This creates the actual trigger that calls the function.
   *
   * @return PostgreSQL trigger creation SQL
   * @deprecated Use toPostgreTriggerStub(Everything) instead
   */
  @Deprecated
  public String toPostgreTriggerStubLegacy() {
    StringBuilder triggerSql = new StringBuilder();
    
    String functionName = schema.toLowerCase() + "_" + triggerName.toLowerCase() + "_func";
    String pgTriggerName = triggerName.toLowerCase() + "_pg";
    
    // Convert Oracle trigger type to PostgreSQL
    String pgTiming = triggerType.toUpperCase().contains("BEFORE") ? "BEFORE" : 
                     triggerType.toUpperCase().contains("AFTER") ? "AFTER" : "INSTEAD OF";
    
    triggerSql.append("CREATE TRIGGER ")
            .append(PostgreSqlIdentifierUtils.quoteIdentifier(pgTriggerName))
            .append("\n  ").append(pgTiming)
            .append(" ").append(triggeringEvent.toUpperCase())
            .append("\n  ON ").append(tableOwner.toLowerCase())
            .append(".").append(PostgreSqlIdentifierUtils.quoteIdentifier(tableName))
            .append("\n  FOR EACH ROW")
            .append("\n  EXECUTE FUNCTION ").append(PostgreSqlIdentifierUtils.quoteIdentifier(functionName))
            .append("();\n");
    
    return triggerSql.toString();
  }
}