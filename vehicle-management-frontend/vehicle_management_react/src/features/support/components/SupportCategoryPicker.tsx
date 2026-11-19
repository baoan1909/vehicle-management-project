import { useEffect } from "react";
import { SelectMenu } from "@/shared/components/ui/SelectMenu";
import type { useAvailableSupportCategories } from "@/features/support/hooks/useAvailableSupportCategories";

export function SupportCategoryPicker({ state, value, onChange, disabled = false }: {
  state: ReturnType<typeof useAvailableSupportCategories>;
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
}) {
  const { categories, error, loading, reload } = state;
  const selected = categories.find((category) => category.categoryId === value);
  useEffect(() => {
    if (!loading && !error && value && !selected) onChange("");
  }, [error, loading, onChange, selected, value]);

  return <div className="tw-grid tw-min-w-0 tw-gap-2">
    <div className="tw-flex tw-items-center tw-justify-between tw-gap-2">
      <span className="tw-text-sm tw-font-bold tw-text-slate-700">Danh mục hỗ trợ</span>
      <button type="button" aria-label="Làm mới danh mục hỗ trợ" title="Làm mới danh mục hỗ trợ"
        disabled={loading || disabled} onClick={() => void reload()}
        className="tw-inline-flex tw-h-8 tw-w-8 tw-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-transparent tw-text-vm-primary hover:tw-bg-slate-100 disabled:tw-opacity-50">
        <i aria-hidden="true" className={loading ? "fas fa-sync-alt fa-spin" : "fas fa-sync-alt"} />
      </button>
    </div>
    <SelectMenu ariaLabel="Danh mục hỗ trợ" clearValue="" value={value} onChange={onChange}
      disabled={disabled || loading || Boolean(error) || categories.length === 0}
      options={[{ label: loading ? "Đang tải danh mục..." : "Chọn danh mục", value: "" },
        ...categories.map((category) => ({ label: category.name, value: category.categoryId }))]}
      searchable searchPlaceholder="Tìm danh mục hỗ trợ..." portal />
    {loading ? <p role="status" className="tw-m-0 tw-text-sm tw-text-slate-500">Đang tải danh mục hỗ trợ...</p> : null}
    {error ? <p role="alert" className="tw-m-0 tw-text-sm tw-text-red-700">{error}</p> : null}
    {!loading && !error && categories.length === 0
      ? <p role="status" className="tw-m-0 tw-text-sm tw-text-slate-500">Chưa có danh mục hỗ trợ khả dụng cho tài khoản của bạn.</p> : null}
    {selected?.description ? <p className="tw-m-0 tw-text-sm tw-font-normal tw-leading-5 tw-text-slate-600">{selected.description}</p> : null}
  </div>;
}
