package me.christianrobert.ora2postgre.plsql.ast.tools;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Comprehensive utility class for mapping Oracle built-in functions to PostgreSQL equivalents.
 * This class can be reused across the entire codebase for functions, procedures, packages, 
 * triggers, and any other PL/SQL elements that need Oracle function transformation.
 */
public class OracleFunctionMapper {
    
    // Simple 1:1 function mappings (Oracle -> PostgreSQL)
    private static final Map<String, String> SIMPLE_FUNCTION_MAPPINGS = new HashMap<>();
    
    // Complex function mappings that require parameter transformation
    private static final Map<String, String> COMPLEX_FUNCTION_MAPPINGS = new HashMap<>();
    
    static {
        initializeSimpleFunctionMappings();
        initializeComplexFunctionMappings();
    }
    
    /**
     * Initialize simple 1:1 function mappings.
     */
    private static void initializeSimpleFunctionMappings() {
        // Date/Time functions
        SIMPLE_FUNCTION_MAPPINGS.put("SYSDATE", "CURRENT_TIMESTAMP");
        SIMPLE_FUNCTION_MAPPINGS.put("SYSTIMESTAMP", "CURRENT_TIMESTAMP");
        SIMPLE_FUNCTION_MAPPINGS.put("CURRENT_DATE", "CURRENT_DATE");
        SIMPLE_FUNCTION_MAPPINGS.put("CURRENT_TIMESTAMP", "CURRENT_TIMESTAMP");
        
        // User/Session functions
        SIMPLE_FUNCTION_MAPPINGS.put("USER", "CURRENT_USER");
        SIMPLE_FUNCTION_MAPPINGS.put("UID", "CURRENT_USER");
        SIMPLE_FUNCTION_MAPPINGS.put("SESSION_USER", "SESSION_USER");
        
        // String functions (simple cases)
        SIMPLE_FUNCTION_MAPPINGS.put("LENGTH", "LENGTH");
        SIMPLE_FUNCTION_MAPPINGS.put("UPPER", "UPPER");
        SIMPLE_FUNCTION_MAPPINGS.put("LOWER", "LOWER");
        SIMPLE_FUNCTION_MAPPINGS.put("LTRIM", "LTRIM");
        SIMPLE_FUNCTION_MAPPINGS.put("RTRIM", "RTRIM");
        SIMPLE_FUNCTION_MAPPINGS.put("TRIM", "TRIM");
        SIMPLE_FUNCTION_MAPPINGS.put("REVERSE", "REVERSE");
        
        // Numeric functions
        SIMPLE_FUNCTION_MAPPINGS.put("ABS", "ABS");
        SIMPLE_FUNCTION_MAPPINGS.put("CEIL", "CEIL");
        SIMPLE_FUNCTION_MAPPINGS.put("FLOOR", "FLOOR");
        SIMPLE_FUNCTION_MAPPINGS.put("ROUND", "ROUND");
        SIMPLE_FUNCTION_MAPPINGS.put("TRUNC", "TRUNC");
        SIMPLE_FUNCTION_MAPPINGS.put("MOD", "MOD");
        SIMPLE_FUNCTION_MAPPINGS.put("POWER", "POWER");
        SIMPLE_FUNCTION_MAPPINGS.put("SQRT", "SQRT");
        SIMPLE_FUNCTION_MAPPINGS.put("SIGN", "SIGN");
        
        // Aggregate functions
        SIMPLE_FUNCTION_MAPPINGS.put("COUNT", "COUNT");
        SIMPLE_FUNCTION_MAPPINGS.put("SUM", "SUM");
        SIMPLE_FUNCTION_MAPPINGS.put("AVG", "AVG");
        SIMPLE_FUNCTION_MAPPINGS.put("MIN", "MIN");
        SIMPLE_FUNCTION_MAPPINGS.put("MAX", "MAX");
        
        // Conversion functions (simple cases)
        SIMPLE_FUNCTION_MAPPINGS.put("TO_CHAR", "TO_CHAR");
        SIMPLE_FUNCTION_MAPPINGS.put("TO_NUMBER", "TO_NUMBER");
        
        // Utility functions (simple cases)
        SIMPLE_FUNCTION_MAPPINGS.put("NVL", "COALESCE");
        SIMPLE_FUNCTION_MAPPINGS.put("GREATEST", "GREATEST");
        SIMPLE_FUNCTION_MAPPINGS.put("LEAST", "LEAST");
    }
    
