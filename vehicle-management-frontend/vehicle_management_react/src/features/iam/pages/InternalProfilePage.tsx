import { useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useAuth } from "@/core/auth/useAuth";
import {
  deleteMyAccountAvatar,
  getMyAccountProfile,
  updateMyAccountProfile,
  uploadMyAccountAvatar,
  type AccountProfileStatusResponse,
  type UpdateAccountProfileRequest
} from "@/features/iam/api/accountProfileApi";
import { AddressPicker, Badge, Button, Card, DatePicker, Input, Modal, SelectMenu } from "@/components/ui";

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

const defaultAvatar = "/assets/admin/dist/img/user2-160x160.jpg";

function roleLabel(role?: string) {
  switch (role) {
    case "ADMIN":
    case "SYSTEM_ADMIN":
      return "Quản trị hệ thống";
    case "PARKING_MANAGER":
      return "Quản lý bãi xe";
    case "EMPLOYEE":
      return "Nhân viên nội bộ";
    case "CUSTOMER":
      return "Khách hàng";
    default:
      return "Nhân sự CoParking";
  }
}

function statusLabel(value?: string) {
  if (!value) return "Chưa có dữ liệu";

  const labels: Record<string, string> = {
    ACTIVE: "Đang hoạt động",
    INACTIVE: "Chưa kích hoạt",
    PENDING: "Chờ duyệt",
    SUSPENDED: "Tạm khóa"
  };

  return labels[value] ?? value;
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
      avatarUrl: user?.avatarUrl ?? defaultAvatar,
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

function StatusPill({ tone = "green", children }: { children: string; tone?: "blue" | "green" | "orange" | "slate" }) {
  const badgeTone = tone === "blue" ? "primary" : tone === "orange" ? "warning" : tone === "green" ? "success" : "neutral";
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
      <StatusPill>{statusLabel(profile.account?.accountStatus)}</StatusPill>

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
          <dd className="tw-m-0 tw-break-words tw-text-[0.88rem] tw-font-extrabold tw-text-vm-slate-900">{profile.employee?.jobTitle ?? roleLabel()}</dd>
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
      description="Cập nhật mật khẩu đăng nhập nội bộ. Endpoint đổi mật khẩu sẽ được nối với Keycloak ở bước tích hợp backend."
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

function StatusPanel({ onChangePassword, profile }: { onChangePassword: () => void; profile: AccountProfileStatusResponse }) {
  return (
    <Card className="tw-min-w-0 tw-rounded-vm-lg tw-border tw-border-solid !tw-border-vm-slate-100 tw-p-4 tw-shadow-[0_14px_36px_rgba(15,23,42,0.05)] max-[1320px]:tw-col-span-full max-[900px]:tw-col-auto">
      <h3 className="tw-m-0 tw-text-vm-section-title tw-font-black tw-text-vm-slate-900">Trạng thái hệ thống</h3>

      <div className="tw-mt-4 tw-grid tw-gap-3 max-[1320px]:tw-grid-cols-3 max-[900px]:tw-grid-cols-1">
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

      <div
        className={`tw-mt-4 tw-grid tw-grid-cols-[26px_minmax(0,1fr)] tw-gap-3 tw-rounded-vm-md tw-border tw-p-3.5 ${
          profile.onboardingRequired ? "tw-border-orange-200 tw-bg-orange-50 tw-text-orange-700" : "tw-border-green-200 tw-bg-green-50 tw-text-green-700"
        }`}
      >
        <i className={profile.onboardingRequired ? "fas fa-exclamation-circle" : "fas fa-check-circle"} />
        <div>
          <strong className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-900">{profile.onboardingRequired ? "Cần hoàn tất hồ sơ" : "Hồ sơ đã sẵn sàng"}</strong>
          <p className="tw-mb-0 tw-mt-1.5 tw-text-[0.78rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-700">
            {profile.onboardingRequired
              ? "Backend yêu cầu bổ sung thông tin trước khi sử dụng đầy đủ tính năng nội bộ."
              : "Thông tin cá nhân đã đủ để đồng bộ với tài khoản nội bộ."}
          </p>
        </div>
      </div>
    </Card>
  );
}

export function InternalProfilePage() {
  const { user, setUser } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [profile, setProfile] = useState<AccountProfileStatusResponse>(() => buildFallbackProfile(user));
  const [form, setForm] = useState<ProfileFormState>(() => normalizeProfile(buildFallbackProfile(user)));
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(() => searchParams.get("action") === "change-password");
  const [notice, setNotice] = useState<string | null>(null);

  const displayName = form.fullName || profile.profile?.fullName || user?.fullName || "Nguyễn Văn Admin";
  const avatarUrl = profile.profile?.avatarUrl || user?.avatarUrl || defaultAvatar;
  const dirty = useMemo(() => JSON.stringify(form) !== JSON.stringify(normalizeProfile(profile)), [form, profile]);

  useEffect(() => {
    let mounted = true;

    getMyAccountProfile()
      .then((response) => {
        if (!mounted) return;
        setProfile(response.data);
        setForm(normalizeProfile(response.data));
        setNotice(null);
      })
      .catch(() => {
        if (!mounted) return;
        const fallback = buildFallbackProfile(user);
        setProfile(fallback);
        setForm(normalizeProfile(fallback));
        setNotice("Đang hiển thị dữ liệu mẫu theo tài khoản hiện tại. API profile đã sẵn sàng để kết nối backend.");
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
    setUser(
      user
        ? {
            ...user,
            avatarUrl: nextProfile.profile?.avatarUrl ?? user.avatarUrl,
            fullName: nextProfile.profile?.fullName ?? user.fullName
          }
        : user
    );
  };

  const handleSave = async () => {
    setSaving(true);
    setNotice(null);

    const payload: UpdateAccountProfileRequest = {
      address: form.address || undefined,
      dateOfBirth: form.dateOfBirth || undefined,
      fullName: form.fullName,
      gender: form.gender || undefined,
      identifyCard: form.identifyCard || undefined,
      phoneNumber: form.phoneNumber
    };

    try {
      const response = await updateMyAccountProfile(payload);
      applyProfileResponse(response.data);
      setNotice("Đã lưu hồ sơ cá nhân thành công.");
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
      setNotice("Đã cập nhật giao diện với dữ liệu mới. Backend chưa phản hồi trong môi trường hiện tại.");
    } finally {
      setSaving(false);
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
      setNotice("Đã xem trước ảnh đại diện. Upload backend sẽ hoạt động khi API sẵn sàng.");
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
          avatarUrl: defaultAvatar
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
    setNotice("Yêu cầu đổi mật khẩu đã được ghi nhận trên giao diện. Cần backend bổ sung endpoint đổi mật khẩu hoặc redirect Keycloak account console.");
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
                <Button className="tw-min-h-11 tw-font-extrabold" variant="secondary" disabled={!dirty || saving} type="button" onClick={() => setForm(normalizeProfile(profile))}>
                  <i className="fas fa-undo" />
                  <span>Hoàn tác</span>
                </Button>
                <Button className="tw-min-h-11 tw-font-extrabold" disabled={saving || loading} type="button" onClick={handleSave}>
                  <i className="far fa-save" />
                  <span>{saving ? "Đang lưu..." : "Lưu hồ sơ"}</span>
                </Button>
              </div>
            </header>

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
                    <p className="tw-mb-0 tw-mt-1.5 tw-text-[0.88rem] tw-font-semibold tw-text-vm-slate-500">Các trường họ tên và số điện thoại là bắt buộc theo rule backend.</p>
                  </div>
                  <StatusPill tone="blue">{roleLabel(user?.role)}</StatusPill>
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
