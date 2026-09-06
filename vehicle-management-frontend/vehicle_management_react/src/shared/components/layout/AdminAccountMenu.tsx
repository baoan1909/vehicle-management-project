import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { useAuth } from "@/core/auth/useAuth";
import { logoutCurrentUser } from "@/core/auth/logout";
import { cn } from "@/lib/cn";
import { getApprovalStatusValue, getRoleLabel, getStatusMeta } from "@/shared/utils/accountStatus";
import { resolvePublicMediaUrl } from "@/shared/utils/mediaUrl";

const panelClassName =
  "tw-absolute tw-right-0 tw-top-[calc(100%+12px)] tw-z-[1080] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-p-2 tw-shadow-[0_18px_42px_rgba(15,23,42,0.16)]";

const itemClassName =
  "tw-my-1 tw-grid tw-grid-cols-[40px_minmax(0,1fr)] tw-items-center tw-gap-3 tw-rounded-vm-md tw-px-2.5 tw-py-2 tw-text-left tw-text-slate-900 tw-transition hover:tw-bg-brand-50 hover:tw-text-slate-900 hover:tw-no-underline";

function HeaderItemIcon({ icon }: { icon: string }) {
  return <span className="tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-vm-lg tw-bg-brand-50 tw-text-[1.12rem] tw-text-vm-primary"><i className={icon} /></span>;
}

function HeaderItemCopy({ title, meta }: { title: string; meta: string }) {
  return <span className="tw-grid tw-min-w-0 tw-gap-1"><strong className="tw-text-[0.92rem] tw-font-extrabold tw-text-slate-900">{title}</strong><small className="tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">{meta}</small></span>;
}

function StatusBadge({ status }: { status?: string }) {
  const meta = getStatusMeta(status);
  return <span className={cn("tw-inline-flex tw-items-center tw-gap-1.5 tw-rounded-full tw-px-2.5 tw-py-1 tw-text-[0.72rem] tw-font-extrabold tw-ring-1", meta.className)}><span className={cn("tw-h-1.5 tw-w-1.5 tw-rounded-full", meta.dotClassName)} />{meta.label}</span>;
}

function UserAvatar({ alt, className, status, src }: { alt: string; className: string; status?: string; src?: string }) {
  const meta = getStatusMeta(status);
  const [imageFailed, setImageFailed] = useState(false);
  const initials = alt.trim().split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part.charAt(0).toUpperCase()).join("") || "ND";

  useEffect(() => setImageFailed(false), [src]);

  return (
    <span className="tw-relative tw-inline-flex tw-flex-shrink-0">
      {src && !imageFailed ? <img src={src} alt={alt} className={cn("tw-rounded-full tw-object-cover", className)} onError={() => setImageFailed(true)} /> : <span aria-label={alt} className={cn("tw-inline-flex tw-items-center tw-justify-center tw-rounded-full tw-bg-brand-50 tw-font-black tw-text-vm-primary", className)}>{initials}</span>}
      <span aria-label={meta.label} className={cn("tw-absolute tw-bottom-0 tw-right-0 tw-inline-flex tw-h-3 tw-w-3 tw-items-center tw-justify-center tw-rounded-full tw-border-2 tw-border-white", meta.dotClassName)} title={meta.label} />
    </span>
  );
}

