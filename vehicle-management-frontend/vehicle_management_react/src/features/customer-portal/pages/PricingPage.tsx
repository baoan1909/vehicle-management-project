import { useState } from "react";
import { ClientPage } from "../../../shared/components/layout/ClientPage";
import { pricingPlans } from "../../../shared/data/mockData";

const tabs = [
  { key: "visitor", label: "Vãng lai", icon: "fas fa-user-clock" },
  { key: "car", label: "Xe hơi", icon: "fas fa-car" },
  { key: "motorcycle", label: "Xe máy", icon: "fas fa-motorcycle" },
  { key: "other", label: "Xe khác", icon: "fas fa-truck" },
] as const;

export function PricingPage() {
  const [activeTab, setActiveTab] = useState<(typeof tabs)[number]["key"]>("visitor");

  return (
    <ClientPage>
      <div className="container-fluid">
        <div className="vm-pricing-hero mt-4">
          <div>
            <h2>Dễ dàng tìm chỗ đậu xe, an toàn và hiệu quả</h2>
          </div>
          <svg className="vm-hero-car" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" aria-hidden="true">
            <path fill="currentColor" d="M499.99 176h-59.87l-16.64-41.6C406.38 91.63 365.57 64 319.5 64h-127c-46.06 0-86.88 27.63-103.99 70.4L71.87 176H12.01C4.2 176-1.53 183.34.37 190.91l6 24C7.7 220.25 12.5 224 18.01 224h20.07C24.65 235.73 16 252.78 16 272v48c0 16.12 6.16 30.67 16 41.93V416c0 17.67 14.33 32 32 32h32c17.67 0 32-14.33 32-32v-32h256v32c0 17.67 14.33 32 32 32h32c17.67 0 32-14.33 32-32v-54.07c9.84-11.25 16-25.8 16-41.93v-48c0-19.22-8.65-36.27-22.07-48H494c5.51 0 10.31-3.75 11.64-9.09l6-24c1.89-7.57-3.84-14.91-11.65-14.91zm-352.06-17.83c7.29-18.22 24.94-30.17 44.57-30.17h127c19.63 0 37.28 11.95 44.57 30.17L384 208H128l19.93-49.83zM96 319.8c-19.2 0-32-12.76-32-31.9S76.8 256 96 256s48 28.71 48 47.85-28.8 15.95-48 15.95zm320 0c-19.2 0-48 3.19-48-15.95S396.8 256 416 256s32 12.76 32 31.9-12.8 31.9-32 31.9z" />
          </svg>
        </div>
        <div className="pricing-tabs">
          <div className="vm-tab-buttons">
            {tabs.map((tab) => (
              <button className={`vm-tab-btn ${activeTab === tab.key ? "active" : ""}`} type="button" key={tab.key} onClick={() => setActiveTab(tab.key)}>
                <i className={tab.icon} /> {tab.label}
              </button>
            ))}
          </div>
        </div>
        <div className="vm-pricing-cards">
          {pricingPlans[activeTab].map((plan) => (
            <div className="vm-pricing-card" key={`${activeTab}-${plan.title}`}>
              <div className="card-header">
                <div className="card-title">{plan.title}</div>
                <div className="card-price">{plan.price}</div>
                <div className="card-period">{plan.period}</div>
              </div>
              <div className="card-body">
                <ul>
                  <li><i className="fas fa-check-circle" /> Thanh toán đơn giản</li>
                  <li><i className="fas fa-check-circle" /> Hỗ trợ 24/7</li>
                  <li><i className="fas fa-check-circle" /> Bắt đầu: 30/04/2025</li>
                  <li><i className="fas fa-check-circle" /> Vị trí ưu tiên</li>
                </ul>
              </div>
              <div className="card-footer">
                <button className="card-button" type="button">Đăng ký ngay</button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </ClientPage>
  );
}
