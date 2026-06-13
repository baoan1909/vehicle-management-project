import { CustomerPageHeader, CustomerPortalLayout, Field, StatusPill } from "./PortalShared";

export function ProfilePage() {
  return (
    <CustomerPortalLayout>
      <CustomerPageHeader
        title="Hồ sơ cá nhân"
        subtitle="Quản lý thông tin tài khoản và hồ sơ khách hàng"
        action={<button className="vm-outline-action" type="button"><i className="fas fa-lock" /> Đổi mật khẩu</button>}
      />
      <div className="vm-profile-top">
        <article className="vm-customer-card vm-profile-summary">
          <div className="vm-large-avatar">KH</div>
          <div>
            <h2>Nguyễn Văn A</h2>
            <div className="vm-pill-row"><StatusPill tone="blue">Khách hàng</StatusPill><StatusPill>Đang hoạt động</StatusPill></div>
            <dl className="vm-info-list">
              <dt><i className="far fa-id-card" /> Mã khách hàng:</dt><dd>KH-000123</dd>
              <dt><i className="far fa-calendar-alt" /> Ngày tham gia:</dt><dd>01/05/2024</dd>
              <dt><i className="far fa-shield-alt" /> Email xác thực:</dt><dd className="vm-green-text">Đã xác thực</dd>
            </dl>
          </div>
        </article>
        <article className="vm-customer-card">
          <h2>Thông tin tài khoản</h2>
          <dl className="vm-info-list">
            <dt><i className="far fa-envelope" /> Email đăng nhập:</dt><dd>nguyenvana@gmail.com</dd>
            <dt><i className="fas fa-phone-alt" /> Số điện thoại:</dt><dd>0901 234 567</dd>
            <dt><i className="far fa-clock" /> Lần đăng nhập gần nhất:</dt><dd>22/05/2026 09:20</dd>
            <dt><i className="far fa-shield-alt" /> Trạng thái:</dt><dd className="vm-green-text">ACTIVE</dd>
          </dl>
          <button className="vm-outline-action" type="button"><i className="fas fa-lock" /> Cập nhật bảo mật</button>
        </article>
      </div>
      <section className="vm-customer-card">
        <h2>Cập nhật hồ sơ</h2>
        <div className="vm-form-grid">
          <Field label="Họ và tên"><input defaultValue="Nguyễn Văn A" /></Field>
          <Field label="Email"><input defaultValue="nguyenvana@gmail.com" /></Field>
          <Field label="Số điện thoại"><input defaultValue="0901 234 567" /></Field>
          <Field label="CCCD/CMND"><input defaultValue="080023242323" /></Field>
          <Field label="Ngày sinh"><input defaultValue="12/09/1998" /></Field>
          <Field label="Giới tính"><select defaultValue="Nam"><option>Nam</option></select></Field>
          <Field label="Địa chỉ"><input defaultValue="123 Trần Hưng Đạo, Quận 1, TP. Hồ Chí Minh" /></Field>
          <Field label="Ghi chú"><textarea placeholder="Thông tin bổ sung..." /></Field>
        </div>
        <div className="vm-form-actions"><button className="vm-outline-btn" type="button">Hủy thay đổi</button><button type="button">Lưu thay đổi</button></div>
      </section>
      <div className="vm-mini-summary">
        <article className="vm-customer-card"><span><i className="fas fa-motorcycle" /></span><b>Xe đang sở hữu</b><strong>2</strong><button className="vm-outline-btn" type="button">Xem xe</button></article>
        <article className="vm-customer-card"><span><i className="far fa-calendar-check" /></span><b>Vé đang dùng</b><strong>Xe máy - Tháng</strong><StatusPill>Còn hiệu lực</StatusPill></article>
        <article className="vm-customer-card"><span><i className="fas fa-headset" /></span><b>Yêu cầu hỗ trợ</b><strong>1</strong><StatusPill tone="orange">Đang xử lý</StatusPill></article>
      </div>
    </CustomerPortalLayout>
  );
}
