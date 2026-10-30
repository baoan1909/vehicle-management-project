import { createRoot, type Root } from "react-dom/client";

import { FullPageCarLoader } from "@/shared/components/ui/PageTransitionLoader";

class LoaderService {
  private container: HTMLDivElement | null = null;
  private pendingCount = 0;
  private root: Root | null = null;

  show(label?: string) {
    this.pendingCount += 1;
    if (this.container) return;

    this.container = document.createElement("div");
    this.container.dataset.vehicleFullPageLoader = "true";
    document.body.appendChild(this.container);

    this.root = createRoot(this.container);
    this.root.render(<FullPageCarLoader label={label} />);
  }

  hide() {
    this.pendingCount = Math.max(this.pendingCount - 1, 0);
    if (this.pendingCount > 0 || !this.container || !this.root) return;

    this.root.unmount();
    this.container.remove();
    this.container = null;
    this.root = null;
  }
}

export default new LoaderService();
