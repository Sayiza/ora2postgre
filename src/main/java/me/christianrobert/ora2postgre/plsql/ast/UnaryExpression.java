package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.SchemaResolutionUtils;
import me.christianrobert.ora2postgre.services.TransformationContext;
import jakarta.inject.Inject;
import me.christianrobert.ora2postgre.plsql.ast.tools.helpers.CollectionTypeInfo;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PackageVariableReferenceTransformer;

import java.util.ArrayList;
import java.util.List;

/**
 * AST class representing unary_expression from the grammar.
 * Grammar rule: unary_expression
 *   : ('-' | '+') unary_expression
 *   | PRIOR unary_expression
 *   | CONNECT_BY_ROOT unary_expression
 *   | NEW unary_expression
 *   | DISTINCT unary_expression
 *   | ALL unary_expression
 *   | case_expression
 *   | unary_expression '.' (
 *       (COUNT | FIRST | LAST | LIMIT)
 *       | (EXISTS | NEXT | PRIOR) '(' index += expression ')'
 *   )
 *   | quantified_expression
 *   | standard_function
 *   | atom
 *   | implicit_cursor_expression
 */
public class UnaryExpression extends PlSqlAst {

  @Inject
  TransformationContext transformationContext;

  /**
   * For testing purposes - allows manual injection of TransformationContext
   * when CDI container is not available.
   */
  public void setTransformationContext(TransformationContext transformationContext) {
    this.transformationContext = transformationContext;
  }

  /**
   * Helper method to get current function from TransformationContext with fallback.
   */
  private Function getCurrentFunctionFromContext() {
    TransformationContext context = transformationContext != null ? transformationContext : TransformationContext.getTestInstance();
    return context != null ? context.getCurrentFunction() : null;
  }

  /**
   * Helper method to get current procedure from TransformationContext with fallback.
   */
  private Procedure getCurrentProcedureFromContext() {
    TransformationContext context = transformationContext != null ? transformationContext : TransformationContext.getTestInstance();
    return context != null ? context.getCurrentProcedure() : null;
  }
  private final String unaryOperator; // -, +, PRIOR, CONNECT_BY_ROOT, NEW, DISTINCT, ALL
  private final UnaryExpression childExpression; // For unary operations
  private final Expression caseExpression;
  private final Expression quantifiedExpression;
  private final Expression standardFunction;
  private final Expression atom;
  private final Expression implicitCursorExpression;
  private final String collectionMethod; // COUNT, FIRST, LAST, LIMIT, EXISTS, NEXT, PRIOR
  private final List<Expression> methodArguments; // For collection methods with arguments
  private final boolean isArrayIndexing; // True if this represents array indexing: arr(i) -> arr[i]
  private final String arrayVariable; // Variable name for array indexing
  private final Expression indexExpression; // Index expression for array indexing
  private final boolean isCollectionConstructor; // True if this represents collection constructor: t_numbers(1,2,3)
  private final String constructorName; // Constructor name for collection constructors
  private final List<Expression> constructorArguments; // Arguments for collection constructors

  // Constructor for unary operations (-, +, PRIOR, etc.)
  public UnaryExpression(String unaryOperator, UnaryExpression childExpression) {
    this.unaryOperator = unaryOperator;
    this.childExpression = childExpression;
    this.caseExpression = null;
    this.quantifiedExpression = null;
    this.standardFunction = null;
    this.atom = null;
    this.implicitCursorExpression = null;
    this.collectionMethod = null;
    this.methodArguments = null;
    this.isArrayIndexing = false;
    this.arrayVariable = null;
    this.indexExpression = null;
    this.isCollectionConstructor = false;
    this.constructorName = null;
    this.constructorArguments = null;
  }

  // Constructor for case expressions
  public UnaryExpression(Expression caseExpression) {
    this.unaryOperator = null;
    this.childExpression = null;
    this.caseExpression = caseExpression;
    this.quantifiedExpression = null;
    this.standardFunction = null;
    this.atom = null;
    this.implicitCursorExpression = null;
    this.collectionMethod = null;
    this.methodArguments = null;
    this.isArrayIndexing = false;
    this.arrayVariable = null;
    this.indexExpression = null;
    this.isCollectionConstructor = false;
    this.constructorName = null;
    this.constructorArguments = null;
  }

  // Private constructor for specific types
  private UnaryExpression(Expression caseExpression, Expression quantifiedExpression, Expression standardFunction, Expression atom, Expression implicitCursorExpression) {
    this.unaryOperator = null;
    this.childExpression = null;
    this.caseExpression = caseExpression;
    this.quantifiedExpression = quantifiedExpression;
    this.standardFunction = standardFunction;
    this.atom = atom;
    this.implicitCursorExpression = implicitCursorExpression;
    this.collectionMethod = null;
    this.methodArguments = null;
    this.isArrayIndexing = false;
    this.arrayVariable = null;
    this.indexExpression = null;
    this.isCollectionConstructor = false;
    this.constructorName = null;
    this.constructorArguments = null;
  }

  // Constructor for standard functions
  public static UnaryExpression forStandardFunction(Expression standardFunction) {
    return new UnaryExpression(null, null, standardFunction, null, null);
  }

  // Constructor for atoms
  public static UnaryExpression forAtom(Expression atom) {
    return new UnaryExpression(null, null, null, atom, null);
  }

  // Constructor for collection method calls
  public UnaryExpression(UnaryExpression baseExpression, String collectionMethod, List<Expression> methodArguments) {
    this.unaryOperator = null;
    this.childExpression = baseExpression;
    this.caseExpression = null;
    this.quantifiedExpression = null;
    this.standardFunction = null;
    this.atom = null;
    this.implicitCursorExpression = null;
    this.collectionMethod = collectionMethod;
    this.methodArguments = methodArguments;
    this.isArrayIndexing = false;
    this.arrayVariable = null;
    this.indexExpression = null;
    this.isCollectionConstructor = false;
    this.constructorName = null;
    this.constructorArguments = null;
  }

