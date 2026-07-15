import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { appConfig } from "@/config/env";
import { getCurrentUserFromAccessToken, saveAuthTokens } from "@/core/auth/session";
import { useAuth } from "@/core/auth/useAuth";
import { getMyAccountProfile } from "@/features/iam/api/accountProfileApi";
import { mergeCurrentUserWithAccountProfile } from "@/features/iam/utils/accountProfileMapper";
import type { CurrentUser } from "@/shared/types/common";
import {
  buildKeycloakLoginUrl,
  exchangeKeycloakAuthorizationCode,
  registerAccount,
  resendVerificationEmail,
  requestPasswordReset,
  type RegisterAccountRequest,
} from "@/features/auth/api/authApi";
import { AuthBrandMark, AuthFormField, AuthFormSectionTitle, AuthInlineNotice, AuthPasswordInput } from "@/features/auth/components/AuthFormControls";
import { Button } from "@/components/ui";
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
  fullName: string;
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
  fullName: "",
  username: "",
  email: "",
  password: "",
  confirmPassword: "",
};

const adminPostLoginRedirectPath = "/api/dashboard/overview";
const customerPostLoginRedirectPath = "/customer/dashboard";
const processedAuthorizationCodeKey = "vm_keycloak_processed_authorization_code";
let activeAuthorizationCode = "";

const authSubmitClassName =
  "tw-flex tw-min-h-11 tw-w-full tw-items-center tw-justify-center tw-gap-[0.55rem] tw-rounded-[10px] tw-border-0 tw-bg-[linear-gradient(135deg,#2563EB,#1D4ED8)] tw-px-4 tw-text-[0.95rem] tw-font-black tw-text-white tw-shadow-[0_14px_28px_rgba(37,99,235,0.22)] tw-transition hover:-tw-translate-y-px hover:tw-text-white hover:tw-brightness-[0.98] disabled:tw-cursor-not-allowed disabled:tw-opacity-60";

function resolvePostLoginRedirectPath(user: CurrentUser | null) {
  return user?.role === "CUSTOMER" ? customerPostLoginRedirectPath : adminPostLoginRedirectPath;
}

async function resolveLoggedInUser(accessToken: string) {
  const tokenUser = getCurrentUserFromAccessToken(accessToken);
  if (!tokenUser) return null;

  try {
    const response = await getMyAccountProfile();
    return mergeCurrentUserWithAccountProfile(tokenUser, response.data);
  } catch {
    return tokenUser;
  }
}

