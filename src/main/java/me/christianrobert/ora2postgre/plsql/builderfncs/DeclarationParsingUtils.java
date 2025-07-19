package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.CursorDeclaration;
import me.christianrobert.ora2postgre.plsql.ast.ExceptionBlock;
import me.christianrobert.ora2postgre.plsql.ast.ExceptionHandler;
import me.christianrobert.ora2postgre.plsql.ast.NestedTableType;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.RecordType;
import me.christianrobert.ora2postgre.plsql.ast.Statement;
import me.christianrobert.ora2postgre.plsql.ast.Variable;
import me.christianrobert.ora2postgre.plsql.ast.VarrayType;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for common declaration parsing operations shared between function and procedure declarations.
 */
public class DeclarationParsingUtils {

  /**
   * Extracts parameters from type_elements_parameter contexts.
   */
  public static List<Parameter> extractParameters(
          List<PlSqlParser.Type_elements_parameterContext> parameterContexts,
          PlSqlAstBuilder astBuilder) {
    List<Parameter> parameters = new ArrayList<>();
    for (PlSqlParser.Type_elements_parameterContext e : parameterContexts) {
      parameters.add((Parameter) astBuilder.visit(e));
    }
    return parameters;
  }

  /**
   * Extracts all declarations from seq_of_declare_specs context.
   */
  public static DeclarationBundle extractDeclarations(
          PlSqlParser.Seq_of_declare_specsContext declareSpecs,
          PlSqlAstBuilder astBuilder) {
    List<Variable> variables = new ArrayList<>();
    List<CursorDeclaration> cursorDeclarations = new ArrayList<>();
    List<RecordType> recordTypes = new ArrayList<>();
    List<VarrayType> varrayTypes = new ArrayList<>();
    List<NestedTableType> nestedTableTypes = new ArrayList<>();

    if (declareSpecs != null) {
      variables = extractVariablesFromDeclareSpecs(declareSpecs, astBuilder);
      cursorDeclarations = extractCursorDeclarationsFromDeclareSpecs(declareSpecs, astBuilder);
      recordTypes = extractRecordTypesFromDeclareSpecs(declareSpecs, astBuilder);
      varrayTypes = extractVarrayTypesFromDeclareSpecs(declareSpecs, astBuilder);
      nestedTableTypes = extractNestedTableTypesFromDeclareSpecs(declareSpecs, astBuilder);
    }

    return new DeclarationBundle(variables, cursorDeclarations, recordTypes, varrayTypes, nestedTableTypes);
  }

  /**
   * Extracts statements from body context.
   */
  public static List<Statement> extractStatements(
          PlSqlParser.BodyContext bodyContext,
          PlSqlAstBuilder astBuilder) {
    List<Statement> statements = new ArrayList<>();
    if (bodyContext != null
            && bodyContext.seq_of_statements() != null
            && bodyContext.seq_of_statements().statement() != null) {
      for (PlSqlParser.StatementContext stmt : bodyContext.seq_of_statements().statement()) {
        statements.add((Statement) astBuilder.visit(stmt));
      }
    }
    return statements;
  }

  /**
   * Bundle class to hold all extracted declarations.
   */
  public static class DeclarationBundle {
    private final List<Variable> variables;
    private final List<CursorDeclaration> cursorDeclarations;
    private final List<RecordType> recordTypes;
    private final List<VarrayType> varrayTypes;
    private final List<NestedTableType> nestedTableTypes;

    public DeclarationBundle(List<Variable> variables, List<CursorDeclaration> cursorDeclarations,
                             List<RecordType> recordTypes, List<VarrayType> varrayTypes,
                             List<NestedTableType> nestedTableTypes) {
      this.variables = variables;
      this.cursorDeclarations = cursorDeclarations;
      this.recordTypes = recordTypes;
      this.varrayTypes = varrayTypes;
      this.nestedTableTypes = nestedTableTypes;
    }

    public List<Variable> getVariables() { return variables; }
    public List<CursorDeclaration> getCursorDeclarations() { return cursorDeclarations; }
    public List<RecordType> getRecordTypes() { return recordTypes; }
    public List<VarrayType> getVarrayTypes() { return varrayTypes; }
    public List<NestedTableType> getNestedTableTypes() { return nestedTableTypes; }
  }

  /**
   * Extracts variables from seq_of_declare_specs context.
   * Returns a list of Variable objects found in the DECLARE section.
   */
  public static List<Variable> extractVariablesFromDeclareSpecs(
          PlSqlParser.Seq_of_declare_specsContext ctx,
          PlSqlAstBuilder astBuilder) {
    List<Variable> variables = new ArrayList<>();

    if (ctx != null && ctx.declare_spec() != null) {
      for (PlSqlParser.Declare_specContext declareSpec : ctx.declare_spec()) {
        if (declareSpec.variable_declaration() != null) {
          Variable variable = (Variable) astBuilder.visit(declareSpec.variable_declaration());
          if (variable != null) {
            variables.add(variable);
          }
        }
        // Note: Other declare_spec types (procedure_spec, function_spec, cursor_declaration, etc.)
        // are handled elsewhere in the parsing process
      }
    }

    return variables;
  }

