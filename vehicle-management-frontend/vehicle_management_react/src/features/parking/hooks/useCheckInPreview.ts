import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { isAbortLikeError } from "@/core/api/apiError";
import {
  previewCheckInParkingSession,
  type CheckInPreviewRequest,
  type ParkingSessionCheckInPreviewResponse,
} from "@/features/parking/api/parkingSessionApi";

type UseCheckInPreviewOptions = {
  debounceMs?: number;
  enabled: boolean;
  request: CheckInPreviewRequest | null;
};

export function useCheckInPreview({
  debounceMs = 300,
  enabled,
  request,
}: UseCheckInPreviewOptions) {
  const [checkInPreview, setCheckInPreview] = useState<ParkingSessionCheckInPreviewResponse | null>(null);
  const [isLoadingCheckInPreview, setIsLoadingCheckInPreview] = useState(false);
  const [previewError, setPreviewError] = useState("");
  const [previewStale, setPreviewStale] = useState(false);
  const [refreshVersion, setRefreshVersion] = useState(0);
  const abortRef = useRef<AbortController | null>(null);
  const requestSequenceRef = useRef(0);
  const previousRequestKeyRef = useRef("");
  const requestKey = useMemo(() => request ? JSON.stringify(request) : "", [request]);

  const clearCheckInPreview = useCallback(() => {
    requestSequenceRef.current += 1;
    abortRef.current?.abort();
    abortRef.current = null;
    previousRequestKeyRef.current = "";
    setCheckInPreview(null);
    setIsLoadingCheckInPreview(false);
    setPreviewError("");
    setPreviewStale(false);
  }, []);

  const refreshCheckInPreview = useCallback(() => {
    if (!enabled || !request) return;
    setPreviewStale(true);
    setRefreshVersion((current) => current + 1);
  }, [enabled, request]);

  const markCheckInPreviewStale = useCallback(() => {
    if (checkInPreview) setPreviewStale(true);
  }, [checkInPreview]);

  useEffect(() => {
    if (!enabled || !request || !requestKey) {
      clearCheckInPreview();
      return undefined;
    }

    const requestChanged = previousRequestKeyRef.current !== requestKey;
    previousRequestKeyRef.current = requestKey;
    if (requestChanged) {
      setCheckInPreview(null);
      setPreviewError("");
      setPreviewStale(false);
    }

    const controller = new AbortController();
    abortRef.current?.abort();
    abortRef.current = controller;
    const sequence = requestSequenceRef.current + 1;
    requestSequenceRef.current = sequence;
    const timeoutId = window.setTimeout(async () => {
      setIsLoadingCheckInPreview(true);
      if (!requestChanged) setPreviewStale(true);
      try {
        const response = await previewCheckInParkingSession(request, controller.signal);
        if (controller.signal.aborted || requestSequenceRef.current !== sequence) return;
        setCheckInPreview(response);
        setPreviewError("");
        setPreviewStale(false);
      } catch (error) {
        if (controller.signal.aborted || requestSequenceRef.current !== sequence || isAbortLikeError(error)) return;
        setCheckInPreview(null);
        setPreviewError(error instanceof Error ? error.message : "Không kiểm tra được điều kiện check-in.");
        setPreviewStale(false);
      } finally {
        if (requestSequenceRef.current === sequence) setIsLoadingCheckInPreview(false);
      }
    }, debounceMs);

    return () => {
      window.clearTimeout(timeoutId);
      controller.abort();
    };
  }, [clearCheckInPreview, debounceMs, enabled, refreshVersion, request, requestKey]);

  return {
    checkInPreview,
    clearCheckInPreview,
    isLoadingCheckInPreview,
    markCheckInPreviewStale,
    previewError,
    previewStale,
    refreshCheckInPreview,
  };
}
