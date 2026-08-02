import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Badge, Button, DateTimeScheduleField, Drawer, Modal, PaginationFooter, SelectMenu, nowLocalDateTime, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import {
  cancelBroadcastAnnouncement,
  createBroadcastAnnouncement,
  deleteBroadcastAnnouncement,
  getBroadcastAnnouncements,
  getNotificationActiveRoles,
  publishBroadcastAnnouncement,
  updateBroadcastAnnouncement,
  updateBroadcastAnnouncementDisplayOrder,
  type BroadcastAnnouncementAudienceType,
  type BroadcastAnnouncementPayload,
  type BroadcastAnnouncementResponse,
  type BroadcastAnnouncementStatus,
  type NotificationActiveRoleResponse,
  type NotificationType,
} from "@/features/notifications/api/notificationApi";
import { cn } from "@/lib/cn";
import { hasAnyPermission } from "@/shared/auth/permissions";

type AnnouncementFormState = {
  audienceType: BroadcastAnnouncementAudienceType;
  displayOrder: string;
  enabled: boolean;
  endAt: string;
  message: string;
  notificationType: NotificationType;
  publishTiming: "now" | "scheduled";
  redirectUrl: string;
  relatedId: string;
  relatedSchema: string;
  relatedTable: string;
  roleCodes: string[];
  startAt: string;
  title: string;
};

type PendingAction = {
  action: "publish" | "cancel" | "delete";
  announcement: BroadcastAnnouncementResponse;
} | null;

type ActionMenuPosition = {
  left: number;
  top: number;
};

type RoleOption = {
  code: string;
  label: string;
};

type AnnouncementFieldErrors = {
  displayOrder?: string;
  endAt?: string;
  message?: string;
  roleCodes?: string;
  startAt?: string;
  title?: string;
};

const pageSizeOptions = [8, 10, 20];

const fallbackRoleOptions: RoleOption[] = [
  { code: "SYSTEM_ADMIN", label: "Quản trị hệ thống" },
  { code: "PARKING_MANAGER", label: "Quản lý bãi xe" },
  { code: "EMPLOYEE", label: "Nhân viên vận hành" },
  { code: "CUSTOMER", label: "Khách hàng" },
];

const notificationTypeOptions: Array<{ badge: string; icon: string; label: string; tone: "primary" | "success" | "warning" | "danger" | "neutral"; value: NotificationType }> = [
  { badge: "Hệ thống", icon: "fas fa-cog", label: "Thông báo hệ thống", tone: "primary", value: "SYSTEM_NOTICE" },
  { badge: "Vé tháng", icon: "far fa-id-card", label: "Đăng ký vé tháng", tone: "primary", value: "SUBSCRIPTION_REQUESTED" },
  { badge: "Vé tháng", icon: "far fa-clock", label: "Vé tháng sắp hết hạn", tone: "warning", value: "SUBSCRIPTION_EXPIRING_SOON" },
  { badge: "Thanh toán", icon: "far fa-file-invoice", label: "Hóa đơn mới", tone: "warning", value: "INVOICE_CREATED" },
  { badge: "Thanh toán", icon: "far fa-check-circle", label: "Thanh toán thành công", tone: "success", value: "PAYMENT_SUCCEEDED" },
  { badge: "Thanh toán", icon: "far fa-times-circle", label: "Thanh toán thất bại", tone: "danger", value: "PAYMENT_FAILED" },
  { badge: "Hỗ trợ", icon: "far fa-life-ring", label: "Ticket mới", tone: "primary", value: "SUPPORT_TICKET_CREATED" },
  { badge: "Hỗ trợ", icon: "far fa-user", label: "Ticket được phân công", tone: "primary", value: "SUPPORT_TICKET_ASSIGNED" },
  { badge: "Nội bộ", icon: "far fa-calendar-check", label: "Phân ca mới", tone: "neutral", value: "SHIFT_ASSIGNED" },
  { badge: "Vận hành", icon: "fas fa-wifi", label: "Thiết bị offline", tone: "danger", value: "DEVICE_OFFLINE" },
  { badge: "Vận hành", icon: "fas fa-tools", label: "Bảo trì thiết bị", tone: "warning", value: "DEVICE_MAINTENANCE" },
  { badge: "Vận hành", icon: "fas fa-road", label: "Bảo trì lane", tone: "warning", value: "LANE_MAINTENANCE" },
  { badge: "Giá", icon: "fas fa-tags", label: "Đổi kế hoạch giá", tone: "warning", value: "PRICE_PLAN_CHANGED" },
  { badge: "Giá", icon: "fas fa-percent", label: "Đổi quy tắc giá", tone: "warning", value: "PRICE_RULE_CHANGED" },
  { badge: "Tài khoản", icon: "far fa-user-circle", label: "Hồ sơ cần duyệt", tone: "primary", value: "ACCOUNT_PROFILE_SUBMITTED" },
  { badge: "Tài khoản", icon: "fas fa-user-shield", label: "Tài khoản đổi trạng thái", tone: "neutral", value: "ACCOUNT_STATUS_CHANGED" },
];

const statusTabs: Array<{ label: string; value: "all" | BroadcastAnnouncementStatus }> = [
  { label: "Tất cả", value: "all" },
  { label: "Nháp", value: "DRAFT" },
  { label: "Đã phát", value: "PUBLISHED" },
  { label: "Đã hủy", value: "CANCELLED" },
];

function buildAudienceOptions(roleOptions: RoleOption[]) {
  return [
    { label: "Tất cả đối tượng", value: "all" },
    { label: "Tất cả tài khoản đang hoạt động", value: "ALL_ACTIVE_ACCOUNTS" },
    ...roleOptions.map((role) => ({ label: role.code, value: role.code })),
  ];
}

const typeOptions = [
  { label: "Loại thông báo", value: "all" },
  ...notificationTypeOptions.map((option) => ({ label: option.label, value: option.value })),
];

const redirectTargetOptions = [
  { label: "Không mở màn hình nào", value: "none" },
  { label: "Bãi xe & sơ đồ vận hành", value: "/admin/parking-lots" },
  { label: "Thiết bị", value: "/admin/devices" },
  { label: "Bảng giá", value: "/admin/price-plans" },
  { label: "Trung tâm hỗ trợ", value: "/admin/support-center" },
  { label: "Tài khoản", value: "/admin/account" },
  { label: "Vé tháng của khách hàng", value: "/customer/subscriptions" },
  { label: "Hỗ trợ khách hàng", value: "/customer/support" },
];

const inputClassName =
  "tw-h-10 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none tw-transition placeholder:tw-text-vm-slate-400 focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)] disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25 disabled:tw-text-vm-slate-500";

const ACTION_MENU_WIDTH = 184;
const ACTION_MENU_HEIGHT = 236;
const ACTION_MENU_GAP = 8;

function toLocalDateTimeInput(value: string | null | undefined) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
  return date.toISOString().slice(0, 16);
}

function toInstant(value: string) {
  return new Date(value).toISOString();
}

function formatDate(value: string | null | undefined) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
}

