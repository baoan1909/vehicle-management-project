interface ActionButtonsProps {
  editHref?: string;
  deleteHref?: string;
}

export function ActionButtons({ editHref = "#", deleteHref = "#" }: ActionButtonsProps) {
  return (
    <div className="row vm-action-cell">
      <div className="col-6">
        <a href={editHref} className="btn btn-info btn-block" aria-label="Chỉnh sửa"><i className="fas fa-pen-square" /></a>
      </div>
      <div className="col-6">
        <a href={deleteHref} className="btn btn-outline-warning btn-block" aria-label="Xóa"><i className="fas fa-trash-alt" /></a>
      </div>
    </div>
  );
}
