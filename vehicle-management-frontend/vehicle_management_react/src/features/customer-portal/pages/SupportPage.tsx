import { useCallback, useEffect, useMemo, useState } from "react";

import { Modal, useToast } from "@/components/ui";
import {
  getCustomerPortalProfile,
  type CustomerPortalProfile,
} from "@/features/customer-portal/api/customerPortalApi";
import {
  closeSupportTicket,
  createSupportTicket,
  getSupportTicketCategories,
  getSupportTickets,
  reopenSupportTicket,
  type SupportTicketCategoryResponse,
  type SupportTicketPriority,
  type SupportTicketResponse,
  type SupportTicketStatus,
} from "@/features/support/api/supportApi";

import { CustomerPageHeader, CustomerPortalLayout, Field, PaginationLite, StatCard, StatusPill } from "./PortalShared";

type PillTone = "green" | "blue" | "orange" | "red" | "gray" | "purple";

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

const statusTone: Record<SupportTicketStatus, PillTone> = {
  CLOSED: "gray",
  IN_PROGRESS: "orange",
  OPEN: "blue",
  RESOLVED: "green",
};

const priorityTone: Record<SupportTicketPriority, PillTone> = {
  HIGH: "red",
  LOW: "green",
  NORMAL: "blue",
  URGENT: "red",
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
    month: "2-digit",
    year: "numeric",
  }).format(date);
}

function shortCode(id: string) {
  return `TK-${id.slice(0, 8).toUpperCase()}`;
}

function activeStepClass(ticket: SupportTicketResponse, step: SupportTicketStatus) {
  const currentIndex = workflow.indexOf(ticket.status);
  const stepIndex = workflow.indexOf(step);

  if (stepIndex < currentIndex || ticket.status === "CLOSED") {
    return "done";
  }

  if (stepIndex === currentIndex) {
    return "current";
  }

  return undefined;
}

