package me.christianrobert.ora2postgre.writing;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.ConstraintMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Export utility for generating PostgreSQL constraint files.
 * 
 * Implements dependency-aware constraint export strategy:
 * 1. Primary Key constraints (no dependencies)
 * 2. Unique constraints (no dependencies)  
 * 3. Check constraints (no dependencies)
 * 4. Foreign Key constraints (depend on target tables and their primary keys)
 * 
 * This ensures constraints are created in the correct order during execution.
 */
public class ExportConstraint {
    
    private static final Logger log = LoggerFactory.getLogger(ExportConstraint.class);
    
    /**
     * Main entry point for constraint export. Generates all constraint types
     * in the correct dependency order and directory structure.
     * 
     * @param basePath Base path for file generation (e.g., target-project/postgre/autoddl/)
     * @param everything Global context containing all table and constraint data
     */
    public static void saveConstraints(String basePath, Everything everything) {
        log.info("Starting constraint export to base path: {}", basePath);
        
        List<TableMetadata> tables = everything.getTableSql();
        
        if (tables.isEmpty()) {
            log.info("No tables found - skipping constraint export");
            return;
        }
        
        // Count total constraints
        int totalConstraints = tables.stream()
            .mapToInt(table -> table.getConstraints().size())
            .sum();
            
        if (totalConstraints == 0) {
            log.info("No constraints found to export");
            return;
        }
        
        log.info("Exporting {} constraints from {} tables", totalConstraints, tables.size());
        
        // Export constraints by type in dependency order
        savePrimaryKeyConstraints(basePath, everything);
        saveUniqueConstraints(basePath, everything);
        saveCheckConstraints(basePath, everything);
        saveForeignKeyConstraints(basePath, everything);
        
        log.info("Constraint export completed successfully");
    }
    
