# Evidence Qwen/Google — 24/11/2026

- `backend-224-tests.log`: gate cuối 224 test qua, gồm 7 test PostgreSQL/HTTP counter. Provider không được chọn có 0 request ở các đường được kiểm tra.
- `frontend-typescript.log`, `frontend-2-tests.log`: TypeScript và 2 browser test qua; browser dùng API fixture.
- `metadata.json`: snapshot cấu hình, corpus manifest hash, harness hash của run03, BE HTTP port và số câu dự kiến. Hash harness tại thời điểm chạy khác bản sau khi bổ sung fail-fast; không sửa lại snapshot để che thay đổi.
- `vectors.json`: 61 vector local thật, input SHA256 và độ trễ; không chứa API key.
- `observations.json`: 11 câu đã ghi, có dấu/không dấu/lỗi gõ, retrieval, bằng chứng và lỗi; raw transcript giữ nguyên.
- `summary.json`: tóm tắt quan sát, không phải nghiệm thu nội dung. Phân biệt lỗi retrieval hạ tầng với pack rỗng trong transcript cũ.
- `resources-run-03.csv`: mẫu RAM/VRAM thực trong lượt lỗi, không phải benchmark chuẩn.
- `stopped.json`, `live-run-03-failed.log`: lý do dừng và lỗi hạ tầng; không có lời tuyên bố live PASS.
- `source-hashes.json`: hash nguồn hiện tại, tách biệt snapshot của lượt đã chạy.

Không có artifact HTTP-count hoàn tất của run03 do DB/test bị dừng trước bước xuất. Không dùng kết quả giả thay cho phần thiếu. Lượt Gemini cũ đang tạm dừng không được tiếp tục.
