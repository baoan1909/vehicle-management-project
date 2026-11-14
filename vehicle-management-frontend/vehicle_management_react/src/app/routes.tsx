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
const AccountDeletionArchivePage = lazyPage(() => import("@/features/iam"), "AccountDeletionArchivePage");
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
const CustomerReservationPage = lazyPage(() => import("@/features/parking"), "CustomerReservationPage");
const ParkingLiveMapPage = lazyPage(() => import("@/features/parking"), "ParkingLiveMapPage");
const ParkingLayoutEditorPage = lazyPage(() => import("@/features/parking"), "ParkingLayoutEditorPage");
const ParkingLotOverviewPage = lazyPage(() => import("@/features/parking"), "ParkingLotOverviewPage");
const ParkingLotStructurePage = lazyPage(() => import("@/features/parking"), "ParkingLotStructurePage");
const ParkingSessionPage = lazyPage(() => import("@/features/parking"), "ParkingSessionPage");
const SwipeListPage = lazyPage(() => import("@/features/parking"), "SwipeListPage");
const PricePlanListPage = lazyPage(() => import("@/features/pricing"), "PricePlanListPage");
const PriceRuleListPage = lazyPage(() => import("@/features/pricing"), "PriceRuleListPage");
const VoucherManagementPage = lazyPage(() => import("@/features/pricing"), "VoucherManagementPage");
const OperationsSupportCenterPage = lazyPage(() => import("@/features/support"), "OperationsSupportCenterPage");
const SupportCategoryWorkflowPage = lazyPage(() => import("@/features/support"), "SupportCategoryWorkflowPage");
const SupportTicketManagementPage = lazyPage(() => import("@/features/support"), "SupportTicketManagementPage");
const ParkingLotReviewManagementPage = lazyPage(() => import("@/features/parking-reviews"), "ParkingLotReviewManagementPage");

export interface RouteDefinition {
  path: string;
  title: string;
  layout: AppLayout;
  element: ReactNode;
  permissions?: string[];
}

