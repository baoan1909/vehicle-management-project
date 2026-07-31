import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { Badge, Button, Card, Drawer, Modal, PaginationFooter, SelectMenu, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import {
  cancelBroadcastAnnouncement,
  createBroadcastAnnouncement,
  deleteBroadcastAnnouncement,
  getBroadcastAnnouncements,
  publishBroadcastAnnouncement,
  updateBroadcastAnnouncement,
  type BroadcastAnnouncementAudienceType,
  type BroadcastAnnouncementPayload,
  type BroadcastAnnouncementResponse,
  type BroadcastAnnouncementStatus,
  type NotificationType,
} from "@/features/notifications/api/notificationApi";
import { cn } from "@/lib/cn";
import { hasAnyPermission } from "@/shared/auth/permissions";

type AnnouncementFormState = {
  audienceType: BroadcastAnnouncementAudienceType;
  enabled: boolean;
  endAt: string;
  message: string;
  notificationType: NotificationType;
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

const pageSizeOptions = [5, 10, 20];

const roleOptions = [
  { code: "SYSTEM_ADMIN", description: "Quan tri he thong, phan quyen va cau hinh cao nhat.", label: "System admin" },
  { code: "PARKING_MANAGER", description: "Quan ly van hanh bai xe, nhan su, thiet bi va phe duyet.", label: "Parking manager" },
  { code: "EMPLOYEE", description: "Nhan vien van hanh, ca truc, check-in/check-out va ho tro.", label: "Employee" },
  { code: "CUSTOMER", description: "Khach hang su dung cong thong tin va dich vu ve thang.", label: "Customer" },
];

const notificationTypeOptions: Array<{ group: string; label: string; value: NotificationType }> = [
  { group: "He thong", label: "Thong bao he thong", value: "SYSTEM_NOTICE" },
  { group: "Ve thang", label: "Dang ky ve thang", value: "SUBSCRIPTION_REQUESTED" },
  { group: "Ve thang", label: "Ve thang sap het han", value: "SUBSCRIPTION_EXPIRING_SOON" },
  { group: "Thanh toan", label: "Hoa don moi", value: "INVOICE_CREATED" },
  { group: "Thanh toan", label: "Thanh toan thanh cong", value: "PAYMENT_SUCCEEDED" },
  { group: "Thanh toan", label: "Thanh toan that bai", value: "PAYMENT_FAILED" },
  { group: "Ho tro", label: "Ticket moi", value: "SUPPORT_TICKET_CREATED" },
  { group: "Ho tro", label: "Ticket duoc phan cong", value: "SUPPORT_TICKET_ASSIGNED" },
  { group: "Van hanh", label: "Phan ca moi", value: "SHIFT_ASSIGNED" },
  { group: "Van hanh", label: "Thiet bi offline", value: "DEVICE_OFFLINE" },
  { group: "Van hanh", label: "Bao tri thiet bi", value: "DEVICE_MAINTENANCE" },
  { group: "Gia & danh muc", label: "Doi ke hoach gia", value: "PRICE_PLAN_CHANGED" },
  { group: "Gia & danh muc", label: "Doi quy tac gia", value: "PRICE_RULE_CHANGED" },
  { group: "Tai khoan", label: "Ho so can duyet", value: "ACCOUNT_PROFILE_SUBMITTED" },
  { group: "Tai khoan", label: "Tai khoan thay doi trang thai", value: "ACCOUNT_STATUS_CHANGED" },
];

const statusTabs: Array<{ label: string; value: "all" | BroadcastAnnouncementStatus }> = [
  { label: "Tat ca", value: "all" },
  { label: "Nhap", value: "DRAFT" },
  { label: "Da phat", value: "PUBLISHED" },
  { label: "Da huy", value: "CANCELLED" },
];

const statusOptions = statusTabs.map((item) => ({ label: item.label, value: item.value }));

const inputClassName =
  "tw-h-10 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none tw-transition placeholder:tw-text-vm-slate-400 focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)] disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25 disabled:tw-text-vm-slate-500";

function nowLocalDateTime() {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  return now.toISOString().slice(0, 16);
}

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

function formatDateTime(value: string | null | undefined) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}

function getInitialForm(announcement?: BroadcastAnnouncementResponse | null): AnnouncementFormState {
  return {
    audienceType: announcement?.audienceType ?? "ALL_ACTIVE_ACCOUNTS",
    enabled: announcement?.enabled ?? true,
    endAt: toLocalDateTimeInput(announcement?.endAt),
    message: announcement?.message ?? "",
    notificationType: announcement?.notificationType ?? "SYSTEM_NOTICE",
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
  if (status === "PUBLISHED") return "Da phat";
  if (status === "CANCELLED") return "Da huy";
  return "Nhap";
}

function getAudienceLabel(announcement: BroadcastAnnouncementResponse) {
  if (announcement.audienceType === "ALL_ACTIVE_ACCOUNTS") {
    return "Tat ca tai khoan dang ACTIVE";
  }
  return (announcement.roleCodes ?? []).join(", ");
}

function getTypeLabel(value: NotificationType) {
  return notificationTypeOptions.find((option) => option.value === value)?.label ?? value;
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
  return {
    audienceType: form.audienceType,
    enabled: form.enabled,
    endAt: form.endAt ? toInstant(form.endAt) : null,
    message: form.message.trim(),
    notificationType: form.notificationType,
    redirectUrl: form.redirectUrl.trim() || null,
    relatedId: form.relatedId.trim() || null,
    relatedSchema: form.relatedSchema.trim() || null,
    relatedTable: form.relatedTable.trim() || null,
    roleCodes: form.audienceType === "ROLE_CODES" ? form.roleCodes : [],
    startAt: toInstant(form.startAt),
    title: form.title.trim(),
  };
}

function MetricCard({ icon, label, value }: { icon: string; label: string; value: number }) {
  return (
    <Card className="tw-flex tw-min-h-[96px] tw-items-center tw-gap-4 tw-p-4">
      <span className="tw-inline-flex tw-h-11 tw-w-11 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-brand-50 tw-text-vm-primary">
        <i className={icon} />
      </span>
      <span>
        <span className="tw-block tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500">{label}</span>
        <strong className="tw-mt-1 tw-block tw-text-[1.55rem] tw-font-black tw-leading-none tw-text-vm-slate-900">
          {value.toLocaleString("vi-VN")}
        </strong>
      </span>
    </Card>
  );
}

function AnnouncementDrawer({
  announcement,
  canSave,
  onClose,
  onSubmit,
  open,
  saving,
}: {
  announcement: BroadcastAnnouncementResponse | null;
  canSave: boolean;
  onClose: () => void;
  onSubmit: (payload: BroadcastAnnouncementPayload) => Promise<void>;
  open: boolean;
  saving: boolean;
}) {
  const [form, setForm] = useState<AnnouncementFormState>(() => getInitialForm(announcement));
  const [formError, setFormError] = useState("");
  const readOnly = Boolean(announcement && announcement.status !== "DRAFT") || !canSave;

  useEffect(() => {
    setForm(getInitialForm(announcement));
    setFormError("");
  }, [announcement, open]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (readOnly) return;

    if (!form.title.trim() || !form.message.trim() || !form.startAt) {
      setFormError("Vui long nhap tieu de, noi dung va thoi diem bat dau.");
      return;
    }
    if (form.audienceType === "ROLE_CODES" && form.roleCodes.length === 0) {
      setFormError("Khi chon theo vai tro, can chon it nhat mot role.");
      return;
    }
    if (form.endAt && new Date(form.endAt).getTime() < new Date(form.startAt).getTime()) {
      setFormError("Thoi diem ket thuc phai lon hon hoac bang thoi diem bat dau.");
      return;
    }

    setFormError("");
    await onSubmit(normalizePayload(form));
  }

  return (
    <Drawer
      actions={
        <div className={cn("tw-grid tw-gap-2", readOnly ? "tw-grid-cols-1" : "tw-grid-cols-2")}>
          <Button variant="secondary" onClick={onClose}>
            {readOnly ? "Dong" : "Huy"}
          </Button>
          {!readOnly ? (
            <Button form="announcement-form" loading={saving} type="submit">
              <i className="far fa-save" />
              Luu nhap
            </Button>
          ) : null}
        </div>
      }
      description={readOnly ? "Announcement da khoa theo trang thai hoac quyen hien tai." : "Soan noi dung, chon doi tuong nhan va luu ban nhap truoc khi phat."}
      onClose={onClose}
      open={open}
      title={announcement ? "Chi tiet announcement" : "Tao announcement"}
      width="xl"
    >
      <form className="tw-grid tw-gap-5" id="announcement-form" onSubmit={(event) => void handleSubmit(event)}>
        {announcement ? (
          <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-3">
            <span className="tw-min-w-0">
              <strong className="tw-block tw-truncate tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900">
                {announcement.broadcastId}
              </strong>
              <small className="tw-mt-1 tw-block tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500">
                Tao luc {formatDateTime(announcement.createdAt)}
              </small>
            </span>
            <Badge tone={getStatusTone(announcement.status)}>{getStatusLabel(announcement.status)}</Badge>
          </div>
        ) : null}

        <section className="tw-grid tw-gap-3">
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.76rem] tw-font-black tw-text-vm-slate-700">Tieu de *</span>
            <input
              className={inputClassName}
              disabled={readOnly}
              maxLength={200}
              value={form.title}
              onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
            />
          </label>
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.76rem] tw-font-black tw-text-vm-slate-700">Noi dung *</span>
            <textarea
              className="tw-min-h-[130px] tw-w-full tw-resize-y tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3 tw-text-[0.86rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25"
              disabled={readOnly}
              value={form.message}
              onChange={(event) => setForm((current) => ({ ...current, message: event.target.value }))}
            />
          </label>
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.76rem] tw-font-black tw-text-vm-slate-700">Loai thong bao</span>
            <SelectMenu
              ariaLabel="Loai thong bao"
              disabled={readOnly}
              menuClassName="tw-max-h-72"
              options={notificationTypeOptions.map((option) => ({ label: `${option.group} - ${option.label}`, value: option.value }))}
              portal
              value={form.notificationType}
              onChange={(value) => setForm((current) => ({ ...current, notificationType: value as NotificationType }))}
            />
          </label>
        </section>

        <section className="tw-grid tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-5">
          <div>
            <h4 className="tw-m-0 tw-text-[0.92rem] tw-font-black tw-text-vm-slate-900">Doi tuong nhan</h4>
            <p className="tw-m-0 tw-mt-1 tw-text-[0.75rem] tw-font-semibold tw-text-vm-slate-500">
              Backend chi tao notification cho account co status ACTIVE.
            </p>
          </div>
          <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[620px]:tw-grid-cols-1">
            <button
              className={cn(
                "tw-rounded-vm-md tw-border tw-border-solid tw-p-3 tw-text-left tw-transition",
                form.audienceType === "ALL_ACTIVE_ACCOUNTS" ? "tw-border-brand-200 tw-bg-brand-50" : "tw-border-vm-slate-100 tw-bg-white",
              )}
              disabled={readOnly}
              type="button"
              onClick={() => setForm((current) => ({ ...current, audienceType: "ALL_ACTIVE_ACCOUNTS", roleCodes: [] }))}
            >
              <strong className="tw-block tw-text-[0.86rem] tw-font-black tw-text-vm-slate-900">Tat ca account ACTIVE</strong>
              <span className="tw-mt-1 tw-block tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500">Khach hang va noi bo deu nhan.</span>
            </button>
            <button
              className={cn(
                "tw-rounded-vm-md tw-border tw-border-solid tw-p-3 tw-text-left tw-transition",
                form.audienceType === "ROLE_CODES" ? "tw-border-brand-200 tw-bg-brand-50" : "tw-border-vm-slate-100 tw-bg-white",
              )}
              disabled={readOnly}
              type="button"
              onClick={() => setForm((current) => ({ ...current, audienceType: "ROLE_CODES" }))}
            >
              <strong className="tw-block tw-text-[0.86rem] tw-font-black tw-text-vm-slate-900">Theo role code</strong>
              <span className="tw-mt-1 tw-block tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500">Chi cac role duoc chon moi nhan.</span>
            </button>
          </div>

          {form.audienceType === "ROLE_CODES" ? (
            <div className="tw-grid tw-gap-2">
              {roleOptions.map((role) => {
                const selected = form.roleCodes.includes(role.code);
                return (
                  <button
                    className={cn(
                      "tw-grid tw-grid-cols-[22px_minmax(0,1fr)] tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-p-3 tw-text-left tw-transition",
                      selected ? "tw-border-brand-200 tw-bg-brand-50" : "tw-border-vm-slate-100 tw-bg-white",
                    )}
                    disabled={readOnly}
                    key={role.code}
                    type="button"
                    onClick={() =>
                      setForm((current) => ({
                        ...current,
                        roleCodes: selected
                          ? current.roleCodes.filter((item) => item !== role.code)
                          : [...current.roleCodes, role.code],
                      }))
                    }
                  >
                    <span className={cn("tw-mt-0.5 tw-inline-flex tw-h-[18px] tw-w-[18px] tw-items-center tw-justify-center tw-rounded tw-border tw-border-solid", selected ? "tw-border-vm-primary tw-bg-vm-primary tw-text-white" : "tw-border-vm-slate-300")}>
                      {selected ? <i className="fas fa-check tw-text-[0.62rem]" /> : null}
                    </span>
                    <span className="tw-min-w-0">
                      <strong className="tw-block tw-text-[0.82rem] tw-font-black tw-text-vm-slate-900">{role.code}</strong>
                      <small className="tw-mt-1 tw-block tw-text-[0.72rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">{role.description}</small>
                    </span>
                  </button>
                );
              })}
            </div>
          ) : null}
        </section>

        <section className="tw-grid tw-grid-cols-2 tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-5 max-[620px]:tw-grid-cols-1">
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.76rem] tw-font-black tw-text-vm-slate-700">Bat dau *</span>
            <input
              className={inputClassName}
              disabled={readOnly}
              type="datetime-local"
              value={form.startAt}
              onChange={(event) => setForm((current) => ({ ...current, startAt: event.target.value }))}
            />
          </label>
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.76rem] tw-font-black tw-text-vm-slate-700">Ket thuc</span>
            <input
              className={inputClassName}
              disabled={readOnly}
              type="datetime-local"
              value={form.endAt}
              onChange={(event) => setForm((current) => ({ ...current, endAt: event.target.value }))}
            />
          </label>
          <label className="tw-grid tw-gap-2 tw-col-span-2 max-[620px]:tw-col-span-1">
            <span className="tw-text-[0.76rem] tw-font-black tw-text-vm-slate-700">Redirect URL</span>
            <input
              className={inputClassName}
              disabled={readOnly}
              placeholder="/admin/dashboard hoac /customer/support"
              value={form.redirectUrl}
              onChange={(event) => setForm((current) => ({ ...current, redirectUrl: event.target.value }))}
            />
          </label>
          <label className="tw-flex tw-items-center tw-gap-3 tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-3">
            <input
              checked={form.enabled}
              disabled={readOnly}
              type="checkbox"
              onChange={(event) => setForm((current) => ({ ...current, enabled: event.target.checked }))}
            />
            <span className="tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-700">Cho phep publish</span>
          </label>
        </section>

        <section className="tw-grid tw-grid-cols-3 tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-5 max-[620px]:tw-grid-cols-1">
          <input className={inputClassName} disabled={readOnly} placeholder="relatedSchema" value={form.relatedSchema} onChange={(event) => setForm((current) => ({ ...current, relatedSchema: event.target.value }))} />
          <input className={inputClassName} disabled={readOnly} placeholder="relatedTable" value={form.relatedTable} onChange={(event) => setForm((current) => ({ ...current, relatedTable: event.target.value }))} />
          <input className={inputClassName} disabled={readOnly} placeholder="relatedId UUID" value={form.relatedId} onChange={(event) => setForm((current) => ({ ...current, relatedId: event.target.value }))} />
        </section>

        <section className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3">
          <span className="tw-text-[0.72rem] tw-font-black tw-uppercase tw-text-vm-slate-500">Preview chuong thong bao</span>
          <div className="tw-mt-3 tw-grid tw-grid-cols-[40px_minmax(0,1fr)] tw-gap-3 tw-rounded-vm-md tw-bg-white tw-p-3">
            <span className="tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-brand-50 tw-text-vm-primary">
              <i className="fas fa-bullhorn" />
            </span>
            <span className="tw-min-w-0">
              <strong className="tw-block tw-truncate tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">{form.title || "Tieu de thong bao"}</strong>
              <small className="tw-mt-1 tw-line-clamp-2 tw-text-[0.75rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">{form.message || "Noi dung se hien thi trong popover notification cua nguoi nhan."}</small>
            </span>
          </div>
        </section>

        {formError ? (
          <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-p-3 tw-text-[0.8rem] tw-font-bold tw-text-red-600">
            {formError}
          </div>
        ) : null}
      </form>
    </Drawer>
  );
}

