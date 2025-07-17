package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.OracleFunctionMapper;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PackageVariableReferenceTransformer;

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
            // If there's also a logical operation, append it (for cases like "expr > 0" where expr is multiset and "> 0" is operation)
            if (logicalOperation != null) {
                sb.append(" ").append(logicalOperation);
            }
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
            // If there's also a logical operation, append it (for cases like "expr > 0" where expr is multiset and "> 0" is operation)
            if (logicalOperation != null) {
                sb.append(" ").append(logicalOperation);
            }
        } else if (logicalOperation != null) {
            // If there's no multiset expression, logicalOperation might contain the raw text
            String rawText = logicalOperation;
            
            // Check if this is a simple package variable reference
            if (isSimpleVariableReference(rawText) && 
                PackageVariableReferenceTransformer.isPackageVariableReference(rawText.trim(), data)) {
                
                OraclePackage pkg = PackageVariableReferenceTransformer.findContainingPackage(rawText.trim(), data);
                if (pkg != null) {
                    String dataType = PackageVariableReferenceTransformer.getPackageVariableDataType(rawText.trim(), pkg);
                    
                    // Transform package variable reference to direct table access
                    if (PackageVariableReferenceTransformer.isCollectionType(dataType)) {
                        // Collection variables - return as array
                        sb.append(PackageVariableReferenceTransformer.transformRead(pkg.getName(), rawText.trim(), dataType));
                    } else {
                        // Regular package variables
                        sb.append(PackageVariableReferenceTransformer.transformRead(pkg.getName(), rawText.trim(), dataType));
                    }
                } else {
                    sb.append(rawText);
                }
            } else {
                sb.append(rawText);
            }
        }
        String result = sb.toString();

        return OracleFunctionMapper.getMappingIfIs2BeApplied(result);
    }
    
    /**
     * Check if the raw text represents a simple variable reference (not a function call or complex expression).
     * Simple variable references are just identifiers without parentheses, operators, or special characters.
     */
    private boolean isSimpleVariableReference(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = text.trim();
        
        // Check for function calls (contains parentheses)
        if (trimmed.contains("(") || trimmed.contains(")")) {
            return false;
        }
        
        // Check for operators that would indicate a complex expression
        if (trimmed.contains("+") || trimmed.contains("-") || trimmed.contains("*") || 
            trimmed.contains("/") || trimmed.contains("=") || trimmed.contains("<") || 
            trimmed.contains(">") || trimmed.contains("!") || trimmed.contains("||") ||
            trimmed.contains("AND") || trimmed.contains("OR") || trimmed.contains("NOT")) {
            return false;
        }
        
        // Check for collection indexing (would be handled elsewhere)
        if (trimmed.contains("[") || trimmed.contains("]")) {
            return false;
        }
        
        // Check if it's a valid identifier (letters, numbers, underscore)
        return trimmed.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }
}