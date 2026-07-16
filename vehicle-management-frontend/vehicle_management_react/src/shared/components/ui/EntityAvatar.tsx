import { useEffect, useState, type HTMLAttributes } from "react";

import { cn } from "@/lib/cn";
import { resolvePublicMediaUrl } from "@/shared/utils/mediaUrl";

type EntityAvatarSize = "sm" | "md" | "lg" | "xl";

type EntityAvatarProps = HTMLAttributes<HTMLDivElement> & {
  initials: string;
  size?: EntityAvatarSize;
  src?: string | null;
  tone?: "blue" | "green" | "amber" | "red" | "violet";
};

const sizeClassName: Record<EntityAvatarSize, string> = {
  sm: "tw-h-8 tw-w-8 tw-text-[0.72rem]",
  md: "tw-h-10 tw-w-10 tw-text-[0.82rem]",
  lg: "tw-h-14 tw-w-14 tw-text-[1.05rem]",
  xl: "tw-h-16 tw-w-16 tw-text-[1.45rem]",
};

const toneClassName: Record<NonNullable<EntityAvatarProps["tone"]>, string> = {
  blue: "tw-bg-brand-100 tw-text-vm-primary",
  green: "tw-bg-green-50 tw-text-green-700",
  amber: "tw-bg-amber-50 tw-text-amber-700",
  red: "tw-bg-red-50 tw-text-vm-danger",
  violet: "tw-bg-violet-50 tw-text-violet-700",
};

export function EntityAvatar({ className, initials, size = "md", src, tone = "blue", ...props }: EntityAvatarProps) {
  const [imageFailed, setImageFailed] = useState(false);
  const imageSrc = resolvePublicMediaUrl(src);
  const shouldShowImage = Boolean(imageSrc) && !imageFailed;

  useEffect(() => {
    setImageFailed(false);
  }, [imageSrc]);

  return (
    <div
      className={cn(
        "tw-inline-flex tw-flex-shrink-0 tw-items-center tw-justify-center tw-overflow-hidden tw-rounded-full tw-font-extrabold",
        sizeClassName[size],
        shouldShowImage ? "tw-bg-vm-slate-100 tw-text-transparent" : toneClassName[tone],
        className,
      )}
      {...props}
    >
      {shouldShowImage ? (
        <img
          alt=""
          className="tw-h-full tw-w-full tw-object-cover"
          src={imageSrc}
          onError={() => setImageFailed(true)}
        />
      ) : (
        initials
      )}
    </div>
  );
}
