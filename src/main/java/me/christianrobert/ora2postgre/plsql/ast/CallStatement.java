package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PackageVariableReferenceTransformer;

import java.util.List;
import java.util.ArrayList;

/**
 * AST node representing an Oracle call statement (procedure or function call).
 * Handles transformation to PostgreSQL CALL statements with proper schema resolution.
 * 
 * Examples:
 * - add2gXpackagevar; → CALL USER_ROBERT.MINITEST_add2gxpackagevar();
 * - pkg.procedure(arg); → CALL SCHEMA.PKG_procedure(arg);
 * - result := pkg.function(arg); → SELECT SCHEMA.PKG_function(arg) INTO result;
 */
public class CallStatement extends Statement {
    
    private String routineName;           // The procedure/function name (e.g., "add2gXpackagevar")
    private String packageName;           // The package name if specified (e.g., "minitest")
    private String schema;                // Resolved schema name
    private List<Expression> arguments;   // Arguments passed to the procedure/function
    private boolean isFunction;           // true for functions, false for procedures
    private Expression returnTarget;      // Target variable for function return (if any)
    
    // Context for resolving package-less calls
    private String callingPackage;        // Package that contains this call statement
    private String callingSchema;         // Schema that contains this call statement
    
    /**
     * Constructor for procedure call without arguments.
     * 
     * @param routineName The name of the procedure to call
     */
    public CallStatement(String routineName) {
        this.routineName = routineName;
        this.packageName = null;
        this.schema = null;
        this.arguments = new ArrayList<>();
        this.isFunction = false;
        this.returnTarget = null;
    }
    
    /**
     * Constructor for procedure/function call with package and arguments.
     * 
     * @param routineName The name of the procedure/function to call
     * @param packageName The package name containing the routine
     * @param arguments List of arguments to pass
     * @param isFunction true if this is a function call, false for procedure
     */
    public CallStatement(String routineName, String packageName, List<Expression> arguments, boolean isFunction) {
        this.routineName = routineName;
        this.packageName = packageName;
        this.schema = null;
        this.arguments = arguments != null ? arguments : new ArrayList<>();
        this.isFunction = isFunction;
        this.returnTarget = null;
    }
    
    /**
     * Constructor for function call with return target.
     * 
     * @param routineName The name of the function to call
     * @param packageName The package name containing the function
     * @param arguments List of arguments to pass
     * @param returnTarget The target variable for the function return value
     */
    public CallStatement(String routineName, String packageName, List<Expression> arguments, Expression returnTarget) {
        this.routineName = routineName;
        this.packageName = packageName;
        this.schema = null;
        this.arguments = arguments != null ? arguments : new ArrayList<>();
        this.isFunction = true;
        this.returnTarget = returnTarget;
    }
    
