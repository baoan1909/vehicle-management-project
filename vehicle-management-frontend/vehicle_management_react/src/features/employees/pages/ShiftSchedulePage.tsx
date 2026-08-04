import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";

import { Badge, Button, Card, EntityAvatar, SelectMenu, useToast } from "@/components/ui";
import { ShiftConfigurationPanel } from "@/features/employees/components/ShiftConfigurationPanel";
import { getEmployees, type EmployeeApiResponse } from "@/features/employees/api/employeesApi";
import {
  approveWorkScheduleWeek,
  createShiftAssignment,
  deleteShiftAssignment,
  generateWorkScheduleWeek,
  getEmployeeRosterRules,
  getGates,
  getParkingLots,
  getShiftAssignments,
  getShiftTemplates,
  getShifts,
  replaceShiftAssignment,
  swapShiftAssignments,
  type EmployeeRosterRuleApiResponse,
  type GateApiResponse,
  type ParkingLotApiResponse,
  type ShiftApiResponse,
  type ShiftAssignmentApiResponse,
  type ShiftStatusApi,
  type ShiftTemplateApiResponse,
  type ShiftTypeApi,
} from "@/features/employees/api/shiftsApi";
import { cn } from "@/lib/cn";

type DrawerPhase = "opening" | "open" | "closing";
type ShiftWorkspace = "schedule" | "templates" | "rules";

type ShiftAssignmentView = {
  assignmentId: string;
  employeeId: string;
  employeeName: string;
  gateId?: string | null;
  gateName: string;
  initials: string;
  role: string;
  status: string;
};

type ShiftCell = {
  assigned: number;
  assignments: ShiftAssignmentView[];
  capacity: number;
  code: string;
  dayIndex: number;
  endTime: string;
  id: string;
  isPlaceholder: boolean;
  lotName: string;
  openingCash: string;
  closingCash: string;
  shiftDate: string;
  startTime: string;
  status: ShiftStatusApi;
  type: ShiftTypeApi;
};

const DRAWER_ANIMATION_MS = 280;
const shiftTypes: ShiftTypeApi[] = ["MORNING", "AFTERNOON", "NIGHT"];

const shiftTypeRows: Array<{ capacity: number; icon: string; label: string; tone: "blue" | "orange" | "indigo"; type: ShiftTypeApi }> = [
  { capacity: 2, icon: "far fa-sun", label: "Ca sáng", tone: "blue", type: "MORNING" },
  { capacity: 2, icon: "fas fa-sun", label: "Ca chiều", tone: "orange", type: "AFTERNOON" },
  { capacity: 2, icon: "far fa-moon", label: "Ca đêm", tone: "indigo", type: "NIGHT" },
];

const statusOptions = [
  { label: "Tất cả trạng thái", value: "all" },
  { label: "Nháp", value: "DRAFT" },
  { label: "Đã lên lịch", value: "SCHEDULED" },
  { label: "Đang mở", value: "OPEN" },
  { label: "Đã đóng", value: "CLOSED" },
  { label: "Đã hủy", value: "CANCELLED" },
];

const shiftTypeOptions = [
  { label: "Tất cả loại ca", value: "all" },
  { label: "Ca sáng", value: "MORNING" },
  { label: "Ca chiều", value: "AFTERNOON" },
  { label: "Ca đêm", value: "NIGHT" },
];

const statusLabels: Record<ShiftStatusApi, string> = {
  CANCELLED: "Đã hủy",
  CLOSED: "Đã đóng",
  DRAFT: "Nháp",
  OPEN: "Đang mở",
  SCHEDULED: "Đã lên lịch",
};

const shiftTypeLabels: Record<ShiftTypeApi, string> = {
  AFTERNOON: "Ca chiều",
  MORNING: "Ca sáng",
  NIGHT: "Ca đêm",
};

function pad(value: number) {
  return `${value}`.padStart(2, "0");
}

function toIsoDate(date: Date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function parseIsoDate(value: string) {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day);
}

function startOfWeek(date: Date) {
  const result = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const day = result.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  result.setDate(result.getDate() + diff);
  return result;
}

function addDays(date: Date, amount: number) {
  const result = new Date(date);
  result.setDate(result.getDate() + amount);
  return result;
}

function addWeeks(date: Date, amount: number) {
  return addDays(date, amount * 7);
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit" }).format(parseIsoDate(value));
}

function formatFullDate(value: string) {
  return new Intl.DateTimeFormat("vi-VN").format(parseIsoDate(value));
}

function formatMoney(value?: string | number | null) {
  const amount = Number(value ?? 0);
  return new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 0, style: "currency", currency: "VND" }).format(Number.isFinite(amount) ? amount : 0);
}

function getEmployeeName(employee?: EmployeeApiResponse) {
  return employee?.userProfile?.fullName ?? employee?.employeeCode ?? "Chưa rõ nhân viên";
}

function getInitials(name: string) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(-2)
    .map((part) => part[0]?.toUpperCase())
    .join("") || "NV";
}

function shortCode(prefix: string, id?: string | null) {
  return id ? `${prefix}-${id.slice(0, 8).toUpperCase()}` : "Chưa có";
}

function buildMap<T extends Record<K, string>, K extends keyof T>(items: T[], key: K) {
  return new Map(items.map((item) => [item[key], item]));
}

function statusTone(status: ShiftStatusApi) {
  if (status === "SCHEDULED") return "primary";
  if (status === "OPEN") return "success";
  if (status === "DRAFT") return "warning";
  if (status === "CANCELLED") return "danger";
  return "neutral";
}

function statusClassName(status: ShiftStatusApi) {
  if (status === "SCHEDULED") return "tw-bg-brand-50 tw-text-vm-primary";
  if (status === "OPEN") return "tw-bg-emerald-50 tw-text-emerald-700";
  if (status === "DRAFT") return "tw-bg-orange-50 tw-text-orange-600";
  if (status === "CANCELLED") return "tw-bg-red-50 tw-text-red-600";
  return "tw-bg-vm-slate-50 tw-text-vm-slate-700";
}

