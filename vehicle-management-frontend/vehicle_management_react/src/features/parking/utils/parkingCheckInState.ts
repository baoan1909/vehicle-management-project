import type {
  ParkingSessionCheckInPreviewResponse,
} from "@/features/parking/api/parkingSessionApi";

export function getSuggestedParkingSpaceId(
  preview: ParkingSessionCheckInPreviewResponse | null,
) {
  if (!preview || preview.trackingMode !== "SPACE") return undefined;
  const recommended = preview.recommendedParkingSpaceId;
  if (recommended && preview.eligibleSpaces.some((space) => space.parkingSpaceId === recommended)) {
    return recommended;
  }
  return preview.eligibleSpaces.length === 1
    ? preview.eligibleSpaces[0]?.parkingSpaceId
    : undefined;
}

export function isSelectedVisitorSpaceEligible(
  preview: ParkingSessionCheckInPreviewResponse | null,
  parkingSpaceId?: string,
) {
  if (!preview || preview.trackingMode !== "SPACE" || preview.customerType !== "VISITOR") {
    return false;
  }
  return Boolean(
    parkingSpaceId
      && preview.eligibleSpaces.some(
        (space) => space.parkingSpaceId === parkingSpaceId && space.status === "AVAILABLE",
      ),
  );
}

export function isCheckInPreviewAdmissible(
  preview: ParkingSessionCheckInPreviewResponse | null,
  selectedParkingSpaceId?: string,
) {
  if (!preview) return false;
  if (preview.trackingMode === "CAPACITY") {
    return preview.physicalAvailable > 0
      && (preview.customerType !== "VISITOR" || preview.visitorAvailable > 0);
  }
  if (preview.customerType === "VISITOR") {
    return isSelectedVisitorSpaceEligible(preview, selectedParkingSpaceId);
  }
  const allocatedSpaceId = getSuggestedParkingSpaceId(preview);
  return Boolean(
    allocatedSpaceId
      && preview.eligibleSpaces.some(
        (space) => space.parkingSpaceId === allocatedSpaceId && space.status === "RESERVED",
      ),
  );
}

export function getCheckInParkingSpaceId(
  preview: ParkingSessionCheckInPreviewResponse | null,
  selectedParkingSpaceId?: string,
) {
  if (preview?.trackingMode !== "SPACE" || preview.customerType !== "VISITOR") {
    return undefined;
  }
  return isSelectedVisitorSpaceEligible(preview, selectedParkingSpaceId)
    ? selectedParkingSpaceId
    : undefined;
}
