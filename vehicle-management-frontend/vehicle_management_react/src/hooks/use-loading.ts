import { useCallback } from "react";

import LoaderService from "@/services/loader-service";

const useLoading = () => {
  const withLoading = useCallback(async <T,>(operation: () => Promise<T>, label?: string): Promise<T> => {
    LoaderService.show(label);

    try {
      return await operation();
    } finally {
      LoaderService.hide();
    }
  }, []);

  return withLoading;
};

export default useLoading;
