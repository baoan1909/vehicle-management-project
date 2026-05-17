import { FormCard } from "../../../shared/components/form/FormCard";

export function TicketFormPage() {
  return (
    <FormCard
      title="Thông tin vé"
      breadcrumbs={[{ label: "Quản lý vé", href: "/admin/ticket" }, { label: "Thông tin vé" }]}
      cardTitle="Chỉnh sửa thông tin vé"
      backHref="/admin/ticket"
      fields={[
        { label: "Tên vé:", name: "name", value: "Vé tháng" },
        { label: "Thời hạn:", name: "duration", value: "30 ngày" },
        { label: "Mô tả:", name: "description", type: "textarea", value: "Dành cho khách đăng ký tháng." },
      ]}
    />
  );
}

export function VehicleFormPage() {
  return (
    <FormCard
      title="Thông tin phương tiện"
      breadcrumbs={[{ label: "Quản lý phương tiện", href: "/admin/vehicle" }, { label: "Thông tin phương tiện" }]}
      cardTitle="Chỉnh sửa thông tin phương tiện"
      backHref="/admin/vehicle"
      fields={[
        { label: "Mã loại xe:", name: "code", value: "MOTORBIKE" },
        { label: "Tên loại xe:", name: "name", value: "Xe máy" },
        { label: "Mô tả:", name: "description", type: "textarea", value: "Xe hai bánh động cơ." },
      ]}
    />
  );
}

export function VisitorFeeFormPage() {
  return (
    <FormCard
      title="Thông tin phí vãng lai"
      breadcrumbs={[{ label: "Phí vãng lai", href: "/admin/visitorParkingFee" }, { label: "Thông tin phí" }]}
      cardTitle="Chỉnh sửa phí vãng lai"
      backHref="/admin/visitorParkingFee"
      fields={[
        { label: "Loại xe:", name: "vehicleType", type: "select", options: ["Xe máy", "Xe hơi", "Xe khác"], value: "Xe máy" },
        { label: "Khung giờ:", name: "timeRange", value: "06:00 - 18:00" },
        { label: "Phí dịch vụ:", name: "price", value: "5.000đ" },
        { label: "Phí mất thẻ:", name: "lostCardFee", value: "100.000đ" },
      ]}
    />
  );
}

export function RegistrationFeeFormPage() {
  return (
    <FormCard
      title="Thông tin phí đăng ký"
      breadcrumbs={[{ label: "Phí đăng ký", href: "/admin/parkingFeeOfCustomer" }, { label: "Thông tin phí" }]}
      cardTitle="Chỉnh sửa phí đăng ký"
      backHref="/admin/parkingFeeOfCustomer"
      fields={[
        { label: "Loại xe:", name: "vehicleType", type: "select", options: ["Xe máy", "Xe hơi", "Xe khác"], value: "Xe máy" },
        { label: "Loại vé:", name: "ticketType", type: "select", options: ["Vé tháng", "VIP"], value: "Vé tháng" },
        { label: "Phí đăng ký:", name: "price", value: "140.000đ" },
        { label: "Thời hạn:", name: "duration", value: "30 ngày" },
      ]}
    />
  );
}
