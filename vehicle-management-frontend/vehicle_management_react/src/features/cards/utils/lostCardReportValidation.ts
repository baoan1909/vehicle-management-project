export const lostCardFieldLimits = {
  identifyCardMaxLength: 12,
  identifyCardMinLength: 9,
  noteMaxLength: 500,
  registrationLicenseMaxLength: 50,
  registrationLicenseMinLength: 5,
  reporterNameMaxLength: 150,
  reporterNameMinLength: 2,
  reporterPhoneMaxLength: 12,
} as const;

export type LostCardReportFormValues = {
  identifyCard: string;
  note: string;
  registrationLicense: string;
  reporterName: string;
  reporterPhone: string;
  timeOfLost: string;
};

export type LostCardReportFieldErrors = Partial<
  Record<keyof LostCardReportFormValues | "evidence", string>
>;

const reporterNamePattern = /^[\p{L}\p{M}]+(?:[ '\-\u2019][\p{L}\p{M}]+)*$/u;
const vietnamPhonePattern = /^(?:0[35789][0-9]{8}|\+84[35789][0-9]{8})$/;
const identifyCardPattern = /^[0-9]{9,12}$/;
const registrationLicensePattern = /^[\p{L}\p{N} ./-]+$/u;
const backendDateTimePattern = /^(\d{2}):(\d{2}) (\d{2})-(\d{2})-(\d{4})$/;

export function parseLostCardDateTime(value: string | Date | null | undefined): Date | null {
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value;
  }

  if (!value?.trim()) return null;

  const directDate = new Date(value);
  if (!Number.isNaN(directDate.getTime())) return directDate;

  const match = backendDateTimePattern.exec(value.trim());
  if (!match) return null;

  const [, hourText, minuteText, dayText, monthText, yearText] = match;
  const hour = Number(hourText);
  const minute = Number(minuteText);
  const day = Number(dayText);
  const month = Number(monthText);
  const year = Number(yearText);
  const parsedDate = new Date(year, month - 1, day, hour, minute);

  if (
    parsedDate.getFullYear() !== year
    || parsedDate.getMonth() !== month - 1
    || parsedDate.getDate() !== day
    || parsedDate.getHours() !== hour
    || parsedDate.getMinutes() !== minute
  ) {
    return null;
  }

  return parsedDate;
}

export function validateLostCardReportForm(
  values: LostCardReportFormValues,
  checkInTime?: string | null,
): LostCardReportFieldErrors {
  const errors: LostCardReportFieldErrors = {};
  const reporterName = values.reporterName.trim();
  const reporterPhone = values.reporterPhone.trim();
  const identifyCard = values.identifyCard.trim();
  const registrationLicense = values.registrationLicense.trim();
  const note = values.note.trim();

  if (!values.timeOfLost) {
    errors.timeOfLost = "Vui lòng nhập thời gian mất thẻ.";
  } else {
    const lostAt = parseLostCardDateTime(values.timeOfLost);
    if (!lostAt) {
      errors.timeOfLost = "Thời gian mất thẻ không hợp lệ.";
    } else if (lostAt.getTime() > Date.now()) {
      errors.timeOfLost = "Thời gian mất thẻ không được ở tương lai.";
    } else if (checkInTime) {
      const checkInAt = parseLostCardDateTime(checkInTime);
      if (checkInAt && lostAt.getTime() < checkInAt.getTime()) {
        errors.timeOfLost = "Thời gian mất thẻ không được trước thời gian check-in.";
      }
    }
  }

  if (!reporterName) {
    errors.reporterName = "Vui lòng nhập người báo mất.";
  } else if (
    reporterName.length < lostCardFieldLimits.reporterNameMinLength
    || reporterName.length > lostCardFieldLimits.reporterNameMaxLength
  ) {
    errors.reporterName = "Tên người báo mất phải có từ 2 đến 150 ký tự.";
  } else if (!reporterNamePattern.test(reporterName)) {
    errors.reporterName = "Tên chỉ được gồm chữ cái, khoảng trắng, dấu nháy đơn hoặc dấu gạch nối.";
  }

  if (!reporterPhone) {
    errors.reporterPhone = "Vui lòng nhập số điện thoại.";
  } else if (!vietnamPhonePattern.test(reporterPhone)) {
    errors.reporterPhone = "Số điện thoại phải có dạng 0xxxxxxxxx hoặc +84xxxxxxxxx.";
  }

  if (identifyCard && !identifyCardPattern.test(identifyCard)) {
    errors.identifyCard = "CCCD/CMND phải có từ 9 đến 12 chữ số.";
  }

  if (registrationLicense) {
    if (
      registrationLicense.length < lostCardFieldLimits.registrationLicenseMinLength
      || registrationLicense.length > lostCardFieldLimits.registrationLicenseMaxLength
    ) {
      errors.registrationLicense = "Giấy đăng ký xe phải có từ 5 đến 50 ký tự.";
    } else if (!registrationLicensePattern.test(registrationLicense)) {
      errors.registrationLicense = "Giấy đăng ký xe chứa ký tự không hợp lệ.";
    }
  }

  if (!identifyCard && !registrationLicense) {
    errors.evidence = "Vui lòng nhập CCCD/CMND hoặc giấy đăng ký xe.";
  }

  if (note.length > lostCardFieldLimits.noteMaxLength) {
    errors.note = "Ghi chú không được vượt quá 500 ký tự.";
  }

  return errors;
}
