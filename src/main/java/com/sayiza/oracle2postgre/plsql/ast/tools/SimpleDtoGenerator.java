package com.sayiza.oracle2postgre.plsql.ast.tools;

import com.sayiza.oracle2postgre.global.Everything;
import com.sayiza.oracle2postgre.global.StringAux;
import com.sayiza.oracle2postgre.plsql.ast.Function;

/**
 * Generates simple DTOs for REST responses when needed.
 * Much simpler than the previous complex DTO generation.
 */
public class SimpleDtoGenerator {
    
    /**
     * Generates a simple response DTO for a function return type.
     * Only creates DTOs for complex return types - primitives are returned directly.
     */
    public static String generateResponseDto(Function function, String javaPackageName, Everything data) {
        String returnType = function.getReturnType();
        
        // For simple types, no DTO needed
        if (isSimpleType(returnType)) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Package declaration
        sb.append("package ").append(javaPackageName).append(".dto;\n\n");
        
        // Imports
        sb.append("import com.fasterxml.jackson.annotation.JsonProperty;\n\n");
        
        // Class declaration
        String className = StringAux.capitalizeFirst(function.getName()) + "Response";
        sb.append("public class ").append(className).append(" {\n\n");
        
        // For now, create a generic result field
        // In a real implementation, this would analyze the return type structure
        String javaType = TypeConverter.toJava(returnType);
        sb.append("    @JsonProperty(\"result\")\n");
        sb.append("    private ").append(javaType).append(" result;\n\n");
        
        // Constructor
        sb.append("    public ").append(className).append("() {}\n\n");
        sb.append("    public ").append(className).append("(").append(javaType).append(" result) {\n");
        sb.append("        this.result = result;\n");
        sb.append("    }\n\n");
        
        // Getter and setter
        sb.append("    public ").append(javaType).append(" getResult() {\n");
        sb.append("        return result;\n");
        sb.append("    }\n\n");
        
        sb.append("    public void setResult(").append(javaType).append(" result) {\n");
        sb.append("        this.result = result;\n");
        sb.append("    }\n");
        
        sb.append("}\n");
        return sb.toString();
    }
    
    /**
     * Checks if a type is simple enough to return directly without a DTO wrapper.
     */
    private static boolean isSimpleType(String type) {
        if (type == null) return true;
        
        String upperType = type.toUpperCase();
        return upperType.contains("VARCHAR") || 
               upperType.contains("NUMBER") || 
               upperType.contains("INTEGER") ||
               upperType.contains("DATE") ||
               upperType.contains("BOOLEAN") ||
               upperType.equals("STRING") ||
               upperType.equals("BIGDECIMAL") ||
               upperType.equals("LONG") ||
               upperType.equals("INT");
    }
    
    /**
     * Gets the DTO class name for a function, or null if no DTO is needed.
     */
    public static String getDtoClassName(Function function) {
        if (isSimpleType(function.getReturnType())) {
            return null;
        }
        return StringAux.capitalizeFirst(function.getName()) + "Response";
    }
}