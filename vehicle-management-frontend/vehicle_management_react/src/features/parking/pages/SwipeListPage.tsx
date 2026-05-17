import { Link } from "react-router-dom";
import { AdminTablePage } from "../../../shared/components/table/AdminTablePage";
import { FilterControls } from "../../../shared/components/form/FilterControls";
import { swipes, ticketTypes, vehicleTypes } from "../../../shared/data/mockData";
import type { TableColumn } from "../../../shared/types/common";

type SwipeRow = (typeof swipes)[number];

const columns: TableColumn<SwipeRow>[] = [
  { key: "stt", label: "STT", render: (_row, index) => index + 1 },
  { key: "id", label: "ID quét thẻ" },
  { key: "cardId", label: "ID thẻ" },
  { key: "licensePlate", label: "Biển số" },
  { key: "checkInTime", label: "Thời gian vào" },
  { key: "checkOutTime", label: "Thời gian ra" },
  { key: "vehicleType", label: "Loại xe" },
  { key: "cardType", label: "Loại thẻ" },
  { key: "price", label: "Phí DV" },
];

export function SwipeListPage() {
  return (
    <AdminTablePage
      title="Quản lý vào ra"
      breadcrumbs={[{ label: "Quản lý", href: "/admin/dashboard" }, { label: "Vào ra" }]}
      tableTitle="Bảng quản lý thông tin vào ra"
      columns={columns}
      rows={swipes}
      filters={
        <FilterControls
          selects={[
            { name: "vehicleTypeId", placeholder: "Tất cả xe", options: vehicleTypes },
            { name: "ticketTypeId", placeholder: "Tất cả vé", options: ticketTypes },
          ]}
        />
      }
      actions={
        <div className="form-group col-md-3 ml-auto mr-3">
          <div className="row">
            <div className="col-md-6">
              <Link to="/admin/swipe/swipein" className="btn btn-info btn-block">
                <i className="fas fa-plus-circle" /> Xe vào
              </Link>
            </div>
            <div className="col-md-6">
              <Link to="/admin/swipe/swipeout" className="btn btn-outline-warning btn-block">
                <i className="fas fa-plus-circle" /> Xe ra
              </Link>
            </div>
          </div>
        </div>
      }
    />
  );
}
