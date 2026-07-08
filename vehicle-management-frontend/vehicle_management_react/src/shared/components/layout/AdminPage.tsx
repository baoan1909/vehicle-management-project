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
      <section className="content tw-pb-8">
        <div className="container-fluid tw-max-w-[1480px]">{children}</div>
      </section>
    </div>
  );
}
