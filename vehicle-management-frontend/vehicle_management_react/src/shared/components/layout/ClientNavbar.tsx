import { useState } from "react";
import { Link } from "react-router-dom";
import { clientNavigation } from "../../../config/navigation";
import { useAuth } from "../../../core/auth/useAuth";

export function ClientNavbar() {
  const { user } = useAuth();
  const [open, setOpen] = useState(false);

  return (
    <nav className="main-header navbar navbar-expand-md navbar-light navbar-white">
      <div className="container">
        <Link to="/pricing" className="navbar-brand">
          <img src="/assets/admin/dist/img/AdminLTELogo.png" alt="AdminLTE Logo" className="brand-image img-circle elevation-3" style={{ opacity: 0.8 }} />
          <span className="brand-text font-weight-light">Admin</span>
        </Link>
        <button className="navbar-toggler order-1" type="button" onClick={() => setOpen((value) => !value)} aria-label="Toggle navigation">
          <span className="navbar-toggler-icon" />
        </button>
        <div className={`collapse navbar-collapse order-3 ${open ? "show" : ""}`}>
          <ul className="navbar-nav">
            {clientNavigation.map((item) => (
              <li className="nav-item" key={item.href}><Link to={item.href ?? "/pricing"} className="nav-link">{item.label}</Link></li>
            ))}
          </ul>
        </div>
        <ul className="order-1 order-md-3 navbar-nav navbar-no-expand ml-auto">
          {user ? (
            <li className="nav-item">
              <Link to="/customerTicket/customer-infor-detail" className="nav-link">
                <img src={user.avatarUrl} className="img-circle elevation-2 mr-2" width={28} height={28} alt="User" />
                {user.username}
              </Link>
            </li>
          ) : (
            <li className="nav-item">
              <ol className="breadcrumb bg-white mb-0">
                <li className="breadcrumb-item"><Link to="/login">Đăng nhập</Link></li>
                <li className="breadcrumb-item"><Link to="/register">Đăng ký</Link></li>
              </ol>
            </li>
          )}
        </ul>
      </div>
    </nav>
  );
}
