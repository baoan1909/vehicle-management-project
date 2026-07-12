import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from "react";

import { Badge, Button, Card, EntityAvatar, InfoBanner, Input, Modal, PaginationFooter, SelectMenu, useToast } from "@/components/ui";
import { openSupportCenterConversation } from "@/features/support";
import { cn } from "@/lib/cn";

import {
  activateCustomer,
  approveCustomerOnboardingApproval,
  activateCustomerVehicle,
  createCustomerVehicle,
  deleteCustomerVehicle,
  fetchCustomerSubscriptionCards,
  fetchCustomerById,
  fetchCustomerOnboardingApprovals,
  fetchCustomerVehicles,
  fetchCustomers,
  fetchVehicleTypes,
  inactivateCustomer,
  inactivateCustomerVehicle,
  markCustomerVehicleAsDefault,
  updateCustomerAdminProfile,
  updateCustomerVehicle,
  type CustomerAdminResponse,
  type CustomerApprovalStatus,
  type CustomerType,
  type CustomerSubscriptionCardResponse,
  type CustomerVehiclePayload,
  type CustomerVehicleAdminResponse,
  type VehicleTypeResponse,
} from "../api/customerApi";

type SegmentValue = "all" | "approved" | "pending" | "vip";

type CustomerFormState = {
  address: string;
  customerType: CustomerType;
  dateOfBirth: string;
  fullName: string;
  gender: string;
  identifyCard: string;
  phoneNumber: string;
};

type VehicleFormState = {
  brand: string;
  color: string;
  isDefault: boolean;
  licensePlate: string;
  vehicleTypeId: string;
};

type LinkedAsset = {
  icon: string;
  meta: string;
  status: string;
  statusTone: "primary" | "success" | "warning" | "danger" | "neutral";
  title: string;
};

type Activity = {
  color: string;
  date: string;
  description: string;
  title: string;
  user: string;
};

const segmentTabs: Array<{ label: string; value: SegmentValue }> = [
  { label: "Tất cả", value: "all" },
  { label: "Đã duyệt", value: "approved" },
  { label: "Chờ duyệt", value: "pending" },
  { label: "VIP", value: "vip" },
];

const customerTypeOptions = [
  { label: "Đăng ký", value: "REGISTERED" },
  { label: "VIP", value: "VIP" },
];

const emptyActivities: Activity[] = [
  {
    color: "#2563EB",
    date: "--",
    description: "Chưa có dữ liệu hoạt động trong API quản lý khách hàng.",
    title: "Chưa có hoạt động",
    user: "Hệ thống",
  },
];

function formatNumber(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value);
}

function getCustomerName(customer?: CustomerAdminResponse | null) {
  return customer?.userProfile?.fullName?.trim() || customer?.accountEmail || customer?.customerCode || "Khách hàng";
}

function getCustomerCode(customer?: CustomerAdminResponse | null) {
  return customer?.customerCode || customer?.customerId?.slice(0, 8) || "--";
}

function getInitials(name: string) {
  const words = name.trim().split(/\s+/).filter(Boolean);
  if (words.length === 0) return "KH";
  return words.slice(-2).map((word) => word[0]).join("").toUpperCase();
}

function formatDateTime(value?: string | null) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
}

function approvalBadgeTone(status?: CustomerApprovalStatus | null) {
  if (status === "PENDING") return "warning";
  if (status === "APPROVED") return "success";
  if (status === "REJECTED" || status === "SUSPENDED") return "danger";
  return "neutral";
}

function approvalLabel(status?: CustomerApprovalStatus | null) {
  if (status === "APPROVED") return "Đã duyệt";
  if (status === "PENDING") return "Chờ duyệt";
  if (status === "REJECTED") return "Từ chối";
  if (status === "SUSPENDED") return "Tạm khóa";
  return "--";
}

function customerTypeLabel(type?: CustomerType | null) {
  if (type === "VIP") return "VIP";
  if (type === "REGISTERED") return "Đăng ký";
  return "--";
}

function customerStatusLabel(status?: string | null) {
  if (status === "ACTIVE") return "ACTIVE";
  if (status === "INACTIVE") return "INACTIVE";
  return "--";
}

function vehicleStatusLabel(status?: string | null) {
  if (status === "ACTIVE") return "Đang dùng";
  if (status === "INACTIVE") return "Ngưng dùng";
  if (status === "BLOCKED") return "Đã khóa";
  return "--";
}

function vehicleStatusTone(status?: string | null) {
  if (status === "ACTIVE") return "success";
  if (status === "BLOCKED") return "danger";
  return "neutral";
}

function subscriptionStatusLabel(status?: string | null) {
  if (status === "ACTIVE") return "Hiệu lực";
  if (status === "PENDING_PAYMENT") return "Chờ thanh toán";
  if (status === "PENDING_APPROVAL") return "Chờ duyệt";
  if (status === "EXPIRED") return "Hết hạn";
  if (status === "CANCELLED") return "Đã hủy";
  if (status === "REJECTED") return "Từ chối";
  return status || "--";
}

function subscriptionStatusTone(status?: string | null) {
  if (status === "ACTIVE") return "primary";
  if (status === "PENDING_PAYMENT" || status === "PENDING_APPROVAL") return "warning";
  if (status === "EXPIRED" || status === "CANCELLED" || status === "REJECTED") return "danger";
  return "neutral";
}

function getVehicleTypeName(vehicleTypes: VehicleTypeResponse[], vehicleTypeId?: string | null) {
  if (!vehicleTypeId) return "Chưa có loại xe";
  const vehicleType = vehicleTypes.find((item) => item.vehicleTypeId === vehicleTypeId);
  return vehicleType?.name ?? "Chưa đồng bộ loại xe";
}

