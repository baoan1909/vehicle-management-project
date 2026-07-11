import { useEffect, useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { adminNavigation } from "../../../config/navigation";
import type { AdminSidebarEntry, AdminSidebarIcon, AdminSidebarLeaf } from "../../types/common";
import { cn } from "@/lib/cn";

function isPathMatch(pathname: string, matches: string[]) {
  return matches.some((match) => pathname === match || pathname.startsWith(`${match}/`));
}

function getLeafMatchScore(item: AdminSidebarLeaf, pathname: string) {
  return item.matches.reduce((bestScore, match) => {
    if (pathname === match) {
      return Math.max(bestScore, match.length + 10000);
    }

    if (pathname.startsWith(`${match}/`)) {
      return Math.max(bestScore, match.length);
    }

    return bestScore;
  }, -1);
}

function SidebarIcon({ icon }: { icon: AdminSidebarIcon }) {
  const iconClassName = {
    dashboard: "fas fa-tachometer-alt",
    swipe: "fas fa-car-side",
    card: "fas fa-credit-card",
    catalog: "fas fa-ticket-alt",
    parking: "fas fa-parking",
    pricing: "fas fa-file-invoice-dollar",
    members: "fas fa-users",
    role: "fas fa-user-shield",
    settings: "fas fa-cog",
    support: "fas fa-headset",
  } satisfies Record<AdminSidebarIcon, string>;

  return <i className={cn("tw-text-[1rem]", iconClassName[icon])} aria-hidden="true" />;
}

function Chevron({ expanded }: { expanded: boolean }) {
  return (
    <i
      className="fas fa-chevron-down tw-text-[0.78rem] tw-transition-transform tw-duration-200"
      style={{ transform: expanded ? "rotate(0deg)" : "rotate(-90deg)" }}
      aria-hidden="true"
    />
  );
}

function isLeafActive(item: AdminSidebarLeaf, pathname: string) {
  return isPathMatch(pathname, item.matches);
}

function isEntryActive(entry: Extract<AdminSidebarEntry, { kind: "group" }>, pathname: string) {
  return entry.items.some((item) => isLeafActive(item, pathname));
}

function getActiveGroupLabel(pathname: string) {
  const activeGroup = adminNavigation.find((entry) => entry.kind === "group" && isEntryActive(entry, pathname));
  return activeGroup?.kind === "group" ? activeGroup.label : null;
}

function SidebarLeaf({ active: activeOverride, collapsed, item }: { active?: boolean; collapsed: boolean; item: AdminSidebarLeaf }) {
  const location = useLocation();
  const active = activeOverride ?? isLeafActive(item, location.pathname);

  return (
    <NavLink
      to={item.to}
      title={collapsed ? item.label : undefined}
      className={cn(
        "tw-flex tw-min-h-10 tw-items-center tw-gap-3 tw-rounded-vm-md tw-px-3 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-700 tw-transition hover:tw-bg-brand-50 hover:tw-text-vm-primary hover:tw-no-underline",
        collapsed ? "tw-ml-0 tw-justify-center" : "tw-ml-8",
        active ? "tw-bg-brand-50 tw-text-vm-primary" : "",
      )}
    >
      <span className={cn("tw-h-2 tw-w-2 tw-rounded-full", active ? "tw-bg-vm-primary" : "tw-bg-slate-300")} aria-hidden="true" />
      <span className={cn("tw-min-w-0 tw-truncate", collapsed ? "tw-sr-only" : "")}>{item.label}</span>
    </NavLink>
  );
}

function SidebarGroup({
  collapsed,
  entry,
  active,
  expanded,
  onToggle,
}: {
  collapsed: boolean;
  entry: Extract<AdminSidebarEntry, { kind: "group" }>;
  active: boolean;
  expanded: boolean;
  onToggle: (label: string) => void;
}) {
  const location = useLocation();
  const leafScores = entry.items.map((item) => getLeafMatchScore(item, location.pathname));
  const bestLeafScore = Math.max(...leafScores);

  return (
    <div className="tw-grid tw-gap-1">
      <button
        type="button"
        title={collapsed ? entry.label : undefined}
        onClick={() => onToggle(entry.label)}
        className={cn(
          "tw-flex tw-min-h-[48px] tw-w-full tw-items-center tw-justify-between tw-gap-3 tw-rounded-vm-sm tw-border-0 tw-px-3 tw-text-left tw-text-[0.96rem] tw-font-extrabold tw-transition",
          collapsed ? "tw-justify-center" : "",
          active ? "tw-bg-vm-primary tw-text-white" : "tw-bg-white tw-text-slate-900 hover:tw-bg-brand-50 hover:tw-text-vm-primary",
        )}
        aria-expanded={!collapsed && expanded}
      >
        <span className="tw-flex tw-min-w-0 tw-items-center tw-gap-3">
          <span className={cn("tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-sm", active ? "tw-bg-white/14 tw-text-white" : "tw-bg-brand-50 tw-text-vm-primary")}>
            <SidebarIcon icon={entry.icon} />
          </span>
          <span className={cn("tw-min-w-0 tw-truncate", collapsed ? "tw-sr-only" : "")}>{entry.label}</span>
        </span>
        {collapsed ? null : <Chevron expanded={expanded} />}
      </button>

      {!collapsed && expanded ? (
        <div className="tw-grid tw-gap-1 tw-border-0 tw-border-l tw-border-solid tw-border-brand-100 tw-pl-1">
          {entry.items.map((item) => (
            <SidebarLeaf
              key={`${entry.label}-${item.label}`}
              active={bestLeafScore >= 0 && getLeafMatchScore(item, location.pathname) === bestLeafScore}
              collapsed={collapsed}
              item={item}
            />
          ))}
        </div>
      ) : null}
    </div>
  );
}

interface AdminSidebarProps {
  collapsed: boolean;
  onCollapsedChange: (collapsed: boolean) => void;
}

export function AdminSidebar({ collapsed, onCollapsedChange }: AdminSidebarProps) {
  const location = useLocation();
  const [expandedGroupLabel, setExpandedGroupLabel] = useState<string | null>(() => getActiveGroupLabel(location.pathname));

  useEffect(() => {
    setExpandedGroupLabel(getActiveGroupLabel(location.pathname));
  }, [location.pathname]);

  const handleToggleGroup = (label: string) => {
    setExpandedGroupLabel((current) => (current === label ? null : label));
  };

  return (
    <aside
      className={cn(
        "tw-fixed tw-bottom-0 tw-left-0 tw-top-[72px] tw-z-[1040] tw-border-0 tw-border-r tw-border-solid tw-border-slate-200/95 tw-bg-white tw-shadow-[12px_0_28px_rgba(15,23,42,0.05)] tw-transition-[width] tw-duration-300 max-[992px]:tw-hidden",
        collapsed ? "tw-w-[84px]" : "tw-w-[248px]",
      )}
      aria-label="CoParking admin sidebar"
    >
      <div className={cn("tw-flex tw-h-full tw-flex-col", collapsed ? "tw-px-3" : "tw-px-4")}>
        <div className="tw-min-h-0 tw-flex-1 tw-overflow-y-auto tw-pb-4 tw-pt-7 [scrollbar-width:none] [&::-webkit-scrollbar]:tw-hidden">
        <nav className="tw-grid tw-gap-2" role="menu" aria-label="CoParking admin navigation">
          {adminNavigation.map((entry, index) => {
            if (entry.kind === "divider") {
              return <div key={`divider-${index}`} className={cn("tw-my-2 tw-h-px tw-bg-slate-100", collapsed ? "tw-mx-2" : "")} />;
            }

            if (entry.kind === "group") {
              return (
                <SidebarGroup
                  key={entry.label}
                  entry={entry}
                  active={isEntryActive(entry, location.pathname)}
                  expanded={expandedGroupLabel === entry.label}
                  collapsed={collapsed}
                  onToggle={handleToggleGroup}
                />
              );
            }

            const active = isPathMatch(location.pathname, entry.matches);

            return (
              <NavLink
                key={entry.label}
                to={entry.to}
                title={collapsed ? entry.label : undefined}
                className={cn(
                  "tw-flex tw-min-h-[48px] tw-items-center tw-gap-3 tw-rounded-vm-sm tw-px-3 tw-text-[0.96rem] tw-font-extrabold tw-transition hover:tw-bg-brand-50 hover:tw-text-vm-primary hover:tw-no-underline",
                  collapsed ? "tw-justify-center" : "",
                  active ? "tw-bg-vm-primary tw-text-white tw-shadow-[0_12px_24px_rgba(37,99,235,0.18)] hover:tw-translate-y-px hover:tw-bg-vm-primary-hover hover:tw-text-white" : "tw-bg-white tw-text-slate-900",
                )}
              >
                <span className={cn("tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-sm", active ? "tw-bg-white/15 tw-text-white" : "tw-bg-brand-50 tw-text-vm-primary")}>
                  <SidebarIcon icon={entry.icon} />
                </span>
                <span className={cn("tw-min-w-0 tw-truncate", collapsed ? "tw-sr-only" : "")}>{entry.label}</span>
              </NavLink>
            );
          })}
        </nav>
        </div>

        <div className="tw-border-0 tw-border-t tw-border-solid tw-border-slate-100 tw-py-3">
          <button
            type="button"
            className={cn(
              "tw-flex tw-min-h-11 tw-w-full tw-items-center tw-gap-3 tw-rounded-vm-sm tw-border-0 tw-bg-white tw-px-3 tw-text-[0.9rem] tw-font-extrabold tw-text-slate-900 tw-transition hover:tw-bg-brand-50 hover:tw-text-vm-primary",
              collapsed ? "tw-justify-center" : "",
            )}
            aria-label={collapsed ? "Mở rộng menu" : "Thu gọn menu"}
            title={collapsed ? "Mở rộng menu" : undefined}
            onClick={() => onCollapsedChange(!collapsed)}
          >
            <span className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-sm tw-bg-brand-50 tw-text-vm-primary">
              <i className={cn("fas tw-transition-transform tw-duration-300", collapsed ? "fa-angle-double-right" : "fa-angle-double-left")} aria-hidden="true" />
            </span>
            <span className={cn("tw-min-w-0 tw-truncate", collapsed ? "tw-sr-only" : "")}>Thu gọn</span>
          </button>
        </div>
      </div>
    </aside>
  );
}