function shiftRowTone(tone: "blue" | "orange" | "indigo") {
  if (tone === "orange") return "tw-bg-orange-50 tw-text-orange-500";
  if (tone === "indigo") return "tw-bg-indigo-50 tw-text-indigo-500";
  return "tw-bg-brand-50 tw-text-vm-primary";
}

function toAssignmentView(
  assignment: ShiftAssignmentApiResponse,
  employeeMap: Map<string, EmployeeApiResponse>,
  gateMap: Map<string, GateApiResponse>,
): ShiftAssignmentView {
  const employee = employeeMap.get(assignment.employeeId);
  const name = getEmployeeName(employee);
  const gate = assignment.gateId ? gateMap.get(assignment.gateId) : undefined;

  return {
    assignmentId: assignment.shiftAssignmentId,
    employeeId: assignment.employeeId,
    employeeName: name,
    gateId: assignment.gateId,
    gateName: gate?.name ?? gate?.code ?? shortCode("Cổng", assignment.gateId),
    initials: getInitials(name),
    role: employee?.jobTitle ?? "Nhân viên ca trực",
    status: assignment.status,
  };
}

function buildShiftCells({
  assignments,
  employees,
  gates,
  parkingLots,
  shifts,
  weekStartDate,
}: {
  assignments: ShiftAssignmentApiResponse[];
  employees: EmployeeApiResponse[];
  gates: GateApiResponse[];
  parkingLots: ParkingLotApiResponse[];
  shifts: ShiftApiResponse[];
  weekStartDate: string;
}) {
  const employeeMap = buildMap(employees, "employeeId");
  const gateMap = buildMap(gates, "gateId");
  const lotMap = buildMap(parkingLots, "parkingLotId");
  const assignmentsByShift = new Map<string, ShiftAssignmentView[]>();

  assignments.forEach((assignment) => {
    const next = assignmentsByShift.get(assignment.shiftId) ?? [];
    next.push(toAssignmentView(assignment, employeeMap, gateMap));
    assignmentsByShift.set(assignment.shiftId, next);
  });

  const shiftBySlot = new Map<string, ShiftApiResponse>();
  shifts.forEach((shift) => {
    shiftBySlot.set(`${shift.shiftDate}:${shift.shiftType}`, shift);
  });

  return Array.from({ length: 7 }, (_, dayIndex) => {
    const shiftDate = toIsoDate(addDays(parseIsoDate(weekStartDate), dayIndex));
    return shiftTypes.map((type) => {
      const shift = shiftBySlot.get(`${shiftDate}:${type}`);
      const row = shiftTypeRows.find((item) => item.type === type)!;
      const shiftAssignments = shift ? assignmentsByShift.get(shift.shiftId) ?? [] : [];

      return {
        assigned: shiftAssignments.filter((assignment) => assignment.status !== "REMOVED").length,
        assignments: shiftAssignments,
        capacity: row.capacity,
        closingCash: formatMoney(shift?.closingCash),
        code: shift?.shiftCode ?? "Chưa có ca",
        dayIndex,
        endTime: shift?.endTime ?? defaultShiftTime(type).end,
        id: shift?.shiftId ?? `empty-${shiftDate}-${type}`,
        isPlaceholder: !shift,
        lotName: shift ? (lotMap.get(shift.parkingLotId)?.name ?? shortCode("Bãi", shift.parkingLotId)) : "Chưa tạo lịch",
        openingCash: formatMoney(shift?.openingCash),
        shiftDate,
        startTime: shift?.startTime ?? defaultShiftTime(type).start,
        status: shift?.status ?? "DRAFT",
        type,
      } satisfies ShiftCell;
    });
  }).flat();
}

function defaultShiftTime(type: ShiftTypeApi) {
  if (type === "MORNING") return { start: "06:00", end: "14:00" };
  if (type === "AFTERNOON") return { start: "14:00", end: "22:00" };
  return { start: "22:00", end: "06:00" };
}

function ShiftMetricCard({
  icon,
  label,
  tone,
  value,
}: {
  icon: string;
  label: string;
  tone: "blue" | "green" | "orange" | "red";
  value: string;
}) {
  const toneClassName = {
    blue: "tw-bg-brand-50 tw-text-vm-primary",
    green: "tw-bg-emerald-50 tw-text-emerald-600",
    orange: "tw-bg-orange-50 tw-text-orange-500",
    red: "tw-bg-red-50 tw-text-red-500",
  }[tone];

  return (
    <Card className="tw-min-h-[104px] tw-p-4">
      <div className="tw-flex tw-items-center tw-gap-4">
        <span className={cn("tw-inline-flex tw-h-14 tw-w-14 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-text-[1.28rem]", toneClassName)}>
          <i className={icon} />
        </span>
        <div className="tw-min-w-0">
          <p className="tw-m-0 tw-text-[0.88rem] tw-font-extrabold tw-text-vm-slate-700">{label}</p>
          <strong className="tw-mt-2 tw-block tw-text-[1.8rem] tw-font-extrabold tw-leading-none tw-text-vm-slate-900">{value}</strong>
        </div>
      </div>
    </Card>
  );
}

