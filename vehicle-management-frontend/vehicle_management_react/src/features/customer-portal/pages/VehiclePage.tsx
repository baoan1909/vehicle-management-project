import { useEffect, useMemo, useState } from "react";

import {
  activateMyCustomerVehicle,
  getCustomerPortalLookups,
  getCustomerPortalProfile,
  getMyCustomerVehicles,
  inactivateMyCustomerVehicle,
  markMyCustomerVehicleAsDefault,
  saveMyCustomerVehicle,
  updateMyCustomerVehicle,
  type CustomerPortalProfile,
  type CustomerPortalVehicle,
  type CustomerPortalVehiclePayload,
  type CustomerPortalVehicleType,
} from "@/features/customer-portal/api/customerPortalApi";

import { CustomerPageHeader, CustomerPortalLayout, Field, PaginationLite, StatCard, StatusPill } from "./PortalShared";

type VehicleForm = {
  brand: string;
  color: string;
  customerVehicleId: string;
  isDefault: boolean;
  licensePlate: string;
  vehicleTypeId: string;
};

const emptyForm: VehicleForm = {
  brand: "",
  color: "",
  customerVehicleId: "",
  isDefault: false,
  licensePlate: "",
  vehicleTypeId: "",
};

type StatusTone = "green" | "blue" | "orange" | "red" | "gray" | "purple";

function formatDate(value?: string | null) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--";
  return new Intl.DateTimeFormat("vi-VN").format(date);
}

function statusTone(status?: string | null): StatusTone {
  if (status === "ACTIVE") return "green";
  if (status === "BLOCKED") return "red";
  if (status === "INACTIVE") return "orange";
  return "gray";
}

function statusLabel(status?: string | null) {
  if (status === "ACTIVE") return "Đang hoạt động";
  if (status === "BLOCKED") return "Bị khóa";
  if (status === "INACTIVE") return "Ngưng dùng";
  return status || "--";
}

function vehicleToForm(vehicle: CustomerPortalVehicle): VehicleForm {
  return {
    brand: vehicle.brand ?? "",
    color: vehicle.color ?? "",
    customerVehicleId: vehicle.customerVehicleId,
    isDefault: Boolean(vehicle.isDefault),
    licensePlate: vehicle.licensePlate ?? "",
    vehicleTypeId: vehicle.vehicleTypeId ?? "",
  };
}

function toPayload(form: VehicleForm): CustomerPortalVehiclePayload {
  return {
    brand: form.brand.trim() || null,
    color: form.color.trim() || null,
    isDefault: form.isDefault,
    licensePlate: form.licensePlate.trim(),
    vehicleTypeId: form.vehicleTypeId,
  };
}

