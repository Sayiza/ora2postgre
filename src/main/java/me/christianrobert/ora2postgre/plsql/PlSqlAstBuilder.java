package me.christianrobert.ora2postgre.plsql;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.antlr.PlSqlParserBaseVisitor;
import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;
import me.christianrobert.ora2postgre.oracledb.tools.NameNormalizer;
import me.christianrobert.ora2postgre.plsql.builderfncs.DeclarationParsingUtils;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitCallStatement;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitCursorDeclaration;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitLogicalExpression;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitLoopStatement;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitIfStatement;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitSelectListElements;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitSelectStatement;
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

import java.util.ArrayList;
import java.util.List;

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

  /**
   * Extracts record type declarations from seq_of_declare_specs context.
   * Returns a list of RecordType objects found in the DECLARE section.
   */
  public List<RecordType> extractRecordTypesFromDeclareSpecs(PlSqlParser.Seq_of_declare_specsContext ctx) {
    List<RecordType> recordTypes = new ArrayList<>();
    
    if (ctx != null && ctx.declare_spec() != null) {
      for (PlSqlParser.Declare_specContext declareSpec : ctx.declare_spec()) {
        if (declareSpec.type_declaration() != null) {
          PlSqlAst typeDeclaration = visit(declareSpec.type_declaration());
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
  public List<VarrayType> extractVarrayTypesFromDeclareSpecs(PlSqlParser.Seq_of_declare_specsContext ctx) {
    List<VarrayType> varrayTypes = new ArrayList<>();
    
    if (ctx != null && ctx.declare_spec() != null) {
      for (PlSqlParser.Declare_specContext declareSpec : ctx.declare_spec()) {
        if (declareSpec.type_declaration() != null) {
          PlSqlAst typeDeclaration = visit(declareSpec.type_declaration());
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
  public List<NestedTableType> extractNestedTableTypesFromDeclareSpecs(PlSqlParser.Seq_of_declare_specsContext ctx) {
    List<NestedTableType> nestedTableTypes = new ArrayList<>();
    
    if (ctx != null && ctx.declare_spec() != null) {
      for (PlSqlParser.Declare_specContext declareSpec : ctx.declare_spec()) {
        if (declareSpec.type_declaration() != null) {
          PlSqlAst typeDeclaration = visit(declareSpec.type_declaration());
          if (typeDeclaration instanceof NestedTableType) {
            nestedTableTypes.add((NestedTableType) typeDeclaration);
          }
        }
      }
    }
    
    return nestedTableTypes;
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
    if (ctx.getChildCount() == 1) {
      PlSqlAst visitedStatement = visit(ctx.getChild(0));
      if (visitedStatement != null) {
        return visitedStatement;
      }
      return new Comment("type of statement not found"+ this.getClass());
    }

    return new Comment("unclear statement structure: " + this.getClass()); //TODO
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
    String cursorName = ctx.cursor_name().getText();
    
    // Parse parameters if present
    List<Expression> parameters = new ArrayList<>();
    if (ctx.expressions_() != null && ctx.expressions_().expression() != null) {
      for (PlSqlParser.ExpressionContext exprCtx : ctx.expressions_().expression()) {
        Expression expr = (Expression) visit(exprCtx);
        if (expr != null) {
          parameters.add(expr);
        }
      }
    }
    
    return new OpenStatement(cursorName, parameters);
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
    Expression expression = (Expression) visit(ctx.expression());
    if (expression != null) {
      return new ReturnStatement(expression);
    }
    return new ReturnStatement(null);
  }

  @Override
  public PlSqlAst visitExit_statement(PlSqlParser.Exit_statementContext ctx) {
    String labelName = null;
    Expression condition = null;
    
    // Check for optional label name
    if (ctx.label_name() != null) {
      labelName = ctx.label_name().getText();
    }
    
    // Check for optional WHEN condition
    if (ctx.condition() != null) {
      condition = (Expression) visit(ctx.condition());
    }
    
    return new ExitStatement(labelName, condition);
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

  /**
   * Visits Oracle WITH clause and creates SelectWithClause AST node.
   * Grammar: WITH (function_body | procedure_body)* with_factoring_clause (',' with_factoring_clause)*
   */
  @Override
  public PlSqlAst visitWith_clause(PlSqlParser.With_clauseContext ctx) {
    return VisitWithClause.visit(ctx, this);
  }
  
  /**
   * Visits Oracle WITH factoring clause and delegates to appropriate handler.
   * Grammar: subquery_factoring_clause | subav_factoring_clause
   */
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
    // Parse table name from general_table_ref
    String schemaName = null;
    String tableName = null;
    
    if (ctx.general_table_ref() != null && 
        ctx.general_table_ref().dml_table_expression_clause() != null &&
        ctx.general_table_ref().dml_table_expression_clause().tableview_name() != null) {
      
      var tableview = ctx.general_table_ref().dml_table_expression_clause().tableview_name();
      if (tableview.identifier() != null) {
        if (tableview.id_expression() != null) {
          // Schema.Table format
          schemaName = tableview.identifier().getText();
          tableName = tableview.id_expression().getText();
        } else {
          // Just table name
          tableName = tableview.identifier().getText();
        }
      }
    }
    
    // Parse WHERE clause if present
    Expression whereClause = null;
    if (ctx.where_clause() != null && ctx.where_clause().condition() != null) {
      whereClause = (Expression) visit(ctx.where_clause().condition());
    }
    
    return new DeleteStatement(schemaName, tableName, whereClause);
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
    if (ctx.cursor_expression() != null) {
      CursorExpression cursorExpr = (CursorExpression) visit(ctx.cursor_expression());
      return new Expression(cursorExpr);
    }
    if (ctx.logical_expression() != null) {
      LogicalExpression logicalExpr = (LogicalExpression) visit(ctx.logical_expression());
      return new Expression(logicalExpr);
    }
    
    // If neither cursor nor logical expression is found, we need to handle this case
    // According to the grammar, this should not happen, but create a simple logical expression as fallback
    LogicalExpression fallbackLogical = new LogicalExpression(new UnaryLogicalExpression(ctx.getText()));
    return new Expression(fallbackLogical);
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
    Long sizeNumeric = null;
    Expression expression = null;
    if (ctx.expression() != null
            && ctx.expression().getText() != null
            && ctx.expression().getText().matches("-?\\d+")) {
      sizeNumeric = Long.parseLong(ctx.expression().getText());
    } else {
      expression = (Expression) visit(ctx.expression());
    }

    return new VarrayType(sizeNumeric, expression, (DataTypeSpec) visit(ctx.type_spec()));
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
    String name = null;

    List<Parameter> parameters = new ArrayList<>();

    if (ctx.type_spec() != null) {
      for (var constpart : ctx.type_spec()) {
        if (constpart instanceof PlSqlParser.Type_specContext) {
          name = constpart.getText();
        }
      }
    }

    for (PlSqlParser.Type_elements_parameterContext e : ctx.type_elements_parameter()) {
      parameters.add((Parameter) visit(e));
    }
    List<Statement> statements = new ArrayList<>();
    if (ctx.body() != null
            && ctx.body().seq_of_statements() != null
            && ctx.body().seq_of_statements().statement() != null) {
      for (PlSqlParser.StatementContext stmt : ctx.body().seq_of_statements().statement()) {
        statements.add((Statement) visit(stmt));
      }
    }

    if (name != null)
      return new Constructor(name, parameters, statements);
    return null;
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
    String name = NameNormalizer.normalizeObjectTypeName(ctx.type_name().getText());
    List<Constructor> constr = new ArrayList<>();
    List<Procedure> procedures = new ArrayList<>();
    List<Function> funcs = new ArrayList<>();
    if (ctx.type_body_elements() != null) {
      for (var member : ctx.type_body_elements()) {
        PlSqlAst memberAst = visit(member);
        if (memberAst instanceof Constructor) {
          constr.add((Constructor) memberAst);
        }
        if (memberAst instanceof Function) {
          funcs.add((Function) memberAst);
        }
        if (memberAst instanceof Procedure) {
          procedures.add((Procedure) memberAst);
        }
      }
    }
    ObjectType o = new ObjectType(name, schema, null, funcs, procedures, constr, null, null);
    o.getConstuctors().forEach(e -> e.setParentType(o));
    o.getFunctions().forEach(e -> e.setParentType(o));
    o.getProcedures().forEach(e -> e.setParentType(o));
    return o;
  }

  // PACKAGE

  @Override
  public PlSqlAst visitParameter(PlSqlParser.ParameterContext ctx) {
    return new Parameter(
            ctx.parameter_name().getText(),
            (DataTypeSpec) visit(ctx.type_spec()),
            ctx.default_value_part() != null ? (Expression) visit(ctx.default_value_part()) : null,
            ctx.IN() != null && !ctx.IN().isEmpty(),
            ctx.OUT() != null && !ctx.IN().isEmpty()
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

  /**
   * Helper method to parse exception handlers into an ExceptionBlock
   */
  public ExceptionBlock parseExceptionBlock(List<PlSqlParser.Exception_handlerContext> handlerContexts) {
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
          Statement statement = (Statement) visit(stmtCtx);
          if (statement != null) {
            statements.add(statement);
          }
        }
      }
      
      handlers.add(new ExceptionHandler(exceptionNames, statements));
    }
    
    return new ExceptionBlock(handlers);
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
    if (ctx.cursor_name() != null) {
      // CURRENT OF cursor_name variant
      String cursorName = ctx.cursor_name().getText();
      return new WhereClause(cursorName);
    } else if (ctx.condition() != null) {
      // condition variant
      Expression condition = (Expression) visit(ctx.condition());
      return new WhereClause(condition);
    }
    return null;
  }

  @Override
  public PlSqlAst visitCondition(PlSqlParser.ConditionContext ctx) {
    if (ctx.expression() != null) {
      return visit(ctx.expression());
    } else if (ctx.JSON_EQUAL() != null && ctx.expressions_() != null) {
      // Handle JSON_EQUAL function - for now, convert to raw text
      String jsonEqualText = "JSON_EQUAL(" + ctx.expressions_().getText() + ")";
      // Convert JSON_EQUAL to logical expression
      LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression(jsonEqualText));
      return new Expression(logicalExpr);
    }
    return null;
  }

  /**
   * Visitor method for unary_expression rule.
   * This is the key method that handles standard_function calls including cursor attributes.
   */
  @Override
  public PlSqlAst visitUnary_expression(PlSqlParser.Unary_expressionContext ctx) {
    return VisitUnaryExpression.visit(ctx, this);
  }


  /**
   * Visitor method for multiset_expression rule.
   */
  @Override
  public PlSqlAst visitMultiset_expression(PlSqlParser.Multiset_expressionContext ctx) {
    if (ctx.relational_expression() != null) {
      // Visit the relational expression
      PlSqlAst relationalAst = visit(ctx.relational_expression());
      if (relationalAst instanceof RelationalExpression) {
        return new MultisetExpression((RelationalExpression) relationalAst);
      }
      // Fallback
      return visitChildren(ctx);
    }
    
    // For other types of multiset expressions, fall back to default behavior
    return visitChildren(ctx);
  }

  /**
   * Visitor method for relational_expression rule.
   */
  @Override
  public PlSqlAst visitRelational_expression(PlSqlParser.Relational_expressionContext ctx) {
    if (ctx.compound_expression() != null) {
      PlSqlAst compoundAst = visit(ctx.compound_expression());
      if (compoundAst instanceof CompoundExpression) {
        return new RelationalExpression((CompoundExpression) compoundAst);
      }
      // Fallback
      return visitChildren(ctx);
    }
    
    // Handle relational operations: relational_expression relational_operator relational_expression
    if (ctx.relational_expression() != null && ctx.relational_expression().size() == 2 
        && ctx.relational_operator() != null) {
      PlSqlAst leftAst = visit(ctx.relational_expression(0));
      PlSqlAst rightAst = visit(ctx.relational_expression(1));
      String operator = ctx.relational_operator().getText();
      
      if (leftAst instanceof RelationalExpression && rightAst instanceof RelationalExpression) {
        return new RelationalExpression((RelationalExpression) leftAst, operator, (RelationalExpression) rightAst);
      }
    }
    
    // Fallback for other cases - should not normally happen, but use raw text
    throw new IllegalStateException("Unhandled relational expression case: " + ctx.getText());
  }

  /**
   * Visitor method for compound_expression rule.
   */
  @Override
  public PlSqlAst visitCompound_expression(PlSqlParser.Compound_expressionContext ctx) {
    if (ctx.concatenation() != null && !ctx.concatenation().isEmpty()) {
      // Visit the first concatenation
      PlSqlAst concatenationAst = visit(ctx.concatenation(0));
      if (concatenationAst instanceof Concatenation) {
        return new CompoundExpression((Concatenation) concatenationAst);
      }
      // Fallback
      return visitChildren(ctx);
    }
    
    // For other compound expressions, fall back to default behavior
    return visitChildren(ctx);
  }

  /**
   * Visitor method for concatenation rule.
   */
  @Override
  public PlSqlAst visitConcatenation(PlSqlParser.ConcatenationContext ctx) {
    // Handle simple model expression (most common case)
    if (ctx.model_expression() != null) {
      PlSqlAst modelAst = visit(ctx.model_expression());
      if (modelAst instanceof ModelExpression) {
        return new Concatenation((ModelExpression) modelAst);
      }
      // Fallback
      return visitChildren(ctx);
    }
    
    // Handle binary operations (arithmetic/concatenation operations)
    if (ctx.concatenation() != null && ctx.concatenation().size() == 2 && ctx.op != null) {
      // Parse left and right operands recursively
      PlSqlAst leftAst = visit(ctx.concatenation(0));
      PlSqlAst rightAst = visit(ctx.concatenation(1));
      
      if (leftAst instanceof Concatenation && rightAst instanceof Concatenation) {
        String operator = ctx.op.getText();
        return new Concatenation((Concatenation) leftAst, operator, (Concatenation) rightAst);
      }
    }
    
    // For other concatenation patterns, fall back to default behavior
    return new Concatenation(new ModelExpression(UnaryExpression.forAtom(new Expression(new LogicalExpression(new UnaryLogicalExpression(ctx.getText()))))));
  }

  /**
   * Visitor method for standard_function rule.
   * This delegates to the appropriate sub-function visitor.
   */
  @Override
  public PlSqlAst visitStandard_function(PlSqlParser.Standard_functionContext ctx) {
    if (ctx.other_function() != null) {
      return visit(ctx.other_function());
    }
    
    // For other types of standard functions, fall back to default behavior
    return visitChildren(ctx);
  }

  /**
   * Visitor method for other_function rule.
   * This handles cursor attributes and other Oracle functions.
   */
  @Override
  public PlSqlAst visitOther_function(PlSqlParser.Other_functionContext ctx) {
    return VisitOtherFunction.visit(ctx, this);
  }


  /**
   * Visitor method for over_clause rule.
   */
  @Override
  public PlSqlAst visitOver_clause(PlSqlParser.Over_clauseContext ctx) {
    java.util.List<Expression> partitionByColumns = null;
    java.util.List<OrderByElement> orderByElements = null;
    WindowingClause windowingClause = null;
    
    // Parse PARTITION BY clause
    if (ctx.query_partition_clause() != null) {
      partitionByColumns = parsePartitionByClause(ctx.query_partition_clause());
    }
    
    // Parse ORDER BY clause
    if (ctx.order_by_clause() != null) {
      orderByElements = parseOrderByClause(ctx.order_by_clause());
    }
    
    // Parse windowing clause
    if (ctx.windowing_clause() != null) {
      windowingClause = (WindowingClause) visit(ctx.windowing_clause());
    }
    
    return new OverClause(partitionByColumns, orderByElements, windowingClause);
  }

  /**
   * Parse PARTITION BY clause.
   */
  private java.util.List<Expression> parsePartitionByClause(PlSqlParser.Query_partition_clauseContext ctx) {
    java.util.List<Expression> partitionByColumns = new java.util.ArrayList<>();
    
    // Create simple text-based expressions for now
    // A more complete implementation would parse the actual expressions
    if (ctx != null && ctx.getText() != null) {
      String text = ctx.getText();
      if (text.contains("PARTITION")) {
        // Simple text-based expression as placeholder
        LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression(text));
        Expression expr = new Expression(logicalExpr);
        partitionByColumns.add(expr);
      }
    }
    
    return partitionByColumns;
  }

  /**
   * Parse ORDER BY clause for OVER clause.
   */
  private java.util.List<OrderByElement> parseOrderByClause(PlSqlParser.Order_by_clauseContext ctx) {
    java.util.List<OrderByElement> orderByElements = new java.util.ArrayList<>();
    
    // Create simple text-based order by elements for now
    // A more complete implementation would parse the actual order by elements
    if (ctx != null && ctx.getText() != null) {
      String text = ctx.getText();
      if (text.contains("ORDER")) {
        // Simple text-based expression as placeholder
        LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression(text));
        Expression expr = new Expression(logicalExpr);
        OrderByElement.SortDirection direction = text.contains("DESC") ? 
            OrderByElement.SortDirection.DESC : OrderByElement.SortDirection.ASC;
        OrderByElement orderByElement = new OrderByElement(expr, direction);
        orderByElements.add(orderByElement);
      }
    }
    
    return orderByElements;
  }

  /**
   * Visitor method for model_expression rule.
   */
  @Override
  public PlSqlAst visitModel_expression(PlSqlParser.Model_expressionContext ctx) {
    if (ctx.unary_expression() != null) {
      PlSqlAst unaryAst = visit(ctx.unary_expression());
      if (unaryAst instanceof UnaryExpression) {
        return new ModelExpression((UnaryExpression) unaryAst);
      }
      // Fallback - create a simple UnaryExpression from text
      UnaryExpression unaryExpr = UnaryExpression.forAtom(new Expression(new LogicalExpression(new UnaryLogicalExpression(ctx.unary_expression().getText()))));
      return new ModelExpression(unaryExpr);
    }
    
    // For other model expressions, fall back to default behavior
    return visitChildren(ctx);
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