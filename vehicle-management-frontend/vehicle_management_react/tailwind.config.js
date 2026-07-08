/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  prefix: "tw-",
  corePlugins: {
    preflight: false,
  },
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#eff6ff",
          100: "#dbeafe",
          200: "#bfdbfe",
          500: "#2563eb",
          600: "#1d4ed8",
          700: "#1e40af",
        },
        vm: {
          primary: "#2563EB",
          "primary-hover": "#1D4ED8",
          success: "#16A34A",
          warning: "#F59E0B",
          danger: "#DC2626",
          info: "#2563EB",
          surface: "#FFFFFF",
          canvas: "#F8FAFC",
          border: "#D9E2F2",
          slate: {
            25: "#F8FAFC",
            50: "#F1F5F9",
            100: "#E2E8F0",
            200: "#CBD5E1",
            500: "#64748B",
            700: "#334155",
            900: "#0F172A",
          },
        },
      },
      borderRadius: {
        vm: "8px",
        "vm-sm": "6px",
        "vm-md": "8px",
        "vm-lg": "10px",
      },
      fontSize: {
        "vm-page-title": ["25px", { lineHeight: "1.2", fontWeight: "700" }],
        "vm-section-title": ["18px", { lineHeight: "1.35", fontWeight: "700" }],
        "vm-table": ["0.88rem", { lineHeight: "1.45" }],
        "vm-badge": ["0.74rem", { lineHeight: "1.2", fontWeight: "700" }],
      },
      boxShadow: {
        soft: "0 14px 35px rgba(15, 23, 42, 0.08)",
        "vm-card": "0 10px 28px rgba(15, 23, 42, 0.04)",
        "vm-dropdown": "0 18px 42px rgba(15, 23, 42, 0.16)",
        "vm-drawer": "-24px 0 52px rgba(15, 23, 42, 0.16)",
        "vm-focus": "0 0 0 4px rgba(37, 99, 235, 0.1)",
      },
      keyframes: {
        "vm-modal-enter": {
          from: { opacity: "0", transform: "translateY(10px) scale(0.98)" },
          to: { opacity: "1", transform: "translateY(0) scale(1)" },
        },
        "vm-drawer-panel-in": {
          from: { transform: "translate3d(104%, 0, 0)" },
          to: { transform: "translate3d(0, 0, 0)" },
        },
        "vm-drawer-panel-out": {
          from: { transform: "translate3d(0, 0, 0)" },
          to: { transform: "translate3d(104%, 0, 0)" },
        },
        "vm-drawer-backdrop-in": {
          from: { opacity: "0" },
          to: { opacity: "1" },
        },
        "vm-drawer-backdrop-out": {
          from: { opacity: "1" },
          to: { opacity: "0" },
        },
      },
      animation: {
        "vm-modal-enter": "vm-modal-enter 0.22s ease both",
        "vm-drawer-panel-in": "vm-drawer-panel-in 0.28s cubic-bezier(0.22, 1, 0.36, 1) both",
        "vm-drawer-panel-out": "vm-drawer-panel-out 0.24s cubic-bezier(0.4, 0, 0.2, 1) both",
        "vm-drawer-backdrop-in": "vm-drawer-backdrop-in 0.2s ease-out both",
        "vm-drawer-backdrop-out": "vm-drawer-backdrop-out 0.2s ease-in both",
      },
    },
  },
  plugins: [],
};
