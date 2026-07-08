import { useMemo, useState } from "react";

import { Badge, Button, Card, EntityAvatar, InfoBanner } from "@/components/ui";
import { openSupportCenterConversation } from "@/features/support";
import { cn } from "@/lib/cn";

type ApprovalStatus = "APPROVED" | "PENDING" | "VIP";
type CustomerStatus = "ACTIVE" | "INACTIVE";

type Customer = {
  address: string;
  approval: ApprovalStatus;
  code: string;
  email: string;
  initials: string;
  name: string;
  phone: string;
  status: CustomerStatus;
  type: "REGISTERED" | "VIP";
};

type LinkedAsset = {
  icon: string;
  meta: string;
  status: string;
  statusTone: "primary" | "success" | "neutral";
  title: string;
};

type Activity = {
  color: string;
  date: string;
  description: string;
  title: string;
  user: string;
};

const customers: Customer[] = [
  {
    address: "123 Đường ABC, P. An Lợi, TP. Thủ Đức, TP. HCM",
    approval: "APPROVED",
    code: "CUS-000456",
    email: "binh.tran@example.com",
    initials: "BT",
    name: "Trần Thị Bình",
    phone: "0902 345 678",
    status: "ACTIVE",
    type: "VIP",
  },
  {
    address: "Quận 7, TP. Hồ Chí Minh",
    approval: "APPROVED",
    code: "CUS-000389",
    email: "an.nguyen@example.com",
    initials: "NA",
    name: "Nguyễn Văn An",
    phone: "0901 234 567",
    status: "ACTIVE",
    type: "REGISTERED",
  },
  {
    address: "Cầu Giấy, Hà Nội",
    approval: "PENDING",
    code: "CUS-000312",
    email: "cuong.le@example.com",
    initials: "LC",
    name: "Lê Minh Cường",
    phone: "0911 222 333",
    status: "ACTIVE",
    type: "REGISTERED",
  },
  {
    address: "Biên Hòa, Đồng Nai",
    approval: "APPROVED",
    code: "CUS-000298",
    email: "dung.pham@example.com",
    initials: "PD",
    name: "Phạm Hoàng Dũng",
    phone: "0933 567 890",
    status: "ACTIVE",
    type: "REGISTERED",
  },
  {
    address: "Quận 1, TP. Hồ Chí Minh",
    approval: "VIP",
    code: "CUS-000277",
    email: "em.hoang@example.com",
    initials: "HE",
    name: "Hoàng Thị Em",
    phone: "0988 112 233",
    status: "ACTIVE",
    type: "VIP",
  },
  {
    address: "Tân Bình, TP. Hồ Chí Minh",
    approval: "PENDING",
    code: "CUS-000266",
    email: "huy.do@example.com",
    initials: "DQ",
    name: "Đỗ Quang Huy",
    phone: "0909 332 211",
    status: "ACTIVE",
    type: "REGISTERED",
  },
  {
    address: "Đống Đa, Hà Nội",
    approval: "APPROVED",
    code: "CUS-000245",
    email: "mai.nguyen@example.com",
    initials: "NT",
    name: "Nguyễn Thị Mai",
    phone: "0918 224 466",
    status: "ACTIVE",
    type: "REGISTERED",
  },
];

const ticketAssets: LinkedAsset[] = [
  { icon: "far fa-calendar-check", meta: "Gói tháng hết hạn 28/07/2026", status: "Hiệu lực", statusTone: "primary", title: "Vé tháng" },
  { icon: "far fa-credit-card", meta: "Trạng thái", status: "IN_USE", statusTone: "primary", title: "Thẻ V000456" },
];

const vehicleAssets: LinkedAsset[] = [
  { icon: "fas fa-car", meta: "Ô tô · Toyota · Trắng", status: "Mặc định", statusTone: "success", title: "30A-987.65" },
  { icon: "fas fa-motorcycle", meta: "Xe máy · Honda · Đen", status: "Đang dùng", statusTone: "neutral", title: "59C1-223.45" },
];

