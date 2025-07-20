package me.christianrobert.ora2postgre.plsql.ast.tools.helpers;

import me.christianrobert.ora2postgre.plsql.ast.DataTypeSpec;

/**
 * Information about a collection type definition found in the Everything context.
 * This class holds the metadata needed to properly transform Oracle collection
 * constructors to PostgreSQL array syntax.
 */
public class CollectionTypeInfo {
  private final String typeName;
  private final String collectionKind; // "VARRAY" or "TABLE"  
  private final DataTypeSpec dataType;
  private final String schema;
  private final String packageName;
  
  /**
   * Create collection type information.
   * 
   * @param typeName The name of the collection type (e.g., "t_numbers")
   * @param collectionKind The Oracle collection kind ("VARRAY" or "TABLE")
   * @param dataType The element data type specification
   * @param schema The schema name where the type is defined
   * @param packageName The package name where the type is defined
   */
  public CollectionTypeInfo(String typeName, String collectionKind, DataTypeSpec dataType, 
                           String schema, String packageName) {
    this.typeName = typeName;
    this.collectionKind = collectionKind;
    this.dataType = dataType;
    this.schema = schema;
    this.packageName = packageName;
  }
  
  /**
   * Get the collection type name.
   * @return Type name (e.g., "t_numbers")
   */
  public String getTypeName() {
    return typeName;
  }
  
  /**
   * Get the Oracle collection kind.
   * @return "VARRAY" or "TABLE"
   */
  public String getCollectionKind() {
    return collectionKind;
  }
  
  /**
   * Get the element data type specification.
   * @return DataTypeSpec for the collection elements
   */
  public DataTypeSpec getDataType() {
    return dataType;
  }
  
  /**
   * Get the schema name where this type is defined.
   * @return Schema name
   */
  public String getSchema() {
    return schema;
  }
  
  /**
   * Get the package name where this type is defined.
   * @return Package name
   */
  public String getPackageName() {
    return packageName;
  }
  
  /**
   * Check if this is a VARRAY type.
   * @return true if this is a VARRAY
   */
  public boolean isVarray() {
    return "VARRAY".equalsIgnoreCase(collectionKind);
  }
  
  /**
   * Check if this is a TABLE OF type.
   * @return true if this is a TABLE OF (nested table)
   */
  public boolean isNestedTable() {
    return "TABLE".equalsIgnoreCase(collectionKind);
  }
  
  @Override
  public String toString() {
    return String.format("CollectionTypeInfo{typeName='%s', kind='%s', schema='%s', package='%s'}", 
                        typeName, collectionKind, schema, packageName);
  }
}