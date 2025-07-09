package me.christianrobert.ora2postgre.postgre;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class PostgresStatsService {

  private static final Logger log = LoggerFactory.getLogger(PostgresStatsService.class);

  public Map<String, Object> getTargetDatabaseStats(Connection postgresConnection) throws SQLException {
    Map<String, Object> stats = new HashMap<>();

    // Count schemas (excluding system schemas)
    stats.put("schemas", countSchemas(postgresConnection));

    // Count tables (excluding system tables)
    stats.put("tables", countTables(postgresConnection));

    // Count views (excluding system views)
    stats.put("views", countViews(postgresConnection));

    // Count functions (excluding system functions)
    stats.put("functions", countFunctions(postgresConnection));

    // Count procedures (excluding system procedures)
    stats.put("procedures", countProcedures(postgresConnection));

    // Count types (composite types, domains, enums - excluding system types)
    stats.put("types", countTypes(postgresConnection));

    // Count triggers (excluding system triggers)
    stats.put("triggers", countTriggers(postgresConnection));

    // Count constraints (excluding system constraints)
    stats.put("constraints", countConstraints(postgresConnection));

    // Count indexes (excluding system indexes)
    stats.put("indexes", countIndexes(postgresConnection));

    // Count total rows (excluding system tables)
    stats.put("totalRowCount", countTotalRows(postgresConnection));

    log.info("Retrieved fresh PostgreSQL target database stats: {}", stats);
    return stats;
  }

  private int countSchemas(Connection conn) throws SQLException {
    String sql = """
            SELECT count(*) 
            FROM information_schema.schemata 
            WHERE schema_name NOT IN ('information_schema', 'pg_catalog', 'pg_toast', 'pg_temp_1', 'pg_toast_temp_1')
            AND schema_name NOT LIKE 'pg_temp_%'
            AND schema_name != 'public'
            AND schema_name NOT LIKE 'pg_toast_temp_%'
            """;

    try (PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
    }
    return 0;
  }

  private int countTables(Connection conn) throws SQLException {
    String sql = """
            SELECT count(*) 
            FROM information_schema.tables 
            WHERE table_schema NOT IN ('information_schema', 'pg_catalog', 'pg_toast')
            AND table_schema NOT LIKE 'pg_temp_%'
            AND table_schema NOT LIKE 'pg_toast_temp_%'
            AND table_type = 'BASE TABLE'
            """;

    try (PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
    }
    return 0;
  }

  private int countViews(Connection conn) throws SQLException {
    String sql = """
            SELECT count(*) 
            FROM information_schema.views 
            WHERE table_schema NOT IN ('information_schema', 'pg_catalog', 'pg_toast')
            AND table_schema NOT LIKE 'pg_temp_%'
            AND table_schema NOT LIKE 'pg_toast_temp_%'
            """;

    try (PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
    }
    return 0;
  }

  private int countFunctions(Connection conn) throws SQLException {
    String sql = """
            SELECT count(*) 
            FROM information_schema.routines 
            WHERE routine_schema NOT IN ('information_schema', 'pg_catalog', 'pg_toast')
            AND routine_schema NOT LIKE 'pg_temp_%'
            AND routine_schema NOT LIKE 'pg_toast_temp_%'
            AND routine_type = 'FUNCTION'
            """;

    try (PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
    }
    return 0;
  }

  private int countProcedures(Connection conn) throws SQLException {
    String sql = """
            SELECT count(*) 
            FROM information_schema.routines 
            WHERE routine_schema NOT IN ('information_schema', 'pg_catalog', 'pg_toast')
            AND routine_schema NOT LIKE 'pg_temp_%'
            AND routine_schema NOT LIKE 'pg_toast_temp_%'
            AND routine_type = 'PROCEDURE'
            """;

    try (PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
    }
    return 0;
  }

  private int countTypes(Connection conn) throws SQLException {
    String sql = """
            SELECT count(*) 
            FROM pg_type t
            JOIN pg_namespace n ON t.typnamespace = n.oid
            WHERE n.nspname NOT IN ('information_schema', 'pg_catalog', 'pg_toast')
            AND n.nspname NOT LIKE 'pg_temp_%'
            AND n.nspname NOT LIKE 'pg_toast_temp_%'
            AND t.typtype IN ('c', 'd', 'e', 'r')
            AND NOT EXISTS (
                SELECT 1 FROM pg_class c 
                WHERE c.reltype = t.oid AND c.relkind IN ('r', 'v', 'm')
            )
            """;

    try (PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
    }
    return 0;
  }

  private long countTotalRows(Connection conn) throws SQLException {
    String sql = """
            SELECT COALESCE(SUM(n_live_tup), 0) 
            FROM pg_stat_user_tables 
            WHERE schemaname NOT IN ('information_schema', 'pg_catalog', 'pg_toast')
            AND schemaname NOT LIKE 'pg_temp_%'
            AND schemaname NOT LIKE 'pg_toast_temp_%'
            """;

    try (PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getLong(1);
      }
    }
    return 0;
  }

  private int countTriggers(Connection conn) throws SQLException {
    String sql = """
            SELECT count(*) 
            FROM information_schema.triggers 
            WHERE trigger_schema NOT IN ('information_schema', 'pg_catalog', 'pg_toast')
            AND trigger_schema NOT LIKE 'pg_temp_%'
            AND trigger_schema NOT LIKE 'pg_toast_temp_%'
            """;

    try (PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
    }
    return 0;
  }

  private int countConstraints(Connection conn) throws SQLException {
    String sql = """
            SELECT count(*) 
            FROM information_schema.table_constraints 
            WHERE constraint_schema NOT IN ('information_schema', 'pg_catalog', 'pg_toast')
            AND constraint_schema NOT LIKE 'pg_temp_%'
            AND constraint_schema NOT LIKE 'pg_toast_temp_%'
            """;

    try (PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
    }
    return 0;
  }

  private int countIndexes(Connection conn) throws SQLException {
    String sql = """
            SELECT count(*) 
            FROM pg_indexes 
            WHERE schemaname NOT IN ('information_schema', 'pg_catalog', 'pg_toast')
            AND schemaname NOT LIKE 'pg_temp_%'
            AND schemaname NOT LIKE 'pg_toast_temp_%'
            """;

    try (PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
    }
    return 0;
  }
}