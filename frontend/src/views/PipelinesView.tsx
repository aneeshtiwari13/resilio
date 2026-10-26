import React, { useState } from 'react';
import {
  GitBranch,
  Play,
  CheckCircle2,
  XCircle,
  Loader2,
  Circle,
  Terminal,
  ExternalLink,
  X,
  Search,
} from 'lucide-react';
import { PipelineRun, PipelineStage } from '../types';

interface PipelinesViewProps {
  pipelines: PipelineRun[];
  onTriggerPipeline: (data: { triggerType: string; commitSha: string; branch: string; message: string }) => Promise<void>;
  onRefresh: () => void;
  isActionLoading: boolean;
  jenkinsUrl: string;
}

export const PipelinesView: React.FC<PipelinesViewProps> = ({
  pipelines,
  onTriggerPipeline,
  onRefresh,
  isActionLoading,
  jenkinsUrl,
}) => {
  const [selectedRun, setSelectedRun] = useState<PipelineRun | null>(null);
  const [showTriggerModal, setShowTriggerModal] = useState(false);
  const [triggerType, setTriggerType] = useState('MANUAL_DISPATCH');
  const [commitSha, setCommitSha] = useState('a1b2c3d4');
  const [branch, setBranch] = useState('main');
  const [message, setMessage] = useState('feat: update checkout workflow');
  const [searchTerm, setSearchTerm] = useState('');

  const filteredPipelines = pipelines.filter(
    (p) =>
      p.buildNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.commitSha.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.commitMessage.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const renderStageIcon = (status: string) => {
    switch (status) {
      case 'PASSED':
        return <CheckCircle2 className="w-4 h-4 text-emerald" />;
      case 'FAILED':
        return <XCircle className="w-4 h-4 text-coral" />;
      case 'RUNNING':
        return <Loader2 className="w-4 h-4 text-violet animate-spin" />;
      default:
        return <Circle className="w-4 h-4 text-gray-300" />;
    }
  };

  return (
    <div className="p-8 space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-border">
        <div>
          <h2 className="text-xl font-bold text-charcoal">Pipelines & CI Execution</h2>
          <p className="text-xs text-muted mt-1">
            Real Jenkins builds, automated test results, containerization, and deployment telemetry
          </p>
        </div>
        <div className="flex items-center gap-3">
          <a
            href={jenkinsUrl}
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1.5 px-3 py-2 rounded-lg border border-border bg-card text-xs font-semibold text-charcoal hover:bg-canvas shadow-2xs transition-colors"
          >
            <GitBranch className="w-3.5 h-3.5 text-violet" />
            <span>Open Jenkins Server</span>
            <ExternalLink className="w-3 h-3 text-muted" />
          </a>
          <button
            onClick={() => setShowTriggerModal(true)}
            className="flex items-center gap-2 px-3.5 py-2 rounded-lg bg-emerald text-white text-xs font-semibold hover:bg-emerald/90 shadow-2xs transition-colors"
          >
            <Play className="w-3.5 h-3.5" />
            Trigger Pipeline
          </button>
          <button
            onClick={onRefresh}
            className="px-3.5 py-2 rounded-lg border border-border bg-card text-xs font-semibold text-charcoal hover:bg-canvas transition-colors"
          >
            Refresh
          </button>
        </div>
      </div>

      {/* Search Bar */}
      <div className="relative">
        <Search className="w-4 h-4 text-muted absolute left-3 top-2.5" />
        <input
          type="text"
          placeholder="Filter pipelines by build #, commit SHA, or message..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full pl-9 pr-4 py-2 bg-card border border-border rounded-lg text-xs text-charcoal placeholder:text-muted focus:outline-none focus:border-emerald"
        />
      </div>

      {/* Pipelines Table */}
      <div className="bg-card rounded-xl border border-border shadow-xs overflow-hidden">
        {filteredPipelines.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead>
                <tr className="border-b border-border text-muted uppercase tracking-wider text-[11px] bg-canvas/60">
                  <th className="py-3 px-4">Build</th>
                  <th className="py-3 px-4">Trigger</th>
                  <th className="py-3 px-4">Commit</th>
                  <th className="py-3 px-4">Message</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Started</th>
                  <th className="py-3 px-4">Duration</th>
                  <th className="py-3 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/60">
                {filteredPipelines.map((run) => (
                  <tr key={run.id} className="hover:bg-canvas/50 transition-colors">
                    <td className="py-3 px-4 font-bold text-charcoal">{run.buildNumber}</td>
                    <td className="py-3 px-4 text-muted">{run.triggerType}</td>
                    <td className="py-3 px-4 font-mono text-[11px] text-charcoal">
                      {run.commitSha}
                    </td>
                    <td className="py-3 px-4 text-charcoal font-medium truncate max-w-[200px]">
                      {run.commitMessage}
                    </td>
                    <td className="py-3 px-4">
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
                    <td className="py-3 px-4 text-muted">
                      {new Date(run.startTime).toLocaleTimeString()}
                    </td>
                    <td className="py-3 px-4 text-muted">
                      {run.durationSeconds ? `${run.durationSeconds}s` : '—'}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <button
                        onClick={() => setSelectedRun(run)}
                        className="px-2.5 py-1 rounded bg-canvas border border-border text-charcoal hover:border-emerald text-[11px] font-semibold transition-colors"
                      >
                        View Stages
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="py-16 text-center text-muted text-xs space-y-2">
            <Terminal className="w-8 h-8 mx-auto text-muted/60" />
            <div className="font-semibold text-charcoal">No pipeline runs recorded yet</div>
            <p className="max-w-md mx-auto text-muted">
              Click <strong>Trigger Pipeline</strong> or push commits to GitHub to start builds.
            </p>
          </div>
        )}
      </div>

      {/* Stages Modal */}
      {selectedRun && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-card rounded-2xl border border-border shadow-xl max-w-2xl w-full p-6 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-border">
              <div>
                <h3 className="text-base font-bold text-charcoal">
                  Build {selectedRun.buildNumber} Stage Details
                </h3>
                <p className="text-xs text-muted mt-0.5">
                  Commit: {selectedRun.commitSha} • {selectedRun.commitMessage}
                </p>
              </div>
              <button
                onClick={() => setSelectedRun(null)}
                className="text-muted hover:text-charcoal"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-3 max-h-[60vh] overflow-y-auto">
              {selectedRun.stages.map((stage: PipelineStage, idx: number) => (
                <div
                  key={stage.id || idx}
                  className="p-3 bg-canvas rounded-xl border border-border space-y-2"
                >
                  <div className="flex items-center justify-between text-xs">
                    <div className="flex items-center gap-2">
                      {renderStageIcon(stage.status)}
                      <span className="font-bold text-charcoal">{stage.stageName}</span>
                    </div>
                    <span
                      className={`text-[10px] font-bold uppercase tracking-wider ${
                        stage.status === 'PASSED'
                          ? 'text-emerald'
                          : stage.status === 'FAILED'
                          ? 'text-coral'
                          : 'text-violet'
                      }`}
                    >
                      {stage.status}
                    </span>
                  </div>

                  {stage.logExcerpt && (
                    <pre className="p-2 bg-card rounded font-mono text-[10px] text-muted border border-border overflow-x-auto whitespace-pre-wrap">
                      {stage.logExcerpt}
                    </pre>
                  )}
                </div>
              ))}
            </div>

            <div className="flex justify-end pt-3 border-t border-border">
              <button
                onClick={() => setSelectedRun(null)}
                className="px-4 py-1.5 rounded-lg bg-card border border-border text-xs font-semibold text-charcoal hover:bg-canvas"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Trigger Modal */}
      {showTriggerModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-card rounded-2xl border border-border shadow-xl max-w-md w-full p-6 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-border">
              <h3 className="text-base font-bold text-charcoal flex items-center gap-2">
                <Play className="w-4 h-4 text-emerald" />
                Trigger Pipeline Dispatch
              </h3>
              <button
                onClick={() => setShowTriggerModal(false)}
                className="text-muted hover:text-charcoal"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-3">
              <div>
                <label className="text-xs font-semibold text-charcoal block mb-1">
                  Trigger Type
                </label>
                <select
                  value={triggerType}
                  onChange={(e) => setTriggerType(e.target.value)}
                  className="w-full p-2 bg-canvas border border-border rounded-lg text-xs text-charcoal"
                >
                  <option value="MANUAL_DISPATCH">MANUAL_DISPATCH</option>
                  <option value="GITHUB_WEBHOOK">GITHUB_WEBHOOK</option>
                  <option value="CHAOS_TEST_INJECTION">CHAOS_TEST_INJECTION</option>
                </select>
              </div>

              <div>
                <label className="text-xs font-semibold text-charcoal block mb-1">Branch</label>
                <input
                  type="text"
                  value={branch}
                  onChange={(e) => setBranch(e.target.value)}
                  className="w-full p-2 bg-canvas border border-border rounded-lg text-xs text-charcoal"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-charcoal block mb-1">
                  Commit SHA
                </label>
                <input
                  type="text"
                  value={commitSha}
                  onChange={(e) => setCommitSha(e.target.value)}
                  className="w-full p-2 bg-canvas border border-border rounded-lg text-xs text-charcoal font-mono"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-charcoal block mb-1">
                  Commit Message
                </label>
                <input
                  type="text"
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  className="w-full p-2 bg-canvas border border-border rounded-lg text-xs text-charcoal"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-border">
              <button
                onClick={() => setShowTriggerModal(false)}
                className="px-3 py-1.5 rounded-lg border border-border text-xs text-charcoal hover:bg-canvas"
              >
                Cancel
              </button>
              <button
                onClick={async () => {
                  await onTriggerPipeline({ triggerType, commitSha, branch, message });
                  setShowTriggerModal(false);
                }}
                disabled={isActionLoading}
                className="px-4 py-1.5 rounded-lg bg-emerald text-white text-xs font-semibold hover:bg-emerald/90 disabled:opacity-50"
              >
                Launch Build
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