const activities: Activity[] = [
  {
    color: "#2563EB",
    date: "28/05/2026 09:15",
    description: "Thêm xe Honda 59C1-223.45",
    title: "Cập nhật phương tiện",
    user: "Nguyễn Văn Admin",
  },
  {
    color: "#10B981",
    date: "26/05/2026 14:08",
    description: "Gia hạn gói Tháng đến 28/07/2026",
    title: "Gia hạn vé tháng",
    user: "Nguyễn Văn Admin",
  },
  {
    color: "#F97316",
    date: "18/05/2026 10:30",
    description: "Hồ sơ khách hàng được duyệt",
    title: "Duyệt hồ sơ",
    user: "Nguyễn Văn Admin",
  },
  {
    color: "#2563EB",
    date: "10/05/2026 16:45",
    description: "Cập nhật số điện thoại",
    title: "Cập nhật thông tin",
    user: "Nguyễn Văn Admin",
  },
];

const segmentTabs = [
  { label: "Tất cả", value: "all" },
  { label: "Đã duyệt", value: "approved" },
  { label: "Chờ duyệt", value: "pending" },
  { label: "VIP", value: "vip" },
] as const;

function approvalBadgeTone(status: ApprovalStatus) {
  if (status === "PENDING") return "warning";
  if (status === "VIP") return "primary";
  return "success";
}

function CustomerMetric({
  icon,
  iconClassName,
  label,
  value,
}: {
  icon: string;
  iconClassName: string;
  label: string;
  value: string;
}) {
  return (
    <Card className="tw-min-h-[88px] tw-p-4">
      <div className="tw-flex tw-items-center tw-gap-4">
        <span className={cn("tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-full tw-text-[1.25rem]", iconClassName)}>
          <i className={icon} />
        </span>
        <div className="tw-min-w-0">
          <p className="tw-m-0 tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-700">{label}</p>
          <strong className="tw-mt-1 tw-block tw-text-[1.75rem] tw-font-extrabold tw-leading-none tw-text-vm-slate-900">{value}</strong>
        </div>
      </div>
    </Card>
  );
}

function CustomerListItem({
  customer,
  onContact,
  selected,
  onSelect,
}: {
  customer: Customer;
  onContact: () => void;
  onSelect: () => void;
  selected: boolean;
}) {
  return (
    <article
      className={cn(
        "tw-flex tw-w-full tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-bg-white tw-p-1.5 tw-transition",
        selected
          ? "tw-border-vm-primary tw-bg-brand-50 tw-shadow-[inset_3px_0_0_#2563EB,0_8px_18px_rgba(37,99,235,0.08)]"
          : "tw-border-transparent hover:tw-border-brand-100 hover:tw-bg-vm-slate-25",
      )}
    >
      <button type="button" className="tw-flex tw-min-w-0 tw-flex-1 tw-items-center tw-gap-3 tw-border-0 tw-bg-transparent tw-px-1 tw-py-1 tw-text-left" onClick={onSelect}>
        <EntityAvatar initials={customer.initials} size="sm" />
        <span className="tw-min-w-0 tw-flex-1">
          <strong className="tw-block tw-truncate tw-text-[0.83rem] tw-font-extrabold tw-text-vm-slate-900">{customer.name}</strong>
          <small className="tw-block tw-truncate tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500">{customer.code}</small>
        </span>
        <Badge tone={approvalBadgeTone(customer.approval)} className="tw-rounded-full tw-px-2 tw-text-[0.62rem]">
          {customer.approval}
        </Badge>
      </button>
      <button
        type="button"
        className="tw-inline-flex tw-h-8 tw-w-8 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-100 tw-bg-white tw-text-vm-primary hover:tw-bg-brand-50"
        onClick={onContact}
        aria-label={`Liên hệ ${customer.name}`}
        title="Liên hệ"
      >
        <i className="far fa-comment-dots tw-text-[0.9rem]" />
      </button>
    </article>
  );
}

function QuickInfoRow({ icon, label }: { icon: string; label: string }) {
  return (
    <div className="tw-flex tw-items-center tw-gap-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-700">
      <i className={cn(icon, "tw-w-4 tw-text-center tw-text-vm-slate-500")} />
      <span>{label}</span>
    </div>
  );
}

