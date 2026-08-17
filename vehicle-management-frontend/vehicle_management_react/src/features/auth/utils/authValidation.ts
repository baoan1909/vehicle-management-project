export const authFieldLimits = {
  emailMaxLength: 255,
  fullNameMaxLength: 150,
  fullNameMinLength: 2,
  passwordMaxLength: 64,
  passwordMinLength: 8,
  usernameMaxLength: 50,
  usernameMinLength: 4,
} as const;

export type RegisterValidationValues = {
  confirmPassword: string;
  email: string;
  fullName: string;
  password: string;
  username: string;
};

export type RegisterFieldErrors = Partial<Record<keyof RegisterValidationValues, string>>;

const fullNamePattern = /^[\p{L}\p{M}]+(?:[ '\-\u2019][\p{L}\p{M}]+)*$/u;
const usernamePattern = /^[A-Za-z][A-Za-z0-9]*(?:[._][A-Za-z0-9]+)*$/;
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateEmail(value: string) {
  const email = value.trim();
  if (!email) return "Vui lòng nhập email.";
  if (email.length > authFieldLimits.emailMaxLength) return "Email không được vượt quá 255 ký tự.";
  if (!emailPattern.test(email)) return "Địa chỉ email không hợp lệ.";
  return "";
}

export function validateRegisterValues(values: RegisterValidationValues): RegisterFieldErrors {
  const errors: RegisterFieldErrors = {};
  const fullName = values.fullName.trim();
  const username = values.username.trim();

  if (!fullName) {
    errors.fullName = "Vui lòng nhập họ và tên.";
  } else if (fullName.length < authFieldLimits.fullNameMinLength || fullName.length > authFieldLimits.fullNameMaxLength) {
    errors.fullName = "Họ và tên phải có từ 2 đến 150 ký tự.";
  } else if (!fullNamePattern.test(fullName)) {
    errors.fullName = "Họ và tên chỉ gồm chữ cái, khoảng trắng, dấu nháy đơn hoặc dấu gạch nối.";
  }

  if (!username) {
    errors.username = "Vui lòng nhập tên đăng nhập.";
  } else if (username.length < authFieldLimits.usernameMinLength || username.length > authFieldLimits.usernameMaxLength) {
    errors.username = "Tên đăng nhập phải có từ 4 đến 50 ký tự.";
  } else if (!usernamePattern.test(username)) {
    errors.username = "Tên đăng nhập phải bắt đầu bằng chữ cái và chỉ gồm chữ không dấu, số, dấu chấm hoặc gạch dưới.";
  }

  const emailError = validateEmail(values.email);
  if (emailError) errors.email = emailError;

  if (!values.password) {
    errors.password = "Vui lòng nhập mật khẩu.";
  } else if (values.password.length < authFieldLimits.passwordMinLength || values.password.length > authFieldLimits.passwordMaxLength) {
    errors.password = "Mật khẩu phải có từ 8 đến 64 ký tự.";
  } else if (/\s/u.test(values.password)) {
    errors.password = "Mật khẩu không được chứa khoảng trắng.";
  } else if (
    !/\p{Lu}/u.test(values.password)
    || !/\p{Ll}/u.test(values.password)
    || !/\p{Nd}/u.test(values.password)
    || !/[^\p{L}\p{N}\s]/u.test(values.password)
  ) {
    errors.password = "Mật khẩu phải có chữ hoa, chữ thường, chữ số và ký tự đặc biệt.";
  }

  if (!values.confirmPassword) {
    errors.confirmPassword = "Vui lòng xác nhận mật khẩu.";
  } else if (values.confirmPassword !== values.password) {
    errors.confirmPassword = "Mật khẩu xác nhận không khớp.";
  }

  return errors;
}
