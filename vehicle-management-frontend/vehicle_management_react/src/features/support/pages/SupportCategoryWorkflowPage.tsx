import { useEffect, useMemo, useState } from "react";

import { Badge, Button, Card, Drawer, Modal, PaginationFooter, SelectMenu } from "@/components/ui";
import { cn } from "@/lib/cn";

type CategoryStatus = "ACTIVE" | "INACTIVE";
type Priority = "HIGH" | "MEDIUM" | "LOW";

type SupportCategory = {
  assigneeGroup: string;
  checklist: Array<{ checked: boolean; label: string }>;
  code: string;
  id: string;
  openTickets: number;
  priority: Priority;
  priorityLabel: string;
  slaHours: number;
  status: CategoryStatus;
  title: string;
};

const categories: SupportCategory[] = [
  {
    assigneeGroup: "Nhóm CSKH",
    checklist: [
      { checked: true, label: "Xác minh thông tin phương tiện & chủ xe" },
      { checked: true, label: "Kiểm tra lịch sử ra vào" },
      { checked: true, label: "Kiểm tra hình ảnh từ camera" },
      { checked: true, label: "Hủy thẻ cũ trong hệ thống" },
      { checked: true, label: "Cấp lại thẻ mới" },
      { checked: false, label: "Hướng dẫn khách hàng nhận thẻ" },
    ],
    code: "LOST_CARD",
    id: "cat-lost-card",
    openTickets: 28,
    priority: "HIGH",
    priorityLabel: "Cao",
    slaHours: 4,
    status: "ACTIVE",
    title: "Mất thẻ xe",
  },
  {
    assigneeGroup: "Nhóm Thu phí",
    checklist: [
      { checked: true, label: "Tra cứu lượt xe" },
      { checked: true, label: "Đối chiếu bảng giá" },
      { checked: false, label: "Ghi nhận điều chỉnh phí" },
    ],
    code: "WRONG_FEE",
    id: "cat-wrong-fee",
    openTickets: 34,
    priority: "HIGH",
    priorityLabel: "Cao",
    slaHours: 8,
    status: "ACTIVE",
    title: "Tính phí sai",
  },
  {
    assigneeGroup: "Nhóm An ninh",
    checklist: [
      { checked: true, label: "Tiếp nhận hình ảnh" },
      { checked: true, label: "Lập biên bản hiện trạng" },
      { checked: false, label: "Chuyển quản lý vận hành" },
    ],
    code: "VEHICLE_DAMAGE",
    id: "cat-damage",
    openTickets: 12,
    priority: "MEDIUM",
    priorityLabel: "Trung bình",
    slaHours: 24,
    status: "ACTIVE",
    title: "Hư hỏng phương tiện",
  },
  {
    assigneeGroup: "Nhóm Kế toán",
    checklist: [
      { checked: true, label: "Kiểm tra giao dịch" },
      { checked: true, label: "Đối soát invoice" },
      { checked: false, label: "Gửi link thanh toán lại" },
    ],
    code: "PAYMENT_PROBLEM",
    id: "cat-payment",
    openTickets: 21,
    priority: "MEDIUM",
    priorityLabel: "Trung bình",
    slaHours: 24,
    status: "ACTIVE",
    title: "Sự cố thanh toán",
  },
  {
    assigneeGroup: "Nhóm CSKH",
    checklist: [
      { checked: true, label: "Kiểm tra gói vé tháng" },
      { checked: false, label: "Xác minh biển số" },
      { checked: false, label: "Gia hạn hoặc tạo invoice" },
    ],
    code: "SUBSCRIPTION_PROBLEM",
    id: "cat-subscription",
    openTickets: 19,
    priority: "LOW",
    priorityLabel: "Thấp",
    slaHours: 24,
    status: "ACTIVE",
    title: "Sự cố vé tháng",
  },
  {
    assigneeGroup: "",
    checklist: [{ checked: false, label: "Phân loại thủ công" }],
    code: "OTHER",
    id: "cat-other",
    openTickets: 14,
    priority: "LOW",
    priorityLabel: "Thấp",
    slaHours: 48,
    status: "ACTIVE",
    title: "Khác",
  },
];

const statusTabs = [
  { label: "Tất cả (6)", value: "all" },
  { label: "Đang hoạt động (6)", value: "active" },
  { label: "Ngưng sử dụng (0)", value: "inactive" },
] as const;

