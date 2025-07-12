package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

/**
 * AST class representing compound_expression from the grammar.
 * Grammar rule: compound_expression
 *   : concatenation (
 *       NOT? (
 *           IN in_elements
 *           | BETWEEN between_elements
 *           | like_type = (LIKE | LIKEC | LIKE2 | LIKE4) concatenation (ESCAPE concatenation)?
 *       )
 *   )?
 */
public class CompoundExpression extends PlSqlAst {
  private final Concatenation concatenation;
  private final boolean hasNot;
  private final String operationType; // IN, BETWEEN, LIKE, LIKEC, LIKE2, LIKE4
  private final List<Expression> inElements; // For IN operations
  private final Expression betweenStart; // For BETWEEN operations
  private final Expression betweenEnd; // For BETWEEN operations
  private final Concatenation likePattern; // For LIKE operations
  private final Concatenation escapeExpression; // For LIKE ESCAPE operations

  // Constructor for simple concatenation (most common case)
  public CompoundExpression(Concatenation concatenation) {
    this.concatenation = concatenation;
    this.hasNot = false;
    this.operationType = null;
    this.inElements = null;
    this.betweenStart = null;
    this.betweenEnd = null;
    this.likePattern = null;
    this.escapeExpression = null;
  }

  // Constructor for IN operations
  public CompoundExpression(Concatenation concatenation, boolean hasNot, List<Expression> inElements) {
    this.concatenation = concatenation;
    this.hasNot = hasNot;
    this.operationType = "IN";
    this.inElements = inElements;
    this.betweenStart = null;
    this.betweenEnd = null;
    this.likePattern = null;
    this.escapeExpression = null;
  }

  // Constructor for BETWEEN operations
  public CompoundExpression(Concatenation concatenation, boolean hasNot, 
                           Expression betweenStart, Expression betweenEnd) {
    this.concatenation = concatenation;
    this.hasNot = hasNot;
    this.operationType = "BETWEEN";
    this.inElements = null;
    this.betweenStart = betweenStart;
    this.betweenEnd = betweenEnd;
    this.likePattern = null;
    this.escapeExpression = null;
  }

  // Constructor for LIKE operations
  public CompoundExpression(Concatenation concatenation, boolean hasNot, String likeType,
                           Concatenation likePattern, Concatenation escapeExpression) {
    this.concatenation = concatenation;
    this.hasNot = hasNot;
    this.operationType = likeType;
    this.inElements = null;
    this.betweenStart = null;
    this.betweenEnd = null;
    this.likePattern = likePattern;
    this.escapeExpression = escapeExpression;
  }

  public Concatenation getConcatenation() {
    return concatenation;
  }

  public boolean hasNot() {
    return hasNot;
  }

  public String getOperationType() {
    return operationType;
  }

  public List<Expression> getInElements() {
    return inElements;
  }

  public Expression getBetweenStart() {
    return betweenStart;
  }

  public Expression getBetweenEnd() {
    return betweenEnd;
  }

  public Concatenation getLikePattern() {
    return likePattern;
  }

  public Concatenation getEscapeExpression() {
    return escapeExpression;
  }

  public boolean isSimpleConcatenation() {
    return operationType == null;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (concatenation != null) {
      sb.append(concatenation.toString());
    }

    if (operationType != null) {
      if (hasNot) {
        sb.append(" NOT");
      }
      sb.append(" ").append(operationType);

      switch (operationType) {
        case "IN":
          if (inElements != null && !inElements.isEmpty()) {
            sb.append(" (");
            for (int i = 0; i < inElements.size(); i++) {
              if (i > 0) sb.append(", ");
              sb.append(inElements.get(i).toString());
            }
            sb.append(")");
          }
          break;

        case "BETWEEN":
          if (betweenStart != null && betweenEnd != null) {
            sb.append(" ").append(betweenStart.toString());
            sb.append(" AND ").append(betweenEnd.toString());
          }
          break;

        case "LIKE":
        case "LIKEC":
        case "LIKE2":
        case "LIKE4":
          if (likePattern != null) {
            sb.append(" ").append(likePattern.toString());
          }
          if (escapeExpression != null) {
            sb.append(" ESCAPE ").append(escapeExpression.toString());
          }
          break;
      }
    }

    return sb.toString();
  }

  public String toPostgre(Everything data) {
    StringBuilder sb = new StringBuilder();
    if (concatenation != null) {
      sb.append(concatenation.toPostgre(data));
    }

    if (operationType != null) {
      if (hasNot) {
        sb.append(" NOT");
      }
      
      // Transform Oracle-specific LIKE operations to PostgreSQL
      String pgOperationType = transformOperationType(operationType);
      sb.append(" ").append(pgOperationType);

      switch (operationType) {
        case "IN":
          if (inElements != null && !inElements.isEmpty()) {
            sb.append(" (");
            for (int i = 0; i < inElements.size(); i++) {
              if (i > 0) sb.append(", ");
              sb.append(inElements.get(i).toPostgre(data));
            }
            sb.append(")");
          }
          break;

        case "BETWEEN":
          if (betweenStart != null && betweenEnd != null) {
            sb.append(" ").append(betweenStart.toPostgre(data));
            sb.append(" AND ").append(betweenEnd.toPostgre(data));
          }
          break;

        case "LIKE":
        case "LIKEC":
        case "LIKE2":
        case "LIKE4":
          if (likePattern != null) {
            sb.append(" ").append(likePattern.toPostgre(data));
          }
          if (escapeExpression != null) {
            sb.append(" ESCAPE ").append(escapeExpression.toPostgre(data));
          }
          break;
      }
    }

    return sb.toString();
  }

  /**
   * Transform Oracle-specific LIKE operations to PostgreSQL equivalents.
   */
  private String transformOperationType(String oracleOperationType) {
    if (oracleOperationType == null) {
      return "";
    }
    
    switch (oracleOperationType.toUpperCase()) {
      case "LIKEC":
      case "LIKE2":
      case "LIKE4":
        // Oracle character set specific LIKE operations -> PostgreSQL LIKE
        return "LIKE";
      case "LIKE":
      case "IN":
      case "BETWEEN":
        return oracleOperationType; // Same in both Oracle and PostgreSQL
      default:
        return oracleOperationType; // Pass through unknown operators
    }
  }
}