function ShiftCard({ shift, selected, onSelect }: { shift: ShiftCell; selected: boolean; onSelect: () => void }) {
  const isMissing = !shift.isPlaceholder && shift.assigned < shift.capacity;
  const missingCount = Math.max(shift.capacity - shift.assigned, 0);
  const mainAssignment = shift.assignments[0];
  const assignmentLabel = shift.isPlaceholder
    ? "Chưa tạo"
    : isMissing
      ? `Còn thiếu ${missingCount}`
      : "Đủ nhân sự";

  return (
    <button
      className={cn(
        "tw-flex tw-min-h-[126px] tw-w-full tw-flex-col tw-rounded-vm-md tw-border tw-border-solid tw-bg-white tw-p-3 tw-text-left tw-transition hover:tw-border-brand-100 hover:tw-bg-brand-50/30",
        selected ? "tw-border-vm-primary tw-shadow-[0_0_0_3px_rgba(37,99,235,0.1)]" : "tw-border-vm-slate-100",
        shift.isPlaceholder ? "tw-bg-vm-slate-25 tw-opacity-80" : "",
      )}
      type="button"
      onClick={onSelect}
    >
      <Badge tone={statusTone(shift.status)} className="tw-w-fit tw-rounded-full tw-px-2 tw-py-0.5 tw-text-[0.62rem]">
        {statusLabels[shift.status]}
      </Badge>
      <div className="tw-mt-3 tw-rounded-vm-md tw-bg-vm-slate-25 tw-px-2.5 tw-py-2">
        <strong className={cn("tw-block tw-text-[0.82rem] tw-font-extrabold", isMissing ? "tw-text-orange-600" : "tw-text-emerald-700")}>
          {assignmentLabel}
        </strong>
        <small className="tw-mt-0.5 tw-block tw-text-[0.68rem] tw-font-semibold tw-text-vm-slate-500">
          {shift.code}
        </small>
      </div>
      <div className="tw-mt-3 tw-flex tw-items-center tw-gap-2 tw-rounded-vm-sm tw-bg-brand-50 tw-px-2 tw-py-1.5 tw-text-[0.7rem] tw-font-extrabold tw-text-vm-primary">
        <i className="far fa-calendar-check" />
        {mainAssignment?.gateName ?? "Chưa gán cổng"}
      </div>
      <div className="tw-mt-auto tw-flex tw-items-center tw-gap-2 tw-text-[0.78rem] tw-font-extrabold">
        <i className="far fa-user tw-text-vm-slate-500" />
        <span className={isMissing ? "tw-text-red-500" : "tw-text-vm-slate-900"}>{shift.assigned}</span>
        <span className="tw-text-vm-slate-500">/ {shift.capacity}</span>
      </div>
    </button>
  );
}

function WeeklyScheduleBoard({
  days,
  onSelectShift,
  selectedShiftId,
  shifts,
}: {
  days: Array<{ date: string; label: string }>;
  onSelectShift: (shift: ShiftCell) => void;
  selectedShiftId: string | null;
  shifts: ShiftCell[];
}) {
  return (
    <Card className="tw-overflow-hidden">
      <div className="tw-overflow-x-auto tw-[scrollbar-width:none] tw-[-ms-overflow-style:none] [&::-webkit-scrollbar]:tw-hidden">
        <div className="tw-min-w-[920px]">
          <div className="tw-grid tw-grid-cols-[96px_repeat(7,minmax(108px,1fr))] tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100">
            <div className="tw-flex tw-min-h-[66px] tw-items-center tw-justify-center tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500">Ca / Thời gian</div>
            {days.map((day) => (
              <div className="tw-flex tw-min-h-[66px] tw-flex-col tw-items-center tw-justify-center tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 last:tw-border-r-0" key={day.date}>
                <strong className="tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-900">{day.label}</strong>
                <span className="tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">{formatDate(day.date)}</span>
              </div>
            ))}
          </div>

          {shiftTypeRows.map((row) => (
            <div className="tw-grid tw-grid-cols-[96px_repeat(7,minmax(108px,1fr))] tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 last:tw-border-b-0" key={row.type}>
              <div className="tw-flex tw-min-h-[184px] tw-flex-col tw-items-center tw-justify-center tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3 tw-text-center">
                <span className={cn("tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-full tw-text-[1.05rem]", shiftRowTone(row.tone))}>
                  <i className={row.icon} />
                </span>
                <strong className="tw-mt-3 tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-900">{row.label}</strong>
                <span className="tw-mt-1 tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500">{defaultShiftTime(row.type).start} - {defaultShiftTime(row.type).end}</span>
                <span className="tw-mt-3 tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-700">{shifts.filter((shift) => shift.type === row.type && !shift.isPlaceholder).length} ca</span>
              </div>
              {days.map((day, index) => {
                const shift = shifts.find((item) => item.type === row.type && item.dayIndex === index)!;
                return (
                  <div className="tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 tw-p-2.5 last:tw-border-r-0" key={`${row.type}-${day.date}`}>
                    <ShiftCard shift={shift} selected={selectedShiftId === shift.id} onSelect={() => onSelectShift(shift)} />
                  </div>
                );
              })}
            </div>
          ))}
        </div>
      </div>
      <div className="tw-m-4 tw-flex tw-w-fit tw-flex-wrap tw-gap-7 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-py-3">
        {[
          ["Nháp", "tw-bg-orange-500"],
          ["Đã lên lịch", "tw-bg-vm-primary"],
          ["Đang mở", "tw-bg-emerald-600"],
          ["Đã đóng", "tw-bg-vm-slate-500"],
          ["Đã hủy", "tw-bg-red-500"],
        ].map(([label, color]) => (
          <span className="tw-inline-flex tw-items-center tw-gap-2 tw-text-[0.72rem] tw-font-extrabold tw-text-vm-slate-700" key={label}>
            <span className={cn("tw-h-2.5 tw-w-2.5 tw-rounded-full", color)} />
            {label}
          </span>
        ))}
      </div>
    </Card>
  );
}

