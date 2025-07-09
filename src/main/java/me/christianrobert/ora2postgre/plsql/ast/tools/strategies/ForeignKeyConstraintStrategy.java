package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils;
import me.christianrobert.ora2postgre.oracledb.ConstraintMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Strategy for transforming Oracle FOREIGN KEY constraints to PostgreSQL.
 * Handles foreign key constraints with proper reference validation and referential actions.
 */
public class ForeignKeyConstraintStrategy implements ConstraintTransformationStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(ForeignKeyConstraintStrategy.class);
    
    // Oracle to PostgreSQL referential action mapping
    private static final Map<String, String> REFERENTIAL_ACTION_MAP = Map.of(
        "CASCADE", "CASCADE",
        "SET NULL", "SET NULL",
        "SET DEFAULT", "SET DEFAULT",
        "RESTRICT", "RESTRICT",
        "NO ACTION", "NO ACTION"
    );
    
    @Override
    public boolean supports(ConstraintMetadata constraint) {
        return ConstraintMetadata.FOREIGN_KEY.equals(constraint.getConstraintType());
    }
    
    @Override
    public String transformConstraintDDL(ConstraintMetadata constraint, Everything context) {
        if (!supports(constraint)) {
            throw new UnsupportedOperationException("ForeignKeyConstraintStrategy does not support constraint type: " + constraint.getConstraintType());
        }
        
        log.debug("Transforming foreign key constraint: {}", constraint.getConstraintName());
        
        StringBuilder ddl = new StringBuilder();
        
        // Add constraint name
        ddl.append("CONSTRAINT ")
           .append(PostgreSqlIdentifierUtils.quoteIdentifier(constraint.getConstraintName()))
           .append(" ");
        
        // Add FOREIGN KEY clause
        ddl.append("FOREIGN KEY (");
        ddl.append(constraint.getColumnNames().stream()
            .map(PostgreSqlIdentifierUtils::quoteIdentifier)
            .collect(Collectors.joining(", ")));
        ddl.append(") REFERENCES ");
        
        // Add referenced table
        if (constraint.getReferencedSchema() != null && !constraint.getReferencedSchema().isEmpty()) {
            ddl.append(PostgreSqlIdentifierUtils.quoteIdentifier(constraint.getReferencedSchema())).append(".");
        }
        ddl.append(PostgreSqlIdentifierUtils.quoteIdentifier(constraint.getReferencedTable()));
        
        // Add referenced columns if specified
        if (constraint.getReferencedColumns() != null && !constraint.getReferencedColumns().isEmpty()) {
            ddl.append(" (");
            ddl.append(constraint.getReferencedColumns().stream()
                .map(PostgreSqlIdentifierUtils::quoteIdentifier)
                .collect(Collectors.joining(", ")));
            ddl.append(")");
        }
        
        // Add ON DELETE clause
        if (constraint.getDeleteRule() != null && !"NO ACTION".equals(constraint.getDeleteRule())) {
            ddl.append(" ON DELETE ").append(REFERENTIAL_ACTION_MAP.getOrDefault(constraint.getDeleteRule(), "NO ACTION"));
        }
        
        // Add ON UPDATE clause
        if (constraint.getUpdateRule() != null && !"NO ACTION".equals(constraint.getUpdateRule())) {
            ddl.append(" ON UPDATE ").append(REFERENTIAL_ACTION_MAP.getOrDefault(constraint.getUpdateRule(), "NO ACTION"));
        }
        
        // Add deferrable clause if applicable
        if (constraint.isDeferrable()) {
            ddl.append(" DEFERRABLE");
            if (constraint.isInitiallyDeferred()) {
                ddl.append(" INITIALLY DEFERRED");
            }
        }
        
        return ddl.toString();
    }
    
    @Override
    public String transformAlterTableDDL(ConstraintMetadata constraint, String schemaName, String tableName, Everything context) {
        StringBuilder ddl = new StringBuilder();
        ddl.append("ALTER TABLE ");
        if (schemaName != null && !schemaName.isEmpty()) {
            ddl.append(PostgreSqlIdentifierUtils.quoteIdentifier(schemaName)).append(".");
        }
        ddl.append(PostgreSqlIdentifierUtils.quoteIdentifier(tableName));
        ddl.append(" ADD ");
        ddl.append(transformConstraintDDL(constraint, context));
        ddl.append(";");
        return ddl.toString();
    }
    
    @Override
    public String getStrategyName() {
        return "Foreign Key";
    }
    
    @Override
    public int getPriority() {
        return 10; // Low priority - foreign keys should be created after primary keys and unique constraints
    }
    
    @Override
    public String getConstraintType() {
        return ConstraintMetadata.FOREIGN_KEY;
    }
    
    @Override
    public boolean requiresDependencyValidation() {
        return true;
    }
    
    @Override
    public boolean validateDependencies(ConstraintMetadata constraint, Everything context) {
        if (!constraint.isForeignKey()) {
            return true; // Not a foreign key, validation passes
        }
        
        String referencedSchema = constraint.getReferencedSchema();
        String referencedTable = constraint.getReferencedTable();
        
        if (referencedSchema == null || referencedTable == null) {
            log.warn("Foreign key constraint {} has null referenced schema or table", constraint.getConstraintName());
            return false;
        }
        
        // Check if referenced table exists in our migration
        for (TableMetadata table : context.getTableSql()) {
            if (referencedSchema.equalsIgnoreCase(table.getSchema()) && 
                referencedTable.equalsIgnoreCase(table.getTableName())) {
                return true;
            }
        }
        
        log.warn("Foreign key constraint {} references table {}.{} which was not found in migration", 
                constraint.getConstraintName(), referencedSchema, referencedTable);
        return false;
    }
    
    @Override
    public String getConversionNotes(ConstraintMetadata constraint) {
        StringBuilder notes = new StringBuilder("Foreign key constraint");
        if (constraint.getDeleteRule() != null && !"NO ACTION".equals(constraint.getDeleteRule())) {
            notes.append(" (ON DELETE ").append(constraint.getDeleteRule()).append(")");
        }
        if (constraint.getUpdateRule() != null && !"NO ACTION".equals(constraint.getUpdateRule())) {
            notes.append(" (ON UPDATE ").append(constraint.getUpdateRule()).append(")");
        }
        if (constraint.isDeferrable()) {
            notes.append(" (deferrable)");
        }
        return notes.toString();
    }
}