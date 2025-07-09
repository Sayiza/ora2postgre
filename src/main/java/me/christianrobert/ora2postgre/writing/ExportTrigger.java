package me.christianrobert.ora2postgre.writing;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.Trigger;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.TriggerTransformationManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Export utility for generating PostgreSQL trigger files.
 * Implements two-phase export strategy: trigger functions first, then trigger definitions.
 * This ensures functions exist before triggers that reference them are created.
 */
public class ExportTrigger {

  private static final Logger log = LoggerFactory.getLogger(ExportTrigger.class);
  private static final TriggerTransformationManager triggerManager = new TriggerTransformationManager();

  /**
   * Main entry point for trigger export. Generates both trigger functions and definitions
   * in the correct order and directory structure.
   *
   * @param basePath Base path for file generation
   * @param everything Global context containing all trigger data
   */
  public static void saveAllTriggers(String basePath, Everything everything) {
    log.info("Starting trigger export to base path: {}", basePath);

    List<Trigger> triggers = everything.getTriggerAst();

    if (triggers.isEmpty()) {
      log.info("No triggers found to export");
      return;
    }

    log.info("Exporting {} triggers", triggers.size());

    // Phase 1: Export trigger functions (must be created first)
    saveTriggerFunctions(basePath, everything);

    // Phase 2: Export trigger definitions (created after functions exist)
    saveTriggerDefinitions(basePath, everything);

    log.info("Trigger export completed successfully");
  }

  /**
   * Export PostgreSQL trigger functions to step7triggers/functions/ directory.
   * Functions must be exported before trigger definitions.
   *
   * @param basePath Base path for file generation
   * @param everything Global context containing trigger data
   */
  public static void saveTriggerFunctions(String basePath, Everything everything) {
    log.info("Exporting trigger functions...");

    List<Trigger> triggers = everything.getTriggerAst();
    Map<String, Integer> schemaFunctionCounts = new HashMap<>();

    for (Trigger trigger : triggers) {
      String functionContent = generateTriggerFunction(trigger, everything);
      String functionFileName = getTriggerFunctionFileName(trigger);
      String fullPath = getTriggerFunctionPath(basePath, trigger.getSchema());

      FileWriter.write(Paths.get(fullPath), functionFileName, functionContent);

      // Track statistics
      schemaFunctionCounts.merge(trigger.getSchema(), 1, Integer::sum);
    }

    // Log export statistics
    for (Map.Entry<String, Integer> entry : schemaFunctionCounts.entrySet()) {
      log.info("Exported {} trigger functions for schema {}", entry.getValue(), entry.getKey());
    }
  }

  /**
   * Export PostgreSQL trigger definitions to step7triggers/definitions/ directory.
   * Definitions are exported after functions to ensure proper dependency order.
   *
   * @param basePath Base path for file generation
   * @param everything Global context containing trigger data
   */
  public static void saveTriggerDefinitions(String basePath, Everything everything) {
    log.info("Exporting trigger definitions...");

    List<Trigger> triggers = everything.getTriggerAst();
    Map<String, Integer> schemaDefinitionCounts = new HashMap<>();

    for (Trigger trigger : triggers) {
      String definitionContent = generateTriggerDefinition(trigger, everything);
      String definitionFileName = getTriggerDefinitionFileName(trigger);
      String fullPath = getTriggerDefinitionPath(basePath, trigger.getSchema());

      FileWriter.write(Paths.get(fullPath), definitionFileName, definitionContent);

      // Track statistics
      schemaDefinitionCounts.merge(trigger.getSchema(), 1, Integer::sum);
    }

    // Log export statistics
    for (Map.Entry<String, Integer> entry : schemaDefinitionCounts.entrySet()) {
      log.info("Exported {} trigger definitions for schema {}", entry.getValue(), entry.getKey());
    }
  }

  /**
   * Generate PostgreSQL trigger function content.
   *
   * @param trigger Trigger AST object
   * @param everything Global context for transformation
   * @return PostgreSQL function DDL content
   */
  private static String generateTriggerFunction(Trigger trigger, Everything everything) {
    StringBuilder content = new StringBuilder();

    // Add header comment
    content.append("-- PostgreSQL trigger function generated from Oracle trigger: ")
            .append(trigger.getTriggerName()).append("\n");
    content.append("-- Original table: ").append(trigger.getTableOwner())
            .append(".").append(trigger.getTableName()).append("\n");
    content.append("-- Trigger type: ").append(trigger.getTriggerType())
            .append(" ").append(trigger.getTriggeringEvent()).append("\n");
    content.append("-- Generated by ora2postgre migration tool\n\n");

    // Generate the function using transformation manager
    content.append(triggerManager.transformTriggerFunction(trigger, everything));

    return content.toString();
  }