function WeekSummaryPanel({ shifts }: { shifts: ShiftCell[] }) {
  const realShifts = shifts.filter((shift) => !shift.isPlaceholder);
  const totalCapacity = realShifts.reduce((total, shift) => total + shift.capacity, 0);
  const totalAssigned = realShifts.reduce((total, shift) => total + shift.assigned, 0);
  const assignmentPercent = totalCapacity ? Math.round((totalAssigned / totalCapacity) * 100) : 0;
  const missingShifts = realShifts.filter((shift) => shift.assigned < shift.capacity).length;

  return (
    <aside className="tw-mt-4 tw-grid tw-grid-cols-[0.85fr_1.2fr] tw-gap-4 max-[1024px]:tw-grid-cols-1">
      <Card className="tw-p-4">
        <h2 className="tw-m-0 tw-text-[1.02rem] tw-font-extrabold tw-text-vm-slate-900">Tổng quan tuần</h2>
        <div className="tw-mt-4 tw-flex tw-items-center tw-gap-5">
          <div className="tw-relative tw-flex tw-h-[106px] tw-w-[106px] tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full" style={{ background: `conic-gradient(#2563EB 0 ${assignmentPercent}%, #E2E8F0 ${assignmentPercent}% 100%)` }}>
            <div className="tw-flex tw-h-[72px] tw-w-[72px] tw-flex-col tw-items-center tw-justify-center tw-rounded-full tw-bg-white">
              <strong className="tw-text-[1.28rem] tw-font-extrabold tw-text-vm-slate-900">{assignmentPercent}%</strong>
              <span className="tw-text-[0.62rem] tw-font-extrabold tw-text-vm-slate-700">đã phân công</span>
            </div>
          </div>
          <div className="tw-grid tw-flex-1 tw-gap-3">
            {[
              ["Tổng ca", `${realShifts.length}`, "tw-bg-vm-primary"],
              ["Nhân sự đã gán", `${totalAssigned}`, "tw-bg-emerald-500"],
              ["Ca thiếu người", `${missingShifts}`, "tw-bg-red-500"],
              ["Ca đang mở", `${realShifts.filter((shift) => shift.status === "OPEN").length}`, "tw-bg-orange-500"],
            ].map(([label, value, color]) => (
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-3" key={label}>
                <span className="tw-inline-flex tw-items-center tw-gap-2 tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-700">
                  <span className={cn("tw-h-2 tw-w-2 tw-rounded-full", color)} />
                  {label}
                </span>
                <strong className="tw-text-[0.88rem] tw-font-extrabold tw-text-vm-slate-900">{value}</strong>
              </div>
            ))}
          </div>
        </div>
      </Card>

      <Card className="tw-p-4">
        <h3 className="tw-m-0 tw-text-[1.02rem] tw-font-extrabold tw-text-vm-slate-900">Cảnh báo</h3>
        <div className="tw-mt-4 tw-grid tw-gap-3">
          {missingShifts ? (
            realShifts.filter((shift) => shift.assigned < shift.capacity).slice(0, 4).map((shift) => (
              <div className="tw-flex tw-gap-3" key={shift.id}>
                <i className="fas fa-exclamation-triangle tw-mt-0.5 tw-text-orange-500" />
                <p className="tw-m-0 tw-text-[0.76rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-700">
                  {shiftTypeLabels[shift.type]} ngày {formatFullDate(shift.shiftDate)} còn thiếu {shift.capacity - shift.assigned} nhân sự.
                </p>
              </div>
            ))
          ) : (
            <div className="tw-flex tw-gap-3">
              <i className="far fa-check-circle tw-mt-0.5 tw-text-emerald-500" />
              <p className="tw-m-0 tw-text-[0.76rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-700">Không có ca thiếu nhân sự trong dữ liệu tuần này.</p>
            </div>
          )}
        </div>
      </Card>
    </aside>
  );
}

