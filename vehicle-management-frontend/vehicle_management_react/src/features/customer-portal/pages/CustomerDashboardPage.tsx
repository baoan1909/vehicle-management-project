import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import {
  getCustomerPortalLookups,
  getCustomerPortalProfile,
  getMyCustomerVehicles,
  getMySubscriptions,
  type CustomerPortalProfile,
  type CustomerPortalSubscription,
  type CustomerPortalTicketType,
  type CustomerPortalVehicle,
  type CustomerPortalVehicleType,
} from "@/features/customer-portal/api/customerPortalApi";

import { CustomerPortalLayout, StatusPill } from "./PortalShared";

type StatusTone = "green" | "blue" | "orange" | "red" | "gray" | "purple";

function formatDate(value?: string | null) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--";
  return new Intl.DateTimeFormat("vi-VN").format(date);
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

function statusLabel(status?: string | null) {
  if (status === "ACTIVE") return "Đang hoạt động";
  if (status === "PENDING") return "Chờ duyệt";
  if (status === "PENDING_PAYMENT") return "Chờ thanh toán";
  if (status === "PENDING_CARD") return "Chờ gán thẻ";
  if (status === "EXPIRED") return "Hết hạn";
  if (status === "CANCELLED") return "Đã hủy";
  if (status === "REJECTED") return "Từ chối";
  return status || "--";
}

function approvalLabel(status?: string | null) {
  if (status === "APPROVED") return "Đã duyệt";
  if (status === "PENDING") return "Chờ duyệt";
  if (status === "REJECTED") return "Từ chối";
  if (status === "SUSPENDED") return "Tạm khóa";
  return status || "--";
}

function approvalTone(status?: string | null): StatusTone {
  if (status === "APPROVED") return "green";
  if (status === "PENDING") return "orange";
  if (status === "REJECTED" || status === "SUSPENDED") return "red";
  return "gray";
}

function StatusBadge({ label, tone, checked = false }: { checked?: boolean; label: string; tone: StatusTone }) {
  return (
    <StatusPill tone={tone}>
      {checked ? <i className="fas fa-check-circle" /> : null}
      {label}
    </StatusPill>
  );
}

function formatCurrency(value?: number | string | null) {
  const numberValue = Number(value ?? 0);
  return `${new Intl.NumberFormat("vi-VN").format(Number.isFinite(numberValue) ? numberValue : 0)} đ`;
}

function vehicleLabel(vehicle?: CustomerPortalVehicle, vehicleTypeById?: Map<string, CustomerPortalVehicleType>) {
  if (!vehicle) return "--";
  const typeName = vehicle.vehicleTypeId ? vehicleTypeById?.get(vehicle.vehicleTypeId)?.name : "";
  return [vehicle.licensePlate, vehicle.brand, typeName].filter(Boolean).join(" - ");
}

function vehicleDescription(vehicle?: CustomerPortalVehicle, vehicleTypeById?: Map<string, CustomerPortalVehicleType>) {
  if (!vehicle) return "--";
  const typeName = vehicle.vehicleTypeId ? vehicleTypeById?.get(vehicle.vehicleTypeId)?.name : "";
  return [vehicle.brand, typeName].filter(Boolean).join(" - ") || "--";
}

function shortCode(value?: string | null) {
  if (!value) return "--";
  return value.length > 14 ? `${value.slice(0, 8)}...${value.slice(-4)}` : value;
}

