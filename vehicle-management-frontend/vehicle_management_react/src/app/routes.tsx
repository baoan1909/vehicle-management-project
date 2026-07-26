import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { getRoutePermissions } from "@/app/routePermissions";
import type { AppLayout } from "@/shared/types/common";
import { LoginPage } from "@/features/auth";
import { VnpayReturnPage } from "@/features/billing/pages/VnpayReturnPage";
import { CardListPage, LostCardCreatePage, LostCardDetailPage, LostCardListPage } from "@/features/cards";
import { SubscriptionApprovalPage, TicketListPage, VehicleListPage } from "@/features/catalog";
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
import { CustomerListPage } from "@/features/customers";
import { DashboardPage } from "@/features/dashboard";
import { EmployeeListPage, ShiftSchedulePage } from "@/features/employees";
import { AccountListPage, InternalProfilePage, RoleListPage } from "@/features/iam";
import { ParkingOperationsPage, ParkingSessionPage, SwipeListPage } from "@/features/parking";
import { PricePlanListPage, PriceRuleListPage } from "@/features/pricing";
import { OperationsSupportCenterPage, SupportCategoryWorkflowPage } from "@/features/support";

export interface RouteDefinition {
  path: string;
  title: string;
  layout: AppLayout;
  element: ReactNode;
  permissions?: string[];
}

function BlankPage() {
  return <div />;
}

const blankPage = <BlankPage />;

const routeDefinitions: Omit<RouteDefinition, "permissions">[] = [
  { path: "/admin/dashboard", title: "Tong quan", layout: "admin", element: <DashboardPage /> },
  { path: "/api/dashboard/overview", title: "Tong quan", layout: "admin", element: <Navigate to="/admin/dashboard" replace /> },
  { path: "/admin/swipe", title: "Quan ly vao ra", layout: "admin", element: <SwipeListPage /> },
  { path: "/admin/swipe/swipein", title: "Xe vao", layout: "admin", element: blankPage },
  { path: "/admin/swipe/swipeout", title: "Xe ra", layout: "admin", element: blankPage },
  { path: "/admin/swipe/sessions", title: "Phien gui xe", layout: "admin", element: <ParkingSessionPage /> },
  { path: "/admin/card", title: "Quan ly the", layout: "admin", element: <CardListPage /> },
  { path: "/admin/card/form", title: "Thong tin the", layout: "admin", element: blankPage },
  { path: "/admin/lost", title: "The bi mat", layout: "admin", element: <LostCardListPage /> },
  { path: "/admin/lost/form", title: "Tao phieu bao mat the", layout: "admin", element: <LostCardCreatePage /> },
  { path: "/admin/lost/detail", title: "Chi tiet phieu bao mat the", layout: "admin", element: <LostCardDetailPage /> },
  { path: "/admin/ticket", title: "Quan ly ve", layout: "admin", element: <TicketListPage /> },
  { path: "/admin/ticket/form", title: "Thong tin ve", layout: "admin", element: blankPage },
  { path: "/admin/subscription-approvals", title: "Duyet dang ky ve thang va gan the", layout: "admin", element: <SubscriptionApprovalPage /> },
  { path: "/admin/vehicle", title: "Quan ly phuong tien", layout: "admin", element: <VehicleListPage /> },
  { path: "/admin/vehicle/form", title: "Thong tin phuong tien", layout: "admin", element: blankPage },
  { path: "/admin/parking-lots", title: "Bãi xe & Sơ đồ vận hành", layout: "admin", element: <ParkingOperationsPage /> },
  { path: "/admin/price-plans", title: "Ke hoach gia", layout: "admin", element: <PricePlanListPage /> },
  { path: "/admin/price-rules", title: "Quy tac gia", layout: "admin", element: <PriceRuleListPage /> },
  { path: "/admin/visitorParkingFee", title: "Phi vang lai", layout: "admin", element: <Navigate to="/admin/price-rules" replace /> },
  { path: "/admin/parkingFeeOfCustomer", title: "Phi dang ky", layout: "admin", element: <Navigate to="/admin/price-rules" replace /> },
  { path: "/admin/employee", title: "Nhân viên", layout: "admin", element: <EmployeeListPage /> },
  { path: "/admin/employee/form", title: "Thong tin nhan vien", layout: "admin", element: blankPage },
  { path: "/admin/shifts", title: "Ca trực & Phân công", layout: "admin", element: <ShiftSchedulePage /> },
  { path: "/admin/account", title: "Tài khoản", layout: "admin", element: <AccountListPage /> },
  { path: "/admin/account/form", title: "Thong tin tai khoan", layout: "admin", element: blankPage },
  { path: "/admin/profile", title: "Thong tin tai khoan ca nhan", layout: "admin", element: <InternalProfilePage /> },
  { path: "/admin/customer", title: "Quản lý khách hàng", layout: "admin", element: <CustomerListPage /> },
  { path: "/admin/customer/form", title: "Thong tin khach hang", layout: "admin", element: blankPage },
  { path: "/admin/role", title: "Phan quyen vai tro", layout: "admin", element: <RoleListPage /> },
  { path: "/admin/role/form", title: "Thong tin vai tro", layout: "admin", element: blankPage },
  { path: "/admin/support-categories", title: "Danh muc ho tro va quy trinh ticket", layout: "admin", element: <SupportCategoryWorkflowPage /> },
  { path: "/admin/support-center", title: "Trung tam ho tro van hanh", layout: "fullscreen", element: <OperationsSupportCenterPage /> },
  { path: "/pricing", title: "Bang gia dich vu do xe", layout: "client", element: <PricingPage /> },
  { path: "/guide", title: "Huong dan", layout: "client", element: <GuidePage /> },
  { path: "/contact", title: "Lien he", layout: "client", element: <ContactPage /> },
  { path: "/payment/vnpay-return", title: "Ket qua thanh toan", layout: "client", element: <VnpayReturnPage /> },
  { path: "/customerTicket/customer-infor", title: "Lich su gui xe", layout: "client", element: <CustomerHistoryPage /> },
  { path: "/customerTicket/customer-infor-detail", title: "Thong tin tai khoan", layout: "client", element: <ProfilePage /> },
  { path: "/customer/dashboard", title: "Tong quan khach hang", layout: "client", element: <CustomerDashboardPage /> },
  { path: "/customer/profile", title: "Ho so ca nhan", layout: "client", element: <ProfilePage /> },
  { path: "/customer/vehicles", title: "Xe cua toi", layout: "client", element: <VehiclePage /> },
  { path: "/customer/subscriptions", title: "Ve thang", layout: "client", element: <SubscriptionPage /> },
  { path: "/customer/parking-history", title: "Lich su gui xe", layout: "client", element: <CustomerHistoryPage /> },
  { path: "/customer/support", title: "Ho tro", layout: "client", element: <SupportPage /> },
  { path: "/login", title: "Đăng nhập", layout: "auth", element: <LoginPage mode="login" /> },
  { path: "/register", title: "Đăng ký", layout: "auth", element: <LoginPage mode="register" /> },
  { path: "/forgot-password", title: "Quên mật khẩu", layout: "auth", element: <LoginPage mode="forgot" /> },
  { path: "/forgot-password/otp", title: "OTP", layout: "auth", element: <Navigate to="/forgot-password" replace /> },
  { path: "/recover-password", title: "Recover", layout: "auth", element: <Navigate to="/forgot-password" replace /> },
];

export const routes: RouteDefinition[] = routeDefinitions.map((route) => ({
  ...route,
  permissions: getRoutePermissions(route.path),
}));