  // Constructor for array indexing calls
  public UnaryExpression(String arrayVariable, Expression indexExpression) {
    this.unaryOperator = null;
    this.childExpression = null;
    this.caseExpression = null;
    this.quantifiedExpression = null;
    this.standardFunction = null;
    this.atom = null;
    this.implicitCursorExpression = null;
    this.collectionMethod = null;
    this.methodArguments = null;
    this.isArrayIndexing = true;
    this.arrayVariable = arrayVariable;
    this.indexExpression = indexExpression;
    this.isCollectionConstructor = false;
    this.constructorName = null;
    this.constructorArguments = null;
  }

  // Constructor for collection constructors (NEW)
  public UnaryExpression(String constructorName, List<Expression> constructorArguments) {
    this.unaryOperator = null;
    this.childExpression = null;
    this.caseExpression = null;
    this.quantifiedExpression = null;
    this.standardFunction = null;
    this.atom = null;
    this.implicitCursorExpression = null;
    this.collectionMethod = null;
    this.methodArguments = null;
    this.isArrayIndexing = false;
    this.arrayVariable = null;
    this.indexExpression = null;
    this.isCollectionConstructor = true;
    this.constructorName = constructorName;
    this.constructorArguments = constructorArguments != null ? new ArrayList<>(constructorArguments) : new ArrayList<>();
  }


  public String getUnaryOperator() {
    return unaryOperator;
  }

  public UnaryExpression getChildExpression() {
    return childExpression;
  }

  public Expression getCaseExpression() {
    return caseExpression;
  }

  public Expression getQuantifiedExpression() {
    return quantifiedExpression;
  }

  public Expression getStandardFunction() {
    return standardFunction;
  }

  public Expression getAtom() {
    return atom;
  }

  public Expression getImplicitCursorExpression() {
    return implicitCursorExpression;
  }

  public String getCollectionMethod() {
    return collectionMethod;
  }

  public List<Expression> getMethodArguments() {
    return methodArguments;
  }

  public boolean isUnaryOperation() {
    return unaryOperator != null && childExpression != null;
  }

  public boolean isCaseExpression() {
    return caseExpression != null;
  }

  public boolean isQuantifiedExpression() {
    return quantifiedExpression != null;
  }

  public boolean isStandardFunction() {
    return standardFunction != null;
  }

  public boolean isAtom() {
    return atom != null;
  }

  public boolean isImplicitCursorExpression() {
    return implicitCursorExpression != null;
  }

  public boolean isCollectionMethodCall() {
    return collectionMethod != null;
  }


  public boolean isArrayIndexing() {
    return isArrayIndexing;
  }

  public String getArrayVariable() {
    return arrayVariable;
  }

  public Expression getIndexExpression() {
    return indexExpression;
  }

  public boolean isCollectionConstructor() {
    return isCollectionConstructor;
  }

  public String getConstructorName() {
    return constructorName;
  }

  public List<Expression> getConstructorArguments() {
    return constructorArguments != null ? new ArrayList<>(constructorArguments) : new ArrayList<>();
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    if (isUnaryOperation()) {
      return unaryOperator + " " + childExpression.toString();
    } else if (isCaseExpression()) {
      return caseExpression.toString();
    } else if (isQuantifiedExpression()) {
      return quantifiedExpression.toString();
    } else if (isStandardFunction()) {
      return standardFunction.toString();
    } else if (isAtom()) {
      return atom.toString();
    } else if (isImplicitCursorExpression()) {
      return implicitCursorExpression.toString();
    } else if (isCollectionMethodCall()) {
      StringBuilder sb = new StringBuilder();
      if (childExpression != null) {
        sb.append(childExpression.toString());
      }
      sb.append(".").append(collectionMethod);
      if (methodArguments != null && !methodArguments.isEmpty()) {
        sb.append("(");
        for (int i = 0; i < methodArguments.size(); i++) {
          if (i > 0) sb.append(", ");
          sb.append(methodArguments.get(i).toString());
        }
        sb.append(")");
      }
      return sb.toString();
    } else if (isArrayIndexing()) {
      return arrayVariable + "(" + indexExpression.toString() + ")";
    } else if (isCollectionConstructor()) {
      StringBuilder sb = new StringBuilder();
      sb.append(constructorName).append("(");
      if (constructorArguments != null) {
        for (int i = 0; i < constructorArguments.size(); i++) {
          if (i > 0) sb.append(", ");
          sb.append(constructorArguments.get(i).toString());
        }
      }
      sb.append(")");
      return sb.toString();
    } else {
      return "/* INVALID UNARY EXPRESSION */";
    }
  }

