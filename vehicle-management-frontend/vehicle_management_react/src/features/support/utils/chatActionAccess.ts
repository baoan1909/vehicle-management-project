import type { CurrentUser } from "@/shared/types/common";
import { hasAnyPermission } from "@/shared/auth/permissions";
import type { ChatConversationResponse, ChatConversationType } from "@/features/support/api/chatApi";

export function isSupportTicketConversation(type?: ChatConversationType) {
  return type === "ASSISTANT_SUPPORT" || type === "SUPPORT_REQUESTER_DIRECT";
}

export function canCreateTicketInChat(user: CurrentUser | null, conversation?: ChatConversationResponse) {
  if (!user || user.accountStatus !== "ACTIVE" || conversation?.status !== "ACTIVE"
    || !isSupportTicketConversation(conversation.conversationType)
    || conversation.ownerAccountId !== user.id
    || !hasAnyPermission(user, ["SUPPORT_TICKET_CREATE_OWN"])) return false;
  return conversation.conversationType === "ASSISTANT_SUPPORT"
    || hasAnyPermission(user, ["SUPPORT_TICKET_CREATE_FROM_CHAT_OWN"]);
}

export function canReadTicketHistoryInChat(user: CurrentUser | null, conversation?: ChatConversationResponse) {
  if (!user || user.accountStatus !== "ACTIVE" || !conversation
    || !isSupportTicketConversation(conversation.conversationType)) return false;
  if (conversation.ownerAccountId === user.id) return hasAnyPermission(user, ["SUPPORT_TICKET_READ_OWN"]);
  return conversation.conversationType === "SUPPORT_REQUESTER_DIRECT"
    && conversation.participants.some((participant) => participant.accountId === user.id)
    && hasAnyPermission(user, ["SUPPORT_TICKET_READ_ALL"]);
}

export function canReadOrdersInChat(user: CurrentUser | null, conversation?: ChatConversationResponse) {
  if (!user || conversation?.status !== "ACTIVE" || conversation.conversationType !== "CUSTOMER_DIRECT"
    || !hasAnyPermission(user, ["CHAT_MESSAGE_SEND_OWN"])) return false;
  const membership = conversation.participants.find((participant) => participant.accountId === user.id);
  if (!membership) return false;
  return hasAnyPermission(user, [membership.memberRole === "CUSTOMER" ? "SUBSCRIPTION_READ_OWN" : "SUBSCRIPTION_READ_ALL"]);
}
