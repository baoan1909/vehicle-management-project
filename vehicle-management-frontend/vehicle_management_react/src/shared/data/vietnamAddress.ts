import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

export type VietnamAddressOption = { label: string; value: string };

export type AdministrativeDivision = {
  code: string;
  name: string;
  fullName: string;
};

export type AdministrativeDivisionPath = {
  province: AdministrativeDivision;
  district: AdministrativeDivision | null;
  ward: AdministrativeDivision;
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

let currentProvinces: AdministrativeDivision[] | null = null;
let legacyProvinces: AdministrativeDivision[] | null = null;
const currentWards = new Map<string, AdministrativeDivision[]>();
const legacyDistricts = new Map<string, AdministrativeDivision[]>();
const legacyWards = new Map<string, AdministrativeDivision[]>();
const currentWardPaths = new Map<string, AdministrativeDivisionPath>();
const legacyWardPaths = new Map<string, AdministrativeDivisionPath>();

export const emptyAddressOption: VietnamAddressOption = {
  label: "Chọn",
  value: ""
};

async function load(path: string) {
  const response = await apiClient<ApiResponse<AdministrativeDivision[]>>(path, { skipAuth: true });
  return response.data;
}

async function loadPath(path: string) {
  const response = await apiClient<ApiResponse<AdministrativeDivisionPath>>(path, { skipAuth: true });
  return response.data;
}

export async function loadCurrentProvinces() {
  currentProvinces ??= await load(apiEndpoints.public.administrativeDivisions.currentProvinces);
  return currentProvinces;
}

export async function loadCurrentWards(provinceCode: string) {
  if (!currentWards.has(provinceCode)) {
    currentWards.set(provinceCode, await load(apiEndpoints.public.administrativeDivisions.currentWards(provinceCode)));
  }
  return currentWards.get(provinceCode) ?? [];
}

export async function loadCurrentWardPath(wardCode: string) {
  if (!currentWardPaths.has(wardCode)) {
    currentWardPaths.set(wardCode, await loadPath(apiEndpoints.public.administrativeDivisions.currentWardPath(wardCode)));
  }
  return currentWardPaths.get(wardCode)!;
}

export async function loadLegacyProvinces() {
  legacyProvinces ??= await load(apiEndpoints.public.administrativeDivisions.legacyProvinces);
  return legacyProvinces;
}

export async function loadLegacyDistricts(provinceCode: string) {
  if (!legacyDistricts.has(provinceCode)) {
    legacyDistricts.set(provinceCode, await load(apiEndpoints.public.administrativeDivisions.legacyDistricts(provinceCode)));
  }
  return legacyDistricts.get(provinceCode) ?? [];
}

export async function loadLegacyWards(districtCode: string) {
  if (!legacyWards.has(districtCode)) {
    legacyWards.set(districtCode, await load(apiEndpoints.public.administrativeDivisions.legacyWards(districtCode)));
  }
  return legacyWards.get(districtCode) ?? [];
}

export async function loadLegacyWardPath(wardCode: string) {
  if (!legacyWardPaths.has(wardCode)) {
    legacyWardPaths.set(wardCode, await loadPath(apiEndpoints.public.administrativeDivisions.legacyWardPath(wardCode)));
  }
  return legacyWardPaths.get(wardCode)!;
}

export function toAddressOptions(divisions: AdministrativeDivision[]): VietnamAddressOption[] {
  return divisions.map((division) => ({ label: division.fullName, value: division.code }));
}

export function getDivisionName(divisions: AdministrativeDivision[], code: string) {
  return divisions.find((division) => division.code === code)?.fullName ?? "";
}