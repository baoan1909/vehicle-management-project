import type { BreadcrumbItem } from "../../types/common";

interface PageHeaderProps {
  title: string;
  breadcrumbs?: BreadcrumbItem[];
}

export function PageHeader({ title, breadcrumbs = [] }: PageHeaderProps) {
  return (
    <div className="container-fluid">
      <div className="row mb-2">
        <div className="col-sm-6">
          <h1 className="m-0">{title}</h1>
        </div>
        {breadcrumbs.length > 0 && (
          <div className="col-sm-6">
            <ol className="breadcrumb float-sm-right">
              {breadcrumbs.map((item, index) => (
                <li className={`breadcrumb-item ${index === breadcrumbs.length - 1 ? "active" : ""}`} key={item.label}>
                  {item.href && index !== breadcrumbs.length - 1 ? <a href={item.href}>{item.label}</a> : item.label}
                </li>
              ))}
            </ol>
          </div>
        )}
      </div>
    </div>
  );
}