    /**
     * Initialize complex function mappings that require parameter transformation.
     */
    private static void initializeComplexFunctionMappings() {
        // String functions with parameter differences
        COMPLEX_FUNCTION_MAPPINGS.put("SUBSTR", "SUBSTRING");
        COMPLEX_FUNCTION_MAPPINGS.put("INSTR", "POSITION");
        
        // Conditional functions
        COMPLEX_FUNCTION_MAPPINGS.put("NVL2", "CASE_WHEN");
        COMPLEX_FUNCTION_MAPPINGS.put("DECODE", "CASE_WHEN");
        
        // Date functions with format differences
        COMPLEX_FUNCTION_MAPPINGS.put("TO_DATE", "TO_TIMESTAMP");
        COMPLEX_FUNCTION_MAPPINGS.put("ADD_MONTHS", "DATE_ADD_MONTHS");
        COMPLEX_FUNCTION_MAPPINGS.put("MONTHS_BETWEEN", "DATE_DIFF_MONTHS");
        
        // Sequence functions
        COMPLEX_FUNCTION_MAPPINGS.put("NEXTVAL", "NEXTVAL_SEQ");
        COMPLEX_FUNCTION_MAPPINGS.put("CURRVAL", "CURRVAL_SEQ");
    }
    
    /**
     * Transform Oracle functions in the given code to PostgreSQL equivalents.
     * This is the main entry point for function transformation.
     * 
     * @param oracleCode The Oracle PL/SQL code containing function calls
     * @return PostgreSQL code with transformed function calls
     */
    public static String transformOracleFunctions(String oracleCode) {
        if (oracleCode == null || oracleCode.trim().isEmpty()) {
            return oracleCode;
        }
        
        String transformed = oracleCode;
        
        // Transform simple 1:1 function mappings
        transformed = transformSimpleFunctions(transformed);
        
        // Transform complex functions that require parameter manipulation
        transformed = transformComplexFunctions(transformed);
        
        // Transform sequence functions
        transformed = transformSequenceFunctions(transformed);
        
        return transformed;
    }
    
    /**
     * Transform simple 1:1 Oracle function mappings.
     */
    public static String transformSimpleFunctions(String code) {
        String transformed = code;
        
        for (Map.Entry<String, String> mapping : SIMPLE_FUNCTION_MAPPINGS.entrySet()) {
            String oracleFunc = mapping.getKey();
            String pgFunc = mapping.getValue();
            
            // Replace function names with word boundaries to avoid partial matches
            Pattern pattern = Pattern.compile("\\b" + oracleFunc + "\\b", Pattern.CASE_INSENSITIVE);
            transformed = pattern.matcher(transformed).replaceAll(pgFunc);
        }
        
        return transformed;
    }
    
    /**
     * Transform complex Oracle functions that require parameter manipulation.
     */
    public static String transformComplexFunctions(String code) {
        String transformed = code;
        
        // Transform SUBSTR to SUBSTRING
        transformed = transformSubstr(transformed);
        
        // Transform INSTR to POSITION
        transformed = transformInstr(transformed);
        
        // Transform NVL2 to CASE expression
        transformed = transformNvl2(transformed);
        
        // Transform DECODE to CASE expression
        transformed = transformDecode(transformed);
        
        // Transform TO_DATE to TO_TIMESTAMP
        transformed = transformToDate(transformed);
        
        return transformed;
    }
    
