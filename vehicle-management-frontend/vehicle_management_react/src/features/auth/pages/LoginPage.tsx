import { useEffect, useState, type FormEvent } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { consumeLogoutRedirectGuard } from "@/core/auth/logout";
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
import { FullPageCarLoader } from "@/shared/components/ui/PageTransitionLoader";

type AuthMode = "login" | "register" | "forgot" | "otp" | "recover";

interface AuthPageProps {
  mode?: AuthMode;
}

type RegisterFormState = {
  fullName: string;
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
};

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
const forgotPasswordEmailStorageKey = "vm_forgot_password_email";
let activeAuthorizationCode = "";

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
  const [returningFromLogout] = useState(() => mode === "login" && consumeLogoutRedirectGuard());

  if (mode === "otp" || mode === "recover") {
    return <Navigate to="/forgot-password" replace />;
  }

  if (mode === "register") {
    return <RegisterScreenV2 />;
  }

  if (mode === "forgot") {
    return <ForgotPasswordScreen />;
  }

  if (returningFromLogout) {
    return <LoggedOutScreen />;
  }

  return (
    <KeycloakRedirectScreen label="Đang chuyển đến đăng nhập..." />
  );
}

function LoggedOutScreen() {
  const [isRedirecting, setIsRedirecting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  async function handleLoginAgain() {
    setErrorMessage("");
    setIsRedirecting(true);

    try {
      const loginUrl = await buildKeycloakLoginUrl({ prompt: "login" });
      window.location.replace(loginUrl);
    } catch (error) {
      console.error(error);
      setErrorMessage("Không thể chuyển đến trang đăng nhập. Vui lòng thử lại.");
      setIsRedirecting(false);
    }
  }

  if (isRedirecting) {
    return <FullPageCarLoader label="Đang chuyển đến đăng nhập..." />;
  }

  return (
    <div className="tw-fixed tw-inset-0 tw-flex tw-min-h-screen tw-min-h-[100dvh] tw-w-screen tw-items-center tw-justify-center tw-bg-slate-50/95 tw-px-5 tw-text-vm-slate-700">
      <section className="tw-grid tw-w-full tw-max-w-[430px] tw-place-items-center tw-gap-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-[#d9e2f2] tw-bg-white tw-px-8 tw-py-8 tw-text-center tw-shadow-[0_18px_45px_rgba(15,23,42,0.08)]">
        <span className="tw-inline-flex tw-h-16 tw-w-16 tw-items-center tw-justify-center tw-rounded-full tw-bg-brand-50 tw-text-[1.45rem] tw-text-vm-primary">
          <i className="fas fa-car-side" />
        </span>
        <div className="tw-grid tw-gap-2">
          <h1 className="tw-m-0 tw-text-[1.32rem] tw-font-black tw-text-vm-slate-900">Đã đăng xuất</h1>
          <p className="tw-m-0 tw-text-[0.9rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-500">
            Phiên làm việc đã được đóng. Khi cần tiếp tục sử dụng hệ thống, vui lòng đăng nhập lại.
          </p>
        </div>
        {errorMessage ? <AuthInlineNotice tone="error">{errorMessage}</AuthInlineNotice> : null}
        <Button className="tw-h-10 tw-w-full tw-rounded-vm-md tw-text-[0.9rem] tw-font-extrabold" loading={isRedirecting} type="button" onClick={handleLoginAgain}>
          Đăng nhập lại
        </Button>
      </section>
    </div>
  );
}

function KeycloakRedirectScreen({ label }: { label: string }) {
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

  return <FullPageCarLoader label={label} />;
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
                  disabled={!resendTargetEmail}
                  loading={isResendingVerification}
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

            <Button className="tw-h-[38px] tw-w-full tw-rounded-vm-md tw-text-[0.9rem] tw-font-extrabold" loading={isSubmitting} type="submit" variant="primary">
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
function ForgotPasswordScreen() {
  const location = useLocation();
  const [email, setEmail] = useState(() => {
    const searchParams = new URLSearchParams(location.search);
    const state = location.state as { email?: unknown } | null;
    const stateEmail = typeof state?.email === "string" ? state.email : "";

    return (searchParams.get("email") || stateEmail || sessionStorage.getItem(forgotPasswordEmailStorageKey) || "").trim();
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  useEffect(() => {
    const searchParams = new URLSearchParams(location.search);
    const state = location.state as { email?: unknown } | null;
    const stateEmail = typeof state?.email === "string" ? state.email.trim() : "";
    const nextEmail = (searchParams.get("email") || stateEmail).trim();

    if (!nextEmail) return;
    sessionStorage.setItem(forgotPasswordEmailStorageKey, nextEmail);
    setEmail(nextEmail);
  }, [location.search, location.state]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");
    setIsSubmitting(true);

    try {
      const response = await requestPasswordReset({ email });
      setSuccessMessage(response.message);
      sessionStorage.removeItem(forgotPasswordEmailStorageKey);
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

            <Button className="tw-h-[38px] tw-w-full tw-rounded-vm-md tw-text-[0.9rem] tw-font-extrabold" loading={isSubmitting} type="submit" variant="primary">
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
