import type { PropsWithChildren } from "react";
import { PageHeader } from "./PageHeader";

interface ClientPageProps extends PropsWithChildren {
  title?: string;
}

export function ClientPage({ title, children }: ClientPageProps) {
  return (
    <div className="content-header">
      {title && <PageHeader title={title} />}
      <section className="content">
        <div className="container-fluid">{children}</div>
      </section>
    </div>
  );
}
