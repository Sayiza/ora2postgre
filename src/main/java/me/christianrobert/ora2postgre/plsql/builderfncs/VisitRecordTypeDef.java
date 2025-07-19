package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.RecordType;

import java.util.ArrayList;
import java.util.List;

public class VisitRecordTypeDef {
  public static PlSqlAst visit(
          PlSqlParser.Record_type_defContext ctx,
          PlSqlAstBuilder astBuilder) {
    List<RecordType.RecordField> fields = new ArrayList<>();
    
    if (ctx.field_spec() != null) {
      for (PlSqlParser.Field_specContext fieldCtx : ctx.field_spec()) {
        RecordType.RecordField field = (RecordType.RecordField) astBuilder.visit(fieldCtx);
        if (field != null) {
          fields.add(field);
        }
      }
    }
    
    return new RecordType("", fields); // Name will be set by type_declaration
  }
}