  public String toPostgre(Everything data) {
    if (isUnaryOperation()) {
      String transformedOperator = transformUnaryOperator(unaryOperator);
      return transformedOperator + " " + childExpression.toPostgre(data);
    } else if (isCaseExpression()) {
      return caseExpression.toPostgre(data);
    } else if (isQuantifiedExpression()) {
      return quantifiedExpression.toPostgre(data);
    } else if (isStandardFunction()) {
      // Check for collection constructor before standard function processing  
      String functionName = extractFunctionNameFromExpression(standardFunction);
      Function currentFunction = getCurrentFunctionFromContext();
      if (functionName != null && SchemaResolutionUtils.isCollectionTypeConstructor(data, functionName, currentFunction)) {
        // Extract arguments from the function expression (simplified approach)
        List<Expression> arguments = extractArgumentsFromExpression(standardFunction);
        return SchemaResolutionUtils.transformCollectionConstructor(data, functionName, arguments, currentFunction);
      }
      // Continue with normal function processing if not a collection constructor
      return standardFunction.toPostgre(data);
    } else if (isAtom()) {
      // FIRST: Check if atom represents table of records field access pattern: l_products(100).prod_id
      String atomText = atom.toString();
      if (atomText != null && containsTableOfRecordsFieldAccess(atomText, data)) {
        return transformTableOfRecordsFieldAccess(atomText, data);
      }
      
      // SECOND: Check if atom represents a collection constructor
      String functionName = extractFunctionNameFromExpression(atom);
      Function currentFunction = getCurrentFunctionFromContext();
      if (functionName != null && SchemaResolutionUtils.isCollectionTypeConstructor(data, functionName, currentFunction)) {
        // Extract arguments from the atom expression
        List<Expression> arguments = extractArgumentsFromExpression(atom);
        return SchemaResolutionUtils.transformCollectionConstructor(data, functionName, arguments, currentFunction);
      }
      
      return atom.toPostgre(data);
    } else if (isImplicitCursorExpression()) {
      return implicitCursorExpression.toPostgre(data);
    } else if (isCollectionMethodCall()) {
      // Transform Oracle collection methods to PostgreSQL function calls
      return transformCollectionMethodToPostgreSQL(data);
    } else if (isArrayIndexing()) {
      // SEMANTIC LAYER: Check if this is actually a collection constructor parsed as array indexing
      Function currentFunction = getCurrentFunctionFromContext();
      if (arrayVariable != null && SchemaResolutionUtils.isCollectionTypeConstructor(data, arrayVariable, currentFunction)) {
        // This is a collection constructor, not array indexing
        // Create argument list from the index expression (parsing limitation workaround)
        List<Expression> arguments = new ArrayList<>();
        if (indexExpression != null) {
          arguments.add(indexExpression);
        }
        return SchemaResolutionUtils.transformCollectionConstructor(data, arrayVariable, arguments, currentFunction);
      }
      
      // Transform Oracle array indexing to PostgreSQL array indexing
      return transformArrayIndexingToPostgreSQL(data);
    } else if (isCollectionConstructor()) {
      // Transform collection constructor to PostgreSQL array syntax
      return transformCollectionConstructorToPostgreSQL(data);
    } else {
      return "/* INVALID UNARY EXPRESSION */";
    }
  }

  /**
   * Transform Oracle unary operators to PostgreSQL equivalents.
   */
  private String transformUnaryOperator(String oracleOperator) {
    if (oracleOperator == null) {
      return "";
    }
    
    switch (oracleOperator.toUpperCase()) {
      case "-":
      case "+":
        return oracleOperator; // Same in PostgreSQL
      case "PRIOR":
        return "/* PRIOR - hierarchical query operator not directly supported */";
      case "CONNECT_BY_ROOT":
        return "/* CONNECT_BY_ROOT - hierarchical query operator not directly supported */";
      case "NEW":
        return "NEW"; // Similar concept in PostgreSQL triggers
      case "DISTINCT":
      case "ALL":
        return oracleOperator; // Same in PostgreSQL
      default:
        return oracleOperator; // Pass through unknown operators
    }
  }

  /**
   * Transform Oracle collection methods to PostgreSQL function calls.
   * This completely restructures the syntax from Oracle dot notation to PostgreSQL functions.
   */
  private String transformCollectionMethodToPostgreSQL(Everything data) {
    if (collectionMethod == null || childExpression == null) {
      return "/* INVALID COLLECTION METHOD CALL */";
    }
    
    // Check if this is actually record field access instead of a collection method
    if (isRecordFieldAccess(data)) {
      return transformRecordFieldAccess(data);
    }
    
    String arrayExpression = childExpression.toPostgre(data);
    
    // Check if this is a package collection variable
    String baseVariable = extractBaseVariableName(arrayExpression);
    if (baseVariable != null && PackageVariableReferenceTransformer.isPackageVariableReference(baseVariable, data)) {
      OraclePackage pkg = PackageVariableReferenceTransformer.findContainingPackage(baseVariable, data);
      if (pkg != null) {
        String targetSchema = pkg.getSchema();
        String packageName = pkg.getName();
        
        // Handle package collection methods with special parameter handling
        switch (collectionMethod.toUpperCase()) {
          case "DELETE":
            if (methodArguments != null && !methodArguments.isEmpty()) {
              // DELETE with index: arr.DELETE(i)
              String index = methodArguments.get(0).toPostgre(data);
              return PackageVariableReferenceTransformer.transformCollectionDelete(targetSchema, packageName, baseVariable, index);
            } else {
              // DELETE all: arr.DELETE
              return PackageVariableReferenceTransformer.transformCollectionDelete(targetSchema, packageName, baseVariable, null);
            }
            
          case "TRIM":
            if (methodArguments != null && !methodArguments.isEmpty()) {
              // TRIM with count: arr.TRIM(n)
              String trimCount = methodArguments.get(0).toPostgre(data);
              return PackageVariableReferenceTransformer.transformCollectionTrim(targetSchema, packageName, baseVariable, trimCount);
            } else {
              // TRIM default: arr.TRIM (removes 1 element)
              return PackageVariableReferenceTransformer.transformCollectionTrim(targetSchema, packageName, baseVariable, null);
            }
            
          case "EXISTS":
            if (methodArguments != null && !methodArguments.isEmpty()) {
              // EXISTS with index: arr.EXISTS(i)
              String index = methodArguments.get(0).toPostgre(data);
              String existsTemplate = PackageVariableReferenceTransformer.transformCollectionMethod(targetSchema, packageName, baseVariable, "EXISTS");
              return String.format(existsTemplate, index);
            } else {
              return "/* EXISTS requires an index argument */";
            }
            
          case "EXTEND":
            if (methodArguments != null && !methodArguments.isEmpty()) {
              // EXTEND with value: arr.EXTEND(value)
              String value = methodArguments.get(0).toPostgre(data);
              return PackageVariableReferenceTransformer.transformCollectionExtend(targetSchema, packageName, baseVariable, value);
            } else {
              // EXTEND default: arr.EXTEND
              return PackageVariableReferenceTransformer.transformCollectionExtend(targetSchema, packageName, baseVariable, null);
            }
            
          default:
            // Other collection methods (COUNT, FIRST, LAST) - use standard method
            return PackageVariableReferenceTransformer.transformCollectionMethod(targetSchema, packageName, baseVariable, collectionMethod);
        }
      }
    }
    
    // Regular collection method transformation (function-local collections)
    
    switch (collectionMethod.toUpperCase()) {
      case "COUNT":
        // Oracle: arr.COUNT → PostgreSQL: array_length(arr, 1)
        return "array_length(" + arrayExpression + ", 1)";
        
      case "FIRST":
        // Oracle: arr.FIRST → PostgreSQL: 1 (arrays are 1-indexed in PostgreSQL)
        return "1";
        
      case "LAST":
        // Oracle: arr.LAST → PostgreSQL: array_length(arr, 1)
        return "array_length(" + arrayExpression + ", 1)";
        
      case "EXISTS":
        // Oracle: arr.EXISTS(i) → PostgreSQL: (i >= 1 AND i <= array_length(arr, 1))
        if (methodArguments != null && !methodArguments.isEmpty()) {
          String index = methodArguments.get(0).toPostgre(data);
          return "(" + index + " >= 1 AND " + index + " <= array_length(" + arrayExpression + ", 1))";
        } else {
          return "/* EXISTS requires an index argument */";
        }
        
      case "NEXT":
        // Oracle: arr.NEXT(i) → PostgreSQL: (CASE WHEN i < array_length(arr, 1) THEN i + 1 ELSE NULL END)
        if (methodArguments != null && !methodArguments.isEmpty()) {
          String index = methodArguments.get(0).toPostgre(data);
          return "(CASE WHEN " + index + " < array_length(" + arrayExpression + ", 1) THEN " + index + " + 1 ELSE NULL END)";
        } else {
          return "/* NEXT requires an index argument */";
        }
        
      case "PRIOR":
        // Oracle: arr.PRIOR(i) → PostgreSQL: (CASE WHEN i > 1 THEN i - 1 ELSE NULL END)
        if (methodArguments != null && !methodArguments.isEmpty()) {
          String index = methodArguments.get(0).toPostgre(data);
          return "(CASE WHEN " + index + " > 1 THEN " + index + " - 1 ELSE NULL END)";
        } else {
          return "/* PRIOR requires an index argument */";
        }
        
      case "LIMIT":
        // Oracle: arr.LIMIT → PostgreSQL: No direct equivalent, return a comment
        return "/* LIMIT - no direct PostgreSQL equivalent for dynamic array limits */";
        
      default:
        return "/* Unknown collection method: " + collectionMethod + " */";
    }
  }

