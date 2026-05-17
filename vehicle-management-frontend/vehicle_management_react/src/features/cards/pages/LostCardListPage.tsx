import { Link } from "react-router-dom";
import { FilterControls } from "../../../shared/components/form/FilterControls";
import { ActionButtons } from "../../../shared/components/table/ActionButtons";
import { AdminTablePage } from "../../../shared/components/table/AdminTablePage";
import { lostCards, vehicleTypes } from "../../../shared/data/mockData";
import type { TableColumn } from "../../../shared/types/common";

type LostCardRow = (typeof lostCards)[number];

const columns: TableColumn<LostCardRow>[] = [
  { key: "stt", label: "STT", render: (_row, index) => index + 1 },
  { key: "id", label: "ID báo mất" },
  { key: "cardNumber", label: "Số thẻ" },
  { key: "reporter", label: "Người báo mất" },
  { key: "phone", label: "Số điện thoại" },
  { key: "licensePlate", label: "Biển số" },
  { key: "notifiedAt", label: "Thời gian báo" },
  { key: "fee", label: "Phí mất thẻ" },
  { key: "status", label: "Trạng thái", render: (row) => <span className="badge bg-cyan">{row.status}</span> },
  { key: "actions", label: "Chức năng", width: "100px", render: () => <ActionButtons editHref="/admin/lost/form" /> },
];

export function LostCardListPage() {
  return (
    <AdminTablePage
      title="Quản lý thẻ bị mất"
      breadcrumbs={[{ label: "Quản lý thẻ", href: "/admin/card" }, { label: "Thẻ bị mất" }]}
      tableTitle="Bảng quản lý thông tin thẻ bị mất"
      columns={columns}
      rows={lostCards}
      filters={<FilterControls selects={[{ name: "vehicleTypeId", placeholder: "Tất cả loại xe", options: vehicleTypes }]} />}
      actions={
        <div className="form-group col-2 ml-auto mr-3">
          <Link to="/admin/lost/form" className="btn btn-info btn-block">
            <i className="fas fa-plus-circle" /> Thêm mới
          </Link>
        </div>
      }
    />
  );
}
