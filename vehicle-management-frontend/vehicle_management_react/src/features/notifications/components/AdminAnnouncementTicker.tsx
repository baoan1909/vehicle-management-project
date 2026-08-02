import { useCallback, useEffect, useMemo, useState } from "react";
import { getBroadcastAnnouncements, type BroadcastAnnouncementResponse } from "@/features/notifications/api/notificationApi";

type AdminAnnouncementTickerProps = {
  onVisibleChange: (visible: boolean) => void;
};

function getTime(value: string | null | undefined) {
  if (!value) return 0;
  const time = new Date(value).getTime();
  return Number.isNaN(time) ? 0 : time;
}

function isActiveAnnouncement(announcement: BroadcastAnnouncementResponse, now: number) {
  if (announcement.status !== "PUBLISHED") return false;
  if (announcement.enabled === false) return false;

  const startAt = getTime(announcement.startAt);
  const endAt = getTime(announcement.endAt);

  if (startAt && startAt > now) return false;
  if (endAt && endAt < now) return false;
  return true;
}

function getDisplayOrder(announcement: BroadcastAnnouncementResponse) {
  return announcement.displayOrder ?? 100;
}

function sortActiveAnnouncements(left: BroadcastAnnouncementResponse, right: BroadcastAnnouncementResponse) {
  return (
    getDisplayOrder(left) - getDisplayOrder(right) ||
    getTime(left.startAt) - getTime(right.startAt) ||
    getTime(left.publishedAt) - getTime(right.publishedAt) ||
    getTime(left.createdAt) - getTime(right.createdAt) ||
    left.broadcastId.localeCompare(right.broadcastId)
  );
}

function TickerItems({ announcements }: { announcements: BroadcastAnnouncementResponse[] }) {
  return (
    <span className="tw-inline-flex tw-items-center tw-gap-7 tw-pr-7">
      {announcements.map((announcement) => (
        <span className="tw-inline-flex tw-items-center tw-gap-3 tw-rounded-full tw-bg-white/75 tw-py-1.5 tw-pl-3 tw-pr-4 tw-ring-1 tw-ring-[#d4e3f4]/80 tw-backdrop-blur" key={announcement.broadcastId}>
          <span className="tw-h-2 tw-w-2 tw-flex-none tw-rounded-full tw-bg-[#2f80ed] tw-shadow-[0_0_0_4px_rgba(47,128,237,0.12)]" />
          <strong className="tw-text-[0.86rem] tw-font-black tw-text-[#16324f]">{announcement.title}</strong>
          <span className="tw-h-4 tw-w-px tw-bg-[#c9d9ec]" />
          <span className="tw-max-w-[760px] tw-truncate tw-text-[0.84rem] tw-font-semibold tw-text-[#60758c]">
            {announcement.message}
          </span>
        </span>
      ))}
    </span>
  );
}

export function AdminAnnouncementTicker({ onVisibleChange }: AdminAnnouncementTickerProps) {
  const [announcements, setAnnouncements] = useState<BroadcastAnnouncementResponse[]>([]);
  const [now, setNow] = useState(() => Date.now());

  const loadAnnouncements = useCallback(async () => {
    try {
      const response = await getBroadcastAnnouncements();
      setAnnouncements(response.data ?? []);
    } catch {
      setAnnouncements([]);
    }
  }, []);

  useEffect(() => {
    void loadAnnouncements();
    const refreshIntervalId = window.setInterval(() => void loadAnnouncements(), 60_000);
    const clockIntervalId = window.setInterval(() => setNow(Date.now()), 30_000);

    return () => {
      window.clearInterval(refreshIntervalId);
      window.clearInterval(clockIntervalId);
    };
  }, [loadAnnouncements]);

  const activeAnnouncements = useMemo(
    () => announcements.filter((announcement) => isActiveAnnouncement(announcement, now)).sort(sortActiveAnnouncements),
    [announcements, now],
  );
  const visible = activeAnnouncements.length > 0;
  const duration = Math.min(56, Math.max(18, activeAnnouncements.length * 12));

  useEffect(() => {
    onVisibleChange(visible);
  }, [onVisibleChange, visible]);

  if (!visible) return null;

  return (
    <div className="tw-fixed tw-inset-x-0 tw-top-[72px] tw-z-[1045] tw-h-10 tw-overflow-hidden tw-border-0 tw-border-y tw-border-solid tw-border-[#d3e3f4]/90 tw-bg-[linear-gradient(90deg,#eaf4ff_0%,#f6faff_42%,#edf7f4_100%)] tw-shadow-[0_8px_22px_rgba(31,78,121,0.09)]">
      <style>
        {`
          @keyframes vm-admin-announcement-marquee {
            from { transform: translateX(0); }
            to { transform: translateX(-50%); }
          }
        `}
      </style>
      <div className="tw-flex tw-h-full tw-items-center tw-gap-3 tw-bg-[linear-gradient(180deg,rgba(255,255,255,0.38),rgba(255,255,255,0.08))]">
        <span className="tw-relative tw-z-[1] tw-flex tw-h-full tw-flex-none tw-items-center tw-gap-2.5 tw-border-0 tw-border-r tw-border-solid tw-border-[#c8dbef]/90 tw-bg-white/70 tw-px-5 tw-text-[0.78rem] tw-font-black tw-uppercase tw-tracking-normal tw-text-[#17446f] tw-backdrop-blur">
          <span className="tw-inline-flex tw-h-6 tw-w-6 tw-items-center tw-justify-center tw-rounded-full tw-bg-[#d8eaff] tw-text-[#2563eb] tw-ring-1 tw-ring-[#c4daf5]">
            <i className="fas fa-bullhorn tw-text-[0.72rem]" />
          </span>
          Thông báo
        </span>
        <div className="tw-relative tw-mr-2 tw-min-w-0 tw-flex-1 tw-overflow-hidden tw-rounded-full">
          <span className="tw-pointer-events-none tw-absolute tw-inset-y-0 tw-left-0 tw-z-[2] tw-w-14 tw-rounded-l-full tw-bg-[linear-gradient(90deg,#f6faff_0%,rgba(246,250,255,0)_100%)]" />
          <span className="tw-pointer-events-none tw-absolute tw-inset-y-0 tw-right-0 tw-z-[2] tw-w-14 tw-rounded-r-full tw-bg-[linear-gradient(270deg,#edf7f4_0%,rgba(237,247,244,0)_100%)]" />
          <div
            className="tw-inline-flex tw-w-max tw-items-center tw-whitespace-nowrap tw-will-change-transform"
            style={{ animation: `vm-admin-announcement-marquee ${duration}s linear infinite` }}
          >
            <TickerItems announcements={activeAnnouncements} />
            <TickerItems announcements={activeAnnouncements} />
          </div>
        </div>
      </div>
    </div>
  );
}
