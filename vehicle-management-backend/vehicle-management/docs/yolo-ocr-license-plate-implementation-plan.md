# Ke hoach tich hop OCR bien so bang Docker image rieng

## 1. Ket luan kien truc

Khong dua source OCR vao du an `VehicleManagementProject`.

Du an `VehicleManagementProject` chi can:

- Backend Spring Boot them endpoint proxy OCR.
- Backend doc config `OCR_BASE_URL`, `OCR_INTERNAL_TOKEN`.
- Docker Compose local chay OCR image da build san.
- Frontend goi backend de auto-fill bien so.

Du an OCR rieng:

```text
C:\DiskD\GitHub\License-Plate-Recognition-YOLOv7-and-CNN-Model
```

se duoc dong goi thanh Docker image rieng, vi du:

```text
vehicle-ocr-service:local
```

Kien truc dung:

```text
Frontend React
  -> POST /vehicle-management/api/parking/ocr/license-plate
Backend Spring Boot
  -> POST ${OCR_BASE_URL}/v1/license-plate/recognize
OCR Docker image
  -> YOLOv7 detect bien so
  -> crop/rotate bien so
  -> CNN doc ky tu
  -> normalize bien so
```

## 2. Vi sao khong dua `ocr-service/` vao Vehicle Management

Khong can tao:

```text
vehicle-management-backend/vehicle-management/ocr-service/
```

Ly do:

- OCR la stack Python/Torch/TensorFlow/OpenCV, khac hoan toan Spring Boot.
- Project OCR da ton tai san, co model weight va pipeline rieng.
- Build image tu repo OCR rieng se sach hon va de nang cap model doc lap.
- Vehicle Management khong phai mang theo file model lon nhu `.pt`, `.h5`.
- CI/CD sau nay co the build backend va OCR image doc lap.

Vehicle Management chi can compose dung image:

```yaml
services:
  ocr-service:
    image: vehicle-ocr-service:local
    container_name: vehicle-ocr-service
    environment:
      OCR_INTERNAL_TOKEN: ${OCR_INTERNAL_TOKEN:-dev-ocr-internal-token}
      OCR_PORT: 8010
    ports:
      - "${OCR_HOST_PORT:-8010}:8010"
    restart: unless-stopped
```

## 3. OCR image lay tu project nao

Dung project hien co:

```text
C:\DiskD\GitHub\License-Plate-Recognition-YOLOv7-and-CNN-Model
```

Project nay co san cac thanh phan can thiet:

| Thanh phan | File/thu muc | Vai tro |
| --- | --- | --- |
| YOLOv7 detector | `LP_detect_yolov7_500img.pt` | Detect vung bien so |
| CNN OCR weight | `src/weights/weight.h5` | Doc ky tu bien so |
| YOLOv7 source | `models/`, `utils/`, `detect.py` | Inference detect |
| Crop/rotate/helper | `utils_LP.py`, `Preprocess.py` | Xu ly bien so sau detect |
| Prototype API | `app.py` | Tham khao, khong dung nguyen xi |

Khong nen dung nguyen `app.py` hien tai cho production vi:

- API dang la `/detect_license_plate`, chua dung contract noi bo.
- Chua co `/health`.
- Chua co `X-Internal-Token`.
- Response chua co `confidence`, `needs_review`, `candidates`.
- Dang mo `CORS(app)` cho moi domain.
- Dang ghi file `license_plates.txt` moi request.
- `detect.py` co nguy co loi neu khong detect duoc bien so.
- Docker nen dung `opencv-python-headless` thay vi OpenCV GUI.

## 4. Contract OCR image can expose

OCR image can expose API noi bo:

```http
GET /health
```

Response:

```json
{
  "status": "ok",
  "device": "cpu"
}
```

Endpoint nhan dien:

```http
POST /v1/license-plate/recognize
Content-Type: multipart/form-data
X-Internal-Token: dev-ocr-internal-token

image=<file>
```

Response thanh cong:

```json
{
  "license_plate": "30A12345",
  "normalized_license_plate": "30A12345",
  "confidence": 0.82,
  "detector_confidence": 0.91,
  "ocr_confidence": 0.74,
  "needs_review": false,
  "candidates": [
    {
      "license_plate": "30A12345",
      "normalized_license_plate": "30A12345",
      "confidence": 0.82,
      "detector_confidence": 0.91,
      "ocr_confidence": 0.74
    }
  ]
}
```

Response khong nhan dien ro:

```json
{
  "license_plate": "",
  "normalized_license_plate": "",
  "confidence": 0.0,
  "detector_confidence": 0.0,
  "ocr_confidence": 0.0,
  "needs_review": true,
  "candidates": []
}
```

## 5. Docker image de xuat cho OCR project

Trong repo OCR them cac file:

```text
Dockerfile
.dockerignore
requirements-service.txt
ocr_service/
  __init__.py
  config.py
  main.py
  plate_pipeline.py
  plate_normalizer.py
README.Docker.md
```

Image build tu repo OCR:

```bash
cd C:\DiskD\GitHub\License-Plate-Recognition-YOLOv7-and-CNN-Model
docker build -t vehicle-ocr-service:local .
```

