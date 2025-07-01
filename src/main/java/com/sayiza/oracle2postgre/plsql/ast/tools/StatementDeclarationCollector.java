package com.sayiza.oracle2postgre.plsql.ast.tools;

import com.sayiza.oracle2postgre.global.Everything;
import com.sayiza.oracle2postgre.plsql.ast.ForLoopStatement;
import com.sayiza.oracle2postgre.plsql.ast.Statement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for collecting necessary declarations from statement lists.
 * This handles FOR loop variable declarations and can be extended for other types
 * of statements that require declarations in the DECLARE section.
 */
public class StatementDeclarationCollector {

    /**
     * Recursively collects all necessary declarations from a list of statements.
     * Currently handles FOR loop variable declarations and can be extended for other statement types.
     * Avoids duplicate declarations for variables with the same name.
     * 
     * @param statements the list of statements to analyze
     * @param data the Everything context for data type resolution
     * @return StringBuilder containing all necessary declarations
     */
    public static StringBuilder collectNecessaryDeclarations(List<Statement> statements, Everything data) {
        Set<String> declaredVariables = new HashSet<>();
        return collectNecessaryDeclarations(statements, data, declaredVariables);
    }

    /**
     * Internal recursive method that tracks declared variables to avoid duplicates.
     * 
     * @param statements the list of statements to analyze
     * @param data the Everything context for data type resolution
     * @param declaredVariables set of already declared variable names
     * @return StringBuilder containing all necessary declarations
     */
    private static StringBuilder collectNecessaryDeclarations(List<Statement> statements, Everything data, Set<String> declaredVariables) {
        StringBuilder declarations = new StringBuilder();
        
        for (Statement statement : statements) {
            if (statement instanceof ForLoopStatement) {
                ForLoopStatement forLoop = (ForLoopStatement) statement;
                
                // Get the variable name from the FOR loop
                String variableName = forLoop.getNameRef();
                
                // Only add declaration if we haven't seen this variable name before
                if (!declaredVariables.contains(variableName)) {
                    declarations.append(forLoop.toPostgreDeclaration(data));
                    declaredVariables.add(variableName);
                }
                
                // Recursively check nested statements within the FOR loop
                declarations.append(collectNecessaryDeclarations(forLoop.getStatements(), data, declaredVariables));
            }
            // TODO: Add handling for other statement types that might contain nested statements
            // such as IF statements, WHILE loops, etc. when they are implemented
        }
        
        return declarations;
    }
}