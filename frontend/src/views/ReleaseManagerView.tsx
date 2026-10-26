import React, { useState } from 'react';
import {
  Layers,
  CheckCircle2,
  XCircle,
  ShieldCheck,
  Star,
  ArrowRight,
  RefreshCw,
  GitCommit,
  ExternalLink,
  X,
  Play,
  Activity,
  AlertCircle,
} from 'lucide-react';
import { ReleaseRecord, VerificationAudit } from '../types';

interface ReleaseManagerViewProps {
  releases: ReleaseRecord[];
  activeSlot: string;
  inactiveSlot: string;
  onPromote: (data: { versionTag: string; commitSha: string; imageTag: string }) => Promise<void>;
  onVerify: (releaseId: string) => Promise<VerificationAudit>;
  onRefresh: () => void;
  isActionLoading: boolean;
}

export const ReleaseManagerView: React.FC<ReleaseManagerViewProps> = ({
  releases,
  activeSlot,
  inactiveSlot,
  onPromote,
  onVerify,
  onRefresh,
  isActionLoading,
}) => {
  const [selectedAudit, setSelectedAudit] = useState<VerificationAudit | null>(null);
  const [showPromoteModal, setShowPromoteModal] = useState(false);
  const [candidateVersion, setCandidateVersion] = useState('1.1.0');
  const [candidateCommitSha, setCandidateCommitSha] = useState('c4f810a9');
  const [candidateImageTag, setCandidateImageTag] = useState('resilio/demo-app:1.1.0');
  const [verifyingReleaseId, setVerifyingReleaseId] = useState<string | null>(null);

  const activeRelease = releases.find((r) => r.isCurrentActive);
  const latestKnownGood = releases.find((r) => r.isKnownGood);

  const handleRunVerify = async (releaseId: string) => {
    setVerifyingReleaseId(releaseId);
    try {
      const audit = await onVerify(releaseId);
      setSelectedAudit(audit);
    } catch (e) {
      alert('Verification request failed. Check server logs.');
    } finally {
      setVerifyingReleaseId(null);
    }
  };

  return (
    <div className="p-8 space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-border">
        <div>
          <h2 className="text-xl font-bold text-charcoal">Release & Blue-Green Traffic Manager</h2>
          <p className="text-xs text-muted mt-1">
            Zero-downtime routing, atomic Nginx upstream switching, and 7-point independent verification
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowPromoteModal(true)}
            className="flex items-center gap-2 px-3.5 py-2 rounded-lg bg-emerald text-white text-xs font-semibold hover:bg-emerald/90 shadow-2xs transition-colors"
          >
            <Play className="w-3.5 h-3.5" />
            Promote Candidate
          </button>
          <button
            onClick={onRefresh}
            className="px-3.5 py-2 rounded-lg border border-border bg-card text-xs font-semibold text-charcoal hover:bg-canvas transition-colors"
          >
            Refresh
          </button>
        </div>
      </div>

      {/* Blue / Green Slots Visualization */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Blue Slot Card */}
        <div
          className={`p-6 rounded-2xl border transition-all ${
            activeSlot === 'BLUE'
              ? 'border-blue-500 bg-blue-50/30 shadow-xs'
              : 'border-border bg-card opacity-80'
          }`}
        >
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-blue-600" />
              <span className="text-sm font-bold text-charcoal">BLUE SLOT</span>
            </div>
            {activeSlot === 'BLUE' ? (
              <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-blue-600 text-white shadow-xs">
                ACTIVE TRAFFIC (:80)
              </span>
            ) : (
              <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-canvas text-muted border border-border">
                STANDBY (:8081)
              </span>
            )}
          </div>

          <div className="space-y-2 mt-4 text-xs">
            <div className="flex justify-between py-1 border-b border-border/50">
              <span className="text-muted">Target Host / Port:</span>
              <span className="font-mono font-medium text-charcoal">127.0.0.1:8081</span>
            </div>
            <div className="flex justify-between py-1 border-b border-border/50">
              <span className="text-muted">Current Workload:</span>
              <span className="font-semibold text-charcoal">
                {activeSlot === 'BLUE' ? activeRelease?.versionTag || '1.0.0' : 'Standby container'}
              </span>
            </div>
            <div className="flex justify-between py-1">
              <span className="text-muted">Direct Health Endpoint:</span>
              <a
                href="http://localhost:8081/actuator/health"
                target="_blank"
                rel="noreferrer"
                className="text-emerald hover:underline flex items-center gap-1 font-mono text-[11px]"
              >
                :8081/actuator/health <ExternalLink className="w-3 h-3" />
              </a>
            </div>
          </div>
        </div>

        {/* Green Slot Card */}
        <div
          className={`p-6 rounded-2xl border transition-all ${
            activeSlot === 'GREEN'
              ? 'border-emerald bg-emerald-light/30 shadow-xs'
              : 'border-border bg-card opacity-80'
          }`}
        >
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-emerald" />
              <span className="text-sm font-bold text-charcoal">GREEN SLOT</span>
            </div>
            {activeSlot === 'GREEN' ? (
              <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald text-white shadow-xs">
                ACTIVE TRAFFIC (:80)
              </span>
            ) : (
              <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-canvas text-muted border border-border">
                STANDBY (:8082)
              </span>
            )}
          </div>

          <div className="space-y-2 mt-4 text-xs">
            <div className="flex justify-between py-1 border-b border-border/50">
              <span className="text-muted">Target Host / Port:</span>
              <span className="font-mono font-medium text-charcoal">127.0.0.1:8082</span>
            </div>
            <div className="flex justify-between py-1 border-b border-border/50">
              <span className="text-muted">Current Workload:</span>
              <span className="font-semibold text-charcoal">
                {activeSlot === 'GREEN' ? activeRelease?.versionTag || '1.1.0' : 'Standby container'}
              </span>
            </div>
            <div className="flex justify-between py-1">
              <span className="text-muted">Direct Health Endpoint:</span>
              <a
                href="http://localhost:8082/actuator/health"
                target="_blank"
                rel="noreferrer"
                className="text-emerald hover:underline flex items-center gap-1 font-mono text-[11px]"
              >
                :8082/actuator/health <ExternalLink className="w-3 h-3" />
              </a>
            </div>
          </div>
        </div>
      </div>

      {/* Upstream Proxy Architecture Banner */}
      <div className="p-4 rounded-xl bg-canvas border border-border flex items-center justify-between text-xs text-muted">
        <div className="flex items-center gap-3">
          <Layers className="w-4 h-4 text-violet" />
          <span>
            Nginx reverse proxy directs incoming public traffic on port <strong>80</strong> to{' '}
            <strong className="text-charcoal">{activeSlot}</strong> slot via atomic configuration swap.
          </span>
        </div>
        <div className="flex items-center gap-2 font-mono text-[11px] text-charcoal bg-card px-2.5 py-1 rounded border border-border">
          <span>Active: {activeSlot}</span>
          <span>•</span>
          <span>Standby: {inactiveSlot}</span>
        </div>
      </div>

      {/* Releases History Table */}
      <div className="bg-card rounded-xl border border-border shadow-xs overflow-hidden">
        <div className="p-4 border-b border-border flex items-center justify-between">
          <h3 className="text-sm font-bold text-charcoal uppercase tracking-wider">
            Release Registry
          </h3>
          <span className="text-xs text-muted">
            Total records: <strong>{releases.length}</strong>
          </span>
        </div>

        {releases.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead>
                <tr className="border-b border-border text-muted uppercase tracking-wider text-[11px] bg-canvas/60">
                  <th className="py-3 px-4">Release ID</th>
                  <th className="py-3 px-4">Version</th>
                  <th className="py-3 px-4">Slot</th>
                  <th className="py-3 px-4">Commit</th>
                  <th className="py-3 px-4">Activation</th>
                  <th className="py-3 px-4">Health Probe</th>
                  <th className="py-3 px-4">Selenium Smoke</th>
                  <th className="py-3 px-4">Known-Good</th>
                  <th className="py-3 px-4 text-right">Verification</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/60">
                {releases.map((rel) => (
                  <tr key={rel.id} className="hover:bg-canvas/50 transition-colors">
                    <td className="py-3 px-4 font-mono font-medium text-charcoal">
                      {rel.releaseId}
                    </td>
                    <td className="py-3 px-4 font-bold text-charcoal">
                      {rel.versionTag}
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold ${
                          rel.deploymentSlot === 'BLUE'
                            ? 'bg-blue-100 text-blue-800'
                            : 'bg-emerald-light text-emerald'
                        }`}
                      >
                        {rel.deploymentSlot}
                      </span>
                    </td>
                    <td className="py-3 px-4 font-mono text-[11px] text-muted">
                      {rel.commitSha}
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold ${
                          rel.activationStatus === 'ACTIVE'
                            ? 'bg-emerald text-white'
                            : rel.activationStatus === 'STANDBY'
                            ? 'bg-canvas text-muted border border-border'
                            : 'bg-coral-light text-coral'
                        }`}
                      >
                        {rel.activationStatus}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold ${
                          rel.healthCheckStatus === 'PASSED'
                            ? 'bg-emerald-light text-emerald'
                            : rel.healthCheckStatus === 'FAILED'
                            ? 'bg-coral-light text-coral'
                            : 'bg-gray-100 text-gray-600'
                        }`}
                      >
                        {rel.healthCheckStatus}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold ${
                          rel.seleniumSmokeStatus === 'PASSED'
                            ? 'bg-emerald-light text-emerald'
                            : rel.seleniumSmokeStatus === 'FAILED'
                            ? 'bg-coral-light text-coral'
                            : 'bg-gray-100 text-gray-600'
                        }`}
                      >
                        {rel.seleniumSmokeStatus}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      {rel.isKnownGood ? (
                        <span className="inline-flex items-center gap-1 text-emerald font-semibold text-[11px]">
                          <Star className="w-3.5 h-3.5 fill-current text-amber-500" />
                          Known-Good
                        </span>
                      ) : (
                        <span className="text-muted text-[11px]">—</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <button
                        onClick={() => handleRunVerify(rel.releaseId)}
                        disabled={verifyingReleaseId === rel.releaseId || isActionLoading}
                        className="px-2.5 py-1 rounded bg-canvas border border-border text-charcoal hover:border-violet text-[11px] font-semibold transition-colors disabled:opacity-50"
                      >
                        {verifyingReleaseId === rel.releaseId ? 'Testing...' : 'Verify'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="py-12 text-center text-muted text-xs">
            No release records logged yet.
          </div>
        )}
      </div>

      {/* 7-Point Verification Audit Modal */}
      {selectedAudit && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-card rounded-2xl border border-border shadow-xl max-w-2xl w-full p-6 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-border">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-emerald" />
                <h3 className="text-base font-bold text-charcoal">
                  7-Point Independent Verification Audit
                </h3>
              </div>
              <button
                onClick={() => setSelectedAudit(null)}
                className="text-muted hover:text-charcoal"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="flex items-center justify-between p-3 rounded-xl bg-canvas border border-border text-xs">
              <div>
                Target: <strong className="text-charcoal">{selectedAudit.targetVersion}</strong> (Slot: {selectedAudit.targetSlot})
              </div>
              <div>
                Outcome:{' '}
                <strong
                  className={selectedAudit.overallPassed ? 'text-emerald' : 'text-coral'}
                >
                  {selectedAudit.overallPassed ? 'ALL CHECKS PASSED' : 'CHECK FAILED'}
                </strong>
              </div>
            </div>

            <p className="text-xs text-muted">{selectedAudit.summary}</p>

            <div className="space-y-2 max-h-[50vh] overflow-y-auto">
              {selectedAudit.checks.map((c, i) => (
                <div
                  key={i}
                  className="p-3 bg-canvas rounded-xl border border-border flex items-start justify-between text-xs gap-3"
                >
                  <div className="space-y-0.5">
                    <div className="font-semibold text-charcoal flex items-center gap-1.5">
                      {c.passed ? (
                        <CheckCircle2 className="w-4 h-4 text-emerald shrink-0" />
                      ) : (
                        <XCircle className="w-4 h-4 text-coral shrink-0" />
                      )}
                      <span>{c.checkName}</span>
                    </div>
                    <div className="text-[11px] text-muted font-mono pl-5.5">
                      Endpoint: {c.targetEndpoint}
                    </div>
                    <div className="text-[11px] text-muted pl-5.5">
                      Expected: <strong>{c.expectedOutcome}</strong> • Actual: <strong>{c.actualOutcome}</strong>
                    </div>
                  </div>
                  <span
                    className={`px-2 py-0.5 rounded text-[10px] font-bold shrink-0 ${
                      c.passed ? 'bg-emerald-light text-emerald' : 'bg-coral-light text-coral'
                    }`}
                  >
                    {c.passed ? 'PASS' : 'FAIL'}
                  </span>
                </div>
              ))}
            </div>

            <div className="flex justify-end pt-3 border-t border-border">
              <button
                onClick={() => setSelectedAudit(null)}
                className="px-4 py-1.5 rounded-lg bg-card border border-border text-xs font-semibold text-charcoal hover:bg-canvas"
              >
                Close Audit
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Promote Candidate Modal */}
      {showPromoteModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-card rounded-2xl border border-border shadow-xl max-w-md w-full p-6 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-border">
              <h3 className="text-base font-bold text-charcoal flex items-center gap-2">
                <Play className="w-4 h-4 text-emerald" />
                Promote Release Candidate
              </h3>
              <button
                onClick={() => setShowPromoteModal(false)}
                className="text-muted hover:text-charcoal"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-muted leading-relaxed">
              Resilio will register the candidate, deploy into the inactive standby slot, run health & Selenium verification, switch the upstream router, and tag the release as known-good.
            </p>

            <div className="space-y-3">
              <div>
                <label className="text-xs font-semibold text-charcoal block mb-1">
                  Version Tag
                </label>
                <input
                  type="text"
                  value={candidateVersion}
                  onChange={(e) => setCandidateVersion(e.target.value)}
                  className="w-full p-2 bg-canvas border border-border rounded-lg text-xs text-charcoal focus:outline-none focus:border-emerald"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-charcoal block mb-1">
                  Commit SHA
                </label>
                <input
                  type="text"
                  value={candidateCommitSha}
                  onChange={(e) => setCandidateCommitSha(e.target.value)}
                  className="w-full p-2 bg-canvas border border-border rounded-lg text-xs text-charcoal focus:outline-none focus:border-emerald font-mono"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-charcoal block mb-1">
                  Docker Image Tag
                </label>
                <input
                  type="text"
                  value={candidateImageTag}
                  onChange={(e) => setCandidateImageTag(e.target.value)}
                  className="w-full p-2 bg-canvas border border-border rounded-lg text-xs text-charcoal focus:outline-none focus:border-emerald font-mono"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-border">
              <button
                onClick={() => setShowPromoteModal(false)}
                className="px-3 py-1.5 rounded-lg border border-border text-xs text-charcoal hover:bg-canvas"
              >
                Cancel
              </button>
              <button
                onClick={async () => {
                  await onPromote({
                    versionTag: candidateVersion,
                    commitSha: candidateCommitSha,
                    imageTag: candidateImageTag,
                  });
                  setShowPromoteModal(false);
                }}
                disabled={isActionLoading}
                className="px-4 py-1.5 rounded-lg bg-emerald text-white text-xs font-semibold hover:bg-emerald/90 disabled:opacity-50"
              >
                Promote to Live
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
