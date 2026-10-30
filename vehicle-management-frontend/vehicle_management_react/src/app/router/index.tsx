import { Suspense, useEffect } from "react";
import type { ReactNode } from "react";
import {
  adminFallbackRoute,
  canAccessRestrictedCustomerSupportRoute,
  canAccessAdminRoute,
  canAccessCustomerRoute,
  customerFallbackRoute,
  getFirstAccessibleAdminPath,
  isCustomerProtectedRoute,
} from "@/app/routePermissions";
import { routes } from "@/app/routes";
import { AccessErrorScreen, NoAccessScreen } from "@/core/auth/AccessStatusScreens";
import { isResolvedAccessStatus, type AccessError } from "@/core/auth/accessState";
import { resolvePostLoginPath } from "@/core/auth/landing";
import { consumeLogoutRedirectGuard, getLogoutRedirectPath, isLogoutRedirectGuardActive, logoutCurrentUser } from "@/core/auth/logout";
import { useAuth } from "@/core/auth/useAuth";
import { AdminLayout } from "@/shared/components/layout/AdminLayout";
import { ClientLayout } from "@/shared/components/layout/ClientLayout";
import { FullPageCarLoader, PageTransitionLoader } from "@/shared/components/ui/PageTransitionLoader";
import type { AppLayout } from "@/shared/types/common";
import {
  createBrowserRouter,
  Navigate,
  Outlet,
  RouterProvider,
  useLocation,
  useMatches,
} from "react-router-dom";

type AppRouteHandle = {
  title: string;
};

function getBodyClassName(layout: AppLayout) {
  if (layout === "admin") {
    return "hold-transition sidebar-mini layout-fixed";
  }

  if (layout === "client") {
    return "hold-transition layout-top-nav";
  }

  if (layout === "fullscreen") {
    return "hold-transition";
  }

  return "hold-transition login-page";
}

function RouteDocument({ layout }: { layout: AppLayout }) {
  const matches = useMatches() as Array<{ handle?: AppRouteHandle }>;
  const activeHandle = [...matches].reverse().find((match) => match.handle)?.handle;

  useEffect(() => {
    document.title = activeHandle?.title ?? "Vehicle Management";
    document.body.className = getBodyClassName(layout);
  }, [activeHandle, layout]);

  return null;
}

function RouteFallback() {
  return <FullPageCarLoader label="Đang tải trang..." />;
}

const fallbackAccessError: AccessError = {
  type: "server",
  retryable: true,
  message: "Không thể tải quyền truy cập của tài khoản.",
};

function AccessErrorGate() {
  const { accessError, retryAccess, setUser } = useAuth();
  const error = accessError ?? fallbackAccessError;

  return (
    <AccessErrorScreen
      error={error}
      onLogout={() => logoutCurrentUser(setUser)}
      onRetry={error.retryable ? retryAccess : undefined}
    />
  );
}

function AuthenticationErrorGate() {
  const { authenticationError, retryAuthentication, setUser } = useAuth();
  return (
    <AccessErrorScreen
      error={{
        message: authenticationError?.message ?? "Không thể khôi phục phiên đăng nhập.",
        retryable: true,
        type: "offline",
      }}
      onLogout={() => logoutCurrentUser(setUser)}
      onRetry={() => void retryAuthentication()}
      title="Không thể khôi phục phiên đăng nhập"
    />
  );
}

function AdminNoAccessGate() {
  const { setUser } = useAuth();

  return <NoAccessScreen onLogout={() => logoutCurrentUser(setUser)} />;
}

function CustomerNoAccessGate() {
  const { setUser } = useAuth();

  return (
    <NoAccessScreen
      onLogout={() => logoutCurrentUser(setUser)}
      safeLabel="Về hồ sơ khách hàng"
      safePath={customerFallbackRoute}
    />
  );
}

