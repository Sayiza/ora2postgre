package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.DataTypeSpec;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;

public class VisitTypeElementsParameter {
  public static PlSqlAst visit(
          PlSqlParser.Type_elements_parameterContext ctx,
          PlSqlAstBuilder astBuilder) {
    return new Parameter(
      ctx.parameter_name().getText(),
      (DataTypeSpec) astBuilder.visit(ctx.type_spec()),
      ctx.default_value_part() != null ? (Expression) astBuilder.visit(ctx.default_value_part()) : null,
      ctx.IN() != null && ctx.IN().getText() != null,
      ctx.OUT() != null  && ctx.OUT().getText() != null
    );
  }
}