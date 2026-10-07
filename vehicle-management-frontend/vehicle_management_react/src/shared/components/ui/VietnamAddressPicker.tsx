import { useEffect, useMemo, useState } from "react";
import {
  emptyAddressOption,
  getDivisionName,
  loadCurrentProvinces,
  loadCurrentWards,
  loadLegacyProvinces,
  loadLegacyDistricts,
  loadLegacyWards,
  toAddressOptions,
  type AdministrativeDivision
} from "@/shared/data/vietnamAddress";
import { SelectMenu } from "@/shared/components/ui/SelectMenu";
import { cn } from "@/lib/cn";

export type VietnamAddressValue = {
  provinceCode: string;
  districtCode: string | null;
  wardCode: string;
  addressDetail: string;
};

export type VietnamAddressPickerProps = {
  value: VietnamAddressValue;
  onChange: (value: VietnamAddressValue) => void;
  label?: string;
  required?: boolean;
  disabled?: boolean;
  compact?: boolean;
  mode?: "current" | "legacy" | "auto";
  onModeChange?: (mode: "current" | "legacy") => void;
};

function buildDisplayAddress(
  provinces: AdministrativeDivision[],
  districts: AdministrativeDivision[],
  wards: AdministrativeDivision[],
  value: VietnamAddressValue,
  mode: "current" | "legacy"
): string {
  const parts = [value.addressDetail.trim()];
  const wardName = getDivisionName(wards, value.wardCode);
  if (wardName) parts.push(wardName);
  if (mode === "legacy") {
    const districtName = getDivisionName(districts, value.districtCode || "");
    if (districtName) parts.push(districtName);
  }
  const provinceName = getDivisionName(provinces, value.provinceCode);
  if (provinceName) parts.push(provinceName);
  return parts.filter(Boolean).join(", ");
}

