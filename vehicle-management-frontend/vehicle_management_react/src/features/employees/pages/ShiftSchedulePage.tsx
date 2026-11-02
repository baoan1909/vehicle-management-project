import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";

import { Badge, Button, Card, EntityAvatar, Modal, SelectMenu, useToast } from "@/components/ui";
import { ShiftConfigurationPanel } from "@/features/employees/components/ShiftConfigurationPanel";
import { getEmployees, type EmployeeApiResponse } from "@/features/employees/api/employeesApi";
import {
  assignEmployeeToParkingLot,
  approveWorkScheduleWeek,
  createShiftAssignment,
  deleteShiftAssignment,
  generateWorkScheduleWeek,
  getEmployeeRosterRules,
  getEmployeeParkingLotAssignments,
  getGates,
  getParkingLots,
  getMyShiftAssignments,
  getShiftAssignments,
  getShiftTemplates,
  getShifts,
  replaceShiftAssignment,
  swapShiftAssignments,
  transferEmployeeToParkingLot,
  type EmployeeParkingLotAssignmentApiResponse,
  type EmployeeRosterRuleApiResponse,
  type EmployeeParkingLotAssignmentTypeApi,
  type GateApiResponse,
  type ParkingLotApiResponse,
  type ShiftApiResponse,
  type ShiftAssignmentApiResponse,
  type ShiftStatusApi,
  type ShiftTemplateApiResponse,
  type ShiftTypeApi,
} from "@/features/employees/api/shiftsApi";
import { cn } from "@/lib/cn";
import { todayApplicationIsoDate } from "@/shared/time/applicationTime";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";

type DrawerPhase = "opening" | "open" | "closing";
type ShiftWorkspace = "schedule" | "parkingAssignments" | "templates" | "rules";

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

function formatDateTime(value?: string | null) {
  if (!value) return "Không giới hạn";
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(value));
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

const employeeParkingLotAssignmentTypeOptions: Array<{
  label: string;
  value: EmployeeParkingLotAssignmentTypeApi;
}> = [
  { label: "Bãi làm việc chính", value: "PRIMARY" },
  { label: "Hỗ trợ bãi", value: "SUPPORT" },
  { label: "Phân công tạm thời", value: "TEMPORARY" },
];