const routeDefinitions: Omit<RouteDefinition, "permissions">[] = [
  { path: "/admin/dashboard", title: "Tổng quan", layout: "admin", element: <DashboardPage /> },
  { path: "/api/dashboard/overview", title: "Tổng quan", layout: "admin", element: <Navigate to="/admin/dashboard" replace /> },
  { path: "/admin/swipe", title: "Vận hành vào / ra bãi", layout: "fullscreen", element: <SwipeListPage /> },
  { path: "/admin/swipe/swipein", title: "Xe vào", layout: "fullscreen", element: <Navigate to="/admin/swipe?mode=check-in" replace /> },
  { path: "/admin/swipe/swipeout", title: "Xe ra", layout: "fullscreen", element: <Navigate to="/admin/swipe?mode=check-out" replace /> },
  { path: "/admin/swipe/sessions", title: "Phiên gửi xe", layout: "admin", element: <ParkingSessionPage /> },
  { path: "/admin/card", title: "Quản lý thẻ", layout: "admin", element: <CardListPage /> },
  { path: "/admin/card/form", title: "Thông tin thẻ", layout: "admin", element: <CardFormPage /> },
  { path: "/admin/lost", title: "Thẻ bị mất", layout: "admin", element: <LostCardListPage /> },
  { path: "/admin/lost/form", title: "Tạo phiếu báo mất thẻ", layout: "admin", element: <LostCardCreatePage /> },
  { path: "/admin/lost/detail", title: "Chi tiết phiếu báo mất thẻ", layout: "admin", element: <LostCardDetailPage /> },
  { path: "/admin/ticket", title: "Quản lý vé", layout: "admin", element: <TicketListPage /> },
  { path: "/admin/ticket/form", title: "Thông tin vé", layout: "admin", element: <TicketFormPage /> },
  { path: "/admin/subscription-approvals", title: "Duyệt đăng ký vé tháng và gán thẻ", layout: "admin", element: <SubscriptionApprovalPage /> },
  { path: "/admin/vehicle", title: "Quản lý phương tiện", layout: "admin", element: <VehicleListPage /> },
  { path: "/admin/vehicle/form", title: "Thông tin phương tiện", layout: "admin", element: <VehicleFormPage /> },
  { path: "/admin/parking-lots", title: "Tổng quan bãi xe", layout: "admin", element: <ParkingLotOverviewPage /> },
  { path: "/admin/parking-structure", title: "Cấu trúc bãi xe", layout: "admin", element: <ParkingLotStructurePage /> },
  { path: "/admin/parking-layouts", title: "Thiết kế sơ đồ bãi xe", layout: "admin", element: <ParkingLayoutEditorPage /> },
  { path: "/admin/parking-live", title: "Sơ đồ vận hành trực tiếp", layout: "admin", element: <ParkingLiveMapPage /> },
  { path: "/admin/devices", title: "Quản lý thiết bị", layout: "admin", element: <DeviceManagementPage /> },
  { path: "/admin/price-plans", title: "Kế hoạch giá", layout: "admin", element: <PricePlanListPage /> },
  { path: "/admin/price-rules", title: "Quy tắc giá", layout: "admin", element: <PriceRuleListPage /> },
  { path: "/admin/vouchers", title: "Quản lý voucher", layout: "admin", element: <VoucherManagementPage /> },
  { path: "/admin/invoices", title: "Quản lý hóa đơn", layout: "admin", element: <InvoiceManagementPage /> },
  { path: "/admin/platform-fees", title: "Phí sàn", layout: "admin", element: <PlatformFeePage /> },
  { path: "/admin/wallets", title: "Quản trị ví", layout: "admin", element: <WalletManagementPage /> },
  { path: "/admin/partner-wallet", title: "Ví đối tác", layout: "admin", element: <PartnerWalletPage /> },
  { path: "/admin/visitorParkingFee", title: "Phí vãng lai", layout: "admin", element: <Navigate to="/admin/price-rules" replace /> },
  { path: "/admin/parkingFeeOfCustomer", title: "Phí đăng ký", layout: "admin", element: <Navigate to="/admin/price-rules" replace /> },
  { path: "/admin/employee", title: "Nhân viên", layout: "admin", element: <EmployeeListPage /> },
  { path: "/admin/employee/form", title: "Thông tin nhân viên", layout: "admin", element: <Navigate to="/admin/employee" replace /> },
  { path: "/admin/shifts", title: "Ca trực & Phân công", layout: "admin", element: <ShiftSchedulePage /> },
  { path: "/admin/account", title: "Tài khoản", layout: "admin", element: <AccountListPage /> },
  { path: "/admin/deletion-audit", title: "Lưu trữ xóa tài khoản", layout: "admin", element: <AccountDeletionArchivePage /> },
  { path: "/admin/account/form", title: "Thông tin tài khoản", layout: "admin", element: <AccountFormPage /> },
  { path: "/admin/avatar-approvals", title: "Duyệt ảnh đại diện", layout: "admin", element: <AvatarApprovalManagementPage /> },
  { path: "/admin/onboarding-approvals", title: "Duyệt đăng ký tài khoản", layout: "admin", element: <Navigate to="/admin/account?tab=onboarding" replace /> },
  { path: "/admin/profile", title: "Thông tin tài khoản cá nhân", layout: "admin", element: <InternalProfilePage /> },
  { path: "/admin/customer", title: "Quản lý khách hàng", layout: "admin", element: <CustomerListPage /> },
  { path: "/admin/customer/form", title: "Thông tin khách hàng", layout: "admin", element: <CustomerFormPage /> },
  { path: "/admin/role", title: "Phân quyền vai trò", layout: "admin", element: <RoleListPage /> },
  { path: "/admin/role/form", title: "Thông tin vai trò", layout: "admin", element: <RoleFormPage /> },
  { path: "/admin/support-categories", title: "Danh mục hỗ trợ và quy trình xử lý yêu cầu", layout: "admin", element: <SupportCategoryWorkflowPage /> },
  { path: "/admin/support-tickets", title: "Yêu cầu hỗ trợ", layout: "admin", element: <SupportTicketManagementPage /> },
  { path: "/admin/parking-reviews", title: "Đánh giá bãi xe", layout: "admin", element: <ParkingLotReviewManagementPage /> },
  { path: "/admin/announcements", title: "Quản lý thông báo", layout: "admin", element: <AnnouncementManagementPage /> },
  { path: "/admin/user-guides", title: "Hướng dẫn sử dụng", layout: "admin", element: <UserGuideManagementPage /> },
  { path: "/admin/ai-models", title: "Quản trị mô hình AI", layout: "admin", element: <AiModelManagementPage /> },
  { path: "/admin/ai-knowledge", title: "Quản lý tri thức AI", layout: "admin", element: <AiKnowledgeAdminPage /> },
  { path: "/admin/ai-knowledge-index", title: "Quản lý tri thức AI", layout: "admin", element: <Navigate to="/admin/ai-knowledge" replace /> },
  { path: "/admin/support-center", title: "Trung tâm hỗ trợ vận hành", layout: "fullscreen", element: <OperationsSupportCenterPage /> },
  { path: "/", title: "Giới thiệu CoParking", layout: "client", element: <AboutPage /> },
  { path: "/parking-lots/nearby", title: "Tìm bãi xe gần tôi", layout: "client", element: <NearbyParkingLotsPage /> },
  { path: "/parking-lots/:parkingLotId/reserve", title: "Đặt chỗ trước", layout: "client", element: <CustomerReservationPage /> },
  { path: "/reservations", title: "Đặt chỗ đã lưu", layout: "client", element: <CustomerReservationPage /> },
  { path: "/reservations/:reservationId", title: "Chi tiết đặt chỗ", layout: "client", element: <CustomerReservationPage /> },
  { path: "/pricing", title: "Bảng giá dịch vụ đỗ xe", layout: "client", element: <PricingPage /> },
  { path: "/pricing/:parkingLotId", title: "Chi tiết bảng giá bãi xe", layout: "client", element: <PricingPage /> },
  { path: "/guide", title: "Hướng dẫn", layout: "client", element: <GuidePage /> },
  { path: "/help", title: "Trợ giúp", layout: "client", element: <Navigate to="/?support=open" replace /> },
  { path: "/guide/:code", title: "Hướng dẫn sử dụng", layout: "client", element: <UserGuidePublicPage /> },
  { path: "/contact", title: "Liên hệ", layout: "client", element: <ContactPage /> },
  { path: "/become-a-partner", title: "Trở thành đối tác CoParking", layout: "auth", element: <PartnerRegistrationPage /> },

  { path: "/payment/vnpay-return", title: "Kết quả thanh toán", layout: "client", element: <VnpayReturnPage /> },
  { path: "/customer/wallet", title: "Ví của tôi", layout: "client", element: <CustomerWalletPage /> },
  { path: "/customer/wallet/topup", title: "Nạp tiền ví", layout: "client", element: <WalletTopUpPage /> },
  { path: "/customer/wallet/topup-result", title: "Kết quả nạp tiền", layout: "client", element: <WalletTopupResultPage /> },
  { path: "/customerTicket/customer-infor", title: "Lịch sử gửi xe", layout: "client", element: <CustomerHistoryPage /> },
  { path: "/customerTicket/customer-infor-detail", title: "Thông tin tài khoản", layout: "client", element: <ProfilePage /> },
  { path: "/customer/dashboard", title: "Tổng quan khách hàng", layout: "client", element: <CustomerDashboardPage /> },
  { path: "/customer/profile", title: "Hồ sơ cá nhân", layout: "client", element: <ProfilePage /> },
  { path: "/customer/vehicles", title: "Xe của tôi", layout: "client", element: <VehiclePage /> },
  { path: "/customer/subscriptions", title: "Vé tháng", layout: "client", element: <SubscriptionPage /> },
  { path: "/customer/reservations", title: "Đặt chỗ của tôi", layout: "client", element: <CustomerReservationPage /> },
  { path: "/customer/reservations/:reservationId", title: "Chi tiết đặt chỗ", layout: "client", element: <CustomerReservationPage /> },
  { path: "/customer/parking-history", title: "Lịch sử gửi xe", layout: "client", element: <CustomerHistoryPage /> },
  { path: "/customer/support", title: "Hỗ trợ", layout: "client", element: <SupportPage /> },
  { path: "/customer/support/chat", title: "Hội thoại hỗ trợ", layout: "fullscreen", element: <OperationsSupportCenterPage /> },
  { path: "/login", title: "Đăng nhập", layout: "auth", element: <LoginPage mode="login" /> },
  { path: "/register", title: "Đăng ký", layout: "auth", element: <LoginPage mode="register" /> },
  { path: "/forgot-password", title: "Quên mật khẩu", layout: "auth", element: <LoginPage mode="forgot" /> },
  { path: "/forgot-password/otp", title: "Xác thực mã OTP", layout: "auth", element: <Navigate to="/forgot-password" replace /> },
  { path: "/recover-password", title: "Khôi phục mật khẩu", layout: "auth", element: <Navigate to="/forgot-password" replace /> },
];

export const routes: RouteDefinition[] = routeDefinitions.map((route) => ({
  ...route,
  permissions: getRoutePermissions(route.path),
}));
