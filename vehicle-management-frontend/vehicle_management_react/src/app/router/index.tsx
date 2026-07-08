import { useEffect } from "react";
import { routes } from "@/app/routes";
import { AdminLayout } from "@/shared/components/layout/AdminLayout";
import { ClientLayout } from "@/shared/components/layout/ClientLayout";
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
  return (
    <>
      <RouteDocument layout="admin" />
      <AdminLayout>
        <Outlet />
      </AdminLayout>
    </>
  );
}

function ClientShell() {
  return (
    <>
      <RouteDocument layout="client" />
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
      <Outlet />
    </>
  );
}

function FullscreenShell() {
  return (
    <>
      <RouteDocument layout="fullscreen" />
      <Outlet />
    </>
  );
}

const adminRoutes = routes
  .filter((route) => route.layout === "admin")
  .map((route) => ({
    path: route.path.replace(/^\/admin\//, ""),
    element: route.element,
    handle: { title: route.title },
  }));

const clientRoutes = routes
  .filter((route) => route.layout === "client")
  .map((route) => ({
    path: route.path.replace(/^\//, ""),
    element: route.element,
    handle: { title: route.title },
  }));

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
    element: route.element,
    handle: { title: route.title },
  }));

const router = createBrowserRouter([
  {
    path: "/",
    element: <Navigate to="/pricing" replace />,
  },
  {
    path: "/admin",
    element: <AdminShell />,
    children: [
      { index: true, element: <Navigate to="/admin/dashboard" replace /> },
      ...adminRoutes,
    ],
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
    element: <Navigate to="/pricing" replace />,
  },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
