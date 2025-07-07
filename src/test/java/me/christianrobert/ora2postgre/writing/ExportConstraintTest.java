package me.christianrobert.ora2postgre.writing;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.ConstraintMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExportConstraint class.
 * Tests constraint file generation and DDL output for all constraint types.
 */
class ExportConstraintTest {

    private Everything everything;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        everything = new Everything();
        
        // Create test tables with constraints
        TableMetadata employeeTable = createEmployeeTable();
        TableMetadata orderTable = createOrderTable();
        
        everything.getTableSql().add(employeeTable);
        everything.getTableSql().add(orderTable);
    }

    @Test
    void testSaveConstraints_AllTypes() throws IOException {
        // Export all constraints
        ExportConstraint.saveConstraints(tempDir.toString(), everything);
        
        // Verify directory structure was created
        Path testschemaDir = tempDir.resolve("testschema").resolve("step8constraints");
        assertTrue(Files.exists(testschemaDir.resolve("primary_keys")));
        assertTrue(Files.exists(testschemaDir.resolve("unique_constraints")));
        assertTrue(Files.exists(testschemaDir.resolve("check_constraints")));
        assertTrue(Files.exists(testschemaDir.resolve("foreign_keys")));
        
        // Verify primary key file exists
        Path primaryKeyFile = testschemaDir.resolve("primary_keys").resolve("pk_employees.sql");
        assertTrue(Files.exists(primaryKeyFile));
        
        // Verify unique constraint file exists
        Path uniqueFile = testschemaDir.resolve("unique_constraints").resolve("uk_emp_email.sql");
        assertTrue(Files.exists(uniqueFile));
        
        // Verify check constraint file exists
        Path checkFile = testschemaDir.resolve("check_constraints").resolve("chk_emp_salary.sql");
        assertTrue(Files.exists(checkFile));
        
        // Verify foreign key file exists
        Path foreignKeyFile = testschemaDir.resolve("foreign_keys").resolve("fk_orders_employee.sql");
        assertTrue(Files.exists(foreignKeyFile));
    }

    @Test
    void testPrimaryKeyConstraintDDL() throws IOException {
        ExportConstraint.savePrimaryKeyConstraints(tempDir.toString(), everything);
        
        Path primaryKeyFile = tempDir.resolve("testschema").resolve("step8constraints")
            .resolve("primary_keys").resolve("pk_employees.sql");
        
        assertTrue(Files.exists(primaryKeyFile));
        
        String content = Files.readString(primaryKeyFile);
        assertTrue(content.contains("-- Constraint: pk_employees"));
        assertTrue(content.contains("-- Type: PRIMARY KEY"));
        assertTrue(content.contains("-- Table: testschema.employees"));
        assertTrue(content.contains("ALTER TABLE testschema.employees ADD CONSTRAINT pk_employees PRIMARY KEY (employee_id);"));
    }

    @Test
    void testUniqueConstraintDDL() throws IOException {
        ExportConstraint.saveUniqueConstraints(tempDir.toString(), everything);
        
        Path uniqueFile = tempDir.resolve("testschema").resolve("step8constraints")
            .resolve("unique_constraints").resolve("uk_emp_email.sql");
        
        assertTrue(Files.exists(uniqueFile));
        
        String content = Files.readString(uniqueFile);
        assertTrue(content.contains("-- Constraint: uk_emp_email"));
        assertTrue(content.contains("-- Type: UNIQUE"));
        assertTrue(content.contains("-- Table: testschema.employees"));
        assertTrue(content.contains("ALTER TABLE testschema.employees ADD CONSTRAINT uk_emp_email UNIQUE (email);"));
    }

    @Test
    void testCheckConstraintDDL() throws IOException {
        ExportConstraint.saveCheckConstraints(tempDir.toString(), everything);
        
        Path checkFile = tempDir.resolve("testschema").resolve("step8constraints")
            .resolve("check_constraints").resolve("chk_emp_salary.sql");
        
        assertTrue(Files.exists(checkFile));
        
        String content = Files.readString(checkFile);
        assertTrue(content.contains("-- Constraint: chk_emp_salary"));
        assertTrue(content.contains("-- Type: CHECK"));
        assertTrue(content.contains("-- Table: testschema.employees"));
        assertTrue(content.contains("ALTER TABLE testschema.employees ADD CONSTRAINT chk_emp_salary CHECK (salary > 0);"));
    }

    @Test
    void testForeignKeyConstraintDDL() throws IOException {
        ExportConstraint.saveForeignKeyConstraints(tempDir.toString(), everything);
        
        Path foreignKeyFile = tempDir.resolve("testschema").resolve("step8constraints")
            .resolve("foreign_keys").resolve("fk_orders_employee.sql");
        
        assertTrue(Files.exists(foreignKeyFile));
        
        String content = Files.readString(foreignKeyFile);
        assertTrue(content.contains("-- Constraint: fk_orders_employee"));
        assertTrue(content.contains("-- Type: FOREIGN KEY"));
        assertTrue(content.contains("-- Table: testschema.orders"));
        assertTrue(content.contains("-- References: testschema.employees"));
        assertTrue(content.contains("ALTER TABLE testschema.orders ADD CONSTRAINT fk_orders_employee FOREIGN KEY (employee_id) REFERENCES testschema.employees (employee_id) ON DELETE CASCADE;"));
    }

    @Test
    void testEmptyConstraints() throws IOException {
        // Create everything context with no constraints
        Everything emptyEverything = new Everything();
        TableMetadata emptyTable = new TableMetadata("testschema", "empty_table");
        emptyEverything.getTableSql().add(emptyTable);
        
        // Export constraints (should handle empty case gracefully)
        ExportConstraint.saveConstraints(tempDir.toString(), emptyEverything);
        
        // Verify no constraint directories were created
        Path schemaDir = tempDir.resolve("testschema");
        if (Files.exists(schemaDir)) {
            Path constraintsDir = schemaDir.resolve("step8constraints");
            assertFalse(Files.exists(constraintsDir));
        }
    }

    @Test
    void testForeignKeyValidation() throws IOException {
        // Create a foreign key that references a non-existent table
        Everything invalidEverything = new Everything();
        TableMetadata table = new TableMetadata("testschema", "invalid_table");
        
        ConstraintMetadata invalidFK = new ConstraintMetadata("fk_invalid", "R", "testschema", "non_existent_table");
        invalidFK.addColumnName("some_id");
        invalidFK.addReferencedColumnName("id");
        table.addConstraint(invalidFK);
        
        invalidEverything.getTableSql().add(table);
        
        // Export foreign keys (should skip invalid references)
        ExportConstraint.saveForeignKeyConstraints(tempDir.toString(), invalidEverything);
        
        // Verify no foreign key files were created (invalid reference skipped)
        Path foreignKeyDir = tempDir.resolve("testschema").resolve("step8constraints").resolve("foreign_keys");
        if (Files.exists(foreignKeyDir)) {
            try (var stream = Files.list(foreignKeyDir)) {
                assertEquals(0, stream.count(), "No foreign key files should be created for invalid references");
            }
        }
    }

    // Helper methods to create test data

    private TableMetadata createEmployeeTable() {
        TableMetadata table = new TableMetadata("testschema", "employees");
        
        // Primary key constraint
        ConstraintMetadata primaryKey = new ConstraintMetadata("pk_employees", "P");
        primaryKey.addColumnName("employee_id");
        table.addConstraint(primaryKey);
        
        // Unique constraint
        ConstraintMetadata uniqueEmail = new ConstraintMetadata("uk_emp_email", "U");
        uniqueEmail.addColumnName("email");
        table.addConstraint(uniqueEmail);
        
        // Check constraint
        ConstraintMetadata checkSalary = new ConstraintMetadata("chk_emp_salary", "C");
        checkSalary.setCheckCondition("salary > 0");
        table.addConstraint(checkSalary);
        
        return table;
    }

    private TableMetadata createOrderTable() {
        TableMetadata table = new TableMetadata("testschema", "orders");
        
        // Primary key constraint
        ConstraintMetadata primaryKey = new ConstraintMetadata("pk_orders", "P");
        primaryKey.addColumnName("order_id");
        table.addConstraint(primaryKey);
        
        // Foreign key constraint
        ConstraintMetadata foreignKey = new ConstraintMetadata("fk_orders_employee", "R", "testschema", "employees");
        foreignKey.addColumnName("employee_id");
        foreignKey.addReferencedColumnName("employee_id");
        foreignKey.setDeleteRule("CASCADE");
        table.addConstraint(foreignKey);
        
        return table;
    }
}