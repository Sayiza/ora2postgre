package me.christianrobert.ora2postgre.plsql;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.antlr.PlSqlParserBaseVisitor;
import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;
import me.christianrobert.ora2postgre.oracledb.tools.NameNormalizer;

import java.util.ArrayList;
import java.util.List;

public class PlSqlAstBuilder extends PlSqlParserBaseVisitor<PlSqlAst> {
  private final String schema;

  public PlSqlAstBuilder(String schema) {
    this.schema = schema;
  }

  @Override
  public PlSqlAst visitSql_script(PlSqlParser.Sql_scriptContext ctx) {
    if (ctx.unit_statement() != null) {
      for (var unit : ctx.unit_statement()) {
        PlSqlAst ast = visit(unit);
        if (ast != null) {
          return ast; // Return first non-null AST
        }
      }
    }
    return null;
  }

  @Override
  public PlSqlAst visitVariable_declaration(PlSqlParser.Variable_declarationContext ctx) {
    String varName = ctx.identifier().id_expression().getText();
    DataTypeSpec dataType = (DataTypeSpec) visit(ctx.type_spec());
    Expression defaultValue = ctx.default_value_part() != null ? (Expression) visit(ctx.default_value_part()) : null;
    return new Variable(varName, dataType, defaultValue);
  }

  @Override
  public PlSqlAst visitCursor_declaration(PlSqlParser.Cursor_declarationContext ctx) {
    String cursorName = ctx.identifier().getText();
    
    // Parse parameters if present
    List<Parameter> parameters = new ArrayList<>();
    if (ctx.parameter_spec() != null) {
      for (PlSqlParser.Parameter_specContext paramCtx : ctx.parameter_spec()) {
        // Extract parameter name and type
        String paramName = paramCtx.parameter_name().getText();
        String paramType = paramCtx.type_spec() != null ? paramCtx.type_spec().getText() : "VARCHAR2";
        
        // Create parameter (assuming IN direction for cursor parameters)
        Parameter param = new Parameter(paramName, new DataTypeSpec(paramType, null, null, null), null, true, false);
        parameters.add(param);
      }
    }
    
    // Parse return type if present
    String returnType = null;
    if (ctx.type_spec() != null) {
      returnType = ctx.type_spec().getText();
    }
    
    // Parse SELECT statement if present
    SelectStatement selectStatement = null;
    if (ctx.select_statement() != null) {
      PlSqlAst selectAst = visit(ctx.select_statement());
      if (selectAst instanceof SelectStatement) {
        selectStatement = (SelectStatement) selectAst;
      }
    }
    
    return new CursorDeclaration(cursorName, parameters, returnType, selectStatement);
  }