    /**
     * Transform Oracle SUBSTR function to PostgreSQL SUBSTRING.
     * SUBSTR(string, start [, length]) -> SUBSTRING(string FROM start [FOR length])
     */
    public static String transformSubstr(String code) {
        // SUBSTR(string, start, length) -> SUBSTRING(string FROM start FOR length)
        Pattern pattern = Pattern.compile("\\bSUBSTR\\s*\\(([^,]+),\\s*([^,]+),\\s*([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(code);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String string = matcher.group(1).trim();
            String start = matcher.group(2).trim();
            String length = matcher.group(3).trim();
            String replacement = String.format("SUBSTRING(%s FROM %s FOR %s)", string, start, length);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        // SUBSTR(string, start) -> SUBSTRING(string FROM start)
        pattern = Pattern.compile("\\bSUBSTR\\s*\\(([^,]+),\\s*([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(result.toString());
        
        result = new StringBuffer();
        while (matcher.find()) {
            String string = matcher.group(1).trim();
            String start = matcher.group(2).trim();
            String replacement = String.format("SUBSTRING(%s FROM %s)", string, start);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Transform Oracle INSTR function to PostgreSQL POSITION.
     * INSTR(string, substring [, position [, occurrence]]) -> POSITION(substring IN string)
     * Note: PostgreSQL POSITION doesn't support position and occurrence parameters
     */
    public static String transformInstr(String code) {
        // Simple case: INSTR(string, substring) -> POSITION(substring IN string)
        Pattern pattern = Pattern.compile("\\bINSTR\\s*\\(([^,]+),\\s*([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(code);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String string = matcher.group(1).trim();
            String substring = matcher.group(2).trim();
            String replacement = String.format("POSITION(%s IN %s)", substring, string);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        // Complex cases with position/occurrence parameters - add TODO comment
        if (result.toString().contains("INSTR(")) {
            return "-- TODO: Complex INSTR with position/occurrence parameters needs manual conversion\n" + result.toString();
        }
        
        return result.toString();
    }
    
    /**
     * Transform Oracle NVL2 function to PostgreSQL CASE expression.
     * NVL2(expr1, expr2, expr3) -> CASE WHEN expr1 IS NOT NULL THEN expr2 ELSE expr3 END
     */
    public static String transformNvl2(String code) {
        Pattern pattern = Pattern.compile("\\bNVL2\\s*\\(([^,]+),\\s*([^,]+),\\s*([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(code);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String expr1 = matcher.group(1).trim();
            String expr2 = matcher.group(2).trim();
            String expr3 = matcher.group(3).trim();
            String replacement = String.format("CASE WHEN %s IS NOT NULL THEN %s ELSE %s END", expr1, expr2, expr3);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Transform Oracle DECODE function to PostgreSQL CASE expression.
     * This is a simplified transformation - full DECODE is quite complex.
     */
    public static String transformDecode(String code) {
        if (code.toUpperCase().contains("DECODE(")) {
            return "-- TODO: Transform DECODE function to PostgreSQL CASE expression\n" + code;
        }
        return code;
    }
    
    /**
     * Transform Oracle TO_DATE function to PostgreSQL TO_TIMESTAMP.
     */
    public static String transformToDate(String code) {
        // TO_DATE(string, format) -> TO_TIMESTAMP(string, format)
        Pattern pattern = Pattern.compile("\\bTO_DATE\\s*\\(", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(code).replaceAll("TO_TIMESTAMP(");
    }
    
    /**
     * Transform Oracle sequence functions to PostgreSQL equivalents.
     * sequence_name.NEXTVAL -> nextval('sequence_name')
     * sequence_name.CURRVAL -> currval('sequence_name')
     */
    public static String transformSequenceFunctions(String code) {
        String transformed = code;
        
        // Transform NEXTVAL
        Pattern nextvalPattern = Pattern.compile("([\\w_]+)\\.NEXTVAL", Pattern.CASE_INSENSITIVE);
        transformed = nextvalPattern.matcher(transformed).replaceAll("nextval('$1')");
        
        // Transform CURRVAL
        Pattern currvalPattern = Pattern.compile("([\\w_]+)\\.CURRVAL", Pattern.CASE_INSENSITIVE);
        transformed = currvalPattern.matcher(transformed).replaceAll("currval('$1')");
        
        return transformed;
    }
    
    /**
     * Check if a function name is a known Oracle function that can be transformed.
     */
    public static boolean isKnownOracleFunction(String functionName) {
        if (functionName == null) {
            return false;
        }
        
        String upperFuncName = functionName.toUpperCase();
        return SIMPLE_FUNCTION_MAPPINGS.containsKey(upperFuncName) || 
               COMPLEX_FUNCTION_MAPPINGS.containsKey(upperFuncName);
    }
    
    /**
     * Get the PostgreSQL equivalent for a simple Oracle function.
     * Returns null if the function is not a simple mapping or doesn't exist.
     */
    public static String getSimpleFunctionMapping(String oracleFunction) {
        if (oracleFunction == null) {
            return null;
        }
        return SIMPLE_FUNCTION_MAPPINGS.get(oracleFunction.toUpperCase());
    }
    
    /**
     * Get all supported Oracle function names for documentation/validation purposes.
     */
    public static java.util.Set<String> getSupportedOracleFunctions() {
        java.util.Set<String> allFunctions = new java.util.HashSet<>();
        allFunctions.addAll(SIMPLE_FUNCTION_MAPPINGS.keySet());
        allFunctions.addAll(COMPLEX_FUNCTION_MAPPINGS.keySet());
        return allFunctions;
    }
    
    /**
     * Get transformation statistics for reporting purposes.
     */
    public static Map<String, Integer> getTransformationStats(String originalCode, String transformedCode) {
        Map<String, Integer> stats = new HashMap<>();
        
        for (String oracleFunc : SIMPLE_FUNCTION_MAPPINGS.keySet()) {
            Pattern pattern = Pattern.compile("\\b" + oracleFunc + "\\b", Pattern.CASE_INSENSITIVE);
            int originalCount = pattern.matcher(originalCode).results().mapToInt(m -> 1).sum();
            int transformedCount = pattern.matcher(transformedCode).results().mapToInt(m -> 1).sum();
            
            if (originalCount > 0) {
                stats.put(oracleFunc + "_original", originalCount);
                stats.put(oracleFunc + "_remaining", transformedCount);
                stats.put(oracleFunc + "_transformed", originalCount - transformedCount);
            }
        }
        
        return stats;
    }
}