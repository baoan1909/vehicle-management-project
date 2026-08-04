import { useState } from "react";

import { ClientPage } from "@/shared/components/layout/ClientPage";
import { PublicContactStrip, PublicHero } from "./PortalShared";

const guideSteps = [
  ["Vào cổng", "Đưa xe vào làn gửi xe và chờ hệ thống ghi nhận.", "fas fa-sign-in-alt"],
  ["Nhận diện xe", "Camera nhận diện biển số hoặc nhân viên quẹt thẻ.", "fas fa-camera"],
  ["Theo dõi thời gian", "Thời gian gửi xe được tính tự động theo khung giờ.", "far fa-clock"],
  ["Thanh toán", "Thanh toán phí khi ra bãi và hoàn tất phiên gửi xe.", "fas fa-receipt"],
];

const frequentlyAskedQuestions = [
  {
    answer: "Có. Bạn có thể thêm nhiều phương tiện vào hồ sơ, nhưng từng xe cần được phê duyệt trước khi đăng ký và sử dụng vé gửi xe.",
    question: "Tôi có thể đăng ký nhiều xe không?",
  },
  {
    answer: "Hãy báo ngay cho nhân viên bãi xe và cung cấp thông tin xác minh. Thẻ cũ sẽ bị khóa; khách đăng ký được cấp thẻ thay thế sau khi hoàn tất phí và quy trình báo mất.",
    question: "Mất thẻ xe thì làm gì?",
  },
  {
    answer: "Sau khi đăng nhập, chọn mục Lịch sử gửi xe trong cổng khách hàng để xem thời gian vào, thời gian ra, chi phí và hình ảnh của từng phiên.",
    question: "Xem lịch sử gửi xe ở đâu?",
  },
];

type GuideTab = "VISITOR" | "CUSTOMER" | "FAQ";

export function GuidePage() {
  const [activeTab, setActiveTab] = useState<GuideTab>("VISITOR");
  const [openQuestion, setOpenQuestion] = useState<number | null>(null);

  return (
    <ClientPage>
      <div className="vm-public-page">
        <PublicHero title="Hướng dẫn sử dụng CoParking" subtitle="Gửi xe nhanh chóng - đăng ký vé dễ dàng - quản lý mọi lúc" />

        <div className="vm-guide-tabs" role="tablist" aria-label="Nhóm hướng dẫn">
          <button className={activeTab === "VISITOR" ? "active" : ""} type="button" role="tab" aria-selected={activeTab === "VISITOR"} onClick={() => setActiveTab("VISITOR")}><i className="fas fa-user" /> Khách vãng lai</button>
          <button className={activeTab === "CUSTOMER" ? "active" : ""} type="button" role="tab" aria-selected={activeTab === "CUSTOMER"} onClick={() => setActiveTab("CUSTOMER")}><i className="fas fa-users" /> Khách đăng ký</button>
          <button className={activeTab === "FAQ" ? "active" : ""} type="button" role="tab" aria-selected={activeTab === "FAQ"} onClick={() => setActiveTab("FAQ")}><i className="far fa-question-circle" /> Câu hỏi thường gặp</button>
        </div>

        {activeTab === "VISITOR" ? (
          <section className="vm-guide-section" role="tabpanel">
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
        ) : null}

        {activeTab === "CUSTOMER" ? (
          <section className="vm-guide-two-columns" role="tabpanel">
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
        ) : null}

        {activeTab === "FAQ" ? (
          <section className="vm-faq-section" role="tabpanel">
            <h2><i className="far fa-question-circle" /> Câu hỏi thường gặp</h2>
            {frequentlyAskedQuestions.map((item, index) => {
              const isOpen = openQuestion === index;
              return (
                <article className={`vm-faq-item${isOpen ? " open" : ""}`} key={item.question}>
                  <button className="vm-faq-row" type="button" aria-expanded={isOpen} onClick={() => setOpenQuestion(isOpen ? null : index)}>
                    <span>{item.question}</span>
                    <i className="fas fa-chevron-down" />
                  </button>
                  {isOpen ? <p className="vm-faq-answer">{item.answer}</p> : null}
                </article>
              );
            })}
          </section>
        ) : null}

        <PublicContactStrip />
      </div>
    </ClientPage>
  );
}