export function VehiclePage() {
  const [profile, setProfile] = useState<CustomerPortalProfile | null>(null);
  const [vehicles, setVehicles] = useState<CustomerPortalVehicle[]>([]);
  const [vehicleTypes, setVehicleTypes] = useState<CustomerPortalVehicleType[]>([]);
  const [form, setForm] = useState<VehicleForm>(emptyForm);
  const [keyword, setKeyword] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [vehicleTypeFilter, setVehicleTypeFilter] = useState("ALL");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const vehicleTypeById = useMemo(
    () => new Map(vehicleTypes.map((type) => [type.vehicleTypeId, type])),
    [vehicleTypes],
  );

  const filteredVehicles = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return vehicles.filter((vehicle) => {
      const typeName = vehicle.vehicleTypeId ? vehicleTypeById.get(vehicle.vehicleTypeId)?.name ?? "" : "";
      const matchesKeyword = !normalizedKeyword
        || vehicle.licensePlate.toLowerCase().includes(normalizedKeyword)
        || (vehicle.brand ?? "").toLowerCase().includes(normalizedKeyword)
        || (vehicle.color ?? "").toLowerCase().includes(normalizedKeyword)
        || typeName.toLowerCase().includes(normalizedKeyword);
      const matchesStatus = statusFilter === "ALL" || vehicle.status === statusFilter;
      const matchesType = vehicleTypeFilter === "ALL" || vehicle.vehicleTypeId === vehicleTypeFilter;
      return matchesKeyword && matchesStatus && matchesType;
    });
  }, [keyword, statusFilter, vehicleTypeById, vehicleTypeFilter, vehicles]);

  const defaultVehicle = vehicles.find((vehicle) => vehicle.isDefault);
  const activeCount = vehicles.filter((vehicle) => vehicle.status === "ACTIVE").length;
  const unavailableCount = vehicles.filter((vehicle) => vehicle.status !== "ACTIVE").length;
  const currentVehicle = vehicles.find((vehicle) => vehicle.customerVehicleId === form.customerVehicleId);
  const totalPages = Math.max(1, Math.ceil(filteredVehicles.length / pageSize));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const pagedVehicles = filteredVehicles.slice((safeCurrentPage - 1) * pageSize, safeCurrentPage * pageSize);

  useEffect(() => {
    setCurrentPage(1);
  }, [keyword, pageSize, statusFilter, vehicleTypeFilter]);

  async function loadData() {
    setLoading(true);
    setError("");
    try {
      const nextProfile = await getCustomerPortalProfile();
      const [nextVehicles, lookups] = await Promise.all([
        getMyCustomerVehicles(nextProfile),
        getCustomerPortalLookups(),
      ]);
      setProfile(nextProfile);
      setVehicles(nextVehicles);
      setVehicleTypes(lookups.vehicleTypes);
      setForm((current) => ({
        ...current,
        vehicleTypeId: current.vehicleTypeId || lookups.vehicleTypes[0]?.vehicleTypeId || "",
      }));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể tải danh sách xe.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadData();
  }, []);

  const resetForm = () => {
    setForm({
      ...emptyForm,
      vehicleTypeId: vehicleTypes[0]?.vehicleTypeId || "",
    });
    setNotice("");
    setError("");
  };

  const handleSave = async () => {
    if (!profile) return;
    setSaving(true);
    setError("");
    setNotice("");
    try {
      if (!form.vehicleTypeId) {
        throw new Error("Vui lòng chọn loại xe.");
      }
      if (!form.licensePlate.trim()) {
        throw new Error("Vui lòng nhập biển số xe.");
      }

      if (form.customerVehicleId) {
        await updateMyCustomerVehicle(form.customerVehicleId, toPayload(form));
        setNotice("Đã cập nhật xe.");
      } else {
        await saveMyCustomerVehicle(profile, toPayload(form));
        setNotice("Đã thêm xe mới.");
      }

      const nextVehicles = await getMyCustomerVehicles(profile);
      setVehicles(nextVehicles);
      resetForm();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể lưu thông tin xe.");
    } finally {
      setSaving(false);
    }
  };

  const handleToggleStatus = async (vehicle: CustomerPortalVehicle) => {
    setSaving(true);
    setError("");
    setNotice("");
    try {
      const updatedVehicle = vehicle.status === "ACTIVE"
        ? await inactivateMyCustomerVehicle(vehicle.customerVehicleId)
        : await activateMyCustomerVehicle(vehicle.customerVehicleId);
      setVehicles((current) => current.map((item) => (
        item.customerVehicleId === updatedVehicle.customerVehicleId ? updatedVehicle : item
      )));
      setNotice(vehicle.status === "ACTIVE" ? "Đã ngưng dùng xe." : "Đã kích hoạt xe.");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể cập nhật trạng thái xe.");
    } finally {
      setSaving(false);
    }
  };

  const handleMarkDefault = async (vehicle: CustomerPortalVehicle) => {
    setSaving(true);
    setError("");
    setNotice("");
    try {
      const updatedVehicle = await markMyCustomerVehicleAsDefault(vehicle.customerVehicleId);
      setVehicles((current) => current.map((item) => ({
        ...item,
        isDefault: item.customerVehicleId === updatedVehicle.customerVehicleId,
      })));
      setNotice("Đã đặt xe mặc định.");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể đặt xe mặc định.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <CustomerPortalLayout>
      <CustomerPageHeader
        title="Xe của tôi"
        subtitle="Quản lý danh sách xe đã đăng ký trong tài khoản"
      />

      {error ? <div className="vm-info-note tw-bg-red-50 tw-text-red-600"><i className="fas fa-exclamation-circle" /> {error}</div> : null}
      {notice ? <div className="vm-info-note tw-bg-green-50 tw-text-green-700"><i className="fas fa-check-circle" /> {notice}</div> : null}

      <div className="vm-stat-grid vm-stat-grid-four">
        <StatCard icon="fas fa-motorcycle" label="Tổng số xe" value={String(vehicles.length)} note="xe" />
        <StatCard
          icon="far fa-star"
          label="Xe mặc định"
          value={defaultVehicle?.licensePlate ?? "--"}
          note={defaultVehicle ? <StatusPill> Mặc định </StatusPill> : "Chưa chọn"}
          tone="green"
        />
        <StatCard icon="far fa-check-circle" label="Đang hoạt động" value={String(activeCount)} note="xe" tone="green" />
        <StatCard icon="fas fa-lock" label="Ngưng dùng / khóa" value={String(unavailableCount)} note="xe" tone="orange" />
      </div>

      <div className="vm-two-column-main">
        <section className="vm-customer-card vm-table-card">
          <h2>Danh sách xe đã đăng ký</h2>
          <div className="vm-table-filters">
            <label><i className="fas fa-search" /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="Tìm biển số, hãng xe..." /></label>
            <select value={vehicleTypeFilter} onChange={(event) => setVehicleTypeFilter(event.target.value)}>
              <option value="ALL">Tất cả loại xe</option>
              {vehicleTypes.map((type) => <option key={type.vehicleTypeId} value={type.vehicleTypeId}>{type.name}</option>)}
            </select>
            <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
              <option value="ALL">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="INACTIVE">Ngưng dùng</option>
              <option value="BLOCKED">Bị khóa</option>
            </select>
          </div>
          <table className="vm-customer-table">
            <thead><tr><th>Biển số</th><th>Loại xe</th><th>Hãng xe</th><th>Màu xe</th><th>Mặc định</th><th>Trạng thái</th><th>Cập nhật</th><th>Thao tác</th></tr></thead>
            <tbody>
              {pagedVehicles.map((vehicle) => (
                <tr key={vehicle.customerVehicleId}>
                  <td>{vehicle.licensePlate}</td>
                  <td>{vehicle.vehicleTypeId ? vehicleTypeById.get(vehicle.vehicleTypeId)?.name ?? "--" : "--"}</td>
                  <td>{vehicle.brand || "--"}</td>
                  <td>{vehicle.color || "--"}</td>
                  <td><StatusPill tone={vehicle.isDefault ? "green" : "gray"}>{vehicle.isDefault ? "Có" : "Không"}</StatusPill></td>
                  <td><StatusPill tone={statusTone(vehicle.status)}>{statusLabel(vehicle.status)}</StatusPill></td>
                  <td>{formatDate(vehicle.updatedAt ?? vehicle.createdAt)}</td>
                  <td className="vm-action-icons">
                    <button className="vm-action-edit" type="button" title="Sửa xe" onClick={() => setForm(vehicleToForm(vehicle))}><i className="fas fa-pencil-alt" /></button>
                    <button className="vm-action-favorite" type="button" title="Đặt mặc định" disabled={Boolean(vehicle.isDefault) || vehicle.status !== "ACTIVE" || saving} onClick={() => handleMarkDefault(vehicle)}><i className="far fa-star" /></button>
                    <button className="vm-action-stop" type="button" title={vehicle.status === "ACTIVE" ? "Ngưng dùng" : "Kích hoạt"} disabled={vehicle.status === "BLOCKED" || saving} onClick={() => handleToggleStatus(vehicle)}><i className={vehicle.status === "ACTIVE" ? "fas fa-ban" : "far fa-check-circle"} /></button>
                  </td>
                </tr>
              ))}
              {!loading && filteredVehicles.length === 0 ? (
                <tr><td colSpan={8}>Chưa có xe phù hợp với bộ lọc.</td></tr>
              ) : null}
              {loading ? <tr><td colSpan={8}>Đang tải dữ liệu...</td></tr> : null}
            </tbody>
          </table>
          <PaginationLite
            currentPage={safeCurrentPage}
            pageSize={pageSize}
            totalRecords={filteredVehicles.length}
            onPageChange={setCurrentPage}
            onPageSizeChange={setPageSize}
          />
        </section>

        <aside className="vm-customer-card vm-side-form">
          <h2>{form.customerVehicleId ? "Cập nhật xe" : "Thêm xe mới"}</h2>
          <Field label="Loại xe">
            <select value={form.vehicleTypeId} onChange={(event) => setForm((current) => ({ ...current, vehicleTypeId: event.target.value }))}>
              <option value="">Chọn loại xe</option>
              {vehicleTypes.map((type) => <option key={type.vehicleTypeId} value={type.vehicleTypeId}>{type.name}</option>)}
            </select>
          </Field>
          <Field label="Biển số xe"><input value={form.licensePlate} onChange={(event) => setForm((current) => ({ ...current, licensePlate: event.target.value.toUpperCase() }))} /></Field>
          <Field label="Hãng xe"><input value={form.brand} onChange={(event) => setForm((current) => ({ ...current, brand: event.target.value }))} /></Field>
          <Field label="Màu xe"><input value={form.color} onChange={(event) => setForm((current) => ({ ...current, color: event.target.value }))} /></Field>
          <label className="vm-checkbox-lite"><input checked={form.isDefault} type="checkbox" onChange={(event) => setForm((current) => ({ ...current, isDefault: event.target.checked }))} /> Đặt làm xe mặc định</label>
          <div className="vm-current-status"><span>Trạng thái hiện tại</span><StatusPill tone={statusTone(currentVehicle?.status)}>{statusLabel(currentVehicle?.status)}</StatusPill></div>
          <div className="vm-form-actions vm-vehicle-form-actions">
            <button className="vm-outline-btn" type="button" onClick={resetForm}>Hủy</button>
            <button type="button" disabled={saving || !profile} onClick={handleSave}>{saving ? "Đang lưu..." : "Lưu xe"}</button>
          </div>
        </aside>
      </div>
    </CustomerPortalLayout>
  );
}
