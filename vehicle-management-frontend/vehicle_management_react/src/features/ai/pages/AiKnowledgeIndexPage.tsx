import { AiKnowledgeAdminPage } from "./AiKnowledgeAdminPage";

/**
 * Backward-compatible alias. The admin knowledge UI now lives at
 * /admin/ai-knowledge with tabs; this keeps old imports compiling.
 */
export function AiKnowledgeIndexPage() {
  return <AiKnowledgeAdminPage />;
}