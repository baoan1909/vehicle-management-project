import { useMemo, useState } from "react";

import { Badge, Button, Card, DateRangeInput, Drawer, PaginationFooter, SelectMenu } from "@/components/ui";
import { cn } from "@/lib/cn";

type SubscriptionStatus = "pending_review" | "waiting_payment" | "waiting_card" | "active";
type PackageType = "basic" | "vip";
type ViewMode = "pipeline" | "table";

type SubscriptionRequest = {
  amount: string;
  cardLabel: string;
  customerEmail: string;
  customerName: string;
  customerPhone: string;
  date: string;
  id: string;
  invoiceCode: string;
  location: string;
  packageLabel: string;
  packageType: PackageType;
  plate: string;
  status: SubscriptionStatus;
  time: string;
  vehicleModel: string;
};

type AvailableCard = {
  code: string;
  label: string;
  state: "available" | "reserved";
};

const statusMeta: Record<SubscriptionStatus, { accent: string; count: number; delta: string; icon: string; label: string; ring: string }> = {
  pending_review: { accent: "tw-bg-blue-50 tw-text-vm-primary", count: 18, delta: "+2 hôm nay", icon: "far fa-calendar-check", label: "Chờ duyệt", ring: "tw-border-t-vm-primary" },
  waiting_payment: { accent: "tw-bg-orange-50 tw-text-orange-500", count: 12, delta: "+1 hôm nay", icon: "far fa-credit-card", label: "Chờ thanh toán", ring: "tw-border-t-orange-400" },
  waiting_card: { accent: "tw-bg-purple-50 tw-text-purple-600", count: 9, delta: "+1 hôm nay", icon: "fas fa-box-open", label: "Chờ gán thẻ", ring: "tw-border-t-purple-500" },
  active: { accent: "tw-bg-green-50 tw-text-green-600", count: 156, delta: "+8 hôm nay", icon: "far fa-check-circle", label: "Đang hoạt động", ring: "tw-border-t-green-500" },
};

const rejectedMetric = { accent: "tw-bg-red-50 tw-text-red-500", count: 7, delta: "+0 hôm nay", icon: "fas fa-tag", label: "Bị từ chối" };

