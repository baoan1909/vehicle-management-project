import { useEffect, useMemo, useState, type KeyboardEvent } from "react";
import { useSearchParams } from "react-router-dom";

import { Badge, Button, Card, EntityAvatar, InfoBanner, SearchInput, SelectMenu } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { cn } from "@/lib/cn";
import { AccountCreateDrawer } from "../components/AccountCreateDrawer";
import { OnboardingApprovalWorkspace } from "./OnboardingApprovalPage";
import { openSupportCenterConversation } from "@/features/support/utils";
import {
  getProvisionedAccounts,
  updateProvisionedAccountRole,
  updateProvisionedAccountStatus,
  type AdminProvisionableAccountRoleCode,
  type ProvisionedAccountResponse,
  type ProvisionedAccountRoleCode,
  type ProvisionedAccountStatus,
} from "@/features/iam/api/provisionedAccountApi";
import { hasAnyPermission } from "@/shared/auth/permissions";

type RoleCode = ProvisionedAccountRoleCode;
type ProvisionableRoleCode = AdminProvisionableAccountRoleCode;
type AccountStatus = ProvisionedAccountStatus;

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

type AccountRoleChangeModalState = {
  account: ProvisionedAccount;
  roleCode: ProvisionableRoleCode;
} | null;

type AccountStatusChangeModalState = {
  account: ProvisionedAccount;
  reason: string;
  status: AccountStatus;
} | null;

type AccountPermissionModalState = ProvisionedAccount | null;
type AccountWorkspaceTab = "accounts" | "onboarding";

const roleOptions: Array<{ label: string; value: ProvisionableRoleCode | "all" }> = [
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

const editableStatusOptions: AccountStatus[] = ["ACTIVE", "LOCKED", "DISABLED"];

function isInternalRole(roleCode: RoleCode) {
  return roleCode !== "CUSTOMER";
}

function isAdminProvisionableRole(roleCode: RoleCode): roleCode is ProvisionableRoleCode {
  return roleCode === "SYSTEM_ADMIN" || roleCode === "PARKING_MANAGER" || roleCode === "EMPLOYEE" || roleCode === "CUSTOMER";
}

function getProvisionableRoleFallback(roleCode: RoleCode): ProvisionableRoleCode {
  if (isAdminProvisionableRole(roleCode)) return roleCode;
  return isInternalRole(roleCode) ? "EMPLOYEE" : "CUSTOMER";
}

function canTransitionRole(currentRole: RoleCode, nextRole: ProvisionableRoleCode) {
  return isInternalRole(currentRole) === isInternalRole(nextRole);
}

function canTransitionStatus(currentStatus: AccountStatus, nextStatus: AccountStatus) {
  return !(currentStatus === "DISABLED" && nextStatus === "LOCKED");
}

function replaceAccount(accounts: ProvisionedAccount[], account: ProvisionedAccount) {
  return accounts.map((currentAccount) => (currentAccount.accountId === account.accountId ? account : currentAccount));
}

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
  canMessage,
  messageDisabledReason,
  onMessage,
  onSelect,
  selected,
}: {
  account: ProvisionedAccount;
  canMessage: boolean;
  messageDisabledReason?: string;
  onMessage: () => void;
  onSelect: () => void;
  selected: boolean;
}) {
  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.target !== event.currentTarget) return;
    if (event.key !== "Enter" && event.key !== " ") return;
    event.preventDefault();
    onSelect();
  }

  return (
    <div
      role="button"
      tabIndex={0}
      className={cn(
        "tw-grid tw-w-full tw-grid-cols-[minmax(210px,1.35fr)_150px_110px_90px_96px] tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-py-3 tw-text-left tw-transition last:tw-border-b-0 max-[1180px]:tw-grid-cols-[minmax(220px,1fr)_110px_90px]",
        selected ? "tw-bg-brand-50 tw-shadow-[inset_3px_0_0_#2563EB]" : "hover:tw-bg-vm-slate-25",
      )}
      onClick={onSelect}
      onKeyDown={handleKeyDown}
    >
      <span className="tw-flex tw-min-w-0 tw-items-center tw-gap-3">
        <EntityAvatar initials={account.initials} size="md" tone={account.roleCode === "SYSTEM_ADMIN" ? "red" : account.roleCode === "PARKING_MANAGER" ? "blue" : "green"} />
        <span className="tw-min-w-0">
          <span className="tw-flex tw-min-w-0 tw-items-center tw-gap-2">
            <strong className="tw-min-w-0 tw-truncate tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-900">{account.username}</strong>
            {canMessage ? (
              <button
                type="button"
                className="tw-inline-flex tw-h-7 tw-w-7 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-border tw-border-solid tw-border-teal-100 tw-bg-teal-50 tw-text-[0.78rem] tw-text-teal-600 tw-transition hover:tw-border-teal-200 hover:tw-bg-teal-100 focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus"
                title="Nhắn tin"
                aria-label={`Nhắn tin với ${account.username}`}
                onClick={(event) => {
                  event.stopPropagation();
                  onMessage();
                }}
              >
                <i className="far fa-comment-dots" />
              </button>
            ) : (
              <span
                className="tw-inline-flex tw-h-7 tw-w-7 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-text-[0.78rem] tw-text-vm-slate-400"
                title={messageDisabledReason ?? "Tài khoản hiện tại chưa có quyền mở trung tâm chat"}
                aria-label="Chưa có quyền nhắn tin"
              >
                <i className="far fa-comment-dots" />
              </span>
            )}
          </span>
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
    </div>
  );
}

