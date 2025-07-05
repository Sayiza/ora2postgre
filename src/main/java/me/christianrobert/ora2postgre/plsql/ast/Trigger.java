package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class Trigger extends PlSqlAst {
    private String triggerName;
    private String tableName;
    private String tableOwner;
    private String triggerType;        // BEFORE/AFTER/INSTEAD OF
    private String triggeringEvent;    // INSERT/UPDATE/DELETE (comma separated)
    private String whenClause;
    private String referencingClause;
    private List<String> updateColumns; // For UPDATE OF column_list
    private List<Statement> triggerBody;      // Parsed trigger body statements
    private String schema;             // Owner schema
    private String status;             // ENABLED/DISABLED

    public Trigger(String triggerName, String tableName, String tableOwner, String schema) {
        this.triggerName = triggerName;
        this.tableName = tableName;
        this.tableOwner = tableOwner;
        this.schema = schema;
        this.triggerType = "";
        this.triggeringEvent = "";
        this.whenClause = "";
        this.referencingClause = "";
        this.updateColumns = new ArrayList<>();
        this.triggerBody = new ArrayList<>();
        this.status = "ENABLED";
    }

    // Getters and setters
    public String getTriggerName() {
        return triggerName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getTableOwner() {
        return tableOwner;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggeringEvent() {
        return triggeringEvent;
    }

    public void setTriggeringEvent(String triggeringEvent) {
        this.triggeringEvent = triggeringEvent;
    }

    public String getWhenClause() {
        return whenClause;
    }

    public void setWhenClause(String whenClause) {
        this.whenClause = whenClause;
    }

    public String getReferencingClause() {
        return referencingClause;
    }

    public void setReferencingClause(String referencingClause) {
        this.referencingClause = referencingClause;
    }

    public List<String> getUpdateColumns() {
        return updateColumns;
    }

    public void setUpdateColumns(List<String> updateColumns) {
        this.updateColumns = updateColumns;
    }

    public List<Statement> getTriggerBody() {
        return triggerBody;
    }

    public void setTriggerBody(List<Statement> triggerBody) {
        this.triggerBody = triggerBody;
    }

    public String getSchema() {
        return schema;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public <T> T accept(PlSqlAstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /**
     * Transforms Oracle trigger to PostgreSQL trigger function and trigger definition.
     * This is the main transformation method that generates PostgreSQL-compatible SQL.
     */
    public String toPostgre(Everything everything) {
        StringBuilder result = new StringBuilder();
        
        // Generate PostgreSQL function first
        result.append(toPostgreTriggerFunction(everything));
        result.append("\n\n");
        
        // Generate PostgreSQL trigger definition
        result.append(toPostgreTriggerDefinition(everything));
        
        return result.toString();
    }

    /**
     * Generates PostgreSQL trigger function that contains the trigger logic.
     */
    public String toPostgreTriggerFunction(Everything everything) {
        StringBuilder function = new StringBuilder();
        
        String functionName = getPostgreFunctionName();
        
        function.append("CREATE OR REPLACE FUNCTION ")
                .append(PostgreSqlIdentifierUtils.quoteIdentifier(functionName))
                .append("() RETURNS TRIGGER AS $$\n")
                .append("BEGIN\n");
        
        // Add trigger logic based on triggering events
        List<String> events = getTriggeringEvents();
        
        if (events.size() == 1) {
            // Single event trigger
            function.append("  -- Single event trigger: ").append(events.get(0)).append("\n");
            function.append(generateTriggerLogic(events.get(0), everything));
        } else {
            // Multiple events trigger - use TG_OP branching
            function.append("  -- Multiple events trigger\n");
            for (int i = 0; i < events.size(); i++) {
                String event = events.get(i);
                if (i == 0) {
                    function.append("  IF TG_OP = '").append(event).append("' THEN\n");
                } else {
                    function.append("  ELSIF TG_OP = '").append(event).append("' THEN\n");
                }
                function.append(generateTriggerLogic(event, everything));
            }
            function.append("  END IF;\n");
        }
        
        // Add return statement
        function.append(generateReturnStatement());
        
        function.append("END;\n")
                .append("$$ LANGUAGE plpgsql;\n");
        
        return function.toString();
    }

    /**
     * Generates PostgreSQL CREATE TRIGGER statement.
     */
    public String toPostgreTriggerDefinition(Everything everything) {
        StringBuilder trigger = new StringBuilder();
        
        String pgTriggerName = getPostgreTriggerName();
        String functionName = getPostgreFunctionName();
        
        trigger.append("CREATE TRIGGER ")
                .append(PostgreSqlIdentifierUtils.quoteIdentifier(pgTriggerName))
                .append("\n  ").append(getPostgreTimingClause())
                .append(" ").append(getPostgreEventList())
                .append("\n  ON ").append(tableOwner.toLowerCase())
                .append(".").append(PostgreSqlIdentifierUtils.quoteIdentifier(tableName))
                .append("\n  FOR EACH ROW");
        
        // Add WHEN clause if present
        if (whenClause != null && !whenClause.trim().isEmpty()) {
            trigger.append("\n  WHEN (").append(transformWhenClause(whenClause, everything)).append(")");
        }
        
        trigger.append("\n  EXECUTE FUNCTION ")
                .append(PostgreSqlIdentifierUtils.quoteIdentifier(functionName))
                .append("();\n");
        
        return trigger.toString();
    }

    /**
     * Helper method to get PostgreSQL trigger timing (BEFORE/AFTER/INSTEAD OF).
     */
    public String getPostgreTimingClause() {
        String upperType = triggerType.toUpperCase();
        if (upperType.contains("BEFORE")) {
            return "BEFORE";
        } else if (upperType.contains("AFTER")) {
            return "AFTER";
        } else if (upperType.contains("INSTEAD OF")) {
            return "INSTEAD OF";
        }
        return "BEFORE"; // Default fallback
    }

    /**
     * Helper method to get PostgreSQL event list (INSERT OR UPDATE OR DELETE).
     */
    public String getPostgreEventList() {
        return triggeringEvent.toUpperCase().replace(",", " OR ");
    }

    /**
     * Helper method to get triggering events as a list.
     */
    public List<String> getTriggeringEvents() {
        List<String> events = new ArrayList<>();
        if (triggeringEvent != null && !triggeringEvent.trim().isEmpty()) {
            String[] eventArray = triggeringEvent.split(",");
            for (String event : eventArray) {
                events.add(event.trim().toUpperCase());
            }
        }
        return events;
    }

    /**
     * Helper method to determine if this is a row-level trigger.
     */
    public boolean needsRowLevelTrigger() {
        // For now, assume all triggers are row-level
        // This can be enhanced based on trigger analysis
        return true;
    }

    /**
     * Generate PostgreSQL function name based on trigger name.
     */
    private String getPostgreFunctionName() {
        return schema.toLowerCase() + "_" + triggerName.toLowerCase() + "_func";
    }

    /**
     * Generate PostgreSQL trigger name based on Oracle trigger name.
     */
    private String getPostgreTriggerName() {
        return triggerName.toLowerCase() + "_pg";
    }

    /**
     * Generate trigger logic for a specific event.
     */
    private String generateTriggerLogic(String event, Everything everything) {
        StringBuilder logic = new StringBuilder();
        
        logic.append("    -- TODO: Transform trigger body statements\n");
        logic.append("    -- Original Oracle trigger logic goes here\n");
        logic.append("    -- Event: ").append(event).append("\n");
        
        // For now, add placeholder logic
        // This will be enhanced in Phase 4 with actual statement transformation
        for (Statement stmt : triggerBody) {
            logic.append("    -- Statement: ").append(stmt.getClass().getSimpleName()).append("\n");
        }
        
        return logic.toString();
    }

    /**
     * Generate appropriate return statement based on trigger type.
     */
    private String generateReturnStatement() {
        StringBuilder returnStmt = new StringBuilder();
        
        List<String> events = getTriggeringEvents();
        if (events.contains("DELETE")) {
            returnStmt.append("  RETURN OLD;\n");
        } else {
            returnStmt.append("  RETURN NEW;\n");
        }
        
        return returnStmt.toString();
    }

    /**
     * Transform Oracle WHEN clause to PostgreSQL equivalent.
     */
    private String transformWhenClause(String oracleWhen, Everything everything) {
        // Basic transformation - replace :NEW with NEW and :OLD with OLD
        String pgWhen = oracleWhen;
        pgWhen = pgWhen.replaceAll(":NEW\\.", "NEW.");
        pgWhen = pgWhen.replaceAll(":OLD\\.", "OLD.");
        
        // Additional transformations can be added here
        return pgWhen;
    }

    @Override
    public String toString() {
        return "Trigger{" +
                "triggerName='" + triggerName + '\'' +
                ", tableName='" + tableName + '\'' +
                ", triggerType='" + triggerType + '\'' +
                ", triggeringEvent='" + triggeringEvent + '\'' +
                ", schema='" + schema + '\'' +
                ", bodyStatements=" + triggerBody.size() +
                '}';
    }
}