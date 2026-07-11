import { useEffect, useMemo, useState } from "react";

import { Badge, DateRangeInput, Drawer, FilterToolbar, PaginationFooter, SelectMenu } from "@/components/ui";
import {
  fetchParkingSessions,
  fetchParkingZones,
  fetchVehicleTypes,
  type ParkingSessionManagementEventResponse,
  type ParkingSessionManagementResponse,
  type VehicleTypeResponse,
  type ZoneResponse,
} from "@/features/parking/api/parkingSessionApi";
import { cn } from "@/lib/cn";

type SessionTab = "all" | "open" | "closed" | "missing_evidence";
type BadgeTone = "primary" | "success" | "warning" | "danger" | "neutral";

const tabItems: Array<{ label: string; value: SessionTab }> = [
  { label: "Tất cả", value: "all" },
  { label: "Đang trong bãi", value: "open" },
  { label: "Đã hoàn tất", value: "closed" },
  { label: "Thiếu bằng chứng", value: "missing_evidence" },
];

function todayIso() {
  const today = new Date();
  return toIsoDate(today);
}

function daysAgoIso(days: number) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return toIsoDate(date);
}

function toIsoDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function splitDateRange(value: string) {
  const [fromDate, toDate] = value.split("|");
  return { fromDate: fromDate || undefined, toDate: toDate || undefined };
}

function formatCurrency(value?: number | null) {
  if (typeof value !== "number" || !Number.isFinite(value)) return "--";
  return new Intl.NumberFormat("vi-VN", {
    currency: "VND",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(value);
}

function fallback(value?: string | null) {
  return value?.trim() || "Chưa có dữ liệu";
}

function statusLabel(status?: string) {
  if (status === "OPEN") return "Đang trong bãi";
  if (status === "CLOSED") return "Đã hoàn tất";
  if (status === "LOST_CARD") return "Mất thẻ";
  return "Chưa có dữ liệu";
}

function statusTone(status?: string): BadgeTone {
  if (status === "OPEN") return "primary";
  if (status === "CLOSED") return "success";
  if (status === "LOST_CARD") return "danger";
  return "neutral";
}

function buildVehicleTypeOptions(vehicleTypes: VehicleTypeResponse[]) {
  return [
    { label: "Loại xe: Tất cả", value: "all" },
    ...vehicleTypes.map((vehicleType) => ({
      label: [vehicleType.code, vehicleType.name].filter(Boolean).join(" • "),
      value: vehicleType.vehicleTypeId,
    })),
  ];
}

function buildZoneOptions(zones: ZoneResponse[]) {
  return [
    { label: "Khu vực: Tất cả", value: "all" },
    ...zones.map((zone) => ({
      label: [zone.code, zone.name].filter(Boolean).join(" • "),
      value: zone.zoneId,
    })),
  ];
}

function customerTypeLabel(session: ParkingSessionManagementResponse) {
  return session.customerId || session.customerVehicleId ? "Khách đăng ký" : "Khách vãng lai";
}

function hasMissingEvidence(session: ParkingSessionManagementResponse) {
  const checkIn = getEvent(session, "CHECK_IN");
  const checkOut = getEvent(session, "CHECK_OUT");
  const missingCheckIn = !checkIn?.licensePlateImagePath || !checkIn?.personImagePath;
  const missingCheckOut = session.status === "CLOSED" && (!checkOut?.licensePlateImagePath || !checkOut?.personImagePath);
  return missingCheckIn || missingCheckOut;
}

function getEvent(session: ParkingSessionManagementResponse, type: "CHECK_IN" | "CHECK_OUT") {
  return session.events?.find((event) => event.eventType === type);
}

function licensePlate(session: ParkingSessionManagementResponse) {
  return session.licensePlateOut || session.licensePlateIn || "Chưa có dữ liệu";
}

function sessionShortId(session: ParkingSessionManagementResponse) {
  return session.parkingSessionId.slice(0, 13);
}

function SessionTabs({
  activeTab,
  counts,
  onChange,
}: {
  activeTab: SessionTab;
  counts: Record<SessionTab, number>;
  onChange: (tab: SessionTab) => void;
}) {
  return (
    <div className="tw-flex tw-min-w-0 tw-gap-8 tw-overflow-x-auto tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-6">
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
          <span className={cn("tw-rounded-vm-sm tw-px-2 tw-py-0.5 tw-text-[0.78rem] tw-font-black", activeTab === tab.value ? "tw-bg-brand-50 tw-text-vm-primary" : "tw-bg-vm-slate-50 tw-text-vm-slate-600")}>
            {counts[tab.value]}
          </span>
        </button>
      ))}
    </div>
  );
}

