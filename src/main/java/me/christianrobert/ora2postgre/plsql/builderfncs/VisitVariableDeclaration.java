package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.DataTypeSpec;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.Variable;

public class VisitVariableDeclaration {
  public static PlSqlAst visit(PlSqlParser.Variable_declarationContext ctx,
                               PlSqlAstBuilder astBuilder
  ) {
    String varName = ctx.identifier().id_expression().getText();
    DataTypeSpec dataType = (DataTypeSpec) astBuilder.visit(ctx.type_spec());
    Expression defaultValue = ctx.default_value_part() != null ? (Expression) astBuilder.visit(ctx.default_value_part()) : null;
    return new Variable(varName, dataType, defaultValue);
  }
}
