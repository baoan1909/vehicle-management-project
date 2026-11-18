import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";

import { Badge, Button, Card, Drawer, Modal, PaginationFooter, SelectMenu, useToast } from "@/components/ui";
import {
  activateSupportTicketCategory,
  createSupportTicketCategory,
  deactivateSupportTicketCategory,
  getSupportTicketCategories,
  getSupportTickets,
  updateSupportTicketCategory,
  type SaveSupportTicketCategoryRequest,
  type SupportTicketCategoryResponse,
  type SupportTicketCategoryStatus,
  type SupportTicketPriority,
  type SupportTicketResponse,
} from "@/features/support/api/supportApi";
import { cn } from "@/lib/cn";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { getIamPermissions, type PermissionAdminResponse } from "@/features/iam/api/rolePermissionApi";

type SupportCategoryRow = SupportTicketCategoryResponse & {
  openTickets: number;
};

const priorityOptions = [
  { label: "Ưu tiên: Tất cả", value: "all" },
  { label: "Khẩn cấp", value: "URGENT" },
  { label: "Cao", value: "HIGH" },
  { label: "Bình thường", value: "NORMAL" },
  { label: "Thấp", value: "LOW" },
];

const statusOptions = [
  { label: "Trạng thái: Tất cả", value: "all" },
  { label: "Đang hoạt động", value: "ACTIVE" },
  { label: "Ngưng sử dụng", value: "INACTIVE" },
];

function priorityLabel(priority: SupportTicketPriority) {
  if (priority === "URGENT") return "Khẩn cấp";
  if (priority === "HIGH") return "Cao";
  if (priority === "NORMAL") return "Bình thường";
  return "Thấp";
}

function priorityTone(priority: SupportTicketPriority) {
  if (priority === "URGENT" || priority === "HIGH") return "danger";
  if (priority === "NORMAL") return "warning";
  return "success";
}

function buildRows(categories: SupportTicketCategoryResponse[], tickets: SupportTicketResponse[]) {
  const openCounts = tickets.reduce((map, ticket) => {
    if (ticket.status === "OPEN" || ticket.status === "IN_PROGRESS") {
      map.set(ticket.categoryId, (map.get(ticket.categoryId) ?? 0) + 1);
    }
    return map;
  }, new Map<string, number>());

  return categories.map((category) => ({
    ...category,
    openTickets: openCounts.get(category.categoryId) ?? 0,
  }));
}

function MetricCard({ icon, iconClassName, label, meta, metaClassName, value }: { icon: string; iconClassName: string; label: string; meta?: string; metaClassName?: string; value: string }) {
  return (
    <Card className="tw-flex tw-min-h-[108px] tw-items-center tw-justify-between tw-p-4">
      <span className="tw-grid">
        <span className="tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-500">{label}</span>
        <strong className="tw-mt-2 tw-text-[1.7rem] tw-font-black tw-leading-none tw-text-vm-slate-900">{value}</strong>
        {meta ? <span className={cn("tw-mt-2 tw-text-[0.72rem] tw-font-black", metaClassName)}>{meta}</span> : null}
      </span>
      <span className={cn("tw-inline-flex tw-h-11 tw-w-11 tw-items-center tw-justify-center tw-rounded-full tw-text-[1.2rem]", iconClassName)}>
        <i className={icon} />
      </span>
    </Card>
  );
}

