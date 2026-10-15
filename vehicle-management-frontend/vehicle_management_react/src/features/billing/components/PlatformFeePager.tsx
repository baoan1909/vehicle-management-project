import { cn } from "@/lib/cn";

type PlatformFeePagerProps = {
  currentPage: number;
  endIndex: number;
  onPageChange: (page: number) => void;
  startIndex: number;
  totalPages: number;
  totalRecords: number;
};

function formatCount(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value);
}

/** Phân trang khớp mockup Quản lý phí sàn: "Hiển thị 1 - 3 của 3 kết quả". */
export function PlatformFeePager({ currentPage, endIndex, onPageChange, startIndex, totalPages, totalRecords }: PlatformFeePagerProps) {
  const pages = Array.from({ length: totalPages }, (_, index) => index + 1);

  return (
    <div className="tw-flex tw-items-center tw-justify-between tw-gap-4 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-py-3 max-[700px]:tw-flex-col max-[700px]:tw-items-stretch">
      <p className="tw-m-0 tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-500">
        {totalRecords === 0
          ? "Không có kết quả phù hợp"
          : `Hiển thị ${formatCount(startIndex)} - ${formatCount(endIndex)} của ${formatCount(totalRecords)} kết quả`}
      </p>
      <nav aria-label="Phân trang phí sàn" className="tw-flex tw-items-center tw-gap-1.5">
        <button
          aria-label="Trang trước"
          className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-vm-slate-25 tw-text-[0.8rem] tw-text-vm-slate-700 tw-transition hover:tw-bg-vm-slate-100 disabled:tw-cursor-not-allowed disabled:tw-opacity-40"
          disabled={currentPage <= 1}
          type="button"
          onClick={() => onPageChange(currentPage - 1)}
        >
          <i className="fas fa-chevron-left" aria-hidden="true" />
        </button>
        {pages.map((item) => (
          <button
            aria-current={item === currentPage ? "page" : undefined}
            className={cn(
              "tw-inline-flex tw-h-8 tw-min-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-px-2 tw-text-[0.86rem] tw-font-bold tw-transition",
              item === currentPage
                ? "tw-bg-vm-primary tw-text-white"
                : "tw-bg-transparent tw-text-vm-slate-700 hover:tw-bg-vm-slate-25",
            )}
            key={item}
            type="button"
            onClick={() => onPageChange(item)}
          >
            {item}
          </button>
        ))}
        <button
          aria-label="Trang sau"
          className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-vm-slate-25 tw-text-[0.8rem] tw-text-vm-slate-700 tw-transition hover:tw-bg-vm-slate-100 disabled:tw-cursor-not-allowed disabled:tw-opacity-40"
          disabled={currentPage >= totalPages}
          type="button"
          onClick={() => onPageChange(currentPage + 1)}
        >
          <i className="fas fa-chevron-right" aria-hidden="true" />
        </button>
      </nav>
    </div>
  );
}
