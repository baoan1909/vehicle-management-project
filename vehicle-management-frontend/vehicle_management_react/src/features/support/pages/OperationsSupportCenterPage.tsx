import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { ChangeEvent, ReactNode } from "react";
import { useSearchParams } from "react-router-dom";

import { Badge, Button, EntityAvatar, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import {
  getChatAttachmentReadUrl,
  getChatConversation,
  getChatInbox,
  getChatMessages,
  markChatConversationRead,
  sendChatImageMessage,
  sendChatTextMessage,
  type ChatAttachmentResponse,
  type ChatConversationParticipantResponse,
  type ChatConversationResponse,
  type ChatConversationType,
  type ChatInboxItemResponse,
  type ChatMessageResponse,
  type ChatRealtimeEvent,
} from "@/features/support/api/chatApi";
import { subscribeChatRealtime } from "@/features/support/api/chatRealtime";
import { getSupportTicketById, type SupportTicketPriority, type SupportTicketResponse, type SupportTicketStatus } from "@/features/support/api/supportApi";
import { cn } from "@/lib/cn";
import { hasAnyPermission } from "@/shared/auth/permissions";

type ConversationStatus = "processing" | "waiting" | "closed";
type Priority = "high" | "medium" | "low";
type ParticipantType = "customer" | "employee";

interface Conversation {
  channel: string;
  avatarUrl?: string | null;
  conversation?: ChatConversationResponse;
  conversationType?: ChatConversationType;
  customerLevel: string;
  email: string;
  id: string;
  initials: string;
  lastMessage: string;
  lastMessageId?: string | null;
  participantId: string;
  participantType: ParticipantType;
  phone: string;
  priority: Priority;
  priorityLabel: string;
  sla: string;
  status: ConversationStatus;
  supportTicketId?: string | null;
  ticketCode: string;
  ticketTitle: string;
  time: string;
  tone: "blue" | "green" | "amber" | "red" | "violet";
  unread?: number;
  userName: string;
}

interface InfoLine {
  icon: string;
  label: string;
  value: string;
  tone?: "default" | "danger" | "success" | "primary";
}

type LoadInboxOptions = {
  showLoading?: boolean;
};

const conversations: Conversation[] = [
  {
    channel: "Ứng dụng di động",
    customerLevel: "Khách hàng thân thiết",
    email: "tung.nguyen@gmail.com",
    id: "chat-tung",
    initials: "NT",
    lastMessage: "Tôi bị mất thẻ xe ở bãi Times City...",
    participantId: "CUS-000245",
    participantType: "customer",
    phone: "0903 123 456",
    priority: "high",
    priorityLabel: "Cao",
    sla: "SLA còn 02:14",
    status: "processing",
    ticketCode: "#TK-20240520-1023",
    ticketTitle: "Mất thẻ xe",
    time: "09:45",
    tone: "blue",
    unread: 2,
    userName: "Nguyễn Thanh Tùng",
  },
  {
    channel: "Web portal",
    customerLevel: "Khách hàng VIP",
    email: "binh.tran@example.com",
    id: "chat-binh",
    initials: "BT",
    lastMessage: "Thanh toán không thành công...",
    participantId: "CUS-000456",
    participantType: "customer",
    phone: "0902 345 678",
    priority: "medium",
    priorityLabel: "Trung bình",
    sla: "SLA còn 05:20",
    status: "waiting",
    ticketCode: "#TK-20240520-1022",
    ticketTitle: "Lỗi thanh toán",
    time: "09:32",
    tone: "amber",
    userName: "Trần Thị Bình",
  },
  {
    channel: "Tổng đài",
    customerLevel: "Khách hàng mới",
    email: "bao.tran@example.com",
    id: "chat-bao",
    initials: "TQ",
    lastMessage: "Xe của tôi bị tính phí sai giờ ra...",
    participantId: "CUS-000312",
    participantType: "customer",
    phone: "0911 222 333",
    priority: "high",
    priorityLabel: "Cao",
    sla: "SLA còn 01:32",
    status: "processing",
    ticketCode: "#TK-20240520-1021",
    ticketTitle: "Sai phí lượt xe",
    time: "09:10",
    tone: "red",
    userName: "Trần Quốc Bảo",
  },
  {
    channel: "Nội bộ",
    customerLevel: "Nhân viên vận hành",
    email: "binh.tran@coparking.vn",
    id: "chat-employee-binh",
    initials: "BT",
    lastMessage: "Cần hỗ trợ đối soát ca sáng...",
    participantId: "EMP-240045",
    participantType: "employee",
    phone: "0902 345 678",
    priority: "medium",
    priorityLabel: "Trung bình",
    sla: "Nội bộ",
    status: "waiting",
    ticketCode: "#IN-20240520-0091",
    ticketTitle: "Đối soát ca trực",
    time: "08:58",
    tone: "green",
    userName: "Trần Thị Bình",
  },
  {
    channel: "Nội bộ",
    customerLevel: "Quản lý bãi",
    email: "an.nguyen@coparking.vn",
    id: "chat-employee-an",
    initials: "NA",
    lastMessage: "Barie cổng B cần kiểm tra...",
    participantId: "EMP-240012",
    participantType: "employee",
    phone: "0901 234 567",
    priority: "high",
    priorityLabel: "Cao",
    sla: "Nội bộ",
    status: "processing",
    ticketCode: "#IN-20240520-0090",
    ticketTitle: "Sự cố thiết bị",
    time: "08:42",
    tone: "violet",
    userName: "Nguyễn Văn An",
  },
  {
    channel: "Ứng dụng di động",
    customerLevel: "Khách hàng thân thiết",
    email: "nam.le@example.com",
    id: "chat-nam",
    initials: "LH",
    lastMessage: "Gia hạn đăng ký tháng bị lỗi",
    participantId: "CUS-000389",
    participantType: "customer",
    phone: "0901 234 567",
    priority: "low",
    priorityLabel: "Thấp",
    sla: "SLA còn 08:09",
    status: "waiting",
    ticketCode: "#TK-20240520-1020",
    ticketTitle: "Gia hạn vé tháng",
    time: "08:58",
    tone: "green",
    userName: "Lê Hoàng Nam",
  },
  {
    channel: "Email",
    customerLevel: "Khách hàng doanh nghiệp",
    email: "mai.vu@example.com",
    id: "chat-mai",
    initials: "VT",
    lastMessage: "Không nhận được hóa đơn VAT",
    participantId: "CUS-000277",
    participantType: "customer",
    phone: "0988 112 233",
    priority: "medium",
    priorityLabel: "Trung bình",
    sla: "SLA còn 06:44",
    status: "waiting",
    ticketCode: "#TK-20240520-1019",
    ticketTitle: "Hóa đơn VAT",
    time: "08:41",
    tone: "amber",
    userName: "Vũ Thị Mai",
  },
];

const ticketInfo: InfoLine[] = [
  { icon: "far fa-folder", label: "Danh mục hỗ trợ", value: "Mất thẻ xe", tone: "danger" },
  { icon: "far fa-bell", label: "Ưu tiên", value: "Cao", tone: "danger" },
  { icon: "far fa-dot-circle", label: "Trạng thái", value: "Đang xử lý", tone: "primary" },
  { icon: "far fa-user", label: "Nhân viên phụ trách", value: "Trần Minh Hiếu" },
  { icon: "far fa-clock", label: "SLA", value: "SLA còn 02:14", tone: "danger" },
  { icon: "far fa-paper-plane", label: "Kênh", value: "Ứng dụng di động" },
  { icon: "far fa-calendar", label: "Thời gian tạo", value: "20/05/2024 09:45" },
  { icon: "far fa-calendar-check", label: "Cập nhật cuối", value: "20/05/2024 09:50" },
];

const customerInfo: InfoLine[] = [
  { icon: "fas fa-star", label: "Hạng khách hàng", value: "Thân thiết", tone: "success" },
  { icon: "fas fa-car-side", label: "Tổng số lượt gửi xe", value: "128 lượt" },
  { icon: "fas fa-coins", label: "Tổng chi tiêu", value: "2.450.000 đ" },
  { icon: "fas fa-wallet", label: "Công nợ hiện tại", value: "0 đ" },
];

const relatedInfo: InfoLine[] = [
  { icon: "fas fa-car", label: "Phương tiện", value: "30F-123.45" },
  { icon: "far fa-calendar-check", label: "Đăng ký", value: "Đang hoạt động", tone: "success" },
  { icon: "far fa-id-card", label: "Thẻ giữ xe", value: "Đã mất", tone: "danger" },
  { icon: "far fa-clock", label: "Lượt gửi xe gần nhất", value: "19/05/2024 18:24" },
  { icon: "far fa-history", label: "Lịch sử ticket", value: "Xem 3 ticket trước đó", tone: "primary" },
];

const chatConversationTypeLabel: Record<ChatConversationType, string> = {
  BILLING: "Thanh toán",
  CUSTOMER_DIRECT: "Hỗ trợ khách hàng",
  INTERNAL_DIRECT: "Nội bộ trực tiếp",
  INTERNAL_GROUP: "Nhóm nội bộ",
  LOST_CARD: "Mất thẻ",
  PARKING_SESSION: "Phiên gửi xe",
  SUPPORT_TICKET: "Ticket hỗ trợ",
  SYSTEM_DIRECT: "Hệ thống",
};

const supportTicketStatusLabel: Record<SupportTicketStatus, string> = {
  CLOSED: "Đã đóng",
  IN_PROGRESS: "Đang xử lý",
  OPEN: "Đang mở",
  RESOLVED: "Đã giải quyết",
};

const supportPriorityLabel: Record<SupportTicketPriority, string> = {
  HIGH: "Cao",
  LOW: "Thấp",
  NORMAL: "Trung bình",
  URGENT: "Khẩn cấp",
};

const conversationTones: Conversation["tone"][] = ["blue", "green", "amber", "red", "violet"];

function isUuid(value: string | null | undefined) {
  return Boolean(value && /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value));
}