const requests: SubscriptionRequest[] = [
  { id: "ĐƠN-000245", customerName: "Nguyễn Văn An", customerPhone: "0987 654 321", customerEmail: "nguyenvanan@gmail.com", plate: "30A-123.45", vehicleModel: "Toyota Camry", date: "01/06/2024", time: "10:23", location: "Bãi xe Mỹ Đình", packageLabel: "Gói cơ bản", packageType: "basic", status: "pending_review", invoiceCode: "HD-000245", amount: "1.200.000 VNĐ", cardLabel: "Chọn thẻ..." },
  { id: "ĐƠN-000246", customerName: "Trần Thị Bình", customerPhone: "0901 224 888", customerEmail: "binh.tran@mail.com", plate: "51G-678.90", vehicleModel: "Honda City", date: "05/06/2024", time: "09:48", location: "Bãi xe Mỹ Đình", packageLabel: "Gói VIP", packageType: "vip", status: "pending_review", invoiceCode: "HD-000246", amount: "2.400.000 VNĐ", cardLabel: "Chọn thẻ..." },
  { id: "ĐƠN-000247", customerName: "Lê Hoàng Nam", customerPhone: "0918 112 334", customerEmail: "nam.le@mail.com", plate: "29A-456.78", vehicleModel: "Mazda CX5", date: "03/06/2024", time: "09:15", location: "Bãi xe Times City", packageLabel: "Gói VIP", packageType: "vip", status: "pending_review", invoiceCode: "HD-000247", amount: "2.400.000 VNĐ", cardLabel: "Chọn thẻ..." },
  { id: "ĐƠN-000232", customerName: "Phạm Quang Huy", customerPhone: "0936 882 991", customerEmail: "huy.pham@mail.com", plate: "30E-111.22", vehicleModel: "Kia Seltos", date: "01/06/2024", time: "11:05", location: "Bãi xe Mỹ Đình", packageLabel: "Gói cơ bản", packageType: "basic", status: "waiting_payment", invoiceCode: "HD-000232", amount: "1.200.000 VNĐ", cardLabel: "Chọn thẻ..." },
  { id: "ĐƠN-000233", customerName: "Vũ Thu Trang", customerPhone: "0904 554 882", customerEmail: "trang.vu@mail.com", plate: "88A-222.33", vehicleModel: "Hyundai Accent", date: "02/06/2024", time: "10:40", location: "Bãi xe Royal City", packageLabel: "Gói VIP", packageType: "vip", status: "waiting_payment", invoiceCode: "HD-000233", amount: "2.400.000 VNĐ", cardLabel: "Chọn thẻ..." },
  { id: "ĐƠN-000218", customerName: "Hoàng Quốc Việt", customerPhone: "0977 223 441", customerEmail: "viet.hoang@mail.com", plate: "30F-555.66", vehicleModel: "Ford Territory", date: "28/05/2024", time: "11:20", location: "Bãi xe Mỹ Đình", packageLabel: "Gói VIP", packageType: "vip", status: "waiting_card", invoiceCode: "HD-000218", amount: "2.400.000 VNĐ", cardLabel: "Thẻ đại giữ chỗ" },
  { id: "ĐƠN-000219", customerName: "Ngô Lan Anh", customerPhone: "0912 778 900", customerEmail: "anh.ngo@mail.com", plate: "51H-666.77", vehicleModel: "VinFast VF8", date: "29/05/2024", time: "10:55", location: "Bãi xe Times City", packageLabel: "Gói cơ bản", packageType: "basic", status: "waiting_card", invoiceCode: "HD-000219", amount: "1.200.000 VNĐ", cardLabel: "Thẻ đại giữ chỗ" },
  { id: "ĐƠN-000190", customerName: "Nguyễn Hoài Phương", customerPhone: "0986 100 222", customerEmail: "phuong.nguyen@mail.com", plate: "30A-999.99", vehicleModel: "Mercedes C200", date: "01/05/2024", time: "08:30", location: "Bãi xe Mỹ Đình", packageLabel: "Gói cơ bản", packageType: "basic", status: "active", invoiceCode: "HD-000190", amount: "1.200.000 VNĐ", cardLabel: "ACTIVE" },
  { id: "ĐƠN-000191", customerName: "Phan Thanh Tùng", customerPhone: "0939 878 111", customerEmail: "tung.phan@mail.com", plate: "51K-888.88", vehicleModel: "Toyota Vios", date: "01/05/2024", time: "08:15", location: "Bãi xe Times City", packageLabel: "Gói VIP", packageType: "vip", status: "active", invoiceCode: "HD-000191", amount: "2.400.000 VNĐ", cardLabel: "ACTIVE" },
];

const availableCards: AvailableCard[] = [
  { code: "CARD-000128", label: "Thẻ đại giữ chỗ", state: "reserved" },
  { code: "CARD-000129", label: "Thẻ khả dụng", state: "available" },
  { code: "CARD-000130", label: "Thẻ khả dụng", state: "available" },
  { code: "CARD-000131", label: "Thẻ khả dụng", state: "available" },
];

const statusOptions = [
  { label: "Trạng thái", value: "all" },
  { label: "Chờ duyệt", value: "pending_review" },
  { label: "Chờ thanh toán", value: "waiting_payment" },
  { label: "Chờ gán thẻ", value: "waiting_card" },
  { label: "Đang hoạt động", value: "active" },
];

const packageOptions = [
  { label: "Gói tháng", value: "all" },
  { label: "Gói cơ bản", value: "basic" },
  { label: "Gói VIP", value: "vip" },
];

const siteOptions = [
  { label: "Bãi xe", value: "all" },
  { label: "Mỹ Đình", value: "Bãi xe Mỹ Đình" },
  { label: "Times City", value: "Bãi xe Times City" },
  { label: "Royal City", value: "Bãi xe Royal City" },
];

