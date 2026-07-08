import { useMemo, useState } from "react";

import { Badge, Button, Card, EntityAvatar, InfoBanner, SelectMenu } from "@/components/ui";
import { openSupportCenterConversation } from "@/features/support";
import { cn } from "@/lib/cn";

type EmployeeRole = "EMPLOYEE" | "PARKING_MANAGER" | "SYSTEM_ADMIN";
type EmployeeStatus = "ACTIVE" | "PENDING" | "SUSPENDED";
type AccountStatus = "LINKED" | "UNLINKED";

type Employee = {
  accountStatus: AccountStatus;
  address: string;
  avatarTone: "blue" | "green" | "amber" | "red" | "violet";
  code: string;
  email: string;
  hiredAt: string;
  initials: string;
  jobTitle: string;
  name: string;
  phone: string;
  role: EmployeeRole;
  roleLabel: string;
  status: EmployeeStatus;
};

type Shift = {
  date: string;
  label: string;
  meta: string;
  status: string;
  tone: "success" | "warning" | "neutral";
};

type TimelineItem = {
  color: string;
  date: string;
  description: string;
  title: string;
};

const roleOptions = [
  { label: "Tất cả vai trò", value: "all" },
  { label: "Nhân viên", value: "EMPLOYEE" },
  { label: "Quản lý", value: "PARKING_MANAGER" },
  { label: "Quản trị", value: "SYSTEM_ADMIN" },
];

const statusTabs = [
  { label: "Tất cả", value: "all" },
  { label: "Hoạt động", value: "ACTIVE" },
  { label: "Chờ duyệt", value: "PENDING" },
  { label: "Tạm khóa", value: "SUSPENDED" },
] as const;

const employees: Employee[] = [
  {
    accountStatus: "LINKED",
    address: "Quận 7, TP. Hồ Chí Minh",
    avatarTone: "blue",
    code: "EMP-240045",
    email: "binh.tran@coparking.vn",
    hiredAt: "08/06/2025",
    initials: "BT",
    jobTitle: "Thu ngân",
    name: "Trần Thị Bình",
    phone: "0902 345 678",
    role: "EMPLOYEE",
    roleLabel: "Nhân viên",
    status: "ACTIVE",
  },
  {
    accountStatus: "LINKED",
    address: "Tân Bình, TP. Hồ Chí Minh",
    avatarTone: "green",
    code: "EMP-240012",
    email: "an.nguyen@coparking.vn",
    hiredAt: "12/02/2025",
    initials: "NA",
    jobTitle: "Quản lý bãi",
    name: "Nguyễn Văn An",
    phone: "0901 234 567",
    role: "PARKING_MANAGER",
    roleLabel: "Quản lý",
    status: "ACTIVE",
  },
  {
    accountStatus: "LINKED",
    address: "Thủ Đức, TP. Hồ Chí Minh",
    avatarTone: "amber",
    code: "EMP-240061",
    email: "cuong.le@coparking.vn",
    hiredAt: "19/08/2025",
    initials: "LC",
    jobTitle: "Bảo vệ ca đêm",
    name: "Lê Minh Cường",
    phone: "0911 222 333",
    role: "EMPLOYEE",
    roleLabel: "Nhân viên",
    status: "SUSPENDED",
  },
  {
    accountStatus: "UNLINKED",
    address: "Biên Hòa, Đồng Nai",
    avatarTone: "red",
    code: "EMP-240077",
    email: "dung.pham@coparking.vn",
    hiredAt: "02/10/2025",
    initials: "PD",
    jobTitle: "Kỹ thuật",
    name: "Phạm Hoàng Dũng",
    phone: "0933 567 890",
    role: "EMPLOYEE",
    roleLabel: "Nhân viên",
    status: "PENDING",
  },
  {
    accountStatus: "LINKED",
    address: "Quận 1, TP. Hồ Chí Minh",
    avatarTone: "violet",
    code: "EMP-240001",
    email: "admin@coparking.vn",
    hiredAt: "01/01/2025",
    initials: "AD",
    jobTitle: "Quản trị hệ thống",
    name: "Nguyễn Văn Admin",
    phone: "0900 111 222",
    role: "SYSTEM_ADMIN",
    roleLabel: "Quản trị",
    status: "ACTIVE",
  },
  {
    accountStatus: "UNLINKED",
    address: "Cầu Giấy, Hà Nội",
    avatarTone: "blue",
    code: "EMP-240083",
    email: "mai.nguyen@coparking.vn",
    hiredAt: "11/11/2025",
    initials: "NM",
    jobTitle: "Chăm sóc khách hàng",
    name: "Nguyễn Thị Mai",
    phone: "0918 224 466",
    role: "EMPLOYEE",
    roleLabel: "Nhân viên",
    status: "PENDING",
  },
];

