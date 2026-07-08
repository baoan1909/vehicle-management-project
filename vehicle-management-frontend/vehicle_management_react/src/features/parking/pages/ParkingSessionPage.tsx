import { useEffect, useMemo, useState } from "react";

import { Badge, Button, DateRangeInput, Drawer, Modal, PaginationFooter, SelectMenu } from "@/components/ui";
import { cn } from "@/lib/cn";

type SessionTab = "in_parking" | "completed" | "review" | "missing_evidence";
type SessionStatus = "OPEN" | "CLOSED";
type EvidenceStatus = "complete" | "missing" | "review";
type CustomerType = "visitor" | "subscription";
type EventType = "CHECK_IN" | "CHECK_OUT";

type EvidenceImage = {
  label: string;
  plate?: string;
  state: "available" | "missing";
  tone: "plate" | "person" | "amber" | "red";
};

type ParkingEventEvidence = {
  actor: string;
  eventTime: string;
  eventType: EventType;
  lane: string;
  licensePlateImage: EvidenceImage;
  note?: string;
  personImage: EvidenceImage;
};

type ParkingSessionRow = {
  cardCode: string;
  customerName: string;
  customerPhone: string;
  customerType: CustomerType;
  duration: string;
  entryTime: string;
  events: ParkingEventEvidence[];
  evidenceStatus: EvidenceStatus;
  exitTime: string | null;
  fee: string;
  gate: string;
  id: string;
  lane: string;
  licensePlate: string;
  parkingLot: string;
  reviewReason?: string;
  status: SessionStatus;
  vehicleType: string;
  zone: string;
};

const tabItems: Array<{ count: number; label: string; value: SessionTab }> = [
  { count: 128, label: "Đang trong bãi", value: "in_parking" },
  { count: 256, label: "Đã hoàn tất", value: "completed" },
  { count: 18, label: "Cần kiểm tra", value: "review" },
  { count: 7, label: "Thiếu bằng chứng", value: "missing_evidence" },
];

const lotOptions = [
  { label: "Bãi xe: Tất cả", value: "all" },
  { label: "Bãi xe Mỹ Đình", value: "Bãi xe Mỹ Đình" },
  { label: "Bãi xe Times City", value: "Bãi xe Times City" },
  { label: "Bãi xe Royal City", value: "Bãi xe Royal City" },
];

const laneOptions = [
  { label: "Làn: Tất cả", value: "all" },
  { label: "A1 - Cổng chính", value: "A1" },
  { label: "B2 - Cổng phụ", value: "B2" },
  { label: "C1 - Tầng hầm", value: "C1" },
];

const cardTypeOptions = [
  { label: "Loại thẻ: Tất cả", value: "all" },
  { label: "Vãng lai", value: "visitor" },
  { label: "Vé tháng", value: "subscription" },
];

