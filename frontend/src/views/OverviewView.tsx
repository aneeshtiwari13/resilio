import React from 'react';
import {
  Layers,
  Activity,
  ShieldCheck,
  Clock,
  ArrowRight,
  CheckCircle2,
  XCircle,
  Loader2,
  Circle,
  AlertCircle,
} from 'lucide-react';
import { PlatformMetrics, PipelineRun, RecoveryEvent } from '../types';

interface OverviewViewProps {
  metrics: PlatformMetrics | null;
  latestPipeline: PipelineRun | null;
  recentRecoveries: RecoveryEvent[];
  onNavigateTab: (tab: string) => void;
  onTriggerPipeline: () => void;
}

export const OverviewView: React.FC<OverviewViewProps> = ({
  metrics,
  latestPipeline,
  recentRecoveries,
  onNavigateTab,
}) => {
  const renderStageIcon = (status: string) => {
    switch (status) {
      case 'PASSED':
        return <CheckCircle2 className="w-4 h-4 text-emerald" />;
      case 'FAILED':
        return <XCircle className="w-4 h-4 text-coral" />;
      case 'RUNNING':
        return <Loader2 className="w-4 h-4 text-violet animate-spin" />;
      case 'SKIPPED':
        return <Circle className="w-4 h-4 text-gray-300" />;
      default:
        return <Circle className="w-4 h-4 text-gray-300" />;
    }
  };

  return (
    <div className="p-8 space-y-8 max-w-7xl mx-auto">
      {/* Row 1: Prominent Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        {/* Active Deployment */}
        <div className="bg-card p-5 rounded-xl border border-border shadow-xs hover:border-emerald/40 transition-colors">
          <div className="flex items-center justify-between text-muted mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Active Workload</span>
            <Layers className="w-4 h-4 text-emerald" />
          </div>
          <div className="text-2xl font-bold text-charcoal">
            {metrics?.activeVersion || 'v1.0.0'}
          </div>
          <div className="flex items-center gap-2 mt-2">
            <span
              className={`inline-block w-2.5 h-2.5 rounded-full ${
                metrics?.activeSlot === 'BLUE' ? 'bg-blue-500' : 'bg-emerald'
              }`}
            />
            <span className="text-xs font-medium text-muted">
              Slot: <strong className="text-charcoal">{metrics?.activeSlot || 'BLUE'}</strong> (via Nginx :80)
            </span>
          </div>
        </div>

        {/* Application Health */}
        <div className="bg-card p-5 rounded-xl border border-border shadow-xs hover:border-emerald/40 transition-colors">
          <div className="flex items-center justify-between text-muted mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Health Status</span>
            <Activity className="w-4 h-4 text-teal" />
          </div>
          <div className="text-2xl font-bold text-charcoal">
            {metrics?.currentHealthStatus || 'INITIALIZING'}
          </div>
          <div className="text-xs text-muted mt-2">
            Real-time HTTP health probe via proxy
          </div>
        </div>

        {/* Recovery Rate */}
        <div className="bg-card p-5 rounded-xl border border-border shadow-xs hover:border-violet/40 transition-colors">
          <div className="flex items-center justify-between text-muted mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Recovery Verification</span>
            <ShieldCheck className="w-4 h-4 text-violet" />
          </div>
          <div className="text-2xl font-bold text-charcoal">
            {metrics?.recoveryRateDisplay || 'No data yet'}
          </div>
          <div className="text-xs text-muted mt-2">
            Sample size: <strong>{metrics?.recoverySampleSize ?? 0}</strong> recorded events
          </div>
        </div>

        {/* Mean Recovery Time */}
        <div className="bg-card p-5 rounded-xl border border-border shadow-xs hover:border-coral/40 transition-colors">
          <div className="flex items-center justify-between text-muted mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Avg Recovery Time</span>
            <Clock className="w-4 h-4 text-coral" />
          </div>
          <div className="text-2xl font-bold text-charcoal">
            {metrics?.avgRecoverySecondsDisplay || 'No data yet'}
          </div>
          <div className="text-xs text-muted mt-2">
            Detection + Rollback + Verification
          </div>
        </div>
      </div>

      {/* Row 2: Live Pipeline Stepper */}
      <div className="bg-card rounded-xl border border-border p-6 shadow-xs">
        <div className="flex items-center justify-between mb-6 pb-4 border-b border-border">
          <div>
            <h3 className="text-base font-bold text-charcoal">Pipeline Stage Execution</h3>
            <p className="text-xs text-muted mt-0.5">
              Current or latest execution stages from checkout to verified release
            </p>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-xs font-medium text-muted">
              Build: <strong className="text-charcoal">{latestPipeline?.buildNumber || '#1'}</strong>
            </span>
            <button
              onClick={() => onNavigateTab('pipelines')}
              className="text-xs text-emerald font-semibold flex items-center gap-1 hover:underline"
            >
              View Full Logs <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {latestPipeline ? (
          <div className="overflow-x-auto pb-2">
            <div className="flex items-center gap-3 min-w-[750px]">
              {latestPipeline.stages.map((stage, idx) => (
                <React.Fragment key={stage.id || idx}>
                  <div className="flex flex-col items-center flex-1 text-center group">
                    <div className="w-8 h-8 rounded-full bg-canvas border border-border flex items-center justify-center mb-2 shadow-2xs group-hover:border-emerald transition-colors">
                      {renderStageIcon(stage.status)}
                    </div>
                    <span className="text-xs font-semibold text-charcoal truncate max-w-[100px]">
                      {stage.stageName}
                    </span>
                    <span
                      className={`text-[10px] uppercase font-bold tracking-wider mt-1 ${
                        stage.status === 'PASSED'
                          ? 'text-emerald'
                          : stage.status === 'FAILED'
                          ? 'text-coral'
                          : stage.status === 'RUNNING'
                          ? 'text-violet'
                          : 'text-muted'
                      }`}
                    >
                      {stage.status}
                    </span>
                  </div>
                  {idx < latestPipeline.stages.length - 1 && (
                    <div className="w-6 h-0.5 bg-border -mt-6" />
                  )}
                </React.Fragment>
              ))}
            </div>
          </div>
        ) : (
          <div className="py-8 text-center text-muted text-sm">
            <AlertCircle className="w-6 h-6 mx-auto mb-2 text-muted" />
            No pipeline executions recorded yet. Click <strong>Trigger Pipeline</strong> above to start.
          </div>
        )}
      </div>

      {/* Row 3: Blue-Green State & Recent Recoveries */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Blue / Green Deployment Topology */}
        <div className="bg-card rounded-xl border border-border p-6 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-bold text-charcoal uppercase tracking-wider">
              Blue / Green Traffic Topology
            </h3>
            <button
              onClick={() => onNavigateTab('deployments')}
              className="text-xs text-emerald font-semibold hover:underline"
            >
              Manage Slots
            </button>
          </div>
          <div className="grid grid-cols-2 gap-4">
            {/* Blue Slot */}
            <div
              className={`p-4 rounded-xl border transition-all ${
                metrics?.activeSlot === 'BLUE'
                  ? 'border-blue-500 bg-blue-50/40'
                  : 'border-border bg-canvas/40 opacity-75'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-bold text-blue-700">BLUE SLOT</span>
                {metrics?.activeSlot === 'BLUE' && (
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-500 text-white">
                    LIVE TRAFFIC
                  </span>
                )}
              </div>
              <div className="text-sm font-semibold text-charcoal">Port: 8081</div>
              <div className="text-xs text-muted mt-1">Image: resilio/demo-app:1.0.0</div>
            </div>

            {/* Green Slot */}
            <div
              className={`p-4 rounded-xl border transition-all ${
                metrics?.activeSlot === 'GREEN'
                  ? 'border-emerald bg-emerald-light/40'
                  : 'border-border bg-canvas/40 opacity-75'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-bold text-emerald">GREEN SLOT</span>
                {metrics?.activeSlot === 'GREEN' && (
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald text-white">
                    LIVE TRAFFIC
                  </span>
                )}
              </div>
              <div className="text-sm font-semibold text-charcoal">Port: 8082</div>
              <div className="text-xs text-muted mt-1">Image: resilio/demo-app:1.1.0</div>
            </div>
          </div>
          <div className="mt-4 p-3 bg-canvas rounded-lg text-xs text-muted border border-border">
            Routing managed atomically by Nginx reverse proxy on port 80.
          </div>
        </div>

        {/* Recent Recovery Events */}
        <div className="bg-card rounded-xl border border-border p-6 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-bold text-charcoal uppercase tracking-wider">
              Recent Self-Healing Events
            </h3>
            <button
              onClick={() => onNavigateTab('recovery')}
              className="text-xs text-emerald font-semibold hover:underline"
            >
              Recovery Log
            </button>
          </div>
          {recentRecoveries && recentRecoveries.length > 0 ? (
            <div className="space-y-3">
              {recentRecoveries.slice(0, 3).map((event) => (
                <div
                  key={event.id}
                  className="p-3 bg-canvas rounded-lg border border-border flex items-center justify-between text-xs"
                >
                  <div>
                    <div className="font-semibold text-charcoal">
                      {event.failureCategory.replace(/_/g, ' ')}
                    </div>
                    <div className="text-muted text-[11px] mt-0.5">
                      Action: {event.selectedAction.replace(/_/g, ' ')}
                    </div>
                  </div>
                  <div className="text-right">
                    <span
                      className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold ${
                        event.verificationResult === 'VERIFIED'
                          ? 'bg-emerald-light text-emerald'
                          : 'bg-coral-light text-coral'
                      }`}
                    >
                      {event.verificationResult}
                    </span>
                    <div className="text-[10px] text-muted mt-0.5">
                      {event.durationSeconds ? `${event.durationSeconds}s` : 'N/A'}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="py-8 text-center text-muted text-sm">
              <ShieldCheck className="w-6 h-6 mx-auto mb-2 text-muted" />
              No failure recovery events triggered. System running baseline stable.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
