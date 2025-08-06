package me.christianrobert.ora2postgre.services;

import me.christianrobert.ora2postgre.plsql.ast.Function;
import me.christianrobert.ora2postgre.plsql.ast.Procedure;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service for managing transformation context during AST processing.
 * 
 * This service tracks the current function context during Oracle PL/SQL to PostgreSQL
 * transformation. Unlike the Everything class which stores static extracted data,
 * this service manages dynamic transformation state that changes as we process
 * different parts of the AST.
 * 
 * The current function context is crucial for:
 * - Collection type resolution (function-local vs package-level types)
 * - Variable vs function distinction for array indexing syntax
 * - Semantic analysis that depends on the current scope
 * 
 * This follows the same pattern as CTETrackingService for consistency.
 */
@ApplicationScoped
public class TransformationContext {
    
    /**
     * The currently active function during transformation.
     * This changes as we enter and exit different function contexts.
     */
    private Function currentFunction = null;
    
    /**
     * The currently active procedure during transformation.
     * This changes as we enter and exit different procedure contexts.
     */
    private Procedure currentProcedure = null;
    
    /**
     * Static test instance for unit tests that don't use CDI container.
     * This enables testing of AST classes that are created through parsing
     * rather than CDI injection.
     */
    private static TransformationContext testInstance = null;
    
    /**
     * Gets the current function context.
     * @return The current function being processed, or null if no function context is set
     */
    public Function getCurrentFunction() {
        return currentFunction;
    }
    
    /**
     * Sets the current function context.
     * @param function The function to set as current context, or null to clear context
     */
    public void setCurrentFunction(Function function) {
        this.currentFunction = function;
    }
    
    /**
     * Clears the current function context.
     */
    public void clearCurrentFunction() {
        this.currentFunction = null;
    }
    
    /**
     * Gets the current procedure context.
     * @return The current procedure being processed, or null if no procedure context is set
     */
    public Procedure getCurrentProcedure() {
        return currentProcedure;
    }
    
    /**
     * Sets the current procedure context.
     * @param procedure The procedure to set as current context, or null to clear context
     */
    public void setCurrentProcedure(Procedure procedure) {
        this.currentProcedure = procedure;
    }
    
    /**
     * Clears the current procedure context.
     */
    public void clearCurrentProcedure() {
        this.currentProcedure = null;
    }
    
    /**
     * Sets a static test instance for use in unit tests.
     * This allows tests to provide transformation context when CDI is not available.
     * 
     * @param instance The test instance to use, or null to clear
     */
    public static void setTestInstance(TransformationContext instance) {
        testInstance = instance;
    }
    
    /**
     * Gets the static test instance for use when CDI is not available.
     * @return The test instance, or null if none has been set
     */
    public static TransformationContext getTestInstance() {
        return testInstance;
    }
    
    /**
     * Executes a block of code with a specific function context, automatically
     * restoring the previous context when done.
     * 
     * @param function The function context to set during execution
     * @param action The action to execute with the function context
     */
    public void withFunctionContext(Function function, Runnable action) {
        Function previousFunction = this.currentFunction;
        try {
            this.currentFunction = function;
            action.run();
        } finally {
            this.currentFunction = previousFunction;
        }
    }
    
    /**
     * Executes a block of code with a specific procedure context, automatically
     * restoring the previous context when done.
     * 
     * @param procedure The procedure context to set during execution
     * @param action The action to execute with the procedure context
     */
    public void withProcedureContext(Procedure procedure, Runnable action) {
        Procedure previousProcedure = this.currentProcedure;
        try {
            this.currentProcedure = procedure;
            action.run();
        } finally {
            this.currentProcedure = previousProcedure;
        }
    }
    
    /**
     * Checks if there is an active function context.
     * @return true if a function context is currently set, false otherwise
     */
    public boolean hasCurrentFunction() {
        return currentFunction != null;
    }
    
    /**
     * Checks if there is an active procedure context.
     * @return true if a procedure context is currently set, false otherwise
     */
    public boolean hasCurrentProcedure() {
        return currentProcedure != null;
    }
    
    @Override
    public String toString() {
        return "TransformationContext{" +
                "currentFunction=" + (currentFunction != null ? currentFunction.getName() : "null") +
                ", currentProcedure=" + (currentProcedure != null ? currentProcedure.getName() : "null") +
                '}';
    }
}