function packageTone(type: PackageType) {
  return type === "vip" ? "success" : "primary";
}

function statusTagClass(status: SubscriptionStatus) {
  if (status === "waiting_payment") return "tw-bg-orange-50 tw-text-orange-600";
  if (status === "waiting_card") return "tw-bg-purple-50 tw-text-purple-600";
  if (status === "active") return "tw-bg-green-50 tw-text-green-600";
  return "tw-bg-blue-50 tw-text-vm-primary";
}

function statusLabel(status: SubscriptionStatus) {
  return statusMeta[status].label;
}

function SummaryCard({ accent, count, delta, icon, label }: { accent: string; count: number; delta: string; icon: string; label: string }) {
  return (
    <Card className="tw-flex tw-min-h-[86px] tw-items-center tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-p-4">
      <span className={cn("tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-vm-md tw-text-[1rem]", accent)}>
        <i className={icon} />
      </span>
      <span className="tw-min-w-0">
        <span className="tw-block tw-text-[0.74rem] tw-font-bold tw-text-vm-slate-500">{label}</span>
        <strong className="tw-mt-1 tw-block tw-text-[1.35rem] tw-font-black tw-leading-none tw-text-vm-slate-900">{count}</strong>
        <small className="tw-mt-1.5 tw-block tw-text-[0.72rem] tw-font-bold tw-text-green-600">{delta}</small>
      </span>
    </Card>
  );
}

function RequestCard({ active, item, onSelect }: { active: boolean; item: SubscriptionRequest; onSelect: (item: SubscriptionRequest) => void }) {
  return (
    <button
      type="button"
      className={cn(
        "tw-grid tw-w-full tw-gap-2 tw-rounded-vm-lg tw-border tw-border-solid tw-bg-white tw-p-3 tw-text-left tw-shadow-[0_8px_20px_rgba(15,23,42,0.04)] tw-transition hover:tw-border-brand-200 hover:tw-shadow-[0_14px_28px_rgba(37,99,235,0.12)]",
        active ? "tw-border-vm-primary tw-shadow-[0_14px_28px_rgba(37,99,235,0.12)]" : "tw-border-vm-slate-100",
      )}
      onClick={() => onSelect(item)}
    >
      <span className="tw-flex tw-items-center tw-justify-between tw-gap-3">
        <strong className="tw-text-[0.82rem] tw-font-black tw-text-vm-slate-900">{item.id}</strong>
        <span className="tw-text-[0.72rem] tw-font-bold tw-text-vm-slate-500">{item.time}</span>
      </span>
      <span className="tw-grid tw-gap-1.5 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-700">
        <span><i className="far fa-user tw-mr-2 tw-text-vm-slate-500" />{item.customerName}</span>
        <span><i className="fas fa-car tw-mr-2 tw-text-vm-slate-500" />{item.plate}</span>
        <span><i className="far fa-calendar-alt tw-mr-2 tw-text-vm-slate-500" />{item.date}</span>
      </span>
      <span className="tw-flex tw-items-center tw-justify-between tw-gap-2">
        <Badge tone={packageTone(item.packageType)} className="tw-rounded-vm-sm tw-px-2.5">{item.packageLabel}</Badge>
        <span className={cn("tw-rounded-vm-sm tw-px-2 tw-py-1 tw-text-[0.66rem] tw-font-black", statusTagClass(item.status))}>{item.cardLabel}</span>
      </span>
      <span className="tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500"><i className="fas fa-map-marker-alt tw-mr-2" />{item.location}</span>
    </button>
  );
}

