import { Link } from "react-router-dom";
import { FilterControls } from "../../../shared/components/form/FilterControls";
import { ActionButtons } from "../../../shared/components/table/ActionButtons";
import { AdminTablePage } from "../../../shared/components/table/AdminTablePage";
import { customers, ticketTypes, vehicleTypes } from "../../../shared/data/mockData";
import type { TableColumn } from "../../../shared/types/common";

type CustomerRow = (typeof customers)[number];

const columns: TableColumn<CustomerRow>[] = [
  { key: "stt", label: "STT", render: (_row, index) => index + 1 },
  { key: "cardNumber", label: "Số thẻ" },
  { key: "id", label: "ID khách hàng" },
  { key: "fullName", label: "Tên khách hàng" },
  { key: "phone", label: "Số điện thoại" },
  { key: "vehicleType", label: "Loại xe" },
  { key: "licensePlate", label: "Biển số" },
  { key: "ticketType", label: "Loại vé" },
  { key: "effectiveDate", label: "Ngày đăng ký" },
  { key: "expirationDate", label: "Ngày hết hạn" },
  { key: "actions", label: "Chức năng", width: "100px", render: () => <ActionButtons editHref="/admin/customer/form" /> },
];

export function CustomerListPage() {
  return (
    <AdminTablePage
      title="Quản lý khách hàng"
      breadcrumbs={[{ label: "Quản lý", href: "/admin/dashboard" }, { label: "Khách hàng" }]}
      tableTitle="Bảng quản lý thông tin khách hàng"
      columns={columns}
      rows={customers}
      filters={
        <FilterControls
          selects={[
            { name: "vehicleTypeId", placeholder: "Tất cả loại xe", options: vehicleTypes },
            { name: "ticketTypeId", placeholder: "Tất cả loại vé", options: ticketTypes },
          ]}
        />
      }
      actions={
        <div className="form-group col-2 ml-auto mr-3">
          <Link to="/admin/customer/form" className="btn btn-info btn-block">
            <i className="fas fa-plus-circle" /> Thêm mới
          </Link>
        </div>
      }
    />
  );
}
