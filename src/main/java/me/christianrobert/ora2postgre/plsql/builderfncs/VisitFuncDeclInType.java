package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.Function;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.Statement;

import java.util.List;

public class VisitFuncDeclInType {
  public static PlSqlAst visit(
          PlSqlParser.Func_decl_in_typeContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    // Extract function name and return type
    String functionName = ctx.function_name().getText();
    String returnType = ctx.type_spec().getText();

    // Extract parameters using shared utility
    List<Parameter> parameters = DeclarationParsingUtils.extractParameters(
            ctx.type_elements_parameter(), astBuilder);

    // Extract declarations using shared utility
    DeclarationParsingUtils.DeclarationBundle declarations = 
            DeclarationParsingUtils.extractDeclarations(ctx.seq_of_declare_specs(), astBuilder);

    // Extract statements using shared utility
    List<Statement> statements = DeclarationParsingUtils.extractStatements(ctx.body(), astBuilder);

    return new Function(
            functionName,
            parameters,
            declarations.getVariables(),
            declarations.getCursorDeclarations(),
            declarations.getRecordTypes(),
            declarations.getVarrayTypes(),
            declarations.getNestedTableTypes(),
            returnType,
            statements,
            null
    );
  }
}