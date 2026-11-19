import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError } from "@/core/api/apiError";
import { getAvailableSupportTicketCategories, type SupportTicketAvailableCategoryResponse } from "@/features/support/api/supportApi";

export function isSupportCategoryError(error: unknown) {
  return error instanceof ApiError && [400, 403, 404, 409].includes(error.status ?? 0)
    && (error.code === "SUPPORT_CATEGORY_NOT_AVAILABLE" || /danh mục|category/i.test(error.message));
}

export function useAvailableSupportCategories(open: boolean) {
  const [categories, setCategories] = useState<SupportTicketAvailableCategoryResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const requestRef = useRef(0);

  const reload = useCallback(async () => {
    const request = ++requestRef.current;
    setLoading(true);
    setError("");
    try {
      const response = await getAvailableSupportTicketCategories();
      if (request !== requestRef.current) return;
      setError("");
      setCategories([...(response.data ?? [])].sort((left, right) =>
        left.displayOrder - right.displayOrder || left.code.localeCompare(right.code)));
    } catch (caught) {
      if (request !== requestRef.current) return;
      setCategories([]);
      setError(caught instanceof Error ? caught.message : "Không thể tải danh mục hỗ trợ.");
    } finally {
      if (request === requestRef.current) setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!open) return;
    void reload();
    const refresh = () => { void reload(); };
    window.addEventListener("focus", refresh);
    window.addEventListener("online", refresh);
    return () => {
      ++requestRef.current;
      window.removeEventListener("focus", refresh);
      window.removeEventListener("online", refresh);
    };
  }, [open, reload]);

  return { categories, error, loading, reload };
}
