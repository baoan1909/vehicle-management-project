import type { ReactNode } from "react";
import type { AppLayout } from "@/shared/types/common";
import { LoginPage } from "@/features/auth";
import { CardFormPage, CardListPage, LostCardFormPage, LostCardListPage } from "@/features/cards";
import {
  RegistrationFeeFormPage,
  RegistrationFeePage,
  TicketFormPage,
  TicketListPage,
  VehicleFormPage,
  VehicleListPage,
  VisitorFeeFormPage,
  VisitorParkingFeePage,
} from "@/features/catalog";
import {
  ContactPage,
  CustomerDashboardPage,
  CustomerHistoryPage,
  GuidePage,
  PricingPage,
  ProfilePage,
  SubscriptionPage,
  SupportPage,
  VehiclePage,
} from "@/features/customer-portal";
import { CustomerFormPage, CustomerListPage } from "@/features/customers";
import { DashboardPage } from "@/features/dashboard";
import { AccountFormPage, AccountListPage, RoleFormPage, RoleListPage } from "@/features/iam";
import { SwipeEntryPage, SwipeListPage } from "@/features/parking";

export interface RouteDefinition {
  path: string;
  title: string;
  layout: AppLayout;
  element: ReactNode;
}

export const routes: RouteDefinition[] = [
  { path: "/admin/dashboard", title: "Trang chủ", layout: "admin", element: <DashboardPage /> },
  { path: "/admin/swipe", title: "Quản lý vào ra", layout: "admin", element: <SwipeListPage /> },
  { path: "/admin/swipe/swipein", title: "Xe vào", layout: "admin", element: <SwipeEntryPage mode="in" /> },
  { path: "/admin/swipe/swipeout", title: "Xe ra", layout: "admin", element: <SwipeEntryPage mode="out" /> },
  { path: "/admin/card", title: "Quản lý thẻ", layout: "admin", element: <CardListPage /> },
  { path: "/admin/card/form", title: "Thông tin thẻ", layout: "admin", element: <CardFormPage /> },
  { path: "/admin/lost", title: "Thẻ bị mất", layout: "admin", element: <LostCardListPage /> },
  { path: "/admin/lost/form", title: "Thông tin thẻ bị mất", layout: "admin", element: <LostCardFormPage /> },
  { path: "/admin/ticket", title: "Quản lý vé", layout: "admin", element: <TicketListPage /> },
  { path: "/admin/ticket/form", title: "Thông tin vé", layout: "admin", element: <TicketFormPage /> },
  { path: "/admin/vehicle", title: "Quản lý phương tiện", layout: "admin", element: <VehicleListPage /> },
  { path: "/admin/vehicle/form", title: "Thông tin phương tiện", layout: "admin", element: <VehicleFormPage /> },
  { path: "/admin/visitorParkingFee", title: "Phí vãng lai", layout: "admin", element: <VisitorParkingFeePage /> },
  { path: "/admin/visitorParkingFee/form", title: "Thông tin phí vãng lai", layout: "admin", element: <VisitorFeeFormPage /> },
  { path: "/admin/parkingFeeOfCustomer", title: "Phí đăng ký", layout: "admin", element: <RegistrationFeePage /> },
  { path: "/admin/parkingFeeOfCustomer/form", title: "Thông tin phí đăng ký", layout: "admin", element: <RegistrationFeeFormPage /> },
  { path: "/admin/account", title: "Quản lý tài khoản", layout: "admin", element: <AccountListPage /> },
  { path: "/admin/account/form", title: "Thông tin tài khoản", layout: "admin", element: <AccountFormPage /> },
  { path: "/admin/customer", title: "Quản lý khách hàng", layout: "admin", element: <CustomerListPage /> },
  { path: "/admin/customer/form", title: "Thông tin khách hàng", layout: "admin", element: <CustomerFormPage /> },
  { path: "/admin/role", title: "Quản lý vai trò", layout: "admin", element: <RoleListPage /> },
  { path: "/admin/role/form", title: "Thông tin vai trò", layout: "admin", element: <RoleFormPage /> },
  { path: "/pricing", title: "Bảng giá gửi xe", layout: "client", element: <PricingPage /> },
  { path: "/guide", title: "Hướng dẫn", layout: "client", element: <GuidePage /> },
  { path: "/contact", title: "Liên hệ", layout: "client", element: <ContactPage /> },
  { path: "/customerTicket/customer-infor", title: "Lịch sử gửi xe", layout: "client", element: <CustomerHistoryPage /> },
  { path: "/customerTicket/customer-infor-detail", title: "Hồ sơ cá nhân", layout: "client", element: <ProfilePage /> },
  { path: "/customer/dashboard", title: "Tổng quan khách hàng", layout: "client", element: <CustomerDashboardPage /> },
  { path: "/customer/profile", title: "Hồ sơ cá nhân", layout: "client", element: <ProfilePage /> },
  { path: "/customer/vehicles", title: "Xe của tôi", layout: "client", element: <VehiclePage /> },
  { path: "/customer/subscriptions", title: "Vé tháng", layout: "client", element: <SubscriptionPage /> },
  { path: "/customer/parking-history", title: "Lịch sử gửi xe", layout: "client", element: <CustomerHistoryPage /> },
  { path: "/customer/support", title: "Hỗ trợ", layout: "client", element: <SupportPage /> },
  { path: "/login", title: "Đăng nhập", layout: "auth", element: <LoginPage mode="login" /> },
  { path: "/register", title: "Đăng ký", layout: "auth", element: <LoginPage mode="register" /> },
  { path: "/forgot-password", title: "Quên mật khẩu", layout: "auth", element: <LoginPage mode="forgot" /> },
  { path: "/forgot-password/otp", title: "Xác nhận OTP", layout: "auth", element: <LoginPage mode="otp" /> },
  { path: "/recover-password", title: "Đặt lại mật khẩu", layout: "auth", element: <LoginPage mode="recover" /> },
];
