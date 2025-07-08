package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

public class InsertStatement extends Statement {
    private final String tableName;
    private final String schemaName;
    private final List<String> columnNames; // Optional column list
    private final List<Expression> values; // Values for INSERT VALUES
    private final SelectStatement selectStatement; // For INSERT SELECT

    public InsertStatement(String schemaName, String tableName, List<String> columnNames, 
                          List<Expression> values, SelectStatement selectStatement) {
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.columnNames = columnNames;
        this.values = values;
        this.selectStatement = selectStatement;
    }

    // Constructor for INSERT VALUES
    public InsertStatement(String schemaName, String tableName, List<String> columnNames, List<Expression> values) {
        this(schemaName, tableName, columnNames, values, null);
    }

    // Constructor for INSERT SELECT
    public InsertStatement(String schemaName, String tableName, List<String> columnNames, SelectStatement selectStatement) {
        this(schemaName, tableName, columnNames, null, selectStatement);
    }

    public String getTableName() {
        return tableName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public List<Expression> getValues() {
        return values;
    }

    public SelectStatement getSelectStatement() {
        return selectStatement;
    }

    public boolean isInsertValues() {
        return values != null;
    }

    public boolean isInsertSelect() {
        return selectStatement != null;
    }

    @Override
    public <T> T accept(PlSqlAstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "InsertStatement{" + 
               "table=" + (schemaName != null ? schemaName + "." : "") + tableName +
               ", columns=" + (columnNames != null ? columnNames.size() : 0) +
               ", values=" + (values != null ? values.size() : 0) +
               ", hasSelect=" + (selectStatement != null) + "}";
    }

    @Override
    public String toPostgre(Everything data) {
        StringBuilder b = new StringBuilder();
        
        b.append(data.getIntendation()).append("INSERT INTO ");
        
        // Resolve schema using Everything's schema resolution logic
        String resolvedSchema = null;
        
        if (schemaName != null && !schemaName.isEmpty()) {
            // Schema was explicitly provided in Oracle code (e.g., SCHEMA.TABLE)
            try {
                resolvedSchema = data.lookupSchema4Field(tableName, schemaName);
            } catch (Exception e) {
                // If schema resolution fails, use the provided schema as-is
                resolvedSchema = schemaName;
            }
        } else {
            // No schema prefix in Oracle code - table is in current schema or is a synonym
            // Use the current schema context from the function/procedure
            String currentSchema = getCurrentSchema(data);
            if (currentSchema != null) {
                try {
                    resolvedSchema = data.lookupSchema4Field(tableName, currentSchema);
                } catch (Exception e) {
                    // If synonym/table lookup fails, assume it's in the current schema
                    resolvedSchema = currentSchema;
                }
            }
        }
        
        // Always emit schema prefix for PostgreSQL reliability
        if (resolvedSchema != null && !resolvedSchema.isEmpty()) {
            b.append(resolvedSchema.toLowerCase()).append(".");
        }
        b.append(tableName.toLowerCase());
        
        // Handle column list if specified
        if (columnNames != null && !columnNames.isEmpty()) {
            b.append(" (");
            for (int i = 0; i < columnNames.size(); i++) {
                if (i > 0) {
                    b.append(", ");
                }
                b.append(columnNames.get(i).toLowerCase());
            }
            b.append(")");
        }
        
        // Handle VALUES clause
        if (isInsertValues()) {
            b.append(" VALUES (");
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    b.append(", ");
                }
                b.append(values.get(i).toPostgre(data));
            }
            b.append(")");
        }
        // Handle SELECT clause
        else if (isInsertSelect()) {
            b.append(" ");
            b.append(selectStatement.toPostgre(data));
        }
        
        b.append(";");
        
        return b.toString();
    }
    
    /**
     * Gets the current schema context for resolving unqualified table names.
     * This should be the schema where the function/procedure containing this INSERT is defined.
     */
    private String getCurrentSchema(Everything data) {
        // For now, use the first user schema as the current schema context
        // TODO: This could be enhanced to use actual function/procedure schema context
        if (!data.getUserNames().isEmpty()) {
            return data.getUserNames().get(0);
        }
        return null;
    }
}