    // Getters and setters
    public String getRoutineName() { return routineName; }
    public void setRoutineName(String routineName) { this.routineName = routineName; }
    
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    
    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }
    
    public List<Expression> getArguments() { return arguments; }
    public void setArguments(List<Expression> arguments) { this.arguments = arguments; }
    
    public boolean isFunction() { return isFunction; }
    public void setFunction(boolean function) { isFunction = function; }
    
    public Expression getReturnTarget() { return returnTarget; }
    public void setReturnTarget(Expression returnTarget) { this.returnTarget = returnTarget; }
    
    // Calling context getters and setters
    public String getCallingPackage() { return callingPackage; }
    public void setCallingPackage(String callingPackage) { this.callingPackage = callingPackage; }
    
    public String getCallingSchema() { return callingSchema; }
    public void setCallingSchema(String callingSchema) { this.callingSchema = callingSchema; }
    
    @Override
    public <T> T accept(PlSqlAstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CallStatement{");
        if (packageName != null) {
            sb.append("package=").append(packageName).append(".");
        }
        sb.append("routine=").append(routineName);
        sb.append(", args=").append(arguments.size());
        sb.append(", isFunction=").append(isFunction);
        if (returnTarget != null) {
            sb.append(", returnTarget=").append(returnTarget);
        }
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Transform Oracle call statement to PostgreSQL syntax.
     * 
     * @param data Everything context for schema resolution and transformation
     * @return PostgreSQL CALL statement or SELECT statement for functions
     */
    @Override
    public String toPostgre(Everything data) {
        StringBuilder b = new StringBuilder();
        b.append(data.getIntendation());
        
        // Resolve schema if not already set
        if (schema == null) {
            schema = resolveSchema(data);
        }
        
        // Transform arguments, handling package variables
        List<String> transformedArgs = new ArrayList<>();
        for (Expression arg : arguments) {
            String transformedArg = transformArgument(arg, data);
            transformedArgs.add(transformedArg);
        }
        
        // Generate PostgreSQL statement
        if (isFunction && returnTarget != null) {
            // Function call with return target: SELECT function_name(args) INTO target
            b.append("SELECT ");
            b.append(buildPostgreRoutineName());
            b.append("(");
            b.append(String.join(", ", transformedArgs));
            b.append(") INTO ");
            b.append(returnTarget.toPostgre(data));
            b.append(";\n");
        } else if (isFunction) {
            // Function call without return target: SELECT function_name(args)
            b.append("SELECT ");
            b.append(buildPostgreRoutineName());
            b.append("(");
            b.append(String.join(", ", transformedArgs));
            b.append(");\n");
        } else {
            // Procedure call: CALL procedure_name(args)
            b.append("CALL ");
            b.append(buildPostgreRoutineName());
            b.append("(");
            b.append(String.join(", ", transformedArgs));
            b.append(");\n");
        }
        
        return b.toString();
    }
    
    /**
     * Build the PostgreSQL routine name with schema prefix.
     * Format: SCHEMA.PACKAGE_routine or SCHEMA.routine
     * 
     * @return Fully qualified PostgreSQL routine name
     */
    private String buildPostgreRoutineName() {
        StringBuilder name = new StringBuilder();
        
        // Add schema prefix
        if (schema != null) {
            name.append(schema.toUpperCase()).append(".");
        }
        
        // Add package prefix if present
        if (packageName != null) {
            name.append(packageName.toUpperCase()).append("_");
        }
        
        // Add routine name (lowercase following PostgreSQL convention)
        name.append(routineName.toLowerCase());
        
        return name.toString();
    }
    
    /**
     * Transform a single argument, handling package variables.
     * 
     * @param arg The argument expression to transform
     * @param data Everything context
     * @return Transformed argument string
     */
    private String transformArgument(Expression arg, Everything data) {
        // For now, use the basic expression transformation
        // Package variable transformation will be handled in the existing Expression.toPostgre() method
        // which already has the integration with PackageVariableReferenceTransformer
        return arg.toPostgre(data);
    }
    
    /**
     * Resolve the schema for this call statement using Everything context.
     * 
     * @param data Everything context for schema resolution
     * @return Resolved schema name
     */
    private String resolveSchema(Everything data) {
        // Get the current schema context - use calling schema if available
        String currentSchema = callingSchema != null ? callingSchema : "USER_ROBERT";
        if (!data.getPackageSpecAst().isEmpty()) {
            currentSchema = data.getPackageSpecAst().get(0).getSchema();
        }
        
        // If no explicit package specified, try calling package first
        if (packageName == null && callingPackage != null) {
            String resolvedSchema = data.lookupProcedureSchema(routineName, callingPackage, currentSchema);
            if (resolvedSchema != null) {
                // Found in calling package - set the package context
                this.packageName = callingPackage;
                this.isFunction = data.isFunction(routineName, callingPackage, resolvedSchema);
                return resolvedSchema;
            }
        }
        
        // Use the existing lookup methods to resolve schema
        String resolvedSchema = data.lookupProcedureSchema(routineName, packageName, currentSchema);
        
        if (resolvedSchema != null) {
            // Also update the isFunction flag based on the resolved information
            this.isFunction = data.isFunction(routineName, packageName, resolvedSchema);
            return resolvedSchema;
        }
        
        // If not found as procedure, try as function
        if (packageName != null) {
            // Search through package specs and bodies for functions
            for (OraclePackage pkg : data.getPackageSpecAst()) {
                if (pkg.getName().equalsIgnoreCase(packageName)) {
                    for (Function func : pkg.getFunctions()) {
                        if (func.getName().equalsIgnoreCase(routineName)) {
                            this.isFunction = true;
                            return pkg.getSchema();
                        }
                    }
                }
            }
            for (OraclePackage pkg : data.getPackageBodyAst()) {
                if (pkg.getName().equalsIgnoreCase(packageName)) {
                    for (Function func : pkg.getFunctions()) {
                        if (func.getName().equalsIgnoreCase(routineName)) {
                            this.isFunction = true;
                            return pkg.getSchema();
                        }
                    }
                }
            }
        } else {
            // Check standalone functions
            for (Function func : data.getStandaloneFunctionAst()) {
                if (func.getName().equalsIgnoreCase(routineName)) {
                    this.isFunction = true;
                    return func.getSchema();
                }
            }
            
            // Check standalone procedures
            for (Procedure proc : data.getStandaloneProcedureAst()) {
                if (proc.getName().equalsIgnoreCase(routineName)) {
                    this.isFunction = false;
                    return proc.getSchema();
                }
            }
        }
        
        // Default to current schema if not found
        return currentSchema;
    }
}