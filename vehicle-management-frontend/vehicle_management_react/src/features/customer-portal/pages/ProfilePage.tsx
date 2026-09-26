import { useEffect, useMemo, useRef, useState, type ChangeEvent } from "react";
import { Link } from "react-router-dom";

import {
  getCustomerPortalLookups,
  getCustomerPortalProfile,
  getMyCustomerVehicles,
  getMySubscriptions,
  updateCustomerPortalProfile,
  type CustomerPortalProfile,
  type CustomerPortalSubscription,
  type CustomerPortalTicketType,
  type CustomerPortalVehicle,
  type CustomerPortalVehicleType,
} from "@/features/customer-portal/api/customerPortalApi";
import { PortalTicketFrame } from "@/features/customer-portal/components/PortalTicketFrame";
import { VehicleVisual } from "@/features/customer-portal/components/VehicleVisual";
import { parsePortalDate } from "@/features/customer-portal/utils/portalDate";
import { requestPasswordReset } from "@/features/auth/api/authApi";
import { useAuth } from "@/core/auth/useAuth";
import { completeMyAccountProfile, uploadMyAccountAvatar, type UpdateAccountProfileRequest } from "@/features/iam/api/accountProfileApi";
import { mergeCurrentUserWithAccountProfile } from "@/features/iam/utils/accountProfileMapper";
import { subscribeNotificationReceived } from "@/features/notifications/utils/notificationEvents";
import {
  fetchMyOnboardingApproval,
  resubmitMyOnboardingApproval,
  type OnboardingApprovalResponse,
} from "@/features/iam/api/onboardingApprovalApi";
import { Modal } from "@/shared/components/ui/Modal";
import { formatInApplicationTime } from "@/shared/time/applicationTime";

import { CustomerPortalLayout } from "./PortalShared";

type ProfileForm = {
  address: string;
  dateOfBirth: string;
  fullName: string;
  gender: string;
  identifyCard: string;
  phoneNumber: string;
};

const emptyForm: ProfileForm = {
  address: "",
  dateOfBirth: "",
  fullName: "",
  gender: "",
  identifyCard: "",
  phoneNumber: "",
};

function initials(name?: string | null) {
  const words = name?.trim().split(/\s+/).filter(Boolean) ?? [];
  if (words.length === 0) return "KH";
  return words.slice(-2).map((word) => word[0]).join("").toUpperCase();
}

