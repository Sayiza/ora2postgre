package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

/**
 * AST class representing model_expression from the grammar.
 * Grammar rule: model_expression
 *   : unary_expression ('[' model_expression_element ']')?
 */
public class ModelExpression extends PlSqlAst {
  private final UnaryExpression unaryExpression;
  private final List<Expression> modelExpressionElements; // For array access: [element1, element2, ...]

  // Constructor for simple unary expression (most common case)
  public ModelExpression(UnaryExpression unaryExpression) {
    this.unaryExpression = unaryExpression;
    this.modelExpressionElements = null;
  }

  // Constructor for model expression with array access
  public ModelExpression(UnaryExpression unaryExpression, List<Expression> modelExpressionElements) {
    this.unaryExpression = unaryExpression;
    this.modelExpressionElements = modelExpressionElements;
  }

  public UnaryExpression getUnaryExpression() {
    return unaryExpression;
  }

  public List<Expression> getModelExpressionElements() {
    return modelExpressionElements;
  }

  public boolean hasArrayAccess() {
    return modelExpressionElements != null && !modelExpressionElements.isEmpty();
  }

  public boolean isSimpleUnaryExpression() {
    return !hasArrayAccess();
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (unaryExpression != null) {
      sb.append(unaryExpression.toString());
    }

    if (hasArrayAccess()) {
      sb.append("[");
      for (int i = 0; i < modelExpressionElements.size(); i++) {
        if (i > 0) sb.append(", ");
        sb.append(modelExpressionElements.get(i).toString());
      }
      sb.append("]");
    }

    return sb.toString();
  }

  public String toPostgre(Everything data) {
    StringBuilder sb = new StringBuilder();
    if (unaryExpression != null) {
      sb.append(unaryExpression.toPostgre(data));
    }

    if (hasArrayAccess()) {
      // PostgreSQL array access syntax is similar but uses different brackets
      sb.append("[");
      for (int i = 0; i < modelExpressionElements.size(); i++) {
        if (i > 0) sb.append(", ");
        sb.append(modelExpressionElements.get(i).toPostgre(data));
      }
      sb.append("]");
    }

    return sb.toString();
  }
}