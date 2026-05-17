import { Link } from "react-router-dom";
import { ClientPage } from "../../../shared/components/layout/ClientPage";
import { useAuth } from "../../../core/auth/useAuth";

export function ProfilePage() {
  const { user } = useAuth();

  return (
    <ClientPage title="Thông tin tài khoản">
      <div className="row justify-content-center mt-4">
        <div className="col-md-8">
          <div className="card card-cyan card-outline shadow">
            <div className="card-header">
              <h3 className="card-title">Hồ sơ khách hàng</h3>
            </div>
            <div className="card-body">
              <div className="row align-items-center">
                <div className="col-md-3 text-center">
                  <img src={user?.avatarUrl} className="img-circle elevation-2 mb-3" width={110} height={110} alt="User" />
                </div>
                <div className="col-md-9">
                  <dl className="row mb-0">
                    <dt className="col-sm-4">Họ tên</dt>
                    <dd className="col-sm-8">{user?.fullName}</dd>
                    <dt className="col-sm-4">Tên đăng nhập</dt>
                    <dd className="col-sm-8">{user?.username}</dd>
                    <dt className="col-sm-4">Vai trò</dt>
                    <dd className="col-sm-8">{user?.role}</dd>
                    <dt className="col-sm-4">Biển số mặc định</dt>
                    <dd className="col-sm-8">60K8-2301</dd>
                  </dl>
                </div>
              </div>
            </div>
            <div className="card-footer">
              <Link to="/customerTicket/customer-infor" className="btn btn-info">
                Xem lịch sử gửi xe
              </Link>
            </div>
          </div>
        </div>
      </div>
    </ClientPage>
  );
}
