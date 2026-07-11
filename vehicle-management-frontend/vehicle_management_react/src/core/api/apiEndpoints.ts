export const apiEndpoints = {
  auth: {
    register: "/public/auth/register",
    resendVerificationEmail: "/public/auth/resend-verification-email",
    forgotPassword: "/public/auth/forgot-password",
  },
  dashboard: "/dashboard",
  parking: {
    lanes: "/parking/lanes",
    ocrLicensePlate: "/parking/ocr/license-plate",
    parkingSessions: "/parking/parking-sessions",
    swipes: "/parking/swipes",
    swipeIn: "/parking/swipes/in",
    swipeOut: "/parking/swipes/out",
    zones: "/parking/zones",
  },
  cards: {
    cards: "/access-control/cards",
    lostCards: "/access-control/lost-card-reports",
  },
  catalog: {
    cardTypes: "/catalog/card-types",
    vehicleTypes: "/catalog/vehicle-types",
    tickets: "/tickets",
    vehicles: "/vehicles",
    visitorParkingFees: "/visitor-parking-fees",
    registrationFees: "/registration-fees",
  },
  customers: {
    customers: "/customers",
    histories: "/customer-histories",
  },
  iam: {
    accounts: "/accounts",
    provisionedAccounts: "/iam/accounts/provisioned",
    accountProfile: {
      onboarding: "/iam/accounts/onboarding",
      profile: "/iam/accounts/profile",
      avatar: "/iam/accounts/profile/avatar",
    },
    roles: "/roles",
  },
} as const;
