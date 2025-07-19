package me.christianrobert.ora2postgre.plsql;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.antlr.PlSqlParserBaseVisitor;
import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;
import me.christianrobert.ora2postgre.oracledb.tools.NameNormalizer;
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
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitWithClause;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitSeqOfDeclareSpecs;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitSqlScript;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitTypeSpec;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitUnaryLogicalExpression;
import me.christianrobert.ora2postgre.plsql.builderfncs.VisitVariableDeclaration;

import java.util.ArrayList;
import java.util.List;

public class PlSqlAstBuilder extends PlSqlParserBaseVisitor<PlSqlAst> {
  private final String schema;
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
   * Extracts cursor declarations from seq_of_declare_specs context.
   * Returns a list of CursorDeclaration objects found in the DECLARE section.
   */
  public List<CursorDeclaration> extractCursorDeclarationsFromDeclareSpecs(PlSqlParser.Seq_of_declare_specsContext ctx) {
    List<CursorDeclaration> cursors = new ArrayList<>();
    
    if (ctx != null && ctx.declare_spec() != null) {
      for (PlSqlParser.Declare_specContext declareSpec : ctx.declare_spec()) {
        if (declareSpec.cursor_declaration() != null) {
          CursorDeclaration cursor = (CursorDeclaration) visit(declareSpec.cursor_declaration());
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
    List<SelectListElement> selectedFields = new ArrayList<>();
    List<TableReference> fromTables = new ArrayList<>();
    WhereClause whereClause = null;

    if (ctx.selected_list().ASTERISK() != null) {
      // TODO get all fields from the tables
    } else {
      for (PlSqlParser.Select_list_elementsContext se : ctx.selected_list().select_list_elements()) {
        selectedFields.add((SelectListElement) visit(se));
      }
    }

    if (ctx.from_clause() != null &&
            ctx.from_clause().table_ref_list() != null &&
            ctx.from_clause().table_ref_list().table_ref() != null
    ) {
      for (PlSqlParser.Table_refContext tr : ctx.from_clause().table_ref_list().table_ref()) {
        fromTables.add((TableReference) visit(tr));
      }
    } // TODO fix this

    // Handle WHERE clause if present
    if (ctx.where_clause() != null) {
      whereClause = (WhereClause) visit(ctx.where_clause());
    }

    return new SelectQueryBlock(schema, selectedFields, fromTables, whereClause);
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
    // Extract query name
    String queryName = ctx.query_name().getText();
    
    // Extract optional column list
    List<String> columnList = null;
    if (ctx.paren_column_list() != null) {
      columnList = new ArrayList<>();
      for (PlSqlParser.Column_nameContext colCtx : ctx.paren_column_list().column_list().column_name()) {
        columnList.add(colCtx.getText());
      }
    }
    
    // Extract subquery
    SelectSubQuery subQuery = null;
    if (ctx.subquery() != null) {
      subQuery = (SelectSubQuery) visit(ctx.subquery());
    }
    
    // Check for recursive CTE (Oracle uses SEARCH and CYCLE clauses to indicate recursion)
    boolean recursive = (ctx.search_clause() != null || ctx.cycle_clause() != null);
    
    // TODO: Handle search_clause and cycle_clause transformation
    // For now, we'll just detect their presence to mark as recursive
    
    return new CommonTableExpression(queryName, columnList, subQuery, recursive);
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
    List<SelectSubQueryBasicElement> unionList = new ArrayList<>();
    List<SelectSubQueryBasicElement> unionAllList = new ArrayList<>();
    List<SelectSubQueryBasicElement> minusList = new ArrayList<>();
    List<SelectSubQueryBasicElement> intersectList = new ArrayList<>();
    for (PlSqlParser.Subquery_operation_partContext s1 : ctx.subquery_operation_part()) {
      if (s1.UNION() != null) {
        unionList.add((SelectSubQueryBasicElement) visit(s1.subquery_basic_elements()));
      }
      if (s1.ALL() != null) {
        unionAllList.add((SelectSubQueryBasicElement) visit(s1.subquery_basic_elements()));
      }
      if (s1.MINUS() != null) {
        minusList.add((SelectSubQueryBasicElement) visit(s1.subquery_basic_elements()));
      }
      if (s1.INTERSECT() != null) {
        intersectList.add((SelectSubQueryBasicElement) visit(s1.subquery_basic_elements()));
      }
    }

    return new TableReferenceAuxInternal(
            schema,
            (TableReference) visit(ctx.table_ref()),
            unionList,
            unionAllList,
            minusList,
            intersectList
    );
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
    String schemaName = null;
    String tableName = "?";
    if (ctx.tableview_name() != null) {
      if (ctx.tableview_name().id_expression() != null) {
        schemaName = ctx.tableview_name().identifier().getText();
        tableName = ctx.tableview_name().id_expression().getText();
      } else {
        tableName = ctx.tableview_name().identifier().getText();
      }
    }

    return new TableExpressionClause(
            schemaName,
            tableName
    );
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
    // Parse the insert_into_clause
    String schemaName = null;
    String tableName = null;
    List<String> columnNames = null;
    
    if (ctx.insert_into_clause() != null) {
      // Parse table name from general_table_ref
      if (ctx.insert_into_clause().general_table_ref() != null && 
          ctx.insert_into_clause().general_table_ref().dml_table_expression_clause() != null &&
          ctx.insert_into_clause().general_table_ref().dml_table_expression_clause().tableview_name() != null) {
        
        var tableview = ctx.insert_into_clause().general_table_ref().dml_table_expression_clause().tableview_name();
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
      
      // Parse column list if present
      if (ctx.insert_into_clause().paren_column_list() != null &&
          ctx.insert_into_clause().paren_column_list().column_list() != null &&
          ctx.insert_into_clause().paren_column_list().column_list().column_name() != null) {
        columnNames = new ArrayList<>();
        for (var columnName : ctx.insert_into_clause().paren_column_list().column_list().column_name()) {
          columnNames.add(columnName.getText());
        }
      }
    }
    
    // Parse VALUES clause
    if (ctx.values_clause() != null) {
      List<Expression> values = new ArrayList<>();
      
      // Handle VALUES (expr1, expr2, ...)
      if (ctx.values_clause().expressions_() != null &&
          ctx.values_clause().expressions_().expression() != null) {
        for (var expr : ctx.values_clause().expressions_().expression()) {
          Expression expression = (Expression) visit(expr);
          if (expression != null) {
            values.add(expression);
          }
        }
      }
      
      return new InsertStatement(schemaName, tableName, columnNames, values);
    }
    
    // Parse SELECT statement
    if (ctx.select_statement() != null) {
      SelectStatement selectStatement = (SelectStatement) visit(ctx.select_statement());
      return new InsertStatement(schemaName, tableName, columnNames, selectStatement);
    }
    
    return new Comment("INSERT statement structure not recognized");
  }

  @Override
  public PlSqlAst visitUpdate_statement(PlSqlParser.Update_statementContext ctx) {
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
    
    // Parse SET clauses from update_set_clause
    List<UpdateStatement.UpdateSetClause> setColumns = new ArrayList<>();
    if (ctx.update_set_clause() != null) {
      // Handle column_based_update_set_clause list
      if (ctx.update_set_clause().column_based_update_set_clause() != null) {
        for (var setClause : ctx.update_set_clause().column_based_update_set_clause()) {
          if (setClause.column_name() != null && setClause.expression() != null) {
            String columnName = setClause.column_name().getText();
            Expression value = (Expression) visit(setClause.expression());
            if (value != null) {
              setColumns.add(new UpdateStatement.UpdateSetClause(columnName, value));
            }
          }
        }
      }
    }
    
    // Parse WHERE clause if present
    Expression whereClause = null;
    if (ctx.where_clause() != null && ctx.where_clause().condition() != null) {
      whereClause = (Expression) visit(ctx.where_clause().condition());
    }
    
    return new UpdateStatement(schemaName, tableName, setColumns, whereClause);
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
    List<Parameter> parameters = new ArrayList<>();
    String returnType = ctx.type_spec().getText();

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

    // Extract declarations from DECLARE section
    List<Variable> variables = new ArrayList<>();
    List<CursorDeclaration> cursorDeclarations = new ArrayList<>();
    List<RecordType> recordTypes = new ArrayList<>();
    List<VarrayType> varrayTypes = new ArrayList<>();
    List<NestedTableType> nestedTableTypes = new ArrayList<>();
    if (ctx.seq_of_declare_specs() != null) {
      variables = VisitSeqOfDeclareSpecs.extractVariablesFromDeclareSpecs(
              ctx.seq_of_declare_specs(), this);
      cursorDeclarations = extractCursorDeclarationsFromDeclareSpecs(ctx.seq_of_declare_specs());
      recordTypes = extractRecordTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
      varrayTypes = extractVarrayTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
      nestedTableTypes = extractNestedTableTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
    }
    return new Function(ctx.function_name().getText(), parameters, variables, cursorDeclarations, recordTypes, varrayTypes, nestedTableTypes, returnType, statements, null);
  }

  @Override
  public PlSqlAst visitProc_decl_in_type(PlSqlParser.Proc_decl_in_typeContext ctx) {
    List<Parameter> parameters = new ArrayList<>();
    String procedureName = ctx.procedure_name().getText();

    for (PlSqlParser.Type_elements_parameterContext e : ctx.type_elements_parameter()) {
      parameters.add((Parameter) visit(e));
    }

    // Extract declarations from DECLARE section
    List<Variable> variables = new ArrayList<>();
    List<CursorDeclaration> cursorDeclarations = new ArrayList<>();
    List<RecordType> recordTypes = new ArrayList<>();
    List<VarrayType> varrayTypes = new ArrayList<>();
    List<NestedTableType> nestedTableTypes = new ArrayList<>();
    if (ctx.seq_of_declare_specs() != null) {
      variables = VisitSeqOfDeclareSpecs.extractVariablesFromDeclareSpecs(
              ctx.seq_of_declare_specs(), this);
      cursorDeclarations = extractCursorDeclarationsFromDeclareSpecs(ctx.seq_of_declare_specs());
      recordTypes = extractRecordTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
      varrayTypes = extractVarrayTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
      nestedTableTypes = extractNestedTableTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
    }

    List<Statement> statements = new ArrayList<>();
    if (ctx.body() != null
            && ctx.body().seq_of_statements() != null
            && ctx.body().seq_of_statements().statement() != null) {
      for (PlSqlParser.StatementContext stmt : ctx.body().seq_of_statements().statement()) {
        statements.add((Statement) visit(stmt));
      }
    }

    // Parse exception handling if present
    ExceptionBlock exceptionBlock = null;
    if (ctx.body() != null && ctx.body().exception_handler() != null && !ctx.body().exception_handler().isEmpty()) {
      exceptionBlock = parseExceptionBlock(ctx.body().exception_handler());
    }

    return new Procedure(procedureName, parameters, variables, cursorDeclarations, recordTypes, statements, exceptionBlock);
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
    List<Parameter> parameters = new ArrayList<>();
    String procedureName = ctx.identifier().getText();

    for (PlSqlParser.ParameterContext e : ctx.parameter()) {
      parameters.add((Parameter) visit(e));
    }

    // Extract declarations from DECLARE section
    List<Variable> variables = new ArrayList<>();
    List<CursorDeclaration> cursorDeclarations = new ArrayList<>();
    List<RecordType> recordTypes = new ArrayList<>();
    List<VarrayType> varrayTypes = new ArrayList<>();
    List<NestedTableType> nestedTableTypes = new ArrayList<>();
    if (ctx.seq_of_declare_specs() != null) {
      variables = VisitSeqOfDeclareSpecs.extractVariablesFromDeclareSpecs(
              ctx.seq_of_declare_specs(), this);
      cursorDeclarations = extractCursorDeclarationsFromDeclareSpecs(ctx.seq_of_declare_specs());
      recordTypes = extractRecordTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
      varrayTypes = extractVarrayTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
      nestedTableTypes = extractNestedTableTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
    }

    List<Statement> statements = new ArrayList<>();
    if (ctx.body() != null
            && ctx.body().seq_of_statements() != null
            && ctx.body().seq_of_statements().statement() != null) {
      for (PlSqlParser.StatementContext stmt : ctx.body().seq_of_statements().statement()) {
        statements.add((Statement) visit(stmt));
      }
    }

    // Parse exception handling if present
    ExceptionBlock exceptionBlock = null;
    if (ctx.body() != null && ctx.body().exception_handler() != null && !ctx.body().exception_handler().isEmpty()) {
      exceptionBlock = parseExceptionBlock(ctx.body().exception_handler());
    }

    return new Procedure(procedureName, parameters, variables, cursorDeclarations, recordTypes, statements, exceptionBlock);
  }

  @Override
  public PlSqlAst visitFunction_body(PlSqlParser.Function_bodyContext ctx) {
    List<Parameter> parameters = new ArrayList<>();
    String procedureName = ctx.identifier().getText();
    String returnType = ctx.type_spec().getText(); //TODO?

    for (PlSqlParser.ParameterContext e : ctx.parameter()) {
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

    // Extract declarations from DECLARE section
    List<Variable> variables = new ArrayList<>();
    List<CursorDeclaration> cursorDeclarations = new ArrayList<>();
    List<RecordType> recordTypes = new ArrayList<>();
    List<VarrayType> varrayTypes = new ArrayList<>();
    List<NestedTableType> nestedTableTypes = new ArrayList<>();
    if (ctx.seq_of_declare_specs() != null) {
      variables = VisitSeqOfDeclareSpecs.extractVariablesFromDeclareSpecs(
              ctx.seq_of_declare_specs(), this);
      cursorDeclarations = extractCursorDeclarationsFromDeclareSpecs(ctx.seq_of_declare_specs());
      recordTypes = extractRecordTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
      varrayTypes = extractVarrayTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
      nestedTableTypes = extractNestedTableTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
    }

    // Parse exception block if present
    ExceptionBlock exceptionBlock = null;
    if (ctx.body() != null && ctx.body().exception_handler() != null && !ctx.body().exception_handler().isEmpty()) {
      exceptionBlock = parseExceptionBlock(ctx.body().exception_handler());
    }

    return new Function(procedureName, parameters, variables, cursorDeclarations, recordTypes, varrayTypes, nestedTableTypes, returnType, statements, exceptionBlock);
  }

  /**
   * Helper method to parse exception handlers into an ExceptionBlock
   */
  private ExceptionBlock parseExceptionBlock(List<PlSqlParser.Exception_handlerContext> handlerContexts) {
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
    String packageName = "UNKNOWN";
    if (ctx.package_name() != null && !ctx.package_name().isEmpty()) {
      for (PlSqlParser.Package_nameContext p : ctx.package_name()) {
        packageName = p.getText();
      }
    }

    List<Variable> variables = new ArrayList<>();
    List<RecordType> recordTypes = new ArrayList<>();
    List<VarrayType> varrayTypes = new ArrayList<>();
    List<NestedTableType> nestedTableTypes = new ArrayList<>();
    List<PackageType> packageTypes = new ArrayList<>();

    if (ctx.package_obj_spec() != null) {
      for (var member : ctx.package_obj_spec()) {
        PlSqlAst memberAst = visit(member);
        if (memberAst instanceof Variable) {
          variables.add((Variable) memberAst);
        } else if (memberAst instanceof RecordType) {
          recordTypes.add((RecordType) memberAst);
        } else if (memberAst instanceof VarrayType) {
          varrayTypes.add((VarrayType) memberAst);
        } else if (memberAst instanceof NestedTableType) {
          nestedTableTypes.add((NestedTableType) memberAst);
        } else if (memberAst instanceof PackageType) {
          packageTypes.add((PackageType) memberAst);
        }
      }
    }
    // TODO $if, subtype, cursor, etc.
    return new OraclePackage(packageName, schema, variables, null, null, packageTypes, recordTypes, varrayTypes, nestedTableTypes, null, null, null);
  }

  @Override
  public PlSqlAst visitCreate_package_body(PlSqlParser.Create_package_bodyContext ctx) {
    String packageName = "UNKNOWN";
    if (ctx.package_name() != null && !ctx.package_name().isEmpty()) {
      for (PlSqlParser.Package_nameContext p : ctx.package_name()) {
        packageName = p.getText();
      }
    }
    
    // Store current package context for call resolution
    this.currentPackageName = packageName;
    List<Procedure> procedures = new ArrayList<>();
    List<Function> funcs = new ArrayList<>();
    List<Variable> variables = new ArrayList<>();
    List<RecordType> recordTypes = new ArrayList<>();
    List<VarrayType> varrayTypes = new ArrayList<>();
    List<NestedTableType> nestedTableTypes = new ArrayList<>();
    if (ctx.package_obj_body() != null) {
      for (var member : ctx.package_obj_body()) {
        PlSqlAst memberAst = visit(member);
        if (memberAst instanceof Variable) {
          variables.add((Variable) memberAst);
        } else if (memberAst instanceof RecordType) {
          recordTypes.add((RecordType) memberAst);
        } else if (memberAst instanceof VarrayType) {
          varrayTypes.add((VarrayType) memberAst);
        } else if (memberAst instanceof NestedTableType) {
          nestedTableTypes.add((NestedTableType) memberAst);
        } else if (memberAst instanceof Function) {
          funcs.add((Function) memberAst);
        } else if (memberAst instanceof Procedure) {
          procedures.add((Procedure) memberAst);
        }
      }
    }
    OraclePackage o = new OraclePackage(packageName, schema, variables, null, null, null, recordTypes, varrayTypes, nestedTableTypes, funcs, procedures, null);
    o.getFunctions().forEach(e -> e.setParentPackage(o));
    o.getProcedures().forEach(e -> e.setParentPackage(o));
    return o;
  }

  @Override
  public PlSqlAst visitCreate_function_body(PlSqlParser.Create_function_bodyContext ctx) {
    // Parse standalone function
    String functionName = ctx.function_name() != null ? ctx.function_name().getText() : "UNKNOWN";
    String returnType = ctx.type_spec() != null ? ctx.type_spec().getText() : "UNKNOWN";
    
    List<Parameter> parameters = new ArrayList<>();
    if (ctx.parameter() != null) {
      for (PlSqlParser.ParameterContext param : ctx.parameter()) {
        parameters.add((Parameter) visit(param));
      }
    }
    
    List<Statement> statements = new ArrayList<>();
    if (ctx.body() != null && ctx.body().seq_of_statements() != null && ctx.body().seq_of_statements().statement() != null) {
      for (PlSqlParser.StatementContext stmt : ctx.body().seq_of_statements().statement()) {
        statements.add((Statement) visit(stmt));
      }
    }

    // Extract declarations from DECLARE section
    List<Variable> variables = new ArrayList<>();
    List<CursorDeclaration> cursorDeclarations = new ArrayList<>();
    List<RecordType> recordTypes = new ArrayList<>();
    List<VarrayType> varrayTypes = new ArrayList<>();
    List<NestedTableType> nestedTableTypes = new ArrayList<>();
    if (ctx.seq_of_declare_specs() != null) {
      variables = VisitSeqOfDeclareSpecs.extractVariablesFromDeclareSpecs(
              ctx.seq_of_declare_specs(), this);
      cursorDeclarations = extractCursorDeclarationsFromDeclareSpecs(ctx.seq_of_declare_specs());
      recordTypes = extractRecordTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
      varrayTypes = extractVarrayTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
      nestedTableTypes = extractNestedTableTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
    }
    
    Function function = new Function(functionName, parameters, variables, cursorDeclarations, recordTypes, varrayTypes, nestedTableTypes, returnType, statements, null);
    function.setStandalone(true);
    function.setSchema(schema);
    return function;
  }

  @Override
  public PlSqlAst visitCreate_procedure_body(PlSqlParser.Create_procedure_bodyContext ctx) {
    // Parse standalone procedure
    String procedureName = ctx.procedure_name() != null ? ctx.procedure_name().getText() : "UNKNOWN";
    
    List<Parameter> parameters = new ArrayList<>();
    if (ctx.parameter() != null) {
      for (PlSqlParser.ParameterContext param : ctx.parameter()) {
        parameters.add((Parameter) visit(param));
      }
    }
    
    // Extract declarations from DECLARE section
    List<Variable> variables = new ArrayList<>();
    List<CursorDeclaration> cursorDeclarations = new ArrayList<>();
    List<RecordType> recordTypes = new ArrayList<>();
    List<VarrayType> varrayTypes = new ArrayList<>();
    List<NestedTableType> nestedTableTypes = new ArrayList<>();
    if (ctx.seq_of_declare_specs() != null) {
      variables = VisitSeqOfDeclareSpecs.extractVariablesFromDeclareSpecs(
              ctx.seq_of_declare_specs(), this);
      cursorDeclarations = extractCursorDeclarationsFromDeclareSpecs(ctx.seq_of_declare_specs());
      recordTypes = extractRecordTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
      varrayTypes = extractVarrayTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
      nestedTableTypes = extractNestedTableTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
    }
    
    List<Statement> statements = new ArrayList<>();
    if (ctx.body() != null && ctx.body().seq_of_statements() != null && ctx.body().seq_of_statements().statement() != null) {
      for (PlSqlParser.StatementContext stmt : ctx.body().seq_of_statements().statement()) {
        statements.add((Statement) visit(stmt));
      }
    }
    
    Procedure procedure = new Procedure(procedureName, parameters, variables, cursorDeclarations, recordTypes, statements, null);
    procedure.setStandalone(true);
    procedure.setSchema(schema);
    return procedure;
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
    // Handle collection method calls: unary_expression '.' (COUNT | FIRST | LAST | etc.)
    if (ctx.unary_expression() != null) {
      // Check for collection methods without arguments
      if (ctx.COUNT() != null || ctx.FIRST() != null || ctx.LAST() != null || ctx.LIMIT() != null) {
        // Parse the base expression
        PlSqlAst baseExpressionAst = visit(ctx.unary_expression());
        if (baseExpressionAst instanceof UnaryExpression) {
          String methodName = null;
          if (ctx.COUNT() != null) methodName = "COUNT";
          else if (ctx.FIRST() != null) methodName = "FIRST";
          else if (ctx.LAST() != null) methodName = "LAST";
          else if (ctx.LIMIT() != null) methodName = "LIMIT";
          
          return new UnaryExpression((UnaryExpression) baseExpressionAst, methodName, null);
        }
      }
      
      // Check for collection methods with arguments: EXISTS, NEXT, PRIOR
      if (ctx.EXISTS() != null || ctx.NEXT() != null || ctx.PRIOR() != null) {
        PlSqlAst baseExpressionAst = visit(ctx.unary_expression());
        if (baseExpressionAst instanceof UnaryExpression) {
          String methodName = null;
          if (ctx.EXISTS() != null) methodName = "EXISTS";
          else if (ctx.NEXT() != null) methodName = "NEXT";
          else if (ctx.PRIOR() != null) methodName = "PRIOR";
          
          // Parse method arguments
          List<Expression> methodArguments = new ArrayList<>();
          if (ctx.index != null) {
            for (var exprCtx : ctx.index) {
              PlSqlAst argAst = visit(exprCtx);
              if (argAst instanceof Expression) {
                methodArguments.add((Expression) argAst);
              }
            }
          }
          
          return new UnaryExpression((UnaryExpression) baseExpressionAst, methodName, methodArguments);
        }
      }
    }
    
    if (ctx.standard_function() != null) {
      PlSqlAst standardFunctionAst = visit(ctx.standard_function());
      if (standardFunctionAst instanceof Expression) {
        return UnaryExpression.forStandardFunction((Expression) standardFunctionAst);
      }
      // Fallback
      return visitChildren(ctx);
    }
    
    if (ctx.atom() != null) {
      // Check if the atom contains a collection method call before visiting
      UnaryExpression collectionMethodCall = checkAtomForCollectionMethod(ctx.atom());
      if (collectionMethodCall != null) {
        return collectionMethodCall;
      }
      
      PlSqlAst atomAst = visit(ctx.atom());
      if (atomAst instanceof Expression) {
        return UnaryExpression.forAtom((Expression) atomAst);
      }
      // Fallback
      return visitChildren(ctx);
    }
    
    // For other types of unary expressions, fall back to default behavior
    return visitChildren(ctx);
  }

  /**
   * Check if an atom contains a collection method call (e.g., v_arr.COUNT, v_arr.FIRST)
   * or array indexing (e.g., v_arr(i)).
   * This handles cases where these expressions are parsed through the general_element path
   * instead of the specific unary_expression dot notation rule.
   */
  private UnaryExpression checkAtomForCollectionMethod(PlSqlParser.AtomContext atomCtx) {
    // Check if atom contains a general_element with dot notation (collection methods)
    if (atomCtx.general_element() != null) {
      // First check for collection constructors (e.g., string_array('a', 'b'))
      UnaryExpression collectionConstructor = checkGeneralElementForCollectionConstructor(atomCtx.general_element());
      if (collectionConstructor != null) {
        return collectionConstructor;
      }
      
      // Then check for collection methods
      UnaryExpression collectionMethod = checkGeneralElementForCollectionMethod(atomCtx.general_element());
      if (collectionMethod != null) {
        return collectionMethod;
      }
      
      // Finally check for array indexing
      UnaryExpression arrayIndexing = checkGeneralElementForArrayIndexing(atomCtx.general_element());
      if (arrayIndexing != null) {
        return arrayIndexing;
      }
    }
    return null;
  }

  /**
   * Check if a general_element represents a collection method call.
   * Looks for patterns like: variable.COUNT, variable.FIRST, variable.LAST, etc.
   */
  private UnaryExpression checkGeneralElementForCollectionMethod(PlSqlParser.General_elementContext generalElementCtx) {
    // Check for the pattern: general_element ('.' general_element_part)+
    if (generalElementCtx.general_element() != null && 
        generalElementCtx.general_element_part() != null && 
        !generalElementCtx.general_element_part().isEmpty()) {
      
      // Get the base expression (the variable part)
      PlSqlParser.General_elementContext baseElement = generalElementCtx.general_element();
      
      // Check each dot notation part for collection methods
      for (PlSqlParser.General_element_partContext partCtx : generalElementCtx.general_element_part()) {
        String methodName = extractCollectionMethodName(partCtx);
        if (methodName != null) {
          // Create a simple text-based expression for the base element
          String variableName = baseElement.getText();
          
          // Create a LogicalExpression that wraps this variable reference
          LogicalExpression logicalExpr = createLogicalExpressionFromText(variableName);
          Expression baseExpression = new Expression(logicalExpr);
          UnaryExpression baseUnaryExpression = UnaryExpression.forAtom(baseExpression);
          
          // Check if it's a method with arguments (like EXISTS, NEXT, PRIOR)
          List<Expression> methodArguments = extractMethodArguments(partCtx);
          
          return new UnaryExpression(baseUnaryExpression, methodName, methodArguments);
        }
      }
    }
    return null;
  }

  /**
   * Extract collection method name from a general_element_part if it's a collection method.
   * Returns null if it's not a recognized collection method.
   */
  private String extractCollectionMethodName(PlSqlParser.General_element_partContext partCtx) {
    if (partCtx.id_expression() != null) {
      String methodName = partCtx.id_expression().getText().toUpperCase();
      switch (methodName) {
        case "COUNT":
        case "FIRST":
        case "LAST":
        case "LIMIT":
        case "EXISTS":
        case "NEXT":
        case "PRIOR":
          return methodName;
        default:
          return null;
      }
    }
    return null;
  }

  /**
   * Extract method arguments from a general_element_part if it has function_argument.
   * Used for methods like EXISTS(index), NEXT(index), PRIOR(index).
   */
  private List<Expression> extractMethodArguments(PlSqlParser.General_element_partContext partCtx) {
    List<Expression> arguments = new ArrayList<>();
    
    if (partCtx.function_argument() != null && !partCtx.function_argument().isEmpty()) {
      for (PlSqlParser.Function_argumentContext argCtx : partCtx.function_argument()) {
        if (argCtx.argument() != null && !argCtx.argument().isEmpty()) {
          for (PlSqlParser.ArgumentContext arg : argCtx.argument()) {
            if (arg.expression() != null) {
              PlSqlAst argAst = visit(arg.expression());
              if (argAst instanceof Expression) {
                arguments.add((Expression) argAst);
              }
            }
          }
        }
      }
    }
    
    return arguments.isEmpty() ? null : arguments;
  }

  /**
   * Create a simple LogicalExpression from text.
   * This is a helper method to wrap variable names in the Expression hierarchy.
   */
  private LogicalExpression createLogicalExpressionFromText(String text) {
    // Create a UnaryLogicalExpression with the text, then wrap it in LogicalExpression
    UnaryLogicalExpression unaryLogicalExpr = new UnaryLogicalExpression(text);
    return new LogicalExpression(unaryLogicalExpr);
  }

  /**
   * Check if a general_element represents array indexing (e.g., v_arr(i)).
   * This uses the Everything metadata to distinguish between function calls and array indexing.
   */
  private UnaryExpression checkGeneralElementForArrayIndexing(PlSqlParser.General_elementContext generalElementCtx) {
    // Check for the pattern: general_element_part with function_argument (parentheses syntax)
    if (generalElementCtx.general_element_part() != null && 
        !generalElementCtx.general_element_part().isEmpty()) {
      
      // We need a simple identifier (not dot notation) with function arguments
      if (generalElementCtx.general_element() == null) {
        // This is a simple identifier with parentheses: identifier(args)
        PlSqlParser.General_element_partContext partCtx = generalElementCtx.general_element_part().get(0);
        
        if (partCtx.id_expression() != null && 
            partCtx.function_argument() != null && 
            !partCtx.function_argument().isEmpty()) {
          
          String identifier = partCtx.id_expression().getText();
          
          // Use Everything.isKnownFunction to determine if this is a function or variable
          // For now, we'll need access to Everything and current function context
          // This will be passed from the calling context
          
          // Extract the first argument as the index expression
          List<Expression> arguments = extractMethodArguments(partCtx);
          if (arguments != null && !arguments.isEmpty()) {
            Expression indexExpression = arguments.get(0);
            
            // TODO: We need to check Everything.isKnownFunction here
            // For now, assume it's array indexing if we can't determine otherwise
            // This check will be enhanced when we have the full context
            
            return new UnaryExpression(identifier, indexExpression);
          }
        }
      }
    }
    return null;
  }

  /**
   * Check if a general_element represents a collection constructor call.
   * Looks for patterns like: string_array('a', 'b'), number_table(1, 2, 3), etc.
   * This needs to distinguish between function calls and collection constructors by checking
   * if the identifier matches a known collection type name.
   */
  private UnaryExpression checkGeneralElementForCollectionConstructor(PlSqlParser.General_elementContext generalElementCtx) {
    // Check for the pattern: general_element_part with function_argument (parentheses syntax)
    if (generalElementCtx.general_element_part() != null && 
        !generalElementCtx.general_element_part().isEmpty()) {
      
      // We need a simple identifier (not dot notation) with function arguments
      if (generalElementCtx.general_element() == null) {
        // This is a simple identifier with parentheses: identifier(args)
        PlSqlParser.General_element_partContext partCtx = generalElementCtx.general_element_part().get(0);
        
        if (partCtx.id_expression() != null && 
            partCtx.function_argument() != null) {
          
          String identifier = partCtx.id_expression().getText();
          
          // Check if this identifier is a collection type name
          // We'll do this by checking if it ends with known collection type patterns
          // and later enhance with Everything context for precise type checking
          if (isLikelyCollectionConstructor(identifier)) {
            // Extract constructor arguments
            List<Expression> arguments = extractMethodArguments(partCtx);
            
            // Create a collection constructor expression
            return new UnaryExpression(identifier, arguments);
          }
        }
      }
    }
    return null;
  }

  /**
   * Check if an identifier is likely a collection constructor based on naming patterns.
   * This is a heuristic check that can be enhanced with full type context later.
   */
  private boolean isLikelyCollectionConstructor(String identifier) {
    if (identifier == null) return false;
    
    String lowerIdentifier = identifier.toLowerCase();
    
    // Common Oracle collection type naming patterns
    return lowerIdentifier.endsWith("_array") || 
           lowerIdentifier.endsWith("_table") ||
           lowerIdentifier.endsWith("_list") ||
           lowerIdentifier.endsWith("_varray") ||
           lowerIdentifier.contains("array") ||
           lowerIdentifier.contains("table");
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
    // Check if this is a cursor attribute (cursor_name %FOUND, %NOTFOUND, etc.)
    if (ctx.cursor_name() != null) {
      String cursorName = ctx.cursor_name().getText();
      
      CursorAttributeExpression.CursorAttributeType attributeType = null;
      
      if (ctx.PERCENT_FOUND() != null) {
        attributeType = CursorAttributeExpression.CursorAttributeType.FOUND;
      } else if (ctx.PERCENT_NOTFOUND() != null) {
        attributeType = CursorAttributeExpression.CursorAttributeType.NOTFOUND;
      } else if (ctx.PERCENT_ROWCOUNT() != null) {
        attributeType = CursorAttributeExpression.CursorAttributeType.ROWCOUNT;
      } else if (ctx.PERCENT_ISOPEN() != null) {
        attributeType = CursorAttributeExpression.CursorAttributeType.ISOPEN;
      }
      
      if (attributeType != null) {
        // Create a CursorAttributeExpression and wrap it in UnaryExpression
        CursorAttributeExpression cursorAttr = new CursorAttributeExpression(cursorName, attributeType);
        
        // Create an Expression that delegates to our cursor attribute
        LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression("CURSOR_ATTR_PLACEHOLDER"));
        return new Expression(logicalExpr) {
          @Override
          public String toPostgre(me.christianrobert.ora2postgre.global.Everything data) {
            return cursorAttr.toPostgre(data);
          }
          
          @Override
          public String toString() {
            return cursorAttr.toString();
          }
        };
      }
    }
    
    // Check for analytical functions: over_clause_keyword function_argument_analytic over_clause?
    if (ctx.over_clause_keyword() != null && ctx.over_clause() != null) {
      return parseAnalyticalFunction(ctx);
    }
    
    // Check for within_or_over functions: within_or_over_clause_keyword function_argument within_or_over_part+
    if (ctx.within_or_over_clause_keyword() != null && !ctx.within_or_over_part().isEmpty()) {
      // Check if any of the within_or_over_part elements contain an over_clause
      for (PlSqlParser.Within_or_over_partContext partCtx : ctx.within_or_over_part()) {
        if (partCtx.over_clause() != null) {
          return parseAnalyticalFunctionFromWithinOrOver(ctx);
        }
      }
    }
    
    // For other function types, fall back to default behavior
    return visitChildren(ctx);
  }

  /**
   * Parse analytical function from over_clause_keyword function_argument_analytic over_clause pattern.
   */
  private AnalyticalFunction parseAnalyticalFunction(PlSqlParser.Other_functionContext ctx) {
    // Determine function type from over_clause_keyword
    AnalyticalFunction.AnalyticalFunctionType functionType = 
        parseAnalyticalFunctionType(ctx.over_clause_keyword().getText());
    
    // Parse arguments (if any)
    java.util.List<Expression> arguments = new java.util.ArrayList<>();
    if (ctx.function_argument_analytic() != null) {
      arguments = parseAnalyticalFunctionArguments(ctx.function_argument_analytic());
    }
    
    // Parse OVER clause
    OverClause overClause = null;
    if (ctx.over_clause() != null) {
      overClause = (OverClause) visit(ctx.over_clause());
    }
    
    return new AnalyticalFunction(functionType, arguments, overClause);
  }

  /**
   * Parse analytical function from within_or_over_clause_keyword pattern.
   */
  private AnalyticalFunction parseAnalyticalFunctionFromWithinOrOver(PlSqlParser.Other_functionContext ctx) {
    // Determine function type from within_or_over_clause_keyword
    AnalyticalFunction.AnalyticalFunctionType functionType = 
        parseAnalyticalFunctionTypeFromWithinOrOver(ctx.within_or_over_clause_keyword().getText());
    
    // Parse arguments
    java.util.List<Expression> arguments = new java.util.ArrayList<>();
    if (ctx.function_argument() != null) {
      // Parse standard function arguments
      arguments = parseStandardFunctionArguments(ctx.function_argument());
    }
    
    // Find the OVER clause from within_or_over_part
    OverClause overClause = null;
    for (PlSqlParser.Within_or_over_partContext partCtx : ctx.within_or_over_part()) {
      if (partCtx.over_clause() != null) {
        overClause = (OverClause) visit(partCtx.over_clause());
        break;
      }
    }
    
    return new AnalyticalFunction(functionType, arguments, overClause);
  }

  /**
   * Map over_clause_keyword to AnalyticalFunctionType.
   */
  private AnalyticalFunction.AnalyticalFunctionType parseAnalyticalFunctionType(String keyword) {
    switch (keyword.toUpperCase()) {
      case "ROW_NUMBER":
        return AnalyticalFunction.AnalyticalFunctionType.ROW_NUMBER;
      case "AVG":
        return AnalyticalFunction.AnalyticalFunctionType.AVG;
      case "MAX":
        return AnalyticalFunction.AnalyticalFunctionType.MAX;
      case "MIN":
        return AnalyticalFunction.AnalyticalFunctionType.MIN;
      case "SUM":
        return AnalyticalFunction.AnalyticalFunctionType.SUM;
      case "COUNT":
        return AnalyticalFunction.AnalyticalFunctionType.COUNT;
      case "NTILE":
        return AnalyticalFunction.AnalyticalFunctionType.NTILE;
      default:
        // For unknown functions, default to a safe option or throw exception
        return AnalyticalFunction.AnalyticalFunctionType.ROW_NUMBER;
    }
  }

  /**
   * Map within_or_over_clause_keyword to AnalyticalFunctionType.
   */
  private AnalyticalFunction.AnalyticalFunctionType parseAnalyticalFunctionTypeFromWithinOrOver(String keyword) {
    switch (keyword.toUpperCase()) {
      case "RANK":
        return AnalyticalFunction.AnalyticalFunctionType.RANK;
      case "DENSE_RANK":
        return AnalyticalFunction.AnalyticalFunctionType.DENSE_RANK;
      case "PERCENT_RANK":
        return AnalyticalFunction.AnalyticalFunctionType.PERCENT_RANK;
      case "CUME_DIST":
        return AnalyticalFunction.AnalyticalFunctionType.CUME_DIST;
      default:
        return AnalyticalFunction.AnalyticalFunctionType.RANK;
    }
  }

  /**
   * Parse analytical function arguments.
   */
  private java.util.List<Expression> parseAnalyticalFunctionArguments(PlSqlParser.Function_argument_analyticContext ctx) {
    java.util.List<Expression> arguments = new java.util.ArrayList<>();
    
    // For most analytical functions, arguments are optional or handled differently
    // This is a placeholder for future argument parsing if needed
    
    return arguments;
  }

  /**
   * Parse standard function arguments.
   */
  private java.util.List<Expression> parseStandardFunctionArguments(PlSqlParser.Function_argumentContext ctx) {
    java.util.List<Expression> arguments = new java.util.ArrayList<>();
    
    // This is a placeholder for standard function argument parsing
    // The exact implementation depends on the function_argument grammar structure
    
    return arguments;
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