package me.christianrobert.ora2postgre.controllers;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.jobs.JobManager;
import me.christianrobert.ora2postgre.jobs.JobStatus;
import me.christianrobert.ora2postgre.jobs.MigrationProgressService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;
import java.util.HashMap;

@Path("/migration")
@ApplicationScoped
@Tag(name = "Job Management", description = "Migration job control and monitoring operations")
public class JobController {

  private static final Logger log = LoggerFactory.getLogger(JobController.class);

  @Inject
  Everything data;

  @Inject
  JobManager jobManager;

  @Inject
  MigrationProgressService progressService;

  @GET
  @Path("/jobs/{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "🔍 Get Detailed Job Status & Progress",
          description = "Provides comprehensive status information for migration jobs including progress percentage, current step, duration, and error details. Essential for monitoring long-running operations."
  )
  @APIResponses({
          @APIResponse(responseCode = "200", description = "Job status retrieved successfully"),
          @APIResponse(responseCode = "404", description = "Job not found - invalid or expired jobId")
  })
  public Response getJobStatus(
          @Parameter(
                  description = "Unique job identifier returned from job start endpoints",
                  required = true,
                  example = "full-1699123456792"
          )
          @PathParam("jobId") String jobId) {
    JobStatus status = jobManager.getJobStatus(jobId);
    if (status == null) {
      Map<String, String> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "Job not found");
      return Response.status(404).entity(error).build();
    }
    return Response.ok(status).build();
  }

  @GET
  @Path("/jobs/{jobId}/progress")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "📊 Get Detailed Migration Progress",
          description = "Returns detailed progress information including current migration step, sub-step progress, and step-specific details for comprehensive progress tracking."
  )
  @APIResponses({
          @APIResponse(responseCode = "200", description = "Progress information retrieved successfully"),
          @APIResponse(responseCode = "404", description = "Job not found - invalid or expired jobId")
  })
  public Response getJobProgress(@PathParam("jobId") String jobId) {
    MigrationProgressService.JobProgressInfo progressInfo = progressService.getDetailedProgress(jobId);
    if (progressInfo == null) {
      Map<String, String> error = new HashMap<>();
      error.put("status", "error");
      error.put("message", "Job not found");
      return Response.status(404).entity(error).build();
    }
    return Response.ok(progressInfo).build();
  }

  @GET
  @Path("/jobs")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "📋 List All Migration Jobs",
          description = "Returns a complete list of all migration jobs including running, completed, and failed jobs with their current status."
  )
  @APIResponse(responseCode = "200", description = "Job list retrieved successfully")
  public Response getAllJobs() {
    return Response.ok(jobManager.getAllJobs()).build();
  }

  @DELETE
  @Path("/jobs/completed")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "🗑️ Clear Completed Jobs",
          description = "Removes all completed and failed jobs from the job history to free up memory and clean up the job list."
  )
  @APIResponse(responseCode = "200", description = "Completed jobs cleared successfully")
  public Response clearCompletedJobs() {
    jobManager.clearCompletedJobs();
    Map<String, String> result = new HashMap<>();
    result.put("status", "success");
    result.put("message", "Completed jobs cleared");
    return Response.ok(result).build();
  }

  @POST
  @Path("/cancel")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "❌ Cancel Running Job",
          description = "Cancels a specific running job by ID, or cancels all jobs if no ID is provided. The job will be marked as cancelled and any queued jobs will continue processing."
  )
  @APIResponses({
          @APIResponse(responseCode = "200", description = "Job cancelled successfully"),
          @APIResponse(responseCode = "404", description = "Job not found"),
          @APIResponse(responseCode = "409", description = "Job already completed or cancelled")
  })
  public Response cancelJob(@QueryParam("jobId") String jobId) {
    log.info("Cancel job endpoint called with jobId: {}", jobId);

    if (jobId == null || jobId.trim().isEmpty()) {
      // Cancel all jobs
      jobManager.cancelAllJobs("Manual cancellation requested");
      Map<String, String> result = new HashMap<>();
      result.put("status", "success");
      result.put("message", "All jobs have been cancelled");
      return Response.ok(result).build();
    }

    // Cancel specific job
    boolean cancelled = jobManager.cancelJob(jobId, "Manual cancellation requested");

    if (!cancelled) {
      JobStatus status = jobManager.getJobStatus(jobId);
      if (status == null) {
        Map<String, String> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", "Job not found: " + jobId);
        return Response.status(404).entity(error).build();
      } else {
        Map<String, String> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", "Job already completed or cancelled: " + status.getState());
        return Response.status(409).entity(error).build();
      }
    }

    Map<String, String> result = new HashMap<>();
    result.put("status", "success");
    result.put("message", "Job cancelled successfully");
    result.put("jobId", jobId);
    return Response.ok(result).build();
  }

  @POST
  @Path("/reset")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "🔄 Reset Migration System",
          description = "Cancels all running jobs and clears all extracted data from memory. This resets the migration system to its initial state for a fresh start."
  )
  @APIResponse(responseCode = "200", description = "System reset completed successfully")
  public Response resetEverything() {
    log.info("Reset endpoint called - cancelling all jobs and clearing data");

    // First, cancel all running jobs
    jobManager.cancelAllJobs("Application reset requested");

    // Clear all data in the Everything singleton
    data.getUserNames().clear();
    data.getTableSql().clear();
    data.getViewDefinition().clear();
    data.getSynonyms().clear();
    data.getObjectTypeSpecPlsql().clear();
    data.getObjectTypeBodyPlsql().clear();
    data.getPackageSpecPlsql().clear();
    data.getPackageBodyPlsql().clear();
    data.getViewSpecAndQueries().clear();
    data.getObjectTypeSpecAst().clear();
    data.getObjectTypeBodyAst().clear();
    data.getPackageSpecAst().clear();
    data.getPackageBodyAst().clear();
    data.getTriggerPlsql().clear();
    data.getTriggerAst().clear();
    data.setTotalRowCount(0);

    // Clear completed jobs from job manager
    jobManager.clearCompletedJobs();

    Map<String, String> result = new HashMap<>();
    result.put("status", "success");
    result.put("message", "Everything data has been reset and all jobs cancelled");
    return Response.ok(result).build();
  }
}