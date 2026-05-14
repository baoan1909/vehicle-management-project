import { ActionButtons } from "../../../shared/components/table/ActionButtons";
import { AdminTablePage } from "../../../shared/components/table/AdminTablePage";
import { roles } from "../../../shared/data/mockData";
import type { TableColumn } from "../../../shared/types/common";

type RoleRow = (typeof roles)[number];

const columns: TableColumn<RoleRow>[] = [
  { key: "stt", label: "STT", render: (_row, index) => index + 1 },
  { key: "id", label: "ID vai trò" },
  { key: "code", label: "Mã vai trò" },
  { key: "name", label: "Tên vai trò" },
  { key: "description", label: "Mô tả" },
  { key: "status", label: "Trạng thái", render: (row) => <span className="badge bg-cyan">{row.status}</span> },
  { key: "actions", label: "Chức năng", width: "100px", render: () => <ActionButtons editHref="#/admin/role/form" /> },
];

export function RoleListPage() {
  return (
    <AdminTablePage
      title="Quản lý vai trò"
      breadcrumbs={[{ label: "Quản lý", href: "#/admin/dashboard" }, { label: "Vai trò" }]}
      tableTitle="Bảng quản lý thông tin vai trò"
      columns={columns}
      rows={roles}
      actions={<div className="form-group col-2 ml-auto mr-3 mt-3"><a href="#/admin/role/form" className="btn btn-info btn-block"><i className="fas fa-plus-circle" /> Thêm mới</a></div>}
    />
  );
}
