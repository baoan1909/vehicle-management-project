import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";

import { Button, Card, ConfirmDialog, Modal, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { SearchInput } from "@/shared/components/ui/SearchInput";
import { SelectMenu } from "@/shared/components/ui/SelectMenu";
import { cn } from "@/lib/cn";
import { deleteParkingLot, getParkingLots, type ParkingLotApiResponse } from "@/features/parking/api/parkingLotsApi";
import {
  activateGate,
  activateLane,
  activateZone,
  closeGate,
  closeLane,
  closeZone,
  createGate,
  createLane,
  createZone,
  deleteGate,
  deleteLane,
  deleteZone,
  getGateById,
  getGates,
  getLaneById,
  getLanes,
  getZoneById,
  getZones,
  markGateMaintenance,
  markLaneMaintenance,
  markZoneMaintenance,
  updateGate,
  updateLane,
  updateZone,
  type GateApiResponse,
  type LaneApiResponse,
  type ZoneApiResponse,
} from "@/features/parking/api/parkingTopologyApi";
import { deleteParkingLevel, createParkingLevel, getLevelsByLot, type ParkingLevelApi } from "@/features/parking/api/parkingLayoutApi";
import { ParkingLotCreateWizard } from "@/features/parking/components/ParkingLotCreateWizard";
import { ParkingLotEditModal } from "@/features/parking/components/ParkingLotEditModal";
import { ParkingManagerAssignmentModal } from "@/features/parking/components/ParkingManagerAssignmentModal";
import { ParkingLotWorkspaceHeader } from "@/features/parking/components/ParkingLotWorkspaceHeader";
import { getVehicleTypes } from "@/features/catalog/api/vehicleTypesApi";
import { hasAnyPermission } from "@/shared/auth/permissions";

/* ------------------------------------------------------------------ */
/* Types (Level -> Zone -> Gate -> Lane)                               */
/* ------------------------------------------------------------------ */

type TopologyStatus = "ACTIVE" | "MAINTENANCE" | "CLOSED";
type LaneDirection = "IN" | "OUT";
type TrackingMode = "SPACE" | "CAPACITY";

type LevelNode = {
  id: string;
  code: string;
  name: string;
  status: TopologyStatus;
  elevation: number | null;
  displayOrder: number;
  zoneCount: number;
  totalCapacity: number;
};
type ZoneNode = {
  id: string;
  code: string;
  name: string;
  levelId: string;
  vehicleTypeIds: string[];
  trackingMode: TrackingMode;
  capacity: number;
  layoutStatus: string | null;
  layoutVersion: number | null;
  status: TopologyStatus;
  gateCount: number;
};
type GateNode = { id: string; code: string; name: string; status: TopologyStatus; zoneId: string; laneCount: number };
type LaneNode = { id: string; code: string; name: string; direction: LaneDirection; status: TopologyStatus; gateId: string };

type SelectedRef =
  | { kind: "level"; id: string }
  | { kind: "zone"; id: string }
  | { kind: "gate"; id: string }
  | { kind: "lane"; id: string };

type DeleteTarget =
  | { kind: "level"; id: string; label: string }
  | { kind: "lot"; id: string; label: string };

type CloseTarget =
  | { kind: "zone"; id: string; label: string }
  | { kind: "gate"; id: string; label: string }
  | { kind: "lane"; id: string; label: string };

type StatusTarget = CloseTarget & { current: TopologyStatus };

type FormModalState =
  | { type: "zone"; mode: "create" | "edit"; id?: string }
  | { type: "gate"; mode: "create" | "edit"; id?: string }
  | { type: "lane"; mode: "create" | "edit"; id?: string };

const STATUS_FILTERS = [
  { label: "Tất cả trạng thái", value: "all" },
  { label: "Hoạt động", value: "ACTIVE" },
  { label: "Bảo trì", value: "MAINTENANCE" },
  { label: "Đã đóng", value: "CLOSED" },
];

const TRACKING_MODE_OPTIONS = [
  { label: "Theo sức chứa (nhập tay)", value: "CAPACITY" },
  { label: "Theo từng ô (sơ đồ)", value: "SPACE" },
];

const DIRECTION_OPTIONS = [
  { label: "Làn vào (IN)", value: "IN" },
  { label: "Làn ra (OUT)", value: "OUT" },
];

const LIFECYCLE_STATUS_OPTIONS: Array<{ label: string; value: TopologyStatus }> = [
  { label: "Hoạt động (ACTIVE)", value: "ACTIVE" },
  { label: "Bảo trì (MAINTENANCE)", value: "MAINTENANCE" },
  { label: "Đã đóng (CLOSED)", value: "CLOSED" },
];

function normalizeStatus(value: unknown): TopologyStatus {
  return value === "ACTIVE" || value === "MAINTENANCE" || value === "CLOSED" ? value : "ACTIVE";
}

function statusLabel(s: TopologyStatus) {
  if (s === "ACTIVE") return "Hoạt động";
  if (s === "MAINTENANCE") return "Bảo trì";
  return "Đã đóng";
}

function statusTone(s: TopologyStatus): "green" | "amber" | "gray" {
  if (s === "ACTIVE") return "green";
  if (s === "MAINTENANCE") return "amber";
  return "gray";
}

/* ------------------------------------------------------------------ */
/* Small pills (khớp mockup)                                          */
/* ------------------------------------------------------------------ */

function StatusPill({ label, tone }: { label: string; tone: "green" | "amber" | "blue" | "gray" }) {
  return (
    <span
      className={cn(
        "tw-inline-flex tw-items-center tw-gap-1.5 tw-rounded-full tw-px-2.5 tw-py-1 tw-text-[0.72rem] tw-font-bold tw-leading-none",
        tone === "green" && "tw-bg-[#dcfce7] tw-text-[#15803d]",
        tone === "amber" && "tw-bg-[#fef3c7] tw-text-[#b45309]",
        tone === "blue" && "tw-bg-[#eff6ff] tw-text-[#2563eb]",
        tone === "gray" && "tw-bg-[#f1f5f9] tw-text-[#64748b]",
      )}
    >
      {tone === "green" || tone === "amber" ? (
        <span
          className={cn(
            "tw-h-[7px] tw-w-[7px] tw-rounded-full",
            tone === "green" && "tw-bg-[#16a34a]",
            tone === "amber" && "tw-bg-[#f59e0b]",
          )}
        />
      ) : null}
      {label}
    </span>
  );
}

function SpacePill() {
  return (
    <span className="tw-inline-flex tw-items-center tw-rounded-[5px] tw-bg-[#eff6ff] tw-px-1.5 tw-py-[3px] tw-text-[0.68rem] tw-font-extrabold tw-tracking-wide tw-text-[#2563eb]">
      SPACE
    </span>
  );
}

/* ------------------------------------------------------------------ */
/* Stat cards                                                         */
/* ------------------------------------------------------------------ */

function StatCard({ icon, iconWrap, value, label }: { icon: string; iconWrap: string; value: string; label: string }) {
  return (
    <Card className="tw-flex tw-items-center tw-gap-3.5 tw-px-5 tw-py-[18px]">
      <span className={cn("tw-inline-flex tw-h-[52px] tw-w-[52px] tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-[10px] tw-text-[1.35rem]", iconWrap)}>
        <i className={icon} aria-hidden="true" />
      </span>
      <span className="tw-min-w-0">
        <strong className="tw-block tw-text-[1.55rem] tw-font-extrabold tw-leading-none tw-text-[#0f172a]">{value}</strong>
        <span className="tw-mt-1.5 tw-block tw-text-[0.86rem] tw-font-medium tw-text-[#64748b]">{label}</span>
      </span>
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/* Tree rows                                                          */
/* ------------------------------------------------------------------ */

function RowShell({
  children,
  depth,
  selected,
}: {
  children: React.ReactNode;
  depth: number;
  selected?: boolean;
}) {
  return (
    <div
      className={cn(
        "tw-relative tw-flex tw-min-h-[52px] tw-items-center tw-gap-2.5 tw-rounded-[8px] tw-border tw-border-solid tw-px-3 tw-py-2",
        selected ? "tw-border-[#bfdbfe] tw-bg-[#eff6ff]" : "tw-border-[#e8eef7] tw-bg-white hover:tw-border-[#cbd5e1]",
      )}
      style={{ marginLeft: depth === 0 ? 0 : depth === 1 ? 26 : depth === 2 ? 52 : 78 }}
    >
      {children}
    </div>
  );
}

function Connector({ depth }: { depth: number }) {
  if (depth === 0) return null;
  return (
    <span
      aria-hidden="true"
      className="tw-pointer-events-none tw-absolute tw-border-0 tw-border-l tw-border-b tw-border-dashed tw-border-[#cbd5e1]"
      style={{
        left: -17,
        top: -8,
        width: 16,
        height: 30,
        borderTopLeftRadius: 0,
      }}
    />
  );
}

function ChevronBtn({ open, onClick, label }: { open: boolean; onClick: () => void; label: string }) {
  return (
    <button
      type="button"
      aria-label={label}
      onClick={onClick}
      className="tw-inline-flex tw-h-6 tw-w-6 tw-flex-shrink-0 tw-items-center tw-justify-center tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.72rem] tw-text-[#0f172a]"
    >
      <i className={cn("fas fa-chevron-down tw-transition-transform", !open && "tw--rotate-90")} aria-hidden="true" />
    </button>
  );
}

function IconBox({ icon, wrap }: { icon: string; wrap: string }) {
  return (
    <span className={cn("tw-inline-flex tw-h-7 tw-w-7 tw-flex-shrink-0 tw-items-center tw-justify-center tw-text-[0.95rem]", wrap)}>
      <i className={icon} aria-hidden="true" />
    </span>
  );
}

function RowMenu({
  label,
  onEdit,
  onChangeStatus,
  onClose,
}: {
  label: string;
  onEdit: () => void;
  onChangeStatus: () => void;
  onClose: () => void;
}) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!open) return undefined;
    const handlePointerDown = (event: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) setOpen(false);
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    window.addEventListener("mousedown", handlePointerDown);
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("mousedown", handlePointerDown);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [open ]);

  return (
    <div ref={rootRef} className="tw-relative tw-flex-shrink-0">
      <button
        type="button"
        aria-label={`Thao tác cho ${label}`}
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
        className="tw-inline-flex tw-h-7 tw-w-7 tw-items-center tw-justify-center tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.92rem] tw-text-[#94a3b8] hover:tw-text-[#475569]"
      >
        <i className="fas fa-ellipsis-v" aria-hidden="true" />
      </button>
      {open ? (
        <div className="tw-absolute tw-right-0 tw-top-[calc(100%+4px)] tw-z-30 tw-w-44 tw-rounded-[8px] tw-border tw-border-solid tw-border-[#e2e8f0] tw-bg-white tw-py-1 tw-shadow-lg">
          <button
            type="button"
            onClick={() => { setOpen(false); onEdit(); }}
            className="tw-flex tw-w-full tw-items-center tw-gap-2 tw-border-0 tw-bg-transparent tw-px-3 tw-py-2 tw-text-left tw-text-[0.84rem] tw-font-semibold tw-text-[#334155] hover:tw-bg-[#f1f5f9]"
          >
            <i className="far fa-edit tw-text-[0.8rem]" aria-hidden="true" /> Chỉnh sửa
          </button>
          <button
            type="button"
            onClick={() => { setOpen(false); onChangeStatus(); }}
            className="tw-flex tw-w-full tw-items-center tw-gap-2 tw-border-0 tw-bg-transparent tw-px-3 tw-py-2 tw-text-left tw-text-[0.84rem] tw-font-semibold tw-text-[#334155] hover:tw-bg-[#f1f5f9]"
          >
            <i className="fas fa-sync-alt tw-text-[0.8rem]" aria-hidden="true" /> Đổi trạng thái
          </button>
          <button
            type="button"
            onClick={() => { setOpen(false); onClose(); }}
            className="tw-flex tw-w-full tw-items-center tw-gap-2 tw-border-0 tw-bg-transparent tw-px-3 tw-py-2 tw-text-left tw-text-[0.84rem] tw-font-semibold tw-text-[#dc2626] hover:tw-bg-[#fef2f2]"
          >
            <i className="far fa-trash-alt tw-text-[0.8rem]" aria-hidden="true" /> Đóng
          </button>
        </div>
      ) : null}
    </div>
  );
}

function RowActions({ deleteBlockedTip, onDelete, onEdit, showDelete }: {
  deleteBlockedTip?: string;
  onDelete?: () => void;
  onEdit?: () => void;
  showDelete?: boolean;
}) {
  return (
    <span className="tw-ml-auto tw-flex tw-flex-shrink-0 tw-items-center tw-gap-4 tw-pl-3">
      <button
        type="button"
        aria-label="Chỉnh sửa"
        onClick={onEdit}
        className="tw-inline-flex tw-h-7 tw-w-7 tw-items-center tw-justify-center tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.92rem] tw-text-[#475569] hover:tw-text-[#2563eb]"
      >
        <i className="far fa-edit" aria-hidden="true" />
      </button>
      {showDelete ? (
        <span className="tw-group tw-relative tw-inline-flex">
          {deleteBlockedTip ? (
            <span className="tw-absolute tw-bottom-[calc(100%+8px)] tw-left-1/2 tw-z-10 tw-hidden tw-w-max tw-max-w-[240px] tw--translate-x-1/2 tw-rounded-[6px] tw-bg-[#0f172a] tw-px-2.5 tw-py-1.5 tw-text-center tw-text-[0.72rem] tw-font-semibold tw-text-white tw-shadow-lg group-hover:tw-block">
              {deleteBlockedTip}
              <span className="tw-absolute tw-left-1/2 tw-top-full tw-h-2 tw-w-2 tw--translate-x-1/2 tw--translate-y-1 tw-rotate-45 tw-bg-[#0f172a]" />
            </span>
          ) : null}
          <button
            type="button"
            aria-label="Xóa"
            title={deleteBlockedTip ?? "Xóa tầng"}
            disabled={Boolean(deleteBlockedTip)}
            onClick={onDelete}
            className={cn(
              "tw-inline-flex tw-h-7 tw-w-7 tw-items-center tw-justify-center tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.92rem] disabled:tw-cursor-not-allowed",
              deleteBlockedTip ? "tw-text-[#94a3b8]" : "tw-text-[#ef4444] hover:tw-text-[#dc2626]",
            )}
          >
            <i className="far fa-trash-alt" aria-hidden="true" />
          </button>
        </span>
      ) : (
        <button
          type="button"
          aria-label="Thêm thao tác"
          className="tw-inline-flex tw-h-7 tw-w-7 tw-items-center tw-justify-center tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.92rem] tw-text-[#94a3b8] hover:tw-text-[#475569]"
        >
          <i className="fas fa-ellipsis-v" aria-hidden="true" />
        </button>
      )}
    </span>
  );
}

