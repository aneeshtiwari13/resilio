import React from 'react';
import {
  LayoutDashboard,
  GitBranch,
  Layers,
  ShieldCheck,
  Compass,
  Boxes,
  Flame,
} from 'lucide-react';

interface SidebarProps {
  currentTab: string;
  onTabChange: (tab: string) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ currentTab, onTabChange }) => {
  const navItems = [
    { id: 'overview', label: 'Overview', icon: LayoutDashboard },
    { id: 'incidents', label: 'Incident Center', icon: ShieldCheck },
    { id: 'releases', label: 'Release Manager', icon: Layers },
    { id: 'analytics', label: 'Recovery Analytics', icon: Compass },
    { id: 'pipelines', label: 'Pipelines & CI', icon: GitBranch },
  ];

  return (
    <aside className="w-64 bg-card border-r border-border flex flex-col justify-between shrink-0 h-screen sticky top-0">
      <div>
        {/* Brand Header */}
        <div className="h-16 flex items-center px-6 border-b border-border gap-3">
          <div className="w-9 h-9 rounded-lg bg-emerald flex items-center justify-center text-white font-bold text-lg shadow-sm">
            R
          </div>
          <div>
            <div className="font-bold text-charcoal text-base tracking-tight leading-none">
              RESILIO
            </div>
            <div className="text-[11px] text-muted font-medium mt-1 uppercase tracking-wider">
              Self-Healing CI/CD
            </div>
          </div>
        </div>

        {/* Navigation Links */}
        <nav className="p-3 space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            const active = currentTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => onTabChange(item.id)}
                className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-colors text-left ${
                  active
                    ? 'bg-canvas text-emerald font-semibold border border-border shadow-xs'
                    : 'text-muted hover:text-charcoal hover:bg-canvas/50'
                }`}
              >
                <Icon className={`w-4 h-4 ${active ? 'text-emerald' : 'text-muted'}`} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>
      </div>

      {/* Expo / Self-Healing Architecture Badge */}
      <div className="p-4 m-3 bg-canvas border border-border rounded-xl space-y-2">
        <div className="flex items-center gap-2 text-xs font-semibold text-charcoal">
          <Flame className="w-3.5 h-3.5 text-coral" />
          <span>Verified Self-Healing</span>
        </div>
        <p className="text-[11px] text-muted leading-relaxed">
          Detect. Recover. Verify. Zero unverified rollbacks. Zero blind restarts.
        </p>
      </div>
    </aside>
  );
};
