package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

public class ForLoopStatement extends Statement {
  String schema;
  String nameRef;
  SelectStatement query;
  List<Statement> statements;

  public ForLoopStatement(String schema, String nameRef, SelectStatement query, List<Statement> statements) {
    this.schema = schema;
    this.nameRef = nameRef;
    this.query = query;
    this.statements = statements;
  }

  public SelectStatement getQuery() {
    return query;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Return{" + query + "}";
  }

  // toJava() method removed - FOR loops stay in PostgreSQL
  // Complex cursor iteration handled by PostgreSQL directly

  /**
   * Generates the DECLARE section entry for the FOR loop record variable.
   * In PostgreSQL, cursor FOR loops need an explicit RECORD declaration.
   */
  public String toPostgreDeclaration(Everything data) {
    return "  " + nameRef + " RECORD;\n";
  }

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    
    // In PostgreSQL, we need to declare the record variable in the DECLARE section
    // This is handled by the containing function's variable declaration logic
    
    // Generate the FOR loop statement
    b.append(data.getIntendation()).append("FOR ").append(nameRef).append(" IN (");
    b.append(query.toPostgre(data));
    b.append(")\n");
    b.append(data.getIntendation()).append("LOOP\n");
    
    // Increase indentation for loop body
    data.intendMore();
    
    // Generate statements inside the loop
    for (Statement stmt : statements) {
      b.append(stmt.toPostgre(data));
    }
    
    // Decrease indentation
    data.intendLess();
    
    // Close the loop
    b.append(data.getIntendation()).append("END LOOP;\n");
    
    return b.toString();
  }
  
  /**
   * Gets the name of the loop record variable for declaration purposes.
   */
  public String getNameRef() {
    return nameRef;
  }
  
  /**
   * Gets the statements inside the FOR loop for recursive declaration collection.
   */
  public List<Statement> getStatements() {
    return statements;
  }
}