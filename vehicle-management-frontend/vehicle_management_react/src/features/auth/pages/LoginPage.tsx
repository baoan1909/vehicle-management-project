import { useState, type ReactNode } from "react";
import { Link } from "react-router-dom";

type AuthMode = "login" | "register" | "forgot" | "otp" | "recover";

interface AuthPageProps {
  mode?: AuthMode;
}

interface AuthFieldProps {
  id: string;
  label: string;
  icon: string;
  type?: string;
  placeholder?: string;
  autoComplete?: string;
  children?: ReactNode;
}

const footerLinks = [
  { label: "Điều khoản", href: "/pricing" },
  { label: "Bảo mật", href: "/pricing" },
  { label: "Liên hệ", href: "/contact" },
];

export function LoginPage({ mode = "login" }: AuthPageProps) {
  if (mode === "register") return <RegisterScreen />;
  if (mode === "forgot") return <ForgotPasswordScreen />;
  if (mode === "otp") return <OtpScreen />;
  if (mode === "recover") return <RecoverPasswordScreen />;
  return <LoginScreen />;
}

function AuthShell({ children, wide = false, cardClassName = "" }: { children: ReactNode; wide?: boolean; cardClassName?: string }) {
  return (
    <div className="vm-auth-shell">
      <header className="vm-auth-topbar">
        <Link className="vm-auth-brand" to="/pricing">
          <span className="vm-auth-brand-mark">
            <img src="/assets/admin/dist/img/AdminLTELogo.png" alt="CoParking" />
          </span>
          <span>CoParking</span>
        </Link>
        <Link className="vm-auth-help" to="/contact" aria-label="Trợ giúp">
          <i className="far fa-question-circle" />
        </Link>
      </header>

      <main className="vm-auth-main">
        <section className={`vm-auth-card ${wide ? "vm-auth-card-wide" : ""} ${cardClassName}`}>{children}</section>
      </main>

      <footer className="vm-auth-footer">
        <span>© 2026 Hệ thống quản lý bãi xe. All rights reserved.</span>
        <nav>
          {footerLinks.map((link) => (
            <Link to={link.href} key={link.label}>
              {link.label}
            </Link>
          ))}
        </nav>
      </footer>
    </div>
  );
}

function AuthHeader({ title, description, compact = false }: { title: string; description?: string; compact?: boolean }) {
  return (
    <div className={`vm-auth-header ${compact ? "vm-auth-header-compact" : ""}`}>
      <div className="vm-auth-logo">
        <img src="/assets/admin/dist/img/AdminLTELogo.png" alt="CoParking" />
      </div>
      <h1>{title}</h1>
      {description && <p>{description}</p>}
    </div>
  );
}

function AuthField({ id, label, icon, type = "text", placeholder, autoComplete, children }: AuthFieldProps) {
  return (
    <div className="vm-auth-field">
      <label htmlFor={id}>{label}</label>
      <div className="vm-auth-input-shell">
        <i className={icon} />
        {children ?? <input id={id} name={id} type={type} placeholder={placeholder} autoComplete={autoComplete} />}
      </div>
    </div>
  );
}

function PasswordField({ id, label, placeholder = "••••••••", autoComplete }: Pick<AuthFieldProps, "id" | "label" | "placeholder" | "autoComplete">) {
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);

  return (
    <div className="vm-auth-field">
      <label htmlFor={id}>{label}</label>
      <div className="vm-auth-input-shell">
        <i className="fas fa-lock" />
        <input id={id} name={id} type={isPasswordVisible ? "text" : "password"} placeholder={placeholder} autoComplete={autoComplete} />
        <button
          className="vm-auth-eye"
          type="button"
          aria-label={isPasswordVisible ? "Ẩn mật khẩu" : "Hiển thị mật khẩu"}
          aria-pressed={isPasswordVisible}
          onClick={() => setIsPasswordVisible((currentValue) => !currentValue)}
        >
          <i className={isPasswordVisible ? "far fa-eye-slash" : "far fa-eye"} />
        </button>
      </div>
    </div>
  );
}

function LoginScreen() {
  return (
    <AuthShell>
      <AuthHeader title="Đăng nhập" />
      <form className="vm-auth-form">
        <AuthField id="email" label="Email" icon="far fa-envelope" type="email" placeholder="admin@parking.local" autoComplete="email" />
        <PasswordField id="password" label="Mật khẩu" autoComplete="current-password" />

        <div className="vm-auth-options">
          <label className="vm-auth-checkbox">
            <input type="checkbox" />
            <span>Ghi nhớ đăng nhập</span>
          </label>
          <Link to="/forgot-password">Quên mật khẩu?</Link>
        </div>

        <button className="vm-auth-submit" type="button">
          Đăng nhập
        </button>
      </form>

      <AuthSwitch text="Chưa có tài khoản?" label="Đăng ký" href="/register" />
    </AuthShell>
  );
}

