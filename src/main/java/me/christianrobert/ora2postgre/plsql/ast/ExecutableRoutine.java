package me.christianrobert.ora2postgre.plsql.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for executable routines (Functions and Procedures).
 * This class centralizes all common functionality to eliminate duplication
 * and ensure consistent behavior between Functions and Procedures.
 * 
 * Common features include:
 * - Variable declarations (variables, recordTypes, varrayTypes, nestedTableTypes)
 * - Cursor declarations
 * - Parameters and statements
 * - Exception handling
 * - Parent relationships (package, object type)
 * - Schema management
 */
public abstract class ExecutableRoutine extends PlSqlAst {
  
  // Core routine properties
  protected String name;
  protected List<Parameter> parameters;
  protected List<Statement> statements;
  protected ExceptionBlock exceptionBlock;
  
  // Declaration sections (DECLARE block)
  protected List<Variable> variables;
  protected List<CursorDeclaration> cursorDeclarations;
  protected List<RecordType> recordTypes;
  protected List<VarrayType> varrayTypes;
  protected List<NestedTableType> nestedTableTypes;
  
  // Parent relationships
  protected ObjectType parentType;
  protected OraclePackage parentPackage;
  protected boolean isStandalone = false;
  protected String schema;
  
  // Protected constructor for subclasses
  protected ExecutableRoutine(
          String name,
          List<Parameter> parameters,
          List<Variable> variables,
          List<CursorDeclaration> cursorDeclarations,
          List<RecordType> recordTypes,
          List<VarrayType> varrayTypes,
          List<NestedTableType> nestedTableTypes,
          List<Statement> statements,
          ExceptionBlock exceptionBlock) {
    this.name = name;
    this.parameters = parameters != null ? parameters : new ArrayList<>();
    this.variables = variables != null ? variables : new ArrayList<>();
    this.cursorDeclarations = cursorDeclarations != null ? cursorDeclarations : new ArrayList<>();
    this.recordTypes = recordTypes != null ? recordTypes : new ArrayList<>();
    this.varrayTypes = varrayTypes != null ? varrayTypes : new ArrayList<>();
    this.nestedTableTypes = nestedTableTypes != null ? nestedTableTypes : new ArrayList<>();
    this.statements = statements != null ? statements : new ArrayList<>();
    this.exceptionBlock = exceptionBlock;
  }
  
  // Common getters
  public String getName() {
    return name;
  }
  
  public List<Parameter> getParameters() {
    return parameters;
  }
  
  public List<Statement> getStatements() {
    return statements;
  }
  
  public ExceptionBlock getExceptionBlock() {
    return exceptionBlock;
  }
  
  public void setExceptionBlock(ExceptionBlock exceptionBlock) {
    this.exceptionBlock = exceptionBlock;
  }
  
  public List<Variable> getVariables() {
    return variables;
  }
  
  public List<CursorDeclaration> getCursorDeclarations() {
    return cursorDeclarations;
  }
  
  public void setCursorDeclarations(List<CursorDeclaration> cursorDeclarations) {
    this.cursorDeclarations = cursorDeclarations != null ? cursorDeclarations : new ArrayList<>();
  }
  
  public List<RecordType> getRecordTypes() {
    return recordTypes;
  }
  
  public void setRecordTypes(List<RecordType> recordTypes) {
    this.recordTypes = recordTypes != null ? recordTypes : new ArrayList<>();
  }
  
  public List<VarrayType> getVarrayTypes() {
    return varrayTypes;
  }
  
  public void setVarrayTypes(List<VarrayType> varrayTypes) {
    this.varrayTypes = varrayTypes != null ? varrayTypes : new ArrayList<>();
  }
  
  public List<NestedTableType> getNestedTableTypes() {
    return nestedTableTypes;
  }
  
  public void setNestedTableTypes(List<NestedTableType> nestedTableTypes) {
    this.nestedTableTypes = nestedTableTypes != null ? nestedTableTypes : new ArrayList<>();
  }
  
  // Parent relationship management
  public ObjectType getParentType() {
    return parentType;
  }
  
  public void setParentType(ObjectType parentType) {
    this.parentType = parentType;
  }
  
  public OraclePackage getParentPackage() {
    return parentPackage;
  }
  
  public void setParentPackage(OraclePackage parentPackage) {
    this.parentPackage = parentPackage;
  }
  
  // Schema management
  public boolean isStandalone() {
    return isStandalone;
  }
  
  public void setStandalone(boolean standalone) {
    this.isStandalone = standalone;
  }
  
  public String getSchema() {
    return schema;
  }
  
  public void setSchema(String schema) {
    this.schema = schema;
  }
  
  // Abstract methods that subclasses must implement
  public abstract String toPostgre(me.christianrobert.ora2postgre.global.Everything data, boolean specOnly);
  
  /**
   * Get the PostgreSQL-compatible name for this routine.
   * Functions and Procedures have different naming conventions.
   */
  public abstract String getPostgreQualifiedName();
  
  @Override
  public String toString() {
    return getClass().getSimpleName() + "{name=" + name + 
           ", parameters=" + (parameters != null ? parameters.size() : 0) + 
           ", variables=" + (variables != null ? variables.size() : 0) + 
           ", recordTypes=" + (recordTypes != null ? recordTypes.size() : 0) + 
           ", nestedTableTypes=" + (nestedTableTypes != null ? nestedTableTypes.size() : 0) + "}";
  }
}