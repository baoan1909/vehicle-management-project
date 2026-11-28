import { getEnumLabel } from "@/shared/utils/enumLabels";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import { useAuth } from "@/core/auth/useAuth";
import { useToast } from "@/components/ui";
import {
  resolveUserGuideJourney,
  type PostOnboardingJourneyStep,
  type PostOnboardingJourneyStepState,
  type PostOnboardingReadinessResponse,
} from "@/features/user-guide/api/userGuideApi";
import { useUserGuideManualLauncher } from "@/features/user-guide/hooks/useUserGuideManualLauncher";
import { cn } from "@/lib/cn";
import { Drawer } from "@/shared/components/ui/Drawer";
import { SelectMenu } from "@/shared/components/ui/SelectMenu";

type Props = {
  onClose: () => void;
  open: boolean;
};

const statePresentation: Record<PostOnboardingJourneyStepState, {
  icon: string;
  label: string;
  className: string;
}> = {
  ACTIONABLE: {
    icon: "fas fa-play",
    label: "Có thể làm ngay",
    className: "tw-border-blue-200 tw-bg-blue-50 tw-text-blue-800",
  },
  DEPENDENCY_REQUIRED: {
    icon: "fas fa-link",
    label: "Cần dependency trước",
    className: "tw-border-amber-200 tw-bg-amber-50 tw-text-amber-800",
  },
  WAITING_FOR_PERMISSION: {
    icon: "fas fa-user-lock",
    label: "Chờ người có quyền",
    className: "tw-border-violet-200 tw-bg-violet-50 tw-text-violet-800",
  },
  NOT_APPLICABLE: {
    icon: "fas fa-minus-circle",
    label: "Không áp dụng",
    className: "tw-border-slate-200 tw-bg-slate-50 tw-text-slate-600",
  },
  COMPLETED: {
    icon: "fas fa-check",
    label: "Đã hoàn thành",
    className: "tw-border-emerald-200 tw-bg-emerald-50 tw-text-emerald-800",
  },
  BLOCKED: {
    icon: "fas fa-exclamation-triangle",
    label: "Đang bị chặn",
    className: "tw-border-red-200 tw-bg-red-50 tw-text-red-800",
  },
};

