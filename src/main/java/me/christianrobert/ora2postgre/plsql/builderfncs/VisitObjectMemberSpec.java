package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.DataTypeSpec;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.Variable;

public class VisitObjectMemberSpec {
  public static PlSqlAst visit(
          PlSqlParser.Object_member_specContext ctx,
          PlSqlAstBuilder astBuilder) {
    if (ctx.identifier() != null &&
            ctx.identifier().id_expression() != null &&
            ctx.type_spec() != null) {
      String name = ctx.identifier().id_expression().getText();
      DataTypeSpec dataType = (DataTypeSpec) astBuilder.visit(ctx.type_spec());
      return new Variable(name, dataType, null);
    }
    if (ctx.element_spec() != null) {
      return astBuilder.visit(ctx.element_spec());
    }
    // element_spec not implemented
    return null;
  }
}