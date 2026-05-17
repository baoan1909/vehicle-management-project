import { Link } from "react-router-dom";
import { FilterControls } from "../../../shared/components/form/FilterControls";
import { ActionButtons } from "../../../shared/components/table/ActionButtons";
import { AdminTablePage } from "../../../shared/components/table/AdminTablePage";
import { accounts } from "../../../shared/data/mockData";
import type { TableColumn } from "../../../shared/types/common";

type AccountRow = (typeof accounts)[number];

const columns: TableColumn<AccountRow>[] = [
  { key: "stt", label: "STT", width: "50px", render: (_row, index) => index + 1 },
  { key: "id", label: "ID tài khoản" },
  { key: "username", label: "Tên tài khoản" },
  { key: "fullName", label: "Tên khách hàng" },
  { key: "email", label: "Email" },
  { key: "role", label: "Vai trò" },
  { key: "status", label: "Trạng thái", render: (row) => <span className="badge bg-cyan">{row.status}</span> },
  { key: "actions", label: "Chức năng", width: "100px", render: () => <ActionButtons editHref="/admin/account/form" /> },
];

export function AccountListPage() {
  return (
    <AdminTablePage
      title="Quản lý tài khoản"
      breadcrumbs={[{ label: "Quản lý", href: "/admin/dashboard" }, { label: "Tài khoản" }]}
      tableTitle="Bảng quản lý thông tin tài khoản"
      columns={columns}
      rows={accounts}
      filters={
        <FilterControls
          selects={[
            {
              name: "isActive",
              placeholder: "Tất cả",
              options: [
                { label: "Đang hoạt động", value: "1" },
                { label: "Đã bị khóa", value: "0" },
              ],
            },
          ]}
        />
      }
      actions={
        <div className="form-group col-2 ml-auto mr-3">
          <Link to="/admin/account/form" className="btn btn-info btn-block">
            <i className="fas fa-plus-circle" /> Thêm mới
          </Link>
        </div>
      }
    />
  );
}
