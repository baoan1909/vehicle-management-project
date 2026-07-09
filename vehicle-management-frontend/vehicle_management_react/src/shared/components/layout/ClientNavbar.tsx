import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../../../core/auth/useAuth";

const publicNavigation = [
  { label: "Bảng giá", href: "/pricing" },
  { label: "Hướng dẫn", href: "/guide" },
  { label: "Liên hệ", href: "/contact" },
];

export function ClientNavbar() {
  const { user } = useAuth();
  const location = useLocation();
  const [open, setOpen] = useState(false);

  return (
    <nav className="vm-public-navbar">
      <Link to="/pricing" className="vm-public-brand">CoParking</Link>
      <button className="vm-public-menu" type="button" onClick={() => setOpen((value) => !value)} aria-label="Toggle navigation">
        <i className="fas fa-bars" />
      </button>
      <div className={`vm-public-navlinks ${open ? "show" : ""}`}>
        {publicNavigation.map((item) => (
          <Link key={item.href} to={item.href} className={location.pathname === item.href ? "active" : ""}>
            {item.label}
          </Link>
        ))}
      </div>
      <div className="vm-public-actions">
        {user ? (
          <Link to="/customer/dashboard" className="vm-public-user">
            <span>KH</span>
            {user.username}
          </Link>
        ) : (
          <>
            <Link className="vm-public-login" to="/login">Đăng nhập</Link>
            <Link className="vm-public-register" to="/register">Đăng ký</Link>
          </>
        )}
      </div>
    </nav>
  );
}
