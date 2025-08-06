package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.ProcedureTransformationManager;

import java.util.ArrayList;
import java.util.List;

public class Procedure extends ExecutableRoutine {
  // No procedure-specific fields needed - all functionality is in base class
  
  private static final ProcedureTransformationManager transformationManager = new ProcedureTransformationManager();

  public Procedure(
          String name,
          List<Parameter> parameters,
          List<Variable> variables,
          List<Statement> statements) {
    super(name, parameters, variables, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), statements, null);
  }

  // Constructor with exception handling
  public Procedure(
          String name,
          List<Parameter> parameters,
          List<Variable> variables,
          List<Statement> statements,
          ExceptionBlock exceptionBlock) {
    super(name, parameters, variables, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), statements, exceptionBlock);
  }

  // Constructor with record types and cursor declarations
  public Procedure(
          String name,
          List<Parameter> parameters,
          List<Variable> variables,
          List<CursorDeclaration> cursorDeclarations,
          List<RecordType> recordTypes,
          List<Statement> statements,
          ExceptionBlock exceptionBlock) {
    super(name, parameters, variables, cursorDeclarations, recordTypes, new ArrayList<>(), new ArrayList<>(), statements, exceptionBlock);
  }

  // Constructor with all declaration types (matching Function constructor pattern)
  public Procedure(
          String name,
          List<Parameter> parameters,
          List<Variable> variables,
          List<CursorDeclaration> cursorDeclarations,
          List<RecordType> recordTypes,
          List<VarrayType> varrayTypes,
          List<NestedTableType> nestedTableTypes,
          List<Statement> statements,
          ExceptionBlock exceptionBlock) {
    super(name, parameters, variables, cursorDeclarations, recordTypes, varrayTypes, nestedTableTypes, statements, exceptionBlock);
  }

  // Procedure-specific methods  
  public boolean hasExceptionHandling() {
    return exceptionBlock != null && exceptionBlock.hasHandlers();
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  // toJava() method removed - business logic now stays in PostgreSQL
  // REST endpoints will call PostgreSQL procedures directly
  
  /**
   * Gets the PostgreSQL procedure name that this procedure will become.
   */
  public String getPostgreProcedureName() {
    return getPostgreQualifiedName();
  }

  @Override
  public String getPostgreQualifiedName() {
    if (isStandalone) {
      return schema.toUpperCase() + "." + name.toLowerCase();
    }
    String schema = parentType != null ? parentType.getSchema().toUpperCase() :
                   parentPackage.getSchema().toUpperCase();
    String objectName = parentType != null ? parentType.getName().toUpperCase() :
                       parentPackage.getName().toUpperCase();
    return schema + "." + objectName + "_" + name.toLowerCase();
  }

  /**
   * Transforms this procedure to PostgreSQL DDL using the transformation manager.
   * This method serves dual purposes in the established dual usage pattern:
   * - As main object: delegates to transformation manager for complex orchestration
   * - As sub-element: enables direct toPostgre() calls in AST chains
   */
  @Override
  public String toPostgre(Everything data, boolean specOnly) {
    return transformationManager.transform(this, data, specOnly);
  }

  private boolean isWeb() {
    return true; //TODO this. has some htp inside? or check for synonyms?
  }
}