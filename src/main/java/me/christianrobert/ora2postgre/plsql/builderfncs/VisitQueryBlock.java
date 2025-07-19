package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.SelectListElement;
import me.christianrobert.ora2postgre.plsql.ast.SelectQueryBlock;
import me.christianrobert.ora2postgre.plsql.ast.TableReference;
import me.christianrobert.ora2postgre.plsql.ast.WhereClause;

import java.util.ArrayList;
import java.util.List;

public class VisitQueryBlock {
  public static PlSqlAst visit(
          PlSqlParser.Query_blockContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    List<SelectListElement> selectedFields = new ArrayList<>();
    List<TableReference> fromTables = new ArrayList<>();
    WhereClause whereClause = null;

    if (ctx.selected_list().ASTERISK() != null) {
      // TODO get all fields from the tables
    } else {
      for (PlSqlParser.Select_list_elementsContext se : ctx.selected_list().select_list_elements()) {
        selectedFields.add((SelectListElement) astBuilder.visit(se));
      }
    }

    if (ctx.from_clause() != null &&
            ctx.from_clause().table_ref_list() != null &&
            ctx.from_clause().table_ref_list().table_ref() != null
    ) {
      for (PlSqlParser.Table_refContext tr : ctx.from_clause().table_ref_list().table_ref()) {
        fromTables.add((TableReference) astBuilder.visit(tr));
      }
    } // TODO fix this

    // Handle WHERE clause if present
    if (ctx.where_clause() != null) {
      whereClause = (WhereClause) astBuilder.visit(ctx.where_clause());
    }

    return new SelectQueryBlock(astBuilder.schema, selectedFields, fromTables, whereClause);
  }
}