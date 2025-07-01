package com.sayiza.oracle2postgre.plsql.ast;

import com.sayiza.oracle2postgre.global.Everything;
import com.sayiza.oracle2postgre.plsql.ast.tools.TypeConverter;

public class DataTypeSpec extends PlSqlAst {

  private String nativeDataType;
  private String custumDataType;
  private String rowTypeFullName; // w. or w.o. schema + table + %rowtype
  private String fieldTypeFullName; // w. or w.o. schema + table + col + %rowtype

  public DataTypeSpec(String nativeDataType, String custumDataType, String rowTypeFullName, String fieldTypeFullName) {
    this.nativeDataType = nativeDataType;
    this.custumDataType = custumDataType;
    this.rowTypeFullName = rowTypeFullName;
    this.fieldTypeFullName = fieldTypeFullName;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  public String getNativeDataType() {
    return nativeDataType;
  }

  public String getRowTypeFullName() {
    return rowTypeFullName;
  }

  public String getFieldTypeFullName() {
    return fieldTypeFullName;
  }

  public String toJava(Everything data) {
    // TypeConverter.toJava( convert here
    if ( nativeDataType != null ) {
      return TypeConverter.toJava(nativeDataType);
    }
    // TODO look up custom type, or %type or %rowtype
    return " /* data type not implemented " + this.getClass() + " */ ";
  }

  public String toPostgre(Everything data) {
    if ( nativeDataType != null ) {
      return TypeConverter.toPostgre(nativeDataType);
    }
    return " /* data type not implemented  */ ";
  }
}