const priorityOptions = [
  { label: "Ưu tiên: Tất cả", value: "all" },
  { label: "Cao", value: "HIGH" },
  { label: "Trung bình", value: "MEDIUM" },
  { label: "Thấp", value: "LOW" },
];

const groupOptions = [
  { label: "Nhóm phụ trách: Tất cả", value: "all" },
  { label: "Nhóm CSKH", value: "support" },
  { label: "Nhóm Thu phí", value: "fee" },
  { label: "Nhóm An ninh", value: "security" },
  { label: "Nhóm Kế toán", value: "accounting" },
];

const statusOptions = [
  { label: "Trạng thái: Tất cả", value: "all" },
  { label: "Đang hoạt động", value: "ACTIVE" },
  { label: "Ngưng sử dụng", value: "INACTIVE" },
];

function priorityTone(priority: Priority) {
  if (priority === "HIGH") return "danger";
  if (priority === "MEDIUM") return "warning";
  return "success";
}

function MetricCard({ icon, iconClassName, label, meta, metaClassName, value }: { icon: string; iconClassName: string; label: string; meta: string; metaClassName: string; value: string }) {
  return (
    <Card className="tw-flex tw-min-h-[108px] tw-items-center tw-justify-between tw-p-4">
      <span className="tw-grid">
        <span className="tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-500">{label}</span>
        <strong className="tw-mt-2 tw-text-[1.7rem] tw-font-black tw-leading-none tw-text-vm-slate-900">{value}</strong>
        <span className={cn("tw-mt-2 tw-text-[0.72rem] tw-font-black", metaClassName)}>{meta}</span>
      </span>
      <span className={cn("tw-inline-flex tw-h-11 tw-w-11 tw-items-center tw-justify-center tw-rounded-full tw-text-[1.2rem]", iconClassName)}>
        <i className={icon} />
      </span>
    </Card>
  );
}

function CategoryTable({
  currentPage,
  onDisable,
  onEdit,
  onPageChange,
  onPageSizeChange,
  pageSize,
  rows,
  startIndex,
  totalRecords,
}: {
  currentPage: number;
  onDisable: (category: SupportCategory) => void;
  onEdit: (category: SupportCategory) => void;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  pageSize: number;
  rows: SupportCategory[];
  startIndex: number;
  totalRecords: number;
}) {
  const endIndex = totalRecords === 0 ? 0 : startIndex + rows.length - 1;
  const totalPages = Math.max(1, Math.ceil(totalRecords / pageSize));

  return (
    <div className="tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white">
      <div className="tw-grid tw-h-11 tw-grid-cols-[48px_150px_minmax(140px,1fr)_92px_150px_118px_132px_88px] tw-items-center tw-bg-vm-slate-25 tw-px-4 tw-text-[0.72rem] tw-font-black tw-text-vm-slate-700 max-[1180px]:tw-hidden">
        <span>#</span>
        <span>Mã danh mục</span>
        <span>Tên danh mục</span>
        <span>Ưu tiên</span>
        <span>Nhóm phụ trách</span>
        <span>SLA mặc định</span>
        <span>Trạng thái</span>
        <span className="tw-text-right">Thao tác</span>
      </div>

      {rows.map((category, index) => (
        <div key={category.id} className="tw-grid tw-min-h-[56px] tw-grid-cols-[48px_150px_minmax(140px,1fr)_92px_150px_118px_132px_88px] tw-items-center tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-700 max-[1180px]:tw-grid-cols-1 max-[1180px]:tw-gap-2 max-[1180px]:tw-py-3">
          <span>{startIndex + index}</span>
          <strong className="tw-text-[0.78rem] tw-text-vm-slate-900">{category.code}</strong>
          <span>{category.title}</span>
          <Badge tone={priorityTone(category.priority)} className="tw-w-fit tw-rounded-vm-sm tw-px-2.5">{category.priorityLabel}</Badge>
          <span className="tw-flex tw-items-center tw-gap-2">
            {category.assigneeGroup ? <i className="far fa-user tw-text-vm-slate-500" /> : null}
            {category.assigneeGroup}
          </span>
          <strong className="tw-text-vm-slate-900">{category.slaHours} giờ</strong>
          <Badge tone="success" className="tw-w-fit tw-rounded-vm-sm tw-px-3">Đang hoạt động</Badge>
          <span className="tw-flex tw-justify-end tw-gap-3 max-[1180px]:tw-justify-start">
            <button type="button" className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-white tw-text-vm-primary hover:tw-bg-brand-50" aria-label={`Chỉnh ${category.title}`} onClick={() => onEdit(category)}>
              <i className="far fa-edit tw-text-[1rem]" />
            </button>
            <button type="button" className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-white tw-text-vm-slate-700 hover:tw-bg-red-50 hover:tw-text-vm-danger" aria-label={`Ngưng ${category.title}`} onClick={() => onDisable(category)}>
              <i className="fas fa-ellipsis-v" />
            </button>
          </span>
        </div>
      ))}

      <PaginationFooter
        ariaLabel="Phân trang danh mục hỗ trợ"
        currentPage={currentPage}
        endIndex={endIndex}
        onPageChange={onPageChange}
        onPageSizeChange={onPageSizeChange}
        pageSize={pageSize}
        pageSizeOptions={[5, 10, 20]}
        startIndex={totalRecords === 0 ? 0 : startIndex}
        totalPages={totalPages}
        totalRecords={totalRecords}
      />
    </div>
  );
}