function CategoryTable({
  canActivate,
  canEdit,
  currentPage,
  loading,
  onDeactivate,
  onEdit,
  onPageChange,
  onPageSizeChange,
  onReactivate,
  pageSize,
  rows,
  startIndex,
  totalRecords,
}: {
  canActivate: boolean;
  canEdit: boolean;
  currentPage: number;
  loading: boolean;
  onDeactivate: (category: SupportCategoryRow) => void;
  onEdit: (category: SupportCategoryRow) => void;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  onReactivate: (category: SupportCategoryRow) => void;
  pageSize: number;
  rows: SupportCategoryRow[];
  startIndex: number;
  totalRecords: number;
}) {
  const endIndex = totalRecords === 0 ? 0 : startIndex + rows.length - 1;
  const totalPages = Math.max(1, Math.ceil(totalRecords / pageSize));

  return (
    <div className="tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white">
      <div className="tw-grid tw-h-11 tw-grid-cols-[48px_230px_minmax(150px,1fr)_110px_130px_132px_112px] tw-items-center tw-bg-vm-slate-25 tw-px-4 tw-text-[0.72rem] tw-font-black tw-text-vm-slate-700 max-[1180px]:tw-hidden">
        <span>#</span>
        <span>Mã danh mục</span>
        <span>Tên danh mục</span>
        <span>Ưu tiên</span>
        <span>Yêu cầu mở</span>
        <span>Trạng thái</span>
        <span className="tw-text-right">Thao tác</span>
      </div>

      {rows.map((category, index) => (
        <div key={category.categoryId} className="tw-grid tw-min-h-[56px] tw-grid-cols-[48px_230px_minmax(150px,1fr)_110px_130px_132px_112px] tw-items-center tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-700 max-[1180px]:tw-grid-cols-1 max-[1180px]:tw-gap-2 max-[1180px]:tw-py-3">
          <span>{startIndex + index}</span>
          <strong className="tw-min-w-0 tw-whitespace-nowrap tw-pr-4 tw-text-[0.78rem] tw-leading-5 tw-text-vm-slate-900">{category.code}</strong>
          <span className="tw-min-w-0">
            <strong className="tw-block tw-text-vm-slate-900">{category.name}</strong>
            {category.description ? <small className="tw-mt-1 tw-block tw-text-vm-slate-500">{category.description}</small> : null}
          </span>
          <Badge tone={priorityTone(category.priority)} className="tw-w-fit tw-rounded-vm-sm tw-px-2.5">{priorityLabel(category.priority)}</Badge>
          <strong className="tw-text-vm-slate-900">{category.openTickets.toLocaleString("vi-VN")}</strong>
          <Badge tone={category.status === "ACTIVE" ? "success" : "neutral"} className="tw-w-fit tw-rounded-vm-sm tw-px-3">{category.status === "ACTIVE" ? "Đang hoạt động" : "Ngưng sử dụng"}</Badge>
          <span className="tw-flex tw-justify-end tw-gap-3 max-[1180px]:tw-justify-start">
            {canEdit ? <button type="button" className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-white tw-text-vm-primary hover:tw-bg-brand-50" aria-label={`Chỉnh sửa danh mục ${category.name}`} title="Chỉnh sửa danh mục" onClick={() => onEdit(category)}>
              <i className="far fa-edit tw-text-[1rem]" />
            </button> : null}
            {canActivate && category.status === "ACTIVE" ? (
              <button type="button" className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-white tw-text-vm-slate-700 hover:tw-bg-red-50 hover:tw-text-vm-danger" aria-label={`Ngưng sử dụng danh mục ${category.name}`} title="Ngưng sử dụng danh mục" onClick={() => onDeactivate(category)}>
                <i className="fas fa-ban" />
              </button>
            ) : canActivate ? (
              <button type="button" className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-white tw-text-emerald-600 hover:tw-bg-emerald-50" aria-label={`Kích hoạt lại danh mục ${category.name}`} title="Kích hoạt lại danh mục" onClick={() => onReactivate(category)}>
                <i className="fas fa-check" />
              </button>
            ) : null}
          </span>
        </div>
      ))}

      {rows.length === 0 ? (
        <div className="tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-py-8 tw-text-center tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-500">
          {loading ? "Đang tải danh mục hỗ trợ..." : "Chưa có danh mục hỗ trợ phù hợp với bộ lọc."}
        </div>
      ) : null}

      <PaginationFooter
        ariaLabel="Phân trang danh mục hỗ trợ"
        currentPage={currentPage}
        endIndex={endIndex}
        onPageChange={onPageChange}
        onPageSizeChange={onPageSizeChange}
        pageSize={pageSize}
        pageSizeOptions={[5, 10, 20]}
        startIndex={totalRecords === 0 ? 0 : startIndex}
        totalPages={totalPages}
        totalRecords={totalRecords}
      />
    </div>
  );
}

