package me.christianrobert.ora2postgre.controllers;

import me.christianrobert.ora2postgre.config.ConfigurationService;
import me.christianrobert.ora2postgre.config.RuntimeConfiguration;

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
@Tag(name = "Configuration", description = "Runtime configuration management operations")
public class ConfigurationController {

  private static final Logger log = LoggerFactory.getLogger(ConfigurationController.class);

  @Inject
  ConfigurationService configurationService;

  @GET
  @Path("/config")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "⚙️ Get Current Configuration",
          description = "Returns the current runtime configuration including database connection parameters, feature flags, and processing options. Used by the frontend to populate configuration forms."
  )
  @APIResponse(responseCode = "200", description = "Configuration retrieved successfully")
  public Response getConfiguration() {
    try {
      RuntimeConfiguration currentConfig = configurationService.getCurrentConfiguration();
      return Response.ok(currentConfig).build();
    } catch (Exception e) {
      log.error("Error getting configuration", e);
      Map<String, String> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "Failed to get configuration: " + e.getMessage());
      return Response.status(500).entity(error).build();
    }
  }

  @PUT
  @Path("/config")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "🔧 Update Runtime Configuration",
          description = "Updates the runtime configuration with new values for database connections, feature flags, and processing options. Changes take effect immediately for new operations."
  )
  @APIResponses({
          @APIResponse(responseCode = "200", description = "Configuration updated successfully"),
          @APIResponse(responseCode = "400", description = "Invalid configuration values provided"),
          @APIResponse(responseCode = "500", description = "Server error updating configuration")
  })
  public Response updateConfiguration(RuntimeConfiguration updates) {
    try {
      configurationService.updateConfiguration(updates);
      RuntimeConfiguration updatedConfig = configurationService.getCurrentConfiguration();

      Map<String, Object> result = new HashMap<>();
      result.put("status", "success");
      result.put("message", "Configuration updated successfully");
      result.put("config", updatedConfig);

      log.info("Configuration updated successfully");
      return Response.ok(result).build();
    } catch (Exception e) {
      log.error("Error updating configuration", e);
      Map<String, String> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "Failed to update configuration: " + e.getMessage());
      return Response.status(500).entity(error).build();
    }
  }
}