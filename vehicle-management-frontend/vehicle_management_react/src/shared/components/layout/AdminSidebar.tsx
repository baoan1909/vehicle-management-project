import { useState } from "react";
import { adminNavigation } from "../../../config/navigation";
import { isActivePath } from "../../utils/format";

interface AdminSidebarProps {
  currentPath: string;
}

export function AdminSidebar({ currentPath }: AdminSidebarProps) {
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({});

  return (
    <aside className="main-sidebar sidebar-light-cyan elevation-4 vm-sidebar tw-border-r tw-border-slate-200 tw-bg-white">
      <a href="#/admin/dashboard" className="brand-link vm-brand">
        <img src="/assets/admin/dist/img/AdminLTELogo.png" alt="Logo" className="brand-image img-circle elevation-3" style={{ opacity: 0.8 }} />
        <span className="brand-text font-weight-light">Admin</span>
      </a>
      <div className="sidebar">
        <nav className="mt-2">
          <ul className="nav nav-pills nav-sidebar flex-column" role="menu">
            {adminNavigation.map((item, index) => {
              const active = isActivePath(currentPath, item.match);
              const hasChildren = Boolean(item.children?.length);
              const isOpen = hasChildren && (active || openGroups[item.label]);
              return (
                <li key={item.label} className={`nav-item ${index === 0 ? "mt-5" : ""} ${isOpen ? "menu-open" : ""}`}>
                  {index === 1 && <div className="nav-item sidebar-divider mb-2" />}
                  <a
                    href={item.href ?? "#"}
                    className={`nav-link vm-sidebar-link ${active ? "active" : ""}`}
                    onClick={(event) => {
                      if (!hasChildren) return;
                      event.preventDefault();
                      setOpenGroups((current) => ({ ...current, [item.label]: !current[item.label] }));
                    }}
                  >
                    <i className={`nav-icon ${item.icon}`} />
                    <p>{item.label}{hasChildren && <i className="fas fa-angle-left right" />}</p>
                  </a>
                  {hasChildren && (
                    <ul className="nav nav-treeview">
                      {item.children?.map((child) => (
                        <li className="nav-item" key={child.label}>
                          <a href={child.href} className={`nav-link vm-sidebar-link vm-sidebar-sublink ${isActivePath(currentPath, child.match) ? "active" : ""}`}>
                            <i className={`${child.icon} nav-icon`} />
                            <p>{child.label}</p>
                          </a>
                        </li>
                      ))}
                    </ul>
                  )}
                </li>
              );
            })}
          </ul>
        </nav>
      </div>
    </aside>
  );
}
