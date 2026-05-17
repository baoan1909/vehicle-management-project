import { FormCard } from "../../../shared/components/form/FormCard";

export function AccountFormPage() {
  return (
    <FormCard
      title="Thông tin tài khoản"
      breadcrumbs={[{ label: "Quản lý tài khoản", href: "/admin/account" }, { label: "Thông tin tài khoản" }]}
      cardTitle="Chỉnh sửa thông tin tài khoản"
      backHref="/admin/account"
      fields={[
        { label: "Tên tài khoản:", name: "username", value: "vovantu" },
        { label: "Tên khách hàng:", name: "fullName", value: "Võ Văn Tú" },
        { label: "Email:", name: "email", type: "email", value: "tu.customer@example.com" },
        { label: "Vai trò:", name: "role", type: "select", options: ["Quản trị viên", "Nhân viên", "Khách hàng"], value: "Khách hàng" },
        { label: "Đang hoạt động", name: "isActive", type: "checkbox", checked: true },
      ]}
    />
  );
}

export function RoleFormPage() {
  return (
    <FormCard
      title="Thông tin vai trò"
      breadcrumbs={[{ label: "Quản lý vai trò", href: "/admin/role" }, { label: "Thông tin vai trò" }]}
      cardTitle="Chỉnh sửa thông tin vai trò"
      backHref="/admin/role"
      fields={[
        { label: "Mã vai trò:", name: "code", value: "CUSTOMER" },
        { label: "Tên vai trò:", name: "name", value: "Khách hàng" },
        { label: "Mô tả:", name: "description", type: "textarea", value: "Khách hàng xem hồ sơ, xe, vé đăng ký và lịch sử gửi xe." },
      ]}
    />
  );
}