function formatClock(value: string | null | undefined) {
  if (!value) return "--";
  const parsedDate = new Date(value);
  if (!Number.isNaN(parsedDate.getTime())) {
    return new Intl.DateTimeFormat("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
    }).format(parsedDate);
  }

  return value;
}

function parseBackendDateTime(value: string | null | undefined) {
  if (!value) return 0;
  const nativeTime = new Date(value).getTime();
  if (!Number.isNaN(nativeTime)) return nativeTime;

  const match = value.match(/^(\d{2}):(\d{2})\s+(\d{2})-(\d{2})-(\d{4})$/);
  if (!match) return 0;

  const [, hour, minute, day, month, year] = match;
  return new Date(Number(year), Number(month) - 1, Number(day), Number(hour), Number(minute)).getTime();
}

function formatDateTime(value: string | null | undefined) {
  if (!value) return "--";
  const parsedDate = new Date(value);
  if (!Number.isNaN(parsedDate.getTime())) {
    return new Intl.DateTimeFormat("vi-VN", {
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      month: "2-digit",
      year: "numeric",
    }).format(parsedDate);
  }

  return value;
}

function shortCode(prefix: string, value: string | null | undefined) {
  return value ? `${prefix}-${value.slice(0, 8).toUpperCase()}` : "--";
}

function getInitials(value: string) {
  const words = value
    .trim()
    .split(/\s+/)
    .filter(Boolean);
  if (!words.length) return "CH";

  return words
    .slice(0, 2)
    .map((word) => word[0]?.toUpperCase() ?? "")
    .join("");
}

function getParticipantDisplayName(participant: ChatConversationParticipantResponse | null | undefined) {
  if (!participant) return "";
  return participant.fullName?.trim()
    || participant.username?.trim()
    || participant.email?.trim()
    || shortCode("ACC", participant.accountId);
}

function resolvePrimaryParticipant(conversation: ChatConversationResponse, currentAccountId?: string | null) {
  const participants = conversation.participants ?? [];
  if (conversation.conversationType === "INTERNAL_DIRECT") {
    return participants.find((participant) => participant.accountId !== currentAccountId) ?? participants[0] ?? null;
  }

  if (conversation.conversationType === "CUSTOMER_DIRECT") {
    return participants.find((participant) => participant.memberRole === "CUSTOMER") ?? participants[0] ?? null;
  }

  return null;
}

function resolveConversationTitle(conversation: ChatConversationResponse, currentAccountId?: string | null) {
  const primaryParticipant = resolvePrimaryParticipant(conversation, currentAccountId);
  const participantName = getParticipantDisplayName(primaryParticipant);
  if (participantName) return participantName;

  return conversation.title?.trim() || chatConversationTypeLabel[conversation.conversationType] || "Hội thoại";
}

function findMessageSender(conversation: Conversation, senderAccountId: string | null) {
  if (!senderAccountId) return null;
  return conversation.conversation?.participants?.find((participant) => participant.accountId === senderAccountId) ?? null;
}

function hasMessageId(message: ChatMessageResponse): message is ChatMessageResponse & { messageId: string } {
  return typeof message.messageId === "string" && message.messageId.trim().length > 0;
}

function normalizeRealtimeMessage(event: ChatRealtimeEvent) {
  const message = event.message;
  const messageId = message?.messageId || event.messageId;
  if (!message || !messageId) return null;

  return {
    ...message,
    attachments: message.attachments ?? [],
    conversationId: message.conversationId || event.conversationId,
    createdAt: message.createdAt || event.occurredAt || new Date().toISOString(),
    messageId,
  };
}

function resolveRealtimeConversationId(event: ChatRealtimeEvent) {
  return event.conversationId || event.message?.conversationId || "";
}

function resolveRealtimeMessageId(event: ChatRealtimeEvent) {
  return event.messageId || event.message?.messageId || null;
}

function compareChatMessageOrder(
  firstMessage: ChatMessageResponse,
  secondMessage: ChatMessageResponse,
  messageIndexById: Map<string, number>,
) {
  const firstTime = parseBackendDateTime(firstMessage.createdAt);
  const secondTime = parseBackendDateTime(secondMessage.createdAt);
  if (firstTime !== secondTime) return firstTime - secondTime;

  const firstIndex = messageIndexById.get(firstMessage.messageId) ?? Number.MAX_SAFE_INTEGER;
  const secondIndex = messageIndexById.get(secondMessage.messageId) ?? Number.MAX_SAFE_INTEGER;
  if (firstIndex !== secondIndex) return firstIndex - secondIndex;

  return firstMessage.messageId.localeCompare(secondMessage.messageId);
}

function upsertChatMessages(currentMessages: ChatMessageResponse[], nextMessages: ChatMessageResponse[]) {
  const messageIndexById = new Map<string, number>();
  const messagesById = new Map(currentMessages.filter(hasMessageId).map((message) => [message.messageId, message]));

  currentMessages.filter(hasMessageId).forEach((message, index) => {
    messageIndexById.set(message.messageId, index);
  });

  nextMessages.filter(hasMessageId).forEach((message) => {
    if (!messageIndexById.has(message.messageId)) {
      messageIndexById.set(message.messageId, messageIndexById.size);
    }
    messagesById.set(message.messageId, message);
  });

  return Array.from(messagesById.values()).sort((firstMessage, secondMessage) => (
    compareChatMessageOrder(firstMessage, secondMessage, messageIndexById)
  ));
}

function normalizeHistoryMessages(messages: ChatMessageResponse[]) {
  return [...messages].reverse();
}

function mapChatStatus(status: ChatConversationResponse["status"]): ConversationStatus {
  if (status === "CLOSED" || status === "ARCHIVED") return "closed";
  return "processing";
}

function mapApiConversation(inboxItem: ChatInboxItemResponse, index: number, currentAccountId?: string): Conversation {
  const { conversation, lastMessage, unreadCount } = inboxItem;
  const primaryParticipant = resolvePrimaryParticipant(conversation, currentAccountId);
  const title = conversation.title?.trim() || chatConversationTypeLabel[conversation.conversationType] || "Hội thoại";
  const fallback = conversations[index % conversations.length];
  const resolvedTitle = resolveConversationTitle(conversation, currentAccountId) || title;
  const isCustomerFlow = Boolean(conversation.customerId || conversation.supportTicketId || conversation.conversationType === "CUSTOMER_DIRECT");
  const priority = fallback?.priority ?? "medium";

  return {
    channel: chatConversationTypeLabel[conversation.conversationType] ?? "Chat",
    avatarUrl: primaryParticipant?.avatarUrl,
    conversation,
    conversationType: conversation.conversationType,
    customerLevel: isCustomerFlow ? "Khách hàng" : "Nội bộ",
    email: primaryParticipant?.email ?? fallback?.email ?? "--",
    id: conversation.conversationId,
    initials: getInitials(resolvedTitle),
    lastMessage: lastMessage?.deleted ? "Tin nhắn đã bị xóa" : lastMessage?.content?.trim() || "Chưa có tin nhắn",
    lastMessageId: lastMessage?.messageId ?? conversation.lastMessageId,
    participantId: primaryParticipant?.accountId ?? conversation.customerId ?? conversation.assignedTo ?? conversation.ownerAccountId ?? conversation.conversationId,
    participantType: isCustomerFlow ? "customer" : "employee",
    phone: fallback?.phone ?? "--",
    priority,
    priorityLabel: fallback?.priorityLabel ?? "Trung bình",
    sla: conversation.status === "CLOSED" ? "Đã đóng" : "Đang theo dõi",
    status: mapChatStatus(conversation.status),
    supportTicketId: conversation.supportTicketId,
    ticketCode: conversation.supportTicketId ? shortCode("TK", conversation.supportTicketId) : shortCode("CHAT", conversation.conversationId),
    ticketTitle: resolvedTitle,
    time: formatClock(conversation.lastMessageAt ?? lastMessage?.createdAt),
    tone: conversationTones[index % conversationTones.length],
    unread: unreadCount || undefined,
    userName: resolvedTitle,
  };
}

function mapTicketPriority(priority?: SupportTicketPriority | null): Priority {
  if (priority === "HIGH" || priority === "URGENT") return "high";
  if (priority === "LOW") return "low";
  return "medium";
}

