package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

public class LogicalExpression extends PlSqlAst {
    
    private UnaryLogicalExpression left;
    private String operator; // "AND" or "OR"
    private LogicalExpression right;

    // Constructor for unary logical expression (no operator)
    public LogicalExpression(UnaryLogicalExpression unaryExpression) {
        this.left = unaryExpression;
        this.operator = null;
        this.right = null;
    }

    // Constructor for binary logical expression (with AND/OR operator)
    public LogicalExpression(LogicalExpression left, String operator, LogicalExpression right) {
        this.left = null; // In binary case, we store left as LogicalExpression
        this.operator = operator;
        this.right = right;
        // Note: For binary operations, the left operand is actually a LogicalExpression
        // This is handled through the leftLogical field below
        this.leftLogical = left;
    }

    private LogicalExpression leftLogical; // Used for binary operations

    public UnaryLogicalExpression getLeft() {
        return left;
    }

    public LogicalExpression getLeftLogical() {
        return leftLogical;
    }

    public String getOperator() {
        return operator;
    }

    public LogicalExpression getRight() {
        return right;
    }

    public boolean isBinary() {
        return operator != null;
    }

    @Override
    public <T> T accept(PlSqlAstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        if (isBinary()) {
            return leftLogical.toString() + " " + operator + " " + right.toString();
        } else {
            return left.toString();
        }
    }

    // toJava() method removed - logical expressions stay in PostgreSQL

    public String toPostgre(Everything data) {
        if (isBinary()) {
            return leftLogical.toPostgre(data) + " " + operator + " " + right.toPostgre(data);
        } else {
            return left.toPostgre(data);
        }
    }
}