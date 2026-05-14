import type { SelectOption } from "../../types/common";

interface FilterSelect {
  name: string;
  options: SelectOption[];
  placeholder?: string;
}

interface FilterControlsProps {
  selects?: FilterSelect[];
  showDateRange?: boolean;
  dateRangeValue?: string;
}

export function FilterControls({ selects = [], showDateRange = true, dateRangeValue = "01/05/2026 - 31/05/2026" }: FilterControlsProps) {
  return (
    <>
      {showDateRange && (
        <div className="form-group col-md-4 ml-auto">
          <div className="input-group">
            <input name="dateRange" type="text" className="form-control float-right" value={dateRangeValue} readOnly />
            <div className="input-group-prepend"><span className="input-group-text bg-cyan"><i className="far fa-calendar-alt" /></span></div>
          </div>
        </div>
      )}
      <div className="col-12 callout callout-info">
        <div className="row">
          {selects.map((select) => (
            <div className="col-md-2 mt-3" key={select.name}>
              <div className="form-group">
                <select name={select.name} className="form-control select2" defaultValue="">
                  <option value="">{select.placeholder ?? "Tất cả"}</option>
                  {select.options.map((option) => <option value={option.value} key={option.value}>{option.label}</option>)}
                </select>
              </div>
            </div>
          ))}
          <div className="col-md-2 mt-3">
            <div className="input-group"><div className="input-group-append"><button type="button" className="bg-cyan btn btn-sidebar"><i className="fa fa-filter" /> Lọc</button></div></div>
          </div>
          <div className="col-md-2 mt-3 ml-auto"><button type="button" className="btn btn-block btn-info">Đặt lại</button></div>
        </div>
      </div>
    </>
  );
}