    /**
     * Export PRIMARY KEY constraints to step8constraints/primary_keys/ directory.
     * Primary keys have no dependencies and can be created first.
     * 
     * @param basePath Base path for file generation
     * @param everything Global context containing table data
     */
    public static void savePrimaryKeyConstraints(String basePath, Everything everything) {
        log.info("Exporting primary key constraints...");
        
        List<ConstraintMetadata> primaryKeys = extractConstraintsByType(everything, "P");
        
        if (primaryKeys.isEmpty()) {
            log.info("No primary key constraints found");
            return;
        }
        
        Map<String, Integer> schemaCounts = new HashMap<>();
        
        for (ConstraintMetadata constraint : primaryKeys) {
            String constraintContent = generateConstraintDDL(constraint, everything);
            String fileName = getConstraintFileName(constraint);
            String fullPath = getConstraintPath(basePath, getSchemaForConstraint(constraint, everything), "primary_keys");
            
            FileWriter.write(Paths.get(fullPath), fileName, constraintContent);
            
            // Track statistics
            String schema = getSchemaForConstraint(constraint, everything);
            schemaCounts.merge(schema, 1, Integer::sum);
        }
        
        // Log export statistics
        int totalPrimaryKeys = schemaCounts.values().stream().mapToInt(Integer::intValue).sum();
        log.info("Exported {} primary key constraints across {} schemas", totalPrimaryKeys, schemaCounts.size());
        for (Map.Entry<String, Integer> entry : schemaCounts.entrySet()) {
            log.debug("  Schema {}: {} primary keys", entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Export UNIQUE constraints to step8constraints/unique_constraints/ directory.
     * Unique constraints have no dependencies and can be created early.
     * 
     * @param basePath Base path for file generation
     * @param everything Global context containing table data
     */
    public static void saveUniqueConstraints(String basePath, Everything everything) {
        log.info("Exporting unique constraints...");
        
        List<ConstraintMetadata> uniqueConstraints = extractConstraintsByType(everything, "U");
        
        if (uniqueConstraints.isEmpty()) {
            log.info("No unique constraints found");
            return;
        }
        
        Map<String, Integer> schemaCounts = new HashMap<>();
        
        for (ConstraintMetadata constraint : uniqueConstraints) {
            String constraintContent = generateConstraintDDL(constraint, everything);
            String fileName = getConstraintFileName(constraint);
            String fullPath = getConstraintPath(basePath, getSchemaForConstraint(constraint, everything), "unique_constraints");
            
            FileWriter.write(Paths.get(fullPath), fileName, constraintContent);
            
            // Track statistics
            String schema = getSchemaForConstraint(constraint, everything);
            schemaCounts.merge(schema, 1, Integer::sum);
        }
        
        // Log export statistics
        int totalUnique = schemaCounts.values().stream().mapToInt(Integer::intValue).sum();
        log.info("Exported {} unique constraints across {} schemas", totalUnique, schemaCounts.size());
        for (Map.Entry<String, Integer> entry : schemaCounts.entrySet()) {
            log.debug("  Schema {}: {} unique constraints", entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Export CHECK constraints to step8constraints/check_constraints/ directory.
     * Check constraints have no dependencies and can be created early.
     * 
     * @param basePath Base path for file generation
     * @param everything Global context containing table data
     */
    public static void saveCheckConstraints(String basePath, Everything everything) {
        log.info("Exporting check constraints...");
        
        List<ConstraintMetadata> checkConstraints = extractConstraintsByType(everything, "C");
        
        if (checkConstraints.isEmpty()) {
            log.info("No check constraints found");
            return;
        }
        
        Map<String, Integer> schemaCounts = new HashMap<>();
        
        for (ConstraintMetadata constraint : checkConstraints) {
            String constraintContent = generateConstraintDDL(constraint, everything);
            String fileName = getConstraintFileName(constraint);
            String fullPath = getConstraintPath(basePath, getSchemaForConstraint(constraint, everything), "check_constraints");
            
            FileWriter.write(Paths.get(fullPath), fileName, constraintContent);
            
            // Track statistics
            String schema = getSchemaForConstraint(constraint, everything);
            schemaCounts.merge(schema, 1, Integer::sum);
        }
        
        // Log export statistics
        int totalCheck = schemaCounts.values().stream().mapToInt(Integer::intValue).sum();
        log.info("Exported {} check constraints across {} schemas", totalCheck, schemaCounts.size());
        for (Map.Entry<String, Integer> entry : schemaCounts.entrySet()) {
            log.debug("  Schema {}: {} check constraints", entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Export FOREIGN KEY constraints to step8constraints/foreign_keys/ directory.
     * Foreign keys must be created last as they depend on target tables and their primary keys.
     * 
     * @param basePath Base path for file generation
     * @param everything Global context containing table data
     */
    public static void saveForeignKeyConstraints(String basePath, Everything everything) {
        log.info("Exporting foreign key constraints...");
        
        List<ConstraintMetadata> foreignKeys = extractConstraintsByType(everything, "R");
        
        if (foreignKeys.isEmpty()) {
            log.info("No foreign key constraints found");
            return;
        }
        
        Map<String, Integer> schemaCounts = new HashMap<>();
        int validForeignKeys = 0;
        int skippedForeignKeys = 0;
        
        for (ConstraintMetadata constraint : foreignKeys) {
            // Validate foreign key references before generating DDL
            if (validateForeignKeyReferences(constraint, everything)) {
                String constraintContent = generateConstraintDDL(constraint, everything);
                String fileName = getConstraintFileName(constraint);
                String fullPath = getConstraintPath(basePath, getSchemaForConstraint(constraint, everything), "foreign_keys");
                
                FileWriter.write(Paths.get(fullPath), fileName, constraintContent);
                
                // Track statistics
                String schema = getSchemaForConstraint(constraint, everything);
                schemaCounts.merge(schema, 1, Integer::sum);
                validForeignKeys++;
            } else {
                log.warn("Skipping foreign key constraint {} - referenced table not found", constraint.getConstraintName());
                skippedForeignKeys++;
            }
        }
        
        // Log export statistics
        log.info("Exported {} foreign key constraints across {} schemas", validForeignKeys, schemaCounts.size());
        if (skippedForeignKeys > 0) {
            log.warn("Skipped {} foreign key constraints due to missing referenced tables", skippedForeignKeys);
        }
        for (Map.Entry<String, Integer> entry : schemaCounts.entrySet()) {
            log.debug("  Schema {}: {} foreign keys", entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Extracts all constraints of a specific type from all tables.
     * 
     * @param everything Global context containing table data
     * @param constraintType Oracle constraint type ("P", "R", "U", "C")
     * @return List of constraints of the specified type
     */
    private static List<ConstraintMetadata> extractConstraintsByType(Everything everything, String constraintType) {
        List<ConstraintMetadata> constraints = new ArrayList<>();
        
        for (TableMetadata table : everything.getTableSql()) {
            for (ConstraintMetadata constraint : table.getConstraints()) {
                if (constraintType.equals(constraint.getConstraintType())) {
                    constraints.add(constraint);
                }
            }
        }
        
        return constraints;
    }
    
    /**
     * Generates PostgreSQL DDL for a constraint.
     * 
     * @param constraint Constraint metadata
     * @param everything Global context for additional validation
     * @return Complete DDL string with ALTER TABLE statement
     */
    private static String generateConstraintDDL(ConstraintMetadata constraint, Everything everything) {
        String schema = getSchemaForConstraint(constraint, everything);
        String tableName = getTableForConstraint(constraint, everything);
        
        if (schema == null || tableName == null) {
            log.error("Could not determine schema/table for constraint: {}", constraint.getConstraintName());
            return "-- ERROR: Could not determine table for constraint " + constraint.getConstraintName();
        }
        
        StringBuilder ddl = new StringBuilder();
        
        // Add header comment
        ddl.append("-- Constraint: ").append(constraint.getConstraintName()).append("\n");
        ddl.append("-- Type: ").append(constraint.getConstraintTypeName()).append("\n");
        ddl.append("-- Table: ").append(schema).append(".").append(tableName).append("\n");
        if (constraint.isForeignKey()) {
            ddl.append("-- References: ").append(constraint.getReferencedSchema()).append(".").append(constraint.getReferencedTable()).append("\n");
        }
        ddl.append("\n");
        
        // Generate ALTER TABLE statement
        ddl.append(constraint.toPostgreAlterTableDDL(schema, tableName));
        
        return ddl.toString();
    }
    
    /**
     * Gets the file name for a constraint DDL file.
     * 
     * @param constraint Constraint metadata
     * @return File name (e.g., "pk_employees.sql")
     */
    private static String getConstraintFileName(ConstraintMetadata constraint) {
        String constraintName = constraint.getConstraintName().toLowerCase();
        return constraintName + ".sql";
    }
    
    /**
     * Gets the full directory path for constraint files.
     * 
     * @param basePath Base path for file generation
     * @param schema Schema name
     * @param constraintType Constraint type directory (e.g., "primary_keys", "foreign_keys")
     * @return Full directory path
     */
    private static String getConstraintPath(String basePath, String schema, String constraintType) {
        return basePath + File.separator + schema.toLowerCase() + File.separator + "step8constraints" + File.separator + constraintType;
    }
    
    /**
     * Finds the schema for a constraint by looking through all tables.
     * 
     * @param constraint Constraint to find schema for
     * @param everything Global context containing table data
     * @return Schema name or null if not found
     */
    private static String getSchemaForConstraint(ConstraintMetadata constraint, Everything everything) {
        for (TableMetadata table : everything.getTableSql()) {
            if (table.getConstraints().contains(constraint)) {
                return table.getSchema();
            }
        }
        return null;
    }
    
    /**
     * Finds the table name for a constraint by looking through all tables.
     * 
     * @param constraint Constraint to find table for
     * @param everything Global context containing table data
     * @return Table name or null if not found
     */
    private static String getTableForConstraint(ConstraintMetadata constraint, Everything everything) {
        for (TableMetadata table : everything.getTableSql()) {
            if (table.getConstraints().contains(constraint)) {
                return table.getTableName();
            }
        }
        return null;
    }
    
    /**
     * Validates that a foreign key constraint references a valid table.
     * 
     * @param constraint Foreign key constraint to validate
     * @param everything Global context containing table data
     * @return true if referenced table exists, false otherwise
     */
    private static boolean validateForeignKeyReferences(ConstraintMetadata constraint, Everything everything) {
        if (!constraint.isForeignKey()) {
            return true; // Not a foreign key, validation passes
        }
        
        String referencedSchema = constraint.getReferencedSchema();
        String referencedTable = constraint.getReferencedTable();
        
        if (referencedSchema == null || referencedTable == null) {
            return false;
        }
        
        // Check if referenced table exists in our migration
        for (TableMetadata table : everything.getTableSql()) {
            if (referencedSchema.equalsIgnoreCase(table.getSchema()) && 
                referencedTable.equalsIgnoreCase(table.getTableName())) {
                return true;
            }
        }
        
        // Table not found in migration
        return false;
    }
}