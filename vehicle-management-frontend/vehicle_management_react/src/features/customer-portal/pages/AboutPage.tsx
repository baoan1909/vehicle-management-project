import { useEffect, useRef, useState, type CSSProperties, type ReactNode } from "react";
import { Link } from "react-router-dom";

import { ClientPage } from "@/shared/components/layout/ClientPage";
import { cn } from "@/lib/cn";

import { PublicFooter } from "./PortalShared";

const audienceTags = ["Chung cư", "Văn phòng", "Trường học", "Bệnh viện", "TTTM", "Bãi thuê"];

const metrics = [
  { label: "Theo dõi vận hành", value: "24/7", text: "Cổng, phiên gửi xe và thông báo được cập nhật liên tục." },
  { label: "Nhận diện biển số", value: "OCR", text: "Hạn chế nhập tay, giảm sai sót khi xe vào và ra." },
  { label: "Dữ liệu tức thời", value: "Realtime", text: "Bảo vệ, quản lý và khách hàng cùng nhìn một trạng thái." },
];

const workflow = [
  {
    icon: "fas fa-camera",
    label: "01",
    title: "Nhận diện và kiểm soát lượt vào ra",
    text: "Camera OCR, biển số, thẻ xe và trạng thái phiên được nối thành một dòng sự kiện rõ ràng.",
  },
  {
    icon: "far fa-user",
    label: "02",
    title: "Quản lý khách hàng, xe và vé tháng",
    text: "Khách đăng ký xe, chọn gói, theo dõi lịch sử gửi xe; quản lý duyệt và gia hạn rõ ràng.",
  },
  {
    icon: "fas fa-receipt",
    label: "03",
    title: "Đối soát, ca trực và hỗ trợ sự cố",
    text: "Doanh thu, hóa đơn, mất thẻ, ticket hỗ trợ và thông báo realtime được gom về cùng nơi.",
  },
];

const operationRows: Array<[string, string, string, "blue" | "green" | "orange"]> = [
  ["08:15", "Khách gửi yêu cầu vé tháng", "Chờ duyệt", "orange"],
  ["09:02", "Quản lý kiểm tra biển số và hồ sơ", "Đạt", "green"],
  ["09:10", "Thanh toán VNPAY được ghi nhận", "Đã trả", "green"],
  ["09:12", "Vé tháng kích hoạt cho xe 59A", "Active", "blue"],
];

const faqItems = [
  ["CoParking phù hợp với mô hình nào?", "Phù hợp cho chung cư, tòa nhà văn phòng, trường học, bệnh viện, trung tâm thương mại và bãi xe thuê ngoài."],
  ["Có hỗ trợ cả khách vãng lai và vé tháng không?", "Có. Hệ thống tách rõ luồng khách vãng lai, khách đăng ký, vé tháng, gia hạn và lịch sử gửi xe."],
  ["Dữ liệu vận hành có cập nhật realtime không?", "Có. Các trạng thái vào ra, thanh toán, ticket hỗ trợ và thông báo được đồng bộ để đội vận hành xử lý nhanh."],
  ["Trang giới thiệu dùng chung với customer được không?", "Được. Header public sẽ tự đổi sang chuông thông báo và thông tin khách hàng khi người dùng đã đăng nhập."],
];

const revealBaseClass =
  "tw-transition-all tw-duration-700 tw-ease-out motion-reduce:tw-transform-none motion-reduce:tw-opacity-100 motion-reduce:tw-transition-none";

type HeroShineState = {
  direction: "enter" | "leave";
  id: number;
  traveling: boolean;
};

function Reveal({
  children,
  className,
  delay = 0,
}: {
  children: ReactNode;
  className?: string;
  delay?: number;
}) {
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
      { rootMargin: "0px 0px -12% 0px", threshold: 0.18 },
    );

    observer.observe(element);
    return () => observer.disconnect();
  }, []);

  return (
    <div
      ref={ref}
      className={cn(
        revealBaseClass,
        visible ? "tw-translate-y-0 tw-opacity-100" : "tw-translate-y-8 tw-opacity-0",
        className,
      )}
      style={{ transitionDelay: `${delay}ms` }}
    >
      {children}
    </div>
  );
}

