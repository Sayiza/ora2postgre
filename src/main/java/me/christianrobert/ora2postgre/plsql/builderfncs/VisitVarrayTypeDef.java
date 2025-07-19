package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.*;

public class VisitVarrayTypeDef {
  public static PlSqlAst visit(
          PlSqlParser.Varray_type_defContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    Long sizeNumeric = null;
    Expression expression = null;
    if (ctx.expression() != null
            && ctx.expression().getText() != null
            && ctx.expression().getText().matches("-?\\d+")) {
      sizeNumeric = Long.parseLong(ctx.expression().getText());
    } else {
      expression = (Expression) astBuilder.visit(ctx.expression());
    }

    return new VarrayType(sizeNumeric, expression, (DataTypeSpec) astBuilder.visit(ctx.type_spec()));
  }
}