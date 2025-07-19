package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.Comment;
import me.christianrobert.ora2postgre.plsql.ast.DataTypeSpec;
import me.christianrobert.ora2postgre.plsql.ast.NestedTableType;
import me.christianrobert.ora2postgre.plsql.ast.PackageType;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.RecordType;
import me.christianrobert.ora2postgre.plsql.ast.VarrayType;

public class VisitTypeDeclaration {
  public static PlSqlAst visit(
          PlSqlParser.Type_declarationContext ctx,
          PlSqlAstBuilder astBuilder) {
    if (ctx.identifier() != null && ctx.record_type_def() != null) {
      String typeName = ctx.identifier().getText();
      RecordType recordType = (RecordType) astBuilder.visit(ctx.record_type_def());
      // Set the name from the declaration
      return new RecordType(typeName, recordType.getFields());
    }
    
    // Handle VARRAY type declarations: TYPE name IS VARRAY(size) OF type_spec
    if (ctx.identifier() != null && ctx.varray_type_def() != null) {
      String typeName = ctx.identifier().getText();
      VarrayType varrayType = (VarrayType) astBuilder.visit(ctx.varray_type_def());
      // Return a named VarrayType (similar to RecordType pattern)
      return new VarrayType(typeName, varrayType.getSize(), varrayType.getSizeExpression(), varrayType.getDataType());
    }
    
    // Handle TABLE OF type declarations: TYPE name IS TABLE OF type_spec
    if (ctx.identifier() != null && ctx.table_type_def() != null) {
      String typeName = ctx.identifier().getText();
      NestedTableType nestedTableType = (NestedTableType) astBuilder.visit(ctx.table_type_def());
      // Return a named NestedTableType (similar to RecordType pattern)
      return new NestedTableType(typeName, nestedTableType.getDataType());
    }
    
    // Handle simple type alias declarations: TYPE name IS type_spec (e.g., TYPE user_id IS NUMBER(10))
    if (ctx.identifier() != null && ctx.type_spec() != null) {
      String typeName = ctx.identifier().getText();
      DataTypeSpec dataTypeSpec = (DataTypeSpec) astBuilder.visit(ctx.type_spec());
      // Return a PackageType for simple type aliases
      return new PackageType(typeName, dataTypeSpec);
    }
    
    // TODO: Handle ref_cursor_type_def
    return new Comment("type_declaration not fully implemented");
  }
}