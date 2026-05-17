import { useEffect, useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { adminNavigation } from "../../../config/navigation";
import type { AdminSidebarEntry, AdminSidebarIcon, AdminSidebarLeaf } from "../../types/common";

function isPathMatch(pathname: string, matches: string[]) {
  return matches.some((match) => pathname === match || pathname.startsWith(`${match}/`));
}

function SidebarIcon({ icon }: { icon: AdminSidebarIcon }) {
  const iconClassName = {
    dashboard: "fas fa-tachometer-alt",
    swipe: "fas fa-car-side",
    card: "fas fa-credit-card",
    catalog: "fas fa-ticket-alt",
    pricing: "fas fa-file-invoice-dollar",
    members: "fas fa-users",
    role: "fas fa-user-shield",
  } satisfies Record<AdminSidebarIcon, string>;

  return <i className={`vm-sidebar-icon ${iconClassName[icon]}`} aria-hidden="true" />;
}

function Chevron({ expanded }: { expanded: boolean }) {
  return <i className={`fas fa-angle-left vm-sidebar-chevron ${expanded ? "is-open" : ""}`} aria-hidden="true" />;
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

function SidebarLeaf({ item }: { item: AdminSidebarLeaf }) {
  const location = useLocation();
  const active = isLeafActive(item, location.pathname);

  return (
    <NavLink to={item.to} className={`vm-sidebar-link vm-sidebar-sublink ${active ? "active" : ""}`}>
      <span className="vm-sidebar-link-main">
        <span className="vm-sidebar-dot" aria-hidden="true" />
        <span className="vm-sidebar-link-label">{item.label}</span>
      </span>
    </NavLink>
  );
}

function SidebarGroup({
  entry,
  active,
  expanded,
  onToggle,
}: {
  entry: Extract<AdminSidebarEntry, { kind: "group" }>;
  active: boolean;
  expanded: boolean;
  onToggle: (label: string) => void;
}) {
  return (
    <div className="vm-sidebar-group">
      <button
        type="button"
        onClick={() => onToggle(entry.label)}
        className={`vm-sidebar-link vm-sidebar-group-trigger ${active ? "active" : ""}`}
      >
        <span className="vm-sidebar-link-main">
          <span className="vm-sidebar-icon-wrap">
            <SidebarIcon icon={entry.icon} />
          </span>
          <span className="vm-sidebar-link-label">{entry.label}</span>
        </span>
        <Chevron expanded={expanded} />
      </button>
      {expanded ? (
        <div className="vm-sidebar-group-panel">
          {entry.items.map((item) => (
            <SidebarLeaf key={`${entry.label}-${item.label}`} item={item} />
          ))}
        </div>
      ) : null}
    </div>
  );
}

export function AdminSidebar() {
  const location = useLocation();
  const [expandedGroupLabel, setExpandedGroupLabel] = useState<string | null>(() => getActiveGroupLabel(location.pathname));

  useEffect(() => {
    const activeGroupLabel = getActiveGroupLabel(location.pathname);
    setExpandedGroupLabel(activeGroupLabel);
  }, [location.pathname]);

  const handleToggleGroup = (label: string) => {
    setExpandedGroupLabel((current) => (current === label ? null : label));
  };

  return (
    <aside className="vm-sidebar" aria-label="CoParking admin sidebar">
      <div className="vm-sidebar-scroll subtle-scrollbar">
        <nav className="vm-sidebar-menu" role="menu" aria-label="CoParking admin navigation">
          {adminNavigation.map((entry, index) => {
            if (entry.kind === "divider") {
              return <div key={`divider-${index}`} className="vm-sidebar-divider" />;
            }

            if (entry.kind === "group") {
              return (
                <SidebarGroup
                  key={entry.label}
                  entry={entry}
                  active={isEntryActive(entry, location.pathname)}
                  expanded={expandedGroupLabel === entry.label}
                  onToggle={handleToggleGroup}
                />
              );
            }

            const active = isPathMatch(location.pathname, entry.matches);

            return (
              <NavLink key={entry.label} to={entry.to} className={`vm-sidebar-link ${active ? "active" : ""}`}>
                <span className="vm-sidebar-link-main">
                  <span className="vm-sidebar-icon-wrap">
                    <SidebarIcon icon={entry.icon} />
                  </span>
                  <span className="vm-sidebar-link-label">{entry.label}</span>
                </span>
              </NavLink>
            );
          })}
        </nav>
      </div>
    </aside>
  );
}