function CategoryDrawer({
  category,
  error,
  onClose,
  onSubmit,
  open,
  permissionOptions,
  saving,
}: {
  category: SupportCategoryRow | null;
  error: string;
  onClose: () => void;
  onSubmit: (payload: SaveSupportTicketCategoryRequest) => Promise<void> | void;
  open: boolean;
  permissionOptions: Array<{ code: string; label: string }>;
  saving: boolean;
}) {
  const [form, setForm] = useState<SaveSupportTicketCategoryRequest>({
    audienceType: "CUSTOMER", code: "", description: "", displayOrder: 0, featureCode: "",
    name: "", permissionMatchMode: "ANY", priority: "NORMAL", requiredPermissionCodes: [],
  });
  const [permissionSearch, setPermissionSearch] = useState("");
  const [formError, setFormError] = useState("");

  useEffect(() => {
    setForm({
      code: category?.code ?? "",
      description: category?.description ?? "",
      audienceType: category?.audienceType ?? "CUSTOMER",
      displayOrder: category?.displayOrder ?? 0,
      featureCode: category?.featureCode ?? "",
      name: category?.name ?? "",
      permissionMatchMode: category?.permissionMatchMode ?? "ANY",
      priority: category?.priority ?? "NORMAL",
      requiredPermissionCodes: category?.requiredPermissionCodes ?? [],
    });
    setPermissionSearch("");
    setFormError("");
  }, [category, open]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError("");

    if (!form.code.trim() || !form.name.trim()) {
      setFormError("Vui lòng nhập mã và tên danh mục.");
      return;
    }

    await onSubmit({
      code: form.code.trim(),
      description: form.description?.trim() || null,
      audienceType: form.audienceType,
      displayOrder: Number(form.displayOrder) || 0,
      featureCode: form.featureCode?.trim() || null,
      name: form.name.trim(),
      permissionMatchMode: form.permissionMatchMode,
      priority: form.priority,
      requiredPermissionCodes: form.requiredPermissionCodes,
    });
  }

  return (
    <Drawer
      actions={
        <div className="tw-grid tw-grid-cols-2 tw-gap-2">
          <Button variant="secondary" onClick={onClose}>Hủy</Button>
          <Button loading={saving} onClick={(event) => void handleSubmit(event as unknown as FormEvent)}>{saving ? "Đang lưu" : "Lưu thay đổi"}</Button>
        </div>
      }
      description="Thiết lập thông tin cho danh mục hỗ trợ"
      onClose={onClose}
      open={open}
      title={category ? "Chỉnh sửa danh mục" : "Thêm danh mục"}
      width="lg"
    >
      <form className="tw-grid tw-gap-4" onSubmit={(event) => void handleSubmit(event)}>
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">Mã danh mục</span>
          <input className="tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200" value={form.code} onChange={(event) => setForm((current) => ({ ...current, code: event.target.value }))} />
        </label>
        <div className="tw-grid tw-grid-cols-2 tw-gap-3">
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">Đối tượng</span>
            <SelectMenu ariaLabel="Đối tượng sử dụng" value={form.audienceType} onChange={(value) => setForm((current) => ({ ...current, audienceType: value as SaveSupportTicketCategoryRequest["audienceType"] }))} options={[{ label: "Khách hàng", value: "CUSTOMER" }, { label: "Đối tác & nhân sự", value: "INTERNAL" }]} portal />
          </label>
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">Chế độ khớp quyền</span>
            <SelectMenu ariaLabel="Chế độ khớp quyền" value={form.permissionMatchMode} onChange={(value) => setForm((current) => ({ ...current, permissionMatchMode: value as SaveSupportTicketCategoryRequest["permissionMatchMode"] }))} options={[{ label: "Có một quyền (ANY)", value: "ANY" }, { label: "Có đủ quyền (ALL)", value: "ALL" }]} portal />
          </label>
        </div>
        <div className="tw-grid tw-grid-cols-[minmax(0,1fr)_140px] tw-gap-3">
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">Mã tính năng</span>
            <input className="tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200" value={form.featureCode ?? ""} onChange={(event) => setForm((current) => ({ ...current, featureCode: event.target.value }))} placeholder="PARKING_SESSION" />
          </label>
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">Thứ tự</span>
            <input type="number" min={0} className="tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200" value={form.displayOrder} onChange={(event) => setForm((current) => ({ ...current, displayOrder: Number(event.target.value) }))} />
          </label>
        </div>
        <fieldset className="tw-grid tw-gap-2 tw-border-0 tw-p-0">
          <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">Quyền bắt buộc</span>
          <input className="tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.82rem] tw-font-semibold tw-outline-none focus:tw-border-brand-200" value={permissionSearch} onChange={(event) => setPermissionSearch(event.target.value)} placeholder="Tìm quyền theo tên hoặc mã..." />
          <div className="tw-max-h-44 tw-overflow-y-auto tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-2">
            {permissionOptions.filter((permission) => `${permission.code} ${permission.label}`.toLowerCase().includes(permissionSearch.trim().toLowerCase())).map((permission) => {
              const checked = form.requiredPermissionCodes.includes(permission.code);
              return <label key={permission.code} className="tw-flex tw-cursor-pointer tw-items-start tw-gap-2 tw-rounded-md tw-p-2 hover:tw-bg-white">
                <input type="checkbox" checked={checked} onChange={() => setForm((current) => ({ ...current, requiredPermissionCodes: checked ? current.requiredPermissionCodes.filter((code) => code !== permission.code) : [...current.requiredPermissionCodes, permission.code] }))} />
                <span className="tw-min-w-0"><strong className="tw-block tw-break-all tw-text-[0.76rem] tw-text-vm-slate-900">{permission.code}</strong><small className="tw-text-vm-slate-500">{permission.label}</small></span>
              </label>;
            })}
            {permissionOptions.length === 0 ? <p className="tw-m-2 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Không tải được danh mục quyền IAM.</p> : null}
          </div>
          <small className="tw-text-vm-slate-500">Không chọn quyền: mọi tài khoản thuộc đúng nhóm đối tượng đều có thể dùng danh mục này.</small>
        </fieldset>
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">Tên danh mục</span>
          <input className="tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200" value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} />
        </label>
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">Ưu tiên</span>
          <SelectMenu ariaLabel="Mức ưu tiên"
                  triggerClassName={"tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-outline-none"}
                  value={form.priority}
                  onChange={(selectedValue) => setForm((current) => ({ ...current, priority: selectedValue as SupportTicketPriority }))}
                  options={[{ label: "Khẩn cấp", value: "URGENT" }, { label: "Cao", value: "HIGH" }, { label: "Bình thường", value: "NORMAL" }, { label: "Thấp", value: "LOW" }]} portal clearValue="" />
        </label>
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">Mô tả</span>
          <textarea className="tw-min-h-[110px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-2 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200" value={form.description ?? ""} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} />
        </label>
        {formError || error ? <div className="tw-rounded-vm-md tw-bg-red-50 tw-p-3 tw-text-[0.8rem] tw-font-bold tw-text-red-600">{formError || error}</div> : null}
      </form>
    </Drawer>
  );
}

