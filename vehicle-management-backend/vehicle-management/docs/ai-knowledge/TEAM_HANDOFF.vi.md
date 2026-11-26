# Bàn giao chatbot Google API — 27/11/2026

## Chạy dự án

Backend cần JDK 21, Maven Wrapper, Docker cho hạ tầng/test. Frontend cần Node/npm. Tạo cấu hình từ .env.example, điền key Google riêng và không đưa .env thật lên Git. Dự án không còn cần LM Studio hay model tải về máy.

Giữ nguyên GEMINI_API_KEY, GEMINI_PROJECT_ID và các cặp dự phòng _1 đến _4, GEMINI_BASE_URL. Luồng đổi model/key, giới hạn quota, timeout, circuit breaker và quyền dùng dữ liệu vẫn được giữ. Không có key hợp lệ hoặc hết quota thì báo lỗi có kiểm soát; không gọi nhà cung cấp local.

## Kho tri thức

Giữ đủ 16 tài liệu trong phase-0/knowledge và bản soạn thảo phase-0/authoring/knowledge. Tài liệu vẫn DRAFT cho đến khi được rà soát và duyệt qua luồng quản trị. Tài liệu upload cần đúng source, scope, phiên bản, hiệu lực và dấu duyệt.

Bốn đợt có 984 câu (240/240/240/264), bộ nền 120 câu; 24 câu bổ sung ở additional-evaluation.jsonl, tách khỏi runtime. Các kết quả cũ không phải nghiệm thu Google live.

Chạy từ backend:

```powershell
node --test scripts/test-sync-ai-knowledge.mjs
node scripts/sync-ai-knowledge.mjs
.\mvnw.cmd test
```

Frontend: npm ci, npm run lint, npm run build, npm run dev. Chạy từng pha để giảm tải; không vừa compile vừa mở nhiều test DB/browser. Không tự gọi Google live để kiểm key hoặc tăng độ phủ khi chưa được yêu cầu.

## Chuyển DB cũ sang Google-only

Migration V20261126120000__retire_local_ai_provider.sql chạy bởi Flyway khi backend khởi động. Dừng bản backend cũ trước khi nâng cấp, sao lưu DB theo quy trình của team. Không sửa/xóa migration đã áp dụng.

Migration lưu snapshot dữ liệu AI local và audit điều phối cũ vào ai.retired_provider_records, rồi loại cấu hình/model/vector/index local khỏi các bảng đang dùng. Không đổi vector local thành vector Google. Tác vụ assistant local chưa hoàn tất bị dừng với AI_PROVIDER_REMOVED, không tự gửi câu hỏi cũ sang Google. Tài liệu/tác vụ xử lý local chưa hoàn tất được dừng để quản trị viên chủ động xử lý lại, không tự tải nội dung sang Google. Các cột/bảng chuyển chế độ và trigger không dùng nữa được gỡ.

Tài liệu, hội thoại, tin nhắn, trích nguồn và dữ liệu Google được giữ. Google index đã ACTIVE vẫn được giữ; nếu chưa có thì cần dựng/bật bằng model embedding Google. Tài liệu chờ duyệt từng chỉ có staged vector local cần xử lý lại bằng Google trước khi duyệt.

Bảng lưu lịch sử không được ứng dụng dùng để sinh câu trả lời. Không gọi model, đăng ký cấu hình hoặc kích hoạt index tự động từ dữ liệu này. Việc gỡ source không đồng nghĩa migration đã chạy trên DB dev của người dùng.

## Giới hạn nghiệm thu

Google live còn cần kiểm bằng đáp án chuẩn qua API chatbox; kết quả offline, FAQ hoặc nhà cung cấp cũ không chứng minh Google đạt các ngưỡng. Báo riêng câu FAQ, câu Google sinh, tool và hỏi lại. Không dùng HTTP 200 làm điểm đúng nghiệp vụ. Xem ALL_BATCHES_REVIEW_STATUS.vi.md.
