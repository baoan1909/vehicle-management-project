export const apiEndpoints = {
  auth: {
    login: "/auth/login",
    register: "/auth/register",
    forgotPassword: "/auth/forgot-password",
    otp: "/auth/otp",
    recoverPassword: "/auth/recover-password",
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
    roles: "/roles",
  },
} as const;
