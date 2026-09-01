import { useEffect, useRef, useState, type MouseEvent, type TouchEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/core/auth/useAuth";
import { getSupportAssistantConversation } from "@/features/support/api/supportApi";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { useToast } from "@/components/ui";

type Position = { x: number; y: number };
type DragOffset = { x: number; y: number };

const WIDGET_CONFIG = { marginX: 24, marginY: 40, size: 56 };

function getDefaultPosition(): Position {
  if (typeof window === "undefined") return { x: 0, y: 0 };
  return {
    x: window.innerWidth - WIDGET_CONFIG.size - WIDGET_CONFIG.marginX,
    y: window.innerHeight - WIDGET_CONFIG.size - WIDGET_CONFIG.marginY,
  };
}

function clampPosition(position: Position): Position {
  if (typeof window === "undefined") return position;
  return {
    x: Math.min(Math.max(position.x, 0), window.innerWidth - WIDGET_CONFIG.size),
    y: Math.min(Math.max(position.y, 0), window.innerHeight - WIDGET_CONFIG.size),
  };
}

function getClientPoint(event: MouseEvent<HTMLElement> | TouchEvent<HTMLElement> | globalThis.MouseEvent | globalThis.TouchEvent) {
  if ("touches" in event && event.touches.length > 0) return event.touches[0];
  if ("changedTouches" in event && event.changedTouches.length > 0) return event.changedTouches[0];
  return event as MouseEvent<HTMLElement> | globalThis.MouseEvent;
}

function SupportSparkleIcon() {
  return (
    <svg width="100%" height="100%" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <path fillRule="evenodd" clipRule="evenodd" d="M8.9.5c.554 0 .781.963 1.061 2.149.299 1.265.657 2.784 1.535 3.662.878.874 2.392 1.23 3.654 1.527 1.187.28 2.15.507 2.15 1.062 0 .555-.961.782-2.146 1.061-1.262.299-2.779.657-3.658 1.535-.878.879-1.236 2.396-1.535 3.658C9.682 16.339 9.455 17.3 8.9 17.3c-.554 0-.78-.96-1.057-2.144-.297-1.263-.653-2.781-1.532-3.66-.878-.878-2.397-1.236-3.662-1.535C1.463 9.681.5 9.454.5 8.9c0-.555.965-.782 2.154-1.061 1.263-.298 2.78-.654 3.657-1.528C7.185 5.434 7.541 3.917 7.839 2.654 8.118 1.465 8.345.5 8.9.5Zm8.4 12.6c.277 0 .394.464.54 1.043.156.619.345 1.367.796 1.821.454.451 1.203.64 1.821.796.579.146 1.043.263 1.043.54 0 .277-.464.394-1.043.54-.618.156-1.367.345-1.821.796-.451.454-.64 1.203-.796 1.821-.146.579-.263 1.043-.54 1.043-.277 0-.394-.464-.54-1.043-.156-.618-.345-1.367-.796-1.821-.454-.451-1.203-.64-1.821-.796-.579-.146-1.043-.263-1.043-.54 0-.277.464-.394 1.043-.54.618-.156 1.367-.345 1.821-.796.451-.454.64-1.202.796-1.821.146-.579.263-1.043.54-1.043Z" fill="url(#support-widget-gradient)" />
      <defs><linearGradient id="support-widget-gradient" x1=".5" y1="7" x2="22" y2="17.5" gradientUnits="userSpaceOnUse"><stop stopColor="#3EB3F4" /><stop offset=".82" stopColor="#2BE9D0" /></linearGradient></defs>
    </svg>
  );
}

/** Permission-first entry point to the customer's long-lived assistant conversation. */
export function SupportFloatingWidget() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const canOpenWidget = hasAnyPermission(user, ["SUPPORT_WIDGET_ACCESS_OWN"]);
  const [position, setPosition] = useState<Position>(() => getDefaultPosition());
  const [isDragging, setIsDragging] = useState(false);
  const [isReturningToDock, setIsReturningToDock] = useState(false);
  const [isOpeningAssistant, setIsOpeningAssistant] = useState(false);
  const defaultPositionRef = useRef<Position>(getDefaultPosition());
  const dragOffsetRef = useRef<DragOffset>({ x: 0, y: 0 });
  const hasMovedRef = useRef(false);
  const openTimerRef = useRef<number | undefined>(undefined);

  useEffect(() => {
    const handleResize = () => {
      defaultPositionRef.current = getDefaultPosition();
      setPosition((current) => (isOpeningAssistant ? defaultPositionRef.current : clampPosition(current)));
    };

    defaultPositionRef.current = getDefaultPosition();
    setPosition(defaultPositionRef.current);
    window.addEventListener("resize", handleResize);
    return () => {
      window.removeEventListener("resize", handleResize);
      if (openTimerRef.current) window.clearTimeout(openTimerRef.current);
    };
  }, [isOpeningAssistant]);

  useEffect(() => {
    if (!isDragging) return undefined;
    const handleMove = (event: globalThis.MouseEvent | globalThis.TouchEvent) => {
      const point = getClientPoint(event);
      setPosition(clampPosition({ x: point.clientX - dragOffsetRef.current.x, y: point.clientY - dragOffsetRef.current.y }));
      hasMovedRef.current = true;
    };
    const stopDragging = () => setIsDragging(false);
    window.addEventListener("mousemove", handleMove);
    window.addEventListener("touchmove", handleMove, { passive: false });
    window.addEventListener("mouseup", stopDragging);
    window.addEventListener("touchend", stopDragging);
    return () => {
      window.removeEventListener("mousemove", handleMove);
      window.removeEventListener("touchmove", handleMove);
      window.removeEventListener("mouseup", stopDragging);
      window.removeEventListener("touchend", stopDragging);
    };
  }, [isDragging]);

  if (!canOpenWidget) return null;

  const openAssistant = async () => {
    setIsOpeningAssistant(true);
    try {
      const response = await getSupportAssistantConversation();
      navigate(`/customer/support/chat?conversationId=${response.data.conversationId}`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể mở Trợ lý hỗ trợ.");
    } finally {
      setIsOpeningAssistant(false);
    }
  };

  const startDrag = (event: MouseEvent<HTMLButtonElement> | TouchEvent<HTMLButtonElement>) => {
    if (isOpeningAssistant || isReturningToDock) return;
    const point = getClientPoint(event);
    hasMovedRef.current = false;
    dragOffsetRef.current = { x: point.clientX - position.x, y: point.clientY - position.y };
    setIsDragging(true);
  };

  const handleWidgetClick = () => {
    if (hasMovedRef.current) {
      hasMovedRef.current = false;
      return;
    }
    if (isReturningToDock || isOpeningAssistant) return;
    const defaultPosition = defaultPositionRef.current;
    const isAtDock = Math.abs(position.x - defaultPosition.x) < 2 && Math.abs(position.y - defaultPosition.y) < 2;
    if (!isAtDock) {
      setIsReturningToDock(true);
      setPosition(defaultPosition);
      openTimerRef.current = window.setTimeout(() => {
        setIsReturningToDock(false);
        void openAssistant();
      }, 300);
      return;
    }
    void openAssistant();
  };

  return (
    <div className={`tw-group tw-fixed tw-z-[1080] ${isReturningToDock ? "tw-transition-all tw-duration-300 tw-ease-in-out" : ""}`} style={{ left: position.x, top: position.y, touchAction: "none" }}>
      <div className="tw-pointer-events-none tw-absolute tw-right-full tw-top-1/2 tw-mr-3 tw-w-max tw--translate-y-1/2 tw-rounded tw-bg-slate-800 tw-px-3 tw-py-1.5 tw-text-xs tw-font-semibold tw-text-white tw-opacity-0 tw-shadow-sm tw-transition-opacity tw-duration-300 group-hover:tw-opacity-100">
        Trợ lý hỗ trợ
        <span className="tw-absolute tw--right-1 tw-top-1/2 tw-h-2 tw-w-2 tw--translate-y-1/2 tw-rotate-45 tw-bg-slate-800" />
      </div>
      <button type="button" aria-label="Mở Trợ lý hỗ trợ" title="Trợ lý hỗ trợ" onClick={handleWidgetClick} onMouseDown={startDrag} onTouchStart={startDrag} className={`tw-relative tw-inline-flex tw-h-14 tw-w-14 tw-items-center tw-justify-center tw-rounded-full tw-border tw-border-solid tw-border-slate-100 tw-bg-white tw-text-slate-700 tw-shadow-[0_4px_12px_rgba(15,23,42,0.15)] tw-transition-transform tw-duration-300 active:tw-scale-95 focus:tw-outline-none focus:tw-ring-4 focus:tw-ring-cyan-100 ${isDragging ? "tw-cursor-grabbing tw-scale-105" : "tw-cursor-grab hover:tw-scale-110"}`}>
        {isOpeningAssistant ? <i className="fas fa-spinner fa-spin tw-text-lg tw-text-slate-500" aria-hidden="true" /> : <span className="tw-h-7 tw-w-7"><SupportSparkleIcon /></span>}
      </button>
    </div>
  );
}
