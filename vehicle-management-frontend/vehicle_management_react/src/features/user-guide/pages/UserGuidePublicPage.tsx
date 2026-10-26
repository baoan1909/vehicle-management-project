import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import { Button } from "@/components/ui/Button";
import { useAuth } from "@/core/auth/useAuth";
import { getPublishedUserGuide, recordUserGuideEvent } from "@/features/user-guide/api/userGuideApi";
import { UserGuideHost } from "@/features/user-guide/components/UserGuideHost";
import { UserGuideProvider, useUserGuide } from "@/features/user-guide/context/UserGuideContext";
import { toPresentation } from "@/features/user-guide/hooks/useUserGuideRuntime";
import type { UserGuideLifecycleEvent, UserGuidePresentation } from "@/features/user-guide/types/userGuide";

type PageState =
  | { status: "loading" }
  | { status: "ready"; guide: UserGuidePresentation }
  | { status: "not-found" }
  | { status: "forbidden" }
  | { status: "unavailable"; message: string };

function isInternalPath(deepLink: string | null | undefined): deepLink is string {
  return Boolean(deepLink?.startsWith("/") && !deepLink.startsWith("//") && !deepLink.includes("\\"));
}

function GuideReader({ guide }: { guide: UserGuidePresentation }) {
  const navigate = useNavigate();
  const { activeGuide, startGuide } = useUserGuide();
  const started = activeGuide?.guideId === guide.guideId;

  return (
    <div className="tw-mx-auto tw-flex tw-w-full tw-max-w-3xl tw-flex-col tw-gap-4 tw-p-4">
      <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-6">
        <p className="tw-m-0 tw-text-[0.8rem] tw-font-black tw-uppercase tw-tracking-wide tw-text-vm-slate-500">
          Hướng dẫn · {guide.code}
        </p>
        <h1 className="tw-mb-2 tw-mt-1 tw-text-[1.4rem] tw-font-black tw-text-vm-slate-900">{guide.title}</h1>
        {guide.summary ? <p className="tw-text-[0.92rem] tw-text-vm-slate-600">{guide.summary}</p> : null}
        <ol className="tw-mt-4 tw-flex tw-flex-col tw-gap-3 tw-p-0" style={{ listStyle: "none" }}>
          {guide.steps.map((step, index) => (
            <li key={step.stepId} className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-p-3">
              <strong className="tw-text-[0.9rem] tw-text-vm-slate-900">
                {index + 1}. {step.title}
              </strong>
              <p className="tw-m-0 tw-mt-1 tw-text-[0.86rem] tw-text-vm-slate-600">{step.body}</p>
              {step.mediaUrl ? (
                <img alt="" className="tw-mt-2 tw-max-h-64 tw-w-full tw-rounded-vm-md tw-object-cover" src={step.mediaUrl} />
              ) : null}
            </li>
          ))}
        </ol>
        <div className="tw-mt-4 tw-flex tw-flex-wrap tw-gap-2">
          {!started ? (
            <Button onClick={() => startGuide(guide)}>
              <i className="fas fa-play" aria-hidden="true" /> Bắt đầu hướng dẫn tương tác
            </Button>
          ) : null}
          {isInternalPath(guide.deepLink) ? (
            <Button onClick={() => navigate(guide.deepLink as string)} variant="secondary">
              Mở màn hình liên quan
            </Button>
          ) : null}
        </div>
      </div>
    </div>
  );
}

export function UserGuidePublicPage() {
  const { user } = useAuth();
  const { code } = useParams<{ code: string }>();
  const [state, setState] = useState<PageState>({ status: "loading" });

  useEffect(() => {
    let cancelled = false;
    async function load() {
      if (!code) {
        setState({ status: "not-found" });
        return;
      }
      setState({ status: "loading" });
      try {
        const response = await getPublishedUserGuide(code);
        if (cancelled) return;
        setState({ status: "ready", guide: toPresentation(response.data, response.data.code) });
      } catch (error) {
        if (cancelled) return;
        const message = error instanceof Error ? error.message : "Không tải được hướng dẫn.";
        if (/quyền|forbidden|403/i.test(message)) {
          setState({ status: "forbidden" });
        } else if (/hết hạn|expired|410/i.test(message)) {
          setState({ status: "unavailable", message });
        } else if (/không tồn tại|not found|404/i.test(message)) {
          setState({ status: "not-found" });
        } else {
          setState({ status: "unavailable", message });
        }
      }
    }
    void load();
    return () => {
      cancelled = true;
    };
  }, [code]);

  const handleLifecycleEvent = useCallback((event: UserGuideLifecycleEvent) => {
    if (!user) return;
    const guideId = event.guide.guideId;
    const eventPayload = { stepId: event.step.stepId, versionNumber: event.guide.versionNumber };
    switch (event.type) {
      case "OPENED":
        void recordUserGuideEvent(guideId, "STARTED", eventPayload).catch(() => undefined);
        break;
      case "STEP_CHANGED":
        void recordUserGuideEvent(guideId, "STEP_VIEWED", eventPayload).catch(() => undefined);
        break;
      case "COMPLETED":
        void recordUserGuideEvent(guideId, "COMPLETED", eventPayload).catch(() => undefined);
        break;
      case "DISMISSED":
        void recordUserGuideEvent(guideId, "DISMISSED", eventPayload).catch(() => undefined);
        break;
      default:
        break;
    }
  }, [user]);

  if (state.status === "loading") {
    return <p className="tw-py-16 tw-text-center tw-font-semibold tw-text-vm-slate-500">Đang tải hướng dẫn...</p>;
  }

  if (state.status === "not-found") {
    return <p className="tw-py-16 tw-text-center tw-font-bold tw-text-vm-slate-700">Không tìm thấy hướng dẫn.</p>;
  }

  if (state.status === "forbidden") {
    return <p className="tw-py-16 tw-text-center tw-font-bold tw-text-vm-slate-700">Bạn không thuộc đối tượng của hướng dẫn này.</p>;
  }

  if (state.status === "unavailable") {
    return <p className="tw-py-16 tw-text-center tw-font-bold tw-text-vm-slate-700">{state.message}</p>;
  }

  return (
    <UserGuideProvider onLifecycleEvent={handleLifecycleEvent}>
      <GuideReader guide={state.guide} />
      <UserGuideHost />
    </UserGuideProvider>
  );
}


