import { useState, type FormEvent, type ReactNode } from "react";
import { Link, Navigate } from "react-router-dom";
import { appConfig } from "@/config/env";
import {
  registerAccount,
  requestPasswordReset,
  type RegisterAccountRequest,
} from "@/features/auth/api/authApi";
import { cn } from "@/lib/cn";

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
  value?: string;
  onChange?: (value: string) => void;
}

type RegisterFormState = {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
};

const footerLinks = [
  { label: "Điều khoản", href: "/pricing" },
  { label: "Bảo mật", href: "/pricing" },
  { label: "Liên hệ", href: "/contact" },
];

const initialRegisterForm: RegisterFormState = {
  username: "",
  email: "",
  password: "",
  confirmPassword: "",
};

const authSubmitClassName =
  "tw-flex tw-min-h-11 tw-w-full tw-items-center tw-justify-center tw-gap-[0.55rem] tw-rounded-[10px] tw-border-0 tw-bg-[linear-gradient(135deg,#2563EB,#1D4ED8)] tw-px-4 tw-text-[0.95rem] tw-font-black tw-text-white tw-shadow-[0_14px_28px_rgba(37,99,235,0.22)] tw-transition hover:-tw-translate-y-px hover:tw-text-white hover:tw-brightness-[0.98] disabled:tw-cursor-not-allowed disabled:tw-opacity-60";

export function LoginPage({ mode = "login" }: AuthPageProps) {
  if (mode === "otp" || mode === "recover") {
    return <Navigate to="/forgot-password" replace />;
  }

  if (mode === "register") {
    return <RegisterScreen />;
  }

  if (mode === "forgot") {
    return <ForgotPasswordScreen />;
  }

  return <LoginScreen />;
}

