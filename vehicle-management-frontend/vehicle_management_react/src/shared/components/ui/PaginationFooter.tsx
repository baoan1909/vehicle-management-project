import { SelectMenu } from "@/shared/components/ui/SelectMenu";
import { cn } from "@/lib/cn";

type PageItem = number | "ellipsis";

type PaginationFooterProps = {
  ariaLabel?: string;
  currentPage: number;
  endIndex: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  pageSize: number;
  pageSizeOptions?: number[];
  startIndex: number;
  totalPages: number;
  totalRecords: number;
  className?: string;
};

function formatCount(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value);
}

function getVisiblePages(currentPage: number, totalPages: number): PageItem[] {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }

  if (currentPage <= 4) {
    return [1, 2, 3, 4, 5, "ellipsis", totalPages];
  }

  if (currentPage >= totalPages - 3) {
    return [1, "ellipsis", totalPages - 4, totalPages - 3, totalPages - 2, totalPages - 1, totalPages];
  }

  return [1, "ellipsis", currentPage - 1, currentPage, currentPage + 1, "ellipsis", totalPages];
}

export function PaginationFooter({
  ariaLabel = "Pagination",
  className = "",
  currentPage,
  endIndex,
  onPageChange,
  onPageSizeChange,
  pageSize,
  pageSizeOptions = [5, 10, 20],
  startIndex,
  totalPages,
  totalRecords
}: PaginationFooterProps) {
  const visiblePages = getVisiblePages(currentPage, totalPages);

  return (
    <div
      className={cn(
        "tw-flex tw-items-center tw-justify-between tw-gap-4 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-pb-4 tw-pt-[0.9rem] max-[900px]:tw-flex-col max-[900px]:tw-items-stretch",
        className,
      )}
    >
      <p className="tw-m-0 tw-text-[0.9rem] tw-font-normal tw-text-vm-slate-500">
        {totalRecords === 0
          ? "Không có bản ghi phù hợp"
          : `Hiển thị ${formatCount(startIndex)} đến ${formatCount(endIndex)} của ${formatCount(totalRecords)} bản ghi`}
      </p>

      <div className="tw-flex tw-items-center tw-gap-[1.15rem] max-[900px]:tw-flex-col max-[900px]:tw-items-stretch">
        <label className="tw-m-0 tw-flex tw-items-center tw-gap-3 tw-text-[0.9rem] tw-font-medium tw-text-vm-slate-500 max-[900px]:tw-flex-col max-[900px]:tw-items-stretch">
          <span>Số dòng mỗi trang</span>
          <SelectMenu
            ariaLabel="Số dòng mỗi trang"
            className="!tw-w-[72px] max-[900px]:!tw-w-full"
            value={String(pageSize)}
            options={pageSizeOptions.map((option) => ({ label: String(option), value: String(option) }))}
            placement="top"
            onChange={(nextValue) => onPageSizeChange(Number(nextValue))}
          />
        </label>

        <nav className="tw-flex tw-items-center tw-gap-[0.35rem] max-[900px]:tw-flex-wrap" aria-label={ariaLabel}>
          <button
            className="tw-inline-flex tw-h-8 tw-min-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-vm-slate-25 tw-px-[0.55rem] tw-text-[0.95rem] tw-font-bold tw-text-vm-slate-700 tw-transition hover:tw-bg-vm-slate-100 disabled:tw-cursor-not-allowed disabled:tw-text-vm-slate-300"
            disabled={currentPage === 1}
            type="button"
            onClick={() => onPageChange(currentPage - 1)}
          >
            <i className="fas fa-chevron-left" />
          </button>

          {visiblePages.map((item, index) =>
            item === "ellipsis" ? (
              <span className="tw-inline-flex tw-min-w-6 tw-items-center tw-justify-center tw-font-bold tw-text-vm-slate-500" key={`ellipsis-${index}`}>
                ...
              </span>
            ) : (
              <button
                className={cn(
                  "tw-inline-flex tw-h-8 tw-min-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-px-[0.55rem] tw-text-[0.95rem] tw-font-bold tw-text-vm-slate-700 tw-transition hover:tw-bg-vm-slate-25",
                  item === currentPage ? "!tw-text-white" : "",
                )}
                key={item}
                type="button"
                aria-current={item === currentPage ? "page" : undefined}
                style={
                  item === currentPage
                    ? {
                        background: "linear-gradient(135deg, #2563EB, #1D4ED8)",
                        color: "#ffffff"
                      }
                    : undefined
                }
                onClick={() => onPageChange(item)}
              >
                <span className={item === currentPage ? "tw-text-white" : undefined}>{item}</span>
              </button>
            )
          )}

          <button
            className="tw-inline-flex tw-h-8 tw-min-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-vm-slate-25 tw-px-[0.55rem] tw-text-[0.95rem] tw-font-bold tw-text-vm-slate-700 tw-transition hover:tw-bg-vm-slate-100 disabled:tw-cursor-not-allowed disabled:tw-text-vm-slate-300"
            disabled={currentPage === totalPages}
            type="button"
            onClick={() => onPageChange(currentPage + 1)}
          >
            <i className="fas fa-chevron-right" />
          </button>
        </nav>
      </div>
    </div>
  );
}
