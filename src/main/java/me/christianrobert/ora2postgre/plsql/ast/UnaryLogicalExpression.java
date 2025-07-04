package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

public class UnaryLogicalExpression extends PlSqlAst {
    
    private boolean hasNot;
    private Expression multisetExpression;
    private String logicalOperation; // IS NULL, IS NOT NULL, etc.

    public UnaryLogicalExpression(boolean hasNot, Expression multisetExpression, String logicalOperation) {
        this.hasNot = hasNot;
        this.multisetExpression = multisetExpression;
        this.logicalOperation = logicalOperation;
    }

    // Constructor for simple expression without NOT or logical operation
    public UnaryLogicalExpression(Expression multisetExpression) {
        this.hasNot = false;
        this.multisetExpression = multisetExpression;
        this.logicalOperation = null;
    }

    // Constructor for simple text (used for raw text conversion)
    public UnaryLogicalExpression(String text) {
        this.hasNot = false;
        this.multisetExpression = null;
        this.logicalOperation = text;
    }

    private static String buildRawText(boolean hasNot, Expression multisetExpression, String logicalOperation) {
        StringBuilder sb = new StringBuilder();
        if (hasNot) {
            sb.append("NOT ");
        }
        if (multisetExpression != null) {
            sb.append(multisetExpression.toString());
        } else if (logicalOperation != null) {
            // If there's no multiset expression, logicalOperation might contain the raw text
            sb.append(logicalOperation);
        }
        return sb.toString();
    }

    public boolean hasNot() {
        return hasNot;
    }

    public Expression getMultisetExpression() {
        return multisetExpression;
    }

    public String getLogicalOperation() {
        return logicalOperation;
    }

    @Override
    public <T> T accept(PlSqlAstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return buildRawText(hasNot, multisetExpression, logicalOperation);
    }

    // toJava() method removed - unary logical expressions stay in PostgreSQL

    public String toPostgre(Everything data) {
        StringBuilder sb = new StringBuilder();
        if (hasNot) {
            sb.append("NOT ");
        }
        if (multisetExpression != null) {
            sb.append(multisetExpression.toPostgre(data));
        } else if (logicalOperation != null) {
            // If there's no multiset expression, logicalOperation might contain the raw text
            sb.append(logicalOperation);
        }
        return sb.toString();
    }
}