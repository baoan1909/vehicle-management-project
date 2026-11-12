import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";

import { logoutCurrentUser } from "@/core/auth/logout";
import { markAccountDeletionFreshLoginRequired } from "@/core/auth/accountSessionTermination";
import { useAuth } from "@/core/auth/useAuth";
import {
  cancelCurrentAccountDeletion,
  getAccountDeletionBlockers,
  getAccountDeletionRemediationPath,
  getCurrentAccountDeletionImpact,
  getCurrentAccountDeletionRequest,
  requestCurrentAccountDeletion,
  type AccountDeletionBlocker,
  type AccountDeletionImpact,
  type AccountDeletionRequest,
} from "@/features/iam/api/accountDeletionApi";
import { Modal, useToast } from "@/components/ui";
import { formatInApplicationTime } from "@/shared/time/applicationTime";

export function AccountDeletionPanel() {
  const { setUser, user } = useAuth();
  const toast = useToast();
  const permissions = user?.permissionCodes ?? [];
  const canRequest = permissions.includes("ACCOUNT_DELETION_REQUEST_SELF");
  const canRead = permissions.includes("ACCOUNT_DELETION_READ_SELF");
  const canCancel = permissions.includes("ACCOUNT_DELETION_CANCEL_SELF");
  const [request, setRequest] = useState<AccountDeletionRequest | null>(null);
  const [impact, setImpact] = useState<AccountDeletionImpact | null>(null);
  const [blockers, setBlockers] = useState<AccountDeletionBlocker[]>([]);
  const [confirmed, setConfirmed] = useState(false);
  const [confirmedNoRefund, setConfirmedNoRefund] = useState(false);
  const [open, setOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!canRead) return;
    let ignore = false;
    void getCurrentAccountDeletionRequest()
      .then((current) => {
        if (ignore) return;
        setRequest(current);
        if (!current && sessionStorage.getItem("vm_account_deletion_requested") === "1") {
          sessionStorage.removeItem("vm_account_deletion_requested");
          toast.success("Yêu cầu xóa đã được hủy và quyền truy cập tổ chức đã được phục hồi.", "Đã khôi phục tài khoản");
        }
      })
      .catch(() => undefined);
    return () => { ignore = true; };
  }, [canRead, toast]);

  const allImpactBlockers = useMemo(
    () => [...(impact?.accountBlockers ?? []), ...(impact?.organizationBlockers ?? [])],
    [impact],
  );
  const visibleBlockers = blockers.length > 0 ? blockers : allImpactBlockers;

  const showImpact = async () => {
    setSubmitting(true);
    setBlockers([]);
    setConfirmed(false);
    setConfirmedNoRefund(false);
    try {
      const nextImpact = await getCurrentAccountDeletionImpact();
      setImpact(nextImpact);
      setOpen(true);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể phân tích tác động xóa tài khoản.", "Thao tác thất bại");
    } finally {
      setSubmitting(false);
    }
  };

  const submit = async () => {
    setSubmitting(true);
    setBlockers([]);
    try {
      const created = await requestCurrentAccountDeletion(confirmed, confirmedNoRefund);
      setRequest(created);
      sessionStorage.setItem("vm_account_deletion_requested", "1");
      markAccountDeletionFreshLoginRequired();
      toast.success("Yêu cầu đã được tiếp nhận. Đăng nhập lại trong thời gian chờ để hủy.", "Đã lên lịch xóa tài khoản");
      setOpen(false);
      logoutCurrentUser(setUser);
    } catch (error) {
      const nextBlockers = getAccountDeletionBlockers(error);
      setBlockers(nextBlockers);
      if (nextBlockers.length === 0) {
        toast.error(error instanceof Error ? error.message : "Không thể tạo yêu cầu xóa tài khoản.", "Thao tác thất bại");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const cancel = async () => {
    setSubmitting(true);
    try {
      await cancelCurrentAccountDeletion("ACCOUNT_OWNER_CANCELLED_FROM_INTERNAL_PROFILE");
      setRequest(null);
      sessionStorage.removeItem("vm_account_deletion_requested");
      toast.success("Tài khoản và tổ chức liên quan đã được phục hồi.", "Đã hủy yêu cầu xóa");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể hủy yêu cầu xóa.", "Thao tác thất bại");
    } finally {
      setSubmitting(false);
    }
  };

  if (!canRequest && !request) return null;

  return (
    <>
      <section className="tw-mt-5 tw-flex tw-items-center tw-gap-4 tw-p-4 max-[640px]:tw-flex-col max-[640px]:tw-items-start">
        <div className="tw-min-w-0">
          <h2 className="tw-m-0 tw-text-base tw-font-black tw-text-slate-950">Xóa tài khoản</h2>
          <p className="tw-mb-0 tw-mt-1 tw-text-sm tw-leading-6 tw-text-slate-900">
            {request
              ? `Dự kiến xử lý lúc ${formatInApplicationTime(request.scheduledDeletionAt, "vi-VN", { dateStyle: "short", timeStyle: "short" })}. Đăng nhập lại bằng phiên mới trước thời điểm này sẽ hủy yêu cầu.`
              : "Hệ thống sẽ phân tích trách nhiệm cần bàn giao và xác định tổ chức có phải đóng cùng tài khoản hay không."}
          </p>
        </div>
        {request?.cancellable && canCancel ? (
          <button className="tw-ml-auto tw-cursor-pointer tw-border-0 tw-bg-transparent tw-p-0 tw-font-bold tw-text-red-600 hover:tw-underline focus-visible:tw-rounded-sm focus-visible:tw-outline focus-visible:tw-outline-2 focus-visible:tw-outline-offset-4 focus-visible:tw-outline-red-600 disabled:tw-cursor-not-allowed disabled:tw-opacity-60 max-[640px]:tw-ml-0" disabled={submitting} type="button" onClick={cancel}>
            {submitting ? "Đang hủy..." : "Hủy yêu cầu xóa"}
          </button>
        ) : !request && canRequest ? (
          <button className="tw-ml-auto tw-cursor-pointer tw-border-0 tw-bg-transparent tw-p-0 tw-font-bold tw-text-red-600 hover:tw-underline focus-visible:tw-rounded-sm focus-visible:tw-outline focus-visible:tw-outline-2 focus-visible:tw-outline-offset-4 focus-visible:tw-outline-red-600 disabled:tw-cursor-not-allowed disabled:tw-opacity-60 max-[640px]:tw-ml-0" disabled={submitting} type="button" onClick={showImpact}>
            {submitting ? "Đang phân tích..." : "Yêu cầu xóa tài khoản"}
          </button>
        ) : null}
      </section>

      <Modal
        open={open}
        title="Tác động khi xóa tài khoản"
        description="Server tự xác định phạm vi theo effective permission; không sử dụng role để quyết định nghiệp vụ."
        width="lg"
        onClose={() => !submitting && setOpen(false)}
        actions={(
          <div className="tw-flex tw-justify-end tw-gap-3">
            <button className="vm-outline-btn" disabled={submitting} type="button" onClick={() => setOpen(false)}>Quay lại</button>
            <button className="tw-rounded-md tw-border-0 tw-bg-red-600 tw-px-4 tw-py-2 tw-font-bold tw-text-white disabled:tw-opacity-60" disabled={submitting || visibleBlockers.length > 0 || Boolean(impact?.confirmationRequired && !confirmed) || Boolean(impact?.automaticActions.some((action) => action.code === "TERMINATE_ACTIVE_SUBSCRIPTION") && !confirmedNoRefund)} type="button" onClick={submit}>
              {submitting ? "Đang gửi..." : "Xác nhận xóa"}
            </button>
          </div>
        )}
      >
        <div className="tw-grid tw-gap-4">
          {impact?.organizationClosureScopes.map((scope) => (
            <div className="tw-rounded-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-4" key={scope.organizationId}>
              <strong className="tw-text-red-900">{scope.organizationName} ({scope.organizationCode}) sẽ được đóng</strong>
              <p className="tw-mb-0 tw-mt-2 tw-text-sm tw-leading-6 tw-text-red-800">
                {scope.affectedParkingLotCount} bãi xe và {scope.affectedMemberCount} thành viên bị vô hiệu quyền truy cập. Account nhân sự khác không bị xóa.
              </p>
            </div>
          ))}

          {impact?.confirmationRequired ? (
            <label className="tw-flex tw-items-start tw-gap-3 tw-rounded-lg tw-border tw-border-solid tw-border-amber-300 tw-bg-amber-50 tw-p-4 tw-text-sm tw-font-bold tw-text-amber-900">
              <input checked={confirmed} className="tw-mt-1" type="checkbox" onChange={(event) => setConfirmed(event.target.checked)} />
              Tôi xác nhận đóng các tổ chức nêu trên; chat nội bộ và tài liệu sẽ được lưu trữ, còn dữ liệu tài chính được giữ lại.
            </label>
          ) : null}

          {visibleBlockers.length > 0 ? (
            <div className="tw-rounded-lg tw-border tw-border-solid tw-border-amber-300 tw-bg-amber-50 tw-p-4" role="alert">
              <strong className="tw-text-amber-900">Chưa thể xóa do còn nghĩa vụ:</strong>
              <ul className="tw-mb-0 tw-mt-2 tw-grid tw-gap-2 tw-pl-5 tw-text-sm tw-text-amber-900">
                {visibleBlockers.map((blocker) => (
                  <li className="tw-pl-0.5" key={`${blocker.code}-${blocker.resourceId ?? blocker.resourceType}`}>
                    <div className="tw-flex tw-items-start tw-justify-between tw-gap-3">
                      <span>{blocker.message}</span>
                      {getAccountDeletionRemediationPath(blocker) ? (
                        <Link
                          aria-label={`Xử lý: ${blocker.message}`}
                          className="tw-inline-flex tw-shrink-0 tw-items-center tw-gap-1.5 tw-font-bold tw-text-red-600 hover:tw-text-red-700 hover:tw-no-underline focus-visible:tw-rounded-sm focus-visible:tw-outline focus-visible:tw-outline-2 focus-visible:tw-outline-offset-2 focus-visible:tw-outline-red-600"
                          to={getAccountDeletionRemediationPath(blocker) ?? "#"}
                        >
                          Xử lý
                          <i aria-hidden="true" className="fas fa-arrow-right tw-text-xs" />
                        </Link>
                      ) : null}
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          ) : null}

          {(impact?.automaticActions.length ?? 0) > 0 ? (
            <div className="tw-rounded-lg tw-border tw-border-solid tw-border-emerald-200 tw-bg-emerald-50 tw-p-4">
              <strong className="tw-text-emerald-900">Sẽ được hệ thống tự động xử lý:</strong>
              <ul className="tw-mb-0 tw-mt-2 tw-grid tw-gap-2 tw-pl-5 tw-text-sm tw-text-emerald-900">
                {impact?.automaticActions.map((action) => (
                  <li key={`${action.code}-${action.resourceId ?? action.resourceType}`}>{action.message}</li>
                ))}
              </ul>
            </div>
          ) : null}
          {impact?.automaticActions.some((action) => action.code === "TERMINATE_ACTIVE_SUBSCRIPTION") ? (
            <label className="tw-flex tw-cursor-pointer tw-items-start tw-gap-3 tw-rounded-lg tw-border tw-border-solid tw-border-red-300 tw-bg-red-50 tw-p-4 tw-text-sm tw-font-bold tw-text-red-900">
              <input
                checked={confirmedNoRefund}
                className="tw-peer tw-sr-only"
                type="checkbox"
                onChange={(event) => setConfirmedNoRefund(event.target.checked)}
              />
              <span
                aria-hidden="true"
                className="tw-mt-1 tw-grid tw-h-5 tw-w-5 tw-shrink-0 tw-place-items-center tw-rounded-[3px] tw-border tw-border-solid tw-border-red-500 tw-bg-white tw-text-white peer-checked:tw-border-red-600 peer-checked:tw-bg-red-600 peer-focus-visible:tw-outline peer-focus-visible:tw-outline-2 peer-focus-visible:tw-outline-offset-2 peer-focus-visible:tw-outline-red-600"
              >
                {confirmedNoRefund ? (
                  <svg className="tw-h-3.5 tw-w-3.5" fill="none" viewBox="0 0 16 16" xmlns="http://www.w3.org/2000/svg">
                    <path d="M3 8.5 6.25 12 13 4.5" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.25" />
                  </svg>
                ) : null}
              </span>
              <span>Tôi hiểu vé đang hoạt động sẽ bị chấm dứt sau thời gian chờ và tôi đồng ý không nhận hoàn tiền.</span>
            </label>
          ) : null}

          {impact && impact.organizationClosureScopes.length === 0 && visibleBlockers.length === 0 ? (
            <div className="tw-rounded-lg tw-bg-slate-50 tw-p-4 tw-text-sm tw-leading-6 tw-text-slate-700">
              Chỉ account hiện tại bị xóa; tổ chức tiếp tục hoạt động. Các dữ liệu định danh unique sẽ được tombstone sau thời gian chờ.
            </div>
          ) : null}
        </div>
      </Modal>
    </>
  );
}
