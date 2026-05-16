import type { ReactNode } from "react";
import type { BreadcrumbItem, TableColumn } from "../../types/common";
import { AdminPage } from "../layout/AdminPage";
import { DataTable } from "./DataTable";

interface AdminTablePageProps<T> {
  title: string;
  breadcrumbs: BreadcrumbItem[];
  tableTitle: string;
  columns: TableColumn<T>[];
  rows: T[];
  filters?: ReactNode;
  actions?: ReactNode;
}

export function AdminTablePage<T>({ title, breadcrumbs, tableTitle, columns, rows, filters, actions }: AdminTablePageProps<T>) {
  return (
    <AdminPage title={title} breadcrumbs={breadcrumbs}>
      {(filters || actions) && (
        <div className="row">
          <div className="col-12 mt-4">
            <div className="card shadow vm-filter-card tw-rounded-2xl tw-border tw-border-slate-100">
              {filters && <div className="card-body">{filters}</div>}
              {actions && <div className="row">{actions}</div>}
            </div>
          </div>
        </div>
      )}
      <div className="row">
        <div className="col-12">
          <div className="card vm-data-card tw-rounded-2xl tw-border tw-border-slate-100 tw-shadow-soft">
            <div className="card-header vm-card-header"><h3 className="card-title">{tableTitle}</h3></div>
            <div className="card-body vm-card-body"><DataTable columns={columns} rows={rows} /></div>
          </div>
        </div>
      </div>
    </AdminPage>
  );
}
