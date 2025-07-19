package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.CursorDeclaration;
import me.christianrobert.ora2postgre.plsql.ast.NestedTableType;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;
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
      variables = VisitSeqOfDeclareSpecs.extractVariablesFromDeclareSpecs(declareSpecs, astBuilder);
      cursorDeclarations = astBuilder.extractCursorDeclarationsFromDeclareSpecs(declareSpecs);
      recordTypes = astBuilder.extractRecordTypesFromDeclareSpecs(declareSpecs);
      varrayTypes = astBuilder.extractVarrayTypesFromDeclareSpecs(declareSpecs);
      nestedTableTypes = astBuilder.extractNestedTableTypesFromDeclareSpecs(declareSpecs);
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
}