// Display labels only. Keep API values, permission checks and form values unchanged.
// A domain is required because OPEN/ACTIVE/PENDING have different business meanings.
export const enumLabels = {
  accessError: {
    unauthorized: "Phiên đăng nhập hết hạn", forbidden: "Không có quyền truy cập", offline: "Mất kết nối",
    timeout: "Hết thời gian chờ", server: "Lỗi máy chủ", "invalid-response": "Phản hồi không hợp lệ",
  },
  accountStatus: {
    ACTIVE: "Đang hoạt động", PENDING: "Chờ kích hoạt", LOCKED: "Đã khóa", DISABLED: "Đã vô hiệu hóa", DELETED: "Đã xóa",
  },
  accountLifecycle: {
    ACTIVE: "Đang hoạt động", INACTIVE: "Ngừng hoạt động", SUSPENDED: "Tạm ngưng",
    LOCKED: "Đã khóa", DISABLED: "Đã vô hiệu hóa", NOT_APPLICABLE: "Không áp dụng",
    PROFILE_REQUIRED: "Chưa hoàn tất hồ sơ", PROFILE_COMPLETED: "Đã hoàn tất hồ sơ",
    NOT_SUBMITTED: "Chưa gửi hồ sơ", PENDING: "Chờ phê duyệt", APPROVED: "Đã phê duyệt",
    REJECTED: "Bị từ chối / Cần bổ sung", NOT_CREATED: "Chưa tạo hồ sơ nhân sự",
    ORGANIZATION_REQUIRED: "Chưa có đơn vị", PARKING_LOT_ASSIGNMENT_REQUIRED: "Chưa được phân công bãi",
    SCOPE_READY: "Đã có phạm vi truy cập", ONBOARDING_REQUIRED: "Cần hoàn tất đăng ký",
    PENDING_APPROVAL: "Đang chờ duyệt", ACCOUNT_PENDING: "Tài khoản chờ kích hoạt",
    ACCOUNT_LOCKED: "Tài khoản đã khóa", ACCOUNT_DISABLED: "Tài khoản đã vô hiệu hóa",
    ROLE_DISABLED: "Vai trò đã vô hiệu hóa", CUSTOMER_INACTIVE: "Hồ sơ khách hàng ngừng hoạt động",
    CUSTOMER_APPROVAL_REQUIRED: "Hồ sơ khách hàng cần được duyệt", EMPLOYEE_INACTIVE: "Nhân viên ngừng hoạt động",
    ORGANIZATION_ACCESS_REQUIRED: "Thiếu phạm vi vận hành", NO_EFFECTIVE_PERMISSIONS: "Chưa có quyền có hiệu lực",
    VERIFY_EMAIL: "Xác minh email", COMPLETE_PROFILE: "Hoàn tất hồ sơ", SUBMIT_FOR_APPROVAL: "Gửi hồ sơ xét duyệt",
    WAIT_FOR_APPROVAL: "Chờ phê duyệt", CONTACT_ADMIN: "Liên hệ quản trị viên",
    CONTACT_ORGANIZATION_ADMIN: "Liên hệ quản trị đối tác",
  },
  customerType: { REGISTERED: "Khách đăng ký", VIP: "Khách ưu tiên", VISITOR: "Khách vãng lai" },
  cardStatus: {
    AVAILABLE: "Sẵn sàng cấp", ASSIGNED: "Đã gán", IN_USE: "Đang sử dụng", RESERVED: "Đã giữ chỗ",
    BLOCKED: "Đã khóa", LOST: "Mất thẻ", RETIRED: "Đã thu hồi",
  },
  invoiceStatus: { UNPAID: "Chưa thanh toán", PAID: "Đã thanh toán", CANCELLED: "Đã hủy", REFUNDED: "Đã hoàn tiền" },
  parkingSessionStatus: { OPEN: "Đang gửi xe", CLOSED: "Đã kết thúc", LOST_CARD: "Mất thẻ", CANCELLED: "Đã hủy" },
  parkingLotStatus: { SETUP: "Đang thiết lập", ACTIVE: "Đang hoạt động", MAINTENANCE: "Đang bảo trì", CLOSED: "Đã đóng" },
  shiftAssignmentStatus: { DRAFT: "Nháp", SCHEDULED: "Đã lên lịch", ACTIVE: "Đang thực hiện", REMOVED: "Đã gỡ phân công" },
  transactionStatus: { PENDING: "Chờ xử lý", POSTED: "Đã ghi sổ", REVERSED: "Đã đảo giao dịch", FAILED: "Thất bại" },
  transactionType: {
    TOP_UP: "Nạp tiền", WALLET_TOP_UP: "Nạp tiền vào ví", INVOICE_PAYMENT: "Thanh toán hóa đơn",
    REFUND: "Hoàn tiền", WALLET_ADJUSTMENT: "Điều chỉnh số dư", REVENUE_ALLOCATION: "Phân bổ doanh thu",
    SETTLEMENT_RELEASE: "Chuyển tiền sau đối soát", PAYOUT_HOLD: "Giữ tiền chờ chi trả",
    PAYOUT_COMPLETED: "Hoàn tất chi trả", PAYOUT_FAILED_REVERT: "Hoàn tiền do chi trả thất bại",
  },
  topupStatus: { PENDING: "Chờ thanh toán", COMPLETED: "Đã nạp tiền", FAILED: "Thất bại", CANCELLED: "Đã hủy", EXPIRED: "Đã hết hạn" },
  guideStatus: { DRAFT: "Nháp", PUBLISHED: "Đã phát hành", INACTIVE: "Ngừng hiển thị", ARCHIVED: "Đã lưu trữ" },
  guideDisplayType: {
    MODAL: "Hộp thoại", BOTTOM_SHEET: "Bảng trượt từ dưới", COACH_MARK: "Hướng dẫn từng bước",
    TOOLTIP: "Chú thích", POPOVER: "Khung thông tin nổi", BANNER: "Dải thông báo",
    INLINE_CARD: "Thẻ trong trang", CALLOUT: "Khung lưu ý", JOURNEY: "Lộ trình vận hành",
  },
  guideAudienceMode: {
    PUBLIC: "Công khai", ALL_AUTHENTICATED: "Mọi tài khoản đăng nhập", ROLE: "Theo vai trò",
    PERMISSION_ANY: "Có ít nhất một quyền", PERMISSION_ALL: "Có đầy đủ các quyền",
  },
  guideReadiness: {
    ACCOUNT_ACCESS: "Quyền truy cập tài khoản", PARKING_SCOPE: "Phạm vi quản lý bãi xe",
    CATALOG_PRICING: "Danh mục và bảng giá", PARKING_TOPOLOGY: "Cấu trúc bãi xe",
    PARKING_ACTIVATION: "Kích hoạt bãi xe", WORKFORCE_SHIFT: "Nhân sự và ca trực",
    CHECK_IN: "Ghi nhận xe vào", FIND_SESSION: "Tìm phiên gửi xe", PREPARE_CHECK_OUT: "Chuẩn bị xe ra",
    COLLECT_PAYMENT: "Thu tiền", COMPLETE_CHECK_OUT: "Hoàn tất xe ra", EXCEPTIONS: "Xử lý ngoại lệ",
  },
  knowledgeDocumentStatus: {
    PENDING: "Chờ xử lý", PROCESSING: "Đang xử lý", REVIEW: "Chờ duyệt",
    READY: "Sẵn sàng", FAILED: "Thất bại", ARCHIVED: "Đã lưu trữ",
  },
  ingestionEvent: {
    JOB_CREATED: "Đã tạo tác vụ", JOB_PROCESSING: "Đang xử lý", JOB_COMPLETED: "Đã hoàn tất",
    JOB_FAILED: "Xử lý thất bại", JOB_RETRY_SCHEDULED: "Đã lên lịch thử lại", JOB_REVIEW: "Chờ duyệt",
    STAGE_EXTRACTING: "Đang đọc tệp", STAGE_CHUNKING: "Đang chia nội dung", STAGE_EMBEDDING: "Đang chuẩn bị tìm kiếm",
    DOCUMENT_APPROVED: "Đã duyệt tài liệu", DOCUMENT_ARCHIVED: "Đã lưu trữ tài liệu", DOCUMENT_REJECTED: "Đã từ chối tài liệu",
  },
  aiModelStatus: { CANDIDATE: "Đang đánh giá", ACTIVE: "Đang sử dụng", FALLBACK: "Dự phòng", DISABLED: "Đã vô hiệu hóa" },
  aiUseCase: {
    SUPPORT_CHAT: "Trò chuyện hỗ trợ", INTENT_CLASSIFICATION: "Phân loại ý định",
    CONVERSATION_SUMMARY: "Tóm tắt hội thoại", EMBEDDING: "Mã hóa dữ liệu tìm kiếm",
  },
  aiModelAction: {
    generateContent: "Tạo nội dung", streamGenerateContent: "Tạo nội dung liên tục",
    embedContent: "Mã hóa nội dung tìm kiếm", batchEmbedContents: "Mã hóa nhiều nội dung",
    countTokens: "Đếm đơn vị văn bản", createCachedContent: "Lưu bộ nhớ đệm",
    predict: "Dự đoán", predictLongRunning: "Dự đoán bất đồng bộ",
  },
  trackingMode: { CAPACITY: "Theo sức chứa", SPACE: "Theo ô đỗ" },
  layoutStatus: { DRAFT: "Nháp", PUBLISHED: "Đã xuất bản", ARCHIVED: "Đã lưu trữ" },
  layoutElementType: {
    WALL: "Tường", COLUMN: "Cột", DRIVE_LANE: "Lối xe chạy", ENTRY: "Lối vào", EXIT: "Lối ra",
    TEXT: "Văn bản", LABEL: "Nhãn", BACKGROUND: "Nền", IMAGE_OVERLAY: "Ảnh phủ",
  },
  identityType: {
    USERNAME: "Tên đăng nhập", EMAIL: "Email", PHONE_NUMBER: "Số điện thoại", IDENTIFY_CARD: "CCCD/CMND",
    CUSTOMER_CODE: "Mã khách hàng", EMPLOYEE_CODE: "Mã nhân viên", PROVIDER_SUBJECT: "Định danh đăng nhập ngoài",
  },
  deletionStatus: {
    GRACE_PERIOD: "Đang chờ hết thời hạn hủy", CANCELLED: "Đã hủy", PROCESSING: "Đang xử lý",
    RETRY: "Chờ thử lại", BLOCKED: "Đang bị chặn", REVIEW_REQUIRED: "Cần kiểm tra", COMPLETED: "Đã hoàn tất",
  },
  cleanupStatus: { SCHEDULED: "Đã lên lịch", PROCESSING: "Đang xử lý", RETRY: "Chờ thử lại", COMPLETED: "Đã hoàn tất", REVIEW_REQUIRED: "Cần kiểm tra" },
  cleanupResource: {
    IDENTITY_PROVIDER_USER: "Tài khoản tại nhà cung cấp đăng nhập", AVATAR_OBJECT: "Ảnh đại diện",
    TENANT_PRIVATE_OBJECT: "Dữ liệu riêng của đơn vị",
  },
  organizationStatus: { ACTIVE: "Đang hoạt động", INACTIVE: "Ngừng hoạt động", SUSPENDED: "Tạm ngưng", CLOSED: "Đã đóng" },
  approvalRequestType: {
    CUSTOMER_ONBOARDING: "Duyệt hồ sơ khách hàng", INTERNAL_EMPLOYEE_ONBOARDING: "Duyệt hồ sơ nhân sự",
    SYSTEM_ADMIN_ONBOARDING: "Duyệt hồ sơ quản trị hệ thống",
    PARTNER_REGISTRATION: "Đăng ký đối tác", ORGANIZATION_PROFILE_UPDATE: "Cập nhật hồ sơ đối tác",
    PARKING_LOT_ACTIVATION: "Kích hoạt bãi xe", AVATAR_UPDATE: "Cập nhật ảnh đại diện",
  },
} as const;

export type EnumLabelDomain = keyof typeof enumLabels;

export function getEnumLabel(domain: EnumLabelDomain, value?: string | null, emptyLabel = "—"): string {
  if (!value?.trim()) return emptyLabel;
  const labels: Readonly<Record<string, string>> = enumLabels[domain];
  // Never show a raw server enum as a fallback, or mislabel a new value as a known state.
  return Object.prototype.hasOwnProperty.call(labels, value) ? labels[value] : "Chưa xác định";
}
