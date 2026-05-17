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
      focus: "#2563EB",
    },
    state: {
      success: "#16A34A",
      warning: "#D97706",
      danger: "#DC2626",
      info: "#2563EB",
    },
  },
  radius: {
    sm: "6px",
    md: "10px",
    lg: "14px",
  },
  shadow: {
    soft: "0 14px 35px rgba(15, 23, 42, 0.08)",
  },
  layout: {
    pageMaxWidth: 1440,
  },
} as const;

export type ThemeTokens = typeof tokens;
