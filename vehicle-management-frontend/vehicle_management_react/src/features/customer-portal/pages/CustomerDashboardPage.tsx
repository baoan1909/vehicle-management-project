import { Link } from "react-router-dom";
import { CustomerPortalLayout, StatusPill } from "./PortalShared";

const historyRows = [
  ["59C1-123.45", "19/05/2024 18:23", "19/05/2024 20:05"],
  ["59C1-123.45", "18/05/2024 07:45", "18/05/2024 12:10"],
  ["59C2-678.90", "17/05/2024 19:02", "17/05/2024 21:15"],
  ["59C1-123.45", "16/05/2024 08:11", "16/05/2024 17:35"],
  ["59C2-678.90", "15/05/2024 18:40", "15/05/2024 20:30"],
];

export function CustomerDashboardPage() {
  return (
    <CustomerPortalLayout>
      <div className="vm-dashboard-cards">
        <article className="vm-customer-card">
          <h2>Vé tháng hiện tại <StatusPill>Đang hoạt động</StatusPill></h2>
          <dl className="vm-info-list">
            <dt>Gói vé</dt><dd>Xe máy - Tháng</dd>
            <dt>Hiệu lực</dt><dd>01/05/2024 - 31/05/2024</dd>
            <dt>Biển số</dt><dd>59C1-123.45</dd>
          </dl>
          <Link className="vm-outline-btn" to="/customer/subscriptions">Xem chi tiết</Link>
        </article>
        <article className="vm-customer-card vm-center-card">
          <h2>Xe đã đăng ký</h2>
          <div className="vm-big-metric"><span><i className="fas fa-motorcycle" /></span><b>2</b><small>xe</small></div>
          <Link className="vm-outline-btn" to="/customer/vehicles">Quản lý xe</Link>
        </article>
        <article className="vm-customer-card">
          <h2>Lần gửi xe gần nhất</h2>
          <dl className="vm-info-list">
            <dt>Biển số</dt><dd>59C1-123.45</dd>
            <dt>Thời gian vào</dt><dd>19/05/2024 18:23</dd>
            <dt>Thời gian ra</dt><dd>19/05/2024 20:05</dd>
            <dt>Trạng thái</dt><dd><StatusPill>Đã thanh toán</StatusPill></dd>
          </dl>
          <Link className="vm-outline-btn" to="/customer/parking-history">Xem lịch sử</Link>
        </article>
      </div>

      <section className="vm-customer-card vm-table-card">
        <h2>Lịch sử gửi xe</h2>
        <table className="vm-customer-table">
          <thead><tr><th>Biển số</th><th>Thời gian vào</th><th>Thời gian ra</th><th>Trạng thái</th></tr></thead>
          <tbody>
            {historyRows.map((row) => (
              <tr key={row.join("-")}><td>{row[0]}</td><td>{row[1]}</td><td>{row[2]}</td><td><StatusPill>Đã thanh toán</StatusPill></td></tr>
            ))}
          </tbody>
        </table>
        <Link className="vm-more-link" to="/customer/parking-history">Xem tất cả <i className="fas fa-arrow-right" /></Link>
      </section>

      <div className="vm-quick-forms">
        <article className="vm-customer-card">
          <h2>Cập nhật hồ sơ</h2>
          <label>Họ và tên<input defaultValue="Nguyễn Văn A" /></label>
          <label>Số điện thoại<input defaultValue="0901 234 567" /></label>
          <label>Email<input defaultValue="nguyenvana@gmail.com" /></label>
          <button type="button">Lưu thay đổi</button>
        </article>
        <article className="vm-customer-card">
          <h2>Thêm xe mới</h2>
          <label>Loại xe<select defaultValue="Xe máy"><option>Xe máy</option></select></label>
          <label>Biển số<input defaultValue="59C1-987.65" /></label>
          <label>Nhãn xe (tùy chọn)<input defaultValue="Honda Vision" /></label>
          <button type="button">Thêm xe</button>
        </article>
        <article className="vm-customer-card">
          <h2>Tạo yêu cầu hỗ trợ</h2>
          <label>Chủ đề<select defaultValue="Vấn đề thanh toán"><option>Vấn đề thanh toán</option></select></label>
          <label>Nội dung<textarea placeholder="Vui lòng mô tả chi tiết vấn đề của bạn..." /></label>
          <button type="button">Gửi yêu cầu</button>
        </article>
      </div>
    </CustomerPortalLayout>
  );
}
