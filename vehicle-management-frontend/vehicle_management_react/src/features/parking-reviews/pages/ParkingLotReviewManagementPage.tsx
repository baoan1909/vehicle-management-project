import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { DateRangeInput, PaginationFooter, SelectMenu, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { getParkingLots, type ParkingLotApiResponse } from "@/features/parking/api/parkingLotsApi";
import { createAndOpenCustomerSupportConversation } from "@/features/support";
import { hasAnyPermission } from "@/shared/auth/permissions";

import { getManagedParkingLotReviews, type ParkingLotReview, type ParkingLotReviewPage } from "../api/parkingLotReviewApi";

const emptyPage: ParkingLotReviewPage = {
  summary: { averageRating: null, reviewCount: 0, distribution: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 } },
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
};

function toStartInstant(date: string) {
  return date ? new Date(`${date}T00:00:00`).toISOString() : undefined;
}

function toExclusiveInstant(date: string) {
  if (!date) return undefined;
  const value = new Date(`${date}T00:00:00`);
  value.setDate(value.getDate() + 1);
  return value.toISOString();
}

function splitDateRange(value: string) {
  const [from = "", to = ""] = value.split("|", 2);
  return { from, to };
}

function formatCount(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value);
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(value));
}

function stars(rating: number) {
  return "★".repeat(rating) + "☆".repeat(5 - rating);
}

function LoadingRows() {
  return <div aria-label="Đang tải đánh giá" className="tw-grid tw-gap-3 tw-p-4">
    {[0, 1, 2].map((item) => <div className="tw-grid tw-min-h-[118px] tw-animate-pulse tw-grid-cols-[minmax(240px,1.25fr)_minmax(170px,.8fr)_minmax(150px,.65fr)] tw-gap-5 tw-rounded-xl tw-bg-slate-50 tw-p-5 max-[900px]:tw-grid-cols-1" key={item}>
      <div><span className="tw-block tw-h-4 tw-w-32 tw-rounded tw-bg-slate-200" /><span className="tw-mt-3 tw-block tw-h-3 tw-w-full tw-rounded tw-bg-slate-200" /><span className="tw-mt-2 tw-block tw-h-3 tw-w-2/3 tw-rounded tw-bg-slate-200" /></div>
      <span className="tw-h-4 tw-w-36 tw-rounded tw-bg-slate-200" />
      <span className="tw-h-4 tw-w-28 tw-rounded tw-bg-slate-200" />
    </div>)}
  </div>;
}

