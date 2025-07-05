package me.christianrobert.ora2postgre.plsql.ast.tools;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Utility class for transforming Oracle trigger syntax to PostgreSQL equivalents.
 * Handles Oracle-specific trigger conditions and syntax patterns.
 * Uses OracleFunctionMapper for Oracle function transformations.
 */
public class TriggerTransformer {
    
    /**
     * Transforms Oracle trigger body PL/SQL code to PostgreSQL equivalent.
     */
    public static String transformTriggerBody(String oracleBody, Everything everything) {
        if (oracleBody == null || oracleBody.trim().isEmpty()) {
            return "-- Empty trigger body";
        }
        
        String transformed = oracleBody;
        
        // Transform Oracle references to PostgreSQL
        transformed = transformOracleReferences(transformed);
        
        // Transform Oracle built-in functions using OracleFunctionMapper
        transformed = OracleFunctionMapper.transformOracleFunctions(transformed);
        
        // Transform Oracle trigger conditions
        transformed = transformOracleTriggerConditions(transformed);
        
        // Transform Oracle exception handling
        transformed = transformExceptionHandling(transformed);
        
        // Transform Oracle-specific SQL constructs
        transformed = transformOracleSqlConstructs(transformed);
        
        return transformed;
    }
    
    /**
     * Transforms Oracle :NEW and :OLD references to PostgreSQL equivalents.
     */
    public static String transformOracleReferences(String code) {
        // Transform :NEW.column to NEW.column
        code = code.replaceAll(":NEW\\.", "NEW.");
        
        // Transform :OLD.column to OLD.column  
        code = code.replaceAll(":OLD\\.", "OLD.");
        
        return code;
    }
    
    
    /**
     * Transforms Oracle trigger conditions (INSERTING, UPDATING, DELETING) to PostgreSQL.
     */
    public static String transformOracleTriggerConditions(String code) {
        String transformed = code;
        
        // Transform INSERTING to TG_OP = 'INSERT'
        transformed = transformed.replaceAll("\\bINSERTING\\b", "TG_OP = 'INSERT'");
        
        // Transform UPDATING to TG_OP = 'UPDATE'
        transformed = transformed.replaceAll("\\bUPDATING\\b", "TG_OP = 'UPDATE'");
        
        // Transform DELETING to TG_OP = 'DELETE'
        transformed = transformed.replaceAll("\\bDELETING\\b", "TG_OP = 'DELETE'");
        
        // Transform UPDATING('column') to TG_OP = 'UPDATE' AND OLD.column IS DISTINCT FROM NEW.column
        transformed = transformUpdatingColumn(transformed);
        
        return transformed;
    }
    
    /**
     * Transforms Oracle exception handling to PostgreSQL.
     */
    public static String transformExceptionHandling(String code) {
        String transformed = code;
        
        // Transform basic exception handling
        transformed = transformed.replaceAll("\\bEXCEPTION\\s+WHEN\\s+OTHERS\\s+THEN", "EXCEPTION WHEN OTHERS THEN");
        
        // Transform NO_DATA_FOUND
        transformed = transformed.replaceAll("\\bNO_DATA_FOUND\\b", "NOT_FOUND");
        
        // Transform TOO_MANY_ROWS
        transformed = transformed.replaceAll("\\bTOO_MANY_ROWS\\b", "TOO_MANY_ROWS");
        
        // Transform DUP_VAL_ON_INDEX
        transformed = transformed.replaceAll("\\bDUP_VAL_ON_INDEX\\b", "UNIQUE_VIOLATION");
        
        return transformed;
    }
    
    /**
     * Transforms Oracle-specific SQL constructs to PostgreSQL.
     */
    public static String transformOracleSqlConstructs(String code) {
        String transformed = code;
        
        // Transform ROWNUM to row_number() or limit
        // This is complex and context-dependent, so we'll add a comment
        if (transformed.contains("ROWNUM")) {
            transformed = transformed.replaceAll("\\bROWNUM\\b", "-- TODO: Transform ROWNUM to PostgreSQL equivalent (row_number() or LIMIT)");
        }
        
        // Transform dual table references
        transformed = transformed.replaceAll("\\bFROM\\s+DUAL\\b", "-- FROM dual not needed in PostgreSQL");
        
        // Transform (+) outer join syntax to proper JOIN syntax
        if (transformed.contains("(+)")) {
            transformed = "-- TODO: Transform Oracle outer join (+) syntax to PostgreSQL JOIN syntax\n" + transformed;
        }
        
        return transformed;
    }
    
    
    /**
     * Transforms Oracle UPDATING('column') to PostgreSQL equivalent.
     */
    private static String transformUpdatingColumn(String code) {
        // UPDATING('column') -> (TG_OP = 'UPDATE' AND OLD.column IS DISTINCT FROM NEW.column)
        Pattern pattern = Pattern.compile("\\bUPDATING\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(code);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String column = matcher.group(1);
            String replacement = String.format("(TG_OP = 'UPDATE' AND OLD.%s IS DISTINCT FROM NEW.%s)", column, column);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Transforms Oracle WHEN clause conditions to PostgreSQL.
     */
    public static String transformWhenClause(String oracleWhen, Everything everything) {
        if (oracleWhen == null || oracleWhen.trim().isEmpty()) {
            return null;
        }
        
        String transformed = oracleWhen;
        
        // Transform Oracle references
        transformed = transformOracleReferences(transformed);
        
        // Transform Oracle functions using OracleFunctionMapper
        transformed = OracleFunctionMapper.transformOracleFunctions(transformed);
        
        // Transform Oracle conditions
        transformed = transformOracleTriggerConditions(transformed);
        
        return transformed;
    }
    
    /**
     * Determines the appropriate PostgreSQL return statement based on trigger events.
     */
    public static String getPostgreTriggerReturn(String triggeringEvents, String triggerType) {
        if (triggeringEvents == null) {
            return "RETURN COALESCE(NEW, OLD);";
        }
        
        String upperEvents = triggeringEvents.toUpperCase();
        String upperType = triggerType != null ? triggerType.toUpperCase() : "";
        
        if (upperEvents.contains("DELETE") && !upperEvents.contains("INSERT") && !upperEvents.contains("UPDATE")) {
            // DELETE-only triggers should return OLD
            return "RETURN OLD;";
        } else if (upperType.contains("BEFORE") && (upperEvents.contains("INSERT") || upperEvents.contains("UPDATE"))) {
            // BEFORE INSERT/UPDATE triggers can modify NEW and must return it
            return "RETURN NEW;";
        } else if (upperType.contains("AFTER")) {
            // AFTER triggers return value is ignored, but we need to return something
            return "RETURN COALESCE(NEW, OLD);";
        } else {
            // Default case
            return "RETURN COALESCE(NEW, OLD);";
        }
    }
}