export function SupportPage() {
  const toast = useToast();
  const [profile, setProfile] = useState<CustomerPortalProfile | null>(null);
  const [tickets, setTickets] = useState<SupportTicketResponse[]>([]);
  const [categories, setCategories] = useState<SupportTicketCategoryResponse[]>([]);
  const [selectedTicketId, setSelectedTicketId] = useState<string>("");
  const [keyword, setKeyword] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [statusFilter, setStatusFilter] = useState<SupportTicketStatus | "ALL">("ALL");
  const [priorityFilter, setPriorityFilter] = useState<SupportTicketPriority | "ALL">("ALL");
  const [form, setForm] = useState(emptyForm);
  const [formError, setFormError] = useState("");
  const [createFormOpen, setCreateFormOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const loadData = useCallback(async (preferredTicketId?: string) => {
    setLoading(true);
    try {
      const nextProfile = await getCustomerPortalProfile();
      const customerId = nextProfile.customer?.customerId;
      if (!customerId) {
        throw new Error("Tài khoản hiện tại chưa liên kết hồ sơ khách hàng.");
      }

      const [ticketResponse, categoryResponse] = await Promise.all([
        getSupportTickets({ customerId }),
        getSupportTicketCategories({ status: "ACTIVE" }),
      ]);
      const nextTickets = ticketResponse.data ?? [];

      setProfile(nextProfile);
      setTickets(nextTickets);
      setCategories(categoryResponse.data ?? []);
      setCurrentPage(1);
      setSelectedTicketId((current) => {
        if (preferredTicketId && nextTickets.some((ticket) => ticket.supportTicketId === preferredTicketId)) {
          return preferredTicketId;
        }

        if (current && nextTickets.some((ticket) => ticket.supportTicketId === current)) {
          return current;
        }

        return nextTickets[0]?.supportTicketId ?? "";
      });
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể tải dữ liệu hỗ trợ.");
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

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
    () => tickets.find((ticket) => ticket.supportTicketId === selectedTicketId) ?? filteredTickets[0] ?? null,
    [filteredTickets, selectedTicketId, tickets],
  );

  const stats = useMemo(() => ({
    inProgress: tickets.filter((ticket) => ticket.status === "IN_PROGRESS").length,
    open: tickets.filter((ticket) => ticket.status === "OPEN").length,
    resolved: tickets.filter((ticket) => ticket.status === "RESOLVED").length,
  }), [tickets]);

  const selectedCategory = categories.find((category) => category.categoryId === form.categoryId);

  const handleSubmit = async () => {
    setFormError("");
    const title = form.title.trim();
    const content = form.content.trim();

    if (!form.categoryId || !title || !content) {
      setFormError("Vui lòng chọn loại yêu cầu, nhập tiêu đề và nội dung cần hỗ trợ.");
      return;
    }

    setSaving(true);
    try {
      const response = await createSupportTicket({
        categoryId: form.categoryId,
        content,
        title,
      });
      setForm(emptyForm);
      setCreateFormOpen(false);
      toast.success("Đã gửi yêu cầu hỗ trợ.");
      await loadData(response.data.supportTicketId);
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "Không thể gửi yêu cầu hỗ trợ.");
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
      const response = await closeSupportTicket(selectedTicket.supportTicketId);
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
      const response = await reopenSupportTicket(selectedTicket.supportTicketId);
      toast.success("Đã mở lại yêu cầu hỗ trợ.");
      await loadData(response.data.supportTicketId);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể mở lại yêu cầu hỗ trợ.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <CustomerPortalLayout>
      <CustomerPageHeader
        title="Hỗ trợ"
        subtitle="Gửi yêu cầu và theo dõi tình trạng xử lý hỗ trợ."
        action={<button type="button" onClick={handleOpenCreateForm}><i className="fas fa-plus" /> Tạo yêu cầu mới</button>}
      />

      <div className="vm-support-layout">
        <div>
          <div className="vm-stat-grid vm-stat-grid-three">
            <StatCard icon="far fa-question-circle" label="Yêu cầu đang mở" value={String(stats.open)} note={<StatusPill tone="blue">Đang mở</StatusPill>} />
            <StatCard icon="fas fa-headset" label="Đang xử lý" value={String(stats.inProgress)} note={<StatusPill tone="orange">Đang xử lý</StatusPill>} tone="orange" />
            <StatCard icon="far fa-check-circle" label="Đã giải quyết" value={String(stats.resolved)} note={<StatusPill>Đã giải quyết</StatusPill>} tone="green" />
          </div>
          <section className="vm-customer-card vm-table-card vm-support-table-card">
            <h2>Danh sách yêu cầu hỗ trợ</h2>
            <div className="vm-table-filters">
              <label>
                <i className="fas fa-search" />
                <input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="Tìm theo tiêu đề, nội dung..." />
              </label>
              <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as SupportTicketStatus | "ALL")}>
                <option value="ALL">Tất cả trạng thái</option>
                {workflow.map((status) => <option key={status} value={status}>{statusLabels[status]}</option>)}
              </select>
              <select value={priorityFilter} onChange={(event) => setPriorityFilter(event.target.value as SupportTicketPriority | "ALL")}>
                <option value="ALL">Tất cả mức độ</option>
                {Object.entries(priorityLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
            </div>
            <table className="vm-customer-table">
              <thead>
                <tr>
                  <th>Mã yêu cầu</th>
                  <th>Tiêu đề</th>
                  <th>Loại yêu cầu</th>
                  <th>Mức độ</th>
                  <th>Trạng thái</th>
                  <th>Người xử lý</th>
                  <th>Ngày tạo</th>
                  <th>Ngày xử lý</th>
                </tr>
              </thead>
              <tbody>
                {pagedTickets.map((ticket) => (
                  <tr
                    key={ticket.supportTicketId}
                    className={`vm-interactive-row${selectedTicket?.supportTicketId === ticket.supportTicketId ? " vm-selected-row" : ""}`}
                    role="button"
                    tabIndex={0}
                    aria-label={`Xem chi tiết ${shortCode(ticket.supportTicketId)}`}
                    aria-pressed={selectedTicket?.supportTicketId === ticket.supportTicketId}
                    onClick={() => setSelectedTicketId(ticket.supportTicketId)}
                    onKeyDown={(event) => {
                      if (event.key === "Enter" || event.key === " ") {
                        event.preventDefault();
                        setSelectedTicketId(ticket.supportTicketId);
                      }
                    }}
                  >
                    <td>{shortCode(ticket.supportTicketId)}</td>
                    <td>{ticket.title}</td>
                    <td>{ticket.categoryName ?? ticket.categoryCode ?? "--"}</td>
                    <td><StatusPill tone={priorityTone[ticket.priority]}>{priorityLabels[ticket.priority]}</StatusPill></td>
                    <td><StatusPill tone={statusTone[ticket.status]}>{statusLabels[ticket.status]}</StatusPill></td>
                    <td>{ticket.assignedTo ?? "--"}</td>
                    <td>{formatDateTime(ticket.createdAt)}</td>
                    <td>{formatDateTime(ticket.resolvedAt)}</td>
                  </tr>
                ))}
                {!loading && filteredTickets.length === 0 && (
                  <tr>
                    <td colSpan={8}>Chưa có yêu cầu hỗ trợ phù hợp với bộ lọc.</td>
                  </tr>
                )}
                {loading && (
                  <tr>
                    <td colSpan={8}>Đang tải dữ liệu hỗ trợ...</td>
                  </tr>
                )}
              </tbody>
            </table>
            <PaginationLite
              currentPage={safeCurrentPage}
              pageSize={pageSize}
              totalRecords={filteredTickets.length}
              onPageChange={setCurrentPage}
              onPageSizeChange={setPageSize}
            />
          </section>
        </div>

        <aside className="vm-support-side">
          <section className="vm-customer-card vm-ticket-detail">
            <h2>{selectedTicket ? `Chi tiết ${shortCode(selectedTicket.supportTicketId)}` : "Chi tiết yêu cầu"}</h2>
            {selectedTicket ? (
              <>
                <dl className="vm-info-list">
                  <dt>Khách hàng:</dt><dd>{profile?.profile?.fullName ?? profile?.account?.username ?? selectedTicket.customerId}</dd>
                  <dt>Trạng thái:</dt><dd><StatusPill tone={statusTone[selectedTicket.status]}>{statusLabels[selectedTicket.status]}</StatusPill></dd>
                  <dt>Mức độ:</dt><dd><StatusPill tone={priorityTone[selectedTicket.priority]}>{priorityLabels[selectedTicket.priority]}</StatusPill></dd>
                  <dt>Loại yêu cầu:</dt><dd>{selectedTicket.categoryName ?? selectedTicket.categoryCode ?? "--"}</dd>
                  <dt>Người xử lý:</dt><dd>{selectedTicket.assignedTo ?? "Chưa phân công"}</dd>
                  <dt>Ngày tạo:</dt><dd>{formatDateTime(selectedTicket.createdAt)}</dd>
                  <dt>Ngày giải quyết:</dt><dd>{formatDateTime(selectedTicket.resolvedAt)}</dd>
                </dl>
                <h3>Nội dung</h3>
                <p>{selectedTicket.content}</p>
                {selectedTicket.resolutionNote && (
                  <>
                    <h3>Kết quả xử lý</h3>
                    <p>{selectedTicket.resolutionNote}</p>
                  </>
                )}
                <div className="vm-ticket-steps">
                  {workflow.map((step) => (
                    <span key={step} className={activeStepClass(selectedTicket, step)}>
                      {statusLabels[step]}
                      <small>{step === selectedTicket.status ? formatDateTime(selectedTicket.updatedAt ?? selectedTicket.createdAt) : "--"}</small>
                    </span>
                  ))}
                </div>
                <div className="vm-form-actions">
                  <button className="vm-outline-btn" type="button" disabled={saving || selectedTicket.status !== "RESOLVED"} onClick={handleReopenTicket}>
                    Mở lại yêu cầu
                  </button>
                  <button type="button" disabled={saving || selectedTicket.status === "CLOSED"} onClick={handleCloseTicket}>
                    Đóng yêu cầu
                  </button>
                </div>
              </>
            ) : (
              <p>Chọn một yêu cầu trong bảng để xem chi tiết xử lý.</p>
            )}
          </section>
        </aside>
      </div>

      <div className="vm-info-note"><i className="fas fa-info-circle" /> Bạn có thể theo dõi tình trạng xử lý tại trang này. Khi yêu cầu được giải quyết, bạn có thể xác nhận và đóng yêu cầu.</div>

      <Modal
        actions={(
          <div className="tw-flex tw-justify-end tw-gap-3">
            <button
              className="tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-font-bold tw-text-vm-slate-700"
              type="button"
              disabled={saving}
              onClick={handleCloseCreateForm}
            >
              Hủy
            </button>
            <button
              className="tw-h-10 tw-rounded-vm-md tw-border-0 tw-bg-vm-primary tw-px-4 tw-font-bold tw-text-white disabled:tw-bg-vm-slate-200"
              type="button"
              disabled={saving}
              onClick={() => void handleSubmit()}
            >
              {saving ? "Đang gửi..." : "Gửi yêu cầu"}
            </button>
          </div>
        )}
        description="Cung cấp đầy đủ thông tin để nhân viên có thể tiếp nhận và xử lý yêu cầu nhanh chóng."
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
            <Field label="Loại yêu cầu">
              <select value={form.categoryId} onChange={(event) => setForm((current) => ({ ...current, categoryId: event.target.value }))}>
                <option value="">Chọn loại yêu cầu</option>
                {categories.map((category) => (
                  <option key={category.categoryId} value={category.categoryId}>{category.name}</option>
                ))}
              </select>
            </Field>
            <Field label="Mức độ ưu tiên">
              <input value={selectedCategory ? priorityLabels[selectedCategory.priority] : "Tự động theo loại yêu cầu"} readOnly />
            </Field>
          </div>
          <Field label="Tiêu đề">
            <input value={form.title} onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} placeholder="Nhập tiêu đề cần hỗ trợ" />
          </Field>
          <Field label="Nội dung">
            <textarea className="tw-min-h-32" value={form.content} onChange={(event) => setForm((current) => ({ ...current, content: event.target.value }))} placeholder="Mô tả chi tiết vấn đề bạn cần hỗ trợ..." />
          </Field>
          <small className="tw-font-semibold tw-text-vm-slate-500"><i className="fas fa-info-circle tw-mr-1" /> Yêu cầu mới sẽ ở trạng thái đang mở để nhân viên tiếp nhận.</small>
        </div>
      </Modal>
    </CustomerPortalLayout>
  );
}
