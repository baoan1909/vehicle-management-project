import { useEffect, useMemo, useState } from "react";

import { Badge, Button, Card, EntityAvatar, SelectMenu } from "@/components/ui";
import { cn } from "@/lib/cn";

type ShiftType = "MORNING" | "AFTERNOON" | "NIGHT";
type ShiftStatus = "DRAFT" | "SCHEDULED" | "OPEN" | "CLOSED" | "CANCELLED";
type DrawerPhase = "opening" | "open" | "closing";

type ShiftAssignment = {
  employeeName?: string;
  gate: string;
  initials?: string;
  role?: string;
};

type ShiftCell = {
  assigned: number;
  capacity: number;
  code: string;
  dayIndex: number;
  id: string;
  status: ShiftStatus;
  type: ShiftType;
  assignments: ShiftAssignment[];
};

const DRAWER_ANIMATION_MS = 280;

const lotOptions = [
  { label: "Bãi xe: CP-Lot A - Trung tâm", value: "lot-a" },
  { label: "Bãi xe: CP-Lot B - Thủ Đức", value: "lot-b" },
  { label: "Bãi xe: CP-Lot C - Sân bay", value: "lot-c" },
];

const shiftTypeOptions = [
  { label: "Loại ca: Tất cả", value: "all" },
  { label: "Ca sáng", value: "MORNING" },
  { label: "Ca chiều", value: "AFTERNOON" },
  { label: "Ca đêm", value: "NIGHT" },
];

const statusOptions = [
  { label: "Trạng thái: Tất cả", value: "all" },
  { label: "DRAFT", value: "DRAFT" },
  { label: "SCHEDULED", value: "SCHEDULED" },
  { label: "OPEN", value: "OPEN" },
  { label: "CLOSED", value: "CLOSED" },
  { label: "CANCELLED", value: "CANCELLED" },
];

const days = [
  { label: "Thứ 2", date: "30/06" },
  { label: "Thứ 3", date: "01/07" },
  { label: "Thứ 4", date: "02/07" },
  { label: "Thứ 5", date: "03/07" },
  { label: "Thứ 6", date: "04/07" },
  { label: "Thứ 7", date: "05/07" },
  { label: "CN", date: "06/07" },
];

const shiftRows: Array<{ icon: string; label: string; meta: string; total: string; type: ShiftType; tone: "blue" | "orange" | "indigo" }> = [
  { icon: "far fa-sun", label: "Ca sáng", meta: "06:00 - 14:00", total: "7 ca", type: "MORNING", tone: "blue" },
  { icon: "fas fa-sun", label: "Ca chiều", meta: "14:00 - 22:00", total: "7 ca", type: "AFTERNOON", tone: "orange" },
  { icon: "far fa-moon", label: "Ca đêm", meta: "22:00 - 06:00", total: "7 ca", type: "NIGHT", tone: "indigo" },
];

