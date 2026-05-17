import { Link } from "react-router-dom";
import { FilterControls } from "../../../shared/components/form/FilterControls";
import { ActionButtons } from "../../../shared/components/table/ActionButtons";
import { AdminTablePage } from "../../../shared/components/table/AdminTablePage";
import { cards, vehicleTypes } from "../../../shared/data/mockData";
import type { TableColumn } from "../../../shared/types/common";

type CardRow = (typeof cards)[number];

const columns: TableColumn<CardRow>[] = [
  { key: "stt", label: "STT", render: (_row, index) => index + 1 },
  { key: "id", label: "ID thẻ" },
  { key: "number", label: "Số thẻ" },
  { key: "type", label: "Loại thẻ" },
  { key: "vehicleType", label: "Loại xe" },
  { key: "isCreated", label: "Thẻ vật lý" },
  { key: "isUsed", label: "Trạng thái" },
  { key: "actions", label: "Chức năng", width: "100px", render: () => <ActionButtons editHref="/admin/card/form" /> },
];

export function CardListPage() {
  return (
    <AdminTablePage
      title="Quản lý thẻ"
      breadcrumbs={[{ label: "Quản lý thẻ", href: "/admin/card" }, { label: "Thẻ" }]}
      tableTitle="Bảng quản lý thông tin thẻ"
      columns={columns}
      rows={cards}
      filters={<FilterControls selects={[{ name: "vehicleTypeId", placeholder: "Tất cả loại xe", options: vehicleTypes }]} />}
      actions={
        <div className="form-group col-2 ml-auto mr-3">
          <Link to="/admin/card/form" className="btn btn-info btn-block">
            <i className="fas fa-plus-circle" /> Thêm mới
          </Link>
        </div>
      }
    />
  );
}