function SelectBox({ children, wide = false }: { children: React.ReactNode; wide?: boolean }) {
  return (
    <button type="button" className={cn("tw-flex tw-h-10 tw-items-center tw-justify-between tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-left tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-700", wide ? "tw-w-full" : "")}>
      <span className="tw-min-w-0 tw-truncate">{children}</span>
      <i className="fas fa-chevron-down tw-text-[0.62rem] tw-text-vm-slate-500" />
    </button>
  );
}

function DrawerField({ children, label }: { children: React.ReactNode; label: string }) {
  return (
    <label className="tw-grid tw-gap-2">
      <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">{label}</span>
      {children}
    </label>
  );
}

function CategoryEditDrawer({ category, onClose, open }: { category: SupportCategory | null; onClose: () => void; open: boolean }) {
  return (
    <Drawer
      actions={
        <div className="tw-grid tw-grid-cols-2 tw-gap-2">
          <Button variant="secondary" onClick={onClose}>Hủy</Button>
          <Button onClick={onClose}>Lưu thay đổi</Button>
        </div>
      }
      description="Cập nhật thông tin danh mục ticket"
      onClose={onClose}
      open={open && Boolean(category)}
      title="Chỉnh sửa danh mục"
      width="lg"
    >
      {category ? (
        <div className="tw-grid tw-gap-4">
          <DrawerField label="Mã danh mục">
            <input className="tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200" defaultValue={category.code} />
          </DrawerField>
          <DrawerField label="Tên danh mục">
            <input className="tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200" defaultValue={category.title} />
          </DrawerField>
          <DrawerField label="Ưu tiên">
            <SelectBox wide><span className="tw-mr-2 tw-inline-block tw-h-2 tw-w-2 tw-rounded-full tw-bg-red-500" />{category.priorityLabel}</SelectBox>
          </DrawerField>
          <div className="tw-grid tw-gap-2">
            <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">SLA</span>
            <div className="tw-grid tw-grid-cols-[minmax(0,1fr)_80px] tw-gap-2">
              <input className="tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-outline-none" defaultValue={category.slaHours} />
              <SelectBox>giờ</SelectBox>
            </div>
            <small className="tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">Thời gian mục tiêu phản hồi & xử lý</small>
          </div>
          <DrawerField label="Nhóm phụ trách">
            <SelectBox wide>{category.assigneeGroup || "Chưa gán"}</SelectBox>
          </DrawerField>
          <div className="tw-grid tw-gap-2">
            <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">Trạng thái</span>
            <div className="tw-grid tw-grid-cols-2 tw-gap-2">
              <button type="button" className="tw-flex tw-h-10 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-brand-50 tw-text-[0.78rem] tw-font-black tw-text-vm-primary">
                <span className="tw-h-2 tw-w-2 tw-rounded-full tw-bg-green-500" /> Đang hoạt động
              </button>
              <button type="button" className="tw-flex tw-h-10 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">
                <span className="tw-h-2 tw-w-2 tw-rounded-full tw-bg-vm-slate-300" /> Ngưng sử dụng
              </button>
            </div>
          </div>
          <div className="tw-flex tw-items-center tw-justify-between tw-pt-1">
            <span className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-900">Cấu hình leo thang</span>
            <button type="button" className="tw-relative tw-h-6 tw-w-11 tw-rounded-full tw-border-0 tw-bg-vm-primary" aria-label="Bật leo thang">
              <span className="tw-absolute tw-right-0.5 tw-top-0.5 tw-h-5 tw-w-5 tw-rounded-full tw-bg-white" />
            </button>
          </div>
          <div className="tw-grid tw-gap-2">
            <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">Thời gian leo thang</span>
            <div className="tw-grid tw-grid-cols-[minmax(0,1fr)_80px] tw-gap-2">
              <input className="tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-outline-none" defaultValue="2" />
              <SelectBox>giờ</SelectBox>
            </div>
          </div>
          <DrawerField label="Nhóm leo thang">
            <SelectBox wide>Nhóm Quản lý vận hành</SelectBox>
          </DrawerField>
          <div className="tw-grid tw-gap-2">
            <span className="tw-text-[0.78rem] tw-font-black tw-text-vm-slate-700">Danh sách kiểm tra khi xử lý</span>
            <div className="tw-grid tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100">
              {category.checklist.map((item) => (
                <label key={item.label} className="tw-flex tw-h-9 tw-items-center tw-gap-2 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-2 last:tw-border-b-0">
                  <i className="fas fa-grip-vertical tw-text-[0.68rem] tw-text-vm-slate-400" />
                  <input className="tw-h-3.5 tw-w-3.5 tw-accent-vm-primary" defaultChecked={item.checked} type="checkbox" />
                  <span className="tw-min-w-0 tw-flex-1 tw-truncate tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-700">{item.label}</span>
                  <i className="far fa-trash-alt tw-text-[0.72rem] tw-text-vm-slate-500" />
                </label>
              ))}
            </div>
            <button type="button" className="tw-mt-1 tw-flex tw-h-9 tw-w-fit tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-100 tw-bg-white tw-px-3 tw-text-[0.78rem] tw-font-bold tw-text-vm-primary">
              <i className="fas fa-plus" /> Thêm mục kiểm tra
            </button>
          </div>
        </div>
      ) : null}
    </Drawer>
  );
}