function getAccessStateRedirect(user: NonNullable<ReturnType<typeof useAuth>["user"]>, currentPath: string) {
  const requiresSafeRoute = Boolean(user.requiredAction)
    || Boolean(user.accessState && user.accessState !== "ACTIVE");
  if (!requiresSafeRoute) return null;

  const target = resolvePostLoginPath(user);
  return target !== currentPath ? target : null;
}

function AdminShell() {
  const { accessStatus, authenticationStatus, user } = useAuth();
  const location = useLocation();

  if (authenticationStatus === "initializing") return <AdminPermissionLoading />;
  if (authenticationStatus === "error" && !isResolvedAccessStatus(accessStatus)) {
    return <AuthenticationErrorGate />;
  }

  if (!user) {
    if (isLogoutRedirectGuardActive()) {
      return <Navigate to={getLogoutRedirectPath()} replace />;
    }

    return <Navigate to="/login" replace />;
  }

  if (accessStatus === "error") {
    return <AccessErrorGate />;
  }

  if (!isResolvedAccessStatus(accessStatus)) {
    return <AdminPermissionLoading />;
  }

  const accessStateRedirect = getAccessStateRedirect(user, location.pathname);
  if (accessStateRedirect) return <Navigate to={accessStateRedirect} replace />;

  return (
    <>
      <RouteDocument layout="admin" />
      <PageTransitionLoader />
      <AdminLayout>
        <Suspense fallback={<RouteFallback />}>
          <Outlet />
        </Suspense>
      </AdminLayout>
    </>
  );
}

function AdminPermissionLoading() {
  return <FullPageCarLoader label="Đang tải quyền truy cập..." />;
}

function AdminIndexRedirect() {
  const { accessStatus, authenticationStatus, user } = useAuth();

  if (authenticationStatus === "initializing") return <AdminPermissionLoading />;
  if (authenticationStatus === "error" && !isResolvedAccessStatus(accessStatus)) {
    return <AuthenticationErrorGate />;
  }

  if (!user) {
    if (isLogoutRedirectGuardActive()) {
      return <Navigate to={getLogoutRedirectPath()} replace />;
    }

    return <Navigate to="/login" replace />;
  }

  if (accessStatus === "error") {
    return <AccessErrorGate />;
  }

  if (!isResolvedAccessStatus(accessStatus)) {
    return <AdminPermissionLoading />;
  }

  return <Navigate to={resolvePostLoginPath(user)} replace />;
}

function AdminRouteGate({ element, path }: { element: ReactNode; path: string }) {
  const { accessStatus, authenticationStatus, user } = useAuth();

  if (authenticationStatus === "initializing") return <AdminPermissionLoading />;
  if (authenticationStatus === "error" && !isResolvedAccessStatus(accessStatus)) {
    return <AuthenticationErrorGate />;
  }

  if (!user) {
    if (isLogoutRedirectGuardActive()) {
      return <Navigate to={getLogoutRedirectPath()} replace />;
    }

    return <Navigate to="/login" replace />;
  }

  if (accessStatus === "error") {
    return <AccessErrorGate />;
  }

  if (!isResolvedAccessStatus(accessStatus)) {
    return <AdminPermissionLoading />;
  }

  const accessStateRedirect = getAccessStateRedirect(user, path);
  if (accessStateRedirect) return <Navigate to={accessStateRedirect} replace />;

  if (!canAccessAdminRoute(user, path)) {
    const fallbackPath = getFirstAccessibleAdminPath(user);
    if (fallbackPath === adminFallbackRoute && path !== adminFallbackRoute) {
      return <AdminNoAccessGate />;
    }

    return <Navigate to={fallbackPath} replace />;
  }

  return <>{element}</>;
}

