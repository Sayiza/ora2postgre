package me.christianrobert.ora2postgre.jobs;

import java.time.LocalDateTime;

public class JobStatus {
  private final String jobId;
  private final String jobType;
  private JobState state;
  private final LocalDateTime startedAt;
  private LocalDateTime completedAt;
  private LocalDateTime cancelledAt;
  private String message;
  private String error;
  private Object result;
  private String cancellationReason;

  // Progress tracking fields
  private String currentStep;
  private int currentStepNumber;
  private int totalSteps;
  private double stepProgress; // 0.0 to 1.0 for current step
  private double overallProgress; // 0.0 to 1.0 for entire job
  private String subStepDetails;
  private LocalDateTime estimatedCompletionTime;

  public JobStatus(String jobId, String jobType, JobState state, LocalDateTime startedAt) {
    this.jobId = jobId;
    this.jobType = jobType;
    this.state = state;
    this.startedAt = startedAt;
  }

  // Getters
  public String getJobId() { return jobId; }
  public String getJobType() { return jobType; }
  public JobState getState() { return state; }
  public LocalDateTime getStartedAt() { return startedAt; }
  public LocalDateTime getCompletedAt() { return completedAt; }
  public LocalDateTime getCancelledAt() { return cancelledAt; }
  public String getMessage() { return message; }
  public String getError() { return error; }
  public Object getResult() { return result; }
  public String getCancellationReason() { return cancellationReason; }

  // Progress getters
  public String getCurrentStep() { return currentStep; }
  public int getCurrentStepNumber() { return currentStepNumber; }
  public int getTotalSteps() { return totalSteps; }
  public double getStepProgress() { return stepProgress; }
  public double getOverallProgress() { return overallProgress; }
  public String getSubStepDetails() { return subStepDetails; }
  public LocalDateTime getEstimatedCompletionTime() { return estimatedCompletionTime; }

  // Convenience methods for frontend
  public int getStepProgressPercentage() { return (int) Math.round(stepProgress * 100); }
  public int getOverallProgressPercentage() { return (int) Math.round(overallProgress * 100); }

  // Setters
  public void setState(JobState state) { this.state = state; }
  public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
  public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
  public void setMessage(String message) { this.message = message; }
  public void setError(String error) { this.error = error; }
  public void setResult(Object result) { this.result = result; }
  public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

  // Progress setters
  public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
  public void setCurrentStepNumber(int currentStepNumber) { this.currentStepNumber = currentStepNumber; }
  public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
  public void setStepProgress(double stepProgress) { this.stepProgress = Math.max(0.0, Math.min(1.0, stepProgress)); }
  public void setOverallProgress(double overallProgress) { this.overallProgress = Math.max(0.0, Math.min(1.0, overallProgress)); }
  public void setSubStepDetails(String subStepDetails) { this.subStepDetails = subStepDetails; }
  public void setEstimatedCompletionTime(LocalDateTime estimatedCompletionTime) { this.estimatedCompletionTime = estimatedCompletionTime; }

  // Convenience methods for job lifecycle
  public boolean isActive() { return state == JobState.RUNNING; }
  public boolean isCompleted() { return state == JobState.COMPLETED || state == JobState.FAILED || state == JobState.CANCELLED; }
  public boolean isCancelled() { return state == JobState.CANCELLED; }

  // Helper method to cancel job
  public void cancel(String reason) {
    this.state = JobState.CANCELLED;
    this.cancelledAt = LocalDateTime.now();
    this.cancellationReason = reason;
  }
}