const sessions: ParkingSessionRow[] = [
  {
    cardCode: "C-00321",
    customerName: "Nguyễn Văn An",
    customerPhone: "0987 654 321",
    customerType: "subscription",
    duration: "1 giờ 32 phút",
    entryTime: "05/06/2024 08:42",
    events: [
      {
        actor: "Trần Minh Hiếu",
        eventTime: "05/06/2024 08:42",
        eventType: "CHECK_IN",
        lane: "A1",
        licensePlateImage: { label: "Biển số vào", plate: "30A-123.45", state: "available", tone: "plate" },
        personImage: { label: "Người lái vào", state: "available", tone: "person" },
      },
    ],
    evidenceStatus: "complete",
    exitTime: null,
    fee: "-",
    gate: "Cổng chính",
    id: "PS-000258",
    lane: "A1",
    licensePlate: "30A-123.45",
    parkingLot: "Bãi xe Times City",
    status: "OPEN",
    vehicleType: "Ô tô",
    zone: "B2",
  },
  {
    cardCode: "C-00456",
    customerName: "Trần Thị Bình",
    customerPhone: "0912 345 678",
    customerType: "subscription",
    duration: "55 phút",
    entryTime: "05/06/2024 09:15",
    events: [
      {
        actor: "Lê Minh C",
        eventTime: "05/06/2024 09:15",
        eventType: "CHECK_IN",
        lane: "A1",
        licensePlateImage: { label: "Biển số vào", plate: "29B-456.78", state: "available", tone: "plate" },
        personImage: { label: "Người lái vào", state: "available", tone: "person" },
      },
    ],
    evidenceStatus: "review",
    exitTime: null,
    fee: "-",
    gate: "Cổng chính",
    id: "PS-000257",
    lane: "A1",
    licensePlate: "29B-456.78",
    parkingLot: "Bãi xe Royal City",
    reviewReason: "Thiếu ảnh ra",
    status: "OPEN",
    vehicleType: "Ô tô",
    zone: "R1",
  },
  {
    cardCode: "V-99887",
    customerName: "Lê Hoàng Nam",
    customerPhone: "0933 221 122",
    customerType: "visitor",
    duration: "30 phút",
    entryTime: "05/06/2024 10:10",
    events: [
      {
        actor: "Ngô Lan Anh",
        eventTime: "05/06/2024 10:10",
        eventType: "CHECK_IN",
        lane: "B2",
        licensePlateImage: { label: "Biển số vào", plate: "51H-567.89", state: "available", tone: "plate" },
        personImage: { label: "Người lái vào", state: "available", tone: "person" },
      },
    ],
    evidenceStatus: "complete",
    exitTime: null,
    fee: "-",
    gate: "Cổng phụ",
    id: "PS-000256",
    lane: "B2",
    licensePlate: "51H-567.89",
    parkingLot: "Bãi xe Times City",
    status: "OPEN",
    vehicleType: "Ô tô",
    zone: "A3",
  },
  {
    cardCode: "C-00388",
    customerName: "Phạm Minh Hạnh",
    customerPhone: "0909 111 000",
    customerType: "subscription",
    duration: "2 giờ 05 phút",
    entryTime: "05/06/2024 07:55",
    events: [
      {
        actor: "Vũ Thu Trang",
        eventTime: "05/06/2024 07:55",
        eventType: "CHECK_IN",
        lane: "C1",
        licensePlateImage: { label: "Biển số vào", plate: "30G-789.01", state: "available", tone: "plate" },
        personImage: { label: "Người lái vào", state: "missing", tone: "red" },
      },
    ],
    evidenceStatus: "missing",
    exitTime: null,
    fee: "-",
    gate: "Tầng hầm",
    id: "PS-000255",
    lane: "C1",
    licensePlate: "30G-789.01",
    parkingLot: "Bãi xe Mỹ Đình",
    reviewReason: "Thiếu ảnh người lái",
    status: "OPEN",
    vehicleType: "Ô tô",
    zone: "MD-A1",
  },
  {
    cardCode: "V-11223",
    customerName: "Đỗ Anh Khoa",
    customerPhone: "0988 222 333",
    customerType: "visitor",
    duration: "12 phút",
    entryTime: "05/06/2024 11:05",
    events: [
      {
        actor: "Đỗ Anh Khoa",
        eventTime: "05/06/2024 11:05",
        eventType: "CHECK_IN",
        lane: "A1",
        licensePlateImage: { label: "Biển số vào", plate: "99A-234.56", state: "available", tone: "plate" },
        personImage: { label: "Người lái vào", state: "available", tone: "person" },
      },
    ],
    evidenceStatus: "complete",
    exitTime: null,
    fee: "-",
    gate: "Cổng chính",
    id: "PS-000254",
    lane: "A1",
    licensePlate: "99A-234.56",
    parkingLot: "Bãi xe Royal City",
    status: "OPEN",
    vehicleType: "Xe máy",
    zone: "R1",
  },
  {
    cardCode: "C-00221",
    customerName: "Bùi Quang Huy",
    customerPhone: "0966 888 777",
    customerType: "subscription",
    duration: "3 giờ 20 phút",
    entryTime: "05/06/2024 06:40",
    events: [
      {
        actor: "Lê Hoàng Nam",
        eventTime: "05/06/2024 06:40",
        eventType: "CHECK_IN",
        lane: "B2",
        licensePlateImage: { label: "Biển số vào", plate: "15A-678.90", state: "available", tone: "plate" },
        personImage: { label: "Người lái vào", state: "available", tone: "person" },
      },
    ],
    evidenceStatus: "review",
    exitTime: null,
    fee: "-",
    gate: "Cổng phụ",
    id: "PS-000253",
    lane: "B2",
    licensePlate: "15A-678.90",
    parkingLot: "Bãi xe Times City",
    reviewReason: "Cần kiểm tra phí",
    status: "OPEN",
    vehicleType: "Ô tô",
    zone: "A2",
  },
  {
    cardCode: "V-44556",
    customerName: "Vũ Thị Mai",
    customerPhone: "0977 654 321",
    customerType: "visitor",
    duration: "22 phút",
    entryTime: "05/06/2024 10:25",
    events: [
      {
        actor: "Phạm Quốc Bảo",
        eventTime: "05/06/2024 10:25",
        eventType: "CHECK_IN",
        lane: "B2",
        licensePlateImage: { label: "Biển số vào", plate: "43A-567.89", state: "available", tone: "plate" },
        personImage: { label: "Người lái vào", state: "available", tone: "person" },
      },
    ],
    evidenceStatus: "complete",
    exitTime: null,
    fee: "-",
    gate: "Cổng phụ",
    id: "PS-000252",
    lane: "B2",
    licensePlate: "43A-567.89",
    parkingLot: "Bãi xe Mỹ Đình",
    status: "OPEN",
    vehicleType: "Ô tô",
    zone: "MD-A2",
  },
];

function customerTypeLabel(type: CustomerType) {
  return type === "subscription" ? "Vé tháng" : "Vãng lai";
}

function statusLabel(status: SessionStatus) {
  return status === "OPEN" ? "Đang trong bãi" : "Đã hoàn tất";
}

function evidenceLabel(status: EvidenceStatus) {
  if (status === "complete") return "Đủ ảnh";
  if (status === "review") return "Cần kiểm tra";
  return "Thiếu ảnh";
}

function evidenceTone(status: EvidenceStatus) {
  if (status === "complete") return "success";
  if (status === "review") return "warning";
  return "danger";
}

function matchesActiveTab(session: ParkingSessionRow, activeTab: SessionTab) {
  if (activeTab === "completed") return session.status === "CLOSED";
  if (activeTab === "review") return session.evidenceStatus === "review";
  if (activeTab === "missing_evidence") return session.evidenceStatus === "missing";
  return session.status === "OPEN";
}

