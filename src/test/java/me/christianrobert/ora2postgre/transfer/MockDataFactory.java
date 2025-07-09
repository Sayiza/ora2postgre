package me.christianrobert.ora2postgre.transfer;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.ColumnMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.plsql.ast.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Factory class for creating mock data objects for testing data conversion functionality.
 * Focuses on the langtable/langdata2 example with Oracle object types.
 */
public class MockDataFactory {

  /**
   * Creates a mock TableMetadata for the langtable example:
   * CREATE TABLE langtable (nr NUMBER, text VARCHAR2(300), langy langdata2)
   */
  public static TableMetadata createLangTableMetadata() {
    TableMetadata t = new TableMetadata("USER_ROBERT", "LANGTABLE");

    // nr NUMBER column
    t.addColumn(new ColumnMetadata("NR", "NUMBER", 22, 0, 0, true, null));

    // text VARCHAR2(300) column
    t.addColumn(new ColumnMetadata("TEXT", "VARCHAR2", 300, 0, 0, true, null));

    // langy langdata2 column (object type)
    t.addColumn(new ColumnMetadata("LANGY", "LANGDATA2", 0, 0, 0, true, null));

    return t;
  }

  /**
   * Creates a mock ObjectType for langdata2:
   * CREATE TYPE LANGDATA2 AS OBJECT (de VARCHAR2(4000), en VARCHAR2(4000))
   */
  public static ObjectType createLangdata2ObjectType() {
    List<Variable> variables = new ArrayList<>();

    // de VARCHAR2(4000) field
    DataTypeSpec deType = new DataTypeSpec("VARCHAR2", "4000", null, null);
    variables.add(new Variable("de", deType, null));

    // en VARCHAR2(4000) field
    DataTypeSpec enType = new DataTypeSpec("VARCHAR2", "4000", null, null);
    variables.add(new Variable("en", enType, null));

    return new ObjectType(
            "LANGDATA2",
            "USER_ROBERT",
            variables,
            new ArrayList<>(), // functions
            new ArrayList<>(), // procedures  
            new ArrayList<>(), // constructors
            null, // varray
            null  // nestedTable
    );
  }

  /**
   * Creates a mock Everything context with the langdata2 object type
   */
  public static Everything createEverythingWithLangdata2() {
    Everything everything = new Everything();

    // Add the langdata2 object type to the context
    ObjectType langdata2 = createLangdata2ObjectType();
    everything.getObjectTypeSpecAst().add(langdata2);

    return everything;
  }

  /**
   * Creates a mock ResultSet with sample langtable data
   */
  public static ResultSet createMockResultSetWithObjectData() throws SQLException {
    ResultSet rs = mock(ResultSet.class);

    // Simulate a single row of data
    when(rs.next()).thenReturn(true, false); // One row, then end

    // nr = 1
    when(rs.getObject("NR")).thenReturn(1);
    when(rs.getString("NR")).thenReturn("1");

    // text = "Test entry"
    when(rs.getObject("TEXT")).thenReturn("Test entry");
    when(rs.getString("TEXT")).thenReturn("Test entry");

    // langy = langdata2('Hallo', 'Hello') - simulated as a structured object
    // In real Oracle, this would be a Struct object, but for testing we'll mock it
    when(rs.getObject("LANGY")).thenReturn(createMockOracleStruct());
    when(rs.getString("LANGY")).thenReturn("LANGDATA2('Hallo', 'Hello')");

    return rs;
  }

  /**
   * Creates a mock ResultSet with NULL object data
   */
  public static ResultSet createMockResultSetWithNullObject() throws SQLException {
    ResultSet rs = mock(ResultSet.class);

    when(rs.next()).thenReturn(true, false);

    // nr = 2
    when(rs.getObject("NR")).thenReturn(2);
    when(rs.getString("NR")).thenReturn("2");

    // text = "Null test"
    when(rs.getObject("TEXT")).thenReturn("Null test");
    when(rs.getString("TEXT")).thenReturn("Null test");

    // langy = NULL
    when(rs.getObject("LANGY")).thenReturn(null);
    when(rs.getString("LANGY")).thenReturn(null);

    return rs;
  }

  /**
   * Simulates an Oracle Struct object for langdata2
   * In a real scenario, this would be a java.sql.Struct with attributes
   */
  public static Object createMockOracleStruct() {
    // For testing purposes, we'll return a simple object that represents
    // the structure. In real implementation, this would be a java.sql.Struct
    return new MockOracleStruct("LANGDATA2", new Object[]{"Hallo", "Hello"});
  }

  /**
   * Simple mock class to represent Oracle Struct objects in tests
   */
  public static class MockOracleStruct {
    private final String typeName;
    private final Object[] attributes;

    public MockOracleStruct(String typeName, Object[] attributes) {
      this.typeName = typeName;
      this.attributes = attributes;
    }

    public String getTypeName() {
      return typeName;
    }

    public Object[] getAttributes() {
      return attributes;
    }

    @Override
    public String toString() {
      return typeName + "(" + String.join(", ",
              java.util.Arrays.stream(attributes)
                      .map(o -> o == null ? "NULL" : "'" + o.toString() + "'")
                      .toArray(String[]::new)) + ")";
    }
  }

  /**
   * Creates expected JSON output for langdata2 object
   */
  public static String getExpectedLangdata2Json() {
    return "{\"de\":\"Hallo\",\"en\":\"Hello\"}";
  }

  /**
   * Creates expected composite type tuple for langdata2 object
   */
  public static String getExpectedLangdata2CompositeType() {
    return "(\"Hallo\",\"Hello\")";
  }

  /**
   * Creates expected PostgreSQL INSERT statement for langtable with object data
   */
  public static String getExpectedPostgreSQLInsert() {
    return "INSERT INTO USER_ROBERT.langtable (nr, text, langy) VALUES " +
            "(1, 'Test entry', (\"Hallo\",\"Hello\"))";
  }

  /**
   * Creates a simple table metadata without object types for comparison
   */
  public static TableMetadata createSimpleTableMetadata() {
    TableMetadata t = new TableMetadata("USER_ROBERT", "LANGTABLE");
    t.addColumn(new ColumnMetadata("ID", "NUMBER", 22, 0, 0, true, null));
    t.addColumn(new ColumnMetadata("NAME", "VARCHAR2", 100, 0, 0, true, null));
    return t;
  }

  /**
   * Creates an ObjectType with numeric fields for testing
   */
  public static ObjectType createObjectTypeWithNumericField() {
    List<Variable> variables = new ArrayList<>();

    // Create variables for numeric type
    DataTypeSpec intType = new DataTypeSpec("INTEGER", null, null, null);
    DataTypeSpec decimalType = new DataTypeSpec("NUMBER", null, null, null);

    variables.add(new Variable("intField", intType, null));
    variables.add(new Variable("decimalField", decimalType, null));

    return new ObjectType("NUMERICTYPE", "USER_ROBERT", variables, null, null, null, null, null);
  }
}