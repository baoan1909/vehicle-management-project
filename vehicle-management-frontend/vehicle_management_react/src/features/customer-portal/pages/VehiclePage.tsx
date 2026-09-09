import { useEffect, useMemo, useRef, useState } from "react";

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

import { VehicleVisual, isMotorcycle } from "../components/VehicleVisual";
import { VehicleBannerBackdrop } from "../components/VehicleBannerBackdrop";
import { CustomerPortalLayout } from "./PortalShared";

type VehicleForm = { brand: string; color: string; customerVehicleId: string; isDefault: boolean; licensePlate: string; vehicleTypeId: string };
const emptyForm: VehicleForm = { brand: "", color: "", customerVehicleId: "", isDefault: false, licensePlate: "", vehicleTypeId: "" };

function formatDate(value?: string | null) {
  if (!value) return "--";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "--" : new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(date);
}

function statusLabel(status?: string | null) {
  if (status === "ACTIVE") return "Đang hoạt động";
  if (status === "BLOCKED") return "Bị khóa";
  if (status === "INACTIVE") return "Ngưng dùng";
  return status || "--";
}

function vehicleToForm(vehicle: CustomerPortalVehicle): VehicleForm {
  return { brand: vehicle.brand ?? "", color: vehicle.color ?? "", customerVehicleId: vehicle.customerVehicleId, isDefault: Boolean(vehicle.isDefault), licensePlate: vehicle.licensePlate ?? "", vehicleTypeId: vehicle.vehicleTypeId ?? "" };
}

function toPayload(form: VehicleForm): CustomerPortalVehiclePayload {
  return { brand: form.brand.trim() || null, color: form.color.trim() || null, isDefault: form.isDefault, licensePlate: form.licensePlate.trim(), vehicleTypeId: form.vehicleTypeId };
}

function StatusBadge({ status }: { status?: string | null }) {
  const active = status === "ACTIVE";
  const blocked = status === "BLOCKED";
  return <span className={`tw-inline-flex tw-items-center tw-gap-1.5 tw-rounded-md tw-border tw-border-solid tw-px-2.5 tw-py-1 tw-text-[0.72rem] tw-font-semibold ${active ? "tw-border-[#bcebd1] tw-bg-[#effaf3] tw-text-[#078553]" : blocked ? "tw-border-[#ffd3d3] tw-bg-[#fff2f2] tw-text-[#c33333]" : "tw-border-[#f7dede] tw-bg-[#fff3f3] tw-text-[#b73b3b]"}`}><i className="fas fa-circle tw-text-[0.38rem]" />{statusLabel(status)}</span>;
}

function DefaultBadge() {
  return <span className="tw-inline-flex tw-items-center tw-gap-1.5 tw-rounded-md tw-border tw-border-solid tw-border-[#f3dfad] tw-bg-[#fff9e9] tw-px-2.5 tw-py-1 tw-text-[0.72rem] tw-font-semibold tw-text-[#b87a13]"><i className="fas fa-star" />Mặc định</span>;
}

