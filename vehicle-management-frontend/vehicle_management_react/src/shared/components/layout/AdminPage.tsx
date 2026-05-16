import type { PropsWithChildren } from "react";
import type { BreadcrumbItem } from "../../types/common";
import { PageHeader } from "./PageHeader";

interface AdminPageProps extends PropsWithChildren {
  title: string;
  breadcrumbs: BreadcrumbItem[];
}

export function AdminPage({ title, breadcrumbs, children }: AdminPageProps) {
  return (
    <div className="content-header">
      <PageHeader title={title} breadcrumbs={breadcrumbs} />
      <section className="content vm-admin-content">
        <div className="container-fluid">{children}</div>
      </section>
    </div>
  );
}
