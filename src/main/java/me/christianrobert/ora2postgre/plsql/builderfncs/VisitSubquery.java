package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.SelectSubQuery;
import me.christianrobert.ora2postgre.plsql.ast.SelectSubQueryBasicElement;

import java.util.ArrayList;
import java.util.List;

public class VisitSubquery {
  public static PlSqlAst visit(
          PlSqlParser.SubqueryContext ctx,
          PlSqlAstBuilder astBuilder) {
    SelectSubQueryBasicElement subQueryBasicElement =
            (SelectSubQueryBasicElement) astBuilder.visit(ctx.subquery_basic_elements());
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

    return new SelectSubQuery(
            astBuilder.getSchema(),
            subQueryBasicElement,
            unionList,
            unionAllList,
            minusList,
            intersectList
    );
  }
}