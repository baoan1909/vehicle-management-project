export const apiEndpoints = {
  auth: {
    register: "/public/auth/register",
    forgotPassword: "/public/auth/forgot-password",
  },
  dashboard: "/dashboard",
  parking: {
    swipes: "/parking/swipes",
    swipeIn: "/parking/swipes/in",
    swipeOut: "/parking/swipes/out",
  },
  cards: {
    cards: "/cards",
    lostCards: "/lost-cards",
  },
  catalog: {
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
    accountProfile: {
      onboarding: "/iam/accounts/onboarding",
      profile: "/iam/accounts/profile",
      avatar: "/iam/accounts/profile/avatar",
    },
    roles: "/roles",
  },
} as const;
