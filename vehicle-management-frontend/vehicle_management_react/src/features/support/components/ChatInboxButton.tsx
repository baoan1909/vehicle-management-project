import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { getChatInbox } from "@/features/support/api/chatApi";
import { subscribeChatRealtime } from "@/features/support/api/chatRealtime";
import { openSupportCenterConversation } from "@/features/support/utils";

const chatReadPermissions = ["CHAT_CONVERSATION_READ_OWN", "CHAT_CONVERSATION_READ_ALL"];

export function ChatInboxButton() {
  const { user } = useAuth();
  const [unreadCount, setUnreadCount] = useState(0);
  const canReadChat = hasAnyPermission(user, chatReadPermissions);

  const loadUnreadCount = useCallback(async () => {
    if (!canReadChat) {
      setUnreadCount(0);
      return;
    }

    try {
      const response = await getChatInbox();
      setUnreadCount(
        (response.data ?? []).reduce((total, item) => total + Math.max(0, item.unreadCount ?? 0), 0),
      );
    } catch {
      // Không làm gián đoạn header nếu người dùng vừa đổi quyền hoặc API chat tạm thời chưa sẵn sàng.
      setUnreadCount(0);
    }
  }, [canReadChat]);

  useEffect(() => {
    void loadUnreadCount();
  }, [loadUnreadCount]);

  useEffect(() => {
    if (!canReadChat) return undefined;

    const refreshInterval = window.setInterval(() => void loadUnreadCount(), 60_000);
    const unsubscribe = subscribeChatRealtime({
      onEvent: () => void loadUnreadCount(),
    });

    return () => {
      window.clearInterval(refreshInterval);
      unsubscribe();
    };
  }, [canReadChat, loadUnreadCount]);

  if (!canReadChat) return null;

  const badgeValue = unreadCount > 99 ? "99+" : String(unreadCount);
  const isApprovedCustomer = user?.accountStatus === "ACTIVE"
    && user?.customerStatus === "ACTIVE"
    && user?.customerApprovalStatus === "APPROVED";

  return (
    <button
      type="button"
      className="tw-relative tw-inline-flex tw-h-[54px] tw-w-[54px] tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-solid tw-border-transparent tw-bg-transparent tw-text-[1.18rem] tw-text-slate-900 tw-transition hover:tw-border-slate-200 hover:tw-bg-slate-100"
      aria-label={unreadCount > 0 ? `Hội thoại, ${unreadCount} tin nhắn chưa đọc` : "Hội thoại"}
      title={unreadCount > 0 ? `${unreadCount} tin nhắn chưa đọc` : "Hội thoại hỗ trợ"}
      onClick={() => openSupportCenterConversation({ target: isApprovedCustomer ? "customer" : "admin" })}
    >
      <i className="far fa-comment-dots" aria-hidden="true" />
      {unreadCount > 0 ? (
        <span className="tw-absolute tw-right-2.5 tw-top-2.5 tw-inline-flex tw-h-[17px] tw-min-w-[17px] tw-items-center tw-justify-center tw-rounded-full tw-bg-red-500 tw-px-1 tw-text-[0.58rem] tw-font-extrabold tw-leading-none tw-text-white">
          {badgeValue}
        </span>
      ) : null}
    </button>
  );
}
