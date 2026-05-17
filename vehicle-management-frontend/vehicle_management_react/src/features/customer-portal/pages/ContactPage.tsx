import { ClientPage } from "../../../shared/components/layout/ClientPage";

export function ContactPage() {
  return (
    <ClientPage title="Liên hệ">
      <div className="row justify-content-center">
        <div className="col-lg-10">
          <div className="vm-contact-card">
            <div className="row no-gutters">
              <div className="col-md-5">
                <div className="vm-contact-info">
                  <h2>Thông tin liên hệ</h2>
                  {[
                    ["fas fa-map-marker-alt", "Địa chỉ", "Số 1 Võ Văn Ngân, TP. Thủ Đức"],
                    ["fas fa-phone", "Điện thoại", "0901 000 001"],
                    ["fas fa-envelope", "Email", "support@parking.local"],
                  ].map(([icon, label, value]) => (
                    <div className="vm-contact-item" key={label}>
                      <div className="vm-contact-icon">
                        <i className={icon} />
                      </div>
                      <div>
                        <strong>{label}</strong>
                        <span>{value}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
              <div className="col-md-7">
                <div className="vm-form-section">
                  <h2>Gửi yêu cầu hỗ trợ</h2>
                  <div className="form-group">
                    <label>Họ tên</label>
                    <input className="form-control" placeholder="Nhập họ tên" />
                  </div>
                  <div className="form-group">
                    <label>Email</label>
                    <input className="form-control" placeholder="Nhập email" />
                  </div>
                  <div className="form-group">
                    <label>Nội dung</label>
                    <textarea className="form-control" rows={5} placeholder="Nhập nội dung cần hỗ trợ" />
                  </div>
                  <button className="btn btn-info" type="button">
                    <i className="fas fa-paper-plane" /> Gửi liên hệ
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </ClientPage>
  );
}
