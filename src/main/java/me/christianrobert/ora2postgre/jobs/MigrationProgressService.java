package me.christianrobert.ora2postgre.jobs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Service for tracking detailed progress of migration jobs.
 * Integrates with JobManager to provide step-by-step progress updates.
 */
@ApplicationScoped
public class MigrationProgressService {
    
    private static final Logger log = LoggerFactory.getLogger(MigrationProgressService.class);
    
    @Inject
    JobManager jobManager;
    
    /**
     * Initialize progress tracking for a migration job
     */
    public void initializeJobProgress(String jobId, MigrationStep firstStep) {
        JobStatus jobStatus = jobManager.getJobStatus(jobId);
        if (jobStatus != null) {
            jobStatus.setCurrentStep(firstStep.getDisplayName());
            jobStatus.setCurrentStepNumber(firstStep.getStepNumber());
            jobStatus.setTotalSteps(MigrationStep.getTotalSteps());
            jobStatus.setStepProgress(0.0);
            jobStatus.setOverallProgress(0.0);
            jobStatus.setSubStepDetails("Starting " + firstStep.getDisplayName());
            
            log.info("Initialized progress tracking for job {} starting with step: {}", 
                    jobId, firstStep.getDisplayName());
        }
    }
    
    /**
     * Update progress within the current step
     */
    public void updateSubStepProgress(String jobId, MigrationStep currentStep, int completedSubSteps, String currentSubStepDetails) {
        JobStatus jobStatus = jobManager.getJobStatus(jobId);
        if (jobStatus != null) {
            double stepProgress = (double) completedSubSteps / currentStep.getSubStepCount();
            double overallProgress = currentStep.calculateOverallProgress(stepProgress);
            
            jobStatus.setStepProgress(stepProgress);
            jobStatus.setOverallProgress(overallProgress);
            jobStatus.setSubStepDetails(currentSubStepDetails);
            
            updateEstimatedCompletion(jobStatus, overallProgress);
            
            log.debug("Updated sub-step progress for job {}: step {}/{} ({}/{}), overall {}%", 
                    jobId, currentStep.getStepNumber(), MigrationStep.getTotalSteps(), 
                    completedSubSteps, currentStep.getSubStepCount(), 
                    Math.round(overallProgress * 100));
        }
    }
    
    /**
     * Move to the next step in the migration workflow
     */
    public void advanceToNextStep(String jobId, MigrationStep nextStep) {
        JobStatus jobStatus = jobManager.getJobStatus(jobId);
        if (jobStatus != null) {
            // Complete current step
            if (jobStatus.getCurrentStep() != null) {
                log.info("Completed step: {} for job {}", jobStatus.getCurrentStep(), jobId);
            }
            
            // Initialize next step
            jobStatus.setCurrentStep(nextStep.getDisplayName());
            jobStatus.setCurrentStepNumber(nextStep.getStepNumber());
            jobStatus.setStepProgress(0.0);
            jobStatus.setOverallProgress(nextStep.getStartingPercentage());
            jobStatus.setSubStepDetails("Starting " + nextStep.getDisplayName());
            
            updateEstimatedCompletion(jobStatus, nextStep.getStartingPercentage());
            
            log.info("Advanced to step {}/{}: {} for job {}", 
                    nextStep.getStepNumber(), MigrationStep.getTotalSteps(), 
                    nextStep.getDisplayName(), jobId);
        }
    }
    
    /**
     * Mark the entire job as completed
     */
    public void completeJob(String jobId) {
        JobStatus jobStatus = jobManager.getJobStatus(jobId);
        if (jobStatus != null) {
            jobStatus.setOverallProgress(1.0);
            jobStatus.setStepProgress(1.0);
            jobStatus.setSubStepDetails("Migration completed successfully");
            jobStatus.setEstimatedCompletionTime(LocalDateTime.now());
            
            log.info("Completed migration job {}", jobId);
        }
    }
    
    /**
     * Handle step failure with detailed error information
     */
    public void handleStepFailure(String jobId, MigrationStep failedStep, String error, Exception exception) {
        JobStatus jobStatus = jobManager.getJobStatus(jobId);
        if (jobStatus != null) {
            String errorDetails = String.format("Failed at step %d/%d (%s): %s", 
                    failedStep.getStepNumber(), MigrationStep.getTotalSteps(), 
                    failedStep.getDisplayName(), error);
            
            jobStatus.setSubStepDetails(errorDetails);
            jobStatus.setError(error);
            
            log.error("Step failure in job {} at step {}: {}", 
                    jobId, failedStep.getDisplayName(), error, exception);
        }
    }
    
