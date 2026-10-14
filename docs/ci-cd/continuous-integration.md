# Continuous Integration (Giai đoạn 2)

## Mục tiêu

Mỗi pull request hoặc push vào develop, main hay master phải kiểm tra được source
code trước khi image Docker được publish. Một job lỗi sẽ làm pipeline lỗi và chặn
job publish image.

## Các job hiện có

| Job | Mục đích | Lệnh chính |
| --- | --- | --- |
| 01 - Secret scan | Phát hiện secret trong source và Git history. | Gitleaks |
| 02 - Frontend build | Bảo đảm React/Vite tạo được production bundle. | npm run build |
| 03 - Frontend static analysis | Kiểm tra TypeScript strict, không sinh file build. | npm run lint |
| 04 - Frontend test | Chạy các kịch bản kiểm tra giao diện hiện có. | npm test |
| 05 - Backend build | Đóng gói Spring Boot mà chưa chạy test. | ./mvnw -B -DskipTests package |
| 06 - Backend static analysis | Biên dịch Java với cảnh báo -Xlint:all. | ./mvnw -B -Pstatic-analysis -DskipTests clean compile |
| 07 - Backend test | Chạy unit/integration test với PostgreSQL PostGIS/pgvector tạm thời. | ./mvnw -B test |

Các job test và static analysis lưu log/report thành GitHub Actions artifact trong
14 ngày. Job build lưu bundle frontend và JAR backend trong 7 ngày để tiện tra
cứu lỗi.

## Vì sao static analysis đang dùng compiler

Frontend đang bật TypeScript strict, vì vậy lệnh tsc --noEmit là bước kiểm tra
kiểu hiệu quả, nhẹ và không đụng vào format source cũ. Backend dùng Maven profile
static-analysis để bật javac -Xlint:all, giúp phát hiện các cảnh báo Java mà
không tự format hoặc gây lỗi hàng loạt trên code đang tồn tại.

Khi baseline cảnh báo ổn định, nhóm có thể bổ sung Checkstyle, SpotBugs hoặc PMD
với cấu hình và kế hoạch xử lý vi phạm riêng.

## Database test tạm thời

Một số test backend cần migration, PostGIS và pgvector. GitHub Actions sẽ build
image từ docker/postgres/Dockerfile, chạy PostgreSQL tạm trên runner và đợi
script kiểm tra PostgreSQL, PostGIS và pgvector thành công trước khi chạy Maven
test. Runner là máy tạm nên database này
và toàn bộ dữ liệu kiểm thử sẽ bị xóa sau job.

## Kiểm tra trước khi push

```powershell
Set-Location vehicle-management-frontend/vehicle_management_react
npm run lint
npm test
npm run build
```

```powershell
Set-Location vehicle-management-backend/vehicle-management
.\mvnw.cmd -B -Pstatic-analysis -DskipTests clean compile
.\mvnw.cmd -B test
```

Lệnh backend test cần Docker Desktop đang chạy vì nó cần PostgreSQL có PostGIS và
pgvector. Trên GitHub Actions, Docker đã có sẵn trên Ubuntu runner.

## Phạm vi sau Giai đoạn 3

Quét dependency và Docker image đã được bổ sung ở Giai đoạn 3; xem
[`security-scanning.md`](security-scanning.md). Deploy staging, rollback và
giám sát/alert vẫn thuộc các giai đoạn tiếp theo.
