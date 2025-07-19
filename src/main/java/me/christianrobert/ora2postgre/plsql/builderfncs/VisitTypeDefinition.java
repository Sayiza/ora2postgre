package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.oracledb.tools.NameNormalizer;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.NestedTableType;
import me.christianrobert.ora2postgre.plsql.ast.ObjectType;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.Variable;
import me.christianrobert.ora2postgre.plsql.ast.VarrayType;

import java.util.ArrayList;
import java.util.List;

public class VisitTypeDefinition {
  public static PlSqlAst visit(
          PlSqlParser.Type_definitionContext ctx,
          PlSqlAstBuilder astBuilder) {
    String typeName = NameNormalizer.normalizeObjectTypeName(ctx.type_name().getText());
    // object types
    List<Variable> variables = new ArrayList<>();
    if (ctx.object_type_def() != null && ctx.object_type_def().object_member_spec() != null) {
      for (var member : ctx.object_type_def().object_member_spec()) {
        PlSqlAst memberAst = astBuilder.visit(member);
        if (memberAst instanceof Variable) {
          variables.add((Variable) memberAst);
        }
      }
    }

    // varrays
    if (ctx.object_type_def() != null
            && ctx.object_type_def().object_as_part() != null
            && ctx.object_type_def().object_as_part().varray_type_def() != null
    ) {
      VarrayType varrayType = (VarrayType) astBuilder.visit(ctx.object_type_def().object_as_part().varray_type_def());
      return new ObjectType(typeName, astBuilder.getSchema(), null, null, null, null,
              varrayType, null);
    }

    if (ctx.object_type_def() != null
            && ctx.object_type_def().object_as_part() != null
            && ctx.object_type_def().object_as_part().nested_table_type_def() != null) {
      NestedTableType nestedTableType = (NestedTableType) astBuilder.visit(ctx.object_type_def().object_as_part().nested_table_type_def());
      return new ObjectType(typeName, astBuilder.getSchema(), null, null, null, null, null, nestedTableType);
    }

    // TODO do something about the under clause?!
    return new ObjectType(typeName, astBuilder.getSchema(), variables, null, null, null, null, null);
  }
}