function PipelineColumn({ activeId, items, status, onSelect }: { activeId?: string; items: SubscriptionRequest[]; status: SubscriptionStatus; onSelect: (item: SubscriptionRequest) => void }) {
  const meta = statusMeta[status];

  return (
    <section className={cn("tw-min-w-[245px] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-border-t-4 tw-bg-vm-slate-25 tw-p-3", meta.ring)}>
      <header className="tw-mb-3 tw-flex tw-items-center tw-justify-between">
        <h2 className="tw-m-0 tw-text-[0.95rem] tw-font-black tw-text-vm-slate-900">{meta.label}</h2>
        <span className="tw-text-[0.82rem] tw-font-black tw-text-vm-slate-700">{meta.count}</span>
      </header>
      <div className="tw-grid tw-gap-2.5">
        {items.map((item) => <RequestCard key={item.id} active={activeId === item.id} item={item} onSelect={onSelect} />)}
      </div>
      <button type="button" className="tw-mt-4 tw-w-full tw-border-0 tw-bg-transparent tw-py-2 tw-text-[0.82rem] tw-font-black tw-text-vm-primary hover:tw-underline">
        Xem tất cả ({meta.count})
      </button>
    </section>
  );
}

function SubscriptionTableView({ activeId, items, onSelect }: { activeId?: string; items: SubscriptionRequest[]; onSelect: (item: SubscriptionRequest) => void }) {
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
  const currentPage = Math.min(page, totalPages);
  const startOffset = items.length === 0 ? 0 : (currentPage - 1) * pageSize;
  const pagedItems = items.slice(startOffset, startOffset + pageSize);
  const startIndex = items.length === 0 ? 0 : startOffset + 1;
  const endIndex = Math.min(startOffset + pageSize, items.length);

  return (
    <section className="tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white">
      <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-py-3 max-[720px]:tw-flex-col max-[720px]:tw-items-stretch">
        <div>
          <h2 className="tw-m-0 tw-text-[1rem] tw-font-black tw-text-vm-slate-900">Danh sách đăng ký vé tháng</h2>
          <p className="tw-m-0 tw-mt-1 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">Theo dõi trạng thái duyệt, thanh toán và gán thẻ trên một bảng.</p>
        </div>
        <span className="tw-inline-flex tw-h-8 tw-items-center tw-rounded-full tw-bg-brand-50 tw-px-3 tw-text-[0.78rem] tw-font-black tw-text-vm-primary">
          {items.length} hồ sơ
        </span>
      </div>

      <div className="tw-overflow-x-auto">
        <table className="table tw-m-0 tw-min-w-[1120px] [&_td]:tw-border-0 [&_td]:tw-border-t [&_td]:tw-border-solid [&_td]:tw-border-vm-slate-100 [&_td]:tw-px-4 [&_td]:tw-py-3 [&_td]:tw-align-middle [&_thead_th]:tw-border-0 [&_thead_th]:tw-bg-vm-slate-25 [&_thead_th]:tw-px-4 [&_thead_th]:tw-py-3 [&_thead_th]:tw-text-left [&_thead_th]:tw-text-[0.76rem] [&_thead_th]:tw-font-black [&_thead_th]:tw-uppercase [&_thead_th]:tw-tracking-normal [&_thead_th]:tw-text-vm-slate-500">
          <thead>
            <tr>
              <th>Mã đơn</th>
              <th>Khách hàng</th>
              <th>Phương tiện</th>
              <th>Gói tháng</th>
              <th>Ngày hiệu lực</th>
              <th>Trạng thái</th>
              <th>Thẻ</th>
              <th>Số tiền</th>
              <th className="tw-text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {pagedItems.map((item) => (
              <tr
                className={cn("tw-cursor-pointer tw-transition hover:[&>td]:tw-bg-brand-50/60", activeId === item.id ? "[&>td]:tw-bg-brand-50 tw-shadow-[inset_3px_0_0_#2563EB]" : "")}
                key={item.id}
                onClick={() => onSelect(item)}
              >
                <td>
                  <strong className="tw-block tw-text-[0.86rem] tw-font-black tw-text-vm-primary">{item.id}</strong>
                  <span className="tw-text-[0.72rem] tw-font-bold tw-text-vm-slate-500">{item.time}</span>
                </td>
                <td>
                  <strong className="tw-block tw-text-[0.86rem] tw-font-black tw-text-vm-slate-900">{item.customerName}</strong>
                  <span className="tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500">{item.customerPhone}</span>
                </td>
                <td>
                  <strong className="tw-block tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">{item.plate}</strong>
                  <span className="tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500">{item.vehicleModel}</span>
                </td>
                <td>
                  <Badge tone={packageTone(item.packageType)} className="tw-rounded-vm-sm tw-px-2.5">{item.packageLabel}</Badge>
                  <span className="tw-mt-1 tw-block tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">{item.location}</span>
                </td>
                <td className="tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-700">{item.date}</td>
                <td>
                  <span className={cn("tw-inline-flex tw-rounded-vm-sm tw-px-2.5 tw-py-1 tw-text-[0.72rem] tw-font-black", statusTagClass(item.status))}>
                    {statusLabel(item.status)}
                  </span>
                </td>
                <td>
                  <span className={cn("tw-inline-flex tw-rounded-vm-sm tw-px-2.5 tw-py-1 tw-text-[0.7rem] tw-font-black", item.cardLabel === "ACTIVE" ? "tw-bg-green-50 tw-text-green-600" : item.cardLabel.includes("giữ") ? "tw-bg-purple-50 tw-text-purple-600" : "tw-bg-vm-slate-50 tw-text-vm-slate-600")}>
                    {item.cardLabel}
                  </span>
                </td>
                <td className="tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">{item.amount}</td>
                <td className="tw-text-right">
                  <button
                    className="tw-inline-flex tw-h-8 tw-items-center tw-justify-center tw-rounded-vm-sm tw-border tw-border-solid tw-border-brand-100 tw-bg-white tw-px-3 tw-text-[0.76rem] tw-font-black tw-text-vm-primary tw-transition hover:tw-bg-brand-50"
                    onClick={(event) => {
                      event.stopPropagation();
                      onSelect(item);
                    }}
                    type="button"
                  >
                    Xem
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <PaginationFooter
        currentPage={currentPage}
        endIndex={endIndex}
        onPageChange={setPage}
        onPageSizeChange={(nextPageSize) => {
          setPageSize(nextPageSize);
          setPage(1);
        }}
        pageSize={pageSize}
        pageSizeOptions={[5, 10, 20]}
        startIndex={startIndex}
        totalPages={totalPages}
        totalRecords={items.length}
      />
    </section>
  );
}

function DetailLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="tw-grid tw-grid-cols-[120px_minmax(0,1fr)] tw-gap-4 tw-text-[0.82rem]">
      <span className="tw-font-semibold tw-text-vm-slate-500">{label}</span>
      <strong className="tw-font-black tw-text-vm-slate-900">{value}</strong>
    </div>
  );
}

function CardPickerPopover({ onChoose, selectedCode }: { onChoose: (card: AvailableCard) => void; selectedCode: string | null }) {
  return (
    <div className="tw-absolute tw-left-0 tw-top-[calc(100%+8px)] tw-z-[2300] tw-w-[min(100%,360px)] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3 tw-shadow-[0_24px_64px_rgba(15,23,42,0.18)]">
      <h4 className="tw-m-0 tw-pb-2 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900">Chọn thẻ để gán</h4>
      <div className="tw-grid tw-gap-2">
        {availableCards.map((card) => (
          <button
            key={card.code}
            type="button"
            className={cn(
              "tw-grid tw-grid-cols-[22px_minmax(0,1fr)_auto] tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-p-2 tw-text-left hover:tw-border-brand-200 hover:tw-bg-brand-50",
              selectedCode === card.code ? "tw-border-vm-primary tw-bg-brand-50" : "tw-border-vm-slate-100 tw-bg-white",
            )}
            onClick={() => onChoose(card)}
          >
            <span className={cn("tw-h-4 tw-w-4 tw-rounded-full tw-border tw-border-solid", selectedCode === card.code ? "tw-border-vm-primary tw-bg-vm-primary tw-shadow-[inset_0_0_0_4px_white]" : "tw-border-vm-slate-300")} />
            <span className="tw-min-w-0">
              <strong className="tw-block tw-text-[0.78rem] tw-font-black tw-text-vm-primary">{card.code}</strong>
              <small className="tw-text-[0.7rem] tw-font-semibold tw-text-vm-slate-500">{card.label}</small>
            </span>
            <span className={cn("tw-rounded-vm-sm tw-px-2 tw-py-1 tw-text-[0.66rem] tw-font-black", card.state === "reserved" ? "tw-bg-purple-50 tw-text-purple-600" : "tw-bg-green-50 tw-text-green-600")}>
              {card.state === "reserved" ? "Thẻ đại giữ chỗ" : "Khả dụng"}
            </span>
          </button>
        ))}
      </div>
      <button type="button" className="tw-mt-3 tw-w-full tw-border-0 tw-bg-transparent tw-py-1 tw-text-[0.78rem] tw-font-black tw-text-vm-primary">Xem tất cả thẻ</button>
    </div>
  );
}

