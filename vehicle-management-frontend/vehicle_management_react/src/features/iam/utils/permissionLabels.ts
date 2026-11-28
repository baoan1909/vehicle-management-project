// These are presentation labels. The matrix still uses the original permission keys.
const modules: Record<string, string> = {
  ACCOUNT: "Tài khoản", ACCOUNT_DELETION: "Xóa tài khoản", ACCOUNT_DELETION_AUDIT: "Nhật ký xóa tài khoản",
  ACCOUNT_DELETION_CANCEL: "Hủy xóa tài khoản", ACCOUNT_DELETION_READ: "Yêu cầu xóa tài khoản",
  ACCOUNT_DELETION_REQUEST: "Đề nghị xóa tài khoản", AI_CATALOG: "Danh mục mô hình AI",
  AI_KNOWLEDGE: "Kho kiến thức AI", AI_MODEL: "Mô hình AI", AUDIT_LOG: "Nhật ký hệ thống",
  AVATAR_APPROVAL: "Duyệt ảnh đại diện",
  BROADCAST_NOTIFICATION: "Thông báo chung", CARD: "Thẻ gửi xe", CARD_TYPE: "Loại thẻ",
  CHAT_ATTACHMENT: "Tệp đính kèm", CHAT_CONVERSATION: "Hội thoại", CHAT_MESSAGE: "Tin nhắn",
  CLOSED_ORGANIZATION: "Đơn vị đã đóng", CUSTOMER: "Khách hàng", CUSTOMER_VEHICLE: "Xe khách hàng",
  DASHBOARD: "Tổng quan", DELETED_ACCOUNT: "Tài khoản đã xóa", DELETED_IDENTITY: "Định danh đã xóa",
  DEVICE: "Thiết bị", DEVICE_STATUS: "Trạng thái thiết bị", EMPLOYEE: "Nhân viên",
  INVOICE: "Hóa đơn", LANE: "Làn xe", LOGIN_ATTEMPT: "Lịch sử đăng nhập", LOST_CARD_REPORT: "Báo mất thẻ",
  ONBOARDING_APPROVAL: "Duyệt hồ sơ đăng ký", ONBOARDING_APPROVAL_REVIEW: "Duyệt hồ sơ đăng ký",
  ONBOARDING_APPROVAL_REVIEW_PARKING: "Duyệt hồ sơ quản lý bãi", ONBOARDING_APPROVAL_REVIEW_SYSTEM: "Duyệt hồ sơ quản trị",
  ORGANIZATION: "Đối tác", ORGANIZATION_APPROVAL: "Duyệt hồ sơ đối tác", ORGANIZATION_MEMBERSHIP: "Thành viên đơn vị",
  ORGANIZATION_PROFILE: "Hồ sơ đối tác", ORGANIZATION_PROFILE_UPDATE: "Cập nhật hồ sơ đối tác",
  ORGANIZATION_REGISTRATION: "Đăng ký đối tác", PARKING: "Vận hành bãi xe", PARKING_DATA_SCOPE: "Phạm vi dữ liệu bãi xe",
  PARKING_EVENT: "Sự kiện gửi xe", PARKING_LAYOUT: "Sơ đồ bãi xe", PARKING_LOT: "Bãi xe",
  PARKING_LOT_REVIEW: "Đánh giá bãi xe", PARKING_SCOPE: "Phạm vi bãi xe", PARKING_SESSION: "Phiên gửi xe",
  PARKING_SESSION_PLATE: "Biển số trong phiên gửi", PARKING_SPACE: "Ô đỗ", PARKING_SPACE_STATUS: "Trạng thái ô đỗ",
  PARKING_TOPOLOGY: "Cấu trúc bãi xe", PAYMENT: "Thanh toán", PERMISSION: "Quyền", PLATFORM_FEE: "Phí nền tảng",
  PRICE_PLAN: "Bảng giá", PRICE_RULE: "Quy tắc giá", PUBLIC_INFO: "Thông tin công khai", REPORT: "Báo cáo",
  ROLE: "Vai trò", ROLE_ASSIGN: "Gán quyền cho vai trò", ROLE_REVOKE: "Thu hồi quyền vai trò",
  SHIFT: "Ca trực", SHIFT_ASSIGNMENT: "Phân công ca trực", SUBSCRIPTION: "Vé tháng", SUPPORT: "Hỗ trợ",
  SUPPORT_TICKET: "Phiếu hỗ trợ", SUPPORT_TICKET_CATEGORY: "Danh mục hỗ trợ",
  SUPPORT_TICKET_CREATE_FROM: "Tạo phiếu hỗ trợ từ", SUPPORT_TICKET_ESCALATION: "Yêu cầu xem xét hỗ trợ",
  SUPPORT_WIDGET: "Khung hỗ trợ", TICKET_TYPE: "Loại vé", USER_GUIDE: "Hướng dẫn sử dụng",
  USER_PROFILE: "Hồ sơ người dùng", VEHICLE_TYPE: "Loại phương tiện", VOUCHER: "Mã ưu đãi", WALLET: "Ví điện tử",
  WALLET_ADJUST: "Điều chỉnh ví", WALLET_BANK_ACCOUNT_MANAGE: "Quản lý tài khoản ngân hàng",
  WALLET_PAY: "Thanh toán từ ví", WALLET_PAYOUT: "Chi trả từ ví", WALLET_PAYOUT_CREATE: "Tạo yêu cầu chi trả",
  WALLET_READ: "Tra cứu ví", WALLET_RECONCILIATION: "Đối soát ví", WALLET_REFUND: "Hoàn tiền vào ví",
  WALLET_SCOPE: "Phạm vi ví", WALLET_SETTLEMENT: "Quyết toán ví", WALLET_SETTLEMENT_READ: "Tra cứu quyết toán",
  WALLET_TOP: "Nạp tiền vào ví", WALLET_TRANSACTION: "Giao dịch ví", WALLET_TRANSACTION_READ: "Tra cứu giao dịch ví",
  ZONE: "Khu vực",
};

