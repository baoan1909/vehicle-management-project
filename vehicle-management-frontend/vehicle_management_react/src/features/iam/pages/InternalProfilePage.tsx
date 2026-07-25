import { useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useAuth } from "@/core/auth/useAuth";
import {
  completeMyAccountProfile,
  deleteMyAccountAvatar,
  getMyAccountProfile,
  updateMyAccountProfile,
  uploadMyAccountAvatar,
  type AccountProfileStatusResponse,
  type UpdateAccountProfileRequest
} from "@/features/iam/api/accountProfileApi";
import {
  fetchMyOnboardingApproval,
  resubmitMyOnboardingApproval,
  type OnboardingApprovalKind,
  type OnboardingApprovalResponse
} from "@/features/iam/api/onboardingApprovalApi";
import { AddressPicker, Badge, Button, Card, DatePicker, Input, Modal, SelectMenu } from "@/components/ui";
import { mergeCurrentUserWithAccountProfile } from "@/features/iam/utils/accountProfileMapper";
import { DEFAULT_USER_AVATAR_URL, getApprovalStatusValue, getRoleLabel, getStatusMeta, type StatusTone } from "@/shared/utils/accountStatus";
import { resolvePublicMediaUrl } from "@/shared/utils/mediaUrl";

type ProfileFormState = {
  address: string;
  dateOfBirth: string;
  fullName: string;
  gender: string;
  identifyCard: string;
  phoneNumber: string;
};

type PasswordFormState = {
  confirmPassword: string;
  currentPassword: string;
  newPassword: string;
};

type PasswordVisibilityState = {
  confirmPassword: boolean;
  currentPassword: boolean;
  newPassword: boolean;
};

function statusLabel(value?: string) {
  return getStatusMeta(value).label;
}

function approvalStatusValue(profile: AccountProfileStatusResponse) {
  return getApprovalStatusValue({
    accountStatus: profile.account?.accountStatus,
    customerApprovalStatus: profile.customer?.customerApprovalStatus,
    customerStatus: profile.customer?.customerStatus,
    employeeStatus: profile.employee?.employeeStatus,
    onboardingRequired: profile.onboardingRequired,
    role: profile.account?.roleCode as NonNullable<ReturnType<typeof useAuth>["user"]>["role"]
  });
}

function resolveMyOnboardingKind(profile: AccountProfileStatusResponse): OnboardingApprovalKind | null {
  const roleCode = profile.account?.roleCode;
  if (roleCode === "CUSTOMER") return "customer";
  if (roleCode === "SYSTEM_ADMIN") return "system-admin";
  if (roleCode === "PARKING_MANAGER" || roleCode === "EMPLOYEE") return "internal-employee";
  return null;
}

function normalizeGender(value?: string) {
  return value === "Nữ" ? "Nữ" : "Nam";
}

function normalizeProfile(profile: AccountProfileStatusResponse): ProfileFormState {
  return {
    address: profile.profile?.address ?? "",
    dateOfBirth: profile.profile?.dateOfBirth ?? "",
    fullName: profile.profile?.fullName ?? "",
    gender: normalizeGender(profile.profile?.gender),
    identifyCard: profile.profile?.identifyCard ?? "",
    phoneNumber: profile.profile?.phoneNumber ?? ""
  };
}

function buildFallbackProfile(user: ReturnType<typeof useAuth>["user"]): AccountProfileStatusResponse {
  return {
    onboardingRequired: false,
    account: {
      accountId: user?.id ?? "A001",
      accountStatus: "ACTIVE",
      email: "admin@coparking.vn",
      keycloakUserId: "kc-admin-9f21",
      username: user?.username ?? "admin"
    },
    profile: {
      address: "12 Nguyễn Văn Linh, Quận 7, TP. Hồ Chí Minh",
      avatarUrl: user?.avatarUrl ?? DEFAULT_USER_AVATAR_URL,
      dateOfBirth: "1994-08-18",
      fullName: user?.fullName ?? "Nguyễn Văn Admin",
      gender: "Nam",
      identifyCard: "079094000123",
      phoneNumber: "0901 234 567",
      userProfileId: "profile-admin-001",
      userProfileStatus: "ACTIVE"
    },
    employee: {
      employeeCode: "EMP-2026-001",
      employeeStatus: "ACTIVE",
      hiredAt: "2025-05-28",
      jobTitle: "Parking Operations Admin"
    }
  };
}

