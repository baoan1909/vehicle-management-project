import { lazy, type ComponentType, type ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { getRoutePermissions } from "@/app/routePermissions";
import type { AppLayout } from "@/shared/types/common";

function lazyPage(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  loader: () => Promise<any>,
  name: string,
) {
  return lazy(() =>
    loader().then((module: Record<string, ComponentType<any>>) => ({ default: module[name] })),
  );
}

// Route-level code splitting: each feature barrel becomes its own chunk so
// Customer sessions never download Admin-only code (and vice versa).
const LoginPage = lazyPage(() => import("@/features/auth"), "LoginPage");
const VnpayReturnPage = lazyPage(() => import("@/features/billing/pages/VnpayReturnPage"), "VnpayReturnPage");
const InvoiceManagementPage = lazyPage(() => import("@/features/billing/pages/InvoiceManagementPage"), "InvoiceManagementPage");
const PlatformFeePage = lazyPage(() => import("@/features/billing/pages/PlatformFeePage"), "PlatformFeePage");
const CustomerWalletPage = lazyPage(() => import("@/features/billing/pages/CustomerWalletPage"), "CustomerWalletPage");
const WalletTopUpPage = lazyPage(() => import("@/features/billing/pages/WalletTopUpPage"), "WalletTopUpPage");
const WalletTopupResultPage = lazyPage(() => import("@/features/billing/pages/WalletTopupResultPage"), "WalletTopupResultPage");
const PartnerWalletPage = lazyPage(() => import("@/features/billing/pages/PartnerWalletPage"), "PartnerWalletPage");
const WalletManagementPage = lazyPage(() => import("@/features/billing/pages/WalletManagementPage"), "WalletManagementPage");
const CardFormPage = lazyPage(() => import("@/features/cards"), "CardFormPage");
const CardListPage = lazyPage(() => import("@/features/cards"), "CardListPage");
const LostCardCreatePage = lazyPage(() => import("@/features/cards"), "LostCardCreatePage");
const LostCardDetailPage = lazyPage(() => import("@/features/cards"), "LostCardDetailPage");
const LostCardListPage = lazyPage(() => import("@/features/cards"), "LostCardListPage");
const SubscriptionApprovalPage = lazyPage(() => import("@/features/catalog"), "SubscriptionApprovalPage");
const TicketFormPage = lazyPage(() => import("@/features/catalog"), "TicketFormPage");
const TicketListPage = lazyPage(() => import("@/features/catalog"), "TicketListPage");
const VehicleFormPage = lazyPage(() => import("@/features/catalog"), "VehicleFormPage");
const VehicleListPage = lazyPage(() => import("@/features/catalog"), "VehicleListPage");
const AboutPage = lazyPage(() => import("@/features/customer-portal"), "AboutPage");
const ContactPage = lazyPage(() => import("@/features/customer-portal"), "ContactPage");
const CustomerDashboardPage = lazyPage(() => import("@/features/customer-portal"), "CustomerDashboardPage");
const CustomerHistoryPage = lazyPage(() => import("@/features/customer-portal"), "CustomerHistoryPage");
const GuidePage = lazyPage(() => import("@/features/customer-portal"), "GuidePage");
const PricingPage = lazyPage(() => import("@/features/customer-portal"), "PricingPage");
const ProfilePage = lazyPage(() => import("@/features/customer-portal"), "ProfilePage");
const PartnerRegistrationPage = lazyPage(() => import("@/features/customer-portal"), "PartnerRegistrationPage");
const SubscriptionPage = lazyPage(() => import("@/features/customer-portal"), "SubscriptionPage");
const SupportPage = lazyPage(() => import("@/features/customer-portal"), "SupportPage");
const VehiclePage = lazyPage(() => import("@/features/customer-portal"), "VehiclePage");
const CustomerFormPage = lazyPage(() => import("@/features/customers"), "CustomerFormPage");
const CustomerListPage = lazyPage(() => import("@/features/customers"), "CustomerListPage");
const DashboardPage = lazyPage(() => import("@/features/dashboard"), "DashboardPage");
const EmployeeListPage = lazyPage(() => import("@/features/employees"), "EmployeeListPage");
const ShiftSchedulePage = lazyPage(() => import("@/features/employees"), "ShiftSchedulePage");
const DeviceManagementPage = lazyPage(() => import("@/features/hardware"), "DeviceManagementPage");
const AccountFormPage = lazyPage(() => import("@/features/iam"), "AccountFormPage");
const AccountListPage = lazyPage(() => import("@/features/iam"), "AccountListPage");
const AvatarApprovalManagementPage = lazyPage(() => import("@/features/iam"), "AvatarApprovalManagementPage");
const InternalProfilePage = lazyPage(() => import("@/features/iam"), "InternalProfilePage");
const RoleFormPage = lazyPage(() => import("@/features/iam"), "RoleFormPage");
const RoleListPage = lazyPage(() => import("@/features/iam"), "RoleListPage");
const AiKnowledgeAdminPage = lazyPage(() => import("@/features/ai"), "AiKnowledgeAdminPage");
const AiModelManagementPage = lazyPage(() => import("@/features/ai"), "AiModelManagementPage");
const AnnouncementManagementPage = lazyPage(() => import("@/features/notifications"), "AnnouncementManagementPage");
const UserGuideManagementPage = lazyPage(() => import("@/features/user-guide"), "UserGuideManagementPage");
const UserGuidePublicPage = lazyPage(() => import("@/features/user-guide"), "UserGuidePublicPage");
const NearbyParkingLotsPage = lazyPage(() => import("@/features/parking"), "NearbyParkingLotsPage");
const ParkingLiveMapPage = lazyPage(() => import("@/features/parking"), "ParkingLiveMapPage");
const ParkingLayoutEditorPage = lazyPage(() => import("@/features/parking"), "ParkingLayoutEditorPage");
const ParkingLotOverviewPage = lazyPage(() => import("@/features/parking"), "ParkingLotOverviewPage");
const ParkingLotStructurePage = lazyPage(() => import("@/features/parking"), "ParkingLotStructurePage");
const ParkingSessionPage = lazyPage(() => import("@/features/parking"), "ParkingSessionPage");
const SwipeEntryPage = lazyPage(() => import("@/features/parking"), "SwipeEntryPage");
const SwipeListPage = lazyPage(() => import("@/features/parking"), "SwipeListPage");
const PricePlanListPage = lazyPage(() => import("@/features/pricing"), "PricePlanListPage");
const PriceRuleListPage = lazyPage(() => import("@/features/pricing"), "PriceRuleListPage");
const VoucherManagementPage = lazyPage(() => import("@/features/pricing"), "VoucherManagementPage");
const OperationsSupportCenterPage = lazyPage(() => import("@/features/support"), "OperationsSupportCenterPage");
const SupportCategoryWorkflowPage = lazyPage(() => import("@/features/support"), "SupportCategoryWorkflowPage");
const SupportTicketManagementPage = lazyPage(() => import("@/features/support"), "SupportTicketManagementPage");
const GuestSupportPage = lazyPage(() => import("@/features/support"), "GuestSupportPage");

export interface RouteDefinition {
  path: string;
  title: string;
  layout: AppLayout;
  element: ReactNode;
  permissions?: string[];
}

const routeDefinitions: Omit<RouteDefinition, "permissions">[] = [
  { path: "/admin/dashboard", title: "Tong quan", layout: "admin", element: <DashboardPage /> },
  { path: "/api/dashboard/overview", title: "Tong quan", layout: "admin", element: <Navigate to="/admin/dashboard" replace /> },
  { path: "/admin/swipe", title: "Quan ly vao ra", layout: "admin", element: <SwipeListPage /> },
  { path: "/admin/swipe/swipein", title: "Xe vao", layout: "admin", element: <SwipeEntryPage mode="in" /> },
  { path: "/admin/swipe/swipeout", title: "Xe ra", layout: "admin", element: <SwipeEntryPage mode="out" /> },
  { path: "/admin/swipe/sessions", title: "Phien gui xe", layout: "admin", element: <ParkingSessionPage /> },
  { path: "/admin/card", title: "Quan ly the", layout: "admin", element: <CardListPage /> },
  { path: "/admin/card/form", title: "Thong tin the", layout: "admin", element: <CardFormPage /> },
  { path: "/admin/lost", title: "The bi mat", layout: "admin", element: <LostCardListPage /> },
  { path: "/admin/lost/form", title: "Tao phieu bao mat the", layout: "admin", element: <LostCardCreatePage /> },
  { path: "/admin/lost/detail", title: "Chi tiet phieu bao mat the", layout: "admin", element: <LostCardDetailPage /> },
  { path: "/admin/ticket", title: "Quan ly ve", layout: "admin", element: <TicketListPage /> },
  { path: "/admin/ticket/form", title: "Thong tin ve", layout: "admin", element: <TicketFormPage /> },
  { path: "/admin/subscription-approvals", title: "Duyet dang ky ve thang va gan the", layout: "admin", element: <SubscriptionApprovalPage /> },
  { path: "/admin/vehicle", title: "Quan ly phuong tien", layout: "admin", element: <VehicleListPage /> },
  { path: "/admin/vehicle/form", title: "Thong tin phuong tien", layout: "admin", element: <VehicleFormPage /> },
  { path: "/admin/parking-lots", title: "Tổng quan bãi xe", layout: "admin", element: <ParkingLotOverviewPage /> },
  { path: "/admin/parking-structure", title: "Cấu trúc bãi xe", layout: "admin", element: <ParkingLotStructurePage /> },
  { path: "/admin/parking-layouts", title: "Thiết kế sơ đồ bãi xe", layout: "admin", element: <ParkingLayoutEditorPage /> },
  { path: "/admin/parking-live", title: "Sơ đồ vận hành trực tiếp", layout: "admin", element: <ParkingLiveMapPage /> },
  { path: "/admin/devices", title: "Quản lý thiết bị", layout: "admin", element: <DeviceManagementPage /> },
  { path: "/admin/price-plans", title: "Ke hoach gia", layout: "admin", element: <PricePlanListPage /> },
  { path: "/admin/price-rules", title: "Quy tac gia", layout: "admin", element: <PriceRuleListPage /> },
  { path: "/admin/vouchers", title: "Quan ly voucher", layout: "admin", element: <VoucherManagementPage /> },
  { path: "/admin/invoices", title: "Quản lý hóa đơn", layout: "admin", element: <InvoiceManagementPage /> },
  { path: "/admin/platform-fees", title: "Phí sàn", layout: "admin", element: <PlatformFeePage /> },
  { path: "/admin/wallets", title: "Quản trị ví", layout: "admin", element: <WalletManagementPage /> },
  { path: "/admin/partner-wallet", title: "Ví đối tác", layout: "admin", element: <PartnerWalletPage /> },
  { path: "/admin/visitorParkingFee", title: "Phi vang lai", layout: "admin", element: <Navigate to="/admin/price-rules" replace /> },
  { path: "/admin/parkingFeeOfCustomer", title: "Phi dang ky", layout: "admin", element: <Navigate to="/admin/price-rules" replace /> },
  { path: "/admin/employee", title: "Nhân viên", layout: "admin", element: <EmployeeListPage /> },
  { path: "/admin/employee/form", title: "Thong tin nhan vien", layout: "admin", element: <Navigate to="/admin/employee" replace /> },
  { path: "/admin/shifts", title: "Ca trực & Phân công", layout: "admin", element: <ShiftSchedulePage /> },
  { path: "/admin/account", title: "Tài khoản", layout: "admin", element: <AccountListPage /> },
  { path: "/admin/account/form", title: "Thong tin tai khoan", layout: "admin", element: <AccountFormPage /> },
  { path: "/admin/avatar-approvals", title: "Duyệt ảnh đại diện", layout: "admin", element: <AvatarApprovalManagementPage /> },
  { path: "/admin/onboarding-approvals", title: "Duyet onboarding", layout: "admin", element: <Navigate to="/admin/account?tab=onboarding" replace /> },
  { path: "/admin/profile", title: "Thong tin tai khoan ca nhan", layout: "admin", element: <InternalProfilePage /> },
  { path: "/admin/customer", title: "Quản lý khách hàng", layout: "admin", element: <CustomerListPage /> },
  { path: "/admin/customer/form", title: "Thong tin khach hang", layout: "admin", element: <CustomerFormPage /> },
  { path: "/admin/role", title: "Phan quyen vai tro", layout: "admin", element: <RoleListPage /> },
  { path: "/admin/role/form", title: "Thong tin vai tro", layout: "admin", element: <RoleFormPage /> },
  { path: "/admin/support-categories", title: "Danh muc ho tro va quy trinh xu ly yeu cau", layout: "admin", element: <SupportCategoryWorkflowPage /> },
  { path: "/admin/support-tickets", title: "Yeu cau ho tro", layout: "admin", element: <SupportTicketManagementPage /> },
  { path: "/admin/announcements", title: "Quản lý thông báo", layout: "admin", element: <AnnouncementManagementPage /> },
  { path: "/admin/user-guides", title: "Hướng dẫn sử dụng", layout: "admin", element: <UserGuideManagementPage /> },
  { path: "/admin/ai-models", title: "Quan tri model AI", layout: "admin", element: <AiModelManagementPage /> },
  { path: "/admin/ai-knowledge", title: "Quan ly tri thuc AI", layout: "admin", element: <AiKnowledgeAdminPage /> },
  { path: "/admin/ai-knowledge-index", title: "Quan ly tri thuc AI", layout: "admin", element: <Navigate to="/admin/ai-knowledge" replace /> },
  { path: "/admin/support-center", title: "Trung tam ho tro van hanh", layout: "fullscreen", element: <OperationsSupportCenterPage /> },
  { path: "/", title: "Gioi thieu CoParking", layout: "client", element: <AboutPage /> },
  { path: "/parking-lots/nearby", title: "Tìm bãi xe gần tôi", layout: "client", element: <NearbyParkingLotsPage /> },
  { path: "/pricing", title: "Bang gia dich vu do xe", layout: "client", element: <PricingPage /> },
  { path: "/guide", title: "Huong dan", layout: "client", element: <GuidePage /> },
  { path: "/help", title: "Trợ giúp công khai", layout: "client", element: <GuestSupportPage /> },
  { path: "/guide/:code", title: "Hướng dẫn sử dụng", layout: "client", element: <UserGuidePublicPage /> },
  { path: "/contact", title: "Lien he", layout: "client", element: <ContactPage /> },
{ path: "/become-a-partner", title: "Trở thành đối tác CoParking", layout: "auth", element: <PartnerRegistrationPage /> },

  { path: "/payment/vnpay-return", title: "Ket qua thanh toan", layout: "client", element: <VnpayReturnPage /> },
  { path: "/customer/wallet", title: "Ví của tôi", layout: "client", element: <CustomerWalletPage /> },
  { path: "/customer/wallet/topup", title: "Nạp tiền ví", layout: "client", element: <WalletTopUpPage /> },
  { path: "/customer/wallet/topup-result", title: "Kết quả nạp tiền", layout: "client", element: <WalletTopupResultPage /> },
  { path: "/customerTicket/customer-infor", title: "Lich su gui xe", layout: "client", element: <CustomerHistoryPage /> },
  { path: "/customerTicket/customer-infor-detail", title: "Thong tin tai khoan", layout: "client", element: <ProfilePage /> },
  { path: "/customer/dashboard", title: "Tong quan khach hang", layout: "client", element: <CustomerDashboardPage /> },
  { path: "/customer/profile", title: "Ho so ca nhan", layout: "client", element: <ProfilePage /> },
  { path: "/customer/vehicles", title: "Xe cua toi", layout: "client", element: <VehiclePage /> },
  { path: "/customer/subscriptions", title: "Ve thang", layout: "client", element: <SubscriptionPage /> },
  { path: "/customer/parking-history", title: "Lich su gui xe", layout: "client", element: <CustomerHistoryPage /> },
  { path: "/customer/support", title: "Ho tro", layout: "client", element: <SupportPage /> },
  { path: "/customer/support/chat", title: "Hội thoại hỗ trợ", layout: "fullscreen", element: <OperationsSupportCenterPage /> },
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
