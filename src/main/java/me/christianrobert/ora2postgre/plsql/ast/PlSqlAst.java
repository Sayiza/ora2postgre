package me.christianrobert.ora2postgre.plsql.ast;

public abstract class PlSqlAst {
  // Base class for all AST nodes
  public abstract <T> T accept(PlSqlAstVisitor<T> visitor);
}