package me.christianrobert.ora2postgre.jobs;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.CancellationException;
import java.util.Map;
import java.util.LinkedList;
import java.util.Queue;

@ApplicationScoped
public class JobManager {

  private static final Logger logger = LoggerFactory.getLogger(JobManager.class);

  private final Map<String, JobStatus> jobs = new ConcurrentHashMap<>();
  private final Map<String, CompletableFuture<Void>> runningJobs = new ConcurrentHashMap<>();
  private final ExecutorService executor = Executors.newFixedThreadPool(5);
  private final ReentrantLock globalJobLock = new ReentrantLock();
  private final Queue<PendingJob> jobQueue = new LinkedList<>();
  private volatile String currentRunningJobId = null;

  public String startJob(String jobType, Runnable task) {
    String jobId = UUID.randomUUID().toString();

    globalJobLock.lock();
    try {
      if (currentRunningJobId != null) {
        // Queue the job if another job is running
        jobQueue.offer(new PendingJob(jobId, jobType, task, null));
        JobStatus status = new JobStatus(jobId, jobType, JobState.RUNNING, LocalDateTime.now());
        status.setMessage("Job queued - waiting for current job to complete");
        jobs.put(jobId, status);
        logger.info("Job {} of type '{}' queued - current job {} is running", jobId, jobType, currentRunningJobId);
        return jobId;
      }

      // Start the job immediately
      currentRunningJobId = jobId;
      JobStatus status = new JobStatus(jobId, jobType, JobState.RUNNING, LocalDateTime.now());
      jobs.put(jobId, status);

      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        try {
          logger.info("Starting job {} of type: {}", jobId, jobType);
          task.run();

          // Check if job was cancelled
          if (status.isCancelled()) {
            logger.info("Job {} was cancelled during execution", jobId);
            return;
          }

          status.setState(JobState.COMPLETED);
          status.setCompletedAt(LocalDateTime.now());
          status.setMessage("Job completed successfully");
          logger.info("Job {} completed successfully", jobId);
        } catch (CancellationException e) {
          logger.info("Job {} was cancelled", jobId);
          status.cancel("Job was cancelled");
        } catch (Exception e) {
          logger.error("Job {} of type '{}' failed with exception", jobId, jobType, e);

          status.setState(JobState.FAILED);
          status.setCompletedAt(LocalDateTime.now());
          status.setMessage("Job failed: " + e.getMessage());
          status.setError(e.getClass().getSimpleName() + ": " + e.getMessage());

          logger.error("Job failure details - ID: {}, Type: {}, Error: {}",
                  jobId, jobType, e.getMessage());
        } finally {
          // Job finished, start next job in queue
          processNextJob();
        }
      }, executor);