function mergeTicketIntoConversation(conversation: Conversation, ticket: SupportTicketResponse | null): Conversation {
  if (!ticket) return conversation;
  const priority = mapTicketPriority(ticket.priority);

  return {
    ...conversation,
    customerLevel: ticket.categoryName ?? conversation.customerLevel,
    participantId: ticket.customerId,
    priority,
    priorityLabel: supportPriorityLabel[ticket.priority],
    sla: supportTicketStatusLabel[ticket.status],
    status: ticket.status === "CLOSED" || ticket.status === "RESOLVED" ? "closed" : ticket.status === "OPEN" ? "waiting" : "processing",
    ticketCode: shortCode("TK", ticket.supportTicketId),
    ticketTitle: ticket.title,
  };
}

function priorityTone(priority: Priority) {
  if (priority === "high") return "danger";
  if (priority === "medium") return "warning";
  return "success";
}

function SectionCard({ children, title }: { children: ReactNode; title: string }) {
  return (
    <section className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-[0_8px_20px_rgba(15,23,42,0.03)]">
      <div className="tw-flex tw-min-h-[48px] tw-items-center tw-justify-between tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-4">
        <h2 className="tw-m-0 tw-text-[0.92rem] tw-font-extrabold tw-text-vm-slate-900">{title}</h2>
        <button type="button" className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-slate-700 hover:tw-bg-vm-slate-25" aria-label={`Thu gọn ${title}`}>
          <i className="fas fa-chevron-up tw-text-[0.72rem]" />
        </button>
      </div>
      <div className="tw-grid tw-gap-3 tw-p-4">{children}</div>
    </section>
  );
}

function InfoLineView({ item }: { item: InfoLine }) {
  const valueClassName = {
    danger: "tw-bg-red-50 tw-text-vm-danger",
    default: "tw-text-vm-slate-900",
    primary: "tw-bg-brand-50 tw-text-vm-primary",
    success: "tw-bg-green-50 tw-text-green-700",
  }[item.tone ?? "default"];

  return (
    <div className="tw-grid tw-grid-cols-[18px_minmax(0,1fr)_auto] tw-items-center tw-gap-2.5 tw-text-[0.8rem]">
      <i className={cn(item.icon, "tw-text-center tw-text-vm-slate-500")} />
      <span className="tw-min-w-0 tw-font-semibold tw-text-vm-slate-500">{item.label}</span>
      <span className={cn("tw-max-w-[150px] tw-truncate tw-rounded-vm-sm tw-px-2 tw-py-1 tw-text-right tw-font-extrabold", valueClassName)}>{item.value}</span>
    </div>
  );
}

function TopBar() {
  return (
    <header className="tw-flex tw-h-[64px] tw-items-center tw-justify-between tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-6">
      <h1 className="tw-m-0 tw-text-[1.35rem] tw-font-extrabold tw-text-vm-slate-900">Trung tâm hỗ trợ vận hành</h1>
      <div className="tw-flex tw-items-center tw-gap-5">
        <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-700">
          <span className="tw-h-2.5 tw-w-2.5 tw-rounded-full tw-bg-green-500" />
          Hệ thống hoạt động bình thường
        </span>
        <button type="button" className="tw-flex tw-h-10 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700">
          <i className="far fa-clock" />
          Ca làm việc: 08:00 - 17:00
          <i className="fas fa-chevron-down tw-text-[0.65rem]" />
        </button>
        <button type="button" className="tw-relative tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-white tw-text-vm-slate-700 hover:tw-bg-vm-slate-25" aria-label="Thông báo">
          <i className="far fa-bell tw-text-[1.1rem]" />
          <span className="tw-absolute tw-right-1 tw-top-1 tw-rounded-full tw-bg-red-500 tw-px-1.5 tw-text-[0.58rem] tw-font-extrabold tw-text-white">12</span>
        </button>
        <button type="button" className="tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-white tw-text-vm-slate-700 hover:tw-bg-vm-slate-25" aria-label="Trợ giúp">
          <i className="far fa-question-circle tw-text-[1.1rem]" />
        </button>
        <button type="button" className="tw-flex tw-items-center tw-gap-3 tw-rounded-vm-md tw-border-0 tw-bg-white tw-px-1 tw-py-1 tw-text-left hover:tw-bg-vm-slate-25">
          <img src="/assets/admin/dist/img/user2-160x160.jpg" alt="Nguyễn Văn A" className="tw-h-10 tw-w-10 tw-rounded-full tw-object-cover" />
          <span className="tw-grid tw-leading-tight">
            <strong className="tw-text-[0.82rem] tw-text-vm-slate-900">Nguyễn Văn A</strong>
            <small className="tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">Quản trị viên</small>
          </span>
          <i className="fas fa-chevron-down tw-text-[0.65rem] tw-text-vm-slate-500" />
        </button>
      </div>
    </header>
  );
}

function ConversationItem({
  active,
  conversation,
  onSelect,
}: {
  active: boolean;
  conversation: Conversation;
  onSelect: () => void;
}) {
  return (
    <button
      type="button"
      className={cn(
        "tw-grid tw-w-full tw-grid-cols-[40px_minmax(0,1fr)_auto] tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-bg-white tw-p-3 tw-text-left tw-transition",
        active
          ? "tw-border-vm-primary tw-bg-brand-50 tw-shadow-[inset_3px_0_0_#2563EB,0_12px_22px_rgba(37,99,235,0.08)]"
          : "tw-border-vm-slate-100 hover:tw-border-brand-100 hover:tw-bg-vm-slate-25",
      )}
      onClick={onSelect}
    >
      <EntityAvatar initials={conversation.initials} src={conversation.avatarUrl} tone={conversation.tone} />
      <span className="tw-min-w-0">
        <span className="tw-flex tw-items-center tw-gap-2">
          <strong className="tw-truncate tw-text-[0.84rem] tw-font-extrabold tw-text-vm-slate-900">{conversation.userName}</strong>
          {conversation.unread ? <span className="tw-rounded-full tw-bg-vm-primary tw-px-1.5 tw-text-[0.58rem] tw-font-extrabold tw-text-white">{conversation.unread}</span> : null}
        </span>
        <span className="tw-mt-1 tw-block tw-truncate tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-700">{conversation.lastMessage}</span>
        <span className="tw-mt-2 tw-block tw-text-[0.74rem] tw-font-bold tw-text-vm-slate-500">{conversation.ticketCode}</span>
      </span>
      <span className="tw-grid tw-justify-items-end tw-gap-4">
        <small className="tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">{conversation.time}</small>
        <Badge tone={priorityTone(conversation.priority)} className="tw-rounded-vm-sm tw-px-2 tw-text-[0.66rem]">
          {conversation.priorityLabel}
        </Badge>
      </span>
    </button>
  );
}

function ConversationList({
  conversations,
  errorMessage,
  isFallback,
  isLoading,
  onRefresh,
  onSearchChange,
  selectedId,
  searchValue,
  onSelect,
}: {
  conversations: Conversation[];
  errorMessage?: string;
  isFallback: boolean;
  isLoading: boolean;
  onRefresh: () => void;
  onSearchChange: (value: string) => void;
  selectedId: string;
  searchValue: string;
  onSelect: (id: string) => void;
}) {
  const processingCount = conversations.filter((conversation) => conversation.status === "processing").length;
  const waitingCount = conversations.filter((conversation) => conversation.status === "waiting").length;

  return (
    <aside className="tw-flex tw-min-h-0 tw-flex-col tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 tw-bg-white">
      <div className="tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-p-5">
        <div className="tw-flex tw-items-center tw-justify-between">
          <h2 className="tw-m-0 tw-text-[1.05rem] tw-font-extrabold tw-text-vm-slate-900">Hộp thoại</h2>
          <button type="button" className="tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-bg-vm-slate-25" aria-label="Tải lại hội thoại" onClick={onRefresh}>
            <i className={isLoading ? "fas fa-spinner fa-spin" : "fas fa-sync-alt"} />
          </button>
        </div>
        <label className="tw-mt-4 tw-flex tw-h-10 tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3">
          <i className="fas fa-search tw-text-[0.82rem] tw-text-vm-slate-500" />
          <input
            className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.82rem] tw-font-semibold tw-outline-none placeholder:tw-text-vm-slate-500"
            placeholder="Tìm kiếm theo SĐT, biển số, mã ticket..."
            value={searchValue}
            onChange={(event) => onSearchChange(event.target.value)}
          />
        </label>
        {errorMessage ? (
          <div className="tw-mt-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-px-3 tw-py-2 tw-text-[0.76rem] tw-font-bold tw-text-amber-800">
            {errorMessage}
          </div>
        ) : null}
        {isFallback ? (
          <div className="tw-mt-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50 tw-px-3 tw-py-2 tw-text-[0.76rem] tw-font-bold tw-text-vm-primary">
            Đang dùng dữ liệu mẫu vì backend chưa có hội thoại trả về cho tài khoản hiện tại.
          </div>
        ) : null}
        <div className="tw-mt-3 tw-grid tw-grid-cols-3 tw-gap-2">
          {["Tất cả kênh", "Trạng thái", "Ưu tiên"].map((label) => (
            <button key={label} type="button" className="tw-flex tw-h-9 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-2 tw-text-[0.72rem] tw-font-bold tw-text-vm-slate-700" title="Bộ lọc nâng cao giữ lại cho phase sau">
              <span className="tw-truncate">{label}</span>
              <i className="fas fa-chevron-down tw-text-[0.58rem]" />
            </button>
          ))}
        </div>
        <div className="tw-mt-4 tw-flex tw-gap-5 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100">
          {[
            ["Tất cả", String(conversations.length)],
            ["Ticket đang xử lý", String(processingCount)],
            ["Chờ phản hồi", String(waitingCount)],
          ].map(([label, count], index) => (
            <button
              key={label}
              type="button"
              className={cn(
                "tw-relative tw-inline-flex tw-items-center tw-gap-1.5 tw-whitespace-nowrap tw-border-0 tw-bg-transparent tw-pb-3 tw-text-[0.76rem] tw-font-extrabold",
                index === 0 ? "tw-text-vm-primary after:tw-absolute after:tw-bottom-0 after:tw-left-0 after:tw-h-0.5 after:tw-w-full after:tw-bg-vm-primary" : "tw-text-vm-slate-700",
              )}
            >
              <span>{label}</span>
              <span className="tw-inline-flex tw-h-5 tw-min-w-5 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-bg-brand-100 tw-px-1.5 tw-text-[0.62rem] tw-text-vm-primary">{count}</span>
            </button>
          ))}
        </div>
      </div>
      <div className="tw-grid tw-min-h-0 tw-flex-1 tw-content-start tw-gap-2 tw-overflow-y-auto tw-p-3">
        {isLoading && conversations.length === 0 ? (
          <div className="tw-flex tw-min-h-[140px] tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-dashed tw-border-brand-100 tw-bg-brand-50 tw-text-[0.82rem] tw-font-extrabold tw-text-vm-primary">
            <i className="fas fa-spinner fa-spin tw-mr-2" />
            Đang tải hội thoại...
          </div>
        ) : null}
        {!isLoading && conversations.length === 0 ? (
          <div className="tw-flex tw-min-h-[140px] tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-dashed tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-4 tw-text-center tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-500">
            Không có hội thoại phù hợp với bộ lọc hiện tại.
          </div>
        ) : null}
        {conversations.map((conversation) => (
          <ConversationItem
            active={conversation.id === selectedId}
            conversation={conversation}
            key={conversation.id}
            onSelect={() => onSelect(conversation.id)}
          />
        ))}
      </div>
      <div className="tw-flex tw-h-[52px] tw-items-center tw-justify-between tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-700">
        <span>Hiển thị {conversations.length ? `1 - ${conversations.length}` : "0"} / {conversations.length} hội thoại</span>
        <span className="tw-flex tw-items-center tw-gap-3">
          <i className="fas fa-chevron-left tw-text-vm-slate-500" />
          <strong className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-brand-50 tw-text-vm-primary">1</strong>
          <span>2</span>
          <i className="fas fa-chevron-right tw-text-vm-slate-500" />
        </span>
      </div>
    </aside>
  );
}

