export interface PipelineStage {
  id: number;
  stageName: string;
  status: 'WAITING' | 'RUNNING' | 'PASSED' | 'FAILED' | 'SKIPPED';
  executionOrder: number;
  durationMs?: number;
  logExcerpt?: string;
}

export interface PipelineRun {
  id: number;
  buildNumber: string;
  triggerType: string;
  commitSha: string;
  branch: string;
  commitMessage: string;
  status: 'QUEUED' | 'RUNNING' | 'PASSED' | 'FAILED' | 'CANCELLED';
  startTime: string;
  endTime?: string;
  durationSeconds?: number;
  stages: PipelineStage[];
}

export interface Deployment {
  id: number;
  versionTag: string;
  commitSha: string;
  environmentSlot: 'BLUE' | 'GREEN';
  status: 'ACTIVE' | 'STANDBY' | 'ROLLED_BACK' | 'FAILED';
  imageTag: string;
  deployedAt: string;
  promotedAt?: string;
  verifiedAt?: string;
  isKnownGood: boolean;
}

export interface HealthCheckLog {
  id: number;
  timestamp: string;
  targetUrl: string;
  environmentSlot: string;
  statusCode?: number;
  latencyMs?: number;
  status: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY' | 'UNREACHABLE';
  consecutiveFailures: number;
  errorDetail?: string;
}

export interface HealthMonitorStatus {
  currentStatus: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY' | 'UNREACHABLE' | 'UNKNOWN';
  consecutiveFailures: number;
  activeSlot: 'BLUE' | 'GREEN';
  lastSuccessfulCheck?: string;
  recentLogs: HealthCheckLog[];
}

export interface IncidentTimelineEvent {
  id: number;
  timestamp: string;
  eventType: string;
  stageName: string;
  status: 'INFO' | 'SUCCESS' | 'FAILURE' | 'WARNING';
  description: string;
  details?: string;
}

export interface Incident {
  id: number;
  incidentId: string;
  jenkinsBuild: string;
  failureCategory:
    | 'BUILD_FAILURE'
    | 'UNIT_TEST_FAILURE'
    | 'CONTAINER_STARTUP_FAILURE'
    | 'HEALTH_CHECK_FAILURE'
    | 'SELENIUM_REGRESSION_FAILURE'
    | 'DEPLOYMENT_FAILURE'
    | 'UNKNOWN_FAILURE'
    | string;
  failedStage: string;
  errorEvidence: string;
  candidateReleaseId: string;
  activeReleaseId: string;
  recoveryDecision:
    | 'CANDIDATE_REJECTED'
    | 'NO_DEPLOYMENT_REQUIRED'
    | 'EXECUTE_ROLLBACK'
    | 'MANUAL_INTERVENTION_REQUIRED'
    | 'RECOVERY_FAILED'
    | string;
  recoveryAction: string;
  actionStartedAt: string;
  actionCompletedAt?: string;
  recoveryDurationSeconds?: number;
  verificationResults?: string;
  finalStatus:
    | 'RECOVERED'
    | 'RECOVERY_FAILED'
    | 'CANDIDATE_REJECTED'
    | 'NO_DEPLOYMENT_REQUIRED'
    | 'MANUAL_INTERVENTION_REQUIRED'
    | 'RECOVERY_IN_PROGRESS';
  explanation: string;
  retryEligible: boolean;
  retryCount: number;
  timelineEvents: IncidentTimelineEvent[];
}

export interface ReleaseRecord {
  id: number;
  releaseId: string;
  versionTag: string;
  commitSha: string;
  imageTag: string;
  deploymentSlot: 'BLUE' | 'GREEN';
  deployedAt: string;
  promotedAt?: string;
  verifiedAt?: string;
  jenkinsBuildRef: string;
  healthCheckStatus: 'PASSED' | 'FAILED' | 'PENDING';
  seleniumSmokeStatus: 'PASSED' | 'FAILED' | 'SKIPPED' | 'PENDING';
  activationStatus: 'ACTIVE' | 'STANDBY' | 'REJECTED' | 'ROLLED_BACK';
  isKnownGood: boolean;
  isCurrentActive: boolean;
}

export interface VerificationCheck {
  checkName: string;
  targetEndpoint: string;
  expectedOutcome: string;
  actualOutcome: string;
  passed: boolean;
  diagnosticDetail?: string;
}

export interface VerificationAudit {
  targetVersion: string;
  targetSlot: string;
  overallPassed: boolean;
  summary: string;
  checks: VerificationCheck[];
}

export interface IntegrationInfo {
  key: string;
  displayName: string;
  status: 'CONNECTED' | 'DISCONNECTED' | 'DEGRADED';
  endpointUrl: string;
  details: string;
  lastChecked: string;
}

export interface PlatformMetrics {
  totalIncidents: number;
  totalReleases: number;
  activeSlot: string;
  inactiveSlot: string;
  activeVersion: string;
  successfulRecoveries: number;
  candidatesRejected: number;
  manualInterventions: number;
  recoverySuccessRateDisplay: string;
  avgRecoveryDurationDisplay: string;
  // Legacy / convenience fields
  totalPipelines?: number;
  totalDeployments?: number;
  currentHealthStatus?: string;
  recoverySampleSize?: number;
  recoveryRateDisplay?: string;
  avgRecoverySecondsDisplay?: string;
  verifiedRecoveryCount?: number;
}