  /**
   * Generate PostgreSQL trigger definition content.
   *
   * @param trigger Trigger AST object
   * @param everything Global context for transformation
   * @return PostgreSQL trigger DDL content
   */
  private static String generateTriggerDefinition(Trigger trigger, Everything everything) {
    StringBuilder content = new StringBuilder();

    // Add header comment
    content.append("-- PostgreSQL trigger definition generated from Oracle trigger: ")
            .append(trigger.getTriggerName()).append("\n");
    content.append("-- Original table: ").append(trigger.getTableOwner())
            .append(".").append(trigger.getTableName()).append("\n");
    content.append("-- Trigger type: ").append(trigger.getTriggerType())
            .append(" ").append(trigger.getTriggeringEvent()).append("\n");
    content.append("-- Generated by ora2postgre migration tool\n\n");

    // Generate the trigger definition using transformation manager
    content.append(triggerManager.transformTriggerDefinition(trigger, everything));

    return content.toString();
  }

  /**
   * Get the file path for trigger functions in the schema directory structure.
   * Uses step7atriggerfunctions to ensure execution before step7btriggerdefinitions.
   *
   * @param basePath Base export path
   * @param schema Database schema name
   * @return Full path to trigger functions directory
   */
  private static String getTriggerFunctionPath(String basePath, String schema) {
    return basePath +
            File.separator + schema.toLowerCase() +
            File.separator + "step7atriggerfunctions";
  }

  /**
   * Get the file path for trigger definitions in the schema directory structure.
   * Uses step7btriggerdefinitions to ensure execution after step7atriggerfunctions.
   *
   * @param basePath Base export path
   * @param schema Database schema name
   * @return Full path to trigger definitions directory
   */
  private static String getTriggerDefinitionPath(String basePath, String schema) {
    return basePath +
            File.separator + schema.toLowerCase() +
            File.separator + "step7btriggerdefinitions";
  }

  /**
   * Generate filename for trigger function SQL file.
   * Uses 'a_' prefix to ensure alphabetical execution before trigger definitions.
   *
   * @param trigger Trigger AST object
   * @return SQL filename for the trigger function
   */
  private static String getTriggerFunctionFileName(Trigger trigger) {
    return "a_" + trigger.getTriggerName().toLowerCase() + "_function.sql";
  }

  /**
   * Generate filename for trigger definition SQL file.
   * Uses 'b_' prefix to ensure alphabetical execution after trigger functions.
   *
   * @param trigger Trigger AST object
   * @return SQL filename for the trigger definition
   */
  private static String getTriggerDefinitionFileName(Trigger trigger) {
    return "b_" + trigger.getTriggerName().toLowerCase() + "_trigger.sql";
  }

  /**
   * Get export statistics for triggers.
   *
   * @param everything Global context containing trigger data
   * @return Map of statistics (schema -> count, total triggers, etc.)
   */
  public static Map<String, Object> getExportStatistics(Everything everything) {
    Map<String, Object> stats = new HashMap<>();
    List<Trigger> triggers = everything.getTriggerAst();

    stats.put("totalTriggers", triggers.size());

    // Count by schema
    Map<String, Integer> schemaStats = new HashMap<>();
    Map<String, Integer> typeStats = new HashMap<>();

    for (Trigger trigger : triggers) {
      schemaStats.merge(trigger.getSchema(), 1, Integer::sum);
      typeStats.merge(trigger.getTriggerType(), 1, Integer::sum);
    }

    stats.put("bySchema", schemaStats);
    stats.put("byType", typeStats);

    return stats;
  }

  /**
   * Validate that all triggers have been properly parsed and are ready for export.
   *
   * @param everything Global context containing trigger data
   * @return Validation results with any issues found
   */
  public static Map<String, Object> validateTriggersForExport(Everything everything) {
    Map<String, Object> validation = new HashMap<>();
    List<Trigger> triggers = everything.getTriggerAst();

    int validTriggers = 0;
    int invalidTriggers = 0;
    java.util.List<String> issues = new java.util.ArrayList<>();

    for (Trigger trigger : triggers) {
      boolean isValid = true;

      // Check required fields
      if (trigger.getTriggerName() == null || trigger.getTriggerName().trim().isEmpty()) {
        issues.add("Trigger has empty name: " + trigger);
        isValid = false;
      }

      if (trigger.getTableName() == null || trigger.getTableName().trim().isEmpty()) {
        issues.add("Trigger has empty table name: " + trigger.getTriggerName());
        isValid = false;
      }

      if (trigger.getTriggerType() == null || trigger.getTriggerType().trim().isEmpty()) {
        issues.add("Trigger has empty type: " + trigger.getTriggerName());
        isValid = false;
      }

      if (isValid) {
        validTriggers++;
      } else {
        invalidTriggers++;
      }
    }

    validation.put("validTriggers", validTriggers);
    validation.put("invalidTriggers", invalidTriggers);
    validation.put("issues", issues);
    validation.put("readyForExport", invalidTriggers == 0);

    return validation;
  }
}