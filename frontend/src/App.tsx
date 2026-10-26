import React, { useState, useEffect } from 'react';
import {
  Activity,
  GitBranch,
  Layers,
  ExternalLink,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  RefreshCw,
  HelpCircle,
  Terminal,
  ShieldAlert,
} from 'lucide-react';

interface AppVersion {
  name?: string;
  version?: string;
  commitSha?: string;
  buildNumber?: string;
  environment?: string;
}

interface PipelineRunSummary {
  buildNumber: string;
  status: 'PASSED' | 'FAILED' | 'RUNNING' | 'QUEUED';
  commitSha: string;
  startTime: string;
  durationSeconds?: number;
}

export const App: React.FC = () => {
  const [healthStatus, setHealthStatus] = useState<'UP' | 'DOWN' | 'UNREACHABLE' | 'CHECKING'>('CHECKING');
  const [appVersion, setAppVersion] = useState<AppVersion | null>(null);
  const [jenkinsConnected, setJenkinsConnected] = useState<boolean | null>(null);
  const [jenkinsUrl, setJenkinsUrl] = useState<string>('http://localhost:8085');
  const [recentRuns, setRecentRuns] = useState<PipelineRunSummary[]>([]);
  const [lastRefreshed, setLastRefreshed] = useState<string>('');
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);

  const checkStatus = async () => {
    setIsRefreshing(true);
    setLastRefreshed(new Date().toLocaleTimeString());

    // 1. Probe Target Demo Application Actuator Health
    try {
      const res = await fetch('http://localhost:8080/actuator/health');
      if (res.ok) {
        const data = await res.json();
        setHealthStatus(data.status === 'UP' ? 'UP' : 'DOWN');
      } else {
        setHealthStatus('DOWN');
      }
    } catch (e) {
      // Fallback try proxy or backend monitor
      try {
        const proxyRes = await fetch('/api/health-monitor/status');
        if (proxyRes.ok) {
          const pData = await proxyRes.json();
          setHealthStatus(pData.currentStatus === 'HEALTHY' ? 'UP' : 'DOWN');
        } else {
          setHealthStatus('UNREACHABLE');
        }
      } catch {
        setHealthStatus('UNREACHABLE');
      }
    }

    // 2. Fetch Version Information
    try {
      const vRes = await fetch('http://localhost:8080/api/version');
      if (vRes.ok) {
        const vData = await vRes.json();
        setAppVersion(vData);
      } else {
        setAppVersion(null);
      }
    } catch {
      setAppVersion(null);
    }

    // 3. Probe Jenkins Integration Status
    try {
      const jRes = await fetch('/api/integrations');
      if (jRes.ok) {
        const integrations = await jRes.json();
        const jenkins = integrations.find((i: any) => i.key === 'JENKINS');
        setJenkinsConnected(jenkins?.status === 'CONNECTED');
        if (jenkins?.endpointUrl) setJenkinsUrl(jenkins.endpointUrl);
      } else {
        setJenkinsConnected(false);
      }
    } catch {
      setJenkinsConnected(false);
    }

    // 4. Fetch Real Pipeline Runs if available
    try {
      const pRes = await fetch('/api/pipelines');
      if (pRes.ok) {
        const pData = await pRes.json();
        setRecentRuns(pData);
      } else {
        setRecentRuns([]);
      }
    } catch {
      setRecentRuns([]);
    }

    setIsRefreshing(false);
  };

  useEffect(() => {
    checkStatus();
    const timer = setInterval(checkStatus, 5000);
    return () => clearInterval(timer);
  }, []);

  return (
    <div className="min-h-screen bg-canvas text-charcoal flex flex-col font-sans">
      {/* Top Navbar */}
      <header className="h-16 bg-card border-b border-border px-8 flex items-center justify-between sticky top-0 z-10">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-emerald flex items-center justify-center text-white font-bold text-base shadow-sm">
            R
          </div>
          <div>
            <div className="font-bold text-charcoal text-base tracking-tight leading-none">
              RESILIO
            </div>
            <div className="text-[11px] text-muted font-medium mt-0.5 tracking-wider uppercase">
              Phase 1 CI/CD Foundation
            </div>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2 text-xs text-muted">
            <span>Refreshed: {lastRefreshed || 'Just now'}</span>
            <button
              onClick={checkStatus}
              disabled={isRefreshing}
              className="p-1 hover:text-charcoal transition-colors disabled:opacity-50"
              title="Refresh telemetry"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-emerald' : ''}`} />
            </button>
          </div>

          <a
            href={jenkinsUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg border border-border text-charcoal hover:border-emerald/40 text-xs font-semibold bg-card transition-colors shadow-2xs"
          >
            <GitBranch className="w-3.5 h-3.5 text-violet" />
            <span>Open Jenkins</span>
            <ExternalLink className="w-3 h-3 text-muted" />
          </a>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-6xl w-full mx-auto p-8 space-y-8">
        {/* Banner Notice */}
        <div className="bg-card border border-border rounded-xl p-5 shadow-xs flex items-start gap-4">
          <div className="w-10 h-10 rounded-lg bg-emerald-light border border-emerald/20 flex items-center justify-center shrink-0">
            <Activity className="w-5 h-5 text-emerald" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-charcoal">Resilio DevOps Engine — Phase 1 Live Dashboard</h2>
            <p className="text-xs text-muted mt-1 leading-relaxed">
              Monitoring the build, test, containerization, and Selenium browser verification pipeline for{' '}
              <code className="text-charcoal font-semibold bg-canvas px-1.5 py-0.5 rounded border border-border">
                resilio-demo-app
              </code>
              . Self-healing decisions and automated rollbacks are deferred to Phase 2.
            </p>
          </div>
        </div>

        {/* 3 Prominent Status Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Card 1: Target Application Health */}
          <div className="bg-card p-6 rounded-xl border border-border shadow-xs hover:border-emerald/30 transition-colors">
            <div className="flex items-center justify-between text-muted mb-3">
              <span className="text-xs font-semibold uppercase tracking-wider">Application Health</span>
              <Activity className="w-4 h-4 text-teal" />
            </div>
            <div className="flex items-center gap-2">
              {healthStatus === 'UP' && (
                <>
                  <CheckCircle2 className="w-6 h-6 text-emerald" />
                  <span className="text-2xl font-bold text-charcoal">UP</span>
                </>
              )}
              {healthStatus === 'DOWN' && (
                <>
                  <XCircle className="w-6 h-6 text-coral" />
                  <span className="text-2xl font-bold text-charcoal">DOWN</span>
                </>
              )}
              {healthStatus === 'UNREACHABLE' && (
                <>
                  <AlertTriangle className="w-6 h-6 text-coral" />
                  <span className="text-2xl font-bold text-charcoal">Unreachable</span>
                </>
              )}
              {healthStatus === 'CHECKING' && (
                <>
                  <RefreshCw className="w-6 h-6 text-violet animate-spin" />
                  <span className="text-2xl font-bold text-charcoal">Checking...</span>
                </>
              )}
            </div>
            <p className="text-xs text-muted mt-3">
              Probe target: <code className="text-[11px] text-charcoal">GET /actuator/health</code>
            </p>
          </div>

          {/* Card 2: Deployed Version Metadata */}
          <div className="bg-card p-6 rounded-xl border border-border shadow-xs hover:border-violet/30 transition-colors">
            <div className="flex items-center justify-between text-muted mb-3">
              <span className="text-xs font-semibold uppercase tracking-wider">Deployed Version</span>
              <Layers className="w-4 h-4 text-violet" />
            </div>
            <div className="text-2xl font-bold text-charcoal">
              {appVersion?.version || (healthStatus === 'UP' ? '1.0.0' : 'Unavailable')}
            </div>
            <div className="text-xs text-muted mt-3 space-y-1">
              <div>
                Commit SHA:{' '}
                <strong className="font-mono text-charcoal">
                  {appVersion?.commitSha || 'N/A'}
                </strong>
              </div>
              <div>
                Build Number:{' '}
                <strong className="text-charcoal">
                  {appVersion?.buildNumber ? `#${appVersion.buildNumber}` : 'N/A'}
                </strong>
              </div>
            </div>
          </div>

          {/* Card 3: Jenkins Integration Status */}
          <div className="bg-card p-6 rounded-xl border border-border shadow-xs hover:border-emerald/30 transition-colors">
            <div className="flex items-center justify-between text-muted mb-3">
              <span className="text-xs font-semibold uppercase tracking-wider">Jenkins CI Status</span>
              <GitBranch className="w-4 h-4 text-emerald" />
            </div>
            <div className="flex items-center gap-2">
              {jenkinsConnected === true && (
                <>
                  <CheckCircle2 className="w-6 h-6 text-emerald" />
                  <span className="text-2xl font-bold text-charcoal">Connected</span>
                </>
              )}
              {jenkinsConnected === false && (
                <>
                  <ShieldAlert className="w-6 h-6 text-amber-500" />
                  <span className="text-2xl font-bold text-charcoal">Disconnected</span>
                </>
              )}
              {jenkinsConnected === null && (
                <>
                  <HelpCircle className="w-6 h-6 text-muted" />
                  <span className="text-2xl font-bold text-charcoal">Checking...</span>
                </>
              )}
            </div>
            <p className="text-xs text-muted mt-3 truncate" title={jenkinsUrl}>
              Host: <span className="font-mono text-charcoal text-[11px]">{jenkinsUrl}</span>
            </p>
          </div>
        </div>

        {/* Jenkins Disconnected Guidance (Shown if Jenkins is unreachable) */}
        {jenkinsConnected === false && (
          <div className="bg-amber-50/60 border border-amber-200 rounded-xl p-5 text-xs text-amber-900 space-y-2">
            <div className="flex items-center gap-2 font-bold text-amber-800">
              <ShieldAlert className="w-4 h-4 text-amber-600" />
              <span>Jenkins Integration Unavailable on Port 8085</span>
            </div>
            <p className="leading-relaxed">
              Jenkins is currently offline or unreachable. To start the local Jenkins container, run:
            </p>
            <div className="bg-white/80 p-2.5 rounded font-mono border border-amber-200/80 text-[11px]">
              docker-compose up -d jenkins
            </div>
            <p>
              Once started, access Jenkins at <a href="http://localhost:8085" target="_blank" rel="noreferrer" className="underline font-bold">http://localhost:8085</a>, create a Pipeline project pointing to your repository's <code className="font-mono">Jenkinsfile</code>, and trigger builds.
            </p>
          </div>
        )}

        {/* Recent Pipeline Executions Table */}
        <div className="bg-card rounded-xl border border-border p-6 shadow-xs">
          <div className="flex items-center justify-between mb-4 pb-3 border-b border-border">
            <div>
              <h3 className="text-sm font-bold text-charcoal uppercase tracking-wider">
                Recent Pipeline Executions
              </h3>
              <p className="text-xs text-muted mt-0.5">
                Real pipeline telemetry received from Jenkins or Resilio trigger client
              </p>
            </div>
            <span className="text-xs text-muted font-medium">
              Recorded runs: <strong>{recentRuns.length}</strong>
            </span>
          </div>

          {recentRuns.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full text-xs text-left">
                <thead>
                  <tr className="border-b border-border text-muted uppercase tracking-wider text-[11px]">
                    <th className="py-2.5 px-3">Build</th>
                    <th className="py-2.5 px-3">Status</th>
                    <th className="py-2.5 px-3">Commit</th>
                    <th className="py-2.5 px-3">Started</th>
                    <th className="py-2.5 px-3 text-right">Duration</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border/60">
                  {recentRuns.map((run, i) => (
                    <tr key={i} className="hover:bg-canvas/50 transition-colors">
                      <td className="py-2.5 px-3 font-semibold text-charcoal">{run.buildNumber}</td>
                      <td className="py-2.5 px-3">
                        <span
                          className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold ${
                            run.status === 'PASSED'
                              ? 'bg-emerald-light text-emerald'
                              : run.status === 'FAILED'
                              ? 'bg-coral-light text-coral'
                              : 'bg-violet-light text-violet'
                          }`}
                        >
                          {run.status}
                        </span>
                      </td>
                      <td className="py-2.5 px-3 font-mono text-[11px] text-muted">{run.commitSha}</td>
                      <td className="py-2.5 px-3 text-muted">{new Date(run.startTime).toLocaleString()}</td>
                      <td className="py-2.5 px-3 text-right text-muted">
                        {run.durationSeconds ? `${run.durationSeconds}s` : 'In progress'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="py-12 text-center text-muted text-xs space-y-2">
              <Terminal className="w-8 h-8 mx-auto text-muted/60" />
              <div className="font-semibold text-charcoal">No pipeline executions recorded yet</div>
              <p className="max-w-md mx-auto text-muted">
                Pipeline execution logs will populate here as Jenkins executes the <code className="font-mono text-charcoal">Jenkinsfile</code> stages.
              </p>
            </div>
          )}
        </div>

        {/* Phase 1 Scope Clarification */}
        <div className="p-4 bg-canvas border border-border rounded-xl text-xs text-muted flex items-center justify-between">
          <span>
            <strong>Phase 1 Scope</strong>: Build, Test, Containerize, Selenium Verify, Deploy. Automatic rollback &amp; health failure decision loops are reserved for Phase 2.
          </span>
          <span className="text-[11px] font-mono text-charcoal bg-card px-2 py-1 rounded border border-border">
            v1.0.0-phase1
          </span>
        </div>
      </main>
    </div>
  );
};
