import React from 'react';
import {
  Activity,
  ShieldCheck,
  Clock,
  Layers,
  AlertTriangle,
  RotateCcw,
  CheckCircle2,
  Lock,
  Compass,
} from 'lucide-react';
import { PlatformMetrics, Incident } from '../types';

interface RecoveryAnalyticsViewProps {
  metrics: PlatformMetrics | null;
  incidents: Incident[];
}

export const RecoveryAnalyticsView: React.FC<RecoveryAnalyticsViewProps> = ({
  metrics,
  incidents,
}) => {
  // Category Breakdown Counts
  const categories = [
    'BUILD_FAILURE',
    'UNIT_TEST_FAILURE',
    'CONTAINER_STARTUP_FAILURE',
    'HEALTH_CHECK_FAILURE',
    'SELENIUM_REGRESSION_FAILURE',
    'DEPLOYMENT_FAILURE',
    'UNKNOWN_FAILURE',
  ];

  const categoryCounts = categories.map((cat) => ({
    name: cat,
    count: incidents.filter((i) => i.failureCategory === cat).length,
  }));

  const decisionRules = [
    {
      ruleNum: 1,
      name: 'Build Failure Gate',
      trigger: 'Maven compilation or packaging fails (exit != 0)',
      action: 'CANDIDATE_REJECTED',
      explanation: 'Pipeline halts immediately. Inactive slot is untouched. Live active release continues serving traffic.',
    },
    {
      ruleNum: 2,
      name: 'Unit Test Regression Gate',
      trigger: 'JUnit test assertion failure in surefire reports',
      action: 'CANDIDATE_REJECTED',
      explanation: 'Candidate container image is rejected. No deployment executed. Active release remains unchanged.',
    },
    {
      ruleNum: 3,
      name: 'Container Startup & Health Check Gate',
      trigger: 'Inactive slot container fails HTTP /actuator/health within timeout',
      action: 'CANDIDATE_REJECTED',
      explanation: 'Faulty container is quarantined. Live router configuration remains on active slot.',
    },
    {
      ruleNum: 4,
      name: 'Pre-Switch Selenium Regression Gate',
      trigger: 'Selenium browser tests fail against candidate port (:8082)',
      action: 'CANDIDATE_REJECTED',
      explanation: 'Production traffic is NOT switched. Candidate is quarantined. Live version untouched.',
    },
    {
      ruleNum: 5,
      name: 'Post-Activation Failure & Rollback',
      trigger: 'Post-activation health or functional checks fail on public route (:80)',
      action: 'EXECUTE_ROLLBACK',
      explanation: 'Immediately reconfigures Nginx upstream to last known-good release, verifies restored route, marks RECOVERED.',
    },
    {
      ruleNum: 6,
      name: 'Missing Known-Good Safe Quarantine',
      trigger: 'Candidate fails post-activation but no verified known-good tag exists in registry',
      action: 'MANUAL_INTERVENTION_REQUIRED',
      explanation: 'Quarantines candidate without guessing or blind restart. Escalates immediately to human operator.',
    },
    {
      ruleNum: 7,
      name: 'Post-Recovery Verification Failure',
      trigger: 'Rollback target fails 7-point post-recovery verification check suite',
      action: 'RECOVERY_FAILED',
      explanation: 'Rollback is marked RECOVERY_FAILED. Operator alert triggered. Prevents false-success claims.',
    },
  ];

  return (
    <div className="p-8 space-y-8 max-w-7xl mx-auto">
      {/* Header */}
      <div className="pb-4 border-b border-border">
        <h2 className="text-xl font-bold text-charcoal">Recovery Analytics & Decision Engine</h2>
        <p className="text-xs text-muted mt-1">
          Rigorous SRE metrics, deterministic rule-based decision logic, and zero-guesswork recovery telemetry
        </p>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="bg-card p-5 rounded-xl border border-border shadow-xs hover:border-violet/40 transition-colors">
          <div className="flex items-center justify-between text-muted mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Recovery Verification Rate</span>
            <ShieldCheck className="w-4 h-4 text-violet" />
          </div>
          <div className="text-2xl font-bold text-charcoal">
            {metrics?.recoverySuccessRateDisplay || 'No data yet'}
          </div>
          <p className="text-[11px] text-muted mt-2">
            Verified rollbacks passing all 7 post-recovery checks
          </p>
        </div>

        <div className="bg-card p-5 rounded-xl border border-border shadow-xs hover:border-coral/40 transition-colors">
          <div className="flex items-center justify-between text-muted mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Mean Recovery Time</span>
            <Clock className="w-4 h-4 text-coral" />
          </div>
          <div className="text-2xl font-bold text-charcoal">
            {metrics?.avgRecoveryDurationDisplay || 'No data yet'}
          </div>
          <p className="text-[11px] text-muted mt-2">
            Failure detection + atomic upstream swap + verification
          </p>
        </div>

        <div className="bg-card p-5 rounded-xl border border-border shadow-xs hover:border-emerald/40 transition-colors">
          <div className="flex items-center justify-between text-muted mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Candidates Rejected</span>
            <CheckCircle2 className="w-4 h-4 text-emerald" />
          </div>
          <div className="text-2xl font-bold text-charcoal">
            {metrics?.candidatesRejected ?? 0}
          </div>
          <p className="text-[11px] text-muted mt-2">
            Flawed releases safely blocked before touching live traffic
          </p>
        </div>

        <div className="bg-card p-5 rounded-xl border border-border shadow-xs hover:border-amber-500/40 transition-colors">
          <div className="flex items-center justify-between text-muted mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Manual Escalations</span>
            <AlertTriangle className="w-4 h-4 text-amber-500" />
          </div>
          <div className="text-2xl font-bold text-charcoal">
            {metrics?.manualInterventions ?? 0}
          </div>
          <p className="text-[11px] text-muted mt-2">
            No known-good release found; human intervention requested
          </p>
        </div>
      </div>

      {/* Row 2: Concurrency & Architectural Safety */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Concurrency & Safety Controls */}
        <div className="bg-card rounded-xl border border-border p-6 shadow-xs space-y-4">
          <div className="flex items-center gap-2">
            <Lock className="w-4 h-4 text-emerald" />
            <h3 className="text-sm font-bold text-charcoal uppercase tracking-wider">
              Self-Healing Safety Guarantees
            </h3>
          </div>
          <p className="text-xs text-muted leading-relaxed">
            Resilio enforces strict SRE invariants to prevent cascading failures or flapping:
          </p>
          <ul className="space-y-2 text-xs text-charcoal">
            <li className="flex items-start gap-2">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald shrink-0 mt-0.5" />
              <span>
                <strong>Atomic Concurrency Lock (<code className="text-emerald">recoveryLock</code>):</strong> Only one rollback or recovery operation can execute at any time; concurrent calls receive HTTP 400 Bad Request.
              </span>
            </li>
            <li className="flex items-start gap-2">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald shrink-0 mt-0.5" />
              <span>
                <strong>No Phantom Rollbacks:</strong> If a candidate fails before traffic activation, Resilio tags it <code className="text-charcoal font-semibold">CANDIDATE_REJECTED</code> without triggering an unnecessary rollback.
              </span>
            </li>
            <li className="flex items-start gap-2">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald shrink-0 mt-0.5" />
              <span>
                <strong>Never Delete Known-Good:</strong> Working releases are preserved in the database registry and on container storage.
              </span>
            </li>
            <li className="flex items-start gap-2">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald shrink-0 mt-0.5" />
              <span>
                <strong>Atomic Nginx Swap:</strong> Upstream configuration is written to a temporary file first and atomically renamed, avoiding partial-write proxy errors.
              </span>
            </li>
          </ul>
        </div>

        {/* Failure Category Distribution */}
        <div className="bg-card rounded-xl border border-border p-6 shadow-xs">
          <div className="flex items-center gap-2 mb-4">
            <Compass className="w-4 h-4 text-violet" />
            <h3 className="text-sm font-bold text-charcoal uppercase tracking-wider">
              Incident Category Distribution
            </h3>
          </div>
          <div className="space-y-2.5">
            {categoryCounts.map((cat) => {
              const total = incidents.length;
              const pct = total > 0 ? Math.round((cat.count / total) * 100) : 0;
              return (
                <div key={cat.name} className="space-y-1">
                  <div className="flex justify-between text-xs">
                    <span className="font-medium text-charcoal">{cat.name}</span>
                    <span className="text-muted font-mono">
                      {cat.count} ({pct}%)
                    </span>
                  </div>
                  <div className="h-2 bg-canvas rounded-full overflow-hidden border border-border/40">
                    <div
                      className="h-full bg-emerald rounded-full transition-all duration-300"
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Row 3: Decision Engine Rule Matrix */}
      <div className="bg-card rounded-xl border border-border p-6 shadow-xs">
        <div className="flex items-center justify-between mb-4 pb-3 border-b border-border">
          <div>
            <h3 className="text-base font-bold text-charcoal">
              Decision Engine Rule Matrix (Rules 1 – 7)
            </h3>
            <p className="text-xs text-muted mt-0.5">
              Deterministic mapping from failure signals and deployment context to recovery actions
            </p>
          </div>
        </div>

        <div className="space-y-3">
          {decisionRules.map((rule) => (
            <div
              key={rule.ruleNum}
              className="p-4 bg-canvas rounded-xl border border-border flex flex-col md:flex-row md:items-center justify-between gap-4 text-xs"
            >
              <div className="space-y-1 md:max-w-xl">
                <div className="flex items-center gap-2">
                  <span className="w-5 h-5 rounded-full bg-card border border-border flex items-center justify-center font-bold text-charcoal text-[11px]">
                    {rule.ruleNum}
                  </span>
                  <span className="font-bold text-charcoal">{rule.name}</span>
                </div>
                <div className="text-muted text-[11px]">
                  <strong>Trigger:</strong> {rule.trigger}
                </div>
                <p className="text-charcoal/80 leading-relaxed text-[11px]">
                  {rule.explanation}
                </p>
              </div>

              <div className="shrink-0">
                <span className="inline-block px-3 py-1 rounded-lg bg-card border border-border font-mono text-[11px] font-bold text-charcoal">
                  {rule.action}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