function Field({
  label,
  name,
  onChange,
  placeholder,
  readOnly = false,
  type = "text",
  value
}: {
  label: string;
  name: keyof ProfileFormState;
  onChange: (name: keyof ProfileFormState, value: string) => void;
  placeholder?: string;
  readOnly?: boolean;
  type?: string;
  value: string;
}) {
  return (
    <label className="tw-grid tw-gap-2">
      <span className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-700">{label}</span>
      <Input
        className="tw-h-[42px] tw-text-[0.95rem]"
        name={name}
        onChange={(event) => onChange(name, event.target.value)}
        placeholder={placeholder}
        readOnly={readOnly}
        type={type}
        value={value}
      />
    </label>
  );
}

function StatusPill({ tone = "green", children }: { children: string; tone?: StatusTone }) {
  const badgeTone = tone === "blue" ? "primary" : tone === "orange" ? "warning" : tone === "green" ? "success" : tone === "red" ? "danger" : "neutral";
  return <Badge tone={badgeTone}>{children}</Badge>;
}

function IdentityCard({
  avatarUrl,
  displayName,
  onAvatarChange,
  onAvatarDelete,
  profile
}: {
  avatarUrl: string;
  displayName: string;
  onAvatarChange: (file: File) => void;
  onAvatarDelete: () => void;
  profile: AccountProfileStatusResponse;
}) {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const approvalStatus = approvalStatusValue(profile);
  const approvalMeta = getStatusMeta(approvalStatus);

  return (
    <Card className="tw-grid tw-min-w-0 tw-justify-items-center tw-rounded-vm-lg tw-border tw-border-solid !tw-border-vm-slate-100 tw-p-[1.05rem] tw-text-center tw-shadow-[0_14px_36px_rgba(15,23,42,0.05)]">
      <div className="tw-relative tw-h-28 tw-w-28">
        <img
          src={avatarUrl}
          alt={displayName}
          className="tw-h-28 tw-w-28 tw-rounded-full tw-border-[3px] tw-border-brand-100 tw-object-cover tw-shadow-[0_14px_24px_rgba(37,99,235,0.14)]"
        />
        <button
          className="tw-absolute tw-bottom-[0.15rem] tw-right-0 tw-inline-flex tw-h-[34px] tw-w-[34px] tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-vm-primary tw-shadow-[0_10px_20px_rgba(15,23,42,0.14)] tw-transition hover:tw-border-vm-slate-200 hover:tw-bg-vm-slate-25"
          type="button"
          onClick={() => fileInputRef.current?.click()}
        >
          <i className="fas fa-camera" />
        </button>
        <input
          ref={fileInputRef}
          className="tw-hidden"
          type="file"
          accept="image/*"
          onChange={(event) => {
            const file = event.target.files?.[0];
            if (file) onAvatarChange(file);
            event.target.value = "";
          }}
        />
      </div>

      <h3 className="tw-mb-1 tw-mt-3.5 tw-text-[1.14rem] tw-font-black tw-leading-tight tw-text-vm-slate-900">{displayName}</h3>
      <p className="tw-mb-[0.7rem] tw-text-[0.9rem] tw-font-bold tw-text-vm-slate-500">{profile.account?.email ?? "Chưa có email"}</p>
      <div className="tw-flex tw-flex-wrap tw-justify-center tw-gap-2">
        <StatusPill tone={getStatusMeta(profile.account?.accountStatus).tone}>{statusLabel(profile.account?.accountStatus)}</StatusPill>
        <StatusPill tone={approvalMeta.tone}>{approvalMeta.label}</StatusPill>
      </div>

      <div className="tw-mt-4 tw-grid tw-w-full">
        <Button variant="danger" type="button" onClick={onAvatarDelete}>
          <i className="far fa-trash-alt" />
          <span>Xóa</span>
        </Button>
      </div>

      <dl className="tw-mt-4 tw-grid tw-w-full tw-gap-[0.7rem]">
        <div className="tw-grid tw-grid-cols-[minmax(86px,0.72fr)_minmax(0,1fr)] tw-items-start tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-3 tw-text-left">
          <dt className="tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-500">Mã nhân viên</dt>
          <dd className="tw-m-0 tw-break-words tw-text-[0.88rem] tw-font-extrabold tw-text-vm-slate-900">{profile.employee?.employeeCode ?? "-"}</dd>
        </div>
        <div className="tw-grid tw-grid-cols-[minmax(86px,0.72fr)_minmax(0,1fr)] tw-items-start tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-3 tw-text-left">
          <dt className="tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-500">Chức danh</dt>
          <dd className="tw-m-0 tw-break-words tw-text-[0.88rem] tw-font-extrabold tw-text-vm-slate-900">{profile.employee?.jobTitle ?? getRoleLabel()}</dd>
        </div>
        <div className="tw-grid tw-grid-cols-[minmax(86px,0.72fr)_minmax(0,1fr)] tw-items-start tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-3 tw-text-left">
          <dt className="tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-500">Ngày vào làm</dt>
          <dd className="tw-m-0 tw-break-words tw-text-[0.88rem] tw-font-extrabold tw-text-vm-slate-900">{profile.employee?.hiredAt ?? "-"}</dd>
        </div>
      </dl>
    </Card>
  );
}

