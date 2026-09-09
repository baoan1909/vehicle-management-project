import { useId } from "react";

/** The two side notches are transparent and their outlines follow the ticket edge. */
export function SubscriptionTicketFrame() {
  const id = useId();
  const edge = "M18 2H662Q678 2 678 18V109C653 113 653 151 678 155V278Q678 294 662 294H18Q2 294 2 278V155C27 151 27 113 2 109V18Q2 2 18 2Z";
  return (
    <svg aria-hidden="true" className="tw-pointer-events-none tw-absolute tw-inset-0 tw-h-full tw-w-full" fill="none" viewBox="0 0 680 296" preserveAspectRatio="none">
      <defs>
        <linearGradient id={`${id}-surface`} x1="0" y1="0" x2="680" y2="296" gradientUnits="userSpaceOnUse">
          <stop stopColor="#051c40" /><stop offset=".6" stopColor="#021733" /><stop offset="1" stopColor="#031a3d" />
        </linearGradient>
        <linearGradient id={`${id}-edge`} x1="0" y1="296" x2="680" y2="180" gradientUnits="userSpaceOnUse">
          <stop stopColor="#667b82" /><stop offset=".24" stopColor="#456789" /><stop offset=".65" stopColor="#1753a4" /><stop offset="1" stopColor="#0765ff" />
        </linearGradient>
        <linearGradient id={`${id}-inset`} x1="0" y1="0" x2="680" y2="0" gradientUnits="userSpaceOnUse">
          <stop stopColor="#d8bd77" stopOpacity=".55" /><stop offset=".5" stopColor="#68839c" stopOpacity=".1" /><stop offset="1" stopColor="#5289e5" stopOpacity=".65" />
        </linearGradient>
      </defs>
      <path d={edge} fill={`url(#${id}-surface)`} stroke={`url(#${id}-edge)`} strokeWidth="4" vectorEffect="non-scaling-stroke" />
      <path d="M19 7H660Q673 7 673 20V105C646 112 646 152 673 159V276Q673 289 660 289H20Q7 289 7 276V159C34 152 34 112 7 105V20Q7 7 19 7Z" stroke={`url(#${id}-inset)`} strokeWidth=".8" vectorEffect="non-scaling-stroke" />
      <path d="M354 3V293" stroke="#bdc7d4" strokeOpacity=".3" strokeDasharray="3 3" vectorEffect="non-scaling-stroke" />
    </svg>
  );
}
