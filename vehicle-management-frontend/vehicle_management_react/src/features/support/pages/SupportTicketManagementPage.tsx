import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import { Badge, Button, Card, EntityAvatar, Modal, PaginationFooter, SearchInput, SelectMenu, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { getEmployees, type EmployeeApiResponse } from "@/features/employees/api/employeesApi";
import { SupportTicketDetailDrawer } from "@/features/support/components/SupportTicketDetailDrawer";
import {
  approveSupportTicketEscalation,
  assignSupportTicket,
  getSupportTicketById,
  getSupportTicketCategories,
  getSupportTickets,
  getSupportTicketEscalations,
  openSupportTicketCustomerConversation,
  resolveSupportTicket,
  rejectSupportTicketEscalation,
  type SupportTicketEscalationDecision,
  type SupportTicketEscalationResponse,
  type SupportTicketCategoryResponse,
  type SupportTicketPriority,
  type SupportTicketResponse,
  type SupportTicketStatus,
} from "@/features/support/api/supportApi";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { cn } from "@/lib/cn";

const pageSizeOptions = [10, 20, 50];

const statusOptions = [
  { label: "Trạng thái", value: "all" },
  { label: "Mở", value: "OPEN" },
  { label: "Đang xử lý", value: "IN_PROGRESS" },
  { label: "Đã xử lý", value: "RESOLVED" },
  { label: "Đã đóng", value: "CLOSED" },
];

const priorityOptions = [
  { label: "Ưu tiên", value: "all" },
  { label: "Khẩn cấp", value: "URGENT" },
  { label: "Cao", value: "HIGH" },
  { label: "Trung bình", value: "NORMAL" },
  { label: "Thấp", value: "LOW" },
];

function shortTicketCode(ticketId: string) {
  return `TK-${ticketId.replace(/-/g, "").slice(0, 10).toUpperCase()}`;
}

function formatDate(value: string | null) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--";
  return new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }).format(date);
}

function employeeName(employee: EmployeeApiResponse | undefined) {
  if (!employee) return "";
  return employee.userProfile?.fullName?.trim() || employee.accountUsername || employee.employeeCode || "Nhân viên chưa có tên";
}

function assignedLabel(ticket: SupportTicketResponse, employee: EmployeeApiResponse | undefined, currentUserId?: string, currentUserName?: string) {
  if (!ticket.assignedTo) return "Chưa phân công";
  if (employee) return employeeName(employee);
  if (ticket.assignedTo === currentUserId) return currentUserName?.trim() || "Bạn";
  return "Đã phân công";
}

function initials(value: string) {
  return value
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join("") || "NV";
}

function priorityLabel(priority: SupportTicketPriority) {
  return ({ URGENT: "Khẩn cấp", HIGH: "Cao", NORMAL: "Trung bình", LOW: "Thấp" } as const)[priority];
}

function priorityTone(priority: SupportTicketPriority) {
  if (priority === "URGENT" || priority === "HIGH") return "danger" as const;
  if (priority === "NORMAL") return "warning" as const;
  return "success" as const;
}

function statusLabel(status: SupportTicketStatus) {
  return ({ OPEN: "Mở", IN_PROGRESS: "Đang xử lý", RESOLVED: "Đã xử lý", CLOSED: "Đã đóng" } as const)[status];
}

function statusTone(status: SupportTicketStatus) {
  if (status === "CLOSED") return "neutral" as const;
  if (status === "RESOLVED") return "success" as const;
  return "primary" as const;
}

function QueueMetric({ icon, iconClassName, label, value }: { icon: string; iconClassName: string; label: string; value: number }) {
  return (
    <Card className="tw-flex tw-min-h-[98px] tw-items-center tw-gap-4 tw-p-4 tw-shadow-none">
      <span className={cn("tw-inline-flex tw-h-12 tw-w-12 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-text-[1.2rem]", iconClassName)}><i className={icon} /></span>
      <span className="tw-grid tw-gap-1">
        <span className="tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">{label}</span>
        <strong className="tw-text-[1.7rem] tw-font-black tw-leading-none tw-text-vm-slate-900">{value.toLocaleString("vi-VN")}</strong>
      </span>
    </Card>
  );
}

