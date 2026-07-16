import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigation } from "react-router-dom";

import { cn } from "@/lib/cn";

const MIN_VISIBLE_MS = 320;
const COMPLETE_DELAY_MS = 180;

function isModifiedEvent(event: MouseEvent) {
  return event.metaKey || event.altKey || event.ctrlKey || event.shiftKey || event.button !== 0;
}

function shouldStartForAnchor(anchor: HTMLAnchorElement) {
  if (anchor.target && anchor.target !== "_self") return false;
  if (anchor.hasAttribute("download")) return false;

  const href = anchor.getAttribute("href");
  if (!href || href.startsWith("#") || href.startsWith("mailto:") || href.startsWith("tel:")) {
    return false;
  }

  try {
    const targetUrl = new URL(anchor.href, window.location.href);
    if (targetUrl.origin !== window.location.origin) return false;
    return `${targetUrl.pathname}${targetUrl.search}${targetUrl.hash}` !== `${window.location.pathname}${window.location.search}${window.location.hash}`;
  } catch {
    return false;
  }
}

function getNextUrl(value?: string | URL | null) {
  if (!value) return null;
  try {
    return new URL(value, window.location.href);
  } catch {
    return null;
  }
}

export function PageTransitionLoader() {
  const location = useLocation();
  const navigation = useNavigation();
  const [visible, setVisible] = useState(false);
  const startedAtRef = useRef(0);
  const hideTimerRef = useRef<number | null>(null);
  const completeTimerRef = useRef<number | null>(null);

  const clearTimers = () => {
    if (hideTimerRef.current) {
      window.clearTimeout(hideTimerRef.current);
      hideTimerRef.current = null;
    }

    if (completeTimerRef.current) {
      window.clearTimeout(completeTimerRef.current);
      completeTimerRef.current = null;
    }
  };

  const startLoading = () => {
    clearTimers();
    startedAtRef.current = Date.now();
    setVisible(true);
  };

  const finishLoading = () => {
    if (!visible) return;
    if (completeTimerRef.current) window.clearTimeout(completeTimerRef.current);

    completeTimerRef.current = window.setTimeout(() => {
      const elapsed = Date.now() - startedAtRef.current;
      const remaining = Math.max(MIN_VISIBLE_MS - elapsed, 0);

      hideTimerRef.current = window.setTimeout(() => {
        setVisible(false);
        hideTimerRef.current = null;
      }, remaining);
    }, COMPLETE_DELAY_MS);
  };

  useEffect(() => {
    function handleDocumentClick(event: MouseEvent) {
      if (isModifiedEvent(event) || event.defaultPrevented) return;
      const target = event.target as Element | null;
      const anchor = target?.closest("a[href]");
      if (anchor instanceof HTMLAnchorElement && shouldStartForAnchor(anchor)) {
        startLoading();
      }
    }

    const originalPushState = window.history.pushState;
    const originalReplaceState = window.history.replaceState;

    window.history.pushState = function pushStateWithLoader(data: unknown, unused: string, url?: string | URL | null) {
      const nextUrl = getNextUrl(url);
      if (nextUrl && `${nextUrl.pathname}${nextUrl.search}${nextUrl.hash}` !== `${window.location.pathname}${window.location.search}${window.location.hash}`) {
        startLoading();
      }
      return originalPushState.call(this, data, unused, url);
    };

    window.history.replaceState = function replaceStateWithLoader(data: unknown, unused: string, url?: string | URL | null) {
      const nextUrl = getNextUrl(url);
      if (nextUrl && `${nextUrl.pathname}${nextUrl.search}${nextUrl.hash}` !== `${window.location.pathname}${window.location.search}${window.location.hash}`) {
        startLoading();
      }
      return originalReplaceState.call(this, data, unused, url);
    };

    window.addEventListener("popstate", startLoading);
    document.addEventListener("click", handleDocumentClick, true);

    return () => {
      document.removeEventListener("click", handleDocumentClick, true);
      window.removeEventListener("popstate", startLoading);
      window.history.pushState = originalPushState;
      window.history.replaceState = originalReplaceState;
      clearTimers();
    };
  }, []);

  useEffect(() => {
    if (navigation.state === "loading" || navigation.state === "submitting") {
      startLoading();
      return;
    }

    finishLoading();
  }, [navigation.state]);

  useEffect(() => {
    finishLoading();
  }, [location.key, location.pathname, location.search]);

  return (
    <div
      className={cn(
        "tw-pointer-events-none tw-fixed tw-inset-0 tw-z-[2200] tw-bg-white/20 tw-backdrop-blur-[1px] tw-transition-opacity tw-duration-200",
        visible ? "tw-opacity-100" : "tw-opacity-0",
      )}
      aria-hidden={!visible}
    >
      <div
        className={cn(
          "tw-absolute tw-left-1/2 tw-top-[44%] tw-flex tw--translate-x-1/2 tw--translate-y-1/2 tw-items-center tw-justify-center tw-transition-transform tw-duration-200",
          visible ? "tw-scale-100" : "tw-scale-95",
        )}
      >
        <div className="tw-relative tw-h-[78px] tw-w-[78px]">
          <span className="tw-absolute tw-inset-0 tw-rounded-full tw-bg-[conic-gradient(from_0deg,rgba(37,99,235,0),rgba(37,99,235,0.92),rgba(16,185,129,0.82),rgba(37,99,235,0))] tw-animate-[vm-loader-spin_1.1s_linear_infinite]" />
          <span className="tw-absolute tw-inset-[5px] tw-rounded-full tw-bg-slate-50/70 tw-shadow-[inset_0_0_0_1px_rgba(191,219,254,0.75)]" />
          <span className="tw-absolute tw-inset-[14px] tw-rounded-full tw-bg-brand-50/80 tw-shadow-[0_12px_24px_rgba(37,99,235,0.16)]" />
          <span className="tw-absolute tw-left-1/2 tw-top-1/2 tw-flex tw-h-[42px] tw-w-[42px] tw--translate-x-1/2 tw--translate-y-1/2 tw-animate-[vm-loader-car_1.2s_ease-in-out_infinite] tw-items-center tw-justify-center tw-rounded-full tw-bg-vm-primary tw-text-white">
            <i className="fas fa-car-side tw-text-[1.1rem]" />
          </span>
          <span className="tw-absolute tw-bottom-[14px] tw-left-[19px] tw-h-[2px] tw-w-[40px] tw-overflow-hidden tw-rounded-full tw-bg-brand-100">
            <span className="tw-block tw-h-full tw-w-1/2 tw-animate-[vm-loader-lane_0.85s_ease-in-out_infinite] tw-rounded-full tw-bg-vm-primary" />
          </span>
        </div>
      </div>
      <style>
        {`
          @keyframes vm-loader-spin {
            from { transform: rotate(0deg); }
            to { transform: rotate(360deg); }
          }

          @keyframes vm-loader-car {
            0%, 100% { transform: translate(-50%, -50%) translateY(0); }
            50% { transform: translate(-50%, -50%) translateY(-3px); }
          }

          @keyframes vm-loader-lane {
            0% { transform: translateX(-120%); }
            100% { transform: translateX(230%); }
          }
        `}
      </style>
    </div>
  );
}
