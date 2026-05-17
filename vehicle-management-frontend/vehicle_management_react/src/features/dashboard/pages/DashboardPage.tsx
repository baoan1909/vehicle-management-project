import { AdminPage } from "../../../shared/components/layout/AdminPage";
import { dashboardStats } from "../../../shared/data/mockData";
import { formatCurrency } from "../../../shared/utils/format";

export function DashboardPage() {
  const points = dashboardStats.revenueByMonth.map((value, index) => `${(index / 11) * 100},${100 - value * 2.8}`).join(" ");

  return (
    <AdminPage title="Trang chủ" breadcrumbs={[{ label: "Quản lý", href: "/admin/dashboard" }, { label: "Trang chủ" }]}>
      <div className="container mt-4">
        <div className="col-12">
          <div className="card card-cyan card-outline">
            <div className="card-body">
              <div className="col-10 mx-auto">
                <div className="row info-box d-flex justify-content-center align-items-center shadow">
                  <div className="col-md-4 text-center d-flex flex-column justify-content-center">
                    <span className="info-box-text">Tổng doanh thu</span>
                    <span className="info-box-number">{formatCurrency(dashboardStats.totalRevenue)}</span>
                  </div>
                  <div className="col-md-4 text-center d-flex flex-column justify-content-center">
                    <span className="info-box-text">Khách vào bãi</span>
                    <span className="info-box-number">{dashboardStats.totalVisitors}</span>
                  </div>
                  <div className="col-md-4 text-center d-flex flex-column justify-content-center">
                    <span className="info-box-text">Thẻ đã đăng ký</span>
                    <span className="info-box-number">{dashboardStats.totalRegisteredCards}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="row">
          <div className="col-xl-8 col-lg-7">
            <div className="card card-cyan card-outline shadow mb-4">
              <div className="card-header">
                <h3 className="card-title">Tổng doanh thu</h3>
                <div className="card-tools">
                  <button type="button" className="btn btn-tool">
                    <i className="fas fa-minus" />
                  </button>
                </div>
              </div>
              <div className="card-body">
                <div className="vm-chart-area">
                  <svg viewBox="0 0 100 100" preserveAspectRatio="none" aria-label="Biểu đồ doanh thu">
                    <polyline fill="none" stroke="#17a2b8" strokeWidth="3" points={points} />
                    <polygon fill="rgba(23,162,184,.16)" points={`0,100 ${points} 100,100`} />
                  </svg>
                  <div className="vm-chart-axis"><span>T1</span><span>T3</span><span>T6</span><span>T9</span><span>T12</span></div>
                </div>
              </div>
            </div>
          </div>
          <div className="col-xl-4 col-lg-5">
            <div className="card card-cyan card-outline shadow mb-4">
              <div className="card-header">
                <h3 className="card-title">Tổng loại xe</h3>
                <div className="card-tools">
                  <button type="button" className="btn btn-tool">
                    <i className="fas fa-minus" />
                  </button>
                </div>
              </div>
              <div className="card-body">
                <div className="chart-pie pt-4 pb-2">
                  <div className="vm-pie-chart" aria-label="Biểu đồ loại xe" />
                </div>
                <div className="mt-4 text-center small">
                  {dashboardStats.vehicleTypeData.map((vehicle) => (
                    <span className="mr-2" key={vehicle.type}>
                      <i className={`fas fa-circle ${vehicle.className}`} /> {vehicle.type}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="row">
          <div className="col-lg-6 mb-4">
            <div className="card card-cyan card-outline shadow mb-4">
              <div className="card-header py-3"><h6 className="card-title">Thẻ</h6></div>
              <div className="card-body">
                {[
                  { label: "Mất", percent: dashboardStats.cardStats.lostCardsPercent, className: "bg-warning" },
                  { label: "Thành viên", percent: dashboardStats.cardStats.memberCardsPercent, className: "bg-info" },
                  { label: "Vãng lai", percent: dashboardStats.cardStats.visitorCardsPercent, className: "bg-success" },
                ].map(({ label, percent, className }) => (
                  <div key={label}>
                    <h4 className="small font-weight-bold">
                      {label}
                      <span className="float-right">{percent}%</span>
                    </h4>
                    <div className="progress mb-4">
                      <div className={`progress-bar ${className}`} role="progressbar" style={{ width: `${percent}%` }} />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
          <div className="col-lg-6 mb-4">
            <div className="card card-cyan card-outline shadow mb-4">
              <div className="card-header py-3"><h6 className="card-title">Thiết bị</h6></div>
              <div className="card-body">
                <div className="row">
                  {[
                    { label: "Camera", count: 50, icon: "fas fa-video", color: "info" },
                    { label: "Đầu đọc thẻ", count: 3, icon: "fas fa-sd-card", color: "warning" },
                    { label: "Máy tính", count: 4, icon: "fas fa-desktop", color: "success" },
                    { label: "Barie", count: 7, icon: "fas fa-pallet", color: "primary" },
                  ].map(({ label, count, icon, color }) => (
                    <div className="col-xl-6 col-md-6 mb-4" key={label}>
                      <div className={`card card-${color} card-outline shadow h-100 py-2`}>
                        <div className="card-body">
                          <div className="row no-gutters align-items-center">
                            <div className="col mr-2">
                              <div className={`text-xs font-weight-bold text-${color} text-uppercase mb-1`}>{label}</div>
                              <div className="h5 mb-0 mr-3 font-weight-bold text-gray-dark">{count}</div>
                            </div>
                            <div className="col-auto">
                              <i className={`${icon} fa-2x text-gray`} />
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </AdminPage>
  );
}