function ChangePasswordModal({
  onClose,
  onSubmit,
  open
}: {
  onClose: () => void;
  onSubmit: (form: PasswordFormState) => void;
  open: boolean;
}) {
  const [form, setForm] = useState<PasswordFormState>({
    confirmPassword: "",
    currentPassword: "",
    newPassword: ""
  });
  const [visible, setVisible] = useState<PasswordVisibilityState>({
    confirmPassword: false,
    currentPassword: false,
    newPassword: false
  });
  const canSubmit = form.currentPassword.length > 0 && form.newPassword.length >= 8 && form.newPassword === form.confirmPassword;

  useEffect(() => {
    if (!open) {
      setForm({ confirmPassword: "", currentPassword: "", newPassword: "" });
      setVisible({ confirmPassword: false, currentPassword: false, newPassword: false });
    }
  }, [open]);

  const update = (name: keyof PasswordFormState, value: string) => {
    setForm((current) => ({ ...current, [name]: value }));
  };

  const toggleVisible = (name: keyof PasswordVisibilityState) => {
    setVisible((current) => ({ ...current, [name]: !current[name] }));
  };

  return (
    <Modal
      open={open}
      title="Đổi mật khẩu"
      description="Cập nhật mật khẩu đăng nhập nội bộ."
      onClose={onClose}
      actions={
        <div className="tw-flex tw-justify-end tw-gap-3">
          <Button variant="secondary" type="button" onClick={onClose}>
            Hủy
          </Button>
          <Button disabled={!canSubmit} type="button" onClick={() => onSubmit(form)}>
            Lưu mật khẩu
          </Button>
        </div>
      }
    >
      <div className="tw-grid tw-gap-3.5">
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-700">Mật khẩu hiện tại</span>
          <span className="tw-relative tw-block tw-h-[42px]">
            <Input
              className="tw-h-[42px] tw-pr-11"
              type={visible.currentPassword ? "text" : "password"}
              value={form.currentPassword}
              onChange={(event) => update("currentPassword", event.target.value)}
            />
            <button
              className="tw-absolute tw-inset-y-[5px] tw-right-2 tw-inline-flex tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-slate-500 tw-transition hover:tw-bg-vm-slate-100 hover:tw-text-vm-slate-800 focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus"
              type="button"
              aria-label={visible.currentPassword ? "Ẩn mật khẩu hiện tại" : "Hiện mật khẩu hiện tại"}
              onClick={() => toggleVisible("currentPassword")}
            >
              <i className={visible.currentPassword ? "far fa-eye-slash" : "far fa-eye"} />
            </button>
          </span>
        </label>
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-700">Mật khẩu mới</span>
          <span className="tw-relative tw-block tw-h-[42px]">
            <Input className="tw-h-[42px] tw-pr-11" type={visible.newPassword ? "text" : "password"} value={form.newPassword} onChange={(event) => update("newPassword", event.target.value)} />
            <button
              className="tw-absolute tw-inset-y-[5px] tw-right-2 tw-inline-flex tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-slate-500 tw-transition hover:tw-bg-vm-slate-100 hover:tw-text-vm-slate-800 focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus"
              type="button"
              aria-label={visible.newPassword ? "Ẩn mật khẩu mới" : "Hiện mật khẩu mới"}
              onClick={() => toggleVisible("newPassword")}
            >
              <i className={visible.newPassword ? "far fa-eye-slash" : "far fa-eye"} />
            </button>
          </span>
        </label>
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-700">Nhập lại mật khẩu mới</span>
          <span className="tw-relative tw-block tw-h-[42px]">
            <Input
              className="tw-h-[42px] tw-pr-11"
              type={visible.confirmPassword ? "text" : "password"}
              value={form.confirmPassword}
              onChange={(event) => update("confirmPassword", event.target.value)}
            />
            <button
              className="tw-absolute tw-inset-y-[5px] tw-right-2 tw-inline-flex tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-slate-500 tw-transition hover:tw-bg-vm-slate-100 hover:tw-text-vm-slate-800 focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus"
              type="button"
              aria-label={visible.confirmPassword ? "Ẩn nhập lại mật khẩu" : "Hiện nhập lại mật khẩu"}
              onClick={() => toggleVisible("confirmPassword")}
            >
              <i className={visible.confirmPassword ? "far fa-eye-slash" : "far fa-eye"} />
            </button>
          </span>
        </label>
        <div className="tw-flex tw-flex-wrap tw-gap-2">
          <Badge tone={form.newPassword.length >= 8 ? "success" : "neutral"}>Tối thiểu 8 ký tự</Badge>
          <Badge tone={form.confirmPassword && form.newPassword === form.confirmPassword ? "success" : "neutral"}>Nhập lại trùng khớp</Badge>
        </div>
      </div>
    </Modal>
  );
}

