package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.StatementDeclarationCollector;
import me.christianrobert.ora2postgre.plsql.ast.tools.ToExportPostgre;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.ProcedureTransformationManager;

import java.util.List;

public class Procedure extends PlSqlAst {
  private String name;
  private List<Parameter> parameters;
  private List<Statement> statements;

  private ObjectType parentType;
  private OraclePackage parentPackage;
  
  private static final ProcedureTransformationManager transformationManager = new ProcedureTransformationManager();

  public Procedure(
          String name,
          List<Parameter> parameters,
          List<Statement> statements) {
    this.name = name;
    this.parameters = parameters;
    this.statements = statements;
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
    return "Procedure{name=" + name + ", parameters=" + parameters + ", body=?}";
  }

  // toJava() method removed - business logic now stays in PostgreSQL
  // REST endpoints will call PostgreSQL procedures directly
  
  /**
   * Gets the PostgreSQL procedure name that this procedure will become.
   */
  public String getPostgreProcedureName() {
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
   * @deprecated Use ProcedureTransformationManager instead for better maintainability and extensibility.
   * This method is maintained for backward compatibility and delegates to the transformation manager.
   */
  @Deprecated
  public String toPostgre(Everything data, boolean specOnly) {
    return transformationManager.transform(this, data, specOnly);
  }

  private boolean isWeb() {
    return true; //TODO this. has some htp inside? or check for synonyms?
  }
}