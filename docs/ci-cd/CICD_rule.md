# CI/CD hiện tại của CoParking

## 1. Hệ thống hiện làm được gì?

Hệ thống đã hoàn thành **CI** và bước đầu của **CD**:

1. Khi tạo Pull Request, push vào `develop`, `main` hoặc `master`, GitHub
   Actions tự kiểm tra mã nguồn, test và quét bảo mật.
2. Khi tất cả các bước bắt buộc xanh và commit được push vào `main` hoặc
   `master`, workflow tự tạo hai Docker image (backend và frontend), rồi đẩy
   chúng lên GitHub Container Registry (GHCR).
3. Mỗi image có tag `latest` và tag là **đúng mã SHA của commit**. Tag SHA là
   bản phát hành không đổi, dùng để xác định chính xác phiên bản và rollback.

**Chưa triển khai ở thời điểm này:** tự động kết nối đến máy staging, triển
khai image lên staging, log tập trung và cảnh báo sau triển khai. Vì vậy không
được trình bày rằng dự án đã tự động deploy staging hay đã hoàn thành giám sát
production. Những nội dung đó là **Giai đoạn staging và vận hành tiếp theo**.

## 2. Luồng tổng thể

```text
Developer push / tạo Pull Request
             |
             v
GitHub Actions: 10 kiểm tra CI
             |
             +-- Có job đỏ -> dừng, không publish image
             |
             +-- Tất cả job xanh
                    |
                    +-- Pull Request hoặc develop -> kết thúc CI
                    |
                    +-- Push main/master -> build Docker image
                                           -> push GHCR với tag SHA + latest
                                           -> kết thúc CD đóng gói/publish
```

Trong workflow, các job độc lập chạy song song để rút ngắn thời gian. Riêng job
`09` và `10` chờ job `05` hoàn thành để tái sử dụng Maven cache, tránh tải hàng
loạt dependency từ Maven Central và bị lỗi `429 Too Many Requests`. Job CD chờ
toàn bộ các kiểm tra bắt buộc thành công.

## 3. Khi nào workflow chạy?

| Sự kiện | Các bước chạy | Có publish Docker image? |
| --- | --- | --- |
| Tạo/cập nhật Pull Request vào `develop`, `main`, `master` | 10 job CI | Không |
| Push vào `develop` | 10 job CI | Không |
| Push vào `main` hoặc `master` | 10 job CI và job CD | Có, nếu CI xanh |
| Chạy thủ công `workflow_dispatch` | 10 job CI | Không |

Điều này có ý nghĩa: Pull Request là nơi phát hiện lỗi an toàn trước khi merge;
`main`/`master` chỉ nhận image đã vượt qua tất cả cổng kiểm tra.

## 4. Mười cổng kiểm tra CI

| # | Job GitHub Actions | Kiểm tra gì? | Nếu đỏ thì sao? |
| --- | --- | --- | --- |
| 01 | Secret scan | Gitleaks quét source và toàn bộ Git history để phát hiện token, mật khẩu, private key bị lộ. | Chặn pipeline. |
| 02 | Frontend build | Cài dependency từ lock file rồi chạy `npm run build` để tạo bundle React/Vite. | Chặn pipeline. |
| 03 | Frontend static analysis | Chạy `tsc --noEmit` để kiểm tra TypeScript strict mà không tạo file build. | Chặn pipeline. |
| 04 | Frontend test | Chạy các script kiểm tra contract/cấu trúc luồng giao diện hiện có bằng `npm test`. | Chặn pipeline. |
| 05 | Backend build | Maven đóng gói Spring Boot bằng JDK 21, không chạy test ở bước này. | Chặn pipeline. |
| 06 | Backend static analysis | Biên dịch Java với `-Xlint:all` qua Maven profile `static-analysis`. | Chặn pipeline. |
| 07 | Backend test | Chạy test backend với PostgreSQL tạm có PostGIS và pgvector. | Chặn pipeline. |
| 08 | Frontend dependency scan | `npm audit` chặn lỗ hổng HIGH/CRITICAL trong dependency chạy production. | Chặn pipeline. |
| 09 | Backend dependency scan | Trivy kiểm tra dependency Java có CVE HIGH/CRITICAL đã có bản vá. | Chặn pipeline. |
| 10 | Container and filesystem security scan | Trivy quét secret/cấu hình sai trong repository, sau đó quét CVE của image backend và frontend. | Chặn pipeline. |

`ignore-unfixed: true` ở Trivy chỉ không chặn các CVE chưa có bản vá. Đây không
phải là bỏ quét: kết quả vẫn có trong report. Mọi ngoại lệ khác phải được ghi
nhận có lý do, người phụ trách và hạn xem xét lại; không thêm ignore tùy tiện
chỉ để làm pipeline xanh.

## 5. CD hiện tại: đóng gói và publish image

Job `CD - Build and Publish Docker Images` chỉ chạy sau toàn bộ 10 job CI, và
chỉ khi push vào `main` hoặc `master`.

