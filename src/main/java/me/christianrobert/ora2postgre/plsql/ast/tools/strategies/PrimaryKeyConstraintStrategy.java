package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PostgreSqlIdentifierUtils;
import me.christianrobert.ora2postgre.oracledb.ConstraintMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * Strategy for transforming Oracle PRIMARY KEY constraints to PostgreSQL.
 * Handles simple primary key constraints with proper identifier quoting and deferrable options.
 */
public class PrimaryKeyConstraintStrategy implements ConstraintTransformationStrategy {

  private static final Logger log = LoggerFactory.getLogger(PrimaryKeyConstraintStrategy.class);

  @Override
  public boolean supports(ConstraintMetadata constraint) {
    return ConstraintMetadata.PRIMARY_KEY.equals(constraint.getConstraintType());
  }

  @Override
  public String transformConstraintDDL(ConstraintMetadata constraint, Everything context) {
    if (!supports(constraint)) {
      throw new UnsupportedOperationException("PrimaryKeyConstraintStrategy does not support constraint type: " + constraint.getConstraintType());
    }

    log.debug("Transforming primary key constraint: {}", constraint.getConstraintName());

    StringBuilder ddl = new StringBuilder();

    // Add constraint name
    ddl.append("CONSTRAINT ")
            .append(PostgreSqlIdentifierUtils.quoteIdentifier(constraint.getConstraintName()))
            .append(" ");

    // Add PRIMARY KEY clause
    ddl.append("PRIMARY KEY (");
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
    return "Primary Key";
  }

  @Override
  public int getPriority() {
    return 100; // High priority - primary keys should be created first
  }

  @Override
  public String getConstraintType() {
    return ConstraintMetadata.PRIMARY_KEY;
  }

  @Override
  public String getConversionNotes(ConstraintMetadata constraint) {
    StringBuilder notes = new StringBuilder("Primary key constraint");
    if (constraint.isDeferrable()) {
      notes.append(" (deferrable)");
    }
    return notes.toString();
  }
}