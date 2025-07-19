package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.FetchStatement;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;

import java.util.ArrayList;
import java.util.List;

public class VisitFetchStatement {
  public static PlSqlAst visit(
          PlSqlParser.Fetch_statementContext ctx,
          PlSqlAstBuilder astBuilder) {
    String cursorName = ctx.cursor_name().getText();
    
    // Parse INTO variables
    List<String> intoVariables = new ArrayList<>();
    if (ctx.variable_or_collection() != null) {
      for (PlSqlParser.Variable_or_collectionContext varCtx : ctx.variable_or_collection()) {
        // Extract variable name from variable_or_collection context
        String varName = varCtx.getText(); // Simple approach - could be enhanced for complex expressions
        intoVariables.add(varName);
      }
    }
    
    return new FetchStatement(cursorName, intoVariables);
  }
}