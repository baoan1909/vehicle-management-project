import { SelectMenu } from "@/shared/components/ui/SelectMenu";
import { useEffect, useState } from "react";
import { Button, Modal } from "@/components/ui";
import { SupportTicketCard } from "@/features/support/components/SupportTicketCard";
import type { SupportTicketResponse, SupportTicketStatus } from "@/features/support/api/supportApi";

export type TicketHistoryLoader = (filter: { keyword?: string; status?: SupportTicketStatus }) => Promise<SupportTicketResponse[]>;

export function SupportTicketHistoryDialog({ canResolve, loadTickets, onClose, onResolve, onSelect, open, title = "Lịch sử yêu cầu" }: {
  canResolve?: (ticket: SupportTicketResponse) => boolean;
  loadTickets: TicketHistoryLoader;
  onClose: () => void;
  onResolve?: (ticket: SupportTicketResponse) => Promise<SupportTicketResponse>;
  onSelect?: (ticket: SupportTicketResponse) => Promise<void> | void;
  open: boolean;
  title?: string;
}) {
  const [tickets, setTickets] = useState<SupportTicketResponse[]>([]);
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState<SupportTicketStatus | "">("");
  const [loading, setLoading] = useState(false);
  const [selectingId, setSelectingId] = useState("");
  const [resolvingId, setResolvingId] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    setLoading(true);
    setError("");
    const timer = window.setTimeout(() => {
      void loadTickets({ keyword: keyword.trim() || undefined, status: status || undefined })
        .then((items) => !cancelled && setTickets(items))
        .catch((caught) => !cancelled && setError(caught instanceof Error ? caught.message : "Không thể tải lịch sử yêu cầu."))
        .finally(() => !cancelled && setLoading(false));
    }, 200);
    return () => { cancelled = true; window.clearTimeout(timer); };
  }, [keyword, loadTickets, open, status]);

  async function select(ticket: SupportTicketResponse) {
    if (!onSelect || selectingId || resolvingId) return;
    setSelectingId(ticket.supportTicketId);
    setError("");
    try {
      await onSelect(ticket);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Không thể chọn phiếu hỗ trợ này.");
    } finally {
      setSelectingId("");
    }
  }

  async function resolve(ticket: SupportTicketResponse) {
    if (!onResolve || resolvingId || selectingId) return;
    setResolvingId(ticket.supportTicketId);
    setError("");
    try {
      const updatedTicket = await onResolve(ticket);
      setTickets((current) => current.map((item) => (
        item.supportTicketId === updatedTicket.supportTicketId ? updatedTicket : item
      )));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Không thể đánh dấu phiếu đã giải quyết.");
    } finally {
      setResolvingId("");
    }
  }

  return (
    <Modal actions={<Button variant="secondary" onClick={onClose}>Đóng</Button>} description="Các phiếu được giới hạn theo tài khoản và quyền truy cập hiện tại." onClose={onClose} open={open} title={title} width="lg">
      <div className="tw-grid tw-gap-3">
        <div className="tw-grid tw-grid-cols-[1fr_180px] tw-gap-2 max-sm:tw-grid-cols-1">
          <input aria-label="Tìm phiếu" className="tw-h-10 tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-px-3" placeholder="Tìm theo mã hoặc tiêu đề..." value={keyword} onChange={(event) => setKeyword(event.target.value)} />
          <SelectMenu ariaLabel={"Lọc trạng thái"}
                  triggerClassName={"tw-h-10 tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-px-3"}
                  value={status}
                  onChange={(selectedValue) => setStatus(selectedValue as SupportTicketStatus | "")}
                  options={[{ label: "Tất cả trạng thái", value: "" }, { label: "Đang mở", value: "OPEN" }, { label: "Đang xử lý", value: "IN_PROGRESS" }, { label: "Đã giải quyết", value: "RESOLVED" }, { label: "Đã đóng", value: "CLOSED" }]} portal clearValue="" />
        </div>
        {loading ? <p className="tw-py-8 tw-text-center tw-text-sm tw-text-slate-500"><i className="fas fa-spinner fa-spin tw-mr-2" />Đang tải phiếu hỗ trợ...</p> : null}
        {error ? <div role="alert" className="tw-rounded-lg tw-bg-red-50 tw-p-3 tw-text-sm tw-font-bold tw-text-red-700">{error}</div> : null}
        {!loading && !error && tickets.length === 0 ? <p className="tw-py-8 tw-text-center tw-text-sm tw-text-slate-500">Chưa có phiếu hỗ trợ phù hợp.</p> : null}
        {!loading && tickets.length > 0 ? <div className="tw-grid tw-max-h-[55vh] tw-gap-3 tw-overflow-y-auto tw-pr-1">{tickets.map((ticket) => {
          const busy = selectingId === ticket.supportTicketId || resolvingId === ticket.supportTicketId;
          const resolveAction = canResolve?.(ticket) && onResolve ? (
            <button
              className="tw-h-9 tw-rounded-vm-md tw-border-0 tw-bg-vm-primary tw-px-4 tw-text-xs tw-font-extrabold tw-text-white hover:tw-bg-vm-primary-hover disabled:tw-cursor-not-allowed disabled:tw-opacity-60"
              disabled={busy}
              type="button"
              onClick={(event) => { event.stopPropagation(); void resolve(ticket); }}
            >
              <i className={`fas ${resolvingId === ticket.supportTicketId ? "fa-spinner fa-spin" : "fa-check"} tw-mr-1.5`} />
              Đã giải quyết
            </button>
          ) : null;
          return <div className={busy ? "tw-opacity-60" : ""} key={ticket.supportTicketId}><SupportTicketCard actions={resolveAction} actionsPlacement="details" ticket={ticket} onSelect={onSelect ? () => void select(ticket) : undefined} /></div>;
        })}</div> : null}
      </div>
    </Modal>
  );
}
