import { Link } from "react-router-dom";
import type { BreadcrumbItem } from "../../types/common";

interface PageHeaderProps {
  title: string;
  breadcrumbs?: BreadcrumbItem[];
}

export function PageHeader({ title, breadcrumbs = [] }: PageHeaderProps) {
  return (
    <div className="container-fluid vm-page-header">
      <div className="row mb-2 align-items-center">
        <div className="col-sm-6">
          <h1 className="m-0 vm-page-title">{title}</h1>
        </div>
        {breadcrumbs.length > 0 && (
          <div className="col-sm-6">
            <ol className="breadcrumb float-sm-right">
              {breadcrumbs.map((item, index) => (
                <li className={`breadcrumb-item ${index === breadcrumbs.length - 1 ? "active" : ""}`} key={item.label}>
                  {item.href && index !== breadcrumbs.length - 1 ? <Link to={item.href}>{item.label}</Link> : item.label}
                </li>
              ))}
            </ol>
          </div>
        )}
      </div>
    </div>
  );
}