function formatTime(value: string | null | undefined) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";

  return new Intl.DateTimeFormat("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function getInitialForm(announcement?: BroadcastAnnouncementResponse | null): AnnouncementFormState {
  return {
    audienceType: announcement?.audienceType ?? "ALL_ACTIVE_ACCOUNTS",
    displayOrder: `${announcement?.displayOrder ?? 100}`,
    enabled: announcement?.enabled ?? true,
    endAt: toLocalDateTimeInput(announcement?.endAt),
    message: announcement?.message ?? "",
    notificationType: announcement?.notificationType ?? "SYSTEM_NOTICE",
    publishTiming: announcement?.startAt && new Date(announcement.startAt).getTime() > Date.now() ? "scheduled" : "now",
    redirectUrl: announcement?.redirectUrl ?? "",
    relatedId: announcement?.relatedId ?? "",
    relatedSchema: announcement?.relatedSchema ?? "",
    relatedTable: announcement?.relatedTable ?? "",
    roleCodes: announcement?.roleCodes ?? [],
    startAt: toLocalDateTimeInput(announcement?.startAt) || nowLocalDateTime(),
    title: announcement?.title ?? "",
  };
}

function getStatusTone(status: BroadcastAnnouncementStatus) {
  if (status === "PUBLISHED") return "success";
  if (status === "CANCELLED") return "danger";
  return "warning";
}

function getStatusLabel(status: BroadcastAnnouncementStatus) {
  if (status === "PUBLISHED") return "Đã phát";
  if (status === "CANCELLED") return "Đã hủy";
  return "Nháp";
}

function getStatusDate(announcement: BroadcastAnnouncementResponse) {
  return announcement.publishedAt ?? announcement.cancelledAt ?? announcement.startAt ?? announcement.createdAt;
}

function getTypeMeta(value: NotificationType) {
  return notificationTypeOptions.find((option) => option.value === value) ?? notificationTypeOptions[0];
}

function getAudienceLabel(announcement: Pick<BroadcastAnnouncementResponse, "audienceType" | "roleCodes">) {
  if (announcement.audienceType === "ALL_ACTIVE_ACCOUNTS") {
    return "Tất cả tài khoản đang hoạt động";
  }
  return (announcement.roleCodes ?? []).join(", ") || "Chưa chọn role";
}

function getAuthorLabel(authorId: string | null | undefined) {
  return authorId ? `bởi ${authorId.slice(0, 8)}` : "bởi Admin";
}

function getPageItems<T>(rows: T[], currentPage: number, pageSize: number) {
  const totalPages = Math.max(1, Math.ceil(rows.length / pageSize));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const startOffset = (safeCurrentPage - 1) * pageSize;
  const pageRows = rows.slice(startOffset, startOffset + pageSize);

  return {
    endIndex: rows.length === 0 ? 0 : startOffset + pageRows.length,
    rows: pageRows,
    safeCurrentPage,
    startIndex: rows.length === 0 ? 0 : startOffset + 1,
    totalPages,
  };
}

function normalizePayload(form: AnnouncementFormState): BroadcastAnnouncementPayload {
  const startAt = form.publishTiming === "now" ? nowLocalDateTime() : form.startAt;
  const relatedId = form.relatedId.trim();
  const relatedSchema = form.relatedSchema.trim();
  const relatedTable = form.relatedTable.trim();

  const payload: BroadcastAnnouncementPayload = {
    audienceType: form.audienceType,
    displayOrder: Math.max(1, Number(form.displayOrder) || 100),
    enabled: form.enabled,
    endAt: form.endAt ? toInstant(form.endAt) : null,
    message: form.message.trim(),
    notificationType: form.notificationType,
    redirectUrl: form.redirectUrl.trim() || null,
    roleCodes: form.audienceType === "ROLE_CODES" ? form.roleCodes : [],
    startAt: toInstant(startAt),
    title: form.title.trim(),
  };

  if (relatedId) payload.relatedId = relatedId;
  if (relatedSchema) payload.relatedSchema = relatedSchema;
  if (relatedTable) payload.relatedTable = relatedTable;

  return payload;
}

function buildCopyForm(announcement: BroadcastAnnouncementResponse): AnnouncementFormState {
  const copiedTitle = `Bản sao - ${announcement.title}`.slice(0, 200);
  const copiedRoleCodes = announcement.roleCodes ?? [];
  const copiedAudienceType =
    announcement.audienceType === "ROLE_CODES" && copiedRoleCodes.length > 0
      ? "ROLE_CODES"
      : "ALL_ACTIVE_ACCOUNTS";

  return {
    audienceType: copiedAudienceType,
    displayOrder: `${announcement.displayOrder ?? 100}`,
    enabled: true,
    endAt: toLocalDateTimeInput(announcement.endAt),
    message: announcement.message,
    notificationType: announcement.notificationType,
    publishTiming: "now",
    redirectUrl: announcement.redirectUrl ?? "",
    relatedId: "",
    relatedSchema: "",
    relatedTable: "",
    roleCodes: copiedAudienceType === "ROLE_CODES" ? copiedRoleCodes : [],
    startAt: nowLocalDateTime(),
    title: copiedTitle,
  };
}

function getExpectedRecipientText(form: AnnouncementFormState | BroadcastAnnouncementResponse) {
  if (form.audienceType === "ALL_ACTIVE_ACCOUNTS") return "Tính theo toàn bộ account ACTIVE khi phát";
  const roleCount = (form.roleCodes ?? []).length;
  return roleCount ? `${roleCount} role được chọn, backend lọc account ACTIVE khi phát` : "Chưa chọn role nhận";
}

function getRedirectOptions(currentRedirectUrl: string) {
  if (!currentRedirectUrl || redirectTargetOptions.some((option) => option.value === currentRedirectUrl)) {
    return redirectTargetOptions;
  }

  return [
    ...redirectTargetOptions,
    { label: `Đường dẫn đang lưu: ${currentRedirectUrl}`, value: currentRedirectUrl },
  ];
}

function mapRoleToOption(role: NotificationActiveRoleResponse): RoleOption | null {
  const code = role.code?.trim();
  if (!code) return null;
  return {
    code,
    label: role.name?.trim() || code,
  };
}

function validateForm(
  form: AnnouncementFormState,
  titleChangeRequirement?: { initialTitle: string; required: boolean },
  titleExists?: (title: string) => boolean,
): AnnouncementFieldErrors {
  const errors: AnnouncementFieldErrors = {};
  if (!form.title.trim()) errors.title = "Vui lòng nhập tiêu đề thông báo.";
  if (
    !errors.title &&
    titleChangeRequirement?.required &&
    form.title.trim() === titleChangeRequirement.initialTitle.trim()
  ) {
    errors.title = "Vui lòng thay đổi tiêu đề thông báo sau khi sao chép.";
  }
  if (!errors.title && titleExists?.(form.title)) {
    errors.title = "Tiêu đề thông báo đã tồn tại. Vui lòng nhập tiêu đề khác.";
  }
  if (!form.message.trim()) errors.message = "Vui lòng nhập nội dung thông báo.";
  if (!Number.isInteger(Number(form.displayOrder)) || Number(form.displayOrder) < 1) {
    errors.displayOrder = "Thứ tự hiển thị phải là số nguyên từ 1 trở lên.";
  }
  if (!form.startAt) errors.startAt = "Vui lòng chọn thời điểm bắt đầu.";
  if (form.endAt && form.startAt && new Date(form.endAt).getTime() < new Date(form.startAt).getTime()) {
    errors.endAt = "Thời điểm kết thúc phải sau thời điểm bắt đầu.";
  }
  if (form.audienceType === "ROLE_CODES" && form.roleCodes.length === 0) {
    errors.roleCodes = "Vui lòng chọn ít nhất một role nhận thông báo.";
  }
  return errors;
}

function getDuplicateTitleError(error: unknown) {
  const message = error instanceof Error ? error.message : "";
  return message.toLowerCase().includes("title") && message.toLowerCase().includes("already exists")
    ? "Tiêu đề thông báo đã tồn tại. Vui lòng nhập tiêu đề khác."
    : message.includes("Tiêu đề thông báo đã tồn tại")
      ? "Tiêu đề thông báo đã tồn tại. Vui lòng nhập tiêu đề khác."
      : "";
}

