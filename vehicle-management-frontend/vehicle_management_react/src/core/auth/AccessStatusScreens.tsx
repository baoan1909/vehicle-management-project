import { Link } from "react-router-dom";
import { Button } from "@/components/ui";
import { getAccessErrorMessage, type AccessError } from "@/core/auth/accessState";

type AccessErrorScreenProps = {
  error: AccessError;
  onLogout?: () => void;
  onRetry?: () => void;
  title?: string;
};

export function AccessErrorScreen({ error, onLogout, onRetry, title }: AccessErrorScreenProps) {
  const message = error.message?.trim() || getAccessErrorMessage(error.type);

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[1200] tw-flex tw-min-h-screen tw-w-screen tw-items-center tw-justify-center tw-bg-[linear-gradient(180deg,#f8fbff_0%,#eef5ff_100%)] tw-px-5 tw-py-6">
      <section
        aria-live="assertive"
        className="tw-w-full tw-max-w-[520px] tw-rounded-vm-md tw-border tw-border-solid tw-border-[#d9e2f2] tw-bg-white tw-px-7 tw-py-7 tw-text-center tw-shadow-[0_18px_45px_rgba(15,23,42,0.12)]"
        role="alert"
      >
        <span className="tw-mx-auto tw-flex tw-h-14 tw-w-14 tw-items-center tw-justify-center tw-rounded-full tw-bg-amber-50 tw-text-2xl tw-text-amber-600">
          <i className="fas fa-shield-alt" aria-hidden="true" />
        </span>
        <h1 className="tw-m-0 tw-mt-4 tw-text-[1.35rem] tw-font-black tw-leading-tight tw-text-vm-slate-900">
          {title ?? "Không thể xác minh quyền truy cập"}
        </h1>
        <p className="tw-mx-auto tw-mb-0 tw-mt-3 tw-max-w-[420px] tw-text-[0.92rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-600">
          {message}
        </p>
        <p className="tw-mb-0 tw-mt-3 tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-400">
          Mã trạng thái: {error.type}{error.status ? ` · HTTP ${error.status}` : ""}
        </p>
        <div className="tw-mt-6 tw-flex tw-flex-wrap tw-justify-center tw-gap-3">
          {error.retryable && onRetry ? (
            <Button onClick={onRetry}>
              <i className="fas fa-rotate-right" aria-hidden="true" />
              Thử lại
            </Button>
          ) : null}
          {onLogout ? (
            <Button variant="secondary" onClick={onLogout}>
              <i className="fas fa-right-from-bracket" aria-hidden="true" />
              Đăng nhập lại
            </Button>
          ) : null}
        </div>
      </section>
    </div>
  );
}

type NoAccessScreenProps = {
  onLogout?: () => void;
  safeLabel?: string;
  safePath?: string;
};

export function NoAccessScreen({
  onLogout,
  safeLabel = "Về hồ sơ cá nhân",
  safePath = "/admin/profile",
}: NoAccessScreenProps) {
  return (
    <div className="tw-fixed tw-inset-0 tw-z-[1200] tw-flex tw-min-h-screen tw-w-screen tw-items-center tw-justify-center tw-bg-[linear-gradient(180deg,#f8fbff_0%,#eef5ff_100%)] tw-px-5 tw-py-6">
      <section
        className="tw-w-full tw-max-w-[520px] tw-rounded-vm-md tw-border tw-border-solid tw-border-[#d9e2f2] tw-bg-white tw-px-7 tw-py-7 tw-text-center tw-shadow-[0_18px_45px_rgba(15,23,42,0.12)]"
        role="alert"
      >
        <span className="tw-mx-auto tw-flex tw-h-14 tw-w-14 tw-items-center tw-justify-center tw-rounded-full tw-bg-slate-100 tw-text-2xl tw-text-vm-slate-500">
          <i className="fas fa-lock" aria-hidden="true" />
        </span>
        <h1 className="tw-m-0 tw-mt-4 tw-text-[1.35rem] tw-font-black tw-leading-tight tw-text-vm-slate-900">
          Chưa có quyền truy cập
        </h1>
        <p className="tw-mx-auto tw-mb-0 tw-mt-3 tw-max-w-[420px] tw-text-[0.92rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-600">
          Tài khoản đã được xác minh nhưng chưa có quyền mở khu vực này. Bạn có thể kiểm tra hồ sơ hoặc liên hệ quản trị viên.
        </p>
        <div className="tw-mt-6 tw-flex tw-flex-wrap tw-justify-center tw-gap-3">
          <Link
            className="tw-inline-flex tw-h-10 tw-items-center tw-justify-center tw-gap-2.5 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-vm-primary tw-px-4 tw-text-[0.92rem] tw-font-semibold tw-text-white tw-no-underline tw-shadow-[0_12px_24px_rgba(37,99,235,0.18)]"
            to={safePath}
          >
            <i className="fas fa-user" aria-hidden="true" />
            {safeLabel}
          </Link>
          {onLogout ? (
            <Button variant="secondary" onClick={onLogout}>
              <i className="fas fa-right-from-bracket" aria-hidden="true" />
              Đăng xuất
            </Button>
          ) : null}
        </div>
      </section>
    </div>
  );
}