function DisableWarningModal({ category, onClose }: { category: SupportCategory; onClose: () => void }) {
  return (
    <Modal
      actions={
        <div className="tw-flex tw-justify-end tw-gap-2">
          <Button variant="secondary" onClick={onClose}>Hủy</Button>
          <Button variant="danger" onClick={onClose}>Ngưng sử dụng</Button>
        </div>
      }
      description="Không thể tắt khi còn ticket đang mở."
      onClose={onClose}
      open={Boolean(category)}
      title="Ngưng sử dụng danh mục"
      width="md"
    >
      <div className="tw-grid tw-grid-cols-[56px_minmax(0,1fr)] tw-gap-4">
        <span className="tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-vm-lg tw-border-2 tw-border-solid tw-border-orange-400 tw-bg-orange-50 tw-text-[1.45rem] tw-text-orange-500">
          <i className="fas fa-exclamation-triangle" />
        </span>
        <div className="tw-min-w-0">
          <p className="tw-mb-0 tw-mt-1 tw-text-[0.9rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-700">
            Bạn có chắc chắn muốn ngưng sử dụng danh mục “{category.title}”?
          </p>
          <p className="tw-mb-0 tw-mt-2 tw-text-[0.86rem] tw-font-black tw-text-red-600">Không thể tắt khi còn ticket đang mở.</p>
        </div>
      </div>
    </Modal>
  );
}

