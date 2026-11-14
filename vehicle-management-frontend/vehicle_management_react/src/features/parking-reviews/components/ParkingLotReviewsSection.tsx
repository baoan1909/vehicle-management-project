import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { useAuth } from "@/core/auth/useAuth";
import { Modal } from "@/shared/components/ui/Modal";

import {
  getMyParkingLotReview,
  getPublicParkingLotReviews,
  upsertMyParkingLotReview,
  type MyParkingLotReview,
  type ParkingLotReviewPage,
} from "../api/parkingLotReviewApi";

const emptyPage: ParkingLotReviewPage = {
  summary: {
    averageRating: null,
    reviewCount: 0,
    distribution: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 },
  },
  content: [],
  page: 0,
  size: 4,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
};

function starLabel(rating: number) {
  return "★".repeat(rating) + "☆".repeat(5 - rating);
}

const reviewHighlights = [
  "Bãi xe dễ tìm, dễ ra vào",
  "Giá vé rõ ràng",
  "An ninh, trông giữ xe tốt",
  "Nhân viên hỗ trợ nhiệt tình",
  "Sẽ tiếp tục gửi xe tại đây",
];

const eligibilityMessage: Record<string, string> = {
  CUSTOMER_NOT_ACTIVE: "Hồ sơ khách hàng của bạn chưa hoạt động.",
  CUSTOMER_NOT_APPROVED: "Hồ sơ khách hàng của bạn chưa được phê duyệt.",
  NO_ELIGIBLE_TRANSACTION: "Bạn cần hoàn tất ít nhất một giao dịch tại bãi xe này trước khi đánh giá.",
};

