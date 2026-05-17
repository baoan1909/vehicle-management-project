import { FormCard } from "../../../shared/components/form/FormCard";

export function CustomerFormPage() {
  return (
    <FormCard
      title="Thông tin khách hàng"
      breadcrumbs={[{ label: "Quản lý khách hàng", href: "/admin/customer" }, { label: "Thông tin khách hàng" }]}
      cardTitle="Chỉnh sửa thông tin khách hàng"
      backHref="/admin/customer"
      fields={[
        { label: "Tên khách hàng:", name: "fullName", value: "Võ Văn Tú" },
        { label: "Số điện thoại:", name: "phone", value: "0901000003" },
        { label: "Biển số:", name: "licensePlate", value: "60K8-2301" },
        { label: "Loại xe:", name: "vehicleType", type: "select", options: ["Xe máy", "Xe hơi", "Xe khác"], value: "Xe máy" },
        { label: "Loại vé:", name: "ticketType", type: "select", options: ["Vé tháng", "Vé ngày", "VIP"], value: "Vé tháng" },
        { label: "Ngày đăng ký:", name: "effectiveDate", type: "date", value: "2026-05-01" },
        { label: "Ngày hết hạn:", name: "expirationDate", type: "date", value: "2026-05-31" },
      ]}
    />
  );
}

