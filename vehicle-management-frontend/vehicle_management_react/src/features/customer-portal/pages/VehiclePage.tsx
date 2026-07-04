import { CustomerPageHeader, CustomerPortalLayout, Field, PaginationLite, StatCard, StatusPill } from "./PortalShared";

const vehicles = [
  ["59C1-123.45", "Xe máy", "Honda Vision", "Trắng", "Có", "ACTIVE", "22/05/2026", "fas fa-star"],
  ["59C1-987.65", "Xe máy", "Yamaha Sirius", "Đen", "Không", "ACTIVE", "18/05/2026", "far fa-star"],
  ["59A-456.78", "Ô tô", "Toyota Vios", "Bạc", "Không", "BLOCKED", "12/05/2026", "fas fa-lock"],
];

export function VehiclePage() {
  return (
    <CustomerPortalLayout>
      <CustomerPageHeader
        title="Xe của tôi"
        subtitle="Quản lý danh sách xe đã đăng ký trong tài khoản"
        action={<button type="button"><i className="fas fa-plus" /> Thêm xe mới</button>}
      />
      <div className="vm-stat-grid vm-stat-grid-four">
        <StatCard icon="fas fa-motorcycle" label="Tổng số xe" value="3" note="xe" />
        <StatCard icon="far fa-star" label="Xe mặc định" value="59C1-123.45" note={<StatusPill>Mặc định</StatusPill>} tone="green" />
        <StatCard icon="far fa-check-circle" label="Đang hoạt động" value="2" note="xe" tone="green" />
        <StatCard icon="fas fa-lock" label="Bị khóa / ngưng dùng" value="1" note="xe" tone="orange" />
      </div>

      <div className="vm-two-column-main">
        <section className="vm-customer-card vm-table-card">
          <h2>Danh sách xe đã đăng ký</h2>
          <div className="vm-table-filters">
            <label><i className="fas fa-search" /><input placeholder="Tìm biển số, hãng xe..." /></label>
            <select defaultValue="Tất cả"><option>Tất cả</option></select>
            <select defaultValue="Tất cả"><option>Tất cả</option></select>
          </div>
          <table className="vm-customer-table">
            <thead><tr><th>Biển số</th><th>Loại xe</th><th>Hãng xe</th><th>Màu xe</th><th>Mặc định</th><th>Trạng thái</th><th>Cập nhật</th><th>Thao tác</th></tr></thead>
            <tbody>
              {vehicles.map((row) => (
                <tr key={row[0]}>
                  <td>{row[0]}</td><td>{row[1]}</td><td>{row[2]}</td><td>{row[3]}</td>
                  <td><StatusPill tone={row[4] === "Có" ? "green" : "gray"}>{row[4]}</StatusPill></td>
                  <td><StatusPill tone={row[5] === "ACTIVE" ? "green" : "red"}>{row[5]}</StatusPill></td>
                  <td>{row[6]}</td>
                  <td className="vm-action-icons"><button type="button"><i className="far fa-eye" /></button><button type="button"><i className="fas fa-pencil-alt" /></button><button type="button"><i className={row[7]} /></button></td>
                </tr>
              ))}
            </tbody>
          </table>
          <PaginationLite />
        </section>

        <aside className="vm-customer-card vm-side-form">
          <h2>Thêm / cập nhật xe</h2>
          <Field label="Loại xe"><select defaultValue="Xe máy"><option>Xe máy</option></select></Field>
          <Field label="Biển số xe"><input defaultValue="59C1-987.65" /></Field>
          <Field label="Hãng xe"><input defaultValue="Honda Vision" /></Field>
          <Field label="Màu xe"><input defaultValue="Trắng" /></Field>
          <label className="vm-checkbox-lite"><input type="checkbox" /> Đặt làm xe mặc định</label>
          <div className="vm-current-status"><span>Trạng thái (hiện tại)</span><StatusPill>ACTIVE</StatusPill></div>
          <div className="vm-form-actions"><button className="vm-outline-btn" type="button">Hủy</button><button type="button">Lưu xe</button></div>
        </aside>
      </div>

      <section className="vm-customer-card vm-note-illustration">
        <div>
          <h2><i className="fas fa-info-circle" /> Lưu ý</h2>
          <ul>
            <li>Biển số xe không được trùng</li>
            <li>Xe mặc định phải ở trạng thái ACTIVE</li>
            <li>Xe bị BLOCKED không thể dùng để đăng ký vé mới</li>
          </ul>
        </div>
        <i className="fas fa-car-side" />
      </section>
    </CustomerPortalLayout>
  );
}
