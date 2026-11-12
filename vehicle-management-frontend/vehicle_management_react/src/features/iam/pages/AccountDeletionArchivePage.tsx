import { useState } from "react";

import { Badge, Button, Card, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import {
  getAccountDeletionAudit,
  getClosedOrganization,
  getClosedOrganizations,
  getDeletedAccount,
  getDeletedAccounts,
  lookupDeletedIdentity,
  type AccountDeletionAudit,
  type ClosedOrganizationArchive,
  type DeletedAccountArchive,
  type DeletedIdentityMatch,
  type DeletedIdentityType,
} from "@/features/iam/api/accountDeletionArchiveApi";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { formatInApplicationTime } from "@/shared/time/applicationTime";

const identityTypes: Array<{ label: string; value: DeletedIdentityType }> = [
  { label: "Email", value: "EMAIL" },
  { label: "Tên đăng nhập", value: "USERNAME" },
  { label: "Số điện thoại", value: "PHONE_NUMBER" },
  { label: "CCCD/CMND", value: "IDENTIFY_CARD" },
  { label: "Mã khách hàng", value: "CUSTOMER_CODE" },
  { label: "Mã nhân viên", value: "EMPLOYEE_CODE" },
  { label: "Provider subject", value: "PROVIDER_SUBJECT" },
];

function formatTime(value?: string | null) {
  return value ? formatInApplicationTime(value, "vi-VN", { dateStyle: "short", timeStyle: "short" }) : "—";
}

export function AccountDeletionArchivePage() {
  const { user } = useAuth();
  const toast = useToast();
  const [reason, setReason] = useState("");
  const [accountId, setAccountId] = useState("");
  const [organizationId, setOrganizationId] = useState("");
  const [identityType, setIdentityType] = useState<DeletedIdentityType>("EMAIL");
  const [identityValue, setIdentityValue] = useState("");
  const [accounts, setAccounts] = useState<DeletedAccountArchive[]>([]);
  const [organizations, setOrganizations] = useState<ClosedOrganizationArchive[]>([]);
  const [audit, setAudit] = useState<AccountDeletionAudit | null>(null);
  const [identityMatches, setIdentityMatches] = useState<DeletedIdentityMatch[]>([]);
  const [loading, setLoading] = useState<string | null>(null);

  const canReadAccounts = hasAnyPermission(user, ["DELETED_ACCOUNT_READ_ALL"]);
  const canReadOrganizations = hasAnyPermission(user, ["CLOSED_ORGANIZATION_READ_ALL"]);
  const canReadAudit = hasAnyPermission(user, ["ACCOUNT_DELETION_AUDIT_READ_ALL"]);
  const canLookupIdentity = hasAnyPermission(user, ["DELETED_IDENTITY_LOOKUP_ALL"]);

  const requireReason = () => {
    if (reason.trim()) return true;
    toast.error("Vui lòng nhập lý do nghiệp vụ trước khi tra cứu.", "Thiếu lý do tra cứu");
    return false;
  };

  const run = async (key: string, action: () => Promise<void>) => {
    if (!requireReason()) return;
    setLoading(key);
    try {
      await action();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể tải dữ liệu lưu trữ.", "Tra cứu thất bại");
    } finally {
      setLoading(null);
    }
  };

  const loadAudit = (requestId: string) => run("audit", async () => {
    setAudit(await getAccountDeletionAudit(requestId, reason));
  });

  return (
    <section className="content tw-pb-8 tw-pt-4">
      <div className="container-fluid tw-max-w-[1480px]">
        <Card className="tw-rounded-vm-lg tw-border tw-border-solid !tw-border-vm-slate-100 tw-p-5">
          <header>
            <p className="tw-m-0 tw-text-xs tw-font-black tw-uppercase tw-tracking-[.12em] tw-text-vm-primary">Archive có kiểm soát</p>
            <h1 className="tw-m-0 tw-mt-1 tw-text-vm-page-title tw-font-black tw-text-vm-slate-900">Dữ liệu xóa account & đóng tổ chức</h1>
            <p className="tw-mb-0 tw-mt-2 tw-text-sm tw-font-semibold tw-text-vm-slate-500">Mỗi lần đọc được kiểm tra permission và ghi audit. Màn hình không hiển thị PII gốc, credential hoặc tombstone marker.</p>
          </header>

          <label className="tw-mt-5 tw-block tw-text-sm tw-font-black tw-text-vm-slate-800">
            Lý do tra cứu <span className="tw-text-red-600">*</span>
            <textarea className="tw-mt-2 tw-min-h-20 tw-w-full tw-resize-y tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-200 tw-p-3 tw-font-medium" maxLength={500} placeholder="Ví dụ: Xử lý ticket hỗ trợ #..." value={reason} onChange={(event) => setReason(event.target.value)} />
          </label>

          <div className="tw-mt-5 tw-grid tw-grid-cols-2 tw-gap-4 max-[900px]:tw-grid-cols-1">
            {canReadAccounts ? <Card className="tw-p-4">
              <h2 className="tw-m-0 tw-text-lg tw-font-black">Account đã xóa</h2>
              <div className="tw-mt-3 tw-flex tw-flex-wrap tw-gap-2">
                <input className="tw-min-w-64 tw-flex-1 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-200 tw-px-3" placeholder="Account UUID (để trống để tải danh sách)" value={accountId} onChange={(event) => setAccountId(event.target.value)} />
                <Button loading={loading === "accounts"} type="button" onClick={() => void run("accounts", async () => {
                  const result = accountId.trim()
                    ? [await getDeletedAccount(accountId.trim(), reason)]
                    : await getDeletedAccounts(reason);
                  setAccounts(result);
                })}>Tra cứu</Button>
              </div>
              <div className="tw-mt-4 tw-grid tw-gap-2">
                {accounts.map((item) => <button key={item.requestId} className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3 tw-text-left" type="button" onClick={() => canReadAudit && void loadAudit(item.requestId)}>
                  <span className="tw-flex tw-items-center tw-justify-between tw-gap-2"><strong className="tw-break-all">{item.accountId}</strong><Badge tone="neutral">{item.accountStatus}</Badge></span>
                  <small className="tw-mt-1 tw-block tw-text-vm-slate-500">Hoàn tất: {formatTime(item.completedAt)} · Request: {item.requestId}</small>
                </button>)}
                {accounts.length === 0 ? <p className="tw-m-0 tw-text-sm tw-text-vm-slate-500">Chưa tải dữ liệu.</p> : null}
              </div>
            </Card> : null}

            {canReadOrganizations ? <Card className="tw-p-4">
              <h2 className="tw-m-0 tw-text-lg tw-font-black">Tổ chức đã đóng</h2>
              <div className="tw-mt-3 tw-flex tw-flex-wrap tw-gap-2">
                <input className="tw-min-w-64 tw-flex-1 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-200 tw-px-3" placeholder="Organization UUID (để trống để tải danh sách)" value={organizationId} onChange={(event) => setOrganizationId(event.target.value)} />
                <Button loading={loading === "organizations"} type="button" onClick={() => void run("organizations", async () => {
                  const result = organizationId.trim()
                    ? [await getClosedOrganization(organizationId.trim(), reason)]
                    : await getClosedOrganizations(reason);
                  setOrganizations(result);
                })}>Tra cứu</Button>
              </div>
              <div className="tw-mt-4 tw-grid tw-gap-2">
                {organizations.map((item) => <article key={item.organizationId} className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-3">
                  <span className="tw-flex tw-items-center tw-justify-between tw-gap-2"><strong>{item.name} ({item.code})</strong><Badge tone="neutral">{item.status}</Badge></span>
                  <small className="tw-mt-1 tw-block tw-text-vm-slate-500">Đóng lúc: {formatTime(item.closedAt)} · {item.organizationId}</small>
                </article>)}
                {organizations.length === 0 ? <p className="tw-m-0 tw-text-sm tw-text-vm-slate-500">Chưa tải dữ liệu.</p> : null}
              </div>
            </Card> : null}
          </div>

          {canLookupIdentity ? <Card className="tw-mt-4 tw-p-4">
            <h2 className="tw-m-0 tw-text-lg tw-font-black">Tra cứu định danh cũ bằng HMAC fingerprint</h2>
            <div className="tw-mt-3 tw-grid tw-grid-cols-[220px_1fr_auto] tw-gap-2 max-[800px]:tw-grid-cols-1">
              <select className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-200 tw-px-3" value={identityType} onChange={(event) => setIdentityType(event.target.value as DeletedIdentityType)}>{identityTypes.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select>
              <input className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-200 tw-px-3" placeholder="Giá trị cần đối chiếu (không được lưu thô)" value={identityValue} onChange={(event) => setIdentityValue(event.target.value)} />
              <Button disabled={!identityValue.trim()} loading={loading === "identity"} type="button" onClick={() => void run("identity", async () => setIdentityMatches(await lookupDeletedIdentity(identityType, identityValue, reason)))}>Đối chiếu</Button>
            </div>
            <div className="tw-mt-3 tw-grid tw-gap-2">{identityMatches.map((match) => <div key={`${match.requestId}-${match.identityType}`} className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-3 tw-text-sm"><strong>{match.accountId}</strong> · {match.identityType} · Request {match.requestId}</div>)}</div>
          </Card> : null}

          {canReadAudit && audit ? <Card className="tw-mt-4 tw-p-4">
            <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-2"><h2 className="tw-m-0 tw-text-lg tw-font-black">Timeline yêu cầu {audit.requestId}</h2><Badge tone="neutral">{audit.status}</Badge></div>
            <p className="tw-mb-0 tw-mt-2 tw-text-sm tw-text-vm-slate-600">Yêu cầu: {formatTime(audit.requestedAt)} · Dự kiến: {formatTime(audit.scheduledDeletionAt)} · Hoàn tất: {formatTime(audit.completedAt)} · Số lần xử lý: {audit.attemptCount}</p>
            {audit.lastErrorCode ? <p className="tw-mb-0 tw-mt-2 tw-font-bold tw-text-amber-700">Mã lỗi gần nhất: {audit.lastErrorCode}</p> : null}
            <div className="tw-mt-4 tw-overflow-x-auto"><table className="tw-w-full tw-border-collapse tw-text-sm"><thead><tr className="tw-bg-vm-slate-25 tw-text-left"><th className="tw-p-2">Loại cleanup</th><th className="tw-p-2">Trạng thái</th><th className="tw-p-2">Lịch chạy</th><th className="tw-p-2">Số lần thử</th></tr></thead><tbody>{audit.cleanupItems.map((item) => <tr key={item.cleanupItemId} className="tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100"><td className="tw-p-2">{item.resourceType}</td><td className="tw-p-2">{item.status}</td><td className="tw-p-2">{formatTime(item.scheduledAt)}</td><td className="tw-p-2">{item.attemptCount}</td></tr>)}</tbody></table></div>
          </Card> : null}
        </Card>
      </div>
    </section>
  );
}
