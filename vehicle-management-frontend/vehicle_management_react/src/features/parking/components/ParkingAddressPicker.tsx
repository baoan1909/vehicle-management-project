import { useEffect, useMemo, useRef, useState } from "react";

import { SelectMenu } from "@/components/ui";
import { AddressModeButtons } from "@/shared/components/ui/AddressModeButtons";
import {
  emptyAddressOption,
  getDivisionName,
  loadCurrentProvinces,
  loadCurrentWardPath,
  loadCurrentWards,
  loadLegacyDistricts,
  loadLegacyProvinces,
  loadLegacyWardPath,
  loadLegacyWards,
  toAddressOptions,
  type AdministrativeDivision,
  type AdministrativeDivisionPath,
} from "@/shared/data/vietnamAddress";

export type ParkingAddressValue = {
  addressDisplay: string;
  addressInputScheme: "CURRENT" | "LEGACY";
  currentWardCode: string | null;
  legacyWardCode: string | null;
};

type Props = {
  value: ParkingAddressValue;
  onChange: (value: ParkingAddressValue) => void;
  compact?: boolean;
};

export function extractAddressDetail(addressDisplay: string, path: AdministrativeDivisionPath) {
  const suffix = [path.ward.fullName, path.district?.fullName, path.province.fullName]
    .filter(Boolean)
    .join(", ");
  if (addressDisplay === suffix) return "";
  const suffixWithSeparator = `, ${suffix}`;
  return addressDisplay.endsWith(suffixWithSeparator)
    ? addressDisplay.slice(0, -suffixWithSeparator.length).trim()
    : addressDisplay;
}