function AuthShell({ children, wide = false, cardClassName = "" }: { children: ReactNode; wide?: boolean; cardClassName?: string }) {
  return (
    <div className="tw-fixed tw-inset-0 tw-flex tw-min-h-screen tw-min-h-[100dvh] tw-w-screen tw-flex-col tw-overflow-y-auto tw-bg-[radial-gradient(circle_at_18%_12%,rgba(37,99,235,0.13),transparent_30%),radial-gradient(circle_at_82%_18%,rgba(22,163,74,0.08),transparent_26%),linear-gradient(135deg,#f8fafc_0%,#eef4ff_100%)] tw-text-vm-slate-700">
      <header className="tw-flex tw-w-full tw-items-center tw-justify-between tw-px-6 tw-py-4 max-[768px]:tw-px-5">
        <Link className="tw-inline-flex tw-items-center tw-gap-[0.65rem] tw-text-[1.1rem] tw-font-extrabold tw-text-slate-900 tw-no-underline tw-transition hover:tw-text-vm-primary-hover hover:tw-no-underline" to="/pricing">
          <span className="tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-overflow-hidden tw-rounded-[10px] tw-bg-[#f8fafc] tw-shadow-[0_12px_24px_rgba(37,99,235,0.22)]">
            <img className="tw-h-full tw-w-full tw-object-contain" src="/assets/admin/dist/img/AdminLTELogo.png" alt="CoParking" />
          </span>
          <span>CoParking</span>
        </Link>
        <Link className="tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-full tw-bg-white tw-text-vm-primary tw-no-underline tw-shadow-[inset_0_0_0_1px_#e2e8f0] tw-transition hover:tw-text-vm-primary-hover" to="/contact" aria-label="Trợ giúp">
          <i className="far fa-question-circle" />
        </Link>
      </header>

      <main className="tw-flex tw-flex-1 tw-items-start tw-justify-center tw-px-6 tw-pb-5 tw-pt-3 max-[768px]:tw-px-5">
        <section
          className={cn(
            "tw-w-[min(100%,440px)] tw-rounded-[12px] tw-border tw-border-solid tw-border-[rgba(203,213,225,0.55)] tw-bg-white/95 tw-p-6 tw-shadow-[0_18px_45px_rgba(15,23,42,0.08)]",
            wide ? "tw-w-[min(100%,840px)]" : "",
            cardClassName,
          )}
        >
          {children}
        </section>
      </main>

      <footer className="tw-flex tw-w-full tw-items-center tw-justify-between tw-gap-4 tw-px-6 tw-py-4 tw-text-[0.88rem] tw-font-bold tw-text-vm-slate-500 max-[768px]:tw-flex-col max-[768px]:tw-px-5">
        <span>© 2026 Vehicle Management Platform. All rights reserved.</span>
        <nav className="tw-flex tw-items-center tw-gap-5">
          {footerLinks.map((link) => (
            <Link className="tw-font-extrabold tw-text-vm-primary tw-no-underline hover:tw-text-vm-primary-hover" to={link.href} key={link.label}>
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
    <div className={cn("tw-mb-5 tw-text-center", compact ? "tw-mb-4" : "")}>
      <div className="tw-mx-auto tw-mb-[0.9rem] tw-inline-flex tw-h-[120px] tw-w-[120px] tw-items-center tw-justify-center tw-overflow-hidden tw-bg-transparent">
        <img className="tw-h-full tw-w-full tw-object-contain" src="/assets/admin/dist/img/AdminLTELogo.png" alt="CoParking" />
      </div>
      <h1 className="tw-m-0 tw-mb-[0.2rem] tw-text-[1.45rem] tw-font-black tw-leading-tight tw-text-vm-slate-900">{title}</h1>
      {description && <p className="tw-mx-auto tw-my-0 tw-max-w-[560px] tw-text-[0.96rem] tw-font-normal tw-leading-[1.45] tw-text-vm-slate-500">{description}</p>}
    </div>
  );
}

function AuthField({ id, label, icon, type = "text", placeholder, autoComplete, value, onChange }: AuthFieldProps) {
  return (
    <div>
      <label className="tw-mb-[0.45rem] tw-block tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-700" htmlFor={id}>{label}</label>
      <div className="tw-relative">
        <i className={cn(icon, "tw-absolute tw-left-4 tw-top-1/2 tw-text-vm-slate-500 -tw-translate-y-1/2")} />
        <input
          className="tw-min-h-[42px] tw-w-full tw-rounded-[10px] tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-py-[0.62rem] tw-pl-[2.8rem] tw-pr-4 tw-text-vm-slate-900 tw-outline-none tw-transition placeholder:tw-text-slate-400 focus:tw-border-[rgba(37,99,235,0.65)] focus:tw-shadow-[0_0_0_4px_rgba(37,99,235,0.1)]"
          id={id}
          name={id}
          type={type}
          placeholder={placeholder}
          autoComplete={autoComplete}
          value={value}
          onChange={(event) => onChange?.(event.target.value)}
        />
      </div>
    </div>
  );
}

function PasswordField({
  id,
  label,
  value,
  onChange,
  placeholder = "********",
  autoComplete,
}: Pick<AuthFieldProps, "id" | "label" | "placeholder" | "autoComplete" | "value" | "onChange">) {
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);

  return (
    <div>
      <label className="tw-mb-[0.45rem] tw-block tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-700" htmlFor={id}>{label}</label>
      <div className="tw-relative">
        <i className="fas fa-lock tw-absolute tw-left-4 tw-top-1/2 tw-text-vm-slate-500 -tw-translate-y-1/2" />
        <input
          className="tw-min-h-[42px] tw-w-full tw-rounded-[10px] tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-py-[0.62rem] tw-pl-[2.8rem] tw-pr-12 tw-text-vm-slate-900 tw-outline-none tw-transition placeholder:tw-text-slate-400 focus:tw-border-[rgba(37,99,235,0.65)] focus:tw-shadow-[0_0_0_4px_rgba(37,99,235,0.1)]"
          id={id}
          name={id}
          type={isPasswordVisible ? "text" : "password"}
          placeholder={placeholder}
          autoComplete={autoComplete}
          value={value}
          onChange={(event) => onChange?.(event.target.value)}
        />
        <button
          className="tw-absolute tw-right-[0.85rem] tw-top-1/2 tw-border-0 tw-bg-transparent tw-p-0 tw-text-vm-slate-500 tw-transition -tw-translate-y-1/2 hover:tw-text-vm-primary"
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

function AuthAlert({ tone, message }: { tone: "success" | "error" | "info"; message: string }) {
  return (
    <div
      className={cn(
        "tw-flex tw-gap-[0.65rem] tw-rounded-[10px] tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50 tw-px-4 tw-py-[0.9rem] tw-font-bold tw-leading-[1.45] tw-text-vm-primary-hover",
        tone === "success" ? "tw-border-emerald-200 tw-bg-emerald-50 tw-text-emerald-800" : "",
        tone === "error" ? "tw-border-red-200 tw-bg-red-50 tw-text-red-700" : "",
      )}
    >
      <i className={cn(tone === "success" ? "fas fa-check-circle" : tone === "error" ? "fas fa-exclamation-circle" : "fas fa-info-circle", "tw-mt-[0.15rem] tw-text-vm-primary")} />
      <span>{message}</span>
    </div>
  );
}

function LoginScreen() {
  const isLoginConfigured = appConfig.keycloakLoginUrl.trim().length > 0;

  function handleLoginRedirect() {
    if (!isLoginConfigured) {
      return;
    }

    window.location.assign(appConfig.keycloakLoginUrl);
  }

  return (
    <AuthShell>
      <AuthHeader
        title="Đăng nhập an toàn"
        description="Đăng nhập được xử lý bởi Keycloak để giữ chuẩn OAuth2/OIDC, MFA và session trung tâm."
      />

      <div className="tw-grid tw-gap-[0.8rem]">
        <AuthAlert
          tone="info"
          message="Tài khoản nhân viên và khách hàng sẽ được xác thực trên trang đăng nhập Keycloak đã custom theme."
        />

        <button className={authSubmitClassName} type="button" onClick={handleLoginRedirect} disabled={!isLoginConfigured}>
          Đăng nhập với Keycloak
        </button>

        {!isLoginConfigured && (
          <AuthAlert
            tone="error"
            message="Chưa cấu hình VITE_KEYCLOAK_LOGIN_URL. Hãy cập nhật env để frontend redirect đúng tới trang login Keycloak."
          />
        )}

        <div className="tw-flex tw-items-center tw-justify-between tw-gap-4 tw-py-1 tw-text-[0.9rem] tw-font-bold tw-text-vm-slate-500 max-[480px]:tw-flex-col max-[480px]:tw-items-start">
          <span>Cần tạo tài khoản mới hoặc yêu cầu đặt lại mật khẩu?</span>
          <Link className="tw-font-extrabold tw-text-vm-primary tw-no-underline hover:tw-text-vm-primary-hover" to="/forgot-password">Quên mật khẩu?</Link>
        </div>
      </div>

      <AuthSwitch text="Chưa có tài khoản?" label="Đăng ký" href="/register" />
    </AuthShell>
  );
}

function RegisterScreen() {
  const [form, setForm] = useState<RegisterFormState>(initialRegisterForm);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  function updateField<Key extends keyof RegisterFormState>(field: Key, value: RegisterFormState[Key]) {
    setForm((currentValue) => ({ ...currentValue, [field]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");

    if (form.password !== form.confirmPassword) {
      setErrorMessage("Mật khẩu xác nhận không khớp.");
      return;
    }

    setIsSubmitting(true);

    const payload: RegisterAccountRequest = {
      username: form.username,
      email: form.email,
      password: form.password,
    };

    try {
      const response = await registerAccount(payload);
      setSuccessMessage(response.message);
      setForm(initialRegisterForm);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Không thể tạo tài khoản.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthShell>
      <AuthHeader
        title="Tạo tài khoản mới"
        description="Đăng ký tài khoản chỉ với username, email và password. Hồ sơ nội bộ sẽ được tạo tối thiểu và có thể bổ sung sau."
      />

      <form className="tw-grid tw-gap-[0.8rem]" onSubmit={handleSubmit}>
        {successMessage && <AuthAlert tone="success" message={successMessage} />}
        {errorMessage && <AuthAlert tone="error" message={errorMessage} />}

        <AuthField
          id="registerUsername"
          label="Tên đăng nhập"
          icon="fas fa-user"
          placeholder="vovantu"
          autoComplete="username"
          value={form.username}
          onChange={(value) => updateField("username", value)}
        />
        <AuthField
          id="email"
          label="Email"
          icon="far fa-envelope"
          type="email"
          placeholder="customer@example.com"
          autoComplete="email"
          value={form.email}
          onChange={(value) => updateField("email", value)}
        />
        <PasswordField
          id="registerPassword"
          label="Mật khẩu"
          autoComplete="new-password"
          value={form.password}
          onChange={(value) => updateField("password", value)}
        />
        <PasswordField
          id="confirmPassword"
          label="Xác nhận mật khẩu"
          autoComplete="new-password"
          value={form.confirmPassword}
          onChange={(value) => updateField("confirmPassword", value)}
        />

        <AuthAlert tone="info" message="Sau khi tạo tài khoản, hệ thống sẽ gửi email verification và tài khoản nội bộ sẽ ở trạng thái PENDING." />

        <button className={authSubmitClassName} type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Đang tạo tài khoản..." : "Tạo tài khoản và gửi email xác minh"}
          <i className="fas fa-arrow-right" />
        </button>
      </form>

      <AuthSwitch text="Đã có tài khoản?" label="Đăng nhập" href="/login" />
    </AuthShell>
  );
}

function ForgotPasswordScreen() {
  const [email, setEmail] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");
    setIsSubmitting(true);

    try {
      const response = await requestPasswordReset({ email });
      setSuccessMessage(response.message);
      setEmail("");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Không thể gửi yêu cầu đặt lại mật khẩu.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthShell cardClassName="tw-flex tw-min-h-[500px] tw-flex-col">
      <AuthHeader
        title="Quên mật khẩu?"
        description="Nhập email đã đăng ký. Backend sẽ yêu cầu Keycloak gửi email reset password đến bạn."
      />

      <form className="tw-grid tw-gap-[0.8rem]" onSubmit={handleSubmit}>
        {successMessage && <AuthAlert tone="success" message={successMessage} />}
        {errorMessage && <AuthAlert tone="error" message={errorMessage} />}

        <AuthField
          id="forgotEmail"
          label="Email"
          icon="far fa-envelope"
          type="email"
          placeholder="customer@example.com"
          autoComplete="email"
          value={email}
          onChange={setEmail}
        />

        <p className="tw-mb-[0.1rem] tw-mt-[-0.2rem] tw-text-[0.9rem] tw-font-semibold tw-leading-[1.45] tw-text-vm-slate-500">
          Link trong email sẽ đưa bạn tới themed page của Keycloak để đặt lại mật khẩu một cách an toàn.
        </p>

        <button className={authSubmitClassName} type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Đang gửi yêu cầu..." : "Gửi email đặt lại mật khẩu"}
        </button>
      </form>

      <AuthBackLink />
    </AuthShell>
  );
}

function AuthSwitch({ text, label, href }: { text: string; label: string; href: string }) {
  return (
    <div className="tw-mt-4 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-4 tw-text-center tw-text-vm-slate-500">
      <span>{text}</span>
      <Link className="tw-ml-[0.35rem] tw-font-extrabold tw-text-vm-primary tw-no-underline hover:tw-text-vm-primary-hover" to={href}>{label}</Link>
    </div>
  );
}

function AuthBackLink() {
  return (
    <div className="tw-mt-auto tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-4 tw-text-center">
      <Link className="tw-flex tw-items-center tw-justify-center tw-gap-2 tw-font-extrabold tw-text-vm-primary tw-no-underline hover:tw-text-vm-primary-hover" to="/login">
        <i className="fas fa-arrow-left" />
        Quay lại đăng nhập
      </Link>
    </div>
  );
}
