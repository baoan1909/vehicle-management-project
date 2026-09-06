import { useCallback, useEffect, useRef, useState, type FormEvent, type MouseEvent, type TouchEvent } from "react";
import { useAuth } from "@/core/auth/useAuth";
import { useToast } from "@/components/ui";
import {
  getChatMessages,
  markChatConversationRead,
  sendChatImageMessage,
  sendChatTextMessage,
  type ChatMessageResponse,
} from "@/features/support/api/chatApi";
import { subscribeChatRealtime } from "@/features/support/api/chatRealtime";
import {
  createSupportTicketChatIntake,
  getMySupportTickets,
  getSupportAssistantConversation,
  getSupportTicketById,
  shareSupportTicketWithAssistant,
  type SupportTicketResponse,
} from "@/features/support/api/supportApi";
import { CreateSupportTicketDialog } from "@/features/support/components/CreateSupportTicketDialog";
import { SupportComposerActions } from "@/features/support/components/SupportComposerActions";
import { SupportTicketCard } from "@/features/support/components/SupportTicketCard";
import { SupportTicketCustomerActions } from "@/features/support/components/SupportTicketCustomerActions";
import { SupportTicketHistoryDialog, type TicketHistoryLoader } from "@/features/support/components/SupportTicketHistoryDialog";
import { hasAnyPermission } from "@/shared/auth/permissions";

type Position = { x: number; y: number };
type DragOffset = { x: number; y: number };

const WIDGET_CONFIG = { marginX: 24, marginY: 40, size: 56 };

function getDefaultPosition(): Position {
  if (typeof window === "undefined") return { x: 0, y: 0 };
  return {
    x: window.innerWidth - WIDGET_CONFIG.size - WIDGET_CONFIG.marginX,
    y: window.innerHeight - WIDGET_CONFIG.size - WIDGET_CONFIG.marginY,
  };
}

function clampPosition(position: Position): Position {
  if (typeof window === "undefined") return position;
  return {
    x: Math.min(Math.max(position.x, 0), window.innerWidth - WIDGET_CONFIG.size),
    y: Math.min(Math.max(position.y, 0), window.innerHeight - WIDGET_CONFIG.size),
  };
}

function getClientPoint(event: MouseEvent<HTMLElement> | TouchEvent<HTMLElement> | globalThis.MouseEvent | globalThis.TouchEvent) {
  if ("touches" in event && event.touches.length > 0) return event.touches[0];
  if ("changedTouches" in event && event.changedTouches.length > 0) return event.changedTouches[0];
  return event as MouseEvent<HTMLElement> | globalThis.MouseEvent;
}

function SupportSparkleIcon() {
  return (
    <svg width="100%" height="100%" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <path fillRule="evenodd" clipRule="evenodd" d="M8.9.5c.554 0 .781.963 1.061 2.149.299 1.265.657 2.784 1.535 3.662.878.874 2.392 1.23 3.654 1.527 1.187.28 2.15.507 2.15 1.062 0 .555-.961.782-2.146 1.061-1.262.299-2.779.657-3.658 1.535-.878.879-1.236 2.396-1.535 3.658C9.682 16.339 9.455 17.3 8.9 17.3c-.554 0-.78-.96-1.057-2.144-.297-1.263-.653-2.781-1.532-3.66-.878-.878-2.397-1.236-3.662-1.535C1.463 9.681.5 9.454.5 8.9c0-.555.965-.782 2.154-1.061 1.263-.298 2.78-.654 3.657-1.528C7.185 5.434 7.541 3.917 7.839 2.654 8.118 1.465 8.345.5 8.9.5Zm8.4 12.6c.277 0 .394.464.54 1.043.156.619.345 1.367.796 1.821.454.451 1.203.64 1.821.796.579.146 1.043.263 1.043.54 0 .277-.464.394-1.043.54-.618.156-1.367.345-1.821.796-.451.454-.64 1.203-.796 1.821-.146.579-.263 1.043-.54 1.043-.277 0-.394-.464-.54-1.043-.156-.618-.345-1.367-.796-1.821-.454-.451-1.203-.64-1.821-.796-.579-.146-1.043-.263-1.043-.54 0-.277.464-.394 1.043-.54.618-.156 1.367-.345 1.821-.796.451-.454.64-1.202.796-1.821.146-.579.263-1.043.54-1.043Z" fill="url(#support-widget-gradient)" />
      <defs><linearGradient id="support-widget-gradient" x1=".5" y1="7" x2="22" y2="17.5" gradientUnits="userSpaceOnUse"><stop stopColor="#3EB3F4" /><stop offset=".82" stopColor="#2BE9D0" /></linearGradient></defs>
    </svg>
  );
}

