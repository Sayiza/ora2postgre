package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.Comment;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.Variable;

import java.util.ArrayList;
import java.util.List;

public class VisitSeqOfDeclareSpecs {
  public static PlSqlAst visit(PlSqlParser.Seq_of_declare_specsContext ctx,
                               PlSqlAstBuilder astBuilder) {
    // This method is called when seq_of_declare_specs is visited directly
    // Usually we want to extract variables using the helper method above
    List<Variable> variables = extractVariablesFromDeclareSpecs(ctx, astBuilder);
    return new Comment("declare_specs with " + variables.size() + " variables");
  }

  /**
   * Extracts variables from seq_of_declare_specs context.
   * Returns a list of Variable objects found in the DECLARE section.
   */
  public static List<Variable> extractVariablesFromDeclareSpecs(
          PlSqlParser.Seq_of_declare_specsContext ctx,
          PlSqlAstBuilder astBuilder) {
    List<Variable> variables = new ArrayList<>();

    if (ctx != null && ctx.declare_spec() != null) {
      for (PlSqlParser.Declare_specContext declareSpec : ctx.declare_spec()) {
        if (declareSpec.variable_declaration() != null) {
          Variable variable = (Variable) astBuilder.visit(declareSpec.variable_declaration());
          if (variable != null) {
            variables.add(variable);
          }
        }
        // Note: Other declare_spec types (procedure_spec, function_spec, cursor_declaration, etc.)
        // are handled elsewhere in the parsing process
      }
    }

    return variables;
  }
}
