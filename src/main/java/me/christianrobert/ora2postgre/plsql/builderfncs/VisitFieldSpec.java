package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.DataTypeSpec;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.RecordType;

public class VisitFieldSpec {
  public static PlSqlAst visit(
          PlSqlParser.Field_specContext ctx,
          PlSqlAstBuilder astBuilder) {
    String fieldName = ctx.column_name().getText();
    
    // Parse data type
    DataTypeSpec dataType = null;
    if (ctx.type_spec() != null) {
      dataType = (DataTypeSpec) astBuilder.visit(ctx.type_spec());
    } else {
      // Default to VARCHAR if no type specified
      dataType = new DataTypeSpec("VARCHAR2", null, null, null);
    }
    
    // Parse NULL/NOT NULL constraint
    boolean notNull = false;
    if (ctx.NULL_() != null && ctx.NOT() != null) {
      notNull = true;
    }
    
    // Parse default value
    Expression defaultValue = null;
    if (ctx.default_value_part() != null) {
      defaultValue = (Expression) astBuilder.visit(ctx.default_value_part());
    }
    
    return new RecordType.RecordField(fieldName, dataType, notNull, defaultValue);
  }
}