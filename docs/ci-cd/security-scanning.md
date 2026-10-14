# Giai đoạn 3 — Quét bảo mật và dependency

CI/CD thực hiện quét bảo mật trước khi Docker image được publish. Mỗi job tạo
bằng chứng (report/log) để nhóm có thể kiểm tra lại khi báo cáo đồ án.

| Job | Công cụ | Ngưỡng chặn | Kết quả lưu lại |
| --- | --- | --- | --- |
| `01 - Secret scan` | Gitleaks | Bất kỳ secret bị phát hiện | Log GitHub Actions |
| `08 - Frontend dependency scan` | npm audit | HIGH/CRITICAL trong dependency runtime | `frontend-npm-audit-report` |
| `09 - Backend dependency scan` | Trivy (backend Java dependency scan) | HIGH/CRITICAL đã có bản vá | `backend-trivy-dependency-report` |
| `10 - Container and filesystem security scan` | Trivy (FS secret/misconfig, image CVE) | HIGH/CRITICAL đã có bản vá | `trivy-security-reports` |

Job `09` chờ backend build hoàn tất và dùng cùng Maven cache (`~/.m2/repository`)
trước khi Trivy phân giải dependency. Điều này tránh việc runner mới gửi hàng loạt
yêu cầu đến Maven Central, vốn có thể trả về `429 Too Many Requests`.

## Vì sao frontend chỉ chặn dependency runtime?

`npm audit --omit=dev` phản ánh chính xác dependency được mang vào Docker image
chạy production. Báo cáo đầy đủ vẫn được lưu để nhóm không bỏ qua rủi ro trong
công cụ build/test. Khi một dependency development có mức HIGH/CRITICAL, nhóm
phải tạo issue hoặc ghi vào AI Usage Log/nhật ký kỹ thuật với: mã advisory,
đường dẫn dependency, lý do không ảnh hưởng runtime, người phụ trách và ngày
xem xét lại. Không dùng `--force` để cập nhật mù quáng vì nó có thể nâng major
version và phá vỡ frontend.

Tại thời điểm thiết lập, audit production không có HIGH/CRITICAL. Các cảnh báo
trong full report phải được xử lý có kiểm soát ở PR riêng; đây không phải là
ngoại lệ để che giấu lỗi production.

### Theo dõi hiện trạng frontend

Audit runtime hiện báo 2 advisory mức **Moderate** liên quan `react-router`
(open redirect và constructor injection). Bản vá đề nghị nâng
`react-router-dom` lên major version 7, nên không chạy `npm audit fix --force`
trong pipeline. Nhóm cần tạo issue để kiểm thử nâng cấp này; nếu mức độ tăng
lên HIGH/CRITICAL thì job `08` sẽ chặn publish image ngay.

## OWASP Dependency-Check và NVD API key

OWASP Dependency-Check vẫn được giữ trong Maven để nhóm có thể chạy kiểm tra
chuyên sâu tại máy cá nhân hoặc theo lịch riêng. Nó tải cơ sở dữ liệu CVE rất
lớn từ NVD; quá trình đồng bộ ban đầu có thể kéo dài và bị NVD giới hạn request.
Vì vậy nó không nằm trên đường commit-to-staging.

Khi cần chạy OWASP Dependency-Check, tạo API key miễn phí tại NVD rồi thêm vào
môi trường local hoặc GitHub repository:

`Settings → Secrets and variables → Actions → New repository secret`

và tạo secret tên `NVD_API_KEY`. Key chỉ được đọc qua biến môi trường, không
ghi vào mã nguồn hoặc log. Quét dependency bắt buộc trong CI dùng Trivy, nên
NVD không còn có thể làm chậm thời gian commit-to-staging.

## Cách xử lý kết quả quét

1. Mở artifact hoặc log của job bị lỗi, xác định CVE/advisory và dependency
   kéo nó vào.
2. Ưu tiên nâng dependency trực tiếp hoặc base image có bản vá; chạy lại test,
   lint và build sau thay đổi.
3. Chỉ với phát hiện không có bản vá, Trivy đang dùng `ignore-unfixed: true`
   để tránh chặn release vì lỗi không thể khắc phục. Nếu cần chấp nhận rủi ro
   khác, phải ghi lý do, phạm vi ảnh hưởng, ngày hết hạn và người chịu trách
   nhiệm trong issue/AI Usage Log — không tự ý thêm ignore file.
4. Sau khi sửa, rerun workflow hoặc push commit mới. Các job bảo mật phải xanh
   trước khi job publish Docker image được chạy.

Trivy hoàn tất cả lượt quét filesystem, backend image và frontend image trước
khi chặn job. Vì vậy khi job `10` đỏ, tải artifact
`trivy-security-reports` để biết chính xác file, image hoặc cấu hình bị phát
hiện; không tắt scanner chỉ để làm pipeline xanh.

### Bài học từ lần quét đầu tiên

Artifact của lần quét đầu tiên đã chỉ ra các dependency/backend image có bản
vá. Nhóm đã nâng các dependency trực tiếp (`minio`, `jsoup`), các dependency
transitive do Spring Boot/MinIO kéo vào (Tomcat, Jackson, RabbitMQ và
Bouncy Castle), đồng thời chuyển image Nginx runtime sang Alpine mới hơn. Đây
là cách xử lý đúng: nâng phiên bản có bản vá, build/test lại, sau đó để Trivy
xác nhận — không thêm ngoại lệ để làm pipeline xanh giả tạo.

## Chạy tại máy cá nhân

```powershell
Set-Location vehicle-management-frontend/vehicle_management_react
npm ci
npm audit --omit=dev --audit-level=high

Set-Location ../../vehicle-management-backend/vehicle-management
$env:NVD_API_KEY = "<key-local-khong-commit>"
.\mvnw.cmd -B -Pdependency-security-scan -DskipTests dependency-check:check
```

Docker Desktop đang chạy thì có thể kiểm tra thêm:

```powershell
trivy fs --scanners secret,misconfig --severity HIGH,CRITICAL --ignore-unfixed .
```

CI là nguồn kết quả chính thức vì sử dụng môi trường sạch và cùng cấu hình mỗi
lần chạy.
