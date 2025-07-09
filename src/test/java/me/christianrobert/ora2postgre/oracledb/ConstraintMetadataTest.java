package me.christianrobert.ora2postgre.oracledb;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for enhanced ConstraintMetadata class.
 * Tests comprehensive constraint support (PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK).
 */
class ConstraintMetadataTest {

  @Test
  void testPrimaryKeyConstraint() {
    ConstraintMetadata constraint = new ConstraintMetadata("pk_test", "P");
    constraint.addColumnName("id");

    assertTrue(constraint.isPrimaryKey());
    assertFalse(constraint.isForeignKey());
    assertFalse(constraint.isUniqueConstraint());
    assertFalse(constraint.isCheckConstraint());

    assertEquals("PRIMARY KEY", constraint.getConstraintTypeName());
    assertTrue(constraint.isValid());

    String ddl = constraint.toPostgreConstraintDDL();
    assertTrue(ddl.contains("CONSTRAINT pk_test PRIMARY KEY (id)"));
  }

  @Test
  void testForeignKeyConstraint() {
    ConstraintMetadata constraint = new ConstraintMetadata("fk_test", "R", "public", "parent_table");
    constraint.addColumnName("parent_id");
    constraint.addReferencedColumnName("id");
    constraint.setDeleteRule("CASCADE");

    assertFalse(constraint.isPrimaryKey());
    assertTrue(constraint.isForeignKey());
    assertFalse(constraint.isUniqueConstraint());
    assertFalse(constraint.isCheckConstraint());

    assertEquals("FOREIGN KEY", constraint.getConstraintTypeName());
    assertEquals("public", constraint.getReferencedSchema());
    assertEquals("parent_table", constraint.getReferencedTable());
    assertEquals("CASCADE", constraint.getDeleteRule());
    assertTrue(constraint.isValid());

    String ddl = constraint.toPostgreConstraintDDL();
    assertTrue(ddl.contains("FOREIGN KEY"));
    assertTrue(ddl.contains("REFERENCES public.parent_table"));
    assertTrue(ddl.contains("ON DELETE CASCADE"));
  }

  @Test
  void testUniqueConstraint() {
    ConstraintMetadata constraint = new ConstraintMetadata("uk_test", "U");
    constraint.addColumnName("email");

    assertFalse(constraint.isPrimaryKey());
    assertFalse(constraint.isForeignKey());
    assertTrue(constraint.isUniqueConstraint());
    assertFalse(constraint.isCheckConstraint());

    assertEquals("UNIQUE", constraint.getConstraintTypeName());
    assertTrue(constraint.isValid());

    String ddl = constraint.toPostgreConstraintDDL();
    assertTrue(ddl.contains("CONSTRAINT uk_test UNIQUE (email)"));
  }

  @Test
  void testCheckConstraint() {
    ConstraintMetadata constraint = new ConstraintMetadata("chk_test", "C");
    constraint.setCheckCondition("salary > 0");

    assertFalse(constraint.isPrimaryKey());
    assertFalse(constraint.isForeignKey());
    assertFalse(constraint.isUniqueConstraint());
    assertTrue(constraint.isCheckConstraint());

    assertEquals("CHECK", constraint.getConstraintTypeName());
    assertEquals("salary > 0", constraint.getCheckCondition());
    assertTrue(constraint.isValid());

    String ddl = constraint.toPostgreConstraintDDL();
    assertTrue(ddl.contains("CONSTRAINT chk_test CHECK (salary > 0)"));
  }

  @Test
  void testCompositePrimaryKey() {
    ConstraintMetadata constraint = new ConstraintMetadata("pk_composite", "P");
    constraint.addColumnName("tenant_id");
    constraint.addColumnName("user_id");

    assertTrue(constraint.isPrimaryKey());
    assertEquals(2, constraint.getColumnNames().size());
    assertTrue(constraint.isValid());

    String ddl = constraint.toPostgreConstraintDDL();
    assertTrue(ddl.contains("PRIMARY KEY (tenant_id, user_id)"));
  }