export function CustomerDashboardPage() {
  const navigate = useNavigate();
  const [profile, setProfile] = useState<CustomerPortalProfile | null>(null);
  const [vehicles, setVehicles] = useState<CustomerPortalVehicle[]>([]);
  const [subscriptions, setSubscriptions] = useState<CustomerPortalSubscription[]>([]);
  const [ticketTypes, setTicketTypes] = useState<CustomerPortalTicketType[]>([]);
  const [vehicleTypes, setVehicleTypes] = useState<CustomerPortalVehicleType[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let ignore = false;

    async function loadData() {
      setLoading(true);
      setError("");
      try {
        const nextProfile = await getCustomerPortalProfile();
        const [nextVehicles, nextSubscriptions, lookups] = await Promise.all([
          getMyCustomerVehicles(nextProfile),
          getMySubscriptions(nextProfile),
          getCustomerPortalLookups(),
        ]);
        if (ignore) return;
        setProfile(nextProfile);
        setVehicles(nextVehicles);
        setSubscriptions(nextSubscriptions);
        setTicketTypes(lookups.ticketTypes);
        setVehicleTypes(lookups.vehicleTypes);
      } catch (requestError) {
        if (!ignore) setError(requestError instanceof Error ? requestError.message : "Không thể tải tổng quan khách hàng.");
      } finally {
        if (!ignore) setLoading(false);
      }
    }

    void loadData();
    return () => {
      ignore = true;
    };
  }, []);

  const vehicleById = useMemo(() => new Map(vehicles.map((vehicle) => [vehicle.customerVehicleId, vehicle])), [vehicles]);
  const ticketTypeById = useMemo(() => new Map(ticketTypes.map((ticketType) => [ticketType.ticketTypeId, ticketType])), [ticketTypes]);
  const vehicleTypeById = useMemo(() => new Map(vehicleTypes.map((type) => [type.vehicleTypeId, type])), [vehicleTypes]);
  const activeSubscription = subscriptions.find((subscription) => subscription.status === "ACTIVE");
  const pendingSubscriptions = subscriptions.filter((subscription) => subscription.status?.startsWith("PENDING"));
  const defaultVehicle = vehicles.find((vehicle) => vehicle.isDefault);
  const recentSubscriptions = subscriptions.slice(0, 5);
  const displayName = profile?.profile?.fullName ?? profile?.account?.username ?? "Khách hàng";
  const approvalStatus = profile?.customer?.customerApprovalStatus;
  const activeVehicles = vehicles.filter((vehicle) => vehicle.status === "ACTIVE").length;
  const inactiveVehicles = vehicles.filter((vehicle) => vehicle.status !== "ACTIVE").length;

  return (
    <CustomerPortalLayout>
      {error ? <div className="vm-info-note tw-bg-red-50 tw-text-red-600"><i className="fas fa-exclamation-circle" /> {error}</div> : null}
      {loading ? <div className="vm-info-note"><i className="fas fa-spinner fa-spin" /> Đang tải dữ liệu khách hàng...</div> : null}

      <section className="vm-customer-hero-card">
        <span className="vm-customer-hero-avatar"><i className="far fa-user" /></span>
        <div className="vm-customer-hero-copy">
          <h1>Xin chào, {displayName}</h1>
          <p>Theo dõi vé tháng, phương tiện và các yêu cầu hỗ trợ của bạn.</p>
          <div className="vm-customer-hero-actions">
            <Link to="/customer/subscriptions"><i className="far fa-calendar-plus" /> Đăng ký vé</Link>
          </div>
        </div>
      </section>

      <div className="vm-dashboard-cards vm-dashboard-cards-polished">
        <article className="vm-customer-card vm-dashboard-summary-card">
          <div className="vm-dashboard-card-title">
            <span className="vm-dashboard-card-icon"><i className="far fa-calendar-check" /></span>
            <h2>Vé tháng hiện tại</h2>
            <StatusBadge
              checked={activeSubscription?.status === "ACTIVE"}
              label={statusLabel(activeSubscription?.status)}
              tone={activeSubscription?.status === "ACTIVE" ? "green" : "gray"}
            />
          </div>
          <div className="vm-dashboard-card-body">
            <dl className="vm-info-list">
              <dt>Gói vé</dt><dd>{activeSubscription ? ticketTypeById.get(activeSubscription.ticketTypeId)?.name ?? "--" : "Chưa có vé hoạt động"}</dd>
              <dt>Hiệu lực</dt><dd>{activeSubscription ? `${formatDate(activeSubscription.effectiveFrom)} - ${formatDate(activeSubscription.effectiveTo)}` : "--"}</dd>
              <dt>Biển số</dt><dd>{vehicleLabel(activeSubscription ? vehicleById.get(activeSubscription.customerVehicleId) : undefined, vehicleTypeById)}</dd>
              <dt>Giá vé</dt><dd>{formatCurrency(activeSubscription?.price)}</dd>
            </dl>
          </div>
          <Link className="vm-outline-btn" to="/customer/subscriptions">Xem chi tiết <i className="fas fa-arrow-right" /></Link>
        </article>

        <article className="vm-customer-card vm-dashboard-summary-card">
          <div className="vm-dashboard-card-title">
            <span className="vm-dashboard-card-icon"><i className="fas fa-motorcycle" /></span>
            <h2>Xe đã đăng ký</h2>
          </div>
          <div className="vm-dashboard-card-body">
            <div className="vm-vehicle-summary">
              <strong>{vehicles.length}</strong>
              <span>xe trong tài khoản</span>
            </div>
            <dl className="vm-info-list">
              <dt>Đang hoạt động</dt><dd>{activeVehicles} xe</dd>
              <dt>Ngưng dùng / khóa</dt><dd>{inactiveVehicles} xe</dd>
              <dt>Xe mặc định</dt><dd>{vehicleLabel(defaultVehicle, vehicleTypeById)}</dd>
            </dl>
          </div>
          <Link className="vm-outline-btn" to="/customer/vehicles">Quản lý xe <i className="fas fa-arrow-right" /></Link>
        </article>

        <article className="vm-customer-card vm-dashboard-summary-card">
          <div className="vm-dashboard-card-title">
            <span className="vm-dashboard-card-icon"><i className="far fa-user-circle" /></span>
            <h2>Trạng thái hồ sơ</h2>
            <StatusBadge
              checked={approvalStatus === "APPROVED"}
              label={approvalLabel(approvalStatus)}
              tone={approvalTone(approvalStatus)}
            />
          </div>
          <div className="vm-dashboard-card-body">
            <dl className="vm-info-list">
              <dt>Khách hàng</dt><dd>{displayName}</dd>
              <dt>Tài khoản</dt><dd>{profile?.account?.accountStatus ?? "--"}</dd>
              <dt>Hồ sơ</dt><dd>{profile?.profile?.userProfileStatus ?? "--"}</dd>
              <dt>Yêu cầu chờ xử lý</dt><dd>{pendingSubscriptions.length} vé</dd>
            </dl>
          </div>
          <Link className="vm-outline-btn" to="/customer/profile">Cập nhật hồ sơ <i className="fas fa-arrow-right" /></Link>
        </article>
      </div>

      <section className="vm-customer-card vm-table-card vm-dashboard-table-card">
        <div className="vm-section-heading">
          <div>
            <h2><i className="fas fa-list-ul" /> Đăng ký vé gần đây</h2>
            <p>Các yêu cầu vé mới nhất của tài khoản này</p>
          </div>
          <Link className="vm-inline-action" to="/customer/subscriptions">Xem tất cả <i className="fas fa-arrow-right" /></Link>
        </div>
        <table className="vm-customer-table vm-dashboard-subscription-table">
          <thead><tr><th>Mã đăng ký</th><th>Gói vé</th><th>Biển số</th><th>Phương tiện</th><th>Hiệu lực</th><th>Giá vé</th><th>Trạng thái</th><th>Ngày đăng ký</th><th aria-label="Mở chi tiết" /></tr></thead>
          <tbody>
            {recentSubscriptions.map((subscription) => {
              const vehicle = vehicleById.get(subscription.customerVehicleId);
              const ticketType = ticketTypeById.get(subscription.ticketTypeId);
              return (
                <tr
                  key={subscription.subscriptionId}
                  className="vm-interactive-row"
                  role="link"
                  tabIndex={0}
                  title="Xem đăng ký vé"
                  onClick={() => navigate(`/customer/subscriptions?subscriptionId=${encodeURIComponent(subscription.subscriptionId)}`)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter" || event.key === " ") {
                      event.preventDefault();
                      navigate(`/customer/subscriptions?subscriptionId=${encodeURIComponent(subscription.subscriptionId)}`);
                    }
                  }}
                >
                  <td title={subscription.subscriptionId}>{shortCode(subscription.subscriptionId)}</td>
                  <td>{ticketType?.name ?? "--"}</td>
                  <td>{vehicle?.licensePlate ?? "--"}</td>
                  <td>{vehicleDescription(vehicle, vehicleTypeById)}</td>
                  <td>{formatDate(subscription.effectiveFrom ?? subscription.requestedEffectiveFrom)} - {formatDate(subscription.effectiveTo)}</td>
                  <td>{formatCurrency(subscription.price)}</td>
                  <td><StatusPill tone={subscription.status === "ACTIVE" ? "green" : subscription.status?.startsWith("PENDING") ? "orange" : "gray"}>{statusLabel(subscription.status)}</StatusPill></td>
                  <td>{formatDateTime(subscription.createdAt)}</td>
                  <td className="vm-row-chevron"><i className="fas fa-chevron-right" /></td>
                </tr>
              );
            })}
            {!loading && recentSubscriptions.length === 0 ? <tr><td colSpan={9}>Chưa có đăng ký vé nào.</td></tr> : null}
          </tbody>
        </table>
      </section>

      <div className="vm-quick-forms vm-dashboard-actions-grid">
        <Link className="vm-customer-card vm-dashboard-action-card" to="/customer/profile">
          <span><i className="far fa-id-card" /></span>
          <div>
            <h2>Quản lý hồ sơ</h2>
            <p>Xem và cập nhật thông tin cá nhân, giấy tờ.</p>
          </div>
          <i className="fas fa-chevron-right" />
        </Link>
        <Link className="vm-customer-card vm-dashboard-action-card" to="/customer/vehicles">
          <span><i className="fas fa-car-side" /></span>
          <div>
            <h2>Quản lý phương tiện</h2>
            <p>Thêm, cập nhật và quản lý xe của bạn.</p>
          </div>
          <i className="fas fa-chevron-right" />
        </Link>
        <Link className="vm-customer-card vm-dashboard-action-card" to="/customer/support">
          <span><i className="far fa-question-circle" /></span>
          <div>
            <h2>Trung tâm hỗ trợ</h2>
            <p>Gửi yêu cầu và theo dõi phản hồi.</p>
          </div>
          <i className="fas fa-chevron-right" />
        </Link>
      </div>

    </CustomerPortalLayout>
  );
}