function hasFieldErrors(errors: AnnouncementFieldErrors) {
  return Object.values(errors).some(Boolean);
}

function FieldError({ message }: { message?: string }) {
  if (!message) return null;

  return (
    <span className="tw-flex tw-items-center tw-gap-1.5 tw-text-[0.76rem] tw-font-bold tw-leading-snug tw-text-red-600">
      <i className="fas fa-exclamation-circle tw-text-[0.72rem]" />
      {message}
    </span>
  );
}

function AnnouncementActionMenu({
  announcement,
  canCancel,
  canCreate,
  canDelete,
  canPublish,
  onCancel,
  onCopy,
  onDelete,
  onPublish,
  onView,
}: {
  announcement: BroadcastAnnouncementResponse;
  canCancel: boolean;
  canCreate: boolean;
  canDelete: boolean;
  canPublish: boolean;
  onCancel: () => void;
  onCopy: () => void;
  onDelete: () => void;
  onPublish: () => void;
  onView: () => void;
}) {
  const [open, setOpen] = useState(false);
  const [menuPosition, setMenuPosition] = useState<ActionMenuPosition | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const editable = announcement.status === "DRAFT";

  useEffect(() => {
    if (!open) return undefined;

    const closeMenu = () => {
      setOpen(false);
      setMenuPosition(null);
    };

    const handlePointerDown = (event: MouseEvent) => {
      const target = event.target as Node;
      if (triggerRef.current?.contains(target) || menuRef.current?.contains(target)) return;
      closeMenu();
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") closeMenu();
    };

    window.addEventListener("mousedown", handlePointerDown);
    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("resize", closeMenu);
    window.addEventListener("scroll", closeMenu, true);

    return () => {
      window.removeEventListener("mousedown", handlePointerDown);
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("resize", closeMenu);
      window.removeEventListener("scroll", closeMenu, true);
    };
  }, [open]);

  const openActionMenu = () => {
    if (open) {
      setOpen(false);
      setMenuPosition(null);
      return;
    }

    const trigger = triggerRef.current;
    if (!trigger) return;

    const rect = trigger.getBoundingClientRect();
    const maxLeft = window.innerWidth - ACTION_MENU_WIDTH - 8;
    const shouldOpenAbove = rect.bottom + ACTION_MENU_GAP + ACTION_MENU_HEIGHT > window.innerHeight - 8;

    setMenuPosition({
      left: Math.max(8, Math.min(rect.right - ACTION_MENU_WIDTH, maxLeft)),
      top: shouldOpenAbove
        ? Math.max(8, rect.top - ACTION_MENU_HEIGHT - ACTION_MENU_GAP)
        : rect.bottom + ACTION_MENU_GAP,
    });
    setOpen(true);
  };

  const runAction = (action: () => void) => {
    setOpen(false);
    setMenuPosition(null);
    action();
  };

  const actionMenu = open && menuPosition
    ? createPortal(
        <div
          className="tw-fixed tw-z-[3200] tw-w-[184px] tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-py-1.5 tw-text-left tw-shadow-[0_18px_48px_rgba(15,23,42,0.2)]"
          ref={menuRef}
          style={{ left: menuPosition.left, top: menuPosition.top }}
        >
          <button
            className="tw-flex tw-min-h-9 tw-w-full tw-items-center tw-gap-2.5 tw-border-0 tw-bg-white tw-px-3 tw-text-left tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-700 tw-transition hover:tw-bg-vm-slate-25 hover:tw-text-vm-primary"
            type="button"
            onClick={() => runAction(onView)}
          >
            <i className="far fa-eye tw-w-4 tw-text-center tw-text-[0.82rem]" />
            <span>Xem</span>
          </button>
          {canCreate ? (
            <button
              className="tw-flex tw-min-h-9 tw-w-full tw-items-center tw-gap-2.5 tw-border-0 tw-bg-white tw-px-3 tw-text-left tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-700 tw-transition hover:tw-bg-vm-slate-25 hover:tw-text-vm-primary"
              type="button"
              onClick={() => runAction(onCopy)}
            >
              <i className="far fa-copy tw-w-4 tw-text-center tw-text-[0.82rem]" />
              <span>Sao chép</span>
            </button>
          ) : null}
          {editable && canPublish ? (
            <button
              className="tw-flex tw-min-h-9 tw-w-full tw-items-center tw-gap-2.5 tw-border-0 tw-bg-white tw-px-3 tw-text-left tw-text-[0.84rem] tw-font-bold tw-text-emerald-700 tw-transition hover:tw-bg-emerald-50"
              type="button"
              onClick={() => runAction(onPublish)}
            >
              <i className="far fa-paper-plane tw-w-4 tw-text-center tw-text-[0.82rem]" />
              <span>Xuất bản</span>
            </button>
          ) : null}
          {editable && canCancel ? (
            <button
              className="tw-flex tw-min-h-9 tw-w-full tw-items-center tw-gap-2.5 tw-border-0 tw-bg-white tw-px-3 tw-text-left tw-text-[0.84rem] tw-font-bold tw-text-amber-700 tw-transition hover:tw-bg-amber-50"
              type="button"
              onClick={() => runAction(onCancel)}
            >
              <i className="fas fa-ban tw-w-4 tw-text-center tw-text-[0.82rem]" />
              <span>Hủy</span>
            </button>
          ) : null}
          {announcement.status !== "PUBLISHED" && canDelete ? (
            <button
              className="tw-flex tw-min-h-9 tw-w-full tw-items-center tw-gap-2.5 tw-border-0 tw-bg-white tw-px-3 tw-text-left tw-text-[0.84rem] tw-font-bold tw-text-red-600 tw-transition hover:tw-bg-red-50"
              type="button"
              onClick={() => runAction(onDelete)}
            >
              <i className="far fa-trash-alt tw-w-4 tw-text-center tw-text-[0.82rem]" />
              <span>Xóa</span>
            </button>
          ) : null}
        </div>,
        document.body,
      )
    : null;

  return (
    <>
      <button
        aria-expanded={open}
        aria-label={`Mở menu thao tác ${announcement.title}`}
        className={cn(
          "tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-slate-600 tw-transition hover:tw-bg-vm-slate-50 hover:tw-text-vm-slate-950 focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus",
          open ? "tw-bg-vm-slate-50 tw-text-vm-primary" : "",
        )}
        ref={triggerRef}
        type="button"
        onClick={openActionMenu}
      >
        <i className="fas fa-ellipsis-v" />
      </button>
      {actionMenu}
    </>
  );
}

