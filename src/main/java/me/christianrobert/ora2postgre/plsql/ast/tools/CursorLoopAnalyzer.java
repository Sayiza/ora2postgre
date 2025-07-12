package me.christianrobert.ora2postgre.plsql.ast.tools;

import me.christianrobert.ora2postgre.plsql.ast.*;

import java.util.List;

/**
 * Utility class for analyzing and detecting cursor loop patterns in PL/SQL.
 * Identifies traditional Oracle cursor loops (OPEN/FETCH/CLOSE) that can be
 * transformed into cleaner PostgreSQL FOR loops.
 */
public class CursorLoopAnalyzer {

    /**
     * Information about a detected cursor loop pattern.
     */
    public static class CursorLoopInfo {
        private final String cursorName;
        private final List<String> fetchVariables;
        private final List<Statement> loopBodyStatements;
        private final OpenStatement openStatement;
        private final CloseStatement closeStatement;
        private final FetchStatement fetchStatement;
        private final ExitStatement exitStatement;

        public CursorLoopInfo(String cursorName, List<String> fetchVariables, 
                             List<Statement> loopBodyStatements,
                             OpenStatement openStatement, CloseStatement closeStatement,
                             FetchStatement fetchStatement, ExitStatement exitStatement) {
            this.cursorName = cursorName;
            this.fetchVariables = fetchVariables;
            this.loopBodyStatements = loopBodyStatements;
            this.openStatement = openStatement;
            this.closeStatement = closeStatement;
            this.fetchStatement = fetchStatement;
            this.exitStatement = exitStatement;
        }

        public String getCursorName() { return cursorName; }
        public List<String> getFetchVariables() { return fetchVariables; }
        public List<Statement> getLoopBodyStatements() { return loopBodyStatements; }
        public OpenStatement getOpenStatement() { return openStatement; }
        public CloseStatement getCloseStatement() { return closeStatement; }
        public FetchStatement getFetchStatement() { return fetchStatement; }
        public ExitStatement getExitStatement() { return exitStatement; }
    }

    /**
     * Analyzes a sequence of statements to detect the Oracle cursor loop pattern:
     * 1. OPEN cursor_name;
     * 2. LOOP
     * 3.   FETCH cursor_name INTO variables;
     * 4.   EXIT WHEN cursor_name%NOTFOUND;
     * 5.   [business logic statements]
     * 6. END LOOP;
     * 7. CLOSE cursor_name;
     * 
     * @param statements List of statements to analyze
     * @return CursorLoopInfo if pattern is detected, null otherwise
     */
    public static CursorLoopInfo detectCursorLoopPattern(List<Statement> statements) {
        if (statements == null || statements.size() < 3) {
            return null; // Need at least OPEN, LOOP, CLOSE
        }

        // Look for OPEN statement
        OpenStatement openStatement = null;
        LoopStatement loopStatement = null;
        CloseStatement closeStatement = null;
        int openIndex = -1;
        int loopIndex = -1;
        int closeIndex = -1;

        for (int i = 0; i < statements.size(); i++) {
            Statement stmt = statements.get(i);
            
            if (stmt instanceof OpenStatement && openStatement == null) {
                openStatement = (OpenStatement) stmt;
                openIndex = i;
            } else if (stmt instanceof LoopStatement && loopStatement == null && openStatement != null) {
                loopStatement = (LoopStatement) stmt;
                loopIndex = i;
            } else if (stmt instanceof CloseStatement && closeStatement == null && loopStatement != null) {
                closeStatement = (CloseStatement) stmt;
                closeIndex = i;
                break; // We found the complete pattern
            }
        }

        // Check if we have the basic structure
        if (openStatement == null || loopStatement == null || closeStatement == null) {
            return null;
        }

        // Check if they are in the correct order
        if (!(openIndex < loopIndex && loopIndex < closeIndex)) {
            return null;
        }

        // Verify they use the same cursor
        String cursorName = openStatement.getCursorName();
        if (!cursorName.equals(closeStatement.getCursorName())) {
            return null;
        }

        // Analyze the loop body for FETCH and EXIT WHEN patterns
        List<Statement> loopBody = loopStatement.getStatements();
        if (loopBody == null || loopBody.isEmpty()) {
            return null;
        }

        // Look for FETCH statement (should be first or early in loop)
        FetchStatement fetchStatement = null;
        ExitStatement exitStatement = null;
        
        for (Statement stmt : loopBody) {
            if (stmt instanceof FetchStatement && fetchStatement == null) {
                FetchStatement fetch = (FetchStatement) stmt;
                if (cursorName.equals(fetch.getCursorName())) {
                    fetchStatement = fetch;
                }
            } else if (stmt instanceof ExitStatement && exitStatement == null) {
                ExitStatement exit = (ExitStatement) stmt;
                // Check if this is "EXIT WHEN cursor_name%NOTFOUND"
                if (exit.hasCondition() && isCursorNotFoundCondition(exit, cursorName)) {
                    exitStatement = exit;
                }
            }
        }

        // We need both FETCH and EXIT WHEN %NOTFOUND for a valid pattern
        if (fetchStatement == null || exitStatement == null) {
            return null;
        }

        // Extract business logic statements (exclude FETCH and EXIT)
        List<Statement> businessLogic = loopBody.stream()
                .filter(stmt -> !(stmt instanceof FetchStatement) && !(stmt instanceof ExitStatement))
                .toList();

        return new CursorLoopInfo(
                cursorName,
                fetchStatement.getIntoVariables(),
                businessLogic,
                openStatement,
                closeStatement,
                fetchStatement,
                exitStatement
        );
    }

    /**
     * Checks if an EXIT statement represents "EXIT WHEN cursor%NOTFOUND".
     * This is a simplified check - a more sophisticated implementation would
     * parse the condition expression properly.
     */
    private static boolean isCursorNotFoundCondition(ExitStatement exitStatement, String cursorName) {
        if (!exitStatement.hasCondition()) {
            return false;
        }
        
        // Simple string check for cursor%NOTFOUND pattern
        String conditionText = exitStatement.getCondition().toString().toUpperCase();
        String expectedPattern = cursorName.toUpperCase() + "%NOTFOUND";
        
        return conditionText.contains(expectedPattern);
    }

    /**
     * Checks if a sequence of statements contains a cursor loop pattern that
     * can be optimized to a PostgreSQL FOR loop.
     */
    public static boolean containsCursorLoopPattern(List<Statement> statements) {
        return detectCursorLoopPattern(statements) != null;
    }
}