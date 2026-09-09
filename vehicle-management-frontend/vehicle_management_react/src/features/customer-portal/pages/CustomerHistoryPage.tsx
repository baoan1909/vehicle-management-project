import { useEffect, useMemo, useRef, useState } from "react";

import {
  getMyParkingSessions,
  type CustomerPortalParkingSession,
} from "@/features/customer-portal/api/customerPortalApi";
import type { ParkingSessionManagementFilters, ParkingSessionResponse } from "@/features/parking/api/parkingSessionApi";
import { DateRangeInput } from "@/components/ui";

import { CustomerPageHeader, CustomerPortalLayout, PortalPagination } from "./PortalShared";

type HistoryFilters = {
  fromDate: string;
  keyword: string;
  status: ParkingSessionResponse["status"] | "ALL";
  toDate: string;
};

function createInitialFilters(): HistoryFilters {
  return { fromDate: "", keyword: "", status: "ALL", toDate: "" };
}

function formatCurrency(value?: number | null) {
  const numberValue = Number(value ?? 0);
  return `${new Intl.NumberFormat("vi-VN").format(Number.isFinite(numberValue) ? numberValue : 0)}đ`;
}

function formatDate(value?: string | null) {
  if (!value) return "---";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "---" : new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
}

function formatTime(value?: string | null) {
  if (!value) return "---";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "---" : new Intl.DateTimeFormat("vi-VN", { hour: "2-digit", minute: "2-digit" }).format(date);
}

function formatDateTime(value?: string | null) {
  if (!value) return "---";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "---";
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
}

function compactCode(value?: string | null) {
  if (!value) return "--";
  return value.length > 18 ? `${value.slice(0, 8)}…${value.slice(-4)}` : value;
}

function durationLabel(session: CustomerPortalParkingSession) {
  if (!session.checkInTime) return "---";
  if (!session.checkOutTime) return "Đang gửi";
  const start = new Date(session.checkInTime).getTime();
  const end = new Date(session.checkOutTime).getTime();
  if (Number.isNaN(start) || Number.isNaN(end) || end < start) return "---";
  const totalMinutes = Math.round((end - start) / 60_000);
  const hours = Math.floor(totalMinutes / 60);
  return hours > 0 ? `${hours} giờ ${totalMinutes % 60} phút` : `${totalMinutes} phút`;
}

function statusConfig(status?: string | null) {
  if (status === "CLOSED") return { badge: "tw-bg-emerald-50 tw-text-emerald-700", dot: "tw-border-emerald-600 tw-bg-emerald-600 tw-text-white", label: "Đã hoàn tất" };
  if (status === "OPEN") return { badge: "tw-bg-blue-50 tw-text-blue-700", dot: "tw-border-blue-500 tw-bg-white tw-text-transparent", label: "Đang gửi" };
  if (status === "LOST_CARD") return { badge: "tw-bg-orange-50 tw-text-orange-700", dot: "tw-border-orange-500 tw-bg-white tw-text-transparent", label: "Mất thẻ" };
  return { badge: "tw-bg-slate-100 tw-text-slate-600", dot: "tw-border-slate-400 tw-bg-white tw-text-transparent", label: status || "--" };
}

function eventLabel(eventType?: string | null) {
  if (eventType === "CHECK_IN") return "Xe vào";
  if (eventType === "CHECK_OUT") return "Xe ra";
  return eventType || "Sự kiện";
}

function EvidenceImage({ alt, src }: { alt: string; src?: string | null }) {
  const [loadFailed, setLoadFailed] = useState(false);
  useEffect(() => setLoadFailed(false), [src]);
  if (!src || loadFailed) {
    return <div className="tw-grid tw-aspect-[1.55/1] tw-place-items-center tw-rounded-lg tw-border tw-border-dashed tw-border-slate-200 tw-bg-slate-50 tw-p-3 tw-text-center tw-text-xs tw-font-semibold tw-text-slate-400"><i className="far fa-image tw-mr-1" /> {loadFailed ? "Không thể tải ảnh" : "Chưa có ảnh"}</div>;
  }
  return <a className="tw-block tw-overflow-hidden tw-rounded-lg" href={src} rel="noreferrer" target="_blank" title="Mở ảnh kích thước đầy đủ"><img alt={alt} className="tw-aspect-[1.55/1] tw-w-full tw-object-cover tw-transition tw-duration-200 hover:tw-scale-[1.03]" loading="lazy" src={src} onError={() => setLoadFailed(true)} /></a>;
}

