# Phase 2 — pgvector, embedding và phiên bản chỉ mục

## Mục tiêu vận hành

- PostgreSQL phải có extension `vector` từ phiên bản `0.8.0`.
- PostgreSQL phải là phiên bản 17 vì schema baseline của dự án được tạo từ PostgreSQL 17.
- Cột `ai.knowledge_embeddings.embedding` phải là `vector(768)`.
- Mỗi embedding thuộc đúng một `knowledge_index_version`.
- Chỉ một phiên bản chỉ mục được ở trạng thái `ACTIVE`.
- Không sửa provider, model ID hoặc dimension của cấu hình đã được dùng để tạo chỉ mục.

## Khởi động PostgreSQL pgvector

Nếu cổng `5433` đang được PostgreSQL hiện tại sử dụng, chọn cổng khác, ví dụ `5434`:

```powershell
$env:DB_HOST_PORT="5434"
docker compose -f docker-compose.postgres-pgvector.yml up -d --force-recreate
docker ps --filter "name=vehicle-pgvector"
```

Nếu trước đó đã tạo volume bằng image PostgreSQL 16, phải sao lưu dữ liệu cần thiết rồi tạo volume PostgreSQL 17 mới. PostgreSQL không hỗ trợ dùng trực tiếp data directory của major version 16 cho version 17.

Không dùng `--remove-orphans`; các compose file khác trong cùng thư mục đang quản lý Keycloak, MinIO, RabbitMQ và OCR.

## Kiểm tra trước khi chạy migration

```sql
SELECT extname, extversion
FROM pg_extension
WHERE extname = 'vector';

SELECT COUNT(*) AS legacy_embedding_count
FROM ai.knowledge_embeddings
WHERE index_version_id IS NULL OR embedding IS NULL;
```

Migration sẽ dừng thay vì tự xóa dữ liệu nếu còn embedding cũ chưa gắn `index_version_id`. Phải sao lưu và backfill tường minh trước khi chạy lại.

## Cấu hình backend

Các giá trị tối thiểu cần cấu hình theo môi trường triển khai:

```text
DB_HOST=localhost
DB_PORT=5434
DB_NAME=vehicle_management_db
AI_EMBEDDING_ENABLED=true
AI_PGVECTOR_REQUIRED=true
AI_EMBEDDING_VECTOR_SEARCH_ENABLED=true
```

Không ghi API key vào source code hoặc log. API key Gemini chỉ được truyền qua secret của môi trường chạy.

## Health check

Actuator health có contributor `pgVector`. Contributor kiểm tra:

- extension `vector` tồn tại;
- phiên bản extension tối thiểu `0.8.0`;
- kiểu cột là `vector(768)`.

Ứng dụng fail-fast khi profile là `prod`/`production`, hoặc khi `AI_EMBEDDING_ENABLED=true`, hoặc `AI_PGVECTOR_REQUIRED=true` mà pgvector chưa sẵn sàng.

## Quy trình tạo và kích hoạt chỉ mục

1. Đồng bộ catalog Gemini và xác nhận model hỗ trợ `embedContent`.
2. Tạo cấu hình use case `EMBEDDING`, dimension `768`.
3. Tạo phiên bản chỉ mục `DRAFT`.
4. Bắt đầu build. Hệ thống snapshot model, prompt version, số lượng chunk và checksum corpus.
5. Chờ trạng thái `READY`; không kích hoạt bản `FAILED` hoặc bản có corpus đã thay đổi.
6. Kích hoạt bản `READY`. Bản `ACTIVE` trước đó chuyển thành `RETIRED`.
7. Theo dõi `/actuator/health`, lỗi provider, tỷ lệ fallback lexical và chất lượng truy vấn.

## Rollback ứng dụng

Trong màn hình quản trị, chọn một phiên bản `RETIRED` đã từng được kích hoạt và nhấn **Quay lại**. Backend chỉ cho phép rollback khi:

- vẫn có một phiên bản `ACTIVE` hiện tại;
- model/provider/dimension của bản cũ còn khớp cấu hình;
- embedding đủ và hợp lệ;
- checksum kho tri thức chưa thay đổi.

Nếu corpus đã thay đổi, không rollback index cũ; hãy tạo và build một phiên bản mới.

## Rollback database

Không chạy down migration trực tiếp trong production. Trước rollout phải tạo backup logic hoặc snapshot volume. Khi migration thất bại:

1. Dừng backend để ngăn ghi mới.
2. Lưu log Flyway và kết quả health check.
3. Khôi phục snapshot database trước migration.
4. Triển khai lại artifact backend trước Phase 2.
5. Chỉ mở traffic sau khi Hibernate schema validation và health check đạt.

Volume của container chỉ được xóa trong môi trường phát triển khi chắc chắn không cần dữ liệu:

```powershell
docker compose -f docker-compose.postgres-pgvector.yml down -v
```