export function VietnamAddressPicker({
  value,
  onChange,
  label = "Địa chỉ",
  required = false,
  disabled = false,
  compact = false,
  mode = "auto",
  onModeChange
}: VietnamAddressPickerProps) {
  const [provinces, setProvinces] = useState<AdministrativeDivision[]>([]);
  const [districts, setDistricts] = useState<AdministrativeDivision[]>([]);
  const [wards, setWards] = useState<AdministrativeDivision[]>([]);
  const [loadingProvinces, setLoadingProvinces] = useState(true);
  const [loadingDistricts, setLoadingDistricts] = useState(false);
  const [loadingWards, setLoadingWards] = useState(false);
  const [currentMode, setCurrentMode] = useState<"current" | "legacy">(
    mode === "auto" ? (value.districtCode ? "legacy" : "current") : mode
  );

  useEffect(() => {
    if (mode === "auto" && value.districtCode) {
      setCurrentMode("legacy");
    }
  }, [mode, value.districtCode]);

  const loadedProvinceOptions = useMemo(() => toAddressOptions(provinces), [provinces]);
  const loadedDistrictOptions = useMemo(() => toAddressOptions(districts), [districts]);
  const loadedWardOptions = useMemo(() => toAddressOptions(wards), [wards]);

  // Load provinces based on mode
  useEffect(() => {
    let mounted = true;
    setLoadingProvinces(true);
    const loadProvinces = currentMode === "current" ? loadCurrentProvinces : loadLegacyProvinces;
    loadProvinces()
      .then((data) => {
        if (!mounted) return;
        setProvinces(data);
      })
      .catch(() => {
        if (!mounted) return;
        setProvinces([]);
      })
      .finally(() => {
        if (mounted) setLoadingProvinces(false);
      });
    return () => { mounted = false; };
  }, [currentMode]);

  // Load districts when province changes (legacy mode only)
  useEffect(() => {
    if (currentMode !== "legacy") {
      setDistricts([]);
      return;
    }
    let mounted = true;
    if (!value.provinceCode) {
      setDistricts([]);
      setWards([]);
      setLoadingDistricts(false);
      setLoadingWards(false);
      return () => { mounted = false; };
    }
    setLoadingDistricts(true);
    loadLegacyDistricts(value.provinceCode)
      .then((data) => {
        if (!mounted) return;
        setDistricts(data);
      })
      .catch(() => {
        if (!mounted) return;
        setDistricts([]);
      })
      .finally(() => {
        if (mounted) setLoadingDistricts(false);
      });
    return () => { mounted = false; };
  }, [value.provinceCode, currentMode]);

  // Load wards when district changes (legacy) or province changes (current)
  useEffect(() => {
    let mounted = true;
    if (currentMode === "legacy") {
      if (!value.districtCode) {
        setWards([]);
        setLoadingWards(false);
        return () => { mounted = false; };
      }
      setLoadingWards(true);
      loadLegacyWards(value.districtCode)
        .then((data) => {
          if (!mounted) return;
          setWards(data);
        })
        .catch(() => {
          if (!mounted) return;
          setWards([]);
        })
        .finally(() => {
          if (mounted) setLoadingWards(false);
        });
    } else {
      if (!value.provinceCode) {
        setWards([]);
        setLoadingWards(false);
        return () => { mounted = false; };
      }
      setLoadingWards(true);
      loadCurrentWards(value.provinceCode)
        .then((data) => {
          if (!mounted) return;
          setWards(data);
        })
        .catch(() => {
          if (!mounted) return;
          setWards([]);
        })
        .finally(() => {
          if (mounted) setLoadingWards(false);
        });
    }
    return () => { mounted = false; };
  }, [currentMode, value.districtCode, value.provinceCode]);

  const commit = (nextValue: VietnamAddressValue) => {
    onChange(nextValue);
  };

  const handleModeChange = (newMode: "current" | "legacy") => {
    if (newMode === currentMode) return;
    setCurrentMode(newMode);
    onModeChange?.(newMode);
    // Reset all fields when mode changes
    commit({
      provinceCode: "",
      districtCode: null,
      wardCode: "",
      addressDetail: ""
    });
  };

  const isCurrentMode = currentMode === "current";
  const displayAddress = buildDisplayAddress(provinces, districts, wards, value, currentMode);
  const fieldLabelClassName = compact
    ? "tw-text-[0.76rem] tw-font-semibold tw-text-[#334a6e]"
    : "tw-text-[0.75rem] tw-font-black tw-uppercase tw-text-vm-slate-500";
  const selectTriggerClassName = compact
    ? "!tw-h-8 !tw-rounded-md !tw-border-[#dce4ef] !tw-px-3 !tw-text-[0.86rem] !tw-font-semibold !tw-text-[#223554] !tw-shadow-none"
    : undefined;

  return (
    <div className={cn("tw-grid", compact ? "tw-gap-2" : "tw-gap-3")}>
      <div className={cn("tw-grid", compact ? "tw-gap-1.5" : "tw-gap-2")}>
        <div className={cn("tw-flex tw-items-center tw-gap-2", compact ? "tw-text-[0.76rem] tw-font-semibold tw-text-[#334a6e]" : "tw-text-[0.86rem] tw-font-black tw-text-vm-slate-700")}>
          <span>{label} <span className="tw-text-xs tw-font-normal tw-text-vm-slate-400">({isCurrentMode ? "Hiện hành" : "Cũ"})</span></span>
          {mode === "auto" && (
            <SelectMenu
              ariaLabel="Chọn kiểu địa chỉ"
              value={currentMode}
              onChange={(v) => handleModeChange(v as "current" | "legacy")}
              options={[
                { label: "Hiện hành (Tỉnh → Phường/Xã)", value: "current" },
                { label: "Cũ (Tỉnh → Quận/Huyện → Phường/Xã)", value: "legacy" }
              ]}
              disabled={disabled}
              className="tw-w-[220px]"
              portal
              triggerClassName={selectTriggerClassName}
            />
          )}
        </div>

        <div className={cn("tw-grid tw-grid-cols-2 max-[560px]:tw-grid-cols-1", compact ? "tw-gap-2" : "tw-gap-3")}>
          <div className={cn("tw-grid", compact ? "tw-gap-1.5" : "tw-gap-2")}>
            <span className={fieldLabelClassName}>Tỉnh/Thành phố</span>
            <SelectMenu
              ariaLabel="Tỉnh hoặc thành phố"
              clearValue=""
              value={value.provinceCode}
              onChange={(provinceCode) => {
                commit({ ...value, provinceCode, districtCode: null, wardCode: "" });
              }}
              options={[
                { ...emptyAddressOption, label: loadingProvinces ? "Đang tải..." : "Chọn tỉnh/thành phố" },
                ...loadedProvinceOptions
              ]}
              disabled={disabled || loadingProvinces}
              portal
              triggerClassName={selectTriggerClassName}
            />
          </div>

          {isCurrentMode ? (
            <>
              <div className={cn("tw-grid", compact ? "tw-gap-1.5" : "tw-gap-2")}>
                <span className={fieldLabelClassName}>Phường/Xã</span>
                <SelectMenu
                  ariaLabel="Phường hoặc xã"
                  clearValue=""
                  value={value.wardCode}
                  onChange={(wardCode) => {
                    commit({ ...value, wardCode });
                  }}
                  options={[
                    {
                      ...emptyAddressOption,
                      label: loadingWards
                        ? "Đang tải..."
                        : value.provinceCode
                        ? "Chọn phường/xã"
                        : "Chọn tỉnh/thành phố trước"
                    },
                    ...loadedWardOptions
                  ]}
                  disabled={disabled || loadingWards || !value.provinceCode}
                  portal
                  triggerClassName={selectTriggerClassName}
                />
              </div>
            </>
          ) : (
            <>
              <div className={cn("tw-grid", compact ? "tw-gap-1.5" : "tw-gap-2")}>
                <span className={fieldLabelClassName}>Quận/Huyện</span>
                <SelectMenu
                  ariaLabel="Quận hoặc huyện"
                  clearValue=""
                  value={value.districtCode || ""}
                  onChange={(districtCode) => {
                    commit({ ...value, districtCode: districtCode || null, wardCode: "" });
                  }}
                  options={[
                    {
                      ...emptyAddressOption,
                      label: loadingDistricts
                        ? "Đang tải..."
                        : value.provinceCode
                        ? "Chọn quận/huyện"
                        : "Chọn tỉnh/thành phố trước"
                    },
                    ...loadedDistrictOptions
                  ]}
                  disabled={disabled || loadingDistricts || !value.provinceCode}
                  portal
                  triggerClassName={selectTriggerClassName}
                />
              </div>
              <div className={cn("tw-grid", compact ? "tw-gap-1.5" : "tw-gap-2")}>
                <span className={fieldLabelClassName}>Phường/Xã</span>
                <SelectMenu
                  ariaLabel="Phường hoặc xã"
                  clearValue=""
                  value={value.wardCode}
                  onChange={(wardCode) => {
                    commit({ ...value, wardCode });
                  }}
                  options={[
                    {
                      ...emptyAddressOption,
                      label: loadingWards
                        ? "Đang tải..."
                        : value.districtCode
                        ? "Chọn phường/xã"
                        : "Chọn quận/huyện trước"
                    },
                    ...loadedWardOptions
                  ]}
                  disabled={disabled || loadingWards || !value.districtCode}
                  portal
                  triggerClassName={selectTriggerClassName}
                />
              </div>
            </>
          )}
        </div>

        <div className={cn("tw-grid", compact ? "tw-gap-1.5" : "tw-gap-2")}>
          <span className={fieldLabelClassName}>Địa chỉ cụ thể</span>
          <input
            aria-label="Địa chỉ cụ thể"
            className={cn(
              "tw-w-full tw-border tw-border-solid tw-bg-white tw-px-3 tw-font-semibold tw-outline-none tw-transition placeholder:tw-text-vm-slate-500 hover:tw-border-vm-slate-200 focus:tw-border-vm-primary focus:tw-shadow-vm-focus disabled:tw-bg-vm-slate-25",
              compact
                ? "tw-h-8 tw-rounded-md tw-border-[#dce4ef] tw-text-[0.86rem] tw-text-[#223554]"
                : "tw-h-[42px] tw-rounded-vm-md tw-border-vm-slate-100 tw-text-[0.88rem] tw-text-vm-slate-900",
            )}
            value={value.addressDetail}
            placeholder="Số nhà, tên đường"
            onChange={(event) => {
              commit({ ...value, addressDetail: event.target.value });
            }}
            disabled={disabled}
            required={required}
          />
        </div>

        {displayAddress && !disabled && (
          <div className="tw-flex tw-min-h-[38px] tw-items-start tw-gap-2 tw-rounded-vm-md tw-border tw-border-brand-100 tw-bg-brand-50 tw-px-3 tw-py-2.5 tw-text-[0.82rem] tw-font-bold tw-leading-6 tw-text-blue-900">
            <i className="fas fa-map-marker-alt tw-mt-1 tw-text-vm-primary" />
            <span>{displayAddress}</span>
          </div>
        )}
      </div>
    </div>
  );
}
