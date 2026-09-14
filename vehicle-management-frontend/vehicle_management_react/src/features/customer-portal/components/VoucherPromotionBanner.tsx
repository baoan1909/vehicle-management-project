import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";

import type { CustomerPortalVoucherBanner } from "@/features/customer-portal/api/customerPortalApi";

const SLIDE_INTERVAL_MS = 4_500;
const TRANSITION_MS = 620;

const bannerTones = [
  {
    background: "tw-bg-[linear-gradient(112deg,#075bd5_0%,#096fe8_52%,#16a7ff_100%)]",
    border: "tw-border-[#73c5ff]",
    glow: "tw-bg-[#69ccff]/40",
    orb: "tw-bg-[linear-gradient(145deg,#e8f8ff_0%,#85d6ff_44%,#2588ee_100%)]",
    icon: "tw-text-[#0c6ada]",
    cta: "tw-text-[#075ccf] hover:tw-bg-[#eaf7ff] hover:tw-text-[#034ba9]",
  },
  {
    background: "tw-bg-[linear-gradient(112deg,#4338ca_0%,#6658dc_49%,#8973f3_100%)]",
    border: "tw-border-[#b7a8ff]",
    glow: "tw-bg-[#c1b2ff]/40",
    orb: "tw-bg-[linear-gradient(145deg,#f4efff_0%,#c5b8ff_46%,#7360dd_100%)]",
    icon: "tw-text-[#5b48cf]",
    cta: "tw-text-[#5340c5] hover:tw-bg-[#f2efff] hover:tw-text-[#3d2ba8]",
  },
  {
    background: "tw-bg-[linear-gradient(112deg,#007d9b_0%,#0698ae_48%,#1db7c9_100%)]",
    border: "tw-border-[#70dfeb]",
    glow: "tw-bg-[#8cebf1]/40",
    orb: "tw-bg-[linear-gradient(145deg,#eaffff_0%,#94e6eb_45%,#269db7_100%)]",
    icon: "tw-text-[#087f9e]",
    cta: "tw-text-[#007c98] hover:tw-bg-[#e8fdff] hover:tw-text-[#00647d]",
  },
] as const;

function useReducedMotion() {
  const [reducedMotion, setReducedMotion] = useState(() => window.matchMedia("(prefers-reduced-motion: reduce)").matches);

  useEffect(() => {
    const mediaQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
    const updatePreference = () => setReducedMotion(mediaQuery.matches);
    mediaQuery.addEventListener("change", updatePreference);
    return () => mediaQuery.removeEventListener("change", updatePreference);
  }, []);

  return reducedMotion;
}

function toneFor(index: number) {
  return bannerTones[index % bannerTones.length];
}

