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
    List<Variable> variables = DeclarationParsingUtils.extractVariablesFromDeclareSpecs(ctx, astBuilder);
    return new Comment("declare_specs with " + variables.size() + " variables");
  }
}
