package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.CommonTableExpression;
import me.christianrobert.ora2postgre.plsql.ast.Function;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.Procedure;
import me.christianrobert.ora2postgre.plsql.ast.SelectWithClause;

import java.util.ArrayList;
import java.util.List;

public class VisitWithClause {
  public static PlSqlAst visit(
          PlSqlParser.With_clauseContext ctx,
          PlSqlAstBuilder astBuilder) {
    List<CommonTableExpression> cteList = new ArrayList<>();
    List<Function> functions = new ArrayList<>();
    List<Procedure> procedures = new ArrayList<>();
    
    // Process function and procedure bodies (Oracle-specific feature)
    if (ctx.function_body() != null) {
      for (PlSqlParser.Function_bodyContext funcCtx : ctx.function_body()) {
        Function function = (Function) astBuilder.visit(funcCtx);
        if (function != null) {
          functions.add(function);
        }
      }
    }
    
    if (ctx.procedure_body() != null) {
      for (PlSqlParser.Procedure_bodyContext procCtx : ctx.procedure_body()) {
        Procedure procedure = (Procedure) astBuilder.visit(procCtx);
        if (procedure != null) {
          procedures.add(procedure);
        }
      }
    }
    
    // Process WITH factoring clauses (CTE definitions)
    if (ctx.with_factoring_clause() != null) {
      for (PlSqlParser.With_factoring_clauseContext factoringCtx : ctx.with_factoring_clause()) {
        CommonTableExpression cte = (CommonTableExpression) astBuilder.visit(factoringCtx);
        if (cte != null) {
          cteList.add(cte);
        }
      }
    }
    
    return new SelectWithClause(cteList, functions, procedures);
  }
}