import { getUserPermissionSet } from "@/shared/auth/permissions";
import type { CurrentUser } from "@/shared/types/common";

// Presentation eligibility only. Runtime audience and business guards remain authoritative.
export const WORKFLOW_COACH_MARKS = [
  {
    "code": "WORKFLOW_STRUCTURE_SETUP",
    "title": "Thiết lập cấu trúc bãi theo thứ tự",
    "path": "/admin/parking-structure",
    "permissions": [
      "PARKING_SPACE_CONFIGURE_ALL",
      "PARKING_TOPOLOGY_CONFIGURE_ALL"
    ],
    "mode": "all"
  },
  {
    "code": "PARKING_LAYOUT_SETUP",
    "title": "Ô đỗ và xuất bản layout cho khu SPACE",
    "path": "/admin/parking-layouts",
    "permissions": [
      "PARKING_SPACE_READ_ALL"
    ],
    "mode": "any"
  },
  {
    "code": "WORKFLOW_CARD_STOCK",
    "title": "Chuẩn bị và tra cứu kho thẻ đúng bãi",
    "path": "/admin/card",
    "permissions": [
      "CARD_READ_ALL"
    ],
    "mode": "any"
  },
  {
    "code": "WORKFLOW_DEVICE_SETUP",
    "title": "Kiểm tra thiết bị và ảnh trước vận hành",
    "path": "/admin/devices",
    "permissions": [
      "DEVICE_READ_ALL"
    ],
    "mode": "any"
  },
  {
    "code": "WORKFLOW_STAFF_ASSIGNMENT",
    "title": "Chuẩn bị nhân sự và phân công bãi",
    "path": "/admin/employee",
    "permissions": [
      "EMPLOYEE_READ_ALL"
    ],
    "mode": "any"
  },
  {
    "code": "WORKFLOW_SHIFT_PLANNING",
    "title": "Phân công bãi, lập và duyệt ca vận hành",
    "path": "/admin/shifts",
    "permissions": [
      "SHIFT_READ_ALL"
    ],
    "mode": "any"
  },
  {
    "code": "WORKFLOW_OWN_SHIFT",
    "title": "Kiểm tra ca của tôi trước xe vào/ra",
    "path": "/admin/shifts",
    "permissions": [
      "SHIFT_ASSIGNMENT_READ_OWN"
    ],
    "mode": "any"
  },
  {
    "code": "WORKFLOW_VEHICLE_DRAFT",
    "title": "Rà soát loại phương tiện nháp",
    "path": "/admin/vehicle",
    "permissions": [
      "VEHICLE_TYPE_READ_ALL"
    ],
    "mode": "any"
  },
  {
    "code": "WORKFLOW_TICKET_DRAFT",
    "title": "Rà soát loại vé nháp",
    "path": "/admin/ticket",
    "permissions": [
      "TICKET_TYPE_READ_ALL"
    ],
    "mode": "any"
  },
  {
    "code": "WORKFLOW_PLAN_DRAFT",
    "title": "Từ kế hoạch giá nháp đến có hiệu lực",
    "path": "/admin/price-plans",
    "permissions": [
      "PRICE_PLAN_READ_ALL"
    ],
    "mode": "any"
  },
  {
    "code": "WORKFLOW_RULE_DRAFT",
    "title": "Từ quy tắc giá nháp đến phủ đủ dịch vụ",
    "path": "/admin/price-rules",
    "permissions": [
      "PRICE_RULE_READ_ALL"
    ],
    "mode": "any"
  },
  {
    "code": "WORKFLOW_SESSION_LOOKUP",
    "title": "Tìm phiên trước khi checkout hoặc báo mất",
    "path": "/admin/swipe/sessions",
    "permissions": [
      "CARD_READ_ALL",
      "PARKING_SESSION_CHECK_IN_ALL",
      "PARKING_SESSION_CHECK_OUT_ALL"
    ],
    "mode": "any"
  },
  {
    "code": "WORKFLOW_VISITOR_ENTRY",
    "title": "Vãng lai: từ chuẩn bị đến xác nhận xe vào",
    "path": "/admin/swipe",
    "permissions": [
      "PARKING_SESSION_CHECK_IN_ALL"
    ],
    "mode": "any"
  },
  {
    "code": "WORKFLOW_CHECKOUT",
    "title": "Xe ra: tìm phiên, tính phí, thanh toán và hoàn tất",
    "path": "/admin/swipe",
    "permissions": [
      "PARKING_SESSION_CHECK_OUT_ALL"
    ],
    "mode": "any"
  },
  {
    "code": "WORKFLOW_SUBSCRIPTION_QR",
    "title": "Xe vào/ra bằng vé đăng ký và QR",
    "path": "/admin/swipe",
    "permissions": [
      "PARKING_SESSION_CHECK_IN_ALL"
    ],
    "mode": "any"
  },
  {
    "code": "WORKFLOW_RESERVATION_QR",
    "title": "Tiếp nhận xe đặt chỗ đã thanh toán",
    "path": "/admin/swipe",
    "permissions": [
      "PARKING_SESSION_CHECK_IN_ALL"
    ],
    "mode": "any"
  },
  {
    "code": "WORKFLOW_OPERATION_RECOVERY",
    "title": "Xử lý blocker, biển số và retry an toàn",
    "path": "/admin/swipe",
    "permissions": [
      "PARKING_SESSION_CHECK_IN_ALL",
      "PARKING_SESSION_CHECK_OUT_ALL"
    ],
    "mode": "any"
  },
  {
    "code": "WORKFLOW_SUBSCRIPTION_FULFILLMENT",
    "title": "Từ yêu cầu vé đến thanh toán và cấp thẻ",
    "path": "/admin/subscription-approvals",
    "permissions": [
      "SUBSCRIPTION_READ_ALL"
    ],
    "mode": "any"
  },
  {
    "code": "WORKFLOW_ACTIVATION",
    "title": "Kiểm tra bàn giao và đưa bãi vào hoạt động",
    "path": "/admin/parking-lots",
    "permissions": [
      "PARKING_LOT_READ_ALL"
    ],
    "mode": "any"
  }
] as const;

export function getWorkflowCoachMarks(user: CurrentUser | null | undefined, pathname: string, search = "") {
  if (!user || user.onboardingRequired || (user.accessState && user.accessState !== "ACTIVE")) return [];
  const permissions = getUserPermissionSet(user);
  const path = pathname.replace(/\/$/, "");
  const eligible = WORKFLOW_COACH_MARKS.filter((item) => item.path === path && (
    item.mode === "all"
      ? item.permissions.every((permission) => permissions.has(permission))
      : item.permissions.some((permission) => permissions.has(permission))
  ));
  const preferred = new URLSearchParams(search).get("mode") === "check-out" ? "WORKFLOW_CHECKOUT" : "WORKFLOW_VISITOR_ENTRY";
  return [...eligible].sort((left, right) => Number(right.code === preferred) - Number(left.code === preferred));
}