/* ------------------------------------------------------------------ */
/* Form shared bits                                                   */
/* ------------------------------------------------------------------ */

function FormField({ label, required, error, children }: {
  label: string;
  required?: boolean;
  error?: string | null;
  children: React.ReactNode;
}) {
  return (
    <label className="tw-block">
      <span className="tw-mb-1 tw-flex tw-items-center tw-gap-1 tw-text-[0.84rem] tw-font-bold tw-text-[#0f172a]">
        {label}
        {required ? <span className="tw-text-red-500">*</span> : null}
      </span>
      {children}
      {error ? (
        <span className="tw-mt-1 tw-block tw-text-[0.78rem] tw-font-semibold tw-text-red-600">{error}</span>
      ) : null}
    </label>
  );
}

function fieldInputClass(hasError: boolean) {
  return cn(
    "tw-h-10 tw-w-full tw-rounded-[8px] tw-border tw-border-solid tw-bg-white tw-px-3 tw-text-[0.9rem] tw-font-medium tw-text-[#0f172a] tw-outline-none focus:tw-border-[#2563eb] disabled:tw-cursor-not-allowed disabled:tw-bg-[#f8fafc] disabled:tw-text-[#94a3b8]",
    hasError ? "tw-border-red-300" : "tw-border-[#dbe3f0]",
  );
}

/* ------------------------------------------------------------------ */
/* Zone form modal                                                    */
/* ------------------------------------------------------------------ */

export type ZoneFormPayload = {
  parkingLevelId: string;
  name: string;
  vehicleTypeIds: string[];
  capacity: number;
  trackingMode: TrackingMode;
};

function ZoneFormModal({ open, title, levels, vehicleTypes, initial, defaultLevelId, submitting, serverError, onSubmit, onClose }: {
  open: boolean;
  title: string;
  levels: LevelNode[];
  vehicleTypes: Array<{ id: string; name: string }>;
  initial: ZoneNode | null;
  defaultLevelId: string;
  submitting: boolean;
  serverError: string | null;
  onSubmit: (payload: ZoneFormPayload) => void;
  onClose: () => void;
}) {
  const [levelId, setLevelId] = useState("");
  const [name, setName] = useState("");
  const [selectedVehicleTypes, setSelectedVehicleTypes] = useState<string[]>([]);
  const [trackingMode, setTrackingMode] = useState<TrackingMode>("CAPACITY");
  const [capacity, setCapacity] = useState("0");
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!open) return;
    setLevelId(initial?.levelId ?? defaultLevelId ?? levels[0]?.id ?? "");
    setName(initial?.name ?? "");
    setSelectedVehicleTypes(initial?.vehicleTypeIds ?? []);
    setTrackingMode(initial?.trackingMode ?? "CAPACITY");
    setCapacity(String(initial?.capacity ?? 0));
    setErrors({});
  }, [open, initial, defaultLevelId, levels]);

  function toggleVehicleType(vehicleTypeId: string) {
    setSelectedVehicleTypes((current) =>
      current.includes(vehicleTypeId) ? current.filter((id) => id !== vehicleTypeId) : [...current, vehicleTypeId],
    );
  }

  function handleSubmit() {
    const nextErrors: Record<string, string> = {};
    if (!levelId) nextErrors.levelId = "Vui lòng chọn tầng.";
    if (!name.trim()) nextErrors.name = "Vui lòng nhập tên khu.";
    else if (name.trim().length > 150) nextErrors.name = "Tên khu tối đa 150 ký tự.";
    if (selectedVehicleTypes.length === 0) nextErrors.vehicleTypeIds = "Vui lòng chọn ít nhất một loại phương tiện.";
    const parsedCapacity = Number(capacity);
    if (trackingMode === "CAPACITY") {
      if (capacity.trim() === "" || !Number.isInteger(parsedCapacity) || parsedCapacity < 0) {
        nextErrors.capacity = "Sức chứa phải là số nguyên không âm.";
      }
    }
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;
    onSubmit({
      parkingLevelId: levelId,
      name: name.trim(),
      vehicleTypeIds: selectedVehicleTypes,
      capacity: trackingMode === "SPACE" ? (initial ? initial.capacity : 0) : Number(capacity),
      trackingMode,
    });
  }

  return (
    <Modal
      open={open}
      title={title}
      description="Khu vực thuộc một tầng trong cùng bãi xe."
      onClose={() => { if (!submitting) onClose(); }}
      actions={(
        <div className="tw-flex tw-justify-end tw-gap-2">
          <Button variant="secondary" disabled={submitting} onClick={onClose}>Hủy</Button>
          <Button loading={submitting} onClick={handleSubmit}>
            <i className="fas fa-save" aria-hidden="true" /> Lưu khu
          </Button>
        </div>
      )}
    >
      <div className="tw-grid tw-gap-3.5">
        {serverError ? (
          <p className="tw-m-0 tw-rounded-[8px] tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-px-3 tw-py-2 tw-text-[0.84rem] tw-font-semibold tw-text-red-700">
            {serverError}
          </p>
        ) : null}
        <FormField label="Tầng" required error={errors.levelId}>
          <SelectMenu
            ariaLabel="Tầng"
            value={levelId}
            onChange={setLevelId}
            clearValue=""
            options={[{ label: "Chọn tầng", value: "" }, ...levels.map((level) => ({ label: `${level.code} — ${level.name}`, value: level.id }))]}
          />
        </FormField>
        <FormField label="Tên khu" required error={errors.name}>
          <input className={fieldInputClass(Boolean(errors.name))} value={name} onChange={(event) => setName(event.target.value)} placeholder="VD: Khu A" maxLength={150} />
        </FormField>
        <FormField label="Loại phương tiện" required error={errors.vehicleTypeIds}>
          <div className="tw-grid tw-max-h-40 tw-gap-1.5 tw-overflow-y-auto tw-rounded-[8px] tw-border tw-border-solid tw-border-[#dbe3f0] tw-p-2">
            {vehicleTypes.length === 0 ? (
              <span className="tw-px-1 tw-py-2 tw-text-[0.84rem] tw-font-medium tw-text-[#94a3b8]">Chưa có loại phương tiện nào.</span>
            ) : vehicleTypes.map((vehicleType) => (
              <label key={vehicleType.id} className="tw-flex tw-cursor-pointer tw-items-center tw-gap-2.5 tw-rounded-[6px] tw-px-2 tw-py-1.5 hover:tw-bg-[#f8fafc]">
                <input
                  type="checkbox"
                  checked={selectedVehicleTypes.includes(vehicleType.id)}
                  onChange={() => toggleVehicleType(vehicleType.id)}
                  className="tw-h-4 tw-w-4 tw-accent-[#2563eb]"
                />
                <span className="tw-text-[0.86rem] tw-font-semibold tw-text-[#334155]">{vehicleType.name}</span>
              </label>
            ))}
          </div>
        </FormField>
        <FormField label="Chế độ quản lý" required>
          <SelectMenu
            ariaLabel="Chế độ quản lý"
            value={trackingMode}
            onChange={(value) => setTrackingMode(value === "SPACE" ? "SPACE" : "CAPACITY")}
            options={TRACKING_MODE_OPTIONS}
          />
        </FormField>
        <FormField label="Sức chứa" required={trackingMode === "CAPACITY"} error={errors.capacity}>
          <input
            type="number"
            min={0}
            step={1}
            className={fieldInputClass(Boolean(errors.capacity))}
            value={capacity}
            disabled={trackingMode === "SPACE"}
            onChange={(event) => setCapacity(event.target.value)}
          />
          {trackingMode === "SPACE" ? (
            <span className="tw-mt-1 tw-block tw-text-[0.78rem] tw-font-medium tw-text-[#64748b]">
              Khu quản lý theo từng ô: sức chứa tự động theo sơ đồ đã xuất bản (khu mới chưa có sơ đồ giữ sức chứa bằng 0).
            </span>
          ) : null}
        </FormField>
      </div>
    </Modal>
  );
}

/* ------------------------------------------------------------------ */
/* Gate form modal                                                    */
/* ------------------------------------------------------------------ */

export type GateFormPayload = { zoneId: string; name: string };

function GateFormModal({ open, title, zones, initial, defaultZoneId, submitting, serverError, onSubmit, onClose }: {
  open: boolean;
  title: string;
  zones: ZoneNode[];
  initial: GateNode | null;
  defaultZoneId: string;
  submitting: boolean;
  serverError: string | null;
  onSubmit: (payload: GateFormPayload) => void;
  onClose: () => void;
}) {
  const [zoneId, setZoneId] = useState("");
  const [name, setName] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!open) return;
    setZoneId(initial?.zoneId ?? defaultZoneId ?? zones[0]?.id ?? "");
    setName(initial?.name ?? "");
    setErrors({});
  }, [open, initial, defaultZoneId, zones]);

  function handleSubmit() {
    const nextErrors: Record<string, string> = {};
    if (!zoneId) nextErrors.zoneId = "Vui lòng chọn khu vực.";
    if (!name.trim()) nextErrors.name = "Vui lòng nhập tên cổng.";
    else if (name.trim().length > 150) nextErrors.name = "Tên cổng tối đa 150 ký tự.";
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;
    onSubmit({ zoneId, name: name.trim() });
  }

  return (
    <Modal
      open={open}
      title={title}
      description="Cổng thuộc một khu vực trong bãi xe."
      onClose={() => { if (!submitting) onClose(); }}
      actions={(
        <div className="tw-flex tw-justify-end tw-gap-2">
          <Button variant="secondary" disabled={submitting} onClick={onClose}>Hủy</Button>
          <Button loading={submitting} onClick={handleSubmit}>
            <i className="fas fa-save" aria-hidden="true" /> Lưu cổng
          </Button>
        </div>
      )}
    >
      <div className="tw-grid tw-gap-3.5">
        {serverError ? (
          <p className="tw-m-0 tw-rounded-[8px] tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-px-3 tw-py-2 tw-text-[0.84rem] tw-font-semibold tw-text-red-700">
            {serverError}
          </p>
        ) : null}
        <FormField label="Khu vực" required error={errors.zoneId}>
          <SelectMenu
            ariaLabel="Khu vực"
            value={zoneId}
            onChange={setZoneId}
            clearValue=""
            options={[{ label: "Chọn khu vực", value: "" }, ...zones.map((zone) => ({ label: `${zone.code} — ${zone.name}`, value: zone.id }))]}
          />
        </FormField>
        <FormField label="Tên cổng" required error={errors.name}>
          <input className={fieldInputClass(Boolean(errors.name))} value={name} onChange={(event) => setName(event.target.value)} placeholder="VD: Cổng A1" maxLength={150} />
        </FormField>
      </div>
    </Modal>
  );
}

/* ------------------------------------------------------------------ */
/* Lane form modal                                                    */
/* ------------------------------------------------------------------ */

export type LaneFormPayload = { gateId: string; name: string; direction: LaneDirection };

function LaneFormModal({ open, title, gates, initial, defaultGateId, submitting, serverError, onSubmit, onClose }: {
  open: boolean;
  title: string;
  gates: GateNode[];
  initial: LaneNode | null;
  defaultGateId: string;
  submitting: boolean;
  serverError: string | null;
  onSubmit: (payload: LaneFormPayload) => void;
  onClose: () => void;
}) {
  const [gateId, setGateId] = useState("");
  const [name, setName] = useState("");
  const [direction, setDirection] = useState<LaneDirection>("IN");
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!open) return;
    setGateId(initial?.gateId ?? defaultGateId ?? gates[0]?.id ?? "");
    setName(initial?.name ?? "");
    setDirection(initial?.direction ?? "IN");
    setErrors({});
  }, [open, initial, defaultGateId, gates]);

  function handleSubmit() {
    const nextErrors: Record<string, string> = {};
    if (!gateId) nextErrors.gateId = "Vui lòng chọn cổng.";
    if (!name.trim()) nextErrors.name = "Vui lòng nhập tên làn.";
    else if (name.trim().length > 150) nextErrors.name = "Tên làn tối đa 150 ký tự.";
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;
    onSubmit({ gateId, name: name.trim(), direction });
  }

  return (
    <Modal
      open={open}
      title={title}
      description="Làn thuộc một cổng trong bãi xe."
      onClose={() => { if (!submitting) onClose(); }}
      actions={(
        <div className="tw-flex tw-justify-end tw-gap-2">
          <Button variant="secondary" disabled={submitting} onClick={onClose}>Hủy</Button>
          <Button loading={submitting} onClick={handleSubmit}>
            <i className="fas fa-save" aria-hidden="true" /> Lưu làn
          </Button>
        </div>
      )}
    >
      <div className="tw-grid tw-gap-3.5">
        {serverError ? (
          <p className="tw-m-0 tw-rounded-[8px] tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-px-3 tw-py-2 tw-text-[0.84rem] tw-font-semibold tw-text-red-700">
            {serverError}
          </p>
        ) : null}
        <FormField label="Cổng" required error={errors.gateId}>
          <SelectMenu
            ariaLabel="Cổng"
            value={gateId}
            onChange={setGateId}
            clearValue=""
            options={[{ label: "Chọn cổng", value: "" }, ...gates.map((gate) => ({ label: `${gate.code} — ${gate.name}`, value: gate.id }))]}
          />
        </FormField>
        <FormField label="Tên làn" required error={errors.name}>
          <input className={fieldInputClass(Boolean(errors.name))} value={name} onChange={(event) => setName(event.target.value)} placeholder="VD: Làn vào" maxLength={150} />
        </FormField>
        <FormField label="Hướng" required>
          <SelectMenu
            ariaLabel="Hướng làn"
            value={direction}
            onChange={(value) => setDirection(value === "OUT" ? "OUT" : "IN")}
            options={DIRECTION_OPTIONS}
          />
        </FormField>
      </div>
    </Modal>
  );
}

