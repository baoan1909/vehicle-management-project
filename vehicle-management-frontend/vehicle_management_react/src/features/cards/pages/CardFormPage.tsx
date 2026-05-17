import { FormCard } from "../../../shared/components/form/FormCard";

export function CardFormPage() {
  return (
    <FormCard
      title="Thông tin chi tiết thẻ"
      breadcrumbs={[{ label: "Quản lý thẻ", href: "/admin/card" }, { label: "Thông tin thẻ" }]}
      cardTitle="Chỉnh sửa thông tin thẻ"
      backHref="/admin/card"
      fields={[
        { label: "Số thẻ:", name: "cardNumber", placeholder: "C002", value: "C001" },
        { label: "Loại thẻ:", name: "type", type: "select", options: ["Vãng lai", "Đăng ký"], value: "Đăng ký" },
        { label: "Loại xe:", name: "vehicleType", type: "select", options: ["Xe máy", "Xe hơi", "Xe khác"], value: "Xe máy" },
        { label: "Đã tạo thẻ vật lý", name: "isCreated", type: "checkbox", checked: true },
        { label: "Đã được sử dụng", name: "isUsed", type: "checkbox", checked: true },
      ]}
    />
  );
}

export function LostCardFormPage() {
  return (
    <FormCard
      title="Thông tin thẻ bị mất"
      breadcrumbs={[{ label: "Quản lý thẻ", href: "/admin/lost" }, { label: "Thông tin thẻ bị mất" }]}
      cardTitle="Xử lý báo mất thẻ"
      backHref="/admin/lost"
      fields={[
        { label: "Số thẻ:", name: "cardNumber", value: "V001" },
        { label: "Người báo mất:", name: "reporter", value: "Nguyễn Văn Khách" },
        { label: "Số điện thoại:", name: "phone", value: "0901999999" },
        { label: "Biển số:", name: "licensePlate", value: "59B1-67890" },
        { label: "Phí mất thẻ:", name: "fee", value: "100.000đ" },
        { label: "Ghi chú:", name: "note", type: "textarea", value: "Đã đối chiếu ảnh camera và thông tin khách hàng." },
      ]}
    />
  );
}
