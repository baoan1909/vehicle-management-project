import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "@/app/App";
import { AppProviders } from "@/app/providers/AppProviders";
import { initializeApplicationTime } from "@/shared/time/applicationTime";
import "@/app/styles/globals.css";

async function bootstrap() {
  try {
    await initializeApplicationTime();
  } catch (error) {
    console.error("Không thể tải múi giờ ứng dụng; giao diện tạm dùng UTC.", error);
  }

  createRoot(document.getElementById("root") as HTMLElement).render(
    <StrictMode>
      <AppProviders>
        <App />
      </AppProviders>
    </StrictMode>,
  );
}

void bootstrap();