function ShiftDetailDrawer({
  allShifts,
  employees,
  gates,
  isOpen,
  onChanged,
  onClose,
  shift,
}: {
  allShifts: ShiftCell[];
  employees: EmployeeApiResponse[];
  gates: GateApiResponse[];
  isOpen: boolean;
  onChanged: () => Promise<void> | void;
  onClose: () => void;
  shift: ShiftCell | null;
}) {
  const toast = useToast();
  const [isRendered, setIsRendered] = useState(isOpen);
  const [phase, setPhase] = useState<DrawerPhase>(isOpen ? "open" : "closing");
  const [assignmentAction, setAssignmentAction] = useState<"add" | "replace" | "swap">("add");
  const [selectedAssignmentId, setSelectedAssignmentId] = useState("");
  const [selectedEmployeeId, setSelectedEmployeeId] = useState("");
  const [selectedGateId, setSelectedGateId] = useState("");
  const [secondAssignmentId, setSecondAssignmentId] = useState("");
  const [actionReason, setActionReason] = useState("");
  const [actionError, setActionError] = useState("");
  const [savingAction, setSavingAction] = useState(false);

  const employeeOptions = useMemo(
    () => employees.map((employee) => ({ label: getEmployeeName(employee), value: employee.employeeId })),
    [employees],
  );
  const gateOptions = useMemo(
    () => gates.map((gate) => ({ label: `${gate.code} - ${gate.name}`, value: gate.gateId })),
    [gates],
  );
  const currentAssignmentOptions = useMemo(
    () => shift?.assignments.map((assignment) => ({ label: `${assignment.employeeName} - ${assignment.gateName}`, value: assignment.assignmentId })) ?? [],
    [shift],
  );
  const allAssignmentOptions = useMemo(
    () => allShifts
      .filter((item) => !item.isPlaceholder)
      .flatMap((item) => item.assignments.map((assignment) => ({
        label: `${item.code} - ${assignment.employeeName}`,
        value: assignment.assignmentId,
      }))),
    [allShifts],
  );

  useEffect(() => {
    if (isOpen) {
      setIsRendered(true);
      setPhase("opening");
      const openTimer = window.setTimeout(() => setPhase("open"), DRAWER_ANIMATION_MS);
      return () => window.clearTimeout(openTimer);
    }

    if (!isRendered) return undefined;

    setPhase("closing");
    const closeTimer = window.setTimeout(() => setIsRendered(false), DRAWER_ANIMATION_MS);
    return () => window.clearTimeout(closeTimer);
  }, [isOpen, isRendered]);

  useEffect(() => {
    if (!isRendered) return undefined;

    const previousBodyOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousBodyOverflow;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [isRendered, onClose]);

  useEffect(() => {
    setActionError("");
    setSelectedAssignmentId(shift?.assignments[0]?.assignmentId ?? "");
    setSelectedEmployeeId(employeeOptions[0]?.value ?? "");
    setSelectedGateId(gateOptions[0]?.value ?? "");
    setSecondAssignmentId(allAssignmentOptions.find((option) => option.value !== shift?.assignments[0]?.assignmentId)?.value ?? "");
    setActionReason("");
  }, [allAssignmentOptions, employeeOptions, gateOptions, shift]);

  async function handleAssignmentSubmit(event: FormEvent) {
    event.preventDefault();

    if (!shift || shift.isPlaceholder) {
      setActionError("Chưa có ca trực để thao tác phân công.");
      return;
    }

    setActionError("");
    setSavingAction(true);

    try {
      if (assignmentAction === "add") {
        if (!selectedEmployeeId || !selectedGateId) {
          throw new Error("Vui lòng chọn nhân viên và cổng.");
        }

        await createShiftAssignment(shift.id, {
          employeeId: selectedEmployeeId,
          gateId: selectedGateId,
        });
        toast.success("Đã thêm phân công ca trực.");
      }

      if (assignmentAction === "replace") {
        if (!selectedAssignmentId || !selectedEmployeeId) {
          throw new Error("Vui lòng chọn phân công và nhân viên thay thế.");
        }

        await replaceShiftAssignment(selectedAssignmentId, {
          reason: actionReason || null,
          replacementEmployeeId: selectedEmployeeId,
        });
        toast.success("Đã thay nhân viên trong ca.");
      }

      if (assignmentAction === "swap") {
        if (!selectedAssignmentId || !secondAssignmentId || selectedAssignmentId === secondAssignmentId) {
          throw new Error("Vui lòng chọn hai phân công khác nhau để đổi ca.");
        }

        await swapShiftAssignments({
          firstAssignmentId: selectedAssignmentId,
          reason: actionReason || null,
          secondAssignmentId,
        });
        toast.success("Đã đổi ca trực.");
      }

      await onChanged();
    } catch (error) {
      setActionError(error instanceof Error ? error.message : "Không thể thực hiện thao tác phân công.");
    } finally {
      setSavingAction(false);
    }
  }

  async function handleDeleteAssignment(assignmentId: string) {
    setActionError("");
    setSavingAction(true);

    try {
      await deleteShiftAssignment(assignmentId);
      toast.success("Đã xóa phân công.");
      await onChanged();
    } catch (error) {
      setActionError(error instanceof Error ? error.message : "Không thể xóa phân công.");
    } finally {
      setSavingAction(false);
    }
  }

  if (!isRendered || !shift) return null;

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[2200] tw-isolate tw-flex tw-justify-end" role="dialog" aria-modal="true" aria-labelledby="shift-detail-drawer-title">
      <button
        type="button"
        aria-label="Đóng chi tiết ca trực"
        className={cn("tw-absolute tw-inset-0 tw-border-0 tw-bg-transparent tw-p-0 tw-will-change-opacity", phase === "opening" ? "tw-animate-vm-drawer-backdrop-in" : "", phase === "closing" ? "tw-animate-vm-drawer-backdrop-out" : "")}
        onClick={onClose}
      />

      <aside
        className={cn(
          "tw-relative tw-z-[1] tw-flex tw-h-full tw-w-[min(100%,520px)] tw-transform-gpu tw-flex-col tw-border-0 tw-border-l tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-vm-drawer tw-will-change-transform [backface-visibility:hidden] max-[768px]:tw-w-full",
          phase === "opening" ? "tw-animate-vm-drawer-panel-in" : "",
          phase === "closing" ? "tw-animate-vm-drawer-panel-out" : "",
        )}
      >
        <header className="tw-flex tw-items-start tw-justify-between tw-gap-4 tw-px-6 tw-py-5">
          <h2 id="shift-detail-drawer-title" className="tw-m-0 tw-text-[1.28rem] tw-font-extrabold tw-text-vm-slate-900">Chi tiết ca trực</h2>
          <button className="tw-inline-flex tw-h-9 tw-w-9 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-slate-600 tw-transition hover:tw-bg-vm-slate-100 hover:tw-text-vm-slate-900 focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus" type="button" aria-label="Đóng" onClick={onClose}>
            <i className="fas fa-times" />
          </button>
        </header>

        <div className="tw-min-h-0 tw-flex-1 tw-overflow-y-auto tw-px-6 tw-pb-5 tw-pt-0 tw-[scrollbar-width:none] tw-[-ms-overflow-style:none] [&::-webkit-scrollbar]:tw-hidden">
          <section className="tw-flex tw-items-center tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4">
            <span className="tw-inline-flex tw-h-12 tw-w-12 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-brand-50 tw-text-[1.1rem] tw-text-vm-primary">
              <i className="far fa-calendar-alt" />
            </span>
            <div className="tw-min-w-0 tw-flex-1">
              <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2">
                <strong className="tw-text-[1rem] tw-font-extrabold tw-text-vm-slate-900">{shift.code}</strong>
                <Badge tone={statusTone(shift.status)} className="tw-rounded-full tw-px-3">{statusLabels[shift.status]}</Badge>
              </div>
              <p className="tw-m-0 tw-mt-1 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">{formatFullDate(shift.shiftDate)} · {shiftTypeLabels[shift.type]}</p>
            </div>
          </section>

          <section className="tw-mt-5">
            <h3 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Thông tin ca</h3>
            <dl className="tw-m-0 tw-mt-4 tw-grid tw-grid-cols-2 tw-gap-x-10 tw-gap-y-4">
              {[
                ["Bãi xe", shift.lotName],
                ["Trạng thái", statusLabels[shift.status]],
                ["Loại ca", shiftTypeLabels[shift.type]],
                ["Thời gian", `${shift.startTime} - ${shift.endTime}`],
                ["Tiền đầu ca", shift.openingCash],
                ["Tiền cuối ca", shift.closingCash],
              ].map(([label, value]) => (
                <div key={label}>
                  <dt className="tw-text-[0.76rem] tw-font-extrabold tw-text-vm-slate-500">{label}</dt>
                  <dd className="tw-m-0 tw-mt-1 tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-900">{value}</dd>
                </div>
              ))}
            </dl>
          </section>

          <section className="tw-mt-6">
            <h3 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Nhân sự phân công ({shift.assigned}/{shift.capacity})</h3>
            <div className="tw-mt-4 tw-grid tw-gap-3">
              {shift.assignments.length ? shift.assignments.map((employee) => (
                <div className="tw-flex tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3" key={employee.assignmentId}>
                  <EntityAvatar initials={employee.initials} size="md" tone="blue" />
                  <div className="tw-min-w-0 tw-flex-1">
                    <strong className="tw-block tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-900">{employee.employeeName}</strong>
                    <small className="tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">{employee.role} · {employee.gateName}</small>
                  </div>
                  <Badge tone={employee.status === "REMOVED" ? "danger" : "success"} className="tw-rounded-full tw-px-3">{employee.status}</Badge>
                  <Button size="sm" variant="danger" disabled={savingAction} onClick={() => void handleDeleteAssignment(employee.assignmentId)}>
                    Xoa
                  </Button>
                </div>
              )) : (
                <div className="tw-rounded-vm-md tw-border tw-border-dashed tw-border-vm-slate-200 tw-bg-vm-slate-25 tw-p-4 tw-text-center tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-500">
                  Chưa có nhân sự phân công cho ca này.
                </div>
              )}
            </div>
          </section>

          <section className="tw-mt-6">
            <h3 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Thao tác nhanh</h3>
            <form className="tw-mt-4 tw-grid tw-gap-3" onSubmit={(event) => void handleAssignmentSubmit(event)}>
              <div className="tw-grid tw-grid-cols-3 tw-gap-2">
                {[
                  ["add", "Thêm"],
                  ["replace", "Thay"],
                  ["swap", "Đổi ca"],
                ].map(([value, label]) => (
                  <button
                    className={cn(
                      "tw-h-9 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-700",
                      assignmentAction === value ? "tw-border-vm-primary tw-bg-brand-50 tw-text-vm-primary" : "",
                    )}
                    key={value}
                    type="button"
                    onClick={() => setAssignmentAction(value as "add" | "replace" | "swap")}
                  >
                    {label}
                  </button>
                ))}
              </div>

              {assignmentAction !== "add" ? (
                <SelectMenu
                  ariaLabel="Phân công hiện tại"
                  disabled={!currentAssignmentOptions.length}
                  options={currentAssignmentOptions.length ? currentAssignmentOptions : [{ label: "Chưa có phân công", value: "" }]}
                  value={selectedAssignmentId}
                  onChange={setSelectedAssignmentId}
                />
              ) : null}

              {assignmentAction === "swap" ? (
                <SelectMenu
                  ariaLabel="Phân công đổi ca"
                  disabled={!allAssignmentOptions.length}
                  options={allAssignmentOptions}
                  value={secondAssignmentId}
                  onChange={setSecondAssignmentId}
                />
              ) : (
                <SelectMenu
                  ariaLabel="Nhân viên"
                  disabled={!employeeOptions.length}
                  options={employeeOptions.length ? employeeOptions : [{ label: "Chưa có nhân viên", value: "" }]}
                  value={selectedEmployeeId}
                  onChange={setSelectedEmployeeId}
                />
              )}

              {assignmentAction === "add" ? (
                <SelectMenu
                  ariaLabel="Cổng"
                  disabled={!gateOptions.length}
                  options={gateOptions.length ? gateOptions : [{ label: "Chưa có cổng", value: "" }]}
                  value={selectedGateId}
                  onChange={setSelectedGateId}
                />
              ) : null}

              {assignmentAction !== "add" ? (
                <textarea
                  className="tw-min-h-[76px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)]"
                  placeholder="Lý do thay đổi"
                  value={actionReason}
                  onChange={(event) => setActionReason(event.target.value)}
                />
              ) : null}

              {actionError ? (
                <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-p-3 tw-text-[0.78rem] tw-font-bold tw-text-red-600">{actionError}</div>
              ) : null}

              <Button disabled={shift.isPlaceholder} loading={savingAction} type="submit">
                {savingAction ? "Đang lưu..." : assignmentAction === "add" ? "Thêm phân công" : assignmentAction === "replace" ? "Thay nhân viên" : "Đổi ca trực"}
              </Button>
            </form>
            <div className="tw-mt-4 tw-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-orange-100 tw-bg-orange-50 tw-p-3 tw-text-[0.8rem] tw-font-bold tw-text-orange-700">
              Mở ca, đóng ca, đổi nhân viên và swap ca cần form nhập liệu riêng theo payload backend. Màn này hiện đã gắn dữ liệu đọc, sinh lịch tuần và duyệt lịch tuần.
            </div>
          </section>
        </div>

        <footer className="tw-grid tw-grid-cols-1 tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-6 tw-py-4">
          <Button variant="secondary" onClick={onClose}>Đóng</Button>
        </footer>
      </aside>
    </div>
  );
}

