// Global state
let autoRefreshInterval = null;
let isAutoRefreshEnabled = false;
let lastKnownExecuteJobState = null;

// Initialize the dashboard
document.addEventListener('DOMContentLoaded', function() {
  refreshSystemStatus();
  refreshDataOverview();
  refreshTargetStats();
  refreshJobs();
  refreshLogs();
  loadConfiguration(); // Load current configuration on startup

  // Start periodic updates
  setInterval(refreshSystemStatus, 10000); // Every 10 seconds
  setInterval(refreshDataOverview, 15000); // Every 15 seconds
  setInterval(refreshJobs, 4000); // Every 4 seconds
  // Note: Target stats are only refreshed manually or when execute job completes
});

// API Functions
async function startJob(jobType) {
  try {
    const response = await fetch(`/migration/${jobType}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    });

    const result = await response.json();

    if (response.ok) {
      showNotification(`${jobType} job started successfully`, 'success');
      refreshJobs();
    } else {
      showNotification(`Failed to start ${jobType} job: ${result.message}`, 'error');
    }
  } catch (error) {
    showNotification(`Error starting ${jobType} job: ${error.message}`, 'error');
  }
}

async function resetSystem() {
  if (!confirm('Are you sure you want to reset the system? This will clear all extracted data.')) {
    return;
  }

  try {
    const response = await fetch('/migration/reset', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    });

    const result = await response.json();

    if (response.ok) {
      showNotification('System reset successfully', 'success');
      refreshDataOverview();
      refreshJobs();
    } else {
      showNotification(`Failed to reset system: ${result.message}`, 'error');
    }
  } catch (error) {
    showNotification(`Error resetting system: ${error.message}`, 'error');
  }
}

async function refreshSystemStatus() {
  try {
    const response = await fetch('/migration/health');
    if (response.ok) {
      const health = await response.json();
      updateSystemStatus(health);
      updateServiceStatus(true);
    } else {
      updateServiceStatus(false);
    }
  } catch (error) {
    console.error('Error fetching health status:', error);
    updateServiceStatus(false);
    // Also update database statuses as disconnected when service is down
    updateSystemStatus({
      oracle: { connected: false, error: 'Service unavailable' },
      postgres: { connected: false, error: 'Service unavailable' }
    });
  }
}

async function refreshDataOverview() {
  try {
    const response = await fetch('/migration/status');
    if (response.ok) {
      const data = await response.json();
      updateDataOverview(data);
    } else {
      // Reset data overview if service is unavailable
      updateDataOverview({});
    }
  } catch (error) {
    console.error('Error fetching data overview:', error);
    updateDataOverview({});
  }
}

async function refreshJobs() {
  try {
    const response = await fetch('/migration/jobs');
    if (response.ok) {
      const jobs = await response.json();
      updateJobsList(jobs);
      updateJobStatuses(jobs);
    } else {
      // Clear jobs if service is unavailable
      updateJobsList([]);
      updateJobStatuses([]);
    }
  } catch (error) {
    console.error('Error fetching jobs:', error);
    updateJobsList([]);
    updateJobStatuses([]);
  }
}

async function refreshLogs() {
  try {
    const response = await fetch('/migration/logs');
    if (response.ok) {
      const logs = await response.text();
      updateLogs(logs);
    } else {
      document.getElementById('log-content').textContent = 'Service unavailable - cannot load logs';
    }
  } catch (error) {
    console.error('Error fetching logs:', error);
    document.getElementById('log-content').textContent = 'Service unavailable - cannot load logs';
  }
}

async function refreshTargetStats() {
  try {
    const response = await fetch('/migration/target-stats');
    if (response.ok) {
      const result = await response.json();
      updateTargetStats(result.stats, result.fetchedAt);
    } else {
      // Reset target stats if service is unavailable
      const errorResult = await response.json().catch(() => ({}));
      const errorMessage = errorResult.message || 'Service unavailable';
      updateTargetStats({}, 0, errorMessage);
    }
  } catch (error) {
    console.error('Error fetching target stats:', error);
    updateTargetStats({}, 0, 'Connection error');
  }
}

// UI Update Functions
function updateSystemStatus(health) {
  const oracleStatus = document.getElementById('oracle-status');
  const postgresStatus = document.getElementById('postgres-status');

  if (health.oracle && health.oracle.connected) {
    oracleStatus.className = 'status-item connected';
    oracleStatus.lastElementChild.textContent = 'Connected';
    oracleStatus.lastElementChild.style.color = '#4CAF50';
  } else {
    oracleStatus.className = 'status-item disconnected';
    const errorMsg = health.oracle && health.oracle.error ? health.oracle.error : 'Disconnected';
    oracleStatus.lastElementChild.textContent = errorMsg;
    oracleStatus.lastElementChild.style.color = '#f44336';
  }

  if (health.postgres && health.postgres.connected) {
    postgresStatus.className = 'status-item connected';
    postgresStatus.lastElementChild.textContent = 'Connected';
    postgresStatus.lastElementChild.style.color = '#4CAF50';
  } else {
    postgresStatus.className = 'status-item disconnected';
    const errorMsg = health.postgres && health.postgres.error ? health.postgres.error : 'Disconnected';
    postgresStatus.lastElementChild.textContent = errorMsg;
    postgresStatus.lastElementChild.style.color = '#f44336';
  }
}

function updateServiceStatus(isOnline) {
  const serviceStatus = document.getElementById('service-status');

  if (isOnline) {
    serviceStatus.className = 'status-item connected';
    serviceStatus.lastElementChild.textContent = 'Online';
    serviceStatus.lastElementChild.style.color = '#4CAF50';
  } else {
    serviceStatus.className = 'status-item disconnected';
    serviceStatus.lastElementChild.textContent = 'Offline';
    serviceStatus.lastElementChild.style.color = '#f44336';
  }
}

function updateDataOverview(data) {
  // Extraction Phase Data
  document.getElementById('schemas-count').textContent = data.schemas || 0;
  document.getElementById('tables-count').textContent = data.tables || 0;
  document.getElementById('views-count').textContent = data.views || 0;
  document.getElementById('synonyms-count').textContent = data.synonyms || 0;
  document.getElementById('objectTypeSpecs-count').textContent = data.objectTypeSpecs || 0;
  document.getElementById('objectTypeBodies-count').textContent = data.objectTypeBodies || 0;
  document.getElementById('packageSpecs-count').textContent = data.packageSpecs || 0;
  document.getElementById('packageBodies-count').textContent = data.packageBodies || 0;
  document.getElementById('triggers-count').textContent = data.triggers || 0;
  document.getElementById('constraints-count').textContent = data.constraints || 0;
  document.getElementById('indexes-count').textContent = data.indexes || 0;

  // Parse AST Phase Data
  document.getElementById('parsedViews-count').textContent = data.parsedViews || 0;
  document.getElementById('parsedObjectTypes-count').textContent = data.parsedObjectTypes || 0;
  document.getElementById('parsedPackages-count').textContent = data.parsedPackages || 0;
  document.getElementById('parsedTriggers-count').textContent = data.parsedTriggers || 0;

  // Row Count Data (format with commas for large numbers)
  const totalRowCount = data.totalRowCount || 0;
  document.getElementById('totalRowCount-count').textContent = totalRowCount.toLocaleString();
}

function updateTargetStats(stats, fetchedAt, errorMessage) {
  // Update target database counts
  document.getElementById('target-schemas-count').textContent = stats.schemas || 0;
  document.getElementById('target-tables-count').textContent = stats.tables || 0;
  document.getElementById('target-views-count').textContent = stats.views || 0;
  document.getElementById('target-functions-count').textContent = stats.functions || 0;
  document.getElementById('target-procedures-count').textContent = stats.procedures || 0;
  document.getElementById('target-types-count').textContent = stats.types || 0;
  document.getElementById('target-triggers-count').textContent = stats.triggers || 0;
  document.getElementById('target-constraints-count').textContent = stats.constraints || 0;
  document.getElementById('target-indexes-count').textContent = stats.indexes || 0;

  // Update target row count (format with commas for large numbers)
  const targetTotalRowCount = stats.totalRowCount || 0;
  document.getElementById('target-totalRowCount-count').textContent = targetTotalRowCount.toLocaleString();

  // Update last fetched timestamp
  const lastUpdatedElement = document.getElementById('target-last-updated');
  if (errorMessage) {
    lastUpdatedElement.textContent = `Error: ${errorMessage}`;
    lastUpdatedElement.style.color = '#f44336';
  } else if (fetchedAt && fetchedAt > 0) {
    const fetchTime = new Date(fetchedAt);
    lastUpdatedElement.textContent = `Last fetched: ${fetchTime.toLocaleString()}`;
    lastUpdatedElement.style.color = '#666';
  } else {
    lastUpdatedElement.textContent = 'Not fetched yet';
    lastUpdatedElement.style.color = '#666';
  }
}

function updateJobsList(jobs) {
  const jobList = document.getElementById('job-list');

  // Convert jobs object to array if needed
  const jobsArray = Array.isArray(jobs) ? jobs : Object.values(jobs);

  if (jobsArray.length === 0) {
    jobList.innerHTML = '<div style="text-align: center; color: #666; padding: 2rem;">No active jobs</div>';
    return;
  }

  // Sort jobs by startedAt in descending order (newest first)
  const sortedJobs = jobsArray.sort((a, b) => new Date(b.startedAt) - new Date(a.startedAt));

  jobList.innerHTML = sortedJobs.map(job => {
    const progressHtml = renderJobProgress(job);
    return `
            <div class="job-item">
                <div class="job-info">
                    <div class="job-type">
                        <span class="status-indicator status-${job.state.toLowerCase()}"></span>
                        ${job.jobType.toUpperCase()} Job
                    </div>
                    <div class="job-time">
                        Started: ${new Date(job.startedAt).toLocaleString()}
                        ${job.completedAt ? `| Completed: ${new Date(job.completedAt).toLocaleString()}` : ''}
                    </div>
                    ${progressHtml}
                    ${job.message ? `<div style="font-size: 0.8rem; color: #666; margin-top: 0.2rem;">${job.message}</div>` : ''}
                    ${job.error ? `<div class="error-message">${job.error}</div>` : ''}
                </div>
            </div>
        `;
  }).join('');
}

function renderJobProgress(job) {
  // Only show progress for running jobs or jobs with progress information
  if (job.state !== 'RUNNING' && !job.overallProgress && !job.currentStep) {
    return '';
  }

  const overallProgress = job.overallProgressPercentage || job.overallProgress * 100 || 0;
  const stepProgress = job.stepProgressPercentage || job.stepProgress * 100 || 0;
  const currentStep = job.currentStep || '';
  const currentStepNumber = job.currentStepNumber || 0;
  const totalSteps = job.totalSteps || 6;
  const subStepDetails = job.subStepDetails || '';
  const estimatedCompletion = job.estimatedCompletionTime;

  // Determine progress bar style based on job state
  let progressFillClass = 'progress-fill';
  if (job.state === 'COMPLETED') {
    progressFillClass += ' completed';
  } else if (job.state === 'FAILED') {
    progressFillClass += ' failed';
  }

  // Format estimated completion time
  let etaText = '';
  if (estimatedCompletion && job.state === 'RUNNING') {
    const eta = new Date(estimatedCompletion);
    const now = new Date();
    if (eta > now) {
      const diffMinutes = Math.round((eta - now) / (1000 * 60));
      if (diffMinutes > 0) {
        etaText = `<span class="progress-eta">ETA: ${diffMinutes}min</span>`;
      }
    }
  }

  return `
        <div class="progress-container">
            <div class="progress-bar">
                <div class="${progressFillClass}" style="width: ${Math.max(0, Math.min(100, overallProgress))}%"></div>
            </div>
            <div class="progress-text">
                <span>Overall: ${Math.round(overallProgress)}%</span>
                ${etaText}
            </div>
            ${currentStep ? `
                <div class="progress-step">
                    Step ${currentStepNumber}/${totalSteps}: ${currentStep} (${Math.round(stepProgress)}%)
                </div>
            ` : ''}
            ${subStepDetails ? `
                <div class="progress-step" style="color: #aaa; font-size: 0.7rem;">
                    ${subStepDetails}
                </div>
            ` : ''}
        </div>
    `;
}

function updateJobStatuses(jobs) {
  // Convert jobs object to array if needed
  const jobsArray = Array.isArray(jobs) ? jobs : Object.values(jobs);

  // Reset only job button indicators (not system health items)
  const jobIndicators = ['extract-status', 'parse-status', 'export-status', 'execute-pre-status', 'execute-post-status', 'full-status'];
  jobIndicators.forEach(indicatorId => {
    const indicator = document.getElementById(indicatorId);
    if (indicator) {
      indicator.className = 'status-indicator status-idle';
    }
  });

  // Track execute job state changes
  let currentExecuteJob = null;

  // Update based on current jobs
  jobsArray.forEach(job => {
    const indicator = document.getElementById(`${job.jobType}-status`);
    if (indicator && jobIndicators.includes(`${job.jobType}-status`)) {
      indicator.className = `status-indicator status-${job.state.toLowerCase()}`;
    }

    // Track execute and full migration jobs for target stats refresh
    if (job.jobType === 'execute-pre' || job.jobType === 'execute-post' || job.jobType === 'full') {
      currentExecuteJob = job;
    }
  });

  // Check if execute job just completed successfully
  if (currentExecuteJob &&
    currentExecuteJob.state === 'COMPLETED' &&
    lastKnownExecuteJobState !== 'COMPLETED') {

    console.log('Execute job completed, refreshing target database stats');
    refreshTargetStats();
    showNotification('Execute job completed - target database stats refreshed', 'success');
  }

  // Update the last known state
  if (currentExecuteJob) {
    lastKnownExecuteJobState = currentExecuteJob.state;
  } else {
    lastKnownExecuteJobState = null;
  }
}

function updateLogs(logs) {
  const logContent = document.getElementById('log-content');
  const filter = document.getElementById('log-filter').value.toLowerCase();

  if (filter) {
    const filteredLogs = logs.split('\n')
      .filter(line => line.toLowerCase().includes(filter))
      .join('\n');
    logContent.textContent = filteredLogs || 'No matching log entries';
  } else {
    logContent.textContent = logs || 'No logs available';
  }

  // Auto-scroll to bottom
  logContent.scrollTop = logContent.scrollHeight;
}

function toggleAutoRefresh() {
  const button = document.getElementById('auto-refresh-text');

  if (isAutoRefreshEnabled) {
    clearInterval(autoRefreshInterval);
    button.textContent = 'Start Auto-Refresh';
    isAutoRefreshEnabled = false;
  } else {
    autoRefreshInterval = setInterval(refreshLogs, 3000); // Every 3 seconds
    button.textContent = 'Stop Auto-Refresh';
    isAutoRefreshEnabled = true;
  }
}

function showNotification(message, type) {
  // Simple notification system
  const notification = document.createElement('div');
  notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 1rem 1.5rem;
        border-radius: 6px;
        color: white;
        font-weight: 500;
        z-index: 1000;
        animation: slideIn 0.3s ease;
        max-width: 400px;
        background: ${type === 'success' ? '#4CAF50' : '#f44336'};
    `;

  notification.textContent = message;
  document.body.appendChild(notification);

  setTimeout(() => {
    notification.style.animation = 'slideOut 0.3s ease';
    setTimeout(() => document.body.removeChild(notification), 300);
  }, 5000);
}

// Log filter functionality
document.addEventListener('DOMContentLoaded', function() {
  document.getElementById('log-filter').addEventListener('input', function() {
    refreshLogs();
  });
});

// Configuration Functions
async function loadConfiguration() {
  try {
    // First, try to load from localStorage
    const localConfig = localStorage.getItem('migrationConfig');
    if (localConfig) {
      try {
        const parsedLocalConfig = JSON.parse(localConfig);
        console.log('Loading configuration from localStorage');
        populateConfigurationForm(parsedLocalConfig);

        // Sync localStorage config to server to ensure consistency
        console.log('Syncing localStorage config to server');
        await syncConfigToServer(parsedLocalConfig);

        refreshSystemStatus();
        refreshTargetStats();

        showNotification('Configuration loaded from local storage and synced to server', 'success');
        return;
      } catch (e) {
        console.warn('Failed to parse localStorage config, falling back to server config');
        localStorage.removeItem('migrationConfig');
      }
    }

    // Fallback to server configuration
    const response = await fetch('/migration/config');
    if (response.ok) {
      const config = await response.json();
      populateConfigurationForm(config);
      console.log('Loading configuration from server (fallback)');
    } else {
      showNotification('Failed to load configuration from server', 'error');
    }
  } catch (error) {
    console.error('Error loading configuration:', error);
    showNotification('Error loading configuration: ' + error.message, 'error');
  }
}

async function syncConfigToServer(config) {
  try {
    const response = await fetch('/migration/config', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(config)
    });

    if (response.ok) {
      console.log('Successfully synced localStorage config to server');
    } else {
      const result = await response.json();
      console.warn('Failed to sync config to server:', result.message);
    }
  } catch (error) {
    console.warn('Error syncing config to server:', error.message);
    // Don't throw error here as config loading should still work
  }
}

async function saveConfiguration() {
  try {
    const config = getConfigurationFromForm();

    // Save to server
    const response = await fetch('/migration/config', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(config)
    });

    const result = await response.json();

    if (response.ok) {
      // Save to localStorage for persistence across restarts
      const fullConfig = result.config || config;
      localStorage.setItem('migrationConfig', JSON.stringify(fullConfig));

      showNotification('Configuration saved successfully (server + local storage)', 'success');
      // Refresh system health after configuration change
      refreshSystemStatus();
    } else {
      showNotification('Failed to save configuration: ' + result.message, 'error');
    }
  } catch (error) {
    console.error('Error saving configuration:', error);
    showNotification('Error saving configuration: ' + error.message, 'error');
  }
}

