import { useEffect, useRef, useState, type ReactNode } from "react";
import { Link } from "react-router-dom";

import { cn } from "@/lib/cn";
import { ClientPage } from "@/shared/components/layout/ClientPage";

import { PublicFooter } from "./PortalShared";

const proofItems = [
  { icon: "fas fa-bolt", title: "24/7", text: "Giám sát liên tục" },
  { icon: "fas fa-expand", title: "OCR", text: "Nhận diện chính xác" },
  { icon: "fas fa-bolt", title: "Realtime", text: "Đồng bộ tức thời" },
  { icon: "far fa-credit-card", title: "Minh bạch", text: "Đối soát rõ ràng" },
];

const productBenefits = [
  { icon: "far fa-credit-card", title: "Giảm thiếu thất thoát doanh thu" },
  { icon: "fas fa-project-diagram", title: "Tối ưu nhân sự và thời gian vận hành" },
  { icon: "fas fa-cubes", title: "Dữ liệu tập trung, báo cáo theo thời gian thực" },
];

const technologyCards = [
  {
    icon: "far fa-id-badge",
    title: "Kiểm soát ra vào",
    text: "OCR nhận diện chính xác, kết hợp barrier và camera AI để kiểm soát mỗi lượt xe.",
    image: "/assets/customer/about/about-feature-access-control.webp",
  },
  {
    icon: "far fa-calendar-check",
    title: "Quản lý vé tháng",
    text: "Quản lý toàn bộ vòng đời vé tháng, cảnh báo sắp hết hạn và gia hạn nhanh chóng.",
    featured: true,
  },
  {
    icon: "fas fa-chart-line",
    title: "Doanh thu & đối soát",
    text: "Báo cáo theo thời gian thực, đối soát tự động, xuất dữ liệu linh hoạt.",
  },
];

const workflowSteps = [
  { no: "01", icon: "fas fa-fingerprint", title: "Nhận diện", text: "Camera OCR nhận diện biển số tự động" },
  { no: "02", icon: "fas fa-user-check", title: "Xác thực", text: "Kiểm tra quyền ra/vào, vé tháng hoặc lượt" },
  { no: "03", icon: "far fa-credit-card", title: "Ghi nhận", text: "Ghi nhận thời gian vào/ra lưu trữ đám mây" },
  { no: "04", icon: "fas fa-chart-pie", title: "Đối soát", text: "Tổng hợp doanh thu, báo cáo minh bạch" },
];

const deploymentModels = [
  { image: "/assets/customer/about/deployment-apartment.webp", title: "Chung cư", value: "200+ dự án", text: "Quản lý cư dân & khách vãng lai" },
  { image: "/assets/customer/about/deployment-office.webp", title: "Văn phòng", value: "150+ tòa nhà", text: "Kiểm soát nhân viên & khách" },
  { image: "/assets/customer/about/deployment-school.webp", title: "Trường học", value: "80+ trường", text: "An toàn cho học sinh & phụ huynh" },
  { image: "/assets/customer/about/deployment-hospital.webp", title: "Bệnh viện", value: "60+ bệnh viện", text: "Ưu tiên xe cấp cứu & bệnh nhân" },
  { image: "/assets/customer/about/deployment-shopping-mall.webp", title: "TTTM", value: "120+ trung tâm", text: "Tối ưu trải nghiệm khách hàng" },
  { image: "/assets/customer/about/deployment-rental-parking.webp", title: "Bãi thuê", value: "300+ bãi xe", text: "Quản lý nhiều bãi xe tập trung" },
];

const impactItems = [
  { icon: "fas fa-project-diagram", value: "-40%", text: "Giảm thao tác thủ công" },
  { icon: "far fa-clock", value: "-30%", text: "Giảm thời gian xử lý" },
  { icon: "fas fa-database", value: "100%", text: "Dữ liệu tập trung" },
  { icon: "fas fa-sync-alt", value: "24/7", text: "Vận hành ổn định" },
];

const faqItems = [
  {
    question: "CoParking có phù hợp với bãi xe nhỏ không?",
    answer: "Có. CoParking linh hoạt cho mọi quy mô, từ bãi xe 20 chỗ đến hệ thống nhiều bãi xe vận hành nghìn lượt xe mỗi ngày.",
  },
  {
    question: "Dữ liệu có được lưu trữ và bảo mật như thế nào?",
    answer: "Dữ liệu được phân quyền theo vai trò, lưu trữ tập trung và có lịch sử đối soát rõ ràng cho từng thao tác.",
  },
  {
    question: "Thời gian triển khai và đào tạo là bao lâu?",
    answer: "Thông thường từ 1–3 ngày tùy quy mô. Đội ngũ CoParking đồng hành cấu hình, kiểm thử và đào tạo vận hành.",
  },
];

type HeroShineState = {
  direction: "enter" | "leave";
  id: number;
  traveling: boolean;
};

function Reveal({ children, className, delay = 0 }: { children: ReactNode; className?: string; delay?: number }) {
  const ref = useRef<HTMLDivElement | null>(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const element = ref.current;
    if (!element) return undefined;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setVisible(true);
          observer.unobserve(entry.target);
        }
      },
      { rootMargin: "0px 0px -8% 0px", threshold: 0.12 },
    );
    observer.observe(element);
    return () => observer.disconnect();
  }, []);

  return (
    <div
      ref={ref}
      className={cn(
        "tw-transition-all tw-duration-700 tw-ease-out motion-reduce:tw-transform-none motion-reduce:tw-opacity-100 motion-reduce:tw-transition-none",
        visible ? "tw-translate-y-0 tw-opacity-100" : "tw-translate-y-7 tw-opacity-0",
        className,
      )}
      style={{ transitionDelay: `${delay}ms` }}
    >
      {children}
    </div>
  );
}

function Eyebrow({ children, light = false }: { children: ReactNode; light?: boolean }) {
  return (
    <span className={cn("tw-block tw-text-[0.75rem] tw-font-black tw-uppercase tw-tracking-[0.16em]", light ? "tw-text-[#65a6ff]" : "tw-text-[#146bff]")}>
      {children}
    </span>
  );
}