const shifts: ShiftCell[] = [
  { assigned: 6, capacity: 6, code: "SHIFT-20260630-MORNING", dayIndex: 0, id: "shift-mon-morning", status: "SCHEDULED", type: "MORNING", assignments: [{ employeeName: "Trần Thị Bình", gate: "Cổng A1", initials: "BT", role: "Thu ngân" }] },
  { assigned: 6, capacity: 6, code: "SHIFT-20260701-MORNING", dayIndex: 1, id: "shift-tue-morning", status: "SCHEDULED", type: "MORNING", assignments: [{ employeeName: "Nguyễn Văn An", gate: "Cổng A2", initials: "NA", role: "Vận hành" }] },
  { assigned: 0, capacity: 6, code: "SHIFT-20260702-MORNING", dayIndex: 2, id: "shift-wed-morning", status: "OPEN", type: "MORNING", assignments: [{ gate: "Cổng A1" }] },
  { assigned: 5, capacity: 6, code: "SHIFT-20260703-MORNING", dayIndex: 3, id: "shift-thu-morning", status: "SCHEDULED", type: "MORNING", assignments: [{ employeeName: "Lê Minh Cường", gate: "Cổng B1", initials: "LC", role: "Thu ngân" }] },
  { assigned: 0, capacity: 6, code: "SHIFT-20260704-MORNING", dayIndex: 4, id: "shift-fri-morning", status: "DRAFT", type: "MORNING", assignments: [{ gate: "Cổng B2" }] },
  { assigned: 6, capacity: 6, code: "SHIFT-20260705-MORNING", dayIndex: 5, id: "shift-sat-morning", status: "SCHEDULED", type: "MORNING", assignments: [{ employeeName: "Phạm H. Dũng", gate: "Cổng C1", initials: "PD", role: "Vận hành" }] },
  { assigned: 0, capacity: 6, code: "SHIFT-20260706-MORNING", dayIndex: 6, id: "shift-sun-morning", status: "OPEN", type: "MORNING", assignments: [{ gate: "Cổng C2" }] },
  { assigned: 6, capacity: 6, code: "SHIFT-20260630-AFTERNOON", dayIndex: 0, id: "shift-mon-afternoon", status: "SCHEDULED", type: "AFTERNOON", assignments: [{ employeeName: "Lê Minh Cường", gate: "Cổng A1", initials: "LC", role: "Vận hành" }] },
  { assigned: 6, capacity: 6, code: "SHIFT-20260701-AFTERNOON", dayIndex: 1, id: "shift-tue-afternoon", status: "SCHEDULED", type: "AFTERNOON", assignments: [{ employeeName: "Phạm H. Dũng", gate: "Cổng A2", initials: "PD", role: "Thu ngân" }] },
  { assigned: 6, capacity: 6, code: "SHIFT-20260702-AFTERNOON", dayIndex: 2, id: "shift-wed-afternoon", status: "SCHEDULED", type: "AFTERNOON", assignments: [{ employeeName: "Trần Thị Bình", gate: "Cổng B1", initials: "BT", role: "Vận hành" }] },
  { assigned: 2, capacity: 6, code: "SHIFT-20260703-AFTERNOON", dayIndex: 3, id: "shift-thu-afternoon", status: "OPEN", type: "AFTERNOON", assignments: [{ gate: "Cổng B2" }] },
  { assigned: 6, capacity: 6, code: "SHIFT-20260704-AFTERNOON", dayIndex: 4, id: "shift-fri-afternoon", status: "SCHEDULED", type: "AFTERNOON", assignments: [{ employeeName: "Nguyễn Văn An", gate: "Cổng C1", initials: "NA", role: "Thu ngân" }] },
  { assigned: 0, capacity: 6, code: "SHIFT-20260705-AFTERNOON", dayIndex: 5, id: "shift-sat-afternoon", status: "DRAFT", type: "AFTERNOON", assignments: [{ gate: "Cổng C2" }] },
  { assigned: 0, capacity: 6, code: "SHIFT-20260706-AFTERNOON", dayIndex: 6, id: "shift-sun-afternoon", status: "CLOSED", type: "AFTERNOON", assignments: [{ employeeName: "Nguyễn Thị Mai", gate: "Cổng C2", initials: "NT", role: "Nghỉ" }] },
  { assigned: 1, capacity: 4, code: "SHIFT-20260630-NIGHT", dayIndex: 0, id: "shift-mon-night", status: "OPEN", type: "NIGHT", assignments: [{ gate: "Cổng A1" }] },
  { assigned: 4, capacity: 4, code: "SHIFT-20260701-NIGHT", dayIndex: 1, id: "shift-tue-night", status: "SCHEDULED", type: "NIGHT", assignments: [{ employeeName: "Hoàng Thị Em", gate: "Cổng A2", initials: "HE", role: "Vận hành" }] },
  { assigned: 0, capacity: 4, code: "SHIFT-20260702-NIGHT", dayIndex: 2, id: "shift-wed-night", status: "CLOSED", type: "NIGHT", assignments: [{ employeeName: "Đỗ Quang Huy", gate: "Cổng B1", initials: "DQ", role: "Nghỉ" }] },
  { assigned: 4, capacity: 4, code: "SHIFT-20260703-NIGHT", dayIndex: 3, id: "shift-thu-night", status: "SCHEDULED", type: "NIGHT", assignments: [{ employeeName: "Nguyễn Thị Mai", gate: "Cổng B2", initials: "NT", role: "Vận hành" }] },
  { assigned: 0, capacity: 4, code: "SHIFT-20260704-NIGHT", dayIndex: 4, id: "shift-fri-night", status: "OPEN", type: "NIGHT", assignments: [{ gate: "Cổng C1" }] },
  { assigned: 4, capacity: 4, code: "SHIFT-20260705-NIGHT", dayIndex: 5, id: "shift-sat-night", status: "SCHEDULED", type: "NIGHT", assignments: [{ employeeName: "Hoàng Thị Em", gate: "Cổng C2", initials: "HE", role: "Vận hành" }] },
  { assigned: 0, capacity: 4, code: "SHIFT-20260706-NIGHT", dayIndex: 6, id: "shift-sun-night", status: "OPEN", type: "NIGHT", assignments: [{ gate: "Cổng C2" }] },
];