function toCustomerForm(customer: CustomerAdminResponse): CustomerFormState {
  return {
    address: customer.userProfile?.address ?? "",
    customerType: customer.customerType ?? "REGISTERED",
    dateOfBirth: customer.userProfile?.dateOfBirth ?? "",
    fullName: getCustomerName(customer),
    gender: customer.userProfile?.gender ?? "",
    identifyCard: customer.userProfile?.identifyCard ?? "",
    phoneNumber: customer.userProfile?.phoneNumber ?? "",
  };
}

function toVehicleForm(vehicle?: CustomerVehicleAdminResponse | null): VehicleFormState {
  return {
    brand: vehicle?.brand ?? "",
    color: vehicle?.color ?? "",
    isDefault: Boolean(vehicle?.isDefault),
    licensePlate: vehicle?.licensePlate ?? "",
    vehicleTypeId: vehicle?.vehicleTypeId ?? "",
  };
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
  customer: CustomerAdminResponse;
  onContact: () => void;
  onSelect: () => void;
  selected: boolean;
}) {
  const name = getCustomerName(customer);

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
        <EntityAvatar initials={getInitials(name)} size="sm" />
        <span className="tw-w-0 tw-min-w-0 tw-flex-1">
          <strong className="tw-block tw-truncate tw-text-[0.83rem] tw-font-extrabold tw-text-vm-slate-900">{name}</strong>
          <small className="tw-block tw-truncate tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500">{getCustomerCode(customer)}</small>
        </span>
        <Badge tone={approvalBadgeTone(customer.approvalStatus)} className="tw-flex-shrink-0 tw-rounded-full tw-px-2 tw-text-[0.62rem]">
          {approvalLabel(customer.approvalStatus)}
        </Badge>
      </button>
      <button
        type="button"
        className="tw-inline-flex tw-h-8 tw-w-8 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-100 tw-bg-white tw-text-vm-primary hover:tw-bg-brand-50"
        onClick={onContact}
        aria-label={`Liên hệ ${name}`}
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
      <span className="tw-min-w-0 tw-break-words">{label || "--"}</span>
    </div>
  );
}

