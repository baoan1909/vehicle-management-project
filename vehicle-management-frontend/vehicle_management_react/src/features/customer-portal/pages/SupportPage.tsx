import { Fragment, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import {
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  InfoBanner,
  Input,
  Modal,
  SearchInput,
  SelectMenu,
  useToast,
} from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { getApplicationTimeZone } from "@/shared/time/applicationTime";
import {
  getCustomerPortalProfile,
  type CustomerPortalProfile,
} from "@/features/customer-portal/api/customerPortalApi";
import {
  closeMySupportTicket,
  createSupportTicket,
  getActiveSupportTicketRequesterConversation,
  getMySupportTickets,
  reopenMySupportTicket,
  type SupportTicketPriority,
  type SupportTicketResponse,
  type SupportTicketStatus,
} from "@/features/support/api/supportApi";
import { SupportCategoryPicker } from "@/features/support/components/SupportCategoryPicker";
import { validateSupportTicketForm } from "@/features/support/utils/validateSupportTicketForm";
import { isSupportCategoryError, useAvailableSupportCategories } from "@/features/support/hooks/useAvailableSupportCategories";
import { openSupportWidget } from "@/features/support/supportWidgetEvents";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { Drawer } from "@/shared/components/ui/Drawer";

import { CustomerPageHeader, CustomerPortalLayout, Field, PortalPagination } from "./PortalShared";

type BadgeTone = "primary" | "success" | "warning" | "danger" | "neutral";

const statusLabels: Record<SupportTicketStatus, string> = {
  CLOSED: "Đã đóng",
  IN_PROGRESS: "Đang xử lý",
  OPEN: "Đang mở",
  RESOLVED: "Đã giải quyết",
};

const priorityLabels: Record<SupportTicketPriority, string> = {
  HIGH: "Cao",
  LOW: "Thấp",
  NORMAL: "Bình thường",
  URGENT: "Khẩn cấp",
};

const statusTone: Record<SupportTicketStatus, BadgeTone> = {
  CLOSED: "neutral",
  IN_PROGRESS: "warning",
  OPEN: "primary",
  RESOLVED: "success",
};

const priorityTone: Record<SupportTicketPriority, BadgeTone> = {
  HIGH: "danger",
  LOW: "success",
  NORMAL: "primary",
  URGENT: "danger",
};

const workflow: SupportTicketStatus[] = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"];

const emptyForm = {
  categoryId: "",
  content: "",
  title: "",
};

function formatDateTime(value: string | null) {
  if (!value) {
    return "--";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "--";
  }

  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    timeZone: getApplicationTimeZone(),
    month: "2-digit",
    year: "numeric",
  }).format(date);
}

function shortCode(id: string) {
  return `TK-${id.slice(0, 8).toUpperCase()}`;
}

function TicketBadge({ children, tone }: { children: string; tone: BadgeTone }) {
  return <Badge className="tw-whitespace-nowrap tw-text-[0.7rem]" tone={tone}>{children}</Badge>;
}

const metricTone = {
  primary: { icon: "tw-bg-brand-50 tw-text-vm-primary", value: "tw-text-vm-primary" },
  success: { icon: "tw-bg-green-50 tw-text-green-700", value: "tw-text-green-700" },
  warning: { icon: "tw-bg-amber-50 tw-text-amber-700", value: "tw-text-amber-700" },
} as const;

function SupportMetric({
  icon,
  label,
  tone,
  value,
}: {
  icon: string;
  label: string;
  tone: keyof typeof metricTone;
  value: number;
}) {
  return (
    <Card className="tw-flex tw-min-h-[104px] tw-items-center tw-gap-3 tw-p-4">
      <span className={`tw-grid tw-h-12 tw-w-12 tw-shrink-0 tw-place-items-center tw-rounded-full tw-text-xl ${metricTone[tone].icon}`}>
        <i className={icon} />
      </span>
      <div className="tw-min-w-0">
        <p className="tw-m-0 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">{label}</p>
        <strong className={`tw-mt-1 tw-block tw-text-[1.65rem] tw-font-bold tw-leading-none ${metricTone[tone].value}`}>{value}</strong>
      </div>
    </Card>
  );
}

