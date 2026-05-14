import { FilterControls } from "../../../shared/components/form/FilterControls";
import { ActionButtons } from "../../../shared/components/table/ActionButtons";
import { AdminTablePage } from "../../../shared/components/table/AdminTablePage";
import { customerParkingFees, tickets, vehicleTypes, vehicles, visitorParkingFees } from "../../../shared/data/mockData";
import type { TableColumn } from "../../../shared/types/common";

type TicketRow = (typeof tickets)[number];
type VehicleRow = (typeof vehicles)[number];
type VisitorFeeRow = (typeof visitorParkingFees)[number];
type CustomerFeeRow = (typeof customerParkingFees)[number];

const badge = (status: string) => <span className="badge bg-cyan">{status}</span>;
const addButton = (href: string) => <div className="form-group col-2 ml-auto mr-3 mt-3"><a href={href} className="btn btn-info btn-block"><i className="fas fa-plus-circle" /> Thêm mới</a></div>;

export function TicketListPage() {
  const columns: TableColumn<TicketRow>[] = [
    { key: "stt", label: "STT", render: (_row, index) => index + 1 },
    { key: "id", label: "ID vé" },
    { key: "name", label: "Tên vé" },
    { key: "duration", label: "Thời hạn" },
    { key: "description", label: "Mô tả" },
    { key: "status", label: "Trạng thái", render: (row) => badge(row.status) },
    { key: "actions", label: "Chức năng", width: "100px", render: () => <ActionButtons editHref="#/admin/ticket/form" /> },
  ];
  return <AdminTablePage title="Quản lý vé" breadcrumbs={[{ label: "Vé & Phương tiện", href: "#/admin/ticket" }, { label: "Vé" }]} tableTitle="Bảng quản lý thông tin vé" columns={columns} rows={tickets} actions={addButton("#/admin/ticket/form")} />;
}

export function VehicleListPage() {
  const columns: TableColumn<VehicleRow>[] = [
    { key: "stt", label: "STT", render: (_row, index) => index + 1 },
    { key: "id", label: "ID phương tiện" },
    { key: "code", label: "Mã loại xe" },
    { key: "name", label: "Tên loại xe" },
    { key: "description", label: "Mô tả" },
    { key: "status", label: "Trạng thái", render: (row) => badge(row.status) },
    { key: "actions", label: "Chức năng", width: "100px", render: () => <ActionButtons editHref="#/admin/vehicle/form" /> },
  ];
  return <AdminTablePage title="Quản lý phương tiện" breadcrumbs={[{ label: "Vé & Phương tiện", href: "#/admin/vehicle" }, { label: "Phương tiện" }]} tableTitle="Bảng quản lý thông tin phương tiện" columns={columns} rows={vehicles} actions={addButton("#/admin/vehicle/form")} />;
}

export function VisitorParkingFeePage() {
  const columns: TableColumn<VisitorFeeRow>[] = [
    { key: "stt", label: "STT", render: (_row, index) => index + 1 },
    { key: "id", label: "ID bảng giá" },
    { key: "vehicleType", label: "Loại xe" },
    { key: "timeRange", label: "Khung giờ" },
    { key: "price", label: "Phí dịch vụ" },
    { key: "lostCardFee", label: "Phí mất thẻ" },
    { key: "status", label: "Trạng thái", render: (row) => badge(row.status) },
    { key: "actions", label: "Chức năng", width: "100px", render: () => <ActionButtons editHref="#/admin/visitorParkingFee/form" /> },
  ];
  return <AdminTablePage title="Quản lý phí vãng lai" breadcrumbs={[{ label: "Bảng giá", href: "#/admin/visitorParkingFee" }, { label: "Phí vãng lai" }]} tableTitle="Bảng quản lý phí vãng lai" columns={columns} rows={visitorParkingFees} filters={<FilterControls selects={[{ name: "vehicleTypeId", placeholder: "Tất cả loại xe", options: vehicleTypes }]} />} actions={addButton("#/admin/visitorParkingFee/form")} />;
}

export function RegistrationFeePage() {
  const columns: TableColumn<CustomerFeeRow>[] = [
    { key: "stt", label: "STT", render: (_row, index) => index + 1 },
    { key: "id", label: "ID phí đăng ký" },
    { key: "vehicleType", label: "Loại xe" },
    { key: "ticketType", label: "Loại vé" },
    { key: "price", label: "Phí đăng ký" },
    { key: "duration", label: "Thời hạn" },
    { key: "status", label: "Trạng thái", render: (row) => badge(row.status) },
    { key: "actions", label: "Chức năng", width: "100px", render: () => <ActionButtons editHref="#/admin/parkingFeeOfCustomer/form" /> },
  ];
  return <AdminTablePage title="Quản lý phí đăng ký" breadcrumbs={[{ label: "Bảng giá", href: "#/admin/parkingFeeOfCustomer" }, { label: "Phí đăng ký" }]} tableTitle="Bảng quản lý phí đăng ký" columns={columns} rows={customerParkingFees} filters={<FilterControls selects={[{ name: "vehicleTypeId", placeholder: "Tất cả loại xe", options: vehicleTypes }]} />} actions={addButton("#/admin/parkingFeeOfCustomer/form")} />;
}
