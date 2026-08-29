import { useEffect, useMemo, useState } from "react";

import { Badge, Button, Drawer, EntityAvatar, SelectMenu } from "@/components/ui";
import type { EmployeeApiResponse } from "@/features/employees/api/employeesApi";
import type { SupportTicketPriority, SupportTicketResponse, SupportTicketStatus } from "@/features/support/api/supportApi";
import { cn } from "@/lib/cn";

type SupportTicketDetailDrawerProps = {
  actionError: string;
  actionLoading: boolean;
  canAssign: boolean;
  canReply: boolean;
  canResolve: boolean;
  canStartProgress: boolean;
  employees: EmployeeApiResponse[];
  onAssign: (assignedTo: string) => void;
  onClose: () => void;
  onReply: () => void;
  onRequestResolve: () => void;
  onStartProgress: () => void;
  open: boolean;
  ticket: SupportTicketResponse | null;
};

function formatDateTime(value: string | null) {
  if (!value) return "--";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
}

function shortTicketCode(ticketId: string) {
  return `TK-${ticketId.replace(/-/g, "").slice(0, 10).toUpperCase()}`;
}

function employeeName(employee: EmployeeApiResponse) {
  return employee.userProfile?.fullName?.trim() || employee.accountUsername || employee.employeeCode || "Nhân viên chưa có tên";
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
  if (status === "IN_PROGRESS") return "primary" as const;
  return "primary" as const;
}

function DetailLine({ icon, label, value }: { icon: string; label: string; value: string }) {
  return (
    <div className="tw-grid tw-grid-cols-[20px_minmax(0,1fr)] tw-gap-3 tw-text-[0.82rem]">
      <i className={cn(icon, "tw-pt-0.5 tw-text-vm-slate-500")} />
      <div className="tw-grid tw-gap-1">
        <span className="tw-font-semibold tw-text-vm-slate-500">{label}</span>
        <strong className="tw-break-words tw-font-black tw-text-vm-slate-900">{value}</strong>
      </div>
    </div>
  );
}

