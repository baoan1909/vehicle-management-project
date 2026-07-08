import { useMemo, useState } from "react";
import type { ReactNode } from "react";
import { useSearchParams } from "react-router-dom";

import { Badge, Button, EntityAvatar } from "@/components/ui";
import { cn } from "@/lib/cn";

type ConversationStatus = "processing" | "waiting" | "closed";
type Priority = "high" | "medium" | "low";
type ParticipantType = "customer" | "employee";

interface Conversation {
  channel: string;
  customerLevel: string;
  email: string;
  id: string;
  initials: string;
  lastMessage: string;
  participantId: string;
  participantType: ParticipantType;
  phone: string;
  priority: Priority;
  priorityLabel: string;
  sla: string;
  status: ConversationStatus;
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
      <EntityAvatar initials={conversation.initials} tone={conversation.tone} />
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
  selectedId,
  onSelect,
}: {
  conversations: Conversation[];
  selectedId: string;
  onSelect: (id: string) => void;
}) {
  return (
    <aside className="tw-flex tw-min-h-0 tw-flex-col tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 tw-bg-white">
      <div className="tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-p-5">
        <div className="tw-flex tw-items-center tw-justify-between">
          <h2 className="tw-m-0 tw-text-[1.05rem] tw-font-extrabold tw-text-vm-slate-900">Hộp thoại</h2>
          <button type="button" className="tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-bg-vm-slate-25" aria-label="Bộ lọc hội thoại">
            <i className="fas fa-sliders-h" />
          </button>
        </div>
        <label className="tw-mt-4 tw-flex tw-h-10 tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3">
          <i className="fas fa-search tw-text-[0.82rem] tw-text-vm-slate-500" />
          <input className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.82rem] tw-font-semibold tw-outline-none placeholder:tw-text-vm-slate-500" placeholder="Tìm kiếm theo SĐT, biển số, mã ticket..." />
        </label>
        <div className="tw-mt-3 tw-grid tw-grid-cols-3 tw-gap-2">
          {["Tất cả kênh", "Trạng thái", "Ưu tiên"].map((label) => (
            <button key={label} type="button" className="tw-flex tw-h-9 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-2 tw-text-[0.72rem] tw-font-bold tw-text-vm-slate-700">
              <span className="tw-truncate">{label}</span>
              <i className="fas fa-chevron-down tw-text-[0.58rem]" />
            </button>
          ))}
        </div>
        <div className="tw-mt-4 tw-flex tw-gap-5 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100">
          {[
            ["Tất cả", "32"],
            ["Ticket đang xử lý", "18"],
            ["Chờ phản hồi", "7"],
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
        <span>Hiển thị 1 - 20 / 32 hội thoại</span>
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
        <EntityAvatar initials={conversation.initials} tone={conversation.tone} size="lg" />
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

function ChatMessages({ conversation }: { conversation: Conversation }) {
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

function Composer() {
  return (
    <div className="tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-pb-4 tw-pt-3">
      <div className="tw-flex tw-gap-6 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100">
        <button type="button" className="tw-relative tw-border-0 tw-bg-transparent tw-pb-3 tw-text-[0.78rem] tw-font-extrabold tw-text-vm-primary after:tw-absolute after:tw-bottom-0 after:tw-left-0 after:tw-h-0.5 after:tw-w-full after:tw-bg-vm-primary">Trả lời</button>
        <button type="button" className="tw-border-0 tw-bg-transparent tw-pb-3 tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500">Ghi chú nội bộ</button>
      </div>
      <textarea className="tw-mt-4 tw-h-20 tw-w-full tw-resize-none tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-vm-focus" placeholder="Nhập nội dung trả lời..." />
      <div className="tw-mt-2 tw-flex tw-items-center tw-justify-between">
        <div className="tw-flex tw-items-center tw-gap-3 tw-text-vm-slate-700">
          {["fas fa-paperclip", "far fa-smile", "far fa-image", "far fa-file-alt", "fas fa-ellipsis-h"].map((icon) => (
            <button key={icon} type="button" className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-white hover:tw-bg-vm-slate-25" aria-label="Công cụ trả lời">
              <i className={icon} />
            </button>
          ))}
        </div>
        <Button className="tw-w-[140px]">
          <i className="far fa-paper-plane" />
          Gửi
          <span className="tw-ml-2 tw-h-6 tw-border-0 tw-border-l tw-border-solid tw-border-white/25" />
          <i className="fas fa-chevron-down tw-text-[0.65rem]" />
        </Button>
      </div>
    </div>
  );
}

function ChatWorkspace({ conversation }: { conversation: Conversation }) {
  return (
    <main className="tw-flex tw-min-h-0 tw-flex-col tw-bg-white">
      <ChatHeader conversation={conversation} />
      <TicketStrip conversation={conversation} />
      <ChatMessages conversation={conversation} />
      <Composer />
    </main>
  );
}

function RightPanel({ className, conversation }: { className?: string; conversation: Conversation }) {
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

  return (
    <aside className={cn("tw-flex tw-min-h-0 tw-flex-col tw-gap-2.5 tw-overflow-y-auto tw-border-0 tw-border-l tw-border-solid tw-border-vm-slate-100 tw-bg-[#fbfdff] tw-p-3", className)}>
      <SectionCard title="Thông tin ticket">
        {ticketInfo.map((item) => <InfoLineView item={item} key={item.label} />)}
      </SectionCard>
      <SectionCard title={participantLabel}>
        {dynamicInfo.map((item) => <InfoLineView item={item} key={item.label} />)}
      </SectionCard>
      <SectionCard title="Thông tin liên quan">
        {relatedInfo.map((item) => <InfoLineView item={item} key={item.label} />)}
      </SectionCard>
      <div className="tw-grid tw-gap-3 tw-p-2">
        <Button className="tw-h-11 tw-w-full">
          <i className="far fa-check-square" />
          Đóng ticket
        </Button>
        <Button className="tw-h-11 tw-w-full" variant="secondary">
          <i className="fas fa-users" />
          Tạo hội thoại nội bộ
        </Button>
        <Button className="tw-h-11 tw-w-full" variant="secondary">
          <i className="fas fa-share" />
          Chuyển ticket
        </Button>
      </div>
    </aside>
  );
}

export function OperationsSupportCenterPage() {
  const [searchParams] = useSearchParams();
  const requestedParticipantId = searchParams.get("participantId");
  const requestedParticipantName = searchParams.get("participantName");

  const initialConversationId = useMemo(() => {
    if (requestedParticipantId) {
      const byParticipant = conversations.find((conversation) => conversation.participantId === requestedParticipantId);
      if (byParticipant) return byParticipant.id;
    }

    if (requestedParticipantName) {
      const normalizedName = requestedParticipantName.toLowerCase();
      const byName = conversations.find((conversation) => conversation.userName.toLowerCase() === normalizedName);
      if (byName) return byName.id;
    }

    return conversations[0].id;
  }, [requestedParticipantId, requestedParticipantName]);

  const [selectedId, setSelectedId] = useState(initialConversationId);
  const selectedConversation = conversations.find((conversation) => conversation.id === selectedId) ?? conversations[0];

  return (
    <div className="tw-h-screen tw-overflow-hidden tw-bg-white tw-text-vm-slate-700">
      <div className="tw-grid tw-h-full tw-min-h-0 tw-min-w-0 tw-grid-rows-[64px_minmax(0,1fr)]">
        <TopBar />
        <div className="tw-grid tw-min-h-0 tw-grid-cols-[390px_minmax(520px,1fr)_330px] max-[1280px]:tw-grid-cols-[360px_minmax(460px,1fr)]">
          <ConversationList conversations={conversations} selectedId={selectedConversation.id} onSelect={setSelectedId} />
          <ChatWorkspace conversation={selectedConversation} />
          <RightPanel className="max-[1280px]:tw-hidden" conversation={selectedConversation} />
        </div>
      </div>
    </div>
  );
}
