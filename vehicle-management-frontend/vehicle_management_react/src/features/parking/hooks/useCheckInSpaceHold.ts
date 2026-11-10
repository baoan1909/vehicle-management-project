import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import {
  cancelCheckInSpaceHold,
  createCheckInSpaceHold,
  type CheckInPreviewRequest,
  type CheckInSpaceHoldResponse,
} from "@/features/parking/api/parkingSessionApi";

type Options = {
  enabled: boolean;
  request: CheckInPreviewRequest | null;
};

export function useCheckInSpaceHold({ enabled, request }: Options) {
  const [hold, setHold] = useState<CheckInSpaceHoldResponse | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [expiredSpaceCode, setExpiredSpaceCode] = useState("");
  const [now, setNow] = useState(() => Date.now());
  const [serverClockOffsetMs, setServerClockOffsetMs] = useState(0);
  const holdRef = useRef<CheckInSpaceHoldResponse | null>(null);
  const requestKeyRef = useRef("");
  const expiredRequestKeyRef = useRef("");
  const sequenceRef = useRef(0);

  useEffect(() => {
    holdRef.current = hold;
  }, [hold]);

  const requestKey = useMemo(() => request ? JSON.stringify(request) : "", [request]);

  useEffect(() => {
    if (!enabled || !request || !requestKey) {
      expiredRequestKeyRef.current = "";
      const current = holdRef.current;
      if (current?.status === "HELD") {
        void cancelCheckInSpaceHold(current.holdId, "CHECK_IN_CONTEXT_CLEARED").catch(() => undefined);
      }
      if (current || requestKeyRef.current) {
        sequenceRef.current += 1;
        requestKeyRef.current = "";
        holdRef.current = null;
        setHold(null);
        setIsLoading(false);
      }
      return;
    }
    if (expiredRequestKeyRef.current === requestKey) return;
    if (requestKeyRef.current === requestKey) return;

    const previous = holdRef.current;
    if (previous?.status === "HELD") {
      void cancelCheckInSpaceHold(previous.holdId, "CHECK_IN_CONTEXT_CHANGED").catch(() => undefined);
    }
    requestKeyRef.current = requestKey;
    expiredRequestKeyRef.current = "";
    const sequence = ++sequenceRef.current;
    const controller = new AbortController();
    setHold(null);
    setExpiredSpaceCode("");
    setError("");
    setIsLoading(true);

    void createCheckInSpaceHold(
      { ...request, requestId: crypto.randomUUID() },
      controller.signal,
    ).then((value) => {
      if (sequence !== sequenceRef.current) {
        void cancelCheckInSpaceHold(value.holdId, "STALE_CLIENT_RESPONSE").catch(() => undefined);
        return;
      }
      setServerClockOffsetMs(new Date(value.serverTime).getTime() - Date.now());
      setHold(value);
      setNow(Date.now());
    }).catch((reason) => {
      if (controller.signal.aborted || sequence !== sequenceRef.current) return;
      setError(reason instanceof Error ? reason.message : "Không thể tạm gán ô đỗ.");
    }).finally(() => {
      if (sequence === sequenceRef.current) setIsLoading(false);
    });

    return () => controller.abort();
  }, [enabled, request, requestKey]);

  useEffect(() => {
    if (!hold) return;
    const timer = window.setInterval(() => setNow(Date.now()), 250);
    return () => window.clearInterval(timer);
  }, [hold]);

  const remainingSeconds = hold
    ? Math.max(0, Math.ceil((new Date(hold.expiresAt).getTime() - (now + serverClockOffsetMs)) / 1000))
    : 0;

  useEffect(() => {
    if (!hold || remainingSeconds > 0) return;
    setExpiredSpaceCode(hold.parkingSpaceCode);
    setHold(null);
    holdRef.current = null;
    expiredRequestKeyRef.current = requestKeyRef.current;
    requestKeyRef.current = "";
    sequenceRef.current += 1;
  }, [hold, remainingSeconds]);

  const cancel = useCallback((reason = "CHECK_IN_CANCELLED") => {
    const current = holdRef.current;
    sequenceRef.current += 1;
    requestKeyRef.current = "";
    holdRef.current = null;
    setHold(null);
    setIsLoading(false);
    if (current?.status === "HELD") {
      void cancelCheckInSpaceHold(current.holdId, reason).catch(() => undefined);
    }
  }, []);

  const markConsumed = useCallback(() => {
    sequenceRef.current += 1;
    requestKeyRef.current = "";
    expiredRequestKeyRef.current = "";
    holdRef.current = null;
    setHold(null);
    setIsLoading(false);
  }, []);

  const clearExpiry = useCallback(() => setExpiredSpaceCode(""), []);

  return { cancel, clearExpiry, error, expiredSpaceCode, hold, isLoading, markConsumed, remainingSeconds };
}
