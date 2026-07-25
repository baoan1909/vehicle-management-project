import type { ReactNode } from "react";
import { useEffect, useMemo, useState } from "react";

import { Badge, Button, Card, InfoBanner, SearchInput, SelectMenu, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import {
  fetchOnboardingApprovals,
  reviewOnboardingApproval,
  type OnboardingApprovalKind,
  type OnboardingApprovalResponse,
  type OnboardingApprovalStatus,
} from "@/features/iam/api/onboardingApprovalApi";
import { cn } from "@/lib/cn";
import { hasAnyPermission } from "@/shared/auth/permissions";

type ApprovalTab = {
  icon: string;
  kind: OnboardingApprovalKind;
  label: string;
  readPermissions: string[];
  writePermissions: string[];
};

type ReviewModalState = {
  canReview: boolean;
  item: OnboardingApprovalResponse;
} | null;

type BadgeTone = "primary" | "success" | "warning" | "danger" | "neutral";

type OnboardingApprovalWorkspaceProps = {
  embedded?: boolean;
};

const approvalTabs: ApprovalTab[] = [
  {
    icon: "fas fa-user-shield",
    kind: "system-admin",
    label: "System Admin",
    readPermissions: ["ACCOUNT_READ_ALL"],
    writePermissions: ["ACCOUNT_UPDATE_ALL"],
  },
  {
    icon: "fas fa-id-badge",
    kind: "internal-employee",
    label: "Nhân sự nội bộ",
    readPermissions: ["ACCOUNT_READ_ALL", "EMPLOYEE_READ_ALL"],
    writePermissions: ["ACCOUNT_UPDATE_ALL", "EMPLOYEE_UPDATE_ALL"],
  },
  {
    icon: "fas fa-user-check",
    kind: "customer",
    label: "Khách hàng",
    readPermissions: ["CUSTOMER_READ_ALL"],
    writePermissions: ["CUSTOMER_UPDATE_ALL"],
  },
];

const statusOptions: Array<{ label: string; value: OnboardingApprovalStatus | "all" }> = [
  { label: "Tất cả trạng thái", value: "all" },
  { label: "Chờ duyệt", value: "PENDING" },
  { label: "Đã duyệt", value: "APPROVED" },
  { label: "Từ chối", value: "REJECTED" },
  { label: "Đã hủy", value: "CANCELLED" },
];

function approvalStatusTone(status?: string | null): BadgeTone {
  if (status === "APPROVED") return "success";
  if (status === "REJECTED" || status === "CANCELLED") return "danger";
  if (status === "PENDING") return "warning";
  return "neutral";
}

function accountStatusTone(status?: string | null): BadgeTone {
  if (status === "ACTIVE") return "success";
  if (status === "LOCKED" || status === "DISABLED") return "danger";
  if (status === "PENDING") return "warning";
  return "neutral";
}

function statusLabel(status?: string | null) {
  const labels: Record<string, string> = {
    ACTIVE: "Đã kích hoạt",
    APPROVED: "Đã duyệt",
    CANCELLED: "Đã hủy",
    DISABLED: "Vô hiệu",
    INACTIVE: "Chưa kích hoạt",
    LOCKED: "Đã khóa",
    PENDING: "Chờ duyệt",
    REJECTED: "Từ chối",
    SUSPENDED: "Tạm ngưng",
  };
  return status ? labels[status] ?? status : "-";
}

function getBusinessStatus(item: OnboardingApprovalResponse, kind: OnboardingApprovalKind) {
  if (kind === "customer") {
    return {
      label: `${statusLabel(item.customer?.customerStatus)} / ${statusLabel(item.customer?.customerApprovalStatus)}`,
      tone: approvalStatusTone(item.customer?.customerApprovalStatus),
    };
  }

  if (kind === "internal-employee") {
    return {
      label: `${statusLabel(item.employee?.employeeStatus)}${item.employee?.jobTitle ? ` - ${item.employee.jobTitle}` : ""}`,
      tone: accountStatusTone(item.employee?.employeeStatus),
    };
  }

  return {
    label: statusLabel(item.account?.accountStatus),
    tone: accountStatusTone(item.account?.accountStatus),
  };
}

function canOperatorSeeTab(tab: ApprovalTab, role?: string) {
  if (tab.kind === "customer") return role === "PARKING_MANAGER";
  if (tab.kind === "system-admin") return role === "SYSTEM_ADMIN";
  if (tab.kind === "internal-employee") return role === "SYSTEM_ADMIN" || role === "PARKING_MANAGER";
  return false;
}

function canReviewItem(item: OnboardingApprovalResponse, tab: ApprovalTab, role?: string, currentAccountId?: string) {
  if (item.request?.approvalRequestStatus !== "PENDING") return false;
  if (tab.kind === "system-admin" && item.account?.accountId && item.account.accountId === currentAccountId) return false;
  if (tab.kind === "customer") return role === "PARKING_MANAGER";
  if (tab.kind === "system-admin") return role === "SYSTEM_ADMIN";
  if (tab.kind === "internal-employee") {
    if (role === "SYSTEM_ADMIN") return item.account?.roleCode === "PARKING_MANAGER";
    if (role === "PARKING_MANAGER") return item.account?.roleCode === "EMPLOYEE";
  }
  return false;
}

function Metric({ icon, label, value }: { icon: string; label: string; value: number }) {
  return (
    <Card className="tw-min-h-[82px] tw-p-4">
      <div className="tw-flex tw-items-center tw-gap-3">
        <span className="tw-inline-flex tw-h-11 tw-w-11 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-brand-50 tw-text-vm-primary">
          <i className={icon} />
        </span>
        <div>
          <p className="tw-m-0 tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500">{label}</p>
          <strong className="tw-mt-1 tw-block tw-text-[1.55rem] tw-font-black tw-leading-none tw-text-vm-slate-900">{value}</strong>
        </div>
      </div>
    </Card>
  );
}

export function OnboardingApprovalPage() {
  return <OnboardingApprovalWorkspace />;
}

function ApprovalRow({
  canReview,
  item,
  kind,
  onOpenDetails,
}: {
  canReview: boolean;
  item: OnboardingApprovalResponse;
  kind: OnboardingApprovalKind;
  onOpenDetails: () => void;
}) {
  const businessStatus = getBusinessStatus(item, kind);
  const displayName = item.profile?.fullName || item.account?.username || "Chưa có tên";
  const requestStatus = item.request?.approvalRequestStatus ?? "PENDING";

  return (
    <article className="tw-grid tw-grid-cols-[minmax(250px,1.25fr)_150px_190px_150px_112px] tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-py-3 last:tw-border-b-0 max-[1180px]:tw-grid-cols-1">
      <div className="tw-min-w-0">
        <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2">
          <strong className="tw-min-w-0 tw-truncate tw-text-[0.92rem] tw-font-black tw-text-vm-slate-900">{displayName}</strong>
          <Badge tone="neutral" className="tw-rounded-full tw-px-2.5">{item.account?.roleCode ?? "-"}</Badge>
        </div>
        <p className="tw-m-0 tw-mt-1 tw-truncate tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">
          {item.account?.email ?? "-"} · {item.profile?.phoneNumber ?? "-"}
        </p>
      </div>
      <Badge tone={accountStatusTone(item.account?.accountStatus)} className="tw-w-fit tw-rounded-full tw-px-3">
        {statusLabel(item.account?.accountStatus)}
      </Badge>
      <Badge tone={businessStatus.tone} className="tw-w-fit tw-rounded-full tw-px-3">
        {businessStatus.label}
      </Badge>
      <Badge tone={approvalStatusTone(requestStatus)} className="tw-w-fit tw-rounded-full tw-px-3">
        {statusLabel(requestStatus)}
      </Badge>
      <div className="tw-justify-self-end max-[1180px]:tw-justify-self-start">
        <Button className="tw-h-8 tw-w-28 tw-whitespace-nowrap tw-gap-1.5 tw-px-2 tw-text-[0.78rem]" size="sm" variant={canReview ? "primary" : "secondary"} onClick={onOpenDetails}>
          <i className="fas fa-eye" />
          Xem chi tiết
        </Button>
      </div>
    </article>
  );
}

function displayValue(value?: string | number | null) {
  const normalizedValue = value == null ? "" : String(value).trim();
  return normalizedValue || "-";
}

function formatDateValue(value?: string | null) {
  if (!value) return "-";
  const datePart = value.slice(0, 10);
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(datePart);
  if (!match) return value;
  return `${match[3]}/${match[2]}/${match[1]}`;
}

function genderLabel(value?: string | null) {
  const normalizedValue = value?.trim().toUpperCase();
  if (!normalizedValue) return "-";
  if (normalizedValue === "MALE" || normalizedValue === "NAM") return "Nam";
  if (normalizedValue === "FEMALE" || normalizedValue === "NỮ" || normalizedValue === "NU") return "Nữ";
  return value ?? "-";
}

function DetailItem({ label, value, wide = false }: { label: string; value?: string | number | null; wide?: boolean }) {
  return (
    <div className={cn("tw-min-w-0 tw-rounded-vm-md tw-bg-vm-slate-25 tw-px-3 tw-py-2", wide ? "md:tw-col-span-2" : "")}>
      <p className="tw-m-0 tw-text-[0.72rem] tw-font-black tw-uppercase tw-text-vm-slate-500">{label}</p>
      <strong className="tw-mt-1 tw-block tw-break-words tw-text-[0.88rem] tw-font-extrabold tw-leading-5 tw-text-vm-slate-900">{displayValue(value)}</strong>
    </div>
  );
}

function DetailSection({ children, title }: { children: ReactNode; title: string }) {
  return (
    <section className="tw-grid tw-gap-3">
      <h4 className="tw-m-0 tw-text-[0.82rem] tw-font-black tw-uppercase tw-text-vm-slate-700">{title}</h4>
      <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[640px]:tw-grid-cols-1">{children}</div>
    </section>
  );
}

function ReviewSubjectSummary({ item }: { item: OnboardingApprovalResponse }) {
  const requestStatus = item.request?.approvalRequestStatus ?? "PENDING";
  const businessStatus = item.customer?.customerApprovalStatus ?? item.employee?.employeeStatus ?? item.account?.accountStatus;

  return (
    <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2">
      <Badge tone={approvalStatusTone(requestStatus)} className="tw-rounded-full tw-px-3">{statusLabel(requestStatus)}</Badge>
      <Badge tone={accountStatusTone(item.account?.accountStatus)} className="tw-rounded-full tw-px-3">{statusLabel(item.account?.accountStatus)}</Badge>
      {businessStatus ? <Badge tone={approvalStatusTone(businessStatus)} className="tw-rounded-full tw-px-3">{statusLabel(businessStatus)}</Badge> : null}
    </div>
  );
}

function ReviewSubjectDetails({ item }: { item: OnboardingApprovalResponse }) {
  return (
    <div className="tw-grid tw-gap-4">
      <DetailSection title="Hồ sơ cá nhân">
        <DetailItem label="Họ và tên" value={item.profile?.fullName} />
        <DetailItem label="Số điện thoại" value={item.profile?.phoneNumber} />
        <DetailItem label="Ngày sinh" value={formatDateValue(item.profile?.dateOfBirth)} />
        <DetailItem label="Giới tính" value={genderLabel(item.profile?.gender)} />
        <DetailItem label="CCCD/CMND" value={item.profile?.identifyCard} />
        <DetailItem label="Địa chỉ" value={item.profile?.address} wide />
      </DetailSection>

      <DetailSection title="Tài khoản đăng nhập">
        <DetailItem label="Username" value={item.account?.username} />
        <DetailItem label="Email" value={item.account?.email} />
        <DetailItem label="Vai trò" value={item.account?.roleCode} />
        <DetailItem label="Trạng thái tài khoản" value={statusLabel(item.account?.accountStatus)} />
      </DetailSection>

      {item.employee ? (
        <DetailSection title="Thông tin nhân sự">
          <DetailItem label="Mã nhân viên" value={item.employee.employeeCode} />
          <DetailItem label="Chức danh" value={item.employee.jobTitle} />
          <DetailItem label="Ngày vào làm" value={formatDateValue(item.employee.hiredAt)} />
          <DetailItem label="Trạng thái nhân sự" value={statusLabel(item.employee.employeeStatus)} />
        </DetailSection>
      ) : null}

      {item.customer ? (
        <DetailSection title="Thông tin khách hàng">
          <DetailItem label="Mã khách hàng" value={item.customer.customerCode} />
          <DetailItem label="Loại khách hàng" value={item.customer.customerType} />
          <DetailItem label="Trạng thái khách hàng" value={statusLabel(item.customer.customerStatus)} />
          <DetailItem label="Trạng thái duyệt" value={statusLabel(item.customer.customerApprovalStatus)} />
        </DetailSection>
      ) : null}

      <DetailSection title="Yêu cầu duyệt">
        <DetailItem label="Loại yêu cầu" value={item.request?.requestType} />
        <DetailItem label="Ngày gửi" value={formatDateValue(item.request?.createdAt)} />
        <DetailItem label="Ghi chú trước đó" value={item.request?.note} wide />
      </DetailSection>
    </div>
  );
}

function ReviewModal({
  actionError,
  isSaving,
  modal,
  note,
  onClose,
  onNoteChange,
  onSubmit,
}: {
  actionError: string;
  isSaving: boolean;
  modal: ReviewModalState;
  note: string;
  onClose: () => void;
  onNoteChange: (value: string) => void;
  onSubmit: (decision: "approve" | "reject") => void;
}) {
  if (!modal) return null;

  const title = "Chi tiết onboarding";
  const name = modal.item.profile?.fullName || modal.item.account?.username || "-";

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[2300] tw-grid tw-place-items-center tw-bg-slate-950/45 tw-p-4" role="dialog" aria-modal="true" aria-labelledby="approval-review-modal-title">
      <div className="tw-flex tw-max-h-[calc(100vh-32px)] tw-w-full tw-max-w-[780px] tw-flex-col tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-[0_28px_80px_rgba(15,23,42,0.24)]">
        <header className="tw-flex tw-items-start tw-justify-between tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-5 tw-py-4">
          <div className="tw-min-w-0">
            <h3 id="approval-review-modal-title" className="tw-m-0 tw-text-[1.08rem] tw-font-black tw-text-vm-slate-900">{title}</h3>
            <p className="tw-m-0 tw-mt-1 tw-text-[0.82rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-500">{name}</p>
            <div className="tw-mt-3">
              <ReviewSubjectSummary item={modal.item} />
            </div>
          </div>
          <button className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-slate-600 hover:tw-bg-vm-slate-100" disabled={isSaving} onClick={onClose} type="button" aria-label="Đóng">
            <i className="fas fa-times" />
          </button>
        </header>
        <div className="tw-grid tw-gap-5 tw-overflow-y-auto tw-px-5 tw-py-4">
          <ReviewSubjectDetails item={modal.item} />
          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-[0.78rem] tw-font-black tw-uppercase tw-text-vm-slate-700">Ghi chú</span>
            <textarea
              className="tw-min-h-[96px] tw-resize-none tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-2 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_4px_rgba(37,99,235,0.08)]"
              disabled={isSaving || !modal.canReview}
              maxLength={255}
              onChange={(event) => onNoteChange(event.target.value)}
              placeholder="Nhập ghi chú xét duyệt..."
              value={note}
            />
          </label>
          {actionError ? <InfoBanner tone="warning" title="Không thể cập nhật" description={actionError} icon={<i className="fas fa-exclamation-circle" />} /> : null}
        </div>
        <footer className="tw-flex tw-justify-end tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-px-5 tw-py-4">
          <Button variant="secondary" disabled={isSaving} onClick={onClose}>Đóng</Button>
          {modal.canReview ? (
            <>
              <Button variant="danger" disabled={isSaving} onClick={() => onSubmit("reject")}>
                <i className="fas fa-times" />
                Từ chối
              </Button>
              <Button variant="primary" loading={isSaving} onClick={() => onSubmit("approve")}>
                {!isSaving ? <i className="fas fa-check" /> : null}
                Duyệt
              </Button>
            </>
          ) : null}
        </footer>
      </div>
    </div>
  );
}

