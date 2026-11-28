# Báo cáo đợt 4 — 24/11/2026

> Local inference đã gỡ ngày 27/11/2026; các lượt đo/command cũ trong báo cáo chỉ có giá trị lịch sử. Dùng [bàn giao hiện tại](../../../TEAM_HANDOFF.vi.md), không tiếp tục chạy công cụ local đã xóa.

Trạng thái: **DRAFT — TẠM DỪNG THEO YÊU CẦU, CHƯA NGHIỆM THU**.

## Điểm dừng

Cả năm biến GEMINI_API_KEY và _1 đến _4 đã có DAILY_QUOTA trên ít nhất một model và lỗi TRANSIENT. Không có bằng chứng mọi model đều hết quota hoặc key sai. Theo yêu cầu người dùng, đã dừng hai cây tiến trình nghiệm thu và xác nhận không còn hàng đợi gọi API; không tự chạy lại hoặc đặt lịch. [Trạng thái chi tiết](evidence/batch-04/live-paused-by-user.json).

Bản cuối v14 chạy74/264 câu, rà soát đủ74:49 đạt,25 thất bại do provider; còn190 chưa chạy. Lượt đầu chạm giới hạn20 phút của harness; lượt hai dừng trước API và chưa có checkpoint. Chưa hoàn tất truy xuất tích lũy780 hoặc nhóm lặp hồi quy cuối. Giữ nguyên25 thất bại, không thay bằng kết quả thử lại. Chưa đủ mẫu để nghiệm thu bất kỳ nhóm chưa hoàn tất nào.

## Phạm vi đã sửa

Bốn nguồn: xe vào/ra; tìm bãi và bản đồ; địa chỉ/giờ/liên hệ/phản hồi; lỗi thường gặp. Corpus16 nguồn DRAFT,1104 câu gồm baseline120, ba đợt240 và đợt4 với264 câu/44 tình huống/sáu biến thể; chưa có holdout mù độc lập.

Code sửa chọn bằng chứng theo hướng xe và giai đoạn phiếu mất thẻ; lịch sử cùng người/đúng thời điểm; phân biệt lỗi đăng nhập với quyền và Google thường/ẩn danh; câu không dấu; phủ định tài chính; giữ liên hệ công khai chỉ khi có cấu hình và nguồn. FAQ hotline/email công ty nhận diện hẹp để trả nguyên nguồn hiện hành, giảm gọi model; không mở rộng sang dữ liệu riêng hay tài chính. Trang Liên hệ dùng thông tin đã duyệt và khóa form chưa có luồng nhận, kèm kênh thay thế.

Mặc định sinh4 giây, ngân sách provider12 giây/lượt, tối đa3 lượt worker; kiểm tra hạn trước candidate, giữ lỗi gốc và giảm chờ riêng timeout. Không coi cấu hình4 giây là tốc độ thực: có ca khoảng38 giây sau retries.

## Kiểm thử và phạm vi bằng chứng

Backend cuối4220 test:4219 qua,1 bỏ qua,0 lỗi; Python12 qua; sync16 nguồn/4 đợt/1104 câu không lỗi, không chạm DB. Frontend lint/regression và Playwright1test đã qua trên source không đổi. [Bằng chứng kiểm tra](evidence/batch-04/verification-status.json), [frontend](evidence/batch-04/frontend-verification.json). Route984 câu của các đợt đã qua, gồm264 mới; không khai baseline120 đều đã live.

Runtime: PostgreSQL/SQL/chunker cô lập; vector thật tương thích hoặc cache vector thật, provider thật khi sinh; actor/chat/audit/business là fixture, answercache tắt. Đây là kiểm thử agent, chưa phải UAT người dùng hoặc production. Snapshot9model đã được ứng dụng cho phép đi qua router/policy thật,1024token; không tự phê duyệt model. Các lượt cũ dùng fixture2model không đại diện cấu hình cuối.

## Kết quả tạm

Các tỷ lệ dưới đây chỉ trên phần đã chạy.

| Nguồn | Đã chạy/dự kiến | Đúng/đã rà soát |
|---|---:|---:|
| common-errors-and-troubleshooting | 14/66 | 8/14 |
| locations-hours-sla-contact | 60/66 | 41/60 |
| parking-entry-exit | 0/66 | 0/0 |
| parking-search-and-map | 0/66 | 0/0 |

| Nhóm câu | Đã chạy/dự kiến | Đúng/đã rà soát |
|---|---:|---:|
| ACCENTED | 13/44 | 13/13 |
| CONTEXTUAL | 12/44 | 5/12 |
| PARAPHRASE | 12/44 | 6/12 |
| SHORT | 12/44 | 6/12 |
| TYPO | 12/44 | 6/12 |
| UNACCENTED | 13/44 | 13/13 |

[Metrics](evidence/batch-04/final-v14-metrics.json) chứa p50/p95/max, retrieval/citation, model và số lần thử. Pool ghi385HTTPsend của run đầu, gồm embedding nếu có; không đồng nghĩa số generationattempt/audit. [Chấm từng ca](evidence/batch-04/final-v14-semantic-review.json), [74 transcript](evidence/batch-04/final-v14-contact-errors-answers.json), [hash](evidence/batch-04/final-v14-input-hashes.json). Không công bố tỷ lệ citation/gap cho toàn264 khi chưa chạy đủ.

## Tiếp tục sau khi quota hồi

Chờ người dùng yêu cầu tiếp tục, xác nhận khả dụng bằng số lần gọi tối thiểu, kiểm tra hash không đổi rồi chạy đúng190ID chưa hoàn tất. Giữ các lỗi cũ; lần lặp báo riêng. Hoàn tất780retrieval, hồi quy cũ/đợt3/ca quan trọng và các gate trước khi kết luận. Không reset/nạp/publish DB dev, không commit/push/deploy hoặc thao tác kinh doanh thật.

Lượt264 trước các sửa cuối đạt240/264 được giữ [transcript lịch sử](evidence/batch-04/before-guide-and-public-contact-fix.json) và [chấm lịch sử](evidence/batch-04/before-guide-and-public-contact-fix-semantic-review.json); không gộp revision cũ với v14.


## Rà soát lại cùng Qwen — 24/11/2026

Đã cập nhật bốn nguồn của đợt này, bản xuất và checksum trong lượt rà soát 16 tài liệu. Giữ kết quả ở trên làm lịch sử. Kiểm tra cấu trúc mới đã qua; Java/SQL/FE/Qwen live của bản mới còn chờ RAM và chưa nghiệm thu đúng/đủ ý. Xem [báo cáo tổng hợp](../../../ALL_BATCHES_REVIEW_STATUS.vi.md) để biết thay đổi, phiên bản, trạng thái từng tài liệu và bằng chứng thực chạy. Không kế thừa điểm của corpus cũ làm điểm của bản mới; Google chưa kiểm chứng trong lượt này.
