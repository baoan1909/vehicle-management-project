import { appConfig } from "@/config/env";
import { tokens } from "@/core/theme/tokens";

export const theme = {
  appName: appConfig.appName,
  adminAccent: tokens.colors.brand.primary,
  primary: tokens.colors.brand.primary,
  customerPrimary: tokens.colors.brand.hover,
  tokens,
};
