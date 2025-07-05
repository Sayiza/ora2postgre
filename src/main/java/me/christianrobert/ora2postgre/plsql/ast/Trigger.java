package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils;
import me.christianrobert.ora2postgre.plsql.ast.tools.TriggerTransformer;

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
        
        // Add WHEN clause if present or if UPDATE OF columns are specified
        String combinedWhenClause = buildCombinedWhenClause(everything);
        if (combinedWhenClause != null && !combinedWhenClause.trim().isEmpty()) {
            trigger.append("\n  WHEN (").append(combinedWhenClause).append(")");
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
        String events = triggeringEvent.toUpperCase().replace(",", " OR ");
        
        // Handle UPDATE OF column_list - PostgreSQL doesn't support this syntax
        // We'll need to add this logic to the WHEN clause or function body
        if (events.contains("UPDATE OF")) {
            events = events.replaceAll("UPDATE OF [\\w,\\s]+", "UPDATE");
        }
        
        return events;
    }
    
    /**
     * Extract UPDATE OF column list if present.
     */
    public List<String> getUpdateOfColumns() {
        List<String> columns = new ArrayList<>();
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "UPDATE\\s+OF\\s+([\\w,\\s]+)", 
            java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(triggeringEvent);
        
        if (matcher.find()) {
            String columnList = matcher.group(1);
            String[] columnArray = columnList.split(",");
            for (String column : columnArray) {
                columns.add(column.trim());
            }
        }
        
        return columns;
    }
    
    /**
     * Check if this trigger has UPDATE OF column restrictions.
     */
    public boolean hasUpdateOfColumns() {
        return triggeringEvent.toUpperCase().contains("UPDATE OF");
    }

    /**
     * Helper method to get triggering events as a list.
     */
    public List<String> getTriggeringEvents() {
        List<String> events = new ArrayList<>();
        if (triggeringEvent != null && !triggeringEvent.trim().isEmpty()) {
            String[] eventArray = triggeringEvent.split(",");
            for (String event : eventArray) {
                String cleanEvent = event.trim().toUpperCase();
                // Handle UPDATE OF by extracting just UPDATE
                if (cleanEvent.startsWith("UPDATE OF")) {
                    events.add("UPDATE");
                } else {
                    events.add(cleanEvent);
                }
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
     * Generate PostgreSQL function name following package naming convention.
     * Pattern: SCHEMA.TRIGGERFUNCTION_triggername (uppercase schema and prefix, lowercase trigger name)
     */
    private String getPostgreFunctionName() {
        return schema.toUpperCase() + ".TRIGGERFUNCTION_" + triggerName.toLowerCase();
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
        
        if (triggerBody.isEmpty()) {
            logic.append("    -- Empty trigger body\n");
            logic.append("    -- Event: ").append(event).append("\n");
            return logic.toString();
        }
        
        logic.append("    -- Trigger logic for event: ").append(event).append("\n");
        
        // Transform each statement in the trigger body
        for (Statement stmt : triggerBody) {
            String transformedStatement = transformStatement(stmt, everything);
            logic.append("    ").append(transformedStatement).append("\n");
        }
        
        return logic.toString();
    }
    
    /**
     * Transform a single trigger body statement from Oracle to PostgreSQL.
     */
    private String transformStatement(Statement statement, Everything everything) {
        if (statement == null) {
            return "-- NULL statement";
        }
        
        // For now, get the statement as string and transform it
        String statementText = statement.toString();
        
        if ("TriggerBodyStatement".equals(statementText)) {
            // This is our placeholder from Phase 3 - try to get actual trigger body
            return getActualTriggerBodyTransformed(everything);
        }
        
        // Transform the statement using TriggerTransformer
        return TriggerTransformer.transformTriggerBody(statementText, everything);
    }
    
    /**
     * Get and transform the actual trigger body from Oracle trigger metadata.
     * This method extracts the raw trigger body and transforms it.
     */
    private String getActualTriggerBodyTransformed(Everything everything) {
        // For Phase 4, we'll implement more sophisticated body extraction
        // For now, return a transformed placeholder
        StringBuilder body = new StringBuilder();
        
        body.append("-- Transformed trigger body\n");
        body.append("    -- Original Oracle trigger: ").append(triggerName).append("\n");
        body.append("    -- Table: ").append(tableOwner).append(".").append(tableName).append("\n");
        body.append("    \n");
        body.append("    -- Example transformations:\n");
        body.append("    -- :NEW.created_date := SYSDATE; becomes NEW.created_date := CURRENT_TIMESTAMP;\n");
        body.append("    -- IF INSERTING THEN becomes IF TG_OP = 'INSERT' THEN\n");
        body.append("    -- IF UPDATING('salary') THEN becomes IF (TG_OP = 'UPDATE' AND OLD.salary IS DISTINCT FROM NEW.salary) THEN\n");
        body.append("    \n");
        body.append("    -- TODO: Add actual trigger body transformation here\n");
        
        return body.toString();
    }

    /**
     * Generate appropriate return statement based on trigger type.
     */
    private String generateReturnStatement() {
        // Use TriggerTransformer to determine the correct return statement
        return "  " + TriggerTransformer.getPostgreTriggerReturn(triggeringEvent, triggerType);
    }

    /**
     * Transform Oracle WHEN clause to PostgreSQL equivalent.
     */
    private String transformWhenClause(String oracleWhen, Everything everything) {
        return TriggerTransformer.transformWhenClause(oracleWhen, everything);
    }
    
    /**
     * Build combined WHEN clause that includes both original WHEN clause and UPDATE OF column logic.
     */
    private String buildCombinedWhenClause(Everything everything) {
        List<String> conditions = new ArrayList<>();
        
        // Add original WHEN clause if present
        if (whenClause != null && !whenClause.trim().isEmpty()) {
            String transformedWhen = transformWhenClause(whenClause, everything);
            if (transformedWhen != null && !transformedWhen.trim().isEmpty()) {
                conditions.add("(" + transformedWhen + ")");
            }
        }
        
        // Add UPDATE OF column conditions if present
        if (hasUpdateOfColumns()) {
            List<String> updateOfColumns = getUpdateOfColumns();
            List<String> columnConditions = new ArrayList<>();
            
            for (String column : updateOfColumns) {
                columnConditions.add(String.format("OLD.%s IS DISTINCT FROM NEW.%s", column, column));
            }
            
            if (!columnConditions.isEmpty()) {
                String updateOfCondition = "TG_OP = 'UPDATE' AND (" + String.join(" OR ", columnConditions) + ")";
                conditions.add("(" + updateOfCondition + ")");
            }
        }
        
        // Combine conditions with AND
        if (conditions.isEmpty()) {
            return null;
        } else if (conditions.size() == 1) {
            return conditions.get(0).replaceAll("^\\((.*)\\)$", "$1"); // Remove outer parentheses
        } else {
            return String.join(" AND ", conditions);
        }
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