function DeactivateModal({ category, onClose, onConfirm, saving }: { category: SupportCategoryRow; onClose: () => void; onConfirm: () => Promise<void> | void; saving: boolean }) {
  return (
    <Modal
      actions={
        <div className="tw-flex tw-justify-end tw-gap-2">
          <Button variant="secondary" disabled={saving} onClick={onClose}>Hủy</Button>
          <Button variant="danger" loading={saving} onClick={() => void onConfirm()}>{saving ? "Đang xử lý" : "Ngưng sử dụng"}</Button>
        </div>
      }
      description="Danh mục sẽ không còn được chọn khi tạo yêu cầu mới; các phiếu hiện có vẫn được giữ nguyên."
      onClose={onClose}
      open={Boolean(category)}
      title="Ngưng sử dụng danh mục"
      width="md"
    >
      <div className="tw-grid tw-grid-cols-[56px_minmax(0,1fr)] tw-gap-4">
        <span className="tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-vm-lg tw-border-2 tw-border-solid tw-border-orange-400 tw-bg-orange-50 tw-text-[1.45rem] tw-text-orange-500">
          <i className="fas fa-exclamation-triangle" />
        </span>
        <div className="tw-min-w-0">
          <p className="tw-mb-0 tw-mt-1 tw-text-[0.9rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-700">
            Bạn có chắc chắn muốn ngưng sử dụng danh mục "{category.name}"?
          </p>
          {category.openTickets > 0 ? <p className="tw-mb-0 tw-mt-2 tw-text-[0.86rem] tw-font-semibold tw-text-orange-600">{category.openTickets} yêu cầu chưa hoàn tất vẫn tiếp tục được xử lý sau khi ngưng danh mục.</p> : null}
        </div>
      </div>
    </Modal>
  );
}