function LinkedAssetCard({ asset, compact = false }: { asset: LinkedAsset; compact?: boolean }) {
  return (
    <article className={cn("tw-flex-1 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3", compact ? "tw-min-w-[150px] tw-max-w-[170px]" : "tw-min-w-[188px] tw-max-w-[220px]")}>
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

function EmptyLinkedAssetState({ icon, message }: { icon: string; message: string }) {
  return (
    <div className="tw-flex tw-min-h-[112px] tw-w-full tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-dashed tw-border-vm-slate-200 tw-bg-vm-slate-25 tw-p-4 tw-text-center">
      <div>
        <span className="tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-full tw-bg-white tw-text-vm-slate-400">
          <i className={icon} />
        </span>
        <p className="tw-mb-0 tw-mt-2 tw-text-[0.82rem] tw-font-semibold tw-leading-snug tw-text-vm-slate-500">{message}</p>
      </div>
    </div>
  );
}

function AssetCarousel({
  accentClassName,
  children,
  empty,
  onMore,
  title,
}: {
  accentClassName: string;
  children: ReactNode;
  empty: boolean;
  onMore?: () => void;
  title: string;
}) {
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const scroll = (direction: "left" | "right") => {
    scrollRef.current?.scrollBy({
      behavior: "smooth",
      left: direction === "left" ? -220 : 220,
    });
  };

  return (
    <section>
      <div className="tw-mb-2 tw-flex tw-items-center tw-justify-between tw-gap-2">
        <div className="tw-flex tw-items-center tw-gap-2">
          <span className={cn("tw-h-2 tw-w-2 tw-rounded-full", accentClassName)} />
          <h3 className="tw-m-0 tw-text-[0.82rem] tw-font-extrabold tw-uppercase tw-tracking-[0.04em] tw-text-vm-slate-500">{title}</h3>
        </div>
        <button
          type="button"
          className="tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.76rem] tw-font-extrabold tw-text-vm-primary hover:tw-text-vm-primary-hover"
          onClick={onMore}
        >
          Xem thêm
        </button>
      </div>
      <div className="tw-group tw-grid tw-grid-cols-[32px_minmax(0,1fr)_32px] tw-items-center tw-gap-2">
        <button
          type="button"
          className={cn(
            "tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-self-center tw-rounded-full tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-[0.78rem] tw-text-vm-slate-600 tw-shadow-[0_6px_14px_rgba(15,23,42,0.08)] tw-transition-opacity hover:tw-bg-vm-slate-25",
            empty
              ? "tw-pointer-events-none tw-opacity-0"
              : "tw-pointer-events-none tw-opacity-0 group-hover:tw-pointer-events-auto group-hover:tw-opacity-100 focus-visible:tw-pointer-events-auto focus-visible:tw-opacity-100",
          )}
          disabled={empty}
          aria-label={`Cuộn ${title} sang trái`}
          onClick={() => scroll("left")}
        >
          <i className="fas fa-chevron-left" />
        </button>
        <div
          ref={scrollRef}
          className="tw-flex tw-min-w-0 tw-gap-3 tw-overflow-x-auto tw-scroll-smooth tw-pb-1 tw-[scrollbar-width:none] tw-[-ms-overflow-style:none] [&::-webkit-scrollbar]:tw-hidden"
        >
          {children}
        </div>
        <button
          type="button"
          className={cn(
            "tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-self-center tw-rounded-full tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-[0.78rem] tw-text-vm-slate-600 tw-shadow-[0_6px_14px_rgba(15,23,42,0.08)] tw-transition-opacity hover:tw-bg-vm-slate-25",
            empty
              ? "tw-pointer-events-none tw-opacity-0"
              : "tw-pointer-events-none tw-opacity-0 group-hover:tw-pointer-events-auto group-hover:tw-opacity-100 focus-visible:tw-pointer-events-auto focus-visible:tw-opacity-100",
          )}
          disabled={empty}
          aria-label={`Cuộn ${title} sang phải`}
          onClick={() => scroll("right")}
        >
          <i className="fas fa-chevron-right" />
        </button>
      </div>
    </section>
  );
}

function Field({ children, label }: { children: ReactNode; label: string }) {
  return (
    <label className="tw-grid tw-gap-1.5">
      <span className="tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700">{label}</span>
      {children}
    </label>
  );
}

export function CustomerListPage() {
  const toast = useToast();
  const [activeSegment, setActiveSegment] = useState<SegmentValue>("all");
  const [customers, setCustomers] = useState<CustomerAdminResponse[]>([]);
  const [subscriptionCards, setSubscriptionCards] = useState<CustomerSubscriptionCardResponse[]>([]);
  const [vehicles, setVehicles] = useState<CustomerVehicleAdminResponse[]>([]);
  const [vehicleTypes, setVehicleTypes] = useState<VehicleTypeResponse[]>([]);
  const [keyword, setKeyword] = useState("");
  const [selectedCustomerId, setSelectedCustomerId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [form, setForm] = useState<CustomerFormState | null>(null);
  const [customerPage, setCustomerPage] = useState(1);
  const [customerPageSize, setCustomerPageSize] = useState(5);
  const [vehicleManagerOpen, setVehicleManagerOpen] = useState(false);
  const [editingVehicle, setEditingVehicle] = useState<CustomerVehicleAdminResponse | null>(null);
  const [vehicleForm, setVehicleForm] = useState<VehicleFormState>(toVehicleForm());

  const loadCustomers = useCallback(async () => {
    setLoading(true);
    try {
      const data = await fetchCustomers({ keyword: keyword.trim() || undefined });
      setCustomers(data);
      setSelectedCustomerId((current) => (current && data.some((item) => item.customerId === current) ? current : data[0]?.customerId ?? null));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không tải được danh sách khách hàng.", "Tải dữ liệu thất bại");
    } finally {
      setLoading(false);
    }
  }, [keyword, toast]);

  useEffect(() => {
    const timeout = window.setTimeout(loadCustomers, 300);
    return () => window.clearTimeout(timeout);
  }, [loadCustomers]);

  useEffect(() => {
    fetchVehicleTypes()
      .then(setVehicleTypes)
      .catch(() => setVehicleTypes([]));
  }, []);

  const selectedCustomer = useMemo(
    () => customers.find((customer) => customer.customerId === selectedCustomerId) ?? customers[0] ?? null,
    [customers, selectedCustomerId],
  );

  useEffect(() => {
    if (!selectedCustomer?.customerId) {
      setVehicles([]);
      setSubscriptionCards([]);
      return;
    }

    Promise.all([
      fetchCustomerVehicles(selectedCustomer.customerId),
      fetchCustomerSubscriptionCards(selectedCustomer.customerId, selectedCustomer.customerCode),
    ])
      .then(([nextVehicles, nextSubscriptionCards]) => {
        setVehicles(nextVehicles);
        setSubscriptionCards(nextSubscriptionCards);
      })
      .catch((error) => {
        setVehicles([]);
        setSubscriptionCards([]);
        toast.error(error instanceof Error ? error.message : "Không tải được tài sản liên kết.", "Tải dữ liệu thất bại");
      });
  }, [selectedCustomer?.customerId, toast]);

  const filteredCustomers = useMemo(() => {
    if (activeSegment === "approved") return customers.filter((customer) => customer.approvalStatus === "APPROVED");
    if (activeSegment === "pending") return customers.filter((customer) => customer.approvalStatus === "PENDING");
    if (activeSegment === "vip") return customers.filter((customer) => customer.customerType === "VIP");
    return customers;
  }, [activeSegment, customers]);

  useEffect(() => {
    setCustomerPage(1);
  }, [activeSegment, keyword]);

  const customerTotalPages = Math.max(1, Math.ceil(filteredCustomers.length / customerPageSize));
  const normalizedCustomerPage = Math.min(customerPage, customerTotalPages);
  const customerStartIndex = filteredCustomers.length === 0 ? 0 : (normalizedCustomerPage - 1) * customerPageSize + 1;
  const customerEndIndex = filteredCustomers.length === 0 ? 0 : Math.min(filteredCustomers.length, customerStartIndex + customerPageSize - 1);
  const pagedCustomers = useMemo(() => {
    const start = (normalizedCustomerPage - 1) * customerPageSize;
    return filteredCustomers.slice(start, start + customerPageSize);
  }, [customerPageSize, filteredCustomers, normalizedCustomerPage]);

  const metrics = useMemo(() => {
    const pending = customers.filter((customer) => customer.approvalStatus === "PENDING").length;
    const vip = customers.filter((customer) => customer.customerType === "VIP").length;
    const activeVehicles = vehicles.filter((vehicle) => vehicle.status === "ACTIVE").length;
    return {
      activeTickets: "--",
      linkedVehicles: formatNumber(activeVehicles),
      pending: formatNumber(pending),
      vip: formatNumber(vip),
    };
  }, [customers, vehicles]);

  const ticketAssets = useMemo<LinkedAsset[]>(
    () =>
      subscriptionCards.slice(0, 4).map((card) => ({
        icon: "far fa-calendar-check",
        meta: [
          `Mã thẻ: ${card.cardNumber || card.uid || "--"}`,
          card.effectiveTo ? `Hết hạn ${card.effectiveTo}` : null,
          card.licensePlate ? `Biển số ${card.licensePlate}` : null,
        ].filter(Boolean).join(" · "),
        status: subscriptionStatusLabel(card.subscriptionStatus),
        statusTone: subscriptionStatusTone(card.subscriptionStatus),
        title: card.ticketTypeName || "Vé tháng",
      })),
    [subscriptionCards],
  );

  const vehicleAssets = useMemo<LinkedAsset[]>(() => {
    if (vehicles.length === 0) {
      return [{ icon: "fas fa-car", meta: "Chưa có xe liên kết cho khách hàng này", status: "Trống", statusTone: "neutral", title: "Xe" }];
    }

    return vehicles.slice(0, 4).map((vehicle) => ({
      icon: "fas fa-car",
      meta: `${getVehicleTypeName(vehicleTypes, vehicle.vehicleTypeId)} · ${vehicle.brand || "--"} · ${vehicle.color || "--"}`,
      status: vehicle.isDefault ? "Mặc định" : vehicleStatusLabel(vehicle.status),
      statusTone: vehicle.isDefault ? "success" : vehicleStatusTone(vehicle.status),
      title: vehicle.licensePlate,
    }));
  }, [vehicleTypes, vehicles]);

  const selectedName = getCustomerName(selectedCustomer);
  const selectedCode = getCustomerCode(selectedCustomer);

  function openEditModal() {
    if (!selectedCustomer) return;
    setForm(toCustomerForm(selectedCustomer));
    setEditOpen(true);
  }

  async function handleSaveCustomer() {
    if (!selectedCustomer || !form) return;
    if (!form.fullName.trim()) {
      toast.error("Vui lòng nhập họ tên khách hàng.", "Thiếu thông tin");
      return;
    }

    setSubmitting(true);
    try {
      await updateCustomerAdminProfile(selectedCustomer.customerId, {
        customer: { customerType: form.customerType },
        userProfile: {
          address: form.address.trim() || null,
          dateOfBirth: form.dateOfBirth || null,
          fullName: form.fullName.trim(),
          gender: form.gender.trim() || null,
          identifyCard: form.identifyCard.trim() || null,
          phoneNumber: form.phoneNumber.trim() || null,
          status: selectedCustomer.userProfile?.status ?? null,
        },
      });
      const refreshedCustomer = await fetchCustomerById(selectedCustomer.customerId);
      setCustomers((current) => current.map((customer) => (customer.customerId === selectedCustomer.customerId ? refreshedCustomer : customer)));
      setEditOpen(false);
      toast.success("Đã cập nhật thông tin khách hàng.", "Cập nhật thành công");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không cập nhật được khách hàng.", "Thao tác thất bại");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleToggleCustomerStatus() {
    if (!selectedCustomer) return;
    setSubmitting(true);
    try {
      if (selectedCustomer.status === "ACTIVE") {
        await inactivateCustomer(selectedCustomer.customerId);
      } else {
        await activateCustomer(selectedCustomer.customerId);
      }
      const refreshedCustomer = await fetchCustomerById(selectedCustomer.customerId);
      setCustomers((current) => current.map((customer) => (customer.customerId === refreshedCustomer.customerId ? refreshedCustomer : customer)));
      toast.success(refreshedCustomer.status === "ACTIVE" ? "Đã kích hoạt lại khách hàng." : "Đã tạm ngưng khách hàng.", "Cập nhật thành công");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không cập nhật được trạng thái khách hàng.", "Thao tác thất bại");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleApproveSelectedCustomer() {
    if (!selectedCustomer) return;
    if (selectedCustomer.approvalStatus !== "PENDING") {
      toast.info("Khách hàng này không ở trạng thái chờ duyệt.", "Không cần duyệt");
      return;
    }

    setSubmitting(true);
    try {
      const approvals = await fetchCustomerOnboardingApprovals({
        keyword: getCustomerCode(selectedCustomer),
        status: "PENDING",
      });
      const approval = approvals.find((item) => item.customer?.customerId === selectedCustomer.customerId);
      if (!approval?.request?.approvalRequestId) {
        toast.error("Không tìm thấy yêu cầu duyệt đang mở của khách hàng này.", "Không thể duyệt");
        return;
      }

      await approveCustomerOnboardingApproval(approval.request.approvalRequestId);
      await loadCustomers();
      toast.success("Đã duyệt đăng ký khách hàng.", "Duyệt thành công");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không duyệt được khách hàng.", "Thao tác thất bại");
    } finally {
      setSubmitting(false);
    }
  }

  async function refreshSelectedVehicles() {
    if (!selectedCustomer?.customerId) {
      setVehicles([]);
      return;
    }
    const nextVehicles = await fetchCustomerVehicles(selectedCustomer.customerId);
    setVehicles(nextVehicles);
  }

  function openVehicleManager() {
    setEditingVehicle(null);
    setVehicleForm(toVehicleForm());
    setVehicleManagerOpen(true);
  }

  function editVehicle(vehicle: CustomerVehicleAdminResponse) {
    setEditingVehicle(vehicle);
    setVehicleForm(toVehicleForm(vehicle));
  }

  function resetVehicleEditor() {
    setEditingVehicle(null);
    setVehicleForm(toVehicleForm());
  }

  async function handleSaveVehicle() {
    if (!selectedCustomer) return;
    if (!vehicleForm.licensePlate.trim()) {
      toast.error("Vui lòng nhập biển số xe.", "Thiếu thông tin");
      return;
    }
    if (!vehicleForm.vehicleTypeId) {
      toast.error("Vui lòng chọn loại phương tiện.", "Thiếu thông tin");
      return;
    }

    const payload: CustomerVehiclePayload = {
      brand: vehicleForm.brand.trim() || null,
      color: vehicleForm.color.trim() || null,
      isDefault: vehicleForm.isDefault,
      licensePlate: vehicleForm.licensePlate.trim().toUpperCase(),
      vehicleTypeId: vehicleForm.vehicleTypeId,
    };

    setSubmitting(true);
    try {
      if (editingVehicle) {
        await updateCustomerVehicle(editingVehicle.customerVehicleId, payload);
        toast.success("Đã cập nhật xe liên kết.", "Cập nhật thành công");
      } else {
        await createCustomerVehicle(selectedCustomer.customerId, payload);
        toast.success("Đã thêm xe liên kết.", "Thêm mới thành công");
      }
      await refreshSelectedVehicles();
      resetVehicleEditor();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không lưu được xe liên kết.", "Thao tác thất bại");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteVehicle(vehicle: CustomerVehicleAdminResponse) {
    setSubmitting(true);
    try {
      await deleteCustomerVehicle(vehicle.customerVehicleId);
      await refreshSelectedVehicles();
      if (editingVehicle?.customerVehicleId === vehicle.customerVehicleId) {
        resetVehicleEditor();
      }
      toast.success("Đã xóa xe liên kết.", "Xóa thành công");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không xóa được xe liên kết.", "Thao tác thất bại");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleToggleVehicle(vehicle: CustomerVehicleAdminResponse) {
    setSubmitting(true);
    try {
      if (vehicle.status === "ACTIVE") {
        await inactivateCustomerVehicle(vehicle.customerVehicleId);
      } else {
        await activateCustomerVehicle(vehicle.customerVehicleId);
      }
      await refreshSelectedVehicles();
      toast.success("Đã cập nhật trạng thái xe.", "Cập nhật thành công");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không cập nhật được trạng thái xe.", "Thao tác thất bại");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleMarkDefaultVehicle(vehicle: CustomerVehicleAdminResponse) {
    setSubmitting(true);
    try {
      await markCustomerVehicleAsDefault(vehicle.customerVehicleId);
      await refreshSelectedVehicles();
      toast.success("Đã đặt xe mặc định.", "Cập nhật thành công");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không đặt được xe mặc định.", "Thao tác thất bại");
    } finally {
      setSubmitting(false);
    }
  }

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
            <Button size="lg" variant="primary" onClick={() => setActiveSegment("pending")}>
              <i className="fas fa-check" />
              Duyệt đăng ký
            </Button>
            <Button size="lg" variant="secondary" onClick={loadCustomers} disabled={loading}>
              <i className="fas fa-download" />
              Xuất dữ liệu
              <i className="fas fa-chevron-down tw-text-[0.72rem]" />
            </Button>
          </div>
        </div>

        <InfoBanner
          tone="warning"
          title="Chờ duyệt đăng ký mới"
          description={`Hiện có ${metrics.pending} đăng ký khách hàng đang chờ duyệt.`}
          icon={<i className="fas fa-exclamation-triangle" />}
          action={
            <button type="button" className="tw-border-0 tw-bg-transparent tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-700 hover:tw-text-vm-primary" onClick={() => setActiveSegment("pending")}>
              Xem danh sách <i className="fas fa-chevron-right tw-ml-2 tw-text-[0.68rem]" />
            </button>
          }
        />

        <div className="tw-mt-4 tw-grid tw-grid-cols-4 tw-gap-4 max-[1180px]:tw-grid-cols-2">
          <CustomerMetric icon="far fa-clock" iconClassName="tw-bg-amber-50 tw-text-amber-500" label="Chờ duyệt" value={metrics.pending} />
          <CustomerMetric icon="fas fa-crown" iconClassName="tw-bg-violet-50 tw-text-violet-600" label="VIP" value={metrics.vip} />
          <CustomerMetric icon="fas fa-car" iconClassName="tw-bg-brand-100 tw-text-vm-primary" label="Xe liên kết" value={metrics.linkedVehicles} />
          <CustomerMetric icon="far fa-credit-card" iconClassName="tw-bg-green-50 tw-text-green-600" label="Vé đang hiệu lực" value={metrics.activeTickets} />
        </div>

        <div className="tw-mt-5 tw-grid tw-grid-cols-[370px_minmax(0,1fr)_240px] tw-items-stretch tw-gap-4 max-[1280px]:tw-grid-cols-[350px_minmax(0,1fr)] max-[960px]:tw-grid-cols-1">
          <Card className="tw-flex tw-h-full tw-min-h-0 tw-flex-col tw-overflow-hidden">
            <div className="tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-py-4">
              <h2 className="tw-m-0 tw-text-[0.95rem] tw-font-extrabold tw-text-vm-slate-900">Danh sách khách hàng</h2>
              <label className="tw-mt-3 tw-flex tw-h-[38px] tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3">
                <i className="fas fa-search tw-text-[0.82rem] tw-text-vm-slate-500" />
                <input
                  className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none placeholder:tw-text-vm-slate-500"
                  placeholder="Tìm tên, mã KH, biển số..."
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                />
              </label>
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
              {loading ? (
                <div className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-4 tw-text-center tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-500">Đang tải khách hàng...</div>
              ) : filteredCustomers.length === 0 ? (
                <div className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-4 tw-text-center tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-500">Không có khách hàng phù hợp.</div>
              ) : (
                pagedCustomers.map((customer) => (
                  <CustomerListItem
                    key={customer.customerId}
                    customer={customer}
                    onContact={() =>
                      openSupportCenterConversation({
                        participantId: getCustomerCode(customer),
                        participantName: getCustomerName(customer),
                        participantType: "customer",
                      })
                    }
                    selected={customer.customerId === selectedCustomer?.customerId}
                    onSelect={() => setSelectedCustomerId(customer.customerId)}
                  />
                ))
              )}
            </div>

            <PaginationFooter
              ariaLabel="Phan trang khach hang"
              className="tw-flex-shrink-0 !tw-flex-col !tw-items-stretch !tw-gap-2 !tw-px-3 !tw-pb-3 !tw-pt-2 [&_p]:tw-text-[0.78rem] [&_label]:tw-text-[0.78rem]"
              currentPage={normalizedCustomerPage}
              endIndex={customerEndIndex}
              onPageChange={setCustomerPage}
              onPageSizeChange={(nextPageSize) => {
                setCustomerPageSize(nextPageSize);
                setCustomerPage(1);
              }}
              pageSize={customerPageSize}
              pageSizeOptions={[5, 10, 20]}
              startIndex={customerStartIndex}
              totalPages={customerTotalPages}
              totalRecords={filteredCustomers.length}
            />
            <div className="tw-hidden tw-flex-shrink-0 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-px-3 tw-py-2.5">
              <Button
                className="tw-h-9 tw-w-full tw-text-[0.82rem]"
                variant="secondary"
                onClick={() => {
                  setActiveSegment("all");
                  setKeyword("");
                }}
              >
                Xem tất cả khách hàng
              </Button>
            </div>
          </Card>

          <div className="tw-grid tw-gap-4">
            <Card className="tw-p-4">
              <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Hồ sơ nhanh</h2>
              {selectedCustomer ? (
                <>
                  <div className="tw-mt-3 tw-grid tw-grid-cols-[56px_minmax(0,1fr)] tw-gap-4">
                    <EntityAvatar initials={getInitials(selectedName)} size="lg" />
                    <div className="tw-min-w-0">
                      <h3 className="tw-m-0 tw-text-[1.14rem] tw-font-extrabold tw-leading-tight tw-text-vm-slate-900">{selectedName}</h3>
                      <p className="tw-m-0 tw-mt-0.5 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">{selectedCode}</p>
                      <div className="tw-mt-2 tw-flex tw-flex-wrap tw-gap-1.5">
                        <Badge tone={selectedCustomer.customerType === "VIP" ? "primary" : "neutral"} className="tw-rounded-full tw-px-2.5 tw-text-[0.66rem]">
                          {customerTypeLabel(selectedCustomer.customerType)}
                        </Badge>
                        <Badge tone={approvalBadgeTone(selectedCustomer.approvalStatus)} className="tw-rounded-full tw-px-2.5 tw-text-[0.66rem]">
                          {approvalLabel(selectedCustomer.approvalStatus)}
                        </Badge>
                        <Badge tone={selectedCustomer.status === "ACTIVE" ? "success" : "danger"} className="tw-rounded-full tw-px-2.5 tw-text-[0.66rem]">
                          {customerStatusLabel(selectedCustomer.status)}
                        </Badge>
                      </div>
                    </div>
                  </div>

                  <div className="tw-mt-3 tw-grid tw-gap-2">
                    <QuickInfoRow icon="far fa-envelope" label={selectedCustomer.accountEmail || "--"} />
                    <QuickInfoRow icon="fas fa-phone" label={selectedCustomer.userProfile?.phoneNumber || "--"} />
                    <QuickInfoRow icon="fas fa-map-marker-alt" label={selectedCustomer.userProfile?.address || "--"} />
                  </div>

                  <div className="tw-mt-3 tw-flex tw-flex-wrap tw-gap-2">
                    <Button
                      size="sm"
                      variant="primary"
                      onClick={() =>
                        openSupportCenterConversation({
                          participantId: selectedCode,
                          participantName: selectedName,
                          participantType: "customer",
                        })
                      }
                    >
                      <i className="far fa-comment-dots" />
                      Liên hệ
                    </Button>
                    <Button size="sm" variant="secondary" onClick={openEditModal}>
                      <i className="fas fa-pen" />
                      Cập nhật
                    </Button>
                    <Button size="sm" variant="secondary" onClick={handleApproveSelectedCustomer} disabled={submitting}>
                      <i className="fas fa-check" />
                      Duyệt
                    </Button>
                    <Button size="sm" variant={selectedCustomer.status === "ACTIVE" ? "danger" : "primary"} onClick={handleToggleCustomerStatus} disabled={submitting}>
                      <i className={selectedCustomer.status === "ACTIVE" ? "fas fa-pause" : "fas fa-play"} />
                      {selectedCustomer.status === "ACTIVE" ? "Tạm ngưng" : "Kích hoạt"}
                    </Button>
                  </div>
                </>
              ) : (
                <p className="tw-mb-0 tw-mt-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-500">Chưa có khách hàng để hiển thị.</p>
              )}
            </Card>

            <Card className="tw-p-5">
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-4">
                <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Tài sản liên kết</h2>
              </div>

              <div className="tw-mt-4 tw-grid tw-gap-4">
                <AssetCarousel accentClassName="tw-bg-vm-primary" empty={ticketAssets.length === 0} title="Vé">
                  {ticketAssets.length > 0 ? (
                    ticketAssets.map((asset) => (
                      <LinkedAssetCard asset={asset} key={`${asset.title}-${asset.meta}`} />
                    ))
                  ) : (
                    <EmptyLinkedAssetState icon="far fa-calendar-check" message="Khách hàng chưa có vé tháng đang liên kết." />
                  )}
                </AssetCarousel>

                <AssetCarousel accentClassName="tw-bg-green-500" empty={vehicles.length === 0} title="Xe" onMore={openVehicleManager}>
                  {vehicles.length > 0 ? (
                    vehicleAssets.map((asset) => (
                      <LinkedAssetCard asset={asset} compact key={`${asset.title}-${asset.status}`} />
                    ))
                  ) : (
                    <EmptyLinkedAssetState icon="fas fa-car" message="Khách hàng chưa có xe liên kết." />
                  )}
                </AssetCarousel>
              </div>

              <div className="tw-hidden tw-mt-4 tw-grid tw-gap-4">
                <section>
                  <div className="tw-mb-2 tw-flex tw-items-center tw-gap-2">
                    <span className="tw-h-2 tw-w-2 tw-rounded-full tw-bg-vm-primary" />
                    <h3 className="tw-m-0 tw-text-[0.82rem] tw-font-extrabold tw-uppercase tw-tracking-[0.04em] tw-text-vm-slate-500">Vé</h3>
                  </div>
                  <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[1180px]:tw-grid-cols-2 max-[620px]:tw-grid-cols-1">
                    {ticketAssets.length > 0 ? (
                      ticketAssets.map((asset) => (
                        <LinkedAssetCard asset={asset} key={`${asset.title}-${asset.meta}`} />
                      ))
                    ) : (
                      <EmptyLinkedAssetState icon="far fa-calendar-check" message="Khách hàng chưa có vé tháng đang liên kết." />
                    )}
                  </div>
                </section>

                <section>
                  <div className="tw-mb-2 tw-flex tw-items-center tw-gap-2">
                    <span className="tw-h-2 tw-w-2 tw-rounded-full tw-bg-green-500" />
                    <h3 className="tw-m-0 tw-text-[0.82rem] tw-font-extrabold tw-uppercase tw-tracking-[0.04em] tw-text-vm-slate-500">Xe</h3>
                  </div>
                  <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[1180px]:tw-grid-cols-2 max-[620px]:tw-grid-cols-1">
                    {vehicles.length > 0 ? (
                      vehicleAssets.map((asset) => (
                      <LinkedAssetCard asset={asset} compact key={`${asset.title}-${asset.status}`} />
                      ))
                    ) : (
                      <EmptyLinkedAssetState icon="fas fa-car" message="Khách hàng chưa có xe liên kết." />
                    )}
                  </div>
                </section>
              </div>

              <Button className="tw-mt-4 tw-hidden tw-w-full" variant="secondary">
                Xem tất cả tài sản
              </Button>
            </Card>
          </div>

          <Card className="tw-flex tw-h-full tw-min-h-0 tw-flex-col tw-overflow-hidden tw-p-5 max-[1280px]:tw-col-span-2 max-[960px]:tw-col-span-1">
            <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Hoạt động gần đây</h2>
            <div className="tw-mt-5 tw-grid tw-min-h-0 tw-flex-1 tw-gap-0 tw-overflow-y-auto tw-pr-1 tw-[scrollbar-width:none] tw-[-ms-overflow-style:none] [&::-webkit-scrollbar]:tw-hidden">
              {emptyActivities.map((activity, index) => (
                <div className="tw-grid tw-grid-cols-[18px_minmax(0,1fr)] tw-gap-3" key={`${activity.date}-${activity.title}`}>
                  <div className="tw-relative tw-flex tw-justify-center">
                    <span className="tw-mt-1.5 tw-h-2.5 tw-w-2.5 tw-rounded-full" style={{ backgroundColor: activity.color }} />
                    {index < emptyActivities.length - 1 ? <span className="tw-absolute tw-bottom-0 tw-top-5 tw-w-px tw-bg-vm-slate-100" /> : null}
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
            <Button className="tw-mt-5 tw-hidden tw-w-full" variant="secondary">
              Xem tất cả hoạt động
            </Button>
          </Card>
        </div>
      </section>

      <Modal
        open={editOpen}
        onClose={() => setEditOpen(false)}
        title="Cập nhật khách hàng"
        description={selectedCustomer ? `${selectedCode} · ${selectedName}` : undefined}
        width="lg"
        actions={
          <div className="tw-flex tw-justify-end tw-gap-2">
            <Button variant="secondary" onClick={() => setEditOpen(false)} disabled={submitting}>
              Hủy
            </Button>
            <Button onClick={handleSaveCustomer} disabled={submitting}>
              <i className="fas fa-save" />
              Lưu thay đổi
            </Button>
          </div>
        }
      >
        {form ? (
          <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[640px]:tw-grid-cols-1">
            <Field label="Họ và tên">
              <Input value={form.fullName} onChange={(event) => setForm((current) => (current ? { ...current, fullName: event.target.value } : current))} />
            </Field>
            <Field label="Số điện thoại">
              <Input value={form.phoneNumber} onChange={(event) => setForm((current) => (current ? { ...current, phoneNumber: event.target.value } : current))} />
            </Field>
            <Field label="Nhóm khách hàng">
              <SelectMenu
                ariaLabel="Nhóm khách hàng"
                clearValue="REGISTERED"
                options={customerTypeOptions}
                value={form.customerType}
                onChange={(value) => setForm((current) => (current ? { ...current, customerType: value as CustomerType } : current))}
              />
            </Field>
            <Field label="Ngày sinh">
              <Input type="date" value={form.dateOfBirth} onChange={(event) => setForm((current) => (current ? { ...current, dateOfBirth: event.target.value } : current))} />
            </Field>
            <Field label="Giới tính">
              <Input value={form.gender} onChange={(event) => setForm((current) => (current ? { ...current, gender: event.target.value } : current))} />
            </Field>
            <Field label="CCCD/CMND">
              <Input value={form.identifyCard} onChange={(event) => setForm((current) => (current ? { ...current, identifyCard: event.target.value } : current))} />
            </Field>
            <div className="tw-col-span-2 max-[640px]:tw-col-span-1">
              <Field label="Địa chỉ">
                <Input value={form.address} onChange={(event) => setForm((current) => (current ? { ...current, address: event.target.value } : current))} />
              </Field>
            </div>
          </div>
        ) : null}
      </Modal>

      <Modal
        open={vehicleManagerOpen}
        onClose={() => setVehicleManagerOpen(false)}
        title="Quản lý xe liên kết"
        description={selectedCustomer ? `${selectedCode} · ${selectedName}` : undefined}
        width="lg"
        actions={
          <div className="tw-flex tw-justify-end tw-gap-2">
            <Button variant="secondary" onClick={() => setVehicleManagerOpen(false)} disabled={submitting}>
              Đóng
            </Button>
          </div>
        }
      >
        <div className="tw-grid tw-gap-4">
          <Card className="tw-p-4">
            <h3 className="tw-m-0 tw-text-[0.96rem] tw-font-black tw-text-vm-slate-900">{editingVehicle ? "Cập nhật xe" : "Thêm xe"}</h3>
            <div className="tw-mt-3 tw-grid tw-grid-cols-2 tw-gap-3 max-[640px]:tw-grid-cols-1">
              <Field label="Biển số">
                <Input value={vehicleForm.licensePlate} onChange={(event) => setVehicleForm((current) => ({ ...current, licensePlate: event.target.value }))} />
              </Field>
              <Field label="Loại phương tiện">
                <SelectMenu
                  ariaLabel="Loại phương tiện"
                  clearValue=""
                  options={vehicleTypes.map((item) => ({ label: `${item.name} (${item.code})`, value: item.vehicleTypeId }))}
                  value={vehicleForm.vehicleTypeId}
                  onChange={(value) => setVehicleForm((current) => ({ ...current, vehicleTypeId: value }))}
                />
              </Field>
              <Field label="Hãng xe">
                <Input value={vehicleForm.brand} onChange={(event) => setVehicleForm((current) => ({ ...current, brand: event.target.value }))} />
              </Field>
              <Field label="Màu xe">
                <Input value={vehicleForm.color} onChange={(event) => setVehicleForm((current) => ({ ...current, color: event.target.value }))} />
              </Field>
            </div>
            <div className="tw-mt-3 tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-3">
              <label className="tw-flex tw-items-center tw-gap-2 tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-700">
                <input
                  type="checkbox"
                  checked={vehicleForm.isDefault}
                  onChange={(event) => setVehicleForm((current) => ({ ...current, isDefault: event.target.checked }))}
                />
                Đặt làm xe mặc định
              </label>
              <div className="tw-flex tw-gap-2">
                {editingVehicle ? (
                  <Button size="sm" variant="secondary" onClick={resetVehicleEditor} disabled={submitting}>
                    Hủy sửa
                  </Button>
                ) : null}
                <Button size="sm" onClick={handleSaveVehicle} disabled={submitting}>
                  <i className="fas fa-save" />
                  {editingVehicle ? "Lưu xe" : "Thêm xe"}
                </Button>
              </div>
            </div>
          </Card>

          <div className="tw-grid tw-gap-2">
            {vehicles.length === 0 ? (
              <EmptyLinkedAssetState icon="fas fa-car" message="Khách hàng chưa có xe liên kết." />
            ) : (
              vehicles.map((vehicle) => (
                <article key={vehicle.customerVehicleId} className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
                  <div className="tw-flex tw-flex-wrap tw-items-start tw-justify-between tw-gap-3">
                    <div className="tw-min-w-0">
                      <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2">
                        <strong className="tw-text-[0.98rem] tw-font-black tw-text-vm-slate-900">{vehicle.licensePlate}</strong>
                        {vehicle.isDefault ? <Badge tone="primary">Mặc định</Badge> : null}
                        <Badge tone={vehicleStatusTone(vehicle.status)}>{vehicleStatusLabel(vehicle.status)}</Badge>
                      </div>
                      <p className="tw-mb-0 tw-mt-1 tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-500">
                        {getVehicleTypeName(vehicleTypes, vehicle.vehicleTypeId)} · {vehicle.brand || "--"} · {vehicle.color || "--"}
                      </p>
                    </div>
                    <div className="tw-flex tw-flex-wrap tw-gap-2">
                      <Button size="sm" variant="secondary" onClick={() => editVehicle(vehicle)} disabled={submitting}>
                        <i className="fas fa-pen" />
                        Sửa
                      </Button>
                      {!vehicle.isDefault ? (
                        <Button size="sm" variant="secondary" onClick={() => handleMarkDefaultVehicle(vehicle)} disabled={submitting}>
                          <i className="fas fa-star" />
                          Mặc định
                        </Button>
                      ) : null}
                      <Button size="sm" variant={vehicle.status === "ACTIVE" ? "danger" : "primary"} onClick={() => handleToggleVehicle(vehicle)} disabled={submitting}>
                        <i className={vehicle.status === "ACTIVE" ? "fas fa-pause" : "fas fa-play"} />
                        {vehicle.status === "ACTIVE" ? "Ngưng" : "Kích hoạt"}
                      </Button>
                      <Button size="sm" variant="danger" onClick={() => handleDeleteVehicle(vehicle)} disabled={submitting}>
                        <i className="fas fa-trash" />
                        Xóa
                      </Button>
                    </div>
                  </div>
                </article>
              ))
            )}
          </div>
        </div>
      </Modal>
    </div>
  );
}
