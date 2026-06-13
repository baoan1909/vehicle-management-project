import { CustomerPageHeader, CustomerPortalLayout, Field, PaginationLite, StatCard, StatusPill } from "./PortalShared";

const subscriptions = [
  ["SUB-000123", "59C1-123.45", "Xe máy - Tháng", "01/05/2024 - 31/05/2024", "120.000 VNĐ", "ACTIVE", "green"],
  ["SUB-000122", "59C1-123.45", "Xe máy - Tháng", "01/04/2024 - 30/04/2024", "120.000 VNĐ", "EXPIRED", "gray"],
  ["SUB-000121", "59C1-987.65", "Xe máy - Tháng", "01/06/2024 - 30/06/2024", "120.000 VNĐ", "PENDING", "orange"],
  ["SUB-000120", "59C1-123.45", "Xe máy - Tháng", "01/03/2024 - 31/03/2024", "120.000 VNĐ", "CANCELLED", "red"],
  ["SUB-000119", "59C2-345.67", "Xe máy - Tháng", "01/02/2024 - 29/02/2024", "120.000 VNĐ", "REJECTED", "red"],
] as const;

export function SubscriptionPage() {
  return (
    <CustomerPortalLayout>
      <CustomerPageHeader
        title="Vé tháng"
        subtitle="Đăng ký, gia hạn và theo dõi vé gửi xe của bạn"
        action={<button type="button"><i className="fas fa-plus" /> Đăng ký vé mới</button>}
      />
      <div className="vm-stat-grid vm-stat-grid-four">
        <StatCard icon="far fa-calendar-check" label="Vé đang hoạt động" value="1" note="vé" tone="green" />
        <StatCard icon="far fa-clock" label="Sắp hết hạn" value="09 ngày" note="31/05/2024" tone="orange" />
        <StatCard icon="fas fa-hourglass-half" label="Chờ duyệt" value="1" note="vé" tone="orange" />
        <StatCard icon="fas fa-wallet" label="Tổng phí tháng này" value="120.000 VNĐ" />
      </div>

      <section className="vm-customer-card vm-subscription-current">
        <div className="vm-section-title-row">
          <h2>Vé tháng hiện tại <StatusPill>ACTIVE</StatusPill></h2>
          <div className="vm-progress-inline"><span>Thời hạn còn lại</span><b>09 ngày</b><em><i /></em><strong>74%</strong></div>
        </div>
        <div className="vm-subscription-detail">
          <dl className="vm-info-list">
            <dt><i className="far fa-hashtag" /> Mã vé</dt><dd>SUB-000123</dd>
            <dt><i className="fas fa-motorcycle" /> Xe đăng ký</dt><dd>59C1-123.45 - Honda Vision</dd>
            <dt><i className="far fa-id-badge" /> Loại vé</dt><dd>Xe máy - Tháng</dd>
            <dt><i className="fas fa-tags" /> Quy tắc giá</dt><dd>Vé tháng xe máy</dd>
            <dt><i className="far fa-credit-card" /> Thẻ sử dụng</dt><dd>CARD-00123</dd>
          </dl>
          <dl className="vm-info-list">
            <dt><i className="far fa-calendar" /> Ngày nhận thẻ</dt><dd>01/05/2024</dd>
            <dt><i className="far fa-clock" /> Hiệu lực từ</dt><dd>01/05/2024</dd>
            <dt><i className="far fa-calendar-check" /> Hiệu lực đến</dt><dd>31/05/2024</dd>
            <dt><i className="far fa-money-bill-alt" /> Giá vé</dt><dd>120.000 VNĐ</dd>
            <dt><i className="far fa-question-circle" /> Ngày duyệt</dt><dd>01/05/2024 08:30</dd>
          </dl>
        </div>
        <div className="vm-form-actions"><button className="vm-outline-btn" type="button">Xem chi tiết</button><button type="button">Gia hạn vé</button></div>
      </section>

      <div className="vm-two-column-even">
        <section className="vm-customer-card vm-table-card">
          <h2>Lịch sử đăng ký vé</h2>
          <div className="vm-table-filters">
            <select defaultValue="Tất cả"><option>Tất cả</option></select>
            <select defaultValue="Tất cả"><option>Tất cả</option></select>
            <label><i className="fas fa-search" /><input placeholder="Tìm biển số..." /></label>
          </div>
          <table className="vm-customer-table">
            <thead><tr><th>Mã vé</th><th>Biển số</th><th>Loại vé</th><th>Hiệu lực</th><th>Giá</th><th>Trạng thái</th></tr></thead>
            <tbody>
              {subscriptions.map((row) => (
                <tr key={row[0]}><td>{row[0]}</td><td>{row[1]}</td><td>{row[2]}</td><td>{row[3]}</td><td>{row[4]}</td><td><StatusPill tone={row[6]}>{row[5]}</StatusPill></td></tr>
              ))}
            </tbody>
          </table>
          <PaginationLite />
        </section>

        <section className="vm-customer-card vm-side-form">
          <h2>Đăng ký / gia hạn nhanh</h2>
          <div className="vm-form-grid">
            <Field label="Xe đăng ký"><select defaultValue="59C1-123.45 - Honda Vision"><option>59C1-123.45 - Honda Vision</option></select></Field>
            <Field label="Loại vé"><select defaultValue="Vé tháng"><option>Vé tháng</option></select></Field>
            <Field label="Bảng giá"><select defaultValue="Vé tháng xe máy - 120.000 VNĐ"><option>Vé tháng xe máy - 120.000 VNĐ</option></select></Field>
            <Field label="Ngày bắt đầu"><input defaultValue="01/06/2024" /></Field>
            <Field label="Ngày kết thúc"><input defaultValue="30/06/2024" /></Field>
            <Field label="Tổng phí"><input defaultValue="120.000 VNĐ" readOnly /></Field>
            <Field label="Thẻ hiện có (tùy chọn)"><select defaultValue="CARD-00123"><option>CARD-00123</option></select></Field>
          </div>
          <button type="button">Gửi đăng ký</button>
          <small><i className="fas fa-info-circle" /> Yêu cầu mới sẽ ở trạng thái PENDING cho đến khi được duyệt.</small>
        </section>
      </div>

      <section className="vm-customer-card vm-note-list">
        <h2>Lưu ý về vé tháng</h2>
        <ul>
          <li><i className="fas fa-check-circle" /> Chỉ xe ACTIVE mới được đăng ký vé</li>
          <li><i className="fas fa-check-circle" /> Vé mới cần được duyệt trước khi kích hoạt</li>
          <li><i className="fas fa-check-circle" /> Có thể gia hạn trước ngày hết hạn</li>
          <li><i className="fas fa-check-circle" /> Vé CANCELLED hoặc REJECTED không thể kích hoạt lại trực tiếp</li>
        </ul>
      </section>
    </CustomerPortalLayout>
  );
}