export function OnboardingApprovalWorkspace({ embedded = false }: OnboardingApprovalWorkspaceProps = {}) {
  const { user } = useAuth();
  const toast = useToast();
  const visibleTabs = useMemo(() => approvalTabs.filter((tab) => canOperatorSeeTab(tab, user?.role) && hasAnyPermission(user, tab.readPermissions)), [user]);
  const [activeKind, setActiveKind] = useState<OnboardingApprovalKind>(() => visibleTabs[0]?.kind ?? "internal-employee");
  const [keyword, setKeyword] = useState("");
  const [selectedStatus, setSelectedStatus] = useState<OnboardingApprovalStatus | "all">("PENDING");
  const [items, setItems] = useState<OnboardingApprovalResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [reviewModal, setReviewModal] = useState<ReviewModalState>(null);
  const [reviewNote, setReviewNote] = useState("");
  const [isReviewSaving, setIsReviewSaving] = useState(false);
  const [reviewError, setReviewError] = useState("");

  const activeTab = visibleTabs.find((tab) => tab.kind === activeKind) ?? visibleTabs[0];

  useEffect(() => {
    if (visibleTabs.length > 0 && !visibleTabs.some((tab) => tab.kind === activeKind)) {
      setActiveKind(visibleTabs[0].kind);
    }
  }, [activeKind, visibleTabs]);

  async function loadApprovals() {
    if (!activeTab) return;

    setIsLoading(true);
    setErrorMessage("");
    try {
      const response = await fetchOnboardingApprovals(activeTab.kind, {
        keyword: keyword.trim() || undefined,
        status: selectedStatus === "all" ? undefined : selectedStatus,
      });
      setItems(response);
    } catch (error) {
      setItems([]);
      setErrorMessage(error instanceof Error ? error.message : "Không thể tải danh sách hồ sơ.");
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadApprovals();
    }, 220);
    return () => window.clearTimeout(timer);
  }, [activeTab?.kind, keyword, selectedStatus]);

  const metrics = useMemo(() => ({
    approved: items.filter((item) => item.request?.approvalRequestStatus === "APPROVED").length,
    pending: items.filter((item) => item.request?.approvalRequestStatus === "PENDING").length,
    rejected: items.filter((item) => item.request?.approvalRequestStatus === "REJECTED").length,
    total: items.length,
  }), [items]);

  function openDetails(item: OnboardingApprovalResponse, canReview: boolean) {
    setReviewError("");
    setReviewNote("");
    setReviewModal({ canReview, item });
  }

  async function submitReview(decision: "approve" | "reject") {
    if (!reviewModal || !activeTab || !reviewModal.item.request?.approvalRequestId) return;

    setIsReviewSaving(true);
    setReviewError("");
    try {
      await reviewOnboardingApproval(activeTab.kind, reviewModal.item.request.approvalRequestId, decision, reviewNote);
      toast.success(decision === "approve" ? "Đã duyệt hồ sơ." : "Đã từ chối hồ sơ.", "Cập nhật thành công");
      setReviewModal(null);
      setReviewNote("");
      void loadApprovals();
    } catch (error) {
      setReviewError(error instanceof Error ? error.message : "Không thể cập nhật hồ sơ.");
    } finally {
      setIsReviewSaving(false);
    }
  }

  if (visibleTabs.length === 0) {
    const noAccessBanner = (
      <InfoBanner
        tone="warning"
        title="Không có quyền duyệt"
        description="Tài khoản hiện tại chưa có quyền xử lý hồ sơ chờ duyệt."
        icon={<i className="fas fa-shield-alt" />}
      />
    );

    return embedded ? noAccessBanner : (
      <div className="tw-px-4 tw-py-4 lg:tw-px-5">
        {noAccessBanner}
      </div>
    );
  }

  return (
    <>
      <div className={embedded ? "" : "tw-px-4 tw-py-4 lg:tw-px-5"}>
        <section className={embedded ? "tw-grid tw-gap-4" : "tw-mx-auto tw-min-h-[calc(100vh-104px)] tw-w-[min(100%,1500px)] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-vm-card"}>
          <div className="tw-mb-5 tw-flex tw-items-center tw-justify-between tw-gap-4 max-[900px]:tw-flex-col max-[900px]:tw-items-stretch">
            <div>
              <h1 className={cn("tw-m-0 tw-text-vm-slate-900", embedded ? "tw-text-[1.1rem] tw-font-black" : "tw-text-vm-page-title")}>Duyệt onboarding</h1>
            </div>
            <Button variant="secondary" onClick={() => void loadApprovals()}>
              <i className="fas fa-sync-alt" />
              Làm mới
            </Button>
          </div>

          <div className="tw-grid tw-grid-cols-4 tw-gap-4 max-[1180px]:tw-grid-cols-2 max-[680px]:tw-grid-cols-1">
            <Metric icon="fas fa-layer-group" label="Tổng hồ sơ" value={metrics.total} />
            <Metric icon="far fa-clock" label="Chờ duyệt" value={metrics.pending} />
            <Metric icon="fas fa-check-circle" label="Đã duyệt" value={metrics.approved} />
            <Metric icon="fas fa-times-circle" label="Từ chối" value={metrics.rejected} />
          </div>

          <Card className="tw-mt-5 tw-p-4">
            <div className="tw-flex tw-flex-wrap tw-gap-2">
              {visibleTabs.map((tab) => {
                const selected = activeTab?.kind === tab.kind;
                return (
                  <button
                    className={cn(
                      "tw-inline-flex tw-min-h-10 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-px-3 tw-text-[0.84rem] tw-font-extrabold tw-transition",
                      selected ? "tw-border-vm-primary tw-bg-brand-50 tw-text-vm-primary" : "tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-border-brand-100",
                    )}
                    key={tab.kind}
                    onClick={() => setActiveKind(tab.kind)}
                    type="button"
                  >
                    <i className={tab.icon} />
                    {tab.label}
                  </button>
                );
              })}
            </div>

            <div className="tw-mt-4 tw-grid tw-grid-cols-[minmax(260px,1fr)_220px_auto] tw-gap-3 max-[900px]:tw-grid-cols-1">
              <SearchInput
                aria-label="Tìm hồ sơ onboarding"
                containerClassName="tw-h-[42px]"
                onChange={setKeyword}
                placeholder="Tìm tên, email, username..."
                value={keyword}
              />
              <SelectMenu ariaLabel="Trạng thái hồ sơ" value={selectedStatus} options={statusOptions} onChange={(value) => setSelectedStatus(value as OnboardingApprovalStatus | "all")} />
              <Button
                className="tw-h-[42px]"
                variant="secondary"
                onClick={() => {
                  setKeyword("");
                  setSelectedStatus("PENDING");
                }}
              >
                <i className="fas fa-undo" />
                Đặt lại
              </Button>
            </div>
          </Card>

          <Card className="tw-mt-4 tw-overflow-hidden">
            <div className="tw-grid tw-grid-cols-[minmax(250px,1.25fr)_150px_190px_150px_112px] tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-px-4 tw-py-3 tw-text-[0.75rem] tw-font-extrabold tw-uppercase tw-text-vm-slate-500 max-[1180px]:tw-hidden">
              <span>Người dùng</span>
              <span>Tài khoản</span>
              <span>Hồ sơ</span>
              <span>Trạng thái</span>
              <span className="tw-text-right">Thao tác</span>
            </div>
            {isLoading ? (
              <div className="tw-p-4">
                <InfoBanner tone="info" title="Đang tải hồ sơ" description="Vui lòng chờ trong giây lát." icon={<i className="fas fa-spinner fa-spin" />} />
              </div>
            ) : null}
            {errorMessage ? (
              <div className="tw-p-4">
                <InfoBanner tone="warning" title="Không thể tải hồ sơ" description={errorMessage} icon={<i className="fas fa-exclamation-circle" />} />
              </div>
            ) : null}
            {!isLoading && !errorMessage && items.length === 0 ? (
              <div className="tw-p-4">
                <InfoBanner tone="success" title="Không có hồ sơ phù hợp" description="Không có hồ sơ nào cần xử lý theo bộ lọc hiện tại." icon={<i className="fas fa-check-circle" />} />
              </div>
            ) : null}
            {!isLoading && !errorMessage && activeTab
              ? items.map((item) => {
                  const canReview = canReviewItem(item, activeTab, user?.role, user?.id) && hasAnyPermission(user, activeTab.writePermissions);
                  return (
                    <ApprovalRow
                      key={item.request?.approvalRequestId ?? `${item.account?.accountId}-${item.profile?.userProfileId}`}
                      canReview={canReview}
                      item={item}
                      kind={activeTab.kind}
                      onOpenDetails={() => openDetails(item, canReview)}
                    />
                  );
                })
              : null}
          </Card>
        </section>
      </div>

      <ReviewModal
        actionError={reviewError}
        isSaving={isReviewSaving}
        modal={reviewModal}
        note={reviewNote}
        onClose={() => {
          if (!isReviewSaving) setReviewModal(null);
        }}
        onNoteChange={setReviewNote}
        onSubmit={(decision) => void submitReview(decision)}
      />
    </>
  );
}
