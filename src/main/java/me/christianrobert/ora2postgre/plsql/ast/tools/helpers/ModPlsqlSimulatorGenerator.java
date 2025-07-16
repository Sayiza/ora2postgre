package me.christianrobert.ora2postgre.plsql.ast.tools.helpers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.StringAux;
import me.christianrobert.ora2postgre.plsql.ast.OraclePackage;
import me.christianrobert.ora2postgre.plsql.ast.Parameter;
import me.christianrobert.ora2postgre.plsql.ast.Procedure;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates mod-plsql simulator controllers that execute PostgreSQL procedures
 * and return generated HTML content via HTP calls.
 * 
 * This replaces the REST controller generation with true mod_plsql functionality
 * that replicates Oracle's mod_plsql web behavior.
 */
public class ModPlsqlSimulatorGenerator {

  /**
   * Generates a mod-plsql simulator controller for an Oracle package.
   * The controller executes procedures that generate HTML via HTP calls.
   */
  public static String generateSimulator(OraclePackage pkg, String javaPackageName, Everything data) {
    StringBuilder sb = new StringBuilder();

    // Package declaration
    sb.append("package ").append(javaPackageName).append(".")
            .append(pkg.getSchema().toLowerCase()).append(".modplsql;\n\n");

    // Imports
    sb.append("import jakarta.enterprise.context.ApplicationScoped;\n");
    sb.append("import jakarta.inject.Inject;\n");
    sb.append("import jakarta.ws.rs.*;\n");
    sb.append("import jakarta.ws.rs.core.MediaType;\n");
    sb.append("import jakarta.ws.rs.core.Response;\n");
    sb.append("import jakarta.ws.rs.core.UriInfo;\n");
    sb.append("import jakarta.ws.rs.core.Context;\n");
    sb.append("import java.sql.Connection;\n");
    sb.append("import java.sql.SQLException;\n");
    sb.append("import java.util.Map;\n");
    sb.append("import java.util.stream.Collectors;\n");
    sb.append("import io.agroal.api.AgroalDataSource;\n");
    sb.append("import ").append(javaPackageName).append(".utils.ModPlsqlExecutor;\n\n");

    // Class declaration
    String className = StringAux.capitalizeFirst(pkg.getName()) + "ModPlsqlController";
    sb.append("@ApplicationScoped\n");
    sb.append("@Path(\"/modplsql/").append(pkg.getSchema().toLowerCase()).append("/")
            .append(pkg.getName().toLowerCase()).append("\")\n");
    sb.append("@Produces(MediaType.TEXT_HTML)\n");
    sb.append("public class ").append(className).append(" {\n\n");

    // DataSource injection
    sb.append("  @Inject\n");
    sb.append("  AgroalDataSource dataSource;\n\n");

    // Generate methods for procedures only (mod-plsql doesn't support functions)
    for (Procedure procedure : pkg.getProcedures()) {
      sb.append(generateProcedureEndpoint(procedure, pkg, data));
      sb.append("\n\n");
    }

    sb.append("}\n");
    return sb.toString();
  }

  /**
   * Generates a mod-plsql endpoint that executes a PostgreSQL procedure
   * and returns the generated HTML content.
   */
  private static String generateProcedureEndpoint(Procedure procedure, OraclePackage pkg, Everything data) {
    StringBuilder sb = new StringBuilder();

    String methodName = StringAux.lowerCaseFirst(procedure.getName());
    String pgProcedureName = procedure.getPostgreProcedureName();

    // Method annotation and signature
    sb.append("  @GET\n");
    sb.append("  @Path(\"/").append(procedure.getName().toLowerCase()).append("\")\n");
    sb.append("  public Response ").append(methodName).append("(@Context UriInfo uriInfo) {\n");

    // Method body
    sb.append("    try (Connection conn = dataSource.getConnection()) {\n");
    sb.append("      // Initialize HTP buffer\n");
    sb.append("      ModPlsqlExecutor.initializeHtpBuffer(conn);\n\n");

    sb.append("      // Extract query parameters\n");
    sb.append("      Map<String, String> params = uriInfo.getQueryParameters().entrySet().stream()\n");
    sb.append("        .collect(java.util.stream.Collectors.toMap(\n");
    sb.append("          Map.Entry::getKey,\n");
    sb.append("          entry -> entry.getValue().isEmpty() ? \"\" : entry.getValue().get(0)\n");
    sb.append("        ));\n\n");

    sb.append("      // Execute procedure with parameters\n");
    sb.append("      String html = ModPlsqlExecutor.executeProcedureWithHtp(\n");
    sb.append("        conn, \"").append(pgProcedureName).append("\", params);\n\n");

    sb.append("      // Return HTML response\n");
    sb.append("      return Response.ok(html)\n");
    sb.append("        .type(MediaType.TEXT_HTML)\n");
    sb.append("        .header(\"Cache-Control\", \"no-cache\")\n");
    sb.append("        .build();\n\n");

    sb.append("    } catch (SQLException e) {\n");
    sb.append("      // Return error page with proper HTML structure\n");
    sb.append("      String errorHtml = \"<html><head><title>Error</title></head><body>\" +\n");
    sb.append("        \"<h1>Database Error</h1>\" +\n");
    sb.append("        \"<p>\" + e.getMessage() + \"</p>\" +\n");
    sb.append("        \"</body></html>\";\n");
    sb.append("      return Response.serverError()\n");
    sb.append("        .entity(errorHtml)\n");
    sb.append("        .type(MediaType.TEXT_HTML)\n");
    sb.append("        .build();\n");
    sb.append("    }\n");
    sb.append("  }");

    return sb.toString();
  }

  /**
   * Generates method signature with procedure parameters.
   * Currently not used as we use UriInfo for dynamic parameter handling,
   * but kept for potential future typed parameter support.
   */
  private static String generateProcedureParameters(List<Parameter> parameters, Everything data) {
    if (parameters.isEmpty()) {
      return "";
    }

    return parameters.stream()
            .map(p -> "@QueryParam(\"" + p.getName() + "\") String " + p.getName())
            .collect(Collectors.joining(", "));
  }
}