import { CustomerPageHeader, CustomerPortalLayout, Field, PaginationLite, StatCard, StatusPill } from "./PortalShared";

const sessions = [
  ["PS-000128", "59C1-123.45", "59C1-123.45", "22/05/2026 08:12", "-", "OPEN", "-", "blue"],
  ["PS-000127", "59C1-123.45", "59C1-123.45", "21/05/2026 18:23", "21/05/2026 20:05", "CLOSED", "5.000 VNĐ", "green"],
  ["PS-000126", "59C2-678.90", "59C2-678.90", "20/05/2026 07:45", "20/05/2026 12:10", "CLOSED", "4.000 VNĐ", "green"],
  ["PS-000125", "59C1-123.45", "-", "19/05/2026 22:30", "20/05/2026 06:10", "LOST_CARD", "135.000 VNĐ", "orange"],
  ["PS-000124", "59A-456.78", "59A-456.78", "18/05/2026 09:00", "18/05/2026 09:05", "CANCELLED", "0 VNĐ", "red"],
] as const;

export function CustomerHistoryPage() {
  return (
    <CustomerPortalLayout>
      <CustomerPageHeader
        title="Lịch sử gửi xe"
        subtitle="Theo dõi các phiên gửi xe và sự kiện vào ra của bạn"
        action={<div className="vm-header-actions"><button className="vm-outline-action" type="button"><i className="far fa-file-alt" /> Xuất Excel</button><button type="button"><i className="far fa-file-alt" /> Tải hóa đơn</button></div>}
      />

      <div className="vm-stat-grid">
        <StatCard icon="far fa-clipboard" label="Tổng phiên gửi" value="28" note="phiên" />
        <StatCard icon="fas fa-car" label="Đang gửi" value="1" note={<StatusPill tone="blue">OPEN</StatusPill>} />
        <StatCard icon="far fa-check-circle" label="Đã hoàn tất" value="26" note={<StatusPill>CLOSED</StatusPill>} tone="green" />
        <StatCard icon="fas fa-wallet" label="Tổng phí tháng này" value="86.000 VNĐ" tone="purple" />
      </div>

      <section className="vm-customer-card vm-filter-row-card">
        <Field label="Từ ngày"><input defaultValue="01/05/2024" /></Field>
        <Field label="Đến ngày"><input defaultValue="22/05/2024" /></Field>
        <Field label="Biển số"><input defaultValue="59C1-123.45" /></Field>
        <Field label="Trạng thái"><select defaultValue="Tất cả"><option>Tất cả</option></select></Field>
        <button type="button"><i className="fas fa-search" /> Lọc dữ liệu</button>
      </section>

      <div className="vm-history-layout">
        <section className="vm-customer-card vm-table-card">
          <h2>Danh sách phiên gửi xe</h2>
          <table className="vm-customer-table">
            <thead><tr><th>Mã phiên</th><th>Biển số vào</th><th>Biển số ra</th><th>Thời gian vào</th><th>Thời gian ra</th><th>Trạng thái</th><th>Tổng phí</th><th>Thao tác</th></tr></thead>
            <tbody>
              {sessions.map((row) => (
                <tr key={row[0]}>
                  <td>{row[0]}</td><td>{row[1]}</td><td>{row[2]}</td><td>{row[3]}</td><td>{row[4]}</td>
                  <td><StatusPill tone={row[7]}>{row[5]}</StatusPill></td><td>{row[6]}</td>
                  <td><button className="vm-mini-btn" type="button">Chi tiết</button></td>
                </tr>
              ))}
            </tbody>
          </table>
          <PaginationLite />
        </section>

        <aside className="vm-customer-card vm-session-detail">
          <h2>Chi tiết phiên PS-000127</h2>
          <dl className="vm-info-list">
            <dt>Thẻ:</dt><dd>CARD-00123</dd>
            <dt>Loại xe:</dt><dd>Xe máy</dd>
            <dt>Vị trí đỗ:</dt><dd>A-12</dd>
            <dt>Quy tắc giá:</dt><dd>Khung giờ 18:00 - 23:59</dd>
            <dt>Tổng phí:</dt><dd className="vm-blue-text">5.000 VNĐ</dd>
          </dl>
          <h3>Sự kiện vào/ra</h3>
          <div className="vm-event-list">
            <div><span><i className="fas fa-sign-in-alt" /></span><b>CHECK_IN</b><em>21/05/2026 18:23<br />59C1-123.45</em></div>
            <div><span><i className="fas fa-sign-out-alt" /></span><b>CHECK_OUT</b><em>21/05/2026 20:05<br />59C1-123.45</em></div>
          </div>
          <div className="vm-image-row">
            <div><strong>Ảnh xe vào</strong><span><i className="far fa-camera" />/images/entry/127_in.jpg</span></div>
            <div><strong>Ảnh xe ra</strong><span><i className="far fa-camera" />/images/exit/127_out.jpg</span></div>
          </div>
        </aside>
      </div>

      <div className="vm-info-note"><i className="fas fa-info-circle" /> Dữ liệu lịch sử chỉ hiển thị các phiên thuộc tài khoản của bạn. Nếu biển số nhận diện sai, vui lòng gửi yêu cầu hỗ trợ.</div>
    </CustomerPortalLayout>
  );
}
