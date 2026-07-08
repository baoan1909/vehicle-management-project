import type { CardInventoryStatus, CardLostState, CardSubscriptionState } from "@/features/cards/components/cardManageData";
import { cn } from "@/lib/cn";

type CardBadgeTone =
  | "inventory-available"
  | "inventory-assigned"
  | "inventory-in-use"
  | "inventory-pending"
  | "inventory-lost"
  | "inventory-blocked"
  | "subscription-none"
  | "subscription-active"
  | "subscription-pending"
  | "subscription-expired"
  | "lost-none"
  | "lost-open";

function getInventoryTone(value: CardInventoryStatus): CardBadgeTone {
  switch (value) {
    case "available":
      return "inventory-available";
    case "assigned":
      return "inventory-assigned";
    case "in_use":
      return "inventory-in-use";
    case "pending":
      return "inventory-pending";
    case "lost":
      return "inventory-lost";
    case "blocked":
      return "inventory-blocked";
  }
}

function getSubscriptionTone(value: CardSubscriptionState): CardBadgeTone {
  switch (value) {
    case "none":
      return "subscription-none";
    case "active":
      return "subscription-active";
    case "pending":
      return "subscription-pending";
    case "expired":
      return "subscription-expired";
  }
}

function getLostTone(value: CardLostState): CardBadgeTone {
  switch (value) {
    case "none":
      return "lost-none";
    case "open":
      return "lost-open";
  }
}

function getToneClass(tone: CardBadgeTone) {
  switch (tone) {
    case "inventory-available":
      return "tw-bg-sky-500/10 tw-text-sky-500";
    case "inventory-assigned":
      return "tw-bg-green-500/10 tw-text-green-600";
    case "inventory-in-use":
      return "tw-bg-brand-600/10 tw-text-brand-600";
    case "inventory-pending":
      return "tw-bg-amber-500/15 tw-text-amber-500";
    case "inventory-lost":
    case "lost-open":
      return "tw-bg-red-500/10 tw-text-red-500";
    case "inventory-blocked":
      return "tw-bg-slate-400/20 tw-text-slate-500";
    default:
      return "tw-bg-slate-100 tw-text-slate-600";
  }
}

interface CardStateBadgeProps {
  kind: "inventory" | "subscription" | "lost";
  label: string;
  value: CardInventoryStatus | CardSubscriptionState | CardLostState;
}

export function CardStateBadge({ kind, label, value }: CardStateBadgeProps) {
  const tone =
    kind === "inventory"
      ? getInventoryTone(value as CardInventoryStatus)
      : kind === "subscription"
        ? getSubscriptionTone(value as CardSubscriptionState)
        : getLostTone(value as CardLostState);

  return (
    <span className={cn("tw-inline-flex tw-min-h-6 tw-items-center tw-justify-center tw-gap-[0.35rem] tw-rounded-full tw-px-[0.56rem] tw-py-[0.22rem] tw-text-[0.74rem] tw-font-extrabold tw-tracking-[0.01em]", getToneClass(tone))}>
      <span className="tw-h-1.5 tw-w-1.5 tw-rounded-full tw-bg-current" />
      <span>{label}</span>
    </span>
  );
}
