import { useEffect, useState, type FormEvent } from "react";

import { Badge, Button } from "@/components/ui";
import { ParkingAddressPicker } from "@/features/parking/components/ParkingAddressPicker";
import { ParkingLocationMap } from "@/features/parking/components/ParkingLocationMap";
import { reverseParkingLocation, searchParkingLocations } from "@/features/parking/api/parkingLocationApi";
import type { AddressInputSchemeApi, ParkingLotStatusApi } from "@/features/parking/api/parkingLotsApi";

export type ParkingLotLocationResolution = "GEOCODED" | "MANUAL" | "NONE";

export type ParkingLotInfoValue = {
  addressDisplay: string;
  addressInputScheme: AddressInputSchemeApi;
  currentWardCode: string | null;
  latitude: number | null;
  legacyWardCode: string | null;
  locationResolution: ParkingLotLocationResolution;
  longitude: number | null;
  name: string;
  totalCapacity: number;
};

export function blankParkingLotInfo(): ParkingLotInfoValue {
  return {
    addressDisplay: "",
    addressInputScheme: "CURRENT",
    currentWardCode: null,
    latitude: null,
    legacyWardCode: null,
    locationResolution: "NONE",
    longitude: null,
    name: "",
    totalCapacity: 0,
  };
}

function lotStatusTone(status: ParkingLotStatusApi) {
  if (status === "ACTIVE") return "success";
  if (status === "SETUP" || status === "MAINTENANCE") return "warning";
  return "neutral";
}

function lotStatusLabel(status: ParkingLotStatusApi) {
  if (status === "SETUP") return "Đang thiết lập";
  if (status === "ACTIVE") return "Đang hoạt động";
  if (status === "MAINTENANCE") return "Bảo trì";
  return "Đã đóng";
}

/**
 * Form thông tin bãi xe dùng chung (tên, sức chứa, địa chỉ + tọa độ + validate).
 * Tách từ drawer "Thêm/Sửa bãi xe" để tái dùng cho modal tạo bãi nhiều bước.
 */