export function VoucherPromotionBanner({ vouchers }: { vouchers: CustomerPortalVoucherBanner[] }) {
  const reducedMotion = useReducedMotion();
  const [activeIndex, setActiveIndex] = useState(0);
  const [leavingIndex, setLeavingIndex] = useState<number | null>(null);
  const [hasEntered, setHasEntered] = useState(false);
  const [paused, setPaused] = useState(false);
  const [shine, setShine] = useState<{ id: number; traveling: boolean } | null>(null);
  const shineIdRef = useRef(0);

  const moveTo = (nextIndex: number) => {
    if (nextIndex === activeIndex) return;
    setLeavingIndex(activeIndex);
    setActiveIndex(nextIndex);
    setHasEntered(false);
    window.requestAnimationFrame(() => setHasEntered(true));
  };

  useEffect(() => {
    setActiveIndex((current) => Math.min(current, Math.max(vouchers.length - 1, 0)));
    setLeavingIndex(null);
    setHasEntered(false);
    const animationFrame = window.requestAnimationFrame(() => setHasEntered(true));
    return () => window.cancelAnimationFrame(animationFrame);
  }, [vouchers.length]);

  useEffect(() => {
    if (vouchers.length < 2 || paused || reducedMotion) return;
    const intervalId = window.setInterval(() => moveTo((activeIndex + 1) % vouchers.length), SLIDE_INTERVAL_MS);
    return () => window.clearInterval(intervalId);
  }, [activeIndex, paused, reducedMotion, vouchers.length]);

  useEffect(() => {
    if (leavingIndex === null || reducedMotion) return;
    const timeoutId = window.setTimeout(() => setLeavingIndex(null), TRANSITION_MS);
    return () => window.clearTimeout(timeoutId);
  }, [leavingIndex, reducedMotion]);

  useEffect(() => {
    if (reducedMotion || paused || vouchers.length === 0) {
      setShine(null);
      return undefined;
    }

    let cancelled = false;
    let finishTimeoutId: number | null = null;
    const playShine = () => {
      const id = ++shineIdRef.current;
      setShine({ id, traveling: false });
      window.requestAnimationFrame(() => window.requestAnimationFrame(() => {
        if (!cancelled) setShine({ id, traveling: true });
      }));
      if (finishTimeoutId !== null) window.clearTimeout(finishTimeoutId);
      finishTimeoutId = window.setTimeout(() => {
        if (!cancelled) setShine((current) => current?.id === id ? null : current);
      }, 1_550);
    };

    playShine();
    const intervalId = window.setInterval(playShine, 6_800);
    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
      if (finishTimeoutId !== null) window.clearTimeout(finishTimeoutId);
    };
  }, [activeIndex, paused, reducedMotion, vouchers.length]);

  if (vouchers.length === 0) return null;

  const activeVoucher = vouchers[activeIndex];
  const leavingVoucher = leavingIndex === null ? null : vouchers[leavingIndex];
  const activeTone = toneFor(activeIndex);
  const transitionClass = reducedMotion ? "tw-duration-0" : "tw-duration-[620ms]";

  return (
    <section
      aria-label="Ưu đãi vé tháng"
      className={`tw-group tw-relative tw-isolate tw-h-[148px] tw-w-full tw-transform-gpu tw-overflow-hidden tw-rounded-[16px] tw-border tw-border-solid tw-bg-[#096fe8] tw-shadow-[0_14px_30px_rgba(12,74,181,.25)] tw-transition-[transform,box-shadow,border-color] tw-duration-300 tw-ease-out hover:-tw-translate-y-1 hover:tw-border-white/85 hover:tw-shadow-[0_20px_38px_rgba(12,74,181,.34)] motion-reduce:tw-transform-none motion-reduce:tw-transition-none max-[700px]:tw-h-[192px] ${activeTone.border}`}
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocus={() => setPaused(true)}
      onBlur={() => setPaused(false)}
    >
      {leavingVoucher ? <PromotionSlide index={leavingIndex!} state="leaving" transitionClass={transitionClass} voucher={leavingVoucher} /> : null}
      <PromotionSlide index={activeIndex} state={hasEntered ? "active" : "entering"} transitionClass={transitionClass} voucher={activeVoucher} />
      {shine ? <span
        aria-hidden="true"
        className="tw-pointer-events-none tw-absolute tw-z-[15] tw-mix-blend-screen"
        key={shine.id}
        style={{
          background: "linear-gradient(90deg,transparent 0%,rgba(255,255,255,.05) 26%,rgba(255,255,255,.42) 50%,rgba(255,255,255,.05) 74%,transparent 100%)",
          height: "330%",
          left: shine.traveling ? "118%" : "-22%",
          opacity: shine.traveling ? 0.7 : 0,
          top: "-116%",
          transform: "rotate(23deg)",
          transition: shine.traveling ? "left 1.35s cubic-bezier(.4,0,.2,1), opacity 160ms ease" : "none",
          width: "12%",
        }}
      /> : null}
      {vouchers.length > 1 ? (
        <div aria-label="Chọn ưu đãi" className="tw-absolute tw-bottom-3.5 tw-left-1/2 tw-z-20 tw-flex -tw-translate-x-1/2 tw-items-center tw-gap-1.5 max-[700px]:tw-bottom-2.5" role="tablist">
          {vouchers.map((voucher, index) => (
            <button
              aria-current={index === activeIndex}
              aria-label={`Xem ưu đãi ${index + 1}: ${voucher.bannerTitle}`}
              className={`tw-h-1.5 tw-rounded-full tw-border-0 tw-p-0 tw-transition-all ${index === activeIndex ? "tw-w-5 tw-bg-white" : "tw-w-1.5 tw-bg-white/45 hover:tw-bg-white/80"}`}
              key={voucher.code}
              type="button"
              onClick={() => moveTo(index)}
            />
          ))}
        </div>
      ) : null}
    </section>
  );
}

