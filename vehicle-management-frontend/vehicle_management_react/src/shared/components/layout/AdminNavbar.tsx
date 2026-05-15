import { useState } from "react";
import { useAuth } from "../../../core/auth/useAuth";

export function AdminNavbar() {
  const { user } = useAuth();
  const [showNotifications, setShowNotifications] = useState(false);
  const [showUserMenu, setShowUserMenu] = useState(false);

  return (
    <nav className="main-header navbar navbar-expand navbar-white navbar-light vm-topbar tw-border-b tw-border-slate-200 tw-bg-white/95 tw-shadow-sm">
      <ul className="navbar-nav">
        <li className="nav-item">
          <a className="nav-link vm-topbar-icon" href="#/admin/dashboard" role="button">
            <i className="fas fa-bars" />
          </a>
        </li>
        <li className="nav-item d-none d-sm-inline-block">
          <a href="#/pricing" className="nav-link vm-topbar-link">Giới thiệu</a>
        </li>
      </ul>

      <ul className="navbar-nav ml-auto align-items-center">
        <li className="nav-item mr-2">
          <div className="input-group vm-navbar-search">
            <input className="form-control form-control-sidebar vm-search-input" type="search" placeholder="Tìm kiếm" aria-label="Search" />
            <div className="input-group-append">
              <button className="btn btn-sidebar vm-search-button" type="button" aria-label="Tìm kiếm">
                <i className="fas fa-search fa-fw" />
              </button>
            </div>
          </div>
        </li>

        <li className={`nav-item dropdown mr-2 ${showNotifications ? "show" : ""}`}>
          <button className="nav-link btn btn-link vm-topbar-icon" type="button" onClick={() => setShowNotifications((value) => !value)}>
            <i className="far fa-bell" />
            <span className="badge badge-warning navbar-badge">15</span>
          </button>
          <div className={`dropdown-menu dropdown-menu-lg dropdown-menu-right ${showNotifications ? "show" : ""}`}>
            <span className="dropdown-item dropdown-header">15 Thông báo</span>
            <div className="dropdown-divider" />
            <a href="#/admin/customer" className="dropdown-item"><i className="fas fa-envelope mr-2" /> 4 tin nhắn mới<span className="float-right text-muted text-sm">3 phút</span></a>
            <div className="dropdown-divider" />
            <a href="#/admin/customer" className="dropdown-item"><i className="fas fa-users mr-2" /> 8 yêu cầu đăng ký<span className="float-right text-muted text-sm">12 giờ</span></a>
            <div className="dropdown-divider" />
            <a href="#/admin/dashboard" className="dropdown-item"><i className="fas fa-file mr-2" /> 3 báo cáo mới<span className="float-right text-muted text-sm">2 ngày</span></a>
            <div className="dropdown-divider" />
            <a href="#/admin/dashboard" className="dropdown-item dropdown-footer">Xem tất cả thông báo</a>
          </div>
        </li>

        <li className={`nav-item dropdown ${showUserMenu ? "show" : ""}`}>
          <button className="user-panel d-flex h-100 btn btn-link p-0 vm-user-trigger" type="button" onClick={() => setShowUserMenu((value) => !value)}>
            <div className="image">
              <img src={user?.avatarUrl} className="img-circle elevation-2" alt="User" />
            </div>
            <div className="info">
              <span className="d-block"><i className="fas fa-sort-down" /></span>
            </div>
          </button>
          <div className={`dropdown-menu dropdown-menu-lg dropdown-menu-right ${showUserMenu ? "show" : ""}`}>
            <div className="dropdown-item dropdown-header">
              <div className="image">
                <img src={user?.avatarUrl} className="img-circle elevation-2 mt-3" style={{ width: 60 }} alt="User" />
              </div>
              <div className="info"><span className="d-block mt-3"><h5>{user?.username}</h5></span></div>
              <div className="info"><span className="d-block mt-3"><h5>Vai trò: {user?.role}</h5></span></div>
            </div>
            <div className="dropdown-divider" />
            <a href="#/admin/account/form" className="dropdown-item"><i className="fas fa-user-circle mr-2" /> Thông tin tài khoản</a>
            <div className="dropdown-divider" />
            <a href="#/contact" className="dropdown-item"><i className="fas fa-question-circle mr-2" /> Trợ giúp</a>
            <div className="dropdown-divider" />
            <a href="#/login" className="dropdown-item"><i className="fas fa-sign-out-alt mr-2" /> Đăng xuất</a>
          </div>
        </li>
      </ul>
    </nav>
  );
}