export function SupportPage({ mode = "customer" }: { mode?: "customer" | "internal" }) {
  const { user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const toast = useToast();
  const canCloseOwnTicket = hasAnyPermission(user, ["SUPPORT_TICKET_CLOSE_OWN", "SUPPORT_TICKET_CLOSE_ALL"]);
  const canReopenOwnTicket = hasAnyPermission(user, ["SUPPORT_TICKET_REOPEN_OWN", "SUPPORT_TICKET_REOPEN_ALL"]);
  const canCreateTicket = hasAnyPermission(user, ["SUPPORT_TICKET_CREATE_OWN"]);
  const canOpenTicketConversation = hasAnyPermission(user, ["SUPPORT_TICKET_READ_OWN"]);
  const [profile, setProfile] = useState<CustomerPortalProfile | null>(null);
  const [tickets, setTickets] = useState<SupportTicketResponse[]>([]);
  const [selectedTicketId, setSelectedTicketId] = useState<string>("");
  const [keyword, setKeyword] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [statusFilter, setStatusFilter] = useState<SupportTicketStatus | "ALL">("ALL");
  const [priorityFilter, setPriorityFilter] = useState<SupportTicketPriority | "ALL">("ALL");
  const [form, setForm] = useState(emptyForm);
  const [formError, setFormError] = useState("");
  const [createFormOpen, setCreateFormOpen] = useState(false);
  const categoryState = useAvailableSupportCategories(createFormOpen);
  const { categories } = categoryState;
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [openingConversationTicketId, setOpeningConversationTicketId] = useState<string | null>(null);
  const supportWidgetOpenedRef = useRef(false);

  const loadData = useCallback(async (preferredTicketId?: string) => {
    setLoading(true);
    try {
      const [nextProfile, ticketResponse] = await Promise.all([
        mode === "customer" ? getCustomerPortalProfile() : Promise.resolve(null),
        getMySupportTickets(),
      ]);
      const nextTickets = ticketResponse.data ?? [];

      setProfile(nextProfile);
      setTickets(nextTickets);
      setCurrentPage(1);
      setSelectedTicketId((current) => {
        if (preferredTicketId && nextTickets.some((ticket) => ticket.supportTicketId === preferredTicketId)) {
          return preferredTicketId;
        }

        if (current && nextTickets.some((ticket) => ticket.supportTicketId === current)) {
          return current;
        }

        return "";
      });
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể tải dữ liệu hỗ trợ.");
    } finally {
      setLoading(false);
    }
  }, [mode, toast]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  useEffect(() => {
    const shouldOpenSupportWidget = mode === "internal"
      && new URLSearchParams(location.search).get("chat") === "open";
    if (!shouldOpenSupportWidget || supportWidgetOpenedRef.current) return undefined;

    supportWidgetOpenedRef.current = true;
    const timer = window.setTimeout(openSupportWidget, 0);
    return () => window.clearTimeout(timer);
  }, [location.search, mode]);

  const filteredTickets = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();

    return tickets.filter((ticket) => {
      const matchesKeyword =
        !normalizedKeyword ||
        ticket.title.toLowerCase().includes(normalizedKeyword) ||
        ticket.content.toLowerCase().includes(normalizedKeyword) ||
        ticket.categoryName?.toLowerCase().includes(normalizedKeyword);
      const matchesStatus = statusFilter === "ALL" || ticket.status === statusFilter;
      const matchesPriority = priorityFilter === "ALL" || ticket.priority === priorityFilter;

      return matchesKeyword && matchesStatus && matchesPriority;
    });
  }, [keyword, priorityFilter, statusFilter, tickets]);
  const totalPages = Math.max(1, Math.ceil(filteredTickets.length / pageSize));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const pagedTickets = filteredTickets.slice((safeCurrentPage - 1) * pageSize, safeCurrentPage * pageSize);

  useEffect(() => {
    setCurrentPage(1);
  }, [keyword, pageSize, priorityFilter, statusFilter]);

  const selectedTicket = useMemo(
    () => tickets.find((ticket) => ticket.supportTicketId === selectedTicketId) ?? null,
    [selectedTicketId, tickets],
  );

  const stats = useMemo(() => ({
    inProgress: tickets.filter((ticket) => ticket.status === "IN_PROGRESS").length,
    open: tickets.filter((ticket) => ticket.status === "OPEN").length,
    resolved: tickets.filter((ticket) => ticket.status === "RESOLVED").length,
  }), [tickets]);

  const selectedCategory = categories.find((category) => category.categoryId === form.categoryId);

  const handleSubmit = async () => {
    if (saving) {
      return;
    }
    setFormError("");
    const title = form.title.trim();
    const content = form.content.trim();

    const validationError = validateSupportTicketForm({ categoryId: form.categoryId, title, content });
    if (validationError) {
      setFormError(validationError);
      return;
    }

    setSaving(true);
    try {
      const idempotencyKey = typeof crypto !== "undefined" && "randomUUID" in crypto
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
      await createSupportTicket({
        categoryId: form.categoryId,
        content,
        title,
      }, idempotencyKey);
      setForm(emptyForm);
      setCreateFormOpen(false);
      toast.success("Đã gửi yêu cầu hỗ trợ.");
      await loadData();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "Không thể gửi yêu cầu hỗ trợ.");
      if (isSupportCategoryError(error)) {
        setForm((current) => ({ ...current, categoryId: "" }));
        await categoryState.reload();
      }
    } finally {
      setSaving(false);
    }
  };

  const handleOpenCreateForm = () => {
    setForm(emptyForm);
    setFormError("");
    setCreateFormOpen(true);
  };

  const handleCloseCreateForm = () => {
    if (saving) {
      return;
    }

    setCreateFormOpen(false);
    setFormError("");
  };

  const handleCloseTicket = async () => {
    if (!selectedTicket || selectedTicket.status === "CLOSED") {
      return;
    }

    setSaving(true);
    try {
      const response = await closeMySupportTicket(selectedTicket.supportTicketId);
      toast.success("Đã đóng yêu cầu hỗ trợ.");
      await loadData(response.data.supportTicketId);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể đóng yêu cầu hỗ trợ.");
    } finally {
      setSaving(false);
    }
  };

  const handleReopenTicket = async () => {
    if (!selectedTicket || selectedTicket.status !== "RESOLVED") {
      return;
    }

    setSaving(true);
    try {
      const response = await reopenMySupportTicket(selectedTicket.supportTicketId);
      toast.success("Đã mở lại yêu cầu hỗ trợ.");
      await loadData(response.data.supportTicketId);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể mở lại yêu cầu hỗ trợ.");
    } finally {
      setSaving(false);
    }
  };

  const handleOpenTicketConversation = async (ticket: SupportTicketResponse) => {
    if (!ticket.assignedTo || openingConversationTicketId) {
      return;
    }

    setOpeningConversationTicketId(ticket.supportTicketId);
    try {
      const response = await getActiveSupportTicketRequesterConversation(ticket.supportTicketId);
      const conversationId = response.data?.conversationId;
      if (!conversationId) {
        throw new Error("Hội thoại hỗ trợ cho yêu cầu này chưa được bắt đầu.");
      }

      const target = mode === "customer" ? "/customer/support/chat" : "/admin/my-support/chat";
      navigate(`${target}?conversationId=${encodeURIComponent(conversationId)}&ticketId=${encodeURIComponent(ticket.supportTicketId)}`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể mở hội thoại hỗ trợ.");
    } finally {
      setOpeningConversationTicketId(null);
    }
  };

  const renderAssignee = (ticket: SupportTicketResponse, unassignedLabel = "--") => {
    const username = ticket.assignedToUsername?.trim()
      || (ticket.assignedTo ? "Chưa cập nhật" : unassignedLabel);
    const opening = openingConversationTicketId === ticket.supportTicketId;

    return (
      <div className="tw-flex tw-min-w-0 tw-items-center tw-gap-2">
        <span className="tw-min-w-0 tw-truncate" title={username}>{username}</span>
        {canOpenTicketConversation && ticket.assignedTo ? (
          <button
            aria-label={`Mở hội thoại hỗ trợ với ${username}`}
            className="tw-grid tw-h-8 tw-w-8 tw-flex-none tw-place-items-center tw-rounded-full tw-border tw-border-solid tw-border-brand-200 tw-bg-white tw-text-vm-primary tw-transition hover:tw-bg-brand-50 focus-visible:tw-outline-none focus-visible:tw-ring-2 focus-visible:tw-ring-brand-300 disabled:tw-cursor-wait disabled:tw-opacity-60"
            disabled={Boolean(openingConversationTicketId)}
            title="Mở hội thoại hỗ trợ"
            type="button"
            onClick={(event) => {
              event.stopPropagation();
              void handleOpenTicketConversation(ticket);
            }}
            onKeyDown={(event) => event.stopPropagation()}
          >
            <i aria-hidden="true" className={opening ? "fas fa-spinner fa-spin" : "far fa-comment-dots"} />
          </button>
        ) : null}
      </div>
    );
  };

  const PageLayout = mode === "customer" ? CustomerPortalLayout : Fragment;

  return (
    <PageLayout>
      <div className={mode === "internal" ? "tw-min-w-0 tw-p-5 lg:tw-p-6" : "tw-contents"}>
      <CustomerPageHeader
        title="Hỗ trợ"
        subtitle="Gửi yêu cầu và theo dõi tình trạng xử lý hỗ trợ."
        action={canCreateTicket ? <Button size="lg" onClick={handleOpenCreateForm}><i className="fas fa-plus" />Tạo yêu cầu mới</Button> : undefined}
      />

      <div className="tw-min-w-0">
        <div className="tw-min-w-0">
          <div className="tw-grid tw-grid-cols-3 tw-gap-3 max-[700px]:tw-grid-cols-1">
            <SupportMetric icon="far fa-question-circle" label="Yêu cầu đang mở" tone="primary" value={stats.open} />
            <SupportMetric icon="fas fa-headset" label="Đang xử lý" tone="warning" value={stats.inProgress} />
            <SupportMetric icon="far fa-check-circle" label="Đã giải quyết" tone="success" value={stats.resolved} />
          </div>

          <Card className="tw-mt-4 tw-min-w-0 tw-overflow-hidden">
            <CardHeader className="tw-flex tw-items-center tw-justify-between tw-gap-3">
              <div>
                <h2 className="tw-m-0 tw-text-[1rem] tw-font-bold tw-text-vm-slate-900">Danh sách yêu cầu hỗ trợ</h2>
                <p className="tw-m-0 tw-mt-1 tw-text-[0.76rem] tw-font-medium tw-text-vm-slate-500">Chọn một yêu cầu để xem tiến độ xử lý.</p>
              </div>
              <Badge tone="neutral">{filteredTickets.length} yêu cầu</Badge>
            </CardHeader>

            <CardContent className="!tw-p-0">
              <div className="tw-grid tw-grid-cols-[minmax(220px,1.2fr)_minmax(170px,.8fr)_minmax(170px,.8fr)] tw-gap-3 tw-p-4 max-[760px]:tw-grid-cols-1">
                <SearchInput value={keyword} onChange={setKeyword} placeholder="Tìm theo tiêu đề, nội dung..." />
                <SelectMenu ariaLabel="Lọc trạng thái yêu cầu" className="tw-min-w-0" options={[{ label: "Tất cả trạng thái", value: "ALL" }, ...workflow.map((status) => ({ label: statusLabels[status], value: status }))]} portal searchable={false} triggerClassName="!tw-h-10 !tw-min-h-10 !tw-text-[0.82rem] !tw-font-semibold" value={statusFilter} onChange={(value) => setStatusFilter(value as SupportTicketStatus | "ALL")} />
                <SelectMenu ariaLabel="Lọc mức độ yêu cầu" className="tw-min-w-0" options={[{ label: "Tất cả mức độ", value: "ALL" }, ...Object.entries(priorityLabels).map(([value, label]) => ({ label, value }))]} portal searchable={false} triggerClassName="!tw-h-10 !tw-min-h-10 !tw-text-[0.82rem] !tw-font-semibold" value={priorityFilter} onChange={(value) => setPriorityFilter(value as SupportTicketPriority | "ALL")} />
              </div>

              <div className="vm-support-desktop-table tw-overflow-x-auto tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100">
                <table className="tw-w-full tw-min-w-[920px] tw-border-collapse tw-text-left">
                  <thead>
                    <tr className="tw-bg-vm-slate-25 tw-text-[0.72rem] tw-font-bold tw-uppercase tw-tracking-wide tw-text-vm-slate-500">
                      {['Mã yêu cầu', 'Tiêu đề', 'Loại yêu cầu', 'Mức độ', 'Trạng thái', 'Người xử lý', 'Ngày tạo', 'Ngày xử lý'].map((heading) => (
                        <th className="tw-whitespace-nowrap tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-3 tw-py-3" key={heading}>{heading}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {pagedTickets.map((ticket) => {
                      const selected = selectedTicket?.supportTicketId === ticket.supportTicketId;
                      return (
                        <tr
                          key={ticket.supportTicketId}
                          className={`tw-cursor-pointer tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-text-[0.78rem] tw-text-vm-slate-700 tw-transition hover:tw-bg-brand-50/50 focus-visible:tw-bg-brand-50 focus-visible:tw-outline-none ${selected ? "tw-bg-brand-50 tw-shadow-[inset_3px_0_0_#2563eb]" : ""}`}
                          role="button"
                          tabIndex={0}
                          aria-label={`Xem chi tiết ${shortCode(ticket.supportTicketId)}`}
                          aria-pressed={selected}
                          onClick={() => setSelectedTicketId(ticket.supportTicketId)}
                          onKeyDown={(event) => {
                            if (event.key === "Enter" || event.key === " ") {
                              event.preventDefault();
                              setSelectedTicketId(ticket.supportTicketId);
                            }
                          }}
                        >
                          <td className="tw-whitespace-nowrap tw-px-3 tw-py-3 tw-font-bold tw-text-vm-primary">{shortCode(ticket.supportTicketId)}</td>
                          <td className="tw-min-w-[170px] tw-px-3 tw-py-3 tw-font-semibold tw-text-vm-slate-900">{ticket.title}</td>
                          <td className="tw-px-3 tw-py-3">{ticket.categoryName ?? ticket.categoryCode ?? "--"}</td>
                          <td className="tw-px-3 tw-py-3"><TicketBadge tone={priorityTone[ticket.priority]}>{priorityLabels[ticket.priority]}</TicketBadge></td>
                          <td className="tw-px-3 tw-py-3"><TicketBadge tone={statusTone[ticket.status]}>{statusLabels[ticket.status]}</TicketBadge></td>
                          <td className="tw-min-w-[160px] tw-px-3 tw-py-3">{renderAssignee(ticket)}</td>
                          <td className="tw-whitespace-nowrap tw-px-3 tw-py-3">{formatDateTime(ticket.createdAt)}</td>
                          <td className="tw-whitespace-nowrap tw-px-3 tw-py-3">{formatDateTime(ticket.resolvedAt)}</td>
                        </tr>
                      );
                    })}
                    {!loading && filteredTickets.length === 0 ? <tr><td className="tw-p-8 tw-text-center tw-text-sm tw-font-semibold tw-text-vm-slate-500" colSpan={8}>Chưa có yêu cầu hỗ trợ phù hợp với bộ lọc.</td></tr> : null}
                    {loading ? <tr><td className="tw-p-8 tw-text-center tw-text-sm tw-font-semibold tw-text-vm-slate-500" colSpan={8}><i className="fas fa-spinner fa-spin tw-mr-2" />Đang tải dữ liệu hỗ trợ...</td></tr> : null}
                  </tbody>
                </table>
              </div>

              <ul className="vm-support-mobile-cards" aria-label="Danh sách yêu cầu hỗ trợ">
                {pagedTickets.map((ticket) => {
                  const selected = selectedTicket?.supportTicketId === ticket.supportTicketId;
                  return (
                    <li key={ticket.supportTicketId} className="vm-support-mobile-card">
                      <div className="tw-flex tw-items-center tw-justify-between tw-gap-2">
                        <strong className="tw-text-sm tw-text-vm-primary">{shortCode(ticket.supportTicketId)}</strong>
                        <TicketBadge tone={statusTone[ticket.status]}>{statusLabels[ticket.status]}</TicketBadge>
                      </div>
                      <span className="tw-text-[0.9rem] tw-font-bold tw-text-vm-slate-900">{ticket.title}</span>
                      <span className="tw-text-[0.76rem] tw-font-medium tw-text-vm-slate-500">{ticket.categoryName ?? ticket.categoryCode ?? "--"} · {formatDateTime(ticket.createdAt)}</span>
                      <div className="tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-700">{renderAssignee(ticket, "Chưa phân công")}</div>
                      <span><TicketBadge tone={priorityTone[ticket.priority]}>{priorityLabels[ticket.priority]}</TicketBadge></span>
                      <Button size="sm" variant={selected ? "primary" : "secondary"} aria-pressed={selected} onClick={() => setSelectedTicketId(ticket.supportTicketId)}>{selected ? "Đang xem chi tiết" : "Xem chi tiết"}</Button>
                    </li>
                  );
                })}
                {!loading && filteredTickets.length === 0 ? <li className="vm-support-mobile-card tw-text-sm tw-text-vm-slate-500">Chưa có yêu cầu hỗ trợ phù hợp với bộ lọc.</li> : null}
                {loading ? <li className="vm-support-mobile-card tw-text-sm tw-text-vm-slate-500">Đang tải dữ liệu hỗ trợ...</li> : null}
              </ul>

              <div className="tw-px-4 tw-pb-4">
                <PortalPagination currentPage={safeCurrentPage} pageSize={pageSize} totalRecords={filteredTickets.length} onPageChange={setCurrentPage} onPageSizeChange={setPageSize} />
              </div>
            </CardContent>
          </Card>
        </div>

        <Drawer
          description="Theo dõi thông tin và tiến độ xử lý yêu cầu hỗ trợ."
          onClose={() => setSelectedTicketId("")}
          open={Boolean(selectedTicket)}
          title={selectedTicket ? `Chi tiết ${shortCode(selectedTicket.supportTicketId)}` : "Chi tiết yêu cầu"}
          width="md"
        >
              {selectedTicket ? (
                <>
                  <dl className="tw-grid tw-grid-cols-[minmax(110px,.8fr)_minmax(0,1fr)] tw-gap-x-3 tw-gap-y-3 tw-text-[0.78rem]">
                    <dt className="tw-font-semibold tw-text-vm-slate-500">Người yêu cầu</dt><dd className="tw-m-0 tw-text-right tw-font-semibold tw-text-vm-slate-900">{profile?.profile?.fullName ?? profile?.account?.username ?? user?.fullName ?? user?.username ?? "--"}</dd>
                    <dt className="tw-font-semibold tw-text-vm-slate-500">Trạng thái</dt><dd className="tw-m-0 tw-text-right"><TicketBadge tone={statusTone[selectedTicket.status]}>{statusLabels[selectedTicket.status]}</TicketBadge></dd>
                    <dt className="tw-font-semibold tw-text-vm-slate-500">Mức độ</dt><dd className="tw-m-0 tw-text-right"><TicketBadge tone={priorityTone[selectedTicket.priority]}>{priorityLabels[selectedTicket.priority]}</TicketBadge></dd>
                    <dt className="tw-font-semibold tw-text-vm-slate-500">Loại yêu cầu</dt><dd className="tw-m-0 tw-text-right tw-font-semibold tw-text-vm-slate-900">{selectedTicket.categoryName ?? selectedTicket.categoryCode ?? "--"}</dd>
                    <dt className="tw-font-semibold tw-text-vm-slate-500">Người xử lý</dt><dd className="tw-m-0 tw-flex tw-justify-end tw-text-right tw-font-semibold tw-text-vm-slate-900">{renderAssignee(selectedTicket, "Chưa phân công")}</dd>
                    <dt className="tw-font-semibold tw-text-vm-slate-500">Ngày tạo</dt><dd className="tw-m-0 tw-text-right tw-font-semibold tw-text-vm-slate-900">{formatDateTime(selectedTicket.createdAt)}</dd>
                    <dt className="tw-font-semibold tw-text-vm-slate-500">Ngày giải quyết</dt><dd className="tw-m-0 tw-text-right tw-font-semibold tw-text-vm-slate-900">{formatDateTime(selectedTicket.resolvedAt)}</dd>
                  </dl>

                  <div className="tw-mt-5 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-4">
                    <h3 className="tw-m-0 tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-900">Nội dung</h3>
                    <p className="tw-m-0 tw-mt-2 tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-3 tw-text-[0.78rem] tw-font-medium tw-leading-5 tw-text-vm-slate-700">{selectedTicket.content}</p>
                  </div>
                  {selectedTicket.resolutionNote ? (
                    <div className="tw-mt-4">
                      <h3 className="tw-m-0 tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-900">Kết quả xử lý</h3>
                      <p className="tw-m-0 tw-mt-2 tw-rounded-vm-md tw-bg-green-50 tw-p-3 tw-text-[0.78rem] tw-font-medium tw-leading-5 tw-text-green-800">{selectedTicket.resolutionNote}</p>
                    </div>
                  ) : null}

                  <div className="tw-mt-5 tw-grid tw-grid-cols-4 tw-gap-1 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-4">
                    {workflow.map((step, index) => {
                      const currentIndex = workflow.indexOf(selectedTicket.status);
                      const reached = index <= currentIndex || selectedTicket.status === "CLOSED";
                      const current = step === selectedTicket.status;
                      return (
                        <div className="tw-relative tw-min-w-0 tw-text-center" key={step}>
                          {index < workflow.length - 1 ? <span aria-hidden="true" className={`tw-absolute tw-left-1/2 tw-right-[-50%] tw-top-[5px] tw-h-0.5 ${index < currentIndex || selectedTicket.status === "CLOSED" ? "tw-bg-vm-primary" : "tw-bg-vm-slate-100"}`} /> : null}
                          <span aria-hidden="true" className={`tw-relative tw-z-10 tw-mx-auto tw-block tw-h-3 tw-w-3 tw-rounded-full tw-ring-4 tw-ring-white ${current ? "tw-bg-amber-500" : reached ? "tw-bg-vm-primary" : "tw-bg-vm-slate-200"}`} />
                          <strong className={`tw-mt-2 tw-block tw-text-[0.63rem] tw-font-bold ${current ? "tw-text-amber-700" : reached ? "tw-text-vm-primary" : "tw-text-vm-slate-500"}`}>{statusLabels[step]}</strong>
                          <small className="tw-mt-1 tw-block tw-text-[0.58rem] tw-font-medium tw-leading-4 tw-text-vm-slate-500">{current ? formatDateTime(selectedTicket.updatedAt ?? selectedTicket.createdAt) : "--"}</small>
                        </div>
                      );
                    })}
                  </div>

                  {selectedTicket.status === "RESOLVED" && (canReopenOwnTicket || canCloseOwnTicket) ? (
                    <div className="tw-mt-5 tw-flex tw-flex-wrap tw-justify-end tw-gap-2 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-4">
                      {canReopenOwnTicket ? <Button loading={saving} size="sm" variant="secondary" onClick={handleReopenTicket}>Mở lại yêu cầu</Button> : null}
                      {canCloseOwnTicket ? <Button loading={saving} size="sm" onClick={handleCloseTicket}>Đóng yêu cầu</Button> : null}
                    </div>
                  ) : null}
                </>
              ) : null}
        </Drawer>
      </div>

      <InfoBanner className="tw-mt-4" description="Khi yêu cầu được giải quyết, bạn có thể xác nhận, mở lại hoặc đóng yêu cầu theo quyền được cấp." icon={<i className="fas fa-info-circle" />} title="Theo dõi tiến độ hỗ trợ tại một nơi" />

      <Modal
        actions={(
          <div className="tw-flex tw-justify-end tw-gap-3">
            <Button disabled={saving} variant="secondary" onClick={handleCloseCreateForm}>Hủy</Button>
            <Button disabled={categoryState.loading || Boolean(categoryState.error) || !selectedCategory} loading={saving} onClick={() => void handleSubmit()}>Gửi yêu cầu</Button>
          </div>
        )}
        description="Chỉ báo lỗi chức năng hệ thống. Dùng module nghiệp vụ tương ứng để xử lý các yêu cầu vận hành."
        onClose={handleCloseCreateForm}
        open={createFormOpen}
        title="Tạo yêu cầu hỗ trợ"
        width="lg"
      >
        <div className="tw-grid tw-gap-4">
          {formError ? (
            <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-sm tw-font-bold tw-text-red-600">
              <i className="fas fa-exclamation-circle tw-mr-2" />
              {formError}
            </div>
          ) : null}
          <div className="tw-grid tw-grid-cols-1 tw-gap-4 sm:tw-grid-cols-2">
            <SupportCategoryPicker state={categoryState} disabled={saving} value={form.categoryId}
                onChange={(categoryId) => setForm((current) => ({ ...current, categoryId }))} />
            <Field label="Mức độ ưu tiên">
              <Input value={selectedCategory ? priorityLabels[selectedCategory.priority] : "Tự động theo loại yêu cầu"} readOnly />
            </Field>
          </div>
          <Field label="Tiêu đề">
            <Input maxLength={150} value={form.title} onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} placeholder="Nhập tiêu đề cần hỗ trợ" />
          </Field>
          <Field label="Nội dung">
            <textarea maxLength={4000} className="tw-min-h-32 tw-w-full tw-resize-y tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3 tw-text-[0.9rem] tw-text-vm-slate-900 tw-outline-none tw-transition placeholder:tw-text-vm-slate-500 focus:tw-border-vm-primary focus:tw-shadow-vm-focus" value={form.content} onChange={(event) => setForm((current) => ({ ...current, content: event.target.value }))} placeholder="Nêu rõ chức năng gặp lỗi, bước thực hiện và lỗi thực tế." />
          </Field>
          <small className="tw-font-semibold tw-text-vm-slate-500"><i className="fas fa-info-circle tw-mr-1" /> Yêu cầu mới sẽ ở trạng thái đang mở để nhân viên tiếp nhận.</small>
        </div>
      </Modal>
      </div>
    </PageLayout>
  );
}

export function InternalSupportPage() {
  return <SupportPage mode="internal" />;
}