function AnnouncementTabs({
  counts,
  status,
  onChange,
}: {
  counts: Record<"all" | BroadcastAnnouncementStatus, number>;
  status: "all" | BroadcastAnnouncementStatus;
  onChange: (status: "all" | BroadcastAnnouncementStatus) => void;
}) {
  return (
    <div className="tw-flex tw-items-end tw-gap-8 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 max-[640px]:tw-gap-4">
      {statusTabs.map((tab) => {
        const active = status === tab.value;
        return (
          <button
            className={cn(
              "tw-relative tw-flex tw-h-11 tw-items-center tw-gap-2 tw-border-0 tw-bg-transparent tw-px-0 tw-text-[0.88rem] tw-font-black tw-text-vm-slate-600 tw-transition hover:tw-text-vm-primary",
              active ? "tw-text-emerald-600" : "",
            )}
            key={tab.value}
            type="button"
            onClick={() => onChange(tab.value)}
          >
            <span>{tab.label}</span>
            <span
              className={cn(
                "tw-inline-flex tw-min-w-7 tw-items-center tw-justify-center tw-rounded-full tw-px-2 tw-py-0.5 tw-text-[0.72rem] tw-font-black",
                tab.value === "all" ? "tw-bg-blue-100 tw-text-blue-700" : "",
                tab.value === "DRAFT" ? "tw-bg-vm-slate-100 tw-text-vm-slate-600" : "",
                tab.value === "PUBLISHED" ? "tw-bg-emerald-100 tw-text-emerald-700" : "",
                tab.value === "CANCELLED" ? "tw-bg-red-100 tw-text-red-600" : "",
              )}
            >
              {counts[tab.value].toLocaleString("vi-VN")}
            </span>
            {active ? <span className="tw-absolute tw-inset-x-0 tw-bottom-[-1px] tw-h-[2px] tw-rounded-full tw-bg-emerald-500" /> : null}
          </button>
        );
      })}
    </div>
  );
}

