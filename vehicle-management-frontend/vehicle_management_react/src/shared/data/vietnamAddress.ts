export type VietnamAddressOption = {
  label: string;
  value: string;
};

export type VietnamProvince = {
  id: number;
  name: string;
};

export type VietnamDistrict = {
  id: number;
  name: string;
  old_province_id: number;
};

export type VietnamWard = {
  id: number;
  name: string;
  old_district_id: number;
};

export type VietnamAddressData = {
  districts: VietnamDistrict[];
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

  const response = await fetch("/assets/data/vietnam-old-provinces.json");
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

export function getDistrictOptions(data: VietnamAddressData, provinceId: string): VietnamAddressOption[] {
  const selectedProvinceId = Number(provinceId);

  if (!selectedProvinceId) return [];

  return data.districts
    .filter((district) => district.old_province_id === selectedProvinceId)
    .map((district) => ({
      label: district.name,
      value: String(district.id)
    }));
}

export function getWardOptions(data: VietnamAddressData, districtId: string): VietnamAddressOption[] {
  const selectedDistrictId = Number(districtId);

  if (!selectedDistrictId) return [];

  return data.wards
    .filter((ward) => ward.old_district_id === selectedDistrictId)
    .map((ward) => ({
      label: ward.name,
      value: String(ward.id)
    }));
}

export function getProvinceName(data: VietnamAddressData | null, provinceId: string) {
  return data?.provinces.find((province) => String(province.id) === provinceId)?.name ?? "";
}

export function getDistrictName(data: VietnamAddressData | null, districtId: string) {
  return data?.districts.find((district) => String(district.id) === districtId)?.name ?? "";
}

export function getWardName(data: VietnamAddressData | null, wardId: string) {
  return data?.wards.find((ward) => String(ward.id) === wardId)?.name ?? "";
}
