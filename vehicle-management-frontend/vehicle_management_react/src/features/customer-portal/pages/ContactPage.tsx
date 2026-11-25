import { ClientPage } from "@/shared/components/layout/ClientPage";
import { SelectMenu } from "@/components/ui";
import { publicContactItems, PublicFooter, PublicHero } from "./PortalShared";

const quickSupport = [
  ["fas fa-id-card", "Hỗ trợ mất thẻ", "Hướng dẫn khai báo và xử lý khi mất thẻ gửi xe."],
  ["far fa-calendar-alt", "Tư vấn vé tháng", "Tư vấn gói vé phù hợp và cách đăng ký nhanh chóng."],
  ["far fa-comment-dots", "Góp ý dịch vụ", "Chia sẻ ý kiến để CoParking phục vụ bạn tốt hơn."],
];

export function ContactPage() {
  return (
    <ClientPage>
      <div className="vm-public-page">
        <PublicHero title="Liên hệ với CoParking" subtitle="Chúng tôi luôn sẵn sàng hỗ trợ bạn trong quá trình gửi xe và đăng ký dịch vụ." />

        <section className="vm-contact-layout">
          <article className="vm-public-card">
            <h2>Thông tin liên hệ</h2>
            <div className="vm-contact-list">
              {publicContactItems.map((item) => (
                <div className="vm-public-contact-item" key={item.title}>
                  <span className="vm-public-contact-icon"><i className={item.icon} /></span>
                  <span>
                    <strong>{item.title}</strong>
                    <b>{item.value}</b>
                    {item.note && <small>{item.note}</small>}
                  </span>
                </div>
              ))}
            </div>
            <div className="vm-contact-map">
              <iframe
                title="Bản đồ trụ sở CoParking"
                src={`https://www.google.com/maps?q=${encodeURIComponent(publicContactItems.find((item) => item.title === "Địa chỉ")?.value ?? "")}&output=embed`}
                loading="lazy"
                referrerPolicy="no-referrer-when-downgrade"
              />
            </div>
          </article>

          <article className="vm-public-card">
            <h2>Gửi yêu cầu liên hệ</h2>
            <p id="contact-form-unavailable">Biểu mẫu liên hệ hiện chưa nhận yêu cầu. Bạn có thể gọi hotline hoặc gửi email theo thông tin bên cạnh. Nếu đang ở bãi và gặp lỗi vào/ra, hãy báo nhân viên tại bãi.</p>
            <fieldset className="vm-contact-form" disabled aria-describedby="contact-form-unavailable" style={{ border: 0, padding: 0, margin: 0, minWidth: 0 }}>
              <label>Họ và tên<input placeholder="Nguyễn Văn A" /></label>
              <label>Email<input placeholder="email@example.com" /></label>
              <label>Số điện thoại<input placeholder="0901 234 567" /></label>
              <div><span>Chủ đề</span><SelectMenu ariaLabel="Chọn chủ đề liên hệ" options={[{ label: "Tư vấn đăng ký vé", value: "subscription" }]} searchable={false} value="subscription" onChange={() => undefined} /></div>
              <label>Nội dung<textarea placeholder="Nhập nội dung cần hỗ trợ..." /></label>
              <button type="button" disabled>Chưa nhận yêu cầu qua biểu mẫu</button>
            </fieldset>
          </article>
        </section>

        <section className="vm-quick-support">
          <h2>Kênh hỗ trợ nhanh</h2>
          <div>
            {quickSupport.map(([icon, title, text]) => (
              <article className="vm-public-card" key={title}>
                <i className={icon} />
                <h3>{title}</h3>
                <p>{text}</p>
                <a href="mailto:support@coparking.vn">Liên hệ qua email <i className="fas fa-arrow-right" /></a>
              </article>
            ))}
          </div>
        </section>
      </div>
      <PublicFooter />
    </ClientPage>
  );
}