function AnnouncementDrawer({
  announcement,
  canPublish,
  canSave,
  copiedInitialForm,
  duplicateTitleExists,
  onClose,
  onPublish,
  onSaveDraft,
  onUpdateDisplayOrder,
  open,
  roleLookupWarning,
  roleOptions,
  saving,
  titleChangeRequired,
}: {
  announcement: BroadcastAnnouncementResponse | null;
  canPublish: boolean;
  canSave: boolean;
  copiedInitialForm: AnnouncementFormState | null;
  duplicateTitleExists: (title: string) => boolean;
  onClose: () => void;
  onPublish: (payload: BroadcastAnnouncementPayload, publishTiming: AnnouncementFormState["publishTiming"]) => Promise<void>;
  onSaveDraft: (payload: BroadcastAnnouncementPayload) => Promise<void>;
  onUpdateDisplayOrder: (broadcastId: string, displayOrder: number) => Promise<void>;
  open: boolean;
  roleLookupWarning: string;
  roleOptions: RoleOption[];
  saving: boolean;
  titleChangeRequired: boolean;
}) {
  const [form, setForm] = useState<AnnouncementFormState>(() => copiedInitialForm ?? getInitialForm(announcement));
  const [fieldErrors, setFieldErrors] = useState<AnnouncementFieldErrors>({});
  const readOnly = Boolean(announcement && announcement.status !== "DRAFT") || !canSave;
  const canEditPublishedDisplayOrder = Boolean(announcement && announcement.status === "PUBLISHED" && canSave);
  const displayOrderReadOnly = readOnly && !canEditPublishedDisplayOrder;
  const typeMeta = getTypeMeta(form.notificationType);
  const characterCount = form.message.length;

  useEffect(() => {
    setForm(copiedInitialForm ?? getInitialForm(announcement));
    setFieldErrors(titleChangeRequired ? { title: "Vui lòng thay đổi tiêu đề thông báo sau khi sao chép." } : {});
  }, [announcement, copiedInitialForm, open, titleChangeRequired]);

  function clearFieldError(field: keyof AnnouncementFieldErrors) {
    setFieldErrors((current) => {
      if (!current[field]) return current;
      return { ...current, [field]: undefined };
    });
  }

  async function submit(intent: "draft" | "publish") {
    if (readOnly) return;
    const errors = validateForm(form, {
      initialTitle: copiedInitialForm?.title ?? announcement?.title ?? "",
      required: titleChangeRequired,
    }, duplicateTitleExists);
    if (hasFieldErrors(errors)) {
      setFieldErrors(errors);
      return;
    }

    setFieldErrors({});
    const payload = normalizePayload(form);
    try {
      if (intent === "publish") {
        await onPublish(payload, form.publishTiming);
        return;
      }
      await onSaveDraft(payload);
    } catch (error) {
      const duplicateTitleError = getDuplicateTitleError(error);
      if (duplicateTitleError) {
        setFieldErrors((current) => ({ ...current, title: duplicateTitleError }));
      }
    }
  }

  async function submitDisplayOrder() {
    if (!announcement || !canEditPublishedDisplayOrder) return;
    if (!Number.isInteger(Number(form.displayOrder)) || Number(form.displayOrder) < 1) {
      setFieldErrors({ displayOrder: "Thứ tự hiển thị phải là số nguyên từ 1 trở lên." });
      return;
    }

    setFieldErrors({});
    try {
      await onUpdateDisplayOrder(announcement.broadcastId, Math.max(1, Number(form.displayOrder) || 100));
    } catch {
      return;
    }
  }

  return (
    <Drawer
      actions={
        canEditPublishedDisplayOrder ? (
          <div className="tw-grid tw-grid-cols-2 tw-gap-3">
            <Button className="tw-h-12" disabled={saving} variant="secondary" onClick={onClose}>
              Đóng
            </Button>
            <Button className="tw-h-12" loading={saving} onClick={() => void submitDisplayOrder()}>
              Lưu thứ tự
            </Button>
          </div>
        ) : readOnly ? (
          <Button className="tw-h-12 tw-w-full" disabled={saving} variant="secondary" onClick={onClose}>
            Đóng
          </Button>
        ) : (
          <div className="tw-grid tw-grid-cols-2 tw-gap-3">
            <Button className="tw-h-12" disabled={saving} variant="secondary" onClick={() => void submit("draft")}>
              Lưu nháp
            </Button>
            <Button className="tw-h-12" disabled={!canPublish} loading={saving} onClick={() => void submit("publish")}>
              Xuất bản
            </Button>
          </div>
        )
      }
      onClose={onClose}
      open={open}
      title={announcement ? "Chi tiết thông báo" : "Tạo thông báo"}
      width="lg"
    >
      <div className="tw-grid tw-gap-6">
        <section className="tw-grid tw-gap-3">
          <label className="tw-grid tw-gap-2">
            <span className={cn("tw-text-[0.82rem] tw-font-black", fieldErrors.title ? "tw-text-red-600" : "tw-text-vm-slate-900")}>Tiêu đề thông báo</span>
            <input
              className={cn(inputClassName, fieldErrors.title ? "tw-border-red-300 tw-shadow-[0_0_0_3px_rgba(239,68,68,0.12)] focus:tw-border-red-300 focus:tw-shadow-[0_0_0_3px_rgba(239,68,68,0.12)]" : "")}
              disabled={readOnly}
              maxLength={200}
              placeholder="Nhập tiêu đề hiển thị trên chuông thông báo"
              value={form.title}
              onChange={(event) => {
                setForm((current) => ({ ...current, title: event.target.value }));
                clearFieldError("title");
              }}
            />
            <FieldError message={fieldErrors.title} />
          </label>
        </section>

        <section className="tw-grid tw-gap-3">
          <h4 className="tw-m-0 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900">Đối tượng nhận</h4>
          <label className="tw-flex tw-items-center tw-gap-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-800">
            <input
              checked={form.audienceType === "ALL_ACTIVE_ACCOUNTS"}
              disabled={readOnly}
              name="audienceType"
              type="radio"
              onChange={() => {
                setForm((current) => ({ ...current, audienceType: "ALL_ACTIVE_ACCOUNTS", roleCodes: [] }));
                clearFieldError("roleCodes");
              }}
            />
            <span>Tất cả tài khoản đang hoạt động</span>
          </label>
          <div className="tw-grid tw-gap-2">
            <div className="tw-flex tw-items-center tw-justify-between tw-gap-2">
              <span className="tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-700">Hoặc chọn theo vai trò</span>
              <button
                className="tw-border-0 tw-bg-transparent tw-text-[0.75rem] tw-font-bold tw-text-vm-primary"
                disabled={readOnly}
                type="button"
                onClick={() => setForm((current) => ({ ...current, audienceType: "ROLE_CODES" }))}
              >
                Chọn role
              </button>
            </div>
            <div className={cn(
              "tw-flex tw-min-h-[72px] tw-flex-wrap tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-bg-white tw-p-3",
              fieldErrors.roleCodes ? "tw-border-red-300 tw-shadow-[0_0_0_3px_rgba(239,68,68,0.12)]" : "tw-border-vm-slate-100",
            )}>
              {roleOptions.map((role) => {
                const selected = form.roleCodes.includes(role.code);
                return (
                  <button
                    className={cn(
                      "tw-inline-flex tw-h-8 tw-items-center tw-gap-2 tw-rounded-vm-sm tw-border tw-border-solid tw-px-2.5 tw-text-[0.78rem] tw-font-black tw-transition",
                      selected ? "tw-border-vm-primary tw-bg-brand-50 tw-text-vm-primary" : "tw-border-transparent tw-bg-vm-slate-50 tw-text-vm-slate-700",
                    )}
                    disabled={readOnly}
                    key={role.code}
                    title={role.label}
                    type="button"
                    onClick={() => {
                      setForm((current) => {
                        const nextRoles = selected
                          ? current.roleCodes.filter((item) => item !== role.code)
                          : [...current.roleCodes, role.code];
                        return { ...current, audienceType: "ROLE_CODES", roleCodes: nextRoles };
                      });
                      if (!selected) clearFieldError("roleCodes");
                    }}
                  >
                    <span>{role.code}</span>
                    {selected ? <i className="fas fa-times tw-text-[0.68rem]" /> : null}
                  </button>
                );
              })}
            </div>
            <FieldError message={fieldErrors.roleCodes} />
            {roleLookupWarning ? (
              <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-100 tw-bg-amber-50 tw-p-3 tw-text-[0.78rem] tw-font-semibold tw-leading-5 tw-text-amber-700">
                {roleLookupWarning}
              </div>
            ) : null}
          </div>
        </section>

        <section className="tw-grid tw-gap-3">
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.82rem] tw-font-black tw-text-vm-slate-900">Loại thông báo</span>
            <SelectMenu
              ariaLabel="Loại thông báo"
              disabled={readOnly}
              menuClassName="tw-max-h-72"
              options={notificationTypeOptions.map((option) => ({ label: option.label, value: option.value }))}
              portal
              searchable
              searchPlaceholder="Tìm loại thông báo..."
              value={form.notificationType}
              onChange={(value) => setForm((current) => ({ ...current, notificationType: value as NotificationType }))}
            />
          </label>
        </section>

        <section className="tw-grid tw-gap-2">
          <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
            <h4 className={cn("tw-m-0 tw-text-[0.9rem] tw-font-black", fieldErrors.message ? "tw-text-red-600" : "tw-text-vm-slate-900")}>Nội dung thông báo</h4>
            <span className="tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">{characterCount}/1000</span>
          </div>
          <div className={cn(
            "tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-bg-white",
            fieldErrors.message ? "tw-border-red-300 tw-shadow-[0_0_0_3px_rgba(239,68,68,0.12)]" : "tw-border-vm-slate-100",
          )}>
            <textarea
              className="tw-min-h-[168px] tw-w-full tw-resize-none tw-border-0 tw-bg-transparent tw-p-3 tw-text-[0.86rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-900 tw-outline-none placeholder:tw-text-vm-slate-400 disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25"
              disabled={readOnly}
              maxLength={1000}
              placeholder="Nhập nội dung gửi tới người nhận..."
              value={form.message}
              onChange={(event) => {
                setForm((current) => ({ ...current, message: event.target.value }));
                clearFieldError("message");
              }}
            />
            <div className="tw-flex tw-h-10 tw-items-center tw-gap-1 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-px-2">
              {["fas fa-paperclip", "far fa-image", "far fa-smile"].map((icon) => (
                <button
                  className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-sm tw-border-0 tw-bg-transparent tw-text-vm-slate-500 hover:tw-bg-vm-slate-50 hover:tw-text-vm-slate-900"
                  disabled
                  key={icon}
                  type="button"
                >
                  <i className={icon} />
                </button>
              ))}
            </div>
          </div>
          <FieldError message={fieldErrors.message} />
        </section>

        <section className="tw-grid tw-gap-3">
          <h4 className="tw-m-0 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900">Xuất bản</h4>
          <div className="tw-grid tw-grid-cols-2 tw-gap-3">
            <label
              className={cn(
                "tw-flex tw-min-h-[46px] tw-cursor-pointer tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-px-3 tw-text-[0.86rem] tw-font-black tw-transition",
                form.publishTiming === "now"
                  ? "tw-border-brand-200 tw-bg-brand-50 tw-text-vm-primary tw-shadow-[0_0_0_3px_rgba(37,99,235,0.06)]"
                  : "tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-border-vm-slate-200",
                readOnly ? "tw-cursor-not-allowed tw-opacity-70" : "",
              )}
            >
              <input
                checked={form.publishTiming === "now"}
                className="tw-accent-vm-primary"
                disabled={readOnly}
                name="publishTiming"
                type="radio"
                onChange={() => setForm((current) => ({ ...current, publishTiming: "now", startAt: nowLocalDateTime() }))}
              />
              <span>Xuất bản ngay</span>
            </label>
            <label
              className={cn(
                "tw-flex tw-min-h-[46px] tw-cursor-pointer tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-px-3 tw-text-[0.86rem] tw-font-black tw-transition",
                form.publishTiming === "scheduled"
                  ? "tw-border-brand-200 tw-bg-brand-50 tw-text-vm-primary tw-shadow-[0_0_0_3px_rgba(37,99,235,0.06)]"
                  : "tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-border-vm-slate-200",
                readOnly ? "tw-cursor-not-allowed tw-opacity-70" : "",
              )}
            >
              <input
                checked={form.publishTiming === "scheduled"}
                className="tw-accent-vm-primary"
                disabled={readOnly}
                name="publishTiming"
                type="radio"
                onChange={() => setForm((current) => ({ ...current, publishTiming: "scheduled" }))}
              />
              <span>Lên lịch xuất bản</span>
            </label>
          </div>
          <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[560px]:tw-grid-cols-1">
            <DateTimeScheduleField
              disabled={readOnly || form.publishTiming === "now"}
              error={fieldErrors.startAt}
              label="Bắt đầu hiển thị"
              value={form.startAt}
              onChange={(value) => {
                setForm((current) => ({ ...current, startAt: value }));
                clearFieldError("startAt");
              }}
            />
            <DateTimeScheduleField
              allowClear
              disabled={readOnly}
              error={fieldErrors.endAt}
              fallbackValue={form.startAt}
              label="Kết thúc hiển thị"
              menuAlign="right"
              value={form.endAt}
              onChange={(value) => {
                setForm((current) => ({ ...current, endAt: value }));
                clearFieldError("endAt");
              }}
            />
          </div>
          <label className="tw-grid tw-gap-2">
            <span className={cn("tw-text-[0.78rem] tw-font-bold", fieldErrors.displayOrder ? "tw-text-red-600" : "tw-text-vm-slate-700")}>Thứ tự chạy trên thanh thông báo</span>
            <div
              className={cn(
                "tw-grid tw-h-[46px] tw-grid-cols-[38px_minmax(0,1fr)] tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-bg-white tw-px-3 tw-shadow-[0_4px_12px_rgba(15,23,42,0.035)] focus-within:tw-border-brand-200 focus-within:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)]",
                fieldErrors.displayOrder ? "tw-border-red-300 tw-shadow-[0_0_0_3px_rgba(239,68,68,0.12)]" : "tw-border-vm-slate-100",
              )}
            >
              <span className="tw-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-sm tw-bg-brand-50 tw-text-vm-primary">
                <i className="fas fa-sort-numeric-down" />
              </span>
              <input
                className="tw-h-full tw-min-w-0 tw-border-0 tw-bg-transparent tw-text-[0.88rem] tw-font-black tw-text-vm-slate-900 tw-outline-none placeholder:tw-text-vm-slate-400 disabled:tw-cursor-not-allowed disabled:tw-text-vm-slate-500"
                disabled={displayOrderReadOnly}
                min={1}
                placeholder="Số nhỏ hiển thị trước"
                type="number"
                value={form.displayOrder}
                onChange={(event) => {
                  setForm((current) => ({ ...current, displayOrder: event.target.value }));
                  clearFieldError("displayOrder");
                }}
              />
            </div>
            <FieldError message={fieldErrors.displayOrder} />
            {canEditPublishedDisplayOrder ? (
              <span className="tw-text-[0.74rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">
                Thông báo đã phát chỉ cho phép đổi thứ tự chạy, các nội dung khác đang được khóa.
              </span>
            ) : null}
          </label>
          <div className="tw-grid tw-grid-cols-1">
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-700">Màn hình mở khi nhấn</span>
              <SelectMenu
                ariaLabel="Màn hình mở khi nhấn thông báo"
                disabled={readOnly}
                options={getRedirectOptions(form.redirectUrl)}
                portal
                value={form.redirectUrl || "none"}
                onChange={(value) => setForm((current) => ({ ...current, redirectUrl: value === "none" ? "" : value }))}
              />
            </label>
          </div>
        </section>

        <section className="tw-grid tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50 tw-p-3">
          <div className="tw-flex tw-items-center tw-gap-2 tw-text-vm-primary">
            <i className="fas fa-info-circle" />
            <span className="tw-text-[0.84rem] tw-font-black">Số người nhận dự kiến</span>
            <strong className="tw-ml-auto tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900">{getExpectedRecipientText(form)}</strong>
          </div>
        </section>

        <section className="tw-grid tw-grid-cols-[42px_minmax(0,1fr)] tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3">
          <span className="tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-full tw-bg-white tw-text-vm-primary">
            <i className={typeMeta.icon} />
          </span>
          <span className="tw-min-w-0">
            <strong className="tw-flex tw-items-start tw-gap-2 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900">
              <span className="tw-min-w-0 tw-flex-1 tw-truncate">{form.title || "Tiêu đề thông báo"}</span>
              <span className="tw-mt-1 tw-h-2 tw-w-2 tw-flex-none tw-rounded-full tw-bg-blue-600" />
            </strong>
            <small className="tw-mt-1 tw-line-clamp-2 tw-text-[0.78rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">
              {form.message || "Nội dung sẽ hiển thị trong menu thông báo và toast realtime."}
            </small>
          </span>
        </section>

      </div>
    </Drawer>
  );
}

