package me.christianrobert.ora2postgre.plsql;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.antlr.PlSqlParserBaseVisitor;
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

  // LOOP and SELECT START
  @Override
  public PlSqlAst visitLoop_statement(PlSqlParser.Loop_statementContext ctx) {
    List<Statement> statements = new ArrayList<>();
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
    return new Comment("this type of loop statement not implemented" + ctx.getText());
  }

  @Override
  public PlSqlAst visitSelect_statement(PlSqlParser.Select_statementContext ctx) {
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

    return new SelectQueryBlock(selectedFields, fromTables, whereClause);
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
    return new CursorExpression(subquery);
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
      // For now, treat multiset_expression as a general expression
      // This can be refined later when multiset expressions are fully implemented
      LogicalExpression logicalExpr = new LogicalExpression(new UnaryLogicalExpression(ctx.multiset_expression().getText()));
      multisetExpr = new Expression(logicalExpr);
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
    return new Function(ctx.function_name().getText(), parameters, returnType, statements);
  }

  @Override
  public PlSqlAst visitProc_decl_in_type(PlSqlParser.Proc_decl_in_typeContext ctx) {
    List<Parameter> parameters = new ArrayList<>();
    String procedureName = ctx.procedure_name().getText();

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

    return new Procedure(procedureName, parameters, statements);
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

    List<Statement> statements = new ArrayList<>();
    if (ctx.body() != null
            && ctx.body().seq_of_statements() != null
            && ctx.body().seq_of_statements().statement() != null) {
      for (PlSqlParser.StatementContext stmt : ctx.body().seq_of_statements().statement()) {
        statements.add((Statement) visit(stmt));
      }
    }

    return new Procedure(procedureName, parameters, statements);
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

    return new Function(procedureName, parameters, returnType, statements);
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

    if (ctx.package_obj_spec() != null) {
      for (var member : ctx.package_obj_spec()) {
        PlSqlAst memberAst = visit(member);
        if (memberAst instanceof Variable) {
          variables.add((Variable) memberAst);
        }
      }
    }
    // TODO $if, subtype, packagetype, cursor, etc.
    return new OraclePackage(packageName,schema, variables, null, null , null, null, null, null);
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
    if (ctx.package_obj_body() != null) {
      for (var member : ctx.package_obj_body()) {
        PlSqlAst memberAst = visit(member);
        if (memberAst instanceof Variable) {
          variables.add((Variable) memberAst);
        }
        if (memberAst instanceof Function) {
          funcs.add((Function) memberAst);
        }
        if (memberAst instanceof Procedure) {
          procedures.add((Procedure) memberAst);
        }
      }
    }
    OraclePackage o = new OraclePackage(packageName, schema, variables, null, null, null, funcs, procedures, null);
    o.getFunctions().forEach(e -> e.setParentPackage(o));
    o.getProcedures().forEach(e -> e.setParentPackage(o));
    return o;
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

}