import { useRef, useState, type FormEvent } from "react";
import { Modal, SelectMenu } from "@/components/ui";
import { saveMyCustomerVehicle, type CustomerPortalVehicle, type CustomerPortalVehicleType } from "../api/customerPortalApi";

type Props = {
  vehicleTypes: CustomerPortalVehicleType[];
  onClose: () => void;
  onCreated: (vehicle: CustomerPortalVehicle) => void;
};

export function CreateVehicleDialog({ vehicleTypes, onClose, onCreated }: Props) {
  const [form, setForm] = useState({ vehicleTypeId: vehicleTypes[0]?.vehicleTypeId ?? "", licensePlate: "", brand: "", color: "", isDefault: false });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const savingRef = useRef(false);
  const fieldClass = "tw-grid tw-min-w-0 tw-gap-1.5 tw-text-sm tw-font-semibold tw-text-slate-700";
  const inputClass = "tw-h-11 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-px-3 tw-text-sm tw-outline-none focus:tw-border-vm-primary disabled:tw-bg-slate-50";

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (savingRef.current) return;
    if (!form.vehicleTypeId || !form.licensePlate.trim()) { setError("Vui lòng chọn loại xe và nhập biển số xe."); return; }
    savingRef.current = true;
    setSaving(true);
    setError("");
    try {
      const vehicle = await saveMyCustomerVehicle({
        vehicleTypeId: form.vehicleTypeId, licensePlate: form.licensePlate.trim().toUpperCase(),
        brand: form.brand.trim() || null, color: form.color.trim() || null, isDefault: form.isDefault,
      });
      onCreated(vehicle);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Không thể thêm phương tiện. Vui lòng thử lại.");
    } finally {
      savingRef.current = false;
      setSaving(false);
    }
  }

  return <Modal open title="Thêm phương tiện" description="Thêm xe để tiếp tục đăng ký vé." onClose={() => { if (!savingRef.current) onClose(); }}>
    <form onSubmit={(event) => void submit(event)} aria-label="Thêm phương tiện" aria-busy={saving}>
      {error ? <p role="alert" className="tw-mb-4 tw-rounded-lg tw-bg-red-50 tw-p-3 tw-text-sm tw-text-red-700">{error}</p> : null}
      <fieldset disabled={saving} className="tw-m-0 tw-min-w-0 tw-border-0 tw-p-0">
        <legend className="tw-sr-only">Thông tin phương tiện</legend>
        <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[560px]:tw-grid-cols-1">
          <div className={fieldClass}><span>Loại xe</span><SelectMenu ariaLabel="Chọn loại xe" portal value={form.vehicleTypeId} disabled={saving}
            options={[{ label: "Chọn loại xe", value: "" }, ...vehicleTypes.map((type) => ({ label: type.name, value: type.vehicleTypeId }))]}
            onChange={(vehicleTypeId) => setForm((current) => ({ ...current, vehicleTypeId }))} /></div>
          <label className={fieldClass}>Biển số xe<input className={inputClass} required maxLength={20} value={form.licensePlate} onChange={(event) => setForm((current) => ({ ...current, licensePlate: event.target.value.toUpperCase() }))} /></label>
          <label className={fieldClass}>Hãng xe<input className={inputClass} maxLength={80} placeholder="Ví dụ: Toyota Camry" value={form.brand} onChange={(event) => setForm((current) => ({ ...current, brand: event.target.value }))} /></label>
          <label className={fieldClass}>Màu xe<input className={inputClass} maxLength={50} placeholder="Ví dụ: Trắng" value={form.color} onChange={(event) => setForm((current) => ({ ...current, color: event.target.value }))} /></label>
        </div>
        <label className="tw-mt-5 tw-flex tw-items-center tw-gap-2 tw-text-sm tw-font-semibold tw-text-slate-700"><input type="checkbox" checked={form.isDefault} onChange={(event) => setForm((current) => ({ ...current, isDefault: event.target.checked }))} />Đặt làm xe mặc định</label>
      </fieldset>
      <div className="tw-mt-6 tw-flex tw-gap-3">
        <button type="button" disabled={saving} onClick={onClose} className="tw-h-11 tw-flex-1 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-font-bold tw-text-slate-700">Hủy</button>
        <button type="submit" disabled={saving || !vehicleTypes.length} className="tw-h-11 tw-flex-1 tw-rounded-vm-md tw-border-0 tw-bg-vm-primary tw-font-bold tw-text-white disabled:tw-opacity-50">{saving ? "Đang lưu..." : "Lưu xe"}</button>
      </div>
    </form>
  </Modal>;
}