function ReviewDrawer({ onClose, open, request }: { onClose: () => void; open: boolean; request: SubscriptionRequest | null }) {
  const [pickerOpen, setPickerOpen] = useState(false);
  const [selectedCard, setSelectedCard] = useState<AvailableCard | null>(availableCards[0]);

  if (!request) return null;

  return (
    <Drawer
      actions={
        <div className="tw-grid tw-grid-cols-[1fr_1fr_1.15fr] tw-gap-3">
          <Button variant="secondary" onClick={onClose}>Hủy</Button>
          <Button variant="danger" onClick={onClose}>Từ chối</Button>
          <Button onClick={onClose}>Duyệt yêu cầu</Button>
        </div>
      }
      description="Duyệt hồ sơ, hóa đơn và gán thẻ tháng"
      onClose={onClose}
      open={open}
      title="Chi tiết đăng ký"
      width="md"
    >
        <div className="tw-grid tw-gap-5">
          <div className="tw-flex tw-items-center tw-gap-2">
            <Badge tone="primary" className="tw-rounded-vm-sm tw-px-3">{request.id}</Badge>
            <span className="tw-rounded-vm-sm tw-bg-blue-50 tw-px-2.5 tw-py-1 tw-text-[0.72rem] tw-font-black tw-text-vm-primary">Chờ duyệt</span>
          </div>
        <section className="tw-grid tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-4">
          <h3 className="tw-m-0 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900"><i className="far fa-user tw-mr-2 tw-text-vm-slate-500" />Thông tin khách hàng</h3>
          <div className="tw-grid tw-grid-cols-[52px_minmax(0,1fr)] tw-gap-3">
            <span className="tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-full tw-bg-vm-slate-100 tw-text-vm-slate-500"><i className="fas fa-user" /></span>
            <div className="tw-grid tw-gap-1">
              <strong className="tw-text-[0.95rem] tw-font-black tw-text-vm-slate-900">{request.customerName}</strong>
              <span className="tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-600"><i className="fas fa-phone tw-mr-2" />{request.customerPhone}</span>
              <span className="tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-600"><i className="far fa-envelope tw-mr-2" />{request.customerEmail}</span>
              <span className="tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-600">CMND/CCCD: 001234567890</span>
            </div>
          </div>
        </section>

        <section className="tw-grid tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-4">
          <h3 className="tw-m-0 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900"><i className="fas fa-car tw-mr-2 tw-text-vm-slate-500" />Thông tin phương tiện</h3>
          <DetailLine label="Biển số" value={request.plate} />
          <DetailLine label="Loại xe" value="Ô tô" />
          <DetailLine label="Nhãn hiệu" value={request.vehicleModel} />
          <DetailLine label="Màu sắc" value="Đen" />
        </section>

        <section className="tw-grid tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-4">
          <DetailLine label="Ngày hiệu lực" value={request.date} />
          <DetailLine label="Gói tháng" value={`${request.packageLabel} - ${request.amount}/tháng`} />
          <DetailLine label="Hóa đơn" value={`${request.invoiceCode} - ${request.amount}`} />
        </section>

        <section className="tw-grid tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-4">
          <div className="tw-flex tw-items-center tw-justify-between">
            <h3 className="tw-m-0 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900"><i className="far fa-id-card tw-mr-2 tw-text-vm-slate-500" />Chọn thẻ khả dụng</h3>
            <button type="button" className="tw-border-0 tw-bg-transparent tw-text-[0.78rem] tw-font-black tw-text-vm-primary"><i className="fas fa-sync-alt tw-mr-1" />Làm mới</button>
          </div>
          <div className="tw-relative">
            <button
              type="button"
              className="tw-flex tw-h-11 tw-w-full tw-items-center tw-justify-between tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-white tw-px-3 tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700"
              onClick={() => setPickerOpen((current) => !current)}
            >
              <span>{selectedCard ? selectedCard.code : "Chọn thẻ..."}</span>
              <i className="fas fa-chevron-down tw-text-[0.7rem]" />
            </button>
            {pickerOpen ? <CardPickerPopover selectedCode={selectedCard?.code ?? null} onChoose={(card) => {
              setSelectedCard(card);
              setPickerOpen(false);
            }} /> : null}
          </div>
        </section>

        <section className="tw-grid tw-gap-3">
          <h3 className="tw-m-0 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900"><i className="fas fa-history tw-mr-2 tw-text-vm-slate-500" />Lịch sử</h3>
          {["Tạo đăng ký", "Chờ duyệt"].map((item) => (
            <div key={item} className="tw-grid tw-grid-cols-[90px_12px_minmax(0,1fr)] tw-items-center tw-gap-3 tw-text-[0.78rem] tw-font-semibold">
              <span className="tw-text-vm-slate-500">24/05/2024</span>
              <span className="tw-h-3 tw-w-3 tw-rounded-full tw-bg-vm-primary" />
              <span className="tw-text-vm-slate-800">{item}<small className="tw-ml-2 tw-text-vm-slate-500">Hệ thống</small></span>
            </div>
          ))}
        </section>
        </div>
    </Drawer>
  );
}