function getCurrentTabTotal(activeTab: SessionTab) {
  return tabItems.find((tab) => tab.value === activeTab)?.count ?? sessions.length;
}

function SessionMetricCard({
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
    blue: "tw-bg-gradient-to-br tw-from-blue-500 tw-to-blue-700 tw-shadow-[0_12px_24px_rgba(37,99,235,0.22)]",
    green: "tw-bg-gradient-to-br tw-from-emerald-500 tw-to-green-700 tw-shadow-[0_12px_24px_rgba(16,185,129,0.20)]",
    orange: "tw-bg-gradient-to-br tw-from-orange-400 tw-to-orange-600 tw-shadow-[0_12px_24px_rgba(249,115,22,0.22)]",
    red: "tw-bg-gradient-to-br tw-from-red-400 tw-to-red-600 tw-shadow-[0_12px_24px_rgba(239,68,68,0.20)]",
  }[tone];

  const deltaDanger = delta.trim().startsWith("-");

  return (
    <section className="tw-flex tw-min-h-[112px] tw-items-center tw-gap-5 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-[0_10px_26px_rgba(15,23,42,0.04)]">
      <span className={cn("tw-inline-flex tw-h-14 tw-w-14 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-lg tw-text-[1.45rem] tw-text-white", toneClassName)}>
        <i className={icon} />
      </span>
      <span className="tw-min-w-0">
        <span className="tw-block tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-600">{label}</span>
        <strong className="tw-mt-1 tw-block tw-text-[1.8rem] tw-font-black tw-leading-none tw-text-vm-slate-900">{value}</strong>
        <span className={cn("tw-mt-2 tw-block tw-text-[0.82rem] tw-font-bold", deltaDanger ? "tw-text-red-500" : "tw-text-emerald-600")}>
          <i className={cn("tw-mr-1 fas", deltaDanger ? "fa-arrow-down" : "fa-arrow-up")} />
          {delta} so với hôm qua
        </span>
      </span>
    </section>
  );
}

function SessionTabs({ activeTab, onChange }: { activeTab: SessionTab; onChange: (tab: SessionTab) => void }) {
  return (
    <div className="tw-flex tw-min-w-0 tw-gap-9 tw-overflow-x-auto tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-6">
      {tabItems.map((tab) => (
        <button
          aria-pressed={activeTab === tab.value}
          className={cn(
            "tw-relative tw-inline-flex tw-h-[54px] tw-flex-shrink-0 tw-items-center tw-gap-2 tw-border-0 tw-bg-transparent tw-px-0 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-600 tw-transition hover:tw-text-vm-primary",
            activeTab === tab.value ? "tw-text-vm-primary after:tw-absolute after:tw-bottom-0 after:tw-left-0 after:tw-h-[3px] after:tw-w-full after:tw-rounded-t-full after:tw-bg-vm-primary" : "",
          )}
          key={tab.value}
          type="button"
          onClick={() => onChange(tab.value)}
        >
          {tab.label}
          <span className={cn("tw-rounded-vm-sm tw-px-2 tw-py-0.5 tw-text-[0.78rem] tw-font-black", activeTab === tab.value ? "tw-bg-brand-50 tw-text-vm-primary" : "tw-bg-vm-slate-50 tw-text-vm-slate-600")}>{tab.count}</span>
        </button>
      ))}
    </div>
  );
}

function EvidenceThumb({ image, size = "sm" }: { image: EvidenceImage; size?: "sm" | "lg" }) {
  const isLarge = size === "lg";

  if (image.state === "missing") {
    return (
      <div
        className={cn(
          "tw-grid tw-place-items-center tw-rounded-vm-md tw-border tw-border-dashed tw-border-vm-slate-200 tw-bg-vm-slate-25 tw-text-vm-slate-400",
          isLarge ? "tw-h-[132px]" : "tw-h-12 tw-w-[74px]",
        )}
      >
        <span className="tw-grid tw-gap-1 tw-text-center">
          <i className="far fa-image tw-text-[1.15rem]" />
          {isLarge ? <small className="tw-font-bold">Chưa có ảnh</small> : null}
        </span>
      </div>
    );
  }

  if (image.tone === "person") {
    return (
      <div className={cn("tw-relative tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-gradient-to-br tw-from-slate-200 tw-via-slate-100 tw-to-blue-100", isLarge ? "tw-h-[132px]" : "tw-h-12 tw-w-[74px]")}>
        <div className="tw-absolute tw-inset-x-0 tw-bottom-0 tw-h-1/2 tw-bg-gradient-to-t tw-from-slate-700/35 tw-to-transparent" />
        <div className="tw-absolute tw-left-1/2 tw-top-[21%] tw-h-8 tw-w-8 -tw-translate-x-1/2 tw-rounded-full tw-bg-slate-700/70" />
        <div className="tw-absolute tw-left-1/2 tw-top-[52%] tw-h-14 tw-w-20 -tw-translate-x-1/2 tw-rounded-t-full tw-bg-slate-700/60" />
      </div>
    );
  }

  return (
    <div className={cn("tw-relative tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-gradient-to-br tw-from-slate-900 tw-via-slate-700 tw-to-slate-950", isLarge ? "tw-h-[132px]" : "tw-h-12 tw-w-[74px]")}>
      <div className="tw-absolute tw-inset-x-2 tw-bottom-2 tw-top-5 tw-rounded tw-bg-slate-800/80" />
      <div className={cn("tw-absolute tw-left-1/2 tw-top-1/2 tw-grid -tw-translate-x-1/2 -tw-translate-y-1/2 tw-place-items-center tw-rounded tw-border tw-border-solid tw-border-slate-300 tw-bg-slate-100 tw-px-2 tw-py-1 tw-text-center tw-font-black tw-leading-tight tw-text-slate-900", isLarge ? "tw-text-[1rem]" : "tw-text-[0.63rem]")}>
        {image.plate ?? "PLATE"}
      </div>
    </div>
  );
}