function buildRequestFilters(filters: HistoryFilters): ParkingSessionManagementFilters {
  return { fromDate: filters.fromDate || undefined, keyword: filters.keyword.trim() || undefined, status: filters.status === "ALL" ? undefined : filters.status, toDate: filters.toDate || undefined };
}

function toDateRangeValue(filters: Pick<HistoryFilters, "fromDate" | "toDate">) {
  return `${filters.fromDate}|${filters.toDate}`;
}

export function CustomerHistoryPage() {
  const [sessions, setSessions] = useState<CustomerPortalParkingSession[]>([]);
  const [selectedSessionId, setSelectedSessionId] = useState("");
  const [filters, setFilters] = useState<HistoryFilters>(createInitialFilters);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const requestVersion = useRef(0);

  const selectedSession = useMemo(() => sessions.find((session) => session.parkingSessionId === selectedSessionId) ?? sessions[0], [selectedSessionId, sessions]);
  const closedCount = sessions.filter((session) => session.status === "CLOSED").length;
  const openCount = sessions.filter((session) => session.status === "OPEN").length;
  const totalPrice = sessions.reduce((sum, session) => sum + Number(session.totalPrice ?? 0), 0);
  const totalPages = Math.max(1, Math.ceil(sessions.length / pageSize));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const pagedSessions = sessions.slice((safeCurrentPage - 1) * pageSize, safeCurrentPage * pageSize);
  const selectedEvents = useMemo(() => [...(selectedSession?.events ?? [])].sort((a, b) => (a.eventTime ?? "").localeCompare(b.eventTime ?? "")), [selectedSession]);
  const checkInEvent = selectedEvents.find((event) => event.eventType === "CHECK_IN");
  const checkOutEvent = selectedEvents.find((event) => event.eventType === "CHECK_OUT");

  async function loadSessions(nextFilters = filters) {
    const version = ++requestVersion.current;
    setLoading(true);
    setError("");
    try {
      if (nextFilters.fromDate && nextFilters.toDate && nextFilters.fromDate > nextFilters.toDate) {
        throw new Error("Ngày kết thúc phải bằng hoặc sau ngày bắt đầu. Vui lòng kiểm tra bộ lọc ngày.");
      }
      const nextSessions = await getMyParkingSessions(buildRequestFilters(nextFilters));
      if (version !== requestVersion.current) return;
      setSessions(nextSessions);
      setCurrentPage(1);
      setSelectedSessionId((current) => nextSessions.some((session) => session.parkingSessionId === current) ? current : nextSessions[0]?.parkingSessionId ?? "");
    } catch (requestError) {
      if (version !== requestVersion.current) return;
      setSessions([]);
      setError(requestError instanceof Error ? requestError.message : "Không thể tải lịch sử gửi xe.");
    } finally {
      if (version === requestVersion.current) setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void loadSessions(filters), 300);
    return () => { window.clearTimeout(timer); requestVersion.current += 1; };
  }, [filters]);

  const stats = [
    ["far fa-clipboard", "Tổng phiên gửi", String(sessions.length), "tw-bg-blue-50 tw-text-blue-600"],
    ["fas fa-car", "Đang gửi", String(openCount), "tw-bg-blue-50 tw-text-blue-600"],
    ["fas fa-check-circle", "Đã hoàn tất", String(closedCount), "tw-bg-emerald-50 tw-text-emerald-600"],
    ["fas fa-wallet", "Tổng phí theo bộ lọc", formatCurrency(totalPrice), "tw-bg-amber-50 tw-text-amber-600"],
  ];

  return (
    <CustomerPortalLayout>
      <CustomerPageHeader eyebrow="NHẬT KÝ RA / VÀO" subtitle="Mỗi lượt xe được ghi nhận rõ ràng từ lúc vào đến khi rời bãi." title="Lịch sử gửi xe" />

      {error ? <div className="tw-mb-4 tw-flex tw-items-start tw-gap-2 tw-rounded-xl tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-px-4 tw-py-3 tw-text-sm tw-font-semibold tw-text-red-700" role="alert"><i className="fas fa-exclamation-circle tw-mt-0.5" /> {error}</div> : null}

      <section className="tw-mb-4 tw-grid tw-grid-cols-4 tw-overflow-hidden tw-rounded-2xl tw-border tw-border-solid tw-border-slate-100 tw-bg-white tw-shadow-[0_10px_28px_rgba(15,39,82,0.06)] max-[1000px]:tw-grid-cols-2 max-[640px]:tw-grid-cols-1">
        {stats.map(([icon, label, value, tone], index) => (
          <div className="tw-relative tw-grid tw-min-h-[116px] tw-min-w-0 tw-place-items-center tw-border-0 tw-border-r tw-border-solid tw-border-slate-100 tw-px-5 tw-py-4 last:tw-border-0 max-[1000px]:tw-border-b max-[1000px]:tw-border-r-0" key={label}>
            <div className="tw-flex tw-min-w-0 tw-items-center tw-justify-center tw-gap-4">
              <span className={`tw-grid tw-h-14 tw-w-14 tw-shrink-0 tw-place-items-center tw-rounded-full tw-text-2xl ${tone}`}><i className={icon} /></span>
              <div className="tw-min-w-0">
                <strong className="tw-block tw-break-words tw-text-[clamp(1.25rem,2vw,1.8rem)] tw-font-bold tw-leading-none tw-text-slate-950">{value}</strong>
                <span className="tw-mt-2 tw-block tw-text-sm tw-font-semibold tw-text-slate-600">{label}</span>
              </div>
            </div>
            {index < stats.length - 1 ? (
              <span aria-hidden="true" className="tw-pointer-events-none tw-absolute tw-right-[-3.5rem] tw-top-1/2 tw-z-10 tw-h-3 tw-w-28 [transform:translateY(-50%)] max-[1100px]:tw-hidden">
                <i className="tw-absolute tw-left-0 tw-right-0 tw-top-1/2 tw-h-px tw-bg-blue-500 [transform:translateY(-50%)]" />
                <i className="tw-absolute tw-left-0 tw-top-1/2 tw-h-2 tw-w-2 tw-rounded-full tw-border tw-border-solid tw-border-blue-500 tw-bg-white [transform:translateY(-50%)]" />
                <i className="tw-absolute tw-right-0 tw-top-1/2 tw-h-2 tw-w-2 tw-rounded-full tw-border tw-border-solid tw-border-blue-500 tw-bg-white [transform:translateY(-50%)]" />
              </span>
            ) : null}
          </div>
        ))}
      </section>

      <section className="tw-mb-4 tw-grid tw-grid-cols-[minmax(0,1.45fr)_minmax(0,1.65fr)_minmax(0,1.15fr)_auto] tw-gap-3 tw-rounded-2xl tw-border tw-border-solid tw-border-slate-100 tw-bg-white tw-p-4 tw-shadow-[0_10px_28px_rgba(15,39,82,0.05)] max-[1000px]:tw-grid-cols-2 max-[640px]:tw-grid-cols-1">
        <DateRangeInput ariaLabel="Khoảng ngày lọc lịch sử gửi xe" label="Khoảng ngày" value={toDateRangeValue(filters)} onChange={(value) => { const [fromDate = "", toDate = ""] = value.split("|", 2); setFilters((current) => ({ ...current, fromDate, toDate })); }} />
        <label className="tw-grid tw-gap-1.5 tw-text-xs tw-font-bold tw-text-slate-600">Tìm kiếm<span className="tw-relative"><input className="tw-h-11 tw-w-full tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-px-3 tw-pr-10 tw-text-sm tw-font-semibold tw-text-slate-800" placeholder="Biển số, mã phiên, thẻ..." value={filters.keyword} onChange={(event) => setFilters((current) => ({ ...current, keyword: event.target.value }))} /><i className="fas fa-search tw-pointer-events-none tw-absolute tw-right-3 tw-top-1/2 [transform:translateY(-50%)] tw-text-slate-400" /></span></label>
        <label className="tw-grid tw-gap-1.5 tw-text-xs tw-font-bold tw-text-slate-600">Trạng thái<select className="tw-h-11 tw-w-full tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-px-3 tw-text-sm tw-font-semibold tw-text-slate-800" value={filters.status} onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value as HistoryFilters["status"] }))}><option value="ALL">Tất cả trạng thái</option><option value="OPEN">Đang gửi</option><option value="CLOSED">Đã hoàn tất</option><option value="LOST_CARD">Mất thẻ</option></select></label>
        <button className="tw-mt-auto tw-flex tw-h-11 tw-items-center tw-justify-center tw-gap-2 tw-rounded-lg tw-border tw-border-solid tw-border-blue-600 tw-bg-white tw-px-4 tw-text-sm tw-font-semibold tw-text-blue-700 hover:tw-bg-blue-50 disabled:tw-opacity-50" disabled={loading} type="button" onClick={() => setFilters(createInitialFilters())}><i className="fas fa-sync-alt" /> Xóa lọc</button>
      </section>

      <div className="tw-grid tw-grid-cols-[minmax(0,1.55fr)_minmax(0,1fr)] tw-gap-3 max-[1000px]:tw-grid-cols-1">
        <section className="tw-min-w-0 tw-rounded-xl tw-border tw-border-solid tw-border-slate-100 tw-bg-white tw-p-3 tw-shadow-[0_10px_28px_rgba(15,39,82,0.05)]">
          <h2 className="tw-m-2 tw-mb-3 tw-text-lg tw-font-bold tw-text-slate-900">Dòng thời gian gửi xe</h2>
          <div className="tw-overflow-x-auto tw-px-1 tw-pb-1">
            <div className="tw-grid tw-min-w-[580px] tw-gap-2">
            {pagedSessions.map((session) => {
              const status = statusConfig(session.status);
              const selected = selectedSession?.parkingSessionId === session.parkingSessionId;
              return <div aria-label={`Xem chi tiết phiên ${compactCode(session.parkingSessionId)}`} className={`!tw-grid tw-w-full tw-cursor-pointer !tw-grid-cols-[22px_minmax(90px,1fr)_minmax(74px,.75fr)_minmax(78px,.8fr)_minmax(74px,.75fr)_minmax(85px,.85fr)_12px] !tw-items-center tw-gap-2 tw-rounded-xl !tw-border tw-border-solid tw-p-3 tw-text-left tw-transition focus-visible:tw-outline focus-visible:tw-outline-2 focus-visible:tw-outline-offset-2 focus-visible:tw-outline-blue-600 ${selected ? "!tw-border-blue-600 !tw-bg-blue-50/60 tw-shadow-[0_4px_12px_rgba(26,103,224,0.1)]" : "!tw-border-slate-100 !tw-bg-white hover:!tw-border-blue-200 hover:!tw-bg-slate-50"}`} key={session.parkingSessionId} role="button" tabIndex={0} onClick={() => setSelectedSessionId(session.parkingSessionId)} onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === " ") {
                  event.preventDefault();
                  setSelectedSessionId(session.parkingSessionId);
                }
              }}>
                <span className={`tw-grid tw-h-5 tw-w-5 tw-place-items-center tw-rounded-full tw-border-2 tw-border-solid tw-text-[0.65rem] ${selected ? "tw-border-blue-600 tw-bg-blue-600 tw-text-white" : "tw-border-slate-300 tw-bg-white tw-text-transparent"}`}><i className="fas fa-check" /></span>
                <span className="tw-min-w-0 tw-border-0 tw-border-r tw-border-solid tw-border-slate-200 tw-pr-2"><strong className="tw-block tw-truncate tw-text-xs tw-font-bold tw-text-blue-700">{compactCode(session.parkingSessionId)}</strong><small className="tw-mt-1 tw-block tw-text-xs tw-font-semibold tw-text-slate-500">Mã phiên</small></span>
                <span className="tw-block tw-min-w-0"><b className="tw-block tw-text-[0.68rem] tw-font-bold tw-text-blue-600">VÀO</b><strong className="tw-block tw-text-lg tw-font-bold tw-leading-tight tw-text-slate-900">{formatTime(session.checkInTime)}</strong><small className="tw-block tw-text-xs tw-font-semibold tw-text-slate-500">{formatDate(session.checkInTime)}</small><em className="tw-mt-1 tw-inline-block tw-rounded tw-bg-slate-100 tw-px-1.5 tw-py-0.5 tw-text-[0.68rem] tw-not-italic tw-font-bold tw-text-slate-700">{session.licensePlateIn || "--"}</em></span>
                <span className="tw-block tw-min-w-0"><span className="tw-relative tw-mb-3 tw-block tw-h-2 tw-w-full"><i className="tw-absolute tw-inset-y-0 tw-left-0 tw-z-10 tw-my-auto tw-h-2 tw-w-2 tw-rounded-full tw-bg-blue-600" /><i className="tw-absolute tw-inset-y-0 tw-left-1 tw-right-1 tw-my-auto tw-h-px tw-bg-blue-500" /><i className="tw-absolute tw-inset-y-0 tw-right-0 tw-z-10 tw-my-auto tw-h-2 tw-w-2 tw-rounded-full tw-border-2 tw-border-solid tw-border-blue-500 tw-bg-white" /></span><small className="tw-block tw-text-center tw-text-xs tw-font-semibold tw-text-slate-500"><i className="far fa-clock tw-mr-1" />{durationLabel(session)}</small></span>
                <span className="tw-block tw-min-w-0"><b className="tw-block tw-text-[0.68rem] tw-font-bold tw-text-blue-600">RA</b><strong className="tw-block tw-text-lg tw-font-bold tw-leading-tight tw-text-slate-900">{formatTime(session.checkOutTime)}</strong><small className="tw-block tw-text-xs tw-font-semibold tw-text-slate-500">{formatDate(session.checkOutTime)}</small><em className="tw-mt-1 tw-inline-block tw-rounded tw-bg-slate-100 tw-px-1.5 tw-py-0.5 tw-text-[0.68rem] tw-not-italic tw-font-bold tw-text-slate-700">{session.licensePlateOut || session.licensePlateIn || "--"}</em></span>
                <span className="tw-grid tw-justify-items-center tw-gap-3"><span className={`tw-rounded-full tw-px-2.5 tw-py-1 tw-text-xs tw-font-semibold ${status.badge}`}>{status.label}</span><strong className="tw-text-base tw-font-bold tw-text-slate-900">{session.status === "OPEN" ? "---" : formatCurrency(session.totalPrice)}</strong></span>
                <i aria-hidden="true" className="fas fa-chevron-right tw-text-xs tw-text-slate-400" />
              </div>;
            })}
              {!loading && sessions.length === 0 ? <div className="tw-py-12 tw-text-center tw-text-sm tw-font-semibold tw-text-slate-500">Chưa có phiên gửi xe phù hợp với bộ lọc.</div> : null}
              {loading ? <div className="tw-py-12 tw-text-center tw-text-sm tw-font-semibold tw-text-slate-500"><i className="fas fa-spinner tw-mr-2 tw-animate-spin" />Đang tải lịch sử...</div> : null}
            </div>
          </div>
          <PortalPagination currentPage={safeCurrentPage} pageSize={pageSize} totalRecords={sessions.length} onPageChange={setCurrentPage} onPageSizeChange={(size) => { setPageSize(size); setCurrentPage(1); }} />
        </section>

        <aside className="tw-min-w-0 tw-overflow-hidden tw-rounded-xl tw-border tw-border-solid tw-border-slate-100 tw-bg-white tw-shadow-[0_10px_28px_rgba(15,39,82,0.05)]">
          {selectedSession ? <>
            <h2 className="tw-m-0 tw-p-4 tw-text-base tw-font-semibold tw-text-slate-900">Chi tiết phiên {compactCode(selectedSession.parkingSessionId)}</h2>
            <div className="tw-mx-2 tw-grid tw-grid-cols-[minmax(0,1fr)_auto] tw-items-center tw-gap-x-3 tw-gap-y-1 tw-rounded-t-xl tw-bg-[linear-gradient(115deg,#031633,#051e46)] tw-px-4 tw-py-3 tw-text-white">
              <strong className="tw-text-[1.45rem] tw-font-semibold">{selectedSession.licensePlateIn || selectedSession.licensePlateOut || "--"}</strong>
              <span className="tw-row-span-2 tw-text-right tw-text-xs tw-text-blue-100">Tổng phí<strong className="tw-mt-1 tw-block tw-text-xl tw-font-semibold tw-text-white">{selectedSession.status === "OPEN" ? "---" : formatCurrency(selectedSession.totalPrice)}</strong></span>
              <span className={`tw-w-fit tw-rounded-full tw-px-3 tw-py-1 tw-text-xs tw-font-medium ${statusConfig(selectedSession.status).badge}`}>{statusConfig(selectedSession.status).label}</span>
            </div>
            <div className="tw-p-4"><div className="tw-grid tw-grid-cols-4 max-[1200px]:tw-grid-cols-2 tw-gap-x-4 tw-gap-y-3 tw-border-0 tw-border-b tw-border-solid tw-border-slate-100 tw-pb-3">{[["Thẻ", selectedSession.cardNumber], ["Loại xe", selectedSession.vehicleTypeName ?? selectedSession.vehicleTypeCode], ["Khu vực", selectedSession.zoneName ?? selectedSession.zoneCode], ["Bãi xe", selectedSession.parkingLotName ?? selectedSession.parkingLotCode]].map(([label, value]) => <div key={label}><span className="tw-block tw-text-xs tw-font-bold tw-text-slate-500">{label}</span><strong className="tw-mt-1 tw-block tw-text-sm tw-font-semibold tw-text-slate-800">{value || "--"}</strong></div>)}</div>
              <h3 className="tw-m-0 tw-mt-3 tw-text-base tw-font-bold tw-text-slate-900">Sự kiện vào / ra</h3>
              <div className="tw-mt-3 tw-grid tw-gap-0">{selectedEvents.map((event, index) => <div className="tw-grid tw-grid-cols-[22px_minmax(0,1fr)_minmax(76px,.55fr)] tw-gap-3" key={event.parkingEventId}><div className="tw-relative tw-flex tw-justify-center">{index < selectedEvents.length - 1 ? <i aria-hidden="true" className="tw-absolute tw-bottom-[-8px] tw-left-1/2 tw-top-2 tw-w-px [transform:translateX(-50%)] tw-bg-blue-500" /> : null}<span aria-hidden="true" className={`tw-relative tw-z-10 tw-mt-1 tw-h-3 tw-w-3 tw-shrink-0 tw-rounded-full tw-border-2 tw-border-solid tw-border-blue-600 ${event.eventType === "CHECK_IN" ? "tw-bg-blue-600" : "tw-bg-white"}`} /></div><div className="tw-pb-4"><strong className="tw-block tw-text-sm tw-font-semibold tw-text-blue-700">{eventLabel(event.eventType)}</strong><span className="tw-mt-1 tw-block tw-text-xs tw-font-semibold tw-text-slate-600">{formatDateTime(event.eventTime)}</span></div><div className="tw-pb-4 tw-text-xs tw-font-semibold tw-leading-5 tw-text-slate-600"><strong className="tw-block tw-text-slate-800">{event.laneName || "--"}</strong>Biển số: {event.licensePlateDetected || "--"}</div></div>)}{selectedEvents.length === 0 ? <p className="tw-mt-3 tw-text-sm tw-font-semibold tw-text-slate-500">Chưa có sự kiện vào/ra.</p> : null}</div>
              <div className="tw-mt-2 tw-grid tw-grid-cols-2 tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-slate-100 tw-pt-4"><div><strong className="tw-mb-2 tw-block tw-text-xs tw-font-bold tw-text-slate-700">Ảnh xe vào</strong><EvidenceImage alt="Ảnh xe vào" src={checkInEvent?.licensePlateImagePath} /></div><div><strong className="tw-mb-2 tw-block tw-text-xs tw-font-bold tw-text-slate-700">Ảnh xe ra</strong><EvidenceImage alt="Ảnh xe ra" src={checkOutEvent?.licensePlateImagePath} /></div></div>
            </div>
          </> : <div className="tw-grid tw-min-h-[330px] tw-place-items-center tw-p-8 tw-text-center tw-text-slate-500"><div><i className="far fa-hand-pointer tw-text-3xl tw-text-blue-500" /><strong className="tw-mt-3 tw-block tw-text-base tw-text-slate-800">Chưa có phiên gửi xe</strong><p className="tw-mt-2 tw-text-sm tw-font-semibold">Chọn khoảng thời gian khác để xem lịch sử.</p></div></div>}
        </aside>
      </div>

      <div className="tw-mt-4 tw-flex tw-items-center tw-gap-3 tw-rounded-xl tw-bg-blue-50 tw-px-5 tw-py-4 tw-text-sm tw-font-semibold tw-text-slate-600"><i className="fas fa-info-circle tw-text-xl tw-text-blue-600" />Lịch sử bao gồm các phiên gửi xe liên kết với các phương tiện trong tài khoản khách hàng hiện tại.</div>
    </CustomerPortalLayout>
  );
}