export function ParkingAddressPicker({ value, onChange, compact = false }: Props) {
  const [provinces, setProvinces] = useState<AdministrativeDivision[]>([]);
  const [districts, setDistricts] = useState<AdministrativeDivision[]>([]);
  const [wards, setWards] = useState<AdministrativeDivision[]>([]);
  const [provinceCode, setProvinceCode] = useState("");
  const [districtCode, setDistrictCode] = useState("");
  const selectedWardCode = value.addressInputScheme === "LEGACY" ? value.legacyWardCode : value.currentWardCode;
  const [wardCode, setWardCode] = useState(selectedWardCode ?? "");
  const [detail, setDetail] = useState(value.addressDisplay);
  const addressDisplayRef = useRef(value.addressDisplay);
  addressDisplayRef.current = value.addressDisplay;
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setLoadError(null);
    const loader = value.addressInputScheme === "LEGACY" ? loadLegacyProvinces : loadCurrentProvinces;
    loader().then((items) => mounted && setProvinces(items))
      .catch(() => {
        if (mounted) {
          setProvinces([]);
          setLoadError("Không tải được danh mục tỉnh/thành. Vui lòng thử lại sau.");
        }
      })
      .finally(() => mounted && setLoading(false));
    return () => { mounted = false; };
  }, [value.addressInputScheme]);

  useEffect(() => {
    let mounted = true;
    if (!selectedWardCode) return () => { mounted = false; };

    setLoading(true);
    setLoadError(null);
    const loader = value.addressInputScheme === "LEGACY"
      ? loadLegacyWardPath(selectedWardCode)
      : loadCurrentWardPath(selectedWardCode);
    loader.then((path) => {
      if (!mounted) return;
      setProvinceCode(path.province.code);
      setDistrictCode(path.district?.code ?? "");
      setWardCode(path.ward.code);
      // The path request can finish after the user has edited the street.
      setDetail(extractAddressDetail(addressDisplayRef.current, path));
    }).catch(() => {
      if (mounted) setLoadError("Không khôi phục được tỉnh/huyện/xã từ mã phường. Vui lòng chọn lại địa chỉ.");
    }).finally(() => mounted && setLoading(false));

    return () => { mounted = false; };
  }, [selectedWardCode, value.addressInputScheme]);

  useEffect(() => {
    let mounted = true;
    setDistricts([]);
    setWards([]);
    if (!provinceCode) return () => { mounted = false; };
    setLoading(true);
    setLoadError(null);
    const loader = value.addressInputScheme === "LEGACY"
      ? loadLegacyDistricts(provinceCode)
      : loadCurrentWards(provinceCode);
    loader.then((items) => {
      if (!mounted) return;
      if (value.addressInputScheme === "LEGACY") setDistricts(items);
      else setWards(items);
    }).catch(() => {
      if (mounted) setLoadError("Không tải được đơn vị hành chính cấp dưới. Vui lòng thử lại sau.");
    }).finally(() => mounted && setLoading(false));
    return () => { mounted = false; };
  }, [provinceCode, value.addressInputScheme]);

  useEffect(() => {
    let mounted = true;
    setWards([]);
    if (value.addressInputScheme !== "LEGACY" || !districtCode) return () => { mounted = false; };
    setLoading(true);
    setLoadError(null);
    loadLegacyWards(districtCode).then((items) => mounted && setWards(items))
      .catch(() => mounted && setLoadError("Không tải được danh mục phường/xã. Vui lòng thử lại sau."))
      .finally(() => mounted && setLoading(false));
    return () => { mounted = false; };
  }, [districtCode, value.addressInputScheme]);

  const options = useMemo(() => ({
    provinces: toAddressOptions(provinces),
    districts: toAddressOptions(districts),
    wards: toAddressOptions(wards),
  }), [districts, provinces, wards]);

  function commit(
    nextDetail: string,
    nextWardCode: string,
    nextProvinceCode = provinceCode,
    nextDistrictCode = districtCode,
  ) {
    const addressDisplay = [
      nextDetail.trim(),
      getDivisionName(wards, nextWardCode),
      value.addressInputScheme === "LEGACY" ? getDivisionName(districts, nextDistrictCode) : "",
      getDivisionName(provinces, nextProvinceCode),
    ].filter(Boolean).join(", ");
    onChange({
      addressDisplay,
      addressInputScheme: value.addressInputScheme,
      currentWardCode: value.addressInputScheme === "CURRENT" ? nextWardCode || null : null,
      legacyWardCode: value.addressInputScheme === "LEGACY" ? nextWardCode || null : null,
    });
  }

  return (
    <div className={`tw-grid tw-gap-3 ${compact ? "tw-grid-cols-1" : "tw-grid-cols-2 max-[900px]:tw-grid-cols-1"}`}>
      <div className="tw-col-span-full">
        <AddressModeButtons value={value.addressInputScheme === "CURRENT" ? "current" : "legacy"} stretch={compact}
            onChange={(mode) => {
              const scheme = mode === "current" ? "CURRENT" : "LEGACY";
              setProvinceCode(""); setDistrictCode(""); setWardCode(""); setDetail("");
              onChange({ addressDisplay: "", addressInputScheme: scheme, currentWardCode: null, legacyWardCode: null });
            }}
        />
      </div>
      <div className="tw-grid tw-gap-1.5">
        {compact ? <span className="tw-text-xs tw-font-extrabold tw-text-vm-slate-700">Tỉnh/thành phố</span> : null}
      <SelectMenu ariaLabel="Tỉnh hoặc thành phố" clearValue="" value={provinceCode}
        disabled={compact && loading} portal={compact} searchable={compact ? true : "auto"} searchPlaceholder="Tìm tỉnh/thành phố..."
        onChange={(code) => { setProvinceCode(code); setDistrictCode(""); setWardCode(""); commit(detail, "", code, ""); }}
        options={[{ ...emptyAddressOption, label: loading ? "Đang tải..." : "Chọn tỉnh/thành" }, ...options.provinces]} />
      </div>
      {value.addressInputScheme === "LEGACY" ? (
        <div className="tw-grid tw-gap-1.5">
        {compact ? <span className="tw-text-xs tw-font-extrabold tw-text-vm-slate-700">Quận/huyện</span> : null}
        <SelectMenu ariaLabel="Quận hoặc huyện" clearValue="" value={districtCode}
          disabled={compact && (!provinceCode || loading)} portal={compact} searchable={compact ? true : "auto"} searchPlaceholder="Tìm quận/huyện..."
          onChange={(code) => { setDistrictCode(code); setWardCode(""); commit(detail, "", provinceCode, code); }}
          options={[{ ...emptyAddressOption, label: provinceCode ? "Chọn quận/huyện" : "Chọn tỉnh trước" }, ...options.districts]} />
        </div>
      ) : null}
      <div className={`tw-grid tw-gap-1.5 ${value.addressInputScheme === "CURRENT" ? "tw-col-span-full" : ""}`}>
        {compact ? <span className="tw-text-xs tw-font-extrabold tw-text-vm-slate-700">Phường/xã</span> : null}
        <SelectMenu ariaLabel="Phường hoặc xã" clearValue="" value={wardCode}
          disabled={compact && (loading || (value.addressInputScheme === "LEGACY" ? !districtCode : !provinceCode))}
          portal={compact} searchable={compact ? true : "auto"} searchPlaceholder="Tìm phường/xã..."
          onChange={(code) => { setWardCode(code); commit(detail, code); }}
          options={[{ ...emptyAddressOption, label: "Chọn phường/xã" }, ...options.wards]} />
      </div>
      {compact ? <span className="tw-text-xs tw-font-extrabold tw-text-vm-slate-700">Địa chỉ chi tiết <span className="tw-font-normal tw-text-vm-slate-500">(không bắt buộc)</span></span> : null}
      <input className="tw-col-span-full tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200 tw-px-3"
        value={detail} placeholder="Số nhà, tên đường" aria-label="Số nhà, tên đường"
        onChange={(event) => { setDetail(event.target.value); commit(event.target.value, wardCode); }} />
      {loadError ? <div className="tw-col-span-full tw-rounded-vm-md tw-bg-red-50 tw-p-3 tw-text-xs tw-font-bold tw-text-red-800" role="alert">{loadError}</div> : null}
      {value.addressDisplay && !compact ? <div className="tw-col-span-full tw-rounded-vm-md tw-bg-blue-50 tw-p-3 tw-text-xs tw-font-bold tw-text-blue-900">{value.addressDisplay}</div> : null}
    </div>
  );
}
