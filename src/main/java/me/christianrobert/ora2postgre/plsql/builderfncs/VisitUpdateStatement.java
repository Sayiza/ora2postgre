package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.UpdateStatement;

import java.util.ArrayList;
import java.util.List;

public class VisitUpdateStatement {
  public static PlSqlAst visit(
          PlSqlParser.Update_statementContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    // Parse table name from general_table_ref
    String schemaName = null;
    String tableName = null;
    
    if (ctx.general_table_ref() != null && 
        ctx.general_table_ref().dml_table_expression_clause() != null &&
        ctx.general_table_ref().dml_table_expression_clause().tableview_name() != null) {
      
      var tableview = ctx.general_table_ref().dml_table_expression_clause().tableview_name();
      if (tableview.identifier() != null) {
        if (tableview.id_expression() != null) {
          // Schema.Table format
          schemaName = tableview.identifier().getText();
          tableName = tableview.id_expression().getText();
        } else {
          // Just table name
          tableName = tableview.identifier().getText();
        }
      }
    }
    
    // Parse SET clauses from update_set_clause
    List<UpdateStatement.UpdateSetClause> setColumns = new ArrayList<>();
    if (ctx.update_set_clause() != null) {
      // Handle column_based_update_set_clause list
      if (ctx.update_set_clause().column_based_update_set_clause() != null) {
        for (var setClause : ctx.update_set_clause().column_based_update_set_clause()) {
          if (setClause.column_name() != null && setClause.expression() != null) {
            String columnName = setClause.column_name().getText();
            Expression value = (Expression) astBuilder.visit(setClause.expression());
            if (value != null) {
              setColumns.add(new UpdateStatement.UpdateSetClause(columnName, value));
            }
          }
        }
      }
    }
    
    // Parse WHERE clause if present
    Expression whereClause = null;
    if (ctx.where_clause() != null && ctx.where_clause().condition() != null) {
      whereClause = (Expression) astBuilder.visit(ctx.where_clause().condition());
    }
    
    return new UpdateStatement(schemaName, tableName, setColumns, whereClause);
  }
}