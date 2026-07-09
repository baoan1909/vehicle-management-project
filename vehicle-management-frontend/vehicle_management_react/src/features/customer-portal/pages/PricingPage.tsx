import { ClientPage } from "@/shared/components/layout/ClientPage";
import { PublicContactStrip, PublicFooter, PublicHero, VehicleTabs } from "./PortalShared";

const visitorPlans = [
  {
    icon: "far fa-sun",
    title: "Khung giờ 6:00 - 17:59",
    price: "4.000 VNĐ",
    period: "/ lượt",
    items: ["Áp dụng ban ngày", "Thanh toán theo lượt", "Không giới hạn thời gian trong khung"],
  },
  {
    icon: "fas fa-moon",
    title: "Khung giờ 18:00 - 23:59",
    price: "5.000 VNĐ",
    period: "/ lượt",
    items: ["Áp dụng buổi tối", "Phù hợp gửi xe ngắn hạn", "Có nhân viên hỗ trợ"],
  },
  {
    icon: "fas fa-moon",
    title: "Qua đêm 00:00 - 5:59",
    price: "15.000 VNĐ",
    period: "/ lượt",
    items: ["Áp dụng qua đêm", "Giữ xe an toàn", "Theo dõi camera"],
  },
];

const customerPlans = [
  {
    icon: "far fa-calendar-alt",
    title: "Vé tháng",
    price: "120.000 VNĐ",
    period: "/ tháng",
    items: ["Hiệu lực 30 ngày", "Dành cho xe đã đăng ký", "Gia hạn dễ dàng"],
  },
  {
    icon: "far fa-calendar-alt",
    title: "Vé quý",
    price: "330.000 VNĐ",
    period: "/ quý",
    items: ["Hiệu lực 3 tháng", "Tiết kiệm hơn vé tháng", "Ưu tiên xử lý gia hạn"],
  },
  {
    icon: "far fa-calendar-check",
    title: "Vé năm",
    price: "1.200.000 VNĐ",
    period: "/ năm",
    items: ["Hiệu lực 12 tháng", "Chi phí ổn định cả năm", "Phù hợp khách gửi thường xuyên"],
  },
];

export function PricingPage() {
  return (
    <ClientPage>
      <div className="vm-public-page">
        <PublicHero title="Bảng giá gửi xe" subtitle="Minh bạch - Tiện lợi - An toàn" />

        <section className="vm-pricing-section">
          <h2><i className="fas fa-user" /> Khách vãng lai</h2>
          <VehicleTabs />
          <div className="vm-price-grid">
            {visitorPlans.map((plan) => (
              <article className="vm-price-card" key={plan.title}>
                <span className="vm-price-icon"><i className={plan.icon} /></span>
                <h3>{plan.title}</h3>
                <strong className="vm-price-blue">{plan.price}</strong>
                <p>{plan.period}</p>
                <ul>
                  {plan.items.map((item) => <li key={item}><i className="fas fa-check-circle" /> {item}</li>)}
                </ul>
              </article>
            ))}
          </div>
        </section>

        <section className="vm-pricing-section">
          <h2><i className="fas fa-users" /> Khách đăng ký</h2>
          <VehicleTabs />
          <div className="vm-price-grid">
            {customerPlans.map((plan) => (
              <article className="vm-price-card vm-price-card-subscription" key={plan.title}>
                <span className="vm-price-icon vm-price-icon-green"><i className={plan.icon} /></span>
                <h3>{plan.title}</h3>
                <strong className="vm-price-green">{plan.price}</strong>
                <p>{plan.period}</p>
                <ul>
                  {plan.items.map((item) => <li key={item}><i className="fas fa-check-circle" /> {item}</li>)}
                </ul>
                <button type="button">Đăng ký</button>
              </article>
            ))}
          </div>
        </section>

        <PublicContactStrip />
        <PublicFooter />
      </div>
    </ClientPage>
  );
}