function formatMessageTime(value: string | null) {
  if (!value) return "";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "" : new Intl.DateTimeFormat("vi-VN", { hour: "2-digit", minute: "2-digit" }).format(date);
}

function mergeMessage(current: ChatMessageResponse[], incoming: ChatMessageResponse) {
  if (current.some((message) => message.messageId === incoming.messageId)) return current;
  return [...current, incoming].sort((left, right) => {
    const leftTime = left.createdAt ? new Date(left.createdAt).getTime() : 0;
    const rightTime = right.createdAt ? new Date(right.createdAt).getTime() : 0;
    return leftTime - rightTime;
  });
}

function getSharedTicketNotice(ticket: SupportTicketResponse) {
  if (ticket.status === "CLOSED") {
    return "Phiếu đã đóng được đưa vào hội thoại để tham chiếu. Hãy tạo phiếu mới nếu bạn cần hỗ trợ tiếp.";
  }
  if (ticket.status === "RESOLVED") {
    return "Phiếu đã giải quyết được đưa vào hội thoại để tham chiếu. Cần mở lại phiếu trước khi tiếp tục xử lý.";
  }
  if (!ticket.assignedTo) {
    return "Đã đưa phiếu vào hội thoại. Phiếu vẫn nằm trong hàng đợi chờ phân công.";
  }
  return "Đã đưa phiếu vào hội thoại. Người phụ trách hiện tại của phiếu không thay đổi.";
}

