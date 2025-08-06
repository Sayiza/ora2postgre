package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.helpers.StatementDeclarationCollector;
import me.christianrobert.ora2postgre.plsql.ast.tools.helpers.ToExportPostgre;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.TypeConverter;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.FunctionTransformationManager;

import java.util.ArrayList;
import java.util.List;

public class Function extends ExecutableRoutine {
  // Function-specific fields
  private String returnType;
  
  private static final FunctionTransformationManager transformationManager = new FunctionTransformationManager();

  public Function(String name,
                  List<Parameter> parameters,
                  List<Variable> variables,
                  String returnType,
                  List<Statement> statements) {
    super(name, parameters, variables, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), statements, null);
    this.returnType = returnType;
  }

  // Constructor with exception handling
  public Function(String name,
                  List<Parameter> parameters,
                  List<Variable> variables,
                  String returnType,
                  List<Statement> statements,
                  ExceptionBlock exceptionBlock) {
    super(name, parameters, variables, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), statements, exceptionBlock);
    this.returnType = returnType;
  }

  // Constructor with record types and cursor declarations
  public Function(String name,
                  List<Parameter> parameters,
                  List<Variable> variables,
                  List<CursorDeclaration> cursorDeclarations,
                  List<RecordType> recordTypes,
                  String returnType,
                  List<Statement> statements,
                  ExceptionBlock exceptionBlock) {
    super(name, parameters, variables, cursorDeclarations, recordTypes, new ArrayList<>(), new ArrayList<>(), statements, exceptionBlock);
    this.returnType = returnType;
  }

  // Constructor with all local declaration types (including collection types)
  public Function(String name,
                  List<Parameter> parameters,
                  List<Variable> variables,
                  List<CursorDeclaration> cursorDeclarations,
                  List<RecordType> recordTypes,
                  List<VarrayType> varrayTypes,
                  List<NestedTableType> nestedTableTypes,
                  String returnType,
                  List<Statement> statements,
                  ExceptionBlock exceptionBlock) {
    super(name, parameters, variables, cursorDeclarations, recordTypes, varrayTypes, nestedTableTypes, statements, exceptionBlock);
    this.returnType = returnType;
  }

  // Function-specific getters
  public String getReturnType() {
    return returnType;
  }

  public boolean hasExceptionHandling() {
    return exceptionBlock != null && exceptionBlock.hasHandlers();
  }


  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  // toJava() method removed - business logic now stays in PostgreSQL
  // REST endpoints will call PostgreSQL functions directly
  
  /**
   * Gets the PostgreSQL function name that this function will become.
   */
  public String getPostgreFunctionName() {
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
   * Transforms this function to PostgreSQL DDL using the transformation manager.
   * This method serves dual purposes in the established dual usage pattern:
   * - As main object: delegates to transformation manager for complex orchestration
   * - As sub-element: enables direct toPostgre() calls in AST chains
   */
  public String toPostgre(Everything data, boolean specOnly) {
    return transformationManager.transform(this, data, specOnly);
  }
  
}