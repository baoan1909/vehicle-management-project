# Truy vết đợt 1 — 19/11/2026

| Capability/actor | UI | API | Policy/use case | DB | Tool | Knowledge | Test |
|---|---|---|---|---|---|---|---|
| Đăng ký/reset/Google — public | /register, /login, /forgot-password; LoginPage | PublicAuthController/social bootstrap | PublicAuthPolicy/PublicAuthUseCaseImpl, SocialAccountPolicy | iam.accounts, account_identities; people.user_profiles | Chưa có tool auth/profile; chỉ hướng dẫn | authentication-and-profile | AUTH01–AUTH10; PublicAuth*Test, social bootstrap tests |
| Hồ sơ riêng — current account | /customer/profile, /admin/profile | AccountProfileController | current account/permission; không dùng RAG xác định status | account/profile/customer approval riêng | Chưa có profile tool; không suy đoán | authentication-and-profile | AUTH08; PERSONAL_DATA protocol tests |
| An toàn/PII — public/own | guest/auth widget | public support/operations chat | PiiRedactionService, AssistantIntentRouter, scope resolver | audit payload redacted | Chỉ registry allowlist, OWN | chatbot-privacy-and-prohibited-data | PRIV01–PRIV10, GuestPublicAssistantServiceTest, AiToolCall*Test |
| Chat khách–đối tác | chat CUSTOMER_DIRECT | ChatConversationController | membership, send permission; context ticket bị chặn | operations.chat_* | Không create/share ticket vào kênh này | chat-conversations-and-support-boundaries | CHAT01–CHAT10; SupportTicketConversationServiceTest, support-flows browser |
| Hỗ trợ khách/nhân sự → hệ thống | /customer/support, /admin/my-support; ASSISTANT_SUPPORT/SUPPORT_REQUESTER_DIRECT | SupportTicketController, from-conversations | requester account, category availability, owner, ACTIVE | support_tickets, links, events | create_support_ticket có confirm; read OWN | support-ticket-and-escalation | SUP01–SUP10; support/context/category/security tests |
| Hỏi lại/nối tiếp — public/own | cả hai widget | contextQuestion optional (guest); history cùng actor (auth) | AssistantQuestionResolver, không copy ID/action/scope | không thay trạng thái nghiệp vụ | không tool khi thiếu topic | cả bốn | 240 route cases, context unit/orchestrator tests, guest browser |
| RAG/citation | citation UI | knowledge retrieval/citation endpoints | source active, document current/effective, scope; JSON labels allowlist | knowledge_*, retrieval audit | read only | manifest 13 docs / 4 batch | Phase1KnowledgeRetrievalIT; malformed-label and cache tests |

## Phân biệt bằng chứng

Route case PASS không chứng minh retrieval/câu trả lời đúng. PostgreSQL IT dùng vector synthetic cho scope/version; method live opt-in mới dùng embedding/generation provider thật. Browser fixture dùng API mock, không thay thế backend authorization. Xem batch-01-report.md cho số thực chạy và transcript.

## Gap giữ nguyên

Không có tool đọc trạng thái hồ sơ hoặc escalation/assign/resolve/close. SLA/reopen/appeal mục tiêu cũ chưa mặc định enforced. Corpus dev chưa được nhập lại. Tài liệu nội bộ EMPLOYEE/ADMIN ngoài scope bốn file được lên backlog, không hạ scope CUSTOMER.


## Kiểm tra bổ sung từ đánh giá thực tế

| Rủi ro | Code | Bằng chứng |
|---|---|---|
| Chunk/embedding cũ so với document | KnowledgeChunkRepository hybrid/lexical/vector SQL | Phase1KnowledgeRetrievalIT.staleDocumentVersionCannotBeUsedAsCurrentEvidence, parentScopeRevocationAndStaleEmbeddingFailClosed |
| Cache nguồn đã bị thu hồi | KnowledgeRetrievalService → areChunksEligible → bounded SQL | Phase1KnowledgeRetrievalIT.cachedEvidenceMustBeReauthorizedAgainstCurrentDatabase; cold/hot có DB thật, cache port test double |
| Citation sau khi mất quyền | AiMessageCitationUseCaseImpl + KnowledgeAccessContextResolver | AiMessageCitationUseCaseImplTest: cùng member nhưng eligibility lần sau false, không trả title/section |
| Tự ghi/retry | AiToolCallUseCaseImpl + AiToolExecutionService | AiToolCallUseCaseImplTest: confirm once, deny, expired, other actor, executing retry, revoked permission |
| Nối tiếp bị trộn tương lai | AssistantOrchestrator lấy history trước input.createdAt | AssistantOrchestratorTest và AssistantQuestionResolverTest |
| Lexical giữ cả cụm hỏi | VietnameseQueryNormalizer.forLexicalSearch; policy-v2 | VietnameseLexicalQueryTest và live retrieval gate (không đổi threshold/topK) |