function EmployeeParkingLotAssignmentModal({
  employees,
  initialParkingLotId,
  onAssigned,
  onClose,
  open,
  parkingLots,
}: {
  employees: EmployeeApiResponse[];
  initialParkingLotId: string;
  onAssigned: () => Promise<void> | void;
  onClose: () => void;
  open: boolean;
  parkingLots: ParkingLotApiResponse[];
}) {
  const toast = useToast();
  const eligibleEmployees = useMemo(
    () => employees.filter((employee) => employee.roleCode === "EMPLOYEE"
      && employee.status === "ACTIVE"
      && employee.accountStatus === "ACTIVE"),
    [employees],
  );
  const activeParkingLots = useMemo(
    () => parkingLots.filter((parkingLot) => parkingLot.status === "ACTIVE"),
    [parkingLots],
  );
  const employeeOptions = useMemo(
    () => eligibleEmployees.map((employee) => ({
      label: `${getEmployeeName(employee)}${employee.employeeCode ? ` · ${employee.employeeCode}` : ""}`,
      value: employee.employeeId,
    })),
    [eligibleEmployees],
  );
  const parkingLotOptions = useMemo(
    () => activeParkingLots.map((parkingLot) => ({
      label: `${parkingLot.code} - ${parkingLot.name}`,
      value: parkingLot.parkingLotId,
    })),
    [activeParkingLots],
  );
  const [employeeId, setEmployeeId] = useState("");
  const [parkingLotId, setParkingLotId] = useState("");
  const [assignmentType, setAssignmentType] = useState<EmployeeParkingLotAssignmentTypeApi>("PRIMARY");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open) return;
    const preferredParkingLotId = initialParkingLotId !== "all"
      && activeParkingLots.some((parkingLot) => parkingLot.parkingLotId === initialParkingLotId)
      ? initialParkingLotId
      : activeParkingLots[0]?.parkingLotId ?? "";
    setEmployeeId(eligibleEmployees[0]?.employeeId ?? "");
    setParkingLotId(preferredParkingLotId);
    setAssignmentType("PRIMARY");
    setError("");
  }, [activeParkingLots, eligibleEmployees, initialParkingLotId, open]);

  async function handleAssign() {
    const parkingLot = activeParkingLots.find((item) => item.parkingLotId === parkingLotId);
    if (!employeeId || !parkingLot) {
      setError("Vui lòng chọn nhân viên và bãi xe đang hoạt động.");
      return;
    }

    setSaving(true);
    setError("");
    try {
      const response = await assignEmployeeToParkingLot(
        parkingLot.organizationId,
        parkingLot.parkingLotId,
        {
          assignmentType,
          effectiveFrom: null,
          effectiveTo: null,
          employeeId,
        },
      );
      toast.success(
        response.data.status === "ACTIVE"
          ? "Nhân viên đã được phân công và quyền truy cập đang được đồng bộ."
          : "Đã ghi nhận lịch phân công nhân viên.",
        "Phân công thành công",
      );
      await onAssigned();
      onClose();
    } catch (assignmentError) {
      setError(assignmentError instanceof Error
        ? assignmentError.message
        : "Không thể phân công nhân viên vào bãi xe.");
    } finally {
      setSaving(false);
    }
  }

  const noEligibleEmployees = employeeOptions.length === 0;
  const noActiveParkingLots = parkingLotOptions.length === 0;

  return (
    <Modal
      actions={(
        <div className="tw-flex tw-justify-end tw-gap-2">
          <Button variant="secondary" disabled={saving} onClick={onClose}>Hủy</Button>
          <Button
            disabled={saving || noEligibleEmployees || noActiveParkingLots}
            loading={saving}
            onClick={() => void handleAssign()}
          >
            {saving ? "Đang phân công..." : "Phân công vào bãi"}
          </Button>
        </div>
      )}
      description="Chọn Employee đã được duyệt và bãi xe thuộc phạm vi đối tác. Phân công có hiệu lực ngay."
      onClose={() => { if (!saving) onClose(); }}
      open={open}
      title="Phân công nhân viên vào bãi"
    >
      <div className="tw-grid tw-gap-4">
        <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50 tw-p-3 tw-text-[0.8rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-700">
          <i className="fas fa-shield-alt tw-mr-2 tw-text-vm-primary" />
          Chỉ hiển thị Employee ACTIVE cùng đối tác và các bãi ACTIVE mà tài khoản hiện tại được phép quản lý.
        </div>

        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-700">Nhân viên</span>
          <SelectMenu
            ariaLabel="Nhân viên cần phân công"
            disabled={saving || noEligibleEmployees}
            options={employeeOptions.length ? employeeOptions : [{ label: "Không có Employee ACTIVE phù hợp", value: "" }]}
            value={employeeId}
            onChange={setEmployeeId}
          />
        </label>

        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-700">Bãi xe</span>
          <SelectMenu
            ariaLabel="Bãi xe phân công"
            disabled={saving || noActiveParkingLots}
            options={parkingLotOptions.length ? parkingLotOptions : [{ label: "Không có bãi xe ACTIVE trong phạm vi", value: "" }]}
            value={parkingLotId}
            onChange={setParkingLotId}
          />
        </label>

        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-700">Loại phân công</span>
          <SelectMenu
            ariaLabel="Loại phân công bãi xe"
            disabled={saving}
            options={employeeParkingLotAssignmentTypeOptions}
            value={assignmentType}
            onChange={(value) => setAssignmentType(value as EmployeeParkingLotAssignmentTypeApi)}
          />
        </label>

        {error ? (
          <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-p-3 tw-text-[0.8rem] tw-font-bold tw-text-red-600">
            {error}
          </div>
        ) : null}
      </div>
    </Modal>
  );
}

const employeeParkingLotAssignmentStatusLabels: Record<string, string> = {
  ACTIVE: "Đang công tác",
  CANCELLED: "Đã hủy",
  ENDED: "Đã kết thúc",
  PENDING_REVIEW: "Chờ xem xét",
  SCHEDULED: "Sắp hiệu lực",
  SUSPENDED: "Tạm dừng",
};

