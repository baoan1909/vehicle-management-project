import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { Drawer } from "@/shared/components/ui/Drawer";
import {
  CatalogFilterSelect,
  CatalogHeader,
  CatalogMetricGrid,
  CatalogPagination,
  CatalogStatusTabs,
  CatalogToolbar,
  TicketCatalogTable,
  TicketDetailPanel,
  VehicleCatalogGrid,
  VehicleDetailPanel,
  getStatusCounts,
  type CatalogMetric
} from "@/features/catalog/components/CatalogManageComponents";
import {
  type CatalogStatus,
  type CatalogStatusTabValue,
  type TicketCatalogRecord,
  type VehicleCatalogRecord
} from "@/features/catalog/components/catalogManageData";
import {
  activateTicketType,
  createTicketType,
  deactivateTicketType,
  getTicketTypes,
  updateTicketType,
  type CreateTicketTypeRequest,
  type TicketTypeApiResponse,
  type UpdateTicketTypeRequest
} from "@/features/catalog/api/ticketTypesApi";
import {
  activateVehicleType,
  createVehicleType,
  deactivateVehicleType,
  getVehicleTypes,
  updateVehicleType,
  type CreateVehicleTypeRequest,
  type UpdateVehicleTypeRequest,
  type VehicleTypeApiResponse
} from "@/features/catalog/api/vehicleTypesApi";
import { getPriceRules } from "@/features/pricing/api/pricingApi";

function matchesText(values: Array<string | number | undefined>, searchValue: string) {
  if (!searchValue.trim()) return true;

  const search = searchValue.trim().toLowerCase();
  return values.some((value) => String(value ?? "").toLowerCase().includes(search));
}

function getPageItems<T>(records: T[], currentPage: number, pageSize: number) {
  const totalPages = Math.max(1, Math.ceil(records.length / pageSize));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const startIndex = (safeCurrentPage - 1) * pageSize;

  return {
    endDisplayIndex: records.length === 0 ? 0 : startIndex + records.slice(startIndex, startIndex + pageSize).length,
    items: records.slice(startIndex, startIndex + pageSize),
    safeCurrentPage,
    startDisplayIndex: records.length === 0 ? 0 : startIndex + 1,
    totalPages
  };
}

const ticketTypeCodeOptions = [
  { label: "Vé lượt", value: "DAILY" },
  { label: "Vé tháng", value: "MONTHLY" },
  { label: "Vé quý", value: "QUARTERLY" },
  { label: "Vé năm", value: "YEARLY" },
  { label: "Miễn phí", value: "FREE" }
];

function formatDateTimeParts(value: string | null | undefined) {
  if (!value) {
    return { date: "-", time: "-" };
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    const [datePart, timePart] = value.split(" ");
    return {
      date: datePart ?? "-",
      time: timePart?.slice(0, 5) ?? "-"
    };
  }

  return {
    date: new Intl.DateTimeFormat("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric"
    }).format(parsed),
    time: new Intl.DateTimeFormat("vi-VN", {
      hour: "2-digit",
      hour12: false,
      minute: "2-digit"
    }).format(parsed)
  };
}

function formatDuration(days: number | null | undefined) {
  if (!days) return "-";
  return `${new Intl.NumberFormat("vi-VN").format(days)} ngày`;
}

function getTicketTypeDisplayName(code: string) {
  return ticketTypeCodeOptions.find((option) => option.value === code)?.label ?? code;
}

function mapTicketType(row: TicketTypeApiResponse, priceRuleCounts: Map<string, number>): TicketCatalogRecord {
  const updated = formatDateTimeParts(row.updatedAt ?? row.createdAt);

  return {
    id: row.ticketTypeId,
    code: row.code,
    createdAt: row.createdAt ?? "",
    createdBy: row.createdBy,
    description: row.description ?? "",
    duration: formatDuration(row.durationDays),
    durationDays: row.durationDays,
    name: row.name || getTicketTypeDisplayName(row.code),
    priceRuleCount: priceRuleCounts.get(row.ticketTypeId) ?? 0,
    status: row.status === "INACTIVE" ? "inactive" : "active",
    updatedAt: updated.date,
    updatedBy: row.updatedBy,
    updatedTime: updated.time
  };
}