function LinkedAssetCard({ asset }: { asset: LinkedAsset }) {
  return (
    <article className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
      <span className="tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-full tw-bg-brand-100 tw-text-[1.25rem] tw-text-vm-primary">
        <i className={asset.icon} />
      </span>
      <h3 className="tw-m-0 tw-mt-3 tw-text-[0.88rem] tw-font-extrabold tw-text-vm-slate-900">{asset.title}</h3>
      <p className="tw-m-0 tw-mt-1 tw-min-h-[34px] tw-text-[0.74rem] tw-font-semibold tw-leading-snug tw-text-vm-slate-500">{asset.meta}</p>
      <Badge tone={asset.statusTone} className="tw-mt-3 tw-rounded-full tw-px-3">
        {asset.status}
      </Badge>
    </article>
  );
}

export function CustomerListPage() {
  const [activeSegment, setActiveSegment] = useState<(typeof segmentTabs)[number]["value"]>("all");
  const [selectedCode, setSelectedCode] = useState("CUS-000456");

  const filteredCustomers = useMemo(() => {
    if (activeSegment === "approved") return customers.filter((customer) => customer.approval === "APPROVED");
    if (activeSegment === "pending") return customers.filter((customer) => customer.approval === "PENDING");
    if (activeSegment === "vip") return customers.filter((customer) => customer.type === "VIP");
    return customers;
  }, [activeSegment]);

  const selectedCustomer = customers.find((customer) => customer.code === selectedCode) ?? customers[0];

  return (
    <div className="tw-px-4 tw-py-4 lg:tw-px-5">
      <section className="tw-mx-auto tw-min-h-[calc(100vh-104px)] tw-w-[min(100%,1500px)] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-vm-card">
        <div className="tw-mb-5 tw-flex tw-items-center tw-justify-between tw-gap-4">
          <div className="tw-flex tw-min-w-0 tw-items-center tw-gap-4">
            <h1 className="tw-m-0 tw-text-vm-page-title tw-tracking-[-0.03em] tw-text-vm-slate-900">Khách hàng</h1>
            <a className="tw-inline-flex tw-items-center tw-gap-2 tw-text-[0.86rem] tw-font-extrabold tw-text-vm-primary hover:tw-text-vm-primary-hover hover:tw-no-underline" href="#customer-help">
              <i className="far fa-question-circle tw-text-[1rem]" />
              Hướng dẫn & Trợ giúp
            </a>
          </div>
          <div className="tw-flex tw-flex-shrink-0 tw-items-center tw-gap-3">
            <Button size="lg" variant="primary">
              <i className="fas fa-check" />
              Duyệt đăng ký
            </Button>
            <Button size="lg" variant="secondary">
              <i className="fas fa-download" />
              Xuất dữ liệu
              <i className="fas fa-chevron-down tw-text-[0.72rem]" />
            </Button>
          </div>
        </div>

        <InfoBanner
          tone="warning"
          title="Chờ duyệt đăng ký mới"
          description="Hiện có 84 đăng ký khách hàng đang chờ duyệt."
          icon={<i className="fas fa-exclamation-triangle" />}
          action={
            <button type="button" className="tw-border-0 tw-bg-transparent tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-700 hover:tw-text-vm-primary">
              Xem danh sách <i className="fas fa-chevron-right tw-ml-2 tw-text-[0.68rem]" />
            </button>
          }
        />

        <div className="tw-mt-4 tw-grid tw-grid-cols-4 tw-gap-4 max-[1180px]:tw-grid-cols-2">
          <CustomerMetric icon="far fa-clock" iconClassName="tw-bg-amber-50 tw-text-amber-500" label="Chờ duyệt" value="84" />
          <CustomerMetric icon="fas fa-crown" iconClassName="tw-bg-violet-50 tw-text-violet-600" label="VIP" value="126" />
          <CustomerMetric icon="fas fa-car" iconClassName="tw-bg-brand-100 tw-text-vm-primary" label="Xe liên kết" value="1.978" />
          <CustomerMetric icon="far fa-credit-card" iconClassName="tw-bg-green-50 tw-text-green-600" label="Vé đang hiệu lực" value="856" />
        </div>

        <div className="tw-mt-5 tw-grid tw-grid-cols-[340px_minmax(0,1fr)_260px] tw-gap-4 max-[1280px]:tw-grid-cols-[320px_minmax(0,1fr)] max-[960px]:tw-grid-cols-1">
          <Card className="tw-flex tw-h-full tw-min-h-0 tw-flex-col tw-overflow-hidden">
            <div className="tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-py-4">
              <h2 className="tw-m-0 tw-text-[0.95rem] tw-font-extrabold tw-text-vm-slate-900">Danh sách khách hàng</h2>
              <div className="tw-mt-3 tw-flex tw-h-[38px] tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3">
                <i className="fas fa-search tw-text-[0.82rem] tw-text-vm-slate-500" />
                <span className="tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Tìm tên, mã KH, biển số...</span>
              </div>
              <div className="tw-mt-3 tw-flex tw-flex-wrap tw-gap-2">
                {segmentTabs.map((tab) => (
                  <button
                    key={tab.value}
                    type="button"
                    className={cn(
                      "tw-h-8 tw-rounded-vm-md tw-border tw-border-solid tw-px-3 tw-text-[0.72rem] tw-font-extrabold tw-transition",
                      activeSegment === tab.value
                        ? "tw-border-vm-primary tw-bg-white tw-text-vm-primary tw-shadow-[inset_0_-2px_0_#2563EB]"
                        : "tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-text-vm-slate-700 hover:tw-border-brand-100 hover:tw-text-vm-primary",
                    )}
                    onClick={() => setActiveSegment(tab.value)}
                  >
                    {tab.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="tw-grid tw-min-h-0 tw-flex-1 tw-content-start tw-gap-1 tw-overflow-y-auto tw-p-3 tw-pr-2 tw-[scrollbar-width:none] tw-[-ms-overflow-style:none] [&::-webkit-scrollbar]:tw-hidden">
              {filteredCustomers.map((customer) => (
                <CustomerListItem
                  key={customer.code}
                  customer={customer}
                  onContact={() => openSupportCenterConversation({ participantId: customer.code, participantName: customer.name, participantType: "customer" })}
                  selected={customer.code === selectedCustomer.code}
                  onSelect={() => setSelectedCode(customer.code)}
                />
              ))}
            </div>

            <div className="tw-flex-shrink-0 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-px-3 tw-py-2.5">
              <Button className="tw-h-9 tw-w-full tw-text-[0.82rem]" variant="secondary">
                Xem tất cả khách hàng
              </Button>
            </div>
          </Card>

          <div className="tw-grid tw-gap-4">
            <Card className="tw-p-4">
              <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Hồ sơ nhanh</h2>
              <div className="tw-mt-3 tw-grid tw-grid-cols-[56px_minmax(0,1fr)] tw-gap-4">
                <EntityAvatar initials={selectedCustomer.initials} size="lg" />
                <div className="tw-min-w-0">
                  <h3 className="tw-m-0 tw-text-[1.14rem] tw-font-extrabold tw-leading-tight tw-text-vm-slate-900">{selectedCustomer.name}</h3>
                  <p className="tw-m-0 tw-mt-0.5 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">{selectedCustomer.code}</p>
                  <div className="tw-mt-2 tw-flex tw-flex-wrap tw-gap-1.5">
                    <Badge tone="primary" className="tw-rounded-full tw-px-2.5 tw-text-[0.66rem]">VIP</Badge>
                    <Badge tone={approvalBadgeTone(selectedCustomer.approval)} className="tw-rounded-full tw-px-2.5 tw-text-[0.66rem]">
                      {selectedCustomer.approval}
                    </Badge>
                    <Badge tone="success" className="tw-rounded-full tw-px-2.5 tw-text-[0.66rem]">{selectedCustomer.status}</Badge>
                  </div>
                </div>
              </div>

              <div className="tw-mt-3 tw-grid tw-gap-2">
                <QuickInfoRow icon="far fa-envelope" label={selectedCustomer.email} />
                <QuickInfoRow icon="fas fa-phone" label={selectedCustomer.phone} />
                <QuickInfoRow icon="fas fa-map-marker-alt" label={selectedCustomer.address} />
              </div>

              <div className="tw-mt-3 tw-flex tw-flex-wrap tw-gap-2">
                <Button
                  size="sm"
                  variant="primary"
                  onClick={() => openSupportCenterConversation({ participantId: selectedCustomer.code, participantName: selectedCustomer.name, participantType: "customer" })}
                >
                  <i className="far fa-comment-dots" />
                  Liên hệ
                </Button>
                <Button size="sm" variant="secondary">
                  <i className="fas fa-pen" />
                  Cập nhật
                </Button>
                <Button size="sm" variant="secondary">
                  <i className="fas fa-check" />
                  Duyệt
                </Button>
                <Button size="sm" variant="danger">
                  <i className="fas fa-pause" />
                  Tạm ngưng
                </Button>
              </div>
            </Card>

            <Card className="tw-p-5">
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-4">
                <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Tài sản liên kết</h2>
              </div>

              <div className="tw-mt-4 tw-grid tw-gap-4">
                <section>
                  <div className="tw-mb-2 tw-flex tw-items-center tw-gap-2">
                    <span className="tw-h-2 tw-w-2 tw-rounded-full tw-bg-vm-primary" />
                    <h3 className="tw-m-0 tw-text-[0.82rem] tw-font-extrabold tw-uppercase tw-tracking-[0.04em] tw-text-vm-slate-500">Vé</h3>
                  </div>
                  <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[1180px]:tw-grid-cols-2 max-[620px]:tw-grid-cols-1">
                    {ticketAssets.map((asset) => (
                      <LinkedAssetCard asset={asset} key={asset.title} />
                    ))}
                  </div>
                </section>

                <section>
                  <div className="tw-mb-2 tw-flex tw-items-center tw-gap-2">
                    <span className="tw-h-2 tw-w-2 tw-rounded-full tw-bg-green-500" />
                    <h3 className="tw-m-0 tw-text-[0.82rem] tw-font-extrabold tw-uppercase tw-tracking-[0.04em] tw-text-vm-slate-500">Xe</h3>
                  </div>
                  <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[1180px]:tw-grid-cols-2 max-[620px]:tw-grid-cols-1">
                    {vehicleAssets.map((asset) => (
                      <LinkedAssetCard asset={asset} key={asset.title} />
                    ))}
                  </div>
                </section>
              </div>

              <Button className="tw-mt-4 tw-w-full" variant="secondary">
                Xem tất cả tài sản
              </Button>
            </Card>
          </div>

          <Card className="tw-p-5 max-[1280px]:tw-col-span-2 max-[960px]:tw-col-span-1">
            <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Hoạt động gần đây</h2>
            <div className="tw-mt-5 tw-grid tw-gap-0">
              {activities.map((activity, index) => (
                <div className="tw-grid tw-grid-cols-[18px_minmax(0,1fr)] tw-gap-3" key={`${activity.date}-${activity.title}`}>
                  <div className="tw-relative tw-flex tw-justify-center">
                    <span className="tw-mt-1.5 tw-h-2.5 tw-w-2.5 tw-rounded-full" style={{ backgroundColor: activity.color }} />
                    {index < activities.length - 1 ? <span className="tw-absolute tw-bottom-0 tw-top-5 tw-w-px tw-bg-vm-slate-100" /> : null}
                  </div>
                  <div className="tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-4 tw-pt-0 last:tw-border-b-0">
                    <p className="tw-m-0 tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">{activity.date}</p>
                    <h3 className="tw-m-0 tw-mt-2 tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-900">{activity.title}</h3>
                    <p className="tw-m-0 tw-mt-1 tw-text-[0.76rem] tw-font-semibold tw-leading-snug tw-text-vm-slate-700">{activity.description}</p>
                    <p className="tw-m-0 tw-mt-1 tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">{activity.user}</p>
                  </div>
                </div>
              ))}
            </div>
            <Button className="tw-mt-5 tw-w-full" variant="secondary">
              Xem tất cả hoạt động
            </Button>
          </Card>
        </div>

        <InfoBanner
          className="tw-mt-4"
          tone="info"
          title="Click khách hàng để mở workspace; chi tiết sâu mở modal/drawer riêng."
          description="Trang mặc định không mở drawer để tránh quá nhiều chi tiết trên một màn hình."
          icon={<i className="fas fa-info-circle" />}
        />
      </section>
    </div>
  );
}
