import { SelectMenu } from "@/shared/components/ui/SelectMenu";
import { useState } from "react";
import type { SelectOption } from "../../types/common";

interface FilterSelect {
  name: string;
  options: SelectOption[];
  placeholder?: string;
}

interface FilterControlsProps {
  selects?: FilterSelect[];
  showDateRange?: boolean;
  defaultStartDate?: string;
  defaultEndDate?: string;
}

export function FilterControls({
  selects = [],
  showDateRange = true,
  defaultStartDate = "2026-05-01",
  defaultEndDate = "2026-05-31",
}: FilterControlsProps) {
  const [startDate, setStartDate] = useState(defaultStartDate);
  const [endDate, setEndDate] = useState(defaultEndDate);
  const [selectValues, setSelectValues] = useState<Record<string, string>>(() =>
    Object.fromEntries(selects.map((select) => [select.name, ""])),
  );

  function resetFilters() {
    setStartDate(defaultStartDate);
    setEndDate(defaultEndDate);
    setSelectValues(Object.fromEntries(selects.map((select) => [select.name, ""])));
  }

  return (
    <>
      {showDateRange && (
        <div className="col-12 vm-filter-date">
          <div className="vm-date-range">
            <div className="vm-date-field">
              <label>Từ ngày</label>
              <div className="vm-date-input-wrap">
                <input
                  name="startDate"
                  type="date"
                  className="form-control vm-form-control vm-date-input"
                  value={startDate}
                  onChange={(event) => setStartDate(event.target.value)}
                />
                <span className="vm-date-picker-icon" aria-hidden="true">
                  <i className="far fa-calendar-alt" />
                </span>
              </div>
            </div>
            <div className="vm-date-field">
              <label>Đến ngày</label>
              <div className="vm-date-input-wrap">
                <input
                  name="endDate"
                  type="date"
                  className="form-control vm-form-control vm-date-input"
                  value={endDate}
                  onChange={(event) => setEndDate(event.target.value)}
                />
                <span className="vm-date-picker-icon" aria-hidden="true">
                  <i className="far fa-calendar-alt" />
                </span>
              </div>
            </div>
          </div>
        </div>
      )}
      <div className="col-12 callout callout-info vm-filter-panel">
        <div className="row align-items-end">
          {selects.map((select) => (
            <div className="col-md-2 mt-3" key={select.name}>
              <div className="form-group">
                <SelectMenu ariaLabel={select.placeholder ?? select.name}
                  name={select.name}
                  value={selectValues[select.name] ?? ""}
                  onChange={(selectedValue) => setSelectValues((current) => ({ ...current, [select.name]: selectedValue }))}
                  options={[{ label: select.placeholder ?? "Tất cả", value: "" }, ...select.options.map((option) => ({ label: option.label, value: option.value }))]} portal clearValue="" />
              </div>
            </div>
          ))}
          <div className="col-md-2 mt-3">
            <div className="input-group">
              <div className="input-group-append">
                <button type="button" className="btn vm-btn-primary">
                  <i className="fa fa-filter" /> Lọc
                </button>
              </div>
            </div>
          </div>
          <div className="col-md-2 mt-3 ml-auto">
            <button type="button" className="btn btn-block vm-btn-reset" onClick={resetFilters}>
              Đặt lại
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
