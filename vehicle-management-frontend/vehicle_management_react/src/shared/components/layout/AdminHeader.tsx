import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../../core/auth/useAuth";

const searchSuggestions = [
  "Tìm thẻ xe",
  "Tra cứu khách hàng",
  "Kiểm tra xe đang trong bãi",
];

const notifications = [
  {
    href: "/admin/customer",
    icon: "fas fa-envelope",
    title: "Tin nhắn mới",
    meta: "4 cuộc trò chuyện cần phản hồi",
  },
  {
    href: "/admin/customer",
    icon: "fas fa-user-check",
    title: "Yêu cầu đăng ký",
    meta: "8 yêu cầu chờ xác nhận",
  },
  {
    href: "/admin/dashboard",
    icon: "fas fa-chart-line",
    title: "Báo cáo mới",
    meta: "3 báo cáo vừa được tạo",
  },
];

function getRoleLabel(role?: string) {
  switch (role) {
    case "ADMIN":
      return "Quản trị viên";
    case "EMPLOYEE":
      return "Nhân viên";
    case "CUSTOMER":
      return "Khách hàng";
    default:
      return "Người dùng";
  }
}

export function AdminHeader() {
  const navigate = useNavigate();
  const { user, setUser } = useAuth();
  const [searchValue, setSearchValue] = useState("");
  const [searchOpen, setSearchOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const searchRef = useRef<HTMLDivElement | null>(null);
  const notificationsRef = useRef<HTMLDivElement | null>(null);
  const profileRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      const target = event.target as Node;

      if (searchRef.current && !searchRef.current.contains(target)) {
        setSearchOpen(false);
      }

      if (notificationsRef.current && !notificationsRef.current.contains(target)) {
        setNotificationsOpen(false);
      }

      if (profileRef.current && !profileRef.current.contains(target)) {
        setProfileOpen(false);
      }
    }

    document.addEventListener("mousedown", handlePointerDown);

    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
    };
  }, []);

  function handleSuggestionSelect(value: string) {
    setSearchValue(value);
    setSearchOpen(false);
  }

  function handleLogout() {
    setUser(null);
    setProfileOpen(false);
    navigate("/login");
  }

  const displayName = user?.fullName ?? user?.username ?? "Người dùng";
  const roleLabel = getRoleLabel(user?.role);

  return (
    <header className="vm-admin-header">
      <div className="vm-admin-header__inner">
        <div className="vm-admin-header__brand">
          <Link to="/admin/dashboard" className="vm-admin-header__brand-link">
            <span className="vm-admin-header__brand-mark">
              <img src="/assets/admin/dist/img/AdminLTELogo.png" alt="CoParking" />
            </span>
            <span className="vm-admin-header__brand-text">
              <strong>CoParking</strong>
              <small>Admin Portal</small>
            </span>
          </Link>
        </div>

        <div className="vm-admin-header__center">
          <div ref={searchRef} className="vm-header-search">
            <div className={`vm-header-search__box ${searchOpen ? "is-active" : ""}`}>
              <i className="fas fa-search vm-header-search__icon" />
              <input
                type="search"
                value={searchValue}
                placeholder="Tìm khách hàng, biển số, thẻ xe..."
                aria-label="Tìm kiếm"
                onFocus={() => setSearchOpen(true)}
                onChange={(event) => setSearchValue(event.target.value)}
              />
              <span className="vm-header-search__hint">Ctrl+K</span>
            </div>

            {searchOpen && (
              <div className="vm-header-panel vm-header-search__panel">
                <div className="vm-header-panel__title">Gợi ý tìm kiếm</div>
                <div className="vm-header-search__suggestions">
                  {searchSuggestions.map((suggestion) => (
                    <button
                      key={suggestion}
                      type="button"
                      className="vm-header-search__suggestion"
                      onClick={() => handleSuggestionSelect(suggestion)}
                    >
                      <i className="fas fa-arrow-up-right-from-square" />
                      <span>{suggestion}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="vm-admin-header__actions">
          <div ref={notificationsRef} className="vm-header-menu">
            <button
              type="button"
              className={`vm-header-menu__trigger ${notificationsOpen ? "is-active" : ""}`}
              onClick={() => {
                setNotificationsOpen((current) => !current);
                setProfileOpen(false);
              }}
              aria-label="Thông báo"
            >
              <i className="far fa-bell" />
              <span className="vm-header-menu__badge">3</span>
            </button>

            {notificationsOpen && (
              <div className="vm-header-panel vm-header-menu__panel">
                <div className="vm-header-panel__title">Thông báo</div>
                {notifications.map((item) => (
                  <Link key={item.title} to={item.href} className="vm-header-item" onClick={() => setNotificationsOpen(false)}>
                    <span className="vm-header-item__icon">
                      <i className={item.icon} />
                    </span>
                    <span className="vm-header-item__content">
                      <strong>{item.title}</strong>
                      <small>{item.meta}</small>
                    </span>
                  </Link>
                ))}
              </div>
            )}
          </div>

          <div ref={profileRef} className="vm-header-menu">
            <button
              type="button"
              className={`vm-header-profile ${profileOpen ? "is-active" : ""}`}
              onClick={() => {
                setProfileOpen((current) => !current);
                setNotificationsOpen(false);
              }}
              aria-label={`Mở hồ sơ ${displayName}`}
            >
              <img src={user?.avatarUrl} alt={displayName} className="vm-header-profile__avatar" />
              <span className="vm-header-profile__meta">
                <strong>{displayName}</strong>
              </span>
            </button>

            {profileOpen && (
              <div className="vm-header-panel vm-header-profile__panel">
                <div className="vm-header-profile__summary">
                  <img src={user?.avatarUrl} alt={displayName} className="vm-header-profile__summary-avatar" />
                  <div className="vm-header-profile__summary-meta">
                    <strong>{displayName}</strong>
                    <small>{roleLabel}</small>
                  </div>
                </div>

                <Link to="/admin/account/form" className="vm-header-item" onClick={() => setProfileOpen(false)}>
                  <span className="vm-header-item__icon">
                    <i className="fas fa-user-circle" />
                  </span>
                  <span className="vm-header-item__content">
                    <strong>Thông tin tài khoản</strong>
                    <small>Cập nhật hồ sơ quản trị</small>
                  </span>
                </Link>

                <Link to="/contact" className="vm-header-item" onClick={() => setProfileOpen(false)}>
                  <span className="vm-header-item__icon">
                    <i className="fas fa-question-circle" />
                  </span>
                  <span className="vm-header-item__content">
                    <strong>Trợ giúp</strong>
                    <small>Xem hướng dẫn và liên hệ hỗ trợ</small>
                  </span>
                </Link>

                <button type="button" className="vm-header-item vm-header-item--button" onClick={handleLogout}>
                  <span className="vm-header-item__icon">
                    <i className="fas fa-sign-out-alt" />
                  </span>
                  <span className="vm-header-item__content">
                    <strong>Đăng xuất</strong>
                    <small>Thoát khỏi phiên làm việc hiện tại</small>
                  </span>
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