  /**
   * Extracts variables from seq_of_declare_specs context.
   * Returns a list of Variable objects found in the DECLARE section.
   */
  public List<Variable> extractVariablesFromDeclareSpecs(PlSqlParser.Seq_of_declare_specsContext ctx) {
    List<Variable> variables = new ArrayList<>();
    
    if (ctx != null && ctx.declare_spec() != null) {
      for (PlSqlParser.Declare_specContext declareSpec : ctx.declare_spec()) {
        if (declareSpec.variable_declaration() != null) {
          Variable variable = (Variable) visit(declareSpec.variable_declaration());
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

  @Override
  public PlSqlAst visitSeq_of_declare_specs(PlSqlParser.Seq_of_declare_specsContext ctx) {
    // This method is called when seq_of_declare_specs is visited directly
    // Usually we want to extract variables using the helper method above
    List<Variable> variables = extractVariablesFromDeclareSpecs(ctx);
    return new Comment("declare_specs with " + variables.size() + " variables");
  }

  @Override
  public PlSqlAst visitType_spec(PlSqlParser.Type_specContext ctx) {
    String nativeDataType = null;
    if (ctx.datatype() != null && ctx.datatype().native_datatype_element() != null ) {
      nativeDataType = ctx.datatype().native_datatype_element().getText();
    }
    
    StringBuilder b = new StringBuilder();
    if (ctx.type_name() != null && ctx.type_name().id_expression() != null) {
      List<PlSqlParser.Id_expressionContext> idExpression = ctx.type_name().id_expression();
      for (int j = 0; j < idExpression.size(); j++) {
        PlSqlParser.Id_expressionContext i = idExpression.get(j);
        b.append(i.getText());
        if (j < idExpression.size() - 1) {
          b.append(".");
        }
      }
    }

    // Handle %ROWTYPE attributes with enhanced support
    if (ctx.PERCENT_ROWTYPE() != null) {
      String[] parts = b.toString().split("\\.");
      if (parts.length == 2) {
        // schema.table%ROWTYPE
        return RecordTypeSpec.forRowType(parts[0], parts[1]);
      } else if (parts.length == 1) {
        // table%ROWTYPE (use current schema)
        return RecordTypeSpec.forRowType(schema, parts[0]);
      }
    }
    
    // Handle %TYPE attributes with enhanced support
    if (ctx.PERCENT_TYPE() != null) {
      String[] parts = b.toString().split("\\.");
      if (parts.length == 3) {
        // schema.table.column%TYPE
        return RecordTypeSpec.forColumnType(parts[0], parts[1], parts[2]);
      } else if (parts.length == 2) {
        // table.column%TYPE (use current schema)
        return RecordTypeSpec.forColumnType(schema, parts[0], parts[1]);
      }
    }

    // Fall back to standard DataTypeSpec for native types and custom types
    return new DataTypeSpec(
            nativeDataType,
            ctx.PERCENT_ROWTYPE() == null && ctx.PERCENT_TYPE() == null ? b.toString() : null,
            ctx.PERCENT_ROWTYPE() != null ? b.toString() : null,
            ctx.PERCENT_TYPE() != null ? b.toString() : null
    );
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
    String cursorName = ctx.cursor_name().getText();
    
    // Parse INTO variables
    List<String> intoVariables = new ArrayList<>();
    if (ctx.variable_or_collection() != null) {
      for (PlSqlParser.Variable_or_collectionContext varCtx : ctx.variable_or_collection()) {
        // Extract variable name from variable_or_collection context
        String varName = varCtx.getText(); // Simple approach - could be enhanced for complex expressions
        intoVariables.add(varName);
      }
    }
    
    return new FetchStatement(cursorName, intoVariables);
  }

  @Override
  public PlSqlAst visitClose_statement(PlSqlParser.Close_statementContext ctx) {
    String cursorName = ctx.cursor_name().getText();
    return new CloseStatement(cursorName);
  }

  @Override
  public PlSqlAst visitCall_statement(PlSqlParser.Call_statementContext ctx) {
    if (ctx.routine_name(0) != null
            && ctx.routine_name(0).getText().contains("htp.p")
            && ctx.function_argument(0) != null
            && ctx.function_argument(0).getChild(1) != null) {
      return new HtpStatement(ctx.function_argument(0).getChild(1).getText());
      // TODO make this more clean
    }
    return new Comment("a callstatement ...");
  }

  @Override
  public PlSqlAst visitReturn_statement(PlSqlParser.Return_statementContext ctx) {
    // TODO process expression child
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
    List<Statement> statements = new ArrayList<>();
    
    // Handle FOR loops
    if (ctx.FOR() != null) {
      if (ctx.cursor_loop_param().record_name() != null
              && ctx.cursor_loop_param().select_statement() != null) {
        if (ctx.seq_of_statements() != null
                && ctx.seq_of_statements().statement() != null) {
          for (PlSqlParser.StatementContext stmt : ctx.seq_of_statements().statement()) {
            statements.add((Statement) visit(stmt));
          }
        }
        String nameRef = ctx.cursor_loop_param().record_name().getText();
        ctx.cursor_loop_param().record_name().getText();
        SelectStatement sel = (SelectStatement) visit(ctx.cursor_loop_param().select_statement());
        return new ForLoopStatement(
                schema,
                nameRef,
                sel,
                statements
        );
      }
    }
    
    // Handle WHILE loops
    if (ctx.WHILE() != null) {
      // Parse WHILE condition
      Expression condition = (Expression) visit(ctx.condition());
      
      // Parse loop body statements
      if (ctx.seq_of_statements() != null && ctx.seq_of_statements().statement() != null) {
        for (PlSqlParser.StatementContext stmt : ctx.seq_of_statements().statement()) {
          Statement statement = (Statement) visit(stmt);
          if (statement != null) {
            statements.add(statement);
          }
        }
      }
      
      return new WhileLoopStatement(condition, statements);
    }
    
    // Handle plain LOOP...END LOOP (no WHILE or FOR)
    // Parse loop body statements
    if (ctx.seq_of_statements() != null && ctx.seq_of_statements().statement() != null) {
      for (PlSqlParser.StatementContext stmt : ctx.seq_of_statements().statement()) {
        Statement statement = (Statement) visit(stmt);
        if (statement != null) {
          statements.add(statement);
        }
      }
    }
    
    return new LoopStatement(statements);
  }

  @Override
  public PlSqlAst visitSelect_statement(PlSqlParser.Select_statementContext ctx) {
    // Check if this is a SELECT INTO statement by looking for into_clause in query_block
    if (ctx.select_only_statement() != null && 
        ctx.select_only_statement().subquery() != null &&
        ctx.select_only_statement().subquery().subquery_basic_elements() != null &&
        ctx.select_only_statement().subquery().subquery_basic_elements().query_block() != null &&
        ctx.select_only_statement().subquery().subquery_basic_elements().query_block().into_clause() != null) {
      
      // This is a SELECT INTO statement - route to SelectIntoStatement
      return visitSelectIntoFromQueryBlock(ctx.select_only_statement().subquery().subquery_basic_elements().query_block());
    }
    
    // Regular SELECT statement - use existing logic
    return new SelectStatement(
            schema, // TODO only selectOnlyClause for now, to for,order etc later
            (SelectSubQuery) visit(ctx.select_only_statement().subquery()),
            ctx.select_only_statement().with_clause() != null ?
                    (SelectWithClause) visit(ctx.select_only_statement().with_clause()) :
                    null,
            ctx.for_update_clause() != null && !ctx.for_update_clause().isEmpty() ?
                    (SelectForUpdateClause) visit(ctx.for_update_clause(0)) :
                    null,
            ctx.order_by_clause() != null && !ctx.order_by_clause().isEmpty() ?
                    (SelectOrderByClause) visit(ctx.order_by_clause(0)) :
                    null,
            ctx.offset_clause() != null && !ctx.offset_clause().isEmpty() ?
                    (SelectOffsetClause) visit(ctx.offset_clause(0)) :
                    null,
            ctx.fetch_clause() != null && !ctx.offset_clause().isEmpty() ?
                    (SelectFetchClause) visit(ctx.offset_clause(0)) :
                    null
    );
  }

  /**
   * Helper method to parse SELECT INTO statements from query_block context
   */
  private PlSqlAst visitSelectIntoFromQueryBlock(PlSqlParser.Query_blockContext ctx) {
    // Parse selected columns
    List<String> selectedColumns = new ArrayList<>();
    if (ctx.selected_list().ASTERISK() != null) {
      selectedColumns.add("*");
    } else {
      for (var selectElement : ctx.selected_list().select_list_elements()) {
        // For simplicity, just get the text of each select element
        // TODO: This could be enhanced to handle complex expressions
        selectedColumns.add(selectElement.getText());
      }
    }
    
    // Parse INTO variables
    List<String> intoVariables = new ArrayList<>();
    if (ctx.into_clause() != null) {
      for (var element : ctx.into_clause().general_element()) {
        intoVariables.add(element.getText());
      }
      // Also handle bind_variable if present
      if (ctx.into_clause().bind_variable() != null) {
        for (var bindVar : ctx.into_clause().bind_variable()) {
          intoVariables.add(bindVar.getText());
        }
      }
    }
    
    // Parse FROM table (simplified approach)
    String schemaName = null;
    String tableName = null;
    
    if (ctx.from_clause() != null &&
        ctx.from_clause().table_ref_list() != null &&
        ctx.from_clause().table_ref_list().table_ref() != null &&
        !ctx.from_clause().table_ref_list().table_ref().isEmpty()) {
      
      // For simplicity, just get the text of the first table reference
      // TODO: This could be enhanced to properly parse complex table expressions
      var firstTableRef = ctx.from_clause().table_ref_list().table_ref().get(0);
      String tableRefText = firstTableRef.getText();
      
      // Simple parsing: check if it contains a dot (schema.table)
      if (tableRefText.contains(".")) {
        String[] parts = tableRefText.split("\\.", 2);
        schemaName = parts[0];
        tableName = parts[1];
      } else {
        tableName = tableRefText;
      }
    }
    
    // Parse WHERE clause if present
    Expression whereClause = null;
    if (ctx.where_clause() != null && ctx.where_clause().condition() != null) {
      whereClause = (Expression) visit(ctx.where_clause().condition());
    }
    
    return new SelectIntoStatement(selectedColumns, intoVariables, schemaName, tableName, whereClause);
  }

  @Override
  public PlSqlAst visitSubquery(PlSqlParser.SubqueryContext ctx) {
    SelectSubQueryBasicElement subQueryBasicElement =
            (SelectSubQueryBasicElement) visit(ctx.subquery_basic_elements());
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

    return new SelectSubQuery(
            schema,
            subQueryBasicElement,
            unionList,
            unionAllList,
            minusList,
            intersectList
    );
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

  @Override
  public PlSqlAst visitSelect_list_elements(PlSqlParser.Select_list_elementsContext ctx) {

    //String columnName = null;
    //String columnPrefix = null;
    String tableNameB4asterisk = null;
    String schemaNameB4asterisk = null;
    if (ctx.tableview_name() != null) {
      if (ctx.tableview_name().id_expression() != null) {
        tableNameB4asterisk = ctx.tableview_name().id_expression().getText();
        schemaNameB4asterisk = ctx.tableview_name().identifier().getText();
      }
    }

    // TODO expression is INSANELY complicated .. do this later

    // expression can either be a cursor expression or an arbitrary logical expression
    // infering the datatype from table is done in the toJava or toPostgre step!
    return new SelectListElement(
                        schema,
            ctx.column_alias() != null ? ctx.column_alias().getText() : null,
            ctx.expression() != null ? (Expression) visit(ctx.expression()) : null,
            ctx.ASTERISK() != null,
            tableNameB4asterisk,
            schemaNameB4asterisk
    );
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
    // Parse the main IF condition
    Expression condition = (Expression) visit(ctx.condition());
    
    // Parse THEN statements
    List<Statement> thenStatements = new ArrayList<>();
    if (ctx.seq_of_statements() != null && ctx.seq_of_statements().statement() != null) {
      for (PlSqlParser.StatementContext stmt : ctx.seq_of_statements().statement()) {
        Statement statement = (Statement) visit(stmt);
        if (statement != null) {
          thenStatements.add(statement);
        }
      }
    }
    
    // Parse ELSIF parts
    List<IfStatement.ElsifPart> elsifParts = null;
    if (ctx.elsif_part() != null && !ctx.elsif_part().isEmpty()) {
      elsifParts = new ArrayList<>();
      for (PlSqlParser.Elsif_partContext elsif : ctx.elsif_part()) {
        Expression elsifCondition = (Expression) visit(elsif.condition());
        List<Statement> elsifStatements = new ArrayList<>();
        if (elsif.seq_of_statements() != null && elsif.seq_of_statements().statement() != null) {
          for (PlSqlParser.StatementContext stmt : elsif.seq_of_statements().statement()) {
            Statement statement = (Statement) visit(stmt);
            if (statement != null) {
              elsifStatements.add(statement);
            }
          }
        }
        elsifParts.add(new IfStatement.ElsifPart(elsifCondition, elsifStatements));
      }
    }
    
    // Parse ELSE statements
    List<Statement> elseStatements = null;
    if (ctx.else_part() != null && ctx.else_part().seq_of_statements() != null && 
        ctx.else_part().seq_of_statements().statement() != null) {
      elseStatements = new ArrayList<>();
      for (PlSqlParser.StatementContext stmt : ctx.else_part().seq_of_statements().statement()) {
        Statement statement = (Statement) visit(stmt);
        if (statement != null) {
          elseStatements.add(statement);
        }
      }
    }
    
    return new IfStatement(condition, thenStatements, elsifParts, elseStatements);
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
    if (ctx.unary_logical_expression() != null && ctx.logical_expression().isEmpty()) {
      // Simple case: just unary logical expression
      UnaryLogicalExpression unaryExpr = (UnaryLogicalExpression) visit(ctx.unary_logical_expression());
      return new LogicalExpression(unaryExpr);
    }
    
    if (ctx.logical_expression().size() == 2) {
      // Binary case: logical_expression AND/OR logical_expression
      LogicalExpression left = (LogicalExpression) visit(ctx.logical_expression(0));
      LogicalExpression right = (LogicalExpression) visit(ctx.logical_expression(1));
      String operator = null;
      
      if (ctx.AND() != null) {
        operator = "AND";
      } else if (ctx.OR() != null) {
        operator = "OR";
      }
      
      return new LogicalExpression(left, operator, right);
    }
    
    // Fallback - convert raw text to logical expression
    LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression(ctx.getText()));
    return new Expression(logicalExpr);
  }

  @Override
  public PlSqlAst visitUnary_logical_expression(PlSqlParser.Unary_logical_expressionContext ctx) {
    boolean hasNot = ctx.NOT() != null;
    Expression multisetExpr = null;
    String logicalOperation = null;
    
    if (ctx.multiset_expression() != null) {
      // Visit the multiset_expression to properly handle cursor attributes and other expressions
      PlSqlAst multisetAst = visit(ctx.multiset_expression());
      if (multisetAst instanceof MultisetExpression) {
        // Create expression from the multiset expression
        LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression("MULTISET_EXPR_PLACEHOLDER"));
        multisetExpr = new Expression(logicalExpr) {
          @Override
          public String toPostgre(me.christianrobert.ora2postgre.global.Everything data) {
            return ((MultisetExpression) multisetAst).toPostgre(data);
          }
          
          @Override
          public String toString() {
            return multisetAst.toString();
          }
        };
      } else {
        // Fallback to text if visit returns something else
        LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression(ctx.multiset_expression().getText()));
        multisetExpr = new Expression(logicalExpr);
      }
    }
    
    if (ctx.unary_logical_operation() != null) {
      // Build the logical operation string (IS NULL, IS NOT NULL, etc.)
      StringBuilder opBuilder = new StringBuilder();
      var operation = ctx.unary_logical_operation();
      
      if (operation.IS() != null) {
        opBuilder.append("IS");
        if (operation.NOT() != null) {
          opBuilder.append(" NOT");
        }
        if (operation.logical_operation() != null) {
          if (operation.logical_operation().NULL_() != null) {
            opBuilder.append(" NULL");
          } else if (operation.logical_operation().NAN_() != null) {
            opBuilder.append(" NAN");
          } else if (operation.logical_operation().EMPTY_() != null) {
            opBuilder.append(" EMPTY");
          } else {
            // Handle other logical operations
            opBuilder.append(" ").append(operation.logical_operation().getText());
          }
        }
      }
      
      logicalOperation = opBuilder.toString();
    }
    
    return new UnaryLogicalExpression(hasNot, multisetExpr, logicalOperation);
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
    String typeName = NameNormalizer.normalizeObjectTypeName(ctx.type_name().getText());
    // object types
    List<Variable> variables = new ArrayList<>();
    if (ctx.object_type_def() != null && ctx.object_type_def().object_member_spec() != null) {
      for (var member : ctx.object_type_def().object_member_spec()) {
        PlSqlAst memberAst = visit(member);
        if (memberAst instanceof Variable) {
          variables.add((Variable) memberAst);
        }
      }
    }

    // varrays
    if (ctx.object_type_def() != null
            && ctx.object_type_def().object_as_part() != null
            && ctx.object_type_def().object_as_part().varray_type_def() != null
    ) {
      VarrayType varrayType = (VarrayType) visit(ctx.object_type_def().object_as_part().varray_type_def());
      return new ObjectType(typeName, schema, null, null, null, null,
              varrayType, null);
    }

    if (ctx.object_type_def() != null
            && ctx.object_type_def().object_as_part() != null
            && ctx.object_type_def().object_as_part().nested_table_type_def() != null) {
      NestedTableType nestedTableType = (NestedTableType) visit(ctx.object_type_def().object_as_part().nested_table_type_def());
      return new ObjectType(typeName, schema, null, null, null, null, null, nestedTableType);
    }

    // TODO do something about the under clause?!
    return new ObjectType(typeName, schema, variables, null, null, null, null, null);
  }

  @Override
  public PlSqlAst visitObject_member_spec(PlSqlParser.Object_member_specContext ctx) {
    if (ctx.identifier() != null &&
            ctx.identifier().id_expression() != null &&
            ctx.type_spec() != null) {
      String name = ctx.identifier().id_expression().getText();
      DataTypeSpec dataType = (DataTypeSpec) visit(ctx.type_spec());
      return new Variable(name, dataType, null);
    }
    if (ctx.element_spec() != null) {
      return visit(ctx.element_spec());
    }
    // element_spec not implemented
    return null;
  }

  @Override
  public PlSqlAst visitType_elements_parameter(PlSqlParser.Type_elements_parameterContext ctx) {
    return new Parameter(
      ctx.parameter_name().getText(),
      (DataTypeSpec) visit(ctx.type_spec()),
      ctx.default_value_part() != null ? (Expression) visit(ctx.default_value_part()) : null,
      ctx.IN() != null && ctx.IN().getText() != null,
      ctx.OUT() != null  && ctx.OUT().getText() != null
    );
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
    if (ctx.seq_of_declare_specs() != null) {
      variables = extractVariablesFromDeclareSpecs(ctx.seq_of_declare_specs());
      cursorDeclarations = extractCursorDeclarationsFromDeclareSpecs(ctx.seq_of_declare_specs());
      recordTypes = extractRecordTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
    }
    return new Function(ctx.function_name().getText(), parameters, variables, cursorDeclarations, recordTypes, returnType, statements, null);
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
    if (ctx.seq_of_declare_specs() != null) {
      variables = extractVariablesFromDeclareSpecs(ctx.seq_of_declare_specs());
      cursorDeclarations = extractCursorDeclarationsFromDeclareSpecs(ctx.seq_of_declare_specs());
      recordTypes = extractRecordTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
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
    if (ctx.seq_of_declare_specs() != null) {
      variables = extractVariablesFromDeclareSpecs(ctx.seq_of_declare_specs());
      cursorDeclarations = extractCursorDeclarationsFromDeclareSpecs(ctx.seq_of_declare_specs());
      recordTypes = extractRecordTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
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
    if (ctx.seq_of_declare_specs() != null) {
      variables = extractVariablesFromDeclareSpecs(ctx.seq_of_declare_specs());
      cursorDeclarations = extractCursorDeclarationsFromDeclareSpecs(ctx.seq_of_declare_specs());
      recordTypes = extractRecordTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
    }

    // Parse exception block if present
    ExceptionBlock exceptionBlock = null;
    if (ctx.body() != null && ctx.body().exception_handler() != null && !ctx.body().exception_handler().isEmpty()) {
      exceptionBlock = parseExceptionBlock(ctx.body().exception_handler());
    }

    return new Function(procedureName, parameters, variables, cursorDeclarations, recordTypes, returnType, statements, exceptionBlock);
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
        }
      }
    }
    // TODO $if, subtype, packagetype, cursor, etc.
    return new OraclePackage(packageName, schema, variables, null, null, null, recordTypes, varrayTypes, nestedTableTypes, null, null, null);
  }

  @Override
  public PlSqlAst visitCreate_package_body(PlSqlParser.Create_package_bodyContext ctx) {
    String packageName = "UNKNOWN";
    if (ctx.package_name() != null && !ctx.package_name().isEmpty()) {
      for (PlSqlParser.Package_nameContext p : ctx.package_name()) {
        packageName = p.getText();
      }
    }
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
    if (ctx.seq_of_declare_specs() != null) {
      variables = extractVariablesFromDeclareSpecs(ctx.seq_of_declare_specs());
      cursorDeclarations = extractCursorDeclarationsFromDeclareSpecs(ctx.seq_of_declare_specs());
      recordTypes = extractRecordTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
    }
    
    Function function = new Function(functionName, parameters, variables, cursorDeclarations, recordTypes, returnType, statements, null);
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
    if (ctx.seq_of_declare_specs() != null) {
      variables = extractVariablesFromDeclareSpecs(ctx.seq_of_declare_specs());
      cursorDeclarations = extractCursorDeclarationsFromDeclareSpecs(ctx.seq_of_declare_specs());
      recordTypes = extractRecordTypesFromDeclareSpecs(ctx.seq_of_declare_specs());
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
    if (ctx.standard_function() != null) {
      PlSqlAst standardFunctionAst = visit(ctx.standard_function());
      if (standardFunctionAst instanceof Expression) {
        return UnaryExpression.forStandardFunction((Expression) standardFunctionAst);
      }
      // Fallback
      return visitChildren(ctx);
    }
    
    if (ctx.atom() != null) {
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
    if (ctx.model_expression() != null) {
      PlSqlAst modelAst = visit(ctx.model_expression());
      if (modelAst instanceof ModelExpression) {
        return new Concatenation((ModelExpression) modelAst);
      }
      // Fallback
      return visitChildren(ctx);
    }
    
    // For concatenation operations, fall back to default behavior
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
    
    // For other function types, fall back to default behavior
    return visitChildren(ctx);
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
    if (ctx.identifier() != null && ctx.record_type_def() != null) {
      String typeName = ctx.identifier().getText();
      RecordType recordType = (RecordType) visit(ctx.record_type_def());
      // Set the name from the declaration
      return new RecordType(typeName, recordType.getFields());
    }
    
    // Handle VARRAY type declarations: TYPE name IS VARRAY(size) OF type_spec
    if (ctx.identifier() != null && ctx.varray_type_def() != null) {
      String typeName = ctx.identifier().getText();
      VarrayType varrayType = (VarrayType) visit(ctx.varray_type_def());
      // Return a named VarrayType (similar to RecordType pattern)
      return new VarrayType(typeName, varrayType.getSize(), varrayType.getSizeExpression(), varrayType.getDataType());
    }
    
    // Handle TABLE OF type declarations: TYPE name IS TABLE OF type_spec
    if (ctx.identifier() != null && ctx.table_type_def() != null) {
      String typeName = ctx.identifier().getText();
      NestedTableType nestedTableType = (NestedTableType) visit(ctx.table_type_def());
      // Return a named NestedTableType (similar to RecordType pattern)
      return new NestedTableType(typeName, nestedTableType.getDataType());
    }
    
    // TODO: Handle ref_cursor_type_def
    return new Comment("type_declaration not fully implemented");
  }

  @Override
  public PlSqlAst visitRecord_type_def(PlSqlParser.Record_type_defContext ctx) {
    List<RecordType.RecordField> fields = new ArrayList<>();
    
    if (ctx.field_spec() != null) {
      for (PlSqlParser.Field_specContext fieldCtx : ctx.field_spec()) {
        RecordType.RecordField field = (RecordType.RecordField) visit(fieldCtx);
        if (field != null) {
          fields.add(field);
        }
      }
    }
    
    return new RecordType("", fields); // Name will be set by type_declaration
  }

  @Override
  public PlSqlAst visitField_spec(PlSqlParser.Field_specContext ctx) {
    String fieldName = ctx.column_name().getText();
    
    // Parse data type
    DataTypeSpec dataType = null;
    if (ctx.type_spec() != null) {
      dataType = (DataTypeSpec) visit(ctx.type_spec());
    } else {
      // Default to VARCHAR if no type specified
      dataType = new DataTypeSpec("VARCHAR2", null, null, null);
    }
    
    // Parse NULL/NOT NULL constraint
    boolean notNull = false;
    if (ctx.NULL_() != null && ctx.NOT() != null) {
      notNull = true;
    }
    
    // Parse default value
    Expression defaultValue = null;
    if (ctx.default_value_part() != null) {
      defaultValue = (Expression) visit(ctx.default_value_part());
    }
    
    return new RecordType.RecordField(fieldName, dataType, notNull, defaultValue);
  }

}