export function SupportTicketDetailDrawer({
  actionError,
  actionLoading,
  canAssign,
  canReply,
  canResolve,
  canStartProgress,
  employees,
  onAssign,
  onClose,
  onReply,
  onRequestResolve,
  onStartProgress,
  open,
  ticket,
}: SupportTicketDetailDrawerProps) {
  const [selectedAssignee, setSelectedAssignee] = useState("");

  useEffect(() => {
    setSelectedAssignee(ticket?.assignedTo ?? "");
  }, [ticket?.assignedTo, ticket?.supportTicketId]);

  const employeeOptions = useMemo(
    () => employees
      .filter((employee) => employee.accountId)
      .map((employee) => ({
        label: `${employeeName(employee)}${employee.jobTitle ? ` — ${employee.jobTitle}` : ""}`,
        value: employee.accountId as string,
      })),
    [employees],
  );

  if (!ticket) return null;

  const assignedEmployee = employees.find((employee) => employee.accountId === ticket.assignedTo);
  const canSubmitAssignment = canAssign && Boolean(selectedAssignee) && selectedAssignee !== ticket.assignedTo;

  return (
    <Drawer
      actions={
        <div className="tw-grid tw-gap-2">
          {actionError ? <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-p-3 tw-text-[0.8rem] tw-font-bold tw-text-red-600">{actionError}</div> : null}
          {canStartProgress && !canReply && !canResolve && !canAssign ? (
            <div className="tw-grid tw-grid-cols-2 tw-gap-2">
              <Button className="tw-w-full" disabled={actionLoading} onClick={onStartProgress}><i className="fas fa-play" />Nhận xử lý</Button>
              <Button className="tw-w-full" disabled={actionLoading} variant="secondary" onClick={onClose}>Đóng</Button>
            </div>
          ) : (
            <>
              {canStartProgress || canReply || canResolve ? (
                <div className="tw-grid tw-gap-2">
                  {canStartProgress ? <Button disabled={actionLoading} onClick={onStartProgress}><i className="fas fa-play" />Nhận xử lý</Button> : null}
                  {canReply ? <Button disabled={actionLoading} variant="secondary" onClick={onReply}><i className="far fa-comment-dots" />Phản hồi khách hàng</Button> : null}
                  {canResolve ? <Button disabled={actionLoading} onClick={onRequestResolve}><i className="far fa-check-circle" />Giải quyết ticket</Button> : null}
                </div>
              ) : null}
              <div className={cn("tw-grid tw-gap-2", canAssign ? "tw-grid-cols-[1fr_1.15fr]" : "tw-grid-cols-1")}>
                <Button disabled={actionLoading} variant="secondary" onClick={onClose}>Đóng</Button>
                {canAssign ? (
                  <Button disabled={!canSubmitAssignment} loading={actionLoading} onClick={() => onAssign(selectedAssignee)}>
                    <i className="fas fa-user-check" />
                    {ticket.assignedTo ? "Cập nhật phân công" : "Phân công"}
                  </Button>
                ) : null}
              </div>
            </>
          )}
        </div>
      }
      description="Theo dõi nội dung yêu cầu và điều phối nhân viên phụ trách"
      onClose={onClose}
      open={open}
      title="Thông tin yêu cầu hỗ trợ"
      width="lg"
    >
      <div className="tw-grid tw-gap-5">
        <section className="tw-grid tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-4">
          <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
            <strong className="tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900">{shortTicketCode(ticket.supportTicketId)}</strong>
            <button aria-label="Tùy chọn yêu cầu" className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-white tw-text-vm-slate-500 hover:tw-bg-vm-slate-100" type="button"><i className="fas fa-ellipsis-h" /></button>
          </div>
          <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2">
            <Badge tone={statusTone(ticket.status)} className="tw-rounded-vm-sm tw-px-2.5">{statusLabel(ticket.status)}</Badge>
            <Badge tone={priorityTone(ticket.priority)} className="tw-rounded-vm-sm tw-px-2.5">{priorityLabel(ticket.priority)}</Badge>
            <span className="tw-text-[0.74rem] tw-font-bold tw-text-vm-slate-500"><i className="far fa-clock tw-mr-1.5" />Tạo {formatDateTime(ticket.createdAt)}</span>
          </div>
        </section>

        <section className="tw-grid tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-5">
          <h4 className="tw-m-0 tw-text-[0.82rem] tw-font-black tw-uppercase tw-tracking-[0.06em] tw-text-vm-slate-500">Khách hàng gửi yêu cầu</h4>
          <div className="tw-grid tw-grid-cols-[48px_minmax(0,1fr)] tw-gap-3 tw-items-center">
            <span className="tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-full tw-bg-brand-50 tw-text-[1rem] tw-font-black tw-text-vm-primary"><i className="far fa-user" /></span>
            <div className="tw-grid tw-gap-1">
              <strong className="tw-text-[0.92rem] tw-font-black tw-text-vm-slate-900">Khách hàng</strong>
              <span className="tw-break-all tw-text-[0.77rem] tw-font-semibold tw-text-vm-slate-500">Mã khách hàng: {ticket.customerId}</span>
            </div>
          </div>
        </section>

        <section className="tw-grid tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-5">
          <h4 className="tw-m-0 tw-text-[0.82rem] tw-font-black tw-uppercase tw-tracking-[0.06em] tw-text-vm-slate-500">Nội dung yêu cầu</h4>
          <div>
            <span className="tw-text-[0.73rem] tw-font-bold tw-text-vm-slate-500">Danh mục</span>
            <p className="tw-mb-0 tw-mt-1 tw-text-[0.86rem] tw-font-black tw-text-vm-slate-900">{ticket.categoryName || ticket.categoryCode || "Chưa xác định"}</p>
          </div>
          <div>
            <span className="tw-text-[0.73rem] tw-font-bold tw-text-vm-slate-500">Tiêu đề</span>
            <p className="tw-mb-0 tw-mt-1 tw-text-[1rem] tw-font-black tw-leading-6 tw-text-vm-slate-900">{ticket.title}</p>
          </div>
          <p className="tw-m-0 tw-whitespace-pre-wrap tw-text-[0.86rem] tw-font-medium tw-leading-6 tw-text-vm-slate-700">{ticket.content}</p>
        </section>

        <section className="tw-grid tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-5">
          <h4 className="tw-m-0 tw-text-[0.82rem] tw-font-black tw-uppercase tw-tracking-[0.06em] tw-text-vm-slate-500">Nhật ký xử lý</h4>
          <DetailLine icon="far fa-calendar-plus" label="Thời gian tạo" value={formatDateTime(ticket.createdAt)} />
          <DetailLine icon="far fa-clock" label="Cập nhật gần nhất" value={formatDateTime(ticket.updatedAt)} />
          <DetailLine icon="far fa-check-circle" label="Hoàn tất xử lý" value={formatDateTime(ticket.resolvedAt)} />
          {ticket.resolutionNote ? <DetailLine icon="far fa-comment-dots" label="Ghi chú xử lý" value={ticket.resolutionNote} /> : null}
        </section>

        {canAssign ? (
          <section className="tw-grid tw-gap-3">
            <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
              <h4 className="tw-m-0 tw-text-[0.82rem] tw-font-black tw-uppercase tw-tracking-[0.06em] tw-text-vm-slate-500">Phân công xử lý</h4>
              {ticket.assignedTo ? <Badge tone="success" className="tw-rounded-vm-sm tw-px-2.5">Đã phân công</Badge> : <Badge tone="warning" className="tw-rounded-vm-sm tw-px-2.5">Chưa phân công</Badge>}
            </div>
            {assignedEmployee ? <div className="tw-flex tw-items-center tw-gap-2 tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-600"><EntityAvatar initials={initials(employeeName(assignedEmployee))} size="sm" src={assignedEmployee.userProfile?.avatarUrl} tone="green" />Đang phụ trách: <strong className="tw-text-vm-slate-900">{employeeName(assignedEmployee)}</strong></div> : null}
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">Nhân viên phụ trách <span className="tw-text-vm-danger">*</span></span>
              <SelectMenu
                ariaLabel="Nhân viên phụ trách"
                disabled={actionLoading || employeeOptions.length === 0}
                onChange={setSelectedAssignee}
                options={employeeOptions}
                portal
                searchable
                searchPlaceholder="Tìm nhân viên đang hoạt động..."
                triggerLabel={selectedAssignee ? undefined : "Chọn nhân viên phụ trách"}
                value={selectedAssignee}
              />
            </label>
            {employeeOptions.length === 0 ? <p className="tw-m-0 tw-text-[0.78rem] tw-font-semibold tw-text-amber-700">Chưa tải được nhân viên đang hoạt động hoặc danh sách trống.</p> : null}
          </section>
        ) : null}
      </div>
    </Drawer>
  );
}
