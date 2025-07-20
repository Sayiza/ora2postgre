package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.helpers.CollectionTypeInfo;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PackageVariableReferenceTransformer;

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
      // NEW: Check for collection constructor before standard function processing  
      String functionName = extractFunctionNameFromExpression(standardFunction);
      if (functionName != null) {
        CollectionTypeInfo typeInfo = lookupCollectionTypeDefinition(functionName, data);
        if (typeInfo != null) {
          return transformTypeAwareCollectionConstructor(typeInfo, data);
        }
      }
      // Continue with normal function processing if not a collection constructor
      return standardFunction.toPostgre(data);
    } else if (isAtom()) {
      // NEW: Check if atom represents a collection constructor
      String functionName = extractFunctionNameFromExpression(atom);
      if (functionName != null) {
        CollectionTypeInfo typeInfo = lookupCollectionTypeDefinition(functionName, data);
        if (typeInfo != null) {
          return transformAtomCollectionConstructor(typeInfo, atom, data);
        }
      }
      
      return atom.toPostgre(data);
    } else if (isImplicitCursorExpression()) {
      return implicitCursorExpression.toPostgre(data);
    } else if (isCollectionMethodCall()) {
      // Transform Oracle collection methods to PostgreSQL function calls
      return transformCollectionMethodToPostgreSQL(data);
    } else if (isArrayIndexing()) {
      // SEMANTIC LAYER: Check if this is actually a collection constructor parsed as array indexing
      if (arrayVariable != null) {
        CollectionTypeInfo typeInfo = lookupCollectionTypeDefinition(arrayVariable, data);
        if (typeInfo != null) {
          // This is a collection constructor, not array indexing
          return transformCollectionConstructorFromArrayIndexing(typeInfo, data);
        }
      }
      
      // Transform Oracle array indexing to PostgreSQL array indexing
      return transformArrayIndexingToPostgreSQL(data);
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
    Function currentFunction = data.getCurrentFunction();
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
   * Transform a collection constructor to PostgreSQL array syntax using type information.
   * This replaces the old heuristic-based transformation with proper type-aware logic.
   */
  private String transformTypeAwareCollectionConstructor(CollectionTypeInfo typeInfo, Everything data) {
    // For now, check if the expression string indicates an empty constructor
    String exprString = standardFunction.toString();
    return transformCollectionConstructorFromExpression(typeInfo, exprString, data);
  }
  
  /**
   * Transform a collection constructor from an atom expression.
   */
  private String transformAtomCollectionConstructor(CollectionTypeInfo typeInfo, Expression atomExpr, Everything data) {
    String exprString = atomExpr.toString();
    return transformCollectionConstructorFromExpression(typeInfo, exprString, data);
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
}