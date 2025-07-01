package com.sayiza.oracle2postgre.jobs;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;

@ApplicationScoped
public class JobManager {
    
    private static final Logger logger = LoggerFactory.getLogger(JobManager.class);
    
    private final Map<String, JobStatus> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    
    public String startJob(String jobType, Runnable task) {
        String jobId = UUID.randomUUID().toString();
        JobStatus status = new JobStatus(jobId, jobType, JobState.RUNNING, LocalDateTime.now());
        jobs.put(jobId, status);
        
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting job {} of type: {}", jobId, jobType);
                task.run();
                status.setState(JobState.COMPLETED);
                status.setCompletedAt(LocalDateTime.now());
                status.setMessage("Job completed successfully");
                logger.info("Job {} completed successfully", jobId);
            } catch (Exception e) {
                // Log comprehensive error information for system logs monitoring
                logger.error("Job {} of type '{}' failed with exception", jobId, jobType, e);
                
                status.setState(JobState.FAILED);
                status.setCompletedAt(LocalDateTime.now());
                status.setMessage("Job failed: " + e.getMessage());
                status.setError(e.getClass().getSimpleName() + ": " + e.getMessage());
                
                // Log additional context for troubleshooting
                logger.error("Job failure details - ID: {}, Type: {}, Error: {}", 
                    jobId, jobType, e.getMessage());
            }
        }, executor);
        
        return jobId;
    }
    
    /**
     * Start a job with access to the jobId for progress tracking
     */
    public String startJobWithId(String jobType, java.util.function.Consumer<String> task) {
        String jobId = UUID.randomUUID().toString();
        JobStatus status = new JobStatus(jobId, jobType, JobState.RUNNING, LocalDateTime.now());
        jobs.put(jobId, status);
        
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting job {} of type: {}", jobId, jobType);
                task.accept(jobId);
                status.setState(JobState.COMPLETED);
                status.setCompletedAt(LocalDateTime.now());
                status.setMessage("Job completed successfully");
                logger.info("Job {} completed successfully", jobId);
            } catch (Exception e) {
                // Log comprehensive error information for system logs monitoring
                logger.error("Job {} of type '{}' failed with exception", jobId, jobType, e);
                
                status.setState(JobState.FAILED);
                status.setCompletedAt(LocalDateTime.now());
                status.setMessage("Job failed: " + e.getMessage());
                status.setError(e.getClass().getSimpleName() + ": " + e.getMessage());
                
                // Log additional context for troubleshooting
                logger.error("Job failure details - ID: {}, Type: {}, Error: {}", 
                    jobId, jobType, e.getMessage());
            }
        }, executor);
        
        return jobId;
    }
    
    public JobStatus getJobStatus(String jobId) {
        return jobs.get(jobId);
    }
    
    public Map<String, JobStatus> getAllJobs() {
        return Map.copyOf(jobs);
    }
    
    public boolean isJobRunning(String jobType) {
        return jobs.values().stream()
            .anyMatch(job -> job.getJobType().equals(jobType) && job.getState() == JobState.RUNNING);
    }
    
    public void clearCompletedJobs() {
        jobs.entrySet().removeIf(entry -> 
            entry.getValue().getState() == JobState.COMPLETED || 
            entry.getValue().getState() == JobState.FAILED);
    }
}