const employeeParkingLotAssignmentTypeLabels: Record<EmployeeParkingLotAssignmentTypeApi, string> = {
  PRIMARY: "Bãi chính",
  SUPPORT: "Hỗ trợ",
  TEMPORARY: "Tạm thời",
};

function employeeParkingLotAssignmentStatusTone(status: string) {
  if (status === "ACTIVE") return "success" as const;
  if (status === "SCHEDULED" || status === "PENDING_REVIEW") return "warning" as const;
  if (status === "SUSPENDED" || status === "CANCELLED") return "danger" as const;
  return "neutral" as const;
}

function effectiveEmployeeParkingLotAssignmentStatus(
  assignment: EmployeeParkingLotAssignmentApiResponse,
): EmployeeParkingLotAssignmentApiResponse["status"] {
  if (!["ACTIVE", "SCHEDULED"].includes(assignment.status)) return assignment.status;
  const now = Date.now();
  if (new Date(assignment.effectiveFrom).getTime() > now) return "SCHEDULED";
  if (assignment.effectiveTo && new Date(assignment.effectiveTo).getTime() <= now) return "ENDED";
  return "ACTIVE";
}

function TransferEmployeeParkingLotModal({
  assignment,
  employees,
  onClose,
  onTransferred,
  parkingLots,
}: {
  assignment: EmployeeParkingLotAssignmentApiResponse | null;
  employees: EmployeeApiResponse[];
  onClose: () => void;
  onTransferred: () => Promise<void> | void;
  parkingLots: ParkingLotApiResponse[];
}) {
  const toast = useToast();
  const [targetParkingLotId, setTargetParkingLotId] = useState("");
  const [reason, setReason] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const employee = employees.find((item) => item.employeeId === assignment?.employeeId);
  const sourceParkingLot = parkingLots.find((item) => item.parkingLotId === assignment?.parkingLotId);
  const targetParkingLots = useMemo(
    () => parkingLots.filter((item) => item.status === "ACTIVE"
      && item.organizationId === assignment?.organizationId
      && item.parkingLotId !== assignment?.parkingLotId),
    [assignment?.organizationId, assignment?.parkingLotId, parkingLots],
  );

  useEffect(() => {
    if (!assignment) return;
    setTargetParkingLotId(targetParkingLots[0]?.parkingLotId ?? "");
    setReason("");
    setError("");
  }, [assignment, targetParkingLots]);

  async function handleTransfer() {
    if (!assignment || !targetParkingLotId || !reason.trim()) {
      setError("Vui lòng chọn bãi nhận công tác và nhập lý do điều chuyển.");
      return;
    }

    setSaving(true);
    setError("");
    try {
      await transferEmployeeToParkingLot(assignment.organizationId, {
        effectiveAt: null,
        employeeId: assignment.employeeId,
        reason: reason.trim(),
        sourceParkingLotId: assignment.parkingLotId,
        targetParkingLotId,
      });
      toast.success(
        "Nhân viên đã được chuyển sang bãi mới và quyền truy cập đang được đồng bộ.",
        "Chuyển công tác thành công",
      );
      await onTransferred();
      onClose();
    } catch (transferError) {
      setError(transferError instanceof Error ? transferError.message : "Không thể chuyển nhân viên sang bãi khác.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      actions={(
        <div className="tw-flex tw-justify-end tw-gap-2">
          <Button variant="secondary" disabled={saving} onClick={onClose}>Hủy</Button>
          <Button disabled={saving || targetParkingLots.length === 0} loading={saving} onClick={() => void handleTransfer()}>
            {saving ? "Đang chuyển..." : "Xác nhận chuyển công tác"}
          </Button>
        </div>
      )}
      description={`${getEmployeeName(employee)} · Từ ${sourceParkingLot?.name ?? "bãi hiện tại"} sang bãi mới trong cùng đối tác.`}
      onClose={() => { if (!saving) onClose(); }}
      open={Boolean(assignment)}
      title="Chuyển công tác sang bãi khác"
    >
      <div className="tw-grid tw-gap-4">
        <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-orange-100 tw-bg-orange-50 tw-p-3 tw-text-[0.8rem] tw-font-semibold tw-leading-5 tw-text-orange-800">
          Phân công tại bãi cũ sẽ kết thúc vào thời điểm hiệu lực. Một phân công chính tại bãi mới sẽ được tạo và quyền truy cập của Employee được đồng bộ lại.
        </div>

        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-700">Bãi nhận công tác</span>
          <SelectMenu
            ariaLabel="Bãi nhận công tác"
            disabled={saving || targetParkingLots.length === 0}
            options={targetParkingLots.length
              ? targetParkingLots.map((lot) => ({ label: `${lot.code} - ${lot.name}`, value: lot.parkingLotId }))
              : [{ label: "Không có bãi ACTIVE khác trong cùng đối tác", value: "" }]}
            value={targetParkingLotId}
            onChange={setTargetParkingLotId}
          />
        </label>

        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-700">Lý do điều chuyển</span>
          <textarea
            className="tw-min-h-24 tw-resize-y tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3.5 tw-text-[0.88rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200"
            disabled={saving}
            maxLength={500}
            placeholder="Ví dụ: Bổ sung nhân sự vận hành cho bãi mới"
            value={reason}
            onChange={(event) => setReason(event.target.value)}
          />
        </label>

        {error ? (
          <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-p-3 tw-text-[0.8rem] tw-font-bold tw-text-red-600">{error}</div>
        ) : null}
      </div>
    </Modal>
  );
}