const shifts: Shift[] = [
  { date: "29/06/2026", label: "Ca sáng", meta: "07:00 - 11:30 · Cổng A", status: "Đã phân ca", tone: "success" },
  { date: "30/06/2026", label: "Ca chiều", meta: "13:00 - 17:30 · Nhà xe B", status: "Sắp tới", tone: "warning" },
  { date: "01/07/2026", label: "Ca đêm", meta: "22:00 - 06:00 · Cổng chính", status: "Chưa xác nhận", tone: "neutral" },
];

const history: TimelineItem[] = [
  { color: "#2563EB", date: "28/06/2026 09:15", description: "Cập nhật Thu ngân cho EMP-240045", title: "Cập nhật chức danh" },
  { color: "#10B981", date: "26/06/2026 14:08", description: "Tài khoản nội bộ được kích hoạt", title: "Đổi trạng thái tài khoản" },
  { color: "#F97316", date: "18/06/2026 10:30", description: "Yêu cầu nội bộ đã được duyệt", title: "Duyệt yêu cầu nội bộ" },
];

function statusBadgeTone(status: EmployeeStatus) {
  if (status === "ACTIVE") return "success";
  if (status === "PENDING") return "warning";
  return "danger";
}

function accountBadgeTone(status: AccountStatus) {
  return status === "LINKED" ? "primary" : "warning";
}

function EmployeeMetric({
  icon,
  iconClassName,
  label,
  value,
}: {
  icon: string;
  iconClassName: string;
  label: string;
  value: string;
}) {
  return (
    <Card className="tw-min-h-[88px] tw-p-4">
      <div className="tw-flex tw-items-center tw-gap-4">
        <span className={cn("tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-full tw-text-[1.18rem]", iconClassName)}>
          <i className={icon} />
        </span>
        <div className="tw-min-w-0">
          <p className="tw-m-0 tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-700">{label}</p>
          <strong className="tw-mt-1 tw-block tw-text-[1.75rem] tw-font-extrabold tw-leading-none tw-text-vm-slate-900">{value}</strong>
        </div>
      </div>
    </Card>
  );
}

function EmployeeListItem({
  employee,
  onContact,
  onSelect,
  selected,
}: {
  employee: Employee;
  onContact: () => void;
  onSelect: () => void;
  selected: boolean;
}) {
  return (
    <article
      className={cn(
        "tw-flex tw-w-full tw-min-w-0 tw-items-center tw-gap-2 tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-bg-white tw-p-1.5 tw-transition",
        selected
          ? "tw-border-vm-primary tw-bg-brand-50 tw-shadow-[inset_3px_0_0_#2563EB,0_8px_18px_rgba(37,99,235,0.08)]"
          : "tw-border-transparent hover:tw-border-brand-100 hover:tw-bg-vm-slate-25",
      )}
    >
      <button type="button" className="tw-flex tw-min-w-0 tw-flex-1 tw-items-center tw-gap-2.5 tw-border-0 tw-bg-transparent tw-px-1 tw-py-1 tw-text-left" onClick={onSelect}>
      <EntityAvatar initials={employee.initials} size="sm" tone={employee.avatarTone} />
      <span className="tw-min-w-0 tw-flex-1">
        <strong className="tw-block tw-truncate tw-text-[0.83rem] tw-font-extrabold tw-text-vm-slate-900">{employee.name}</strong>
        <small className="tw-block tw-truncate tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500">
          {employee.code} · {employee.jobTitle}
        </small>
      </span>
      <Badge tone={statusBadgeTone(employee.status)} className="tw-flex-shrink-0 tw-rounded-full tw-px-2 tw-text-[0.62rem]">
        {employee.status}
      </Badge>
      </button>
      <button
        type="button"
        className="tw-inline-flex tw-h-8 tw-w-8 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-100 tw-bg-white tw-text-vm-primary hover:tw-bg-brand-50"
        onClick={onContact}
        aria-label={`Liên hệ ${employee.name}`}
        title="Liên hệ"
      >
        <i className="far fa-comment-dots tw-text-[0.9rem]" />
      </button>
    </article>
  );
}

function InfoRow({ icon, label }: { icon: string; label: string }) {
  return (
    <div className="tw-flex tw-items-center tw-gap-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-700">
      <i className={cn(icon, "tw-w-4 tw-text-center tw-text-vm-slate-500")} />
      <span>{label}</span>
    </div>
  );
}

