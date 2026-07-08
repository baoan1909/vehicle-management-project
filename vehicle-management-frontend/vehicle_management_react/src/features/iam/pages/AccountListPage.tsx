import { useMemo, useState } from "react";

import { Badge, Button, Card, EntityAvatar, InfoBanner, SelectMenu } from "@/components/ui";
import { cn } from "@/lib/cn";
import { AccountCreateDrawer } from "../components/AccountCreateDrawer";

type RoleCode = "SYSTEM_ADMIN" | "PARKING_MANAGER" | "EMPLOYEE" | "CUSTOMER";
type AccountStatus = "ACTIVE" | "LOCKED" | "DISABLED" | "PENDING";

type ProvisionedAccount = {
  accountId: string;
  createdAt: string;
  email: string;
  fullName: string;
  initials: string;
  keycloakUserId: string;
  permissionCodes: string[];
  permissionCount: number;
  roleCode: RoleCode;
  roleName: string;
  status: AccountStatus;
  updatedAt: string;
  username: string;
};

const roleOptions = [
  { label: "Tất cả vai trò", value: "all" },
  { label: "SYSTEM_ADMIN", value: "SYSTEM_ADMIN" },
  { label: "PARKING_MANAGER", value: "PARKING_MANAGER" },
  { label: "EMPLOYEE", value: "EMPLOYEE" },
  { label: "CUSTOMER", value: "CUSTOMER" },
];

const statusOptions = [
  { label: "Tất cả trạng thái", value: "all" },
  { label: "ACTIVE", value: "ACTIVE" },
  { label: "PENDING", value: "PENDING" },
  { label: "LOCKED", value: "LOCKED" },
  { label: "DISABLED", value: "DISABLED" },
];

const statusTabs: Array<{ label: string; value: AccountStatus | "all" }> = [
  { label: "Tất cả", value: "all" },
  { label: "ACTIVE", value: "ACTIVE" },
  { label: "PENDING", value: "PENDING" },
  { label: "LOCKED", value: "LOCKED" },
  { label: "DISABLED", value: "DISABLED" },
];

