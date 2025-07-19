package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;

public class VisitSqlScript {
  public static PlSqlAst visit(
          PlSqlParser.Sql_scriptContext ctx,
          PlSqlAstBuilder astBuilder
  ) {
    if (ctx.unit_statement() != null) {
      for (var unit : ctx.unit_statement()) {
        PlSqlAst ast = astBuilder.visit(unit);
        if (ast != null) {
          return ast; // Return first non-null AST
        }
      }
    }
    return null;
  }
}