  /**
   * Transform collection constructor to PostgreSQL array syntax.
   * Oracle: t_numbers(1, 2, 3) → PostgreSQL: ARRAY[1, 2, 3]
   * Falls back to array indexing if not actually a collection constructor.
   * SPECIAL HANDLING: Detects table of records variables and transforms to JSONB operations.
   */
  private String transformCollectionConstructorToPostgreSQL(Everything data) {
    if (constructorName == null) {
      return "/* INVALID COLLECTION CONSTRUCTOR */";
    }
    
    // FIRST: Check if this is a table of records variable access
    if (isTableOfRecordsVariable(constructorName, data)) {
      return transformTableOfRecordsAccess(constructorName, constructorArguments, data);
    }
    
    // SECOND: Check if this is actually a collection type constructor
    Function currentFunction = getCurrentFunctionFromContext();
    if (SchemaResolutionUtils.isCollectionTypeConstructor(data, constructorName, currentFunction)) {
      // Use the Everything.transformCollectionConstructor method for proper transformation
      return SchemaResolutionUtils.transformCollectionConstructor(data, constructorName, constructorArguments, currentFunction);
    } else {
      // This is not a collection constructor - it's probably array indexing
      // Fall back to array indexing transformation: v_arr(i) → v_arr[i]
      if (constructorArguments != null && constructorArguments.size() == 1) {
        // Single argument - treat as array indexing
        String indexString = constructorArguments.get(0).toPostgre(data);
        return constructorName + "[" + indexString + "]";
      } else {
        // Multiple arguments but not a collection constructor - this might be a function call
        StringBuilder sb = new StringBuilder();
        sb.append(constructorName).append("(");
        if (constructorArguments != null) {
          for (int i = 0; i < constructorArguments.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(constructorArguments.get(i).toPostgre(data));
          }
        }
        sb.append(")");
        return sb.toString();
      }
    }
  }

  /**
   * Transform Oracle array indexing to PostgreSQL array indexing.
   * Oracle uses parentheses: arr(i), PostgreSQL uses brackets: arr[i]
   */
  private String transformArrayIndexingToPostgreSQL(Everything data) {
    if (arrayVariable == null || indexExpression == null) {
      return "/* INVALID ARRAY INDEXING */";
    }
    
    String indexString = indexExpression.toPostgre(data);
    
    // Check if this is a package collection variable
    if (PackageVariableReferenceTransformer.isPackageVariableReference(arrayVariable, data)) {
      OraclePackage pkg = PackageVariableReferenceTransformer.findContainingPackage(arrayVariable, data);
      if (pkg != null) {
        String dataType = PackageVariableReferenceTransformer.getPackageVariableDataType(arrayVariable, pkg);
        
        // For package collections, we need to get the element type
        String elementType = extractElementDataType(dataType);
        
        // Transform package collection element access to direct table access
        return PackageVariableReferenceTransformer.transformCollectionElementRead(
            pkg.getSchema(), pkg.getName(), arrayVariable, elementType, indexString);
      }
    }
    
    // Regular array indexing transformation (function-local collections)
    // Transform Oracle arr(i) to PostgreSQL arr[i]
    return arrayVariable + "[" + indexString + "]";
  }
  
