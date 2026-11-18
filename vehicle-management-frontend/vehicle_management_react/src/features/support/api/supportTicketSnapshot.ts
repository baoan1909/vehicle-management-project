import type { ChatMessageResponse } from "@/features/support/api/chatApi";
import type { SupportTicketPriority, SupportTicketResponse, SupportTicketStatus } from "@/features/support/api/supportApi";

export type SupportTicketSnapshot = {
  actorAccountId: string | null;
  assignedTo: string | null;
  categoryCode: string | null;
  categoryName: string | null;
  content: string;
  eventId: string;
  eventType: "SHARED" | "STARTED_PROCESSING" | "RESOLVED" | "REOPENED" | "CLOSED" | "LEGACY_IMPORTED";
  legacyInferred: boolean;
  occurredAt: string | null;
  priority: SupportTicketPriority;
  schemaVersion: number;
  snapshotStatus: SupportTicketStatus;
  ticketId: string;
  ticketCode: string;
  title: string;
};

const statuses = new Set<SupportTicketStatus>(["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"]);
const priorities = new Set<SupportTicketPriority>(["LOW", "NORMAL", "HIGH", "URGENT"]);

export function parseSupportTicketSnapshot(message: ChatMessageResponse): SupportTicketSnapshot | null {
  if (message.messageType !== "SUPPORT_REQUEST" || !message.metadata) return null;
  try {
    const value = JSON.parse(message.metadata) as Partial<SupportTicketSnapshot>;
    if (
      typeof value.eventId !== "string"
      || typeof value.ticketId !== "string"
      || typeof value.ticketCode !== "string"
      || typeof value.title !== "string"
      || typeof value.content !== "string"
      || !statuses.has(value.snapshotStatus as SupportTicketStatus)
      || !priorities.has(value.priority as SupportTicketPriority)
    ) return null;
    return value as SupportTicketSnapshot;
  } catch {
    return null;
  }
}

export function applySnapshotToTicket(
  ticket: SupportTicketResponse,
  snapshot: SupportTicketSnapshot | null,
): SupportTicketResponse {
  if (!snapshot) return ticket;
  return {
    ...ticket,
    assignedTo: snapshot.assignedTo,
    categoryCode: snapshot.categoryCode,
    categoryName: snapshot.categoryName,
    content: snapshot.content,
    createdAt: snapshot.occurredAt ?? ticket.createdAt,
    priority: snapshot.priority,
    status: snapshot.snapshotStatus,
    title: snapshot.title,
  };
}