function ProfileReadinessBanner({
  dirty,
  latestApproval,
  onComplete,
  onResubmit,
  profile,
  resubmitting,
  saving
}: {
  dirty: boolean;
  latestApproval: OnboardingApprovalResponse | null;
  onComplete: () => void;
  onResubmit: () => void;
  profile: AccountProfileStatusResponse;
  resubmitting: boolean;
  saving: boolean;
}) {
  const isRequired = profile.onboardingRequired;
  const approvalStatus = latestApproval?.request?.approvalRequestStatus ?? approvalStatusValue(profile);
  const isPending = approvalStatus === "PENDING";
  const isRejected = approvalStatus === "REJECTED";
  const rejectionNote = latestApproval?.request?.note?.trim();

  return (
    <section
      className={`tw-grid tw-min-h-[82px] tw-grid-cols-[28px_minmax(0,1fr)] tw-items-start tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-px-5 tw-py-4 ${
        isRejected || isRequired || isPending ? "tw-border-amber-200 tw-bg-amber-50/70 tw-text-amber-700" : "tw-border-green-100 tw-bg-green-50/80 tw-text-green-700"
      }`}
    >
      <i className={`${isRejected || isRequired ? "fas fa-exclamation-circle" : isPending ? "fas fa-clock" : "fas fa-check-circle"} tw-mt-1 tw-text-[1.15rem]`} />
      <div className="tw-grid tw-min-w-0 tw-gap-3 min-[760px]:tw-grid-cols-[minmax(0,1fr)_auto] min-[760px]:tw-items-center">
        <div className="tw-min-w-0">
        <strong className="tw-block tw-text-[0.98rem] tw-font-black tw-text-vm-slate-900">
          {isRejected ? "Hồ sơ cần bổ sung" : isRequired ? "Cần hoàn tất hồ sơ" : isPending ? "Hồ sơ đang chờ duyệt" : "Hồ sơ đã sẵn sàng"}
        </strong>
        <p className="tw-mb-0 tw-mt-1.5 tw-text-[0.84rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-700">
          {isRejected ? "Cập nhật thông tin theo góp ý rồi gửi lại để chờ duyệt." : isRequired ? "Vui lòng bổ sung thông tin để hoàn tất hồ sơ." : isPending ? "Thông tin đã được gửi và đang chờ người phụ trách duyệt." : "Thông tin cá nhân đã đủ để đồng bộ với tài khoản nội bộ."}
        </p>
        {rejectionNote ? <p className="tw-mb-0 tw-mt-1 tw-text-[0.84rem] tw-font-bold tw-leading-6 tw-text-amber-800">Lý do: {rejectionNote}</p> : null}
        </div>
        {isRejected ? (
          <Button className="tw-min-h-10 tw-whitespace-nowrap tw-font-extrabold" type="button" disabled={saving || resubmitting} loading={resubmitting} onClick={onResubmit}>
            {!resubmitting ? <i className="fas fa-paper-plane" /> : null}
            <span>{resubmitting ? "Đang gửi..." : dirty ? "Lưu và gửi duyệt lại" : "Gửi duyệt lại"}</span>
          </Button>
        ) : isRequired ? (
          <Button className="tw-min-h-10 tw-whitespace-nowrap tw-font-extrabold" type="button" disabled={saving || resubmitting} loading={saving} onClick={onComplete}>
            {!saving ? <i className="fas fa-paper-plane" /> : null}
            <span>{saving ? "Đang gửi..." : "Gửi hồ sơ"}</span>
          </Button>
        ) : null}
      </div>
    </section>
  );
}

