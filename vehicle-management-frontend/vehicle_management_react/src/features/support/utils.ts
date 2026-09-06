import { createCustomerSupportConversation } from "./api/chatApi";

export type SupportOpenMode = "customer-direct" | "internal-direct";
export type SupportParticipantType = "customer" | "employee";
export type SupportCenterTarget = "admin" | "customer";

interface OpenSupportCenterOptions {
  conversationId?: string;
  mode?: SupportOpenMode;
  participantId?: string;
  participantName?: string;
  participantType?: SupportParticipantType;
  target?: SupportCenterTarget;
}

export function openSupportCenterConversation(options: OpenSupportCenterOptions = {}) {
  const url = buildSupportCenterUrl(options);
  const openedWindow = window.open(url, "_blank", "noopener,noreferrer");
  openedWindow?.focus();
}

export async function createAndOpenCustomerSupportConversation(options: {
  customerId: string;
  customerName?: string;
}) {
  // Open synchronously to keep the browser from treating it as a popup, then send the
  // completed conversation ID to that tab once the API has returned.
  const openedWindow = window.open("about:blank", "_blank");
  if (openedWindow) {
    openedWindow.opener = null;
  }

  try {
    const response = await createCustomerSupportConversation({
      customerId: options.customerId,
      title: options.customerName ? `Hỗ trợ khách hàng ${options.customerName}` : undefined,
    });
    const url = buildSupportCenterUrl({
      conversationId: response.data.conversationId,
      participantName: options.customerName,
      participantType: "customer",
    });

    if (openedWindow && !openedWindow.closed) {
      openedWindow.location.replace(url);
      openedWindow.focus();
    } else {
      window.location.assign(url);
    }
    return response.data;
  } catch (error) {
    openedWindow?.close();
    throw error;
  }
}

function buildSupportCenterUrl(options: OpenSupportCenterOptions = {}) {
  const params = new URLSearchParams();

  if (options.conversationId) params.set("conversationId", options.conversationId);
  if (options.mode) params.set("mode", options.mode);
  if (options.participantType) params.set("participantType", options.participantType);
  if (options.participantId) params.set("participantId", options.participantId);
  if (options.participantName) params.set("participantName", options.participantName);

  const query = params.toString();
  const targetPath = options.target === "customer" ? "/customer/support/chat" : "/admin/support-center";
  return `${targetPath}${query ? `?${query}` : ""}`;
}