function MetaTile({ icon, label, value }: { icon: string; label: string; value: string }) {
  return (
    <div className="tw-grid tw-gap-1">
      <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">
        <i className={icon} />
        {label}
      </span>
      <strong className="tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">{value}</strong>
    </div>
  );
}

function DrawerTabs({ activeTab, onChange }: { activeTab: string; onChange: (tab: "overview" | "evidence" | "payment" | "audit") => void }) {
  const tabs = [
    ["overview", "Tổng quan"],
    ["evidence", "Bằng chứng"],
    ["payment", "Thanh toán"],
    ["audit", "Nhật ký"],
  ] as const;

  return (
    <div className="tw-flex tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100">
      {tabs.map(([value, label]) => (
        <button
          className={cn(
            "tw-relative tw-h-11 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.84rem] tw-font-black tw-text-vm-slate-500",
            activeTab === value ? "tw-text-vm-primary after:tw-absolute after:tw-bottom-0 after:tw-left-4 after:tw-right-4 after:tw-h-[2px] after:tw-rounded-full after:tw-bg-vm-primary" : "",
          )}
          key={value}
          type="button"
          onClick={() => onChange(value)}
        >
          {label}
        </button>
      ))}
    </div>
  );
}

function EventEvidencePanel({ event, title }: { event?: ParkingEventEvidence; title: string }) {
  if (!event) {
    return (
      <section className="tw-grid tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
        <div className="tw-flex tw-items-center tw-justify-between">
          <strong className="tw-flex tw-items-center tw-gap-2 tw-text-[0.95rem] tw-font-black tw-text-vm-slate-900">
            <i className="far fa-clock tw-text-amber-500" />
            {title}
          </strong>
          <Badge tone="danger">Chưa có ảnh ra</Badge>
        </div>
        <div className="tw-grid tw-grid-cols-2 tw-gap-3">
          <EvidenceThumb image={{ label: "Biển số ra", state: "missing", tone: "red" }} size="lg" />
          <EvidenceThumb image={{ label: "Người lái ra", state: "missing", tone: "red" }} size="lg" />
        </div>
        <p className="tw-m-0 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">URL còn hiệu lực: -</p>
      </section>
    );
  }

  return (
    <section className="tw-grid tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
      <div className="tw-flex tw-items-center tw-justify-between">
        <strong className="tw-flex tw-items-center tw-gap-2 tw-text-[0.95rem] tw-font-black tw-text-vm-slate-900">
          <i className="far fa-check-circle tw-text-emerald-600" />
          {title}
        </strong>
        <Badge tone="success">URL còn hiệu lực</Badge>
      </div>
      <div className="tw-grid tw-grid-cols-2 tw-gap-3">
        <div className="tw-grid tw-gap-2">
          <EvidenceThumb image={event.licensePlateImage} size="lg" />
          <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">Ảnh biển số</span>
        </div>
        <div className="tw-grid tw-gap-2">
          <EvidenceThumb image={event.personImage} size="lg" />
          <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">Ảnh người lái</span>
        </div>
      </div>
      <div className="tw-flex tw-items-center tw-justify-between tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">
        <span>{event.eventTime}</span>
        <button className="tw-border-0 tw-bg-transparent tw-font-black tw-text-vm-primary" type="button">Làm mới URL</button>
      </div>
    </section>
  );
}

