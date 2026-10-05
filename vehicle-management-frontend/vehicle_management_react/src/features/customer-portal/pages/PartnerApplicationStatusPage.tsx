import { useCallback, useEffect, useState } from "react";
import { Link, Navigate } from "react-router-dom";

import { Button } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { resendVerificationEmail } from "@/features/auth/api/authApi";
import {
  getMyPartnerRegistrationStatus,
  type PartnerApplicationStatus,
} from "@/features/iam/api/partnerRegistrationApi";
import { subscribeNotificationReceived } from "@/features/notifications/utils/notificationEvents";
import { ClientPage } from "@/shared/components/layout/ClientPage";

import { PublicFooter } from "./PortalShared";

function statusCopy(status: PartnerApplicationStatus) {
  if (!status.emailVerified) {
    return {
      icon: "fas fa-envelope-open-text",
      title: "Chờ xác minh email",
      description: "Hãy mở email từ CoParking và hoàn tất xác minh. Hồ sơ chỉ có thể được duyệt sau bước này.",
      tone: "tw-border-amber-200 tw-bg-amber-50 tw-text-amber-800",
    };
  }
  if (status.nextAction === "COMPLETE_PROFILE") {
    return {
      icon: "fas fa-user-edit",
      title: "Cần hoàn thiện hồ sơ",
      description: "Vui lòng bổ sung thông tin cá nhân, ảnh đại diện và địa chỉ trước khi hồ sơ được xét duyệt.",
      tone: "tw-border-amber-200 tw-bg-amber-50 tw-text-amber-800",
    };
  }
  if (status.approvalStatus === "REJECTED" || status.nextAction === "REVIEW_REJECTED") {
    return {
      icon: "fas fa-times-circle",
      title: "Hồ sơ chưa được duyệt",
      description: "Tài khoản vẫn ở trạng thái chờ và chưa có quyền nghiệp vụ. Vui lòng xem phản hồi của reviewer bên dưới.",
      tone: "tw-border-red-200 tw-bg-red-50 tw-text-red-800",
    };
  }
  if (status.approvalStatus === "APPROVED" && status.accountStatus === "ACTIVE") {
    return {
      icon: "fas fa-check-circle",
      title: "Hồ sơ đã được duyệt",
      description: "Organization và quyền quản trị đối tác đã được kích hoạt. Hãy vào khu vực quản trị để bắt đầu.",
      tone: "tw-border-emerald-200 tw-bg-emerald-50 tw-text-emerald-800",
    };
  }
  return {
    icon: "fas fa-hourglass-half",
    title: "Đang chờ xét duyệt",
    description: "Email đã được xác minh. Đội ngũ CoParking đang kiểm tra thông tin đơn vị của bạn.",
    tone: "tw-border-blue-200 tw-bg-blue-50 tw-text-blue-800",
  };
}

