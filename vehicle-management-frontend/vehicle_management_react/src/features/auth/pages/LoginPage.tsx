import { useEffect, useState, type FormEvent } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { getLogoutRedirectPath, isLogoutRedirectGuardActive } from "@/core/auth/logout";
import {
  clearAuthTokens,
  getCurrentUserFromAccessToken,
  getIdentityProviderFromAccessToken,
  saveAuthTokens,
} from "@/core/auth/session";
import { useAuth } from "@/core/auth/useAuth";
import { bootstrapSocialAccount, getMyAccountProfile } from "@/features/iam/api/accountProfileApi";
import { mergeCurrentUserWithAccountProfile } from "@/features/iam/utils/accountProfileMapper";
import type { CurrentUser } from "@/shared/types/common";
import {
  buildKeycloakLoginUrl,
  buildKeycloakLogoutUrl,
  exchangeKeycloakAuthorizationCode,
  prepareKeycloakLoginUrl,
  registerAccount,
  resendVerificationEmail,
  requestPasswordReset,
  type RegisterAccountRequest,
} from "@/features/auth/api/authApi";
import { AuthBrandMark, AuthFormField, AuthFormSectionTitle, AuthInlineNotice, AuthPasswordInput } from "@/features/auth/components/AuthFormControls";
import {
  authFieldLimits,
  validateEmail,
  validateRegisterValues,
  type RegisterFieldErrors,
} from "@/features/auth/utils/authValidation";
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
const socialLoginErrorStorageKey = "vm_social_login_error";
let activeAuthorizationCode = "";

function resolvePostLoginRedirectPath(user: CurrentUser | null) {
  if (user?.role === "CUSTOMER") {
    return user.onboardingRequired ? "/customer/profile" : customerPostLoginRedirectPath;
  }
  return adminPostLoginRedirectPath;
}

async function resolveLoggedInUser(accessToken: string) {
  const tokenUser = getCurrentUserFromAccessToken(accessToken);
  if (!tokenUser) return null;

  if (getIdentityProviderFromAccessToken(accessToken) === "google") {
    await bootstrapSocialAccount();
    const response = await getMyAccountProfile();
    return mergeCurrentUserWithAccountProfile(tokenUser, response.data);
  }

  try {
    const response = await getMyAccountProfile();
    return mergeCurrentUserWithAccountProfile(tokenUser, response.data);
  } catch {
    return tokenUser;
  }
}

export function LoginPage({ mode = "login" }: AuthPageProps) {
  if (mode === "login" && isLogoutRedirectGuardActive()) {
    return <Navigate to={getLogoutRedirectPath()} replace />;
  }

  if (mode === "otp" || mode === "recover") {
    return <Navigate to="/forgot-password" replace />;
  }

  if (mode === "register") {
    return <RegisterScreenV2 />;
  }

  if (mode === "forgot") {
    return <ForgotPasswordScreen />;
  }

  return (
    <KeycloakRedirectScreen label="Đang chuyển đến đăng nhập..." />
  );
}

