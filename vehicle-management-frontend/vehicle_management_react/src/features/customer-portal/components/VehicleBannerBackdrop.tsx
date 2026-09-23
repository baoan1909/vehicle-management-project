import { useId } from "react";

export function VehicleBannerBackdrop() {
  const id = useId();

  return (
    <svg aria-hidden="true" className="tw-pointer-events-none tw-absolute tw-inset-0 tw-h-full tw-w-full" fill="none" preserveAspectRatio="none" viewBox="0 0 720 252">
      <defs>
        <pattern id={`${id}-grid`} width="42" height="34" patternUnits="userSpaceOnUse">
          <path d="M42 0H0V34" stroke="#8faed2" strokeOpacity=".2" strokeWidth=".7" />
        </pattern>
        <linearGradient id={`${id}-fade`} x1="220" y1="225" x2="660" y2="30" gradientUnits="userSpaceOnUse">
          <stop stopColor="white" stopOpacity="0" />
          <stop offset=".5" stopColor="white" stopOpacity=".45" />
          <stop offset="1" stopColor="white" />
        </linearGradient>
        <mask id={`${id}-mask`}>
          <rect width="720" height="252" fill={`url(#${id}-fade)`} />
        </mask>
        <linearGradient id={`${id}-gold`} x1="130" y1="190" x2="620" y2="0" gradientUnits="userSpaceOnUse">
          <stop stopColor="#d6b75d" stopOpacity=".06" />
          <stop offset=".55" stopColor="#c7a549" stopOpacity=".28" />
          <stop offset="1" stopColor="#c7a549" stopOpacity=".65" />
        </linearGradient>
      </defs>
      <g mask={`url(#${id}-mask)`}>
        <rect x="230" width="490" height="252" fill={`url(#${id}-grid)`} />
        <path d="M291 172V64h47v108m14-34V42h63v115m20-57V16h72v128m22-39V54h43v118m30-33V1h48v180m-374-71h414M308 82h376M399 49h271" stroke="#9db7d5" strokeOpacity=".22" strokeWidth=".8" vectorEffect="non-scaling-stroke" />
      </g>
      <path d="M137 184C271 78 441 8 636-10M152 187C310 288 493 289 703 254" stroke={`url(#${id}-gold)`} strokeWidth="1" vectorEffect="non-scaling-stroke" />
      <rect x="3" y="3" width="714" height="246" rx="10" stroke="white" strokeOpacity=".8" strokeWidth=".8" vectorEffect="non-scaling-stroke" />
    </svg>
  );
}
