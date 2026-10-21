
-- Seed quyền quản trị phí sàn toàn hệ thống.
-- Chỉ cấp cho SYSTEM_ADMIN, không hard-code role trong Java.
-- 6 quyền: CREATE / READ / UPDATE / ACTIVATE / ARCHIVE / PREVIEW.

INSERT INTO iam.permission_modules (
    module_id,
    code,
    name,
    description,
    created_at
)
VALUES (
    '00000000-0000-0000-0000-000000001045',
    'PLATFORM_FEE',
    'Phí sàn',
    'Quản trị các loại phí sàn áp dụng toàn hệ thống cho giao dịch mua vé.',
    now()
)
ON CONFLICT DO NOTHING;


INSERT INTO iam.permission_actions (
    action_id,
    code,
    name,
    description,
    created_at
)
VALUES
    (
        'e7c2b3f1-9a4d-4e2b-8f1c-000000000001',
        'ARCHIVE',
        'Lưu trữ',
        'Cho phép lưu trữ đối tượng nghiệp vụ.',
        now()
    ),
    (
        'e7c2b3f1-9a4d-4e2b-8f1c-000000000002',
        'PREVIEW',
        'Xem trước',
        'Cho phép xem trước kết quả tính toán.',
        now()
    )
ON CONFLICT DO NOTHING;


INSERT INTO iam.permissions (
    permission_id,
    permission_code,
    name,
    description,
    created_at,
    module_id,
    action_id,
    scope_id
)
VALUES
    (
        '00000000-0000-0000-0000-000000004401',
        'PLATFORM_FEE_CREATE_ALL',
        'Tạo phí sàn',
        'Cho phép tạo loại phí sàn toàn hệ thống.',
        now(),
        '00000000-0000-0000-0000-000000001045',
        '00000000-0000-0000-0000-000000002001',
        '00000000-0000-0000-0000-000000003001'
    ),
    (
        '00000000-0000-0000-0000-000000004402',
        'PLATFORM_FEE_READ_ALL',
        'Xem phí sàn',
        'Cho phép xem danh sách, chi tiết và lịch sử phí sàn.',
        now(),
        '00000000-0000-0000-0000-000000001045',
        '00000000-0000-0000-0000-000000002002',
        '00000000-0000-0000-0000-000000003001'
    ),
    (
        '00000000-0000-0000-0000-000000004403',
        'PLATFORM_FEE_UPDATE_ALL',
        'Cập nhật phí sàn',
        'Cho phép cập nhật loại phí và phiên bản ở trạng thái DRAFT.',
        now(),
        '00000000-0000-0000-0000-000000001045',
        '00000000-0000-0000-0000-000000002003',
        '00000000-0000-0000-0000-000000003001'
    ),
    (
        '00000000-0000-0000-0000-000000004404',
        'PLATFORM_FEE_ACTIVATE_ALL',
        'Kích hoạt phí sàn',
        'Cho phép kích hoạt hoặc ngừng áp dụng loại phí và phiên bản tỷ lệ.',
        now(),
        '00000000-0000-0000-0000-000000001045',
        '38f8b771-b1d7-4b5d-9b26-8c905804a3bf',
        '00000000-0000-0000-0000-000000003001'
    ),
    (
        '00000000-0000-0000-0000-000000004405',
        'PLATFORM_FEE_ARCHIVE_ALL',
        'Lưu trữ phí sàn',
        'Cho phép lưu trữ loại phí và phiên bản, không xóa cứng dữ liệu tài chính.',
        now(),
        '00000000-0000-0000-0000-000000001045',
        'e7c2b3f1-9a4d-4e2b-8f1c-000000000001',
        '00000000-0000-0000-0000-000000003001'
    ),
    (
        '00000000-0000-0000-0000-000000004406',
        'PLATFORM_FEE_PREVIEW_ALL',
        'Xem trước phí sàn',
        'Cho phép xem trước tổng phí áp dụng trên đơn mua vé giả lập.',
        now(),
        '00000000-0000-0000-0000-000000001045',
        'e7c2b3f1-9a4d-4e2b-8f1c-000000000002',
        '00000000-0000-0000-0000-000000003001'
    )
ON CONFLICT DO NOTHING;


INSERT INTO iam.role_permissions (
    id,
    role_id,
    permission_id,
    created_at,
    is_active,
    is_system
)
SELECT
    gen_random_uuid(),
    system_admin.role_id,
    permission.permission_id,
    now(),
    true,
    true
FROM iam.roles system_admin
JOIN iam.permissions permission
    ON permission.permission_code IN (
        'PLATFORM_FEE_CREATE_ALL',
        'PLATFORM_FEE_READ_ALL',
        'PLATFORM_FEE_UPDATE_ALL',
        'PLATFORM_FEE_ACTIVATE_ALL',
        'PLATFORM_FEE_ARCHIVE_ALL',
        'PLATFORM_FEE_PREVIEW_ALL'
    )
WHERE system_admin.code = 'SYSTEM_ADMIN'
ON CONFLICT DO NOTHING;