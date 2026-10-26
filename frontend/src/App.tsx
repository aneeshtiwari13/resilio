import React, { useState, useEffect, useCallback } from 'react';
import { Sidebar } from './components/Sidebar';
import { Header } from './components/Header';
import { OverviewView } from './views/OverviewView';
import { IncidentCenterView } from './views/IncidentCenterView';
import { ReleaseManagerView } from './views/ReleaseManagerView';
import { RecoveryAnalyticsView } from './views/RecoveryAnalyticsView';
import { PipelinesView } from './views/PipelinesView';
import { api } from './services/api';
import {
  PlatformMetrics,
  PipelineRun,
  Incident,
  ReleaseRecord,
  VerificationAudit,
} from './types';

export const App: React.FC = () => {
  const [currentTab, setCurrentTab] = useState<string>('overview');
  const [metrics, setMetrics] = useState<PlatformMetrics | null>(null);
  const [pipelines, setPipelines] = useState<PipelineRun[]>([]);
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [releases, setReleases] = useState<ReleaseRecord[]>([]);
  const [activeSlot, setActiveSlot] = useState<string>('BLUE');
  const [inactiveSlot, setInactiveSlot] = useState<string>('GREEN');
  const [healthStatus, setHealthStatus] = useState<string>('HEALTHY');
  const [jenkinsUrl, setJenkinsUrl] = useState<string>('http://localhost:8085');
  const [isActionLoading, setIsActionLoading] = useState<boolean>(false);

  const fetchAllData = useCallback(async () => {
    try {
      // 1. Metrics
      const m = await api.getMetrics().catch(() => null);
      if (m) {
        setMetrics(m);
        if (m.activeSlot) setActiveSlot(m.activeSlot);
        if (m.inactiveSlot) setInactiveSlot(m.inactiveSlot);
      }

      // 2. Incidents
      const inc = await api.getIncidents().catch(() => []);
      setIncidents(inc);

      // 3. Releases
      const rel = await api.getReleases().catch(() => []);
      setReleases(rel);

      // 4. Pipelines
      const p = await api.getPipelines().catch(() => []);
      setPipelines(p);

      // 5. Integrations / Jenkins URL
      const integrations = await api.getIntegrations().catch(() => []);
      const jenkins = integrations.find((i: any) => i.key === 'JENKINS');
      if (jenkins?.endpointUrl) setJenkinsUrl(jenkins.endpointUrl);

      // 6. Direct Health check
      try {
        const hRes = await fetch('http://localhost:8080/actuator/health');
        if (hRes.ok) {
          const hData = await hRes.json();
          setHealthStatus(hData.status === 'UP' ? 'HEALTHY' : 'UNHEALTHY');
        } else {
          setHealthStatus('DEGRADED');
        }
      } catch {
        setHealthStatus(m?.currentHealthStatus || 'HEALTHY');
      }
    } catch (err) {
      console.error('Error fetching dashboard state:', err);
    }
  }, []);

  useEffect(() => {
    fetchAllData();
    const interval = setInterval(fetchAllData, 5000);
    return () => clearInterval(interval);
  }, [fetchAllData]);

  // Action Handlers
  const handleRetryRecovery = async (incidentId: string, reason?: string) => {
    setIsActionLoading(true);
    try {
      await api.retryIncidentRecovery(incidentId, reason);
      await fetchAllData();
    } catch (err: any) {
      alert(err.message || 'Retry failed');
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleTriggerRollback = async (reason: string, evidence: string) => {
    setIsActionLoading(true);
    try {
      await api.triggerManualRollback(reason, evidence);
      await fetchAllData();
      setCurrentTab('incidents');
    } catch (err: any) {
      alert(err.message || 'Rollback execution failed');
    } finally {
      setIsActionLoading(false);
    }
  };

  const handlePromoteCandidate = async (data: {
    versionTag: string;
    commitSha: string;
    imageTag: string;
  }) => {
    setIsActionLoading(true);
    try {
      await api.promoteRelease(data);
      await fetchAllData();
    } catch (err: any) {
      alert(err.message || 'Candidate promotion failed');
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleVerifyRelease = async (releaseId: string): Promise<VerificationAudit> => {
    return api.verifyRelease(releaseId);
  };

  const handleTriggerPipeline = async (data?: {
    triggerType?: string;
    commitSha?: string;
    branch?: string;
    message?: string;
  }) => {
    setIsActionLoading(true);
    try {
      await api.triggerPipeline(data);
      await fetchAllData();
      setCurrentTab('pipelines');
    } catch (err: any) {
      alert(err.message || 'Failed to trigger pipeline');
    } finally {
      setIsActionLoading(false);
    }
  };

  const getTabTitle = () => {
    switch (currentTab) {
      case 'incidents':
        return { title: 'Incident & Self-Healing Center', subtitle: 'Automated recovery decisions, timeline audits, and verification telemetry' };
      case 'releases':
        return { title: 'Release & Traffic Manager', subtitle: 'Active Blue/Green upstream routing and 7-point independent verification' };
      case 'analytics':
        return { title: 'Recovery Analytics', subtitle: 'Decision Engine rules matrix, SRE metrics, and safety guarantees' };
      case 'pipelines':
        return { title: 'Pipelines & CI Execution', subtitle: 'Real-time Jenkins builds, unit tests, and candidate packaging' };
      default:
        return { title: 'Platform Overview', subtitle: 'Live telemetry across builds, deployments, and verified recovery' };
    }
  };

  const tabHeader = getTabTitle();

  return (
    <div className="min-h-screen bg-canvas text-charcoal flex font-sans">
      {/* Sidebar Navigation */}
      <Sidebar currentTab={currentTab} onTabChange={setCurrentTab} />

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0">
        <Header
          title={tabHeader.title}
          subtitle={tabHeader.subtitle}
          healthStatus={healthStatus}
          onTriggerPipeline={() => handleTriggerPipeline()}
          onEmergencyRollback={() =>
            handleTriggerRollback(
              'Operator Emergency Override',
              'Manual emergency rollback initiated from top header button'
            )
          }
        />

        <main className="flex-1 overflow-y-auto">
          {currentTab === 'overview' && (
            <OverviewView
              metrics={metrics}
              latestPipeline={pipelines.length > 0 ? pipelines[0] : null}
              recentRecoveries={incidents.map((i) => ({
                id: i.id,
                failureCategory: i.failureCategory,
                evidenceDetails: i.errorEvidence,
                selectedAction: i.recoveryDecision,
                executionOutcome: i.finalStatus === 'RECOVERED' ? 'SUCCESS' : 'FAILED',
                verificationResult: i.finalStatus === 'RECOVERED' ? 'VERIFIED' : 'FAILED',
                previousWorkingVersion: i.activeReleaseId,
                failedCandidateVersion: i.candidateReleaseId,
                restoredVersion: i.activeReleaseId,
                startedAt: i.actionStartedAt,
                completedAt: i.actionCompletedAt,
                durationSeconds: i.recoveryDurationSeconds,
                operatorTriggered: false,
              }))}
              onNavigateTab={setCurrentTab}
              onTriggerPipeline={() => handleTriggerPipeline()}
            />
          )}

          {currentTab === 'incidents' && (
            <IncidentCenterView
              incidents={incidents}
              onRetry={handleRetryRecovery}
              onTriggerRollback={handleTriggerRollback}
              onRefresh={fetchAllData}
              isActionLoading={isActionLoading}
            />
          )}

          {currentTab === 'releases' && (
            <ReleaseManagerView
              releases={releases}
              activeSlot={activeSlot}
              inactiveSlot={inactiveSlot}
              onPromote={handlePromoteCandidate}
              onVerify={handleVerifyRelease}
              onRefresh={fetchAllData}
              isActionLoading={isActionLoading}
            />
          )}

          {currentTab === 'analytics' && (
            <RecoveryAnalyticsView metrics={metrics} incidents={incidents} />
          )}

          {currentTab === 'pipelines' && (
            <PipelinesView
              pipelines={pipelines}
              onTriggerPipeline={handleTriggerPipeline}
              onRefresh={fetchAllData}
              isActionLoading={isActionLoading}
              jenkinsUrl={jenkinsUrl}
            />
          )}
        </main>
      </div>
    </div>
  );
};
export default App;