export function AnnouncementManagementPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [announcements, setAnnouncements] = useState<BroadcastAnnouncementResponse[]>([]);
  const [activeRoleOptions, setActiveRoleOptions] = useState<RoleOption[]>(fallbackRoleOptions);
  const [audienceFilter, setAudienceFilter] = useState("all");
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingAnnouncement, setEditingAnnouncement] = useState<BroadcastAnnouncementResponse | null>(null);
  const [keyword, setKeyword] = useState("");
  const [loading, setLoading] = useState(true);
  const [pageSize, setPageSize] = useState(8);
  const [currentPage, setCurrentPage] = useState(1);
  const [saving, setSaving] = useState(false);
  const [statusFilter, setStatusFilter] = useState<"all" | BroadcastAnnouncementStatus>("all");
  const [typeFilter, setTypeFilter] = useState("all");
  const [roleLookupWarning, setRoleLookupWarning] = useState("");
  const [pendingAction, setPendingAction] = useState<PendingAction>(null);
  const [copiedInitialForm, setCopiedInitialForm] = useState<AnnouncementFormState | null>(null);

  const canCreate = hasAnyPermission(user, ["BROADCAST_NOTIFICATION_CREATE_ALL"]);
  const canUpdate = hasAnyPermission(user, ["BROADCAST_NOTIFICATION_UPDATE_ALL"]);
  const canPublish = hasAnyPermission(user, ["BROADCAST_NOTIFICATION_PUBLISH_ALL"]);
  const canCancel = hasAnyPermission(user, ["BROADCAST_NOTIFICATION_CANCEL_ALL"]);
  const canDelete = hasAnyPermission(user, ["BROADCAST_NOTIFICATION_DELETE_ALL"]);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [announcementResponse, roleResponse] = await Promise.allSettled([
        getBroadcastAnnouncements(),
        getNotificationActiveRoles(),
      ]);

      if (announcementResponse.status === "fulfilled") {
        setAnnouncements(announcementResponse.value.data ?? []);
      } else {
        throw announcementResponse.reason;
      }

      if (roleResponse.status === "fulfilled") {
        const nextRoleOptions = (roleResponse.value.data ?? [])
          .map(mapRoleToOption)
          .filter((role): role is RoleOption => Boolean(role));
        setActiveRoleOptions(nextRoleOptions.length ? nextRoleOptions : fallbackRoleOptions);
        setRoleLookupWarning(nextRoleOptions.length ? "" : "Không có role active nào từ backend, hệ thống đang dùng danh sách role mặc định.");
      } else {
        setActiveRoleOptions(fallbackRoleOptions);
        setRoleLookupWarning("Không tải được danh sách role active từ backend. Tài khoản cần quyền announcement để dùng lookup role của màn thông báo.");
      }
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể tải danh sách thông báo.", "Tải dữ liệu thất bại");
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const counts = useMemo<Record<"all" | BroadcastAnnouncementStatus, number>>(
    () => ({
      all: announcements.length,
      CANCELLED: announcements.filter((item) => item.status === "CANCELLED").length,
      DRAFT: announcements.filter((item) => item.status === "DRAFT").length,
      PUBLISHED: announcements.filter((item) => item.status === "PUBLISHED").length,
    }),
    [announcements],
  );

  const filteredAnnouncements = useMemo(() => {
    const search = keyword.trim().toLowerCase();

    return announcements.filter((item) => {
      const audienceText = getAudienceLabel(item).toLowerCase();
      const matchesStatus = statusFilter === "all" || item.status === statusFilter;
      const matchesType = typeFilter === "all" || item.notificationType === typeFilter;
      const matchesAudience =
        audienceFilter === "all" ||
        item.audienceType === audienceFilter ||
        (item.roleCodes ?? []).includes(audienceFilter);
      const matchesSearch =
        !search ||
        [item.title, item.message, getTypeMeta(item.notificationType).label, audienceText]
          .some((value) => String(value ?? "").toLowerCase().includes(search));

      return matchesStatus && matchesType && matchesAudience && matchesSearch;
    });
  }, [announcements, audienceFilter, keyword, statusFilter, typeFilter]);

  const audienceSelectOptions = useMemo(() => buildAudienceOptions(activeRoleOptions), [activeRoleOptions]);

  const page = getPageItems(filteredAnnouncements, currentPage, pageSize);

  const duplicateTitleExists = useCallback(
    (title: string) => {
      const normalizedTitle = title.trim().toLowerCase();
      if (!normalizedTitle) return false;

      return announcements.some((item) => {
        if (editingAnnouncement?.broadcastId && item.broadcastId === editingAnnouncement.broadcastId) return false;
        return item.title.trim().toLowerCase() === normalizedTitle;
      });
    },
    [announcements, editingAnnouncement?.broadcastId],
  );

  useEffect(() => {
    if (currentPage !== page.safeCurrentPage) {
      setCurrentPage(page.safeCurrentPage);
    }
  }, [currentPage, page.safeCurrentPage]);

  function resetPage() {
    setCurrentPage(1);
  }

  async function saveDraft(payload: BroadcastAnnouncementPayload) {
    setSaving(true);
    try {
      const response = editingAnnouncement
        ? await updateBroadcastAnnouncement(editingAnnouncement.broadcastId, payload)
        : await createBroadcastAnnouncement(payload);
      toast.success(response.message || "Đã lưu bản nháp thông báo.", "Lưu thành công");
      setDrawerOpen(false);
      setEditingAnnouncement(null);
      setCopiedInitialForm(null);
      await loadData();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể lưu thông báo.", "Lưu thất bại");
      throw error;
    } finally {
      setSaving(false);
    }
  }

  async function updateDisplayOrder(broadcastId: string, displayOrder: number) {
    setSaving(true);
    try {
      const response = await updateBroadcastAnnouncementDisplayOrder(broadcastId, displayOrder);
      toast.success(response.message || "Đã cập nhật thứ tự chạy trên thanh thông báo.", "Cập nhật thành công");
      setDrawerOpen(false);
      setEditingAnnouncement(null);
      setCopiedInitialForm(null);
      await loadData();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể cập nhật thứ tự chạy.", "Cập nhật thất bại");
      throw error;
    } finally {
      setSaving(false);
    }
  }

  async function publishFromDrawer(payload: BroadcastAnnouncementPayload, publishTiming: AnnouncementFormState["publishTiming"]) {
    setSaving(true);
    try {
      const saved = editingAnnouncement
        ? await updateBroadcastAnnouncement(editingAnnouncement.broadcastId, payload)
        : await createBroadcastAnnouncement(payload);
      if (publishTiming === "scheduled" && new Date(payload.startAt).getTime() > Date.now()) {
        toast.info("Backend hiện lưu thời điểm phát trên bản nháp; scheduler tự phát chưa có endpoint riêng.", "Đã lưu lịch dự kiến");
      } else {
        await publishBroadcastAnnouncement(saved.data.broadcastId);
        toast.success("Thông báo đã được phát tới các tài khoản ACTIVE thuộc đối tượng nhận.", "Đã xuất bản");
      }
      setDrawerOpen(false);
      setEditingAnnouncement(null);
      setCopiedInitialForm(null);
      await loadData();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể xuất bản thông báo.", "Xuất bản thất bại");
      throw error;
    } finally {
      setSaving(false);
    }
  }

  function copyAnnouncement(announcement: BroadcastAnnouncementResponse) {
    if (!canCreate) return;
    setEditingAnnouncement(null);
    setCopiedInitialForm(buildCopyForm(announcement));
    setDrawerOpen(true);
  }

  async function handleConfirmAction() {
    if (!pendingAction) return;
    setSaving(true);
    try {
      if (pendingAction.action === "publish") {
        await publishBroadcastAnnouncement(pendingAction.announcement.broadcastId);
        toast.success("Thông báo đã được phát realtime tới người nhận.", "Đã xuất bản");
      }
      if (pendingAction.action === "cancel") {
        await cancelBroadcastAnnouncement(pendingAction.announcement.broadcastId);
        toast.success("Thông báo đã được chuyển sang trạng thái đã hủy.", "Đã hủy");
      }
      if (pendingAction.action === "delete") {
        await deleteBroadcastAnnouncement(pendingAction.announcement.broadcastId);
        toast.success("Thông báo đã được xóa khỏi hệ thống.", "Đã xóa");
      }
      setPendingAction(null);
      await loadData();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể thực hiện thao tác.", "Thao tác thất bại");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="tw-px-4 tw-py-4 lg:tw-px-5">
      <section className="tw-mx-auto tw-min-h-[calc(100vh-104px)] tw-w-[min(100%,1560px)]">
        <header className="tw-flex tw-items-center tw-justify-between tw-gap-4 max-[720px]:tw-flex-col max-[720px]:tw-items-stretch">
          <h1 className="tw-m-0 tw-text-[1.72rem] tw-font-black tw-tracking-normal tw-text-vm-slate-950 max-[640px]:tw-text-[1.35rem]">
            Quản lý thông báo
          </h1>
          <div className="tw-flex tw-gap-2">
            <Button disabled={loading} variant="secondary" onClick={() => void loadData()}>
              <i className="fas fa-sync-alt" />
              Làm mới
            </Button>
            {canCreate ? (
              <Button
                onClick={() => {
                  setEditingAnnouncement(null);
                  setCopiedInitialForm(null);
                  setDrawerOpen(true);
                }}
              >
                <i className="fas fa-plus" />
                Tạo thông báo
              </Button>
            ) : null}
          </div>
        </header>

        <div className="tw-mt-5">
          <AnnouncementTabs
            counts={counts}
            status={statusFilter}
            onChange={(value) => {
              setStatusFilter(value);
              resetPage();
            }}
          />
        </div>

        <main className="tw-mt-5 tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-vm-card">
          <div className="tw-grid tw-grid-cols-[minmax(260px,1fr)_180px_180px_42px] tw-gap-2 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3 max-[1120px]:tw-grid-cols-2 max-[640px]:tw-grid-cols-1">
            <label className="tw-flex tw-h-[42px] tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3">
              <i className="fas fa-search tw-text-vm-slate-500" />
              <input
                className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none placeholder:tw-text-vm-slate-400"
                placeholder="Tìm kiếm tiêu đề, nội dung..."
                value={keyword}
                onChange={(event) => {
                  setKeyword(event.target.value);
                  resetPage();
                }}
              />
            </label>
            <SelectMenu
              ariaLabel="Loại thông báo"
              options={typeOptions}
              portal
              searchable
              searchPlaceholder="Tìm loại thông báo..."
              value={typeFilter}
              onChange={(value) => {
                setTypeFilter(value);
                resetPage();
              }}
            />
            <SelectMenu
              ariaLabel="Đối tượng nhận"
              options={audienceSelectOptions}
              portal
              value={audienceFilter}
              onChange={(value) => {
                setAudienceFilter(value);
                resetPage();
              }}
            />
            <button
              aria-label="Lọc theo ngày"
              className="tw-inline-flex tw-h-[42px] tw-w-[42px] tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-600 hover:tw-border-vm-slate-200 hover:tw-bg-vm-slate-25 max-[640px]:tw-w-full"
              disabled
              title="Bộ lọc ngày sẽ dùng khi backend hỗ trợ query theo ngày."
              type="button"
            >
              <i className="far fa-calendar" />
            </button>
          </div>

          <div className="tw-overflow-visible">
            <table className="table tw-m-0 tw-w-full tw-table-fixed [&_td]:tw-min-w-0 [&_td]:tw-border-0 [&_td]:tw-border-t [&_td]:tw-border-solid [&_td]:tw-border-vm-slate-100 [&_td]:tw-px-3 [&_td]:tw-py-3 [&_td]:tw-align-middle [&_thead_th]:tw-border-0 [&_thead_th]:tw-bg-vm-slate-25 [&_thead_th]:tw-px-3 [&_thead_th]:tw-py-3.5 [&_thead_th]:tw-text-left [&_thead_th]:tw-text-[0.74rem] [&_thead_th]:tw-font-black [&_thead_th]:tw-leading-4 [&_thead_th]:tw-text-vm-slate-700">
              <colgroup>
                <col className="tw-w-[3.5%]" />
                <col className="tw-w-[22%]" />
                <col className="tw-w-[11%]" />
                <col className="tw-w-[15%]" />
                <col className="tw-w-[15%]" />
                <col className="tw-w-[9%]" />
                <col className="tw-w-[8%]" />
                <col className="tw-w-[11.5%]" />
                <col className="tw-w-[5%]" />
              </colgroup>
              <thead>
                <tr>
                  <th>
                    <input aria-label="Chọn tất cả thông báo" type="checkbox" disabled />
                  </th>
                  <th>Tiêu đề</th>
                  <th>Loại thông báo</th>
                  <th>Đối tượng nhận</th>
                  <th>Số người nhận dự kiến</th>
                  <th>Trạng thái</th>
                  <th>Thứ tự chạy</th>
                  <th>Ngày tạo/đăng</th>
                  <th className="tw-pr-6" />
                </tr>
              </thead>
              <tbody>
                {page.rows.map((announcement) => {
                  const typeMeta = getTypeMeta(announcement.notificationType);
                  const statusDate = getStatusDate(announcement);
                  return (
                    <tr className="tw-transition hover:tw-bg-vm-slate-25" key={announcement.broadcastId}>
                      <td>
                        <input aria-label={`Chọn ${announcement.title}`} type="checkbox" />
                      </td>
                      <td>
                        <button
                          className="tw-w-full tw-min-w-0 tw-border-0 tw-bg-transparent tw-p-0 tw-text-left"
                          type="button"
                          onClick={() => {
                            setEditingAnnouncement(announcement);
                            setCopiedInitialForm(null);
                            setDrawerOpen(true);
                          }}
                        >
                          <strong className="tw-block tw-line-clamp-2 tw-text-[0.84rem] tw-font-black tw-leading-5 tw-text-vm-slate-950">
                            {announcement.title}
                          </strong>
                          <small className="tw-mt-1 tw-block tw-line-clamp-2 tw-text-[0.74rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">
                            {announcement.message}
                          </small>
                        </button>
                      </td>
                      <td>
                        <Badge tone={typeMeta.tone} className="tw-rounded-vm-sm tw-px-2.5">
                          {typeMeta.badge}
                        </Badge>
                      </td>
                      <td className="tw-text-[0.8rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-800">
                        <span className="tw-block tw-line-clamp-2 tw-break-words">{getAudienceLabel(announcement)}</span>
                      </td>
                      <td className="tw-text-[0.8rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-600">
                        <span className="tw-block tw-line-clamp-2 tw-break-words">{getExpectedRecipientText(announcement)}</span>
                      </td>
                      <td>
                        <Badge tone={getStatusTone(announcement.status)} className="tw-gap-2 tw-rounded-vm-sm tw-px-2.5">
                          <span className="tw-h-1.5 tw-w-1.5 tw-rounded-full tw-bg-current" />
                          {getStatusLabel(announcement.status)}
                        </Badge>
                      </td>
                      <td>
                        <span className="tw-inline-flex tw-h-8 tw-min-w-10 tw-items-center tw-justify-center tw-rounded-full tw-bg-sky-50 tw-px-2.5 tw-text-[0.78rem] tw-font-black tw-text-sky-700 tw-ring-1 tw-ring-sky-100">
                          #{announcement.displayOrder ?? 100}
                        </span>
                      </td>
                      <td className="tw-text-[0.78rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-600">
                        <strong className="tw-block tw-text-vm-slate-900">{formatDate(statusDate)} {formatTime(statusDate)}</strong>
                        <span>{getAuthorLabel(announcement.createdBy)}</span>
                      </td>
                      <td className="tw-pr-6 tw-text-right">
                        <AnnouncementActionMenu
                          announcement={announcement}
                          canCancel={canCancel}
                          canCreate={canCreate}
                          canDelete={canDelete}
                          canPublish={canPublish}
                          onCancel={() => setPendingAction({ action: "cancel", announcement })}
                          onCopy={() => copyAnnouncement(announcement)}
                          onDelete={() => setPendingAction({ action: "delete", announcement })}
                          onPublish={() => setPendingAction({ action: "publish", announcement })}
                          onView={() => {
                            setEditingAnnouncement(announcement);
                            setCopiedInitialForm(null);
                            setDrawerOpen(true);
                          }}
                        />
                      </td>
                    </tr>
                  );
                })}
                {page.rows.length === 0 ? (
                  <tr>
                    <td className="tw-py-12 tw-text-center" colSpan={9}>
                      <span className="tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-full tw-bg-vm-slate-50 tw-text-vm-slate-400">
                        <i className={loading ? "fas fa-spinner fa-spin" : "far fa-bell"} />
                      </span>
                      <p className="tw-mb-0 tw-mt-3 tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-500">
                        {loading ? "Đang tải thông báo..." : "Chưa có thông báo phù hợp."}
                      </p>
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>

          <PaginationFooter
            ariaLabel="Phân trang thông báo"
            currentPage={page.safeCurrentPage}
            endIndex={page.endIndex}
            onPageChange={setCurrentPage}
            onPageSizeChange={(value) => {
              setPageSize(value);
              setCurrentPage(1);
            }}
            pageSize={pageSize}
            pageSizeOptions={pageSizeOptions}
            startIndex={page.startIndex}
            totalPages={page.totalPages}
            totalRecords={filteredAnnouncements.length}
          />
        </main>
      </section>

      <AnnouncementDrawer
        announcement={editingAnnouncement}
        canPublish={canPublish}
        canSave={editingAnnouncement ? canUpdate : canCreate}
        copiedInitialForm={copiedInitialForm}
        duplicateTitleExists={duplicateTitleExists}
        onClose={() => {
          if (saving) return;
          setDrawerOpen(false);
          setEditingAnnouncement(null);
          setCopiedInitialForm(null);
        }}
        onPublish={publishFromDrawer}
        onSaveDraft={saveDraft}
        onUpdateDisplayOrder={updateDisplayOrder}
        open={drawerOpen}
        roleLookupWarning={roleLookupWarning}
        roleOptions={activeRoleOptions}
        saving={saving}
        titleChangeRequired={Boolean(copiedInitialForm)}
      />

      <Modal
        actions={
          <div className="tw-grid tw-grid-cols-2 tw-gap-2">
            <Button disabled={saving} variant="secondary" onClick={() => setPendingAction(null)}>
              Đóng
            </Button>
            <Button loading={saving} variant={pendingAction?.action === "delete" ? "danger" : "primary"} onClick={() => void handleConfirmAction()}>
              {pendingAction?.action === "publish" ? "Xuất bản" : pendingAction?.action === "cancel" ? "Hủy" : "Xóa"}
            </Button>
          </div>
        }
        description="Thao tác sẽ gọi trực tiếp API announcements của backend và áp dụng rule trạng thái hiện có."
        onClose={() => {
          if (!saving) setPendingAction(null);
        }}
        open={Boolean(pendingAction)}
        title="Xác nhận thao tác"
      >
        <p className="tw-m-0 tw-text-[0.9rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-600">
          Bạn muốn {pendingAction?.action === "publish" ? "xuất bản" : pendingAction?.action === "cancel" ? "hủy" : "xóa"} thông báo
          <strong className="tw-text-vm-slate-900"> {pendingAction?.announcement.title}</strong>?
        </p>
      </Modal>
    </div>
  );
}
