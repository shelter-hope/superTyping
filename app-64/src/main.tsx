import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

// 字体本地打包（离线可用，中文衬线走 unicode-range 分片按需加载）
import "@fontsource/inter/400.css";
import "@fontsource/inter/500.css";
import "@fontsource/inter/600.css";
import "@fontsource/noto-serif-sc/400.css";
import "@fontsource/noto-serif-sc/500.css";
import "@fontsource/noto-serif-sc/700.css";
import "@fontsource/jetbrains-mono/400.css";
import "@fontsource/jetbrains-mono/500.css";

import "./index.css";
import App from "./App.tsx";
import { AppErrorBoundary } from "./components/common/AppErrorBoundary.tsx";
import { AppWrapper } from "./components/common/PageMeta.tsx";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <AppErrorBoundary>
      <AppWrapper>
        <App />
      </AppWrapper>
    </AppErrorBoundary>
  </StrictMode>
);