function RegisterScreen() {
  return (
    <AuthShell wide>
      <AuthHeader title="Tạo tài khoản mới" compact />
      <form className="vm-auth-form">
        <div className="vm-auth-grid">
          <AuthField id="fullName" label="Họ và tên" icon="fas fa-user-circle" placeholder="Nguyễn Văn A" autoComplete="name" />
          <AuthField id="dateOfBirth" label="Ngày sinh" icon="far fa-calendar-alt" type="date" />

          <div className="vm-auth-field">
            <label htmlFor="gender">Giới tính</label>
            <div className="vm-auth-input-shell">
              <i className="fas fa-venus-mars" />
              <select id="gender" name="gender" defaultValue="">
                <option value="" disabled>
                  Chọn giới tính
                </option>
                <option value="Nam">Nam</option>
                <option value="Nữ">Nữ</option>
                <option value="Khác">Khác</option>
              </select>
            </div>
          </div>

          <AuthField id="phoneNumber" label="Số điện thoại" icon="fas fa-phone-alt" type="tel" placeholder="0901000001" autoComplete="tel" />
          <AuthField id="email" label="Email" icon="far fa-envelope" type="email" placeholder="customer@example.com" autoComplete="email" />
          <AuthField id="identifyCard" label="CCCD/CMND" icon="fas fa-address-card" placeholder="079203000003" />
          <AuthField id="address" label="Địa chỉ" icon="fas fa-map-marker-alt" placeholder="Thủ Đức, TP.HCM" autoComplete="street-address" />
          <AuthField id="registerUsername" label="Tên đăng nhập" icon="fas fa-user" placeholder="vovantu" autoComplete="username" />
          <PasswordField id="registerPassword" label="Mật khẩu" autoComplete="new-password" />
          <PasswordField id="confirmPassword" label="Xác nhận mật khẩu" autoComplete="new-password" />
        </div>

        <label className="vm-auth-terms">
          <input type="checkbox" />
          <span>
            Tôi đồng ý với <Link to="/pricing">điều khoản và điều kiện</Link> của CoParking.
          </span>
        </label>

        <button className="vm-auth-submit" type="button">
          Tạo tài khoản
          <i className="fas fa-arrow-right" />
        </button>
      </form>

      <AuthSwitch text="Đã có tài khoản?" label="Đăng nhập" href="/login" />
    </AuthShell>
  );
}

function ForgotPasswordScreen() {
  return (
    <AuthShell cardClassName="vm-auth-card-reset">
      <AuthHeader title="Quên mật khẩu?" />
      <form className="vm-auth-form">
        <AuthField id="forgotEmail" label="Email nhận OTP" icon="far fa-envelope" type="email" placeholder="customer@example.com" autoComplete="email" />
        <p className="vm-auth-field-note">Mã OTP sẽ được gửi đến email dùng cho việc đặt lại mật khẩu mới.</p>
        <Link className="vm-auth-submit" to="/forgot-password/otp">
          Nhận OTP
        </Link>
      </form>
      <AuthBackLink />
    </AuthShell>
  );
}

function OtpScreen() {
  return (
    <AuthShell cardClassName="vm-auth-card-reset">
      <AuthHeader title="Xác nhận OTP" description="Nhập mã OTP gồm 6 chữ số đã được gửi đến email của bạn." />
      <form className="vm-auth-form">
        <AuthField id="otp" label="Mã OTP" icon="fas fa-key">
          <input id="otp" name="otp" className="vm-auth-otp-input" type="text" inputMode="numeric" maxLength={6} placeholder="••••••" />
        </AuthField>
        <div className="vm-auth-note vm-auth-note-success">
          <i className="far fa-clock" />
          <span>Thời gian hiệu lực của mã OTP là 2 phút.</span>
        </div>
        <Link className="vm-auth-submit" to="/recover-password">
          Xác nhận OTP
        </Link>
        <button className="vm-auth-link-button" type="button">
          Gửi lại mã OTP
        </button>
      </form>
      <AuthBackLink />
    </AuthShell>
  );
}

function RecoverPasswordScreen() {
  return (
    <AuthShell cardClassName="vm-auth-card-reset">
      <AuthHeader title="Đặt lại mật khẩu" description="Nhập mật khẩu mới cho tài khoản của bạn." />
      <form className="vm-auth-form">
        <PasswordField id="newPassword" label="Nhập mật khẩu mới" autoComplete="new-password" />
        <PasswordField id="confirmNewPassword" label="Nhập lại mật khẩu mới" autoComplete="new-password" />
        <div className="vm-auth-note">
          <i className="fas fa-shield-alt" />
          <span>Nên dùng ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.</span>
        </div>
        <button className="vm-auth-submit" type="button">
          Đặt lại mật khẩu
        </button>
      </form>
      <AuthBackLink />
    </AuthShell>
  );
}

function AuthSwitch({ text, label, href }: { text: string; label: string; href: string }) {
  return (
    <div className="vm-auth-switch">
      <span>{text}</span>
      <Link to={href}>{label}</Link>
    </div>
  );
}

function AuthBackLink() {
  return (
    <div className="vm-auth-back">
      <Link to="/login">
        <i className="fas fa-arrow-left" />
        Quay lại đăng nhập
      </Link>
    </div>
  );
}
