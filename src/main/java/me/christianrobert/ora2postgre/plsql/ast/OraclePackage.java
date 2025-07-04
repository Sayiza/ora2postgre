package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.ToExportJava;

import java.util.ArrayList;
import java.util.List;

public class OraclePackage extends PlSqlAst {
  private String name;
  private String schema;
  private List<Variable> variables;
  private List<SubType> subtypes;
  private List<Cursor> cursors;
  private List<PackageType> types;
  private List<Function> functions;
  private List<Procedure> procedures;
  private List<Statement> bodyStatements;

  public OraclePackage(String name,
                       String schema,
                       List<Variable> variables,
                       List<SubType> subtypes,
                       List<Cursor> cursors,
                       List<PackageType> types,
                       List<Function> functions,
                       List<Procedure> procedures,
                       List<Statement> bodyStatements) {

    this.name = name;
    this.schema = schema;
    this.variables = variables != null ? variables : new ArrayList<>();
    this.functions = functions != null ? functions : new ArrayList<>();
    this.procedures = procedures != null ? procedures : new ArrayList<>();
    this.subtypes = subtypes != null ? subtypes : new ArrayList<>();
    this.cursors = cursors != null ? cursors : new ArrayList<>();
    this.types = types != null ? types : new ArrayList<>();
    this.bodyStatements = bodyStatements != null ? bodyStatements : new ArrayList<>();
  }

  public String getName() { return name; }

  public List<Procedure> getProcedures() { return procedures; }

  public String getSchema() {
    return schema;
  }

  public List<Variable> getVariables() {
    return variables;
  }

  public List<Function> getFunctions() {
    return functions;
  }

  public List<SubType> getSubtypes() {
    return subtypes;
  }

  public List<Cursor> getCursors() {
    return cursors;
  }

  public List<PackageType> getTypes() {
    return types;
  }

  public List<Statement> getBodyStatements() {
    return bodyStatements;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Package{name=" + name +
            ", vars " + variables.size() +
            ", procedures=" + procedures.size() +
            ", functions=" + functions.size() +
            ", cursors=" + cursors.size() +
            "}";
  }

  // toJava() method removed - packages become PostgreSQL schema objects
  // REST endpoints will call PostgreSQL functions directly

  public String toPostgre(Everything data, boolean specOnly) {
    StringBuilder b = new StringBuilder();
    for (Variable variable : variables) {
      // todo
    }
    for (SubType subtype : subtypes) {
      // todo
    }
    for (Cursor cursor : cursors) {
      // todo
    }
    for (PackageType type : types) {
      // todo
    }
    for (Function function : functions) {
      b.append(function.toPostgre(data, specOnly));
      b.append("\n\n");
    }
    for (Procedure procedure : procedures) {
      b.append(procedure.toPostgre(data, specOnly));
      b.append("\n\n");
    }
    // todo body statements need to be passed to functions and procedures!
    return b.toString();
  }
}