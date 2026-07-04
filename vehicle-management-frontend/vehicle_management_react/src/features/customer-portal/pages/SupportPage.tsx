import { CustomerPageHeader, CustomerPortalLayout, Field, PaginationLite, StatCard, StatusPill } from "./PortalShared";

const tickets = [
  ["TK-00021", "Vấn đề thanh toán vé tháng", "HIGH", "IN_PROGRESS", "NV hỗ trợ 01", "22/05/2026 09:12", "--", "red", "orange"],
  ["TK-00020", "Sai biển số trong lịch sử gửi xe", "NORMAL", "OPEN", "--", "21/05/2026 18:40", "--", "blue", "blue"],
  ["TK-00019", "Yêu cầu cập nhật số điện thoại", "LOW", "RESOLVED", "NV hỗ trợ 02", "18/05/2026 10:22", "18/05/2026 11:05", "green", "green"],
  ["TK-00018", "Báo lỗi đăng ký vé tháng", "URGENT", "CLOSED", "NV hỗ trợ 01", "15/05/2026 08:30", "15/05/2026 09:15", "red", "gray"],
] as const;

export function SupportPage() {
  return (
    <CustomerPortalLayout>
      <CustomerPageHeader
        title="Hỗ trợ"
        subtitle="Gửi yêu cầu và theo dõi tình trạng xử lý hỗ trợ."
        action={<button type="button"><i className="fas fa-plus" /> Tạo ticket mới</button>}
      />

      <div className="vm-support-layout">
        <div>
          <div className="vm-stat-grid vm-stat-grid-three">
            <StatCard icon="far fa-question-circle" label="Ticket đang mở" value="2" note={<StatusPill tone="blue">OPEN</StatusPill>} />
            <StatCard icon="fas fa-headset" label="Đang xử lý" value="1" note={<StatusPill tone="orange">IN_PROGRESS</StatusPill>} tone="orange" />
            <StatCard icon="far fa-check-circle" label="Đã giải quyết" value="5" note={<StatusPill>RESOLVED</StatusPill>} tone="green" />
          </div>
          <section className="vm-customer-card vm-table-card">
            <h2>Danh sách yêu cầu hỗ trợ</h2>
            <div className="vm-table-filters">
              <label><i className="fas fa-search" /><input placeholder="Tìm theo tiêu đề..." /></label>
              <select defaultValue="Tất cả"><option>Tất cả</option></select>
              <select defaultValue="Tất cả"><option>Tất cả</option></select>
            </div>
            <table className="vm-customer-table">
              <thead><tr><th>Mã ticket</th><th>Tiêu đề</th><th>Mức độ</th><th>Trạng thái</th><th>Người xử lý</th><th>Ngày tạo</th><th>Ngày xử lý</th><th>Thao tác</th></tr></thead>
              <tbody>
                {tickets.map((row) => (
                  <tr key={row[0]}>
                    <td>{row[0]}</td><td>{row[1]}</td><td><StatusPill tone={row[7]}>{row[2]}</StatusPill></td><td><StatusPill tone={row[8]}>{row[3]}</StatusPill></td><td>{row[4]}</td><td>{row[5]}</td><td>{row[6]}</td>
                    <td><button className="vm-mini-btn" type="button"><i className="far fa-eye" /> Chi tiết</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
            <PaginationLite />
          </section>
        </div>

        <aside className="vm-support-side">
          <section className="vm-customer-card vm-side-form">
            <h2>Tạo yêu cầu hỗ trợ</h2>
            <Field label="Tiêu đề"><input defaultValue="Vấn đề thanh toán" /></Field>
            <Field label="Mức độ ưu tiên"><select defaultValue="NORMAL"><option>NORMAL</option></select></Field>
            <Field label="Nội dung"><textarea placeholder="Mô tả chi tiết vấn đề bạn cần hỗ trợ..." /></Field>
            <button type="button">Gửi yêu cầu</button>
            <small><i className="fas fa-info-circle" /> Ticket mới sẽ ở trạng thái OPEN.</small>
          </section>

          <section className="vm-customer-card vm-ticket-detail">
            <h2>Chi tiết ticket TK-00021</h2>
            <dl className="vm-info-list">
              <dt>Khách hàng:</dt><dd>Nguyễn Văn A</dd>
              <dt>Trạng thái:</dt><dd><StatusPill tone="orange">IN_PROGRESS</StatusPill></dd>
              <dt>Mức độ:</dt><dd><StatusPill tone="red">HIGH</StatusPill></dd>
              <dt>Người xử lý:</dt><dd>NV hỗ trợ 01</dd>
              <dt>Ngày tạo:</dt><dd>22/05/2026 09:12</dd>
              <dt>Ngày giải quyết:</dt><dd>--</dd>
            </dl>
            <h3>Nội dung</h3>
            <p>Tôi đã thanh toán vé tháng nhưng trạng thái vẫn chưa cập nhật.</p>
            <div className="vm-ticket-steps">
              <span className="done">OPEN<small>22/05/2026 09:12</small></span>
              <span className="current">IN_PROGRESS<small>22/05/2026 09:25</small></span>
              <span>RESOLVED<small>--</small></span>
              <span>CLOSED<small>--</small></span>
            </div>
          </section>
        </aside>
      </div>

      <div className="vm-info-note"><i className="fas fa-info-circle" /> Bạn có thể theo dõi tình trạng xử lý tại trang này. Khi ticket được RESOLVED, bạn có thể xác nhận và đóng yêu cầu.</div>
    </CustomerPortalLayout>
  );
}