  /**
   * Extracts cursor declarations from seq_of_declare_specs context.
   * Returns a list of CursorDeclaration objects found in the DECLARE section.
   */
  public static List<CursorDeclaration> extractCursorDeclarationsFromDeclareSpecs(
          PlSqlParser.Seq_of_declare_specsContext ctx,
          PlSqlAstBuilder astBuilder) {
    List<CursorDeclaration> cursors = new ArrayList<>();

    if (ctx != null && ctx.declare_spec() != null) {
      for (PlSqlParser.Declare_specContext declareSpec : ctx.declare_spec()) {
        if (declareSpec.cursor_declaration() != null) {
          CursorDeclaration cursor = (CursorDeclaration) astBuilder.visit(declareSpec.cursor_declaration());
          if (cursor != null) {
            cursors.add(cursor);
          }
        }
      }
    }

    return cursors;
  }

  /**
   * Extracts record type declarations from seq_of_declare_specs context.
   * Returns a list of RecordType objects found in the DECLARE section.
   */
  public static List<RecordType> extractRecordTypesFromDeclareSpecs(
          PlSqlParser.Seq_of_declare_specsContext ctx,
          PlSqlAstBuilder astBuilder) {
    List<RecordType> recordTypes = new ArrayList<>();
    
    if (ctx != null && ctx.declare_spec() != null) {
      for (PlSqlParser.Declare_specContext declareSpec : ctx.declare_spec()) {
        if (declareSpec.type_declaration() != null) {
          PlSqlAst typeDeclaration = astBuilder.visit(declareSpec.type_declaration());
          if (typeDeclaration instanceof RecordType) {
            recordTypes.add((RecordType) typeDeclaration);
          }
        }
      }
    }
    
    return recordTypes;
  }

  /**
   * Extracts VARRAY type declarations from seq_of_declare_specs context.
   * Returns a list of VarrayType objects found in the DECLARE section.
   */
  public static List<VarrayType> extractVarrayTypesFromDeclareSpecs(
          PlSqlParser.Seq_of_declare_specsContext ctx,
          PlSqlAstBuilder astBuilder) {
    List<VarrayType> varrayTypes = new ArrayList<>();
    
    if (ctx != null && ctx.declare_spec() != null) {
      for (PlSqlParser.Declare_specContext declareSpec : ctx.declare_spec()) {
        if (declareSpec.type_declaration() != null) {
          PlSqlAst typeDeclaration = astBuilder.visit(declareSpec.type_declaration());
          if (typeDeclaration instanceof VarrayType) {
            varrayTypes.add((VarrayType) typeDeclaration);
          }
        }
      }
    }
    
    return varrayTypes;
  }

  /**
   * Extracts TABLE OF type declarations from seq_of_declare_specs context.
   * Returns a list of NestedTableType objects found in the DECLARE section.
   */
  public static List<NestedTableType> extractNestedTableTypesFromDeclareSpecs(
          PlSqlParser.Seq_of_declare_specsContext ctx,
          PlSqlAstBuilder astBuilder) {
    List<NestedTableType> nestedTableTypes = new ArrayList<>();
    
    if (ctx != null && ctx.declare_spec() != null) {
      for (PlSqlParser.Declare_specContext declareSpec : ctx.declare_spec()) {
        if (declareSpec.type_declaration() != null) {
          PlSqlAst typeDeclaration = astBuilder.visit(declareSpec.type_declaration());
          if (typeDeclaration instanceof NestedTableType) {
            nestedTableTypes.add((NestedTableType) typeDeclaration);
          }
        }
      }
    }
    
    return nestedTableTypes;
  }

  /**
   * Helper method to parse exception handlers into an ExceptionBlock
   */
  public static ExceptionBlock parseExceptionBlock(List<PlSqlParser.Exception_handlerContext> handlerContexts, PlSqlAstBuilder astBuilder) {
    List<ExceptionHandler> handlers = new ArrayList<>();
    
    for (PlSqlParser.Exception_handlerContext handlerCtx : handlerContexts) {
      // Parse exception names (can be multiple with OR)
      List<String> exceptionNames = new ArrayList<>();
      if (handlerCtx.exception_name() != null) {
        for (var exceptionNameCtx : handlerCtx.exception_name()) {
          exceptionNames.add(exceptionNameCtx.getText());
        }
      }
      
      // Parse THEN statements
      List<Statement> statements = new ArrayList<>();
      if (handlerCtx.seq_of_statements() != null && handlerCtx.seq_of_statements().statement() != null) {
        for (PlSqlParser.StatementContext stmtCtx : handlerCtx.seq_of_statements().statement()) {
          Statement statement = (Statement) astBuilder.visit(stmtCtx);
          if (statement != null) {
            statements.add(statement);
          }
        }
      }
      
      handlers.add(new ExceptionHandler(exceptionNames, statements));
    }
    
    return new ExceptionBlock(handlers);
  }
}