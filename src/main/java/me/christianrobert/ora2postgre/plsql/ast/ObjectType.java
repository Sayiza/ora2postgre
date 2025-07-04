package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.StringAux;
import me.christianrobert.ora2postgre.plsql.ast.tools.ToExportJava;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ObjectType extends PlSqlAst {
  private String name;
  private String schema;

  // Object
  private List<Variable> variables;
  private List<Function> functions;
  private List<Procedure> procedures;
  private List<Constructor> constuctors;

  // Collection type
  private VarrayType varray;
  private NestedTableType nestedTable;

  public ObjectType(String name,
                    String schema,
                    List<Variable> variables,
                    List<Function> functions,
                    List<Procedure> procedures,
                    List<Constructor> constructors,
                    VarrayType varray,
                    NestedTableType nestedTable) {
    this.name = name;
    this.schema = schema;
    this.variables = variables != null ? variables : new ArrayList<>();
    this.functions = functions != null ? functions : new ArrayList<>();
    this.procedures = procedures != null ? procedures : new ArrayList<>();
    this.constuctors = constructors != null ? constructors : new ArrayList<>();
    this.varray = varray;
    this.nestedTable = nestedTable;
  }

  public String getName() {
    return name;
  }

  public String getSchema() {
    return schema;
  }

  public List<Variable> getVariables() {
    return variables;
  }

  public List<Function> getFunctions() {
    return functions;
  }

  public List<Procedure> getProcedures() {
    return procedures;
  }

  public List<Constructor> getConstuctors() {
    return constuctors;
  }

  public VarrayType getVarray() {
    return varray;
  }

  public NestedTableType getNestedTable() {
    return nestedTable;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "ObjectType{name=" + name + "}";
  }

  // toJava() method removed - object types now become PostgreSQL composite types
  // REST layer will use simple DTOs when needed

  public String toPostgreType(Everything data) {
    if (varray != null) {
      return new StringBuilder().append("CREATE DOMAIN ")
              .append(name.toLowerCase())
              .append(" AS ")
              .append(varray.toPostgre(data))
              .toString();
    }
    if (nestedTable != null) {
      return new StringBuilder().append("CREATE DOMAIN ")
              .append(name.toLowerCase())
              .append(" AS ")
              .append(nestedTable.toPostgre(data))
              .toString();
    }

    StringBuilder b = new StringBuilder();

    b.append("CREATE TYPE ")
            .append(schema)
            .append(".")
            .append(name.toLowerCase())
            .append(" AS (\n  ");
    if (variables.isEmpty()) {
      b.append(" TODOTMP numeric ");
      // TODO this may happen if there is a under clause, get the content of the super type
    } else {
      b.append(variables.stream()
              .map(e -> e.toPostgre(data))
              .collect(Collectors.joining(",\n  ")));
    }
    b.append("\n)\n;\n\n");
    b.append(toPostgreFunctions(data, true));
    return b.toString();

    // TODO add the function stubs?!
  }

  public String toPostgreFunctions(Everything data, boolean specOnly) {
    StringBuilder b = new StringBuilder();
    b.append(constuctors.stream()
            .map(e -> e.toPostgre(data))
            .collect(Collectors.joining(",\n  ")))
            .append("\n")
            .append(procedures.stream()
                    .map(e -> e.toPostgre(data, specOnly))
                    .collect(Collectors.joining(",\n  ")))
            .append("\n")
            .append(functions.stream()
                    .map(e -> e.toPostgre(data, specOnly))
                    .collect(Collectors.joining(",\n  ")))
            .append("\n")
    ;
    return b.toString();
  }
}