Chay thu:

```bash
docker run --rm -p 8010:8010 ^
  -e OCR_INTERNAL_TOKEN=dev-ocr-internal-token ^
  vehicle-ocr-service:local
```

Test:

```bash
curl -X POST http://localhost:8010/v1/license-plate/recognize ^
  -H "X-Internal-Token: dev-ocr-internal-token" ^
  -F "image=@data/test/images/xemay2324.jpg"
```

## 6. Docker Compose trong Vehicle Management

File de xuat:

```text
vehicle-management-backend/vehicle-management/docker-compose.ocr.yml
```

No chi dung image, khong build source:

```yaml
services:
  ocr-service:
    image: ${OCR_IMAGE:-vehicle-ocr-service:local}
    container_name: ${OCR_CONTAINER_NAME:-vehicle-ocr-service}
    environment:
      OCR_HOST: 0.0.0.0
      OCR_PORT: 8010
      OCR_INTERNAL_TOKEN: ${OCR_INTERNAL_TOKEN:-dev-ocr-internal-token}
      OCR_CONF_THRESHOLD: ${OCR_CONF_THRESHOLD:-0.25}
      OCR_REVIEW_THRESHOLD: ${OCR_REVIEW_THRESHOLD:-0.70}
      OCR_MAX_IMAGE_SIZE_BYTES: ${OCR_MAX_IMAGE_SIZE_BYTES:-5242880}
    ports:
      - "${OCR_HOST_PORT:-8010}:8010"
    restart: unless-stopped
```

## 7. Backend config de them

`application.yaml`:

```yaml
app:
  ocr:
    enabled: ${OCR_ENABLED:true}
    base-url: ${OCR_BASE_URL:http://localhost:8010}
    internal-token: ${OCR_INTERNAL_TOKEN:dev-ocr-internal-token}
    connect-timeout-ms: ${OCR_CONNECT_TIMEOUT_MS:1500}
    read-timeout-ms: ${OCR_READ_TIMEOUT_MS:5000}
    confidence-threshold: ${OCR_CONFIDENCE_THRESHOLD:0.70}
```

`.env.example`:

```properties
OCR_ENABLED=true
OCR_BASE_URL=http://localhost:8010
OCR_INTERNAL_TOKEN=dev-ocr-internal-token
OCR_CONNECT_TIMEOUT_MS=1500
OCR_READ_TIMEOUT_MS=5000
OCR_CONFIDENCE_THRESHOLD=0.70
```

Neu backend cung chay Docker chung network voi OCR:

```properties
OCR_BASE_URL=http://ocr-service:8010
```

Neu backend chay truc tiep tren may local:

```properties
OCR_BASE_URL=http://localhost:8010
```

## 8. Backend endpoint cho frontend

Them endpoint:

```http
POST /vehicle-management/api/parking/ocr/license-plate
Authorization: Bearer <token>
Content-Type: multipart/form-data

licensePlateImage=<file>
```

Backend lam cac viec:

- Validate auth/permission.
- Validate MIME va size file.
- Forward file sang OCR image bang `image=<file>`.
- Gui header `X-Internal-Token`.
- Map response OCR ve DTO Java.
- Neu OCR timeout/unavailable, tra loi de frontend cho nhap tay.

Khong de frontend goi truc tiep OCR image.

## 9. Frontend behavior

Sau khi chup/tai anh bien so:

- Goi backend OCR endpoint.
- Neu OCR thanh cong va confidence dat nguong:
  - Auto-fill `Bien so nhan dien`.
  - Hien badge `OCR 82%`.
- Neu confidence thap:
  - Co the fill candidate tot nhat.
  - Hien warning `Can kiem tra lai bien so`.
- Neu OCR loi:
  - Khong block check-in/check-out.
  - Cho nhan vien nhap tay.

Nhan vien luon co the sua bien so truoc khi submit.

## 10. Checklist thuc hien tiep

OCR project:

- [ ] Them Dockerfile.
- [ ] Them `.dockerignore`.
- [ ] Them `requirements-service.txt`.
- [ ] Them package `ocr_service`.
- [ ] Them `/health`.
- [ ] Them `/v1/license-plate/recognize`.
- [ ] Them `X-Internal-Token`.
- [ ] Khong ghi `license_plates.txt` trong API moi.
- [ ] Xu ly truong hop khong detect duoc bien so.
- [ ] Build image `vehicle-ocr-service:local`.

Vehicle Management:

- [ ] Them `docker-compose.ocr.yml` dung image.
- [ ] Them backend config `app.ocr`.
- [ ] Them backend proxy endpoint.
- [ ] Them frontend API call OCR.
- [ ] Auto-fill `licensePlate`.
- [ ] Van cho sua tay.

## 11. Ket luan

Dung project OCR hien co la hop ly. Cach lam dung la:

- Refactor project OCR thanh Docker image doc lap.
- Vehicle Management khong chua source OCR.
- Vehicle Management chi chay image qua compose va goi qua backend proxy.

Huong nay sach hon, it pha code hien tai, va cho phep nang cap model OCR doc lap voi nghiep vu check-in/check-out.
