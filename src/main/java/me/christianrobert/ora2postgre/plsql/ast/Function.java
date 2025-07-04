package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.StringAux;
import me.christianrobert.ora2postgre.plsql.ast.tools.StatementDeclarationCollector;
import me.christianrobert.ora2postgre.plsql.ast.tools.ToExportJava;
import me.christianrobert.ora2postgre.plsql.ast.tools.ToExportPostgre;
import me.christianrobert.ora2postgre.plsql.ast.tools.TypeConverter;

import java.util.List;

public class Function extends PlSqlAst {
  private String name;
  private List<Parameter> parameters;
  private String returnType;
  private List<Statement> statements;

  private ObjectType parentType;
  private OraclePackage parentPackage;

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


  public String toPostgre(Everything data, boolean specOnly) {
    StringBuilder b = new StringBuilder();
    b.append("CREATE OR REPLACE FUNCTION ")
            .append(parentType != null ? parentType.getSchema().toUpperCase() :
                    parentPackage.getSchema().toUpperCase() )
            .append(".")
            .append(parentType != null ? parentType.getName().toUpperCase() :
                    parentPackage.getName().toUpperCase() ) //TODO
            .append("_")
            .append(name.toLowerCase())
            .append("(");
    ToExportPostgre.doParametersPostgre(b, parameters, data);
    b.append(") \n")
            .append("RETURNS ")
            .append(TypeConverter.toPostgre(returnType))
            .append("\nLANGUAGE plpgsql AS $$\n")
            .append("DECLARE\n");
    // Collect and add variable declarations from FOR loops
    StringBuilder declarations = StatementDeclarationCollector.collectNecessaryDeclarations(statements, data);
    b.append(declarations);
    b.append("BEGIN\n");
    if (specOnly) {
      b.append("return null;\n");
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
  
}