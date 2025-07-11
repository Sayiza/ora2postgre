package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.helpers.StatementDeclarationCollector;
import me.christianrobert.ora2postgre.plsql.ast.tools.helpers.ToExportPostgre;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.FunctionTransformationManager;

import java.util.List;

public class Function extends PlSqlAst {
  private String name;
  private List<Parameter> parameters;
  private List<Variable> variables; // Variable declarations from DECLARE section
  private String returnType;
  private List<Statement> statements;
  private ExceptionBlock exceptionBlock; // Exception handling

  private ObjectType parentType;
  private OraclePackage parentPackage;
  private boolean isStandalone = false;
  private String schema; // For standalone functions
  
  private static final FunctionTransformationManager transformationManager = new FunctionTransformationManager();

  public Function(String name,
                  List<Parameter> parameters,
                  List<Variable> variables,
                  String returnType,
                  List<Statement> statements) {
    this.name = name;
    this.parameters = parameters;
    this.variables = variables;
    this.returnType = returnType;
    this.statements = statements;
    this.exceptionBlock = null; // No exception handling by default
  }

  // Constructor with exception handling
  public Function(String name,
                  List<Parameter> parameters,
                  List<Variable> variables,
                  String returnType,
                  List<Statement> statements,
                  ExceptionBlock exceptionBlock) {
    this.name = name;
    this.parameters = parameters;
    this.variables = variables;
    this.returnType = returnType;
    this.statements = statements;
    this.exceptionBlock = exceptionBlock;
  }

  public void setParentPackage(OraclePackage parentPackage) {
    this.parentPackage = parentPackage;
  }

  public void setParentType(ObjectType parentType) {
    this.parentType = parentType;
  }

  public String getName() {
    return name;
  }

  public String getReturnType() {
    return returnType;
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

  public boolean hasExceptionHandling() {
    return exceptionBlock != null && exceptionBlock.hasHandlers();
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

  public List<Variable> getVariables() {
    return variables;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Function{name=" + name + ", parameters=" + parameters + ", body=?}";
  }

  // toJava() method removed - business logic now stays in PostgreSQL
  // REST endpoints will call PostgreSQL functions directly
  
  /**
   * Gets the PostgreSQL function name that this function will become.
   */
  public String getPostgreFunctionName() {
    if (isStandalone) {
      return schema.toUpperCase() + "." + name.toLowerCase();
    }
    String schema = parentType != null ? parentType.getSchema().toUpperCase() :
                   parentPackage.getSchema().toUpperCase();
    String objectName = parentType != null ? parentType.getName().toUpperCase() :
                       parentPackage.getName().toUpperCase();
    return schema + "." + objectName + "_" + name.toLowerCase();
  }


  /**
   * @deprecated Use FunctionTransformationManager instead for better maintainability and extensibility.
   * This method is maintained for backward compatibility and delegates to the transformation manager.
   */
  @Deprecated
  public String toPostgre(Everything data, boolean specOnly) {
    return transformationManager.transform(this, data, specOnly);
  }
  
}