function KeycloakRedirectScreen({ label }: { label: string }) {
  const location = useLocation();
  const navigate = useNavigate();
  const { setUser } = useAuth();
  const [isExchangingCode, setIsExchangingCode] = useState(false);
  const [loginError, setLoginError] = useState(
    () => sessionStorage.getItem(socialLoginErrorStorageKey) ?? "",
  );

  useEffect(() => {
    const searchParams = new URLSearchParams(location.search);
    const code = searchParams.get("code");
    const returnedState = searchParams.get("state");
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
      let idToken: string | undefined;

      try {
        const tokenResponse = await exchangeKeycloakAuthorizationCode(authorizationCode, returnedState);
        idToken = tokenResponse.id_token;
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
        const message = error instanceof Error ? error.message : "Không thể hoàn tất đăng nhập Google.";
        sessionStorage.setItem(socialLoginErrorStorageKey, message);
        clearAuthTokens();
        if (idToken) {
          window.location.replace(buildKeycloakLogoutUrl(idToken, "/login"));
          return;
        }
        setLoginError(message);
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
    if (code || loginError) return;

    async function redirectToKeycloak() {
      try {
        const loginUrl = await prepareKeycloakLoginUrl();
        window.location.replace(loginUrl);
      } catch (error) {
        console.error(error);
      }
    }

    void redirectToKeycloak();
  }, [location.search, loginError]);

  async function retryLogin() {
    sessionStorage.removeItem(socialLoginErrorStorageKey);
    setLoginError("");
    const loginUrl = await buildKeycloakLoginUrl({ prompt: "login" });
    window.location.replace(loginUrl);
  }

  if (loginError) {
    return (
      <div className="tw-fixed tw-inset-0 tw-flex tw-items-center tw-justify-center tw-bg-[linear-gradient(180deg,#f8fbff_0%,#eef5ff_100%)] tw-p-5">
        <section className="tw-w-full tw-max-w-[520px] tw-rounded-vm-md tw-border tw-border-solid tw-border-[#d9e2f2] tw-bg-white tw-p-8 tw-shadow-[0_18px_45px_rgba(15,23,42,0.08)]">
          <div className="tw-mb-5 tw-text-center"><AuthBrandMark /></div>
          <AuthInlineNotice tone="error">{loginError}</AuthInlineNotice>
          <Button className="tw-mt-5 tw-w-full" type="button" variant="primary" onClick={() => void retryLogin()}>
            Thử lại với tài khoản Google khác
          </Button>
        </section>
      </div>
    );
  }

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
  const [fieldErrors, setFieldErrors] = useState<RegisterFieldErrors>({});
  const resendTargetEmail = (lastVerificationEmail || form.email).trim();

  function updateField<Key extends keyof RegisterFormState>(field: Key, value: RegisterFormState[Key]) {
    setForm((currentValue) => ({ ...currentValue, [field]: value }));
    setFieldErrors((currentErrors) => {
      if (!currentErrors[field]) return currentErrors;
      const nextErrors = { ...currentErrors };
      delete nextErrors[field];
      return nextErrors;
    });
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");

    const validationErrors = validateRegisterValues(form);
    setFieldErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) {
      setErrorMessage("Vui lòng kiểm tra lại các thông tin đăng ký.");
      return;
    }

    if (!acceptedTerms) {
      setErrorMessage("Vui lòng đồng ý điều khoản sử dụng và chính sách bảo mật.");
      return;
    }

    setIsSubmitting(true);

    const verificationEmail = form.email.trim();
    const payload: RegisterAccountRequest = {
      fullName: form.fullName.trim(),
      username: form.username.trim(),
      email: verificationEmail,
      password: form.password,
    };

    try {
      const response = await registerAccount(payload);
      setSuccessMessage(response.message);
      setLastVerificationEmail(verificationEmail);
      setForm(initialRegisterForm);
      setFieldErrors({});
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

    const validationError = validateEmail(resendTargetEmail);
    if (validationError) {
      setErrorMessage(validationError);
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
    <div className="tw-fixed tw-inset-0 tw-flex tw-min-h-screen tw-min-h-[100dvh] tw-w-screen tw-flex-col tw-overflow-y-auto tw-bg-[linear-gradient(180deg,#f8fbff_0%,#eef5ff_100%)] tw-px-5 tw-py-2 tw-text-vm-slate-700">
      <main className="tw-mx-auto tw-flex tw-w-full tw-max-w-[920px] tw-flex-1 tw-flex-col tw-items-stretch tw-justify-center">
        <section className="tw-rounded-vm-md tw-border tw-border-solid tw-border-[#d9e2f2] tw-bg-white tw-px-8 tw-pb-4 tw-pt-4 tw-shadow-[0_18px_45px_rgba(15,23,42,0.08)] max-[768px]:tw-px-5">
          <header className="tw-mx-auto tw-mb-4 tw-text-center">
            <AuthBrandMark />
            <h1 className="tw-m-0 tw-mt-2 tw-text-[1.3rem] tw-font-black tw-leading-tight tw-text-vm-slate-900">Tạo tài khoản mới</h1>
            <p className="tw-mx-auto tw-mb-0 tw-mt-1 tw-max-w-[560px] tw-text-[0.86rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">
              Vui lòng nhập thông tin để tạo tài khoản và sử dụng hệ thống.
            </p>
          </header>

          <form className="tw-grid tw-gap-3" noValidate onSubmit={handleSubmit}>
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
                  error={fieldErrors.fullName}
                  maxLength={authFieldLimits.fullNameMaxLength}
                  minLength={authFieldLimits.fullNameMinLength}
                  placeholder="Nhập họ và tên"
                  required
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
                  error={fieldErrors.username}
                  maxLength={authFieldLimits.usernameMaxLength}
                  minLength={authFieldLimits.usernameMinLength}
                  placeholder="Nhập tên đăng nhập"
                  required
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
                  error={fieldErrors.email}
                  maxLength={authFieldLimits.emailMaxLength}
                  placeholder="Nhập email của bạn"
                  required
                  type="email"
                  value={form.email}
                  onChange={(value) => updateField("email", value)}
                />
                <AuthPasswordInput
                  autoComplete="new-password"
                  id="registerPassword"
                  label="Mật khẩu"
                  error={fieldErrors.password}
                  maxLength={authFieldLimits.passwordMaxLength}
                  minLength={authFieldLimits.passwordMinLength}
                  placeholder="Nhập mật khẩu"
                  required
                  value={form.password}
                  onChange={(value) => updateField("password", value)}
                />
                <AuthPasswordInput
                  autoComplete="new-password"
                  id="confirmPassword"
                  label="Xác nhận mật khẩu"
                  error={fieldErrors.confirmPassword}
                  maxLength={authFieldLimits.passwordMaxLength}
                  minLength={authFieldLimits.passwordMinLength}
                  placeholder="Nhập lại mật khẩu"
                  required
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
  const [emailError, setEmailError] = useState("");

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
    const normalizedEmail = email.trim();
    const validationError = validateEmail(normalizedEmail);
    setEmailError(validationError);
    if (validationError) return;
    setIsSubmitting(true);

    try {
      const response = await requestPasswordReset({ email: normalizedEmail });
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
    <div className="tw-fixed tw-inset-0 tw-flex tw-min-h-screen tw-min-h-[100dvh] tw-w-screen tw-flex-col tw-overflow-y-auto tw-bg-[linear-gradient(180deg,#f8fbff_0%,#eef5ff_100%)] tw-px-5 tw-py-4 tw-text-vm-slate-700">
      <main className="tw-mx-auto tw-flex tw-w-full tw-max-w-[520px] tw-flex-1 tw-flex-col tw-items-stretch tw-justify-center">
        <section className="tw-rounded-vm-md tw-border tw-border-solid tw-border-[#d9e2f2] tw-bg-white tw-px-8 tw-pb-6 tw-pt-6 tw-shadow-[0_18px_45px_rgba(15,23,42,0.08)] max-[480px]:tw-px-5">
          <header className="tw-mx-auto tw-mb-5 tw-text-center">
            <AuthBrandMark />
            <h1 className="tw-m-0 tw-mt-4 tw-text-[1.35rem] tw-font-black tw-leading-tight tw-text-vm-slate-900">Đặt lại mật khẩu</h1>
          </header>

          <form className="tw-grid tw-gap-4" noValidate onSubmit={handleSubmit}>
            {successMessage ? <AuthInlineNotice tone="success">{successMessage}</AuthInlineNotice> : null}
            {errorMessage ? <AuthInlineNotice tone="error">{errorMessage}</AuthInlineNotice> : null}

            <AuthFormField
              autoComplete="email"
              id="forgotEmail"
              icon="far fa-envelope"
              label="Email"
              error={emailError}
              maxLength={authFieldLimits.emailMaxLength}
              placeholder="Nhập email của bạn"
              required
              type="email"
              value={email}
              onChange={(value) => {
                setEmail(value);
                setEmailError("");
              }}
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
