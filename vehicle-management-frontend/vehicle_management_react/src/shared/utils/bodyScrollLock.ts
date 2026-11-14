let activeBodyScrollLocks = 0;
let originalBodyOverflow: string | null = null;

/**
 * Locks document scrolling while one or more overlays are open.
 *
 * A shared counter is required because drawers can open confirmation modals.
 * Restoring each overlay's locally captured overflow value can otherwise leave
 * the page permanently locked when multiple overlays close in the same render.
 */
export function acquireBodyScrollLock() {
  if (typeof document === "undefined") return () => undefined;

  if (activeBodyScrollLocks === 0) {
    originalBodyOverflow = document.body.style.overflow;
  }
  activeBodyScrollLocks += 1;
  document.body.style.overflow = "hidden";

  let released = false;
  return () => {
    if (released) return;
    released = true;
    activeBodyScrollLocks = Math.max(0, activeBodyScrollLocks - 1);
    if (activeBodyScrollLocks !== 0) return;

    document.body.style.overflow = originalBodyOverflow ?? "";
    originalBodyOverflow = null;
  };
}
