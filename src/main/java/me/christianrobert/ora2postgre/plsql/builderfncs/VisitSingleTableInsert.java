package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.Comment;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.InsertStatement;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.SelectStatement;

import java.util.ArrayList;
import java.util.List;

public class VisitSingleTableInsert {
  public static PlSqlAst visit(
          PlSqlParser.Single_table_insertContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    // Parse the insert_into_clause
    String schemaName = null;
    String tableName = null;
    List<String> columnNames = null;
    
    if (ctx.insert_into_clause() != null) {
      // Parse table name from general_table_ref
      if (ctx.insert_into_clause().general_table_ref() != null && 
          ctx.insert_into_clause().general_table_ref().dml_table_expression_clause() != null &&
          ctx.insert_into_clause().general_table_ref().dml_table_expression_clause().tableview_name() != null) {
        
        var tableview = ctx.insert_into_clause().general_table_ref().dml_table_expression_clause().tableview_name();
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
      
      // Parse column list if present
      if (ctx.insert_into_clause().paren_column_list() != null &&
          ctx.insert_into_clause().paren_column_list().column_list() != null &&
          ctx.insert_into_clause().paren_column_list().column_list().column_name() != null) {
        columnNames = new ArrayList<>();
        for (var columnName : ctx.insert_into_clause().paren_column_list().column_list().column_name()) {
          columnNames.add(columnName.getText());
        }
      }
    }
    
    // Parse VALUES clause
    if (ctx.values_clause() != null) {
      List<Expression> values = new ArrayList<>();
      
      // Handle VALUES (expr1, expr2, ...)
      if (ctx.values_clause().expressions_() != null &&
          ctx.values_clause().expressions_().expression() != null) {
        for (var expr : ctx.values_clause().expressions_().expression()) {
          Expression expression = (Expression) astBuilder.visit(expr);
          if (expression != null) {
            values.add(expression);
          }
        }
      }
      
      return new InsertStatement(schemaName, tableName, columnNames, values);
    }
    
    // Parse SELECT statement
    if (ctx.select_statement() != null) {
      SelectStatement selectStatement = (SelectStatement) astBuilder.visit(ctx.select_statement());
      return new InsertStatement(schemaName, tableName, columnNames, selectStatement);
    }
    
    return new Comment("INSERT statement structure not recognized");
  }
}