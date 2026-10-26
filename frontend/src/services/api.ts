import {
  PipelineRun,
  Deployment,
  HealthMonitorStatus,
  RecoveryEvent,
  IntegrationInfo,
  PlatformMetrics,
  Incident,
  IncidentTimelineEvent,
  ReleaseRecord,
  VerificationAudit,
} from '../types';

const API_BASE = '/api';

export const api = {
  // Platform Metrics
  async getMetrics(): Promise<PlatformMetrics> {
    const res = await fetch(`${API_BASE}/metrics`);
    if (!res.ok) throw new Error('Failed to fetch metrics');
    return res.json();
  },

  // Incident Management
  async getIncidents(): Promise<Incident[]> {
    const res = await fetch(`${API_BASE}/incidents`);
    if (!res.ok) throw new Error('Failed to fetch incidents');
    return res.json();
  },

  async getIncidentById(id: string): Promise<Incident> {
    const res = await fetch(`${API_BASE}/incidents/${id}`);
    if (!res.ok) throw new Error(`Failed to fetch incident ${id}`);
    return res.json();
  },

  async getIncidentTimeline(id: string): Promise<IncidentTimelineEvent[]> {
    const res = await fetch(`${API_BASE}/incidents/${id}/timeline`);
    if (!res.ok) throw new Error(`Failed to fetch timeline for incident ${id}`);
    return res.json();
  },

  async reportPipelineFailure(data: {
    stageName: string;
    exitCode?: number;
    rawLogs?: string;
    candidateVersion?: string;
    jenkinsBuild?: string;
    isPostActivation?: boolean;
  }): Promise<Incident> {
    const res = await fetch(`${API_BASE}/incidents/report-failure`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error('Failed to report pipeline failure');
    return res.json();
  },

  // Release Management
  async getReleases(): Promise<ReleaseRecord[]> {
    const res = await fetch(`${API_BASE}/releases`);
    if (!res.ok) throw new Error('Failed to fetch releases');
    return res.json();
  },

  async getActiveRelease(): Promise<{
    activeSlot: string;
    inactiveSlot: string;
    release: ReleaseRecord | null;
    lastKnownGood: ReleaseRecord | null;
  }> {
    const res = await fetch(`${API_BASE}/releases/active`);
    if (!res.ok) throw new Error('Failed to fetch active release');
    return res.json();
  },

  async getKnownGoodReleases(): Promise<ReleaseRecord[]> {
    const res = await fetch(`${API_BASE}/releases/known-good`);
    if (!res.ok) throw new Error('Failed to fetch known-good releases');
    return res.json();
  },

  async promoteRelease(data: {
    versionTag: string;
    commitSha: string;
    imageTag: string;
    jenkinsBuildRef?: string;
  }): Promise<any> {
    const res = await fetch(`${API_BASE}/releases/promote`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error('Failed to promote candidate release');
    return res.json();
  },

  async verifyRelease(releaseId: string): Promise<VerificationAudit> {
    const res = await fetch(`${API_BASE}/releases/${releaseId}/verify`, {
      method: 'POST',
    });
    if (!res.ok) throw new Error(`Failed to verify release ${releaseId}`);
    return res.json();
  },

  // Recovery Control
  async getRecoveryStatus(): Promise<{
    recoveryInProgress: boolean;
    activeSlot: string;
    inactiveSlot: string;
    activeRelease: ReleaseRecord | null;
    latestKnownGood: ReleaseRecord | null;
    rollbackAvailable: boolean;
  }> {
    const res = await fetch(`${API_BASE}/recovery/status`);
    if (!res.ok) throw new Error('Failed to fetch recovery status');
    return res.json();
  },

  async retryIncidentRecovery(
    incidentId: string,
    reason?: string,
    operator: string = 'dashboard-operator'
  ): Promise<any> {
    const res = await fetch(`${API_BASE}/recovery/${incidentId}/retry`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ reason: reason || 'Manual retry requested from UI', operator }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ error: 'Retry failed' }));
      throw new Error(err.error || 'Retry failed');
    }
    return res.json();
  },

  async triggerManualRollback(reason?: string, evidence?: string): Promise<Incident> {
    const res = await fetch(`${API_BASE}/recovery/trigger`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ reason, evidence }),
    });
    if (!res.ok) throw new Error('Manual rollback invocation failed');
    return res.json();
  },

  // Pipelines
  async getPipelines(): Promise<PipelineRun[]> {
    const res = await fetch(`${API_BASE}/pipelines`);
    if (!res.ok) throw new Error('Failed to fetch pipelines');
    return res.json();
  },

  async getPipelineById(id: number): Promise<PipelineRun> {
    const res = await fetch(`${API_BASE}/pipelines/${id}`);
    if (!res.ok) throw new Error(`Failed to fetch pipeline ${id}`);
    return res.json();
  },

  async triggerPipeline(triggerData?: {
    triggerType?: string;
    commitSha?: string;
    branch?: string;
    message?: string;
  }): Promise<PipelineRun> {
    const res = await fetch(`${API_BASE}/pipelines/trigger`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(triggerData || {}),
    });
    if (!res.ok) throw new Error('Failed to trigger pipeline');
    return res.json();
  },

  // Integrations & Legacy compatibility
  async getIntegrations(): Promise<IntegrationInfo[]> {
    const res = await fetch(`${API_BASE}/integrations`);
    if (!res.ok) throw new Error('Failed to fetch integrations');
    return res.json();
  },

  async getHealthStatus(): Promise<HealthMonitorStatus> {
    const res = await fetch(`${API_BASE}/health-monitor/status`);
    if (!res.ok) throw new Error('Failed to fetch health status');
    return res.json();
  },

  async getDeployments(): Promise<Deployment[]> {
    const res = await fetch(`${API_BASE}/deployments`);
    if (!res.ok) throw new Error('Failed to fetch deployments');
    return res.json();
  },

  async triggerRollback(reason?: string, evidence?: string): Promise<RecoveryEvent> {
    const res = await fetch(`${API_BASE}/deployments/rollback`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ reason, evidence }),
    });
    if (!res.ok) throw new Error('Rollback invocation failed');
    return res.json();
  },
};
