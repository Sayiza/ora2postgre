package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.SelectSubQueryBasicElement;
import me.christianrobert.ora2postgre.plsql.ast.TableReference;
import me.christianrobert.ora2postgre.plsql.ast.TableReferenceAuxInternal;

import java.util.ArrayList;
import java.util.List;

public class VisitTableRefAuxInternalTwo {
  public static PlSqlAst visit(
          PlSqlParser.Table_ref_aux_internal_twoContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    List<SelectSubQueryBasicElement> unionList = new ArrayList<>();
    List<SelectSubQueryBasicElement> unionAllList = new ArrayList<>();
    List<SelectSubQueryBasicElement> minusList = new ArrayList<>();
    List<SelectSubQueryBasicElement> intersectList = new ArrayList<>();
    
    for (PlSqlParser.Subquery_operation_partContext s1 : ctx.subquery_operation_part()) {
      if (s1.UNION() != null) {
        unionList.add((SelectSubQueryBasicElement) astBuilder.visit(s1.subquery_basic_elements()));
      }
      if (s1.ALL() != null) {
        unionAllList.add((SelectSubQueryBasicElement) astBuilder.visit(s1.subquery_basic_elements()));
      }
      if (s1.MINUS() != null) {
        minusList.add((SelectSubQueryBasicElement) astBuilder.visit(s1.subquery_basic_elements()));
      }
      if (s1.INTERSECT() != null) {
        intersectList.add((SelectSubQueryBasicElement) astBuilder.visit(s1.subquery_basic_elements()));
      }
    }

    return new TableReferenceAuxInternal(
            astBuilder.schema,
            (TableReference) astBuilder.visit(ctx.table_ref()),
            unionList,
            unionAllList,
            minusList,
            intersectList
    );
  }
}