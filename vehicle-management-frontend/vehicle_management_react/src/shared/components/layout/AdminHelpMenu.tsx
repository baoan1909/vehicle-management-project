import { useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Link } from "react-router-dom";

import { useAuth } from "@/core/auth/useAuth";
import { PostOnboardingReadinessDrawer } from "@/features/user-guide/components/PostOnboardingReadinessDrawer";
import { useUserGuideManualLauncher } from "@/features/user-guide/hooks/useUserGuideManualLauncher";
import { resolvePostOnboardingJourneyGuide } from "@/features/user-guide/utils/postOnboardingJourney";
import { cn } from "@/lib/cn";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { useToast } from "@/shared/components/ui/ToastProvider";

export function AdminHelpMenu() {
  const { user } = useAuth();
  const { launchGuide, launchingCode } = useUserGuideManualLauncher();
  const toast = useToast();
  const menuId = useId();
  const containerRef = useRef<HTMLDivElement | null>(null);
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const guideItemRef = useRef<HTMLButtonElement | null>(null);
  const [open, setOpen] = useState(false);
  const [readinessOpen, setReadinessOpen] = useState(false);
  const guideCode = resolvePostOnboardingJourneyGuide(user);
  const canReadOwnSupportTickets = hasAnyPermission(user, ["SUPPORT_TICKET_READ_OWN"]);
  const readinessAvailable = Boolean(guideCode || (
    user
    && !user.onboardingRequired
    && (
      user.accessState === "ORGANIZATION_ACCESS_REQUIRED"
      || (user.accessState === "ACTIVE" && hasAnyPermission(user, [
        "PARKING_LOT_READ_ALL",
        "PARKING_SESSION_CHECK_IN_ALL",
        "PARKING_SESSION_CHECK_OUT_ALL",
      ]))
    )
  ));

  useEffect(() => {
    if (!open) return undefined;
    guideItemRef.current?.focus();

    const handlePointerDown = (event: MouseEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false);
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key !== "Escape") return;
      setOpen(false);
      triggerRef.current?.focus();
    };

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  const handleGuideOpen = async () => {
    if (!guideCode || launchingCode) return;
    setOpen(false);
    await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));
    try {
      await launchGuide(guideCode);
    } catch (caught) {
      toast.error(
        caught instanceof Error ? caught.message : "Không tải được hướng dẫn đã phát hành.",
        "Không thể mở hướng dẫn",
      );
    }
  };

  const handleReadinessOpen = () => {
    if (!readinessAvailable) return;
    setOpen(false);
    setReadinessOpen(true);
  };

  return (
    <div ref={containerRef} className="tw-relative" {...{ "data-user-guide-anchor": "ADMIN.HELP.LAUNCHER" }}>
      <button
        ref={triggerRef}
        type="button"
        className={cn(
          "tw-inline-flex tw-h-[54px] tw-w-[54px] tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-solid tw-border-transparent tw-bg-transparent tw-text-[1.18rem] tw-text-slate-900 tw-transition hover:tw-border-slate-200 hover:tw-bg-slate-100",
          open ? "tw-border-slate-200 tw-bg-white tw-shadow-[0_8px_20px_rgba(15,23,42,0.06)]" : "",
        )}
        aria-controls={menuId}
        aria-expanded={open}
        aria-haspopup="menu"
        aria-label="Mở hướng dẫn và hỗ trợ"
        onClick={() => setOpen((current) => !current)}
      >
        <i className="far fa-question-circle" aria-hidden="true" />
      </button>

      {open ? (
        <div
          id={menuId}
          role="menu"
          aria-label="Hướng dẫn và hỗ trợ"
          className="tw-absolute tw-right-0 tw-top-[calc(100%+10px)] tw-z-[1090] tw-w-[280px] tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-2 tw-shadow-[0_18px_42px_rgba(15,23,42,0.18)]"
        >
          <button
            ref={guideItemRef}
            type="button"
            role="menuitem"
            disabled={!guideCode || Boolean(launchingCode)}
            className="tw-flex tw-min-h-[52px] tw-w-full tw-items-center tw-gap-3 tw-rounded-vm-md tw-border-0 tw-bg-white tw-px-3 tw-text-left tw-text-vm-slate-800 tw-transition hover:tw-bg-brand-50 hover:tw-text-vm-primary disabled:tw-cursor-not-allowed disabled:tw-opacity-50"
            onClick={() => void handleGuideOpen()}
          >
            <span className="tw-inline-flex tw-h-9 tw-w-9 tw-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-bg-brand-50 tw-text-vm-primary">
              <i className="far fa-compass" aria-hidden="true" />
            </span>
            <strong className="tw-min-w-0 tw-text-[0.94rem] tw-font-extrabold">Hướng dẫn</strong>
          </button>

          <button
            type="button"
            role="menuitem"
            disabled={!readinessAvailable}
            className="tw-flex tw-min-h-[52px] tw-w-full tw-items-center tw-gap-3 tw-rounded-vm-md tw-border-0 tw-bg-white tw-px-3 tw-text-left tw-text-vm-slate-800 tw-transition hover:tw-bg-brand-50 hover:tw-text-vm-primary disabled:tw-cursor-not-allowed disabled:tw-opacity-50"
            onClick={handleReadinessOpen}
          >
            <span className="tw-inline-flex tw-h-9 tw-w-9 tw-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-bg-brand-50 tw-text-vm-primary">
              <i className="far fa-life-ring" aria-hidden="true" />
            </span>
            <strong className="tw-min-w-0 tw-text-[0.94rem] tw-font-extrabold">Trợ giúp</strong>
          </button>

          {canReadOwnSupportTickets ? (
            <Link
              to="/admin/my-support"
              role="menuitem"
              className="tw-flex tw-min-h-[52px] tw-w-full tw-items-center tw-gap-3 tw-rounded-vm-md tw-px-3 tw-text-left tw-text-vm-slate-800 tw-transition hover:tw-bg-brand-50 hover:tw-text-vm-primary hover:tw-no-underline"
              onClick={() => setOpen(false)}
            >
              <span className="tw-inline-flex tw-h-9 tw-w-9 tw-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-bg-brand-50 tw-text-vm-primary">
                <i className="far fa-list-alt" aria-hidden="true" />
              </span>
              <strong className="tw-min-w-0 tw-text-[0.94rem] tw-font-extrabold">Yêu cầu của tôi</strong>
            </Link>
          ) : null}
        </div>
      ) : null}
      {typeof document !== "undefined" ? createPortal(
        <PostOnboardingReadinessDrawer
          onClose={() => setReadinessOpen(false)}
          open={readinessOpen}
        />,
        document.body,
      ) : null}
    </div>
  );
}
