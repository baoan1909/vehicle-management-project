import { useEffect } from "react";
import type { ReactNode } from "react";
import { canAccessAdminRoute, getFirstAccessibleAdminPath, getRoutePermissions } from "@/app/routePermissions";
import { routes } from "@/app/routes";
import { consumeLogoutRedirectGuard, getLogoutRedirectPath, isLogoutRedirectGuardActive } from "@/core/auth/logout";
import { useAuth } from "@/core/auth/useAuth";
import { hasResolvedPermissions } from "@/shared/auth/permissions";
import { AdminLayout } from "@/shared/components/layout/AdminLayout";
import { ClientLayout } from "@/shared/components/layout/ClientLayout";
import { FullPageCarLoader, PageTransitionLoader } from "@/shared/components/ui/PageTransitionLoader";
import type { AppLayout } from "@/shared/types/common";
import {
  createBrowserRouter,
  Navigate,
  Outlet,
  RouterProvider,
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

function AdminShell() {
  const { user } = useAuth();

  if (!user) {
    if (isLogoutRedirectGuardActive()) {
      return <Navigate to={getLogoutRedirectPath()} replace />;
    }

    return <Navigate to="/login" replace />;
  }

  if (user?.role === "CUSTOMER") {
    return <Navigate to="/customer/dashboard" replace />;
  }

  return (
    <>
      <RouteDocument layout="admin" />
      <PageTransitionLoader />
      <AdminLayout>
        <Outlet />
      </AdminLayout>
    </>
  );
}

function AdminPermissionLoading() {
  return <FullPageCarLoader label="Đang tải quyền truy cập..." />;
}

function AdminIndexRedirect() {
  const { isAccessLoading, user } = useAuth();

  if (!user) {
    if (isLogoutRedirectGuardActive()) {
      return <Navigate to={getLogoutRedirectPath()} replace />;
    }

    return <Navigate to="/login" replace />;
  }

  if (user && !hasResolvedPermissions(user) && isAccessLoading) {
    return <AdminPermissionLoading />;
  }

  return <Navigate to={getFirstAccessibleAdminPath(user)} replace />;
}

function AdminRouteGate({ element, path }: { element: ReactNode; path: string }) {
  const { isAccessLoading, user } = useAuth();
  const permissions = getRoutePermissions(path);

  if (!user) {
    if (isLogoutRedirectGuardActive()) {
      return <Navigate to={getLogoutRedirectPath()} replace />;
    }

    return <Navigate to="/login" replace />;
  }

  if (permissions.length > 0 && user && !hasResolvedPermissions(user) && isAccessLoading) {
    return <AdminPermissionLoading />;
  }

  if (!canAccessAdminRoute(user, path)) {
    return <Navigate to={getFirstAccessibleAdminPath(user)} replace />;
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
        <Outlet />
      </ClientLayout>
    </>
  );
}

function AuthShell() {
  return (
    <>
      <RouteDocument layout="auth" />
      <PageTransitionLoader />
      <Outlet />
    </>
  );
}

function FullscreenShell() {
  return (
    <>
      <RouteDocument layout="fullscreen" />
      <PageTransitionLoader />
      <Outlet />
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
          element: route.element,
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