function buildTicketMetrics(records: TicketCatalogRecord[]): CatalogMetric[] {
  return [
    { label: "Tổng loại vé", value: String(records.length), delta: "Theo dữ liệu API", tone: "blue", icon: "ticket" },
    { label: "Đang hoạt động", value: String(records.filter((row) => row.status === "active").length), delta: "Có thể áp dụng", tone: "green", icon: "check" },
    { label: "Ngừng dùng", value: String(records.filter((row) => row.status === "inactive").length), delta: "Không dùng cho cấu hình mới", tone: "red", icon: "x" }
  ];
}

function escapeCsv(value: string) {
  return `"${value.replace(/"/g, '""')}"`;
}

function exportTicketTypes(rows: TicketCatalogRecord[]) {
  const columns = [
    { label: "Mã loại vé", value: (row: TicketCatalogRecord) => row.code },
    { label: "Tên loại vé", value: (row: TicketCatalogRecord) => row.name },
    { label: "Thời hạn", value: (row: TicketCatalogRecord) => row.duration },
    { label: "Trạng thái", value: (row: TicketCatalogRecord) => (row.status === "active" ? "Đang hoạt động" : "Ngừng dùng") },
    { label: "Số rule giá", value: (row: TicketCatalogRecord) => String(row.priceRuleCount) },
    { label: "Mô tả", value: (row: TicketCatalogRecord) => row.description || "-" }
  ];
  const header = columns.map((column) => escapeCsv(column.label)).join(",");
  const body = rows.map((row) => columns.map((column) => escapeCsv(column.value(row))).join(",")).join("\n");
  const blob = new Blob(["\ufeff", [header, body].filter(Boolean).join("\n")], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");

  link.href = url;
  link.download = `loai-ve-${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function getVehicleIcon(code: string): VehicleCatalogRecord["icon"] {
  const normalizedCode = code.toUpperCase();

  if (normalizedCode.includes("MOTOR") || normalizedCode.includes("MOTO")) return "motorbike";
  if (normalizedCode.includes("BICYCLE") || normalizedCode.includes("BIKE")) return "bike";
  if (normalizedCode.includes("SCOOTER") || normalizedCode.includes("ELECTRIC")) return "scooter";
  if (normalizedCode.includes("HEAVY")) return "heavyTruck";
  if (normalizedCode.includes("TRUCK")) return "truck";
  return "car";
}

function mapVehicleType(row: VehicleTypeApiResponse, priceRuleCounts: Map<string, number>): VehicleCatalogRecord {
  const updated = formatDateTimeParts(row.updatedAt ?? row.createdAt);

  return {
    id: row.vehicleTypeId,
    code: row.code,
    createdAt: row.createdAt ?? "",
    createdBy: row.createdBy,
    description: row.description ?? "",
    icon: getVehicleIcon(row.code),
    isActive: row.isActive,
    linkedCount: 0,
    name: row.name,
    priceRuleCount: priceRuleCounts.get(row.vehicleTypeId) ?? 0,
    status: row.isActive === false ? "inactive" : "active",
    updatedAt: updated.date,
    updatedBy: row.updatedBy,
    updatedTime: updated.time
  };
}

function buildVehicleMetrics(records: VehicleCatalogRecord[]): CatalogMetric[] {
  return [
    { label: "Tổng loại xe", value: String(records.length), delta: "Theo dữ liệu API", tone: "blue", icon: "vehicle" },
    { label: "Đang hoạt động", value: String(records.filter((row) => row.status === "active").length), delta: "Có thể áp dụng", tone: "green", icon: "check" },
    { label: "Ngừng dùng", value: String(records.filter((row) => row.status === "inactive").length), delta: "Không dùng cho cấu hình mới", tone: "red", icon: "x" }
  ];
}

function exportVehicleTypes(rows: VehicleCatalogRecord[]) {
  const columns = [
    { label: "Mã loại xe", value: (row: VehicleCatalogRecord) => row.code },
    { label: "Tên loại xe", value: (row: VehicleCatalogRecord) => row.name },
    { label: "Trạng thái", value: (row: VehicleCatalogRecord) => (row.status === "active" ? "Đang hoạt động" : "Ngừng dùng") },
    { label: "Số xe liên kết", value: (row: VehicleCatalogRecord) => String(row.linkedCount) },
    { label: "Số rule giá", value: (row: VehicleCatalogRecord) => String(row.priceRuleCount) },
    { label: "Mô tả", value: (row: VehicleCatalogRecord) => row.description || "-" }
  ];
  const header = columns.map((column) => escapeCsv(column.label)).join(",");
  const body = rows.map((row) => columns.map((column) => escapeCsv(column.value(row))).join(",")).join("\n");
  const blob = new Blob(["\ufeff", [header, body].filter(Boolean).join("\n")], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");

  link.href = url;
  link.download = `loai-phuong-tien-${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

const statusOptions = [
  { label: "Tất cả", value: "all" },
  { label: "Đang hoạt động", value: "active" },
  { label: "Ngừng dùng", value: "inactive" }
];

const priceRuleOptions = [
  { label: "Tất cả", value: "all" },
  { label: "Đã áp dụng giá", value: "has-price" },
  { label: "Chưa có giá", value: "no-price" }
];

function CatalogPageShell({ children }: { children: ReactNode }) {
  return (
    <div className="content-header tw-px-0 tw-pb-4 tw-pt-3">
      <section className="content tw-pb-8">
        <div className="container-fluid tw-max-w-[1480px]">
          <div className="tw-grid tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4 tw-shadow-[0_16px_34px_rgba(15,23,42,0.04)]">{children}</div>
        </div>
      </section>
    </div>
  );
}

type TicketTypeFormDrawerProps = {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (payload: CreateTicketTypeRequest) => Promise<void>;
  onUpdate: (id: string, payload: UpdateTicketTypeRequest) => Promise<void>;
  row: TicketCatalogRecord | null;
};

function TicketTypeFormDrawer({ isOpen, onClose, onCreate, onUpdate, row }: TicketTypeFormDrawerProps) {
  const [form, setForm] = useState({ code: "DAILY", description: "", name: "" });
  const [formError, setFormError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const isEditMode = Boolean(row);

  useEffect(() => {
    if (!isOpen) return;

    setForm({
      code: row?.code ?? "DAILY",
      description: row?.description ?? "",
      name: row?.name ?? ""
    });
    setFormError("");
    setIsSubmitting(false);
  }, [isOpen, row]);

  const codeOptions = ticketTypeCodeOptions.some((option) => option.value === form.code)
    ? ticketTypeCodeOptions
    : [{ label: form.code, value: form.code }, ...ticketTypeCodeOptions];

  const updateField = (field: keyof typeof form, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
    setFormError("");
  };

  const validateForm = () => {
    if (!form.code.trim()) return "Vui lòng chọn mã loại vé.";
    if (!form.name.trim()) return "Vui lòng nhập tên loại vé.";
    return "";
  };

  const handleSubmit = async () => {
    const validationMessage = validateForm();
    if (validationMessage) {
      setFormError(validationMessage);
      return;
    }

    const payload = {
      code: form.code.trim().toUpperCase(),
      description: form.description.trim() || null,
      name: form.name.trim()
    };

    try {
      setIsSubmitting(true);
      setFormError("");

      if (row) {
        await onUpdate(row.id, payload);
      } else {
        await onCreate(payload);
      }

      onClose();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "Không thể lưu loại vé.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Drawer
      open={isOpen}
      title={isEditMode ? "Cập nhật loại vé" : "Thêm loại vé"}
      description="Loại vé dùng cho bảng giá và luồng đăng ký vé."
      width="md"
      onClose={onClose}
      actions={
        <div className="tw-grid tw-grid-cols-2 tw-gap-3">
          <button className="tw-min-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-font-extrabold tw-text-vm-slate-700" type="button" onClick={onClose}>
            Hủy
          </button>
          <button className="tw-min-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-vm-primary tw-font-extrabold tw-text-white disabled:tw-cursor-not-allowed disabled:tw-opacity-60" type="button" disabled={isSubmitting} onClick={handleSubmit}>
            {isSubmitting ? "Đang lưu..." : isEditMode ? "Lưu thay đổi" : "Thêm loại vé"}
          </button>
        </div>
      }
    >
      <div className="tw-grid tw-gap-4">
        {formError ? (
          <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-px-4 tw-py-3 tw-text-[0.88rem] tw-font-bold tw-text-red-600">
            {formError}
          </div>
        ) : null}
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-700">Mã loại vé</span>
          <select
            className="tw-min-h-12 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-font-bold tw-text-vm-slate-900 focus:tw-border-vm-primary focus:tw-outline-none focus:tw-shadow-vm-focus"
            value={form.code}
            onChange={(event) => updateField("code", event.target.value)}
          >
            {codeOptions.map((option) => (
              <option key={option.value} value={option.value}>{option.label} ({option.value})</option>
            ))}
          </select>
          <span className="tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Thời hạn được backend tự xác định theo mã loại vé.</span>
        </label>
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-700">Tên loại vé</span>
          <input
            className="tw-min-h-12 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-font-bold tw-text-vm-slate-900 focus:tw-border-vm-primary focus:tw-outline-none focus:tw-shadow-vm-focus"
            value={form.name}
            onChange={(event) => updateField("name", event.target.value)}
          />
        </label>
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-700">Mô tả</span>
          <textarea
            className="tw-min-h-[120px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-3 tw-font-bold tw-text-vm-slate-900 focus:tw-border-vm-primary focus:tw-outline-none focus:tw-shadow-vm-focus"
            value={form.description}
            onChange={(event) => updateField("description", event.target.value)}
          />
        </label>
      </div>
    </Drawer>
  );
}

type VehicleTypeFormDrawerProps = {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (payload: CreateVehicleTypeRequest) => Promise<void>;
  onUpdate: (id: string, payload: UpdateVehicleTypeRequest) => Promise<void>;
  row: VehicleCatalogRecord | null;
};

function VehicleTypeFormDrawer({ isOpen, onClose, onCreate, onUpdate, row }: VehicleTypeFormDrawerProps) {
  const [form, setForm] = useState({ code: "", description: "", isActive: "true", name: "" });
  const [formError, setFormError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const isEditMode = Boolean(row);

  useEffect(() => {
    if (!isOpen) return;

    setForm({
      code: row?.code ?? "",
      description: row?.description ?? "",
      isActive: row?.status === "inactive" ? "false" : "true",
      name: row?.name ?? ""
    });
    setFormError("");
    setIsSubmitting(false);
  }, [isOpen, row]);

  const updateField = (field: keyof typeof form, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
    setFormError("");
  };

  const validateForm = () => {
    if (!form.code.trim()) return "Vui lòng nhập mã loại xe.";
    if (!form.name.trim()) return "Vui lòng nhập tên loại xe.";
    return "";
  };

  const handleSubmit = async () => {
    const validationMessage = validateForm();
    if (validationMessage) {
      setFormError(validationMessage);
      return;
    }

    const payload = {
      code: form.code.trim().toUpperCase(),
      description: form.description.trim() || null,
      isActive: form.isActive === "true",
      name: form.name.trim()
    };

    try {
      setIsSubmitting(true);
      setFormError("");

      if (row) {
        await onUpdate(row.id, payload);
      } else {
        await onCreate(payload);
      }

      onClose();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "Không thể lưu loại phương tiện.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Drawer
      open={isOpen}
      title={isEditMode ? "Cập nhật loại phương tiện" : "Thêm loại phương tiện"}
      description="Loại phương tiện dùng cho luồng vào ra, khu vực gửi xe và quy tắc giá."
      width="md"
      onClose={onClose}
      actions={
        <div className="tw-grid tw-grid-cols-2 tw-gap-3">
          <button className="tw-min-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-font-extrabold tw-text-vm-slate-700" type="button" onClick={onClose}>
            Hủy
          </button>
          <button className="tw-min-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-vm-primary tw-font-extrabold tw-text-white disabled:tw-cursor-not-allowed disabled:tw-opacity-60" type="button" disabled={isSubmitting} onClick={handleSubmit}>
            {isSubmitting ? "Đang lưu..." : isEditMode ? "Lưu thay đổi" : "Thêm loại xe"}
          </button>
        </div>
      }
    >
      <div className="tw-grid tw-gap-4">
        {formError ? (
          <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-px-4 tw-py-3 tw-text-[0.88rem] tw-font-bold tw-text-red-600">
            {formError}
          </div>
        ) : null}
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-700">Mã loại xe</span>
          <input
            className="tw-min-h-12 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-font-bold tw-uppercase tw-text-vm-slate-900 focus:tw-border-vm-primary focus:tw-outline-none focus:tw-shadow-vm-focus"
            value={form.code}
            onChange={(event) => updateField("code", event.target.value)}
            placeholder="VD: MOTORBIKE"
          />
        </label>
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-700">Tên loại xe</span>
          <input
            className="tw-min-h-12 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-font-bold tw-text-vm-slate-900 focus:tw-border-vm-primary focus:tw-outline-none focus:tw-shadow-vm-focus"
            value={form.name}
            onChange={(event) => updateField("name", event.target.value)}
          />
        </label>
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-700">Trạng thái</span>
          <select
            className="tw-min-h-12 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-font-bold tw-text-vm-slate-900 focus:tw-border-vm-primary focus:tw-outline-none focus:tw-shadow-vm-focus"
            value={form.isActive}
            onChange={(event) => updateField("isActive", event.target.value)}
          >
            <option value="true">Đang hoạt động</option>
            <option value="false">Ngừng dùng</option>
          </select>
        </label>
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-700">Mô tả</span>
          <textarea
            className="tw-min-h-[120px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-3 tw-font-bold tw-text-vm-slate-900 focus:tw-border-vm-primary focus:tw-outline-none focus:tw-shadow-vm-focus"
            value={form.description}
            onChange={(event) => updateField("description", event.target.value)}
          />
        </label>
      </div>
    </Drawer>
  );
}

export function TicketListPage() {
  const [records, setRecords] = useState<TicketCatalogRecord[]>([]);
  const [activeStatus, setActiveStatus] = useState<CatalogStatusTabValue>("all");
  const [statusValue, setStatusValue] = useState("all");
  const [priceRuleValue, setPriceRuleValue] = useState("all");
  const [searchValue, setSearchValue] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [editingRecord, setEditingRecord] = useState<TicketCatalogRecord | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const filteredRecords = records.filter((row) => {
    const matchesTab = activeStatus === "all" ? true : row.status === activeStatus;
    const matchesStatus = statusValue === "all" ? true : row.status === (statusValue as CatalogStatus);
    const matchesPrice =
      priceRuleValue === "all" ? true : priceRuleValue === "has-price" ? row.priceRuleCount > 0 : row.priceRuleCount === 0;

    return matchesTab && matchesStatus && matchesPrice && matchesText([row.code, row.name, row.duration, row.description], searchValue);
  });

  const page = getPageItems<TicketCatalogRecord>(filteredRecords, currentPage, pageSize);
  const effectiveSelectedId = filteredRecords.some((row) => row.id === selectedId) ? selectedId : filteredRecords[0]?.id ?? null;
  const selectedRecord = filteredRecords.find((row) => row.id === effectiveSelectedId) ?? null;

  const loadTicketTypes = async () => {
    setIsLoading(true);
    setErrorMessage("");

    try {
      const [ticketResponse, priceRuleResponse] = await Promise.all([getTicketTypes(), getPriceRules()]);
      const priceRuleCounts = new Map<string, number>();

      priceRuleResponse.data.forEach((rule) => {
        if (!rule.ticketTypeId || rule.isActive === false) return;
        priceRuleCounts.set(rule.ticketTypeId, (priceRuleCounts.get(rule.ticketTypeId) ?? 0) + 1);
      });

      const mappedRecords = ticketResponse.data.map((ticketType) => mapTicketType(ticketType, priceRuleCounts));
      setRecords(mappedRecords);
      setSelectedId((currentId) => currentId ?? mappedRecords[0]?.id ?? null);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Không thể tải danh sách loại vé.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (currentPage !== page.safeCurrentPage) {
      setCurrentPage(page.safeCurrentPage);
    }
  }, [currentPage, page.safeCurrentPage]);

  useEffect(() => {
    void loadTicketTypes();
  }, []);

  const resetFilters = () => {
    setActiveStatus("all");
    setStatusValue("all");
    setPriceRuleValue("all");
    setSearchValue("");
    setCurrentPage(1);
  };

  const upsertRecord = (record: TicketCatalogRecord) => {
    setRecords((currentRecords) => {
      const exists = currentRecords.some((currentRecord) => currentRecord.id === record.id);
      if (exists) {
        return currentRecords.map((currentRecord) => (currentRecord.id === record.id ? record : currentRecord));
      }

      return [record, ...currentRecords];
    });
    setSelectedId(record.id);
  };

  const mapSingleTicketType = (ticketType: TicketTypeApiResponse) => {
    const existingPriceRuleCounts = new Map(records.map((record) => [record.id, record.priceRuleCount]));
    return mapTicketType(ticketType, existingPriceRuleCounts);
  };

  const handleOpenCreate = () => {
    setEditingRecord(null);
    setIsFormOpen(true);
  };

  const handleOpenEdit = (row: TicketCatalogRecord) => {
    setEditingRecord(row);
    setIsFormOpen(true);
  };

  const handleCreateTicketType = async (payload: CreateTicketTypeRequest) => {
    const response = await createTicketType(payload);
    upsertRecord(mapSingleTicketType(response.data));
    setSuccessMessage(response.message || "Thêm loại vé thành công.");
    setCurrentPage(1);
  };

  const handleUpdateTicketType = async (id: string, payload: UpdateTicketTypeRequest) => {
    const response = await updateTicketType(id, payload);
    upsertRecord(mapSingleTicketType(response.data));
    setSuccessMessage(response.message || "Cập nhật loại vé thành công.");
  };

  const handleActivateTicketType = async (row: TicketCatalogRecord) => {
    try {
      setErrorMessage("");
      const response = await activateTicketType(row.id);
      upsertRecord(mapSingleTicketType(response.data));
      setSuccessMessage(response.message || "Kích hoạt loại vé thành công.");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Không thể kích hoạt loại vé.");
    }
  };

  const handleDeactivateTicketType = async (row: TicketCatalogRecord) => {
    try {
      setErrorMessage("");
      const response = await deactivateTicketType(row.id);
      upsertRecord({ ...row, status: "inactive" });
      setSuccessMessage(response.message || "Ngừng dùng loại vé thành công.");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Không thể ngừng dùng loại vé.");
    }
  };

  return (
    <CatalogPageShell>
      <CatalogHeader createLabel="Thêm loại vé" title="Loại vé" onCreateClick={handleOpenCreate} onExportClick={() => exportTicketTypes(filteredRecords)} />
      <CatalogMetricGrid items={buildTicketMetrics(records)} />
      <CatalogStatusTabs activeValue={activeStatus} counts={getStatusCounts(records)} onChange={(value) => {
        setActiveStatus(value);
        setCurrentPage(1);
      }} />

      <div className="tw-grid tw-grid-cols-[minmax(0,1fr)_minmax(290px,0.34fr)] tw-items-start tw-gap-[0.9rem] max-[1360px]:tw-grid-cols-1">
        <main className="tw-min-w-0 tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-[0_14px_36px_rgba(15,23,42,0.05)]">
          <CatalogToolbar
            variant="ticket"
            searchPlaceholder="Tìm mã, tên loại vé..."
            searchValue={searchValue}
            onSearchChange={(value) => {
              setSearchValue(value);
              setCurrentPage(1);
            }}
            onReset={resetFilters}
          >
            <CatalogFilterSelect
              label="Trạng thái"
              options={statusOptions}
              value={statusValue}
              onChange={(value) => {
                setStatusValue(value);
                setCurrentPage(1);
              }}
            />
            <CatalogFilterSelect
              label="Áp dụng giá"
              options={priceRuleOptions}
              value={priceRuleValue}
              onChange={(value) => {
                setPriceRuleValue(value);
                setCurrentPage(1);
              }}
            />
          </CatalogToolbar>

          {successMessage ? (
            <div className="tw-m-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-green-100 tw-bg-green-50 tw-px-4 tw-py-3 tw-text-[0.9rem] tw-font-bold tw-text-green-700">
              {successMessage}
            </div>
          ) : null}
          {errorMessage ? (
            <div className="tw-m-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-px-4 tw-py-3 tw-text-[0.9rem] tw-font-bold tw-text-red-600">
              {errorMessage}
            </div>
          ) : null}
          {isLoading ? (
            <div className="tw-px-4 tw-py-5 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-500">Đang tải dữ liệu loại vé...</div>
          ) : null}
          <TicketCatalogTable rows={page.items} selectedId={effectiveSelectedId} onSelect={setSelectedId} />
          <CatalogPagination
            currentPage={page.safeCurrentPage}
            endIndex={page.endDisplayIndex}
            onPageChange={setCurrentPage}
            onPageSizeChange={(value) => {
              setPageSize(value);
              setCurrentPage(1);
            }}
            pageSize={pageSize}
            startIndex={page.startDisplayIndex}
            totalPages={page.totalPages}
            totalRecords={filteredRecords.length}
          />
        </main>

        <TicketDetailPanel row={selectedRecord} onEdit={handleOpenEdit} onActivate={handleActivateTicketType} onDeactivate={handleDeactivateTicketType} />
      </div>
      <TicketTypeFormDrawer
        isOpen={isFormOpen}
        row={editingRecord}
        onClose={() => setIsFormOpen(false)}
        onCreate={handleCreateTicketType}
        onUpdate={handleUpdateTicketType}
      />
    </CatalogPageShell>
  );
}

export function VehicleListPage() {
  const [records, setRecords] = useState<VehicleCatalogRecord[]>([]);
  const [statusValue, setStatusValue] = useState("all");
  const [searchValue, setSearchValue] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [editingRecord, setEditingRecord] = useState<VehicleCatalogRecord | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const filteredRecords = records.filter((row) => {
    const matchesStatus = statusValue === "all" ? true : row.status === (statusValue as CatalogStatus);

    return matchesStatus && matchesText([row.code, row.name, row.description], searchValue);
  });

  const page = getPageItems<VehicleCatalogRecord>(filteredRecords, currentPage, pageSize);
  const effectiveSelectedId = filteredRecords.some((row) => row.id === selectedId) ? selectedId : filteredRecords[0]?.id ?? null;
  const selectedRecord = filteredRecords.find((row) => row.id === effectiveSelectedId) ?? null;

  const loadVehicleTypes = async () => {
    setIsLoading(true);
    setErrorMessage("");

    try {
      const [vehicleResponse, priceRuleResponse] = await Promise.all([getVehicleTypes(), getPriceRules()]);
      const priceRuleCounts = new Map<string, number>();

      priceRuleResponse.data.forEach((rule) => {
        if (!rule.vehicleTypeId || rule.isActive === false) return;
        priceRuleCounts.set(rule.vehicleTypeId, (priceRuleCounts.get(rule.vehicleTypeId) ?? 0) + 1);
      });

      const mappedRecords = vehicleResponse.data.map((vehicleType) => mapVehicleType(vehicleType, priceRuleCounts));
      setRecords(mappedRecords);
      setSelectedId((currentId) => currentId ?? mappedRecords[0]?.id ?? null);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Không thể tải danh sách loại phương tiện.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (currentPage !== page.safeCurrentPage) {
      setCurrentPage(page.safeCurrentPage);
    }
  }, [currentPage, page.safeCurrentPage]);

  useEffect(() => {
    void loadVehicleTypes();
  }, []);

  const resetFilters = () => {
    setStatusValue("all");
    setSearchValue("");
    setCurrentPage(1);
  };

  const upsertRecord = (record: VehicleCatalogRecord) => {
    setRecords((currentRecords) => {
      const exists = currentRecords.some((currentRecord) => currentRecord.id === record.id);
      if (exists) {
        return currentRecords.map((currentRecord) => (currentRecord.id === record.id ? record : currentRecord));
      }

      return [record, ...currentRecords];
    });
    setSelectedId(record.id);
  };

  const mapSingleVehicleType = (vehicleType: VehicleTypeApiResponse) => {
    const existingPriceRuleCounts = new Map(records.map((record) => [record.id, record.priceRuleCount]));
    return mapVehicleType(vehicleType, existingPriceRuleCounts);
  };

  const handleOpenCreate = () => {
    setEditingRecord(null);
    setIsFormOpen(true);
  };

  const handleOpenEdit = (row: VehicleCatalogRecord) => {
    setEditingRecord(row);
    setIsFormOpen(true);
  };

  const handleCreateVehicleType = async (payload: CreateVehicleTypeRequest) => {
    const response = await createVehicleType(payload);
    upsertRecord(mapSingleVehicleType(response.data));
    setSuccessMessage(response.message || "Thêm loại phương tiện thành công.");
    setCurrentPage(1);
  };

  const handleUpdateVehicleType = async (id: string, payload: UpdateVehicleTypeRequest) => {
    const response = await updateVehicleType(id, payload);
    upsertRecord(mapSingleVehicleType(response.data));
    setSuccessMessage(response.message || "Cập nhật loại phương tiện thành công.");
  };

  const handleActivateVehicleType = async (row: VehicleCatalogRecord) => {
    try {
      setErrorMessage("");
      const response = await activateVehicleType(row.id);
      upsertRecord(mapSingleVehicleType(response.data));
      setSuccessMessage(response.message || "Kích hoạt loại phương tiện thành công.");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Không thể kích hoạt loại phương tiện.");
    }
  };

  const handleDeactivateVehicleType = async (row: VehicleCatalogRecord) => {
    try {
      setErrorMessage("");
      const response = await deactivateVehicleType(row.id);
      upsertRecord({ ...row, isActive: false, status: "inactive" });
      setSuccessMessage(response.message || "Ngừng dùng loại phương tiện thành công.");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Không thể ngừng dùng loại phương tiện.");
    }
  };

  return (
    <CatalogPageShell>
      <CatalogHeader createLabel="Thêm loại xe" title="Loại phương tiện" onCreateClick={handleOpenCreate} onExportClick={() => exportVehicleTypes(filteredRecords)} />
      <CatalogMetricGrid items={buildVehicleMetrics(records)} />

      <div className="tw-grid tw-grid-cols-[minmax(0,1fr)_minmax(300px,0.32fr)] tw-items-start tw-gap-[0.9rem] max-[1360px]:tw-grid-cols-1">
        <main className="tw-min-w-0 tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-[0_14px_36px_rgba(15,23,42,0.05)]">
          <CatalogToolbar
            variant="vehicle"
            searchPlaceholder="Tìm mã, tên loại xe..."
            searchValue={searchValue}
            onSearchChange={(value) => {
              setSearchValue(value);
              setCurrentPage(1);
            }}
            onReset={resetFilters}
          >
            <CatalogFilterSelect
              label="Trạng thái"
              options={statusOptions}
              value={statusValue}
              onChange={(value) => {
                setStatusValue(value);
                setCurrentPage(1);
              }}
            />
          </CatalogToolbar>

          {successMessage ? (
            <div className="tw-m-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-green-100 tw-bg-green-50 tw-px-4 tw-py-3 tw-text-[0.9rem] tw-font-bold tw-text-green-700">
              {successMessage}
            </div>
          ) : null}
          {errorMessage ? (
            <div className="tw-m-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-px-4 tw-py-3 tw-text-[0.9rem] tw-font-bold tw-text-red-600">
              {errorMessage}
            </div>
          ) : null}
          {isLoading ? (
            <div className="tw-px-4 tw-py-5 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-500">Đang tải dữ liệu loại phương tiện...</div>
          ) : null}
          <VehicleCatalogGrid rows={page.items} selectedId={effectiveSelectedId} onSelect={setSelectedId} />
          <CatalogPagination
            currentPage={page.safeCurrentPage}
            endIndex={page.endDisplayIndex}
            onPageChange={setCurrentPage}
            onPageSizeChange={(value) => {
              setPageSize(value);
              setCurrentPage(1);
            }}
            pageSize={pageSize}
            startIndex={page.startDisplayIndex}
            totalPages={page.totalPages}
            totalRecords={filteredRecords.length}
          />
        </main>

        <VehicleDetailPanel row={selectedRecord} onEdit={handleOpenEdit} onActivate={handleActivateVehicleType} onDeactivate={handleDeactivateVehicleType} />
      </div>
      <VehicleTypeFormDrawer
        isOpen={isFormOpen}
        row={editingRecord}
        onClose={() => setIsFormOpen(false)}
        onCreate={handleCreateVehicleType}
        onUpdate={handleUpdateVehicleType}
      />
    </CatalogPageShell>
  );
}
