package me.christianrobert.ora2postgre.controllers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.PlsqlCode;
import me.christianrobert.ora2postgre.plsql.PlSqlAstMain;
import me.christianrobert.ora2postgre.plsql.ast.SelectStatement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;
import java.util.HashMap;

@Path("/migration")
@ApplicationScoped
@Tag(name = "SQL Transformation", description = "Runtime SQL transformation service")
public class SqlTransformController {

  private static final Logger log = LoggerFactory.getLogger(SqlTransformController.class);

  @Inject
  Everything data;

  @POST
  @Path("/transform/sql")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "🔄 Transform Oracle SQL to PostgreSQL",
          description = "Transforms Oracle SQL statements to PostgreSQL-compatible SQL using the full migration context for synonym resolution and schema mapping. Useful for runtime SQL transformation in external applications."
  )
  @APIResponses({
          @APIResponse(responseCode = "200", description = "SQL transformation completed successfully"),
          @APIResponse(responseCode = "400", description = "Invalid request - missing required parameters"),
          @APIResponse(responseCode = "500", description = "SQL transformation failed")
  })
  public Response transformSql(Map<String, String> request) {
    try {
      String schema = request.get("schema");
      String sql = request.get("sql");

      if (schema == null || schema.trim().isEmpty()) {
        Map<String, String> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", "schema parameter is required");
        return Response.status(400).entity(error).build();
      }

      if (sql == null || sql.trim().isEmpty()) {
        Map<String, String> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", "sql parameter is required");
        return Response.status(400).entity(error).build();
      }

      // Use the existing transformation pipeline with full Everything context
      SelectStatement ast = (SelectStatement) PlSqlAstMain.processPlsqlCode(
              new PlsqlCode(schema, sql));
      String transformedSql = ast.toPostgre(data);

      Map<String, String> result = new HashMap<>();
      result.put("status", "success");
      result.put("originalSql", sql);
      result.put("transformedSql", transformedSql);
      result.put("schema", schema);
      return Response.ok(result).build();

    } catch (Exception e) {
      log.error("SQL transformation failed for request: {}", request, e);
      Map<String, String> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "SQL transformation failed: " + e.getMessage());
      error.put("errorType", e.getClass().getSimpleName());
      return Response.status(500).entity(error).build();
    }
  }
}