function SessionDetailDrawer({
  onClose,
  onOpenReview,
  open,
  session,
}: {
  onClose: () => void;
  onOpenReview: () => void;
  open: boolean;
  session: ParkingSessionRow | null;
}) {
  const [activeTab, setActiveTab] = useState<"overview" | "evidence" | "payment" | "audit">("evidence");

  useEffect(() => {
    if (open) setActiveTab("evidence");
  }, [open, session?.id]);

  if (!session) return null;

  const checkInEvent = session.events.find((event) => event.eventType === "CHECK_IN");
  const checkOutEvent = session.events.find((event) => event.eventType === "CHECK_OUT");

  return (
    <Drawer
      actions={
        <div className="tw-grid tw-gap-3">
          <div className="tw-grid tw-grid-cols-3 tw-gap-2">
            <Button className="tw-h-10 tw-px-2" variant="secondary"><i className="fas fa-download tw-mr-2" />Tải xuống</Button>
            <Button className="tw-h-10 tw-px-2" variant="secondary"><i className="fas fa-box-archive tw-mr-2" />Tạo gói bằng chứng</Button>
            <Button className="tw-h-10 tw-px-2" variant="secondary"><i className="fas fa-headset tw-mr-2" />Gửi hỗ trợ</Button>
          </div>
          <div className="tw-grid tw-grid-cols-[1fr_0.9fr] tw-gap-3">
            <Button className="tw-border-amber-200 tw-bg-amber-50 tw-text-amber-700 hover:tw-bg-amber-100" variant="secondary" onClick={onOpenReview}>
              <i className="fas fa-exclamation-triangle tw-mr-2" />Đánh dấu cần kiểm tra
            </Button>
            <Button variant="secondary" onClick={onClose}>Đóng</Button>
          </div>
        </div>
      }
      onClose={onClose}
      open={open}
      title="Chi tiết phiên gửi xe"
      width="lg"
    >
      <div className="tw-grid tw-gap-4">
        <div className="tw-flex tw-items-center tw-gap-3">
          <strong className="tw-text-[1.18rem] tw-font-black tw-text-vm-primary">{session.id}</strong>
          <Badge tone="success">{statusLabel(session.status)}</Badge>
        </div>

        <section className="tw-grid tw-grid-cols-[1fr_1.25fr] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white">
          <div className="tw-flex tw-items-center tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 tw-p-4">
            <strong className="tw-text-[1.35rem] tw-font-black tw-text-vm-slate-900">{session.licensePlate}</strong>
          </div>
          <div className="tw-grid tw-gap-1 tw-p-4">
            <strong className="tw-text-[0.98rem] tw-font-black tw-text-vm-slate-900">{session.customerName}</strong>
            <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-500"><i className="fas fa-phone" />{session.customerPhone}</span>
          </div>
        </section>

        <section className="tw-grid tw-grid-cols-4 tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4">
          <MetaTile icon="far fa-id-card" label="Thẻ" value={session.cardCode} />
          <MetaTile icon="far fa-calendar-check" label="Loại thẻ" value={customerTypeLabel(session.customerType)} />
          <MetaTile icon="fas fa-location-dot" label="Bãi xe" value={session.parkingLot.replace("Bãi xe ", "")} />
          <MetaTile icon="far fa-user" label="Khu vực" value={session.zone} />
          <MetaTile icon="fas fa-road" label="Làn" value={`${session.lane} - ${session.gate}`} />
          <MetaTile icon="far fa-clock" label="Giờ vào" value={session.entryTime} />
          <MetaTile icon="far fa-hourglass-half" label="Thời gian ở" value={session.duration} />
          <MetaTile icon="far fa-image" label="Bằng chứng" value={evidenceLabel(session.evidenceStatus)} />
        </section>

        <DrawerTabs activeTab={activeTab} onChange={setActiveTab} />

        {activeTab === "overview" ? (
          <section className="tw-grid tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4 tw-text-[0.86rem]">
            <MetaTile icon="fas fa-car" label="Phương tiện" value={`${session.vehicleType} - ${session.licensePlate}`} />
            <MetaTile icon="fas fa-warehouse" label="Vị trí" value={`${session.parkingLot}, khu ${session.zone}, làn ${session.lane}`} />
            <MetaTile icon="far fa-flag" label="Lý do kiểm tra" value={session.reviewReason ?? "Không có"} />
          </section>
        ) : null}

        {activeTab === "evidence" ? (
          <div className="tw-grid tw-grid-cols-2 tw-gap-3">
            <EventEvidencePanel event={checkInEvent} title="Vào bãi" />
            <EventEvidencePanel event={checkOutEvent} title="Ra bãi" />
          </div>
        ) : null}

        {activeTab === "payment" ? (
          <section className="tw-grid tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4">
            <MetaTile icon="far fa-credit-card" label="Phí gửi xe" value="20.000 đ" />
            <MetaTile icon="far fa-money-bill-1" label="Phí dịch vụ" value="2.000 đ" />
            <MetaTile icon="fas fa-calculator" label="Tổng tạm tính" value="22.000 đ" />
          </section>
        ) : null}

        {activeTab === "audit" ? (
          <section className="tw-grid tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4">
            {["Tạo phiên gửi xe", "Upload ảnh vào bãi", "Chờ xác nhận ảnh ra"].map((label, index) => (
              <div className="tw-grid tw-grid-cols-[14px_minmax(0,1fr)] tw-gap-3" key={label}>
                <span className={cn("tw-mt-1 tw-h-3 tw-w-3 tw-rounded-full", index === 2 ? "tw-bg-amber-500" : "tw-bg-vm-primary")} />
                <span className="tw-grid">
                  <strong className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-900">{label}</strong>
                  <small className="tw-mt-1 tw-font-semibold tw-text-vm-slate-500">05/06/2024 08:{index + 42}</small>
                </span>
              </div>
            ))}
          </section>
        ) : null}
      </div>
    </Drawer>
  );
}

function ReasonCard({ checked, label }: { checked?: boolean; label: string }) {
  return (
    <label className={cn("tw-flex tw-min-h-[66px] tw-items-start tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-bg-white tw-p-3 tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700", checked ? "tw-border-vm-primary tw-bg-brand-50 tw-text-vm-slate-900" : "tw-border-vm-slate-100")}>
      <input className="tw-mt-0.5 tw-accent-vm-primary" defaultChecked={checked} name="review-reason" type="radio" />
      {label}
    </label>
  );
}

