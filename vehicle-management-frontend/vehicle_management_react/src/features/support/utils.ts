export type SupportParticipantType = "customer" | "employee";

interface OpenSupportCenterOptions {
  participantId?: string;
  participantName?: string;
  participantType?: SupportParticipantType;
}

export function openSupportCenterConversation(options: OpenSupportCenterOptions = {}) {
  const params = new URLSearchParams();

  if (options.participantType) params.set("participantType", options.participantType);
  if (options.participantId) params.set("participantId", options.participantId);
  if (options.participantName) params.set("participantName", options.participantName);

  const query = params.toString();
  const url = `/admin/support-center${query ? `?${query}` : ""}`;
  const openedWindow = window.open(url, "_blank", "noopener,noreferrer");
  openedWindow?.focus();
}