export function SupportFloatingWidget() {
  const { user } = useAuth();
  const toast = useToast();
  const canOpenWidget = hasAnyPermission(user, ["SUPPORT_WIDGET_ACCESS_OWN"]);
  const canAttachImages = hasAnyPermission(user, ["CHAT_ATTACHMENT_CREATE_OWN"]);
  const [position, setPosition] = useState<Position>(() => getDefaultPosition());
  const [isDragging, setIsDragging] = useState(false);
  const [isReturningToDock, setIsReturningToDock] = useState(false);
  const [isOpeningAssistant, setIsOpeningAssistant] = useState(false);
  const [isPanelOpen, setIsPanelOpen] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [panelError, setPanelError] = useState("");
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [historyDialogOpen, setHistoryDialogOpen] = useState(false);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [ticketsById, setTicketsById] = useState<Record<string, SupportTicketResponse>>({});
  const [draft, setDraft] = useState("");
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const defaultPositionRef = useRef<Position>(getDefaultPosition());
  const dragOffsetRef = useRef<DragOffset>({ x: 0, y: 0 });
  const hasMovedRef = useRef(false);
  const openTimerRef = useRef<number | undefined>(undefined);
  const messageEndRef = useRef<HTMLDivElement | null>(null);

  const refreshMessages = useCallback(async (targetConversationId = conversationId) => {
    if (!targetConversationId) return;
    const response = await getChatMessages(targetConversationId, { limit: 100 });
    setMessages([...(response.data ?? [])].reverse());
  }, [conversationId]);

  const loadMyTickets = useCallback<TicketHistoryLoader>(async (filter) => {
    const response = await getMySupportTickets(filter);
    return response.data ?? [];
  }, []);

  useEffect(() => {
    const handleResize = () => {
      defaultPositionRef.current = getDefaultPosition();
      setPosition((current) => (isOpeningAssistant ? defaultPositionRef.current : clampPosition(current)));
    };
    defaultPositionRef.current = getDefaultPosition();
    setPosition(defaultPositionRef.current);
    window.addEventListener("resize", handleResize);
    return () => {
      window.removeEventListener("resize", handleResize);
      if (openTimerRef.current) window.clearTimeout(openTimerRef.current);
    };
  }, [isOpeningAssistant]);

  useEffect(() => {
    if (!isDragging) return undefined;
    const handleMove = (event: globalThis.MouseEvent | globalThis.TouchEvent) => {
      const point = getClientPoint(event);
      setPosition(clampPosition({ x: point.clientX - dragOffsetRef.current.x, y: point.clientY - dragOffsetRef.current.y }));
      hasMovedRef.current = true;
    };
    const stopDragging = () => setIsDragging(false);
    window.addEventListener("mousemove", handleMove);
    window.addEventListener("touchmove", handleMove, { passive: false });
    window.addEventListener("mouseup", stopDragging);
    window.addEventListener("touchend", stopDragging);
    return () => {
      window.removeEventListener("mousemove", handleMove);
      window.removeEventListener("touchmove", handleMove);
      window.removeEventListener("mouseup", stopDragging);
      window.removeEventListener("touchend", stopDragging);
    };
  }, [isDragging]);

  useEffect(() => {
    if (!isPanelOpen || !conversationId) return undefined;
    return subscribeChatRealtime({
      conversationId,
      onError: () => setPanelError("Kết nối realtime bị gián đoạn. Hệ thống sẽ tự kết nối lại."),
      onEvent: (event) => {
        if (event.conversationId !== conversationId || !event.message) return;
        setPanelError("");
        setMessages((current) => mergeMessage(current, event.message!));
        void markChatConversationRead(conversationId, event.message.messageId).catch(() => undefined);
      },
    });
  }, [conversationId, isPanelOpen]);

  useEffect(() => {
    if (isPanelOpen) messageEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [isPanelOpen, messages]);

  useEffect(() => {
    if (!isPanelOpen || createDialogOpen || historyDialogOpen) return undefined;
    const handleEscape = (event: KeyboardEvent) => { if (event.key === "Escape") setIsPanelOpen(false); };
    window.addEventListener("keydown", handleEscape);
    return () => window.removeEventListener("keydown", handleEscape);
  }, [createDialogOpen, historyDialogOpen, isPanelOpen]);

  useEffect(() => {
    const missingTicketIds = [...new Set(messages
      .filter((message) => message.messageType === "SUPPORT_REQUEST" && message.relatedId)
      .map((message) => message.relatedId!))]
      .filter((ticketId) => !ticketsById[ticketId]);
    missingTicketIds.forEach((ticketId) => {
      void getSupportTicketById(ticketId)
        .then((response) => setTicketsById((current) => ({ ...current, [ticketId]: response.data })))
        .catch(() => undefined);
    });
  }, [messages, ticketsById]);

  if (!canOpenWidget) return null;

  async function openAssistant() {
    if (isPanelOpen) { setIsPanelOpen(false); return; }
    setIsOpeningAssistant(true);
    setMessagesLoading(true);
    setPanelError("");
    setIsPanelOpen(true);
    try {
      const conversationResponse = await getSupportAssistantConversation();
      const nextConversationId = conversationResponse.data.conversationId;
      setConversationId(nextConversationId);
      await refreshMessages(nextConversationId);
      if (conversationResponse.data.lastMessageId) {
        void markChatConversationRead(nextConversationId, conversationResponse.data.lastMessageId).catch(() => undefined);
      }
    } catch (caught) {
      const message = caught instanceof Error ? caught.message : "Không thể mở Trợ lý hỗ trợ CoParking.";
      setPanelError(message);
      toast.error(message);
    } finally {
      setMessagesLoading(false);
      setIsOpeningAssistant(false);
    }
  }

  async function handleSendMessage(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const content = draft.trim();
    if (!conversationId || (!content && selectedFiles.length === 0) || isSending) return;
    setIsSending(true);
    try {
      const response = selectedFiles.length
        ? await sendChatImageMessage(conversationId, content || null, selectedFiles)
        : await sendChatTextMessage(conversationId, content);
      setMessages((current) => mergeMessage(current, response.data));
      setDraft("");
      setSelectedFiles([]);
    } catch (caught) {
      toast.error(caught instanceof Error ? caught.message : "Không thể gửi tin nhắn.");
    } finally {
      setIsSending(false);
    }
  }

  const startDrag = (event: MouseEvent<HTMLButtonElement> | TouchEvent<HTMLButtonElement>) => {
    if (isPanelOpen || isOpeningAssistant || isReturningToDock) return;
    const point = getClientPoint(event);
    hasMovedRef.current = false;
    dragOffsetRef.current = { x: point.clientX - position.x, y: point.clientY - position.y };
    setIsDragging(true);
  };

  const handleWidgetClick = () => {
    if (hasMovedRef.current) { hasMovedRef.current = false; return; }
    if (isReturningToDock || isOpeningAssistant) return;
    const dock = defaultPositionRef.current;
    if (Math.abs(position.x - dock.x) >= 2 || Math.abs(position.y - dock.y) >= 2) {
      setIsReturningToDock(true);
      setPosition(dock);
      openTimerRef.current = window.setTimeout(() => { setIsReturningToDock(false); void openAssistant(); }, 300);
      return;
    }
    void openAssistant();
  };

  return (
    <>
      {isPanelOpen ? (
        <section aria-label="Trợ lý hỗ trợ CoParking" className="tw-fixed tw-bottom-28 tw-right-6 tw-z-[1079] tw-flex tw-h-[min(620px,calc(100vh-9rem))] tw-w-[min(410px,calc(100vw-2rem))] tw-flex-col tw-overflow-hidden tw-rounded-2xl tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-shadow-[0_24px_70px_rgba(15,23,42,0.28)] max-[480px]:tw-bottom-24 max-[480px]:tw-left-2 max-[480px]:tw-right-2 max-[480px]:tw-w-auto" role="dialog">
          <header className="tw-flex tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-slate-100 tw-bg-white tw-px-4 tw-py-3">
            <span className="tw-flex tw-h-10 tw-w-10 tw-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-bg-cyan-50 tw-p-2"><SupportSparkleIcon /></span>
            <div className="tw-min-w-0 tw-flex-1"><h2 className="tw-m-0 tw-text-sm tw-font-extrabold tw-text-slate-900">Trợ lý hỗ trợ CoParking</h2><p className="tw-m-0 tw-text-xs tw-font-medium tw-text-slate-500">Luôn sẵn sàng tiếp nhận yêu cầu của bạn</p></div>
            <button aria-label="Đóng hộp thoại hỗ trợ" className="tw-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-slate-100 tw-text-slate-600 hover:tw-bg-slate-200" onClick={() => setIsPanelOpen(false)} type="button"><i aria-hidden="true" className="fas fa-times" /></button>
          </header>
          <div aria-live="polite" className="tw-flex-1 tw-space-y-3 tw-overflow-y-auto tw-bg-slate-50 tw-p-4">
            {messagesLoading ? <div className="tw-py-10 tw-text-center tw-text-sm tw-font-semibold tw-text-slate-500"><i className="fas fa-spinner fa-spin tw-mr-2" />Đang tải lịch sử trò chuyện...</div> : null}
            {panelError ? <div role="alert" className="tw-rounded-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-sm tw-font-bold tw-text-red-700">{panelError}</div> : null}
            {!messagesLoading && !panelError && messages.length === 0 ? <div className="tw-mx-auto tw-mt-8 tw-max-w-[260px] tw-text-center tw-text-sm tw-text-slate-500"><span className="tw-mx-auto tw-mb-3 tw-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-full tw-bg-cyan-50 tw-p-3"><SupportSparkleIcon /></span>Xin chào! Bạn cần CoParking hỗ trợ vấn đề gì?</div> : null}
            {messages.filter((message) => !message.deleted).map((message) => {
              const ticket = message.relatedId ? ticketsById[message.relatedId] : undefined;
              if (message.messageType === "SUPPORT_REQUEST" && ticket) return <SupportTicketCard actions={<SupportTicketCustomerActions ticket={ticket} />} key={message.messageId} ticket={ticket} />;
              if (["SYSTEM", "CONTEXT_CARD", "ACTION_CARD", "SUPPORT_REQUEST"].includes(message.messageType)) return <div className="tw-rounded-xl tw-border tw-border-solid tw-border-sky-100 tw-bg-white tw-p-3 tw-text-sm tw-text-slate-700 tw-shadow-sm" key={message.messageId}><div className="tw-mb-1 tw-flex tw-items-center tw-gap-2 tw-font-bold tw-text-sky-700"><i className="fas fa-info-circle" />Cập nhật hỗ trợ</div><p className="tw-m-0 tw-whitespace-pre-wrap tw-break-words">{message.content}</p><time className="tw-mt-1 tw-block tw-text-right tw-text-[10px] tw-text-slate-400">{formatMessageTime(message.createdAt)}</time></div>;
              const own = message.senderAccountId === user?.id;
              return <div className={`tw-flex ${own ? "tw-justify-end" : "tw-justify-start"}`} key={message.messageId}><div className={`tw-max-w-[82%] tw-rounded-2xl tw-px-3 tw-py-2 tw-text-sm tw-shadow-sm ${own ? "tw-rounded-br-md tw-bg-sky-700 tw-text-white" : "tw-rounded-bl-md tw-bg-white tw-text-slate-700"}`}><p className="tw-m-0 tw-whitespace-pre-wrap tw-break-words">{message.content}</p><time className={`tw-mt-1 tw-block tw-text-right tw-text-[10px] ${own ? "tw-text-sky-100" : "tw-text-slate-400"}`}>{formatMessageTime(message.createdAt)}</time></div></div>;
            })}
            <div ref={messageEndRef} />
          </div>
          <footer className="tw-border-0 tw-border-t tw-border-solid tw-border-slate-200 tw-bg-white">
            <SupportComposerActions
              canAttach={canAttachImages}
              disabled={isSending || !conversationId}
              onCreateTicket={() => setCreateDialogOpen(true)}
              onFilesSelected={(files) => setSelectedFiles((current) => [...current, ...files])}
              onOpenHistory={() => setHistoryDialogOpen(true)}
            />
            {selectedFiles.length ? (
              <div className="tw-flex tw-flex-wrap tw-gap-2 tw-px-3 tw-pt-2">
                {selectedFiles.map((file, index) => (
                  <button
                    key={`${file.name}-${file.size}-${index}`}
                    type="button"
                    className="tw-inline-flex tw-max-w-full tw-items-center tw-gap-2 tw-rounded-lg tw-border tw-border-solid tw-border-sky-100 tw-bg-sky-50 tw-px-2.5 tw-py-1.5 tw-text-xs tw-font-bold tw-text-sky-700"
                    title="Bỏ ảnh đã chọn"
                    onClick={() => setSelectedFiles((current) => current.filter((_, fileIndex) => fileIndex !== index))}
                  >
                    <i className="far fa-image" />
                    <span className="tw-max-w-52 tw-truncate">{file.name}</span>
                    <i className="fas fa-times" />
                  </button>
                ))}
              </div>
            ) : null}
            <form className="tw-flex tw-items-end tw-gap-2 tw-p-3 tw-pt-2" onSubmit={handleSendMessage}>
              <textarea aria-label="Nhập tin nhắn hỗ trợ" className="tw-max-h-24 tw-min-h-10 tw-flex-1 tw-resize-none tw-rounded-xl tw-border tw-border-solid tw-border-slate-200 tw-bg-slate-50 tw-px-3 tw-py-2 tw-text-sm tw-text-slate-800 focus:tw-border-sky-400 focus:tw-outline-none focus:tw-ring-2 focus:tw-ring-sky-100" disabled={isSending || !conversationId} onChange={(event) => setDraft(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); event.currentTarget.form?.requestSubmit(); } }} placeholder="Nhập nội dung cần hỗ trợ..." rows={1} value={draft} />
              <button aria-label="Gửi tin nhắn" className="tw-flex tw-h-10 tw-w-10 tw-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-sky-700 tw-text-white hover:tw-bg-sky-800 disabled:tw-cursor-not-allowed disabled:tw-bg-slate-300" disabled={isSending || !conversationId || (!draft.trim() && selectedFiles.length === 0)} type="submit"><i className={isSending ? "fas fa-spinner fa-spin" : "fas fa-paper-plane"} /></button>
            </form>
          </footer>
        </section>
      ) : null}

      <CreateSupportTicketDialog
        createTicket={async (payload, key) => (await createSupportTicketChatIntake(payload, key)).data.ticket}
        onClose={() => setCreateDialogOpen(false)}
        onCreated={(ticket) => { setTicketsById((current) => ({ ...current, [ticket.supportTicketId]: ticket })); toast.success("Đã tạo phiếu hỗ trợ."); void refreshMessages(); }}
        open={createDialogOpen}
      />
      <SupportTicketHistoryDialog
        loadTickets={loadMyTickets}
        onClose={() => setHistoryDialogOpen(false)}
        onSelect={async (ticket) => { await shareSupportTicketWithAssistant(ticket.supportTicketId); setTicketsById((current) => ({ ...current, [ticket.supportTicketId]: ticket })); setHistoryDialogOpen(false); await refreshMessages(); toast.success(getSharedTicketNotice(ticket)); }}
        open={historyDialogOpen}
        title="Phiếu yêu cầu của tôi"
      />

      <div className={`tw-group tw-fixed tw-z-[1080] ${isReturningToDock ? "tw-transition-all tw-duration-300 tw-ease-in-out" : ""}`} style={{ left: position.x, top: position.y, touchAction: "none" }}>
        <div className="tw-pointer-events-none tw-absolute tw-right-full tw-top-1/2 tw-mr-3 tw-w-max tw--translate-y-1/2 tw-rounded tw-bg-slate-800 tw-px-3 tw-py-1.5 tw-text-xs tw-font-semibold tw-text-white tw-opacity-0 tw-shadow-sm tw-transition-opacity tw-duration-300 group-hover:tw-opacity-100">Trợ lý hỗ trợ CoParking<span className="tw-absolute tw--right-1 tw-top-1/2 tw-h-2 tw-w-2 tw--translate-y-1/2 tw-rotate-45 tw-bg-slate-800" /></div>
        <button type="button" aria-expanded={isPanelOpen} aria-label={isPanelOpen ? "Đóng Trợ lý hỗ trợ CoParking" : "Mở Trợ lý hỗ trợ CoParking"} title="Trợ lý hỗ trợ CoParking" onClick={handleWidgetClick} onMouseDown={startDrag} onTouchStart={startDrag} className={`tw-relative tw-inline-flex tw-h-14 tw-w-14 tw-items-center tw-justify-center tw-rounded-full tw-border tw-border-solid tw-border-slate-100 tw-bg-white tw-text-slate-700 tw-shadow-[0_4px_12px_rgba(15,23,42,0.15)] tw-transition-transform tw-duration-300 active:tw-scale-95 focus:tw-outline-none focus:tw-ring-4 focus:tw-ring-cyan-100 ${isPanelOpen ? "tw-cursor-pointer" : isDragging ? "tw-cursor-grabbing tw-scale-105" : "tw-cursor-grab hover:tw-scale-110"}`}>{isOpeningAssistant ? <i className="fas fa-spinner fa-spin tw-text-lg tw-text-slate-500" /> : isPanelOpen ? <i className="fas fa-times tw-text-lg tw-text-slate-600" /> : <span className="tw-h-7 tw-w-7"><SupportSparkleIcon /></span>}</button>
      </div>
    </>
  );
}
