package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.ProcedureTransformationManager;

import java.util.ArrayList;
import java.util.List;

public class Procedure extends PlSqlAst {
  private String name;
  private List<Parameter> parameters;
  private List<Variable> variables; // Variable declarations from DECLARE section
  private List<CursorDeclaration> cursorDeclarations; // Cursor declarations from DECLARE section
  private List<RecordType> recordTypes; // Record type declarations from DECLARE section
  private List<Statement> statements;
  private ExceptionBlock exceptionBlock; // Exception handling

  private ObjectType parentType;
  private OraclePackage parentPackage;
  private boolean isStandalone = false;
  private String schema; // For standalone procedures
  
  private static final ProcedureTransformationManager transformationManager = new ProcedureTransformationManager();

  public Procedure(
          String name,
          List<Parameter> parameters,
          List<Variable> variables,
          List<Statement> statements) {
    this.name = name;
    this.parameters = parameters;
    this.variables = variables != null ? variables : new ArrayList<>();
    this.cursorDeclarations = new ArrayList<>(); // Initialize empty list
    this.recordTypes = new ArrayList<>(); // Initialize empty list
    this.statements = statements;
    this.exceptionBlock = null; // No exception handling by default
  }

  // Constructor with exception handling
  public Procedure(
          String name,
          List<Parameter> parameters,
          List<Variable> variables,
          List<Statement> statements,
          ExceptionBlock exceptionBlock) {
    this.name = name;
    this.parameters = parameters;
    this.variables = variables != null ? variables : new ArrayList<>();
    this.cursorDeclarations = new ArrayList<>(); // Initialize empty list
    this.recordTypes = new ArrayList<>(); // Initialize empty list
    this.statements = statements;
    this.exceptionBlock = exceptionBlock;
  }

  // Constructor with record types and cursor declarations
  public Procedure(
          String name,
          List<Parameter> parameters,
          List<Variable> variables,
          List<CursorDeclaration> cursorDeclarations,
          List<RecordType> recordTypes,
          List<Statement> statements,
          ExceptionBlock exceptionBlock) {
    this.name = name;
    this.parameters = parameters;
    this.variables = variables != null ? variables : new ArrayList<>();
    this.cursorDeclarations = cursorDeclarations != null ? cursorDeclarations : new ArrayList<>();
    this.recordTypes = recordTypes != null ? recordTypes : new ArrayList<>();
    this.statements = statements;
    this.exceptionBlock = exceptionBlock;
  }

  public void setParentType(ObjectType parentType) {
    this.parentType = parentType;
  }

  public void setParentPackage(OraclePackage parentPackage) {
    this.parentPackage = parentPackage;
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

  public boolean hasExceptionHandling() {
    return exceptionBlock != null && exceptionBlock.hasHandlers();
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

  public ObjectType getParentType() {
    return parentType;
  }

  public OraclePackage getParentPackage() {
    return parentPackage;
  }

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

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Procedure{name=" + name + ", parameters=" + parameters + ", body=?}";
  }

  // toJava() method removed - business logic now stays in PostgreSQL
  // REST endpoints will call PostgreSQL procedures directly
  
  /**
   * Gets the PostgreSQL procedure name that this procedure will become.
   */
  public String getPostgreProcedureName() {
    if (isStandalone) {
      return schema.toUpperCase() + "." + name.toLowerCase();
    }
    String schema = parentType != null ? parentType.getSchema().toUpperCase() :
                   parentPackage.getSchema().toUpperCase();
    String objectName = parentType != null ? parentType.getName().toUpperCase() :
                       parentPackage.getName().toUpperCase();
    return schema + "." + objectName + "_" + name.toLowerCase();
  }
  
  public String getName() {
    return name;
  }
  
  public List<Parameter> getParameters() {
    return parameters;
  }

  /**
   * Transforms this procedure to PostgreSQL DDL using the transformation manager.
   * This method serves dual purposes in the established dual usage pattern:
   * - As main object: delegates to transformation manager for complex orchestration
   * - As sub-element: enables direct toPostgre() calls in AST chains
   */
  public String toPostgre(Everything data, boolean specOnly) {
    return transformationManager.transform(this, data, specOnly);
  }

  private boolean isWeb() {
    return true; //TODO this. has some htp inside? or check for synonyms?
  }
}