# Thử nghiệm thu Qwen — attempt01

Java mới đã biên dịch và 50/50 test offline qua. Gồm984 câu chạy matcher/route trong fixture của 16 nguồn DRAFT;209 canonical, 209 output-validator PASS, 0 conflict. Chưa chấm semantic và không phải Qwen live.

- lightweight-compile.log, lightweight-offline.log, lightweight-summary.json: compile và JUnit thực.
- offline-observations.json, offline-metadata.json, offline-summary.json: corpus/query/answer/hash và loại bằng chứng.
- live-launch.log: launcher thật đã được gọi nhưng guard dừng trước inference vì RAM 418 MB dưới ngưỡng 1536 MB.
- readiness.json: snapshot RAM và trạng thái môi trường; giá trị thay đổi theo thời điểm.

Docker và LM Studio có model phù hợp; điều đó không chứng minh đã sinh trả lời qua Qwen. Không gọi Google hoặc thay dữ liệu/provider dev. Không ghi PASS về chất lượng hay p95 khi chưa có mẫu live.
