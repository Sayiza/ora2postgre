package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.Comment;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;

public class VisitStatement {
  public static PlSqlAst visit(
          PlSqlParser.StatementContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    if (ctx.getChildCount() == 1) {
      PlSqlAst visitedStatement = astBuilder.visit(ctx.getChild(0));
      if (visitedStatement != null) {
        return visitedStatement;
      }
      return new Comment("type of statement not found"+ astBuilder.getClass());
    }
    return new Comment("unclear statement structure: " + astBuilder.getClass()); //TODO
  }
}