interface ActionButtonsProps {
  editHref?: string;
  deleteHref?: string;
}

export function ActionButtons({ editHref = "#", deleteHref = "#" }: ActionButtonsProps) {
  return (
    <div className="vm-action-cell tw-flex tw-items-center tw-gap-2">
      <a href={editHref} className="btn vm-icon-btn vm-icon-btn-primary" aria-label="Chỉnh sửa"><i className="fas fa-pen-square" /></a>
      <a href={deleteHref} className="btn vm-icon-btn vm-icon-btn-danger" aria-label="Xóa"><i className="fas fa-trash-alt" /></a>
    </div>
  );
}