    /**
     * Enhanced data transfer progress tracking with dynamic table-by-table updates.
     * Calculates progress based on actual table counts and current table being processed.
     */
    public void updateDynamicDataTransferProgress(String jobId, int currentTableIndex, int totalTables, 
                                                String currentTableName, boolean isStarting, 
                                                Long rowsTransferredSoFar) {
        JobStatus jobStatus = jobManager.getJobStatus(jobId);
        if (jobStatus != null) {
            MigrationStep transferStep = MigrationStep.TRANSFERDATA;
            
            // Calculate step progress based on table completion
            double stepProgress;
            String transferDetails;
            
            if (isStarting) {
                // Starting a table - progress is based on tables completed so far
                stepProgress = (double) currentTableIndex / totalTables;
                transferDetails = String.format("Transferring table %d of %d: %s", 
                    currentTableIndex + 1, totalTables, currentTableName);
            } else {
                // Completed a table - progress includes this completed table
                stepProgress = (double) (currentTableIndex + 1) / totalTables;
                String rowsInfo = rowsTransferredSoFar != null ? 
                    String.format(" (%,d rows)", rowsTransferredSoFar) : "";
                transferDetails = String.format("Completed table %d of %d: %s%s", 
                    currentTableIndex + 1, totalTables, currentTableName, rowsInfo);
            }
            
            // Update job status
            jobStatus.setStepProgress(stepProgress);
            
            // Calculate overall progress 
            double overallProgress = transferStep.calculateOverallProgress(stepProgress);
            jobStatus.setOverallProgress(overallProgress);
            
            // Update details
            jobStatus.setSubStepDetails(transferDetails);
            
            updateEstimatedCompletion(jobStatus, overallProgress);
            
            // Log table starts and completions
            log.info("Data transfer job {}: {} ({}% of step, {}% overall)", 
                    jobId, transferDetails, Math.round(stepProgress * 100), Math.round(overallProgress * 100));
        }
    }
    
    /**
     * Special handling for data transfer progress integration
     * This bridges the existing TransferProgress with our step-based progress
     */
    public void updateDataTransferProgress(String jobId, double transferProgress, String transferDetails) {
        JobStatus jobStatus = jobManager.getJobStatus(jobId);
        if (jobStatus != null) {
            MigrationStep transferStep = MigrationStep.TRANSFERDATA;
            
            // Update step progress with transfer progress
            jobStatus.setStepProgress(transferProgress);
            
            // Calculate overall progress 
            double overallProgress = transferStep.calculateOverallProgress(transferProgress);
            jobStatus.setOverallProgress(overallProgress);
            
            // Update details
            jobStatus.setSubStepDetails(transferDetails);
            
            updateEstimatedCompletion(jobStatus, overallProgress);
            
            // Log less frequently for data transfer to avoid spam
            if (Math.round(transferProgress * 100) % 10 == 0) { // Log every 10%
                log.info("Data transfer progress for job {}: {}% (overall: {}%)", 
                        jobId, Math.round(transferProgress * 100), Math.round(overallProgress * 100));
            }
        }
    }
    
    /**
     * Calculate and update estimated completion time based on current progress
     */
    private void updateEstimatedCompletion(JobStatus jobStatus, double overallProgress) {
        if (overallProgress > 0.0 && jobStatus.getStartedAt() != null) {
            long elapsedMinutes = ChronoUnit.MINUTES.between(jobStatus.getStartedAt(), LocalDateTime.now());
            
            if (elapsedMinutes > 0) { // Avoid division by zero
                double estimatedTotalMinutes = elapsedMinutes / overallProgress;
                double remainingMinutes = estimatedTotalMinutes - elapsedMinutes;
                
                if (remainingMinutes > 0) {
                    LocalDateTime estimatedCompletion = LocalDateTime.now().plusMinutes((long) remainingMinutes);
                    jobStatus.setEstimatedCompletionTime(estimatedCompletion);
                }
            }
        }
    }
    
    /**
     * Get detailed progress information for a specific job
     */
    public JobProgressInfo getDetailedProgress(String jobId) {
        JobStatus jobStatus = jobManager.getJobStatus(jobId);
        if (jobStatus == null) {
            return null;
        }
        
        return new JobProgressInfo(jobStatus);
    }
    
    /**
     * Progress information class for API responses
     */
    public static class JobProgressInfo {
        private final String jobId;
        private final String jobType;
        private final JobState state;
        private final String currentStep;
        private final int currentStepNumber;
        private final int totalSteps;
        private final int stepProgressPercentage;
        private final int overallProgressPercentage;
        private final String subStepDetails;
        private final LocalDateTime startedAt;
        private final LocalDateTime estimatedCompletionTime;
        private final String error;
        
        public JobProgressInfo(JobStatus jobStatus) {
            this.jobId = jobStatus.getJobId();
            this.jobType = jobStatus.getJobType();
            this.state = jobStatus.getState();
            this.currentStep = jobStatus.getCurrentStep();
            this.currentStepNumber = jobStatus.getCurrentStepNumber();
            this.totalSteps = jobStatus.getTotalSteps();
            this.stepProgressPercentage = jobStatus.getStepProgressPercentage();
            this.overallProgressPercentage = jobStatus.getOverallProgressPercentage();
            this.subStepDetails = jobStatus.getSubStepDetails();
            this.startedAt = jobStatus.getStartedAt();
            this.estimatedCompletionTime = jobStatus.getEstimatedCompletionTime();
            this.error = jobStatus.getError();
        }
        
        // Getters
        public String getJobId() { return jobId; }
        public String getJobType() { return jobType; }
        public JobState getState() { return state; }
        public String getCurrentStep() { return currentStep; }
        public int getCurrentStepNumber() { return currentStepNumber; }
        public int getTotalSteps() { return totalSteps; }
        public int getStepProgressPercentage() { return stepProgressPercentage; }
        public int getOverallProgressPercentage() { return overallProgressPercentage; }
        public String getSubStepDetails() { return subStepDetails; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public LocalDateTime getEstimatedCompletionTime() { return estimatedCompletionTime; }
        public String getError() { return error; }
    }
}