function Sparkline({ className }: { className?: string }) {
  return (
    <svg className={cn("tw-h-10 tw-w-24", className)} viewBox="0 0 100 40" fill="none" aria-hidden="true">
      <path d="M2 35L17 29L26 31L35 18L45 23L56 13L67 18L79 7L88 12L98 2" stroke="#1b77ff" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M2 35L17 29L26 31L35 18L45 23L56 13L67 18L79 7L88 12L98 2V40H2Z" fill="url(#spark-fill)" opacity="0.26" />
      <defs>
        <linearGradient id="spark-fill" x1="50" y1="2" x2="50" y2="40" gradientUnits="userSpaceOnUse">
          <stop stopColor="#287dff" />
          <stop offset="1" stopColor="#287dff" stopOpacity="0" />
        </linearGradient>
      </defs>
    </svg>
  );
}

function ProductDashboard({ active, shine }: { active: boolean; shine: HeroShineState | null }) {
  return (
    <div
      className={cn(
        "tw-relative tw-overflow-hidden tw-rounded-[26px] tw-border-[12px] tw-border-solid tw-border-white tw-bg-[#071d3d] tw-p-3 tw-shadow-[0_26px_60px_rgba(4,25,58,0.24),0_0_0_1px_rgba(18,55,102,.08)] tw-transition-all tw-duration-300 tw-ease-out tw-will-change-transform",
        active ? "tw-shadow-[0_34px_72px_rgba(4,25,58,0.34),0_0_0_1px_rgba(18,55,102,.08)]" : "tw-shadow-[0_26px_60px_rgba(4,25,58,0.24),0_0_0_1px_rgba(18,55,102,.08)]",
      )}
      data-testid="product-dashboard"
      style={{ transform: active ? "translate3d(0,-9px,0)" : "translate3d(0,0,0)" }}
    >
      <span
        aria-hidden="true"
        data-testid="product-dashboard-shine"
        key={shine?.id ?? "product-shine-idle"}
        className="tw-pointer-events-none tw-absolute tw-z-20 tw-mix-blend-screen"
        style={{
          background: "linear-gradient(90deg,transparent 0%,rgba(255,255,255,.08) 28%,rgba(255,255,255,.36) 50%,rgba(255,255,255,.08) 72%,transparent 100%)",
          top: "-75%",
          left:
            shine?.direction === "leave"
              ? shine.traveling ? "-18%" : "118%"
              : shine?.traveling ? "118%" : "-18%",
          width: "8%",
          height: "250%",
          opacity: shine?.traveling ? 0.72 : 0,
          transform: "rotate(24deg)",
          transformOrigin: "center",
          transition: shine?.traveling ? "left 1.3s cubic-bezier(.4,0,.2,1), opacity 150ms ease" : "none",
        }}
      />
      <div className="tw-flex tw-items-center tw-justify-between tw-rounded-t-[12px] tw-bg-[#0b2c5c] tw-px-4 tw-py-3 tw-text-white">
        <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.84rem] tw-font-black"><i className="fas fa-parking tw-text-[#4d95ff]" /> CoParking</span>
        <span className="tw-flex tw-gap-4 tw-text-[0.72rem] tw-text-brand-200"><i className="fas fa-search" /><i className="far fa-bell" /><i className="far fa-user-circle" /></span>
      </div>
      <div className="tw-grid tw-grid-cols-[116px_minmax(0,1fr)] tw-bg-[#f5f8fd] max-[640px]:tw-grid-cols-[74px_minmax(0,1fr)]">
        <aside className="tw-bg-[#082551] tw-px-3 tw-py-4 tw-text-[0.68rem] tw-font-bold tw-text-[#a7c8f5] max-[640px]:tw-px-2">
          {["Tổng quan", "Lượt xe", "Khách hàng", "Vé tháng", "Doanh thu", "Đối soát"].map((label, index) => (
            <span className={cn("tw-mb-2 tw-flex tw-items-center tw-gap-2 tw-rounded-[6px] tw-px-2 tw-py-2", index === 0 && "tw-bg-[#176fff] tw-text-white")} key={label}>
              <i className={cn(index === 0 ? "fas fa-th-large" : "far fa-circle", "tw-text-[0.55rem]")} />
              <span className="max-[640px]:tw-hidden">{label}</span>
            </span>
          ))}
        </aside>
        <div className="tw-min-w-0 tw-p-4 max-[640px]:tw-p-3">
          <div className="tw-flex tw-items-center tw-justify-between">
            <strong className="tw-text-[0.88rem] tw-font-black tw-text-[#0b1f3a]">Tổng quan</strong>
            <span className="tw-rounded-full tw-bg-emerald-50 tw-px-2 tw-py-1 tw-text-[0.62rem] tw-font-black tw-text-emerald-600">Đang hoạt động</span>
          </div>
          <div className="tw-mt-3 tw-grid tw-grid-cols-3 tw-gap-2">
            {[['1.284', 'Lượt xe'], ['126', 'Chỗ trống'], ['342', 'Vé tháng']].map(([value, label]) => (
              <span className="tw-rounded-[8px] tw-bg-white tw-p-2.5 tw-shadow-[0_5px_13px_rgba(38,77,126,0.08)]" key={label}>
                <b className="tw-block tw-text-[0.86rem] tw-font-black tw-text-[#102b50]">{value}</b>
                <small className="tw-text-[0.6rem] tw-font-bold tw-text-[#7890ae]">{label}</small>
              </span>
            ))}
          </div>
          <div className="tw-mt-3 tw-grid tw-grid-cols-[1.15fr_0.85fr] tw-gap-3 max-[560px]:tw-grid-cols-1">
            <div className="tw-rounded-[9px] tw-bg-white tw-p-3 tw-shadow-[0_5px_13px_rgba(38,77,126,0.08)]">
              <span className="tw-text-[0.66rem] tw-font-black tw-text-[#6d85a4]">Lượt xe theo giờ</span>
              <div className="tw-mt-3 tw-flex tw-h-[76px] tw-items-end tw-gap-[6px]">
                {[30, 44, 36, 58, 49, 76, 62, 85, 69, 92, 72, 98].map((height, index) => (
                  <span className="tw-flex-1 tw-rounded-t-[3px] tw-bg-gradient-to-t tw-from-[#176fff] tw-to-[#70adff]" style={{ height: `${height}%` }} key={`${height}-${index}`} />
                ))}
              </div>
            </div>
            <div className="tw-rounded-[9px] tw-bg-white tw-p-3 tw-shadow-[0_5px_13px_rgba(38,77,126,0.08)]">
              <span className="tw-text-[0.66rem] tw-font-black tw-text-[#6d85a4]">Doanh thu hôm nay</span>
              <div className="tw-mt-3 tw-flex tw-items-center tw-gap-3">
                <span className="tw-grid tw-h-16 tw-w-16 tw-flex-none tw-place-items-center tw-rounded-full tw-bg-[conic-gradient(#176fff_0_68%,#54c5a5_68%_86%,#f2ad42_86%)] before:tw-h-8 before:tw-w-8 before:tw-rounded-full before:tw-bg-white before:tw-content-['']" />
                <span><b className="tw-block tw-text-[0.8rem] tw-font-black tw-text-[#102b50]">18.560.000 đ</b><small className="tw-text-[0.6rem] tw-font-bold tw-text-emerald-600">+12% hôm nay</small></span>
              </div>
            </div>
          </div>
          <div className="tw-mt-3 tw-overflow-hidden tw-rounded-[8px] tw-bg-white tw-shadow-[0_5px_13px_rgba(38,77,126,0.08)]">
            {[['59A-482.16', 'Vào', '10:30:45'], ['51H-123.45', 'Ra', '10:28:12'], ['30G-468.76', 'Vào', '10:21:06']].map(([plate, status, time]) => (
              <span className="tw-grid tw-grid-cols-[1fr_60px_70px] tw-border-0 tw-border-b tw-border-solid tw-border-slate-100 tw-px-3 tw-py-1.5 tw-text-[0.61rem] tw-font-bold last:tw-border-b-0" key={plate}>
                <b className="tw-text-[#17385f]">{plate}</b><i className="tw-not-italic tw-text-emerald-600">{status}</i><small className="tw-text-right tw-text-[#7890ae]">{time}</small>
              </span>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

function LiveOperationsPanel({ shine, active }: { shine: HeroShineState | null; active: boolean }) {
  return (
    <div
      className={cn(
        "tw-relative tw-overflow-hidden tw-rounded-[20px] tw-border tw-border-solid tw-border-[#2c507b] tw-bg-[#061a38]/95 tw-p-5 tw-text-white tw-shadow-[0_24px_58px_rgba(0,10,30,0.36)] tw-backdrop-blur-xl tw-transition-all tw-duration-300 tw-ease-out tw-will-change-transform",
        active ? "tw-shadow-[0_30px_68px_rgba(0,10,30,0.48)]" : "tw-shadow-[0_24px_58px_rgba(0,10,30,0.36)]",
      )}
      style={{ transform: active ? "translate3d(0,-9px,0)" : "translate3d(0,0,0)" }}
    >
      <span
        key={shine?.id ?? "live-shine-idle"}
        aria-hidden="true"
        data-testid="live-operations-shine"
        className="tw-pointer-events-none tw-absolute tw-z-20 tw-mix-blend-screen"
        style={{
          background: "linear-gradient(90deg,transparent 0%,rgba(255,255,255,.08) 28%,rgba(255,255,255,.34) 50%,rgba(255,255,255,.08) 72%,transparent 100%)",
          top: "-75%",
          left:
            shine?.direction === "leave"
              ? shine.traveling ? "-18%" : "118%"
              : shine?.traveling ? "118%" : "-18%",
          width: "8%",
          height: "250%",
          opacity: shine?.traveling ? 0.72 : 0,
          transform: "rotate(24deg)",
          transformOrigin: "center",
          transition: shine?.traveling ? "left 1.3s cubic-bezier(.4,0,.2,1), opacity 150ms ease" : "none",
        }}
      />
      <div className="tw-relative tw-z-[1] tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-3">
        <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.8rem] tw-font-black tw-uppercase tw-tracking-[0.08em] tw-text-[#b6cae7]"><i className="fas fa-cubes tw-text-[#5ca2ff]" /> Trung tâm vận hành trực tiếp</span>
        <b className="tw-flex tw-items-center tw-gap-2 tw-rounded-full tw-bg-emerald-400/[0.12] tw-px-3 tw-py-1 tw-text-[0.72rem] tw-font-black tw-text-emerald-300"><span className="tw-h-2 tw-w-2 tw-rounded-full tw-bg-emerald-400 tw-shadow-[0_0_10px_#34d399]" /> Đang hoạt động</b>
      </div>
      <div className="tw-relative tw-z-[1] tw-mt-4 tw-grid tw-grid-cols-[1.35fr_0.95fr] tw-gap-4 max-[760px]:tw-grid-cols-1">
        <div className="tw-rounded-[13px] tw-border tw-border-solid tw-border-white/[0.08] tw-bg-[#09244a]/78 tw-p-4">
          <span className="tw-text-[0.8rem] tw-font-bold tw-text-[#b9cae0]">Toàn cảnh bãi xe hôm nay</span>
          <small className="tw-mt-0.5 tw-block tw-text-[0.62rem] tw-font-semibold tw-text-[#7896bb]">Cập nhật lúc 10:30:45</small>
          <div className="tw-mt-3 tw-grid tw-grid-cols-3 tw-gap-2">
            {[['fas fa-car', '1.284', 'Lượt xe'], ['fas fa-parking', '126', 'Chỗ trống'], ['far fa-credit-card', '342', 'Vé tháng']].map(([icon, value, label]) => (
              <span className="tw-rounded-[9px] tw-bg-white/[0.045] tw-p-3" key={label}>
                <i className={cn(icon, "tw-mr-2 tw-text-[#55a0ff]")} /><strong className="tw-text-[1.1rem] tw-font-black">{value}</strong>
                <small className="tw-mt-1 tw-block tw-text-[0.64rem] tw-font-bold tw-text-[#8aa8cc]">{label}</small>
              </span>
            ))}
          </div>
          <div className="tw-mt-3 tw-grid tw-grid-cols-[1fr_0.9fr] tw-gap-2 max-[540px]:tw-grid-cols-1">
            <div className="tw-rounded-[9px] tw-bg-[#04152f]/75 tw-p-3">
              <span className="tw-text-[0.65rem] tw-font-black tw-uppercase tw-text-[#89a7cc]">Hoạt động gần đây</span>
              <p className="tw-m-0 tw-mt-2 tw-flex tw-justify-between tw-text-[0.69rem] tw-font-bold"><span><i className="fas fa-car tw-mr-2 tw-text-[#5ba3ff]" />Xe vào 59A-482.16</span><small className="tw-text-[#7e9bc0]">10:30:45</small></p>
              <p className="tw-m-0 tw-mt-2 tw-flex tw-justify-between tw-text-[0.69rem] tw-font-bold"><span><i className="fas fa-car tw-mr-2 tw-text-[#5ba3ff]" />Xe ra 51H-123.45</span><small className="tw-text-[#7e9bc0]">10:28:12</small></p>
            </div>
            <div className="tw-rounded-[9px] tw-bg-[#04152f]/75 tw-p-3">
              <span className="tw-text-[0.65rem] tw-font-black tw-uppercase tw-text-[#89a7cc]">Thống kê nhanh</span>
              <div className="tw-mt-2 tw-flex tw-items-end tw-justify-between"><span><b className="tw-block tw-text-[0.88rem]">18.560.000 đ</b><small className="tw-text-[0.6rem] tw-text-[#85a3c8]">Doanh thu hôm nay</small></span><Sparkline className="tw-h-8 tw-w-16" /></div>
            </div>
          </div>
        </div>
        <div className="tw-rounded-[13px] tw-border tw-border-solid tw-border-white/[0.08] tw-bg-[#09244a]/78 tw-p-4">
          <span className="tw-text-[0.66rem] tw-font-black tw-uppercase tw-text-[#8aa8cc]">Nhận diện biển số (OCR)</span>
          <div className="tw-mt-3 tw-rounded-[8px] tw-border-2 tw-border-solid tw-border-[#c9d4e1] tw-bg-white tw-px-4 tw-py-3 tw-text-center tw-text-[clamp(1.45rem,2.5vw,2.1rem)] tw-font-black tw-tracking-[0.06em] tw-text-[#10213a] tw-shadow-inner">59A-482.16</div>
          <div className="tw-mt-2 tw-flex tw-justify-between tw-text-[0.62rem] tw-font-bold tw-text-[#85a3c8]"><span>Thời gian: 10:30:45</span><span>Loại vé: Lượt thường</span></div>
          <div className="tw-mt-4 tw-grid tw-grid-cols-2 tw-gap-2">
            <span className="tw-rounded-[8px] tw-bg-[#04152f]/75 tw-p-3"><small className="tw-block tw-text-[0.6rem] tw-font-bold tw-text-[#85a3c8]">Tỷ lệ lấp đầy</small><b className="tw-mt-1 tw-block tw-text-[1.08rem]">72%</b><span className="tw-mt-2 tw-block tw-h-1.5 tw-rounded-full tw-bg-white/10"><i className="tw-block tw-h-full tw-w-[72%] tw-rounded-full tw-bg-[#1e78ff]" /></span></span>
            <span className="tw-rounded-[8px] tw-bg-[#04152f]/75 tw-p-3"><small className="tw-block tw-text-[0.6rem] tw-font-bold tw-text-[#85a3c8]">Độ chính xác</small><b className="tw-mt-1 tw-block tw-text-[1.08rem]">98.4%</b><span className="tw-mt-2 tw-block tw-h-1.5 tw-rounded-full tw-bg-white/10"><i className="tw-block tw-h-full tw-w-[98%] tw-rounded-full tw-bg-emerald-400" /></span></span>
          </div>
        </div>
      </div>
    </div>
  );
}

export function AboutPage() {
  const [heroPanelActive, setHeroPanelActive] = useState(false);
  const [heroShine, setHeroShine] = useState<HeroShineState | null>(null);
  const [productDashboardActive, setProductDashboardActive] = useState(false);
  const [productDashboardShine, setProductDashboardShine] = useState<HeroShineState | null>(null);
  const [activeTechnologyIndex, setActiveTechnologyIndex] = useState(1);
  const shineIdRef = useRef(0);
  const shineTimeoutRef = useRef<number | null>(null);
  const productShineIdRef = useRef(0);
  const productShineTimeoutRef = useRef<number | null>(null);

  useEffect(() => () => {
    if (shineTimeoutRef.current) window.clearTimeout(shineTimeoutRef.current);
    if (productShineTimeoutRef.current) window.clearTimeout(productShineTimeoutRef.current);
  }, []);

  function playHeroShine(direction: HeroShineState["direction"]) {
    if (shineTimeoutRef.current) window.clearTimeout(shineTimeoutRef.current);
    const id = shineIdRef.current + 1;
    shineIdRef.current = id;
    setHeroShine({ direction, id, traveling: false });
    window.requestAnimationFrame(() => window.requestAnimationFrame(() => setHeroShine({ direction, id, traveling: true })));
    shineTimeoutRef.current = window.setTimeout(() => setHeroShine((current) => current?.id === id ? null : current), 1700);
  }

  function setPanelInteraction(active: boolean) {
    setHeroPanelActive(active);
    playHeroShine(active ? "enter" : "leave");
  }

  function playProductShine(direction: HeroShineState["direction"]) {
    if (productShineTimeoutRef.current) window.clearTimeout(productShineTimeoutRef.current);
    const id = productShineIdRef.current + 1;
    productShineIdRef.current = id;
    setProductDashboardShine({ direction, id, traveling: false });
    window.requestAnimationFrame(() => window.requestAnimationFrame(() => setProductDashboardShine({ direction, id, traveling: true })));
    productShineTimeoutRef.current = window.setTimeout(
      () => setProductDashboardShine((current) => current?.id === id ? null : current),
      1700,
    );
  }

  function setProductInteraction(active: boolean) {
    setProductDashboardActive(active);
    playProductShine(active ? "enter" : "leave");
  }

  return (
    <ClientPage>
      <main className="tw-overflow-hidden tw-bg-white tw-text-[#071b38]">
        <section className="tw-relative tw-mx-auto tw-w-full tw-max-w-[1600px] tw-px-4 tw-pb-[62px] tw-pt-5 min-[992px]:tw-px-9 min-[992px]:tw-pt-7">
          <div className="tw-relative tw-min-h-[850px] tw-overflow-hidden tw-rounded-[28px] tw-bg-[#03152f] tw-shadow-[0_26px_60px_rgba(4,28,66,0.2)] max-[900px]:tw-min-h-[1040px] max-[640px]:tw-min-h-[1320px]">
            <img className="tw-absolute tw-inset-0 tw-h-full tw-w-full tw-object-cover tw-object-center" src="/assets/customer/about/about-hero-operations-hub.webp" alt="Trung tâm điều hành bãi xe thông minh CoParking về đêm" />
            <div className="tw-absolute tw-inset-0 tw-bg-[linear-gradient(90deg,rgba(2,15,35,.98)_0%,rgba(2,18,42,.91)_32%,rgba(2,18,42,.28)_66%,rgba(2,18,42,.18)_100%)] max-[900px]:tw-bg-[linear-gradient(180deg,rgba(2,15,35,.97)_0%,rgba(2,18,42,.73)_55%,rgba(2,18,42,.4)_100%)]" />
            <div className="tw-pointer-events-none tw-absolute -tw-left-20 tw-bottom-4 tw-h-52 tw-w-[470px] tw-rounded-[50%] tw-border tw-border-solid tw-border-[#b88a3a]/40" />
            <div className="tw-pointer-events-none tw-absolute -tw-left-32 tw-bottom-16 tw-h-52 tw-w-[520px] tw-rounded-[50%] tw-border tw-border-solid tw-border-[#b88a3a]/25" />

            <Reveal className="tw-relative tw-z-10 tw-max-w-[810px] tw-px-7 tw-pt-16 min-[768px]:tw-px-[58px] min-[768px]:tw-pt-[66px]">
              <Eyebrow light>Nền tảng quản lý bãi xe thông minh</Eyebrow>
              <h1 className="tw-m-0 tw-mt-5 tw-font-[Cambria] !tw-text-[clamp(2.9rem,4.6vw,4.55rem)] tw-font-bold tw-leading-[0.98] tw-tracking-[-0.025em] tw-text-white">
                Mỗi lượt xe rõ ràng. <span className="tw-mt-2 tw-block tw-text-[#1774ff]">Mỗi vận hành an tâm.</span>
              </h1>
              <p className="tw-m-0 tw-mt-6 tw-max-w-[535px] tw-text-[1.02rem] tw-font-semibold tw-leading-7 tw-text-[#c3d3e9]">CoParking giúp doanh nghiệp kiểm soát ra vào, quản lý vé tháng và doanh thu minh bạch trên một nền tảng tập trung.</p>
              <div className="tw-mt-7 tw-flex tw-flex-wrap tw-gap-5">
                <Link className="tw-inline-flex tw-min-h-12 tw-items-center tw-gap-4 tw-rounded-[8px] tw-bg-[#146cff] tw-px-6 tw-text-[0.86rem] tw-font-black tw-text-white tw-shadow-[0_12px_26px_rgba(20,108,255,.3)] tw-transition hover:-tw-translate-y-0.5 hover:tw-bg-[#287cff] hover:tw-text-white hover:tw-no-underline" to="/pricing">Khám phá giải pháp <i className="fas fa-arrow-right tw-text-[0.75rem]" /></Link>
                <Link className="tw-inline-flex tw-min-h-12 tw-items-center tw-gap-4 tw-border-0 tw-border-b-2 tw-border-solid tw-border-[#c18c34] tw-px-2 tw-text-[0.86rem] tw-font-black tw-text-white tw-transition hover:tw-text-[#72aaff] hover:tw-no-underline" to="/guide">Xem quy trình <i className="fas fa-arrow-right tw-text-[0.75rem] tw-text-[#d6a54d]" /></Link>
              </div>
            </Reveal>

            <Reveal className="tw-absolute tw-right-[4.5%] tw-top-[60px] tw-z-10 tw-w-[225px] max-[900px]:tw-right-6 max-[900px]:tw-top-[420px] max-[640px]:tw-hidden" delay={100}>
              <aside className="tw-rounded-[16px] tw-border tw-border-solid tw-border-white/25 tw-bg-[#061a38]/75 tw-p-4 tw-text-white tw-shadow-[0_18px_38px_rgba(0,8,26,.3)] tw-backdrop-blur-xl">
                <span className="tw-text-[0.74rem] tw-font-black tw-uppercase tw-tracking-[0.08em] tw-text-[#b9cce6]">Trạng thái hiện tại</span>
                <b className="tw-mt-3 tw-flex tw-items-center tw-gap-2 tw-rounded-[7px] tw-bg-emerald-400/10 tw-px-2 tw-py-2 tw-text-[0.7rem] tw-text-emerald-300"><i className="fas fa-circle tw-text-[0.46rem]" /> Đang hoạt động</b>
                <div className="tw-mt-2 tw-grid tw-gap-1.5">
                  {[['fas fa-random', 'Cổng vào', 'Hoạt động'], ['fas fa-sign-out-alt', 'Cổng ra', 'Hoạt động'], ['fas fa-camera', 'Camera OCR', 'Bình thường'], ['far fa-credit-card', 'Máy chủ', 'Bình thường']].map(([icon, title, state]) => (
                    <span className="tw-flex tw-items-center tw-gap-3 tw-rounded-[7px] tw-bg-white/[0.045] tw-px-2 tw-py-2" key={title}><i className={cn(icon, "tw-w-4 tw-text-center tw-text-[#8cb6ed]")} /><small className="tw-leading-4"><b className="tw-block tw-text-[0.67rem] tw-text-white">{title}</b><i className="tw-not-italic tw-text-[0.58rem] tw-font-semibold tw-text-[#86a3c7]">{state}</i></small></span>
                  ))}
                </div>
              </aside>
            </Reveal>

            <div
              className="tw-absolute tw-bottom-[70px] tw-left-1/2 tw-z-10 tw-w-[min(78%,1040px)] -tw-translate-x-1/2 max-[1100px]:tw-w-[86%] max-[900px]:tw-bottom-[64px] max-[900px]:tw-w-[92%]"
              style={{ transform: "translateX(-50%)" }}
              onBlur={() => setPanelInteraction(false)}
              onFocus={() => setPanelInteraction(true)}
              onPointerEnter={() => setPanelInteraction(true)}
              onPointerLeave={() => setPanelInteraction(false)}
              tabIndex={0}
            >
              <LiveOperationsPanel shine={heroShine} active={heroPanelActive} />
            </div>
          </div>

          <Reveal className="tw-relative tw-z-20 tw-mx-auto -tw-mt-[38px] tw-w-[min(86%,1240px)] max-[900px]:tw-w-[92%]" delay={120}>
            <div className="tw-grid tw-grid-cols-4 tw-overflow-hidden tw-rounded-[16px] tw-border tw-border-solid tw-border-[#dce7f7] tw-bg-white tw-shadow-[0_18px_38px_rgba(36,72,123,.13)] max-[700px]:tw-grid-cols-2">
              {proofItems.map((item) => (
                <article className="tw-flex tw-min-h-[88px] tw-items-center tw-justify-center tw-gap-4 tw-border-0 tw-border-r tw-border-solid tw-border-[#dce7f7] tw-px-5 last:tw-border-r-0 max-[700px]:tw-border-b max-[700px]:even:tw-border-r-0" key={item.title}>
                  <i className={cn(item.icon, "tw-text-[1.5rem] tw-text-[#2979ff]")} />
                  <span><b className="tw-block tw-text-[0.96rem] tw-font-black tw-text-[#102442]">{item.title}</b><small className="tw-text-[0.79rem] tw-font-semibold tw-text-[#7387a2]">{item.text}</small></span>
                </article>
              ))}
            </div>
          </Reveal>
        </section>

        <section className="tw-mx-auto tw-grid tw-w-[min(1320px,calc(100%_-_32px))] tw-grid-cols-[0.9fr_1.45fr] tw-items-center tw-gap-14 tw-py-16 max-[960px]:tw-grid-cols-1">
          <Reveal>
            <Eyebrow>Vận hành thông minh</Eyebrow>
            <h2 className="tw-m-0 tw-mt-4 tw-font-[Cambria] !tw-text-[clamp(2.15rem,3.2vw,3.15rem)] tw-font-bold tw-leading-[1.05] tw-text-[#0b1d37]">Một nền tảng.<br />Một luồng vận hành hoàn chỉnh.</h2>
            <p className="tw-m-0 tw-mt-5 tw-max-w-[420px] tw-text-[1.04rem] tw-font-semibold tw-leading-7 tw-text-[#627792]">CoParking kết nối toàn bộ quy trình từ lúc xe vào đến khi đối soát doanh thu, giúp dữ liệu minh bạch, vận hành đơn giản và kiểm soát dễ dàng.</p>
            <div className="tw-mt-7 tw-grid tw-gap-4">
              {productBenefits.map((item) => (
                <span className="tw-flex tw-items-center tw-gap-3 tw-text-[0.92rem] tw-font-bold tw-text-[#354d6d]" key={item.title}><i className={cn(item.icon, "tw-grid tw-h-10 tw-w-10 tw-place-items-center tw-rounded-full tw-bg-[#edf5ff] tw-text-[#2778ff]")} /> {item.title}</span>
              ))}
            </div>
          </Reveal>

          <Reveal className="tw-relative tw-px-10 tw-py-10 max-[640px]:tw-px-0" delay={100}>
            <div
              className="tw-relative tw-z-[5]"
              onBlur={() => setProductInteraction(false)}
              onFocus={() => setProductInteraction(true)}
              onPointerEnter={() => setProductInteraction(true)}
              onPointerLeave={() => setProductInteraction(false)}
              tabIndex={0}
            >
              <ProductDashboard active={productDashboardActive} shine={productDashboardShine} />
            </div>
            <div
              aria-hidden={!productDashboardActive}
              className="tw-pointer-events-none tw-absolute tw-left-0 tw-top-[34%] tw-z-10 tw-w-[140px] tw-rounded-[12px] tw-border tw-border-solid tw-border-[#dce7f5] tw-bg-white tw-p-3 tw-shadow-[0_15px_35px_rgba(30,63,107,.12)] tw-transition-all tw-duration-300 tw-ease-out max-[640px]:tw-hidden"
              style={{ opacity: productDashboardActive ? 1 : 0, transform: productDashboardActive ? "translate3d(0,0,0) scale(1)" : "translate3d(-12px,8px,0) scale(.96)", transitionDelay: productDashboardActive ? "60ms" : "0ms" }}
            >
              <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.78rem] tw-font-black"><i className="fas fa-sign-in-alt tw-text-[#2778ff]" /> Cổng vào</span><small className="tw-mt-2 tw-block tw-text-[0.66rem] tw-font-semibold tw-leading-4 tw-text-[#6c809b]">Nhận diện OCR<br />Ghi nhận thời gian vào bãi</small>
            </div>
            <div
              aria-hidden={!productDashboardActive}
              className="tw-pointer-events-none tw-absolute tw-bottom-0 tw-left-[10%] tw-z-10 tw-w-[145px] tw-rounded-[12px] tw-border tw-border-solid tw-border-[#dce7f5] tw-bg-white tw-p-3 tw-shadow-[0_15px_35px_rgba(30,63,107,.12)] tw-transition-all tw-duration-300 tw-ease-out max-[640px]:tw-hidden"
              style={{ opacity: productDashboardActive ? 1 : 0, transform: productDashboardActive ? "translate3d(0,0,0) scale(1)" : "translate3d(0,12px,0) scale(.96)", transitionDelay: productDashboardActive ? "100ms" : "0ms" }}
            >
              <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.78rem] tw-font-black"><i className="far fa-calendar-check tw-text-[#2778ff]" /> Vé tháng</span><small className="tw-mt-2 tw-block tw-text-[0.66rem] tw-font-semibold tw-leading-4 tw-text-[#6c809b]">Quản lý danh sách<br />Gia hạn & cảnh báo sắp hết hạn</small>
            </div>
            <div
              aria-hidden={!productDashboardActive}
              className="tw-pointer-events-none tw-absolute tw-right-0 tw-top-[18%] tw-z-10 tw-w-[140px] tw-rounded-[12px] tw-border tw-border-solid tw-border-[#dce7f5] tw-bg-white tw-p-3 tw-shadow-[0_15px_35px_rgba(30,63,107,.12)] tw-transition-all tw-duration-300 tw-ease-out max-[640px]:tw-hidden"
              style={{ opacity: productDashboardActive ? 1 : 0, transform: productDashboardActive ? "translate3d(0,0,0) scale(1)" : "translate3d(12px,8px,0) scale(.96)", transitionDelay: productDashboardActive ? "140ms" : "0ms" }}
            >
              <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.78rem] tw-font-black"><i className="far fa-credit-card tw-text-[#2778ff]" /> Thanh toán</span><small className="tw-mt-2 tw-block tw-text-[0.66rem] tw-font-semibold tw-leading-4 tw-text-[#6c809b]">Đa phương thức<br />Hóa đơn & biên lai điện tử</small>
            </div>
            <div
              aria-hidden={!productDashboardActive}
              className="tw-pointer-events-none tw-absolute tw-bottom-[15%] tw-right-0 tw-z-10 tw-w-[140px] tw-rounded-[12px] tw-border tw-border-solid tw-border-[#dce7f5] tw-bg-white tw-p-3 tw-shadow-[0_15px_35px_rgba(30,63,107,.12)] tw-transition-all tw-duration-300 tw-ease-out max-[640px]:tw-hidden"
              style={{ opacity: productDashboardActive ? 1 : 0, transform: productDashboardActive ? "translate3d(0,0,0) scale(1)" : "translate3d(12px,8px,0) scale(.96)", transitionDelay: productDashboardActive ? "180ms" : "0ms" }}
            >
              <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.78rem] tw-font-black"><i className="fas fa-chart-pie tw-text-[#2778ff]" /> Đối soát</span><small className="tw-mt-2 tw-block tw-text-[0.66rem] tw-font-semibold tw-leading-4 tw-text-[#6c809b]">Báo cáo doanh thu<br />Đối soát minh bạch</small>
            </div>
            <span className="tw-pointer-events-none tw-absolute tw-left-[112px] tw-top-[44%] tw-h-px tw-w-[42px] tw-bg-[#c79442] tw-transition-opacity tw-duration-300 max-[640px]:tw-hidden" style={{ opacity: productDashboardActive ? 1 : 0 }} /><span className="tw-pointer-events-none tw-absolute tw-bottom-[88px] tw-left-[22%] tw-h-[34px] tw-w-px tw-bg-[#c79442] tw-transition-opacity tw-duration-300 max-[640px]:tw-hidden" style={{ opacity: productDashboardActive ? 1 : 0 }} /><span className="tw-pointer-events-none tw-absolute tw-right-[112px] tw-top-[27%] tw-h-px tw-w-[42px] tw-bg-[#c79442] tw-transition-opacity tw-duration-300 max-[640px]:tw-hidden" style={{ opacity: productDashboardActive ? 1 : 0 }} /><span className="tw-pointer-events-none tw-absolute tw-bottom-[27%] tw-right-[112px] tw-h-px tw-w-[42px] tw-bg-[#c79442] tw-transition-opacity tw-duration-300 max-[640px]:tw-hidden" style={{ opacity: productDashboardActive ? 1 : 0 }} />
          </Reveal>
        </section>

        <section className="tw-mx-auto tw-w-[min(1460px,calc(100%_-_32px))] tw-overflow-hidden tw-rounded-[24px] tw-bg-[radial-gradient(circle_at_50%_110%,rgba(22,105,255,.23),transparent_38%),linear-gradient(120deg,#041a39,#062753)] tw-px-6 tw-py-14 tw-text-white tw-shadow-[0_24px_50px_rgba(2,24,57,.18)] min-[900px]:tw-px-12">
          <Reveal className="tw-text-center">
            <Eyebrow light>Công nghệ cốt lõi</Eyebrow>
            <h2 className="tw-m-0 tw-mt-3 tw-font-[Cambria] !tw-text-[clamp(2rem,3vw,3.2rem)] tw-font-bold tw-leading-tight">Công nghệ đứng sau một hành trình liền mạch.</h2>
          </Reveal>
          <div className="tw-mt-8 tw-grid tw-grid-cols-3 tw-gap-5 max-[860px]:tw-grid-cols-1">
            {technologyCards.map((item, index) => {
              const isActive = activeTechnologyIndex === index;
              return (
              <Reveal delay={index * 80} key={item.title}>
                <article
                  className={cn(
                    "tw-group tw-relative tw-h-full tw-min-h-[248px] tw-overflow-hidden tw-rounded-[16px] tw-border tw-border-solid tw-p-5 tw-transition-all tw-duration-300 tw-ease-out tw-will-change-transform",
                    isActive
                      ? "tw-border-[#2680ff] tw-bg-[#08295b] tw-shadow-[0_0_0_2px_rgba(38,128,255,.32),0_24px_48px_rgba(0,75,201,.32)]"
                      : "tw-border-white/[0.14] tw-bg-white/[0.035] tw-shadow-none",
                  )}
                  onBlur={() => setActiveTechnologyIndex(1)}
                  onFocus={() => setActiveTechnologyIndex(index)}
                  onPointerEnter={() => setActiveTechnologyIndex(index)}
                  onPointerLeave={() => setActiveTechnologyIndex(1)}
                  style={{ transform: isActive ? "translate3d(0,-9px,0)" : "translate3d(0,0,0)" }}
                  tabIndex={0}
                >
                  <div className="tw-flex tw-items-start tw-gap-3"><i className={cn(item.icon, "tw-grid tw-h-11 tw-w-11 tw-flex-none tw-place-items-center tw-rounded-full tw-bg-[#0d3977] tw-text-[#4a98ff]")} /><div><h3 className="tw-m-0 tw-text-[1.1rem] tw-font-black">{item.title}</h3><p className="tw-m-0 tw-mt-2 tw-text-[0.84rem] tw-font-semibold tw-leading-5 tw-text-[#b7cae4]">{item.text}</p></div></div>
                  {item.image ? <img className="tw-absolute tw-bottom-0 tw-left-0 tw-h-[116px] tw-w-full tw-object-cover tw-opacity-85 tw-transition tw-duration-500 group-hover:tw-scale-105" src={item.image} alt="Hệ thống kiểm soát xe ra vào" /> : item.featured ? (
                    <div className="tw-mt-7 tw-rounded-[11px] tw-border tw-border-solid tw-border-white/10 tw-bg-[#051a3b] tw-p-4">
                      <span className="tw-flex tw-items-center tw-justify-between tw-text-[0.75rem] tw-font-bold tw-text-[#a9c5e9]"><span><i className="far fa-calendar-check tw-mr-2 tw-text-[#4a98ff]" />Vé tháng</span><b className="tw-text-white">51A-123.45</b><small>Còn 3 ngày</small></span><span className="tw-mt-4 tw-block tw-h-2 tw-rounded-full tw-bg-white/10"><i className="tw-block tw-h-full tw-w-[72%] tw-rounded-full tw-bg-[#1c73ff]" /></span>
                    </div>
                  ) : (
                    <div className="tw-mt-7 tw-flex tw-items-end tw-justify-between tw-rounded-[11px] tw-border tw-border-solid tw-border-white/10 tw-bg-[#051a3b] tw-p-4"><span><small className="tw-block tw-text-[0.68rem] tw-font-bold tw-text-[#91add0]">Doanh thu hôm nay</small><b className="tw-mt-2 tw-block tw-text-[1.3rem]">18.560.000 đ</b></span><Sparkline /></div>
                  )}
                </article>
              </Reveal>
              );
            })}
          </div>
        </section>

        <section className="tw-mx-auto tw-w-[min(1460px,calc(100%_-_32px))] tw-py-14">
          <Reveal className="tw-text-center"><Eyebrow>Quy trình vận hành</Eyebrow><h2 className="tw-m-0 tw-mt-3 tw-font-[Cambria] !tw-text-[clamp(2rem,3.1vw,3.15rem)] tw-font-bold tw-leading-tight tw-text-[#0a1f3c]">4 bước đơn giản cho một hành trình liền mạch.</h2></Reveal>
          <div className="tw-relative tw-mt-8 tw-grid tw-grid-cols-4 tw-gap-4 before:tw-absolute before:tw-left-[5%] before:tw-right-[5%] before:tw-top-1/2 before:tw-h-px before:tw-bg-[#c99646] before:tw-content-[''] max-[820px]:tw-grid-cols-2 max-[820px]:before:tw-hidden max-[500px]:tw-grid-cols-1">
            {workflowSteps.map((step, index) => (
              <Reveal className="tw-relative tw-z-[1]" delay={index * 70} key={step.no}>
                <article className="tw-flex tw-min-h-[116px] tw-items-center tw-gap-4 tw-rounded-[14px] tw-border tw-border-solid tw-border-[#dfe8f5] tw-bg-white tw-p-4 tw-shadow-[0_12px_28px_rgba(25,57,99,.07)] tw-transition tw-duration-300 hover:-tw-translate-y-1 hover:tw-border-brand-200 hover:tw-shadow-[0_18px_34px_rgba(25,91,184,.13)]">
                  <i className={cn(step.icon, "tw-grid tw-h-11 tw-w-11 tw-flex-none tw-place-items-center tw-rounded-full tw-bg-[#f0f6ff] tw-text-[#376cae]")} /><span><small className="tw-text-[0.7rem] tw-font-black tw-text-[#c18d3d]">{step.no}</small><b className="tw-block tw-text-[0.94rem] tw-font-black tw-text-[#142942]">{step.title}</b><p className="tw-m-0 tw-mt-1 tw-text-[0.74rem] tw-font-semibold tw-leading-5 tw-text-[#6a7e98]">{step.text}</p></span>
                </article>
              </Reveal>
            ))}
          </div>
          <Reveal className="tw-mt-8 tw-overflow-hidden tw-rounded-[14px] tw-shadow-[0_18px_35px_rgba(20,54,96,.14)]"><img className="tw-block tw-h-[118px] tw-w-full tw-object-cover tw-object-center min-[900px]:tw-h-[142px]" src="/assets/customer/about/about-workflow-parking-lane.webp" alt="Làn xe vận hành tự động của CoParking" /></Reveal>
        </section>

        <section className="tw-mx-auto tw-w-[min(1460px,calc(100%_-_32px))] tw-pb-14">
          <Reveal className="tw-text-center"><Eyebrow>Phù hợp với mọi mô hình</Eyebrow><h2 className="tw-m-0 tw-mt-3 tw-font-[Cambria] !tw-text-[clamp(2rem,3.1vw,3.15rem)] tw-font-bold tw-leading-tight tw-text-[#0a1f3c]">Phù hợp với mọi mô hình bãi xe đang vận hành.</h2></Reveal>
          <div className="tw-mt-8 tw-grid tw-grid-cols-6 tw-gap-4 max-[1050px]:tw-grid-cols-3 max-[600px]:tw-grid-cols-2">
            {deploymentModels.map((item, index) => (
              <Reveal delay={index * 50} key={item.title}>
                <article className="tw-group tw-h-full tw-overflow-hidden tw-rounded-[14px] tw-border tw-border-solid tw-border-[#deE8f5] tw-bg-white tw-shadow-[0_12px_28px_rgba(25,57,99,.07)] tw-transition tw-duration-300 hover:-tw-translate-y-1 hover:tw-shadow-[0_18px_38px_rgba(25,91,184,.14)]">
                  <div className="tw-h-[120px] tw-overflow-hidden"><img className="tw-h-full tw-w-full tw-object-cover tw-transition tw-duration-500 group-hover:tw-scale-105" src={item.image} alt={item.title} /></div>
                  <div className="tw-p-4"><h3 className="tw-m-0 tw-text-[1rem] tw-font-black tw-text-[#102541]">{item.title}</h3><b className="tw-mt-1 tw-block tw-text-[0.75rem] tw-font-black tw-text-[#2778ff]">{item.value}</b><p className="tw-m-0 tw-mt-2 tw-text-[0.74rem] tw-font-semibold tw-leading-5 tw-text-[#71849d]">{item.text}</p></div>
                </article>
              </Reveal>
            ))}
          </div>
        </section>

        <Reveal className="tw-mx-auto tw-w-[min(1460px,calc(100%_-_32px))]">
          <section className="tw-relative tw-grid tw-grid-cols-4 tw-overflow-hidden tw-rounded-[20px] tw-bg-[linear-gradient(110deg,#061b3a,#082d5e)] tw-px-7 tw-py-8 tw-text-white tw-shadow-[0_20px_42px_rgba(5,30,67,.18)] max-[700px]:tw-grid-cols-2">
            <span className="tw-pointer-events-none tw-absolute -tw-bottom-20 -tw-left-16 tw-h-40 tw-w-80 tw-rounded-[50%] tw-border tw-border-solid tw-border-[#c2903f]/60" /><span className="tw-pointer-events-none tw-absolute -tw-bottom-24 -tw-left-4 tw-h-40 tw-w-80 tw-rounded-[50%] tw-border tw-border-solid tw-border-[#c2903f]/35" />
            {impactItems.map((item) => (
              <article className="tw-relative tw-z-[1] tw-flex tw-items-center tw-justify-center tw-gap-4 tw-border-0 tw-border-r tw-border-solid tw-border-[#b68a48]/40 tw-px-4 last:tw-border-r-0 max-[700px]:tw-border-b max-[700px]:tw-py-4 max-[700px]:even:tw-border-r-0" key={item.value}><i className={cn(item.icon, "tw-grid tw-h-12 tw-w-12 tw-place-items-center tw-rounded-full tw-border tw-border-solid tw-border-[#ba8b43]/70 tw-text-[#d7a857]")} /><span><b className="tw-text-[1.95rem] tw-font-black">{item.value}</b><small className="tw-block tw-text-[0.72rem] tw-font-semibold tw-text-[#c4d5eb]">{item.text}</small></span></article>
            ))}
          </section>
        </Reveal>

        <section className="tw-mx-auto tw-grid tw-w-[min(1360px,calc(100%_-_32px))] tw-grid-cols-[0.8fr_1.2fr] tw-gap-20 tw-py-16 max-[860px]:tw-grid-cols-1 max-[860px]:tw-gap-10">
          <Reveal>
            <Eyebrow>Triển khai linh hoạt</Eyebrow><h2 className="tw-m-0 tw-mt-3 tw-font-[Cambria] !tw-text-[clamp(2rem,3.5vw,3.35rem)] tw-font-bold tw-leading-[1.06] tw-text-[#0a1f3c]">Sẵn sàng triển khai theo quy mô của bạn</h2>
            <ul className="tw-m-0 tw-mt-6 tw-grid tw-list-none tw-gap-3 tw-p-0">{["Triển khai nhanh chóng từ 1–3 ngày", "Hỗ trợ thiết bị đa dạng, dễ mở rộng", "Đội ngũ đồng hành 24/7"].map((item) => <li className="tw-flex tw-gap-3 tw-text-[0.94rem] tw-font-bold tw-text-[#617690]" key={item}><i className="fas fa-check tw-mt-1 tw-text-[#2478ff]" />{item}</li>)}</ul>
            <Link className="tw-mt-7 tw-inline-flex tw-min-h-11 tw-items-center tw-gap-4 tw-rounded-[7px] tw-bg-[#176fff] tw-px-6 tw-text-[0.88rem] tw-font-black tw-text-white tw-shadow-[0_12px_24px_rgba(23,111,255,.22)] hover:tw-bg-[#2a7cff] hover:tw-text-white hover:tw-no-underline" to="/contact">Liên hệ tư vấn <i className="fas fa-arrow-right tw-text-[0.76rem]" /></Link>
          </Reveal>
          <Reveal delay={90}>
            <Eyebrow>Câu hỏi thường gặp</Eyebrow>
            <div className="tw-mt-4 tw-grid tw-gap-3">
              {faqItems.map((item, index) => (
                <details className="tw-group tw-rounded-[12px] tw-border tw-border-solid tw-border-[#dce7f5] tw-bg-white tw-px-5 tw-py-4 tw-shadow-[0_8px_20px_rgba(24,55,96,.05)] open:tw-border-brand-200 open:tw-shadow-[0_14px_30px_rgba(37,99,235,.1)]" open={index === 0} key={item.question}>
                  <summary className="tw-flex tw-cursor-pointer tw-list-none tw-items-center tw-justify-between tw-gap-4 tw-text-[0.94rem] tw-font-black tw-text-[#172d49] after:tw-content-['＋'] group-open:after:tw-content-['−']">{item.question}</summary>
                  <p className="tw-m-0 tw-mt-3 tw-pr-8 tw-text-[0.84rem] tw-font-semibold tw-leading-6 tw-text-[#6b8099]">{item.answer}</p>
                </details>
              ))}
            </div>
          </Reveal>
        </section>

        <Reveal className="tw-mx-auto tw-w-[min(1460px,calc(100%_-_32px))] tw-pb-5">
          <section className="tw-relative tw-flex tw-min-h-[132px] tw-flex-wrap tw-items-center tw-justify-center tw-gap-x-16 tw-gap-y-5 tw-overflow-hidden tw-rounded-[20px] tw-bg-[linear-gradient(110deg,#061b3a,#082d5e)] tw-px-7 tw-py-7 tw-text-center tw-text-white tw-shadow-[0_20px_42px_rgba(5,30,67,.18)]">
            <span className="tw-pointer-events-none tw-absolute -tw-bottom-24 -tw-left-16 tw-h-44 tw-w-[440px] tw-rounded-[50%] tw-border tw-border-solid tw-border-[#c2903f]/55" /><span className="tw-pointer-events-none tw-absolute -tw-right-20 -tw-top-24 tw-h-48 tw-w-[380px] tw-rounded-[50%] tw-border tw-border-solid tw-border-[#c2903f]/35" />
            <div className="tw-relative tw-z-[1]"><h2 className="tw-m-0 tw-font-[Cambria] !tw-text-[clamp(1.75rem,2.9vw,2.8rem)] tw-font-bold">Sẵn sàng nâng cấp trải nghiệm bãi xe?</h2><p className="tw-m-0 tw-mt-2 tw-text-[0.84rem] tw-font-semibold tw-text-[#b8cae3]">CoParking đồng hành cùng bạn xây dựng hệ thống bãi xe thông minh, minh bạch và hiệu quả.</p></div>
            <Link className="tw-relative tw-z-[1] tw-inline-flex tw-min-h-11 tw-items-center tw-gap-4 tw-rounded-[7px] tw-bg-[#176fff] tw-px-6 tw-text-[0.88rem] tw-font-black tw-text-white tw-shadow-[0_12px_24px_rgba(23,111,255,.24)] hover:-tw-translate-y-0.5 hover:tw-bg-[#2a7cff] hover:tw-text-white hover:tw-no-underline" to="/contact">Liên hệ tư vấn <i className="fas fa-arrow-right tw-text-[0.76rem]" /></Link>
          </section>
        </Reveal>
      </main>
      <PublicFooter />
    </ClientPage>
  );
}
