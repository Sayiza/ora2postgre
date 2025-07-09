package me.christianrobert.ora2postgre.integration;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.ConstraintMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.oracledb.ColumnMetadata;
import me.christianrobert.ora2postgre.writing.ExportConstraint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the complete constraint pipeline from TableMetadata to generated files.
 * Tests the full workflow of constraint extraction → export → file generation.
 */
class ConstraintPipelineIntegrationTest {

  private Everything everything;

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    everything = new Everything();

    // Create a comprehensive test scenario with all constraint types
    createTestDatabase();
  }

  @Test
  void testCompleteConstraintPipeline() throws IOException {
    // Act: Export all constraints using the main entry point
    ExportConstraint.saveConstraints(tempDir.toString(), everything);

    // Verify: Check that all expected constraint files were created
    verifyConstraintFilesCreated();
    verifyConstraintContents();
    verifyFileStructure();
  }

  @Test
  void testConstraintExecutionOrder() throws IOException {
    // Act: Export constraints
    ExportConstraint.saveConstraints(tempDir.toString(), everything);

    // Verify: Ensure files are organized in correct execution order directories
    Path schemaDir = tempDir.resolve("companydb").resolve("step8constraints");

    // Primary keys (no dependencies)
    assertTrue(Files.exists(schemaDir.resolve("primary_keys")));

    // Unique constraints (no dependencies)
    assertTrue(Files.exists(schemaDir.resolve("unique_constraints")));

    // Check constraints (no dependencies)
    assertTrue(Files.exists(schemaDir.resolve("check_constraints")));

    // Foreign keys (depend on target tables - execute last)
    assertTrue(Files.exists(schemaDir.resolve("foreign_keys")));
  }

  @Test
  void testConstraintValidation() throws IOException {
    // Create a scenario with invalid foreign key reference
    TableMetadata invalidTable = new TableMetadata("companydb", "invalid_table");

    ConstraintMetadata invalidFK = new ConstraintMetadata("fk_invalid", "R", "companydb", "non_existent_table");
    invalidFK.addColumnName("some_id");
    invalidFK.addReferencedColumnName("id");
    invalidTable.addConstraint(invalidFK);

    everything.getTableSql().add(invalidTable);

    // Act: Export constraints (should skip invalid references)
    ExportConstraint.saveConstraints(tempDir.toString(), everything);

    // Verify: Invalid foreign key should not create a file
    Path foreignKeyDir = tempDir.resolve("companydb").resolve("step8constraints").resolve("foreign_keys");

    if (Files.exists(foreignKeyDir)) {
      try (Stream<Path> files = Files.list(foreignKeyDir)) {
        long invalidFKFiles = files
                .map(path -> path.getFileName().toString())
                .filter(name -> name.equals("fk_invalid.sql"))
                .count();

        assertEquals(0, invalidFKFiles, "Invalid foreign key should not generate a file");
      }
    }
  }

  private void createTestDatabase() {
    // Create DEPARTMENTS table (referenced by EMPLOYEES)
    TableMetadata departments = createDepartmentsTable();
    everything.getTableSql().add(departments);

    // Create EMPLOYEES table (references DEPARTMENTS)
    TableMetadata employees = createEmployeesTable();
    everything.getTableSql().add(employees);

    // Create PROJECTS table (independent)
    TableMetadata projects = createProjectsTable();
    everything.getTableSql().add(projects);
  }

  private TableMetadata createDepartmentsTable() {
    TableMetadata table = new TableMetadata("companydb", "departments");

    // Add some columns for context
    table.addColumn(new ColumnMetadata("dept_id", "NUMBER", null, 10, 0, false, null));
    table.addColumn(new ColumnMetadata("dept_name", "VARCHAR2", 100, null, null, false, null));
    table.addColumn(new ColumnMetadata("budget", "NUMBER", null, 15, 2, true, null));

    // Primary key constraint
    ConstraintMetadata primaryKey = new ConstraintMetadata("pk_departments", "P");
    primaryKey.addColumnName("dept_id");
    table.addConstraint(primaryKey);

    // Unique constraint on department name
    ConstraintMetadata uniqueName = new ConstraintMetadata("uk_dept_name", "U");
    uniqueName.addColumnName("dept_name");
    table.addConstraint(uniqueName);

    // Check constraint on budget
    ConstraintMetadata checkBudget = new ConstraintMetadata("chk_dept_budget", "C");
    checkBudget.setCheckCondition("budget > 0");
    table.addConstraint(checkBudget);

    return table;
  }

  private TableMetadata createEmployeesTable() {
    TableMetadata table = new TableMetadata("companydb", "employees");

    // Add some columns for context
    table.addColumn(new ColumnMetadata("emp_id", "NUMBER", null, 10, 0, false, null));
    table.addColumn(new ColumnMetadata("emp_name", "VARCHAR2", 100, null, null, false, null));
    table.addColumn(new ColumnMetadata("email", "VARCHAR2", 255, null, null, false, null));
    table.addColumn(new ColumnMetadata("salary", "NUMBER", null, 10, 2, true, null));
    table.addColumn(new ColumnMetadata("dept_id", "NUMBER", null, 10, 0, true, null));

    // Primary key constraint
    ConstraintMetadata primaryKey = new ConstraintMetadata("pk_employees", "P");
    primaryKey.addColumnName("emp_id");
    table.addConstraint(primaryKey);

    // Unique constraint on email
    ConstraintMetadata uniqueEmail = new ConstraintMetadata("uk_emp_email", "U");
    uniqueEmail.addColumnName("email");
    table.addConstraint(uniqueEmail);

    // Check constraint on salary
    ConstraintMetadata checkSalary = new ConstraintMetadata("chk_emp_salary", "C");
    checkSalary.setCheckCondition("salary > 0 AND salary <= 999999.99");
    table.addConstraint(checkSalary);

    // Foreign key constraint to departments
    ConstraintMetadata foreignKey = new ConstraintMetadata("fk_emp_dept", "R", "companydb", "departments");
    foreignKey.addColumnName("dept_id");
    foreignKey.addReferencedColumnName("dept_id");
    foreignKey.setDeleteRule("SET NULL");
    table.addConstraint(foreignKey);

    return table;
  }

  private TableMetadata createProjectsTable() {
    TableMetadata table = new TableMetadata("companydb", "projects");

    // Add some columns for context
    table.addColumn(new ColumnMetadata("project_id", "NUMBER", null, 10, 0, false, null));
    table.addColumn(new ColumnMetadata("project_name", "VARCHAR2", 200, null, null, false, null));
    table.addColumn(new ColumnMetadata("status", "VARCHAR2", 20, null, null, false, "'ACTIVE'"));

    // Composite primary key constraint
    ConstraintMetadata primaryKey = new ConstraintMetadata("pk_projects", "P");
    primaryKey.addColumnName("project_id");
    table.addConstraint(primaryKey);

    // Check constraint on status
    ConstraintMetadata checkStatus = new ConstraintMetadata("chk_project_status", "C");
    checkStatus.setCheckCondition("status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')");
    table.addConstraint(checkStatus);

    return table;
  }

  private void verifyConstraintFilesCreated() throws IOException {
    Path schemaDir = tempDir.resolve("companydb").resolve("step8constraints");

    // Primary keys
    assertTrue(Files.exists(schemaDir.resolve("primary_keys").resolve("pk_departments.sql")));
    assertTrue(Files.exists(schemaDir.resolve("primary_keys").resolve("pk_employees.sql")));
    assertTrue(Files.exists(schemaDir.resolve("primary_keys").resolve("pk_projects.sql")));

    // Unique constraints
    assertTrue(Files.exists(schemaDir.resolve("unique_constraints").resolve("uk_dept_name.sql")));
    assertTrue(Files.exists(schemaDir.resolve("unique_constraints").resolve("uk_emp_email.sql")));

    // Check constraints
    assertTrue(Files.exists(schemaDir.resolve("check_constraints").resolve("chk_dept_budget.sql")));
    assertTrue(Files.exists(schemaDir.resolve("check_constraints").resolve("chk_emp_salary.sql")));
    assertTrue(Files.exists(schemaDir.resolve("check_constraints").resolve("chk_project_status.sql")));

    // Foreign keys
    assertTrue(Files.exists(schemaDir.resolve("foreign_keys").resolve("fk_emp_dept.sql")));
  }

  private void verifyConstraintContents() throws IOException {
    Path schemaDir = tempDir.resolve("companydb").resolve("step8constraints");

    // Verify primary key DDL
    String pkContent = Files.readString(schemaDir.resolve("primary_keys").resolve("pk_departments.sql"));
    assertTrue(pkContent.contains("-- Constraint: pk_departments"));
    assertTrue(pkContent.contains("-- Type: PRIMARY KEY"));
    assertTrue(pkContent.contains("-- Table: companydb.departments"));
    assertTrue(pkContent.contains("ALTER TABLE companydb.departments ADD CONSTRAINT pk_departments PRIMARY KEY (dept_id);"));

    // Verify foreign key DDL with referential action
    String fkContent = Files.readString(schemaDir.resolve("foreign_keys").resolve("fk_emp_dept.sql"));
    assertTrue(fkContent.contains("-- Constraint: fk_emp_dept"));
    assertTrue(fkContent.contains("-- Type: FOREIGN KEY"));
    assertTrue(fkContent.contains("-- Table: companydb.employees"));
    assertTrue(fkContent.contains("-- References: companydb.departments"));
    assertTrue(fkContent.contains("FOREIGN KEY (dept_id) REFERENCES companydb.departments (dept_id)"));
    assertTrue(fkContent.contains("ON DELETE SET NULL"));

    // Verify check constraint DDL
    String checkContent = Files.readString(schemaDir.resolve("check_constraints").resolve("chk_project_status.sql"));
    assertTrue(checkContent.contains("-- Constraint: chk_project_status"));
    assertTrue(checkContent.contains("-- Type: CHECK"));
    assertTrue(checkContent.contains("CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED'))"));
  }

  private void verifyFileStructure() throws IOException {
    Path schemaDir = tempDir.resolve("companydb").resolve("step8constraints");

    // Verify all constraint type directories exist
    assertTrue(Files.isDirectory(schemaDir.resolve("primary_keys")));
    assertTrue(Files.isDirectory(schemaDir.resolve("unique_constraints")));
    assertTrue(Files.isDirectory(schemaDir.resolve("check_constraints")));
    assertTrue(Files.isDirectory(schemaDir.resolve("foreign_keys")));

    // Count files in each directory
    try (Stream<Path> primaryKeys = Files.list(schemaDir.resolve("primary_keys"))) {
      assertEquals(3, primaryKeys.count(), "Should have 3 primary key constraint files");
    }

    try (Stream<Path> uniqueConstraints = Files.list(schemaDir.resolve("unique_constraints"))) {
      assertEquals(2, uniqueConstraints.count(), "Should have 2 unique constraint files");
    }

    try (Stream<Path> checkConstraints = Files.list(schemaDir.resolve("check_constraints"))) {
      assertEquals(3, checkConstraints.count(), "Should have 3 check constraint files");
    }

    try (Stream<Path> foreignKeys = Files.list(schemaDir.resolve("foreign_keys"))) {
      assertEquals(1, foreignKeys.count(), "Should have 1 foreign key constraint file");
    }
  }
}