const actions: Record<string, string> = {
  ACCESS: "Truy cập", ACCESS_SCOPE: "Truy cập phạm vi", ACCESS_WIDGET: "Mở khung hỗ trợ",
  ACTIVATE: "Kích hoạt", ADJUST: "Điều chỉnh", ADMIN: "Quản trị hệ thống", APPROVE: "Phê duyệt",
  APPROVE_ADJUSTMENT: "Duyệt điều chỉnh", APPROVE_PAYOUT: "Duyệt chi trả", ARCHIVE: "Lưu trữ",
  ASSIGN: "Gán", ASSIGN_CARD: "Cấp thẻ", CANCEL: "Hủy", CHAT: "Hội thoại",
  CHECK_IN: "Ghi nhận vào", CHECK_OUT: "Ghi nhận ra", CLAIM: "Nhận xử lý", CLOSE: "Đóng",
  COMPLETE: "Hoàn tất", CONFIGURE: "Cấu hình", CONFIG_UPDATE: "Cập nhật cấu hình", CREATE: "Tạo mới",
  CREATE_FROM_CHAT: "Tạo từ hội thoại", CUSTOMER: "Khách hàng", DELETE: "Xóa", EMPLOYEE: "Nhân viên",
  EXPIRE: "Kết thúc hiệu lực", GENERATE: "Tạo", INACTIVATE: "Ngừng hoạt động", INTERNAL: "Nội bộ",
  INVOICE: "Hóa đơn", LOCK: "Khóa", LOOKUP: "Tra cứu", MANAGE: "Quản lý", MANAGER: "Quản lý bãi",
  MARK_DEFAULT: "Đặt mặc định", MARK_READ: "Đánh dấu đã đọc", MODERATE: "Kiểm duyệt", OBSERVE: "Giám sát",
  OPEN: "Mở", OVERRIDE: "Ghi đè", OVERRIDE_PLATE: "Ghi đè biển số", PARKING: "Bãi xe", PARTNER: "Đối tác",
  PAY: "Thanh toán", PAYOUT: "Chi trả", PAY_INVOICE: "Thanh toán hóa đơn", PERMISSION: "Quyền",
  PLATFORM: "Nền tảng", PREVIEW: "Xem trước", PROCESS: "Xử lý", PUBLISH: "Phát hành", READ: "Xem",
  READ_AUDIT: "Xem nhật ký", READ_CLOSED: "Xem dữ liệu đã đóng", READ_DELETED: "Xem dữ liệu đã xóa",
  READ_TRANSACTION: "Xem giao dịch", READ_WALLET: "Xem ví", RECONCILE: "Đối soát", REFUND: "Hoàn tiền",
  REINDEX: "Lập chỉ mục lại", REJECT: "Từ chối", REOPEN: "Mở lại", REQUEST: "Yêu cầu",
  RESET_PASSWORD: "Đặt lại mật khẩu", RESOLVE: "Giải quyết", RESPOND: "Phản hồi", REVIEW: "Xem xét",
  SCOPE: "Phạm vi", SELF: "Bản thân", SEND: "Gửi", SETTLE: "Quyết toán", SHIFT: "Ca trực",
  STATUS_UPDATE: "Cập nhật trạng thái", SUSPEND: "Tạm ngưng", SYNC: "Đồng bộ", TICKET: "Phiếu hỗ trợ",
  TOP_UP: "Nạp tiền", UNASSIGN: "Thu hồi", UNLOCK: "Mở khóa", UP: "Nạp tiền", UPDATE: "Cập nhật",
};

const scopes: Record<string, string> = {
  ALL: "Toàn bộ", OWN: "Của mình", PUBLIC: "Công khai", ASSIGNED: "Được phân công", LOT: "Theo bãi xe",
  SELF: "Bản thân", PLATFORM: "Nền tảng", PARTNER: "Đối tác", CUSTOMER: "Khách hàng",
  EMPLOYEE: "Nhân viên", TENANT_PRIVATE: "Riêng theo đơn vị",
};

export function getPermissionModuleLabel(code: string) {
  return Object.prototype.hasOwnProperty.call(modules, code) ? modules[code] : "Chức năng khác";
}

export function getPermissionActionLabel(code: string) {
  const normalized = code.toUpperCase();
  return Object.prototype.hasOwnProperty.call(actions, normalized) ? actions[normalized] : "Thao tác khác";
}

export function getPermissionScopeLabel(code: string) {
  const normalized = code.toUpperCase();
  return Object.prototype.hasOwnProperty.call(scopes, normalized) ? scopes[normalized] : "Phạm vi khác";
}
