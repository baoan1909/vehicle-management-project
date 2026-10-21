/**
 * Maps the current route to the user-guide feature convention used by the
 * backend. Feature codes themselves are admin-defined data; this table only
 * provides the default code per application section so the runtime host can
 * resolve guides without each page wiring it manually.
 *
 * Unknown routes resolve to `null` and skip fetching entirely.
 */
const routeFeatureMap: Array<{ prefix: string; featureCode: string }> = [
  { prefix: "/admin/dashboard", featureCode: "DASHBOARD" },
  { prefix: "/api/dashboard", featureCode: "DASHBOARD" },
  { prefix: "/admin/swipe", featureCode: "PARKING_SESSION" },
  { prefix: "/admin/card", featureCode: "CARD_MANAGEMENT" },
  { prefix: "/admin/lost", featureCode: "CARD_MANAGEMENT" },
  { prefix: "/admin/parking-lots", featureCode: "PARKING_LOT" },
  { prefix: "/admin/parking-structure", featureCode: "PARKING_LOT" },
  { prefix: "/admin/parking-layouts", featureCode: "PARKING_LOT" },
  { prefix: "/admin/parking-live", featureCode: "PARKING_LOT" },
  { prefix: "/admin/devices", featureCode: "DEVICE" },
  { prefix: "/admin/price-plans", featureCode: "PRICING" },
  { prefix: "/admin/price-rules", featureCode: "PRICING" },
  { prefix: "/admin/vouchers", featureCode: "VOUCHER" },
  { prefix: "/admin/invoices", featureCode: "INVOICE" },
  { prefix: "/admin/platform-fees", featureCode: "PLATFORM_FEE" },
  { prefix: "/admin/wallets", featureCode: "WALLET" },
  { prefix: "/admin/partner-wallet", featureCode: "WALLET" },
  { prefix: "/admin/employee", featureCode: "EMPLOYEE" },
  { prefix: "/admin/shifts", featureCode: "EMPLOYEE" },
  { prefix: "/admin/account", featureCode: "IAM_ACCOUNT" },
  { prefix: "/admin/partner-registrations", featureCode: "IAM_ACCOUNT" },
  { prefix: "/admin/avatar-approvals", featureCode: "IAM_ACCOUNT" },
  { prefix: "/admin/onboarding-approvals", featureCode: "IAM_ACCOUNT" },
  { prefix: "/admin/profile", featureCode: "IAM_ACCOUNT" },
  { prefix: "/admin/customer", featureCode: "CUSTOMER" },
  { prefix: "/admin/role", featureCode: "IAM_ROLE" },
  { prefix: "/admin/support-categories", featureCode: "SUPPORT" },
  { prefix: "/admin/support-tickets", featureCode: "SUPPORT" },
  { prefix: "/admin/support-center", featureCode: "SUPPORT" },
  { prefix: "/admin/announcements", featureCode: "ANNOUNCEMENT" },
  { prefix: "/admin/ai-models", featureCode: "AI_KNOWLEDGE" },
  { prefix: "/admin/ai-knowledge", featureCode: "AI_KNOWLEDGE" },
  { prefix: "/admin/user-guides", featureCode: "USER_GUIDE" },
  { prefix: "/customer/dashboard", featureCode: "CUSTOMER_PORTAL" },
  { prefix: "/customer/profile", featureCode: "CUSTOMER_PORTAL" },
  { prefix: "/customer/vehicles", featureCode: "CUSTOMER_PORTAL" },
  { prefix: "/customer/subscriptions", featureCode: "CUSTOMER_PORTAL" },
  { prefix: "/customer/wallet", featureCode: "CUSTOMER_PORTAL" },
  { prefix: "/customer/parking-history", featureCode: "CUSTOMER_PORTAL" },
  { prefix: "/customer/support", featureCode: "CUSTOMER_PORTAL" },
  { prefix: "/customerTicket", featureCode: "CUSTOMER_PORTAL" },
  { prefix: "/parking-lots/nearby", featureCode: "CUSTOMER_PORTAL" },
  { prefix: "/pricing", featureCode: "CUSTOMER_PORTAL" },
  { prefix: "/guide", featureCode: "GUIDE_PAGE" },
  { prefix: "/contact", featureCode: "CUSTOMER_PORTAL" },
];

export function deriveGuideContext(pathname: string): { featureCode: string | null; contextKey: string } {
  const normalized = pathname.split("?")[0].split("#")[0] || "/";
  const match = routeFeatureMap.find((entry) => normalized.startsWith(entry.prefix));
  return {
    contextKey: normalized.toUpperCase(),
    featureCode: match ? match.featureCode : null,
  };
}
