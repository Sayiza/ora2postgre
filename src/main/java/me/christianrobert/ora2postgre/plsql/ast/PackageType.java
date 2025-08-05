package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter;

public class PackageType extends PlSqlAst {
  private String name;
  private DataTypeSpec dataType; //TODO

  public PackageType(String name, DataTypeSpec dataType) {
    this.name = name;
    this.dataType = dataType;
  }

  public String getName() {
    return name;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "PackageType{name=" + name + ", dataType=" + dataType + "}";
  }

  public String toJava(Everything data) {
    StringBuilder b = new StringBuilder();
    b.append(dataType.toJava(data))
            .append(" ")
            .append(name)
            .append(";") //TODO
    ;
    return b.toString();
  }

  /**
   * Generates PostgreSQL DOMAIN DDL for package type alias.
   * Oracle: TYPE user_id_type IS NUMBER(10);
   * PostgreSQL: CREATE DOMAIN schema_package_user_id_type AS NUMERIC(10);
   */
  public String toPostgre(Everything data) {
    // PackageType should not be called directly for DDL generation.
    // Instead, it should be processed by StandardPackageStrategy which generates the DOMAIN DDL.
    // This method is used for type resolution when the type is referenced.
    return dataType.toPostgre(data);
  }

  /**
   * Generates PostgreSQL DOMAIN DDL statement for package-level type alias.
   * This method is called by StandardPackageStrategy during package processing.
   */
  public String toDomainDDL(String schema, String packageName) {
    StringBuilder b = new StringBuilder();
    b.append("CREATE DOMAIN ");
    
    // Generate domain name: schema_package_typename (consistent with collection types)
    String domainName = schema.toLowerCase() + "_" + packageName.toLowerCase() + "_" + name.toLowerCase();
    b.append(domainName);
    
    b.append(" AS ");
    // Build parameterized type string from DataTypeSpec fields
    String postgresType = buildParameterizedType();
    b.append(postgresType);
    b.append(";");
    
    return b.toString();
  }
  
  /**
   * Builds a parameterized PostgreSQL type from the DataTypeSpec fields.
   * This preserves precision and scale for numeric types and length for character types.
   */
  private String buildParameterizedType() {
    if (dataType.getNativeDataType() == null) {
      return "text"; // fallback
    }
    
    String nativeType = dataType.getNativeDataType().toLowerCase().trim();
    
    // Handle NUMBER types with precision and scale
    if (nativeType.startsWith("number")) {
      String baseType = "numeric";
      
      // Check for precision and scale parameters
      String precision = dataType.getRowTypeFullName(); // reusing field for precision
      String scale = dataType.getFieldTypeFullName(); // reusing field for scale
      
      if (precision != null && scale != null) {
        return baseType + "(" + precision + "," + scale + ")";
      } else if (precision != null) {
        return baseType + "(" + precision + ")";
      } else {
        return baseType;
      }
    }
    
    // Handle VARCHAR2 types with length
    if (nativeType.startsWith("varchar2")) {
      String baseType = "varchar";
      String length = dataType.getCustumDataType(); // reusing field for length
      
      if (length != null) {
        return baseType + "(" + length + ")";
      } else {
        return "text"; // fallback to text for unspecified length
      }
    }
    
    // Handle CHAR types with length
    if (nativeType.startsWith("char")) {
      String baseType = "char";
      String length = dataType.getCustumDataType(); // reusing field for length
      
      if (length != null) {
        return baseType + "(" + length + ")";
      } else {
        return "char";
      }
    }
    
    // For other types, use the standard type converter
    return TypeConverter.toPostgre(nativeType);
  }

  /**
   * Simple Oracle to PostgreSQL type conversion for domain creation.
   * This is a simplified version - could be enhanced to use full DataTypeSpec.toPostgre() if needed.
   */
  private String convertOracleTypeToPostgreSQL(String oracleType) {
    if (oracleType == null) return "text";
    
    String lowerType = oracleType.toLowerCase().trim();
    
    // Handle parameterized types
    if (lowerType.startsWith("number")) return "numeric" + extractParameters(lowerType);
    if (lowerType.startsWith("varchar2")) return "varchar" + extractParameters(lowerType);
    if (lowerType.startsWith("varchar")) return "varchar" + extractParameters(lowerType);
    if (lowerType.startsWith("char")) return "char" + extractParameters(lowerType);
    
    // Handle exact matches
    switch (lowerType) {
      case "integer": case "int": return "integer";
      case "date": return "timestamp";
      case "timestamp": return "timestamp";
      case "clob": return "text";
      case "blob": return "bytea";
      case "raw": return "bytea";
      default: return "text"; // Safe fallback
    }
  }

  /**
   * Extracts parameters from Oracle type specification.
   * Examples: NUMBER(10,2) -> (10,2), VARCHAR2(100) -> (100)
   */
  private String extractParameters(String oracleType) {
    int startParen = oracleType.indexOf('(');
    int endParen = oracleType.lastIndexOf(')');
    
    if (startParen != -1 && endParen != -1 && endParen > startParen) {
      return oracleType.substring(startParen, endParen + 1);
    }
    
    return ""; // No parameters
  }

  public DataTypeSpec getDataType() {
    return dataType;
  }
}