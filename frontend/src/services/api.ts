import {
  PipelineRun,
  Deployment,
  HealthMonitorStatus,
  RecoveryEvent,
  IntegrationInfo,
  PlatformMetrics,
} from '../types';

const API_BASE = '/api';

export const api = {
  async getMetrics(): Promise<PlatformMetrics> {
    const res = await fetch(`${API_BASE}/metrics`);
    if (!res.ok) throw new Error('Failed to fetch metrics');
    return res.json();
  },

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

  async triggerPipeline(triggerData?: { triggerType?: string; commitSha?: string; branch?: string; message?: string }): Promise<PipelineRun> {
    const res = await fetch(`${API_BASE}/pipelines/trigger`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(triggerData || {}),
    });
    if (!res.ok) throw new Error('Failed to trigger pipeline');
    return res.json();
  },

  async getDeployments(): Promise<Deployment[]> {
    const res = await fetch(`${API_BASE}/deployments`);
    if (!res.ok) throw new Error('Failed to fetch deployments');
    return res.json();
  },

  async getActiveDeployment(): Promise<{ activeSlot: string; inactiveSlot: string; deployment: Deployment | null; lastKnownGood: Deployment | null }> {
    const res = await fetch(`${API_BASE}/deployments/active`);
    if (!res.ok) throw new Error('Failed to fetch active deployment');
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

  async switchSlot(targetSlot: string): Promise<{ success: boolean; activeSlot: string }> {
    const res = await fetch(`${API_BASE}/deployments/switch-slot?targetSlot=${targetSlot}`, {
      method: 'POST',
    });
    if (!res.ok) throw new Error('Slot switch failed');
    return res.json();
  },

  async getHealthStatus(): Promise<HealthMonitorStatus> {
    const res = await fetch(`${API_BASE}/health-monitor/status`);
    if (!res.ok) throw new Error('Failed to fetch health status');
    return res.json();
  },

  async getRecoveryEvents(): Promise<RecoveryEvent[]> {
    const res = await fetch(`${API_BASE}/recovery/events`);
    if (!res.ok) throw new Error('Failed to fetch recovery events');
    return res.json();
  },

  async getRecoveryById(id: number): Promise<RecoveryEvent> {
    const res = await fetch(`${API_BASE}/recovery/events/${id}`);
    if (!res.ok) throw new Error(`Failed to fetch recovery event ${id}`);
    return res.json();
  },

  async getIntegrations(): Promise<IntegrationInfo[]> {
    const res = await fetch(`${API_BASE}/integrations`);
    if (!res.ok) throw new Error('Failed to fetch integrations');
    return res.json();
  },
};
