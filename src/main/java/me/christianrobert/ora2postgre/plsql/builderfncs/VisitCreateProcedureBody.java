package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.CursorDeclaration;
import me.christianrobert.ora2postgre.plsql.ast.NestedTableType;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.Procedure;
import me.christianrobert.ora2postgre.plsql.ast.RecordType;
import me.christianrobert.ora2postgre.plsql.ast.Statement;
import me.christianrobert.ora2postgre.plsql.ast.Variable;
import me.christianrobert.ora2postgre.plsql.ast.VarrayType;

import java.util.ArrayList;
import java.util.List;

public class VisitCreateProcedureBody {
  public static PlSqlAst visit(
          PlSqlParser.Create_procedure_bodyContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    // Parse standalone procedure
    String procedureName = ctx.procedure_name() != null ? ctx.procedure_name().getText() : "UNKNOWN";
    
    List<Parameter> parameters = new ArrayList<>();
    if (ctx.parameter() != null) {
      for (PlSqlParser.ParameterContext param : ctx.parameter()) {
        parameters.add((Parameter) astBuilder.visit(param));
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
    
    List<Statement> statements = new ArrayList<>();
    if (ctx.body() != null && ctx.body().seq_of_statements() != null && ctx.body().seq_of_statements().statement() != null) {
      for (PlSqlParser.StatementContext stmt : ctx.body().seq_of_statements().statement()) {
        statements.add((Statement) astBuilder.visit(stmt));
      }
    }
    
    Procedure procedure = new Procedure(procedureName, parameters, variables, cursorDeclarations, recordTypes, varrayTypes, nestedTableTypes, statements, null);
    procedure.setStandalone(true);
    procedure.setSchema(astBuilder.schema);
    return procedure;
  }
}