function MetricCard({ icon, label, tone, value }: { icon: string; label: string; tone: "blue" | "green" | "orange" | "red"; value: number }) {
  const toneClassName = {
    blue: "tw-bg-blue-600",
    green: "tw-bg-emerald-600",
    orange: "tw-bg-amber-500",
    red: "tw-bg-red-500",
  }[tone];

  return (
    <section className="tw-flex tw-min-h-[104px] tw-items-center tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-[0_10px_26px_rgba(15,23,42,0.04)]">
      <span className={cn("tw-inline-flex tw-h-12 tw-w-12 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-lg tw-text-white", toneClassName)}>
        <i className={icon} />
      </span>
      <span className="tw-min-w-0">
        <span className="tw-block tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-600">{label}</span>
        <strong className="tw-mt-1 tw-block tw-text-[1.65rem] tw-font-black tw-leading-none tw-text-vm-slate-900">{value}</strong>
      </span>
    </section>
  );
}

function EvidenceImage({ label, src }: { label: string; src?: string | null }) {
  return (
    <div className="tw-grid tw-gap-2">
      <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">{label}</span>
      {src ? (
        <div className="tw-h-[150px] tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25">
          <img alt={label} className="tw-h-full tw-w-full tw-object-cover" src={src} />
        </div>
      ) : (
        <div className="tw-grid tw-h-[150px] tw-place-items-center tw-rounded-vm-md tw-border tw-border-dashed tw-border-vm-slate-200 tw-bg-vm-slate-25 tw-text-vm-slate-500">
          <span className="tw-grid tw-gap-2 tw-text-center tw-text-[0.82rem] tw-font-bold">
            <i className="far fa-image tw-text-[1.35rem]" />
            Chưa có ảnh
          </span>
        </div>
      )}
    </div>
  );
}

function EventBlock({ event, title }: { event?: ParkingSessionManagementEventResponse; title: string }) {
  return (
    <section className="tw-grid tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4">
      <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
        <strong className="tw-text-[0.96rem] tw-font-black tw-text-vm-slate-900">{title}</strong>
        <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">{fallback(event?.eventTime)}</span>
      </div>
      <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[620px]:tw-grid-cols-1">
        <EvidenceImage label="Ảnh biển số" src={event?.licensePlateImagePath} />
        <EvidenceImage label="Ảnh người / tài xế" src={event?.personImagePath} />
      </div>
      <div className="tw-grid tw-grid-cols-2 tw-gap-3 tw-text-[0.82rem] max-[620px]:tw-grid-cols-1">
        <Detail label="Làn xe" value={[event?.laneCode, event?.laneName].filter(Boolean).join(" • ")} />
        <Detail label="Biển số nhận diện" value={event?.licensePlateDetected} />
      </div>
      {event?.note ? <p className="tw-m-0 tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-3 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-700">{event.note}</p> : null}
    </section>
  );
}

function Detail({ label, value }: { label: string; value?: string | number | null }) {
  return (
    <div className="tw-grid tw-gap-1">
      <span className="tw-text-[0.76rem] tw-font-bold tw-text-vm-slate-500">{label}</span>
      <strong className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-900">{fallback(value == null ? null : String(value))}</strong>
    </div>
  );
}