function ChatHeader({ conversation }: { conversation: Conversation }) {
  return (
    <div className="tw-flex tw-h-[72px] tw-items-center tw-justify-between tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-6">
      <div className="tw-flex tw-min-w-0 tw-items-center tw-gap-3">
        <EntityAvatar initials={conversation.initials} src={conversation.avatarUrl} tone={conversation.tone} size="lg" />
        <div className="tw-min-w-0">
          <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2">
            <h2 className="tw-m-0 tw-text-[0.92rem] tw-font-extrabold tw-text-vm-slate-900">{conversation.userName}</h2>
            <Badge tone="success" className="tw-rounded-vm-sm tw-px-2">
              <i className="fas fa-star tw-mr-1 tw-text-[0.65rem]" />
              {conversation.customerLevel}
            </Badge>
          </div>
          <p className="tw-m-0 tw-mt-1 tw-flex tw-flex-wrap tw-gap-4 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-700">
            <span>{conversation.phone}</span>
            <span><i className="far fa-envelope tw-mr-1.5" />{conversation.email}</span>
          </p>
        </div>
      </div>
      <div className="tw-flex tw-items-center tw-gap-2">
        {["fas fa-phone", "fas fa-video", "fas fa-ellipsis-v"].map((icon) => (
          <button key={icon} type="button" className="tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-white tw-text-vm-slate-700 hover:tw-bg-vm-slate-25" aria-label="Tác vụ hội thoại">
            <i className={icon} />
          </button>
        ))}
      </div>
    </div>
  );
}

function TicketStrip({ conversation }: { conversation: Conversation }) {
  return (
    <div className="tw-flex tw-h-[52px] tw-items-center tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-6 tw-text-[0.82rem] tw-font-extrabold">
      <Badge tone="danger" className="tw-rounded-vm-sm tw-px-3">{conversation.ticketTitle}</Badge>
      <span className="tw-text-vm-slate-400">|</span>
      <span className="tw-text-vm-slate-500">{conversation.ticketCode}</span>
      <span className="tw-text-vm-slate-400">|</span>
      <Badge tone={priorityTone(conversation.priority)} className="tw-rounded-vm-sm tw-px-3">{conversation.priorityLabel}</Badge>
      <span className="tw-text-vm-slate-400">|</span>
      <span className="tw-text-vm-danger">{conversation.sla}</span>
    </div>
  );
}

function AttachmentPreview({ icon, label, tone }: { icon: string; label: string; tone: string }) {
  return (
    <div className={cn("tw-relative tw-h-[112px] tw-w-[112px] tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-gradient-to-br tw-p-3", tone)}>
      <button type="button" className="tw-absolute tw-right-2 tw-top-2 tw-inline-flex tw-h-7 tw-w-7 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-slate-900/82 tw-text-white" aria-label={`Tải ${label}`}>
        <i className="fas fa-download tw-text-[0.72rem]" />
      </button>
      <div className="tw-flex tw-h-full tw-flex-col tw-items-start tw-justify-end tw-gap-2 tw-text-white">
        <i className={cn(icon, "tw-text-[1.55rem] tw-drop-shadow")} />
        <span className="tw-text-[0.72rem] tw-font-extrabold tw-drop-shadow">{label}</span>
      </div>
    </div>
  );
}

function ApiAttachmentPreview({ attachment, onOpen }: { attachment: ChatAttachmentResponse; onOpen: (attachment: ChatAttachmentResponse) => void }) {
  const isImage = attachment.attachmentType === "IMAGE" || attachment.contentType?.startsWith("image/");
  const icon = isImage ? "far fa-image" : "far fa-file-alt";
  const label = attachment.originalFilename || (isImage ? "Ảnh đính kèm" : "Tệp đính kèm");
  const sizeLabel = attachment.sizeBytes ? `${Math.ceil(attachment.sizeBytes / 1024)} KB` : "Tệp riêng tư";

  return (
    <button
      type="button"
      className="tw-inline-flex tw-max-w-[220px] tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3 tw-text-left tw-shadow-[0_8px_20px_rgba(15,23,42,0.04)] hover:tw-border-brand-100 hover:tw-bg-brand-50"
      onClick={() => onOpen(attachment)}
    >
      <span className={cn("tw-inline-flex tw-h-10 tw-w-10 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-text-white", isImage ? "tw-bg-vm-primary" : "tw-bg-vm-slate-700")}>
        <i className={icon} />
      </span>
      <span className="tw-min-w-0">
        <strong className="tw-block tw-truncate tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-900">{label}</strong>
        <small className="tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">{sizeLabel}</small>
      </span>
    </button>
  );
}

function ChatMessageBubble({
  conversation,
  currentUserId,
  message,
  onOpenAttachment,
}: {
  conversation: Conversation;
  currentUserId?: string;
  message: ChatMessageResponse;
  onOpenAttachment: (attachment: ChatAttachmentResponse) => void;
}) {
  const isOwnMessage = Boolean(currentUserId && message.senderAccountId === currentUserId);
  const sender = findMessageSender(conversation, message.senderAccountId);
  const senderName = getParticipantDisplayName(sender) || (isOwnMessage ? "You" : conversation.userName);
  const senderInitials = getInitials(senderName);
  const senderAvatarUrl = sender?.avatarUrl ?? (isOwnMessage ? null : conversation.avatarUrl);
  const content = message.deleted ? "Tin nhắn đã bị xóa" : message.content?.trim();

  if (isOwnMessage) {
    return (
      <div className="tw-grid tw-grid-cols-[minmax(0,1fr)_40px] tw-gap-3 tw-self-end">
        <div className="tw-max-w-[560px] tw-rounded-vm-lg tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50 tw-p-4 tw-text-[0.86rem] tw-font-semibold tw-leading-relaxed tw-text-vm-slate-900">
          {content ? <p className="tw-m-0">{content}</p> : null}
          {message.attachments.length ? (
            <div className="tw-mt-3 tw-flex tw-flex-wrap tw-gap-2">
              {message.attachments.map((attachment) => (
                <ApiAttachmentPreview attachment={attachment} key={attachment.attachmentId} onOpen={onOpenAttachment} />
              ))}
            </div>
          ) : null}
          <div className="tw-mt-2 tw-flex tw-justify-end tw-gap-2 tw-text-[0.72rem] tw-text-vm-slate-500">
            {formatClock(message.createdAt)}
            <i className="fas fa-check-double tw-text-vm-primary" />
          </div>
        </div>
        <EntityAvatar initials={senderInitials} src={senderAvatarUrl} tone="blue" title={senderName} />
      </div>
    );
  }

  return (
    <div className="tw-grid tw-grid-cols-[40px_minmax(0,1fr)] tw-gap-3">
      <EntityAvatar initials={senderInitials} src={senderAvatarUrl} tone={conversation.tone} title={senderName} />
      <div className="tw-max-w-[560px] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4 tw-shadow-[0_8px_20px_rgba(15,23,42,0.04)]">
        {content ? <p className="tw-m-0 tw-text-[0.86rem] tw-font-semibold tw-leading-relaxed tw-text-vm-slate-900">{content}</p> : null}
        {message.attachments.length ? (
          <div className="tw-mt-3 tw-flex tw-flex-wrap tw-gap-2">
            {message.attachments.map((attachment) => (
              <ApiAttachmentPreview attachment={attachment} key={attachment.attachmentId} onOpen={onOpenAttachment} />
            ))}
          </div>
        ) : null}
        <div className="tw-mt-2 tw-text-right tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">{formatClock(message.createdAt)}</div>
      </div>
    </div>
  );
}