export function ShiftSchedulePage() {
  const toast = useToast();
  const [parkingLots, setParkingLots] = useState<ParkingLotApiResponse[]>([]);
  const [gates, setGates] = useState<GateApiResponse[]>([]);
  const [employees, setEmployees] = useState<EmployeeApiResponse[]>([]);
  const [shifts, setShifts] = useState<ShiftApiResponse[]>([]);
  const [assignments, setAssignments] = useState<ShiftAssignmentApiResponse[]>([]);
  const [shiftTemplates, setShiftTemplates] = useState<ShiftTemplateApiResponse[]>([]);
  const [rosterRules, setRosterRules] = useState<EmployeeRosterRuleApiResponse[]>([]);
  const [workspace, setWorkspace] = useState<ShiftWorkspace>("schedule");
  const [selectedLot, setSelectedLot] = useState("all");
  const [selectedShiftType, setSelectedShiftType] = useState("all");
  const [selectedStatus, setSelectedStatus] = useState("all");
  const [selectedShiftId, setSelectedShiftId] = useState<string | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [weekStartDate, setWeekStartDate] = useState(() => toIsoDate(startOfWeek(new Date())));
  const [searchValue, setSearchValue] = useState("");
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");

  const weekEndDate = useMemo(() => toIsoDate(addDays(parseIsoDate(weekStartDate), 6)), [weekStartDate]);
  const days = useMemo(() => Array.from({ length: 7 }, (_, index) => {
    const date = toIsoDate(addDays(parseIsoDate(weekStartDate), index));
    return {
      date,
      label: index === 6 ? "CN" : `Thứ ${index + 2}`,
    };
  }), [weekStartDate]);

  const lotOptions = useMemo(() => [
    { label: "Tất cả bãi xe", value: "all" },
    ...parkingLots.map((lot) => ({ label: `${lot.code} - ${lot.name}`, value: lot.parkingLotId })),
  ], [parkingLots]);

  useEffect(() => {
    if (selectedLot === "all" && parkingLots.length === 1) {
      setSelectedLot(parkingLots[0].parkingLotId);
    }
  }, [parkingLots, selectedLot]);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const [shiftResponse, assignmentResponse, employeeResponse, lotResponse, gateResponse, templateResponse, rosterRuleResponse] = await Promise.all([
        getShifts({
          fromDate: weekStartDate,
          keyword: searchValue.trim() || undefined,
          parkingLotId: selectedLot === "all" ? undefined : selectedLot,
          shiftType: selectedShiftType === "all" ? undefined : (selectedShiftType as ShiftTypeApi),
          status: selectedStatus === "all" ? undefined : (selectedStatus as ShiftStatusApi),
          toDate: weekEndDate,
        }),
        getShiftAssignments({
          fromDate: weekStartDate,
          parkingLotId: selectedLot === "all" ? undefined : selectedLot,
          shiftType: selectedShiftType === "all" ? undefined : (selectedShiftType as ShiftTypeApi),
          toDate: weekEndDate,
        }),
        getEmployees(),
        getParkingLots(),
        getGates(),
        getShiftTemplates({
          parkingLotId: selectedLot === "all" ? undefined : selectedLot,
        }),
        getEmployeeRosterRules({
          parkingLotId: selectedLot === "all" ? undefined : selectedLot,
        }),
      ]);

      setShifts(shiftResponse.data ?? []);
      setAssignments(assignmentResponse.data ?? []);
      setEmployees(employeeResponse.data ?? []);
      setParkingLots(lotResponse.data ?? []);
      setGates(gateResponse.data ?? []);
      setShiftTemplates(templateResponse.data ?? []);
      setRosterRules(rosterRuleResponse.data ?? []);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Không thể tải dữ liệu ca trực.");
    } finally {
      setLoading(false);
    }
  }, [searchValue, selectedLot, selectedShiftType, selectedStatus, weekEndDate, weekStartDate]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const shiftCells = useMemo(() => buildShiftCells({
    assignments,
    employees,
    gates,
    parkingLots,
    shifts,
    weekStartDate,
  }), [assignments, employees, gates, parkingLots, shifts, weekStartDate]);

  const selectedShift = useMemo(() => shiftCells.find((shift) => shift.id === selectedShiftId) ?? null, [selectedShiftId, shiftCells]);
  const realShifts = shiftCells.filter((shift) => !shift.isPlaceholder);
  const assignedCount = realShifts.reduce((total, shift) => total + shift.assigned, 0);
  const needsAttention = realShifts.filter((shift) => shift.assigned < shift.capacity || shift.status === "DRAFT").length;

  function handleSelectShift(shift: ShiftCell) {
    setSelectedShiftId(shift.id);
    setDrawerOpen(true);
  }

  async function handleGenerateWeek() {
    if (selectedLot === "all") {
      setError("Vui lòng chọn một bãi xe cụ thể trước khi sinh lịch tuần.");
      return;
    }

    setActionLoading(true);
    setError("");
    try {
      await generateWorkScheduleWeek(selectedLot, weekStartDate);
      toast.success("Đã sinh lịch tuần.");
      await loadData();
    } catch (generateError) {
      setError(generateError instanceof Error ? generateError.message : "Không thể sinh lịch tuần.");
    } finally {
      setActionLoading(false);
    }
  }

  async function handleApproveWeek() {
    if (selectedLot === "all") {
      setError("Vui lòng chọn một bãi xe cụ thể trước khi duyệt lịch tuần.");
      return;
    }

    setActionLoading(true);
    setError("");
    try {
      await approveWorkScheduleWeek(selectedLot, weekStartDate);
      toast.success("Đã duyệt lịch tuần.");
      await loadData();
    } catch (approveError) {
      setError(approveError instanceof Error ? approveError.message : "Không thể duyệt lịch tuần.");
    } finally {
      setActionLoading(false);
    }
  }

  return (
    <>
      <div className="tw-px-4 tw-py-4 lg:tw-px-5">
        <section className="tw-mx-auto tw-min-h-[calc(100vh-104px)] tw-w-[min(100%,1560px)] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-vm-card">
          <div className="tw-mb-5 tw-flex tw-items-center tw-justify-between tw-gap-4 max-[1024px]:tw-flex-col max-[1024px]:tw-items-stretch">
            <div className="tw-flex tw-min-w-0 tw-items-center tw-gap-4">
              <h1 className="tw-m-0 tw-text-vm-page-title tw-tracking-normal tw-text-vm-slate-900">Ca trực & Phân công</h1>
            </div>
            <div className="tw-flex tw-flex-shrink-0 tw-items-center tw-gap-3 max-[720px]:tw-grid max-[720px]:tw-grid-cols-1">
              <Button size="lg" variant="primary" disabled={actionLoading} onClick={() => void handleGenerateWeek()}>
                <i className="fas fa-plus" />
                Sinh lịch tuần
              </Button>
              <Button size="lg" variant="secondary" disabled={actionLoading} onClick={() => void handleApproveWeek()}>
                <i className="far fa-check-circle" />
                Duyệt lịch
              </Button>
              <Button size="lg" variant="secondary" disabled={loading} onClick={() => void loadData()}>
                <i className="fas fa-sync-alt" />
                Làm mới
              </Button>
            </div>
          </div>

          <div className="tw-mb-4 tw-flex tw-flex-wrap tw-gap-2" role="tablist" aria-label="Quản lý ca trực">
            {[
              ["schedule", "Lịch tuần"],
              ["templates", "Mẫu ca"],
              ["rules", "Quy tắc phân công"],
            ].map(([value, label]) => (
              <button
                aria-pressed={workspace === value}
                className={cn(
                  "tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-text-[0.88rem] tw-font-extrabold tw-text-vm-slate-600 tw-transition hover:tw-border-brand-100 hover:tw-text-vm-primary",
                  workspace === value ? "tw-border-vm-primary tw-bg-brand-50 tw-text-vm-primary" : "",
                )}
                key={value}
                type="button"
                onClick={() => setWorkspace(value as ShiftWorkspace)}
              >
                {label}
              </button>
            ))}
          </div>

          {workspace === "schedule" ? (
            <>
          <div className="tw-grid tw-grid-cols-4 tw-gap-4 max-[1180px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
            <ShiftMetricCard icon="far fa-calendar-alt" label="Ca tuần này" tone="blue" value={`${realShifts.length}`} />
            <ShiftMetricCard icon="fas fa-users" label="Đã phân công" tone="green" value={`${assignedCount}`} />
            <ShiftMetricCard icon="far fa-clock" label="Đang mở" tone="orange" value={`${realShifts.filter((shift) => shift.status === "OPEN").length}`} />
            <ShiftMetricCard icon="fas fa-exclamation-triangle" label="Cần xử lý" tone="red" value={`${needsAttention}`} />
          </div>

          <WeekSummaryPanel shifts={shiftCells} />

          <Card className="tw-mt-4 tw-overflow-visible tw-p-3">
            <div
              className="tw-grid tw-items-start tw-gap-3 max-[720px]:tw-grid-cols-1"
              style={{ gridTemplateColumns: "minmax(220px, 270px) 44px minmax(190px, 1fr) 44px minmax(140px, 150px) minmax(150px, 160px) minmax(180px, 1fr)" }}
            >
              <SelectMenu className="tw-self-start" ariaLabel="Bãi xe" options={lotOptions} value={selectedLot} clearValue="all" onChange={setSelectedLot} />
              <Button className="tw-h-[42px] tw-px-0" variant="secondary" aria-label="Tuần trước" onClick={() => setWeekStartDate(toIsoDate(addWeeks(parseIsoDate(weekStartDate), -1)))}>
                <i className="fas fa-chevron-left" />
              </Button>
              <Button className="tw-h-[42px] tw-justify-between" variant="secondary">
                <span>Tuần {formatDate(weekStartDate)} - {formatDate(weekEndDate)}</span>
                <i className="far fa-calendar-alt" />
              </Button>
              <Button className="tw-h-[42px] tw-px-0" variant="secondary" aria-label="Tuần sau" onClick={() => setWeekStartDate(toIsoDate(addWeeks(parseIsoDate(weekStartDate), 1)))}>
                <i className="fas fa-chevron-right" />
              </Button>
              <SelectMenu className="tw-self-start" ariaLabel="Loại ca" options={shiftTypeOptions} value={selectedShiftType} onChange={setSelectedShiftType} />
              <SelectMenu className="tw-self-start" ariaLabel="Trạng thái" options={statusOptions} value={selectedStatus} onChange={setSelectedStatus} />
              <label className="tw-m-0 tw-box-border tw-flex tw-h-[42px] tw-self-start tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3.5 tw-shadow-[0_4px_10px_rgba(15,23,42,0.025)] focus-within:tw-border-brand-200 focus-within:tw-shadow-[0_0_0_4px_rgba(37,99,235,0.08)]">
                <i className="fas fa-search tw-flex-shrink-0 tw-text-[0.92rem] tw-leading-none tw-text-vm-slate-500" />
                <input className="tw-m-0 tw-h-full tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.88rem] tw-font-semibold tw-leading-[42px] tw-text-vm-slate-900 tw-outline-none placeholder:tw-text-vm-slate-500" placeholder="Tìm nhân viên, cổng, ca..." value={searchValue} onChange={(event) => setSearchValue(event.target.value)} />
              </label>
            </div>
          </Card>

          {error ? (
            <div className="tw-mt-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-p-3 tw-text-[0.84rem] tw-font-bold tw-text-red-600">
              {error}
            </div>
          ) : null}

          <div className="tw-mt-4">
            {loading ? (
              <Card className="tw-p-8 tw-text-center tw-text-[0.9rem] tw-font-bold tw-text-vm-slate-500">Đang tải dữ liệu ca trực...</Card>
            ) : (
              <WeeklyScheduleBoard days={days} selectedShiftId={selectedShiftId} shifts={shiftCells} onSelectShift={handleSelectShift} />
            )}
          </div>
            </>
          ) : (
            <ShiftConfigurationPanel
              employees={employees}
              gates={gates}
              mode={workspace}
              onChanged={loadData}
              parkingLots={parkingLots}
              rosterRules={rosterRules}
              selectedLot={selectedLot}
              shiftTemplates={shiftTemplates}
            />
          )}
        </section>
      </div>

      <ShiftDetailDrawer
        allShifts={shiftCells}
        employees={employees}
        gates={gates}
        isOpen={drawerOpen}
        onChanged={loadData}
        onClose={() => setDrawerOpen(false)}
        shift={selectedShift}
      />
    </>
  );
}
