package me.christianrobert.ora2postgre.plsql.ast;

public interface PlSqlAstVisitor<T> {
  T visit(ObjectType objectType);
  T visit(Variable variable);
  T visit(Procedure procedure);
  T visit(Function function);
  T visit(Constructor constructor);
  T visit(Parameter parameter);
  T visit(Statement statement);
  T visit(PackageType packageType);
  T visit(SubType subType);
  T visit(Cursor cursor);
  T visit(Comment comment);
  T visit(SelectStatement selectStatement);
  T visit(SelectListElement selectListElement);
  T visit(SelectFetchClause selectFetchClause);
  T visit(SelectForUpdateClause selectForUpdateClause);
  T visit(SelectOffsetClause selectOffsetClause);
  T visit(SelectOrderByClause selectOrderByClause);
  T visit(SelectQueryBlock selectQueryBlock);
  T visit(SelectSubQuery subQuery);
  T visit(SelectSubQueryBasicElement subQueryBasicElement);
  T visit(SelectWithClause selectWithClause);
  T visit(TableReference tableReference);
  T visit(TableReferenceAux tableReferenceAux);
  T visit(TableReferenceAuxInternal tableReferenceAuxInternal);
  T visit(TableExpressionClause tableExpressionClause);

  T visit(OraclePackage pkg);
  T visit(ReturnStatement statement);
  T visit(AssignmentStatement statement);
  T visit(Expression expression);

  T visit(VarrayType varrayType);
  T visit(NestedTableType nestedTableType);
  T visit(DataTypeSpec dataTypeSpec);
  T visit(WhereClause whereClause);
  T visit(UnaryLogicalExpression unaryLogicalExpression);
  T visit(LogicalExpression logicalExpression);
  T visit(CursorExpression cursorExpression);
  T visit(Trigger trigger);
  T visit(ExceptionBlock exceptionBlock);
  T visit(ExceptionHandler exceptionHandler);
}