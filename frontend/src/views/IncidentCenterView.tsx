import React, { useState } from 'react';
import {
  ShieldAlert,
  Clock,
  RotateCcw,
  Search,
  Filter,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  FileText,
  Activity,
  Layers,
  Terminal,
  X,
  Send,
} from 'lucide-react';
import { Incident, IncidentTimelineEvent } from '../types';

interface IncidentCenterViewProps {
  incidents: Incident[];
  onRetry: (incidentId: string, reason?: string) => Promise<void>;
  onTriggerRollback: (reason: string, evidence: string) => Promise<void>;
  onRefresh: () => void;
  isActionLoading: boolean;
}

export const IncidentCenterView: React.FC<IncidentCenterViewProps> = ({
  incidents,
  onRetry,
  onTriggerRollback,
  onRefresh,
  isActionLoading,
}) => {
  const [selectedIncident, setSelectedIncident] = useState<Incident | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Manual Rollback Modal State
  const [showRollbackModal, setShowRollbackModal] = useState(false);
  const [rollbackReason, setRollbackReason] = useState('Production Latency Spike / Post-Activation Regression');
  const [rollbackEvidence, setRollbackEvidence] = useState('Selenium automated monitor reported 3 consecutive failed transactions');

  // Retry Modal State
  const [retryTargetIncident, setRetryTargetIncident] = useState<Incident | null>(null);
  const [retryReason, setRetryReason] = useState('Operator verified environment issue resolved');

  const filteredIncidents = incidents.filter((inc) => {
    const matchesSearch =
      inc.incidentId.toLowerCase().includes(searchTerm.toLowerCase()) ||
      inc.jenkinsBuild.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (inc.explanation && inc.explanation.toLowerCase().includes(searchTerm.toLowerCase())) ||
      inc.failedStage.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesCategory =
      categoryFilter === 'ALL' || inc.failureCategory === categoryFilter;

    const matchesStatus =
      statusFilter === 'ALL' || inc.finalStatus === statusFilter;

    return matchesSearch && matchesCategory && matchesStatus;
  });

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'RECOVERED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-light text-emerald border border-emerald/20">
            <CheckCircle2 className="w-3 h-3" />
            RECOVERED
          </span>
        );
      case 'CANDIDATE_REJECTED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
            <AlertTriangle className="w-3 h-3" />
            CANDIDATE REJECTED
          </span>
        );
      case 'MANUAL_INTERVENTION_REQUIRED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-coral-light text-coral border border-coral/20">
            <ShieldAlert className="w-3 h-3" />
            MANUAL INTERVENTION
          </span>
        );
      case 'RECOVERY_FAILED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-coral-light text-coral border border-coral/20">
            <XCircle className="w-3 h-3" />
            RECOVERY FAILED
          </span>
        );
      case 'NO_DEPLOYMENT_REQUIRED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-teal-light text-teal border border-teal/20">
            <CheckCircle2 className="w-3 h-3" />
            NO ACTION NEEDED
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-violet-light text-violet border border-violet/20">
            <Activity className="w-3 h-3 animate-spin" />
            {status}
          </span>
        );
    }
  };

  const getTimelineEventStatusColor = (status: string) => {
    switch (status) {
      case 'SUCCESS':
        return 'text-emerald border-emerald bg-emerald-light/30';
      case 'FAILURE':
        return 'text-coral border-coral bg-coral-light/30';
      case 'WARNING':
        return 'text-amber-600 border-amber-500 bg-amber-50';
      default:
        return 'text-violet border-violet bg-violet-light/30';
    }
  };

  return (
    <div className="p-8 space-y-6 max-w-7xl mx-auto">
      {/* Header and Quick Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-border">
        <div>
          <h2 className="text-xl font-bold text-charcoal">Incident & Self-Healing Center</h2>
          <p className="text-xs text-muted mt-1">
            Real failure classification, automated recovery decisions, and immutable audit timeline
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowRollbackModal(true)}
            className="flex items-center gap-2 px-3.5 py-2 rounded-lg bg-coral text-white text-xs font-semibold hover:bg-coral/90 shadow-2xs transition-colors"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            Trigger Manual Rollback
          </button>
          <button
            onClick={onRefresh}
            className="px-3.5 py-2 rounded-lg border border-border bg-card text-xs font-semibold text-charcoal hover:bg-canvas transition-colors"
          >
            Refresh
          </button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col md:flex-row gap-3">
        <div className="flex-1 relative">
          <Search className="w-4 h-4 text-muted absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Search by incident ID, build #, failed stage, or explanation..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-card border border-border rounded-lg text-xs text-charcoal placeholder:text-muted focus:outline-none focus:border-emerald"
          />
        </div>
        <div className="flex items-center gap-2">
          <Filter className="w-3.5 h-3.5 text-muted shrink-0" />
          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            className="bg-card border border-border rounded-lg text-xs py-2 px-2.5 text-charcoal focus:outline-none focus:border-emerald"
          >
            <option value="ALL">All Categories</option>
            <option value="BUILD_FAILURE">BUILD_FAILURE</option>
            <option value="UNIT_TEST_FAILURE">UNIT_TEST_FAILURE</option>
            <option value="CONTAINER_STARTUP_FAILURE">CONTAINER_STARTUP_FAILURE</option>
            <option value="HEALTH_CHECK_FAILURE">HEALTH_CHECK_FAILURE</option>
            <option value="SELENIUM_REGRESSION_FAILURE">SELENIUM_REGRESSION_FAILURE</option>
            <option value="DEPLOYMENT_FAILURE">DEPLOYMENT_FAILURE</option>
          </select>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-card border border-border rounded-lg text-xs py-2 px-2.5 text-charcoal focus:outline-none focus:border-emerald"
          >
            <option value="ALL">All Statuses</option>
            <option value="RECOVERED">RECOVERED</option>
            <option value="CANDIDATE_REJECTED">CANDIDATE_REJECTED</option>
            <option value="MANUAL_INTERVENTION_REQUIRED">MANUAL_INTERVENTION_REQUIRED</option>
            <option value="RECOVERY_FAILED">RECOVERY_FAILED</option>
            <option value="NO_DEPLOYMENT_REQUIRED">NO_DEPLOYMENT_REQUIRED</option>
          </select>
        </div>
      </div>

      {/* Incidents Table */}
      <div className="bg-card rounded-xl border border-border shadow-xs overflow-hidden">
        {filteredIncidents.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead>
                <tr className="border-b border-border text-muted uppercase tracking-wider text-[11px] bg-canvas/60">
                  <th className="py-3 px-4">Incident ID</th>
                  <th className="py-3 px-4">Build Ref</th>
                  <th className="py-3 px-4">Category</th>
                  <th className="py-3 px-4">Failed Stage</th>
                  <th className="py-3 px-4">Decision</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Recovery Time</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/60">
                {filteredIncidents.map((inc) => (
                  <tr key={inc.id} className="hover:bg-canvas/50 transition-colors">
                    <td className="py-3 px-4 font-mono font-bold text-charcoal">
                      {inc.incidentId}
                    </td>
                    <td className="py-3 px-4 text-muted font-medium">
                      {inc.jenkinsBuild}
                    </td>
                    <td className="py-3 px-4">
                      <span className="font-semibold text-charcoal">
                        {inc.failureCategory}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-muted truncate max-w-[150px]">
                      {inc.failedStage}
                    </td>
                    <td className="py-3 px-4 font-mono text-[11px] text-charcoal">
                      {inc.recoveryDecision}
                    </td>
                    <td className="py-3 px-4">
                      {getStatusBadge(inc.finalStatus)}
                    </td>
                    <td className="py-3 px-4 text-muted">
                      {inc.recoveryDurationSeconds != null
                        ? `${inc.recoveryDurationSeconds}s`
                        : inc.finalStatus === 'RECOVERED'
                        ? '1s'
                        : '—'}
                    </td>
                    <td className="py-3 px-4 text-right space-x-2">
                      <button
                        onClick={() => setSelectedIncident(inc)}
                        className="px-2.5 py-1 rounded bg-canvas border border-border text-charcoal hover:border-emerald text-[11px] font-semibold transition-colors"
                      >
                        Timeline
                      </button>
                      {inc.retryEligible && (
                        <button
                          onClick={() => setRetryTargetIncident(inc)}
                          disabled={isActionLoading}
                          className="px-2.5 py-1 rounded bg-emerald text-white hover:bg-emerald/90 text-[11px] font-semibold transition-colors disabled:opacity-50"
                        >
                          Retry
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="py-16 text-center text-muted text-xs space-y-2">
            <ShieldAlert className="w-8 h-8 mx-auto text-muted/60" />
            <div className="font-semibold text-charcoal">No incidents found</div>
            <p className="max-w-md mx-auto text-muted">
              Pipeline executions and self-healing events will populate here automatically.
            </p>
          </div>
        )}
      </div>

      {/* Timeline Modal */}
      {selectedIncident && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-card rounded-2xl border border-border shadow-xl max-w-3xl w-full max-h-[85vh] flex flex-col overflow-hidden">
            {/* Modal Header */}
            <div className="p-6 border-b border-border flex items-center justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-base font-bold text-charcoal">
                    {selectedIncident.incidentId} Audit Timeline
                  </h3>
                  {getStatusBadge(selectedIncident.finalStatus)}
                </div>
                <p className="text-xs text-muted mt-1">
                  Build {selectedIncident.jenkinsBuild} • Stage: {selectedIncident.failedStage} • Candidate: {selectedIncident.candidateReleaseId || 'N/A'}
                </p>
              </div>
              <button
                onClick={() => setSelectedIncident(null)}
                className="p-1 rounded-lg text-muted hover:text-charcoal transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-6 overflow-y-auto space-y-6 flex-1">
              {/* Explanation Summary Box */}
              <div className="p-4 rounded-xl bg-canvas border border-border space-y-2">
                <div className="text-xs font-bold text-charcoal flex items-center gap-2">
                  <FileText className="w-3.5 h-3.5 text-violet" />
                  Decision Rationale & Explanation
                </div>
                <p className="text-xs text-muted leading-relaxed">
                  {selectedIncident.explanation || 'No detailed explanation recorded.'}
                </p>
                {selectedIncident.errorEvidence && (
                  <div className="mt-3 pt-3 border-t border-border">
                    <span className="text-[11px] font-semibold text-charcoal">Evidence Extract:</span>
                    <pre className="mt-1 p-2 bg-card rounded font-mono text-[11px] text-charcoal/90 border border-border overflow-x-auto whitespace-pre-wrap">
                      {selectedIncident.errorEvidence}
                    </pre>
                  </div>
                )}
              </div>

              {/* Chronological Timeline */}
              <div>
                <h4 className="text-xs font-bold uppercase tracking-wider text-muted mb-4">
                  Chronological Event Stream
                </h4>
                {selectedIncident.timelineEvents && selectedIncident.timelineEvents.length > 0 ? (
                  <div className="space-y-4 relative before:absolute before:inset-0 before:left-3.5 before:w-0.5 before:bg-border">
                    {selectedIncident.timelineEvents.map((event: IncidentTimelineEvent, idx: number) => (
                      <div key={event.id || idx} className="relative flex items-start gap-4 pl-1">
                        <div
                          className={`w-6 h-6 rounded-full border-2 flex items-center justify-center shrink-0 z-10 ${getTimelineEventStatusColor(
                            event.status
                          )}`}
                        >
                          <div className="w-1.5 h-1.5 rounded-full bg-current" />
                        </div>
                        <div className="flex-1 bg-canvas p-3 rounded-xl border border-border">
                          <div className="flex items-center justify-between text-[11px]">
                            <span className="font-bold text-charcoal">{event.eventType}</span>
                            <span className="text-muted font-mono">
                              {new Date(event.timestamp).toLocaleTimeString()}
                            </span>
                          </div>
                          <div className="text-xs text-charcoal mt-1 font-medium">
                            {event.description}
                          </div>
                          {event.details && (
                            <pre className="mt-2 p-2 bg-card rounded font-mono text-[10px] text-muted border border-border overflow-x-auto">
                              {event.details}
                            </pre>
                          )}
                          <div className="text-[10px] text-muted mt-1.5">
                            Stage: <strong>{event.stageName}</strong>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-xs text-muted py-4 text-center">
                    No timeline events logged for this incident.
                  </div>
                )}
              </div>
            </div>

            {/* Modal Footer */}
            <div className="p-4 border-t border-border bg-canvas/40 flex justify-end">
              <button
                onClick={() => setSelectedIncident(null)}
                className="px-4 py-2 rounded-lg bg-card border border-border text-xs font-semibold text-charcoal hover:bg-canvas transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Manual Rollback Trigger Modal */}
      {showRollbackModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-card rounded-2xl border border-border shadow-xl max-w-lg w-full p-6 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-border">
              <h3 className="text-base font-bold text-charcoal flex items-center gap-2">
                <RotateCcw className="w-4 h-4 text-coral" />
                Trigger Verified Manual Rollback
              </h3>
              <button
                onClick={() => setShowRollbackModal(false)}
                className="text-muted hover:text-charcoal"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-muted leading-relaxed">
              Resilio will immediately restore the last known-good release, reconfigure the Nginx upstream proxy, and execute the 7-point verification suite before declaring recovery complete.
            </p>

            <div className="space-y-3">
              <div>
                <label className="text-xs font-semibold text-charcoal block mb-1">
                  Rollback Reason
                </label>
                <input
                  type="text"
                  value={rollbackReason}
                  onChange={(e) => setRollbackReason(e.target.value)}
                  className="w-full p-2 bg-canvas border border-border rounded-lg text-xs text-charcoal focus:outline-none focus:border-emerald"
                  placeholder="e.g. Memory leak detected after 100 requests"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-charcoal block mb-1">
                  Evidence / Diagnostic Details
                </label>
                <textarea
                  rows={3}
                  value={rollbackEvidence}
                  onChange={(e) => setRollbackEvidence(e.target.value)}
                  className="w-full p-2 bg-canvas border border-border rounded-lg text-xs text-charcoal focus:outline-none focus:border-emerald"
                  placeholder="e.g. SRE observed 500 status spikes on checkout API"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-border">
              <button
                onClick={() => setShowRollbackModal(false)}
                className="px-3 py-1.5 rounded-lg border border-border text-xs text-charcoal hover:bg-canvas"
              >
                Cancel
              </button>
              <button
                onClick={async () => {
                  await onTriggerRollback(rollbackReason, rollbackEvidence);
                  setShowRollbackModal(false);
                }}
                disabled={isActionLoading}
                className="px-4 py-1.5 rounded-lg bg-coral text-white text-xs font-semibold hover:bg-coral/90 disabled:opacity-50 flex items-center gap-1.5"
              >
                <Send className="w-3.5 h-3.5" />
                Execute Rollback
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Retry Modal */}
      {retryTargetIncident && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-card rounded-2xl border border-border shadow-xl max-w-md w-full p-6 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-border">
              <h3 className="text-base font-bold text-charcoal flex items-center gap-2">
                <RotateCcw className="w-4 h-4 text-emerald" />
                Retry Incident Recovery
              </h3>
              <button
                onClick={() => setRetryTargetIncident(null)}
                className="text-muted hover:text-charcoal"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-muted leading-relaxed">
              Re-executes the Decision Engine evaluation and safe recovery pipeline for{' '}
              <strong className="text-charcoal">{retryTargetIncident.incidentId}</strong>.
            </p>

            <div>
              <label className="text-xs font-semibold text-charcoal block mb-1">
                Reason for Retry
              </label>
              <input
                type="text"
                value={retryReason}
                onChange={(e) => setRetryReason(e.target.value)}
                className="w-full p-2 bg-canvas border border-border rounded-lg text-xs text-charcoal focus:outline-none focus:border-emerald"
              />
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-border">
              <button
                onClick={() => setRetryTargetIncident(null)}
                className="px-3 py-1.5 rounded-lg border border-border text-xs text-charcoal hover:bg-canvas"
              >
                Cancel
              </button>
              <button
                onClick={async () => {
                  await onRetry(retryTargetIncident.incidentId, retryReason);
                  setRetryTargetIncident(null);
                }}
                disabled={isActionLoading}
                className="px-4 py-1.5 rounded-lg bg-emerald text-white text-xs font-semibold hover:bg-emerald/90 disabled:opacity-50"
              >
                Confirm Retry
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