function PrimaryLink({ children, to }: { children: ReactNode; to: string }) {
  return (
    <Link
      className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-rounded-full tw-border tw-border-solid tw-border-slate-950 tw-bg-slate-950 tw-px-5 tw-text-[0.86rem] tw-font-black tw-text-white tw-shadow-[0_16px_30px_rgba(15,23,42,0.18)] tw-transition hover:-tw-translate-y-0.5 hover:tw-bg-brand-700 hover:tw-text-white hover:tw-no-underline hover:tw-shadow-[0_20px_36px_rgba(37,99,235,0.22)] active:tw-translate-y-0"
      to={to}
    >
      {children}
    </Link>
  );
}

function SecondaryLink({ children, to }: { children: ReactNode; to: string }) {
  return (
    <Link
      className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-rounded-full tw-border tw-border-solid tw-border-vm-slate-200 tw-bg-white tw-px-5 tw-text-[0.86rem] tw-font-black tw-text-slate-950 tw-shadow-[0_10px_22px_rgba(15,23,42,0.05)] tw-transition hover:-tw-translate-y-0.5 hover:tw-border-brand-200 hover:tw-bg-brand-50 hover:tw-text-vm-primary hover:tw-no-underline active:tw-translate-y-0"
      to={to}
    >
      {children}
    </Link>
  );
}

function Kicker({ children }: { children: ReactNode }) {
  return (
    <span className="tw-mb-3 tw-inline-flex tw-w-fit tw-items-center tw-rounded-full tw-bg-brand-50 tw-px-3.5 tw-py-1.5 tw-text-[0.72rem] tw-font-black tw-uppercase tw-tracking-normal tw-text-vm-primary tw-ring-1 tw-ring-brand-100">
      {children}
    </span>
  );
}

function StatusBadge({ children, tone }: { children: ReactNode; tone: "blue" | "green" | "orange" }) {
  return (
    <b
      className={cn(
        "tw-rounded-full tw-px-2.5 tw-py-1 tw-text-[0.7rem] tw-font-black",
        tone === "blue" && "tw-bg-brand-50 tw-text-vm-primary",
        tone === "green" && "tw-bg-emerald-50 tw-text-emerald-700",
        tone === "orange" && "tw-bg-amber-50 tw-text-amber-700",
      )}
    >
      {children}
    </b>
  );
}

