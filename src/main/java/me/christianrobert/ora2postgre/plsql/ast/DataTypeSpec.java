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

  // this function is currently only called under the assumption that a package level
  // variable is being used, and needs to be augmented to the full name
  // by the convention schema_package_name
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

  public String toPostgre(Everything data, Function function) {
    if ( nativeDataType != null ) {
      return TypeConverter.toPostgre(nativeDataType);
    }
    
    // Handle function-local collection types as type aliases
    if ( custumDataType != null && function != null ) {
      // Look for the custom type in function's local collection types
      
      // Check VARRAY types - resolve to base type + []
      for (VarrayType varrayType : function.getVarrayTypes()) {
        if (varrayType.getName().equalsIgnoreCase(custumDataType)) {
          // Get the base type from the VARRAY definition and add []
          String baseType = varrayType.getDataType().toPostgre(data);
          if (baseType.contains("/* data type not implemented")) {
            return "text[]"; // fallback
          }
          return baseType + "[]";
        }
      }
      
      // Check TABLE OF types - resolve to base type + []
      for (NestedTableType nestedTableType : function.getNestedTableTypes()) {
        if (nestedTableType.getName().equalsIgnoreCase(custumDataType)) {
          // Get the base type from the TABLE OF definition and add []
          String baseType = nestedTableType.getDataType().toPostgre(data);
          if (baseType.contains("/* data type not implemented")) {
            return "text[]"; // fallback
          }
          return baseType + "[]";
        }
      }
      
      // If not found in function-local types, fall back to package-level resolution
      if (function.getParentPackage() != null) {
        return toPostgre(data, function.getParentPackage().getSchema(), function.getParentPackage().getName());
      }
    }
    
    return " /* data type not implemented  */ ";
  }
}
