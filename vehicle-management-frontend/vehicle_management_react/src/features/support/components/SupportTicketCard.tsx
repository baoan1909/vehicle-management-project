import type { SupportTicketResponse } from "@/features/support/api/supportApi";
import type { ReactNode } from "react";

const statusLabels = {
  OPEN: "Đang mở",
  IN_PROGRESS: "Đang xử lý",
  RESOLVED: "Đã giải quyết",
  CLOSED: "Đã đóng",
} as const;

const priorityLabels = {
  LOW: "Thấp",
  NORMAL: "Bình thường",
  HIGH: "Cao",
  URGENT: "Khẩn cấp",
} as const;

export function supportTicketCode(ticketId: string) {
  return `TK-${ticketId.slice(0, 8).toUpperCase()}`;
}

function formatDate(value: string | null) {
  if (!value) return "--";
  const legacyMatch = value.match(/^(\d{2}):(\d{2})\s+(\d{2})-(\d{2})-(\d{4})$/);
  const date = legacyMatch
    ? new Date(
        Number(legacyMatch[5]),
        Number(legacyMatch[4]) - 1,
        Number(legacyMatch[3]),
        Number(legacyMatch[1]),
        Number(legacyMatch[2]),
      )
    : new Date(value);

  if (Number.isNaN(date.getTime())) return value;

  const pad = (part: number) => String(part).padStart(2, "0");
  return `${pad(date.getHours())}:${pad(date.getMinutes())} ${pad(date.getDate())}-${pad(date.getMonth() + 1)}-${date.getFullYear()}`;
}

export function SupportTicketCard({
  onSelect,
  actions,
  ticket,
}: {
  onSelect?: (ticket: SupportTicketResponse) => void;
  actions?: ReactNode;
  ticket: SupportTicketResponse;
}) {
  const content = (
    <>
      <div className="tw-flex tw-items-start tw-justify-between tw-gap-3">
        <div className="tw-min-w-0">
          <div className="tw-text-xs tw-font-black tw-uppercase tw-tracking-wide tw-text-sky-700">
            <i className="far fa-life-ring tw-mr-1.5" />{supportTicketCode(ticket.supportTicketId)}
          </div>
          <h3 className="tw-mb-0 tw-mt-1 tw-line-clamp-2 tw-text-sm tw-font-extrabold tw-text-slate-900">{ticket.title}</h3>
        </div>
        <span className="tw-shrink-0 tw-rounded-full tw-bg-sky-50 tw-px-2 tw-py-1 tw-text-[10px] tw-font-extrabold tw-text-sky-700">
          {statusLabels[ticket.status]}
        </span>
      </div>
      <dl className="tw-mb-0 tw-mt-3 tw-grid tw-grid-cols-[auto_1fr] tw-gap-x-3 tw-gap-y-1 tw-text-xs">
        <dt className="tw-text-slate-500">Danh mục</dt><dd className="tw-m-0 tw-font-bold tw-text-slate-700">{ticket.categoryName ?? ticket.categoryCode ?? "--"}</dd>
        <dt className="tw-text-slate-500">Ưu tiên</dt><dd className="tw-m-0 tw-font-bold tw-text-slate-700">{priorityLabels[ticket.priority]}</dd>
        <dt className="tw-text-slate-500">Phụ trách</dt><dd className="tw-m-0 tw-truncate tw-font-bold tw-text-slate-700">{ticket.assignedTo ?? "Chưa phân công"}</dd>
        <dt className="tw-text-slate-500">Ngày tạo</dt><dd className="tw-m-0 tw-font-bold tw-text-slate-700">{formatDate(ticket.createdAt)}</dd>
      </dl>
      {actions}
    </>
  );

  return onSelect ? (
    <button
      type="button"
      className="tw-w-full tw-rounded-xl tw-border tw-border-solid tw-border-sky-100 tw-bg-white tw-p-3 tw-text-left tw-shadow-sm hover:tw-border-sky-300 hover:tw-bg-sky-50/40 focus-visible:tw-outline-none focus-visible:tw-ring-2 focus-visible:tw-ring-sky-300"
      onClick={() => onSelect(ticket)}
    >
      {content}
    </button>
  ) : (
    <article className="tw-rounded-xl tw-border tw-border-solid tw-border-sky-100 tw-bg-white tw-p-3 tw-shadow-sm">{content}</article>
  );
}