function PromotionSlide({ index, state, transitionClass, voucher }: { index: number; state: "active" | "entering" | "leaving"; transitionClass: string; voucher: CustomerPortalVoucherBanner }) {
  const tone = toneFor(index);
  const registrationUrl = `/customer/subscriptions?voucherCode=${encodeURIComponent(voucher.code)}&register=true`;
  const motionClass = state === "leaving" ? "-tw-translate-x-full tw-opacity-0" : state === "entering" ? "tw-translate-x-full tw-opacity-0" : "tw-translate-x-0 tw-opacity-100";
  const headline = voucher.bannerDescription || `Giảm giá vé tháng với mã ${voucher.code}`;
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!copied) return undefined;
    const timeoutId = window.setTimeout(() => setCopied(false), 2_000);
    return () => window.clearTimeout(timeoutId);
  }, [copied]);

  const copyVoucherCode = async () => {
    try {
      await navigator.clipboard.writeText(voucher.code);
      setCopied(true);
    } catch {
      setCopied(false);
    }
  };

  return (
    <div className={`tw-absolute tw-inset-0 tw-overflow-hidden tw-transition-[transform,opacity] tw-ease-[cubic-bezier(.22,1,.36,1)] ${transitionClass} ${motionClass} ${tone.background}`}>
      <span aria-hidden="true" className={`tw-pointer-events-none tw-absolute tw-right-[-62px] tw-top-[-76px] tw-h-[230px] tw-w-[230px] tw-rounded-full tw-blur-xl ${tone.glow}`} />
      <span aria-hidden="true" className="tw-pointer-events-none tw-absolute tw-bottom-[-108px] tw-right-[16%] tw-h-[180px] tw-w-[340px] tw-rotate-[-18deg] tw-rounded-[46%] tw-border tw-border-solid tw-border-white/20" />
      <span aria-hidden="true" className="tw-pointer-events-none tw-absolute tw-left-[34%] tw-top-[-120px] tw-h-[280px] tw-w-[210px] tw-rotate-[28deg] tw-rounded-[40px] tw-bg-white/[.07]" />
      <span aria-hidden="true" className="tw-pointer-events-none tw-absolute tw-bottom-[-40px] tw-left-[-22px] tw-h-[102px] tw-w-[250px] tw-rotate-[11deg] tw-rounded-full tw-bg-[#001c68]/[.16]" />

      <div className="tw-relative tw-z-10 tw-grid tw-h-full tw-grid-cols-[96px_minmax(0,1fr)_auto] tw-items-center tw-gap-4 tw-px-6 tw-py-3 max-[700px]:tw-grid-cols-[64px_minmax(0,1fr)] max-[700px]:tw-gap-3 max-[700px]:tw-px-4 max-[700px]:tw-py-3">
        <span className={`tw-relative tw-grid tw-h-[90px] tw-w-[90px] tw-shrink-0 tw-place-items-center tw-rounded-full tw-border tw-border-solid tw-border-white/60 tw-shadow-[inset_0_1px_0_rgba(255,255,255,.7),0_13px_23px_rgba(0,35,107,.24)] max-[700px]:tw-h-[58px] max-[700px]:tw-w-[58px] ${tone.orb}`}>
          <span aria-hidden="true" className="tw-absolute tw-inset-[9px] tw-rounded-full tw-border tw-border-solid tw-border-white/50" />
          <i aria-hidden="true" className={`fas fa-gift tw-relative tw-text-[2.5rem] tw-drop-shadow-[0_4px_4px_rgba(255,255,255,.45)] max-[700px]:tw-text-[1.65rem] ${tone.icon}`} />
          <span aria-hidden="true" className="tw-absolute tw-right-[12px] tw-top-[13px] tw-h-3 tw-w-3 tw-rounded-full tw-bg-white/80" />
        </span>

        <div className="tw-min-w-0 tw-pr-2">
          <span className="tw-inline-flex tw-max-w-full tw-items-center tw-gap-1.5 tw-truncate tw-rounded-full tw-border tw-border-solid tw-border-white/20 tw-bg-white/[.17] tw-px-2.5 tw-py-1 tw-text-[0.7rem] tw-font-bold tw-text-white tw-shadow-[0_5px_12px_rgba(0,47,135,.15)] max-[700px]:tw-text-[.62rem]">
            <i aria-hidden="true" className="fas fa-gift tw-text-[.63rem]" />{voucher.bannerTitle}
          </span>
          <strong className="tw-mt-1 tw-block tw-max-w-[710px] tw-text-[clamp(1.15rem,2vw,1.8rem)] tw-font-bold tw-leading-[1.14] tw-tracking-[-.035em] tw-text-white tw-drop-shadow-[0_2px_1px_rgba(0,49,132,.14)] max-[700px]:tw-text-[1.08rem]">{headline}</strong>
          <span className="tw-mt-1.5 tw-inline-flex tw-items-center tw-gap-2 tw-rounded-full tw-border tw-border-solid tw-border-white/60 tw-bg-[#0757c8]/30 tw-px-3.5 tw-py-1.5 tw-text-[1.04rem] tw-font-bold tw-tracking-[.04em] tw-text-white tw-shadow-[inset_0_1px_0_rgba(255,255,255,.2)] max-[700px]:tw-mt-2 max-[700px]:tw-px-2.5 max-[700px]:tw-py-1 max-[700px]:tw-text-[.82rem]">
            <i aria-hidden="true" className="fas fa-ticket-alt tw-text-[.85rem]" />{voucher.code}
            <button aria-label={`Sao chép mã ${voucher.code}`} className="tw-grid tw-h-6 tw-w-6 tw-place-items-center tw-rounded-full tw-border-0 tw-bg-white/15 tw-p-0 tw-text-[.8rem] tw-text-white tw-transition hover:tw-scale-105 hover:tw-bg-white/30 focus-visible:tw-outline focus-visible:tw-outline-2 focus-visible:tw-outline-offset-2 focus-visible:tw-outline-white" title={copied ? "Đã sao chép" : "Sao chép mã"} type="button" onClick={() => void copyVoucherCode()}>
              <i aria-hidden="true" className={copied ? "fas fa-check" : "far fa-copy"} />
            </button>
          </span>
        </div>

        <Link className={`tw-inline-flex tw-h-11 tw-shrink-0 tw-items-center tw-justify-center tw-gap-2 tw-rounded-full tw-border tw-border-solid tw-border-white tw-bg-white tw-px-5 tw-text-[.86rem] tw-font-bold tw-shadow-[0_9px_20px_rgba(0,28,88,.24)] tw-transition-all tw-duration-200 hover:-tw-translate-y-px hover:tw-shadow-[0_13px_24px_rgba(0,28,88,.31)] hover:tw-no-underline focus-visible:tw-outline focus-visible:tw-outline-2 focus-visible:tw-outline-offset-2 focus-visible:tw-outline-white max-[700px]:tw-col-span-2 max-[700px]:tw-h-9 max-[700px]:tw-w-full max-[700px]:tw-text-[.76rem] ${tone.cta}`} to={registrationUrl}>
          Đăng ký ngay <i aria-hidden="true" className="fas fa-arrow-right tw-text-[.7rem]" />
        </Link>
      </div>
    </div>
  );
}