function formatDate(value?: string | null) {
  if (!value) return "--";
  const date = parsePortalDate(value);
  if (!date) return "--";
  return formatInApplicationTime(date, "vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
}

function compactCode(value?: string | null) {
  if (!value) return "--";
  return value.length > 18 ? `${value.slice(0, 8)}...${value.slice(-6)}` : value;
}

function accountStatusLabel(status?: string | null) {
  if (status === "ACTIVE") return "Đang hoạt động";
  if (status === "INACTIVE") return "Ngừng hoạt động";
  if (status === "SUSPENDED") return "Tạm khóa";
  if (status === "PENDING") return "Chờ kích hoạt";
  return status || "--";
}

function approvalStatusLabel(status?: string | null) {
  if (status === "APPROVED") return "Đã phê duyệt";
  if (status === "PENDING") return "Chờ phê duyệt";
  if (status === "REJECTED") return "Cần bổ sung";
  return status || "--";
}

function profileStatusLabel(status?: string | null) {
  if (status === "ACTIVE") return "Đã kích hoạt";
  if (status === "INACTIVE") return "Ngừng hoạt động";
  if (status === "SUSPENDED") return "Tạm khóa";
  if (status === "PENDING") return "Chờ kích hoạt";
  return status || "--";
}

function profileToForm(profile: CustomerPortalProfile): ProfileForm {
  return {
    address: profile.profile?.address ?? "",
    dateOfBirth: profile.profile?.dateOfBirth ?? "",
    fullName: profile.profile?.fullName ?? "",
    gender: profile.profile?.gender ?? "",
    identifyCard: profile.profile?.identifyCard ?? "",
    phoneNumber: profile.profile?.phoneNumber ?? "",
  };
}

function buildProfilePayload(form: ProfileForm): UpdateAccountProfileRequest {
  return {
    address: form.address.trim() || undefined,
    dateOfBirth: form.dateOfBirth || undefined,
    fullName: form.fullName.trim() || undefined,
    gender: form.gender.trim() || undefined,
    identifyCard: form.identifyCard.trim() || undefined,
    phoneNumber: form.phoneNumber.trim() || undefined,
  };
}

export function ProfilePage() {
  const { setUser } = useAuth();
  const [profile, setProfile] = useState<CustomerPortalProfile | null>(null);
  const [vehicles, setVehicles] = useState<CustomerPortalVehicle[]>([]);
  const [subscriptions, setSubscriptions] = useState<CustomerPortalSubscription[]>([]);
  const [ticketTypes, setTicketTypes] = useState<CustomerPortalTicketType[]>([]);
  const [vehicleTypes, setVehicleTypes] = useState<CustomerPortalVehicleType[]>([]);
  const [latestApproval, setLatestApproval] = useState<OnboardingApprovalResponse | null>(null);
  const [form, setForm] = useState<ProfileForm>(emptyForm);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [passwordSending, setPasswordSending] = useState(false);
  const [passwordError, setPasswordError] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [resubmitting, setResubmitting] = useState(false);
  const [avatarUploading, setAvatarUploading] = useState(false);
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const avatarInputRef = useRef<HTMLInputElement>(null);
  const verificationRef = useRef<HTMLElement>(null);

  useEffect(() => {
    let ignore = false;

    async function loadProfile() {
      setLoading(true);
      setError("");
      try {
        const nextProfile = await getCustomerPortalProfile();
        if (ignore) return;
        setProfile(nextProfile);
        setForm(profileToForm(nextProfile));
        setUser((currentUser) => (currentUser ? mergeCurrentUserWithAccountProfile(currentUser, nextProfile) : currentUser));
        if (nextProfile.customer?.customerId) {
          const [nextVehicles, nextSubscriptions, lookups] = await Promise.all([
            getMyCustomerVehicles(nextProfile),
            getMySubscriptions(nextProfile),
            getCustomerPortalLookups(),
          ]);
          if (ignore) return;
          setVehicles(nextVehicles);
          setSubscriptions(nextSubscriptions);
          setTicketTypes(lookups.ticketTypes);
          setVehicleTypes(lookups.vehicleTypes);
        }
        try {
          const approval = await fetchMyOnboardingApproval("customer");
          if (!ignore) setLatestApproval(approval);
        } catch {
          if (!ignore) setLatestApproval(null);
        }
      } catch (requestError) {
        if (!ignore) setError(requestError instanceof Error ? requestError.message : "Không thể tải hồ sơ khách hàng.");
      } finally {
        if (!ignore) setLoading(false);
      }
    }

    void loadProfile();
    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => subscribeNotificationReceived((notification) => {
    if (!["CUSTOMER_ONBOARDING_APPROVED", "CUSTOMER_ONBOARDING_REJECTED"].includes(notification.notificationType)) return;

    void getCustomerPortalProfile().then(async (nextProfile) => {
      setProfile(nextProfile);
      setForm(profileToForm(nextProfile));
      setUser((currentUser) => currentUser
        ? mergeCurrentUserWithAccountProfile(currentUser, nextProfile)
        : currentUser);
      try {
        setLatestApproval(await fetchMyOnboardingApproval("customer"));
      } catch {
        setLatestApproval(null);
      }
    });
  }), [setUser]);

  const displayName = form.fullName || profile?.account?.username || "Khách hàng";
  const avatarUrl = profile?.profile?.avatarUrl?.trim();
  const email = profile?.account?.email ?? "--";
  const customerCode = profile?.customer?.customerCode ?? "--";
  const displayCustomerCode = compactCode(customerCode);
  const customerStatus = profile?.customer?.customerStatus ?? "--";
  // The customer record is updated atomically by the approval workflow, while the
  // latest request is informational and can be stale after a resubmission.
  const customerApprovalStatus = profile?.customer?.customerApprovalStatus;
  const approvalStatus = customerApprovalStatus ?? "--";
  const accountStatus = profile?.account?.accountStatus ?? "--";
  const isOnboardingRequired = Boolean(profile?.onboardingRequired || (profile && !profile.profile?.userProfileId));
  const profileStatus = profile?.profile?.userProfileStatus ?? "--";
  const isApproved = approvalStatus === "APPROVED";
  const formChanged = useMemo(() => {
    if (!profile) return false;
    return JSON.stringify(form) !== JSON.stringify(profileToForm(profile));
  }, [form, profile]);
  const approvalRequestStatus = latestApproval?.request?.approvalRequestStatus;
  const effectiveApprovalStatus = customerApprovalStatus ?? approvalRequestStatus;
  const isPending = effectiveApprovalStatus === "PENDING";
  const isRejected = effectiveApprovalStatus === "REJECTED";
  const showPendingOrRequiredOnboardingBanner = isOnboardingRequired || (isPending && !isRejected);
  const rejectionNote = latestApproval?.request?.note?.trim();
  const saveButtonDisabled = saving || resubmitting || avatarUploading || !profile || (!isOnboardingRequired && !formChanged);
  const currentApprovalStatus = effectiveApprovalStatus ?? "--";
  const showSaveProfileButton = currentApprovalStatus === "APPROVED";
  const showUndoButton = showSaveProfileButton || formChanged;
  const saveButtonLabel = saving ? "Đang lưu..." : "Lưu thay đổi";
  const activeSubscription = subscriptions.find((subscription) => subscription.status === "ACTIVE");
  const ticketEndDate = parsePortalDate(activeSubscription?.effectiveTo);
  const ticketDaysRemaining = ticketEndDate ? Math.max(0, Math.ceil((ticketEndDate.getTime() - Date.now()) / 86_400_000)) : null;
  const pendingSubscriptionCount = subscriptions.filter((subscription) => subscription.status.startsWith("PENDING")).length;
  const defaultVehicle = vehicles.find((vehicle) => vehicle.isDefault);
  const vehicleTypeById = useMemo(() => new Map(vehicleTypes.map((type) => [type.vehicleTypeId, type])), [vehicleTypes]);
  const defaultVehicleTypeName = defaultVehicle?.vehicleTypeId ? vehicleTypeById.get(defaultVehicle.vehicleTypeId)?.name : undefined;
  const ticketName = activeSubscription ? ticketTypes.find((item) => item.ticketTypeId === activeSubscription.ticketTypeId)?.name ?? "Vé tháng CoParking" : "Chưa có vé tháng";
  const panelClass = "tw-overflow-hidden tw-rounded-xl tw-border tw-border-solid tw-border-[#e0e8f3] tw-bg-white tw-p-3.5 tw-shadow-[0_4px_12px_rgba(15,23,42,0.04)]";
  const panelTitleClass = "tw-m-0 tw-mb-2 tw-flex tw-items-center tw-gap-3 tw-text-base tw-font-bold tw-text-[#14213d] [&>i]:tw-text-[#0d62de]";

  const applyProfile = (nextProfile: CustomerPortalProfile) => {
    setProfile(nextProfile);
    setForm(profileToForm(nextProfile));
    setUser((currentUser) => (currentUser ? mergeCurrentUserWithAccountProfile(currentUser, nextProfile) : currentUser));
  };

  const handleAvatarChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    if (!file.type.startsWith("image/")) {
      setError("Vui lòng chọn tệp hình ảnh hợp lệ.");
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setError("Ảnh đại diện không được vượt quá 5 MB.");
      return;
    }

    const previewUrl = URL.createObjectURL(file);
    setAvatarPreviewUrl(previewUrl);
    setAvatarUploading(true);
    setError("");
    setNotice("");
    try {
      const response = await uploadMyAccountAvatar(file);
      // Updating the avatar must preserve any unsaved personal information.
      setProfile(response.data);
      setUser((currentUser) => (currentUser ? mergeCurrentUserWithAccountProfile(currentUser, response.data) : currentUser));
      setNotice("Đã cập nhật ảnh đại diện.");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể cập nhật ảnh đại diện.");
    } finally {
      URL.revokeObjectURL(previewUrl);
      setAvatarPreviewUrl("");
      setAvatarUploading(false);
    }
  };

  const saveProfileChanges = async () => {
    const payload = buildProfilePayload(form);
    const shouldCompleteOnboarding = Boolean(profile?.onboardingRequired || !profile?.profile?.userProfileId);
    const updatedProfile = shouldCompleteOnboarding
      ? (await completeMyAccountProfile(payload)).data
      : await updateCustomerPortalProfile(payload);
    applyProfile(updatedProfile);
    return {
      profile: updatedProfile,
      shouldCompleteOnboarding,
    };
  };

  const handleSave = async () => {
    setSaving(true);
    setError("");
    setNotice("");
    try {
      const result = await saveProfileChanges();
      if (result.shouldCompleteOnboarding) {
        try {
          const approval = await fetchMyOnboardingApproval("customer");
          setLatestApproval(approval);
        } catch {
          setLatestApproval(null);
        }
      }
      setNotice(result.shouldCompleteOnboarding ? "Đã gửi hồ sơ để chờ duyệt." : "Đã cập nhật hồ sơ.");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể cập nhật hồ sơ.");
    } finally {
      setSaving(false);
    }
  };

  const handleResubmitApproval = async () => {
    setResubmitting(true);
    setError("");
    setNotice("");
    try {
      if (formChanged) {
        await saveProfileChanges();
      }
      const approval = await resubmitMyOnboardingApproval("customer");
      const nextProfile = await getCustomerPortalProfile();
      setLatestApproval(approval);
      applyProfile(nextProfile);
      setNotice("Đã gửi lại hồ sơ để chờ duyệt.");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể gửi lại hồ sơ để duyệt.");
    } finally {
      setResubmitting(false);
    }
  };

  const closePasswordModal = () => {
    setPasswordOpen(false);
    setPasswordError("");
  };

  const handleSendPasswordReset = async () => {
    if (!profile?.account?.email) {
      setPasswordError("Tài khoản hiện tại chưa có email để gửi liên kết đổi mật khẩu.");
      return;
    }

    setPasswordSending(true);
    setPasswordError("");
    try {
      await requestPasswordReset({ email: profile.account.email });
      closePasswordModal();
      setNotice("Đã gửi liên kết đổi mật khẩu đến email đăng nhập.");
    } catch (requestError) {
      setPasswordError(requestError instanceof Error ? requestError.message : "Không thể gửi liên kết đổi mật khẩu.");
    } finally {
      setPasswordSending(false);
    }
  };

  return (
    <CustomerPortalLayout>
      {error ? <div className="vm-info-note tw-bg-red-50 tw-text-red-600"><i className="fas fa-exclamation-circle" /> {error}</div> : null}
      {notice ? <div className="vm-info-note tw-bg-green-50 tw-text-green-700"><i className="fas fa-check-circle" /> {notice}</div> : null}
      {showPendingOrRequiredOnboardingBanner ? (
        <div className="vm-info-note tw-items-start tw-justify-between tw-gap-4 tw-bg-amber-50 tw-text-amber-800">
          <div className="tw-flex tw-min-w-0 tw-gap-3">
            <i className={`fas ${isOnboardingRequired ? "fa-exclamation-circle" : "fa-clock"} tw-mt-1`} />
            <div>
              <strong className="tw-block tw-text-vm-slate-900">
                {isOnboardingRequired ? "Cần hoàn tất hồ sơ" : "Hồ sơ đang chờ duyệt"}
              </strong>
              <span className="tw-mt-1 tw-block">
                {isOnboardingRequired
                  ? "Bổ sung thông tin bắt buộc rồi gửi hồ sơ để nhân viên phê duyệt."
                  : "Thông tin đã được gửi và đang chờ nhân viên phụ trách phê duyệt."}
              </span>
            </div>
          </div>
          {isOnboardingRequired ? (
            <button
              className="tw-inline-flex tw-min-h-10 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-vm-primary tw-px-4 tw-text-[0.88rem] tw-font-semibold tw-text-white tw-shadow-[0_10px_18px_rgba(37,99,235,0.18)] disabled:tw-cursor-not-allowed disabled:tw-opacity-60"
              type="button"
              disabled={saveButtonDisabled}
              onClick={handleSave}
            >
              {saving ? "Đang gửi..." : "Gửi hồ sơ"}
            </button>
          ) : null}
        </div>
      ) : null}
      {isRejected ? (
        <div className="vm-info-note tw-items-start tw-justify-between tw-gap-4 tw-bg-amber-50 tw-text-amber-800">
          <div className="tw-flex tw-min-w-0 tw-gap-3">
            <i className="fas fa-exclamation-circle tw-mt-1" />
            <div>
              <strong className="tw-block tw-text-vm-slate-900">Hồ sơ cần bổ sung</strong>
              <span className="tw-mt-1 tw-block">Cập nhật thông tin theo góp ý rồi gửi lại để chờ duyệt.</span>
              {rejectionNote ? <span className="tw-mt-1 tw-block tw-font-semibold">Lý do: {rejectionNote}</span> : null}
            </div>
          </div>
          <button
            className="tw-inline-flex tw-min-h-10 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-vm-primary tw-px-4 tw-text-[0.88rem] tw-font-semibold tw-text-white tw-shadow-[0_10px_18px_rgba(37,99,235,0.18)] disabled:tw-cursor-not-allowed disabled:tw-opacity-60"
            type="button"
            disabled={saving || resubmitting || !profile}
            onClick={handleResubmitApproval}
          >
            {resubmitting ? "Đang gửi..." : "Gửi duyệt lại"}
          </button>
        </div>
      ) : null}

      <section aria-label="Thông tin hồ sơ" className="tw-relative tw-isolate tw-overflow-hidden tw-rounded-[10px] tw-border tw-border-solid tw-border-[#28416b] tw-bg-[#03143b] tw-px-7 tw-py-4 tw-text-white tw-shadow-sm max-[1100px]:tw-px-5">
        <div aria-hidden="true" className="tw-pointer-events-none tw-absolute tw-inset-y-0 tw-left-[37%] tw-right-[23%] tw-overflow-hidden max-[1000px]:tw-left-[32%] max-[760px]:tw-left-0 max-[760px]:tw-right-0 max-[760px]:tw-opacity-30">
          <img alt="" className="tw-h-full tw-w-full tw-object-cover tw-object-center [transform:scale(1.18)] [mask-image:linear-gradient(90deg,transparent,black_12%,black_88%,transparent)]" src="/assets/customer/portal/profile-parking-banner-v1.png" />
        </div>
        <div className="tw-relative tw-grid tw-grid-cols-[132px_minmax(0,1fr)_280px] tw-items-center tw-gap-7 max-[1100px]:tw-grid-cols-[112px_minmax(0,1fr)_256px] max-[1100px]:tw-gap-5 max-[760px]:tw-grid-cols-[88px_minmax(0,1fr)] max-[760px]:tw-gap-4">
          <input ref={avatarInputRef} className="tw-sr-only" accept="image/png,image/jpeg,image/webp" type="file" onChange={handleAvatarChange} />
          <button
            aria-label={avatarUploading ? "Đang cập nhật ảnh đại diện" : "Đổi ảnh đại diện"}
            className="tw-relative tw-grid tw-h-[132px] tw-w-[132px] tw-place-items-center tw-overflow-hidden tw-rounded-full tw-border-[3px] tw-border-solid tw-border-white/95 tw-bg-[#dceafe] tw-p-0 tw-text-3xl tw-font-bold tw-text-[#0759d8] tw-shadow-lg focus-visible:tw-outline focus-visible:tw-outline-2 focus-visible:tw-outline-offset-4 focus-visible:tw-outline-white disabled:tw-cursor-wait max-[1100px]:tw-h-[112px] max-[1100px]:tw-w-[112px] max-[760px]:tw-h-[88px] max-[760px]:tw-w-[88px]"
            disabled={avatarUploading || saving || resubmitting || loading || !profile}
            title="Đổi ảnh đại diện"
            type="button"
            onClick={() => avatarInputRef.current?.click()}
          >
            {avatarPreviewUrl || avatarUrl ? <img alt="Ảnh đại diện" className="tw-h-full tw-w-full tw-object-cover" src={avatarPreviewUrl || avatarUrl} /> : initials(displayName)}
          </button>
          <div className="tw-min-w-0 tw-self-center">
            <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-x-4 tw-gap-y-2">
              <h1 className="tw-m-0 !tw-font-[Cambria,Georgia,serif] tw-text-[1.5rem] tw-font-bold tw-leading-tight tw-text-white">{loading ? "Đang tải..." : displayName}</h1>
              <span className="tw-inline-flex tw-items-center tw-gap-1.5 tw-rounded-lg tw-border tw-border-solid tw-border-[#a99359]/60 tw-bg-[#243347]/80 tw-px-3 tw-py-1.5 tw-text-[0.72rem] tw-font-medium tw-leading-none tw-text-[#e4c56f]">
                <i aria-hidden="true" className={profile?.customer?.customerType === "VIP" ? "fas fa-crown" : "fas fa-user-check"} />
                {profile?.customer?.customerType === "VIP" ? "Khách hàng VIP" : "Khách hàng"}
              </span>
            </div>
            <div className="tw-mt-3 tw-grid tw-gap-1.5 tw-text-[0.82rem] tw-font-normal tw-leading-5 tw-text-[#e1e9f8]">
              <span className="tw-flex tw-min-w-0 tw-items-center tw-gap-2.5"><i aria-hidden="true" className="far fa-envelope tw-w-3.5 tw-shrink-0" /><span className="tw-truncate" title={email}>{email}</span></span>
              <span className="tw-flex tw-min-w-0 tw-items-center tw-gap-2.5"><i aria-hidden="true" className="fas fa-phone-alt tw-w-3.5 tw-shrink-0" /><span className="tw-truncate">{form.phoneNumber || "Chưa cập nhật số điện thoại"}</span></span>
              <span className="tw-flex tw-min-w-0 tw-items-center tw-gap-2.5"><i aria-hidden="true" className="fas fa-map-marker-alt tw-w-3.5 tw-shrink-0" /><span className="tw-truncate" title={form.address}>{form.address || "Chưa cập nhật địa chỉ"}</span></span>
            </div>
          </div>
          <div className="tw-grid tw-min-h-[120px] tw-grid-cols-[minmax(0,.9fr)_minmax(0,1.1fr)] tw-content-between tw-gap-x-4 tw-gap-y-5 tw-border-0 tw-border-l tw-border-solid tw-border-white/10 tw-pl-5 tw-text-[0.75rem] max-[760px]:tw-col-span-2 max-[760px]:tw-min-h-0 max-[760px]:tw-border-l-0 max-[760px]:tw-border-t max-[760px]:tw-pl-0 max-[760px]:tw-pt-4">
            <div className="tw-col-span-2">
              <span className="tw-block tw-text-[#e1e9f8]">Trạng thái tài khoản</span>
              <strong className={`tw-mt-1.5 tw-flex tw-items-center tw-gap-2 tw-text-[1.1rem] tw-font-semibold ${accountStatus === "ACTIVE" && isApproved ? "tw-text-[#53d38b]" : "tw-text-amber-200"}`}>
                <i aria-hidden="true" className={accountStatus === "ACTIVE" && isApproved ? "far fa-check-circle" : "far fa-clock"} />
                {accountStatus === "ACTIVE" ? (isApproved ? "Đã xác minh" : approvalStatusLabel(currentApprovalStatus)) : accountStatusLabel(accountStatus)}
              </strong>
            </div>
            <div className="tw-min-w-0">
              <span className="tw-block tw-text-[#e1e9f8]">Thành viên từ</span>
              <span className="tw-mt-1.5 tw-block tw-text-[0.82rem]">Chưa cập nhật</span>
            </div>
            <div className="tw-min-w-0">
              <span className="tw-block tw-text-[#e1e9f8]">Mã khách hàng</span>
              <span className="tw-mt-1.5 tw-block tw-break-words tw-text-[0.82rem]" title={customerCode}>{displayCustomerCode}</span>
            </div>
          </div>
        </div>
      </section>

      <section className={`${panelClass} tw-mt-4`}><h2 className={panelTitleClass}><i className="far fa-file-alt" /> Thông tin cá nhân</h2><div className="tw-grid tw-grid-cols-3 tw-gap-x-6 tw-gap-y-2 [&>label]:tw-min-w-0 max-[640px]:tw-grid-cols-1"><label className="tw-grid tw-gap-1.5 tw-text-[0.76rem] tw-font-semibold tw-text-[#334a6e]">Họ và tên<input className="tw-h-8 tw-w-full tw-rounded-md tw-border tw-border-solid tw-border-[#dce4ef] tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-[#223554]" value={form.fullName} onChange={(event) => setForm((current) => ({ ...current, fullName: event.target.value }))} /></label><label className="tw-grid tw-gap-1.5 tw-text-[0.76rem] tw-font-semibold tw-text-[#334a6e]">Số điện thoại<input className="tw-h-8 tw-w-full tw-rounded-md tw-border tw-border-solid tw-border-[#dce4ef] tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-[#223554]" value={form.phoneNumber} onChange={(event) => setForm((current) => ({ ...current, phoneNumber: event.target.value }))} /></label><label className="tw-grid tw-gap-1.5 tw-text-[0.76rem] tw-font-semibold tw-text-[#334a6e]">Ngày sinh<input className="tw-h-8 tw-w-full tw-rounded-md tw-border tw-border-solid tw-border-[#dce4ef] tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-[#223554]" type="date" value={form.dateOfBirth} onChange={(event) => setForm((current) => ({ ...current, dateOfBirth: event.target.value }))} /></label><label className="tw-grid tw-gap-1.5 tw-text-[0.76rem] tw-font-semibold tw-text-[#334a6e]">Email<input className="tw-h-8 tw-w-full tw-rounded-md tw-border tw-border-solid tw-border-[#dce4ef] tw-bg-slate-50 tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-[#71819a]" value={email} readOnly /></label><label className="tw-grid tw-gap-1.5 tw-text-[0.76rem] tw-font-semibold tw-text-[#334a6e]">Giới tính<select className="tw-h-8 tw-w-full tw-rounded-md tw-border tw-border-solid tw-border-[#dce4ef] tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-[#223554]" value={form.gender} onChange={(event) => setForm((current) => ({ ...current, gender: event.target.value }))}><option value="">Chưa chọn</option><option value="Nam">Nam</option><option value="Nữ">Nữ</option><option value="Khác">Khác</option></select></label><label className="tw-grid tw-gap-1.5 tw-text-[0.76rem] tw-font-semibold tw-text-[#334a6e]">CMND/CCCD<input className="tw-h-8 tw-w-full tw-rounded-md tw-border tw-border-solid tw-border-[#dce4ef] tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-[#223554]" value={form.identifyCard} onChange={(event) => setForm((current) => ({ ...current, identifyCard: event.target.value }))} /></label><label className="tw-col-span-3 max-[640px]:tw-col-span-1 tw-grid tw-gap-1.5 tw-text-[0.76rem] tw-font-semibold tw-text-[#334a6e]">Địa chỉ<input className="tw-h-8 tw-w-full tw-rounded-md tw-border tw-border-solid tw-border-[#dce4ef] tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-[#223554]" value={form.address} onChange={(event) => setForm((current) => ({ ...current, address: event.target.value }))} /></label></div>
        <div className="tw-mt-4 tw-grid tw-grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)_auto] tw-items-end tw-gap-6 max-[1100px]:tw-grid-cols-2 max-[640px]:tw-grid-cols-1">
          <div className="tw-min-w-0">
            <span className="tw-block tw-text-[0.76rem] tw-font-semibold tw-text-[#334a6e]">Ảnh đại diện</span>
            <div className="tw-mt-1.5 tw-flex tw-flex-wrap tw-items-center tw-gap-x-6 tw-gap-y-2">
              <p id="profile-avatar-help" className="tw-m-0 tw-text-[0.68rem] tw-leading-4 tw-text-[#71819a]">Định dạng JPG, PNG, WebP.<br />Dung lượng tối đa 5 MB.</p>
              <button
                aria-describedby="profile-avatar-help"
                className="tw-inline-flex tw-min-h-8 tw-shrink-0 tw-items-center tw-justify-center tw-gap-2 tw-rounded-md tw-border tw-border-solid tw-border-[#dce4ef] tw-bg-white tw-px-3 tw-text-[0.76rem] tw-font-semibold tw-text-[#0759d8] hover:tw-bg-blue-50 disabled:tw-cursor-not-allowed disabled:tw-opacity-60"
                disabled={avatarUploading || saving || resubmitting || loading || !profile}
                type="button"
                onClick={() => avatarInputRef.current?.click()}
              >
                <i aria-hidden="true" className={avatarUploading ? "fas fa-spinner fa-spin" : "fas fa-upload"} />
                {avatarUploading ? "Đang tải ảnh..." : "Đổi ảnh"}
              </button>
            </div>
          </div>
          <div className="tw-min-w-0">
            <span className="tw-block tw-text-[0.76rem] tw-font-semibold tw-text-[#334a6e]">Chữ ký (tùy chọn)</span>
            <div className="tw-mt-1.5 tw-flex tw-flex-wrap tw-items-center tw-gap-x-6 tw-gap-y-2">
              <p id="profile-signature-help" className="tw-m-0 tw-text-[0.68rem] tw-leading-4 tw-text-[#71819a]">Định dạng JPG, PNG.<br />Dung lượng tối đa 2 MB.</p>
              <button
                aria-describedby="profile-signature-help"
                className="tw-inline-flex tw-min-h-8 tw-shrink-0 tw-cursor-not-allowed tw-items-center tw-justify-center tw-gap-2 tw-rounded-md tw-border tw-border-solid tw-border-[#dce4ef] tw-bg-slate-50 tw-px-3 tw-text-[0.76rem] tw-font-semibold tw-text-[#71819a]"
                disabled
                title="Chức năng tải chữ ký chưa khả dụng"
                type="button"
              >
                <i aria-hidden="true" className="fas fa-upload" /> Tải lên chữ ký
              </button>
            </div>
          </div>
          <div className="tw-flex tw-flex-wrap tw-justify-end tw-gap-3 max-[1100px]:tw-col-span-2 max-[640px]:tw-col-span-1">{showUndoButton ? <button className="tw-min-h-8 tw-rounded-md tw-border tw-border-solid tw-border-[#b8cae4] tw-bg-white tw-px-4 tw-text-[0.78rem] tw-font-semibold tw-text-[#334a6e] disabled:tw-opacity-60" type="button" disabled={!profile || saving || resubmitting || !formChanged} onClick={() => profile && setForm(profileToForm(profile))}>Hủy thay đổi</button> : null}{showSaveProfileButton ? <button className="tw-min-h-8 tw-rounded-md tw-border-0 tw-bg-[linear-gradient(135deg,#146cf3,#0756d8)] tw-px-5 tw-text-[0.78rem] tw-font-semibold tw-text-white tw-shadow-[0_10px_18px_rgba(20,99,230,0.18)] disabled:tw-opacity-60" type="button" disabled={saveButtonDisabled} onClick={handleSave}>{saveButtonLabel}</button> : null}</div>
        </div>
      </section>

      <div className="tw-mt-4 tw-grid tw-grid-cols-[minmax(0,1fr)_minmax(0,1.05fr)_minmax(0,1.2fr)] tw-gap-3 max-[900px]:tw-grid-cols-1"><section className={panelClass}><h2 className={panelTitleClass}><i className="far fa-check-circle" /> Trạng thái tài khoản</h2><strong className={`tw-text-[1.05rem] ${isApproved ? "tw-text-[#078553]" : "tw-text-amber-700"}`}><i className="fas fa-check-circle tw-mr-2" />{approvalStatusLabel(approvalStatus)}</strong><p className="tw-mb-2 tw-mt-2 tw-text-[0.76rem] tw-font-semibold tw-text-[#71819a]">{isApproved ? "Hồ sơ khách hàng đã được phê duyệt." : "Theo dõi trạng thái và bổ sung hồ sơ theo yêu cầu."}</p><button className="tw-inline-block tw-rounded-md tw-border tw-border-solid tw-border-[#b8cae4] tw-bg-white tw-px-3 tw-py-1.5 tw-text-[0.78rem] tw-font-semibold tw-text-[#0759d8]" type="button" onClick={() => verificationRef.current?.scrollIntoView({ behavior: "smooth", block: "center" })}>Xem chi tiết xác minh <i className="fas fa-arrow-right tw-ml-2" /></button></section><section className={panelClass}><h2 className={panelTitleClass}><i className="fas fa-lock" /> Bảo mật tài khoản</h2><div className="tw-grid tw-gap-2 tw-text-[0.8rem] tw-font-semibold tw-text-[#334a6e]"><div className="tw-flex tw-items-center tw-justify-between"><span><i className="fas fa-key tw-mr-2 tw-w-4" />Mật khẩu</span><button className="tw-rounded-md tw-border tw-border-solid tw-border-[#b8cae4] tw-bg-white tw-px-3 tw-py-1.5 tw-text-[0.72rem] tw-font-semibold tw-text-[#0759d8]" type="button" onClick={() => setPasswordOpen(true)}>Đổi mật khẩu</button></div><div><i className="fas fa-shield-alt tw-mr-2 tw-w-4" />Trạng thái tài khoản <span className="tw-float-right">{accountStatusLabel(accountStatus)}</span></div><div className="tw-text-xs tw-leading-5 tw-text-slate-500">Liên kết đổi mật khẩu được gửi tới email đăng nhập của bạn.</div></div></section><section className={panelClass}>
        <h2 className={panelTitleClass}><i className="fas fa-chart-bar" /> Thống kê nhanh</h2>
        <div className="tw-grid tw-grid-cols-3 tw-gap-2 tw-py-2">
          <div className="tw-grid tw-grid-cols-[24px_minmax(0,1fr)] tw-content-start tw-gap-x-2 tw-gap-y-1">
            <i className="fas fa-car-side tw-text-xl tw-text-[#1263e9]" />
            <strong className="tw-text-xl tw-font-medium tw-leading-none tw-text-[#243756]">{vehicles.length}</strong>
            <small className="tw-col-start-2 tw-text-[0.68rem] tw-text-[#71819a]">Xe đã đăng ký</small>
          </div>
          <div className="tw-grid tw-grid-cols-[24px_minmax(0,1fr)] tw-content-start tw-gap-x-2 tw-gap-y-1">
            <i className="fas fa-ticket-alt tw-text-xl tw-text-[#1263e9]" />
            <strong className="tw-text-xl tw-font-medium tw-leading-none tw-text-[#243756]">{subscriptions.filter((item) => item.status === "ACTIVE").length}</strong>
            <small className="tw-col-start-2 tw-text-[0.68rem] tw-text-[#71819a]">Vé hoạt động</small>
          </div>
          <div className="tw-grid tw-grid-cols-[24px_minmax(0,1fr)] tw-content-start tw-gap-x-2 tw-gap-y-1">
            <i className="far fa-file-alt tw-text-xl tw-text-[#1263e9]" />
            <strong className="tw-text-xl tw-font-medium tw-leading-none tw-text-[#243756]">{pendingSubscriptionCount}</strong>
            <small className="tw-col-start-2 tw-text-[0.68rem] tw-text-[#71819a]">Yêu cầu chờ</small>
          </div>
        </div>
      </section></div>

      <div className="tw-mt-4 tw-grid tw-grid-cols-[minmax(0,1fr)_minmax(0,1fr)] max-[900px]:tw-grid-cols-1 tw-gap-4"><section ref={verificationRef} className={`${panelClass} tw-scroll-mt-24`}><h2 className={panelTitleClass}><i className="far fa-file-alt" /> Tài liệu xác minh</h2><div className="tw-grid tw-gap-0"><div className="tw-grid tw-grid-cols-[28px_minmax(0,1fr)_auto] tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-[#e8edf4] tw-py-2.5"><i className={isApproved ? "fas fa-check-circle tw-text-[#0e73e8]" : "far fa-clock tw-text-amber-600"} /><span><strong className="tw-block tw-text-[0.84rem] tw-text-[#263a5d]">Thông tin cá nhân <em className={`tw-ml-2 tw-not-italic tw-text-[0.68rem] ${isApproved ? "tw-text-[#078553]" : "tw-text-amber-700"}`}>{isApproved ? "Hồ sơ đã duyệt" : "Chưa xác minh"}</em></strong><small className="tw-text-[0.72rem] tw-text-[#71819a]">Họ tên, ngày sinh, số điện thoại</small></span><i className="fas fa-chevron-down tw-text-[#71819a]" /></div><div className="tw-grid tw-grid-cols-[28px_minmax(0,1fr)_auto] tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-[#e8edf4] tw-py-3"><i className={isApproved ? "fas fa-check-circle tw-text-[#0e73e8]" : "far fa-clock tw-text-amber-600"} /><span><strong className="tw-block tw-text-[0.84rem] tw-text-[#263a5d]">CMND/CCCD <em className={`tw-ml-2 tw-not-italic tw-text-[0.68rem] ${isApproved ? "tw-text-[#078553]" : "tw-text-amber-700"}`}>{isApproved ? "Hồ sơ đã duyệt" : "Chưa xác minh"}</em></strong><small className="tw-text-[0.72rem] tw-text-[#71819a]">Số CCCD: {form.identifyCard || "--"}</small></span><i className="fas fa-chevron-down tw-text-[#71819a]" /></div><div className="tw-grid tw-grid-cols-[28px_minmax(0,1fr)_auto] tw-items-center tw-gap-3 tw-py-2.5"><i className={isApproved ? "fas fa-check-circle tw-text-[#0e73e8]" : "far fa-clock tw-text-amber-600"} /><span><strong className="tw-block tw-text-[0.84rem] tw-text-[#263a5d]">Ảnh chân dung <em className={`tw-ml-2 tw-not-italic tw-text-[0.68rem] ${isApproved ? "tw-text-[#078553]" : "tw-text-amber-700"}`}>{isApproved ? "Hồ sơ đã duyệt" : "Chưa xác minh"}</em></strong><small className="tw-text-[0.72rem] tw-text-[#71819a]">{avatarUrl ? "Đã cập nhật ảnh đại diện" : "Chưa cập nhật ảnh đại diện"}</small></span><i className="fas fa-chevron-down tw-text-[#71819a]" /></div></div></section>
        <div className="tw-grid tw-content-start tw-gap-2.5">
          <section className={`${panelClass} !tw-px-3 !tw-py-2.5`} aria-label="Xe mặc định">
            <div className="tw-flex tw-items-center tw-justify-between tw-gap-2">
              <h2 className={`${panelTitleClass} !tw-mb-1 !tw-text-[0.88rem]`}><i className="fas fa-car-side" /> Xe mặc định</h2>
              <Link className="tw-mb-1 tw-text-[0.68rem] tw-text-[#0759d8]" to="/customer/vehicles">Quản lý xe</Link>
            </div>
            {defaultVehicle ? (
              <div className="tw-grid tw-grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)] tw-items-center tw-gap-3 max-[540px]:tw-grid-cols-1">
                <div className="tw-grid tw-min-w-0 tw-grid-cols-[112px_minmax(0,1fr)] tw-items-center tw-gap-2 [&>span[role=img]]:tw-h-[58px] [&>span[role=img]]:tw-w-[112px]">
                  <VehicleVisual color={defaultVehicle.color} typeName={defaultVehicleTypeName} />
                  <div className="tw-min-w-0">
                    <strong className="tw-block tw-text-[1rem] tw-leading-5 tw-text-[#1a2945]">{defaultVehicle.licensePlate}</strong>
                    <small className="tw-mt-1 tw-block tw-text-[0.68rem] tw-leading-4 tw-text-[#71819a]">{[defaultVehicle.brand, defaultVehicle.color].filter(Boolean).join(" · ") || "Chưa cập nhật hãng, màu xe"}</small>
                    <span className="tw-mt-1 tw-inline-block tw-rounded-sm tw-border tw-border-solid tw-border-[#ccebd8] tw-bg-[#effaf3] tw-px-1.5 tw-py-0.5 tw-text-[0.58rem] tw-leading-none tw-text-[#17824f]">Mặc định</span>
                  </div>
                </div>
                <dl className="tw-m-0 tw-grid tw-min-w-0 tw-grid-cols-[auto_minmax(0,1fr)] tw-gap-x-3 tw-gap-y-2 tw-border-0 tw-border-l tw-border-solid tw-border-[#e8edf4] tw-pl-3 tw-text-[0.68rem] tw-leading-4 tw-text-[#263a5d] max-[540px]:tw-border-l-0 max-[540px]:tw-pl-0">
                  <dt className="tw-font-normal">Loại xe</dt>
                  <dd className="tw-m-0 tw-text-right">{defaultVehicleTypeName || "Chưa cập nhật"}</dd>
                  <dt className="tw-font-normal">Ngày thêm</dt>
                  <dd className="tw-m-0 tw-text-right">{formatDate(defaultVehicle.createdAt)}</dd>
                </dl>
              </div>
            ) : <p className="tw-m-0 tw-py-4 tw-text-[0.8rem] tw-text-[#71819a]">{loading ? "Đang tải phương tiện..." : "Chưa chọn xe mặc định."}</p>}
          </section>
          <section className={`${panelClass} !tw-px-3 !tw-py-2.5`} aria-label="Vé tháng hiện tại">
            <div className="tw-flex tw-items-center tw-justify-between tw-gap-2">
              <h2 className={`${panelTitleClass} !tw-mb-1.5 !tw-text-[0.88rem]`}><i className="far fa-calendar-alt" /> Vé tháng hiện tại</h2>
              <Link className="tw-mb-1.5 tw-text-[0.68rem] tw-text-[#0759d8]" to="/customer/subscriptions">Xem tất cả</Link>
            </div>
            {activeSubscription ? (
              <>
                <div className="tw-relative tw-isolate tw-text-white">
                  <PortalTicketFrame variant="compact" />
                  <div className="tw-relative tw-grid tw-min-h-[78px] tw-grid-cols-[minmax(0,1.6fr)_minmax(145px,1fr)_minmax(62px,.5fr)] tw-items-center tw-px-5 tw-py-2.5 max-[540px]:tw-grid-cols-[minmax(0,1fr)_80px] max-[540px]:tw-gap-y-3">
                    <div className="tw-min-w-0 tw-pr-3 max-[540px]:tw-col-span-2">
                      <strong className="tw-block tw-text-[0.86rem] tw-font-semibold tw-leading-5 tw-text-[#f1d178]">{ticketName}</strong>
                      <span className="tw-mt-0.5 tw-flex tw-items-center tw-gap-1.5 tw-text-[0.64rem] tw-leading-4 tw-text-[#e0e8f7]"><i aria-hidden="true" className="fas fa-map-marker-alt" /> Chưa cập nhật bãi xe</span>
                      <span className="tw-block tw-text-[0.64rem] tw-leading-4 tw-text-[#e0e8f7]">Chưa cập nhật địa chỉ bãi xe</span>
                    </div>
                    <div className="tw-min-w-0 tw-border-0 tw-border-l tw-border-solid tw-border-white/15 tw-px-3 max-[540px]:tw-border-l-0 max-[540px]:tw-pl-0">
                      <small className="tw-block tw-text-[0.6rem] tw-leading-4 tw-text-[#c7d6ee]">Hiệu lực</small>
                      <span className="tw-mt-1 tw-block tw-text-[0.65rem] tw-leading-4">{formatDate(activeSubscription.effectiveFrom)} – {formatDate(activeSubscription.effectiveTo)}</span>
                    </div>
                    <div className="tw-border-0 tw-border-l tw-border-solid tw-border-white/15 tw-pl-3">
                      <small className="tw-block tw-text-[0.6rem] tw-leading-4 tw-text-[#c7d6ee]">Còn lại</small>
                      <strong className="tw-mt-1 tw-block tw-whitespace-nowrap tw-text-[0.8rem] tw-font-semibold tw-leading-4 tw-text-[#f1d178]">{ticketDaysRemaining === null ? "--" : `${ticketDaysRemaining} ngày`}</strong>
                    </div>
                  </div>
                </div>
                <Link className="tw-mt-1.5 tw-flex tw-items-center tw-justify-center tw-gap-3 tw-text-[0.72rem] tw-text-[#0759d8] hover:tw-no-underline" to={`/customer/subscriptions?subscriptionId=${encodeURIComponent(activeSubscription.subscriptionId)}`}>Gia hạn vé tháng <i aria-hidden="true" className="fas fa-arrow-right tw-text-[0.65rem]" /></Link>
              </>
            ) : <div className="tw-py-3 tw-text-center tw-text-[0.76rem] tw-text-[#71819a]">{loading ? "Đang tải vé tháng..." : "Chưa có vé tháng đang hoạt động."}<Link className="tw-mt-2 tw-block tw-text-[#0759d8]" to="/customer/subscriptions">Đăng ký vé tháng</Link></div>}
          </section>
        </div>
      </div>

      <section className="tw-mt-4 tw-flex tw-items-center tw-gap-4 tw-rounded-[11px] tw-bg-[linear-gradient(100deg,#051d4d,#072969)] tw-px-6 tw-py-4 tw-text-white"><span className="tw-grid tw-h-[42px] tw-w-[42px] tw-place-items-center tw-rounded-full tw-border tw-border-solid tw-border-[#d7ad5a] tw-text-[#f2cc76]"><i className="fas fa-headset" /></span><div><strong> Cần hỗ trợ?</strong><p className="tw-m-0 tw-mt-0.5 tw-text-[0.78rem] tw-text-[#bccce7]">Chúng tôi luôn sẵn sàng hỗ trợ bạn.</p></div><span className="tw-ml-auto tw-text-[0.78rem] tw-font-semibold tw-text-[#dceaff]"><i className="fas fa-phone-alt tw-mr-2" />Hotline: 1900 1234</span><Link className="tw-rounded-md tw-border tw-border-solid tw-border-[#d7ad5a] tw-px-4 tw-py-2 tw-text-[0.78rem] tw-font-semibold tw-text-[#f5ce7d] hover:tw-no-underline" to="/customer/support">Gửi yêu cầu hỗ trợ</Link></section>

      <Modal
        open={passwordOpen}
        title="Đổi mật khẩu"
        description="Hệ thống sẽ gửi liên kết đổi mật khẩu đến email đăng nhập của tài khoản."
        width="sm"
        onClose={closePasswordModal}
        actions={
          <div className="vm-password-modal-actions">
            <button className="vm-outline-btn" type="button" onClick={closePasswordModal}>Đóng</button>
            <button type="button" disabled={passwordSending || !profile?.account?.email} onClick={handleSendPasswordReset}>
              {passwordSending ? "Đang gửi..." : "Gửi liên kết"}
            </button>
          </div>
        }
      >
        <div className="vm-password-form">
          {passwordError ? <div className="vm-info-note tw-bg-red-50 tw-text-red-600"><i className="fas fa-exclamation-circle" /> {passwordError}</div> : null}
          <div className="vm-password-reset-card">
            <span><i className="fas fa-envelope-open-text" /></span>
            <div>
              <p>Email nhận liên kết</p>
              <strong>{profile?.account?.email ?? "--"}</strong>
            </div>
          </div>
          <div className="vm-info-note">
            <i className="fas fa-info-circle" /> Sau khi xác nhận, vui lòng kiểm tra email và làm theo hướng dẫn từ hệ thống xác thực.
          </div>
        </div>
      </Modal>
    </CustomerPortalLayout>
  );
}
