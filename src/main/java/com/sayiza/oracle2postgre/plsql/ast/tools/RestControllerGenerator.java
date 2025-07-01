package com.sayiza.oracle2postgre.plsql.ast.tools;

import com.sayiza.oracle2postgre.global.Everything;
import com.sayiza.oracle2postgre.global.StringAux;
import com.sayiza.oracle2postgre.plsql.ast.Function;
import com.sayiza.oracle2postgre.plsql.ast.OraclePackage;
import com.sayiza.oracle2postgre.plsql.ast.Parameter;
import com.sayiza.oracle2postgre.plsql.ast.Procedure;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates minimal REST controllers that call PostgreSQL functions directly.
 * This replaces the complex Java code generation with simple database-calling endpoints.
 */
public class RestControllerGenerator {

    /**
     * Generates a REST controller for an Oracle package that calls PostgreSQL functions.
     */
    public static String generateController(OraclePackage pkg, String javaPackageName, Everything data) {
        StringBuilder sb = new StringBuilder();
        
        // Package declaration
        sb.append("package ").append(javaPackageName).append(".")
          .append(pkg.getSchema().toLowerCase()).append(".controllers;\n\n");
        
        // Imports
        sb.append("import jakarta.enterprise.context.ApplicationScoped;\n");
        sb.append("import jakarta.inject.Inject;\n");
        sb.append("import jakarta.ws.rs.*;\n");
        sb.append("import jakarta.ws.rs.core.MediaType;\n");
        sb.append("import jakarta.ws.rs.core.Response;\n");
        sb.append("import java.sql.CallableStatement;\n");
        sb.append("import java.sql.Connection;\n");
        sb.append("import java.sql.SQLException;\n");
        sb.append("import io.agroal.api.AgroalDataSource;\n\n");
        
        // Class declaration
        String className = StringAux.capitalizeFirst(pkg.getName()) + "Controller";
        sb.append("@ApplicationScoped\n");
        sb.append("@Path(\"/").append(pkg.getSchema().toLowerCase()).append("/")
          .append(pkg.getName().toLowerCase()).append("\")\n");
        sb.append("@Produces(MediaType.APPLICATION_JSON)\n");
        sb.append("@Consumes(MediaType.APPLICATION_JSON)\n");
        sb.append("public class ").append(className).append(" {\n\n");
        
        // DataSource injection
        sb.append("    @Inject\n");
        sb.append("    AgroalDataSource dataSource;\n\n");
        
        // Generate methods for functions
        for (Function function : pkg.getFunctions()) {
            sb.append(generateFunctionEndpoint(function, pkg, data));
            sb.append("\n\n");
        }
        
        // Generate methods for procedures
        for (Procedure procedure : pkg.getProcedures()) {
            sb.append(generateProcedureEndpoint(procedure, pkg, data));
            sb.append("\n\n");
        }
        
        sb.append("}\n");
        return sb.toString();
    }
    
    /**
     * Generates a REST endpoint that calls a PostgreSQL function.
     */
    private static String generateFunctionEndpoint(Function function, OraclePackage pkg, Everything data) {
        StringBuilder sb = new StringBuilder();
        
        String methodName = StringAux.lowerCaseFirst(function.getName());
        String pgFunctionName = function.getPostgreFunctionName();
        
        // Method annotation and signature
        sb.append("    @GET\n");
        sb.append("    @Path(\"/").append(function.getName().toLowerCase()).append("\")\n");
        sb.append("    public Response ").append(methodName).append("(");
        
        // Parameters
        List<String> params = function.getParameters().stream()
            .map(p -> generateRestParameter(p, data))
            .collect(Collectors.toList());
        sb.append(String.join(", ", params));
        sb.append(") {\n");
        
        // Method body
        sb.append("        try (Connection conn = dataSource.getConnection()) {\n");
        sb.append("            String sql = \"SELECT * FROM ").append(pgFunctionName).append("(");
        
        // Parameter placeholders
        String placeholders = function.getParameters().stream()
            .map(p -> "?")
            .collect(Collectors.joining(", "));
        sb.append(placeholders).append(")\";\n");
        
        sb.append("            try (CallableStatement stmt = conn.prepareCall(sql)) {\n");
        
        // Set parameters
        for (int i = 0; i < function.getParameters().size(); i++) {
            Parameter param = function.getParameters().get(i);
            sb.append("                stmt.setString(").append(i + 1).append(", ")
              .append(param.getName()).append(");\n");
        }
        
        sb.append("                var resultSet = stmt.executeQuery();\n");
        sb.append("                // TODO: Convert ResultSet to appropriate response object\n");
        sb.append("                return Response.ok(\"Function result placeholder\").build();\n");
        sb.append("            }\n");
        sb.append("        } catch (SQLException e) {\n");
        sb.append("            return Response.serverError()\n");
        sb.append("                .entity(\"Database error: \" + e.getMessage())\n");
        sb.append("                .build();\n");
        sb.append("        }\n");
        sb.append("    }");
        
        return sb.toString();
    }
    
    /**
     * Generates a REST endpoint that calls a PostgreSQL procedure.
     */
    private static String generateProcedureEndpoint(Procedure procedure, OraclePackage pkg, Everything data) {
        StringBuilder sb = new StringBuilder();
        
        String methodName = StringAux.lowerCaseFirst(procedure.getName());
        String pgProcedureName = procedure.getPostgreProcedureName();
        
        // Method annotation and signature
        sb.append("    @POST\n");
        sb.append("    @Path(\"/").append(procedure.getName().toLowerCase()).append("\")\n");
        sb.append("    public Response ").append(methodName).append("(");
        
        // Parameters
        List<String> params = procedure.getParameters().stream()
            .map(p -> generateRestParameter(p, data))
            .collect(Collectors.toList());
        sb.append(String.join(", ", params));
        sb.append(") {\n");
        
        // Method body
        sb.append("        try (Connection conn = dataSource.getConnection()) {\n");
        sb.append("            String sql = \"CALL ").append(pgProcedureName).append("(");
        
        // Parameter placeholders
        String placeholders = procedure.getParameters().stream()
            .map(p -> "?")
            .collect(Collectors.joining(", "));
        sb.append(placeholders).append(")\";\n");
        
        sb.append("            try (CallableStatement stmt = conn.prepareCall(sql)) {\n");
        
        // Set parameters
        for (int i = 0; i < procedure.getParameters().size(); i++) {
            Parameter param = procedure.getParameters().get(i);
            sb.append("                stmt.setString(").append(i + 1).append(", ")
              .append(param.getName()).append(");\n");
        }
        
        sb.append("                stmt.execute();\n");
        sb.append("                return Response.ok(\"Procedure executed successfully\").build();\n");
        sb.append("            }\n");
        sb.append("        } catch (SQLException e) {\n");
        sb.append("            return Response.serverError()\n");
        sb.append("                .entity(\"Database error: \" + e.getMessage())\n");
        sb.append("                .build();\n");
        sb.append("        }\n");
        sb.append("    }");
        
        return sb.toString();
    }
    
    /**
     * Generates a REST parameter from a PL/SQL parameter.
     */
    private static String generateRestParameter(Parameter param, Everything data) {
        String javaType = "String";//for now keep it as a string, as it is a query parameter anyway.     param.getDataType().toJava(data);// TODO ... TypeConverter.toJava(param.getDataType().toPostgre(data));
        return "@QueryParam(\"" + param.getName() + "\") " + javaType + " " + param.getName();
    }
}