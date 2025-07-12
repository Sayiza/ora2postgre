package me.christianrobert.ora2postgre.plsql.ast.tools;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.*;
import me.christianrobert.ora2postgre.plsql.ast.tools.CursorLoopAnalyzer.CursorLoopInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Transforms Oracle cursor loop patterns into PostgreSQL FOR loop syntax.
 * Converts the verbose OPEN/FETCH/CLOSE pattern into cleaner FOR loop iterations.
 */
public class CursorLoopTransformer {

    /**
     * Transforms a cursor loop pattern into a PostgreSQL FOR loop statement.
     * 
     * Input pattern:
     * OPEN emp_cursor;
     * LOOP
     *   FETCH emp_cursor INTO v_emp_id, v_first_name;
     *   EXIT WHEN emp_cursor%NOTFOUND;
     *   [business logic]
     * END LOOP;
     * CLOSE emp_cursor;
     * 
     * Output pattern:
     * FOR rec IN emp_cursor LOOP
     *   v_emp_id := rec.column1;
     *   v_first_name := rec.column2;
     *   [business logic]
     * END LOOP;
     */
    public static CursorForLoopStatement transformToCursorForLoop(CursorLoopInfo cursorInfo) {
        String cursorName = cursorInfo.getCursorName();
        String recordVariable = "rec"; // Standard PostgreSQL record variable name
        
        // Create assignment statements to map record fields to variables
        List<Statement> transformedStatements = new ArrayList<>();
        
        // Add assignments for FETCH variables: v_emp_id := rec.column1;
        List<String> fetchVariables = cursorInfo.getFetchVariables();
        for (int i = 0; i < fetchVariables.size(); i++) {
            String variable = fetchVariables.get(i);
            String recordField = recordVariable + ".column" + (i + 1); // rec.column1, rec.column2, etc.
            
            // Create assignment statement: variable := rec.columnN;
            AssignmentStatement assignment = createRecordFieldAssignment(variable, recordField);
            transformedStatements.add(assignment);
        }
        
        // Add the original business logic statements
        transformedStatements.addAll(cursorInfo.getLoopBodyStatements());
        
        return new CursorForLoopStatement(recordVariable, cursorName, transformedStatements);
    }

    /**
     * Creates an assignment statement for mapping record fields to variables.
     * Example: v_emp_id := rec.column1;
     */
    private static AssignmentStatement createRecordFieldAssignment(String variable, String recordField) {
        // Create expression for the right side (record field)
        Expression rightSide = createSimpleExpression(recordField);
        
        // AssignmentStatement expects (String target, Expression expression)
        return new AssignmentStatement(variable, rightSide);
    }

    /**
     * Creates a simple expression from a string.
     * This is a utility method for creating variable and field reference expressions.
     */
    private static Expression createSimpleExpression(String text) {
        // Create a simple unary logical expression with the text
        UnaryLogicalExpression unary = new UnaryLogicalExpression(text);
        LogicalExpression logical = new LogicalExpression(unary);
        return new Expression(logical);
    }

    /**
     * Represents a PostgreSQL cursor FOR loop statement.
     * This generates the "FOR rec IN cursor_name LOOP ... END LOOP;" syntax.
     */
    public static class CursorForLoopStatement extends Statement {
        private final String recordVariable;
        private final String cursorName;
        private final List<Statement> statements;

        public CursorForLoopStatement(String recordVariable, String cursorName, List<Statement> statements) {
            this.recordVariable = recordVariable;
            this.cursorName = cursorName;
            this.statements = statements;
        }

        public String getRecordVariable() {
            return recordVariable;
        }

        public String getCursorName() {
            return cursorName;
        }

        public List<Statement> getStatements() {
            return statements;
        }

        @Override
        public <T> T accept(PlSqlAstVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public String toString() {
            return "CursorForLoopStatement{cursor='" + cursorName + "', record='" + recordVariable + "', statements=" + statements.size() + "}";
        }

        @Override
        public String toPostgre(Everything data) {
            StringBuilder b = new StringBuilder();

            // FOR rec IN cursor_name LOOP
            b.append(data.getIntendation()).append("FOR ").append(recordVariable).append(" IN ").append(cursorName).append(" LOOP\n");

            // Loop body statements with increased indentation
            data.intendMore();
            for (Statement stmt : statements) {
                b.append(stmt.toPostgre(data));
                if (!stmt.toPostgre(data).endsWith("\n")) {
                    b.append("\n");
                }
            }
            data.intendLess();

            // END LOOP
            b.append(data.getIntendation()).append("END LOOP;");

            return b.toString();
        }
    }

    /**
     * Checks if cursor loop transformation should be applied.
     * Currently always returns true, but could be enhanced with configuration
     * or heuristics to decide when to use FOR loops vs. manual cursor management.
     */
    public static boolean shouldTransformToForLoop(CursorLoopInfo cursorInfo) {
        // For now, always transform to FOR loop as it's cleaner
        // Could add logic here to check for complex cursor usage that might need manual management
        return true;
    }
}