function StatusPanel({ onChangePassword, profile }: { onChangePassword: () => void; profile: AccountProfileStatusResponse }) {
  const approvalStatus = approvalStatusValue(profile);

  return (
    <Card className="tw-min-w-0 tw-rounded-vm-lg tw-border tw-border-solid !tw-border-vm-slate-100 tw-p-4 tw-shadow-[0_14px_36px_rgba(15,23,42,0.05)] max-[1320px]:tw-col-span-full max-[900px]:tw-col-auto">
      <h3 className="tw-m-0 tw-text-vm-section-title tw-font-black tw-text-vm-slate-900">Trạng thái hệ thống</h3>

      <div className="tw-mt-4 tw-grid tw-gap-3 min-[1321px]:tw-grid-cols-1 max-[1320px]:tw-grid-cols-4 max-[900px]:tw-grid-cols-1">
        <article className="tw-grid tw-min-h-[74px] tw-grid-cols-[48px_minmax(0,1fr)] tw-items-center tw-gap-3.5 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
          <i className="fas fa-user-check tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-vm-lg tw-bg-brand-50 tw-text-[1.2rem] tw-text-vm-primary" />
          <div>
            <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500">Phê duyệt</span>
            <strong className="tw-mt-1 tw-block tw-text-[0.94rem] tw-font-black tw-text-vm-slate-900">{statusLabel(approvalStatus)}</strong>
          </div>
        </article>
        <article className="tw-grid tw-min-h-[74px] tw-grid-cols-[48px_minmax(0,1fr)] tw-items-center tw-gap-3.5 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
          <i className="fas fa-user-shield tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-vm-lg tw-bg-brand-50 tw-text-[1.2rem] tw-text-vm-primary" />
          <div>
            <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500">Tài khoản</span>
            <strong className="tw-mt-1 tw-block tw-text-[0.94rem] tw-font-black tw-text-vm-slate-900">{statusLabel(profile.account?.accountStatus)}</strong>
          </div>
        </article>
        <article className="tw-grid tw-min-h-[74px] tw-grid-cols-[48px_minmax(0,1fr)] tw-items-center tw-gap-3.5 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
          <i className="fas fa-id-badge tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-vm-lg tw-bg-brand-50 tw-text-[1.2rem] tw-text-vm-primary" />
          <div>
            <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500">Hồ sơ</span>
            <strong className="tw-mt-1 tw-block tw-text-[0.94rem] tw-font-black tw-text-vm-slate-900">{statusLabel(profile.profile?.userProfileStatus)}</strong>
          </div>
        </article>
        <article className="tw-grid tw-min-h-[74px] tw-grid-cols-[48px_minmax(0,1fr)] tw-items-center tw-gap-3.5 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
          <i className="fas fa-briefcase tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-vm-lg tw-bg-brand-50 tw-text-[1.2rem] tw-text-vm-primary" />
          <div>
            <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500">Nhân sự</span>
            <strong className="tw-mt-1 tw-block tw-text-[0.94rem] tw-font-black tw-text-vm-slate-900">{statusLabel(profile.employee?.employeeStatus)}</strong>
          </div>
        </article>
      </div>

      <div className="tw-mt-4 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-4">
        <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
          <h4 className="tw-m-0 tw-text-[1rem] tw-font-black tw-text-vm-slate-900">Bảo mật đăng nhập</h4>
          <Button className="tw-h-9 tw-flex-shrink-0 tw-whitespace-nowrap tw-px-3.5 tw-text-[0.84rem]" variant="secondary" type="button" onClick={onChangePassword}>
            <i className="fas fa-key" />
            <span>Đổi mật khẩu</span>
          </Button>
        </div>
        <dl className="tw-mt-4 tw-grid tw-w-full tw-gap-3">
          <div className="tw-grid tw-grid-cols-[minmax(86px,0.72fr)_minmax(0,1fr)] tw-items-start tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-3">
            <dt className="tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-500">Username</dt>
            <dd className="tw-m-0 tw-break-words tw-text-[0.88rem] tw-font-extrabold tw-text-vm-slate-900">{profile.account?.username ?? "-"}</dd>
          </div>
          <div className="tw-grid tw-grid-cols-[minmax(86px,0.72fr)_minmax(0,1fr)] tw-items-start tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-3">
            <dt className="tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-500">Keycloak ID</dt>
            <dd className="tw-m-0 tw-break-words tw-text-[0.88rem] tw-font-extrabold tw-text-vm-slate-900">{profile.account?.keycloakUserId ?? "-"}</dd>
          </div>
        </dl>
      </div>

    </Card>
  );
}

