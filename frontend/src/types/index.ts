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

export interface RecoveryEvent {
  id: number;
  failureCategory: string;
  evidenceDetails: string;
  selectedAction: string;
  executionOutcome: 'IN_PROGRESS' | 'SUCCESS' | 'FAILED';
  verificationResult: 'VERIFIED' | 'FAILED' | 'PENDING';
  previousWorkingVersion: string;
  failedCandidateVersion: string;
  restoredVersion: string;
  startedAt: string;
  completedAt?: string;
  durationSeconds?: number;
  operatorTriggered: boolean;
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
  totalPipelines: number;
  totalDeployments: number;
  activeSlot: string;
  activeVersion: string;
  currentHealthStatus: string;
  recoverySampleSize: number;
  recoveryRateDisplay: string;
  avgRecoverySecondsDisplay: string;
  verifiedRecoveryCount: number;
}
