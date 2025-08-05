package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.SchemaResolutionUtils;
import me.christianrobert.ora2postgre.oracledb.ColumnMetadata;
import me.christianrobert.ora2postgre.oracledb.TableMetadata;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Extended DataTypeSpec that handles record types and %ROWTYPE attributes.
 * 
 * This class extends the existing DataTypeSpec to support:
 * - Oracle RECORD types
 * - %ROWTYPE attributes (table%ROWTYPE)
 * - %TYPE attributes (table.column%TYPE)
 * - Custom user-defined types
 */
public class RecordTypeSpec extends DataTypeSpec {
  
  private final RecordType recordType;        // For custom record types
  private final String rowtypeTableName;     // For table%ROWTYPE
  private final String rowtypeSchemaName;    // Schema for %ROWTYPE resolution
  private final String typeTableName;       // For table.column%TYPE
  private final String typeColumnName;      // Column for %TYPE
  private final String typeSchemaName;      // Schema for %TYPE resolution
  
  /**
   * Constructor for custom record types
   */
  public RecordTypeSpec(RecordType recordType) {
    super(null, recordType.getName(), null, null);
    this.recordType = recordType;
    this.rowtypeTableName = null;
    this.rowtypeSchemaName = null;
    this.typeTableName = null;
    this.typeColumnName = null;
    this.typeSchemaName = null;
  }
  
  /**
   * Constructor for %ROWTYPE attributes
   */
  public static RecordTypeSpec forRowType(String schemaName, String tableName) {
    RecordTypeSpec spec = new RecordTypeSpec(null, null, schemaName, tableName, null, null, null);
    return spec;
  }
  
  /**
   * Constructor for %TYPE attributes  
   */
  public static RecordTypeSpec forColumnType(String schemaName, String tableName, String columnName) {
    RecordTypeSpec spec = new RecordTypeSpec(null, null, schemaName, null, tableName, columnName, schemaName);
    return spec;
  }
  
  /**
   * Private constructor for attribute-based types
   */
  private RecordTypeSpec(RecordType recordType, String customTypeName, String rowtypeSchemaName, 
                        String rowtypeTableName, String typeTableName, String typeColumnName, String typeSchemaName) {
    super(null, customTypeName, 
          rowtypeTableName != null ? rowtypeSchemaName + "." + rowtypeTableName + "%ROWTYPE" : null,
          typeTableName != null ? typeSchemaName + "." + typeTableName + "." + typeColumnName + "%TYPE" : null);
    this.recordType = recordType;
    this.rowtypeSchemaName = rowtypeSchemaName;
    this.rowtypeTableName = rowtypeTableName;
    this.typeTableName = typeTableName;
    this.typeColumnName = typeColumnName;
    this.typeSchemaName = typeSchemaName;
  }

  public RecordType getRecordType() {
    return recordType;
  }

  public String getRowtypeTableName() {
    return rowtypeTableName;
  }

  public String getRowtypeSchemaName() {
    return rowtypeSchemaName;
  }

  public boolean isRowType() {
    return rowtypeTableName != null;
  }
  
  public boolean isColumnType() {
    return typeTableName != null && typeColumnName != null;
  }
  
  public boolean isRecordType() {
    return recordType != null;
  }

  @Override
  public String toPostgre(Everything data) {
    // Handle %TYPE attributes (column types)
    if (isColumnType()) {
      return resolveColumnType(data);
    }
    
    // Handle %ROWTYPE attributes
    if (isRowType()) {
      return resolveRowType(data);
    }
    
    // Handle custom record types
    if (isRecordType()) {
      return recordType.getName().toLowerCase();
    }
    
    // Fall back to parent implementation
    return super.toPostgre(data);
  }
  