export function ParkingLotReviewsSection({
  parkingLotId,
  parkingLotName,
}: {
  parkingLotId: string;
  parkingLotName: string;
}) {
  const { user } = useAuth();
  const [data, setData] = useState(emptyPage);
  const [ratingFilter, setRatingFilter] = useState<number | undefined>();
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [mine, setMine] = useState<MyParkingLotReview | null>(null);
  const [eligibilityLoading, setEligibilityLoading] = useState(false);
  const [reviewModalOpen, setReviewModalOpen] = useState(false);
  const [rating, setRating] = useState(5);
  const [hoverRating, setHoverRating] = useState<number | null>(null);
  const [comment, setComment] = useState("");
  const [saving, setSaving] = useState(false);
  const [formMessage, setFormMessage] = useState("");
  const selectedHighlights = new Set(comment.split(/\r?\n/).map((line) => line.trim()));

  function commentWithHighlight(highlight: string) {
    const sentence = `${highlight}.`;
    if (selectedHighlights.has(sentence)) {
      return comment.split(/\r?\n/).filter((line) => line.trim() !== sentence).join("\n").trim();
    }
    return comment.trim() ? `${comment.trimEnd()}\n${sentence}` : sentence;
  }

  const loadReviews = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await getPublicParkingLotReviews(parkingLotId, {
        page,
        rating: ratingFilter,
        size: 4,
      });
      setData(response.data);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể tải đánh giá bãi xe.");
    } finally {
      setLoading(false);
    }
  }, [page, parkingLotId, ratingFilter]);

  useEffect(() => {
    void loadReviews();
  }, [loadReviews]);

  const loadMyReview = useCallback(async () => {
    if (user?.role !== "CUSTOMER") {
      setMine(null);
      return;
    }
    setEligibilityLoading(true);
    try {
      const response = await getMyParkingLotReview(parkingLotId);
      setMine(response.data);
      setRating(response.data.currentReview?.rating ?? 5);
      setComment(response.data.currentReview?.comment ?? "");
      if (window.location.hash === "#write-review") setReviewModalOpen(true);
    } catch (requestError) {
      setFormMessage(requestError instanceof Error ? requestError.message : "Không thể kiểm tra điều kiện đánh giá.");
    } finally {
      setEligibilityLoading(false);
    }
  }, [parkingLotId, user?.role]);

  useEffect(() => {
    void loadMyReview();
  }, [loadMyReview]);

  function openReviewModal() {
    setFormMessage("");
    setReviewModalOpen(true);
    if (!mine && !eligibilityLoading) void loadMyReview();
  }

  function closeReviewModal() {
    if (saving) return;
    setReviewModalOpen(false);
    if (window.location.hash === "#write-review") {
      window.history.replaceState(null, "", "#parking-reviews");
    }
  }

  async function submitReview() {
    setSaving(true);
    setFormMessage("");
    try {
      const response = await upsertMyParkingLotReview(parkingLotId, { rating, comment });
      if (!response.data) throw new Error("Máy chủ không trả về đánh giá vừa lưu.");
      const wasEditing = Boolean(mine?.currentReview);
      setMine((current) => current ? {
        ...current,
        eligible: true,
        reasonCode: "ELIGIBLE_EXISTING_REVIEW",
        currentReview: response.data,
      } : current);
      setFormMessage(wasEditing ? "Đã cập nhật đánh giá của bạn." : "Đã gửi đánh giá của bạn.");
      await loadReviews();
      setReviewModalOpen(false);
      if (window.location.hash === "#write-review") {
        window.history.replaceState(null, "", "#parking-reviews");
      }
    } catch (requestError) {
      setFormMessage(requestError instanceof Error ? requestError.message : "Không thể lưu đánh giá.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section
      aria-labelledby="parking-reviews-title"
      className="tw-mx-auto tw-mb-6 tw-max-w-[1320px] tw-scroll-mt-24 tw-overflow-hidden tw-rounded-[14px] tw-border tw-border-solid tw-border-brand-100 tw-bg-white tw-shadow-[0_14px_34px_rgba(31,66,116,0.09)]"
      id="parking-reviews"
      tabIndex={-1}
    >
      <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-slate-100 tw-bg-[linear-gradient(120deg,#f8fbff,#eef5ff)] tw-px-5 tw-py-5 min-[768px]:tw-px-7">
        <div>
          <p className="tw-m-0 tw-text-[0.68rem] tw-font-black tw-uppercase tw-tracking-[0.14em] tw-text-vm-primary">Trải nghiệm thực tế</p>
          <h2 className="tw-m-0 tw-mt-1 tw-font-[Cambria] tw-text-[1.55rem] tw-font-bold tw-text-vm-slate-900 min-[640px]:tw-text-[1.9rem]" id="parking-reviews-title">Đánh giá {parkingLotName}</h2>
          <p className="tw-m-0 tw-mt-1 tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500"><i className="fas fa-shield-alt tw-mr-1.5 tw-text-emerald-600" />Chỉ hiển thị đánh giá từ khách hàng đã có giao dịch.</p>
        </div>
        {user?.role === "CUSTOMER" ? (
          <button className="tw-inline-flex tw-min-h-10 tw-items-center tw-justify-center tw-gap-2 tw-rounded-[9px] tw-border-0 tw-bg-vm-primary tw-px-4 tw-text-[0.76rem] tw-font-black tw-text-white tw-shadow-[0_8px_18px_rgba(21,87,221,0.2)] hover:tw-bg-vm-primary-hover" onClick={openReviewModal} type="button">
            <i className="fas fa-star" aria-hidden="true" />{mine?.currentReview ? "Sửa đánh giá" : "Viết đánh giá"}
          </button>
        ) : (
          <Link className="tw-inline-flex tw-min-h-10 tw-items-center tw-justify-center tw-gap-2 tw-rounded-[9px] tw-bg-vm-primary tw-px-4 tw-text-[0.76rem] tw-font-black tw-text-white tw-shadow-[0_8px_18px_rgba(21,87,221,0.2)] hover:tw-bg-vm-primary-hover hover:tw-text-white hover:tw-no-underline" to="/login">
            <i className="fas fa-sign-in-alt" />Đăng nhập để đánh giá
          </Link>
        )}
      </div>

      <div className="tw-grid tw-gap-5 tw-p-5 min-[768px]:tw-p-7 min-[1080px]:tw-grid-cols-[250px_280px_minmax(0,1fr)]">
        <div className="tw-flex tw-min-h-[170px] tw-flex-col tw-justify-center tw-rounded-[12px] tw-bg-[#071b39] tw-p-5 tw-text-white">
          <div className="tw-flex tw-items-end tw-gap-3">
            <strong className="tw-text-[3.4rem] tw-font-black tw-leading-none">{data.summary.averageRating?.toFixed(1) ?? "—"}</strong>
            <span className="tw-pb-1 tw-text-sm tw-font-bold tw-text-blue-100">/ 5</span>
          </div>
          <span aria-label={`${data.summary.averageRating ?? 0} trên 5 sao`} className="tw-mt-3 tw-text-[1.15rem] tw-tracking-1 tw-text-amber-400">{starLabel(Math.round(data.summary.averageRating ?? 0))}</span>
          <span className="tw-mt-2 tw-text-[0.76rem] tw-font-semibold tw-text-blue-100">{data.summary.reviewCount} đánh giá đã xác minh</span>
        </div>

        <div className="tw-rounded-[12px] tw-border tw-border-solid tw-border-slate-100 tw-bg-slate-50/70 tw-p-4">
          <div className="tw-flex tw-items-center tw-justify-between tw-gap-2">
            <strong className="tw-text-[0.82rem] tw-font-black tw-text-vm-slate-900">Phân bố số sao</strong>
            {ratingFilter ? <button className="tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.7rem] tw-font-black tw-text-vm-primary" onClick={() => { setPage(0); setRatingFilter(undefined); }} type="button">Xem tất cả</button> : null}
          </div>
          {[5, 4, 3, 2, 1].map((rating) => {
            const count = data.summary.distribution[rating] ?? 0;
            const percentage = data.summary.reviewCount ? (count / data.summary.reviewCount) * 100 : 0;
            const selected = ratingFilter === rating;
            return (
              <button
                aria-label={`${rating} sao, ${count} đánh giá`}
                aria-pressed={selected}
                className={`tw-mt-2 tw-grid tw-w-full tw-grid-cols-[38px_1fr_28px] tw-items-center tw-gap-2 tw-rounded-md tw-border-0 tw-px-1 tw-py-0.5 tw-text-left tw-text-[0.72rem] tw-font-bold ${selected ? "tw-bg-blue-50 tw-text-vm-primary" : "tw-bg-transparent tw-text-vm-slate-600"}`}
                key={rating}
                onClick={() => { setPage(0); setRatingFilter(selected ? undefined : rating); }}
                type="button"
              >
                <span>{rating} <i className="fas fa-star tw-text-amber-400" /></span>
                <span className="tw-h-1.5 tw-overflow-hidden tw-rounded-full tw-bg-slate-200"><span className="tw-block tw-h-full tw-rounded-full tw-bg-amber-400" style={{ width: `${percentage}%` }} /></span>
                <span className="tw-text-right">{count}</span>
              </button>
            );
          })}
        </div>

        <div className="tw-min-w-0">
          <div className="tw-mb-3 tw-flex tw-items-center tw-justify-between tw-gap-3">
            <strong className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-900">{ratingFilter ? `Đánh giá ${ratingFilter} sao` : "Đánh giá mới nhất"}</strong>
            {data.totalPages > 1 ? <span className="tw-text-[0.7rem] tw-font-bold tw-text-vm-slate-500">Trang {data.page + 1}/{data.totalPages}</span> : null}
          </div>
          {loading ? <div className="tw-grid tw-min-h-[155px] tw-place-items-center tw-rounded-[10px] tw-bg-slate-50 tw-text-sm tw-font-semibold tw-text-vm-slate-500"><span><i className="fas fa-spinner tw-mr-2 tw-animate-spin" />Đang tải đánh giá...</span></div> : null}
          {!loading && error ? <div className="tw-rounded-[10px] tw-bg-red-50 tw-p-4 tw-text-sm tw-font-semibold tw-text-red-600">{error}</div> : null}
          {!loading && !error && data.content.length === 0 ? <div className="tw-grid tw-min-h-[155px] tw-place-items-center tw-rounded-[10px] tw-border tw-border-dashed tw-border-slate-200 tw-bg-slate-50 tw-p-4 tw-text-center tw-text-sm tw-font-semibold tw-text-vm-slate-500">Chưa có đánh giá phù hợp.</div> : null}
          {!loading && !error && data.content.length > 0 ? (
            <div className="tw-grid tw-gap-3 min-[640px]:tw-grid-cols-2 min-[1080px]:tw-grid-cols-1 min-[1320px]:tw-grid-cols-2">
              {data.content.map((review, index) => (
                <article className="tw-min-w-0 tw-rounded-[10px] tw-border tw-border-solid tw-border-slate-100 tw-bg-white tw-p-4" key={`${review.reviewerDisplayName}-${review.createdAt}-${index}`}>
                  <div className="tw-flex tw-items-start tw-justify-between tw-gap-2">
                    <strong className="tw-truncate tw-text-[0.78rem] tw-font-black tw-text-vm-slate-900">{review.reviewerDisplayName}</strong>
                    <span aria-label={`${review.rating} sao`} className="tw-flex-none tw-text-[0.7rem] tw-text-amber-400">{starLabel(review.rating)}</span>
                  </div>
                  <span className="tw-mt-1 tw-block tw-text-[0.66rem] tw-font-semibold tw-text-vm-slate-400">Đã xác minh · {new Date(review.createdAt).toLocaleDateString("vi-VN")}{review.edited ? " · Đã chỉnh sửa" : ""}</span>
                  <p className="tw-m-0 tw-mt-2 tw-line-clamp-3 tw-whitespace-pre-wrap tw-text-[0.74rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-600">{review.comment || "Khách hàng đã chấm điểm mà không để lại nhận xét."}</p>
                </article>
              ))}
            </div>
          ) : null}
          {data.totalPages > 1 ? (
            <div className="tw-mt-3 tw-flex tw-justify-end tw-gap-2">
              <button className="tw-rounded-md tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-px-3 tw-py-1.5 tw-text-[0.7rem] tw-font-black tw-text-vm-slate-600 disabled:tw-opacity-40" disabled={data.first || loading} onClick={() => setPage((current) => Math.max(0, current - 1))} type="button"><i className="fas fa-chevron-left" /> <span className="tw-sr-only">Trang trước</span></button>
              <button className="tw-rounded-md tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-px-3 tw-py-1.5 tw-text-[0.7rem] tw-font-black tw-text-vm-slate-600 disabled:tw-opacity-40" disabled={data.last || loading} onClick={() => setPage((current) => current + 1)} type="button"><span className="tw-sr-only">Trang sau</span> <i className="fas fa-chevron-right" /></button>
            </div>
          ) : null}
        </div>
      </div>

      <Modal
        description={`Chia sẻ trải nghiệm thực tế của bạn tại ${parkingLotName}.`}
        onClose={closeReviewModal}
        open={reviewModalOpen}
        title={mine?.currentReview ? "Sửa đánh giá bãi xe" : "Viết đánh giá bãi xe"}
        width="lg"
      >
        {eligibilityLoading ? <div className="tw-grid tw-min-h-40 tw-place-items-center tw-text-sm tw-font-semibold tw-text-vm-slate-500"><span><i className="fas fa-spinner tw-mr-2 tw-animate-spin" />Đang kiểm tra điều kiện đánh giá...</span></div> : null}
        {!eligibilityLoading && mine?.eligible ? (
          <div>
            <p className="tw-m-0 tw-text-center tw-text-base tw-font-bold tw-text-vm-slate-500">Trải nghiệm của bạn như thế nào?</p>
            <div role="group" aria-label="Chọn số sao" className="tw-mt-2 tw-flex tw-justify-center tw-gap-1" onMouseLeave={() => setHoverRating(null)}>
              {[1, 2, 3, 4, 5].map((value) => (
                <button aria-label={`${value} sao`} aria-pressed={rating === value} disabled={saving}
                  className="tw-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-lg tw-border-0 tw-bg-transparent tw-p-1 tw-transition hover:tw-scale-110 focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus disabled:tw-opacity-50"
                  key={value} onClick={() => setRating(value)} onMouseEnter={() => setHoverRating(value)} onBlur={() => setHoverRating(null)} type="button">
                  <svg viewBox="0 0 24 24" className={`tw-h-9 tw-w-9 ${value <= (hoverRating ?? rating) ? "tw-fill-amber-200 tw-stroke-amber-400" : "tw-fill-white tw-stroke-slate-300"}`} strokeWidth="1.8" strokeLinejoin="round" aria-hidden="true">
                    <path d="m12 2.5 2.9 5.88 6.49.94-4.7 4.58 1.11 6.47L12 17.32l-5.8 3.05 1.1-6.47-4.69-4.58 6.49-.94Z" />
                  </svg>
                </button>
              ))}
            </div>
            <fieldset className="tw-m-0 tw-mt-5 tw-min-w-0 tw-border-0 tw-p-0" disabled={saving}>
              <legend className="tw-mb-2 tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">Chọn các đặc điểm nổi bật:</legend>
              <div className="tw-grid tw-grid-cols-2 tw-gap-2 max-[480px]:tw-grid-cols-1">
                {reviewHighlights.map((highlight) => {
                  const selected = selectedHighlights.has(`${highlight}.`);
                  const nextComment = commentWithHighlight(highlight);
                  return <button type="button" key={highlight} aria-pressed={selected} disabled={saving || nextComment.length > 2000}
                    onClick={() => setComment(nextComment)}
                    className={`tw-flex tw-min-h-[40px] tw-items-center tw-gap-2 tw-rounded-full tw-border-0 tw-px-3 tw-py-2 tw-text-left tw-text-xs tw-font-bold tw-transition focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus disabled:tw-opacity-50 ${selected ? "tw-bg-blue-100 tw-text-blue-700" : "tw-bg-slate-100 tw-text-vm-slate-500 hover:tw-bg-slate-200"}`}>
                    <i className={`fas ${selected ? "fa-check-circle" : "fa-plus"} tw-shrink-0`} aria-hidden="true" /><span>{highlight}</span>
                  </button>;
                })}
              </div>
              <p className="tw-mb-0 tw-mt-2 tw-text-xs tw-text-vm-slate-500">Chọn một đặc điểm để thêm nhanh vào nhận xét.</p>
            </fieldset>
            <label className="tw-mt-5 tw-block tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500" htmlFor="parking-review-comment">Nhận xét chi tiết <span className="tw-font-semibold">(không bắt buộc):</span></label>
            <textarea className="tw-mt-2 tw-min-h-32 tw-w-full tw-resize-y tw-rounded-[12px] tw-border tw-border-solid tw-border-brand-100 tw-bg-slate-50/50 tw-p-3 tw-text-sm tw-shadow-inner tw-outline-none focus:tw-border-vm-primary focus:tw-shadow-vm-focus" disabled={saving} id="parking-review-comment" maxLength={2000} onChange={(event) => setComment(event.target.value)} placeholder="Chia sẻ trải nghiệm gửi xe của bạn tại bãi xe này" value={comment} />
            <div className="tw-mt-1 tw-text-right tw-text-[0.68rem] tw-font-semibold tw-text-vm-slate-400">{comment.length}/2000</div>
            {formMessage ? <div className="tw-mt-3 tw-rounded-[9px] tw-bg-blue-50 tw-p-3 tw-text-[0.76rem] tw-font-semibold tw-text-blue-700">{formMessage}</div> : null}
            <div className="tw-mt-5 tw-flex tw-justify-end tw-gap-3">
              <button className="tw-rounded-[9px] tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-px-4 tw-py-2.5 tw-text-[0.78rem] tw-font-black tw-text-vm-slate-600" disabled={saving} onClick={closeReviewModal} type="button">Hủy</button>
              <button className="tw-rounded-[9px] tw-border-0 tw-bg-vm-primary tw-px-5 tw-py-2.5 tw-text-[0.78rem] tw-font-black tw-text-white disabled:tw-opacity-50" disabled={saving} onClick={() => void submitReview()} type="button">{saving ? "Đang lưu..." : mine.currentReview ? "Cập nhật đánh giá" : "Gửi đánh giá"}</button>
            </div>
          </div>
        ) : null}
        {!eligibilityLoading && mine && !mine.eligible ? <div className="tw-rounded-[12px] tw-bg-amber-50 tw-p-5 tw-text-center"><i className="fas fa-info-circle tw-text-2xl tw-text-amber-500" /><strong className="tw-mt-3 tw-block tw-text-vm-slate-900">Chưa thể đánh giá bãi xe</strong><p className="tw-mb-0 tw-mt-2 tw-text-sm tw-font-semibold tw-leading-6 tw-text-vm-slate-600">{eligibilityMessage[mine.reasonCode] ?? "Bạn chưa đủ điều kiện đánh giá bãi xe này."}</p></div> : null}
        {!eligibilityLoading && !mine && formMessage ? <div className="tw-rounded-[12px] tw-bg-red-50 tw-p-4 tw-text-sm tw-font-semibold tw-text-red-600">{formMessage}</div> : null}
      </Modal>
    </section>
  );
}