export function SupportCategoryWorkflowPage() {
  const [activeTab, setActiveTab] = useState<(typeof statusTabs)[number]["value"]>("all");
  const [group, setGroup] = useState("all");
  const [priority, setPriority] = useState("all");
  const [status, setStatus] = useState("all");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [editingCategory, setEditingCategory] = useState<SupportCategory | null>(null);
  const [warningCategory, setWarningCategory] = useState<SupportCategory | null>(null);

  const visibleCategories = useMemo(() => {
    return categories.filter((category) => {
      const matchTab =
        activeTab === "all" ||
        (activeTab === "active" && category.status === "ACTIVE") ||
        (activeTab === "inactive" && category.status === "INACTIVE");
      const matchPriority = priority === "all" || category.priority === priority;
      const matchStatus = status === "all" || category.status === status;
      return matchTab && matchPriority && matchStatus;
    });
  }, [activeTab, priority, status]);

  const totalPages = Math.max(1, Math.ceil(visibleCategories.length / pageSize));

  useEffect(() => {
    setCurrentPage(1);
  }, [activeTab, group, pageSize, priority, status]);

  useEffect(() => {
    setCurrentPage((page) => Math.min(page, totalPages));
  }, [totalPages]);

  const pageStartIndex = (currentPage - 1) * pageSize;
  const paginatedCategories = visibleCategories.slice(pageStartIndex, pageStartIndex + pageSize);

  return (
    <div className="tw-px-4 tw-py-4 lg:tw-px-5">
      <section className="tw-mx-auto tw-min-h-[calc(100vh-104px)] tw-w-[min(100%,1500px)] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-vm-card">
        <header>
          <h1 className="tw-m-0 tw-text-vm-page-title tw-text-vm-slate-900">Danh mục hỗ trợ & Quy trình ticket</h1>
          <p className="tw-mb-0 tw-mt-2 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-500">Quản lý danh mục ticket và cấu hình quy trình xử lý</p>
        </header>

        <div className="tw-mt-6 tw-grid tw-grid-cols-5 tw-gap-3 max-[1280px]:tw-grid-cols-3 max-[900px]:tw-grid-cols-2">
          <MetricCard icon="fas fa-list-ul" iconClassName="tw-bg-brand-50 tw-text-vm-primary" label="Tổng danh mục" meta="Danh mục" metaClassName="tw-text-vm-slate-500" value="6" />
          <MetricCard icon="far fa-check-circle" iconClassName="tw-bg-green-50 tw-text-green-600" label="Đang hoạt động" meta="100%" metaClassName="tw-text-green-600" value="6" />
          <MetricCard icon="fas fa-user-slash" iconClassName="tw-bg-red-50 tw-text-red-500" label="Ngưng sử dụng" meta="0%" metaClassName="tw-text-red-500" value="0" />
          <MetricCard icon="fas fa-ticket-alt" iconClassName="tw-bg-orange-50 tw-text-orange-500" label="Ticket đang mở" meta="Chi tiết" metaClassName="tw-text-vm-primary" value="128" />
          <MetricCard icon="far fa-clock" iconClassName="tw-bg-red-50 tw-text-red-500" label="Quá SLA" meta="Chi tiết" metaClassName="tw-text-vm-primary" value="7" />
        </div>

        <div className="tw-mt-7 tw-flex tw-gap-8 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100">
          {statusTabs.map((tab) => (
            <button
              key={tab.value}
              type="button"
              className={cn(
                "tw-relative tw-border-0 tw-bg-transparent tw-pb-3 tw-text-[0.86rem] tw-font-bold",
                activeTab === tab.value ? "tw-text-vm-primary after:tw-absolute after:tw-bottom-0 after:tw-left-0 after:tw-h-0.5 after:tw-w-full after:tw-bg-vm-primary" : "tw-text-vm-slate-500",
              )}
              onClick={() => setActiveTab(tab.value)}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="tw-mt-5 tw-grid tw-grid-cols-[minmax(240px,1fr)_160px_200px_170px_88px] tw-gap-3 max-[1180px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
          <label className="tw-flex tw-h-10 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3">
            <input className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.82rem] tw-font-semibold tw-outline-none placeholder:tw-text-vm-slate-500" placeholder="Tìm theo mã hoặc tên danh mục..." />
            <i className="fas fa-search tw-text-vm-slate-500" />
          </label>
          <SelectMenu ariaLabel="Ưu tiên" options={priorityOptions} value={priority} onChange={setPriority} />
          <SelectMenu ariaLabel="Nhóm phụ trách" options={groupOptions} value={group} onChange={setGroup} />
          <SelectMenu ariaLabel="Trạng thái" options={statusOptions} value={status} onChange={setStatus} />
          <button type="button" className="tw-flex tw-h-10 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-700">
            <i className="fas fa-sliders-h" /> Bộ lọc
          </button>
        </div>

        <div className="tw-mt-5">
          <CategoryTable
            currentPage={currentPage}
            onEdit={setEditingCategory}
            onDisable={setWarningCategory}
            onPageChange={setCurrentPage}
            onPageSizeChange={setPageSize}
            pageSize={pageSize}
            rows={paginatedCategories}
            startIndex={pageStartIndex + 1}
            totalRecords={visibleCategories.length}
          />
        </div>
      </section>

      <CategoryEditDrawer category={editingCategory} open={Boolean(editingCategory)} onClose={() => setEditingCategory(null)} />
      {warningCategory ? <DisableWarningModal category={warningCategory} onClose={() => setWarningCategory(null)} /> : null}
    </div>
  );
}
