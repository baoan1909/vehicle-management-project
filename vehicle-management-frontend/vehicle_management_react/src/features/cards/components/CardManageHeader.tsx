interface CardManageHeaderProps {
  canCreate: boolean;
  onCreate: () => void;
  onCreateBatch: () => void;
}

export function CardManageHeader({ canCreate, onCreate, onCreateBatch }: CardManageHeaderProps) {
  return (
    <div className="tw-flex tw-items-center tw-justify-between tw-gap-4 tw-pb-[0.1rem] max-[760px]:tw-flex-col max-[760px]:tw-items-stretch">
      <div className="tw-flex tw-items-center tw-gap-[1.05rem] max-[760px]:tw-flex-col max-[760px]:tw-items-start">
        <h2 className="tw-m-0 tw-text-[25px] tw-font-extrabold tw-leading-none tw-text-slate-900">Quản lý thẻ</h2>
        <button className="tw-inline-flex tw-min-h-0 tw-items-center tw-gap-[0.65rem] tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.93rem] tw-font-bold tw-leading-[1.1] tw-text-vm-primary" type="button">
          <i className="far fa-question-circle tw-text-base" />
          <span>Hướng dẫn &amp; Trợ giúp</span>
        </button>
      </div>

      <div className="tw-flex tw-flex-wrap tw-justify-end tw-gap-[0.65rem] max-[760px]:tw-justify-start">
        <button
          className="tw-inline-flex tw-min-h-[46px] tw-items-center tw-justify-center tw-gap-[0.6rem] tw-whitespace-nowrap tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-primary/30 tw-bg-white tw-px-5 tw-py-[0.7rem] tw-text-[0.95rem] tw-font-bold tw-text-vm-primary tw-transition-colors hover:tw-bg-brand-600/5 disabled:tw-cursor-not-allowed disabled:tw-border-slate-200 disabled:tw-bg-slate-50 disabled:tw-text-slate-400"
          disabled={!canCreate}
          type="button"
          onClick={onCreateBatch}
          title={canCreate ? undefined : "Bạn chỉ có quyền xem danh sách thẻ."}
        >
          <i className="fas fa-layer-group" />
          <span>Cấp hàng loạt</span>
        </button>
        <button
          className="tw-inline-flex tw-min-h-[46px] tw-items-center tw-justify-center tw-gap-[0.6rem] tw-whitespace-nowrap tw-rounded-vm-lg tw-border tw-border-solid tw-border-[#2563EB] tw-bg-[linear-gradient(135deg,#2563EB,#1D4ED8)] tw-px-5 tw-py-[0.7rem] tw-text-[0.95rem] tw-font-bold tw-text-white tw-shadow-[0_12px_22px_rgba(37,99,235,0.18)] tw-transition-all hover:tw-translate-y-px hover:tw-text-white hover:tw-shadow-[0_8px_16px_rgba(37,99,235,0.16)] active:tw-translate-y-0.5 active:tw-shadow-[0_5px_11px_rgba(37,99,235,0.14)] disabled:tw-cursor-not-allowed disabled:tw-border-slate-200 disabled:tw-bg-slate-100 disabled:tw-text-slate-400 disabled:tw-shadow-none"
          disabled={!canCreate}
          type="button"
          onClick={onCreate}
          title={canCreate ? undefined : "Bạn chỉ có quyền xem danh sách thẻ."}
        >
          <i className="far fa-plus-square" />
          <span>Cấp thẻ mới</span>
        </button>
      </div>
    </div>
  );
}