function toInitials(value: string) {
  const parts = value
    .trim()
    .split(/[\s._-]+/)
    .filter(Boolean)
    .slice(0, 2);
  return (parts.map((part) => part[0]?.toUpperCase()).join("") || "TK").slice(0, 2);
}

function mapProvisionedAccount(response: ProvisionedAccountResponse): ProvisionedAccount {
  return {
    accountId: response.account.accountId,
    createdAt: response.account.createdAt,
    email: response.account.email,
    fullName: response.account.username,
    initials: toInitials(response.account.username),
    keycloakUserId: response.account.keycloakUserId,
    permissionCodes: response.role.permissionCodes ?? [],
    permissionCount: response.role.permissionCodes?.length ?? 0,
    roleCode: response.role.roleCode,
    roleName: response.role.roleName,
    status: response.account.accountStatus,
    updatedAt: response.account.updatedAt,
    username: response.account.username,
  };
}

function AccountRoleChangeModal({
  canUpdateAccount,
  errorMessage,
  isSaving,
  modal,
  onClose,
  onRoleChange,
  onSubmit,
}: {
  canUpdateAccount: boolean;
  errorMessage: string;
  isSaving: boolean;
  modal: AccountRoleChangeModalState;
  onClose: () => void;
  onRoleChange: (roleCode: ProvisionableRoleCode) => void;
  onSubmit: () => void;
}) {
  if (!modal) return null;

  const availableOptions = roleOptions
    .filter((option): option is { label: string; value: ProvisionableRoleCode } => option.value !== "all")
    .map((option) => ({
      ...option,
      disabled: !canTransitionRole(modal.account.roleCode, option.value),
      reason: !canTransitionRole(modal.account.roleCode, option.value) ? "Không hỗ trợ đổi giữa nhóm internal và customer." : "",
    }));
  const selectedOption = availableOptions.find((option) => option.value === modal.roleCode);
  const cannotSubmit =
    isSaving ||
    modal.roleCode === modal.account.roleCode ||
    !selectedOption ||
    selectedOption.disabled ||
    !canUpdateAccount;

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[2300] tw-grid tw-place-items-center tw-bg-slate-950/45 tw-p-4" role="dialog" aria-modal="true" aria-labelledby="account-role-modal-title">
      <div className="tw-w-full tw-max-w-[520px] tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-[0_28px_80px_rgba(15,23,42,0.24)]">
        <header className="tw-flex tw-items-start tw-justify-between tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-5 tw-py-4">
          <div>
            <h3 id="account-role-modal-title" className="tw-m-0 tw-text-[1.08rem] tw-font-black tw-text-vm-slate-900">Đổi vai trò</h3>
            <p className="tw-m-0 tw-mt-1 tw-text-[0.82rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-500">{modal.account.username}</p>
          </div>
          <button className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-slate-600 hover:tw-bg-vm-slate-100" disabled={isSaving} onClick={onClose} type="button" aria-label="Đóng">
            <i className="fas fa-times" />
          </button>
        </header>

        <div className="tw-grid tw-gap-4 tw-px-5 tw-py-4">
          <div className="tw-grid tw-grid-cols-2 tw-gap-3">
            <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3">
              <span className="tw-text-[0.72rem] tw-font-black tw-uppercase tw-text-vm-slate-500">Hiện tại</span>
              <Badge tone={roleBadgeTone(modal.account.roleCode)} className="tw-mt-2 tw-w-fit tw-rounded-full tw-px-3">{modal.account.roleCode}</Badge>
            </div>
            <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50 tw-p-3">
              <span className="tw-text-[0.72rem] tw-font-black tw-uppercase tw-text-vm-slate-500">Role mới</span>
              <Badge tone={roleBadgeTone(modal.roleCode)} className="tw-mt-2 tw-w-fit tw-rounded-full tw-px-3">{modal.roleCode}</Badge>
            </div>
          </div>

          <div>
            <label className="tw-mb-1.5 tw-block tw-text-[0.78rem] tw-font-black tw-uppercase tw-text-vm-slate-700">Chọn vai trò</label>
            <SelectMenu
              ariaLabel="Vai trò mới"
              clearValue={modal.account.roleCode}
              disabled={isSaving}
              options={availableOptions.map((option) => ({ label: option.label, value: option.value }))}
              value={modal.roleCode}
              onChange={(value) => onRoleChange(value as ProvisionableRoleCode)}
            />
            {selectedOption?.disabled ? <p className="tw-m-0 tw-mt-2 tw-text-[0.78rem] tw-font-semibold tw-text-vm-danger">{selectedOption.reason}</p> : null}
          </div>

          <InfoBanner
            tone="info"
            title="Quy định thay đổi vai trò"
            description="Chỉ có thể đổi vai trò trong phạm vi được phân quyền. Không hỗ trợ đổi qua lại giữa tài khoản nội bộ và khách hàng."
            icon={<i className="fas fa-shield-alt" />}
          />
          {errorMessage ? <InfoBanner tone="warning" title="Không thể đổi vai trò" description={errorMessage} icon={<i className="fas fa-exclamation-circle" />} /> : null}
        </div>

        <footer className="tw-flex tw-justify-end tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-px-5 tw-py-4">
          <Button variant="secondary" disabled={isSaving} onClick={onClose}>Hủy</Button>
          <Button variant="primary" disabled={cannotSubmit} loading={isSaving} onClick={onSubmit}>
            {!isSaving ? <i className="fas fa-user-shield" /> : null}
            {isSaving ? "Đang lưu..." : "Lưu vai trò"}
          </Button>
        </footer>
      </div>
    </div>
  );
}