const accounts: ProvisionedAccount[] = [
  {
    accountId: "acc-001",
    createdAt: "01/01/2025",
    email: "admin@coparking.vn",
    fullName: "Nguyễn Văn Admin",
    initials: "AD",
    keycloakUserId: "kc-9f41-****-a201",
    permissionCodes: ["ACCOUNT_READ_ALL", "ACCOUNT_CREATE_ALL", "ACCOUNT_UPDATE_ALL", "ROLE_UPDATE_ALL"],
    permissionCount: 32,
    roleCode: "SYSTEM_ADMIN",
    roleName: "Quản trị hệ thống",
    status: "ACTIVE",
    updatedAt: "28/06/2026",
    username: "admin.system",
  },
  {
    accountId: "acc-002",
    createdAt: "12/02/2025",
    email: "manager@coparking.vn",
    fullName: "Nguyễn Văn An",
    initials: "NA",
    keycloakUserId: "kc-72ab-****-01de",
    permissionCodes: ["CUSTOMER_READ_ALL", "CARD_READ_ALL", "SUBSCRIPTION_READ_ALL", "REPORT_READ_ALL"],
    permissionCount: 24,
    roleCode: "PARKING_MANAGER",
    roleName: "Quản lý bãi xe",
    status: "ACTIVE",
    updatedAt: "26/06/2026",
    username: "manager.parking",
  },
  {
    accountId: "acc-003",
    createdAt: "08/06/2025",
    email: "binh.tran@coparking.vn",
    fullName: "Trần Thị Bình",
    initials: "BT",
    keycloakUserId: "kc-a7c2-****-b901",
    permissionCodes: ["ACCOUNT_READ_ALL", "CUSTOMER_READ_ALL", "CARD_READ_ALL", "SUBSCRIPTION_READ_ALL"],
    permissionCount: 18,
    roleCode: "EMPLOYEE",
    roleName: "Nhân viên vận hành",
    status: "ACTIVE",
    updatedAt: "28/06/2026",
    username: "binh.tran",
  },
  {
    accountId: "acc-004",
    createdAt: "20/06/2026",
    email: "pending@coparking.vn",
    fullName: "Phạm Hoàng Dũng",
    initials: "PD",
    keycloakUserId: "kc-143e-****-0f31",
    permissionCodes: ["CUSTOMER_READ_ALL", "CARD_READ_ALL", "SUBSCRIPTION_READ_ALL"],
    permissionCount: 18,
    roleCode: "EMPLOYEE",
    roleName: "Nhân viên vận hành",
    status: "PENDING",
    updatedAt: "20/06/2026",
    username: "pending.staff",
  },
  {
    accountId: "acc-005",
    createdAt: "03/03/2026",
    email: "locked@coparking.vn",
    fullName: "Lê Minh Cường",
    initials: "LC",
    keycloakUserId: "kc-778e-****-ab54",
    permissionCodes: ["PUBLIC_INFO_READ_PUBLIC", "CUSTOMER_READ_OWN"],
    permissionCount: 6,
    roleCode: "CUSTOMER",
    roleName: "Khách hàng",
    status: "LOCKED",
    updatedAt: "18/06/2026",
    username: "locked.user",
  },
  {
    accountId: "acc-006",
    createdAt: "11/05/2026",
    email: "disabled@coparking.vn",
    fullName: "Đỗ Quang Huy",
    initials: "DQ",
    keycloakUserId: "kc-3c8e-****-b552",
    permissionCodes: ["CUSTOMER_READ_OWN"],
    permissionCount: 4,
    roleCode: "CUSTOMER",
    roleName: "Khách hàng",
    status: "DISABLED",
    updatedAt: "19/06/2026",
    username: "disabled.user",
  },
];

function statusBadgeTone(status: AccountStatus) {
  if (status === "ACTIVE") return "success";
  if (status === "PENDING") return "warning";
  if (status === "LOCKED") return "danger";
  return "neutral";
}

function roleBadgeTone(roleCode: RoleCode) {
  if (roleCode === "SYSTEM_ADMIN") return "danger";
  if (roleCode === "PARKING_MANAGER") return "primary";
  if (roleCode === "EMPLOYEE") return "success";
  return "neutral";
}

