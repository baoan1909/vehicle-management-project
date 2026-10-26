import { useEffect, useRef, useState } from "react";

import {
  verifyGuestCaptcha,
  type CaptchaPurpose,
} from "@/features/support/api/guestSupportApi";

const SCRIPT_ID = "cloudflare-turnstile-script";
const SCRIPT_URL = "https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit";

type TurnstileApi = {
  remove: (widgetId: string) => void;
  render: (element: HTMLElement, options: Record<string, unknown>) => string;
};

declare global {
  interface Window {
    turnstile?: TurnstileApi;
  }
}

type CaptchaChallengeProps = {
  purpose: CaptchaPurpose;
  resetKey: number;
  sessionId: string;
  siteKey: string;
  onProof: (proof: string | null) => void;
};

type ChallengeState = "loading" | "ready" | "verifying" | "expired" | "unavailable";

function loadTurnstileScript() {
  const existing = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null;
  if (window.turnstile) return Promise.resolve();
  if (existing) {
    return new Promise<void>((resolve, reject) => {
      existing.addEventListener("load", () => resolve(), { once: true });
      existing.addEventListener("error", () => reject(new Error("CAPTCHA unavailable")), { once: true });
    });
  }
  return new Promise<void>((resolve, reject) => {
    const script = document.createElement("script");
    script.id = SCRIPT_ID;
    script.src = SCRIPT_URL;
    script.async = true;
    script.defer = true;
    script.addEventListener("load", () => resolve(), { once: true });
    script.addEventListener("error", () => reject(new Error("CAPTCHA unavailable")), { once: true });
    document.head.appendChild(script);
  });
}

export function CaptchaChallenge({ purpose, resetKey, sessionId, siteKey, onProof }: CaptchaChallengeProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [retryNonce, setRetryNonce] = useState(0);
  const [state, setState] = useState<ChallengeState>("loading");

  useEffect(() => {
    let active = true;
    let widgetId: string | null = null;
    onProof(null);
    setState("loading");
    if (!siteKey.trim()) {
      setState("unavailable");
      return undefined;
    }

    void loadTurnstileScript()
      .then(() => {
        if (!active || !containerRef.current || !window.turnstile) return;
        widgetId = window.turnstile.render(containerRef.current, {
          action: purpose === "INTAKE" ? "support_intake" : "support_assistant",
          callback: async (challengeToken: string) => {
            if (!active) return;
            setState("verifying");
            try {
              const response = await verifyGuestCaptcha(challengeToken, purpose, sessionId);
              if (!active) return;
              onProof(response.data.proof);
              setState("ready");
            } catch {
              if (!active) return;
              onProof(null);
              setState("unavailable");
            }
          },
          "error-callback": () => {
            if (!active) return;
            onProof(null);
            setState("unavailable");
          },
          "expired-callback": () => {
            if (!active) return;
            onProof(null);
            setState("expired");
          },
          sitekey: siteKey,
        });
      })
      .catch(() => {
        if (active) setState("unavailable");
      });

    return () => {
      active = false;
      if (widgetId && window.turnstile) window.turnstile.remove(widgetId);
      if (containerRef.current) containerRef.current.replaceChildren();
    };
  }, [onProof, purpose, resetKey, retryNonce, sessionId, siteKey]);

  return (
    <div className="tw-grid tw-gap-2" aria-live="polite">
      <div ref={containerRef} aria-label={`CAPTCHA ${purpose === "INTAKE" ? "gửi yêu cầu" : "trợ lý"}`} />
      {state === "loading" ? <p className="tw-m-0 tw-text-sm tw-text-slate-600" role="status">Đang tải CAPTCHA...</p> : null}
      {state === "verifying" ? <p className="tw-m-0 tw-text-sm tw-text-slate-600" role="status">Đang xác minh CAPTCHA...</p> : null}
      {state === "ready" ? <p className="tw-m-0 tw-text-sm tw-font-semibold tw-text-emerald-700">CAPTCHA đã xác minh.</p> : null}
      {state === "expired" ? <p className="tw-m-0 tw-text-sm tw-text-amber-700" role="alert">CAPTCHA đã hết hạn.</p> : null}
      {state === "unavailable" ? (
        <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2" role="alert">
          <span className="tw-text-sm tw-text-red-700">Không thể xác minh CAPTCHA. Bạn có thể dùng trình đọc màn hình của thử thách hoặc thử lại.</span>
          <button className="tw-rounded-vm-md tw-border tw-border-slate-300 tw-bg-white tw-px-3 tw-py-1.5 tw-font-bold" type="button" onClick={() => setRetryNonce((value) => value + 1)}>Thử lại CAPTCHA</button>
        </div>
      ) : null}
    </div>
  );
}