export function PostOnboardingReadinessDrawer({ onClose, open }: Props) {
  const navigate = useNavigate();
  const toast = useToast();
  const { launchGuide, launchingCode } = useUserGuideManualLauncher();
  const { user } = useAuth();
  const [data, setData] = useState<PostOnboardingReadinessResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async (parkingLotId?: string | null, signal?: AbortSignal) => {
    setLoading(true);
    setError("");
    try {
      const response = await resolveUserGuideJourney(
        "OPERATIONS_ONBOARDING",
        "/ADMIN/HEADER",
        parkingLotId,
        signal,
      );
      setData(response.data);
    } catch (caught) {
      if (signal?.aborted) return;
      setData(null);
      setError(caught instanceof Error ? caught.message : "Không tải được lộ trình vận hành.");
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!open) return undefined;
    const controller = new AbortController();
    void load(null, controller.signal);
    return () => controller.abort();
  }, [load, open, user?.accessRevision]);

  const lotOptions = useMemo(() => (data?.parkingLots ?? []).map((lot) => ({
    label: `${lot.name} · ${getEnumLabel("parkingLotStatus", lot.status)}`,
    value: lot.parkingLotId,
  })), [data?.parkingLots]);

  const goToStep = (step: PostOnboardingJourneyStep) => {
    if (!step.route) return;
    onClose();
    navigate(step.route);
  };

  const openLinkedGuide = async (step: PostOnboardingJourneyStep) => {
    if (!step.linkedGuideCode) return;
    try {
      onClose();
      await launchGuide(step.linkedGuideCode);
    } catch (caught) {
      toast.error(caught instanceof Error ? caught.message : "Không mở được Coach mark liên kết.");
    }
  };

  const actionableCount = data?.steps.filter((step) => step.state === "ACTIONABLE").length ?? 0;
  const blockedCount = data?.steps.filter((step) => (
    step.state === "BLOCKED" || step.state === "WAITING_FOR_PERMISSION" || step.state === "DEPENDENCY_REQUIRED"
  )).length ?? 0;

  return (
    <Drawer
      open={open}
      onClose={onClose}
      title={data?.title ?? "Trợ giúp"}
      description={data?.summary ?? undefined}
      width="xl"
    >
      <div className="tw-grid tw-gap-4">
        {loading && !data ? (
          <div className="tw-flex tw-min-h-40 tw-items-center tw-justify-center tw-text-sm tw-font-semibold tw-text-vm-slate-500" role="status">
            <i className="fas fa-spinner fa-spin tw-mr-2" aria-hidden="true" />Đang kiểm tra dữ liệu thực tế…
          </div>
        ) : null}

        {error ? (
          <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-4 tw-text-sm tw-font-semibold tw-text-red-800" role="alert">
            <div><i className="fas fa-exclamation-circle tw-mr-2" aria-hidden="true" />{error}</div>
            <button type="button" className="tw-mt-3 tw-border-0 tw-bg-transparent tw-p-0 tw-font-extrabold tw-text-red-900" onClick={() => void load(data?.selectedParkingLotId)}>
              Thử tải lại
            </button>
          </div>
        ) : null}

        {data ? (
          <>
            <section className="tw-grid tw-grid-cols-[minmax(0,1fr)_auto] tw-items-end tw-gap-3 max-[600px]:tw-grid-cols-1">
              <div>
                <label className="tw-mb-1.5 tw-block tw-text-[0.76rem] tw-font-extrabold tw-text-vm-slate-700">Bãi xe đang đánh giá</label>
                {lotOptions.length > 0 ? (
                  <SelectMenu
                    ariaLabel="Chọn bãi xe để đánh giá"
                    disabled={loading}
                    onChange={(value) => void load(value)}
                    options={lotOptions}
                    portal
                    searchable="auto"
                    value={data.selectedParkingLotId ?? lotOptions[0]?.value ?? ""}
                  />
                ) : (
                  <div className="tw-flex tw-h-[42px] tw-items-center tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-px-3 tw-text-[0.8rem] tw-font-semibold tw-text-amber-800">
                    Chưa có bãi trong phạm vi hiện tại.
                  </div>
                )}
              </div>
              <button
                type="button"
                className="tw-h-[42px] tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-px-4 tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-700 hover:tw-bg-slate-50 disabled:tw-opacity-50"
                disabled={loading}
                onClick={() => void load(data.selectedParkingLotId)}
              >
                <i className={cn("fas fa-sync-alt tw-mr-2", loading ? "fa-spin" : "")} aria-hidden="true" />Kiểm tra lại
              </button>
            </section>

            <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[520px]:tw-grid-cols-1">
              <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-blue-100 tw-bg-blue-50 tw-p-3">
                <span className="tw-text-[0.72rem] tw-font-extrabold tw-uppercase tw-tracking-wide tw-text-blue-700">Có thể làm ngay</span>
                <strong className="tw-mt-1 tw-block tw-text-xl tw-font-black tw-text-blue-900">{actionableCount}</strong>
              </div>
              <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-100 tw-bg-amber-50 tw-p-3">
                <span className="tw-text-[0.72rem] tw-font-extrabold tw-uppercase tw-tracking-wide tw-text-amber-700">Cần xử lý hoặc chờ</span>
                <strong className="tw-mt-1 tw-block tw-text-xl tw-font-black tw-text-amber-900">{blockedCount}</strong>
              </div>
            </div>

            <ol className="tw-m-0 tw-grid tw-list-none tw-gap-3 tw-p-0">
              {data.steps.map((step) => {
                const state = statePresentation[step.state];
                return (
                  <li key={step.stepKey} className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-4">
                    <div className="tw-flex tw-items-start tw-gap-3">
                      <span className="tw-flex tw-h-8 tw-w-8 tw-flex-none tw-items-center tw-justify-center tw-rounded-full tw-bg-slate-100 tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">
                        {step.displayOrder}
                      </span>
                      <div className="tw-min-w-0 tw-flex-1">
                        <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-2">
                          <h4 className="tw-m-0 tw-text-[0.92rem] tw-font-black tw-text-vm-slate-900">{step.title}</h4>
                          <span className={cn("tw-inline-flex tw-items-center tw-gap-1.5 tw-rounded-full tw-border tw-border-solid tw-px-2.5 tw-py-1 tw-text-[0.68rem] tw-font-extrabold", state.className)}>
                            <i className={state.icon} aria-hidden="true" />{state.label}
                          </span>
                        </div>
                        <p className="tw-mb-0 tw-mt-2 tw-text-[0.79rem] tw-font-medium tw-leading-5 tw-text-vm-slate-600">{step.description}</p>
                        {step.ownerHint ? (
                          <p className="tw-mb-0 tw-mt-2 tw-text-[0.75rem] tw-font-bold tw-text-violet-700">
                            <i className="fas fa-user-shield tw-mr-1.5" aria-hidden="true" />Cần: {step.ownerHint}
                          </p>
                        ) : null}
                        {step.route ? (
                          <button
                            type="button"
                            className="tw-mt-3 tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.78rem] tw-font-extrabold tw-text-vm-primary hover:tw-underline"
                            onClick={() => goToStep(step)}
                          >
                            {step.actionLabel ?? "Đến màn hình phù hợp"} <i className="fas fa-arrow-right tw-ml-1" aria-hidden="true" />
                          </button>
                        ) : null}
                        {step.linkedGuideCode ? (
                          <button
                            type="button"
                            className="tw-ml-4 tw-mt-3 tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.78rem] tw-font-extrabold tw-text-vm-primary hover:tw-underline disabled:tw-opacity-50"
                            disabled={launchingCode !== null}
                            onClick={() => void openLinkedGuide(step)}
                          >
                            <i className="far fa-question-circle tw-mr-1" aria-hidden="true" />Xem hướng dẫn thao tác
                          </button>
                        ) : null}
                      </div>
                    </div>
                  </li>
                );
              })}
            </ol>
            {data.steps.length === 0 ? (
              <div className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-6 tw-text-center tw-text-sm tw-font-semibold tw-text-vm-slate-500">
                Lộ trình đã phát hành chưa có bước phù hợp.
              </div>
            ) : null}
          </>
        ) : null}
      </div>
    </Drawer>
  );
}