function EmployeeParkingLotAssignmentsPanel({
  assignments,
  canTransfer,
  employees,
  loading,
  onChanged,
  onSelectedLotChange,
  parkingLots,
  selectedLot,
}: {
  assignments: EmployeeParkingLotAssignmentApiResponse[];
  canTransfer: boolean;
  employees: EmployeeApiResponse[];
  loading: boolean;
  onChanged: () => Promise<void> | void;
  onSelectedLotChange: (parkingLotId: string) => void;
  parkingLots: ParkingLotApiResponse[];
  selectedLot: string;
}) {
  const [statusFilter, setStatusFilter] = useState("CURRENT");
  const [keyword, setKeyword] = useState("");
  const [transferAssignment, setTransferAssignment] = useState<EmployeeParkingLotAssignmentApiResponse | null>(null);
  const employeeMap = useMemo(() => buildMap(employees, "employeeId"), [employees]);
  const parkingLotMap = useMemo(() => buildMap(parkingLots, "parkingLotId"), [parkingLots]);
  const normalizedKeyword = keyword.trim().toLocaleLowerCase("vi");
  const filteredAssignments = assignments.filter((assignment) => {
    const effectiveStatus = effectiveEmployeeParkingLotAssignmentStatus(assignment);
    if (selectedLot !== "all" && assignment.parkingLotId !== selectedLot) return false;
    if (statusFilter === "CURRENT" && !["ACTIVE", "SCHEDULED"].includes(effectiveStatus)) return false;
    if (statusFilter !== "all" && statusFilter !== "CURRENT" && effectiveStatus !== statusFilter) return false;
    if (!normalizedKeyword) return true;
    const employee = employeeMap.get(assignment.employeeId);
    const parkingLot = parkingLotMap.get(assignment.parkingLotId);
    return [getEmployeeName(employee), employee?.employeeCode, employee?.accountEmail, parkingLot?.name, parkingLot?.code]
      .filter(Boolean)
      .some((value) => String(value).toLocaleLowerCase("vi").includes(normalizedKeyword));
  });
  const activeCount = assignments.filter((item) => effectiveEmployeeParkingLotAssignmentStatus(item) === "ACTIVE").length;
  const scheduledCount = assignments.filter((item) => effectiveEmployeeParkingLotAssignmentStatus(item) === "SCHEDULED").length;
  const historyCount = assignments.filter((item) => ["ENDED", "CANCELLED"].includes(effectiveEmployeeParkingLotAssignmentStatus(item))).length;

  return (
    <div className="tw-grid tw-gap-4">
      <div className="tw-grid tw-grid-cols-3 tw-gap-4 max-[820px]:tw-grid-cols-1">
        <ShiftMetricCard icon="fas fa-map-marker-alt" label="Đang công tác" tone="green" value={`${activeCount}`} />
        <ShiftMetricCard icon="far fa-clock" label="Sắp hiệu lực" tone="orange" value={`${scheduledCount}`} />
        <ShiftMetricCard icon="fas fa-history" label="Lịch sử phân công" tone="blue" value={`${historyCount}`} />
      </div>

      <Card className="tw-p-4">
        <div className="tw-grid tw-grid-cols-[minmax(220px,1fr)_240px_220px] tw-gap-3 max-[960px]:tw-grid-cols-1">
          <label className="tw-flex tw-h-[42px] tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3.5 focus-within:tw-border-brand-200">
            <i className="fas fa-search tw-text-vm-slate-500" />
            <input
              className="tw-h-full tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.88rem] tw-font-semibold tw-outline-none"
              placeholder="Tìm nhân viên hoặc bãi xe..."
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </label>
          <SelectMenu
            ariaLabel="Lọc theo bãi xe"
            options={[
              { label: "Tất cả bãi xe", value: "all" },
              ...parkingLots.map((lot) => ({ label: `${lot.code} - ${lot.name}`, value: lot.parkingLotId })),
            ]}
            value={selectedLot}
            onChange={onSelectedLotChange}
          />
          <SelectMenu
            ariaLabel="Lọc trạng thái phân công bãi xe"
            options={[
              { label: "Đang hiệu lực & sắp tới", value: "CURRENT" },
              { label: "Tất cả lịch sử", value: "all" },
              { label: "Đang công tác", value: "ACTIVE" },
              { label: "Sắp hiệu lực", value: "SCHEDULED" },
              { label: "Đã kết thúc", value: "ENDED" },
              { label: "Tạm dừng", value: "SUSPENDED" },
            ]}
            value={statusFilter}
            onChange={setStatusFilter}
          />
        </div>
      </Card>

      <Card className="tw-overflow-hidden tw-p-0">
        <div className="tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-5 tw-py-4">
          <h2 className="tw-m-0 tw-text-[1rem] tw-font-extrabold tw-text-vm-slate-900">Danh sách phân công bãi xe</h2>
          <p className="tw-m-0 tw-mt-1 tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-500">Theo dõi bãi đang công tác, lịch sắp hiệu lực và lịch sử điều chuyển của Employee.</p>
        </div>
        {loading ? (
          <div className="tw-p-8 tw-text-center tw-text-sm tw-font-semibold tw-text-vm-slate-500">Đang tải danh sách phân công...</div>
        ) : filteredAssignments.length === 0 ? (
          <div className="tw-p-8 tw-text-center">
            <i className="fas fa-user-tag tw-mb-3 tw-text-3xl tw-text-vm-slate-300" />
            <p className="tw-m-0 tw-text-sm tw-font-bold tw-text-vm-slate-600">Không có phân công phù hợp với bộ lọc.</p>
          </div>
        ) : (
          <div className="tw-overflow-x-auto">
            <table className="table tw-m-0 tw-min-w-[1080px] [&_td]:tw-border-0 [&_td]:tw-border-t [&_td]:tw-border-solid [&_td]:tw-border-vm-slate-100 [&_td]:tw-px-4 [&_td]:tw-py-3 [&_td]:tw-align-middle [&_thead_th]:tw-border-0 [&_thead_th]:tw-bg-vm-slate-25 [&_thead_th]:tw-px-4 [&_thead_th]:tw-py-3.5 [&_thead_th]:tw-text-left [&_thead_th]:tw-text-[0.78rem] [&_thead_th]:tw-font-black [&_thead_th]:tw-text-vm-slate-700">
              <thead>
                <tr>
                  <th>Nhân viên</th>
                  <th>Bãi công tác</th>
                  <th>Loại phân công</th>
                  <th>Hiệu lực</th>
                  <th>Trạng thái</th>
                  <th>Nguồn</th>
                  <th className="tw-text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {filteredAssignments.map((assignment) => {
                  const employee = employeeMap.get(assignment.employeeId);
                  const parkingLot = parkingLotMap.get(assignment.parkingLotId);
                  const effectiveStatus = effectiveEmployeeParkingLotAssignmentStatus(assignment);
                  return (
                    <tr key={assignment.assignmentId}>
                      <td>
                        <div className="tw-flex tw-items-center tw-gap-3">
                          <EntityAvatar initials={getInitials(getEmployeeName(employee))} size="sm" src={employee?.userProfile?.avatarUrl} />
                          <div>
                            <div className="tw-font-extrabold tw-text-vm-slate-900">{getEmployeeName(employee)}</div>
                            <div className="tw-mt-0.5 tw-text-[0.75rem] tw-font-semibold tw-text-vm-slate-500">{employee?.employeeCode ?? shortCode("EMP", assignment.employeeId)}</div>
                          </div>
                        </div>
                      </td>
                      <td>
                        <div className="tw-font-bold tw-text-vm-slate-900">{parkingLot?.name ?? "Bãi không còn trong danh mục"}</div>
                        <div className="tw-mt-0.5 tw-text-[0.75rem] tw-font-semibold tw-text-vm-slate-500">{parkingLot?.code ?? shortCode("LOT", assignment.parkingLotId)}</div>
                      </td>
                      <td><Badge tone="primary">{employeeParkingLotAssignmentTypeLabels[assignment.assignmentType]}</Badge></td>
                      <td>
                        <div className="tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-700">Từ {formatDateTime(assignment.effectiveFrom)}</div>
                        <div className="tw-mt-1 tw-text-[0.75rem] tw-font-semibold tw-text-vm-slate-500">Đến {formatDateTime(assignment.effectiveTo)}</div>
                      </td>
                      <td><Badge tone={employeeParkingLotAssignmentStatusTone(effectiveStatus)}>{employeeParkingLotAssignmentStatusLabels[effectiveStatus] ?? effectiveStatus}</Badge></td>
                      <td className="tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-600">{assignment.assignmentSource === "PARTNER" ? "Đối tác" : assignment.assignmentSource === "MANAGER" ? "Quản lý bãi" : "Chuyển đổi dữ liệu"}</td>
                      <td className="tw-text-right">
                        {canTransfer && effectiveStatus === "ACTIVE" ? (
                          <Button size="sm" variant="secondary" onClick={() => setTransferAssignment(assignment)}>
                            <i className="fas fa-exchange-alt" />
                            Chuyển công tác
                          </Button>
                        ) : <span className="tw-text-vm-slate-400">—</span>}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <TransferEmployeeParkingLotModal
        assignment={transferAssignment}
        employees={employees}
        onClose={() => setTransferAssignment(null)}
        onTransferred={onChanged}
        parkingLots={parkingLots}
      />
    </div>
  );
}

export function ShiftSchedulePage() {
  const { user } = useAuth();
  const canReadAllAssignments = hasAnyPermission(user, ["SHIFT_ASSIGNMENT_READ_ALL"]);
  const canReadOwnAssignments = hasAnyPermission(user, ["SHIFT_ASSIGNMENT_READ_OWN"]);
  const canReadEmployeeParkingLotAssignments = hasAnyPermission(user, ["EMPLOYEE_READ_ALL"])
    && hasAnyPermission(user, ["PARKING_SCOPE_PARTNER"]);
  const canAssignEmployeeToParkingLot = hasAnyPermission(user, ["EMPLOYEE_UPDATE_ALL"])
    && hasAnyPermission(user, ["PARKING_SCOPE_PARTNER"]);
  const canTransferEmployeeParkingLot = canAssignEmployeeToParkingLot
    && hasAnyPermission(user, ["ORGANIZATION_MEMBERSHIP_MANAGE_ALL"]);
  const ownShiftView = !canReadAllAssignments && canReadOwnAssignments;
  const toast = useToast();
  const [parkingLots, setParkingLots] = useState<ParkingLotApiResponse[]>([]);
  const [gates, setGates] = useState<GateApiResponse[]>([]);
  const [employees, setEmployees] = useState<EmployeeApiResponse[]>([]);
  const [shifts, setShifts] = useState<ShiftApiResponse[]>([]);
  const [assignments, setAssignments] = useState<ShiftAssignmentApiResponse[]>([]);
  const [shiftTemplates, setShiftTemplates] = useState<ShiftTemplateApiResponse[]>([]);
  const [rosterRules, setRosterRules] = useState<EmployeeRosterRuleApiResponse[]>([]);
  const [employeeParkingLotAssignments, setEmployeeParkingLotAssignments] = useState<EmployeeParkingLotAssignmentApiResponse[]>([]);
  const [workspace, setWorkspace] = useState<ShiftWorkspace>("schedule");
  const [selectedLot, setSelectedLot] = useState("all");
  const [selectedShiftType, setSelectedShiftType] = useState("all");
  const [selectedStatus, setSelectedStatus] = useState("all");
  const [selectedShiftId, setSelectedShiftId] = useState<string | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [employeeParkingLotAssignmentOpen, setEmployeeParkingLotAssignmentOpen] = useState(false);
  const [weekStartDate, setWeekStartDate] = useState(() => toIsoDate(startOfWeek(parseIsoDate(todayApplicationIsoDate()))));
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
  const workspaceTabs = useMemo<Array<{ label: string; value: ShiftWorkspace }>>(() => [
    { label: "Lịch tuần", value: "schedule" },
    ...(canReadEmployeeParkingLotAssignments
      ? [{ label: "Phân công bãi xe", value: "parkingAssignments" as const }]
      : []),
    { label: "Mẫu ca", value: "templates" },
    { label: "Quy tắc phân công", value: "rules" },
  ], [canReadEmployeeParkingLotAssignments]);

  useEffect(() => {
    if (selectedLot === "all" && parkingLots.length === 1) {
      setSelectedLot(parkingLots[0].parkingLotId);
    }
  }, [parkingLots, selectedLot]);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      if (ownShiftView) {
        const assignmentResponse = await getMyShiftAssignments({
          fromDate: weekStartDate,
          toDate: weekEndDate,
        });
        setAssignments(assignmentResponse.data ?? []);
        setShifts([]);
        setEmployees([]);
        setParkingLots([]);
        setGates([]);
        setShiftTemplates([]);
        setRosterRules([]);
        setEmployeeParkingLotAssignments([]);
        return;
      }

      const results = await Promise.allSettled([
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
      const [shiftResult, assignmentResult, employeeResult, lotResult, gateResult, templateResult, rosterRuleResult] = results;
      setShifts(shiftResult.status === "fulfilled" ? shiftResult.value.data ?? [] : []);
      setAssignments(assignmentResult.status === "fulfilled" ? assignmentResult.value.data ?? [] : []);
      setEmployees(employeeResult.status === "fulfilled" ? employeeResult.value.data ?? [] : []);
      const nextParkingLots = lotResult.status === "fulfilled" ? lotResult.value.data ?? [] : [];
      setParkingLots(nextParkingLots);
      setGates(gateResult.status === "fulfilled" ? gateResult.value.data ?? [] : []);
      setShiftTemplates(templateResult.status === "fulfilled" ? templateResult.value.data ?? [] : []);
      setRosterRules(rosterRuleResult.status === "fulfilled" ? rosterRuleResult.value.data ?? [] : []);
      if (shiftResult.status === "rejected" || assignmentResult.status === "rejected") {
        throw new Error("Không thể tải dữ liệu lịch ca bắt buộc.");
      }

      if (canReadEmployeeParkingLotAssignments) {
        const organizationIds = [...new Set(nextParkingLots.map((lot) => lot.organizationId).filter(Boolean))];
        const employeeParkingLotResponses = await Promise.all(
          organizationIds.map((organizationId) => getEmployeeParkingLotAssignments(organizationId)),
        );
        setEmployeeParkingLotAssignments(employeeParkingLotResponses.flatMap((response) => response.data ?? []));
      } else {
        setEmployeeParkingLotAssignments([]);
      }
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Không thể tải dữ liệu ca trực.");
    } finally {
      setLoading(false);
    }
  }, [canReadEmployeeParkingLotAssignments, ownShiftView, searchValue, selectedLot, selectedShiftType, selectedStatus, weekEndDate, weekStartDate]);

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

  if (ownShiftView) {
    return (
      <main className="tw-px-4 tw-py-4 lg:tw-px-5">
        <section className="tw-mx-auto tw-w-[min(100%,980px)] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-vm-card">
          <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
            <div>
              <h1 className="tw-m-0 tw-text-vm-page-title tw-text-vm-slate-900">Ca trực của tôi</h1>
              <p className="tw-m-0 tw-mt-1 tw-text-sm tw-text-vm-slate-500">Chỉ hiển thị phân công ca của chính tài khoản hiện tại.</p>
            </div>
            <Button variant="secondary" disabled={loading} onClick={() => void loadData()}>
              <i className="fas fa-sync-alt" /> Làm mới
            </Button>
          </div>
          {error ? <div className="tw-mt-4 tw-rounded-vm-md tw-bg-amber-50 tw-p-3 tw-text-sm tw-font-semibold tw-text-amber-800">{error}</div> : null}
          <div className="tw-mt-5 tw-grid tw-gap-3">
            {!loading && assignments.length === 0 ? <p className="tw-text-sm tw-text-vm-slate-500">Không có ca được phân công trong tuần này.</p> : null}
            {assignments.map((assignment) => (
              <Card key={assignment.shiftAssignmentId} className="tw-flex tw-items-center tw-justify-between tw-gap-4 tw-p-4 tw-shadow-none">
                <div>
                  <strong className="tw-block tw-text-sm tw-text-vm-slate-900">Ca #{assignment.shiftId.slice(0, 8)}</strong>
                  <span className="tw-mt-1 tw-block tw-text-xs tw-text-vm-slate-500">Mã phân công: {assignment.shiftAssignmentId}</span>
                </div>
                <Badge tone={assignment.status === "ACTIVE" ? "success" : "neutral"}>{assignment.status}</Badge>
              </Card>
            ))}
          </div>
        </section>
      </main>
    );
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
              {canAssignEmployeeToParkingLot ? (
                <Button size="lg" variant="primary" disabled={loading} onClick={() => setEmployeeParkingLotAssignmentOpen(true)}>
                  <i className="fas fa-user-plus" />
                  Phân công nhân viên vào bãi
                </Button>
              ) : null}
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

          <nav
            className="tw-mb-4 tw-flex tw-h-[52px] tw-items-end tw-gap-4 tw-overflow-x-auto tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100"
            role="tablist"
            aria-label="Quản lý ca trực"
          >
            {workspaceTabs.map(({ label, value }) => (
              <button
                aria-selected={workspace === value}
                className={cn(
                  "tw-relative tw-inline-flex tw-h-[52px] tw-flex-shrink-0 tw-items-center tw-border-0 tw-bg-transparent tw-px-4 tw-text-[0.9rem] tw-font-bold tw-transition hover:tw-text-vm-primary",
                  workspace === value ? "tw-text-vm-primary" : "tw-text-vm-slate-500",
                )}
                key={value}
                role="tab"
                type="button"
                onClick={() => setWorkspace(value)}
              >
                {label}
                {workspace === value ? <span className="tw-absolute tw-inset-x-0 tw-bottom-[-1px] tw-h-0.5 tw-bg-vm-primary" /> : null}
              </button>
            ))}
          </nav>

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
          ) : workspace === "parkingAssignments" ? (
            <EmployeeParkingLotAssignmentsPanel
              assignments={employeeParkingLotAssignments}
              canTransfer={canTransferEmployeeParkingLot}
              employees={employees}
              loading={loading}
              onChanged={loadData}
              onSelectedLotChange={setSelectedLot}
              parkingLots={parkingLots}
              selectedLot={selectedLot}
            />
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
      <EmployeeParkingLotAssignmentModal
        employees={employees}
        initialParkingLotId={selectedLot}
        onAssigned={loadData}
        onClose={() => setEmployeeParkingLotAssignmentOpen(false)}
        open={employeeParkingLotAssignmentOpen}
        parkingLots={parkingLots}
      />
    </>
  );
}