export function SupportCategoryWorkflowPage() {
  const toast = useToast();
  const { user } = useAuth();
  const canCreate = hasAnyPermission(user, ["SUPPORT_TICKET_CATEGORY_CREATE_ALL"]);
  const canUpdate = hasAnyPermission(user, ["SUPPORT_TICKET_CATEGORY_UPDATE_ALL"]);
  const canActivate = hasAnyPermission(user, ["SUPPORT_TICKET_CATEGORY_ACTIVATE_ALL"]);
  const canReadSupportTickets = hasAnyPermission(user, ["SUPPORT_TICKET_READ_ALL"]);
  const [rows, setRows] = useState<SupportCategoryRow[]>([]);
  const [audience, setAudience] = useState<"CUSTOMER" | "INTERNAL">("CUSTOMER");
  const [permissionOptions, setPermissionOptions] = useState<Array<{ code: string; label: string }>>([]);
  const [keyword, setKeyword] = useState("");
  const [priority, setPriority] = useState("all");
  const [status, setStatus] = useState("all");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [editingCategory, setEditingCategory] = useState<SupportCategoryRow | null>(null);
  const [deactivatingCategory, setDeactivatingCategory] = useState<SupportCategoryRow | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    void getIamPermissions().then((response) => {
      if (!active) return;
      setPermissionOptions((response.data ?? []).map((permission: PermissionAdminResponse) => ({
        code: permission.permissionCode ?? permission.permission_code ?? "",
        label: permission.name ?? permission.description ?? "",
      })).filter((permission) => Boolean(permission.code)).sort((left, right) => left.code.localeCompare(right.code)));
    }).catch(() => {
      if (active) setPermissionOptions([]);
    });
    return () => { active = false; };
  }, []);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const categoryFilter = {
        keyword: keyword.trim() || undefined,
        priority: priority === "all" ? undefined : priority as SupportTicketPriority,
        status: status === "all" ? undefined : status as SupportTicketCategoryStatus,
      };
      const categoryResponse = await getSupportTicketCategories(categoryFilter);
      const tickets = canReadSupportTickets ? (await getSupportTickets()).data ?? [] : [];
      setRows(buildRows(categoryResponse.data ?? [], tickets));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể tải dữ liệu hỗ trợ.");
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [canReadSupportTickets, keyword, priority, status]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadData();
    }, 250);

    return () => window.clearTimeout(timer);
  }, [loadData]);

  const visibleRows = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return rows.filter((category) => {
      const matchKeyword =
        !normalizedKeyword ||
        category.code.toLowerCase().includes(normalizedKeyword) ||
        category.name.toLowerCase().includes(normalizedKeyword) ||
        (category.description ?? "").toLowerCase().includes(normalizedKeyword);
      const matchPriority = priority === "all" || category.priority === priority;
      const matchStatus = status === "all" || category.status === status;
      const matchAudience = category.audienceType === audience;
      return matchKeyword && matchPriority && matchStatus && matchAudience;
    });
  }, [audience, keyword, priority, rows, status]);

  const totalPages = Math.max(1, Math.ceil(visibleRows.length / pageSize));

  useEffect(() => {
    setCurrentPage(1);
  }, [audience, keyword, pageSize, priority, status]);

  useEffect(() => {
    setCurrentPage((page) => Math.min(page, totalPages));
  }, [totalPages]);

  const pageStartIndex = (currentPage - 1) * pageSize;
  const paginatedRows = visibleRows.slice(pageStartIndex, pageStartIndex + pageSize);
  const activeCount = rows.filter((row) => row.status === "ACTIVE").length;
  const inactiveCount = rows.filter((row) => row.status === "INACTIVE").length;
  const openTicketCount = rows.reduce((total, row) => total + row.openTickets, 0);

  const handleOpenCreate = () => {
    setEditingCategory(null);
    setDrawerOpen(true);
    setError("");
  };

  const handleOpenEdit = (category: SupportCategoryRow) => {
    setEditingCategory(category);
    setDrawerOpen(true);
    setError("");
  };

  const handleSubmitCategory = async (payload: SaveSupportTicketCategoryRequest) => {
    setSaving(true);
    setError("");

    try {
      if (editingCategory) {
        await updateSupportTicketCategory(editingCategory.categoryId, payload);
        toast.success("Đã cập nhật danh mục hỗ trợ.");
      } else {
        await createSupportTicketCategory(payload);
        toast.success("Đã tạo danh mục hỗ trợ.");
      }
      setDrawerOpen(false);
      setEditingCategory(null);
      await loadData();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể lưu danh mục.");
    } finally {
      setSaving(false);
    }
  };

  const handleDeactivate = async () => {
    if (!deactivatingCategory) return;
    setSaving(true);
    setError("");

    try {
      await deactivateSupportTicketCategory(deactivatingCategory.categoryId);
      toast.success("Đã ngưng sử dụng danh mục.");
      setDeactivatingCategory(null);
      await loadData();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể ngưng sử dụng danh mục.");
    } finally {
      setSaving(false);
    }
  };

  const handleReactivate = async (category: SupportCategoryRow) => {
    setSaving(true);
    setError("");

    try {
      await activateSupportTicketCategory(category.categoryId);
      toast.success("Đã kích hoạt danh mục.");
      await loadData();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể kích hoạt danh mục.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="tw-px-4 tw-py-4 lg:tw-px-5">
      <section className="tw-mx-auto tw-min-h-[calc(100vh-104px)] tw-w-[min(100%,1500px)] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-vm-card">
        <header className="tw-flex tw-items-start tw-justify-between tw-gap-4">
          <div>
            <h1 className="tw-m-0 tw-text-vm-page-title tw-text-vm-slate-900">Danh mục hỗ trợ & Quy trình xử lý yêu cầu</h1>
          </div>
          <div className="tw-flex tw-gap-2">
            <Button variant="secondary" disabled={loading} onClick={() => void loadData()}>
              <i className="fas fa-sync-alt" />
              Làm mới
            </Button>
            {canCreate ? <Button onClick={handleOpenCreate}>
              <i className="fas fa-plus" />
              Thêm danh mục
            </Button> : null}
          </div>
        </header>

        <div className="tw-mt-6 tw-grid tw-grid-cols-4 tw-gap-3 max-[1280px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
          <MetricCard icon="fas fa-list-ul" iconClassName="tw-bg-brand-50 tw-text-vm-primary" label="Tổng danh mục" value={rows.length.toLocaleString("vi-VN")} />
          <MetricCard icon="far fa-check-circle" iconClassName="tw-bg-green-50 tw-text-green-600" label="Đang hoạt động" meta="Có thể tạo yêu cầu" metaClassName="tw-text-green-600" value={activeCount.toLocaleString("vi-VN")} />
          <MetricCard icon="fas fa-user-slash" iconClassName="tw-bg-red-50 tw-text-red-500" label="Ngưng sử dụng" meta="Ẩn khỏi biểu mẫu tạo mới" metaClassName="tw-text-red-500" value={inactiveCount.toLocaleString("vi-VN")} />
          <MetricCard icon="fas fa-ticket-alt" iconClassName="tw-bg-orange-50 tw-text-orange-500" label="Yêu cầu đang mở" meta="Đang mở / Đang xử lý" metaClassName="tw-text-vm-primary" value={openTicketCount.toLocaleString("vi-VN")} />
        </div>

        <div className="tw-mt-5 tw-grid tw-grid-cols-[minmax(240px,1fr)_180px_190px_120px] tw-gap-3 max-[1180px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
          <label className="tw-flex tw-h-10 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3">
            <input className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.82rem] tw-font-semibold tw-outline-none placeholder:tw-text-vm-slate-500" placeholder="Tìm theo mã, tên, mô tả..." value={keyword} onChange={(event) => setKeyword(event.target.value)} />
            <i className="fas fa-search tw-text-vm-slate-500" />
          </label>
          <SelectMenu ariaLabel="Ưu tiên" options={priorityOptions} value={priority} onChange={setPriority} />
          <SelectMenu ariaLabel="Trạng thái" options={statusOptions} value={status} onChange={setStatus} />
          <button type="button" className="tw-flex tw-h-10 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-700" onClick={() => { setKeyword(""); setPriority("all"); setStatus("all"); }}>
            <i className="fas fa-sync-alt" /> Xóa lọc
          </button>
        </div>

        <div className="tw-mt-4 tw-flex tw-w-fit tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-1" role="tablist" aria-label="Nhóm danh mục hỗ trợ">
          <button type="button" role="tab" aria-selected={audience === "CUSTOMER"} className={cn("tw-rounded-vm-sm tw-border-0 tw-px-4 tw-py-2 tw-text-[0.8rem] tw-font-black", audience === "CUSTOMER" ? "tw-bg-white tw-text-vm-primary tw-shadow-sm" : "tw-bg-transparent tw-text-vm-slate-500")} onClick={() => setAudience("CUSTOMER")}>Khách hàng</button>
          <button type="button" role="tab" aria-selected={audience === "INTERNAL"} className={cn("tw-rounded-vm-sm tw-border-0 tw-px-4 tw-py-2 tw-text-[0.8rem] tw-font-black", audience === "INTERNAL" ? "tw-bg-white tw-text-vm-primary tw-shadow-sm" : "tw-bg-transparent tw-text-vm-slate-500")} onClick={() => setAudience("INTERNAL")}>Đối tác & nhân sự</button>
        </div>

        {error ? <div className="tw-mt-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-px-3 tw-py-2 tw-text-[0.82rem] tw-font-semibold tw-text-red-700">{error}</div> : null}

        <div className="tw-mt-5">
          <CategoryTable
            canActivate={canActivate}
            canEdit={canUpdate}
            currentPage={currentPage}
            loading={loading}
            onDeactivate={setDeactivatingCategory}
            onEdit={handleOpenEdit}
            onPageChange={setCurrentPage}
            onPageSizeChange={setPageSize}
            onReactivate={handleReactivate}
            pageSize={pageSize}
            rows={paginatedRows}
            startIndex={pageStartIndex + 1}
            totalRecords={visibleRows.length}
          />
        </div>
      </section>

      <CategoryDrawer
        category={editingCategory}
        error={error}
        onClose={() => {
          setDrawerOpen(false);
          setEditingCategory(null);
          setError("");
        }}
        onSubmit={handleSubmitCategory}
        open={drawerOpen}
        permissionOptions={permissionOptions}
        saving={saving}
      />
      {deactivatingCategory ? <DeactivateModal category={deactivatingCategory} onClose={() => setDeactivatingCategory(null)} onConfirm={handleDeactivate} saving={saving} /> : null}
    </div>
  );
}
