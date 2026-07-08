import { ClientPage } from "../../../shared/components/layout/ClientPage";
import { DataTable } from "../../../shared/components/table/DataTable";
import { customerHistory } from "../../../shared/data/mockData";
import type { TableColumn } from "../../../shared/types/common";

type HistoryRow = (typeof customerHistory)[number];

const columns: TableColumn<HistoryRow>[] = [
  { key: "stt", label: "STT", render: (_row, index) => index + 1 },
  { key: "fullName", label: "Tên chủ xe" },
  { key: "licensePlate", label: "Biển số xe" },
  { key: "checkInTime", label: "Giờ vào gần nhất" },
  { key: "checkOutTime", label: "Giờ ra gần nhất" },
  { key: "effectiveDate", label: "Ngày hiệu lực" },
  { key: "expirationDate", label: "Ngày hết hạn" },
];

export function CustomerHistoryPage() {
  return (
    <ClientPage title="Thông tin khách hàng">
      <div className="row mt-4">
        <div className="col-md-3">
          <div
            className="info-box"
            style={{ background: "linear-gradient(135deg, var(--vm-indigo), #3a0ca3)", color: "white", boxShadow: "0 4px 15px rgba(67, 97, 238, 0.3)" }}
          >
            <div className="info-box-content">
              <label htmlFor="filter">Xem theo:</label>
              <select name="filter" id="filter" className="form-control select2">
                <option value="">Tất cả loại xe</option>
                <option value="month">1 tháng gần nhất</option>
                <option value="all">Tất cả lịch sử</option>
              </select>
            </div>
          </div>
        </div>
      </div>
      <div className="row">
        <div className="col-12">
          <div className="card">
            <div
              className="card-header"
              style={{ background: "linear-gradient(135deg, var(--vm-indigo), #3a0ca3)", color: "white", boxShadow: "0 4px 15px rgba(67, 97, 238, 0.3)" }}
            >
              <h3 className="card-title">Bảng quản lý thông tin thẻ</h3>
            </div>
            <div className="card-body">
              <DataTable columns={columns} rows={customerHistory} />
            </div>
          </div>
        </div>
      </div>
    </ClientPage>
  );
}