function AccountMetric({
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

function AccountRow({
  account,
  onSelect,
  selected,
}: {
  account: ProvisionedAccount;
  onSelect: () => void;
  selected: boolean;
}) {
  return (
    <button
      type="button"
      className={cn(
        "tw-grid tw-w-full tw-grid-cols-[minmax(210px,1.35fr)_150px_110px_90px_96px] tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-py-3 tw-text-left tw-transition last:tw-border-b-0 max-[1180px]:tw-grid-cols-[minmax(220px,1fr)_110px_90px]",
        selected ? "tw-bg-brand-50 tw-shadow-[inset_3px_0_0_#2563EB]" : "hover:tw-bg-vm-slate-25",
      )}
      onClick={onSelect}
    >
      <span className="tw-flex tw-min-w-0 tw-items-center tw-gap-3">
        <EntityAvatar initials={account.initials} size="md" tone={account.roleCode === "SYSTEM_ADMIN" ? "red" : account.roleCode === "PARKING_MANAGER" ? "blue" : "green"} />
        <span className="tw-min-w-0">
          <strong className="tw-block tw-truncate tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-900">{account.username}</strong>
          <small className="tw-block tw-truncate tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">{account.email}</small>
        </span>
      </span>
      <Badge tone={roleBadgeTone(account.roleCode)} className="tw-w-fit tw-rounded-full tw-px-3 max-[1180px]:tw-hidden">
        {account.roleCode}
      </Badge>
      <Badge tone={statusBadgeTone(account.status)} className="tw-w-fit tw-rounded-full tw-px-3">
        {account.status}
      </Badge>
      <span className="tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-700">{account.permissionCount} quyền</span>
      <span className="tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500 max-[1180px]:tw-hidden">{account.updatedAt}</span>
    </button>
  );
}

export function AccountListPage() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selectedAccountId, setSelectedAccountId] = useState("acc-003");
  const [selectedRole, setSelectedRole] = useState("all");
  const [selectedStatus, setSelectedStatus] = useState<AccountStatus | "all">("all");

  const filteredAccounts = useMemo(() => {
    return accounts.filter((account) => {
      const matchesRole = selectedRole === "all" || account.roleCode === selectedRole;
      const matchesStatus = selectedStatus === "all" || account.status === selectedStatus;
      return matchesRole && matchesStatus;
    });
  }, [selectedRole, selectedStatus]);

  const selectedAccount = accounts.find((account) => account.accountId === selectedAccountId) ?? accounts[0];

  return (
    <>
      <div className="tw-px-4 tw-py-4 lg:tw-px-5">
        <section className="tw-mx-auto tw-min-h-[calc(100vh-104px)] tw-w-[min(100%,1500px)] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-vm-card">
          <div className="tw-mb-5 tw-flex tw-items-center tw-justify-between tw-gap-4">
            <div className="tw-flex tw-min-w-0 tw-items-center tw-gap-4">
              <h1 className="tw-m-0 tw-text-vm-page-title tw-tracking-[-0.03em] tw-text-vm-slate-900">Tài khoản</h1>
              <a className="tw-inline-flex tw-items-center tw-gap-2 tw-text-[0.86rem] tw-font-extrabold tw-text-vm-primary hover:tw-text-vm-primary-hover hover:tw-no-underline" href="#account-help">
                <i className="far fa-question-circle tw-text-[1rem]" />
                Hướng dẫn & Trợ giúp
              </a>
            </div>
            <div className="tw-flex tw-flex-shrink-0 tw-items-center tw-gap-3">
              <Button size="lg" variant="primary" onClick={() => setDrawerOpen(true)}>
                <i className="fas fa-plus" />
                Tạo tài khoản
              </Button>
              <Button size="lg" variant="secondary">
                <i className="fas fa-download" />
                Xuất dữ liệu
                <i className="fas fa-chevron-down tw-text-[0.72rem]" />
              </Button>
            </div>
          </div>

          <div className="tw-grid tw-grid-cols-4 tw-gap-4 max-[1180px]:tw-grid-cols-2">
            <AccountMetric icon="fas fa-users-cog" iconClassName="tw-bg-brand-100 tw-text-vm-primary" label="Tổng tài khoản" value="148" />
            <AccountMetric icon="fas fa-user-check" iconClassName="tw-bg-green-50 tw-text-green-600" label="Đang hoạt động" value="126" />
            <AccountMetric icon="far fa-clock" iconClassName="tw-bg-amber-50 tw-text-amber-500" label="Chờ kích hoạt" value="14" />
            <AccountMetric icon="fas fa-lock" iconClassName="tw-bg-red-50 tw-text-vm-danger" label="Bị khóa" value="8" />
          </div>

          <Card className="tw-mt-5 tw-p-4">
            <div className="tw-flex tw-items-center tw-justify-between tw-gap-4 max-[1180px]:tw-flex-col max-[1180px]:tw-items-stretch">
              <div className="tw-min-w-0">
                <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Bộ lọc tài khoản</h2>
                <p className="tw-m-0 tw-mt-1 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Lọc theo username, vai trò và trạng thái tài khoản cấp sẵn.</p>
              </div>
              <div className="tw-grid tw-flex-1 tw-grid-cols-[minmax(260px,1.5fr)_190px_190px_auto] tw-items-center tw-gap-3 max-[1180px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
                <div className="tw-flex tw-h-[42px] tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3">
                  <i className="fas fa-search tw-text-[0.82rem] tw-text-vm-slate-500" />
                  <span className="tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-500">Tìm username, email...</span>
                </div>
                <SelectMenu ariaLabel="Vai trò tài khoản" value={selectedRole} options={roleOptions} onChange={setSelectedRole} />
                <SelectMenu ariaLabel="Trạng thái tài khoản" value={selectedStatus} options={statusOptions} onChange={(value) => setSelectedStatus(value as AccountStatus | "all")} />
                <Button className="tw-h-[42px]" variant="secondary">
                  <i className="fas fa-sync-alt" />
                  Xóa bộ lọc
                </Button>
              </div>
            </div>

            <div className="tw-mt-4 tw-flex tw-flex-wrap tw-items-center tw-gap-2">
              <span className="tw-mr-1 tw-text-[0.78rem] tw-font-extrabold tw-uppercase tw-tracking-[0.04em] tw-text-vm-slate-500">Trạng thái nhanh</span>
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
              <span className="tw-ml-auto tw-flex tw-flex-wrap tw-gap-2 max-[1180px]:tw-ml-0">
                {["ACCOUNT_READ_ALL", "ACCOUNT_CREATE_ALL", "ACCOUNT_UPDATE_ALL"].map((permission) => (
                  <Badge key={permission} tone="primary" className="tw-rounded-full tw-px-2 tw-text-[0.65rem]">
                    {permission}
                  </Badge>
                ))}
              </span>
            </div>
          </Card>

          <div className="tw-mt-4 tw-grid tw-grid-cols-[minmax(0,1fr)_340px] tw-gap-4 max-[1280px]:tw-grid-cols-1">
            <Card className="tw-overflow-hidden">
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-py-4">
                <div>
                  <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Danh sách tài khoản</h2>
                  <p className="tw-m-0 tw-mt-1 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Quản lý tài khoản cấp sẵn, vai trò và trạng thái đồng bộ Keycloak.</p>
                </div>
              </div>
              <div className="tw-grid tw-grid-cols-[minmax(210px,1.35fr)_150px_110px_90px_96px] tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-px-4 tw-py-3 tw-text-[0.75rem] tw-font-extrabold tw-uppercase tw-tracking-[0.04em] tw-text-vm-slate-500 max-[1180px]:tw-grid-cols-[minmax(220px,1fr)_110px_90px]">
                <span>Tài khoản</span>
                <span className="max-[1180px]:tw-hidden">Vai trò</span>
                <span>Trạng thái</span>
                <span>Quyền</span>
                <span className="max-[1180px]:tw-hidden">Cập nhật</span>
              </div>
              <div className="tw-max-h-[470px] tw-overflow-y-auto tw-[scrollbar-width:none] tw-[-ms-overflow-style:none] [&::-webkit-scrollbar]:tw-hidden">
                {filteredAccounts.map((account) => (
                  <AccountRow
                    key={account.accountId}
                    account={account}
                    selected={account.accountId === selectedAccount.accountId}
                    onSelect={() => setSelectedAccountId(account.accountId)}
                  />
                ))}
              </div>
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-4 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-py-3">
                <p className="tw-m-0 tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-500">Hiển thị 1 đến 10 của 148 tài khoản</p>
                <div className="tw-flex tw-items-center tw-gap-2">
                  <Button size="sm" variant="secondary"><i className="fas fa-chevron-left" /></Button>
                  <span className="tw-inline-flex tw-h-8 tw-min-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-vm-primary tw-px-2 tw-text-[0.9rem] tw-font-extrabold tw-text-white">1</span>
                  <Button size="sm" variant="secondary"><i className="fas fa-chevron-right" /></Button>
                </div>
              </div>
            </Card>

            <Card className="tw-flex tw-min-h-full tw-flex-col tw-p-5 max-[1280px]:tw-col-span-2 max-[960px]:tw-col-span-1">
              <div className="tw-flex tw-items-start tw-justify-between tw-gap-4">
                <h2 className="tw-m-0 tw-text-[1.05rem] tw-font-extrabold tw-text-vm-slate-900">Chi tiết tài khoản</h2>
                <Badge tone={statusBadgeTone(selectedAccount.status)} className="tw-rounded-full tw-px-3">{selectedAccount.status}</Badge>
              </div>

              <div className="tw-mt-5 tw-rounded-vm-lg tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50/70 tw-p-4">
                <div className="tw-flex tw-items-center tw-gap-4">
                  <EntityAvatar initials={selectedAccount.initials} size="xl" tone={selectedAccount.roleCode === "SYSTEM_ADMIN" ? "red" : selectedAccount.roleCode === "PARKING_MANAGER" ? "blue" : "green"} />
                  <div className="tw-min-w-0">
                    <h3 className="tw-m-0 tw-truncate tw-text-[1.18rem] tw-font-extrabold tw-text-vm-slate-900">{selectedAccount.username}</h3>
                    <p className="tw-m-0 tw-mt-1 tw-truncate tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">{selectedAccount.email}</p>
                    <div className="tw-mt-3 tw-flex tw-flex-wrap tw-gap-2">
                      <Badge tone={roleBadgeTone(selectedAccount.roleCode)} className="tw-rounded-full tw-px-3">{selectedAccount.roleCode}</Badge>
                      <Badge tone="neutral" className="tw-rounded-full tw-px-3">{selectedAccount.roleName}</Badge>
                    </div>
                  </div>
                </div>
              </div>

              <div className="tw-mt-4 tw-grid tw-gap-3 tw-text-[0.86rem]">
                {[
                  ["Keycloak ID", selectedAccount.keycloakUserId],
                  ["Ngày tạo", selectedAccount.createdAt],
                  ["Cập nhật", selectedAccount.updatedAt],
                  ["Account ID", selectedAccount.accountId],
                ].map(([label, value]) => (
                  <div key={label} className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-3 last:tw-border-b-0">
                    <span className="tw-font-bold tw-text-vm-slate-500">{label}</span>
                    <strong className="tw-min-w-0 tw-truncate tw-text-right tw-font-extrabold tw-text-vm-slate-900">{value}</strong>
                  </div>
                ))}
              </div>

              <div className="tw-mt-5">
                <h3 className="tw-m-0 tw-text-[0.92rem] tw-font-extrabold tw-text-vm-slate-900">Quyền nổi bật</h3>
                <div className="tw-mt-3 tw-flex tw-flex-wrap tw-gap-2">
                  {selectedAccount.permissionCodes.map((permission) => (
                    <Badge key={permission} tone="primary" className="tw-rounded-full tw-px-2 tw-text-[0.65rem]">
                      {permission}
                    </Badge>
                  ))}
                </div>
              </div>

              <div className="tw-mt-auto tw-grid tw-grid-cols-1 tw-gap-3 tw-pt-5">
                <Button variant="secondary">
                  <i className="fas fa-user-shield" />
                  Đổi vai trò
                </Button>
                <Button variant="secondary">
                  <i className="fas fa-toggle-on" />
                  Đổi trạng thái
                </Button>
                <Button variant="primary">
                  <i className="fas fa-key" />
                  Xem quyền
                </Button>
              </div>
            </Card>
          </div>

          <InfoBanner
            className="tw-mt-4"
            tone="info"
            title="Tạo mới, đổi vai trò và đổi trạng thái sẽ mở bằng drawer/modal để tránh quá tải màn hình."
            description="Trang chính tập trung vào quan sát, lọc và chọn tài khoản; thao tác phức tạp được tách riêng."
            icon={<i className="fas fa-info-circle" />}
          />
        </section>
      </div>

      <AccountCreateDrawer isOpen={drawerOpen} onClose={() => setDrawerOpen(false)} />
    </>
  );
}