export function SubscriptionApprovalPage() {
  const [selectedRequest, setSelectedRequest] = useState<SubscriptionRequest | null>(requests[0]);
  const [drawerOpen, setDrawerOpen] = useState(true);
  const [statusFilter, setStatusFilter] = useState("all");
  const [packageFilter, setPackageFilter] = useState("all");
  const [siteFilter, setSiteFilter] = useState("all");
  const [searchValue, setSearchValue] = useState("");
  const [dateRange, setDateRange] = useState("2024-05-01|2024-05-31");
  const [viewMode, setViewMode] = useState<ViewMode>("pipeline");

  const filteredRequests = useMemo(() => {
    const search = searchValue.trim().toLowerCase();
    return requests.filter((request) => {
      const matchesStatus = statusFilter === "all" || request.status === statusFilter;
      const matchesPackage = packageFilter === "all" || request.packageType === packageFilter;
      const matchesSite = siteFilter === "all" || request.location === siteFilter;
      const matchesSearch = !search || [request.id, request.customerName, request.plate].some((value) => value.toLowerCase().includes(search));
      return matchesStatus && matchesPackage && matchesSite && matchesSearch;
    });
  }, [packageFilter, searchValue, siteFilter, statusFilter]);

  const pipelineStatuses: SubscriptionStatus[] = ["pending_review", "waiting_payment", "waiting_card", "active"];

  return (
    <div className="content-header tw-px-0 tw-pb-4 tw-pt-3">
      <section className="content tw-pb-8">
        <div className="container-fluid tw-max-w-[1480px]">
          <div className="tw-grid tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4 tw-shadow-[0_16px_34px_rgba(15,23,42,0.04)]">
            <header>
              <h1 className="tw-m-0 tw-text-vm-page-title tw-font-black tw-text-vm-slate-900">Duyệt đăng ký vé tháng & Gán thẻ</h1>
            </header>

            <div className="tw-grid tw-grid-cols-5 tw-gap-4 max-[1280px]:tw-grid-cols-3 max-[900px]:tw-grid-cols-1">
              {pipelineStatuses.map((status) => <SummaryCard key={status} {...statusMeta[status]} />)}
              <SummaryCard {...rejectedMetric} />
            </div>

            <div className="tw-grid tw-grid-cols-[minmax(210px,230px)_116px_116px_104px_236px_104px_84px] tw-gap-2 max-[1080px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
              <label className="tw-flex tw-h-10 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3">
                <input className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.82rem] tw-font-semibold tw-outline-none placeholder:tw-text-vm-slate-500" placeholder="Tìm kiếm theo tên, SĐT, biển số, mã đơn..." value={searchValue} onChange={(event) => setSearchValue(event.target.value)} />
                <i className="fas fa-search tw-text-vm-slate-500" />
              </label>
              <SelectMenu ariaLabel="Trạng thái" options={statusOptions} value={statusFilter} onChange={setStatusFilter} />
              <SelectMenu ariaLabel="Gói tháng" options={packageOptions} value={packageFilter} onChange={setPackageFilter} />
              <SelectMenu ariaLabel="Bãi xe" options={siteOptions} value={siteFilter} onChange={setSiteFilter} />
              <DateRangeInput value={dateRange} onChange={setDateRange} />
              <Button variant="secondary" className="tw-h-10 tw-whitespace-nowrap tw-px-3"><i className="fas fa-filter tw-mr-2" />Bộ lọc</Button>
              <div className="tw-flex tw-h-10 tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white">
                <button
                  aria-label="Xem pipeline"
                  className={cn(
                    "tw-inline-flex tw-h-full tw-w-10 tw-items-center tw-justify-center tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 tw-bg-transparent tw-text-vm-slate-600 tw-transition hover:tw-bg-brand-50 hover:tw-text-vm-primary",
                    viewMode === "pipeline" ? "tw-bg-vm-primary tw-text-white hover:tw-bg-vm-primary hover:tw-text-white" : "",
                  )}
                  onClick={() => setViewMode("pipeline")}
                  type="button"
                >
                  <i className="fas fa-columns" />
                </button>
                <button
                  aria-label="Xem bảng"
                  className={cn(
                    "tw-inline-flex tw-h-full tw-w-10 tw-items-center tw-justify-center tw-border-0 tw-bg-transparent tw-text-vm-slate-600 tw-transition hover:tw-bg-brand-50 hover:tw-text-vm-primary",
                    viewMode === "table" ? "tw-bg-vm-primary tw-text-white hover:tw-bg-vm-primary hover:tw-text-white" : "",
                  )}
                  onClick={() => setViewMode("table")}
                  type="button"
                >
                  <i className="fas fa-list" />
                </button>
              </div>
            </div>

            {viewMode === "pipeline" ? (
              <div className="tw-overflow-x-auto tw-pb-2">
                <div className="tw-grid tw-min-w-[1040px] tw-grid-cols-4 tw-gap-4">
                  {pipelineStatuses.map((status) => (
                    <PipelineColumn
                      key={status}
                      status={status}
                      activeId={selectedRequest?.id}
                      items={filteredRequests.filter((request) => request.status === status)}
                      onSelect={(request) => {
                        setSelectedRequest(request);
                        setDrawerOpen(true);
                      }}
                    />
                  ))}
                </div>
              </div>
            ) : (
              <SubscriptionTableView
                activeId={selectedRequest?.id}
                items={filteredRequests}
                onSelect={(request) => {
                  setSelectedRequest(request);
                  setDrawerOpen(true);
                }}
              />
            )}
          </div>
        </div>
      </section>

      <ReviewDrawer open={drawerOpen} request={selectedRequest} onClose={() => setDrawerOpen(false)} />
    </div>
  );
}