export function PartnerApplicationStatusPage() {
  const { user, setUser } = useAuth();
  const [status, setStatus] = useState<PartnerApplicationStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [resending, setResending] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const loadStatus = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await getMyPartnerRegistrationStatus();
      setStatus(response.data);
      setUser((currentUser) => currentUser ? {
        ...currentUser,
        accountStatus: response.data.accountStatus,
        partnerApplicationStatus: response.data.approvalStatus,
        partnerNextAction: response.data.nextAction,
        permissionCodes: response.data.accountStatus === "ACTIVE" ? undefined : currentUser.permissionCodes,
      } : currentUser);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể tải trạng thái hồ sơ đối tác.");
    } finally {
      setLoading(false);
    }
  }, [setUser]);

  useEffect(() => {
    if (user) void loadStatus();
  }, [loadStatus, user?.id]);

  useEffect(() => subscribeNotificationReceived((notification) => {
    const isPartnerReviewOutcome =
      notification.redirectUrl === "/partner/application-status"
      && notification.relatedSchema === "operations"
      && notification.relatedTable === "approval_requests"
      && ["ACCOUNT_STATUS_CHANGED", "SYSTEM_NOTICE"].includes(notification.notificationType);
    if (!isPartnerReviewOutcome) return;

    void loadStatus();
  }), [loadStatus]);

  if (!user) return <Navigate to="/login" replace />;
  const userEmail = user.email;

  async function resendEmail() {
    if (!userEmail || resending) return;
    setResending(true);
    setMessage("");
    setError("");
    try {
      const response = await resendVerificationEmail({ email: userEmail });
      setMessage(response.message || "Đã gửi lại email xác thực.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể gửi lại email xác thực.");
    } finally {
      setResending(false);
    }
  }

  const copy = status ? statusCopy(status) : null;

  return (
    <ClientPage>
      <main className="tw-min-h-[calc(100vh-72px)] tw-bg-[#f5f9ff] tw-py-12">
        <section className="tw-mx-auto tw-grid tw-w-[min(760px,calc(100%_-_32px))] tw-gap-5 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-8 tw-shadow-[0_20px_50px_rgba(15,55,105,.12)] max-[560px]:tw-p-5">
          <header className="tw-flex tw-items-start tw-justify-between tw-gap-4">
            <div>
              <p className="tw-m-0 tw-text-xs tw-font-black tw-uppercase tw-tracking-[.12em] tw-text-vm-primary">Đăng ký đối tác</p>
              <h1 className="tw-m-0 tw-mt-2 tw-text-2xl tw-font-black tw-text-vm-slate-900">Trạng thái hồ sơ đối tác</h1>
            </div>
            <Button loading={loading} size="sm" type="button" variant="secondary" onClick={() => void loadStatus()}>Làm mới</Button>
          </header>

          {error ? <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-4 tw-font-semibold tw-text-red-700">{error}</div> : null}
          {message ? <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-emerald-200 tw-bg-emerald-50 tw-p-4 tw-font-semibold tw-text-emerald-700">{message}</div> : null}
          {loading && !status ? <div className="tw-py-10 tw-text-center tw-font-semibold tw-text-vm-slate-500">Đang tải trạng thái hồ sơ...</div> : null}

          {status && copy ? (
            <>
              <div className={`tw-flex tw-items-start tw-gap-4 tw-rounded-vm-md tw-border tw-border-solid tw-p-5 ${copy.tone}`}>
                <i className={`${copy.icon} tw-mt-1 tw-text-2xl`} />
                <div>
                  <h2 className="tw-m-0 tw-text-lg tw-font-black">{copy.title}</h2>
                  <p className="tw-m-0 tw-mt-2 tw-leading-6">{copy.description}</p>
                </div>
              </div>

              <dl className="tw-grid tw-grid-cols-2 tw-gap-3 max-[560px]:tw-grid-cols-1">
                <div className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-4"><dt className="tw-text-xs tw-font-black tw-uppercase tw-text-vm-slate-500">Đơn vị</dt><dd className="tw-m-0 tw-mt-1 tw-font-bold tw-text-vm-slate-900">{status.organizationName}</dd></div>
                <div className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-4"><dt className="tw-text-xs tw-font-black tw-uppercase tw-text-vm-slate-500">Mã đơn vị</dt><dd className="tw-m-0 tw-mt-1 tw-font-bold tw-text-vm-slate-900">{status.organizationCode}</dd></div>
                <div className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-4"><dt className="tw-text-xs tw-font-black tw-uppercase tw-text-vm-slate-500">Tài khoản</dt><dd className="tw-m-0 tw-mt-1 tw-font-bold tw-text-vm-slate-900">{status.accountStatus}</dd></div>
                <div className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-4"><dt className="tw-text-xs tw-font-black tw-uppercase tw-text-vm-slate-500">Xét duyệt</dt><dd className="tw-m-0 tw-mt-1 tw-font-bold tw-text-vm-slate-900">{status.approvalStatus}</dd></div>
              </dl>

              {(!status.hasCompletePersonalProfile || !status.hasAvatar || !status.hasPersonalAddress || !status.hasOrganizationAddress) && (
                <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-4">
                  <strong className="tw-text-sm tw-font-black tw-text-vm-slate-900">Thiếu thông tin:</strong>
                  <ul className="tw-mt-2 tw-ml-4 tw-list-disc tw-space-y-1 tw-text-sm tw-text-vm-slate-700">
                    {!status.hasCompletePersonalProfile && <li>Thông tin cá nhân (ngày sinh, giới tính, CCCD)</li>}
                    {!status.hasAvatar && <li>Ảnh đại diện</li>}
                    {!status.hasPersonalAddress && <li>Địa chỉ liên hệ cá nhân</li>}
                    {!status.hasOrganizationAddress && <li>Địa chỉ đơn vị</li>}
                  </ul>
                </div>
              )}

              {status.reviewNote ? (
                <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-p-4">
                  <strong className="tw-text-sm tw-font-black tw-text-vm-slate-900">Phản hồi của reviewer</strong>
                  <p className="tw-m-0 tw-mt-2 tw-whitespace-pre-wrap tw-leading-6 tw-text-vm-slate-700">{status.reviewNote}</p>
                </div>
              ) : null}

              <div className="tw-flex tw-flex-wrap tw-justify-end tw-gap-3">
                {!status.emailVerified ? <Button loading={resending} type="button" variant="secondary" onClick={resendEmail}>Gửi lại email xác thực</Button> : null}
                {status.nextAction === "COMPLETE_PROFILE" ? <Link className="tw-inline-flex tw-h-10 tw-items-center tw-rounded-vm-md tw-bg-vm-primary tw-px-4 tw-font-extrabold tw-text-white hover:tw-bg-vm-primary-hover hover:tw-text-white hover:tw-no-underline" to="/partner/profile-completion">Hoàn thiện hồ sơ</Link> : null}
                {status.nextAction === "ACCESS_PARTNER_PORTAL" ? <Link className="tw-inline-flex tw-h-10 tw-items-center tw-rounded-vm-md tw-bg-vm-primary tw-px-4 tw-font-extrabold tw-text-white hover:tw-bg-vm-primary-hover hover:tw-text-white hover:tw-no-underline" to="/admin">Vào trang quản trị</Link> : null}
              </div>
            </>
          ) : null}
        </section>
      </main>
      <PublicFooter />
    </ClientPage>
  );
}
