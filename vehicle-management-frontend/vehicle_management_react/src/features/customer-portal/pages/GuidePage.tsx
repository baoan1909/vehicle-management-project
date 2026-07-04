import { ClientPage } from "@/shared/components/layout/ClientPage";
import { PublicContactStrip, PublicFooter, PublicHero } from "./PortalShared";

const guideSteps = [
  ["Vào cổng", "Đưa xe vào làn gửi xe và chờ hệ thống ghi nhận.", "fas fa-sign-in-alt"],
  ["Nhận diện xe", "Camera nhận diện biển số hoặc nhân viên quẹt thẻ.", "fas fa-camera"],
  ["Theo dõi thời gian", "Thời gian gửi xe được tính tự động theo khung giờ.", "far fa-clock"],
  ["Thanh toán", "Thanh toán phí khi ra bãi và hoàn tất phiên gửi xe.", "fas fa-receipt"],
];

export function GuidePage() {
  return (
    <ClientPage>
      <div className="vm-public-page">
        <PublicHero title="Hướng dẫn sử dụng CoParking" subtitle="Gửi xe nhanh chóng - đăng ký vé dễ dàng - quản lý mọi lúc" />

        <div className="vm-guide-tabs">
          <button className="active" type="button"><i className="fas fa-user" /> Khách vãng lai</button>
          <button type="button"><i className="fas fa-users" /> Khách đăng ký</button>
          <button type="button"><i className="far fa-question-circle" /> Câu hỏi thường gặp</button>
        </div>

        <section className="vm-guide-section">
          <h2><i className="far fa-clipboard" /> Quy trình gửi xe vãng lai</h2>
          <div className="vm-guide-step-grid">
            {guideSteps.map(([title, text, icon], index) => (
              <article className="vm-guide-step-card" key={title}>
                <span>{index + 1}</span>
                <i className={icon} />
                <h3>{title}</h3>
                <p>{text}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="vm-guide-two-columns">
          <article className="vm-public-card">
            <h2><i className="fas fa-users" /> Khách đăng ký cần làm gì?</h2>
            <ul className="vm-check-list">
              {["Đăng ký tài khoản", "Cập nhật hồ sơ cá nhân", "Thêm xe của tôi", "Chọn vé tháng / quý / năm", "Theo dõi lịch sử gửi xe"].map((item) => (
                <li key={item}><i className="fas fa-check-circle" /> {item}</li>
              ))}
            </ul>
            <a className="vm-public-primary-btn" href="/register">Đăng ký ngay</a>
          </article>
          <article className="vm-public-card">
            <h2><i className="fas fa-shield-alt" /> Quy định & lưu ý</h2>
            <ul className="vm-check-list">
              {["Giữ thẻ xe trong suốt quá trình gửi", "Mất thẻ cần cung cấp thông tin xác minh", "Vé đăng ký chỉ áp dụng cho xe đã được duyệt", "Qua đêm áp dụng mức phí riêng"].map((item) => (
                <li key={item}><i className="fas fa-check-circle" /> {item}</li>
              ))}
            </ul>
          </article>
        </section>

        <section className="vm-faq-section">
          <h2><i className="far fa-question-circle" /> Câu hỏi thường gặp</h2>
          {["Tôi có thể đăng ký nhiều xe không?", "Mất thẻ xe thì làm gì?", "Xem lịch sử gửi xe ở đâu?"].map((item) => (
            <div className="vm-faq-row" key={item}>{item}<i className="fas fa-chevron-down" /></div>
          ))}
        </section>

        <PublicContactStrip />
        <PublicFooter />
      </div>
    </ClientPage>
  );
}
