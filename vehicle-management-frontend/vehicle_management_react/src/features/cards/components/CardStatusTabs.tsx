import type { CardStatusTabValue } from "@/features/cards/components/cardManageData";
import { StatusTabs } from "@/shared/components/ui/StatusTabs";

interface CardStatusTabsProps {
  activeValue: CardStatusTabValue;
  counts: Record<CardStatusTabValue, number>;
  tabs: Array<{ value: CardStatusTabValue; label: string }>;
  onChange: (value: CardStatusTabValue) => void;
}

export function CardStatusTabs({ activeValue, counts, tabs, onChange }: CardStatusTabsProps) {
  return <StatusTabs activeValue={activeValue} ariaLabel="Card status tabs" className="tw-flex-1 tw-min-w-0" counts={counts} onChange={onChange} tabs={tabs} />;
}