function AccountStatusChangeModal({
  errorMessage,
  isSaving,
  modal,
  onClose,
  onReasonChange,
  onStatusChange,
  onSubmit,
}: {
  errorMessage: string;
  isSaving: boolean;
  modal: AccountStatusChangeModalState;
  onClose: () => void;
  onReasonChange: (reason: string) => void;
  onStatusChange: (status: AccountStatus) => void;
  onSubmit: () => void;
}) {
  if (!modal) return null;

  const invalidTransition = !canTransitionStatus(modal.account.status, modal.status);
  const cannotSubmit = isSaving || modal.status === modal.account.status || invalidTransition;

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[2300] tw-grid tw-place-items-center tw-bg-slate-950/45 tw-p-4" role="dialog" aria-modal="true" aria-labelledby="account-status-modal-title">
      <div className="tw-w-full tw-max-w-[540px] tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-[0_28px_80px_rgba(15,23,42,0.24)]">
        <header className="tw-flex tw-items-start tw-justify-between tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-5 tw-py-4">
          <div>
            <h3 id="account-status-modal-title" className="tw-m-0 tw-text-[1.08rem] tw-font-black tw-text-vm-slate-900">Đổi trạng thái</h3>
            <p className="tw-m-0 tw-mt-1 tw-text-[0.82rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-500">{modal.account.username}</p>
          </div>
          <button className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-slate-600 hover:tw-bg-vm-slate-100" disabled={isSaving} onClick={onClose} type="button" aria-label="Đóng">
            <i className="fas fa-times" />
          </button>
        </header>

        <div className="tw-grid tw-gap-4 tw-px-5 tw-py-4">
          <div className="tw-flex tw-flex-wrap tw-gap-2">
            {editableStatusOptions.map((status) => {
              const disabled = !canTransitionStatus(modal.account.status, status);
              const selected = modal.status === status;

              return (
                <button
                  className={cn(
                    "tw-inline-flex tw-min-h-10 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-px-3 tw-text-[0.82rem] tw-font-extrabold tw-transition disabled:tw-cursor-not-allowed disabled:tw-opacity-55",
                    selected ? "tw-border-vm-primary tw-bg-brand-50 tw-text-vm-primary" : "tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-border-brand-100",
                  )}
                  disabled={disabled || isSaving}
                  key={status}
                  onClick={() => onStatusChange(status)}
                  type="button"
                >
                  {status}
                </button>
              );
            })}
          </div>

          {invalidTransition ? (
            <InfoBanner tone="warning" title="Chuyển trạng thái không hợp lệ" description="Không cho chuyển tài khoản từ DISABLED sang LOCKED." icon={<i className="fas fa-ban" />} />
          ) : null}

          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-[0.78rem] tw-font-black tw-uppercase tw-text-vm-slate-700">Lý do</span>
            <textarea
              className="tw-min-h-[92px] tw-resize-none tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-2 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_4px_rgba(37,99,235,0.08)]"
              disabled={isSaving}
              maxLength={255}
              onChange={(event) => onReasonChange(event.target.value)}
              placeholder="Nhập lý do cập nhật trạng thái..."
              value={modal.reason}
            />
          </label>

          <InfoBanner tone="info" title="PENDING không hỗ trợ cập nhật thủ công" description="Tài khoản cấp sẵn chỉ có thể cập nhật sang ACTIVE, LOCKED hoặc DISABLED." icon={<i className="fas fa-info-circle" />} />
          {errorMessage ? <InfoBanner tone="warning" title="Không thể đổi trạng thái" description={errorMessage} icon={<i className="fas fa-exclamation-circle" />} /> : null}
        </div>

        <footer className="tw-flex tw-justify-end tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-px-5 tw-py-4">
          <Button variant="secondary" disabled={isSaving} onClick={onClose}>Hủy</Button>
          <Button variant="primary" disabled={cannotSubmit} loading={isSaving} onClick={onSubmit}>
            {!isSaving ? <i className="fas fa-toggle-on" /> : null}
            {isSaving ? "Đang lưu..." : "Lưu trạng thái"}
          </Button>
        </footer>
      </div>
    </div>
  );
}

