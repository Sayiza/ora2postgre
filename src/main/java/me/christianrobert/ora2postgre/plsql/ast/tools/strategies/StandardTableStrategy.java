package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.ColumnMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard implementation for transforming Oracle tables to PostgreSQL.
 * This strategy handles most common Oracle table patterns and converts them
 * to PostgreSQL-compatible CREATE TABLE statements.
 */
public class StandardTableStrategy implements TableTransformationStrategy {

  private static final Logger log = LoggerFactory.getLogger(StandardTableStrategy.class);

  @Override
  public boolean supports(TableMetadata table) {
    // Standard strategy supports all tables as a fallback
    return true;
  }

  @Override
  public List<String> transform(TableMetadata table, Everything context) {
    log.debug("Transforming table {}.{} using StandardTableStrategy",
            table.getSchema(), table.getTableName());

    List<String> statements = new ArrayList<>();

    // Build CREATE TABLE statement
    StringBuilder createTable = new StringBuilder("CREATE TABLE ");
    createTable.append(table.getSchema())
            .append(".")
            .append(table.getTableName()).append(" (\n");

    // Columns
    List<String> columnDefs = new ArrayList<>();
    for (ColumnMetadata col : table.getColumns()) {
      columnDefs.add(col.toPostgre(context, table.getSchema(), table.getTableName()));
    }
    createTable.append(String.join(",\n", columnDefs));
    createTable.append("\n);");
    statements.add(createTable.toString());

    log.debug("Generated {} DDL statements for table {}.{}",
            statements.size(), table.getSchema(), table.getTableName());

    return statements;
  }

  @Override
  public String getStrategyName() {
    return "Standard Table";
  }

  @Override
  public int getPriority() {
    // Standard strategy has lowest priority as it's the fallback
    return 0;
  }

  @Override
  public String getConversionNotes(TableMetadata table) {
    return "Converted using standard table transformation";
  }
}