export function LoginPage({ mode = "login" }: AuthPageProps) {
  if (mode === "otp" || mode === "recover") {
    return <Navigate to="/forgot-password" replace />;
  }

  if (mode === "register") {
    return <RegisterScreenV2 />;
  }

  if (mode === "forgot") {
    return <ForgotPasswordScreen />;
  }

  return <KeycloakRedirectScreen />;
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

function KeycloakRedirectScreen() {
  const location = useLocation();
  const navigate = useNavigate();
  const { setUser } = useAuth();
  const [isExchangingCode, setIsExchangingCode] = useState(false);

  useEffect(() => {
    const searchParams = new URLSearchParams(location.search);
    const code = searchParams.get("code");
    if (!code || isExchangingCode) return;
    if (activeAuthorizationCode === code) return;
    if (sessionStorage.getItem(processedAuthorizationCodeKey) === code) {
      navigate("/login", { replace: true });
      return;
    }
    const authorizationCode = code;
    activeAuthorizationCode = authorizationCode;
    sessionStorage.setItem(processedAuthorizationCodeKey, authorizationCode);

    async function exchangeCode() {
      setIsExchangingCode(true);

      try {
        const tokenResponse = await exchangeKeycloakAuthorizationCode(authorizationCode);
        saveAuthTokens({
          accessToken: tokenResponse.access_token,
          refreshToken: tokenResponse.refresh_token,
          idToken: tokenResponse.id_token,
        });
        const loggedInUser = await resolveLoggedInUser(tokenResponse.access_token);
        setUser(loggedInUser);
        activeAuthorizationCode = "";
        navigate(resolvePostLoginRedirectPath(loggedInUser), { replace: true });
      } catch (error) {
        console.error(error);
        activeAuthorizationCode = "";
        navigate("/login", { replace: true });
      } finally {
        setIsExchangingCode(false);
      }
    }

    void exchangeCode();
  }, [isExchangingCode, location.search, navigate, setUser]);

  useEffect(() => {
    const searchParams = new URLSearchParams(location.search);
    const code = searchParams.get("code");
    if (code) return;

    async function redirectToKeycloak() {
      try {
        const loginUrl = await buildKeycloakLoginUrl();
        window.location.replace(loginUrl);
      } catch (error) {
        console.error(error);
      }
    }

    void redirectToKeycloak();
  }, [location.search]);

  return null;
}

function LoginScreen() {
  const location = useLocation();
  const navigate = useNavigate();
  const { setUser } = useAuth();
  const isLoginConfigured = appConfig.keycloakLoginUrl.trim().length > 0;
  const [callbackMessage, setCallbackMessage] = useState("");
  const [callbackError, setCallbackError] = useState("");
  const [isExchangingCode, setIsExchangingCode] = useState(false);

  useEffect(() => {
    const searchParams = new URLSearchParams(location.search);
    const code = searchParams.get("code");
    if (!code || isExchangingCode) return;
    if (activeAuthorizationCode === code) return;
    if (sessionStorage.getItem(processedAuthorizationCodeKey) === code) {
      navigate("/login", { replace: true });
      return;
    }
    const authorizationCode = code;
    activeAuthorizationCode = authorizationCode;
    sessionStorage.setItem(processedAuthorizationCodeKey, authorizationCode);

    async function exchangeCode() {
      setIsExchangingCode(true);
      setCallbackError("");
      setCallbackMessage("Đang hoàn tất đăng nhập...");

      try {
        const tokenResponse = await exchangeKeycloakAuthorizationCode(authorizationCode);
        saveAuthTokens({
          accessToken: tokenResponse.access_token,
          refreshToken: tokenResponse.refresh_token,
          idToken: tokenResponse.id_token,
        });
        const loggedInUser = await resolveLoggedInUser(tokenResponse.access_token);
        setUser(loggedInUser);
        activeAuthorizationCode = "";
        navigate(resolvePostLoginRedirectPath(loggedInUser), { replace: true });
      } catch (error) {
        activeAuthorizationCode = "";
        setCallbackMessage("");
        setCallbackError(error instanceof Error ? error.message : "Không thể hoàn tất đăng nhập Keycloak.");
        navigate("/login", { replace: true });
      } finally {
        setIsExchangingCode(false);
      }
    }

    void exchangeCode();
  }, [isExchangingCode, location.search, navigate, setUser]);

  async function handleLoginRedirect() {
    if (!isLoginConfigured) {
      return;
    }

    const loginUrl = await buildKeycloakLoginUrl();
    window.location.assign(loginUrl);
  }

  return (
    <AuthShell>
      <AuthHeader
        title="Đăng nhập an toàn"
        description="Đăng nhập được xử lý bởi Keycloak để giữ chuẩn OAuth2/OIDC, MFA và session trung tâm."
      />

      <div className="tw-grid tw-gap-[0.8rem]">
        {callbackMessage && <AuthAlert tone="info" message={callbackMessage} />}
        {callbackError && <AuthAlert tone="error" message={callbackError} />}

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

      <ResendVerificationPanel />

      <AuthSwitch text="Chưa có tài khoản?" label="Đăng ký" href="/register" />
    </AuthShell>
  );
}

function RegisterScreenV2() {
  const [form, setForm] = useState<RegisterFormState>(initialRegisterForm);
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isResendingVerification, setIsResendingVerification] = useState(false);
  const [lastVerificationEmail, setLastVerificationEmail] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const resendTargetEmail = (lastVerificationEmail || form.email).trim();

  function updateField<Key extends keyof RegisterFormState>(field: Key, value: RegisterFormState[Key]) {
    setForm((currentValue) => ({ ...currentValue, [field]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");

    if (!acceptedTerms) {
      setErrorMessage("Vui lòng đồng ý điều khoản sử dụng và chính sách bảo mật.");
      return;
    }

    if (form.password !== form.confirmPassword) {
      setErrorMessage("Mật khẩu xác nhận không khớp.");
      return;
    }

    setIsSubmitting(true);

    const verificationEmail = form.email.trim();
    const payload: RegisterAccountRequest = {
      fullName: form.fullName,
      username: form.username,
      email: verificationEmail,
      password: form.password,
    };

    try {
      const response = await registerAccount(payload);
      setSuccessMessage(response.message);
      setLastVerificationEmail(verificationEmail);
      setForm(initialRegisterForm);
      setAcceptedTerms(false);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Không thể tạo tài khoản.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleResendVerificationEmail() {
    setErrorMessage("");
    setSuccessMessage("");

    if (!resendTargetEmail) {
      setErrorMessage("Vui lòng nhập email để gửi lại email xác thực.");
      return;
    }

    setIsResendingVerification(true);

    try {
      const response = await resendVerificationEmail({ email: resendTargetEmail });
      setSuccessMessage(response.message || "Đã gửi lại email xác thực.");
      setLastVerificationEmail(resendTargetEmail);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Không thể gửi lại email xác thực.");
    } finally {
      setIsResendingVerification(false);
    }
  }

  return (
    <div className="tw-fixed tw-inset-0 tw-flex tw-min-h-screen tw-min-h-[100dvh] tw-w-screen tw-flex-col tw-overflow-hidden tw-bg-[linear-gradient(180deg,#f8fbff_0%,#eef5ff_100%)] tw-px-5 tw-py-2 tw-text-vm-slate-700 max-[768px]:tw-overflow-y-auto">
      <main className="tw-mx-auto tw-flex tw-w-full tw-max-w-[920px] tw-flex-1 tw-flex-col tw-items-stretch tw-justify-center">
        <section className="tw-rounded-vm-md tw-border tw-border-solid tw-border-[#d9e2f2] tw-bg-white tw-px-8 tw-pb-4 tw-pt-4 tw-shadow-[0_18px_45px_rgba(15,23,42,0.08)] max-[768px]:tw-px-5">
          <header className="tw-mx-auto tw-mb-4 tw-text-center">
            <AuthBrandMark />
            <h1 className="tw-m-0 tw-mt-2 tw-text-[1.3rem] tw-font-black tw-leading-tight tw-text-vm-slate-900">Tạo tài khoản mới</h1>
            <p className="tw-mx-auto tw-mb-0 tw-mt-1 tw-max-w-[560px] tw-text-[0.86rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">
              Vui lòng nhập thông tin để tạo tài khoản và sử dụng hệ thống.
            </p>
          </header>

          <form className="tw-grid tw-gap-3" onSubmit={handleSubmit}>
            {successMessage ? <AuthInlineNotice tone="success">{successMessage}</AuthInlineNotice> : null}
            {errorMessage ? <AuthInlineNotice tone="error">{errorMessage}</AuthInlineNotice> : null}

            <div className="tw-grid tw-grid-cols-2 tw-gap-x-8 tw-gap-y-3 max-[768px]:tw-grid-cols-1">
              <section className="tw-grid tw-content-start tw-gap-2.5">
                <AuthFormSectionTitle>Thông tin cá nhân</AuthFormSectionTitle>
                <AuthFormField
                  autoComplete="name"
                  id="fullName"
                  icon="far fa-user"
                  label="Họ và tên"
                  placeholder="Nhập họ và tên"
                  value={form.fullName}
                  onChange={(value) => updateField("fullName", value)}
                />
              </section>

              <section className="tw-grid tw-content-start tw-gap-2.5">
                <AuthFormSectionTitle>Thông tin đăng nhập</AuthFormSectionTitle>
                <AuthFormField
                  autoComplete="username"
                  id="registerUsername"
                  icon="far fa-user"
                  label="Tên đăng nhập"
                  placeholder="Nhập tên đăng nhập"
                  value={form.username}
                  onChange={(value) => updateField("username", value)}
                />
              </section>

              <section className="tw-col-span-2 tw-grid tw-gap-2.5 max-[768px]:tw-col-span-1">
                <AuthFormField
                  autoComplete="email"
                  id="email"
                  icon="far fa-envelope"
                  label="Email"
                  placeholder="Nhập email của bạn"
                  type="email"
                  value={form.email}
                  onChange={(value) => updateField("email", value)}
                />
                <AuthPasswordInput
                  autoComplete="new-password"
                  id="registerPassword"
                  label="Mật khẩu"
                  placeholder="Nhập mật khẩu"
                  value={form.password}
                  onChange={(value) => updateField("password", value)}
                />
                <AuthPasswordInput
                  autoComplete="new-password"
                  id="confirmPassword"
                  label="Xác nhận mật khẩu"
                  placeholder="Nhập lại mật khẩu"
                  value={form.confirmPassword}
                  onChange={(value) => updateField("confirmPassword", value)}
                />
              </section>
            </div>

            <AuthInlineNotice>
              <div className="tw-flex tw-min-w-0 tw-flex-1 tw-items-center tw-gap-3 max-[768px]:tw-flex-col max-[768px]:tw-items-start">
                <span className="tw-min-w-0 tw-flex-1">Sau khi tạo tài khoản, email xác thực sẽ được gửi đến địa chỉ email của bạn. Vui lòng kiểm tra email để kích hoạt tài khoản.</span>
                <Button
                  className="tw-ml-auto tw-mr-2 tw-h-8 tw-flex-shrink-0 tw-rounded-vm-sm tw-px-3 tw-text-[0.78rem] tw-font-extrabold max-[768px]:tw-ml-0 max-[768px]:tw-mr-0"
                  disabled={isResendingVerification || !resendTargetEmail}
                  size="sm"
                  type="button"
                  variant="secondary"
                  onClick={handleResendVerificationEmail}
                >
                  {isResendingVerification ? "Đang gửi..." : "Gửi lại email"}
                </Button>
              </div>
            </AuthInlineNotice>

            <label className="tw-flex tw-items-center tw-gap-2.5 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-700">
              <input
                checked={acceptedTerms}
                className="tw-h-4 tw-w-4 tw-rounded-vm-sm tw-border tw-border-solid tw-border-[#cbd5e1] tw-accent-vm-primary"
                type="checkbox"
                onChange={(event) => setAcceptedTerms(event.target.checked)}
              />
              <span>
                Tôi đã đọc và đồng ý với{" "}
                <Link className="tw-font-extrabold tw-text-vm-primary tw-no-underline hover:tw-text-vm-primary-hover" to="/pricing">Điều khoản sử dụng</Link>
                {" "}và{" "}
                <Link className="tw-font-extrabold tw-text-vm-primary tw-no-underline hover:tw-text-vm-primary-hover" to="/pricing">Chính sách bảo mật</Link>
              </span>
            </label>

            <Button className="tw-h-[38px] tw-w-full tw-rounded-vm-md tw-text-[0.9rem] tw-font-extrabold" disabled={isSubmitting} type="submit" variant="primary">
              {isSubmitting ? "Đang tạo tài khoản..." : "Tạo tài khoản"}
            </Button>
          </form>
        </section>

        <div className="tw-mt-2 tw-text-center tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-700">
          Đã có tài khoản?{" "}
          <Link className="tw-font-extrabold tw-text-vm-primary tw-no-underline hover:tw-text-vm-primary-hover" to="/login">Đăng nhập</Link>
        </div>
      </main>
    </div>
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
      fullName: form.fullName,
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
          id="fullName"
          label="Họ và tên"
          icon="far fa-id-card"
          placeholder="Nguyễn Văn A"
          autoComplete="name"
          value={form.fullName}
          onChange={(value) => updateField("fullName", value)}
        />
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
    <div className="tw-fixed tw-inset-0 tw-flex tw-min-h-screen tw-min-h-[100dvh] tw-w-screen tw-flex-col tw-overflow-hidden tw-bg-[linear-gradient(180deg,#f8fbff_0%,#eef5ff_100%)] tw-px-5 tw-py-4 tw-text-vm-slate-700 max-[768px]:tw-overflow-y-auto">
      <main className="tw-mx-auto tw-flex tw-w-full tw-max-w-[520px] tw-flex-1 tw-flex-col tw-items-stretch tw-justify-center">
        <section className="tw-rounded-vm-md tw-border tw-border-solid tw-border-[#d9e2f2] tw-bg-white tw-px-8 tw-pb-6 tw-pt-6 tw-shadow-[0_18px_45px_rgba(15,23,42,0.08)] max-[480px]:tw-px-5">
          <header className="tw-mx-auto tw-mb-5 tw-text-center">
            <AuthBrandMark />
            <h1 className="tw-m-0 tw-mt-4 tw-text-[1.35rem] tw-font-black tw-leading-tight tw-text-vm-slate-900">Đặt lại mật khẩu</h1>
          </header>

          <form className="tw-grid tw-gap-4" onSubmit={handleSubmit}>
            {successMessage ? <AuthInlineNotice tone="success">{successMessage}</AuthInlineNotice> : null}
            {errorMessage ? <AuthInlineNotice tone="error">{errorMessage}</AuthInlineNotice> : null}

            <AuthFormField
              autoComplete="email"
              id="forgotEmail"
              icon="far fa-envelope"
              label="Email"
              placeholder="Nhập email của bạn"
              type="email"
              value={email}
              onChange={setEmail}
            />

            <AuthInlineNotice>
              Vui lòng kiểm tra hộp thư đến và thư rác. Đường dẫn đặt lại mật khẩu chỉ có hiệu lực trong một khoảng thời gian nhất định.
            </AuthInlineNotice>

            <Button className="tw-h-[38px] tw-w-full tw-rounded-vm-md tw-text-[0.9rem] tw-font-extrabold" disabled={isSubmitting} type="submit" variant="primary">
              {isSubmitting ? "Đang gửi..." : "Gửi hướng dẫn đặt lại mật khẩu"}
            </Button>
          </form>
        </section>

        <div className="tw-mt-4 tw-text-center tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-700">
          Đã nhớ mật khẩu?{" "}
          <Link className="tw-font-extrabold tw-text-vm-primary tw-no-underline hover:tw-text-vm-primary-hover" to="/login">Đăng nhập</Link>
        </div>
      </main>
    </div>
  );
}

function ResendVerificationPanel() {
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
      const response = await resendVerificationEmail({ email });
      setSuccessMessage(response.message);
      setEmail("");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Không thể gửi lại email xác minh.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className="tw-mt-4 tw-grid tw-gap-[0.8rem] tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-4" onSubmit={handleSubmit}>
      <div>
        <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Chưa nhận được email xác minh?</h2>
        <p className="tw-m-0 tw-mt-1 tw-text-[0.86rem] tw-font-semibold tw-leading-[1.45] tw-text-vm-slate-500">
          Nhập email đã đăng ký để hệ thống gửi lại email xác minh nếu tài khoản còn đang chờ kích hoạt.
        </p>
      </div>

      {successMessage && <AuthAlert tone="success" message={successMessage} />}
      {errorMessage && <AuthAlert tone="error" message={errorMessage} />}

      <AuthField
        id="resendVerificationEmail"
        label="Email cần xác minh"
        icon="far fa-envelope"
        type="email"
        placeholder="customer@example.com"
        autoComplete="email"
        value={email}
        onChange={setEmail}
      />

      <button className={authSubmitClassName} type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Đang gửi lại email..." : "Gửi lại email xác minh"}
      </button>
    </form>
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