Nó thực hiện:

1. Đăng nhập GHCR bằng `GITHUB_TOKEN` có quyền `packages: write`.
2. Build image backend từ
   `vehicle-management-backend/vehicle-management/Dockerfile`.
3. Build image frontend từ
   `vehicle-management-frontend/vehicle_management_react/Dockerfile`.
4. Push mỗi image với hai tag:

   ```text
   ghcr.io/<owner>/<repository>/coparking-backend:<commit-sha>
   ghcr.io/<owner>/<repository>/coparking-backend:latest
   ghcr.io/<owner>/<repository>/coparking-frontend:<commit-sha>
   ghcr.io/<owner>/<repository>/coparking-frontend:latest
   ```

Khi cần rollback trong tương lai, staging sẽ dùng lại tag SHA của commit xanh
trước đó. Không build lại source cũ trên máy staging, vì làm vậy không bảo đảm
image nhận được giống hệt image đã từng kiểm tra.

## 6. Artifact và cách kiểm tra kết quả

Sau mỗi workflow, vào **GitHub → Actions → chọn workflow run** để xem job,
log và artifact. Các artifact hiện có:

| Artifact | Dùng để làm gì? | Lưu trong |
| --- | --- | --- |
| `frontend-dist` | Bundle frontend của job build. | 7 ngày |
| `backend-package` | File JAR backend đã đóng gói. | 7 ngày |
| `frontend-test-log` | Log kiểm tra frontend. | 14 ngày |
| `backend-static-analysis-log` | Cảnh báo/lỗi `javac -Xlint:all`. | 14 ngày |
| `backend-test-report` | Surefire report và log test Maven. | 14 ngày |
| `frontend-npm-audit-report` | Toàn bộ kết quả npm audit. | 30 ngày |
| `backend-trivy-dependency-report` | CVE dependency Java do Trivy phát hiện. | 30 ngày |
| `trivy-security-reports` | Report filesystem, backend image và frontend image. | 30 ngày |

**Cách đọc nhanh kết quả:**

- Job xanh: kiểm tra đó đã qua trong môi trường GitHub runner sạch.
- Job đỏ: mở job, tìm dòng `ERROR`, `FAILURE`, CVE hoặc tên test thất bại; sau
  đó tải artifact tương ứng để xem chi tiết.
- Job CD bị `skipped` khi chạy Pull Request là đúng thiết kế, vì PR không được
  phép publish release image.
- Thời gian tổng của workflow xanh là bằng chứng CI đang đáp ứng mục tiêu thời
  gian kiểm tra. Chỉ khi triển khai staging tự động mới đo được đầy đủ thời
  gian từ commit đến staging.

## 7. Các file tham gia vào CI/CD và vai trò của chúng

| File/thư mục | Ý nghĩa | Tham gia vào luồng nào? |
| --- | --- | --- |
| `.github/workflows/ci-cd.yml` | Định nghĩa trigger, 10 job CI, điều kiện chặn, artifact và publish GHCR. | Trung tâm của CI/CD. |
| `.gitleaks.toml` | Quy tắc quét secret và các allowlist có kiểm soát, ví dụ UUID không phải secret. | Job 01. |
| `.pre-commit-config.yaml` | Cho phép thành viên chạy Gitleaks trước khi commit ở máy cá nhân. Đây là lớp phòng ngừa, không thay thế job 01 trên GitHub. | Trước commit local. |
| `vehicle-management-frontend/vehicle_management_react/package.json` | Khai báo lệnh `build`, `lint`, `test` và dependency frontend. | Job 02, 03, 04, 08; Docker build frontend. |
| `vehicle-management-frontend/vehicle_management_react/package-lock.json` | Khóa chính xác phiên bản npm dependency. | `npm ci`, cache và audit ổn định. |
| `vehicle-management-frontend/vehicle_management_react/tsconfig*.json` | Quy tắc TypeScript, trong đó có chế độ strict. | Job 03 và build frontend. |
| `vehicle-management-frontend/vehicle_management_react/Dockerfile` | Build React/Vite rồi chạy bundle bằng Nginx không có quyền root. | Job 10 và CD. |
| `vehicle-management-frontend/vehicle_management_react/nginx.conf` | Nginx chạy cổng nội bộ 8080, `/healthz`, proxy API tương đối đến service backend. | Image frontend. |
| `vehicle-management-backend/vehicle-management/pom.xml` | Khai báo Java 21, Maven dependency, test, profile static analysis và dependency security scan tùy chọn. | Job 05, 06, 07, 09; Docker build backend. |
| `vehicle-management-backend/vehicle-management/Dockerfile` | Build JAR Spring Boot rồi chạy bằng JRE 21, user `coparking`, health check Actuator. | Job 10 và CD. |
| `vehicle-management-backend/vehicle-management/docker/postgres/Dockerfile` | Tạo PostgreSQL 17 có pgvector và PostGIS. | Job 07. |
| `vehicle-management-backend/vehicle-management/docker/postgres/initdb/001-enable-required-extensions.sql` | Bật `vector` và `postgis` khi tạo database mới. | Job 07 / database Docker. |
| `vehicle-management-backend/vehicle-management/docker/postgres/healthcheck.sh` | Xác nhận PostgreSQL và hai extension thực sự sẵn sàng trước khi test. | Job 07. |
| `deploy/staging/docker-compose.yml` | Mẫu ghép image backend/frontend đã build sẵn. Chưa được workflow gọi ở giai đoạn hiện tại. | Chuẩn bị cho staging sau này. |
| `deploy/staging/.env.example` | Danh sách biến môi trường staging không có secret thật. File `.env` thật bị Git ignore. | Chuẩn bị cho staging sau này. |

