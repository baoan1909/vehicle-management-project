import type { ButtonHTMLAttributes } from "react";

import { useToast } from "@/components/ui";
import { useUserGuideManualLauncher } from "@/features/user-guide/hooks/useUserGuideManualLauncher";
import { cn } from "@/lib/cn";

type Props = Omit<ButtonHTMLAttributes<HTMLButtonElement>, "onClick" | "type"> & {
  enabled?: boolean;
  guideCode: string;
  label?: string;
};

export function UserGuideHelpButton({
  className,
  enabled = true,
  guideCode,
  label = "Hướng dẫn & Trợ giúp",
  ...props
}: Props) {
  const toast = useToast();
  const { launchGuide, launchingCode } = useUserGuideManualLauncher();
  const isLaunching = launchingCode !== null;
  const { "aria-label": ariaLabel, ...buttonProps } = props;

  const handleOpen = async () => {
    if (isLaunching) return;

    try {
      await launchGuide(guideCode);
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Không tải được hướng dẫn đã phát hành.",
        "Không thể mở hướng dẫn",
      );
    }
  };

  return (
    <button
      {...buttonProps}
      aria-label={ariaLabel ?? `Mở lại ${label.toLocaleLowerCase("vi-VN")}`}
      aria-busy={isLaunching}
      className={cn(
        "tw-inline-flex tw-min-h-0 tw-origin-center tw-items-center tw-gap-2 tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.93rem] tw-font-bold tw-leading-[1.1] tw-text-vm-primary tw-transition-[color,transform,opacity] tw-duration-150 hover:tw-text-vm-primary-hover active:tw-scale-[0.97] active:tw-opacity-70 disabled:tw-cursor-not-allowed disabled:tw-opacity-50",
        className,
      )}
      disabled={!enabled}
      onClick={() => void handleOpen()}
      type="button"
    >
      <i className="far fa-question-circle tw-text-base" aria-hidden="true" />
      <span>{label}</span>
    </button>
  );
}
