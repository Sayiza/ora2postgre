package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter;

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
    return toPostgre(data, null, null);
  }

  public String toPostgre(Everything data, String schemaName, String packageName) {
    if ( nativeDataType != null ) {
      return TypeConverter.toPostgre(nativeDataType);
    }
    
    // Handle custom types (collection types defined in package)
    if ( custumDataType != null && schemaName != null && packageName != null ) {
      // Check if this is a custom collection type defined in the current package
      String domainName = schemaName.toLowerCase() + "_" + packageName.toLowerCase() + "_" + custumDataType.toLowerCase();
      return domainName;
    }
    
    return " /* data type not implemented  */ ";
  }
}
