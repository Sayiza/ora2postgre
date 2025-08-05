package me.christianrobert.ora2postgre.plsql.ast.tools.managers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager class that collects record types from all PL/SQL components and generates
 * unique schema-level qualified names for PostgreSQL composite type creation.
 * 
 * This manager follows the singleton pattern and maintains a global collection
 * of record types across all parsing phases.
 */
public class RecordTypeCollectionManager {

  private static final Logger log = LoggerFactory.getLogger(RecordTypeCollectionManager.class);

  // Thread-safe collections for the singleton pattern
  private static final Map<String, RecordTypeInfo> collectedRecordTypes = new ConcurrentHashMap<>();
  private static final Set<String> usedNames = ConcurrentHashMap.newKeySet();

  /**
   * Information about a collected record type including its source context
   */
  public static class RecordTypeInfo {
    private final RecordType recordType;
    private final String qualifiedName;
    private final String sourceSchema;
    private final String sourcePackage;
    private final String sourceComponent;
    private final String sourceComponentType; // "FUNCTION", "PROCEDURE", "PACKAGE", "TRIGGER"

    public RecordTypeInfo(RecordType recordType, String qualifiedName, String sourceSchema, 
                         String sourcePackage, String sourceComponent, String sourceComponentType) {
      this.recordType = recordType;
      this.qualifiedName = qualifiedName;
      this.sourceSchema = sourceSchema;
      this.sourcePackage = sourcePackage;
      this.sourceComponent = sourceComponent;
      this.sourceComponentType = sourceComponentType;
    }

    public RecordType getRecordType() { return recordType; }
    public String getQualifiedName() { return qualifiedName; }
    public String getSourceSchema() { return sourceSchema; }
    public String getSourcePackage() { return sourcePackage; }
    public String getSourceComponent() { return sourceComponent; }
    public String getSourceComponentType() { return sourceComponentType; }
  }

  /**
   * Collects record types from a function and generates qualified names
   */
  public static void collectFromFunction(Function function) {
    if (function == null || function.getRecordTypes() == null || function.getRecordTypes().isEmpty()) {
      return;
    }

    String schema = getSchemaName(function);
    String packageName = getPackageName(function);
    String functionName = function.getName();

    for (RecordType recordType : function.getRecordTypes()) {
      String qualifiedName = generateQualifiedName(schema, packageName, functionName, recordType.getName());
      String key = generateKey(schema, packageName, functionName, recordType.getName());
      
      RecordTypeInfo info = new RecordTypeInfo(
          recordType, qualifiedName, schema, packageName, functionName, "FUNCTION"
      );
      
      collectedRecordTypes.put(key, info);
      log.debug("Collected record type from function: {} -> {}", recordType.getName(), qualifiedName);
    }
  }

  /**
   * Collects record types from a procedure and generates qualified names
   */
  public static void collectFromProcedure(Procedure procedure) {
    if (procedure == null || procedure.getRecordTypes() == null || procedure.getRecordTypes().isEmpty()) {
      return;
    }

    String schema = getSchemaName(procedure);
    String packageName = getPackageName(procedure);
    String procedureName = procedure.getName();

    for (RecordType recordType : procedure.getRecordTypes()) {
      String qualifiedName = generateQualifiedName(schema, packageName, procedureName, recordType.getName());
      String key = generateKey(schema, packageName, procedureName, recordType.getName());
      
      RecordTypeInfo info = new RecordTypeInfo(
          recordType, qualifiedName, schema, packageName, procedureName, "PROCEDURE"
      );
      
      collectedRecordTypes.put(key, info);
      log.debug("Collected record type from procedure: {} -> {}", recordType.getName(), qualifiedName);
    }
  }

  /**
   * Collects record types from a package and generates qualified names
   */
  public static void collectFromPackage(OraclePackage oraclePackage) {
    if (oraclePackage == null || oraclePackage.getRecordTypes() == null || oraclePackage.getRecordTypes().isEmpty()) {
      return;
    }

    String schema = oraclePackage.getSchema();
    String packageName = oraclePackage.getName();

    for (RecordType recordType : oraclePackage.getRecordTypes()) {
      String qualifiedName = generateQualifiedName(schema, packageName, null, recordType.getName());
      String key = generateKey(schema, packageName, null, recordType.getName());
      
      RecordTypeInfo info = new RecordTypeInfo(
          recordType, qualifiedName, schema, packageName, packageName, "PACKAGE"
      );
      
      collectedRecordTypes.put(key, info);
      log.debug("Collected record type from package: {} -> {}", recordType.getName(), qualifiedName);
    }
  }

  /**
   * Generates qualified name for a record type based on its context
   */
  private static String generateQualifiedName(String schema, String packageName, String componentName, String recordTypeName) {
    StringBuilder name = new StringBuilder();
    
    // Always include schema
    if (schema != null && !schema.isEmpty()) {
      name.append(schema.toLowerCase()).append("_");
    }
    
    // Include package name
    if (packageName != null && !packageName.isEmpty() && !packageName.equalsIgnoreCase("null")) {
      name.append(packageName.toLowerCase()).append("_");
    }
    
    // Include component name (function/procedure) if present
    if (componentName != null && !componentName.isEmpty()) {
      name.append(componentName.toLowerCase()).append("_");
    }
    
    // Always include record type name
    name.append(recordTypeName.toLowerCase());
    
    // Handle name conflicts by adding numeric suffix
    String baseName = name.toString();
    String finalName = baseName;
    int counter = 1;
    
    while (usedNames.contains(finalName)) {
      finalName = baseName + "_" + counter;
      counter++;
    }
    
    usedNames.add(finalName);
    return finalName;
  }