  /**
   * Extract the element data type from a collection data type.
   */
  private String extractElementDataType(String collectionType) {
    if (collectionType == null) {
      return "text";
    }
    
    // Handle VARRAY and TABLE OF types
    if (collectionType.toUpperCase().contains("VARRAY") || collectionType.toUpperCase().contains("TABLE")) {
      // Look for "OF type" pattern
      int ofIndex = collectionType.toUpperCase().indexOf(" OF ");
      if (ofIndex != -1) {
        String elementType = collectionType.substring(ofIndex + 4).trim();
        // Remove any trailing size specifications
        if (elementType.contains("(")) {
          elementType = elementType.substring(0, elementType.indexOf("(")).trim();
        }
        return elementType;
      }
    }
    
    return "text"; // Default fallback
  }

  
  /**
   * Extract the base variable name from a PostgreSQL expression.
   * This is used to identify package collection variables.
   */
  private String extractBaseVariableName(String expression) {
    if (expression == null || expression.trim().isEmpty()) {
      return null;
    }
    
    String trimmed = expression.trim();
    
    // Handle function calls - extract variable name from function parameters
    if (trimmed.startsWith("sys.get_package_collection(")) {
      // This is already a transformed package collection - extract the variable name
      // Expected format: sys.get_package_collection('package', 'variable')
      int firstQuote = trimmed.indexOf("'", trimmed.indexOf(','));
      if (firstQuote != -1) {
        int secondQuote = trimmed.indexOf("'", firstQuote + 1);
        if (secondQuote != -1) {
          return trimmed.substring(firstQuote + 1, secondQuote);
        }
      }
    }
    
    // Handle simple variable names (for function-local collections)
    if (trimmed.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
      return trimmed;
    }
    
    // Handle complex expressions - try to extract the first identifier
    if (trimmed.contains("[") || trimmed.contains("(") || trimmed.contains(".")) {
      // Extract the first identifier before any special characters
      int specialChar = Integer.MAX_VALUE;
      for (char c : new char[]{'[', '(', '.', ' ', '\t', '\n'}) {
        int pos = trimmed.indexOf(c);
        if (pos != -1 && pos < specialChar) {
          specialChar = pos;
        }
      }
      
      if (specialChar != Integer.MAX_VALUE && specialChar > 0) {
        String candidate = trimmed.substring(0, specialChar);
        if (candidate.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
          return candidate;
        }
      }
    }
    
    return null;
  }

  /**
   * Extract function name from an Expression that represents a function call.
   * This is a helper method to work with the generic Expression type.
   */
  private String extractFunctionNameFromExpression(Expression expr) {
    if (expr == null) {
      return null;
    }
    
    // Use toString() and extract the function name part
    String exprString = expr.toString();
    if (exprString == null || exprString.trim().isEmpty()) {
      return null;
    }
    
    // For simple function calls like "t_numbers", "string_array", etc.
    // Extract identifier before parentheses or whitespace
    String trimmed = exprString.trim();
    int parenIndex = trimmed.indexOf('(');
    if (parenIndex > 0) {
      String candidate = trimmed.substring(0, parenIndex).trim();
      if (isValidIdentifier(candidate)) {
        return candidate;
      }
    } else {
      // No parentheses - might be just an identifier
      if (isValidIdentifier(trimmed)) {
        return trimmed;
      }
    }
    
    return null;
  }
  
  /**
   * Check if a string is a valid Oracle identifier.
   */
  private boolean isValidIdentifier(String str) {
    if (str == null || str.isEmpty()) {
      return false;
    }
    return str.matches("[a-zA-Z_][a-zA-Z0-9_]*");
  }

  /**
   * Check if the argument list is simple enough to be a collection constructor.
   * Collection constructors should have simple expressions, not complex nested calls.
   */
  private boolean isSimpleArgumentList(List<Expression> arguments) {
    if (arguments == null || arguments.isEmpty()) {
      return true;
    }
    
    // For now, accept any argument list - we can add more sophisticated checks later
    // if needed to distinguish between function calls and collection constructors
    return arguments.size() <= 10; // Reasonable limit for collection constructors
  }

  /**
   * Look up a collection type definition in the Everything context.
   * Returns the collection type information if found, null otherwise.
   */
  private CollectionTypeInfo lookupCollectionTypeDefinition(String typeName, Everything data) {
    if (typeName == null || data == null) {
      return null;
    }
    
    // 1. Search in current function context first (highest priority for function-local types)
    Function currentFunction = getCurrentFunctionFromContext();
    if (currentFunction != null) {
      CollectionTypeInfo info = searchFunctionForCollectionType(currentFunction, typeName);
      if (info != null) return info;
    }
    
    // 2. Search in all functions for function-local collection types
    for (Function func : data.getAllFunctions()) {
      CollectionTypeInfo info = searchFunctionForCollectionType(func, typeName);
      if (info != null) return info;
    }
    
    // 3. Search in package specs
    for (OraclePackage pkg : data.getPackageSpecAst()) {
      CollectionTypeInfo info = searchPackageForCollectionType(pkg, typeName);
      if (info != null) return info;
    }
    
    // 4. Search in package bodies  
    for (OraclePackage pkg : data.getPackageBodyAst()) {
      CollectionTypeInfo info = searchPackageForCollectionType(pkg, typeName);
      if (info != null) return info;
    }
    
    return null;
  }

  /**
   * Search a specific function for collection type definitions.
   */
  private CollectionTypeInfo searchFunctionForCollectionType(Function func, String typeName) {
    if (func == null || typeName == null) {
      return null;
    }
    
    // Search VARRAY types
    for (VarrayType varray : func.getVarrayTypes()) {
      if (typeName.equalsIgnoreCase(varray.getName())) {
        return new CollectionTypeInfo(
          typeName, 
          "VARRAY", 
          varray.getDataType(),
          func.getSchema(), 
          func.getName()
        );
      }
    }
    
    // Search nested table types
    for (NestedTableType nestedTable : func.getNestedTableTypes()) {
      if (typeName.equalsIgnoreCase(nestedTable.getName())) {
        return new CollectionTypeInfo(
          typeName, 
          "TABLE", 
          nestedTable.getDataType(),
          func.getSchema(), 
          func.getName()
        );
      }
    }
    
    return null;
  }

