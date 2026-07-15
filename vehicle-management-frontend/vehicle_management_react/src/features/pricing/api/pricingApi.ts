import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";
import type { PricePlanAppliesTo } from "@/features/pricing/components/pricingManageData";

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
};

export type PricePlanFilter = {
  appliesTo?: PricePlanAppliesTo;
  effectiveDate?: string;
  isActive?: boolean;
  keyword?: string;
};

export type CreatePricePlanRequest = {
  appliesTo: PricePlanAppliesTo;
  code: string;
  description?: string | null;
  effectiveFrom: string;
  effectiveTo?: string | null;
  name: string;
};

export type PriceRuleFilter = {
  isActive?: boolean;
  keyword?: string;
  pricePlanId?: string;
  ticketTypeId?: string;
  vehicleTypeId?: string;
};

export type CreatePriceRuleRequest = {
  basePrice: number;
  lostCardFee?: number | null;
  pricePlanId: string;
  priority?: number | null;
  ruleName: string;
  ticketTypeId: string;
  timeFrom?: string | null;
  timeTo?: string | null;
  unit?: string | null;
  vehicleTypeId: string;
};

export type UpdatePriceRuleRequest = Omit<CreatePriceRuleRequest, "pricePlanId">;

export type PricePlanApiResponse = {
  appliesTo: PricePlanAppliesTo;
  code: string;
  createdAt: string | null;
  createdBy: string | null;
  description: string | null;
  effectiveFrom: string | null;
  effectiveTo: string | null;
  isActive: boolean | null;
  name: string;
  pricePlanId: string;
  updatedAt: string | null;
  updatedBy: string | null;
};

export type PriceRuleApiResponse = {
  basePrice: number | null;
  createdAt: string | null;
  createdBy: string | null;
  isActive: boolean | null;
  lostCardFee: number | null;
  pricePlanId: string | null;
  priceRuleId: string;
  priority: number | null;
  ruleName: string;
  ticketTypeId: string | null;
  timeFrom: string | null;
  timeTo: string | null;
  unit: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
  vehicleTypeId: string | null;
};

export type VehicleTypeApiResponse = {
  code: string;
  description: string | null;
  isActive: boolean | null;
  name: string;
  vehicleTypeId: string;
};

export type TicketTypeApiResponse = {
  code: string;
  description: string | null;
  durationDays: number | null;
  name: string;
  status: string | null;
  ticketTypeId: string;
};

function buildQuery(filter: Record<string, string | number | boolean | null | undefined>) {
  const params = new URLSearchParams();

  Object.entries(filter).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.set(key, String(value));
    }
  });

  const query = params.toString();
  return query ? `?${query}` : "";
}

export function getPricePlans(filter: PricePlanFilter = {}) {
  return apiClient<ApiResponse<PricePlanApiResponse[]>>(
    `${apiEndpoints.catalog.pricePlans}${buildQuery(filter)}`,
  );
}

export function getPublicPricePlans(filter: PricePlanFilter = {}) {
  return apiClient<ApiResponse<PricePlanApiResponse[]>>(
    `${apiEndpoints.public.pricing.pricePlans}${buildQuery(filter)}`,
    { skipAuth: true },
  );
}

export function createPricePlan(payload: CreatePricePlanRequest) {
  return apiClient<ApiResponse<PricePlanApiResponse>>(apiEndpoints.catalog.pricePlans, {
    method: "POST",
    body: payload,
  });
}

export function getPriceRules(filter: PriceRuleFilter = {}) {
  return apiClient<ApiResponse<PriceRuleApiResponse[]>>(
    `${apiEndpoints.catalog.priceRules}${buildQuery(filter)}`,
  );
}

export function getPublicPriceRules(filter: PriceRuleFilter = {}) {
  return apiClient<ApiResponse<PriceRuleApiResponse[]>>(
    `${apiEndpoints.public.pricing.priceRules}${buildQuery(filter)}`,
    { skipAuth: true },
  );
}

export function createPriceRule(payload: CreatePriceRuleRequest) {
  return apiClient<ApiResponse<PriceRuleApiResponse>>(apiEndpoints.catalog.priceRules, {
    method: "POST",
    body: payload,
  });
}

export function updatePriceRule(priceRuleId: string, payload: UpdatePriceRuleRequest) {
  return apiClient<ApiResponse<PriceRuleApiResponse>>(`${apiEndpoints.catalog.priceRules}/${priceRuleId}`, {
    method: "PUT",
    body: payload,
  });
}

export function getPricingVehicleTypes() {
  return apiClient<ApiResponse<VehicleTypeApiResponse[]>>(
    `${apiEndpoints.catalog.vehicleTypes}${buildQuery({ isActive: true })}`,
  );
}

export function getPricingTicketTypes() {
  return apiClient<ApiResponse<TicketTypeApiResponse[]>>(
    `${apiEndpoints.catalog.ticketTypes}${buildQuery({ status: "ACTIVE" })}`,
  );
}

export function getPublicPricingVehicleTypes() {
  return apiClient<ApiResponse<VehicleTypeApiResponse[]>>(
    `${apiEndpoints.public.pricing.vehicleTypes}${buildQuery({ isActive: true })}`,
    { skipAuth: true },
  );
}

export function getPublicPricingTicketTypes() {
  return apiClient<ApiResponse<TicketTypeApiResponse[]>>(
    `${apiEndpoints.public.pricing.ticketTypes}${buildQuery({ status: "ACTIVE" })}`,
    { skipAuth: true },
  );
}
