package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils;
import me.christianrobert.ora2postgre.oracledb.ConstraintMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Strategy for transforming Oracle CHECK constraints to PostgreSQL.
 * Handles check constraints with Oracle-to-PostgreSQL condition transformation.
 */
public class CheckConstraintStrategy implements ConstraintTransformationStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(CheckConstraintStrategy.class);
    
    @Override
    public boolean supports(ConstraintMetadata constraint) {
        return ConstraintMetadata.CHECK.equals(constraint.getConstraintType());
    }
    
    @Override
    public String transformConstraintDDL(ConstraintMetadata constraint, Everything context) {
        if (!supports(constraint)) {
            throw new UnsupportedOperationException("CheckConstraintStrategy does not support constraint type: " + constraint.getConstraintType());
        }
        
        log.debug("Transforming check constraint: {}", constraint.getConstraintName());
        
        StringBuilder ddl = new StringBuilder();
        
        // Add constraint name
        ddl.append("CONSTRAINT ")
           .append(PostgreSqlIdentifierUtils.quoteIdentifier(constraint.getConstraintName()))
           .append(" ");
        
        // Add CHECK clause
        ddl.append("CHECK (");
        if (constraint.getCheckCondition() != null) {
            ddl.append(transformCheckCondition(constraint.getCheckCondition()));
        }
        ddl.append(")");
        
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
        return "Check";
    }
    
    @Override
    public int getPriority() {
        return 30; // Medium priority - check constraints should be created after primary keys and unique constraints
    }
    
    @Override
    public String getConstraintType() {
        return ConstraintMetadata.CHECK;
    }
    
    @Override
    public String getConversionNotes(ConstraintMetadata constraint) {
        StringBuilder notes = new StringBuilder("Check constraint");
        if (constraint.isDeferrable()) {
            notes.append(" (deferrable)");
        }
        if (constraint.getCheckCondition() != null && constraint.getCheckCondition().contains("SYSDATE")) {
            notes.append(" (contains transformed Oracle functions)");
        }
        return notes.toString();
    }
    
    /**
     * Transforms Oracle check constraint conditions to PostgreSQL equivalents.
     * 
     * @param oracleCondition The Oracle check condition
     * @return PostgreSQL-compatible check condition
     */
    private String transformCheckCondition(String oracleCondition) {
        if (oracleCondition == null) {
            return "";
        }
        
        String postgresCondition = oracleCondition;
        
        // Transform Oracle functions to PostgreSQL equivalents
        postgresCondition = postgresCondition.replace("SYSDATE", "CURRENT_TIMESTAMP");
        postgresCondition = postgresCondition.replace("USER", "CURRENT_USER");
        postgresCondition = postgresCondition.replace("SYSTIMESTAMP", "CURRENT_TIMESTAMP");
        
        // Transform Oracle data types in conditions
        postgresCondition = postgresCondition.replaceAll("(?i)VARCHAR2", "TEXT");
        postgresCondition = postgresCondition.replaceAll("(?i)NUMBER", "NUMERIC");
        
        // Note: More complex transformations may be needed for advanced Oracle features
        // This could be enhanced to handle more Oracle-specific functions and operators
        
        return postgresCondition;
    }
}