function resetConfiguration() {
  if (confirm('Are you sure you want to reset configuration to defaults? This will clear local storage and reload server configuration.')) {
    // Clear localStorage
    localStorage.removeItem('migrationConfig');

    // Force load from server
    loadConfigurationFromServer();
    showNotification('Configuration reset to server values (local storage cleared)', 'success');
  }
}

async function loadConfigurationFromServer() {
  try {
    const response = await fetch('/migration/config');
    if (response.ok) {
      const config = await response.json();
      populateConfigurationForm(config);
      showNotification('Configuration loaded from server', 'success');
      console.log('Loading configuration from server (forced)');
    } else {
      showNotification('Failed to load configuration from server', 'error');
    }
  } catch (error) {
    console.error('Error loading configuration from server:', error);
    showNotification('Error loading configuration: ' + error.message, 'error');
  }
}

function populateConfigurationForm(config) {
  // Process flags
  document.getElementById('doAllSchemas').checked = config.doAllSchemas || false;
  document.getElementById('doOnlyTestSchema').value = config.doOnlyTestSchema || '';
  document.getElementById('doTable').checked = config.doTable || false;
  document.getElementById('doSynonyms').checked = config.doSynonyms || false;
  document.getElementById('doData').checked = config.doData || false;
  document.getElementById('doObjectTypeSpec').checked = config.doObjectTypeSpec || false;
  document.getElementById('doObjectTypeBody').checked = config.doObjectTypeBody || false;
  document.getElementById('doPackageSpec').checked = config.doPackageSpec || false;
  document.getElementById('doPackageBody').checked = config.doPackageBody || false;
  document.getElementById('doViewSignature').checked = config.doViewSignature || false;
  document.getElementById('doViewDdl').checked = config.doViewDdl || false;
  document.getElementById('doTriggers').checked = config.doTriggers || false;
  document.getElementById('doConstraints').checked = config.doConstraints || false;
  document.getElementById('doIndexes').checked = config.doIndexes || false;
  document.getElementById('doWriteRestControllers').checked = config.doWriteRestControllers || false;
  document.getElementById('doWritePostgreFiles').checked = config.doWritePostgreFiles || false;
  document.getElementById('doExecutePostgreFiles').checked = config.doExecutePostgreFiles || false;
  document.getElementById('doRestControllerFunctions').checked = config.doRestControllerFunctions || false;
  document.getElementById('doRestControllerProcedures').checked = config.doRestControllerProcedures || false;

  // Connection settings
  document.getElementById('oracleUrl').value = config.oracleUrl || '';
  document.getElementById('oracleUser').value = config.oracleUser || '';
  document.getElementById('oraclePassword').value = config.oraclePassword || '';
  document.getElementById('postgreUrl').value = config.postgreUrl || '';
  document.getElementById('postgreUsername').value = config.postgreUsername || '';
  document.getElementById('postgrePassword').value = config.postgrePassword || '';

  // Path settings
  document.getElementById('javaGeneratedPackageName').value = config.javaGeneratedPackageName || '';
  document.getElementById('pathTargetProjectRoot').value = config.pathTargetProjectRoot || '';
  document.getElementById('pathTargetProjectJava').value = config.pathTargetProjectJava || '';
  document.getElementById('pathTargetProjectResources').value = config.pathTargetProjectResources || '';
  document.getElementById('pathTargetProjectPostgre').value = config.pathTargetProjectPostgre || '';
}

