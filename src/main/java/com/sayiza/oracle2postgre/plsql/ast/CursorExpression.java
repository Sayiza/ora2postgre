package com.sayiza.oracle2postgre.plsql.ast;

import com.sayiza.oracle2postgre.global.Everything;

public class CursorExpression extends PlSqlAst {
    
    private SelectSubQuery subquery;

    public CursorExpression(SelectSubQuery subquery) {
        this.subquery = subquery;
    }

    public SelectSubQuery getSubquery() {
        return subquery;
    }

    @Override
    public <T> T accept(PlSqlAstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "CURSOR(" + (subquery != null ? subquery.toString() : "") + ")";
    }

    // toJava() method removed - cursor expressions stay in PostgreSQL

    public String toPostgre(Everything data) {
        // In PostgreSQL, cursor expressions can be handled with CURSOR FOR syntax
        if (subquery != null) {
            return "CURSOR FOR " + subquery.toPostgre(data);
        }
        return "CURSOR FOR /* TODO: handle empty cursor */";
    }
}