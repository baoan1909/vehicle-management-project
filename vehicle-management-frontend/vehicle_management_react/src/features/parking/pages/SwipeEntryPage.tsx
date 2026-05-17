import { useState, type ChangeEvent } from "react";
import { Link } from "react-router-dom";
import { AdminPage } from "../../../shared/components/layout/AdminPage";
import { cards } from "../../../shared/data/mockData";

interface SwipeEntryPageProps {
  mode: "in" | "out";
}

export function SwipeEntryPage({ mode }: SwipeEntryPageProps) {
  const [preview, setPreview] = useState("");
  const isIn = mode === "in";
  const title = isIn ? "Quản lý xe vào" : "Quản lý xe ra";

  function handlePreview(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (file) setPreview(URL.createObjectURL(file));
  }

  return (
    <AdminPage title={title} breadcrumbs={[{ label: "Quản lý vào ra", href: "/admin/swipe" }, { label: isIn ? "Xe vào" : "Xe ra" }]}>
      <div className="form-group col-md-1 ml-auto mr-3">
        <Link to="/admin/swipe" className="btn btn-outline-dark btn-block">
          <i className="fas fa-arrow-alt-circle-left" /> Thoát
        </Link>
      </div>
      <form>
        <div className="row ml-3 mr-3">
          <div className="col-md-5">
            <div className="card card-cyan">
              <div className="card-header">
                <h3 className="card-title">Hình ảnh xe {isIn ? "vào" : "ra"}</h3>
              </div>
              <div className="card-body">
                <div className="vm-image-preview">
                  {preview ? <img src={preview} alt="Ảnh xe" className="img-fluid" /> : <span className="text-muted">Chưa chọn ảnh</span>}
                </div>
              </div>
              <div className="card-footer">
                <div className="form-group">
                  <div className="custom-file">
                    <input type="file" className="custom-file-input" id="vehicleImage" accept="image/*" onChange={handlePreview} />
                    <label className="custom-file-label" htmlFor="vehicleImage">Chọn ảnh</label>
                  </div>
                </div>
                <button type="button" className="btn btn-info" style={{ width: "100%" }}>
                  <i className="fas fa-qrcode" /> Quét biển số từ ảnh
                </button>
              </div>
            </div>
          </div>
          <div className="col-md-7">
            <div className="card card-cyan card-outline shadow">
              <div className="card-header">
                <h3 className="card-title">Thông tin xe {isIn ? "vào" : "ra"}</h3>
                <div className="card-tools">
                  <button type="button" className="btn btn-tool">
                    <i className="fas fa-minus" />
                  </button>
                </div>
              </div>
              <div className="card-body">
                <div className="form-group">
                  <label>ID thẻ xe</label>
                  <select className="form-control select2" defaultValue="">
                    <option value="">-- Chọn thẻ --</option>
                    {cards.map((card) => (
                      <option value={card.id} key={card.id}>
                        (ID:{card.id}) {card.number}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Loại thẻ</label>
                  <input type="text" className="form-control" placeholder="-- Loại thẻ --" />
                </div>
                <div className="form-group">
                  <label>Loại xe</label>
                  <input type="text" className="form-control" placeholder="-- Loại xe --" />
                </div>
                <div className="form-group">
                  <label>Biển số</label>
                  <input type="text" className="form-control" placeholder="80H-1826-127" />
                </div>
                {!isIn && (
                  <div className="form-group">
                    <label>Phí dịch vụ</label>
                    <input type="text" className="form-control" placeholder="5.000đ" />
                  </div>
                )}
                <div className="form-group">
                  <label>Thời gian {isIn ? "vào" : "ra"}:</label>
                  <div className="input-group date">
                    <input type="text" className="form-control datetimepicker-input" placeholder="14/05/2026 08:00" />
                    <div className="input-group-append">
                      <div className="input-group-text bg-cyan">
                        <i className="fa fa-calendar" />
                      </div>
                    </div>
                  </div>
                </div>
                <div className="row">
                  <div className="col-12">
                    <Link to={isIn ? "/admin/swipe/swipein" : "/admin/swipe/swipeout"} className="btn btn-outline-warning">
                      Hủy
                    </Link>
                    <button type="button" className="btn btn-info float-right">
                      <i className="fas fa-save" /> Lưu
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </form>
    </AdminPage>
  );
}