function AccountPermissionModal({
  account,
  onClose,
}: {
  account: AccountPermissionModalState;
  onClose: () => void;
}) {
  if (!account) return null;

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[2300] tw-grid tw-place-items-center tw-bg-slate-950/45 tw-p-4" role="dialog" aria-modal="true" aria-labelledby="account-permission-modal-title">
      <div className="tw-flex tw-max-h-[min(680px,calc(100vh-32px))] tw-w-full tw-max-w-[720px] tw-flex-col tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-[0_28px_80px_rgba(15,23,42,0.24)]">
        <header className="tw-flex tw-items-start tw-justify-between tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-5 tw-py-4">
          <div className="tw-min-w-0">
            <h3 id="account-permission-modal-title" className="tw-m-0 tw-text-[1.08rem] tw-font-black tw-text-vm-slate-900">Quyền của tài khoản</h3>
            <p className="tw-m-0 tw-mt-1 tw-truncate tw-text-[0.82rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-500">{account.username}</p>
          </div>
          <button className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-slate-600 hover:tw-bg-vm-slate-100" onClick={onClose} type="button" aria-label="Đóng">
            <i className="fas fa-times" />
          </button>
        </header>

        <div className="tw-grid tw-gap-4 tw-overflow-y-auto tw-px-5 tw-py-4">
          <div className="tw-grid tw-grid-cols-3 tw-gap-3 max-[640px]:tw-grid-cols-1">
            <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3">
              <span className="tw-text-[0.72rem] tw-font-black tw-uppercase tw-text-vm-slate-500">Vai trò</span>
              <Badge tone={roleBadgeTone(account.roleCode)} className="tw-mt-2 tw-w-fit tw-rounded-full tw-px-3">{account.roleCode}</Badge>
            </div>
            <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3">
              <span className="tw-text-[0.72rem] tw-font-black tw-uppercase tw-text-vm-slate-500">Trạng thái</span>
              <Badge tone={statusBadgeTone(account.status)} className="tw-mt-2 tw-w-fit tw-rounded-full tw-px-3">{account.status}</Badge>
            </div>
            <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3">
              <span className="tw-text-[0.72rem] tw-font-black tw-uppercase tw-text-vm-slate-500">Tổng quyền</span>
              <strong className="tw-mt-2 tw-block tw-text-[1.35rem] tw-font-black tw-leading-none tw-text-vm-slate-900">{account.permissionCount}</strong>
            </div>
          </div>

          {account.permissionCodes.length > 0 ? (
            <div className="tw-grid tw-grid-cols-2 tw-gap-2.5 max-[640px]:tw-grid-cols-1">
              {account.permissionCodes.map((permission) => (
                <div key={permission} className="tw-flex tw-min-h-10 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-2 tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-800">
                  <i className="fas fa-check-circle tw-text-[0.82rem] tw-text-green-600" />
                  <span className="tw-min-w-0 tw-truncate">{permission}</span>
                </div>
              ))}
            </div>
          ) : (
            <InfoBanner tone="info" title="Chưa có quyền" description="Vai trò hiện tại chưa trả về danh sách quyền cho tài khoản này." icon={<i className="far fa-folder-open" />} />
          )}
        </div>

        <footer className="tw-flex tw-justify-end tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-px-5 tw-py-4">
          <Button variant="secondary" onClick={onClose}>Đóng</Button>
        </footer>
      </div>
    </div>
  );
}

