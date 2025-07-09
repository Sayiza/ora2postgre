package me.christianrobert.ora2postgre;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy CLI entry point (deprecated).
 *
 * This class maintains the legacy CLI functionality for backward compatibility.
 * The main application now runs as a Quarkus web service with REST controllers
 * in the controllers package.
 *
 * Use 'mvn quarkus:dev' to run the web service instead of the legacy CLI.
 *
 * API Controllers:
 * - MigrationController: Core migration pipeline (/migration/extract, /parse, /export, /full)
 * - ExecutionController: SQL execution (/migration/execute-pre, /execute-post)
 * - DataTransferController: Data migration (/migration/transferdata)
 * - JobController: Job management (/migration/jobs/*, /cancel, /reset)
 * - StatusController: Monitoring (/migration/status, /health, /logs, /target-stats)
 * - ConfigurationController: Configuration (/migration/config)
 * - SqlTransformController: SQL transformation (/migration/transform/sql)
 */
@ApplicationScoped
public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  /**
   * Legacy CLI entry point.
   *
   * @deprecated Use 'mvn quarkus:dev' to run the web service instead.
   * The REST API provides better control and monitoring capabilities.
   */
  public static void main(String[] args) {
    log.warn("Legacy CLI mode is deprecated. Please use 'mvn quarkus:dev' to run the web service.");
    log.info("The migration is now available as REST API endpoints:");
    log.info("  POST /migration/extract     - Extract Oracle metadata");
    log.info("  POST /migration/parse       - Parse PL/SQL to AST");
    log.info("  POST /migration/export      - Generate PostgreSQL code");
    log.info("  POST /migration/execute-pre - Execute pre-transfer SQL");
    log.info("  POST /migration/transferdata- Transfer data");
    log.info("  POST /migration/execute-post- Execute post-transfer SQL");
    log.info("  POST /migration/full        - Run complete pipeline");
    log.info("  GET  /migration/status      - Get migration status");
    log.info("  GET  /migration/health      - Check system health");

    System.out.println("Oracle to PostgreSQL Migration Tool");
    System.out.println("====================================");
    System.out.println();
    System.out.println("This CLI mode is deprecated.");
    System.out.println("Please use the web service instead:");
    System.out.println();
    System.out.println("  mvn quarkus:dev");
    System.out.println();
    System.out.println("Then open http://localhost:8080 in your browser");
    System.out.println("or use the REST API endpoints listed above.");

    System.exit(0);
  }
}