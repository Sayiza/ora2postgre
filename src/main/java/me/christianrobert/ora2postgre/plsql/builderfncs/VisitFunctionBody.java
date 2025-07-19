package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.CursorDeclaration;
import me.christianrobert.ora2postgre.plsql.ast.ExceptionBlock;
import me.christianrobert.ora2postgre.plsql.ast.Function;
import me.christianrobert.ora2postgre.plsql.ast.NestedTableType;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.RecordType;
import me.christianrobert.ora2postgre.plsql.ast.Statement;
import me.christianrobert.ora2postgre.plsql.ast.Variable;
import me.christianrobert.ora2postgre.plsql.ast.VarrayType;

import java.util.ArrayList;
import java.util.List;

public class VisitFunctionBody {
  public static PlSqlAst visit(
          PlSqlParser.Function_bodyContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    List<Parameter> parameters = new ArrayList<>();
    String procedureName = ctx.identifier().getText();
    String returnType = ctx.type_spec().getText(); //TODO?

    for (PlSqlParser.ParameterContext e : ctx.parameter()) {
      parameters.add((Parameter) astBuilder.visit(e));
    }

    List<Statement> statements = new ArrayList<>();
    if (ctx.body() != null
            && ctx.body().seq_of_statements() != null
            && ctx.body().seq_of_statements().statement() != null) {
      for (PlSqlParser.StatementContext stmt : ctx.body().seq_of_statements().statement()) {
        statements.add((Statement) astBuilder.visit(stmt));
      }
    }

    // Extract declarations from DECLARE section using DeclarationParsingUtils
    DeclarationParsingUtils.DeclarationBundle declarations = 
        DeclarationParsingUtils.extractDeclarations(ctx.seq_of_declare_specs(), astBuilder);

    List<Variable> variables = declarations.getVariables();
    List<CursorDeclaration> cursorDeclarations = declarations.getCursorDeclarations();
    List<RecordType> recordTypes = declarations.getRecordTypes();
    List<VarrayType> varrayTypes = declarations.getVarrayTypes();
    List<NestedTableType> nestedTableTypes = declarations.getNestedTableTypes();

    // Parse exception block if present
    ExceptionBlock exceptionBlock = null;
    if (ctx.body() != null && ctx.body().exception_handler() != null && !ctx.body().exception_handler().isEmpty()) {
      exceptionBlock = DeclarationParsingUtils.parseExceptionBlock(ctx.body().exception_handler(), astBuilder);
    }

    return new Function(procedureName, parameters, variables, cursorDeclarations, recordTypes, varrayTypes, nestedTableTypes, returnType, statements, exceptionBlock);
  }
}