function CustomerRouteGate({ element, path }: { element: ReactNode; path: string }) {
  const { accessStatus, authenticationStatus, user } = useAuth();

  if (authenticationStatus === "initializing") return <AdminPermissionLoading />;
  if (authenticationStatus === "error" && !isResolvedAccessStatus(accessStatus)) {
    return <AuthenticationErrorGate />;
  }

  if (!user) {
    if (isLogoutRedirectGuardActive()) {
      return <Navigate to={getLogoutRedirectPath()} replace />;
    }

    return <Navigate to="/login" replace />;
  }

  if (accessStatus === "error") {
    return <AccessErrorGate />;
  }

  if (!isResolvedAccessStatus(accessStatus)) {
    return <AdminPermissionLoading />;
  }

  const accessStateRedirect = getAccessStateRedirect(user, path);
  if (accessStateRedirect && !canAccessRestrictedCustomerSupportRoute(user, path)) {
    return <Navigate to={accessStateRedirect} replace />;
  }

  if (!canAccessCustomerRoute(user, path)) {
    if (path !== customerFallbackRoute) {
      return <CustomerNoAccessGate />;
    }

    return <Navigate to="/" replace />;
  }

  return <>{element}</>;
}

function ClientShell() {
  useEffect(() => {
    consumeLogoutRedirectGuard();
  }, []);

  return (
    <>
      <RouteDocument layout="client" />
      <PageTransitionLoader />
      <ClientLayout>
        <Suspense fallback={<RouteFallback />}>
          <Outlet />
        </Suspense>
      </ClientLayout>
    </>
  );
}

function AuthShell() {
  return (
    <>
      <RouteDocument layout="auth" />
      <PageTransitionLoader />
      <Suspense fallback={<RouteFallback />}>
        <Outlet />
      </Suspense>
    </>
  );
}

function FullscreenShell() {
  return (
    <>
      <RouteDocument layout="fullscreen" />
      <PageTransitionLoader />
      <Suspense fallback={<RouteFallback />}>
        <Outlet />
      </Suspense>
    </>
  );
}

const adminRoutes = routes
  .filter((route) => route.layout === "admin" && route.path.startsWith("/admin/"))
  .map((route) => ({
    path: route.path.replace(/^\/admin\//, ""),
    element: <AdminRouteGate path={route.path} element={route.element} />,
    handle: { title: route.title },
  }));

const apiAdminRoutes = routes
  .filter((route) => route.layout === "admin" && route.path.startsWith("/api/"))
  .map((route) => ({
    path: route.path.replace(/^\/api\//, ""),
    element: <AdminRouteGate path={route.path} element={route.element} />,
    handle: { title: route.title },
  }));

const clientRoutes = routes
  .filter((route) => route.layout === "client")
  .map((route) =>
    route.path === "/"
      ? {
          index: true,
          element: route.element,
          handle: { title: route.title },
        }
      : {
          path: route.path.replace(/^\//, ""),
          element: isCustomerProtectedRoute(route.path)
            ? <CustomerRouteGate path={route.path} element={route.element} />
            : route.element,
          handle: { title: route.title },
        },
  );

const authRoutes = routes
  .filter((route) => route.layout === "auth")
  .map((route) => ({
    path: route.path.replace(/^\//, ""),
    element: route.element,
    handle: { title: route.title },
  }));

const fullscreenRoutes = routes
  .filter((route) => route.layout === "fullscreen")
  .map((route) => ({
    path: route.path.replace(/^\//, ""),
    element: route.path.startsWith("/admin/") ? <AdminRouteGate path={route.path} element={route.element} /> : route.element,
    handle: { title: route.title },
  }));

const router = createBrowserRouter([
  {
    path: "/admin",
    element: <AdminShell />,
    children: [
      { index: true, element: <AdminIndexRedirect /> },
      ...adminRoutes,
    ],
  },
  {
    path: "/api",
    element: <AdminShell />,
    children: apiAdminRoutes,
  },
  {
    path: "/",
    element: <ClientShell />,
    children: clientRoutes,
  },
  {
    path: "/",
    element: <AuthShell />,
    children: authRoutes,
  },
  {
    path: "/",
    element: <FullscreenShell />,
    children: fullscreenRoutes,
  },
  {
    path: "*",
    element: <Navigate to="/" replace />,
  },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
