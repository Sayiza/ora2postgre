package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.BulkCollectStatement;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.SelectFetchClause;
import me.christianrobert.ora2postgre.plsql.ast.SelectForUpdateClause;
import me.christianrobert.ora2postgre.plsql.ast.SelectIntoStatement;
import me.christianrobert.ora2postgre.plsql.ast.SelectOffsetClause;
import me.christianrobert.ora2postgre.plsql.ast.SelectOrderByClause;
import me.christianrobert.ora2postgre.plsql.ast.SelectStatement;
import me.christianrobert.ora2postgre.plsql.ast.SelectSubQuery;
import me.christianrobert.ora2postgre.plsql.ast.SelectWithClause;

import java.util.ArrayList;
import java.util.List;

public class VisitSelectStatement {
  public static PlSqlAst visit(
          PlSqlParser.Select_statementContext ctx,
          PlSqlAstBuilder astBuilder) {
    // Check if this is a SELECT INTO statement by looking for into_clause in query_block
    if (ctx.select_only_statement() != null && 
        ctx.select_only_statement().subquery() != null &&
        ctx.select_only_statement().subquery().subquery_basic_elements() != null &&
        ctx.select_only_statement().subquery().subquery_basic_elements().query_block() != null &&
        ctx.select_only_statement().subquery().subquery_basic_elements().query_block().into_clause() != null) {
      
      // This is a SELECT INTO statement - route to SelectIntoStatement
      // Extract WITH clause if present
      SelectWithClause withClause = ctx.select_only_statement().with_clause() != null ?
              (SelectWithClause) astBuilder.visit(ctx.select_only_statement().with_clause()) : null;
      
      return visitSelectIntoFromQueryBlock(ctx.select_only_statement().subquery().subquery_basic_elements().query_block(), withClause, astBuilder);
    }
    
    // Regular SELECT statement - use existing logic
    return new SelectStatement(
            astBuilder.getSchema(), // TODO only selectOnlyClause for now, to for,order etc later
            (SelectSubQuery) astBuilder.visit(ctx.select_only_statement().subquery()),
            ctx.select_only_statement().with_clause() != null ?
                    (SelectWithClause) astBuilder.visit(ctx.select_only_statement().with_clause()) :
                    null,
            ctx.for_update_clause() != null && !ctx.for_update_clause().isEmpty() ?
                    (SelectForUpdateClause) astBuilder.visit(ctx.for_update_clause(0)) :
                    null,
            ctx.order_by_clause() != null && !ctx.order_by_clause().isEmpty() ?
                    (SelectOrderByClause) astBuilder.visit(ctx.order_by_clause(0)) :
                    null,
            ctx.offset_clause() != null && !ctx.offset_clause().isEmpty() ?
                    (SelectOffsetClause) astBuilder.visit(ctx.offset_clause(0)) :
                    null,
            ctx.fetch_clause() != null && !ctx.offset_clause().isEmpty() ?
                    (SelectFetchClause) astBuilder.visit(ctx.offset_clause(0)) :
                    null
    );
  }

  /**
   * Helper method to parse SELECT INTO statements from query_block context
   */
  private static PlSqlAst visitSelectIntoFromQueryBlock(PlSqlParser.Query_blockContext ctx, SelectWithClause withClause, PlSqlAstBuilder astBuilder) {
    // Parse selected columns
    List<String> selectedColumns = new ArrayList<>();
    if (ctx.selected_list().ASTERISK() != null) {
      selectedColumns.add("*");
    } else {
      for (var selectElement : ctx.selected_list().select_list_elements()) {
        // For simplicity, just get the text of each select element
        // TODO: This could be enhanced to handle complex expressions
        selectedColumns.add(selectElement.getText());
      }
    }
    
    // Check if this is BULK COLLECT INTO
    boolean isBulkCollect = false;
    if (ctx.into_clause() != null) {
      // Check for BULK COLLECT keywords in the into_clause
      String intoClauseText = ctx.into_clause().getText().toUpperCase();
      isBulkCollect = intoClauseText.contains("BULKCOLLECT");
    }
    
    // Parse INTO variables
    List<String> intoVariables = new ArrayList<>();
    if (ctx.into_clause() != null) {
      for (var element : ctx.into_clause().general_element()) {
        intoVariables.add(element.getText());
      }
      // Also handle bind_variable if present
      if (ctx.into_clause().bind_variable() != null) {
        for (var bindVar : ctx.into_clause().bind_variable()) {
          intoVariables.add(bindVar.getText());
        }
      }
    }
    
    // Parse FROM table (simplified approach)
    String schemaName = null;
    String tableName = null;
    
    if (ctx.from_clause() != null &&
        ctx.from_clause().table_ref_list() != null &&
        ctx.from_clause().table_ref_list().table_ref() != null &&
        !ctx.from_clause().table_ref_list().table_ref().isEmpty()) {
      
      // For simplicity, just get the text of the first table reference
      // TODO: This could be enhanced to properly parse complex table expressions
      var firstTableRef = ctx.from_clause().table_ref_list().table_ref().get(0);
      String tableRefText = firstTableRef.getText();
      
      // Simple parsing: check if it contains a dot (schema.table)
      if (tableRefText.contains(".")) {
        String[] parts = tableRefText.split("\\.", 2);
        schemaName = parts[0];
        tableName = parts[1];
      } else {
        tableName = tableRefText;
      }
    }
    
    // Parse WHERE clause if present
    Expression whereClause = null;
    if (ctx.where_clause() != null && ctx.where_clause().condition() != null) {
      whereClause = (Expression) astBuilder.visit(ctx.where_clause().condition());
    }
    
    // Return appropriate statement type based on BULK COLLECT detection
    if (isBulkCollect) {
      return new BulkCollectStatement(selectedColumns, intoVariables, schemaName, tableName, whereClause);
    } else {
      return new SelectIntoStatement(selectedColumns, intoVariables, schemaName, tableName, whereClause, withClause);
    }
  }
}