package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.ExceptionBlock;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.Procedure;
import me.christianrobert.ora2postgre.plsql.ast.Statement;

import java.util.List;

public class VisitProcDeclInType {
  public static PlSqlAst visit(
          PlSqlParser.Proc_decl_in_typeContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    // Extract procedure name
    String procedureName = ctx.procedure_name().getText();

    // Extract parameters using shared utility
    List<Parameter> parameters = DeclarationParsingUtils.extractParameters(
            ctx.type_elements_parameter(), astBuilder);

    // Extract declarations using shared utility
    DeclarationParsingUtils.DeclarationBundle declarations = 
            DeclarationParsingUtils.extractDeclarations(ctx.seq_of_declare_specs(), astBuilder);

    // Extract statements using shared utility
    List<Statement> statements = DeclarationParsingUtils.extractStatements(ctx.body(), astBuilder);

    // Parse exception handling if present (procedure-specific functionality)
    ExceptionBlock exceptionBlock = null;
    if (ctx.body() != null && ctx.body().exception_handler() != null && !ctx.body().exception_handler().isEmpty()) {
      exceptionBlock = astBuilder.parseExceptionBlock(ctx.body().exception_handler());
    }

    return new Procedure(
            procedureName,
            parameters,
            declarations.getVariables(),
            declarations.getCursorDeclarations(),
            declarations.getRecordTypes(),
            statements,
            exceptionBlock
    );
  }
}