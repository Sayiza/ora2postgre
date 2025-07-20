package me.christianrobert.ora2postgre.plsql;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.antlr.PlSqlParserBaseVisitor;
import me.christianrobert.ora2postgre.plsql.ast.*;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitCallStatement;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitCursorDeclaration;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitLogicalExpression;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitLoopStatement;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitIfStatement;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitSelectListElements;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitReturnStatement;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitSelectStatement;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitStatement;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitSubquery;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitFetchStatement;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitFieldSpec;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitObjectMemberSpec;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitRecordTypeDef;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitTypeDeclaration;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitTypeDefinition;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitTypeElementsParameter;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitFuncDeclInType;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitProcDeclInType;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitWithClause;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitSeqOfDeclareSpecs;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitSqlScript;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitTypeSpec;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitUnaryLogicalExpression;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitVariableDeclaration;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitUpdateStatement;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitSingleTableInsert;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitQueryBlock;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitSubqueryFactoringClause;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitTableRefAuxInternalTwo;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitDmlTableExpressionClause;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitUnaryExpression;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitOtherFunction;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitFunctionBody;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitProcedureBody;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitCreateFunctionBody;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitCreatePackageBody;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitCreateProcedureBody;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitCreatePackage;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitDeleteStatement;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitConstructorDeclaration;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitConcatenation;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitTypeBody;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitRelationalExpression;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitOverClause;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitOpenStatement;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitExitStatement;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitExpression;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitMultisetExpression;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitModelExpression;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitCompoundExpression;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitVarrayTypeDef;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitWhereClause;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitCondition;

public class PlSqlAstBuilder extends PlSqlParserBaseVisitor<PlSqlAst> {
  public final String schema;
  private String currentPackageName;  // Track current package context for call resolution

  public PlSqlAstBuilder(String schema) {
    this.schema = schema;
  }

  public String getSchema() {
    return schema;
  }

  public String getCurrentPackageName() {
    return currentPackageName;
  }

  public void setCurrentPackageName(String currentPackageName) {
    this.currentPackageName = currentPackageName;
  }

