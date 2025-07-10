package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.StatementDeclarationCollector;
import me.christianrobert.ora2postgre.plsql.ast.tools.ToExportPostgre;
import me.christianrobert.ora2postgre.plsql.ast.tools.TypeConverter;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.FunctionTransformationManager;

import java.util.List;

public class Function extends PlSqlAst {
  private String name;
  private List<Parameter> parameters;
  private String returnType;
  private List<Statement> statements;

  private ObjectType parentType;
  private OraclePackage parentPackage;
  
  private static final FunctionTransformationManager transformationManager = new FunctionTransformationManager();

  public Function(String name, List<Parameter> parameters, String returnType, List<Statement> statements) {
    this.name = name;
    this.parameters = parameters;
    this.returnType = returnType;
    this.statements = statements;
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

  public ObjectType getParentType() {
    return parentType;
  }

  public OraclePackage getParentPackage() {
    return parentPackage;
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