function ChatMessages({
  conversation,
  currentUserId,
  errorMessage,
  isLoading,
  messages,
  onOpenAttachment,
  usingMockData,
}: {
  conversation: Conversation;
  currentUserId?: string;
  errorMessage?: string;
  isLoading: boolean;
  messages: ChatMessageResponse[];
  onOpenAttachment: (attachment: ChatAttachmentResponse) => void;
  usingMockData: boolean;
}) {
  const messagesContainerRef = useRef<HTMLDivElement | null>(null);
  const messageIndexById = new Map(messages.map((message, index) => [message.messageId, index]));
  const orderedMessages = [...messages].sort((firstMessage, secondMessage) => {
    return compareChatMessageOrder(firstMessage, secondMessage, messageIndexById);
  });
  const latestMessageKey = orderedMessages.at(-1)?.messageId ?? "";

  useEffect(() => {
    if (usingMockData) return;

    const container = messagesContainerRef.current;
    if (!container) return;

    const animationFrameId = window.requestAnimationFrame(() => {
      container.scrollTop = container.scrollHeight;
    });

    return () => window.cancelAnimationFrame(animationFrameId);
  }, [latestMessageKey, usingMockData]);

  if (!usingMockData) {
    return (
      <div ref={messagesContainerRef} className="tw-flex tw-min-h-0 tw-flex-1 tw-flex-col tw-gap-5 tw-overflow-y-auto tw-bg-[#fbfdff] tw-px-6 tw-py-4">
        <div className="tw-text-center tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Tin nhắn gần đây</div>
        {isLoading ? (
          <div className="tw-flex tw-min-h-[180px] tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-dashed tw-border-brand-100 tw-bg-brand-50 tw-text-[0.86rem] tw-font-extrabold tw-text-vm-primary">
            <i className="fas fa-spinner fa-spin tw-mr-2" />
            Đang tải tin nhắn...
          </div>
        ) : null}
        {errorMessage ? (
          <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-4 tw-text-[0.84rem] tw-font-bold tw-text-red-700">
            {errorMessage}
          </div>
        ) : null}
        {!isLoading && !errorMessage && orderedMessages.length === 0 ? (
          <div className="tw-flex tw-min-h-[180px] tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-dashed tw-border-vm-slate-100 tw-bg-white tw-p-4 tw-text-center tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-500">
            Hội thoại này chưa có tin nhắn.
          </div>
        ) : null}
        {orderedMessages.map((message) => (
          <ChatMessageBubble
            conversation={conversation}
            currentUserId={currentUserId}
            key={message.messageId}
            message={message}
            onOpenAttachment={onOpenAttachment}
          />
        ))}
      </div>
    );
  }

  return (
    <div className="tw-flex tw-min-h-0 tw-flex-1 tw-flex-col tw-gap-5 tw-overflow-y-auto tw-bg-[#fbfdff] tw-px-6 tw-py-4">
      <div className="tw-text-center tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Hôm nay</div>

      <div className="tw-grid tw-grid-cols-[40px_minmax(0,1fr)] tw-gap-3">
        <EntityAvatar initials={conversation.initials} tone={conversation.tone} />
        <div className="tw-max-w-[560px] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4 tw-shadow-[0_8px_20px_rgba(15,23,42,0.04)]">
          <p className="tw-m-0 tw-text-[0.86rem] tw-font-semibold tw-leading-relaxed tw-text-vm-slate-900">
            Tôi bị mất thẻ xe ở bãi Times City Tòa T8 ngày 19/05 lúc 18:30. Biển số xe: 30F-123.45. Mong hỗ trợ cấp lại thẻ.
          </p>
          <div className="tw-mt-4 tw-flex tw-gap-2">
            <AttachmentPreview icon="fas fa-warehouse" label="Bãi B2" tone="tw-from-slate-700 tw-to-slate-950" />
            <AttachmentPreview icon="fas fa-car-side" label="Cổng vào" tone="tw-from-cyan-700 tw-to-slate-900" />
            <AttachmentPreview icon="far fa-id-card" label="Thẻ giữ xe" tone="tw-from-blue-500 tw-to-sky-900" />
          </div>
          <div className="tw-mt-2 tw-text-right tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">09:45</div>
        </div>
      </div>

      <div className="tw-grid tw-grid-cols-[minmax(0,1fr)_40px] tw-gap-3 tw-self-end">
        <div className="tw-max-w-[470px] tw-rounded-vm-lg tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50 tw-p-4 tw-text-[0.86rem] tw-font-semibold tw-leading-relaxed tw-text-vm-slate-900">
          Chào anh/chị {conversation.userName.split(" ").slice(-1).join(" ")},<br />
          Em đã kiểm tra thông tin. Anh/chị có thể cho em biết thêm màu xe để xác nhận chính xác hơn không ạ?
          <div className="tw-mt-2 tw-flex tw-justify-end tw-gap-2 tw-text-[0.72rem] tw-text-vm-slate-500">
            09:48 <i className="fas fa-check-double tw-text-vm-primary" />
          </div>
        </div>
        <img src="/assets/admin/dist/img/user2-160x160.jpg" alt="Admin" className="tw-h-10 tw-w-10 tw-rounded-full tw-object-cover" />
      </div>

      <div className="tw-grid tw-grid-cols-[40px_minmax(0,1fr)] tw-gap-3">
        <EntityAvatar initials={conversation.initials} tone={conversation.tone} />
        <div className="tw-max-w-[300px] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900">
          Xe màu trắng anh nhé.
          <div className="tw-mt-2 tw-text-right tw-text-[0.72rem] tw-text-vm-slate-500">09:49</div>
        </div>
      </div>

      <div className="tw-grid tw-grid-cols-[minmax(0,1fr)_40px] tw-gap-3 tw-self-end">
        <div className="tw-max-w-[520px] tw-rounded-vm-lg tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50 tw-p-4 tw-text-[0.86rem] tw-font-semibold tw-leading-relaxed tw-text-vm-slate-900">
          Dạ vâng, em đã xác nhận. Anh/chị vui lòng mang CCCD và giấy tờ xe đến văn phòng hỗ trợ tại sảnh B2 để làm thủ tục cấp lại thẻ ạ. Phí cấp lại thẻ là 50.000đ.
          <div className="tw-mt-2 tw-flex tw-justify-end tw-gap-2 tw-text-[0.72rem] tw-text-vm-slate-500">
            09:50 <i className="fas fa-check-double tw-text-vm-primary" />
          </div>
        </div>
        <img src="/assets/admin/dist/img/user2-160x160.jpg" alt="Admin" className="tw-h-10 tw-w-10 tw-rounded-full tw-object-cover" />
      </div>

      <div className="tw-self-end tw-rounded-vm-lg tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50 tw-p-3">
        <div className="tw-flex tw-items-center tw-gap-3">
          <span className="tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-red-600 tw-text-[0.72rem] tw-font-black tw-text-white">PDF</span>
          <span className="tw-grid">
            <strong className="tw-text-[0.82rem] tw-text-vm-slate-900">Huong_dan_cap_lai_the_xe.pdf</strong>
            <small className="tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500">512 KB</small>
          </span>
          <span className="tw-ml-6 tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">09:50 <i className="fas fa-check-double tw-ml-1 tw-text-vm-primary" /></span>
        </div>
      </div>
    </div>
  );
}

