import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { cn } from "@/lib/cn";
import { NotificationBell } from "@/features/notifications/components/NotificationBell";
import { ChatInboxButton } from "@/features/support/components/ChatInboxButton";
import { AdminAccountMenu } from "./AdminAccountMenu";

const searchSuggestions = ["Tìm thẻ xe", "Tra cứu khách hàng", "Kiểm tra xe đang trong bãi"];

export function AdminHeader() {
  const [searchValue, setSearchValue] = useState("");
  const [searchOpen, setSearchOpen] = useState(false);
  const searchRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      const target = event.target as Node;

      if (searchRef.current && !searchRef.current.contains(target)) setSearchOpen(false);
    }

    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, []);

  function handleSuggestionSelect(value: string) {
    setSearchValue(value);
    setSearchOpen(false);
  }

  return (
    <header className="tw-fixed tw-inset-x-0 tw-top-0 tw-z-[1050] tw-border-0 tw-border-b tw-border-solid tw-border-slate-200/95 tw-bg-white/95 tw-shadow-[0_10px_28px_rgba(15,23,42,0.08)] tw-backdrop-blur-[14px]">
      <div className="tw-grid tw-min-h-[72px] tw-grid-cols-[240px_minmax(280px,1fr)_auto] tw-items-center tw-gap-6 tw-px-6 max-[768px]:tw-grid-cols-[minmax(0,1fr)_auto] max-[768px]:tw-gap-4 max-[768px]:tw-px-4">
        <div className="tw-min-w-0">
          <Link to="/admin/dashboard" className="tw-flex tw-min-w-0 tw-items-center tw-gap-3 tw-text-slate-900 hover:tw-text-slate-900 hover:tw-no-underline">
            <span className="tw-inline-flex tw-h-12 tw-w-12 tw-flex-shrink-0 tw-items-center tw-justify-center">
              <img className="tw-block tw-h-12 tw-w-12 tw-object-contain" src="/assets/admin/dist/img/AdminLTELogo.png" alt="CoParking" />
            </span>
            <span className="tw-flex tw-min-w-0 tw-flex-col max-[768px]:tw-hidden">
              <strong className="tw-text-[1.35rem] tw-font-extrabold tw-leading-none tw-text-vm-primary">CoParking</strong>
              <small className="tw-mt-1 tw-text-[0.74rem] tw-font-bold tw-uppercase tw-tracking-[0.08em] tw-text-vm-slate-500">Admin Portal</small>
            </span>
          </Link>
        </div>

        <div className="tw-flex tw-justify-center max-[768px]:tw-hidden">
          <div ref={searchRef} className="tw-relative tw-w-[min(100%,520px)]">
            <div
              className={cn(
                "tw-flex tw-min-h-11 tw-items-center tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-px-4 tw-shadow-[0_8px_18px_rgba(15,23,42,0.04)] tw-transition",
                searchOpen ? "tw-border-brand-200 tw-shadow-[0_0_0_4px_rgba(37,99,235,0.08)]" : "",
              )}
            >
              <i className="fas fa-search tw-text-vm-slate-500" />
              <input
                className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.9rem] tw-font-semibold tw-text-slate-900 tw-outline-none placeholder:tw-text-vm-slate-500"
                type="search"
                value={searchValue}
                placeholder="Tìm khách hàng, biển số, thẻ xe..."
                aria-label="Tìm kiếm"
                onFocus={() => setSearchOpen(true)}
                onChange={(event) => setSearchValue(event.target.value)}
              />
              <span className="tw-rounded-vm-sm tw-bg-slate-100 tw-px-2 tw-py-1 tw-text-[0.7rem] tw-font-extrabold tw-text-vm-slate-500">Ctrl+K</span>
            </div>

            {searchOpen ? (
              <div className="tw-absolute tw-left-0 tw-right-0 tw-top-[calc(100%+10px)] tw-z-[1080] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-p-2 tw-shadow-[0_18px_42px_rgba(15,23,42,0.16)]">
                <div className="tw-px-3 tw-py-2 tw-text-[0.78rem] tw-font-extrabold tw-uppercase tw-tracking-[0.04em] tw-text-vm-slate-500">Gợi ý tìm kiếm</div>
                <div className="tw-grid tw-gap-1">
                  {searchSuggestions.map((suggestion) => (
                    <button
                      key={suggestion}
                      type="button"
                      className="tw-flex tw-min-h-10 tw-w-full tw-items-center tw-gap-3 tw-rounded-vm-md tw-border-0 tw-bg-white tw-px-3 tw-text-left tw-text-[0.88rem] tw-font-bold tw-text-vm-slate-700 hover:tw-bg-brand-50 hover:tw-text-vm-primary"
                      onClick={() => handleSuggestionSelect(suggestion)}
                    >
                      <i className="fas fa-arrow-up-right-from-square" />
                      <span>{suggestion}</span>
                    </button>
                  ))}
                </div>
              </div>
            ) : null}
          </div>
        </div>

        <div className="tw-flex tw-items-center tw-justify-end tw-gap-[1.1rem]">
          <ChatInboxButton />
          <NotificationBell variant="admin" />

          <AdminAccountMenu />
        </div>
      </div>
    </header>
  );
}