function PackageThumb({ image, label, missing }: { image?: EvidenceImage; label: string; missing?: boolean }) {
  return (
    <div className="tw-grid tw-gap-2">
      <div className={cn("tw-h-[70px] tw-rounded-vm-md", missing ? "tw-grid tw-place-items-center tw-border tw-border-dashed tw-border-red-300 tw-bg-red-50 tw-text-red-500" : "")}>
        {missing ? (
          <span className="tw-grid tw-gap-1 tw-text-center tw-text-[0.78rem] tw-font-black"><i className="fas fa-exclamation-triangle" />Thiếu ảnh ra</span>
        ) : (
          <EvidenceThumb image={image ?? { label, state: "missing", tone: "red" }} size="lg" />
        )}
      </div>
      <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">{label}</span>
      <small className="tw-font-semibold tw-text-vm-slate-500">08:12</small>
    </div>
  );
}

function ReviewSessionModal({ onClose, open, session }: { onClose: () => void; open: boolean; session: ParkingSessionRow | null }) {
  if (!session) return null;

  return (
    <Modal
      actions={
        <div className="tw-flex tw-justify-end tw-gap-3">
          <Button className="tw-min-w-[140px]" variant="secondary" onClick={onClose}>Hủy</Button>
          <Button className="tw-min-w-[220px]" onClick={onClose}>Lưu & chuyển xử lý</Button>
        </div>
      }
      onClose={onClose}
      open={open}
      title="Xử lý phiên cần kiểm tra"
      width="lg"
    >
      <div className="tw-grid tw-gap-4">
        <section className="tw-grid tw-grid-cols-[44px_1fr_1fr_auto] tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-4">
          <span className="tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-brand-50 tw-text-vm-primary"><i className="fas fa-car" /></span>
          <div className="tw-grid">
            <strong className="tw-text-[1rem] tw-font-black tw-text-vm-slate-900">{session.id}</strong>
            <span className="tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Mã phiên</span>
          </div>
          <div className="tw-grid">
            <strong className="tw-text-[1rem] tw-font-black tw-text-vm-slate-900">{session.licensePlate}</strong>
            <span className="tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Biển số</span>
          </div>
          <div className="tw-flex tw-gap-2">
            <Badge tone="warning">MANUAL_REVIEW</Badge>
            <Badge tone="danger">Thiếu bằng chứng</Badge>
          </div>
        </section>

        <section className="tw-grid tw-gap-2">
          <strong className="tw-text-[0.88rem] tw-font-black tw-text-vm-slate-900">Lý do kiểm tra</strong>
          <div className="tw-grid tw-grid-cols-5 tw-gap-2">
            <ReasonCard checked label="Thiếu ảnh ra" />
            <ReasonCard label="Biển số không khớp" />
            <ReasonCard label="Khách khiếu nại phí" />
            <ReasonCard label="Mất thẻ" />
            <ReasonCard label="Khác" />
          </div>
        </section>

        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.88rem] tw-font-black tw-text-vm-slate-900">Ghi chú xử lý</span>
          <span className="tw-relative">
            <textarea className="tw-min-h-[92px] tw-w-full tw-resize-none tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-white tw-p-3 tw-pr-16 tw-text-[0.88rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-900 tw-outline-none tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)]" defaultValue="Ảnh ra chưa được upload từ làn A1, cần kiểm tra camera lúc 08:42" />
            <small className="tw-absolute tw-bottom-3 tw-right-3 tw-font-semibold tw-text-vm-slate-500">63/500</small>
          </span>
        </label>

        <section className="tw-grid tw-gap-2">
          <strong className="tw-text-[0.88rem] tw-font-black tw-text-vm-slate-900">Tạo ticket hỗ trợ</strong>
          <div className="tw-grid tw-grid-cols-[0.7fr_1.4fr] tw-gap-2">
            <label className="tw-flex tw-h-11 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-brand-50 tw-px-3 tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-900">
              <input className="tw-accent-vm-primary" defaultChecked name="ticket-mode" type="radio" />
              Tạo ticket mới
            </label>
            <label className="tw-flex tw-h-11 tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-700">
              <input className="tw-accent-vm-primary" name="ticket-mode" type="radio" />
              Gắn vào ticket hiện có
              <SelectMenu ariaLabel="Ticket hiện có" className="tw-ml-auto tw-w-[220px]" options={[{ label: "TK-20240605-018", value: "TK-20240605-018" }, { label: "TK-20240605-012", value: "TK-20240605-012" }]} value="TK-20240605-018" onChange={() => undefined} />
            </label>
          </div>
        </section>

        <div className="tw-grid tw-grid-cols-3 tw-gap-3">
          <SelectMenu ariaLabel="Người phụ trách" options={[{ label: "Trần Thị Mai - Nhân viên vận hành", value: "mai" }, { label: "Trần Minh Hiếu", value: "hieu" }]} value="mai" onChange={() => undefined} />
          <SelectMenu ariaLabel="Nhóm vận hành" options={[{ label: "Nhóm vận hành bãi", value: "ops" }, { label: "Nhóm CSKH", value: "cskh" }]} value="ops" onChange={() => undefined} />
          <SelectMenu ariaLabel="Ưu tiên" options={[{ label: "Cao", value: "high" }, { label: "Trung bình", value: "normal" }, { label: "Thấp", value: "low" }]} value="high" onChange={() => undefined} />
        </div>

        <section className="tw-grid tw-gap-3">
          <strong className="tw-text-[0.88rem] tw-font-black tw-text-vm-slate-900">Tạo gói bằng chứng</strong>
          <div className="tw-grid tw-grid-cols-[1fr_1fr_1fr_1fr_112px] tw-gap-3">
            <PackageThumb image={session.events[0]?.licensePlateImage} label="Biển số vào" />
            <PackageThumb image={session.events[0]?.personImage} label="Người lái vào" />
            <PackageThumb label="Biển số ra" missing />
            <PackageThumb label="Người lái ra" missing />
            <div className="tw-grid tw-content-center tw-gap-2">
              <Button className="tw-h-10 tw-px-2" variant="secondary">Xem tất cả (4)</Button>
              <span className="tw-text-center tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">Gói bao gồm: 4 mục</span>
            </div>
          </div>
        </section>

        <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-px-4 tw-py-3 tw-text-[0.82rem] tw-font-bold tw-text-amber-700">
          <i className="fas fa-lock tw-mr-2" />Chỉ người được phân quyền mới xem được ảnh riêng tư. Link tải xuống sẽ hết hạn theo thời gian đã chọn.
        </div>
      </div>
    </Modal>
  );
}

