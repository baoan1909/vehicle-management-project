import { createContext, useCallback, useContext, useEffect, useRef, useState, type PropsWithChildren } from "react";

import { useAuth } from "@/core/auth/useAuth";
import { askGuestAssistant, type GuestAssistantAnswer } from "@/features/support/api/guestSupportApi";
import { classifyGuestSupportError, getGuestSessionId, type GuestSupportError } from "@/features/support/guestSupportState";

type GuestChatMessage = {
  id: string;
  role: "user" | "assistant";
  content: string;
  answer?: GuestAssistantAnswer;
};

type GuestSupportConversation = {
  sessionId: string;
  messages: GuestChatMessage[];
  draft: string;
  setDraft: (value: string) => void;
  busy: boolean;
  error: GuestSupportError | null;
  send: (question: string, captchaProof?: string) => Promise<void>;
};

const GuestSupportContext = createContext<GuestSupportConversation | null>(null);

// Only React memory contains the transcript. A page reload always starts a new chat.
export function GuestSupportConversationProvider({ children }: PropsWithChildren) {
  const { authenticationStatus, user } = useAuth();
  const isGuest = authenticationStatus === "unauthenticated" && !user;
  const guestRef = useRef(isGuest);
  guestRef.current = isGuest;
  const [sessionId] = useState(getGuestSessionId);
  const [messages, setMessages] = useState<GuestChatMessage[]>([]);
  const [draft, setDraft] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<GuestSupportError | null>(null);
  const requestRef = useRef<AbortController | null>(null);

  useEffect(() => {
    if (isGuest) return;
    requestRef.current?.abort();
    requestRef.current = null;
    setMessages([]);
    setDraft("");
    setBusy(false);
    setError(null);
  }, [isGuest]);

  useEffect(() => () => requestRef.current?.abort(), []);

  const send = useCallback(async (value: string, captchaProof?: string) => {
    const question = value.trim();
    if (!question || !guestRef.current || requestRef.current) return;
    const controller = new AbortController();
    requestRef.current = controller;
    setBusy(true);
    setError(null);
    setDraft("");
    setMessages((current) => [...current, { id: crypto.randomUUID(), role: "user", content: question }]);
    try {
      const response = await askGuestAssistant(question, sessionId, captchaProof, controller.signal);
      if (requestRef.current !== controller || !guestRef.current || controller.signal.aborted) return;
      setMessages((current) => [...current, {
        id: crypto.randomUUID(), role: "assistant", content: response.data.answer, answer: response.data,
      }]);
    } catch (caught) {
      if (requestRef.current !== controller || !guestRef.current || controller.signal.aborted) return;
      setError(classifyGuestSupportError(caught));
      setDraft(question);
    } finally {
      if (requestRef.current === controller) {
        requestRef.current = null;
        setBusy(false);
      }
    }
  }, [sessionId]);

  return (
    <GuestSupportContext.Provider value={{ sessionId, messages, draft, setDraft, busy, error, send }}>
      {children}
    </GuestSupportContext.Provider>
  );
}

export function useGuestSupportConversation() {
  const context = useContext(GuestSupportContext);
  if (!context) throw new Error("Guest support conversation provider is missing");
  return context;
}
