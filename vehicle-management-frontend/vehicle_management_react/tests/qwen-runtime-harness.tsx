import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { AiRuntimeModePanel } from "../src/features/ai/pages/components/AiRuntimeModePanel";
import "@/app/styles/tailwind.css";
import "@/app/styles/globals.css";
createRoot(document.getElementById("root")!).render(<StrictMode><AiRuntimeModePanel canManage={!new URLSearchParams(location.search).has("readonly")} /></StrictMode>);