function statusTone(status: ShiftStatus) {
  if (status === "SCHEDULED") return "primary";
  if (status === "OPEN") return "success";
  if (status === "DRAFT") return "warning";
  if (status === "CANCELLED") return "danger";
  return "neutral";
}

function statusClassName(status: ShiftStatus) {
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

function ShiftMetricCard({
  delta,
  icon,
  label,
  tone,
  value,
}: {
  delta: string;
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

  const sparklineClassName = {
    blue: "tw-stroke-vm-primary",
    green: "tw-stroke-emerald-500",
    orange: "tw-stroke-orange-500",
    red: "tw-stroke-red-500",
  }[tone];

  return (
    <Card className="tw-min-h-[116px] tw-p-4">
      <div className="tw-flex tw-items-start tw-gap-4">
        <span className={cn("tw-inline-flex tw-h-14 tw-w-14 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-text-[1.28rem]", toneClassName)}>
          <i className={icon} />
        </span>
        <div className="tw-min-w-0">
          <p className="tw-m-0 tw-text-[0.88rem] tw-font-extrabold tw-text-vm-slate-700">{label}</p>
          <strong className="tw-mt-2 tw-block tw-text-[1.8rem] tw-font-extrabold tw-leading-none tw-text-vm-slate-900">{value}</strong>
        </div>
      </div>
      <div className="tw-mt-4 tw-flex tw-items-end tw-justify-between tw-gap-3">
        <span className={cn("tw-text-[0.78rem] tw-font-extrabold", tone === "red" || tone === "orange" ? "tw-text-red-500" : "tw-text-emerald-600")}>{delta}</span>
        <svg className="tw-h-8 tw-w-[96px]" viewBox="0 0 96 32" aria-hidden="true">
          <polyline className={cn("tw-fill-none tw-stroke-[2.5]", sparklineClassName)} points="2,24 12,18 22,21 32,13 42,17 52,10 62,15 72,7 82,12 94,4" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </div>
    </Card>
  );
}

function ShiftCard({ shift, selected, onSelect }: { shift: ShiftCell; selected: boolean; onSelect: () => void }) {
  const mainAssignment = shift.assignments[0];
  const isMissing = shift.assigned < shift.capacity;
  const missingCount = Math.max(shift.capacity - shift.assigned, 0);
  const assignmentLabel =
    shift.status === "DRAFT"
      ? "Chưa tạo"
      : shift.status === "CLOSED"
        ? "Đã đóng"
        : isMissing
          ? `Còn thiếu ${missingCount}`
          : "Đã phân công";

  return (
    <button
      className={cn(
        "tw-flex tw-min-h-[126px] tw-w-full tw-flex-col tw-rounded-vm-md tw-border tw-border-solid tw-bg-white tw-p-3 tw-text-left tw-transition hover:tw-border-brand-100 hover:tw-bg-brand-50/30",
        selected ? "tw-border-vm-primary tw-shadow-[0_0_0_3px_rgba(37,99,235,0.1)]" : "tw-border-vm-slate-100",
      )}
      type="button"
      onClick={onSelect}
    >
      <Badge tone={statusTone(shift.status)} className="tw-w-fit tw-rounded-full tw-px-2 tw-py-0.5 tw-text-[0.62rem]">
        {shift.status}
      </Badge>
      <div className="tw-mt-3 tw-rounded-vm-md tw-bg-vm-slate-25 tw-px-2.5 tw-py-2">
        <strong className={cn("tw-block tw-text-[0.82rem] tw-font-extrabold", isMissing ? "tw-text-orange-600" : "tw-text-emerald-700")}>
          {assignmentLabel}
        </strong>
        <small className="tw-mt-0.5 tw-block tw-text-[0.68rem] tw-font-semibold tw-text-vm-slate-500">Click để xem danh sách nhân sự</small>
      </div>
      <div className="tw-mt-3 tw-flex tw-items-center tw-gap-2 tw-rounded-vm-sm tw-bg-brand-50 tw-px-2 tw-py-1.5 tw-text-[0.7rem] tw-font-extrabold tw-text-vm-primary">
        <i className="far fa-calendar-check" />
        {mainAssignment.gate}
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
  onSelectShift,
  selectedShiftId,
}: {
  onSelectShift: (shift: ShiftCell) => void;
  selectedShiftId: string;
}) {
  return (
    <Card className="tw-overflow-hidden">
      <div className="tw-overflow-x-auto tw-[scrollbar-width:none] tw-[-ms-overflow-style:none] [&::-webkit-scrollbar]:tw-hidden">
        <div className="tw-min-w-[920px]">
          <div className="tw-grid tw-grid-cols-[96px_repeat(7,minmax(108px,1fr))] tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100">
            <div className="tw-flex tw-min-h-[66px] tw-items-center tw-justify-center tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500">Ca / Thời gian</div>
            {days.map((day) => (
              <div className="tw-flex tw-min-h-[66px] tw-flex-col tw-items-center tw-justify-center tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 last:tw-border-r-0" key={day.label}>
                <strong className="tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-900">{day.label}</strong>
                <span className="tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">{day.date}</span>
              </div>
            ))}
          </div>

          {shiftRows.map((row) => (
            <div className="tw-grid tw-grid-cols-[96px_repeat(7,minmax(108px,1fr))] tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 last:tw-border-b-0" key={row.type}>
              <div className="tw-flex tw-min-h-[184px] tw-flex-col tw-items-center tw-justify-center tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3 tw-text-center">
                <span className={cn("tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-full tw-text-[1.05rem]", shiftRowTone(row.tone))}>
                  <i className={row.icon} />
                </span>
                <strong className="tw-mt-3 tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-900">{row.label}</strong>
                <span className="tw-mt-1 tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500">{row.meta}</span>
                <span className="tw-mt-3 tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-700">{row.total}</span>
              </div>
              {days.map((day, index) => {
                const shift = shifts.find((item) => item.type === row.type && item.dayIndex === index)!;
                return (
                  <div className="tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 tw-p-2.5 last:tw-border-r-0" key={`${row.type}-${day.label}`}>
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
          ["DRAFT", "tw-bg-orange-500"],
          ["SCHEDULED", "tw-bg-vm-primary"],
          ["OPEN", "tw-bg-emerald-600"],
          ["CLOSED", "tw-bg-vm-slate-500"],
          ["CANCELLED", "tw-bg-red-500"],
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

function WeekSummaryPanel() {
  return (
    <aside className="tw-mt-4 tw-grid tw-grid-cols-[0.82fr_1.78fr_0.95fr] tw-gap-4 max-[1024px]:tw-grid-cols-1">
      <Card className="tw-p-4">
        <h2 className="tw-m-0 tw-text-[1.02rem] tw-font-extrabold tw-text-vm-slate-900">Tổng quan tuần</h2>
        <div className="tw-mt-4 tw-flex tw-items-center tw-gap-5">
          <div className="tw-relative tw-flex tw-h-[106px] tw-w-[106px] tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full" style={{ background: "conic-gradient(#2563EB 0 86%, #E2E8F0 86% 100%)" }}>
            <div className="tw-flex tw-h-[72px] tw-w-[72px] tw-flex-col tw-items-center tw-justify-center tw-rounded-full tw-bg-white">
              <strong className="tw-text-[1.28rem] tw-font-extrabold tw-text-vm-slate-900">86%</strong>
              <span className="tw-text-[0.62rem] tw-font-extrabold tw-text-vm-slate-700">đã phân công</span>
            </div>
          </div>
          <div className="tw-grid tw-flex-1 tw-gap-3">
            {[
              ["Ca thiếu người", "4", "tw-bg-red-500"],
              ["Nhân viên nghỉ", "7", "tw-bg-vm-slate-400"],
              ["Đổi ca chờ duyệt", "2", "tw-bg-orange-500"],
              ["Ca đang mở", "3", "tw-bg-emerald-500"],
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
        <h3 className="tw-m-0 tw-text-[1.02rem] tw-font-extrabold tw-text-vm-slate-900">Rule nhân sự</h3>
        <div className="tw-mt-4 tw-grid tw-grid-cols-3 tw-gap-3">
          {[
            ["FIXED", "Ca cố định theo vị trí và khung giờ. Ưu tiên đúng người, đúng ca.", "fas fa-car-side", "tw-bg-brand-50 tw-text-vm-primary"],
            ["RELIEF", "Ca dự phòng tự động thay thế khi có người nghỉ hoặc đổi ca.", "far fa-calendar-check", "tw-bg-emerald-50 tw-text-emerald-600"],
            ["Ngày nghỉ", "Thiết lập ngày nghỉ mặc định theo ca, tuần, nhóm nhân sự.", "fas fa-umbrella-beach", "tw-bg-purple-50 tw-text-purple-600"],
          ].map(([title, desc, icon, tone]) => (
            <button className="tw-flex tw-min-h-[88px] tw-min-w-0 tw-items-start tw-gap-2.5 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-2.5 tw-text-left tw-transition hover:tw-border-brand-100 hover:tw-bg-vm-slate-25" key={title} type="button">
              <span className={cn("tw-inline-flex tw-h-9 tw-w-9 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-text-[0.86rem]", tone)}>
                <i className={icon} />
              </span>
              <span className="tw-min-w-0 tw-flex-1">
                <strong className="tw-block tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-900">{title}</strong>
                <small className="tw-mt-1 tw-block tw-text-[0.66rem] tw-font-semibold tw-leading-4 tw-text-vm-slate-500">{desc}</small>
              </span>
              <i className="fas fa-chevron-right tw-mt-1 tw-flex-shrink-0 tw-text-[0.64rem] tw-text-vm-slate-500" />
            </button>
          ))}
        </div>
      </Card>

      <Card className="tw-p-4">
        <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
          <h3 className="tw-m-0 tw-text-[1.02rem] tw-font-extrabold tw-text-vm-slate-900">Cảnh báo</h3>
          <button className="tw-border-0 tw-bg-transparent tw-text-[0.72rem] tw-font-extrabold tw-text-vm-primary" type="button">Xem tất cả</button>
        </div>
        <div className="tw-mt-4 tw-grid tw-gap-3">
          {[
            ["Ca sáng Thứ 4 còn 6 vị trí chưa được phân công", "tw-text-red-500"],
            ["Ca chiều Thứ 5 còn 4 vị trí chưa được phân công", "tw-text-orange-500"],
            ["7 nhân viên có yêu cầu nghỉ trong tuần này", "tw-text-orange-500"],
            ["2 yêu cầu đổi ca đang chờ duyệt", "tw-text-vm-primary"],
          ].map(([text, color]) => (
            <div className="tw-flex tw-gap-3" key={text}>
              <i className={cn("fas fa-exclamation-triangle tw-mt-0.5", color)} />
              <p className="tw-m-0 tw-text-[0.76rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-700">{text}</p>
            </div>
          ))}
        </div>
      </Card>
    </aside>
  );
}

function ShiftDetailDrawer({ isOpen, onClose, shift }: { isOpen: boolean; onClose: () => void; shift: ShiftCell }) {
  const [isRendered, setIsRendered] = useState(isOpen);
  const [phase, setPhase] = useState<DrawerPhase>(isOpen ? "open" : "closing");

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

  if (!isRendered) return null;

  const mainAssignment = shift.assignments[0];
  const day = days[shift.dayIndex];
  const shiftLabel = shiftRows.find((row) => row.type === shift.type)?.label ?? "Ca chiều";

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[2200] tw-isolate tw-flex tw-justify-end" role="dialog" aria-modal="true" aria-labelledby="shift-detail-drawer-title">
      <button
        type="button"
        aria-label="Đóng drawer chi tiết ca trực"
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
                <Badge tone={statusTone(shift.status)} className="tw-rounded-full tw-px-3">{shift.status}</Badge>
              </div>
              <p className="tw-m-0 tw-mt-1 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">{day.label}, {day.date}/2026 · {shiftLabel}</p>
            </div>
          </section>

          <section className="tw-mt-5">
            <h3 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Thông tin ca</h3>
            <dl className="tw-m-0 tw-mt-4 tw-grid tw-grid-cols-2 tw-gap-x-10 tw-gap-y-4">
              {[
                ["Bãi xe", "CP-Lot A - Trung tâm"],
                ["Trạng thái", shift.status],
                ["Loại ca", shiftLabel],
                ["Cổng", mainAssignment.gate],
                ["Thời gian", shift.type === "MORNING" ? "06:00 - 14:00" : shift.type === "AFTERNOON" ? "14:00 - 22:00" : "22:00 - 06:00"],
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
              {[
                { initials: "BT", name: "Trần Thị Bình", role: "Thu ngân" },
                { initials: "NA", name: "Nguyễn Văn An", role: "Vận hành" },
                { initials: "LC", name: "Lê Minh Cường", role: "Hỗ trợ" },
              ].map((employee) => (
                <div className="tw-flex tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3" key={employee.initials}>
                  <EntityAvatar initials={employee.initials} size="md" tone="blue" />
                  <div className="tw-min-w-0 tw-flex-1">
                    <strong className="tw-block tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-900">{employee.name}</strong>
                    <small className="tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">{employee.role}</small>
                  </div>
                  <Badge tone="success" className="tw-rounded-full tw-px-3">ACTIVE</Badge>
                  <button className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-sm tw-border-0 tw-bg-transparent tw-text-vm-slate-500 hover:tw-bg-vm-slate-25" type="button" aria-label="Tác vụ nhân sự">
                    <i className="fas fa-ellipsis-v" />
                  </button>
                </div>
              ))}
            </div>
          </section>

          <section className="tw-mt-6">
            <h3 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Thao tác nhanh</h3>
            <div className="tw-mt-4 tw-grid tw-grid-cols-4 tw-gap-2">
              {[
                ["Mở ca", "fas fa-sync-alt", "tw-border-emerald-100 tw-bg-emerald-50 tw-text-emerald-700"],
                ["Đổi nhân viên", "fas fa-user-friends", "tw-border-brand-100 tw-bg-brand-50 tw-text-vm-primary"],
                ["Swap ca", "fas fa-exchange-alt", "tw-border-purple-100 tw-bg-purple-50 tw-text-purple-600"],
                ["Hủy ca", "fas fa-ban", "tw-border-red-100 tw-bg-red-50 tw-text-red-600"],
              ].map(([label, icon, className]) => (
                <button className={cn("tw-flex tw-min-h-[42px] tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-text-[0.74rem] tw-font-extrabold", className)} key={label} type="button">
                  <i className={icon} />
                  {label}
                </button>
              ))}
            </div>
          </section>

          <section className="tw-mt-6">
            <h3 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Tiền đầu ca / cuối ca</h3>
            <div className="tw-mt-4 tw-grid tw-grid-cols-2 tw-gap-3">
              <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
                <span className="tw-text-[0.72rem] tw-font-extrabold tw-text-vm-slate-500">Tiền đầu ca</span>
                <strong className="tw-mt-1 tw-block tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-900">2.000.000đ</strong>
              </div>
              <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
                <span className="tw-text-[0.72rem] tw-font-extrabold tw-text-vm-slate-500">Tiền cuối ca</span>
                <strong className="tw-mt-1 tw-block tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-900">-</strong>
              </div>
            </div>
          </section>

          <section className="tw-mt-6">
            <h3 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Lịch sử gần đây</h3>
            <div className="tw-mt-4 tw-grid tw-gap-4">
              {[
                ["06/07/2026 10:15", "Generated", "Ca trực được tạo tự động từ mẫu lịch tuần.", "tw-bg-vm-primary"],
                ["06/07/2026 10:20", "Approved", "Ca trực đã được duyệt.", "tw-bg-emerald-500"],
                ["06/07/2026 10:32", "Assignment updated", "Cập nhật nhân sự phân công.", "tw-bg-orange-500"],
              ].map(([time, title, desc, color]) => (
                <div className="tw-flex tw-gap-3" key={title}>
                  <span className={cn("tw-mt-1.5 tw-h-2.5 tw-w-2.5 tw-rounded-full", color)} />
                  <div>
                    <span className="tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">{time}</span>
                    <strong className="tw-mt-1 tw-block tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-900">{title}</strong>
                    <p className="tw-m-0 tw-mt-1 tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">{desc}</p>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>

        <div className="tw-absolute tw-bottom-[84px] tw-right-5 tw-w-[250px] tw-rounded-vm-lg tw-border tw-border-solid tw-border-brand-100 tw-bg-white tw-p-4 tw-shadow-[0_18px_42px_rgba(15,23,42,0.16)]">
          <strong className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-900">Đổi nhân viên cho: Nguyễn Văn An</strong>
          <label className="tw-mt-3 tw-flex tw-h-9 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-px-3">
            <input className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.78rem] tw-outline-none" placeholder="Tìm nhân viên thay thế..." />
            <i className="fas fa-search tw-text-vm-slate-500" />
          </label>
          <div className="tw-mt-3 tw-flex tw-items-center tw-gap-2">
            <EntityAvatar initials="PD" size="sm" tone="green" />
            <div className="tw-min-w-0 tw-flex-1">
              <strong className="tw-block tw-text-[0.76rem] tw-font-extrabold tw-text-vm-slate-900">Phạm H. Dũng</strong>
              <small className="tw-text-[0.68rem] tw-font-semibold tw-text-vm-slate-500">Vận hành</small>
            </div>
            <Badge tone="success" className="tw-rounded-full">ACTIVE</Badge>
          </div>
          <div className="tw-mt-3 tw-flex tw-justify-end tw-gap-2">
            <Button size="sm" variant="secondary">Không</Button>
            <Button size="sm" variant="primary">Xác nhận</Button>
          </div>
        </div>

        <footer className="tw-grid tw-grid-cols-[1fr_1.4fr_1.4fr] tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-6 tw-py-4">
          <Button variant="secondary" onClick={onClose}>Hủy</Button>
          <Button variant="primary">
            <i className="far fa-save" />
            Lưu thay đổi
          </Button>
          <Button className="tw-border-orange-200 tw-bg-white tw-text-orange-600 hover:tw-bg-orange-50" variant="secondary">
            <i className="fas fa-user-friends" />
            Đổi phân công
          </Button>
        </footer>
      </aside>
    </div>
  );
}

export function ShiftSchedulePage() {
  const [selectedLot, setSelectedLot] = useState("lot-a");
  const [selectedShiftType, setSelectedShiftType] = useState("all");
  const [selectedStatus, setSelectedStatus] = useState("all");
  const [selectedShiftId, setSelectedShiftId] = useState("shift-wed-afternoon");
  const [drawerOpen, setDrawerOpen] = useState(false);

  const selectedShift = useMemo(() => shifts.find((shift) => shift.id === selectedShiftId) ?? shifts[9], [selectedShiftId]);

  function handleSelectShift(shift: ShiftCell) {
    setSelectedShiftId(shift.id);
    setDrawerOpen(true);
  }

  return (
    <>
      <div className="tw-px-4 tw-py-4 lg:tw-px-5">
        <section className="tw-mx-auto tw-min-h-[calc(100vh-104px)] tw-w-[min(100%,1560px)] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-vm-card">
          <div className="tw-mb-5 tw-flex tw-items-center tw-justify-between tw-gap-4">
            <div className="tw-flex tw-min-w-0 tw-items-center tw-gap-4">
              <h1 className="tw-m-0 tw-text-vm-page-title tw-tracking-[-0.03em] tw-text-vm-slate-900">Ca trực & Phân công</h1>
              <a className="tw-inline-flex tw-items-center tw-gap-2 tw-text-[0.86rem] tw-font-extrabold tw-text-vm-primary hover:tw-text-vm-primary-hover hover:tw-no-underline" href="#shift-help">
                <i className="far fa-question-circle tw-text-[1rem]" />
                Hướng dẫn & Trợ giúp
              </a>
            </div>
            <div className="tw-flex tw-flex-shrink-0 tw-items-center tw-gap-3">
              <Button size="lg" variant="primary">
                <i className="fas fa-plus" />
                Sinh lịch tuần
              </Button>
              <Button size="lg" variant="secondary">
                <i className="far fa-check-circle" />
                Duyệt lịch
              </Button>
              <Button size="lg" variant="secondary">
                <i className="fas fa-download" />
                Xuất dữ liệu
                <i className="fas fa-chevron-down tw-text-[0.72rem]" />
              </Button>
            </div>
          </div>

          <div className="tw-grid tw-grid-cols-4 tw-gap-4 max-[1180px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
            <ShiftMetricCard delta="+2 so với tuần trước" icon="far fa-calendar-alt" label="Ca tuần này" tone="blue" value="21" />
            <ShiftMetricCard delta="+8 so với tuần trước" icon="fas fa-users" label="Đã phân công" tone="green" value="58" />
            <ShiftMetricCard delta="-1 so với tuần trước" icon="far fa-clock" label="Đang mở" tone="orange" value="3" />
            <ShiftMetricCard delta="+2 so với tuần trước" icon="far fa-exclamation-circle" label="Cần xử lý" tone="red" value="6" />
          </div>

          <WeekSummaryPanel />

          <Card className="tw-mt-4 tw-p-3">
            <div className="tw-grid tw-grid-cols-[230px_44px_1fr_44px_150px_160px_minmax(180px,1fr)] tw-items-start tw-gap-3 max-[1280px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
              <SelectMenu className="tw-self-start" ariaLabel="Bãi xe" options={lotOptions} value={selectedLot} clearValue="lot-a" onChange={setSelectedLot} />
              <Button className="tw-h-[42px] tw-px-0" variant="secondary" aria-label="Tuần trước">
                <i className="fas fa-chevron-left" />
              </Button>
              <Button className="tw-h-[42px] tw-justify-between" variant="secondary">
                <span>Tuần 30/06 - 06/07</span>
                <i className="far fa-calendar-alt" />
              </Button>
              <Button className="tw-h-[42px] tw-px-0" variant="secondary" aria-label="Tuần sau">
                <i className="fas fa-chevron-right" />
              </Button>
              <SelectMenu className="tw-self-start" ariaLabel="Loại ca" options={shiftTypeOptions} value={selectedShiftType} onChange={setSelectedShiftType} />
              <SelectMenu className="tw-self-start" ariaLabel="Trạng thái" options={statusOptions} value={selectedStatus} onChange={setSelectedStatus} />
              <label className="tw-m-0 tw-box-border tw-flex tw-h-[42px] tw-self-start tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3.5 tw-shadow-[0_4px_10px_rgba(15,23,42,0.025)] focus-within:tw-border-brand-200 focus-within:tw-shadow-[0_0_0_4px_rgba(37,99,235,0.08)]">
                <i className="fas fa-search tw-flex-shrink-0 tw-text-[0.92rem] tw-leading-none tw-text-vm-slate-500" />
                <input className="tw-m-0 tw-h-full tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.88rem] tw-font-semibold tw-leading-[42px] tw-text-vm-slate-900 tw-outline-none placeholder:tw-text-vm-slate-500" placeholder="Tìm nhân viên, cổng, ca..." />
              </label>
            </div>
          </Card>

          <div className="tw-mt-4">
            <WeeklyScheduleBoard selectedShiftId={selectedShiftId} onSelectShift={handleSelectShift} />
          </div>
        </section>
      </div>

      <ShiftDetailDrawer isOpen={drawerOpen} onClose={() => setDrawerOpen(false)} shift={selectedShift} />
    </>
  );
}