  /**
   * Generates a unique key for the record type collection
   */
  private static String generateKey(String schema, String packageName, String componentName, String recordTypeName) {
    return String.format("%s.%s.%s.%s", 
        schema != null ? schema : "NULL",
        packageName != null ? packageName : "NULL", 
        componentName != null ? componentName : "NULL",
        recordTypeName
    );
  }

  /**
   * Gets the qualified name for a record type from a function context
   */
  public static String getQualifiedName(Function function, RecordType recordType) {
    String key = generateKey(getSchemaName(function), getPackageName(function), function.getName(), recordType.getName());
    RecordTypeInfo info = collectedRecordTypes.get(key);
    return info != null ? info.getQualifiedName() : recordType.getName().toLowerCase();
  }

  /**
   * Gets the qualified name for a record type from a procedure context
   */
  public static String getQualifiedName(Procedure procedure, RecordType recordType) {
    String key = generateKey(getSchemaName(procedure), getPackageName(procedure), procedure.getName(), recordType.getName());
    RecordTypeInfo info = collectedRecordTypes.get(key);
    return info != null ? info.getQualifiedName() : recordType.getName().toLowerCase();
  }

  /**
   * Gets the qualified name for a record type from a package context
   */
  public static String getQualifiedName(OraclePackage oraclePackage, RecordType recordType) {
    String key = generateKey(oraclePackage.getSchema(), oraclePackage.getName(), null, recordType.getName());
    RecordTypeInfo info = collectedRecordTypes.get(key);
    return info != null ? info.getQualifiedName() : recordType.getName().toLowerCase();
  }

  /**
   * Gets all collected record types organized by schema
   */
  public static Map<String, List<RecordTypeInfo>> getAllRecordTypesBySchema() {
    Map<String, List<RecordTypeInfo>> bySchema = new HashMap<>();
    
    for (RecordTypeInfo info : collectedRecordTypes.values()) {
      String schema = info.getSourceSchema();
      if (schema == null || schema.isEmpty()) {
        schema = "DEFAULT";
      }
      
      bySchema.computeIfAbsent(schema, k -> new ArrayList<>()).add(info);
    }
    
    return bySchema;
  }

  /**
   * Generates PostgreSQL composite type definitions for all collected record types
   */
  public static String generateSchemaLevelTypes(Everything data) {
    StringBuilder result = new StringBuilder();
    Map<String, List<RecordTypeInfo>> recordTypesBySchema = getAllRecordTypesBySchema();
    
    if (recordTypesBySchema.isEmpty()) {
      log.info("No record types collected - skipping schema-level type generation");
      return "";
    }
    
    log.info("Generating schema-level composite types for {} schemas", recordTypesBySchema.size());
    
    for (Map.Entry<String, List<RecordTypeInfo>> entry : recordTypesBySchema.entrySet()) {
      String schema = entry.getKey();
      List<RecordTypeInfo> recordTypes = entry.getValue();
      
      result.append("-- =====================================================\n");
      result.append("-- Record Types for Schema: ").append(schema.toUpperCase()).append("\n");
      result.append("-- Generated from block-level record type definitions\n");
      result.append("-- =====================================================\n\n");
      
      for (RecordTypeInfo info : recordTypes) {
        result.append("-- Source: ").append(info.getSourceComponentType())
              .append(" ").append(info.getSourceComponent())
              .append(" (").append(info.getSourceSchema());
        
        if (info.getSourcePackage() != null && !info.getSourcePackage().equals("null")) {
          result.append(".").append(info.getSourcePackage());
        }
        
        result.append(")\n");
        result.append("CREATE TYPE ").append(info.getQualifiedName()).append(" AS (\n");
        
        if (info.getRecordType().getFields() != null && !info.getRecordType().getFields().isEmpty()) {
          for (int i = 0; i < info.getRecordType().getFields().size(); i++) {
            RecordType.RecordField field = info.getRecordType().getFields().get(i);
            result.append("  ").append(field.toPostgre(data));
            
            if (i < info.getRecordType().getFields().size() - 1) {
              result.append(",");
            }
            result.append("\n");
          }
        }
        
        result.append(");\n\n");
      }
    }
    
    log.info("Generated {} composite types across {} schemas", 
             collectedRecordTypes.size(), recordTypesBySchema.size());
    
    return result.toString();
  }

  /**
   * Clears all collected record types - used between migration runs
   */
  public static void clear() {
    int previousCount = collectedRecordTypes.size();
    collectedRecordTypes.clear();
    usedNames.clear();
    
    if (previousCount > 0) {
      log.info("Cleared {} previously collected record types", previousCount);
    }
  }

  /**
   * Gets the current count of collected record types
   */
  public static int getCollectedCount() {
    return collectedRecordTypes.size();
  }

  // Helper methods to extract schema and package names from components
  private static String getSchemaName(Function function) {
    if (function.getParentPackage() != null) {
      return function.getParentPackage().getSchema();
    }
    return function.getSchema();
  }

  private static String getSchemaName(Procedure procedure) {
    if (procedure.getParentPackage() != null) {
      return procedure.getParentPackage().getSchema();
    }
    return procedure.getSchema();
  }

  private static String getPackageName(Function function) {
    if (function.getParentPackage() != null) {
      return function.getParentPackage().getName();
    }
    return "null";
  }

  private static String getPackageName(Procedure procedure) {
    if (procedure.getParentPackage() != null) {
      return procedure.getParentPackage().getName();
    }
    return "null";
  }
}