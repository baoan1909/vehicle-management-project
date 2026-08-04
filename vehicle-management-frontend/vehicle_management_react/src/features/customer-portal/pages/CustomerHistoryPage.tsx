import { useEffect, useMemo, useState } from "react";

import {
  getMyParkingSessions,
  type CustomerPortalParkingSession,
} from "@/features/customer-portal/api/customerPortalApi";
import type { ParkingSessionManagementFilters, ParkingSessionResponse } from "@/features/parking/api/parkingSessionApi";

import { CustomerPageHeader, CustomerPortalLayout, Field, PaginationLite, StatCard, StatusPill } from "./PortalShared";

type StatusTone = "green" | "blue" | "orange" | "red" | "gray" | "purple";

type HistoryFilters = {
  fromDate: string;
  keyword: string;
  status: ParkingSessionResponse["status"] | "ALL";
  toDate: string;
};

function formatDateInput(date: Date) {
  const timezoneOffset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - timezoneOffset).toISOString().slice(0, 10);
}

function createInitialFilters(): HistoryFilters {
  const currentDate = new Date();
  const thirtyDaysAgo = new Date(currentDate);
  thirtyDaysAgo.setDate(currentDate.getDate() - 30);

  return {
    fromDate: formatDateInput(thirtyDaysAgo),
    keyword: "",
    status: "ALL",
    toDate: formatDateInput(currentDate),
  };
}

function formatCurrency(value?: number | null) {
  const numberValue = Number(value ?? 0);
  return `${new Intl.NumberFormat("vi-VN").format(Number.isFinite(numberValue) ? numberValue : 0)} đ`;
}

function compactCode(value?: string | null) {
  if (!value) return "--";
  return value.length > 18 ? `${value.slice(0, 8)}...${value.slice(-6)}` : value;
}

function formatDateTime(value?: string | null) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--";
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
}

function statusTone(status?: string | null): StatusTone {
  if (status === "OPEN") return "blue";
  if (status === "CLOSED") return "green";
  if (status === "LOST_CARD") return "orange";
  return "gray";
}

function statusLabel(status?: string | null) {
  if (status === "OPEN") return "Đang gửi";
  if (status === "CLOSED") return "Đã hoàn tất";
  if (status === "LOST_CARD") return "Mất thẻ";
  return status || "--";
}

function eventLabel(eventType?: string | null) {
  if (eventType === "CHECK_IN") return "Vào bãi";
  if (eventType === "CHECK_OUT") return "Ra bãi";
  return eventType || "--";
}

function EvidenceImage({ alt, src }: { alt: string; src?: string | null }) {
  const [loadFailed, setLoadFailed] = useState(false);

  useEffect(() => {
    setLoadFailed(false);
  }, [src]);

  if (!src || loadFailed) {
    return (
      <span className="vm-evidence-empty">
        <i className="far fa-image" />
        {loadFailed ? "Không thể tải ảnh" : "Chưa có ảnh"}
      </span>
    );
  }

  return (
    <a
      className="vm-evidence-image-link"
      href={src}
      target="_blank"
      rel="noreferrer"
      title="Mở ảnh kích thước đầy đủ"
    >
      <img src={src} alt={alt} loading="lazy" onError={() => setLoadFailed(true)} />
    </a>
  );
}

function buildRequestFilters(filters: HistoryFilters): ParkingSessionManagementFilters {
  return {
    fromDate: filters.fromDate || undefined,
    keyword: filters.keyword.trim() || undefined,
    status: filters.status === "ALL" ? undefined : filters.status,
    toDate: filters.toDate || undefined,
  };
}

