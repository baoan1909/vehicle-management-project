import { useEffect, useState } from "react";
import { Button, Modal } from "@/components/ui";
import { SupportTicketCard } from "@/features/support/components/SupportTicketCard";
import type { SupportTicketResponse, SupportTicketStatus } from "@/features/support/api/supportApi";

export type TicketHistoryLoader = (filter: { keyword?: string; status?: SupportTicketStatus }) => Promise<SupportTicketResponse[]>;

export function SupportTicketHistoryDialog({ loadTickets, onClose, onSelect, open, title = "Lịch sử yêu cầu" }: {
  loadTickets: TicketHistoryLoader;
  onClose: () => void;
  onSelect?: (ticket: SupportTicketResponse) => Promise<void> | void;
  open: boolean;
  title?: string;
}) {
  const [tickets, setTickets] = useState<SupportTicketResponse[]>([]);
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState<SupportTicketStatus | "">("");
  const [loading, setLoading] = useState(false);
  const [selectingId, setSelectingId] = useState("");
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
    if (!onSelect || selectingId) return;
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

  return (
    <Modal actions={<Button variant="secondary" onClick={onClose}>Đóng</Button>} description="Các phiếu được giới hạn theo tài khoản và quyền truy cập hiện tại." onClose={onClose} open={open} title={title} width="lg">
      <div className="tw-grid tw-gap-3">
        <div className="tw-grid tw-grid-cols-[1fr_180px] tw-gap-2 max-sm:tw-grid-cols-1">
          <input aria-label="Tìm phiếu" className="tw-h-10 tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-px-3" placeholder="Tìm theo mã hoặc tiêu đề..." value={keyword} onChange={(event) => setKeyword(event.target.value)} />
          <select aria-label="Lọc trạng thái" className="tw-h-10 tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-px-3" value={status} onChange={(event) => setStatus(event.target.value as SupportTicketStatus | "")}>
            <option value="">Tất cả trạng thái</option><option value="OPEN">Đang mở</option><option value="IN_PROGRESS">Đang xử lý</option><option value="RESOLVED">Đã giải quyết</option><option value="CLOSED">Đã đóng</option>
          </select>
        </div>
        {loading ? <p className="tw-py-8 tw-text-center tw-text-sm tw-text-slate-500"><i className="fas fa-spinner fa-spin tw-mr-2" />Đang tải phiếu hỗ trợ...</p> : null}
        {error ? <div role="alert" className="tw-rounded-lg tw-bg-red-50 tw-p-3 tw-text-sm tw-font-bold tw-text-red-700">{error}</div> : null}
        {!loading && !error && tickets.length === 0 ? <p className="tw-py-8 tw-text-center tw-text-sm tw-text-slate-500">Chưa có phiếu hỗ trợ phù hợp.</p> : null}
        {!loading && !error ? <div className="tw-grid tw-max-h-[55vh] tw-gap-3 tw-overflow-y-auto tw-pr-1">{tickets.map((ticket) => <div className={selectingId === ticket.supportTicketId ? "tw-pointer-events-none tw-opacity-60" : ""} key={ticket.supportTicketId}><SupportTicketCard ticket={ticket} onSelect={onSelect ? () => void select(ticket) : undefined} /></div>)}</div> : null}
      </div>
    </Modal>
  );
}