  /**
   * Search a specific package for collection type definitions.
   */
  private CollectionTypeInfo searchPackageForCollectionType(OraclePackage pkg, String typeName) {
    if (pkg == null || typeName == null) {
      return null;
    }
    
    // Search VARRAY types
    for (VarrayType varray : pkg.getVarrayTypes()) {
      if (typeName.equalsIgnoreCase(varray.getName())) {
        return new CollectionTypeInfo(
          typeName, 
          "VARRAY", 
          varray.getDataType(),
          pkg.getSchema(), 
          pkg.getName()
        );
      }
    }
    
    // Search nested table types
    for (NestedTableType nestedTable : pkg.getNestedTableTypes()) {
      if (typeName.equalsIgnoreCase(nestedTable.getName())) {
        return new CollectionTypeInfo(
          typeName, 
          "TABLE", 
          nestedTable.getDataType(),
          pkg.getSchema(), 
          pkg.getName()
        );
      }
    }
    
    return null;
  }

  /**
   * Extract arguments from an expression that represents a function call.
   * This is a simplified approach to handle parsing limitations.
   * Returns empty list for empty constructors, single-element list for parsed expressions.
   */
  private List<Expression> extractArgumentsFromExpression(Expression expr) {
    List<Expression> arguments = new ArrayList<>();
    
    // For now, this is a simplified approach
    // In a proper implementation, we would parse the AST structure properly
    // This method exists to interface with the new Everything-based architecture
    
    String exprString = expr.toString();
    if (exprString != null && !exprString.trim().endsWith("()")) {
      // For non-empty constructors, we'll create a simple wrapper for the expression
      // This is a transitional approach until proper AST parsing is implemented
      arguments.add(expr);
    }
    
    return arguments;
  }
  
  /**
   * Transform a collection constructor that was incorrectly parsed as array indexing.
   * This handles the parsing limitation where local_array('a','b') gets parsed as array[index].
   */
  private String transformCollectionConstructorFromArrayIndexing(CollectionTypeInfo typeInfo, Everything data) {
    if (indexExpression == null) {
      // Empty constructor
      String baseType = typeInfo.getDataType().toPostgre(data);
      return "ARRAY[]::" + baseType + "[]";
    }
    
    // Due to parsing limitations, we only have the first argument in indexExpression
    // Transform it as a single-argument constructor
    String firstArg = indexExpression.toPostgre(data);
    return "ARRAY[" + firstArg + "]";
  }
  

  /**
   * Common logic to transform collection constructor from expression string.
   */
  private String transformCollectionConstructorFromExpression(CollectionTypeInfo typeInfo, String exprString, Everything data) {
    if (exprString != null && exprString.trim().endsWith("()")) {
      // Handle empty constructor: t_numbers() → ARRAY[]::NUMERIC[]
      String baseType = typeInfo.getDataType().toPostgre(data);
      return "ARRAY[]::" + baseType + "[]";
    }
    
    // For constructors with arguments, we need to parse them from the expression string
    // This is a simplified approach - in a full implementation, we'd want to parse
    // the arguments properly from the AST
    if (exprString != null && exprString.contains("(") && !exprString.trim().endsWith("()")) {
      // Extract the content between parentheses
      int startParen = exprString.indexOf('(');
      int endParen = exprString.lastIndexOf(')');
      if (startParen >= 0 && endParen > startParen) {
        String argsString = exprString.substring(startParen + 1, endParen).trim();
        if (!argsString.isEmpty()) {
          // Handle constructor with arguments: t_numbers(1, 2, 3) → ARRAY[1, 2, 3]
          return "ARRAY[" + argsString + "]";
        }
      }
    }
    
    // Fallback: empty array with proper type
    String baseType = typeInfo.getDataType().toPostgre(data);
    return "ARRAY[]::" + baseType + "[]";
  }

  /**
   * Check if this expression represents record field access rather than a collection method.
   * This distinguishes between record.field (field access) and collection.method() (collection methods).
   */
  private boolean isRecordFieldAccess(Everything data) {
    if (collectionMethod == null || childExpression == null) {
      return false;
    }
    
    // Extract the base variable name from the child expression
    String baseVariableName = extractBaseVariableName(childExpression.toPostgre(data));
    if (baseVariableName == null) {
      return false;
    }
    
    // Get current function/procedure context to check for record types
    Function currentFunction = getCurrentFunctionFromContext();
    if (currentFunction != null) {
      // Check if this variable is declared as a record type in the current function
      if (isVariableOfRecordType(currentFunction, baseVariableName, collectionMethod)) {        
        return true;
      }
    }
    
    // Check procedure context as well (if available)
    Procedure currentProcedure = getCurrentProcedureFromContext();
    if (currentProcedure != null) {
      // Check if this variable is declared as a record type in the current procedure
      if (isVariableOfRecordTypeInProcedure(currentProcedure, baseVariableName, collectionMethod)) {        
        return true;
      }
    }
    
    return false;
  }

  /**
   * Check if a variable is declared as a record type and the field exists in that record type.
   */
  private boolean isVariableOfRecordType(Function function, String variableName, String fieldName) {
    // Check function variables for record type declarations
    if (function.getVariables() != null) {
      for (me.christianrobert.ora2postgre.plsql.ast.Variable variable : function.getVariables()) {
        if (variable.getName().equalsIgnoreCase(variableName)) {
          // Check if this variable's data type is a custom type (potential record type)
          me.christianrobert.ora2postgre.plsql.ast.DataTypeSpec dataType = variable.getDataType();
          if (dataType.getCustumDataType() != null) {
            String customTypeName = dataType.getCustumDataType();
            
            // Check if this custom type is a record type in the current function
            for (me.christianrobert.ora2postgre.plsql.ast.RecordType recordType : function.getRecordTypes()) {
              if (recordType.getName().equalsIgnoreCase(customTypeName)) {
                // Check if the field exists in this record type
                return recordTypeHasField(recordType, fieldName);
              }
            }
          }
        }
      }
    }
    
    return false;
  }