export function CustomerHistoryPage() {
  const [sessions, setSessions] = useState<CustomerPortalParkingSession[]>([]);
  const [selectedSessionId, setSelectedSessionId] = useState("");
  const [filters, setFilters] = useState<HistoryFilters>(createInitialFilters);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const selectedSession = useMemo(
    () => sessions.find((session) => session.parkingSessionId === selectedSessionId),
    [selectedSessionId, sessions],
  );

  const closedCount = sessions.filter((session) => session.status === "CLOSED").length;
  const openCount = sessions.filter((session) => session.status === "OPEN").length;
  const monthTotal = sessions.reduce((sum, session) => sum + Number(session.totalPrice ?? 0), 0);
  const totalPages = Math.max(1, Math.ceil(sessions.length / pageSize));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const pagedSessions = sessions.slice((safeCurrentPage - 1) * pageSize, safeCurrentPage * pageSize);

  async function loadSessions(nextFilters = filters) {
    setLoading(true);
    setError("");
    try {
      const nextSessions = await getMyParkingSessions(buildRequestFilters(nextFilters));
      setSessions(nextSessions);
      setCurrentPage(1);
      setSelectedSessionId((current) => (
        nextSessions.some((session) => session.parkingSessionId === current)
          ? current
          : ""
      ));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể tải lịch sử gửi xe.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadSessions(filters);
    }, 300);

    return () => window.clearTimeout(timer);
  }, [filters]);

  const handleResetFilters = () => {
    setFilters(createInitialFilters());
  };

  return (
    <CustomerPortalLayout>
      <CustomerPageHeader
        title="Lịch sử gửi xe"
        subtitle="Theo dõi các phiên gửi xe và sự kiện vào ra của bạn"
      />

      {error ? <div className="vm-info-note tw-bg-red-50 tw-text-red-600"><i className="fas fa-exclamation-circle" /> {error}</div> : null}

      <div className="vm-stat-grid vm-stat-grid-four">
        <StatCard icon="far fa-clipboard" label="Tổng phiên gửi" value={String(sessions.length)} note="phiên" />
        <StatCard icon="fas fa-car" label="Đang gửi" value={String(openCount)} note={<StatusPill tone="blue">Đang gửi</StatusPill>} />
        <StatCard icon="far fa-check-circle" label="Đã hoàn tất" value={String(closedCount)} note={<StatusPill>Đã hoàn tất</StatusPill>} tone="green" />
        <StatCard icon="fas fa-wallet" label="Tổng phí theo bộ lọc" value={formatCurrency(monthTotal)} tone="purple" />
      </div>

      <section className="vm-customer-card vm-filter-row-card">
        <Field label="Từ ngày"><input type="date" value={filters.fromDate} onChange={(event) => setFilters((current) => ({ ...current, fromDate: event.target.value }))} /></Field>
        <Field label="Đến ngày"><input type="date" value={filters.toDate} onChange={(event) => setFilters((current) => ({ ...current, toDate: event.target.value }))} /></Field>
        <Field label="Tìm kiếm"><input value={filters.keyword} onChange={(event) => setFilters((current) => ({ ...current, keyword: event.target.value }))} placeholder="Biển số, mã phiên, thẻ..." /></Field>
        <Field label="Trạng thái">
          <select value={filters.status} onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value as HistoryFilters["status"] }))}>
            <option value="ALL">Tất cả</option>
            <option value="OPEN">Đang gửi</option>
            <option value="CLOSED">Đã hoàn tất</option>
            <option value="LOST_CARD">Mất thẻ</option>
          </select>
        </Field>
        <Field label="Thao tác">
          <button className="vm-outline-btn" type="button" onClick={handleResetFilters} disabled={loading}><i className="fas fa-redo" /> Xóa lọc</button>
        </Field>
      </section>

      <div className="vm-history-layout">
        <section className="vm-customer-card vm-table-card vm-history-table-card">
          <h2>Danh sách phiên gửi xe</h2>
          <table className="vm-customer-table">
            <thead><tr><th>Mã phiên</th><th>Biển số vào</th><th>Biển số ra</th><th>Thời gian vào</th><th>Thời gian ra</th><th>Trạng thái</th><th>Tổng phí</th></tr></thead>
            <tbody>
              {pagedSessions.map((session) => (
                <tr
                  key={session.parkingSessionId}
                  className={`vm-interactive-row${selectedSessionId === session.parkingSessionId ? " vm-selected-row" : ""}`}
                  role="button"
                  tabIndex={0}
                  aria-label={`Xem chi tiết phiên ${compactCode(session.parkingSessionId)}`}
                  aria-pressed={selectedSessionId === session.parkingSessionId}
                  title="Nhấn để xem chi tiết phiên gửi xe"
                  onClick={() => setSelectedSessionId(session.parkingSessionId)}
                  onKeyDown={(event) => {
                    if (event.target !== event.currentTarget) return;
                    if (event.key === "Enter" || event.key === " ") {
                      event.preventDefault();
                      setSelectedSessionId(session.parkingSessionId);
                    }
                  }}
                >
                  <td title={session.parkingSessionId}>{compactCode(session.parkingSessionId)}</td>
                  <td>{session.licensePlateIn || "--"}</td>
                  <td>{session.licensePlateOut || "--"}</td>
                  <td>{formatDateTime(session.checkInTime)}</td>
                  <td>{formatDateTime(session.checkOutTime)}</td>
                  <td><StatusPill tone={statusTone(session.status)}>{statusLabel(session.status)}</StatusPill></td>
                  <td>{formatCurrency(session.totalPrice)}</td>
                </tr>
              ))}
              {!loading && sessions.length === 0 ? <tr><td colSpan={7}>Chưa có phiên gửi xe phù hợp.</td></tr> : null}
              {loading ? <tr><td colSpan={7}>Đang tải dữ liệu...</td></tr> : null}
            </tbody>
          </table>
          <PaginationLite
            currentPage={safeCurrentPage}
            pageSize={pageSize}
            totalRecords={sessions.length}
            onPageChange={setCurrentPage}
            onPageSizeChange={setPageSize}
          />
        </section>

        <aside className="vm-customer-card vm-session-detail">
          <h2>{selectedSession ? `Chi tiết phiên ${compactCode(selectedSession.parkingSessionId)}` : "Chi tiết phiên gửi xe"}</h2>
          {selectedSession ? (
            <>
              <dl className="vm-info-list">
                <dt>Thẻ:</dt><dd>{selectedSession.cardNumber ?? "--"}</dd>
                <dt>Loại xe:</dt><dd>{selectedSession.vehicleTypeName ?? selectedSession.vehicleTypeCode ?? "--"}</dd>
                <dt>Khu vực:</dt><dd>{selectedSession.zoneName ?? selectedSession.zoneCode ?? "--"}</dd>
                <dt>Bãi xe:</dt><dd>{selectedSession.parkingLotName ?? selectedSession.parkingLotCode ?? "--"}</dd>
                <dt>Tổng phí:</dt><dd className="vm-blue-text">{formatCurrency(selectedSession.totalPrice)}</dd>
              </dl>
              <h3>Sự kiện vào/ra</h3>
              <div className="vm-event-list">
                {(selectedSession.events ?? []).map((event) => (
                  <div key={event.parkingEventId}>
                    <span><i className={event.eventType === "CHECK_OUT" ? "fas fa-sign-out-alt" : "fas fa-sign-in-alt"} /></span>
                    <b>{eventLabel(event.eventType)}</b>
                    <em>{formatDateTime(event.eventTime)}<br />{event.licensePlateDetected || "--"}{event.laneName ? ` - ${event.laneName}` : ""}</em>
                  </div>
                ))}
                {(selectedSession.events ?? []).length === 0 ? <div>Chưa có sự kiện vào/ra.</div> : null}
              </div>
              <div className="vm-image-row">
                {(selectedSession.events ?? [])
                  .filter((event) => event.eventType === "CHECK_IN" || event.eventType === "CHECK_OUT")
                  .slice(0, 2)
                  .map((event) => (
                  <div key={`${event.parkingEventId}-image`}>
                    <strong>{eventLabel(event.eventType)}</strong>
                    <EvidenceImage
                      alt={`Ảnh biển số khi ${eventLabel(event.eventType).toLocaleLowerCase("vi-VN")}`}
                      src={event.licensePlateImagePath}
                    />
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div className="vm-session-empty">
              <i className="far fa-hand-pointer" />
              <strong>Chưa chọn phiên gửi xe</strong>
              <span>Chọn một dòng trong bảng hoặc nhấn nút chi tiết để xem dữ liệu.</span>
            </div>
          )}
        </aside>
      </div>

      <div className="vm-info-note"><i className="fas fa-info-circle" /> Dữ liệu lịch sử chỉ lấy các phiên gắn với xe thuộc tài khoản customer hiện tại.</div>
    </CustomerPortalLayout>
  );
}
