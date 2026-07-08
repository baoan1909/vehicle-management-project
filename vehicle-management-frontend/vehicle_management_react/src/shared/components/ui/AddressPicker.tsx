import { useEffect, useMemo, useState } from "react";
import {
  emptyAddressOption,
  getDistrictName,
  getDistrictOptions,
  getProvinceName,
  getWardName,
  getWardOptions,
  loadVietnamAddressData,
  toProvinceOptions,
  type VietnamAddressData
} from "@/shared/data/vietnamAddress";
import { SelectMenu } from "@/shared/components/ui/SelectMenu";

type AddressPickerState = {
  detail: string;
  districtId: string;
  provinceId: string;
  wardId: string;
};

type AddressPickerProps = {
  onChange: (value: string) => void;
  value: string;
};

function buildAddress(data: VietnamAddressData | null, { detail, districtId, provinceId, wardId }: AddressPickerState) {
  return [detail.trim(), getWardName(data, wardId), getDistrictName(data, districtId), getProvinceName(data, provinceId)].filter(Boolean).join(", ");
}

export function AddressPicker({ onChange, value }: AddressPickerProps) {
  const [addressData, setAddressData] = useState<VietnamAddressData | null>(null);
  const [loading, setLoading] = useState(true);
  const [state, setState] = useState<AddressPickerState>({
    detail: value,
    districtId: "",
    provinceId: "",
    wardId: ""
  });

  const loadedProvinceOptions = useMemo(() => (addressData ? toProvinceOptions(addressData) : []), [addressData]);
  const districtOptions = useMemo(() => (addressData ? getDistrictOptions(addressData, state.provinceId) : []), [addressData, state.provinceId]);
  const wardOptions = useMemo(() => (addressData ? getWardOptions(addressData, state.districtId) : []), [addressData, state.districtId]);

  useEffect(() => {
    let mounted = true;

    loadVietnamAddressData()
      .then((data) => {
        if (!mounted) return;
        setAddressData(data);
      })
      .catch(() => {
        if (!mounted) return;
        setAddressData(null);
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });

    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    if (state.provinceId || state.districtId || state.wardId || state.detail === value) return;
    setState((current) => ({ ...current, detail: value }));
  }, [state.detail, state.districtId, state.provinceId, state.wardId, value]);

  const commit = (nextState: AddressPickerState) => {
    setState(nextState);
    onChange(buildAddress(addressData, nextState));
  };

  return (
    <div className="tw-grid tw-grid-cols-2 tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3 max-[900px]:tw-grid-cols-1">
      <label className="tw-grid tw-min-w-0 tw-gap-2">
        <span className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-700">Tỉnh/Thành phố</span>
        <SelectMenu
          ariaLabel="Tỉnh hoặc thành phố"
          clearValue=""
          value={state.provinceId}
          onChange={(provinceId) => {
            commit({ ...state, districtId: "", provinceId, wardId: "" });
          }}
          options={[{ ...emptyAddressOption, label: loading ? "Đang tải địa giới..." : "Chọn tỉnh/thành phố" }, ...loadedProvinceOptions]}
        />
      </label>

      <label className="tw-grid tw-min-w-0 tw-gap-2">
        <span className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-700">Quận/Huyện</span>
        <SelectMenu
          ariaLabel="Quận hoặc huyện"
          clearValue=""
          value={state.districtId}
          onChange={(districtId) => {
            commit({ ...state, districtId, wardId: "" });
          }}
          options={[{ ...emptyAddressOption, label: state.provinceId ? "Chọn quận/huyện" : "Chọn tỉnh/thành phố trước" }, ...districtOptions]}
        />
      </label>

      <label className="tw-col-span-full tw-grid tw-min-w-0 tw-gap-2">
        <span className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-700">Phường/Xã</span>
        <SelectMenu
          ariaLabel="Phường hoặc xã"
          clearValue=""
          value={state.wardId}
          onChange={(wardId) => {
            commit({ ...state, wardId });
          }}
          options={[{ ...emptyAddressOption, label: state.districtId ? "Chọn phường/xã" : "Chọn quận/huyện trước" }, ...wardOptions]}
        />
      </label>

      <label className="tw-col-span-full tw-grid tw-gap-2">
        <span className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-700">Địa chỉ cụ thể</span>
        <input
          className="tw-h-[42px] tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.88rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none tw-transition placeholder:tw-text-vm-slate-500 hover:tw-border-vm-slate-200 focus:tw-border-vm-primary focus:tw-shadow-vm-focus"
          value={state.detail}
          placeholder="Số nhà, tên đường"
          onChange={(event) => {
            commit({ ...state, detail: event.target.value });
          }}
        />
      </label>

      {buildAddress(addressData, state) ? (
        <div className="tw-col-span-full tw-flex tw-min-h-[38px] tw-items-start tw-gap-2 tw-rounded-vm-md tw-border tw-border-brand-100 tw-bg-brand-50 tw-px-3 tw-py-2.5 tw-text-[0.82rem] tw-font-bold tw-leading-6 tw-text-blue-900">
          <i className="fas fa-map-marker-alt tw-mt-1 tw-text-vm-primary" />
          <span>{buildAddress(addressData, state)}</span>
        </div>
      ) : null}
    </div>
  );
}