export function EmployeeListPage() {
  const [selectedCode, setSelectedCode] = useState("EMP-240045");
  const [selectedRole, setSelectedRole] = useState("all");
  const [selectedStatus, setSelectedStatus] = useState<(typeof statusTabs)[number]["value"]>("all");

  const filteredEmployees = useMemo(() => {
    return employees.filter((employee) => {
      const matchRole = selectedRole === "all" || employee.role === selectedRole;
      const matchStatus = selectedStatus === "all" || employee.status === selectedStatus;
      return matchRole && matchStatus;
    });
  }, [selectedRole, selectedStatus]);

  const selectedEmployee = employees.find((employee) => employee.code === selectedCode) ?? employees[0];

  return (
    <div className="tw-px-4 tw-py-4 lg:tw-px-5">
      <section className="tw-mx-auto tw-min-h-[calc(100vh-104px)] tw-w-[min(100%,1500px)] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-vm-card">
        <div className="tw-mb-5 tw-flex tw-items-center tw-justify-between tw-gap-4">
          <div className="tw-flex tw-min-w-0 tw-items-center tw-gap-4">
            <h1 className="tw-m-0 tw-text-vm-page-title tw-tracking-[-0.03em] tw-text-vm-slate-900">Nhân viên</h1>
            <a className="tw-inline-flex tw-items-center tw-gap-2 tw-text-[0.86rem] tw-font-extrabold tw-text-vm-primary hover:tw-text-vm-primary-hover hover:tw-no-underline" href="#employee-help">
              <i className="far fa-question-circle tw-text-[1rem]" />
              Hướng dẫn & Trợ giúp
            </a>
          </div>
          <div className="tw-flex tw-flex-shrink-0 tw-items-center tw-gap-3">
            <Button size="lg" variant="primary">
              <i className="fas fa-plus" />
              Mời tài khoản
            </Button>
            <Button size="lg" variant="secondary">
              <i className="fas fa-download" />
              Xuất dữ liệu
              <i className="fas fa-chevron-down tw-text-[0.72rem]" />
            </Button>
          </div>
        </div>

        <div className="tw-grid tw-grid-cols-4 tw-gap-4 max-[1180px]:tw-grid-cols-2">
          <EmployeeMetric icon="fas fa-user-check" iconClassName="tw-bg-green-50 tw-text-green-600" label="Đang hoạt động" value="112" />
          <EmployeeMetric icon="far fa-clock" iconClassName="tw-bg-amber-50 tw-text-amber-500" label="Chờ duyệt nội bộ" value="8" />
          <EmployeeMetric icon="fas fa-link" iconClassName="tw-bg-violet-50 tw-text-violet-600" label="Chưa liên kết tài khoản" value="6" />
          <EmployeeMetric icon="fas fa-lock" iconClassName="tw-bg-red-50 tw-text-vm-danger" label="Tạm khóa" value="9" />
        </div>

        <div className="tw-mt-5 tw-grid tw-grid-cols-[340px_minmax(0,1fr)_300px] tw-gap-4 max-[1280px]:tw-grid-cols-[330px_minmax(0,1fr)] max-[960px]:tw-grid-cols-1">
          <Card className="tw-flex tw-h-full tw-min-h-0 tw-flex-col tw-overflow-hidden">
            <div className="tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-py-4">
              <h2 className="tw-m-0 tw-text-[0.95rem] tw-font-extrabold tw-text-vm-slate-900">Danh sách nhân viên</h2>
              <div className="tw-mt-3 tw-flex tw-h-[38px] tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3">
                <i className="fas fa-search tw-text-[0.82rem] tw-text-vm-slate-500" />
                <span className="tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Tìm tên, mã NV, email...</span>
              </div>
              <div className="tw-mt-3">
                <SelectMenu
                  ariaLabel="Vai trò"
                  value={selectedRole}
                  options={roleOptions}
                  onChange={setSelectedRole}
                />
              </div>
              <div className="tw-mt-3 tw-flex tw-flex-wrap tw-gap-2">
                {statusTabs.map((tab) => (
                  <button
                    key={tab.value}
                    type="button"
                    className={cn(
                      "tw-h-8 tw-rounded-vm-md tw-border tw-border-solid tw-px-3 tw-text-[0.72rem] tw-font-extrabold tw-transition",
                      selectedStatus === tab.value
                        ? "tw-border-vm-primary tw-bg-white tw-text-vm-primary tw-shadow-[inset_0_-2px_0_#2563EB]"
                        : "tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-text-vm-slate-700 hover:tw-border-brand-100 hover:tw-text-vm-primary",
                    )}
                    onClick={() => setSelectedStatus(tab.value)}
                  >
                    {tab.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="tw-grid tw-min-h-0 tw-flex-1 tw-content-start tw-gap-1 tw-overflow-y-auto tw-p-3 tw-pr-2 tw-[scrollbar-width:none] tw-[-ms-overflow-style:none] [&::-webkit-scrollbar]:tw-hidden">
              {filteredEmployees.map((employee) => (
                <EmployeeListItem
                  key={employee.code}
                  employee={employee}
                  onContact={() => openSupportCenterConversation({ participantId: employee.code, participantName: employee.name, participantType: "employee" })}
                  selected={employee.code === selectedEmployee.code}
                  onSelect={() => setSelectedCode(employee.code)}
                />
              ))}
            </div>

            <div className="tw-flex-shrink-0 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-px-3 tw-py-2.5">
              <Button className="tw-h-9 tw-w-full tw-text-[0.82rem]" variant="secondary">
                Xem tất cả nhân viên
              </Button>
            </div>
          </Card>

          <div className="tw-grid tw-gap-4">
            <Card className="tw-p-5">
              <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Hồ sơ nhân viên</h2>
              <div className="tw-mt-5 tw-grid tw-grid-cols-[72px_minmax(0,1fr)] tw-gap-5">
                <EntityAvatar initials={selectedEmployee.initials} size="xl" tone={selectedEmployee.avatarTone} />
                <div className="tw-min-w-0">
                  <h3 className="tw-m-0 tw-text-[1.45rem] tw-font-extrabold tw-leading-tight tw-text-vm-slate-900">{selectedEmployee.name}</h3>
                  <p className="tw-m-0 tw-mt-1 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-500">
                    {selectedEmployee.code} · {selectedEmployee.jobTitle}
                  </p>
                  <div className="tw-mt-3 tw-flex tw-flex-wrap tw-gap-2">
                    <Badge tone={statusBadgeTone(selectedEmployee.status)} className="tw-rounded-full tw-px-3">
                      {selectedEmployee.status}
                    </Badge>
                    <Badge tone={accountBadgeTone(selectedEmployee.accountStatus)} className="tw-rounded-full tw-px-3">
                      {selectedEmployee.accountStatus === "LINKED" ? "Đã liên kết tài khoản" : "Chưa liên kết"}
                    </Badge>
                    <Badge tone="neutral" className="tw-rounded-full tw-px-3">{selectedEmployee.roleLabel}</Badge>
                  </div>
                </div>
              </div>

              <div className="tw-mt-5 tw-grid tw-gap-3">
                <InfoRow icon="far fa-envelope" label={selectedEmployee.email} />
                <InfoRow icon="fas fa-phone" label={selectedEmployee.phone} />
                <InfoRow icon="far fa-calendar-check" label={`Ngày vào làm ${selectedEmployee.hiredAt}`} />
                <InfoRow icon="fas fa-map-marker-alt" label={selectedEmployee.address} />
              </div>

              <div className="tw-mt-5 tw-flex tw-flex-wrap tw-gap-3">
                <Button
                  variant="primary"
                  onClick={() => openSupportCenterConversation({ participantId: selectedEmployee.code, participantName: selectedEmployee.name, participantType: "employee" })}
                >
                  <i className="far fa-comment-dots" />
                  Liên hệ
                </Button>
                <Button variant="secondary">
                  <i className="fas fa-pen" />
                  Cập nhật
                </Button>
                <Button variant="secondary">
                  <i className="far fa-image" />
                  Đổi ảnh
                </Button>
                <Button variant="danger">
                  <i className="fas fa-lock" />
                  Tạm khóa
                </Button>
              </div>
            </Card>

            <Card className="tw-p-5">
              <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Ca làm gần đây</h2>
              <div className="tw-mt-4 tw-grid tw-grid-cols-3 tw-gap-3 max-[1180px]:tw-grid-cols-1">
                {shifts.map((shift) => (
                  <article key={`${shift.label}-${shift.date}`} className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
                    <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
                      <span className="tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-full tw-bg-brand-100 tw-text-vm-primary">
                        <i className="far fa-calendar" />
                      </span>
                      <Badge tone={shift.tone} className="tw-rounded-full tw-px-2">{shift.status}</Badge>
                    </div>
                    <h3 className="tw-m-0 tw-mt-3 tw-text-[0.92rem] tw-font-extrabold tw-text-vm-slate-900">{shift.label}</h3>
                    <p className="tw-m-0 tw-mt-1 tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">{shift.date}</p>
                    <p className="tw-m-0 tw-mt-1 tw-text-[0.76rem] tw-font-semibold tw-leading-snug tw-text-vm-slate-700">{shift.meta}</p>
                  </article>
                ))}
              </div>
            </Card>
          </div>

          <div className="tw-grid tw-gap-4 max-[1280px]:tw-col-span-2 max-[960px]:tw-col-span-1">
            <Card className="tw-p-5">
              <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Tài khoản & phân quyền</h2>
              <div className="tw-mt-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50/70 tw-p-4">
                <div className="tw-flex tw-items-center tw-gap-3">
                  <span className="tw-inline-flex tw-h-11 tw-w-11 tw-items-center tw-justify-center tw-rounded-full tw-bg-white tw-text-vm-primary">
                    <i className="fas fa-user-shield" />
                  </span>
                  <div className="tw-min-w-0">
                    <h3 className="tw-m-0 tw-text-[0.92rem] tw-font-extrabold tw-text-vm-slate-900">Tài khoản nội bộ</h3>
                    <p className="tw-m-0 tw-mt-1 tw-truncate tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">{selectedEmployee.email}</p>
                  </div>
                </div>
                <div className="tw-mt-4 tw-grid tw-gap-2">
                  <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
                    <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">Vai trò</span>
                    <Badge tone="primary" className="tw-rounded-full tw-px-3">{selectedEmployee.role}</Badge>
                  </div>
                  <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
                    <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">Trạng thái</span>
                    <Badge tone="success" className="tw-rounded-full tw-px-3">Đang hoạt động</Badge>
                  </div>
                </div>
              </div>
              <div className="tw-mt-4 tw-grid tw-grid-cols-2 tw-gap-3">
                <Button variant="secondary">
                  <i className="fas fa-key" />
                  Quyền
                </Button>
                <Button variant="secondary">
                  <i className="fas fa-external-link-alt" />
                  Tài khoản
                </Button>
              </div>
            </Card>

            <Card className="tw-p-5">
              <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Việc cần xử lý</h2>
              <div className="tw-mt-4 tw-grid tw-gap-3">
                {[
                  { icon: "fas fa-link", label: "6 tài khoản chưa liên kết", tone: "tw-bg-violet-50 tw-text-violet-600" },
                  { icon: "far fa-clock", label: "8 yêu cầu nội bộ chờ duyệt", tone: "tw-bg-amber-50 tw-text-amber-600" },
                  { icon: "fas fa-lock", label: "9 nhân viên tạm khóa", tone: "tw-bg-red-50 tw-text-vm-danger" },
                ].map((item) => (
                  <div key={item.label} className="tw-flex tw-items-center tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
                    <span className={cn("tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-full", item.tone)}>
                      <i className={item.icon} />
                    </span>
                    <strong className="tw-min-w-0 tw-flex-1 tw-text-[0.84rem] tw-font-extrabold tw-text-vm-slate-900">{item.label}</strong>
                    <i className="fas fa-chevron-right tw-text-[0.68rem] tw-text-vm-slate-500" />
                  </div>
                ))}
              </div>
            </Card>

            <Card className="tw-p-5">
              <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Lịch sử gần đây</h2>
              <div className="tw-mt-4 tw-grid tw-gap-0">
                {history.map((item, index) => (
                  <div className="tw-grid tw-grid-cols-[18px_minmax(0,1fr)] tw-gap-3" key={`${item.date}-${item.title}`}>
                    <div className="tw-relative tw-flex tw-justify-center">
                      <span className="tw-mt-1.5 tw-h-2.5 tw-w-2.5 tw-rounded-full" style={{ backgroundColor: item.color }} />
                      {index < history.length - 1 ? <span className="tw-absolute tw-bottom-0 tw-top-5 tw-w-px tw-bg-vm-slate-100" /> : null}
                    </div>
                    <div className="tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-4 last:tw-border-b-0">
                      <p className="tw-m-0 tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">{item.date}</p>
                      <h3 className="tw-m-0 tw-mt-2 tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-900">{item.title}</h3>
                      <p className="tw-m-0 tw-mt-1 tw-text-[0.76rem] tw-font-semibold tw-leading-snug tw-text-vm-slate-700">{item.description}</p>
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          </div>
        </div>

        <InfoBanner
          className="tw-mt-4"
          tone="info"
          title="Nhấn vào nhân viên để mở hồ sơ; tác vụ phức tạp mở bằng modal/drawer riêng."
          description="Trang mặc định tập trung vào điều phối nội bộ, tài khoản và phân quyền, không mở drawer chi tiết sẵn."
          icon={<i className="fas fa-info-circle" />}
        />
      </section>
    </div>
  );
}
