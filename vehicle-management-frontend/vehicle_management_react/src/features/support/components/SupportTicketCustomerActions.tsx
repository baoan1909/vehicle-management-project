import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { Button, Modal, SelectMenu, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import {
  createSupportTicketEscalation,
  getActiveSupportTicketCustomerConversation,
  getMyCurrentSupportTicketEscalation,
  type SupportTicketEscalationReason,
  type SupportTicketResponse,
} from "@/features/support/api/supportApi";
import { hasAnyPermission } from "@/shared/auth/permissions";

const reasonOptions = [
  { label: "Phản hồi quá chậm", value: "RESPONSE_DELAY" },
  { label: "Hướng dẫn chưa giải quyết vấn đề", value: "UNRESOLVED" },
  { label: "Giao tiếp không phù hợp", value: "INAPPROPRIATE_COMMUNICATION" },
  { label: "Muốn đổi người hỗ trợ", value: "REQUEST_DIFFERENT_ASSIGNEE" },
  { label: "Lý do khác", value: "OTHER" },
] satisfies Array<{ label: string; value: SupportTicketEscalationReason }>;

export function SupportTicketCustomerActions({ ticket }: { ticket: SupportTicketResponse }) {
  const navigate = useNavigate();
  const toast = useToast();
  const { user } = useAuth();
  const canCreateEscalation = hasAnyPermission(user, ["SUPPORT_TICKET_ESCALATION_CREATE_OWN"]);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [hasPendingEscalation, setHasPendingEscalation] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [reasonCode, setReasonCode] = useState<SupportTicketEscalationReason>("REQUEST_DIFFERENT_ASSIGNEE");
  const [description, setDescription] = useState("");
  const [error, setError] = useState("");

  const isActive = ticket.status === "OPEN" || ticket.status === "IN_PROGRESS";

  useEffect(() => {
    let active = true;
    setLoading(true);
    const conversationRequest = ticket.assignedTo
      ? getActiveSupportTicketCustomerConversation(ticket.supportTicketId).catch(() => null)
      : Promise.resolve(null);
    const escalationRequest = canCreateEscalation && isActive && ticket.assignedTo
      ? getMyCurrentSupportTicketEscalation(ticket.supportTicketId).catch(() => null)
      : Promise.resolve(null);

    void Promise.all([conversationRequest, escalationRequest]).then(([conversation, escalation]) => {
      if (!active) return;
      setConversationId(conversation?.data.conversationId ?? null);
      setHasPendingEscalation(escalation?.data?.status === "PENDING");
      setLoading(false);
    });
    return () => { active = false; };
  }, [canCreateEscalation, isActive, ticket.assignedTo, ticket.status, ticket.supportTicketId]);

  function openConversation() {
    if (!conversationId) return;
    navigate(`/customer/support/chat?conversationId=${encodeURIComponent(conversationId)}&ticketId=${encodeURIComponent(ticket.supportTicketId)}`);
  }

  async function submitEscalation() {
    const normalizedDescription = description.trim();
    if (normalizedDescription.length < 10) {
      setError("Vui lòng mô tả cụ thể ít nhất 10 ký tự.");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      await createSupportTicketEscalation(
        ticket.supportTicketId,
        { reasonCode, description: normalizedDescription },
        crypto.randomUUID(),
      );
      setHasPendingEscalation(true);
      setModalOpen(false);
      setDescription("");
      toast.success("Yêu cầu đã được gửi đến người có quyền quản lý phân công.");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Không thể gửi yêu cầu xem xét.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <div className="tw-mt-3 tw-flex tw-flex-wrap tw-items-center tw-gap-2 tw-border-0 tw-border-t tw-border-solid tw-border-slate-100 tw-pt-3">
        {conversationId ? (
          <button className="tw-rounded-lg tw-border tw-border-solid tw-border-sky-200 tw-bg-white tw-px-3 tw-py-1.5 tw-text-xs tw-font-extrabold tw-text-sky-700 hover:tw-bg-sky-50" type="button" onClick={openConversation}>
            <i className="far fa-comment-dots tw-mr-1.5" />
            {isActive ? "Tiếp tục trao đổi" : "Xem trao đổi"}
          </button>
        ) : (
          <span className="tw-text-xs tw-font-bold tw-text-slate-500">
            <i className="far fa-clock tw-mr-1.5" />
            {ticket.assignedTo ? "Chờ nhân viên phản hồi" : "Chờ phân công"}
          </span>
        )}
        {canCreateEscalation && isActive && ticket.assignedTo ? (
          hasPendingEscalation ? (
            <span className="tw-rounded-full tw-bg-amber-50 tw-px-2.5 tw-py-1.5 tw-text-[11px] tw-font-extrabold tw-text-amber-700">
              Đang chờ quản lý xem xét
            </span>
          ) : (
            <button className="tw-rounded-lg tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-px-3 tw-py-1.5 tw-text-xs tw-font-extrabold tw-text-amber-800 hover:tw-bg-amber-100 disabled:tw-opacity-60" disabled={loading} type="button" onClick={() => setModalOpen(true)}>
              <i className="fas fa-user-shield tw-mr-1.5" />Yêu cầu quản lý xem xét
            </button>
          )
        ) : null}
      </div>

      <Modal
        actions={<div className="tw-flex tw-justify-end tw-gap-2"><Button disabled={submitting} variant="secondary" onClick={() => setModalOpen(false)}>Hủy</Button><Button loading={submitting} onClick={() => void submitEscalation()}>Gửi yêu cầu</Button></div>}
        description="Người có quyền quản lý phân công sẽ xem xét và quyết định giữ nguyên, trực tiếp xử lý hoặc phân công nhân viên khác."
        onClose={() => { if (!submitting) setModalOpen(false); }}
        open={modalOpen}
        title="Yêu cầu quản lý xem xét"
        width="md"
      >
        <div className="tw-grid tw-gap-4">
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-sm tw-font-extrabold tw-text-slate-700">Lý do</span>
            <SelectMenu ariaLabel="Lý do yêu cầu xem xét" disabled={submitting} onChange={(value) => setReasonCode(value as SupportTicketEscalationReason)} options={reasonOptions} portal value={reasonCode} />
          </label>
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-sm tw-font-extrabold tw-text-slate-700">Nội dung chi tiết</span>
            <textarea className="tw-min-h-28 tw-w-full tw-resize-y tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-p-3 tw-text-sm tw-text-slate-800 focus:tw-border-sky-400 focus:tw-outline-none" disabled={submitting} maxLength={1000} placeholder="Mô tả vấn đề bạn gặp phải và mong muốn được hỗ trợ..." value={description} onChange={(event) => setDescription(event.target.value)} />
            <small className="tw-text-right tw-text-xs tw-text-slate-500">{description.length}/1000</small>
          </label>
          <p className="tw-m-0 tw-rounded-lg tw-bg-sky-50 tw-p-3 tw-text-xs tw-font-semibold tw-leading-5 tw-text-sky-800">Nhân viên hiện tại vẫn có thể tiếp tục hỗ trợ trong thời gian quản lý xem xét để không làm gián đoạn yêu cầu.</p>
          {error ? <p className="tw-m-0 tw-rounded-lg tw-bg-red-50 tw-p-3 tw-text-sm tw-font-bold tw-text-red-700">{error}</p> : null}
        </div>
      </Modal>
    </>
  );
}
