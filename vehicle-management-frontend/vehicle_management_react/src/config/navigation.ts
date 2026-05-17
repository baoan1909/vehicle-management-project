import type { AdminSidebarEntry } from "../shared/types/common";

export const adminNavigation: AdminSidebarEntry[] = [
  {
    kind: "link",
    label: "Trang chủ",
    to: "/admin/dashboard",
    matches: ["/admin/dashboard"],
    icon: "dashboard",
  },
  { kind: "divider" },
  {
    kind: "link",
    label: "Quản lý vào ra",
    to: "/admin/swipe",
    matches: ["/admin/swipe"],
    icon: "swipe",
  },
  {
    kind: "group",
    label: "Quản lý thẻ",
    icon: "card",
    defaultExpanded: true,
    items: [
      { label: "Thẻ", to: "/admin/card", matches: ["/admin/card"] },
      { label: "Thẻ bị mất", to: "/admin/lost", matches: ["/admin/lost"] },
    ],
  },
  {
    kind: "group",
    label: "Vé và phương tiện",
    icon: "catalog",
    defaultExpanded: true,
    items: [
      { label: "Vé", to: "/admin/ticket", matches: ["/admin/ticket"] },
      { label: "Phương tiện", to: "/admin/vehicle", matches: ["/admin/vehicle"] },
    ],
  },
  {
    kind: "group",
    label: "Bảng giá",
    icon: "pricing",
    items: [
      { label: "Phí vãng lai", to: "/admin/visitorParkingFee", matches: ["/admin/visitorParkingFee"] },
      { label: "Phí đăng ký", to: "/admin/parkingFeeOfCustomer", matches: ["/admin/parkingFeeOfCustomer"] },
    ],
  },
  {
    kind: "group",
    label: "Thành viên",
    icon: "members",
    items: [
      { label: "Tài khoản", to: "/admin/account", matches: ["/admin/account"] },
      { label: "Khách hàng", to: "/admin/customer", matches: ["/admin/customer"] },
    ],
  },
  { kind: "divider" },
  {
    kind: "link",
    label: "Quản lý vai trò",
    to: "/admin/role",
    matches: ["/admin/role"],
    icon: "role",
  },
];

export const clientNavigation = [
  { label: "Trang chủ", href: "/pricing" },
  { label: "Vào ra", href: "/customerTicket/customer-infor" },
  { label: "Liên hệ", href: "/contact" },
];