  /**
   * Check if a variable is declared as a record type in a procedure and the field exists in that record type.
   */
  private boolean isVariableOfRecordTypeInProcedure(Procedure procedure, String variableName, String fieldName) {
    // Check procedure variables for record type declarations
    if (procedure.getVariables() != null) {
      for (me.christianrobert.ora2postgre.plsql.ast.Variable variable : procedure.getVariables()) {
        if (variable.getName().equalsIgnoreCase(variableName)) {
          // Check if this variable's data type is a custom type (potential record type)
          me.christianrobert.ora2postgre.plsql.ast.DataTypeSpec dataType = variable.getDataType();
          if (dataType.getCustumDataType() != null) {
            String customTypeName = dataType.getCustumDataType();
            
            // Check if this custom type is a record type in the current procedure
            for (me.christianrobert.ora2postgre.plsql.ast.RecordType recordType : procedure.getRecordTypes()) {
              if (recordType.getName().equalsIgnoreCase(customTypeName)) {
                // Check if the field exists in this record type
                return recordTypeHasField(recordType, fieldName);
              }
            }
          }
        }
      }
    }
    
    return false;
  }

  /**
   * Check if a record type has a specific field.
   */
  private boolean recordTypeHasField(me.christianrobert.ora2postgre.plsql.ast.RecordType recordType, String fieldName) {
    if (recordType.getFields() == null) {
      return false;
    }
    
    for (me.christianrobert.ora2postgre.plsql.ast.RecordType.RecordField field : recordType.getFields()) {
      if (field.getName().equalsIgnoreCase(fieldName)) {
        return true;
      }
    }
    
    return false;
  }

  /**
   * Transform record field access to PostgreSQL composite type field access.
   * Oracle: record_var.field_name → PostgreSQL: (record_var).field_name
   */
  private String transformRecordFieldAccess(Everything data) {
    if (collectionMethod == null || childExpression == null) {
      return "/* INVALID RECORD FIELD ACCESS */";
    }
    
    String baseExpression = childExpression.toPostgre(data);
    
    // PostgreSQL composite type field access syntax: (composite_value).field_name
    return "(" + baseExpression + ")." + collectionMethod.toLowerCase();
  }

  /**
   * Check if a variable is declared as a table of records type.
   * Uses TransformationContext first, falls back to Everything data scan.
   */
  private boolean isTableOfRecordsVariable(String varName, Everything data) {
    if (varName == null) {
      return false;
    }
    
    // Try to get transformation context first
    TransformationContext context = transformationContext != null ? transformationContext : TransformationContext.getTestInstance();
    
    // If we have context, use it
    if (context != null) {
      // Check procedure context
      Procedure currentProcedure = context.getCurrentProcedure();
      if (currentProcedure != null && isVariableTableOfRecordsInProcedure(currentProcedure, varName)) {
        return true;
      }
      
      // Check function context
      Function currentFunction = context.getCurrentFunction();
      if (currentFunction != null && isVariableTableOfRecordsInFunction(currentFunction, varName)) {
        return true;
      }
    }
    
    // FALLBACK: Search all packages for table of records variables with this name
    if (data != null) {
      // Check package bodies (which contain procedure/function implementations)
      if (data.getPackageBodyAst() != null) {
        for (OraclePackage pkg : data.getPackageBodyAst()) {
          // Check package procedures
          if (pkg.getProcedures() != null) {
            for (Procedure procedure : pkg.getProcedures()) {
              if (isVariableTableOfRecordsInProcedure(procedure, varName)) {
                return true;
              }
            }
          }
          
          // Check package functions  
          if (pkg.getFunctions() != null) {
            for (Function function : pkg.getFunctions()) {
              if (isVariableTableOfRecordsInFunction(function, varName)) {
                return true;
              }
            }
          }
        }
      }
    }
    
    return false;
  }

  /**
   * Check if a variable is a table of records type in a procedure.
   */
  private boolean isVariableTableOfRecordsInProcedure(Procedure procedure, String varName) {
    if (procedure.getVariables() == null) {
      return false;
    }
    
    for (me.christianrobert.ora2postgre.plsql.ast.Variable variable : procedure.getVariables()) {
      if (variable.getName().equalsIgnoreCase(varName)) {
        return variable.isTableOfRecords();
      }
    }
    
    return false;
  }

  /**
   * Check if a variable is a table of records type in a function.
   */
  private boolean isVariableTableOfRecordsInFunction(Function function, String varName) {
    if (function.getVariables() == null) {
      return false;
    }
    
    for (me.christianrobert.ora2postgre.plsql.ast.Variable variable : function.getVariables()) {
      if (variable.getName().equalsIgnoreCase(varName)) {
        return variable.isTableOfRecords();
      }
    }
    
    return false;
  }

  /**
   * Transform table of records access to PostgreSQL JSONB operations.
   * Handles both assignment target and field access patterns.
   */
  private String transformTableOfRecordsAccess(String collectionName, List<Expression> arguments, Everything data) {
    if (arguments == null || arguments.isEmpty()) {
      return "/* INVALID TABLE OF RECORDS ACCESS - NO INDEX */";
    }
    
    // Get the index expression (should be single argument for l_products(100))
    String indexValue = arguments.get(0).toPostgre(data);
    
    // Get record type name for proper composite type casting
    String recordTypeName = getRecordTypeNameForTableOfRecords(collectionName, data);
    
    if (recordTypeName != null) {
      // Transform: collection(index) → jsonb_populate_record(NULL::composite_type, collection->'index')
      return String.format("jsonb_populate_record(NULL::%s, %s->'%s')", 
          recordTypeName, collectionName, indexValue);
    } else {
      // Fallback without explicit type casting
      return String.format("(%s->'%s')::jsonb", collectionName, indexValue);
    }
  }