export function ParkingSessionPage() {
  const [activeTab, setActiveTab] = useState<SessionTab>("in_parking");
  const [dateRange, setDateRange] = useState("2024-06-01|2024-06-05");
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [laneFilter, setLaneFilter] = useState("all");
  const [lotFilter, setLotFilter] = useState("all");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [reviewOpen, setReviewOpen] = useState(false);
  const [searchValue, setSearchValue] = useState("");
  const [selectedSession, setSelectedSession] = useState<ParkingSessionRow | null>(sessions[0]);
  const [typeFilter, setTypeFilter] = useState("all");

  const filteredSessions = useMemo(() => {
    const search = searchValue.trim().toLowerCase();

    return sessions.filter((session) => {
      const matchesTab = matchesActiveTab(session, activeTab);
      const matchesLot = lotFilter === "all" || session.parkingLot === lotFilter;
      const matchesLane = laneFilter === "all" || session.lane === laneFilter;
      const matchesType = typeFilter === "all" || session.customerType === typeFilter;
      const matchesSearch = !search || [session.id, session.licensePlate, session.cardCode, session.customerName].some((value) => value.toLowerCase().includes(search));

      return matchesTab && matchesLot && matchesLane && matchesType && matchesSearch;
    });
  }, [activeTab, laneFilter, lotFilter, searchValue, typeFilter]);

  useEffect(() => {
    setPage(1);
  }, [activeTab, laneFilter, lotFilter, searchValue, typeFilter]);

  const tabTotal = getCurrentTabTotal(activeTab);
  const totalPages = Math.max(1, Math.ceil(tabTotal / pageSize));
  const currentPage = Math.min(page, totalPages);
  const startIndex = filteredSessions.length === 0 ? 0 : (currentPage - 1) * pageSize + 1;
  const endIndex = filteredSessions.length === 0 ? 0 : Math.min((currentPage - 1) * pageSize + filteredSessions.length, tabTotal);

  const openSession = (session: ParkingSessionRow) => {
    setSelectedSession(session);
    setDrawerOpen(true);
  };

  return (
    <div className="content-header tw-px-0 tw-pb-4 tw-pt-3">
      <section className="content tw-pb-8">
        <div className="container-fluid tw-max-w-[1480px]">
          <div className="tw-grid tw-gap-6">
            <header>
              <h1 className="tw-m-0 tw-text-vm-page-title tw-font-black tw-text-vm-slate-900">Phiên gửi xe</h1>
              <p className="tw-m-0 tw-mt-2 tw-text-[0.92rem] tw-font-semibold tw-text-vm-slate-500">Quản lý toàn bộ phiên gửi xe theo trạng thái và bằng chứng.</p>
            </header>

            <div className="tw-grid tw-grid-cols-4 tw-gap-5 max-[1180px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
              <SessionMetricCard delta="+12" icon="fas fa-car" label="Đang trong bãi" tone="blue" value="128" />
              <SessionMetricCard delta="+18" icon="far fa-check-circle" label="Đã hoàn tất hôm nay" tone="green" value="256" />
              <SessionMetricCard delta="+6" icon="fas fa-exclamation-triangle" label="Cần kiểm tra" tone="orange" value="24" />
              <SessionMetricCard delta="-3" icon="far fa-image" label="Thiếu bằng chứng" tone="red" value="16" />
            </div>

            <section className="tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-[0_16px_34px_rgba(15,23,42,0.04)]">
              <SessionTabs activeTab={activeTab} onChange={setActiveTab} />

              <div className="tw-grid tw-grid-cols-[minmax(280px,1fr)_160px_160px_170px_248px_120px] tw-gap-3 tw-px-6 tw-py-5 max-[1260px]:tw-grid-cols-3 max-[760px]:tw-grid-cols-1">
                <label className="tw-flex tw-h-[42px] tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-shadow-[0_4px_10px_rgba(15,23,42,0.025)]">
                  <input className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.88rem] tw-font-semibold tw-outline-none placeholder:tw-text-vm-slate-500" placeholder="Tìm theo biển số, mã phiên, mã thẻ..." value={searchValue} onChange={(event) => setSearchValue(event.target.value)} />
                  <i className="fas fa-search tw-text-vm-slate-500" />
                </label>
                <SelectMenu ariaLabel="Bãi xe" options={lotOptions} value={lotFilter} onChange={setLotFilter} />
                <SelectMenu ariaLabel="Làn" options={laneOptions} value={laneFilter} onChange={setLaneFilter} />
                <SelectMenu ariaLabel="Loại thẻ" options={cardTypeOptions} value={typeFilter} onChange={setTypeFilter} />
                <DateRangeInput value={dateRange} onChange={setDateRange} />
                <Button className="tw-h-[42px] tw-whitespace-nowrap tw-px-4" variant="secondary"><i className="fas fa-filter tw-mr-2" />Bộ lọc</Button>
              </div>

              <div className="tw-px-6 tw-pb-5">
                <div className="tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100">
                  <div className="tw-overflow-x-auto">
                    <table className="table tw-m-0 tw-min-w-[1220px] [&_td]:tw-border-0 [&_td]:tw-border-t [&_td]:tw-border-solid [&_td]:tw-border-vm-slate-100 [&_td]:tw-px-4 [&_td]:tw-py-3 [&_td]:tw-align-middle [&_thead_th]:tw-border-0 [&_thead_th]:tw-bg-vm-slate-25 [&_thead_th]:tw-px-4 [&_thead_th]:tw-py-3.5 [&_thead_th]:tw-text-left [&_thead_th]:tw-text-[0.78rem] [&_thead_th]:tw-font-black [&_thead_th]:tw-text-vm-slate-700">
                      <thead>
                        <tr>
                          <th className="tw-w-10"><input className="tw-accent-vm-primary" type="checkbox" /></th>
                          <th>Mã phiên</th>
                          <th>Biển số</th>
                          <th>Khách hàng</th>
                          <th>Thẻ</th>
                          <th>Bãi xe / Làn</th>
                          <th>Giờ vào</th>
                          <th>Trạng thái</th>
                          <th>Bằng chứng</th>
                          <th>Thao tác</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredSessions.map((session, index) => {
                          const checkInEvent = session.events.find((event) => event.eventType === "CHECK_IN");
                          const isSelected = selectedSession?.id === session.id;

                          return (
                            <tr
                              className={cn("tw-cursor-pointer tw-transition hover:[&>td]:tw-bg-brand-50/60", isSelected ? "[&>td]:tw-bg-brand-50 tw-shadow-[inset_3px_0_0_#2563EB]" : "")}
                              key={session.id}
                              onClick={() => openSession(session)}
                            >
                              <td><input checked={isSelected} className="tw-accent-vm-primary" readOnly type="checkbox" /></td>
                              <td><strong className="tw-text-[0.88rem] tw-font-black tw-text-vm-primary">{session.id}</strong></td>
                              <td><strong className="tw-text-[0.88rem] tw-font-black tw-text-vm-slate-900">{session.licensePlate}</strong></td>
                              <td>
                                <strong className="tw-block tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">{session.customerName}</strong>
                                <span className="tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">{session.customerPhone}</span>
                              </td>
                              <td>
                                <strong className="tw-block tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">{session.cardCode}</strong>
                                <span className="tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">{customerTypeLabel(session.customerType)}</span>
                              </td>
                              <td>
                                <strong className="tw-block tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">{session.parkingLot}</strong>
                                <Badge className="tw-mt-1" tone="neutral">{session.zone} - {session.lane}</Badge>
                              </td>
                              <td>
                                <strong className="tw-block tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">{session.entryTime.split(" ")[0]}</strong>
                                <span className="tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">{session.duration}</span>
                              </td>
                              <td><Badge tone="success">{statusLabel(session.status)}</Badge></td>
                              <td><Badge tone={evidenceTone(session.evidenceStatus)}>{evidenceLabel(session.evidenceStatus)}</Badge></td>
                              <td>
                                <div className="tw-flex tw-items-center tw-gap-2">
                                  <button className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-vm-primary hover:tw-bg-brand-50" type="button" aria-label="Xem chi tiết" onClick={(event) => { event.stopPropagation(); openSession(session); }}>
                                    <i className="far fa-eye" />
                                  </button>
                                  <button className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-vm-primary hover:tw-bg-brand-50" type="button" aria-label="Xem bằng chứng" onClick={(event) => { event.stopPropagation(); openSession(session); }}>
                                    <i className="far fa-image" />
                                  </button>
                                  <button className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-slate-500 hover:tw-bg-vm-slate-25" type="button" aria-label="Tác vụ" onClick={(event) => event.stopPropagation()}>
                                    <i className="fas fa-ellipsis-v" />
                                  </button>
                                </div>
                                <span className="tw-sr-only">{checkInEvent?.actor ?? `NV ${index + 1}`}</span>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>

                  <PaginationFooter
                    currentPage={currentPage}
                    endIndex={endIndex}
                    onPageChange={setPage}
                    onPageSizeChange={(nextPageSize) => {
                      setPageSize(nextPageSize);
                      setPage(1);
                    }}
                    pageSize={pageSize}
                    pageSizeOptions={[10, 20, 50]}
                    startIndex={startIndex}
                    totalPages={totalPages}
                    totalRecords={tabTotal}
                  />
                </div>
              </div>
            </section>
          </div>
        </div>
      </section>

      <SessionDetailDrawer
        onClose={() => setDrawerOpen(false)}
        onOpenReview={() => setReviewOpen(true)}
        open={drawerOpen}
        session={selectedSession}
      />
      <ReviewSessionModal open={reviewOpen} session={selectedSession} onClose={() => setReviewOpen(false)} />
    </div>
  );
}