  @Test
  void testCompositeForeignKey() {
    ConstraintMetadata constraint = new ConstraintMetadata("fk_composite", "R", "public", "parent_table");
    constraint.addColumnName("tenant_id");
    constraint.addColumnName("parent_id");
    constraint.addReferencedColumnName("tenant_id");
    constraint.addReferencedColumnName("id");
    constraint.setDeleteRule("SET NULL");

    assertTrue(constraint.isForeignKey());
    assertEquals(2, constraint.getColumnNames().size());
    assertEquals(2, constraint.getReferencedColumns().size());
    assertTrue(constraint.isValid());

    String ddl = constraint.toPostgreConstraintDDL();
    assertTrue(ddl.contains("FOREIGN KEY (tenant_id, parent_id)"));
    assertTrue(ddl.contains("REFERENCES public.parent_table (tenant_id, id)"));
    assertTrue(ddl.contains("ON DELETE SET NULL"));
  }

  @Test
  void testDeferrableConstraint() {
    ConstraintMetadata constraint = new ConstraintMetadata("pk_deferred", "P");
    constraint.addColumnName("id");
    constraint.setDeferrable(true);
    constraint.setInitiallyDeferred(true);

    assertTrue(constraint.isDeferrable());
    assertTrue(constraint.isInitiallyDeferred());

    String ddl = constraint.toPostgreConstraintDDL();
    assertTrue(ddl.contains("DEFERRABLE INITIALLY DEFERRED"));
  }

  @Test
  void testAlterTableDDLGeneration() {
    ConstraintMetadata constraint = new ConstraintMetadata("pk_test", "P");
    constraint.addColumnName("id");

    String alterDDL = constraint.toPostgreAlterTableDDL("test_schema", "test_table");
    assertEquals("ALTER TABLE test_schema.test_table ADD CONSTRAINT pk_test PRIMARY KEY (id);",
            alterDDL);
  }

  @Test
  void testCheckConditionTransformation() {
    ConstraintMetadata constraint = new ConstraintMetadata("chk_oracle", "C");
    // Test Oracle function transformation
    constraint.setCheckCondition("created_date >= SYSDATE AND status IN ('ACTIVE', 'PENDING')");

    String ddl = constraint.toPostgreConstraintDDL();
    assertTrue(ddl.contains("CURRENT_TIMESTAMP")); // SYSDATE should be transformed
    assertFalse(ddl.contains("SYSDATE")); // Original Oracle function should be gone
  }

  @Test
  void testConstraintValidation() {
    // Valid primary key
    ConstraintMetadata validPK = new ConstraintMetadata("pk_valid", "P");
    validPK.addColumnName("id");
    assertTrue(validPK.isValid());

    // Invalid primary key (no columns)
    ConstraintMetadata invalidPK = new ConstraintMetadata("pk_invalid", "P");
    assertFalse(invalidPK.isValid());

    // Valid foreign key
    ConstraintMetadata validFK = new ConstraintMetadata("fk_valid", "R", "public", "parent");
    validFK.addColumnName("parent_id");
    assertTrue(validFK.isValid());

    // Invalid foreign key (no referenced table)
    ConstraintMetadata invalidFK = new ConstraintMetadata("fk_invalid", "R");
    invalidFK.addColumnName("parent_id");
    assertFalse(invalidFK.isValid());

    // Valid check constraint
    ConstraintMetadata validCheck = new ConstraintMetadata("chk_valid", "C");
    validCheck.setCheckCondition("value > 0");
    assertTrue(validCheck.isValid());

    // Invalid check constraint (no condition)
    ConstraintMetadata invalidCheck = new ConstraintMetadata("chk_invalid", "C");
    assertFalse(invalidCheck.isValid());
  }

  @Test
  void testConstraintToString() {
    ConstraintMetadata fkConstraint = new ConstraintMetadata("fk_test", "R", "public", "parent_table");
    fkConstraint.addColumnName("parent_id");
    fkConstraint.addReferencedColumnName("id");
    fkConstraint.setStatus("ENABLED");

    String str = fkConstraint.toString();
    assertTrue(str.contains("fk_test"));
    assertTrue(str.contains("R"));
    assertTrue(str.contains("parent_table"));
    assertTrue(str.contains("ENABLED"));
  }
}