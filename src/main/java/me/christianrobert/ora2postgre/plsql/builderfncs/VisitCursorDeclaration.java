package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.CursorDeclaration;
import me.christianrobert.ora2postgre.plsql.ast.DataTypeSpec;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.SelectStatement;

import java.util.ArrayList;
import java.util.List;

public class VisitCursorDeclaration {
  public static PlSqlAst visit(PlSqlParser.Cursor_declarationContext ctx,
                               PlSqlAstBuilder astBuilder
  ) {
    String cursorName = ctx.identifier().getText();

    // Parse parameters if present
    List<Parameter> parameters = new ArrayList<>();
    if (ctx.parameter_spec() != null) {
      for (PlSqlParser.Parameter_specContext paramCtx : ctx.parameter_spec()) {
        // Extract parameter name and type
        String paramName = paramCtx.parameter_name().getText();
        String paramType = paramCtx.type_spec() != null ? paramCtx.type_spec().getText() : "VARCHAR2";

        // Create parameter (assuming IN direction for cursor parameters)
        Parameter param = new Parameter(paramName, new DataTypeSpec(paramType, null, null, null), null, true, false);
        parameters.add(param);
      }
    }

    // Parse return type if present
    String returnType = null;
    if (ctx.type_spec() != null) {
      returnType = ctx.type_spec().getText();
    }

    // Parse SELECT statement if present
    SelectStatement selectStatement = null;
    if (ctx.select_statement() != null) {
      PlSqlAst selectAst = astBuilder.visit(ctx.select_statement());
      if (selectAst instanceof SelectStatement) {
        selectStatement = (SelectStatement) selectAst;
      }
    }

    return new CursorDeclaration(cursorName, parameters, returnType, selectStatement);
  }
}
