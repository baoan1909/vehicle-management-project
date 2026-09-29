# PostgreSQL 17 với pgvector và PostGIS

## Khởi động môi trường phát triển

Image nội bộ được build từ `pgvector/pgvector:0.8.6-pg17` và cài thêm
PostGIS cho PostgreSQL 17:

```powershell
$env:DB_HOST_PORT = "5434"
docker compose -f docker-compose.postgres-pgvector.yml up -d --build
docker inspect --format "{{.State.Health.Status}}" vehicle-pgvector
```

Với volume mới, init script cài cả `vector` và `postgis` trước khi health
check chạy. Với volume đã tồn tại, Flyway migration
`V20260928100000__add_postgis_parking_lot_location.sql` cài PostGIS và container
sẽ chuyển sang healthy sau khi migration hoàn tất.

Kiểm tra trực tiếp:

```sql
SELECT extname, extversion
FROM pg_extension
WHERE extname IN ('vector', 'postgis')
ORDER BY extname;
```

Kết quả phải có đúng hai dòng `postgis` và `vector`.

## Tách tài khoản migration và runtime

Ứng dụng hỗ trợ thông tin đăng nhập Flyway độc lập:

```text
DB_USERNAME=vehicle_app
DB_PASSWORD=<runtime-secret>
FLYWAY_DB_USERNAME=vehicle_migrator
FLYWAY_DB_PASSWORD=<migration-secret>
```

`vehicle_migrator` là tài khoản triển khai, được cấp quyền cài extension và thay
đổi schema. `vehicle_app` chỉ cần các quyền kết nối, sử dụng schema và DML cần
thiết; không cấp `SUPERUSER`, quyền tạo database, role hoặc extension cho tài
khoản này. Trong production, secret của migration chỉ tồn tại trong migration
job hoặc giai đoạn khởi động, không cung cấp cho request-serving container sau
khi migrate xong.

Các biến `FLYWAY_DB_URL`, `FLYWAY_DB_USERNAME`, `FLYWAY_DB_PASSWORD` mặc định
dùng lại datasource để môi trường local không bị phá vỡ. Production phải đặt
chúng tách biệt.

## Bất biến dữ liệu không gian

- API và entity tiếp tục nhận/lưu `latitude`, `longitude` dạng số.
- Client không có trường WKT/GeoJSON dành cho cột `location`.
- Trigger `parking.trg_parking_lots_sync_location` luôn tạo lại `location` từ
  longitude/latitude và bỏ qua geography được truyền trực tiếp.
- Cặp tọa độ phải cùng có hoặc cùng null, latitude thuộc `[-90, 90]`, longitude
  thuộc `[-180, 180]`.
- `location` có kiểu `geography(Point,4326)` và được lập chỉ mục GiST một phần
  cho các bãi `ACTIVE`.

## Xác minh sau triển khai

```sql
SELECT count(*) AS inconsistent_rows
FROM parking.parking_lots
WHERE (latitude IS NULL) <> (longitude IS NULL)
   OR (latitude IS NOT NULL AND location IS NULL)
   OR (location IS NOT NULL AND ST_SRID(location) <> 4326);

EXPLAIN (ANALYZE, BUFFERS)
SELECT parking_lot_id
FROM parking.parking_lots
WHERE status = 'ACTIVE'
  AND location IS NOT NULL
  AND ST_DWithin(
      location,
      ST_SetSRID(ST_MakePoint(106.700806, 10.776889), 4326)::geography,
      5000
  );
```

Truy vấn đầu phải trả `0`. Execution plan của truy vấn sau phải dùng
`idx_parking_lots_active_location_gist` khi thống kê và độ chọn lọc của dữ liệu
phù hợp. Integration test ép tắt sequential scan để xác nhận index có thể được
planner chọn độc lập với kích thước fixture nhỏ.