/** Account menu shared by every internal header, backed by the authenticated session. */
export function AdminAccountMenu() {
  const navigate = useNavigate();
  const { user, setUser } = useAuth();
  const [open, setOpen] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const rootRef = useRef<HTMLDivElement | null>(null);
  const usernameLabel = user?.username?.trim() || user?.email?.trim() || "";
  const displayName = user?.fullName?.trim() || usernameLabel || "Người dùng";
  const roleLabel = getRoleLabel(user?.role, user?.roleLabel);
  const approvalStatus = getApprovalStatusValue(user);
  const accountStatus = user?.accountStatus;
  const avatarUrl = resolvePublicMediaUrl(user?.avatarUrl) || undefined;

  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) setOpen(false);
    }

    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, []);

  function handleLogout() {
    if (isLoggingOut) return;
    setIsLoggingOut(true);
    setOpen(false);
    logoutCurrentUser(setUser);
  }

  return (
    <div ref={rootRef} className="tw-relative">
      <button type="button" className={cn("tw-flex tw-min-h-12 tw-w-auto tw-min-w-[238px] tw-items-center tw-gap-3 tw-rounded-full tw-border tw-border-solid tw-border-brand-200 tw-bg-white tw-px-3 tw-py-1 tw-text-left tw-transition hover:tw-bg-brand-50", open ? "tw-bg-brand-50 tw-shadow-[0_8px_20px_rgba(37,99,235,0.08)]" : "")} onClick={() => setOpen((current) => !current)} aria-label={`Mở hồ sơ ${displayName}`}>
        <UserAvatar src={avatarUrl} alt={displayName} status={approvalStatus} className="tw-h-9 tw-w-9" />
        <span className="tw-min-w-0 tw-flex-1"><strong className="tw-block tw-truncate tw-text-[0.92rem] tw-font-extrabold tw-leading-tight tw-text-slate-900">{displayName}</strong><small className="tw-mt-0.5 tw-block tw-truncate tw-text-[0.74rem] tw-font-bold tw-leading-tight tw-text-vm-slate-500">{roleLabel}</small></span>
      </button>

      {open ? (
        <div className={cn(panelClassName, "tw-w-[330px]")}>
          <div className="tw-flex tw-items-center tw-gap-3 tw-rounded-vm-md tw-bg-brand-50 tw-p-3"><UserAvatar src={avatarUrl} alt={displayName} status={approvalStatus} className="tw-h-12 tw-w-12" /><div className="tw-min-w-0"><strong className="tw-block tw-truncate tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900">{displayName}</strong><small className="tw-block tw-truncate tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-500">{roleLabel}</small></div></div>
          <div className="tw-my-2 tw-grid tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
            <div className="tw-flex tw-items-center tw-justify-between tw-gap-3"><span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500">Tên tài khoản</span><span className="tw-min-w-0 tw-truncate tw-text-right tw-text-[0.8rem] tw-font-black tw-text-vm-slate-900">{usernameLabel || displayName}</span></div>
            <div className="tw-flex tw-items-center tw-justify-between tw-gap-3"><span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500">Trạng thái duyệt</span><StatusBadge status={approvalStatus} /></div>
            <div className="tw-flex tw-items-center tw-justify-between tw-gap-3"><span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500">Tài khoản</span><StatusBadge status={accountStatus} /></div>
          </div>
          <Link to="/admin/profile" className={itemClassName} onClick={() => setOpen(false)}><HeaderItemIcon icon="fas fa-user-circle" /><HeaderItemCopy title="Thông tin tài khoản" meta="Cập nhật hồ sơ quản trị" /></Link>
          <button type="button" className={cn(itemClassName, "tw-w-full tw-border-0 tw-bg-white")} onClick={() => { setOpen(false); navigate("/admin/profile?action=change-password"); }}><HeaderItemIcon icon="fas fa-key" /><HeaderItemCopy title="Đổi mật khẩu" meta="Cập nhật mật khẩu đăng nhập nội bộ" /></button>
          <Link to="/contact" className={itemClassName} onClick={() => setOpen(false)}><HeaderItemIcon icon="fas fa-question-circle" /><HeaderItemCopy title="Trợ giúp" meta="Xem hướng dẫn và liên hệ hỗ trợ" /></Link>
          <button type="button" className={cn(itemClassName, "tw-w-full tw-border-0 tw-bg-white", isLoggingOut ? "tw-cursor-wait tw-opacity-75" : "")} disabled={isLoggingOut} aria-busy={isLoggingOut} onClick={handleLogout}><HeaderItemIcon icon={isLoggingOut ? "fas fa-spinner fa-spin" : "fas fa-sign-out-alt"} /><HeaderItemCopy title={isLoggingOut ? "Đang đăng xuất" : "Đăng xuất"} meta="Thoát khỏi phiên làm việc hiện tại" /></button>
        </div>
      ) : null}
    </div>
  );
}