/* ------------------------------------------------------------------ */
/* Status modal                                                       */
/* ------------------------------------------------------------------ */

function StatusChangeModal({ target, kindLabel, submitting, serverError, onSubmit, onClose }: {
  target: StatusTarget | null;
  kindLabel: string;
  submitting: boolean;
  serverError: string | null;
  onSubmit: (next: TopologyStatus) => void;
  onClose: () => void;
}) {
  const [next, setNext] = useState<TopologyStatus>("ACTIVE");

  useEffect(() => {
    if (target) setNext(target.current);
  }, [target]);

  return (
    <Modal
      open={Boolean(target)}
      title={`Đổi trạng thái ${kindLabel}`}
      description={target ? `${target.label} đang ở trạng thái ${statusLabel(target.current)}.` : undefined}
      onClose={() => { if (!submitting) onClose(); }}
      actions={(
        <div className="tw-flex tw-justify-end tw-gap-2">
          <Button variant="secondary" disabled={submitting} onClick={onClose}>Hủy</Button>
          <Button loading={submitting} disabled={!target || next === target.current} onClick={() => onSubmit(next)}>
            <i className="fas fa-sync-alt" aria-hidden="true" /> Xác nhận
          </Button>
        </div>
      )}
    >
      <div className="tw-grid tw-gap-3">
        {serverError ? (
          <p className="tw-m-0 tw-rounded-[8px] tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-px-3 tw-py-2 tw-text-[0.84rem] tw-font-semibold tw-text-red-700">
            {serverError}
          </p>
        ) : null}
        <FormField label="Trạng thái mới" required>
          <SelectMenu
            ariaLabel="Trạng thái mới"
            value={next}
            onChange={(value) => setNext(value === "MAINTENANCE" ? "MAINTENANCE" : value === "CLOSED" ? "CLOSED" : "ACTIVE")}
            options={LIFECYCLE_STATUS_OPTIONS.map((option) => ({ label: option.label, value: option.value }))}
          />
        </FormField>
        {target && next === target.current ? (
          <p className="tw-m-0 tw-text-[0.82rem] tw-font-medium tw-text-[#64748b]">Trạng thái mới trùng với trạng thái hiện tại.</p>
        ) : null}
        {next === "CLOSED" ? (
          <p className="tw-m-0 tw-rounded-[8px] tw-border tw-border-solid tw-border-[#fde68a] tw-bg-[#fffbeb] tw-px-3 tw-py-2 tw-text-[0.82rem] tw-font-semibold tw-text-[#92400e]">
            Đóng là xóa mềm: dữ liệu lịch sử được giữ lại. Hệ thống chặn đóng khi còn ràng buộc hoạt động (cổng/làn đang hoạt động, phiên đang mở).
          </p>
        ) : null}
      </div>
    </Modal>
  );
}

/* ------------------------------------------------------------------ */
/* Page                                                               */
/* ------------------------------------------------------------------ */