  /**
   * Get the qualified record type name for a table of records collection.
   * Reuses the same logic as other classes for consistency.
   */
  private String getRecordTypeNameForTableOfRecords(String collectionName, Everything data) {
    TransformationContext context = transformationContext != null ? transformationContext : TransformationContext.getTestInstance();
    
    // If we have context, try it first
    if (context != null) {
      // Check procedure context
      Procedure currentProcedure = context.getCurrentProcedure();
      if (currentProcedure != null) {
        for (me.christianrobert.ora2postgre.plsql.ast.Variable variable : currentProcedure.getVariables()) {
          if (variable.getName().equalsIgnoreCase(collectionName) && variable.isTableOfRecords()) {
            String recordTypeName = variable.getRecordTypeName();
            if (recordTypeName != null) {
              return buildQualifiedRecordTypeName(recordTypeName, currentProcedure, data);
            }
          }
        }
      }
      
      // Check function context
      Function currentFunction = context.getCurrentFunction();
      if (currentFunction != null) {
        for (me.christianrobert.ora2postgre.plsql.ast.Variable variable : currentFunction.getVariables()) {
          if (variable.getName().equalsIgnoreCase(collectionName) && variable.isTableOfRecords()) {
            String recordTypeName = variable.getRecordTypeName();
            if (recordTypeName != null) {
              return buildQualifiedRecordTypeName(recordTypeName, currentFunction, data);
            }
          }
        }
      }
    }
    
    // FALLBACK: Search all packages for table of records variables with this name
    if (data != null) {
      // Check package bodies (which contain procedure/function implementations)
      if (data.getPackageBodyAst() != null) {
        for (OraclePackage pkg : data.getPackageBodyAst()) {
          // Check package procedures
          if (pkg.getProcedures() != null) {
            for (Procedure procedure : pkg.getProcedures()) {
              for (me.christianrobert.ora2postgre.plsql.ast.Variable variable : procedure.getVariables()) {
                if (variable.getName().equalsIgnoreCase(collectionName) && variable.isTableOfRecords()) {
                  String recordTypeName = variable.getRecordTypeName();
                  if (recordTypeName != null) {
                    return buildQualifiedRecordTypeName(recordTypeName, procedure, data);
                  }
                }
              }
            }
          }
          
          // Check package functions  
          if (pkg.getFunctions() != null) {
            for (Function function : pkg.getFunctions()) {
              for (me.christianrobert.ora2postgre.plsql.ast.Variable variable : function.getVariables()) {
                if (variable.getName().equalsIgnoreCase(collectionName) && variable.isTableOfRecords()) {
                  String recordTypeName = variable.getRecordTypeName();
                  if (recordTypeName != null) {
                    return buildQualifiedRecordTypeName(recordTypeName, function, data);
                  }
                }
              }
            }
          }
        }
      }
    }
    
    return null;
  }

  /**
   * Build qualified record type name using the same pattern as other classes.
   * Format: schema_package_routine_recordtype (lowercase with underscores)
   */
  private String buildQualifiedRecordTypeName(String recordTypeName, ExecutableRoutine routine, Everything data) {
    if (recordTypeName == null || routine == null) {
      return null;
    }
    
    // Build qualified name: schema_package_routine_recordtype
    StringBuilder qualifiedName = new StringBuilder();
    
    // Add schema (lowercase)
    String schema = routine.getSchema();
    if (schema == null && data != null && !data.getUserNames().isEmpty()) {
      // Use the first schema from the data context as fallback
      schema = data.getUserNames().get(0);
    }
    if (schema == null) {
      schema = "unknown_schema"; // Last resort fallback
    }
    qualifiedName.append(schema.toLowerCase());
    
    // Add package/parent context
    if (routine.getParentPackage() != null) {
      qualifiedName.append("_").append(routine.getParentPackage().getName().toLowerCase());
    } else if (routine.getParentType() != null) {
      qualifiedName.append("_").append(routine.getParentType().getName().toLowerCase());
    } else {
      // Standalone routine - no package context
    }
    
    // Add routine name
    qualifiedName.append("_").append(routine.getName().toLowerCase());
    
    // Add record type name
    qualifiedName.append("_").append(recordTypeName.toLowerCase());
    
    return qualifiedName.toString();
  }

  /**
   * Check if atom text contains table of records field access pattern.
   * Pattern: l_products(100).prod_id
   */
  private boolean containsTableOfRecordsFieldAccess(String atomText, Everything data) {
    if (atomText == null) {
      return false;
    }
    
    // Look for pattern: variable_name(index).field_name
    if (atomText.contains("(") && atomText.contains(").")) {
      int parenIndex = atomText.indexOf('(');
      if (parenIndex > 0) {
        String variableName = atomText.substring(0, parenIndex);
        return isTableOfRecordsVariable(variableName, data);
      }
    }
    
    return false;
  }

  /**
   * Transform table of records field access from atom text.
   * Pattern: l_products(100).prod_id → (l_products->'100'->>'prod_id')::NUMERIC
   */
  private String transformTableOfRecordsFieldAccess(String atomText, Everything data) {
    if (atomText == null) {
      return "/* INVALID TABLE OF RECORDS FIELD ACCESS */";
    }
    
    // Parse the pattern: variable_name(index).field_name
    int parenStartIndex = atomText.indexOf('(');
    int parenEndIndex = atomText.indexOf(')', parenStartIndex);
    int dotIndex = atomText.indexOf('.', parenEndIndex);
    
    if (parenStartIndex > 0 && parenEndIndex > parenStartIndex && dotIndex > parenEndIndex) {
      String variableName = atomText.substring(0, parenStartIndex);
      String indexValue = atomText.substring(parenStartIndex + 1, parenEndIndex);
      String fieldName = atomText.substring(dotIndex + 1);
      
      // Clean up field name (remove any additional whitespace or special characters)
      fieldName = fieldName.trim();
      
      // Transform to PostgreSQL JSONB field access
      // Pattern: (variable->'index'->>'field_name')::TYPE
      return String.format("(%s->'%s'->>'%s')", variableName, indexValue, fieldName);
    }
    
    return "/* INVALID TABLE OF RECORDS FIELD ACCESS PATTERN */";
  }
}