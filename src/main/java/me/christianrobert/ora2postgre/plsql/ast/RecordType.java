package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

/**
 * AST class for Oracle RECORD type definitions.
 * 
 * Oracle pattern:
 * TYPE record_name IS RECORD (
 *   field1 VARCHAR2(100),
 *   field2 NUMBER,
 *   field3 DATE DEFAULT SYSDATE
 * );
 * 
 * PostgreSQL transformation:
 * CREATE TYPE record_name AS (
 *   field1 TEXT,
 *   field2 NUMERIC,
 *   field3 TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 * );
 */
public class RecordType extends PlSqlAst {
  private final String name;
  private final List<RecordField> fields;

  public RecordType(String name, List<RecordField> fields) {
    this.name = name;
    this.fields = fields;
  }

  public String getName() {
    return name;
  }

  public List<RecordField> getFields() {
    return fields;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "RecordType{name=" + name + ", fields=" + (fields != null ? fields.size() : 0) + "}";
  }

  /**
   * Generate PostgreSQL composite type definition
   */
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    
    b.append("-- Record type: ").append(name).append("\n");
    b.append("CREATE TYPE ").append(name.toLowerCase()).append(" AS (\n");
    
    if (fields != null && !fields.isEmpty()) {
      for (int i = 0; i < fields.size(); i++) {
        RecordField field = fields.get(i);
        b.append("  ").append(field.toPostgre(data));
        
        if (i < fields.size() - 1) {
          b.append(",");
        }
        b.append("\n");
      }
    }
    
    b.append(");");
    
    return b.toString();
  }

  /**
   * Represents a field within a record type
   */
  public static class RecordField extends PlSqlAst {
    private final String name;
    private final DataTypeSpec dataType;
    private final boolean notNull;
    private final Expression defaultValue;

    public RecordField(String name, DataTypeSpec dataType, boolean notNull, Expression defaultValue) {
      this.name = name;
      this.dataType = dataType;
      this.notNull = notNull;
      this.defaultValue = defaultValue;
    }

    public String getName() {
      return name;
    }

    public DataTypeSpec getDataType() {
      return dataType;
    }

    public boolean isNotNull() {
      return notNull;
    }

    public Expression getDefaultValue() {
      return defaultValue;
    }

    @Override
    public <T> T accept(PlSqlAstVisitor<T> visitor) {
      return visitor.visit(this);
    }

    @Override
    public String toString() {
      return "RecordField{name=" + name + ", dataType=" + dataType + ", notNull=" + notNull + "}";
    }

    /**
     * Generate PostgreSQL field definition for composite type
     */
    public String toPostgre(Everything data) {
      StringBuilder b = new StringBuilder();
      
      b.append(name.toLowerCase()).append(" ").append(dataType.toPostgre(data));
      
      // Note: PostgreSQL composite types don't support NOT NULL constraints or defaults
      // These would need to be enforced at the table/function level where the type is used
      if (notNull) {
        b.append(" /* NOT NULL constraint must be enforced at usage level */");
      }
      
      if (defaultValue != null) {
        b.append(" /* DEFAULT ").append(defaultValue.toPostgre(data))
          .append(" must be handled at usage level */");
      }
      
      return b.toString();
    }
  }
}