function Composer({
  canAttach,
  canSend,
  disabledReason,
  isSending,
  onSend,
}: {
  canAttach: boolean;
  canSend: boolean;
  disabledReason?: string;
  isSending: boolean;
  onSend: (content: string, files: File[]) => Promise<void>;
}) {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [content, setContent] = useState("");
  const [files, setFiles] = useState<File[]>([]);
  const trimmedContent = content.trim();
  const canSubmit = canSend && !isSending && Boolean(trimmedContent || files.length);

  async function handleSubmit() {
    if (!canSubmit) return;
    await onSend(trimmedContent, files);
    setContent("");
    setFiles([]);
  }

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const selectedFiles = Array.from(event.target.files ?? []).filter((file) => file.type.startsWith("image/"));
    setFiles(selectedFiles);
    event.target.value = "";
  }

  return (
    <div className="tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-pb-4 tw-pt-3">
      <input ref={fileInputRef} className="tw-hidden" type="file" accept="image/*" multiple onChange={handleFileChange} />
      <div className="tw-flex tw-gap-6 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100">
        <button type="button" className="tw-relative tw-border-0 tw-bg-transparent tw-pb-3 tw-text-[0.78rem] tw-font-extrabold tw-text-vm-primary after:tw-absolute after:tw-bottom-0 after:tw-left-0 after:tw-h-0.5 after:tw-w-full after:tw-bg-vm-primary">Trả lời</button>
        <button type="button" className="tw-border-0 tw-bg-transparent tw-pb-3 tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500" title="Backend chưa có API ghi chú nội bộ riêng cho chat, giữ lại cho phase sau">Ghi chú nội bộ</button>
      </div>
      <textarea
        className="tw-mt-4 tw-h-20 tw-w-full tw-resize-none tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-vm-focus disabled:tw-bg-vm-slate-25"
        disabled={!canSend || isSending}
        placeholder={disabledReason || "Nhập nội dung trả lời..."}
        value={content}
        onChange={(event) => setContent(event.target.value)}
      />
      {files.length ? (
        <div className="tw-mt-2 tw-flex tw-flex-wrap tw-gap-2">
          {files.map((file) => (
            <button
              key={`${file.name}-${file.size}`}
              type="button"
              className="tw-inline-flex tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50 tw-px-2.5 tw-py-1.5 tw-text-[0.76rem] tw-font-bold tw-text-vm-primary"
              onClick={() => setFiles((currentFiles) => currentFiles.filter((currentFile) => currentFile !== file))}
            >
              <i className="far fa-image" />
              <span className="tw-max-w-[180px] tw-truncate">{file.name}</span>
              <i className="fas fa-times" />
            </button>
          ))}
        </div>
      ) : null}
      <div className="tw-mt-2 tw-flex tw-items-center tw-justify-between">
        <div className="tw-flex tw-items-center tw-gap-3 tw-text-vm-slate-700">
          <button
            type="button"
            className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-white hover:tw-bg-vm-slate-25 disabled:tw-cursor-not-allowed disabled:tw-opacity-50"
            aria-label="Đính kèm ảnh"
            disabled={!canAttach || isSending}
            title={canAttach ? "Đính kèm ảnh" : "Bạn chưa có quyền gửi ảnh hoặc backend chỉ cho phép thành viên hội thoại gửi ảnh"}
            onClick={() => fileInputRef.current?.click()}
          >
            <i className="fas fa-paperclip" />
          </button>
          {[
            ["far fa-smile", "Emoji sẽ nối backend ở phase sau"],
            ["far fa-image", canAttach ? "Đính kèm ảnh" : "Bạn chưa có quyền gửi ảnh"],
            ["far fa-file-alt", "Backend hiện chỉ nhận ảnh, tài liệu để phase sau"],
            ["fas fa-ellipsis-h", "Tác vụ mở rộng giữ lại cho phase sau"],
          ].map(([icon, title]) => (
            <button
              key={icon}
              type="button"
              className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-white hover:tw-bg-vm-slate-25 disabled:tw-cursor-not-allowed disabled:tw-opacity-50"
              aria-label="Công cụ trả lời"
              disabled={icon !== "far fa-image" || !canAttach}
              title={title}
              onClick={icon === "far fa-image" && canAttach ? () => fileInputRef.current?.click() : undefined}
            >
              <i className={icon} />
            </button>
          ))}
        </div>
        <Button className="tw-w-[140px]" disabled={!canSubmit} loading={isSending} onClick={handleSubmit}>
          <i className="far fa-paper-plane" />
          Gửi
          <span className="tw-ml-2 tw-h-6 tw-border-0 tw-border-l tw-border-solid tw-border-white/25" />
          <i className="fas fa-chevron-down tw-text-[0.65rem]" />
        </Button>
      </div>
    </div>
  );
}

function ChatWorkspace({
  canAttach,
  canSend,
  conversation,
  currentUserId,
  disabledReason,
  isSending,
  messages,
  messageError,
  messagesLoading,
  onOpenAttachment,
  onSend,
  usingMockData,
}: {
  canAttach: boolean;
  canSend: boolean;
  conversation: Conversation;
  currentUserId?: string;
  disabledReason?: string;
  isSending: boolean;
  messages: ChatMessageResponse[];
  messageError?: string;
  messagesLoading: boolean;
  onOpenAttachment: (attachment: ChatAttachmentResponse) => void;
  onSend: (content: string, files: File[]) => Promise<void>;
  usingMockData: boolean;
}) {
  return (
    <main className="tw-flex tw-min-h-0 tw-flex-col tw-bg-white">
      <ChatHeader conversation={conversation} />
      <TicketStrip conversation={conversation} />
      <ChatMessages
        conversation={conversation}
        currentUserId={currentUserId}
        errorMessage={messageError}
        isLoading={messagesLoading}
        messages={messages}
        onOpenAttachment={onOpenAttachment}
        usingMockData={usingMockData}
      />
      <Composer
        canAttach={canAttach}
        canSend={canSend}
        disabledReason={disabledReason}
        isSending={isSending}
        onSend={onSend}
      />
    </main>
  );
}

function RightPanel({ className, conversation, ticket }: { className?: string; conversation: Conversation; ticket: SupportTicketResponse | null }) {
  const participantLabel = conversation.participantType === "employee" ? "Thông tin nhân viên" : "Thông tin khách hàng";
  const dynamicInfo =
    conversation.participantType === "employee"
      ? [
          { icon: "fas fa-id-badge", label: "Mã nhân viên", value: conversation.participantId },
          { icon: "fas fa-user-shield", label: "Vai trò", value: conversation.customerLevel },
          { icon: "fas fa-phone", label: "Số điện thoại", value: conversation.phone },
          { icon: "far fa-envelope", label: "Email", value: conversation.email },
        ]
      : customerInfo;
  const resolvedTicketInfo: InfoLine[] = ticket
    ? [
        { icon: "far fa-folder", label: "Danh mục hỗ trợ", value: ticket.categoryName ?? ticket.categoryCode ?? "--", tone: "danger" },
        { icon: "far fa-bell", label: "Ưu tiên", value: supportPriorityLabel[ticket.priority], tone: mapTicketPriority(ticket.priority) === "high" ? "danger" : "primary" },
        { icon: "far fa-dot-circle", label: "Trạng thái", value: supportTicketStatusLabel[ticket.status], tone: ticket.status === "RESOLVED" || ticket.status === "CLOSED" ? "success" : "primary" },
        { icon: "far fa-user", label: "Nhân viên phụ trách", value: ticket.assignedTo ?? "--" },
        { icon: "far fa-clock", label: "SLA", value: conversation.sla, tone: mapTicketPriority(ticket.priority) === "high" ? "danger" : "primary" },
        { icon: "far fa-paper-plane", label: "Kênh", value: conversation.channel },
        { icon: "far fa-calendar", label: "Thời gian tạo", value: formatDateTime(ticket.createdAt) },
        { icon: "far fa-calendar-check", label: "Cập nhật cuối", value: formatDateTime(ticket.updatedAt) },
      ]
    : ticketInfo;

  return (
    <aside className={cn("tw-flex tw-min-h-0 tw-flex-col tw-gap-2.5 tw-overflow-y-auto tw-border-0 tw-border-l tw-border-solid tw-border-vm-slate-100 tw-bg-[#fbfdff] tw-p-3", className)}>
      <SectionCard title="Thông tin ticket">
        {resolvedTicketInfo.map((item) => <InfoLineView item={item} key={item.label} />)}
      </SectionCard>
      <SectionCard title={participantLabel}>
        {dynamicInfo.map((item) => <InfoLineView item={item} key={item.label} />)}
      </SectionCard>
      <SectionCard title="Thông tin liên quan">
        {relatedInfo.map((item) => <InfoLineView item={item} key={item.label} />)}
      </SectionCard>
      <div className="tw-grid tw-gap-3 tw-p-2">
        <Button className="tw-h-11 tw-w-full" disabled={!ticket} title={ticket ? undefined : "Backend chat chưa trả ticket hoặc hội thoại này không gắn ticket"}>
          <i className="far fa-check-square" />
          Đóng ticket
        </Button>
        <Button className="tw-h-11 tw-w-full" variant="secondary" title="API tạo hội thoại nội bộ đã có, UI chọn thành viên giữ lại cho phase sau">
          <i className="fas fa-users" />
          Tạo hội thoại nội bộ
        </Button>
        <Button className="tw-h-11 tw-w-full" variant="secondary" title="Backend chat chưa có API chuyển ticket trong màn này, giữ lại cho phase sau">
          <i className="fas fa-share" />
          Chuyển ticket
        </Button>
      </div>
    </aside>
  );
}

