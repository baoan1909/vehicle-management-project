export const tokens = {
  colors: {
    brand: {
      primary: "#2563EB",
      hover: "#1D4ED8",
      soft: "#DBEAFE",
      contrast: "#FFFFFF",
    },
    text: {
      primary: "#0F172A",
      strong: "#111827",
      secondary: "#475569",
      muted: "#64748B",
      inverse: "#FFFFFF",
    },
    background: {
      app: "#F8FAFC",
      surface: "#FFFFFF",
      subtle: "#EFF6FF",
    },
    border: {
      default: "#D9E2F2",
      subtle: "#E2E8F0",
      focus: "#2563EB",
    },
    state: {
      success: "#16A34A",
      warning: "#F59E0B",
      danger: "#DC2626",
      info: "#2563EB",
    },
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
  radius: {
    sm: "6px",
    md: "8px",
    lg: "10px",
  },
  shadow: {
    soft: "0 14px 35px rgba(15, 23, 42, 0.08)",
    card: "0 10px 28px rgba(15, 23, 42, 0.04)",
    dropdown: "0 18px 42px rgba(15, 23, 42, 0.16)",
    drawer: "-24px 0 52px rgba(15, 23, 42, 0.16)",
    focus: "0 0 0 4px rgba(37, 99, 235, 0.1)",
  },
  fontSize: {
    pageTitle: "25px",
    sectionTitle: "18px",
    table: "0.88rem",
    badge: "0.74rem",
  },
  layout: {
    pageMaxWidth: 1440,
  },
} as const;

export type ThemeTokens = typeof tokens;
