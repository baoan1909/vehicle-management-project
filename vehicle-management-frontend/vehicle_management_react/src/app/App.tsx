import { useEffect, useMemo, useState } from "react";
import { AppProviders } from "./providers/AppProviders";
import { fallbackRoute, routes } from "./routes";
import { AdminLayout } from "../shared/components/layout/AdminLayout";
import { ClientLayout } from "../shared/components/layout/ClientLayout";
import { normalizeHashPath } from "../shared/utils/format";

function useHashPath() {
  const [path, setPath] = useState(() => normalizeHashPath(window.location.hash));

  useEffect(() => {
    const handleHashChange = () => setPath(normalizeHashPath(window.location.hash));
    window.addEventListener("hashchange", handleHashChange);
    return () => window.removeEventListener("hashchange", handleHashChange);
  }, []);

  return path;
}

export function App() {
  const currentPath = useHashPath();
  const route = useMemo(() => routes.find((item) => item.path === currentPath) ?? fallbackRoute, [currentPath]);

  useEffect(() => {
    document.title = route.title;
    document.body.className =
      route.layout === "admin"
        ? "hold-transition sidebar-mini layout-fixed"
        : route.layout === "client"
          ? "hold-transition layout-top-nav"
          : "hold-transition login-page";
  }, [route]);

  const content =
    route.layout === "admin" ? (
      <AdminLayout currentPath={currentPath}>{route.element}</AdminLayout>
    ) : route.layout === "client" ? (
      <ClientLayout>{route.element}</ClientLayout>
    ) : (
      route.element
    );

  return <AppProviders>{content}</AppProviders>;
}