export function ParkingLotInfoForm({ formKey, initial, onClose, onSubmit, serverError, statusMeta, submitLabel, submitting }: {
  formKey: string;
  initial: ParkingLotInfoValue | null;
  onClose: () => void;
  onSubmit: (value: ParkingLotInfoValue) => void;
  serverError: string | null;
  statusMeta?: { geocodingStatus?: string | null; status: ParkingLotStatusApi } | null;
  submitLabel: string;
  submitting: boolean;
}) {
  const [form, setForm] = useState<ParkingLotInfoValue>(() => initial ?? blankParkingLotInfo());
  const [formError, setFormError] = useState("");
  const [locationSearchError, setLocationSearchError] = useState("");
  const [searchingLocation, setSearchingLocation] = useState(false);
  const [reverseAddress, setReverseAddress] = useState("");
  const [locationMismatchWarning, setLocationMismatchWarning] = useState("");

  useEffect(() => {
    setForm(initial ?? blankParkingLotInfo());
    setFormError("");
    setLocationSearchError("");
    setReverseAddress("");
    setLocationMismatchWarning("");
  }, [initial, formKey]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError("");

    if (!form.name.trim() || form.totalCapacity < 0) {
      setFormError("Vui lòng nhập tên bãi và sức chứa không âm.");
      return;
    }

    if (!form.addressDisplay.trim()) {
      setFormError("Vui lòng nhập địa chỉ và bấm “Xác định vị trí”.");
      return;
    }
    if (form.addressInputScheme === "CURRENT" && !form.currentWardCode) {
      setFormError("Vui lòng chọn phường/xã hiện hành.");
      return;
    }
    if (form.addressInputScheme === "LEGACY" && !form.legacyWardCode) {
      setFormError("Vui lòng chọn phường/xã theo địa chỉ cũ.");
      return;
    }

    if ((form.latitude === null) !== (form.longitude === null)) {
      setFormError("Dữ liệu vị trí chưa hoàn chỉnh. Hãy tìm lại vị trí hoặc tạo bãi không kèm tọa độ.");
      return;
    }

    if (form.latitude !== null && form.longitude !== null && (
      !Number.isFinite(form.latitude)
      || !Number.isFinite(form.longitude)
      || form.latitude < -90
      || form.latitude > 90
      || form.longitude < -180
      || form.longitude > 180
    )) {
      setFormError("Vĩ độ phải từ -90 đến 90 và kinh độ phải từ -180 đến 180.");
      return;
    }

    onSubmit({
      ...form,
      addressDisplay: form.addressDisplay.trim(),
      name: form.name.trim(),
      totalCapacity: Number(form.totalCapacity),
    });
  }

  async function handleSearchLocation() {
    const query = form.addressDisplay.trim();
    if (!query) {
      setLocationSearchError("Vui lòng nhập địa chỉ trước khi xác định vị trí.");
      return;
    }

    setSearchingLocation(true);
    setLocationSearchError("");
    try {
      const response = await searchParkingLocations(query);
      const result = response.data?.[0];
      if (!result) {
        setLocationSearchError("Không tìm thấy địa chỉ này. Hãy bổ sung số nhà, đường hoặc phường/xã rồi thử lại.");
        return;
      }

      setForm((current) => ({
        ...current,
        addressDisplay: result.displayName,
        latitude: Number(result.latitude),
        longitude: Number(result.longitude),
        locationResolution: "GEOCODED",
      }));
      setReverseAddress(result.displayName);
    } catch {
      setLocationSearchError(
        "Không thể tra tọa độ tự động vì mạng hiện tại không kết nối được dịch vụ định vị. "
        + "Bạn vẫn có thể tạo bãi bằng địa chỉ này; bãi sẽ ở trạng thái chưa định vị và chưa thể tính khoảng cách cho khách hàng.",
      );
    } finally {
      setSearchingLocation(false);
    }
  }

  async function handleMarkerMoved(latitude: number, longitude: number) {
    setForm((current) => ({ ...current, latitude, longitude, locationResolution: "NONE" }));
    setReverseAddress("");
    setLocationMismatchWarning("");
    try {
      const response = await reverseParkingLocation(latitude, longitude);
      const resolved = response.data?.[0]?.displayName ?? "";
      setReverseAddress(resolved);
      const normalize = (value: string) => value.normalize("NFD").replace(/[\u0300-\u036F]/g, "")
        .toLowerCase().replace(/[^a-z0-9 ]/g, " ");
      const expectedTokens = new Set(normalize(form.addressDisplay).split(/\s+/).filter((token) => token.length >= 4));
      const resolvedTokens = new Set(normalize(resolved).split(/\s+/).filter(Boolean));
      const matches = [...expectedTokens].filter((token) => resolvedTokens.has(token)).length;
      if (resolved && expectedTokens.size > 0 && matches / expectedTokens.size < 0.25) {
        setLocationMismatchWarning("Tọa độ vừa chọn có vẻ không khớp địa chỉ đã nhập. Hãy kiểm tra kỹ trước khi xác nhận.");
      }
    } catch {
      setLocationMismatchWarning("Không reverse geocode được vị trí mới. Chưa thể xác nhận thủ công.");
    }
  }

  return (
    <form className="tw-flex tw-min-h-0 tw-flex-1 tw-flex-col" onSubmit={(event) => void handleSubmit(event)}>
      <div className="tw-grid tw-gap-4 tw-overflow-y-auto tw-px-1 tw-pb-5">
        <label className="tw-m-0 tw-grid tw-gap-2">
          <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-600">Tên bãi xe</span>
          <input
            className="tw-h-[42px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)]"
            placeholder="Bãi xe HCMUTE"
            value={form.name}
            onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
          />
        </label>

        <section className="tw-grid tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-blue-100 tw-bg-blue-50 tw-p-4">
          <div>
            <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-700">Địa chỉ bãi xe</span>
            <p className="tw-m-0 tw-mt-1 tw-text-[0.75rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">
              Nhập địa chỉ để lưu thông tin bãi. Việc tìm tọa độ tự động là tùy chọn.
            </p>
          </div>
          <ParkingAddressPicker
            key={formKey}
            value={{
              addressDisplay: form.addressDisplay,
              addressInputScheme: form.addressInputScheme ?? "CURRENT",
              currentWardCode: form.currentWardCode ?? null,
              legacyWardCode: form.legacyWardCode ?? null,
            }}
            onChange={(address) => {
              setLocationSearchError("");
              setReverseAddress("");
              setLocationMismatchWarning("");
              setForm((current) => ({
                ...current,
                ...address,
                latitude: null,
                longitude: null,
                locationResolution: "NONE",
              }));
            }}
          />
          <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-3">
            <Button disabled={!form.addressDisplay.trim() || searchingLocation} loading={searchingLocation} type="button" onClick={() => void handleSearchLocation()}>
              <i className="fas fa-location-arrow" />
              Tìm tọa độ tự động
            </Button>
            <a
              className="tw-inline-flex tw-items-center tw-gap-2 tw-text-[0.78rem] tw-font-extrabold tw-text-vm-primary hover:tw-text-vm-primary-hover hover:tw-no-underline"
              href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(form.addressDisplay)}`}
              rel="noreferrer"
              target="_blank"
            >
              <i className="fas fa-external-link-alt" />
              Kiểm tra địa chỉ trên Google Maps
            </a>
            {form.latitude !== null && form.longitude !== null ? (
              <a
                className="tw-inline-flex tw-items-center tw-gap-2 tw-text-[0.78rem] tw-font-extrabold tw-text-vm-primary hover:tw-text-vm-primary-hover hover:tw-no-underline"
                href={`https://www.google.com/maps?q=${form.latitude},${form.longitude}`}
                rel="noreferrer"
                target="_blank"
              >
                <i className="fas fa-external-link-alt" />
                Kiểm tra vị trí đã tìm
              </a>
            ) : null}
          </div>
          {locationSearchError ? <span className="tw-text-[0.75rem] tw-font-semibold tw-text-red-600">{locationSearchError}</span> : null}
          {form.latitude !== null && form.longitude !== null ? (
            <>
              <ParkingLocationMap latitude={form.latitude} longitude={form.longitude} onMarkerMoved={(lat, lon) => void handleMarkerMoved(lat, lon)} />
              <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-emerald-100 tw-bg-emerald-50 tw-p-3 tw-text-[0.78rem] tw-font-semibold tw-leading-5 tw-text-emerald-800">
                <div><i className="fas fa-map-marker-alt tw-mr-2" />{form.latitude.toFixed(6)}, {form.longitude.toFixed(6)}</div>
                {reverseAddress ? <div className="tw-mt-1">Reverse geocode: {reverseAddress}</div> : null}
              </div>
              {locationMismatchWarning ? <div className="tw-rounded-vm-md tw-bg-amber-100 tw-p-3 tw-text-xs tw-font-bold tw-text-amber-900">{locationMismatchWarning}</div> : null}
              <Button
                disabled={!reverseAddress || Boolean(locationMismatchWarning)}
                type="button"
                variant="secondary"
                onClick={() => setForm((current) => ({ ...current, locationResolution: "MANUAL" }))}
              >
                <i className="fas fa-check-circle" />
                {form.locationResolution === "MANUAL" ? "Đã xác nhận thủ công" : "Xác nhận vị trí thủ công"}
              </Button>
            </>
          ) : (
            <div className="tw-flex tw-items-start tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-100 tw-bg-amber-50 tw-p-3 tw-text-[0.78rem] tw-font-semibold tw-leading-5 tw-text-amber-800">
              <i className="fas fa-info-circle tw-mt-0.5" />
              <span>Chưa có tọa độ. Bạn vẫn có thể tạo bãi; chỉ chức năng tìm bãi gần và tính khoảng cách sẽ chờ định vị bổ sung.</span>
            </div>
          )}
        </section>
        <label className="tw-m-0 tw-grid tw-gap-2">
          <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-600">Tổng sức chứa ban đầu</span>
          <input
            className="tw-h-[42px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)]"
            min={0}
            type="number"
            value={form.totalCapacity || ""}
            onChange={(event) => setForm((current) => ({ ...current, totalCapacity: Number(event.target.value) }))}
          />
        </label>

        {statusMeta ? (
          <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3">
            <span className="tw-text-[0.74rem] tw-font-extrabold tw-text-vm-slate-500">Trạng thái hiện tại</span>
            <div className="tw-mt-2">
              <Badge tone={lotStatusTone(statusMeta.status)} className="tw-rounded-full tw-px-3">{lotStatusLabel(statusMeta.status)}</Badge>
              <span className="tw-ml-2 tw-text-xs tw-font-bold tw-text-slate-600">Geocoding: {statusMeta.geocodingStatus ?? "NOT_REQUESTED"}</span>
            </div>
          </div>
        ) : null}

        {formError || serverError ? <div className="tw-rounded-vm-md tw-bg-red-50 tw-p-3 tw-text-[0.8rem] tw-font-bold tw-text-red-600">{formError || serverError}</div> : null}
      </div>

      <div className="tw-grid tw-grid-cols-2 tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-py-4">
        <Button variant="secondary" type="button" disabled={submitting} onClick={onClose}>Hủy</Button>
        <Button loading={submitting} type="submit">{submitting ? "Đang lưu..." : submitLabel}</Button>
      </div>
    </form>
  );
}
