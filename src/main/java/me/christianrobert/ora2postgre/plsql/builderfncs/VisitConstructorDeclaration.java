package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.Constructor;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.Statement;

import java.util.ArrayList;
import java.util.List;

public class VisitConstructorDeclaration {
  public static PlSqlAst visit(
          PlSqlParser.Constructor_declarationContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    String name = null;

    List<Parameter> parameters = new ArrayList<>();

    if (ctx.type_spec() != null) {
      for (var constpart : ctx.type_spec()) {
        if (constpart instanceof PlSqlParser.Type_specContext) {
          name = constpart.getText();
        }
      }
    }

    for (PlSqlParser.Type_elements_parameterContext e : ctx.type_elements_parameter()) {
      parameters.add((Parameter) astBuilder.visit(e));
    }
    List<Statement> statements = new ArrayList<>();
    if (ctx.body() != null
            && ctx.body().seq_of_statements() != null
            && ctx.body().seq_of_statements().statement() != null) {
      for (PlSqlParser.StatementContext stmt : ctx.body().seq_of_statements().statement()) {
        statements.add((Statement) astBuilder.visit(stmt));
      }
    }

    if (name != null)
      return new Constructor(name, parameters, statements);
    return null;
  }
}