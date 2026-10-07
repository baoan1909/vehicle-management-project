import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";
import type { AvatarModerationStatus } from "@/features/iam/api/accountProfileApi";

type ApiResponse<T> = { data: T; message: string; success: boolean; timestamp: string };

export async function fetchAvatarApprovals(status: AvatarModerationStatus["approvalStatus"] | "ALL" = "PENDING") {
  const query = status === "ALL" ? "" : `?status=${status}`;
  const response = await apiClient<ApiResponse<AvatarModerationStatus[]>>(
    `${apiEndpoints.operations.avatarApprovals}${query}`,
  );
  return response.data;
}

export async function reviewAvatarApproval(
  approvalRequestId: string,
  decision: "approve" | "reject",
  note?: string,
) {
  const response = await apiClient<ApiResponse<AvatarModerationStatus>>(
    `${apiEndpoints.operations.avatarApprovals}/${approvalRequestId}/${decision}`,
    { body: { note: note?.trim() || null }, method: "POST" },
  );
  return response.data;
}