export function SupportTicketManagementPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const canAssign = hasAnyPermission(user, ["SUPPORT_TICKET_ASSIGN"]);
  const canReviewEscalations = canAssign && hasAnyPermission(user, ["SUPPORT_TICKET_ESCALATION_REVIEW_ALL"]);
  const canProcessAll = hasAnyPermission(user, ["SUPPORT_TICKET_PROCESS_ALL"]);
  const canProcessAssigned = hasAnyPermission(user, ["SUPPORT_TICKET_PROCESS_ASSIGNED"]);
  const canReadAssigned = hasAnyPermission(user, ["SUPPORT_TICKET_READ_ASSIGNED"]);
  const canRespondAssigned = hasAnyPermission(user, ["SUPPORT_TICKET_RESPOND_ASSIGNED"]);
  const canCreateCustomerDirect = hasAnyPermission(user, ["CHAT_CONVERSATION_CREATE_CUSTOMER_DIRECT"]);
  const [tickets, setTickets] = useState<SupportTicketResponse[]>([]);
  const [escalations, setEscalations] = useState<SupportTicketEscalationResponse[]>([]);
  const [categories, setCategories] = useState<SupportTicketCategoryResponse[]>([]);
  const [employees, setEmployees] = useState<EmployeeApiResponse[]>([]);
  const [selectedTicket, setSelectedTicket] = useState<SupportTicketResponse | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [resolutionNote, setResolutionNote] = useState("");
  const [resolutionOpen, setResolutionOpen] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [priorityFilter, setPriorityFilter] = useState("all");
  const [assigneeFilter, setAssigneeFilter] = useState("all");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const loadQueue = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [ticketResult, categoryResult, employeeResult, escalationResult] = await Promise.allSettled([
        getSupportTickets(),
        getSupportTicketCategories(),
        getEmployees({ status: "ACTIVE" }),
        canReviewEscalations ? getSupportTicketEscalations("PENDING") : Promise.resolve(null),
      ]);

      if (ticketResult.status === "rejected") {
        throw ticketResult.reason;
      }

      setTickets(ticketResult.value.data);
      setCategories(categoryResult.status === "fulfilled" ? categoryResult.value.data : []);
      setEmployees(employeeResult.status === "fulfilled" ? employeeResult.value?.data ?? [] : []);
      setEscalations(escalationResult.status === "fulfilled" && escalationResult.value ? escalationResult.value.data ?? [] : []);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Không thể tải danh sách yêu cầu hỗ trợ.");
    } finally {
      setLoading(false);
    }
  }, [canReviewEscalations]);

  useEffect(() => { void loadQueue(); }, [loadQueue]);
  useEffect(() => { setCurrentPage(1); }, [keyword, statusFilter, priorityFilter, assigneeFilter, pageSize]);

  const employeeByAccountId = useMemo(() => new Map(employees.filter((employee) => employee.accountId).map((employee) => [employee.accountId as string, employee])), [employees]);
  const categoryOptions = useMemo(() => [{ label: "Tất cả danh mục", value: "all" }, ...categories.map((category) => ({ label: category.name, value: category.categoryId }))], [categories]);
  const [categoryFilter, setCategoryFilter] = useState("all");

  useEffect(() => { setCurrentPage(1); }, [categoryFilter]);

  const assigneeOptions = useMemo(() => [
    { label: "Nhân viên phụ trách", value: "all" },
    { label: "Chưa phân công", value: "unassigned" },
    ...employees.filter((employee) => employee.accountId).map((employee) => ({ label: employeeName(employee), value: employee.accountId as string })),
  ], [employees]);

  const filteredTickets = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLocaleLowerCase("vi-VN");
    return tickets.filter((ticket) => {
      const matchesKeyword = !normalizedKeyword || [ticket.title, ticket.content, ticket.categoryName, shortTicketCode(ticket.supportTicketId), ticket.customerId]
        .filter((value): value is string => Boolean(value))
        .some((value) => value.toLocaleLowerCase("vi-VN").includes(normalizedKeyword));
      const matchesStatus = statusFilter === "all" || ticket.status === statusFilter;
      const matchesPriority = priorityFilter === "all" || ticket.priority === priorityFilter;
      const matchesCategory = categoryFilter === "all" || ticket.categoryId === categoryFilter;
      const matchesAssignee = assigneeFilter === "all" || (assigneeFilter === "unassigned" ? !ticket.assignedTo : ticket.assignedTo === assigneeFilter);
      return matchesKeyword && matchesStatus && matchesPriority && matchesCategory && matchesAssignee;
    });
  }, [assigneeFilter, categoryFilter, keyword, priorityFilter, statusFilter, tickets]);

  const totalPages = Math.max(1, Math.ceil(filteredTickets.length / pageSize));
  const safePage = Math.min(currentPage, totalPages);
  const startIndex = filteredTickets.length === 0 ? 0 : (safePage - 1) * pageSize + 1;
  const pageTickets = filteredTickets.slice((safePage - 1) * pageSize, safePage * pageSize);
  const endIndex = filteredTickets.length === 0 ? 0 : startIndex + pageTickets.length - 1;
  const metrics = useMemo(() => ({
    open: tickets.filter((ticket) => ticket.status === "OPEN").length,
    unassigned: tickets.filter((ticket) => !ticket.assignedTo && ticket.status !== "CLOSED").length,
    inProgress: tickets.filter((ticket) => ticket.status === "IN_PROGRESS").length,
    attention: tickets.filter((ticket) => ((ticket.priority === "URGENT" || ticket.priority === "HIGH") || escalations.some((item) => item.supportTicketId === ticket.supportTicketId)) && ticket.status !== "CLOSED").length,
  }), [escalations, tickets]);
  const escalationByTicketId = useMemo(() => new Map(escalations.map((item) => [item.supportTicketId, item])), [escalations]);
  const canProcessSelectedTicket = Boolean(selectedTicket && (
    canProcessAll || (canProcessAssigned && selectedTicket.assignedTo === user?.id)
  ));
  const canReplySelectedTicket = Boolean(
    (selectedTicket?.status === "OPEN" || selectedTicket?.status === "IN_PROGRESS")
      && selectedTicket.assignedTo === user?.id
      && canProcessSelectedTicket
      && canReadAssigned
      && canRespondAssigned
      && canCreateCustomerDirect,
  );
  const canResolveSelectedTicket = canProcessSelectedTicket && selectedTicket?.status === "IN_PROGRESS";

  async function openTicket(ticket: SupportTicketResponse) {
    setSelectedTicket(ticket);
    setDrawerOpen(true);
    setActionError("");
    try {
      const response = await getSupportTicketById(ticket.supportTicketId);
      setSelectedTicket(response.data);
    } catch {
      // The queue item remains usable when the optional detail refresh cannot be completed.
    }
  }

  async function handleAssign(assignedTo: string) {
    if (!selectedTicket) return;
    setActionLoading(true);
    setActionError("");
    try {
      const response = await assignSupportTicket(selectedTicket.supportTicketId, assignedTo);
      setSelectedTicket(response.data);
      setTickets((current) => current.map((ticket) => ticket.supportTicketId === response.data.supportTicketId ? response.data : ticket));
      toast.success("Đã phân công yêu cầu cho nhân viên phụ trách.");
    } catch (assignError) {
      setActionError(assignError instanceof Error ? assignError.message : "Không thể phân công yêu cầu này.");
    } finally {
      setActionLoading(false);
    }
  }

  async function handleReviewEscalation(
    decision: SupportTicketEscalationDecision,
    assignedTo: string | undefined,
    note: string,
  ) {
    if (!selectedTicket) return;
    const escalation = escalationByTicketId.get(selectedTicket.supportTicketId);
    if (!escalation) return;
    setActionLoading(true);
    setActionError("");
    try {
      await approveSupportTicketEscalation(escalation.escalationId, { decision, assignedTo, note });
      const refreshedTicket = await getSupportTicketById(selectedTicket.supportTicketId);
      applyTicketUpdate(refreshedTicket.data);
      setEscalations((current) => current.filter((item) => item.escalationId !== escalation.escalationId));
      toast.success(decision === "REASSIGN" ? "Đã chuyển phiếu cho người phụ trách mới." : "Đã ghi nhận quyết định giữ người phụ trách hiện tại.");
    } catch (caught) {
      setActionError(caught instanceof Error ? caught.message : "Không thể xử lý yêu cầu xem xét.");
    } finally {
      setActionLoading(false);
    }
  }

  async function handleRejectEscalation(note: string) {
    if (!selectedTicket) return;
    const escalation = escalationByTicketId.get(selectedTicket.supportTicketId);
    if (!escalation) return;
    setActionLoading(true);
    setActionError("");
    try {
      await rejectSupportTicketEscalation(escalation.escalationId, note);
      setEscalations((current) => current.filter((item) => item.escalationId !== escalation.escalationId));
      toast.success("Đã ghi nhận quyết định từ chối và thông báo cho khách hàng.");
    } catch (caught) {
      setActionError(caught instanceof Error ? caught.message : "Không thể từ chối yêu cầu xem xét.");
    } finally {
      setActionLoading(false);
    }
  }

  function applyTicketUpdate(ticket: SupportTicketResponse) {
    setSelectedTicket(ticket);
    setTickets((current) => current.map((currentTicket) => currentTicket.supportTicketId === ticket.supportTicketId ? ticket : currentTicket));
  }

  async function handleReplyCustomer() {
    if (!selectedTicket) return;
    // Open synchronously from the click event so browsers do not block the tab while the API request is pending.
    const supportCenterTab = window.open("", "_blank");
    if (supportCenterTab) supportCenterTab.opener = null;
    setActionLoading(true);
    setActionError("");
    try {
      const response = await openSupportTicketCustomerConversation(selectedTicket.supportTicketId);
      const supportCenterUrl = `/admin/support-center?conversationId=${encodeURIComponent(response.data.conversationId)}&ticketId=${encodeURIComponent(selectedTicket.supportTicketId)}`;
      if (supportCenterTab) {
        supportCenterTab.location.assign(supportCenterUrl);
        toast.success("Đã mở hội thoại riêng với khách hàng ở tab mới.");
      } else {
        navigate(supportCenterUrl);
        toast.info("Trình duyệt đã chặn tab mới nên hội thoại được mở tại tab hiện tại.");
      }
    } catch (replyError) {
      supportCenterTab?.close();
      setActionError(replyError instanceof Error ? replyError.message : "Không thể mở hội thoại với khách hàng.");
    } finally {
      setActionLoading(false);
    }
  }

  async function handleResolveTicket() {
    if (!selectedTicket) return;
    const note = resolutionNote.trim();
    if (!note) {
      setActionError("Vui lòng nhập kết quả xử lý trước khi giải quyết ticket.");
      return;
    }

    setActionLoading(true);
    setActionError("");
    try {
      const response = await resolveSupportTicket(selectedTicket.supportTicketId, note);
      applyTicketUpdate(response.data);
      setResolutionOpen(false);
      setResolutionNote("");
      toast.success("Đã đánh dấu ticket là đã giải quyết.");
    } catch (resolveError) {
      setActionError(resolveError instanceof Error ? resolveError.message : "Không thể giải quyết ticket này.");
    } finally {
      setActionLoading(false);
    }
  }

  return (
    <main className="tw-min-h-full tw-bg-[#f7f9fc] tw-p-4 min-[1100px]:tw-p-5">
      <div className="tw-mx-auto tw-grid tw-max-w-[1540px] tw-gap-4">
        <header className="tw-flex tw-flex-wrap tw-items-end tw-justify-between tw-gap-3">
          <div>
            <div className="tw-flex tw-items-center tw-gap-2">
              <h1 className="tw-m-0 tw-text-[1.56rem] tw-font-black tw-tracking-[-0.03em] tw-text-vm-slate-900">Yêu cầu hỗ trợ</h1>
              <span className="tw-inline-flex tw-items-center tw-gap-1.5 tw-rounded-full tw-bg-brand-50 tw-px-2.5 tw-py-1 tw-text-[0.7rem] tw-font-black tw-text-vm-primary"><i className="fas fa-shield-alt" />Điều phối vận hành</span>
            </div>
            <p className="tw-mb-0 tw-mt-1 tw-text-[0.84rem] tw-font-medium tw-text-vm-slate-500">Theo dõi yêu cầu của khách hàng, chọn ticket để xem chi tiết và phân công xử lý.</p>
          </div>
          <Button disabled={loading} variant="secondary" onClick={() => void loadQueue()}><i className="fas fa-sync-alt" />Làm mới</Button>
        </header>

        <section className="tw-grid tw-grid-cols-2 tw-gap-3 min-[1040px]:tw-grid-cols-4">
          <QueueMetric icon="far fa-clipboard" iconClassName="tw-bg-brand-50 tw-text-vm-primary" label="Mới" value={metrics.open} />
          <QueueMetric icon="far fa-user" iconClassName="tw-bg-amber-50 tw-text-amber-600" label="Chưa phân công" value={metrics.unassigned} />
          <QueueMetric icon="fas fa-spinner" iconClassName="tw-bg-blue-50 tw-text-blue-600" label="Đang xử lý" value={metrics.inProgress} />
          <QueueMetric icon="fas fa-exclamation" iconClassName="tw-bg-red-50 tw-text-red-600" label="Cần chú ý" value={metrics.attention} />
        </section>

        <Card className="tw-overflow-hidden tw-shadow-none">
          <div className="tw-flex tw-flex-nowrap tw-items-center tw-gap-2 tw-overflow-x-auto tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-p-3 max-[900px]:tw-flex-wrap max-[900px]:tw-overflow-visible">
            <SearchInput containerClassName="tw-min-w-[240px] !tw-w-auto tw-flex-1 max-[900px]:!tw-w-full" onChange={setKeyword} placeholder="Tìm mã ticket, khách hàng, nội dung..." value={keyword} />
            <SelectMenu ariaLabel="Trạng thái ticket" className="!tw-w-[150px] tw-flex-shrink-0 max-[900px]:!tw-w-[calc(50%-4px)]" onChange={setStatusFilter} options={statusOptions} portal value={statusFilter} />
            <SelectMenu ariaLabel="Ưu tiên ticket" className="!tw-w-[135px] tw-flex-shrink-0 max-[900px]:!tw-w-[calc(50%-4px)]" onChange={setPriorityFilter} options={priorityOptions} portal value={priorityFilter} />
            <SelectMenu ariaLabel="Danh mục hỗ trợ" className="!tw-w-[175px] tw-flex-shrink-0 max-[900px]:!tw-w-[calc(50%-4px)]" onChange={setCategoryFilter} options={categoryOptions} portal portalFitContent searchable value={categoryFilter} />
            <SelectMenu ariaLabel="Nhân viên phụ trách" className="!tw-w-[190px] tw-flex-shrink-0 max-[900px]:!tw-w-[calc(50%-4px)]" onChange={setAssigneeFilter} options={assigneeOptions} portal portalFitContent searchable value={assigneeFilter} />
            <button aria-label="Xóa bộ lọc" className="tw-inline-flex tw-h-[42px] tw-flex-shrink-0 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-text-[0.9rem] tw-font-bold tw-text-vm-slate-700 tw-transition hover:tw-border-vm-slate-200 hover:tw-bg-vm-slate-25 hover:tw-text-vm-primary" type="button" onClick={() => { setKeyword(""); setStatusFilter("all"); setPriorityFilter("all"); setCategoryFilter("all"); setAssigneeFilter("all"); }}><i className="fas fa-sync-alt" /><span>Xóa bộ lọc</span></button>
          </div>

          <div className="tw-overflow-x-auto">
            <div className="tw-min-w-[1050px]">
              <div className="tw-grid tw-grid-cols-[150px_150px_minmax(160px,1fr)_110px_130px_175px_42px] tw-items-center tw-bg-vm-slate-25 tw-px-3 tw-py-3 tw-text-[0.69rem] tw-font-black tw-text-vm-slate-700">
                <span>Mã ticket</span><span>Khách hàng</span><span>Danh mục / yêu cầu</span><span>Ưu tiên</span><span>Trạng thái</span><span>Nhân viên phụ trách</span><span />
              </div>
              {pageTickets.map((ticket) => {
                const assignee = employeeByAccountId.get(ticket.assignedTo ?? "");
                const assigneeDisplayName = assignedLabel(ticket, assignee, user?.id, user?.fullName);
                const isCurrentAssignee = Boolean(ticket.assignedTo && ticket.assignedTo === user?.id);
                return (
                  <button className="tw-grid tw-min-h-[67px] tw-w-full tw-grid-cols-[150px_150px_minmax(160px,1fr)_110px_130px_175px_42px] tw-items-center tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-left tw-transition hover:tw-bg-brand-50/45 focus-visible:tw-relative focus-visible:tw-z-[1] focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus" key={ticket.supportTicketId} type="button" onClick={() => void openTicket(ticket)}>
                    <span className="tw-grid tw-gap-1"><strong className="tw-text-[0.75rem] tw-font-black tw-text-vm-primary">{shortTicketCode(ticket.supportTicketId)}</strong><small className="tw-text-[0.68rem] tw-font-semibold tw-text-vm-slate-500">{formatDate(ticket.createdAt)}</small></span>
                    <span className="tw-min-w-0 tw-pr-3"><strong className="tw-block tw-truncate tw-text-[0.76rem] tw-font-bold tw-text-vm-slate-900">Khách hàng</strong><small className="tw-block tw-truncate tw-text-[0.67rem] tw-font-semibold tw-text-vm-slate-500">{ticket.customerId}</small>{escalationByTicketId.has(ticket.supportTicketId) ? <small className="tw-mt-1 tw-block tw-font-black tw-text-amber-700"><i className="fas fa-user-shield tw-mr-1" />Yêu cầu xem xét</small> : null}</span>
                    <span className="tw-min-w-0 tw-pr-3"><strong className="tw-block tw-truncate tw-text-[0.77rem] tw-font-bold tw-text-vm-slate-900">{ticket.categoryName || ticket.categoryCode || "Khác"}</strong><small className="tw-mt-1 tw-block tw-truncate tw-text-[0.7rem] tw-font-semibold tw-text-vm-slate-500">{ticket.title}</small></span>
                    <Badge tone={priorityTone(ticket.priority)} className="tw-w-fit tw-rounded-vm-sm tw-px-2">{priorityLabel(ticket.priority)}</Badge>
                    <Badge tone={statusTone(ticket.status)} className="tw-w-fit tw-rounded-vm-sm tw-px-2">{statusLabel(ticket.status)}</Badge>
                    <span className="tw-flex tw-min-w-0 tw-items-center tw-gap-2 tw-pr-2">
                      {assignee ? <EntityAvatar initials={initials(assigneeDisplayName)} size="sm" src={assignee.userProfile?.avatarUrl} tone="green" /> : isCurrentAssignee ? <EntityAvatar initials={initials(assigneeDisplayName)} size="sm" src={user?.avatarUrl} tone="green" /> : ticket.assignedTo ? <span className="tw-grid tw-h-8 tw-w-8 tw-flex-shrink-0 tw-place-items-center tw-rounded-full tw-bg-green-50 tw-text-[0.72rem] tw-text-green-700"><i className="fas fa-user-check" /></span> : <span className="tw-grid tw-h-8 tw-w-8 tw-flex-shrink-0 tw-place-items-center tw-rounded-full tw-bg-vm-slate-100 tw-text-[0.72rem] tw-text-vm-slate-500"><i className="far fa-user-clock" /></span>}
                      <strong className="tw-truncate tw-text-[0.72rem] tw-font-bold tw-text-vm-slate-700">{assigneeDisplayName}</strong>
                    </span>
                    <i className="fas fa-ellipsis-v tw-justify-self-end tw-text-vm-slate-500" />
                  </button>
                );
              })}
              {pageTickets.length === 0 ? <div className="tw-grid tw-place-items-center tw-gap-2 tw-py-14 tw-text-center"><i className="far fa-folder-open tw-text-[1.7rem] tw-text-vm-slate-400" /><strong className="tw-text-[0.88rem] tw-text-vm-slate-700">{loading ? "Đang tải yêu cầu hỗ trợ..." : error || "Không có yêu cầu phù hợp với bộ lọc."}</strong>{error ? <Button size="sm" variant="secondary" onClick={() => void loadQueue()}>Thử lại</Button> : null}</div> : null}
            </div>
          </div>
          <PaginationFooter ariaLabel="Phân trang yêu cầu hỗ trợ" currentPage={safePage} endIndex={endIndex} onPageChange={setCurrentPage} onPageSizeChange={setPageSize} pageSize={pageSize} pageSizeOptions={pageSizeOptions} startIndex={startIndex} totalPages={totalPages} totalRecords={filteredTickets.length} />
        </Card>
      </div>

      <SupportTicketDetailDrawer
        actionError={actionError}
        actionLoading={actionLoading}
        canAssign={canAssign}
        canReply={canReplySelectedTicket}
        canResolve={canResolveSelectedTicket}
        canReviewEscalation={Boolean(canReviewEscalations && selectedTicket?.assignedTo !== user?.id)}
        employees={employees}
        escalation={selectedTicket ? escalationByTicketId.get(selectedTicket.supportTicketId) : null}
        reviewerAccountId={user?.id}
        onAssign={(assignedTo) => void handleAssign(assignedTo)}
        onClose={() => { setDrawerOpen(false); setActionError(""); }}
        onReply={() => void handleReplyCustomer()}
        onRequestResolve={() => { setActionError(""); setResolutionNote(""); setResolutionOpen(true); }}
        onReviewEscalation={(decision, assignedTo, note) => void handleReviewEscalation(decision, assignedTo, note)}
        onRejectEscalation={(note) => void handleRejectEscalation(note)}
        open={drawerOpen}
        ticket={selectedTicket}
      />
      <Modal
        actions={<div className="tw-flex tw-justify-end tw-gap-2"><Button disabled={actionLoading} variant="secondary" onClick={() => setResolutionOpen(false)}>Hủy</Button><Button loading={actionLoading} onClick={() => void handleResolveTicket()}><i className="far fa-check-circle" />Xác nhận giải quyết</Button></div>}
        description="Kết quả này được lưu vào ticket để khách hàng theo dõi và là điều kiện trước khi đóng ticket."
        onClose={() => { if (!actionLoading) setResolutionOpen(false); }}
        open={resolutionOpen}
        title="Giải quyết ticket"
        width="md"
      >
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.82rem] tw-font-black tw-text-vm-slate-700">Kết quả xử lý <span className="tw-text-vm-danger">*</span></span>
          <textarea className="tw-min-h-[130px] tw-w-full tw-resize-y tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3 tw-text-[0.88rem] tw-font-medium tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-vm-focus" placeholder="Mô tả cách xử lý, kết quả và lưu ý dành cho khách hàng..." value={resolutionNote} onChange={(event) => setResolutionNote(event.target.value)} />
        </label>
        {actionError ? <p className="tw-mb-0 tw-mt-3 tw-rounded-vm-md tw-bg-red-50 tw-p-3 tw-text-[0.8rem] tw-font-bold tw-text-red-600">{actionError}</p> : null}
      </Modal>
    </main>
  );
}
