import React from 'react';
import { Play, RotateCcw, CheckCircle2, AlertTriangle, XCircle, HelpCircle } from 'lucide-react';

interface HeaderProps {
  title: string;
  subtitle: string;
  healthStatus: string;
  onTriggerPipeline: () => void;
  onEmergencyRollback: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  title,
  subtitle,
  healthStatus,
  onTriggerPipeline,
  onEmergencyRollback,
}) => {
  const getHealthBadge = () => {
    switch (healthStatus) {
      case 'HEALTHY':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-light text-emerald border border-emerald/20">
            <CheckCircle2 className="w-3.5 h-3.5" />
            System Healthy
          </span>
        );
      case 'DEGRADED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
            <AlertTriangle className="w-3.5 h-3.5 text-amber-600" />
            Performance Degraded
          </span>
        );
      case 'UNHEALTHY':
      case 'UNREACHABLE':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-coral-light text-coral border border-coral/20">
            <XCircle className="w-3.5 h-3.5" />
            Application Failure Detected
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-gray-100 text-muted border border-border">
            <HelpCircle className="w-3.5 h-3.5 text-muted" />
            Status Initializing
          </span>
        );
    }
  };

  return (
    <header className="h-16 bg-card border-b border-border px-8 flex items-center justify-between sticky top-0 z-10">
      <div>
        <h1 className="text-lg font-bold text-charcoal tracking-tight">{title}</h1>
        <p className="text-xs text-muted">{subtitle}</p>
      </div>

      <div className="flex items-center gap-3">
        {getHealthBadge()}

        <button
          onClick={onEmergencyRollback}
          className="flex items-center gap-2 px-3.5 py-1.5 rounded-lg border border-coral/30 text-coral bg-card hover:bg-coral-light text-xs font-semibold transition-colors shadow-xs"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          Rollback
        </button>

        <button
          onClick={onTriggerPipeline}
          className="flex items-center gap-2 px-4 py-1.5 rounded-lg bg-emerald hover:bg-emerald-hover text-white text-xs font-semibold transition-colors shadow-xs"
        >
          <Play className="w-3.5 h-3.5 fill-current" />
          Trigger Pipeline
        </button>
      </div>
    </header>
  );
};
