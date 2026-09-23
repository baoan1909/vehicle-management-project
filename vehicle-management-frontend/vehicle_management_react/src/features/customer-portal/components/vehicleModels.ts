import type { VehicleKind } from "./vehicleAppearance";

export const vehicleModels: Record<Exclude<VehicleKind, "other">, { image: string; label: string; luminanceGain: number; whiteBackground: boolean }> = {
  car: { image: "/assets/customer/portal/default-sedan-white-v1.png", label: "Ô tô", luminanceGain: 1, whiteBackground: false },
  motorcycle: { image: "/assets/customer/portal/motor-scooter-blue-v1.png", label: "Xe máy", luminanceGain: 1.85, whiteBackground: false },
  truck: { image: "/assets/customer/portal/representative-light-truck-v1.png", label: "Xe tải nhỏ", luminanceGain: 1.2, whiteBackground: true },
  bicycle: { image: "/assets/customer/portal/representative-bicycle-v1.png", label: "Xe đạp", luminanceGain: 1.15, whiteBackground: true },
};
