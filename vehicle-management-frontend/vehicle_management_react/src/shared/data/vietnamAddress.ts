export type VietnamAddressOption = {
  label: string;
  value: string;
};

export type VietnamProvince = {
  id: number;
  name: string;
};

export type VietnamWard = {
  id: number;
  name: string;
  province_id: number;
};

export type VietnamAddressData = {
  provinces: VietnamProvince[];
  wards: VietnamWard[];
};

let cachedAddressData: VietnamAddressData | null = null;

export const emptyAddressOption: VietnamAddressOption = {
  label: "Chọn",
  value: ""
};

export async function loadVietnamAddressData() {
  if (cachedAddressData) return cachedAddressData;

  const response = await fetch("/assets/data/vietnam-addresses-2025.json");
  if (!response.ok) {
    throw new Error("Không thể tải dữ liệu địa giới Việt Nam.");
  }

  cachedAddressData = (await response.json()) as VietnamAddressData;
  return cachedAddressData;
}

export function toProvinceOptions(data: VietnamAddressData): VietnamAddressOption[] {
  return data.provinces.map((province) => ({
    label: province.name,
    value: String(province.id)
  }));
}

export function getWardOptions(data: VietnamAddressData, provinceId: string): VietnamAddressOption[] {
  const selectedProvinceId = Number(provinceId);

  if (!selectedProvinceId) return [];

  return data.wards
    .filter((ward) => ward.province_id === selectedProvinceId)
    .map((ward) => ({
      label: ward.name,
      value: String(ward.id)
    }));
}

export function getProvinceName(data: VietnamAddressData | null, provinceId: string) {
  return data?.provinces.find((province) => String(province.id) === provinceId)?.name ?? "";
}

export function getWardName(data: VietnamAddressData | null, wardId: string) {
  return data?.wards.find((ward) => String(ward.id) === wardId)?.name ?? "";
}
