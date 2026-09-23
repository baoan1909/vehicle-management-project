/** Decorative line drawing, shared by the monthly ticket summaries. */
export function PortalTicketArtwork({ className = "" }: { className?: string }) {
  return (
    <svg aria-hidden="true" className={className} fill="none" viewBox="0 0 320 140">
      <g stroke="currentColor" strokeWidth="1">
        <path d="M8 127h304M15 121V64l58-19 48 18v58M22 119V70l51-17 41 15v51M73 53v67M23 78l42-13m-42 24 42-13m-42 24 42-13m15-19 25 9m-25 3 25 9m-25 3 25 9M127 121V39l104-24 69 28v78M135 117V45l96-22 61 25v69M231 24v92M141 53l81-19v14l-81 18zm0 26 81-19v14l-81 18zm0 26 81-19v14l-81 18zM239 37l44 17v14l-44-16zm0 29 44 17v14l-44-16z" />
        <path d="M31 127v-18h30v18m-24-17v16m8-16v16m8-16v16M256 115V95h24v20m-20-9V99h10a4 4 0 0 1 0 8h-10m0 0v6M96 128v-9l7-3 6-13h30l9 13 9 3v9m-53-12h42m-37-3 4-7h21l6 7m-37 7h8m31 0h8m-42 8a4 4 0 1 0 8 0m27 0a4 4 0 1 0 8 0" />
        <path d="M7 132c74 8 179-13 306-3M10 137c94 4 159-6 300-5" opacity=".6" />
      </g>
    </svg>
  );
}