export function OperationsSupportCenterPage() {
  const toast = useToast();
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  const requestedParticipantId = searchParams.get("participantId");
  const requestedParticipantName = searchParams.get("participantName");
  const [apiConversations, setApiConversations] = useState<Conversation[]>([]);
  const [selectedId, setSelectedId] = useState("");
  const [searchValue, setSearchValue] = useState("");
  const [inboxError, setInboxError] = useState("");
  const [isInboxLoading, setIsInboxLoading] = useState(false);
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [messageError, setMessageError] = useState("");
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [selectedTicket, setSelectedTicket] = useState<SupportTicketResponse | null>(null);
  const [isSending, setIsSending] = useState(false);
  const selectedConversationIdRef = useRef("");
  const inboxLoadedRef = useRef(false);
  const lastMarkedReadKeyRef = useRef<string | null>(null);
  const latestRealtimeMessageIdRef = useRef<string | null>(null);
  const realtimeSyncTimerIdRef = useRef<number | undefined>(undefined);
  const realtimeSyncVersionRef = useRef(0);
  const canReadChat = hasAnyPermission(user, ["CHAT_CONVERSATION_READ_OWN", "CHAT_CONVERSATION_READ_ALL"]);
  const canSendChat = hasAnyPermission(user, ["CHAT_MESSAGE_SEND_OWN"]);
  const canAttachChat = canSendChat && hasAnyPermission(user, ["CHAT_ATTACHMENT_CREATE_OWN"]);
  const canReadAttachment = hasAnyPermission(user, ["CHAT_ATTACHMENT_READ_OWN"]);

  const resolvePreferredConversationId = useCallback((sourceConversations: Conversation[]) => {
    if (requestedParticipantId) {
      const byParticipant = sourceConversations.find((conversation) =>
        conversation.participantId === requestedParticipantId ||
        conversation.id === requestedParticipantId ||
        conversation.conversation?.customerId === requestedParticipantId ||
        conversation.conversation?.assignedTo === requestedParticipantId ||
        conversation.conversation?.ownerAccountId === requestedParticipantId,
      );
      if (byParticipant) return byParticipant.id;
    }

    if (requestedParticipantName) {
      const normalizedName = requestedParticipantName.toLowerCase();
      const byName = sourceConversations.find((conversation) =>
        conversation.userName.toLowerCase() === normalizedName ||
        conversation.ticketTitle.toLowerCase() === normalizedName,
      );
      if (byName) return byName.id;
    }

    if (requestedParticipantId && isUuid(requestedParticipantId)) {
      const byUuid = sourceConversations.find((conversation) => conversation.supportTicketId === requestedParticipantId);
      if (byUuid) return byUuid.id;
    }

    return sourceConversations[0]?.id ?? "";
  }, [requestedParticipantId, requestedParticipantName]);

  const loadInbox = useCallback(async (options: LoadInboxOptions = {}) => {
    const shouldShowLoading = options.showLoading ?? !inboxLoadedRef.current;
    if (!canReadChat) {
      setApiConversations([]);
      setInboxError("Tài khoản hiện tại chưa có quyền CHAT_CONVERSATION_READ_OWN hoặc CHAT_CONVERSATION_READ_ALL.");
      inboxLoadedRef.current = true;
      if (shouldShowLoading) setIsInboxLoading(false);
      return;
    }

    if (shouldShowLoading) {
      setIsInboxLoading(true);
      setInboxError("");
    }
    try {
      const response = await getChatInbox();
      const inboxItems = response.data ?? [];
      const nextConversations = inboxItems.map((item, index) => mapApiConversation(item, index, user?.id));
      setInboxError("");
      setApiConversations(nextConversations);
      setSelectedId((currentSelectedId) => {
        if (currentSelectedId && nextConversations.some((conversation) => conversation.id === currentSelectedId)) {
          return currentSelectedId;
        }

        return resolvePreferredConversationId(nextConversations.length ? nextConversations : conversations);
      });
    } catch (error) {
      if (!shouldShowLoading && inboxLoadedRef.current) {
        return;
      }
      if (!inboxLoadedRef.current) {
        setApiConversations([]);
      }
      setInboxError(error instanceof Error ? error.message : "Không tải được hội thoại từ backend.");
      setSelectedId((currentSelectedId) => currentSelectedId || resolvePreferredConversationId(conversations));
    } finally {
      inboxLoadedRef.current = true;
      if (shouldShowLoading) setIsInboxLoading(false);
    }
  }, [canReadChat, resolvePreferredConversationId, user?.id]);

  const markReadAndRefreshInbox = useCallback((conversationId: string, messageId?: string | null) => {
    const normalizedMessageId = messageId?.trim();
    if (!normalizedMessageId) return;

    const readKey = `${conversationId}:${normalizedMessageId}`;
    if (lastMarkedReadKeyRef.current === readKey) return;

    lastMarkedReadKeyRef.current = readKey;
    void markChatConversationRead(conversationId, normalizedMessageId)
      .then(() => loadInbox({ showLoading: false }))
      .catch(() => {
        if (lastMarkedReadKeyRef.current === readKey) {
          lastMarkedReadKeyRef.current = null;
        }
      });
  }, [loadInbox]);

  useEffect(() => {
    void loadInbox({ showLoading: true });
  }, [loadInbox]);

  const sourceConversations = apiConversations.length ? apiConversations : conversations;
  const usingMockData = apiConversations.length === 0;
  const filteredConversations = useMemo(() => {
    const normalizedKeyword = searchValue.trim().toLowerCase();
    if (!normalizedKeyword) return sourceConversations;

    return sourceConversations.filter((conversation) =>
      [
        conversation.userName,
        conversation.ticketTitle,
        conversation.ticketCode,
        conversation.lastMessage,
        conversation.participantId,
        conversation.phone,
        conversation.email,
      ].some((value) => value.toLowerCase().includes(normalizedKeyword)),
    );
  }, [searchValue, sourceConversations]);

  useEffect(() => {
    if (!sourceConversations.length) return;
    setSelectedId((currentSelectedId) => {
      if (currentSelectedId && sourceConversations.some((conversation) => conversation.id === currentSelectedId)) {
        return currentSelectedId;
      }

      return resolvePreferredConversationId(sourceConversations);
    });
  }, [resolvePreferredConversationId, sourceConversations]);

  const selectedBaseConversation = filteredConversations.find((conversation) => conversation.id === selectedId)
    ?? filteredConversations[0]
    ?? sourceConversations[0]
    ?? conversations[0];
  const effectiveSelectedTicket = selectedTicket && selectedTicket.supportTicketId === selectedBaseConversation.supportTicketId ? selectedTicket : null;
  const selectedConversation = mergeTicketIntoConversation(selectedBaseConversation, effectiveSelectedTicket);
  selectedConversationIdRef.current = selectedBaseConversation.id;

  useEffect(() => {
    if (usingMockData || !selectedBaseConversation.conversation || !canReadChat) {
      setMessages([]);
      setMessageError("");
      setSelectedTicket(null);
      return;
    }

    let ignore = false;
    const conversationId = selectedBaseConversation.id;

    async function loadSelectedConversation() {
      setMessagesLoading(true);
      setMessageError("");
      setMessages([]);
      setSelectedTicket(null);

      try {
        const [conversationResponse, messageResponse] = await Promise.all([
          getChatConversation(conversationId),
          getChatMessages(conversationId, { limit: 100 }),
        ]);

        if (ignore) return;

        const refreshedConversation = conversationResponse.data;
        const refreshedPrimaryParticipant = resolvePrimaryParticipant(refreshedConversation, user?.id);
        const refreshedTitle = resolveConversationTitle(refreshedConversation, user?.id);
        setApiConversations((currentConversations) => currentConversations.map((conversation) => (
          conversation.id === conversationId
            ? {
                ...conversation,
                avatarUrl: refreshedPrimaryParticipant?.avatarUrl ?? conversation.avatarUrl,
                channel: chatConversationTypeLabel[refreshedConversation.conversationType] ?? conversation.channel,
                conversation: refreshedConversation,
                conversationType: refreshedConversation.conversationType,
                email: refreshedPrimaryParticipant?.email ?? conversation.email,
                initials: getInitials(refreshedTitle),
                participantId: refreshedPrimaryParticipant?.accountId ?? conversation.participantId,
                status: mapChatStatus(refreshedConversation.status),
                supportTicketId: refreshedConversation.supportTicketId,
                ticketTitle: refreshedTitle,
                userName: refreshedTitle,
              }
            : conversation
        )));
        setMessages((currentMessages) => upsertChatMessages(
          currentMessages,
          normalizeHistoryMessages(messageResponse.data ?? []),
        ));

        const lastMessageId = refreshedConversation.lastMessageId ?? messageResponse.data?.[0]?.messageId;
        if (lastMessageId) {
          markReadAndRefreshInbox(conversationId, lastMessageId);
        }

        if (refreshedConversation.supportTicketId) {
          try {
            const ticketResponse = await getSupportTicketById(refreshedConversation.supportTicketId);
            if (!ignore) setSelectedTicket(ticketResponse.data);
          } catch {
            if (!ignore) setSelectedTicket(null);
          }
        }
      } catch (error) {
        if (!ignore) {
          setMessages([]);
          setMessageError(error instanceof Error ? error.message : "Không tải được lịch sử tin nhắn.");
        }
      } finally {
        if (!ignore) setMessagesLoading(false);
      }
    }

    void loadSelectedConversation();

    return () => {
      ignore = true;
    };
  }, [canReadChat, loadInbox, markReadAndRefreshInbox, selectedBaseConversation.id, user?.id, usingMockData]);

  useEffect(() => {
    if (usingMockData || !canReadChat) return;
    const timerId = window.setInterval(() => {
      void loadInbox({ showLoading: false });
    }, 15000);

    return () => window.clearInterval(timerId);
  }, [canReadChat, loadInbox, usingMockData]);

  useEffect(() => {
    if (!canReadChat) return;

    const clearRealtimeSyncTimer = () => {
      if (realtimeSyncTimerIdRef.current !== undefined) {
        window.clearTimeout(realtimeSyncTimerIdRef.current);
        realtimeSyncTimerIdRef.current = undefined;
      }
    };

    const syncSelectedConversation = (conversationId: string) => {
      const readMessageId = latestRealtimeMessageIdRef.current;
      latestRealtimeMessageIdRef.current = null;
      const syncVersion = ++realtimeSyncVersionRef.current;

      void getChatMessages(conversationId, { limit: 100 })
        .then((messageResponse) => {
          if (syncVersion !== realtimeSyncVersionRef.current || conversationId !== selectedConversationIdRef.current) {
            return;
          }

          setMessages((currentMessages) => upsertChatMessages(
            currentMessages,
            normalizeHistoryMessages(messageResponse.data ?? []),
          ));
          if (readMessageId) {
            markReadAndRefreshInbox(conversationId, readMessageId);
          }
        })
        .catch(() => undefined);

      void getChatConversation(conversationId)
        .then((conversationResponse) => {
          if (syncVersion !== realtimeSyncVersionRef.current || conversationId !== selectedConversationIdRef.current) {
            return;
          }

          const refreshedConversation = conversationResponse.data;
          const refreshedPrimaryParticipant = resolvePrimaryParticipant(refreshedConversation, user?.id);
          const refreshedTitle = resolveConversationTitle(refreshedConversation, user?.id);

          setApiConversations((currentConversations) => currentConversations.map((conversation) => (
            conversation.id === conversationId
              ? {
                  ...conversation,
                  avatarUrl: refreshedPrimaryParticipant?.avatarUrl ?? conversation.avatarUrl,
                  channel: chatConversationTypeLabel[refreshedConversation.conversationType] ?? conversation.channel,
                  conversation: refreshedConversation,
                  conversationType: refreshedConversation.conversationType,
                  email: refreshedPrimaryParticipant?.email ?? conversation.email,
                  initials: getInitials(refreshedTitle),
                  participantId: refreshedPrimaryParticipant?.accountId ?? conversation.participantId,
                  status: mapChatStatus(refreshedConversation.status),
                  supportTicketId: refreshedConversation.supportTicketId,
                  ticketTitle: refreshedTitle,
                  userName: refreshedTitle,
                }
              : conversation
          )));

          if (refreshedConversation.supportTicketId) {
            void getSupportTicketById(refreshedConversation.supportTicketId)
              .then((ticketResponse) => {
                if (syncVersion === realtimeSyncVersionRef.current && conversationId === selectedConversationIdRef.current) {
                  setSelectedTicket(ticketResponse.data);
                }
              })
              .catch(() => undefined);
          }
        })
        .catch(() => undefined);

      void loadInbox({ showLoading: false });
    };

    const scheduleSelectedConversationSync = (conversationId: string, messageId: string | null | undefined) => {
      latestRealtimeMessageIdRef.current = messageId ?? latestRealtimeMessageIdRef.current;
      clearRealtimeSyncTimer();
      realtimeSyncTimerIdRef.current = window.setTimeout(() => {
        realtimeSyncTimerIdRef.current = undefined;
        if (conversationId !== selectedConversationIdRef.current) {
          return;
        }

        syncSelectedConversation(conversationId);
      }, 250);
    };

    const activeRealtimeConversationId = selectedBaseConversation.conversation?.conversationId ?? null;
    const unsubscribe = subscribeChatRealtime({
      conversationId: activeRealtimeConversationId,
      onError: (error) => {
        if (import.meta.env.DEV) {
          console.warn("[chat-realtime]", error.message);
        }
      },
      onEvent: (event) => {
        const realtimeConversationId = resolveRealtimeConversationId(event);
        if (realtimeConversationId !== selectedConversationIdRef.current) {
          void loadInbox({ showLoading: false });
          return;
        }

        const realtimeMessage = normalizeRealtimeMessage(event);
        if (realtimeMessage) {
          setMessages((currentMessages) => upsertChatMessages(currentMessages, [realtimeMessage]));
        }
        scheduleSelectedConversationSync(realtimeConversationId, resolveRealtimeMessageId(event));
      },
    });

    return () => {
      unsubscribe();
      clearRealtimeSyncTimer();
      realtimeSyncVersionRef.current += 1;
    };
  }, [canReadChat, loadInbox, markReadAndRefreshInbox, selectedBaseConversation.conversation?.conversationId, selectedBaseConversation.id, user?.id]);

  useEffect(() => {
    if (usingMockData || !selectedBaseConversation.conversation || !canReadChat) return;

    let cancelled = false;
    let syncing = false;
    const conversationId = selectedBaseConversation.id;

    async function reconcileSelectedHistory() {
      if (syncing || document.hidden) return;
      syncing = true;

      try {
        const messageResponse = await getChatMessages(conversationId, { limit: 100 });
        if (cancelled || conversationId !== selectedConversationIdRef.current) {
          return;
        }

        const historyMessages = normalizeHistoryMessages(messageResponse.data ?? []);
        setMessages((currentMessages) => upsertChatMessages(currentMessages, historyMessages));

        const latestMessageId = messageResponse.data?.[0]?.messageId;
        if (latestMessageId) {
          markReadAndRefreshInbox(conversationId, latestMessageId);
        }
      } catch {
        // The realtime socket remains the primary path; this reconciler quietly retries on the next tick.
      } finally {
        syncing = false;
      }
    }

    const intervalId = window.setInterval(() => {
      void reconcileSelectedHistory();
    }, 2500);

    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
    };
  }, [canReadChat, markReadAndRefreshInbox, selectedBaseConversation.conversation?.conversationId, selectedBaseConversation.id, usingMockData]);

  async function handleSendMessage(content: string, files: File[]) {
    if (usingMockData || !selectedConversation.conversation) {
      toast.warning("Hội thoại mẫu chưa thể gửi tin. Cần có conversation thật từ backend.", "Chưa có dữ liệu thật");
      return;
    }

    if (files.length && !canAttachChat) {
      toast.error("Bạn chưa có quyền CHAT_ATTACHMENT_CREATE_OWN.", "Không thể gửi ảnh");
      return;
    }

    if (!files.length && !canSendChat) {
      toast.error("Bạn chưa có quyền CHAT_MESSAGE_SEND_OWN.", "Không thể gửi tin");
      return;
    }

    setIsSending(true);
    try {
      const response = files.length
        ? await sendChatImageMessage(selectedConversation.id, content, files)
        : await sendChatTextMessage(selectedConversation.id, content);

      setMessages((currentMessages) => upsertChatMessages(currentMessages, [response.data]));
      toast.success("Đã gửi tin nhắn.", "Chat");
      await loadInbox({ showLoading: false });
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không gửi được tin nhắn.", "Gửi thất bại");
    } finally {
      setIsSending(false);
    }
  }

  async function handleOpenAttachment(attachment: ChatAttachmentResponse) {
    if (!canReadAttachment) {
      toast.error("Bạn chưa có quyền CHAT_ATTACHMENT_READ_OWN.", "Không thể mở tệp");
      return;
    }

    try {
      const response = await getChatAttachmentReadUrl(attachment.attachmentId);
      window.open(response.data.readUrl, "_blank", "noopener,noreferrer");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không tạo được đường dẫn đọc tệp.", "Mở tệp thất bại");
    }
  }

  const composerDisabledReason = selectedConversation.conversation?.status === "CLOSED"
    ? "Hội thoại đã đóng."
    : !canSendChat
      ? "Bạn chưa có quyền gửi tin nhắn."
      : usingMockData
        ? "Hội thoại mẫu chưa thể gửi tin."
        : undefined;
  const canUseComposer = Boolean(!usingMockData && selectedConversation.conversation && canSendChat && selectedConversation.conversation.status !== "CLOSED");

  return (
    <div className="tw-h-screen tw-overflow-hidden tw-bg-white tw-text-vm-slate-700">
      <div className="tw-grid tw-h-full tw-min-h-0 tw-min-w-0 tw-grid-rows-[64px_minmax(0,1fr)]">
        <TopBar />
        <div className="tw-grid tw-min-h-0 tw-grid-cols-[390px_minmax(520px,1fr)_330px] max-[1280px]:tw-grid-cols-[360px_minmax(460px,1fr)]">
          <ConversationList
            conversations={filteredConversations}
            errorMessage={inboxError}
            isFallback={usingMockData}
            isLoading={isInboxLoading}
            onRefresh={() => void loadInbox({ showLoading: true })}
            onSearchChange={setSearchValue}
            selectedId={selectedConversation.id}
            searchValue={searchValue}
            onSelect={setSelectedId}
          />
          <ChatWorkspace
            canAttach={canUseComposer && canAttachChat}
            canSend={canUseComposer}
            conversation={selectedConversation}
            currentUserId={user?.id}
            disabledReason={composerDisabledReason}
            isSending={isSending}
            messageError={messageError}
            messages={messages}
            messagesLoading={messagesLoading}
            onOpenAttachment={handleOpenAttachment}
            onSend={handleSendMessage}
            usingMockData={usingMockData}
          />
          <RightPanel className="max-[1280px]:tw-hidden" conversation={selectedConversation} ticket={effectiveSelectedTicket} />
        </div>
      </div>
    </div>
  );
}
