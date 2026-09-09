import { useId } from "react";

/** Inset gold rules follow the clipped corners; both ends have perforated edges. */
export function PortalTicketFrame({ variant = "full" }: { variant?: "full" | "compact" }) {
  const maskId = useId();

  if (variant === "compact") {
    return (
      <svg aria-hidden="true" className="tw-pointer-events-none tw-absolute tw-inset-0 tw-h-full tw-w-full" fill="none" preserveAspectRatio="none" viewBox="0 0 600 78">
        <defs>
          <mask id={maskId} maskUnits="userSpaceOnUse" x="0" y="0" width="600" height="78">
            <rect width="600" height="78" rx="5" fill="white" />
            {[18, 30, 42, 54, 66].map((y) => (
              <g fill="black" key={y}><circle cx="0" cy={y} r="4" /><circle cx="600" cy={y} r="4" /></g>
            ))}
          </mask>
        </defs>
        <rect width="600" height="78" fill="#031942" mask={`url(#${maskId})`} />
      </svg>
    );
  }

  return (
    <svg aria-hidden="true" className="tw-pointer-events-none tw-absolute tw-inset-0 tw-h-full tw-w-full" fill="none" preserveAspectRatio="none" viewBox="0 0 760 200">
      <defs>
        <mask id={maskId} maskUnits="userSpaceOnUse" x="0" y="0" width="760" height="200">
          <rect width="760" height="200" fill="white" />
          <g fill="black">
            <circle cx="0" cy="0" r="14" />
            <circle cx="760" cy="0" r="14" />
            <circle cx="0" cy="200" r="14" />
            <circle cx="760" cy="200" r="14" />
            {[34, 56, 78, 100, 122, 144, 166].map((y) => (
              <g key={y}><circle cx="0" cy={y} r="5" /><circle cx="760" cy={y} r="5" /></g>
            ))}
          </g>
        </mask>
      </defs>
      <g mask={`url(#${maskId})`}>
        <rect width="760" height="200" fill="#031735" />
        <rect x=".5" y=".5" width="759" height="199" stroke="#173759" vectorEffect="non-scaling-stroke" />
        <path d="M24 9H736A10 10 0 0 0 746 19V181A10 10 0 0 0 736 191H24A10 10 0 0 0 14 181V19A10 10 0 0 0 24 9Z" stroke="#c9ad64" strokeWidth=".85" vectorEffect="non-scaling-stroke" />
        <path d="M28 15H732A12 12 0 0 0 740 23V177A12 12 0 0 0 732 185H28A12 12 0 0 0 20 177V23A12 12 0 0 0 28 15Z" stroke="#6b7180" strokeOpacity=".35" strokeWidth=".65" vectorEffect="non-scaling-stroke" />
      </g>
    </svg>
  );
}
