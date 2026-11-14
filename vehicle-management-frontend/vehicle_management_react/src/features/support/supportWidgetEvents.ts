export const OPEN_SUPPORT_WIDGET_EVENT = "coparking:open-support-widget";

export function openSupportWidget() {
  window.dispatchEvent(new Event(OPEN_SUPPORT_WIDGET_EVENT));
}
