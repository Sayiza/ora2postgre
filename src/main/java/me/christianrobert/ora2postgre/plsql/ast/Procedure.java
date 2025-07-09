package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.StatementDeclarationCollector;
import me.christianrobert.ora2postgre.plsql.ast.tools.ToExportPostgre;

import java.util.List;

public class Procedure extends PlSqlAst {
  private String name;
  private List<Parameter> parameters;
  private List<Statement> statements;

  private ObjectType parentType;
  private OraclePackage parentPackage;

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

  public String toPostgre(Everything data, boolean specOnly) {
    StringBuilder b = new StringBuilder();
    b.append("CREATE OR REPLACE PROCEDURE ")
            .append(parentType != null ? parentType.getSchema().toUpperCase() :
                    parentPackage.getSchema().toUpperCase() )
            .append(".")
            .append(parentType != null ? parentType.getName().toUpperCase() :
                    parentPackage.getName().toUpperCase() ) //TODO
            .append("_")
            .append(name.toLowerCase())
            .append("(");
    ToExportPostgre.doParametersPostgre(b, parameters, data);
    b.append(") LANGUAGE plpgsql AS $$\n")
            .append("DECLARE\n");
    // Collect and add variable declarations from FOR loops
    StringBuilder declarations = StatementDeclarationCollector.collectNecessaryDeclarations(statements, data);
    b.append(declarations);
    b.append("BEGIN\n");
    if (specOnly) {
      b.append("null;\n");
    } else {
      // loop over statements
      for (Statement statement : statements) {
        b.append(statement.toPostgre(data))
                .append("\n");
      }
    }
    b.append("END;\n$$\n;\n");
    return b.toString();
  }

  private boolean isWeb() {
    return true; //TODO this. has some htp inside? or check for synonyms?
  }
}