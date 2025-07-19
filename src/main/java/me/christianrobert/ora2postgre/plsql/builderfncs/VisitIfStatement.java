package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.IfStatement;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.Statement;

import java.util.ArrayList;
import java.util.List;

public class VisitIfStatement {
  public static PlSqlAst visit(
          PlSqlParser.If_statementContext ctx,
          PlSqlAstBuilder astBuilder) {
    // Parse the main IF condition
    Expression condition = (Expression) astBuilder.visit(ctx.condition());
    
    // Parse THEN statements
    List<Statement> thenStatements = new ArrayList<>();
    if (ctx.seq_of_statements() != null && ctx.seq_of_statements().statement() != null) {
      for (PlSqlParser.StatementContext stmt : ctx.seq_of_statements().statement()) {
        Statement statement = (Statement) astBuilder.visit(stmt);
        if (statement != null) {
          thenStatements.add(statement);
        }
      }
    }
    
    // Parse ELSIF parts
    List<IfStatement.ElsifPart> elsifParts = null;
    if (ctx.elsif_part() != null && !ctx.elsif_part().isEmpty()) {
      elsifParts = new ArrayList<>();
      for (PlSqlParser.Elsif_partContext elsif : ctx.elsif_part()) {
        Expression elsifCondition = (Expression) astBuilder.visit(elsif.condition());
        List<Statement> elsifStatements = new ArrayList<>();
        if (elsif.seq_of_statements() != null && elsif.seq_of_statements().statement() != null) {
          for (PlSqlParser.StatementContext stmt : elsif.seq_of_statements().statement()) {
            Statement statement = (Statement) astBuilder.visit(stmt);
            if (statement != null) {
              elsifStatements.add(statement);
            }
          }
        }
        elsifParts.add(new IfStatement.ElsifPart(elsifCondition, elsifStatements));
      }
    }
    
    // Parse ELSE statements
    List<Statement> elseStatements = null;
    if (ctx.else_part() != null && ctx.else_part().seq_of_statements() != null && 
        ctx.else_part().seq_of_statements().statement() != null) {
      elseStatements = new ArrayList<>();
      for (PlSqlParser.StatementContext stmt : ctx.else_part().seq_of_statements().statement()) {
        Statement statement = (Statement) astBuilder.visit(stmt);
        if (statement != null) {
          elseStatements.add(statement);
        }
      }
    }
    
    return new IfStatement(condition, thenStatements, elsifParts, elseStatements);
  }
}