function getConfigurationFromForm() {
  return {
    // Process flags
    doAllSchemas: document.getElementById('doAllSchemas').checked,
    doOnlyTestSchema: document.getElementById('doOnlyTestSchema').value.trim(),
    doTable: document.getElementById('doTable').checked,
    doSynonyms: document.getElementById('doSynonyms').checked,
    doData: document.getElementById('doData').checked,
    doObjectTypeSpec: document.getElementById('doObjectTypeSpec').checked,
    doObjectTypeBody: document.getElementById('doObjectTypeBody').checked,
    doPackageSpec: document.getElementById('doPackageSpec').checked,
    doPackageBody: document.getElementById('doPackageBody').checked,
    doViewSignature: document.getElementById('doViewSignature').checked,
    doViewDdl: document.getElementById('doViewDdl').checked,
    doTriggers: document.getElementById('doTriggers').checked,
    doConstraints: document.getElementById('doConstraints').checked,
    doIndexes: document.getElementById('doIndexes').checked,
    doWriteRestControllers: document.getElementById('doWriteRestControllers').checked,
    doWritePostgreFiles: document.getElementById('doWritePostgreFiles').checked,
    doExecutePostgreFiles: document.getElementById('doExecutePostgreFiles').checked,
    doRestControllerFunctions: document.getElementById('doRestControllerFunctions').checked,
    doRestControllerProcedures: document.getElementById('doRestControllerProcedures').checked,

    // Connection settings - only include if not empty to avoid overriding with blank values
    ...(document.getElementById('oracleUrl').value.trim() && { oracleUrl: document.getElementById('oracleUrl').value.trim() }),
    ...(document.getElementById('oracleUser').value.trim() && { oracleUser: document.getElementById('oracleUser').value.trim() }),
    ...(document.getElementById('oraclePassword').value.trim() && { oraclePassword: document.getElementById('oraclePassword').value.trim() }),
    ...(document.getElementById('postgreUrl').value.trim() && { postgreUrl: document.getElementById('postgreUrl').value.trim() }),
    ...(document.getElementById('postgreUsername').value.trim() && { postgreUsername: document.getElementById('postgreUsername').value.trim() }),
    ...(document.getElementById('postgrePassword').value.trim() && { postgrePassword: document.getElementById('postgrePassword').value.trim() }),

    // Path settings - only include if not empty
    ...(document.getElementById('javaGeneratedPackageName').value.trim() && { javaGeneratedPackageName: document.getElementById('javaGeneratedPackageName').value.trim() }),
    ...(document.getElementById('pathTargetProjectRoot').value.trim() && { pathTargetProjectRoot: document.getElementById('pathTargetProjectRoot').value.trim() }),
    ...(document.getElementById('pathTargetProjectJava').value.trim() && { pathTargetProjectJava: document.getElementById('pathTargetProjectJava').value.trim() }),
    ...(document.getElementById('pathTargetProjectResources').value.trim() && { pathTargetProjectResources: document.getElementById('pathTargetProjectResources').value.trim() }),
    ...(document.getElementById('pathTargetProjectPostgre').value.trim() && { pathTargetProjectPostgre: document.getElementById('pathTargetProjectPostgre').value.trim() })
  };
}