export function ParkingLotStructurePage() {
  const { user } = useAuth();
  const toast = useToast();
  const params = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  const [lots, setLots] = useState<ParkingLotApiResponse[]>([]);
  const [levels, setLevels] = useState<LevelNode[]>([]);
  const [zones, setZones] = useState<ZoneNode[]>([]);
  const [gates, setGates] = useState<GateNode[]>([]);
  const [lanes, setLanes] = useState<LaneNode[]>([]);
  const [vehicleTypes, setVehicleTypes] = useState<Array<{ id: string; name: string }>>([]);

  const [bootLoading, setBootLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [topologyError, setTopologyError] = useState<string | null>(null);

  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [selected, setSelected] = useState<SelectedRef | null>(null);
  const [openLevels, setOpenLevels] = useState<string[]>([]);
  const [openZones, setOpenZones] = useState<string[]>([]);
  const [openGates, setOpenGates] = useState<string[]>([]);
  const [deleteTarget, setDeleteTarget] = useState<DeleteTarget | null>(null);
  const [deleting, setDeleting] = useState(false);

  const [formModal, setFormModal] = useState<FormModalState | null>(null);
  const [formLoading, setFormLoading] = useState(false);
  const [formSubmitting, setFormSubmitting] = useState(false);
  const [formServerError, setFormServerError] = useState<string | null>(null);

  const [statusTarget, setStatusTarget] = useState<StatusTarget | null>(null);
  const [statusSubmitting, setStatusSubmitting] = useState(false);
  const [statusServerError, setStatusServerError] = useState<string | null>(null);

  const [closeTarget, setCloseTarget] = useState<CloseTarget | null>(null);
  const [closing, setClosing] = useState(false);

  const [lotWizardOpen, setLotWizardOpen] = useState(false);
  const [lotEditOpen, setLotEditOpen] = useState(false);
  const [managerAssignmentOpen, setManagerAssignmentOpen] = useState(false);
  const [levelModalOpen, setLevelModalOpen] = useState(false);
  const [levelCode, setLevelCode] = useState("");
  const [levelName, setLevelName] = useState("");
  const [levelElevation, setLevelElevation] = useState("0");
  const [levelSubmitting, setLevelSubmitting] = useState(false);
  const [levelError, setLevelError] = useState<string | null>(null);

  const selectedRef = useRef<SelectedRef | null>(null);
  selectedRef.current = selected;

  const paramLotId = params.parkingLotId ?? searchParams.get("lotId") ?? searchParams.get("lot") ?? "";

  const activeLot: ParkingLotApiResponse | null = useMemo(
    () => lots.find((l) => l.parkingLotId === paramLotId) ?? lots[0] ?? null,
    [lots, paramLotId],
  );
  const hasActiveLot = activeLot !== null;
  const activeLotId = activeLot?.parkingLotId ?? "";
  const lotCode = activeLot?.code ?? "CHƯA THIẾT LẬP";
  const lotName = activeLot?.name?.trim() ? activeLot.name : "Chưa có bãi xe";
  const lotAddress = activeLot?.addressDisplay?.trim()
    ? activeLot.addressDisplay
    : "Tạo bãi xe đầu tiên để bắt đầu thiết lập cấu trúc và vận hành.";
  const lotActive = activeLot?.status === "ACTIVE";
  const canDeleteLot = hasAnyPermission(user, ["PARKING_LOT_UPDATE_ALL"]);
  const canCreateLot = hasAnyPermission(user, ["PARKING_LOT_CREATE_ALL"]);
  const canEditLot = hasAnyPermission(user, ["PARKING_LOT_UPDATE_ALL"]);
  const canAssignManager = hasAnyPermission(user, ["ORGANIZATION_MEMBERSHIP_MANAGE_ALL"]);
  const canDeleteLevel = hasAnyPermission(user, ["PARKING_SPACE_CONFIGURE_ALL"]);
  const canConfigureTopology = hasAnyPermission(user, ["PARKING_SPACE_CONFIGURE_ALL"]);

  const toLevelNode = useCallback((level: ParkingLevelApi, levelZones: ZoneApiResponse[]): LevelNode => {
    const totalCapacity = levelZones.reduce((sum, zone) => sum + Number(zone.capacity ?? 0), 0);
    return {
      id: level.parkingLevelId,
      code: level.code,
      name: level.name,
      status: normalizeStatus(level.status),
      elevation: level.elevation ?? null,
      displayOrder: level.displayOrder ?? 0,
      zoneCount: levelZones.length,
      totalCapacity,
    };
  }, []);

  const toZoneNode = useCallback((zone: ZoneApiResponse, gateCount: number): ZoneNode => ({
    id: zone.zoneId,
    code: zone.code,
    name: zone.name,
    levelId: zone.parkingLevelId ?? "",
    vehicleTypeIds: zone.vehicleTypeIds ?? (zone.vehicleTypeId ? [zone.vehicleTypeId] : []),
    trackingMode: zone.trackingMode === "SPACE" ? "SPACE" : "CAPACITY",
    capacity: Number(zone.capacity ?? 0),
    layoutStatus: zone.layoutStatus ?? null,
    layoutVersion: zone.layoutVersion ?? null,
    status: normalizeStatus(zone.status),
    gateCount,
  }), []);

  const expandSelection = useCallback((selection: SelectedRef | null, nextLevels: LevelNode[], nextZones: ZoneNode[], nextGates: GateNode[]) => {
    if (!selection) return;
    const zoneOfGate = (gateId: string) => nextGates.find((gate) => gate.id === gateId)?.zoneId;
    const levelOfZone = (zoneId: string) => nextZones.find((zone) => zone.id === zoneId)?.levelId;
    if (selection.kind === "lane") {
      const gateId = nextGates.some((gate) => gate.id === selection.id)
        ? selection.id
        : nextGates[0]?.id;
      const laneGateId = gateId ?? "";
      const zoneId = zoneOfGate(laneGateId) ?? "";
      const levelId = levelOfZone(zoneId) ?? "";
      if (levelId) setOpenLevels((current) => (current.includes(levelId) ? current : [...current, levelId]));
      if (zoneId) setOpenZones((current) => (current.includes(zoneId) ? current : [...current, zoneId]));
      if (laneGateId) setOpenGates((current) => (current.includes(laneGateId) ? current : [...current, laneGateId]));
      return;
    }
    const zoneId = selection.kind === "zone"
      ? selection.id
      : selection.kind === "gate"
        ? (zoneOfGate(selection.id) ?? "")
        : "";
    const levelId = selection.kind === "level" ? selection.id : levelOfZone(zoneId) ?? "";
    if (levelId) setOpenLevels((current) => (current.includes(levelId) ? current : [...current, levelId]));
    if (zoneId) setOpenZones((current) => (current.includes(zoneId) ? current : [...current, zoneId]));
    if (selection.kind === "gate") setOpenGates((current) => (current.includes(selection.id) ? current : [...current, selection.id]));
    void nextLevels;
  }, []);

  const resolveSelection = useCallback((
    previous: SelectedRef | null,
    focus: SelectedRef | undefined,
    nextLevels: LevelNode[],
    nextZones: ZoneNode[],
    nextGates: GateNode[],
    nextLanes: LaneNode[],
  ): SelectedRef | null => {
    const exists = (selection: SelectedRef | null): boolean => {
      if (!selection) return false;
      if (selection.kind === "level") return nextLevels.some((level) => level.id === selection.id);
      if (selection.kind === "zone") return nextZones.some((zone) => zone.id === selection.id);
      if (selection.kind === "gate") return nextGates.some((gate) => gate.id === selection.id);
      return nextLanes.some((lane) => lane.id === selection.id);
    };
    if (focus && exists(focus)) return focus;
    const candidate = previous ?? null;
    if (candidate && exists(candidate)) return candidate;
    if (candidate) {
      if (candidate.kind === "lane") {
        const lane = nextLanes.find((item) => item.id === candidate.id);
        const gateId = lane?.gateId ?? nextGates[0]?.id;
        if (gateId && nextGates.some((gate) => gate.id === gateId)) return { kind: "gate", id: gateId };
      }
      if (candidate.kind === "gate" || candidate.kind === "lane") {
        const gateId = candidate.kind === "gate" ? candidate.id : nextGates[0]?.id;
        const zoneId = nextGates.find((gate) => gate.id === gateId)?.zoneId ?? nextZones[0]?.id;
        if (zoneId && nextZones.some((zone) => zone.id === zoneId)) return { kind: "zone", id: zoneId };
      }
      if (candidate.kind === "zone" || candidate.kind === "gate") {
        const zoneId = candidate.kind === "zone" ? candidate.id : nextZones[0]?.id;
        const levelId = nextZones.find((zone) => zone.id === zoneId)?.levelId ?? nextLevels[0]?.id;
        if (levelId && nextLevels.some((level) => level.id === levelId)) return { kind: "level", id: levelId };
      }
    }
    if (nextZones[0]) return { kind: "zone", id: nextZones[0].id };
    if (nextLevels[0]) return { kind: "level", id: nextLevels[0].id };
    return null;
  }, []);

  const reloadTopology = useCallback(async (lotId: string, focus?: SelectedRef) => {
    if (!lotId) return;
    setRefreshing(true);
    setTopologyError(null);
    try {
      const [levelRes, zoneRes, vehicleTypeRes] = await Promise.all([
        getLevelsByLot(lotId),
        getZones({ parkingLotId: lotId }),
        getVehicleTypes({ isActive: true }),
      ]);
      const apiLevels: ParkingLevelApi[] = levelRes.data ?? [];
      const apiZones: ZoneApiResponse[] = zoneRes.data ?? [];
      const gateGroups = await Promise.all(apiZones.map((zone) => getGates({ zoneId: zone.zoneId })));
      const apiGates: GateApiResponse[] = gateGroups.flatMap((group) => group.data ?? []);
      const laneGroups = await Promise.all(apiGates.map((gate) => getLanes({ gateId: gate.gateId })));
      const apiLanes: LaneApiResponse[] = laneGroups.flatMap((group) => group.data ?? []);

      const lanesByGate = new Map<string, number>();
      apiLanes.forEach((lane) => lanesByGate.set(lane.gateId, (lanesByGate.get(lane.gateId) ?? 0) + 1));
      const gatesByZone = new Map<string, number>();
      apiGates.forEach((gate) => gatesByZone.set(gate.zoneId, (gatesByZone.get(gate.zoneId) ?? 0) + 1));
      const levelIds = new Set(apiLevels.map((level) => level.parkingLevelId));
      const fallbackLevelId = apiLevels[0]?.parkingLevelId ?? "";

      const nextLevels = [...apiLevels]
        .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
        .map((level) => toLevelNode(
          level,
          apiZones.filter((zone) => zone.parkingLevelId === level.parkingLevelId),
        ));
      const nextZones = apiZones.map((zone) => {
        const levelId = zone.parkingLevelId && levelIds.has(zone.parkingLevelId) ? zone.parkingLevelId : fallbackLevelId;
        return toZoneNode(zone, gatesByZone.get(zone.zoneId) ?? 0);
      }).map((zone) => (zone.levelId ? zone : { ...zone, levelId: fallbackLevelId }));
      const nextGates: GateNode[] = apiGates.map((gate) => ({
        id: gate.gateId,
        code: gate.code,
        name: gate.name,
        status: normalizeStatus(gate.status),
        zoneId: gate.zoneId,
        laneCount: lanesByGate.get(gate.gateId) ?? 0,
      }));
      const nextLanes: LaneNode[] = apiLanes.map((lane) => ({
        id: lane.laneId,
        code: lane.code,
        name: lane.name,
        direction: String(lane.direction).toUpperCase() === "OUT" ? "OUT" : "IN",
        status: normalizeStatus(lane.status),
        gateId: lane.gateId,
      }));

      // Gán lại zoneCount/totalCapacity theo levelId thực tế sau fallback.
      const zonesByLevel = new Map<string, ZoneApiResponse[]>();
      apiZones.forEach((zone) => {
        const levelId = zone.parkingLevelId && levelIds.has(zone.parkingLevelId) ? zone.parkingLevelId : fallbackLevelId;
        const list = zonesByLevel.get(levelId) ?? [];
        list.push(zone);
        zonesByLevel.set(levelId, list);
      });
      const withCounts = nextLevels.map((level) => {
        const list = zonesByLevel.get(level.id) ?? [];
        return { ...level, zoneCount: list.length, totalCapacity: list.reduce((sum, zone) => sum + Number(zone.capacity ?? 0), 0) };
      });

      setLevels(withCounts);
      setZones(nextZones);
      setGates(nextGates);
      setLanes(nextLanes);
      setVehicleTypes((vehicleTypeRes.data ?? []).map((vehicleType) => ({ id: vehicleType.vehicleTypeId, name: vehicleType.name })));

      const nextSelection = resolveSelection(selectedRef.current, focus, withCounts, nextZones, nextGates, nextLanes);
      setSelected(nextSelection);
      expandSelection(nextSelection, withCounts, nextZones, nextGates);
    } catch (requestError) {
      setTopologyError(requestError instanceof Error ? requestError.message : "Không tải được cấu trúc bãi xe.");
    } finally {
      setRefreshing(false);
      setBootLoading(false);
    }
  }, [expandSelection, resolveSelection, toLevelNode, toZoneNode]);

  /* Nạp chức năng hiện có: bãi xe rồi tới tầng/khu/cổng/làn của đúng bãi đang chọn. */
  useEffect(() => {
    let alive = true;
    (async () => {
      setBootLoading(true);
      setTopologyError(null);
      try {
        const lotRes = await getParkingLots();
        if (!alive) return;
        const apiLots = lotRes.data ?? [];
        setLots(apiLots);
        const targetLot = apiLots.find((lot) => lot.parkingLotId === paramLotId) ?? apiLots[0] ?? null;
        if (!targetLot) {
          setLevels([]);
          setZones([]);
          setGates([]);
          setLanes([]);
          setVehicleTypes([]);
          setSelected(null);
          return;
        }
        await reloadTopology(targetLot.parkingLotId);
      } catch (requestError) {
        if (!alive) return;
        setTopologyError(requestError instanceof Error ? requestError.message : "Không tải được danh sách bãi xe.");
      } finally {
        if (alive) setBootLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [paramLotId, reloadTopology]);

  useEffect(() => {
    const action = searchParams.get("action");
    if (!action) return;
    if (action === "create") {
      if (canCreateLot) setLotWizardOpen(true);
      else toast.error("Bạn không có quyền tạo bãi xe.");
    } else if (!activeLot) {
      return;
    } else if (action === "edit") {
      if (canEditLot) setLotEditOpen(true);
      else toast.error("Bạn không có quyền sửa thông tin bãi xe.");
    } else if (action === "manager") {
      if (canAssignManager) setManagerAssignmentOpen(true);
      else toast.error("Bạn không có quyền phân công Parking Manager.");
    }
    const next = new URLSearchParams(searchParams);
    next.delete("action");
    setSearchParams(next, { replace: true });
  }, [activeLot, canAssignManager, canCreateLot, canEditLot, searchParams, setSearchParams, toast]);

  const normalizedKeyword = keyword.trim().toLowerCase();
  const matchStatus = (s: string) => statusFilter === "all" || s === statusFilter;

  const visibleLevels = useMemo(() => {
    if (!normalizedKeyword && statusFilter === "all") return levels;
    return levels.filter((lv) => {
      const zoneHit = zones
        .filter((z) => z.levelId === lv.id)
        .some((z) => {
          const gateHit = gates
            .filter((g) => g.zoneId === z.id)
            .some((g) => {
              const laneHit = lanes
                .filter((l) => l.gateId === g.id)
                .some((l) => (!normalizedKeyword || `${l.code} ${l.name}`.toLowerCase().includes(normalizedKeyword)) && matchStatus(l.status));
              return (
                (!normalizedKeyword || `${g.code} ${g.name}`.toLowerCase().includes(normalizedKeyword)) && matchStatus(g.status) ? true : laneHit
              );
            });
          return (
            (!normalizedKeyword || `${z.code} ${z.name}`.toLowerCase().includes(normalizedKeyword)) && matchStatus(z.status) ? true : gateHit
          );
        });
      return (
        ((!normalizedKeyword || `${lv.code} ${lv.name}`.toLowerCase().includes(normalizedKeyword)) && matchStatus(lv.status)) || zoneHit
      );
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [levels, zones, gates, lanes, normalizedKeyword, statusFilter]);

  const vehicleTypeNameOf = useCallback((vehicleTypeId: string) =>
    vehicleTypes.find((vehicleType) => vehicleType.id === vehicleTypeId)?.name ?? vehicleTypeId.slice(0, 8),
  [vehicleTypes]);

  const zonesOf = (levelId: string) => zones.filter((z) => z.levelId === levelId);
  const unassignedZones = useMemo(() => {
    const levelIds = new Set(levels.map((level) => level.id));
    return zones.filter((zone) => !levelIds.has(zone.levelId));
  }, [levels, zones]);
  const gatesOf = (zoneId: string) => gates.filter((g) => g.zoneId === zoneId);
  const lanesOf = (gateId: string) => lanes.filter((l) => l.gateId === gateId);

  const levelOfZone = (zoneId: string) => levels.find((level) => level.id === zones.find((zone) => zone.id === zoneId)?.levelId) ?? null;
  const zoneOfGate = (gateId: string) => zones.find((zone) => zone.id === gates.find((gate) => gate.id === gateId)?.zoneId) ?? null;
  const gateOfLane = (laneId: string) => gates.find((gate) => gate.id === lanes.find((lane) => lane.id === laneId)?.gateId) ?? null;

  const selectedLevel = selected?.kind === "level" ? (levels.find((level) => level.id === selected.id) ?? null) : null;
  const selectedZone = selected?.kind === "zone"
    ? (zones.find((zone) => zone.id === selected.id) ?? null)
    : selected?.kind === "gate"
      ? zoneOfGate(selected.id)
      : selected?.kind === "lane"
        ? (() => {
          const gate = gateOfLane(selected.id);
          return zones.find((zone) => zone.id === gate?.zoneId) ?? null;
        })()
        : null;
  const selectedGate = selected?.kind === "gate"
    ? (gates.find((gate) => gate.id === selected.id) ?? null)
    : selected?.kind === "lane"
      ? gateOfLane(selected.id)
      : null;
  const selectedLane = selected?.kind === "lane" ? (lanes.find((lane) => lane.id === selected.id) ?? null) : null;

  /* Parent mặc định cho form: suy từ selection hiện tại trong cùng bãi. */
  const defaultLevelId = useMemo(() => {
    if (selected?.kind === "level") return selected.id;
    if (selected?.kind === "zone") return zones.find((zone) => zone.id === selected.id)?.levelId ?? "";
    if (selected?.kind === "gate") {
      const zone = zoneOfGate(selected.id);
      return zone?.levelId ?? "";
    }
    if (selected?.kind === "lane") {
      const gate = gateOfLane(selected.id);
      const zone = zones.find((zone) => zone.id === gate?.zoneId);
      return zone?.levelId ?? "";
    }
    return "";
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selected, zones, gates, lanes]);

  const defaultZoneId = useMemo(() => {
    if (selected?.kind === "zone") return selected.id;
    if (selected?.kind === "gate") return gates.find((gate) => gate.id === selected.id)?.zoneId ?? "";
    if (selected?.kind === "lane") {
      const gate = gateOfLane(selected.id);
      return gate?.zoneId ?? "";
    }
    if (selected?.kind === "level") return zones.find((zone) => zone.levelId === selected.id)?.id ?? "";
    return "";
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selected, zones, gates, lanes]);

  const defaultGateId = useMemo(() => {
    if (selected?.kind === "gate") return selected.id;
    if (selected?.kind === "lane") return lanes.find((lane) => lane.id === selected.id)?.gateId ?? "";
    if (selected?.kind === "zone") return gates.find((gate) => gate.zoneId === selected.id)?.id ?? "";
    if (selected?.kind === "level") {
      const levelZones = zones.filter((zone) => zone.levelId === selected.id);
      for (const zone of levelZones) {
        const gate = gates.find((item) => item.zoneId === zone.id);
        if (gate) return gate.id;
      }
      return "";
    }
    return "";
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selected, zones, gates, lanes]);

  const toggle = (list: string[], id: string, set: (v: string[]) => void) =>
    set(list.includes(id) ? list.filter((x) => x !== id) : [...list, id]);

  const goTab = (to: string) => {
    const query = activeLotId ? `?lotId=${encodeURIComponent(activeLotId)}` : "";
    navigate(`${to}${query}`);
  };
  const activeZoneCount = zones.filter((zone) => zone.status === "ACTIVE").length;

  function openFormModal(modal: FormModalState) {
    setFormServerError(null);
    setFormModal(modal);
  }

  function handleWizardCompleted(lot: ParkingLotApiResponse) {
    setLotWizardOpen(false);
    navigate(`/admin/parking-structure?lotId=${lot.parkingLotId}`);
  }

  function openLevelModal() {
    setLevelCode("");
    setLevelName("");
    setLevelElevation("0");
    setLevelError(null);
    setLevelModalOpen(true);
  }

  async function handleCreateLevel() {
    if (!activeLotId || levelSubmitting) return;
    const code = levelCode.trim().toUpperCase();
    const name = levelName.trim();
    const elevation = Number(levelElevation);
    if (!code) {
      setLevelError("Vui lòng nhập mã tầng.");
      return;
    }
    if (!/^[A-Za-z0-9_-]+$/.test(code)) {
      setLevelError("Mã tầng chỉ gồm chữ cái, số, gạch dưới hoặc gạch ngang.");
      return;
    }
    if (code.length > 50) {
      setLevelError("Mã tầng tối đa 50 ký tự.");
      return;
    }
    if (!name) {
      setLevelError("Vui lòng nhập tên tầng.");
      return;
    }
    if (!Number.isFinite(elevation)) {
      setLevelError("Cao độ tầng phải là số hợp lệ.");
      return;
    }
    setLevelSubmitting(true);
    setLevelError(null);
    try {
      const created = await createParkingLevel(activeLotId, {
        canvasHeight: 700,
        canvasWidth: 1000,
        code,
        displayOrder: levels.length,
        elevation,
        floorHeight: 3.5,
        name,
      });
      toast.success("Đã thêm tầng.");
      setLevelModalOpen(false);
      await reloadTopology(activeLotId, { kind: "level", id: created.data.parkingLevelId });
    } catch (requestError) {
      setLevelError(requestError instanceof Error ? requestError.message : "Không thể thêm tầng.");
    } finally {
      setLevelSubmitting(false);
    }
  }

  /* Mở form sửa với dữ liệu tươi từ API trước khi hydrate. */
  async function openZoneEdit(zoneId: string) {
    setFormLoading(true);
    try {
      const res = await getZoneById(zoneId);
      const fresh = res.data;
      setZones((current) => current.map((zone) => zone.id === zoneId
        ? {
          id: fresh.zoneId,
          code: fresh.code,
          name: fresh.name,
          levelId: fresh.parkingLevelId ?? zone.levelId,
          vehicleTypeIds: fresh.vehicleTypeIds ?? (fresh.vehicleTypeId ? [fresh.vehicleTypeId] : []),
          trackingMode: fresh.trackingMode === "SPACE" ? "SPACE" : "CAPACITY",
          capacity: Number(fresh.capacity ?? 0),
          layoutStatus: fresh.layoutStatus ?? null,
          layoutVersion: fresh.layoutVersion ?? null,
          status: normalizeStatus(fresh.status),
          gateCount: zone.gateCount,
        }
        : zone));
      openFormModal({ type: "zone", mode: "edit", id: zoneId });
    } catch (requestError) {
      toast.error(requestError instanceof Error ? requestError.message : "Không tải được khu vực.");
    } finally {
      setFormLoading(false);
    }
  }

  async function openGateEdit(gateId: string) {
    setFormLoading(true);
    try {
      const res = await getGateById(gateId);
      const fresh = res.data;
      setGates((current) => current.map((gate) => gate.id === gateId
        ? { id: fresh.gateId, code: fresh.code, name: fresh.name, status: normalizeStatus(fresh.status), zoneId: fresh.zoneId, laneCount: gate.laneCount }
        : gate));
      openFormModal({ type: "gate", mode: "edit", id: gateId });
    } catch (requestError) {
      toast.error(requestError instanceof Error ? requestError.message : "Không tải được cổng.");
    } finally {
      setFormLoading(false);
    }
  }

  async function openLaneEdit(laneId: string) {
    setFormLoading(true);
    try {
      const res = await getLaneById(laneId);
      const fresh = res.data;
      setLanes((current) => current.map((lane) => lane.id === laneId
        ? {
          id: fresh.laneId,
          code: fresh.code,
          name: fresh.name,
          direction: String(fresh.direction).toUpperCase() === "OUT" ? "OUT" : "IN",
          status: normalizeStatus(fresh.status),
          gateId: fresh.gateId,
        }
        : lane));
      openFormModal({ type: "lane", mode: "edit", id: laneId });
    } catch (requestError) {
      toast.error(requestError instanceof Error ? requestError.message : "Không tải được làn.");
    } finally {
      setFormLoading(false);
    }
  }

  async function handleZoneSubmit(payload: ZoneFormPayload) {
    if (!activeLotId) return;
    setFormSubmitting(true);
    setFormServerError(null);
    try {
      if (formModal?.type === "zone" && formModal.mode === "edit" && formModal.id) {
        await updateZone(formModal.id, {
          parkingLevelId: payload.parkingLevelId,
          name: payload.name,
          vehicleTypeId: payload.vehicleTypeIds[0] ?? null,
          vehicleTypeIds: payload.vehicleTypeIds,
          capacity: payload.capacity,
          trackingMode: payload.trackingMode,
        });
        toast.success("Đã cập nhật khu vực.");
        setFormModal(null);
        await reloadTopology(activeLotId, { kind: "zone", id: formModal.id });
      } else {
        const created = await createZone({
          parkingLotId: activeLotId,
          parkingLevelId: payload.parkingLevelId,
          name: payload.name,
          vehicleTypeId: payload.vehicleTypeIds[0] ?? null,
          vehicleTypeIds: payload.vehicleTypeIds,
          capacity: payload.capacity,
          trackingMode: payload.trackingMode,
        });
        toast.success("Đã thêm khu vực.");
        setFormModal(null);
        await reloadTopology(activeLotId, { kind: "zone", id: created.data.zoneId });
      }
    } catch (requestError) {
      const message = requestError instanceof Error ? requestError.message : "Không thể lưu khu vực.";
      setFormServerError(message);
      toast.error(message);
    } finally {
      setFormSubmitting(false);
    }
  }

  async function handleGateSubmit(payload: GateFormPayload) {
    if (!activeLotId) return;
    setFormSubmitting(true);
    setFormServerError(null);
    try {
      if (formModal?.type === "gate" && formModal.mode === "edit" && formModal.id) {
        await updateGate(formModal.id, { name: payload.name });
        toast.success("Đã cập nhật cổng.");
        setFormModal(null);
        await reloadTopology(activeLotId, { kind: "gate", id: formModal.id });
      } else {
        const created = await createGate(payload);
        toast.success("Đã thêm cổng.");
        setFormModal(null);
        await reloadTopology(activeLotId, { kind: "gate", id: created.data.gateId });
      }
    } catch (requestError) {
      const message = requestError instanceof Error ? requestError.message : "Không thể lưu cổng.";
      setFormServerError(message);
      toast.error(message);
    } finally {
      setFormSubmitting(false);
    }
  }

  async function handleLaneSubmit(payload: LaneFormPayload) {
    if (!activeLotId) return;
    setFormSubmitting(true);
    setFormServerError(null);
    try {
      if (formModal?.type === "lane" && formModal.mode === "edit" && formModal.id) {
        await updateLane(formModal.id, { name: payload.name, direction: payload.direction });
        toast.success("Đã cập nhật làn.");
        setFormModal(null);
        await reloadTopology(activeLotId, { kind: "lane", id: formModal.id });
      } else {
        const created = await createLane(payload);
        toast.success("Đã thêm làn.");
        setFormModal(null);
        await reloadTopology(activeLotId, { kind: "lane", id: created.data.laneId });
      }
    } catch (requestError) {
      const message = requestError instanceof Error ? requestError.message : "Không thể lưu làn.";
      setFormServerError(message);
      toast.error(message);
    } finally {
      setFormSubmitting(false);
    }
  }

  function openStatusModal(target: StatusTarget) {
    setStatusServerError(null);
    setStatusTarget(target);
  }

  async function handleStatusSubmit(next: TopologyStatus) {
    if (!statusTarget || !activeLotId) return;
    setStatusSubmitting(true);
    setStatusServerError(null);
    try {
      const { kind, id } = statusTarget;
      if (kind === "zone") {
        if (next === "ACTIVE") await activateZone(id);
        else if (next === "MAINTENANCE") await markZoneMaintenance(id);
        else await closeZone(id);
      } else if (kind === "gate") {
        if (next === "ACTIVE") await activateGate(id);
        else if (next === "MAINTENANCE") await markGateMaintenance(id);
        else await closeGate(id);
      } else {
        if (next === "ACTIVE") await activateLane(id);
        else if (next === "MAINTENANCE") await markLaneMaintenance(id);
        else await closeLane(id);
      }
      toast.success(`Đã chuyển trạng thái thành ${statusLabel(next)}.`);
      setStatusTarget(null);
      await reloadTopology(activeLotId, { kind, id });
    } catch (requestError) {
      const message = requestError instanceof Error ? requestError.message : "Không thể đổi trạng thái.";
      setStatusServerError(message);
      toast.error(message);
    } finally {
      setStatusSubmitting(false);
    }
  }

  function openCloseConfirm(target: CloseTarget) {
    setCloseTarget(target);
  }

  async function handleConfirmClose() {
    if (!closeTarget || !activeLotId) return;
    setClosing(true);
    try {
      const { kind, id, label } = closeTarget;
      if (kind === "zone") await deleteZone(id);
      else if (kind === "gate") await deleteGate(id);
      else await deleteLane(id);
      toast.success(`Đã đóng ${label}.`);
      setCloseTarget(null);
      // Rơi về parent gần nhất khi item vừa đóng khỏi cây.
      const fallback: SelectedRef | undefined = kind === "zone"
        ? (() => {
          const zone = zones.find((item) => item.id === id);
          return zone ? { kind: "level", id: zone.levelId } as SelectedRef : undefined;
        })()
        : kind === "gate"
          ? (() => {
            const gate = gates.find((item) => item.id === id);
            return gate ? { kind: "zone", id: gate.zoneId } as SelectedRef : undefined;
          })()
          : (() => {
            const lane = lanes.find((item) => item.id === id);
            return lane ? { kind: "gate", id: lane.gateId } as SelectedRef : undefined;
          })();
      await reloadTopology(activeLotId, fallback);
    } catch (requestError) {
      toast.error(requestError instanceof Error ? requestError.message : "Không thể đóng.");
    } finally {
      setClosing(false);
    }
  }

  async function handleConfirmDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      if (deleteTarget.kind === "level") {
        await deleteParkingLevel(deleteTarget.id);
        setLevels((current) => current.filter((level) => level.id !== deleteTarget.id));
        setOpenLevels((current) => current.filter((levelId) => levelId !== deleteTarget.id));
        setSelected((current) => current?.kind === "level" && current.id === deleteTarget.id
          ? (zones[0] ? { kind: "zone", id: zones[0].id } : null)
          : current);
        toast.success(`Đã xóa tầng ${deleteTarget.label}.`);
      } else {
        await deleteParkingLot(deleteTarget.id);
        toast.success(`Đã đóng bãi ${deleteTarget.label}.`);
        navigate("/admin/parking-lots", { replace: true });
      }
      setDeleteTarget(null);
    } catch (requestError) {
      toast.error(requestError instanceof Error ? requestError.message : "Không thể thực hiện thao tác xóa.");
    } finally {
      setDeleting(false);
    }
  }

  const formZoneInitial = formModal?.type === "zone" && formModal.mode === "edit" && formModal.id
    ? (zones.find((zone) => zone.id === formModal.id) ?? null)
    : null;
  const formGateInitial = formModal?.type === "gate" && formModal.mode === "edit" && formModal.id
    ? (gates.find((gate) => gate.id === formModal.id) ?? null)
    : null;
  const formLaneInitial = formModal?.type === "lane" && formModal.mode === "edit" && formModal.id
    ? (lanes.find((lane) => lane.id === formModal.id) ?? null)
    : null;

  const detailLevel = selectedLevel ?? (selectedZone ? levelOfZone(selectedZone.id) : selectedGate ? levelOfZone(zoneOfGate(selectedGate.id)?.id ?? "") : null);

  return (
    <main className="tw-mx-auto tw-w-full tw-max-w-[1480px] tw-px-5 tw-py-5 max-[768px]:tw-px-4">
      {/* Breadcrumb */}
      <nav aria-label="Breadcrumb" className="tw-flex tw-items-center tw-gap-2 tw-text-[0.84rem]">
        <Link to="/admin/parking-lots" className="tw-font-medium tw-text-[#64748b] hover:tw-text-[#2563eb] hover:tw-no-underline">
          Bãi xe
        </Link>
        <span className="tw-text-[#94a3b8]">/</span>
        <span className="tw-font-semibold tw-text-[#0f172a]">{lotName}</span>
      </nav>

      {activeLot ? (
        <ParkingLotWorkspaceHeader
          activeTab="structure"
          parkingLot={activeLot}
          parkingLots={lots}
          onParkingLotChange={(parkingLotId) => navigate(`/admin/parking-structure?lotId=${encodeURIComponent(parkingLotId)}`)}
          actions={(
            <>
              {canDeleteLot ? (
                <Button variant="danger" onClick={() => setDeleteTarget({ kind: "lot", id: activeLot.parkingLotId, label: activeLot.name })}>
                  <i className="far fa-trash-alt" aria-hidden="true" />Xóa bãi
                </Button>
              ) : null}
              {canEditLot ? <Button variant="secondary" onClick={() => setLotEditOpen(true)}><i className="far fa-edit" aria-hidden="true" />Sửa bãi</Button> : null}
              {canAssignManager ? <Button variant="secondary" onClick={() => setManagerAssignmentOpen(true)}><i className="fas fa-users-cog" aria-hidden="true" />Gán quản lý</Button> : null}
              {canCreateLot ? <Button onClick={() => setLotWizardOpen(true)}><i className="fas fa-plus" aria-hidden="true" />Thêm bãi</Button> : null}
            </>
          )}
        />
      ) : (
        <>
          <div className="tw-mt-2 tw-flex tw-min-h-[86px] tw-items-center tw-justify-between tw-gap-4">
            <div className="tw-flex tw-min-w-0 tw-items-center tw-gap-4">
              <span className="tw-flex tw-size-[62px] tw-flex-none tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-solid tw-border-blue-100 tw-bg-gradient-to-br tw-from-blue-50 tw-to-white tw-shadow-sm">
                <span className="tw-flex tw-size-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-vm-primary tw-text-xl tw-font-black tw-text-white">P</span>
              </span>
              <div className="tw-min-w-0">
                <h1 className="tw-m-0 tw-text-[1.7rem] tw-font-extrabold tw-text-vm-slate-900">{lotName} · {lotCode}</h1>
                <p className="tw-m-0 tw-mt-1.5 tw-flex tw-items-center tw-gap-2 tw-text-[0.86rem] tw-text-vm-slate-500"><i className="fas fa-map-marker-alt" aria-hidden="true" />{lotAddress}</p>
              </div>
            </div>
            {canCreateLot ? <Button onClick={() => setLotWizardOpen(true)}><i className="fas fa-plus" aria-hidden="true" />Thêm bãi</Button> : null}
          </div>
          <div className="tw-mt-1 tw-flex tw-h-[52px] tw-items-end tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100" aria-label="Điều hướng quản lý bãi xe">
            {["Tổng quan", "Cấu trúc", "Thiết kế sơ đồ", "Trực tiếp"].map((label) => <span key={label} className={cn("tw-inline-flex tw-h-[52px] tw-items-center tw-px-4 tw-text-[0.9rem] tw-font-bold", label === "Cấu trúc" ? "tw-text-vm-primary" : "tw-text-vm-slate-300")}>{label}</span>)}
          </div>
        </>
      )}

      {/* Info banner */}
      <div className="tw-mt-4 tw-flex tw-items-center tw-gap-3 tw-rounded-[8px] tw-border tw-border-solid tw-border-[#dbeafe] tw-bg-[#eff6ff] tw-px-4 tw-py-3">
        <span className="tw-inline-flex tw-h-6 tw-w-6 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-bg-[#2563eb] tw-text-[0.8rem] tw-text-white">
          <i className="fas fa-info" aria-hidden="true" />
        </span>
        <p className="tw-m-0 tw-text-[0.88rem] tw-font-medium tw-text-[#1d4ed8]">
          {hasActiveLot
            ? "Thiết lập Tầng → Khu vực → Cổng → Làn trước khi thiết kế sơ đồ."
            : "Bắt đầu bằng việc tạo bãi xe. Các bước cấu hình và phân công sẽ được mở sau khi bãi được tạo."}
        </p>
      </div>

      {/* Stats: số liệu thật từ API */}
      <div className="tw-mt-4 tw-grid tw-grid-cols-4 tw-gap-4 max-[1100px]:tw-grid-cols-2 max-[620px]:tw-grid-cols-1">
        <StatCard icon="fas fa-layer-group" iconWrap="tw-bg-[#eff6ff] tw-text-[#2563eb]" value={String(levels.length)} label="Tầng" />
        <StatCard icon="fas fa-th-large" iconWrap="tw-bg-[#dcfce7] tw-text-[#16a34a]" value={String(zones.length)} label="Khu vực" />
        <StatCard icon="fas fa-door-open" iconWrap="tw-bg-[#ede9fe] tw-text-[#7c3aed]" value={String(gates.length)} label="Cổng" />
        <StatCard icon="fas fa-road" iconWrap="tw-bg-[#fef3c7] tw-text-[#d97706]" value={String(lanes.length)} label="Làn" />
      </div>

      {/* Content grid */}
      <div className="tw-mt-4 tw-grid tw-grid-cols-[minmax(0,1.9fr)_minmax(340px,1fr)] tw-items-start tw-gap-4 max-[1100px]:tw-grid-cols-1">
        {/* Left: tree */}
        <Card className="tw-p-5">
          <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
            <h2 className="tw-m-0 tw-text-[1.12rem] tw-font-extrabold tw-text-[#0f172a]">Cấu trúc bãi xe</h2>
            {refreshing && !bootLoading ? (
              <span className="tw-inline-flex tw-items-center tw-gap-2 tw-text-[0.8rem] tw-font-semibold tw-text-[#64748b]">
                <i className="fas fa-sync-alt fa-spin" aria-hidden="true" /> Đang tải lại…
              </span>
            ) : null}
          </div>
          <div className="tw-mt-3 tw-flex tw-flex-wrap tw-items-center tw-gap-2.5">
            <div className="tw-min-w-[220px] tw-flex-1">
              <SearchInput disabled={!hasActiveLot} value={keyword} onChange={setKeyword} placeholder="Tìm theo mã hoặc tên" aria-label="Tìm theo mã hoặc tên" />
            </div>
            <div className="tw-w-[190px]">
              <SelectMenu ariaLabel="Trạng thái" disabled={!hasActiveLot} value={statusFilter} onChange={setStatusFilter} options={STATUS_FILTERS} />
            </div>
            {canConfigureTopology ? (
              <>
                <Button size="sm" disabled={!hasActiveLot} className="tw-h-10 tw-rounded-[8px] tw-px-3.5 tw-text-[0.84rem] tw-font-bold" onClick={() => openFormModal({ type: "zone", mode: "create" })}>
                  <i className="fas fa-plus tw-text-[0.75rem]" aria-hidden="true" /> Thêm khu
                </Button>
                <Button size="sm" disabled={!hasActiveLot} className="tw-h-10 tw-rounded-[8px] tw-px-3.5 tw-text-[0.84rem] tw-font-bold" onClick={() => openFormModal({ type: "gate", mode: "create" })}>
                  <i className="fas fa-plus tw-text-[0.75rem]" aria-hidden="true" /> Thêm cổng
                </Button>
                <Button size="sm" disabled={!hasActiveLot} className="tw-h-10 tw-rounded-[8px] tw-px-3.5 tw-text-[0.84rem] tw-font-bold" onClick={() => openFormModal({ type: "lane", mode: "create" })}>
                  <i className="fas fa-plus tw-text-[0.75rem]" aria-hidden="true" /> Thêm làn
                </Button>
              </>
            ) : null}
          </div>

          <div className="tw-mt-4 tw-grid tw-gap-2.5">
            {!hasActiveLot ? (
              <div className="tw-rounded-[8px] tw-border tw-border-dashed tw-border-[#93c5fd] tw-bg-[#eff6ff] tw-px-4 tw-py-8 tw-text-center">
                <i className="fas fa-parking tw-text-3xl tw-text-[#2563eb]" aria-hidden="true" />
                <p className="tw-m-0 tw-mt-3 tw-text-[0.95rem] tw-font-extrabold tw-text-[#0f172a]">Chưa có bãi xe để cấu hình</p>
                <p className="tw-m-0 tw-mt-1 tw-text-[0.84rem] tw-font-medium tw-text-[#475569]">Tạo bãi xe đầu tiên, sau đó bạn có thể thiết lập tầng, khu vực, cổng, làn và phân công Parking Manager.</p>
                {canCreateLot ? (
                  <Button size="sm" className="tw-mt-4 tw-h-10 tw-rounded-[8px] tw-px-3.5 tw-text-[0.84rem] tw-font-bold" onClick={() => setLotWizardOpen(true)}>
                    <i className="fas fa-plus tw-text-[0.75rem]" aria-hidden="true" /> Tạo bãi xe đầu tiên
                  </Button>
                ) : null}
              </div>
            ) : bootLoading ? (
              <div className="tw-grid tw-gap-2.5" aria-label="Đang tải cấu trúc bãi xe">
                {[0, 1, 2].map((skeleton) => (
                  <div key={skeleton} className="tw-h-[52px] tw-animate-pulse tw-rounded-[8px] tw-bg-[#eef2f7]" />
                ))}
              </div>
            ) : topologyError ? (
              <div className="tw-rounded-[8px] tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-px-4 tw-py-6 tw-text-center">
                <p className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-red-700">Không tải được cấu trúc bãi xe.</p>
                <p className="tw-m-0 tw-mt-1 tw-text-[0.84rem] tw-font-medium tw-text-red-600">{topologyError}</p>
                <Button
                  size="sm"
                  variant="secondary"
                  className="tw-mt-3 tw-h-10 tw-rounded-[8px] tw-font-bold"
                  loading={refreshing}
                  onClick={() => { if (activeLotId) void reloadTopology(activeLotId); }}
                >
                  <i className="fas fa-sync-alt tw-text-[0.8rem]" aria-hidden="true" /> Thử lại
                </Button>
              </div>
            ) : levels.length === 0 && zones.length === 0 ? (
              <div className="tw-rounded-[8px] tw-border tw-border-dashed tw-border-[#cbd5e1] tw-bg-[#f8fafc] tw-px-4 tw-py-8 tw-text-center">
                <p className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">Bãi xe chưa có tầng nào.</p>
                <p className="tw-m-0 tw-mt-1 tw-text-[0.84rem] tw-font-medium tw-text-[#64748b]">
                  Hãy thêm tầng cho bãi xe trước, sau đó thêm khu vực, cổng và làn.
                </p>
                {canDeleteLevel ? (
                  <Button size="sm" className="tw-mt-3 tw-h-10 tw-rounded-[8px] tw-px-3.5 tw-text-[0.84rem] tw-font-bold" onClick={openLevelModal}>
                    <i className="fas fa-plus tw-text-[0.75rem]" aria-hidden="true" /> Thêm tầng đầu tiên
                  </Button>
                ) : null}
              </div>
            ) : (
              <>
                {visibleLevels.map((lv) => {
                  const lvOpen = openLevels.includes(lv.id);
                  const lvZones = zonesOf(lv.id);
                  const lvSelected = selected?.kind === "level" && selected.id === lv.id;
                  return (
                    <div key={lv.id} className="tw-grid tw-gap-2.5">
                      <div className="tw-relative">
                        <Connector depth={0} />
                        <RowShell depth={0} selected={lvSelected}>
                          <ChevronBtn open={lvOpen} onClick={() => toggle(openLevels, lv.id, setOpenLevels)} label={lvOpen ? `Thu gọn ${lv.code}` : `Mở rộng ${lv.code}`} />
                          <IconBox icon="fas fa-layer-group" wrap="tw-text-[#2563eb]" />
                          <button
                            type="button"
                            onClick={() => {
                              setSelected({ kind: "level", id: lv.id });
                              toggle(openLevels, lv.id, setOpenLevels);
                            }}
                            className="tw-flex tw-min-w-0 tw-flex-1 tw-items-center tw-gap-2 tw-border-0 tw-bg-transparent tw-p-0 tw-text-left"
                          >
                            <strong className="tw-text-[0.92rem] tw-font-extrabold tw-text-[#0f172a]">{lv.code}</strong>
                            <span className="tw-text-[0.86rem] tw-text-[#94a3b8]">·</span>
                            <span className="tw-truncate tw-text-[0.88rem] tw-text-[#475569]">{lv.name}</span>
                            <span className="tw-ml-2">
                              <StatusPill label={statusLabel(lv.status)} tone={statusTone(lv.status)} />
                            </span>
                          </button>
                          <span className="tw-ml-auto tw-flex tw-items-center tw-gap-4">
                            <span className="tw-text-[0.86rem] tw-font-medium tw-text-[#475569]">{lv.totalCapacity} chỗ</span>
                            <RowActions
                              showDelete={canDeleteLevel && /^[0-9a-f-]{36}$/i.test(lv.id)}
                              deleteBlockedTip={lvZones.length > 0 ? `Không thể xóa: đang có ${lvZones.length} khu vực` : undefined}
                              onDelete={() => setDeleteTarget({ kind: "level", id: lv.id, label: `${lv.code} – ${lv.name}` })}
                              onEdit={() => goTab("/admin/parking-lots")}
                            />
                          </span>
                        </RowShell>
                      </div>

                      {lvOpen
                        ? (
                          <>
                            {lvZones.map((z) => {
                              const zOpen = openZones.includes(z.id);
                              const zGates = gatesOf(z.id);
                              const zSelected = selected?.kind === "zone" && selected.id === z.id;
                              return (
                                <div key={z.id} className="tw-grid tw-gap-2.5">
                                  <div className="tw-relative">
                                    <RowShell depth={1} selected={zSelected}>
                                      <Connector depth={1} />
                                      <ChevronBtn open={zOpen} onClick={() => toggle(openZones, z.id, setOpenZones)} label={zOpen ? `Thu gọn ${z.name}` : `Mở rộng ${z.name}`} />
                                      <IconBox icon="fas fa-th-large" wrap="tw-text-[#2563eb]" />
                                      <button
                                        type="button"
                                        onClick={() => setSelected({ kind: "zone", id: z.id })}
                                        className="tw-flex tw-min-w-0 tw-flex-1 tw-items-center tw-gap-2 tw-border-0 tw-bg-transparent tw-p-0 tw-text-left"
                                      >
                                        <strong className="tw-text-[0.9rem] tw-font-extrabold tw-text-[#0f172a]">{z.name}</strong>
                                        <span className="tw-text-[0.84rem] tw-text-[#94a3b8]">·</span>
                                        <span className="tw-truncate tw-text-[0.86rem] tw-text-[#475569]">{z.code}</span>
                                        {z.trackingMode === "SPACE" ? (
                                          <span className="tw-ml-1">
                                            <SpacePill />
                                          </span>
                                        ) : null}
                                        <span className="tw-ml-1">
                                          <StatusPill label={statusLabel(z.status)} tone={statusTone(z.status)} />
                                        </span>
                                      </button>
                                      <span className="tw-ml-auto tw-flex tw-items-center tw-gap-2">
                                        <span className="tw-text-[0.86rem] tw-font-medium tw-text-[#475569]">{z.capacity} chỗ</span>
                                        {canConfigureTopology ? (
                                          <span className="tw-flex tw-items-center">
                                            <button
                                              type="button"
                                              aria-label={`Chỉnh sửa ${z.name}`}
                                              disabled={formLoading}
                                              onClick={() => void openZoneEdit(z.id)}
                                              className="tw-inline-flex tw-h-7 tw-w-7 tw-items-center tw-justify-center tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.92rem] tw-text-[#475569] hover:tw-text-[#2563eb]"
                                            >
                                              <i className="far fa-edit" aria-hidden="true" />
                                            </button>
                                            <RowMenu
                                              label={z.name}
                                              onEdit={() => void openZoneEdit(z.id)}
                                              onChangeStatus={() => openStatusModal({ kind: "zone", id: z.id, label: `${z.code} – ${z.name}`, current: z.status })}
                                              onClose={() => openCloseConfirm({ kind: "zone", id: z.id, label: `khu vực ${z.code} – ${z.name}` })}
                                            />
                                          </span>
                                        ) : null}
                                      </span>
                                    </RowShell>
                                  </div>

                                  {zOpen
                                    ? (
                                      <>
                                        {zGates.map((g) => {
                                          const gOpen = openGates.includes(g.id);
                                          const gLanes = lanesOf(g.id);
                                          const gSelected = selected?.kind === "gate" && selected.id === g.id;
                                          return (
                                            <div key={g.id} className="tw-grid tw-gap-2.5">
                                              <div className="tw-relative">
                                                <RowShell depth={2} selected={gSelected}>
                                                  <Connector depth={2} />
                                                  <ChevronBtn open={gOpen} onClick={() => toggle(openGates, g.id, setOpenGates)} label={gOpen ? `Thu gọn ${g.name}` : `Mở rộng ${g.name}`} />
                                                  <IconBox icon="fas fa-door-open" wrap="tw-text-[#2563eb]" />
                                                  <button
                                                    type="button"
                                                    onClick={() => setSelected({ kind: "gate", id: g.id })}
                                                    className="tw-flex tw-min-w-0 tw-flex-1 tw-items-center tw-gap-2 tw-border-0 tw-bg-transparent tw-p-0 tw-text-left"
                                                  >
                                                    <strong className="tw-text-[0.9rem] tw-font-extrabold tw-text-[#0f172a]">{g.name}</strong>
                                                    <span className="tw-text-[0.84rem] tw-text-[#94a3b8]">·</span>
                                                    <span className="tw-truncate tw-text-[0.86rem] tw-text-[#475569]">{g.code}</span>
                                                    <span className="tw-ml-2">
                                                      <StatusPill label={statusLabel(g.status)} tone={statusTone(g.status)} />
                                                    </span>
                                                  </button>
                                                  {canConfigureTopology ? (
                                                    <span className="tw-ml-auto tw-flex tw-items-center">
                                                      <button
                                                        type="button"
                                                        aria-label={`Chỉnh sửa ${g.name}`}
                                                        disabled={formLoading}
                                                        onClick={() => void openGateEdit(g.id)}
                                                        className="tw-inline-flex tw-h-7 tw-w-7 tw-items-center tw-justify-center tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.92rem] tw-text-[#475569] hover:tw-text-[#2563eb]"
                                                      >
                                                        <i className="far fa-edit" aria-hidden="true" />
                                                      </button>
                                                      <RowMenu
                                                        label={g.name}
                                                        onEdit={() => void openGateEdit(g.id)}
                                                        onChangeStatus={() => openStatusModal({ kind: "gate", id: g.id, label: `${g.code} – ${g.name}`, current: g.status })}
                                                        onClose={() => openCloseConfirm({ kind: "gate", id: g.id, label: `cổng ${g.code} – ${g.name}` })}
                                                      />
                                                    </span>
                                                  ) : null}
                                                </RowShell>
                                              </div>
                                              {gOpen
                                                ? (
                                                  <>
                                                    {gLanes.map((l) => {
                                                      const lSelected = selected?.kind === "lane" && selected.id === l.id;
                                                      return (
                                                        <div key={l.id} className="tw-relative">
                                                          <RowShell depth={3} selected={lSelected}>
                                                            <Connector depth={3} />
                                                            <span className="tw-inline-flex tw-h-6 tw-w-6 tw-flex-shrink-0 tw-items-center tw-justify-center tw-text-[0.95rem]">
                                                              {l.direction === "IN" ? (
                                                                <i className="fas fa-arrow-right tw-text-[#16a34a]" aria-hidden="true" />
                                                              ) : (
                                                                <i className="fas fa-arrow-left tw-text-[#2563eb]" aria-hidden="true" />
                                                              )}
                                                            </span>
                                                            <button
                                                              type="button"
                                                              onClick={() => setSelected({ kind: "lane", id: l.id })}
                                                              className="tw-flex tw-min-w-0 tw-flex-1 tw-items-center tw-gap-2 tw-border-0 tw-bg-transparent tw-p-0 tw-text-left"
                                                            >
                                                              <strong className="tw-text-[0.88rem] tw-font-bold tw-text-[#334155]">{l.code}</strong>
                                                              <span className="tw-text-[0.84rem] tw-text-[#94a3b8]">·</span>
                                                              <span className="tw-truncate tw-text-[0.86rem] tw-text-[#475569]">{l.name}</span>
                                                              <span className="tw-ml-1">
                                                                <StatusPill label={statusLabel(l.status)} tone={statusTone(l.status)} />
                                                              </span>
                                                            </button>
                                                            {canConfigureTopology ? (
                                                              <span className="tw-ml-auto tw-flex tw-items-center">
                                                                <button
                                                                  type="button"
                                                                  aria-label={`Chỉnh sửa ${l.name}`}
                                                                  disabled={formLoading}
                                                                  onClick={() => void openLaneEdit(l.id)}
                                                                  className="tw-inline-flex tw-h-7 tw-w-7 tw-items-center tw-justify-center tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.92rem] tw-text-[#475569] hover:tw-text-[#2563eb]"
                                                                >
                                                                  <i className="far fa-edit" aria-hidden="true" />
                                                                </button>
                                                                <RowMenu
                                                                  label={l.name}
                                                                  onEdit={() => void openLaneEdit(l.id)}
                                                                  onChangeStatus={() => openStatusModal({ kind: "lane", id: l.id, label: `${l.code} – ${l.name}`, current: l.status })}
                                                                  onClose={() => openCloseConfirm({ kind: "lane", id: l.id, label: `làn ${l.code} – ${l.name}` })}
                                                                />
                                                              </span>
                                                            ) : null}
                                                          </RowShell>
                                                        </div>
                                                      );
                                                    })}
                                                    {gLanes.length === 0 ? (
                                                      <div className="tw-rounded-[8px] tw-border tw-border-dashed tw-border-[#cbd5e1] tw-bg-[#f8fafc] tw-px-4 tw-py-4 tw-text-center tw-text-[0.82rem] tw-font-semibold tw-text-[#64748b]" style={{ marginLeft: 78 }}>
                                                        Cổng chưa có làn nào.
                                                        {canConfigureTopology ? (
                                                          <button type="button" onClick={() => openFormModal({ type: "lane", mode: "create" })} className="tw-ml-2 tw-border-0 tw-bg-transparent tw-p-0 tw-font-bold tw-text-[#2563eb] hover:tw-underline">
                                                            Thêm làn
                                                          </button>
                                                        ) : null}
                                                      </div>
                                                    ) : null}
                                                  </>
                                                )
                                                : null}
                                            </div>
                                          );
                                        })}
                                        {zGates.length === 0 ? (
                                          <div className="tw-rounded-[8px] tw-border tw-border-dashed tw-border-[#cbd5e1] tw-bg-[#f8fafc] tw-px-4 tw-py-4 tw-text-center tw-text-[0.82rem] tw-font-semibold tw-text-[#64748b]" style={{ marginLeft: 52 }}>
                                            Khu vực chưa có cổng nào.
                                            {canConfigureTopology ? (
                                              <button type="button" onClick={() => openFormModal({ type: "gate", mode: "create" })} className="tw-ml-2 tw-border-0 tw-bg-transparent tw-p-0 tw-font-bold tw-text-[#2563eb] hover:tw-underline">
                                                Thêm cổng
                                              </button>
                                            ) : null}
                                          </div>
                                        ) : null}
                                      </>
                                    )
                                    : null}
                                </div>
                              );
                            })}
                            {lvZones.length === 0 ? (
                              <div className="tw-rounded-[8px] tw-border tw-border-dashed tw-border-[#cbd5e1] tw-bg-[#f8fafc] tw-px-4 tw-py-4 tw-text-center tw-text-[0.82rem] tw-font-semibold tw-text-[#64748b]" style={{ marginLeft: 26 }}>
                                Tầng chưa có khu vực nào.
                                {canConfigureTopology ? (
                                  <button type="button" onClick={() => openFormModal({ type: "zone", mode: "create" })} className="tw-ml-2 tw-border-0 tw-bg-transparent tw-p-0 tw-font-bold tw-text-[#2563eb] hover:tw-underline">
                                    Thêm khu
                                  </button>
                                ) : null}
                              </div>
                            ) : null}
                          </>
                        )
                        : null}
                    </div>
                  );
                })}
                {visibleLevels.length === 0 ? (
                  <div className="tw-rounded-[8px] tw-border tw-border-dashed tw-border-[#cbd5e1] tw-bg-[#f8fafc] tw-px-4 tw-py-8 tw-text-center tw-text-[0.88rem] tw-font-semibold tw-text-[#64748b]">
                    Không tìm thấy tầng / khu / cổng / làn phù hợp.
                  </div>
                ) : null}
                {unassignedZones.length > 0 ? (
                  <div className="tw-rounded-[8px] tw-border tw-border-solid tw-border-[#fde68a] tw-bg-[#fffbeb] tw-px-4 tw-py-3 tw-text-[0.84rem] tw-font-semibold tw-text-[#92400e]">
                    Có {unassignedZones.length} khu vực chưa gán tầng ({unassignedZones.map((zone) => zone.code).join(", ")}). Hãy chỉnh sửa để gán tầng.
                  </div>
                ) : null}
              </>
            )}
          </div>
        </Card>

        {/* Right: panel chi tiết động theo loại đang chọn */}
        <div className="tw-grid tw-gap-4">
          <Card className="tw-p-5">
            {selected?.kind === "level" && selectedLevel ? (
              <>
                <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
                  <h2 className="tw-m-0 tw-text-[1.12rem] tw-font-extrabold tw-text-[#0f172a]">Chi tiết tầng</h2>
                  <StatusPill label={statusLabel(selectedLevel.status)} tone={statusTone(selectedLevel.status)} />
                </div>
                <p className="tw-m-0 tw-mt-2 tw-text-[0.86rem]">
                  <span className="tw-font-extrabold tw-text-[#2563eb]">{selectedLevel.code}</span>
                  <span className="tw-mx-2 tw-text-[#94a3b8]">/</span>
                  <span className="tw-font-medium tw-text-[#475569]">{selectedLevel.name}</span>
                </p>
                <div className="tw-my-4 tw-h-px tw-bg-[#eef2f7]" />
                <dl className="tw-m-0 tw-grid tw-gap-[13px]">
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Mã tầng:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">{selectedLevel.code}</dd>
                  </div>
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Tên:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">{selectedLevel.name}</dd>
                  </div>
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Cao độ:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">{selectedLevel.elevation ?? "—"}</dd>
                  </div>
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Số khu:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">{selectedLevel.zoneCount}</dd>
                  </div>
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Tổng sức chứa:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">{selectedLevel.totalCapacity} chỗ</dd>
                  </div>
                </dl>
                {canConfigureTopology ? (
                  <div className="tw-mt-5">
                    <Button className="tw-h-11 tw-w-full tw-rounded-[8px] tw-font-bold" onClick={() => openFormModal({ type: "zone", mode: "create" })}>
                      <i className="fas fa-plus tw-text-[0.85rem]" aria-hidden="true" />
                      Thêm khu vào tầng này
                    </Button>
                  </div>
                ) : null}
                {canDeleteLevel ? (
                  <div className="tw-mt-3">
                    <Button variant="secondary" className="tw-h-11 tw-w-full tw-rounded-[8px] tw-border-[#dbe3f0] tw-font-bold tw-text-[#0f172a]" onClick={openLevelModal}>
                      <i className="fas fa-plus tw-text-[0.85rem]" aria-hidden="true" />
                      Thêm tầng mới (cùng bãi)
                    </Button>
                  </div>
                ) : null}
              </>
            ) : selected?.kind === "gate" && selectedGate ? (
              <>
                <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
                  <h2 className="tw-m-0 tw-text-[1.12rem] tw-font-extrabold tw-text-[#0f172a]">Chi tiết cổng</h2>
                  <StatusPill label={statusLabel(selectedGate.status)} tone={statusTone(selectedGate.status)} />
                </div>
                <p className="tw-m-0 tw-mt-2 tw-text-[0.86rem]">
                  <span className="tw-font-extrabold tw-text-[#2563eb]">{selectedGate.code}</span>
                  <span className="tw-mx-2 tw-text-[#94a3b8]">/</span>
                  <span className="tw-font-medium tw-text-[#475569]">{selectedGate.name}</span>
                </p>
                <div className="tw-my-4 tw-h-px tw-bg-[#eef2f7]" />
                <dl className="tw-m-0 tw-grid tw-gap-[13px]">
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Mã cổng:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">{selectedGate.code}</dd>
                  </div>
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Tên:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">{selectedGate.name}</dd>
                  </div>
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Khu vực:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">
                      {selectedZone ? `${selectedZone.code} — ${selectedZone.name}` : "—"}
                    </dd>
                  </div>
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Số làn:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">{selectedGate.laneCount}</dd>
                  </div>
                </dl>
                {canConfigureTopology ? (
                  <div className="tw-mt-5 tw-grid tw-grid-cols-2 tw-gap-3">
                    <Button className="tw-h-11 tw-rounded-[8px] tw-font-bold" loading={formLoading} onClick={() => void openGateEdit(selectedGate.id)}>
                      <i className="far fa-edit tw-text-[0.9rem]" aria-hidden="true" />
                      Chỉnh sửa
                    </Button>
                    <Button variant="secondary" className="tw-h-11 tw-rounded-[8px] tw-border-[#dbe3f0] tw-font-bold tw-text-[#0f172a]" onClick={() => openStatusModal({ kind: "gate", id: selectedGate.id, label: `${selectedGate.code} – ${selectedGate.name}`, current: selectedGate.status })}>
                      <i className="fas fa-sync-alt tw-text-[0.85rem]" aria-hidden="true" />
                      Đổi trạng thái
                    </Button>
                    <Button variant="danger" className="tw-col-span-2 tw-h-11 tw-rounded-[8px] tw-font-bold" onClick={() => openCloseConfirm({ kind: "gate", id: selectedGate.id, label: `cổng ${selectedGate.code} – ${selectedGate.name}` })}>
                      <i className="far fa-trash-alt tw-text-[0.9rem]" aria-hidden="true" />
                      Đóng cổng
                    </Button>
                  </div>
                ) : null}
              </>
            ) : selected?.kind === "lane" && selectedLane ? (
              <>
                <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
                  <h2 className="tw-m-0 tw-text-[1.12rem] tw-font-extrabold tw-text-[#0f172a]">Chi tiết làn</h2>
                  <StatusPill label={statusLabel(selectedLane.status)} tone={statusTone(selectedLane.status)} />
                </div>
                <p className="tw-m-0 tw-mt-2 tw-text-[0.86rem]">
                  <span className="tw-font-extrabold tw-text-[#2563eb]">{selectedLane.code}</span>
                  <span className="tw-mx-2 tw-text-[#94a3b8]">/</span>
                  <span className="tw-font-medium tw-text-[#475569]">{selectedLane.name}</span>
                </p>
                <div className="tw-my-4 tw-h-px tw-bg-[#eef2f7]" />
                <dl className="tw-m-0 tw-grid tw-gap-[13px]">
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Mã làn:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">{selectedLane.code}</dd>
                  </div>
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Tên:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">{selectedLane.name}</dd>
                  </div>
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Cổng:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">
                      {selectedGate ? `${selectedGate.code} — ${selectedGate.name}` : "—"}
                    </dd>
                  </div>
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Hướng:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">{selectedLane.direction === "IN" ? "Vào (IN)" : "Ra (OUT)"}</dd>
                  </div>
                </dl>
                {canConfigureTopology ? (
                  <div className="tw-mt-5 tw-grid tw-grid-cols-2 tw-gap-3">
                    <Button className="tw-h-11 tw-rounded-[8px] tw-font-bold" loading={formLoading} onClick={() => void openLaneEdit(selectedLane.id)}>
                      <i className="far fa-edit tw-text-[0.9rem]" aria-hidden="true" />
                      Chỉnh sửa
                    </Button>
                    <Button variant="secondary" className="tw-h-11 tw-rounded-[8px] tw-border-[#dbe3f0] tw-font-bold tw-text-[#0f172a]" onClick={() => openStatusModal({ kind: "lane", id: selectedLane.id, label: `${selectedLane.code} – ${selectedLane.name}`, current: selectedLane.status })}>
                      <i className="fas fa-sync-alt tw-text-[0.85rem]" aria-hidden="true" />
                      Đổi trạng thái
                    </Button>
                    <Button variant="danger" className="tw-col-span-2 tw-h-11 tw-rounded-[8px] tw-font-bold" onClick={() => openCloseConfirm({ kind: "lane", id: selectedLane.id, label: `làn ${selectedLane.code} – ${selectedLane.name}` })}>
                      <i className="far fa-trash-alt tw-text-[0.9rem]" aria-hidden="true" />
                      Đóng làn
                    </Button>
                  </div>
                ) : null}
              </>
            ) : selectedZone ? (
              <>
                <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
                  <h2 className="tw-m-0 tw-text-[1.12rem] tw-font-extrabold tw-text-[#0f172a]">Chi tiết khu vực</h2>
                  <StatusPill label={statusLabel(selectedZone.status)} tone={statusTone(selectedZone.status)} />
                </div>
                <p className="tw-m-0 tw-mt-2 tw-text-[0.86rem]">
                  <span className="tw-font-extrabold tw-text-[#2563eb]">{detailLevel?.code ?? "—"}</span>
                  <span className="tw-mx-2 tw-text-[#94a3b8]">/</span>
                  <span className="tw-font-medium tw-text-[#475569]">{selectedZone.name}</span>
                </p>
                <div className="tw-my-4 tw-h-px tw-bg-[#eef2f7]" />
                <dl className="tw-m-0 tw-grid tw-gap-[13px]">
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Mã khu vực:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">{selectedZone.code}</dd>
                  </div>
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Tên:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">{selectedZone.name}</dd>
                  </div>
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Tầng:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">
                      {detailLevel ? `${detailLevel.code} – ${detailLevel.name}` : "Chưa gán tầng"}
                    </dd>
                  </div>
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Loại phương tiện:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">
                      {selectedZone.vehicleTypeIds.length > 0
                        ? selectedZone.vehicleTypeIds.map(vehicleTypeNameOf).join(", ")
                        : "—"}
                    </dd>
                  </div>
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Chế độ quản lý:</dt>
                    <dd className="tw-m-0 tw-flex tw-items-center tw-gap-2 tw-text-[0.9rem] tw-font-medium tw-text-[#334155]">
                      {selectedZone.trackingMode === "SPACE" ? (<>Theo từng ô <SpacePill /></>) : "Theo sức chứa"}
                    </dd>
                  </div>
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Sức chứa:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">{selectedZone.capacity} chỗ</dd>
                  </div>
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Sơ đồ:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">
                      {selectedZone.layoutStatus ?? "NOT_CONFIGURED"}{selectedZone.layoutVersion != null && selectedZone.layoutVersion > 0 ? ` (v${selectedZone.layoutVersion})` : ""}
                    </dd>
                  </div>
                  <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3">
                    <dt className="tw-text-[0.86rem] tw-text-[#64748b]">Số cổng:</dt>
                    <dd className="tw-m-0 tw-text-[0.9rem] tw-font-bold tw-text-[#0f172a]">{selectedZone.gateCount}</dd>
                  </div>
                </dl>
                {canConfigureTopology ? (
                  <div className="tw-mt-5 tw-grid tw-grid-cols-2 tw-gap-3">
                    <Button className="tw-h-11 tw-rounded-[8px] tw-font-bold" loading={formLoading} onClick={() => void openZoneEdit(selectedZone.id)}>
                      <i className="far fa-edit tw-text-[0.9rem]" aria-hidden="true" />
                      Chỉnh sửa
                    </Button>
                    <Button variant="secondary" className="tw-h-11 tw-rounded-[8px] tw-border-[#dbe3f0] tw-font-bold tw-text-[#0f172a]" onClick={() => openStatusModal({ kind: "zone", id: selectedZone.id, label: `${selectedZone.code} – ${selectedZone.name}`, current: selectedZone.status })}>
                      <i className="fas fa-sync-alt tw-text-[0.85rem]" aria-hidden="true" />
                      Đổi trạng thái
                    </Button>
                    <Button variant="danger" className="tw-col-span-2 tw-h-11 tw-rounded-[8px] tw-font-bold" onClick={() => openCloseConfirm({ kind: "zone", id: selectedZone.id, label: `khu vực ${selectedZone.code} – ${selectedZone.name}` })}>
                      <i className="far fa-trash-alt tw-text-[0.9rem]" aria-hidden="true" />
                      Đóng khu vực
                    </Button>
                  </div>
                ) : null}
              </>
            ) : !hasActiveLot ? (
              <div className="tw-px-2 tw-py-8 tw-text-center">
                <i className="fas fa-lock tw-text-2xl tw-text-[#94a3b8]" aria-hidden="true" />
                <p className="tw-m-0 tw-mt-3 tw-text-[0.95rem] tw-font-bold tw-text-[#0f172a]">Chi tiết sẽ mở sau khi tạo bãi</p>
                <p className="tw-m-0 tw-mt-1 tw-text-[0.84rem] tw-font-medium tw-text-[#64748b]">
                  Chọn “Thêm bãi” để tạo bãi đầu tiên, rồi tiếp tục phân công Manager và cấu hình vận hành.
                </p>
              </div>
            ) : (
              <div className="tw-px-2 tw-py-8 tw-text-center">
                <p className="tw-m-0 tw-text-[0.95rem] tw-font-bold tw-text-[#0f172a]">Chưa chọn mục nào</p>
                <p className="tw-m-0 tw-mt-1 tw-text-[0.84rem] tw-font-medium tw-text-[#64748b]">
                  {levels.length === 0 ? "Bãi xe chưa có dữ liệu cấu trúc." : "Chọn một tầng, khu vực, cổng hoặc làn để xem chi tiết."}
                </p>
              </div>
            )}
          </Card>

          <Card className="tw-p-5">
            <h2 className="tw-m-0 tw-text-[1.12rem] tw-font-extrabold tw-text-[#0f172a]">Thao tác nhanh</h2>
            <div className="tw-mt-3 tw-grid tw-gap-2.5">
              <button
                type="button"
                disabled={!hasActiveLot}
                onClick={() => goTab("/admin/parking-layouts")}
                className="tw-flex tw-h-[52px] tw-w-full tw-items-center tw-gap-3 tw-rounded-[8px] tw-border tw-border-solid tw-border-[#e2e8f0] tw-bg-white tw-px-4 tw-text-left tw-transition hover:tw-border-[#bfdbfe] hover:tw-bg-[#eff6ff] disabled:tw-cursor-not-allowed disabled:tw-opacity-50"
              >
                <i className="far fa-map tw-text-[1.05rem] tw-text-[#2563eb]" aria-hidden="true" />
                <span className="tw-flex-1 tw-text-[0.92rem] tw-font-bold tw-text-[#2563eb]">Mở thiết kế sơ đồ</span>
                <i className="fas fa-chevron-right tw-text-[0.8rem] tw-text-[#94a3b8]" aria-hidden="true" />
              </button>
              <button
                type="button"
                disabled={!hasActiveLot}
                onClick={() => goTab("/admin/parking-live")}
                className="tw-flex tw-h-[52px] tw-w-full tw-items-center tw-gap-3 tw-rounded-[8px] tw-border tw-border-solid tw-border-[#e2e8f0] tw-bg-white tw-px-4 tw-text-left tw-transition hover:tw-border-[#bfdbfe] hover:tw-bg-[#eff6ff] disabled:tw-cursor-not-allowed disabled:tw-opacity-50"
              >
                <i className="far fa-camera tw-text-[1.05rem] tw-text-[#2563eb]" aria-hidden="true" />
                <span className="tw-flex-1 tw-text-[0.92rem] tw-font-bold tw-text-[#2563eb]">Xem trực tiếp</span>
                <i className="fas fa-chevron-right tw-text-[0.8rem] tw-text-[#94a3b8]" aria-hidden="true" />
              </button>
            </div>
          </Card>
        </div>
      </div>

      {/* Thêm bãi (wizard) + thêm tầng cho bãi hiện có */}
      <ParkingLotCreateWizard
        open={lotWizardOpen}
        onClose={() => setLotWizardOpen(false)}
        onCompleted={handleWizardCompleted}
      />
      <ParkingLotEditModal
        lot={activeLot}
        open={lotEditOpen}
        onClose={() => setLotEditOpen(false)}
        onCompleted={(updatedLot) => {
          setLots((current) => current.map((lot) => lot.parkingLotId === updatedLot.parkingLotId ? updatedLot : lot));
          setLotEditOpen(false);
          toast.success("Đã cập nhật bãi xe.");
        }}
      />
      <ParkingManagerAssignmentModal
        lot={activeLot}
        open={managerAssignmentOpen}
        onClose={() => setManagerAssignmentOpen(false)}
        onChanged={(message) => toast.success(message)}
      />

      <Modal
        open={levelModalOpen}
        title="Thêm tầng"
        description={activeLot ? `Tầng mới thuộc bãi ${activeLot.code} — ${activeLot.name}.` : undefined}
        onClose={() => { if (!levelSubmitting) setLevelModalOpen(false); }}
        actions={(
          <div className="tw-flex tw-justify-end tw-gap-2">
            <Button variant="secondary" disabled={levelSubmitting} onClick={() => setLevelModalOpen(false)}>Hủy</Button>
            <Button loading={levelSubmitting} onClick={() => void handleCreateLevel()}>
              <i className="fas fa-save" aria-hidden="true" /> Lưu tầng
            </Button>
          </div>
        )}
      >
        <div className="tw-grid tw-gap-3.5">
          {levelError ? (
            <p className="tw-m-0 tw-rounded-[8px] tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-px-3 tw-py-2 tw-text-[0.84rem] tw-font-semibold tw-text-red-700">
              {levelError}
            </p>
          ) : null}
          <FormField label="Mã tầng" required>
            <input className={fieldInputClass(false)} value={levelCode} onChange={(event) => setLevelCode(event.target.value.toUpperCase())} placeholder="VD: GROUND, L1, B1" maxLength={50} />
          </FormField>
          <FormField label="Tên tầng" required>
            <input className={fieldInputClass(false)} value={levelName} onChange={(event) => setLevelName(event.target.value)} placeholder="VD: Tầng trệt" maxLength={150} />
          </FormField>
          <FormField label="Cao độ" required>
            <input type="number" className={fieldInputClass(false)} value={levelElevation} disabled={levelSubmitting} onChange={(event) => setLevelElevation(event.target.value)} />
          </FormField>
        </div>
      </Modal>

      {/* Form modals */}
      {formModal?.type === "zone" ? (
        <ZoneFormModal
          open
          title={formModal.mode === "edit" ? "Chỉnh sửa khu vực" : "Thêm khu vực"}
          levels={levels}
          vehicleTypes={vehicleTypes}
          initial={formZoneInitial}
          defaultLevelId={formModal.mode === "edit" ? (formZoneInitial?.levelId ?? defaultLevelId) : defaultLevelId}
          submitting={formSubmitting}
          serverError={formServerError}
          onSubmit={(payload) => void handleZoneSubmit(payload)}
          onClose={() => { if (!formSubmitting) { setFormModal(null); setFormServerError(null); } }}
        />
      ) : null}
      {formModal?.type === "gate" ? (
        <GateFormModal
          open
          title={formModal.mode === "edit" ? "Chỉnh sửa cổng" : "Thêm cổng"}
          zones={zones}
          initial={formGateInitial}
          defaultZoneId={formModal.mode === "edit" ? (formGateInitial?.zoneId ?? defaultZoneId) : defaultZoneId}
          submitting={formSubmitting}
          serverError={formServerError}
          onSubmit={(payload) => void handleGateSubmit(payload)}
          onClose={() => { if (!formSubmitting) { setFormModal(null); setFormServerError(null); } }}
        />
      ) : null}
      {formModal?.type === "lane" ? (
        <LaneFormModal
          open
          title={formModal.mode === "edit" ? "Chỉnh sửa làn" : "Thêm làn"}
          gates={gates}
          initial={formLaneInitial}
          defaultGateId={formModal.mode === "edit" ? (formLaneInitial?.gateId ?? defaultGateId) : defaultGateId}
          submitting={formSubmitting}
          serverError={formServerError}
          onSubmit={(payload) => void handleLaneSubmit(payload)}
          onClose={() => { if (!formSubmitting) { setFormModal(null); setFormServerError(null); } }}
        />
      ) : null}

      {/* Đổi trạng thái */}
      <StatusChangeModal
        target={statusTarget}
        kindLabel={statusTarget?.kind === "zone" ? "khu vực" : statusTarget?.kind === "gate" ? "cổng" : "làn"}
        submitting={statusSubmitting}
        serverError={statusServerError}
        onSubmit={(next) => void handleStatusSubmit(next)}
        onClose={() => { if (!statusSubmitting) { setStatusTarget(null); setStatusServerError(null); } }}
      />

      {/* Đóng (xóa mềm) zone/gate/lane */}
      <ConfirmDialog
        open={Boolean(closeTarget)}
        title={`Đóng ${closeTarget?.kind === "zone" ? "khu vực" : closeTarget?.kind === "gate" ? "cổng" : "làn"}?`}
        description="Đóng là xóa mềm: dữ liệu lịch sử được giữ lại."
        message={closeTarget ? `Bạn đang đóng ${closeTarget.label}. Hệ thống sẽ chặn nếu còn ràng buộc hoạt động.` : ""}
        confirmLabel="Đóng"
        cancelLabel="Hủy"
        tone="danger"
        loading={closing}
        onCancel={() => { if (!closing) setCloseTarget(null); }}
        onConfirm={() => void handleConfirmClose()}
      />

      {/* Xóa tầng / bãi (giữ luồng hiện có) */}
      <Modal
        open={Boolean(deleteTarget)}
        width="sm"
        title={deleteTarget?.kind === "level" ? "Xóa tầng bãi xe?" : "Xóa bãi xe?"}
        description={deleteTarget?.kind === "level"
          ? "Tầng chỉ có thể xóa khi không còn khu vực trực thuộc."
          : "Hệ thống sẽ đóng bãi và loại bãi khỏi vận hành; dữ liệu lịch sử vẫn được giữ lại."}
        onClose={() => { if (!deleting) setDeleteTarget(null); }}
        actions={(
          <div className="tw-flex tw-justify-end tw-gap-2">
            <Button variant="secondary" disabled={deleting} onClick={() => setDeleteTarget(null)}>Hủy</Button>
            <Button
              variant="danger"
              loading={deleting}
              disabled={deleteTarget?.kind === "lot" && activeZoneCount > 0}
              onClick={() => void handleConfirmDelete()}
            >
              <i className="far fa-trash-alt" aria-hidden="true" />
              {deleteTarget?.kind === "level" ? "Xóa tầng" : "Xóa bãi"}
            </Button>
          </div>
        )}
      >
        <div className="tw-flex tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-p-3">
          <i className="fas fa-exclamation-triangle tw-mt-0.5 tw-text-red-500" aria-hidden="true" />
          <p className="tw-m-0 tw-text-[0.82rem] tw-font-semibold tw-leading-5 tw-text-red-700">
            {deleteTarget?.kind === "level"
              ? `Bạn đang xóa tầng ${deleteTarget.label}. Thao tác này không thể hoàn tác.`
              : activeZoneCount > 0
                ? `Bãi đang có ${activeZoneCount} khu vực hoạt động. Hãy đóng hoặc chuyển các khu vực sang bảo trì trước khi xóa bãi.`
                : `Bạn đang xóa bãi ${deleteTarget?.label ?? ""}. Bãi sẽ chuyển sang trạng thái Đã đóng.`}
          </p>
        </div>
      </Modal>
    </main>
  );
}