export function AnnouncementManagementPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [announcements, setAnnouncements] = useState<BroadcastAnnouncementResponse[]>([]);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingAnnouncement, setEditingAnnouncement] = useState<BroadcastAnnouncementResponse | null>(null);
  const [keyword, setKeyword] = useState("");
  const [loading, setLoading] = useState(true);
  const [pageSize, setPageSize] = useState(5);
  const [currentPage, setCurrentPage] = useState(1);
  const [saving, setSaving] = useState(false);
  const [statusFilter, setStatusFilter] = useState<"all" | BroadcastAnnouncementStatus>("all");
  const [pendingAction, setPendingAction] = useState<PendingAction>(null);

  const canCreate = hasAnyPermission(user, ["BROADCAST_NOTIFICATION_CREATE_ALL"]);
  const canUpdate = hasAnyPermission(user, ["BROADCAST_NOTIFICATION_UPDATE_ALL"]);
  const canPublish = hasAnyPermission(user, ["BROADCAST_NOTIFICATION_PUBLISH_ALL"]);
  const canCancel = hasAnyPermission(user, ["BROADCAST_NOTIFICATION_CANCEL_ALL"]);
  const canDelete = hasAnyPermission(user, ["BROADCAST_NOTIFICATION_DELETE_ALL"]);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const response = await getBroadcastAnnouncements(statusFilter === "all" ? {} : { status: statusFilter });
      setAnnouncements(response.data ?? []);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Khong the tai announcement.", "Tai du lieu that bai");
    } finally {
      setLoading(false);
    }
  }, [statusFilter, toast]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const filteredAnnouncements = useMemo(() => {
    const search = keyword.trim().toLowerCase();
    if (!search) return announcements;
    return announcements.filter((item) =>
      [item.title, item.message, item.notificationType, getAudienceLabel(item)]
        .some((value) => String(value ?? "").toLowerCase().includes(search)),
    );
  }, [announcements, keyword]);

  const page = getPageItems(filteredAnnouncements, currentPage, pageSize);

  useEffect(() => {
    if (currentPage !== page.safeCurrentPage) {
      setCurrentPage(page.safeCurrentPage);
    }
  }, [currentPage, page.safeCurrentPage]);

  async function handleSubmit(payload: BroadcastAnnouncementPayload) {
    setSaving(true);
    try {
      const response = editingAnnouncement
        ? await updateBroadcastAnnouncement(editingAnnouncement.broadcastId, payload)
        : await createBroadcastAnnouncement(payload);
      toast.success(response.message || "Da luu announcement.", "Luu thanh cong");
      setDrawerOpen(false);
      setEditingAnnouncement(null);
      await loadData();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Khong the luu announcement.", "Luu that bai");
    } finally {
      setSaving(false);
    }
  }

  async function handleConfirmAction() {
    if (!pendingAction) return;
    setSaving(true);
    try {
      if (pendingAction.action === "publish") {
        await publishBroadcastAnnouncement(pendingAction.announcement.broadcastId);
        toast.success("Announcement da duoc phat thanh notification cho nguoi nhan ACTIVE.", "Da phat");
      }
      if (pendingAction.action === "cancel") {
        await cancelBroadcastAnnouncement(pendingAction.announcement.broadcastId);
        toast.success("Announcement da duoc huy.", "Da huy");
      }
      if (pendingAction.action === "delete") {
        await deleteBroadcastAnnouncement(pendingAction.announcement.broadcastId);
        toast.success("Announcement da duoc xoa.", "Da xoa");
      }
      setPendingAction(null);
      await loadData();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Khong the thuc hien thao tac.", "Thao tac that bai");
    } finally {
      setSaving(false);
    }
  }

  const draftCount = announcements.filter((item) => item.status === "DRAFT").length;
  const publishedCount = announcements.filter((item) => item.status === "PUBLISHED").length;
  const cancelledCount = announcements.filter((item) => item.status === "CANCELLED").length;

  return (
    <div className="tw-px-4 tw-py-4 lg:tw-px-5">
      <section className="tw-mx-auto tw-min-h-[calc(100vh-104px)] tw-w-[min(100%,1560px)]">
        <header className="tw-flex tw-items-start tw-justify-between tw-gap-4 max-[720px]:tw-flex-col">
          <div>
            <h1 className="tw-m-0 tw-text-vm-page-title tw-text-vm-slate-900">Quan ly thong bao phat rong</h1>
            <p className="tw-mb-0 tw-mt-2 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-500">
              Tao announcement, chon audience theo role va publish thanh notification WEB cho tung tai khoan ACTIVE.
            </p>
          </div>
          <div className="tw-flex tw-gap-2 max-[720px]:tw-w-full">
            <Button className="max-[720px]:tw-flex-1" disabled={loading} variant="secondary" onClick={() => void loadData()}>
              <i className="fas fa-sync-alt" />
              Lam moi
            </Button>
            {canCreate ? (
              <Button
                className="max-[720px]:tw-flex-1"
                onClick={() => {
                  setEditingAnnouncement(null);
                  setDrawerOpen(true);
                }}
              >
                <i className="fas fa-plus" />
                Tao thong bao
              </Button>
            ) : null}
          </div>
        </header>

        <div className="tw-mt-6 tw-grid tw-grid-cols-4 tw-gap-3 max-[1180px]:tw-grid-cols-2 max-[620px]:tw-grid-cols-1">
          <MetricCard icon="fas fa-bullhorn" label="Tong announcement" value={announcements.length} />
          <MetricCard icon="far fa-edit" label="Ban nhap" value={draftCount} />
          <MetricCard icon="far fa-paper-plane" label="Da phat" value={publishedCount} />
          <MetricCard icon="fas fa-ban" label="Da huy" value={cancelledCount} />
        </div>

        <main className="tw-mt-5 tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-vm-card">
          <div className="tw-flex tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3 max-[860px]:tw-flex-col">
            <label className="tw-flex tw-h-[42px] tw-min-w-[260px] tw-flex-1 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 max-[860px]:tw-w-full">
              <i className="fas fa-search tw-text-vm-slate-500" />
              <input
                className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none placeholder:tw-text-vm-slate-400"
                placeholder="Tim tieu de, noi dung, loai, audience..."
                value={keyword}
                onChange={(event) => {
                  setKeyword(event.target.value);
                  setCurrentPage(1);
                }}
              />
            </label>
            <div className="tw-w-[190px] max-[860px]:tw-w-full">
              <SelectMenu
                ariaLabel="Trang thai announcement"
                options={statusOptions}
                portal
                value={statusFilter}
                onChange={(value) => {
                  setStatusFilter(value as "all" | BroadcastAnnouncementStatus);
                  setCurrentPage(1);
                }}
              />
            </div>
          </div>

          <div className="tw-overflow-x-auto">
            <table className="table tw-m-0 tw-w-full tw-min-w-[1180px] tw-table-fixed [&_td]:tw-border-0 [&_td]:tw-border-t [&_td]:tw-border-solid [&_td]:tw-border-vm-slate-100 [&_td]:tw-px-4 [&_td]:tw-py-3 [&_td]:tw-align-middle [&_thead_th]:tw-border-0 [&_thead_th]:tw-bg-white [&_thead_th]:tw-px-4 [&_thead_th]:tw-py-3.5 [&_thead_th]:tw-text-left [&_thead_th]:tw-text-[0.75rem] [&_thead_th]:tw-font-black [&_thead_th]:tw-text-vm-slate-700">
              <colgroup>
                <col className="tw-w-[300px]" />
                <col className="tw-w-[170px]" />
                <col className="tw-w-[250px]" />
                <col className="tw-w-[130px]" />
                <col className="tw-w-[190px]" />
                <col className="tw-w-[140px]" />
              </colgroup>
              <thead>
                <tr>
                  <th>Noi dung</th>
                  <th>Loai</th>
                  <th>Doi tuong nhan</th>
                  <th>Trang thai</th>
                  <th>Lich phat</th>
                  <th className="tw-text-right">Thao tac</th>
                </tr>
              </thead>
              <tbody>
                {page.rows.map((announcement) => {
                  const editable = announcement.status === "DRAFT";
                  return (
                    <tr className="tw-transition hover:tw-bg-vm-slate-25" key={announcement.broadcastId}>
                      <td>
                        <button
                          className="tw-w-full tw-border-0 tw-bg-transparent tw-p-0 tw-text-left"
                          type="button"
                          onClick={() => {
                            setEditingAnnouncement(announcement);
                            setDrawerOpen(true);
                          }}
                        >
                          <strong className="tw-block tw-truncate tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">{announcement.title}</strong>
                          <small className="tw-mt-1 tw-line-clamp-2 tw-text-[0.73rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">{announcement.message}</small>
                        </button>
                      </td>
                      <td className="tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-700">{getTypeLabel(announcement.notificationType)}</td>
                      <td>
                        <strong className="tw-block tw-truncate tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-800">{getAudienceLabel(announcement)}</strong>
                        <small className="tw-mt-1 tw-block tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">
                          So nguoi nhan du kien: tinh khi publish
                        </small>
                      </td>
                      <td>
                        <Badge tone={getStatusTone(announcement.status)}>{getStatusLabel(announcement.status)}</Badge>
                      </td>
                      <td className="tw-text-[0.74rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-600">
                        <strong className="tw-block tw-text-vm-slate-800">{formatDateTime(announcement.startAt)}</strong>
                        <span>{announcement.endAt ? `Den ${formatDateTime(announcement.endAt)}` : "Khong gioi han ket thuc"}</span>
                      </td>
                      <td>
                        <div className="tw-flex tw-justify-end tw-gap-1">
                          <button
                            aria-label="Xem hoac sua announcement"
                            className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-primary hover:tw-bg-brand-50"
                            title={editable && canUpdate ? "Sua" : "Xem chi tiet"}
                            type="button"
                            onClick={() => {
                              setEditingAnnouncement(announcement);
                              setDrawerOpen(true);
                            }}
                          >
                            <i className={editable && canUpdate ? "far fa-edit" : "far fa-eye"} />
                          </button>
                          {editable && canPublish ? (
                            <button className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-emerald-600 hover:tw-bg-emerald-50" title="Xuat ban" type="button" onClick={() => setPendingAction({ action: "publish", announcement })}>
                              <i className="far fa-paper-plane" />
                            </button>
                          ) : null}
                          {editable && canCancel ? (
                            <button className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-amber-600 hover:tw-bg-amber-50" title="Huy" type="button" onClick={() => setPendingAction({ action: "cancel", announcement })}>
                              <i className="fas fa-ban" />
                            </button>
                          ) : null}
                          {announcement.status !== "PUBLISHED" && canDelete ? (
                            <button className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-red-600 hover:tw-bg-red-50" title="Xoa" type="button" onClick={() => setPendingAction({ action: "delete", announcement })}>
                              <i className="far fa-trash-alt" />
                            </button>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {page.rows.length === 0 ? (
                  <tr>
                    <td className="tw-py-12 tw-text-center" colSpan={6}>
                      <span className="tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-full tw-bg-vm-slate-50 tw-text-vm-slate-400">
                        <i className={loading ? "fas fa-spinner fa-spin" : "far fa-bell"} />
                      </span>
                      <p className="tw-mb-0 tw-mt-3 tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-500">
                        {loading ? "Dang tai announcement..." : "Chua co announcement phu hop."}
                      </p>
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>

          <PaginationFooter
            ariaLabel="Phan trang announcement"
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
        canSave={editingAnnouncement ? canUpdate : canCreate}
        onClose={() => {
          if (saving) return;
          setDrawerOpen(false);
          setEditingAnnouncement(null);
        }}
        onSubmit={handleSubmit}
        open={drawerOpen}
        saving={saving}
      />

      <Modal
        actions={
          <div className="tw-grid tw-grid-cols-2 tw-gap-2">
            <Button disabled={saving} variant="secondary" onClick={() => setPendingAction(null)}>Dong</Button>
            <Button loading={saving} variant={pendingAction?.action === "delete" ? "danger" : "primary"} onClick={() => void handleConfirmAction()}>
              {pendingAction?.action === "publish" ? "Xuat ban" : pendingAction?.action === "cancel" ? "Huy" : "Xoa"}
            </Button>
          </div>
        }
        description="Thao tac nay se goi truc tiep API backend va ap dung rule trang thai hien co."
        onClose={() => {
          if (!saving) setPendingAction(null);
        }}
        open={Boolean(pendingAction)}
        title="Xac nhan thao tac"
      >
        <p className="tw-m-0 tw-text-[0.9rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-600">
          Ban muon {pendingAction?.action === "publish" ? "xuat ban" : pendingAction?.action === "cancel" ? "huy" : "xoa"} announcement
          <strong className="tw-text-vm-slate-900"> {pendingAction?.announcement.title}</strong>?
        </p>
      </Modal>
    </div>
  );
}