function SessionDetailDrawer({
  onClose,
  open,
  session,
}: {
  onClose: () => void;
  open: boolean;
  session: ParkingSessionManagementResponse | null;
}) {
  if (!session) return null;
  const checkIn = getEvent(session, "CHECK_IN");
  const checkOut = getEvent(session, "CHECK_OUT");

  return (
    <Drawer
      description={`${licensePlate(session)} • ${statusLabel(session.status)}`}
      onClose={onClose}
      open={open}
      title={`Phiên ${sessionShortId(session)}`}
      width="xl"
    >
      <div className="tw-grid tw-gap-4">
        <section className="tw-grid tw-grid-cols-2 tw-gap-3 max-[620px]:tw-grid-cols-1">
          <Detail label="Mã thẻ" value={[session.cardNumber, session.cardUid].filter(Boolean).join(" • ")} />
          <Detail label="Loại thẻ" value={session.cardTypeName || session.cardTypeCode} />
          <Detail label="Khách" value={customerTypeLabel(session)} />
          <Detail label="Loại xe" value={[session.vehicleTypeCode, session.vehicleTypeName].filter(Boolean).join(" • ")} />
          <Detail label="Bãi xe" value={[session.parkingLotCode, session.parkingLotName].filter(Boolean).join(" • ")} />
          <Detail label="Khu vực" value={[session.zoneCode, session.zoneName].filter(Boolean).join(" • ")} />
          <Detail label="Thời gian vào" value={session.checkInTime} />
          <Detail label="Thời gian ra" value={session.checkOutTime} />
          <Detail label="Phí" value={formatCurrency(session.totalPrice)} />
          <div className="tw-grid tw-gap-1">
            <span className="tw-text-[0.76rem] tw-font-bold tw-text-vm-slate-500">Trạng thái</span>
            <span><Badge tone={statusTone(session.status)}>{statusLabel(session.status)}</Badge></span>
          </div>
        </section>

        <EventBlock event={checkIn} title="Bằng chứng check-in" />
        <EventBlock event={checkOut} title="Bằng chứng check-out" />
      </div>
    </Drawer>
  );
}

