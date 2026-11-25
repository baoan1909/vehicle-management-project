# Runbook dự kiến reset/reimport corpus dev — chưa thực thi

## Phạm vi

Đợt 1 chỉ kiểm kê, đồng bộ file và thử trên PostgreSQL Testcontainers. Không có SQL migration purge, không publish vào DB dev, không sửa Flyway history, không xóa file MinIO/cache dùng chung.

## Tiền kiểm khi thực hiện ở đợt được giao reset

1. Xác nhận môi trường dev bằng marker cấu hình server và DB cụ thể, không dựa duy nhất tên host.
2. Chốt manifest ID source/document/version/index/object key; kiểm kê FK knowledge blocks/chunks/embeddings/jobs/events/audit results/citations. Không dùng tên tài liệu làm điều kiện xóa duy nhất.
3. Export metadata và file, ghi checksum; thử restore trong DB tách biệt. Giữ cấu hình model/permission nghiệp vụ không thuộc corpus.
4. Dừng worker ingestion/index/assistant liên quan, kiểm tra lease và job đang xử lý.
5. Chạy dry-run: hiển thị ID/count dự kiến, kiểm tra không có record ngoài manifest; mismatch thì dừng transaction.

## Thay corpus

- Maintenance/migration chỉ ở location dev được bật rõ ràng; không đưa destructive migration vào Flyway location chung.
- Dọn theo FK, gồm stale/failed index thuộc corpus đã chốt; không TRUNCATE CASCADE toàn schema.
- Citation lịch sử phải được archive/đánh dấu nguồn không còn khả dụng; không tự nối citation cũ tới tài liệu mới và không gây lỗi mở chat.
- SQL không dọn MinIO. Chỉ dọn object key manifest sau kiểm tra corpus mới và khả năng phục hồi, không xóa bucket.
- Reimport theo manifest, kiểm doc_version/chunker/prompt/model/dimension, số chunk và scope. Một index ACTIVE hợp lệ.
- Đổi corpus/cache namespace hoặc targeted invalidation; không FLUSHALL. Kiểm câu trả lời cũ không được phục vụ lại từ cache.
- Test chạy lại không tạo bản sao; lỗi giữa chừng rollback/restore; kiểm orphan, link citation, scope và các số lượng.

## Phục hồi

Trả index/corpus snapshot tương ứng, phục hồi citation/object key cùng phiên bản; đổi namespace cache để không trộn hai corpus. Chỉ bật worker sau khi smoke retrieval/citation theo từng scope đạt. Nghiệp vụ khách hàng/ticket/chat không thuộc phạm vi xóa mặc định.
