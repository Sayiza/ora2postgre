package me.christianrobert.ora2postgre.writing;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.RecordTypeCollectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Export class for generating schema-level PostgreSQL composite types from
 * Oracle block-level record type definitions.
 * 
 * This class generates CREATE TYPE statements for all record types collected
 * from functions, procedures, packages, and triggers, placing them in a
 * separate export phase (step2brecordtypes) that executes before component
 * generation to ensure types are available when referenced.
 */
public class ExportRecordType {

  private static final Logger log = LoggerFactory.getLogger(ExportRecordType.class);

  /**
   * Saves all collected record types as PostgreSQL composite types.
   * 
   * This method generates one SQL file per schema containing all record types
   * from that schema, following the established export pattern.
   * 
   * @param path The base path for export files
   * @param data The Everything context containing schema information
   */
  public static void saveRecordTypesToPostgre(String path, Everything data) {
    log.info("Starting record type export to PostgreSQL composite types");
    
    // Check if any record types were collected
    int collectedCount = RecordTypeCollectionManager.getCollectedCount();
    if (collectedCount == 0) {
      log.info("No record types collected - skipping export");
      return;
    }
    
    log.info("Exporting {} collected record types", collectedCount);
    
    // Get all record types organized by schema
    Map<String, List<RecordTypeCollectionManager.RecordTypeInfo>> recordTypesBySchema = 
        RecordTypeCollectionManager.getAllRecordTypesBySchema();
    
    // Generate and save one file per schema
    for (Map.Entry<String, List<RecordTypeCollectionManager.RecordTypeInfo>> entry : recordTypesBySchema.entrySet()) {
      String schema = entry.getKey();
      List<RecordTypeCollectionManager.RecordTypeInfo> recordTypes = entry.getValue();
      
      saveSchemaRecordTypes(path, schema, recordTypes, data);
    }
    
    log.info("Record type export completed successfully");
  }

  /**
   * Saves record types for a specific schema to a SQL file.
   * 
   * @param basePath The base export path
   * @param schema The schema name
   * @param recordTypes List of record type information for this schema
   * @param data The Everything context for transformation
   */
  private static void saveSchemaRecordTypes(String basePath, String schema, 
                                          List<RecordTypeCollectionManager.RecordTypeInfo> recordTypes, 
                                          Everything data) {
    if (recordTypes == null || recordTypes.isEmpty()) {
      return;
    }
    
    log.debug("Generating record types for schema: {} ({} types)", schema, recordTypes.size());
    
    // Generate the SQL content
    StringBuilder content = new StringBuilder();
    
    // Add file header
    content.append("-- =====================================================\n");
    content.append("-- PostgreSQL Composite Types for Schema: ").append(schema.toUpperCase()).append("\n");
    content.append("-- Generated from Oracle block-level record types\n");
    content.append("-- Total types in this file: ").append(recordTypes.size()).append("\n");
    content.append("-- =====================================================\n\n");
    
    // Add schema comment
    content.append("-- These composite types replace Oracle block-level record type definitions\n");
    content.append("-- and are created at schema level for PostgreSQL compatibility\n\n");
    
    // Generate each record type
    for (RecordTypeCollectionManager.RecordTypeInfo info : recordTypes) {
      content.append(generateRecordTypeDefinition(info, data));
      content.append("\n");
    }
    
    // Construct the file path following the established pattern
    String fullPathAsString = basePath + File.separator + 
                             schema.toLowerCase() + File.separator + 
                             "step2brecordtypes";
    
    String fileName = schema.toLowerCase() + "_record_types.sql";
    
    // Write the file
    FileWriter.write(
        Paths.get(fullPathAsString),
        fileName,
        content.toString()
    );
    
    log.info("Generated record types for schema {}: {} types -> {}/{}", 
             schema, recordTypes.size(), fullPathAsString, fileName);
  }

  /**
   * Generates the PostgreSQL CREATE TYPE definition for a single record type.
   * 
   * @param info The record type information
   * @param data The Everything context for field transformation
   * @return The CREATE TYPE SQL statement
   */
  private static String generateRecordTypeDefinition(RecordTypeCollectionManager.RecordTypeInfo info, 
                                                   Everything data) {
    StringBuilder result = new StringBuilder();
    
    // Add source information comment
    result.append("-- Record type: ").append(info.getRecordType().getName()).append("\n");
    result.append("-- Source: ").append(info.getSourceComponentType())
          .append(" ").append(info.getSourceComponent());
    
    if (!"PACKAGE".equals(info.getSourceComponentType()) && 
        info.getSourcePackage() != null && 
        !"null".equals(info.getSourcePackage())) {
      result.append(" in package ").append(info.getSourcePackage());
    }
    
    result.append("\n");
    result.append("-- Qualified name: ").append(info.getQualifiedName()).append("\n");
    
    // Generate the CREATE TYPE statement
    result.append("CREATE TYPE ").append(info.getQualifiedName()).append(" AS (\n");
    
    if (info.getRecordType().getFields() != null && !info.getRecordType().getFields().isEmpty()) {
      for (int i = 0; i < info.getRecordType().getFields().size(); i++) {
        me.christianrobert.ora2postgre.plsql.ast.RecordType.RecordField field = 
            info.getRecordType().getFields().get(i);
        
        result.append("  ").append(field.toPostgre(data));
        
        if (i < info.getRecordType().getFields().size() - 1) {
          result.append(",");
        }
        result.append("\n");
      }
    } else {
      result.append("  -- No fields defined\n");
    }
    
    result.append(");");
    
    return result.toString();
  }

  /**
   * Utility method to check if record type export is needed based on configuration.
   * 
   * This method checks if any of the component flags that can contain record types
   * are enabled, following the user's requirement for conditional execution.
   * 
   * @param configService The configuration service to check flags
   * @return true if record type export should be performed
   */
  public static boolean isRecordTypeExportNeeded(me.christianrobert.ora2postgre.config.ConfigurationService configService) {
    return configService.isDoTriggers() || 
           configService.isDoPackageBody() || 
           configService.isDoStandaloneFunctions() || 
           configService.isDoStandaloneProcedures();
  }

  /**
   * Gets the step name for record type export.
   * This follows the established naming convention for export steps.
   * 
   * @return The step directory name
   */
  public static String getStepName() {
    return "step2brecordtypes";
  }

  /**
   * Gets statistics about the record type export.
   * 
   * @return A summary of collected and exported record types
   */
  public static String getExportSummary() {
    int totalCount = RecordTypeCollectionManager.getCollectedCount();
    Map<String, List<RecordTypeCollectionManager.RecordTypeInfo>> bySchema = 
        RecordTypeCollectionManager.getAllRecordTypesBySchema();
    
    if (totalCount == 0) {
      return "No record types collected for export";
    }
    
    return String.format("Exported %d record types across %d schemas", 
                        totalCount, bySchema.size());
  }
}