export function ParkingSessionPage() {
  const defaultDateRange = `${daysAgoIso(6)}|${todayIso()}`;
  const [activeTab, setActiveTab] = useState<SessionTab>("all");
  const [dateRange, setDateRange] = useState(defaultDateRange);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [searchValue, setSearchValue] = useState("");
  const [selectedSession, setSelectedSession] = useState<ParkingSessionManagementResponse | null>(null);
  const [sessions, setSessions] = useState<ParkingSessionManagementResponse[]>([]);
  const [vehicleTypeFilter, setVehicleTypeFilter] = useState("all");
  const [vehicleTypes, setVehicleTypes] = useState<VehicleTypeResponse[]>([]);
  const [zoneFilter, setZoneFilter] = useState("all");
  const [zones, setZones] = useState<ZoneResponse[]>([]);
  const [loadError, setLoadError] = useState("");
  const [filterLoadError, setFilterLoadError] = useState("");

  useEffect(() => {
    let active = true;

    async function loadFilters() {
      setFilterLoadError("");
      try {
        const [nextVehicleTypes, nextZones] = await Promise.all([
          fetchVehicleTypes(),
          fetchParkingZones("ACTIVE"),
        ]);
        if (!active) return;
        setVehicleTypes(nextVehicleTypes);
        setZones(nextZones);
      } catch (error) {
        if (!active) return;
        setVehicleTypes([]);
        setZones([]);
        setFilterLoadError(error instanceof Error ? error.message : "Không tải được bộ lọc loại xe/khu vực.");
      }
    }

    void loadFilters();

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    const { fromDate, toDate } = splitDateRange(dateRange);

    async function loadSessions() {
      setIsLoading(true);
      setLoadError("");
      try {
        const nextSessions = await fetchParkingSessions({
          fromDate,
          keyword: searchValue,
          toDate,
          vehicleTypeId: vehicleTypeFilter === "all" ? undefined : vehicleTypeFilter,
          zoneId: zoneFilter === "all" ? undefined : zoneFilter,
        });
        if (!active) return;
        setSessions(nextSessions);
        setSelectedSession((current) => {
          if (!current) return nextSessions[0] ?? null;
          return nextSessions.find((session) => session.parkingSessionId === current.parkingSessionId) ?? nextSessions[0] ?? null;
        });
      } catch (error) {
        if (!active) return;
        setSessions([]);
        setSelectedSession(null);
        setLoadError(error instanceof Error ? error.message : "Không tải được danh sách phiên gửi xe.");
      } finally {
        if (active) setIsLoading(false);
      }
    }

    const timer = window.setTimeout(() => {
      void loadSessions();
    }, 250);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [dateRange, searchValue, vehicleTypeFilter, zoneFilter]);

  const vehicleTypeOptions = useMemo(() => buildVehicleTypeOptions(vehicleTypes), [vehicleTypes]);

  const filteredZoneOptions = useMemo(() => {
    const matchedZones = vehicleTypeFilter === "all"
      ? zones
      : zones.filter((zone) => zone.vehicleTypeId === vehicleTypeFilter);

    return buildZoneOptions(matchedZones);
  }, [vehicleTypeFilter, zones]);

  useEffect(() => {
    if (zoneFilter === "all") return;
    if (filteredZoneOptions.some((option) => option.value === zoneFilter)) return;
    setZoneFilter("all");
  }, [filteredZoneOptions, zoneFilter]);

  const counts = useMemo<Record<SessionTab, number>>(() => ({
    all: sessions.length,
    open: sessions.filter((session) => session.status === "OPEN").length,
    closed: sessions.filter((session) => session.status === "CLOSED").length,
    missing_evidence: sessions.filter(hasMissingEvidence).length,
  }), [sessions]);

  const filteredSessions = useMemo(() => sessions.filter((session) => {
    if (activeTab === "open") return session.status === "OPEN";
    if (activeTab === "closed") return session.status === "CLOSED";
    if (activeTab === "missing_evidence") return hasMissingEvidence(session);
    return true;
  }), [activeTab, sessions]);

  useEffect(() => {
    setPage(1);
  }, [activeTab, dateRange, searchValue, vehicleTypeFilter, zoneFilter]);

  const totalRecords = filteredSessions.length;
  const totalPages = Math.max(1, Math.ceil(totalRecords / pageSize));
  const currentPage = Math.min(page, totalPages);
  const pageStart = (currentPage - 1) * pageSize;
  const visibleSessions = filteredSessions.slice(pageStart, pageStart + pageSize);
  const startIndex = totalRecords === 0 ? 0 : pageStart + 1;
  const endIndex = Math.min(pageStart + pageSize, totalRecords);

  const openSession = (session: ParkingSessionManagementResponse) => {
    setSelectedSession(session);
    setDrawerOpen(true);
  };

  const handleVehicleTypeFilterChange = (value: string) => {
    setVehicleTypeFilter(value);
    setZoneFilter("all");
  };

  const resetFilters = () => {
    setActiveTab("all");
    setDateRange(defaultDateRange);
    setSearchValue("");
    setVehicleTypeFilter("all");
    setZoneFilter("all");
    setPage(1);
  };

  return (
    <div className="content-header tw-px-0 tw-pb-4 tw-pt-3">
      <section className="content tw-pb-8">
        <div className="container-fluid tw-max-w-[1480px]">
          <div className="tw-grid tw-gap-6 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4 tw-shadow-[0_16px_34px_rgba(15,23,42,0.04)]">
            <header>
              <h1 className="tw-m-0 tw-text-vm-page-title tw-font-black tw-text-vm-slate-900">Phiên gửi xe</h1>
              <p className="tw-m-0 tw-mt-2 tw-text-[0.92rem] tw-font-semibold tw-text-vm-slate-500">Quản lý phiên check-in/check-out và bằng chứng vận hành.</p>
            </header>

            <div className="tw-grid tw-grid-cols-4 tw-gap-5 max-[1180px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
              <MetricCard icon="fas fa-list" label="Tổng phiên" tone="blue" value={counts.all} />
              <MetricCard icon="fas fa-car" label="Đang trong bãi" tone="orange" value={counts.open} />
              <MetricCard icon="fas fa-check" label="Đã hoàn tất" tone="green" value={counts.closed} />
              <MetricCard icon="far fa-image" label="Thiếu bằng chứng" tone="red" value={counts.missing_evidence} />
            </div>

            <section className="tw-overflow-visible tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-[0_16px_34px_rgba(15,23,42,0.04)]">
              <SessionTabs activeTab={activeTab} counts={counts} onChange={setActiveTab} />

              <FilterToolbar
                className="tw-grid tw-grid-cols-[minmax(260px,1fr)_190px_190px_248px_auto] tw-items-end tw-gap-3 tw-px-6 tw-py-5 max-[1180px]:tw-grid-cols-3 max-[820px]:tw-grid-cols-2 max-[620px]:tw-grid-cols-1"
                onReset={resetFilters}
                onSearchChange={setSearchValue}
                searchPlaceholder="Tìm theo biển số, mã phiên, mã thẻ, UID..."
                searchValue={searchValue}
              >
                <label className="tw-m-0 tw-grid tw-gap-[0.35rem]">
                  <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">Loại xe</span>
                  <SelectMenu ariaLabel="Loại xe" options={vehicleTypeOptions} value={vehicleTypeFilter} onChange={handleVehicleTypeFilterChange} />
                </label>
                <label className="tw-m-0 tw-grid tw-gap-[0.35rem]">
                  <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">Khu vực</span>
                  <SelectMenu ariaLabel="Khu vực" options={filteredZoneOptions} value={zoneFilter} onChange={setZoneFilter} />
                </label>
                <DateRangeInput label="Khoảng ngày" value={dateRange} onChange={setDateRange} />
              </FilterToolbar>

              {filterLoadError ? (
                <div className="tw-mx-6 tw-mb-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-px-4 tw-py-3 tw-text-[0.86rem] tw-font-bold tw-text-amber-800">
                  {filterLoadError}
                </div>
              ) : null}

              {loadError ? (
                <div className="tw-mx-6 tw-mb-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-px-4 tw-py-3 tw-text-[0.86rem] tw-font-bold tw-text-red-700">
                  {loadError}
                </div>
              ) : null}

              <div className="tw-px-6 tw-pb-5">
                <div className="tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100">
                  <div className="tw-overflow-x-auto">
                    <table className="table tw-m-0 tw-min-w-[1120px] [&_td]:tw-border-0 [&_td]:tw-border-t [&_td]:tw-border-solid [&_td]:tw-border-vm-slate-100 [&_td]:tw-px-4 [&_td]:tw-py-3 [&_td]:tw-align-middle [&_thead_th]:tw-border-0 [&_thead_th]:tw-bg-vm-slate-25 [&_thead_th]:tw-px-4 [&_thead_th]:tw-py-3.5 [&_thead_th]:tw-text-left [&_thead_th]:tw-text-[0.78rem] [&_thead_th]:tw-font-black [&_thead_th]:tw-text-vm-slate-700">
                      <thead>
                        <tr>
                          <th>Mã phiên</th>
                          <th>Biển số</th>
                          <th>Thẻ</th>
                          <th>Khách / Loại xe</th>
                          <th>Bãi xe / Khu vực</th>
                          <th>Giờ vào</th>
                          <th>Giờ ra</th>
                          <th>Phí</th>
                          <th>Trạng thái</th>
                          <th>Bằng chứng</th>
                          <th>Thao tác</th>
                        </tr>
                      </thead>
                      <tbody>
                        {visibleSessions.map((session) => {
                          const missing = hasMissingEvidence(session);
                          const isSelected = selectedSession?.parkingSessionId === session.parkingSessionId;
                          return (
                            <tr
                              className={cn("tw-cursor-pointer tw-transition hover:[&>td]:tw-bg-brand-50/60", isSelected ? "[&>td]:tw-bg-brand-50 tw-shadow-[inset_3px_0_0_#2563EB]" : "")}
                              key={session.parkingSessionId}
                              onClick={() => openSession(session)}
                            >
                              <td><strong className="tw-text-[0.88rem] tw-font-black tw-text-vm-primary">{sessionShortId(session)}</strong></td>
                              <td><strong className="tw-text-[0.88rem] tw-font-black tw-text-vm-slate-900">{licensePlate(session)}</strong></td>
                              <td>
                                <strong className="tw-block tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">{fallback(session.cardNumber)}</strong>
                                <span className="tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">{fallback(session.cardUid)}</span>
                              </td>
                              <td>
                                <strong className="tw-block tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">{customerTypeLabel(session)}</strong>
                                <span className="tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">{fallback(session.vehicleTypeName || session.vehicleTypeCode)}</span>
                              </td>
                              <td>
                                <strong className="tw-block tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">{fallback(session.parkingLotName || session.parkingLotCode)}</strong>
                                <span className="tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">{fallback(session.zoneName || session.zoneCode)}</span>
                              </td>
                              <td><strong className="tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">{fallback(session.checkInTime)}</strong></td>
                              <td><span className="tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-700">{fallback(session.checkOutTime)}</span></td>
                              <td><strong className="tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">{formatCurrency(session.totalPrice)}</strong></td>
                              <td><Badge tone={statusTone(session.status)}>{statusLabel(session.status)}</Badge></td>
                              <td><Badge tone={missing ? "danger" : "success"}>{missing ? "Thiếu ảnh" : "Đủ ảnh"}</Badge></td>
                              <td>
                                <button className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-vm-primary hover:tw-bg-brand-50" type="button" aria-label="Xem chi tiết" onClick={(event) => { event.stopPropagation(); openSession(session); }}>
                                  <i className="far fa-eye" />
                                </button>
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
                    totalRecords={totalRecords}
                  />
                </div>
              </div>
            </section>
          </div>
        </div>
      </section>

      <SessionDetailDrawer onClose={() => setDrawerOpen(false)} open={drawerOpen} session={selectedSession} />
    </div>
  );
}