export function AboutPage() {
  const [heroCardActive, setHeroCardActive] = useState(false);
  const [heroShine, setHeroShine] = useState<HeroShineState | null>(null);
  const heroShineIdRef = useRef(0);
  const heroShineTimeoutRef = useRef<number | null>(null);

  useEffect(() => {
    return () => {
      if (heroShineTimeoutRef.current) {
        window.clearTimeout(heroShineTimeoutRef.current);
      }
    };
  }, []);

  function playHeroShine(direction: HeroShineState["direction"]) {
    if (heroShineTimeoutRef.current) {
      window.clearTimeout(heroShineTimeoutRef.current);
    }

    const id = heroShineIdRef.current + 1;
    heroShineIdRef.current = id;

    setHeroShine({ direction, id, traveling: false });
    window.requestAnimationFrame(() => {
      window.requestAnimationFrame(() => setHeroShine({ direction, id, traveling: true }));
    });
    heroShineTimeoutRef.current = window.setTimeout(() => {
      setHeroShine((current) => (current?.id === id ? null : current));
    }, 3050);
  }

  function activateHeroCard() {
    setHeroCardActive(true);
    playHeroShine("enter");
  }

  function deactivateHeroCard() {
    setHeroCardActive(false);
    playHeroShine("leave");
  }

  return (
    <ClientPage>
      <main className="tw-overflow-hidden tw-bg-[#f8fbff] tw-text-slate-950">
        <section className="tw-relative tw-isolate tw-bg-[radial-gradient(circle_at_86%_18%,rgba(37,99,235,0.16),transparent_26%),linear-gradient(180deg,#ffffff_0%,#f8fbff_100%)]">
          <div className="tw-mx-auto tw-grid tw-w-[min(1180px,calc(100%_-_32px))] tw-grid-cols-[minmax(0,0.95fr)_minmax(360px,0.78fr)] tw-items-start tw-gap-14 tw-pt-9 tw-pb-14 max-[980px]:tw-grid-cols-1 max-[640px]:tw-pt-7 max-[640px]:tw-pb-10">
            <Reveal>
              <Kicker>Nền tảng quản lý bãi xe thông minh</Kicker>
              <h1 className="tw-m-0 tw-max-w-[680px] tw-text-[clamp(2.4rem,5vw,4.8rem)] tw-font-black tw-leading-[1.02] tw-tracking-normal tw-text-slate-950">
                Quản lý bãi xe rõ ràng từ cổng vào đến đối soát.
              </h1>
              <p className="tw-m-0 tw-mt-5 tw-max-w-[620px] tw-text-[1.08rem] tw-font-semibold tw-leading-8 tw-text-vm-slate-500">
                CoParking giúp đội vận hành theo dõi xe ra vào, vé tháng, thanh toán, ca trực và sự cố trên một luồng dữ liệu thống nhất.
              </p>
              <div className="tw-mt-7 tw-flex tw-flex-wrap tw-gap-3">
                <PrimaryLink to="/pricing">Khám phá giải pháp</PrimaryLink>
                <SecondaryLink to="/guide">Xem quy trình</SecondaryLink>
              </div>
            </Reveal>

            <Reveal className="tw-relative tw-min-h-[420px] max-[640px]:tw-min-h-[360px]" delay={90}>
              <div
                className="tw-group tw-relative tw-min-h-[420px] max-[640px]:tw-min-h-[360px]"
                onBlur={deactivateHeroCard}
                onFocus={activateHeroCard}
                onPointerEnter={activateHeroCard}
                onPointerLeave={deactivateHeroCard}
              >
              <div className="tw-absolute tw-inset-x-10 tw-top-0 tw-h-72 tw-rounded-full tw-bg-brand-100/70 tw-blur-3xl" />
              <div
                className={cn(
                  "tw-relative tw-ml-auto tw-w-[min(100%,392px)] tw-transform-gpu tw-rounded-[34px] tw-bg-white tw-p-4 tw-shadow-[0_28px_58px_rgba(15,23,42,0.14)] tw-ring-1 tw-ring-vm-slate-100 tw-transition-all tw-duration-300 tw-ease-out tw-will-change-transform group-hover:-tw-translate-y-8 group-hover:tw-shadow-[0_22px_48px_rgba(15,23,42,0.2)]",
                  heroCardActive
                    ? "-tw-translate-y-5 tw-shadow-[0_22px_48px_rgba(15,23,42,0.2)]"
                    : "tw-translate-y-0 tw-shadow-[0_28px_58px_rgba(15,23,42,0.14)]",
                )}
                style={{ transform: heroCardActive ? "translate3d(0, -18px, 0)" : "translate3d(0, 0, 0)" }}
              >
                <div className="tw-relative tw-overflow-hidden tw-rounded-[26px] tw-bg-slate-950 tw-p-5 tw-text-white">
                <span
                  key={heroShine?.id ?? "hero-shine-idle"}
                  aria-hidden="true"
                  className="tw-pointer-events-none tw-absolute -tw-left-[58%] -tw-top-[96%] tw-z-20 tw-h-[250%] tw-w-[62%] tw-opacity-0 tw-blur-[0.5px] tw-mix-blend-screen tw-shadow-[0_0_22px_rgba(255,255,255,0.32)]"
                  style={{
                    background: "linear-gradient(90deg, transparent 0%, rgba(255,255,255,0.1) 30%, rgba(255,255,255,0.56) 50%, rgba(255,255,255,0.1) 70%, transparent 100%)",
                    opacity: heroShine?.traveling ? 0.82 : 0,
                    transform:
                      heroShine?.direction === "leave"
                        ? heroShine.traveling
                          ? "translate3d(-105%, -60%, 0) rotate(34deg)"
                          : "translate3d(345%, 155%, 0) rotate(34deg)"
                        : heroShine?.traveling
                          ? "translate3d(345%, 155%, 0) rotate(34deg)"
                          : "translate3d(-105%, -60%, 0) rotate(34deg)",
                    transition: heroShine?.traveling
                      ? "transform 2.85s cubic-bezier(0.22, 1, 0.36, 1), opacity 220ms ease-out"
                      : "transform 0ms linear, opacity 180ms ease-in",
                  }}
                />
                <div className="tw-relative tw-z-[1] tw-flex tw-items-center tw-justify-between tw-gap-3">
                  <span className="tw-text-[0.72rem] tw-font-black tw-uppercase tw-text-slate-400">Trạm cổng đang chạy</span>
                  <b className="tw-rounded-full tw-bg-emerald-400/[0.15] tw-px-3 tw-py-1 tw-text-[0.68rem] tw-font-black tw-text-emerald-300">Online</b>
                </div>
                <h2 className="tw-relative tw-z-[1] tw-m-0 tw-mt-4 tw-text-[1.65rem] tw-font-black tw-leading-tight">Cổng B1 - luồng xe vào</h2>
                <div className="tw-relative tw-z-[1] tw-mt-5 tw-grid tw-grid-cols-3 tw-gap-3">
                  {[
                    ["1.284", "Lượt hôm nay"],
                    ["126", "Chỗ trống"],
                    ["342", "Vé tháng"],
                  ].map(([value, label]) => (
                    <span className="tw-rounded-vm-lg tw-bg-white/[0.08] tw-p-3 tw-transition hover:tw-bg-white/[0.12]" key={label}>
                      <strong className="tw-block tw-text-[1.18rem] tw-font-black">{value}</strong>
                      <small className="tw-mt-1 tw-block tw-text-[0.68rem] tw-font-bold tw-leading-4 tw-text-slate-400">{label}</small>
                    </span>
                  ))}
                </div>
                <div className="tw-relative tw-z-[1] tw-mt-5 tw-rounded-vm-lg tw-bg-white tw-p-3 tw-text-slate-950">
                  <small className="tw-block tw-text-[0.74rem] tw-font-black tw-text-vm-slate-500">Biển số nhận diện</small>
                  <strong className="tw-mt-2 tw-block tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-950 tw-bg-brand-50 tw-py-2 tw-text-center tw-text-[1.55rem] tw-font-black tw-tracking-normal">59A-482.16</strong>
                </div>
                <div className="tw-relative tw-z-[1] tw-mt-3 tw-grid tw-gap-2 tw-rounded-vm-lg tw-bg-white tw-p-3 tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-700">
                  <span className="tw-flex tw-items-center tw-justify-between">Khách vãng lai <StatusBadge tone="orange">Chờ trả</StatusBadge></span>
                  <span className="tw-flex tw-items-center tw-justify-between">Vé tháng hợp lệ <StatusBadge tone="green">Cho qua</StatusBadge></span>
                </div>
              </div>
              </div>
              <div
                className={cn(
                  "tw-absolute tw-right-0 tw-top-8 tw-transform-gpu tw-rounded-vm-lg tw-bg-white tw-px-4 tw-py-3 tw-shadow-[0_18px_40px_rgba(15,23,42,0.16)] tw-transition-all tw-duration-300 tw-ease-out group-hover:-tw-translate-y-6 group-hover:tw-scale-[1.05] group-hover:tw-shadow-[0_22px_46px_rgba(15,23,42,0.22)] max-[640px]:tw-right-2",
                  heroCardActive
                    ? "-tw-translate-y-3 tw-scale-100 tw-opacity-100 tw-shadow-[0_22px_46px_rgba(15,23,42,0.22)]"
                    : "tw-translate-y-2 tw-scale-[0.96] tw-opacity-0",
                )}
                style={{
                  opacity: heroCardActive ? 1 : 0,
                  pointerEvents: heroCardActive ? "auto" : "none",
                  transform: heroCardActive ? "translate3d(0, -12px, 0) scale(1)" : "translate3d(0, 10px, 0) scale(0.96)",
                }}
              >
                <span className="tw-block tw-text-[0.68rem] tw-font-black tw-uppercase tw-text-vm-slate-500">Độ chính xác OCR</span>
                <strong className="tw-text-[1.25rem] tw-font-black tw-text-slate-950">98.4%</strong>
                <span
                  className={cn(
                    "tw-pointer-events-none tw-absolute tw-left-1/2 tw-top-[calc(100%_+_10px)] tw-z-20 tw-w-max -tw-translate-x-1/2 tw-rounded-full tw-bg-slate-950 tw-px-3 tw-py-1.5 tw-text-[0.68rem] tw-font-black tw-text-white tw-shadow-[0_14px_28px_rgba(15,23,42,0.28)] tw-transition-all tw-duration-300 group-hover:tw-translate-y-0 group-hover:tw-opacity-100",
                    heroCardActive ? "tw-translate-y-0 tw-opacity-100" : "tw-translate-y-2 tw-opacity-0",
                  )}
                >OCR ổn định</span>
              </div>
              <div
                className={cn(
                  "tw-absolute tw-bottom-8 tw-left-2 tw-transform-gpu tw-rounded-vm-lg tw-bg-white tw-px-4 tw-py-3 tw-shadow-[0_18px_40px_rgba(15,23,42,0.14)] tw-transition-all tw-duration-300 tw-ease-out group-hover:-tw-translate-y-6 group-hover:tw-scale-[1.05] group-hover:tw-shadow-[0_22px_46px_rgba(15,23,42,0.22)]",
                  heroCardActive
                    ? "-tw-translate-y-3 tw-scale-100 tw-opacity-100 tw-shadow-[0_22px_46px_rgba(15,23,42,0.22)]"
                    : "tw-translate-y-2 tw-scale-[0.96] tw-opacity-0",
                )}
                style={{
                  opacity: heroCardActive ? 1 : 0,
                  pointerEvents: heroCardActive ? "auto" : "none",
                  transform: heroCardActive ? "translate3d(0, -12px, 0) scale(1)" : "translate3d(0, 10px, 0) scale(0.96)",
                }}
              >
                <span className="tw-block tw-text-[0.68rem] tw-font-black tw-uppercase tw-text-vm-slate-500">Doanh thu hôm nay</span>
                <strong className="tw-text-[1.25rem] tw-font-black tw-text-slate-950">18.6 triệu</strong>
                <span
                  className={cn(
                    "tw-pointer-events-none tw-absolute tw-bottom-[calc(100%_+_10px)] tw-left-1/2 tw-z-20 tw-w-max -tw-translate-x-1/2 tw-rounded-full tw-bg-slate-950 tw-px-3 tw-py-1.5 tw-text-[0.68rem] tw-font-black tw-text-white tw-shadow-[0_14px_28px_rgba(15,23,42,0.28)] tw-transition-all tw-duration-300 group-hover:tw-translate-y-0 group-hover:tw-opacity-100",
                    heroCardActive ? "tw-translate-y-0 tw-opacity-100" : "tw-translate-y-2 tw-opacity-0",
                  )}
                >Theo dõi tức thời</span>
              </div>
              </div>
            </Reveal>
          </div>
        </section>

        <section className="tw-bg-[#eef6ff]">
          <div className="tw-mx-auto tw-grid tw-w-[min(1180px,calc(100%_-_32px))] tw-grid-cols-[minmax(0,1fr)_420px] tw-items-center tw-gap-8 tw-py-8 max-[900px]:tw-grid-cols-1">
            <Reveal>
              <Kicker>Thiết kế cho các mô hình bãi xe đang vận hành</Kicker>
              <h2 className="tw-m-0 tw-max-w-[780px] tw-text-[1.28rem] tw-font-black tw-leading-7 tw-text-slate-950">
                Phù hợp cho chung cư, tòa nhà văn phòng, trường học, trung tâm thương mại và bãi xe thuê ngoài.
              </h2>
            </Reveal>
            <Reveal className="tw-flex tw-flex-wrap tw-justify-end tw-gap-2 max-[900px]:tw-justify-start" delay={80}>
              {audienceTags.map((tag) => (
                <span className="tw-rounded-full tw-border tw-border-solid tw-border-brand-100 tw-bg-white tw-px-3.5 tw-py-2 tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700 tw-shadow-[0_8px_16px_rgba(37,99,235,0.06)] tw-transition hover:-tw-translate-y-0.5 hover:tw-border-brand-200 hover:tw-text-vm-primary" key={tag}>
                  {tag}
                </span>
              ))}
            </Reveal>
          </div>
        </section>

        <section className="tw-mx-auto tw-grid tw-w-[min(1180px,calc(100%_-_32px))] tw-grid-cols-3 tw-gap-6 tw-py-12 max-[900px]:tw-grid-cols-1">
          {metrics.map((item, index) => (
            <Reveal delay={index * 70} key={item.label}>
              <article className="tw-group tw-h-full tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-6 tw-shadow-[0_18px_34px_rgba(15,23,42,0.06)] tw-transition tw-duration-300 hover:-tw-translate-y-1 hover:tw-border-brand-200 hover:tw-shadow-[0_24px_46px_rgba(37,99,235,0.14)]">
                <span className="tw-mb-5 tw-block tw-h-7 tw-w-7 tw-rounded-full tw-bg-vm-primary tw-transition group-hover:tw-scale-110 group-hover:tw-shadow-[0_0_0_8px_rgba(37,99,235,0.1)]" />
                <strong className="tw-block tw-text-[2rem] tw-font-black tw-text-slate-950">{item.value}</strong>
                <b className="tw-mt-1 tw-block tw-text-[0.82rem] tw-font-black tw-text-vm-slate-700">{item.label}</b>
                <p className="tw-m-0 tw-mt-3 tw-text-[0.92rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-500">{item.text}</p>
              </article>
            </Reveal>
          ))}
        </section>

        <section className="tw-mx-auto tw-w-[min(1180px,calc(100%_-_32px))] tw-pb-10">
          <Reveal>
            <div className="tw-relative tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-brand-100 tw-bg-gradient-to-br tw-from-brand-50 tw-to-white tw-p-8 tw-shadow-[0_18px_36px_rgba(37,99,235,0.08)] tw-transition tw-duration-500 before:tw-absolute before:tw-inset-y-0 before:-tw-left-1/3 before:tw-w-1/3 before:tw-bg-gradient-to-r before:tw-from-transparent before:tw-via-white/80 before:tw-to-transparent before:tw-content-[''] before:tw-transition-all before:tw-duration-700 hover:tw-border-brand-200 hover:tw-shadow-[0_24px_50px_rgba(37,99,235,0.13)] hover:before:tw-left-full">
              <Kicker>Bài toán vận hành phổ biến</Kicker>
              <h2 className="tw-m-0 tw-max-w-[780px] tw-text-[clamp(1.7rem,3vw,2.45rem)] tw-font-black tw-leading-tight tw-text-slate-950">
                Không chỉ ghi nhận xe ra vào, CoParking giúp kiểm soát toàn bộ vòng đời gửi xe.
              </h2>
              <p className="tw-m-0 tw-mt-4 tw-max-w-[900px] tw-text-[1rem] tw-font-semibold tw-leading-7 tw-text-vm-slate-500">
                Từ đăng ký tài khoản, thêm phương tiện, duyệt vé tháng, check-in/check-out, thanh toán VNPAY đến xử lý mất thẻ và hỗ trợ trực tuyến.
              </p>
            </div>
          </Reveal>
        </section>

        <section className="tw-mx-auto tw-w-[min(1180px,calc(100%_-_32px))] tw-py-10">
          <Reveal className="tw-max-w-[720px]">
            <Kicker>Một quy trình liền mạch</Kicker>
            <h2 className="tw-m-0 tw-text-[clamp(1.85rem,3.5vw,3rem)] tw-font-black tw-leading-tight tw-text-slate-950">
              Ba lớp vận hành chính cần rõ trong trạng thái gửi xe.
            </h2>
            <p className="tw-m-0 tw-mt-4 tw-text-[1rem] tw-font-semibold tw-leading-7 tw-text-vm-slate-500">
              Trang giới thiệu nêu ngay được hệ thống giải quyết việc gì, ai dùng, và vì sao dữ liệu phải tin cậy.
            </p>
          </Reveal>
          <div className="tw-mt-8 tw-grid tw-grid-cols-3 tw-gap-6 max-[900px]:tw-grid-cols-1">
            {workflow.map((step, index) => (
              <Reveal delay={index * 80} key={step.label}>
                <article className="tw-group tw-h-full tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-6 tw-shadow-[0_18px_34px_rgba(15,23,42,0.06)] tw-transition tw-duration-300 hover:tw-rotate-[0.25deg] hover:tw-scale-[1.015] hover:tw-border-brand-200 hover:tw-shadow-[0_24px_46px_rgba(37,99,235,0.13)]">
                  <div className="tw-flex tw-items-center tw-justify-between">
                    <span className="tw-inline-flex tw-h-9 tw-min-w-9 tw-items-center tw-justify-center tw-rounded-full tw-bg-slate-950 tw-px-3 tw-text-[0.76rem] tw-font-black tw-text-white">{step.label}</span>
                    <i className={cn(step.icon, "tw-text-[1.45rem] tw-text-vm-primary tw-transition group-hover:tw-rotate-6 group-hover:tw-scale-110")} />
                  </div>
                  <h3 className="tw-m-0 tw-mt-5 tw-text-[1.05rem] tw-font-black tw-leading-6 tw-text-slate-950">{step.title}</h3>
                  <p className="tw-m-0 tw-mt-3 tw-text-[0.92rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-500">{step.text}</p>
                </article>
              </Reveal>
            ))}
          </div>
        </section>

        <section className="tw-mx-auto tw-grid tw-w-[min(1180px,calc(100%_-_32px))] tw-grid-cols-[minmax(0,0.86fr)_minmax(320px,0.76fr)] tw-items-center tw-gap-16 tw-py-12 max-[980px]:tw-grid-cols-1">
          <Reveal>
            <Kicker>Tại cổng ra vào</Kicker>
            <h2 className="tw-m-0 tw-text-[clamp(1.85rem,3.5vw,3rem)] tw-font-black tw-leading-tight tw-text-slate-950">Giảm nhầm lẫn trong từng lượt check-in và check-out.</h2>
            <p className="tw-m-0 tw-mt-4 tw-text-[1rem] tw-font-semibold tw-leading-7 tw-text-vm-slate-500">
              Nhân viên cổng cần nhìn được xe nào đang vào, biển số nào vừa được nhận diện, vé nào hợp lệ và trường hợp nào cần xác minh.
            </p>
            <ul className="tw-m-0 tw-mt-5 tw-grid tw-list-none tw-gap-3 tw-p-0">
              {["Ghi nhận biển số, ảnh xe, thời điểm và loại phương tiện.", "Đối chiếu vé tháng, khách vãng lai và trạng thái thanh toán.", "Cảnh báo nhanh khi mất thẻ, sai biển số hoặc phiên chưa đóng."].map((item) => (
                <li className="tw-flex tw-gap-3 tw-text-[0.94rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-700" key={item}>
                  <span className="tw-mt-2 tw-h-2 tw-w-2 tw-flex-none tw-rounded-full tw-bg-vm-primary" />
                  {item}
                </li>
              ))}
            </ul>
          </Reveal>
          <Reveal delay={80}>
            <div className="tw-group tw-relative tw-grid tw-min-h-[280px] tw-place-items-center tw-rounded-vm-lg tw-bg-slate-950 tw-p-8 tw-shadow-[0_24px_50px_rgba(15,23,42,0.2)] tw-transition tw-duration-500 hover:tw-shadow-[0_30px_64px_rgba(15,23,42,0.28)]">
              <div className="tw-absolute tw-inset-y-0 tw-left-1/2 tw-w-px tw-bg-white/70" />
              <div className="tw-relative tw-z-[1] tw-w-[min(100%,260px)] tw-rounded-vm-lg tw-bg-white tw-p-5 tw-shadow-[0_18px_40px_rgba(15,23,42,0.2)] tw-transition tw-duration-300 group-hover:tw-scale-[1.03]">
                <small className="tw-flex tw-justify-between tw-text-[0.76rem] tw-font-black tw-text-vm-slate-500">Check-in vừa ghi nhận <b className="tw-text-slate-950">08:42</b></small>
                <strong className="tw-mt-3 tw-block tw-rounded-vm-md tw-bg-brand-50 tw-px-3 tw-py-2 tw-text-center tw-text-[1.1rem] tw-font-black tw-text-slate-950">59A-482.16</strong>
                <div className="tw-mt-3 tw-grid tw-grid-cols-2 tw-gap-2 tw-text-[0.76rem] tw-font-black">
                  <span className="tw-rounded-vm-sm tw-bg-vm-slate-50 tw-px-2 tw-py-1.5 tw-text-vm-slate-700">Xe máy</span>
                  <span className="tw-rounded-vm-sm tw-bg-vm-slate-50 tw-px-2 tw-py-1.5 tw-text-vm-slate-700">Thẻ CX-2048</span>
                  <span className="tw-col-span-2 tw-rounded-vm-sm tw-bg-emerald-50 tw-px-2 tw-py-1.5 tw-text-center tw-text-emerald-700">Hợp lệ</span>
                </div>
              </div>
            </div>
          </Reveal>
        </section>

        <section className="tw-mx-auto tw-grid tw-w-[min(1180px,calc(100%_-_32px))] tw-grid-cols-[minmax(320px,0.76fr)_minmax(0,0.86fr)] tw-items-center tw-gap-16 tw-py-12 max-[980px]:tw-grid-cols-1">
          <Reveal>
            <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-[0_24px_46px_rgba(15,23,42,0.1)]">
              {operationRows.map(([time, text, status, tone], index) => (
                <div
                  className="tw-grid tw-min-h-14 tw-grid-cols-[54px_minmax(0,1fr)_auto] tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-text-[0.84rem] tw-transition tw-duration-300 hover:tw-translate-x-1 hover:tw-bg-brand-50/70 last:tw-border-b-0"
                  key={`${time}-${text}`}
                  style={{ "--row-delay": `${index * 70}ms` } as CSSProperties}
                >
                  <strong className="tw-font-black tw-text-slate-950">{time}</strong>
                  <span className="tw-font-semibold tw-text-vm-slate-500">{text}</span>
                  <StatusBadge tone={tone}>{status}</StatusBadge>
                </div>
              ))}
            </div>
          </Reveal>
          <Reveal delay={80}>
            <Kicker>Khách hàng và vé tháng</Kicker>
            <h2 className="tw-m-0 tw-text-[clamp(1.85rem,3.5vw,3rem)] tw-font-black tw-leading-tight tw-text-slate-950">Đăng ký, duyệt, gia hạn và tra cứu đều nằm trên một luồng.</h2>
            <p className="tw-m-0 tw-mt-4 tw-text-[1rem] tw-font-semibold tw-leading-7 tw-text-vm-slate-500">
              Khách hàng tự cập nhật xe, chọn gói gửi phù hợp, gửi yêu cầu và nhận trạng thái duyệt thay vì phải trao đổi rời rạc.
            </p>
            <ul className="tw-m-0 tw-mt-5 tw-grid tw-list-none tw-gap-3 tw-p-0">
              {["Hồ sơ khách hàng, phương tiện và gói vé liên kết trực tiếp.", "Thông báo realtime khi vé được duyệt hoặc sắp hết hạn.", "Lịch sử gửi xe giúp khách tự kiểm tra và giảm tải hỗ trợ."].map((item) => (
                <li className="tw-flex tw-gap-3 tw-text-[0.94rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-700" key={item}>
                  <span className="tw-mt-2 tw-h-2 tw-w-2 tw-flex-none tw-rounded-full tw-bg-vm-primary" />
                  {item}
                </li>
              ))}
            </ul>
          </Reveal>
        </section>

        <section className="tw-mx-auto tw-w-[min(1180px,calc(100%_-_32px))] tw-py-12">
          <Reveal>
            <div className="tw-relative tw-grid tw-grid-cols-[minmax(0,1fr)_auto] tw-items-center tw-gap-8 tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-brand-100 tw-bg-gradient-to-br tw-from-white tw-to-brand-50/70 tw-p-8 tw-shadow-[0_18px_36px_rgba(37,99,235,0.08)] tw-transition tw-duration-500 hover:-tw-translate-y-0.5 hover:tw-border-brand-200 hover:tw-shadow-[0_24px_50px_rgba(37,99,235,0.14)] max-[780px]:tw-grid-cols-1">
              <div>
                <Kicker>Sẵn sàng triển khai</Kicker>
                <h2 className="tw-m-0 tw-max-w-[760px] tw-text-[clamp(1.75rem,3vw,2.55rem)] tw-font-black tw-leading-tight tw-text-slate-950">
                  Bắt đầu từ một bãi xe, mở rộng thành hệ thống vận hành chuẩn hóa.
                </h2>
                <p className="tw-m-0 tw-mt-4 tw-max-w-[780px] tw-text-[1rem] tw-font-semibold tw-leading-7 tw-text-vm-slate-500">
                  Hướng thiết kế ưu tiên thông điệp rõ, giao diện tin cậy và đủ dữ kiện để khách hàng hiểu CoParking làm tốt phần nào.
                </p>
              </div>
              <div className="tw-flex tw-min-w-[220px] tw-flex-col tw-gap-3 max-[780px]:tw-min-w-0">
                <PrimaryLink to="/pricing">Xem bảng giá</PrimaryLink>
                <SecondaryLink to="/contact">Liên hệ tư vấn</SecondaryLink>
              </div>
            </div>
          </Reveal>
        </section>

        <section className="tw-mx-auto tw-grid tw-w-[min(1180px,calc(100%_-_32px))] tw-grid-cols-2 tw-gap-4 tw-pb-16 max-[780px]:tw-grid-cols-1">
          {faqItems.map(([question, answer], index) => (
            <Reveal delay={index * 45} key={question}>
              <article className="tw-h-full tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-[0_14px_30px_rgba(15,23,42,0.05)] tw-transition tw-duration-300 hover:tw-translate-x-1 hover:tw-border-l-4 hover:tw-border-brand-500 hover:tw-shadow-[0_18px_38px_rgba(37,99,235,0.11)]">
                <h3 className="tw-m-0 tw-text-[1rem] tw-font-black tw-text-slate-950">{question}</h3>
                <p className="tw-m-0 tw-mt-2 tw-text-[0.9rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-500">{answer}</p>
              </article>
            </Reveal>
          ))}
        </section>
      </main>
      <PublicFooter />
    </ClientPage>
  );
}