export function AccountListPage() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [accountItems, setAccountItems] = useState<ProvisionedAccount[]>([]);
  const [selectedAccountId, setSelectedAccountId] = useState("");
  const [keyword, setKeyword] = useState("");
  const [selectedRole, setSelectedRole] = useState("all");
  const [selectedStatus, setSelectedStatus] = useState<AccountStatus | "all">("all");
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [roleModal, setRoleModal] = useState<AccountRoleChangeModalState>(null);
  const [statusModal, setStatusModal] = useState<AccountStatusChangeModalState>(null);
  const [permissionModal, setPermissionModal] = useState<AccountPermissionModalState>(null);
  const [isActionSaving, setIsActionSaving] = useState(false);
  const [actionErrorMessage, setActionErrorMessage] = useState("");
  const canReadProvisionedAccount = hasAnyPermission(user, ["ACCOUNT_READ_ALL"]);
  const canReadOnboardingApprovals =
    (user?.role === "SYSTEM_ADMIN" && hasAnyPermission(user, ["ACCOUNT_READ_ALL", "EMPLOYEE_READ_ALL"])) ||
    (user?.role === "PARKING_MANAGER" && hasAnyPermission(user, ["ACCOUNT_READ_ALL", "EMPLOYEE_READ_ALL", "CUSTOMER_READ_ALL"]));
  const canCreateProvisionedAccount = hasAnyPermission(user, ["ACCOUNT_CREATE_ALL"]);
  const canUpdateProvisionedAccount = hasAnyPermission(user, ["ACCOUNT_UPDATE_ALL"]);
  const canOpenSupportCenter = hasAnyPermission(user, ["CHAT_CONVERSATION_READ_OWN", "CHAT_CONVERSATION_READ_ALL"]);
  const canCreateChatConversation = hasAnyPermission(user, ["CHAT_CONVERSATION_CREATE_OWN"]);
  const requestedWorkspaceTab = searchParams.get("tab") === "onboarding" ? "onboarding" : "accounts";
  const activeWorkspaceTab: AccountWorkspaceTab =
    requestedWorkspaceTab === "onboarding" && canReadOnboardingApprovals ? "onboarding" : canReadProvisionedAccount ? "accounts" : "onboarding";

  function setWorkspaceTab(tab: AccountWorkspaceTab) {
    const nextSearchParams = new URLSearchParams(searchParams);
    if (tab === "onboarding") {
      nextSearchParams.set("tab", "onboarding");
    } else {
      nextSearchParams.delete("tab");
    }
    setSearchParams(nextSearchParams, { replace: true });
  }

  async function loadAccounts() {
    if (!canReadProvisionedAccount) return;

    setIsLoading(true);
    setErrorMessage("");

    try {
      const response = await getProvisionedAccounts({
        accountStatus: selectedStatus === "all" ? undefined : selectedStatus,
        keyword: keyword.trim() || undefined,
        roleCode: selectedRole === "all" ? undefined : (selectedRole as ProvisionableRoleCode),
      });
      const mappedAccounts = response.data.map(mapProvisionedAccount);
      setAccountItems(mappedAccounts);
      setSelectedAccountId((currentValue) => currentValue || mappedAccounts[0]?.accountId || "");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Không thể tải danh sách tài khoản.");
      setAccountItems([]);
      setSelectedAccountId("");
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    if (activeWorkspaceTab !== "accounts") return;
    void loadAccounts();
  }, [activeWorkspaceTab, canReadProvisionedAccount, selectedRole, selectedStatus]);

  const filteredAccounts = useMemo(() => {
    return accountItems.filter((account) => {
      const matchesRole = selectedRole === "all" || account.roleCode === selectedRole;
      const matchesStatus = selectedStatus === "all" || account.status === selectedStatus;
      const normalizedKeyword = keyword.trim().toLowerCase();
      const matchesKeyword =
        !normalizedKeyword ||
        account.username.toLowerCase().includes(normalizedKeyword) ||
        account.email.toLowerCase().includes(normalizedKeyword);
      return matchesRole && matchesStatus && matchesKeyword;
    });
  }, [accountItems, keyword, selectedRole, selectedStatus]);

  const selectedAccount = accountItems.find((account) => account.accountId === selectedAccountId) ?? accountItems[0];
  const metricValues = useMemo(() => {
    return {
      active: accountItems.filter((account) => account.status === "ACTIVE").length,
      locked: accountItems.filter((account) => account.status === "LOCKED").length,
      pending: accountItems.filter((account) => account.status === "PENDING").length,
      total: accountItems.length,
    };
  }, [accountItems]);

  const emptyAccount: ProvisionedAccount = {
    accountId: "-",
    createdAt: "-",
    email: "-",
    fullName: "-",
    initials: "TK",
    keycloakUserId: "-",
    permissionCodes: [],
    permissionCount: 0,
    roleCode: "CUSTOMER",
    roleName: "-",
    status: "PENDING",
    updatedAt: "-",
    username: "Chưa có dữ liệu",
  };
  const activeAccount = selectedAccount ?? emptyAccount;
  const canManageActiveAccount = canUpdateProvisionedAccount && activeAccount.accountId !== "-";

  function openRoleModal(account: ProvisionedAccount) {
    if (!canUpdateProvisionedAccount) return;
    setActionErrorMessage("");
    setRoleModal({ account, roleCode: getProvisionableRoleFallback(account.roleCode) });
  }

  function openStatusModal(account: ProvisionedAccount) {
    if (!canUpdateProvisionedAccount) return;
    const defaultStatus = account.status === "ACTIVE" ? "LOCKED" : "ACTIVE";
    setActionErrorMessage("");
    setStatusModal({
      account,
      reason: "",
      status: canTransitionStatus(account.status, defaultStatus) ? defaultStatus : "ACTIVE",
    });
  }

  function openAccountConversation(account: ProvisionedAccount) {
    if (getAccountChatDisabledReason(account)) return;

    openSupportCenterConversation({
      mode: "internal-direct",
      participantId: account.accountId,
      participantName: account.username,
      participantType: "employee",
    });
  }

  function getAccountChatDisabledReason(account: ProvisionedAccount) {
    if (account.accountId === "-") return "Chưa có tài khoản để mở chat.";
    if (account.accountId === user?.id) return "Không thể tạo hội thoại trực tiếp với chính mình.";
    if (account.status !== "ACTIVE") return "Chỉ có thể nhắn tin với tài khoản đang ACTIVE.";
    if (!isInternalRole(account.roleCode)) return "Từ màn Tài khoản hiện chỉ tạo chat trực tiếp cho tài khoản nội bộ.";
    if (!canOpenSupportCenter) return "Cần quyền CHAT_CONVERSATION_READ_OWN hoặc CHAT_CONVERSATION_READ_ALL.";
    if (!canCreateChatConversation) return "Cần quyền CHAT_CONVERSATION_CREATE_OWN để tạo hội thoại nội bộ.";
    return "";
  }

  async function submitRoleChange() {
    if (!roleModal) return;

    setIsActionSaving(true);
    setActionErrorMessage("");

    try {
      const response = await updateProvisionedAccountRole(roleModal.account.accountId, { roleCode: roleModal.roleCode });
      const mappedAccount = mapProvisionedAccount(response.data);
      setAccountItems((currentAccounts) => replaceAccount(currentAccounts, mappedAccount));
      setSelectedAccountId(mappedAccount.accountId);
      setRoleModal(null);
    } catch (error) {
      setActionErrorMessage(error instanceof Error ? error.message : "Không thể đổi vai trò tài khoản.");
    } finally {
      setIsActionSaving(false);
    }
  }

  async function submitStatusChange() {
    if (!statusModal) return;

    setIsActionSaving(true);
    setActionErrorMessage("");

    try {
      const response = await updateProvisionedAccountStatus(statusModal.account.accountId, {
        reason: statusModal.reason.trim() || undefined,
        status: statusModal.status,
      });
      const mappedAccount = mapProvisionedAccount(response.data);
      setAccountItems((currentAccounts) => replaceAccount(currentAccounts, mappedAccount));
      setSelectedAccountId(mappedAccount.accountId);
      setStatusModal(null);
    } catch (error) {
      setActionErrorMessage(error instanceof Error ? error.message : "Không thể đổi trạng thái tài khoản.");
    } finally {
      setIsActionSaving(false);
    }
  }

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
            {activeWorkspaceTab === "accounts" && canReadProvisionedAccount ? (
              <div className="tw-flex tw-flex-shrink-0 tw-items-center tw-gap-3">
                <Button size="lg" variant="primary" disabled={!canCreateProvisionedAccount} onClick={() => setDrawerOpen(true)}>
                  <i className="fas fa-plus" />
                  Tạo tài khoản
                </Button>
                <Button size="lg" variant="secondary">
                  <i className="fas fa-download" />
                  Xuất dữ liệu
                  <i className="fas fa-chevron-down tw-text-[0.72rem]" />
                </Button>
              </div>
            ) : null}
          </div>

          <div className="tw-mb-5 tw-flex tw-flex-wrap tw-gap-2">
            {canReadProvisionedAccount ? (
              <button
                className={cn(
                  "tw-inline-flex tw-min-h-10 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-px-3 tw-text-[0.84rem] tw-font-extrabold tw-transition",
                  activeWorkspaceTab === "accounts"
                    ? "tw-border-vm-primary tw-bg-brand-50 tw-text-vm-primary"
                    : "tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-border-brand-100",
                )}
                onClick={() => setWorkspaceTab("accounts")}
                type="button"
              >
                <i className="fas fa-users-cog" />
                Danh sách tài khoản
              </button>
            ) : null}
            {canReadOnboardingApprovals ? (
              <button
                className={cn(
                  "tw-inline-flex tw-min-h-10 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-px-3 tw-text-[0.84rem] tw-font-extrabold tw-transition",
                  activeWorkspaceTab === "onboarding"
                    ? "tw-border-vm-primary tw-bg-brand-50 tw-text-vm-primary"
                    : "tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-border-brand-100",
                )}
                onClick={() => setWorkspaceTab("onboarding")}
                type="button"
              >
                <i className="fas fa-user-check" />
                Duyệt onboarding
              </button>
            ) : null}
          </div>

          {activeWorkspaceTab === "accounts" ? (
            <>
          <div className="tw-grid tw-grid-cols-4 tw-gap-4 max-[1180px]:tw-grid-cols-2">
            <AccountMetric icon="fas fa-users-cog" iconClassName="tw-bg-brand-100 tw-text-vm-primary" label="Tổng tài khoản" value={String(metricValues.total)} />
            <AccountMetric icon="fas fa-user-check" iconClassName="tw-bg-green-50 tw-text-green-600" label="Đang hoạt động" value={String(metricValues.active)} />
            <AccountMetric icon="far fa-clock" iconClassName="tw-bg-amber-50 tw-text-amber-500" label="Chờ kích hoạt" value={String(metricValues.pending)} />
            <AccountMetric icon="fas fa-lock" iconClassName="tw-bg-red-50 tw-text-vm-danger" label="Bị khóa" value={String(metricValues.locked)} />
          </div>

          <Card className="tw-mt-5 tw-p-4">
            <div className="tw-flex tw-items-center tw-justify-between tw-gap-4 max-[1180px]:tw-flex-col max-[1180px]:tw-items-stretch">
              <div className="tw-min-w-0">
                <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Bộ lọc tài khoản</h2>
                <p className="tw-m-0 tw-mt-1 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Lọc theo username, vai trò và trạng thái tài khoản cấp sẵn.</p>
              </div>
              <div className="tw-grid tw-flex-1 tw-grid-cols-[minmax(260px,1.5fr)_190px_190px_auto] tw-items-center tw-gap-3 max-[1180px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
                <SearchInput
                  aria-label="Tìm tài khoản"
                  containerClassName="tw-h-[42px]"
                  onChange={setKeyword}
                  placeholder="Tìm username, email..."
                  value={keyword}
                />
                <SelectMenu ariaLabel="Vai trò tài khoản" value={selectedRole} options={roleOptions} onChange={setSelectedRole} />
                <SelectMenu ariaLabel="Trạng thái tài khoản" value={selectedStatus} options={statusOptions} onChange={(value) => setSelectedStatus(value as AccountStatus | "all")} />
                <Button
                  className="tw-h-[42px]"
                  variant="secondary"
                  onClick={() => {
                    setKeyword("");
                    setSelectedRole("all");
                    setSelectedStatus("all");
                  }}
                >
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
                {isLoading ? (
                  <div className="tw-p-4">
                    <InfoBanner tone="info" title="Đang tải dữ liệu" description="Hệ thống đang lấy danh sách tài khoản." icon={<i className="fas fa-spinner fa-spin" />} />
                  </div>
                ) : null}
                {errorMessage ? (
                  <div className="tw-p-4">
                    <InfoBanner tone="warning" title="Không thể tải tài khoản" description={errorMessage} icon={<i className="fas fa-exclamation-circle" />} />
                  </div>
                ) : null}
                {!isLoading && !errorMessage && filteredAccounts.length === 0 ? (
                  <div className="tw-p-4">
                    <InfoBanner tone="info" title="Chưa có dữ liệu phù hợp" description="Thử thay đổi bộ lọc hoặc tạo tài khoản cấp sẵn mới." icon={<i className="far fa-folder-open" />} />
                  </div>
                ) : null}
                {filteredAccounts.map((account) => {
                  const messageDisabledReason = getAccountChatDisabledReason(account);

                  return (
                    <AccountRow
                      key={account.accountId}
                      account={account}
                      canMessage={!messageDisabledReason}
                      messageDisabledReason={messageDisabledReason || undefined}
                      selected={account.accountId === activeAccount.accountId}
                      onMessage={() => openAccountConversation(account)}
                      onSelect={() => setSelectedAccountId(account.accountId)}
                    />
                  );
                })}
              </div>
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-4 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-py-3">
                <p className="tw-m-0 tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-500">Hiển thị {filteredAccounts.length} tài khoản</p>
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
                <Badge tone={statusBadgeTone(activeAccount.status)} className="tw-rounded-full tw-px-3">{activeAccount.status}</Badge>
              </div>

              <div className="tw-mt-5 tw-rounded-vm-lg tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50/70 tw-p-4">
                <div className="tw-flex tw-items-center tw-gap-4">
                  <EntityAvatar initials={activeAccount.initials} size="xl" tone={activeAccount.roleCode === "SYSTEM_ADMIN" ? "red" : activeAccount.roleCode === "PARKING_MANAGER" ? "blue" : "green"} />
                  <div className="tw-min-w-0">
                    <h3 className="tw-m-0 tw-truncate tw-text-[1.18rem] tw-font-extrabold tw-text-vm-slate-900">{activeAccount.username}</h3>
                    <p className="tw-m-0 tw-mt-1 tw-truncate tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">{activeAccount.email}</p>
                    <div className="tw-mt-3 tw-flex tw-flex-wrap tw-gap-2">
                      <Badge tone={roleBadgeTone(activeAccount.roleCode)} className="tw-rounded-full tw-px-3">{activeAccount.roleCode}</Badge>
                      <Badge tone="neutral" className="tw-rounded-full tw-px-3">{activeAccount.roleName}</Badge>
                    </div>
                  </div>
                </div>
              </div>

              <div className="tw-mt-4 tw-grid tw-gap-3 tw-text-[0.86rem]">
                {[
                  ["Keycloak ID", activeAccount.keycloakUserId],
                  ["Ngày tạo", activeAccount.createdAt],
                  ["Cập nhật", activeAccount.updatedAt],
                  ["Account ID", activeAccount.accountId],
                ].map(([label, value]) => (
                  <div key={label} className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-3 last:tw-border-b-0">
                    <span className="tw-font-bold tw-text-vm-slate-500">{label}</span>
                    <strong className="tw-min-w-0 tw-truncate tw-text-right tw-font-extrabold tw-text-vm-slate-900">{value}</strong>
                  </div>
                ))}
              </div>

              <div className="tw-mt-auto tw-grid tw-grid-cols-1 tw-gap-3 tw-pt-5">
                <Button variant="secondary" disabled={!canManageActiveAccount} onClick={() => openRoleModal(activeAccount)}>
                  <i className="fas fa-user-shield" />
                  Đổi vai trò
                </Button>
                <Button variant="secondary" disabled={!canManageActiveAccount} onClick={() => openStatusModal(activeAccount)}>
                  <i className="fas fa-toggle-on" />
                  Đổi trạng thái
                </Button>
                <Button variant="primary" disabled={activeAccount.accountId === "-"} onClick={() => setPermissionModal(activeAccount)}>
                  <i className="fas fa-key" />
                  Xem quyền
                </Button>
              </div>
            </Card>
          </div>

          <InfoBanner
            className="tw-mt-4"
            tone="info"
            title="Quản lý tài khoản theo quyền"
            description="Các thao tác hiển thị theo quyền của tài khoản hiện tại."
            icon={<i className="fas fa-info-circle" />}
          />
            </>
          ) : (
            <OnboardingApprovalWorkspace embedded />
          )}
        </section>
      </div>

      <AccountCreateDrawer
        isOpen={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onCreated={(account) => {
          const mappedAccount = mapProvisionedAccount(account);
          setAccountItems((currentValue) => [mappedAccount, ...currentValue]);
          setSelectedAccountId(mappedAccount.accountId);
        }}
      />
      <AccountRoleChangeModal
        canUpdateAccount={canUpdateProvisionedAccount}
        errorMessage={actionErrorMessage}
        isSaving={isActionSaving}
        modal={roleModal}
        onClose={() => {
          if (!isActionSaving) setRoleModal(null);
        }}
        onRoleChange={(roleCode) => setRoleModal((currentValue) => (currentValue ? { ...currentValue, roleCode } : currentValue))}
        onSubmit={() => void submitRoleChange()}
      />
      <AccountStatusChangeModal
        errorMessage={actionErrorMessage}
        isSaving={isActionSaving}
        modal={statusModal}
        onClose={() => {
          if (!isActionSaving) setStatusModal(null);
        }}
        onReasonChange={(reason) => setStatusModal((currentValue) => (currentValue ? { ...currentValue, reason } : currentValue))}
        onStatusChange={(status) => setStatusModal((currentValue) => (currentValue ? { ...currentValue, status } : currentValue))}
        onSubmit={() => void submitStatusChange()}
      />
      <AccountPermissionModal account={permissionModal} onClose={() => setPermissionModal(null)} />
    </>
  );
}