export function VehiclePage() {
  const [profile, setProfile] = useState<CustomerPortalProfile | null>(null);
  const [vehicles, setVehicles] = useState<CustomerPortalVehicle[]>([]);
  const [vehicleTypes, setVehicleTypes] = useState<CustomerPortalVehicleType[]>([]);
  const [form, setForm] = useState<VehicleForm>(emptyForm);
  const [keyword, setKeyword] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [vehicleTypeFilter, setVehicleTypeFilter] = useState("ALL");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const pageSize = 5;
  const vehicleFormRef = useRef<HTMLElement>(null);
  const focusVehicleForm = () => {
    vehicleFormRef.current?.scrollIntoView({ behavior: "smooth", block: "nearest" });
    vehicleFormRef.current?.querySelector("select")?.focus({ preventScroll: true });
  };

  const vehicleTypeById = useMemo(() => new Map(vehicleTypes.map((type) => [type.vehicleTypeId, type])), [vehicleTypes]);
  const typeName = (vehicle?: CustomerPortalVehicle) => vehicle?.vehicleTypeId ? vehicleTypeById.get(vehicle.vehicleTypeId)?.name ?? "Phương tiện" : "Phương tiện";
  const filteredVehicles = useMemo(() => {
    const query = keyword.trim().toLowerCase();
    return vehicles.filter((vehicle) => {
      const kind = typeName(vehicle).toLowerCase();
      const matchesQuery = !query || vehicle.licensePlate.toLowerCase().includes(query) || (vehicle.brand ?? "").toLowerCase().includes(query) || (vehicle.color ?? "").toLowerCase().includes(query) || kind.includes(query);
      return matchesQuery && (statusFilter === "ALL" || vehicle.status === statusFilter) && (vehicleTypeFilter === "ALL" || vehicle.vehicleTypeId === vehicleTypeFilter);
    });
  }, [keyword, statusFilter, vehicleTypeById, vehicleTypeFilter, vehicles]);
  const totalPages = Math.max(1, Math.ceil(filteredVehicles.length / pageSize));
  const safePage = Math.min(currentPage, totalPages);
  const visibleVehicles = filteredVehicles.slice((safePage - 1) * pageSize, safePage * pageSize);
  const defaultVehicle = vehicles.find((vehicle) => vehicle.isDefault);
  const activeCount = vehicles.filter((vehicle) => vehicle.status === "ACTIVE").length;
  const inactiveCount = vehicles.filter((vehicle) => vehicle.status !== "ACTIVE").length;
  const currentVehicle = vehicles.find((vehicle) => vehicle.customerVehicleId === form.customerVehicleId);
  const cardClass = "tw-rounded-xl tw-border tw-border-solid tw-border-[#e1e8f3] tw-bg-white tw-shadow-[0_7px_18px_rgba(15,23,42,0.055)]";

  useEffect(() => setCurrentPage(1), [keyword, statusFilter, vehicleTypeFilter]);
  useEffect(() => { void loadData(); }, []);

  async function loadData() {
    setLoading(true); setError("");
    try {
      const nextProfile = await getCustomerPortalProfile();
      const [nextVehicles, lookups] = await Promise.all([getMyCustomerVehicles(nextProfile), getCustomerPortalLookups()]);
      setProfile(nextProfile); setVehicles(nextVehicles); setVehicleTypes(lookups.vehicleTypes);
      setForm((value) => ({ ...value, vehicleTypeId: value.vehicleTypeId || lookups.vehicleTypes[0]?.vehicleTypeId || "" }));
    } catch (requestError) { setError(requestError instanceof Error ? requestError.message : "Không thể tải danh sách xe."); }
    finally { setLoading(false); }
  }

  const resetForm = () => { setForm({ ...emptyForm, vehicleTypeId: vehicleTypes[0]?.vehicleTypeId || "" }); setError(""); setNotice(""); };
  const selectVehicle = (vehicle: CustomerPortalVehicle) => { setForm(vehicleToForm(vehicle)); setNotice(""); };
  const handleSave = async () => {
    if (!profile) return;
    setSaving(true); setError(""); setNotice("");
    try {
      if (!form.vehicleTypeId) throw new Error("Vui lòng chọn loại xe.");
      if (!form.licensePlate.trim()) throw new Error("Vui lòng nhập biển số xe.");
      if (form.customerVehicleId) await updateMyCustomerVehicle(form.customerVehicleId, toPayload(form));
      else await saveMyCustomerVehicle(profile, toPayload(form));
      setVehicles(await getMyCustomerVehicles(profile));
      resetForm();
      setNotice(form.customerVehicleId ? "Đã cập nhật phương tiện." : "Đã thêm phương tiện mới.");
    } catch (requestError) { setError(requestError instanceof Error ? requestError.message : "Không thể lưu thông tin xe."); }
    finally { setSaving(false); }
  };
  const handleToggleStatus = async (vehicle: CustomerPortalVehicle) => {
    setSaving(true); setError(""); setNotice("");
    try {
      const changed = vehicle.status === "ACTIVE" ? await inactivateMyCustomerVehicle(vehicle.customerVehicleId) : await activateMyCustomerVehicle(vehicle.customerVehicleId);
      setVehicles((items) => items.map((item) => item.customerVehicleId === changed.customerVehicleId ? changed : item));
      setNotice(vehicle.status === "ACTIVE" ? "Đã ngưng dùng xe." : "Đã kích hoạt xe.");
    } catch (requestError) { setError(requestError instanceof Error ? requestError.message : "Không thể cập nhật trạng thái xe."); }
    finally { setSaving(false); }
  };
  const handleMarkDefault = async (vehicle: CustomerPortalVehicle) => {
    setSaving(true); setError(""); setNotice("");
    try {
      const changed = await markMyCustomerVehicleAsDefault(vehicle.customerVehicleId);
      setVehicles((items) => items.map((item) => ({ ...item, isDefault: item.customerVehicleId === changed.customerVehicleId })));
      setForm((current) => current.customerVehicleId ? { ...current, isDefault: current.customerVehicleId === changed.customerVehicleId } : current);
      setNotice("Đã đặt xe mặc định.");
    } catch (requestError) { setError(requestError instanceof Error ? requestError.message : "Không thể đặt xe mặc định."); }
    finally { setSaving(false); }
  };

  return <CustomerPortalLayout>
    <section className="tw-flex tw-items-center tw-justify-between tw-gap-6 tw-pb-4 max-[640px]:tw-flex-wrap"><div><span className="tw-text-[0.76rem] tw-font-semibold tw-uppercase tw-tracking-wide tw-text-[#0759d8]">Phương tiện cá nhân</span><h1 className="tw-m-0 tw-mt-1 tw-font-[Cambria,Georgia,serif] tw-text-[clamp(2.45rem,4vw,4rem)] tw-font-bold tw-leading-none tw-text-[#081634]">Xe của tôi</h1><p className="tw-m-0 tw-mt-3 tw-text-[0.95rem] tw-font-medium tw-text-[#3f506c]">Quản lý phương tiện rõ ràng trong từng hành trình.</p></div><button className="tw-inline-flex tw-min-h-12 tw-items-center tw-gap-3 tw-rounded-lg tw-border-0 tw-bg-[linear-gradient(135deg,#126cf2,#0757db)] tw-px-6 tw-text-[0.9rem] tw-font-semibold tw-text-white tw-shadow-[0_10px_22px_rgba(9,90,221,0.22)]" type="button" onClick={() => { resetForm(); focusVehicleForm(); }}><i className="fas fa-plus" />Thêm xe mới</button></section>

    {error ? <div className="tw-mb-4 tw-rounded-lg tw-bg-red-50 tw-p-3 tw-text-[0.85rem] tw-font-semibold tw-text-red-600"><i className="fas fa-exclamation-circle tw-mr-2" />{error}</div> : null}
    {notice ? <div className="tw-mb-4 tw-rounded-lg tw-bg-green-50 tw-p-3 tw-text-[0.85rem] tw-font-semibold tw-text-green-700"><i className="fas fa-check-circle tw-mr-2" />{notice}</div> : null}

    <div className="tw-grid tw-grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)] tw-gap-3 max-[900px]:tw-grid-cols-1"><section className={`${cardClass} tw-relative tw-min-w-0 tw-min-h-[252px] tw-overflow-hidden tw-bg-[linear-gradient(110deg,#fbfdff_5%,#f3f7fd_48%,#eaf3fe_100%)] tw-p-7`}><div className="tw-absolute tw-inset-y-0 tw-right-0 tw-w-[58%] tw-bg-[radial-gradient(circle_at_75%_55%,rgba(12,101,232,0.12),transparent_62%)]" /><VehicleBannerBackdrop /><div className="tw-relative tw-z-10 tw-grid tw-max-w-[48%] tw-gap-2"><span className="tw-text-[0.85rem] tw-font-semibold tw-text-[#bb7b13]"><i className="fas fa-star tw-mr-2" />Xe mặc định</span><strong className="tw-text-[clamp(1.35rem,2.25vw,2.1rem)] tw-leading-none tw-text-[#0c1c3b]">{defaultVehicle?.licensePlate ?? "Chưa chọn xe"}</strong><span className="tw-text-[0.88rem] tw-font-semibold tw-text-[#3c4c66]">{defaultVehicle ? [defaultVehicle.brand, typeName(defaultVehicle), defaultVehicle.color].filter(Boolean).join(" · ") : "Thêm phương tiện để bắt đầu quản lý."}</span>{defaultVehicle ? <div className="tw-mt-2 tw-flex tw-flex-wrap tw-gap-2"><StatusBadge status={defaultVehicle.status} /><DefaultBadge /></div> : null}<button className="tw-mt-5 tw-w-fit tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.82rem] tw-font-semibold tw-text-[#263a59]" type="button" onClick={() => defaultVehicle && selectVehicle(defaultVehicle)}>Xem chi tiết <i className="fas fa-chevron-right tw-ml-2" /></button></div>{defaultVehicle ? <div className="tw-absolute tw-bottom-1 tw-right-2 tw-z-10 tw-w-[57%]"><VehicleVisual color={defaultVehicle.color} size="hero" typeName={typeName(defaultVehicle)} /></div> : <i className="fas fa-car-side tw-absolute tw-bottom-10 tw-right-16 tw-text-[7rem] tw-text-[#dbe9ff]" />}</section>
      <div className="tw-grid tw-flex-1 tw-grid-cols-2 tw-gap-4"><section className={`${cardClass} tw-flex tw-min-w-0 tw-items-center tw-gap-3 tw-p-4 max-[1100px]:tw-gap-2 max-[1100px]:tw-p-3`}><span className="tw-grid tw-h-14 tw-w-14 tw-shrink-0 max-[1100px]:tw-h-11 max-[1100px]:tw-w-11 tw-place-items-center tw-rounded-full tw-bg-[#eaf2ff] tw-text-2xl tw-text-[#1263e9]"><i className="fas fa-car-side" /></span><div><strong className="tw-block tw-text-[1.55rem] tw-leading-none tw-text-[#14213d]">{vehicles.length}</strong><span className="tw-mt-1 tw-block tw-text-[0.78rem] tw-font-semibold tw-text-[#4c5c75]">Tổng số xe</span></div></section><section className={`${cardClass} tw-flex tw-min-w-0 tw-items-center tw-gap-3 tw-p-4 max-[1100px]:tw-gap-2 max-[1100px]:tw-p-3`}><span className="tw-grid tw-h-14 tw-w-14 tw-shrink-0 max-[1100px]:tw-h-11 max-[1100px]:tw-w-11 tw-place-items-center tw-rounded-full tw-bg-[#fff7e4] tw-text-2xl tw-text-[#c58a18]"><i className="fas fa-star" /></span><div><strong className="tw-block tw-text-[1.55rem] tw-leading-none tw-text-[#14213d]">{vehicles.filter((vehicle) => vehicle.isDefault).length}</strong><span className="tw-mt-1 tw-block tw-text-[0.78rem] tw-font-semibold tw-text-[#4c5c75]">Xe mặc định</span></div></section><section className={`${cardClass} tw-flex tw-min-w-0 tw-items-center tw-gap-3 tw-p-4 max-[1100px]:tw-gap-2 max-[1100px]:tw-p-3`}><span className="tw-grid tw-h-14 tw-w-14 tw-shrink-0 max-[1100px]:tw-h-11 max-[1100px]:tw-w-11 tw-place-items-center tw-rounded-full tw-bg-[#e9faf2] tw-text-2xl tw-text-[#078553]"><i className="fas fa-wave-square" /></span><div><strong className="tw-block tw-text-[1.55rem] tw-leading-none tw-text-[#14213d]">{activeCount}</strong><span className="tw-mt-1 tw-block tw-text-[0.78rem] tw-font-semibold tw-text-[#4c5c75]">Đang hoạt động</span></div></section><section className={`${cardClass} tw-flex tw-min-w-0 tw-items-center tw-gap-3 tw-p-4 max-[1100px]:tw-gap-2 max-[1100px]:tw-p-3`}><span className="tw-grid tw-h-14 tw-w-14 tw-shrink-0 max-[1100px]:tw-h-11 max-[1100px]:tw-w-11 tw-place-items-center tw-rounded-full tw-bg-[#fff0f0] tw-text-2xl tw-text-[#d34242]"><i className="fas fa-lock" /></span><div><strong className="tw-block tw-text-[1.55rem] tw-leading-none tw-text-[#14213d]">{inactiveCount}</strong><span className="tw-mt-1 tw-block tw-text-[0.78rem] tw-font-semibold tw-text-[#4c5c75]">Ngưng dùng / khóa</span></div></section></div></div>

    <section className={`${cardClass} tw-mt-4 tw-grid tw-grid-cols-[minmax(0,1.25fr)_minmax(0,1fr)_minmax(0,1fr)_auto] tw-items-center tw-gap-3 tw-p-3 max-[760px]:tw-grid-cols-2`}><label className="tw-flex tw-h-11 tw-min-w-0 tw-items-center tw-gap-3 tw-rounded-md tw-border tw-border-solid tw-border-[#e3e9f2] tw-px-4 tw-text-[#667892]"><i className="fas fa-search" /><input className="tw-w-full tw-border-0 tw-bg-transparent tw-text-[0.82rem] tw-font-semibold tw-outline-none" value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="Tìm biển số, hãng xe, màu xe..." /></label><label className="tw-relative tw-flex-1"><i className="fas fa-car-side tw-pointer-events-none tw-absolute tw-left-4 tw-top-[14px] tw-z-10 tw-text-[#293c5a]" /><select className="tw-h-11 tw-w-full tw-appearance-none tw-rounded-md tw-border tw-border-solid tw-border-[#e3e9f2] tw-bg-white tw-pl-11 tw-pr-9 tw-text-[0.82rem] tw-font-bold tw-text-[#283b59]" value={vehicleTypeFilter} onChange={(event) => setVehicleTypeFilter(event.target.value)}><option value="ALL">Tất cả loại xe</option>{vehicleTypes.map((type) => <option key={type.vehicleTypeId} value={type.vehicleTypeId}>{type.name}</option>)}</select><i className="fas fa-chevron-down tw-pointer-events-none tw-absolute tw-right-4 tw-top-[15px] tw-text-[0.7rem]" /></label><label className="tw-relative tw-flex-1"><i className="fas fa-list tw-pointer-events-none tw-absolute tw-left-4 tw-top-[14px] tw-z-10 tw-text-[#293c5a]" /><select className="tw-h-11 tw-w-full tw-appearance-none tw-rounded-md tw-border tw-border-solid tw-border-[#e3e9f2] tw-bg-white tw-pl-11 tw-pr-9 tw-text-[0.82rem] tw-font-bold tw-text-[#283b59]" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}><option value="ALL">Tất cả trạng thái</option><option value="ACTIVE">Đang hoạt động</option><option value="INACTIVE">Ngưng dùng</option><option value="BLOCKED">Bị khóa</option></select><i className="fas fa-chevron-down tw-pointer-events-none tw-absolute tw-right-4 tw-top-[15px] tw-text-[0.7rem]" /></label><strong className="tw-whitespace-nowrap tw-pr-3 tw-text-[0.8rem] tw-text-[#172541]">{filteredVehicles.length} kết quả</strong></section>

    <div className="tw-mt-4 tw-grid tw-grid-cols-[minmax(0,1.65fr)_minmax(300px,1fr)] max-[1000px]:tw-grid-cols-1 tw-gap-4"><section className={`${cardClass} tw-min-w-0 tw-p-4`}><h2 className="tw-m-0 tw-mb-3 tw-text-[1.05rem] tw-font-bold tw-text-[#14213d]">Danh sách xe đã đăng ký</h2><div className="tw-grid tw-gap-3">{visibleVehicles.map((vehicle) => { const selected = vehicle.customerVehicleId === form.customerVehicleId; return <article className={`tw-grid tw-grid-cols-[minmax(130px,.65fr)_minmax(0,1.5fr)_128px] tw-min-h-[145px] max-[640px]:tw-grid-cols-[110px_minmax(0,1fr)] tw-items-center tw-gap-x-3 tw-gap-y-1 tw-rounded-xl tw-border tw-border-solid tw-p-3 tw-transition ${selected ? "tw-border-[#0a67f0] tw-bg-[#fbfdff] tw-shadow-[0_3px_12px_rgba(22,102,235,0.13)]" : "tw-border-[#edf0f5] tw-bg-white hover:tw-border-[#bcd4fb]"}`} key={vehicle.customerVehicleId}><VehicleVisual color={vehicle.color} size="card" typeName={typeName(vehicle)} /><div className="tw-min-w-0"><div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2"><strong className="tw-text-[1.2rem] tw-text-[#14213d]">{vehicle.licensePlate}</strong>{vehicle.isDefault ? <DefaultBadge /> : null}<StatusBadge status={vehicle.status} /></div><div className="tw-mt-3 tw-flex tw-flex-wrap tw-items-center tw-gap-x-5 tw-gap-y-2 tw-text-[0.78rem] tw-font-semibold tw-text-[#42526b]"><span><i className={isMotorcycle(typeName(vehicle)) ? "fas fa-motorcycle tw-mr-2" : "fas fa-car-side tw-mr-2"} />{typeName(vehicle)}</span><span><i className="fas fa-certificate tw-mr-2" />{vehicle.brand || "--"}</span><span><i className="fas fa-circle tw-mr-2 tw-text-[0.65rem]" />{vehicle.color || "--"}</span></div><small className="tw-mt-3 tw-block tw-text-[0.7rem] tw-font-semibold tw-text-[#71819a]"><i className="far fa-calendar-alt tw-mr-2" />Cập nhật: {formatDate(vehicle.updatedAt ?? vehicle.createdAt)}</small></div><div className="tw-col-start-3 tw-flex tw-items-center tw-justify-end tw-gap-1 max-[640px]:tw-col-start-2"><button aria-label="Chỉnh sửa xe" className="tw-grid tw-h-9 tw-w-9 tw-place-items-center tw-rounded-full tw-border-0 tw-bg-transparent tw-text-[#1c3150]" type="button" onClick={(event) => { event.stopPropagation(); selectVehicle(vehicle); focusVehicleForm(); }}><i className="fas fa-pencil-alt" /></button><button aria-label="Đặt xe mặc định" className={`tw-grid tw-h-9 tw-w-9 tw-place-items-center tw-rounded-full tw-border-0 tw-bg-transparent ${vehicle.isDefault ? "tw-text-[#c58a18]" : "tw-text-[#1c3150]"} disabled:tw-opacity-40`} disabled={Boolean(vehicle.isDefault) || vehicle.status !== "ACTIVE" || saving} type="button" onClick={(event) => { event.stopPropagation(); void handleMarkDefault(vehicle); }}><i className={vehicle.isDefault ? "fas fa-star" : "far fa-star"} /></button><button role="switch" aria-checked={vehicle.status === "ACTIVE"} aria-label={vehicle.status === "ACTIVE" ? "Ngưng dùng xe" : "Kích hoạt xe"} className={`tw-relative tw-h-6 tw-w-11 tw-shrink-0 tw-rounded-full tw-border-0 tw-transition ${vehicle.status === "ACTIVE" ? "tw-bg-[#0a69ee]" : "tw-bg-[#cbd3df]"} disabled:tw-opacity-40`} disabled={vehicle.status === "BLOCKED" || saving} type="button" onClick={(event) => { event.stopPropagation(); void handleToggleStatus(vehicle); }}><span className={`tw-absolute tw-top-1 tw-h-4 tw-w-4 tw-rounded-full tw-bg-white tw-shadow tw-transition-all ${vehicle.status === "ACTIVE" ? "tw-left-6" : "tw-left-1"}`} /></button></div></article>; })}{!loading && !visibleVehicles.length ? <p className="tw-p-8 tw-text-center tw-text-[0.84rem] tw-font-semibold tw-text-[#71819a]">Chưa có xe phù hợp với bộ lọc.</p> : null}{loading ? <p className="tw-p-8 tw-text-center tw-text-[0.84rem] tw-font-semibold tw-text-[#71819a]">Đang tải danh sách xe...</p> : null}</div><div className="tw-mt-5 tw-flex tw-items-center tw-justify-center tw-gap-3"><button aria-label="Trang trước" className="tw-grid tw-h-9 tw-w-9 tw-place-items-center tw-rounded-md tw-border tw-border-solid tw-border-[#e2e8f1] tw-bg-white disabled:tw-opacity-40" disabled={safePage === 1} type="button" onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}><i className="fas fa-chevron-left tw-text-[0.7rem]" /></button><span className="tw-grid tw-h-9 tw-min-w-9 tw-place-items-center tw-rounded-md tw-bg-[#0b68ef] tw-px-2 tw-text-[0.8rem] tw-font-semibold tw-text-white">{safePage}</span><button aria-label="Trang sau" className="tw-grid tw-h-9 tw-w-9 tw-place-items-center tw-rounded-md tw-border tw-border-solid tw-border-[#e2e8f1] tw-bg-white disabled:tw-opacity-40" disabled={safePage === totalPages} type="button" onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}><i className="fas fa-chevron-right tw-text-[0.7rem]" /></button></div></section>

      <aside ref={vehicleFormRef} className={`${cardClass} tw-h-fit tw-min-w-0 tw-scroll-mt-24 tw-p-5 [&_label]:tw-min-w-0 [&_input:not([type=checkbox])]:tw-w-full [&_select]:tw-w-full`}><h2 className="tw-m-0 tw-flex tw-items-center tw-gap-3 tw-text-[1.1rem] tw-font-bold tw-text-[#14213d]"><span className="tw-grid tw-h-10 tw-w-10 tw-place-items-center tw-rounded-full tw-bg-[#eaf2ff] tw-text-[#0a68ef]"><i className="fas fa-car-side" /></span>{form.customerVehicleId ? "Chỉnh sửa phương tiện" : "Thêm phương tiện"}</h2><p className="tw-m-0 tw-mt-1 tw-text-[0.76rem] tw-font-medium tw-text-[#71819a]">Cập nhật thông tin phương tiện của bạn</p><div className="tw-mt-5 tw-grid tw-grid-cols-2 tw-gap-4"><label className="tw-grid tw-gap-1.5 tw-text-[0.74rem] tw-font-semibold tw-text-[#304462]">Loại xe<select className="tw-h-10 tw-rounded-md tw-border tw-border-solid tw-border-[#dfe6ef] tw-bg-white tw-px-3 tw-text-[0.82rem] tw-font-semibold" value={form.vehicleTypeId} onChange={(event) => setForm((value) => ({ ...value, vehicleTypeId: event.target.value }))}><option value="">Chọn loại xe</option>{vehicleTypes.map((type) => <option key={type.vehicleTypeId} value={type.vehicleTypeId}>{type.name}</option>)}</select></label><label className="tw-grid tw-gap-1.5 tw-text-[0.74rem] tw-font-semibold tw-text-[#304462]">Biển số xe<input className="tw-h-10 tw-rounded-md tw-border tw-border-solid tw-border-[#dfe6ef] tw-px-3 tw-text-[0.82rem] tw-font-semibold" value={form.licensePlate} onChange={(event) => setForm((value) => ({ ...value, licensePlate: event.target.value.toUpperCase() }))} /></label><label className="tw-grid tw-gap-1.5 tw-text-[0.74rem] tw-font-semibold tw-text-[#304462]">Hãng xe<input className="tw-h-10 tw-rounded-md tw-border tw-border-solid tw-border-[#dfe6ef] tw-px-3 tw-text-[0.82rem] tw-font-semibold" value={form.brand} onChange={(event) => setForm((value) => ({ ...value, brand: event.target.value }))} placeholder="Ví dụ: Toyota Camry" /></label><label className="tw-grid tw-gap-1.5 tw-text-[0.74rem] tw-font-semibold tw-text-[#304462]">Màu xe<input className="tw-h-10 tw-rounded-md tw-border tw-border-solid tw-border-[#dfe6ef] tw-px-3 tw-text-[0.82rem] tw-font-semibold" value={form.color} onChange={(event) => setForm((value) => ({ ...value, color: event.target.value }))} placeholder="Ví dụ: Trắng" /></label></div><label className="tw-mt-5 tw-flex tw-items-center tw-gap-2 tw-text-[0.8rem] tw-font-semibold tw-text-[#3d506d]"><input className="tw-h-4 tw-w-4 tw-accent-[#0a68ef]" checked={form.isDefault} type="checkbox" onChange={(event) => setForm((value) => ({ ...value, isDefault: event.target.checked }))} />Đặt làm xe mặc định</label><div className="tw-mt-5 tw-flex tw-items-center tw-justify-between tw-rounded-md tw-bg-[#f7f9fc] tw-px-3 tw-py-3"><span className="tw-text-[0.78rem] tw-font-bold tw-text-[#4a5b74]">Trạng thái hiện tại</span><StatusBadge status={currentVehicle?.status} /></div><div className="tw-mt-7 tw-grid tw-grid-cols-2 tw-gap-3"><button className="tw-h-11 tw-rounded-md tw-border tw-border-solid tw-border-[#dce4ee] tw-bg-white tw-text-[0.84rem] tw-font-semibold tw-text-[#40516a]" type="button" onClick={resetForm}>Hủy</button><button className="tw-h-11 tw-rounded-md tw-border-0 tw-bg-[linear-gradient(135deg,#146cf3,#0756d8)] tw-text-[0.84rem] tw-font-semibold tw-text-white tw-shadow-[0_8px_18px_rgba(20,99,230,0.18)] disabled:tw-opacity-60" disabled={saving || !profile} type="button" onClick={handleSave}>{saving ? "Đang lưu..." : "Lưu xe"}</button></div></aside></div>
  </CustomerPortalLayout>;
}
