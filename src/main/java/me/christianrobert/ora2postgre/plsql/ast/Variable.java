package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.services.TransformationContext;
import jakarta.inject.Inject;

public class Variable extends PlSqlAst {

  @Inject
  TransformationContext transformationContext;

  /**
   * For testing purposes - allows manual injection of TransformationContext
   * when CDI container is not available.
   */
  public void setTransformationContext(TransformationContext transformationContext) {
    this.transformationContext = transformationContext;
  }
  private String name;
  private DataTypeSpec dataType;
  private Expression defaultValue;

  public Variable(String name, DataTypeSpec dataType, Expression defaultValue) {
    this.name = name;
    this.dataType = dataType;
    this.defaultValue = defaultValue;
  }

  public String getName() {
    return name;
  }

  public DataTypeSpec getDataType() {
    return dataType;
  }

  public Expression getDefaultValue() { return defaultValue; }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Variable{name=" + name + ", dataType=" + dataType + "}";
  }

  // toJava() method removed - variables stay in PostgreSQL

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    b.append(name)
            .append(" ")
            .append(dataType.toPostgre(data));
    
    // Add default value if present
    if (defaultValue != null) {
      b.append(" := ").append(defaultValue.toPostgre(data));
    }
    
    return b.toString();
  }

  public String toPostgre(Everything data, Function function) {
    StringBuilder b = new StringBuilder();
    b.append(name)
            .append(" ")
            .append(dataType.toPostgre(data, function));
    
    // Add default value if present
    if (defaultValue != null) {
      // Set function context for collection constructor resolution
      TransformationContext context = transformationContext != null ? transformationContext : TransformationContext.getTestInstance();
      if (context != null) {
        context.withFunctionContext(function, () -> {
          b.append(" := ").append(defaultValue.toPostgre(data));
        });
      } else {
        // Fallback if no context available
        b.append(" := ").append(defaultValue.toPostgre(data));
      }
    }
    
    return b.toString();
  }
}