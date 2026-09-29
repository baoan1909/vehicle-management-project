# Báo cáo chuyển đổi dữ liệu hành chính Việt Nam

## Nguồn chuẩn được chọn

| Bộ dữ liệu | Git tag | File nguồn | SHA-256 | Số lượng |
| --- | --- | --- | --- | --- |
| Hiện hành hai cấp | `v5.2.0` | `json/full_json_generated_data_vn_units.json` | `965c9a2ad85481e18ed2464fd87666eda311856b86a72a87f1130c27376b8d2f` | 34 tỉnh/thành, 3.321 phường/xã |
| Legacy ba cấp | `v2.4.1` | `json/full_json_generated_data_vn_units.json` | `9b0498612a737019374293273a31510757016e5a9c487e1782bab4443bf45309` | 63 tỉnh/thành, 696 quận/huyện, 10.035 phường/xã/thị trấn |

Nguồn: `https://github.com/thanglequoc/vietnamese-provinces-database`. Dữ liệu được đọc trực tiếp từ Git object bằng `git show`; không script nào từ repository nguồn được thực thi. License MIT và attribution được lưu trong seed metadata cũng như `docs/third-party/vietnamese-provinces-database-LICENSE.txt`.

## Lưu ý về count legacy

Yêu cầu ban đầu ghi 10.040 đơn vị cấp xã. Kiểm tra độc lập cả full JSON và PostgreSQL import tại tag `v2.4.1` đều cho kết quả **10.035**. Migration cố ý kiểm tra 10.035 để không tự tạo thêm hoặc bỏ qua bản ghi ngoài nguồn chuẩn.

## Đối chiếu file frontend cũ

| File cũ | SHA-256 | Count trong file | So với nguồn chuẩn |
| --- | --- | --- | --- |
| `vietnam-addresses-2025.json` | `4d1aef39bacea40bfaf77dbbf31c7836b7e9fc23be755c6788aefe3ab7e3200b` | 34 tỉnh, 3.321 phường/xã | Count bằng nhau; so theo đường dẫn `tỉnh/full-name → ward/full-name` có 645 đường dẫn chỉ có trong file frontend và 645 chỉ có trong v5.2.0. Phần lớn đến từ khác biệt chính tả/dấu và dữ liệu cập nhật, ví dụ `Hòa`/`Hoà`. |
| `vietnam-old-provinces.json` | `f2afc79f198d5eb0c411b5e3738c1810c2659a630eeb242f0efa2747a315d1d9` | 63 tỉnh, 718 huyện, 11.555 xã/phường | Nhiều hơn nguồn v2.4.1 22 huyện và 1.520 xã/phường; có 3.056 đường dẫn chỉ có trong file frontend, 1.537 chỉ có trong nguồn, và một đường dẫn bị trùng trong file frontend. |

Việc đối chiếu dùng mã dạng chuỗi và đường dẫn tên đầy đủ có cấp cha để tránh coi các phường/xã trùng tên ở địa phương khác là cùng một bản ghi.

## Quyết định chuyển đổi

- Backend/Flyway là nguồn dữ liệu duy nhất khi chạy ứng dụng.
- Frontend gọi API theo cấp và không còn đọc JSON trong `public/assets/data`.
- Mã hành chính được lưu dạng `varchar`, vì vậy mã có số `0` ở đầu không bị mất.
- Bảng `legacy_ward_mappings` lưu khóa ghép legacy/current, cho phép nhiều ánh xạ ứng viên thay vì giả định một-một.
- Quá trình chuyển đổi đã kiểm tra mã trùng, định dạng mã, tên không rỗng, quan hệ cha-con và count trước khi tạo SQL; migration tiếp tục kiểm tra lại sau khi insert.
- Hai Flyway seed migration là artifact chính thức được lưu trong repository. Runtime không phụ thuộc vào repository dữ liệu nguồn hay công cụ sinh seed riêng.
