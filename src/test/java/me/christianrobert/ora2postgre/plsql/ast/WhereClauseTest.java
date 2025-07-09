package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WhereClauseTest {

  @Test
  void testCursorBasedWhereClause() {
    WhereClause whereClause = new WhereClause("my_cursor");

    assertTrue(whereClause.isCursorBased());
    assertEquals("my_cursor", whereClause.getCursorName());
    assertNull(whereClause.getCondition());
    assertEquals("WHERE CURRENT OF my_cursor", whereClause.toString());
  }

  @Test
  void testConditionBasedWhereClause() {
    LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression("column1 = 'value1'"));
    Expression condition = new Expression(logicalExpr);
    WhereClause whereClause = new WhereClause(condition);

    assertFalse(whereClause.isCursorBased());
    assertNull(whereClause.getCursorName());
    assertEquals(condition, whereClause.getCondition());
    assertEquals("WHERE column1 = 'value1'", whereClause.toString());
  }

  @Test
  void testPostgreOutputForCursorWhereClause() {
    WhereClause whereClause = new WhereClause("test_cursor");
    Everything data = new Everything();

    assertEquals("WHERE CURRENT OF test_cursor", whereClause.toPostgre(data));
  }

  @Test
  void testPostgreOutputForConditionWhereClause() {
    LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression("age > 18 AND status = 'active'"));
    Expression condition = new Expression(logicalExpr);
    WhereClause whereClause = new WhereClause(condition);
    Everything data = new Everything();

    assertEquals("WHERE age > 18 AND status = 'active'", whereClause.toPostgre(data));
  }

  @Test
  void testSelectQueryBlockWithWhereClause() {
    // Create a simple SelectQueryBlock with WHERE clause
    LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression("active = 1"));
    Expression condition = new Expression(logicalExpr);
    WhereClause whereClause = new WhereClause(condition);

    // Test that WhereClause can be created and used
    assertNotNull(whereClause);
    assertEquals(condition, whereClause.getCondition());
    assertFalse(whereClause.isCursorBased());

    // Test PostgreSQL output
    Everything data = new Everything();
    String postgreOutput = whereClause.toPostgre(data);
    assertEquals("WHERE active = 1", postgreOutput);
  }
}