## 8. Dockerfile và cách các container phối hợp

```text
Backend Dockerfile
  Maven + JDK 21 build JAR
  -> JRE 21 chạy Spring Boot bằng user coparking
  -> /vehicle-management/actuator/health

Frontend Dockerfile
  Node 22 build React/Vite
  -> Nginx chạy bằng user nginx ở cổng 8080
  -> /healthz và proxy /vehicle-management/api/ đến backend:8080

PostgreSQL Dockerfile
  PostgreSQL 17 + pgvector
  -> cài PostGIS
  -> khởi tạo extension vector + postgis
```

Trong CI, database chỉ là container tạm cho job test backend. Runner GitHub kết
thúc thì container và dữ liệu kiểm thử bị xóa. Database staging thật không nằm
trong Docker Compose hiện tại, nhằm tránh việc một lần deploy application vô tình
thay thế dữ liệu trạng thái.

## 9. Secret và nguyên tắc an toàn

- Không ghi mật khẩu, API key, token hoặc private key vào Git, Dockerfile,
  workflow, `.env.example` hay log.
- `GITHUB_TOKEN` được GitHub cấp tạm cho job CD để push package, không cần tự
  ghi token vào source.
- Các secret của staging sau này phải lưu bằng **GitHub Environment Secrets**
  hoặc secret manager của hạ tầng, sau đó inject khi deploy.
- `NVD_API_KEY` chỉ cần cho OWASP Dependency-Check chạy chuyên sâu theo lịch
  hoặc local. CI bắt buộc hiện dùng Trivy, nên không phụ thuộc NVD bulk sync.

## 10. Quy trình thành viên nên làm trước khi tạo Pull Request

```powershell
# Frontend
Set-Location vehicle-management-frontend/vehicle_management_react
npm ci
npm run lint
npm test
npm run build

# Backend
Set-Location ../../vehicle-management-backend/vehicle-management
.\mvnw.cmd -B -Pstatic-analysis -DskipTests clean compile
.\mvnw.cmd -B test
```

Backend test cần Docker Desktop chạy, vì test cần PostgreSQL có PostGIS và
pgvector. Nếu chưa có Docker Desktop, ít nhất chạy build và static analysis;
GitHub Actions vẫn là môi trường kiểm tra chính thức, sạch và lặp lại được.

## 11. Giai đoạn staging và vận hành tiếp theo — chưa thực hiện

Khi nhóm có staging host, domain/registry access và quyền quản trị phù hợp, bổ
sung sau vào workflow:

1. Tạo GitHub Environment `staging`; lưu SSH key, host, registry credential và
   application secret trong Environment Secrets.
2. Sau job publish image, SSH hoặc dùng self-hosted runner để cập nhật hai image
   tag SHA trong môi trường staging và chạy `docker compose pull` / `up -d`.
3. Chờ health check backend và frontend. Nếu lỗi, tự động quay về tag SHA xanh
   trước đó, đồng thời ghi nhận log deploy.
4. Gửi log tập trung (ví dụ Loki/ELK), metric (Prometheus/Grafana) và alert khi
   health check, error rate hoặc tài nguyên vượt ngưỡng.
5. Lưu bằng chứng ít nhất 10 lần auto-deploy, thời gian commit-to-staging và tỷ
   lệ workflow xanh để đối chiếu rubric.

Khi làm giai đoạn này, cập nhật trực tiếp tài liệu này với hạ tầng thực tế, thời
điểm triển khai và bằng chứng kiểm thử. Không thay thế bằng mô tả giả định.

## 12. Câu trả lời ngắn khi báo cáo

> Nhóm dùng GitHub Actions làm CI. Mỗi Pull Request đều qua secret scan, build,
> static analysis, test và quét dependency/container trước khi merge. Sau khi
> push vào main/master và tất cả cổng xanh, workflow build rồi publish image
> backend/frontend lên GHCR với tag commit SHA để truy vết và rollback. Hiện
> nhóm đã hoàn thành CI và CD ở mức đóng gói/publish image; auto-deploy staging,
> log tập trung và alert là giai đoạn vận hành tiếp theo nên nhóm không tuyên bố
> là đã hoàn thành.