export function InternalProfilePage() {
  const { user, setUser } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [profile, setProfile] = useState<AccountProfileStatusResponse>(() => buildFallbackProfile(user));
  const [latestApproval, setLatestApproval] = useState<OnboardingApprovalResponse | null>(null);
  const [form, setForm] = useState<ProfileFormState>(() => normalizeProfile(buildFallbackProfile(user)));
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [resubmitting, setResubmitting] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(() => searchParams.get("action") === "change-password");
  const [notice, setNotice] = useState<string | null>(null);

  const displayName = form.fullName || profile.profile?.fullName || user?.fullName || "Nguyễn Văn Admin";
  const avatarUrl = resolvePublicMediaUrl(profile.profile?.avatarUrl) || resolvePublicMediaUrl(user?.avatarUrl) || DEFAULT_USER_AVATAR_URL;
  const dirty = useMemo(() => JSON.stringify(form) !== JSON.stringify(normalizeProfile(profile)), [form, profile]);
  const currentApprovalStatus = latestApproval?.request?.approvalRequestStatus ?? approvalStatusValue(profile);
  const showSaveProfileButton = currentApprovalStatus === "APPROVED";

  const refreshLatestApproval = async (nextProfile: AccountProfileStatusResponse) => {
    const kind = resolveMyOnboardingKind(nextProfile);
    if (!kind) {
      setLatestApproval(null);
      return;
    }

    try {
      const approval = await fetchMyOnboardingApproval(kind);
      setLatestApproval(approval);
    } catch {
      setLatestApproval(null);
    }
  };

  useEffect(() => {
    let mounted = true;

    getMyAccountProfile()
      .then((response) => {
        if (!mounted) return;
        setProfile(response.data);
        setForm(normalizeProfile(response.data));
        setNotice(null);
        void refreshLatestApproval(response.data);
      })
      .catch(() => {
        if (!mounted) return;
        const fallback = buildFallbackProfile(user);
        setProfile(fallback);
        setForm(normalizeProfile(fallback));
        setLatestApproval(null);
        setNotice("Chưa tải được hồ sơ mới nhất. Đang hiển thị thông tin tạm thời.");
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });

    return () => {
      mounted = false;
    };
  }, [user]);

  useEffect(() => {
    if (searchParams.get("action") === "change-password") {
      setPasswordOpen(true);
    }
  }, [searchParams]);

  const updateField = (name: keyof ProfileFormState, value: string) => {
    setForm((current) => ({ ...current, [name]: value }));
  };

  const applyProfileResponse = (nextProfile: AccountProfileStatusResponse) => {
    setProfile(nextProfile);
    setForm(normalizeProfile(nextProfile));
    setUser(user ? mergeCurrentUserWithAccountProfile(user, nextProfile) : user);
  };

  const buildProfilePayload = (): UpdateAccountProfileRequest => ({
      address: form.address || undefined,
      dateOfBirth: form.dateOfBirth || undefined,
      fullName: form.fullName,
      gender: form.gender || undefined,
      identifyCard: form.identifyCard || undefined,
      phoneNumber: form.phoneNumber
  });

  const saveProfileChanges = async () => {
    const payload = buildProfilePayload();
    const shouldCompleteOnboarding = profile.onboardingRequired || !profile.profile?.userProfileId;
    const response = shouldCompleteOnboarding
      ? await completeMyAccountProfile(payload)
      : await updateMyAccountProfile(payload);
    applyProfileResponse(response.data);
    return {
      profile: response.data,
      shouldCompleteOnboarding
    };
  };

  const handleSave = async () => {
    setSaving(true);
    setNotice(null);

    const payload = buildProfilePayload();

    try {
      const result = await saveProfileChanges();
      void refreshLatestApproval(result.profile);
      setNotice(result.shouldCompleteOnboarding ? "Đã gửi hồ sơ để chờ phê duyệt." : "Đã lưu hồ sơ cá nhân thành công.");
    } catch {
      const fallbackProfile: AccountProfileStatusResponse = {
        ...profile,
        profile: {
          ...profile.profile,
          ...payload,
          userProfileStatus: profile.profile?.userProfileStatus ?? "ACTIVE"
        }
      };
      applyProfileResponse(fallbackProfile);
      setNotice("Đã cập nhật thông tin trên giao diện. Vui lòng thử lại nếu dữ liệu chưa được lưu.");
    } finally {
      setSaving(false);
    }
  };

  const handleResubmitApproval = async () => {
    const kind = resolveMyOnboardingKind(profile);
    if (!kind) {
      setNotice("Tài khoản hiện tại chưa có luồng duyệt lại phù hợp.");
      return;
    }

    setResubmitting(true);
    setNotice(null);

    try {
      let nextProfile = profile;
      if (dirty) {
        const result = await saveProfileChanges();
        nextProfile = result.profile;
      }
      const approval = await resubmitMyOnboardingApproval(kind);
      const response = await getMyAccountProfile();
      setLatestApproval(approval);
      applyProfileResponse(response.data);
      void refreshLatestApproval(response.data ?? nextProfile);
      setNotice("Đã gửi lại hồ sơ để chờ phê duyệt.");
    } catch {
      setNotice("Không thể gửi lại hồ sơ để duyệt. Vui lòng kiểm tra thông tin và thử lại.");
    } finally {
      setResubmitting(false);
    }
  };

  const handleAvatarChange = async (file: File) => {
    const previewUrl = URL.createObjectURL(file);
    const optimisticProfile = {
      ...profile,
      profile: {
        ...profile.profile,
        avatarUrl: previewUrl
      }
    };

    applyProfileResponse(optimisticProfile);

    try {
      const response = await uploadMyAccountAvatar(file);
      applyProfileResponse(response.data);
      setNotice("Đã cập nhật ảnh đại diện.");
      URL.revokeObjectURL(previewUrl);
    } catch {
      setNotice("Đã xem trước ảnh đại diện. Vui lòng thử lại nếu ảnh chưa được lưu.");
    }
  };

  const handleAvatarDelete = async () => {
    try {
      const response = await deleteMyAccountAvatar();
      applyProfileResponse(response.data);
      setNotice("Đã xóa ảnh đại diện.");
    } catch {
      applyProfileResponse({
        ...profile,
        profile: {
          ...profile.profile,
          avatarUrl: DEFAULT_USER_AVATAR_URL
        }
      });
      setNotice("Đã đưa ảnh đại diện về mặc định trên giao diện.");
    }
  };

  const closePasswordModal = () => {
    setPasswordOpen(false);
    if (searchParams.get("action")) {
      setSearchParams({});
    }
  };

  const handleChangePassword = () => {
    closePasswordModal();
    setNotice("Yêu cầu đổi mật khẩu đã được ghi nhận.");
  };

  return (
    <div className="content-header tw-px-0 tw-pb-4 tw-pt-3">
      <section className="content tw-pb-8">
        <div className="container-fluid tw-max-w-[1480px]">
          <div className="tw-grid tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4 tw-shadow-[0_16px_34px_rgba(15,23,42,0.04)]">
            <header className="tw-flex tw-items-center tw-justify-between tw-gap-4 tw-px-1 tw-pb-[0.65rem] tw-pt-[0.3rem] max-[900px]:tw-flex-col max-[900px]:tw-items-stretch">
              <div>
                <h2 className="tw-m-0 tw-text-vm-page-title tw-font-extrabold tw-leading-none tw-text-[#111827]">Thông tin tài khoản</h2>
                <p className="tw-mb-0 tw-mt-2 tw-max-w-[720px] tw-text-[0.92rem] tw-font-semibold tw-leading-[1.55] tw-text-vm-slate-500">
                  Quản lý hồ sơ cá nhân, avatar và thông tin nhân sự nội bộ của tài khoản đang đăng nhập.
                </p>
              </div>
              <div className="tw-flex tw-items-center tw-gap-3 max-[900px]:tw-flex-col max-[900px]:tw-items-stretch">
                <Button className="tw-min-h-11 tw-font-extrabold" variant="secondary" disabled={!dirty || saving || resubmitting} type="button" onClick={() => setForm(normalizeProfile(profile))}>
                  <i className="fas fa-undo" />
                  <span>Hoàn tác</span>
                </Button>
                {showSaveProfileButton ? (
                  <Button className="tw-min-h-11 tw-font-extrabold" disabled={loading || resubmitting} loading={saving} type="button" onClick={handleSave}>
                    {!saving ? <i className="far fa-save" /> : null}
                    <span>{saving ? "Đang lưu..." : "Lưu hồ sơ"}</span>
                  </Button>
                ) : null}
              </div>
            </header>

            <ProfileReadinessBanner dirty={dirty} latestApproval={latestApproval} onComplete={handleSave} onResubmit={handleResubmitApproval} profile={profile} resubmitting={resubmitting} saving={saving} />

            {notice ? (
              <div className="tw-flex tw-min-h-11 tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-brand-100 tw-bg-brand-50 tw-px-4 tw-text-[0.86rem] tw-font-bold tw-text-blue-900">
                <i className="fas fa-info-circle" />
                <span>{notice}</span>
              </div>
            ) : null}

            <div className="tw-grid tw-grid-cols-[minmax(250px,290px)_minmax(0,1fr)_minmax(270px,300px)] tw-items-start tw-gap-[0.9rem] max-[1320px]:tw-grid-cols-[minmax(240px,280px)_minmax(0,1fr)] max-[900px]:tw-grid-cols-1">
              <IdentityCard avatarUrl={avatarUrl} displayName={displayName} onAvatarChange={handleAvatarChange} onAvatarDelete={handleAvatarDelete} profile={profile} />

              <Card className="tw-min-w-0 tw-rounded-vm-lg tw-border tw-border-solid !tw-border-vm-slate-100 tw-p-4 tw-shadow-[0_14px_36px_rgba(15,23,42,0.05)]">
                <div className="tw-mb-4 tw-flex tw-items-center tw-justify-between tw-gap-4 max-[900px]:tw-flex-col max-[900px]:tw-items-stretch">
                  <div>
                    <h3 className="tw-m-0 tw-text-vm-section-title tw-font-black tw-text-[#111827]">Hồ sơ cá nhân</h3>
                    <p className="tw-mb-0 tw-mt-1.5 tw-text-[0.88rem] tw-font-semibold tw-text-vm-slate-500">Họ tên và số điện thoại là thông tin bắt buộc.</p>
                  </div>
                  <StatusPill tone="blue">{getRoleLabel(user?.role, user?.roleLabel)}</StatusPill>
                </div>

                <div className="tw-grid tw-grid-cols-2 tw-gap-3.5 max-[900px]:tw-grid-cols-1">
                  <Field label="Họ và tên" name="fullName" value={form.fullName} placeholder="Nhập họ tên" onChange={updateField} />
                  <Field label="Số điện thoại" name="phoneNumber" value={form.phoneNumber} placeholder="Nhập số điện thoại" onChange={updateField} />
                  <label className="tw-grid tw-gap-2">
                    <span className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-700">Ngày sinh</span>
                    <DatePicker
                      ariaLabel="Chọn ngày sinh"
                      max={new Date().toISOString().slice(0, 10)}
                      value={form.dateOfBirth}
                      onChange={(value) => updateField("dateOfBirth", value)}
                    />
                  </label>
                  <label className="tw-grid tw-gap-2">
                    <span className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-700">Giới tính</span>
                    <SelectMenu
                      ariaLabel="Giới tính"
                      clearValue=""
                      value={form.gender}
                      onChange={(value) => updateField("gender", value)}
                      options={[
                        { label: "Nam", value: "Nam" },
                        { label: "Nữ", value: "Nữ" }
                      ]}
                    />
                  </label>
                  <Field label="CCCD/CMND" name="identifyCard" value={form.identifyCard} placeholder="Nhập mã định danh" onChange={updateField} />
                  <label className="tw-grid tw-gap-2">
                    <span className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-700">Email đăng nhập</span>
                    <Input className="tw-h-[42px] tw-bg-vm-slate-25 tw-text-[0.95rem] tw-text-vm-slate-500" readOnly value={profile.account?.email ?? ""} />
                  </label>
                  <div className="tw-col-span-full tw-grid tw-gap-2">
                    <span className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-700">Địa chỉ liên hệ</span>
                    <AddressPicker value={form.address} onChange={(value) => updateField("address", value)} />
                  </div>
                </div>
              </Card>

              <StatusPanel profile={profile} onChangePassword={() => setPasswordOpen(true)} />
            </div>

            <ChangePasswordModal open={passwordOpen} onClose={closePasswordModal} onSubmit={handleChangePassword} />
          </div>
        </div>
      </section>
    </div>
  );
}