      runningJobs.put(jobId, future);
      return jobId;
    } finally {
      globalJobLock.unlock();
    }
  }

  /**
   * Start a job with access to the jobId for progress tracking
   */
  public String startJobWithId(String jobType, java.util.function.Consumer<String> task) {
    String jobId = UUID.randomUUID().toString();

    globalJobLock.lock();
    try {
      if (currentRunningJobId != null) {
        // Queue the job if another job is running
        jobQueue.offer(new PendingJob(jobId, jobType, null, task));
        JobStatus status = new JobStatus(jobId, jobType, JobState.RUNNING, LocalDateTime.now());
        status.setMessage("Job queued - waiting for current job to complete");
        jobs.put(jobId, status);
        logger.info("Job {} of type '{}' queued - current job {} is running", jobId, jobType, currentRunningJobId);
        return jobId;
      }

      // Start the job immediately
      currentRunningJobId = jobId;
      JobStatus status = new JobStatus(jobId, jobType, JobState.RUNNING, LocalDateTime.now());
      jobs.put(jobId, status);

      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        try {
          logger.info("Starting job {} of type: {}", jobId, jobType);
          task.accept(jobId);

          // Check if job was cancelled
          if (status.isCancelled()) {
            logger.info("Job {} was cancelled during execution", jobId);
            return;
          }

          status.setState(JobState.COMPLETED);
          status.setCompletedAt(LocalDateTime.now());
          status.setMessage("Job completed successfully");
          logger.info("Job {} completed successfully", jobId);
        } catch (CancellationException e) {
          logger.info("Job {} was cancelled", jobId);
          status.cancel("Job was cancelled");
        } catch (Exception e) {
          logger.error("Job {} of type '{}' failed with exception", jobId, jobType, e);

          status.setState(JobState.FAILED);
          status.setCompletedAt(LocalDateTime.now());
          status.setMessage("Job failed: " + e.getMessage());
          status.setError(e.getClass().getSimpleName() + ": " + e.getMessage());

          logger.error("Job failure details - ID: {}, Type: {}, Error: {}",
                  jobId, jobType, e.getMessage());
        } finally {
          // Job finished, start next job in queue
          processNextJob();
        }
      }, executor);

      runningJobs.put(jobId, future);
      return jobId;
    } finally {
      globalJobLock.unlock();
    }
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

  public boolean isAnyJobRunning() {
    return currentRunningJobId != null;
  }

  public void clearCompletedJobs() {
    jobs.entrySet().removeIf(entry ->
            entry.getValue().getState() == JobState.COMPLETED ||
                    entry.getValue().getState() == JobState.FAILED ||
                    entry.getValue().getState() == JobState.CANCELLED);
  }

  /**
   * Cancel a specific job by ID
   */
  public boolean cancelJob(String jobId, String reason) {
    JobStatus status = jobs.get(jobId);
    if (status == null) {
      return false;
    }

    if (status.isCompleted()) {
      logger.warn("Cannot cancel job {} - already completed with state: {}", jobId, status.getState());
      return false;
    }

    // Cancel the job
    status.cancel(reason);

    // Cancel the CompletableFuture if it's running
    CompletableFuture<Void> future = runningJobs.get(jobId);
    if (future != null) {
      future.cancel(true);
      runningJobs.remove(jobId);
    }

    // If this is the current running job, process next job
    if (jobId.equals(currentRunningJobId)) {
      processNextJob();
    }

    logger.info("Job {} cancelled: {}", jobId, reason);
    return true;
  }

  /**
   * Cancel all jobs (running and queued)
   */
  public void cancelAllJobs(String reason) {
    logger.info("Cancelling all jobs: {}", reason);

    globalJobLock.lock();
    try {
      // Cancel all running jobs
      for (Map.Entry<String, CompletableFuture<Void>> entry : runningJobs.entrySet()) {
        String jobId = entry.getKey();
        CompletableFuture<Void> future = entry.getValue();

        JobStatus status = jobs.get(jobId);
        if (status != null && !status.isCompleted()) {
          status.cancel(reason);
        }

        future.cancel(true);
        logger.info("Cancelled running job: {}", jobId);
      }

      // Cancel all queued jobs
      while (!jobQueue.isEmpty()) {
        PendingJob pendingJob = jobQueue.poll();
        JobStatus status = jobs.get(pendingJob.getJobId());
        if (status != null) {
          status.cancel(reason);
        }
        logger.info("Cancelled queued job: {}", pendingJob.getJobId());
      }

      // Clear state
      runningJobs.clear();
      currentRunningJobId = null;

    } finally {
      globalJobLock.unlock();
    }
  }

  /**
   * Get current running job ID
   */
  public String getCurrentRunningJobId() {
    return currentRunningJobId;
  }

  /**
   * Get number of queued jobs
   */
  public int getQueuedJobCount() {
    return jobQueue.size();
  }

  /**
   * Process the next job in the queue
   */
  private void processNextJob() {
    globalJobLock.lock();
    try {
      // Clear current running job
      currentRunningJobId = null;

      // Get next job from queue
      PendingJob nextJob = jobQueue.poll();
      if (nextJob == null) {
        return; // No more jobs in queue
      }

      // Start the next job
      currentRunningJobId = nextJob.getJobId();
      JobStatus status = jobs.get(nextJob.getJobId());
      if (status != null) {
        status.setMessage("Job started from queue");
      }

      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        try {
          logger.info("Starting queued job {} of type: {}", nextJob.getJobId(), nextJob.getJobType());

          // Execute the appropriate task
          if (nextJob.getSimpleTask() != null) {
            nextJob.getSimpleTask().run();
          } else if (nextJob.getTaskWithId() != null) {
            nextJob.getTaskWithId().accept(nextJob.getJobId());
          }

          // Check if job was cancelled
          if (status != null && status.isCancelled()) {
            logger.info("Queued job {} was cancelled during execution", nextJob.getJobId());
            return;
          }

          if (status != null) {
            status.setState(JobState.COMPLETED);
            status.setCompletedAt(LocalDateTime.now());
            status.setMessage("Job completed successfully");
          }
          logger.info("Queued job {} completed successfully", nextJob.getJobId());

        } catch (CancellationException e) {
          logger.info("Queued job {} was cancelled", nextJob.getJobId());
          if (status != null) {
            status.cancel("Job was cancelled");
          }
        } catch (Exception e) {
          logger.error("Queued job {} of type '{}' failed with exception", nextJob.getJobId(), nextJob.getJobType(), e);

          if (status != null) {
            status.setState(JobState.FAILED);
            status.setCompletedAt(LocalDateTime.now());
            status.setMessage("Job failed: " + e.getMessage());
            status.setError(e.getClass().getSimpleName() + ": " + e.getMessage());
          }
        } finally {
          // Remove from running jobs and process next
          runningJobs.remove(nextJob.getJobId());
          processNextJob();
        }
      }, executor);

      runningJobs.put(nextJob.getJobId(), future);

    } finally {
      globalJobLock.unlock();
    }
  }

  /**
   * Inner class to represent a pending job
   */
  private static class PendingJob {
    private final String jobId;
    private final String jobType;
    private final Runnable simpleTask;
    private final java.util.function.Consumer<String> taskWithId;

    public PendingJob(String jobId, String jobType, Runnable simpleTask, java.util.function.Consumer<String> taskWithId) {
      this.jobId = jobId;
      this.jobType = jobType;
      this.simpleTask = simpleTask;
      this.taskWithId = taskWithId;
    }

    public String getJobId() { return jobId; }
    public String getJobType() { return jobType; }
    public Runnable getSimpleTask() { return simpleTask; }
    public java.util.function.Consumer<String> getTaskWithId() { return taskWithId; }
  }

  public Map getJobError() {
    Map<String, String> error = new HashMap<>();
    error.put("status", "error");
    error.put("message", "Another job is already running. Only one job can run at a time.");
    error.put("currentJobId", this.getCurrentRunningJobId());
    return error;
  }
}