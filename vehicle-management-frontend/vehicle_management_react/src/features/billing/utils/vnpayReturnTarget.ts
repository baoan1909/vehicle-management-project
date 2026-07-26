const VNPAY_RETURN_TARGET_KEY = "billing.vnpay-return-target";

export function storeVnpayReturnTarget(path: string) {
  sessionStorage.setItem(VNPAY_RETURN_TARGET_KEY, path);
}

export function readVnpayReturnTarget() {
  return sessionStorage.getItem(VNPAY_RETURN_TARGET_KEY);
}

export function clearVnpayReturnTarget() {
  sessionStorage.removeItem(VNPAY_RETURN_TARGET_KEY);
}
