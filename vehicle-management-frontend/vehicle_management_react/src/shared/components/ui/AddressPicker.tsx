import { useEffect, useMemo, useState } from "react";
import {
  emptyAddressOption,
  getDivisionName,
  loadCurrentProvinces,
  loadCurrentWards,
  toAddressOptions,
  type AdministrativeDivision
} from "@/shared/data/vietnamAddress";
import { SelectMenu } from "@/shared/components/ui/SelectMenu";

type AddressPickerState = {
  detail: string;
  provinceId: string;
  wardId: string;
};

type AddressPickerProps = {
  onChange: (value: string) => void;
  value: string;
};

function buildAddress(
  provinces: AdministrativeDivision[],
  wards: AdministrativeDivision[],
  { detail, provinceId, wardId }: AddressPickerState
) {
  return [detail.trim(), getDivisionName(wards, wardId), getDivisionName(provinces, provinceId)].filter(Boolean).join(", ");
}

export function AddressPicker({ onChange, value }: AddressPickerProps) {
  const [provinces, setProvinces] = useState<AdministrativeDivision[]>([]);
  const [wards, setWards] = useState<AdministrativeDivision[]>([]);
  const [loadingProvinces, setLoadingProvinces] = useState(true);
  const [loadingWards, setLoadingWards] = useState(false);
  const [state, setState] = useState<AddressPickerState>({
    detail: value,
    provinceId: "",
    wardId: ""
  });

  const loadedProvinceOptions = useMemo(() => toAddressOptions(provinces), [provinces]);
  const wardOptions = useMemo(() => toAddressOptions(wards), [wards]);

  useEffect(() => {
    let mounted = true;

    loadCurrentProvinces()
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

    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    let mounted = true;
    if (!state.provinceId) {
      setWards([]);
      setLoadingWards(false);
      return () => {
        mounted = false;
      };
    }

    setWards([]);
    setLoadingWards(true);
    loadCurrentWards(state.provinceId)
      .then((data) => {
        if (mounted) setWards(data);
      })
      .catch(() => {
        if (mounted) setWards([]);
      })
      .finally(() => {
        if (mounted) setLoadingWards(false);
      });

    return () => {
      mounted = false;
    };
  }, [state.provinceId]);

  useEffect(() => {
    if (state.provinceId || state.wardId || state.detail === value) return;
    setState((current) => ({ ...current, detail: value }));
  }, [state.detail, state.provinceId, state.wardId, value]);

  const commit = (nextState: AddressPickerState) => {
    setState(nextState);
    onChange(buildAddress(provinces, nextState.provinceId === state.provinceId ? wards : [], nextState));
  };

  return (
    <div className="tw-grid tw-grid-cols-2 tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3 max-[900px]:tw-grid-cols-1">
      <label className="tw-col-span-full tw-grid tw-min-w-0 tw-gap-2">
        <span className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-700">Tỉnh/Thành phố</span>
        <SelectMenu
          ariaLabel="Tỉnh hoặc thành phố"
          clearValue=""
          value={state.provinceId}
          onChange={(provinceId) => {
            commit({ ...state, provinceId, wardId: "" });
          }}
          options={[{ ...emptyAddressOption, label: loadingProvinces ? "Đang tải địa giới..." : "Chọn tỉnh/thành phố" }, ...loadedProvinceOptions]}
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
          options={[{
            ...emptyAddressOption,
            label: loadingWards ? "Đang tải phường/xã..." : state.provinceId ? "Chọn phường/xã" : "Chọn tỉnh/thành phố trước"
          }, ...wardOptions]}
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

      {buildAddress(provinces, wards, state) ? (
        <div className="tw-col-span-full tw-flex tw-min-h-[38px] tw-items-start tw-gap-2 tw-rounded-vm-md tw-border tw-border-brand-100 tw-bg-brand-50 tw-px-3 tw-py-2.5 tw-text-[0.82rem] tw-font-bold tw-leading-6 tw-text-blue-900">
          <i className="fas fa-map-marker-alt tw-mt-1 tw-text-vm-primary" />
          <span>{buildAddress(provinces, wards, state)}</span>
        </div>
      ) : null}
    </div>
  );
}