export function ParkingLotReviewManagementPage() {
  const { user } = useAuth();
  const toast = useToast();
  const requestIdRef = useRef(0);
  const [lots, setLots] = useState<ParkingLotApiResponse[]>([]);
  const [data, setData] = useState<ParkingLotReviewPage>(emptyPage);
  const [parkingLotId, setParkingLotId] = useState("");
  const [rating, setRating] = useState("");
  const [dateRange, setDateRange] = useState("");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [loadedAt, setLoadedAt] = useState<Date | null>(null);
  const [openingReviewerId, setOpeningReviewerId] = useState<string | null>(null);
  const canContactReviewer = hasAnyPermission(user, ["CHAT_CONVERSATION_CREATE_CUSTOMER_DIRECT"])
    && hasAnyPermission(user, ["CHAT_CONVERSATION_READ_OWN", "CHAT_CONVERSATION_READ_ALL"]);

  useEffect(() => {
    let active = true;
    void getParkingLots()
      .then((response) => { if (active) setLots(response.data ?? []); })
      .catch(() => { if (active) setLots([]); });
    return () => { active = false; };
  }, []);

  const loadReviews = useCallback(async () => {
    const requestId = ++requestIdRef.current;
    const { from, to } = splitDateRange(dateRange);
    setLoading(true);
    setError("");
    try {
      const response = await getManagedParkingLotReviews({
        parkingLotId: parkingLotId || undefined,
        rating: rating ? Number(rating) : undefined,
        from: toStartInstant(from),
        to: toExclusiveInstant(to),
        page,
        size: pageSize,
      });
      if (requestId !== requestIdRef.current) return;
      setData(response.data);
      setLoadedAt(new Date());
    } catch (reason) {
      if (requestId !== requestIdRef.current) return;
      setError(reason instanceof Error ? reason.message : "Không thể tải đánh giá.");
    } finally {
      if (requestId === requestIdRef.current) setLoading(false);
    }
  }, [dateRange, page, pageSize, parkingLotId, rating]);

  useEffect(() => { void loadReviews(); }, [loadReviews]);

  const lotOptions = useMemo(() => [
    { label: "Tất cả bãi được phép xem", value: "" },
    ...lots.map((lot) => ({ label: `${lot.name} · ${lot.code}`, value: lot.parkingLotId })),
  ], [lots]);
  const ratingOptions = [
    { label: "Tất cả số sao", value: "" },
    ...[5, 4, 3, 2, 1].map((value) => ({ label: `${value} sao`, value: String(value) })),
  ];
  const selectedLot = lots.find((lot) => lot.parkingLotId === parkingLotId);
  const distribution = data.summary.distribution;
  const positiveCount = (distribution[4] ?? 0) + (distribution[5] ?? 0);
  const neutralCount = distribution[3] ?? 0;
  const negativeCount = (distribution[1] ?? 0) + (distribution[2] ?? 0);
  const summaryCount = data.summary.reviewCount;
  const positiveRate = summaryCount ? Math.round((positiveCount / summaryCount) * 100) : 0;
  const negativeRate = summaryCount ? Math.round((negativeCount / summaryCount) * 100) : 0;
  const hasSummary = Boolean(parkingLotId && summaryCount);
  const startIndex = data.totalElements ? data.page * data.size + 1 : 0;
  const endIndex = Math.min((data.page + 1) * data.size, data.totalElements);

  const actionMessage = !parkingLotId
    ? "Chọn một bãi xe để xem điểm trung bình và cơ cấu phản hồi chính xác của bãi."
    : negativeRate >= 20
      ? "Tỷ lệ đánh giá tiêu cực đang ở mức cần chú ý. Hãy rà soát các nhận xét 1–2 sao và quy trình vận hành liên quan."
      : negativeCount > 0
        ? "Có phản hồi chưa hài lòng cần được theo dõi. Ưu tiên xem các đánh giá 1–2 sao mới nhất."
        : "Chưa ghi nhận đánh giá tiêu cực trong dữ liệu hiện tại. Tiếp tục duy trì chất lượng phục vụ ổn định.";

  function resetFilters() {
    setParkingLotId("");
    setRating("");
    setDateRange("");
    setPage(0);
  }

  async function openReviewerConversation(review: ParkingLotReview) {
    if (!canContactReviewer) {
      toast.warning(
        "Tài khoản cần quyền mở và xem hội thoại khách hàng.",
        "Chưa có quyền chat",
      );
      return;
    }
    if (!review.reviewerCustomerId) {
      toast.warning(
        "API chưa cung cấp định danh người đánh giá. Vui lòng khởi động lại backend để nạp phiên bản mới.",
        "Chưa thể mở hội thoại",
      );
      return;
    }
    if (openingReviewerId) return;
    setOpeningReviewerId(review.reviewerCustomerId);
    try {
      await createAndOpenCustomerSupportConversation({
        customerId: review.reviewerCustomerId,
        customerName: review.reviewerDisplayName,
      });
    } catch (reason) {
      toast.error(
        reason instanceof Error ? reason.message : "Không thể mở hội thoại với người đánh giá.",
        "Liên hệ thất bại",
      );
    } finally {
      setOpeningReviewerId(null);
    }
  }

  return <div className="tw-min-h-full tw-bg-[#f6f7f9] tw-p-4 md:tw-p-6">
    <div className="tw-mx-auto tw-max-w-[1480px]">
      <header className="tw-flex tw-flex-wrap tw-items-end tw-justify-between tw-gap-4">
        <div>
          <p className="tw-m-0 tw-text-[0.7rem] tw-font-black tw-uppercase tw-tracking-[0.14em] tw-text-vm-primary">Chất lượng dịch vụ</p>
          <h1 className="tw-m-0 tw-mt-1 tw-font-[Cambria,Georgia,serif] tw-text-[clamp(2rem,3vw,2.8rem)] tw-font-bold tw-leading-tight tw-text-vm-slate-900">Đánh giá bãi xe</h1>
          <p className="tw-m-0 tw-mt-2 tw-text-sm tw-font-semibold tw-text-vm-slate-500">Theo dõi trải nghiệm đã xác minh trong phạm vi bãi xe bạn quản lý hoặc được phân công.</p>
        </div>
        <button className="tw-inline-flex tw-h-10 tw-items-center tw-gap-2 tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-px-4 tw-text-xs tw-font-black tw-text-vm-slate-700 tw-shadow-sm hover:tw-border-blue-200 hover:tw-text-vm-primary disabled:tw-opacity-50" disabled={loading} onClick={() => void loadReviews()} type="button"><i className={`fas fa-sync-alt ${loading ? "tw-animate-spin" : ""}`} />Làm mới dữ liệu</button>
      </header>

      <section aria-label="Tổng quan đánh giá" className="tw-mt-6 tw-grid tw-grid-cols-[minmax(260px,.42fr)_minmax(0,1.58fr)] tw-gap-4 max-[900px]:tw-grid-cols-1">
        <article className="tw-overflow-hidden tw-rounded-2xl tw-bg-[#071b39] tw-p-6 tw-text-white tw-shadow-[0_14px_34px_rgba(7,27,57,.16)]">
          <span className="tw-text-xs tw-font-bold tw-text-blue-100">Điểm trung bình {selectedLot ? `· ${selectedLot.name}` : ""}</span>
          <div className="tw-mt-5 tw-flex tw-items-end tw-gap-3"><strong className="tw-text-6xl tw-font-black tw-leading-none">{hasSummary ? data.summary.averageRating?.toFixed(1) : "—"}</strong><span className="tw-pb-1 tw-text-sm tw-font-bold tw-text-blue-100">/ 5</span></div>
          <div aria-label={`${data.summary.averageRating ?? 0} trên 5 sao`} className="tw-mt-4 tw-text-xl tw-tracking-[0.12em] tw-text-amber-400">{stars(Math.round(data.summary.averageRating ?? 0))}</div>
          <p className="tw-mb-0 tw-mt-3 tw-text-xs tw-font-semibold tw-leading-5 tw-text-blue-100">{parkingLotId ? `${formatCount(summaryCount)} đánh giá đã xác minh` : "Chọn bãi xe để xem điểm tổng hợp"}</p>
        </article>

        <article className="tw-rounded-2xl tw-border tw-border-solid tw-border-slate-100 tw-bg-white tw-p-6 tw-shadow-sm">
          <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-3"><div><h2 className="tw-m-0 tw-text-lg tw-font-black tw-text-vm-slate-900">Cơ cấu phản hồi</h2><p className="tw-m-0 tw-mt-1 tw-text-xs tw-font-semibold tw-text-vm-slate-500">Tích cực 4–5 sao · Trung lập 3 sao · Tiêu cực 1–2 sao</p></div><strong className="tw-text-2xl tw-font-black tw-text-vm-primary">{hasSummary ? `${positiveRate}%` : "—"}</strong></div>
          <div className="tw-mt-6 tw-grid tw-gap-4 sm:tw-grid-cols-3">
            {[
              { count: positiveCount, icon: "fa-smile", label: "Tích cực", rate: positiveRate, tone: "tw-bg-emerald-500", text: "tw-text-emerald-600" },
              { count: neutralCount, icon: "fa-meh", label: "Trung lập", rate: summaryCount ? Math.round((neutralCount / summaryCount) * 100) : 0, tone: "tw-bg-amber-400", text: "tw-text-amber-600" },
              { count: negativeCount, icon: "fa-frown", label: "Tiêu cực", rate: negativeRate, tone: "tw-bg-rose-500", text: "tw-text-rose-600" },
            ].map((item) => <div className="tw-rounded-xl tw-bg-slate-50 tw-p-4" key={item.label}><div className="tw-flex tw-items-center tw-justify-between"><span className={`tw-text-xs tw-font-black ${item.text}`}><i className={`far ${item.icon} tw-mr-1.5`} />{item.label}</span><strong className="tw-text-sm tw-text-vm-slate-900">{hasSummary ? item.count : "—"}</strong></div><div className="tw-mt-3 tw-h-1.5 tw-overflow-hidden tw-rounded-full tw-bg-slate-200"><span className={`tw-block tw-h-full tw-rounded-full ${item.tone}`} style={{ width: `${hasSummary ? item.rate : 0}%` }} /></div><span className="tw-mt-2 tw-block tw-text-[0.68rem] tw-font-bold tw-text-vm-slate-500">{hasSummary ? `${item.rate}% tổng đánh giá` : "Chưa có dữ liệu"}</span></div>)}
          </div>
        </article>

        <article className="tw-col-span-full tw-rounded-2xl tw-border tw-border-solid tw-border-blue-100 tw-bg-[linear-gradient(135deg,#f8fbff,#edf5ff)] tw-p-6 tw-shadow-sm max-[900px]:tw-col-span-1">
          <div className="tw-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-full tw-bg-white tw-text-xl tw-text-vm-primary tw-shadow-sm"><i className="far fa-lightbulb" /></div>
          <h2 className="tw-m-0 tw-mt-4 tw-text-lg tw-font-black tw-text-vm-slate-900">Hành động đề xuất</h2>
          <p className="tw-mb-0 tw-mt-2 tw-text-sm tw-font-semibold tw-leading-6 tw-text-vm-slate-600">{actionMessage}</p>
          {parkingLotId && negativeCount > 0 ? <button className="tw-mt-4 tw-border-0 tw-bg-transparent tw-p-0 tw-text-xs tw-font-black tw-text-vm-primary" onClick={() => { setRating("2"); setPage(0); }} type="button">Xem phản hồi cần chú ý <i className="fas fa-arrow-right tw-ml-1" /></button> : null}
        </article>
      </section>

      <section aria-label="Bộ lọc đánh giá" className="tw-mt-5 tw-rounded-2xl tw-border tw-border-solid tw-border-slate-100 tw-bg-white tw-p-5 tw-shadow-sm">
        <div className="tw-grid tw-grid-cols-[minmax(240px,.8fr)_minmax(220px,.8fr)_minmax(300px,1.2fr)_auto] tw-items-end tw-gap-3 max-[1100px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
          <label className="tw-mb-0 tw-grid tw-min-w-0 tw-gap-1.5 tw-text-xs tw-font-black tw-text-vm-slate-600">Bãi xe<SelectMenu ariaLabel="Lọc theo bãi xe" options={lotOptions} portal portalFitContent searchable value={parkingLotId} onChange={(value) => { setParkingLotId(value); setPage(0); }} /></label>
          <label className="tw-mb-0 tw-grid tw-min-w-0 tw-gap-1.5 tw-text-xs tw-font-black tw-text-vm-slate-600">Điểm đánh giá<SelectMenu ariaLabel="Lọc theo số sao" options={ratingOptions} portal searchable={false} value={rating} onChange={(value) => { setRating(value); setPage(0); }} /></label>
          <DateRangeInput ariaLabel="Lọc đánh giá theo khoảng ngày" className="tw-min-w-0" label="Khoảng ngày" value={dateRange} onChange={(value) => { setDateRange(value); setPage(0); }} />
          <button className="tw-h-[42px] tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-slate-50 tw-px-4 tw-text-xs tw-font-black tw-text-vm-slate-600 hover:tw-bg-slate-100" onClick={resetFilters} type="button"><i className="fas fa-undo-alt tw-mr-2" />Đặt lại</button>
        </div>
        <div className="tw-mt-4 tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-2 tw-border-0 tw-border-t tw-border-solid tw-border-slate-100 tw-pt-4"><span className="tw-text-xs tw-font-semibold tw-text-vm-slate-500">{loadedAt ? `Cập nhật lần cuối: ${formatDateTime(loadedAt.toISOString())}` : "Đang chờ dữ liệu"}</span><span className="tw-rounded-full tw-bg-blue-50 tw-px-3 tw-py-1.5 tw-text-xs tw-font-black tw-text-vm-primary">{formatCount(data.totalElements)} kết quả phù hợp</span></div>
      </section>

      <section className="tw-mt-5 tw-overflow-hidden tw-rounded-2xl tw-border tw-border-solid tw-border-slate-100 tw-bg-white tw-shadow-sm">
        <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-slate-100 tw-px-5 tw-py-5"><div><h2 className="tw-m-0 tw-text-xl tw-font-black tw-text-vm-slate-900">{formatCount(data.totalElements)} lượt đánh giá</h2><p className="tw-m-0 tw-mt-1 tw-text-xs tw-font-semibold tw-text-vm-slate-500">Sắp xếp theo thời gian mới nhất</p></div>{rating ? <span className="tw-rounded-full tw-bg-amber-50 tw-px-3 tw-py-1.5 tw-text-xs tw-font-black tw-text-amber-700">Đang xem {rating} sao</span> : null}</div>

        {loading ? <LoadingRows /> : null}
        {!loading && error ? <div className="tw-m-5 tw-rounded-xl tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-p-5 tw-text-center"><i className="fas fa-exclamation-circle tw-text-2xl tw-text-red-500" /><strong className="tw-mt-2 tw-block tw-text-sm tw-text-red-700">Không thể tải đánh giá</strong><p className="tw-mb-0 tw-mt-1 tw-text-xs tw-font-semibold tw-text-red-600">{error}</p><button className="tw-mt-4 tw-rounded-lg tw-border-0 tw-bg-red-600 tw-px-4 tw-py-2 tw-text-xs tw-font-black tw-text-white" onClick={() => void loadReviews()} type="button">Thử lại</button></div> : null}
        {!loading && !error && data.content.length === 0 ? <div className="tw-grid tw-min-h-[260px] tw-place-items-center tw-p-6 tw-text-center"><div><span className="tw-mx-auto tw-grid tw-h-14 tw-w-14 tw-place-items-center tw-rounded-full tw-bg-slate-100 tw-text-xl tw-text-slate-400"><i className="far fa-comment-dots" /></span><strong className="tw-mt-4 tw-block tw-text-sm tw-text-vm-slate-800">Chưa có đánh giá phù hợp</strong><p className="tw-mb-0 tw-mt-1 tw-text-xs tw-font-semibold tw-text-vm-slate-500">Thử thay đổi bãi xe, số sao hoặc khoảng ngày.</p></div></div> : null}

        {!loading && !error && data.content.length > 0 ? <div className="tw-divide-y tw-divide-slate-100">
          <div className="tw-grid tw-grid-cols-[minmax(300px,1.35fr)_minmax(200px,.8fr)_minmax(170px,.65fr)] tw-gap-5 tw-bg-slate-50 tw-px-5 tw-py-3 tw-text-[0.68rem] tw-font-black tw-uppercase tw-tracking-wider tw-text-vm-slate-500 max-[900px]:tw-hidden"><span>Điểm và nhận xét</span><span>Bãi xe & khách hàng</span><span>Xác minh & thời gian</span></div>
          {data.content.map((review, index) => <article className="tw-grid tw-grid-cols-[minmax(300px,1.35fr)_minmax(200px,.8fr)_minmax(170px,.65fr)] tw-gap-5 tw-px-5 tw-py-5 hover:tw-bg-slate-50/60 max-[900px]:tw-grid-cols-1 max-[900px]:tw-gap-3" key={`${review.parkingLotName}-${review.createdAt}-${index}`}>
            <div className="tw-min-w-0"><span aria-label={`${review.rating} sao`} className="tw-text-base tw-tracking-[0.08em] tw-text-amber-400">{stars(review.rating)}</span><p className="tw-mb-0 tw-mt-2 tw-whitespace-pre-wrap tw-text-sm tw-font-semibold tw-leading-6 tw-text-vm-slate-700">{review.comment || "Khách hàng đã chấm điểm mà không để lại nhận xét."}</p>{review.edited ? <span className="tw-mt-2 tw-inline-block tw-text-[0.68rem] tw-font-bold tw-text-vm-slate-400">Đã chỉnh sửa</span> : null}</div>
            <div className="tw-min-w-0"><strong className="tw-block tw-truncate tw-text-sm tw-font-black tw-text-vm-slate-900">{review.parkingLotName || "Bãi xe"}</strong><span className="tw-mt-2 tw-flex tw-min-w-0 tw-items-center tw-gap-2 tw-text-xs tw-font-semibold tw-text-vm-slate-500"><span className="tw-grid tw-h-7 tw-w-7 tw-flex-shrink-0 tw-place-items-center tw-rounded-full tw-bg-blue-50 tw-text-vm-primary"><i className="fas fa-user" /></span><span className="tw-truncate">{review.reviewerDisplayName}</span><button aria-label={`Chat với ${review.reviewerDisplayName}`} className="tw-inline-flex tw-h-7 tw-w-7 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-border tw-border-solid tw-border-blue-100 tw-bg-blue-50 tw-text-vm-primary tw-transition hover:tw-border-blue-200 hover:tw-bg-blue-100 disabled:tw-cursor-wait disabled:tw-opacity-45" disabled={openingReviewerId !== null} onClick={() => void openReviewerConversation(review)} title={review.reviewerCustomerId ? "Mở hội thoại với người đánh giá" : "Backend chưa cung cấp định danh người đánh giá"} type="button"><i className={openingReviewerId !== null && openingReviewerId === review.reviewerCustomerId ? "fas fa-spinner fa-spin" : "far fa-comment-dots"} /></button></span></div>
            <div><span className="tw-inline-flex tw-items-center tw-gap-1.5 tw-rounded-full tw-bg-emerald-50 tw-px-2.5 tw-py-1.5 tw-text-[0.68rem] tw-font-black tw-text-emerald-700"><i className="fas fa-shield-alt" />Đã xác minh</span><time className="tw-mt-3 tw-block tw-text-xs tw-font-semibold tw-text-vm-slate-500" dateTime={review.createdAt}>{formatDateTime(review.createdAt)}</time></div>
          </article>)}
        </div> : null}

        {!loading && !error && data.totalElements > 0 ? <PaginationFooter ariaLabel="Phân trang đánh giá" currentPage={data.page + 1} endIndex={endIndex} onPageChange={(nextPage) => setPage(nextPage - 1)} onPageSizeChange={(nextSize) => { setPageSize(nextSize); setPage(0); }} pageSize={pageSize} pageSizeOptions={[10, 20, 50]} startIndex={startIndex} totalPages={Math.max(1, data.totalPages)} totalRecords={data.totalElements} /> : null}
      </section>
    </div>
  </div>;
}
