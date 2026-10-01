import { useEffect, useMemo, useState } from "react";

import { SelectMenu } from "@/components/ui";
import {
  emptyAddressOption,
  getDivisionName,
  loadCurrentProvinces,
  loadCurrentWards,
  loadLegacyDistricts,
  loadLegacyProvinces,
  loadLegacyWards,
  toAddressOptions,
  type AdministrativeDivision,
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
};

export function ParkingAddressPicker({ value, onChange }: Props) {
  const [provinces, setProvinces] = useState<AdministrativeDivision[]>([]);
  const [districts, setDistricts] = useState<AdministrativeDivision[]>([]);
  const [wards, setWards] = useState<AdministrativeDivision[]>([]);
  const [provinceCode, setProvinceCode] = useState("");
  const [districtCode, setDistrictCode] = useState("");
  const [wardCode, setWardCode] = useState(value.legacyWardCode ?? value.currentWardCode ?? "");
  const [detail, setDetail] = useState(value.addressDisplay);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    const loader = value.addressInputScheme === "LEGACY" ? loadLegacyProvinces : loadCurrentProvinces;
    loader().then((items) => mounted && setProvinces(items))
      .catch(() => mounted && setProvinces([]))
      .finally(() => mounted && setLoading(false));
    return () => { mounted = false; };
  }, [value.addressInputScheme]);

  useEffect(() => {
    let mounted = true;
    setDistricts([]);
    setWards([]);
    if (!provinceCode) return () => { mounted = false; };
    setLoading(true);
    const loader = value.addressInputScheme === "LEGACY"
      ? loadLegacyDistricts(provinceCode)
      : loadCurrentWards(provinceCode);
    loader.then((items) => {
      if (!mounted) return;
      if (value.addressInputScheme === "LEGACY") setDistricts(items);
      else setWards(items);
    }).catch(() => undefined).finally(() => mounted && setLoading(false));
    return () => { mounted = false; };
  }, [provinceCode, value.addressInputScheme]);

  useEffect(() => {
    let mounted = true;
    setWards([]);
    if (value.addressInputScheme !== "LEGACY" || !districtCode) return () => { mounted = false; };
    setLoading(true);
    loadLegacyWards(districtCode).then((items) => mounted && setWards(items))
      .catch(() => undefined).finally(() => mounted && setLoading(false));
    return () => { mounted = false; };
  }, [districtCode, value.addressInputScheme]);

  const options = useMemo(() => ({
    provinces: toAddressOptions(provinces),
    districts: toAddressOptions(districts),
    wards: toAddressOptions(wards),
  }), [districts, provinces, wards]);

  function commit(nextDetail: string, nextWardCode: string) {
    const addressDisplay = [
      nextDetail.trim(),
      getDivisionName(wards, nextWardCode),
      value.addressInputScheme === "LEGACY" ? getDivisionName(districts, districtCode) : "",
      getDivisionName(provinces, provinceCode),
    ].filter(Boolean).join(", ");
    onChange({
      addressDisplay,
      addressInputScheme: value.addressInputScheme,
      currentWardCode: value.addressInputScheme === "CURRENT" ? nextWardCode || null : null,
      legacyWardCode: value.addressInputScheme === "LEGACY" ? nextWardCode || null : null,
    });
  }

  return (
    <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[900px]:tw-grid-cols-1">
      <div className="tw-col-span-full tw-flex tw-gap-2">
        {(["CURRENT", "LEGACY"] as const).map((scheme) => (
          <button
            className={`tw-rounded-vm-md tw-border tw-border-solid tw-px-3 tw-py-2 tw-text-xs tw-font-extrabold ${value.addressInputScheme === scheme ? "tw-border-blue-500 tw-bg-blue-600 tw-text-white" : "tw-border-slate-200 tw-bg-white tw-text-slate-700"}`}
            key={scheme}
            type="button"
            onClick={() => {
              setProvinceCode(""); setDistrictCode(""); setWardCode(""); setDetail("");
              onChange({ addressDisplay: "", addressInputScheme: scheme, currentWardCode: null, legacyWardCode: null });
            }}
          >
            {scheme === "CURRENT" ? "Địa chỉ hiện hành" : "Địa chỉ cũ (có huyện)"}
          </button>
        ))}
      </div>
      <SelectMenu ariaLabel="Tỉnh hoặc thành phố" clearValue="" value={provinceCode}
        onChange={(code) => { setProvinceCode(code); setDistrictCode(""); setWardCode(""); commit(detail, ""); }}
        options={[{ ...emptyAddressOption, label: loading ? "Đang tải..." : "Chọn tỉnh/thành" }, ...options.provinces]} />
      {value.addressInputScheme === "LEGACY" ? (
        <SelectMenu ariaLabel="Quận hoặc huyện" clearValue="" value={districtCode}
          onChange={(code) => { setDistrictCode(code); setWardCode(""); commit(detail, ""); }}
          options={[{ ...emptyAddressOption, label: provinceCode ? "Chọn quận/huyện" : "Chọn tỉnh trước" }, ...options.districts]} />
      ) : null}
      <div className={value.addressInputScheme === "CURRENT" ? "tw-col-span-full" : ""}>
        <SelectMenu ariaLabel="Phường hoặc xã" clearValue="" value={wardCode}
          onChange={(code) => { setWardCode(code); commit(detail, code); }}
          options={[{ ...emptyAddressOption, label: "Chọn phường/xã" }, ...options.wards]} />
      </div>
      <input className="tw-col-span-full tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200 tw-px-3"
        value={detail} placeholder="Số nhà, tên đường"
        onChange={(event) => { setDetail(event.target.value); commit(event.target.value, wardCode); }} />
      {value.addressDisplay ? <div className="tw-col-span-full tw-rounded-vm-md tw-bg-blue-50 tw-p-3 tw-text-xs tw-font-bold tw-text-blue-900">{value.addressDisplay}</div> : null}
    </div>
  );
}
