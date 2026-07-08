import type { ReactNode } from "react";

import { cn } from "@/lib/cn";

type InfoBannerTone = "warning" | "info" | "success";

type InfoBannerProps = {
  action?: ReactNode;
  className?: string;
  description: string;
  icon?: ReactNode;
  title: string;
  tone?: InfoBannerTone;
};

const toneClassName: Record<InfoBannerTone, string> = {
  warning: "tw-border-amber-200 tw-bg-amber-50/70 tw-text-amber-700",
  info: "tw-border-brand-100 tw-bg-brand-50/80 tw-text-vm-primary",
  success: "tw-border-green-100 tw-bg-green-50/80 tw-text-green-700",
};

export function InfoBanner({ action, className, description, icon, title, tone = "info" }: InfoBannerProps) {
  return (
    <section
      className={cn(
        "tw-flex tw-items-center tw-justify-between tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-px-4 tw-py-3",
        toneClassName[tone],
        className,
      )}
    >
      <div className="tw-flex tw-min-w-0 tw-items-start tw-gap-3">
        {icon ? <span className="tw-mt-0.5 tw-inline-flex tw-flex-shrink-0 tw-text-[1rem]">{icon}</span> : null}
        <div className="tw-min-w-0">
          <h2 className="tw-m-0 tw-text-[0.94rem] tw-font-extrabold tw-text-vm-slate-900">{title}</h2>
          <p className="tw-m-0 tw-mt-1 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">{description}</p>
        </div>
      </div>
      {action ? <div className="tw-flex-shrink-0">{action}</div> : null}
    </section>
  );
}