  /**
   * Resolve %TYPE to actual PostgreSQL column type
   */
  private String resolveColumnType(Everything data) {
    try {
      // Use existing schema resolution infrastructure
      String resolvedSchema = SchemaResolutionUtils.lookupSchema4Field(data, typeTableName, typeSchemaName);
      String dataType = findColumnDataTypePublic(data, typeColumnName, resolvedSchema, typeTableName);
      
      if (dataType != null) {
        return TypeConverter.toPostgre(dataType);
      }
    } catch (Exception e) {
      // Log warning but continue
      System.err.println("Warning: Could not resolve %TYPE for " + typeSchemaName + "." + typeTableName + "." + typeColumnName + ": " + e.getMessage());
    }
    
    return "/* " + typeSchemaName + "." + typeTableName + "." + typeColumnName + "%TYPE - could not resolve */";
  }
  
  /**
   * Helper method to find column data type using public APIs
   */
  private String findColumnDataTypePublic(Everything data, String columnName, String schema, String tableName) {
    // Check tables
    for (var table : data.getTableSql()) {
      if (table.getSchema().equalsIgnoreCase(schema) && table.getTableName().equalsIgnoreCase(tableName)) {
        for (var column : table.getColumns()) {
          if (column.getColumnName().equalsIgnoreCase(columnName)) {
            return column.getDataType();
          }
        }
      }
    }
    
    // Check views  
    for (var view : data.getViewDefinition()) {
      if (view.getSchema().equalsIgnoreCase(schema) && view.getViewName().equalsIgnoreCase(tableName)) {
        for (var column : view.getColumns()) {
          if (column.getColumnName().equalsIgnoreCase(columnName)) {
            return column.getDataType();
          }
        }
      }
    }
    
    return null;
  }
  
  /**
   * Resolve %ROWTYPE to PostgreSQL composite type or record structure
   */
  private String resolveRowType(Everything data) {
    try {
      // Find the table metadata
      TableMetadata table = findTableMetadata(data);
      if (table != null) {
        // Generate a composite type name based on the table
        String compositeTypeName = (rowtypeSchemaName + "_" + rowtypeTableName + "_rowtype").toLowerCase();
        
        // The actual composite type creation should be handled elsewhere (in package/function generation)
        // For now, just return the type name
        return compositeTypeName;
      }
    } catch (Exception e) {
      System.err.println("Warning: Could not resolve %ROWTYPE for " + rowtypeSchemaName + "." + rowtypeTableName + ": " + e.getMessage());
    }
    
    return "/* " + rowtypeSchemaName + "." + rowtypeTableName + "%ROWTYPE - could not resolve */";
  }
  
  /**
   * Find table metadata for %ROWTYPE resolution
   */
  private TableMetadata findTableMetadata(Everything data) {
    String resolvedSchema = SchemaResolutionUtils.lookupSchema4Field(data, rowtypeTableName, rowtypeSchemaName);
    
    for (TableMetadata table : data.getTableSql()) {
      if (table.getTableName().equalsIgnoreCase(rowtypeTableName) && 
          table.getSchema().equalsIgnoreCase(resolvedSchema)) {
        return table;
      }
    }
    
    return null;
  }
  
  /**
   * Generate PostgreSQL composite type definition for %ROWTYPE
   * This should be called during the export phase to create the necessary types
   */
  public String generateCompositeTypeDefinition(Everything data) {
    if (!isRowType()) {
      return null;
    }
    
    TableMetadata table = findTableMetadata(data);
    if (table == null) {
      return "-- Could not find table: " + rowtypeSchemaName + "." + rowtypeTableName;
    }
    
    StringBuilder b = new StringBuilder();
    String compositeTypeName = (rowtypeSchemaName + "_" + rowtypeTableName + "_rowtype").toLowerCase();
    
    b.append("-- Composite type for ").append(rowtypeSchemaName).append(".").append(rowtypeTableName).append("%ROWTYPE\n");
    b.append("CREATE TYPE ").append(compositeTypeName).append(" AS (\n");
    
    List<ColumnMetadata> columns = table.getColumns();
    for (int i = 0; i < columns.size(); i++) {
      ColumnMetadata col = columns.get(i);
      b.append("  ").append(col.getColumnName().toLowerCase())
       .append(" ").append(TypeConverter.toPostgre(col.getDataType()));
      
      if (i < columns.size() - 1) {
        b.append(",");
      }
      b.append("\n");
    }
    
    b.append(");");
    
    return b.toString();
  }
}