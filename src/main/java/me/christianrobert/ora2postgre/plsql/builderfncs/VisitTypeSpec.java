package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.ast.DataTypeSpec;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.RecordTypeSpec;

import java.util.List;

public class VisitTypeSpec {
  public static PlSqlAst visit(PlSqlParser.Type_specContext ctx,
                                 String schema) {
    String nativeDataType = null;
    if (ctx.datatype() != null && ctx.datatype().native_datatype_element() != null ) {
      nativeDataType = ctx.datatype().native_datatype_element().getText();
    }

    StringBuilder b = new StringBuilder();
    if (ctx.type_name() != null && ctx.type_name().id_expression() != null) {
      List<PlSqlParser.Id_expressionContext> idExpression = ctx.type_name().id_expression();
      for (int j = 0; j < idExpression.size(); j++) {
        PlSqlParser.Id_expressionContext i = idExpression.get(j);
        b.append(i.getText());
        if (j < idExpression.size() - 1) {
          b.append(".");
        }
      }
    }

    // Handle %ROWTYPE attributes with enhanced support
    if (ctx.PERCENT_ROWTYPE() != null) {
      String[] parts = b.toString().split("\\.");
      if (parts.length == 2) {
        // schema.table%ROWTYPE
        return RecordTypeSpec.forRowType(parts[0], parts[1]);
      } else if (parts.length == 1) {
        // table%ROWTYPE (use current schema)
        return RecordTypeSpec.forRowType(schema, parts[0]);
      }
    }

    // Handle %TYPE attributes with enhanced support
    if (ctx.PERCENT_TYPE() != null) {
      String[] parts = b.toString().split("\\.");
      if (parts.length == 3) {
        // schema.table.column%TYPE
        return RecordTypeSpec.forColumnType(parts[0], parts[1], parts[2]);
      } else if (parts.length == 2) {
        // table.column%TYPE (use current schema)
        return RecordTypeSpec.forColumnType(schema, parts[0], parts[1]);
      }
    }

    // Fall back to standard DataTypeSpec for native types and custom types
    return new DataTypeSpec(
            nativeDataType,
            ctx.PERCENT_ROWTYPE() == null && ctx.PERCENT_TYPE() == null ? b.toString() : null,
            ctx.PERCENT_ROWTYPE() != null ? b.toString() : null,
            ctx.PERCENT_TYPE() != null ? b.toString() : null
    );
  }
}
