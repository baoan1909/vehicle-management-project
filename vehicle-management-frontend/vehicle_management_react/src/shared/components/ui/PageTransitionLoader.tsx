import { useCallback, useEffect, useRef, useState } from "react";
import { useLocation, useNavigation } from "react-router-dom";

import { cn } from "@/lib/cn";

const MIN_VISIBLE_MS = 320;
const COMPLETE_DELAY_MS = 180;

function CarLoaderGraphic() {
  return (
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
  );
}

function LoaderKeyframes() {
  return (
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
  );
}

export function FullPageCarLoader({ label = "Đang tải dữ liệu..." }: { label?: string }) {
  return (
    <div className="tw-fixed tw-inset-0 tw-z-[2400] tw-flex tw-items-center tw-justify-center tw-bg-slate-50/95 tw-text-vm-slate-700 tw-backdrop-blur-[2px]" role="status" aria-live="polite">
      <div className="tw-grid tw-place-items-center tw-gap-4">
        <CarLoaderGraphic />
        <span className="tw-text-[0.94rem] tw-font-extrabold tw-text-vm-slate-600">{label}</span>
      </div>
      <LoaderKeyframes />
    </div>
  );
}

export function PageTransitionLoader() {
  const location = useLocation();
  const navigation = useNavigation();
  const [visible, setVisible] = useState(false);
  const visibleRef = useRef(false);
  const startedAtRef = useRef(0);
  const hideTimerRef = useRef<number | null>(null);
  const completeTimerRef = useRef<number | null>(null);
  const lastLocationRef = useRef(`${location.key}:${location.pathname}${location.search}${location.hash}`);

  const clearTimers = useCallback(() => {
    if (hideTimerRef.current) {
      window.clearTimeout(hideTimerRef.current);
      hideTimerRef.current = null;
    }

    if (completeTimerRef.current) {
      window.clearTimeout(completeTimerRef.current);
      completeTimerRef.current = null;
    }
  }, []);

  const startLoading = useCallback(() => {
    clearTimers();
    startedAtRef.current = Date.now();
    visibleRef.current = true;
    setVisible(true);
  }, [clearTimers]);

  const finishLoading = useCallback(() => {
    if (!visibleRef.current) return;
    if (completeTimerRef.current) window.clearTimeout(completeTimerRef.current);

    completeTimerRef.current = window.setTimeout(() => {
      const elapsed = Date.now() - startedAtRef.current;
      const remaining = Math.max(MIN_VISIBLE_MS - elapsed, 0);

      hideTimerRef.current = window.setTimeout(() => {
        visibleRef.current = false;
        setVisible(false);
        hideTimerRef.current = null;
      }, remaining);
    }, COMPLETE_DELAY_MS);
  }, []);

  useEffect(() => {
    return () => {
      clearTimers();
      visibleRef.current = false;
    };
  }, [clearTimers]);

  useEffect(() => {
    if (navigation.state === "loading" || navigation.state === "submitting") {
      startLoading();
      return;
    }

    finishLoading();
  }, [finishLoading, navigation.state, startLoading]);

  useEffect(() => {
    const nextLocation = `${location.key}:${location.pathname}${location.search}${location.hash}`;
    if (lastLocationRef.current === nextLocation) return;

    lastLocationRef.current = nextLocation;
    startLoading();
    finishLoading();
  }, [finishLoading, location.hash, location.key, location.pathname, location.search, startLoading]);

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
        <CarLoaderGraphic />
      </div>
      <LoaderKeyframes />
    </div>
  );
}