  @Override
  public PlSqlAst visitSql_script(PlSqlParser.Sql_scriptContext ctx) {
    return VisitSqlScript.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitVariable_declaration(PlSqlParser.Variable_declarationContext ctx) {
    return VisitVariableDeclaration.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitCursor_declaration(PlSqlParser.Cursor_declarationContext ctx) {
    return VisitCursorDeclaration.visit(ctx, this);
  }


  @Override
  public PlSqlAst visitSeq_of_declare_specs(PlSqlParser.Seq_of_declare_specsContext ctx) {
    return VisitSeqOfDeclareSpecs.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitType_spec(PlSqlParser.Type_specContext ctx) {
    return VisitTypeSpec.visit(ctx, schema);
  }

  // Statement START
  @Override
  public PlSqlAst visitStatement(PlSqlParser.StatementContext ctx) {
    return VisitStatement.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitNull_statement(PlSqlParser.Null_statementContext ctx) {
    return new Comment("do nothing");
  }

  @Override
  public PlSqlAst visitCursor_manipulation_statements(PlSqlParser.Cursor_manipulation_statementsContext ctx) {
    // Delegate to specific cursor statement visitors
    if (ctx.getChildCount() == 1) {
      return visit(ctx.getChild(0));
    }
    return new Comment("unclear cursor manipulation statement structure");
  }

  @Override
  public PlSqlAst visitOpen_statement(PlSqlParser.Open_statementContext ctx) {
    return VisitOpenStatement.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitFetch_statement(PlSqlParser.Fetch_statementContext ctx) {
    return VisitFetchStatement.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitClose_statement(PlSqlParser.Close_statementContext ctx) {
    String cursorName = ctx.cursor_name().getText();
    return new CloseStatement(cursorName);
  }

  @Override
  public PlSqlAst visitCall_statement(PlSqlParser.Call_statementContext ctx) {
    return VisitCallStatement.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitReturn_statement(PlSqlParser.Return_statementContext ctx) {
    return VisitReturnStatement.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitExit_statement(PlSqlParser.Exit_statementContext ctx) {
    return VisitExitStatement.visit(ctx, this);
  }

  // LOOP and SELECT START
  @Override
  public PlSqlAst visitLoop_statement(PlSqlParser.Loop_statementContext ctx) {
    return VisitLoopStatement.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitSelect_statement(PlSqlParser.Select_statementContext ctx) {
    return VisitSelectStatement.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitSubquery(PlSqlParser.SubqueryContext ctx) {
    return VisitSubquery.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitSubquery_basic_elements(PlSqlParser.Subquery_basic_elementsContext ctx) {
    return new SelectSubQueryBasicElement(
            schema,
            ctx.subquery() != null ? (SelectSubQuery) visit(ctx.subquery()) : null,
            ctx.query_block() != null ? (SelectQueryBlock) visit(ctx.query_block()) : null
    );
  }

  @Override
  public PlSqlAst visitQuery_block(PlSqlParser.Query_blockContext ctx) {
    return VisitQueryBlock.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitWith_clause(PlSqlParser.With_clauseContext ctx) {
    return VisitWithClause.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitWith_factoring_clause(PlSqlParser.With_factoring_clauseContext ctx) {
    if (ctx.subquery_factoring_clause() != null) {
      return visit(ctx.subquery_factoring_clause());
    } else if (ctx.subav_factoring_clause() != null) {
      // TODO: Handle subav_factoring_clause (advanced analytical feature)
      return null;
    }
    return null;
  }
  
  /**
   * Visits Oracle subquery factoring clause and creates CommonTableExpression AST node.
   * Grammar: query_name paren_column_list? AS '(' subquery order_by_clause? ')' search_clause? cycle_clause?
   */
  @Override
  public PlSqlAst visitSubquery_factoring_clause(PlSqlParser.Subquery_factoring_clauseContext ctx) {
    return VisitSubqueryFactoringClause.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitSelect_list_elements(PlSqlParser.Select_list_elementsContext ctx) {
    return VisitSelectListElements.visit(ctx, this);
  }
  // LOOP and SELECT END

  // Table-From START
  @Override
  public PlSqlAst visitTable_ref(PlSqlParser.Table_refContext ctx) {
    return new TableReference(
            (TableReferenceAux) visit(ctx.table_ref_aux()),
            null,null,null //TODO
    );
  }

  @Override
  public PlSqlAst visitTable_ref_aux(PlSqlParser.Table_ref_auxContext ctx) {
    return new TableReferenceAux(
            (TableReferenceAuxInternal) visit(ctx.table_ref_aux_internal()),
            null, //TODO
            ctx.table_alias() != null ? ctx.table_alias().getText() : null
    );
  }

  @Override
  public PlSqlAst visitTable_ref_aux_internal_one(PlSqlParser.Table_ref_aux_internal_oneContext ctx) {
    return new TableReferenceAuxInternal(
            schema,
            (TableExpressionClause) visit(ctx.dml_table_expression_clause()),
            null, //TODO
            null, //TODO
            false
    );
  }

  @Override
  public PlSqlAst visitTable_ref_aux_internal_two(PlSqlParser.Table_ref_aux_internal_twoContext ctx) {
    return VisitTableRefAuxInternalTwo.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitTable_ref_aux_internal_thre(PlSqlParser.Table_ref_aux_internal_threContext ctx) {
    return new TableReferenceAuxInternal(
            schema,
            (TableExpressionClause) visit(ctx.dml_table_expression_clause()),
            null,
            null,
            true
    );
  }

  @Override
  public PlSqlAst visitDml_table_expression_clause(PlSqlParser.Dml_table_expression_clauseContext ctx) {
    return VisitDmlTableExpressionClause.visit(ctx, this);
  }
  // Table-From END

  @Override
  public PlSqlAst visitAssignment_statement(PlSqlParser.Assignment_statementContext ctx) {

    String target = ctx.general_element().getText();
    Expression expressionVisited = (Expression) visit(ctx.expression());
    //Expression expression = new Expression(ctx.expression().getText());
    return new AssignmentStatement(target, expressionVisited);
  }

  @Override
  public PlSqlAst visitIf_statement(PlSqlParser.If_statementContext ctx) {
    return VisitIfStatement.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitInsert_statement(PlSqlParser.Insert_statementContext ctx) {
    // For now, focus on single_table_insert (most common case)
    if (ctx.single_table_insert() != null) {
      return visitSingle_table_insert(ctx.single_table_insert());
    }
    
    // TODO: Handle multi_table_insert later if needed
    return new Comment("multi_table_insert not implemented yet");
  }

  @Override
  public PlSqlAst visitSingle_table_insert(PlSqlParser.Single_table_insertContext ctx) {
    return VisitSingleTableInsert.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitUpdate_statement(PlSqlParser.Update_statementContext ctx) {
    return VisitUpdateStatement.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitDelete_statement(PlSqlParser.Delete_statementContext ctx) {
    return VisitDeleteStatement.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitRaise_statement(PlSqlParser.Raise_statementContext ctx) {
    // Handle bare RAISE (re-raise current exception)
    if (ctx.exception_name() == null) {
      return new RaiseStatement(); // Default constructor for re-raise
    }
    
    // Handle RAISE with specific exception name
    String exceptionName = ctx.exception_name().getText();
    return new RaiseStatement(exceptionName);
  }
  // Statement END

  @Override
  public PlSqlAst visitExpression(PlSqlParser.ExpressionContext ctx) {
    return VisitExpression.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitCursor_expression(PlSqlParser.Cursor_expressionContext ctx) {
    SelectSubQuery subquery = null;
    if (ctx.subquery() != null) {
      subquery = (SelectSubQuery) visit(ctx.subquery());
    }
    return new CursorExpression(subquery, schema);
  }

  @Override
  public PlSqlAst visitLogical_expression(PlSqlParser.Logical_expressionContext ctx) {
    return VisitLogicalExpression.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitUnary_logical_expression(PlSqlParser.Unary_logical_expressionContext ctx) {
    return VisitUnaryLogicalExpression.visit(ctx, this);
  }

  // Start CollectionTypes, other than Object Types:
  @Override
  public PlSqlAst visitVarray_type_def(PlSqlParser.Varray_type_defContext ctx) {
    return VisitVarrayTypeDef.visit(ctx, this);
  }
  @Override
  public PlSqlAst visitNested_table_type_def(PlSqlParser.Nested_table_type_defContext ctx) {
    return new NestedTableType((DataTypeSpec) visit(ctx.type_spec()));
  }

  @Override
  public PlSqlAst visitTable_type_def(PlSqlParser.Table_type_defContext ctx) {
    return new NestedTableType((DataTypeSpec) visit(ctx.type_spec()));
  }

  // Start Object Types
  @Override
  public PlSqlAst visitType_definition(PlSqlParser.Type_definitionContext ctx) {
    return VisitTypeDefinition.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitObject_member_spec(PlSqlParser.Object_member_specContext ctx) {
    return VisitObjectMemberSpec.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitType_elements_parameter(PlSqlParser.Type_elements_parameterContext ctx) {
    return VisitTypeElementsParameter.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitFunc_decl_in_type(PlSqlParser.Func_decl_in_typeContext ctx) {
    return VisitFuncDeclInType.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitProc_decl_in_type(PlSqlParser.Proc_decl_in_typeContext ctx) {
    return VisitProcDeclInType.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitConstructor_declaration(PlSqlParser.Constructor_declarationContext ctx) {
    return VisitConstructorDeclaration.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitSubprog_decl_in_type(PlSqlParser.Subprog_decl_in_typeContext ctx) {
    if (ctx.func_decl_in_type() != null) {
      return visit(ctx.func_decl_in_type());
    }
    if (ctx.proc_decl_in_type() != null) {
      return visit(ctx.proc_decl_in_type());
    }
    return null;
  }

  @Override
  public PlSqlAst visitType_body_elements(PlSqlParser.Type_body_elementsContext ctx) {
    if (ctx.constructor_declaration() != null) {
      return visit(ctx.constructor_declaration());
    }
    if (ctx.subprog_decl_in_type() != null) {
      return visit(ctx.subprog_decl_in_type());
    }
    return null;
  }

  @Override
  public PlSqlAst visitType_body(PlSqlParser.Type_bodyContext ctx) {
    return VisitTypeBody.visit(ctx, this);
  }

  // PACKAGE
  @Override
  public PlSqlAst visitParameter(PlSqlParser.ParameterContext ctx) {
    return new Parameter(
            ctx.parameter_name().getText(),
            (DataTypeSpec) visit(ctx.type_spec()),
            ctx.default_value_part() != null ? (Expression) visit(ctx.default_value_part()) : null,
            ctx.IN() != null && !ctx.IN().isEmpty(),
            ctx.OUT() != null && !ctx.OUT().isEmpty()
    );
  }

  @Override
  public PlSqlAst visitProcedure_spec(PlSqlParser.Procedure_specContext ctx) {
    return null; // TODO add Procdure to decide between public and private?!
    // do the same 4 function!?
  }

  @Override
  public PlSqlAst visitProcedure_body(PlSqlParser.Procedure_bodyContext ctx) {
    return VisitProcedureBody.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitFunction_body(PlSqlParser.Function_bodyContext ctx) {
    return VisitFunctionBody.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitCreate_package(PlSqlParser.Create_packageContext ctx) {
    return VisitCreatePackage.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitCreate_package_body(PlSqlParser.Create_package_bodyContext ctx) {
    return VisitCreatePackageBody.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitCreate_function_body(PlSqlParser.Create_function_bodyContext ctx) {
    return VisitCreateFunctionBody.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitCreate_procedure_body(PlSqlParser.Create_procedure_bodyContext ctx) {
    return VisitCreateProcedureBody.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitWhere_clause(PlSqlParser.Where_clauseContext ctx) {
    return VisitWhereClause.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitCondition(PlSqlParser.ConditionContext ctx) {
    return VisitCondition.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitUnary_expression(PlSqlParser.Unary_expressionContext ctx) {
    return VisitUnaryExpression.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitMultiset_expression(PlSqlParser.Multiset_expressionContext ctx) {
    return VisitMultisetExpression.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitRelational_expression(PlSqlParser.Relational_expressionContext ctx) {
    return VisitRelationalExpression.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitCompound_expression(PlSqlParser.Compound_expressionContext ctx) {
    return VisitCompoundExpression.visit(ctx, this);
  }

  /**
   * Visitor method for concatenation rule.
   */
  @Override
  public PlSqlAst visitConcatenation(PlSqlParser.ConcatenationContext ctx) {
    return VisitConcatenation.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitStandard_function(PlSqlParser.Standard_functionContext ctx) {
    if (ctx.other_function() != null) {
      return visit(ctx.other_function());
    }
    
    // For other types of standard functions, fall back to default behavior
    return visitChildren(ctx);
  }

  @Override
  public PlSqlAst visitOther_function(PlSqlParser.Other_functionContext ctx) {
    return VisitOtherFunction.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitOver_clause(PlSqlParser.Over_clauseContext ctx) {
    return VisitOverClause.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitModel_expression(PlSqlParser.Model_expressionContext ctx) {
    return VisitModelExpression.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitType_declaration(PlSqlParser.Type_declarationContext ctx) {
    return VisitTypeDeclaration.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitRecord_type_def(PlSqlParser.Record_type_defContext ctx) {
    return VisitRecordTypeDef.visit(ctx, this);
  }

  @Override
  public PlSqlAst visitField_spec(PlSqlParser.Field_specContext ctx) {
    return VisitFieldSpec.visit(ctx, this);
  }
}