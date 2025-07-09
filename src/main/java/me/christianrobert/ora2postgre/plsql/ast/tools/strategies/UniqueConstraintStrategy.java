package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils;
import me.christianrobert.ora2postgre.oracledb.ConstraintMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * Strategy for transforming Oracle UNIQUE constraints to PostgreSQL.
 * Handles unique constraints with proper identifier quoting and deferrable options.
 */
public class UniqueConstraintStrategy implements ConstraintTransformationStrategy {

  private static final Logger log = LoggerFactory.getLogger(UniqueConstraintStrategy.class);

  @Override
  public boolean supports(ConstraintMetadata constraint) {
    return ConstraintMetadata.UNIQUE.equals(constraint.getConstraintType());
  }

  @Override
  public String transformConstraintDDL(ConstraintMetadata constraint, Everything context) {
    if (!supports(constraint)) {
      throw new UnsupportedOperationException("UniqueConstraintStrategy does not support constraint type: " + constraint.getConstraintType());
    }

    log.debug("Transforming unique constraint: {}", constraint.getConstraintName());

    StringBuilder ddl = new StringBuilder();

    // Add constraint name
    ddl.append("CONSTRAINT ")
            .append(PostgreSqlIdentifierUtils.quoteIdentifier(constraint.getConstraintName()))
            .append(" ");

    // Add UNIQUE clause
    ddl.append("UNIQUE (");
    ddl.append(constraint.getColumnNames().stream()
            .map(PostgreSqlIdentifierUtils::quoteIdentifier)
            .collect(Collectors.joining(", ")));
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
    return "Unique";
  }

  @Override
  public int getPriority() {
    return 50; // Medium priority - unique constraints should be created after primary keys but before foreign keys
  }

  @Override
  public String getConstraintType() {
    return ConstraintMetadata.UNIQUE;
  }

  @Override
  public String getConversionNotes(ConstraintMetadata constraint) {
    StringBuilder notes = new StringBuilder("Unique constraint");
    if (constraint.isDeferrable()) {
      notes.append(" (deferrable)");
    }
    return notes.toString();
  }
}