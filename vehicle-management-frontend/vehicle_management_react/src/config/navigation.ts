import type { NavItem } from "../shared/types/common";

export const adminNavigation: NavItem[] = [
  { label: "Trang chủ", icon: "fas fa-tachometer-alt", href: "#/admin/dashboard", match: ["/admin/dashboard"] },
  { label: "Quản lý vào ra", icon: "fas fa-th", href: "#/admin/swipe", match: ["/admin/swipe"] },
  {
    label: "Quản lý thẻ",
    icon: "fas fa-credit-card",
    match: ["/admin/card", "/admin/lost"],
    children: [
      { label: "Thẻ", icon: "far fa-circle", href: "#/admin/card", match: ["/admin/card"] },
      { label: "Thẻ bị mất", icon: "far fa-circle", href: "#/admin/lost", match: ["/admin/lost"] },
    ],
  },
  {
    label: "Vé & Phương tiện",
    icon: "fas fa-book",
    match: ["/admin/ticket", "/admin/vehicle"],
    children: [
      { label: "Vé", icon: "far fa-circle", href: "#/admin/ticket", match: ["/admin/ticket"] },
      { label: "Phương tiện", icon: "far fa-circle", href: "#/admin/vehicle", match: ["/admin/vehicle"] },
    ],
  },
  {
    label: "Bảng giá",
    icon: "fas fa-file-invoice-dollar",
    match: ["/admin/visitorParkingFee", "/admin/parkingFeeOfCustomer"],
    children: [
      { label: "Phí vãng lai", icon: "far fa-circle", href: "#/admin/visitorParkingFee", match: ["/admin/visitorParkingFee"] },
      { label: "Phí đăng ký", icon: "far fa-circle", href: "#/admin/parkingFeeOfCustomer", match: ["/admin/parkingFeeOfCustomer"] },
    ],
  },
  {
    label: "Thành viên",
    icon: "fas fa-users",
    match: ["/admin/account", "/admin/customer"],
    children: [
      { label: "Tài khoản", icon: "far fa-circle", href: "#/admin/account", match: ["/admin/account"] },
      { label: "Khách hàng", icon: "far fa-circle", href: "#/admin/customer", match: ["/admin/customer"] },
    ],
  },
  { label: "Quản lý vai trò", icon: "fas fa-chart-pie", href: "#/admin/role", match: ["/admin/role"] },
];

export const clientNavigation = [
  { label: "Trang chủ", href: "#/pricing" },
  { label: "Vào ra", href: "#/customerTicket/customer-infor" },
  { label: "Liên hệ", href: "#/contact" },
];
